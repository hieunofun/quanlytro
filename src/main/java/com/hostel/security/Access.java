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
        var r=db.one("select * from rooms where id=?",roomId);
        String scope=role().equals("TENANT")?"id in (select c.room_id from contracts c where "+contractScope("c")+")":roomScope("id");
        if(db.jdbc.queryForObject("select count(*) from rooms where id=? and "+scope,Long.class,roomId)==0) denied();
        return r;
    }
    public Map<String,Object> maintenance(long requestId) {
        var m=db.one("select * from maintenance_requests where id=?",requestId);
        if(role().equals("TENANT")) {if(id(m,"submitted_by")!=userId()) denied();}
        else if(m.get("room_id")!=null) room(id(m,"room_id")); else property(id(m,"property_id"));
        return m;
    }
    public String propertyReadScope(String column) {
        return "("+propertyScope(column)+" or "+column+" in (select r.property_id from rooms r join room_employee_assignments e on e.room_id=r.id where e.is_active and e.user_id="+userId()+"))";
    }
    public String roomScope(String column) {
        if(role().equals("ADMIN")) return "true";
        if(!role().equals("STAFF")) return "false";
        return "("+column+" in (select id from rooms where "+propertyScope("property_id")+") or "+column+" in (select room_id from room_employee_assignments where is_active and user_id="+userId()+"))";
    }
    public String maintenanceScope(String alias) {
        return "("+roomScope(alias+".room_id")+" or "+alias+".room_id is null and "+propertyScope(alias+".property_id")+")";
    }
    public boolean canManageRoom(long userId,long roomId) {
        return db.jdbc.queryForObject("select count(*) from users u join rooms r on r.id=? where u.id=? and u.status='ACTIVE' and (u.role='ADMIN' or u.role='STAFF' and (exists(select 1 from employee_assignments e where e.user_id=u.id and e.property_id=r.property_id and e.is_active) or exists(select 1 from room_employee_assignments e where e.user_id=u.id and e.room_id=r.id and e.is_active)))",Long.class,roomId,userId)>0;
    }
    public Map<String,Object> contract(long contractId) {
        var c=db.one("select c.*,r.property_id from contracts c join rooms r on r.id=c.room_id where c.id=?",contractId);
        if(role().equals("TENANT")) {
            var tenant=db.one("select * from tenant_profiles where id=?",id(c,"primary_tenant_id"));
            if(tenant.get("user_id")==null || id(tenant,"user_id")!=userId()) denied();
        } else room(id(c,"room_id"));
        return c;
    }
    public Map<String,Object> invoice(long invoiceId) {
        var i=db.one("select * from invoices where id=?",invoiceId);
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
