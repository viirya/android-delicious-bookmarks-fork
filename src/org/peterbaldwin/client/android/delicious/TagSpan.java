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

import android.text.style.ClickableSpan;
import android.view.View;

public class TagSpan extends ClickableSpan {
	public interface OnTagClickListener {
		public void onTagClick(String tag);
	}

	private final String mTag;
	private OnTagClickListener mOnTagClickListener;

	public TagSpan(String tag) {
		super();
		if (tag == null) {
			throw new NullPointerException();
		}
		mTag = tag;
	}

	@Override
	public void onClick(View widget) {
		if (mOnTagClickListener != null) {
			mOnTagClickListener.onTagClick(mTag);
		}
	}

	public OnTagClickListener getOnTagClickListener() {
		return mOnTagClickListener;
	}

	public void setOnTagClickListener(OnTagClickListener onTagClickListener) {
		mOnTagClickListener = onTagClickListener;
	}
}
