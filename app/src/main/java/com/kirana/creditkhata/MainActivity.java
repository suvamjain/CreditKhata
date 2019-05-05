package com.kirana.creditkhata;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.kirana.creditkhata.Adapters.DuesListAdapter;
import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.Modals.Dues;
import com.kirana.creditkhata.Modals.Users;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_SET_ALARM = 2;
    private static final int MY_PERMISSIONS_READ_PHONE_STATE = 3;

    private DatabaseReference mDatabaseRef;
    private RecyclerView mDuesRecyclerView;
    private DuesListAdapter mDuesAdapter;
    private ArrayList<Dues> mDuesList;
    private FloatingActionButton addCreditFab;
    private TextView noDues, toolTitle;
    private AlertDialog levelDialog;
    private ImageButton filterBtn, searchBtn, refreshBtn, overflowBtn;
    private AppCompatSpinner spinnerMode;
    private int sortAsc = 1;
    private String[] listArray;

    public SearchView searchView;
    public Toolbar toolbar;
    public Toolbar searchtoolbar;
    Menu search_menu;
    MenuItem item_search;
    private TextView noResultsFoundView;
    private FrameLayout mFrameLayout;

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            int noti_id = getIntent().getIntExtra("noti_id", -1);
            if (noti_id > 0) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(noti_id);
            }
        }

        // check user is logged in or not? get firebase auth instance
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Log.e("Main activity","user is null in onCreate, opening Login");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.e(TAG, "firebase user: " + user);
                if (user == null) {
                    Log.e("Main activity","Auth state changed, opening Login");
                    // user auth state is changed - user is null so launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setSearchtoolbar();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            Log.d("Perm:READ_PHONE_STATE", "Permission Denied");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_READ_PHONE_STATE);
        } else {
            Log.d("Perm:READ_PHONE_STATE", "Permission Exists");
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM)!= PackageManager.PERMISSION_GRANTED){
            Log.d("Perm check:SET_ALARM", "Permission Denied");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SET_ALARM}, MY_PERMISSIONS_REQUEST_SET_ALARM);
        } else {
            Log.d("Perm check:SET_ALARM", "Permission Exists");
        }

        SharedPreferences loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginPrefsEditor = loginPreferences.edit();
        String userPh = loginPreferences.getString("phNo",null);

        //Users u = new Gson().fromJson(loginPreferences.getString("User", null), Users.class);
//        if (u != null)
//            userPh = u.getU_ph();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("user_auth/" + userPh + "/customers/");
        mDatabaseRef.keepSynced(true);

        toolTitle = findViewById(R.id.toolTitle);
        searchBtn = findViewById(R.id.search_btn);
        filterBtn = findViewById(R.id.filter_btn);
        refreshBtn = findViewById(R.id.refresh_btn);
        overflowBtn = findViewById(R.id.overflow_btn);
        addCreditFab = (FloatingActionButton) findViewById(R.id.addCreditFab);
        mDuesRecyclerView = (RecyclerView) findViewById(R.id.dues_recycler);
        noDues = (TextView) findViewById(R.id.noDues);

        noResultsFoundView = findViewById(R.id.noResultsFoundView);
        mFrameLayout = (FrameLayout) findViewById(R.id.noSearchResults_container);

        searchBtn.setOnClickListener(this);
        filterBtn.setOnClickListener(this);
        refreshBtn.setOnClickListener(this);
        overflowBtn.setOnClickListener(this);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        mDuesRecyclerView.setLayoutManager(llm);
        mDuesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mDuesList = new ArrayList<Dues>();
        mDuesAdapter = new DuesListAdapter(mDuesList, this);
        mDuesRecyclerView.setAdapter(mDuesAdapter);
        //loadAllDues();  //--> commented as called by spinner's 'credit' option automatically on Start

        listArray = getResources().getStringArray(R.array.list_spinner);
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.list_spinner, R.layout.list_spinner);
        spinnerMode = findViewById(R.id.listSpinner);   //new Spinner(getSupportActionBar().getThemedContext());
        spinnerMode.setAdapter(spinnerAdapter);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(MainActivity.this, "you selected: " + listArray[position], Toast.LENGTH_SHORT).show();
                switch (listArray[position]) {
                    case "Credits":
                        Log.e(TAG, "Credits selected in Spinner");
                        toolTitle.setText("Credits Due");
                        loadAllDues();
                        break;

                    case "Debits":
                        Log.e(TAG, "Debits selected in Spinner");
                        toolTitle.setText("Debits Due");
                        mDuesList.clear();
                        mDuesAdapter.notifyDataSetChanged();
                        setUpDuesList();
                        break;

                    case "All":
                        Log.e(TAG, "All selected in Spinner");
                        toolTitle.setText("All Dues");
                        loadAllDues();
                        break;

                    default:
                        Log.e(TAG, "Nothing selected in Spinner");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        addCreditFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddCreditActivity.class));
            }
        });

        mDuesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && addCreditFab.isShown())
                    addCreditFab.hide();
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    addCreditFab.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public void loadAllDues() {
        mDatabaseRef.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mDuesList.clear();
                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        if (d.hasChild("credits")) {
                            Credits c = d.getValue(Credits.class);
                            TreeMap<String, Credits.creditVal> ct = new TreeMap<String, Credits.creditVal>(c.getCredits());
                            TreeMap<String, Credits.creditVal> credits = new TreeMap<>();
                            Double totalAmt = 0.0;
                            for (String k : ct.keySet()) {
                                Credits.creditVal cval = ct.get(k);
                                if (cval.getStatus() == null) {
                                    credits.put(k, cval);
                                    totalAmt += Double.parseDouble(cval.getAmount());
                                }
                            }
                            if (credits.size() > 0)
                                mDuesList.add(new Dues("" + totalAmt, c.getName(), d.getKey(), credits));
                            Log.e(TAG, "All Due Credits for " + c.getName() + "" + credits);
                        }
                    }
                    //sort the dues-list first based on oldest/newest dues on top
                    sortDuesList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        mDuesAdapter.notifyDataSetChanged();
        setUpDuesList();
    }

    private void setUpDuesList() {
        if (mDuesList.size() > 0) {
            noDues.setVisibility(View.GONE);
            mDuesRecyclerView.setVisibility(View.VISIBLE);
            mDuesRecyclerView.setHasFixedSize(true);
            runLayoutAnimation(mDuesRecyclerView);
            //addCreditFab.show();
        } else {
            //addCreditFab.hide();
            mDuesRecyclerView.setVisibility(View.GONE);
            noDues.setVisibility(View.VISIBLE);
        }
    }

    private void runLayoutAnimation(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.scheduleLayoutAnimation();
    }

    public void sortDuesList() {
        //sort the dues-list acc to sortAsc value;
        Collections.sort(mDuesList, new Comparator<Dues>() {
            @Override
            public int compare(Dues o1, Dues o2) {
                int a = o1.getDueCredits().firstKey().split("_")[0].compareTo(o2.getDueCredits().firstKey().split("_")[0]);
//              Log.e(TAG,"Comparing : " + o1.getFromDate() + " & " + o2.getFromDate() + " = " + a);
                return a * sortAsc;
                // -1 -> less than, 1 -> greater than, 0 -> equal, all inversed for descending
                //return o1.getFromDate() > o2.getFromDate() ? -1 : (o1.customInt < o2.customInt ) ? 1 : 0;
            }
        });
        mDuesAdapter.notifyDataSetChanged();
        setUpDuesList();
    }

    public void sendSMS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.SEND_SMS)) {
            }
            else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        } else {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);  //deliveredPI if wanted to track delivered or not
        }

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        //Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered\nPlease check the number", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSIONS_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Phone State Permission granted\nClick again to give perm", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Read Phone State Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            }

            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "SMS Permission granted\nClick again to send sms", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "SMS Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            }

            case MY_PERMISSIONS_REQUEST_SET_ALARM: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "ALARM Permission granted\nClick again to SET ALARM", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "ALARM Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_btn:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    circleReveal(R.id.searchtoolbar, 1, true, true);
                else
                    searchtoolbar.setVisibility(View.VISIBLE);

                item_search.expandActionView();
                break;

            case R.id.filter_btn:
                // Strings to Show In Dialog with Radio Buttons
                final CharSequence[] items = {"Oldest First", "Newest First"};
                // Creating and Building the Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Sort Dues");
                builder.setSingleChoiceItems(items, sortAsc == 1 ? 0 : 1, new DialogInterface.OnClickListener() { // mark the already selected filter
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                sortAsc = 1;
                                sortDuesList();
                                break;
                            case 1:
                                sortAsc = -1;
                                sortDuesList();
                                break;
                            default:
                                break;
                        }
                        levelDialog.cancel();
                    }
                });
                levelDialog = builder.create();
                levelDialog.show();
                break;

            case R.id.refresh_btn:
                loadAllDues();
                runLayoutAnimation(mDuesRecyclerView);
                break;

            case R.id.overflow_btn:
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, overflowBtn);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
//                        Toast.makeText(MainActivity.this,"You Clicked : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                        onOptionsItemSelected(item);
                        return true;
                    }
                });

                // Force icons to show
                Object menuHelper;
                Class[] argTypes;
                try {
                    Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
                    fMenuHelper.setAccessible(true);
                    menuHelper = fMenuHelper.get(popup);
                    argTypes = new Class[] { boolean.class };
                    menuHelper.getClass().getDeclaredMethod("setForceShowIcon",argTypes).invoke(menuHelper, true);
                } catch (Exception e) {
                    // Possible exceptions are NoSuchMethodError and NoSuchFieldError
                    // In either case, an exception indicates something is wrong with the reflection code, or the
                    // structure of the PopupMenu class or its dependencies has changed.
                    // These exceptions should never happen since we’re shipping the AppCompat library in our own apk,
                    // but in the case that they do, we simply can’t force icons to display, so log the error and show the menu normally.
                    Log.w(TAG, "error forcing menu icons to show", e);
                    popup.show();
                    return;
                }
                popup.show();//showing popup menu
                break;

            default:
                break;
        }
    }

    public void setSearchtoolbar() {
        searchtoolbar = (Toolbar) findViewById(R.id.searchtoolbar);
        if (searchtoolbar != null) {
            searchtoolbar.inflateMenu(R.menu.menu_search);
            search_menu = searchtoolbar.getMenu();

            searchtoolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        circleReveal(R.id.searchtoolbar, 1, true, false);
                    else
                        searchtoolbar.setVisibility(View.GONE);
                }
            });

            item_search = search_menu.findItem(R.id.action_filter_search);

            MenuItemCompat.setOnActionExpandListener(item_search, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // Do something when collapsed
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        circleReveal(R.id.searchtoolbar, 1, true, false);
                    } else
                        searchtoolbar.setVisibility(View.GONE);
                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Do something when expanded
                    return true;
                }
            });

            initSearchView();
        } else
            Log.e("toolbar", "setSearchtoolbar: NULL");
    }

    public void initSearchView() {
        searchView = (SearchView) search_menu.findItem(R.id.action_filter_search).getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // Enable/Disable Submit button in the keyboard
        searchView.setSubmitButtonEnabled(false);

        // Change search close button icon
        ImageView closeButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        closeButton.setImageResource(R.drawable.ic_close);
        closeButton.setColorFilter(getResources().getColor(R.color.white));

        // set hint and the text colors
        EditText txtSearch = ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        txtSearch.setHint(R.string.search_hint);
        txtSearch.setHintTextColor(getResources().getColor(R.color.colorPrimaryLight));
        txtSearch.setTextColor(getResources().getColor(R.color.white));

        // to remove the line below editText in search widget
        View searchPlateView = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        if (searchPlateView != null) {
            searchPlateView.setBackgroundColor(Color.TRANSPARENT);
        }

        // set the cursor
        AutoCompleteTextView searchTextView = (AutoCompleteTextView) txtSearch;
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            //Below line sets the cursor resource ID to 0 or @null which will make it visible on white background
            mCursorDrawableRes.set(searchTextView, R.drawable.search_cursor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mDuesAdapter.getFilter().filter(query);
                searchView.clearFocus();  //required to hide keyboard on search btn click
                return true;  //if false is returned keyboard opens new activity on clicking search btn in keyboard.
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mDuesAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void circleReveal(int viewID, int posFromRight, boolean containsOverflow, final boolean isShow) {
        final View myView = findViewById(viewID);

        int width = myView.getWidth();

        if (posFromRight > 0)
            width -= (posFromRight * getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_action_button_min_width_material)) - (getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_action_button_min_width_material) / 2);
        if (containsOverflow)
            width -= getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_action_button_min_width_overflow_material);

        int cx = width;
        int cy = myView.getHeight() / 2;

        Animator anim;
        if (isShow)
            anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, (float) width);
        else
            anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, (float) width, 0);

        anim.setDuration((long) 220);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isShow) {
                    super.onAnimationEnd(animation);
                    myView.setVisibility(View.INVISIBLE);
                }
            }
        });

        // make the view visible and start the animation
        if (isShow)
            myView.setVisibility(View.VISIBLE);

        // start the animation
        anim.start();
    }

    public void showNoSearchResults(int v, String s) {
        noResultsFoundView.setText(getResources().getString(R.string.no_search_results, s));
        mFrameLayout.setVisibility(v);
    }

    // Function to read the result from newly created activity (i.e. SettleActivity)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("Result code: ",resultCode + " and request code " + requestCode);
        if(resultCode == 1) {
//            mAddedUser.addAll(data.getExtras().<Credits.creditVal>getParcelableArrayList("selected_users")); //update the newly_selected_users to added_users_list
//            mDuesAdapter.notifyDataSetChanged();
            Log.e("SettleActivityResults-",
                    " updated after results : " + data.getExtras().<Credits.creditVal>getParcelableArrayList("update_details"));
        }
    }

    @Override
    public void onBackPressed() {
        if (searchtoolbar.getVisibility() == View.VISIBLE) {
            EditText text = searchView.findViewById(R.id.search_src_text);
            text.setText("");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                circleReveal(R.id.searchtoolbar, 1, true, false);
            else
                searchtoolbar.setVisibility(View.GONE);
            return;
        } else {
            finish();
        }
        super.onBackPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
        Log.e(TAG,"auth in onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
//            ((AlarmTestActivity)this).cancelAlarm();   //cancel the alarm service before logging out
            auth.signOut();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, AlarmTestActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}