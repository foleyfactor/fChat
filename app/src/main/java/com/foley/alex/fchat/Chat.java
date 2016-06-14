package com.foley.alex.fchat;

//Imports
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

//Class declaration
public class Chat {

    //Constant class values
    private static final String TAG = "Chat";
    private static final int MAX_BLURB_LENGTH = 40;

    //Class variables
    private String chatId, title;

    //Class constructor
    public Chat(String c, String t) {
        //Assign the class variables based on the arguments given
        this.chatId = c;
        this.title = t;
    }

    public Chat() {
        //A blank constructor is required for Firebase to store/retrieve Classes.
    }

    //Getters: return private class variables
    public String getChatId() {
        return this.chatId;
    }

    public String getTitle() {
        return this.title;
    }

    //Returns the DatabaseReference corresponding to the current chat
    private DatabaseReference getMessageDB() {
         return FirebaseDatabase.getInstance().getReference("chats/" + this.chatId);
    }

    //Get this chat's reference (for functions within functions)
    private Chat getChat() {
        return this;
    }

    //Method for drawing the little blurb on the screen with all of the chats on it
    //updates when a new message is sent to the chat
    public void drawChatBlurb(final LinearLayout l, final HomeActivity h, final String id) {
        //Create a new query for the most recent message, according to timestamp, in the chat
        Query orderByTime = this.getMessageDB().child("messages").orderByChild("timestamp").limitToLast(1);

        //Add a value listener for any change in value (i.e. called when messages are sent)
        orderByTime.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Create a layout for the blurb and set its properties
                        LinearLayout blurb = new LinearLayout(l.getContext());
                        blurb.setGravity(Gravity.CENTER_HORIZONTAL);
                        blurb.setOrientation(LinearLayout.VERTICAL);
                        blurb.setPadding(20, 20, 20, 20);

                        //Create a text view with the chat's title and set its properties
                        TextView title = new TextView(l.getContext());
                        title.setText(getChat().title);
                        title.setTextSize(20);
                        title.setTextColor(Color.BLACK);
                        title.setPadding(0,5,0,15);

                        //Add the title to the blurb layout
                        blurb.addView(title);

                        //If the chat has children (messages)
                        if (dataSnapshot.hasChildren()) {
                            //Get an iterable for the message
                            Iterable<DataSnapshot> message = dataSnapshot.getChildren();

                            //Loop through the iterable (the one most recent message)
                            for (DataSnapshot d : message) {
                                //Create a text view for the message and set properties
                                TextView recentMessage = new TextView(l.getContext());
                                recentMessage.setTextSize(14);
                                recentMessage.setPadding(0, 5, 0, 15);

                                //Get the message from the database as a Message object
                                Message m = d.getValue(Message.class);

                                //Create a string for the sender
                                String sender;

                                //Either set the sender to be "you" or the username of the sender
                                //depending on who sent the message
                                if (m.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    sender = "You";
                                } else {
                                    sender = m.getSender();
                                }

                                //If the text is longer than the blurb length, truncate it
                                //Then, set the text of the message
                                if (m.getText().length() + sender.length() > MAX_BLURB_LENGTH) {
                                    int messageCutoff = MAX_BLURB_LENGTH - sender.length();
                                    recentMessage.setText(sender + ":  " + m.getText().substring(0, messageCutoff + 1) + "...");
                                } else {
                                    recentMessage.setText(sender + ":  " + m.getText());
                                }

                                //Add the recent message layout to the blurb
                                blurb.addView(recentMessage);
                            }
                        } else {
                            //Note: this should never be called because messages are sent to the chat
                            //on creation announcing creation and joining of the chat. Regardless,
                            //if messages somehow get deleted from the database this will be called

                            //Create a new text view for the text and set properties
                            TextView newChat = new TextView(l.getContext());
                            newChat.setTextSize(14);
                            newChat.setPadding(0,5,0,15);

                            //Set the blurb to say that this is a new chat
                            newChat.setText("New chat...");

                            //Add the new chat text to the blurb
                            blurb.addView(newChat);
                        }

                        //Create a layout for the divider line between chats in the list
                        LinearLayout divider = new LinearLayout(l.getContext());
                        divider.setBackgroundColor(Color.BLACK);

                        //Create layout parameters specifying the width and height of the divider
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
                        divider.setLayoutParams(params);

                        //Add the divider to the blurb
                        blurb.addView(divider);

                        //Give the blurb a tag (id) corresponding to the chatId so that we can
                        //remove it if we have a newer message to display
                        blurb.setTag(chatId);

                        //Add an click listener to the text view that transitions to the chat
                        //that's clicked on
                        blurb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                h.toChat(id);
                            }
                        });

                        //Get the old blurb with the same chatId (for updating the most recent message)
                        LinearLayout oldBlurb = (LinearLayout) l.findViewWithTag(chatId);

                        //If there is a view with that tag, remove it and add this chat to the top of
                        //the list
                        if (oldBlurb != null) {
                            l.removeView(oldBlurb);
                            l.addView(blurb, 0);

                        //Otherwise, just add this blurb to the list
                        } else {
                            l.addView(blurb);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //Log the error so it can be fixed
                        Log.e(TAG, "DrawChatBlurb: error");
                    }
                }
        );
    }

    //Draws the chat's messages for the current chat (updates automatically)
    public void drawChatMessages(final ScrollView l) {
        Query orderByTime = this.getMessageDB().child("messages").orderByChild("timestamp");
        orderByTime.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        LinearLayout chatView = new LinearLayout(l.getContext());
                        LinearLayout.LayoutParams lparams =
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        chatView.setOrientation(LinearLayout.VERTICAL);
                        chatView.setLayoutParams(lparams);
                        Iterable<DataSnapshot> messages = dataSnapshot.getChildren();
                        for (DataSnapshot d : messages) {
                            LinearLayout message = d.getValue(Message.class).formatMessage(l.getContext());
                            message.setPadding(0,0,0,50);
                            chatView.addView(message);

                        }
                        l.removeAllViews();
                        l.addView(chatView);
                        l.post(new Runnable() {

                            @Override
                            public void run() {
                                l.fullScroll(l.FOCUS_DOWN);
                            }
                        });
                    }

                    //Called if there is an error
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //Log the error so I can troubleshoot it
                        Log.e(TAG, "Error while getting messages: " + databaseError.getDetails());
                    }
                }
        );
    }

    //Sends a message to the current chat
    public void sendMessage(Message m) {
        this.getMessageDB().child("messages").push().setValue(m);
    }
}
