package net.b07z.sepia.proxies;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Command-line interface to start a proxy.
 * 
 * @author Florian Quirin
 *
 */
public class Start {

	//defaults
	private static String host = "localhost";
	private static int port = 20726;
	
	//Command-line parameters have priority
	private static boolean ignoreSettingsHost = false;
	private static boolean ignoreSettingsPort = false;
	
	/**
	 * Run a proxy.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String proxy = "";
		
		//Check if arguments are given
		if (args == null || args.length == 0){
			System.out.println("Missing proxy-name to run, e.g. 'tiny'.");
			help();
			return;
		}
		
		//Proxy to run:
		if (args[0].equals("tiny")){
			proxy = "tiny";
			
			for (String arg : args){
				//Port
				if (arg.startsWith("-port=")){
					port = Integer.parseInt(arg.replaceFirst(".*?=", "").trim());
					ignoreSettingsPort = true;
				
				//Host
				}else if (arg.startsWith("-host=")){
					host = arg.replaceFirst(".*?=", "").trim();
					ignoreSettingsHost = true;
				
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
			
			//Read settings
			List<ProxyAction> actions = null;
			try{
				actions = loadSettings("settings/proxy.properties");
			}catch(Exception e){
				System.out.println("Could not read 'settings/proxy.properties' file! Error: " + e.getMessage());
				return;
			}
			
			//Create tiny reverse proxy
			TinyReverseProxy reverseProxy = new TinyReverseProxy(host, port);

			//Add actions
			for (ProxyAction pa : actions){
				if (pa.actionType.equals("redirect")){
					reverseProxy.addPrefixPath(pa.redirectPath, pa.redirectTarget);
				}
			}
			/*	
			reverseProxy.addPrefixPath("/sepia/assist", "http://localhost:20721");
			reverseProxy.addPrefixPath("/sepia/teach", "http://localhost:20722");
			reverseProxy.addPrefixPath("/sepia/chat", "http://localhost:20723");
			*/
			
			//Start proxy
			reverseProxy.start();
			
			//Note
			System.out.println("\nSEPIA '" + proxy + "' reverse proxy started as: " + host + ":" + port);
			
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
	
	/**
	 * Class holding proxy actions loaded from settings.
	 */
	private static class ProxyAction {
		String redirectPath;
		String redirectTarget;
		boolean targetIsPublic = false;
		String actionType = "";
		
		public ProxyAction(){}
		
		public ProxyAction setRedirect(String path, String target, boolean isPublic){
			this.redirectPath = path;
			this.redirectTarget = target;
			this.targetIsPublic = isPublic;
			this.actionType = "redirect";
			return this;
		}
	}
	
	/**
	 * Load settings from properties file and return actions list.
	 * @param configFile - path and file
	 * @throws IOException 
	 */
	private static List<ProxyAction> loadSettings(String configFile) throws IOException {
		BufferedInputStream stream=null;
		Properties config = new Properties();
		stream = new BufferedInputStream(new FileInputStream(configFile));
		config.load(stream);
		stream.close();
		List<ProxyAction> actions = new ArrayList<>();
		for (Object key : config.keySet()){
			String entry = (String) key;
			//has to be format: action_type_name, e.g. redirect_path_1
			//has to have types: path, target, public
			if (entry.startsWith("redirect")){
				String[] info = entry.split("_");
				if (info.length != 3){
					throw new RuntimeException("Settings file has invalid format in entry: " + entry);
				}else{
					String name = info[2];
					String path = config.getProperty("redirect_path_" + name);
					String target = config.getProperty("redirect_target_" + name);
					boolean isPublic = Boolean.getBoolean(config.getProperty("redirect_public_" + name));
					actions.add(new ProxyAction().setRedirect(path, target, isPublic));
				}
			
			}else if (entry.equals("host") && !ignoreSettingsHost){
				host = config.getProperty(entry);
			}else if (entry.equals("port") && !ignoreSettingsPort){
				port = Integer.parseInt(config.getProperty(entry));
			}
		}
		return actions;
	}

}
