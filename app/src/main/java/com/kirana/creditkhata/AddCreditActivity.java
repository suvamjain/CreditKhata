package com.kirana.creditkhata;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.Modals.Users;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddCreditActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

    private AppCompatEditText mPhone, mAmt, mName, mAddr, mOther;
    private FloatingActionButton mAddDebit, mAddCredit; //mSave, mReset;
    private DatabaseReference mDatabaseRef;
    private Pattern validPhoneNo = Pattern.compile("\\d{10}");
    private boolean phNoHasBeenEdited = false, amtHasBeenEdited = false, nameHasBeenEdited = false, shouldShowError = false;
    private View parentLayout;
    private boolean userExists=false, sendBackToSettle = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_credit_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        parentLayout = findViewById(R.id.add_credit_form);
        mPhone = findViewById(R.id.add_phone_no);
        mAmt = findViewById(R.id.add_credit_amt);
        mName = findViewById(R.id.add_name);
        mAddr = findViewById(R.id.add_addr);
        mOther = findViewById(R.id.add_other_detail);
        mAddCredit = findViewById(R.id.add_cr);
        mAddDebit = findViewById(R.id.add_dr);

        SharedPreferences loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginPrefsEditor = loginPreferences.edit();
        String userPh = loginPreferences.getString("phNo", null);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/" + userPh + "/customers/");
        mDatabaseRef.keepSynced(true);

        if (getIntent().getExtras() != null) {
            Bundle b = getIntent().getExtras();
            mPhone.setText("" +  b.get("phNo"));
            mPhone.setEnabled(false);
            mPhone.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            checkUser();
            sendBackToSettle = true;
        }
        mAddCredit.setOnClickListener(this);
        mAddDebit.setOnClickListener(this);

        mPhone.setOnFocusChangeListener(this);
        mAmt.setOnFocusChangeListener(this);
        mName.setOnFocusChangeListener(this);

        mAmt.setActivated(false);
        mPhone.setActivated(false);
        mName.setActivated(false);

        mPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPhone.setActivated(false);
                phNoHasBeenEdited = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mName.setActivated(false);
                nameHasBeenEdited = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mAmt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAmt.setActivated(false);
                amtHasBeenEdited = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.add_cr:
                if (!isPhoneValid()) {
                    Toast.makeText(this, "Please enter a valid Phone Number", Toast.LENGTH_SHORT).show();
//                    Snackbar.make(parentLayout, "Please enter a valid Phone Number", Snackbar.LENGTH_SHORT).show();
                    mPhone.setActivated(true);
                }
                else if (TextUtils.isEmpty(mName.getText())) {
                    Toast.makeText(this, "Please enter customer's name", Toast.LENGTH_SHORT).show();
//                    Snackbar.make(parentLayout, "Please enter a valid Phone Number", Snackbar.LENGTH_SHORT).show();
                    mName.setActivated(true);
                }
                else if (TextUtils.isEmpty(mAmt.getText())) {
                    Toast.makeText(this, "Please enter a valid Credit Amount", Toast.LENGTH_SHORT).show();
                    mAmt.setActivated(true);
                }
                else {
                    final CustomDialogClass cdd = new CustomDialogClass(AddCreditActivity.this, 0,
                            "Confirmation ?", getResources().getString(R.string.add_credit), null);
                    cdd.show();
                    cdd.yes.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String dt = new SimpleDateFormat("yyMMdd").format(new Date());
                            final Query qr = mDatabaseRef.child("" + mPhone.getText());
                            mAmt.setText(String.format("%.2f",Double.parseDouble(mAmt.getText().toString())));
                            final String time = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
                            final Credits c = new Credits (
                                    "" + mName.getText(),
                                    "" + mAddr.getText(),
                                    "" + dt + "_" + time,
                                    "" + mAmt.getText(),
                                    "" + mOther.getText());

                            if (userExists) {
                                qr.getRef().child("credits/"+dt + "_" + time)
                                        .setValue(c.setOnlyCredit("" + mAmt.getText(), "" + mOther.getText(), time));
                                qr.getRef().child("name").setValue(mName.getText()+"");
                                qr.getRef().child("addr").setValue(mAddr.getText()+"");
                            }
                            else {
                                qr.getRef().setValue(c);
                                Log.e("---Adding new user", c.toString());
                            }
                            Toast.makeText(AddCreditActivity.this, "New Credit Added", Toast.LENGTH_SHORT).show();
                            cdd.dismiss();
                            if(sendBackToSettle) {
                                //send results back to settle activity too
                                Intent i = new Intent(AddCreditActivity.this, SettleActivity.class);
                                Credits cr = new Credits();
                                i.putExtra("mDate", dt);
                                i.putExtra("val", cr.setOnlyCredit("" + mAmt.getText(), "" + mOther.getText(), time));
                                setResult(1, i);
                            }
                            finish();
                        }
                    });
                }
                break;
//            case R.id.add_dr:
//                //clear all the fields
//                mPhone.setText(null);
//                mAmt.setText(null);
//                mName.setText(null);
//                mAddr.setText(null);
//                mOther.setText(null);
//                break;

            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

        switch (v.getId()) {
            case R.id.add_phone_no:
                shouldShowError = !hasFocus && !isPhoneValid() && phNoHasBeenEdited;
                if (shouldShowError)
                    mPhone.setActivated(true);
                else if (!hasFocus && isPhoneValid() && phNoHasBeenEdited)
                    checkUser();
                break;

            case R.id.add_name:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mName.getText()) && nameHasBeenEdited;
                if (shouldShowError)
                    mName.setActivated(true);
                break;

            case R.id.add_credit_amt:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mAmt.getText()) && amtHasBeenEdited;
                if (shouldShowError)
                    mAmt.setActivated(true);
//                else if (!hasFocus && !TextUtils.isEmpty(mAmt.getText()) && amtHasBeenEdited)
//                    mAmt.setText(String.format("%.2f",Double.parseDouble(mAmt.getText().toString())));
                break;

            default:
                break;
        }
    }
    private boolean isPhoneValid() {
        String phNo = mPhone.getText().toString();
        Matcher matcher = validPhoneNo.matcher(phNo);
        return matcher.find();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUser() {
        final ProgressDialog pd = new ProgressDialog(AddCreditActivity.this);
        pd.setTitle("Please Wait...");
        pd.setMessage("Loading User Details");
        pd.setCancelable(false);
        pd.show();
        final Query q = mDatabaseRef.child("" + mPhone.getText());
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userExists = true;
//                    phNoHasBeenEdited=false;
                    mAddr.setText("" + dataSnapshot.child("addr").getValue());
                    mName.setText("" + dataSnapshot.child("name").getValue());
                }
                else {
                    userExists = false;
                    mAddr.setText(null);
                    mName.setText(null);
                }
                Runnable progressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                    }
                };
                Handler pdCanceller = new Handler();
                pdCanceller.postDelayed(progressRunnable, 1000);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
                Toast.makeText(AddCreditActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                Log.e("--check user_ph error", databaseError.toString());
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (sendBackToSettle)
            setResult(0);
        finish();
        super.onBackPressed();
    }
}

