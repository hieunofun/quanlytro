// npm install --prefix .runtime/dbml-tools @viz-js/viz @dbml/core
const fs = require('node:fs');
const path = require('node:path');
const {instance} = require('../.runtime/dbml-tools/node_modules/@viz-js/viz');
const {Parser} = require('../.runtime/dbml-tools/node_modules/@dbml/core');
const {chromium} = require('../frontend/node_modules/playwright');
const root = path.resolve(__dirname,'..');
const out = path.join(root,'docs/diagrams');
(async()=>{
  const metadata=JSON.parse(fs.readFileSync(path.join(root,'docs/design-schema.json'),'utf8'));
  const model=new Parser().parse(fs.readFileSync(path.join(root,'docs/quanlytro.dbml'),'utf8'),'dbmlv2');
  const tables=model.schemas.flatMap(s=>s.tables),refs=model.schemas.flatMap(s=>s.refs);
  if(tables.length!==17 || refs.length!==28 || tables.reduce((n,t)=>n+t.fields.length,0)!==138) throw new Error('DBML differs from verified schema');
  for(const t of tables){
    const cols=metadata.columns.filter(c=>c.table_name===t.name);
    if(JSON.stringify(t.fields.map(f=>f.name))!==JSON.stringify(cols.map(c=>c.name))) throw new Error(t.name);
    if(t.checks.length!==metadata.constraints.filter(c=>c.table_name===t.name && c.kind==='c').length) throw new Error('Missing checks '+t.name);
  }
  const viz=await instance();
  const browser=await chromium.launch({channel:'chrome',headless:true});
  try{
    for(const name of ['quanlytro-dbdiagram','erd-cu-tru','erd-dien-nuoc','erd-tai-chinh','erd-su-co']){
      let svg=viz.renderString(fs.readFileSync(path.join(out,name+'.dot'),'utf8'),{format:'svg',engine:'dot'});
      fs.writeFileSync(path.join(out,name+'.svg'),svg);
      const page=await browser.newPage({viewport:{width:1600,height:1000},deviceScaleFactor:1});
      await page.goto('file:///'+path.join(out,name+'.svg').replaceAll('\\','/'));
      await page.locator('svg').screenshot({path:path.join(out,name+'.png'),timeout:30000});
      if(name==='quanlytro-dbdiagram'){
        const dimensions=await page.locator('svg').evaluate(el=>({width:el.getBoundingClientRect().width,height:el.getBoundingClientRect().height}));
        await page.pdf({path:path.join(out,name+'.pdf'),width:dimensions.width+'px',height:dimensions.height+'px',margin:{top:0,right:0,bottom:0,left:0},printBackground:true});
      }
      await page.close();
      console.log(name+': SVG/PNG'+(name==='quanlytro-dbdiagram'?'/PDF':''));
    }
  }finally{await browser.close();}
  console.log('DBML validated: 17 tables, 138 columns, 28 foreign keys, 55 checks.');
})().catch(error=>{console.error(error);process.exit(1);});
