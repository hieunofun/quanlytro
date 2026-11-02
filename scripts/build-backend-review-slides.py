"""Editable PPTX + standalone HTML. Facts come from executed tests/API evidence; no speaker notes."""
from pathlib import Path
import json, html, re, sys, zipfile

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.runtime/presentation-tools'))
from pptx import Presentation
from pptx.util import Inches,Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE

OUT=ROOT/'docs/backend-review'
OUT.mkdir(parents=True,exist_ok=True)
tests=json.loads((ROOT/'docs/implementation-validation49.json').read_text(encoding='utf-8'))
sql=json.loads((ROOT/'docs/database-validation.json').read_text(encoding='utf-8'))
demo=json.loads((OUT/'demo-evidence.json').read_text(encoding='utf-8'))
assert tests['all_passed'] and demo['all_assertions_passed'] and sql['all_passed']
assert tests['validated_on']=='2026-11-01' and demo['validated_on']=='2026-11-01'
assert demo['turnover']['old_tenant_total']==1770000 and demo['turnover']['new_tenant_total']==1763000
money=lambda n:f'{n:,.0f}'.replace(',','.')
M='src/main/java/com/hostel/model49/'
T='src/test/java/com/hostel/'
slides=[]
def add(title,kind='bullets',**data):slides.append(dict(title=title,kind=kind,**data))
add('Backend hệ thống\nquản lý nhà trọ','title',subtitle='Hợp đồng · điện nước · hóa đơn · thu tiền · quyết toán',body='Java 21  |  Spring Boot  |  PostgreSQL\nBản kiểm tra ngày 01/11/2026',sources=['pom.xml','docs/implementation-validation49.json'])
add('Các phần backend đang có',bullets=['Hợp đồng, người thực ở và giao/trả phòng.','Chỉ số điện nước, cảnh báo và hóa đơn có nguồn.','Thu tiền, cọc, trả trước, khấu trừ và hoàn tiền.','Sự cố, chi phí sửa chữa và phân quyền theo phạm vi.'],sources=[M+'Api49.java',M+'Operations49.java'])
add('Một yêu cầu đi qua backend như thế nào?','flow',steps=['Client\nHTTP + JSON','JWT Filter\nXác thực','Controller\nNhận request','Service\nQuy tắc nghiệp vụ','PostgreSQL\nGiao dịch + ràng buộc'],body='Truy vấn nghiệp vụ chủ yếu qua Database / JdbcTemplate.\nJPA có ánh xạ entity và repository đọc snapshot; không mô tả toàn bộ truy cập dữ liệu là JPA.',sources=['src/main/java/com/hostel/security/JwtFilter.java',M+'Api49.java','src/main/java/com/hostel/service/Database.java',M+'repository/BillingItemSnapshotRepository.java'])
add('CSDL tách rõ nguồn của từng nghiệp vụ','table',headers=['Nhóm','Các bảng chính'],rows=[['Cư trú','contracts, contract_terms, contract_residencies, room_possessions'],['Điện nước','meters, meter_readings, consumption_segments'],['Chứng từ','billing_documents, billing_items'],['Tiền','cash_transactions, fund_accounts, fund_entries, fund_holds']],body=f"Toàn mô hình: {sql['tables']} bảng, {sql['foreign_keys']} khóa ngoại, {len(sql['views'])} view. Bảng trên chỉ nêu các nhóm chính.\nFlyway hiện tại tới V13; tiền và công nợ được tính từ nguồn qua view.",sources=['docs/design-schema.json','src/main/resources/db/migration/V13__billing_integrity_and_receipt_intent.sql'])
add('Phân quyền được kiểm tra ở backend','table',headers=['Vai trò','Phạm vi'],rows=[['ADMIN','Quản lý toàn bộ trong mô hình một chủ nhà.'],['STAFF','Đúng phòng/khu được giao; hoàn/cấn cọc/điều chỉnh cần quyền bổ sung.'],['TENANT','Hợp đồng, hóa đơn và dữ liệu cá nhân được cấp quyền.']],body='JWT truy cập và refresh xoay vòng. Phiên lưu trong RAM của một instance; khởi động lại cần đăng nhập lại.',sources=[M+'Access49.java','src/main/java/com/hostel/security/SessionRegistry.java',T+'BusinessIntegrityTests.java'])
add('Ký hợp đồng và nhận phòng là hai việc riêng','flow',steps=['DRAFT\nSoạn hợp đồng','ACTIVE\nGhi nhận đã ký','Giao phòng\nThời điểm thực tế','Trả phòng\nGhi bàn giao','Quyết toán\nDuyệt tài chính'],body='Cọc cam kết không phải tiền đã nhận. Người đứng tên không tự được tính là đang ở.\nSau trả phòng: TURNOVER → kiểm tra, chuẩn bị → READY.',sources=[M+'Leases49.java',T+'Model49Tests.java'])
add('Hóa đơn giữ cách tính và nguồn số liệu',bullets=['Tiền phòng chia theo số ngày của tháng; mốc kết thúc không bao gồm.','Dịch vụ chia ngày theo phòng, số lượng hoặc ngày-người thực ở.','Điện nước = chênh chỉ số × đơn giá có hiệu lực; không chia sản lượng giả định.','Đã phát hành: giữ dòng gốc và sửa sai bằng chứng từ điều chỉnh.'],sources=[M+'Billing49.java',M+'Finance49.java','src/main/resources/db/migration/V11__approved_49_table_model.sql'])
add('Đổi khách giữa tháng: không tính trùng','table',headers=['','Khách A','Phòng trống','Khách B'],rows=[['Khoảng tính','01–15/01','16/01','17–31/01'],['Tiền phòng','1.500.000','0','1.500.000'],['Điện','30 kWh','2 kWh chủ nhà','28 kWh'],['Tổng hóa đơn','1.770.000','Không gán cho khách','1.763.000']],body='Dữ liệu thử qua API thật. Tiền phòng 3.100.000/tháng; điện 3.500/kWh.\nTổng gồm phòng, internet, dịch vụ theo người và điện. Dịch vụ phụ không phát sinh; giá rác bằng 0 trong bộ thử.',sources=['docs/backend-review/demo-evidence.json',T+'BusinessIntegrityTests.java'])
add('Tiền thực nhận khác tiền phân bổ','money',cards=[['Khách B nộp',money(1800000)],['Phân bổ hóa đơn',money(1763000)],['Còn trả trước',money(37000)]],body='Gửi lại cùng mã phiếu không ghi thêm tiền.\nTham chiếu ngân hàng đã xác nhận không được ghi hai lần.',sources=[M+'Billing49.java',M+'Finance49.java','docs/backend-review/api-transcript.json'])
add('Sửa tiền điện sau khi đã thu đủ','money',cards=[['Ban đầu: 50 × 3.500','175.000'],['Đúng: 40 × 3.500','140.000'],['Điều chỉnh giảm','35.000']],body='Giữ hóa đơn và chỉ số gốc. Bản điểm đo mới liên kết dòng điều chỉnh.\n35.000 trở về đúng quỹ đã thanh toán; chưa có nghĩa đã chi hoàn.\nNếu đã có kỳ sau phụ thuộc: chặn sửa để đối soát chuỗi.',sources=[M+'Finance49.java','docs/backend-review/demo-evidence.json'])
add('Trả phòng: chốt nợ rồi ghi tiền hoàn','money',cards=[['Cọc thực nhận A','2.000.000'],['Cấn các khoản A','1.770.000'],['Đã chi hoàn A','230.000']],body='Duyệt quyết toán và chi hoàn là hai thao tác.\nTrả sớm: điều chỉnh tiền phòng và dịch vụ PRORATED đã phát hành.\nFULL_PERIOD / ONE_TIME giữ theo chế độ thu; khoản tranh chấp không tự bị cấn.',sources=[M+'Settlements49.java',M+'Finance49.java',T+'BusinessIntegrityTests.java','docs/backend-review/demo-evidence.json'])
add('Các kiểm soát khi ghi dữ liệu',bullets=['Thao tác tài chính chạy trong giao dịch; lỗi thì rollback.','Khóa bản ghi và mã chống lặp khi thu, phân bổ, hoàn tiền.','UNIQUE / FK / CHECK / EXCLUDE bảo vệ tính nhất quán ở PostgreSQL.','Sổ tiền và chứng từ đã phát hành được bảo vệ khỏi sửa đè.'],sources=[M+'Finance49.java',M+'Api49.java','src/main/resources/db/migration/V11__approved_49_table_model.sql',T+'Model49Tests.java'])
add('Kết quả kiểm chứng đã chạy','table',headers=['Phạm vi','Kết quả'],rows=[['Ứng dụng/API V13',str(tests['current_model_tests'])+' ca đạt'],['Migration',str(tests['migration_tests'])+' ca đạt'],['Hồi quy V10 + đơn vị',str(tests['historical_and_unit_tests'])+' ca đạt'],['Tổng Java',str(tests['tests'])+' ca; 0 lỗi; 0 bỏ qua'],['DDL và quy tắc SQL',str(sql['database_cases_passed'])+' ca đạt']],body='Có ca HTTP gọi thật vào Spring Boot và PostgreSQL riêng.\nSố lượng test không phải tỷ lệ bao phủ hoặc xác nhận đúng mọi nghiệp vụ.',sources=['docs/implementation-validation49.json','docs/database-validation.json','docs/backend-review/demo-evidence.json'])
add('Mở backend và dữ liệu minh chứng','table',headers=['Nội dung','Nơi xem'],rows=[['Ứng dụng local',demo['base_url']],['API kiểm tra','backend.postman_collection.json'],['Dữ liệu quyết toán A',f"GET /api/contracts/{demo['contracts']['a']}/settlement"],['Hóa đơn B',f"GET /api/invoices/{demo['invoices']['b']}"],['Đối chiếu CSDL','backend-readonly.sql']],body='Bộ request dùng đúng ID đã tạo; token lấy khi đăng nhập.\nTất cả tên người và dữ liệu trong bộ thử là giả.',sources=['docs/backend-review/backend.postman_collection.json','docs/backend-review/api-transcript.json'])
add('Giới hạn hiện tại',bullets=['Một chủ nhà; phiên xác thực trong RAM của một instance.','Xác minh chuyển khoản và ghi nhận đã hoàn còn là thao tác thủ công.','Chưa tự đối soát dây chuyền nhiều kỳ đo; thiếu điểm ranh giới thì phải bổ sung.','Chưa kiểm chứng tải lớn và phục hồi dữ liệu thực tế.'],sources=['src/main/java/com/hostel/security/SessionRegistry.java',M+'Finance49.java',M+'Billing49.java','docs/implementation-validation49.json'])

for s in slides:
    for path in s['sources']:assert (ROOT/path).exists(),path
manifest={'created_on':'2026-11-01','speaker_notes':False,'slides':slides,'evidence':{'java':tests['tests'],'current_model':tests['current_model_tests'],'sql':sql['database_cases_passed'],'demo_requests':demo['requests']}}
(OUT/'slide-facts.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Plain, editable PowerPoint: white background, Arial, one blue rule, no stock images.
prs=Presentation();prs.slide_width=Inches(13.333333);prs.slide_height=Inches(7.5)
prs.core_properties.title='Backend hệ thống quản lý nhà trọ'
prs.core_properties.author='Nhóm dự án quản lý nhà trọ'
prs.core_properties.subject='Nội dung đối chiếu mã nguồn và kết quả thực thi 01/11/2026'
INK='202B38';BLUE='245A81';GREY='5D6975';LINE='D8E0E8';PALE='F3F6F9'
def box(slide,x,y,w,h,fill='FFFFFF',line=None):
    obj=slide.shapes.add_shape(MSO_SHAPE.RECTANGLE,Inches(x),Inches(y),Inches(w),Inches(h))
    obj.fill.solid();obj.fill.fore_color.rgb=RGBColor.from_string(fill)
    if line:obj.line.color.rgb=RGBColor.from_string(line)
    else:obj.line.fill.background()
    return obj
def text(slide,x,y,w,h,value,size=22,bold=False,color=INK):
    obj=slide.shapes.add_textbox(Inches(x),Inches(y),Inches(w),Inches(h));tf=obj.text_frame;tf.word_wrap=True
    tf.margin_left=tf.margin_right=tf.margin_top=tf.margin_bottom=0
    for i,line in enumerate(str(value).split('\n')):
        p=tf.paragraphs[0] if i==0 else tf.add_paragraph();p.text=line;p.font.name='Arial';p.font.size=Pt(size);p.font.bold=bold;p.font.color.rgb=RGBColor.from_string(color);p.space_after=Pt(8)
    return obj
for n,s in enumerate(slides,1):
    slide=prs.slides.add_slide(prs.slide_layouts[6]);box(slide,0,0,13.34,7.5)
    if s['kind']=='title':
        text(slide,.8,1.2,11.7,1.7,s['title'],38,True,BLUE);box(slide,.8,3.1,1.0,.05,BLUE)
        text(slide,.8,3.5,11.6,.65,s['subtitle'],24);text(slide,.8,4.65,11.6,1.0,s['body'],21,color=GREY)
    else:
        text(slide,.65,.5,12,.7,s['title'],29,True,BLUE);box(slide,.65,1.3,12,.018,LINE)
        if s['kind']=='bullets':
            for i,b in enumerate(s['bullets']):
                text(slide,.8,1.8+i*1.0,.25,.5,'•',24,color=BLUE);text(slide,1.2,1.8+i*1.0,10.95,.9,b,23)
        elif s['kind']=='table':
            headers=s['headers'];cols=len(headers);x=.75;w=11.85
            widths=([3.1,w-3.1] if cols==2 else [2.05]+[(w-2.05)/(cols-1)]*(cols-1))
            rowh=.65 if len(s['rows'])<=4 else .57
            for r,row in enumerate([headers]+s['rows']):
                xx=x
                for c,value in enumerate(row):
                    box(slide,xx,1.65+r*rowh,widths[c],rowh,PALE if r==0 else 'FFFFFF',LINE)
                    text(slide,xx+.12,1.78+r*rowh,widths[c]-.24,rowh-.08,value,17 if cols>2 else 18,r==0)
                    xx+=widths[c]
            text(slide,.8,5.55,11.8,.95,s['body'],18,color=GREY)
        elif s['kind']=='flow':
            for i,step in enumerate(s['steps']):
                x=.75+i*2.48;box(slide,x,2.05,2.22,1.48,PALE,LINE);text(slide,x+.14,2.32,1.94,1.0,step,21,True)
                if i<4:text(slide,x+2.25,2.55,.3,.5,'→',20,color=BLUE)
            text(slide,.85,4.4,11.7,1.3,s['body'],21)
        elif s['kind']=='money':
            for i,(label,value) in enumerate(s['cards']):
                x=.8+i*4.23;box(slide,x,1.85,3.93,1.55,PALE,LINE)
                text(slide,x+.18,2.05,3.55,.4,label,18);text(slide,x+.18,2.57,3.55,.55,value,31,True,BLUE)
            text(slide,.85,4.1,11.6,1.85,s['body'],21)
    text(slide,.65,7.03,11.3,.24,'QUẢN LÝ NHÀ TRỌ  ·  BACKEND  ·  01/11/2026',10,color=GREY)
    text(slide,12.0,7.03,.7,.24,f'{n:02}/{len(slides):02}',10,color=GREY)
prs.save(OUT/'backend-he-thong-nha-tro.pptx')
with zipfile.ZipFile(OUT/'backend-he-thong-nha-tro.pptx') as archive:
    assert not any(re.fullmatch(r'ppt/notesSlides/notesSlide\d+\.xml',name) for name in archive.namelist()),'Do not add speaker notes'

def esc(x):return html.escape(str(x)).replace('\n','<br>')
pages=[]
for i,s in enumerate(slides,1):
    kind=s['kind'];parts=[f'<section class="slide {kind}" id="s{i}" data-slide="{i}"><h1>{esc(s["title"])}</h1>']
    if kind=='title':parts += ['<p class="subtitle">'+esc(s['subtitle'])+'</p>','<p>'+esc(s['body'])+'</p>']
    elif kind=='bullets':parts+=['<ul>'+''.join('<li>'+esc(b)+'</li>' for b in s['bullets'])+'</ul>']
    elif kind=='table':parts+=['<table><thead><tr>'+''.join('<th>'+esc(h)+'</th>' for h in s['headers'])+'</tr></thead><tbody>'+''.join('<tr>'+''.join('<td>'+esc(c)+'</td>' for c in r)+'</tr>' for r in s['rows'])+'</tbody></table>']
    elif kind=='flow':parts+=['<div class="steps">'+''.join('<div>'+esc(t)+'</div>' for t in s['steps'])+'</div>']
    elif kind=='money':parts+=['<div class="cards">'+''.join('<div><p>'+esc(a)+'</p><strong>'+esc(b)+'</strong></div>' for a,b in s['cards'])+'</div>']
    if kind!='title' and s.get('body'):parts+=['<p class="body">'+esc(s['body'])+'</p>']
    parts += [f'<footer><span>QUẢN LÝ NHÀ TRỌ · BACKEND · 01/11/2026</span><span>{i:02}/{len(slides):02}</span></footer></section>']
    pages.append(''.join(parts))
style='''
:root{--ink:#202b38;--primary:#245a81;--muted:#5d6975;--line:#d8e0e8;--pale:#f3f6f9;--slide-width:1280px;--slide-height:720px}
*{box-sizing:border-box}body{margin:0;background:#e4e7eb;font-family:Arial,sans-serif;color:var(--ink)}
.slide{width:var(--slide-width);height:var(--slide-height);padding:48px 64px;background:white;position:relative;margin:24px auto;overflow:hidden;page-break-after:always}
h1{margin:0 0 35px;font-size:35px;line-height:1.18;color:var(--primary);font-weight:700;padding-bottom:20px;border-bottom:1px solid var(--line)}
ul{padding-left:35px;margin:50px 0 0}li{font-size:28px;line-height:1.45;padding-left:12px;margin:0 0 28px}li::marker{color:var(--primary)}
table{border-collapse:collapse;width:100%;margin-top:25px;table-layout:fixed;font-size:22px}th,td{border:1px solid var(--line);text-align:left;padding:16px 14px;line-height:1.25;overflow-wrap:anywhere}th{background:var(--pale)}th:first-child,td:first-child{width:25%}
.body{font-size:23px;line-height:1.4;margin-top:32px;color:var(--muted)}
.steps{display:flex;gap:22px;margin-top:70px}.steps>div{flex:1;min-width:0;padding:25px 15px;background:var(--pale);border:1px solid var(--line);font-size:25px;line-height:1.4;font-weight:bold}.steps+.body{margin-top:65px;font-size:25px;color:var(--ink)}
.cards{display:grid;grid-template-columns:repeat(3,1fr);gap:25px;margin-top:45px}.cards>div{border:1px solid var(--line);background:var(--pale);padding:20px}.cards p{margin:0 0 20px;font-size:23px}.cards strong{font-size:39px;color:var(--primary)}.cards+.body{font-size:25px;margin-top:55px;color:var(--ink)}
footer{position:absolute;bottom:25px;left:64px;right:64px;display:flex;justify-content:space-between;font-size:12px;color:var(--muted)}
.title h1{font-size:49px;border:0;margin-top:85px;margin-bottom:55px;line-height:1.2}.title p{font-size:26px;line-height:1.7;color:var(--muted)}.title .subtitle{color:var(--ink)}
nav{position:fixed;bottom:12px;right:18px;z-index:9;display:flex;gap:8px}button,a.button{border:1px solid var(--line);background:white;color:var(--primary);padding:10px 14px;text-decoration:none;font:16px Arial;cursor:pointer}
body.present{overflow:hidden;background:#20252c}body.present .slide{display:none;position:absolute;margin:0;left:50%;top:50%;transform:translate(-50%,-50%) scale(var(--scale,1))}body.present .slide.active{display:block}
@media print{@page{size:13.333333in 7.5in;margin:0}body{background:white}.slide{margin:0;width:13.333333in;height:7.5in;page-break-after:always}nav{display:none}body.present .slide{display:block;position:relative;left:auto;top:auto;transform:none}}
'''
script='''
const slides=[...document.querySelectorAll('.slide')];let current=0;
function show(i){current=Math.max(0,Math.min(slides.length-1,i));slides.forEach((s,n)=>s.classList.toggle('active',n===current));document.documentElement.style.setProperty('--scale',Math.min(innerWidth/1280,innerHeight/720));location.hash='s'+(current+1)}
document.querySelector('#prev').onclick=()=>show(current-1);document.querySelector('#next').onclick=()=>show(current+1);
document.querySelector('#mode').onclick=()=>{document.body.classList.toggle('present');show(current)};
addEventListener('keydown',e=>{if(['ArrowRight','PageDown',' '].includes(e.key)){e.preventDefault();show(current+1)}if(['ArrowLeft','PageUp'].includes(e.key)){e.preventDefault();show(current-1)}if(e.key==='Escape')document.body.classList.remove('present')});addEventListener('resize',()=>show(current));show(0);
'''
(OUT/'backend-he-thong-nha-tro.html').write_text('<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Backend hệ thống quản lý nhà trọ</title><style>'+style+'</style><body>'+''.join(pages)+'<nav><button id="prev" aria-label="Trang trước">←</button><button id="next" aria-label="Trang sau">→</button><button id="mode">Trình chiếu</button><a class="button" href="backend.postman_collection.json" download>Bộ API</a></nav><script>'+script+'</script></body></html>',encoding='utf-8')
print(json.dumps({'slides':len(slides),'pptx':'docs/backend-review/backend-he-thong-nha-tro.pptx','html':'docs/backend-review/backend-he-thong-nha-tro.html','speaker_notes':False},ensure_ascii=True))
