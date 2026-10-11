package com.hostel.service;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
@Service
public class BillingCalendar {
    private final Database db;
    public BillingCalendar(Database db) {this.db=db;}
    public Map<String,Object> dates(long contractId,String period) {
        var c=db.one("select c.payment_cycle_day,p.meter_cutoff_day,p.invoice_issue_day from contracts c join rooms r on r.id=c.room_id join properties p on p.id=r.property_id where c.id=?",contractId);
        YearMonth month=YearMonth.parse(period),next=month.plusMonths(1);
        LocalDate cutoff=month.atDay(Math.min(month.lengthOfMonth(),((Number)c.get("meter_cutoff_day")).intValue()));
        LocalDate issue=next.atDay(((Number)c.get("invoice_issue_day")).intValue());
        LocalDate due=next.atDay(((Number)c.get("payment_cycle_day")).intValue());
        if(due.isBefore(issue)) due=due.plusMonths(1);
        return Map.of("period",period,"cutoffDate",cutoff,"issueDate",issue,"dueDate",due);
    }
}
