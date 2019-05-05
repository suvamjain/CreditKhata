//package com.kirana.creditkhata;
//
//import android.os.Bundle;
//import android.util.Log;
//
//import com.kirana.creditkhata.Adapters.CollapsingItemAdapter;
//import com.kirana.creditkhata.Adapters.SimpleDemoAdapter;
//import com.kirana.creditkhata.Modals.Credits;
//
//import org.zakariya.stickyheaders.StickyHeaderLayoutManager;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.TreeMap;
//
///**
// * Created by shamyl on 6/7/16.
// */
//public class CollapsingSectionsDemoActivity extends DemoActivity {
//
//	SimpleDemoAdapter adapter;
//	private CollapsingItemAdapter mAdapter;
//
//	private TreeMap<String, ArrayList<Credits.creditVal>> mSettleDuesList;
//	private TreeMap<String, Credits.creditVal> mList;
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//
//		super.onCreate(savedInstanceState);
//
//		mList = new TreeMap<String, Credits.creditVal>();
//		mSettleDuesList = new TreeMap<String, ArrayList<Credits.creditVal>>();
//
//		if (getIntent().getExtras() != null) {
//			Bundle b = getIntent().getExtras();
//			mList.putAll(new TreeMap<String, Credits.creditVal>((HashMap<String, Credits.creditVal>) b.get("credits")));
//			grouping();
//		}
//
////		adapter = new SimpleDemoAdapter(100, 5, false, false, true, SHOW_ADAPTER_POSITIONS);
////		recyclerView.setLayoutManager(new StickyHeaderLayoutManager());
////		recyclerView.setAdapter(adapter);
//
//		mAdapter = new CollapsingItemAdapter(mSettleDuesList, mSettleDuesList.keySet().toArray(new String[mSettleDuesList.size()]), this, "", this);
//		recyclerView.setLayoutManager(new StickyHeaderLayoutManager());
//		recyclerView.setAdapter(mAdapter);
//
//	}
//
//	private void grouping() {
//		for (String key: mList.keySet()) {
//			Credits.creditVal val = mList.get(key);
//			String mDate = key.split("_")[0];
//			if (mSettleDuesList.containsKey(mDate)) {
//				mSettleDuesList.get(mDate).add(val);
//			}
//			else {
//				mSettleDuesList.put(mDate,new ArrayList<Credits.creditVal>(Collections.singleton(val)));
//			}
//		}
//		Log.e( "Result Grouped Map: " , "" + mSettleDuesList);
//	}
//}
