package com.example.demo.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    private final ObjectMapper objectMapper;

    public LoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Pointcut("within(com.example.demo.controller..*)")
    public void controllerPointcut() {
    }

    @Pointcut("within(com.example.demo.service.implementation..*)")
    public void servicePointcut() {}

    @Pointcut("within(com.example.demo.messaging..*)")
    public void messagingPointcut() {}

    @Around("controllerPointcut()")
    public Object logHttp(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        HttpServletResponse response = attributes != null ? attributes.getResponse() : null;

        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryParams = request != null && request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String fullUri = requestUri + queryParams;

        String requestBody = serializeArgs(joinPoint.getArgs(), true);
        log.info("START HTTP {} {} | method={} | request={}", httpMethod, fullUri, method, requestBody);

        Object result = joinPoint.proceed();
        int status = response != null ? response.getStatus() : 200;

        log.info("END HTTP {} {} | status={} | durationMs={} | response={}",
                httpMethod, fullUri, status, System.currentTimeMillis() - start, serializeResult(result));

        return result;
    }

    @Around("servicePointcut() || messagingPointcut()")
    public Object logInternal(ProceedingJoinPoint joinPoint) throws Throwable {
        String type = joinPoint.getSignature().getDeclaringTypeName().contains("messaging") ? "RMQ" : "SERVICE";
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        String payload = serializeArgs(joinPoint.getArgs(), false);
        log.info("START {} | method={} | args={}", type, method, payload);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("ERROR {} | method={} | durationMs={} | error={}", type, method, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }

        log.info("END {} | method={} | durationMs={} | result={}", type, method, System.currentTimeMillis() - start, serializeResult(result));
        return result;
    }

    private String serializeArgs(Object[] argsArray, boolean filterHttp) {
        try {
            List<Object> args = Arrays.stream(argsArray)
                    .filter(arg -> !filterHttp || (!(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse)))
                    .toList();
            if (args.isEmpty()) return "";
            return objectMapper.writeValueAsString(args.size() == 1 ? args.getFirst() : args);
        } catch (Exception e) {
            return "[Error serializing args]";
        }
    }

    private String serializeResult(Object result) {
        try {
            if (result instanceof ResponseEntity<?> responseEntity) {
                return objectMapper.writeValueAsString(responseEntity.getBody());
            }
            return result != null ? objectMapper.writeValueAsString(result) : "null";
        } catch (Exception e) {
            return "[Error serializing result]";
        }
    }
}