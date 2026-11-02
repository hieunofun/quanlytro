package com.hostel.model49;

import com.hostel.security.Access;
import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;
import static com.hostel.service.Database.*;

@Component
@Profile("model49")
public class Access49 {
    private final Database db;
    private final Access identity;

    public Access49(Database db, Access identity) {
        this.db = db;
        this.identity = identity;
    }

    public long user() {
        return identity.userId();
    }

    public String role() {
        return identity.role();
    }

    public void admin() {
        identity.admin();
    }

    public void manager() {
        identity.manager();
    }

    /** Điều kiện SQL giới hạn các phòng mà tài khoản hiện tại được truy cập. */
    public String roomScope(String column) {
        if (role().equals("ADMIN")) {
            return "true";
        }
        if (role().equals("STAFF")) {
            return column
                    + " in (select r.id from rooms r where can_manage_room("
                    + user()
                    + ",r.id))";
        }
        return column
                + " in (select c.room_id from contracts c where "
                + contractScope("c", false)
                + ")";
    }

    public String propertyScope(String column) {
        if (role().equals("ADMIN")) {
            return "true";
        }
        if (!role().equals("STAFF")) {
            return "false";
        }
        return column
                + " in (select r.property_id from rooms r where can_manage_room("
                + user()
                + ",r.id))";
    }

    /** Quyền theo hợp đồng; dữ liệu tài chính cần thêm quyền xem hóa đơn của liên hệ. */
    public String contractScope(String alias, boolean financial) {
        if (!role().equals("TENANT")) {
            return roomScope(alias + ".room_id");
        }
        String person = "select id from tenant_profiles where user_id=" + user();
        String base =
                "exists(select 1 from contract_parties p where p.contract_id="
                        + alias
                        + ".id and p.party_role='PRIMARY' and p.tenant_id in ("
                        + person
                        + ")) or "
                        + "exists(select 1 from contract_contacts p where p.contract_id="
                        + alias
                        + ".id and p.tenant_id in ("
                        + person
                        + ") and p.effective_from<=current_date and (p.effective_to is null or"
                        + " p.effective_to>current_date)"
                        + (financial ? " and p.can_view_billing" : "")
                        + ")";
        if (!financial) {
            base +=
                    " or exists(select 1 from contract_residencies p where p.contract_id="
                            + alias
                            + ".id and p.tenant_id in ("
                            + person
                            + ") and p.effective_from<=current_date and (p.effective_to is null or"
                            + " p.effective_to>current_date))";
        }
        return "(" + base + ")";
    }

    public Map<String, Object> room(long id) {
        var r = db.one("select * from v_room_status r where r.id=? and " + roomScope("r.id"), id);
        if (role().equals("TENANT")) {
            r.remove("internal_note");
        }
        return r;
    }

    public Map<String, Object> contract(long id) {
        return db.one(
                "select c.* from v_contract_current c where c.id=? and " + contractScope("c", true),
                id);
    }

    public Map<String, Object> document(long id) {
        var d = db.one("select * from v_document_balances where id=?", id);
        if (d.get("contract_id") != null) {
            contract(id(d, "contract_id"));
        } else {
            manager();
            var r = db.one("select room_id from reservations where id=?", d.get("reservation_id"));
            room(id(r, "room_id"));
        }
        if (role().equals("TENANT") && !"ISSUED".equals(d.get("status"))) {
            Access.denied();
        }
        return d;
    }

    public Map<String, Object> fund(long id) {
        var f = db.one("select * from v_fund_balances where id=?", id);
        if (f.get("contract_id") != null) {
            contract(id(f, "contract_id"));
        } else if (f.get("reservation_id") != null) {
            manager();
            room(
                    id(
                            db.one(
                                    "select room_id from reservations where id=?",
                                    f.get("reservation_id")),
                            "room_id"));
        } else {
            var tx =
                    db.one(
                            "select t.*,a.property_id from cash_transactions t join cash_accounts a"
                                    + " on a.id=t.cash_account_id where t.id=?",
                            f.get("unapplied_receipt_id"));
            cashAccount(id(tx, "cash_account_id"));
            if (role().equals("STAFF")
                    && id(tx, "recorded_by") != user()
                    && !Boolean.TRUE.equals(
                            db.jdbc.queryForObject(
                                    "select can_manage_property(?,?)",
                                    Boolean.class,
                                    user(),
                                    tx.get("property_id")))) {
                Access.denied();
            }
        }
        return f;
    }

    public Map<String, Object> cashAccount(long id) {
        manager();
        var a = db.one("select * from cash_accounts where id=? and is_active", id);
        if (a.get("property_id") == null) {
            admin();
        } else if (role().equals("STAFF")
                && db.jdbc.queryForObject(
                                "select count(*) from rooms r where r.property_id=? and"
                                        + " can_manage_room(?,r.id)",
                                Long.class,
                                a.get("property_id"),
                                user())
                        == 0) {
            Access.denied();
        }
        return a;
    }

    public void property(long id) {
        manager();
        if (!Boolean.TRUE.equals(
                db.jdbc.queryForObject(
                        "select can_manage_property(?,?)", Boolean.class, user(), id))) {
            Access.denied();
        }
    }

    public void action(long room, String action, BigDecimal amount) {
        // Kiểm tra vai trò quản lý và phạm vi phòng trước khi xét quyền hành động.
        manager();
        room(room);
        if (role().equals("ADMIN")) {
            return;
        }

        // Nhân viên trong phạm vi được giao có các quyền vận hành cơ bản này.
        if (java.util.Set.of("MANAGE", "BILLING", "RECEIVE", "MAINTENANCE").contains(action)) {
            return;
        }

        // Các quyền bổ sung phải còn hạn và đủ hạn mức cho số tiền đang thao tác.
        long n =
                db.jdbc.queryForObject(
                        "select count(*) from staff_permission_grants g join employee_assignments a"
                                + " on a.id=g.assignment_id join rooms r on r.id=? where a.user_id=?"
                                + " and a.assigned_at<=clock_timestamp() and a.revoked_at is null and"
                                + " (a.property_id=r.property_id or a.room_id=r.id) and g.action_code=?"
                                + " and (g.expires_at is null or g.expires_at>clock_timestamp()) and"
                                + " (g.amount_limit is null or g.amount_limit>=?)",
                        Long.class,
                        room,
                        user(),
                        action,
                        amount == null ? BigDecimal.ZERO : amount);
        if (n == 0) {
            Access.denied();
        }
    }

    public Map<String, Object> maintenance(long id) {
        var m = db.one("select * from v_maintenance where id=?", id);
        if (role().equals("TENANT")) {
            if (id(m, "submitted_by") != user()) {
                Access.denied();
            }
        } else if (m.get("resolved_room_id") != null) {
            room(id(m, "resolved_room_id"));
        } else {
            property(id(m, "resolved_property_id"));
        }
        return m;
    }
}
