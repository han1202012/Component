package kim.hsl.router_annotation.model;

import javax.lang.model.element.Element;
import kim.hsl.router_annotation.Route;

/**
 * 存储路由节点信息的 Bean
 */
public class RouteBean {

    /**
     * 注解类的类型
     * Activity 界面 / Service 服务
     */
    public enum Type {
        ACTIVITY,
        SERVICE
    }

    private Type type;

    /**
     * 被注解的节点
     */
    private Element element;

    /**
     * 被注解类
     */
    private Class<?> clazz;

    /**
     * 路由地址
     */
    private String routeAddress;

    /**
     * 路由组
     */
    private String routeGroup;

    public RouteBean(Type type, Element element, Route route) {
        this.type = type;
        this.element = element;
        this.clazz = null;
        this.routeAddress = route.path();
        this.routeGroup = route.group();
    }

    public RouteBean(Type type, Class<?> clazz, String routeAddress, String routeGroup) {
        this.type = type;
        this.element = null;
        this.clazz = clazz;
        this.routeAddress = routeAddress;
        this.routeGroup = routeGroup;
    }

    public RouteBean(Type type, Element element, Class<?> clazz, String routeAddress, String routeGroup) {
        this.type = type;
        this.element = element;
        this.clazz = clazz;
        this.routeAddress = routeAddress;
        this.routeGroup = routeGroup;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getRouteAddress() {
        return routeAddress;
    }

    public void setRouteAddress(String routeAddress) {
        this.routeAddress = routeAddress;
    }

    public String getRouteGroup() {
        return routeGroup;
    }

    public void setRouteGroup(String routeGroup) {
        this.routeGroup = routeGroup;
    }

    @Override
    public String toString() {
        return "RouteBean{" +
                "type=" + type +
                ", element=" + element +
                ", clazz=" + clazz +
                ", routeAddress='" + routeAddress + '\'' +
                ", routeGroup='" + routeGroup + '\'' +
                '}';
    }
}
