import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('../frontend/node_modules/playwright');

const BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8093';
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASS = process.env.ADMIN_PASS || 'ReviewAdmin49#2026';
const OUT_DIR = path.resolve(process.cwd(), 'docs/report-assets');

fs.mkdirSync(OUT_DIR, { recursive: true });

async function settle(page, ms = 450) {
  await page.waitForLoadState('networkidle').catch(() => {});
  const loader = page.getByText('Đang tải dữ liệu…', { exact: true });
  if (await loader.count()) {
    await loader.first().waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
  }
  await page.waitForTimeout(ms);
}

async function nav(page, name) {
  await page.locator('nav').getByRole('button', { name, exact: true }).click();
  await page.getByRole('heading', { level: 1, name, exact: true }).waitFor({ timeout: 10000 });
  await settle(page, 500);
}

// Inject crisp print contrast CSS & hide left sidebar on detail screens so UI cards/tables are 45% larger and crystal clear!
async function setContentOnlyMode(page, hideSidebar = true) {
  await page.evaluate((hide) => {
    let st = document.getElementById('__print_boost');
    if (!st) {
      st = document.createElement('style');
      st.id = '__print_boost';
      document.head.appendChild(st);
    }
    st.textContent = hide
      ? `.sidebar, aside, .topbar, header.topbar, footer { display: none !important; }
         .app, .layout, .shell, .workspace { display: block !important; grid-template-columns: 1fr !important; padding-left: 0 !important; margin-left: 0 !important; }
         main, .main, .content, .page, [class*="content"], [class*="main"] { margin-left: 0 !important; width: 100% !important; max-width: 100% !important; padding: 14px 18px !important; }
         body { background: #FFFFFF !important; }`
      : `body { background: #F8FAFC !important; } footer { display: none !important; }`;
    window.scrollTo(0, 0);
    document.querySelectorAll('.table-wrap, [style*="overflow"]').forEach((el) => {
      el.scrollLeft = 0;
    });
  }, hideSidebar);
  await page.waitForTimeout(150);
}

async function run() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    // Use deviceScaleFactor: 2 on ~980-1000px width -> ~1960-2000px PNG (<= 2048px Google Docs limit -> ZERO blur!)
    const ctx = await browser.newContext({
      viewport: { width: 1420, height: 860 },
      deviceScaleFactor: 1.42, // 1420 * 1.42 = 2016px (<= 2048px Google Docs limit -> ZERO downscaling & ZERO table clipping!)
    });
    const page = await ctx.newPage();

    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await settle(page, 350);

    // Login
    await page.getByLabel('Tên đăng nhập').fill(ADMIN_USER);
    await page.getByLabel('Mật khẩu', { exact: true }).fill(ADMIN_PASS);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await page.locator('.sidebar').waitFor({ timeout: 15000 });
    await settle(page, 600);

    const token = await page.evaluate(() => JSON.parse(sessionStorage.getItem('session')).accessToken);
    const contracts = await (await fetch(`${BASE_URL}/api/contracts`, {
      headers: { Authorization: `Bearer ${token}` },
    })).json();
    const cP101 = contracts.find((c) => String(c.contract_code || c.code || '').includes('P101')) || contracts[0];
    const pdfRes = await fetch(`${BASE_URL}/api/contracts/${cP101.id}/pdf?version=2`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    fs.writeFileSync(path.join(OUT_DIR, '_tmp-contract-v2.pdf'), Buffer.from(await pdfRes.arrayBuffer()));

    // 1. Dashboard Month 2026-01 (KEEP sidebar here so 1 screenshot shows full navigation, at 1000x920 -> 2000x1840 <= 2048px!)
    await page.setViewportSize({ width: 1000, height: 930 });
    const monthInput = page.locator('input[type="month"]');
    if (await monthInput.count()) {
      await monthInput.first().fill('2026-01');
      await monthInput.first().dispatchEvent('change');
      await settle(page, 600);
    }
    await setContentOnlyMode(page, false);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-dashboard-month.png'), fullPage: false });
    console.log('Saved sharp ui-dashboard-month.png');

    // 2. Reports (content-only at 1440x920 -> ALL columns incl. Tiền vào quỹ / Hoàn quỹ fit with zero clipping!)
    await page.setViewportSize({ width: 1440, height: 920 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Báo cáo');
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-reports.png'), fullPage: true });
    console.log('Saved sharp ui-reports.png');

    // 3. Tenant Debts (content-only at 1380x820 -> expand P102)
    await page.setViewportSize({ width: 1380, height: 820 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Công nợ khách');
    const bichRow = page.getByRole('row').filter({ hasText: 'P102' }).first();
    if (await bichRow.count()) {
      await bichRow.getByRole('button', { name: /Xem/i }).first().click();
      await settle(page, 500);
    }
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-debts.png'), fullPage: true });
    console.log('Saved sharp ui-debts.png');

    // 4. Meter Readings & Alerts (content-only at 1280x740)
    await page.setViewportSize({ width: 1280, height: 740 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Điện nước');
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-meter-alerts.png'), fullPage: false });
    console.log('Saved sharp ui-meter-alerts.png');

    // 5. Invoice 2026-01 detail (content-only at 1380x880)
    await page.setViewportSize({ width: 1380, height: 880 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Hóa đơn');
    const janRow = page.getByRole('row').filter({ hasText: '2026-01' }).first();
    if (await janRow.count()) {
      await janRow.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 600);
    }
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-invoice-detail.png'), fullPage: true });
    console.log('Saved sharp ui-invoice-detail.png');

    // 6. Room P101 Assets (content-only at 1320x780)
    await page.setViewportSize({ width: 1320, height: 780 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Phòng');
    const p101Row = page.getByRole('row').filter({ hasText: 'P101' }).first();
    if (await p101Row.count()) {
      await p101Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 500);
    }
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-room-assets.png'), fullPage: true });
    console.log('Saved sharp ui-room-assets.png');

    // 7. Contract P101 detail (content-only at 1380x880)
    await page.setViewportSize({ width: 1380, height: 880 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Hợp đồng');
    const c101Row = page.getByRole('row').filter({ hasText: 'P101' }).first();
    if (await c101Row.count()) {
      await c101Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 600);
    }
    await setContentOnlyMode(page, true);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-contract-p101.png'), fullPage: false });
    console.log('Saved sharp ui-contract-p101.png');

    // 8. Contract P201 Handover comparison + Checkout settlement (content-only at 1380x900, scroll cleanly to HO-OUT-3 card)
    await page.setViewportSize({ width: 1380, height: 900 });
    await setContentOnlyMode(page, false);
    await nav(page, 'Hợp đồng');
    const c201Row = page.getByRole('row').filter({ hasText: 'P201' }).first();
    if (await c201Row.count()) {
      await c201Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 600);
    }
    await setContentOnlyMode(page, true);
    const hoHeading = page.getByRole('heading', { name: /Biên bản bàn giao/i }).first();
    if (await hoHeading.count()) {
      await hoHeading.scrollIntoViewIfNeeded();
      await page.waitForTimeout(200);
    }
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-contract-p201-handover.png'), fullPage: false });
    console.log('Saved sharp ui-contract-p201-handover.png');

    await ctx.close();
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
