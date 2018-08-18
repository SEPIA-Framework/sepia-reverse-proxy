package net.b07z.sepia.proxies;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.Undertow.Builder;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;
import net.b07z.sepia.proxies.handlers.PathHandlerWithIpFilter;

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
	int httpSupportPort = 0;
	boolean ssl = false;
	boolean sslSupportHttp = false;
	SSLContext sslContext = null;
	
	Undertow reverseProxy;
	int state = 0; 		//0: pre-built, 1: started, 2: stopped
	
	Map<String, LoadBalancingProxyClient> prefixMappings;
	Map<String, Boolean> pathIsPublicMappings;
	Map<String, LoadBalancingProxyClient> exactMappings;
	
	/**
	 * Create server running at port.
	 * @param host - usually "localhost" or "0.0.0.0" for all IPs
	 * @param port - port to run on
	 */
	public TinyReverseProxy(String host, int port){
		this.host = host;
		this.port = port;
		prefixMappings = new HashMap<>();
		pathIsPublicMappings = new HashMap<>();
		exactMappings = new HashMap<>();
	}
	/**
	 * Create server running at 'port' with SSL support.
	 * @param host - usually "localhost" or "0.0.0.0" for all IPs
	 * @param port - port to run on
	 * @param useSSL - use given SSL context?
	 * @param sslContext - SSL context to use
	 */
	public TinyReverseProxy(String host, int port, boolean useSSL, SSLContext sslContext){
		this(host, port);
		this.ssl = useSSL;
		this.sslContext = sslContext;
	}
	
	/**
	 * Expose endpoints over HTTP as well when using SSL? (default is false).
	 * Http requires an extra port, usually (https-port + 1)
	 * @param useHttpWithSsl
	 * @param httpPort
	 */
	public void setSslHttpSupport(boolean useHttpWithSsl, int httpPort){
		this.sslSupportHttp = useHttpWithSsl;
		this.httpSupportPort = httpPort;
	}
	
	/**
	 * Start server. Add some proxy-paths first!
	 */
	public void start(){
		Builder proxyBuilder = Undertow.builder()
				.setServerOption(UndertowOptions.ENABLE_HTTP2, true) 		//TODO: do we want to support this?
                .setIoThreads(IO_THREADS);
		
		//Use SSL?
		if (ssl){
			proxyBuilder.addHttpsListener(this.port, this.host, sslContext);
			if (sslSupportHttp && httpSupportPort > 0){
				proxyBuilder.addHttpListener(this.httpSupportPort, this.host);
			}
		}else{
			proxyBuilder.addHttpListener(this.port, this.host);
		}
		
		//PathHandler pathHandler = Handlers.path();
		PathHandlerWithIpFilter pathHandler = new PathHandlerWithIpFilter(pathIsPublicMappings);
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
	 * @param isPublic - will be available only from local IP addresses or every? Note: this only works if there is no other redirect in front!!!
	 * @throws Exception
	 */
	public void addPrefixPath(String path, String target, boolean isPublic) throws Exception{
		if (state != 0){
			throw new RuntimeException("Not possible while reverse-proxy is running!");
		}else{
			LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
	                .addHost(new URI(target))
	                .setConnectionsPerThread(LB_CONNECTIONS_PER_THREAD);
			prefixMappings.put(path, loadBalancer);
			pathIsPublicMappings.put(path, isPublic);
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
			pathIsPublicMappings.remove(path);
		}
	}
	
	//TODO: add and test exact path mappings
}
