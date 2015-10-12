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

public class MainActivity extends AppCompatActivity implements LoginFragment.OnLoginListener, PasswordFragment.OnLogoutListener, Firebase.AuthResultHandler, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String FIREBASE_REPO = "passwordkeeper";
    public static final String FIREBASE_WEBSITE = "https://" + FIREBASE_REPO + ".firebaseapp.com/";
    public static final String FIREBASE_URL = "https://" + FIREBASE_REPO + ".firebaseio.com/";
    public static final String FIREBASE = "FIREBASE";
    private static final int RC_GOOGLE_LOGIN = 1;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            Firebase.setAndroidContext(this);
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        // Path doesn't matter for auth
        Firebase firebase = new Firebase(FIREBASE_URL);
        if (firebase.getAuth() != null && !isExpired(firebase.getAuth())) {
            // Done: Use uid for user's data
            switchToPasswordFragment(FIREBASE_URL + "/users/" + firebase.getAuth().getUid());
        } else {
            switchToLoginFragment();
        }
    }

    private boolean isExpired(AuthData auth) {
        // getExpires returns second since the epoch
        return (auth.getExpires() * 1000) < System.currentTimeMillis();
    }

    @Override
    public void onLogin(String email, String password) {
        //Done: Log user in with username & password
        Firebase firebase = new Firebase(FIREBASE_URL);
        firebase.authWithPassword(email, password, this);
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        switchToPasswordFragment(FIREBASE_URL + "/users/" + authData.getUid());
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        showLoginError(firebaseError.getMessage());
    }

    @Override
    public void onGoogleLogin() {
        Log.d("FPK", "onGoogleLogin");
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else {
            this.getGoogleOAuthToken();
        }
    }

    private void onGoogleLogin(String oAuthToken) {
        //Done: Log user in with OAuth Token
        Firebase firebase = new Firebase(FIREBASE_URL);
        firebase.authWithOAuthToken("google", oAuthToken, this);
        Log.d("FPK", "onGoogleLoginWithToken");
    }


    @Override
    public void onLogout() {
        //Done: Log user out
        Firebase firebase = new Firebase(FIREBASE_URL);
        firebase.unauth();
        switchToLoginFragment();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("FPK", "onConnected");
        getGoogleOAuthToken();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Do Nothing
        Log.d("FPK", "onConnecitonSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("FPK", "onConnectionFailed");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RC_GOOGLE_LOGIN);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_GOOGLE_LOGIN && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        } else if (requestCode == RC_GOOGLE_LOGIN) {
            showLoginError("You must accept the consent screen!");
        }
    }

    // MARK: Provided Helper Methods
    private void switchToLoginFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, new LoginFragment(), "Login");
        ft.commit();
    }

    private void switchToPasswordFragment(String repoUrl) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment passwordFragment = new PasswordFragment();
        Bundle args = new Bundle();
        args.putString(FIREBASE, repoUrl);
        passwordFragment.setArguments(args);
        ft.replace(R.id.fragment, passwordFragment, "Passwords");
        ft.commit();
    }

    private void showLoginError(String message) {
        LoginFragment loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag("Login");
        loginFragment.onLoginError(message);
    }

    private void getGoogleOAuthToken() {
        Log.d("FPK", "getOAuth");
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
                if (token != null) {
                    onGoogleLogin(token);
                } else if (errorMessage != null) {
                    showLoginError(errorMessage);
                }
            }
        };
        task.execute();
    }
}
