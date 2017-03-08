package com.yjt.apt.router.compiler.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.yjt.apt.router.annotation.Interceptor;
import com.yjt.apt.router.compiler.constant.Constant;
import com.yjt.apt.router.compiler.messager.Messager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
//@SupportedOptions(Constant.KEY_MODULE_NAME)
//@SupportedSourceVersion(SourceVersion.RELEASE_7)
//@SupportedAnnotationTypes(Constant.ANNOTATION_TYPE_INTECEPTOR)
public class InterceptorProcessor extends AbstractProcessor {

    private Map<Integer, Element> interceptors = new TreeMap<>();
    private Elements elements;
    private Filer filer;       // File util, write class file into disk.
    private Messager messager;
    private String moduleName;   // Module name, maybe its 'app' or others

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();                  // Generate class.
        elements = processingEnv.getElementUtils();      // Get class meta.
        messager = new Messager(processingEnv.getMessager());   // Package the log utils.

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(Constant.KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");
            messager.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            messager.error("These no module name, at 'build.gradle', like :\n" +
                                   "apt {\n" +
                                   "    arguments {\n" +
                                   "        moduleName project.getName();\n" +
                                   "    }\n" +
                                   "}\n");
            throw new RuntimeException("Router_InterceptorProcessor::Compiler >>> No module name, for more information, look at gradle log.");
        }
        messager.info(">>> InterceptorProcessor init. <<<");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Interceptor.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Sets.newHashSet(Constant.KEY_MODULE_NAME);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            try {
                messager.info(">>> Found routes, start... <<<");
                parseInterceptors(roundEnv.getElementsAnnotatedWith(Interceptor.class));
            } catch (IOException e) {
                messager.error(e);
            }
            return true;
        }
        return false;
    }

    private void parseInterceptors(Set<? extends Element> elements) throws IOException {
        if (CollectionUtils.isNotEmpty(elements)) {
            messager.info(">>> Found interceptors, size is " + elements.size() + " <<<");

            // Verify and cache, sort incidentally.
            for (Element element : elements) {
                if (verify(element)) {  // Check the interceptor meta
                    messager.info("A interceptor verify over, its " + element.asType());
                    Interceptor interceptor = element.getAnnotation(Interceptor.class);
                    Element lastInterceptor = interceptors.get(interceptor.priority());
                    if (null != lastInterceptor) { // Added, throw exceptions
                        throw new IllegalArgumentException(
                                String.format(Locale.getDefault(), "More than one interceptors use same priority [%d], They are [%s] and [%s].",
                                              interceptor.priority(),
                                              lastInterceptor.getSimpleName(),
                                              element.getSimpleName())
                        );
                    }
                    interceptors.put(interceptor.priority(), element);
                } else {
                    messager.error("A interceptor verify failed, its " + element.asType());
                }
            }

            // Interface of Router.
            TypeElement type_ITollgate = this.elements.getTypeElement(Constant.IINTERCEPTOR);
            TypeElement type_ITollgateGroup = this.elements.getTypeElement(Constant.IINTERCEPTOR_GROUP);

            ParameterizedTypeName inputMapTypeOfTollgate = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Integer.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_ITollgate))
                    )
            );

            // Build input param name.
            ParameterSpec tollgateParamSpec = ParameterSpec.builder(inputMapTypeOfTollgate, "interceptors").build();

            // Build method : 'loadInto'
            MethodSpec.Builder loadIntoMethodOfTollgateBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(tollgateParamSpec);

            // Generate
            if (null != interceptors && interceptors.size() > 0) {
                // Build method body
                for (Map.Entry<Integer, Element> entry : interceptors.entrySet()) {
                    loadIntoMethodOfTollgateBuilder.addStatement("interceptors.put(" + entry.getKey() + ", $T.class)", ClassName.get((TypeElement) entry.getValue()));
                }
            }

            // Write to disk(Write file even interceptors is empty.)
            JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE,
                             TypeSpec.classBuilder(Constant.NAME_OF_INTERCEPTOR + Constant.SEPARATOR + moduleName)
                                     .addModifiers(PUBLIC)
                                     .addJavadoc(Constant.WARNING_TIPS)
                                     .addMethod(loadIntoMethodOfTollgateBuilder.build())
                                     .addSuperinterface(ClassName.get(type_ITollgateGroup))
                                     .build()
            ).build().writeTo(filer);
            messager.info(">>> Interceptor group write over. <<<");
        }
    }

    private boolean verify(Element element) {
        // It must be implement the interface IInterceptor and marked with annotation Interceptor.
        return null != element.getAnnotation(Interceptor.class) && ((TypeElement) element).getInterfaces().contains(elements.getTypeElement(Constant.IINTERCEPTOR).asType());
    }
}
