"""Build a local zoomable viewer for the one verified database diagram."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/dbdiagram"


def main():
    render = json.loads((OUT / "render-validation.json").read_text(encoding="utf-8"))
    layout = json.loads((OUT / "layout.json").read_text(encoding="utf-8"))
    model = {k: render[k] for k in ("width", "height")}
    model.update({"groups": layout["groups"], "tables": layout["tablePositions"]})
    url = (OUT / "dbdiagram-link.txt").read_text(encoding="utf-8").strip()
    template = r'''<!doctype html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>CSDL quản lý nhà trọ · V13</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#eef2f6;color:#172033;font:14px Arial,sans-serif}button,a,input,select{font:inherit}button,select{cursor:pointer}header{background:#fff;border-bottom:1px solid #dce3eb;padding:18px 24px;display:flex;justify-content:space-between;align-items:center;gap:16px}h1{font-size:19px;margin:0 0 6px}header p{margin:0;color:#64748b;font-size:13px}.links{display:flex;gap:8px;flex-wrap:wrap}.links a,button,select,input{border:1px solid #d4dce6;background:#fff;color:#334155;border-radius:6px;padding:9px 12px;text-decoration:none}.links a:hover,button:hover{border-color:#526f98;background:#f3f6fb}.links .primary{color:#fff;background:#315775;border-color:#315775}.toolbar{padding:12px 24px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;background:#f8fafc;border-bottom:1px solid #dce3eb}.toolbar input{width:235px}.toolbar select{max-width:285px}.toolbar output{width:55px;text-align:center;font-variant-numeric:tabular-nums}.toolbar .status{margin-left:auto;color:#64748b;font-size:12px}.viewport{height:calc(100vh - 164px);overflow:auto;padding:24px;outline:none}.paper{position:relative;background:#fff;box-shadow:0 3px 18px #24385417;margin:auto}.paper iframe{position:absolute;top:0;left:0;border:0;transform-origin:0 0;pointer-events:none;display:block}.hint{position:fixed;bottom:14px;right:24px;background:#fff;color:#64748b;border:1px solid #dce3eb;padding:7px 11px;border-radius:6px;font-size:12px;pointer-events:none}@media(max-width:900px){header{align-items:flex-start}.toolbar .status{display:none}.viewport{height:calc(100vh - 210px)}.links{max-width:250px}.hint{display:none}}@media print{header,.toolbar,.hint{display:none}.viewport{height:auto;padding:0}.paper{box-shadow:none}}
</style></head><body>
<header><div><h1>Cơ sở dữ liệu · Quản lý nhà trọ</h1><p>PostgreSQL · model49 / V13 · 49 bảng · 455 cột · 151 khóa ngoại</p></div>
<nav class="links" aria-label="Tài liệu sơ đồ"><a href="modules/index.html" class="primary">8 sơ đồ nghiệp vụ</a><a href="quanlytro-database.pdf" target="_blank">PDF tổng</a><a href="quanlytro-dbdiagram.svg" target="_blank">SVG</a><a href="../quanlytro.dbml" download>DBML</a><a href="__URL__" target="_blank" rel="noopener">Mở dbdiagram.io ↗</a></nav></header>
<div class="toolbar" role="toolbar" aria-label="Điều khiển sơ đồ">
<button id="fit">Vừa màn hình</button><button id="out" aria-label="Thu nhỏ">−</button><output id="zoom" aria-live="polite">100%</output><button id="in" aria-label="Phóng to">+</button><button id="actual">100%</button>
<select id="module" aria-label="Đến nhóm bảng"><option value="">Đến nhóm nghiệp vụ…</option></select>
<input id="table" list="tables" placeholder="Tìm bảng, ví dụ: fund_entries" aria-label="Tìm tên bảng"><datalist id="tables"></datalist>
<span class="status">Đã đối chiếu PostgreSQL · 01/11/2026</span></div>
<main class="viewport" id="viewport" tabindex="0" aria-label="Sơ đồ đầy đủ; dùng thanh cuộn và nút phóng to để đọc"><div class="paper" id="paper"><iframe src="poster.html" title="Sơ đồ CSDL quản lý nhà trọ đã kiểm chứng" id="diagram" tabindex="-1"></iframe></div></main>
<div class="hint">Bản PDF/SVG giữ bố cục này. Liên kết dbdiagram.io tự xếp lại vị trí.</div>
<script>
const model=__DATA__;
const viewport=document.getElementById('viewport'),paper=document.getElementById('paper'),frame=document.getElementById('diagram');let scale=1;
frame.width=model.width;frame.height=model.height;
function setZoom(value,keepCenter=true){const old=scale,cx=(viewport.scrollLeft+viewport.clientWidth/2)/old,cy=(viewport.scrollTop+viewport.clientHeight/2)/old;scale=Math.max(.15,Math.min(2.5,value));paper.style.width=model.width*scale+'px';paper.style.height=model.height*scale+'px';frame.style.transform='scale('+scale+')';document.getElementById('zoom').textContent=Math.round(scale*100)+'%';if(keepCenter){viewport.scrollLeft=cx*scale-viewport.clientWidth/2;viewport.scrollTop=cy*scale-viewport.clientHeight/2}}
function fit(){setZoom(Math.min((viewport.clientWidth-55)/model.width,(viewport.clientHeight-55)/model.height),false);viewport.scrollTo(0,0)}
document.getElementById('fit').onclick=fit;document.getElementById('actual').onclick=()=>setZoom(1);document.getElementById('in').onclick=()=>setZoom(scale*1.25);document.getElementById('out').onclick=()=>setZoom(scale/1.25);
function focusAt(x,y){setZoom(1.15,false);viewport.scrollTo({left:Math.max(0,(x+70)*scale-48),top:Math.max(0,(y+200)*scale-35),behavior:'smooth'})}
const modules=document.getElementById('module');for(const g of model.groups){const o=document.createElement('option');o.value=g.group;o.textContent=g.title;modules.append(o)}modules.onchange=()=>{const g=model.groups.find(g=>g.group===modules.value);if(g)focusAt(g.x,g.y)};
for(const t of [...model.tables].sort((a,b)=>a.name.localeCompare(b.name))){const o=document.createElement('option');o.value=t.name;document.getElementById('tables').append(o)}
const search=document.getElementById('table');search.onchange=()=>{const t=model.tables.find(t=>t.name===search.value.trim());if(t)focusAt(t.x,t.y-80)};
viewport.addEventListener('keydown',e=>{if(e.key==='+'||e.key==='='){e.preventDefault();setZoom(scale*1.25)}if(e.key==='-'){e.preventDefault();setZoom(scale/1.25)}if(e.key==='0'){e.preventDefault();fit()}});
window.addEventListener('resize',()=>{if(scale<.5)fit()});fit();
</script></body></html>'''
    (OUT / "index.html").write_text(template.replace("__DATA__", json.dumps(model, ensure_ascii=False)).replace("__URL__", url), encoding="utf-8")
    print("Built offline database viewer: docs/dbdiagram/index.html")


if __name__ == "__main__":
    main()
