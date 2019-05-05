package com.kirana.creditkhata.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.R;
import com.kirana.creditkhata.SettleActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

public class SettleDuesListAdapter extends RecyclerView.Adapter<SettleDuesListAdapter.ViewHolder> {

    private static final String TAG = "SettleDuesAdapter";
    private TreeMap<String, Credits.creditVal> mAllDues;
    private String[] mDuesDateKey;
    private Double val = 0.0;
    private Context mContext;
    private LayoutInflater inflater;

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mDate, mVisit, mAmtDue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mDate = itemView.findViewById(R.id.settle_card_dt);
            mVisit = itemView.findViewById(R.id.settle_card_visit);
            mAmtDue = itemView.findViewById(R.id.settle_card_amt);
            mDuesDateKey = mAllDues.keySet().toArray(new String[mAllDues.size()]);
        }
    }

    public SettleDuesListAdapter(TreeMap<String, Credits.creditVal> mAllDues, Context mContext) {
        this.mAllDues = mAllDues;
        this.mContext = mContext;
        this.inflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.settle_details, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {

        final Credits.creditVal dueItem = mAllDues.get(mDuesDateKey[position]);
        String[] date_visit = mDuesDateKey[position].split("_");
        try {
            final Date dt = new SimpleDateFormat("yyMMdd").parse(date_visit[0]);
            viewHolder.mDate.setText(new SimpleDateFormat("dd-MM-yy").format(dt));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String amt = dueItem.getAmount();
        val += Double.parseDouble(amt);
        viewHolder.mVisit.setText(date_visit[1]);
        viewHolder.mAmtDue.setText("₹ " + amt);

        //find some logic when needed to implement settle activity for a custom ph input
//        if (position==mAllDues.size()-1)
//            ((SettleActivity)mContext).setTotalFromAdapter("₹ "+ val);
    }

    @Override
    public int getItemCount() {
        return mAllDues.size();
    }
}
