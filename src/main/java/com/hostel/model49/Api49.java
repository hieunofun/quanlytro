package com.hostel.model49;

import com.hostel.service.ContractPdf;
import com.hostel.service.Database;
import com.hostel.service.PdfService;
import com.hostel.dto.Requests;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@RestController
@RequestMapping("/api")
@Profile("model49")
@Transactional
public class Api49 {
    private final Database db;
    private final Access49 access;
    private final Catalog49 catalog;
    private final Leases49 leases;
    private final Finance49 finance;
    private final Billing49 billing;
    private final Meters49 meters;
    private final Operations49 operations;
    private final Events49 events;
    private final PasswordEncoder passwords;
    private final PdfService pdf;
    private final Settlements49 settlements;

    public Api49(
            Database db,
            Access49 access,
            Catalog49 catalog,
            Leases49 leases,
            Finance49 finance,
            Billing49 billing,
            Meters49 meters,
            Operations49 operations,
            Events49 events,
            PasswordEncoder passwords,
            PdfService pdf,
            Settlements49 settlements) {
        this.db = db;
        this.access = access;
        this.catalog = catalog;
        this.leases = leases;
        this.finance = finance;
        this.billing = billing;
        this.meters = meters;
        this.operations = operations;
        this.events = events;
        this.passwords = passwords;
        this.pdf = pdf;
        this.settlements = settlements;
    }

    @GetMapping("/users")
    public Object users() {
        access.admin();
        return db.rows(
                "select id,username,username full_name,role,status,created_at from users order by"
                        + " id");
    }

    @PostMapping("/users")
    public Object createUser(@Valid @RequestBody Requests.User r) {
        access.admin();
        long id =
                db.insert(
                        "users",
                        "username,password_hash,role",
                        r.username(),
                        passwords.encode(r.password()),
                        r.role());
        return Map.of("id", id);
    }

    @PutMapping("/users/{id}/status")
    public Object userStatus(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.admin();
        require(id != access.user(), "Cannot lock own account");
        db.jdbc.update("update users set status=? where id=?", required(m, "status"), id);
        return Map.of("id", id);
    }

    @GetMapping("/catalog/{resource}")
    public Object catalog(@PathVariable String resource) {
        return catalog.list(resource);
    }

    @GetMapping("/catalog/{resource}/{id}")
    public Object catalogGet(@PathVariable String resource, @PathVariable long id) {
        return catalog.get(resource, id);
    }

    @PostMapping("/catalog/{resource}")
    public Object catalogCreate(@PathVariable String resource, @RequestBody Map<String, Object> m) {
        return catalog.save(resource, null, m);
    }

    @PutMapping("/catalog/{resource}/{id}")
    public Object catalogUpdate(
            @PathVariable String resource,
            @PathVariable long id,
            @RequestBody Map<String, Object> m) {
        return catalog.save(resource, id, m);
    }

    @GetMapping({
        "/properties",
        "/rooms",
        "/tenants",
        "/services",
        "/prices",
        "/assignments",
        "/thresholds",
        "/maintenance-schedules",
        "/cash-accounts",
        "/reservations"
    })
    public Object collection(HttpServletRequest request) {
        String resource = request.getRequestURI().substring(5);
        var rows = catalog.list(resource);
        if (resource.equals("properties")) {
            for (var r : rows) {
                var p =
                        db.rows(
                                "select threshold_percentage,history_months from alert_policies"
                                        + " where property_id=? order by effective_from desc limit 1",
                                r.get("id"));
                r.put(
                        "alert_threshold_percentage",
                        p.isEmpty() ? 50 : p.getFirst().get("threshold_percentage"));
                r.put("alert_history_months", p.isEmpty() ? 3 : p.getFirst().get("history_months"));
            }
        }
        if (resource.equals("assignments")) {
            for (var r : rows) {
                r.put(
                        "full_name",
                        db.one("select username from users where id=?", r.get("user_id"))
                                .get("username"));
                r.put(
                        "property_name",
                        r.get("property_id") != null
                                ? db.one(
                                                "select name from properties where id=?",
                                                r.get("property_id"))
                                        .get("name")
                                : "Phòng "
                                        + db.one(
                                                        "select room_code from rooms where id=?",
                                                        r.get("room_id"))
                                                .get("room_code"));
            }
        }
        return rows;
    }

    private Map<String, Object> rename(Map<String, Object> m, String... pairs) {
        var x = new LinkedHashMap<>(m);
        for (int i = 0; i < pairs.length; i += 2) {
            if (x.containsKey(pairs[i])) {
                x.put(pairs[i + 1], x.remove(pairs[i]));
            }
        }
        return x;
    }

    @PostMapping("/properties")
    public Object property(@RequestBody Map<String, Object> m) {
        var p = catalog.save("properties", null, rename(m, "code", "propertyCode"));
        db.insert(
                "cash_accounts",
                "property_id,account_code,name,account_type",
                p.get("id"),
                "CASH-" + p.get("id"),
                "Quỹ tiền mặt " + p.get("name"),
                "CASH");
        return p;
    }

    @PutMapping("/properties/{id}")
    public Object property(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var x = rename(m, "code", "propertyCode");
        x.putIfAbsent("reason", "Operator property update");
        return catalog.save("properties", id, x);
    }

    @PutMapping("/properties/{id}/status")
    public Object propertyStatus(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var x = new HashMap<>(m);
        x.putIfAbsent("reason", "Operator status update");
        return catalog.save("properties", id, x);
    }

    @PutMapping("/properties/{id}/billing-policy")
    public Object propertyPolicy(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var x = new HashMap<>(m);
        x.putIfAbsent("reason", "Operator billing policy update");
        return catalog.save("properties", id, x);
    }

    @PostMapping("/rooms")
    public Object room(@RequestBody Map<String, Object> m) {
        access.admin();
        var x = rename(m, "code", "roomCode", "area", "areaSqm");
        BigDecimal price = amount(x, "0", "price");
        x.remove("price");
        var r = catalog.save("rooms", null, x);
        db.insert(
                "room_price_history",
                "room_id,effective_from,amount,recorded_by",
                r.get("id"),
                date(r.get("opened_on")),
                price,
                access.user());
        db.insert(
                "room_operating_periods",
                "room_id,valid_from,status,recorded_by",
                r.get("id"),
                time(r.get("opened_on")),
                "READY",
                access.user());
        return access.room(com.hostel.service.Database.id(r, "id"));
    }

    @GetMapping("/rooms/{id}")
    public Object room(@PathVariable long id) {
        var r = new LinkedHashMap<>(access.room(id));
        r.put("assets", roomAssets(id));
        return r;
    }

    @GetMapping("/rooms/{id}/assets")
    public Object roomAssets(@PathVariable long id) {
        access.room(id);
        return db.rows(
                "select ap.id placement_id, ap.asset_id, a.asset_code, a.quantity, a.status, a.note asset_note,"
                        + " at.name asset_type_name, at.code asset_type_code, at.unit_name,"
                        + " ap.valid_from, ap.valid_to"
                        + " from asset_placements ap"
                        + " join assets a on a.id=ap.asset_id"
                        + " join asset_types at on at.id=a.asset_type_id"
                        + " where ap.room_id=? and (ap.valid_to is null or ap.valid_to > clock_timestamp())"
                        + " order by at.name, a.asset_code",
                id);
    }

    @PostMapping("/rooms/{id}/assets")
    public Object addRoomAsset(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        access.room(id);
        Long assetId = optionalId(m, "assetId");
        if (assetId == null) {
            Long typeId = keyId(m, "assetTypeId");
            String code = text(m, "AST-" + id + "-" + (System.currentTimeMillis() % 100000), "assetCode");
            BigDecimal qty = amount(m, "1", "quantity");
            String note = text(m, "", "note");
            assetId = db.insert("assets", "asset_type_id,asset_code,quantity,status,note", typeId, code, qty, "ACTIVE", note);
        }
        OffsetDateTime placedAt = value(m, "validFrom") == null ? now() : time(value(m, "validFrom"));
        require(!placedAt.isAfter(now()), "Thời điểm bố trí phải là thời điểm đã xảy ra");
        long placementId = db.insert(
                "asset_placements",
                "asset_id,room_id,valid_from,recorded_by",
                assetId, id, placedAt, access.user());
        events.audit("PLACE_ASSET", "asset_placements", placementId, null, m, "Asset placed in room " + id);
        return roomAssets(id);
    }

    @PostMapping("/rooms/{id}/assets/{placementId}/remove")
    public Object removeRoomAsset(@PathVariable long id, @PathVariable long placementId, @RequestBody(required = false) Map<String, Object> m) {
        access.manager();
        access.room(id);
        var old = db.one("select * from asset_placements where id=? and room_id=? and valid_to is null", placementId, id);
        db.jdbc.update("update asset_placements set valid_to=clock_timestamp() where id=?", placementId);
        events.audit("REMOVE_ASSET", "asset_placements", placementId, old, m, "Asset removed from room " + id);
        return roomAssets(id);
    }

    @PutMapping("/rooms/{id}")
    public Object room(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var x = rename(m, "code", "roomCode", "area", "areaSqm");
        Object price = x.remove("price");
        x.putIfAbsent("reason", "Operator room update");
        var r = catalog.save("rooms", id, x);
        if (price != null) {
            access.admin();
            var old =
                    db.rows(
                            "select * from room_price_history where room_id=? and"
                                    + " effective_from=current_date",
                            id);
            db.jdbc.update(
                    "insert into room_price_history(room_id,effective_from,amount,recorded_by)"
                            + " values(?,current_date,?,?) on conflict(room_id,effective_from) do"
                            + " update set amount=excluded.amount,recorded_by=excluded.recorded_by",
                    id,
                    new BigDecimal(price.toString()),
                    access.user());
            events.audit(
                    "LIST_PRICE",
                    "rooms",
                    id,
                    old,
                    Map.of("price", price),
                    "Current-day price correction; signed contracts unaffected");
        }
        return access.room(id);
    }

    @PutMapping("/rooms/{id}/status")
    public Object roomStatus(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        access.room(id);
        var at = now();
        db.one("select id from rooms where id=? for update", id);
        db.jdbc.update(
                "update room_operating_periods set valid_to=? where room_id=? and valid_to is null",
                at,
                id);
        db.insert(
                "room_operating_periods",
                "room_id,valid_from,status,recorded_by,note",
                id,
                at,
                text(m, "READY", "status").equals("AVAILABLE") ? "READY" : required(m, "status"),
                access.user(),
                text(m, "Operator readiness update", "reason"));
        return access.room(id);
    }

    @DeleteMapping("/rooms/{id}")
    public Object deleteRoom(@PathVariable long id) {
        access.admin();
        access.room(id);
        require(
                db.jdbc.queryForObject(
                                "select count(*) from contracts where room_id=?", Long.class, id)
                        == 0,
                "Room has tenancy history; retire instead of deleting");
        db.jdbc.update("delete from room_price_history where room_id=?", id);
        db.jdbc.update("delete from room_operating_periods where room_id=?", id);
        db.jdbc.update("delete from rooms where id=?", id);
        return Map.of("id", id);
    }

    @PostMapping("/tenants")
    public Object person(@RequestBody Map<String, Object> m) {
        return catalog.save("tenants", null, m);
    }

    @PutMapping({"/tenants/{id}", "/tenants/{id}/profile"})
    public Object person(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var x = new HashMap<>(m);
        x.putIfAbsent("reason", "Verified profile update");
        return catalog.save("tenants", id, x);
    }

    @PostMapping("/services")
    public Object service(@RequestBody Map<String, Object> m) {
        return catalog.save("services", null, rename(m, "code", "serviceCode"));
    }

    @PostMapping("/prices")
    public Object price(@RequestBody Map<String, Object> m) {
        return catalog.save("prices", null, rename(m, "serviceId", "serviceItemId"));
    }

    @PostMapping("/assignments")
    public Object assignment(@RequestBody Map<String, Object> m) {
        return catalog.save("assignments", null, m);
    }

    @DeleteMapping("/assignments/{id}")
    public Object unassign(@PathVariable long id) {
        return catalog.save(
                "assignments", id, Map.of("revokedAt", now(), "reason", "Assignment withdrawn"));
    }

    @PostMapping("/thresholds")
    public Object threshold(@RequestBody Map<String, Object> m) {
        access.admin();
        var results = new ArrayList<Object>();
        for (var s : db.rows("select id from service_items where calculation_type='METER_BASED'")) {
            var x = new HashMap<String, Object>();
            x.put("propertyId", keyId(m, "propertyId"));
            x.put("serviceItemId", s.get("id"));
            x.put("effectiveFrom", day(m, now().toLocalDate(), "effectiveFrom"));
            x.put("thresholdPercentage", amount(m, "50", "percentage"));
            x.put("historyMonths", Integer.parseInt(text(m, "3", "months")));
            results.add(catalog.save("thresholds", null, x));
        }
        return results;
    }

    @GetMapping("/floors")
    public Object floors() {
        access.manager();
        return db.rows(
                "select distinct floor_number id,floor_number,property_id from rooms r where "
                        + access.roomScope("r.id")
                        + " order by property_id,floor_number");
    }

    @GetMapping("/contracts")
    public Object contracts() {
        return db.rows(
                "select c.*,r.room_code,t.full_name from v_contract_current c join rooms r on"
                        + " r.id=c.room_id left join tenant_profiles t on t.id=c.primary_tenant_id"
                        + " where "
                        + access.contractScope("c", true)
                        + " order by c.id desc");
    }

    @GetMapping("/contracts/{id}")
    public Object contract(@PathVariable long id) {
        return leases.detail(id);
    }

    @PostMapping("/contracts")
    public Object contract(@RequestBody Map<String, Object> m) {
        return leases.create(m);
    }

    @PostMapping("/contracts/{id}/activate")
    public Object activate(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.activate(id, m);
    }

    @PostMapping("/contracts/{id}/renew")
    public Object renew(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.renew(id, m);
    }

    @PostMapping("/contracts/{id}/members")
    public Object resident(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.reside(id, m);
    }

    @PostMapping("/contracts/{id}/members/{memberId}/depart")
    public Object depart(
            @PathVariable long id,
            @PathVariable long memberId,
            @RequestBody Map<String, Object> m) {
        return leases.depart(id, memberId, m);
    }

    @PostMapping("/contracts/{id}/contacts")
    public Object contact(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.contact(id, m);
    }

    @PostMapping("/contracts/{id}/possessions")
    public Object possess(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.possess(id, m);
    }

    @PostMapping("/contracts/{id}/handovers")
    public Object createHandover(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var c = access.contract(id);
        long room = com.hostel.service.Database.id(c, "room_id");
        access.action(room, "MANAGE", BigDecimal.ZERO);
        db.one("select id from contracts where id=? for update", id);

        String type = text(m, "MOVE_IN", "handoverType");
        require(Set.of("MOVE_IN", "MOVE_OUT", "RESPONSIBILITY").contains(type), "Invalid handover type");

        String code = text(m, "HO-" + type + "-" + UUID.randomUUID(), "handoverCode");
        OffsetDateTime occurred = time(value(m, "occurredAt"));
        require(!occurred.isAfter(now()), "Thời điểm bàn giao phải là thời điểm đã xảy ra");
        Long possessionId = db.jdbc.queryForObject("""
                select (select id from room_possessions
                        where contract_id=? and taken_at<=?
                          and (returned_at is null or returned_at>=?)
                        order by taken_at desc,id desc limit 1)
                """, Long.class, id, occurred, occurred);
        Long requestedPossession = optionalId(m, "possessionId");
        require(requestedPossession == null || requestedPossession.equals(possessionId),
                "Lượt nhận phòng không phù hợp với thời điểm bàn giao");
        Long receivedBy = optionalId(m, "receivedByTenantId");
        if (receivedBy == null && c.get("primary_tenant_id") != null) {
            receivedBy = ((Number) c.get("primary_tenant_id")).longValue();
        }

        long handoverId = db.insert(
                "handovers",
                "contract_id,possession_id,handover_code,handover_type,occurred_at,status,given_by,received_by_tenant_id,note",
                id, possessionId, code, type, occurred, "DRAFT", access.user(), receivedBy, text(m, "", "note"));

        if (bool(m, true, "autoPopulateItems")) {
            if ("MOVE_OUT".equals(type)) {
                db.jdbc.update("""
                        insert into handover_items (handover_id,asset_placement_id,line_no,
                            item_name_snapshot,quantity,condition_note,original_item_id)
                        select ?,i.asset_placement_id,i.line_no,i.item_name_snapshot,i.quantity,'',i.id
                        from handover_items i where i.handover_id=(
                            select h.id from handovers h where h.contract_id=? and h.occurred_at<=?
                            and h.handover_type='MOVE_IN' and h.status='CONFIRMED'
                            and (h.possession_id is null or h.possession_id=?)
                            order by h.occurred_at desc,h.id desc limit 1)
                        """, handoverId, id, occurred, possessionId);
            } else {
                db.jdbc.update("""
                        insert into handover_items (handover_id,asset_placement_id,line_no,
                            item_name_snapshot,quantity,condition_note)
                        select ?,ap.id,row_number() over(order by at.name,a.asset_code),
                               left(at.name||' ('||a.asset_code||')',150),a.quantity,''
                        from asset_placements ap join assets a on a.id=ap.asset_id
                        join asset_types at on at.id=a.asset_type_id
                        where ap.room_id=? and ap.valid_from<=? and (ap.valid_to is null or ap.valid_to>?)
                        """, handoverId, room, occurred, occurred);
            }
        }

        events.audit("CREATE_HANDOVER", "handovers", handoverId, null, m, "Created handover " + code + " for contract " + id);
        return leases.detail(id);
    }

    @PostMapping("/handovers/{id}/items")
    public Object addHandoverItem(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var h = db.one("select * from handovers where id=? for update", id);
        require(!"CONFIRMED".equals(h.get("status")), "Cannot modify confirmed handover");
        var contract = access.contract(com.hostel.service.Database.id(h, "contract_id"));
        access.action(com.hostel.service.Database.id(contract, "room_id"), "MANAGE", BigDecimal.ZERO);

        int nextLine = db.jdbc.queryForObject(
                "select coalesce(max(line_no), 0) + 1 from handover_items where handover_id=?",
                Integer.class,
                id);
        long itemId = db.insert(
                "handover_items",
                "handover_id,asset_placement_id,line_no,item_name_snapshot,quantity,condition_note,note",
                id,
                optionalId(m, "assetPlacementId"),
                nextLine,
                required(m, "itemName"),
                amount(m, "1", "quantity"),
                text(m, "Hoạt động tốt", "conditionNote"),
                text(m, "", "note"));
        return db.one("select * from handover_items where id=?", itemId);
    }

    @PutMapping("/handovers/{id}/items/{itemId}")
    public Object updateHandoverItem(@PathVariable long id, @PathVariable long itemId,
            @RequestBody Map<String, Object> m) {
        access.manager();
        var h = db.one("select * from handovers where id=? for update", id);
        require(!"CONFIRMED".equals(h.get("status")), "Biên bản đã xác nhận");
        var c = access.contract(com.hostel.service.Database.id(h, "contract_id"));
        access.action(com.hostel.service.Database.id(c, "room_id"), "MANAGE", BigDecimal.ZERO);
        var item = db.one("select * from handover_items where handover_id=? and id=?", id, itemId);
        BigDecimal quantity = amount(m, item.get("quantity").toString(), "quantity");
        require(quantity.signum() >= 0 && quantity.stripTrailingZeros().scale() <= 2,
                "Số lượng phải từ 0 và có tối đa hai chữ số thập phân");
        db.jdbc.update("update handover_items set quantity=?,condition_note=?,note=? where id=?",
                quantity, required(m, "conditionNote"), text(m, "", "note"), itemId);
        events.audit("UPDATE_HANDOVER_ITEM", "handover_items", itemId, item, m, "Updated item inspection");
        return db.one("select * from handover_items where id=?", itemId);
    }

    @PostMapping("/handovers/{id}/confirm")
    public Object confirmHandover(@PathVariable long id, @RequestBody(required = false) Map<String, Object> m) {
        access.manager();
        var h = db.one("select * from handovers where id=? for update", id);
        require(!"CONFIRMED".equals(h.get("status")), "Handover already confirmed");
        long contractId = com.hostel.service.Database.id(h, "contract_id");
        var contract = access.contract(contractId);
        access.action(com.hostel.service.Database.id(contract, "room_id"), "MANAGE", BigDecimal.ZERO);
        require(db.rows("select id from handover_items where handover_id=? and btrim(condition_note)=''", id).isEmpty(),
                "Ghi nhận hiện trạng của từng đồ vật trước khi xác nhận");

        db.jdbc.update(
                "update handovers set status='CONFIRMED',confirmed_by=?,confirmed_at=clock_timestamp(),note=coalesce(?,note) where id=?",
                access.user(), m != null ? text(m, null, "note") : null, id);
        events.audit("CONFIRM_HANDOVER", "handovers", id, h, m, "Confirmed handover " + h.get("handover_code"));
        return leases.detail(contractId);
    }

    @PostMapping("/handovers/{id}/damage-compensation")
    public Object damageCompensation(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var h = db.one("select * from handovers where id=?", id);
        long contractId = com.hostel.service.Database.id(h, "contract_id");
        var c = access.contract(contractId);
        long roomId = com.hostel.service.Database.id(c, "room_id");
        BigDecimal amount = cashAmount(m, "amount");
        access.action(roomId, "ADJUST", amount);
        db.one("select id from contracts where id=? for update", contractId);
        require("CONFIRMED".equals(h.get("status")) && "MOVE_OUT".equals(h.get("handover_type")),
                "Chỉ lập bồi thường từ biên bản trả phòng đã xác nhận");

        long handoverItemId = keyId(m, "handoverItemId");
        var item = db.one("select * from handover_items where id=? and handover_id=?", handoverItemId, id);
        LocalDate issueDate = day(m, now().toLocalDate(), "issueDate");
        LocalDate dueDate = day(m, issueDate.plusDays(3), "dueDate");
        String reason = required(m, "note", "reason");
        String code = text(m, "COMP-HO-" + handoverItemId, "documentCode");
        var previous = db.rows("""
                select d.id,d.document_code,d.issue_date,d.due_date,d.reason,i.unit_price_snapshot
                from billing_documents d join billing_items i on i.document_id=d.id
                where i.handover_item_id=? and i.item_kind='DAMAGE' and d.document_type='COMPENSATION'
                """, handoverItemId);
        if (!previous.isEmpty()) {
            var existing = previous.getFirst();
            require(code.equals(existing.get("document_code"))
                            && issueDate.equals(date(existing.get("issue_date")))
                            && dueDate.equals(date(existing.get("due_date")))
                            && reason.equals(existing.get("reason"))
                            && amount.compareTo(money(existing, "unit_price_snapshot")) == 0,
                    "Đồ vật đã có chứng từ bồi thường");
            return finance.document(com.hostel.service.Database.id(existing, "id"));
        }
        var checkout = db.rows("select id,status,return_handover_id from checkout_records where contract_id=?"
                + " order by id desc limit 1", contractId);
        if (!checkout.isEmpty()) {
            require("DRAFT".equals(checkout.getFirst().get("status")), "Quyết toán đã được duyệt");
        }
        var document = new HashMap<String, Object>(Map.of(
                "contractId", contractId,
                "documentCode", code,
                "documentType", "COMPENSATION",
                "issueDate", issueDate,
                "dueDate", dueDate,
                "billingPeriod", issueDate.withDayOfMonth(1),
                "reason", reason));
        if (!checkout.isEmpty() && Objects.equals(checkout.getFirst().get("return_handover_id"), id)) {
            document.put("checkoutId", checkout.getFirst().get("id"));
        }
        long docId = finance.createDocument(document);

        String itemName = "Bồi thường: " + item.get("item_name_snapshot");
        finance.line(docId, Map.of(
                "itemKind", "DAMAGE",
                "itemName", itemName.substring(0, itemName.offsetByCodePoints(
                        0, Math.min(150, itemName.codePointCount(0, itemName.length())))),
                "quantity", BigDecimal.ONE,
                "unitPrice", amount,
                "unitName", "Lần",
                "handoverItemId", handoverItemId,
                "sourceKey", "HANDOVER-DAMAGE-" + handoverItemId,
                "basisNote", reason));

        return finance.issue(docId);
    }

    @PostMapping("/contracts/{id}/move-out-notices")
    public Object notice(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.notice(id, m);
    }

    @PutMapping("/move-out-notices/{id}")
    public Object cancelNotice(@PathVariable long id, @RequestBody Map<String, Object> m) {
        var old = db.one("select * from move_out_notices where id=?", id);
        access.contract(com.hostel.service.Database.id(old, "contract_id"));
        require(
                Set.of("CANCELLED", "APPROVED").contains(required(m, "status")),
                "Invalid notice decision");
        if ("APPROVED".equals(m.get("status"))) {
            access.manager();
        }
        db.jdbc.update("update move_out_notices set status=? where id=?", m.get("status"), id);
        events.audit(
                "NOTICE_DECISION",
                "move_out_notices",
                id,
                old,
                m,
                text(m, "Reported notice decision", "reason"));
        return db.one("select * from move_out_notices where id=?", id);
    }

    @PostMapping("/contracts/{id}/checkout")
    public Object checkout(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.checkout(id, m);
    }

    @PostMapping("/contracts/{id}/checkout/approve")
    public Object approveCheckout(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return settlements.approve(id, m);
    }

    @GetMapping("/contracts/{id}/checkout")
    public Object checkout(@PathVariable long id) {
        return leases.detail(id).get("checkout");
    }

    @PostMapping("/contracts/{id}/reconcile")
    public Object reconcile(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return leases.reconcile(id, m);
    }

    @GetMapping("/contracts/{id}/billing-calendar")
    public Object calendar(@PathVariable long id, @RequestParam String period) {
        return billing.calendar(id, YearMonth.parse(period).atDay(1));
    }

    @GetMapping("/invoices")
    public Object invoices() {
        var rows =
                db.rows(
                        "select d.*,d.document_code invoice_code,r.room_code,t.full_name from"
                                + " v_document_balances d join v_contract_current c on"
                                + " c.id=d.contract_id join rooms r on r.id=c.room_id left join"
                                + " tenant_profiles t on t.id=c.primary_tenant_id where "
                                + access.contractScope("c", true)
                                + (access.role().equals("TENANT") ? " and d.status='ISSUED'" : "")
                                + " order by d.id desc");
        for (var d : rows) {
            if (Set.of("ADJUSTMENT", "REIMBURSEMENT").contains(d.get("document_type"))) {
                d.put(
                        "total_amount",
                        db.jdbc.queryForObject(
                                "select coalesce(sum(case direction when 'CREDIT' then -subtotal"
                                        + " else subtotal end),0) from v_billing_lines where"
                                        + " document_id=?",
                                BigDecimal.class,
                                d.get("id")));
            }
        }
        return rows;
    }

    @PostMapping("/invoices")
    public Object monthly(@RequestBody Map<String, Object> m) {
        return billing.monthly(m);
    }

    @GetMapping({"/invoices/{id}", "/billing-documents/{id}"})
    public Object document(@PathVariable long id) {
        return finance.document(id);
    }

    @PostMapping("/billing-documents")
    public Object document(@RequestBody Map<String, Object> m) {
        long id = finance.createDocument(m);
        if (m.get("items") instanceof List<?> lines) {
            for (Object line : lines) {
                @SuppressWarnings("unchecked")
                var x = (Map<String, Object>) line;
                finance.line(id, x);
            }
        }
        return finance.document(id);
    }

    @PostMapping("/billing-documents/{id}/items")
    public Object line(@PathVariable long id, @RequestBody Map<String, Object> m) {
        long line = finance.line(id, m);
        return db.one("select * from v_billing_lines where id=?", line);
    }

    @PostMapping("/billing-items/{id}/meter-corrections")
    public Object correctMeter(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return finance.correctMeterLine(id, m);
    }

    @GetMapping("/contracts/{id}/settlement")
    public Object settlement(@PathVariable long id) {
        return settlements.statement(id);
    }

    @PostMapping({"/invoices/{id}/issue", "/billing-documents/{id}/issue"})
    public Object issue(@PathVariable long id) {
        return finance.issue(id);
    }

    @DeleteMapping("/invoices/{id}")
    public Object deleteDraft(@PathVariable long id) {
        access.manager();
        var d = access.document(id);
        require("DRAFT".equals(d.get("status")), "Only a draft may be deleted");
        events.audit(
                "DELETE_DRAFT",
                "billing_documents",
                id,
                finance.document(id),
                null,
                "Operator removed draft with source history retained");
        db.jdbc.update("delete from billing_items where document_id=?", id);
        db.jdbc.update("delete from billing_documents where id=?", id);
        return Map.of("id", id);
    }

    @PostMapping("/invoices/{id}/payments")
    public Object pay(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return billing.pay(id, m);
    }

    @PostMapping("/cash-transactions")
    public Object receive(@RequestBody Map<String, Object> m) {
        return finance.receive(m);
    }

    @GetMapping("/cash-transactions")
    public Object cash() {
        access.manager();
        return db.rows(
                "select tx.* from cash_transactions tx join cash_accounts a on"
                        + " a.id=tx.cash_account_id where "
                        + (access.role().equals("ADMIN")
                                ? "true"
                                : "(can_manage_property("
                                        + access.user()
                                        + ",a.property_id) or tx.recorded_by="
                                        + access.user()
                                        + ")")
                        + " order by tx.id desc limit 1000");
    }

    @PostMapping("/cash-transactions/{id}/confirm")
    public Object confirm(@PathVariable long id) {
        return finance.confirm(id);
    }

    @PostMapping("/fund-entries")
    public Object entry(@RequestBody Map<String, Object> m) {
        return finance.entry(m);
    }

    @GetMapping("/fund-accounts")
    public Object allFunds() {
        access.manager();
        return db.rows(
                "select f.* from v_fund_balances f left join contracts c on c.id=f.contract_id left"
                        + " join reservations r on r.id=f.reservation_id left join cash_transactions tx"
                        + " on tx.id=f.unapplied_receipt_id where "
                        + (access.role().equals("ADMIN")
                                ? "true"
                                : "(can_manage_room("
                                        + access.user()
                                        + ",c.room_id) or can_manage_room("
                                        + access.user()
                                        + ",r.room_id) or tx.recorded_by="
                                        + access.user()
                                        + ")")
                        + " order by f.id desc limit 1000");
    }

    @PostMapping("/reservations/{id}/fund")
    public Object reservationFund(@PathVariable long id) {
        access.manager();
        var r = db.one("select * from reservations where id=? for update", id);
        access.room(com.hostel.service.Database.id(r, "room_id"));
        var existing = db.rows("select id from fund_accounts where reservation_id=?", id);
        long f =
                existing.isEmpty()
                        ? db.insert(
                                "fund_accounts",
                                "fund_code,fund_type,reservation_id,beneficiary_tenant_id",
                                "RESERVATION-" + id,
                                "RESERVATION",
                                id,
                                r.get("tenant_id"))
                        : com.hostel.service.Database.id(existing.getFirst(), "id");
        return access.fund(f);
    }

    @GetMapping("/contracts/{id}/funds")
    public Object funds(@PathVariable long id) {
        access.contract(id);
        return db.rows("select * from v_fund_balances where contract_id=? order by id", id);
    }

    @GetMapping("/fund-accounts/{id}")
    public Object fund(@PathVariable long id) {
        return access.fund(id);
    }

    @PostMapping("/fund-accounts/{id}/reconcile")
    public Object reconcileFund(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return finance.reconcileFund(id, m);
    }

    @PostMapping("/fund-accounts/{id}/refund")
    public Object refund(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return finance.refund(id, m);
    }

    @GetMapping("/fund-accounts/{id}/holds")
    public Object holds(@PathVariable long id) {
        access.fund(id);
        return db.rows("select * from fund_holds where fund_account_id=? order by id", id);
    }

    @PostMapping("/fund-accounts/{id}/holds")
    public Object hold(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var f = access.fund(id);
        require(f.get("contract_id") != null, "Contract fund required");
        var c = access.contract(com.hostel.service.Database.id(f, "contract_id"));
        access.action(
                com.hostel.service.Database.id(c, "room_id"), "ADJUST", amount(m, "0", "amount"));
        long h =
                db.insert(
                        "fund_holds",
                        "fund_account_id,related_billing_item_id,amount,reason,review_on,created_by",
                        id,
                        optionalId(m, "billingItemId"),
                        amount(m, "0", "amount"),
                        required(m, "reason"),
                        day(m, null, "reviewOn"),
                        access.user());
        events.audit("HOLD", "fund_holds", h, null, m, required(m, "reason"));
        return db.one("select * from fund_holds where id=?", h);
    }

    @PostMapping("/fund-holds/{id}/release")
    public Object release(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var h = db.one("select * from fund_holds where id=? for update", id);
        var f = access.fund(com.hostel.service.Database.id(h, "fund_account_id"));
        var c = access.contract(com.hostel.service.Database.id(f, "contract_id"));
        access.action(com.hostel.service.Database.id(c, "room_id"), "ADJUST", money(h, "amount"));
        db.jdbc.update(
                "update fund_holds set released_at=coalesce(released_at,clock_timestamp()) where"
                        + " id=?",
                id);
        events.audit("RELEASE_HOLD", "fund_holds", id, h, null, required(m, "reason"));
        return db.one("select * from fund_holds where id=?", id);
    }

    @GetMapping("/payments")
    public Object payments() {
        return db.rows(
                "select e.id,e.entry_code transaction_code,e.amount,e.posted_at"
                        + " payment_date,d.document_code invoice_code,r.room_code,case when"
                        + " f.fund_type='DEPOSIT' then 'DEPOSIT_OFFSET' else 'FUNDS_ALLOCATION' end"
                        + " payment_method from fund_entries e join fund_accounts f on"
                        + " f.id=e.from_fund_id join billing_items i on i.id=e.debit_item_id join"
                        + " billing_documents d on d.id=i.document_id join contracts c on"
                        + " c.id=d.contract_id join rooms r on r.id=c.room_id where"
                        + " e.entry_type='PAYMENT' and "
                        + access.contractScope("c", true)
                        + " order by e.id desc");
    }

    @PostMapping("/meter-points")
    public Object reading(@RequestBody Map<String, Object> m) {
        return meters.reading(m);
    }

    @PostMapping("/meter-readings")
    public Object measuredUsage(@RequestBody Map<String, Object> m) {
        return meters.usage(m);
    }

    @GetMapping("/meter-readings/{id}")
    public Object measurement(@PathVariable long id) {
        access.manager();
        var s =
                db.one(
                        "select s.*,s.service_end::date reading_date from v_segments s where"
                                + " s.id=?",
                        id);
        access.room(com.hostel.service.Database.id(s, "room_id"));
        return s;
    }

    @PostMapping("/consumption-segments")
    public Object segment(@RequestBody Map<String, Object> m) {
        return meters.segment(m);
    }

    @PostMapping("/consumption-segments/{id}/revise")
    public Object reviseSegment(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return meters.revise(id, m);
    }

    @PostMapping("/meters/{id}/replace")
    public Object replace(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return meters.replace(id, m);
    }

    @GetMapping("/meter-readings")
    public Object measurements() {
        access.manager();
        return db.rows(
                "select s.*,date_trunc('month',(s.service_end-interval '1 second') at time zone 'Asia/Ho_Chi_Minh')::date"
                        + " billing_period,r.room_code,s.service_end::date reading_date,case"
                        + " si.service_code when 'ELEC' then 'ELECTRICITY' else si.service_code end"
                        + " meter_type,exists(select 1 from consumption_alerts a where"
                        + " a.segment_id=s.id and a.status='PENDING') has_anomaly,"
                        + " a.id alert_id, a.status alert_status,"
                        + " case when a.baseline_daily_average > 0 and (extract(epoch from (s.service_end - s.service_start))/86400) > 0"
                        + " then round((((s.consumption / greatest(0.1, extract(epoch from (s.service_end - s.service_start))/86400)) - a.baseline_daily_average) / a.baseline_daily_average * 100)::numeric, 1)"
                        + " else a.applied_threshold end percentage_increase,"
                        + " a.baseline_daily_average baseline_average"
                        + " from v_segments s"
                        + " join rooms r on r.id=s.room_id"
                        + " join service_items si on si.id=s.service_item_id"
                        + " left join consumption_alerts a on a.segment_id=s.id"
                        + " where "
                        + access.roomScope("s.room_id")
                        + " order by (case when a.status='PENDING' then 0 else 1 end) asc, s.service_end desc, r.room_code asc, s.id desc");
    }

    @GetMapping("/alerts")
    public Object alerts() {
        access.manager();
        return db.rows(
                "select a.*,s.contract_id,s.room_id,s.consumption,r.room_code,"
                        + " case si.service_code when 'ELEC' then 'ELECTRICITY' else si.service_code end meter_type,"
                        + " si.name service_name, to_char((s.service_end-interval '1 second') at time zone 'Asia/Ho_Chi_Minh', 'YYYY-MM') billing_period,"
                        + " s.old_index start_reading, s.new_index end_reading, s.service_start, s.service_end,"
                        + " case when a.baseline_daily_average > 0 and (extract(epoch from (s.service_end - s.service_start))/86400) > 0"
                        + " then round((((s.consumption / greatest(0.1, extract(epoch from (s.service_end - s.service_start))/86400)) - a.baseline_daily_average) / a.baseline_daily_average * 100)::numeric, 1)"
                        + " else a.applied_threshold end percentage_increase,"
                        + " a.baseline_daily_average baseline_average"
                        + " from consumption_alerts a"
                        + " join v_segments s on s.id=a.segment_id"
                        + " join rooms r on r.id=s.room_id"
                        + " join service_items si on si.id=s.service_item_id"
                        + " where "
                        + access.roomScope("s.room_id")
                        + " order by (case when a.status='PENDING' then 0 else 1 end) asc, a.id desc");
    }

    @PutMapping("/alerts/{id}")
    public Object review(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        var a =
                db.one(
                        "select a.*,s.room_id from consumption_alerts a join v_segments s on"
                                + " s.id=a.segment_id where a.id=?",
                        id);
        access.room(com.hostel.service.Database.id(a, "room_id"));
        require(
                Set.of("CONFIRMED", "DISMISSED").contains(required(m, "status")),
                "Invalid review state");
        db.jdbc.update(
                "update consumption_alerts set"
                        + " status=?,checked_by=?,checked_at=clock_timestamp(),context_note=? where"
                        + " id=?",
                m.get("status"),
                access.user(),
                required(m, "note"),
                id);
        return db.one("select * from consumption_alerts where id=?", id);
    }

    @GetMapping("/maintenance-requests")
    public Object repairs() {
        var rows =
                db.rows(
                        "select m.* from v_maintenance m where "
                                + (access.role().equals("TENANT")
                                        ? "m.submitted_by=" + access.user()
                                        : "("
                                                + access.roomScope("m.resolved_room_id")
                                                + " or can_manage_property("
                                                + access.user()
                                                + ",m.resolved_property_id))")
                                + " order by m.id desc");
        var out = new ArrayList<Object>();
        for (var r : rows) {
            out.add(operations.detail(com.hostel.service.Database.id(r, "id")));
        }
        return out;
    }

    @GetMapping("/maintenance-requests/{id}")
    public Object repair(@PathVariable long id) {
        return operations.detail(id);
    }

    @PostMapping("/maintenance-requests")
    public Object repair(@RequestBody Map<String, Object> m) {
        return operations.create(m);
    }

    @PutMapping("/maintenance-requests/{id}")
    public Object repairProgress(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return operations.progress(id, m);
    }

    @PostMapping("/maintenance-requests/{id}/costs")
    public Object repairCost(@PathVariable long id, @RequestBody Map<String, Object> m) {
        return operations.cost(id, m);
    }

    @PostMapping("/maintenance-schedules/{id}/complete")
    public Object schedule(
            @PathVariable long id, @RequestBody(required = false) Map<String, Object> m) {
        return operations.schedule(id, m == null ? Map.of() : m);
    }

    @GetMapping("/notifications")
    public Object notifications() {
        return db.rows(
                "select n.*,r.read_at,r.read_at is not null is_read from notifications n join"
                        + " notification_recipients r on r.notification_id=n.id where r.user_id=? order"
                        + " by n.id desc",
                access.user());
    }

    @PostMapping("/contracts/{id}/announcements")
    public Object announce(@PathVariable long id, @RequestBody Map<String, Object> m) {
        access.manager();
        access.contract(id);
        events.notifyContract(
                id,
                "ANNOUNCEMENT",
                required(m, "title"),
                required(m, "body"),
                "ANNOUNCE-" + id + "-" + required(m, "code"),
                null);
        return Map.of("message", "Thông báo đã lưu trong ứng dụng");
    }

    @PutMapping("/notifications/{id}/read")
    public Object read(@PathVariable long id) {
        db.one(
                "select id from notification_recipients where notification_id=? and user_id=?",
                id,
                access.user());
        db.jdbc.update(
                "update notification_recipients set read_at=coalesce(read_at,clock_timestamp())"
                        + " where notification_id=? and user_id=?",
                id,
                access.user());
        return Map.of("id", id);
    }

    @GetMapping("/audit-logs")
    public Object audit() {
        access.admin();
        return db.rows("select * from audit_events order by id desc limit 500");
    }

    @GetMapping({"/rooms/{id}/assignees", "/properties/{id}/assignees"})
    public Object assignees(@PathVariable long id, HttpServletRequest req) {
        access.manager();
        boolean room = req.getRequestURI().contains("/rooms/");
        if (room) {
            access.room(id);
        } else {
            access.property(id);
        }
        return db.rows(
                "select id,username full_name from users u where status='ACTIVE' and role in"
                        + " ('ADMIN','STAFF') and "
                        + (room ? "can_manage_room(u.id,?)" : "can_manage_property(u.id,?)")
                        + " order by id",
                id);
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable long id) throws java.io.IOException {
        var d = finance.document(id);
        var lines = new ArrayList<String>();
        lines.add("Kỳ nhãn: " + d.get("billing_period"));
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) d.get("items");
        for (var i : items) {
            lines.add(i.get("item_name_snapshot") + " · " + i.get("formula_description"));
        }
        @SuppressWarnings("unchecked")
        var adjustments = (List<Map<String, Object>>) d.get("adjustments");
        for (var a : adjustments) {
            lines.add(
                    "Điều chỉnh "
                            + a.get("adjustment_code")
                            + " ngày "
                            + a.get("adjustment_date")
                            + ": "
                            + a.get("item_name_snapshot")
                            + " "
                            + ("CREDIT".equals(a.get("direction")) ? "−" : "+")
                            + a.get("subtotal"));
        }
        lines.add("Tổng sau điều chỉnh: " + d.get("total_amount"));
        lines.add("Đã phân bổ: " + d.get("paid_amount") + " · Còn nợ: " + d.get("balance_due"));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf")
                .body(pdf.render("CHỨNG TỪ " + d.get("document_code"), lines));
    }

    @GetMapping("/contracts/{id}/pdf")
    public ResponseEntity<byte[]> contractPdf(
            @PathVariable long id, @RequestParam(required = false) Integer version) throws java.io.IOException {
        var contract = access.contract(id);
        var terms = version == null
                ? db.one("select * from contract_terms where contract_id=? order by version_no desc limit 1", id)
                : db.one("select * from contract_terms where contract_id=? and version_no=?", id, version);
        var room = db.one("""
                select r.room_code,r.floor_number,r.area_sqm,p.name,p.address
                from rooms r join properties p on p.id=r.property_id where r.id=?
                """, contract.get("room_id"));
        var tenant = db.one("select * from tenant_profiles where id=?", contract.get("primary_tenant_id"));
        var dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu");
        var number = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        number.setMaximumFractionDigits(2);
        LocalDate start = date(terms.get("effective_from"));
        LocalDate end = date(terms.get("lease_end"));
        var residents = db.rows("""
                select t.full_name from contract_residencies r join tenant_profiles t on t.id=r.tenant_id
                where r.contract_id=? and r.effective_from<=? and (r.effective_to is null or r.effective_to>?)
                order by r.effective_from,r.id
                """, id, start, start).stream().map(row -> row.get("full_name").toString()).toList();
        var handovers = db.rows("""
                select id,handover_code,occurred_at from handovers
                where contract_id=? and handover_type='MOVE_IN' and status='CONFIRMED'
                and occurred_at<? order by occurred_at,id limit 1
                """, id, end.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
        String handover = "";
        List<ContractPdf.Item> items = List.of();
        if (!handovers.isEmpty()) {
            var record = handovers.getFirst();
            handover = record.get("handover_code") + " · " + time(record.get("occurred_at")).format(dateFormat);
            items = db.rows("""
                    select item_name_snapshot,quantity,condition_note from handover_items
                    where handover_id=? order by line_no
                    """, record.get("id")).stream().map(row -> new ContractPdf.Item(
                            row.get("item_name_snapshot").toString(), number.format(row.get("quantity")),
                            row.get("condition_note").toString())).toList();
        }
        String blank = "……………………";
        String code = contract.get("contract_code").toString();
        int versionNumber = ((Number) terms.get("version_no")).intValue();
        var lease = new ContractPdf.Lease(
                code, versionNumber,
                terms.get("signed_at") == null ? "" : time(terms.get("signed_at")).format(dateFormat),
                new ContractPdf.Party(
                        pdfField(terms, "lessor_name", blank), pdfField(terms, "lessor_identity", blank),
                        pdfField(terms, "lessor_phone", blank), pdfField(terms, "lessor_address", blank)),
                new ContractPdf.Party(
                        pdfField(tenant, "full_name", blank), pdfField(tenant, "identity_number", blank),
                        pdfField(tenant, "phone", blank), pdfField(terms, "tenant_address", blank)),
                room.get("room_code") + " · Tầng " + room.get("floor_number"),
                room.get("name").toString(), pdfField(room, "address", blank),
                room.get("area_sqm") == null ? blank : number.format(room.get("area_sqm")) + " m²",
                start.format(dateFormat), end.format(dateFormat),
                number.format(terms.get("rental_price")) + " đ",
                number.format(terms.get("deposit_required")) + " đ",
                "Ngày " + terms.get("payment_day") + " hằng tháng · "
                        + ("IN_ADVANCE".equals(terms.get("collection_timing")) ? "Thu đầu kỳ" : "Thu cuối kỳ"),
                pdfField(terms, "terms", ""), handover, items, residents);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers -> headers.setContentDisposition(ContentDisposition.attachment()
                        .filename("hop-dong-" + code + "-v" + versionNumber + ".pdf",
                                java.nio.charset.StandardCharsets.UTF_8).build()))
                .body(ContractPdf.render(lease));
    }

    private static String pdfField(Map<String, Object> row, String key, String fallback) {
        String value = Objects.toString(row.get(key), "").strip();
        return value.isBlank() ? fallback : value;
    }
}
