package com.wordnik.swagger.codegen.languages;

import com.wordnik.swagger.models.*;
import com.wordnik.swagger.codegen.*;
import com.wordnik.swagger.models.properties.*;

import java.util.*;
import java.io.File;

public class LamudiJavaClientCodegen extends DefaultCodegen implements CodegenConfig {
    protected String requestPackage;
    protected String responsePackage;
    protected String servicePackage;
    protected String groupId = "io.swagger";
    protected String artifactId = "swagger-client";
    protected String artifactVersion = "1.0.0";
    protected String sourceFolder = "src/main/java";

    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    public String getName() {
        return "lamudijava";
    }

    public String getHelp() {
        return "Generates a Java client library.";
    }

    public LamudiJavaClientCodegen() {
        super();
        outputFolder = "generated-code/java";
        modelTemplateFiles.put("model.mustache", ".java");
        apiTemplateFiles.put("api.mustache", ".java");

        templateDir = "Java";
        apiPackage = "com.lamudi.networking.api";
        modelPackage = apiPackage + ".model";
        requestPackage = apiPackage + ".request";
        responsePackage = apiPackage + ".response";
        servicePackage = apiPackage + ".service";

        reservedWords = new HashSet<String>(
                Arrays.asList(
                        "abstract", "continue", "for", "new", "switch", "assert",
                        "default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
                        "this", "break", "double", "implements", "protected", "throw", "byte", "else",
                        "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
                        "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
                        "native", "super", "while")
        );

        additionalProperties.put("groupId", groupId);
        additionalProperties.put("artifactId", artifactId);
        additionalProperties.put("artifactVersion", artifactVersion);

        String apiSourceFolder = (sourceFolder + File.separator + apiPackage).replace(".", java.io.File.separator);
        String modelSourceFolder = (sourceFolder + File.separator + modelPackage).replace(".", java.io.File.separator);
        String serviceSourceFolder = (sourceFolder + File.separator + servicePackage).replace(".", java.io.File.separator);

        supportingFiles.add(new SupportingFile("APIConstants.java", apiSourceFolder, "APIConstants.java"));
        supportingFiles.add(new SupportingFile("OkHttpClientHelper.java", apiSourceFolder, "OkHttpClientHelper.java"));
        supportingFiles.add(new SupportingFile("ConversionException.java", apiSourceFolder, "ConversionException.java"));
        supportingFiles.add(new SupportingFile("JacksonConverterFactory.java", apiSourceFolder, "JacksonConverterFactory.java"));
        supportingFiles.add(new SupportingFile("JacksonRequestBodyConverter.java", apiSourceFolder, "JacksonRequestBodyConverter.java"));
        supportingFiles.add(new SupportingFile("JacksonResponseBodyConverter.java", apiSourceFolder, "JacksonResponseBodyConverter.java"));
        supportingFiles.add(new SupportingFile("PersistentDataManager.java", apiSourceFolder, "PersistentDataManager.java"));
        supportingFiles.add(new SupportingFile("RequestManager.java", apiSourceFolder, "RequestManager.java"));
        supportingFiles.add(new SupportingFile("ResponseCallback.java", apiSourceFolder, "ResponseCallback.java"));
        supportingFiles.add(new SupportingFile("RestError.java", apiSourceFolder, "RestError.java"));
        supportingFiles.add(new SupportingFile("BaseRequest.java", apiSourceFolder, "BaseRequest.java"));
        supportingFiles.add(new SupportingFile("OkHttpClientHelper.java", apiSourceFolder, "OkHttpClientHelper.java"));
        supportingFiles.add(new SupportingFile("BaseDTO.java", modelSourceFolder, "BaseDTO.java"));
        supportingFiles.add(new SupportingFile("BaseResponse.java", modelSourceFolder, "BaseResponse.java"));
        supportingFiles.add(new SupportingFile("SavedSearchesResponseTemp.java", modelSourceFolder, "SavedSearchesResponseTemp.java"));
        supportingFiles.add(new SupportingFile("WorkaroundBaseResponse.java", modelSourceFolder, "WorkaroundBaseResponse.java"));
        supportingFiles.add(new SupportingFile("WorkaroundMessagesDTO.java", modelSourceFolder, "WorkaroundMessagesDTO.java"));
        supportingFiles.add(new SupportingFile("WorkAroundUserService.java", serviceSourceFolder, "WorkAroundUserService.java"));

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "String",
                        "boolean",
                        "Boolean",
                        "Double",
                        "Integer",
                        "Long",
                        "Float",
                        "Object")
        );

        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("map", "HashMap");
    }

    @Override
    public String toVarName(String name) {
        if (reservedWords.contains(name))
            return "m" + escapeSpecialChars(toCamelCase(escapeReservedWord(name)));
        else
            return "m" + escapeSpecialChars(toCamelCase(name));
    }

    public String escapeSpecialChars(String name) {
        return name.replaceAll("[^a-zA-Z0-9]+", "");
    }

    public String toCamelCase(String s) {
        String[] parts = s.split("_");
        if (parts.length == 1) {
            parts = s.split(" ");
        }
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + initialCaps(part);
        }
        return camelCaseString;
    }

    public String initialLower(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public String addIs(String s) {
        if (s.substring(0, 2).equals("is")) {
            return s;
        }
        return "is" + s;
    }

    @Override
    public CodegenProperty fromProperty(String name, Property p) {
        CodegenProperty property = super.fromProperty(name, p);
        property.getter = "get" + escapeSpecialChars(toCamelCase(name));
        property.setter = "set" + escapeSpecialChars(toCamelCase(name));
        return property;
    }

    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + servicePackage.replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();

            return getSwaggerType(p) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public String toModelFilename(String name) {
        return isResponse(name) ? name : name + "DTO";
    }

    @Override
    public String toModelName(String name) {
        if (typeMapping.keySet().contains(name) || importMapping.keySet().contains(name)
                || defaultIncludes.contains(name) || languageSpecificPrimitives.contains(name)
                || isResponse(name)) {
            return initialCaps(name);
        } else {
            return initialCaps(name) + "DTO";
        }
    }

    @Override
    public CodegenModel fromModel(String name, Model model) {
        CodegenModel m = super.fromModel(name, model);
        if (isResponse(m.classname)) {
            m.parent = "BaseResponse";
        } else {
            m.parent = "BaseDTO";
        }

        if (m.imports.contains("List")) {
            m.imports.add("ArrayList");
        }

        if (m.imports.contains("Map")) {
            m.imports.add("HashMap");
        }
        return m;
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation operation : ops) {
                if (operation.hasConsumes == Boolean.TRUE) {
                    Map<String, String> firstType = operation.consumes.get(0);
                    if (firstType != null) {
                        if ("multipart/form-data".equals(firstType.get("mediaType"))) {
                            operation.isMultipart = Boolean.TRUE;
                        }
                    }
                }
                if (operation.returnType == null) {
                    operation.returnType = "Void";
                }
            }
        }
        return objs;
    }

    public boolean isResponse(String name) {
        return name.toLowerCase().contains("response");
    }

    public String toPrimitiveTypeName(String name) {
        return initialCaps(name);
    }

    @Override
    public String toApiName(String name) {
        return initialCaps(name) + "Service";
    }

    public String toApiFilename(String name) {
        return initialCaps(name) + "Service";
    }

    @Override
    public String toParamName(String name) {
        if (reservedWords.contains(name)) {
            return escapeSpecialChars(escapeReservedWord(name));
        }
        return escapeSpecialChars(name);
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation);
        op.requestName = initialCaps(op.operationId);
        if (operation.getVendorExtensions() != null && operation.getVendorExtensions().size() > 0) {
            for (Map.Entry<String, Object> pair : operation.getVendorExtensions().entrySet()) {
                String key = pair.getKey();
                String value = pair.getValue().toString();
                if (key.equalsIgnoreCase("x-operationType")) {
                    op.operationType = value;
                } else if (key.equalsIgnoreCase("x-needsLogin")) {
                    op.needsLogin = Boolean.parseBoolean(value);
                } else if (key.equalsIgnoreCase("x-baseUrl")) {
                    op.baseUrl = value;
                } else if (key.equalsIgnoreCase("x-isAngi")) {
                    op.isAngi = Boolean.parseBoolean(value);
                } else if (key.equalsIgnoreCase("x-bundle")) {
                    op.bundle = value;
                }
            }
        }
        return op;
    }
}
