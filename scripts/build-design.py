"""Build DBML and ERD sources from the PostgreSQL metadata verified by verify-design.py."""
from html import escape
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
GROUPS = [
 ('TaiKhoan',['users','employee_assignments']),
 ('NhaTro',['properties','rooms']),
 ('CuTru',['tenant_profiles','contracts','contract_members']),
 ('DienNuoc',['service_items','service_price_configs','meter_readings','consumption_alerts']),
 ('TaiChinh',['invoices','invoice_items','payment_transactions','checkout_records']),
 ('SuCo',['maintenance_requests','maintenance_images']),
]


def quoted(value):
    return "'"+str(value).replace('\\','\\\\').replace("'","\\'").replace('\n','\\n')+"'"


def default(value):
    if re.fullmatch(r'[-+]?\d+(\.\d+)?|true|false',value,re.I): return value.lower()
    literal=re.fullmatch(r"'((?:[^']|'')*)'::(?:character varying|text)",value)
    return quoted(literal[1].replace("''","'")) if literal else '`'+value+'`'


def main():
    source=ROOT/'.runtime/schema-review/metadata.json'
    metadata=json.loads(source.read_text(encoding='utf-8'))
    (ROOT/'docs/design-schema.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    tables=[table for _,names in GROUPS for table in names]
    assert set(tables)=={c['table_name'] for c in metadata['columns']}
    lines=['// Thiết kế mục tiêu bước 1–2. Nguồn: quanlytro.sql + database-rules.sql.',
      '// 17 bảng; các giá trị tính toán nằm trong 6 view, không tạo bảng trùng dữ kiện.',
      "Project QuanLyNhaTro {", "  database_type: 'PostgreSQL'",'}','']
    for _, names in GROUPS:
        for name in names:
            cols=[c for c in metadata['columns'] if c['table_name']==name]
            cons=[c for c in metadata['constraints'] if c['table_name']==name]
            pk=next(c['columns'] for c in cons if c['kind']=='p')
            unique={c['columns'][0] for c in cons if c['kind']=='u' and len(c['columns'])==1}
            lines.append(f'Table {name} [headercolor: #315775] {{')
            for col in cols:
                flags=[]
                if pk==[col['name']]: flags.append('pk')
                if col['not_null']: flags.append('not null')
                if col['name'] in unique: flags.append('unique')
                if col['identity']: flags.append('increment')
                elif col['default_value'] is not None: flags.append('default: '+default(col['default_value']))
                if col['note']: flags.append('note: '+quoted(col['note']))
                typ=col['type'].replace('character varying','varchar').replace('timestamp with time zone','timestamptz')
                if ' ' in typ: typ='"'+typ+'"'
                lines.append('  '+col['name']+' '+typ+(' ['+', '.join(flags)+']' if flags else ''))
            indexes=[]; notes=[]
            for idx in metadata['indexes']:
                if idx['table_name']!=name or idx['is_primary']: continue
                if idx['constraint_kind']=='u' and len(idx['columns'])==1: continue
                if idx['predicate'] or idx['constraint_kind']=='x': notes.append(idx['definition']); continue
                terms=idx['columns']; flags=['name: '+quoted(idx['name'])]
                if idx['is_unique']: flags.append('unique')
                if idx['access_method'] in ('btree','hash'): flags.append('type: '+idx['access_method'])
                indexes.append('    '+(terms[0] if len(terms)==1 else '('+', '.join(terms)+')')+' ['+', '.join(flags)+']')
            if indexes: lines+=['','  indexes {',*indexes,'  }']
            checks=[c for c in cons if c['kind']=='c']
            if checks:
                lines+=['','  checks {']+['    `'+c['check_expression']+'` [name: '+quoted(c['name'])+']' for c in checks]+['  }']
            if notes: lines+=['','  Note: '+quoted('PostgreSQL: '+'; '.join(notes))]
            lines+=['}','']
    actions={'a':'no action','r':'restrict','c':'cascade','n':'set null','d':'set default'}
    for fk in (c for c in metadata['constraints'] if c['kind']=='f'):
        assert len(fk['columns'])==len(fk['parent_columns'])==1
        child=fk['table_name']+'.'+fk['columns'][0]; parent=fk['parent_table']+'.'+fk['parent_columns'][0]
        unique=any(c['kind'] in ('p','u') and c['table_name']==fk['table_name'] and c['columns']==fk['columns'] for c in metadata['constraints'])
        relation=parent+' - '+child if unique else child+' > '+parent
        lines.append('Ref '+fk['name']+': '+relation+' [delete: '+actions[fk['on_delete']]+']')
    for group,names in GROUPS: lines+=['',f'TableGroup {group} {{',*['  '+n for n in names],'}']
    (ROOT/'docs/quanlytro.dbml').write_text('\n'.join(lines)+'\n',encoding='utf-8')

    # Full field-level ERD; detail figures keep the same table/foreign-key source.
    variants={'quanlytro-dbdiagram':tables,
      'erd-cu-tru':['users','tenant_profiles','properties','rooms','contracts','contract_members','employee_assignments'],
      'erd-dien-nuoc':['properties','service_items','service_price_configs','contracts','meter_readings','consumption_alerts'],
      'erd-tai-chinh':['contracts','meter_readings','invoices','invoice_items','payment_transactions','checkout_records'],
      'erd-su-co':['users','contracts','maintenance_requests','maintenance_images']}
    for filename, selected in variants.items():
        full=filename=='quanlytro-dbdiagram'
        dot=['digraph ERD {','graph [rankdir=LR, bgcolor="white", pad="0.25", nodesep="0.5", ranksep="1.05", splines=polyline, fontname="Arial"];',
          'node [shape=plain, fontname="Arial"];','edge [fontname="Arial", fontsize=10, color="#6B7280", arrowsize=0.6];']
        for name in selected:
            cons=[c for c in metadata['constraints'] if c['table_name']==name]
            pks={n for c in cons if c['kind']=='p' for n in c['columns']}
            fks={n for c in cons if c['kind']=='f' for n in c['columns']}
            allcols=[c for c in metadata['columns'] if c['table_name']==name]
            cols=allcols if full else [c for c in allcols if c['name'] in pks|fks or c['name'] in {'start_date','end_date','billing_period','effective_from','unit_price','quantity','amount','old_index','new_index','status','url'}]
            rows=[]
            for col in cols:
                flags=('PK ' if col['name'] in pks else '')+('FK ' if col['name'] in fks else '')
                typ=col['type'].replace('character varying','varchar').replace('timestamp with time zone','timestamptz')
                label=escape(flags+col['name']+(' : '+typ if full else ''))
                rows.append(f'<TR><TD PORT="{col["name"]}" ALIGN="LEFT"><FONT POINT-SIZE="12">{label}</FONT></TD></TR>')
            label='<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="7" COLOR="#CBD5E1"><TR><TD BGCOLOR="#E9EEF3"><B>'+name+'</B></TD></TR>'+''.join(rows)+'</TABLE>'
            dot.append(name+' [label=<'+label+'>];')
        for fk in (c for c in metadata['constraints'] if c['kind']=='f' and c['table_name'] in selected and c['parent_table'] in selected):
            unique=any(c['kind'] in ('p','u') and c['table_name']==fk['table_name'] and c['columns']==fk['columns'] for c in metadata['constraints'])
            dot.append(f'{fk["parent_table"]}:{fk["parent_columns"][0]} -> {fk["table_name"]}:{fk["columns"][0]} [dir=both, arrowtail=tee, arrowhead={"tee" if unique else "crow"}];')
        dot.append('}')
        (ROOT/'docs/diagrams'/f'{filename}.dot').write_text('\n'.join(dot)+'\n',encoding='utf-8')
    print('Built DBML, design-schema.json and five ERD sources from verified PostgreSQL metadata.')


if __name__=='__main__': main()
