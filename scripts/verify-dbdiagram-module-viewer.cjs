const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {pathToFileURL}=require('node:url');
const {chromium}=require('../frontend/node_modules/playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/dbdiagram/modules');
(async()=>{
 const manifest=JSON.parse(fs.readFileSync(path.join(out,'manifest.json'),'utf8')),browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1600,height:1000}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(pathToFileURL(path.join(out,'index.html')).href,{waitUntil:'load'});
  assert.equal(await page.locator('#modules button').count(),8);
  for(const m of manifest.modules){
   await page.locator(`#modules button[data-slug="${m.slug}"]`).click();
   await page.frameLocator('#diagram').locator('.diagram').waitFor();
   assert.equal(await page.frameLocator('#diagram').locator('.diagram').evaluate(img=>img.complete&&img.naturalWidth>0),true);
   assert.ok((await page.locator('#title').textContent()).includes(m.title));
   assert.equal(await page.locator('#stats').textContent(),`${m.owned_tables.length} bảng chính · ${m.context_tables.length} tham chiếu rút gọn · ${m.foreign_keys.length} FK`);
   assert.equal(await page.locator('#pdf').getAttribute('href'),m.slug+'.pdf');
   assert.equal(await page.locator('#dbml').getAttribute('href'),m.slug+'.dbml');
   assert.equal(await page.locator('#modules button[aria-current="true"]').count(),1);
   const fit=await page.evaluate(()=>{const a=document.getElementById('viewport'),p=document.getElementById('paper');return {areaWidth:a.clientWidth,areaHeight:a.clientHeight,paperWidth:p.getBoundingClientRect().width,paperHeight:p.getBoundingClientRect().height}});
   assert.ok(fit.paperWidth<=fit.areaWidth-35&&fit.paperHeight<=fit.areaHeight-35,m.slug+' fit layout');
  }
  const before=await page.locator('#zoom').textContent();await page.getByRole('button',{name:'Phóng to',exact:true}).click();assert.notEqual(await page.locator('#zoom').textContent(),before);
  await page.getByRole('button',{name:'100%',exact:true}).click();assert.equal(await page.locator('#zoom').textContent(),'100%');
  await page.locator('#modules button[data-slug="05-hoa-don-dong-tien"]').click();await page.frameLocator('#diagram').locator('h1').filter({hasText:'Hóa đơn và dòng tiền'}).waitFor();
  await page.screenshot({path:path.join(out,'viewer-preview.png')});
  assert.equal(errors.length,0);
  const report=JSON.parse(fs.readFileSync(path.join(out,'render-validation.json'),'utf8'));Object.assign(report,{offline_module_viewer:'passed',eight_modules_navigation:'passed',viewer_counts_and_file_links:'passed',viewer_zoom:'passed',viewer_js_errors:errors});
  fs.writeFileSync(path.join(out,'render-validation.json'),JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({modules:8,offline_navigation:'passed',counts_and_links:'passed',zoom:'passed',js_errors:errors}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
