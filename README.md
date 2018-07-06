# SEPIA Reverse-Proxy
Part of the [SEPIA Framework](https://sepia-framework.github.io/)  

This is a tiny reverse proxy made completely in Java and based on Undertow. It was primarily built to work as a cross-platform, lightweight proxy for the [SEPIA custom-bundle](https://github.com/SEPIA-Framework/sepia-installation-and-setup) but can easily be adapted for any other project:

```java
//Create tiny reverse proxy
TinyReverseProxy reverseProxy = new TinyReverseProxy(host, port);

//Add paths - SEPIA defaults for custom-bundle:
reverseProxy.addPrefixPath("/sepia/assist", "http://localhost:20721");
reverseProxy.addPrefixPath("/sepia/teach", "http://localhost:20722");
reverseProxy.addPrefixPath("/sepia/chat", "http://localhost:20723");

//Start proxy
reverseProxy.start();
```

Be sure to check-out the source to tweak the number of possible threads for more performance.
