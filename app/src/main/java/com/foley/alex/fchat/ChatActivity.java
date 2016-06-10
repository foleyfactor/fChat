package com.foley.alex.fchat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    private static String TAG = "ChatActivity";
    private Chat chat;

    //Method called on the creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        String chatId = intent.getStringExtra("chat");

        final Activity thisActivity = this;

        final ProgressDialog loading = makeLoadingSpinner("Loading chat", "Get ready.");
        loading.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats/" + chatId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Chat c = dataSnapshot.getValue(Chat.class);
                setTitle("fChat - " + c.getTitle());
                chat = c;
                loading.cancel();
                c.drawChatMessages((ScrollView) thisActivity.findViewById(R.id.messages));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading chat");
                loading.cancel();
            }
        });
    }

    //Method for sending a message from the user's input
    public void sendMessage(View v) {
        String text = ((EditText)findViewById(R.id.message_field)).getText().toString();
        if (text.trim().length() > 0) {
            chat.sendMessage(new Message(text, FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), new Date().getTime(), FirebaseAuth.getInstance().getCurrentUser().getUid()));
            ((EditText) findViewById(R.id.message_field)).setText("");
        }
    }

    //Function for creating a loading dialog while we wait for the
    //asynchronous calls to complete
    public ProgressDialog makeLoadingSpinner(CharSequence t, CharSequence m) {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setTitle(t);
        loading.setMessage(m);
        loading.setCancelable(false);
        return loading;
    }
}
