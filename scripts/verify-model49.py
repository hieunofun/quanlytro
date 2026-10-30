"""Verify canonical 49-table DDL in a newly created, disposable database."""
from pathlib import Path
import argparse,importlib.util,json,os,subprocess,uuid
ROOT=Path(__file__).resolve().parents[1]
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--host',default='127.0.0.1');ap.add_argument('--port',default='55449');ap.add_argument('--user',default='postgres');ap.add_argument('--pg-bin',default='C:/Program Files/PostgreSQL/18/bin');args=ap.parse_args()
    spec=importlib.util.spec_from_file_location('metadata49',ROOT/'scripts/verify-design.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    name='hostel_design49_'+uuid.uuid4().hex[:12];conn=['-h',args.host,'-p',args.port,'-U',args.user]
    def call(tool,*extra):
        result=subprocess.run([str(Path(args.pg_bin)/(tool+('.exe' if os.name=='nt' else ''))),*conn,*extra],capture_output=True,text=True,encoding='utf-8')
        if result.returncode:raise RuntimeError(result.stderr)
        return result.stdout
    call('createdb',name)
    try:
        call('psql','-X','-v','ON_ERROR_STOP=1','-d',name,'-f',str(ROOT/'docs/quanlytro.sql'),'-f',str(ROOT/'docs/database-rules.sql'))
        meta=json.loads(call('psql','-X','-At','-d',name,'-c',module.METADATA_SQL))
        output=call('psql','-X','-At','-v','ON_ERROR_STOP=1','-d',name,'-f',str(ROOT/'docs/database-cases49.sql'))
        passed=int(next(line.split('|')[1] for line in output.splitlines() if line.startswith('MODEL49_CASES_PASSED|')))
        c=meta['constraints'];report={'schema':'V13-model49','validated_on':'2026-10-30','tables':len({x['table_name'] for x in meta['columns']}),'columns':len(meta['columns']),'primary_keys':sum(x['kind']=='p' for x in c),'foreign_keys':sum(x['kind']=='f' for x in c),'check_constraints':sum(x['kind']=='c' for x in c),'exclusion_constraints':sum(x['kind']=='x' for x in c),'views':meta['views'],'database_cases_passed':passed,'postgresql':meta['server_version'],'all_passed':True,'scope':'DDL/rules only; Java, HTTP, migration and concurrency results are separate in implementation-validation49.json'}
        assert report['tables']==49 and passed>=20
        (ROOT/'docs/database-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        evidence=ROOT/'.runtime/schema-review';evidence.mkdir(exist_ok=True);(evidence/'metadata.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps(report,ensure_ascii=False))
    finally:
        assert name.startswith('hostel_design49_');call('dropdb',name)
if __name__=='__main__':main()
