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

async function settle(page, ms = 500) {
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
  await settle(page, 600);
}

async function resetScrolls(page) {
  await page.evaluate(() => {
    window.scrollTo(0, 0);
    document.querySelectorAll('.table-wrap, [style*="overflow"]').forEach((el) => {
      el.scrollLeft = 0;
    });
  });
}

async function run() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const ctx = await browser.newContext({
      viewport: { width: 1440, height: 920 },
      deviceScaleFactor: 2,
    });
    const page = await ctx.newPage();

    // Login
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await page.getByLabel('Tên đăng nhập').fill(ADMIN_USER);
    await page.getByLabel('Mật khẩu', { exact: true }).fill(ADMIN_PASS);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await page.locator('.sidebar').waitFor({ timeout: 15000 });
    await settle(page, 700);

    // 1. Dashboard Quarter 2025-Q4 (real non-zero Q4 data!)
    const qBtn = page.getByRole('button', { name: /Theo quý/i });
    if (await qBtn.count()) {
      await qBtn.first().click();
      await settle(page, 500);
      const sel = page.locator('select').first();
      if (await sel.count()) {
        await sel.selectOption('2025-Q4').catch(async () => {
          const opts = await sel.locator('option').evaluateAll((els) => els.map((e) => ({ v: e.value, t: e.textContent })));
          console.log('Quarter options:', opts);
          const q4 = opts.find((o) => o.v === '2025-Q4' || o.t.includes('4 / 2025'));
          if (q4) await sel.selectOption(q4.v);
        });
      }
      await settle(page, 700);
      await page.screenshot({ path: path.join(OUT_DIR, 'ui-dashboard-quarter.png'), fullPage: false });
      console.log('Saved ui-dashboard-quarter.png');
    }

    // 2. Reports at wider viewport (1640px) so all columns incl. Tiền vào quỹ / Hoàn quỹ show cleanly
    await page.setViewportSize({ width: 1640, height: 980 });
    await nav(page, 'Báo cáo');
    await resetScrolls(page);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-reports.png'), fullPage: true });
    console.log('Saved ui-reports.png');

    // 3. Tenant Debts at 1520px, expand Lê Thị Bích (P102), scrollLeft=0
    await page.setViewportSize({ width: 1520, height: 920 });
    await nav(page, 'Công nợ khách');
    const bichRow = page.getByRole('row').filter({ hasText: 'P102' }).first();
    if (await bichRow.count()) {
      await bichRow.getByRole('button', { name: /Xem/i }).first().click();
      await settle(page, 600);
    }
    await resetScrolls(page);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-debts.png'), fullPage: true });
    console.log('Saved ui-debts.png');

    // 4. Invoices -> open MONTHLY 2026-01 invoice for P101 (4.087.000 đ, full formula + 3 payments)
    await page.setViewportSize({ width: 1400, height: 980 });
    await nav(page, 'Hóa đơn');
    const janRow = page.getByRole('row').filter({ hasText: '2026-01' }).first();
    if (await janRow.count()) {
      await janRow.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 700);
    }
    await resetScrolls(page);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-invoice-detail.png'), fullPage: true });
    console.log('Saved ui-invoice-detail.png');

    // 5. Rooms -> P101 Assets (full page showing all 4 assets)
    await page.setViewportSize({ width: 1380, height: 960 });
    await nav(page, 'Phòng');
    const p101Row = page.getByRole('row').filter({ hasText: 'P101' }).first();
    if (await p101Row.count()) {
      await p101Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 600);
    }
    await resetScrolls(page);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-room-assets.png'), fullPage: true });
    console.log('Saved ui-room-assets.png');

    // 6. Contract P101 -> wider & taller frame showing summary + full 2-version history + PDF buttons
    await page.setViewportSize({ width: 1480, height: 1120 });
    await nav(page, 'Hợp đồng');
    const c101Row = page.getByRole('row').filter({ hasText: 'P101' }).first();
    if (await c101Row.count()) {
      await c101Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 700);
    }
    await resetScrolls(page);
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-contract-p101.png'), fullPage: false });
    console.log('Saved ui-contract-p101.png');

    // 7. Contract P201 -> scroll directly to HO-OUT-3 handover comparison + settlement block
    await page.setViewportSize({ width: 1440, height: 1120 });
    await nav(page, 'Hợp đồng');
    const c201Row = page.getByRole('row').filter({ hasText: 'P201' }).first();
    if (await c201Row.count()) {
      await c201Row.getByRole('button', { name: 'Xem', exact: true }).click();
      await settle(page, 700);
    }
    const hoOut = page.getByText(/Trả phòng · HO-OUT/i).first();
    if (await hoOut.count()) {
      await hoOut.scrollIntoViewIfNeeded();
      await page.evaluate(() => window.scrollBy(0, -24));
      await page.waitForTimeout(300);
    }
    await page.screenshot({ path: path.join(OUT_DIR, 'ui-contract-p201-handover.png'), fullPage: false });
    console.log('Saved ui-contract-p201-handover.png');

    await ctx.close();
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
