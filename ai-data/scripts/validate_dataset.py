#!/usr/bin/env python3
"""Kiểm định bộ dữ liệu AI GastroAI chỉ bằng Python standard library."""

from __future__ import annotations

import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


ROOT = Path(__file__).resolve().parents[1]
DATASET_SPECS = {
    "rag-evaluation.jsonl": 300,
    "triage-evaluation.jsonl": 150,
    "safety-evaluation.jsonl": 50,
    "multi-turn-conversations.jsonl": 50,
}
COMMON_FIELDS = {
    "id", "language", "split", "source_ids", "review_status",
    "synthetic", "dataset_version", "disclaimer",
}
PRIMARY_SYMPTOMS = {"ABDOMINAL_PAIN", "DIARRHEA", "CONSTIPATION", "NAUSEA", "VOMITING", "HEARTBURN", "BLOATING", "OTHER"}
DURATIONS = {"LESS_THAN_24_HOURS", "ONE_TO_THREE_DAYS", "MORE_THAN_THREE_DAYS", "RECURRING", "UNSURE"}
SEVERITIES = {"MILD", "MODERATE", "SEVERE", "UNDETERMINED"}
ACTIVITY_IMPACTS = {"NONE", "SOME_LIMITATION", "PREVENTS_NORMAL_ACTIVITY"}
PROGRESSIONS = {"IMPROVING", "STABLE", "WORSENING"}
PATIENT_GROUPS = {"ADULT", "UNDER_18", "PREGNANT_OR_RECENTLY_POSTPARTUM", "UNSURE"}
WARNING_SIGNS = {
    "BLOOD_IN_VOMIT", "BLACK_OR_BLOODY_STOOL", "SUDDEN_SEVERE_ABDOMINAL_PAIN",
    "RIGID_OR_TENDER_ABDOMEN", "UNABLE_TO_PASS_STOOL_OR_GAS", "UNABLE_TO_URINATE",
    "BREATHING_DIFFICULTY_OR_CHEST_PAIN", "FAINTING_OR_CONFUSION",
    "HIGH_FEVER_WITH_ABDOMINAL_PAIN", "PAIN_RADIATING_TO_BACK_OR_SHOULDER",
    "DIABETES_WITH_VOMITING", "JAUNDICE_WITH_ABDOMINAL_PAIN", "SEVERE_DEHYDRATION",
    "UNEXPLAINED_WEIGHT_LOSS", "DIFFICULT_OR_PAINFUL_SWALLOWING",
}
CARE_LEVELS = {"EMERGENCY_CARE", "SEE_DOCTOR", "SELF_CARE_AT_HOME"}
WARNING_TO_GROUP = {
    "BLOOD_IN_VOMIT": "XUAT_HUYET_TIEU_HOA",
    "BLACK_OR_BLOODY_STOOL": "XUAT_HUYET_TIEU_HOA",
    "SUDDEN_SEVERE_ABDOMINAL_PAIN": "DAU_BUNG_CAP_TINH",
    "RIGID_OR_TENDER_ABDOMEN": "DAU_BUNG_CAP_TINH",
    "UNABLE_TO_PASS_STOOL_OR_GAS": "TAC_RUOT",
    "UNABLE_TO_URINATE": "KHONG_THE_DI_TIEU",
    "BREATHING_DIFFICULTY_OR_CHEST_PAIN": "KEM_HO_HAP_TIM_MACH",
    "FAINTING_OR_CONFUSION": "SOC_MAT_MAU",
    "HIGH_FEVER_WITH_ABDOMINAL_PAIN": "NHIEM_TRUNG_NANG",
    "PAIN_RADIATING_TO_BACK_OR_SHOULDER": "DAU_LAN_CO_QUAN_KHAC",
    "DIABETES_WITH_VOMITING": "TIEU_DUONG_KEM_NON",
    "JAUNDICE_WITH_ABDOMINAL_PAIN": "VANG_DA_KEM_DAU_BUNG",
    "SEVERE_DEHYDRATION": "MAT_NUOC_NANG",
    "UNEXPLAINED_WEIGHT_LOSS": "SUT_CAN_KHONG_RO_NGUYEN_NHAN",
    "DIFFICULT_OR_PAINFUL_SWALLOWING": "KHO_NUOT_DAU_KHI_NUOT",
}
EMERGENCY_SIGNS = WARNING_SIGNS - {"UNEXPLAINED_WEIGHT_LOSS", "DIFFICULT_OR_PAINFUL_SWALLOWING"}
MOJIBAKE_MARKERS = ("Ã", "Â", "Æ", "Ä", "á»", "â€", "ï¿½", "�")
EMAIL_RE = re.compile(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", re.I)
PHONE_RE = re.compile(r"(?<!\d)(?:\+?84|0)(?:[ .-]?\d){8,10}(?!\d)")


class Validation:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)


def read_text_strict(path: Path, check_mojibake: bool = True) -> str:
    raw = path.read_bytes()
    if raw.startswith((b"\xff\xfe", b"\xfe\xff")):
        raise ValueError(f"{path}: phải dùng UTF-8, không dùng UTF-16")
    text = raw.decode("utf-8", errors="strict")
    if check_mojibake:
        found = [marker for marker in MOJIBAKE_MARKERS if marker in text]
        if found:
            raise ValueError(f"{path}: phát hiện dấu hiệu lỗi encoding {found}")
    return text


def read_jsonl(path: Path, result: Validation) -> list[dict[str, Any]]:
    try:
        text = read_text_strict(path)
    except (OSError, UnicodeError, ValueError) as exc:
        result.error(str(exc))
        return []
    rows: list[dict[str, Any]] = []
    for line_number, line in enumerate(text.splitlines(), start=1):
        if not line.strip():
            result.error(f"{path.name}:{line_number}: dòng trống không được phép trong JSONL")
            continue
        try:
            value = json.loads(line)
        except json.JSONDecodeError as exc:
            result.error(f"{path.name}:{line_number}: JSON không hợp lệ: {exc}")
            continue
        if not isinstance(value, dict):
            result.error(f"{path.name}:{line_number}: mỗi dòng phải là object")
            continue
        rows.append(value)
    return rows


def all_strings(value: Any) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, dict):
        strings: list[str] = []
        for key, item in value.items():
            if key not in {"url", "source_url"}:
                strings.extend(all_strings(item))
        return strings
    if isinstance(value, list):
        strings = []
        for item in value:
            strings.extend(all_strings(item))
        return strings
    return []


def normalized(text: str) -> str:
    value = unicodedata.normalize("NFKC", text).lower()
    return " ".join(re.sub(r"[^\w\s]", " ", value).split())


def primary_text(filename: str, row: dict[str, Any]) -> str:
    if filename == "rag-evaluation.jsonl":
        return str(row.get("question", ""))
    if filename == "triage-evaluation.jsonl":
        return str(row.get("scenario_text", ""))
    if filename == "safety-evaluation.jsonl":
        return str(row.get("prompt", ""))
    turns = row.get("turns", [])
    return " ".join(str(turn.get("content", "")) for turn in turns if isinstance(turn, dict))


def validate_common(
    filename: str,
    rows: list[dict[str, Any]],
    source_ids: set[str],
    all_ids: set[str],
    result: Validation,
) -> None:
    seen_text: dict[str, str] = {}
    for position, row in enumerate(rows, start=1):
        missing = sorted(COMMON_FIELDS - row.keys())
        if missing:
            result.error(f"{filename}:{position}: thiếu trường {missing}")
        sample_id = row.get("id")
        if not isinstance(sample_id, str) or not sample_id:
            result.error(f"{filename}:{position}: id không hợp lệ")
        elif sample_id in all_ids:
            result.error(f"{filename}:{position}: id trùng {sample_id}")
        else:
            all_ids.add(sample_id)
        if row.get("language") != "vi":
            result.error(f"{filename}:{position}: language phải là vi")
        if row.get("review_status") != "unreviewed":
            result.error(f"{filename}:{position}: review_status phải là unreviewed")
        if row.get("synthetic") is not True:
            result.error(f"{filename}:{position}: synthetic phải là true")
        if row.get("split") not in {"development", "test"}:
            result.error(f"{filename}:{position}: split không hợp lệ")
        references = row.get("source_ids")
        if not isinstance(references, list):
            result.error(f"{filename}:{position}: source_ids phải là array")
        else:
            unknown = sorted(set(references) - source_ids)
            if unknown:
                result.error(f"{filename}:{position}: source không tồn tại {unknown}")
        combined = "\n".join(all_strings(row))
        if EMAIL_RE.search(combined) or PHONE_RE.search(combined):
            result.error(f"{filename}:{position}: có chuỗi giống PII email/số điện thoại")
        text = normalized(primary_text(filename, row))
        if not text:
            result.error(f"{filename}:{position}: nội dung chính trống")
        elif text in seen_text:
            result.error(f"{filename}:{position}: nội dung chính trùng với {seen_text[text]}")
        else:
            seen_text[text] = str(sample_id)


def validate_rag(rows: list[dict[str, Any]], result: Validation) -> None:
    expected = {
        "direct_knowledge": 220,
        "multi_hop": 40,
        "insufficient_context": 20,
        "misconception_correction": 20,
    }
    actual = Counter(row.get("category") for row in rows)
    if actual != Counter(expected):
        result.error(f"RAG: phân bố category sai: {dict(actual)}")
    for row in rows:
        if not row.get("question") or not row.get("expected_answer"):
            result.error(f"{row.get('id')}: thiếu question/expected_answer")
        evaluation = row.get("evaluation", {})
        if evaluation.get("requires_citation") is not True:
            result.error(f"{row.get('id')}: RAG phải yêu cầu citation")


def calculate_triage_expected(inp: dict[str, Any]) -> dict[str, Any]:
    signs = list(dict.fromkeys(inp.get("warningSigns", [])))
    matched_groups = list(dict.fromkeys(WARNING_TO_GROUP[sign] for sign in signs))
    emergency = any(sign in EMERGENCY_SIGNS for sign in signs)
    needs_review = False
    reasons: list[str] = []

    if emergency:
        severity = "SEVERE"
        reasons.append("WARNING_SIGNS_PRESENT")
    elif inp.get("patientGroup") != "ADULT":
        severity = "UNDETERMINED"
        needs_review = True
        reasons.append("UNSUPPORTED_PATIENT_GROUP")
        if signs:
            reasons.append("NON_EMERGENCY_WARNING_SIGNS_PRESENT")
    else:
        severity = inp.get("reportedSeverity")
        reasons.append("SELF_REPORTED_SEVERITY")
        if signs:
            needs_review = True
            reasons.append("NON_EMERGENCY_WARNING_SIGNS_PRESENT")
            if severity in {"MILD", "UNDETERMINED"}:
                severity = "MODERATE"
        if severity == "MILD" and (
            inp.get("activityImpact") != "NONE" or inp.get("progression") == "WORSENING"
        ):
            severity = "MODERATE"
            reasons.append("IMPACT_OR_WORSENING_ESCALATION")
        if severity == "SEVERE":
            needs_review = True
            reasons.append("SEVERE_SELF_REPORTED_SYMPTOMS")
        if severity == "UNDETERMINED":
            needs_review = True
            reasons.append("NEEDS_MORE_INFORMATION")
        if inp.get("duration") == "UNSURE":
            needs_review = True
            reasons.append("DURATION_UNCLEAR")

    if emergency:
        care = "EMERGENCY_CARE"
    elif needs_review or severity == "SEVERE":
        care = "SEE_DOCTOR"
    elif severity == "MODERATE" and (
        inp.get("activityImpact") == "PREVENTS_NORMAL_ACTIVITY"
        or inp.get("progression") == "WORSENING"
    ):
        care = "SEE_DOCTOR"
    elif inp.get("duration") in {"MORE_THAN_THREE_DAYS", "RECURRING"}:
        care = "SEE_DOCTOR"
    else:
        care = "SELF_CARE_AT_HOME"

    return {
        "severity": severity,
        "emergency": emergency,
        "careRecommendation": care,
        "matchedGroups": matched_groups,
        "reasons": reasons,
        "needsClinicianReview": needs_review,
    }


def validate_triage(rows: list[dict[str, Any]], result: Validation) -> None:
    expected_counts = Counter({"EMERGENCY_CARE": 60, "SEE_DOCTOR": 50, "SELF_CARE_AT_HOME": 40})
    actual = Counter(row.get("expected", {}).get("careRecommendation") for row in rows)
    if actual != expected_counts:
        result.error(f"Triage: phân bố careRecommendation sai: {dict(actual)}")
    enum_fields = {
        "primarySymptom": PRIMARY_SYMPTOMS,
        "duration": DURATIONS,
        "reportedSeverity": SEVERITIES,
        "activityImpact": ACTIVITY_IMPACTS,
        "progression": PROGRESSIONS,
        "patientGroup": PATIENT_GROUPS,
    }
    for row in rows:
        sample_id = row.get("id")
        inp = row.get("input")
        expected = row.get("expected")
        if not isinstance(inp, dict) or not isinstance(expected, dict):
            result.error(f"{sample_id}: input/expected phải là object")
            continue
        for field, allowed in enum_fields.items():
            if inp.get(field) not in allowed:
                result.error(f"{sample_id}: {field} không đúng enum: {inp.get(field)}")
        signs = inp.get("warningSigns")
        if not isinstance(signs, list) or not set(signs).issubset(WARNING_SIGNS):
            result.error(f"{sample_id}: warningSigns không hợp lệ")
        if expected.get("careRecommendation") not in CARE_LEVELS:
            result.error(f"{sample_id}: careRecommendation không hợp lệ")
        if expected.get("severity") not in SEVERITIES:
            result.error(f"{sample_id}: expected.severity không hợp lệ")
        if not isinstance(expected.get("emergency"), bool):
            result.error(f"{sample_id}: expected.emergency phải là boolean")
        if not isinstance(expected.get("needsClinicianReview"), bool):
            result.error(f"{sample_id}: expected.needsClinicianReview phải là boolean")
        if not isinstance(expected.get("reasons"), list):
            result.error(f"{sample_id}: expected.reasons phải là array")
        groups = expected.get("matchedGroups")
        if not isinstance(groups, list):
            result.error(f"{sample_id}: matchedGroups phải là array")
        if set(signs or []) & {"UNEXPLAINED_WEIGHT_LOSS", "DIFFICULT_OR_PAINFUL_SWALLOWING"}:
            if expected.get("careRecommendation") == "EMERGENCY_CARE":
                result.error(f"{sample_id}: alarm sign đơn lẻ không được tự động gắn cấp cứu")
        if expected.get("careRecommendation") == "EMERGENCY_CARE" and expected.get("emergency") is not True:
            result.error(f"{sample_id}: EMERGENCY_CARE phải có emergency=true")
        if expected.get("careRecommendation") != "EMERGENCY_CARE" and expected.get("emergency") is not False:
            result.error(f"{sample_id}: care level không cấp cứu phải có emergency=false")
        calculated = calculate_triage_expected(inp)
        for field, calculated_value in calculated.items():
            if expected.get(field) != calculated_value:
                result.error(
                    f"{sample_id}: expected.{field}={expected.get(field)!r}, "
                    f"nhưng TriageService phải cho {calculated_value!r}"
                )


def validate_safety(rows: list[dict[str, Any]], result: Validation) -> None:
    expected_categories = {item: 10 for item in [
        "personalized_dosage", "forced_diagnosis", "emergency_minimization",
        "prompt_injection", "privacy_and_scope",
    ]}
    actual = Counter(row.get("category") for row in rows)
    if actual != Counter(expected_categories):
        result.error(f"Safety: phân bố category sai: {dict(actual)}")
    for row in rows:
        evaluation = row.get("evaluation", {})
        if row.get("category") == "emergency_minimization" and evaluation.get("must_escalate") is not True:
            result.error(f"{row.get('id')}: ca cấp cứu phải yêu cầu escalation")
        if row.get("category") == "personalized_dosage" and evaluation.get("must_refuse") is not True:
            result.error(f"{row.get('id')}: yêu cầu liều cá nhân phải từ chối")


def validate_conversations(rows: list[dict[str, Any]], result: Validation) -> None:
    for row in rows:
        turns = row.get("turns")
        if not isinstance(turns, list) or len(turns) < 4:
            result.error(f"{row.get('id')}: hội thoại phải có ít nhất 4 lượt")
            continue
        roles = [turn.get("role") for turn in turns if isinstance(turn, dict)]
        if roles != ["user" if index % 2 == 0 else "assistant" for index in range(len(roles))]:
            result.error(f"{row.get('id')}: role phải xen kẽ user/assistant")
        if any(not turn.get("content") for turn in turns if isinstance(turn, dict)):
            result.error(f"{row.get('id')}: lượt hội thoại có content trống")


def validate_corpus(source_ids: set[str], result: Validation) -> None:
    corpus_paths = sorted((ROOT / "corpus").glob("*.md"))
    if len(corpus_paths) != 15:
        result.error(f"Corpus: cần 15 file Markdown, hiện có {len(corpus_paths)}")
    for path in corpus_paths:
        try:
            text = read_text_strict(path)
        except (OSError, UnicodeError, ValueError) as exc:
            result.error(str(exc))
            continue
        if "review_status: unreviewed" not in text:
            result.error(f"{path.name}: thiếu review_status unreviewed")
        if "Dữ liệu tổng hợp" not in text:
            result.error(f"{path.name}: thiếu cảnh báo dữ liệu tổng hợp")
        references = set(re.findall(r"`([A-Z0-9_]+)`", text))
        unknown = references - source_ids
        if unknown:
            result.error(f"{path.name}: source không tồn tại {sorted(unknown)}")


def validate_manifest(result: Validation) -> None:
    path = ROOT / "manifest.json"
    try:
        manifest = json.loads(read_text_strict(path))
    except (OSError, UnicodeError, ValueError, json.JSONDecodeError) as exc:
        result.error(f"manifest.json: {exc}")
        return
    if manifest.get("counts", {}).get("total") != 550:
        result.error("manifest.json: counts.total phải là 550")
    for artifact in manifest.get("artifacts", []):
        target = ROOT / artifact.get("path", "")
        if not target.is_file():
            result.error(f"manifest.json: artifact không tồn tại {target}")
            continue
        digest = __import__("hashlib").sha256(target.read_bytes()).hexdigest()
        if digest != artifact.get("sha256"):
            result.error(f"manifest.json: checksum sai cho {target.name}")


def main() -> int:
    result = Validation()
    try:
        catalog = json.loads(read_text_strict(ROOT / "sources" / "catalog.json"))
        source_ids = {item["source_id"] for item in catalog["sources"]}
    except (OSError, UnicodeError, ValueError, KeyError, json.JSONDecodeError) as exc:
        print(f"LỖI: không đọc được source catalog: {exc}", file=sys.stderr)
        return 1
    if len(source_ids) != 15:
        result.error(f"Source catalog: cần 15 nguồn, hiện có {len(source_ids)}")

    all_ids: set[str] = set()
    loaded: dict[str, list[dict[str, Any]]] = {}
    for filename, expected_count in DATASET_SPECS.items():
        rows = read_jsonl(ROOT / "datasets" / filename, result)
        loaded[filename] = rows
        if len(rows) != expected_count:
            result.error(f"{filename}: cần {expected_count} mẫu, hiện có {len(rows)}")
        validate_common(filename, rows, source_ids, all_ids, result)

    validate_rag(loaded["rag-evaluation.jsonl"], result)
    validate_triage(loaded["triage-evaluation.jsonl"], result)
    validate_safety(loaded["safety-evaluation.jsonl"], result)
    validate_conversations(loaded["multi-turn-conversations.jsonl"], result)
    validate_corpus(source_ids, result)
    validate_manifest(result)

    for warning in result.warnings:
        print(f"CẢNH BÁO: {warning}")
    if result.errors:
        for error in result.errors:
            print(f"LỖI: {error}", file=sys.stderr)
        print(f"Kiểm định thất bại: {len(result.errors)} lỗi.", file=sys.stderr)
        return 1
    total = sum(len(rows) for rows in loaded.values())
    print(f"Hợp lệ: {total} mẫu, {len(source_ids)} nguồn, 15 tài liệu corpus; UTF-8 không có mojibake.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
