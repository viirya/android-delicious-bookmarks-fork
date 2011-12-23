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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Dialog implements View.OnClickListener {

	private EditText mEditUsername;
	private EditText mEditPassword;
	private Button mButtonLogin;
	private Button mButtonCancel;
	
	private DialogInterface.OnClickListener mOnClickListener;

	public Login(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.title_login);
		setContentView(R.layout.login);

		mEditUsername = (EditText) findViewById(R.id.edit_username);
		mEditPassword = (EditText) findViewById(R.id.edit_password);

		mButtonLogin = (Button) findViewById(R.id.button_login);
		mButtonCancel = (Button) findViewById(R.id.button_cancel);
		
		mButtonLogin.setOnClickListener(this);
		mButtonCancel.setOnClickListener(this);
	}
	
	public String getUsername() {
		return mEditUsername.getText().toString();
	}
	
	public String getPassword() {
		return mEditPassword.getText().toString();
	}

	public DialogInterface.OnClickListener getOnClickListener() {
		return mOnClickListener;
	}

	public void setOnClickListener(DialogInterface.OnClickListener onClickListener) {
		mOnClickListener = onClickListener;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onClick(View v) {
		if (mOnClickListener != null) {
			if (v == mButtonLogin) {
				mOnClickListener.onClick(this, BUTTON_POSITIVE);
			} else if (v == mButtonCancel) {
				mOnClickListener.onClick(this, BUTTON_NEGATIVE);
			}
		}
		dismiss();
	}
}
