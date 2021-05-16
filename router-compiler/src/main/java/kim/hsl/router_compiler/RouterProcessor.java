package kim.hsl.router_compiler;

import com.google.auto.service.AutoService;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

// 自动注册注解处理器
@AutoService(Processor.class)
// 支持的注解类型
@SupportedAnnotationTypes({"kim.hsl.router_annotation.Route"})
// 支持的 Java 版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RouterProcessor extends AbstractProcessor {

    /**
     * 注解处理器中使用 Messager 对象打印日志
     */
    private Messager mMessager;

    /**
     * 该函数在初始化时调用 , 相当于构造函数
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        // 获取打印日志接口
        mMessager = processingEnvironment.getMessager();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "Messager Print Log");
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
        }

        return false;
    }
}
