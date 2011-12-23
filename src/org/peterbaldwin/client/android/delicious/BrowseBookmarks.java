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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

public class BrowseBookmarks extends Activity {
	// This URL will redirect to the user's bookmarks
	Uri HOME = Uri.parse("http://m.delicious.com/home");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(Intent.ACTION_VIEW, HOME);
		intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
		startActivity(intent);
		finish();
	}
}