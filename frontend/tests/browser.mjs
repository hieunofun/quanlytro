import { chromium } from "playwright";
import { mkdir } from "node:fs/promises";
import path from "node:path";
import assert from "node:assert/strict";
const base = process.env.BASE_URL || "http://localhost:8080";
const output = path.resolve(process.env.SCREENSHOT_DIR || "../.runtime/mvp-screenshots");
await mkdir(output, { recursive: true });
const browser = await chromium.launch(
  process.env.CHROME_PATH
    ? { executablePath: process.env.CHROME_PATH, headless: true }
    : { channel: process.env.BROWSER_CHANNEL || "chrome", headless: true },
);
const errors = [];
try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
  });
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto(base);
  await page.getByRole("heading", { name: "Đăng nhập", exact: true }).waitFor();
  await page.screenshot({
    path: path.join(output, "login-desktop.png"),
    fullPage: true,
  });
  async function login(username, password) {
    await page.getByLabel("Tên đăng nhập").fill(username);
    await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await page.locator(".sidebar").waitFor();
  }
  await login("admin", process.env.ADMIN_PASSWORD || "DemoAdmin#2026");
  await page.locator("input[type=month]").fill("2026-09");
  await page.getByText("Đã lập hóa đơn", { exact: true }).waitFor();
  await page.getByText("Công nợ kỳ 2026-09", { exact: true }).waitFor();
  await page.screenshot({
    path: path.join(output, "dashboard-desktop.png"),
    fullPage: true,
  });
  for (const label of [
    "Khu nhà",
    "Phòng",
    "Người thuê",
    "Hợp đồng",
    "Điện nước",
    "Hóa đơn",
    "Sự cố",
    "Thiết lập",
  ]) {
    await page
      .locator("nav")
      .getByRole("button", { name: label, exact: true })
      .click();
    await page
      .getByRole("heading", { name: label, exact: true, level: 1 })
      .waitFor();
    await page
      .getByText("Đang tải dữ liệu…", { exact: true })
      .waitFor({ state: "hidden" });
    assert.equal(
      await page.locator(".banner.error").count(),
      0,
      `Error on ${label}`,
    );
  }
  await page
    .locator("nav")
    .getByRole("button", { name: "Phòng", exact: true })
    .click();
  await page.getByRole("button", { name: "Thêm mới", exact: true }).click();
  await page.getByRole("dialog").waitFor();
  assert.equal(await page.getByLabel("Khu nhà", { exact: false }).count(), 1);
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: "Hóa đơn", exact: true })
    .click();
  const invoice = page.getByRole("row").filter({ hasText: "2026-09" }).first();
  await invoice.getByRole("button", { name: "Xem", exact: true }).click();
  await page
    .getByRole("heading", { name: "Chi tiết cách tính", exact: true })
    .waitFor();
  assert.match(await page.locator("main").innerText(), /160[.\s]000/);
  await page.screenshot({
    path: path.join(output, "invoice-desktop.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await page.getByRole("heading", { name: "Đăng nhập", exact: true }).waitFor();
  await login("tenant_demo", "DemoTenant#2026");
  await page.reload();
  await page
    .getByRole("heading", { name: "Hóa đơn", exact: true, level: 1 })
    .waitFor();
  assert.equal(
    await page
      .getByRole("button", { name: "Tạo hóa đơn", exact: true })
      .count(),
    0,
  );
  await page
    .getByRole("row")
    .filter({ hasText: "2026-09" })
    .first()
    .getByRole("button", { name: "Xem", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "Chi tiết cách tính", exact: true })
    .waitFor();
  assert.equal(
    await page.getByRole("button", { name: "Thu tiền", exact: true }).count(),
    0,
  );
  await page.setViewportSize({ width: 375, height: 812 });
  assert.ok(
    await page
      .locator(".invoice-mobile-lines")
      .getByText(/155.*147/)
      .isVisible(),
    "Formula must be visible on mobile",
  );
  await page.screenshot({
    path: path.join(output, "tenant-mobile.png"),
    fullPage: true,
  });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
    "Horizontal page overflow at 375px",
  );
  await page.getByRole("button", { name: "Mở menu", exact: true }).click();
  await page
    .locator("nav")
    .getByRole("button", { name: "Sự cố", exact: true })
    .click();
  await page.getByRole("button", { name: "Báo sự cố", exact: true }).click();
  await page.getByRole("dialog").waitFor();
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
    "Dialog overflow at 375px",
  );
  await page.screenshot({
    path: path.join(output, "repair-mobile.png"),
    fullPage: true,
  });
  assert.deepEqual(errors, [], "Browser console errors");
  console.log(
    JSON.stringify(
      {
        status: "PASS",
        pages: 8,
        roles: ["ADMIN", "TENANT"],
        viewports: [1440, 375],
        screenshots: 5,
        errors,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
