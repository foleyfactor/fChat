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
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats/" + c);

        //Show the spinner while we wait for the request
        final ProgressDialog wait = makeLoadingSpinner("Joining chat: " + c, "Just a moment.");
        wait.show();

        //Add the chat's id to the user's set of chats
        userRef.child(c).setValue("").addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //If the user is added successfully
                if (task.isSuccessful()) {
                    //Access the chats once
                    chatRef.addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    //When the chat loads, send a message that we've joined
                                    dataSnapshot.getValue(Chat.class)
                                            .sendMessage(new Message(FirebaseAuth.getInstance().getCurrentUser().getDisplayName() + " has joined the chat.",
                                                    "System", new Date().getTime(), "@system"));

                                    //Then cancel the spinner and load the chats
                                    wait.cancel();
                                    loadChats();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    //Log the error so I can troubleshoot it, then cancel the spinner
                                    Log.e(TAG, "AddUser: ChatRef Error");
                                    wait.cancel();
                                }
                            }
                    );
                } else {
                    //Otherwise, cancel the spinner and notify the user
                    wait.cancel();
                    Toast.makeText(getApplicationContext(), "Error while joining chat. Try again later.", Toast.LENGTH_SHORT)
                            .show();
                    //Note: toasting is a way to alert the user of information, it
                    //creates the little pop-up blurb on the screen
                }
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
                //Log the error so I can troubleshoot and cancel the spinner
                Log.e(TAG, "LoadChats: error");
                loading.cancel();
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
        //Create a new builder to make a dialog on this activity
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Get the layout inflater for this activity so that we can inflate a view
        LayoutInflater inflater = this.getLayoutInflater();

        //Inflate a view with the layout for the creating chat dialog
        final View dialogView = inflater.inflate(R.layout.create_chat_dialog, null);

        //Set the view to the one inflated from the layout
        builder.setView(dialogView)
                //Create the positive button with a click listener
                .setPositiveButton(R.string.create_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Set the chat title and id based on the fields on the dialog
                        final String chatTitle = ((TextView) dialogView.findViewById(R.id.chat_title)).getText().toString();
                        final String chatId = ((TextView) dialogView.findViewById(R.id.chat_id)).getText().toString().toLowerCase();

                        //Don't do anything if either field is blank
                        if (chatId.isEmpty() || chatTitle.isEmpty()) {
                            dialog.cancel();
                            return;
                        }

                        //Create and show a loading spinner
                        final ProgressDialog wait = makeLoadingSpinner("Creating chat", "Preparing your chat from scratch...");
                        wait.show();

                        //Create a database reference corresponding to the chats section
                        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats");

                        //Access the database once
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.hasChild(chatId)) {

                                    //If a chat with that id doesn't exist, create a chat!
                                    final Chat c = new Chat(chatId, chatTitle);

                                    //Set the value in the database
                                    ref.child(chatId).setValue(c).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //When that's done, cancel the spinner and add the
                                            //user to the chat
                                            wait.cancel();
                                            addUserToChat(chatId);

                                            //Send a message letting everyone know that the user
                                            //has joined the chat
                                            c.sendMessage(new Message(FirebaseAuth.getInstance().getCurrentUser().getDisplayName() + " created the chat.",
                                                    "System", new Date().getTime(), "@system"));
                                        }
                                    });
                                } else {
                                    //If a chat already exists, cancel the spinner and let the user
                                    //know that there is already a chat
                                    wait.cancel();

                                    Toast.makeText(getApplicationContext(), "Error: a chat with that id already exists.", Toast.LENGTH_SHORT)
                                            .show();
                                    //Note: toasting is a way to alert the user of information, it
                                    //creates the little pop-up blurb on the screen
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                //Cancel the spinner and log the error so that I can troubleshoot it
                                wait.cancel();
                                Log.e(TAG, "CreateChat: error");
                            }
                        });
                    }
                })
                //Create and show the dialog
                .create().show();
    }

    //Method for showing the dialog prompting the user to join a chat
    public void showJoinChatDialog() {
        //Create a new builder to make a dialog on this activity
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Get the layout inflater so that we can inflate a view
        LayoutInflater inflater = this.getLayoutInflater();

        //Inflate the join chat dialog layout into a new view for the dialog
        final View dialogView = inflater.inflate(R.layout.join_chat_dialog, null);

        //Set the view to be the one we inflated
        builder.setView(dialogView)

                //Create a positive (Join Chat) button with a click listener
                .setPositiveButton(R.string.join_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Get the chat id that the user input
                        final String chatId = ((TextView) dialogView.findViewById(R.id.chat_id)).getText().toString().toLowerCase();

                        //If the user didn't enter anything, cancel the dialog and quit
                        if (chatId.isEmpty()) {
                            dialog.cancel();
                            return;
                        }

                        //Create a new spinner for the user's waiting pleasure and show it
                        final ProgressDialog wait = makeLoadingSpinner("Looking for chat: " + chatId, "Please be patient.");
                        wait.show();

                        //Create a reference to the database in the chats section
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats");

                        //Access the database once
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (!dataSnapshot.hasChild(chatId)) {
                                            //If there doesn't exist a chat with that
                                            //id, tell the user and cancel the spinner
                                            Toast.makeText(getApplicationContext(), "Error: no chat found.", Toast.LENGTH_SHORT)
                                                    .show();

                                            wait.cancel();
                                        } else {
                                            //Otherwise, add the user to the chat and cancel the
                                            //spinner
                                            addUserToChat(chatId);
                                            wait.cancel();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //Log the error for troubleshooting and cancel the spinner
                                        wait.cancel();
                                        Log.e(TAG, "JoinChat: error");
                                    }
                                });
                    }
                })
                //Create a negative (Create Chat) button with a click listener
                .setNegativeButton(R.string.create_chat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Hide the current dialog and create a "Create Chat" dialog instead
                        dialog.cancel();
                        showCreateChatDialog();
                    }
                })
                //Create and show the dialog
                .create().show();
    }

    //Method for showing the dialog prompting the user to login
    public void showLoginDialog() {
        //Create a new builder so that we can create a dialog on this activity
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Get the layout inflater for this activity
        LayoutInflater inflater = this.getLayoutInflater();

        //Inflate a view using the inflater and the layout for the login dialog
        final View dialogView = inflater.inflate(R.layout.login_dialog, null);

        //Set the view of the dialog to be the one that we inflated
        builder.setView(dialogView)
                //Don't allow the user to cancel this dialog
                .setCancelable(false)
                //Create a positive (Login) button with a click listener
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Get the email and password from the dialog
                        String email = ((EditText) dialogView.findViewById(R.id.email)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.pass)).getText().toString();

                        //If either email or password is blank, cancel this dialog and create
                        //a new one for them (so they can fill it in properly)
                        if (email.isEmpty() || password.isEmpty()) {
                            dialog.cancel();
                            showLoginDialog();
                            return;
                        }

                        //Cancel the dialog
                        dialog.cancel();

                        //Create a spinner to let the user what's up and show it to them
                        final ProgressDialog wait = makeLoadingSpinner("Logging in", "Please wait.");
                        wait.show();

                        //Sign them in using the email and password that they provided,
                        //and listen for the event to finish
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //When the authorization is complete

                                //Cancel the spinner
                                wait.cancel();

                                //If it wasn't successful, prompt for login again
                                if (!task.isSuccessful()) {
                                    showLoginDialog();
                                //If it was successful, update the user and load the chats
                                } else {
                                    user = FirebaseAuth.getInstance().getCurrentUser();
                                    loadChats();
                                }
                            }
                        });
                    }
                })
                //Create a negative (Sign Up) button with a click listener
                .setNegativeButton(R.string.signup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Cancel the current dialog and show the sign up dialog
                        dialog.cancel();
                        showSignUpDialog();
                    }
                });
        //Create and show the dialog
        builder.create().show();
    }

    //Method for showing the dialog prompting the user to sign up
    public void showSignUpDialog() {
        //Create a new builder so we can make a dialog on this activity
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Get the activity's inflater
        LayoutInflater inflater = this.getLayoutInflater();

        //Inflate a view with the layout for sign up dialogs
        final View dialogView = inflater.inflate(R.layout.sign_up_dialog, null);

        //Set the view of the builder to be the one we inflated
        builder.setView(dialogView)
                //Don't let the user cancel this dialog
                .setCancelable(false)
                //Create a positive (Sign Up) button with a click listener
                .setPositiveButton(R.string.signup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Cancel the current dialog
                        dialog.cancel();

                        //Get the username, password and email from the dialog
                        final String username = ((EditText) dialogView.findViewById(R.id.user)).getText().toString();
                        String email = ((EditText) dialogView.findViewById(R.id.email)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.pass)).getText().toString();

                        //Re-prompt the user if the fields are empty
                        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            dialog.cancel();
                            showSignUpDialog();
                            return;
                        }

                        //Create and show a waiting spinner for the user
                        final ProgressDialog wait = makeLoadingSpinner("Signing up", "Just a moment.");
                        wait.show();

                        //Create a new user with the user's info
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //If something went wrong, cancel the spinner and make another
                                //dialog
                                if (!task.isSuccessful()) {
                                    wait.cancel();
                                    showSignUpDialog();
                                } else {
                                    //Assign the user to the created user
                                    user = FirebaseAuth.getInstance().getCurrentUser();

                                    //Make a profile change request to update the user's display
                                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(username)
                                            .build();

                                    //Update the user's profile and listen for completion
                                    user.updateProfile(profile).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //Cancel the loading and load the chats
                                            wait.cancel();
                                            loadChats();
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                //Create a negative (Login) button with a click listener
                .setNegativeButton(R.string.login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Cancel the dialog and prompt the user to login
                        dialog.cancel();
                        showLoginDialog();
                    }
                });
        //Create and show the dialog
        builder.create().show();
    }

    //Method that creates the action bar's option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflates the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    //Method called when an item on the action bar is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar handles the back arrow click because parent activity is specified in the
        // manifest.xml file
        int id = item.getItemId();

        //If they select logout, log out and show the dialog
        if (id == R.id.logout_option) {
            logOutAndShowDialog();
        }

        //Call the super event on the item
        return super.onOptionsItemSelected(item);
    }

    //Method that logs the user out and shows them a dialog
    public void logOutAndShowDialog() {
        //Remove all of the chat blurbs
        ((ScrollView) findViewById(R.id.chats)).removeAllViews();

        //Sign out the auth instance
        FirebaseAuth.getInstance().signOut();

        //Prompt the user to login
        showLoginDialog();
    }
}
