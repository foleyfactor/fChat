package com.foley.alex.fchat;

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

/**
 * Created by Alex on 2016-05-31.
 */
public class Message {
    private String text, sender, uid;
    private long timestamp;
    private static String TAG = "Message";

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String t, String s, Long d, String u) {
        this.text = t;
        this.sender = s;
        this.timestamp = d;
        this.uid = u;
    }

    public String getText() {
        return this.text;
    }

    public String getSender() {
        return this.sender;
    }

    public String getUid() { return this.uid; }

    public long getTimestamp() {
        return this.timestamp;
    }

    //Returns the layout corresponding to the message, with spacing and background
    public LinearLayout formatMessage(Context c) {
        boolean sentByUser = this.uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());

        LinearLayout outerLayout = new LinearLayout(c);
        LinearLayout messageLayout = new LinearLayout(c);
        View alignRight = new View(c);

        LinearLayout.LayoutParams outerParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams innerParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightParams =
                new LinearLayout.LayoutParams(0, 0, 1);

        alignRight.setLayoutParams(rightParams);
        outerLayout.setOrientation(LinearLayout.HORIZONTAL);
        messageLayout.setOrientation(LinearLayout.VERTICAL);

        outerLayout.setLayoutParams(outerParams);
        messageLayout.setLayoutParams(innerParams);

        TextView senderText = new TextView(c);
        if (sentByUser) {
            senderText.setText("You");
        } else {
            senderText.setText(this.sender);
        }
        senderText.setTextColor(Color.DKGRAY);

        TextView messageText = new TextView(c);
        messageText.setText(this.text);
        messageText.setTextColor(Color.BLACK);

        TextView timestampText = new TextView(c);
        timestampText.setText(DateFormat.getDateTimeInstance().format(new Date(this.timestamp)));
        timestampText.setTextColor(Color.DKGRAY);

        messageLayout.addView(senderText);
        messageLayout.addView(messageText);
        messageLayout.addView(timestampText);

        if (sentByUser) {
            outerLayout.addView(alignRight);
            senderText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            timestampText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            messageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            messageLayout.setBackgroundResource(R.drawable.user_message);
        } else {
            messageLayout.setBackgroundResource(R.drawable.other_message);
        }

        outerLayout.addView(messageLayout);

        return outerLayout;
    }
}
