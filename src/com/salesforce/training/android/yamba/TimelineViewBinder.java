package com.salesforce.training.android.yamba;

import java.util.Date;

import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.View;
import android.widget.TextView;

import com.salesforce.training.android.yamba.content.*;
public class TimelineViewBinder implements ViewBinder {

	@Override
    public boolean setViewValue(View view, Cursor cursor, int index) {
        if (index == cursor.getColumnIndex(YambaDBHelper.COL_TIMESTAMP)) {
            // get a locale based string for the date
            long date = cursor.getLong(index);
            Date dateObj = new Date(date * 1000);
            ((TextView) view).setText(android.text.format.DateFormat.getDateFormat(null).format(dateObj));
            return true;
        } else {
            return false;
        }
    }

}
