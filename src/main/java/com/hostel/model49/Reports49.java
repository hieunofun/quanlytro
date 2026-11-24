package com.hostel.model49;

import com.hostel.service.Database;
import com.hostel.service.PdfService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Profile("model49")
public class Reports49 {
    private final Database db;
    private final Access49 access;
    private final PdfService pdf;
    private final Catalog49 catalog;

    public Reports49(Database db, Access49 access, PdfService pdf, Catalog49 catalog) {
        this.db = db;
        this.access = access;
        this.pdf = pdf;
        this.catalog = catalog;
    }

    private String cashScope() {
        return access.role().equals("ADMIN")
                ? "true"
                : "(a.property_id in (select property_id from employee_assignments where user_id="
                        + access.user()
                        + " and revoked_at is null and property_id is not null) or tx.recorded_by="
                        + access.user()
                        + ")";
    }

    private static final String PAYMENT_AMOUNTS = """
            coalesce(sum(a.amount) filter (where receipt.day < d.due_date), 0) as paid_early,
            coalesce(sum(a.amount) filter (where receipt.day = d.due_date), 0) as paid_on_time,
            coalesce(sum(a.amount) filter (where receipt.day > d.due_date), 0) as paid_late
            """;

    private static final String PAYMENT_APPLICATIONS = """
             join v_item_applications a on a.item_id=i.id
             join fund_entries entry on entry.id=a.entry_id
             left join fund_entries original on original.id=entry.original_entry_id
             left join fund_entries source on source.id=original.original_entry_id
             join fund_entries payment on payment.id=coalesce(source.id,original.id,entry.id)
             left join cash_transactions tx on tx.payment_document_id=d.id and tx.status='CONFIRMED'
                  and payment.entry_code=tx.transaction_code||'-PAY-'||i.id
             cross join lateral (
                 select (coalesce(tx.occurred_at,payment.posted_at) at time zone 'Asia/Ho_Chi_Minh')::date as day
             ) receipt
            """;

    private static final String ITEM_CLASSIFICATION = """
             left join v_segments seg on seg.id=i.segment_id
             left join service_items si on si.id=seg.service_item_id
             cross join lateral (
                 select case
                     when i.item_kind='ROOM_RENT' then 'ROOM_RENT'
                     when si.service_code in ('ELEC','ELECTRICITY')
                          or (i.item_kind='UTILITY' and (
                              i.item_name_snapshot like '%Điện%'
                              or lower(i.item_name_snapshot) like '%điện%'
                              or lower(i.item_name_snapshot) like '%elec%'
                          )) then 'ELECTRICITY'
                     when si.service_code='WATER'
                          or (i.item_kind='UTILITY' and (
                              i.item_name_snapshot like '%Nước%'
                              or lower(i.item_name_snapshot) like '%nước%'
                              or lower(i.item_name_snapshot) like '%water%'
                          )) then 'WATER'
                     else 'OTHER'
                 end as bucket
             ) cls
            """;

    private List<String> billingPeriods() {
        return db.rows(
                        "select distinct to_char(coalesce(d.billing_period,d.issue_date),'YYYY-MM') p"
                                + " from billing_documents d join contracts c on c.id=d.contract_id"
                                + " where d.status='ISSUED' and "
                                + access.contractScope("c", true) + " order by p desc")
                .stream().map(row -> row.get("p").toString()).toList();
    }

    @GetMapping("/dashboard")
    public Object dashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String viewType) {
        access.manager();
        var periods = billingPeriods();
        LocalDate start, end;
        String currentLabel, mode;

        if (quarter != null && quarter.matches("\\d{4}-Q[1-4]")) {
            mode = "quarter";
            int year = Integer.parseInt(quarter.substring(0, 4));
            int q = Integer.parseInt(quarter.substring(6));
            start = LocalDate.of(year, (q - 1) * 3 + 1, 1);
            end = start.plusMonths(3);
            currentLabel = quarter;
        } else if (period != null && period.matches("\\d{4}-Q[1-4]")) {
            mode = "quarter";
            int year = Integer.parseInt(period.substring(0, 4));
            int q = Integer.parseInt(period.substring(6));
            start = LocalDate.of(year, (q - 1) * 3 + 1, 1);
            end = start.plusMonths(3);
            currentLabel = period;
        } else {
            mode = "quarter".equalsIgnoreCase(viewType) ? "quarter" : "month";
            LocalDate p =
                    period == null || period.isBlank()
                            ? (periods.isEmpty()
                                    ? YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                                    : YearMonth.parse(periods.getFirst())).atDay(1)
                            : (period.length() == 7 ? YearMonth.parse(period) : YearMonth.from(LocalDate.parse(period))).atDay(1);
            if ("quarter".equals(mode)) {
                int q = (p.getMonthValue() - 1) / 3 + 1;
                start = LocalDate.of(p.getYear(), (q - 1) * 3 + 1, 1);
                end = start.plusMonths(3);
                currentLabel = p.getYear() + "-Q" + q;
            } else {
                start = p;
                end = p.plusMonths(1);
                currentLabel = p.toString().substring(0, 7);
            }
        }

        var result =
                new LinkedHashMap<String, Object>(
                        db.one(
                                "select count(*) total_rooms,count(*) filter(where"
                                        + " display_status='AVAILABLE') available_rooms,count(*)"
                                        + " filter(where display_status='RENTED') rented_rooms,count(*)"
                                        + " filter(where operational_status in"
                                        + " ('TURNOVER','MAINTENANCE')) maintenance_rooms from"
                                        + " v_room_status r where "
                                        + access.roomScope("r.id")));

        result.putAll(
                db.one(
                        "select coalesce(sum(i.charge_amount),0) billed,"
                                + "coalesce(sum(i.paid_amount),0) credited,"
                                + "coalesce(sum(i.balance_due),0) outstanding from"
                                + " v_item_balances i join contracts c on c.id=i.contract_id join"
                                + " billing_documents d on d.id=i.document_id where"
                                + " i.document_status='ISSUED' and "
                                + access.contractScope("c", true)
                                + " and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)>=?"
                                + " and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)<?",
                        start,
                        end));

        result.putAll(
                db.one(
                        "select " + PAYMENT_AMOUNTS
                                + "from billing_documents d join contracts c on c.id=d.contract_id"
                                + " join billing_items i on i.document_id=d.id"
                                + PAYMENT_APPLICATIONS
                                + "where d.status='ISSUED' and i.direction='DEBIT' and i.adjusts_item_id is null "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)>=? "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)<? "
                                + "and "
                                + access.contractScope("c", true),
                        start,
                        end));

        result.putAll(
                db.one(
                        "select coalesce(sum(i.charge_amount) filter(where cls.bucket='ELECTRICITY'),0) electricity,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='WATER'),0) water,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='ROOM_RENT'),0) room_rent,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='OTHER'),0) services_other "
                                + "from v_item_balances i join contracts c on c.id=i.contract_id "
                                + "join billing_documents d on d.id=i.document_id "
                                + ITEM_CLASSIFICATION
                                + "where i.document_status='ISSUED' "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)>=? "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)<? "
                                + "and "
                                + access.contractScope("c", true),
                        start,
                        end));

        BigDecimal invoiceCollected = (BigDecimal) result.get("credited");
        result.put("collected", invoiceCollected);
        result.put("unpaid", result.get("outstanding"));

        result.put(
                "cash_collected",
                db.jdbc.queryForObject(
                        "select coalesce(sum(tx.amount),0) from cash_transactions tx join"
                                + " cash_accounts a on a.id=tx.cash_account_id where"
                                + " tx.status='CONFIRMED' and tx.direction='IN' and"
                                + " (tx.occurred_at at time zone 'Asia/Ho_Chi_Minh')::date>=? and (tx.occurred_at at time zone 'Asia/Ho_Chi_Minh')::date<? and "
                                + cashScope(),
                        BigDecimal.class,
                        start,
                        end));
        result.put(
                "refunded",
                db.jdbc.queryForObject(
                        "select coalesce(sum(tx.amount),0) from cash_transactions tx join"
                                + " cash_accounts a on a.id=tx.cash_account_id where"
                                + " tx.status='CONFIRMED' and tx.direction='OUT' and"
                                + " (tx.occurred_at at time zone 'Asia/Ho_Chi_Minh')::date>=? and (tx.occurred_at at time zone 'Asia/Ho_Chi_Minh')::date<? and "
                                + cashScope(),
                        BigDecimal.class,
                        start,
                        end));

        result.put(
                "expiringContracts",
                db.rows(
                        "select c.*,r.room_code from v_contract_current c join rooms r on"
                                + " r.id=c.room_id where c.status='ACTIVE' and c.end_date between"
                                + " (now() at time zone 'Asia/Ho_Chi_Minh')::date and"
                                + " ((now() at time zone 'Asia/Ho_Chi_Minh')::date + 30) and "
                                + access.contractScope("c", true)));
        result.put(
                "open_maintenance",
                db.jdbc.queryForObject(
                        "select count(*) from v_maintenance m where m.status not in"
                                + " ('COMPLETED','CANCELLED') and ("
                                + access.roomScope("m.resolved_room_id")
                                + " or can_manage_property("
                                + access.user()
                                + ",m.resolved_property_id))",
                        Long.class));

        result.put("period", currentLabel);
        result.put("viewType", mode);
        result.put("startDate", start.toString());
        result.put("endDate", end.toString());

        result.put("available_periods", periods);
        return result;
    }

    @GetMapping("/reports/debts")
    public Object debts() {
        access.manager();
        return db.rows(
                "select d.document_code"
                        + " invoice_code,d.billing_period,r.room_code,t.full_name,sum(i.charge_amount)"
                        + " total_amount,sum(i.paid_amount) paid_amount,sum(i.balance_due)"
                        + " balance_due,d.due_date from v_item_balances i join billing_documents d on"
                        + " d.id=i.document_id join v_contract_current c on c.id=i.contract_id join"
                        + " rooms r on r.id=c.room_id left join tenant_profiles t on"
                        + " t.id=c.primary_tenant_id where i.document_status='ISSUED' and "
                        + access.contractScope("c", true)
                        + " group by"
                        + " d.id,d.document_code,d.billing_period,r.room_code,t.full_name,d.due_date"
                        + " having sum(i.balance_due)>0 order by d.due_date");
    }

    private List<Map<String, Object>> openTenantInvoices(Long tenantId) {
        return db.rows("""
                select d.id,d.document_code as invoice_code,d.billing_period,d.due_date,d.issue_date,
                       r.room_code,c.contract_code,t.id as tenant_id,t.full_name,t.phone,t.identity_number,
                       d.total_amount,d.paid_amount,d.balance_due,
                       greatest(0, (now() at time zone 'Asia/Ho_Chi_Minh')::date-d.due_date) as overdue_days
                from v_document_balances d
                join v_contract_current c on c.id=d.contract_id
                join rooms r on r.id=c.room_id
                join tenant_profiles t on t.id=c.primary_tenant_id
                where d.status='ISSUED' and d.balance_due>0 and
                """ + access.contractScope("c", true)
                + " and (?::bigint is null or t.id=?) order by d.due_date,d.id", tenantId, tenantId);
    }

    @GetMapping("/reports/tenant-debts")
    public List<Map<String, Object>> tenantDebts() {
        access.manager();
        var groups = new LinkedHashMap<Long, List<Map<String, Object>>>();
        for (var invoice : openTenantInvoices(null)) {
            long tenant = ((Number) invoice.get("tenant_id")).longValue();
            groups.computeIfAbsent(tenant, ignored -> new ArrayList<>()).add(invoice);
        }
        return groups.values().stream().map(invoices -> tenantBalance(invoices.getFirst(), invoices))
                .sorted(Comparator.<Map<String, Object>>comparingInt(t -> (int) t.get("max_overdue_days"))
                        .thenComparing(t -> (BigDecimal) t.get("total_debt")).reversed())
                .toList();
    }

    @GetMapping("/reports/tenant-debts/{id}")
    public Map<String, Object> tenantDebtDetail(@PathVariable long id) {
        access.manager();
        var tenant = catalog.get("tenants", id);
        tenant.put("tenant_id", id);
        return tenantBalance(tenant, openTenantInvoices(id));
    }

    private Map<String, Object> tenantBalance(
            Map<String, Object> tenant, List<Map<String, Object>> invoices) {
        var result = new LinkedHashMap<String, Object>();
        for (String field : List.of("tenant_id", "full_name", "phone", "identity_number")) {
            result.put(field, tenant.get(field));
        }
        var rooms = new TreeSet<String>();
        var contracts = new TreeSet<String>();
        BigDecimal billed = BigDecimal.ZERO, paid = BigDecimal.ZERO, debt = BigDecimal.ZERO;
        int overdue = 0;
        for (var invoice : invoices) {
            rooms.add(invoice.get("room_code").toString());
            contracts.add(invoice.get("contract_code").toString());
            billed = billed.add((BigDecimal) invoice.get("total_amount"));
            paid = paid.add((BigDecimal) invoice.get("paid_amount"));
            debt = debt.add((BigDecimal) invoice.get("balance_due"));
            overdue = Math.max(overdue, ((Number) invoice.get("overdue_days")).intValue());
        }
        result.put("room_codes", String.join(", ", rooms));
        result.put("contract_codes", String.join(", ", contracts));
        result.put("total_billed", billed);
        result.put("total_paid", paid);
        result.put("total_debt", debt);
        result.put("unpaid_invoice_count", invoices.size());
        result.put("earliest_due_date", invoices.isEmpty() ? null : invoices.getFirst().get("due_date"));
        result.put("max_overdue_days", overdue);
        result.put("invoices", invoices);
        return result;
    }

    @GetMapping("/reports/revenue")
    public List<Map<String, Object>> revenue() {
        access.manager();
        var rows =
                db.rows(
                        "with bills as ("
                                + "select to_char(coalesce(d.billing_period, d.issue_date), 'YYYY-MM') as period,"
                                + "count(distinct d.id) as invoice_count,"
                                + "sum(i.charge_amount) as billed,"
                                + "sum(i.paid_amount) as collected,"
                                + "sum(i.balance_due) as outstanding,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='ELECTRICITY'), 0) as electricity,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='WATER'), 0) as water,"
                                + "coalesce(sum(i.charge_amount) filter(where cls.bucket='ROOM_RENT'), 0) as room_rent "
                                + "from v_item_balances i "
                                + "join billing_documents d on d.id=i.document_id "
                                + "join contracts c on c.id=i.contract_id "
                                + ITEM_CLASSIFICATION
                                + "where i.document_status='ISSUED' and "
                                + access.contractScope("c", true)
                                + " group by period"
                                + "), timing as ("
                                + "select to_char(coalesce(d.billing_period, d.issue_date), 'YYYY-MM') as period,"
                                + PAYMENT_AMOUNTS
                                + "from billing_documents d"
                                + " join contracts c on c.id=d.contract_id"
                                + " join billing_items i on i.document_id=d.id"
                                + PAYMENT_APPLICATIONS
                                + "where d.status='ISSUED' and i.direction='DEBIT' and i.adjusts_item_id is null and "
                                + access.contractScope("c", true)
                                + " group by period"
                                + "), cash as ("
                                + "select to_char(tx.occurred_at at time zone 'Asia/Ho_Chi_Minh', 'YYYY-MM') as period,"
                                + "sum(tx.amount) filter(where tx.direction='IN') as cash_in,"
                                + "sum(tx.amount) filter(where tx.direction='OUT') as cash_out "
                                + "from cash_transactions tx "
                                + "join cash_accounts a on a.id=tx.cash_account_id "
                                + "where tx.status='CONFIRMED' and "
                                + cashScope()
                                + " group by period"
                                + ") "
                                + "select coalesce(b.period, t.period, c.period) as billing_period,"
                                + "coalesce(b.billed, 0) as billed,"
                                + "coalesce(b.collected, 0) as collected,"
                                + "coalesce(b.outstanding, 0) as outstanding,"
                                + "coalesce(t.paid_early, 0) as paid_early,"
                                + "coalesce(t.paid_on_time, 0) as paid_on_time,"
                                + "coalesce(t.paid_late, 0) as paid_late,"
                                + "coalesce(b.electricity, 0) as electricity,"
                                + "coalesce(b.water, 0) as water,"
                                + "coalesce(b.room_rent, 0) as room_rent,"
                                + "coalesce(c.cash_in, 0) as cash_in,"
                                + "coalesce(c.cash_out, 0) as cash_out,"
                                + "coalesce(b.invoice_count, 0) as invoice_count,"
                                + "round(case when coalesce(b.billed, 0) > 0 then (coalesce(b.collected, 0) / b.billed * 100) else 0 end, 1) as collection_rate "
                                + "from bills b "
                                + "full join timing t on t.period=b.period "
                                + "full join cash c on c.period=coalesce(b.period, t.period) "
                                + "order by billing_period");
        return rows;
    }

    @GetMapping("/reports/revenue-quarterly")
    public Collection<Map<String, Object>> quarterly() {
        var rows = revenue();
        var groups = new LinkedHashMap<String, Map<String, Object>>();
        for (var row : rows) {
            String pStr = row.get("billing_period").toString();
            YearMonth p = YearMonth.parse(pStr);
            String key = p.getYear() + "-Q" + ((p.getMonthValue() - 1) / 3 + 1);
            var out =
                    groups.computeIfAbsent(
                            key,
                            k -> {
                                var m = new LinkedHashMap<String, Object>();
                                m.put("quarter", k);
                                m.put("billing_period", k);
                                for (String f :
                                        List.of(
                                                "billed",
                                                "collected",
                                                "outstanding",
                                                "paid_early",
                                                "paid_on_time",
                                                "paid_late",
                                                "electricity",
                                                "water",
                                                "room_rent",
                                                "cash_in",
                                                "cash_out")) {
                                    m.put(f, BigDecimal.ZERO);
                                }
                                m.put("invoice_count", 0L);
                                m.put("collection_rate", 0.0);
                                return m;
                            });
            for (String f :
                    List.of(
                            "billed",
                            "collected",
                            "outstanding",
                            "paid_early",
                            "paid_on_time",
                            "paid_late",
                            "electricity",
                            "water",
                            "room_rent",
                            "cash_in",
                            "cash_out")) {
                BigDecimal current = (BigDecimal) out.get(f);
                BigDecimal add = (BigDecimal) row.getOrDefault(f, BigDecimal.ZERO);
                out.put(f, current.add(add));
            }
            long cnt = ((Number) out.get("invoice_count")).longValue();
            long addCnt = ((Number) row.getOrDefault("invoice_count", 0L)).longValue();
            out.put("invoice_count", cnt + addCnt);
        }
        for (var out : groups.values()) {
            BigDecimal billed = (BigDecimal) out.get("billed");
            BigDecimal collected = (BigDecimal) out.get("collected");
            double rate =
                    billed.signum() > 0
                            ? collected
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(billed, 1, java.math.RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
            out.put("collection_rate", rate);
        }
        return groups.values();
    }

    @GetMapping("/reports/revenue-yearly")
    public Collection<Map<String, Object>> yearly() {
        var rows = revenue();
        var groups = new LinkedHashMap<String, Map<String, Object>>();
        for (var row : rows) {
            String pStr = row.get("billing_period").toString();
            String year = pStr.substring(0, 4);
            var out =
                    groups.computeIfAbsent(
                            year,
                            k -> {
                                var m = new LinkedHashMap<String, Object>();
                                m.put("year", k);
                                m.put("billing_period", k);
                                for (String f :
                                        List.of(
                                                "billed",
                                                "collected",
                                                "outstanding",
                                                "paid_early",
                                                "paid_on_time",
                                                "paid_late",
                                                "electricity",
                                                "water",
                                                "room_rent",
                                                "cash_in",
                                                "cash_out")) {
                                    m.put(f, BigDecimal.ZERO);
                                }
                                m.put("invoice_count", 0L);
                                m.put("collection_rate", 0.0);
                                return m;
                            });
            for (String f :
                    List.of(
                            "billed",
                            "collected",
                            "outstanding",
                            "paid_early",
                            "paid_on_time",
                            "paid_late",
                            "electricity",
                            "water",
                            "room_rent",
                            "cash_in",
                            "cash_out")) {
                BigDecimal current = (BigDecimal) out.get(f);
                BigDecimal add = (BigDecimal) row.getOrDefault(f, BigDecimal.ZERO);
                out.put(f, current.add(add));
            }
            long cnt = ((Number) out.get("invoice_count")).longValue();
            long addCnt = ((Number) row.getOrDefault("invoice_count", 0L)).longValue();
            out.put("invoice_count", cnt + addCnt);
        }
        for (var out : groups.values()) {
            BigDecimal billed = (BigDecimal) out.get("billed");
            BigDecimal collected = (BigDecimal) out.get("collected");
            double rate =
                    billed.signum() > 0
                            ? collected
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(billed, 1, java.math.RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
            out.put("collection_rate", rate);
        }
        return groups.values();
    }

    @GetMapping("/reports/occupancy")
    public Object occupancy() {
        access.manager();
        return db.rows(
                "select p.id,p.name,count(r.id) total_rooms,count(r.id) filter(where"
                        + " r.display_status='RENTED') rented_rooms,round(100.0*count(r.id)"
                        + " filter(where r.display_status='RENTED')/nullif(count(r.id),0),2)"
                        + " occupancy_percentage from properties p join v_room_status r on"
                        + " r.property_id=p.id where "
                        + access.roomScope("r.id")
                        + " group by p.id,p.name order by p.id");
    }

    @GetMapping("/reports/occupancy-history")
    public Object occupancyHistory() {
        access.manager();
        return db.rows(
                "select p.name,to_char(m.month,'YYYY-MM') period,count(distinct r.id)"
                        + " total_rooms,sum(greatest(0,least(coalesce(r.retired_on,(m.month+interval '1"
                        + " month')::date),(m.month+interval '1"
                        + " month')::date)-greatest(r.opened_on,m.month::date)))"
                        + " available_room_days,coalesce(sum((select"
                        + " sum(greatest(0,least(coalesce(x.returned_at::date,(m.month+interval '1"
                        + " month')::date),(m.month+interval '1"
                        + " month')::date)-greatest(x.taken_at::date,m.month::date))) from"
                        + " room_possessions x join contracts c on c.id=x.contract_id where"
                        + " c.room_id=r.id)),0) occupied_room_days from properties p join rooms r on"
                        + " r.property_id=p.id cross join"
                        + " generate_series(date_trunc('month',current_date)-interval '11"
                        + " months',date_trunc('month',current_date),interval '1 month') m(month) where"
                        + " "
                        + access.roomScope("r.id")
                        + " group by p.id,p.name,m.month order by m.month,p.id");
    }

    @GetMapping("/reports/maintenance")
    public Object maintenance() {
        access.manager();
        return db.rows(
                "select to_char(m.created_at,'YYYY-MM') period,m.incident_type,m.status,count(*)"
                        + " total,sum(m.actual_cost) cost from v_maintenance m where ("
                        + access.roomScope("m.resolved_room_id")
                        + " or can_manage_property("
                        + access.user()
                        + ",m.resolved_property_id)) group by period,m.incident_type,m.status order"
                        + " by period");
    }

    @GetMapping(value = "/reports/debts.csv", produces = "text/csv;charset=UTF-8")
    public String csv() {
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) debts();
        StringBuilder out =
                new StringBuilder(
                        "\uFEFFMã chứng từ,Kỳ,Phòng,Người thuê,Tổng,Đã phân bổ,Nợ,Hạn\r\n");
        for (var r : rows) {
            out.append(
                            String.join(
                                    ",",
                                    r.values().stream()
                                            .map(
                                                    v -> {
                                                        String s = Objects.toString(v, "");
                                                        if (s.matches("^[=+@-].*")) {
                                                             s = "'" + s;
                                                        }
                                                        return "\""
                                                                + s.replace("\"", "\"\"")
                                                                + "\"";
                                                    })
                                            .toList()))
                    .append("\r\n");
        }
        return out.toString();
    }

    @GetMapping("/reports/revenue/pdf")
    public ResponseEntity<byte[]> revenuePdf(@RequestParam(defaultValue = "month") String group)
            throws java.io.IOException {
        var rows = switch (group) {
            case "month" -> revenue();
            case "quarter" -> quarterly();
            case "year" -> yearly();
            default -> throw new IllegalArgumentException("Invalid report grouping");
        };
        String periodLabel = switch (group) {
            case "quarter" -> "quý";
            case "year" -> "năm";
            default -> "tháng";
        };
        var lines = new ArrayList<String>();
        var number = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        for (var row : rows) {
            lines.add("Kỳ " + row.get("billing_period") + "  ·  " + row.get("invoice_count") + " chứng từ");
            lines.add("Lập hóa đơn: " + number.format(row.get("billed"))
                    + " đ   |   Đã thu: " + number.format(row.get("collected"))
                    + " đ   |   Còn nợ: " + number.format(row.get("outstanding")) + " đ");
            lines.add("Điện: " + number.format(row.get("electricity"))
                    + " đ   |   Nước: " + number.format(row.get("water")) + " đ");
            lines.add("Dòng tiền quỹ — Thực vào: " + number.format(row.get("cash_in"))
                    + " đ   |   Hoàn quỹ: " + number.format(row.get("cash_out")) + " đ");
            lines.add("");
        }
        if (lines.isEmpty()) lines.add("Chưa có số liệu cho báo cáo.");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=revenue-" + group + ".pdf")
                .body(pdf.render("BÁO CÁO DOANH THU THEO " + periodLabel.toUpperCase(Locale.forLanguageTag("vi-VN")), lines));
    }

    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> report() throws java.io.IOException {
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) debts();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=debts.pdf")
                .body(pdf.render("CÔNG NỢ ĐANG MỞ", rows.stream().map(Object::toString).toList()));
    }
}
