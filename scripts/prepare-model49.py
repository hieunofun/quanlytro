"""Keep V10 regression fixtures runnable, select V11 for the actual application."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
legacy=[
'controller/CatalogController.java','controller/ContractController.java','controller/InvoiceController.java',
'controller/MeterController.java','controller/MaintenanceController.java','controller/ImageController.java',
'controller/ReportController.java','controller/ExportController.java','controller/RetiredPrototypeController.java',
'service/ContractService.java','service/InvoiceService.java','service/MeterService.java','service/PricingService.java','service/BillingCalendar.java']
for name in legacy:
    path=ROOT/'src/main/java/com/hostel'/name
    text=path.read_text(encoding='utf-8')
    marker='@org.springframework.context.annotation.Profile("!model49")'
    if marker not in text:
        at=text.index('@RestController' if name.startswith('controller/') else '@Service')
        text=text[:at]+marker+'\n'+text[at:]
        path.write_text(text,encoding='utf-8')
test=ROOT/'src/test/resources/application-test.yml'
text=test.read_text(encoding='utf-8')
if 'target: 10' not in text:text=text.replace('  flyway:\n','  flyway:\n    target: 10 # historical V10 regression suite; V11 tests override explicitly\n')
test.write_text(text,encoding='utf-8')
path=ROOT/'src/test/java/com/hostel/MigrationTests.java'
text=path.read_text(encoding='utf-8').replace('dataSource(url,user,password).load().migrate()','dataSource(url,user,password).target("10").load().migrate()')
path.write_text(text,encoding='utf-8')
app=ROOT/'src/main/resources/application.yml'
text=app.read_text(encoding='utf-8')
if 'default: dev' not in text:
    text=text.replace('  profiles:\n    active: dev\n','')
    text=text.replace('spring:\n','spring:\n  profiles:\n    default: dev\n    group:\n      dev: model49\n      prod: model49\n',1)
app.write_text(text,encoding='utf-8')
print('Selected model49 for runtime; historical tests stay explicitly on V10.')
