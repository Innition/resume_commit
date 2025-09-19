package cn.lazylhxzzy.resume_commit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 安全日志注解
 * 用于标记需要记录安全相关操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecurityLog {
    
    /**
     * 操作描述
     */
    String value() default "";
    
    /**
     * 风险级别：LOW, MEDIUM, HIGH
     */
    String riskLevel() default "LOW";
    
    /**
     * 操作类型
     */
    String operation() default "";
}
