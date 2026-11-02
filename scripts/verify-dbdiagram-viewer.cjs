const fs=require('node:fs');
const path=require('node:path');
const zlib=require('node:zlib');
const assert=require('node:assert/strict');
const {pathToFileURL}=require('node:url');
const {chromium}=require('../frontend/node_modules/playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/dbdiagram');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1500,height:1040}}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto(pathToFileURL(path.join(out,'index.html')).href,{waitUntil:'load'});
  const poster=page.frameLocator('#diagram');await poster.locator('.diagram').waitFor();
  assert.equal(await poster.locator('.diagram').evaluate(img=>img.complete&&img.naturalWidth>0),true);
  assert.equal(await poster.locator('.label').count(),8);
  assert.equal(await page.locator('#tables option').count(),49);
  const initial=await page.locator('#zoom').textContent();
  await page.getByRole('button',{name:'Phóng to',exact:true}).click();
  assert.notEqual(await page.locator('#zoom').textContent(),initial);
  await page.locator('#table').fill('fund_entries');await page.locator('#table').dispatchEvent('change');
  assert.equal(await page.locator('#zoom').textContent(),'115%');
  await page.waitForFunction(()=>document.getElementById('viewport').scrollTop>500);
  await page.locator('#module').selectOption('TaiKhoan');assert.equal(await page.locator('#zoom').textContent(),'115%');
  await page.getByRole('button',{name:'Vừa màn hình',exact:true}).click();
  const layout=JSON.parse(fs.readFileSync(path.join(out,'layout.json'),'utf8'));
  const labels=await poster.locator('.label').evaluateAll(es=>es.map(e=>({top:e.offsetTop,bottom:e.offsetTop+e.offsetHeight,left:e.offsetLeft,right:e.offsetLeft+e.offsetWidth})));
  // Export trims the empty initial top margin and adds a 10px shadow margin.
  const minY=Math.min(...layout.tablePositions.map(t=>t.y));
  const headingsOverlap=labels.some((label,i)=>{const group=layout.groups[i];const firstTableY=Math.min(...layout.tablePositions.filter(t=>group.group===JSON.parse(fs.readFileSync(path.join(root,'docs/schema49-tables.json'),'utf8')).find(m=>m.name===t.name).group).map(t=>t.y));return label.bottom>200+80+10+firstTableY-minY});
  assert.equal(headingsOverlap,false,'Module title covers table header');
  const online=await page.getByRole('link',{name:'Mở dbdiagram.io ↗'}).getAttribute('href');
  const encoded=new URL(online).hash.slice(3),compressed=decodeURIComponent(encoded);
  assert.ok(compressed.startsWith('pako:'));
  assert.equal(zlib.inflateSync(Buffer.from(compressed.slice(5),'base64')).toString('utf8'),fs.readFileSync(path.join(root,'docs/quanlytro.dbml'),'utf8'));
  assert.equal(errors.length,0);
  await page.screenshot({path:path.join(out,'viewer-preview.png')});
  const report=JSON.parse(fs.readFileSync(path.join(out,'render-validation.json'),'utf8'));
  Object.assign(report,{offline_viewer:'passed',viewer_zoom_and_search:'passed',module_titles_do_not_cover_tables:true,url_embeds_verified_dbml:true});
  fs.writeFileSync(path.join(out,'render-validation.json'),JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({offline_viewer:'passed',zoom:'passed',table_search:'passed',module_navigation:'passed',module_title_overlaps:0,embed_dbml_matches:true,js_errors:errors}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
