package kim.hsl.route_core;

import java.util.HashMap;
import java.util.Map;

import kim.hsl.route_core.template.IRouteGroup;
import kim.hsl.route_core.template.IService;
import kim.hsl.router_annotation.model.RouteBean;

/**
 * 存放路由表的静态类仓库
 */
public class Warehouse {

    /**
     * Root 映射表 , 保存分组信息
     */
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();

    /**
     * 保存一个路由分组中的路由数据
     */
    static Map<String, RouteBean> routes = new HashMap<>();

    /**
     * IService 服务的映射表
     */
    static Map<Class, IService> services = new HashMap<>();
}
