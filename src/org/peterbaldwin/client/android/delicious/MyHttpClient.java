package org.peterbaldwin.client.android.delicious;

import android.content.Context;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import android.util.Log;

import java.io.InputStream;
import java.security.KeyStore;

public class MyHttpClient extends DefaultHttpClient {

  private final Context context;
  private static final String LOG_TAG = "DeliciousApiRequest";

  public MyHttpClient(Context context) {
    this.context = context;
  }

  @Override protected ClientConnectionManager createClientConnectionManager() {
    SchemeRegistry registry = new SchemeRegistry();
    registry.register(
        new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    registry.register(new Scheme("https", newSslSocketFactory(), 443));
    final HttpParams params = new BasicHttpParams();
    return new SingleClientConnManager(params, registry);
  }

  private SSLSocketFactory newSslSocketFactory() {
    try {
      KeyStore trusted = KeyStore.getInstance("BKS");
      InputStream in = context.getResources().openRawResource(R.raw.mystore);
      try {
        trusted.load(in, "ez24get".toCharArray());
        // Log.d(LOG_TAG, "Read KeyStore");
      } finally {
        in.close();
      }
      return new AdditionalKeyStoresSSLSocketFactory(trusted);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}

