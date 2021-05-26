package kim.hsl.router_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数自动注入注解
 * 该注解使用在 成员字段 上面
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Extra {
    /**
     * 参数名称
     * @return
     */
    String name() default "";
}
