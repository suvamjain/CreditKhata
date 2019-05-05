package com.kirana.creditkhata.Adapters;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kirana.creditkhata.Modals.Credits;
import com.kirana.creditkhata.R;
import com.kirana.creditkhata.SettleActivity;

import org.zakariya.stickyheaders.SectioningAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

public class CollapsingItemAdapter extends SectioningAdapter {

    static final String TAG = CollapsingItemAdapter.class.getSimpleName();

    public ArrayList<Section> sections = new ArrayList<>();
    TreeMap<String, ArrayList<Credits.creditVal>> mGroupedMap;
    private String[] mDuesDateKey;
    private Double val = 0.0;
    private Context mContext;
    private LayoutInflater inflater;
    private String customerDetail;
    private DetailsChangedListener listener;

    private class Section {
        String headerDate;
        ArrayList<Credits.creditVal> items = new ArrayList<>();
    }

    public CollapsingItemAdapter (TreeMap<String, ArrayList<Credits.creditVal>> GroupedMap, String[] keySet, Context mContext,
                                  String custDetail, DetailsChangedListener listener)
    {
        this.mGroupedMap = GroupedMap;
        this.mContext = mContext;
        this.listener = listener;
        this.inflater = LayoutInflater.from(mContext);
        this.mDuesDateKey = keySet;
        this.customerDetail = custDetail;  //to show in popup

        Log.e("Collapsing Adapter", "" + mDuesDateKey.length);
        for(String dt: mDuesDateKey) {
            appendSection(dt);
        }
    }

    void appendSection(String headerDate) {
        Section section = new Section();
        //section.index = index;
        section.headerDate = headerDate;
        section.items.addAll(mGroupedMap.get(headerDate));
        sections.add(section);
    }

    public class ItemViewHolder extends SectioningAdapter.ItemViewHolder implements View.OnClickListener {
        TextView mDetail, mAmount;
        ImageView mIcon;
        LinearLayout mView;

        ItemViewHolder(View itemView) {
            super(itemView);
            mView = (LinearLayout) itemView.findViewById(R.id.settle_item_bg);
            mDetail = (TextView) itemView.findViewById(R.id.settle_item_detail);
            mAmount = (TextView) itemView.findViewById(R.id.settle_item_amt);
            mIcon = (ImageView) itemView.findViewById(R.id.settle_item_icon);
            mView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final boolean[] detailEdited = {false};
            int adapterPosition = getAdapterPosition();
            final int section = CollapsingItemAdapter.this.getSectionForAdapterPosition(adapterPosition);
            final int item = CollapsingItemAdapter.this.getPositionOfItemInSection(section, adapterPosition);
            final Section s = sections.get(section); //get the item section object for feeding popup views
            // Edit code here;
            final Dialog myDialog;
            myDialog = new Dialog(mContext);
            myDialog.setContentView(R.layout.settle_detail_popup);
            ProgressBar prog = myDialog.findViewById(R.id.details_progress);
            prog.setVisibility(View.VISIBLE);
            TextView txtclose = (TextView) myDialog.findViewById(R.id.txtclose);
            txtclose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //mDetail.setText(((EditText)myDialog.findViewById(R.id.dialog_detail_view)).getText().toString());

                    //update the edited details in sections ArrayList's specific section item to reflect changes everywhere
                    sections.get(section).items.get(item).setDetail(((EditText)myDialog.findViewById(R.id.dialog_detail_view)).getText().toString());
                    myDialog.dismiss();
                    if (detailEdited[0])
                        listener.onDetailsChanged(s.headerDate + "_" + s.items.get(item).getTransacTime(), sections.get(section).items.get(item));
                    notifySectionDataSetChanged(section);
                }
            });
            TextView dialogDate = myDialog.findViewById(R.id.dialogDate);
            TextView dialogTime = myDialog.findViewById(R.id.dialogTime);
            try {
                final Date dt = new SimpleDateFormat("yyMMdd").parse(s.headerDate);
                dialogDate.setText(new SimpleDateFormat("dd MMMM yy").format(dt));
                final  Date tm = new SimpleDateFormat("HHmmss").parse(s.items.get(item).getTransacTime());
                dialogTime.setText(new SimpleDateFormat("hh:mm a").format(tm));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            TextView dialogAmt = myDialog.findViewById(R.id.dialogAmt);
            dialogAmt.setText(mAmount.getText() + " Cr");
            TextView customerDetails = myDialog.findViewById(R.id.customer_details);
            customerDetails.setText(customerDetail);
            EditText dialogDetail = myDialog.findViewById(R.id.dialog_detail_view);
            dialogDetail.setText(mDetail.getText());
            prog.setVisibility(View.GONE);
            myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            myDialog.setCancelable(false);
            myDialog.show();

            dialogDetail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    detailEdited[0] = true;
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    public class HeaderViewHolder extends SectioningAdapter.HeaderViewHolder implements View.OnClickListener {
        TextView mDate, mTotal, mItemsCount;
        ImageButton collapseButton;

        HeaderViewHolder(View itemView) {
            super(itemView);
            mDate = (TextView) itemView.findViewById(R.id.settle_header_date);
            mTotal = (TextView) itemView.findViewById(R.id.settle_header_total);
            mItemsCount = (TextView) itemView.findViewById(R.id.settle_header_count);
            collapseButton = (ImageButton) itemView.findViewById(R.id.collapseButton);
            collapseButton.setOnClickListener(this);

//            if (!CollapsingItemAdapter.this.showAdapterPositions) {
//                deleteButton.setVisibility(View.INVISIBLE);
//                adapterPositionTextView.setVisibility(View.INVISIBLE);
//            }
        }

        void updateSectionCollapseToggle(boolean sectionIsCollapsed) {
            @DrawableRes int id = sectionIsCollapsed ? R.drawable.ic_expand_more_black_24dp : R.drawable.ic_expand_less_black_24dp;
            collapseButton.setImageDrawable(ContextCompat.getDrawable(collapseButton.getContext(), id));
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            final int section = CollapsingItemAdapter.this.getSectionForAdapterPosition(position);
            if (v == collapseButton) {
                CollapsingItemAdapter.this.onToggleSectionCollapse(section);
                updateSectionCollapseToggle(CollapsingItemAdapter.this.isSectionCollapsed(section));
            }
        }
    }

    //footer here

//    public void collapseHeaders(boolean b) {
//        for(int i=0; i<getNumberOfSections(); i++) {
//            setSectionIsCollapsed(i, b);
//            //notifyDataSetChanged();
//        }
//        notifyAllSectionsDataSetChanged();
//    }
//
//    void onDeleteSection(int sectionIndex) {
////        Log.d(TAG, "onDeleteSection() called with: " + "sectionIndex = [" + sectionIndex + "]");
////        //sections.remove(sectionIndex);
////        //notifySectionRemoved(sectionIndex);
////    }
////
////    void onDeleteItem(int sectionIndex, int itemIndex) {
////        Log.d(TAG, "onDeleteItem() called with: " + "sectionIndex = [" + sectionIndex + "], itemIndex = [" + itemIndex + "]");
//////        CollapsingItemAdapter.Section s = sections.get(sectionIndex);
//////        s.items.remove(itemIndex);
//////        notifySectionItemRemoved(sectionIndex, itemIndex);
////    }

    void onToggleSectionCollapse(int sectionIndex) {
        Log.d(TAG, "onToggleSectionCollapse() called with: " + "sectionIndex = [" + sectionIndex + "]");
        setSectionIsCollapsed(sectionIndex, !isSectionCollapsed(sectionIndex));
    }

    @Override
    public int getNumberOfSections() {
        return sections.size();
    }

    @Override
    public int getNumberOfItemsInSection(int sectionIndex) { return sections.get(sectionIndex).items.size(); }

    @Override
    public boolean doesSectionHaveHeader(int sectionIndex) { return !TextUtils.isEmpty(sections.get(sectionIndex).headerDate); }

    @Override
    public CollapsingItemAdapter.ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.settle_item, parent, false);
        return new CollapsingItemAdapter.ItemViewHolder(v);
    }

    @Override
    public CollapsingItemAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.settle_header, parent, false);
        return new CollapsingItemAdapter.HeaderViewHolder(v);
    }

    //added this GhostHeaderViewHolder's in onCreate method to solve the problem of crashing due to attachment of views to parent
    @Override
    public GhostHeaderViewHolder onCreateGhostHeaderViewHolder(ViewGroup parent) {
        final View ghostView = new View(parent.getContext());
        ghostView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new GhostHeaderViewHolder(ghostView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindItemViewHolder(SectioningAdapter.ItemViewHolder viewHolder, int sectionIndex, int itemIndex, int itemType) {
        CollapsingItemAdapter.Section s = sections.get(sectionIndex);
        CollapsingItemAdapter.ItemViewHolder ivh = (CollapsingItemAdapter.ItemViewHolder) viewHolder;
        if (itemIndex > -1) {
            //ivh.mView.setBackgroundColor(mContext.getResources().getColor(R.color.green_bg));
            ivh.mIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.red_text), android.graphics.PorterDuff.Mode.SRC_IN);
            ivh.mAmount.setTextColor(mContext.getResources().getColor(R.color.red_text));
        }
        else {
            //ivh.mView.setBackgroundColor(mContext.getResources().getColor(R.color.red_bg));
            ivh.mIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimaryDark), android.graphics.PorterDuff.Mode.SRC_IN);
            ivh.mAmount.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
        }
        ivh.mDetail.setText("" + s.items.get(itemIndex).getDetail());
        ivh.mAmount.setText("₹ " + s.items.get(itemIndex).getAmount());
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindHeaderViewHolder(SectioningAdapter.HeaderViewHolder viewHolder, int sectionIndex, int headerType) {
        CollapsingItemAdapter.Section s = sections.get(sectionIndex);
        CollapsingItemAdapter.HeaderViewHolder hvh = (CollapsingItemAdapter.HeaderViewHolder) viewHolder;

        try {
            final Date dt = new SimpleDateFormat("yyMMdd").parse(s.headerDate);
            hvh.mDate.setText(new SimpleDateFormat("dd-MMM-yy").format(dt));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        double totalAmt = 0.0;
        for (Credits.creditVal cval : s.items)
            totalAmt += Double.parseDouble(cval.getAmount());

        hvh.mTotal.setText("₹ " + totalAmt);
        hvh.mItemsCount.setText(s.items.size() + " visits");
        hvh.updateSectionCollapseToggle(isSectionCollapsed(sectionIndex));
    }

    @Override
    public void onBindGhostHeaderViewHolder(SectioningAdapter.GhostHeaderViewHolder viewHolder, int sectionIndex) {
//        if (USE_DEBUG_APPEARANCE) {
//            viewHolder.itemView.setBackgroundColor(0xFF9999FF);
//        }
    }

    public interface DetailsChangedListener {
        void onDetailsChanged(String date, Credits.creditVal item);
    }
}
