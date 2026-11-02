// Render the verified DBML with dbdiagram.io itself, without an account or publish.
const fs=require('node:fs');
const path=require('node:path');
const zlib=require('node:zlib');
const {pathToFileURL}=require('node:url');
const assert=require('node:assert/strict');
const {chromium}=require('../frontend/node_modules/playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/dbdiagram');
const source=fs.readFileSync(path.join(root,'docs/quanlytro.dbml'),'utf8');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'docs/schema49-tables.json'),'utf8'));
const report=JSON.parse(fs.readFileSync(path.join(out,'validation.json'),'utf8'));
const url='https://dbdiagram.io/embed#c='+encodeURIComponent('pako:'+zlib.deflateSync(source).toString('base64'));
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
function pack(tables,cols,x,y,gapX=90,gapY=65){
 const widths=Array.from({length:cols},(_,i)=>Math.max(...tables.filter((_,n)=>n%cols===i).map(t=>t.width)));
 const xs=widths.map((_,i)=>x+widths.slice(0,i).reduce((n,w)=>n+w+gapX,0));
 const ys=Array(cols).fill(y),positions=[];
 for(const [i,t] of tables.entries()){const c=i%cols;positions.push({name:t.name,schemaName:'public',x:xs[c],y:ys[c]});ys[c]+=t.height+gapY}
 return {positions,width:widths.reduce((n,w)=>n+w,0)+gapX*(cols-1),height:Math.max(...ys)-y-gapY};
}
(async()=>{
 assert.equal(report.all_passed,true);
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1800,height:1200}}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto(url,{waitUntil:'domcontentloaded',timeout:60000});
  await page.waitForFunction(()=>window.Konva?.stages?.[0]?.isVisible(),{},{timeout:60000});
  const initial=await page.evaluate(()=>{
   const seen=new Set();function find(v){if(!v||typeof v!=='object'||seen.has(v))return;seen.add(v);if(v.component){const c=v.component;if(c.exposed?.getAllVizData)return c;const r=find(c.subTree);if(r)return r}if(Array.isArray(v.children))for(const v2 of v.children){const r=find(v2);if(r)return r}}
   const c=find(document.querySelector('#app')._vnode);if(!c)throw Error('dbdiagram renderer component unavailable');
   window.reviewDiagram=c;
   const stage=window.Konva.stages[0];
   const positions=c.exposed.getAllTablePositions();
   const shapes=stage.find(n=>/^table-shadow-table-/.test(n.id()));
   // Shadow rectangles have the exact visible table dimensions and positions.
   const dimensions=positions.map(p=>{const s=shapes.find(s=>Math.abs(s.x()-p.x)<.01&&Math.abs(s.y()-p.y)<.01);if(!s)throw Error('Table dimensions unavailable: '+p.name);return {...p,width:s.width(),height:s.height()}});
   const db=c.props.database;
   return {dimensions,tables:Object.values(db.tables).map(t=>({name:t.name||t.tableName})),fields:Object.keys(db.fields).length,refs:Object.keys(db.refs).length};
  });
  assert.equal(initial.dimensions.length,49);assert.equal(initial.fields,455);assert.equal(initial.refs,151);
  const dimensions=new Map(initial.dimensions.map(t=>[t.name,t]));
  const groupOrder=['TaiKhoan','NhaTro','SuCo','CuTru','DienNuoc','TaiChinh','ThongBao','Tep'];
  const groupNames={TaiKhoan:'01 · Tài khoản và phân quyền',NhaTro:'02 · Nhà trọ và tài sản',CuTru:'03 · Hợp đồng và cư trú',DienNuoc:'04 · Dịch vụ và điện nước',TaiChinh:'05 · Hóa đơn và dòng tiền',SuCo:'06 · Sự cố và bảo trì',ThongBao:'07 · Thông báo',Tep:'08 · Tệp đính kèm'};
  const sizes={};
  for(const group of groupOrder){const tables=manifest.filter(t=>t.group===group).map(t=>dimensions.get(t.name));sizes[group]=pack(tables,group==='ThongBao'||group==='Tep'?2:3,0,0)}
  const gap=220,groupHeader=80;
  // Three aligned lanes; connected contract / meter / finance modules share a row.
  const col1=Math.max(sizes.TaiKhoan.width,sizes.CuTru.width,sizes.ThongBao.width),col2=Math.max(sizes.NhaTro.width,sizes.DienNuoc.width,sizes.Tep.width);
  const noticeY=sizes.TaiKhoan.height+groupHeader+120,fileY=sizes.SuCo.height+groupHeader+120;
  const row1=Math.max(noticeY+sizes.ThongBao.height+groupHeader,fileY+sizes.Tep.height+groupHeader,sizes.NhaTro.height+groupHeader)+gap;
  const origins={TaiKhoan:[0,0],NhaTro:[col1+gap,0],SuCo:[col1+col2+gap*2,0],CuTru:[0,row1],DienNuoc:[col1+gap,row1],TaiChinh:[col1+col2+gap*2,row1],ThongBao:[0,noticeY],Tep:[col1+col2+gap*2,fileY]};
  const positions=[],groups=[];
  for(const group of groupOrder){const [x,y]=origins[group];const result=pack(manifest.filter(t=>t.group===group).map(t=>dimensions.get(t.name)),group==='ThongBao'||group==='Tep'?2:3,x,y+groupHeader);positions.push(...result.positions);groups.push({group,title:groupNames[group],x,y,width:result.width,height:result.height+groupHeader})}
  // No tables overlap, even when text metrics change on a later site release.
  for(let i=0;i<positions.length;i++)for(let j=i+1;j<positions.length;j++){const a=positions[i],b=positions[j],ad=dimensions.get(a.name),bd=dimensions.get(b.name);assert.ok(!(a.x<b.x+bd.width&&a.x+ad.width>b.x&&a.y<b.y+bd.height&&a.y+ad.height>b.y),'Overlapping tables '+a.name+' '+b.name)}
  const result=await page.evaluate(async positions=>{
   const c=window.reviewDiagram;await c.exposed.applyDiagramViz(positions,[],undefined,[],[]);
   const svg=await c.exposed.exportSVG('SVG');
   return {svg:svg[0],positions:c.exposed.getAllTablePositions(),viz:c.exposed.getAllVizData()};
  },positions);
  assert.equal(result.positions.length,49);
  for(const p of positions){const actual=result.positions.find(a=>a.name===p.name);assert.ok(Math.abs(actual.x-p.x)<.01&&Math.abs(actual.y-p.y)<.01,p.name+' layout')}
  const raw=result.svg.content;
  // The original official export remains untouched and is separately downloadable.
  fs.writeFileSync(path.join(out,'quanlytro-dbdiagram.svg'),raw);
  fs.writeFileSync(path.join(out,'layout.json'),JSON.stringify({renderer:'dbdiagram.io',tablePositions:positions,groups,diagramViz:result.viz},null,2)+'\n');
  fs.writeFileSync(path.join(out,'dbdiagram-link.txt'),url+'\n');
  await page.screenshot({path:path.join(out,'dbdiagram-preview.png')});
  const preview=await browser.newPage({viewport:{width:1500,height:1000}});
  const rawWidth=Number(raw.match(/<svg[^>]*\bwidth="([\d.]+)"/)[1]),rawHeight=Number(raw.match(/<svg[^>]*\bheight="([\d.]+)"/)[1]);
  // Cover/legend sit outside the renderer export. No database entities are added.
  const titleHeight=200,footerHeight=210,margin=70,width=Math.ceil(rawWidth+margin*2),height=Math.ceil(rawHeight+titleHeight+footerHeight+groupHeader);
  const src='quanlytro-dbdiagram.svg';
  const labels=groups.map(g=>`<div class="label" style="left:${g.x+margin+12}px;top:${g.y+titleHeight+14}px">${g.title}<small>${manifest.filter(t=>t.group===g.group).length} bảng</small></div>`).join('');
  const poster=`<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>CSDL quản lý nhà trọ · V13</title><style>@page{size:${width}px ${height}px;margin:0}*{box-sizing:border-box}body{margin:0;background:#fff;font-family:Arial,sans-serif;color:#0f172a}.poster{width:${width}px;height:${height}px;position:relative;overflow:hidden}header{position:absolute;top:44px;left:${margin}px;right:${margin}px;border-bottom:2px solid #e2e8f0;padding-bottom:24px}h1{font-size:44px;margin:0 0 16px}header p{font-size:23px;margin:0;color:#475569}.diagram{position:absolute;left:${margin}px;top:${titleHeight+groupHeader}px;width:${rawWidth}px;height:${rawHeight}px}.label{position:absolute;background:#fff;padding:8px 14px;border-left:5px solid #315775;font-size:24px;font-weight:700;white-space:nowrap}.label small{font-weight:400;color:#64748b;font-size:18px;margin-left:18px}footer{position:absolute;bottom:42px;left:${margin}px;right:${margin}px;border-top:2px solid #e2e8f0;padding-top:22px;font-size:22px;color:#475569;line-height:1.65}footer p{margin:0}</style></head><body><main class="poster"><header><h1>CƠ SỞ DỮ LIỆU · QUẢN LÝ NHÀ TRỌ</h1><p>PostgreSQL · model49 / V13 &nbsp; | &nbsp; 49 bảng · 455 cột · 151 khóa ngoại &nbsp; | &nbsp; Đối chiếu database ngày ${report.validated_on}</p></header><img class="diagram" alt="Sơ đồ đầy đủ do dbdiagram.io render từ DBML đã kiểm chứng" src="${src}">${labels}<footer><p><b>Khóa và quan hệ:</b> ký hiệu khóa = PK · nhánh chân quạ = nhiều · vòng tròn = phía quan hệ tùy chọn. Tên bảng, cột và kiểu dữ liệu giữ theo PostgreSQL.</p><p><b>Phạm vi:</b> 49 bảng nghiệp vụ; không đưa flyway_schema_history và 10 SQL view vào thành bảng. 132 CHECK, 7 EXCLUDE và chỉ mục có điều kiện được lưu trong DBML/ghi chú.</p><p><b>Nguồn:</b> pg_catalog của hostel_review49 sau V1–V13; sơ đồ xuất bằng dbdiagram.io. Tiêu đề nhóm và chú giải được đặt bên ngoài bản xuất.</p></footer></main></body></html>`;
  fs.writeFileSync(path.join(out,'poster.html'),poster);
  await preview.goto(pathToFileURL(path.join(out,'poster.html')).href,{waitUntil:'load'});await preview.evaluate(()=>document.fonts.ready);
  await preview.pdf({path:path.join(out,'quanlytro-database.pdf'),preferCSSPageSize:true,printBackground:true,timeout:60000});
  // Full resolution: text remains readable when enlarged. Small preview is separate.
  await preview.setViewportSize({width,height});
  await preview.screenshot({path:path.join(out,'quanlytro-database.png'),timeout:60000});
  const validation={validated_on:report.validated_on,renderer_url:'https://dbdiagram.io/embed',renderer:'Actual dbdiagram.io web renderer and its SVG exporter',tables:49,columns:initial.fields,foreign_keys:initial.refs,layout_positions_verified:true,table_overlaps:0,js_errors:errors,original_svg:'quanlytro-dbdiagram.svg',poster_pdf:'quanlytro-database.pdf',poster_png:'quanlytro-database.png',poster_changes:'Title, module labels and legend outside the official export; no schema changes',dbdiagram_account_used:false,server_diagram_published:false,embed_link_retains_custom_positions:false,width,height,all_passed:errors.length===0};
  assert.equal(validation.all_passed,true);fs.writeFileSync(path.join(out,'render-validation.json'),JSON.stringify(validation,null,2)+'\n');
  console.log(JSON.stringify(validation));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
