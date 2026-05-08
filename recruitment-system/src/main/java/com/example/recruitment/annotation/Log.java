package com.example.recruitment.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义操作日志注解
 * <p>
 * 标注在Controller方法上，AOP切面将自动记录该操作日志到sys_log表。
 * 使用示例：@Log(action = "用户登录")
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;
}
