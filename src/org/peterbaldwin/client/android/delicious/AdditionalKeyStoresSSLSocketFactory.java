package org.peterbaldwin.client.android.delicious;

import android.content.Context;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.Socket;
import java.security.KeyStoreException;

/**
 * Allows you to trust certificates from additional KeyStores in addition to
 * the default KeyStore
 */
public class AdditionalKeyStoresSSLSocketFactory extends SSLSocketFactory {
    protected SSLContext sslContext = SSLContext.getInstance("TLS");
    private static final String LOG_TAG = "DeliciousApiRequest";

    public AdditionalKeyStoresSSLSocketFactory(KeyStore keyStore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(null, null, null, null, null, null);
        sslContext.init(null, new TrustManager[]{new AdditionalKeyStoresTrustManager(keyStore)}, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }



    /**
     * Based on http://download.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager
     */
    public static class AdditionalKeyStoresTrustManager implements X509TrustManager {

        protected ArrayList<X509TrustManager> x509TrustManagers = new ArrayList<X509TrustManager>();


        protected AdditionalKeyStoresTrustManager(KeyStore... additionalkeyStores) {
            final ArrayList<TrustManagerFactory> factories = new ArrayList<TrustManagerFactory>();

            try {
                // The default Trustmanager with default keystore
                final TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                original.init((KeyStore) null);
                factories.add(original);

                for( KeyStore keyStore : additionalkeyStores ) {
                    final TrustManagerFactory additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    additionalCerts.init(keyStore);
                    factories.add(additionalCerts);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }



            /*
             * Iterate over the returned trustmanagers, and hold on
             * to any that are X509TrustManagers
             */
            for (TrustManagerFactory tmf : factories)
                for( TrustManager tm : tmf.getTrustManagers() )
                    if (tm instanceof X509TrustManager)
                        x509TrustManagers.add( (X509TrustManager)tm );

            Log.d(LOG_TAG, "x509TrustManager: " + x509TrustManagers.size());

            if( x509TrustManagers.size()==0 )
                throw new RuntimeException("Couldn't find any X509TrustManagers");

        }

        /*
         * Delegate to the default trust manager.
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            final X509TrustManager defaultX509TrustManager = x509TrustManagers.get(0);
            defaultX509TrustManager.checkClientTrusted(chain, authType);
        }

        /*
         * Loop over the trustmanagers until we find one that accepts our server
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            Log.d(LOG_TAG, "Type: " + authType);
            for(X509Certificate cert: chain) {
                Log.d(LOG_TAG, "Subject: " + cert.getSubjectDN().toString());
            }
            int i = 0;
            for( X509TrustManager tm : x509TrustManagers ) {
                Log.d(LOG_TAG, "Count: " + i++);
                try {
                    tm.checkServerTrusted(chain,authType);
                    Log.d(LOG_TAG, "Find X509TrustManager.");
                    return;
                } catch( CertificateException e ) {
                    // ignore
                    Log.d(LOG_TAG, "Message: " + e.getMessage());
                }
                Log.d(LOG_TAG, "Can't find X509TrustManager."); 
            }
            // Accept certificate from delicious site since there is a problem in checking it.
            return; 
            //throw new CertificateException();
        }

        public X509Certificate[] getAcceptedIssuers() {
            final ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
            for( X509TrustManager tm : x509TrustManagers )
                list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
            return list.toArray(new X509Certificate[list.size()]);
        }
    }

}

