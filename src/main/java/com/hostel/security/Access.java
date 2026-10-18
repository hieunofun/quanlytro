package com.hostel.security;
import com.hostel.service.Database;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;
import static com.hostel.service.Database.id;
@Component
public class Access {
    private final Database db;
    public Access(Database db) { this.db=db; }
    public long userId() { return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName()); }
    public String role() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().substring(5); }
    public void admin() { if(!role().equals("ADMIN")) denied(); }
    public void manager() { if(role().equals("TENANT")) denied(); }
    public void property(long propertyId) {
        if(role().equals("ADMIN")) return;
        if(!role().equals("STAFF") || db.jdbc.queryForObject("select count(*) from employee_assignments where user_id=? and property_id=? and is_active",Long.class,userId(),propertyId)==0) denied();
    }
    public void propertyRead(long propertyId) {
        manager();
        if(db.jdbc.queryForObject("select count(*) from properties where id=? and "+propertyReadScope("id"),Long.class,propertyId)==0) denied();
    }
    public Map<String,Object> room(long roomId) {
        var r=db.one("select r.*,display_status status from v_room_status r where id=?",roomId);
        String scope=role().equals("TENANT")?"id in (select c.room_id from contracts c where "+contractScope("c")+")":roomScope("id");
        if(db.jdbc.queryForObject("select count(*) from rooms where id=? and "+scope,Long.class,roomId)==0) denied();
        if(role().equals("TENANT")) r.remove("internal_note");
        return r;
    }
    public Map<String,Object> maintenance(long requestId) {
        var m=db.one("select m.*,c.room_id,r.property_id from maintenance_requests m join contracts c on c.id=m.contract_id join rooms r on r.id=c.room_id where m.id=?",requestId);
        contract(id(m,"contract_id"));return m;
    }
    public String propertyReadScope(String column) {return propertyScope(column);}
    public String roomScope(String column) {
        if(role().equals("ADMIN"))return "true";
        if(!role().equals("STAFF"))return "false";
        return column+" in (select id from rooms where "+propertyScope("property_id")+")";
    }
    public String maintenanceScope(String alias) {return alias+".contract_id in (select c.id from contracts c where "+contractScope("c")+")";}
    public boolean canManageRoom(long actor,long room) {return Boolean.TRUE.equals(db.jdbc.queryForObject("select can_manage_property(?,property_id) from rooms where id=?",Boolean.class,actor,room));}
    public Map<String,Object> contract(long contractId) {
        var c=db.one("select c.*,r.property_id from contracts c join rooms r on r.id=c.room_id where c.id=?",contractId);
        if(role().equals("TENANT")) {
            var tenant=db.one("select * from tenant_profiles where id=?",id(c,"primary_tenant_id"));
            if(tenant.get("user_id")==null || id(tenant,"user_id")!=userId()) denied();
        } else room(id(c,"room_id"));
        return c;
    }
    public Map<String,Object> invoice(long invoiceId) {
        var i=db.one("select * from v_invoice_balances where id=?",invoiceId);
        contract(id(i,"contract_id")); return i;
    }
    public String propertyScope(String column) {
        if(role().equals("ADMIN")) return "true";
        if(role().equals("STAFF")) return column+" in (select property_id from employee_assignments where is_active and user_id="+userId()+")";
        return "false";
    }
    public String contractScope(String alias) {
        if(role().equals("TENANT")) return alias+".primary_tenant_id in (select id from tenant_profiles where user_id="+userId()+")";
        return roomScope(alias+".room_id");
    }
    public static void denied() { throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied"); }
}
