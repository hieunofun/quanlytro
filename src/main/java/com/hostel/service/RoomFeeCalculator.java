package com.hostel.service;
import java.math.*;
import org.springframework.stereotype.Component;
@Component
public class RoomFeeCalculator implements FeeCalculator {
    public BigDecimal calculate(BigDecimal price,BigDecimal quantity,int days,int periodDays) {
        return price.multiply(quantity).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(periodDays),2,RoundingMode.HALF_UP);
    }
}
