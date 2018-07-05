package net.b07z.sepia.proxies;

/**
 * Command-line interface to start a proxy.
 * 
 * @author Florian Quirin
 *
 */
public class Start {

	/**
	 * Run a proxy.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String proxy = "";
		
		//Proxy to run:
		if (args[0].equals("tiny")){
			proxy = "tiny";
			
			//default
			int port = 20726;
			String host = "localhost";
			
			for (String arg : args){
				//Port
				if (arg.startsWith("-port=")){
					port = Integer.parseInt(arg.replaceFirst(".*?=", "").trim());
				
				//Host
				}else if (arg.startsWith("-host=")){
					host = arg.replaceFirst(".*?=", "").trim();
				
				//Paths
				}else if (arg.startsWith("-defaultPaths=")){
					String paths = arg.replaceFirst(".*?=", "").trim();
					if (!paths.equals("true")){
						System.out.println("Sorry any other than the default paths are not yet supported via command-line interface!");
						return;
					}
					//TODO: add a way to define custom prefix-path combinations (best: load from config and give config-file here as value)
				}
			}
			
			//Create tiny reverse proxy
			TinyReverseProxy reverseProxy = new TinyReverseProxy(host, port);
			
			//Add paths - SEPIA defaults for custom-bundle:
			reverseProxy.addPrefixPath("/sepia/assist", "http://localhost:20721");
			reverseProxy.addPrefixPath("/sepia/teach", "http://localhost:20722");
			reverseProxy.addPrefixPath("/sepia/chat", "http://localhost:20723");
			
			//Start proxy
			reverseProxy.start();
			
			//Note
			System.out.println("SEPIA '" + proxy + "' reverse proxy started at: " + host + ":" + port);
			
			return;
		
		//Help
		}else{
			help();
			return;
		}
	}
	
	/**
	 * Command-line interface help.
	 */
	private static void help(){
		System.out.println("\nUsage:");
		System.out.println("[proxy-name] [arguments]");
		System.out.println("\nProxies:");
		System.out.println("tiny - args: -defaultPaths=true, -port=20726, -host=localhost");
		System.out.println("");
	}

}
