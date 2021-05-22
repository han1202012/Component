package kim.hsl.router_compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

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
     * 该函数在初始化时调用 , 相当于构造函数
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        // 获取打印日志接口
        this.mMessager = processingEnvironment.getMessager();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "Messager Print Log");

        this.mFiler = processingEnvironment.getFiler();
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
        for (TypeElement typeElement: set){
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

        }
        return false;
    }
}
