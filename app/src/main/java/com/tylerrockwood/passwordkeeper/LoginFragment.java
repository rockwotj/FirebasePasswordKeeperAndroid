package com.tylerrockwood.passwordkeeper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.SignInButton;

import java.net.URI;


/**
 * A placeholder fragment containing a simple view.
 */
public class LoginFragment extends Fragment {

    private EditText mPasswordView;
    private EditText mEmailView;
    private View mLoginForm;
    private View mProgressSpinner;
    private boolean mLoggingIn;
    private OnLoginListener mListener;
    private SignInButton mGoogleSignInButton;

    public LoginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoggingIn = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        mEmailView = (EditText) rootView.findViewById(R.id.email);
        mPasswordView = (EditText) rootView.findViewById(R.id.password);
        mLoginForm = rootView.findViewById(R.id.login_form);
        mProgressSpinner = rootView.findViewById(R.id.login_progress);
        View loginButton = rootView.findViewById(R.id.email_sign_in_button);
        mGoogleSignInButton = (SignInButton) rootView.findViewById(R.id.google_sign_in_button);
        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_NEXT) {
                    mPasswordView.requestFocus();
                    return true;
                }
                return false;
            }
        });
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_NULL) {
                    login();
                    return true;
                }
                return false;
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        mGoogleSignInButton.setColorScheme(SignInButton.COLOR_LIGHT);
        mGoogleSignInButton.setSize(SignInButton.SIZE_WIDE);
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithGoogle();
            }
        });
        return rootView;
    }

    private void loginWithGoogle() {
        if (mLoggingIn) {
            return;
        }
        mEmailView.setError(null);
        mPasswordView.setError(null);

        showProgress(true);
        mLoggingIn = true;
        mListener.onGoogleLogin();
        hideKeyboard();
    }


    public void login() {
        if (mLoggingIn) {
            return;
        }

        mEmailView.setError(null);
        mPasswordView.setError(null);

        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancelLogin = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.invalid_password));
            focusView = mPasswordView;
            cancelLogin = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.field_required));
            focusView = mEmailView;
            cancelLogin = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.invalid_email));
            focusView = mEmailView;
            cancelLogin = true;
        }

        if (cancelLogin) {
            // error in login
            focusView.requestFocus();
        } else {
            // show progress spinner, and start background task to login
            showProgress(true);
            mLoggingIn = true;
            mListener.onLogin(email, password);
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), 0);
    }

    public void onLoginError(String message) {
        new MaterialDialog.Builder(getActivity())
                .title(getActivity().getString(R.string.login_error))
                .content(message)
                .positiveText(android.R.string.ok)
                .positiveColorRes(R.color.primary)
                .show();
        showProgress(false);
        mLoggingIn = false;
    }

    private void showProgress(boolean show) {
        mProgressSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mGoogleSignInButton.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    public interface OnLoginListener {
        void onLogin(String email, String password);
        void onGoogleLogin();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLoginListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
