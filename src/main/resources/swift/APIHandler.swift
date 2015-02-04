//
//  APIHandler.swift
//  Lamudi
//
//  Created by Mert Senyurt on 10/11/14.
//  Copyright (c) 2014 Rocket Internet Gmbh. All rights reserved.
//

import Foundation

private let singletonInstance = APIHandler()

@objc class APIHandler {
    
    class var api: APIHandler {
        return singletonInstance
    }
    
    private let operationQueue = NSOperationQueue()
    
    init() {
        operationQueue.maxConcurrentOperationCount = NSOperationQueueDefaultMaxConcurrentOperationCount
    }
    
    func addRequestToQueue(request: BaseRequest) {
        if (request.shouldAddToQueue(self.operationQueue)) {
            self.operationQueue.addOperation(request)
        }

    }
}