"""Export the running model49 PostgreSQL schema; never write to the database."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import hashlib
from datetime import date

ROOT = Path(__file__).resolve().parents[1]
GROUP_STYLE = {
    "TaiKhoan": ("01 · Tài khoản và phân quyền", "#475569"),
    "NhaTro": ("02 · Nhà trọ và tài sản", "#0F766E"),
    "CuTru": ("03 · Hợp đồng và cư trú", "#2563EB"),
    "DienNuoc": ("04 · Dịch vụ và điện nước", "#0284C7"),
    "TaiChinh": ("05 · Hóa đơn và dòng tiền", "#7C3AED"),
    "SuCo": ("06 · Sự cố và bảo trì", "#B45309"),
    "ThongBao": ("07 · Thông báo", "#BE185D"),
    "Tep": ("08 · Tệp đính kèm", "#4D7C0F"),
}
MANIFEST = json.loads((ROOT / "docs/schema49-tables.json").read_text(encoding="utf-8"))
GROUPS = [(name, style[1], [t["name"] for t in MANIFEST if t["group"] == name])
          for name, style in GROUP_STYLE.items()]

QUERY = """
WITH tables AS (
  SELECT t.oid, t.relname FROM pg_class t
  JOIN pg_namespace n ON n.oid=t.relnamespace
  WHERE n.nspname='public' AND t.relkind='r'
    AND t.relname<>'flyway_schema_history'
), cols AS (
  SELECT t.relname AS table_name, a.attname AS name, a.attnum AS position,
    format_type(a.atttypid,a.atttypmod) AS type,
    a.attnotnull AS not_null, a.attidentity AS identity,
    pg_get_expr(d.adbin,d.adrelid) AS default_value,
    col_description(t.oid,a.attnum) AS note
  FROM tables t JOIN pg_attribute a ON a.attrelid=t.oid
  LEFT JOIN pg_attrdef d ON d.adrelid=t.oid AND d.adnum=a.attnum
  WHERE a.attnum>0 AND NOT a.attisdropped
), cons AS (
  SELECT t.relname AS table_name, c.conname AS name, c.contype AS kind,
    ARRAY(SELECT a.attname FROM unnest(c.conkey) WITH ORDINALITY k(num,pos)
      JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.num ORDER BY k.pos) AS columns,
    p.relname AS parent_table,
    ARRAY(SELECT a.attname FROM unnest(c.confkey) WITH ORDINALITY k(num,pos)
      JOIN pg_attribute a ON a.attrelid=c.confrelid AND a.attnum=k.num ORDER BY k.pos) AS parent_columns,
    c.confdeltype AS on_delete, c.confupdtype AS on_update,
    pg_get_expr(c.conbin,c.conrelid) AS check_expression,
    pg_get_constraintdef(c.oid) AS definition
  FROM tables t JOIN pg_constraint c ON c.conrelid=t.oid
  LEFT JOIN pg_class p ON p.oid=c.confrelid
  WHERE c.contype IN ('p','u','f','c','x')
), idx AS (
  SELECT t.relname AS table_name, i.relname AS name,
    x.indisunique AS is_unique, x.indisprimary AS is_primary,
    ARRAY(SELECT a.attname FROM unnest(x.indkey::smallint[]) WITH ORDINALITY k(num,pos)
      LEFT JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.num
      WHERE k.pos<=x.indnkeyatts ORDER BY k.pos) AS columns,
    ARRAY(SELECT pg_get_indexdef(x.indexrelid,pos,true)
      FROM generate_series(1,x.indnkeyatts) pos) AS terms,
    pg_get_expr(x.indpred,x.indrelid) AS predicate,
    pg_get_indexdef(x.indexrelid) AS definition,
    c.contype AS constraint_kind, am.amname AS access_method
  FROM tables t JOIN pg_index x ON x.indrelid=t.oid
  JOIN pg_class i ON i.oid=x.indexrelid
  JOIN pg_am am ON am.oid=i.relam
  LEFT JOIN pg_constraint c ON c.conindid=x.indexrelid AND c.conrelid=t.oid
    AND c.contype IN ('p','u','x')
)
SELECT json_build_object(
  'columns', (SELECT json_agg(cols ORDER BY table_name,position) FROM cols),
  'constraints', (SELECT json_agg(cons ORDER BY table_name,name) FROM cons),
  'indexes', (SELECT json_agg(idx ORDER BY table_name,name) FROM idx),
  'views', (SELECT json_agg(json_build_object('name',c.relname,
    'definition',pg_get_viewdef(c.oid,true)) ORDER BY c.relname)
    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='public' AND c.relkind='v'),
  'server_version',current_setting('server_version'),
  'migrations', (SELECT json_agg(json_build_object('version',version,
    'script',script,'checksum',checksum,'success',success) ORDER BY installed_rank)
    FROM flyway_schema_history WHERE version IS NOT NULL)
);
"""


def quoted(value):
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"


def expression(value):
    assert "`" not in value, "Unexpected backtick in SQL expression"
    return "`" + value + "`"


def column_type(value):
    value = value.replace("character varying", "varchar").replace("timestamp with time zone", "timestamptz")
    return '"' + value + '"' if " " in value else value


def dbml_default(value):
    if re.fullmatch(r"[-+]?\d+(\.\d+)?|true|false", value, re.I):
        return value.lower()
    literal = re.fullmatch(r"'((?:[^']|'')*)'::(?:character varying|text)", value)
    if literal:
        return quoted(literal.group(1).replace("''", "'"))
    return expression(value)


def render_dbml(metadata):
    """Only visual annotations are authored; all structural facts come from PG."""
    ordered = [table for _, _, tables in GROUPS for table in tables]
    assert set(ordered) == {c["table_name"] for c in metadata["columns"]}
    assert len(ordered) == len(set(ordered)) == 49
    descriptions = {t["name"]: t["description"] for t in MANIFEST}
    actions = {"a": "no action", "r": "restrict", "c": "cascade", "n": "set null", "d": "set default"}
    views = metadata.get("views", [])
    view_names = [v["name"] if isinstance(v, dict) else v for v in views]
    counts = {kind: sum(c["kind"] == kind for c in metadata["constraints"]) for kind in "pfucx"}
    lines = [
        "// QUẢN LÝ NHÀ TRỌ · CSDL đang triển khai · model49 / V13",
        "// Sinh từ pg_catalog; không thêm bảng, cột, khóa ngoại hay enum giả định.",
        "// Màu và TableGroup chỉ phục vụ trình bày, không phải SQL schema mới.",
        "// Không dùng DBML này thay migration để tạo lại đầy đủ trigger PostgreSQL.",
        "Project QuanLyNhaTro {", "  database_type: 'PostgreSQL'",
        "  Note: " + quoted(
            f"49 bảng nghiệp vụ · {len(metadata['columns'])} cột · {counts['f']} FK.\n"
            f"{counts['c']} CHECK được giữ trong checks; {counts['x']} EXCLUDE và chỉ mục có điều kiện được ghi nguyên SQL trong Note từng bảng.\n"
            "Nguồn chuẩn khi triển khai: src/main/resources/db/migration/V1–V13.\n"
            "Không đưa bảng kỹ thuật flyway_schema_history vào sơ đồ nghiệp vụ.\n"
            f"{len(view_names)} SQL view hiện có (không vẽ thành bảng): " + ", ".join(view_names)
        ), "}", "",
    ]
    for group_name, color, tables in GROUPS:
        lines += ["// #region " + GROUP_STYLE[group_name][0], ""]
        for table in tables:
            cols = [c for c in metadata["columns"] if c["table_name"] == table]
            cons = [c for c in metadata["constraints"] if c["table_name"] == table]
            indexes = [i for i in metadata["indexes"] if i["table_name"] == table]
            pk = next((c["columns"] for c in cons if c["kind"] == "p"), [])
            unique = {c["columns"][0] for c in cons if c["kind"] == "u" and len(c["columns"]) == 1}
            lines.append(f"Table {table} [headercolor: {color}] {{")
            for col in cols:
                settings = []
                if pk == [col["name"]]: settings.append("pk")
                if col["not_null"]: settings.append("not null")
                if col["name"] in unique: settings.append("unique")
                auto = bool(col["identity"]) or (col["default_value"] or "").startswith("nextval(")
                if auto: settings.append("increment")
                elif col["default_value"] is not None: settings.append("default: " + dbml_default(col["default_value"]))
                if col.get("note"): settings.append("note: " + quoted(col["note"]))
                flags = " [" + ", ".join(settings) + "]" if settings else ""
                lines.append(f"  {col['name']} {column_type(col['type'])}{flags}")
            visible_indexes, notes = [], [descriptions[table]]
            for index in indexes:
                if index["constraint_kind"] == "x":
                    notes.append(index["definition"])
                    continue
                if index["predicate"] is not None:
                    notes.append(index["definition"])
                    continue
                if index["is_primary"] and len(index["columns"]) == 1: continue
                if index["constraint_kind"] == "u" and len(index["columns"]) == 1: continue
                terms = []
                for name, term in zip(index["columns"], index.get("terms", index["columns"])):
                    if name is None:
                        terms.append(expression(term))
                    else:
                        terms.append(name)
                        if term != name: notes.append(index["definition"])
                target = terms[0] if len(terms) == 1 else "(" + ", ".join(terms) + ")"
                settings = ["name: " + quoted(index["name"])]
                if index["is_primary"]: settings.append("pk")
                elif index["is_unique"]: settings.append("unique")
                if index["access_method"] in ("btree", "hash"): settings.append("type: " + index["access_method"])
                visible_indexes.append("    " + target + " [" + ", ".join(settings) + "]")
            if visible_indexes:
                lines += ["", "  indexes {"] + visible_indexes + ["  }"]
            checks = [c for c in cons if c["kind"] == "c"]
            if checks:
                lines += ["", "  checks {"]
                for check in checks:
                    lines.append("    " + expression(check["check_expression"]) + " [name: " + quoted(check["name"]) + "]")
                lines += ["  }"]
            notes += [c["name"] + ": " + c["definition"] for c in cons if c["kind"] == "x"]
            if notes:
                lines += ["", "  Note: " + quoted("\n\n".join(notes))]
            lines += ["}", ""]
        lines += ["// #endregion", ""]
    lines += ["// KHÓA NGOẠI", ""]
    refs = [c for c in metadata["constraints"] if c["kind"] == "f"]
    for ref in refs:
        def endpoint(table, cols):
            return table + "." + (cols[0] if len(cols) == 1 else "(" + ", ".join(cols) + ")")
        child = endpoint(ref["table_name"], ref["columns"])
        parent = endpoint(ref["parent_table"], ref["parent_columns"])
        keys = [c["columns"] for c in metadata["constraints"] if c["table_name"] == ref["table_name"] and c["kind"] in ("p", "u")]
        keys += [i["columns"] for i in metadata["indexes"] if i["table_name"] == ref["table_name"] and i["is_unique"] and not i["predicate"] and all(i["columns"])]
        unique_fk = any(set(key).issubset(ref["columns"]) for key in keys)
        nullable = any(not c["not_null"] for c in metadata["columns"] if c["table_name"] == ref["table_name"] and c["name"] in ref["columns"])
        relationship = (parent + (" ?- " if nullable else " - ") + child) if unique_fk else (child + (" >? " if nullable else " > ") + parent)
        lines.append(f"Ref {ref['name']}: {relationship} [delete: {actions[ref['on_delete']]}, update: {actions[ref['on_update']]}]")
    lines += ["", "// NHÓM NGHIỆP VỤ", ""]
    for name, color, tables in GROUPS:
        lines += [f"TableGroup {name} [color: {color}, note: {quoted(GROUP_STYLE[name][0])}] {{"] + ["  " + t for t in tables] + ["}", ""]
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", default="55449")
    ap.add_argument("--database", default="hostel_review49")
    ap.add_argument("--user", default="postgres")
    bundled = ROOT / ".runtime/tools/postgresql-18.6/pgsql/bin/psql.exe"
    ap.add_argument("--psql", default=str(bundled) if bundled.exists() else "psql")
    args = ap.parse_args()
    result = subprocess.run(
        [args.psql, "-X", "-w", "-h", args.host, "-p", args.port, "-U", args.user, "-d", args.database, "-v", "ON_ERROR_STOP=1", "-A", "-t"],
        input=QUERY, capture_output=True, text=True, encoding="utf-8",
        env={**os.environ, "PGCLIENTENCODING": "UTF8", "PGOPTIONS": "-c default_transaction_read_only=on"}, check=True,
    )
    metadata = json.loads(result.stdout)
    assert [m["version"] for m in metadata["migrations"]] == [str(n) for n in range(1, 14)]
    assert all(m["success"] for m in metadata["migrations"])
    expected = json.loads((ROOT / "docs/design-schema.json").read_text(encoding="utf-8"))
    # Consolidated DDL may order an ADD COLUMN earlier; use the live physical order.
    order_differences = []
    for section in ("columns", "constraints", "indexes"):
        for recorded in expected[section]:
            match = next((row for row in metadata[section] if row["table_name"] == recorded["table_name"] and row["name"] == recorded["name"]), None)
            assert match is not None and all(match.get(k) == v for k, v in recorded.items() if not (section == "columns" and k == "position")), f"Published schema differs: {section} {recorded['name']}"
            if section == "columns" and match["position"] != recorded["position"]:
                order_differences.append({"table": recorded["table_name"], "column": recorded["name"], "published_position": recorded["position"], "live_position": match["position"]})
        assert len(expected[section]) == len(metadata[section]), section
    metadata["source"] = {"validated_on": date.today().isoformat(), "database": args.database,
                          "host": args.host, "port": int(args.port), "read_only": True,
                          "matches_published_schema": True, "schema": "public", "model": "V13-model49",
                          "column_order_differences": order_differences}
    output = ROOT / "docs/quanlytro.dbml"
    output.write_text(render_dbml(metadata), encoding="utf-8")
    snapshot = ROOT / "docs/dbdiagram/schema-snapshot.json"
    snapshot.parent.mkdir(parents=True, exist_ok=True)
    snapshot.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"file": str(output), "tables": 49, "columns": len(metadata["columns"]), "foreign_keys": sum(c["kind"] == "f" for c in metadata["constraints"]), "matches_published_schema": True, "sha256": hashlib.sha256(output.read_bytes()).hexdigest()}, ensure_ascii=False))


if __name__ == "__main__":
    main()
