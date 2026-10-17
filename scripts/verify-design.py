"""Execute the presentation schema and meaningful DB cases in a disposable database."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]

METADATA_SQL = """
WITH t AS (SELECT c.oid,c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
  WHERE n.nspname='public' AND c.relkind='r'), cols AS (
 SELECT t.relname AS table_name,a.attname AS name,a.attnum AS position,
 format_type(a.atttypid,a.atttypmod) AS type,a.attnotnull AS not_null,a.attidentity AS identity,
 pg_get_expr(d.adbin,d.adrelid) AS default_value,col_description(t.oid,a.attnum) AS note
 FROM t JOIN pg_attribute a ON a.attrelid=t.oid
 LEFT JOIN pg_attrdef d ON d.adrelid=t.oid AND d.adnum=a.attnum WHERE a.attnum>0 AND NOT a.attisdropped
), cons AS (
 SELECT t.relname AS table_name,c.conname AS name,c.contype AS kind,
 ARRAY(SELECT a.attname FROM unnest(c.conkey) WITH ORDINALITY k(num,pos)
 JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.num ORDER BY k.pos) AS columns,
 p.relname AS parent_table,
 ARRAY(SELECT a.attname FROM unnest(c.confkey) WITH ORDINALITY k(num,pos)
 JOIN pg_attribute a ON a.attrelid=c.confrelid AND a.attnum=k.num ORDER BY k.pos) AS parent_columns,
 c.confdeltype AS on_delete,c.confupdtype AS on_update,
 pg_get_expr(c.conbin,c.conrelid) AS check_expression,pg_get_constraintdef(c.oid) AS definition
 FROM t JOIN pg_constraint c ON c.conrelid=t.oid LEFT JOIN pg_class p ON p.oid=c.confrelid
 WHERE c.contype IN ('p','u','f','c','x')
), idx AS (
 SELECT t.relname AS table_name,i.relname AS name,x.indisunique AS is_unique,x.indisprimary AS is_primary,
 ARRAY(SELECT a.attname FROM unnest(x.indkey::smallint[]) WITH ORDINALITY k(num,pos)
 LEFT JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.num WHERE k.pos<=x.indnkeyatts ORDER BY k.pos) AS columns,
 pg_get_expr(x.indpred,x.indrelid) AS predicate,pg_get_indexdef(x.indexrelid) AS definition,c.contype AS constraint_kind,
 am.amname AS access_method
 FROM t JOIN pg_index x ON x.indrelid=t.oid JOIN pg_class i ON i.oid=x.indexrelid JOIN pg_am am ON am.oid=i.relam
 LEFT JOIN pg_constraint c ON c.conindid=x.indexrelid AND c.conrelid=t.oid AND c.contype IN ('p','u','x')
)
SELECT json_build_object('columns',(SELECT json_agg(cols ORDER BY table_name,position) FROM cols),
 'constraints',(SELECT json_agg(cons ORDER BY table_name,name) FROM cons),
 'indexes',(SELECT json_agg(idx ORDER BY table_name,name) FROM idx),
 'views',(SELECT json_agg(table_name ORDER BY table_name) FROM information_schema.views WHERE table_schema='public'),
 'server_version',current_setting('server_version'));
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--host', default='127.0.0.1')
    ap.add_argument('--port', default='55436')
    ap.add_argument('--user', default='postgres')
    ap.add_argument('--pg-bin', default='C:/Program Files/PostgreSQL/18/bin')
    args = ap.parse_args()
    target = 'qlytro_design_check_' + uuid.uuid4().hex[:12]
    assert re.fullmatch(r'qlytro_design_check_[0-9a-f]{12}', target)
    env = {**os.environ, 'PGCLIENTENCODING': 'UTF8'}
    conn = ['-w','-h',args.host,'-p',args.port,'-U',args.user]
    evidence = ROOT / '.runtime/schema-review'
    evidence.mkdir(parents=True, exist_ok=True)

    def binary(name):
        candidate = Path(args.pg_bin) / (name+'.exe' if os.name=='nt' else name)
        return str(candidate) if candidate.exists() else shutil.which(name) or name

    def psql(sql=None, files=None, variables=None):
        command = [binary('psql'),'-X',*conn,'-d',target,'-v','ON_ERROR_STOP=1','-At']
        for key,value in (variables or {}).items(): command += ['-v',f'{key}={value}']
        for file in files or []: command += ['-f',str(ROOT/file)]
        if sql is not None: command += ['-c',sql]
        return subprocess.run(command,capture_output=True,text=True,encoding='utf-8',env=env,check=True).stdout

    created = False
    try:
        subprocess.run([binary('createdb'),*conn,target],capture_output=True,text=True,encoding='utf-8',env=env,check=True)
        created = True
        psql(files=['docs/quanlytro.sql','docs/database-rules.sql'])
        metadata = json.loads(psql(METADATA_SQL))
        (evidence/'metadata.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf-8')
        output = psql(files=['docs/database-cases.sql'],variables={'keep_data':'1'})
        (evidence/'cases-output.txt').write_text(output,encoding='utf-8')
        matched = re.search(r'(?m)^(\d+)\|t$',output)
        assert matched, output[-1200:]
        passed = int(matched[1])

        first_sql = "SET application_name='design_payment_first'; BEGIN; INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(2,'CONCURRENT-A',600,'CASH',1); SELECT pg_sleep(4); COMMIT;"
        command = [binary('psql'),'-X',*conn,'-d',target,'-v','ON_ERROR_STOP=1','-At','-c',first_sql]
        first = subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,encoding='utf-8',env=env)
        def wait_for_activity(app,condition):
            until=time.monotonic()+3
            while time.monotonic()<until:
                if psql("SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE datname=current_database() AND application_name='"+app+"' AND "+condition+")").strip()=='t': return True
                time.sleep(.05)
            return False
        assert wait_for_activity('design_payment_first',"wait_event='PgSleep'"),'First writer did not hold the transaction open'
        second = subprocess.Popen([binary('psql'),'-X',*conn,'-d',target,'-v','ON_ERROR_STOP=1','-At','-c',
          "SET application_name='design_payment_second'; INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(2,'CONCURRENT-B',600,'CASH',1);"],
          stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,encoding='utf-8',env=env)
        lock_observed=wait_for_activity('design_payment_second',"wait_event_type='Lock'")
        rest,error = first.communicate(timeout=20)
        second_output,second_error=second.communicate(timeout=20)
        assert first.returncode==0,error
        assert lock_observed,'Second writer was not observed waiting for the first transaction lock'
        assert second.returncode!=0 and 'Payment exceeds debt' in second_error,second_error
        balance = psql("SELECT balance_due FROM v_invoice_balances WHERE id=2").strip()
        assert balance=='400.00',balance
        checks = metadata['constraints']
        report = {'schema':'presentation-v2','tables':len({c['table_name'] for c in metadata['columns']}),
          'columns':len(metadata['columns']),'primary_keys':sum(c['kind']=='p' for c in checks),
          'foreign_keys':sum(c['kind']=='f' for c in checks),'check_constraints':sum(c['kind']=='c' for c in checks),
          'exclusion_constraints':sum(c['kind']=='x' for c in checks),'views':metadata['views'],
          'database_cases_passed':passed,'concurrent_payment_test':{'passed':True,'lock_wait_observed':lock_observed,'invoice_total':1000,'attempts':[600,600],
            'accepted_payments':1,'remaining_balance':400},'postgresql':metadata['server_version'],'all_passed':True}
        (ROOT/'docs/database-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(json.dumps(report,ensure_ascii=False))
    except subprocess.CalledProcessError as exc:
        print(exc.stderr)
        raise
    finally:
        if created:
            assert target.startswith('qlytro_design_check_')
            subprocess.run([binary('dropdb'),*conn,target],capture_output=True,env=env,check=True)


if __name__=='__main__': main()
