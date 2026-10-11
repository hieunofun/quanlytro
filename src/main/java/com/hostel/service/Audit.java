package com.hostel.service;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
@Service
public class Audit {
    private final Database db; private final Access access;
    public Audit(Database db,Access access) { this.db=db; this.access=access; }
    public void log(String action,String type,long id,Object before,Object after) {
        db.insert("audit_logs","user_id,action,resource_type,resource_id,before_data,after_data",access.userId(),action,type,id,
            before==null?null:before.toString(),after==null?null:after.toString());
    }
}
