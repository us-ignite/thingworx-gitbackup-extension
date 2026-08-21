package org.us_ignite.thingworx.dap;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/** Generates the exact ThingWorx entity surface declared by the DAP annotations. */
@Generated("org.us_ignite.thingworx.dap")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions({"dap.output", "dap.packageVersion", "dap.minimumThingWorxVersion"})
@SupportedAnnotationTypes({
    "org.us_ignite.thingworx.dap.*",
    "com.thingworx.metadata.annotations.ThingworxServiceDefinition"
})
public final class DeclarativeAnnotationProcessor extends AbstractProcessor {
    private static final String SDK_SERVICE =
            "com.thingworx.metadata.annotations.ThingworxServiceDefinition";
    private static final String SDK_PARAMETER =
            "com.thingworx.metadata.annotations.ThingworxServiceParameter";
    private static final String SDK_RESULT =
            "com.thingworx.metadata.annotations.ThingworxServiceResult";

    private final Set<String> emitted = new HashSet<>();
    private final Set<String> generatedResultShapes = new HashSet<>();
    private final Set<String> referencedServiceResults = new HashSet<>();
    private final Map<String, ShapeModel> shapes = new LinkedHashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(DapDataShape.class)) {
            generateDataShape((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapNoPayloadServiceResult.class)) {
            generateNoPayload((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapThingShape.class)) {
            generateThingShape((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapThingTemplate.class)) {
            generateThingTemplate((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapThing.class)) {
            generateThing((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapProject.class)) {
            generateProject((TypeElement) element);
        }
        for (Element element : round.getElementsAnnotatedWith(DapExtensionPackage.class)) {
            generateMetadata((TypeElement) element);
        }
        if (round.processingOver()) validateReferencedShapes();
        return false;
    }

    private void generateDataShape(TypeElement type) {
        DapDataShape declaration = type.getAnnotation(DapDataShape.class);
        List<FieldModel> fields = fields(type);
        validateFields(type, declaration.name(), fields);
        String resultName =
                declaration.serviceResultName().isBlank()
                        ? declaration.name().replace(".DataShape", ".ServiceResult.DataShape")
                        : declaration.serviceResultName();
        ShapeModel model =
                new ShapeModel(
                        declaration.name(),
                        declaration.projectName(),
                        declaration.description(),
                        declaration.generateServiceResult() ? resultName : "",
                        fields);
        ShapeModel previous = shapes.putIfAbsent(model.name, model);
        if (previous != null) error(type, "Duplicate DAP DataShape " + model.name);

        writeEntity(
                "DataShapes",
                declaration.name() + ".xml",
                dataShapeXml(model.name, model.project, model.description, fields));
        if (declaration.generateServiceResult()) {
            generatedResultShapes.add(resultName);
            writeEntity(
                    "DataShapes",
                    resultName + ".xml",
                    serviceResultXml(
                            resultName,
                            model.project,
                            declaration.serviceResultDescription(),
                            model.name,
                            declaration.payloadDescription()));
        }
        generateProxies(type, model);
    }

    private void generateNoPayload(TypeElement type) {
        DapNoPayloadServiceResult declaration = type.getAnnotation(DapNoPayloadServiceResult.class);
        generatedResultShapes.add(declaration.name());
        String xml =
                entityDocument(
                        "DataShapes",
                        "        "
                                + dataShape(
                                        declaration.name(),
                                        declaration.projectName(),
                                        declaration.description(),
                                        List.of(
                                                new FieldModel(
                                                        "message",
                                                        "Message",
                                                        DapBaseType.STRING,
                                                        1,
                                                        "Success or user-friendly error message",
                                                        false,
                                                        ""),
                                                new FieldModel(
                                                        "error",
                                                        "Error",
                                                        DapBaseType.BOOLEAN,
                                                        2,
                                                        "True when the service failed",
                                                        false,
                                                        ""))));
        writeEntity("DataShapes", declaration.name() + ".xml", xml);
        generateNoPayloadContract(type, declaration.name());
    }

    private void generateNoPayloadContract(TypeElement type, String resultName) {
        String packageName =
                processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String specName = type.getSimpleName().toString();
        String baseName =
                specName.endsWith("Spec") ? specName.substring(0, specName.length() - 4) : specName;
        String contractName = baseName + "Contract";
        String source =
                "package "
                        + packageName
                        + ";\n\n"
                        + "@javax.annotation.processing.Generated(\"org.us_ignite.thingworx.dap.DeclarativeAnnotationProcessor\")\n"
                        + "public final class "
                        + contractName
                        + " {\n"
                        + "  public static final org.us_ignite.thingworx.dap.runtime.DapNoPayloadServiceContract SERVICE_RESULT = new org.us_ignite.thingworx.dap.runtime.DapNoPayloadServiceContract(new org.us_ignite.thingworx.dap.runtime.DapServiceResultShape(\""
                        + javaEscape(resultName)
                        + "\", null));\n"
                        + "  private "
                        + contractName
                        + "() {}\n"
                        + "}\n";
        writeSource(packageName + "." + contractName, source, type);
    }

    private void generateThingShape(TypeElement type) {
        DapThingShape declaration = type.getAnnotation(DapThingShape.class);
        List<ServiceModel> services = services(type);
        StringBuilder body = new StringBuilder();
        body.append(
                        "        <ThingShape aspect.isEditableExtensionObject=\"false\" aspect.isExtension=\"true\"")
                .append(" className=\"")
                .append(escape(type.getQualifiedName().toString()))
                .append("\"")
                .append(" description=\"")
                .append(escape(declaration.description()))
                .append("\"")
                .append(" documentationContent=\"\" homeMashup=\"\" name=\"")
                .append(escape(declaration.name()))
                .append("\" projectName=\"")
                .append(escape(declaration.projectName()))
                .append("\" tags=\"\">\n");
        body.append("            <PropertyDefinitions>\n");
        for (DapProperty property : declaration.properties()) {
            body.append("                <PropertyDefinition");
            if (property.cacheTime() >= 0)
                body.append(" aspect.cacheTime=\"").append(property.cacheTime()).append("\"");
            if (!property.dataChangeType().isBlank())
                body.append(" aspect.dataChangeType=\"")
                        .append(escape(property.dataChangeType()))
                        .append("\"");
            body.append(" aspect.defaultValue=\"")
                    .append(escape(property.defaultValue()))
                    .append("\"")
                    .append(" aspect.isPersistent=\"")
                    .append(property.persistent())
                    .append("\"")
                    .append(" baseType=\"")
                    .append(property.baseType())
                    .append("\"")
                    .append(" category=\"")
                    .append(escape(property.category()))
                    .append("\"")
                    .append(" description=\"")
                    .append(escape(property.description()))
                    .append("\"")
                    .append(" name=\"")
                    .append(escape(property.name()))
                    .append("\"")
                    .append(" ordinal=\"")
                    .append(property.ordinal())
                    .append("\"/>\n");
        }
        body.append("            </PropertyDefinitions>\n");
        body.append("            <ServiceDefinitions>\n");
        for (ServiceModel service : services) body.append(serviceXml(service));
        body.append("            </ServiceDefinitions>\n")
                .append("            <EventDefinitions/>\n")
                .append("            <ServiceMappings/>\n")
                .append("            <ServiceImplementations/>\n")
                .append("            <Subscriptions/>\n")
                .append(commonEntitySections("            ", true));
        body.append("            <AlertConfigurations>\n");
        for (String property : declaration.alertProperties())
            body.append("                <AlertDefinitions name=\"")
                    .append(escape(property))
                    .append("\"/>\n");
        body.append("            </AlertConfigurations>\n")
                .append("            <InstanceRunTimePermissions/>\n")
                .append("        </ThingShape>");
        writeEntity(
                "ThingShapes",
                declaration.name() + ".xml",
                entityDocument("ThingShapes", body.toString()));
    }

    private void generateThingTemplate(TypeElement type) {
        DapThingTemplate declaration = type.getAnnotation(DapThingTemplate.class);
        StringBuilder body = new StringBuilder();
        body.append(
                        "        <ThingTemplate aspect.isEditableExtensionObject=\"false\" aspect.isExtension=\"true\"")
                .append(" baseThingTemplate=\"")
                .append(escape(declaration.baseThingTemplate()))
                .append("\"")
                .append(" description=\"")
                .append(escape(declaration.description()))
                .append("\"")
                .append(" documentationContent=\"\" homeMashup=\"\" name=\"")
                .append(escape(declaration.name()))
                .append("\" projectName=\"")
                .append(escape(declaration.projectName()))
                .append("\" tags=\"\">\n")
                .append(commonEntitySections("            ", true))
                .append("            <AlertConfigurations/>\n")
                .append("            <ImplementedShapes>\n");
        for (String shape : declaration.implementedShapes())
            body.append("                <ImplementedShape name=\"")
                    .append(escape(shape))
                    .append("\"/>\n");
        body.append("            </ImplementedShapes>\n")
                .append(emptyInlineThingShape("            "))
                .append("            <InstanceRunTimePermissions/>\n")
                .append("        </ThingTemplate>");
        writeEntity(
                "ThingTemplates",
                declaration.name() + ".xml",
                entityDocument("ThingTemplates", body.toString()));
    }

    private void generateThing(TypeElement type) {
        DapThing declaration = type.getAnnotation(DapThing.class);
        StringBuilder body = new StringBuilder();
        body.append(
                        "        <Thing aspect.isEditableExtensionObject=\"false\" aspect.isExtension=\"true\"")
                .append(" description=\"")
                .append(escape(declaration.description()))
                .append("\"")
                .append(" documentationContent=\"\" effectiveThingPackage=\"")
                .append(escape(declaration.effectiveThingPackage()))
                .append("\" enabled=\"")
                .append(declaration.enabled())
                .append("\" homeMashup=\"\" identifier=\"\"")
                .append(" inheritedValueStream=\"\" name=\"")
                .append(escape(declaration.name()))
                .append("\" projectName=\"")
                .append(escape(declaration.projectName()))
                .append("\" published=\"")
                .append(declaration.published())
                .append("\" tags=\"\" thingTemplate=\"")
                .append(escape(declaration.thingTemplate()))
                .append("\" valueStream=\"\">\n")
                .append(commonPermissions("            "))
                .append("            <ConfigurationTableDefinitions/>\n")
                .append("            <ConfigurationTables/>\n")
                .append("            <ThingShape>\n")
                .append("                <PropertyDefinitions/>\n")
                .append("                <ServiceDefinitions/>\n")
                .append("                <EventDefinitions/>\n")
                .append("                <Subscriptions>\n");
        for (DapSubscription subscription : declaration.subscriptions())
            body.append(subscriptionXml(subscription));
        body.append("                </Subscriptions>\n")
                .append("            </ThingShape>\n")
                .append(emptyBindings("            "))
                .append("            <AlertConfigurations/>\n")
                .append("            <ImplementedShapes>\n");
        for (String shape : declaration.implementedShapes())
            body.append("                <ImplementedShape name=\"")
                    .append(escape(shape))
                    .append("\"/>\n");
        body.append("            </ImplementedShapes>\n")
                .append("            <ThingProperties/>\n")
                .append("        </Thing>");
        writeEntity(
                "Things", declaration.name() + ".xml", entityDocument("Things", body.toString()));
    }

    private void generateProject(TypeElement type) {
        DapProject declaration = type.getAnnotation(DapProject.class);
        String body =
                "        <Project "
                        + (declaration.editable()
                                ? "aspect.isEditableExtensionObject=\"true\" "
                                : "")
                        + "dependsOn=\"\" description=\""
                        + escape(declaration.description())
                        + "\" documentationContent=\"\" homeMashup=\"\" name=\""
                        + escape(declaration.name())
                        + "\" packageVersion=\""
                        + escape(declaration.packageVersion())
                        + "\" projectName=\""
                        + escape(declaration.name())
                        + "\" tags=\"\">\n"
                        + commonPermissions("            ")
                        + "            <ConfigurationTableDefinitions/>\n"
                        + "            <ConfigurationTables/>\n"
                        + "            <ConfigurationChanges/>\n"
                        + "        </Project>";
        writeEntity("Projects", declaration.name() + ".xml", entityDocument("Projects", body));
    }

    private void generateMetadata(TypeElement type) {
        DapExtensionPackage declaration = type.getAnnotation(DapExtensionPackage.class);
        String version =
                processingEnv.getOptions().getOrDefault("dap.packageVersion", "@PACKAGE_VERSION@");
        String minimum =
                processingEnv
                        .getOptions()
                        .getOrDefault("dap.minimumThingWorxVersion", "@@MIN_THINGWORX_VERSION@@");
        StringBuilder xml =
                new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Entities>\n");
        xml.append(
                        "    <ExtensionPackages>\n        <ExtensionPackage dependsOn=\"\" description=\"")
                .append(escape(declaration.description()))
                .append("\" haCompatible=\"")
                .append(declaration.haCompatible())
                .append("\" migratorClass=\"")
                .append(escape(declaration.migratorClass()))
                .append("\" minimumThingWorxVersion=\"")
                .append(escape(minimum))
                .append("\" name=\"")
                .append(escape(declaration.name()))
                .append("\" packageVersion=\"")
                .append(escape(version))
                .append("\" vendor=\"")
                .append(escape(declaration.vendor()))
                .append("\">\n")
                .append("            <JarResources><!-- DAP:JAR_RESOURCES --></JarResources>\n")
                .append("        </ExtensionPackage>\n    </ExtensionPackages>\n")
                .append("    <ThingPackages>\n");
        for (DapThingPackage thingPackage : declaration.thingPackages()) {
            xml.append("        <ThingPackage className=\"")
                    .append(escape(thingPackage.className()))
                    .append("\" description=\"")
                    .append(escape(thingPackage.description()))
                    .append("\" name=\"")
                    .append(escape(thingPackage.name()))
                    .append("\"/>\n");
        }
        xml.append("    </ThingPackages>\n")
                .append("    <LocalizationTables>\n")
                .append(localizationXml(declaration.localizationTokens()))
                .append("    </LocalizationTables>\n")
                .append("    <Resources/>\n</Entities>\n");
        writeOutput("metadata.xml", xml.toString());
    }

    private List<FieldModel> fields(TypeElement type) {
        List<FieldModel> fields = new ArrayList<>();
        for (Element child : type.getEnclosedElements()) {
            DapField field = child.getAnnotation(DapField.class);
            if (field == null) continue;
            String javaName = child.getSimpleName().toString();
            fields.add(
                    new FieldModel(
                            javaName,
                            field.name(),
                            field.baseType(),
                            field.ordinal(),
                            field.description(),
                            field.primaryKey(),
                            field.dataShape()));
        }
        fields.sort(Comparator.comparingInt(FieldModel::ordinal));
        return fields;
    }

    private List<ServiceModel> services(TypeElement type) {
        List<ServiceModel> result = new ArrayList<>();
        for (Element child : type.getEnclosedElements()) {
            if (child.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) child;
            AnnotationMirror service = annotation(method, SDK_SERVICE);
            if (service == null) continue;
            Map<String, Object> values = values(service);
            AnnotationMirror serviceResult = annotation(method, SDK_RESULT);
            if (serviceResult == null) {
                error(method, "ThingWorx service requires @ThingworxServiceResult");
                continue;
            }
            Map<String, Object> resultValues = values(serviceResult);
            String declaredResultShape = aspect(resultValues, "dataShape");
            if (!declaredResultShape.isBlank()) referencedServiceResults.add(declaredResultShape);
            validateServicePayload(method, resultValues);
            List<ParameterModel> parameters = new ArrayList<>();
            int ordinal = 1;
            for (VariableElement parameter : method.getParameters()) {
                AnnotationMirror parameterAnnotation = annotation(parameter, SDK_PARAMETER);
                if (parameterAnnotation == null) {
                    error(
                            parameter,
                            "ThingWorx service parameter lacks @ThingworxServiceParameter");
                    continue;
                }
                Map<String, Object> parameterValues = values(parameterAnnotation);
                parameters.add(
                        new ParameterModel(
                                string(parameterValues, "name"),
                                string(parameterValues, "description"),
                                string(parameterValues, "baseType"),
                                ordinal++,
                                aspect(parameterValues, "defaultValue")));
            }
            result.add(
                    new ServiceModel(
                            string(values, "name"),
                            string(values, "description"),
                            string(values, "category"),
                            bool(values, "isAllowOverride"),
                            bool(values, "isLocalOnly"),
                            bool(values, "isOpen"),
                            bool(values, "isPrivate"),
                            aspect(values, "isAsync"),
                            string(resultValues, "name"),
                            string(resultValues, "description"),
                            string(resultValues, "baseType"),
                            aspect(resultValues, "dataShape"),
                            parameters));
        }
        return result;
    }

    private void validateServicePayload(
            ExecutableElement method, Map<String, Object> resultValues) {
        AnnotationMirror payload = annotation(method, DapServicePayload.class.getCanonicalName());
        if (payload == null) {
            error(
                    method,
                    "ThingWorx service must declare @DapServicePayload for its generated result contract");
            return;
        }
        if (!method.getReturnType().toString().equals("com.thingworx.types.InfoTable")) {
            error(method, "@DapServicePayload services must return com.thingworx.types.InfoTable");
        }
        Object rawType = values(payload).get("value");
        if (!(rawType instanceof TypeMirror typeMirror)) {
            error(method, "Unable to resolve @DapServicePayload declaration");
            return;
        }
        Element payloadElement = processingEnv.getTypeUtils().asElement(typeMirror);
        if (!(payloadElement instanceof TypeElement payloadType)) {
            error(method, "@DapServicePayload must reference a declared type");
            return;
        }
        String expected;
        DapDataShape dataShape = payloadType.getAnnotation(DapDataShape.class);
        DapNoPayloadServiceResult noPayload =
                payloadType.getAnnotation(DapNoPayloadServiceResult.class);
        if (dataShape != null) {
            expected =
                    dataShape.serviceResultName().isBlank()
                            ? dataShape.name().replace(".DataShape", ".ServiceResult.DataShape")
                            : dataShape.serviceResultName();
        } else if (noPayload != null) {
            expected = noPayload.name();
        } else {
            error(
                    method,
                    "@DapServicePayload target must declare a DAP DataShape or no-payload result");
            return;
        }
        String actual = aspect(resultValues, "dataShape");
        if (!expected.equals(actual)) {
            error(
                    method,
                    "DAP payload declares result DataShape "
                            + expected
                            + " but @ThingworxServiceResult declares "
                            + actual);
        }
    }

    private void generateProxies(TypeElement type, ShapeModel shape) {
        String packageName =
                processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
        String specName = type.getSimpleName().toString();
        String baseName =
                specName.endsWith("Spec") ? specName.substring(0, specName.length() - 4) : specName;
        String rowName = baseName + "Row";
        String tableName = baseName + "Table";
        String shapeExpression =
                "new org.us_ignite.thingworx.dap.runtime.DapShape(\""
                        + javaEscape(shape.name)
                        + "\", new org.us_ignite.thingworx.dap.runtime.DapServiceResultShape(\""
                        + javaEscape(shape.resultName)
                        + "\", \""
                        + javaEscape(shape.name)
                        + "\"))";
        StringBuilder row = new StringBuilder();
        row.append("package ")
                .append(packageName)
                .append(";\n\n")
                .append(
                        "@javax.annotation.processing.Generated(\"org.us_ignite.thingworx.dap.DeclarativeAnnotationProcessor\")\n")
                .append("public final class ")
                .append(rowName)
                .append(
                        " implements org.us_ignite.thingworx.dap.runtime.DapRowProxy<com.thingworx.types.collections.ValueCollection> {\n")
                .append(
                        "  public static final org.us_ignite.thingworx.dap.runtime.DapShape SHAPE = ")
                .append(shapeExpression)
                .append(";\n")
                .append("  private final com.thingworx.types.collections.ValueCollection row;\n")
                .append("  private ")
                .append(rowName)
                .append(
                        "(com.thingworx.types.collections.ValueCollection row) { this.row = java.util.Objects.requireNonNull(row); }\n")
                .append("  public static ")
                .append(rowName)
                .append(" create() { return new ")
                .append(rowName)
                .append("(new com.thingworx.types.collections.ValueCollection()); }\n")
                .append("  public static ")
                .append(rowName)
                .append(" wrap(com.thingworx.types.collections.ValueCollection row) { return new ")
                .append(rowName)
                .append("(row); }\n");
        for (FieldModel field : shape.fields) {
            String javaType = javaType(field.baseType);
            String cap =
                    Character.toUpperCase(field.javaName.charAt(0)) + field.javaName.substring(1);
            row.append("  public ")
                    .append(javaType)
                    .append(" ")
                    .append(field.javaName)
                    .append("() { return org.us_ignite.thingworx.dap.runtime.DapValues.get")
                    .append(capType(field.baseType))
                    .append("(row, \"")
                    .append(javaEscape(field.name))
                    .append("\"); }\n")
                    .append("  public ")
                    .append(rowName)
                    .append(" ")
                    .append(field.javaName)
                    .append("(")
                    .append(javaType)
                    .append(" value) { org.us_ignite.thingworx.dap.runtime.DapValues.put")
                    .append(capType(field.baseType))
                    .append("(row, \"")
                    .append(javaEscape(field.name))
                    .append("\", value); return this; }\n");
        }
        row.append(
                        "  @Override public org.us_ignite.thingworx.dap.runtime.DapShape shape() { return SHAPE; }\n")
                .append(
                        "  @Override public com.thingworx.types.collections.ValueCollection toValueCollection() { return row; }\n")
                .append("}\n");
        writeSource(packageName + "." + rowName, row.toString(), type);

        String serviceContract =
                shape.resultName.isBlank()
                        ? ""
                        : "  public static final org.us_ignite.thingworx.dap.runtime.DapServiceContract<"
                                + tableName
                                + "> SERVICE_RESULT = new org.us_ignite.thingworx.dap.runtime.DapServiceContract<>(SHAPE.serviceResult());\n";
        String table =
                "package "
                        + packageName
                        + ";\n\n"
                        + "@javax.annotation.processing.Generated(\"org.us_ignite.thingworx.dap.DeclarativeAnnotationProcessor\")\n"
                        + "public final class "
                        + tableName
                        + " implements org.us_ignite.thingworx.dap.runtime.DapDataShapeProxy<com.thingworx.types.InfoTable> {\n"
                        + "  public static final org.us_ignite.thingworx.dap.runtime.DapShape SHAPE = "
                        + rowName
                        + ".SHAPE;\n"
                        + serviceContract
                        + "  private final com.thingworx.types.InfoTable table;\n"
                        + "  private "
                        + tableName
                        + "(com.thingworx.types.InfoTable table) { this.table = java.util.Objects.requireNonNull(table); }\n"
                        + "  public static "
                        + tableName
                        + " create() throws Exception { return new "
                        + tableName
                        + "(com.thingworx.data.util.InfoTableInstanceFactory.createInfoTableFromDataShape(SHAPE.dataShapeName())); }\n"
                        + "  public static "
                        + tableName
                        + " wrap(com.thingworx.types.InfoTable table) { return new "
                        + tableName
                        + "(table); }\n"
                        + "  public "
                        + tableName
                        + " add("
                        + rowName
                        + " row) { table.addRow(row.toValueCollection()); return this; }\n"
                        + "  public "
                        + rowName
                        + " row(int index) { return "
                        + rowName
                        + ".wrap(table.getRow(index)); }\n"
                        + "  public int size() { return table.getRowCount(); }\n"
                        + "  @Override public org.us_ignite.thingworx.dap.runtime.DapShape shape() { return SHAPE; }\n"
                        + "  @Override public com.thingworx.types.InfoTable toInfoTable() { return table; }\n"
                        + "}\n";
        writeSource(packageName + "." + tableName, table, type);
    }

    private void validateFields(TypeElement type, String name, List<FieldModel> fields) {
        if (fields.isEmpty())
            error(type, "DAP DataShape " + name + " must declare at least one @DapField");
        Set<String> names = new HashSet<>();
        Set<Integer> ordinals = new HashSet<>();
        for (FieldModel field : fields) {
            if (!names.add(field.name))
                error(type, "Duplicate field " + field.name + " in " + name);
            if (!ordinals.add(field.ordinal))
                error(type, "Duplicate ordinal " + field.ordinal + " in " + name);
            if (field.baseType == DapBaseType.INFOTABLE && field.dataShape.isBlank())
                error(type, "INFOTABLE field " + field.name + " requires dataShape");
        }
    }

    private void validateReferencedShapes() {
        for (ShapeModel shape : shapes.values()) {
            for (FieldModel field : shape.fields) {
                if (field.baseType == DapBaseType.INFOTABLE
                        && !field.dataShape.isBlank()
                        && !shapes.containsKey(field.dataShape)) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "DAP DataShape "
                                            + shape.name
                                            + " references undeclared "
                                            + field.dataShape);
                }
            }
        }
        for (String resultShape : referencedServiceResults) {
            if (!generatedResultShapes.contains(resultShape)) {
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "ThingWorx service references a result DataShape not generated by DAP: "
                                        + resultShape);
            }
        }
    }

    private String dataShapeXml(
            String name, String project, String description, List<FieldModel> fields) {
        return entityDocument(
                "DataShapes", "        " + dataShape(name, project, description, fields));
    }

    private String dataShape(
            String name, String project, String description, List<FieldModel> fields) {
        StringBuilder xml = new StringBuilder();
        xml.append(
                        "<DataShape aspect.isEditableExtensionObject=\"true\" aspect.isExtension=\"true\"")
                .append(" baseDataShape=\"\" description=\"")
                .append(escape(description))
                .append("\" documentationContent=\"\" homeMashup=\"\" name=\"")
                .append(escape(name))
                .append("\" projectName=\"")
                .append(escape(project))
                .append("\" tags=\"\">\n")
                .append(commonPermissions("            "))
                .append("            <ConfigurationTableDefinitions/>\n")
                .append("            <ConfigurationTables/>\n")
                .append("            <FieldDefinitions>\n");
        for (FieldModel field : fields) {
            xml.append("                <FieldDefinition");
            if (!field.dataShape.isBlank())
                xml.append(" aspect.dataShape=\"").append(escape(field.dataShape)).append("\"");
            if (field.primaryKey) xml.append(" aspect.isPrimaryKey=\"true\"");
            xml.append(" baseType=\"")
                    .append(field.baseType)
                    .append("\"")
                    .append(" description=\"")
                    .append(escape(field.description))
                    .append("\"")
                    .append(" name=\"")
                    .append(escape(field.name))
                    .append("\"")
                    .append(" ordinal=\"")
                    .append(field.ordinal)
                    .append("\"/>\n");
        }
        xml.append("            </FieldDefinitions>\n        </DataShape>");
        return xml.toString();
    }

    private String serviceResultXml(
            String name,
            String project,
            String description,
            String payloadShape,
            String payloadDescription) {
        List<FieldModel> fields =
                List.of(
                        new FieldModel(
                                "result",
                                "Result",
                                DapBaseType.INFOTABLE,
                                1,
                                payloadDescription,
                                false,
                                payloadShape),
                        new FieldModel(
                                "message",
                                "Message",
                                DapBaseType.STRING,
                                2,
                                "Status message",
                                false,
                                ""),
                        new FieldModel(
                                "error",
                                "Error",
                                DapBaseType.BOOLEAN,
                                3,
                                "True when the service failed",
                                false,
                                ""));
        return dataShapeXml(name, project, description, fields);
    }

    private String serviceXml(ServiceModel service) {
        StringBuilder xml = new StringBuilder();
        xml.append("                <ServiceDefinition aspect.isAsync=\"")
                .append(service.isAsync.isBlank() ? "false" : escape(service.isAsync))
                .append("\"")
                .append(" category=\"")
                .append(escape(service.category))
                .append("\"")
                .append(" description=\"")
                .append(escape(service.description))
                .append("\"")
                .append(" isAllowOverride=\"")
                .append(service.allowOverride)
                .append("\"")
                .append(" isLocalOnly=\"")
                .append(service.localOnly)
                .append("\"")
                .append(" isOpen=\"")
                .append(service.open)
                .append("\"")
                .append(" isPrivate=\"")
                .append(service.privateService)
                .append("\"")
                .append(" name=\"")
                .append(escape(service.name))
                .append("\">\n")
                .append("                    <ResultType");
        if (!service.resultDataShape.isBlank())
            xml.append(" aspect.dataShape=\"").append(escape(service.resultDataShape)).append("\"");
        xml.append(" baseType=\"")
                .append(escape(service.resultBaseType))
                .append("\"")
                .append(" description=\"")
                .append(escape(service.resultDescription))
                .append("\"")
                .append(" name=\"")
                .append(escape(service.resultName))
                .append("\" ordinal=\"0\"/>\n")
                .append("                    <ParameterDefinitions>\n");
        for (ParameterModel parameter : service.parameters) {
            xml.append("                        <FieldDefinition");
            if (!parameter.defaultValue.isBlank())
                xml.append(" aspect.defaultValue=\"")
                        .append(escape(parameter.defaultValue))
                        .append("\"");
            xml.append(" baseType=\"")
                    .append(escape(parameter.baseType))
                    .append("\"")
                    .append(" description=\"")
                    .append(escape(parameter.description))
                    .append("\"")
                    .append(" name=\"")
                    .append(escape(parameter.name))
                    .append("\"")
                    .append(" ordinal=\"")
                    .append(parameter.ordinal)
                    .append("\"/>\n");
        }
        xml.append("                    </ParameterDefinitions>\n")
                .append("                </ServiceDefinition>\n");
        return xml.toString();
    }

    private String subscriptionXml(DapSubscription subscription) {
        return "                    <Subscription description=\"\" enabled=\""
                + subscription.enabled()
                + "\" eventName=\""
                + escape(subscription.eventName())
                + "\" name=\""
                + escape(subscription.name())
                + "\" source=\""
                + escape(subscription.source())
                + "\" sourceProperty=\""
                + escape(subscription.sourceProperty())
                + "\" sourceType=\"Thing\">\n"
                + "                        <ServiceImplementation description=\"\" handlerName=\"Script\" name=\""
                + escape(subscription.name())
                + "\">\n"
                + "                            <ConfigurationTables>\n"
                + "                                <ConfigurationTable description=\"\" isMultiRow=\"false\" name=\"Script\" ordinal=\"0\">\n"
                + "                                    <DataShape><FieldDefinitions><FieldDefinition baseType=\"STRING\" description=\"code\" name=\"code\" ordinal=\"0\"/></FieldDefinitions></DataShape>\n"
                + "                                    <Rows><Row><code><![CDATA["
                + cdata(subscription.script())
                + "]]></code></Row></Rows>\n"
                + "                                </ConfigurationTable>\n"
                + "                            </ConfigurationTables>\n"
                + "                        </ServiceImplementation>\n"
                + "                    </Subscription>\n";
    }

    private String localizationXml(DapLocalizationToken[] tokens) {
        StringBuilder xml = new StringBuilder();
        xml.append(
                        "        <LocalizationTable description=\"Default localization table\" documentationContent=\"\" homeMashup=\"\"")
                .append(
                        " languageCommon=\"English\" languageNative=\"English\" name=\"Default\" projectName=\"\" tags=\"\">\n")
                .append(commonPermissions("            "))
                .append("            <ConfigurationTables>\n")
                .append(
                        "                <ConfigurationTable description=\"Localization tokens and usage\" isMultiRow=\"true\" name=\"LocalizationTokens\" ordinal=\"0\">\n")
                .append("                    <DataShape><FieldDefinitions>\n")
                .append(
                        "                        <FieldDefinition aspect.friendlyName=\"Translation context\" baseType=\"STRING\" description=\"Translation context\" name=\"context\" ordinal=\"3\"/>\n")
                .append(
                        "                        <FieldDefinition aspect.friendlyName=\"Token name\" baseType=\"STRING\" description=\"Token name\" name=\"name\" ordinal=\"0\"/>\n")
                .append(
                        "                        <FieldDefinition aspect.friendlyName=\"Token usage\" baseType=\"STRING\" description=\"Token usage\" name=\"usage\" ordinal=\"2\"/>\n")
                .append(
                        "                        <FieldDefinition aspect.friendlyName=\"Localized value\" baseType=\"STRING\" description=\"Localized value\" name=\"value\" ordinal=\"1\"/>\n")
                .append(
                        "                    </FieldDefinitions></DataShape>\n                    <Rows>\n");
        for (DapLocalizationToken token : tokens) {
            xml.append("                        <Row><context><![CDATA[")
                    .append(cdata(token.context()))
                    .append("]]></context><name><![CDATA[")
                    .append(cdata(token.name()))
                    .append("]]></name><usage><![CDATA[")
                    .append(cdata(token.usage()))
                    .append("]]></usage><value><![CDATA[")
                    .append(cdata(token.value()))
                    .append("]]></value></Row>\n");
        }
        xml.append("                    </Rows>\n                </ConfigurationTable>\n")
                .append("            </ConfigurationTables>\n        </LocalizationTable>\n");
        return xml.toString();
    }

    private String commonEntitySections(String indent, boolean bindings) {
        String value =
                commonPermissions(indent)
                        + indent
                        + "<ConfigurationTableDefinitions/>\n"
                        + indent
                        + "<ConfigurationTables/>\n";
        return bindings ? value + emptyBindings(indent) : value;
    }

    private String commonPermissions(String indent) {
        return indent
                + "<avatar/>\n"
                + indent
                + "<DesignTimePermissions><Create/><Read/><Update/><Delete/><Metadata/></DesignTimePermissions>\n"
                + indent
                + "<RunTimePermissions/>\n"
                + indent
                + "<VisibilityPermissions><Visibility/></VisibilityPermissions>\n";
    }

    private String emptyBindings(String indent) {
        return indent
                + "<PropertyBindings/>\n"
                + indent
                + "<RemotePropertyBindings/>\n"
                + indent
                + "<RemoteServiceBindings/>\n"
                + indent
                + "<RemoteEventBindings/>\n";
    }

    private String emptyInlineThingShape(String indent) {
        return indent
                + "<ThingShape>\n"
                + indent
                + "    <PropertyDefinitions/>\n"
                + indent
                + "    <ServiceDefinitions/>\n"
                + indent
                + "    <EventDefinitions/>\n"
                + indent
                + "    <ServiceMappings/>\n"
                + indent
                + "    <ServiceImplementations/>\n"
                + indent
                + "    <Subscriptions/>\n"
                + indent
                + "</ThingShape>\n";
    }

    private String entityDocument(String collection, String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Entities majorVersion=\"9\" minorVersion=\"3\" universal=\"password\">\n"
                + "    <"
                + collection
                + ">\n"
                + body
                + "\n"
                + "    </"
                + collection
                + ">\n"
                + "</Entities>\n";
    }

    private AnnotationMirror annotation(Element element, String qualifiedName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName()
                    .contentEquals(qualifiedName)) return mirror;
        }
        return null;
    }

    private Map<String, Object> values(AnnotationMirror mirror) {
        Map<String, Object> result = new HashMap<>();
        processingEnv
                .getElementUtils()
                .getElementValuesWithDefaults(mirror)
                .forEach(
                        (key, value) ->
                                result.put(key.getSimpleName().toString(), value.getValue()));
        return result;
    }

    private String aspect(Map<String, Object> values, String key) {
        Object raw = values.get("aspects");
        if (!(raw instanceof List<?> list)) return "";
        String prefix = key + ":";
        for (Object item : list) {
            Object value =
                    item instanceof AnnotationValue annotationValue
                            ? annotationValue.getValue()
                            : item;
            String text = String.valueOf(value);
            if (text.startsWith(prefix)) return text.substring(prefix.length());
        }
        return "";
    }

    private String string(Map<String, Object> values, String key) {
        return String.valueOf(values.getOrDefault(key, ""));
    }

    private boolean bool(Map<String, Object> values, String key) {
        return Boolean.parseBoolean(String.valueOf(values.getOrDefault(key, false)));
    }

    private void writeEntity(String entityType, String filename, String content) {
        writeOutput(entityType + "/" + filename, content);
    }

    private void writeOutput(String relative, String content) {
        if (!emitted.add(relative)) return;
        String configured = processingEnv.getOptions().get("dap.output");
        try {
            if (configured != null && !configured.isBlank()) {
                Path target = Path.of(configured).resolve(relative);
                Files.createDirectories(target.getParent());
                Files.writeString(target, content, StandardCharsets.UTF_8);
            } else {
                try (Writer writer =
                        processingEnv
                                .getFiler()
                                .createResource(
                                        StandardLocation.CLASS_OUTPUT, "", "dap/" + relative)
                                .openWriter()) {
                    writer.write(content);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate " + relative, exception);
        }
    }

    private void writeSource(String qualifiedName, String source, Element originating) {
        if (!emitted.add("source:" + qualifiedName)) return;
        try {
            JavaFileObject file =
                    processingEnv.getFiler().createSourceFile(qualifiedName, originating);
            try (Writer writer = file.openWriter()) {
                writer.write(source);
            }
        } catch (FilerException duplicate) {
            // Another round already created the same deterministic proxy.
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate " + qualifiedName, exception);
        }
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private String javaType(DapBaseType type) {
        return switch (type) {
            case BOOLEAN -> "java.lang.Boolean";
            case DATETIME -> "org.joda.time.DateTime";
            case INFOTABLE -> "com.thingworx.types.InfoTable";
            case INTEGER -> "java.lang.Integer";
            case PASSWORD, STRING, THINGNAME -> "java.lang.String";
        };
    }

    private String capType(DapBaseType type) {
        return switch (type) {
            case PASSWORD -> "Password";
            case STRING, THINGNAME -> "String";
            case BOOLEAN -> "Boolean";
            case DATETIME -> "Datetime";
            case INFOTABLE -> "InfoTable";
            case INTEGER -> "Integer";
        };
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String javaEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String cdata(String value) {
        return value.replace("]]>", "]]]]><![CDATA[>");
    }

    private record FieldModel(
            String javaName,
            String name,
            DapBaseType baseType,
            int ordinal,
            String description,
            boolean primaryKey,
            String dataShape) {}

    private record ShapeModel(
            String name,
            String project,
            String description,
            String resultName,
            List<FieldModel> fields) {}

    private record ParameterModel(
            String name, String description, String baseType, int ordinal, String defaultValue) {}

    private record ServiceModel(
            String name,
            String description,
            String category,
            boolean allowOverride,
            boolean localOnly,
            boolean open,
            boolean privateService,
            String isAsync,
            String resultName,
            String resultDescription,
            String resultBaseType,
            String resultDataShape,
            List<ParameterModel> parameters) {}
}
