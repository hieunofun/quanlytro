package com.hostel.service;
import java.math.*;
import org.springframework.stereotype.Component;
@Component
public class ElectricityFeeCalculator implements FeeCalculator {
    public BigDecimal calculate(BigDecimal price,BigDecimal quantity,int days,int periodDays) { return FeeCalculator.round(price.multiply(quantity)); }
}
