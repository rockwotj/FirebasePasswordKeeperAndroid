package com.tylerrockwood.passwordkeeper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public class MainActivity extends AppCompatActivity implements LoginFragment.OnLoginListener, Firebase.AuthResultHandler, PasswordFragment.OnLogoutListener {

    public static final String FIREBASE_REPO = "<YOUR USERNAME HERE>-passwordkeeper";
    public static final String FIREBASE_WEBSITE = "https://" + FIREBASE_REPO + ".firebaseapp.com/";
    public static final String FIREBASE_URL = "https://" + FIREBASE_REPO + ".firebaseio.com/";
    public static final String USER_ID = "USER_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);
        Firebase passwordKeeperRef = new Firebase(FIREBASE_URL);
        if (passwordKeeperRef.getAuth() == null || authExpired(passwordKeeperRef.getAuth())){
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

    private boolean authExpired(AuthData auth) {
        long timeWhenExpired = auth.getExpires() * 1000L;
        long currentTime = System.currentTimeMillis();
        return currentTime > timeWhenExpired;
    }

    @Override
    public void onAuthenticated(AuthData authData) {
        String userId = authData.getUid();
        switchToPasswordFragment(userId);
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        LoginFragment loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag("Login");
        loginFragment.onLoginError(firebaseError.getMessage());
    }

    @Override
    public void onLogin(String email, String password) {
        Firebase ref = new Firebase(FIREBASE_URL);
        ref.authWithPassword(email, password, this);
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
}
