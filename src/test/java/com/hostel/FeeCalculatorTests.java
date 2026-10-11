package com.hostel;
import com.hostel.service.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class FeeCalculatorTests {
    @Test void utilitiesUseConsumptionWithoutProration() {assertEquals(new BigDecimal("160000.00"),new WaterFeeCalculator().calculate(new BigDecimal("20000"),new BigDecimal("8"),15,30));}
    @Test void fixedServiceUsesPersonCountAndOccupiedDays() {assertEquals(new BigDecimal("30000.00"),new ServiceFeeCalculator().calculate(new BigDecimal("30000"),new BigDecimal("2"),15,30));}
    @Test void rentUsesCalendarDaysAndRoundsOnce() {assertEquals(new BigDecimal("1548387.10"),new RoomFeeCalculator().calculate(new BigDecimal("3000000"),BigDecimal.ONE,16,31));}
}
