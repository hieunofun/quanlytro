import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('../frontend/node_modules/playwright');

const OUT_DIR = path.resolve(process.cwd(), 'docs/report-assets');
fs.mkdirSync(OUT_DIR, { recursive: true });

// Calibrated at 960px logical width × 2x scale = 1920px exact PNG (< 2048px Google Docs limit -> ZERO downscaling, 300+ DPI on A4!)
const COMMON_CSS = `
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #FFFFFF;
    color: #0F172A;
    font-family: 'Segoe UI', Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    text-rendering: geometricPrecision;
  }
  .canvas {
    width: 960px;
    padding: 20px 22px 16px 22px;
    background: #FFFFFF;
    border: 2px solid #94A3B8;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    gap: 12px;
    border-bottom: 2.5px solid #1B365D;
    padding-bottom: 8px;
    margin-bottom: 14px;
  }
  .title { font-size: 17px; font-weight: 800; color: #1B365D; letter-spacing: -0.2px; }
  .subtitle { font-size: 12px; color: #334155; font-weight: 600; text-align: right; }
  .footer-note {
    margin-top: 12px;
    padding-top: 7px;
    border-top: 1.5px solid #CBD5E1;
    font-size: 11.5px;
    font-weight: 500;
    color: #334155;
    display: flex;
    justify-content: space-between;
  }
`;

// 1. Architecture Diagram (4-layer, large readable labels)
const DIAG_ARCH = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.layers { display: flex; flex-direction: column; gap: 11px; }
.layer {
  border: 1.8px solid #64748B;
  border-radius: 6px;
  overflow: hidden;
  background: #F8FAFC;
}
.layer-head {
  background: #1B365D;
  color: #FFF;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
}
.layer-head.l2 { background: #1E40AF; }
.layer-head.l3 { background: #0F5257; }
.layer-head.l4 { background: #1E293B; }
.layer-head span.tag { font-weight: 600; font-size: 11.5px; color: #E2E8F0; }
.layer-body {
  padding: 10px 10px;
  display: grid;
  gap: 8px;
}
.g3 { grid-template-columns: repeat(3, 1fr); }
.g4 { grid-template-columns: repeat(4, 1fr); }
.box {
  background: #FFFFFF;
  border: 1.5px solid #94A3B8;
  border-left: 4.5px solid #1B365D;
  border-radius: 4px;
  padding: 7px 9px;
}
.box.accent { border-left-color: #1E40AF; }
.box.teal { border-left-color: #0F766E; }
.box.dark { border-left-color: #334155; }
.box h4 { font-size: 12.8px; font-weight: 700; color: #0F172A; margin-bottom: 3px; }
.box p { font-size: 11.6px; color: #1E293B; line-height: 1.35; }
.arrow-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin: -4px 0;
  font-size: 11.5px;
  font-weight: 700;
  color: #1B365D;
}
.arrow-pill {
  background: #EFF6FF;
  border: 1.5px solid #60A5FA;
  border-radius: 10px;
  padding: 2px 12px;
}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">KIẾN TRÚC 4 TẦNG HỆ THỐNG QUẢN LÝ NHÀ TRỌ (V14)</div>
    <div class="subtitle">Java 21 · Spring Boot 4.1.1 · PostgreSQL 18.6 · React 19</div>
  </div>
  <div class="layers">
    <div class="layer">
      <div class="layer-head"><span>1. TẦNG GIAO DIỆN WEB (REACT 19 + VITE 7 SPA)</span><span class="tag">Desktop &amp; Mobile 390px</span></div>
      <div class="layer-body g4">
        <div class="box"><h4>Tổng quan &amp; Báo cáo</h4><p>6 chỉ số thu–điện–nước, biểu đồ tháng/quý/năm, công nợ.</p></div>
        <div class="box"><h4>Hợp đồng &amp; Bàn giao</h4><p>Ký/gia hạn, xuất PDF, đối chiếu đồ nhận–trả, quyết toán.</p></div>
        <div class="box"><h4>Điện nước &amp; Hóa đơn</h4><p>Ghi công tơ, cảnh báo bất thường, hóa đơn &amp; thu tiền.</p></div>
        <div class="box"><h4>Cổng Khách &amp; Vận hành</h4><p>TENANT xem HĐ/hóa đơn/báo hỏng; quản lý 22 danh mục.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">REST JSON · Bearer JWT · Multipart Upload · PDF Stream</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l2"><span>2. TẦNG API, BẢO MẬT &amp; AOP (SPRING WEB MVC + SECURITY)</span><span class="tag">JJWT 0.13 · AspectJ</span></div>
      <div class="layer-body g4">
        <div class="box accent"><h4>JwtFilter &amp; Session</h4><p>Xác thực JWT, xoay vòng Refresh Token, thu hồi phiên.</p></div>
        <div class="box accent"><h4>Access49 (Phân quyền)</h4><p>ADMIN / STAFF (khu, phòng, hạn mức) / TENANT.</p></div>
        <div class="box accent"><h4>Api49 &amp; Controllers</h4><p>Điều phối REST cho hợp đồng, sổ quỹ, bàn giao, báo cáo.</p></div>
        <div class="box accent"><h4>ApiErrors &amp; TimingAOP</h4><p>Đổi 22 lỗi trigger DB sang HTTP 409; đo thời gian API.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">@Transactional · SELECT FOR UPDATE · pg_advisory_xact_lock · Idempotency</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l3"><span>3. TẦNG DỊCH VỤ NGHIỆP VỤ (PACKAGE com.hostel.model49)</span><span class="tag">JdbcTemplate + PDFBox 3.0.7</span></div>
      <div class="layer-body g4">
        <div class="box teal"><h4>Leases49 &amp; Catalog49</h4><p>Hợp đồng, phụ lục, cư trú, nhận/trả phòng, đồ đạc phòng.</p></div>
        <div class="box teal"><h4>Meters49</h4><p>Công tơ, điểm đo, đoạn tiêu thụ &amp; cảnh báo WEIGHTED_DAILY.</p></div>
        <div class="box teal"><h4>Billing49 &amp; Finance49</h4><p>Hóa đơn khóa bất biến, phiếu thu/chi thật, sổ cái quỹ khách.</p></div>
        <div class="box teal"><h4>Settlements49, Reports49 &amp; PDF</h4><p>Quyết toán cọc, báo cáo doanh thu, xuất PDF NotoSans &amp; lưu ảnh.</p></div>
      </div>
    </div>

    <div class="arrow-row"><span>▼</span><span class="arrow-pill">PreparedStatement (Chống SQLi) · Hibernate ddl-auto=validate · Flyway V1–V14</span><span>▲</span></div>

    <div class="layer">
      <div class="layer-head l4"><span>4. TẦNG DỮ LIỆU &amp; TỆP (POSTGRESQL 18.6 + UPLOAD STORAGE)</span><span class="tag">49 bảng · 461 cột</span></div>
      <div class="layer-body g3">
        <div class="box dark"><h4>49 Bảng chuẩn hóa</h4><p>49 PK · 152 FK · 133 CHECK · 7 GiST EXCLUDE chống chồng lịch.</p></div>
        <div class="box dark"><h4>10 View &amp; Trigger khóa sổ</h4><p>Tính nợ/số dư qua View; Trigger chặn sửa HĐ đã phát hành &amp; sổ quỹ.</p></div>
        <div class="box dark"><h4>Flyway &amp; Kho tệp chứng cứ</h4><p>Nâng cấp V1→V14 (lưu lịch sử V10); kiểm tra ImageIO + SHA-256.</p></div>
      </div>
    </div>
  </div>
  <div class="footer-note">
    <span>Đóng gói thành 1 file JAR duy nhất phục vụ cả giao diện tĩnh và REST API.</span>
    <span>131/131 test Java PASS · 20/20 ca SQL PASS</span>
  </div>
</div>
</body></html>`;

// 2. UML Use Case Diagram (crisp 916px SVG -> huge readable labels)
const DIAG_USECASE = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">BIỂU ĐỒ USE CASE TỔNG QUÁT HỆ THỐNG</div>
    <div class="subtitle">3 Tác nhân: Chủ nhà (ADMIN) · Nhân viên (STAFF) · Người thuê (TENANT)</div>
  </div>
  <svg viewBox="0 0 912 560" width="912" height="560" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="openArr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#1B365D" stroke-width="1.8"/>
      </marker>
    </defs>

    <rect x="155" y="8" width="602" height="544" rx="8" fill="#F8FAFC" stroke="#1B365D" stroke-width="2.2"/>
    <text x="456" y="30" text-anchor="middle" font-size="14.5" font-weight="800" fill="#1B365D">HỆ THỐNG QUẢN LÝ NHÀ TRỌ (V14)</text>

    <!-- Left column groups -->
    <rect x="170" y="42" width="276" height="136" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="180" y="58" font-size="11.5" font-weight="700" fill="#1B365D">1. Quản trị &amp; Danh mục</text>
    <ellipse cx="308" cy="82" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="86" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Tài khoản, phân công &amp; hạn mức</text>
    <ellipse cx="308" cy="118" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="122" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Khu nhà, phòng, giá &amp; ngưỡng</text>
    <ellipse cx="308" cy="154" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="158" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Đồ đạc &amp; tài sản trong phòng</text>

    <rect x="170" y="190" width="276" height="150" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="180" y="206" font-size="11.5" font-weight="700" fill="#1B365D">3. Điện nước &amp; Cảnh báo</text>
    <ellipse cx="308" cy="232" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="236" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Ghi chỉ số &amp; thay công tơ</text>
    <ellipse cx="308" cy="276" rx="124" ry="16" fill="#FEF3C7" stroke="#B45309" stroke-width="1.8"/>
    <text x="308" y="280" text-anchor="middle" font-size="12" font-weight="700" fill="#92400E">Đánh giá cảnh báo bất thường</text>
    <line x1="308" y1="248" x2="308" y2="260" stroke="#1B365D" stroke-width="1.4" stroke-dasharray="3 2" marker-end="url(#openArr)"/>
    <text x="348" y="257" font-size="10.5" font-weight="700" font-style="italic" fill="#1B365D">«include»</text>
    <ellipse cx="308" cy="316" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="320" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Kiểm tra &amp; xác nhận cảnh báo</text>

    <rect x="170" y="352" width="276" height="104" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="180" y="368" font-size="11.5" font-weight="700" fill="#1B365D">5. Báo cáo &amp; Công nợ</text>
    <ellipse cx="308" cy="394" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="398" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Tổng quan 6 chỉ số &amp; dư nợ</text>
    <ellipse cx="308" cy="432" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="308" y="436" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Báo cáo Tháng/Quý/Năm &amp; PDF</text>

    <rect x="170" y="468" width="276" height="72" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="180" y="484" font-size="11.5" font-weight="700" fill="#1B365D">7. Tài khoản chung</text>
    <ellipse cx="308" cy="512" rx="124" ry="16" fill="#F1F5F9" stroke="#334155" stroke-width="1.6"/>
    <text x="308" y="516" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Đăng nhập JWT, mật khẩu, TB</text>

    <!-- Right column groups -->
    <rect x="466" y="42" width="276" height="176" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="476" y="58" font-size="11.5" font-weight="700" fill="#1B365D">2. Hợp đồng &amp; Bàn giao</text>
    <ellipse cx="604" cy="80" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="84" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Lập, ký, gia hạn &amp; xuất PDF</text>
    <ellipse cx="604" cy="114" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="118" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Cư trú thực tế &amp; giao phòng</text>
    <ellipse cx="604" cy="148" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="152" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Biên bản đối chiếu nhận/trả</text>
    <line x1="604" y1="181" x2="604" y2="164" stroke="#B45309" stroke-width="1.4" stroke-dasharray="3 2" marker-end="url(#openArr)"/>
    <text x="646" y="176" font-size="10.2" font-weight="700" font-style="italic" fill="#92400E">«extend»</text>
    <ellipse cx="604" cy="197" rx="120" ry="14.5" fill="#FEF3C7" stroke="#B45309" stroke-width="1.5"/>
    <text x="604" y="201" text-anchor="middle" font-size="11.4" font-weight="700" fill="#92400E">Ảnh hỏng đồ &amp; bồi thường</text>

    <rect x="466" y="226" width="276" height="142" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="476" y="242" font-size="11.5" font-weight="700" fill="#1B365D">4. Hóa đơn, Sổ quỹ &amp; Quyết toán</text>
    <ellipse cx="604" cy="266" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="270" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Lập nháp, phát hành &amp; điều chỉnh</text>
    <ellipse cx="604" cy="303" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="307" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Thu tiền, tách quỹ cọc / tiền dư</text>
    <ellipse cx="604" cy="340" rx="124" ry="15" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="344" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Duyệt quyết toán &amp; chi hoàn cọc</text>

    <rect x="466" y="376" width="276" height="164" rx="5" fill="#FFF" stroke="#94A3B8" stroke-width="1.3" stroke-dasharray="4 3"/>
    <text x="476" y="392" font-size="11.5" font-weight="700" fill="#166534">6. Sự cố &amp; Cổng Người thuê</text>
    <ellipse cx="604" cy="420" rx="124" ry="16" fill="#F0FDF4" stroke="#166534" stroke-width="1.6"/>
    <text x="604" y="424" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Xem hợp đồng, hóa đơn, lịch sử</text>
    <ellipse cx="604" cy="462" rx="124" ry="16" fill="#F0FDF4" stroke="#166534" stroke-width="1.6"/>
    <text x="604" y="466" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Gửi báo hỏng &amp; đính kèm ảnh</text>
    <ellipse cx="604" cy="506" rx="124" ry="16" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.6"/>
    <text x="604" y="510" text-anchor="middle" font-size="12" font-weight="700" fill="#0F172A">Xử lý sự cố, chi phí &amp; bảo trì</text>

    <!-- Actors -->
    <g transform="translate(68, 105)">
      <circle cx="0" cy="-22" r="13" fill="#EFF6FF" stroke="#1B365D" stroke-width="2.4"/>
      <line x1="0" y1="-9" x2="0" y2="22" stroke="#1B365D" stroke-width="2.4"/>
      <line x1="-20" y1="2" x2="20" y2="2" stroke="#1B365D" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="-16" y2="46" stroke="#1B365D" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="16" y2="46" stroke="#1B365D" stroke-width="2.4"/>
      <text x="0" y="64" text-anchor="middle" font-size="13" font-weight="800" fill="#1B365D">Chủ nhà</text>
      <text x="0" y="80" text-anchor="middle" font-size="11.5" font-weight="700" fill="#334155">(ADMIN)</text>
    </g>

    <line x1="68" y1="194" x2="68" y2="306" stroke="#1B365D" stroke-width="2"/>
    <polygon points="68,318 61,304 75,304" fill="#FFFFFF" stroke="#1B365D" stroke-width="2"/>
    <text x="78" y="256" font-size="11" font-weight="600" font-style="italic" fill="#1B365D">kế thừa</text>

    <g transform="translate(68, 355)">
      <circle cx="0" cy="-22" r="13" fill="#EFF6FF" stroke="#1E40AF" stroke-width="2.4"/>
      <line x1="0" y1="-9" x2="0" y2="22" stroke="#1E40AF" stroke-width="2.4"/>
      <line x1="-20" y1="2" x2="20" y2="2" stroke="#1E40AF" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="-16" y2="46" stroke="#1E40AF" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="16" y2="46" stroke="#1E40AF" stroke-width="2.4"/>
      <text x="0" y="64" text-anchor="middle" font-size="13" font-weight="800" fill="#1E40AF">Nhân viên</text>
      <text x="0" y="80" text-anchor="middle" font-size="11.5" font-weight="700" fill="#334155">(STAFF)</text>
      <text x="0" y="95" text-anchor="middle" font-size="10.5" font-weight="600" fill="#475569">Theo khu/phòng</text>
    </g>

    <g transform="translate(838, 435)">
      <circle cx="0" cy="-22" r="13" fill="#F0FDF4" stroke="#166534" stroke-width="2.4"/>
      <line x1="0" y1="-9" x2="0" y2="22" stroke="#166534" stroke-width="2.4"/>
      <line x1="-20" y1="2" x2="20" y2="2" stroke="#166534" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="-16" y2="46" stroke="#166534" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="16" y2="46" stroke="#166534" stroke-width="2.4"/>
      <text x="0" y="64" text-anchor="middle" font-size="13" font-weight="800" fill="#166534">Người thuê</text>
      <text x="0" y="80" text-anchor="middle" font-size="11.5" font-weight="700" fill="#334155">(TENANT)</text>
    </g>

    <!-- Associations -->
    <line x1="92" y1="98" x2="184" y2="82" stroke="#1B365D" stroke-width="1.7"/>
    <line x1="92" y1="108" x2="184" y2="118" stroke="#1B365D" stroke-width="1.7"/>
    <line x1="92" y1="118" x2="184" y2="154" stroke="#1B365D" stroke-width="1.7"/>

    <line x1="92" y1="340" x2="184" y2="232" stroke="#1E40AF" stroke-width="1.6"/>
    <line x1="92" y1="348" x2="184" y2="316" stroke="#1E40AF" stroke-width="1.6"/>
    <line x1="92" y1="360" x2="184" y2="394" stroke="#1E40AF" stroke-width="1.6"/>
    <line x1="92" y1="370" x2="184" y2="432" stroke="#1E40AF" stroke-width="1.6"/>
    <line x1="92" y1="382" x2="184" y2="512" stroke="#1E40AF" stroke-width="1.6"/>

    <path d="M 92 354 L 456 354 L 480 266" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 354 L 480 303" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 354 L 480 340" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 354 L 456 80 L 480 80" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 114 L 480 114" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 148 L 480 148" fill="none" stroke="#1E40AF" stroke-width="1.5"/>
    <path d="M 456 354 L 456 506 L 480 506" fill="none" stroke="#1E40AF" stroke-width="1.5"/>

    <line x1="814" y1="425" x2="728" y2="420" stroke="#166534" stroke-width="1.8"/>
    <line x1="814" y1="442" x2="728" y2="462" stroke="#166534" stroke-width="1.8"/>
    <path d="M 814 456 L 772 456 L 772 546 L 308 546 L 308 528" fill="none" stroke="#166534" stroke-width="1.7"/>
  </svg>
  <div class="footer-note">
    <span>Mọi kiểm tra quyền và phạm vi khu/phòng đều thực hiện tại backend (Access49.java).</span>
    <span>UML 2.5</span>
  </div>
</div>
</body></html>`;

// 3. 8-Module Database Overview (960px logical width -> bold readable table names)
const DIAG_DB_MODULES = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.mod {
  border: 1.8px solid #64748B;
  border-radius: 5px;
  overflow: hidden;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
}
.mod-hd {
  background: #1B365D;
  color: #FFF;
  padding: 6px 8px;
  font-size: 12.2px;
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
.cnt { background: rgba(255,255,255,0.22); padding: 1px 6px; border-radius: 8px; font-size: 10.8px; }
.mod-bd { padding: 7px 8px; flex: 1; }
.tbl-list { list-style: none; }
.tbl-list li {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 11.4px;
  font-weight: 600;
  color: #0F172A;
  padding: 1.5px 0;
  border-bottom: 1px dotted #CBD5E1;
}
.tbl-list li:last-child { border-bottom: none; }
.v14 {
  background: #FEF3C7;
  color: #92400E;
  font-family: 'Segoe UI', Arial, sans-serif;
  font-weight: 700;
  font-size: 9.5px;
  padding: 0 4px;
  border-radius: 3px;
}
.mod-ft {
  background: #F1F5F9;
  border-top: 1px solid #CBD5E1;
  padding: 5px 8px;
  font-size: 10.8px;
  font-weight: 500;
  color: #1E293B;
}
.summary-bar { margin-top: 11px; display: grid; grid-template-columns: 1.35fr 1fr; gap: 10px; }
.info-card {
  background: #F8FAFC;
  border: 1.5px solid #94A3B8;
  border-left: 4.5px solid #1B365D;
  border-radius: 4px;
  padding: 7px 10px;
  font-size: 11.5px;
  line-height: 1.42;
}
.info-card.v14box { border-left-color: #B45309; background: #FFFBEB; }
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">TỔ CHỨC CSDL 49 BẢNG THEO 8 PHÂN HỆ (POSTGRESQL 18.6 — V14)</div>
    <div class="subtitle">49 bảng · 461 cột · 152 FK · 133 CHECK · 7 EXCLUDE · 10 View</div>
  </div>
  <div class="grid">
    <div class="mod">
      <div class="mod-hd"><span>A. Tài khoản &amp; Quyền</span><span class="cnt">6</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>users</li><li>tenant_profiles</li><li>employee_assignments</li>
        <li>staff_permission_grants</li><li>password_reset_tokens</li><li>audit_events</li>
      </ul></div>
      <div class="mod-ft">Phân quyền, phạm vi khu/phòng &amp; nhật ký.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m2"><span>B. Khu, Phòng &amp; Đồ đạc</span><span class="cnt">7</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>properties · rooms</li><li>room_price_history</li><li>room_operating_periods</li>
        <li>asset_types · assets</li><li>asset_placements</li>
      </ul></div>
      <div class="mod-ft">Lịch sử giá phòng &amp; vị trí tài sản theo kỳ.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m3"><span>C. Hợp đồng &amp; Bàn giao</span><span class="cnt">12</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>reservations · contracts</li><li>contract_parties</li><li>contract_contacts</li>
        <li>contract_residencies</li><li>contract_terms <span class="v14">+5 V14</span></li>
        <li>room_possessions</li><li>move_out_notices</li><li>handovers</li>
        <li>handover_items <span class="v14">+1 V14</span></li><li>contract_violations</li><li>checkout_records</li>
      </ul></div>
      <div class="mod-ft">Tách người ký / thực ở; đối chiếu nhận–trả.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m4"><span>D. Dịch vụ &amp; Điện nước</span><span class="cnt">9</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>service_items</li><li>service_price_configs</li><li style="font-size:10.3px;">contract_service_subscriptions</li>
        <li>meters · meter_readings</li><li>consumption_segments</li><li>alert_policies</li>
        <li>consumption_alerts</li><li>alert_baseline_sources</li>
      </ul></div>
      <div class="mod-ft">Công tơ → điểm đo → đoạn dùng &amp; cảnh báo.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m6"><span>E. Sự cố &amp; Bảo trì</span><span class="cnt">4</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>maintenance_requests</li><li>maintenance_events</li>
        <li>maintenance_costs</li><li>maintenance_schedules</li>
      </ul></div>
      <div class="mod-ft">Báo hỏng theo vị trí; tách chi phí sửa thật.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m5"><span>F. Hóa đơn &amp; Sổ quỹ</span><span class="cnt">7</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>billing_documents</li><li>billing_items</li><li>cash_accounts</li>
        <li>cash_transactions</li><li>fund_accounts</li><li>fund_entries</li><li>fund_holds</li>
      </ul></div>
      <div class="mod-ft">Tách chứng từ nợ ↔ tiền thật ↔ quỹ khách.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m7"><span>G. Thông báo</span><span class="cnt">2</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>notifications</li><li>notification_recipients</li>
      </ul></div>
      <div class="mod-ft">Theo dõi trạng thái đọc từng tài khoản.</div>
    </div>
    <div class="mod">
      <div class="mod-hd m8"><span>H. Tệp &amp; Ảnh minh chứng</span><span class="cnt">2</span></div>
      <div class="mod-bd"><ul class="tbl-list">
        <li>stored_files</li><li>file_attachments</li>
      </ul></div>
      <div class="mod-ft">10 cột FK đích (CHECK đúng 1 cột khác NULL).</div>
    </div>
  </div>
  <div class="summary-bar">
    <div class="info-card"><b>10 View tính tự động (không lưu cột tổng dư thừa):</b> <code>v_contract_current</code>, <code>v_room_status</code>, <code>v_segments</code>, <code>v_billing_lines</code>, <code>v_fund_movements</code>, <code>v_fund_balances</code>, <code>v_item_applications</code>, <code>v_item_balances</code>, <code>v_document_balances</code>, <code>v_maintenance</code>.</div>
    <div class="info-card v14box"><b>Cập nhật V14:</b> Thêm <code>handover_items.original_item_id</code> (đối chiếu đồ lúc nhận–trả) và 5 cột Bên A/Bên B trên <code>contract_terms</code> để xuất PDF.</div>
  </div>
</div>
</body></html>`;

// 4. Core ERD (912px SVG -> super crisp large fonts)
const DIAG_ERD_CORE = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">ERD TRỌNG TÂM: HỢP ĐỒNG – BÀN GIAO (V14) &amp; HÓA ĐƠN – SỔ QUỸ</div>
    <div class="subtitle">Ký hiệu bản số 1:N / 0..1 · Tên bảng và cột đối chiếu nguyên văn docs/quanlytro.sql</div>
  </div>
  <svg viewBox="0 0 912 525" width="912" height="525" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <rect x="4" y="8" width="434" height="510" rx="6" fill="#F8FAFC" stroke="#94A3B8" stroke-width="1.4"/>
    <text x="14" y="27" font-size="12" font-weight="800" fill="#1B365D">HỢP ĐỒNG, PHỤ LỤC &amp; BÀN GIAO ĐỒ ĐẠC (V14)</text>

    <rect x="450" y="8" width="458" height="510" rx="6" fill="#F8FAFC" stroke="#94A3B8" stroke-width="1.4"/>
    <text x="460" y="27" font-size="12" font-weight="800" fill="#3730A3">CHỨNG TỪ PHẢI THU &amp; SỔ QUỸ TÀI CHÍNH</text>

    <!-- contracts -->
    <g transform="translate(14, 42)">
      <rect width="192" height="126" rx="4" fill="#FFF" stroke="#1B365D" stroke-width="1.8"/>
      <rect width="192" height="25" rx="3" fill="#1B365D"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">contracts</text>
      <text x="8" y="43" font-size="11.2" font-weight="600" font-family="Consolas,monospace">PK id : bigint</text>
      <text x="8" y="60" font-size="11.2" font-family="Consolas,monospace">FK room_id → rooms</text>
      <text x="8" y="77" font-size="11.2" font-family="Consolas,monospace">UQ contract_code</text>
      <text x="8" y="94" font-size="11.2" font-family="Consolas,monospace">   start_date : date</text>
      <text x="8" y="111" font-size="11.2" font-family="Consolas,monospace">   status: ACTIVE|ENDED…</text>
    </g>

    <!-- contract_terms -->
    <g transform="translate(228, 42)">
      <rect width="198" height="174" rx="4" fill="#FFF" stroke="#1B365D" stroke-width="1.8"/>
      <rect width="198" height="25" rx="3" fill="#1B365D"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">contract_terms</text>
      <text x="8" y="42" font-size="10.9" font-weight="600" font-family="Consolas,monospace">PK id · FK contract_id</text>
      <text x="8" y="58" font-size="10.9" font-family="Consolas,monospace">   version_no · lease_end</text>
      <text x="8" y="74" font-size="10.9" font-family="Consolas,monospace">   rental_price</text>
      <text x="8" y="90" font-size="10.9" font-family="Consolas,monospace">   deposit_required</text>
      <text x="8" y="105" font-size="10.9" font-family="Consolas,monospace">   payment_day · signed_at</text>
      <rect x="4" y="112" width="190" height="57" rx="3" fill="#FEF3C7" stroke="#D97706" stroke-width="1.2"/>
      <text x="8" y="126" font-size="10.3" font-weight="700" fill="#92400E">+5 cột V14 (xuất PDF):</text>
      <text x="8" y="140" font-size="10.1" font-family="Consolas,monospace" fill="#78350F">lessor_name, lessor_identity</text>
      <text x="8" y="153" font-size="10.1" font-family="Consolas,monospace" fill="#78350F">lessor_phone, lessor_address</text>
      <text x="8" y="165" font-size="10.1" font-family="Consolas,monospace" fill="#78350F">tenant_address</text>
    </g>

    <!-- handovers -->
    <g transform="translate(14, 202)">
      <rect width="192" height="126" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.8"/>
      <rect width="192" height="25" rx="3" fill="#0F4C81"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">handovers</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK contract_id</text>
      <text x="8" y="60" font-size="10.5" font-family="Consolas,monospace">handover_type: MOVE_IN|OUT</text>
      <text x="8" y="77" font-size="10.9" font-family="Consolas,monospace">   status: DRAFT|CONFIRMED</text>
      <text x="8" y="94" font-size="11.1" font-family="Consolas,monospace">   occurred_at</text>
      <text x="8" y="111" font-size="11.1" font-family="Consolas,monospace">   confirmed_by</text>
    </g>

    <!-- handover_items -->
    <g transform="translate(228, 236)">
      <rect width="198" height="148" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.8"/>
      <rect width="198" height="25" rx="3" fill="#0F4C81"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">handover_items</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK handover_id</text>
      <text x="8" y="60" font-size="11.1" font-family="Consolas,monospace">FK asset_placement_id</text>
      <text x="8" y="77" font-size="11.1" font-family="Consolas,monospace">   item_name_snapshot</text>
      <text x="8" y="94" font-size="10.6" font-family="Consolas,monospace">   quantity · condition_note</text>
      <rect x="4" y="104" width="190" height="38" rx="3" fill="#FEF3C7" stroke="#D97706" stroke-width="1.2"/>
      <text x="8" y="119" font-size="10.5" font-weight="700" fill="#92400E">V14 đối chiếu nhận–trả:</text>
      <text x="8" y="135" font-size="10.8" font-weight="600" font-family="Consolas,monospace" fill="#78350F">FK original_item_id</text>
    </g>

    <!-- checkout_records -->
    <g transform="translate(14, 368)">
      <rect width="192" height="132" rx="4" fill="#FFF" stroke="#0F4C81" stroke-width="1.8"/>
      <rect width="192" height="25" rx="3" fill="#0F4C81"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">checkout_records</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK contract_id</text>
      <text x="8" y="60" font-size="11.1" font-family="Consolas,monospace">FK return_handover_id</text>
      <text x="8" y="77" font-size="11.1" font-family="Consolas,monospace">   billing_end · status</text>
      <text x="8" y="94" font-size="11.1" font-family="Consolas,monospace">   deposit_snapshot</text>
      <text x="8" y="111" font-size="11.1" font-family="Consolas,monospace">   outstanding_snapshot</text>
    </g>

    <!-- billing_documents -->
    <g transform="translate(462, 42)">
      <rect width="208" height="142" rx="4" fill="#FFF" stroke="#3730A3" stroke-width="1.8"/>
      <rect width="208" height="25" rx="3" fill="#3730A3"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">billing_documents</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK contract_id</text>
      <text x="8" y="60" font-size="11.1" font-family="Consolas,monospace">UQ document_code</text>
      <text x="8" y="77" font-size="10.4" font-family="Consolas,monospace">document_type: MONTHLY|COMP…</text>
      <text x="8" y="94" font-size="10.7" font-family="Consolas,monospace">billing_period · due_date</text>
      <text x="8" y="111" font-size="11.1" font-family="Consolas,monospace">   status: DRAFT|ISSUED</text>
      <text x="8" y="129" font-size="10.6" font-style="italic" fill="#334155">* Trigger khóa khi ISSUED</text>
    </g>

    <!-- billing_items -->
    <g transform="translate(690, 42)">
      <rect width="208" height="156" rx="4" fill="#FFF" stroke="#3730A3" stroke-width="1.8"/>
      <rect width="208" height="25" rx="3" fill="#3730A3"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">billing_items</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK document_id</text>
      <text x="8" y="60" font-size="11.1" font-family="Consolas,monospace">FK segment_id (công tơ)</text>
      <text x="8" y="77" font-size="11.1" font-family="Consolas,monospace">FK handover_item_id</text>
      <text x="8" y="94" font-size="10.6" font-family="Consolas,monospace">   item_kind · direction</text>
      <text x="8" y="111" font-size="11.1" font-family="Consolas,monospace">   unit_price_snapshot</text>
      <text x="8" y="128" font-size="11.1" font-family="Consolas,monospace">   quantity_snapshot</text>
      <text x="8" y="145" font-size="11.1" font-family="Consolas,monospace">   source_key · line_no</text>
    </g>

    <!-- cash_transactions -->
    <g transform="translate(462, 224)">
      <rect width="208" height="136" rx="4" fill="#FFF" stroke="#0F766E" stroke-width="1.8"/>
      <rect width="208" height="25" rx="3" fill="#0F766E"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">cash_transactions</text>
      <text x="8" y="43" font-size="10.8" font-weight="600" font-family="Consolas,monospace">PK id · FK cash_account_id</text>
      <text x="8" y="60" font-size="11.1" font-family="Consolas,monospace">FK payment_document_id</text>
      <text x="8" y="77" font-size="11.1" font-family="Consolas,monospace">UQ transaction_code</text>
      <text x="8" y="94" font-size="11.1" font-family="Consolas,monospace">   direction: IN | OUT</text>
      <text x="8" y="111" font-size="11.1" font-family="Consolas,monospace">   amount · method</text>
      <text x="8" y="127" font-size="10.6" font-style="italic" fill="#0F766E">* Tiền mặt / Ngân hàng thật</text>
    </g>

    <!-- fund_accounts -->
    <g transform="translate(462, 384)">
      <rect width="208" height="122" rx="4" fill="#FFF" stroke="#4338CA" stroke-width="1.8"/>
      <rect width="208" height="25" rx="3" fill="#4338CA"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">fund_accounts</text>
      <text x="8" y="43" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · FK contract_id</text>
      <text x="8" y="60" font-size="10.9" font-family="Consolas,monospace">fund_type: DEPOSIT|ADVANCE</text>
      <text x="8" y="76" font-size="10.9" font-family="Consolas,monospace">  |RESERVATION|UNAPPLIED</text>
      <text x="8" y="95" font-size="10.6" font-style="italic" fill="#334155">* Số dư tính qua View</text>
      <text x="8" y="111" font-size="10.6" font-style="italic" fill="#334155">* Có bảng tạm giữ fund_holds</text>
    </g>

    <!-- fund_entries -->
    <g transform="translate(690, 236)">
      <rect width="208" height="270" rx="4" fill="#FFF" stroke="#4338CA" stroke-width="1.8"/>
      <rect width="208" height="25" rx="3" fill="#4338CA"/>
      <text x="8" y="17" fill="#FFF" font-size="12.5" font-weight="700">fund_entries (Sổ cái)</text>
      <text x="8" y="45" font-size="11.1" font-weight="600" font-family="Consolas,monospace">PK id · UQ entry_code</text>
      <text x="8" y="65" font-size="11.1" font-family="Consolas,monospace">entry_type:</text>
      <text x="8" y="82" font-size="11" font-family="Consolas,monospace"> • RECEIVE (tiền vào quỹ)</text>
      <text x="8" y="98" font-size="11" font-family="Consolas,monospace"> • PAYMENT (trả/cấn nợ)</text>
      <text x="8" y="114" font-size="11" font-family="Consolas,monospace"> • TRANSFER (chuyển quỹ)</text>
      <text x="8" y="130" font-size="11" font-family="Consolas,monospace"> • REFUND (chi hoàn thật)</text>
      <text x="8" y="146" font-size="11" font-family="Consolas,monospace"> • REVERSAL (đảo sai)</text>
      <text x="8" y="168" font-size="11.1" font-family="Consolas,monospace">FK cash_transaction_id</text>
      <text x="8" y="186" font-size="10.4" font-family="Consolas,monospace">FK from_fund_id·to_fund_id</text>
      <text x="8" y="204" font-size="11.1" font-family="Consolas,monospace">FK debit_item_id</text>
      <text x="8" y="222" font-size="11.1" font-family="Consolas,monospace">   amount (&gt; 0)</text>
      <text x="8" y="246" font-size="10.6" font-weight="600" fill="#4338CA">* Chỉ ghi thêm (Append-only)</text>
    </g>

    <!-- Connectors -->
    <line x1="206" y1="88" x2="228" y2="88" stroke="#1B365D" stroke-width="2"/>
    <text x="209" y="82" font-size="11" font-weight="800" fill="#1B365D">1:N</text>

    <line x1="110" y1="168" x2="110" y2="202" stroke="#0F4C81" stroke-width="2"/>
    <text x="116" y="190" font-size="11" font-weight="800" fill="#0F4C81">1:N</text>

    <line x1="206" y1="274" x2="228" y2="274" stroke="#0F4C81" stroke-width="2"/>
    <text x="209" y="268" font-size="11" font-weight="800" fill="#0F4C81">1:N</text>

    <line x1="110" y1="328" x2="110" y2="368" stroke="#0F4C81" stroke-width="2"/>
    <text x="116" y="352" font-size="11" font-weight="800" fill="#0F4C81">0..1</text>

    <!-- Self loop on handover_items -->
    <path d="M 328 384 L 328 408 L 432 408 L 432 362 L 426 362" fill="none" stroke="#D97706" stroke-width="1.8" stroke-dasharray="4 2"/>
    <text x="242" y="423" font-size="10.8" font-weight="700" fill="#92400E">0..1 trỏ dòng nhận phòng gốc</text>

    <!-- contracts -> billing_documents -->
    <path d="M 110 42 L 110 33 L 555 33 L 555 42" fill="none" stroke="#3730A3" stroke-width="1.8"/>
    <rect x="422" y="22" width="40" height="16" fill="#FFF"/>
    <text x="442" y="34" text-anchor="middle" font-size="11" font-weight="800" fill="#3730A3">1:N</text>

    <!-- handover_items -> billing_items -->
    <path d="M 426 254 L 444 254 L 444 208 L 760 208 L 760 198" fill="none" stroke="#D97706" stroke-width="1.7" stroke-dasharray="5 3"/>
    <text x="605" y="203" text-anchor="middle" font-size="10.8" font-weight="700" fill="#92400E">Căn cứ lập bồi thường hỏng đồ</text>

    <line x1="670" y1="105" x2="690" y2="105" stroke="#3730A3" stroke-width="2"/>
    <text x="672" y="99" font-size="11" font-weight="800" fill="#3730A3">1:N</text>

    <line x1="815" y1="198" x2="815" y2="236" stroke="#4338CA" stroke-width="2"/>
    <text x="821" y="222" font-size="11" font-weight="800" fill="#4338CA">1:N</text>

    <line x1="670" y1="295" x2="690" y2="295" stroke="#0F766E" stroke-width="2"/>
    <text x="672" y="289" font-size="11" font-weight="800" fill="#0F766E">1:N</text>

    <line x1="670" y1="435" x2="690" y2="435" stroke="#4338CA" stroke-width="2"/>
    <text x="672" y="429" font-size="11" font-weight="800" fill="#4338CA">1:N</text>
  </svg>
</div>
</body></html>`;

// 5. Sequence 1: Meter -> Alert -> Invoice -> Payment (912px SVG, crisp bold text)
const DIAG_SEQ_BILLING = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">LUỒNG GHI CHỈ SỐ — CẢNH BÁO — HÓA ĐƠN — THU TIỀN</div>
    <div class="subtitle">Khóa dòng FOR UPDATE · Chặn phát hành khi cảnh báo PENDING · Chống thu lặp</div>
  </div>
  <svg viewBox="0 0 912 500" width="912" height="500" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="a1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#1B365D"/>
      </marker>
      <marker id="a2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#334155" stroke-width="1.8"/>
      </marker>
    </defs>

    <g font-size="12" font-weight="700" text-anchor="middle">
      <rect x="12" y="8" width="122" height="36" rx="4" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.8"/>
      <text x="73" y="30" fill="#1B365D">Quản lý (UI)</text>

      <rect x="198" y="8" width="136" height="36" rx="4" fill="#1B365D"/>
      <text x="266" y="30" fill="#FFF">Meters49</text>

      <rect x="394" y="8" width="136" height="36" rx="4" fill="#1B365D"/>
      <text x="462" y="30" fill="#FFF">Billing49</text>

      <rect x="590" y="8" width="136" height="36" rx="4" fill="#1B365D"/>
      <text x="658" y="30" fill="#FFF">Finance49</text>

      <rect x="776" y="8" width="126" height="36" rx="4" fill="#1E293B"/>
      <text x="839" y="30" fill="#FFF">PostgreSQL</text>
    </g>

    <line x1="73" y1="44" x2="73" y2="490" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="266" y1="44" x2="266" y2="490" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="462" y1="44" x2="462" y2="490" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="658" y1="44" x2="658" y2="490" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="839" y1="44" x2="839" y2="490" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>

    <!-- Block 1 -->
    <rect x="8" y="56" width="896" height="142" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="56" width="235" height="20" fill="#1B365D"/>
    <text x="16" y="70" font-size="11" font-weight="700" fill="#FFF">1. Ghi công tơ &amp; Cảnh báo bất thường</text>

    <line x1="73" y1="96" x2="261" y2="96" stroke="#1B365D" stroke-width="1.8" marker-end="url(#a1)"/>
    <text x="168" y="90" text-anchor="middle" font-size="11.2" font-weight="700">POST /meter-readings</text>

    <line x1="266" y1="120" x2="834" y2="120" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="550" y="114" text-anchor="middle" font-size="11.3" font-weight="600">FOR UPDATE (rooms, contracts, meters) → lưu điểm đo &amp; đoạn tiêu thụ</text>

    <line x1="266" y1="150" x2="834" y2="150" stroke="#B45309" stroke-width="1.8" marker-end="url(#a1)"/>
    <text x="550" y="144" text-anchor="middle" font-size="11.3" font-weight="700" fill="#92400E">analyze(): So mức dùng/ngày với TB lịch sử → tạo cảnh báo PENDING nếu vượt %</text>

    <line x1="73" y1="182" x2="261" y2="182" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="168" y="176" text-anchor="middle" font-size="11.2" font-weight="600">PUT /alerts/{id} (Xác nhận / Bỏ qua)</text>

    <!-- Block 2 -->
    <rect x="8" y="210" width="896" height="128" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="210" width="235" height="20" fill="#0F4C81"/>
    <text x="16" y="224" font-size="11" font-weight="700" fill="#FFF">2. Lập nháp &amp; Phát hành hóa đơn</text>

    <line x1="73" y1="250" x2="457" y2="250" stroke="#1B365D" stroke-width="1.8" marker-end="url(#a1)"/>
    <text x="265" y="244" text-anchor="middle" font-size="11.3" font-weight="700">POST /invoices (contractId, period)</text>

    <line x1="462" y1="274" x2="834" y2="274" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="648" y="268" text-anchor="middle" font-size="11.2" font-weight="600">Chụp snapshot giá, ngày ở, chỉ số → DRAFT</text>

    <line x1="73" y1="302" x2="457" y2="302" stroke="#1B365D" stroke-width="1.8" marker-end="url(#a1)"/>
    <text x="265" y="296" text-anchor="middle" font-size="11.3" font-weight="700">POST /billing-documents/{id}/issue</text>

    <line x1="462" y1="324" x2="834" y2="324" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="648" y="318" text-anchor="middle" font-size="11.2" font-weight="600">Kiểm hết PENDING → ISSUED (Trigger khóa cứng)</text>

    <!-- Block 3 -->
    <rect x="8" y="350" width="896" height="136" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="350" width="255" height="20" fill="#3730A3"/>
    <text x="16" y="364" font-size="11" font-weight="700" fill="#FFF">3. Thu tiền &amp; Tách tiền thừa vào ADVANCE</text>

    <line x1="73" y1="392" x2="653" y2="392" stroke="#1B365D" stroke-width="1.8" marker-end="url(#a1)"/>
    <text x="363" y="386" text-anchor="middle" font-size="11.3" font-weight="700">POST /invoices/{id}/payments (code, amount, cashAccountId)</text>

    <line x1="658" y1="420" x2="834" y2="420" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="746" y="414" text-anchor="middle" font-size="10.8" font-weight="600">1× cash_transactions (IN)</text>

    <line x1="658" y1="448" x2="834" y2="448" stroke="#1B365D" stroke-width="1.7" marker-end="url(#a1)"/>
    <text x="746" y="442" text-anchor="middle" font-size="10.8" font-weight="600">PAYMENT ≤ nợ; dư → ADVANCE</text>

    <line x1="658" y1="474" x2="78" y2="474" stroke="#334155" stroke-width="1.6" stroke-dasharray="4 3" marker-end="url(#a2)"/>
    <text x="368" y="468" text-anchor="middle" font-size="11.2" font-weight="700" fill="#0F766E">Cập nhật nợ qua v_document_balances (bấm gửi lặp cùng code không thu 2 lần)</text>
  </svg>
</div>
</body></html>`;

// 6. Sequence 2: Checkout -> Handover Comparison -> Compensation -> Settlement -> Refund
const DIAG_SEQ_CHECKOUT = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">LUỒNG TRẢ PHÒNG — ĐỐI CHIẾU ĐỒ ĐẠC — BỒI THƯỜNG — HOÀN CỌC</div>
    <div class="subtitle">Ví dụ phòng P201: Cọc giữ 3.800.000 ₫ − Bồi thường hỏng điều hòa 350.000 ₫ = Hoàn 3.450.000 ₫</div>
  </div>
  <svg viewBox="0 0 912 485" width="912" height="485" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="b1" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#1B365D"/>
      </marker>
      <marker id="b2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#334155" stroke-width="1.8"/>
      </marker>
    </defs>

    <g font-size="12" font-weight="700" text-anchor="middle">
      <rect x="12" y="8" width="122" height="36" rx="4" fill="#EFF6FF" stroke="#1B365D" stroke-width="1.8"/>
      <text x="73" y="30" fill="#1B365D">Quản lý (UI)</text>

      <rect x="198" y="8" width="136" height="36" rx="4" fill="#0F4C81"/>
      <text x="266" y="30" fill="#FFF">Api49 &amp; Files49</text>

      <rect x="394" y="8" width="136" height="36" rx="4" fill="#1B365D"/>
      <text x="462" y="30" fill="#FFF">Settlements49</text>

      <rect x="590" y="8" width="136" height="36" rx="4" fill="#3730A3"/>
      <text x="658" y="30" fill="#FFF">Finance49</text>

      <rect x="776" y="8" width="126" height="36" rx="4" fill="#1E293B"/>
      <text x="839" y="30" fill="#FFF">PostgreSQL</text>
    </g>

    <line x1="73" y1="44" x2="73" y2="476" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="266" y1="44" x2="266" y2="476" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="462" y1="44" x2="462" y2="476" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="658" y1="44" x2="658" y2="476" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>
    <line x1="839" y1="44" x2="839" y2="476" stroke="#94A3B8" stroke-width="1.5" stroke-dasharray="5 4"/>

    <!-- Step 1 -->
    <rect x="8" y="56" width="896" height="156" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="56" width="285" height="20" fill="#0F4C81"/>
    <text x="16" y="70" font-size="11" font-weight="700" fill="#FFF">1. Biên bản trả phòng &amp; Bồi thường hỏng đồ (V14)</text>

    <line x1="73" y1="96" x2="261" y2="96" stroke="#1B365D" stroke-width="1.8" marker-end="url(#b1)"/>
    <text x="168" y="90" text-anchor="middle" font-size="11.2" font-weight="700">POST /handovers (MOVE_OUT)</text>

    <line x1="266" y1="118" x2="834" y2="118" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="550" y="112" text-anchor="middle" font-size="11.2" font-weight="600">Nạp danh sách đồ từ biên bản nhận MOVE_IN (gắn FK original_item_id)</text>

    <line x1="73" y1="146" x2="261" y2="146" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="168" y="140" text-anchor="middle" font-size="11.1" font-weight="600">Ghi hiện trạng + ảnh hỏng + confirm</text>

    <line x1="73" y1="176" x2="261" y2="176" stroke="#1B365D" stroke-width="1.8" marker-end="url(#b1)"/>
    <text x="168" y="170" text-anchor="middle" font-size="11.1" font-weight="700">POST /damage-compensation (350k)</text>

    <line x1="266" y1="198" x2="834" y2="198" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="550" y="192" text-anchor="middle" font-size="11.2" font-weight="600">Phát hành chứng từ COMPENSATION (dòng DAMAGE gắn handover_item_id)</text>

    <!-- Step 2 -->
    <rect x="8" y="224" width="896" height="140" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="224" width="265" height="20" fill="#1B365D"/>
    <text x="16" y="238" font-size="11" font-weight="700" fill="#FFF">2. Trả phòng vật lý &amp; Duyệt quyết toán</text>

    <line x1="73" y1="264" x2="457" y2="264" stroke="#1B365D" stroke-width="1.8" marker-end="url(#b1)"/>
    <text x="265" y="258" text-anchor="middle" font-size="11.3" font-weight="700">POST /contracts/{id}/checkout</text>

    <line x1="462" y1="288" x2="834" y2="288" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="648" y="282" text-anchor="middle" font-size="11.2" font-weight="600">Đóng room_possessions, đổi phòng → TURNOVER, tạo checkout DRAFT</text>

    <line x1="73" y1="318" x2="457" y2="318" stroke="#1B365D" stroke-width="1.8" marker-end="url(#b1)"/>
    <text x="265" y="312" text-anchor="middle" font-size="11.3" font-weight="700">POST /contracts/{id}/checkout/approve</text>

    <line x1="462" y1="346" x2="834" y2="346" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="648" y="340" text-anchor="middle" font-size="11.2" font-weight="600">Cấn nợ 350.000 ₫ từ quỹ DEPOSIT + lưu snapshot quyết toán</text>

    <!-- Step 3 -->
    <rect x="8" y="376" width="896" height="98" rx="4" fill="#F8FAFC" fill-opacity="0.6" stroke="#94A3B8"/>
    <rect x="8" y="376" width="255" height="20" fill="#3730A3"/>
    <text x="16" y="390" font-size="11" font-weight="700" fill="#FFF">3. Chi hoàn tiền cọc thực tế (OUT)</text>

    <line x1="73" y1="416" x2="653" y2="416" stroke="#1B365D" stroke-width="1.8" marker-end="url(#b1)"/>
    <text x="363" y="410" text-anchor="middle" font-size="11.3" font-weight="700">POST /fund-accounts/{id}/refund (3.450.000 ₫, paid=true)</text>

    <line x1="658" y1="440" x2="834" y2="440" stroke="#1B365D" stroke-width="1.7" marker-end="url(#b1)"/>
    <text x="746" y="434" text-anchor="middle" font-size="10.8" font-weight="600">cash_transactions OUT + REFUND</text>

    <line x1="658" y1="462" x2="78" y2="462" stroke="#334155" stroke-width="1.6" stroke-dasharray="4 3" marker-end="url(#b2)"/>
    <text x="368" y="456" text-anchor="middle" font-size="11.2" font-weight="700" fill="#0F766E">Quỹ cọc về 0 ₫ · Cột Hoàn quỹ trên báo cáo ghi nhận đúng 3.450.000 ₫</text>
  </svg>
</div>
</body></html>`;

// Helper for crisp compact horizontal Actor Use Case diagrams (960px × ~250px -> 1920x500px sharp)
function buildActorUseCaseHtml(title, subtitle, actorName, actorCode, accent, fill, items) {
  const cols = 2;
  const rows = Math.ceil(items.length / cols);
  const svgH = Math.max(200, rows * 44 + 46);
  const actorY = Math.round(svgH / 2) - 18;
  let ellipses = '';
  let lines = '';
  items.forEach((label, idx) => {
    const col = idx % cols;
    const row = Math.floor(idx / cols);
    const cx = col === 0 ? 356 : 716;
    const cy = 54 + row * 44;
    const rx = 156;
    const ry = 16;
    ellipses += `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fill}" stroke="${accent}" stroke-width="1.7"/>`;
    ellipses += `<text x="${cx}" y="${cy + 4.5}" text-anchor="middle" font-size="12.2" font-weight="700" fill="#0F172A">${label}</text>`;
    if (col === 0) {
      lines += `<line x1="112" y1="${actorY + 8}" x2="${cx - rx}" y2="${cy}" stroke="${accent}" stroke-width="1.5"/>`;
    } else {
      lines += `<path d="M 112 ${actorY + 8} L 154 ${actorY + 8} L 154 ${svgH - 12} L 538 ${svgH - 12} L 538 ${cy} L ${cx - rx} ${cy}" fill="none" stroke="${accent}" stroke-width="1.35" stroke-opacity="0.82"/>`;
    }
  });
  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>${COMMON_CSS}</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">${title}</div>
    <div class="subtitle">${subtitle}</div>
  </div>
  <svg viewBox="0 0 912 ${svgH}" width="912" height="${svgH}" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <rect x="174" y="8" width="730" height="${svgH - 16}" rx="7" fill="#F8FAFC" stroke="${accent}" stroke-width="2"/>
    <text x="539" y="27" text-anchor="middle" font-size="13" font-weight="800" fill="${accent}">CHỨC NĂNG CỦA ${actorName.toUpperCase()} (${actorCode})</text>
    <g transform="translate(74, ${actorY})">
      <circle cx="0" cy="-22" r="13" fill="${fill}" stroke="${accent}" stroke-width="2.4"/>
      <line x1="0" y1="-9" x2="0" y2="22" stroke="${accent}" stroke-width="2.4"/>
      <line x1="-20" y1="2" x2="20" y2="2" stroke="${accent}" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="-16" y2="44" stroke="${accent}" stroke-width="2.4"/>
      <line x1="0" y1="22" x2="16" y2="44" stroke="${accent}" stroke-width="2.4"/>
      <text x="0" y="62" text-anchor="middle" font-size="13" font-weight="800" fill="${accent}">${actorName}</text>
      <text x="0" y="78" text-anchor="middle" font-size="11.5" font-weight="700" fill="#334155">(${actorCode})</text>
    </g>
    ${lines}
    ${ellipses}
  </svg>
</div></body></html>`;
}

const DIAG_UC_ADMIN = buildActorUseCaseHtml(
  'BIỂU ĐỒ USE CASE MỨC ĐỈNH — CHỦ NHÀ (ADMIN)',
  'Toàn quyền quản trị khu phòng, cấu hình, hợp đồng, tài chính và báo cáo',
  'Chủ nhà', 'ADMIN', '#1B365D', '#EFF6FF',
  [
    'Quản lý tài khoản & đặt lại mật khẩu',
    'Phân công nhân viên & cấp hạn mức',
    'Quản lý khu nhà, phòng & đồ đạc',
    'Lập, ký, gia hạn hợp đồng & xuất PDF',
    'Cấu hình giá dịch vụ & ngưỡng cảnh báo',
    'Ghi chỉ số công tơ & duyệt cảnh báo',
    'Lập, phát hành & điều chỉnh hóa đơn',
    'Ghi thu tiền, cấn cọc & chi hoàn cọc',
    'Đối chiếu bàn giao & quyết toán trả phòng',
    'Theo dõi sự cố, báo cáo & công nợ',
  ]
);

const DIAG_UC_STAFF = buildActorUseCaseHtml(
  'BIỂU ĐỒ USE CASE MỨC ĐỈNH — NHÂN VIÊN (STAFF)',
  'Thao tác trong phạm vi khu/phòng được phân công và hạn mức ủy quyền',
  'Nhân viên', 'STAFF', '#1E40AF', '#EFF6FF',
  [
    'Xem khu nhà, phòng & đồ đạc được giao',
    'Quản lý hồ sơ người thuê & cư trú',
    'Lập hợp đồng & biên bản bàn giao phòng',
    'Ghi chỉ số điện nước & kiểm tra cảnh báo',
    'Lập và phát hành hóa đơn trong phạm vi',
    'Ghi nhận thu tiền vào quỹ tiền mặt/NH',
    'Tiếp nhận, xử lý sự cố & ghi chi phí',
    'Xem báo cáo & công nợ khu được giao',
    'Hoàn tiền / điều chỉnh (khi được ủy quyền)',
  ]
);

const DIAG_UC_TENANT = buildActorUseCaseHtml(
  'BIỂU ĐỒ USE CASE MỨC ĐỈNH — NGƯỜI THUÊ (TENANT)',
  'Chỉ truy cập hợp đồng, hóa đơn, tài sản và sự cố gắn với nơi đang cư trú',
  'Người thuê', 'TENANT', '#166534', '#F0FDF4',
  [
    'Đăng nhập JWT & đổi mật khẩu',
    'Xem hợp đồng, phụ lục & đồ đạc phòng',
    'Xem hóa đơn đã phát hành (ISSUED)',
    'Theo dõi lịch sử thanh toán & số dư quỹ',
    'Xem và tải ảnh/tệp chứng từ được phép',
    'Gửi báo hỏng sự cố kèm ảnh tại phòng ở',
    'Theo dõi tiến độ xử lý sự cố của mình',
    'Đọc thông báo từ chủ nhà / quản lý',
  ]
);

// 7. MVC Class & Component Diagram (crisp 960px -> 1920px exact, zero cramped labels!)
const DIAG_MVC_CLASSES = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
.mvc-stack { display: flex; flex-direction: column; gap: 10px; }
.tier {
  border: 1.8px solid #64748B;
  border-radius: 6px;
  overflow: hidden;
  background: #F8FAFC;
}
.tier-hd {
  background: #1B365D;
  color: #FFF;
  padding: 5px 12px;
  font-size: 12.6px;
  font-weight: 800;
  display: flex;
  justify-content: space-between;
}
.tier-hd.ctrl { background: #1E40AF; }
.tier-hd.mdl { background: #0F5257; }
.tier-hd.db { background: #1E293B; }
.tier-bd { padding: 9px 10px; display: grid; gap: 8px; }
.c3 { grid-template-columns: repeat(3, 1fr); }
.c4 { grid-template-columns: repeat(4, 1fr); }
.c2 { grid-template-columns: 1.35fr 1fr; }
.cls {
  background: #FFFFFF;
  border: 1.5px solid #64748B;
  border-radius: 4px;
  overflow: hidden;
}
.cls-name {
  background: #EFF6FF;
  border-bottom: 1.3px solid #94A3B8;
  padding: 4px 8px;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 11.8px;
  font-weight: 700;
  color: #1B365D;
  display: flex;
  align-items: center;
  gap: 5px;
}
.cls-name.teal { background: #F0FDFA; color: #0F5257; }
.cls-name.slate { background: #F1F5F9; color: #0F172A; }
.badge {
  display: inline-block;
  width: 16px; height: 16px; line-height: 15px; text-align: center;
  border-radius: 50%; background: #1B365D; color: #FFF;
  font-size: 10px; font-weight: 800; font-family: 'Segoe UI', Arial, sans-serif;
}
.badge.t { background: #0F766E; }
.badge.d { background: #334155; }
.cls-body { padding: 5px 8px; font-family: Consolas, 'Courier New', monospace; font-size: 10.9px; color: #0F172A; line-height: 1.42; }
.cls-note { font-family: 'Segoe UI', Arial, sans-serif; font-size: 10.6px; color: #475569; margin-top: 2px; }
.conn {
  display: flex; justify-content: center; align-items: center; gap: 10px;
  margin: -3px 0; font-size: 11.2px; font-weight: 700; color: #1B365D;
}
.pill { background: #EFF6FF; border: 1.4px solid #60A5FA; border-radius: 10px; padding: 1.5px 11px; }
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">BIỂU ĐỒ LỚP VÀ THÀNH PHẦN THEO KIẾN TRÚC MVC (PHIÊN BẢN V14)</div>
    <div class="subtitle">Đối chiếu trực tiếp mã nguồn frontend/src và package com.hostel.model49</div>
  </div>
  <div class="mvc-stack">
    <div class="tier">
      <div class="tier-hd"><span>VIEW — GIAO DIỆN REACT 19 SPA (frontend/src)</span><span>Chạy trên trình duyệt</span></div>
      <div class="tier-bd c3">
        <div class="cls">
          <div class="cls-name"><span class="badge">V</span>main.jsx</div>
          <div class="cls-body">App() · Dashboard()<br>Detail() · Login()<div class="cls-note">Điều hướng 13 màn hình &amp; trạng thái phiên</div></div>
        </div>
        <div class="cls">
          <div class="cls-name"><span class="badge">V</span>Model49Panels.jsx</div>
          <div class="cls-body">ReportsPanel() · DebtsPanel()<br>Contract49Panel() · RoomAssetsPanel()<div class="cls-note">Đồ đạc, bàn giao nhận–trả, báo cáo &amp; công nợ</div></div>
        </div>
        <div class="cls">
          <div class="cls-name"><span class="badge">V</span>api.js</div>
          <div class="cls-body">+api(path, method, data)<br>+saveSession() · +upload()<div class="cls-note">Gắn Bearer JWT, tự refresh 401 một lần</div></div>
        </div>
      </div>
    </div>

    <div class="conn"><span>▼</span><span class="pill">REST JSON (/api/**) · Bearer JWT · Multipart · PDF Stream</span><span>▲</span></div>

    <div class="tier">
      <div class="tier-hd ctrl"><span>CONTROLLER &amp; BẢO MẬT — SPRING WEB MVC + SECURITY</span><span>Kiểm tra quyền &amp; điều phối</span></div>
      <div class="tier-bd c4">
        <div class="cls">
          <div class="cls-name"><span class="badge">C</span>AuthController</div>
          <div class="cls-body">+login() · +refresh()<br>+logout() · +change()<br>+me() · +health()</div>
        </div>
        <div class="cls">
          <div class="cls-name"><span class="badge">C</span>Api49</div>
          <div class="cls-body">+createHandover() · +pay()<br>+checkout() · +contractPdf()<br>+damageCompensation()</div>
        </div>
        <div class="cls">
          <div class="cls-name"><span class="badge">C</span>Reports49</div>
          <div class="cls-body">+dashboard() · +tenantDebts()<br>+revenue() · +quarterly()<br>+yearly() · +revenuePdf()</div>
        </div>
        <div class="cls">
          <div class="cls-name"><span class="badge">S</span>Access49 &amp; ApiErrors</div>
          <div class="cls-body">+admin() · +manager() · +action()<br>+room() · +contract() · +document()<br>ApiErrors: conflict() · status()</div>
        </div>
      </div>
    </div>

    <div class="conn"><span>▼</span><span class="pill">Gọi dịch vụ @Transactional · Kiểm soát phạm vi khu/phòng &amp; hạn mức</span><span>▲</span></div>

    <div class="tier">
      <div class="tier-hd mdl"><span>MODEL NGHIỆP VỤ — CÁC LỚP SERVICE (com.hostel.model49)</span><span>Tách biệt theo phân hệ</span></div>
      <div class="tier-bd c4">
        <div class="cls">
          <div class="cls-name teal"><span class="badge t">M</span>Leases49 &amp; Catalog49</div>
          <div class="cls-body">+create() · +activate() · +renew()<br>+reside() · +possess() · +checkout()<br>Catalog49: +list() · +save()</div>
        </div>
        <div class="cls">
          <div class="cls-name teal"><span class="badge t">M</span>Meters49 &amp; Billing49</div>
          <div class="cls-body">+reading() · +segment() · +revise()<br>+replace() · Billing49: +monthly()<br>+pay() · +meterLine()</div>
        </div>
        <div class="cls">
          <div class="cls-name teal"><span class="badge t">M</span>Finance49 &amp; Settlements49</div>
          <div class="cls-body">+issue() · +receive() · +entry()<br>+refund() · +createDocument()<br>Settlements49: +statement()·+approve()</div>
        </div>
        <div class="cls">
          <div class="cls-name teal"><span class="badge t">M</span>Operations49, Files49, PDF</div>
          <div class="cls-body">+create() · +progress() · +cost()<br>Files49: +attach() · +download()<br>ContractPdf: +render()</div>
        </div>
      </div>
    </div>

    <div class="conn"><span>▼</span><span class="pill">PreparedStatement · SELECT FOR UPDATE · pg_advisory_xact_lock</span><span>▲</span></div>

    <div class="tier">
      <div class="tier-hd db"><span>TẦNG TRUY CẬP DỮ LIỆU &amp; LƯU TRỮ (PERSISTENCE)</span><span>PostgreSQL 18.6 / Flyway V1–V14</span></div>
      <div class="tier-bd c2">
        <div class="cls">
          <div class="cls-name slate"><span class="badge d">D</span>Database (bọc JdbcTemplate)</div>
          <div class="cls-body">+rows(sql, args) · +one(sql, args) · +insert(...) · +id() · +money()<div class="cls-note">Tuyến truy vấn chính cho toàn bộ service nghiệp vụ &amp; 10 View tài chính</div></div>
        </div>
        <div class="cls">
          <div class="cls-name slate"><span class="badge d">E</span>49 JPA Entities (model49.entity)</div>
          <div class="cls-body">com.hostel.model49.entity.* (49 lớp Entity)<div class="cls-note">Dùng Hibernate ddl-auto=validate để đối soát khớp 100% lược đồ DB</div></div>
        </div>
      </div>
    </div>
  </div>
  <div class="footer-note">
    <span>Các lớp nghiệp vụ đều đi qua Access49 để kiểm tra phạm vi và Events49 để ghi audit_events.</span>
    <span>MVC + Service Layer</span>
  </div>
</div>
</body></html>`;

// 8. Deployment Diagram (crisp 960px SVG -> 1920px exact, ZERO overlapping arrows!)
const DIAG_DEPLOYMENT = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><style>
${COMMON_CSS}
</style></head><body>
<div class="canvas">
  <div class="header">
    <div class="title">BIỂU ĐỒ TRIỂN KHAI HỆ THỐNG (DEPLOYMENT — THEO COMPOSE.YML)</div>
    <div class="subtitle">Đóng gói Docker Compose · Tương thích PostgreSQL 16 (compose.yml) &amp; 18.6 (môi trường kiểm chứng)</div>
  </div>
  <svg viewBox="0 0 912 470" width="912" height="470" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial, sans-serif">
    <defs>
      <marker id="depArr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
        <path d="M 0 1 L 10 5 L 0 9 z" fill="#1B365D"/>
      </marker>
    </defs>

    <!-- Client Node -->
    <g transform="translate(276, 10)">
      <polygon points="0,12 12,0 360,0 348,12" fill="#E2E8F0" stroke="#1B365D" stroke-width="1.8"/>
      <polygon points="348,12 360,0 360,66 348,78" fill="#CBD5E1" stroke="#1B365D" stroke-width="1.8"/>
      <rect x="0" y="12" width="348" height="66" fill="#FFFFFF" stroke="#1B365D" stroke-width="1.8"/>
      <text x="174" y="32" text-anchor="middle" font-size="13" font-weight="800" fill="#1B365D">«device» Máy người dùng (Desktop / Mobile)</text>
      <rect x="24" y="40" width="300" height="28" rx="4" fill="#EFF6FF" stroke="#60A5FA" stroke-width="1.3"/>
      <text x="174" y="58" text-anchor="middle" font-size="11.6" font-weight="600" fill="#0F172A">Trình duyệt Web · React 19 SPA + sessionStorage</text>
    </g>

    <!-- HTTP Arrow -->
    <line x1="450" y1="88" x2="450" y2="128" stroke="#1B365D" stroke-width="2" marker-end="url(#depArr)"/>
    <rect x="462" y="96" width="210" height="22" rx="4" fill="#EFF6FF" stroke="#93C5FD"/>
    <text x="567" y="111" text-anchor="middle" font-size="11.5" font-weight="700" fill="#1B365D">HTTP/HTTPS cổng 8080 (cùng origin)</text>

    <!-- Docker Host Node -->
    <g transform="translate(18, 128)">
      <polygon points="0,14 14,0 876,0 862,14" fill="#E2E8F0" stroke="#1E293B" stroke-width="2"/>
      <polygon points="862,14 876,0 876,318 862,332" fill="#CBD5E1" stroke="#1E293B" stroke-width="2"/>
      <rect x="0" y="14" width="862" height="318" fill="#F8FAFC" stroke="#1E293B" stroke-width="2"/>
      <text x="16" y="36" font-size="13.2" font-weight="800" fill="#1E293B">«executionEnvironment» Máy chủ Docker (compose.yml — mạng nội bộ Docker)</text>

      <!-- Container app -->
      <g transform="translate(260, 50)">
        <polygon points="0,10 10,0 352,0 342,10" fill="#DBEAFE" stroke="#1E40AF" stroke-width="1.8"/>
        <polygon points="342,10 352,0 352,86 342,96" fill="#BFDBFE" stroke="#1E40AF" stroke-width="1.8"/>
        <rect x="0" y="10" width="342" height="86" fill="#FFFFFF" stroke="#1E40AF" stroke-width="1.8"/>
        <text x="171" y="29" text-anchor="middle" font-size="12.6" font-weight="800" fill="#1E40AF">«container» app (Java 21 JRE)</text>
        <rect x="16" y="38" width="310" height="46" rx="4" fill="#EFF6FF" stroke="#60A5FA" stroke-width="1.3"/>
        <text x="171" y="56" text-anchor="middle" font-size="11.6" font-weight="700" fill="#0F172A">«artifact» Spring Boot 4.1.1 JAR (:8080)</text>
        <text x="171" y="74" text-anchor="middle" font-size="11" fill="#334155">Static React SPA + REST API + Flyway V1–V14</text>
      </g>

      <!-- Volume hostel-images (Left) -->
      <g transform="translate(22, 166)">
        <rect x="0" y="0" width="208" height="86" rx="5" fill="#FFFFFF" stroke="#0F766E" stroke-width="1.8"/>
        <rect x="0" y="0" width="208" height="24" rx="4" fill="#0F766E"/>
        <text x="104" y="16" text-anchor="middle" font-size="11.6" font-weight="700" fill="#FFF">«volume» hostel-images</text>
        <text x="104" y="46" text-anchor="middle" font-size="11.3" font-weight="600" fill="#0F172A">Thư mục /app/uploads</text>
        <text x="104" y="64" text-anchor="middle" font-size="10.8" fill="#334155">Ảnh hiện trạng, bàn giao, sự cố</text>
        <text x="104" y="79" text-anchor="middle" font-size="10.5" fill="#0F766E">(Kiểm tra quyền qua Files49)</text>
      </g>

      <!-- Arrow app -> hostel-images -->
      <path d="M 260 105 L 126 105 L 126 162" fill="none" stroke="#0F766E" stroke-width="1.8" marker-end="url(#depArr)"/>
      <text x="192" y="99" text-anchor="middle" font-size="10.8" font-weight="700" fill="#0F766E">Đọc/ghi có kiểm tra</text>

      <!-- Container db (Center bottom) -->
      <g transform="translate(260, 180)">
        <polygon points="0,10 10,0 352,0 342,10" fill="#E0E7FF" stroke="#3730A3" stroke-width="1.8"/>
        <polygon points="342,10 352,0 352,112 342,122" fill="#C7D2FE" stroke="#3730A3" stroke-width="1.8"/>
        <rect x="0" y="10" width="342" height="112" fill="#FFFFFF" stroke="#3730A3" stroke-width="1.8"/>
        <text x="171" y="29" text-anchor="middle" font-size="12.5" font-weight="800" fill="#3730A3">«container» db (postgres:16-alpine)</text>
        <text x="171" y="46" text-anchor="middle" font-size="10.6" font-style="italic" fill="#475569">Tương thích PostgreSQL 16 &amp; 18.6 (cổng nội bộ 5432)</text>
        <rect x="16" y="55" width="310" height="54" rx="4" fill="#EEF2FF" stroke="#818CF8" stroke-width="1.3"/>
        <text x="171" y="74" text-anchor="middle" font-size="11.5" font-weight="700" fill="#0F172A">«database» Lược đồ V14 (49 bảng · 461 cột)</text>
        <text x="171" y="92" text-anchor="middle" font-size="10.8" fill="#334155">152 FK · 133 CHECK · 7 GiST EXCLUDE · 10 View</text>
      </g>

      <!-- Arrow app -> db -->
      <line x1="431" y1="146" x2="431" y2="177" stroke="#1B365D" stroke-width="2" marker-end="url(#depArr)"/>
      <text x="518" y="166" font-size="11" font-weight="700" fill="#1B365D">JDBC nội bộ db:5432</text>

      <!-- Volume hostel-data (Right bottom) -->
      <g transform="translate(636, 200)">
        <rect x="0" y="0" width="206" height="86" rx="5" fill="#FFFFFF" stroke="#334155" stroke-width="1.8"/>
        <rect x="0" y="0" width="206" height="24" rx="4" fill="#334155"/>
        <text x="103" y="16" text-anchor="middle" font-size="11.6" font-weight="700" fill="#FFF">«volume» hostel-data</text>
        <text x="103" y="46" text-anchor="middle" font-size="11.1" font-weight="600" fill="#0F172A">/var/lib/postgresql/data</text>
        <text x="103" y="64" text-anchor="middle" font-size="10.8" fill="#334155">Dữ liệu bền vững PostgreSQL</text>
        <text x="103" y="79" text-anchor="middle" font-size="10.5" fill="#475569">Tách khỏi vòng đời container</text>
      </g>

      <!-- Arrow db -> hostel-data -->
      <line x1="612" y1="244" x2="633" y2="244" stroke="#334155" stroke-width="1.9" marker-end="url(#depArr)"/>

      <!-- Notes box (Right top) -->
      <g transform="translate(636, 56)">
        <rect x="0" y="0" width="206" height="106" rx="4" fill="#FFFBEB" stroke="#D97706" stroke-width="1.5"/>
        <text x="10" y="20" font-size="11.4" font-weight="800" fill="#92400E">Cấu hình &amp; Lưu ý vận hành</text>
        <text x="10" y="38" font-size="10.8" fill="#78350F">• Bí mật nạp qua biến môi trường</text>
        <text x="10" y="54" font-size="10.8" fill="#78350F">  (JWT_SECRET, DB_PASSWORD)</text>
        <text x="10" y="70" font-size="10.8" fill="#78350F">• Flyway tự nâng cấp V1→V14</text>
        <text x="10" y="86" font-size="10.8" fill="#78350F">• Phiên &amp; đếm sai lưu trên RAM</text>
        <text x="10" y="100" font-size="10.6" font-style="italic" fill="#92400E">  (Cần HTTPS + sao lưu khi mở public)</text>
      </g>
      <line x1="612" y1="98" x2="636" y2="98" stroke="#D97706" stroke-width="1.4" stroke-dasharray="4 3"/>
    </g>
  </svg>
</div>
</body></html>`;

const DIAGRAMS = [
  ['diag-01-architecture.png', DIAG_ARCH],
  ['diag-02-usecase.png', DIAG_USECASE],
  ['diag-03-db-8modules.png', DIAG_DB_MODULES],
  ['diag-04-erd-core-finance.png', DIAG_ERD_CORE],
  ['diag-05-seq-billing-alert.png', DIAG_SEQ_BILLING],
  ['diag-06-seq-checkout-handover.png', DIAG_SEQ_CHECKOUT],
  ['diag-uc-admin.png', DIAG_UC_ADMIN],
  ['diag-uc-staff.png', DIAG_UC_STAFF],
  ['diag-uc-tenant.png', DIAG_UC_TENANT],
  ['diag-mvc-classes.png', DIAG_MVC_CLASSES],
  ['diag-deployment-v14.png', DIAG_DEPLOYMENT],
];

async function run() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const page = await browser.newPage({
      viewport: { width: 1000, height: 800 },
      deviceScaleFactor: 2, // 960 * 2 = 1920px exact (< 2048px Google Docs CDN cap!)
    });
    for (const [filename, html] of DIAGRAMS) {
      await page.setContent(html, { waitUntil: 'load' });
      await page.evaluate(() => document.fonts.ready);
      const box = page.locator('.canvas');
      await box.screenshot({ path: path.join(OUT_DIR, filename) });
      console.log('Generated sharp', filename);
    }
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
