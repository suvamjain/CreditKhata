package com.kirana.creditkhata;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.kirana.creditkhata.Modals.Users;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private AppCompatEditText mEmailView, mPasswordView;
    private Button loginBtn, btn_SignUp, btnReset;
    private View mProgressView, mLoginFormView;
    private Boolean saveLogin;
    private CheckBox saveLoginCheckBox;

    private boolean emailHasBeenEdited = false, pswdHasBeenEdited = false;
    private boolean shouldShowError = false;
    private Pattern validEmailAddressRegex = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
//        authListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = auth.getCurrentUser();
            Log.e("Login Activity", "firebase user: " + user);
            if (user != null) {
                // user auth state is changed - user is not null
                // launch main activity
                Log.e("Login activity","Auth state changed, opening Main Activity");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                Toast.makeText(LoginActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
//            }
//        };

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEmailView = findViewById(R.id.emailAddress);
        mPasswordView = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        btn_SignUp = findViewById(R.id.btn_signUp_login);
        btnReset = (Button) findViewById(R.id.btn_forgot_password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        saveLoginCheckBox = findViewById(R.id.rememberPswd);
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        saveLogin = loginPreferences.getBoolean("saveLogin", false);
        if (saveLogin) {
            mEmailView.setText(loginPreferences.getString("email", ""));
            mPasswordView.setText(loginPreferences.getString("password", ""));
            saveLoginCheckBox.setChecked(true);
            updateLoginButtonState();
        }

        mEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEmailView.setActivated(false);
                emailHasBeenEdited = true;
                updateLoginButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        mPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPasswordView.setActivated(false);
                pswdHasBeenEdited = true;
                updateLoginButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mEmailView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                shouldShowError = !hasFocus && !isEmailValid(mEmailView.getText().toString()) && emailHasBeenEdited;
                if (shouldShowError)
                    mEmailView.setActivated(true);
            }
        });

        mPasswordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                shouldShowError = !hasFocus && !isPasswordValid(mPasswordView.getText().toString()) && pswdHasBeenEdited;
                if (shouldShowError)
                    mPasswordView.setActivated(true);
            }
        });

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    //if false that means correct credentials so hide keyboard
                    return attemptLogin();
                }
                return false;
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                attemptLogin();
            }
        });

        btn_SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });
    }

    private void updateLoginButtonState() {
        if (!TextUtils.isEmpty(mEmailView.getText()) && !TextUtils.isEmpty(mPasswordView.getText())) {
            loginBtn.setEnabled(true);
        } else {
            loginBtn.setEnabled(false);
        }
    }

    private boolean isEmailValid(String emailStr) {
        Matcher matcher = validEmailAddressRegex.matcher(emailStr);
        return matcher.find();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6 && password.length() <= 90;
    }

    private void showProgress(final boolean show) {
        hideSoftKeyboard();
        if (show) {
            mLoginFormView.setEnabled(false);
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mLoginFormView.setEnabled(true);
            mProgressView.setVisibility(View.GONE);
        }
    }

    public void hideSoftKeyboard() {
        final InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private boolean attemptLogin() {

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

        if (isEmailValid(email) && isPasswordValid(password)) {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.

            // Reset errors.
            mEmailView.setActivated(false);
            mPasswordView.setActivated(false);

            showProgress(true);
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    showProgress(false);
                    if (!task.isSuccessful()) {
                        //handle with auth errors ---
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            showSnackBar("No user exists with this Email ID");
                            mEmailView.setActivated(true);
                        }
                        else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                            showSnackBar(getString(R.string.auth_failed));
                            mPasswordView.setActivated(true);
                        }
                        else if(task.getException() instanceof FirebaseNetworkException) {
                            showSnackBar("Network Error, check Internet Connection !!");
                        }
                        else {
                            showSnackBar("Authentication failed: Unknown Error");
                            Log.e("Login auth failed", task.getException() + "");
                        }
                    }
                    else {
                        // i.e. everything is correct let user sign in -
                        if (saveLoginCheckBox.isChecked()) {
                            loginPrefsEditor.putBoolean("saveLogin", true);
                            loginPrefsEditor.putString("email", email);
                            loginPrefsEditor.putString("password", password);
                            loginPrefsEditor.apply();
                        }
                        else {
                            loginPrefsEditor.clear();
                            loginPrefsEditor.commit();
                        }

                        final DatabaseReference mUserDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/");
                        mUserDatabaseRef.keepSynced(true);

                        Query loginUserDetails  = mUserDatabaseRef.orderByChild("u_email").equalTo(email);
                        loginUserDetails.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                if (dataSnapshot.exists()) {
                                    Users u = dataSnapshot.getValue(Users.class);
                                    loginPrefsEditor.putString("phNo", dataSnapshot.getKey());
                                    u.setU_ph(dataSnapshot.getKey());

                                    //these 2 lines is for updating the tokenId of the user who is logging in with this device
                                    // new or old and set tokenID to current devices id

                                    final String currentTokenId = FirebaseInstanceId.getInstance().getToken();
                                    mUserDatabaseRef.child(u.getU_ph()).child("u_token").setValue(currentTokenId);

                                    String json = new Gson().toJson(u);
                                    Log.e("Login Activity","Logged in User details saving to prefs " + json);
                                    loginPrefsEditor.putString("User", json);
                                    loginPrefsEditor.apply();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    Toast.makeText(LoginActivity.this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                            @Override
                            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
                            @Override
                            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });
                    }
                }
            });

            return false; //hide keyboard now
        }
        else {
            if(!isEmailValid("" + mEmailView.getText())) {
                mEmailView.setActivated(true);
                showSnackBar("Please enter a valid email !!");
            }
            else if (!isPasswordValid("" + mPasswordView.getText())) {
                mPasswordView.setActivated(true);
                showSnackBar("Password length too short !!");
            }
            return true; //don't hide keyboard so true
        }
    }

    public void showSnackBar(String msg){
        Snackbar snackbar = Snackbar.make(mLoginFormView,msg,Snackbar.LENGTH_SHORT);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_grey));
        snackbar.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        //auth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            //auth.removeAuthStateListener(authListener);
        }
    }
}

