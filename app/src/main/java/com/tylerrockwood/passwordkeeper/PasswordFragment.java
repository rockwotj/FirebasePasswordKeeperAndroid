package com.tylerrockwood.passwordkeeper;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar.Callback;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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
        String firebaseUrl = getArguments().getString(MainActivity.FIREBASE);
        mPasswordKeeper = new Firebase(firebaseUrl);
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
        final View fab = rootView.findViewById(R.id.fab_add);
        fab.setOnClickListener(this);
        //Recycler View
        RecyclerView passwordList = (RecyclerView) rootView.findViewById(R.id.password_list);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        passwordList.setLayoutManager(manager);
        mAdapter = new PasswordAdapter(getActivity(), mPasswordKeeper);
        passwordList.setAdapter(mAdapter);
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped item from list and notify the RecyclerView
                final int position = viewHolder.getAdapterPosition();
                final Password password = mAdapter.hide(position);
                final Snackbar snackbar = Snackbar
                        .make(fab, "Password removed!", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mAdapter.insert(password, position);
                                Snackbar snackbar1 = Snackbar.make(fab, "Password restored!", Snackbar.LENGTH_SHORT);
                                snackbar1.show();
                            }
                        })
                        .setCallback(new Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (event != Callback.DISMISS_EVENT_ACTION && event != Callback.DISMISS_EVENT_CONSECUTIVE) {
                                    mAdapter.delete(password);
                                }
                            }
                        });

                snackbar.show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(passwordList);

        return rootView;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_logout:
                Log.d("PK", "LOGOUT Menu Item Clicked!");
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
                .negativeColorRes(R.color.primary)
                .positiveColorRes(R.color.primary)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Password pw = new Password(
                                null,
                                passwordView.getText().toString(),
                                serviceView.getText().toString());
                        String username = usernameView.getText().toString();
                        pw.setUsername(username.isEmpty() ? null : username);
                        mAdapter.add(pw);
                    }
                }).build();
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_NULL) {
                    Password pw = new Password(
                            null,
                            passwordView.getText().toString(),
                            serviceView.getText().toString());
                    String username = usernameView.getText().toString();
                    pw.setUsername(username.isEmpty() ? null : username);
                    mAdapter.add(pw);
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
    public void onPause() {
        super.onPause();
        mPasswordKeeper.removeEventListener(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.clear();
        mPasswordKeeper.addChildEventListener(mAdapter);
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
