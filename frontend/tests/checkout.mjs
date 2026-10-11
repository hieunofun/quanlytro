import {chromium} from "playwright";
import assert from "node:assert/strict";
import {mkdir} from "node:fs/promises";
import path from "node:path";

// Checkout is irreversible through the API. Run this suite against a disposable
// database only; it deliberately leaves its fixtures for inspection.
assert.equal(process.env.E2E_ISOLATED_DATABASE,"true","Use a disposable database and set E2E_ISOLATED_DATABASE=true");
const base=process.env.BASE_URL;
assert.ok(base && process.env.ADMIN_PASSWORD,"BASE_URL and ADMIN_PASSWORD are required");
const browser=await chromium.launch({channel:"chrome",headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[];
page.on("pageerror",e=>errors.push(e.message));
const output=path.resolve("../docs/screenshots");await mkdir(output,{recursive:true});
let token;
async function login(username,password) {
  await page.getByLabel("Tên đăng nhập").fill(username);
  await page.getByLabel("Mật khẩu",{exact:true}).fill(password);
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await page.locator(".sidebar").waitFor();
  token=await page.evaluate(()=>JSON.parse(sessionStorage.getItem("session")).accessToken);
}
async function request(endpoint,data,method) {
  const r=await page.request.fetch(base+"/api"+endpoint,{method:method||(data?"POST":"GET"),headers:{Authorization:"Bearer "+token},...(data?{data}:{})});
  assert.ok(r.ok(),await r.text());return r.json();
}
async function contracts() {
  await page.locator("nav").getByRole("button",{name:"Hợp đồng",exact:true}).click();
  await page.getByRole("heading",{name:"Hợp đồng",exact:true,level:1}).waitFor();
  await page.getByText("Đang tải dữ liệu…",{exact:true}).waitFor({state:"hidden"});
}
async function open(code) {
  await page.getByRole("row").filter({hasText:code}).getByRole("button",{name:"Xem",exact:true}).click();
}
async function checkout(code) {
  await contracts();await open(code);
  await page.getByRole("button",{name:"Trả phòng",exact:true}).click();
  await page.getByLabel(/^Ngày trả phòng/).fill("2026-01-16");
  await page.getByLabel("Chỉ số điện cuối").fill("20");
  await page.getByLabel("Chỉ số nước cuối").fill("2");
  await page.getByLabel("Khấu trừ thiệt hại").fill(code.includes("DEBT")?"2500000":"0");
  await page.getByRole("button",{name:"Lưu",exact:true}).click();
  await page.getByRole("dialog").waitFor({state:"hidden"});
  await page.getByRole("heading",{name:"Quyết toán trả phòng",exact:true}).waitFor();
  assert.equal(await page.evaluate(()=>window.scrollY),0,"Opening settlement starts at the top of the detail page");
}
async function checkAmount(label,number) {
  const row=page.getByRole("region",{name:"Quyết toán trả phòng"}).locator("dl > div").filter({has:page.locator("dt",{hasText:label})});
  assert.equal(await row.locator("dd").innerText(),new Intl.NumberFormat("vi-VN",{style:"currency",currency:"VND",maximumFractionDigits:0}).format(number));
}
try {
  await page.goto(base);await login("admin",process.env.ADMIN_PASSWORD);
  const key=Date.now().toString(),password="CheckoutE2e#2026",username="checkout_"+key;
  const user=await request("/users",{username,password,fullName:"Khách kiểm thử quyết toán",phone:"U"+key,email:null,role:"TENANT"});
  const property=await request("/properties",{code:"E2E-"+key,name:"Kiểm thử trả phòng",address:"Database kiểm thử"});
  const floor=await request("/floors",{propertyId:property.id,number:1,name:"Tầng 1"});
  for(const service of await request("/services")) {
    const price={ELEC:3500,WATER:20000,TRASH:31000,PARKING:31000}[service.service_code];
    if(price!==undefined)await request("/prices",{propertyId:property.id,serviceId:service.id,unitPrice:price,effectiveFrom:"2026-01-01"});
  }
  const fixtures=[];
  for(const kind of ["REFUND","DEBT","ZERO"]) {
    const room=await request("/rooms",{propertyId:property.id,floorId:floor.id,code:kind,price:3100000,maxOccupants:2,area:30});
    const tenant=await request("/tenants",{...(kind==="REFUND"?{userId:user.id}:{}),fullName:"Khách "+kind,identityNumber:kind+key,phone:"0900000000",email:null});
    const code=kind+"-"+key;
    const contract=await request("/contracts",{code,roomId:room.id,tenantId:tenant.id,startDate:"2026-01-01",endDate:"2027-01-01",rentalPrice:3100000,deposit:kind==="ZERO"?1640000:3100000,paymentDay:5,electricIndex:0,waterIndex:0});
    fixtures.push({code,contract});
  }
  await checkout(fixtures[0].code);await checkAmount("Tiền hoàn cọc",1460000);await checkAmount("Điện nước kỳ cuối",110000);
  const settled=await request("/contracts/"+fixtures[0].contract.id);
  assert.equal(settled.status,"TERMINATED");assert.equal(settled.checkout.actual_refund_amount,1460000);
  await page.screenshot({path:path.join(output,"checkout-desktop.png"),fullPage:true});
  await page.reload();await page.locator(".sidebar").waitFor();await contracts();await open(fixtures[0].code);await checkAmount("Tiền hoàn cọc",1460000);
  await page.setViewportSize({width:375,height:812});
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.screenshot({path:path.join(output,"checkout-mobile.png"),fullPage:true});
  await page.setViewportSize({width:1440,height:1000});
  await checkout(fixtures[1].code);await checkAmount("Cần thu thêm",1040000);
  await checkout(fixtures[2].code);await checkAmount("Tiền hoàn cọc",0);
  await page.getByRole("button",{name:"Đăng xuất",exact:true}).click();await login(username,password);
  await contracts();await open(fixtures[0].code);await checkAmount("Tiền hoàn cọc",1460000);
  assert.equal(await page.getByRole("button",{name:"Trả phòng",exact:true}).count(),0);
  assert.equal(await page.getByRole("row").filter({hasText:fixtures[1].code}).count(),0);
  assert.deepEqual(errors,[]);
  console.log(JSON.stringify({status:"PASS",features:["checkout immediately shows settlement","refund persists after reload","additional debt shown as positive amount","zero refund","tenant own settlement"],widths:[1440,375],errors},null,2));
} finally {await browser.close();}
