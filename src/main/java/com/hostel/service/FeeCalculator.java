package com.hostel.service;
import java.math.*;
public interface FeeCalculator {
    BigDecimal calculate(BigDecimal price,BigDecimal quantity,int occupiedDays,int periodDays);
    static BigDecimal round(BigDecimal value) { return value.setScale(2,RoundingMode.HALF_UP); }
}
