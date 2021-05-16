package kim.hsl.router_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Route {
    /**
     * 路由路径, 标识一个路由节点
     * 该字段没有默认值, 必须设置
     * @return
     */
    String path();

    /**
     * 路由分组, 默认为空, 选择性设置
     * 路由节点可以按照分组进行加载
     * @return
     */
    String group() default "";
}
