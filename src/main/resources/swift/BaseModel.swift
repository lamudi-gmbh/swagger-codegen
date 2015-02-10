//
//  BaseModel.swift
//  Lamudi
//
//  Copyright (c) 2014 Lamudi Gmbh. All rights reserved.
//

import Foundation

class BaseModel: NSObject, Printable {
        
    func dictionaryRepresentation() -> Dictionary<String,AnyObject> {
        
        var selfClass = self.dynamicType
        var dictionary = self.toDictionary(selfClass)
        var superClass: (AnyClass!) = class_getSuperclass(selfClass);
        if superClass !== NSObject.classForCoder() {
            let superDictionary = self.toDictionary(superClass)
            for (key, value) in superDictionary {
                dictionary[key] = value
            }
        }
        return dictionary
    }

    /*
     * Sub classes, which has to be validated against optional and
     * required properties has to override this method.
     *
     */
    
    func sanitizeAndValidate() -> (Bool, String?) {
        // if object do not need validation
        // just return true
        return (true, nil)
    }

    private func toDictionary(clazz: AnyClass) -> Dictionary<String,AnyObject> {
            var propertiesCount : UInt32 = 0
            let propertiesInAClass : UnsafeMutablePointer<objc_property_t> = class_copyPropertyList(clazz, &propertiesCount)
            var propertiesDictionary = Dictionary<String,AnyObject>()
            
            for var i = 0; i < Int(propertiesCount); i++ {
                var property = propertiesInAClass[i]
                var propName = NSString(CString: property_getName(property), encoding: NSUTF8StringEncoding)
                var propType = property_getAttributes(property)
                var propValue : AnyObject? = self.valueForKey(propName!);
                
                if let val: AnyObject = propValue {
                    propertiesDictionary[propName!] = valueToObject(val)
                }
                
            }
            
            free(propertiesInAClass)
            
            return propertiesDictionary
    }
    
    private func valueToObject(value: AnyObject) -> AnyObject {
        switch value {
        case let val as BaseModel:
            return val.dictionaryRepresentation()
        case let val as Array<AnyObject>:
            
            var array = Array<AnyObject>()
            
            for aval in val {
                array.append(valueToObject(aval))
            }
            
            return array
        case let val as Dictionary<String, AnyObject>:
            
            var dictionary = Dictionary<String, AnyObject>()
            for (key, aval) in val {
                dictionary[key] = valueToObject(aval)
            }
            
            return dictionary
        default:
            return value
        }
    }
    
}