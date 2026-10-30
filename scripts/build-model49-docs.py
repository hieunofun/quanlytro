"""Generate field dictionary and collect verification evidence without inferring passes."""
from pathlib import Path
import json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
def main():
    metadata=json.loads((ROOT/'docs/design-schema.json').read_text(encoding='utf-8'))
    manifest=json.loads((ROOT/'docs/schema49-tables.json').read_text(encoding='utf-8'))
    out=['# Từ điển dữ liệu — 49 bảng V13','','Ngày cập nhật: 30/10/2026. Sinh từ metadata PostgreSQL đã thực thi; không tính bảng lịch sử Flyway. Xem [luồng triển khai và kiểm chứng](TRIEN-KHAI-MO-HINH-49-BANG.md).','','Quy ước khoảng thời gian: `[từ, đến)`, ngày/thời điểm kết thúc không thuộc khoảng. Tất cả tiền dùng `numeric`; tiền giao dịch dương, chiều tiền nằm ở loại bút toán. Thời điểm dùng `timestamptz`, hiển thị theo Asia/Ho_Chi_Minh.','']
    for i,t in enumerate(manifest,1):
        name=t['name'];constraints=[c for c in metadata['constraints'] if c['table_name']==name]
        out.extend([f'## {i:02}. `{name}`', '',t['description'], '', '| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |','| --- | --- | --- | --- |'])
        for c in (c for c in metadata['columns'] if c['table_name']==name):
            keys=[]
            for k in constraints:
                if c['name'] in k['columns']:
                    if k['kind']=='p':keys.append('PK')
                    if k['kind']=='f':keys.append('→ `'+k['parent_table']+'.'+','.join(k['parent_columns'])+'`')
                    if k['kind']=='u':keys.append('UNIQUE'+(' ghép' if len(k['columns'])>1 else ''))
            out.append('| `'+c['name']+'` | `'+c['type']+'` | '+('Có' if c['not_null'] else 'Không')+' | '+('; '.join(keys) or '—')+' |')
        out.append('')
    (ROOT/'docs/TU-DIEN-DU-LIEU-49-BANG.md').write_text('\n'.join(out),encoding='utf-8')
    suites=[]
    for path in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
        r=ET.parse(path).getroot();suites.append({'name':r.attrib['name'],'tests':int(r.attrib['tests']),'failures':int(r.attrib['failures']),'errors':int(r.attrib['errors']),'skipped':int(r.attrib['skipped'])})
    report={'validated_on':'2026-10-30','schema':'V13-model49','suites':suites,'tests':sum(s['tests'] for s in suites),'failures':sum(s['failures'] for s in suites),'errors':sum(s['errors'] for s in suites),'scope_notes':['BusinessIntegrityTests, Model49Tests, Model49HttpTests and OpenKosWorkflowTests exercise V13.','Migration suites separately verify V9/V10/V11/V13 transitions; the remaining application suites retain V10 regressions.','Database-only DDL checks are in database-validation.json.','Browser results from previous runs are not included as current evidence.','These results do not certify production deployment, all real-world policies or migration of the user live database.']}
    current={'BusinessIntegrityTests','Model49Tests','Model49HttpTests','OpenKosWorkflowTests'}
    report['current_model_tests']=sum(s['tests'] for s in suites if s['name'].split('.')[-1] in current)
    report['migration_tests']=sum(s['tests'] for s in suites if s['name'].endswith('MigrationTests'))
    report['historical_and_unit_tests']=report['tests']-report['current_model_tests']-report['migration_tests']
    report['all_passed']=bool(suites) and report['failures']==0 and report['errors']==0 and not any(s['skipped'] for s in suites)
    (ROOT/'docs/implementation-validation49.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Generated 49-table field dictionary; test evidence:',report['tests'],'tests; all_passed=',report['all_passed'])
if __name__=='__main__':main()
