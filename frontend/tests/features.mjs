import {chromium} from "playwright";
import assert from "node:assert/strict";
import path from "node:path";
const base=process.env.BASE_URL||"http://localhost:8080";
const browser=await chromium.launch({channel:"chrome",headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[];
page.on("pageerror",e=>errors.push(e.message));
const output=path.resolve("../docs/screenshots");
let staffId,ownerToken,assignment,uploadedImage,fixtureRoomId;
async function login(username,password) {
  await page.getByLabel("Tên đăng nhập").fill(username);await page.getByLabel("Mật khẩu",{exact:true}).fill(password);
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();await page.locator(".sidebar").waitFor();
}
async function nav(name) {await page.locator("nav").getByRole("button",{name,exact:true}).click();await page.getByRole("heading",{name,exact:true,level:1}).waitFor();await page.getByText("Đang tải dữ liệu…",{exact:true}).waitFor({state:"hidden"});}
async function request(endpoint,method="GET",data) {
  const r=await page.request.fetch(base+"/api"+endpoint,{method,headers:{Authorization:"Bearer "+ownerToken},...(data===undefined?{}:{data})});
  assert.ok(r.ok(),await r.text());return r.json();
}
try {
  await page.goto(base);await login("admin",process.env.ADMIN_PASSWORD||"DemoAdmin#2026");
  ownerToken=await page.evaluate(()=>JSON.parse(sessionStorage.getItem("session")).accessToken);
  const rooms=await request("/rooms"),room=rooms.find(r=>r.room_code==="203" && r.property_name==="Nhà trọ An Phú"),free=rooms.find(r=>r.room_code==="204" && r.property_id===room.property_id);
  assert.ok(room && free,"Demo rooms required");
  fixtureRoomId=room.id;
  await nav("Phòng");await page.getByRole("row").filter({hasText:"203"}).first().getByRole("button",{name:"Xem",exact:true}).click();
  const before=await request("/rooms/"+room.id+"/images");
  await page.getByLabel("Thêm ảnh từ máy").setInputFiles(path.resolve("tests/fixtures/photo.png"));
  await page.getByText("Đã tải xong",{exact:true}).waitFor();
  await page.getByAltText("Ảnh phòng 203").first().waitFor();
  uploadedImage=(await request("/rooms/"+room.id+"/images")).find(i=>!before.some(b=>b.id===i.id));assert.ok(uploadedImage);
  await page.screenshot({path:path.join(output,"room-upload-desktop.png"),fullPage:true});
  await nav("Hợp đồng");await page.getByRole("row").filter({hasText:"203"}).first().getByRole("button",{name:"Xem",exact:true}).click();
  await page.getByRole("button",{name:"Ghi nhận vi phạm",exact:true}).click();
  const reason="Demo quy trình xử lý "+Date.now();await page.getByLabel("Nội dung và lý do vi phạm").fill(reason);await page.getByRole("button",{name:"Lưu",exact:true}).click();await page.getByRole("dialog").waitFor({state:"hidden"});
  await page.getByRole("row").filter({hasText:"203"}).first().getByRole("button",{name:"Xem",exact:true}).click();
  const violation=page.locator(".violation").filter({hasText:reason});await violation.getByRole("button",{name:"Xử lý vi phạm"}).click();
  await page.getByLabel("Trạng thái").selectOption("RESOLVED");await page.getByLabel("Nội dung xử lý / lý do").fill("Đã kiểm tra và khắc phục trong kịch bản demo");await page.getByRole("button",{name:"Lưu",exact:true}).click();await page.getByRole("dialog").waitFor({state:"hidden"});
  await page.getByRole("row").filter({hasText:"203"}).first().getByRole("button",{name:"Xem",exact:true}).click();await page.locator(".violation").filter({hasText:reason}).getByText("Đã giải quyết",{exact:true}).waitFor();
  await page.screenshot({path:path.join(output,"contract-violation-desktop.png"),fullPage:true});
  const contract=(await request("/contracts")).find(c=>c.room_id===room.id && c.status==="ACTIVE");
  await nav("Hóa đơn");await page.getByRole("button",{name:"Tạo hóa đơn",exact:true}).click();await page.getByLabel(/^Hợp đồng/).selectOption(String(contract.id));
  await page.getByLabel("Kỳ hóa đơn").fill("2026-09");await page.getByText(/Chốt số từ 2026-09-28.*Phát hành 2026-10-01.*2026-10-05/).waitFor();await page.getByRole("button",{name:"Hủy",exact:true}).click();
  const username="room_e2e_"+Date.now();staffId=(await request("/users","POST",{username,password:"RoomE2eTest#2026",fullName:"Nhân viên demo một phòng",phone:"E"+Date.now(),email:null,role:"STAFF"})).id;
  await nav("Thiết lập");await page.getByRole("button",{name:"Phân công",exact:true}).click();await page.getByRole("combobox",{name:/^Nhân viên/}).selectOption(String(staffId));await page.getByRole("combobox",{name:/^Khu nhà/}).selectOption(String(room.property_id));await page.getByRole("combobox",{name:/^Phòng \(để trống/}).selectOption(String(free.id));await page.getByRole("button",{name:"Lưu",exact:true}).click();await page.getByRole("dialog").waitFor({state:"hidden"});assignment={userId:staffId,propertyId:room.property_id,roomId:free.id,active:false};
  await page.getByRole("button",{name:"Đăng xuất",exact:true}).click();await login(username,"RoomE2eTest#2026");
  await nav("Phòng");await page.getByRole("row").filter({hasText:"204"}).waitFor();assert.equal(await page.getByRole("row").filter({hasText:"203"}).count(),0);
  await page.setViewportSize({width:375,height:812});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:path.join(output,"staff-room-mobile.png"),fullPage:true});
  assert.deepEqual(errors,[]);console.log(JSON.stringify({status:"PASS",features:["private photo upload","violation resolved","billing calendar preview","room assignment via UI"],roles:["ADMIN","STAFF"],errors},null,2));
} finally {
  if(ownerToken) {
    // Logout revokes all sessions for the admin account; obtain a fresh token for cleanup.
    const cleanupLogin=await page.request.post(base+"/api/auth/login",{data:{username:"admin",password:process.env.ADMIN_PASSWORD||"DemoAdmin#2026"}});
    if(cleanupLogin.ok())ownerToken=(await cleanupLogin.json()).accessToken;
    if(assignment)await request("/assignments","POST",assignment).catch(()=>{});
    if(staffId)await request("/users/"+staffId+"/status","PUT",{status:"INACTIVE"}).catch(()=>{});
    // The uploaded file is a generated test fixture. Leave no fixture photo in the demo gallery.
    if(uploadedImage)await request("/rooms/"+fixtureRoomId+"/images/"+uploadedImage.id,"DELETE").catch(()=>{});
  }
  await browser.close();
}
