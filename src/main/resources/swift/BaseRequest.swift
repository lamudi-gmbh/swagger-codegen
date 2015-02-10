//
//  Request.swift
//  Lamudi
//
//  Copyright (c) 2014 Lamudi Gmbh. All rights reserved.
//

import Foundation

class BaseRequest: NSOperation, NSURLConnectionDataDelegate {
    
    // helper variables
    private var _executing = false
    private var _finished = false
    private var _cancelled = false
        
    var failureHander: ((Error)->())?
    
    var connection: NSURLConnection?
    var receivedData: NSMutableData?
    var httpResponse: NSHTTPURLResponse?
    var port: NSPort?
    let method:String
    var finalPath:String = ""

    override var executing: Bool {
        get {
            return _executing
        }
        set(newValue) {
            if _executing != newValue {
                willChangeValueForKey("isExecuting")
                _executing = newValue
                didChangeValueForKey("isExecuting")
            }
        }
    }
    
    override var finished: Bool {
        get {
            return _finished
        }
        set(newValue) {
            if _finished != newValue {
                willChangeValueForKey("isFinished")
                _finished = finished
                didChangeValueForKey("isFinished")
            }
        }
    }
    
    override var cancelled: Bool {
        get {
            return _cancelled
        }
        set(newValue) {
            if _cancelled != newValue {
                _cancelled = newValue
            }
        }
    }
    
    override var asynchronous: Bool {
        return true
    }
    
    init(failure: ((Error) -> ())? = nil, method:String) {
        self.failureHander = failure
        self.method = method
        super.init()
    }
    
    
    func addToQueue() -> BaseRequest {
        APIHandler.api.addRequestToQueue(self)
        
        return self
    }
    
    // to finish the operation you need to call this methos
    // calling self.finished and self.executing one after another
    // does not work
    func finishOperation() {
        
        if let oPort = port {
            let runloop = NSRunLoop.currentRunLoop()
            runloop.removePort(oPort, forMode: NSDefaultRunLoopMode)
        }
        
        willChangeValueForKey("isFinished")
        willChangeValueForKey("isExecuting")
        _executing = false
        _finished = true
        didChangeValueForKey("isExecuting")
        didChangeValueForKey("isFinished")
        
    }
    
    func shouldAddToQueue(queue: NSOperationQueue) -> Bool {
        return true
    }
    
    func proccessResult(responseDictionary: [String:AnyObject]) -> (Bool, String?) {
        // As default do nothing
        return (true, nil)
    }
    
    func path() -> String {
        networkLogger?.error("This property has to be overridden by sub class. Terminating")
        fatalError("This property has to be overridden by sub class. Terminating")
    }

    func requestBody() -> String? {
        return nil
    }

    // MARK: main
    
    override func main() {
        if self.cancelled || self.finished {
            return
        }
    
        self.executing = true
        
        let urlString = self.path()
        self.finalPath = urlString

        networkLogger?.info("Sending request to \(urlString) with params")
        
        if let url = NSURL(string: urlString) {
            
            let request = NSMutableURLRequest(URL: url, cachePolicy: NSURLRequestCachePolicy.UseProtocolCachePolicy, timeoutInterval: 30)
            if let postString = self.requestBody() {
                request.HTTPBody = postString.dataUsingEncoding(NSUTF8StringEncoding)
            }
            self.connection = NSURLConnection(request: request, delegate: self, startImmediately: false)
            port = NSPort()
            let runloop = NSRunLoop.currentRunLoop()
            runloop.addPort(port!, forMode: NSDefaultRunLoopMode)
            connection?.scheduleInRunLoop(runloop, forMode: NSDefaultRunLoopMode)
            connection?.start()
            runloop.run()
            
        } else {
            // invalid url
            networkLogger?.error("Tried to send request to invalid url: \(urlString)")
        }
    }
    
    override func start() {
        if self.cancelled {
            self.finished = true
            return
        }
        self.executing = true
        self.main()
    }
    
    override func cancel() {
        if let conn = connection {
            conn.cancel()
        }
        
        self.cancelled = true
        
        self.finishOperation()
        networkLogger?.info("Request to \(self.finalPath) has been cancelled")
    }
    
    func runFail(error: Error) {
        networkLogger?.info("Error on response handling: \(error.errorDescription)")
        ErrorHandler.handler.handle(error)
        if let failHandler = self.failureHander {
            dispatch_async(dispatch_get_main_queue(), { () -> Void in
                failHandler(error)
            })
        }
        
        self.finishOperation()
    }
    
    // NSURLConnectionDelegate
    
    func connection(connection: NSURLConnection, didFailWithError error: NSError) {
        receivedData = nil
        networkLogger?.error("Connection failed to \(self.finalPath)")
        self.runFail(ApplicationError(code: .ConnectionFail, errorDescription: "Connection failed"))
    }
    
    func connection(connection: NSURLConnection, didReceiveResponse response: NSURLResponse) {
        receivedData = NSMutableData()
        httpResponse = response as? NSHTTPURLResponse
    }
    
    func connection(connection: NSURLConnection, didReceiveData data: NSData) {
        receivedData?.appendData(data)
    }
    
    func connectionDidFinishLoading(connection: NSURLConnection) {
        if self.cancelled {
            return
        }
        
        var url = self.path()
        
        if httpResponse == nil {
            // no response
            networkLogger?.error("No http response from \(url). Terminating request. Warning, this serious problem please investigate")
            return
        }
        
        networkLogger?.info("Response received from \(url):")
        
        if let responseData = receivedData {
            let responseString: String? = NSString(data: responseData, encoding: NSUTF8StringEncoding)
            
            // check content type
                if let contentType = httpResponse!.allHeaderFields["Content-Type"] as? String {
                    if contentType != "application/json" {
                        // response is not json
                        networkLogger?.error("content type of response is not json. Content: ")
                        networkLogger?.error(responseString)
                        self.runFail(ServiceError(code: .WrongContentType, errorDescription: "Response is not json", receivedUrl: url, response: responseString))
                        return
                    }
            }
            
            if self.cancelled {
                return
            }
            networkLogger?.verbose(NSString(data: responseData, encoding: NSUTF8StringEncoding))
            
            var parseError: NSError?
            var result: AnyObject? = NSJSONSerialization.JSONObjectWithData(responseData, options: NSJSONReadingOptions.allZeros, error: &parseError)
            
            if parseError != nil {
                // Parsing has failed
                
                networkLogger?.error(responseString)
                self.runFail(ServiceError(code: .ParseFail, errorDescription: "Error while parsing json", receivedUrl: url, response: responseString))
                return
            }
            if self.cancelled {
                return
            }
            if let resultDictionary = result as? [String:AnyObject] {
                if httpResponse!.statusCode != 200 {
                    // We should get application error
                    let errorModel = ErrorModel(data: resultDictionary)
                    
                    if errorModel.sanitizeAndValidate().0 {
                        self.runFail(ApplicationError(errorModel: errorModel))
                    } else {
                        // Mapping to error object did fail
                        self.runFail(ServiceError(code: .ErrorMappingFail, errorDescription: "Mapping to error object did fail", receivedUrl: url, response: responseString))
                    }
                    return
                }
                if self.cancelled {
                    return
                }
                let (processSuccess, processFailureMessage) = self.proccessResult(resultDictionary)
                if processSuccess {
                    self.finishOperation()
                    return
                } else {
                    // Mapping to expected object did fail
                    self.runFail(ServiceError(code: .ExpectedObjectMappingFail, errorDescription: processFailureMessage!, receivedUrl: url, response: responseString))
                }
            } else {
                // response is not dictionary
                self.runFail(ServiceError(code: .ResponseIsNotADictionary, errorDescription: "Response is not a dictionary object", receivedUrl: url, response: responseString))
            }
        } else {
            // no response data
            self.runFail(ServiceError(code: .NoResponseData, errorDescription: "No response data received", receivedUrl: url, response: nil))
        }
    }

}