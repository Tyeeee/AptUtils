package com.yjt.apt.router.compiler;

import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.yjt.apt.router.annotation.Parameter;
import com.yjt.apt.router.annotation.Route;
import com.yjt.apt.router.annotation.constant.RouteType;
import com.yjt.apt.router.annotation.model.RouteMetadata;
import com.yjt.apt.router.compiler.messager.Messager;
import com.yjt.apt.router.compiler.constant.Constant;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
public class RouteProcessor extends AbstractProcessor {

    private Map<String, Set<RouteMetadata>> groupMap = new HashMap<>(); // ModuleName and RouteMetadata.
    private Map<String, String> rootMap = new TreeMap<>();  // Map of root metas, used for generate class file in order.
    private Messager messager;
    private Types types;
    private Elements elements;
    private Filer filer;       // File util, write class file into disk.
    private String moduleName;   // Module name, maybe its 'app' or others

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
            throw new RuntimeException("Router::Compiler >>> No module name, for more information, look at gradle log.");
        }
        messager.info(">>> RouteProcessor init. <<<");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportAnnotations = new HashSet<>();
        supportAnnotations.add(Route.class.getCanonicalName());     // This annotation mark class which can be router.
        return supportAnnotations;
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
        Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);
        boolean parseResult = false;
        try {
            messager.info(">>> Found routes, start... <<<");
            parseRoutes(routeElements);
            parseResult = true;
        } catch (IOException e) {
            messager.error(e);
        }
        return parseResult;
    }

    private void parseRoutes(Set<? extends Element> routeElements) throws IOException {
        if (CollectionUtils.isNotEmpty(routeElements)) {
            // Perpare the type an so on.
            messager.info(">>> Found routes, size is " + routeElements.size() + " <<<");
            rootMap.clear();
            // Fantastic four
            TypeElement type_Activity = elements.getTypeElement(Constant.ACTIVITY);
            TypeElement type_Service = elements.getTypeElement(Constant.SERVICE);
            // Interface of Router.
            TypeElement type_IProvider = elements.getTypeElement(Constant.IPROVIDER);
            TypeElement type_IRouteGroup = elements.getTypeElement(Constant.IROUTE_GROUP);
            TypeElement type_IProviderGroup = elements.getTypeElement(Constant.IPROVIDER_GROUP);
            ClassName RouteMetadataCn = ClassName.get(RouteMetadata.class);
            ClassName routeTypeCn = ClassName.get(RouteType.class);

            ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                    )
            );

            ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouteMetadata.class)
            );

            ParameterSpec rootParameterSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();
            ParameterSpec groupParameterSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
            ParameterSpec providerParameterSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();  // Ps. its Parameter type same as groupParameterSpec!

            MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(rootParameterSpec);

            //  Follow a sequence, find out metas of group first, generate java file, then statistics them as root.
            for (Element element : routeElements) {
                TypeMirror tm = element.asType();
                Route route = element.getAnnotation(Route.class);
                RouteMetadata routeMete = null;

                if (types.isSubtype(tm, type_Activity.asType())) {                 // Activity
                    messager.info(">>> Found activity route: " + tm.toString() + " <<<");

                    // Get all fields annotation by @Parameter
                    Map<String, Integer> parametersType = new HashMap<>();
                    for (Element field : element.getEnclosedElements()) {
                        if (field.getKind().isField() && field.getAnnotation(Parameter.class) != null) {
                            Parameter ParameterConfig = field.getAnnotation(Parameter.class);
                            parametersType.put(StringUtils.isEmpty(ParameterConfig.name()) ? field.getSimpleName().toString() : field.getSimpleName().toString() + "|" + ParameterConfig.name(), exchange(field.asType()));
                        }
                    }
                    parametersType.size();
                    routeMete = new RouteMetadata(route, element, RouteType.ACTIVITY, parametersType);
                } else if (types.isSubtype(tm, type_IProvider.asType())) {         // IProvider
                    messager.info(">>> Found provider route: " + tm.toString() + " <<<");
                    routeMete = new RouteMetadata(route, element, RouteType.PROVIDER, null);
                } else if (types.isSubtype(tm, type_Service.asType())) {           // Service
                    messager.info(">>> Found service route: " + tm.toString() + " <<<");
                    routeMete = new RouteMetadata(route, element, RouteType.parse(Constant.SERVICE), null);
                }
                categories(routeMete);
//                if (StringUtils.isEmpty(moduleName)) {   // Hasn't generate the module name.
//                    moduleName = ModuleUtils.generateModuleName(element, messager);
//                }
            }

            MethodSpec.Builder loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(providerParameterSpec);

            // Start generate java source, structure is divided into upper and lower levels, used for demand initialization.
            for (Map.Entry<String, Set<RouteMetadata>> entry : groupMap.entrySet()) {
                String groupName = entry.getKey();

                MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(Constant.METHOD_LOAD_INTO)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(groupParameterSpec);
                // Build group method body
                Set<RouteMetadata> groupData = entry.getValue();
                for (RouteMetadata metadata : groupData) {
                    switch (metadata.getType()) {
                        case PROVIDER:  // Need cache provider's super class
                            List<? extends TypeMirror> interfaces = ((TypeElement) metadata.getRawType()).getInterfaces();
                            for (TypeMirror tm : interfaces) {
                                if (types.isSubtype(tm, type_IProvider.asType())) {
                                    // This interface extend the IProvider, so it can be used for mark provider
                                    loadIntoMethodOfProviderBuilder.addStatement(
                                            "providers.put($S, $T.build($T." + metadata.getType() + ", $T.class, $S, $S, null, " + metadata.getPriority() + ", " + metadata.getExtra() + "))",
                                            tm.toString().substring(tm.toString().lastIndexOf(".") + 1),    // Spite unuseless name
                                            RouteMetadataCn,
                                            routeTypeCn,
                                            ClassName.get((TypeElement) metadata.getRawType()),
                                            metadata.getPath(),
                                            metadata.getGroup());
                                }
                            }
                            break;
                        default:
                            break;
                    }

                    // Make map body for ParametersType
                    StringBuilder builder = new StringBuilder();
                    Map<String, Integer> ParametersType = metadata.getParametersType();
                    if (MapUtils.isNotEmpty(ParametersType)) {
                        for (Map.Entry<String, Integer> types : ParametersType.entrySet()) {
                            builder.append("put(\"").append(types.getKey()).append("\", ").append(types.getValue()).append("); ");
                        }
                    }
                    String mapBody = builder.toString();

                    loadIntoMethodOfGroupBuilder.addStatement(
                            "atlas.put($S, $T.build($T." + metadata.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + builder.toString() + "}}")) + ", " + metadata.getPriority() + ", " + metadata.getExtra() + "))",
                            metadata.getPath(),
                            RouteMetadataCn,
                            routeTypeCn,
                            ClassName.get((TypeElement) metadata.getRawType()),
                            metadata.getPath().toLowerCase(),
                            metadata.getGroup().toLowerCase());
                }

                // Generate groups
                String groupFileName = Constant.NAME_OF_GROUP + groupName;
                JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE,
                                 TypeSpec.classBuilder(groupFileName)
                                         .addJavadoc(Constant.WARNING_TIPS)
                                         .addSuperinterface(ClassName.get(type_IRouteGroup))
                                         .addModifiers(PUBLIC)
                                         .addMethod(loadIntoMethodOfGroupBuilder.build())
                                         .build()
                ).build().writeTo(filer);

                messager.info(">>> Generated group: " + groupName + "<<<");
                rootMap.put(groupName, groupFileName);
            }

            if (MapUtils.isNotEmpty(rootMap)) {
                // Generate root meta by group name, it must be generated before root, then I can findout the class of group.
                for (Map.Entry<String, String> entry : rootMap.entrySet()) {
                    loadIntoMethodOfRootBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(Constant.PACKAGE_OF_GENERATE_FILE, entry.getValue()));
                }
            }

            // Wirte provider into disk
            String providerMapFileName = Constant.NAME_OF_PROVIDER + Constant.SEPARATOR + moduleName;
            JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE,
                             TypeSpec.classBuilder(providerMapFileName)
                                     .addJavadoc(Constant.WARNING_TIPS)
                                     .addSuperinterface(ClassName.get(type_IProviderGroup))
                                     .addModifiers(PUBLIC)
                                     .addMethod(loadIntoMethodOfProviderBuilder.build())
                                     .build()
            ).build().writeTo(filer);

            messager.info(">>> Generated provider map, name is " + providerMapFileName + " <<<");

            // Write root meta into disk.
            String rootFileName = Constant.NAME_OF_ROOT + Constant.SEPARATOR + moduleName;
            JavaFile.builder(Constant.PACKAGE_OF_GENERATE_FILE,
                             TypeSpec.classBuilder(rootFileName)
                                     .addJavadoc(Constant.WARNING_TIPS)
                                     .addSuperinterface(ClassName.get(elements.getTypeElement(Constant.ITROUTE_ROOT)))
                                     .addModifiers(PUBLIC)
                                     .addMethod(loadIntoMethodOfRootBuilder.build())
                                     .build()
            ).build().writeTo(filer);

            messager.info(">>> Generated root, name is " + rootFileName + " <<<");
        }
    }

    private void categories(RouteMetadata routeMete) {
        if (routeVerify(routeMete)) {
            messager.info(">>> Start categories, group = " + routeMete.getGroup() + ", path = " + routeMete.getPath() + " <<<");
            Set<RouteMetadata> RouteMetadatas = groupMap.get(routeMete.getGroup());
            if (CollectionUtils.isEmpty(RouteMetadatas)) {
                Set<RouteMetadata> RouteMetadataSet = new TreeSet<>(new Comparator<RouteMetadata>() {
                    @Override
                    public int compare(RouteMetadata r1, RouteMetadata r2) {
                        try {
                            return r1.getPath().compareTo(r2.getPath());
                        } catch (NullPointerException npe) {
                            messager.error(npe.getMessage());
                            return 0;
                        }
                    }
                });
                RouteMetadataSet.add(routeMete);
                groupMap.put(routeMete.getGroup(), RouteMetadataSet);
            } else {
                RouteMetadatas.add(routeMete);
            }
        } else {
            messager.warning(">>> Route meta verify error, group is " + routeMete.getGroup() + " <<<");
        }
    }

    private boolean routeVerify(RouteMetadata meta) {
        String path = meta.getPath();
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {   // The path must be start with '/' and not empty!
            return false;
        }
        if (StringUtils.isEmpty(meta.getGroup())) { // Use default group(the first word in path)
            try {
                String defaultGroup = path.substring(1, path.indexOf("/", 1));
                if (StringUtils.isEmpty(defaultGroup)) {
                    return false;
                }
                meta.setGroup(defaultGroup);
                return true;
            } catch (StringIndexOutOfBoundsException e) {
                messager.error("Failed to extract default group! " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private int exchange(TypeMirror rawType) {
        if (rawType.getKind().isPrimitive()) {  // is java base type
            return rawType.getKind().ordinal();
        }
        switch (rawType.toString()) {
            case Constant.BYTE:
                return TypeKind.BYTE.ordinal();
            case Constant.SHORT:
                return TypeKind.SHORT.ordinal();
            case Constant.INTEGER:
                return TypeKind.INT.ordinal();
            case Constant.LONG:
                return TypeKind.LONG.ordinal();
            case Constant.FLOAT:
                return TypeKind.FLOAT.ordinal();
            case Constant.DOUBEL:
                return TypeKind.DOUBLE.ordinal();
            case Constant.BOOLEAN:
                return TypeKind.BOOLEAN.ordinal();
            case Constant.STRING:
            default:
                return TypeKind.OTHER.ordinal();  // I say it was java.long.String
        }
    }
}
