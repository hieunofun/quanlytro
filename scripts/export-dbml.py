"""Export the local project's live PostgreSQL schema as paste-ready DBML."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
GROUPS = [
    ("TaiKhoan", "#4C6FFF", ["users", "auth_sessions", "password_reset_codes", "employee_assignments", "room_employee_assignments"]),
    ("NhaTro", "#007D78", ["properties", "floors", "rooms", "room_images", "room_price_history"]),
    ("ThuePhong", "#B86916", ["tenant_profiles", "contracts", "contract_members", "contract_violations"]),
    ("DienNuoc", "#25865D", ["service_items", "service_price_configs", "alert_thresholds", "meter_readings", "meter_reading_history", "consumption_alerts"]),
    ("TaiChinh", "#8C4DC7", ["invoices", "invoice_items", "payment_transactions", "checkout_records"]),
    ("VanHanh", "#B64B5D", ["maintenance_requests", "maintenance_schedules", "notifications", "audit_logs", "uploaded_images"]),
]

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
    pg_get_expr(d.adbin,d.adrelid) AS default_value
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
  'migrations', (SELECT json_agg(version ORDER BY installed_rank)
    FROM flyway_schema_history WHERE success AND version IS NOT NULL)
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", default="55434")
    ap.add_argument("--database", default="hostel_demo")
    ap.add_argument("--user", default="postgres")
    ap.add_argument("--psql", default="C:/Program Files/PostgreSQL/18/bin/psql.exe")
    args = ap.parse_args()
    result = subprocess.run(
        [args.psql, "-X", "-w", "-h", args.host, "-p", args.port, "-U", args.user, "-d", args.database, "-v", "ON_ERROR_STOP=1", "-A", "-t"],
        input=QUERY, capture_output=True, text=True, encoding="utf-8",
        env={**os.environ, "PGCLIENTENCODING": "UTF8"}, check=True,
    )
    metadata = json.loads(result.stdout)
    ordered = [table for _, _, tables in GROUPS for table in tables]
    assert set(ordered) == {c["table_name"] for c in metadata["columns"]}
    assert metadata["migrations"] == [str(n) for n in range(1, 10)]
    actions = {"a": "no action", "r": "restrict", "c": "cascade", "n": "set null", "d": "set default"}
    lines = [
        "// DBML xuất từ PostgreSQL của repo quanlytro, sau migration V1–V9.",
        "// 29 bảng nghiệp vụ; không đưa flyway_schema_history vào ERD.",
        "Project QuanLyNhaTro {", "  database_type: 'PostgreSQL'", "}", "",
    ]
    for group_name, color, tables in GROUPS:
        lines += ["// " + group_name, ""]
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
                flags = " [" + ", ".join(settings) + "]" if settings else ""
                lines.append(f"  {col['name']} {column_type(col['type'])}{flags}")
            visible_indexes, notes = [], []
            for index in indexes:
                if index["constraint_kind"] == "x": continue
                if index["predicate"] is not None:
                    notes.append(index["definition"])
                    continue
                if index["is_primary"] and len(index["columns"]) == 1: continue
                if index["constraint_kind"] == "u" and len(index["columns"]) == 1: continue
                terms = []
                for name, term in zip(index["columns"], index["terms"]):
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
                lines += ["", "  Note: " + quoted("PostgreSQL — ràng buộc / chi tiết chỉ mục bổ sung:\n" + "\n".join(notes))]
            lines += ["}", ""]
    lines += ["// KHÓA NGOẠI", ""]
    refs = [c for c in metadata["constraints"] if c["kind"] == "f"]
    for ref in refs:
        def endpoint(table, cols):
            return table + "." + (cols[0] if len(cols) == 1 else "(" + ", ".join(cols) + ")")
        child = endpoint(ref["table_name"], ref["columns"])
        parent = endpoint(ref["parent_table"], ref["parent_columns"])
        keys = [c["columns"] for c in metadata["constraints"] if c["table_name"] == ref["table_name"] and c["kind"] in ("p", "u")]
        unique_fk = any(set(key).issubset(ref["columns"]) for key in keys)
        relationship = f"{parent} - {child}" if unique_fk else f"{child} > {parent}"
        lines.append(f"Ref {ref['name']}: {relationship} [delete: {actions[ref['on_delete']]}, update: {actions[ref['on_update']]}]")
    lines += ["", "// NHÓM NGHIỆP VỤ", ""]
    for name, _, tables in GROUPS:
        lines += [f"TableGroup {name} {{"] + ["  " + t for t in tables] + ["}", ""]
    output = ROOT / "docs/quanlytro.dbml"
    output.write_text("\n".join(lines), encoding="utf-8")
    snapshot = ROOT / ".runtime/dbml-tools/schema.json"
    snapshot.parent.mkdir(parents=True, exist_ok=True)
    snapshot.write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"file": str(output), "tables": len(ordered), "columns": len(metadata["columns"]), "foreign_keys": len(refs), "checks": sum(c["kind"] == "c" for c in metadata["constraints"]), "lines": len(lines)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
