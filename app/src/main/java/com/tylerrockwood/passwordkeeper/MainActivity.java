package com.tylerrockwood.passwordkeeper;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements LoginFragment.OnLoginListener, Firebase.AuthResultHandler, PasswordFragment.OnLogoutListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String FIREBASE_REPO = "passwordkeeper";
    public static final String FIREBASE_WEBSITE = "https://" + FIREBASE_REPO + ".firebaseapp.com/";
    public static final String FIREBASE_URL = "https://" + FIREBASE_REPO + ".firebaseio.com/";
    public static final int RC_GOOGLE_LOGIN = 1;
    public static final String USER_ID = "USER_ID";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            Firebase.setAndroidContext(this);
            if (!Firebase.getDefaultConfig().isPersistenceEnabled()) {
                Firebase.getDefaultConfig().setPersistenceEnabled(true);
            }
        }
        Firebase passwordKeeperRef = new Firebase(FIREBASE_URL);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        if (passwordKeeperRef.getAuth() == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment, new LoginFragment(), "Login");
            ft.commit();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment passwordFragment = new PasswordFragment();
            Bundle args = new Bundle();
            args.putString(USER_ID, passwordKeeperRef.getAuth().getUid());
            passwordFragment.setArguments(args);
            ft.add(R.id.fragment, passwordFragment, "Passwords");
            ft.commit();
        }
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        String userId = authData.getUid();
        switchToPasswordFragment(userId);
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        onLoginError(firebaseError.getMessage());
    }

    @Override
    public void onLogin(String email, String password) {
        Firebase ref = new Firebase(FIREBASE_URL);
        ref.authWithPassword(email, password, this);
    }

    @Override
    public void onGoogleLogin() {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else {
            this.getGoogleOAuthToken();
        }

    }

    private void onLoginError(String message) {
        LoginFragment loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag("Login");
        loginFragment.onLoginError(message);
    }

    @Override
    public void onLogout() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, new LoginFragment(), "Login");
        ft.commit();
    }

    private void switchToPasswordFragment(String userId) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment passwordFragment = new PasswordFragment();
        Bundle args = new Bundle();
        args.putString(USER_ID, userId);
        passwordFragment.setArguments(args);
        ft.replace(R.id.fragment, passwordFragment, "Passwords");
        ft.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_GOOGLE_LOGIN && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("FPK", "onConnected");
        getGoogleOAuthToken();
    }

    private void getGoogleOAuthToken() {
        final Firebase ref = new Firebase(FIREBASE_URL);
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;

            @Override
            protected String doInBackground(Void... params) {
                String token = null;
                try {
                    String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
                    token = GoogleAuthUtil.getToken(MainActivity.this, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    Intent recover = e.getIntent();
                    startActivityForResult(recover, MainActivity.RC_GOOGLE_LOGIN);
                } catch (GoogleAuthException authEx) {
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }
                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                Log.d("FPK", "onConnected");
                if (token != null) {
                    ref.authWithOAuthToken("google", token, MainActivity.this);
                } else if (errorMessage != null) {
                    onLoginError(errorMessage);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("FPK", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("FPK", "onConnectionFailed");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, MainActivity.RC_GOOGLE_LOGIN);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        }
    }


}
