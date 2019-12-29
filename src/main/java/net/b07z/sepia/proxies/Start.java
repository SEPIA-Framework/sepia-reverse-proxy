package net.b07z.sepia.proxies;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import net.b07z.sepia.proxies.security.SSLContextBuilder;

/**
 * Command-line interface to start a proxy.
 * 
 * @author Florian Quirin
 *
 */
public class Start {
	
	//Overwrite JBoss logger
	//private static Logger logger;
	static{
		System.setProperty("org.jboss.logging.provider", "slf4j");
		//logger = LoggerFactory.getLogger(Start.class);
	}
	
	//Defaults
	private static String host = "localhost";
	private static int port = 20726;
	private static boolean ssl = false;
	private static boolean sslSupportHttp = false;
	private static String sslKeystore = "";
	private static String sslKeystorePwd = "";
	
	private static String SETTINGS_FILE = "settings/proxy.properties";
	
	//Command-line parameters have priority
	private static boolean ignoreSettingsHost = false;
	private static boolean ignoreSettingsPort = false;
	
	public static void info(String msg){
		System.out.println(msg);
	}
	public static void error(String msg){
		//logger.error(msg);
		System.out.println(msg);
	}
	
	/**
	 * Run a proxy.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		String proxy = "";
		
		//Check if arguments are given
		if (args == null || args.length == 0){
			error("Missing proxy-name to run, e.g. 'tiny'.");
			help();
			System.exit(1);
		}
		
		//Proxy to run:
		if (args[0].equals("tiny")){
			proxy = "tiny";
			info("Starting tiny proxy ...");
			
			for (String arg : args){
				//Port
				if (arg.startsWith("-port=")){
					port = Integer.parseInt(arg.replaceFirst(".*?=", "").trim());
					ignoreSettingsPort = true;
				
				//Host
				}else if (arg.startsWith("-host=")){
					host = arg.replaceFirst(".*?=", "").trim();
					ignoreSettingsHost = true;
					
				//SSL
				}else if (arg.startsWith("-ssl=")){
					ssl = Boolean.parseBoolean(arg.replaceFirst(".*?=", "").trim());
				
				//Paths
				}else if (arg.startsWith("-defaultPaths=")){
					String paths = arg.replaceFirst(".*?=", "").trim();
					if (!paths.equals("true")){
						error("Sorry any other than the default paths are not yet supported via command-line interface!");
						System.exit(1);
					}
					//TODO: add a way to define custom prefix-path combinations (best: load from config and give config-file here as value)
				}
			}
			
			//Read settings
			List<ProxyAction> actions = null;
			KeyStore ks = null;
			SSLContext sslContext = null;
			try{
				info("Loading settings from '" + SETTINGS_FILE + "' ...");
				actions = loadSettings(SETTINGS_FILE);
			}catch(Exception e){
				error("Could not read '" + SETTINGS_FILE + "' file! Error: " + e.getMessage());
				System.exit(1);
			}
			//Check SSL settings and keystore
			if (ssl && (sslKeystore.isEmpty() || sslKeystorePwd.isEmpty())){
				error("Missing SSL keystore and/or keystore password!");
				System.exit(1);
			}else if (ssl){
				try{
					ks = SSLContextBuilder.loadKeyStore("JKS", sslKeystore, sslKeystorePwd);	//TODO: use JKS, PKCS12 or default?
				}catch (Exception e){
					error("Could not load keystore located at: " + sslKeystore + " - check path and password!");
					error("Error msg.: " + e.getMessage());
					System.exit(1);
				}
				try{
					sslContext = SSLContextBuilder.create(ks, null, sslKeystorePwd);
				}catch (Exception e){
					error("Could not create SSLContext from keystore!");
					error("Error msg.: " + e.getMessage());
					System.exit(1);
				}
			}
			
			//Create tiny reverse proxy
			TinyReverseProxy reverseProxy = new TinyReverseProxy(host, port, ssl, sslContext);
			reverseProxy.setSslHttpSupport(sslSupportHttp, (port + 1)); 		//HTTP support is done via listener on PORT+1

			//Add actions
			for (ProxyAction pa : actions){
				if (pa.actionType.equals("redirect")){
					reverseProxy.addPrefixPath(pa.redirectPath, pa.redirectTarget, pa.targetIsPublic);
				}
			}
			/*	
			reverseProxy.addPrefixPath("/sepia/assist", "http://localhost:20721", true);
			*/
			
			//Start proxy
			reverseProxy.start();
			
			//Note
			info("\nSEPIA '" + proxy + "' reverse proxy started as: " + host + ":" + port);
			info("Using SSL: " + ssl);
			if (ssl){
				info("SSL keystore: " + sslKeystore);
				if (sslSupportHttp){
					info("NOTE: All calls to simple HTTP are available at port: " + (port + 1));
				}else{
					info("NOTE: All calls to simple HTTP are deactivated when SSL is active!");
				}
			}
			
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
		info("\nUsage:");
		info("[proxy-name] [arguments]");
		info("\nProxies:");
		info("tiny - args: -defaultPaths=true, -port=20726, -host=localhost, -ssl=true");
		info("\nConfiguration is done via 'settings/proxy.properties' file.");
		info("");
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
					boolean isPublic = Boolean.parseBoolean(config.getProperty("redirect_public_" + name));
					actions.add(new ProxyAction().setRedirect(path, target, isPublic));
				}
			
			}else if (entry.equals("host") && !ignoreSettingsHost){
				host = config.getProperty(entry);
			}else if (entry.equals("port") && !ignoreSettingsPort){
				port = Integer.parseInt(config.getProperty(entry));
			
			}else if (entry.equals("ssl_keystore")){
				sslKeystore = config.getProperty(entry);
			}else if (entry.equals("ssl_keystore_pwd")){
				sslKeystorePwd = config.getProperty(entry);
			}else if (entry.equals("ssl_support_http")){
				sslSupportHttp = Boolean.parseBoolean(config.getProperty(entry));
			}
		}
		return actions;
	}

}
