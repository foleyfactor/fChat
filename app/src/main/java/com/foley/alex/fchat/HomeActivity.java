package com.foley.alex.fchat;

//Imports
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

//Class declaration
public class HomeActivity extends AppCompatActivity {

    //Constant class variables
    private static final String TAG = "HomeActivity";
    private static final String PREFS_NAME = "fChatPrefs";

    //Class variables
    private FirebaseUser user;
    private boolean loginShowing = false;

    //Method called on the creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Call the superclass's on create method (resumes the app if necessary)
        super.onCreate(savedInstanceState);

        //Set the current view to be the activity home layout
        setContentView(R.layout.activity_home);

        //Initialize the toolbar and set it as the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Boolean to see if the user has been away from the app for more than 5 minutes
        boolean deltaTime = (new Date().getTime()) - getLastTime() > 300000f;

        //Check if the user is currently logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            //If they aren't, show the login dialog
            showLoginDialog();
        } else {
            //If they are, but have been away for too long, log them out and show the dialog
            if (deltaTime) {
               logOutAndShowDialog();
            //Otherwise, set the user and load the chats
            } else {
                user = FirebaseAuth.getInstance().getCurrentUser();
                loadChats();
            }
        }

        //Initialize the floating action button (+ button) and set its function
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
        //Call the superclass's onRestart method
        super.onRestart();

        //If the user is no longer logged in, show the login dialog
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showLoginDialog();
        } else {
        //Otherwise, set the user and load the chats
            user = FirebaseAuth.getInstance().getCurrentUser();
            loadChats();
        }
    }

    //Method called when the app is stopped
    @Override
    protected void onStop() {
        //Call the superclass's onStop method
        super.onStop();

        //Update the last time the app was used
        setLastTime();
    }

    //Method for adding a user to a specified chat
    public void addUserToChat(String c) {
        //Get the database reference for the specified chat
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());

        //Show the spinner while we wait for the request
        final ProgressDialog wait = makeLoadingSpinner("Joining chat: " + c, "Just a moment.");
        wait.show();

        //Add the chat's id to the user's set of chats
        ref.child(c).setValue("").addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //When this is done, cancel the spinner and load the chats
                wait.cancel();
                loadChats();
            }
        });
    }

    //Function returning the last time that the application was active
    public long getLastTime() {
        //Access the app's local storage
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);

        //Return the value set for time, or 0 if it doesn't exist
        return prefs.getLong("time", 0);
    }

    //Method for setting the last time that the app was active
    public void setLastTime() {
        //Access the app's local storage
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);

        //Create a local storage editor
        SharedPreferences.Editor editor = prefs.edit();

        //Set the value of time to the current time
        editor.putLong("time", new Date().getTime());

        //Commit the changes to the storage
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
        //Create a new intent, pointing to the ChatActivity class
        Intent intent = new Intent(this, ChatActivity.class);

        //Add the chat's id as an extra piece of data
        intent.putExtra("chat", chatId);

        //Start the new activity, using the intent
        startActivity(intent);
    }

    //Method for loading all of the chats that a user is a part of
    public void loadChats() {
        //Get the unique id of the current user
        String uid = user.getUid();

        //Get a reference to the current user's info in the database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/" + uid);

        //Create and show a loading spinner while we wait for the chats to load
        final ProgressDialog loading = makeLoadingSpinner("Loading chats", "This will just take a moment.");
        loading.show();

        //Check the database once
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Set the chats scrolling view to the one in the activity
                ScrollView chats = (ScrollView) getActivity().findViewById(R.id.chats);

                //If there are no children (chats) for the current user, add some text telling them
                //to join one
                if (!dataSnapshot.hasChildren()) {
                    //Create a new TextView
                    TextView noChats = new TextView(getActivity());

                    //Set the text to the no chats string
                    noChats.setText(R.string.no_chats);

                    //Set the text's size to 20
                    noChats.setTextSize(20);

                    //Set the gravity (alignment) to center.
                    noChats.setGravity(Gravity.CENTER);

                    //Remove the current views in the scroll view and add the one we've created
                    chats.removeAllViews();
                    chats.addView(noChats);

                //Otherwise, the user is in chats
                } else {
                    //Create a LinearLayout for all of the chat blurbs
                    final LinearLayout layout = new LinearLayout(chats.getContext());

                    //Set the orientation to be vertical
                    layout.setOrientation(LinearLayout.VERTICAL);

                    //Create a new iterable from the user's children (the list of chats they're
                    //involved in)
                    Iterable<DataSnapshot> chatList = dataSnapshot.getChildren();

                    //Loop through the list of chats the user is involved in
                    for (DataSnapshot d : chatList) {

                        //Get the chat id from the snapshot
                        final String chatId = d.getKey();

                        //Create a database reference for the chats section of the database
                        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats");

                        //Access the chat reference's child corresponding to the current chat once
                        chatRef.child(chatId).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //Get the data as a Chat object
                                        Chat c = dataSnapshot.getValue(Chat.class);

                                        //Draw the chat's blurb on the layout
                                        c.drawChatBlurb(layout, getHomeActivity(), chatId);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //Log the error for debugging
                                        Log.e(TAG, "LoadChats: ChatBlurb: error");
                                    }
                                }
                        );
                    }
                    //Remove all of the views in the scrolling view and add the one with the chat blurbs
                    chats.removeAllViews();
                    chats.addView(layout);
                }
                //Cancel the loading dialog
                loading.cancel();
            }

            //Log the error for debugging
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "LoadChats:error");
            }
        });
    }

    //Function for creating a loading spinner (while waiting for async calls)
    public ProgressDialog makeLoadingSpinner(CharSequence t, CharSequence m) {
        //Create a new ProgressDialog (spinner) object on the current activity
        ProgressDialog loading = new ProgressDialog(this);

        //Set the title and message based on the arguments
        loading.setTitle(t);
        loading.setMessage(m);

        //Don't allow the user to cancel it
        loading.setCancelable(false);

        //Return the spinner
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
