package kim.hsl.router_compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import kim.hsl.router_annotation.Extra;
import kim.hsl.router_annotation.Route;
import kim.hsl.router_annotation.model.RouteBean;

import static javax.lang.model.element.Modifier.PUBLIC;

// 注解处理器接收的参数
@SupportedOptions("moduleName")
// 自动注册注解处理器
@AutoService(Processor.class)
// 支持的注解类型
@SupportedAnnotationTypes({"kim.hsl.router_annotation.Extra"})
// 支持的 Java 版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ExtraProcessor extends AbstractProcessor {

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
     * 获取所有需要注入的节点集合 , 并按照其父节点 Activity 进行分组
     * 键 ( Key ) : Activity 节点
     * 值 ( Value ) : Activity 中被 @Extra 注解的属性节点
     */
    private Map<TypeElement, List<Element>> mActivity2Field = new HashMap<>();


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
        mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : Messager Print Log");

        this.mFiler = processingEnvironment.getFiler();
        this.mElementUtils = processingEnvironment.getElementUtils();
        this.mTypeUtils = processingEnvironment.getTypeUtils();

        // 获取 moduleName 参数
        // 先获取 注解处理器 选项
        Map<String, String> options = processingEnvironment.getOptions();
        if (options != null){
            mModuleName = options.get("moduleName");
            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : 打印 moduleName 参数 : " + mModuleName);
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
        mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : " + mModuleName + " process ");
        if (set == null || set.isEmpty()){
            // 如果没有检测到注解 , 直接退出
            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : 检测到注解为空 , 直接退出 mModuleName : " + mModuleName);
            return false;
        }

        // 获取被 @Extra 注解的属性节点集合
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Extra.class);

        // 采集这些属性节点集合的 类型 和 变量名称
        for (Element element : elements) {
            // 获取这些被 @Extra 标注的字段的父节点 Activity 节点
            TypeElement activityElement = (TypeElement) element.getEnclosingElement();
            if (mActivity2Field.containsKey(activityElement)) {
                // 如果该 Activity 父节点存在 , 直接添加到子节点集合中
                mActivity2Field.get(activityElement).add(element);
            } else {
                // 如果该 Activity 父节点不存在 , 先创建子节点集合 , 再添加到集合中
                List<Element> childs = new ArrayList<>();
                childs.add(element);
                mActivity2Field.put(activityElement, childs);
            }
            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : " + mModuleName + " 添加注解类型 : " + element.getSimpleName());
        }

        // 至此 , 已经将所有 Activity 以及其下使用 @Extra 标注的属性都存放在了
        // Map<TypeElement, List<Element>> mActivity2Field 集合中


        /*
            生成 Java 代码
         */

        // 获取 Activity 类型
        TypeMirror activityTypeMirror = mElementUtils.getTypeElement("android.app.Activity").asType();
        // 获取 IExtra 接口类型节点
        TypeElement IExtra = mElementUtils.getTypeElement("kim.hsl.route_core.template.IExtra");

        // 生成 IExtra 接口中 void loadExtra(Object target); 方法的 Object target 参数
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();

        // 遍历所有需要注入的 类:属性
        for (Map.Entry<TypeElement, List<Element>> entry : mActivity2Field.entrySet()) {
            // 每个 Map 键值对元素都要生成一个对应的 Java 类

            // 获取 Activity 类
            TypeElement rawClassElement = entry.getKey();
            // 如果该类不是 Activity 子类 , 直接抛出异常
            if (!mTypeUtils.isSubtype(rawClassElement.asType(), activityTypeMirror)) {
                throw new RuntimeException("ExtraProcessor Activity 类型错误");
            }

            // 创建 void loadExtra(Object target) 方法
            MethodSpec.Builder builder = MethodSpec.methodBuilder("loadExtra")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(objectParamSpec);

            // 生成类型转换代码 : MainActivity t = (MainActivity)target;
            // 获取 Activity 类名称
            ClassName className = ClassName.get(rawClassElement);
            // 类型转换, 将 Activity 类转为指定的 Activity 子类类型
            builder.addStatement("$T t = ($T)target", className, className);

            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : 开始循环 Map 元素个数" + entry.getValue().size());

            // 遍历被 @Extra 标注的属性字段
            for (int i = 0; i < entry.getValue().size(); i++) {
                Element element = entry.getValue().get(i);
                buildStatement(element, builder);
            }

            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : 结束循环 Map 元素个数" + entry.getValue().size());


            // 生成 java 类名, 原来的 Activity 类名基础上添加 "_Extra" 后缀
            String extraClassName = rawClassElement.getSimpleName() + "_Extra";

            // 创建 Java 类
            TypeSpec typeSpec = TypeSpec.classBuilder(extraClassName)
                    .addSuperinterface(ClassName.get(IExtra))   // 实现 IExtra 接口
                    .addModifiers(PUBLIC)   //
                    .addMethod(builder.build()) // 设置函数
                    .build();   // 正式创建

            // Java 文件
            JavaFile javaFile = JavaFile.builder(className.packageName(), typeSpec).build();

            // 写出生成的 java 代码
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }


            mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : 生成文件结束 : " + mModuleName + " " +javaFile.toString());
        }
        return true;
    }

    /**
     * 拼装如下代码
     * t.a = t.getIntent().getStringExtra("a");
     * @param element
     */
    public void buildStatement(Element element, MethodSpec.Builder builder) {
        TypeMirror typeMirror = element.asType();
        int type = typeMirror.getKind().ordinal();

        //属性名 String text 获得text
        String fieldName = element.getSimpleName().toString();
        //获得注解 name值 , 默认是传入的 name 注解属性值
        String extraName = element.getAnnotation(Extra.class).name();

        mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : " + mModuleName + " 处理注解类型 : " + typeMirror.toString() + " , 字段名称 : " + fieldName + " , 注解属性值 : " + extraName);

        if (extraName == null || extraName.length() == 0) {
            // 如果 name 注解属性值为空 , 则取值 字段名称
            extraName = fieldName;
        }

        mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : extraName : " + extraName);


        String defaultValue = "t." + fieldName;
        String statement = defaultValue + " = t.getIntent().";
        if (type == TypeKind.BOOLEAN.ordinal()) {
            statement += "getBooleanExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.BYTE.ordinal()) {
            statement += "getByteExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.SHORT.ordinal()) {
            statement += "getShortExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.INT.ordinal()) {
            statement += "getIntExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.LONG.ordinal()) {
            statement += "getLongExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.CHAR.ordinal()) {
            statement += "getCharExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.FLOAT.ordinal()) {
            statement += "getFloatExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.DOUBLE.ordinal()) {
            statement += "getDoubleExtra($S, " + defaultValue + ")";
        } else{
            //数组类型
            if (type == TypeKind.ARRAY.ordinal()) {
                addArrayStatement(statement, fieldName, extraName, typeMirror, element, builder);
            } else {
                // 对象类型
                addObjectStatement(statement, fieldName, extraName, typeMirror, element, builder);
            }
            return;
        }
        builder.addStatement(statement, extraName);

        mMessager.printMessage(Diagnostic.Kind.NOTE, "ExtraProcessor : extraName : " + extraName + " 生成完毕");
    }

    private void addArrayStatement(String statement, String fieldName, String extraName, TypeMirror
            typeMirror, Element elementm , MethodSpec.Builder builder) {

        // 获取 Parcelable 类型
        TypeMirror parcelableType = mElementUtils.getTypeElement("android.os.Parcelable").asType();

        // 处理数组
        switch (typeMirror.toString()) {
            case "boolean[]":
                statement += "getBooleanArrayExtra($S)";
                break;
            case "int[]":
                statement += "getIntArrayExtra($S)";
                break;
            case "short[]":
                statement += "getShortArrayExtra($S)";
                break;
            case "float[]":
                statement += "getFloatArrayExtra($S)";
                break;
            case "double[]":
                statement += "getDoubleArrayExtra($S)";
                break;
            case "byte[]":
                statement += "getByteArrayExtra($S)";
                break;
            case "char[]":
                statement += "getCharArrayExtra($S)";
                break;
            case "long[]":
                statement += "getLongArrayExtra($S)";
                break;
            case "java.lang.String[]":
                statement += "getStringArrayExtra($S)";
                break;
            default:
                // 处理 Parcelable 数组
                String defaultValue = "t." + fieldName;
                // object数组 componentType 获得 object类型
                ArrayTypeName arrayTypeName = (ArrayTypeName) ClassName.get(typeMirror);
                TypeElement typeElement = mElementUtils.getTypeElement(arrayTypeName
                        .componentType.toString());

                // 如果不是 Parcelable 抛异常退出
                if (!mTypeUtils.isSubtype(typeElement.asType(), parcelableType)) {
                    throw new RuntimeException("不支持的 Extra 类型 : " + typeMirror);
                }

                // 字符串格式
                statement = "$T[] " + fieldName + " = t.getIntent()" + ".getParcelableArrayExtra" + "($S)";
                builder.addStatement(statement, parcelableType, extraName);
                builder.beginControlFlow("if( null != $L)", fieldName);
                statement = defaultValue + " = new $T[" + fieldName + ".length]";
                builder.addStatement(statement, arrayTypeName.componentType)
                        .beginControlFlow("for (int i = 0; i < " + fieldName + "" +
                                ".length; " +
                                "i++)")
                        .addStatement(defaultValue + "[i] = ($T)" + fieldName + "[i]",
                                arrayTypeName.componentType)
                        .endControlFlow();
                builder.endControlFlow();
                return;
        }
        builder.addStatement(statement, extraName);
    }

    private void addObjectStatement(String statement, String fieldName, String extraName,
                                    TypeMirror typeMirror,
                                    Element element, MethodSpec.Builder builder) {
        // 获取 Parcelable 类型
        TypeMirror parcelableType = mElementUtils.getTypeElement("android.os.Parcelable").asType();
        // 获取 IService 类型
        TypeMirror iServiceType = mElementUtils.getTypeElement("kim.hsl.route_core.template.IService").asType();

        if (mTypeUtils.isSubtype(typeMirror, parcelableType)) {
            statement += "getParcelableExtra($S)";

        } else if (typeMirror.toString().equals("java.lang.String")) {
            statement += "getStringExtra($S)";

        } else if (mTypeUtils.isSubtype(typeMirror, iServiceType)) {

            ClassName routerClassName = ClassName.get("kim.hsl.route_core", "Router");

            statement = "t." + fieldName + " = ($T) $T.getInstance().build($S).navigation()";
            builder.addStatement(statement, TypeName.get(element.asType()), routerClassName,
                    extraName);
            return;

        } else {
            // List
            TypeName typeName = ClassName.get(typeMirror);
            //泛型
            if (typeName instanceof ParameterizedTypeName) {
                //list 或 arraylist
                ClassName rawType = ((ParameterizedTypeName) typeName).rawType;
                //泛型类型
                List<TypeName> typeArguments = ((ParameterizedTypeName) typeName)
                        .typeArguments;
                if (!rawType.toString().equals("java.util.ArrayList") && !rawType.toString()
                        .equals("java.util.List")) {
                    throw new RuntimeException("Not Support Inject Type:" + typeMirror + " " +
                            element);
                }
                if (typeArguments.isEmpty() || typeArguments.size() != 1) {
                    throw new RuntimeException("List Must Specify Generic Type:" + typeArguments);
                }
                TypeName typeArgumentName = typeArguments.get(0);
                TypeElement typeElement = mElementUtils.getTypeElement(typeArgumentName
                        .toString());

                // Parcelable 类型
                if (mTypeUtils.isSubtype(typeElement.asType(), parcelableType)) {
                    statement += "getParcelableArrayListExtra($S)";
                } else if (typeElement.asType().toString().equals("java.lang.String")) {
                    statement += "getStringArrayListExtra($S)";
                } else if (typeElement.asType().toString().equals("java.lang.Integer")) {
                    statement += "getIntegerArrayListExtra($S)";
                } else {
                    throw new RuntimeException("Not Support Generic Type : " + typeMirror + " " +
                            element);
                }
            } else {
                throw new RuntimeException("Not Support Extra Type : " + typeMirror + " " +
                        element);
            }
        }
        builder.addStatement(statement, extraName);
    }

}
