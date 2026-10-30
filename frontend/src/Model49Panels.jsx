import React,{useState,useEffect} from 'react';
import {api,getSession} from './api';
const field=(name,label,type='text',extra={})=>({name,label,type,...extra});
const money=n=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(n||0);
const label=r=>r.full_name||r.name||r.room_code||r.contract_code||r.meter_code||r.asset_code||r.reservation_code||r.title||r.fund_code||`#${r.id}`;

export function Contract49Panel({contract:c,tenant,showForm,run}){
  const [holds,setHolds]=useState({});
  const endpoint='/contracts/'+c.id;
  const receipt=()=>run(async()=>{const accounts=await api('/cash-accounts');showForm('Ghi tiền thực nhận',[
    field('cashAccountId','Quỹ hoặc tài khoản nhận','select',{options:accounts.filter(a=>a.is_active&&(!a.property_id||a.property_id===c.property_id)).map(a=>[a.id,a.name])}),
    field('fundType','Phân loại tiền','select',{options:[['DEPOSIT','Cọc'],['ADVANCE','Trả trước']]}),field('amount','Tiền thực nhận','number'),field('occurredAt','Thời điểm nhận (giờ Việt Nam)','datetime-local'),field('code','Mã phiếu thu'),field('confirmed','Đã kiểm tra tiền thực vào quỹ','boolean',{optional:true}),field('note','Ghi chú','textarea',{optional:true}),
  ],'/cash-transactions','POST',{contractId:c.id,code:'RC-'+Date.now()});});
  const refund=f=>run(async()=>{const accounts=await api('/cash-accounts');showForm('Ghi nhận tiền đã hoàn',[
    field('cashAccountId','Quỹ hoặc tài khoản chi','select',{options:accounts.filter(a=>a.is_active).map(a=>[a.id,a.name])}),field('amount','Số tiền hoàn thực tế','number',{max:f.available}),field('occurredAt','Thời điểm chi (giờ Việt Nam)','datetime-local'),field('code','Mã phiếu chi'),field('paid','Đã chi tiền thành công','boolean',{optional:true}),field('reason','Căn cứ hoàn tiền','textarea'),
  ],'/fund-accounts/'+f.id+'/refund','POST',{code:'RF-'+Date.now()});});
  return <section className="card">
    <h2>Ký kết, bàn giao và tiền đang giữ</h2>
    <p className="muted">Cọc cam kết {money(c.deposit_amount)}. Chỉ phiếu thu đã xác nhận mới tạo tiền giữ; phân bổ hoặc cấn cọc không tạo thêm doanh thu tiền mặt.</p>
    {!tenant&&<div className="actions">
      {['DRAFT','PENDING'].includes(c.status)&&<button onClick={()=>showForm('Ghi nhận hợp đồng đã ký',[field('signedAt','Ngày giờ thực tế ký (giờ Việt Nam)','datetime-local')],endpoint+'/activate')}>Ghi nhận đã ký</button>}
      {c.status==='ACTIVE'&&!c.possessions?.some(p=>!p.returned_at)&&<button onClick={()=>showForm('Giao phòng thực tế',[field('takenAt','Thời điểm giao (giờ Việt Nam)','datetime-local'),field('reason','Căn cứ giao, hoặc căn cứ cho thiếu cọc','textarea',{optional:true})],endpoint+'/possessions')}>Giao phòng</button>}
      <button onClick={receipt}>Thu cọc / trả trước</button>
      {getSession().user.role==='ADMIN'&&c.reconciliation_status==='PENDING'&&<button onClick={()=>showForm('Xác minh hợp đồng chuyển từ dữ liệu cũ',[field('confirmed','Đã kiểm tra hồ sơ gốc','boolean'),field('reason','Kết quả đối soát','textarea')],endpoint+'/reconcile')}>Đối soát hồ sơ cũ</button>}
      <button onClick={()=>showForm('Ghi đề nghị trả phòng',[field('requestedDate','Ngày đề nghị trả','date'),field('reason','Lý do','textarea')],endpoint+'/move-out-notices')}>Đề nghị trả</button>
    </div>}
    {c.possessions?.map(p=><p key={p.id}>Đã nhận phòng lúc {new Date(p.taken_at).toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'})}{p.returned_at?' · Đã trả '+new Date(p.returned_at).toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'}):' · Chưa giao trả'}</p>)}
    {c.funds?.map(f=><article className="metric" key={f.id}>
      <div><strong>{f.fund_type==='DEPOSIT'?'Tiền cọc':'Tiền trả trước'}</strong><small>Số dư {money(f.balance)} · Tạm giữ {money(f.held_amount)} · Có thể dùng {money(f.available)}{f.reconciliation_status==='PENDING'?' · Chưa đối soát':''}</small></div>
      {!tenant&&<div className="actions">
        {f.reconciliation_status==='VERIFIED'&&Number(f.available)>0&&<button onClick={()=>refund(f)}>Ghi đã hoàn tiền</button>}
        {getSession().user.role==='ADMIN'&&f.reconciliation_status==='PENDING'&&<button onClick={()=>showForm('Xác nhận số dư có bằng chứng',[field('verifiedBalance','Số dư cọc thực còn giữ','number'),field('reason','Biên nhận / xác nhận đối soát','textarea')],'/fund-accounts/'+f.id+'/reconcile')}>Đối soát số dư</button>}
        <button onClick={()=>showForm('Tạm giữ một phần để đối soát',[field('amount','Số tiền tạm giữ','number'),field('reviewOn','Ngày phải rà soát lại','date'),field('reason','Khoản chưa rõ và căn cứ tạm giữ','textarea')],'/fund-accounts/'+f.id+'/holds')}>Tạm giữ</button>
        <button onClick={()=>run(async()=>{const data=await api('/fund-accounts/'+f.id+'/holds');setHolds(v=>({...v,[f.id]:data}));})}>Xem khoản giữ</button>
      </div>}
    </article>)}
    {!tenant&&Object.values(holds).flat().map(h=><p key={h.id}>{money(h.amount)} · {h.reason} · Rà lại {h.review_on} {h.released_at?'· Đã giải tỏa':<button onClick={()=>showForm('Giải tỏa khoản tạm giữ',[field('reason','Kết quả đối soát','textarea')],'/fund-holds/'+h.id+'/release')}>Giải tỏa</button>}</p>)}
    {!!c.versions?.length&&<details><summary>Lịch sử điều kiện đã ký</summary>{c.versions.map(v=><p key={v.id}>Bản {v.version_no}: {v.effective_from} → {v.lease_end} · {money(v.rental_price)}/tháng · {v.origin==='LEGACY_V10'?'Dữ liệu cũ chưa có ngày ký':v.signed_at||'Chưa ký'}</p>)}</details>}
  </section>;
}

const schemas={
  reservations:['Giữ chỗ',[['roomId','Phòng','rooms'],['tenantId','Người giữ chỗ','tenants',true],['reservationCode','Mã giữ chỗ'],['contactName','Người liên hệ'],['contactPhone','Điện thoại'],['startDate','Ngày dự kiến vào','date'],['endDate','Ngày hết khoảng giữ','date'],['expiresAt','Hết thời hạn giữ (giờ Việt Nam)','datetime-local'],['conditions','Điều kiện hoàn / mất tiền','textarea']]],
  'asset-types':['Loại tài sản',[['code','Mã loại'],['name','Tên loại'],['unitName','Đơn vị']]],
  assets:['Tài sản',[['assetTypeId','Loại tài sản','asset-types'],['assetCode','Mã tài sản'],['quantity','Số lượng','number'],['note','Ghi chú','textarea',true]]],
  'asset-placements':['Bố trí tài sản',[['assetId','Tài sản','assets'],['roomId','Phòng (chọn phòng hoặc khu)','rooms',true],['propertyId','Khu vực chung','properties',true],['validFrom','Bắt đầu bố trí','datetime-local']]],
  meters:['Công tơ vật lý',[['roomId','Phòng','rooms'],['serviceItemId','Dịch vụ','services'],['meterCode','Mã công tơ'],['installedAt','Thời điểm lắp (giờ Việt Nam)','datetime-local']]],
  subscriptions:['Đăng ký dịch vụ',[['contractId','Hợp đồng','contracts'],['serviceItemId','Dịch vụ','services'],['effectiveFrom','Bắt đầu tính','date'],['effectiveTo','Ngừng tính','date',true],['quantity','Số lượng đăng ký','number']]],
  'maintenance-schedules':['Lịch bảo trì',[['roomId','Phòng','rooms',true],['propertyId','Khu vực chung','properties',true],['title','Nội dung'],['intervalDays','Lặp sau số ngày','number'],['nextDue','Lần đến hạn','date']]],
  handovers:['Biên bản bàn giao',[['contractId','Hợp đồng','contracts'],['handoverCode','Mã biên bản'],['handoverType','Loại: MOVE_IN / MOVE_OUT / RESPONSIBILITY'],['occurredAt','Thời điểm thực tế (giờ Việt Nam)','datetime-local'],['note','Nội dung','textarea']]],
  'handover-items':['Chi tiết bàn giao',[['handoverId','Biên bản','handovers'],['lineNo','Số thứ tự','number'],['itemNameSnapshot','Tên tài sản tại lúc giao'],['quantity','Số lượng','number'],['conditionNote','Hiện trạng ghi nhận','textarea']]],
  'staff-permissions':['Ủy quyền nhân viên',[['assignmentId','Phân công','assignments'],['actionCode','Quyền: REFUND / TRANSFER / ADJUST / DEPOSIT_OFFSET / WAIVE_DEPOSIT'],['amountLimit','Hạn mức mỗi thao tác','number',true],['expiresAt','Hết hạn ủy quyền','datetime-local',true]]],
};
export function Workflow49Panel({showForm,run}){
  const [resource,setResource]=useState('meters'),[rows,setRows]=useState([]),[error,setError]=useState(''),[loading,setLoading]=useState(false);
  const admin=getSession().user.role==='ADMIN';
  const load=async()=>{setLoading(true);setError('');try{setRows(await api('/catalog/'+resource));}catch(e){setError(e.message);}finally{setLoading(false);}};
  useEffect(()=>{load();},[resource]);
  const create=()=>run(async()=>{const definition=schemas[resource][1];const fields=[];for(const [name,title,type='text',optional=false]of definition){if(schemas[type]||['rooms','properties','services','contracts','tenants','assignments'].includes(type)){const list=await api(type==='contracts'?'/contracts':'/catalog/'+type);fields.push(field(name,title,'select',{optional,options:list.map(r=>[r.id,label(r)])}));}else fields.push(field(name,title,type,{optional}));}showForm('Thêm '+schemas[resource][0].toLowerCase(),fields,'/catalog/'+resource,'POST',{},load);});
  return <section className="card"><h2>Vận hành</h2><div className="toolbar"><label>Nhóm nghiệp vụ <select value={resource} onChange={e=>setResource(e.target.value)}>{Object.entries(schemas).filter(([key])=>admin||!['asset-types','assets','staff-permissions'].includes(key)).map(([key,[name]])=><option key={key} value={key}>{name}</option>)}</select></label><button onClick={create}>Thêm bản ghi</button><button onClick={load}>Làm mới</button></div>
    {error&&<p className="banner error" role="alert">{error}</p>}{loading?<p role="status">Đang tải…</p>:rows.length===0?<p className="muted">Chưa có bản ghi.</p>:<div className="table-wrap"><table><thead><tr><th>Mã / nội dung</th><th>Thời gian / trạng thái</th><th>Thao tác</th></tr></thead><tbody>{rows.map(r=><tr key={r.id}><td>{label(r)}</td><td>{r.status||r.next_due||r.effective_from||r.installed_at||r.valid_from||'—'}</td><td>
      {resource==='meters'&&<button onClick={()=>showForm('Thay công tơ',[field('replacedAt','Thời điểm thay','datetime-local'),field('closingIndex','Chỉ số chốt công tơ cũ','number'),field('newMeterCode','Mã công tơ mới'),field('openingIndex','Chỉ số đầu công tơ mới','number'),field('reason','Căn cứ thay','textarea')],'/meters/'+r.id+'/replace','POST',{},load)}>Thay công tơ</button>}
      {resource==='maintenance-schedules'&&<button onClick={()=>showForm('Tạo phiếu bảo trì cho lần đến hạn',[],'/maintenance-schedules/'+r.id+'/complete','POST',{},load)}>Tạo phiếu đến hạn</button>}
      {resource==='handovers'&&r.status==='DRAFT'&&<button onClick={()=>showForm('Xác nhận biên bản',[field('confirmedAt','Thời điểm xác nhận','datetime-local'),field('reason','Căn cứ xác nhận','textarea')],'/catalog/handovers/'+r.id,'PUT',{status:'CONFIRMED'},load)}>Xác nhận</button>}
      {resource==='reservations'&&<button onClick={()=>showForm('Mở quỹ theo dõi tiền giữ chỗ',[],'/reservations/'+r.id+'/fund','POST',{},load)}>Mở quỹ giữ chỗ</button>}
    </td></tr>)}</tbody></table></div>}
    <p className="muted">Giữ chỗ và biên bản không tự ghi thu/chi. Thông tin đã ký, phát hành hoặc xác nhận được giữ lịch sử.</p>
  </section>;
}
