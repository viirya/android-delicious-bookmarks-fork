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

import android.content.Context;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.net.Uri;
import android.os.Handler;

public class DeliciousApiRequestFactory {

    private final Context context;
	private final DefaultHttpClient mClient;
	
	private final Handler mHandler;

	private volatile int mRequestId = 0;

	private static final String SCHEME = "https";
	private static final String AUTHORITY = "api.del.icio.us";
	private static final int PORT = 443;

	private static final AuthScope SCOPE = new AuthScope(AUTHORITY, PORT);

	// Delicious advises against using a generic User-Agent
	private static final String USER_AGENT = "android-delicious-bookmarks";

	public DeliciousApiRequestFactory(Context context, Handler handler) {
		// mClient = new DefaultHttpClient();
        this.context = context;
        mClient = new MyHttpClient(context);
		mHandler = handler;
		updateUserAgent();
	}

	public void setCredentials(String username, String password) {
		CredentialsProvider provider = mClient.getCredentialsProvider();
		Credentials credentials = new UsernamePasswordCredentials(username,
				password);
		provider.setCredentials(SCOPE, credentials);
	}

	public boolean hasCredentials() {
		CredentialsProvider provider = mClient.getCredentialsProvider();
		return provider.getCredentials(SCOPE) != null;
	}

	public void clearCredentials() {
		CredentialsProvider provider = mClient.getCredentialsProvider();
		provider.clear();
	}

	private void updateUserAgent() {
		HttpParams params = mClient.getParams();
		HttpProtocolParams.setUserAgent(params, USER_AGENT);
	}

	public DeliciousApiRequest createPostsAddRequest(String url, String description,
			String extended, String tags, boolean replace, boolean shared) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME);
		builder.authority(AUTHORITY);
		builder.appendEncodedPath("v1/posts/add");
		builder.appendQueryParameter("url", url);
		builder.appendQueryParameter("description", description);
		builder.appendQueryParameter("extended", extended);
		builder.appendQueryParameter("tags", tags);
		builder.appendQueryParameter("replace", replace ? "yes" : "no");
		builder.appendQueryParameter("shared", shared ? "yes" : "no");
		Uri uri = builder.build();

		DeliciousApiResponseHandler responseHandler = new ResultResponse();

		return new DeliciousApiRequest(DeliciousApiRequest.REQUEST_POSTS_ADD,
				mRequestId++, uri, mClient, responseHandler, mHandler);
	}

	public DeliciousApiRequest createPostsSuggestRequest(String url) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME);
		builder.authority(AUTHORITY);
		builder.appendEncodedPath("v1/posts/suggest");
		builder.appendQueryParameter("url", url);
		Uri uri = builder.build();

		DeliciousApiResponseHandler responseHandler = new SuggestResponse();

		return new DeliciousApiRequest(
				DeliciousApiRequest.REQUEST_POSTS_SUGGEST, mRequestId++, uri,
				mClient, responseHandler, mHandler);
	}

	public DeliciousApiRequest createTagsGetRequest() {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(SCHEME);
		builder.authority(AUTHORITY);
		builder.appendEncodedPath("v1/tags/get");
		Uri uri = builder.build();

		DeliciousApiResponseHandler responseHandler = new TagsResponse();

		return new DeliciousApiRequest(DeliciousApiRequest.REQUEST_TAGS_GET,
				mRequestId++, uri, mClient, responseHandler, mHandler);
	}
}
