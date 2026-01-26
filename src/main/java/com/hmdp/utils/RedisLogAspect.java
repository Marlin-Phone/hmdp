//package com.hmdp.utils;
//
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.stereotype.Component;
//import java.util.Arrays;
//
//@Slf4j
//@Aspect
//@Component
//public class RedisLogAspect {
//    /**
//     * 拦截所有StringRedisTemplate和RedisTemplate的public方法
//     */
//    @Around("execution(public * org.springframework.data.redis.core.StringRedisTemplate.*(..)) || execution(public * org.springframework.data.redis.core.RedisTemplate.*(..))")
//    public Object logRedisOperation(ProceedingJoinPoint joinPoint) throws Throwable {
//        String methodName = joinPoint.getSignature().toShortString();
//        Object[] args = joinPoint.getArgs();
//        log.info("[Redis操作] 方法: {}，参数: {}", methodName, Arrays.toString(args));
//        Object result = null;
//        try {
//            result = joinPoint.proceed();
//            log.info("[Redis操作] 方法: {}，返回值: {}", methodName, result);
//        } catch (Throwable e) {
//            log.error("[Redis操作] 方法: {}，异常: {}", methodName, e.getMessage());
//            throw e;
//        }
//        return result;
//    }
//}
