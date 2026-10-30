package com.hostel.model49;

import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import static com.hostel.service.Database.require;

public final class Input49 {
    private Input49(){}
    public static Object value(Map<String,Object> m,String... names){for(String n:names)if(m.get(n)!=null)return m.get(n);return null;}
    public static String text(Map<String,Object> m,String fallback,String... names){Object v=value(m,names);return v==null?fallback:v.toString();}
    public static String required(Map<String,Object> m,String... names){String v=text(m,"",names).trim();require(!v.isEmpty(),"Required: "+names[0]);return v;}
    public static Long optionalId(Map<String,Object> m,String... names){Object v=value(m,names);return v==null||v.toString().isBlank()?null:Long.valueOf(v.toString());}
    public static long keyId(Map<String,Object> m,String... names){Long v=optionalId(m,names);require(v!=null&&v>0,"Required positive ID: "+names[0]);return v;}
    public static BigDecimal amount(Map<String,Object> m,String fallback,String... names){Object v=value(m,names);return new BigDecimal(v==null?fallback:v.toString());}
    public static BigDecimal cashAmount(Map<String,Object> m,String... names){
        BigDecimal v=amount(m,"0",names);
        require(v.signum()>0&&v.stripTrailingZeros().scale()<=2&&v.precision()-v.scale()<=12,"Money must be positive, at most two decimal places, and within numeric(14,2)");
        return v;
    }
    public static LocalDate monthDay(LocalDate month,int day){require(day>=1&&day<=31,"Billing day must be between 1 and 31");return month.withDayOfMonth(Math.min(day,month.lengthOfMonth()));}
    public static boolean bool(Map<String,Object> m,boolean fallback,String... names){Object v=value(m,names);return v==null?fallback:Boolean.parseBoolean(v.toString());}
    public static LocalDate date(Object v){if(v instanceof LocalDate d)return d;if(v instanceof java.sql.Date d)return d.toLocalDate();String s=v.toString();return s.length()==7?YearMonth.parse(s).atDay(1):LocalDate.parse(s.substring(0,10));}
    public static LocalDate day(Map<String,Object> m,LocalDate fallback,String... names){Object v=value(m,names);return v==null?fallback:date(v);}
    public static OffsetDateTime time(Object v){
        require(v!=null,"Explicit event date/time is required");if(v instanceof OffsetDateTime d)return d.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        if(v instanceof java.sql.Timestamp t)return t.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        String s=v.toString();if(s.length()==10)return LocalDate.parse(s).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        try{return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}catch(java.time.format.DateTimeParseException e){return LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();}
    }
    public static OffsetDateTime now(){return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
}
