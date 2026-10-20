#!/usr/bin/env python3
"""Chia lại JSONL theo ID bằng hàm băm ổn định, không xáo trộn ngẫu nhiên."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


def bucket(sample_id: str, development_ratio: float) -> str:
    digest = hashlib.sha256(sample_id.encode("utf-8")).digest()
    score = int.from_bytes(digest[:8], "big") / float(2**64)
    return "development" if score < development_ratio else "test"


def split_file(source: Path, destination: Path, development_ratio: float) -> tuple[int, int]:
    development = 0
    test = 0
    output: list[str] = []
    for line_number, line in enumerate(source.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        row = json.loads(line)
        sample_id = row.get("id")
        if not isinstance(sample_id, str) or not sample_id:
            raise ValueError(f"{source}:{line_number}: thiếu id")
        row["split"] = bucket(sample_id, development_ratio)
        if row["split"] == "development":
            development += 1
        else:
            test += 1
        output.append(json.dumps(row, ensure_ascii=False, sort_keys=True))
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text("\n".join(output) + "\n", encoding="utf-8", newline="\n")
    return development, test


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inputs", nargs="+", type=Path, help="Một hoặc nhiều file JSONL")
    parser.add_argument("--output-dir", type=Path, required=True, help="Thư mục nhận file đã chia")
    parser.add_argument("--development-ratio", type=float, default=0.6, help="Tỷ lệ development, mặc định 0.6")
    args = parser.parse_args()
    if not 0.0 < args.development_ratio < 1.0:
        parser.error("--development-ratio phải nằm giữa 0 và 1")
    for source in args.inputs:
        if not source.is_file():
            parser.error(f"Không tìm thấy file: {source}")
        development, test = split_file(source, args.output_dir / source.name, args.development_ratio)
        print(f"{source.name}: development={development}, test={test}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
