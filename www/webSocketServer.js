

    var exec = require('cordova/exec');

    var WebSocketServer = function(port)
    {
        this.port = port || 3001;
    }

    WebSocketServer.prototype.startServerSuccessCallback = function(response)
    {
        console.log('server start callback ' + JSON.stringify(response));
        
        if(response.event === "connected"){
            var clientId = response.client;
            if(this.onClientConnected){
                this.onClientConnected(response);
            }
        }
        
        if(response.event === "disconnected"){
            var clientId = response.client;
            if(this.onClientDisconnected){
                this.onClientDisconnected(response);
            }
        }
        
        if(response.event === 'message'){
            if(this.onmessage){
                this.onmessage(response);
            }
        }
    }
    
    WebSocketServer.prototype.startWebSocketServer = function(onSuccess, onError)
    {
        exec(
            function(response){
                if(response.event === 'server:started'){
                    onSuccess(response);
                }else{
                    self.startServerSuccessCallback.call(self, response);
                }
            }, onError, "WebSocketServer", "start", [{port: this.port}]
        );
    }

    WebSocketServer.prototype.stopWebSocketServer = function(onSuccess, onError)
    {
        exec(onSuccess, onError, "WebSocketServer", "stop", []);
    }
    
    WebSocketServer.prototype.send = function(onSuccess, onError, client, message)
    {
        exec(onSuccess, onError, "WebSocketServer", "send", [{"clientId": client, "message": message}]);
    }
    
    WebSocketServer.prototype.sendToAllClients = function(onSuccess, onError, message)
    {
        exec(onSuccess, onError, "WebSocketServer", "sendToAllClients", [{"message": message}]);
    }
    
    WebSocketServer.prototype.getClients = function(onSuccess, onError)
    {
        exec(onSuccess, onError, "WebSocketServer", "getClients", []);
    }
    
    WebSocketServer.prototype.getAddress = function(onSuccess, onError)
    {
        exec(onSuccess, onError, "WebSocketServer", "getAddress", []);
    }
    
    WebSocketServer.prototype.getStatus = function(onSuccess, onError)
    {
        exec(onSuccess, onError, "WebSocketServer", "getStatus", []);
    }
    
    module.exports = new WebSocketServer();

