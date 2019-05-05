package com.kirana.creditkhata;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.Modals.Dues;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;
import static android.support.constraint.Constraints.TAG;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // here you can start an activity or service depending on your need
//        String phoneNumberReceiver="7358691791";// phone number to which SMS to be send
//        String message="Few users have dues pending for more than 30 days";// message to send
//        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(phoneNumberReceiver, null, message, null, null);

        // Show the toast  like in above screen shot
//        Toast.makeText(context, "Alarm Triggered", Toast.LENGTH_LONG).show();
        Log.e("Sent -->", "Alarm Triggered at: " + new GregorianCalendar().getTimeInMillis());
        checkDuesCount(context);
    }

    /**
     * Create and show a simple notification containing the message.
     * @param messageBody message body.
     */
    private void sendNotification(Context con, String messageBody) {
        int notificationId = new Random().nextInt(60000);
        Intent intent = new Intent(con, MainActivity.class);  //Send later to MainActivity.class acc to docs
        intent.putExtra("noti_id", notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(con,  0/* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = con.getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(con, channelId)
                        .setSmallIcon(R.drawable.ic_notify)
                        .setColor(con.getResources().getColor(R.color.colorPrimary))
                        .setContentTitle("Credit Overdue")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
//                        .setSubText("bookedBy")
                        .setShowWhen(true)
                        .setSound(defaultSoundUri)
                        .addAction(R.drawable.ic_details,"View", pendingIntent)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since in android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,"Channel human readable title", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Cancel the notification after its selected
        notificationBuilder.build().flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(notificationId /* ID of notification is set to 0 initially */, notificationBuilder.build());
    }

    public void checkDuesCount(final Context con) {
        final SharedPreferences loginPreferences = con.getSharedPreferences("loginPrefs", MODE_PRIVATE);
        String userPh = loginPreferences.getString("phNo",null);
        final int threshold = loginPreferences.getInt("threshold",15);
        final int threshPos = loginPreferences.getInt("threshListPos", 0);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("user_auth/" + userPh + "/customers/");
        final int[] count = {0};

        ref.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        if (d.hasChild("credits")) {
                            Credits c = d.getValue(Credits.class);
                            TreeMap<String, Credits.creditVal> ct = new TreeMap<String, Credits.creditVal>(c.getCredits());
                            for (String k : ct.keySet()) {
                                if (ct.get(k).getStatus() == null) {
                                    Date pendingFromDate = null;
                                    try {
                                        pendingFromDate = new SimpleDateFormat("yyMMdd").parse(k.split("_")[0]);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    int days = Days.daysBetween(new DateTime(pendingFromDate), new DateTime(new Date())).getDays();
                                    if (days > threshold) {
                                        Log.e("Alarm count:", c.getName());
                                        count[0]++;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (count[0] > 0) //send notification iff there are more than 1 customer above threshold
                        sendNotification(con, count[0] + " customers have dues pending for more than " + con.getResources().getStringArray(R.array.list_threshold)[threshPos]);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}