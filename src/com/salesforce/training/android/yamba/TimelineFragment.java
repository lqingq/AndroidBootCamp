package com.salesforce.training.android.yamba;

import com.salesforce.training.android.yamba.content.YambaContract;
import com.salesforce.training.android.yamba.services.YambaService;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TimelineFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	private static final int TIMELINE_LOADER = 999;
	private static final String[] PROJ = {
		YambaContract.Timeline.Columns.ID,
		YambaContract.Timeline.Columns.USER, 
		YambaContract.Timeline.Columns.STATUS, 
		YambaContract.Timeline.Columns.TIMESTAMP };
	private static final String[] FROM = new String[PROJ.length - 1];
	private static final int[] TO = {R.id.timeline_user, R.id.timeline_status, R.id.timeline_timestamp};

	static {
		System.arraycopy(PROJ, 1, FROM, 0, PROJ.length - 1);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		
		// display the cursor in list view
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
				R.layout.timline_row, 
				null, 
				FROM, 
				TO,
				0);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int index) {
				if (R.id.timeline_timestamp != view.getId()) {
					return false;
				}
				long t = cursor.getLong(index);
				String dateAsString = null;
				if (t > 0) {
//					Date dateObj = new Date(cursor.getLong(index));
//		            ((TextView) view).setText(dateFormatter.format(dateObj));
		            
					dateAsString = DateUtils.getRelativeTimeSpanString(t, System.currentTimeMillis(), 0).toString();
				}
				((TextView) view).setText(dateAsString);
				
				return true;
		    }
		});
		this.setListAdapter(adapter);
		
		getLoaderManager().initLoader(TIMELINE_LOADER, null, this);
		return null;
	}

	

	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), YambaContract.Timeline.URI, PROJ, null, null, YambaContract.Timeline.Columns.TIMESTAMP + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		((SimpleCursorAdapter)this.getListAdapter()).swapCursor(arg1);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		 ((SimpleCursorAdapter) getListAdapter()).swapCursor(null);	
	}
}
