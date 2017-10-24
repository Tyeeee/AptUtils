package com.yjt.apt.router.compiler.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yjt.apt.router.annotation.Autowire;
import com.yjt.apt.router.compiler.constant.Constant;
import com.yjt.apt.router.compiler.messager.Messager;
import com.yjt.apt.router.compiler.utils.TypeUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
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

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedOptions(Constant.KEY_MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({Constant.ANNOTATION_TYPE_AUTOWIRE})
public class AutowireProcessor extends AbstractProcessor {

    private Filer filer;       // File util, write class file into disk.
    private Messager messager;
    private Types types;
    private Elements elements;
    private String moduleName;
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();   // Contain field need autowire and his super class.
    private static final ClassName ROUTER = ClassName.get("com.yjt.apt.router", "Router");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();                  // Generate class.
        types = processingEnv.getTypeUtils();            // Get type utils.
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
            throw new RuntimeException("Router_AutowireProcessor::Compiler >>> No module name, for more information, look at gradle log.");
        }
        messager.info(">>> AutowireProcessor init. <<<");
    }

//    @Override
//    public Set<String> getSupportedAnnotationTypes() {
//        return Collections.singleton(Autowire.class.getCanonicalName());
//    }
//
//    @Override
//    public SourceVersion getSupportedSourceVersion() {
//        return SourceVersion.latestSupported();
//    }
//
//    @Override
//    public Set<String> getSupportedOptions() {
//        return Sets.newHashSet(Constant.KEY_MODULE_NAME);
//    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                messager.info(">>> Found autowire field, start... <<<");
                categories(roundEnvironment.getElementsAnnotatedWith(Autowire.class));
                generateHelper();
            } catch (IOException | IllegalAccessException e) {
                messager.error(e);
            }
            return true;
        }
        return false;
    }

    private void generateHelper() throws IOException, IllegalAccessException {
        TypeElement typeElement = elements.getTypeElement(Constant.ISYRINGE);
        TypeMirror iProvider = elements.getTypeElement(Constant.IPROVIDER).asType();
        TypeMirror activityTm = elements.getTypeElement(Constant.ACTIVITY).asType();
        TypeMirror fragmentTm = elements.getTypeElement(Constant.FRAGMENT).asType();
        TypeMirror fragmentTmV4 = elements.getTypeElement(Constant.FRAGMENT_V4).asType();

        if (MapUtils.isNotEmpty(parentAndChild)) {
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                // Build method : 'inject'
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder(Constant.METHOD_INJECT)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(TypeName.OBJECT, "target").build());

                TypeElement parent = entry.getKey();
                List<Element> childs = entry.getValue();
                String qualifiedName = parent.getQualifiedName().toString();
                String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
                String fileName = parent.getSimpleName() + Constant.NAME_OF_AUTOWIRE;

                messager.info(">>> Start process " + childs.size() + " field in " + parent.getSimpleName() + " ... <<<");

                injectMethodBuilder.addStatement("$T substitute = ($T)target", ClassName.get(parent), ClassName.get(parent));

                // Generate method body, start inject.
                for (Element element : childs) {
                    Autowire fieldConfig = element.getAnnotation(Autowire.class);
                    String   fieldName   = element.getSimpleName().toString();
                    if (types.isSubtype(element.asType(), iProvider)) {  // It's provider
                        if ("".equals(fieldConfig.name())) {    // User has not set service path, then use byType.
                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = $T.getInstance().navigation($T.class)",
                                    ROUTER,
                                    ClassName.get(element.asType())
                            );
                        } else {    // use byName
                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = ($T)$T.getInstance().build($S).navigation();",
                                    ClassName.get(element.asType()),
                                    ROUTER,
                                    fieldConfig.name()
                            );
                        }
                        // Validater
                        if (fieldConfig.required()) {
                            injectMethodBuilder.beginControlFlow("if (substitute." + fieldName + " == null)");
                            injectMethodBuilder.addStatement(
                                    "throw new RuntimeException(\"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    } else {    // It's normal intent value
                        String statment = "substitute." + fieldName + " = substitute.";
                        boolean isActivity = false;
                        if (types.isSubtype(parent.asType(), activityTm)) {  // Activity, then use getIntent()
                            isActivity = true;
                            statment += "getIntent().";
                        } else if (types.isSubtype(parent.asType(), fragmentTm) || types.isSubtype(parent.asType(), fragmentTmV4)) {   // Fragment, then use getArguments()
                            statment += "getArguments().";
                        } else {
                            throw new IllegalAccessException("The field [" + fieldName + "] need autowire from intent, its parent must be activity or fragment!");
                        }
                        statment = buildStatement(statment, TypeUtil.typeExchange(element.asType()), isActivity);
                        injectMethodBuilder.addStatement(statment, StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name());
                        // Validater
                        if (fieldConfig.required() && !element.asType().getKind().isPrimitive()) {  // Primitive wont be check.
                            injectMethodBuilder.beginControlFlow("if (substitute." + fieldName + " == null)");
                            injectMethodBuilder.addStatement(
                                    "throw new RuntimeException(\"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    }
                }

                // Generate autowire helper
                JavaFile.builder(packageName,
                                 TypeSpec.classBuilder(fileName)
                                         .addJavadoc(Constant.WARNING_TIPS)
                                         .addSuperinterface(ClassName.get(typeElement))
                                         .addModifiers(PUBLIC)
                                         .addMethod(injectMethodBuilder.build())
                                         .build()
                ).build().writeTo(filer);
                messager.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }
            messager.info(">>> Autowire processor stop. <<<");
        }
    }

    private String buildStatement(String statment, int type, boolean isActivity) {
        if (type == TypeKind.BOOLEAN.ordinal()) {
            statment += (isActivity ? ("getBooleanExtra($S, false)") : ("getBoolean($S)"));
        } else if (type == TypeKind.BYTE.ordinal()) {
            statment += (isActivity ? ("getByteExtra($S, (byte) 0)") : ("getByte($S)"));
        } else if (type == TypeKind.SHORT.ordinal()) {
            statment += (isActivity ? ("getShortExtra($S, (short) 0)") : ("getShort($S)"));
        } else if (type == TypeKind.INT.ordinal()) {
            statment += (isActivity ? ("getIntExtra($S, 0)") : ("getInt($S)"));
        } else if (type == TypeKind.LONG.ordinal()) {
            statment += (isActivity ? ("getLongExtra($S, 0)") : ("getLong($S)"));
        } else if (type == TypeKind.FLOAT.ordinal()) {
            statment += (isActivity ? ("getFloatExtra($S, 0)") : ("getFloat($S)"));
        } else if (type == TypeKind.DOUBLE.ordinal()) {
            statment += (isActivity ? ("getDoubleExtra($S, 0)") : ("getDouble($S)"));
        } else if (type == TypeKind.OTHER.ordinal()) {
            statment += (isActivity ? ("getStringExtra($S)") : ("getString($S)"));
        }
        return statment;
    }

    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(elements)) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The autowire fields CAN NOT BE 'private'!!! please check field ["
                                                             + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }
                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> childs = new ArrayList<>();
                    childs.add(element);
                    parentAndChild.put(enclosingElement, childs);
                }
            }
            messager.info("categories finished.");
        }
    }
}
