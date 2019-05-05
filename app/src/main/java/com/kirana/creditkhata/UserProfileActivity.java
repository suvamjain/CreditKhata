package com.kirana.creditkhata;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.kirana.creditkhata.Modals.Users;

import java.util.regex.Pattern;


public class UserProfileActivity extends AppCompatActivity implements TextWatcher, View.OnFocusChangeListener {

    private AppCompatEditText mEmail, mName, mAddr, mPhone;
    private Button saveBtn, change_pwd;
    private View mProgressView, mSignUpFormView;
    private DatabaseReference mUserDatabaseRef;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;

    private boolean nameHasEdited = false, addrHasEdited = false;
    private boolean shouldShowError = false;
    private Users mUsr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView toolTitle = findViewById(R.id.tool_title);
        toolTitle.setText("User Profile");

        //intilaize the db refs
        mUserDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/");
        mUserDatabaseRef.keepSynced(true);

        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        mUsr = new Gson().fromJson(loginPreferences.getString("User", null), Users.class);

        mEmail = findViewById(R.id.emailAddress);
        mName = findViewById(R.id.name);
        mPhone = findViewById(R.id.phone_no);
        mAddr = findViewById(R.id.addr);
        saveBtn = findViewById(R.id.save_profile_btn);
        change_pwd = findViewById(R.id.change_pwd);
        mSignUpFormView = findViewById(R.id.signUp_form);
        mProgressView = findViewById(R.id.signup_progress);

        saveBtn.setVisibility(View.VISIBLE);
        change_pwd.setVisibility(View.VISIBLE);

        TextView desc = findViewById(R.id.passwordDescription);
        desc.setVisibility(View.GONE);
        TextInputLayout pwd = findViewById(R.id.passwordLayout);
        pwd.setVisibility(View.GONE);
        TextInputLayout cpwd = findViewById(R.id.cnf_passwordLayout);
        cpwd.setVisibility(View.GONE);
        Button signUp = findViewById(R.id.sign_up_btn);
        signUp.setVisibility(View.GONE);
        Button signIn = findViewById(R.id.signup_btn_Login);
        signIn.setVisibility(View.GONE);

        mEmail.setEnabled(false);
        mPhone.setEnabled(false);
        mEmail.setBackgroundColor(getResources().getColor(R.color.silver_dark));
        mPhone.setBackgroundColor(getResources().getColor(R.color.silver_dark));

        if (mUsr != null) {
            mEmail.setText(mUsr.getU_email());
            mName.setText(mUsr.getU_name());
            mAddr.setText(mUsr.getU_addr());
            mPhone.setText(mUsr.getU_ph());
        }

        mName.addTextChangedListener(this);
        mAddr.addTextChangedListener(this);

        mName.setOnFocusChangeListener(this);
        mAddr.setOnFocusChangeListener(this);

        change_pwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CustomDialogClass cdd = new CustomDialogClass(UserProfileActivity.this, 0, "Confirmation ?", getResources().getString(R.string.change_pwd), null);
                cdd.show();
                cdd.yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(true);
                        FirebaseAuth.getInstance().sendPasswordResetEmail(mUsr.getU_email()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    showSnackBar("We have sent the instructions to reset your password on your email !!", Snackbar.LENGTH_LONG);
                                }
                                else {
                                    showSnackBar("Failed to send reset email !!", Snackbar.LENGTH_LONG);
                                }
                                showProgress(false);
                            }
                        });
                        cdd.dismiss();
                    }
                });
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserToFirebase();
            }
        });
    }


    private void updatesignUpBtnState() {
        if (!TextUtils.isEmpty(mName.getText())
                && !TextUtils.isEmpty(mAddr.getText())
                && !TextUtils.isEmpty(mPhone.getText())
                && !TextUtils.isEmpty(mEmail.getText())) {

            saveBtn.setEnabled(true);
        } else {
            saveBtn.setEnabled(false);
        }
    }

    private void showProgress(final boolean show) {
        hideSoftKeyboard();
        if (show) {
            mSignUpFormView.setEnabled(false);
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mSignUpFormView.setEnabled(true);
            mProgressView.setVisibility(View.GONE);
        }
    }

    public void hideSoftKeyboard() {
        final InputMethodManager inputManager = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void updateUserToFirebase() {
        if (TextUtils.isEmpty(mName.getText())) {
            showSnackBar("Please enter your full name!", Snackbar.LENGTH_SHORT);
            mName.setActivated(true);
            return;
        }
        else if (TextUtils.isEmpty(mAddr.getText())) {
            showSnackBar("Please enter your address!", Snackbar.LENGTH_SHORT);
            mAddr.setActivated(true);
            return;
        }

        mUsr.setU_name(mName.getText().toString());
        mUsr.setU_addr(mAddr.getText().toString());
        
        showProgress(true);
        //Now save user to firebase
        DatabaseReference mDbRef = mUserDatabaseRef.child(mUsr.getU_ph());
        mDbRef.child("u_name").setValue(mUsr.getU_name());
        mDbRef.child("u_addr").setValue(mUsr.getU_addr());

        //Also save user details to shared prefs
        String json = new Gson().toJson(mUsr);
        loginPrefsEditor.putString("User", json);
        loginPrefsEditor.apply();
        showProgress(false);
        Toast.makeText(getApplicationContext(), "Profile Saved Successfully", Toast.LENGTH_LONG).show();
        finish();
    }

    public void showSnackBar(String msg, int dur){
        Snackbar snackbar = Snackbar.make(mSignUpFormView,msg,dur);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_grey));
        snackbar.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        if(s.hashCode() == mName.getText().hashCode()) {
            mName.setActivated(false);
            nameHasEdited = true;
        }
        if(s.hashCode() == mAddr.getText().hashCode()) {
            mAddr.setActivated(false);
            addrHasEdited = true;
        }
        updatesignUpBtnState();
    }

    @Override
    public void afterTextChanged(Editable s) { }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {

            case R.id.name:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mName.getText()) && nameHasEdited;
                if (shouldShowError)
                    mName.setActivated(true);
                break;

            case R.id.addr:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mAddr.getText()) && addrHasEdited;
                if (shouldShowError)
                    mAddr.setActivated(true);
                break;

            default:
                break;
        }
    }
}
