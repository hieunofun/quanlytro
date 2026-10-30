"""Introspect a disposable V11 database; export the actual schema and JPA snapshots."""
from pathlib import Path
import argparse,importlib.util,json,subprocess,re

ROOT=Path(__file__).resolve().parents[1]
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--database',default='hostel_migration49');ap.add_argument('--port',default='55449');args=ap.parse_args()
    spec=importlib.util.spec_from_file_location('verify_design',ROOT/'scripts/verify-design.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
    result=subprocess.run(['C:/Program Files/PostgreSQL/18/bin/psql.exe','-X','-h','127.0.0.1','-p',args.port,'-U','postgres','-d',args.database,'-At','-c',mod.METADATA_SQL],check=True,capture_output=True,text=True,encoding='utf-8')
    meta=json.loads(result.stdout)
    assert len({c['table_name'] for c in meta['columns']})==49
    evidence=ROOT/'.runtime/schema-review';evidence.mkdir(exist_ok=True)
    (evidence/'metadata.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
    tables=json.loads((ROOT/'docs/schema49-tables.json').read_text(encoding='utf-8'))
    groups={}
    for t in tables:groups.setdefault(t['group'],[]).append(t['name'])
    path=ROOT/'scripts/build-design.py';text=path.read_text(encoding='utf-8')
    start=text.index('GROUPS = [');end=text.index('\n\n\ndef quoted',start)
    text=text[:start]+'GROUPS = '+repr(list(groups.items()))+text[end:]
    text=text.replace('// 17 bảng; các giá trị tính toán nằm trong 6 view, không tạo bảng trùng dữ kiện.','// 49 bảng V11; nguồn là metadata PostgreSQL đã thực thi. Tổng và số dư tính bằng view.')
    start=text.index("    variants=");end=text.index('\n    for filename, selected in variants.items():',start)
    variants={'quanlytro-dbdiagram':list(t['name'] for t in tables),
      'erd-cu-tru':['users','tenant_profiles','properties','rooms','contracts','contract_parties','contract_contacts','contract_residencies','contract_terms','room_possessions','handovers','handover_items','checkout_records'],
      'erd-dien-nuoc':['properties','rooms','contracts','service_items','service_price_configs','contract_service_subscriptions','meters','meter_readings','consumption_segments','alert_policies','consumption_alerts','alert_baseline_sources'],
      'erd-tai-chinh':['contracts','reservations','billing_documents','billing_items','cash_accounts','cash_transactions','fund_accounts','fund_entries','fund_holds','checkout_records'],
      'erd-su-co':['users','properties','rooms','assets','asset_placements','maintenance_requests','maintenance_events','maintenance_costs','maintenance_schedules','stored_files','file_attachments']}
    text=text[:start]+'    variants='+repr(variants)+text[end:]
    path.write_text(text,encoding='utf-8')
    build=importlib.util.spec_from_file_location('build_design',path);module=importlib.util.module_from_spec(build);build.loader.exec_module(module);module.main()
    entity_dir=ROOT/'src/main/java/com/hostel/model49/entity';entity_dir.mkdir(parents=True,exist_ok=True)
    mapping={}
    for t in tables:
        name=t['name'];clazz=''.join(part.title() for part in name.split('_'))+'49Entity';mapping[name]=clazz
        lines=['package com.hostel.model49.entity;','', 'import jakarta.persistence.*;','import org.hibernate.annotations.Immutable;','import lombok.Getter;','',f'@Entity @Immutable @Table(name="{name}") @Getter',f'public class {clazz} {{']
        for col in (c for c in meta['columns'] if c['table_name']==name):
            typ=col['type'];field=re.sub(r'_([a-z])',lambda m:m[1].upper(),col['name'])
            java='String'
            if typ=='bigint':java='Long'
            elif typ=='integer':java='Integer'
            elif typ=='smallint':java='Short'
            elif typ=='boolean':java='Boolean'
            elif typ=='date':java='java.time.LocalDate'
            elif typ=='timestamp with time zone':java='java.time.Instant'
            elif typ.startswith('numeric'):java='java.math.BigDecimal'
            if col['name']=='id':lines.append(' @Id @GeneratedValue(strategy=GenerationType.IDENTITY)')
            if typ=='jsonb':lines.append(' @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)')
            attrs=[f'name="{col["name"]}"',f'nullable={str(not col["not_null"]).lower()}']
            match=re.fullmatch(r'numeric\((\d+),(\d+)\)',typ)
            if match:attrs += [f'precision={match[1]}',f'scale={match[2]}']
            elif typ in ('text','jsonb'):attrs.append(f'columnDefinition="{typ}"')
            elif typ.startswith('character varying('):attrs.append('length='+re.search(r'\((\d+)\)',typ)[1])
            lines.append(' @Column('+','.join(attrs)+') private '+java+' '+field+';')
        lines.append('}');(entity_dir/(clazz+'.java')).write_text('\n'.join(lines)+'\n',encoding='utf-8')
    repo_dir=ROOT/'src/main/java/com/hostel/model49/repository';repo_dir.mkdir(exist_ok=True)
    (repo_dir/'BillingItemSnapshotRepository.java').write_text('''package com.hostel.model49.repository;
import com.hostel.model49.entity.BillingItems49Entity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BillingItemSnapshotRepository extends JpaRepository<BillingItems49Entity,Long> {
 long countByDocumentId(Long documentId);
}
''',encoding='utf-8')
    print('Generated DBML, ERD sources and 49 immutable JPA entity mappings from PostgreSQL metadata.')

if __name__=='__main__':main()
