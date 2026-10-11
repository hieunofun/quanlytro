package com.hostel.config;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
@Aspect @Component
public class ApiTimingAspect {
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object time(ProceedingJoinPoint call) throws Throwable {
        long start=System.nanoTime();
        try {return call.proceed();} finally {
            LoggerFactory.getLogger(ApiTimingAspect.class).debug("API {} took {} ms",call.getSignature().toShortString(),(System.nanoTime()-start)/1_000_000);
        }
    }
}
