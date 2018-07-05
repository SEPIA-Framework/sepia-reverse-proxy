package net.b07z.sepia.proxies;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class TestSimpleReverseProxy {

	@Test
	public void test() throws Exception {
		
		//Start test-server
		Undertow s1 = startTestServer(9213, "Test1");
		Undertow s2 = startTestServer(9214, "Test2");
		
		//Create tiny reverse proxy
		TinyReverseProxy reverseProxy = new TinyReverseProxy("localhost", 9212);
		
		//Add paths
		reverseProxy.addPrefixPath("/one", "http://localhost:9213");
		reverseProxy.addPrefixPath("/two", "http://localhost:9214");
		reverseProxy.addPrefixPath("/sepia/three", "http://localhost:9213");
		reverseProxy.addPrefixPath("/sepia/four", "http://localhost:9214");
		
		//Start proxy
		reverseProxy.start();
		
		//Test-calls and close server
		String res = httpGET("http://localhost:9213");			assertTrue(res.equals("Test1 - path: /"));
		res = httpGET("http://localhost:9214");					assertTrue(res.equals("Test2 - path: /"));
		res = httpGET("http://localhost:9212");					assertTrue(res.startsWith("SEPIA"));
		res = httpGET("http://localhost:9212/one/1");			assertTrue(res.equals("Test1 - path: /1"));
		res = httpGET("http://localhost:9212/two/2");			assertTrue(res.equals("Test2 - path: /2"));
		res = httpGET("http://localhost:9212/sepia/three/3");	assertTrue(res.equals("Test1 - path: /3"));
		res = httpGET("http://localhost:9212/sepia/four/4");	assertTrue(res.equals("Test2 - path: /4"));
		
		reverseProxy.stop();
		s1.stop();
		s2.stop();
		
		assertTrue(true);
	}
	
	/**
	 * Start a test server at port with custom message response in plain text.
	 */
	private Undertow startTestServer(int port, String msg){
		Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler((exchange) -> {
                	exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send(makeTestMessage(exchange, msg));
                }).build();
        server.start();
        return server;
	}
	/**
	 * Make a test message for test server
	 */
	private String makeTestMessage(HttpServerExchange ex, String msg){
		String fullMsg = msg + " - path: " + ex.getRequestPath();
		//System.out.println(fullMsg);
		return fullMsg;
	}
	
	/**
	 * Make a HTTP GET call. 
	 */
	private static String httpGET(String url) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setConnectTimeout(3000);
		con.setReadTimeout(3000);

		int responseCode = con.getResponseCode();
		//System.out.println("GET Response Code : " + responseCode);		//debug

		//success?
		if (responseCode >= 200 && responseCode < 300){
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
 			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			//result
			String result = response.toString();
			//System.out.println(response.toString());						//debug
			return result;
		
		}else{
			return String.valueOf(responseCode);
		}
	}

}
