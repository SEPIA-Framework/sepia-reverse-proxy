package net.b07z.sepia.proxies;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.StatusCodes;

public class PathHandlerWithIpFilter extends PathHandler {
	
	private final boolean DEFAULT_ALLOW = true;
	
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
    	InetSocketAddress peer = exchange.getSourceAddress();
    	String path = exchange.getRelativePath();
    	System.out.println(peer.getAddress().toString() + " - " + path);
        if (!isAllowed(peer.getAddress())) {
            exchange.setStatusCode(StatusCodes.FORBIDDEN);
            exchange.endExchange();
            return;
        }
    	super.handleRequest(exchange);
    }
    
    boolean isAllowed(InetAddress address) {
        if(address instanceof Inet4Address) {
        	/*
            for (PeerMatch rule : ipv4acl) {
                if (rule.matches(address)) {
                    return !rule.isDeny();
                }
            }
            */
        } else if(address instanceof Inet6Address) {
            /*
        	for (PeerMatch rule : ipv6acl) {
                if (rule.matches(address)) {
                    return !rule.isDeny();
                }
            }
            */
        }
        return DEFAULT_ALLOW;
    }

}
