package net.b07z.sepia.proxies.handlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.StatusCodes;

public class PathHandlerWithIpFilter extends PathHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(PathHandlerWithIpFilter.class);
	
	private final boolean DEFAULT_ALLOW = true;
	private boolean debugCalls = true;
	
	private Map<String, Boolean> pathPublicAllowed;
	
	/**
	 * Create a new path handler with a map of paths that are (or are not) allowed publicly.<br>
	 * NOTE: The check for local IP addresses only works if there is no other proxy in front !!!
	 * @param pathPublicAllowed
	 */
	public PathHandlerWithIpFilter(Map<String, Boolean> pathPublicAllowed){
		this.pathPublicAllowed = pathPublicAllowed;
	}
	
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
    	InetSocketAddress peer = exchange.getSourceAddress();
    	String path = exchange.getRelativePath();
    	boolean isAllowed = isAllowed(peer.getAddress(), path);
    	if (debugCalls){
    		logger.debug("Peer: " + peer.getAddress().toString() + " - path: " + path + " - isAllowed: " + isAllowed);
    	}
    	
    	if (!isAllowed){
            exchange.setStatusCode(StatusCodes.FORBIDDEN);
            exchange.endExchange();
            return;
        }
    	super.handleRequest(exchange);
    }
    
    //Check if the path is restricted and if so check if address is local address 
    private boolean isAllowed(InetAddress address, String path) {
    	boolean isPublic = pathPublicAllowed.getOrDefault(path, DEFAULT_ALLOW);
    	if (!isPublic){
    		return  (address.isSiteLocalAddress() || 
    	             address.isAnyLocalAddress()  || 
    	             address.isLinkLocalAddress() || 
    	             address.isLoopbackAddress()  || 
    	             address.isMulticastAddress()); 		//TODO: is that secure enough?
    	}else{
    		return true;
    	}
    }

}
