package com.foley.alex.fchat;

//Imports
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Date;

//Class declaration
public class Message {

    //Class variables
    private String text, sender, uid;
    private long timestamp;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    //Class constructor
    public Message(String t, String s, Long d, String u) {
        //Assign the object's variables based on the parameters
        this.text = t;
        this.sender = s;
        this.timestamp = d;
        this.uid = u;
    }

    //Getters: return the private class variables
    public String getText() { return this.text; }

    public String getSender() { return this.sender; }

    public String getUid() { return this.uid; }

    public long getTimestamp() { return timestamp; }

    //Returns the layout corresponding to the message, with spacing and background
    public LinearLayout formatMessage(Context c) {
        //Check if the sender is the same as the current user
        boolean sentByUser = this.uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
        boolean sentBySystem = this.uid.equals("@system");

        //Create a linear layout that will contain the message, a layout for the message and a view
        //for optional right alignment
        LinearLayout outerLayout = new LinearLayout(c);
        LinearLayout messageLayout = new LinearLayout(c);
        View alignRight = new View(c);

        //Create layout parameters specifying the layouts' widths, heights, and weights
        LinearLayout.LayoutParams outerParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams innerParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams =
                new LinearLayout.LayoutParams(0, 0, 1);

        //Set the orientation of the message layout to vertical and the outer layout to horizontal
        outerLayout.setOrientation(LinearLayout.HORIZONTAL);
        messageLayout.setOrientation(LinearLayout.VERTICAL);

        //Set the layout parameters for the layouts and view
        outerLayout.setLayoutParams(outerParams);
        messageLayout.setLayoutParams(innerParams);
        alignRight.setLayoutParams(rightParams);

        //Create a text view for the sender's info
        TextView senderText = new TextView(c);

        //Set the sender to be "You" if the user sent it, and the username of the sender if someone
        //else sent it
        if (sentByUser) {
            senderText.setText("You");
        } else {
            senderText.setText(this.sender);
        }

        //Set the text to be a dark gray
        senderText.setTextColor(Color.DKGRAY);

        //Create a text view for the message, set its text and text's color
        TextView messageText = new TextView(c);
        messageText.setText(this.text);
        messageText.setTextColor(Color.BLACK);

        //Create a text view for the timestamp, set its text and the text's color
        TextView timestampText = new TextView(c);
        timestampText.setText(DateFormat.getDateTimeInstance().format(new Date(this.timestamp)));
        timestampText.setTextColor(Color.DKGRAY);

        //Add all of the views to the message's layout
        messageLayout.addView(senderText);
        messageLayout.addView(messageText);
        messageLayout.addView(timestampText);

        //Check if the message was sent by the user
        if (sentByUser) {
            //If so, align everything right and set the background to be the user message background
            outerLayout.addView(alignRight);

            senderText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            timestampText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            messageLayout.setBackgroundResource(R.drawable.user_message);
        } else if (sentBySystem) {
            //Set the background to be that of a system message
            messageLayout.setBackgroundResource(R.drawable.system_message);
        } else {
            //Otherwise, leave it left aligned and set the background to be that of a regular message
            messageLayout.setBackgroundResource(R.drawable.other_message);
        }

        //Add message view to the outer layout
        outerLayout.addView(messageLayout);

        //Return the outer layout
        return outerLayout;
    }
}
