package com.kirana.creditkhata.Adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.kirana.creditkhata.MainActivity;
import com.kirana.creditkhata.Modals.Dues;
import com.kirana.creditkhata.R;
import com.kirana.creditkhata.SettleActivity;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class DuesListAdapter extends RecyclerView.Adapter<DuesListAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "DuesAdapter";
    private ArrayList<Dues> mAllDues;
    private Context mContext;
    private LayoutInflater inflater;
    private List<Dues> DuesListFiltered;
    private String searchedText = "";

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName, mPhNo, mFromDt, mAmtDue;
        Button mCall, mMsg, mSettle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mPhNo = itemView.findViewById(R.id.number);
            mAmtDue = itemView.findViewById(R.id.amountDue);
            mFromDt = itemView.findViewById(R.id.fromDate);
            mCall = itemView.findViewById(R.id.call_btn);
            mMsg = itemView.findViewById(R.id.msg_btn);
            mSettle = itemView.findViewById(R.id.settle_btn);
        }
    }

    public DuesListAdapter(ArrayList<Dues> mAllDues, Context mContext) {
        this.mAllDues = mAllDues;
        this.DuesListFiltered = mAllDues;
        this.mContext = mContext;
        this.inflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dues_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        //final Dues dueItem = mAllDues.get(position);  //changed for search

        final Dues dueItem = DuesListFiltered.get(position);

//        Double totalAmt = 0.0;
//        final TreeMap<String,Credits.creditVal> credits = dueItem.getDueCredits(); //new TreeMap<>();
//        final TreeMap<String,Credits.creditVal> actualMap = dueItem.getDueCredits();
//        for (String d: actualMap.keySet()) {
//            Credits.creditVal c = actualMap.get(d);
//            if (c.getStatus() == null) {
//                credits.put(d,c);
//                totalAmt += Double.parseDouble(c.getAmount());
//            }
//        }

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {  //also on tapping the cardView item open settleActivity
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SettleActivity.class);
                intent.putExtra("credits", dueItem.getDueCredits());
                intent.putExtra("name", dueItem.getName());
                intent.putExtra("phNo", dueItem.getPhNo());
                intent.putExtra("total", "₹ " + String.format("%.2f", Double.parseDouble(dueItem.getAmtDue())));
                mContext.startActivity(intent);
//                ((MainActivity)mContext).startActivityForResult(intent, 100);

                //hide the search bar before opening settle activity
                Toolbar searchtoolbar = ((MainActivity)mContext).searchtoolbar;
                if (searchtoolbar.getVisibility() == View.VISIBLE) {
                    EditText text = ((MainActivity) mContext).searchView.findViewById(R.id.search_src_text);
                    text.setText("");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        ((MainActivity) mContext).circleReveal(R.id.searchtoolbar, 1, true, false);
                    else
                        searchtoolbar.setVisibility(View.GONE);
                }
            }
        });

        viewHolder.mName.setText(dueItem.getName());
        viewHolder.mPhNo.setText(dueItem.getPhNo());

        highlightString(searchedText,viewHolder.mName);
        highlightString(searchedText,viewHolder.mPhNo);

        viewHolder.mAmtDue.setText("₹ " + String.format("%.2f", Double.parseDouble(dueItem.getAmtDue())));
//        viewHolder.mFromDt.setText("From - " + dueItem.getFromDate());
        int days = Days.daysBetween(new DateTime(dueItem.getActualDate()), new DateTime(new Date())).getDays();
        String lastPaid = days > 0 ? days + " days" : "today";
        viewHolder.mFromDt.setText("Since " + lastPaid);

        viewHolder.mCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("Calling -- ", dueItem.getPhNo() + " at pos " + viewHolder.getAdapterPosition());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+ dueItem.getPhNo()));
                mContext.startActivity(intent);
            }
        });

        viewHolder.mMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)mContext).sendSMS("+91" + dueItem.getPhNo(),
                        "Shopkeeper requested you to pay " + viewHolder.mAmtDue.getText() + " pending " + viewHolder.mFromDt.getText());
            }
        });

        viewHolder.mSettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SettleActivity.class);
                intent.putExtra("credits", dueItem.getDueCredits());
                intent.putExtra("phNo", dueItem.getPhNo());
                intent.putExtra("total", "₹ " + String.format("%.2f", Double.parseDouble(dueItem.getAmtDue())));
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
//        return mAllDues.size();  //changed for search
        return DuesListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString().trim();
                searchedText = charString.toLowerCase().trim();
                if (charString.isEmpty()) {
                    DuesListFiltered = mAllDues;
                } else {
                    List<Dues> filteredList = new ArrayList<>();
                    for (Dues row : mAllDues) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (row.getName().toLowerCase().contains(charString.toLowerCase()) || row.getPhNo().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    DuesListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = DuesListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                DuesListFiltered = (ArrayList<Dues>) filterResults.values;

                if (DuesListFiltered.size() == 0 && mAllDues.size() > 0)
                    ((MainActivity)mContext).showNoSearchResults(View.VISIBLE,charSequence.toString());
                else
                    ((MainActivity)mContext).showNoSearchResults(View.GONE,charSequence.toString());

                // refresh the list with filtered data
                notifyDataSetChanged();
            }
        };
    }

    private void highlightString(String input, TextView mTextView) {

        String text = mTextView.getText().toString().toLowerCase();
        if (text.contains(input)) {
            //Log.e("test", text + " contains: " + input);
            int startPos = text.indexOf(input);
            int endPos = startPos + input.length();

            Spannable spanText = Spannable.Factory.getInstance().newSpannable(mTextView.getText()); // <- EDITED: Use the original string, as `country` has been converted to lowercase.
            spanText.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.red_text)), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            mTextView.setText(spanText, TextView.BufferType.SPANNABLE);
        }
    }
}
