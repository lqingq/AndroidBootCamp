package com.salesforce.training.android.yamba.services;

import java.util.ArrayList;
import java.util.List;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClient.Status;
import com.marakana.android.yamba.clientlib.YambaClientException;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.salesforce.training.android.yamba.content.*;

public class YambaService extends IntentService {
	private static final String TAG = "YambaService";
	private static final int MAX_RESULTS = 100;
	
	private static final String PARAM_OP = "YambaService.OP";
	private static final int OP_POST = 6001;
	private static final int OP_POLL = 6002;
	private static final int OP_START_POLL = 6003;
	private static final int OP_STOP_POLL = 6004;
	
	private static final String PARAM_STATUS = "YambaService.STATUS";
	private static final String PARAM_MESSENGER = "YambaService.MESSENGER";
	
	private YambaClient mClient;
	
	// Polling constants
	private static final int POLL_INTERVAL = 60 * 1000; //1 minute
	private static final int POLL_INTENT_REQUESTCODE = 42;
	
	public YambaService() {
		super(TAG);
	}

	// Create and start a new intent for post message
	public static void post(Context context, String messageToPost, Messenger messenger) {
		Intent i = new Intent(context, YambaService.class);
		i.putExtra(PARAM_OP, OP_POST);
		i.putExtra(PARAM_STATUS, messageToPost);
		if (null != messenger) {
			i.putExtra(PARAM_MESSENGER, messenger);
		}
		context.startService(i);
	}
	
	// Create and start polling
	public static void startPolling(Context context) {
		Intent i = new Intent(context, YambaService.class);
		i.putExtra(PARAM_OP, OP_START_POLL);
		context.startService(i);
	}
	
	// Create and stop polling
	public static void stopPolling(Context context) {
		Intent i = new Intent(context, YambaService.class);
		i.putExtra(PARAM_OP, OP_STOP_POLL);
		context.startService(i);
	}
	
	private PendingIntent getPendingPollIntent() {
		Intent i = new Intent(this, YambaService.class);
		i.putExtra(PARAM_OP, OP_POLL);
		return PendingIntent.getService(this, POLL_INTENT_REQUESTCODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		int op = intent.getIntExtra(PARAM_OP, 0);
		switch(op) {
			case OP_POST:
				doPost(intent);
				break;
			case OP_START_POLL:
				schedulePolling();
				break;
			case OP_POLL:
				doPoll(intent);
				break;
			case OP_STOP_POLL:
				doStopPolling();
				break;
			default:
		}
		Log.d(TAG, "OnHandleIntent called on thread " + Thread.currentThread());
	}
	
	private synchronized YambaClient getClient() {
		if (null == mClient) {
			mClient = new YambaClient("student", "password", "http://yamba.marakana.com/api");
		}
		return mClient;
	}
	
	private Boolean postMessage(String message) {
		try {
			YambaClient client = this.getClient();
			client.postStatus(message);
			return Boolean.valueOf(true);
		} catch(YambaClientException e) {
			Log.e(TAG, e.getLocalizedMessage());
			e.printStackTrace();
			return Boolean.valueOf(false);
		}
	}
	
	private synchronized void doPost(Intent intent) {
		String msgToSend = intent.getStringExtra(PARAM_STATUS);
		Boolean result = this.postMessage(msgToSend);
		
		Messenger messenger = (Messenger) intent.getExtras().get(PARAM_MESSENGER);
		if (null != messenger) {
			Message msgToReturn = Message.obtain();
			msgToReturn.obj = result;
			try {
		         messenger.send(msgToReturn);
		    } catch (RemoteException e) {
		        Log.i(TAG, e.getLocalizedMessage());
		    }
		}
	}
	
	private synchronized List<Status> doPoll(Intent intent) {
		List<Status> timeline = null;
		try {
			YambaClient client = this.getClient();
			timeline = client.getTimeline(MAX_RESULTS);
			
			Log.d(TAG, "Get timeline, total is # " + timeline.size());
			
//			for (Status status: timeline) {
//	            Log.d(TAG, "Status: " + status.getMessage());
//	        }
			
			processTimeline(timeline);
		} catch(YambaClientException e) {
			Log.e(TAG, e.getLocalizedMessage());
			e.printStackTrace();
		}
		return timeline;
	}
	
	// Polling method
	private void schedulePolling() {
		Log.d(TAG, "Schedule polling");
		AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		
		alarmMgr.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 100, //ensure it runs in the future clock
				POLL_INTERVAL, this.getPendingPollIntent());
	}
	
	private void doStopPolling() {
		AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(this.getPendingPollIntent());
	}
	
	// Find the timestamp for the most recent record in the database
    // Build a list of new records for the database, omitting any record that is already there
    // Use the list of database records to perform a bulk update on the database
    private void processTimeline(List<Status> timeline) {
        long mostRecentStatus = getMostRecentTimestamp();

        List<ContentValues> update = new ArrayList<ContentValues>();
        for (Status status: timeline) {
            long t = status.getCreatedAt().getTime();
            if (t <= mostRecentStatus) { continue; }

            ContentValues vals = new ContentValues();
            vals.put(YambaContract.Timeline.Columns.TIMESTAMP, t);
            vals.put(YambaContract.Timeline.Columns.ID, status.getId());
            vals.put(YambaContract.Timeline.Columns.USER, status.getUser());
            vals.put(YambaContract.Timeline.Columns.STATUS, status.getMessage());
            update.add(vals);
        }

        if (0 < update.size()) {
            int n = getContentResolver().bulkInsert(
                    YambaContract.Timeline.URI,
                    update.toArray(new ContentValues[update.size()]));
            Log.d(TAG, n + " statuses inserted");
        }
    }
    
 // find the most recent timestamp in the database
    // SQL: SELECT max_timestamp FROM uri;
    private long getMostRecentTimestamp() {
        Cursor c = getContentResolver().query(
                YambaContract.Timeline.URI,
                new String[] { YambaContract.Timeline.Columns.MAX_TIMESTAMP },
                null,
                null,
                null);

        long t = Long.MIN_VALUE;
        if (null != c) {
            if (c.moveToNext()) { t = c.getLong(0); }
            c.close();
        }

        Log.d(TAG, "latest record at time: " + t);

        return t;
    }
}
