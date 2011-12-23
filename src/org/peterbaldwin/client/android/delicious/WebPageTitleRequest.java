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

import java.io.IOException;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xml.sax.XMLReader;

import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.util.Log;

/**
 * Extracts the title from a web page.
 */
public class WebPageTitleRequest extends DefaultRedirectHandler implements
		Runnable, Html.TagHandler {

	public static final int HANDLE_TITLE = 1;
	public static final int HANDLE_REDIRECT = 2;

	private static final String LOG_TAG = "WebPageTitleRequest";

	/**
	 * Removes charset and any other parameters from Content-Type.
	 * <p>
	 * For example, {@code removeCharset("text/html; charset=utf-8")} returns
	 * {@code "text/html"}.
	 */
	private static String normalizeContentType(String contentType) {
		if (contentType != null) {
			int index = contentType.indexOf(';');
			if (index != -1) {
				contentType = contentType.substring(0, index);
			}
		}
		contentType = contentType.trim();
		return contentType;
	}

	private static boolean isHtml(String contentType) {
		contentType = normalizeContentType(contentType);
		return "text/html".equals(contentType)
				|| "application/xhtml+xml".equals(contentType)
				|| "application/xml".equals(contentType);
	}

	private final String mUrl;
	private final Handler mHandler;
	private int mStart;
	private String mTitle;
	private String mRedirectLocation;

	public WebPageTitleRequest(String url, Handler handler) {
		super();
		mUrl = url;
		mHandler = handler;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			client.setRedirectHandler(this);
			try {
				HttpGet request = new HttpGet(mUrl);

				// Set a generic User-Agent to avoid being 
				// redirected to a mobile UI.
				request.addHeader("User-Agent", "Mozilla/5.0");

				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();
				StatusLine statusLine = response.getStatusLine();
				try {
					int statusCode = statusLine.getStatusCode();
					if (statusCode != HttpStatus.SC_OK) {
						throw new IOException("Unexpected response code: "
								+ statusCode);
					}
					
					// Send redirect before checking content type
					// because the redirect is important even if the
					// title cannot be extracted.
					if (mRedirectLocation != null
							&& !mUrl.equals(mRedirectLocation)) {
						int what = HANDLE_REDIRECT;
						Object obj = mRedirectLocation;
						Message msg = mHandler.obtainMessage(what, obj);
						msg.sendToTarget();
					}
					Header contentType = entity.getContentType();
					if (contentType != null) {
						String value = contentType.getValue();
						if (!isHtml(value)) {
							// This is important because the user might try
							// bookmarking a video or another large file.
							throw new IOException("Unsupported content type: "
									+ value);
						}
					} else {
						throw new IOException("Content type is missing");
					}
					String source = EntityUtils.toString(entity);
					Html.ImageGetter imageGetter = null;
					Html.TagHandler tagHandler = this;
					Html.fromHtml(source, imageGetter, tagHandler);
				} finally {
					if (entity != null) {
						entity.consumeContent();
					}
				}
			} finally {
				client.getConnectionManager().shutdown();
			}
		} catch (TerminateParser e) {
			// Thrown by handleTag to terminate parser early.
		} catch (IOException e) {
			Log.e(LOG_TAG, "i/o error", e);
		} catch (RuntimeException e) {
			Log.e(LOG_TAG, "runtime error", e);
		} catch (Error e) {
			Log.e(LOG_TAG, "severe error", e);
		} finally {
			Message msg = mHandler.obtainMessage(HANDLE_TITLE, mTitle);
			msg.sendToTarget();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void handleTag(boolean opening, String tag, Editable output,
			XMLReader xmlReader) {
		if ("title".equalsIgnoreCase(tag)) {
			if (opening) {
				mStart = output.length();
			} else {
				int end = output.length();
				String title = output.subSequence(mStart, end).toString();
				
				// Collapse internal whitespace
				title = title.replaceAll("\\s+", " ");
				
				// Remove leading/trailing space
				title = title.trim();
				
				mTitle = title;
				
				throw new TerminateParser();
			}
		}
	}

	@Override
	public URI getLocationURI(HttpResponse response, HttpContext context)
			throws ProtocolException {
		URI location = super.getLocationURI(response, context);
		if (location != null) {
			mRedirectLocation = location.toString();
		}
		return location;
	}

	@SuppressWarnings("serial")
	private static class TerminateParser extends RuntimeException {
	};
}
