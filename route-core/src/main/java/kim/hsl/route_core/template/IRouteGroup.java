package kim.hsl.route_core.template;

import java.util.Map;

import kim.hsl.router_annotation.model.RouteBean;

/**
 * 路由分组接口
 */
public interface IRouteGroup {
    void loadInto(Map<String, RouteBean> atlas);
}
