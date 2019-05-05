package com.kirana.creditkhata;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.database.Query;
import com.kirana.creditkhata.Modals.Credits;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class AlarmTestActivity extends AppCompatActivity {

    private ComponentName componentName;
    private PackageManager packageManager;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    private Boolean showNotifications;
    private SharedPreferences loginPreferences;
    private AppCompatSpinner spinnerThreshold, repeatSpinner;
    private String[] listRepeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_test);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        componentName = new ComponentName(this, AlarmReceiver.class);
        packageManager = getPackageManager();

        // create an Intent and set the class which will execute when Alarm triggers, here we have
        // given AlarmReceiver in the Intent, the onReceive() method of this class will execute when alarm triggers and
        // we will write the code to send SMS inside onReceive() method pf AlarmReceiver class
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(this,1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

        // create the object
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Switch alarmSwitch = (Switch) findViewById(R.id.alarmSwitch);
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        showNotifications = loginPreferences.getBoolean("showNotifications", false);
        alarmSwitch.setChecked(showNotifications); //set the switch value stored in prefs previously

        final String[] listArray = getResources().getStringArray(R.array.list_threshold);
        final int[] threshold = {loginPreferences.getInt("threshold", 15)};

        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.list_threshold, R.layout.list_spinner);
        spinnerThreshold = findViewById(R.id.thresSpinner);
        spinnerThreshold.setAdapter(spinnerAdapter);
        spinnerThreshold.setSelection(loginPreferences.getInt("threshListPos", 0));
        spinnerThreshold.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(MainActivity.this, "you selected: " + listArray[position], Toast.LENGTH_SHORT).show();
                switch (listArray[position]) {

                    case "15 days":
                        threshold[0] = 15;
                        break;

                    case "1 month":
                        threshold[0] = 30;
                        break;

                    case "2 months":
                        threshold[0] = 60;
                        break;

                    case "4 months":
                        threshold[0] = 120;
                        break;

                    case "6 months":
                        threshold[0] = 180;
                        break;

                    case "8 months":
                        threshold[0] = 240;
                        break;

                    case "1 year":
                        threshold[0] = 365;
                        break;

                    default:
                        break;
                }

                loginPreferences.edit().putInt("threshold", threshold[0]).apply(); //save threshold in prefs
                loginPreferences.edit().putInt("threshListPos", position).apply(); //save thresholdListPos in prefs
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    /* Enable these lines if want to set the repeat after value too
        listRepeat = getResources().getStringArray(R.array.list_repeat);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        SpinnerAdapter repeatAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.list_repeat, R.layout.list_spinner);
        repeatSpinner.setAdapter(repeatAdapter);
        repeatSpinner.setSelection(loginPreferences.getInt("repeatPos",0));
        repeatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loginPreferences.edit().putInt("repeatPos", position).apply();
                //reschedule the alarm with new repeating params if the alarm is already enabled
                if (showNotifications){
                    cancelAlarm();
                    scheduleAlarm();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    */

        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    scheduleAlarm();
                    showNotifications = true;
                    loginPreferences.edit().putBoolean("showNotifications", true).apply();
                } else {
                    cancelAlarm();
                    showNotifications = false;
                    loginPreferences.edit().putBoolean("showNotifications", false).apply();
                }
            }
        });
    }

    public void scheduleAlarm() {
        // time at which alarm will be scheduled here alarm is scheduled at 1 day from current time
        // i.e. 24*60*60*1000= 86,400,000 milliseconds in a day
        long time = new GregorianCalendar().getTimeInMillis()+2*1000;

//        // create an Intent and set the class which will execute when Alarm triggers, here we have
//        // given AlarmReceiver in the Intent, the onReceive() method of this class will execute when alarm triggers and
//        // we will write the code to send SMS inside onReceive() method pf AlarmReceiver class
//        Intent intentAlarm = new Intent(this, AlarmReceiver.class);

//        // create the object
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //set the alarm for particular time
//        alarmManager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(this,1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, Long.parseLong(listRepeat[repeatSpinner.getSelectedItemPosition()])*24*60*60*1000,
//                PendingIntent.getBroadcast(this,1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, 60*1000, pendingIntent);
        Toast.makeText(this, "Notification service started", Toast.LENGTH_SHORT).show();
        Log.e("Alarm Scheduled", " for time: " + time);

    }

    public void cancelAlarm() {
        Log.e("Cancelling Alarm ","<------------->");
        alarmManager.cancel(pendingIntent);
        packageManager.setComponentEnabledSetting(componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, // or  COMPONENT_ENABLED_STATE_ENABLED
            PackageManager.DONT_KILL_APP);
        Toast.makeText(this, "Notification service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
