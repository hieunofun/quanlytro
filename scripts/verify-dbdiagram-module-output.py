"""Verify each SVG/PDF is the same actual partial schema, then package exactly 8 PDFs."""
from pathlib import Path
import hashlib
import json
import sys
import unicodedata
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/dbdiagram/modules"
sys.path.insert(0, str(ROOT / ".runtime/presentation-tools"))
import pymupdf


def main():
    manifest = json.loads((OUT / "manifest.json").read_text(encoding="utf-8"))
    schema = json.loads((ROOT / "docs/dbdiagram/schema-snapshot.json").read_text(encoding="utf-8"))
    report = json.loads((OUT / "render-validation.json").read_text(encoding="utf-8"))
    ns = {"s": "http://www.w3.org/2000/svg"}
    for module in manifest["modules"]:
        slug = module["slug"]
        svg = ET.fromstring((OUT / (slug + ".svg")).read_text(encoding="utf-8"))
        nodes = svg.findall('.//s:g[@class="node"]', ns)
        edges = svg.findall('.//s:g[@class="edge"]', ns)
        assert {n.get("id").removeprefix("table-") for n in nodes} == set(module["columns_shown"])
        for node in nodes:
            table = node.get("id").removeprefix("table-")
            texts = ["".join(t.itertext()) for t in node.findall(".//s:text", ns)]
            assert texts[0] == table
            fields = texts[2:]
            assert len(fields) == len(module["columns_shown"][table]) * 4, (table, fields)
            actual = [[fields[i + 1], fields[i + 2], fields[i + 3]] for i in range(0, len(fields), 4)]
            expected = [[c["name"], c["type"].replace("character varying", "varchar").replace("timestamp with time zone", "timestamptz"), "NN" if c["not_null"] else "NULL"] for c in schema["columns"] if c["table_name"] == table and c["name"] in module["columns_shown"][table]]
            assert actual == expected, (slug, table)
            if table in module["context_tables"]: assert "THAM CHIẾU" in texts[1]
        assert {e.get("id").removeprefix("fk-") for e in edges} == set(module["foreign_keys"])
        for edge in edges:
            fk = next(c for c in schema["constraints"] if c["name"] == edge.get("id").removeprefix("fk-") and c["kind"] == "f")
            title = edge.find("s:title", ns).text
            assert title == fk["parent_table"] + ":" + fk["parent_columns"][0] + "->" + fk["table_name"] + ":" + fk["columns"][0], title
        pdf = pymupdf.open(OUT / (slug + ".pdf"))
        assert len(pdf) == 1
        text = unicodedata.normalize("NFKC", pdf[0].get_text())
        for table, columns in module["columns_shown"].items():
            assert table in text, (slug, table)
            assert all(column in text for column in columns), (slug, table, "PDF column missing")
        result = next(r for r in report["modules"] if r["slug"] == slug)
        result.update({"svg_tables_checked": len(nodes), "svg_foreign_keys_checked": len(edges), "svg_columns_types_nullability_match": True, "pdf_pages": len(pdf), "pdf_names_present": True, "pdf_sha256": hashlib.sha256((OUT / (slug + ".pdf")).read_bytes()).hexdigest()})
    archive = OUT / "8-so-do-nghiep-vu.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
        for m in manifest["modules"]: z.write(OUT / (m["slug"] + ".pdf"), m["slug"] + ".pdf")
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist()) == 8 and z.testzip() is None
    report.update({"pdf_files": 8, "svg_fk_total": sum(r["svg_foreign_keys_checked"] for r in report["modules"]), "zip_contains_exactly_eight_pdfs": True, "all_passed": True})
    (OUT / "render-validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"pdf_files": 8, "svg_foreign_keys": report["svg_fk_total"], "all_tables_columns_and_types_checked": True, "all_passed": True}))


if __name__ == "__main__":
    main()
