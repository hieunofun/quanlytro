"""Derive eight bounded ERD documents from the verified schema, with no SQL writes."""
from pathlib import Path
import hashlib
import importlib.util
import json
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/dbdiagram/modules"
MODULES = [
    ("TaiKhoan", "01-tai-khoan-phan-quyen", "Tài khoản và phân quyền"),
    ("NhaTro", "02-nha-tro-tai-san", "Nhà trọ và tài sản"),
    ("CuTru", "03-hop-dong-cu-tru", "Hợp đồng và cư trú"),
    ("DienNuoc", "04-dich-vu-dien-nuoc", "Dịch vụ và điện nước"),
    ("TaiChinh", "05-hoa-don-dong-tien", "Hóa đơn và dòng tiền"),
    ("SuCo", "06-su-co-bao-tri", "Sự cố và bảo trì"),
    ("ThongBao", "07-thong-bao", "Thông báo"),
    ("Tep", "08-tep-dinh-kem", "Tệp đính kèm"),
]


def main():
    spec = importlib.util.spec_from_file_location("dbml_export", ROOT / "scripts/export-dbml.py")
    emitter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(emitter)
    source = (ROOT / "docs/quanlytro.dbml").read_bytes().decode("utf-8")
    meta = json.loads((ROOT / "docs/dbdiagram/schema-snapshot.json").read_text(encoding="utf-8"))
    passed = json.loads((ROOT / "docs/dbdiagram/validation.json").read_text(encoding="utf-8"))
    assert passed["all_passed"] and hashlib.sha256(source.encode()).hexdigest() == passed["dbml_sha256"]
    source = source.replace("\r\n", "\n")
    manifest = json.loads((ROOT / "docs/schema49-tables.json").read_text(encoding="utf-8"))
    blocks = dict(re.findall(r"(?ms)^Table (\w+) [^\n]*\{\n(.*?)^\}", source))
    assert set(blocks) == {t["name"] for t in manifest}
    full_blocks = {m.group(1): m.group(0) for m in re.finditer(r"(?ms)^Table (\w+) [^\n]*\{\n.*?^\}", source)}
    columns = {t["name"]: [c for c in meta["columns"] if c["table_name"] == t["name"]] for t in manifest}
    refs = {m.group(1): m.group(0) for m in re.finditer(r"(?m)^Ref (\w+):[^\n]+", source)}
    fks = [c for c in meta["constraints"] if c["kind"] == "f"]
    display_choices = ["username", "full_name", "property_code", "room_code", "contract_code", "reservation_code", "document_code", "service_code", "meter_code", "asset_code", "handover_code", "entry_code", "account_code", "fund_code", "item_name_snapshot", "title", "name", "status", "observed_at"]
    documents = []
    OUT.mkdir(parents=True, exist_ok=True)
    for order, (group, slug, title) in enumerate(MODULES, 1):
        owned = [t["name"] for t in manifest if t["group"] == group]
        edges = [fk for fk in fks if fk["table_name"] in owned]
        context = sorted({fk["parent_table"] for fk in edges} - set(owned))
        color = emitter.GROUP_STYLE[group][1]
        lines = [f"// {order:02} · {title} — một phần của model49 / V13.",
                 "// Bảng chính giữ đủ cột; bảng ngoài nhóm chỉ là THAM CHIẾU RÚT GỌN.",
                 "// Chỉ gồm FK có bảng nguồn thuộc nhóm. Không mở rộng đệ quy từ bảng tham chiếu.",
                 "// Không dùng file từng phần để tạo lại database.",
                 f"Project {group} {{", "  database_type: 'PostgreSQL'",
                 "  Note: " + emitter.quoted(f"{title}: {len(owned)} bảng chính đầy đủ và {len(context)} bảng tham chiếu rút gọn. {len(edges)} FK thật có bảng nguồn thuộc nhóm. Bảng xám chỉ hiện PK/cột đích FK và một mã/tên có thật để nhận diện; không phải toàn bộ định nghĩa bảng. Bản chuẩn đầy đủ: docs/quanlytro.dbml."), "}", ""]
        projected_columns = {}
        for table in owned:
            lines += [full_blocks[table], ""]
            projected_columns[table] = [c["name"] for c in columns[table]]
        for table in context:
            keys = [c for c in meta["constraints"] if c["table_name"] == table]
            required = {n for c in keys if c["kind"] == "p" for n in c["columns"]}
            required |= {n for fk in edges if fk["parent_table"] == table for n in fk["parent_columns"]}
            display = next((n for n in display_choices if n in {c["name"] for c in columns[table]} and n not in required), None)
            if display:
                required.add(display)
            selected = [c for c in columns[table] if c["name"] in required]
            projected_columns[table] = [c["name"] for c in selected]
            lines += [f"// THAM CHIẾU: {table} — {len(selected)}/{len(columns[table])} cột thật.", f"Table {table} [headercolor: #64748B] {{"]
            for col in selected:
                flags = []
                if any(k["kind"] == "p" and k["columns"] == [col["name"]] for k in keys): flags.append("pk")
                if col["not_null"]: flags.append("not null")
                if any(k["kind"] == "u" and k["columns"] == [col["name"]] for k in keys): flags.append("unique")
                if col["identity"]: flags.append("increment")
                elif col["default_value"] is not None: flags.append("default: " + emitter.dbml_default(col["default_value"]))
                if col.get("note"): flags.append("note: " + emitter.quoted(col["note"]))
                lines.append(f"  {col['name']} {emitter.column_type(col['type'])}" + (" [" + ", ".join(flags) + "]" if flags else ""))
            lines += ["  Note: " + emitter.quoted(f"THAM CHIẾU RÚT GỌN · Bảng thuộc nhóm khác. Chỉ hiện {len(selected)}/{len(columns[table])} cột để đọc quan hệ; không hiện các FK xuất phát từ bảng này. Định nghĩa đầy đủ trong docs/quanlytro.dbml."), "}", ""]
        lines += ["// FK có bảng nguồn trong nhóm", *[refs[fk["name"]] for fk in edges], "", f"TableGroup {group} [color: {color}] {{", *["  " + t for t in owned], "}", ""]
        if context: lines += ["TableGroup ThamChieuRutGon [color: #64748B] {", *["  " + t for t in context], "}", ""]
        text = "\n".join(lines)
        (OUT / (slug + ".dbml")).write_text(text, encoding="utf-8", newline="\n")
        documents.append({"number": order, "group": group, "slug": slug, "title": title, "color": color,
                          "owned_tables": owned, "context_tables": context, "columns_shown": projected_columns,
                          "owned_columns": sum(len(columns[t]) for t in owned), "foreign_keys": [fk["name"] for fk in edges],
                          "dbml_sha256": hashlib.sha256(text.encode()).hexdigest()})
    assert len({t for d in documents for t in d["owned_tables"]}) == 49
    assert sum(len(d["foreign_keys"]) for d in documents) == 151
    result = {"schema": "V13-model49", "schema_verified_on": meta["source"]["validated_on"],
              "canonical_dbml_sha256": passed["dbml_sha256"], "policy": "All 49 tables are fully shown once as owned tables; all 151 FK sources assigned once; one-hop external parents are explicitly abbreviated reference copies.", "modules": documents}
    (OUT / "manifest.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"modules": len(documents), "owned_tables": 49, "owned_columns": sum(d["owned_columns"] for d in documents), "foreign_keys": 151}, ensure_ascii=False))


if __name__ == "__main__":
    main()
