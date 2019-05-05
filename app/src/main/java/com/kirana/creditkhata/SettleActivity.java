package com.kirana.creditkhata;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.kirana.creditkhata.Adapters.CollapsingItemAdapter;
import com.kirana.creditkhata.Adapters.CollapsingItemAdapter.HeaderViewHolder;
import com.kirana.creditkhata.Adapters.SettleDuesListAdapter;
import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.Modals.Users;

import org.zakariya.stickyheaders.StickyHeaderLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static android.content.Context.MODE_PRIVATE;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;

public class SettleActivity extends AppCompatActivity implements View.OnFocusChangeListener, CollapsingItemAdapter.DetailsChangedListener {

    private boolean phNoHasBeenEdited = false, amtHasBeenEdited = false, shouldShowError = false;
    private DatabaseReference mDatabaseRef;
    private RecyclerView mSettleRecyclerView;
    private CollapsingItemAdapter mAdapter;
    private TreeMap<String, ArrayList<Credits.creditVal>> mSettleDuesList;
    private TreeMap<String, Credits.creditVal> mList;
    private TextView noDues, totalAmt, editPhBtn;
    private AppCompatEditText mPh, mAmtRec, mSettleDetails;
    private Button mSettle, mAddNewCredit;
    private String custDetail;
    private ImageButton mExpandAll, mCollapseAll;
    private Map<String, Object> updateChildrenList; //a map containing all the credit nodes details to be updated

    private static final String STATE_SCROLL_POSITION = "SettleActivity.STATE_SCROLL_POSITION";
    public static final boolean SHOW_ADAPTER_POSITIONS = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settle_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        updateChildrenList = new HashMap<>();
        mSettleRecyclerView = (RecyclerView) findViewById(R.id.settle_dues_recycler);
        mSettleDuesList = new TreeMap<String, ArrayList<Credits.creditVal>>(Collections.reverseOrder()); //to use this treemap in reverse order
        mList = new TreeMap<String, Credits.creditVal>();   // acts as a default list to hold the raw data came from intent or loaded
        noDues = (TextView) findViewById(R.id.settle_noDues);
        totalAmt = findViewById(R.id.settle_due_val);
        mPh = (AppCompatEditText) findViewById(R.id.settle_phone_no);
        mAmtRec = (AppCompatEditText) findViewById(R.id.settle_amt);
        mSettle = (Button) findViewById(R.id.amt_settle_btn);
        editPhBtn = findViewById(R.id.edit_settle_ph);
        mAddNewCredit = (Button) findViewById(R.id.add_from_settle);
        mSettleDetails = (AppCompatEditText) findViewById(R.id.settle_details);

//        mExpandAll = findViewById(R.id.expand_all);
//        mCollapseAll = findViewById(R.id.collapse_all);

        SharedPreferences loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginPrefsEditor = loginPreferences.edit();
        String userPh = loginPreferences.getString("phNo",null);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/" + userPh + "/customers/");
        mDatabaseRef.keepSynced(true);

        mPh.setActivated(false);
        mAmtRec.setActivated(false);

        if (getIntent().getExtras() != null) {
            Bundle b = getIntent().getExtras();
            mList.putAll(new TreeMap<String, Credits.creditVal>((HashMap<String, Credits.creditVal>) b.get("credits")));
            grouping();
            custDetail = "" + b.get("name");
            mPh.setText("" + b.get("phNo"));
            custDetail += "\n" + mPh.getText();
            mPh.setEnabled(false);
//            totalAmt.setText("" + b.get("total"));
            mAdapter = new CollapsingItemAdapter(mSettleDuesList, mSettleDuesList.keySet().toArray(new String[mSettleDuesList.size()]),
                    this, custDetail, this);

            //set all items in section to be collapsed by default. Can be called only before setting the adapter to recycler.
            for (int i = 0; i < mAdapter.getNumberOfSections(); i++) {
                mAdapter.setSectionIsCollapsed(i, true);
            }

            mSettleRecyclerView.setLayoutManager(new StickyHeaderLayoutManager());
            mSettleRecyclerView.setAdapter(mAdapter);
            setUpDuesList();
        }

        mPh.setOnFocusChangeListener(this);
        mAmtRec.setOnFocusChangeListener(this);

        mAmtRec.setActivated(false);
        mPh.setActivated(false);

        mPh.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mPh.setActivated(false);
                phNoHasBeenEdited = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mAmtRec.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAmtRec.setActivated(false);
                amtHasBeenEdited = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mSettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settleDues();
            }
        });
        mAddNewCredit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettleActivity.this, AddCreditActivity.class);
                intent.putExtra("phNo", mPh.getText().toString());
                startActivityForResult(intent,100);
            }
        });
        hideSoftKeyboard();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //added to support recycler view with collapsible items
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        RecyclerView.LayoutManager lm = mSettleRecyclerView.getLayoutManager();
        Parcelable scrollState = lm.onSaveInstanceState();
        outState.putParcelable(STATE_SCROLL_POSITION, scrollState);
        super.onSaveInstanceState(outState);
    }

    //added to support recycler view with collapsible items
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mSettleRecyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(STATE_SCROLL_POSITION));
        }
    }

    public void settleDues() {
        if (TextUtils.isEmpty(mPh.getText())) {
            mPh.setActivated(true);
            Toast.makeText(this, "Please enter the Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(mAmtRec.getText())) {
            mAmtRec.setActivated(true);
            Toast.makeText(this, "Please enter the Amount Recieved", Toast.LENGTH_SHORT).show();
        }
        else {
            final Integer[] amt = {(int) Double.parseDouble(mAmtRec.getText().toString())};
            if (amt[0] > (int) Double.parseDouble("" + totalAmt.getText().toString().substring(2))) {
                mAmtRec.setActivated(true);
                Toast.makeText(SettleActivity.this, "Entered amount is larger than actual credit!!", Toast.LENGTH_SHORT).show();
            } else {
                final CustomDialogClass cdd = new CustomDialogClass(SettleActivity.this, 0,
                        "Confirmation ?", getResources().getString(R.string.settle_credit), null);
                cdd.show();
                cdd.yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //add to debits tab
                        final String dt = new SimpleDateFormat("yyMMdd").format(new Date());
                        final String time = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
                        Credits.creditVal dbt = new Credits.creditVal(String.format("%.2f", Double.parseDouble("" + amt[0])),
                                "" + mSettleDetails.getText(),time);
                        mDatabaseRef.child(mPh.getText() + "/debits/").child(dt + "_" + time).setValue(dbt);

                        //now also use the settle logic to update in credits tab
                        Map<String, Object> updateChildren = new HashMap<String, Object>(); //a map containing all the credit nodes to be updated
                        if (amt[0] == (int) Double.parseDouble("" + totalAmt.getText().toString().substring(2))) {
                            for (String k : mList.keySet()) {
                                Credits.creditVal c = mList.get(k);
                                c.setStatus("paid");
                                if (c.getActualAmt() != null) {
                                    c.setAmount(c.getActualAmt());
                                    c.setActualAmt(null);
                                }
                                updateChildren.put(k, c);
                            }
                        }
                        else {
                            for (String d : mList.keySet()) {
                                Credits.creditVal c = mList.get(d);
                                Integer dVal = (int) Double.parseDouble("" + c.getAmount());
                                if (amt[0] >= dVal) {
                                    amt[0] -= dVal;
                                    c.setStatus("paid");
                                    if (c.getActualAmt() != null) {
                                        c.setAmount(c.getActualAmt());
                                        c.setActualAmt(null);
                                    }
                                    updateChildren.put(d, c);
                                } else if (amt[0] != 0) {
                                    dVal -= amt[0];
                                    amt[0] = 0;
                                    if (c.getActualAmt() == null)
                                        c.setActualAmt(c.getAmount());
                                    c.setAmount(String.format("%.2f", Double.parseDouble("" + dVal)));
                                    updateChildren.put(d, c);
                                    break;
                                }
                            }
                        }
                        Log.e("---Update children ", "" + updateChildren);
                        final ProgressDialog pd = new ProgressDialog(SettleActivity.this);
                        pd.setTitle("Please Wait...");
                        pd.setMessage("Settling Dues");
                        pd.setCancelable(false);
                        pd.show();
                        mDatabaseRef.child(mPh.getText() + "/credits/").updateChildren(updateChildren).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Runnable progressRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.cancel();
                                        Toast.makeText(SettleActivity.this, "Dues Settled Successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                };
                                Handler pdCanceller = new Handler();
                                pdCanceller.postDelayed(progressRunnable, 800);
                            }
                        });
                        cdd.dismiss();
                    }
                });
            }
        }
    }

    private void setUpDuesList() {
        editPhBtn.setVisibility(View.GONE);
        if (mSettleDuesList.size() > 0) {
            noDues.setVisibility(View.GONE);
            mSettleRecyclerView.setVisibility(View.VISIBLE);
            runLayoutAnimation(mSettleRecyclerView);
        } else  {
            mSettleRecyclerView.setVisibility(View.GONE);
            noDues.setVisibility(View.VISIBLE);
        }
    }
    
    private void runLayoutAnimation(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_anim_slide_right);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.scheduleLayoutAnimation();
    }

    public void editPhone(View view) {
        editPhBtn.setVisibility(View.GONE);
        mPh.setEnabled(true);
        mPh.setText(null);
        mAmtRec.setText(null);
        totalAmt.setText("₹ 0");
        //clearing both lists
        mSettleDuesList.clear();
        mList.clear();
        mAdapter.notifyAllSectionsDataSetChanged(); //check the logic here when implemented
        setUpDuesList();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.settle_phone_no:
                shouldShowError = !hasFocus && !isPhoneValid() && phNoHasBeenEdited;
                if (shouldShowError)
                     mPh.setActivated(true);
                else if (!hasFocus && isPhoneValid() && phNoHasBeenEdited)
                     loadUserDues();
                break;

            case R.id.settle_amt:
                shouldShowError = !hasFocus && TextUtils.isEmpty(mAmtRec.getText()) && amtHasBeenEdited;
                if (shouldShowError)
                    mAmtRec.setActivated(true);
                break;

            default:
                break;
        }
    }

    private void loadUserDues() {
        final ProgressDialog pd = new ProgressDialog(SettleActivity.this);
        pd.setTitle("Please Wait...");
        pd.setMessage("Loading User Dues");
        pd.setCancelable(false);
        pd.show();
        final Query q = mDatabaseRef.child(mPh.getText() + "/credits/");
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //clearing both lists
                    mSettleDuesList.clear();
                    mList.clear();
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        mList.put(d.getKey(), d.getValue(Credits.creditVal.class));
                    }
                    Log.e("SettleCredit from query", "" + mList);
                    grouping();
                    mAdapter.notifyAllSectionsDataSetChanged(); //check the usage here is right or wrong
                    setUpDuesList();
                } else {
                    Toast.makeText(SettleActivity.this, "No Dues found with this Number", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SettleActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private boolean isPhoneValid() {
        String phNo = mPh.getText().toString();
        Matcher matcher = Pattern.compile("\\d{10}").matcher(phNo);
        return matcher.find();
    }

    private void hideSoftKeyboard() {
        final InputMethodManager inputManager = (InputMethodManager)SettleActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(new View(this).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void grouping() {
        mSettleDuesList.clear();
        Double updatedTotal = 0.0;
        for (String key: mList.keySet()) {
            Credits.creditVal val = mList.get(key);
            updatedTotal += Double.parseDouble(val.getAmount());
            //calc totalAmt here
            String mDate = key.split("_")[0];
            if (mSettleDuesList.containsKey(mDate)) {
                mSettleDuesList.get(mDate).add(val);
            }
            else {
                mSettleDuesList.put(mDate,new ArrayList<Credits.creditVal>(Collections.singleton(val)));
            }
        }
        totalAmt.setText("₹ " + String.format("%.2f", updatedTotal));
        Log.e( "Settle GroupedMap: " , "" + mSettleDuesList);
    }

    @Override
    public void onDetailsChanged(String date, Credits.creditVal item) {
        Log.e("SettleActivity","Listener called - Detail changed to " + item.toString() + " on " + date);
        updateChildrenList.put(date,item);
    }

    @Override
    public void onBackPressed() {
        if (updateChildrenList.size() > 0 ) {
            Log.e("---Updating Details", "" + updateChildrenList);
            final ProgressDialog pd = new ProgressDialog(SettleActivity.this);
            pd.setTitle("Please Wait...");
            pd.setMessage("Updating Details");
            pd.setCancelable(false);
            pd.show();
            mDatabaseRef.child(mPh.getText() + "/credits/").updateChildren(updateChildrenList).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Runnable progressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            Toast.makeText(SettleActivity.this, "Details updated successfully", Toast.LENGTH_SHORT).show();
//                            Intent i = new Intent(SettleActivity.this,MainActivity.class);
//                            Bundle bundle = new Bundle();
//                            bundle.putParcelableArrayList("update_details", mUpdateDetailsList);
//                            i.putExtras(bundle);
//                            setResult(1,i);
                            finish();
                        }
                    };
                    Handler pdCanceller = new Handler();
                    pdCanceller.postDelayed(progressRunnable, 800);
                }
            });
        }
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("Result code: ",resultCode + " and request code " + requestCode + " and data " + data);
        if(resultCode == 1) {
            Credits.creditVal val = (Credits.creditVal) data.getExtras().get("val");
            String date = data.getExtras().get("mDate").toString();
//            Double updatedTotal = Double.parseDouble(totalAmt.getText().toString().substring(1)) + Double.parseDouble(val.getAmount());
//            totalAmt.setText("₹ " + String.format("%.2f", updatedTotal));
            mList.put(date + "_" + val.getTransacTime(), val);
            grouping();
            //again set the collapsing adapter with updated list as data
            mAdapter = new CollapsingItemAdapter(mSettleDuesList, mSettleDuesList.keySet().toArray(new String[mSettleDuesList.size()]),
                    this, custDetail, this);
            //set all items in section to be collapsed by default. Can be called only before setting the adapter to recycler.
            for (int i = 0; i < mAdapter.getNumberOfSections(); i++) {
                mAdapter.setSectionIsCollapsed(i, true);
            }
            mSettleRecyclerView.setLayoutManager(new StickyHeaderLayoutManager());
            mSettleRecyclerView.setAdapter(mAdapter);
            setUpDuesList();
        }
    }
}
