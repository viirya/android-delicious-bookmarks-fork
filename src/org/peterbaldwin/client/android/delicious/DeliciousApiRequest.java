/*-
 *  Copyright (C) 2009 Peter Baldwin   
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.client.android.delicious;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

// TODO: improve error messages
public class DeliciousApiRequest implements Runnable {
	@SuppressWarnings("serial")
	private static class ConnectionException extends IOException {
		private final int mStatusCode;

		private ConnectionException(int statusCode) {
			mStatusCode = statusCode;
		}

		public int getStatusCode() {
			return mStatusCode;
		}
	}

	private static final String LOG_TAG = "DeliciousApiRequest";
	// TODO: make error codes into constructor parameters?
	public static final int HANDLE_AUTH_ERROR = -2;
	public static final int HANDLE_ERROR = -1;
	public static final int HANDLE_DONE = 0;

	public static final int REQUEST_POSTS_ADD = 1;
	public static final int REQUEST_POSTS_SUGGEST = 2;
	public static final int REQUEST_TAGS_GET = 3;

	private static final int BUFFER_SIZE = 1024;

	private final int mRequestType;
	private final int mRequestId;
	private final Uri mUri;
	private final HttpClient mClient;
	private final DeliciousApiResponseHandler mResponseHandler;
	private final Handler mHandler;
	private File mCacheFile;
	private boolean mUseCache;
	private boolean mUpdateCache;
	private boolean mAlwaysUpdateCache;

	DeliciousApiRequest(int requestType, int requestId, Uri uri,
			HttpClient client, DeliciousApiResponseHandler responseHandler,
			Handler handler) {
		super();
		mRequestType = requestType;
		mRequestId = requestId;
		mUri = uri;
		mClient = client;
		mResponseHandler = responseHandler;
		mHandler = handler;
	}

	private void setError(Message msg, Throwable t) {
		if (mCacheFile != null) {
			mCacheFile.delete();
		}
		String error = String.valueOf(t);
		msg.what = HANDLE_ERROR;
		msg.obj = error;
		Log.e(LOG_TAG, error, t);
	}

	private InputStream readFromCache() throws IOException {
		long start = now();
		try {
			if (mCacheFile == null) {
				throw new NullPointerException();
			}
			if (mCacheFile.exists()) {
				InputStream in = new FileInputStream(mCacheFile);
				in = new BufferedInputStream(in);
				return in;
			}
			return null;
		} finally {
			logTiming("read cache", start);
		}
	}

	private InputStream updateCache(InputStream in) throws IOException {
		long start = now();
		try {
			try {
				if (mCacheFile == null) {
					throw new NullPointerException();
				}
				File parent = mCacheFile.getParentFile();

				// An exception will be thrown below when trying to write
				// the file if the parent directories were not created.
				parent.mkdirs();

				mCacheFile.createNewFile();
				OutputStream fout = new FileOutputStream(mCacheFile);
				fout = new BufferedOutputStream(fout);
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				try {
					byte[] buffer = new byte[BUFFER_SIZE];
					while (true) {
						int size = in.read(buffer);
						if (size < 0) {
							break;
						}
						bout.write(buffer, 0, size);
						fout.write(buffer, 0, size);
					}
				} finally {
					bout.close();
					fout.close();
				}
				byte[] data = bout.toByteArray();
				return new ByteArrayInputStream(data);
			} finally {
				in.close();
			}
		} finally {
			logTiming("update cache", start);
		}
	}

	private InputStream readFromNetwork() throws IOException {
		long start = now();
		try {
            Log.d(LOG_TAG, "URL: " + mUri);
			HttpGet request = new HttpGet(String.valueOf(mUri));
			HttpResponse response = mClient.execute(request);
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new ConnectionException(statusCode);
			}
			HttpEntity entity = response.getEntity();
			InputStream in = entity.getContent();
			in = new BufferedInputStream(in);
			return in;
		} finally {
			logTiming("read network", start);
		}
	}

	private static long now() {
		return SystemClock.uptimeMillis();
	}

	private static void logTiming(String action, long start) {
		long end = now();
		long duration = end - start;
		String msg = String.format("%s took %d ms", action, duration);
		Log.d(LOG_TAG, msg);
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		Message msg = mHandler.obtainMessage();
		msg.arg1 = mRequestType;
		msg.arg2 = mRequestId;
		msg.what = HANDLE_ERROR;
		msg.obj = "unknown error";
		boolean cacheUpdated = false;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();

			ContentHandler handler = mResponseHandler.getContentHandler();
			reader.setContentHandler(handler);

			InputStream in = null;
			if (in == null && mUseCache && mCacheFile != null) {
				in = readFromCache();
			}
			if (in == null) {
				in = readFromNetwork();

				if (mUpdateCache && mCacheFile != null) {
					in = updateCache(in);
					cacheUpdated = true;
				}
			}
			try {
				InputSource input = new InputSource(in);
				long start = now();
				try {
					reader.parse(input);
				} finally {
					logTiming("parse", start);
				}
			} finally {
				in.close();
			}
			msg.what = HANDLE_DONE;
			msg.obj = mResponseHandler;
		} catch (ConnectionException e) {
			setError(msg, e);
			int statusCode = e.getStatusCode();
			if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
				msg.what = HANDLE_AUTH_ERROR;
				msg.obj = "invalid username or password";
			} else {
				msg.what = HANDLE_ERROR;
				msg.obj = "unexpected response: " + statusCode;
			}
		} catch (IOException e) {
			setError(msg, e);
		} catch (ParserConfigurationException e) {
			setError(msg, e);
		} catch (SAXException e) {
			setError(msg, e);
		} catch (RuntimeException e) {
			setError(msg, e);
		} catch (Error e) {
			setError(msg, e);
		} finally {
			mHandler.sendMessage(msg);
		}

		if (msg.what != HANDLE_AUTH_ERROR && !cacheUpdated
				&& mAlwaysUpdateCache && mCacheFile != null) {
			// If the authentication is valid, but the cache was not updated,
			// silently update the cache in the background after dispatching the
			// result to the handler.
			try {
				InputStream in = readFromNetwork();
				in = updateCache(in);
				in.close();
				Log.i(LOG_TAG, "cache file updated: " + mCacheFile);
			} catch (IOException e) {
				Log.e(LOG_TAG, "error updating cache", e);
			} catch (RuntimeException e) {
				Log.e(LOG_TAG, "error updating cache", e);
			} catch (Error e) {
				Log.e(LOG_TAG, "error updating cache", e);
			}
		}
	}

	public void setCacheFile(File cacheFile) {
		mCacheFile = cacheFile;
	}

	public void setUseCache(boolean useCache) {
		mUseCache = useCache;
	}

	public void setUpdateCache(boolean updateCache) {
		mUpdateCache = updateCache;
	}

	public void setAlwaysUpdateCache(boolean alwaysUpdateCache) {
		mAlwaysUpdateCache = alwaysUpdateCache;
	}
}
