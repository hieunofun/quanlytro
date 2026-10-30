import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {mkdir,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
const base=process.env.BASE_URL||'http://127.0.0.1:8091';
const output=fileURLToPath(new URL('../../.runtime/implementation-49/browser',import.meta.url));await mkdir(output,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1000}});const failures=[];page.on('pageerror',e=>failures.push(e.message));
try{
  await page.goto(base);await page.getByLabel('Tên đăng nhập').fill('admin');await page.getByLabel('Mật khẩu',{exact:true}).fill(process.env.ADMIN_PASSWORD||'ReviewAdmin49#2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.locator('.sidebar').waitFor();
  const token=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('session')).accessToken);
  const call=async(path,body)=>{const response=await fetch(base+'/api'+path,{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},body:JSON.stringify(body)});const data=await response.json();assert.equal(response.status,200,JSON.stringify(data));return data;};
  const suffix=Date.now().toString().slice(-8),code='BROWSER49-'+suffix;
  const property=await call('/properties',{code:'B49-'+suffix,name:'Nhà thử nghiệm '+suffix,address:'Dữ liệu kiểm thử riêng'});
  const room=await call('/rooms',{propertyId:property.id,code:'101',price:3100000,maxOccupants:3});
  const person=await call('/tenants',{fullName:'Người thuê thử '+suffix});
  await call('/contracts',{code,roomId:room.id,tenantId:person.id,startDate:'2026-01-01',endDate:'2027-01-01',rentalPrice:3100000,deposit:1000000,paymentDay:5});
  const nav=async name=>{await page.locator('nav').getByRole('button',{name,exact:true}).click();await page.getByRole('heading',{level:1,name,exact:true}).waitFor();await page.getByText('Đang tải dữ liệu…',{exact:true}).waitFor({state:'hidden'});};
  const open=async()=>{await nav('Hợp đồng');await page.getByRole('row').filter({has:page.getByText(code,{exact:true})}).getByRole('button',{name:'Xem',exact:true}).click();await page.getByRole('heading',{name:'Ký kết, bàn giao và tiền đang giữ'}).waitFor();};
  const save=async()=>{await page.getByRole('dialog').getByRole('button',{name:/Lưu|Xác nhận/}).click();await page.getByRole('dialog').waitFor({state:'hidden'});};
  await open();await page.getByRole('button',{name:'Ghi nhận đã ký',exact:true}).click();await page.getByLabel('Ngày giờ thực tế ký (giờ Việt Nam)').fill('2025-12-31T10:00');await save();
  await open();await page.getByRole('button',{name:'Thu cọc / trả trước'}).click();await page.getByLabel('Quỹ hoặc tài khoản nhận').selectOption({index:1});await page.getByLabel('Phân loại tiền').selectOption('DEPOSIT');await page.getByRole('spinbutton',{name:'Tiền thực nhận *',exact:true}).fill('1000000');await page.getByLabel('Thời điểm nhận (giờ Việt Nam)').fill('2026-01-01T09:00');await page.getByLabel('Đã kiểm tra tiền thực vào quỹ').check();await save();
  await open();await page.getByRole('button',{name:'Giao phòng',exact:true}).click();await page.getByLabel('Thời điểm giao (giờ Việt Nam)').fill('2026-01-01T10:00');await save();
  await open();assert.equal(await page.getByText('Chưa ghi nhận người thực tế cư trú. Người đứng tên không được tự tính là đang ở.').count(),1);await page.screenshot({path:output+'/contract-funds.png',fullPage:true});
  for(const name of ['Khu nhà','Phòng','Người thuê','Hợp đồng','Điện nước','Hóa đơn','Sự cố','Thiết lập','Vận hành']){await nav(name);assert.equal(await page.locator('.banner.error').count(),0,'Error on '+name);}
  await page.screenshot({path:output+'/operations-desktop.png',fullPage:true});
  await page.setViewportSize({width:390,height:844});await page.screenshot({path:output+'/operations-mobile.png',fullPage:true});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true,'Page overflows mobile viewport');
  assert.deepEqual(failures,[]);const report={passed:true,checks:['login','create draft','record signed date','receive real deposit','take physical possession','no implicit resident','nine navigation screens','mobile viewport','no JavaScript page errors'],screenshots:['contract-funds.png','operations-desktop.png','operations-mobile.png']};await writeFile(output+'/result.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
}catch(error){await page.screenshot({path:output+'/failure.png',fullPage:true});console.error(JSON.stringify({pageErrors:failures,text:(await page.locator('body').innerText()).slice(-5500)}));throw error;}finally{await browser.close();}
