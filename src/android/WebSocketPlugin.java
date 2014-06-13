package mobi.monaca.framework.plugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.http.conn.util.InetAddressUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketPlugin extends CordovaPlugin{
	WebSocketServer server;
	int port;
	CallbackContext callbackContext;
	HashMap<String, WebSocket> sockets = new HashMap<String, WebSocket>();
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		
		if(action.equalsIgnoreCase("start")){
			if(server == null){
				JSONObject params = args.getJSONObject(0);
				this.port = params.getInt("port");
				createServer(port);
				server.start();
			}
			JSONObject result;
			try {
				result = createAddressJSON();
				result.put("event", "server:started");
				PluginResult pluginResult = new PluginResult(Status.OK, result);
				pluginResult.setKeepCallback(true);
				callbackContext.sendPluginResult(pluginResult);
				this.callbackContext = callbackContext;
			} catch (SocketException e) {
				callbackContext.error(e.getMessage());
				e.printStackTrace();
			}
			
			return true;
		}
		
		if(action.equalsIgnoreCase("getStatus")){
			if(server == null){
				JSONObject statusJSON = new JSONObject();
				statusJSON.put("status", "stopped");
				callbackContext.success(statusJSON);
			}else{
				JSONObject statusJSON;
				try {
					statusJSON = createAddressJSON();
					statusJSON.put("status", "started");
					JSONArray clients = new JSONArray(sockets.keySet());
					statusJSON.put("clients", clients);
					callbackContext.success(statusJSON);
				} catch (SocketException e) {
					callbackContext.error(e.getMessage());
					e.printStackTrace();
				}
				
			}
			return true;
		}
		
		if(action.equalsIgnoreCase("getAddress")){
			if(server == null){
				callbackContext.error("You need to start server first");
			}else{
				JSONObject result;
				try {
					result = createAddressJSON();
					callbackContext.success(result);
				} catch (SocketException e) {
					callbackContext.error(e.getMessage());
					e.printStackTrace();
				}
				
			}
			return true;
		}
		
		if(action.equalsIgnoreCase("getClients")){
			if(server == null){
				callbackContext.error("You need to start server first");
			}else{
				JSONArray message = new JSONArray(sockets.keySet());
				callbackContext.success(message);
			}
			return true;
		}
		
		if(action.equalsIgnoreCase("send")){
			if(server == null){
				callbackContext.error("You need to start server before sending a message");
			}else{
				JSONObject params = args.getJSONObject(0);
				String clientId = params.getString("clientId");
				String message = params.getString("message");
				if(sockets.containsKey(clientId)){
					WebSocket webSocket = sockets.get(clientId);
					webSocket.send(message);
					callbackContext.success();
				}else{
					callbackContext.error("client " + clientId + " is not yet connected");
				}
			}
			return true;
		}
		
		if(action.equalsIgnoreCase("sendToAllClients")){
			if(server == null){
				callbackContext.error("You need to start server before sending a message");
			}else{
				JSONObject params = args.getJSONObject(0);
				String message = params.getString("message");
				Set<String> clients = sockets.keySet();
				WebSocket webSocket;
				for (String client : clients) {
					webSocket = sockets.get(client);
					webSocket.send(message);
				}				
				JSONObject resultJSON = new JSONObject();
				resultJSON.put("send", "success");
				resultJSON.put("clientsSent", sockets.size());
				callbackContext.success(resultJSON);
			}
			return true;
		}
		
		if(action.equalsIgnoreCase("stop")){
			if(server != null){
				try {
					stopServer();
					callbackContext.success();
				} catch (Exception e) {
					e.printStackTrace();
					callbackContext.error(e.getMessage());
				}
			}else{
				callbackContext.error("server not yet started");
			}
			
			return true;
		}
		return super.execute(action, args, callbackContext);
	}

	private void stopServer() throws IOException, InterruptedException {
		server.stop();
		sockets.clear();
		server = null;
	}

	private JSONObject createAddressJSON() throws JSONException, SocketException {
		JSONObject result = new JSONObject();
		result.put("networks", getIPAddresses());
		result.put("port", port);
		return result;
	}
	
	@Override
	public void onDestroy() {
		if(server != null){
			try {
				stopServer();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		super.onDestroy();
	}
	
	private void createServer(int port){
		server = new WebSocketServer(new InetSocketAddress(port)){

			@Override
			public void onClose(WebSocket webSocket, int arg1, String msg,
					boolean arg3) {
				String clientId = getClientId(webSocket);
				sockets.remove(clientId);
				try {
					JSONObject message = createJSONMessage("disconnected", clientId);
					message.put("message", msg);
					PluginResult pluginResult = new PluginResult(Status.OK, message);
					pluginResult.setKeepCallback(true);
					WebSocketPlugin.this.callbackContext.sendPluginResult(pluginResult);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onError(WebSocket webSocket, Exception msg) {
				String clientId = getClientId(webSocket);
				try {
					JSONObject message = createJSONMessage("error", clientId);
					message.put("message", msg.toString());
					PluginResult pluginResult = new PluginResult(Status.OK, message);
					pluginResult.setKeepCallback(true);
					WebSocketPlugin.this.callbackContext.sendPluginResult(pluginResult);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, String msg) {
				String clientId = getClientId(webSocket);
				try {
					JSONObject message = createJSONMessage("message", clientId);
					message.put("message", msg);
					PluginResult pluginResult = new PluginResult(Status.OK, message);
					pluginResult.setKeepCallback(true);
					WebSocketPlugin.this.callbackContext.sendPluginResult(pluginResult);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				String clientId = getClientId(webSocket);
				sockets.put(clientId, webSocket);
				
				try {
					JSONObject message = createJSONMessage("connected", clientId);
					PluginResult pluginResult = new PluginResult(Status.OK, message);
					pluginResult.setKeepCallback(true);
					WebSocketPlugin.this.callbackContext.sendPluginResult(pluginResult);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			private String getClientId(WebSocket webSocket) {
				if(webSocket != null){
					String clientId = webSocket.getRemoteSocketAddress().toString();
					return clientId;
				}
				return null;
			}
		};
	}
	
	private JSONObject createJSONMessage(String event, String clientId)
			throws JSONException {
			JSONObject message = new JSONObject();
			message.put("event", event);
			message.put("client", clientId);
			return message;
	}
	
	private String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								return delim < 0 ? sAddr : sAddr.substring(0,
										delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		} // for now eat exceptions
		return "";
	}

	private JSONObject getIPAddresses() throws SocketException, JSONException {
		JSONObject networkJson= new JSONObject();
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface
				.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
			for (InetAddress addr : addrs) {
				if (!addr.isLoopbackAddress()) {
					String ipAddress = addr.getHostAddress().toUpperCase();
					boolean isIPv4 = InetAddressUtils.isIPv4Address(ipAddress);
					if (isIPv4) {						
						String interfacename = intf.getName();
						networkJson.put(interfacename, ipAddress);
					}
				}
			}
		}
		return networkJson;
	}	
}