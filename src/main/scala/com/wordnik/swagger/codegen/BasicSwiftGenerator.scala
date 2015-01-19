/**
 *  Copyright 2014 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wordnik.swagger.codegen

import com.wordnik.swagger.codegen.model._

object BasicSwiftGenerator extends BasicObjcGenerator {
  def main(args: Array[String]) = generateClient(args)
}

class BasicSwiftGenerator extends BasicGenerator {
  override def defaultIncludes = Set(
    "Boolean",
    "Int",
    "String",
    "Array")

  override def reservedWords = Set("class", "break", "as", "associativity", "deinit", "case", "dynamicType", "convenience", "enum", "continue", "false", "dynamic", "extension", "default", "is", "didSet", "func", "do", "nil", "final", "import", "else", "self", "get", "init", "fallthrough", "Self", "infix", "internal", "for", "super", "inout", "let", "if", "true", "lazy", "operator", "in", "COLUMN", "left", "private", "return", "FILE", "mutating", "protocol", "switch", "FUNCTION", "none", "public", "where", "LINE", "nonmutating", "static", "while", "optional", "struct", "override", "subscript", "postfix", "typealias", "precedence", "var", "prefix", "Protocol", "required", "right", "set", "Type", "unowned", "weak")

  override def typeMapping = Map(
    "enum" -> "String",
    "date" -> "Date",
    "Date" -> "Date",
    "boolean" -> "Boolean",
    "string" -> "String",
    "integer" -> "Int",
    "int" -> "Int",
    "float" -> "Float",
    "long" -> "Float",
    "double" -> "Float",
    "Array" -> "Array",
    "array" -> "Array",
    "List" -> "Array",
    "object" -> "NSObject")

  override def toModelFilename(name: String) = name

  // naming for the models
  override def toModelName(name: String) = {
    (typeMapping.keys ++ 
      importMapping.values ++ 
      defaultIncludes ++ 
      languageSpecificPrimitives
    ).toSet.contains(name) match {
      case true => name(0).toUpper + name.substring(1)
      case _ => {
        "SWG" + name(0).toUpper + name.substring(1)
      }
    }
  }

  // objective c doesn't like variables starting with "new"
  override def toVarName(name: String): String = {
    val paramName = name.replaceAll("[^a-zA-Z0-9_]","")
    paramName
  }

  // naming for the apis
  override def toApiName(name: String) = name(0).toUpper + name.substring(1) + "Api"

  // location of templates
  override def templateDir = "swift"

  // template used for models
  modelTemplateFiles += "model.mustache" -> ".swift"

  // template used for apis
  apiTemplateFiles += "api.mustache" -> ".swift"

  // package for models
  override def invokerPackage: Option[String] = None

  // package for models
  override def modelPackage: Option[String] = None

  // package for api classes
  override def apiPackage: Option[String] = None

  // response classes
  override def processResponseClass(responseClass: String): Option[String] = {
    typeMapping.contains(responseClass) match {
      case true => Some(typeMapping(responseClass))
      case false => {
        responseClass match {
          case "void" => None
          case e: String => {
            if(responseClass.toLowerCase.startsWith("array") || responseClass.toLowerCase.startsWith("list"))
              Some("Array")
            else
              Some(toModelName(responseClass))
          }
        }
      }
    }
  }

  override def processApiMap(m: Map[String, AnyRef]): Map[String, AnyRef] = {
    val mutable = scala.collection.mutable.Map() ++ m
    mutable += "newline" -> "\n"

    mutable.map(k => {
      k._1 match {
        case e: String if (e == "allParams") => {
          val sp = (mutable(e)).asInstanceOf[List[_]]
          sp.size match {
            case i: Int if(i > 0) => mutable += "hasParams" -> "true"
            case _ =>
          }
        }
        case _ =>
      }
    })
    mutable.toMap
  }

  override def processResponseDeclaration(responseClass: String): Option[String] = {
    processResponseClass(responseClass) match {
      case Some("void") => Some("void")
      case Some(e) => Some(e)
      case _ => Some(responseClass)
    }
  }

  override def toDeclaredType(dt: String): String = {
    val declaredType = dt.indexOf("[") match {
      case -1 => dt
      case n: Int => "Array"
    }
    val t = typeMapping.getOrElse(declaredType, declaredType)

    (languageSpecificPrimitives.contains(t)) match {
      case true => toModelName(t)
      case _ => toModelName(t) // needs pointer
    }
  }

  override def toDeclaration(obj: ModelProperty) = {
    var declaredType = toDeclaredType(obj.`type`)
    declaredType.toLowerCase match {
      case "list" => {
        declaredType = "array"
      }
      case e: String => e
    }

    val defaultValue = toDefaultValue(declaredType, obj)
    declaredType match {
      case "array" => {
        val inner = {
          obj.items match {
            case Some(items) => {
              if(items.ref != null) 
                items.ref
              else
                items.`type`
            }
            case _ => {
              println("failed on " + obj)
              throw new Exception("no inner type defined")
            }
          }
        }
        "Array"
      }
      case "set" => {
        val inner = {
          obj.items match {
            case Some(items) => items.ref.getOrElse(items.`type`)
            case _ => {
              println("failed on " + obj)
              throw new Exception("no inner type defined")
            }
          }
        }
        "Array"
      }
      case _ =>
    }
    (declaredType, defaultValue)
  }

  override def escapeReservedWord(word: String) = "_" + word

  override def toDefaultValue(properCase: String, obj: ModelProperty) = {
    properCase match {
      case "boolean" => "false"
      case "int" => "0"
      case "long" => "0"
      case "float" => "0.0"
      case "double" => "0.0"
      case "List" => {
        val inner = {
          obj.items match {
            case Some(items) => {
              if(items.ref != null) 
                items.ref
              else
                items.`type`
            }
            case _ => {
              println("failed on " + properCase + ", " + obj)
              throw new Exception("no inner type defined")
            }
          }
        }
        "[] as Array[" + inner + "]"
      }
      case _ => "nil"
    }
  }
}
