package com.example.recruitment.aspect;

import com.example.recruitment.annotation.Log;
import com.example.recruitment.entity.SysLog;
import com.example.recruitment.service.SysLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Spring AOP 操作日志切面
 * <p>
 * 拦截所有标注了 @Log 注解的Controller方法，自动记录操作日志到sys_log表。
 * 采用@Async异步写入，不影响业务接口性能。
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogService sysLogService;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知：拦截 @Log 注解标注的方法
     */
    @Around("@annotation(com.example.recruitment.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        // 构建日志实体
        SysLog sysLog = new SysLog();
        sysLog.setAction(logAnnotation.value());
        sysLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName());
        sysLog.setCreatedAt(java.time.LocalDateTime.now());

        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            sysLog.setUri(request.getRequestURI());
            sysLog.setIp(getIpAddress(request));
        }

        // 获取当前用户
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                sysLog.setUsername(authentication.getName());
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
        }

        // 记录请求参数
        if (logAnnotation.saveParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    // 过滤掉HttpServletRequest等不可序列化参数
                    StringBuilder paramsBuilder = new StringBuilder();
                    for (Object arg : args) {
                        if (arg instanceof HttpServletRequest) continue;
                        if (arg instanceof jakarta.servlet.http.HttpServletResponse) continue;
                        try {
                            String json = objectMapper.writeValueAsString(arg);
                            if (paramsBuilder.length() + json.length() > 2000) {
                                paramsBuilder.append(json, 0, 2000 - paramsBuilder.length());
                                break;
                            }
                            paramsBuilder.append(json).append(";");
                        } catch (Exception e) {
                            paramsBuilder.append(arg.getClass().getSimpleName()).append(";");
                        }
                    }
                    sysLog.setParams(paramsBuilder.toString());
                }
            } catch (Exception e) {
                log.debug("序列化请求参数失败: {}", e.getMessage());
            }
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
            sysLog.setSuccess(true);
        } catch (Throwable e) {
            sysLog.setSuccess(false);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            sysLog.setErrorMsg(errorMsg);
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("操作日志: action={}, method={}, uri={}, elapsed={}ms",
                    sysLog.getAction(), sysLog.getMethod(), sysLog.getUri(), elapsed);
            // 异步保存日志
            sysLogService.save(sysLog);
        }

        return result;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
