import React, { useState, useEffect, useCallback, useRef } from "react";
import { createRoot } from "react-dom/client";
import {
  Building2,
  LayoutDashboard,
  DoorOpen,
  Users,
  FileText,
  Gauge,
  Receipt,
  Wrench,
  Bell,
  Settings,
  LogOut,
  Plus,
  Search,
  ArrowLeft,
  Download,
  Check,
  AlertTriangle,
  RefreshCw,
  Menu,
  ShieldCheck,
} from "lucide-react";
import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/inter/700.css";
import { api, download, upload, imageBlob, getSession, saveSession } from "./api";
import "./tokens.css";
import "./style.css";
const money = (n) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(n || 0);
const date = () => {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en", {
      timeZone: "Asia/Ho_Chi_Minh",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
      .formatToParts(new Date())
      .map((p) => [p.type, p.value]),
  );
  return `${parts.year}-${parts.month}-${parts.day}`;
};
const period = () => date().slice(0, 7);
const labels = {
  OPEN: "Chưa xử lý",
  HANDLING: "Đang xử lý",
  RESOLVED: "Đã giải quyết",
  ACTIVE: "Hiệu lực",
  AVAILABLE: "Trống",
  RENTED: "Đang thuê",
  MAINTENANCE: "Bảo trì",
  TERMINATED: "Đã trả phòng",
  EXPIRED: "Hết hạn",
  DRAFT: "Bản nháp",
  ISSUED: "Đã phát hành",
  UNPAID: "Chưa thanh toán",
  PARTIALLY_PAID: "Thanh toán một phần",
  PAID: "Đã thanh toán",
  PENDING: "Chờ kiểm tra",
  CONFIRMED: "Đã xác nhận",
  DISMISSED: "Đã loại bỏ",
  NEW: "Mới tạo",
  ACCEPTED: "Đã tiếp nhận",
  IN_PROGRESS: "Đang xử lý",
  COMPLETED: "Hoàn thành",
  ADMIN: "Chủ nhà",
  STAFF: "Nhân viên",
  TENANT: "Người thuê",
  LOCKED: "Đã khóa",
  INACTIVE: "Ngừng hoạt động",
  URGENT: "Khẩn cấp",
  NORMAL: "Bình thường",
  LOW: "Không gấp",
  ELECTRICITY: "Điện",
  WATER: "Nước",
  CASH: "Tiền mặt",
  BANK_TRANSFER_MANUAL: "Chuyển khoản",
  OTHER: "Khác",
  DEPOSIT_OFFSET: "Cấn trừ tiền cọc",
};
const Badge = ({ value }) => (
  <span
    className={
      "badge " +
      (["PENDING", "UNPAID", "PARTIALLY_PAID", "URGENT"].includes(value)
        ? "warning"
        : ["PAID", "AVAILABLE", "COMPLETED"].includes(value)
          ? "success"
          : "")
    }
  >
    {labels[value] || value}
  </span>
);
const routes = {
  dashboard: ["Tổng quan", LayoutDashboard],
  properties: ["Khu nhà", Building2],
  rooms: ["Phòng", DoorOpen],
  tenants: ["Người thuê", Users],
  contracts: ["Hợp đồng", FileText],
  "meter-readings": ["Điện nước", Gauge],
  invoices: ["Hóa đơn", Receipt],
  "maintenance-requests": ["Sự cố", Wrench],
  notifications: ["Thông báo", Bell],
  settings: ["Thiết lập", Settings],
};
const columns = {
  properties: [
    ["property_code", "Mã khu"],
    ["name", "Tên khu"],
    ["address", "Địa chỉ"],
  ],
  rooms: [
    ["room_code", "Phòng"],
    ["property_name", "Khu nhà"],
    ["base_price", "Giá thuê", money],
    ["max_occupants", "Sức chứa"],
    ["status", "Trạng thái"],
  ],
  tenants: [
    ["full_name", "Họ tên"],
    ["identity_number", "CCCD"],
    ["phone", "Điện thoại"],
  ],
  contracts: [
    ["contract_code", "Hợp đồng"],
    ["room_code", "Phòng"],
    ["full_name", "Người đại diện"],
    ["end_date", "Hết hạn"],
    ["status", "Trạng thái"],
  ],
  invoices: [
    ["invoice_code", "Hóa đơn"],
    ["room_code", "Phòng"],
    ["billing_period", "Kỳ"],
    ["total_amount", "Tổng tiền", money],
    ["balance_due", "Còn nợ", money],
    ["status", "Phát hành"],
    ["payment_status", "Thanh toán"],
  ],
  "meter-readings": [
    ["room_code", "Phòng"],
    ["billing_period", "Kỳ"],
    ["meter_type", "Đồng hồ"],
    ["old_index", "Chỉ số cũ"],
    ["new_index", "Chỉ số mới"],
    ["consumption", "Tiêu thụ"],
    ["has_anomaly", "Cảnh báo", (v) => (v ? "Cần kiểm tra" : "Bình thường")],
  ],
  "maintenance-requests": [
    ["title", "Nội dung"],
    ["room_code", "Phòng"],
    ["priority", "Ưu tiên"],
    ["expected_date", "Hạn xử lý"],
    ["status", "Trạng thái"],
  ],
};
const f = (name, label, type = "text", extra = {}) => ({
  name,
  label,
  type,
  ...extra,
});
function App() {
  const loadVersion = useRef(0);
  const [session, setSession] = useState(getSession()),
    [route, setRoute] = useState(() =>
      getSession()?.user.role === "TENANT" ? "invoices" : "dashboard",
    ),
    [rows, setRows] = useState([]),
    [loading, setLoading] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [search, setSearch] = useState(""),
    [detail, setDetail] = useState(null),
    [form, setForm] = useState(null),
    [mobile, setMobile] = useState(false),
    [dash, setDash] = useState(null),
    [alerts, setAlerts] = useState([]),
    [month, setMonth] = useState(period());
  const tenant = session?.user.role === "TENANT",
    admin = session?.user.role === "ADMIN";
  useEffect(() => {
    const expired = () => {
      loadVersion.current++;
      setSession(null);
      setDetail(null);
      setForm(null);
      setRows([]);
      setDash(null);
      setAlerts([]);
    };
    window.addEventListener("session-expired", expired);
    return () => window.removeEventListener("session-expired", expired);
  }, []);
  const run = async (action) => {
    setError("");
    try {
      await action();
    } catch (e) {
      setError(e.message);
    }
  };
  const load = useCallback(async () => {
    if (!session) return;
    const version = ++loadVersion.current;
    const current = () =>
      version === loadVersion.current &&
      getSession()?.user.id === session.user.id;
    setLoading(true);
    setError("");
    try {
      if (route === "dashboard") {
        const [d, a] = await Promise.all([
          api("/dashboard?period=" + month),
          api("/alerts"),
        ]);
        if (!current()) return;
        setDash(d);
        setAlerts(a);
      } else if (route === "settings") {
        const [p, s, prices, th, u, a] = await Promise.all([
          api("/properties"),
          api("/services"),
          api("/prices"),
          api("/thresholds"),
          admin ? api("/users") : Promise.resolve([]),
          admin ? api("/assignments") : Promise.resolve([]),
        ]);
        if (!current()) return;
        setRows({
          properties: p,
          services: s,
          prices,
          thresholds: th,
          users: u,
          assignments: a,
        });
      } else {
        const data = await api("/" + route);
        if (!current()) return;
        setRows(data);
        if (route === "meter-readings") {
          const data = await api("/alerts");
          if (current()) setAlerts(data);
        }
      }
    } catch (e) {
      if (current()) setError(e.message);
    } finally {
      if (current()) setLoading(false);
    }
  }, [session, route, month, admin]);
  useEffect(() => {
    load();
  }, [load]);
  useEffect(() => {
    if (detail) window.scrollTo({ top: 0, left: 0, behavior: "instant" });
  }, [detail]);
  const navigate = (r) => {
    setRoute(r);
    setSearch("");
    setDetail(null);
    setNotice("");
    setMobile(false);
  };
  const open = async (row) =>
    run(async () => {
      if (["invoices", "contracts"].includes(route))
        setDetail(await api("/" + route + "/" + row.id));
      else setDetail(row);
    });
  const showForm = (
    title,
    fields,
    endpoint,
    method = "POST",
    initial = {},
    after,
  ) => setForm({ title, fields, endpoint, method, initial, after });
  const options = (items, label) => items.map((v) => [v.id, label(v)]);
  const create = () =>
    run(async () => {
      const properties = tenant ? [] : await api("/properties");
      const prop = options(properties, (p) => p.name);
      if (route === "properties")
        showForm(
          "Thêm khu nhà",
          [f("code", "Mã khu"), f("name", "Tên khu"), f("address", "Địa chỉ")],
          "/properties",
        );
      if (route === "rooms") {
        const floors = await api("/floors");
        showForm(
          "Thêm phòng",
          [
            f("propertyId", "Khu nhà", "select", { options: prop }),
            f("floorId", "Tầng", "select", {
              options: options(
                floors,
                (v) =>
                  properties.find((p) => p.id === v.property_id)?.name +
                  " · " +
                  v.name,
              ),
            }),
            f("code", "Mã phòng"),
            f("price", "Giá thuê / tháng", "number"),
            f("maxOccupants", "Sức chứa", "number"),
            f("area", "Diện tích (m²)", "number"),
          ],
          "/rooms",
          "POST",
          { maxOccupants: 2 },
        );
      }
      if (route === "tenants") {
        const accounts = admin ? await api("/users") : [];
        showForm(
          "Thêm người thuê",
          [
            f("fullName", "Họ tên"),
            f("identityNumber", "CCCD"),
            f("phone", "Điện thoại"),
            f("email", "Email", "email", { optional: true }),
            ...(admin
              ? [
                  f("userId", "Tài khoản người thuê", "select", {
                    optional: true,
                    options: accounts
                      .filter((u) => u.role === "TENANT")
                      .map((u) => [u.id, u.full_name + " · " + u.username]),
                  }),
                ]
              : []),
          ],
          "/tenants",
        );
      }
      if (route === "contracts") {
        const [r, t] = await Promise.all([api("/rooms"), api("/tenants")]);
        showForm(
          "Lập hợp đồng",
          [
            f("code", "Mã hợp đồng"),
            f("roomId", "Phòng", "select", {
              options: options(
                r.filter((v) => v.status === "AVAILABLE"),
                (v) => v.property_name + " · " + v.room_code,
              ),
            }),
            f("tenantId", "Người đại diện", "select", {
              options: options(t, (v) => v.full_name),
            }),
            f("startDate", "Ngày bắt đầu", "date"),
            f("endDate", "Ngày kết thúc (không tính ngày này)", "date"),
            f("rentalPrice", "Tiền thuê / tháng", "number"),
            f("deposit", "Tiền cọc đã nhận", "number"),
            f("paymentDay", "Ngày thanh toán (1–28)", "number"),
            f("electricIndex", "Chỉ số điện ban đầu", "number"),
            f("waterIndex", "Chỉ số nước ban đầu", "number"),
          ],
          "/contracts",
          "POST",
          {
            startDate: date(),
            paymentDay: 5,
            deposit: 0,
            electricIndex: 0,
            waterIndex: 0,
          },
        );
      }
      if (route === "meter-readings") {
        const c = await api("/contracts");
        showForm(
          "Ghi chỉ số điện nước",
          [
            f("contractId", "Hợp đồng", "select", {
              options: options(
                c.filter((v) => v.status === "ACTIVE"),
                (v) => v.room_code + " · " + v.full_name,
              ),
            }),
            f("period", "Kỳ", "month"),
            f("meterType", "Đồng hồ", "select", {
              options: [
                ["ELECTRICITY", "Điện"],
                ["WATER", "Nước"],
              ],
            }),
            f("newIndex", "Chỉ số mới", "number"),
            f("readingDate", "Ngày ghi", "date"),
          ],
          "/meter-readings",
          "POST",
          { period: period(), readingDate: date() },
        );
      }
      if (route === "invoices") {
        const c = await api("/contracts");
        showForm(
          "Tạo hóa đơn nháp",
          [
            f("contractId", "Hợp đồng", "select", {
              options: options(c, (v) => v.room_code + " · " + v.full_name),
            }),
            f("period", "Kỳ hóa đơn", "month"),
            f("issueDate", "Ngày phát hành (tự lấy theo khu nhà)", "date", {optional:true}),
            f("dueDate", "Hạn thanh toán (tự lấy theo hợp đồng)", "date", {optional:true}),
          ],
          "/invoices",
          "POST",
          { period: period() },
        );
      }
      if (route === "maintenance-requests") {
        const c = await api("/contracts");
        showForm(
          "Báo sự cố",
          [
            f("contractId", "Phòng / hợp đồng", "select", {
              options: options(
                c.filter((v) => v.status === "ACTIVE"),
                (v) => v.room_code + " · " + v.contract_code,
              ),
            }),
            f("title", "Tiêu đề"),
            f("description", "Mô tả", "textarea"),
            f("category", "Loại sự cố", "select", {
              options: [
                ["ELECTRICITY", "Điện"],
                ["WATER", "Nước"],
                ["AIR_CONDITIONER", "Điều hòa"],
                ["LOCK", "Khóa cửa"],
                ["OTHER", "Khác"],
              ],
            }),
            f("priority", "Ưu tiên", "select", {
              options: [
                ["NORMAL", "Bình thường"],
                ["URGENT", "Khẩn cấp"],
                ["LOW", "Không gấp"],
              ],
            }),
            f("imageUrls", "Link ảnh (mỗi dòng một ảnh)", "urls", {
              optional: true,
            }),
          ],
          "/maintenance-requests",
          "POST",
          { priority: "NORMAL", category: "OTHER" },
        );
      }
    });
  const mutate = async (path, method, data, message) => {
    await api(path, method, data);
    setNotice(message);
    setDetail(null);
    await load();
  };
  const logout = () =>
    run(async () => {
      await api("/auth/logout", "POST");
      saveSession(null);
      setSession(null);
    });
  if (!session)
    return (
      <Login
        onLogin={(s) => {
          saveSession(s);
          loadVersion.current++;
          setSession(s);
          setDetail(null);
          setForm(null);
          setRows([]);
          setDash(null);
          setAlerts([]);
          setSearch("");
          setError("");
          setNotice("");
          setRoute(s.user.role === "TENANT" ? "invoices" : "dashboard");
        }}
      />
    );
  const nav = Object.entries(routes).filter(
    ([r]) =>
      !tenant ||
      [
        "rooms",
        "contracts",
        "invoices",
        "maintenance-requests",
        "notifications",
      ].includes(r),
  );
  return (
    <div className="app">
      <aside className={mobile ? "sidebar open" : "sidebar"}>
        <a
          className="brand"
          href="#"
          onClick={(e) => {
            e.preventDefault();
            navigate(tenant ? "invoices" : "dashboard");
          }}
        >
          <Building2 size={22} />
          <span>
            Nhà Trọ<small>Quản lý minh bạch</small>
          </span>
        </a>
        <nav>
          {nav.map(([r, [label, Icon]]) => (
            <button
              key={r}
              className={route === r ? "nav active" : "nav"}
              onClick={() => navigate(r)}
            >
              <Icon size={18} />
              {label}
            </button>
          ))}
        </nav>
        <div className="profile">
          <strong>{session.user.name}</strong>
          <small>{labels[session.user.role]}</small>
          {tenant && (
            <button
              onClick={() =>
                showForm(
                  "Đổi mật khẩu",
                  [
                    f("oldPassword", "Mật khẩu hiện tại", "password"),
                    f("newPassword", "Mật khẩu mới", "password", {
                      minLength: 12,
                    }),
                  ],
                  "/auth/change-password",
                  "POST",
                  {},
                  () => {
                    saveSession(null);
                    window.dispatchEvent(new Event("session-expired"));
                  },
                )
              }
            >
              Đổi mật khẩu
            </button>
          )}
          <button className="danger" onClick={logout}>
            <LogOut size={16} />
            Đăng xuất
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <button
            className="menu"
            aria-label="Mở menu"
            onClick={() => setMobile(!mobile)}
          >
            <Menu size={20} />
          </button>
          <span>{tenant ? "Cổng người thuê" : "Không gian quản lý"}</span>
          <button onClick={() => navigate("notifications")}>
            <Bell size={16} />
            Thông báo
          </button>
        </header>
        <main>
          <div className="pagehead">
            <div>
              <p className="eyebrow">
                {tenant ? "THÔNG TIN CỦA BẠN" : "QUẢN LÝ NHÀ TRỌ"}
              </p>
              <h1>{routes[route][0]}</h1>
              <p className="muted">
                {route === "invoices"
                  ? "Mỗi khoản thu đều có chỉ số, đơn giá và cách tính rõ ràng."
                  : route === "meter-readings"
                    ? "Kiểm tra cảnh báo trước khi phát hành hóa đơn."
                    : "Dữ liệu được cập nhật từ hệ thống quản lý."}
              </p>
            </div>
            {!detail &&
              !["dashboard", "settings", "notifications"].includes(route) &&
              (!tenant || route === "maintenance-requests") && (
                <button className="primary" onClick={create}>
                  <Plus size={16} />
                  {route === "invoices"
                    ? "Tạo hóa đơn"
                    : route === "maintenance-requests"
                      ? "Báo sự cố"
                      : "Thêm mới"}
                </button>
              )}
          </div>
          {error && (
            <div className="banner error" role="alert">
              {error}
              <button aria-label="Đóng lỗi" onClick={() => setError("")}>
                ×
              </button>
            </div>
          )}
          {notice && (
            <div className="banner success" role="status">
              {notice}
            </div>
          )}
          {loading ? (
            <div className="empty">
              <RefreshCw className="spin" />
              Đang tải dữ liệu…
            </div>
          ) : detail ? (
            <Detail
              route={route}
              data={detail}
              tenant={tenant}
              back={() => setDetail(null)}
              showForm={showForm}
              run={run}
              mutate={mutate}
            />
          ) : route === "dashboard" ? (
            <Dashboard
              data={dash}
              alerts={alerts}
              month={month}
              setMonth={setMonth}
              navigate={navigate}
              run={run}
            />
          ) : route === "settings" ? (
            <SettingsPanel
              data={rows}
              admin={admin}
              showForm={showForm}
              run={run}
              mutate={mutate}
            />
          ) : route === "notifications" ? (
            <div className="card notifications">
              {Array.isArray(rows) && rows.length ? (
                rows.map((n) => (
                  <article key={n.id} className={n.is_read ? "read" : ""}>
                    <Bell size={18} />
                    <div>
                      <strong>{n.message}</strong>
                      <small>
                        {new Date(n.created_at).toLocaleString("vi-VN")}
                      </small>
                    </div>
                    {!n.is_read && (
                      <button
                        onClick={() =>
                          run(() =>
                            mutate(
                              "/notifications/" + n.id + "/read",
                              "PUT",
                              undefined,
                              "Đã đánh dấu đã đọc",
                            ),
                          )
                        }
                      >
                        Đã đọc
                      </button>
                    )}
                  </article>
                ))
              ) : (
                <Empty text="Chưa có thông báo." />
              )}
            </div>
          ) : (
            <>
              <div className="toolbar">
                <label className="search">
                  <Search size={17} />
                  <input
                    aria-label="Tìm kiếm"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Tìm trong danh sách…"
                  />
                </label>
                <span className="muted">
                  {Array.isArray(rows) ? rows.length : 0} bản ghi
                </span>
                <button onClick={load} aria-label="Làm mới">
                  <RefreshCw size={16} />
                </button>
              </div>
              {route === "meter-readings" &&
                alerts.filter((a) => a.status === "PENDING").length > 0 && (
                  <div className="card alert-list">
                    <h2>Cảnh báo cần kiểm tra</h2>
                    {alerts
                      .filter((a) => a.status === "PENDING")
                      .map((a) => (
                        <div key={a.id} className="alert-row">
                          <AlertTriangle size={18} />
                          <span>
                            Phòng {a.room_code} · {labels[a.meter_type]} kỳ{" "}
                            {a.billing_period}: tăng {a.percentage_increase}% so
                            với trung bình {a.baseline_average}
                          </span>
                          <button
                            onClick={() =>
                              showForm(
                                "Kiểm tra cảnh báo",
                                [
                                  f("status", "Kết luận", "select", {
                                    options: [
                                      ["CONFIRMED", "Đã kiểm tra, chỉ số đúng"],
                                      ["DISMISSED", "Loại bỏ cảnh báo"],
                                    ],
                                  }),
                                  f("note", "Ghi chú kiểm tra", "textarea"),
                                ],
                                "/alerts/" + a.id,
                                "PUT",
                              )
                            }
                          >
                            Kiểm tra
                          </button>
                        </div>
                      ))}
                  </div>
                )}
              <Table
                columns={columns[route]}
                rows={
                  Array.isArray(rows)
                    ? rows.filter((r) =>
                        Object.values(r).some((v) =>
                          String(v ?? "")
                            .toLowerCase()
                            .includes(search.toLowerCase()),
                        ),
                      )
                    : []
                }
                onOpen={open}
              />
            </>
          )}
        </main>
        <footer>Nhà Trọ · Hóa đơn minh bạch, quản lý rõ ràng</footer>
      </div>
      {form && (
        <FormDialog
          spec={form}
          onClose={() => setForm(null)}
          onSave={async (values) => {
            const saved = await api(form.endpoint, form.method, values);
            setForm(null);
            setDetail(null);
            setNotice("Đã lưu thành công.");
            await load();
            if (form.endpoint.endsWith("/checkout") && form.method === "POST") {
              setDetail(await api("/contracts/" + saved.contract_id));
              setNotice("Đã trả phòng và lập quyết toán cọc.");
            }
            await form.after?.(saved);
          }}
        />
      )}
    </div>
  );
}
function Empty({
  text = "Chưa có dữ liệu. Thêm bản ghi đầu tiên để bắt đầu.",
}) {
  return (
    <div className="empty">
      <FileText size={28} />
      <p>{text}</p>
    </div>
  );
}
function Table({ columns = [], rows, onOpen }) {
  return (
    <div className="card table-wrap">
      {rows.length ? (
        <table>
          <thead>
            <tr>
              {columns.map(([k, l]) => (
                <th key={k}>{l}</th>
              ))}
              <th>Chi tiết</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                {columns.map(([k, , format]) => (
                  <td key={k}>
                    {format ? (
                      format(r[k])
                    ) : [
                        "status",
                        "payment_status",
                        "priority",
                        "meter_type",
                      ].includes(k) ? (
                      <Badge value={r[k]} />
                    ) : (
                      (r[k] ?? "—")
                    )}
                  </td>
                ))}
                <td>
                  <button onClick={() => onOpen(r)}>Xem</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <Empty />
      )}
    </div>
  );
}
function Dashboard({ data, alerts, month, setMonth, navigate, run }) {
  if (!data) return null;
  return (
    <>
      <div className="toolbar">
        <label>
          Kỳ thống kê{" "}
          <input
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
          />
        </label>
        <button
          onClick={() =>
            run(() => download("/reports/debts.csv", "cong-no.csv"))
          }
        >
          <Download size={16} />
          Xuất công nợ
        </button>
        <button
          onClick={() => run(() => download("/reports/pdf", "bao-cao.pdf"))}
        >
          Báo cáo PDF
        </button>
      </div>
      <div className="stats">
        {[
          ["Tổng số phòng", data.total_rooms, DoorOpen],
          ["Phòng trống", data.available_rooms, Building2],
          ["Đã lập hóa đơn", money(data.billed), Receipt],
          ["Thực tế đã thu", money(data.collected), Check],
        ].map(([label, value, Icon]) => (
          <div className="card stat" key={label}>
            <div>
              <span className="muted">{label}</span>
              <Icon size={18} />
            </div>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
      <p className="muted">Tiền thu tính theo ngày nhận trong tháng {month}; cấn trừ cọc là giao dịch nội bộ. Tiền đã bù vào hóa đơn kỳ này: {money(data.credited)}.</p>
      <div className="dashboard-grid">
        <section className="card">
          <h2>Công nợ và việc cần xử lý</h2>
          <div className="metric">
            <span>Công nợ kỳ {month}</span>
            <strong>{money(data.outstanding)}</strong>
          </div>
          <div className="metric">
            <span>Phòng đang thuê / bảo trì</span>
            <strong>
              {data.rented_rooms} / {data.maintenance_rooms}
            </strong>
          </div>
          <div className="metric">
            <span>Sự cố chưa hoàn thành</span>
            <strong>{data.open_maintenance}</strong>
          </div>
          <button onClick={() => navigate("invoices")}>Xem hóa đơn</button>
        </section>
        <section className="card">
          <h2>Cảnh báo tiêu thụ</h2>
          {alerts.filter((a) => a.status === "PENDING").length ? (
            alerts
              .filter((a) => a.status === "PENDING")
              .slice(0, 4)
              .map((a) => (
                <div className="metric" key={a.id}>
                  <span>
                    Phòng {a.room_code} · {labels[a.meter_type]}
                  </span>
                  <Badge value="PENDING" />
                </div>
              ))
          ) : (
            <p className="muted">Không có cảnh báo chờ kiểm tra.</p>
          )}
          <button onClick={() => navigate("meter-readings")}>
            Mở điện nước
          </button>
        </section>
      </div>
      <section className="card">
        <h2>Hợp đồng sắp hết hạn · 30 ngày</h2>
        {data.expiringContracts?.length ? (
          data.expiringContracts.map((c) => (
            <div className="metric" key={c.id}>
              <span>
                {c.contract_code} · Phòng {c.room_code}
              </span>
              <strong>{c.end_date}</strong>
            </div>
          ))
        ) : (
          <p className="muted">Không có hợp đồng sắp hết hạn.</p>
        )}
      </section>
    </>
  );
}
function Detail({ route, data: d, tenant, back, showForm, run, mutate }) {
  const [repairImages,setRepairImages]=useState((d.image_urls||"").split("\n").filter(Boolean));
  const [lookups, setLookups] = useState({
    accounts: [],
    floors: [],
    properties: [],
    assignees: [],
    images: [],
  });
  useEffect(() => {
    let mounted = true;
    setRepairImages((d.image_urls||"").split("\n").filter(Boolean));
    run(async () => {
      const result = {
        accounts: [],
        floors: [],
        properties: [],
        assignees: [],
        images: [],
      };
      if (route === "rooms") {
        result.images = await api("/rooms/" + d.id + "/images");
        if (!tenant) {
          [result.floors, result.properties] = await Promise.all([
            api("/floors"),
            api("/properties"),
          ]);
        }
      }
      if (route === "tenants" && getSession().user.role === "ADMIN")
        result.accounts = await api("/users");
      if (route === "maintenance-requests" && !tenant)
        result.assignees = await api(
          "/rooms/" + d.room_id + "/assignees",
        );
      if (mounted) setLookups(result);
    });
    return () => {
      mounted = false;
    };
  }, [route, d.id, tenant]);
  const names = {
    contract_code: "Mã hợp đồng",
    room_code: "Phòng",
    start_date: "Ngày bắt đầu",
    end_date: "Ngày kết thúc",
    rental_price: "Giá thuê",
    deposit_amount: "Tiền cọc",
    status: "Trạng thái",
    billing_period: "Kỳ",
    issue_date: "Ngày phát hành",
    due_date: "Hạn thanh toán",
    total_amount: "Tổng tiền",
    paid_amount: "Đã thanh toán",
    balance_due: "Còn nợ",
    payment_status: "Thanh toán",
    full_name: "Họ tên",
    identity_number: "CCCD",
    phone: "Điện thoại",
    email: "Email",
    property_name: "Khu nhà",
    base_price: "Giá thuê",
    max_occupants: "Sức chứa",
    area_sqm: "Diện tích",
    title: "Tiêu đề",
    description: "Mô tả",
    priority: "Ưu tiên",
    expected_date: "Hạn xử lý",
    actual_cost: "Chi phí",
    progress_note: "Tiến độ",
    meter_type: "Đồng hồ",
    old_index: "Chỉ số cũ",
    new_index: "Chỉ số mới",
    consumption: "Tiêu thụ",
    reading_date: "Ngày ghi",
    property_code: "Mã khu",
    name: "Tên",
    address: "Địa chỉ",
    amenities: "Tiện nghi",
    internal_note: "Ghi chú nội bộ",
    terms: "Điều khoản",
    violation_note: "Vi phạm",
  };
  return (
    <>
      <div className="toolbar">
        <button onClick={back}>
          <ArrowLeft size={16} />
          Danh sách
        </button>
        {["invoices", "contracts"].includes(route) && (
          <button
            onClick={() =>
              run(() =>
                download(
                  "/" + route + "/" + d.id + "/pdf",
                  route + "-" + d.id + ".pdf",
                ),
              )
            }
          >
            <Download size={16} />
            Tải PDF
          </button>
        )}
        {!tenant && route === "invoices" && d.status === "DRAFT" && (
          <>
            <button
              className="primary"
              onClick={() =>
                run(() =>
                  mutate(
                    "/invoices/" + d.id + "/issue",
                    "POST",
                    undefined,
                    "Đã phát hành hóa đơn.",
                  ),
                )
              }
            >
              Phát hành
            </button>
            <button
              className="danger"
              onClick={() =>
                showForm("Xóa hóa đơn nháp", [], "/invoices/" + d.id, "DELETE")
              }
            >
              Xóa nháp
            </button>
          </>
        )}
        {!tenant &&
          route === "invoices" &&
          d.status === "ISSUED" &&
          Number(d.balance_due) > 0 && (
            <button
              className="primary"
              onClick={() =>
                showForm(
                  "Ghi nhận thanh toán",
                  [
                    f("code", "Mã giao dịch"),
                    f("amount", "Số tiền", "number", { max: d.balance_due }),
                    f("method", "Phương thức", "select", {
                      options: [
                        ["CASH", "Tiền mặt"],
                        ["BANK_TRANSFER_MANUAL", "Chuyển khoản"],
                        ["OTHER", "Khác"],
                      ],
                    }),
                    f("notes", "Ghi chú", "textarea", { optional: true }),
                  ],
                  "/invoices/" + d.id + "/payments",
                  "POST",
                  {
                    code: "PAY-" + Date.now(),
                    amount: d.balance_due,
                    method: "CASH",
                  },
                )
              }
            >
              Thu tiền
            </button>
          )}
      </div>
      <section className="card">
        <h2>
          {d.invoice_code ||
            d.contract_code ||
            d.room_code ||
            d.full_name ||
            d.title ||
            d.name ||
            "Thông tin chi tiết"}
        </h2>
        <dl>
          {Object.entries(names)
            .filter(([key]) => d[key] !== undefined && d[key] !== null)
            .map(([key, label]) => (
              <div key={key}>
                <dt>{label}</dt>
                <dd>
                  {key.includes("amount") ||
                  [
                    "rental_price",
                    "base_price",
                    "actual_cost",
                    "balance_due",
                  ].includes(key)
                    ? money(d[key])
                    : labels[d[key]] || String(d[key])}
                </dd>
              </div>
            ))}
        </dl>
      </section>
      {route === "contracts" && d.checkout && (
        <section className="card" aria-label="Quyết toán trả phòng">
          <h2>Quyết toán trả phòng</h2>
          <dl>
            <div><dt>Ngày trả phòng</dt><dd>{d.checkout.checkout_date}</dd></div>
            <div><dt>Tiền cọc ban đầu</dt><dd>{money(d.checkout.initial_deposit)}</dd></div>
            <div><dt>Công nợ trước cấn cọc</dt><dd>{money(d.checkout.unpaid_invoices_amount)}</dd></div>
            <div><dt>Khấu trừ thiệt hại</dt><dd>{money(d.checkout.damage_amount)}</dd></div>
            <div><dt>Chỉ số điện cuối</dt><dd>{d.checkout.final_electric_index}</dd></div>
            <div><dt>Chỉ số nước cuối</dt><dd>{d.checkout.final_water_index}</dd></div>
            <div><dt>Điện nước kỳ cuối</dt><dd>{money(d.checkout.final_utility_amount)}</dd></div>
            <div><dt>{Number(d.checkout.actual_refund_amount) < 0 ? "Cần thu thêm" : "Tiền hoàn cọc"}</dt><dd><strong>{money(Math.abs(Number(d.checkout.actual_refund_amount)))}</strong></dd></div>
          </dl>
          <p className="muted">Điện nước kỳ cuối đã nằm trong hóa đơn; số tiền quyết toán chỉ khấu trừ công nợ một lần.</p>
          {d.checkout.notes && <p>{d.checkout.notes}</p>}
        </section>
      )}
      {route === "rooms" && (
        <section className="card">
          <h2>Ảnh phòng</h2>
          {lookups.images.length ? (
            <div className="room-images">
              {lookups.images.map((i) => (
                <div key={i.id}>
                  <ProtectedImage src={i.url} alt={"Ảnh phòng " + d.room_code} />
                  {!tenant && <button onClick={()=>run(async()=>{await api("/rooms/"+d.id+"/images/"+i.id,"DELETE");const images=await api("/rooms/"+d.id+"/images");setLookups(v=>({...v,images}));})}>Gỡ ảnh</button>}
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">Chưa có ảnh phòng.</p>
          )}
          {!tenant && <ImageUpload endpoint={"/rooms/"+d.id+"/images"} reload={async () => {const images=await api("/rooms/"+d.id+"/images");setLookups(v=>({...v,images}));}} />}
        </section>
      )}
      {route === "maintenance-requests" && repairImages.length > 0 && (
        <section className="card">
          <h2>Ảnh sự cố</h2>
          <div className="room-images">
            {repairImages
              .map((url, i) => (
                <div key={i}>
                  <ProtectedImage src={url} alt={"Ảnh sự cố " + (i + 1)} />
                </div>
              ))}
          </div>
        </section>
      )}
      {route === "maintenance-requests" && d.status !== "COMPLETED" && <section className="card"><h2>Đính kèm ảnh</h2><ImageUpload endpoint={"/maintenance-requests/"+d.id+"/images"} reload={async()=>{const rows=await api("/maintenance-requests");setRepairImages((rows.find(r=>r.id===d.id)?.image_urls||"").split("\n").filter(Boolean));}} /></section>}
      {route === "invoices" && (
        <>
          <section className="card">
            <h2>Chi tiết cách tính</h2>
            <div className="invoice-mobile-lines">
              {d.items?.map((i) => (
                <article key={i.id}>
                  <div className="metric">
                    <strong>{i.item_name_snapshot}</strong>
                    <strong>{money(i.subtotal)}</strong>
                  </div>
                  <small>Đơn giá: {money(i.unit_price_snapshot)}</small>
                  <p>{i.formula_description}</p>
                </article>
              ))}
            </div>
            <div className="table-wrap invoice-desktop">
              <table>
                <thead>
                  <tr>
                    <th>Khoản thu</th>
                    <th>Đơn giá</th>
                    <th>Cách tính</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {d.items?.map((i) => (
                    <tr key={i.id}>
                      <td>{i.item_name_snapshot}</td>
                      <td>{money(i.unit_price_snapshot)}</td>
                      <td>{i.formula_description}</td>
                      <td>{money(i.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
          <section className="card">
            <h2>Lịch sử thanh toán</h2>
            {d.payments?.length ? (
              d.payments.map((p) => (
                <div className="metric" key={p.id}>
                  <span>
                    {p.transaction_code} · {labels[p.payment_method]}
                    <small>{p.notes}</small>
                  </span>
                  <strong>{money(p.amount)}</strong>
                </div>
              ))
            ) : (
              <p className="muted">Chưa ghi nhận thanh toán.</p>
            )}
          </section>
        </>
      )}
      {!tenant && route === "meter-readings" && (
        <button
          onClick={() =>
            showForm(
              "Sửa chỉ số",
              [
                f("newIndex", "Chỉ số mới", "number"),
                f("reason", "Lý do chỉnh sửa", "textarea"),
              ],
              "/meter-readings/" + d.id,
              "PUT",
              { newIndex: d.new_index },
            )
          }
        >
          Sửa chỉ số
        </button>
      )}
      {route === "contracts" && (
        <section className="card">
          <h2>Thành viên cùng phòng</h2>
          {d.members?.length ? (
            d.members.map((m) => (
              <div className="metric" key={m.id}>
                <span>{m.full_name}</span>
                <span>
                  {m.phone || "—"}
                  <small>
                    {m.effective_from} → {m.effective_to || "Đang ở"}
                  </small>
                </span>
                {!tenant && !m.effective_to && d.status === "ACTIVE" && (
                  <button
                    onClick={() =>
                      showForm(
                        "Ghi nhận thành viên rời phòng",
                        [f("effectiveTo", "Ngày rời phòng", "date")],
                        "/contracts/" + d.id + "/members/" + m.id + "/depart",
                      )
                    }
                  >
                    Rời phòng
                  </button>
                )}
              </div>
            ))
          ) : (
            <p className="muted">Chỉ có người đại diện.</p>
          )}
          {!tenant && d.status === "ACTIVE" && (
            <div className="actions">
              <button
                onClick={() =>
                  showForm(
                    "Thêm thành viên",
                    [
                      f("fullName", "Họ tên"),
                      f("identityNumber", "CCCD", "text", { optional: true }),
                      f("phone", "Điện thoại", "text", { optional: true }),
                      f("relationship", "Quan hệ", "text", { optional: true }),
                      f("effectiveFrom", "Ngày vào ở", "date"),
                    ],
                    "/contracts/" + d.id + "/members",
                  )
                }
              >
                Thêm thành viên
              </button>
              <button
                onClick={() =>
                  showForm(
                    "Gia hạn hợp đồng",
                    [f("endDate", "Ngày kết thúc mới", "date")],
                    "/contracts/" + d.id + "/renew",
                  )
                }
              >
                Gia hạn
              </button>
              <button
                onClick={() =>
                  showForm(
                    "Điều khoản và vi phạm",
                    [
                      f("terms", "Điều khoản", "textarea"),
                      f("violationNote", "Ghi chú vi phạm", "textarea", {
                        optional: true,
                      }),
                    ],
                    "/contracts/" + d.id + "/notes",
                    "PUT",
                    {
                      terms: d.terms || "",
                      violationNote: d.violation_note || "",
                    },
                  )
                }
              >
                Điều khoản
              </button>
              <button
                className="danger"
                onClick={() =>
                  showForm(
                    "Trả phòng và quyết toán cọc",
                    [
                      f("date", "Ngày trả phòng (không tính ngày này)", "date"),
                      f("electricIndex", "Chỉ số điện cuối", "number"),
                      f("waterIndex", "Chỉ số nước cuối", "number"),
                      f("damage", "Khấu trừ thiệt hại", "number"),
                      f("notes", "Ghi chú", "textarea", { optional: true }),
                    ],
                    "/contracts/" + d.id + "/checkout",
                    "POST",
                    { date: date(), damage: 0 },
                  )
                }
              >
                Trả phòng
              </button>
            </div>
          )}
        </section>
      )}
      {route === "contracts" && <section className="card">
        <h2>Vi phạm hợp đồng</h2>
        {d.violations?.length ? d.violations.map(v=><div className="violation" key={v.id}>
          <div className="metric"><strong>{v.reason}</strong><span>{labels[v.status]}</span></div>
          <small>Ghi nhận {new Date(v.created_at).toLocaleString("vi-VN")} · {v.recorded_by_name}</small>
          {v.resolution_note && <p>{v.resolution_note}</p>}
          {!tenant && ["OPEN","HANDLING"].includes(v.status) && <button onClick={()=>showForm("Xử lý vi phạm",[f("status","Trạng thái","select",{options:[["HANDLING","Đang xử lý"],["RESOLVED","Đã giải quyết"],["DISMISSED","Loại bỏ"]]}),f("note","Nội dung xử lý / lý do","textarea")],"/contracts/"+d.id+"/violations/"+v.id,"PUT")}>Xử lý vi phạm</button>}
        </div>) : <p className="muted">Chưa có vi phạm được ghi nhận.</p>}
        {!tenant && d.status === "ACTIVE" && <button onClick={()=>showForm("Ghi nhận vi phạm",[f("reason","Nội dung và lý do vi phạm","textarea")],"/contracts/"+d.id+"/violations")}>Ghi nhận vi phạm</button>}
      </section>}
      {!tenant &&
        route === "maintenance-requests" &&
        d.status !== "COMPLETED" && (
          <button
            className="primary"
            onClick={() =>
              showForm(
                "Cập nhật xử lý",
                [
                  f("status", "Trạng thái tiếp theo", "select", {
                    options: (d.status === "NEW"
                      ? ["ACCEPTED"]
                      : d.status === "ACCEPTED"
                        ? ["ACCEPTED", "IN_PROGRESS"]
                        : ["IN_PROGRESS", "COMPLETED"]
                    ).map((v) => [v, labels[v]]),
                  }),
                  f("assignedTo", "Người xử lý", "select", {
                    optional: true,
                    options: lookups.assignees.map((u) => [u.id, u.full_name]),
                  }),
                  f("expectedDate", "Hạn xử lý", "date"),
                  f("actualCost", "Chi phí thực tế", "number"),
                  f("note", "Tiến độ", "textarea", { optional: true }),
                ],
                "/maintenance-requests/" + d.id,
                "PUT",
                {
                  expectedDate: d.expected_date,
                  actualCost: d.actual_cost || 0,
                },
              )
            }
          >
            Cập nhật xử lý
          </button>
        )}
      {!tenant && route === "rooms" && (
        <div className="actions">
          <button
            onClick={() =>
              showForm(
                "Sửa thông tin phòng",
                [
                  f("propertyId", "Khu nhà", "select", {
                    options: lookups.properties
                      .filter((p) => p.id === d.property_id)
                      .map((p) => [p.id, p.name]),
                  }),
                  f("floorId", "Tầng", "select", {
                    options: lookups.floors
                      .filter((v) => v.property_id === d.property_id)
                      .map((v) => [v.id, v.name]),
                  }),
                  f("code", "Mã phòng"),
                  f("price", "Giá thuê", "number"),
                  f("maxOccupants", "Sức chứa", "number"),
                  f("area", "Diện tích", "number", { optional: true }),
                ],
                "/rooms/" + d.id,
                "PUT",
                {
                  propertyId: d.property_id,
                  floorId: d.floor_id,
                  code: d.room_code,
                  price: d.base_price,
                  maxOccupants: d.max_occupants,
                  area: d.area_sqm,
                },
              )
            }
          >
            Sửa phòng / giá
          </button>
          {
            <button
              onClick={() =>
                showForm(
                  "Tiện nghi, ảnh và bảo trì",
                  [
                    f("status", "Trạng thái", "select", {
                      options:
                        d.status === "RENTED"
                          ? [["RENTED", "Đang thuê"]]
                          : [
                              ["AVAILABLE", "Trống"],
                              ["MAINTENANCE", "Bảo trì"],
                            ],
                    }),
                    f("amenities", "Tiện nghi", "textarea", { optional: true }),
                    f("internalNote", "Ghi chú nội bộ", "textarea", {
                      optional: true,
                    }),
                    f("imageUrls", "Link ảnh (mỗi dòng một ảnh)", "urls", {
                      optional: true,
                    }),
                  ],
                  "/rooms/" + d.id + "/details",
                  "PUT",
                  {
                    status: d.status,
                    amenities: d.amenities || "",
                    internalNote: d.internal_note || "",
                    imageUrls: lookups.images.map((i) => i.url).join("\n"),
                  },
                )
              }
            >
              Tiện nghi / ảnh
            </button>
          }
        </div>
      )}
      {!tenant && route === "tenants" && (
        <button
          onClick={() =>
            showForm(
              "Sửa người thuê",
              [
                f("fullName", "Họ tên"),
                f("identityNumber", "CCCD"),
                f("phone", "Điện thoại"),
                f("email", "Email", "email", { optional: true }),
                ...(getSession().user.role === "ADMIN"
                  ? [
                      f("userId", "Tài khoản người thuê", "select", {
                        optional: true,
                        options: lookups.accounts
                          .filter((u) => u.role === "TENANT")
                          .map((u) => [u.id, u.full_name + " · " + u.username]),
                      }),
                    ]
                  : []),
              ],
              "/tenants/" + d.id,
              "PUT",
              {
                fullName: d.full_name,
                identityNumber: d.identity_number,
                phone: d.phone,
                email: d.email,
                userId: d.user_id,
              },
            )
          }
        >
          Sửa thông tin
        </button>
      )}
      {!tenant && route === "tenants" && (
        <button
          onClick={() =>
            showForm(
              "Thông tin cá nhân",
              [
                f("dateOfBirth", "Ngày sinh", "date", { optional: true }),
                f("hometown", "Địa chỉ / quê quán", "textarea", {
                  optional: true,
                }),
              ],
              "/tenants/" + d.id + "/profile",
              "PUT",
              { dateOfBirth: d.date_of_birth, hometown: d.hometown },
            )
          }
        >
          Ngày sinh / địa chỉ
        </button>
      )}
    </>
  );
}
function SettingsPanel({ data: d, admin, showForm, run, mutate }) {
  const [rooms, setRooms] = useState([]),
    [schedules, setSchedules] = useState([]),
    [reports, setReports] = useState(null),
    [auditLogs, setAuditLogs] = useState(null);
  useEffect(() => {
    run(async () => {
      setRooms(await api("/rooms"));
      setSchedules(await api("/maintenance-schedules"));
    });
  }, []);
  if (!d.properties) return null;
  const prop = d.properties.map((p) => [p.id, p.name]);
  return (
    <>
      <section className="card">
        <h2>Cấu trúc và đơn giá</h2>
        <div className="actions">
          {admin && <button onClick={()=>showForm("Thêm dịch vụ",[f("code","Mã dịch vụ (chữ hoa, số, gạch dưới)"),f("name","Tên dịch vụ"),f("calculationType","Cách tính","select",{options:[["PER_ROOM","Theo phòng"],["PER_PERSON","Theo đầu người"]]}),f("unitName","Đơn vị")],"/services","POST",{calculationType:"PER_ROOM",unitName:"Tháng"})}>Thêm dịch vụ</button>}
          <button
            onClick={() =>
              showForm(
                "Thêm tầng",
                [
                  f("propertyId", "Khu nhà", "select", { options: prop }),
                  f("number", "Số tầng", "number"),
                  f("name", "Tên tầng"),
                ],
                "/floors",
              )
            }
          >
            Thêm tầng
          </button>
          <button
            onClick={() =>
              showForm(
                "Đơn giá áp dụng theo thời gian",
                [
                  f("propertyId", "Khu nhà", "select", { options: prop }),
                  f("serviceId", "Dịch vụ", "select", {
                    options: d.services.map((s) => [s.id, s.name]),
                  }),
                  f("unitPrice", "Đơn giá", "number"),
                  f("effectiveFrom", "Áp dụng từ ngày", "date"),
                ],
                "/prices",
              )
            }
          >
            Thêm đơn giá
          </button>
          <button
            onClick={() =>
              showForm(
                "Ngưỡng cảnh báo",
                [
                  f("propertyId", "Khu nhà", "select", { options: prop }),
                  f("roomId", "Phòng (trống = toàn khu)", "select", {
                    optional: true,
                    options: rooms.map((r) => [
                      r.id,
                      r.property_name + " · " + r.room_code,
                    ]),
                  }),
                  f("meterType", "Đồng hồ", "select", {
                    options: [
                      ["ELECTRICITY", "Điện"],
                      ["WATER", "Nước"],
                    ],
                  }),
                  f("percentage", "Tăng trên (%)", "number"),
                  f("months", "Số tháng tính trung bình", "number"),
                ],
                "/thresholds",
                "POST",
                { percentage: 50, months: 3 },
              )
            }
          >
            Ngưỡng cảnh báo
          </button>
          <button
            onClick={() =>
              showForm(
                "Lịch bảo trì định kỳ",
                [
                  f("propertyId", "Khu nhà", "select", { options: prop }),
                  f("title", "Nội dung"),
                  f("nextDate", "Ngày thực hiện", "date"),
                  f("intervalDays", "Lặp lại sau (ngày)", "number"),
                  f("notes", "Ghi chú", "textarea", { optional: true }),
                ],
                "/maintenance-schedules",
                "POST",
                { intervalDays: 30 },
              )
            }
          >
            Lịch bảo trì
          </button>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Khu</th>
                <th>Dịch vụ</th>
                <th>Đơn giá</th>
                <th>Áp dụng từ</th>
              </tr>
            </thead>
            <tbody>
              {d.prices.map((p) => (
                <tr key={p.id}>
                  <td>
                    {d.properties.find((v) => v.id === p.property_id)?.name}
                  </td>
                  <td>{p.name}</td>
                  <td>{money(p.unit_price)}</td>
                  <td>{p.effective_from}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      {admin && (
        <section className="card">
          <h2>Tài khoản và phân công</h2>
        {d.assignments?.map((a,i)=><div className="metric" key={i}><span>{d.users.find(u=>u.id===a.user_id)?.full_name}<small>{d.properties.find(p=>p.id===a.property_id)?.name}{a.room_id?" · Phòng "+rooms.find(r=>r.id===a.room_id)?.room_code:" · Toàn khu"}</small></span><span>{a.is_active?"Đang phân công":"Đã thu hồi"}</span></div>)}
          <div className="actions">
            <button
              onClick={() =>
                showForm(
                  "Tạo tài khoản",
                  [
                    f("username", "Tên đăng nhập"),
                    f("password", "Mật khẩu (ít nhất 12 ký tự)", "password", {
                      minLength: 12,
                    }),
                    f("fullName", "Họ tên"),
                    f("phone", "Điện thoại"),
                    f("email", "Email", "email", { optional: true }),
                    f("role", "Vai trò", "select", {
                      options: [
                        ["STAFF", "Nhân viên"],
                        ["TENANT", "Người thuê"],
                        ["ADMIN", "Chủ nhà"],
                      ],
                    }),
                  ],
                  "/users",
                )
              }
            >
              Tạo tài khoản
            </button>
            <button
              onClick={() =>
                showForm(
                  "Phân công nhân viên",
                  [
                    f("userId", "Nhân viên", "select", {
                      options: d.users
                        .filter((u) => u.role === "STAFF")
                        .map((u) => [u.id, u.full_name]),
                    }),
                    f("propertyId", "Khu nhà", "select", { options: prop }),
                    f("roomId", "Phòng (để trống để giao toàn khu)", "select", {optional:true, options:rooms.map(r=>[r.id,r.property_name+" · "+r.room_code]), dependsOn:"propertyId", source:rooms, parentKey:"property_id"}),
                    f("active", "Kích hoạt phân công", "boolean"),
                  ],
                  "/assignments",
                  "POST",
                  { active: true },
                )
              }
            >
              Phân công
            </button>
            <button
              onClick={() =>
                showForm(
                  "Thông báo chung",
                  [f("message", "Nội dung", "textarea")],
                  "/notifications/announce",
                )
              }
            >
              Thông báo chung
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Người dùng</th>
                  <th>Đăng nhập</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {d.users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.full_name}</td>
                    <td>{u.username}</td>
                    <td>{labels[u.role]}</td>
                    <td>
                      <Badge value={u.status} />
                    </td>
                    <td>
                      <div className="actions">
                        <button
                          onClick={() =>
                            run(() =>
                              mutate(
                                "/users/" + u.id + "/status",
                                "PUT",
                                {
                                  status:
                                    u.status === "ACTIVE" ? "LOCKED" : "ACTIVE",
                                },
                                "Đã cập nhật tài khoản.",
                              ),
                            )
                          }
                        >
                          {u.status === "ACTIVE" ? "Khóa" : "Mở khóa"}
                        </button>
                        <button
                          onClick={() =>
                            run(async () => {
                              const c = await api(
                                "/users/" + u.id + "/reset-code",
                                "POST",
                              );
                              navigator.clipboard?.writeText(c.code);
                              window.prompt(
                                "Mã đặt lại mật khẩu, hết hạn sau 15 phút. Chuyển cho chủ tài khoản đã xác minh:",
                                c.code,
                              );
                            })
                          }
                        >
                          Mã đặt lại
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      <section className="card">
        <h2>Bảo mật tài khoản</h2>
        <button
          onClick={() =>
            showForm(
              "Đổi mật khẩu",
              [
                f("oldPassword", "Mật khẩu hiện tại", "password"),
                f(
                  "newPassword",
                  "Mật khẩu mới (ít nhất 12 ký tự)",
                  "password",
                  { minLength: 12 },
                ),
              ],
              "/auth/change-password",
              "POST",
              {},
              () => {
                saveSession(null);
                window.dispatchEvent(new Event("session-expired"));
              },
            )
          }
        >
          Đổi mật khẩu
        </button>
      </section>
      <section className="card">
        <h2>Lịch bảo trì định kỳ</h2>
        {schedules.length ? (
          schedules.map((s) => (
            <div className="metric" key={s.id}>
              <span>
                {s.title}
                <small>
                  {d.properties.find((p) => p.id === s.property_id)?.name} · Lặp
                  mỗi {s.interval_days} ngày
                </small>
              </span>
              <strong>{s.next_date}</strong>
              <button
                onClick={() =>
                  run(async () => {
                    await mutate(
                      "/maintenance-schedules/" + s.id + "/complete",
                      "POST",
                      undefined,
                      "Đã hoàn tất kỳ bảo trì.",
                    );
                    setSchedules(await api("/maintenance-schedules"));
                  })
                }
              >
                Hoàn tất kỳ
              </button>
            </div>
          ))
        ) : (
          <p className="muted">Chưa có lịch bảo trì.</p>
        )}
      </section>
      <section className="card">
        <h2>Phân tích và báo cáo</h2>
        <div className="actions">
          <button
            onClick={() =>
              run(async () =>
                setReports({
                  revenue: await api("/reports/revenue"),
                  quarterly: await api("/reports/revenue-quarterly"),
                  history: await api("/reports/occupancy-history"),
                  debts: await api("/reports/debts"),
                  occupancy: await api("/reports/occupancy"),
                  maintenance: await api("/reports/maintenance"),
                }),
              )
            }
          >
            Xem báo cáo
          </button>
          <button
            onClick={() => run(() => download("/reports/pdf", "bao-cao.pdf"))}
          >
            Tải PDF
          </button>
          <button
            onClick={() =>
              run(() => download("/reports/debts.csv", "cong-no.csv"))
            }
          >
            Xuất công nợ CSV
          </button>
        </div>
        {reports && (
          <>
            <h2>Doanh thu theo kỳ</h2>
            {reports.revenue.map((r) => (
              <div className="metric" key={r.billing_period}>
                <span>{r.billing_period}</span>
                <span>
                  Đã lập: {money(r.billed)}
                  <small>
                    Đã thu: {money(r.collected)} · Nợ: {money(r.outstanding)}
                  </small>
                </span>
              </div>
            ))}
            <h2>Doanh thu theo quý</h2>
            {reports.quarterly.map(r=><div className="metric" key={r.quarter}><span>{r.quarter}</span><span>Đã lập: {money(r.billed)}<small>Đã thu: {money(r.collected)} · Nợ: {money(r.outstanding)}</small></span></div>)}
            <h2>Tỷ lệ lấp đầy hiện tại</h2>
            {reports.occupancy.map((r) => (
              <div className="metric" key={r.id}>
                <span>{r.name}</span>
                <span>
                  {r.rented_rooms}/{r.total_rooms} phòng ·{" "}
                  {r.occupancy_percentage || 0}%
                </span>
              </div>
            ))}
            <h2>Lấp đầy cuối tháng (12 tháng gần nhất)</h2>
            {reports.history.map((r,i)=><div className="metric" key={i}><span>{r.period} · {r.name}</span><span>{r.occupied_rooms}/{r.total_rooms} phòng</span></div>)}
            <h2>Chi phí bảo trì</h2>
            {reports.maintenance.map((r, i) => (
              <div className="metric" key={i}>
                <span>
                  {r.period} · {labels[r.category] || r.category} · {labels[r.status]}
                </span>
                <span>
                  {r.total} sự cố · {money(r.cost)}
                </span>
              </div>
            ))}
          </>
        )}
      </section>
      {admin && (
        <section className="card">
          <h2>Nhật ký và chính sách hóa đơn</h2>
          <div className="actions">
            <button
              onClick={() =>
                run(async () => setAuditLogs(await api("/audit-logs")))
              }
            >
              Xem nhật ký thao tác
            </button>
            {d.properties.map((p) => (
              <button
                key={p.id}
                onClick={() =>
                  showForm(
                    "Chính sách hóa đơn · " + p.name,
                    [
                      f("meterCutoffDay", "Ngày chốt số (1–31)", "number"),
                      f(
                        "invoiceIssueDay",
                        "Ngày phát hành tháng sau (1–28)",
                        "number",
                      ),
                      f("lateFeeEnabled", "Bật phí chậm thanh toán", "boolean"),
                      f("lateFeePercentage", "Tỷ lệ phí chậm (%)", "number"),
                    ],
                    "/properties/" + p.id + "/billing-policy",
                    "PUT",
                    {
                      meterCutoffDay: p.meter_cutoff_day,
                      invoiceIssueDay: p.invoice_issue_day,
                      lateFeeEnabled: p.late_fee_enabled,
                      lateFeePercentage: p.late_fee_percentage,
                    },
                  )
                }
              >
                Chính sách · {p.name}
              </button>
            ))}
          </div>
          {auditLogs && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Thời gian</th>
                    <th>Người thực hiện</th>
                    <th>Thao tác</th>
                    <th>Dữ liệu</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.map((a) => (
                    <tr key={a.id}>
                      <td>{new Date(a.created_at).toLocaleString("vi-VN")}</td>
                      <td>{a.full_name}</td>
                      <td>{a.action}</td>
                      <td>
                        {a.resource_type} #{a.resource_id}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </>
  );
}

function CalendarPreview({contractId,period}) {
  const [calendar,setCalendar]=useState(null);
  useEffect(()=>{let active=true;setCalendar(null);if(contractId && period)api("/contracts/"+contractId+"/billing-calendar?period="+period).then(v=>{if(active)setCalendar(v);}).catch(()=>{});return()=>{active=false;};},[contractId,period]);
  return calendar?<p className="muted calendar-preview">Chốt số từ {calendar.cutoffDate} · Phát hành {calendar.issueDate} · Hạn thanh toán {calendar.dueDate}</p>:null;
}
function ProtectedImage({src,alt}) {
  const [url,setUrl]=useState(src.startsWith("/api/")?null:src);
  useEffect(()=>{let active=true,blob;setUrl(src.startsWith("/api/")?null:src);if(src.startsWith("/api/"))imageBlob(src).then(v=>{blob=v;if(active)setUrl(v);else URL.revokeObjectURL(v);}).catch(()=>{});return()=>{active=false;if(blob)URL.revokeObjectURL(blob);};},[src]);
  return url?<a href={url} target="_blank" rel="noreferrer"><img src={url} alt={alt}/></a>:<p className="muted">Đang tải ảnh…</p>;
}
function ImageUpload({endpoint,reload}) {
  const [busy,setBusy]=useState(false),[drag,setDrag]=useState(false),[files,setFiles]=useState([]),[error,setError]=useState("");
  const send=async chosen=>{
    if(busy||!chosen.length)return;
    const list=chosen.map(file=>({file,status:"waiting",error:""}));setFiles(list);setBusy(true);setError("");
    const update=(index,status,error="")=>setFiles(v=>v.map((row,i)=>i===index?{...row,status,error}:row));
    let saved=false;
    try {
      for(let i=0;i<chosen.length;i++) {
        const file=chosen[i];update(i,"uploading");
        try {if(file.size>6*1024*1024)throw new Error("Ảnh vượt quá 6 MB");await upload(endpoint,file);saved=true;update(i,"done");}
        catch(error){update(i,"error",error.message);}
      }
      if(saved)await reload?.();
    }catch(err){setError(err.message);}finally{setBusy(false);}
  };
  return <div className="image-upload">
    <label className={"upload-zone"+(drag?" dragging":"")} onDragOver={e=>{e.preventDefault();if(!busy)setDrag(true);}} onDragLeave={()=>setDrag(false)} onDrop={e=>{e.preventDefault();setDrag(false);if(!busy)send(Array.from(e.dataTransfer.files));}}>
      <input className="visually-hidden" aria-label="Thêm ảnh từ máy" type="file" accept="image/png,image/jpeg" multiple disabled={busy} onChange={e=>{const chosen=Array.from(e.target.files);e.target.value="";send(chosen);}}/>
      <strong className="drag-instruction">{busy?"Đang tải ảnh lên…":drag?"Thả ảnh để tải lên":"Kéo thả ảnh vào đây"}</strong>
      <small>JPEG hoặc PNG, tối đa 6 MB/ảnh và 12 ảnh.</small><span className="upload-choice">{busy?"Đang tải lên…":"Chọn ảnh từ máy"}</span>
    </label>
    <div aria-live="polite">{files.map((row,i)=><div className="upload-row" key={i}><span className="upload-name">{row.file.name}<small>{row.status==="done"?"Đã tải xong":row.status==="uploading"?"Đang tải lên…":row.status==="error"?row.error:"Đang chờ"}</small></span>{row.status==="error" && !busy && <button onClick={()=>send([row.file])}>Thử lại</button>}</div>)}{error && <p className="banner error" role="alert">{error}</p>}</div>
  </div>;
}
function FormDialog({ spec: s, onClose, onSave }) {
  const [values, setValues] = useState(s.initial),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    const handler = (e) => {
      if (e.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [busy, onClose]);
  return (
    <div className="overlay">
      <section
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="form-title"
      >
        <div className="dialog-head">
          <h2 id="form-title">{s.title}</h2>
          <button aria-label="Đóng" onClick={onClose}>
            ×
          </button>
        </div>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (busy) return;
            setBusy(true);
            setError("");
            const result = { ...s.initial };
            s.fields.forEach((field) => {
              const v = values[field.name];
              result[field.name] =
                field.type === "boolean"
                  ? !!v
                  : field.type === "number" ||
                      (field.type === "select" &&
                        /Id$|^assignedTo$/.test(field.name))
                    ? v === "" || v == null
                      ? null
                      : Number(v)
                    : field.type === "urls"
                      ? (v || "")
                          .split("\n")
                          .map((x) => x.trim())
                          .filter(Boolean)
                      : field.optional &&
                          (field.type === "date" || field.type === "email") &&
                          !v
                        ? null
                        : (v ?? "");
            });
            try {
              await onSave(result);
            } catch (e) {
              setError(e.message);
              setBusy(false);
            }
          }}
        >
          <div className="form-body">
            {s.method === "DELETE" && (
              <p>
                Hóa đơn nháp sẽ bị xóa. Chỉ số điện nước được giữ lại để lập lại
                hóa đơn.
              </p>
            )}
            {s.endpoint === "/invoices" && <CalendarPreview contractId={values.contractId} period={values.period} />}
            {s.endpoint === "/meter-readings" && <CalendarPreview contractId={values.contractId} period={values.period} />}
            {s.fields.map((field) => (
              <label key={field.name}>
                {field.label}
                {!field.optional && <span className="required"> *</span>}
                {field.type === "select" ? (
                  <select
                    required={!field.optional}
                    value={values[field.name] ?? ""}
                    onChange={(e) =>
                      setValues({ ...values, ...(field.name==="propertyId"?{roomId:""}:{}), [field.name]: e.target.value })
                    }
                  >
                    <option value="">Chọn…</option>
                    {field.options.filter(([v])=>!field.dependsOn || field.source.find(x=>x.id===v)?.[field.parentKey]===Number(values[field.dependsOn])).map(([v, l]) => (
                      <option value={v} key={v}>
                        {l}
                      </option>
                    ))}
                  </select>
                ) : field.type === "boolean" ? (
                  <input
                    type="checkbox"
                    checked={!!values[field.name]}
                    onChange={(e) =>
                      setValues({ ...values, [field.name]: e.target.checked })
                    }
                  />
                ) : ["textarea", "urls"].includes(field.type) ? (
                  <textarea
                    required={!field.optional}
                    value={values[field.name] ?? ""}
                    onChange={(e) =>
                      setValues({ ...values, [field.name]: e.target.value })
                    }
                  />
                ) : (
                  <input
                    autoComplete={
                      field.type === "password" ? "new-password" : "off"
                    }
                    required={!field.optional}
                    type={field.type}
                    min={field.type === "number" ? 0 : undefined}
                    max={field.max}
                    minLength={field.minLength}
                    step={field.type === "number" ? "0.01" : undefined}
                    value={values[field.name] ?? ""}
                    onChange={(e) =>
                      setValues({ ...values, [field.name]: e.target.value })
                    }
                  />
                )}
              </label>
            ))}
            {error && (
              <p className="banner error" role="alert">
                {error}
              </p>
            )}
          </div>
          <div className="dialog-footer">
            <button type="button" onClick={onClose}>
              Hủy
            </button>
            <button
              className={s.method === "DELETE" ? "danger" : "primary"}
              aria-busy={busy}
              aria-disabled={busy}
              onClick={(e) => {
                if (busy) e.preventDefault();
              }}
            >
              {busy ? <RefreshCw className="spin" size={16} /> : null}
              {s.method === "DELETE" ? "Xóa nháp" : "Lưu"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
function Login({ onLogin }) {
  const [username, setUsername] = useState(""),
    [password, setPassword] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [reset, setReset] = useState(false);
  return (
    <div className="login">
      <section className="card login-card">
        <div className="brand">
          <Building2 size={28} />
          <span>
            Nhà Trọ<small>Quản lý minh bạch</small>
          </span>
        </div>
        <h1>{reset ? "Đặt lại mật khẩu" : "Đăng nhập"}</h1>
        <p className="muted">
          {reset
            ? "Nhận mã xác nhận từ chủ nhà sau khi xác minh tài khoản."
            : "Theo dõi phòng, hợp đồng và từng khoản trong hóa đơn."}
        </p>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (busy) return;
            setBusy(true);
            setError("");
            try {
              if (reset) {
                await api("/auth/reset-password", "POST", {
                  code: username,
                  newPassword: password,
                });
                setReset(false);
                setUsername("");
                setPassword("");
                setError("Đã đặt lại mật khẩu. Hãy đăng nhập.");
              } else
                onLogin(
                  await api("/auth/login", "POST", { username, password }),
                );
            } catch (e) {
              setError(e.message);
            } finally {
              setBusy(false);
            }
          }}
        >
          <label>
            {reset ? "Mã đặt lại" : "Tên đăng nhập"}
            <input
              required
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>
          <label>
            {reset ? "Mật khẩu mới" : "Mật khẩu"}
            <input
              required
              minLength={reset ? 12 : undefined}
              type="password"
              autoComplete={reset ? "new-password" : "current-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          {error && (
            <p className="banner error" role="alert">
              {error}
            </p>
          )}
          <button
            className="primary full"
            aria-busy={busy}
            aria-disabled={busy}
            onClick={(e) => {
              if (busy) e.preventDefault();
            }}
          >
            {busy && <RefreshCw className="spin" size={16} />}{" "}
            {reset ? "Đặt lại mật khẩu" : "Đăng nhập"}
          </button>
          <button
            type="button"
            className="full"
            onClick={() => {
              setReset(!reset);
              setError("");
            }}
          >
            {reset ? "Quay lại đăng nhập" : "Quên mật khẩu"}
          </button>
        </form>
        <div className="login-note">
          <ShieldCheck size={18} />
          <span>Dữ liệu được bảo vệ theo vai trò và phạm vi quản lý.</span>
        </div>
      </section>
    </div>
  );
}
createRoot(document.getElementById("root")).render(<App />);
