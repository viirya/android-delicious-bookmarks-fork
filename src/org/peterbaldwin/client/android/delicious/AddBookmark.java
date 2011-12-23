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

import java.io.File;
import java.util.ArrayList;

import org.peterbaldwin.client.android.delicious.TagsResponse.Tag;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Browser;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AddBookmark extends Activity implements View.OnClickListener,
		DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
		TagSpan.OnTagClickListener, TextView.OnEditorActionListener {

	private NonConfigurationInstance mNonConfigurationInstance;

	private static final int REQUEST_CODE_LOGIN_FOR_ADD = 1;
	private static final int REQUEST_CODE_LOGIN_FOR_TAGS = 2;

	private static final String LOG_TAG = "DeliciousAddBookmark";

	private static final String PREFS_NAME = "deliciousAuth";
	private static final String PREFS_KEY_USERNAME = "username";
	private static final String PREFS_KEY_PASSWORD = "password";

	private static final String STATE_TAGS = "tags";
	private static final String STATE_RECOMMENDED_TAGS = "recommendedTags";
	private static final String STATE_POPULAR_TAGS = "popularTags";
	private static final String STATE_REQUEST_CODE = "requestCode";

	private static final int DIALOG_LOGIN = 1;
	private static final int DIALOG_SAVE_PROGRESS = 2;

	private TextView mTextUser;
	private EditText mEditUrl;
	private EditText mEditTitle;
	private EditText mEditNotes;
	private MultiAutoCompleteTextView mEditTags;
	private CheckBox mCheckDoNotShare;
	private TextView mTextRecommended;
	private TextView mTextPopular;

	private ArrayList<String> mTags;
	private ArrayList<String> mRecommendedTags;
	private ArrayList<String> mPopularTags;

	private Button mButtonSave;
	private Button mButtonDiscard;
	
	private int mRequestCode;

	private DeliciousApiRequestFactory mFactory;

	private Login mLoginDialog;

	private void dismissProgress(int type) {
		switch (type) {
		case DeliciousApiRequest.REQUEST_POSTS_ADD:
			dismissProgressDialog();
			break;
		case DeliciousApiRequest.REQUEST_POSTS_SUGGEST:
			dismissProgressSpinner(R.id.progress_recommended_tags);
			dismissProgressSpinner(R.id.progress_popular_tags);
			break;
		case DeliciousApiRequest.REQUEST_TAGS_GET:
			dismissProgressSpinner(R.id.progress_tags);
			break;
		}
	}

	private void dismissProgressDialog() {
		dismissDialog(DIALOG_SAVE_PROGRESS);
	}

	private void dismissProgressSpinner(int id) {
		ProgressBar spinner = (ProgressBar) findViewById(id);
		spinner.setVisibility(View.GONE);
	}

	private void showProgressSpinner(int id) {
		ProgressBar spinner = (ProgressBar) findViewById(id);
		spinner.setVisibility(View.VISIBLE);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int type = msg.arg1;
			switch (msg.what) {
			case WebPageTitleRequest.HANDLE_TITLE:
				mNonConfigurationInstance.setTitlePending(false);
				dismissProgressSpinner(R.id.progress_title);
				String title = (String) msg.obj;
				if (title != null) {
					if (mEditTitle.getText().length() == 0) {
						mEditTitle.setText(title.trim());
					}
				}
				break;
			case WebPageTitleRequest.HANDLE_REDIRECT:
				String url = (String) msg.obj;
				mEditUrl.setText(url);
				break;
			case DeliciousApiRequest.HANDLE_DONE:
				dismissProgress(type);
				switch (type) {
				case DeliciousApiRequest.REQUEST_POSTS_ADD:
					ResultResponse resultResponse = (ResultResponse) msg.obj;
					if (resultResponse.hasError()) {
						msg.what = DeliciousApiRequest.HANDLE_ERROR;
						msg.obj = resultResponse.getError();
						handleMessage(msg);
					} else {
						finish();
					}
					break;
				case DeliciousApiRequest.REQUEST_TAGS_GET:
					mNonConfigurationInstance.setTagsPending(false);
					TagsResponse tagsResponse = (TagsResponse) msg.obj;
					Tag[] tags = tagsResponse.getTags();
					setTags(tags);
					break;
				case DeliciousApiRequest.REQUEST_POSTS_SUGGEST:
					mNonConfigurationInstance.setSuggestionsPending(false);
					SuggestResponse suggestResponse = (SuggestResponse) msg.obj;
					String[] recommended = suggestResponse.getRecommended();
					String[] popular = suggestResponse.getPopular();
					setRecommended(recommended);
					setPopular(popular);
					break;
				}
				break;
			case DeliciousApiRequest.HANDLE_AUTH_ERROR:
				clearCredentials();
			case DeliciousApiRequest.HANDLE_ERROR:
				String message = (String) msg.obj;
				dismissProgress(type);
				if (type == DeliciousApiRequest.REQUEST_POSTS_ADD) {
					error(message);
				} else {
					Log.e(LOG_TAG, message);
				}
				break;
			}
		}
	};

	void error(CharSequence message) {
		Context context = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setPositiveButton(android.R.string.ok, this);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	void error(int msg) {
		CharSequence message = getText(msg);
		error(message);
	}

	private void setTags(TextView textView, ArrayList<String> list) {
		if (list == null) {
			textView.setText(R.string.label_tags_loading);
		} else if (list.isEmpty()) {
			textView.setText(R.string.label_tags_none);
		} else {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			for (final String tag : list) {
				if (builder.length() != 0) {
					builder.append("  ");
				}
				int start = builder.length();
				builder.append(tag);
				int end = builder.length();
				TagSpan span = new TagSpan(tag);
				span.setOnTagClickListener(this);

				int flags = 0;
				builder.setSpan(span, start, end, flags);
			}
			textView.setText(builder);
		}
	}

	void setRecommended(String[] recommended) {
		setRecommended(asArrayList(recommended));
	}

	void setPopular(String[] popular) {
		setPopular(asArrayList(popular));
	}

	void setRecommended(ArrayList<String> recommended) {
		mRecommendedTags = recommended;
		setTags(mTextRecommended, mRecommendedTags);
	}

	void setPopular(ArrayList<String> popular) {
		mPopularTags = popular;
		setTags(mTextPopular, mPopularTags);
	}

	private static ArrayList<String> asArrayList(Object[] array) {
		ArrayList<String> list = new ArrayList<String>(array.length);
		for (Object element : array) {
			list.add(String.valueOf(element));
		}
		return list;
	}

	void setTags(ArrayList<String> tags) {
		mTags = tags;
		Context context = this;
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_dropdown_item_1line, mTags);
		mEditTags.setAdapter(adapter);
	}

	void setTags(Tag[] tags) {
		setTags(asArrayList(tags));
	}

	private File getTagsCacheFile() {
		File file = getFilesDir();
		file = new File(file, "v1-tags-get.xml");
		return file;
	}

	void loadCredentials() {
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		String username = preferences.getString(PREFS_KEY_USERNAME, null);
		String password = preferences.getString(PREFS_KEY_PASSWORD, null);
		if (username != null && password != null) {
			mFactory.setCredentials(username, password);
			
			CharSequence template = getText(R.string.current_user);
			CharSequence text = TextUtils.expandTemplate(template, username);
			mTextUser.setText(text);
		} else {
			mTextUser.setText(R.string.current_user_none);
		}
	}

	boolean saveCredentials(String username, String password) {
		mFactory.setCredentials(username, password);
		
		CharSequence template = getText(R.string.current_user);
		CharSequence text = TextUtils.expandTemplate(template, username);
		mTextUser.setText(text);
		
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFS_KEY_USERNAME, username);
		editor.putString(PREFS_KEY_PASSWORD, password);
		return editor.commit();
	}

	boolean clearCredentials() {
		getTagsCacheFile().delete();
		mFactory.clearCredentials();
		SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(PREFS_KEY_USERNAME);
		editor.remove(PREFS_KEY_PASSWORD);
		return editor.commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_bookmark);
		mTextUser = (TextView) findViewById(R.id.user);
		mEditUrl = (EditText) findViewById(R.id.edit_url);
		mEditTitle = (EditText) findViewById(R.id.edit_title);
		mEditNotes = (EditText) findViewById(R.id.edit_notes);
		mEditTags = (MultiAutoCompleteTextView) findViewById(R.id.edit_tags);
		mCheckDoNotShare = (CheckBox) findViewById(R.id.check_do_not_share);
		mTextRecommended = (TextView) findViewById(R.id.text_recommended_tags);
		mTextPopular = (TextView) findViewById(R.id.text_popular_tags);
		mEditTags.setTokenizer(new TagTokenizer());
		mEditTags.setThreshold(1);
		mEditTags.setOnEditorActionListener(this);
		prepareTagView(mTextRecommended);
		prepareTagView(mTextPopular);

		mButtonSave = (Button) findViewById(R.id.button_save);
		mButtonDiscard = (Button) findViewById(R.id.button_discard);

		if (savedInstanceState == null) {
			Intent intent = getIntent();
			String action = intent.getAction();
			Uri data = intent.getData();
			String type = intent.getType();
			if (Intent.ACTION_INSERT.equals(action)
					&& Browser.BOOKMARKS_URI.equals(data)) {
				String url = intent.getStringExtra("url");
				String title = intent.getStringExtra("title");
				mEditUrl.setText(url);
				mEditTitle.setText(title);
			} else if (Intent.ACTION_SEND.equals(action)
					&& "text/plain".equals(type)) {
				String url = intent.getStringExtra(Intent.EXTRA_TEXT);
				mEditUrl.setText(url);
			} else {
				// For debugging
				mEditUrl.setText("http://www.youtube.com/");
				mEditTitle.setText("");
			}
		} else {
			ArrayList<String> tags = savedInstanceState.getStringArrayList(STATE_TAGS);
			setTags(tags);
			mRecommendedTags = savedInstanceState.getStringArrayList(STATE_RECOMMENDED_TAGS);
			mPopularTags = savedInstanceState.getStringArrayList(STATE_POPULAR_TAGS);
			mRequestCode = savedInstanceState.getInt(STATE_REQUEST_CODE);
		}

		mButtonSave.setOnClickListener(this);
		mButtonDiscard.setOnClickListener(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		Object nonConfigurationInstance = getLastNonConfigurationInstance();
		mNonConfigurationInstance = (NonConfigurationInstance) nonConfigurationInstance;
		if (mNonConfigurationInstance == null) {
			Looper looper = Looper.myLooper();
			mNonConfigurationInstance = new NonConfigurationInstance(looper);
		}

		// Create a new request factory. All messages are proxied
		// through the NonConfigurationInstance because the activity
		// might be destroyed and re-created before the request completes.
		Handler proxy = mNonConfigurationInstance.getProxy();
		mFactory = new DeliciousApiRequestFactory(this, proxy);

		// Set the the proxy end-point to the Handler belonging to
		// the current Activity instance.
		mNonConfigurationInstance.setHandler(mHandler);

		loadCredentials();

		setTags(mTextRecommended, mRecommendedTags);
		setTags(mTextPopular, mPopularTags);

		if (nonConfigurationInstance == null) {
			String title = mEditTitle.getText().toString();
			if (title.length() == 0) {
				String url = mEditUrl.getText().toString();
				requestTitle(url);
			}

			if (mFactory.hasCredentials()) {
				if (mTags == null) {
					requestUserTags(false);
				}
				if (mRecommendedTags == null || mPopularTags == null) {
					requestSuggestions();
				}
			} else {
				showLogin(REQUEST_CODE_LOGIN_FOR_TAGS);
			}
		}
		showProgressSpinners();
	}
	
	private void showProgressSpinners() {
		if (mNonConfigurationInstance.isTitlePending()) {
			showProgressSpinner(R.id.progress_title);
		}
		if (mNonConfigurationInstance.isTagsPending()) {
			showProgressSpinner(R.id.progress_tags);
		}
		if (mNonConfigurationInstance.isSuggestionsPending()) {
			showProgressSpinner(R.id.progress_recommended_tags);
			showProgressSpinner(R.id.progress_popular_tags);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Object nonConfigurationInstance = mNonConfigurationInstance;
		mNonConfigurationInstance = null;
		return nonConfigurationInstance;
	}

	@Override
	protected void onDestroy() {
		if (mNonConfigurationInstance != null) {
			mNonConfigurationInstance.quit();
			mNonConfigurationInstance = null;
		}

		super.onDestroy();
	}

	private static final void addLinkMovementMethod(TextView t) {
		MovementMethod m = t.getMovementMethod();

		if ((m == null) || !(m instanceof LinkMovementMethod)) {
			if (t.getLinksClickable()) {
				t.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
	}

	private void prepareTagView(TextView textView) {
		addLinkMovementMethod(textView);
		setTags(textView, null);
	}

	private void requestTitle(String url) {
		Handler proxy = mNonConfigurationInstance.getProxy();
		Runnable request = new WebPageTitleRequest(url, proxy);
		mNonConfigurationInstance.sendTitleRequest(request);
		mNonConfigurationInstance.setTitlePending(true);
	}

	private void requestUserTags(boolean refresh) {
		DeliciousApiRequest request = mFactory.createTagsGetRequest();
		File cacheFile = getTagsCacheFile();
		request.setCacheFile(cacheFile);
		request.setUseCache(!refresh);
		request.setUpdateCache(true);
		request.setAlwaysUpdateCache(true);
		mNonConfigurationInstance.sendDeliciousRequest(request);
		mNonConfigurationInstance.setTagsPending(true);
	}

	private boolean requestSuggestions() {
		String url = mEditUrl.getText().toString();
		Runnable request = mFactory.createPostsSuggestRequest(url);
		mNonConfigurationInstance.sendDeliciousRequest(request);
		mNonConfigurationInstance.setSuggestionsPending(true);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTags != null) {
			outState.putStringArrayList(STATE_TAGS, mTags);
		}
		if (mRecommendedTags != null) {
			outState.putStringArrayList(STATE_RECOMMENDED_TAGS,
					mRecommendedTags);
		}
		if (mPopularTags != null) {
			outState.putStringArrayList(STATE_POPULAR_TAGS, mPopularTags);
		}
		outState.putInt(STATE_REQUEST_CODE, mRequestCode);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_LOGIN) {
			mLoginDialog = new Login(this);
			mLoginDialog.setOnClickListener(this);
			mLoginDialog.setOnCancelListener(this);
			return mLoginDialog;
		} else if (id == DIALOG_SAVE_PROGRESS) {
			CharSequence message = getText(R.string.message_saving);
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(message);
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		} else {
			return super.onCreateDialog(id);
		}
	}

	private void saveCredentials(DialogInterface dialog) {
		Login login = (Login) dialog;
		String username = login.getUsername();
		String password = login.getPassword();
		saveCredentials(username, password);
	}

	private void showLogin(int requestCode) {
		mRequestCode = requestCode;
		showDialog(DIALOG_LOGIN);
	}

	private static boolean isEmpty(EditText editText) {
		return editText.getText().toString().trim().length() == 0;
	}

	public void save() {
		if (isEmpty(mEditUrl)) {
			mEditUrl.requestFocus();
			error(R.string.error_url_empty);
			return;
		}
		if (isEmpty(mEditTitle)) {
			mEditTitle.requestFocus();
			error(R.string.error_title_empty);
			return;
		}
		if (!mFactory.hasCredentials()) {
			showLogin(REQUEST_CODE_LOGIN_FOR_ADD);
			return;
		}
		String url = mEditUrl.getText().toString();
		String description = mEditTitle.getText().toString();
		String extended = mEditNotes.getText().toString();
		String tags = mEditTags.getText().toString();
		boolean replace = true;
		boolean shared = !mCheckDoNotShare.isChecked();
		Runnable request = mFactory.createPostsAddRequest(url, description,
				extended, tags, replace, shared);

		showDialog(DIALOG_SAVE_PROGRESS);
		mNonConfigurationInstance.sendDeliciousRequest(request);
	}

	/**
	 * {@inheritDoc}
	 */
	public void onClick(View v) {
		if (v == mButtonSave) {
			save();
		} else if (v == mButtonDiscard) {
			finish();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			// This should be automatic, but it isn't for some reason.
			InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			IBinder windowToken = v.getWindowToken();
			manager.hideSoftInputFromWindow(windowToken, 0);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (dialog == mLoginDialog) {
				if (mRequestCode == REQUEST_CODE_LOGIN_FOR_ADD) {
					saveCredentials(dialog);
					save();
				} else if (mRequestCode == REQUEST_CODE_LOGIN_FOR_TAGS) {
					saveCredentials(dialog);
					requestUserTags(true);
					requestSuggestions();
					showProgressSpinners();
				}
			}
		} else {
			onCancel(dialog);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onCancel(DialogInterface dialog) {
		if (dialog == mLoginDialog) {
			finish();
		}
	}

	private void addTag(String tag) {
		Editable tags = mEditTags.getText();
		int length = tags.length();
		if (length != 0) {
			char last = tags.charAt(length - 1);
			if (!Character.isWhitespace(last)) {
				tags.append(' ');
			}
		}
		tags.append(tag);

		// Add a space to avoid triggering auto-complete
		tags.append(' ');
	}

	public void onTagClick(String tag) {
		// TODO: Toggle tag instead
		addTag(tag);
	}
}

class RequestHandler extends Handler {

	public RequestHandler(Looper looper) {
		super(looper);
	}

	@Override
	public void handleMessage(Message msg) {
		Runnable runnable = (Runnable) msg.obj;
		runnable.run();
	}
}

class NonConfigurationInstance implements Handler.Callback {

	private final Handler mProxy;
	private final Handler mTitleRequestHandler;
	private final Handler mDeliciousRequestHandler;

	private boolean mTitlePending;
	private boolean mTagsPending;
	private boolean mSuggestionsPending;

	private Handler mHandler;

	public NonConfigurationInstance(Looper looper) {
		super();
		Handler.Callback callback = this;
		mProxy = new Handler(looper, callback);
		mTitleRequestHandler = startHandlerThread("Title Request Handler");
		mDeliciousRequestHandler = startHandlerThread("Tags Request Handler");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean handleMessage(Message msg) {
		if (mHandler != null) {
			mHandler.handleMessage(msg);
			return true;
		} else {
			return false;
		}
	}

	private static Handler startHandlerThread(String name) {
		HandlerThread handlerThread = new HandlerThread(name);
		handlerThread.start();
		Looper looper = handlerThread.getLooper();
		return new RequestHandler(looper);
	}

	private static void stopHandlerThread(Handler handler) {
		Looper looper = handler.getLooper();
		looper.quit();
	}

	public void quit() {
		stopHandlerThread(mTitleRequestHandler);
		stopHandlerThread(mDeliciousRequestHandler);
		mHandler = null;
	}

	public boolean isTitlePending() {
		return mTitlePending;
	}

	public void setTitlePending(boolean titlePending) {
		mTitlePending = titlePending;
	}

	public boolean isTagsPending() {
		return mTagsPending;
	}

	public void setTagsPending(boolean tagsPending) {
		mTagsPending = tagsPending;
	}

	public boolean isSuggestionsPending() {
		return mSuggestionsPending;
	}

	public void setSuggestionsPending(boolean suggestionsPending) {
		mSuggestionsPending = suggestionsPending;
	}

	public void sendTitleRequest(Runnable runnable) {
		mTitleRequestHandler.obtainMessage(0, runnable).sendToTarget();
	}

	public void sendDeliciousRequest(Runnable runnable) {
		mDeliciousRequestHandler.obtainMessage(0, runnable).sendToTarget();
	}

	public Handler getProxy() {
		return mProxy;
	}

	public void setHandler(Handler handler) {
		mHandler = handler;
	}
}
