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

import android.widget.MultiAutoCompleteTextView;

public class TagTokenizer implements MultiAutoCompleteTextView.Tokenizer {

	/**
	 * {@inheritDoc}
	 */
	public int findTokenStart(CharSequence text, int cursor) {
		int i = cursor;
		while (i > 0 && text.charAt(i - 1) != ' ') {
			i--;
		}
		return i;
	}

	/**
	 * {@inheritDoc}
	 */
	public int findTokenEnd(CharSequence text, int cursor) {
		int len = text.length();
		for (int i = cursor; i < len; i++) {
			if (text.charAt(i) == ' ') {
				return i;
			}
		}
		return len;
	}

	/**
	 * {@inheritDoc}
	 */
	public CharSequence terminateToken(CharSequence text) {
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			if (c == ' ') {
				// Remove any meta-data following the tag (e.g., the count)
				return text.subSequence(0, i + 1);
			}
		}
		return text + " ";
	}
}
