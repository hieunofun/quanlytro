"""Check the actual dbdiagram SVG/PDF, not just its DBML input."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/dbdiagram"
sys.path.insert(0, str(ROOT / ".runtime/presentation-tools"))
import pymupdf


def main():
    meta = json.loads((OUT / "schema-snapshot.json").read_text(encoding="utf-8"))
    result = json.loads((OUT / "render-validation.json").read_text(encoding="utf-8"))
    root = ET.fromstring((OUT / "quanlytro-dbdiagram.svg").read_text(encoding="utf-8"))
    ns = {"s": "http://www.w3.org/2000/svg"}
    drawing = root.find("s:g", ns)
    names = {c["table_name"] for c in meta["columns"]}
    drawn, current, relations = {}, None, 0
    for element in drawing:
        texts = ["".join(t.itertext()) for t in element.findall(".//s:text", ns)]
        if len(texts) == 1 and texts[0] in names:
            current = texts[0]
            assert current not in drawn
            drawn[current] = []
        elif len(texts) == 2 and all(t in ("*", "1", "0..1", "0..*") for t in texts):
            relations += 1
        elif element.tag == "{" + ns["s"] + "}svg" and len(texts) == 2:
            assert current is not None
            drawn[current].append(texts)
    assert set(drawn) == names
    for table, columns in drawn.items():
        expected = [[c["name"], c["type"].replace("character varying", "varchar").replace("timestamp with time zone", "timestamptz")]
                    for c in meta["columns"] if c["table_name"] == table]
        assert columns == expected, table
    assert relations == 151
    pdf = pymupdf.open(OUT / "quanlytro-database.pdf")
    assert len(pdf) == 1
    text = unicodedata.normalize("NFKC", "\n".join(p.get_text() for p in pdf))
    assert all(name in text for name in names), "PDF missing table titles"
    assert all(c["name"] in text for c in meta["columns"]), "PDF missing column names"
    result.update({"svg_tables_checked": len(drawn), "svg_columns_checked": sum(map(len, drawn.values())),
                   "svg_relationships_checked": relations, "svg_types_and_column_order_match": True,
                   "pdf_pages": len(pdf), "pdf_table_and_column_names_present": True})
    (OUT / "render-validation.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
