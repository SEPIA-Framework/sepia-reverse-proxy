# SEPIA Reverse-Proxy
Part of the [SEPIA Framework](https://sepia-framework.github.io/)  
  
**NOTE:** Development on this tool was frozen in favor of global Nginx support throughout all SEPIA components. Occasional security updates might be uploaded though.  
  
This is a tiny reverse proxy made completely in Java and based on Undertow. It was primarily built to work as a cross-platform, lightweight proxy for the [SEPIA custom-bundle](https://github.com/SEPIA-Framework/sepia-installation-and-setup) but can easily be adapted for any other project:

```java
//Create tiny reverse proxy
int port = 20726;
String host = "localhost";
TinyReverseProxy reverseProxy = new TinyReverseProxy(host, port);

//Add paths - SEPIA defaults for custom-bundle:
boolean isPublic = true;    //allows access from public and local IPs
reverseProxy.addPrefixPath("/sepia/assist", "http://localhost:20721", isPublic);
reverseProxy.addPrefixPath("/sepia/teach", "http://localhost:20722", isPublic);
reverseProxy.addPrefixPath("/sepia/chat", "http://localhost:20723", isPublic);

//Start proxy
reverseProxy.start();
```
SSL is supported and can for example be used with Letsencrypt certificates, you just need to convert them to a Java key-store. See this [bash script](https://github.com/SEPIA-Framework/sepia-installation-and-setup/blob/master/sepia-custom-bundle-folder/letsencrypt/copy-cert-to-keystore.sh) for an example.  

Hostname, port and proxy paths can be configured via the `proxy.properties` file in `settings`.  

Before using: Be sure to check-out the source to tweak the number of possible threads for more performance. If you use the IP filter for local addresses make sure that there is NO OTHER proxy in front otherwhise all your IPs will be local and the filter will NOT WORK!  

Good to know: Thanks to Undertow it also works as proxy for WebSocket servers out-of-the-box :-)

