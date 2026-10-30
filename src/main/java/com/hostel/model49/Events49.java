package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service @Profile("model49")
public class Events49 {
    private final Database db;private final Access49 access;private final ObjectMapper mapper=new ObjectMapper();
    public Events49(Database db,Access49 access){this.db=db;this.access=access;}
    private String safe(Object value){
        if(value==null)return null;
        if(value instanceof Map<?,?> map){var clean=new LinkedHashMap<String,Object>();map.forEach((k,v)->{String s=k.toString().toLowerCase(Locale.ROOT);if(!s.contains("password")&&!s.contains("token")&&!s.contains("secret"))clean.put(k.toString(),Objects.toString(v,null));});return mapper.writeValueAsString(clean);}
        return mapper.writeValueAsString(value);
    }
    public void audit(String action,String type,long id,Object before,Object after,String reason){
        db.jdbc.update("insert into audit_events(actor_user_id,action,resource_type,resource_id,before_data,after_data,reason) values(?,?,?,?,?::jsonb,?::jsonb,?)",access.user(),action,type,Long.toString(id),safe(before),safe(after),reason);
    }
    public void notifyContract(Number contract,String type,String title,String body,String key,Long document){
        if(contract==null)return;
        Long id=db.jdbc.queryForObject("insert into notifications(notification_type,title,body,contract_id,billing_document_id,deduplication_key,created_by) values(?,?,?,?,?,?,?) on conflict(deduplication_key) do update set deduplication_key=excluded.deduplication_key returning id",Long.class,type,title,body,document==null?contract.longValue():null,document,key,access.user());
        db.jdbc.update("insert into notification_recipients(notification_id,user_id) select ?,t.user_id from tenant_profiles t where t.user_id is not null and (exists(select 1 from contract_parties p where p.contract_id=? and p.party_role='PRIMARY' and p.tenant_id=t.id) or exists(select 1 from contract_contacts c where c.contract_id=? and c.tenant_id=t.id and c.can_view_billing and (c.effective_to is null or c.effective_to>current_date))) on conflict do nothing",id,contract.longValue(),contract.longValue());
    }
}
