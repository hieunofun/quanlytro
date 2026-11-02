// Verify the file with the official DBML parser against the live catalog snapshot.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const crypto=require('node:crypto');
const root=path.resolve(__dirname,'..');
const {Parser}=require(path.join(root,'.runtime/dbml-tools/node_modules/@dbml/core'));
const source=fs.readFileSync(path.join(root,'docs/quanlytro.dbml'),'utf8');
const meta=JSON.parse(fs.readFileSync(path.join(root,'docs/dbdiagram/schema-snapshot.json'),'utf8'));
const db=new Parser().parse(source,'dbmlv2');
const tables=db.schemas.flatMap(s=>s.tables),refs=db.schemas.flatMap(s=>s.refs);
const allConstraints=meta.constraints;
const normalizeType=s=>s.replaceAll('character varying','varchar').replaceAll('timestamp with time zone','timestamptz').replaceAll(' ','');
assert.equal(tables.length,49);
assert.deepEqual(tables.map(t=>t.name).sort(),[...new Set(meta.columns.map(c=>c.table_name))].sort());
for(const table of tables){
 const cols=meta.columns.filter(c=>c.table_name===table.name);
 const constraints=allConstraints.filter(c=>c.table_name===table.name);
 assert.deepEqual(table.fields.map(c=>c.name),cols.map(c=>c.name),table.name+' column order');
 for(const [i,field] of table.fields.entries()){
  const col=cols[i],pk=constraints.find(c=>c.kind==='p');
  assert.equal(normalizeType(field.type.type_name),normalizeType(col.type),table.name+'.'+field.name+' type');
  assert.equal(Boolean(field.not_null),col.not_null,table.name+'.'+field.name+' nullability');
  assert.equal(Boolean(field.pk),pk.columns.length===1&&pk.columns[0]===field.name);
  assert.equal(Boolean(field.increment),Boolean(col.identity)||(col.default_value||'').startsWith('nextval('));
  const unique=constraints.some(c=>c.kind==='u'&&c.columns.length===1&&c.columns[0]===field.name);
  assert.equal(Boolean(field.unique),unique,table.name+'.'+field.name+' unique');
  if(col.default_value!==null&&!field.increment){
   const sql=col.default_value;let expected;
   if(/^[-+]?\d+(\.\d+)?$/.test(sql))expected={type:'number',value:Number(sql)};
   else if(/^(true|false)$/i.test(sql))expected={type:'boolean',value:sql.toLowerCase()};
   else {const literal=sql.match(/^'((?:[^']|'')*)'::(?:character varying|text)$/);expected=literal?{type:'string',value:literal[1].replaceAll("''","'")}:{type:'expression',value:sql}}
   assert.deepEqual(field.dbdefault,expected,table.name+'.'+field.name+' default');
  }else assert.ok(!field.dbdefault,table.name+'.'+field.name+' unexpected default');
 }
 const checks=constraints.filter(c=>c.kind==='c');
 assert.equal(table.checks.length,checks.length);
 for(const check of checks){
  const actual=table.checks.find(c=>c.name===check.name);
  assert.ok(actual,check.name);
  assert.equal(actual.expression,check.check_expression,check.name+' expression');
 }
 for(const constraint of constraints.filter(c=>c.kind==='x'))assert.ok(table.note.includes(constraint.definition),constraint.name);
 for(const index of meta.indexes.filter(i=>i.table_name===table.name&&i.predicate))assert.ok(table.note.includes(index.definition),index.name);
 for(const index of meta.indexes.filter(i=>i.table_name===table.name&&!i.predicate&&!i.is_primary&&i.constraint_kind!=='x'&&!(i.constraint_kind==='u'&&i.columns.length===1))){
  const actual=table.indexes.find(i=>i.name===index.name);
  assert.ok(actual,index.name);
  assert.equal(Boolean(actual.unique),index.is_unique,index.name+' uniqueness');
  assert.deepEqual(actual.columns.map(c=>c.value),index.columns,index.name+' columns');
 }
}
const actions={a:'no action',r:'restrict',c:'cascade',n:'set null',d:'set default'};
const fks=allConstraints.filter(c=>c.kind==='f');assert.equal(refs.length,fks.length);
for(const fk of fks){
 const ref=refs.find(r=>r.name===fk.name);assert.ok(ref,fk.name);
 const ends=ref.endpoints;
 // The second endpoint is the FK side for a one-to-one relation in DBML.
 const oneToOne=ends.every(e=>['1','0..1'].includes(e.relation));
 const child=oneToOne?ends[1]:ends.find(e=>e.relation==='*');
 const parent=oneToOne?ends[0]:ends.find(e=>['1','0..1'].includes(e.relation));
 assert.deepEqual({table:child.tableName,columns:child.fieldNames},{table:fk.table_name,columns:fk.columns},fk.name+' child');
 assert.deepEqual({table:parent.tableName,columns:parent.fieldNames},{table:fk.parent_table,columns:fk.parent_columns},fk.name+' parent');
 const keys=allConstraints.filter(c=>c.table_name===fk.table_name&&['p','u'].includes(c.kind)).map(c=>c.columns);
 keys.push(...meta.indexes.filter(i=>i.table_name===fk.table_name&&i.is_unique&&!i.predicate&&i.columns.every(Boolean)).map(i=>i.columns));
 assert.equal(oneToOne,keys.some(k=>k.every(n=>fk.columns.includes(n))),fk.name+' cardinality');
 assert.equal(ref.onDelete,actions[fk.on_delete]);assert.equal(ref.onUpdate,actions[fk.on_update]);
 const line=source.split('\n').find(l=>l.startsWith('Ref '+fk.name+':'));
 const nullable=meta.columns.some(c=>c.table_name===fk.table_name&&fk.columns.includes(c.name)&&!c.not_null);
 assert.equal(line.includes('?'),nullable,fk.name+' optionality');
}
const groups=db.schemas.flatMap(s=>s.tableGroups),manifest=JSON.parse(fs.readFileSync(path.join(root,'docs/schema49-tables.json'),'utf8'));
assert.equal(groups.length,8);
for(const group of groups)assert.deepEqual(group.export().tables.map(t=>t.tableName).sort(),manifest.filter(t=>t.group===group.name).map(t=>t.name).sort(),group.name);
assert.equal(db.schemas.flatMap(s=>s.enums).length,0,'No invented ENUM types');
const report={validated_on:meta.source.validated_on,source:'PostgreSQL public schema, hostel_review49, migrations V1–V13',parser_version:require(path.join(root,'.runtime/dbml-tools/node_modules/@dbml/core/package.json')).version,tables:tables.length,columns:tables.reduce((n,t)=>n+t.fields.length,0),primary_keys:allConstraints.filter(c=>c.kind==='p').length,foreign_keys:refs.length,checks:tables.reduce((n,t)=>n+t.checks.length,0),exclusion_constraints_documented:allConstraints.filter(c=>c.kind==='x').length,partial_indexes_documented:meta.indexes.filter(i=>i.predicate).length,table_groups:groups.length,sql_views_listed_not_drawn:meta.views.map(v=>v.name),checks_performed:['table and column sets; physical column order','PostgreSQL types; nullability; primary and unique keys; identity','all default values and expressions; CHECK expressions; EXCLUDE and partial-index SQL notes','index columns and uniqueness','all foreign-key endpoints, cardinality, optionality, ON DELETE and ON UPDATE','group membership; no fabricated enums'],excluded:['flyway_schema_history infrastructure table','SQL views are listed in the project note, not represented as base tables','triggers and functions remain in PostgreSQL migrations; DBML is not a migration replacement'],dbml_sha256:crypto.createHash('sha256').update(source).digest('hex'),all_passed:true};
fs.writeFileSync(path.join(root,'docs/dbdiagram/validation.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
