package net.b07z.sepia.proxies;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;

/**
 * Very basic, tiny, reverse proxy based on Undertow.
 * 
 * @author Florian Quirin
 *
 */
public class TinyReverseProxy {
	
	int IO_THREADS = 3;
	int LB_CONNECTIONS_PER_THREAD = 3;
	int MAX_REQ_TIME = 30000;
	
	String host = "localhost";
	int port = 20726;
	
	Undertow reverseProxy;
	int state = 0; 		//0: pre-built, 1: started, 2: stopped
	
	Map<String, LoadBalancingProxyClient> prefixMappings;
	Map<String, LoadBalancingProxyClient> exactMappings;
	
	/**
	 * Create server running at port.
	 * @param host - usually "localhost"
	 * @param port - port to run on
	 */
	public TinyReverseProxy(String host, int port){
		this.host = host;
		this.port = port;
		prefixMappings = new HashMap<>();
		exactMappings = new HashMap<>();
	}
	
	/**
	 * Start server. Add some proxy-paths first!
	 */
	public void start(){
		Builder proxyBuilder = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setIoThreads(IO_THREADS);
		
		//PathHandler pathHandler = Handlers.path();
		PathHandlerWithIpFilter pathHandler = new PathHandlerWithIpFilter();
		pathHandler.addExactPath("/", (exchange) -> {
        	exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("SEPIA reverse-proxy powered by Undertow");
        });
		
		//Exact-paths
		for (String path : exactMappings.keySet()){
			// REST API path
			pathHandler.addExactPath(path,	
				ProxyHandler.builder()
					.setProxyClient(prefixMappings.get(path))
					.setMaxRequestTime(MAX_REQ_TIME)
					.build()
			);
		}
		//Prefix-paths
		for (String path : prefixMappings.keySet()){
			// REST API path
			pathHandler.addPrefixPath(path,	
				ProxyHandler.builder()
					.setProxyClient(prefixMappings.get(path))
					.setMaxRequestTime(MAX_REQ_TIME)
					.build()
			);
		}
		proxyBuilder.setHandler(pathHandler);
                
		reverseProxy = proxyBuilder.build();
        reverseProxy.start();
        state = 1;
	}
	
	/**
	 * Stop server.
	 */
	public void stop(){
		reverseProxy.stop();
		state = 2;
	}

	/**
	 * Add a reverse proxy path and target.
	 * @param path - path of server to forward, e.g. "/sepia"
	 * @param target - target server, e.g. "http://localhost:20721"
	 * @throws Exception
	 */
	public void addPrefixPath(String path, String target) throws Exception{
		if (state != 0){
			throw new RuntimeException("Not possible while reverse-proxy is running!");
		}else{
			LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
	                .addHost(new URI(target))
	                .setConnectionsPerThread(LB_CONNECTIONS_PER_THREAD);
			prefixMappings.put(path, loadBalancer);
		}
	}
	
	/**
	 * Remove previously set path.
	 * @param path - path of server to forward, e.g. "/sepia"
	 */
	public void removePrefixPath(String path){
		if (state != 0){
			throw new RuntimeException("Not possible while reverse-proxy is running!");
		}else{
			prefixMappings.remove(path);
		}
	}
	
	//TODO: add and test exact path mappings
}
