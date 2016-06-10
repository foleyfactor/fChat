package com.foley.alex.fchat;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.channels.GatheringByteChannel;
import java.sql.DatabaseMetaData;
import java.util.Date;
import java.util.HashMap;
import java.util.PropertyResourceBundle;
import java.util.prefs.Preferences;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final String PREFS_NAME = "fChatPrefs";

    private FirebaseUser user;
    private boolean loginShowing = false;

    //Method called on the creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean deltaTime = (new Date().getTime()) - getLastTime() > 300000f;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showLoginDialog();
        } else {
            if (deltaTime) {
               logOutAndShowDialog();
            } else {
                user = FirebaseAuth.getInstance().getCurrentUser();
                loadChats();
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showJoinChatDialog();
            }
        });
    }

    //Method called when the activity is restarted (when back is pressed or when app is running in
    //background and then resumed)
    @Override
    protected void onRestart() {
        super.onRestart();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showLoginDialog();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            loadChats();
        }
    }

    //Method called when the app is stopped
    @Override
    protected void onStop() {
        super.onStop();

        setLastTime();
    }

    //Method for adding a user to a specified chat
    public void addUserToChat(String c) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        final ProgressDialog wait = makeLoadingSpinner("Joining chat: " + c, "Just a moment.");
        wait.show();
        ref.child(c).setValue("").addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                wait.cancel();
                loadChats();
            }
        });
    }

    //Function returning the last time that the application was active
    public long getLastTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong("time", 0);
    }

    //Method for setting the last time that the app was active
    public void setLastTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("time", new Date().getTime());

        editor.commit();
    }

    //Returns this activity as an activity (for functions within functions)
    private Activity getActivity() {
        return this;
    }

    //Returns this activity as a HomeActivity object (for functions within functions)
    private HomeActivity getHomeActivity() {
        return this;
    }

    //Method for transitioning to a chat's activity
    public void toChat(String chatId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat", chatId);
        startActivity(intent);
    }

    //Method for loading all of the chats that a user is a part of
    public void loadChats() {
        String uid = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/" + uid);
        final ProgressDialog loading = makeLoadingSpinner("Loading chats", "This will just take a moment.");
        loading.show();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "Data changed.");
                ScrollView chats = (ScrollView) getActivity().findViewById(R.id.chats);
                if (!dataSnapshot.hasChildren()) {
                    TextView noChats = new TextView(getActivity());
                    noChats.setText(R.string.no_chats);
                    noChats.setTextSize(20);
                    noChats.setGravity(Gravity.CENTER);

                    chats.removeAllViews();
                    chats.addView(noChats);
                } else {
                    final LinearLayout layout = new LinearLayout(chats.getContext());
                    layout.setOrientation(LinearLayout.VERTICAL);

                    Iterable<DataSnapshot> chatList = dataSnapshot.getChildren();

                    for (DataSnapshot d : chatList) {
                        final String chatId = d.getKey();
                        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats");
                        chatRef.child(chatId).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Chat c = dataSnapshot.getValue(Chat.class);
                                        c.drawChatBlurb(layout, getHomeActivity(), chatId);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.e(TAG, "LoadChats: ChatBlurb: error");
                                    }
                                }
                        );
                    }

                    chats.removeAllViews();
                    chats.addView(layout);
                }
                loading.cancel();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "LoadChats:error");
            }
        });
    }

    //Function for creating a loading spinner (waiting for async)
    public ProgressDialog makeLoadingSpinner(CharSequence t, CharSequence m) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setTitle(t);
        loading.setMessage(m);
        loading.setCancelable(false);
        return loading;
    }

    //Method for showing the dialog prompting a user to create a chat
    public void showCreateChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.create_chat_dialog, null);
        builder.setView(dialogView)
                .setPositiveButton(R.string.create_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String chatTitle = ((TextView) dialogView.findViewById(R.id.chat_title)).getText().toString();
                        final String chatId = ((TextView) dialogView.findViewById(R.id.chat_id)).getText().toString().toLowerCase();
                        final ProgressDialog wait = makeLoadingSpinner("Creating chat", "Preparing your chat from scratch");
                        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats");
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.hasChild(chatId)) {
                                    //if the chat id doesn't exist, create one!
                                    final Chat c = new Chat(chatId, chatTitle);
                                    ref.child(chatId).setValue(c).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            wait.cancel();
                                            //c.sendMessage(new Message("Created chat", user.getDisplayName(), new Date().getTime(), user.getUid()));
                                            addUserToChat(chatId);
                                        }
                                    });
                                } else {
                                    wait.cancel();
                                    Toast.makeText(getApplicationContext(), "Error: a chat with that id already exists.", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "CreateChat: error");
                            }
                        });
                    }
                })
                .create().show();
    }

    //Method for showing the dialog prompting the user to join a chat
    public void showJoinChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.join_chat_dialog, null);
        builder.setView(dialogView)
                .setPositiveButton(R.string.join_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String chatId = ((TextView) dialogView.findViewById(R.id.chat_id)).getText().toString().toLowerCase();
                        final ProgressDialog wait = makeLoadingSpinner("Looking for chat: " + chatId, "Please be patient.");
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats");
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (!dataSnapshot.hasChild(chatId)) {
                                            //if there doesn't exist a chat with that
                                            //id, tell the user
                                            Toast.makeText(getApplicationContext(), "Error: no chat found.", Toast.LENGTH_SHORT)
                                                    .show();
                                        } else {
                                            addUserToChat(chatId);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.e(TAG, "JoinChat: error");
                                    }
                                });
                    }
                })
                .setNegativeButton(R.string.create_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        showCreateChatDialog();
                    }
                })
                .create().show();
    }

    //Method for showing the dialog prompting the user to login
    public void showLoginDialog() {
        if (!loginShowing) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.login_dialog, null);
            builder.setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String email = ((EditText) dialogView.findViewById(R.id.email)).getText().toString();
                            String password = ((EditText) dialogView.findViewById(R.id.pass)).getText().toString();
                            dialog.cancel();
                            loginShowing = false;
                            final ProgressDialog wait = makeLoadingSpinner("Logging in", "Please wait.");
                            wait.show();
                            try {
                                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        wait.cancel();
                                        if (!task.isSuccessful()) {
                                            showLoginDialog();
                                        } else {
                                            user = FirebaseAuth.getInstance().getCurrentUser();
                                            loadChats();
                                        }
                                    }
                                });
                            } catch (IllegalArgumentException e) {
                                wait.cancel();
                                showLoginDialog();
                            }
                        }
                    })
                    .setNegativeButton(R.string.signup, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            loginShowing = false;
                            showSignUpDialog();
                        }
                    });
            loginShowing = true;
            builder.create().show();
        }
    }

    //Method for showing the dialog prompting the user to sign up
    public void showSignUpDialog() {
        if (!loginShowing) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.sign_up_dialog, null);
            builder.setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.signup, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            loginShowing = false;
                            final String username = ((EditText) dialogView.findViewById(R.id.user)).getText().toString();
                            String email = ((EditText) dialogView.findViewById(R.id.email)).getText().toString();
                            String password = ((EditText) dialogView.findViewById(R.id.pass)).getText().toString();
                            final ProgressDialog wait = makeLoadingSpinner("Signing up", "Just a moment.");
                            wait.show();
                            try {
                                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (!task.isSuccessful()) {
                                            wait.cancel();
                                            showSignUpDialog();
                                        } else {
                                            user = FirebaseAuth.getInstance().getCurrentUser();
                                            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(username)
                                                    .build();
                                            user.updateProfile(profile).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    wait.cancel();
                                                    loadChats();
                                                }
                                            });
                                        }
                                    }
                                });
                            } catch (IllegalArgumentException e) {
                                wait.cancel();
                                showSignUpDialog();
                            }
                        }
                    })
                    .setNegativeButton(R.string.login, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            loginShowing = false;
                            showLoginDialog();
                        }
                    });
            loginShowing = true;
            builder.create().show();
        }
    }

    //Method that creates the action bar's option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    //Method called when an item on the action bar is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.logout_option) {
            logOutAndShowDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    //Method that logs the user out and shows them a dialog
    public void logOutAndShowDialog() {
        ((ScrollView) findViewById(R.id.chats)).removeAllViews();
        FirebaseAuth.getInstance().signOut();
        showLoginDialog();
    }
}
