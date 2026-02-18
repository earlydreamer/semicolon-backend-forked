package dukku.common.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @Scheduled 메서드 실행 시 MDC에 traceId, spanId를 자동 주입하는 AOP.
 * HTTP 컨텍스트가 없는 스케줄러에서도 로그 추적이 가능하도록 한다.
 */
@Slf4j
@Aspect
@Component
public class MdcSchedulerAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object injectMdc(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = "sched-" + UUID.randomUUID().toString().replace("-", "");
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put(MdcLoggingFilter.TRACE_ID, traceId);
        MDC.put(MdcLoggingFilter.SPAN_ID, spanId);

        try {
            return joinPoint.proceed();
        } finally {
            MDC.clear();
        }
    }
}
