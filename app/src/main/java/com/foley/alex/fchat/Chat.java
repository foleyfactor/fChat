package com.foley.alex.fchat;

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

/**
 * Created by Alex on 2016-05-31.
 */
public class Chat {

    private String chatId, title;
    private static String TAG = "Chat";

    private static final int MAX_BLURB_LENGTH = 40;

    public Chat(String c, String t) {
        this.chatId = c;
        this.title = t;
    }

    public Chat() {
        //A blank constructor is required for Firebase to store/retrieve Classes.
    }

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
        Query orderByTime = this.getMessageDB().child("messages").orderByChild("timestamp").limitToLast(1);
        orderByTime.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        LinearLayout blurb = new LinearLayout(l.getContext());
                        blurb.setGravity(Gravity.CENTER_HORIZONTAL);
                        blurb.setOrientation(LinearLayout.VERTICAL);
                        blurb.setPadding(20, 20, 20, 20);

                        TextView title = new TextView(l.getContext());
                        title.setText(getChat().title);
                        title.setTextSize(20);
                        title.setTextColor(Color.BLACK);
                        title.setPadding(0,5,0,15);
                        blurb.addView(title);

                        if (dataSnapshot.hasChildren()) {
                            Iterable<DataSnapshot> message = dataSnapshot.getChildren();
                            for (DataSnapshot d : message) {
                                TextView recentMessage = new TextView(l.getContext());
                                Message m = d.getValue(Message.class);
                                String sender;
                                if (m.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    sender = "You";
                                } else {
                                    sender = m.getSender();
                                }
                                if (m.getText().length() + sender.length() > MAX_BLURB_LENGTH) {
                                    int messageCutoff = MAX_BLURB_LENGTH - sender.length();
                                    recentMessage.setText(sender + ":  " + m.getText().substring(0, messageCutoff + 1) + "...");
                                } else {
                                    recentMessage.setText(sender + ":  " + m.getText());
                                }
                                recentMessage.setTextSize(14);
                                recentMessage.setPadding(0, 5, 0, 15);
                                blurb.addView(recentMessage);
                            }
                        } else {
                            TextView newChat = new TextView(l.getContext());
                            newChat.setText("New chat...");
                            newChat.setTextSize(14);
                            newChat.setPadding(0,5,0,15);
                            blurb.addView(newChat);
                        }
                        LinearLayout divider = new LinearLayout(l.getContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
                        divider.setLayoutParams(params);
                        divider.setBackgroundColor(Color.BLACK);

                        blurb.addView(divider);
                        blurb.setTag(chatId);

                        blurb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                h.toChat(id);
                            }
                        });

                        LinearLayout oldBlurb = (LinearLayout) l.findViewWithTag(chatId);
                        if (oldBlurb != null) {
                            l.removeView(oldBlurb);
                            l.addView(blurb, 0);
                        } else {
                            l.addView(blurb);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
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
