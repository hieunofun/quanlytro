"""Render standalone architecture and data diagrams for the Word report."""
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

out=Path(__file__).resolve().parents[1]/'docs/diagrams';out.mkdir(parents=True,exist_ok=True)
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':11,'svg.fonttype':'none'})
def canvas(title):
    fig,ax=plt.subplots(figsize=(12,8));ax.set(xlim=(0,12),ylim=(0,8));ax.axis('off');ax.text(.4,7.6,title,fontsize=19,weight='bold');return fig,ax
def box(ax,x,y,title,description,width=2.8,height=1.1):
    ax.add_patch(FancyBboxPatch((x,y),width,height,boxstyle='round,pad=0.04,rounding_size=0.08',facecolor='#f7f7f8',edgecolor='#c7c7cc',linewidth=1))
    ax.text(x+.15,y+height-.3,title,fontsize=12,weight='bold');ax.text(x+.15,y+.18,description,fontsize=10,va='bottom',linespacing=1.6)
def arrow(ax,a,b,label=''):
    ax.add_patch(FancyArrowPatch(a,b,arrowstyle='-|>',mutation_scale=13,linewidth=1.1,color='#666666'))
    if label: ax.text((a[0]+b[0])/2,(a[1]+b[1])/2+.1,label,fontsize=9,ha='center',bbox=dict(facecolor='white',edgecolor='none',pad=2))
def save(fig,name):
    fig.savefig(out/(name+'.png'),dpi=180,bbox_inches='tight',facecolor='white');fig.savefig(out/(name+'.svg'),bbox_inches='tight',facecolor='white');plt.close(fig)
fig,ax=canvas('Kiến trúc hệ thống Java/Web')
box(ax,.4,5.8,'React / Vite','Quản lý và người thuê\nForm, bảng, chi tiết')
box(ax,4.5,5.8,'REST Controllers','DTO + Bean Validation\nJWT + Access')
box(ax,8.4,5.8,'Dịch vụ nghiệp vụ','Contract / Meter / Invoice\nTransaction + BillingCalendar')
box(ax,8.4,3.1,'Tính phí và đơn giá','FeeCalculator + Pricing\nSnapshot công thức, đơn giá')
box(ax,4.5,3.1,'JPA và JDBC','Repository đọc snapshot\nQuery + khóa hàng')
box(ax,.4,3.1,'PostgreSQL / Flyway','Ràng buộc, lịch sử dữ liệu\n29 bảng, 8 migration')
box(ax,4.5,.5,'Tệp và thông báo','PDF tiếng Việt, ảnh có quyền\nIn-app + reminder + audit')
arrow(ax,(3.25,6.35),(4.4,6.35),'JSON + JWT');arrow(ax,(7.35,6.35),(8.3,6.35));arrow(ax,(9.8,5.75),(9.8,4.3));arrow(ax,(8.3,3.65),(7.4,3.65));arrow(ax,(4.4,3.65),(3.3,3.65));arrow(ax,(5.9,3.05),(5.9,1.75));
ax.text(.4,.15,'Ảnh lưu trên đĩa/volume; backend kiểm tra cùng phạm vi tài nguyên khi đọc.',fontsize=10,color='#666666')
save(fig,'architecture')
fig,ax=canvas('Quan hệ dữ liệu cốt lõi (lược đồ rút gọn)')
box(ax,.4,5.8,'Khu / tầng / phòng','properties → floors → rooms',3.2)
box(ax,4.6,5.8,'Hợp đồng / thành viên','contracts → contract_members\ncontract_violations',3.2)
box(ax,8.6,5.8,'Người thuê / tài khoản','tenant_profiles → users',3.0)
box(ax,.4,3.2,'Phân công / ảnh','employee_assignments\nroom_employee_assignments\nroom_images / uploaded_images',3.2,1.4)
box(ax,4.6,3.2,'Chỉ số / đơn giá','meter_readings + history\nservice_price_configs\nconsumption_alerts',3.2,1.4)
box(ax,8.6,3.2,'Hóa đơn / giao dịch','invoices → invoice_items\npayment_transactions\ncheckout_records',3.0,1.4)
box(ax,4.6,.6,'Sự cố / vận hành','maintenance_requests\nmaintenance_schedules\nnotifications / audit_logs',3.2,1.6)
arrow(ax,(3.65,6.35),(4.5,6.35),'1 : N');arrow(ax,(8.5,6.35),(7.9,6.35),'1 : N');arrow(ax,(2,5.7),(2,4.7),'1 : N');arrow(ax,(6.2,5.7),(6.2,4.7),'1 : N');arrow(ax,(7.9,3.9),(8.5,3.9),'tính tiền');arrow(ax,(7.85,5.75),(8.55,4.7),'1 : N')
ax.plot([4.5,4.15,4.15],[6.1,6.1,1.4],color='#666666',linewidth=1.1);arrow(ax,(4.15,1.4),(4.5,1.4))
ax.text(4.04,2.7,'contract_id',rotation=90,fontsize=9,ha='center',color='#666666')
ax.text(.4,.1,'Chi tiết bảng, khóa và check constraint nằm trong src/main/resources/db/migration.',fontsize=10,color='#666666')
save(fig,'data-model')
print('Created docs/diagrams (PNG and SVG)')
