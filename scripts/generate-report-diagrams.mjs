import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('../frontend/node_modules/playwright');

const OUT_DIR = path.resolve(process.cwd(), 'docs/report-assets');
fs.mkdirSync(OUT_DIR, { recursive: true });

const COMMON_CSS = `
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #FFFFFF;
    color: #0F172A;
    font-family: 'Segoe UI', 'Inter', Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
  }
  .canvas {
    width: 1200px;
    padding: 26px 28px 22px 28px;
    background: #FFFFFF;
    border: 1.5px solid #CBD5E1;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    gap: 16px;
    border-bottom: 2px solid #1B365D;
    padding-bottom: 10px;
    margin-bottom: 18px;
  }
  .title { font-size: 18px; font-weight: 700; color: #1B365D; letter-spacing: -0.2px; white-space: nowrap; }
  .subtitle { font-size: 12.2px; color: #475569; font-weight: 500; text-align: right; }
  .footer-note {
    margin-top: 14px;
    padding-top: 9px;
    border-top: 1px solid #E2E8F0;
    font-size: 12px;
    color: #475569;
    display: flex;
    justify-content: space-between;
  }
`;

// 1. Architecture Diagram (4-layer)
const DIAG_ARCH = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.layers { display: flex; flex-direction: column; gap: 14px; }
.layer {
  border: 1.5px solid #94A3B8;
  border-radius: 6px;
  overflow: hidden;
  background: #F8FAFC;
}
.layer-head {
  background: #1B365D;
  color: #FFF;
  padding: 7px 14px;
  font-size: 13.5px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
}
.layer-head.l2 { background: #264B77; }
.layer-head.l3 { background: #335C67; }
.layer-head.l4 { background: #1E293B; }
.layer-head span.tag { font-weight: 400; font-size: 12px; opacity: 0.9; }
.layer-body {
  padding: 12px 14px;
  display: grid;
  gap: 10px;
}
.g3 { grid-template-columns: repeat(3, 1fr); }
.g4 { grid-template-columns: repeat(4, 1fr); }
.g5 { grid-template-columns: repeat(5, 1fr); }
.box {
  background: #FFFFFF;
  border: 1.2px solid #CBD5E1;
  border-left: 4px solid #1B365D;
  border-radius: 4px;
  padding: 9px 11px;
}
.box.accent { border-left-color: #2B6CB0; }
.box.teal { border-left-color: #2C7A7B; }
.box.dark { border-left-color: #334155; }
.box h4 { font-size: 13px; font-weight: 700; color: #0F172A; margin-bottom: 4px; }
.box p { font-size: 11.8px; color: #334155; line-height: 1.4; }
.arrow-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin: -5px 0;
  font-size: 11.8px;
  font-weight: 600;
  color: #1B365D;
}
.arrow-pill {
  background: #EFF6FF;
  border: 1px solid #93C5FD;
  border-radius: 12px;
  padding: 2px 12px;
}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">KIẾN TRÚC TỔNG THỂ HỆ THỐNG QUẢN LÝ NHÀ TRỌ (MODEL49 / V14)</div>
    <div class="subtitle">Java 21 · Spring Boot 4.1.1 · PostgreSQL 18.6 · React 19 + Vite 7</div>
  </div>
  <div class="layers">
    <div class="layer">
      <div class="layer-head"><span>TẦNG 1 — GIAO DIỆN NGƯỜI DÙNG (PRESENTATION / SPA)</span><span class="tag">React 19.2 · Vite 7.1 · Responsive Desktop & Mobile</span></div>
      <div class="layer-body g4">
        <div class="box"><h4>Điều hành & Báo cáo</h4><p>Tổng quan tháng/quý (6 chỉ số thu), biểu đồ doanh thu–dòng tiền, công nợ khách thuê.</p></div>
        <div class="box"><h4>Hợp đồng & Bàn giao</h4><p>Vòng đời hợp đồng, phụ lục PDF, đối chiếu tài sản nhận–trả phòng, quyết toán trả phòng.</p></div>
        <div class="box"><h4>Điện nước & Hóa đơn</h4><p>Ghi chỉ số, hàng đợi cảnh báo bất thường, hóa đơn minh bạch & ghi nhận thanh toán.</p></div>
        <div class="box"><h4>Cổng Khách & Vận hành</h4><p>TENANT xem hợp đồng/hóa đơn/sự cố; nhân viên xử lý danh mục 22 tài nguyên & tải tệp.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">REST/JSON qua HTTPS/HTTP · Authorization: Bearer JWT · Multipart Upload · PDF Stream</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l2"><span>TẦNG 2 — GIAO TIẾP API, BẢO MẬT & CẮT NGANG (WEB / SECURITY / AOP)</span><span class="tag">Spring Web MVC · Spring Security · JJWT 0.13 · AspectJ</span></div>
      <div class="layer-body g4">
        <div class="box accent"><h4>JwtFilter & SessionRegistry</h4><p>Xác thực Access Token, xoay vòng Refresh Token, thu hồi phiên khi khóa tài khoản/đổi mật khẩu.</p></div>
        <div class="box accent"><h4>Access49 (RBAC + Scope)</h4><p>Phân quyền ADMIN / STAFF / TENANT, kiểm tra phạm vi khu–phòng & hạn mức ủy quyền.</p></div>
        <div class="box accent"><h4>Api49 & AuthController</h4><p>Điểm vào REST cho hợp đồng, hóa đơn, sổ quỹ, bàn giao, báo cáo, danh mục & tệp đính kèm.</p></div>
        <div class="box accent"><h4>ApiErrors & ApiTimingAspect</h4><p>Ánh xạ 22 lỗi trigger DB sang HTTP 409 rõ nghĩa; AOP đo thời gian xử lý API tự động.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">@Transactional · Row Locking (FOR UPDATE) · pg_advisory_xact_lock · Idempotency Keys</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l3"><span>TẦNG 3 — XỬ LÝ NGHIỆP VỤ MIỀN (DOMAIN SERVICES — PROFILE model49)</span><span class="tag">Spring JDBC Template + JPA Validate + Apache PDFBox 3.0.7</span></div>
      <div class="layer-body g5">
        <div class="box teal"><h4>Leases49 & Catalog49</h4><p>Hợp đồng, phụ lục, cư trú, nhận/trả phòng, biên bản bàn giao & tài sản phòng.</p></div>
        <div class="box teal"><h4>Meters49</h4><p>Công tơ, điểm đo, khoảng tiêu thụ, thay công tơ & cảnh báo WEIGHTED_DAILY.</p></div>
        <div class="box teal"><h4>Billing49 & Finance49</h4><p>Lập/phát hành chứng từ bất biến, sổ quỹ tiền thật, phân bổ, cấn cọc, hoàn & đảo.</p></div>
        <div class="box teal"><h4>Settlements49 & Reports49</h4><p>Quyết toán trả phòng, điều chỉnh trả sớm, báo cáo tháng/quý/năm & công nợ.</p></div>
        <div class="box teal"><h4>ContractPdf & Files49</h4><p>Xuất PDF hợp đồng/phụ lục/báo cáo (NotoSans Unicode), xác thực ảnh ImageIO + SHA-256.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">PostgreSQL JDBC Driver · Prepared Statements (No Raw SQL Concatenation) · Flyway Forward-Only</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l4"><span>TẦNG 4 — LƯU TRỮ DỮ LIỆU & TOÀN VẸN CẤP CSDL (PERSISTENCE & STORAGE)</span><span class="tag">PostgreSQL 18.6 · Flyway V1–V14 · File Storage</span></div>
      <div class="layer-body g3">
        <div class="box dark"><h4>Lược đồ chuẩn hóa 49 bảng (461 cột)</h4><p>49 PK · 152 FK thật · 133 CHECK · 7 GiST EXCLUDE (chống chồng lịch phòng/cư trú/tài sản/công tơ).</p></div>
        <div class="box dark"><h4>10 View dẫn xuất & Trigger khóa sổ</h4><p>Tính tổng/nợ/số dư quỹ tự động qua View; Trigger chặn sửa hóa đơn phát hành, chặn chi/cấn vượt số dư, bảo toàn append-only.</p></div>
        <div class="box dark"><h4>Migration & Kho tệp chứng cứ</h4><p>Flyway V1–V14 bảo lưu lịch sử V10 tại <code>model_v10</code>; lưu ảnh/PDF gắn <code>stored_files</code> + kiểm quyền khi tải.</p></div>
      </div>
    </div>
  </div>
  <div class="footer-note">
    <span>Nguồn: Kiến trúc hiện thực thực tế tại package <code>com.hostel.*</code> và <code>frontend/src/*</code> (phiên bản V14).</span>
    <span>131/131 kiểm thử Java PASS · 20/20 kiểm thử SQL PASS</span>
  </div>
</div>
</body></html>`;

// 2. UML Use Case Diagram (SVG inside clean frame)
const DIAG_USECASE = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.svg-wrap { width: 100%; display: flex; justify-content: center; }
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">BIỂU ĐỒ USE CASE TỔNG QUÁT HỆ THỐNG QUẢN LÝ NHÀ TRỌ</div>
    <div class="subtitle">3 Tác nhân (ADMIN · STAFF · TENANT) · 6 Nhóm nghiệp vụ trọng tâm</div>
  </div>
  <div class="svg-wrap">
    <svg viewBox="0 0 1140 650" width="1140" height="650" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
      <defs>
        <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
          <path d="M 0 1 L 10 5 L 0 9 z" fill="#475569"/>
        </marker>
        <marker id="openArr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
          <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#1B365D" stroke-width="1.4"/>
        </marker>
      </defs>

      <!-- System Boundary -->
      <rect x="210" y="10" width="720" height="630" rx="8" fill="#F8FAFC" stroke="#1B365D" stroke-width="2"/>
      <text x="570" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#1B365D">HỆ THỐNG QUẢN LÝ NHÀ TRỌ (HOSTEL MANAGEMENT V14)</text>

      <!-- Group boxes inside boundary -->
      <rect x="230" y="48" width="330" height="150" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="242" y="66" font-size="11.5" font-weight="700" fill="#475569">1. Quản trị, Danh mục &amp; Tài sản</text>

      <rect x="580" y="48" width="330" height="182" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="592" y="66" font-size="11.5" font-weight="700" fill="#475569">2. Hợp đồng, Cư trú &amp; Bàn giao</text>

      <rect x="230" y="210" width="330" height="175" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="242" y="228" font-size="11.5" font-weight="700" fill="#475569">3. Điện nước &amp; Cảnh báo bất thường</text>

      <rect x="580" y="242" width="330" height="190" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="592" y="260" font-size="11.5" font-weight="700" fill="#475569">4. Hóa đơn, Thanh toán &amp; Quyết toán</text>

      <rect x="230" y="397" width="330" height="115" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="242" y="415" font-size="11.5" font-weight="700" fill="#475569">5. Báo cáo &amp; Công nợ</text>

      <rect x="580" y="444" width="330" height="178" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="592" y="462" font-size="11.5" font-weight="700" fill="#475569">6. Sự cố, Bảo trì &amp; Cổng Khách thuê</text>

      <rect x="230" y="524" width="330" height="98" rx="6" fill="#FFFFFF" stroke="#CBD5E1" stroke-dasharray="4 3"/>
      <text x="242" y="542" font-size="11.5" font-weight="700" fill="#475569">7. Xác thực &amp; Tài khoản</text>

      <!-- Use Case Ellipses helper -->
      <!-- Group 1 -->
      <ellipse cx="395" cy="94" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="98" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Quản lý tài khoản, phân công &amp; hạn mức</text>

      <ellipse cx="395" cy="136" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="140" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Quản lý khu, phòng, đơn giá &amp; ngưỡng</text>

      <ellipse cx="395" cy="176" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="180" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Quản lý đồ đạc, tài sản bố trí phòng</text>

      <!-- Group 2 -->
      <ellipse cx="745" cy="94" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="98" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Lập, ký, gia hạn hợp đồng &amp; xuất PDF</text>

      <ellipse cx="745" cy="138" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="142" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Quản lý cư trú thực tế &amp; giao/nhận phòng</text>

      <ellipse cx="745" cy="182" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="186" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Lập &amp; đối chiếu biên bản nhận/trả phòng</text>

      <text x="745" y="218" text-anchor="middle" font-size="11" font-style="italic" fill="#475569">«extend» Đính ảnh minh chứng &amp; lập bồi thường hỏng đồ</text>

      <!-- Group 3 -->
      <ellipse cx="395" cy="256" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="260" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Ghi chỉ số công tơ &amp; thay công tơ</text>

      <ellipse cx="395" cy="306" rx="142" ry="18" fill="#FFFBEB" stroke="#B45309" stroke-width="1.4"/>
      <text x="395" y="310" text-anchor="middle" font-size="12" font-weight="600" fill="#92400E">Tự động đánh giá cảnh báo bất thường</text>

      <ellipse cx="395" cy="356" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="360" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Kiểm tra, xác nhận / bỏ qua cảnh báo</text>

      <line x1="395" y1="274" x2="395" y2="288" stroke="#1B365D" stroke-width="1.2" stroke-dasharray="3 2" marker-end="url(#openArr)"/>
      <text x="444" y="285" font-size="10.5" font-style="italic" fill="#1B365D">«include»</text>

      <!-- Group 4 -->
      <ellipse cx="745" cy="288" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="292" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Lập nháp, phát hành &amp; điều chỉnh hóa đơn</text>

      <ellipse cx="745" cy="336" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="340" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Thu tiền, tách quỹ cọc / trả trước / dư</text>

      <ellipse cx="745" cy="384" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="388" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Duyệt quyết toán trả phòng &amp; chi hoàn cọc</text>

      <text x="745" y="420" text-anchor="middle" font-size="11" font-style="italic" fill="#475569">«constraint» Chặn phát hành khi còn cảnh báo PENDING</text>

      <!-- Group 5 -->
      <ellipse cx="395" cy="444" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="448" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Xem tổng quan 6 chỉ số &amp; công nợ khách</text>

      <ellipse cx="395" cy="488" rx="142" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="395" y="492" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Báo cáo doanh thu tháng/quý/năm &amp; PDF</text>

      <!-- Group 6 -->
      <ellipse cx="745" cy="492" rx="144" ry="18" fill="#F0FDF4" stroke="#166534" stroke-width="1.3"/>
      <text x="745" y="496" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Tra cứu hợp đồng, hóa đơn &amp; lịch sử thu</text>

      <ellipse cx="745" cy="540" rx="144" ry="18" fill="#F0FDF4" stroke="#166534" stroke-width="1.3"/>
      <text x="745" y="544" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Gửi yêu cầu sửa chữa &amp; đính kèm ảnh</text>

      <ellipse cx="745" cy="588" rx="144" ry="18" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.3"/>
      <text x="745" y="592" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Xử lý sự cố, ghi chi phí &amp; lịch bảo trì</text>

      <!-- Group 7 -->
      <ellipse cx="395" cy="574" rx="142" ry="18" fill="#F1F5F9" stroke="#334155" stroke-width="1.3"/>
      <text x="395" y="578" text-anchor="middle" font-size="12" font-weight="600" fill="#0F172A">Đăng nhập JWT, đổi mật khẩu, thông báo</text>

      <!-- Actors LEFT: ADMIN & STAFF -->
      <!-- ADMIN Stick figure -->
      <g transform="translate(88, 125)">
        <circle cx="0" cy="-24" r="14" fill="#EFF6FF" stroke="#1B365D" stroke-width="2.2"/>
        <line x1="0" y1="-10" x2="0" y2="26" stroke="#1B365D" stroke-width="2.2"/>
        <line x1="-22" y1="2" x2="22" y2="2" stroke="#1B365D" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="-18" y2="52" stroke="#1B365D" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="18" y2="52" stroke="#1B365D" stroke-width="2.2"/>
        <text x="0" y="72" text-anchor="middle" font-size="13.5" font-weight="700" fill="#1B365D">Chủ nhà</text>
        <text x="0" y="88" text-anchor="middle" font-size="11.5" font-weight="600" fill="#475569">(ADMIN)</text>
        <text x="0" y="103" text-anchor="middle" font-size="10.5" fill="#64748B">Toàn quyền hệ thống</text>
      </g>

      <!-- STAFF Stick figure -->
      <g transform="translate(88, 395)">
        <circle cx="0" cy="-24" r="14" fill="#EFF6FF" stroke="#264B77" stroke-width="2.2"/>
        <line x1="0" y1="-10" x2="0" y2="26" stroke="#264B77" stroke-width="2.2"/>
        <line x1="-22" y1="2" x2="22" y2="2" stroke="#264B77" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="-18" y2="52" stroke="#264B77" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="18" y2="52" stroke="#264B77" stroke-width="2.2"/>
        <text x="0" y="72" text-anchor="middle" font-size="13.5" font-weight="700" fill="#264B77">Nhân viên</text>
        <text x="0" y="88" text-anchor="middle" font-size="11.5" font-weight="600" fill="#475569">(STAFF)</text>
        <text x="0" y="103" text-anchor="middle" font-size="10.5" fill="#64748B">Theo khu/phòng</text>
        <text x="0" y="117" text-anchor="middle" font-size="10.5" fill="#64748B">&amp; hạn mức ủy quyền</text>
      </g>

      <!-- Actor RIGHT: TENANT -->
      <g transform="translate(1045, 495)">
        <circle cx="0" cy="-24" r="14" fill="#F0FDF4" stroke="#166534" stroke-width="2.2"/>
        <line x1="0" y1="-10" x2="0" y2="26" stroke="#166534" stroke-width="2.2"/>
        <line x1="-22" y1="2" x2="22" y2="2" stroke="#166534" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="-18" y2="52" stroke="#166534" stroke-width="2.2"/>
        <line x1="0" y1="26" x2="18" y2="52" stroke="#166534" stroke-width="2.2"/>
        <text x="0" y="72" text-anchor="middle" font-size="13.5" font-weight="700" fill="#166534">Người thuê</text>
        <text x="0" y="88" text-anchor="middle" font-size="11.5" font-weight="600" fill="#475569">(TENANT)</text>
        <text x="0" y="103" text-anchor="middle" font-size="10.5" fill="#64748B">Theo hợp đồng</text>
        <text x="0" y="117" text-anchor="middle" font-size="10.5" fill="#64748B">đại diện / cư trú</text>
      </g>

      <!-- Generalization: ADMIN inherits STAFF operational use cases (clean hollow triangle per UML 2.5) -->
      <line x1="88" y1="234" x2="88" y2="354" stroke="#1B365D" stroke-width="1.8"/>
      <polygon points="88,365 81,351 95,351" fill="#FFFFFF" stroke="#1B365D" stroke-width="1.8"/>
      <text x="100" y="302" font-size="10.5" font-style="italic" fill="#1B365D">kế thừa quyền</text>
      <text x="100" y="316" font-size="10.5" font-style="italic" fill="#1B365D">vận hành</text>

      <!-- ADMIN exclusive lines (Group 1 + full reports) -->
      <line x1="115" y1="120" x2="252" y2="94" stroke="#1B365D" stroke-width="1.5"/>
      <line x1="115" y1="130" x2="252" y2="136" stroke="#1B365D" stroke-width="1.5"/>
      <line x1="115" y1="142" x2="252" y2="176" stroke="#1B365D" stroke-width="1.5"/>

      <!-- STAFF lines (and inherited by ADMIN) -->
      <line x1="115" y1="382" x2="252" y2="256" stroke="#264B77" stroke-width="1.4"/>
      <line x1="115" y1="392" x2="252" y2="356" stroke="#264B77" stroke-width="1.4"/>
      <line x1="115" y1="402" x2="252" y2="444" stroke="#264B77" stroke-width="1.4"/>
      <line x1="115" y1="412" x2="252" y2="488" stroke="#264B77" stroke-width="1.4"/>
      <line x1="115" y1="422" x2="252" y2="574" stroke="#264B77" stroke-width="1.4"/>
      <path d="M 115 398 L 568 398 L 601 288" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 398 L 601 336" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 398 L 601 384" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 398 L 568 94 L 601 94" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 138 L 601 138" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 182 L 601 182" fill="none" stroke="#264B77" stroke-width="1.3"/>
      <path d="M 568 398 L 568 588 L 601 588" fill="none" stroke="#264B77" stroke-width="1.3"/>

      <!-- TENANT lines -->
      <line x1="1018" y1="490" x2="889" y2="492" stroke="#166534" stroke-width="1.6"/>
      <line x1="1018" y1="504" x2="889" y2="540" stroke="#166534" stroke-width="1.6"/>
      <path d="M 1018 520 L 965 520 L 965 632 L 395 632 L 395 592" fill="none" stroke="#166534" stroke-width="1.4"/>
    </svg>
  </div>
  <div class="footer-note">
    <span>Ghi chú: ADMIN bao quát toàn bộ nghiệp vụ vận hành của STAFF và độc quyền quản trị tài khoản, phân công, đơn giá, ngưỡng cảnh báo và tạo danh mục tài sản.</span>
    <span>Ký hiệu UML 2.5</span>
  </div>
</div>
</body></html>`;

// 3. 8-Module Database Architecture Overview (49 tables)
const DIAG_DB_MODULES = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.mod {
  border: 1.5px solid #94A3B8;
  border-radius: 6px;
  overflow: hidden;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
}
.mod-hd {
  background: #1B365D;
  color: #FFF;
  padding: 7px 10px;
  font-size: 12.5px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mod-hd.m2 { background: #1E40AF; }
.mod-hd.m3 { background: #0F4C81; }
.mod-hd.m4 { background: #0E7490; }
.mod-hd.m5 { background: #4338CA; }
.mod-hd.m6 { background: #334155; }
.mod-hd.m7 { background: #475569; }
.mod-hd.m8 { background: #1E293B; }
.cnt {
  background: rgba(255,255,255,0.2);
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 11px;
}
.mod-bd { padding: 8px 10px; font-size: 11.4px; line-height: 1.48; flex: 1; }
.tbl-list { list-style: none; }
.tbl-list li {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 11.2px;
  color: #0F172A;
  padding: 1.5px 0;
  border-bottom: 1px dotted #E2E8F0;
}
.tbl-list li:last-child { border-bottom: none; }
.tbl-list li span.v14 {
  background: #FEF3C7;
  color: #92400E;
  font-family: 'Segoe UI', Arial, sans-serif;
  font-weight: 700;
  font-size: 9.8px;
  padding: 0 4px;
  border-radius: 3px;
  margin-left: 4px;
}
.mod-ft {
  background: #F8FAFC;
  border-top: 1px solid #E2E8F0;
  padding: 6px 10px;
  font-size: 11px;
  color: #475569;
}
.summary-bar {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 12px;
}
.info-card {
  background: #F8FAFC;
  border: 1.2px solid #CBD5E1;
  border-left: 4px solid #1B365D;
  border-radius: 4px;
  padding: 9px 12px;
  font-size: 11.8px;
  line-height: 1.48;
}
.info-card.v14box { border-left-color: #B45309; background: #FFFBEB; }
.info-card b { color: #0F172A; }
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">CƠ SỞ DỮ LIỆU 49 BẢNG CHIA THEO 8 PHÂN HỆ (POSTGRESQL 18.6 — V14)</div>
    <div class="subtitle">49 bảng · 461 cột · 49 PK · 152 FK · 133 CHECK · 7 EXCLUDE · 10 View</div>
  </div>
  <div class="grid">
    <div class="mod">
      <div class="mod-hd"><span>A. Tài khoản &amp; Quyền</span><span class="cnt">6 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>users</li>
        <li>tenant_profiles</li>
        <li>employee_assignments</li>
        <li>staff_permission_grants</li>
        <li>password_reset_tokens</li>
        <li>audit_events</li>
      </ul></div>
      <div class="mod-ft">RBAC, phạm vi khu/phòng, hạn mức ủy quyền &amp; nhật ký tra soát.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m2"><span>B. Khu, Phòng &amp; Tài sản</span><span class="cnt">7 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>properties</li>
        <li>rooms</li>
        <li>room_price_history</li>
        <li>room_operating_periods</li>
        <li>asset_types</li>
        <li>assets</li>
        <li>asset_placements</li>
      </ul></div>
      <div class="mod-ft">Lịch sử giá niêm yết, trạng thái khai thác &amp; vị trí đồ đạc theo thời gian.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m3"><span>C. Hợp đồng &amp; Bàn giao</span><span class="cnt">12 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>reservations · contracts</li>
        <li>contract_parties</li>
        <li>contract_contacts</li>
        <li>contract_residencies</li>
        <li>contract_terms <span class="v14">+5 cột V14</span></li>
        <li>room_possessions</li>
        <li>move_out_notices</li>
        <li>handovers</li>
        <li>handover_items <span class="v14">+1 FK V14</span></li>
        <li>contract_violations</li>
        <li>checkout_records</li>
      </ul></div>
      <div class="mod-ft">Tách bên ký/liên hệ/thực ở; phiên bản điều khoản bất biến; đối chiếu nhận–trả.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m4"><span>D. Dịch vụ &amp; Điện nước</span><span class="cnt">9 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>service_items</li>
        <li>service_price_configs</li>
        <li>contract_service_subscriptions</li>
        <li>meters</li>
        <li>meter_readings</li>
        <li>consumption_segments</li>
        <li>alert_policies</li>
        <li>consumption_alerts</li>
        <li>alert_baseline_sources</li>
      </ul></div>
      <div class="mod-ft">Công tơ vật lý → điểm đo → đoạn dùng; lưu đủ căn cứ baseline cảnh báo.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m6"><span>E. Sửa chữa &amp; Bảo trì</span><span class="cnt">4 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>maintenance_requests</li>
        <li>maintenance_events</li>
        <li>maintenance_costs</li>
        <li>maintenance_schedules</li>
      </ul></div>
      <div class="mod-ft">Gắn theo vị trí phòng/khu/tài sản; tách chi phí sửa thực tế khỏi tiền bồi thường.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m5"><span>F. Chứng từ &amp; Dòng tiền</span><span class="cnt">7 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>billing_documents</li>
        <li>billing_items</li>
        <li>cash_accounts</li>
        <li>cash_transactions</li>
        <li>fund_accounts</li>
        <li>fund_entries</li>
        <li>fund_holds</li>
      </ul></div>
      <div class="mod-ft">Tách chứng từ phải thu khỏi tiền thật (IN/OUT) &amp; quỹ khách (DEPOSIT/ADVANCE).</div>
    </div>

    <div class="mod">
      <div class="mod-hd m7"><span>G. Thông báo</span><span class="cnt">2 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>notifications</li>
        <li>notification_recipients</li>
      </ul></div>
      <div class="mod-ft">Khóa chống gửi lặp; theo dõi thời điểm đọc <code>read_at</code> theo từng người dùng.</div>
    </div>

    <div class="mod">
      <div class="mod-hd m8"><span>H. Tệp &amp; Chứng cứ ảnh</span><span class="cnt">2 bảng</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>stored_files</li>
        <li>file_attachments</li>
      </ul></div>
      <div class="mod-ft">10 cột FK đích có CHECK đúng 1 cột khác NULL (không dùng đa hình thiếu FK).</div>
    </div>
  </div>

  <div class="summary-bar">
    <div class="info-card">
      <b>10 View dẫn xuất (không lưu trùng số tổng/công nợ/số dư):</b>
      <code>v_contract_current</code>, <code>v_room_status</code>, <code>v_segments</code>, <code>v_billing_lines</code>, <code>v_fund_movements</code>, <code>v_fund_balances</code>, <code>v_item_applications</code>, <code>v_item_balances</code>, <code>v_document_balances</code>, <code>v_maintenance</code>.
    </div>
    <div class="info-card v14box">
      <b>Mở rộng bổ sung tại Flyway V14:</b><br>
      • <code>handover_items.original_item_id</code> (FK tự tham chiếu đối chiếu đồ nhận–trả).<br>
      • <code>contract_terms</code> thêm 5 cột thông tin 2 bên xuất PDF hợp đồng/phụ lục.
    </div>
  </div>
</div>
</body></html>`;

// 4. Core ERD: Contract–Handover (V14) & Billing–Ledger (Print-calibrated SVG)
const DIAG_ERD_CORE = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.svg-wrap { width: 100%; }
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">ERD LÕI: HỢP ĐỒNG – BÀN GIAO (V14) &amp; CHỨNG TỪ – SỔ QUỸ TÀI CHÍNH</div>
    <div class="subtitle">Crow's Foot · Tách Nghĩa vụ chứng từ ↔ Tiền thật ↔ Quỹ tiền khách</div>
  </div>
  <div class="svg-wrap">
    <svg viewBox="0 0 1140 615" width="1140" height="615" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
      <!-- Helper styles via SVG elements -->
      <!-- Zone backgrounds -->
      <rect x="6" y="6" width="535" height="602" rx="6" fill="#F8FAFC" stroke="#CBD5E1"/>
      <text x="18" y="26" font-size="12" font-weight="700" fill="#1B365D">KHỐI HỢP ĐỒNG, ĐIỀU KHOẢN &amp; BÀN GIAO ĐỐI CHIẾU (V14)</text>

      <rect x="556" y="6" width="578" height="602" rx="6" fill="#F8FAFC" stroke="#CBD5E1"/>
      <text x="568" y="26" font-size="12" font-weight="700" fill="#3730A3">KHỐI CHỨNG TỪ PHẢI THU &amp; SỔ QUỸ APPEND-ONLY</text>

      <!-- Table macro rendered explicitly -->
      <!-- 1. contracts -->
      <g transform="translate(22, 42)">
        <rect width="235" height="148" rx="4" fill="#FFF" stroke="#1B365D" stroke-width="1.5"/>
        <rect width="235" height="26" rx="3" fill="#1B365D"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">contracts</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#1B365D">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> room_id → rooms</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> previous_contract_id</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> contract_code : varchar</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   start_date : date</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   status : DRAFT|ACTIVE|ENDED</text>
      </g>

      <!-- 2. contract_terms (+V14) -->
      <g transform="translate(285, 42)">
        <rect width="242" height="196" rx="4" fill="#FFF" stroke="#1B365D" stroke-width="1.5"/>
        <rect width="242" height="26" rx="3" fill="#1B365D"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">contract_terms</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#1B365D">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_id · version_no</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace">   effective_from · lease_end</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace">   rental_price · deposit_req</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   payment_day · signed_at</text>
        <rect x="4" y="126" width="234" height="64" rx="3" fill="#FEF3C7" stroke="#D97706" stroke-width="0.9"/>
        <text x="10" y="142" font-size="10.5" font-weight="700" fill="#92400E">Mở rộng V14 (xuất PDF hợp đồng/phụ lục):</text>
        <text x="10" y="158" font-size="10.8" font-family="Consolas,monospace" fill="#78350F">lessor_name, lessor_identity,</text>
        <text x="10" y="172" font-size="10.8" font-family="Consolas,monospace" fill="#78350F">lessor_phone, lessor_address,</text>
        <text x="10" y="185" font-size="10.8" font-family="Consolas,monospace" fill="#78350F">tenant_address</text>
      </g>

      <!-- 3. handovers -->
      <g transform="translate(22, 232)">
        <rect width="235" height="148" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.5"/>
        <rect width="235" height="26" rx="3" fill="#0F4C81"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">handovers</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#0F4C81">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_id → contracts</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> possession_id (NULL)</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace">   handover_type: MOVE_IN|OUT</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   status: DRAFT|CONFIRMED</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   confirmed_by · confirmed_at</text>
      </g>

      <!-- 4. handover_items (+V14 self FK) -->
      <g transform="translate(285, 272)">
        <rect width="242" height="162" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.5"/>
        <rect width="242" height="26" rx="3" fill="#0F4C81"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">handover_items</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#0F4C81">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> handover_id → handovers</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> asset_placement_id (NULL)</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace">   item_name_snapshot · qty</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   condition_note · note</text>
        <rect x="4" y="124" width="234" height="32" rx="3" fill="#FEF3C7" stroke="#D97706" stroke-width="0.9"/>
        <text x="10" y="139" font-size="10.5" font-weight="700" fill="#92400E">V14 Self-FK đối chiếu nhận–trả:</text>
        <text x="10" y="152" font-size="10.8" font-family="Consolas,monospace" fill="#78350F">FK original_item_id → self.id</text>
      </g>

      <!-- 5. checkout_records -->
      <g transform="translate(22, 430)">
        <rect width="235" height="148" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.5"/>
        <rect width="235" height="26" rx="3" fill="#0F4C81"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">checkout_records</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#0F4C81">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_id · return_handover</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace">   billing_end · status</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace">   deposit_snapshot : numeric</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   advance_snapshot : numeric</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   outstanding_snapshot</text>
      </g>

      <!-- 6. billing_documents -->
      <g transform="translate(574, 42)">
        <rect width="256" height="162" rx="4" fill="#FFF" stroke="#3730A3" stroke-width="1.5"/>
        <rect width="256" height="26" rx="3" fill="#3730A3"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">billing_documents</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#3730A3">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_id · checkout_id</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> related_document_id (ADJ)</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> document_code : varchar</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   document_type: MONTHLY|COMP…</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   billing_period · issue/due</text>
        <text x="10" y="153" font-size="11.2" font-family="Consolas,monospace">   status: DRAFT|ISSUED (khóa)</text>
      </g>

      <!-- 7. billing_items -->
      <g transform="translate(856, 42)">
        <rect width="262" height="178" rx="4" fill="#FFF" stroke="#3730A3" stroke-width="1.5"/>
        <rect width="262" height="26" rx="3" fill="#3730A3"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">billing_items</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#3730A3">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> document_id → billing_docs</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> segment_id · handover_item_id</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_term_id · adjusts_id</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   item_kind · direction (DR/CR)</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   unit_price_snapshot · qty</text>
        <text x="10" y="153" font-size="11.2" font-family="Consolas,monospace">   service_from · service_to</text>
        <text x="10" y="170" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> source_key (chống lập trùng)</text>
      </g>

      <!-- 8. cash_accounts & cash_transactions -->
      <g transform="translate(574, 250)">
        <rect width="256" height="156" rx="4" fill="#FFF" stroke="#0F766E" stroke-width="1.5"/>
        <rect width="256" height="26" rx="3" fill="#0F766E"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">cash_transactions (Tiền thật)</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#0F766E">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> cash_account_id → cash_acc</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> payment_document_id (V13)</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> transaction_code · ext_ref</text>
        <text x="10" y="117" font-size="11.2" font-family="Consolas,monospace">   direction: IN | OUT (&gt; 0)</text>
        <text x="10" y="135" font-size="11.2" font-family="Consolas,monospace">   amount · method · occurred_at</text>
        <text x="10" y="149" font-size="11.2" font-family="Consolas,monospace">   status: PENDING | CONFIRMED</text>
      </g>

      <!-- 9. fund_accounts -->
      <g transform="translate(574, 444)">
        <rect width="256" height="146" rx="4" fill="#FFF" stroke="#4338CA" stroke-width="1.5"/>
        <rect width="256" height="26" rx="3" fill="#4338CA"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">fund_accounts (Quỹ khách)</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#4338CA">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> contract_id · reservation_id</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> fund_code : varchar</text>
        <text x="10" y="99" font-size="11.2" font-family="Consolas,monospace">   fund_type: DEPOSIT|ADVANCE|</text>
        <text x="10" y="115" font-size="11.2" font-family="Consolas,monospace">              RESERVATION|UNAPPL</text>
        <text x="10" y="135" font-size="10.8" fill="#475569" font-style="italic">* Số dư tính từ v_fund_balances</text>
      </g>

      <!-- 10. fund_entries -->
      <g transform="translate(856, 260)">
        <rect width="262" height="198" rx="4" fill="#FFF" stroke="#4338CA" stroke-width="1.5"/>
        <rect width="262" height="26" rx="3" fill="#4338CA"/>
        <text x="10" y="18" fill="#FFF" font-size="12.5" font-weight="700">fund_entries (Sổ cái bất biến)</text>
        <text x="10" y="45" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#4338CA">PK</tspan> id : bigint</text>
        <text x="10" y="63" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">UQ</tspan> entry_code : varchar</text>
        <text x="10" y="81" font-size="11.2" font-family="Consolas,monospace">   entry_type: RECEIVE|PAYMENT|</text>
        <text x="10" y="96" font-size="11.2" font-family="Consolas,monospace">     TRANSFER|REFUND|REVERSAL…</text>
        <text x="10" y="114" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> cash_transaction_id</text>
        <text x="10" y="132" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> from_fund_id · to_fund_id</text>
        <text x="10" y="150" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> debit_item_id · credit_item</text>
        <text x="10" y="168" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#475569">FK</tspan> original_entry_id (đảo)</text>
        <text x="10" y="186" font-size="11.2" font-family="Consolas,monospace">   amount (&gt;0) · posted_at</text>
      </g>

      <!-- 11. fund_holds -->
      <g transform="translate(856, 488)">
        <rect width="262" height="102" rx="4" fill="#FFF" stroke="#475569" stroke-width="1.4"/>
        <rect width="262" height="24" rx="3" fill="#475569"/>
        <text x="10" y="17" fill="#FFF" font-size="12" font-weight="700">fund_holds (Tạm giữ có căn cứ)</text>
        <text x="10" y="42" font-size="11.2" font-family="Consolas,monospace"><tspan font-weight="700" fill="#334155">PK</tspan> id · <tspan font-weight="700">FK</tspan> fund_account_id</text>
        <text x="10" y="60" font-size="11.2" font-family="Consolas,monospace">   amount · review_on · reason</text>
        <text x="10" y="78" font-size="11.2" font-family="Consolas,monospace">   released_at (NULL = đang giữ)</text>
        <text x="10" y="94" font-size="10.5" fill="#475569" font-style="italic">Khả dụng = Số dư quỹ − Tạm giữ</text>
      </g>

      <!-- Relationship connectors with cardinality labels -->
      <!-- contracts 1--N contract_terms -->
      <line x1="257" y1="92" x2="285" y2="92" stroke="#1B365D" stroke-width="1.6"/>
      <text x="261" y="86" font-size="10.5" font-weight="700" fill="#1B365D">1</text>
      <text x="272" y="106" font-size="10.5" font-weight="700" fill="#1B365D">N</text>

      <!-- contracts 1--N handovers -->
      <line x1="138" y1="190" x2="138" y2="232" stroke="#0F4C81" stroke-width="1.6"/>
      <text x="144" y="205" font-size="10.5" font-weight="700" fill="#0F4C81">1</text>
      <text x="144" y="226" font-size="10.5" font-weight="700" fill="#0F4C81">0..N</text>

      <!-- handovers 1--N handover_items -->
      <path d="M 257 315 L 285 315" fill="none" stroke="#0F4C81" stroke-width="1.6"/>
      <text x="261" y="309" font-size="10.5" font-weight="700" fill="#0F4C81">1</text>
      <text x="271" y="330" font-size="10.5" font-weight="700" fill="#0F4C81">N</text>

      <!-- handover_items self-ref loop -->
      <path d="M 527 415 L 542 415 L 542 454 L 405 454 L 405 434" fill="none" stroke="#D97706" stroke-width="1.5" stroke-dasharray="4 2"/>
      <text x="415" y="470" font-size="10.2" font-weight="700" fill="#92400E">0..1 đối chiếu dòng nhận gốc</text>

      <!-- handovers 0..1 -- checkout_records -->
      <line x1="138" y1="380" x2="138" y2="430" stroke="#0F4C81" stroke-width="1.6"/>
      <text x="144" y="396" font-size="10.5" font-weight="700" fill="#0F4C81">0..1</text>
      <text x="144" y="424" font-size="10.5" font-weight="700" fill="#0F4C81">0..1</text>

      <!-- contracts -> billing_documents (routed cleanly ABOVE contract_terms header) -->
      <path d="M 140 42 L 140 31 L 690 31 L 690 42" fill="none" stroke="#3730A3" stroke-width="1.6"/>
      <rect x="520" y="21" width="52" height="16" rx="3" fill="#FFFFFF"/>
      <text x="546" y="33" text-anchor="middle" font-size="10.5" font-weight="700" fill="#3730A3">1 : N</text>

      <!-- handover_items -> billing_items (COMPENSATION damage link) -->
      <path d="M 527 290 L 550 290 L 550 228 L 960 228 L 960 220" fill="none" stroke="#D97706" stroke-width="1.4" stroke-dasharray="5 3"/>
      <text x="760" y="222" text-anchor="middle" font-size="10.2" font-weight="700" fill="#92400E">Căn cứ bồi thường (handover_item_id)</text>

      <!-- billing_documents 1--N billing_items -->
      <line x1="830" y1="108" x2="856" y2="108" stroke="#3730A3" stroke-width="1.6"/>
      <text x="833" y="102" font-size="10.5" font-weight="700" fill="#3730A3">1</text>
      <text x="844" y="122" font-size="10.5" font-weight="700" fill="#3730A3">N</text>

      <!-- billing_items 1--N fund_entries -->
      <line x1="990" y1="220" x2="990" y2="260" stroke="#4338CA" stroke-width="1.6"/>
      <text x="996" y="238" font-size="10.5" font-weight="700" fill="#4338CA">1</text>
      <text x="996" y="255" font-size="10.5" font-weight="700" fill="#4338CA">0..N (PAYMENT)</text>

      <!-- cash_transactions 1--N fund_entries -->
      <line x1="830" y1="330" x2="856" y2="330" stroke="#0F766E" stroke-width="1.6"/>
      <text x="833" y="324" font-size="10.5" font-weight="700" fill="#0F766E">1</text>
      <text x="843" y="344" font-size="10.5" font-weight="700" fill="#0F766E">N</text>

      <!-- fund_accounts 1--N fund_entries -->
      <path d="M 830 470 L 845 470 L 845 390 L 856 390" fill="none" stroke="#4338CA" stroke-width="1.6"/>
      <text x="835" y="465" font-size="10.5" font-weight="700" fill="#4338CA">1</text>
      <text x="845" y="384" font-size="10.5" font-weight="700" fill="#4338CA">N</text>

      <!-- fund_accounts 1--N fund_holds -->
      <line x1="830" y1="538" x2="856" y2="538" stroke="#475569" stroke-width="1.6"/>
      <text x="833" y="532" font-size="10.5" font-weight="700" fill="#475569">1</text>
      <text x="839" y="553" font-size="10.5" font-weight="700" fill="#475569">0..N</text>
    </svg>
  </div>
  <div class="footer-note">
    <span>Nguồn: Đối chiếu trực tiếp từ <code>docs/quanlytro.sql</code> và <code>V11–V14</code>. Toàn bộ 8 phân hệ chi tiết đầy đủ lưu tại <code>docs/dbdiagram/modules/</code>.</span>
    <span>ERD Lõi V14</span>
  </div>
</div>
</body></html>`;

// 5. Sequence Diagram 1: Meter -> Alert -> Invoice -> Payment (Overpayment split)
const DIAG_SEQ_BILLING = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">TUẦN TỰ: GHI CHỈ SỐ — CẢNH BÁO BẤT THƯỜNG — HÓA ĐƠN — THANH TOÁN</div>
    <div class="subtitle">Khóa đồng thời (FOR UPDATE) · Chống gửi lặp (Idempotency)</div>
  </div>
  <svg viewBox="0 0 1140 605" width="1140" height="605" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="mSolid" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#1B365D"/>
      </marker>
      <marker id="mDash" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#475569" stroke-width="1.6"/>
      </marker>
      <marker id="mWarn" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#B45309"/>
      </marker>
    </defs>

    <!-- Lifelines headers -->
    <g font-size="12.5" font-weight="700" text-anchor="middle">
      <rect x="20" y="10" width="140" height="40" rx="4" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
      <text x="90" y="28" fill="#1B365D">ADMIN / STAFF</text>
      <text x="90" y="43" font-size="10.8" font-weight="500" fill="#475569">(Giao diện React)</text>

      <rect x="235" y="10" width="165" height="40" rx="4" fill="#1B365D"/>
      <text x="317" y="28" fill="#FFF">Meters49</text>
      <text x="317" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Đo &amp; Cảnh báo</text>

      <rect x="475" y="10" width="165" height="40" rx="4" fill="#1B365D"/>
      <text x="557" y="28" fill="#FFF">Billing49</text>
      <text x="557" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Hóa đơn bất biến</text>

      <rect x="715" y="10" width="165" height="40" rx="4" fill="#1B365D"/>
      <text x="797" y="28" fill="#FFF">Finance49</text>
      <text x="797" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Sổ tiền &amp; Quỹ khách</text>

      <rect x="955" y="10" width="165" height="40" rx="4" fill="#1E293B"/>
      <text x="1037" y="28" fill="#FFF">PostgreSQL 18.6</text>
      <text x="1037" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Trigger · View · Lock</text>
    </g>

    <!-- Vertical dashed lifelines -->
    <line x1="90" y1="50" x2="90" y2="592" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="317" y1="50" x2="317" y2="592" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="557" y1="50" x2="557" y2="592" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="797" y1="50" x2="797" y2="592" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="1037" y1="50" x2="1037" y2="592" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>

    <!-- Phase 1: Meter reading & Alert -->
    <rect x="16" y="66" width="1108" height="172" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="66" width="230" height="22" rx="2" fill="#1B365D"/>
    <text x="26" y="81" font-size="11.2" font-weight="700" fill="#FFF">1. Ghi chỉ số &amp; Đánh giá bất thường</text>

    <line x1="90" y1="108" x2="312" y2="108" stroke="#1B365D" stroke-width="1.6" marker-end="url(#mSolid)"/>
    <text x="200" y="102" text-anchor="middle" font-size="11.5" font-weight="600">POST /api/meter-readings (oldIndex, newIndex)</text>

    <line x1="317" y1="134" x2="1032" y2="134" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="675" y="128" text-anchor="middle" font-size="11.3">FOR UPDATE (rooms, contracts, meters) → INSERT meter_readings + consumption_segments</text>

    <line x1="317" y1="164" x2="1032" y2="164" stroke="#B45309" stroke-width="1.5" marker-end="url(#mWarn)"/>
    <text x="675" y="158" text-anchor="middle" font-size="11.3" fill="#92400E" font-weight="600">analyze(): So mức dùng/ngày hiện tại với baseline WEIGHTED_DAILY → lưu consumption_alerts + baseline_sources</text>

    <line x1="317" y1="194" x2="95" y2="194" stroke="#475569" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#mDash)"/>
    <text x="205" y="188" text-anchor="middle" font-size="11.2" fill="#92400E">Trả segment + cảnh báo PENDING (nếu vượt ngưỡng %)</text>

    <line x1="90" y1="224" x2="312" y2="224" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="200" y="218" text-anchor="middle" font-size="11.3">PUT /api/alerts/{id} (CONFIRMED / DISMISSED + ghi chú)</text>

    <!-- Phase 2: Invoice Draft & Issue -->
    <rect x="16" y="250" width="1108" height="156" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="250" width="248" height="22" rx="2" fill="#0F4C81"/>
    <text x="26" y="265" font-size="11.2" font-weight="700" fill="#FFF">2. Lập nháp &amp; Phát hành hóa đơn khóa</text>

    <line x1="90" y1="292" x2="552" y2="292" stroke="#1B365D" stroke-width="1.6" marker-end="url(#mSolid)"/>
    <text x="320" y="286" text-anchor="middle" font-size="11.5" font-weight="600">POST /api/invoices (contractId, period)</text>

    <line x1="557" y1="318" x2="1032" y2="318" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="795" y="312" text-anchor="middle" font-size="11.3">Khóa hợp đồng → Snapshot đơn giá/ngày-người/chỉ số → DRAFT</text>

    <line x1="90" y1="348" x2="552" y2="348" stroke="#1B365D" stroke-width="1.6" marker-end="url(#mSolid)"/>
    <text x="320" y="342" text-anchor="middle" font-size="11.5" font-weight="600">POST /api/billing-documents/{id}/issue</text>

    <line x1="557" y1="376" x2="1032" y2="376" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="795" y="370" text-anchor="middle" font-size="11.3">Kiểm tra không còn alert PENDING → UPDATE status='ISSUED' (Trigger khóa sửa/xóa)</text>

    <line x1="557" y1="396" x2="95" y2="396" stroke="#475569" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#mDash)"/>
    <text x="320" y="391" text-anchor="middle" font-size="11.1" fill="#475569">200 OK (hoặc 409 nếu còn cảnh báo PENDING chưa xác nhận)</text>

    <!-- Phase 3: Payment & Overpayment Split -->
    <rect x="16" y="418" width="1108" height="168" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="418" width="290" height="22" rx="2" fill="#3730A3"/>
    <text x="26" y="433" font-size="11.2" font-weight="700" fill="#FFF">3. Thu tiền, Idempotency &amp; Tách tiền thừa</text>

    <line x1="90" y1="462" x2="792" y2="462" stroke="#1B365D" stroke-width="1.6" marker-end="url(#mSolid)"/>
    <text x="440" y="456" text-anchor="middle" font-size="11.5" font-weight="600">POST /api/invoices/{id}/payments (code, amount, cashAccountId, occurredAt, externalReference)</text>

    <line x1="797" y1="490" x2="1032" y2="490" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="915" y="484" text-anchor="middle" font-size="11">Khóa advisory + kiểm mã trùng</text>

    <line x1="797" y1="520" x2="1032" y2="520" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="915" y="514" text-anchor="middle" font-size="11">1× cash_transactions (IN)</text>

    <line x1="797" y1="548" x2="1032" y2="548" stroke="#1B365D" stroke-width="1.5" marker-end="url(#mSolid)"/>
    <text x="915" y="542" text-anchor="middle" font-size="10.3">RECEIVE → PAYMENT (≤ nợ); dư → ADVANCE</text>

    <line x1="797" y1="574" x2="95" y2="574" stroke="#475569" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#mDash)"/>
    <text x="445" y="568" text-anchor="middle" font-size="11.2" fill="#0F766E" font-weight="600">Trả kết quả thu &amp; số dư công nợ mới từ v_document_balances (gửi lại cùng code trả bản gốc, không thu trùng)</text>
  </svg>
  <div class="footer-note">
    <span>Nguồn: Hiện thực thực tế tại <code>Meters49.java</code>, <code>Billing49.java</code> và <code>Finance49.java</code>.</span>
    <span>UML Sequence Diagram</span>
  </div>
</div>
</body></html>`;

// 6. Sequence Diagram 2: Checkout -> Move-out Handover Comparison -> Damage Compensation -> Settlement -> Refund
const DIAG_SEQ_CHECKOUT = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">TUẦN TỰ: TRẢ PHÒNG — ĐỐI CHIẾU ĐỒ ĐẠC — BỒI THƯỜNG — QUYẾT TOÁN</div>
    <div class="subtitle">4 bước tách biệt: Kiểm kê → Bồi thường → Duyệt quyết toán → Chi hoàn thật</div>
  </div>
  <svg viewBox="0 0 1140 580" width="1140" height="580" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="sSolid" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#1B365D"/>
      </marker>
      <marker id="sDash" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#475569" stroke-width="1.6"/>
      </marker>
    </defs>

    <!-- Lifelines -->
    <g font-size="12.5" font-weight="700" text-anchor="middle">
      <rect x="20" y="10" width="140" height="40" rx="4" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
      <text x="90" y="28" fill="#1B365D">ADMIN / STAFF</text>
      <text x="90" y="43" font-size="10.8" font-weight="500" fill="#475569">(Màn hình Hợp đồng)</text>

      <rect x="235" y="10" width="165" height="40" rx="4" fill="#0F4C81"/>
      <text x="317" y="28" fill="#FFF">Api49 &amp; Files49</text>
      <text x="317" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Bàn giao &amp; Bồi thường</text>

      <rect x="475" y="10" width="165" height="40" rx="4" fill="#1B365D"/>
      <text x="557" y="28" fill="#FFF">Settlements49</text>
      <text x="557" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Trả phòng &amp; Quyết toán</text>

      <rect x="715" y="10" width="165" height="40" rx="4" fill="#3730A3"/>
      <text x="797" y="28" fill="#FFF">Finance49</text>
      <text x="797" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">Cấn cọc &amp; Chi hoàn</text>

      <rect x="955" y="10" width="165" height="40" rx="4" fill="#1E293B"/>
      <text x="1037" y="28" fill="#FFF">PostgreSQL 18.6</text>
      <text x="1037" y="43" font-size="10.8" font-weight="400" fill="#CBD5E1">V14 · ACID Tx</text>
    </g>

    <line x1="90" y1="50" x2="90" y2="565" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="317" y1="50" x2="317" y2="565" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="557" y1="50" x2="557" y2="565" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="797" y1="50" x2="797" y2="565" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>
    <line x1="1037" y1="50" x2="1037" y2="565" stroke="#94A3B8" stroke-width="1.4" stroke-dasharray="5 4"/>

    <!-- Step 1: Move-out handover & comparison -->
    <rect x="16" y="66" width="1108" height="190" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="66" width="335" height="22" rx="2" fill="#0F4C81"/>
    <text x="26" y="81" font-size="11.2" font-weight="700" fill="#FFF">1. Lập biên bản trả phòng &amp; Đối chiếu đồ đạc (V14)</text>

    <line x1="90" y1="108" x2="312" y2="108" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="200" y="102" text-anchor="middle" font-size="11.4" font-weight="600">POST /contracts/{id}/handovers (MOVE_OUT)</text>

    <line x1="317" y1="132" x2="1032" y2="132" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="675" y="126" text-anchor="middle" font-size="11.3">Tự nạp hạng mục từ biên bản MOVE_IN đã xác nhận và gắn FK original_item_id</text>

    <line x1="90" y1="160" x2="312" y2="160" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="200" y="154" text-anchor="middle" font-size="11.3">PUT items + POST /files/handover-item/{id} (ảnh hỏng)</text>

    <line x1="90" y1="190" x2="312" y2="190" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="200" y="184" text-anchor="middle" font-size="11.4" font-weight="600">POST /handovers/{id}/confirm (khóa kiểm kê)</text>

    <line x1="90" y1="220" x2="312" y2="220" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="200" y="214" text-anchor="middle" font-size="11.2" font-weight="600">POST /handovers/{id}/damage-compensation (350k)</text>

    <line x1="317" y1="244" x2="1032" y2="244" stroke="#1B365D" stroke-width="1.4" marker-end="url(#sSolid)"/>
    <text x="675" y="238" text-anchor="middle" font-size="11.2">Tạo &amp; phát hành chứng từ COMPENSATION (dòng DAMAGE gắn handover_item_id)</text>

    <!-- Step 2: Physical Checkout & Settlement Approval -->
    <rect x="16" y="268" width="1108" height="162" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="268" width="320" height="22" rx="2" fill="#1B365D"/>
    <text x="26" y="283" font-size="11.2" font-weight="700" fill="#FFF">2. Trả phòng vật lý &amp; Duyệt quyết toán tài chính</text>

    <line x1="90" y1="308" x2="552" y2="308" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="320" y="302" text-anchor="middle" font-size="11.4" font-weight="600">POST /contracts/{id}/checkout (billingEnd, returnedAt, returnHandoverId)</text>

    <line x1="557" y1="334" x2="1032" y2="334" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="795" y="328" text-anchor="middle" font-size="11.2">Đóng room_possessions, chuyển phòng → TURNOVER, tạo checkout_records (DRAFT)</text>

    <line x1="90" y1="364" x2="552" y2="364" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="320" y="358" text-anchor="middle" font-size="11.4" font-weight="600">POST /contracts/{id}/checkout/approve (useDeposit=true, applyAdvance=true)</text>

    <line x1="557" y1="392" x2="792" y2="392" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="675" y="386" text-anchor="middle" font-size="11">Điều chỉnh trả sớm + chốt điện cuối + cấn nợ</text>

    <line x1="797" y1="418" x2="1032" y2="418" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="915" y="412" text-anchor="middle" font-size="11">Ghi bút toán PAYMENT + snapshot</text>

    <!-- Step 3: Actual Refund Outflow -->
    <rect x="16" y="442" width="1108" height="114" rx="4" fill="#F8FAFC" fill-opacity="0.55" stroke="#CBD5E1"/>
    <rect x="16" y="442" width="320" height="22" rx="2" fill="#3730A3"/>
    <text x="26" y="457" font-size="11.2" font-weight="700" fill="#FFF">3. Xác nhận chi hoàn thực tế cho khách (OUT)</text>

    <line x1="90" y1="488" x2="792" y2="488" stroke="#1B365D" stroke-width="1.6" marker-end="url(#sSolid)"/>
    <text x="440" y="482" text-anchor="middle" font-size="11.4" font-weight="600">POST /fund-accounts/{id}/refund (cashAccountId, code, amount=3.450.000đ, paid=true)</text>

    <line x1="797" y1="516" x2="1032" y2="516" stroke="#1B365D" stroke-width="1.5" marker-end="url(#sSolid)"/>
    <text x="915" y="510" text-anchor="middle" font-size="11">cash_transactions (OUT) + REFUND</text>

    <line x1="797" y1="544" x2="95" y2="544" stroke="#475569" stroke-width="1.4" stroke-dasharray="4 3" marker-end="url(#sDash)"/>
    <text x="445" y="538" text-anchor="middle" font-size="11.2" fill="#0F766E" font-weight="600">Số dư quỹ cọc về 0 ₫; báo cáo dòng tiền phản ánh đúng khoản Hoàn quỹ (OUT)</text>
  </svg>
  <div class="footer-note">
    <span>Ví dụ minh họa theo dữ liệu demo P201: Cọc giữ 3.800.000 ₫ − Bồi thường kính nứt 350.000 ₫ = Hoàn thực tế 3.450.000 ₫.</span>
    <span>UML Sequence Diagram</span>
  </div>
</div>
</body></html>`;

const DIAGRAMS = [
  ['diag-01-architecture.png', DIAG_ARCH],
  ['diag-02-usecase.png', DIAG_USECASE],
  ['diag-03-db-8modules.png', DIAG_DB_MODULES],
  ['diag-04-erd-core-finance.png', DIAG_ERD_CORE],
  ['diag-05-seq-billing-alert.png', DIAG_SEQ_BILLING],
  ['diag-06-seq-checkout-handover.png', DIAG_SEQ_CHECKOUT],
];

async function run() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const page = await browser.newPage({
      viewport: { width: 1260, height: 900 },
      deviceScaleFactor: 2,
    });
    for (const [filename, html] of DIAGRAMS) {
      await page.setContent(html, { waitUntil: 'load' });
      await page.evaluate(() => document.fonts.ready);
      const box = page.locator('.canvas');
      const outPath = path.join(OUT_DIR, filename);
      await box.screenshot({ path: outPath });
      console.log('Generated', filename);
    }
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
