package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service
@Profile("model49")
@Transactional
public class Settlements49 {
    private final Database db;
    private final Access49 access;
    private final Finance49 finance;
    private final Billing49 billing;
    private final Leases49 leases;
    private final Events49 events;

    public Settlements49(
            Database db,
            Access49 access,
            Finance49 finance,
            Billing49 billing,
            Leases49 leases,
            Events49 events) {
        this.db = db;
        this.access = access;
        this.finance = finance;
        this.billing = billing;
        this.leases = leases;
        this.events = events;
    }

    public Map<String, Object> statement(long contract) {
        access.manager();
        var detail = leases.detail(contract); // Includes financial scope checks for the caller.
        var result = new LinkedHashMap<String, Object>();
        result.put("contractId", contract);
        result.put("checkout", detail.get("checkout"));
        result.put("funds", detail.get("funds"));
        result.put(
                "charges",
                db.rows(
                        "select * from v_item_balances where contract_id=? and"
                                + " document_status='ISSUED' order by due_date,id",
                        contract));
        result.put(
                "adjustments",
                db.rows(
                        "select * from v_billing_lines where contract_id=? and"
                                + " document_status='ISSUED' and adjusts_item_id is not null order by"
                                + " document_id,id",
                        contract));
        result.put(
                "holds",
                db.rows(
                        "select h.* from fund_holds h join fund_accounts f on"
                                + " f.id=h.fund_account_id where f.contract_id=? order by h.id",
                        contract));
        result.put(
                "allocations",
                db.rows(
                        "select e.* from fund_entries e join fund_accounts f on"
                                + " f.id=coalesce(e.from_fund_id,e.to_fund_id) where f.contract_id=?"
                                + " and e.entry_type in ('PAYMENT','ADJUSTMENT_RELEASE') order by e.id",
                        contract));
        result.put(
                "refunds",
                db.rows(
                        "select tx.* from cash_transactions tx join fund_entries e on"
                                + " e.cash_transaction_id=tx.id join fund_accounts f on"
                                + " f.id=e.from_fund_id where f.contract_id=? and e.entry_type='REFUND'"
                                + " order by tx.id",
                        contract));
        result.put(
                "pendingItems",
                db.rows(
                        "select * from v_billing_lines where contract_id=? and verification_status"
                                + " in ('DISPUTED','PENDING') order by document_id,id",
                        contract));
        return result;
    }

    public Object approve(long contract, Map<String, Object> m) {
        access.manager();
        var c = access.contract(contract);
        long room = com.hostel.service.Database.id(c, "room_id");
        access.action(room, "ADJUST", BigDecimal.ZERO);
        db.one("select id from rooms where id=? for update", room);
        db.one("select id from contracts where id=? for update", contract);
        var x =
                db.one(
                        "select * from checkout_records where contract_id=? order by id desc limit"
                                + " 1 for update",
                        contract);
        if ("APPROVED".equals(x.get("status"))) {
            return leases.detail(contract).get("checkout");
        }
        require("DRAFT".equals(x.get("status")), "Legacy checkout must be reconciled explicitly");
        require(
                db.jdbc.queryForObject(
                                "select count(*) from billing_documents where contract_id=? and"
                                        + " status='DRAFT'",
                                Long.class,
                                contract)
                        == 0,
                "Issue/remove outstanding draft documents before financial approval");
        LocalDate end = date(x.get("billing_end"));
        Long adjustment = null;
        for (LocalDate period = date(c.get("start_date")).withDayOfMonth(1);
                period.isBefore(end);
                period = period.plusMonths(1)) {
            if (db.rows(
                            "select id from billing_documents where contract_id=? and"
                                    + " document_type='MONTHLY' and billing_period=?",
                            contract,
                            period)
                    .isEmpty()) {
                var created =
                        billing.monthly(
                                Map.of(
                                        "contractId",
                                        contract,
                                        "period",
                                        period,
                                        "issueDate",
                                        LocalDate.now(),
                                        "dueDate",
                                        LocalDate.now()));
                finance.issue(com.hostel.service.Database.id(created, "id"));
            }
        }
        var returnTime =
                db.rows(
                        "select returned_at from room_possessions where contract_id=? and"
                                + " returned_at is not null order by returned_at desc limit 1",
                        contract);
        var until =
                returnTime.isEmpty() ? time(end) : time(returnTime.getFirst().get("returned_at"));
        long missing =
                db.jdbc.queryForObject(
                        "select count(*) from contract_service_subscriptions sub join service_items"
                                + " si on si.id=sub.service_item_id where sub.contract_id=? and"
                                + " si.calculation_type='METER_BASED' and sub.effective_from<? and"
                                + " (sub.effective_to is null or sub.effective_to>=?) and not"
                                + " exists(select 1 from v_segments s where"
                                + " s.contract_id=sub.contract_id and"
                                + " s.service_item_id=sub.service_item_id and s.service_end>=? and not"
                                + " exists(select 1 from consumption_segments n where"
                                + " n.supersedes_segment_id=s.id))",
                        Long.class,
                        contract,
                        end,
                        end,
                        until);
        if (missing > 0) {
            required(m, "pendingUtilityReason");
            require(
                    db.jdbc
                                    .queryForObject(
                                            "select coalesce(sum(held_amount),0) from"
                                                    + " v_fund_balances where contract_id=?",
                                            BigDecimal.class,
                                            contract)
                                    .signum()
                            > 0,
                    "Unmeasured final utilities require an explicit hold and review date before"
                            + " settlement approval");
        }
        for (var line :
                db.rows(
                        "select b.*,si.calculation_type from v_item_balances b left join"
                                + " contract_service_subscriptions sub on sub.id=b.subscription_id left"
                                + " join service_items si on si.id=sub.service_item_id where"
                                + " b.contract_id=? and b.document_status='ISSUED' and b.item_kind in"
                                + " ('ROOM_RENT','SERVICE') and b.calculation_basis='PRORATED' and"
                                + " b.service_to>?",
                        contract,
                        end)) {
            LocalDate from = date(line.get("service_from")), to = date(line.get("service_to"));
            long correctDays =
                    Math.max(0, ChronoUnit.DAYS.between(from, to.isAfter(end) ? end : to));
            BigDecimal quantity = BigDecimal.valueOf(correctDays);
            if ("SERVICE".equals(line.get("item_kind"))) {
                if ("PER_PERSON".equals(line.get("calculation_type"))) {
                    quantity =
                            correctDays == 0
                                    ? BigDecimal.ZERO
                                    : db.jdbc.queryForObject(
                                            (("select coalesce(sum(greatest(0,least(coalesce(effective_to,"
                                                    + "?::date),?::date)-greatest(effective_from,?::date))),0)::numeric "
                                                    + "from contract_residencies where contract_id=?")),
                                            BigDecimal.class,
                                            end,
                                            end,
                                            from,
                                            contract);
                } else {
                    quantity =
                            money(line, "billed_quantity")
                                    .multiply(BigDecimal.valueOf(correctDays))
                                    .divide(
                                            BigDecimal.valueOf(ChronoUnit.DAYS.between(from, to)),
                                            6,
                                            RoundingMode.HALF_UP);
                }
            }
            BigDecimal correct =
                    money(line, "unit_price_snapshot")
                            .multiply(quantity)
                            .divide(
                                    BigDecimal.valueOf(
                                            date(line.get("calculation_period")).lengthOfMonth()),
                                    2,
                                    RoundingMode.HALF_UP);
            BigDecimal delta = money(line, "charge_amount").subtract(correct);
            if (delta.signum() <= 0) {
                continue;
            }
            if (adjustment == null) {
                adjustment =
                        finance.createDocument(
                                Map.of(
                                        "contractId",
                                        contract,
                                        "checkoutId",
                                        x.get("id"),
                                        "documentType",
                                        "ADJUSTMENT",
                                        "relatedDocumentId",
                                        line.get("document_id"),
                                        "code",
                                        "END-ADJ-" + x.get("id"),
                                        "issueDate",
                                        LocalDate.now(),
                                        "reason",
                                        required(m, "reason")));
            }
            finance.line(
                    adjustment,
                    Map.of(
                            "itemKind",
                            "DISCOUNT",
                            "direction",
                            "CREDIT",
                            "adjustsItemId",
                            line.get("id"),
                            "name",
                            "Điều chỉnh " + line.get("item_name_snapshot") + " sau chốt ngày trả",
                            "unitPrice",
                            delta,
                            "quantity",
                            1,
                            "serviceFrom",
                            from,
                            "serviceTo",
                            to,
                            "sourceKey",
                            "END-CREDIT-" + x.get("id") + "-" + line.get("id")));
        }
        if (adjustment != null) {
            finance.issue(adjustment);
            for (var credit :
                    db.rows("select * from v_billing_lines where document_id=?", adjustment)) {
                finance.releaseOverpayment(
                        com.hostel.service.Database.id(credit, "id"), required(m, "reason"));
            }
        }
        Long finalDocument = null;
        for (var s :
                db.rows(
                        "select s.* from v_segments s where s.contract_id=? and s.service_end<=?"
                                + " and not exists(select 1 from consumption_segments n where"
                                + " n.supersedes_segment_id=s.id) and not exists(select 1 from"
                                + " billing_items b where b.segment_id=s.id and b.adjusts_item_id is"
                                + " null)",
                        contract,
                        until)) {
            var price = billing.effectivePrice(c, s);
            if (finalDocument == null) {
                finalDocument =
                        finance.createDocument(
                                Map.of(
                                        "contractId",
                                        contract,
                                        "checkoutId",
                                        x.get("id"),
                                        "documentType",
                                        "FINAL_CHARGE",
                                        "code",
                                        "END-USAGE-" + x.get("id"),
                                        "issueDate",
                                        LocalDate.now(),
                                        "reason",
                                        required(m, "reason")));
            }
            billing.meterLine(finalDocument, s, price);
        }
        if (finalDocument != null) {
            finance.issue(finalDocument);
        }
        long deposit = finance.ensureFund(contract, "DEPOSIT"),
                advance = finance.ensureFund(contract, "ADVANCE");
        var dep = access.fund(deposit);
        var adv = access.fund(advance);
        require(
                "VERIFIED".equals(dep.get("reconciliation_status")),
                "Verify actual deposit balance before approving settlement");
        BigDecimal debt =
                db.jdbc.queryForObject(
                        "select coalesce(sum(balance_due),0) from v_item_balances where"
                                + " contract_id=? and document_status='ISSUED'",
                        BigDecimal.class,
                        contract);
        db.jdbc.update(
                ("update checkout_records set deposit_snapshot=?,"
                        + "advance_snapshot=?,outstanding_snapshot=?,status='APPROVED',"
                        + "approved_by=?,approved_at=clock_timestamp() where id=?"),
                dep.get("balance"),
                adv.get("balance"),
                debt,
                access.user(),
                x.get("id"));
        List<Long> sources = new ArrayList<>();
        if (bool(m, false, "applyAdvance")) {
            sources.add(advance);
        }
        if (bool(m, false, "useDeposit")) {
            sources.add(deposit);
        }
        for (long source : sources) {
            for (var line :
                    db.rows(
                            "select * from v_item_balances where contract_id=? and"
                                    + " document_status='ISSUED' and verification_status in"
                                    + " ('VERIFIED','ESTIMATED') and balance_due>0 order by due_date"
                                    + " nulls last,id",
                            contract)) {
                BigDecimal available = money(access.fund(source), "available"),
                        part = available.min(money(line, "balance_due"));
                if (part.signum() <= 0) {
                    break;
                }
                finance.entry(
                        Map.of(
                                "code",
                                "END-APPLY-" + x.get("id") + "-" + source + "-" + line.get("id"),
                                "entryType",
                                "PAYMENT",
                                "fromFundId",
                                source,
                                "debitItemId",
                                line.get("id"),
                                "amount",
                                part,
                                "reason",
                                required(m, "reason")));
            }
        }
        events.audit(
                "APPROVE_CHECKOUT",
                "checkout_records",
                com.hostel.service.Database.id(x, "id"),
                x,
                db.one("select * from checkout_records where id=?", x.get("id")),
                required(m, "reason"));
        return leases.detail(contract).get("checkout");
    }
}
