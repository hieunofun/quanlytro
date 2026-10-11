package com.hostel.service;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import static com.hostel.service.Database.*;
@Service
public class PricingService {
    private final Database db;
    public PricingService(Database db) {this.db=db;}
    public List<Map<String,Object>> prices(long propertyId,LocalDate date) {
        return db.rows("select s.*,p.unit_price from service_items s join lateral (select unit_price from service_price_configs where property_id=? and service_item_id=s.id and effective_from<=? order by effective_from desc limit 1) p on true order by s.id",propertyId,date);
    }
    public void requireMandatory(long propertyId,LocalDate date) {
        long missing=db.jdbc.queryForObject("select count(*) from service_items s where is_mandatory and not exists(select 1 from service_price_configs p where p.service_item_id=s.id and p.property_id=? and p.effective_from<=?)",Long.class,propertyId,date);
        require(missing==0,"Configure all mandatory service prices before billing");
    }
}
