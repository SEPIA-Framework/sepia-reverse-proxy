package net.b07z.sepia.proxies.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Class to build the SSLContext required to secure proxy with SSL certificates.<br>
 * Note: Certificates have to be converted to Java keystore (jks) before you can use them.
 */
public class SSLContextBuilder {

	/**
	 * Create a SSLContext using a key-store and a trust-store (null for default) with password.
	 * @param keyStore - pre-loaded key-store
	 * @param trustStore - pre-loaded trust-store or null for default
	 * @param password - password of key-store
	 * @return
	 * @throws Exception
	 */
	public static SSLContext create(final KeyStore keyStore, final KeyStore trustStore, String password) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }
	
	/**
	 * Load a keystore file.
	 * @param pathAndFilename
	 * @param password
	 * @return
	 * @throws Exception
	 */
    public static KeyStore loadKeyStore(String pathAndFilename, String password) throws Exception {
        final InputStream stream = Files.newInputStream(Paths.get(pathAndFilename));
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try(InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, password.toCharArray());
            return loadedKeystore;
        }
    }
}
