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

    public Reports49(Database db, Access49 access, PdfService pdf) {
        this.db = db;
        this.access = access;
        this.pdf = pdf;
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

    @GetMapping("/dashboard")
    public Object dashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String viewType) {
        access.manager();
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
                            ? YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh")).atDay(1)
                            : YearMonth.parse(period.substring(0, 7)).atDay(1);
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
                        "select coalesce(sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date < d.due_date then e.amount else 0 end),0) paid_early,"
                                + "coalesce(sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date = d.due_date then e.amount else 0 end),0) paid_on_time,"
                                + "coalesce(sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date > d.due_date then e.amount else 0 end),0) paid_late "
                                + "from billing_documents d join contracts c on c.id=d.contract_id "
                                + "join billing_items i on i.document_id=d.id "
                                + "join fund_entries e on e.debit_item_id=i.id and e.entry_type='PAYMENT' "
                                + "where d.status='ISSUED' and i.direction='DEBIT' and i.adjusts_item_id is null "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)>=? "
                                + "and coalesce(d.billing_period,date_trunc('month',d.issue_date)::date)<? "
                                + "and "
                                + access.contractScope("c", true),
                        start,
                        end));

        result.putAll(
                db.one(
                        "select coalesce(sum(case when (exists(select 1 from v_segments seg join service_items si on si.id=seg.service_item_id where seg.id=i.segment_id and (si.service_code in ('ELEC','ELECTRICITY') or lower(si.name) like '%điện%')) or lower(i.item_name_snapshot) like '%điện%') then i.charge_amount else 0 end),0) electricity,"
                                + "coalesce(sum(case when (exists(select 1 from v_segments seg join service_items si on si.id=seg.service_item_id where seg.id=i.segment_id and (si.service_code in ('WATER') or lower(si.name) like '%nước%')) or lower(i.item_name_snapshot) like '%nước%') then i.charge_amount else 0 end),0) water,"
                                + "coalesce(sum(case when i.item_kind='ROOM_RENT' then i.charge_amount else 0 end),0) room_rent,"
                                + "coalesce(sum(case when i.item_kind not in ('ROOM_RENT') and not (lower(i.item_name_snapshot) like '%điện%' or lower(i.item_name_snapshot) like '%nước%') then i.charge_amount else 0 end),0) services_other "
                                + "from v_item_balances i join contracts c on c.id=i.contract_id "
                                + "join billing_documents d on d.id=i.document_id "
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
                                + " current_date and current_date+30 and "
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

        var periods =
                db.rows(
                        "select distinct to_char(coalesce(d.billing_period, d.issue_date), 'YYYY-MM') as p "
                                + "from billing_documents d join contracts c on c.id=d.contract_id "
                                + "where d.status='ISSUED' and "
                                + access.contractScope("c", true)
                                + " order by p desc");
        result.put("available_periods", periods.stream().map(m -> m.get("p")).toList());

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

    @GetMapping("/reports/tenant-debts")
    public Object tenantDebts() {
        access.manager();
        var tenants =
                db.rows(
                        "select t.id as tenant_id,t.full_name,t.phone,t.identity_number,"
                                + "string_agg(distinct r.room_code, ', ' order by r.room_code) as room_codes,"
                                + "string_agg(distinct c.contract_code, ', ' order by c.contract_code) as contract_codes,"
                                + "sum(i.charge_amount) as total_billed,"
                                + "sum(i.paid_amount) as total_paid,"
                                + "sum(i.balance_due) as total_debt,"
                                + "count(distinct d.id) as unpaid_invoice_count,"
                                + "min(d.due_date) as earliest_due_date,"
                                + "max(greatest(0, current_date - d.due_date)) as max_overdue_days "
                                + "from v_item_balances i "
                                + "join billing_documents d on d.id=i.document_id "
                                + "join v_contract_current c on c.id=i.contract_id "
                                + "join rooms r on r.id=c.room_id "
                                + "join tenant_profiles t on t.id=c.primary_tenant_id "
                                + "where i.document_status='ISSUED' and "
                                + access.contractScope("c", true)
                                + " group by t.id,t.full_name,t.phone,t.identity_number "
                                + "having sum(i.balance_due)>0 "
                                + "order by max(greatest(0, current_date - d.due_date)) desc, sum(i.balance_due) desc");

        for (var t : tenants) {
            long tenantId = ((Number) t.get("tenant_id")).longValue();
            var invoices =
                    db.rows(
                            "select d.id,d.document_code as invoice_code,d.billing_period,d.due_date,d.issue_date,"
                                    + "r.room_code,c.contract_code,"
                                    + "sum(i.charge_amount) as total_amount,"
                                    + "sum(i.paid_amount) as paid_amount,"
                                    + "sum(i.balance_due) as balance_due,"
                                    + "greatest(0, current_date - d.due_date) as overdue_days "
                                    + "from v_item_balances i "
                                    + "join billing_documents d on d.id=i.document_id "
                                    + "join v_contract_current c on c.id=i.contract_id "
                                    + "join rooms r on r.id=c.room_id "
                                    + "where i.document_status='ISSUED' and c.primary_tenant_id=? and "
                                    + access.contractScope("c", true)
                                    + " group by d.id,d.document_code,d.billing_period,d.due_date,d.issue_date,r.room_code,c.contract_code "
                                    + "having sum(i.balance_due)>0 "
                                    + "order by d.due_date asc",
                            tenantId);
            t.put("invoices", invoices);
        }
        return tenants;
    }

    @GetMapping("/reports/tenant-debts/{id}")
    public Object tenantDebtDetail(@PathVariable long id) {
        access.manager();
        var rows = (List<Map<String, Object>>) tenantDebts();
        for (var row : rows) {
            if (((Number) row.get("tenant_id")).longValue() == id) {
                return row;
            }
        }
        var tenant = db.one("select id as tenant_id,full_name,phone,identity_number from tenant_profiles where id=?", id);
        tenant.put("room_codes", "—");
        tenant.put("contract_codes", "—");
        tenant.put("total_billed", BigDecimal.ZERO);
        tenant.put("total_paid", BigDecimal.ZERO);
        tenant.put("total_debt", BigDecimal.ZERO);
        tenant.put("unpaid_invoice_count", 0);
        tenant.put("earliest_due_date", null);
        tenant.put("max_overdue_days", 0);
        tenant.put("invoices", List.of());
        return tenant;
    }

    @GetMapping("/reports/revenue")
    public Object revenue() {
        access.manager();
        var rows =
                db.rows(
                        "with bills as ("
                                + "select to_char(coalesce(d.billing_period, d.issue_date), 'YYYY-MM') as period,"
                                + "count(distinct d.id) as invoice_count,"
                                + "sum(i.charge_amount) as billed,"
                                + "sum(i.paid_amount) as collected,"
                                + "sum(i.balance_due) as outstanding,"
                                + "sum(case when (exists(select 1 from v_segments seg join service_items si on si.id=seg.service_item_id where seg.id=i.segment_id and (si.service_code in ('ELEC','ELECTRICITY') or lower(si.name) like '%điện%')) or lower(i.item_name_snapshot) like '%điện%') then i.charge_amount else 0 end) as electricity,"
                                + "sum(case when (exists(select 1 from v_segments seg join service_items si on si.id=seg.service_item_id where seg.id=i.segment_id and (si.service_code in ('WATER') or lower(si.name) like '%nước%')) or lower(i.item_name_snapshot) like '%nước%') then i.charge_amount else 0 end) as water,"
                                + "sum(case when i.item_kind='ROOM_RENT' then i.charge_amount else 0 end) as room_rent "
                                + "from v_item_balances i "
                                + "join billing_documents d on d.id=i.document_id "
                                + "join contracts c on c.id=i.contract_id "
                                + "where i.document_status='ISSUED' and "
                                + access.contractScope("c", true)
                                + " group by period"
                                + "), timing as ("
                                + "select to_char(coalesce(d.billing_period, d.issue_date), 'YYYY-MM') as period,"
                                + "sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date < d.due_date then e.amount else 0 end) as paid_early,"
                                + "sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date = d.due_date then e.amount else 0 end) as paid_on_time,"
                                + "sum(case when (e.posted_at at time zone 'Asia/Ho_Chi_Minh')::date > d.due_date then e.amount else 0 end) as paid_late "
                                + "from billing_documents d "
                                + "join contracts c on c.id=d.contract_id "
                                + "join billing_items i on i.document_id=d.id "
                                + "join fund_entries e on e.debit_item_id=i.id and e.entry_type='PAYMENT' "
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
    public Object quarterly() {
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) revenue();
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
    public Object yearly() {
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) revenue();
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
