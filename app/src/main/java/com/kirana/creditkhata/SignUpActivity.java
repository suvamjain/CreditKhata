package com.kirana.creditkhata;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.kirana.creditkhata.Modals.Users;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity implements TextWatcher, View.OnFocusChangeListener {

    private AppCompatEditText mEmail, mPassword, mCnfPassword, mName, mAddr, mPhone;
    private Button signUpBtn, loginBtn;
    private View mProgressView, mSignUpFormView;

    private boolean emailHasEdited = false, pswdHasEdited = false, nameHasEdited = false, addrHasEdited = false, phHasEdited = false, cnfPwdHasEdited = false;
    private boolean shouldShowError = false;
    private Pattern validPhoneNo = Pattern.compile("\\d{10}");
    private Pattern validEmailAddressRegex = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private Pattern atLeastOneNumberPattern = Pattern.compile(".*[0-9].*");

    private FirebaseAuth auth;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;
    private DatabaseReference mUserDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        //intilaize the db refs
        mUserDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/");
        mUserDatabaseRef.keepSynced(true);

        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        mCnfPassword = findViewById(R.id.cnf_password);
        mName = findViewById(R.id.name);
        mPhone = findViewById(R.id.phone_no);
        mAddr = findViewById(R.id.addr);

        signUpBtn = findViewById(R.id.sign_up_btn);
        loginBtn = findViewById(R.id.signup_btn_Login);
        mSignUpFormView = findViewById(R.id.signUp_form);
        mProgressView = findViewById(R.id.signup_progress);
        
        mEmail.addTextChangedListener(this);
        mName.addTextChangedListener(this);
        mAddr.addTextChangedListener(this);
        mPhone.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        mCnfPassword.addTextChangedListener(this);

        mEmail.setOnFocusChangeListener(this);
        mName.setOnFocusChangeListener(this);
        mAddr.setOnFocusChangeListener(this);
        mPhone.setOnFocusChangeListener(this);
        mPassword.setOnFocusChangeListener(this);
        mCnfPassword.setOnFocusChangeListener(this);

        mCnfPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    //if false that means correct credentials so hide keyboard
                    return attemptSignUp();
                }
                return false;
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignUp();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(false);
                onBackPressed();
            }
        });
    }

    private void updatesignUpBtnState() {
        if (!TextUtils.isEmpty(mName.getText())
                && !TextUtils.isEmpty(mAddr.getText())
                && !TextUtils.isEmpty(mPhone.getText())
                && !TextUtils.isEmpty(mEmail.getText())
                && !TextUtils.isEmpty(mPassword.getText())
                && mPassword.getText().toString().length() >= 6
                && !TextUtils.isEmpty(mCnfPassword.getText())
        ) {
            signUpBtn.setEnabled(true);
        } else {
            signUpBtn.setEnabled(false);
        }
    }

    private boolean isEmailValid(String emailStr) {
        Matcher matcher = validEmailAddressRegex.matcher(emailStr);
        return matcher.find();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6 && password.length() <= 90 && atLeastOneNumberPattern.matcher(password).find();
    }

    private boolean isPhoneValid(String phNo) {
        Matcher matcher = validPhoneNo.matcher(phNo);
        return matcher.find();
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        if(s.hashCode() == mEmail.getText().hashCode()) {
            mEmail.setActivated(false);
            emailHasEdited = true;
        }
        if(s.hashCode() == mName.getText().hashCode()) {
            mName.setActivated(false);
            nameHasEdited = true;
        }
        if(s.hashCode() == mAddr.getText().hashCode()) {
            mAddr.setActivated(false);
            addrHasEdited = true;
        }
        if(s.hashCode() == mPhone.getText().hashCode()) {
            mPhone.setActivated(false);
            phHasEdited = true;
        }
        if(s.hashCode() == mPassword.getText().hashCode()) {
            mPassword.setActivated(false);
            pswdHasEdited = true;
        }
        if(s.hashCode() == mCnfPassword.getText().hashCode()) {
            mCnfPassword.setActivated(false);
            cnfPwdHasEdited = true;
        }
        updatesignUpBtnState();
    }

    @Override
    public void afterTextChanged(Editable s) { }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {

            case R.id.emailAddress:
                shouldShowError = !hasFocus && !isEmailValid(mEmail.getText().toString()) && emailHasEdited;
                if (shouldShowError)
                    mEmail.setActivated(true);
                break;

            case R.id.name:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mName.getText()) && nameHasEdited;
                if (shouldShowError)
                    mName.setActivated(true);
                break;
            
            case R.id.phone_no:
                shouldShowError = !hasFocus && !isPhoneValid(mPhone.getText().toString()) && phHasEdited;
                if (shouldShowError)
                    mPhone.setActivated(true);
//                else if (!hasFocus && isPhoneValid() && phHasEdited)
//                    checkUser();
                break;
                
            case R.id.addr:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mAddr.getText()) && addrHasEdited;
                if (shouldShowError)
                    mAddr.setActivated(true);
                break;

            case R.id.password:
                shouldShowError = !hasFocus && !isPasswordValid(mPassword.getText().toString()) && pswdHasEdited;
                if (shouldShowError)
                    mPassword.setActivated(true);
                break;

            case R.id.cnf_password:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mCnfPassword.getText()) && cnfPwdHasEdited;
                if (shouldShowError)
                    mCnfPassword.setActivated(true);
                break;

            default:
                break;
        }
    }

    private boolean attemptSignUp() {

        // Store values at the time of the sign up attempt.

        final String email = mEmail.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        String cnfPassword = mCnfPassword.getText().toString().trim();
        final String name = mName.getText().toString().trim();
        final String phoneNo = mPhone.getText().toString().trim();
        final String addr = mAddr.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            showSnackBar("Please enter your full name!");
            mName.setActivated(true);
            return true;
        }
        else if (TextUtils.isEmpty(addr)) {
            showSnackBar("Please enter your address!");
            mAddr.setActivated(true);
            return true;
        }
        else if (!isPhoneValid(phoneNo)) {
            showSnackBar("Please enter a valid Phone Number!");
            mPhone.setActivated(true);
        }
        else if (!isEmailValid("" + email)) {
            showSnackBar( "Please enter valid email address!");
            mEmail.setActivated(true);
            return true;
        }
        else if (!isPasswordValid("" + password)) {
            showSnackBar("Please enter valid password!");
            mPassword.setActivated(true);
            return true;
        }
        else if (TextUtils.isEmpty(cnfPassword) || !password.equals(cnfPassword)) {
            showSnackBar("Password doesn't match");
            mCnfPassword.setActivated(true);
            return true;
        }
        else{

            // Reset errors.
            mName.setActivated(false);
            mAddr.setActivated(false);
            mPhone.setActivated(false);
            mEmail.setActivated(false);
            mPassword.setActivated(false);
            mCnfPassword.setActivated(false);

            showProgress(true);
            checkUserPhNo(new Users(name, addr, email, null,null), phoneNo, password);
        }
        return false; //hide keyboard now
    }

    private void checkUserPhNo(final Users mUser, final String mPhNo, final String mPwd) {
        Query mQuery = mUserDatabaseRef.orderByKey().equalTo(mPhNo);
        mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() > 0) {
                    Log.d("Ph No Check more child", dataSnapshot.getValue().toString());
                    showSnackBar( "Phone number already in use");
                    mPhone.setActivated(true);
                    showProgress(false);
                }
                else {
                    showSnackBar("Phone number verified");
                    //create user
                    showProgress(true);
                    auth.createUserWithEmailAndPassword(mUser.getU_email(), mPwd)
                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    Log.d("user creation", " createUserWithEmail:onComplete: " + task.isSuccessful());
                                    showProgress(false);

                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.

                                    if (!task.isSuccessful()) {
                                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                            mEmail.setActivated(true);
                                            showSnackBar("Authentication failed: Email already in use!");
                                        }
                                        else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                            mEmail.setActivated(true);
                                            showSnackBar("Email entered is not valid!");
                                        }
                                        else if(task.getException() instanceof FirebaseNetworkException) {
                                            showSnackBar("Network Error, check Internet Connection!");
                                        }
                                        else {
                                            showSnackBar("Authentication failed: Unknown Error" + task.getException());
                                            Log.d("authentication failed", task.getException() + "");
                                        }
                                        return;
                                    }
                                    String token = FirebaseInstanceId.getInstance().getToken();
                                    mUser.setU_token(token); //saving token for future notifications
                                    saveUserToFirebase(mUser, mPhNo);
                                }
                            });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("onCancelled", "" + databaseError);
                showSnackBar("Unknown error occurred. Please try again later");
            }
        });
    }

    private void saveUserToFirebase(Users user, String phNumber){
        showProgress(true);
        //Now save user to firebase
        DatabaseReference mDbRef = mUserDatabaseRef.child("" + phNumber);
        mDbRef.setValue(user);

        //Also save user details to shared prefs
        user.setU_ph(phNumber);  //save the phNumber too for MainActivity operations
        loginPrefsEditor.clear();
        String json = new Gson().toJson(user);
        loginPrefsEditor.putString("User", json);
        loginPrefsEditor.putString("phNo", phNumber);
        loginPrefsEditor.apply();
        showProgress(false);
        Toast.makeText(getApplicationContext(), "Registration Successful", Toast.LENGTH_LONG).show();
        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        finish();
    }

    public void showSnackBar(String msg){
        Snackbar snackbar = Snackbar.make(mSignUpFormView,msg,Snackbar.LENGTH_SHORT);
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
        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        finish();
        super.onBackPressed();
    }
}
