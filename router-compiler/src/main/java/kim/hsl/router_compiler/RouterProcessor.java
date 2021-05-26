package kim.hsl.router_compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import kim.hsl.router_annotation.Route;
import kim.hsl.router_annotation.model.RouteBean;

import static javax.lang.model.element.Modifier.PUBLIC;

// 注解处理器接收的参数
@SupportedOptions("moduleName")
// 自动注册注解处理器
@AutoService(Processor.class)
// 支持的注解类型
@SupportedAnnotationTypes({"kim.hsl.router_annotation.Route"})
// 支持的 Java 版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RouterProcessor extends AbstractProcessor {

    /**
     * 注解处理器中使用 Messager 对象打印日志
     */
    private Messager mMessager;

    /**
     * 用于写出生成的 Java 代码
     */
    private Filer mFiler;

    /**
     * 注解节点工具
     */
    private Elements mElementUtils;
    /**
     * 类工具
     */
    private Types mTypeUtils;

    /**
     * 获取的 moduleName 参数
     */
    private String mModuleName;

    /**
     * 管理路由信息
     * 键 ( Key ) : 路由分组名称
     * 值 ( Value ) : 路由信息集合
     */
    private HashMap<String, ArrayList<RouteBean>> mGroupMap = new HashMap<>();

    /**
     * 管理 路由表信息
     * 键 ( Key ) : 组名
     * 值 ( Value ) : 类名
     */
    private Map<String, String> mRootMap = new TreeMap<>();

    /**
     * 该函数在初始化时调用 , 相当于构造函数
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        // 获取打印日志接口
        this.mMessager = processingEnvironment.getMessager();
        // 测试日志打印
        //mMessager.printMessage(Diagnostic.Kind.NOTE, "RouterProcessor : Messager Print Log");

        this.mFiler = processingEnvironment.getFiler();
        this.mElementUtils = processingEnvironment.getElementUtils();
        this.mTypeUtils = processingEnvironment.getTypeUtils();

        // 获取 moduleName 参数
        // 先获取 注解处理器 选项
        Map<String, String> options = processingEnvironment.getOptions();
        if (options != null){
            mModuleName = options.get("moduleName");
            //mMessager.printMessage(Diagnostic.Kind.NOTE, "RouterProcessor : 打印 moduleName 参数 : " + mModuleName);
        }
    }

    /**
     * 该函数在注解处理器注册时自动执行, 是处理注解的核心函数
     *
     * Set<? extends TypeElement> set 参数 : 该集合表示使用了相关注解的节点的集合
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //mMessager.printMessage(Diagnostic.Kind.NOTE, "RouterProcessor : " + mModuleName + " process ");
        if (set == null || set.isEmpty()){
            // 如果没有检测到注解 , 直接退出
            //mMessager.printMessage(Diagnostic.Kind.NOTE, "RouterProcessor : 检测到注解为空 , 直接退出 mModuleName : " + mModuleName);
            return false;
        }

        // 获取被 @Route 注解的节点
        // 这些 注解节点 都是类节点 , TypeElement 类型的
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        generateRouteClass(routeElements);

        // 生成 路由组件 分组表 对应的 Java 路由表 类
        generateGroupTable();

        // 生成 Root 路由表 , 组名 <-> 路由表类
        generateRootTable();

        return true;
    }

    /**
     * 生成 Root 表
     */
    private void generateRootTable() {
        // 获取 kim.hsl.route_core.template.IRouteGroup 类节点
        TypeElement iRouteGroup = mElementUtils.getTypeElement("kim.hsl.route_core.template.IRouteGroup");
        // 获取 kim.hsl.route_core.template.IRouteRoot 类节点
        TypeElement iRouteRoot = mElementUtils.getTypeElement("kim.hsl.route_core.template.IRouteRoot");

        // 生成参数类型名称
        // Map<String,Class<? extends IRouteGroup>> routes>
        ParameterizedTypeName routesTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(iRouteGroup))
                )
        );

        // 生成参数
        // Map<String,Class<? extends IRouteGroup>> routes> routes
        ParameterSpec rootParameterSpec = ParameterSpec.builder(routesTypeName, "routes")
                .build();

        // 生成函数
        // public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        MethodSpec.Builder loadIntoMethodBuilder = MethodSpec.methodBuilder("loadInto")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(rootParameterSpec);

        // 生成函数体
        for (Map.Entry<String, String> entry : mRootMap.entrySet()) {
            loadIntoMethodBuilder.addStatement(
                    "routes.put($S, $T.class)",
                    entry.getKey(),
                    ClassName.get("kim.hsl.router", entry.getValue())
            );
        }

        // 生成 Root 类
        String rootClassName = "Router_Root_" + mModuleName;

        // 创建 Java 类
        TypeSpec typeSpec = TypeSpec.classBuilder(rootClassName)
                .addSuperinterface(ClassName.get(iRouteRoot))
                .addModifiers(PUBLIC)
                .addMethod(loadIntoMethodBuilder.build())
                .build();

        // 生成 Java 源文件
        JavaFile javaFile = JavaFile.builder("kim.hsl.router", typeSpec).build();

        // 写出到文件中
        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成 路由组件 分组表 对应的 Java 类
     */
    private void generateGroupTable() {
        // 获取要生成的类 需要实现的接口节点
        TypeElement iRouteGroup = mElementUtils.getTypeElement(
                "kim.hsl.route_core.template.IRouteGroup");

        // 打印类节点全类名
        //mMessager.printMessage(Diagnostic.Kind.NOTE, "打印 路由表 需要实现的接口节点 iRouteGroup : " + iRouteGroup.getQualifiedName());

        // 生成参数类型 Map<String, RouteBean> atlas
        ParameterizedTypeName atlasType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteBean.class)
        );

        // 生成参数 Map<String, RouteBean> atlas
        ParameterSpec atlasValue = ParameterSpec.builder(atlasType, "atlas").build();

        // 遍历 HashMap<String, ArrayList<RouteBean>> mGroupMap = new HashMap<>() 路由分组
        // 为每个 路由分组 创建一个类
        for (Map.Entry<String, ArrayList<RouteBean>> entry : mGroupMap.entrySet()){
            // 创建函数 loadInto
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("loadInto")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(atlasValue);

            // 函数体中的代码生成

            // 获取 ArrayList<RouteBean> 数据
            ArrayList<RouteBean> groupRoutes = entry.getValue();
            // 组名
            String groupName = "";

            // 生成函数体代码
            for (RouteBean routeBean : groupRoutes){
                // 获取组名
                groupName = routeBean.getRouteGroup();

                // $S 表示字符串
                // $T 表示类
                // $L 表示字面量 , 原封不动的字符串替换
                methodBuilder.addStatement("atlas.put($S, new $T($T.$L, $T.class, $S, $S))",
                        // $S 字符串 : "main"
                        routeBean.getRouteGroup(),
                        // $T 类名 : RouteBean
                        ClassName.get(RouteBean.class),
                        // $T 类名 : Type
                        ClassName.get(RouteBean.Type.class),
                        // $L 字面量 : ACTIVITY
                        routeBean.getType(),
                        // $T 类名 : kim.hsl.component.MainActivity 类
                        ClassName.get((TypeElement) routeBean.getElement()),
                        // $S 字符串 : "/app/MainActivity"
                        routeBean.getRouteAddress(),
                        // $S 字符串 : "app"
                        routeBean.getRouteGroup());
            }

            // 创建类

            // 构造类名  Router_Group_main
            String groupClassName = "Router_Group_" + groupName;

            // 创建类
            TypeSpec typeSpec = TypeSpec.classBuilder(groupClassName)
                    .addSuperinterface(ClassName.get(iRouteGroup))
                    .addModifiers(PUBLIC)
                    .addMethod(methodBuilder.build())
                    .build();

            // 生成 Java 源码文件
            JavaFile javaFile = JavaFile.builder("kim.hsl.router", typeSpec).build();

            // 将 Java 源文件写出到相应目录中
            try {
                //mMessager.printMessage(Diagnostic.Kind.NOTE, "输出文件 : " + groupClassName);
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
                //mMessager.printMessage(Diagnostic.Kind.NOTE, "输出文件出现异常");
            }finally {
                //mMessager.printMessage(Diagnostic.Kind.NOTE, "输出文件完毕");
            }

            // 统计路由表信息
            mRootMap.put(groupName, groupClassName);
        }
    }


    private void generateRouteClass(Set<? extends Element> routeElements) {
        // 获取 android.app.Activity 类型的注解节点
        TypeElement activityElement = mElementUtils.getTypeElement("android.app.Activity");
        // 获取 组件间共享服务 的接口, 该接口仅用于表示组件类型
        TypeElement iServiceElement = mElementUtils.getTypeElement("kim.hsl.route_core.template.IService");

        // 处理 @Route(path = "app/MainActivity") 节点
        for (Element element : routeElements) {
            // 获取 Route 注解
            Route route = element.getAnnotation(Route.class);
            // 路由表中的单个路由对象
            RouteBean routeBean = null;

            // 判断 typeMirror 注解节点是否是 Activity 类型
            if (mTypeUtils.isSubtype(element.asType(), activityElement.asType())) {
                // 该节点是 android.app.Activity 类型的
                routeBean = new RouteBean(
                        RouteBean.Type.ACTIVITY,    // 路由对象类型
                        element,         // 路由节点
                        null,    // 类对象
                        route.path(),   // 路由地址
                        route.group()); // 路由组
            }else if (mTypeUtils.isSubtype(element.asType(), iServiceElement.asType())) {
                // 该节点是 kim.hsl.route_core.template.IService 类型的
                routeBean = new RouteBean(
                        RouteBean.Type.ISERVICE,    // 路由对象类型
                        element,         // 路由节点
                        null,    // 类对象
                        route.path(),   // 路由地址
                        route.group()); // 路由组
            }else{
                // 该节点不是 android.app.Activity 类型的
                throw new RuntimeException("@Route 注解节点类型错误");
            }

            // 检查路由地址
            checkRouteAddress(routeBean);

            // 打印路由信息
            //mMessager.printMessage(Diagnostic.Kind.NOTE, "打印路由信息 : " + routeBean.toString());

            // 处理路由信息分组
            routeGroup(routeBean);
        }
    }

    /**
     * 处理路由信息分组
     * @param routeBean
     */
    private void routeGroup(RouteBean routeBean) {
        // 首先从 groupMap 集合中获取该分组的所有 路由信息
        ArrayList<RouteBean> routeBeans = mGroupMap.get(routeBean.getRouteGroup());

        if (routeBeans == null){
            // 如果从 mGroupMap 获取的该分组的路由信息集合为空
            // 则创建新集合, 放置路由信息, 并加入到 mGroupMap 中
            routeBeans = new ArrayList<>();
            routeBeans.add(routeBean);
            mGroupMap.put(routeBean.getRouteGroup(), routeBeans);
        }else{
            // 从 mGroupMap 获取的路由分组对应的路由信息集合不为空
            // 直接添加 路由信息 即可
            routeBeans.add(routeBean);
        }
    }

    /**
     * 验证路由地址
     * @Route(path = "/app/MainActivity")
     * @param routeBean
     */
    private void checkRouteAddress(RouteBean routeBean){
        // 获取路由地址
        String routeAddress = routeBean.getRouteAddress();
        // 获取路由分组
        String routeGroup = routeBean.getRouteGroup();

        // 验证路由地址是否以 "/" 开头
        if (!routeAddress.startsWith("/")) {
            throw new RuntimeException("路由地址 " + routeAddress + " 格式错误");
        }

        // 如果路由地址的分组为空 ,
        // 则截取第 0 和 第 1 个 "/" 之间的字符串作为分组名称
        if (routeGroup == null || "".equals(routeGroup)){
            String group = routeAddress.substring(
                    routeAddress.indexOf("/", 0) + 1,
                    routeAddress.indexOf("/", 1)
            );

            if (group == null || "".equals(group)){
                throw new RuntimeException("路由地址 " + routeAddress + " 获取分组错误");
            }

            // 打印组名
            //mMessager.printMessage(Diagnostic.Kind.NOTE, "打印路由地址 " + routeAddress + " 的组名为 " + group);

            // 正式设置路由地址分组
            routeBean.setRouteGroup(group);
        }
    }
}
