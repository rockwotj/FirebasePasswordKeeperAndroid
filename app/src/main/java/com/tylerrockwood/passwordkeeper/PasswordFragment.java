package com.tylerrockwood.passwordkeeper;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.client.Firebase;
import com.hudomju.swipe.OnItemClickListener;
import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.SwipeableItemClickListener;
import com.hudomju.swipe.adapter.RecyclerViewAdapter;

public class PasswordFragment extends Fragment implements Toolbar.OnMenuItemClickListener, View.OnClickListener {

    private Firebase mPasswordKeeper;
    private OnLogoutListener mListener;
    private PasswordAdapter mAdapter;

    public PasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userId = getArguments().getString(MainActivity.USER_ID);
        mPasswordKeeper = new Firebase(MainActivity.FIREBASE_URL + "/users/" + userId);
        mPasswordKeeper.keepSynced(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_password, container, false);
        // Setup Toolbar
        Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.app_name);
        getActivity().getMenuInflater().inflate(R.menu.main, mToolbar.getMenu());
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        //Recycler View
        RecyclerView passwordList = (RecyclerView) rootView.findViewById(R.id.password_list);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        passwordList.setLayoutManager(manager);
        mAdapter = new PasswordAdapter(getActivity(), mPasswordKeeper);
        passwordList.setAdapter(mAdapter);
        final SwipeToDismissTouchListener touchListener = new SwipeToDismissTouchListener(new RecyclerViewAdapter(passwordList), mAdapter);
        passwordList.setOnTouchListener(touchListener);
        passwordList.addOnScrollListener((RecyclerView.OnScrollListener) touchListener.makeScrollListener());
        passwordList.addOnItemTouchListener(new SwipeableItemClickListener(getActivity(),
                new OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (view.getId() == R.id.txt_delete) {
                            touchListener.processPendingDismisses();
                        } else if (view.getId() == R.id.txt_undo) {
                            touchListener.undoPendingDismiss();
                        } else if (view.getId() == R.id.edit_button) {
                            editPasswordAtPosition(position);
                        } else {
                            mAdapter.toggleCard(position);
                        }
                    }
                }));
        rootView.findViewById(R.id.fab_add).setOnClickListener(this);
        return rootView;
    }

    private void editPasswordAtPosition(int position) {
        final Password password = mAdapter.get(position);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View contentView = inflater.inflate(R.layout.dialog_insert, null);
        final EditText serviceView = (EditText) contentView.findViewById(R.id.service);
        final EditText usernameView = (EditText) contentView.findViewById(R.id.username);
        final EditText passwordView = (EditText) contentView.findViewById(R.id.password);
        passwordView.setImeActionLabel("Save", EditorInfo.IME_NULL);
        serviceView.setText(password.getService());
        usernameView.setText(password.getUsername());
        passwordView.setText(password.getPassword());
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.edit_password_title)
                .customView(contentView, true)
                .negativeText(android.R.string.cancel)
                .positiveText(R.string.save)
                .widgetColorRes(R.color.primary)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mAdapter.update(password.getKey(),
                                serviceView.getText().toString(),
                                usernameView.getText().toString(),
                                passwordView.getText().toString());
                    }
                })
                .build();
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_NULL) {
                    mAdapter.update(password.getKey(),
                            serviceView.getText().toString(),
                            usernameView.getText().toString(),
                            passwordView.getText().toString());
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_logout:
                Log.d("PK", "LOGOUT Menu Item Clicked!");
                mPasswordKeeper.unauth();
                mListener.onLogout();
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View contentView = inflater.inflate(R.layout.dialog_insert, null);
        final EditText serviceView = (EditText) contentView.findViewById(R.id.service);
        final EditText usernameView = (EditText) contentView.findViewById(R.id.username);
        final EditText passwordView = (EditText) contentView.findViewById(R.id.password);
        passwordView.setImeActionLabel("Create", EditorInfo.IME_NULL);
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.add_password_title)
                .customView(contentView, true)
                .negativeText(android.R.string.cancel)
                .positiveText(R.string.add)
                .widgetColorRes(R.color.primary)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mAdapter.add(serviceView.getText().toString(),
                                usernameView.getText().toString(),
                                passwordView.getText().toString());
                    }
                }).build();
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if(id == EditorInfo.IME_NULL) {
                    mAdapter.add(serviceView.getText().toString(),
                            usernameView.getText().toString(),
                            passwordView.getText().toString());
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    public interface OnLogoutListener {
        void onLogout();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLogoutListener) activity;
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
