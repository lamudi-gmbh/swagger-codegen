//
//  ErrorHandler.swift
//  Lamudi
//
//  Created by Mert Senyurt on 01/12/14.
//  Copyright (c) 2014 Rocket Internet Gmbh. All rights reserved.
//

import Foundation

private let singletonInstance = ErrorHandler()

class ErrorHandler {
    
    class var handler: ErrorHandler {
        return singletonInstance
    }
    
    func handle(error: Error) {
        // TODO: do something with error
        
        switch error {
        case let appError as ApplicationError:
            errorHandlingLogger?.info("App error has been received")
        case let serviceError as ServiceError:
            errorHandlingLogger?.info("service error has been received")
        default:
            errorHandlingLogger?.error("Unknown type of error has been received")
        }
    }
}