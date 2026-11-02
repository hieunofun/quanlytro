const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {chromium}=require('../frontend/node_modules/playwright');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/backend-review');
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1360,height:820},deviceScaleFactor:1});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(pathToFileURL(path.join(out,'backend-he-thong-nha-tro.html')).href);
  await page.evaluate(()=>document.fonts.ready);
  const layout=await page.locator('.slide').evaluateAll(slides=>slides.map(s=>{
   const footer=s.querySelector('footer').getBoundingClientRect();
   const overflowing=[...s.children].filter(e=>e.tagName!=='FOOTER').filter(e=>e.getBoundingClientRect().bottom>footer.top-8||e.scrollWidth>e.clientWidth+2).map(e=>e.tagName);
   return {slide:s.id,overflowing};
  }));
  if(layout.some(s=>s.overflowing.length))throw new Error('Slide content overflow: '+JSON.stringify(layout.filter(s=>s.overflowing.length)));
  const images=path.join(root,'.runtime/backend-review-slides');fs.mkdirSync(images,{recursive:true});
  for(let i=0;i<layout.length;i++)await page.locator('.slide').nth(i).screenshot({path:path.join(images,`slide-${String(i+1).padStart(2,'0')}.png`)});
  await page.pdf({path:path.join(out,'backend-he-thong-nha-tro.pdf'),preferCSSPageSize:true,printBackground:true});
  await page.getByRole('button',{name:'Trình chiếu',exact:true}).click();
  await page.keyboard.press('ArrowRight');
  if(await page.locator('.slide.active').getAttribute('id')!=='s2')throw new Error('Slide navigation failed');
  if(errors.length)throw new Error(errors.join('\n'));
  const result={validated_on:'2026-11-01',slides:layout.length,html_layout:'passed',html_javascript:'passed',pdf_generated:true,speaker_notes:false,screenshots:'.runtime/backend-review-slides'};
  fs.writeFileSync(path.join(out,'slide-validation.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result));
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exit(1)});
