package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

/** Explicit resource/column allowlists; money and signed records use domain operations. */
@Service
@Profile("model49")
@Transactional
public class Catalog49 {
    record Spec(String table, String fields, String scope, boolean adminWrite) {}

    // Mỗi danh mục khai báo bảng, cột được ghi, phạm vi đọc và yêu cầu quyền ADMIN.
    private static final Map<String, Spec> SPECS =
            Map.ofEntries(
                    Map.entry(
                            "properties",
                            new Spec(
                                    "properties",
                                    "property_code name address status meter_cutoff_day"
                                            + " invoice_issue_day collection_timing",
                                    "property",
                                    true)),
                    Map.entry(
                            "rooms",
                            new Spec(
                                    "rooms",
                                    "property_id floor_number room_code max_occupants area_sqm"
                                            + " description internal_note opened_on retired_on",
                                    "room",
                                    false)),
                    Map.entry(
                            "tenants",
                            new Spec(
                                    "tenant_profiles",
                                    "user_id full_name identity_number phone email date_of_birth"
                                            + " hometown",
                                    "person",
                                    true)),
                    Map.entry(
                            "assignments",
                            new Spec(
                                    "employee_assignments",
                                    "user_id property_id room_id assigned_at revoked_at",
                                    "admin",
                                    true)),
                    Map.entry(
                            "staff-permissions",
                            new Spec(
                                    "staff_permission_grants",
                                    "assignment_id action_code amount_limit expires_at",
                                    "admin",
                                    true)),
                    Map.entry(
                            "room-prices",
                            new Spec(
                                    "room_price_history",
                                    "room_id effective_from amount",
                                    "room-child",
                                    true)),
                    Map.entry(
                            "room-operating-periods",
                            new Spec(
                                    "room_operating_periods",
                                    "room_id valid_from valid_to status note",
                                    "room-child",
                                    false)),
                    Map.entry(
                            "asset-types",
                            new Spec("asset_types", "code name unit_name", "manager", true)),
                    Map.entry(
                            "assets",
                            new Spec(
                                    "assets",
                                    "asset_type_id asset_code quantity status note",
                                    "asset",
                                    true)),
                    Map.entry(
                            "asset-placements",
                            new Spec(
                                    "asset_placements",
                                    "asset_id room_id property_id valid_from valid_to",
                                    "location",
                                    false)),
                    Map.entry(
                            "reservations",
                            new Spec(
                                    "reservations",
                                    "room_id tenant_id reservation_code contact_name contact_phone"
                                            + " start_date end_date expires_at status conditions",
                                    "room-child",
                                    false)),
                    Map.entry(
                            "parties",
                            new Spec(
                                    "contract_parties",
                                    "contract_id tenant_id party_role effective_from effective_to"
                                            + " agreement_note",
                                    "contract-child",
                                    false)),
                    Map.entry(
                            "subscriptions",
                            new Spec(
                                    "contract_service_subscriptions",
                                    "contract_id service_item_id effective_from effective_to"
                                            + " quantity charge_mode",
                                    "contract-child",
                                    false)),
                    Map.entry(
                            "handovers",
                            new Spec(
                                    "handovers",
                                    "contract_id possession_id handover_code handover_type"
                                            + " occurred_at status given_by received_by_tenant_id"
                                            + " confirmed_at note",
                                    "contract-child",
                                    false)),
                    Map.entry(
                            "handover-items",
                            new Spec(
                                    "handover_items",
                                    "handover_id asset_placement_id line_no item_name_snapshot"
                                            + " quantity condition_note note",
                                    "handover-item",
                                    false)),
                    Map.entry(
                            "violations",
                            new Spec(
                                    "contract_violations",
                                    "contract_id violation_type description occurred_on status"
                                            + " resolution_note",
                                    "contract-child",
                                    false)),
                    Map.entry(
                            "services",
                            new Spec(
                                    "service_items",
                                    "service_code name unit_name calculation_type is_mandatory"
                                            + " is_active",
                                    "manager",
                                    true)),
                    Map.entry(
                            "prices",
                            new Spec(
                                    "service_price_configs",
                                    "property_id service_item_id effective_from unit_price",
                                    "property-child",
                                    true)),
                    Map.entry(
                            "meters",
                            new Spec(
                                    "meters",
                                    "room_id service_item_id meter_code installed_at retired_at",
                                    "room-child",
                                    false)),
                    Map.entry(
                            "thresholds",
                            new Spec(
                                    "alert_policies",
                                    "property_id room_id service_item_id effective_from"
                                            + " history_months minimum_days threshold_percentage"
                                            + " absolute_daily_threshold",
                                    "location",
                                    true)),
                    Map.entry(
                            "maintenance-schedules",
                            new Spec(
                                    "maintenance_schedules",
                                    "asset_id room_id property_id title interval_days next_due"
                                            + " is_active",
                                    "schedule",
                                    false)),
                    Map.entry(
                            "cash-accounts",
                            new Spec(
                                    "cash_accounts",
                                    "property_id account_code name account_type is_active",
                                    "cash-account",
                                    true)));
    private final Database db;
    private final Access49 access;
    private final Events49 events;

    public Catalog49(Database db, Access49 access, Events49 events) {
        this.db = db;
        this.access = access;
        this.events = events;
    }

    private Spec spec(String resource) {
        var s = SPECS.get(resource);
        require(s != null, "Unknown catalog resource");
        return s;
    }

    /** Sinh điều kiện lọc dữ liệu theo loại danh mục và quyền của tài khoản. */
    private String scope(Spec s) {
        boolean tenant = access.role().equals("TENANT");
        return switch (s.scope()) {
            case "admin" -> {
                access.admin();
                yield "true";
            }
            case "manager" -> {
                access.manager();
                yield "true";
            }
            case "room" -> access.roomScope("x.id");
            case "room-child" -> {
                access.manager();
                yield access.roomScope("x.room_id");
            }
            case "property" -> {
                access.manager();
                yield access.propertyScope("x.id");
            }
            case "property-child" -> {
                access.manager();
                yield access.propertyScope("x.property_id");
            }
            case "cash-account" -> {
                access.manager();
                yield access.role().equals("ADMIN")
                        ? "true"
                        : access.propertyScope("x.property_id");
            }
            case "person" ->
                    tenant
                            ? "x.user_id=" + access.user()
                            : access.role().equals("ADMIN")
                                    ? "true"
                                    : "exists(select 1 from contract_parties p join contracts c on"
                                            + " c.id=p.contract_id where p.tenant_id=x.id and "
                                            + access.contractScope("c", true)
                                            + ") or exists(select 1 from contract_residencies p"
                                            + " join contracts c on c.id=p.contract_id where"
                                            + " p.tenant_id=x.id and "
                                            + access.contractScope("c", false)
                                            + ")";
            case "contract-child" ->
                    "exists(select 1 from contracts c where c.id=x.contract_id and "
                            + access.contractScope("c", true)
                            + ")";
            case "handover-item" ->
                    "exists(select 1 from handovers h join contracts c on c.id=h.contract_id where"
                            + " h.id=x.handover_id and "
                            + access.contractScope("c", true)
                            + ")";
            case "location" -> {
                access.manager();
                yield "("
                        + access.roomScope("x.room_id")
                        + " or can_manage_property("
                        + access.user()
                        + ",x.property_id))";
            }
            case "asset" -> {
                access.manager();
                yield access.role().equals("ADMIN")
                        ? "true"
                        : "exists(select 1 from asset_placements p where p.asset_id=x.id and ("
                                + access.roomScope("p.room_id")
                                + " or can_manage_property("
                                + access.user()
                                + ",p.property_id)))";
            }
            case "schedule" -> {
                access.manager();
                yield access.role().equals("ADMIN")
                        ? "true"
                        : "("
                                + access.roomScope("x.room_id")
                                + " or can_manage_property("
                                + access.user()
                                + ",x.property_id) or exists(select 1 from asset_placements p where"
                                + " p.asset_id=x.asset_id and ("
                                + access.roomScope("p.room_id")
                                + " or can_manage_property("
                                + access.user()
                                + ",p.property_id))))";
            }
            default -> "false";
        };
    }

    public List<Map<String, Object>> list(String resource) {
        var s = spec(resource);
        String source = resource.equals("rooms") ? "v_room_status" : s.table();
        var rows =
                db.rows(
                        "select x.* from "
                                + source
                                + " x where "
                                + scope(s)
                                + " order by x.id desc limit 1000");
        if (access.role().equals("TENANT")) {
            rows.forEach(r -> r.remove("internal_note"));
        }
        if (resource.equals("rooms")) {
            for (var r : rows) {
                r.put("status", r.get("display_status"));
                r.put("floor_id", r.get("floor_number"));
            }
        }
        if (resource.equals("assignments")) {
            for (var r : rows) {
                r.put("is_active", r.get("revoked_at") == null);
            }
        }
        if (resource.equals("prices")) {
            for (var r : rows) {
                var service =
                        db.one(
                                "select name,calculation_type from service_items where id=?",
                                r.get("service_item_id"));
                r.putAll(service);
            }
        }
        return rows;
    }

    public Map<String, Object> get(String resource, long id) {
        var s = spec(resource);
        var row =
                db.one(
                        "select x.* from " + s.table() + " x where x.id=? and (" + scope(s) + ")",
                        id);
        if (access.role().equals("TENANT")) {
            row.remove("internal_note");
        }
        return row;
    }

    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    public Map<String, Object> save(String resource, Long id, Map<String, Object> input) {
        // 1. Kiểm tra quyền ghi và quyền truy cập bản ghi hiện có nếu đang cập nhật.
        var s = spec(resource);
        access.manager();
        if (s.adminWrite()) {
            access.admin();
        }
        if (resource.equals("rooms") && id == null) {
            access.admin();
        }
        Map<String, Object> before = id == null ? null : get(resource, id);
        if (id != null) {
            required(input, "reason");
        }
        var values = new LinkedHashMap<String, Object>();
        Set<String> allowed = new HashSet<>(List.of(s.fields().split(" ")));

        // 2. Chuyển camelCase sang snake_case và chỉ nhận các cột đã được cho phép.
        for (var e : input.entrySet()) {
            String key = snake(e.getKey());
            if (key.equals("reason")) {
                continue;
            }
            require(allowed.contains(key), "Field is not writable: " + key);
            values.put(key, e.getValue());
        }
        require(!values.isEmpty(), "No writable fields provided");
        if (resource.equals("parties")) {
            String party =
                    text(
                            values,
                            before == null ? "" : before.get("party_role").toString(),
                            "party_role");
            require(
                    !"PRIMARY".equals(party)
                            && (before == null || !"PRIMARY".equals(before.get("party_role"))),
                    "Changing primary responsibility requires a successor contract");
        }
        if (values.get("tenant_id") != null && !access.role().equals("ADMIN")) {
            long tenant = Long.parseLong(values.get("tenant_id").toString());
            require(
                    db.jdbc.queryForObject(
                                    "select count(*) from contract_parties p join contracts c on"
                                            + " c.id=p.contract_id where p.tenant_id=? and "
                                            + access.contractScope("c", true),
                                    Long.class,
                                    tenant)
                            > 0,
                    "Existing person must be within staff scope; new people use contract intake");
        }
        // 3. Đọc kiểu cột để ép kiểu tham số SQL và tự ghi nhận người thao tác.
        var columns =
                db.rows(
                        "select column_name,data_type from information_schema.columns where"
                                + " table_schema='public' and table_name=?",
                        s.table());
        var types = new HashMap<String, String>();
        for (var c : columns) {
            types.put(c.get("column_name").toString(), c.get("data_type").toString());
        }
        for (String actor : List.of("recorded_by", "created_by", "granted_by", "authorized_by")) {
            if (types.containsKey(actor) && id == null) {
                values.put(actor, access.user());
            }
        }
        if (resource.equals("handovers") && "CONFIRMED".equals(values.get("status"))) {
            values.put("confirmed_by", access.user());
        }
        var casts = new ArrayList<String>();
        var args = new ArrayList<Object>();
        for (var e : values.entrySet()) {
            String type = types.get(e.getKey());
            require(type != null, "Invalid column");
            String cast =
                    switch (type) {
                        case "bigint" -> "bigint";
                        case "integer" -> "integer";
                        case "smallint" -> "smallint";
                        case "numeric" -> "numeric";
                        case "boolean" -> "boolean";
                        case "date" -> "date";
                        case "timestamp with time zone" -> "timestamptz";
                        default -> "text";
                    };
            casts.add("?::" + cast);
            args.add(
                    e.getValue() == null
                            ? null
                            : type.equals("timestamp with time zone")
                                    ? time(e.getValue()).toString()
                                    : e.getValue().toString());
        }
        if (id == null) {
            id =
                    db.jdbc.queryForObject(
                            "insert into "
                                    + s.table()
                                    + " ("
                                    + String.join(",", values.keySet())
                                    + ") values ("
                                    + String.join(",", casts)
                                    + ") returning id",
                            Long.class,
                            args.toArray());
        } else {
            var sets = new ArrayList<String>();
            int n = 0;
            for (String field : values.keySet()) {
                sets.add(field + "=" + casts.get(n++));
            }
            args.add(id);
            db.jdbc.update(
                    "update " + s.table() + " set " + String.join(",", sets) + " where id=?",
                    args.toArray());
        }
        var after = get(resource, id);
        events.audit(
                before == null ? "CREATE" : "UPDATE",
                s.table(),
                id,
                before,
                after,
                text(input, "", "reason"));
        return after;
    }
}
