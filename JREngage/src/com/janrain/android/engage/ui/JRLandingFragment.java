/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Copyright (c) 2010, Janrain, Inc.

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
   other materials provided with the distribution.
 * Neither the name of the Janrain, Inc. nor the names of its
   contributors may be used to endorse or promote products derived from this
   software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.janrain.android.engage.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.janrain.android.engage.R;
import com.janrain.android.engage.session.JRProvider;

/**
 * @internal
 *
 * @class JRLandingFragment
 **/
public class JRLandingFragment extends JRUiFragment {
    public static final int RESULT_SWITCH_ACCOUNTS = 1;
    public static final int RESULT_RESTART = 2;

    {
        TAG = JRLandingFragment.class.getSimpleName();
    }

    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        public void onClick(View view) {
            Log.i(TAG, "[onClick] handled");

            if (view.equals(mSignInButton)) {
                onSignInClick();
            } else if (view.equals(mSwitchAccountButton)) {
                onSwitchAccountsClick();
            }
        }
    };

    private boolean mIsAlertShowing = false;
    private boolean mIsFinishPending = false;

    private ImageView mLogo;
    private EditText mUserInput;

    private TextView mWelcomeLabel;

    private ColorButton mSwitchAccountButton;
    private ColorButton mSignInButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.jr_provider_landing, container, false);

        mLogo = (ImageView)view.findViewById(R.id.jr_landing_logo);
        mUserInput = (EditText)view.findViewById(R.id.jr_landing_edit);
        mWelcomeLabel = (TextView)view.findViewById(R.id.jr_landing_welcome_label);
        mSwitchAccountButton = (ColorButton)view.findViewById(R.id.jr_landing_switch_account_button);
        mSignInButton = (ColorButton)view.findViewById(R.id.jr_landing_small_signin_button);

        prepareUserInterface();

        return view;
    }

    private void onSignInClick() {
        if (mSessionData.getCurrentlyAuthenticatingProvider().requiresInput()) {
            //TODO validate OpenID URLs so they don't hang the WebView
            String text = mUserInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                String title = getString(R.string.jr_landing_bad_user_input);
                String message = getString(R.string.jr_landing_bad_input_long);
                showAlertDialog(title, message);
            } else {
                mSessionData.getCurrentlyAuthenticatingProvider().setUserInput(text);
                showWebView();
            }
        } else {
            showWebView();
        }
    }

    private void onSwitchAccountsClick() {
        Log.i(TAG, "[onSwitchAccountsClick]");

        mSessionData.getCurrentlyAuthenticatingProvider().setForceReauth(true);
        mSessionData.setReturningBasicProvider("");
        mSessionData.triggerAuthenticationDidRestart();
        getActivity().setResult(RESULT_SWITCH_ACCOUNTS);
        getActivity().finish();
    }

    private void showAlertDialog(String title, String message) {
        mIsAlertShowing = true;
        new AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.jr_dialog_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mIsAlertShowing = false;
                    if (mIsFinishPending) {
                        mIsFinishPending = false;
                        /* If there is a pending Finish that we deferred because the dialog was displayed
                         * it is called here, now. */
                        tryToFinishActivity();
                    }
                }
            }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WEBVIEW) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                    break;
                case JRWebViewFragment.RESULT_RESTART:
                    getActivity().setResult(RESULT_RESTART);
                    getActivity().finish();
                    break;
                case JRWebViewFragment.RESULT_BAD_OPENID_URL:
                    break;
                default:
                    throw new RuntimeException("unrecognized result code: " + resultCode);
            }
        } else {
            throw new RuntimeException("unrecognized request code: " + requestCode);
        }
    }

    @Override
    protected void tryToFinishActivity() {
        Log.i(TAG, "[tryToFinishActivity]");
        if (mIsAlertShowing) {
            mIsFinishPending = true;
        } else {
            getActivity().finish();
        }
    }

    private void prepareUserInterface() {
        mSwitchAccountButton.setOnClickListener(mButtonListener);
        mSignInButton.setOnClickListener(mButtonListener);

        mSwitchAccountButton.setColor(0xffaaaaaa);
        mSignInButton.setColor(0xff1a557c);

        JRProvider currentlyAuthenticatingProvider =
                mSessionData.getCurrentlyAuthenticatingProvider();
        if (currentlyAuthenticatingProvider.getName().equals("openid")) {
            mUserInput.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI);
        }

        getActivity().setTitle(getCustomTitle());

        mLogo.setImageDrawable(currentlyAuthenticatingProvider.getProviderLogo(getActivity()));

        if (currentlyAuthenticatingProvider.getName().equals(
                mSessionData.getReturningBasicProvider())) {
            configureButtonVisibility(false);

            if (currentlyAuthenticatingProvider.requiresInput()) {
                if (Config.LOGD) Log.d(TAG, "[prepareUserInterface] current provider requires input");

                mUserInput.setVisibility(View.VISIBLE);
                mWelcomeLabel.setVisibility(View.GONE);

                String userInput = currentlyAuthenticatingProvider.getUserInput();
                if (!TextUtils.isEmpty(userInput)) {
                    mUserInput.setText(userInput);
                } else { // Will probably never happen
                    mUserInput.setText("");
                    configureButtonVisibility(true);
                }

                mUserInput.setHint(currentlyAuthenticatingProvider.getPlaceholderText());
            } else {
                if (Config.LOGD) Log.d(TAG, "[prepareUserInterface] current provider doesn't require input");

                mUserInput.setVisibility(View.GONE);
                mWelcomeLabel.setVisibility(View.VISIBLE);

                mWelcomeLabel.setText(currentlyAuthenticatingProvider.getWelcomeString());
            }
        } else {
             configureButtonVisibility(true);

            if (currentlyAuthenticatingProvider.requiresInput()) {
                if (Config.LOGD) {
                    Log.d(TAG, "[prepareUserInterface] current provider requires input"); }

                mUserInput.setVisibility(View.VISIBLE);
                mWelcomeLabel.setVisibility(View.GONE);

                String userInput = currentlyAuthenticatingProvider.getUserInput();
                if (!TextUtils.isEmpty(userInput)) {
                    mUserInput.setText(userInput);
                } else {
                    mUserInput.setText("");
                }

                mUserInput.setHint(currentlyAuthenticatingProvider.getPlaceholderText());
            } else {
                // Will never happen
            }
        }
    }

    private void configureButtonVisibility(boolean isSingleButtonLayout) {
        if (isSingleButtonLayout) {
            mSwitchAccountButton.setVisibility(View.INVISIBLE);
            mSignInButton.setVisibility(View.VISIBLE);
        } else {
            mSwitchAccountButton.setVisibility(View.VISIBLE);
            mSignInButton.setVisibility(View.VISIBLE);
        }
    }

    private String getCustomTitle() {
        JRProvider provider = mSessionData.getCurrentlyAuthenticatingProvider();
        if (provider.requiresInput()) {
            return provider.getShortText();
        } else {
            return getString(R.string.jr_landing_default_custom_title);
        }
    }
}