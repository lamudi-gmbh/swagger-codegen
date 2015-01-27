//
//  ErrorModel.swift
//  Lamudi
//
//  Created by Mert Senyurt on 27/11/14.
//  Copyright (c) 2014 Rocket Internet Gmbh. All rights reserved.
//

import Foundation

class ErrorModel: BaseModel, Deserializable {
    
    var errorDescription: String?
    var errorCode: Int?
    
    required init(data: [String : AnyObject]) {
        if let errorDictionary = data["error"] as? [String : AnyObject] {
            
            errorDescription <<< errorDictionary["description"]
            errorCode <<< errorDictionary["code"]
        }
    }
    
    override func isValidObject() -> (Bool, String?) {
        
        var failedProp: String?
        if errorDescription == nil {
            failedProp = "description"
        }
        
        if errorCode == nil {
            failedProp = "code"
        }
        
        if let prop = failedProp {
            return (false, "Error object without \(prop) is not valid. Mapping failed.")
        }
        
        return (true, nil)
    }
}