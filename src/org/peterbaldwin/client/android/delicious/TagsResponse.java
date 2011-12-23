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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;

public class TagsResponse implements DeliciousApiResponseHandler,
		StartElementListener {

	private static final String LOG_TAG = "TagsResponse";

	public static class Tag {
		private final String mTag;
		private final int mCount;
		private final String mStringValue;

		private Tag(String tag, int count) {
			super();
			this.mTag = tag;
			this.mCount = count;
			mStringValue = count < 0 ? tag : tag + " (" + count + ")";
		}

		public String getTag() {
			return mTag;
		}

		public int getCount() {
			return mCount;
		}

		@Override
		public String toString() {
			return mStringValue;
		}
	}

	private final ContentHandler mContentHandler;

	private final List<Tag> mTags = new ArrayList<Tag>();

	public TagsResponse() {
		RootElement root = new RootElement("tags");
		Element tag = root.getChild("tag");
		tag.setStartElementListener(this);
		mContentHandler = root.getContentHandler();
	}

	/**
	 * {@inheritDoc}
	 */
	public ContentHandler getContentHandler() {
		return mContentHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	public void start(Attributes attributes) {
		String tag = attributes.getValue("tag");
		if (tag != null) {
			String countString = attributes.getValue("count");
			int count = -1;
			if (countString != null) {
				try {
					count = Integer.valueOf(countString);
				} catch (NumberFormatException e) {
					Log.e(LOG_TAG, "invalid count: " + countString, e);
				}
			} else {
				Log.e(LOG_TAG, "count missing" + countString);
			}
			mTags.add(new Tag(tag, count));
		} else {
			Log.e(LOG_TAG, "tag name missing");
		}
	}

	public Tag[] getTags() {
		return mTags.toArray(new Tag[mTags.size()]);
	}
}
