package com.hostel.service;
import org.springframework.stereotype.Component;
@Component
public class ServiceFeeCalculator implements FeeCalculator {
    public java.math.BigDecimal calculate(java.math.BigDecimal price,java.math.BigDecimal quantity,int days,int periodDays) {
        return new RoomFeeCalculator().calculate(price,quantity,days,periodDays);
    }
}
