package com.tylerrockwood.passwordkeeper;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordView> implements ChildEventListener {

    private final LayoutInflater mInflator;
    private final Firebase mPasswordKeeper;
    private final List<Password> mPasswords;
    private final Random mRandom;
    private final int mExpandedHalfHeight;
    private final int mCollapsedHeight;
    private final int mExpandedFullHeight;

    public PasswordAdapter(Context context, Firebase firebaseRef) {
        mPasswords = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
        mPasswordKeeper = firebaseRef;
        mRandom = new Random();
        Resources r = context.getResources();
        mCollapsedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, r.getDisplayMetrics());
        mExpandedFullHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 176, r.getDisplayMetrics());
        mExpandedHalfHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 124, r.getDisplayMetrics());
    }

    @Override
    public PasswordView onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflator.inflate(R.layout.view_list_password, parent, false);
        PasswordView holder = new PasswordView(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(PasswordView holder, int position) {
        holder.bindToView(mPasswords.get(position));
    }

    @Override
    public int getItemCount() {
        return mPasswords.size();
    }

    public void add(Password pw) {
        mPasswordKeeper.push().setValue(pw);
    }

    public void update(Password pw) {
        mPasswordKeeper.child(pw.getKey()).setValue(pw);
    }

    public void delete(Password password) {
        mPasswordKeeper.child(password.getKey()).removeValue();
    }

    public void insert(Password password, int position) {
        mPasswords.add(position, password);
        notifyItemInserted(position);
    }

    public Password hide(int position) {
        Password pw = mPasswords.remove(position);
        notifyItemRemoved(position);
        return pw;
    }

    private Password remove(String key) {
        for (int i = 0; i < mPasswords.size(); i++) {
            Password pw = mPasswords.get(i);
            if (key.equals(pw.getKey())) {
                mPasswords.remove(i);
                notifyItemRemoved(i);
                return pw;
            }
        }
        return null;
    }

    public void clear() {
        mPasswords.clear();
    }

    public Password get(int position) {
        return mPasswords.get(position);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
        String key = dataSnapshot.getKey();
        if (key.equals("users")) return;
        Password pw = dataSnapshot.getValue(Password.class);
        pw.setKey(key);
        mPasswords.add(0, pw);
        notifyItemInserted(0);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String previousChild) {
        String service = dataSnapshot.child("service").getValue(String.class);
        String username = dataSnapshot.child("username").getValue(String.class);
        String password = dataSnapshot.child("password").getValue(String.class);
        int i;
        for (i = 0; i < mPasswords.size(); i++) {
            Password pw = mPasswords.get(i);
            if (dataSnapshot.getKey().equals(pw.getKey())) {
                pw.setService(service);
                pw.setUsername(username);
                pw.setPassword(password);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        String key = dataSnapshot.getKey();
        remove(key);
    }

    private static final int[] mLockImages = new int[]{
            R.mipmap.ic_lock_cyan,
            R.mipmap.ic_lock_orange,
            R.mipmap.ic_lock_green,
            R.mipmap.ic_lock_purple,
            R.mipmap.ic_lock_pink,
            R.mipmap.ic_lock_red
    };

    public class PasswordView extends RecyclerView.ViewHolder {

        private final TextView mServiceView;
        private final View mCard;
        private final TextView mUsernameView;
        private final TextView mPasswordView;
        private final View mUsernameCaptionView;
        private final View mEditButton;
        private boolean mToggled;

        public PasswordView(View itemView) {
            super(itemView);
            int index = mRandom.nextInt(mLockImages.length);
            ImageView image = (ImageView) itemView.findViewById(R.id.lock_icon);
            image.setImageResource(mLockImages[index]);
            mServiceView = (TextView) itemView.findViewById(R.id.service_name);
            mUsernameView = (TextView) itemView.findViewById(R.id.username);
            mUsernameCaptionView = itemView.findViewById(R.id.username_caption);
            mPasswordView = (TextView) itemView.findViewById(R.id.password);
            mEditButton = itemView.findViewById(R.id.edit_button);
            mToggled = false;
            mCard = itemView.findViewById(R.id.lyt_container);
            mCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggle();
                }
            });
        }

        public void bindToView(final Password password) {
            mServiceView.setText(password.getService());
            if (password.getUsername() != null) {
                mUsernameView.setText(password.getUsername());
                mUsernameView.setVisibility(View.VISIBLE);
                mUsernameCaptionView.setVisibility(View.VISIBLE);
            } else {
                mUsernameView.setVisibility(View.GONE);
                mUsernameCaptionView.setVisibility(View.GONE);
            }
            mPasswordView.setText(password.getPassword());
            mEditButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final View contentView = mInflator.inflate(R.layout.dialog_insert, null);
                    final EditText serviceView = (EditText) contentView.findViewById(R.id.service);
                    final EditText usernameView = (EditText) contentView.findViewById(R.id.username);
                    final EditText passwordView = (EditText) contentView.findViewById(R.id.password);
                    passwordView.setImeActionLabel("Save", EditorInfo.IME_NULL);
                    serviceView.setText(password.getService());
                    usernameView.setText(password.getUsername());
                    passwordView.setText(password.getPassword());
                    final MaterialDialog dialog = new MaterialDialog.Builder(mInflator.getContext())
                            .title(R.string.edit_password_title)
                            .customView(contentView, true)
                            .negativeText(android.R.string.cancel)
                            .positiveText(R.string.save)
                            .widgetColorRes(R.color.primary)
                            .negativeColorRes(R.color.primary)
                            .positiveColorRes(R.color.primary)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    password.setService(serviceView.getText().toString());
                                    password.setPassword(passwordView.getText().toString());
                                    String username = usernameView.getText().toString();
                                    password.setUsername(username.isEmpty() ? null : username);
                                    update(password);
                                }
                            })
                            .build();
                    passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                            if (id == EditorInfo.IME_NULL) {
                                password.setService(serviceView.getText().toString());
                                password.setPassword(passwordView.getText().toString());
                                String username = usernameView.getText().toString();
                                password.setUsername(username.isEmpty() ? null : username);
                                update(password);
                                dialog.dismiss();
                                return true;
                            }
                            return false;
                        }
                    });
                    dialog.show();
                }
            });
        }

        public void toggle() {
            mToggled = !mToggled;
            Animation toggleAnimation = new ToggleAnimation(mCard, mToggled, mUsernameView.getVisibility() == View.VISIBLE);
            toggleAnimation.setDuration(750);
            toggleAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (mToggled) {
                        mEditButton.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!mToggled) {
                        mEditButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mCard.startAnimation(toggleAnimation);
        }

    }

    public class ToggleAnimation extends Animation {
        private final View mView;
        private final int mEndHeight;
        private final int mStartHeight;

        public ToggleAnimation(View view, boolean toggled, boolean fullExapand) {
            this.mView = view;
            int expandedHeight = fullExapand ? mExpandedFullHeight : mExpandedHalfHeight;
            mStartHeight = toggled ? mCollapsedHeight : expandedHeight;
            mEndHeight = toggled ? expandedHeight : mCollapsedHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            mView.getLayoutParams().height = (int) ((mEndHeight - mStartHeight) * interpolatedTime) + mStartHeight;
            mView.requestLayout();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        //Do nothing
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        //Do nothing
    }
}
