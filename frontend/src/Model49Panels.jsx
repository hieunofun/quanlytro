import React, { useState, useEffect } from "react";
import { api, download, getSession } from "./api";
import {
  Download,
  AlertTriangle,
  Check,
  RefreshCw,
  Plus,
  Trash2,
  ShieldAlert,
  FileText,
  ChevronDown,
  ChevronRight,
  BarChart3,
  CreditCard,
  Package,
} from "lucide-react";

const field = (name, label, type = "text", extra = {}) => ({
  name,
  label,
  type,
  ...extra,
});
const money = (n) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(n || 0);
const label = (r) =>
  r.full_name ||
  r.name ||
  r.room_code ||
  r.contract_code ||
  r.meter_code ||
  r.asset_code ||
  r.reservation_code ||
  r.title ||
  r.fund_code ||
  `#${r.id}`;

export function Contract49Panel({ contract: c, tenant, showForm, run, reload }) {
  const [holds, setHolds] = useState({});
  const endpoint = "/contracts/" + c.id;

  const receipt = () =>
    run(async () => {
      const accounts = await api("/cash-accounts");
      showForm(
        "Ghi tiền thực nhận",
        [
          field("cashAccountId", "Quỹ hoặc tài khoản nhận", "select", {
            options: accounts
              .filter((a) => a.is_active && (!a.property_id || a.property_id === c.property_id))
              .map((a) => [a.id, a.name]),
          }),
          field("fundType", "Phân loại tiền", "select", {
            options: [
              ["DEPOSIT", "Cọc"],
              ["ADVANCE", "Trả trước"],
            ],
          }),
          field("amount", "Tiền thực nhận", "number"),
          field("occurredAt", "Thời điểm nhận (giờ Việt Nam)", "datetime-local"),
          field("code", "Mã phiếu thu"),
          field("confirmed", "Đã kiểm tra tiền thực vào quỹ", "boolean", { optional: true }),
          field("note", "Ghi chú", "textarea", { optional: true }),
        ],
        "/cash-transactions",
        "POST",
        { contractId: c.id, code: "RC-" + Date.now() },
        reload
      );
    });

  const refund = (f) =>
    run(async () => {
      const accounts = await api("/cash-accounts");
      showForm(
        "Ghi nhận tiền đã hoàn",
        [
          field("cashAccountId", "Quỹ hoặc tài khoản chi", "select", {
            options: accounts.filter((a) => a.is_active).map((a) => [a.id, a.name]),
          }),
          field("amount", "Số tiền hoàn thực tế", "number", { max: f.available }),
          field("occurredAt", "Thời điểm chi (giờ Việt Nam)", "datetime-local"),
          field("code", "Mã phiếu chi"),
          field("paid", "Đã chi tiền thành công", "boolean", { optional: true }),
          field("reason", "Căn cứ hoàn tiền", "textarea"),
        ],
        "/fund-accounts/" + f.id + "/refund",
        "POST",
        { code: "RF-" + Date.now() },
        reload
      );
    });

  const createHandover = (type = "MOVE_IN") => {
    showForm(
      type === "MOVE_IN" ? "Tạo biên bản nhận phòng & kiểm kê bàn giao" : "Tạo biên bản trả phòng & đối chiếu đồ đạc",
      [
        field("handoverType", "Loại biên bản", "select", {
          options: [
            ["MOVE_IN", "Nhận phòng (MOVE_IN)"],
            ["MOVE_OUT", "Trả phòng (MOVE_OUT)"],
          ],
        }),
        field("occurredAt", "Thời điểm bàn giao thực tế", "datetime-local"),
        field("autoPopulateItems", "Tự động nạp đồ đạc hiện có trong phòng", "boolean"),
        field("note", "Ghi chú bàn giao", "textarea", { optional: true }),
      ],
      endpoint + "/handovers",
      "POST",
      {
        handoverType: type,
        occurredAt: new Date().toISOString().slice(0, 16),
        autoPopulateItems: true,
      },
      reload
    );
  };

  const confirmHandover = (hId, code) => {
    showForm(
      `Xác nhận hoàn tất biên bản ${code}`,
      [field("note", "Ghi chú xác nhận", "textarea", { optional: true })],
      `/handovers/${hId}/confirm`,
      "POST",
      {},
      reload
    );
  };

  const addHandoverItem = (hId) => {
    showForm(
      "Bổ sung đồ đạc vào biên bản",
      [
        field("itemName", "Tên trang thiết bị / đồ đạc"),
        field("quantity", "Số lượng", "number", { min: 1 }),
        field("conditionNote", "Hiện trạng ghi nhận (VD: Mới 100%, Trầy xước nhẹ...)"),
        field("note", "Ghi chú thêm", "textarea", { optional: true }),
      ],
      `/handovers/${hId}/items`,
      "POST",
      { quantity: 1, conditionNote: "Hoạt động tốt" },
      reload
    );
  };

  const damageCompensation = (hId, itemId, itemName) => {
    showForm(
      `Phát sinh bồi thường hư hỏng: ${itemName}`,
      [
        field("amount", "Chi phí bồi thường / sửa chữa (VNĐ)", "number", { min: 1000 }),
        field("issueDate", "Ngày phát hành hóa đơn", "date"),
        field("dueDate", "Hạn nộp tiền bồi thường", "date"),
        field("note", "Lý do và căn cứ bồi thường", "textarea"),
      ],
      `/handovers/${hId}/damage-compensation`,
      "POST",
      {
        handoverItemId: itemId,
        issueDate: new Date().toISOString().slice(0, 10),
        dueDate: new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10),
        amount: 200000,
      },
      reload
    );
  };

  return (
    <>
      <section className="card">
        <div className="handover-card-header">
          <div>
            <h2>Điều khoản hợp đồng & Văn bản</h2>
            <p className="muted">
              Ngày đóng tiền định kỳ: <strong>Ngày {c.payment_day || 5} hàng tháng</strong> · Hình thức:{" "}
              <strong>{c.collection_timing === "IN_ARREARS" ? "Thu cuối kỳ (Trả sau)" : "Thu đầu kỳ (Trả trước)"}</strong>
            </p>
          </div>
          <div className="actions">
            <button onClick={() => run(() => download(endpoint + "/pdf", `hop-dong-${c.contract_code}.pdf`))}>
              <Download size={16} /> Tải Hợp đồng đầy đủ (PDF)
            </button>
          </div>
        </div>

        {!!c.versions?.length && (
          <div style={{ marginTop: 12 }}>
            <h3>Lịch sử phiên bản điều khoản hợp đồng</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Bản</th>
                    <th>Thời hạn hiệu lực</th>
                    <th>Giá thuê</th>
                    <th>Cọc quy định</th>
                    <th>Ngày thu / Hình thức</th>
                    <th>Ngày ký xác nhận</th>
                    <th>Tài liệu đính kèm</th>
                  </tr>
                </thead>
                <tbody>
                  {c.versions.map((v) => (
                    <tr key={v.id}>
                      <td><strong>Số {v.version_no}</strong></td>
                      <td>{v.effective_from} → {v.lease_end}</td>
                      <td><strong>{money(v.rental_price)}</strong></td>
                      <td>{money(v.deposit_required)}</td>
                      <td>
                        Ngày {v.payment_day || 5} (
                        {v.collection_timing === "IN_ARREARS" ? "Trả sau" : "Trả trước"})
                      </td>
                      <td>{v.signed_at ? new Date(v.signed_at).toLocaleString("vi-VN") : "Chưa ghi nhận ngày ký"}</td>
                      <td>
                        {v.files?.length ? (
                          v.files.map((fl) => (
                            <a key={fl.id} href={fl.url} target="_blank" rel="noreferrer" style={{ display: "block" }}>
                              {fl.original_name || "Tệp PDF"}
                            </a>
                          ))
                        ) : (
                          <span className="muted">Hợp đồng điện tử</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      <section className="card">
        <div className="handover-card-header">
          <div>
            <h2>Biên bản bàn giao & Kiểm kê đồ đạc phòng</h2>
            <p className="muted">
              Biên bản bàn giao khi nhận phòng (MOVE_IN) và trả phòng (MOVE_OUT), ghi nhận từng đồ vật, hiện trạng và phát sinh bồi thường.
            </p>
          </div>
          {!tenant && (
            <div className="actions">
              <button className="primary" onClick={() => createHandover("MOVE_IN")}>
                <Plus size={16} /> Biên bản nhận phòng (MOVE_IN)
              </button>
              <button onClick={() => createHandover("MOVE_OUT")}>
                <Package size={16} /> Biên bản trả phòng (MOVE_OUT)
              </button>
            </div>
          )}
        </div>

        {c.handovers?.length ? (
          c.handovers.map((h) => (
            <div key={h.id} className="handover-card">
              <div className="handover-card-header">
                <div>
                  <strong>
                    {h.handover_type === "MOVE_IN"
                      ? "📦 BIÊN BẢN GIAO PHÒNG KHI VÀO Ở"
                      : "🚪 BIÊN BẢN BÀN GIAO TRẢ PHÒNG"}
                    {" "}— {h.handover_code}
                  </strong>
                  <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                    Thời điểm: {new Date(h.occurred_at).toLocaleString("vi-VN")} · Người nhận:{" "}
                    <strong>{h.received_by_name || "Khách thuê"}</strong> · Trạng thái:{" "}
                    <span className={h.status === "CONFIRMED" ? "badge active" : "badge"}>
                      {h.status === "CONFIRMED" ? "Đã xác nhận" : "Bản nháp (DRAFT)"}
                    </span>
                  </div>
                </div>
                {!tenant && (
                  <div className="actions">
                    {h.status === "DRAFT" && (
                      <>
                        <button onClick={() => addHandoverItem(h.id)}>+ Thêm đồ</button>
                        <button className="primary" onClick={() => confirmHandover(h.id, h.handover_code)}>
                          <Check size={16} /> Xác nhận biên bản
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>

              {h.items?.length ? (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>STT</th>
                        <th>Tên đồ đạc / Trang thiết bị</th>
                        <th>Số lượng</th>
                        <th>Hiện trạng lúc kiểm kê</th>
                        <th>Ghi chú</th>
                        {!tenant && <th>Xử lý hư hỏng</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {h.items.map((it) => (
                        <tr key={it.id}>
                          <td>{it.line_no}</td>
                          <td><strong>{it.item_name_snapshot}</strong></td>
                          <td>{it.quantity}</td>
                          <td>
                            <span className="item-badge">{it.condition_note || "Bình thường"}</span>
                          </td>
                          <td>{it.note || "—"}</td>
                          {!tenant && (
                            <td>
                              <button
                                className="danger"
                                style={{ fontSize: 12, padding: "4px 8px", minHeight: 28 }}
                                onClick={() => damageCompensation(h.id, it.id, it.item_name_snapshot)}
                              >
                                Phạt bồi thường
                              </button>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="muted" style={{ padding: 8 }}>Chưa có dòng đồ đạc nào trong biên bản này.</p>
              )}
            </div>
          ))
        ) : (
          <p className="muted">Chưa có biên bản bàn giao phòng nào được lập. Bấm nút phía trên để tạo biên bản nhận phòng.</p>
        )}
      </section>

      <section className="card">
        <h2>Quỹ cọc, tiền trả trước & Quyền giao phòng</h2>
        <p className="muted">
          Cọc cam kết {money(c.deposit_amount)}. Chỉ phiếu thu đã xác nhận mới tạo tiền giữ; phân bổ hoặc cấn cọc không tạo thêm doanh thu tiền mặt.
        </p>
        {!tenant && (
          <div className="actions">
            {["DRAFT", "PENDING"].includes(c.status) && (
              <button
                onClick={() =>
                  showForm(
                    "Ghi nhận hợp đồng đã ký",
                    [field("signedAt", "Ngày giờ thực tế ký (giờ Việt Nam)", "datetime-local")],
                    endpoint + "/activate",
                    "POST",
                    {},
                    reload
                  )
                }
              >
                Ghi nhận đã ký
              </button>
            )}
            {c.status === "ACTIVE" && !c.possessions?.some((p) => !p.returned_at) && (
              <button
                onClick={() =>
                  showForm(
                    "Giao phòng thực tế",
                    [
                      field("takenAt", "Thời điểm giao (giờ Việt Nam)", "datetime-local"),
                      field("reason", "Căn cứ giao, hoặc căn cứ cho thiếu cọc", "textarea", { optional: true }),
                    ],
                    endpoint + "/possessions",
                    "POST",
                    {},
                    reload
                  )
                }
              >
                Giao phòng
              </button>
            )}
            <button onClick={receipt}>Thu cọc / trả trước</button>
            {getSession().user.role === "ADMIN" && c.reconciliation_status === "PENDING" && (
              <button
                onClick={() =>
                  showForm(
                    "Xác minh hợp đồng chuyển từ dữ liệu cũ",
                    [
                      field("confirmed", "Đã kiểm tra hồ sơ gốc", "boolean"),
                      field("reason", "Kết quả đối soát", "textarea"),
                    ],
                    endpoint + "/reconcile",
                    "POST",
                    {},
                    reload
                  )
                }
              >
                Đối soát hồ sơ cũ
              </button>
            )}
            <button
              onClick={() =>
                showForm(
                  "Ghi đề nghị trả phòng",
                  [field("requestedDate", "Ngày đề nghị trả", "date"), field("reason", "Lý do", "textarea")],
                  endpoint + "/move-out-notices",
                  "POST",
                  {},
                  reload
                )
              }
            >
              Đề nghị trả
            </button>
          </div>
        )}
        {c.possessions?.map((p) => (
          <p key={p.id}>
            Đã nhận phòng lúc {new Date(p.taken_at).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })}
            {p.returned_at
              ? " · Đã trả " + new Date(p.returned_at).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })
              : " · Chưa giao trả"}
          </p>
        ))}
        {c.funds?.map((f) => (
          <article className="metric" key={f.id}>
            <div>
              <strong>{f.fund_type === "DEPOSIT" ? "Tiền cọc" : "Tiền trả trước"}</strong>
              <small>
                Số dư {money(f.balance)} · Tạm giữ {money(f.held_amount)} · Có thể dùng {money(f.available)}
                {f.reconciliation_status === "PENDING" ? " · Chưa đối soát" : ""}
              </small>
            </div>
            {!tenant && (
              <div className="actions">
                {f.reconciliation_status === "VERIFIED" && Number(f.available) > 0 && (
                  <button onClick={() => refund(f)}>Ghi đã hoàn tiền</button>
                )}
                {getSession().user.role === "ADMIN" && f.reconciliation_status === "PENDING" && (
                  <button
                    onClick={() =>
                      showForm(
                        "Xác nhận số dư có bằng chứng",
                        [
                          field("verifiedBalance", "Số dư cọc thực còn giữ", "number"),
                          field("reason", "Biên nhận / xác nhận đối soát", "textarea"),
                        ],
                        "/fund-accounts/" + f.id + "/reconcile",
                        "POST",
                        {},
                        reload
                      )
                    }
                  >
                    Đối soát số dư
                  </button>
                )}
                <button
                  onClick={() =>
                    showForm(
                      "Tạm giữ một phần để đối soát",
                      [
                        field("amount", "Số tiền tạm giữ", "number"),
                        field("reviewOn", "Ngày phải rà soát lại", "date"),
                        field("reason", "Khoản chưa rõ và căn cứ tạm giữ", "textarea"),
                      ],
                      "/fund-accounts/" + f.id + "/holds",
                      "POST",
                      {},
                      reload
                    )
                  }
                >
                  Tạm giữ
                </button>
                <button
                  onClick={() =>
                    run(async () => {
                      const data = await api("/fund-accounts/" + f.id + "/holds");
                      setHolds((v) => ({ ...v, [f.id]: data }));
                    })
                  }
                >
                  Xem khoản giữ
                </button>
              </div>
            )}
          </article>
        ))}
        {!tenant &&
          Object.values(holds)
            .flat()
            .map((h) => (
              <p key={h.id}>
                {money(h.amount)} · {h.reason} · Rà lại {h.review_on}{" "}
                {h.released_at ? (
                  "· Đã giải tỏa"
                ) : (
                  <button
                    onClick={() =>
                      showForm(
                        "Giải tỏa khoản tạm giữ",
                        [field("reason", "Kết quả đối soát", "textarea")],
                        "/fund-holds/" + h.id + "/release",
                        "POST",
                        {},
                        reload
                      )
                    }
                  >
                    Giải tỏa
                  </button>
                )}
              </p>
            ))}
      </section>
    </>
  );
}

export function RoomAssetsPanel({ roomId, showForm, run, reload }) {
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const data = await api("/rooms/" + roomId + "/assets");
      setAssets(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [roomId]);

  const addAsset = () =>
    run(async () => {
      const types = await api("/catalog/asset-types");
      showForm(
        "Thêm trang thiết bị vào phòng",
        [
          field("assetTypeId", "Loại đồ đạc", "select", {
            options: types.map((t) => [t.id, `${t.name} (${t.unit_name})`]),
          }),
          field("assetCode", "Mã thiết bị / tem kiểm kê"),
          field("quantity", "Số lượng", "number", { min: 1 }),
          field("note", "Hiện trạng / Mô tả", "textarea", { optional: true }),
        ],
        "/rooms/" + roomId + "/assets",
        "POST",
        { quantity: 1 },
        () => {
          load();
          reload?.();
        }
      );
    });

  const removeAsset = (placementId, assetName) => {
    showForm(
      `Gỡ "${assetName}" khỏi phòng`,
      [field("reason", "Lý do tháo gỡ / di dời", "textarea", { optional: true })],
      `/rooms/${roomId}/assets/${placementId}/remove`,
      "POST",
      {},
      () => {
        load();
        reload?.();
      }
    );
  };

  return (
    <section className="card">
      <div className="handover-card-header">
        <div>
          <h2>Đồ đạc & Tài sản trong phòng</h2>
          <p className="muted">
            Quản lý trang thiết bị, số lượng và hiện trạng đồ đạc bố trí trong phòng phục vụ bàn giao và kiểm kê.
          </p>
        </div>
        <button onClick={addAsset} className="primary">
          <Plus size={16} /> Thêm đồ đạc
        </button>
      </div>

      {loading ? (
        <p className="muted">Đang tải danh mục đồ đạc...</p>
      ) : assets.length === 0 ? (
        <p className="muted" style={{ padding: "14px 0" }}>
          Chưa có trang thiết bị nào được ghi nhận bố trí trong phòng này.
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Loại đồ đạc</th>
                <th>Mã thiết bị</th>
                <th>Số lượng</th>
                <th>Đơn vị</th>
                <th>Ngày bố trí</th>
                <th>Hiện trạng / Ghi chú</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {assets.map((a) => (
                <tr key={a.placement_id}>
                  <td><strong>{a.asset_type_name}</strong></td>
                  <td><code>{a.asset_code}</code></td>
                  <td><strong>{a.quantity}</strong></td>
                  <td>{a.unit_name || "Cái"}</td>
                  <td>{a.valid_from ? new Date(a.valid_from).toLocaleDateString("vi-VN") : "—"}</td>
                  <td>{a.asset_note || "Hoạt động tốt"}</td>
                  <td>
                    <button
                      className="danger"
                      onClick={() => removeAsset(a.placement_id, a.asset_type_name)}
                    >
                      Gỡ bỏ
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export function DebtsPanel({ showForm, run, navigate }) {
  const [debts, setDebts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [expanded, setExpanded] = useState({});

  const load = async () => {
    setLoading(true);
    try {
      const data = await api("/reports/tenant-debts");
      setDebts(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = debts.filter((d) => {
    const matchSearch =
      (d.full_name || "").toLowerCase().includes(search.toLowerCase()) ||
      (d.phone || "").includes(search) ||
      (d.room_codes || "").toLowerCase().includes(search.toLowerCase()) ||
      (d.contract_codes || "").toLowerCase().includes(search.toLowerCase());
    const matchOverdue = !overdueOnly || Number(d.max_overdue_days) > 0;
    return matchSearch && matchOverdue;
  });

  const totalDebt = debts.reduce((s, d) => s + Number(d.total_debt || 0), 0);
  const overdueCount = debts.filter((d) => Number(d.max_overdue_days) > 0).length;

  const toggleExpand = (id) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <section className="card">
      <div className="handover-card-header">
        <div>
          <h2>Quản lý Dư nợ Khách hàng</h2>
          <p className="muted">
            Theo dõi danh sách khách thuê đang mở dư nợ, số ngày quá hạn và chi tiết từng hóa đơn cần thu.
          </p>
        </div>
        <div className="actions">
          <button onClick={() => run(() => download("/reports/debts.csv", "cong-no-khach-hang.csv"))}>
            <Download size={16} /> Xuất CSV
          </button>
          <button onClick={load}><RefreshCw size={16} /> Làm mới</button>
        </div>
      </div>

      <div className="stats" style={{ marginBottom: 20 }}>
        <div className="card stat">
          <div><span className="muted">Tổng dư nợ toàn hệ thống</span></div>
          <strong style={{ color: "#dc2626" }}>{money(totalDebt)}</strong>
        </div>
        <div className="card stat">
          <div><span className="muted">Khách hàng đang nợ</span></div>
          <strong>{debts.length} người</strong>
        </div>
        <div className="card stat">
          <div><span className="muted">Khách nợ quá hạn</span></div>
          <strong style={{ color: overdueCount > 0 ? "#dc2626" : "inherit" }}>
            {overdueCount} người
          </strong>
        </div>
      </div>

      <div className="toolbar">
        <label className="search" style={{ flex: 1 }}>
          <input
            placeholder="Tìm theo tên khách, SĐT, số phòng..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        <label style={{ display: "inline-flex", alignItems: "center", gap: 8, cursor: "pointer" }}>
          <input
            type="checkbox"
            checked={overdueOnly}
            onChange={(e) => setOverdueOnly(e.target.checked)}
          />
          Chỉ xem khách quá hạn
        </label>
      </div>

      {loading ? (
        <p className="muted">Đang tải danh sách công nợ...</p>
      ) : filtered.length === 0 ? (
        <p className="muted" style={{ padding: "20px 0", textAlign: "center" }}>
          Không có khách hàng nào còn dư nợ theo bộ lọc.
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Khách thuê</th>
                <th>Phòng / Hợp đồng</th>
                <th>Tổng dư nợ</th>
                <th>Số HĐ nợ</th>
                <th>Hạn nợ sớm nhất</th>
                <th>Tình trạng quá hạn</th>
                <th>Chi tiết / Thu tiền</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => (
                <React.Fragment key={t.tenant_id}>
                  <tr>
                    <td>
                      <strong>{t.full_name}</strong>
                      <br />
                      <small className="muted">{t.phone || "—"} {t.identity_number ? `· ${t.identity_number}` : ""}</small>
                    </td>
                    <td>
                      <span>{t.room_codes || "—"}</span>
                      <br />
                      <small className="muted">{t.contract_codes || ""}</small>
                    </td>
                    <td>
                      <strong style={{ color: "#dc2626" }}>{money(t.total_debt)}</strong>
                      <br />
                      <small className="muted">Đã thu: {money(t.total_paid)} / {money(t.total_billed)}</small>
                    </td>
                    <td>{t.unpaid_invoice_count}</td>
                    <td>{t.earliest_due_date || "—"}</td>
                    <td>
                      {Number(t.max_overdue_days) > 0 ? (
                        <span className="badge-overdue">Quá hạn {t.max_overdue_days} ngày</span>
                      ) : (
                        <span className="badge">Trong hạn</span>
                      )}
                    </td>
                    <td>
                      <button onClick={() => toggleExpand(t.tenant_id)}>
                        {expanded[t.tenant_id] ? "Ẩn hóa đơn" : `Xem ${t.invoices?.length || 0} HĐ nợ`}
                      </button>
                    </td>
                  </tr>
                  {expanded[t.tenant_id] && (
                    <tr>
                      <td colSpan={7} style={{ background: "var(--surface-hover)", padding: 12 }}>
                        <div className="debt-invoice-box">
                          <strong>Danh sách hóa đơn chưa thanh toán đủ của {t.full_name}:</strong>
                          <table style={{ marginTop: 8, width: "100%" }}>
                            <thead>
                              <tr>
                                <th>Mã hóa đơn</th>
                                <th>Kỳ</th>
                                <th>Phòng</th>
                                <th>Hạn thanh toán</th>
                                <th>Tổng tiền</th>
                                <th>Đã trả</th>
                                <th>Còn nợ</th>
                                <th>Quá hạn</th>
                                <th>Hành động</th>
                              </tr>
                            </thead>
                            <tbody>
                              {t.invoices?.map((inv) => (
                                <tr key={inv.id}>
                                  <td><strong>{inv.invoice_code}</strong></td>
                                  <td>{inv.billing_period || "—"}</td>
                                  <td>{inv.room_code}</td>
                                  <td>{inv.due_date}</td>
                                  <td>{money(inv.total_amount)}</td>
                                  <td>{money(inv.paid_amount)}</td>
                                  <td><strong style={{ color: "#dc2626" }}>{money(inv.balance_due)}</strong></td>
                                  <td>
                                    {Number(inv.overdue_days) > 0 ? (
                                      <span className="badge-overdue">{inv.overdue_days} ngày</span>
                                    ) : (
                                      <span className="badge">Trong hạn</span>
                                    )}
                                  </td>
                                  <td>
                                    <button
                                      className="primary"
                                      onClick={() =>
                                        showForm(
                                          "Thu tiền hóa đơn " + inv.invoice_code,
                                          [
                                            field("code", "Mã giao dịch"),
                                            field("amount", "Số tiền thực nhận", "number"),
                                            field("occurredAt", "Thời điểm thực nhận", "datetime-local"),
                                            field("method", "Phương thức", "select", {
                                              options: [
                                                ["CASH", "Tiền mặt"],
                                                ["BANK_TRANSFER_MANUAL", "Chuyển khoản"],
                                                ["OTHER", "Khác"],
                                              ],
                                            }),
                                            field("notes", "Ghi chú", "textarea", { optional: true }),
                                          ],
                                          "/invoices/" + inv.id + "/payments",
                                          "POST",
                                          {
                                            code: "PAY-" + Date.now(),
                                            amount: inv.balance_due,
                                            method: "CASH",
                                          },
                                          load
                                        )
                                      }
                                    >
                                      Thu tiền
                                    </button>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export function ReportsPanel({ run }) {
  const [tab, setTab] = useState("month");
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const endpoint =
        tab === "quarter"
          ? "/reports/revenue-quarterly"
          : tab === "year"
          ? "/reports/revenue-yearly"
          : "/reports/revenue";
      const rows = await api(endpoint);
      setData(rows);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [tab]);

  const totalBilled = data.reduce((s, r) => s + Number(r.billed || 0), 0);
  const totalCollected = data.reduce((s, r) => s + Number(r.collected || 0), 0);
  const totalOutstanding = data.reduce((s, r) => s + Number(r.outstanding || 0), 0);
  const overallRate = totalBilled > 0 ? ((totalCollected / totalBilled) * 100).toFixed(1) : 0;
  const maxVal = Math.max(1, ...data.map((r) => Math.max(Number(r.billed || 0), Number(r.collected || 0))));

  return (
    <section className="card">
      <div className="handover-card-header">
        <div>
          <h2>Báo cáo Doanh thu & Dòng tiền</h2>
          <p className="muted">
            Phân tích số liệu tài chính hóa đơn, phân loại thu nợ và dòng tiền theo thời gian linh hoạt (Tháng / Quý / Năm).
          </p>
        </div>
        <div className="actions">
          <div className="tabs-control">
            <button className={tab === "month" ? "active" : ""} onClick={() => setTab("month")}>
              Theo Tháng
            </button>
            <button className={tab === "quarter" ? "active" : ""} onClick={() => setTab("quarter")}>
              Theo Quý
            </button>
            <button className={tab === "year" ? "active" : ""} onClick={() => setTab("year")}>
              Theo Năm
            </button>
          </div>
          <button onClick={() => run(() => download("/reports/pdf", `bao-cao-${tab}.pdf`))}>
            <Download size={16} /> Xuất PDF
          </button>
          <button onClick={load}><RefreshCw size={16} /></button>
        </div>
      </div>

      <div className="stats" style={{ marginBottom: 20 }}>
        <div className="card stat">
          <div><span className="muted">Tổng hóa đơn phát hành</span></div>
          <strong>{money(totalBilled)}</strong>
        </div>
        <div className="card stat">
          <div><span className="muted">Thực tế đã thu</span></div>
          <strong style={{ color: "#059669" }}>{money(totalCollected)}</strong>
        </div>
        <div className="card stat">
          <div><span className="muted">Công nợ còn tồn</span></div>
          <strong style={{ color: "#dc2626" }}>{money(totalOutstanding)}</strong>
        </div>
        <div className="card stat">
          <div><span className="muted">Tỷ lệ thu nợ</span></div>
          <strong style={{ color: "#2563eb" }}>{overallRate}%</strong>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Biểu đồ Doanh thu (Lập hóa đơn vs Thực thu)</h3>
        {loading ? (
          <p className="muted">Đang tải dữ liệu biểu đồ...</p>
        ) : data.length === 0 ? (
          <p className="muted">Không có dữ liệu trong kỳ.</p>
        ) : (
          <div className="trend-chart">
            {data.map((r, i) => {
              const hBilled = Math.max(4, (Number(r.billed || 0) / maxVal) * 130);
              const hCollected = Math.max(4, (Number(r.collected || 0) / maxVal) * 130);
              return (
                <div key={i} className="chart-col">
                  <div className="chart-bars-wrap">
                    <div
                      className="chart-bar billed"
                      style={{ height: `${hBilled}px` }}
                      title={`Lập HĐ: ${money(r.billed)}`}
                    />
                    <div
                      className="chart-bar collected"
                      style={{ height: `${hCollected}px` }}
                      title={`Đã thu: ${money(r.collected)} (${r.collection_rate || 0}%)`}
                    />
                  </div>
                  <span className="chart-label">{r.billing_period || r.quarter || r.year}</span>
                </div>
              );
            })}
          </div>
        )}
        <div style={{ display: "flex", gap: 16, marginTop: 10, fontSize: 12, color: "var(--muted)" }}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
            <span style={{ width: 12, height: 12, background: "#cbd5e1", borderRadius: 2 }} /> Doanh số lập hóa đơn
          </span>
          <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
            <span style={{ width: 12, height: 12, background: "#3b82f6", borderRadius: 2 }} /> Thực tế thu được
          </span>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{tab === "quarter" ? "Quý" : tab === "year" ? "Năm" : "Kỳ / Tháng"}</th>
              <th>Tiền phòng</th>
              <th>Tiền điện</th>
              <th>Tiền nước</th>
              <th>Tổng lập HĐ</th>
              <th>Thu trước hạn</th>
              <th>Thu đúng hạn</th>
              <th>Thu sau hạn</th>
              <th>Tổng thực thu</th>
              <th>Còn nợ</th>
              <th>Tỷ lệ thu</th>
            </tr>
          </thead>
          <tbody>
            {data.map((r, i) => (
              <tr key={i}>
                <td><strong>{r.billing_period || r.quarter || r.year}</strong></td>
                <td>{money(r.room_rent)}</td>
                <td>{money(r.electricity)}</td>
                <td>{money(r.water)}</td>
                <td><strong>{money(r.billed)}</strong></td>
                <td style={{ color: "#059669" }}>{money(r.paid_early)}</td>
                <td style={{ color: "#2563eb" }}>{money(r.paid_on_time)}</td>
                <td style={{ color: "#d97706" }}>{money(r.paid_late)}</td>
                <td><strong style={{ color: "#059669" }}>{money(r.collected)}</strong></td>
                <td style={{ color: Number(r.outstanding) > 0 ? "#dc2626" : "inherit" }}>
                  {money(r.outstanding)}
                </td>
                <td>
                  <span className="badge" style={{ fontWeight: 600 }}>
                    {r.collection_rate ?? 0}%
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

const schemas = {
  reservations: ["Giữ chỗ", [["roomId", "Phòng", "rooms"], ["tenantId", "Người giữ chỗ", "tenants", true], ["reservationCode", "Mã giữ chỗ"], ["contactName", "Người liên hệ"], ["contactPhone", "Điện thoại"], ["startDate", "Ngày dự kiến vào", "date"], ["endDate", "Ngày hết khoảng giữ", "date"], ["expiresAt", "Hết thời hạn giữ (giờ Việt Nam)", "datetime-local"], ["conditions", "Điều kiện hoàn / mất tiền", "textarea"]]],
  "asset-types": ["Loại tài sản", [["code", "Mã loại"], ["name", "Tên loại"], ["unitName", "Đơn vị"]]],
  assets: ["Tài sản", [["assetTypeId", "Loại tài sản", "asset-types"], ["assetCode", "Mã tài sản"], ["quantity", "Số lượng", "number"], ["note", "Ghi chú", "textarea", true]]],
  "asset-placements": ["Bố trí tài sản", [["assetId", "Tài sản", "assets"], ["roomId", "Phòng (chọn phòng hoặc khu)", "rooms", true], ["propertyId", "Khu vực chung", "properties", true], ["validFrom", "Bắt đầu bố trí", "datetime-local"]]],
  meters: ["Công tơ vật lý", [["roomId", "Phòng", "rooms"], ["serviceItemId", "Dịch vụ", "services"], ["meterCode", "Mã công tơ"], ["installedAt", "Thời điểm lắp (giờ Việt Nam)", "datetime-local"]]],
  subscriptions: ["Đăng ký dịch vụ", [["contractId", "Hợp đồng", "contracts"], ["serviceItemId", "Dịch vụ", "services"], ["effectiveFrom", "Bắt đầu tính", "date"], ["effectiveTo", "Ngừng tính", "date", true], ["quantity", "Số lượng đăng ký", "number"]]],
  "maintenance-schedules": ["Lịch bảo trì", [["roomId", "Phòng", "rooms", true], ["propertyId", "Khu vực chung", "properties", true], ["title", "Nội dung"], ["intervalDays", "Lặp sau số ngày", "number"], ["nextDue", "Lần đến hạn", "date"]]],
  handovers: ["Biên bản bàn giao", [["contractId", "Hợp đồng", "contracts"], ["handoverCode", "Mã biên bản"], ["handoverType", "Loại: MOVE_IN / MOVE_OUT / RESPONSIBILITY"], ["occurredAt", "Thời điểm thực tế (giờ Việt Nam)", "datetime-local"], ["note", "Nội dung", "textarea"]]],
  "handover-items": ["Chi tiết bàn giao", [["handoverId", "Biên bản", "handovers"], ["lineNo", "Số thứ tự", "number"], ["itemNameSnapshot", "Tên tài sản tại lúc giao"], ["quantity", "Số lượng", "number"], ["conditionNote", "Hiện trạng ghi nhận", "textarea"]]],
  "staff-permissions": ["Ủy quyền nhân viên", [["assignmentId", "Phân công", "assignments"], ["actionCode", "Quyền: REFUND / TRANSFER / ADJUST / DEPOSIT_OFFSET / WAIVE_DEPOSIT"], ["amountLimit", "Hạn mức mỗi thao tác", "number", true], ["expiresAt", "Hết hạn ủy quyền", "datetime-local", true]]],
};

export function Workflow49Panel({ showForm, run }) {
  const [resource, setResource] = useState("meters");
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const admin = getSession().user.role === "ADMIN";

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      setRows(await api("/catalog/" + resource));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [resource]);

  const create = () =>
    run(async () => {
      const definition = schemas[resource][1];
      const fields = [];
      for (const [name, title, type = "text", optional = false] of definition) {
        if (
          schemas[type] ||
          ["rooms", "properties", "services", "contracts", "tenants", "assignments"].includes(type)
        ) {
          const list = await api(type === "contracts" ? "/contracts" : "/catalog/" + type);
          fields.push(field(name, title, "select", { optional, options: list.map((r) => [r.id, label(r)]) }));
        } else {
          fields.push(field(name, title, type, { optional }));
        }
      }
      showForm("Thêm " + schemas[resource][0].toLowerCase(), fields, "/catalog/" + resource, "POST", {}, load);
    });

  return (
    <section className="card">
      <h2>Vận hành</h2>
      <div className="toolbar">
        <label>
          Nhóm nghiệp vụ{" "}
          <select value={resource} onChange={(e) => setResource(e.target.value)}>
            {Object.entries(schemas)
              .filter(([key]) => admin || !["asset-types", "assets", "staff-permissions"].includes(key))
              .map(([key, [name]]) => (
                <option key={key} value={key}>
                  {name}
                </option>
              ))}
          </select>
        </label>
        <button onClick={create}>Thêm bản ghi</button>
        <button onClick={load}>Làm mới</button>
      </div>
      {error && <p className="banner error" role="alert">{error}</p>}
      {loading ? (
        <p role="status">Đang tải…</p>
      ) : rows.length === 0 ? (
        <p className="muted">Chưa có bản ghi.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Mã / nội dung</th>
                <th>Thời gian / trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>{label(r)}</td>
                  <td>{r.status || r.next_due || r.effective_from || r.installed_at || r.valid_from || "—"}</td>
                  <td>
                    {resource === "meters" && (
                      <button
                        onClick={() =>
                          showForm(
                            "Thay công tơ",
                            [
                              field("replacedAt", "Thời điểm thay", "datetime-local"),
                              field("closingIndex", "Chỉ số chốt công tơ cũ", "number"),
                              field("newMeterCode", "Mã công tơ mới"),
                              field("openingIndex", "Chỉ số đầu công tơ mới", "number"),
                              field("reason", "Căn cứ thay", "textarea"),
                            ],
                            "/meters/" + r.id + "/replace",
                            "POST",
                            {},
                            load
                          )
                        }
                      >
                        Thay công tơ
                      </button>
                    )}
                    {resource === "maintenance-schedules" && (
                      <button
                        onClick={() =>
                          showForm("Tạo phiếu bảo trì cho lần đến hạn", [], "/maintenance-schedules/" + r.id + "/complete", "POST", {}, load)
                        }
                      >
                        Tạo phiếu đến hạn
                      </button>
                    )}
                    {resource === "handovers" && r.status === "DRAFT" && (
                      <button
                        onClick={() =>
                          showForm(
                            "Xác nhận biên bản",
                            [
                              field("confirmedAt", "Thời điểm xác nhận", "datetime-local"),
                              field("reason", "Căn cứ xác nhận", "textarea"),
                            ],
                            "/catalog/handovers/" + r.id,
                            "PUT",
                            { status: "CONFIRMED" },
                            load
                          )
                        }
                      >
                        Xác nhận
                      </button>
                    )}
                    {resource === "reservations" && (
                      <button
                        onClick={() =>
                          showForm("Mở quỹ theo dõi tiền giữ chỗ", [], "/reservations/" + r.id + "/fund", "POST", {}, load)
                        }
                      >
                        Mở quỹ giữ chỗ
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="muted">Giữ chỗ và biên bản không tự ghi thu/chi. Thông tin đã ký, phát hành hoặc xác nhận được giữ lịch sử.</p>
    </section>
  );
}
