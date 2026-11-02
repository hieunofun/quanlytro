package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service
@Profile("model49")
@Transactional
public class Billing49 {
    private final Database db;
    private final Access49 access;
    private final Finance49 finance;

    public Billing49(Database db, Access49 access, Finance49 finance) {
        this.db = db;
        this.access = access;
        this.finance = finance;
    }

    private LocalDate cutoffDate(LocalDate issue, Map<String, Object> property) {
        int day = ((Number) property.get("meter_cutoff_day")).intValue();
        LocalDate cutoff = monthDay(issue, day);
        return cutoff.isAfter(issue) ? monthDay(issue.minusMonths(1), day) : cutoff;
    }

    public Map<String, Object> calendar(long contract, LocalDate period) {
        var c = access.contract(contract);
        LocalDate p = period.withDayOfMonth(1);
        var t =
                db.one(
                        "select * from contract_terms where contract_id=? and status='APPROVED' and"
                                + " effective_from<? order by effective_from desc limit 1",
                        contract,
                        p.plusMonths(1));
        var property = db.one("select * from properties where id=?", c.get("property_id"));
        boolean advance = "IN_ADVANCE".equals(t.get("collection_timing"));
        LocalDate issue =
                monthDay(
                        advance ? p : p.plusMonths(1),
                        ((Number) property.get("invoice_issue_day")).intValue());
        int paymentDay = ((Number) t.get("payment_day")).intValue();
        LocalDate due = monthDay(issue, paymentDay);
        if (due.isBefore(issue)) {
            due = monthDay(issue.plusMonths(1), paymentDay);
        }
        return Map.of(
                "cutoffDate",
                cutoffDate(issue, property),
                "issueDate",
                issue,
                "dueDate",
                due,
                "collectionTiming",
                t.get("collection_timing"));
    }

    /** Lập hóa đơn tháng từ điều kiện hợp đồng, dịch vụ và các khoảng đo chưa tính. */
    public Map<String, Object> monthly(Map<String, Object> m) {
        access.manager();
        long contract = keyId(m, "contractId");
        access.contract(contract);
        db.one("select id from contracts where id=? for update", contract);
        var c = access.contract(contract);
        LocalDate p = day(m, null, "period");
        require(p != null, "Period is required");
        p = p.withDayOfMonth(1);
        var prior =
                db.rows(
                        "select * from billing_documents where contract_id=? and"
                                + " document_type='MONTHLY' and billing_period=?",
                        contract,
                        p);
        if (!prior.isEmpty()) {
            var existing = prior.getFirst();
            if (value(m, "code") != null) {
                require(
                        required(m, "code").equals(existing.get("document_code")),
                        "Monthly invoice already exists with a different code");
            }
            for (String field : List.of("issueDate", "dueDate")) {
                if (value(m, field) != null) {
                    require(
                            day(m, null, field)
                                    .equals(
                                            date(
                                                    existing.get(
                                                            field.equals("issueDate")
                                                                    ? "issue_date"
                                                                    : "due_date"))),
                            "Monthly invoice already exists with different dates");
                }
            }
            return finance.document(com.hostel.service.Database.id(existing, "id"));
        }
        var versions =
                db.rows(
                        "select * from contract_terms where contract_id=? and status='APPROVED' and"
                                + " effective_from<? order by effective_from",
                        contract,
                        p.plusMonths(1));
        require(!versions.isEmpty(), "Approved terms required");
        var t = versions.getLast();
        boolean advance = "IN_ADVANCE".equals(t.get("collection_timing"));
        var property = db.one("select * from properties where id=?", c.get("property_id"));
        LocalDate scheduled =
                monthDay(
                        advance ? p : p.plusMonths(1),
                        ((Number) property.get("invoice_issue_day")).intValue());
        LocalDate issue = day(m, scheduled, "issueDate"),
                scheduledDue = monthDay(issue, ((Number) t.get("payment_day")).intValue());
        if (scheduledDue.isBefore(issue)) {
            scheduledDue =
                    monthDay(issue.plusMonths(1), ((Number) t.get("payment_day")).intValue());
        }
        LocalDate due = day(m, scheduledDue, "dueDate");
        LocalDate tenancyEnd = date(c.get("end_date"));
        var ended =
                db.rows(
                        "select billing_end from checkout_records where contract_id=? order by id"
                                + " desc limit 1",
                        contract);
        if (!ended.isEmpty()) {
            tenancyEnd = date(ended.getFirst().get("billing_end"));
        }
        require(
                date(c.get("start_date")).isBefore(p.plusMonths(1)) && tenancyEnd.isAfter(p),
                "Billing month does not intersect the tenancy");
        long doc =
                finance.createDocument(
                        new HashMap<>(
                                Map.of(
                                        "contractId",
                                        contract,
                                        "code",
                                        text(m, "INV-" + contract + "-" + p, "code"),
                                        "documentType",
                                        "MONTHLY",
                                        "period",
                                        p,
                                        "issueDate",
                                        issue,
                                        "dueDate",
                                        due)));
        for (int i = 0; i < versions.size(); i++) {
            var v = versions.get(i);
            LocalDate from = max(p, max(date(c.get("start_date")), date(v.get("effective_from")))),
                    to =
                            min(
                                    date(v.get("lease_end")),
                                    min(
                                            p.plusMonths(1),
                                            min(
                                                    tenancyEnd,
                                                    i + 1 < versions.size()
                                                            ? date(
                                                                    versions.get(i + 1)
                                                                            .get("effective_from"))
                                                            : tenancyEnd)));
            long days = ChronoUnit.DAYS.between(from, to);
            if (days <= 0) {
                continue;
            }
            var line =
                    new HashMap<String, Object>(
                            Map.of(
                                    "itemKind",
                                    "ROOM_RENT",
                                    "name",
                                    "Tiền phòng",
                                    "unit",
                                    "Tháng",
                                    "unitPrice",
                                    v.get("rental_price"),
                                    "quantity",
                                    days,
                                    "calculationBasis",
                                    "PRORATED",
                                    "calculationPeriod",
                                    p,
                                    "serviceFrom",
                                    from,
                                    "serviceTo",
                                    to));
            line.put("contractTermId", v.get("id"));
            finance.line(doc, line);
        }
        for (var sub :
                db.rows(
                        "select x.*,s.name,s.unit_name,s.calculation_type from"
                                + " contract_service_subscriptions x join service_items s on"
                                + " s.id=x.service_item_id where x.contract_id=? and x.effective_from<?"
                                + " and (x.effective_to is null or x.effective_to>?) order by x.id",
                        contract,
                        p.plusMonths(1),
                        p)) {
            if ("METER_BASED".equals(sub.get("calculation_type"))) {
                LocalDate until = cutoffDate(issue, property).plusDays(1);
                for (var s :
                        db.rows(
                                "select s.* from v_segments s where s.contract_id=? and"
                                        + " s.service_item_id=? and s.service_end<=? and not"
                                        + " exists(select 1 from consumption_segments n where"
                                        + " n.supersedes_segment_id=s.id) and not exists(select 1 from"
                                        + " billing_items b where b.segment_id=s.id and"
                                        + " b.adjusts_item_id is null) order by service_start",
                                contract,
                                sub.get("service_item_id"),
                                time(until))) {
                    meterLine(doc, s, effectivePrice(c, s));
                }
                continue;
            }
            LocalDate
                    from = max(p, max(date(c.get("start_date")), date(sub.get("effective_from")))),
                    to =
                            min(
                                    p.plusMonths(1),
                                    min(
                                            tenancyEnd,
                                            sub.get("effective_to") == null
                                                    ? tenancyEnd
                                                    : date(sub.get("effective_to"))));
            long days = ChronoUnit.DAYS.between(from, to);
            if (days <= 0) {
                continue;
            }
            if ("ONE_TIME".equals(sub.get("charge_mode"))
                    && !db.rows(
                                    "select id from billing_items where subscription_id=? and"
                                            + " adjusts_item_id is null",
                                    sub.get("id"))
                            .isEmpty()) {
                continue;
            }
            var prices =
                    db.rows(
                            "select * from service_price_configs where property_id=? and"
                                    + " service_item_id=? and effective_from<=? order by effective_from"
                                    + " desc limit 1",
                            c.get("property_id"),
                            sub.get("service_item_id"),
                            from);
            require(!prices.isEmpty(), "Missing effective service price: " + sub.get("name"));
            var price = prices.getFirst();
            require(
                    db.jdbc.queryForObject(
                                    "select count(*) from service_price_configs where property_id=?"
                                            + " and service_item_id=? and effective_from>? and"
                                            + " effective_from<?",
                                    Long.class,
                                    c.get("property_id"),
                                    sub.get("service_item_id"),
                                    from,
                                    to)
                            == 0,
                    "Service price changes inside period; split subscription/charge intervals"
                            + " explicitly");
            long quantity = days * ((Number) sub.get("quantity")).longValue();
            String basis = "PRORATED";
            if ("PER_PERSON".equals(sub.get("calculation_type"))) {
                quantity =
                        db.jdbc.queryForObject(
                                (("select coalesce(sum(greatest(0,least(coalesce(effective_to,"
                                        + "?::date),?::date)-greatest(effective_from,?::date))),0)::bigint "
                                        + "from contract_residencies where contract_id=?")),
                                Long.class,
                                to,
                                to,
                                from,
                                contract);
            }
            if ("FULL_PERIOD".equals(sub.get("charge_mode"))
                    || "ONE_TIME".equals(sub.get("charge_mode"))) {
                basis = "FULL_PERIOD";
                quantity =
                        "PER_PERSON".equals(sub.get("calculation_type"))
                                ? db.jdbc.queryForObject(
                                        "select count(distinct tenant_id) from contract_residencies"
                                                + " where contract_id=? and effective_from<? and"
                                                + " (effective_to is null or effective_to>?)",
                                        Long.class,
                                        contract,
                                        to,
                                        from)
                                : ((Number) sub.get("quantity")).longValue();
            }
            if (quantity == 0) {
                continue;
            }
            var line =
                    new HashMap<String, Object>(
                            Map.of(
                                    "itemKind",
                                    "SERVICE",
                                    "name",
                                    sub.get("name"),
                                    "unit",
                                    sub.get("unit_name"),
                                    "unitPrice",
                                    price.get("unit_price"),
                                    "quantity",
                                    quantity,
                                    "calculationBasis",
                                    basis,
                                    "calculationPeriod",
                                    p,
                                    "serviceFrom",
                                    from,
                                    "serviceTo",
                                    to));
            line.put("subscriptionId", sub.get("id"));
            line.put("servicePriceConfigId", price.get("id"));
            if ("ONE_TIME".equals(sub.get("charge_mode"))) {
                line.put("sourceKey", "ONCE-" + sub.get("id"));
            }
            finance.line(doc, line);
        }
        return finance.document(doc);
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    public Map<String, Object> effectivePrice(Map<String, Object> c, Map<String, Object> s) {
        LocalDate from = time(s.get("service_start")).toLocalDate(),
                to = time(s.get("service_end")).toLocalDate();
        var prices =
                db.rows(
                        "select * from service_price_configs where property_id=? and"
                                + " service_item_id=? and effective_from<=? order by effective_from"
                                + " desc limit 1",
                        c.get("property_id"),
                        s.get("service_item_id"),
                        from);
        require(!prices.isEmpty(), "Missing effective meter price");
        require(
                db.jdbc.queryForObject(
                                "select count(*) from service_price_configs where property_id=? and"
                                        + " service_item_id=? and effective_from>? and"
                                        + " effective_from<?",
                                Long.class,
                                c.get("property_id"),
                                s.get("service_item_id"),
                                from,
                                to)
                        == 0,
                "Meter price changes inside interval; record a boundary measurement or explicit"
                        + " estimated adjustment");
        return prices.getFirst();
    }

    public void meterLine(long doc, Map<String, Object> s, Map<String, Object> price) {
        var service = db.one("select * from service_items where id=?", s.get("service_item_id"));
        LocalDate from = time(s.get("service_start")).toLocalDate(),
                to = time(s.get("service_end")).toLocalDate();
        if (!to.isAfter(from)) {
            to = from.plusDays(1);
        }
        var line =
                new HashMap<String, Object>(
                        Map.of(
                                "itemKind",
                                "UTILITY",
                                "name",
                                service.get("name"),
                                "unit",
                                service.get("unit_name"),
                                "unitPrice",
                                price.get("unit_price"),
                                "calculationBasis",
                                "METER",
                                "serviceFrom",
                                from,
                                "serviceTo",
                                to,
                                "segmentId",
                                s.get("id"),
                                "sourceKey",
                                "SEGMENT-" + s.get("id")));
        line.put("servicePriceConfigId", price.get("id"));
        finance.line(doc, line);
    }

    /** Ghi đủ tiền thực nhận, phân bổ vào nợ và giữ phần dư trong quỹ ADVANCE. */
    public Map<String, Object> pay(long document, Map<String, Object> m) {
        access.manager();
        var d = access.document(document);
        require("ISSUED".equals(d.get("status")), "Only issued documents may be paid");
        long contract = com.hostel.service.Database.id(d, "contract_id");
        var c = access.contract(contract);
        db.one("select id from contracts where id=? for update", contract);
        BigDecimal actual = cashAmount(m, "amount");
        long account;
        Long selected = optionalId(m, "cashAccountId");
        if (selected != null) {
            account = selected;
        } else {
            var existing =
                    db.rows(
                            "select id from cash_accounts where property_id=? and is_active and"
                                    + " account_type=? order by id limit 1",
                            c.get("property_id"),
                            "CASH".equals(text(m, "CASH", "method")) ? "CASH" : "BANK");
            require(
                    !existing.isEmpty(),
                    "Configure/select a real receiving account before recording cash");
            account = com.hostel.service.Database.id(existing.getFirst(), "id");
        }
        String code = required(m, "code");
        var previous = db.rows("select * from cash_transactions where transaction_code=?", code);
        boolean prior = !previous.isEmpty();
        Object occurred = value(m, "occurredAt");
        if (occurred == null) {
            occurred = prior ? previous.getFirst().get("occurred_at") : now();
        }
        var receipt =
                new HashMap<String, Object>(
                        Map.of(
                                "cashAccountId",
                                account,
                                "contractId",
                                contract,
                                "paymentDocumentId",
                                document,
                                "code",
                                code,
                                "amount",
                                actual,
                                "method",
                                text(m, "CASH", "method"),
                                "occurredAt",
                                occurred,
                                "confirmed",
                                true));
        for (String field : List.of("externalReference", "payerId", "note")) {
            if (value(m, field) != null) {
                receipt.put(field, value(m, field));
            }
        }
        finance.receive(receipt);
        if (prior) {
            return finance.document(document);
        }
        long fund = finance.ensureFund(contract, "ADVANCE");
        BigDecimal remaining = actual;
        for (var item :
                db.rows(
                        "select * from v_item_balances where document_id=? and balance_due>0 order"
                                + " by id",
                        document)) {
            BigDecimal part = remaining.min(money(item, "balance_due"));
            if (part.signum() <= 0) {
                break;
            }
            finance.entry(
                    Map.of(
                            "code",
                            code + "-PAY-" + item.get("id"),
                            "entryType",
                            "PAYMENT",
                            "fromFundId",
                            fund,
                            "debitItemId",
                            item.get("id"),
                            "amount",
                            part,
                            "reason",
                            text(m, "Recorded manual payment", "notes")));
            remaining = remaining.subtract(part);
        }
        return finance.document(document);
    }
}
