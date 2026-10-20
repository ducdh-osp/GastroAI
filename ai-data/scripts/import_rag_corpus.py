#!/usr/bin/env python3
"""Đóng gói corpus Markdown thành DOCX rồi upload qua API tài liệu GastroAI."""

from __future__ import annotations

import argparse
import json
import mimetypes
import re
import shutil
import sys
import tempfile
import urllib.error
import urllib.request
import uuid
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


ROOT = Path(__file__).resolve().parents[1]


def markdown_text(path: Path) -> tuple[str, list[str]]:
    text = path.read_text(encoding="utf-8")
    source_ids: list[str] = []
    match = re.search(r"^source_ids:\s*(\[.*?\])\s*$", text, flags=re.MULTILINE)
    if match:
        source_ids = json.loads(match.group(1))
    if text.startswith("---\n"):
        _, _, text = text.partition("\n---\n")
    cleaned: list[str] = []
    for line in text.splitlines():
        line = re.sub(r"^#{1,6}\s+", "", line)
        line = re.sub(r"^[-*]\s+", "• ", line)
        line = line.replace("`", "")
        cleaned.append(line)
    return "\n".join(cleaned).strip(), source_ids


def make_docx(text: str, destination: Path) -> None:
    paragraphs: list[str] = []
    for line in text.splitlines():
        if line.strip():
            paragraphs.append(f"<w:p><w:r><w:t xml:space=\"preserve\">{escape(line)}</w:t></w:r></w:p>")
        else:
            paragraphs.append("<w:p/>")
    document_xml = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
        "<w:body>" + "".join(paragraphs) + "<w:sectPr/></w:body></w:document>"
    )
    content_types = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
        "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
        "</Types>"
    )
    relationships = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
        "</Relationships>"
    )
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", content_types)
        archive.writestr("_rels/.rels", relationships)
        archive.writestr("word/document.xml", document_xml)


def multipart_body(file_path: Path, source_url: str | None) -> tuple[bytes, str]:
    boundary = "----GastroAI" + uuid.uuid4().hex
    crlf = b"\r\n"
    parts: list[bytes] = []
    disposition = f'Content-Disposition: form-data; name="file"; filename="{file_path.name}"'
    content_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
    parts.extend([
        f"--{boundary}".encode(), disposition.encode(), f"Content-Type: {content_type}".encode(), b"", file_path.read_bytes()
    ])
    if source_url:
        parts.extend([
            f"--{boundary}".encode(), b'Content-Disposition: form-data; name="sourceUrl"', b"", source_url.encode("utf-8")
        ])
    parts.append(f"--{boundary}--".encode())
    return crlf.join(parts) + crlf, boundary


def upload(base_url: str, file_path: Path, source_url: str | None, cookie: str | None, bearer: str | None, timeout: int) -> dict:
    body, boundary = multipart_body(file_path, source_url)
    request = urllib.request.Request(
        base_url.rstrip("/") + "/api/v1/documents",
        data=body,
        method="POST",
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}", "Accept": "application/json"},
    )
    if cookie:
        request.add_header("Cookie", cookie)
    if bearer:
        request.add_header("Authorization", f"Bearer {bearer}")
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code}: {detail}") from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--cookie", help="Cookie phiên CMS, ví dụ JSESSIONID=...")
    parser.add_argument("--bearer-token", help="Bearer token nếu môi trường có hỗ trợ")
    parser.add_argument("--timeout", type=int, default=60)
    parser.add_argument("--execute", action="store_true", help="Thực sự upload; mặc định chỉ dry-run")
    parser.add_argument("--keep-docx", type=Path, help="Giữ các DOCX đã tạo tại thư mục này")
    args = parser.parse_args()
    if args.execute and not (args.cookie or args.bearer_token):
        parser.error("Khi --execute cần truyền --cookie hoặc --bearer-token")

    catalog = json.loads((ROOT / "sources" / "catalog.json").read_text(encoding="utf-8"))
    source_urls = {item["source_id"]: item["url"] for item in catalog["sources"]}
    corpus_paths = sorted((ROOT / "corpus").glob("*.md"))
    if not corpus_paths:
        parser.error("Không tìm thấy corpus. Hãy chạy generate_dataset.py trước.")

    temp_dir = Path(tempfile.mkdtemp(prefix="gastroai-corpus-"))
    try:
        for source_path in corpus_paths:
            text, source_ids = markdown_text(source_path)
            docx_path = temp_dir / f"{source_path.stem}.docx"
            make_docx(text, docx_path)
            source_url = source_urls.get(source_ids[0]) if source_ids else None
            if args.keep_docx:
                args.keep_docx.mkdir(parents=True, exist_ok=True)
                shutil.copy2(docx_path, args.keep_docx / docx_path.name)
            if not args.execute:
                print(f"DRY-RUN {docx_path.name} <- {source_path.name}; sourceUrl={source_url or '-'}")
                continue
            response = upload(args.base_url, docx_path, source_url, args.cookie, args.bearer_token, args.timeout)
            print(f"UPLOADED {docx_path.name}: documentId={response.get('documentId')} status={response.get('status')}")
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
