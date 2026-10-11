package com.hostel.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReminderService {
    private final Database db;
    public ReminderService(Database db) {this.db=db;}
    @Scheduled(fixedDelay=3600000,initialDelay=10000)
    @Transactional
    public void reminders() {
        db.jdbc.update("insert into notifications(user_id,type,message,reference_key) select t.user_id,'OVERDUE','Hóa đơn '||i.invoice_code||' quá hạn. Còn nợ: '||i.balance_due,'overdue:'||i.id from invoices i join contracts c on c.id=i.contract_id join tenant_profiles t on t.id=c.primary_tenant_id where i.status='ISSUED' and i.balance_due>0 and i.due_date<current_date and t.user_id is not null on conflict(user_id,reference_key) do nothing");
        db.jdbc.update("insert into notifications(user_id,type,message,reference_key) select t.user_id,'CONTRACT_EXPIRING','Hợp đồng '||c.contract_code||' sắp hết hạn: '||c.end_date,'expiry:'||c.id||':'||c.end_date from contracts c join tenant_profiles t on t.id=c.primary_tenant_id where c.status='ACTIVE' and c.end_date<=current_date+30 and t.user_id is not null on conflict(user_id,reference_key) do nothing");
        db.jdbc.update("insert into notifications(user_id,type,message,reference_key) select distinct u.id,'CONSUMPTION_ALERT','Cảnh báo tiêu thụ '||m.meter_type||' tại phòng '||r.room_code,'alert:'||a.id from consumption_alerts a join meter_readings m on m.id=a.meter_reading_id join rooms r on r.id=m.room_id join users u on u.role='ADMIN' or u.role='STAFF' and exists(select 1 from employee_assignments e where e.user_id=u.id and e.property_id=r.property_id and e.is_active) or u.role='STAFF' and exists(select 1 from room_employee_assignments e where e.user_id=u.id and e.room_id=r.id and e.is_active) where a.status='PENDING' and u.status='ACTIVE' on conflict(user_id,reference_key) do nothing");
        db.jdbc.update("insert into notifications(user_id,type,message,reference_key) select distinct u.id,'CONTRACT_EXPIRING','Hợp đồng '||c.contract_code||' sắp hết hạn: '||c.end_date,'manager-expiry:'||c.id||':'||c.end_date from contracts c join rooms r on r.id=c.room_id join users u on u.role='ADMIN' or u.role='STAFF' and exists(select 1 from employee_assignments e where e.user_id=u.id and e.property_id=r.property_id and e.is_active) or u.role='STAFF' and exists(select 1 from room_employee_assignments e where e.user_id=u.id and e.room_id=r.id and e.is_active) where c.status='ACTIVE' and c.end_date<=current_date+30 and u.status='ACTIVE' on conflict(user_id,reference_key) do nothing");
    }
}
