import assert from 'node:assert/strict';
import {mkdir,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';

// Creates fictional review data through real HTTP APIs. No SQL writes or mocked responses.
const base=(process.env.BASE_URL||'http://127.0.0.1:8093').replace(/\/$/,'');
assert.match(base,/^http:\/\/(127\.0\.0\.1|localhost):\d+$/,'Use a separate local review server');
const root=fileURLToPath(new URL('../',import.meta.url));
const output=root+'docs/backend-review/';await mkdir(output,{recursive:true});
const suffix=Date.now().toString().slice(-10),prefix='BE-'+suffix;
const transcript=[];let token;
async function call(name,path,body,method=body===undefined?'GET':'POST',expected=200,auth=token){
  const response=await fetch(base+'/api'+path,{method,headers:{'Content-Type':'application/json',...(auth?{Authorization:'Bearer '+auth}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})});
  const data=await response.json();
  if(!path.startsWith('/auth/'))transcript.push({name,request:{method,path:'/api'+path,...(body===undefined?{}:{body})},status:response.status,response:data});
  assert.equal(response.status,expected,name+': '+JSON.stringify(data));return data;
}
const money=(actual,expected,label)=>assert.equal(Number(actual),expected,label);
const login=await call('Login','/auth/login',{username:'admin',password:process.env.ADMIN_PASSWORD||'ReviewAdmin49#2026'},'POST',200,null);token=login.accessToken;
const property=await call('Tạo khu nhà thử','/properties',{code:prefix,name:'Khu kiểm tra backend '+suffix,address:'Dữ liệu giả để kiểm tra'});
await call('Ngày chốt điện 31','/properties/'+property.id+'/billing-policy',{meterCutoffDay:31,invoiceIssueDay:1},'PUT');
const accounts=await call('Đọc tài khoản thu','/cash-accounts');const account=accounts.find(a=>a.property_id===property.id);assert.ok(account);
const room=await call('Tạo phòng 101','/rooms',{propertyId:property.id,code:'101',price:3100000,maxOccupants:4,openedOn:'2026-01-01'});
const service=async(code,name,type,price)=>{
  const s=await call('Tạo dịch vụ '+name,'/catalog/services',{serviceCode:prefix+'-'+code,name,unitName:type==='METER_BASED'?'kWh':'Tháng',calculationType:type,isMandatory:false});
  await call('Giá dịch vụ '+name,'/catalog/prices',{propertyId:property.id,serviceItemId:s.id,effectiveFrom:'2026-01-01',unitPrice:price});return s;
};
const defaults=await call('Đọc dịch vụ tham chiếu','/catalog/services');
const elec=defaults.find(s=>s.service_code==='ELEC');assert.ok(elec);
await call('Giá điện tại khu thử','/catalog/prices',{propertyId:property.id,serviceItemId:elec.id,effectiveFrom:'2026-01-01',unitPrice:3500});
const otherMandatory=defaults.filter(s=>s.is_mandatory&&s.is_active&&s.id!==elec.id);
for(const s of otherMandatory)await call('Giá dịch vụ bắt buộc '+s.name,'/catalog/prices',{propertyId:property.id,serviceItemId:s.id,effectiveFrom:'2026-01-01',unitPrice:s.calculation_type==='METER_BASED'?15000:0});
const net=await service('NET','Internet','PER_ROOM',310000),people=await service('PERSON','Dịch vụ theo người','PER_PERSON',31000);
const meter=await call('Tạo công tơ','/catalog/meters',{roomId:room.id,serviceItemId:elec.id,meterCode:prefix+'-M101',installedAt:'2026-01-01T00:00:00+07:00'});
const extraMeters=[];
for(const s of otherMandatory.filter(s=>s.calculation_type==='METER_BASED'))extraMeters.push(await call('Công tơ '+s.name,'/catalog/meters',{roomId:room.id,serviceItemId:s.id,meterCode:prefix+'-EX-'+s.id,installedAt:'2026-01-01T00:00:00+07:00'}));
async function lease(code,person,start,end,rent=3100000,targetRoom=room.id){
  const c=await call('Lập hợp đồng '+code,'/contracts',{code:prefix+'-'+code,roomId:targetRoom,tenant:{fullName:person,identityNumber:prefix+'-'+code},startDate:start,endDate:end,rentalPrice:rent,deposit:2000000,primaryResides:false});
  return call('Ghi nhận ký '+code,'/contracts/'+c.id+'/activate',{signedAt:code==='A'?'2025-12-31T10:00:00+07:00':'2026-01-16T10:00:00+07:00'});
}
async function subscribe(c,start,services=[net,people,elec]){
  const existing=await call('Đọc đăng ký dịch vụ','/catalog/subscriptions');
  for(const s of services)if(!existing.some(x=>x.contract_id===c.id&&x.service_item_id===s.id))await call('Đăng ký '+s.name,'/catalog/subscriptions',{contractId:c.id,serviceItemId:s.id,effectiveFrom:start,chargeMode:'PRORATED',quantity:1});
}
async function receive(c,amount,at,code,kind='DEPOSIT'){
  return call('Thu '+code,'/cash-transactions',{cashAccountId:account.id,contractId:c.id,fundType:kind,code:prefix+'-'+code,amount,method:'CASH',occurredAt:at,confirmed:true});
}
const primary=c=>c.parties.find(p=>p.party_role==='PRIMARY').tenant_id;
const funds=c=>Object.fromEntries(c.funds.map(f=>[f.fund_type,f.id]));
const usage=async(name,c,start,end,oldIndex,newIndex)=>{
  for(const extra of extraMeters)await call('Chỉ số dịch vụ phụ không phát sinh trong dữ liệu thử','/meter-readings',{meterId:extra.id,...(c?{contractId:c.id}:{}),startAt:start,endAt:end,oldIndex:0,newIndex:0});
  return call(name,'/meter-readings',{meterId:meter.id,...(c?{contractId:c.id}:{}),startAt:start,endAt:end,oldIndex,newIndex});
};
const a=await lease('A','Khách A (dữ liệu giả)','2026-01-01','2027-01-01');await subscribe(a,'2026-01-01');
await receive(a,2000000,'2026-01-01T09:00:00+07:00','DEP-A');
await call('Ghi người A thực ở','/contracts/'+a.id+'/members',{tenantId:primary(a),effectiveFrom:'2026-01-01'});
await call('Giao phòng A','/contracts/'+a.id+'/possessions',{takenAt:'2026-01-01T10:00:00+07:00'});
const aUsage=await usage('Điện A: 100 → 130',a,'2026-01-01T10:00:00+07:00','2026-01-16T10:00:00+07:00',100,130);
await call('Trả phòng A','/contracts/'+a.id+'/checkout',{billingEnd:'2026-01-16',returnedAt:'2026-01-16T10:00:00+07:00'});
const aCheckout=await call('Duyệt quyết toán A','/contracts/'+a.id+'/checkout/approve',{reason:'Đã kiểm tra bàn giao và chỉ số cuối',useDeposit:true});money(aCheckout.refund_due,230000,'Refund due A');money(aCheckout.debt_remaining,0,'Debt A');
const refundBody={cashAccountId:account.id,code:prefix+'-REFUND-A',amount:230000,method:'BANK_TRANSFER_MANUAL',externalReference:prefix+'-BANK-OUT-A',occurredAt:'2026-01-16T11:00:00+07:00',paid:true,reason:'Đã kiểm tra chứng từ tiền chi thực tế'};
await call('Ghi hoàn cọc A','/fund-accounts/'+funds(a).DEPOSIT+'/refund',refundBody);
await call('Gửi lại cùng phiếu hoàn','/fund-accounts/'+funds(a).DEPOSIT+'/refund',refundBody);
const ownerUsage=await usage('Điện khi phòng trống: 130 → 132',null,'2026-01-16T10:00:00+07:00','2026-01-17T10:00:00+07:00',130,132);
const operating=await call('Đọc trạng thái vận hành','/catalog/room-operating-periods');const turnover=operating.find(s=>s.room_id===room.id&&s.status==='TURNOVER'&&!s.valid_to);assert.ok(turnover);
await call('Kết thúc chuẩn bị phòng','/catalog/room-operating-periods/'+turnover.id,{validTo:'2026-01-17T10:00:00+07:00',reason:'Đã kiểm tra và vệ sinh phòng'},'PUT');
await call('Phòng sẵn sàng','/catalog/room-operating-periods',{roomId:room.id,validFrom:'2026-01-17T10:00:00+07:00',status:'READY'});
const b=await lease('B','Khách B (dữ liệu giả)','2026-01-17','2027-01-17');await subscribe(b,'2026-01-17');
await receive(b,2000000,'2026-01-17T09:00:00+07:00','DEP-B');
await call('Ghi người B thực ở','/contracts/'+b.id+'/members',{tenantId:primary(b),effectiveFrom:'2026-01-17'});
await call('Giao phòng B','/contracts/'+b.id+'/possessions',{takenAt:'2026-01-17T10:00:00+07:00'});
const bUsage=await usage('Điện B: 132 → 160',b,'2026-01-17T10:00:00+07:00','2026-02-01T00:00:00+07:00',132,160);
const bInvoice=await call('Lập hóa đơn B tháng 1','/invoices',{contractId:b.id,period:'2026-01',issueDate:'2026-02-01'});money(bInvoice.total_amount,1763000,'Invoice B');
await call('Phát hành hóa đơn B','/invoices/'+bInvoice.id+'/issue',{});
const paymentBody={code:prefix+'-PAY-B',amount:1800000,cashAccountId:account.id,method:'CASH',occurredAt:'2026-02-02T10:00:00+07:00'};
await call('Khách B nộp 1.800.000','/invoices/'+bInvoice.id+'/payments',paymentBody);
await call('Gửi lại phiếu thu B','/invoices/'+bInvoice.id+'/payments',paymentBody);
const bAdvance=await call('Tiền dư B','/fund-accounts/'+funds(b).ADVANCE);money(bAdvance.balance,37000,'Advance B');
const aStatement=await call('Bảng quyết toán A','/contracts/'+a.id+'/settlement');const bFinal=await call('Hóa đơn B sau thu','/invoices/'+bInvoice.id);money(bFinal.balance_due,0,'Debt B');
const chargesA=aStatement.charges.reduce((sum,c)=>sum+Number(c.charge_amount),0);money(chargesA,1770000,'Charges A');money(aStatement.checkout.actual_refund_amount,230000,'Actual refund A');
money(aUsage.consumption,30,'A kWh');money(ownerUsage.consumption,2,'Owner kWh');money(bUsage.consumption,28,'B kWh');

// Independent paid-meter correction: no later interval depends on this endpoint.
const correctionRoom=await call('Phòng 201 thử điều chỉnh','/rooms',{propertyId:property.id,code:'201',price:0,maxOccupants:2,openedOn:'2026-01-01'});
const c=await lease('CORR','Khách C (dữ liệu giả)','2026-01-01','2027-01-01',0,correctionRoom.id);await subscribe(c,'2026-01-01',[]);
await receive(c,2000000,'2026-01-01T09:00:00+07:00','DEP-C');await call('Giao phòng C','/contracts/'+c.id+'/possessions',{takenAt:'2026-01-01T10:00:00+07:00'});
const cm=await call('Công tơ phòng 201','/catalog/meters',{roomId:correctionRoom.id,serviceItemId:elec.id,meterCode:prefix+'-M201',installedAt:'2026-01-01T00:00:00+07:00'});
const cs=await call('Điện ban đầu 50 kWh','/meter-readings',{meterId:cm.id,contractId:c.id,startAt:'2026-01-01T10:00:00+07:00',endAt:'2026-02-01T00:00:00+07:00',oldIndex:0,newIndex:50});
const ci=await call('Chứng từ điện C','/billing-documents',{contractId:c.id,code:prefix+'-ELEC-C',documentType:'FINAL_CHARGE',items:[{name:'Điện',itemKind:'UTILITY',unit:'kWh',unitPrice:3500,calculationBasis:'METER',segmentId:cs.id,serviceFrom:'2026-01-01',serviceTo:'2026-02-01'}]});
await call('Phát hành điện C','/billing-documents/'+ci.id+'/issue',{});await call('Thu đủ điện C','/invoices/'+ci.id+'/payments',{code:prefix+'-PAY-C',amount:175000,cashAccountId:account.id,occurredAt:'2026-02-02T10:00:00+07:00'});
const correctionBody={code:prefix+'-CORR-C',correctedClosingIndex:40,reason:'Đã đối chiếu ảnh công tơ, số đúng là 40'};
const adjustment=await call('Sửa còn 40 kWh','/billing-items/'+ci.items[0].id+'/meter-corrections',correctionBody);money(adjustment.total_amount,-35000,'Correction');
const corrected=await call('Hóa đơn gốc sau điều chỉnh','/invoices/'+ci.id);money(corrected.total_amount,140000,'Corrected charge');
const cAdvance=await call('Phần phân bổ dư C','/fund-accounts/'+funds(c).ADVANCE);money(cAdvance.balance,35000,'Restored advance C');
await call('Không cho ghi đè dòng đã phát hành','/billing-documents/'+ci.id+'/items',{name:'Không hợp lệ',unitPrice:1},'POST',400);
await call('Không token','/invoices/'+ci.id,undefined,'GET',401,null);
const allCash=await call('Phiếu tiền thật','/cash-transactions');const cash=allCash.filter(t=>t.cash_account_id===account.id&&t.status==='CONFIRMED');
const netCash=cash.reduce((sum,t)=>sum+(t.direction==='IN'?1:-1)*Number(t.amount),0);money(netCash,7745000,'Property actual cash');
const evidence={validated_on:'2026-11-01',fictional_data:true,base_url:base,prefix,property_id:property.id,room_id:room.id,
  contracts:{a:a.id,b:b.id,c:c.id},invoices:{b:bInvoice.id,c:ci.id},funds:{bAdvance:funds(b).ADVANCE,cAdvance:funds(c).ADVANCE},
  turnover:{old_tenant_days:15,vacant_days:1,new_tenant_days:15,old_tenant_total:chargesA,new_tenant_total:Number(bInvoice.total_amount),old_tenant_refund:230000,new_tenant_advance:37000,electricity_kwh:{old:30,owner:2,new:28},cash_net_before_correction_case:5570000},
  correction:{original_usage:50,verified_usage:40,rate:3500,credit:35000,corrected_charge:140000,restored_advance:35000},
  cash_in:cash.filter(t=>t.direction==='IN').reduce((s,t)=>s+Number(t.amount),0),cash_out:cash.filter(t=>t.direction==='OUT').reduce((s,t)=>s+Number(t.amount),0),cash_net:netCash,
  requests:transcript.length,all_assertions_passed:true};
await writeFile(output+'demo-evidence.json',JSON.stringify(evidence,null,2)+'\n');
await writeFile(output+'api-transcript.json',JSON.stringify(transcript,null,2)+'\n');
const request=(name,path,body,method=body===undefined?'GET':'POST')=>({name,request:{method,header:[{key:'Content-Type',value:'application/json'}],url:'{{baseUrl}}/api'+path,...(body===undefined?{}:{body:{mode:'raw',raw:JSON.stringify(body,null,2)}})}});
const authRequest=request('Đăng nhập','/auth/login',{username:'admin',password:'{{password}}'});authRequest.request.auth={type:'noauth'};authRequest.event=[{listen:'test',script:{exec:['pm.test("HTTP 200", () => pm.response.to.have.status(200));','pm.collectionVariables.set("token", pm.response.json().accessToken);']}}];
const collection={info:{name:'Backend nhà trọ — dữ liệu đã kiểm chứng 01-11-2026',schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},auth:{type:'bearer',bearer:[{key:'token',value:'{{token}}',type:'string'}]},variable:[{key:'baseUrl',value:base},{key:'password',value:'ReviewAdmin49#2026'},{key:'token',value:''}],item:[authRequest,
  request('Health','/health'),request('Hợp đồng A','/contracts/'+a.id),request('Quyết toán A','/contracts/'+a.id+'/settlement'),request('Hóa đơn B','/invoices/'+bInvoice.id),request('Tiền dư B','/fund-accounts/'+funds(b).ADVANCE),
  request('Gửi lại phiếu thu B','/invoices/'+bInvoice.id+'/payments',paymentBody),request('Gửi lại phiếu hoàn A','/fund-accounts/'+funds(a).DEPOSIT+'/refund',refundBody),request('Hóa đơn điện C sau điều chỉnh','/invoices/'+ci.id),request('Điều chỉnh đã lập','/billing-documents/'+adjustment.id),request('Tiền phân bổ dư C','/fund-accounts/'+funds(c).ADVANCE)]};
await writeFile(output+'backend.postman_collection.json',JSON.stringify(collection,null,2)+'\n');
console.log(JSON.stringify(evidence,null,2));
