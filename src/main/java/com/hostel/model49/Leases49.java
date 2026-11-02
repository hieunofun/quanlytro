package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service
@Profile("model49")
@Transactional
public class Leases49 {
    private final Database db;
    private final Access49 access;
    private final Finance49 finance;
    private final Events49 events;

    public Leases49(Database db, Access49 access, Finance49 finance, Events49 events) {
        this.db = db;
        this.access = access;
        this.finance = finance;
        this.events = events;
    }

    public Map<String, Object> detail(long id) {
        var c = new LinkedHashMap<>(access.contract(id));
        if (c.get("rental_price") == null) {
            var t =
                    db.rows(
                            "select * from contract_terms where contract_id=? order by version_no"
                                    + " desc limit 1",
                            id);
            if (!t.isEmpty()) {
                c.put("rental_price", t.getFirst().get("rental_price"));
                c.put("deposit_amount", t.getFirst().get("deposit_required"));
                c.put("end_date", t.getFirst().get("lease_end"));
            }
        }
        c.put(
                "members",
                db.rows(
                        "select x.*,t.full_name,t.identity_number,t.phone from contract_residencies"
                                + " x join tenant_profiles t on t.id=x.tenant_id where x.contract_id=?"
                                + " order by x.effective_from,x.id",
                        id));
        c.put(
                "contacts",
                db.rows(
                        "select x.*,t.full_name from contract_contacts x join tenant_profiles t on"
                                + " t.id=x.tenant_id where x.contract_id=? order by x.id",
                        id));
        c.put(
                "parties",
                db.rows(
                        "select p.*,t.full_name from contract_parties p join tenant_profiles t on"
                                + " t.id=p.tenant_id where p.contract_id=?",
                        id));
        c.put(
                "versions",
                db.rows(
                        "select * from contract_terms where contract_id=? order by version_no",
                        id));
        c.put(
                "possessions",
                db.rows(
                        "select * from room_possessions where contract_id=? order by taken_at",
                        id));
        c.put(
                "funds",
                db.rows("select * from v_fund_balances where contract_id=? order by id", id));
        c.put("violations", db.rows("select * from contract_violations where contract_id=?", id));
        var x =
                db.rows(
                        "select x.*,x.billing_end checkout_date,x.deposit_snapshot"
                                + " initial_deposit,x.outstanding_snapshot unpaid_invoices_amount from"
                                + " checkout_records x where contract_id=? order by id desc limit 1",
                        id);
        if (!x.isEmpty()) {
            var out = x.getFirst();
            var funds =
                    db.one(
                            "select coalesce(sum(balance),0) held,coalesce(sum(available),0)"
                                    + " available,count(*) filter(where"
                                    + " reconciliation_status='PENDING') pending from v_fund_balances"
                                    + " where contract_id=?",
                            id);
            BigDecimal debt =
                    db.jdbc.queryForObject(
                            "select coalesce(sum(balance_due),0) from v_item_balances where"
                                    + " contract_id=? and document_status='ISSUED'",
                            BigDecimal.class,
                            id);
            out.put("held_balance", funds.get("held"));
            out.put("debt_remaining", debt);
            out.put("net_settlement_due", money(funds, "held").subtract(debt));
            out.put(
                    "refund_due",
                    ((Number) funds.get("pending")).longValue() > 0
                            ? null
                            : money(funds, "available").subtract(debt).max(BigDecimal.ZERO));
            out.put(
                    "actual_refund_amount",
                    db.jdbc.queryForObject(
                            "select coalesce(sum(e.amount),0) from fund_entries e join"
                                    + " fund_accounts f on f.id=e.from_fund_id join cash_transactions"
                                    + " tx on tx.id=e.cash_transaction_id where f.contract_id=? and"
                                    + " e.entry_type='REFUND' and tx.status='CONFIRMED'",
                            BigDecimal.class,
                            id));
            out.put(
                    "damage_amount",
                    db.jdbc.queryForObject(
                            "select coalesce(sum(charge_amount),0) from v_item_balances where"
                                    + " contract_id=? and item_kind='DAMAGE' and"
                                    + " document_status='ISSUED'",
                            BigDecimal.class,
                            id));
            c.put("checkout", out);
        } else {
            c.put("checkout", null);
        }
        return c;
    }

    /** Tạo hợp đồng nháp và bản điều kiện; chưa ghi nhận tiền cọc thực nhận. */
    public Map<String, Object> create(Map<String, Object> m) {
        access.manager();
        long room = keyId(m, "roomId");
        access.action(room, "MANAGE", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", room);
        Long tenant = optionalId(m, "tenantId");
        require(
                !(tenant != null && m.get("tenant") != null),
                "Choose an existing tenant or a new tenant payload, not both");
        if (tenant == null && m.get("tenant") instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            var t = (Map<String, Object>) raw;
            String identity = text(t, null, "identityNumber");
            require(
                    identity == null
                            || db.rows(
                                            "select id from tenant_profiles where"
                                                    + " identity_number=?",
                                            identity)
                                    .isEmpty(),
                    "Identity already exists; ask ADMIN to verify the shared profile");
            tenant =
                    db.insert(
                            "tenant_profiles",
                            "full_name,identity_number,phone,email",
                            required(t, "fullName"),
                            identity,
                            text(t, null, "phone"),
                            text(t, null, "email"));
        }
        require(tenant != null, "A verified tenant or new tenant payload is required");
        if (access.role().equals("STAFF") && m.get("tenant") == null) {
            long person = tenant;
            require(
                    db.jdbc.queryForObject(
                                    "select count(*) from contract_parties p join contracts c on"
                                            + " c.id=p.contract_id where p.tenant_id=? and "
                                            + access.contractScope("c", true),
                                    Long.class,
                                    person)
                            > 0,
                    "Existing profile outside assignment; use verified new-tenant intake or ask"
                            + " ADMIN");
        }
        LocalDate start = day(m, null, "startDate"), end = day(m, null, "endDate");
        require(start != null && end != null && end.isAfter(start), "Valid start/end are required");
        long id =
                db.insert(
                        "contracts",
                        ("room_id,reservation_id,previous_contract_id,continuation_reason,"
                                + "contract_code,start_date,created_by"),
                        room,
                        optionalId(m, "reservationId"),
                        optionalId(m, "previousContractId"),
                        text(m, null, "continuationReason"),
                        required(m, "code"),
                        start,
                        access.user());
        db.insert(
                "contract_parties",
                "contract_id,tenant_id,party_role,effective_from",
                id,
                tenant,
                "PRIMARY",
                start);
        db.insert(
                "contract_contacts",
                "contract_id,tenant_id,effective_from,can_view_billing,authorized_by",
                id,
                tenant,
                start,
                true,
                access.user());
        db.insert(
                "contract_terms",
                ("contract_id,version_no,effective_from,lease_end,rental_price,"
                        + "deposit_required,payment_day,collection_timing,terms"),
                id,
                1,
                start,
                end,
                amount(m, "0", "rentalPrice"),
                amount(m, "0", "deposit"),
                Integer.parseInt(text(m, "5", "paymentDay")),
                text(m, "IN_ADVANCE", "collectionTiming"),
                text(m, "", "terms"));
        if (bool(m, false, "primaryResides")) {
            db.insert(
                    "contract_residencies",
                    "contract_id,tenant_id,effective_from,recorded_by",
                    id,
                    tenant,
                    start,
                    access.user());
        }
        if (value(m, "signedAt") != null) {
            activate(id, m);
        }
        events.audit(
                "CREATE",
                "contracts",
                id,
                null,
                db.one("select * from contracts where id=?", id),
                "Scoped tenant intake");
        return detail(id);
    }

    public Map<String, Object> activate(long id, Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        access.action(com.hostel.service.Database.id(c, "room_id"), "MANAGE", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", c.get("room_id"));
        db.one("select id from contracts where id=? for update", id);
        require(
                "DRAFT".equals(c.get("status")) || "PENDING".equals(c.get("status")),
                "Contract is already active or ended");
        var t =
                db.one(
                        "select * from contract_terms where contract_id=? order by version_no limit"
                                + " 1",
                        id);
        db.jdbc.update(
                "update contract_terms set status='APPROVED',signed_at=?,approved_by=? where id=?",
                time(value(m, "signedAt")),
                access.user(),
                t.get("id"));
        db.jdbc.update("update contracts set status='ACTIVE' where id=?", id);
        finance.ensureFund(id, "DEPOSIT");
        finance.ensureFund(id, "ADVANCE");
        for (var s : db.rows("select id from service_items where is_mandatory and is_active")) {
            db.insert(
                    "contract_service_subscriptions",
                    "contract_id,service_item_id,effective_from,recorded_by",
                    id,
                    s.get("id"),
                    c.get("start_date"),
                    access.user());
        }
        events.audit(
                "ACTIVATE",
                "contracts",
                id,
                c,
                db.one("select * from contracts where id=?", id),
                "Manually reported signed date; no digital signature claim");
        return detail(id);
    }

    public Map<String, Object> renew(long id, Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        access.action(com.hostel.service.Database.id(c, "room_id"), "MANAGE", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", c.get("room_id"));
        db.one("select id from contracts where id=? for update", id);
        require("ACTIVE".equals(c.get("status")), "Only active tenancy may be renewed");
        var previous =
                db.one(
                        "select * from contract_terms where contract_id=? and status='APPROVED'"
                                + " order by version_no desc limit 1",
                        id);
        LocalDate from = day(m, date(previous.get("lease_end")), "effectiveFrom"),
                end = day(m, null, "endDate");
        require(end != null && end.isAfter(from), "Renewed end must follow effective date");
        int version = ((Number) previous.get("version_no")).intValue() + 1;
        db.insert(
                "contract_terms",
                ("contract_id,version_no,effective_from,lease_end,rental_price,"
                        + "deposit_required,payment_day,collection_timing,terms,signed_at,"
                        + "approved_by,status"),
                id,
                version,
                from,
                end,
                amount(m, previous.get("rental_price").toString(), "rentalPrice"),
                amount(m, previous.get("deposit_required").toString(), "deposit"),
                previous.get("payment_day"),
                previous.get("collection_timing"),
                text(m, previous.get("terms").toString(), "terms"),
                time(value(m, "signedAt")),
                access.user(),
                "APPROVED");
        events.audit(
                "RENEW",
                "contracts",
                id,
                previous,
                db.one(
                        "select * from contract_terms where contract_id=? and version_no=?",
                        id,
                        version),
                required(m, "reason"));
        return detail(id);
    }

    private void personScope(long person) {
        if (access.role().equals("STAFF")) {
            require(
                    db.jdbc.queryForObject(
                                    "select count(*) from tenant_profiles t where t.id=? and"
                                            + " (exists(select 1 from contract_parties p join contracts"
                                            + " c on c.id=p.contract_id where p.tenant_id=t.id and "
                                            + access.contractScope("c", true)
                                            + ") or exists(select 1 from contract_residencies p"
                                            + " join contracts c on c.id=p.contract_id where"
                                            + " p.tenant_id=t.id and "
                                            + access.contractScope("c", false)
                                            + "))",
                                    Long.class,
                                    person)
                            > 0,
                    "Person is outside assignment; request verified intake from ADMIN");
        }
    }

    public Object reside(long id, Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        long person = keyId(m, "tenantId");
        personScope(person);
        LocalDate from = day(m, date(c.get("start_date")), "effectiveFrom");
        long row =
                db.insert(
                        "contract_residencies",
                        "contract_id,tenant_id,effective_from,effective_to,relationship,recorded_by",
                        id,
                        person,
                        from,
                        day(m, null, "effectiveTo"),
                        text(m, null, "relationship"),
                        access.user());
        events.audit(
                "RESIDE",
                "contract_residencies",
                row,
                null,
                m,
                "Actual resident recorded explicitly");
        return db.one("select * from contract_residencies where id=?", row);
    }

    public Object depart(long contract, long residency, Map<String, Object> m) {
        access.manager();
        access.contract(contract);
        var old =
                db.one(
                        "select * from contract_residencies where id=? and contract_id=? for"
                                + " update",
                        residency,
                        contract);
        require(old.get("effective_to") == null, "Residency already ended");
        db.jdbc.update(
                "update contract_residencies set effective_to=? where id=?",
                day(m, null, "effectiveTo"),
                residency);
        events.audit(
                "DEPART",
                "contract_residencies",
                residency,
                old,
                db.one("select * from contract_residencies where id=?", residency),
                text(m, "Reported departure", "reason"));
        return detail(contract);
    }

    public Object contact(long id, Map<String, Object> m) {
        access.manager();
        access.contract(id);
        long person = keyId(m, "tenantId");
        personScope(person);
        db.one("select id from contracts where id=? for update", id);
        LocalDate from = day(m, LocalDate.now(), "effectiveFrom");
        db.jdbc.update(
                "update contract_contacts set effective_to=? where contract_id=? and"
                        + " contact_role='PRIMARY' and effective_to is null",
                from,
                id);
        db.insert(
                "contract_contacts",
                "contract_id,tenant_id,effective_from,can_view_billing,authorized_by",
                id,
                person,
                from,
                bool(m, false, "canViewBilling"),
                access.user());
        events.audit("CONTACT_CHANGE", "contracts", id, null, m, required(m, "reason"));
        return detail(id);
    }

    /** Ghi nhận giao phòng thực tế sau khi kiểm tra trạng thái phòng và tiền cọc. */
    public Object possess(long id, Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        long room = com.hostel.service.Database.id(c, "room_id");
        access.action(room, "MANAGE", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", room);
        db.one("select id from contracts where id=? for update", id);
        c = access.contract(id);
        require(
                "ACTIVE".equals(c.get("status"))
                        && "VERIFIED".equals(c.get("reconciliation_status")),
                "Active reconciled tenancy required");
        BigDecimal required = money(c, "deposit_amount"),
                held = money(access.fund(finance.ensureFund(id, "DEPOSIT")), "balance");
        if (held.compareTo(required) < 0) {
            access.action(room, "WAIVE_DEPOSIT", required.subtract(held));
            required(m, "reason");
        }
        var taken = time(value(m, "takenAt"));
        require(
                !taken.isAfter(now()) && !taken.toLocalDate().isBefore(date(c.get("start_date"))),
                "Actual handover time must follow contract start and cannot be in the future");
        long p =
                db.insert(
                        "room_possessions",
                        "contract_id,taken_at,recorded_by,note",
                        id,
                        taken,
                        access.user(),
                        text(m, "", "reason"));
        events.audit("TAKE_POSSESSION", "contracts", id, null, m, text(m, "", "reason"));
        return db.one("select * from room_possessions where id=?", p);
    }

    public Object notice(long id, Map<String, Object> m) {
        var c = access.contract(id);
        long n =
                db.insert(
                        "move_out_notices",
                        "contract_id,supersedes_notice_id,requested_date,reason,submitted_by",
                        id,
                        optionalId(m, "supersedesNoticeId"),
                        day(m, null, "requestedDate"),
                        text(m, "", "reason"),
                        access.user());
        Long old = optionalId(m, "supersedesNoticeId");
        if (old != null) {
            db.jdbc.update(
                    "update move_out_notices set status='SUPERSEDED' where id=? and contract_id=?",
                    old,
                    id);
        }
        return db.one("select * from move_out_notices where id=?", n);
    }

    /** Ghi nhận trả phòng vật lý và ngày kết thúc tính tiền để lập quyết toán. */
    public Object checkout(long id, Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        access.action(com.hostel.service.Database.id(c, "room_id"), "MANAGE", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", c.get("room_id"));
        db.one("select id from contracts where id=? for update", id);
        var previous =
                db.rows("select * from checkout_records where contract_id=? order by id desc", id);
        if (!previous.isEmpty()) {
            return detail(id).get("checkout");
        }
        c = access.contract(id);
        require("ACTIVE".equals(c.get("status")), "Only an active tenancy may be checked out");
        require(
                !db.rows(
                                "select id from room_possessions where contract_id=? and"
                                        + " returned_at is null",
                                id)
                        .isEmpty(),
                "Record/verify the physical handover before reporting a physical return");
        LocalDate end = day(m, null, "date", "billingEnd");
        require(
                end != null && end.isAfter(date(c.get("start_date"))),
                "Valid agreed billing end is required");
        OffsetDateTime returned = time(value(m, "returnedAt", "date"));
        require(!returned.isAfter(now()), "Actual return cannot occur in the future");
        db.jdbc.update(
                "update room_possessions set returned_at=? where contract_id=? and returned_at is"
                        + " null",
                returned,
                id);
        db.jdbc.update(
                "update contract_residencies set effective_to=? where contract_id=? and"
                        + " effective_from<? and (effective_to is null or effective_to>?)",
                end,
                id,
                end,
                end);
        db.jdbc.update("update contracts set status='ENDED' where id=?", id);
        long x =
                db.insert(
                        "checkout_records",
                        "contract_id,billing_end,notes",
                        id,
                        end,
                        text(m, "", "notes", "reason"));
        db.jdbc.update(
                "update room_operating_periods set valid_to=? where room_id=? and valid_to is null",
                returned,
                c.get("room_id"));
        db.insert(
                "room_operating_periods",
                "room_id,valid_from,status,recorded_by,note",
                c.get("room_id"),
                returned,
                "TURNOVER",
                access.user(),
                "Returned; inspect/prepare before next handover");
        events.audit(
                "RETURN_POSSESSION",
                "checkout_records",
                x,
                c,
                m,
                "Physical return recorded; financial approval/refund are separate");
        return detail(id).get("checkout");
    }

    public Object reconcile(long id, Map<String, Object> m) {
        access.admin();
        var c = access.contract(id);
        required(m, "reason");
        require(bool(m, false, "confirmed"), "Explicit confirmation is required");
        db.jdbc.update("update contracts set reconciliation_status='VERIFIED' where id=?", id);
        events.audit("RECONCILE_CONTRACT", "contracts", id, c, m, required(m, "reason"));
        return detail(id);
    }
}
