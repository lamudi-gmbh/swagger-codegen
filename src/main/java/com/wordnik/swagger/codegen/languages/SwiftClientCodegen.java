package com.wordnik.swagger.codegen.languages;

import com.wordnik.swagger.util.Json;
import com.wordnik.swagger.codegen.*;
import com.wordnik.swagger.models.properties.*;

import java.util.*;
import java.io.File;

public class SwiftClientCodegen extends DefaultCodegen implements CodegenConfig {
  protected Set<String> foundationClasses = new HashSet<String>();
  protected String sourceFolder = "client";
  protected static String PREFIX = "SWG";

  public String getName() {
    return "objc";
  }

  public String getHelp() {
    return "Generates an Objective-C client library.";
  }

  public SwiftClientCodegen() {
    super();
    outputFolder = "generated-code/objc";
    modelTemplateFiles.put("model.mustache", ".swift");
    apiTemplateFiles.put("api.mustache", ".swift");
    templateDir = "objc";
    modelPackage = "";

    defaultIncludes = new HashSet<String>(
      Arrays.asList("Boolean", "Int", "String", "Array"));

    reservedWords = new HashSet<String>(
      Arrays.asList("class", "break", "as", "associativity", "deinit", "case", "dynamicType", "convenience", "enum", "continue", 
        "false", "dynamic", "extension", "default", "is", "didSet", "func", "do", "nil", "final", "import", "else", "self", 
        "get", "init", "fallthrough", "Self", "infix", "internal", "for", "super", "inout", "let", "if", "true", "lazy", "operator", 
        "in", "COLUMN", "left", "private", "return", "FILE", "mutating", "protocol", "switch", "FUNCTION", "none", "public", "where", 
        "LINE", "nonmutating", "static", "while", "optional", "struct", "override", "subscript", "postfix", "typealias", 
        "precedence", "var", "prefix", "Protocol", "required", "right", "set", "Type", "unowned", "weak"));

    typeMapping = new HashMap<String, String>();
    typeMapping.put("enum", "String");
    typeMapping.put("date", "Date");
    typeMapping.put("Date", "Date");
    typeMapping.put("boolean", "Boolean");
    typeMapping.put("string", "String");
    typeMapping.put("integer", "Int");
    typeMapping.put("int", "Int");
    typeMapping.put("float", "Float");
    typeMapping.put("long", "Float");
    typeMapping.put("double", "Float");
    typeMapping.put("Array", "Array");
    typeMapping.put("array", "Array");
    typeMapping.put("List", "Array");
    typeMapping.put("object", "NSObject");

    instantiationTypes.put("array", "Array");
    instantiationTypes.put("map", "Dictionary");

    supportingFiles.add(new SupportingFile("BaseModel.swift", sourceFolder, "BaseModel.swift"));
    supportingFiles.add(new SupportingFile("BaseRequest.swift", sourceFolder, "BaseRequest.swift"));
    supportingFiles.add(new SupportingFile("JSONHelper.swift", sourceFolder, "JSONHelper.swift"));
  }

  @Override
  public String toInstantiationType(Property p) {
    if (p instanceof MapProperty) {
      MapProperty ap = (MapProperty) p;
      String inner = getSwaggerType(ap.getAdditionalProperties());
      return instantiationTypes.get("map");
    }
    else if (p instanceof ArrayProperty) {
      ArrayProperty ap = (ArrayProperty) p;
      String inner = getSwaggerType(ap.getItems());
      return instantiationTypes.get("array");
    }
    else
      return null;
  }

  @Override
  public String getSwaggerType(Property p) {
    String swaggerType = super.getSwaggerType(p);
    String type = null;
    if(typeMapping.containsKey(swaggerType)) {
      type = typeMapping.get(swaggerType);
      if(languageSpecificPrimitives.contains(type) && !foundationClasses.contains(type))
        return toModelName(type);
    }
    else
      type = swaggerType;
    return toModelName(type);
  }

  @Override
  public String toModelName(String type) {
    if(typeMapping.keySet().contains(type) ||
      foundationClasses.contains(type) ||
      importMapping.values().contains(type) ||
      defaultIncludes.contains(type) ||
      languageSpecificPrimitives.contains(type)) {
      return Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }
    else {
      return PREFIX + Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }
  }

  @Override
  public String toModelImport(String name) {
    if("".equals(modelPackage()))
      return name;
    else
      return modelPackage() + "." + name;
  }

  @Override
  public String toDefaultValue(Property p) {
    return null;
  }

  @Override
  public String apiFileFolder() {
    return outputFolder + File.separator + sourceFolder;
  }

  @Override
  public String modelFileFolder() {
    return outputFolder + File.separator + sourceFolder;
  }

  @Override
  public String toModelFilename(String name) {
    return PREFIX + initialCaps(name);
  }

  @Override
  public String toApiName(String name) {
    return PREFIX + initialCaps(name) + "Api";
  }

  public String toApiFilename(String name) {
    return PREFIX + initialCaps(name) + "Api";
  }

  @Override
  public String toVarName(String name) {
    String paramName = name.replaceAll("[^a-zA-Z0-9_]","");
    return paramName;
  }

  public String escapeReservedWord(String name) {
    return "_" + name;
  }
}