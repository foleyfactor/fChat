package com.foley.alex.fchat;

//Imports
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;

//Class declaration
public class ChatActivity extends AppCompatActivity {

    //Constant
    private static String TAG = "ChatActivity";

    //Class variable
    private Chat chat;

    //Method called on the creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Call the superclass's onCreate with the saved state
        super.onCreate(savedInstanceState);

        //Set the layout of the view to be the chat activity's layout
        setContentView(R.layout.activity_chat);

        //Get the toolbar and set it as the action bar, also add the back arrow (up) button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get the chat id from the intent that started this activity
        Intent intent = getIntent();
        String chatId = intent.getStringExtra("chat");

        //Assign this to a variable so that we can access it in the inner functions
        final Activity thisActivity = this;

        //Create and show a dialog for while the chat loads
        final ProgressDialog loading = makeLoadingSpinner("Loading chat", "Get ready.");
        loading.show();

        //Create a database reference for the chat
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats/" + chatId);

        //Access the database once
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Get the chat object from the database
                Chat c = dataSnapshot.getValue(Chat.class);

                //Set the title of the activity (on the action bar) to include the chat title
                setTitle("fChat - " + c.getTitle());

                //Assign the class variable to the chat
                chat = c;

                //Cancel the spinner and draw the messages on the screen
                loading.cancel();
                c.drawChatMessages((ScrollView) thisActivity.findViewById(R.id.messages));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Log the error (troubleshooting) and cancel the spinner
                Log.e(TAG, "Error loading chat");
                loading.cancel();
            }
        });
    }

    //Method for sending a message from the user's input
    public void sendMessage(View v) {
        //Get the text from the text field
        String text = ((EditText)findViewById(R.id.message_field)).getText().toString();

        //Only send the message if there's at least one non-whitespace character
        if (text.trim().length() > 0) {
            //Send the message and clear the text box
            chat.sendMessage(new Message(text, FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), new Date().getTime(), FirebaseAuth.getInstance().getCurrentUser().getUid()));
            ((EditText) findViewById(R.id.message_field)).setText("");
        }
    }

    //Function for creating a loading dialog while we wait for the
    //asynchronous calls to complete
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

    //Method for leaving the chat
    public void leaveChat() {
        //Create a database reference to the user's involvement in this chat
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/"
                + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/"
                + chat.getChatId());

        //Remove the value and listen for completion
        ref.removeValue().addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //Trigger a back button press
                        ChatActivity.super.onBackPressed();
                        //Send a message to the chat saying that the user has left
                        chat.sendMessage(new Message(FirebaseAuth.getInstance().getCurrentUser().getDisplayName() + " has left the chat.",
                                "System", new Date().getTime(), "@system"));
                    }
                }
        );
    }

    //Method called for creating the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu corresponding to the menu for the chat activity
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    //Method called when an option is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Get the id of the item
        int id = item.getItemId();

        //If it's the item for leaving the chat, leave the chat
        if (id == R.id.leave_chat) {
            leaveChat();
        }

        //Call the superclass's method for that item being selected
        return super.onOptionsItemSelected(item);
    }
}
