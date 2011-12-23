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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ContentHandler;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;

public class SuggestResponse implements DeliciousApiResponseHandler {

	private final ContentHandler mContentHandler;
	private final List<String> mRecommended = new ArrayList<String>();
	private final List<String> mPopular = new ArrayList<String>();

	public SuggestResponse() {
		RootElement root = new RootElement("suggest");

		Element recommended = root.getChild("recommended");
		recommended.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				String suggestion = body.trim();
				mRecommended.add(suggestion);
			}
		});

		Element popular = root.getChild("popular");
		popular.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				String suggestion = body.trim();
				mPopular.add(suggestion);
			}
		});

		mContentHandler = root.getContentHandler();
	}

	/**
	 * {@inheritDoc}
	 */
	public ContentHandler getContentHandler() {
		return mContentHandler;
	}

	public String[] getRecommended() {
		return mRecommended.toArray(new String[mRecommended.size()]);
	}

	public String[] getPopular() {
		return mPopular.toArray(new String[mPopular.size()]);
	}
}
