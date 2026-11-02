const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/dbdiagram/modules');
const {Parser}=require('../.runtime/dbml-tools/node_modules/@dbml/core');
const parse=s=>new Parser().parse(s,'dbmlv2');
const masterText=fs.readFileSync(path.join(root,'docs/quanlytro.dbml'),'utf8'),master=parse(masterText),manifest=JSON.parse(fs.readFileSync(path.join(out,'manifest.json'),'utf8'));
const tables=master.schemas.flatMap(s=>s.tables),refs=master.schemas.flatMap(s=>s.refs),owned=[],edges=[],reports=[];
assert.equal(crypto.createHash('sha256').update(masterText).digest('hex'),manifest.canonical_dbml_sha256);
assert.equal(manifest.modules.length,8);
for(const m of manifest.modules){
 const text=fs.readFileSync(path.join(out,m.slug+'.dbml'),'utf8'),db=parse(text),local=db.schemas.flatMap(s=>s.tables),localRefs=db.schemas.flatMap(s=>s.refs);
 assert.equal(crypto.createHash('sha256').update(text).digest('hex'),m.dbml_sha256);
 assert.deepEqual(local.map(t=>t.name).sort(),[...m.owned_tables,...m.context_tables].sort());
 for(const t of local){const original=tables.find(x=>x.name===t.name);assert.ok(original);
  if(m.owned_tables.includes(t.name)){assert.deepEqual(t.export(),original.export(),m.slug+': complete owned table '+t.name);owned.push(t.name)}
  else {assert.ok(t.note.includes('THAM CHIẾU RÚT GỌN'));assert.deepEqual(t.fields.map(f=>f.name),m.columns_shown[t.name]);for(const field of t.fields)assert.deepEqual(field.export(),original.fields.find(f=>f.name===field.name).export(),t.name+'.'+field.name)}
 }
 assert.deepEqual(localRefs.map(r=>r.name).sort(),m.foreign_keys.slice().sort());
 for(const ref of localRefs){assert.deepEqual(ref.export(),refs.find(r=>r.name===ref.name).export(),ref.name);edges.push(ref.name)}
 reports.push({slug:m.slug,owned_tables:m.owned_tables.length,owned_columns:m.owned_columns,reference_copies:m.context_tables.length,foreign_keys:localRefs.length});
}
assert.equal(owned.length,49);assert.equal(new Set(owned).size,49);assert.equal(edges.length,151);assert.equal(new Set(edges).size,151);
assert.equal(reports.reduce((n,r)=>n+r.owned_columns,0),455);
const result={schema:'V13-model49',schema_verified_on:manifest.schema_verified_on,canonical_dbml_sha256:manifest.canonical_dbml_sha256,modules:reports,owned_tables_once:49,owned_columns_once:455,foreign_key_sources_once:151,owned_definitions_match_master:true,reference_columns_match_master:true,all_foreign_keys_match_master:true,all_passed:true};
fs.writeFileSync(path.join(out,'validation.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result));
