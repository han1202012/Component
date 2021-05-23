package kim.hsl.router_compiler;

import com.google.auto.service.AutoService;

import java.util.Map;
import java.util.Set;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import kim.hsl.router_annotation.Route;
import kim.hsl.router_annotation.model.RouteBean;

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
     * 该函数在初始化时调用 , 相当于构造函数
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        // 获取打印日志接口
        this.mMessager = processingEnvironment.getMessager();
        // 测试日志打印
        mMessager.printMessage(Diagnostic.Kind.NOTE, "Messager Print Log");

        this.mFiler = processingEnvironment.getFiler();
        this.mElementUtils = processingEnvironment.getElementUtils();
        this.mTypeUtils = processingEnvironment.getTypeUtils();

        // 获取 moduleName 参数
        // 先获取 注解处理器 选项
        Map<String, String> options = processingEnvironment.getOptions();
        if (options != null){
            mModuleName = options.get("moduleName");
            mMessager.printMessage(Diagnostic.Kind.NOTE, "打印 moduleName 参数 : " + mModuleName);
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
        if (set == null || set.isEmpty()){
            // 如果没有检测到注解 , 直接退出
            return false;
        }

        // 获取被 @Route 注解的节点
        // 这些 注解节点 都是类节点 , TypeElement 类型的
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        generateRouteClass(routeElements);

        /*for (TypeElement typeElement: set){

            // 遍历注解节点
            mMessager.printMessage(Diagnostic.Kind.NOTE, "SupportedAnnotationTypes : " + typeElement.getQualifiedName());


            // 生成 public static void main(String[] args) 函数
            MethodSpec main = MethodSpec.methodBuilder("main")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(String[].class, "args")
                    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
                    .build();

            // 指定 public final class HelloWorld 类
            TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(main)
                    .build();

            // 正式在 "com.example.helloworld" 包名下创建 HelloWorld 类
            JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
                    .build();

            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }*/
        return false;
    }

    private void generateRouteClass(Set<? extends Element> routeElements) {
        // 获取 android.app.Activity 类型的注解节点
        TypeElement activityElement = mElementUtils.getTypeElement("android.app.Activity");

        // 处理 @Route(path = "app/MainActivity") 节点
        for (Element element : routeElements) {
            // 获取 Route 注解
            Route route = element.getAnnotation(Route.class);
            // 路由表中的单个路由对象
            RouteBean routeBean = null;

            // 打印类节点全类名
            mMessager.printMessage(Diagnostic.Kind.NOTE,
                    "打印类节点 typeElement : " + activityElement.getQualifiedName());

            // 判断 typeMirror 注解节点是否是 Activity 类型
            if (mTypeUtils.isSubtype(element.asType(), activityElement.asType())) {
                // 该节点是 android.app.Activity 类型的
                routeBean = new RouteBean(
                        RouteBean.Type.ACTIVITY,    // 路由对象类型
                        element,         // 路由节点
                        null,    // 类对象
                        route.path(),   // 路由地址
                        route.group()); // 路由组

                // 检查路由地址
                checkRouteAddress(routeBean);

                // 打印路由信息
                mMessager.printMessage(Diagnostic.Kind.NOTE,
                        "打印路由信息 : " + routeBean.toString());

            }else{
                // 该节点不是 android.app.Activity 类型的
                throw new RuntimeException("@Route 注解节点类型错误");
            }
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
            mMessager.printMessage(Diagnostic.Kind.NOTE,
                    "打印路由地址 " + routeAddress + " 的组名为 " + group);

            // 正式设置路由地址分组
            routeBean.setRouteGroup(group);
        }
    }
}
