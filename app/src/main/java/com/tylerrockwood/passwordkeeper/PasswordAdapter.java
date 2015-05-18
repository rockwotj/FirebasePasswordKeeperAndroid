package com.tylerrockwood.passwordkeeper;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.adapter.ViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordView> implements ChildEventListener, SwipeToDismissTouchListener.DismissCallbacks {

    private final LayoutInflater mInflator;
    private final Firebase mPasswordKeeper;
    private final List<Password> mPasswords;
    private final Random mRandom;
    private final int mExpandedHalfHeight;
    private final int mCollapsedHeight;
    private final ArrayList<PasswordView> mCards;
    private final int mExpandedFullHeight;

    public PasswordAdapter(Context context, Firebase firebaseRef) {
        mPasswords = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
        mPasswordKeeper = firebaseRef;
        mPasswordKeeper.addChildEventListener(this);
        mRandom = new Random();
        Resources r = context.getResources();
        mCollapsedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, r.getDisplayMetrics());
        mExpandedFullHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 176, r.getDisplayMetrics());
        mExpandedHalfHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 124, r.getDisplayMetrics());
        mCards = new ArrayList<>();
    }

    @Override
    public PasswordView onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflator.inflate(R.layout.view_list_password, parent, false);
        PasswordView holder =  new PasswordView(view);
        mCards.add(holder);
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

    public void add(String service, String username, String password) {
        Map<String, Object> json = new HashMap<>(3);
        json.put("service", service);
        if (!username.isEmpty())
            json.put("username", username);
        json.put("password", password);
        mPasswordKeeper.push().setValue(json);
    }

    public void update(String key, String service, String username, String password) {
        Map<String, Object> json = new HashMap<>(3);
        json.put("service", service);
        if (!username.isEmpty())
            json.put("username", username);
        json.put("password", password);
        mPasswordKeeper.child(key).updateChildren(json);
    }

    private void remove(int position) {
        String key = get(position).getKey();
        mPasswordKeeper.child(key).removeValue();
        remove(key);
    }

    private void remove(String key) {
        int i;
        for(i = 0; i < mPasswords.size(); i++) {
            Password pw = mPasswords.get(i);
            if(key.equals(pw.getKey())) {
                mPasswords.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public Password get(int position) {
        return mPasswords.get(position);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
        String key = dataSnapshot.getKey();
        String service = dataSnapshot.child("service").getValue(String.class);
        String username = dataSnapshot.child("username").getValue(String.class);
        String password = dataSnapshot.child("password").getValue(String.class);
        mPasswords.add(0, new Password(key, username, password, service));
        notifyItemInserted(0);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String previousChild) {
        String service = dataSnapshot.child("service").getValue(String.class);
        String username = dataSnapshot.child("username").getValue(String.class);
        String password = dataSnapshot.child("password").getValue(String.class);
        int i;
        for(i = 0; i < mPasswords.size(); i++) {
            Password pw = mPasswords.get(i);
            if(dataSnapshot.getKey().equals(pw.getKey())) {
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

    private static final int[] mLockImages = new int[] {
            R.mipmap.ic_lock_cyan,
            R.mipmap.ic_lock_orange,
            R.mipmap.ic_lock_green,
            R.mipmap.ic_lock_purple,
            R.mipmap.ic_lock_pink,
            R.mipmap.ic_lock_red
    };

    @Override
    public boolean canDismiss(int position) {
        return true;
    }

    @Override
    public void onDismiss(ViewAdapter viewAdapter, int position) {
        Log.d("PK", "Password Removed!");
        this.remove(position);
    }


    public void toggleCard(int position) {
        for (PasswordView card : mCards) {
            card.toggle(position);
        }
    }

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
        }

        public void bindToView(Password password) {
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
        }

        public void toggle(int position) {
            if (position == getLayoutPosition()) {
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
