package com.salesforce.training.android.yamba;

import java.lang.ref.WeakReference;

import com.marakana.android.yamba.clientlib.YambaClient;
import com.marakana.android.yamba.clientlib.YambaClientException;
import com.salesforce.training.android.yamba.services.YambaService;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.app.Activity;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StatusActivity extends Activity {
	private TextView countLabel;
	public TextView submitStatusText;
	private EditText statusText;
	private static final int MAXIMUM_COUNT = 50;
	private static final String LOG_TAG = "StatusActivity";
	private boolean pollingStarted = false;
	
	private void updateActivityWithPostStatus(Boolean result) {
		if (result) {
			this.statusText.setText("");
			String text = this.getString(R.string.message_submit_successful);
			Toast.makeText(this, text, Toast.LENGTH_LONG).show();
			this.submitStatusText.setTextColor(Color.GREEN);
		} else { 
			this.submitStatusText.setTextColor(Color.RED);
			}	    	
		this.submitStatusText.setText(String.format("Message sent %s", result ? "successfully" : "failed"));
	}
	
	// wire up button on click
	private static class YambaServiceHandler extends Handler {
		WeakReference<StatusActivity> mActivity;

		public YambaServiceHandler(StatusActivity aActivity) {
			mActivity = new WeakReference<StatusActivity>(aActivity);
        }
		
	    @Override
	    public void handleMessage(Message msg) {
	    	StatusActivity theActivity = mActivity.get();
	    	Boolean result = (Boolean) msg.obj;
            theActivity.updateActivityWithPostStatus(result);
	   }
	}
			
	// AsyncTask to send post
	private static class SendPostTask extends AsyncTask<String, Integer, Boolean> {
		WeakReference<StatusActivity> mActivity;
		public SendPostTask(StatusActivity aActivity) {
			mActivity = new WeakReference<StatusActivity>(aActivity);
        }
		@Override
		protected Boolean doInBackground(String... params) {
			YambaClient client = new YambaClient("student", "password", "http://yamba.marakana.com/api");
			try {
				client.postStatus(params[0]);
				return Boolean.valueOf(true);
			} catch(YambaClientException e) {
				Log.e(LOG_TAG, e.getLocalizedMessage());
				e.printStackTrace();
				return Boolean.valueOf(false);
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			StatusActivity theActivity = mActivity.get();
			theActivity.updateActivityWithPostStatus(result);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_status);
		
		//wire up
		statusText = (EditText) this.findViewById(R.id.editText1);
		countLabel = (TextView) this.findViewById(R.id.countText);
		submitStatusText = (TextView) this.findViewById(R.id.submitStatus);
		countLabel.setText(String.valueOf(MAXIMUM_COUNT));
		
		statusText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				int remaingCount = MAXIMUM_COUNT - s.length();
				countLabel.setText(String.valueOf(remaingCount));
				if (remaingCount <= 0) {
					countLabel.setTextColor(Color.RED);
				} else if (remaingCount <= 10) {
					countLabel.setTextColor(Color.MAGENTA);
				} else {
					countLabel.setTextColor(Color.GREEN);
				}
			}
		});
		
		Button button = (Button)this.findViewById(R.id.buttonSubmit);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				String statusMsg = statusText.getText().toString().trim();
				String errorMsg = null;
				int statusMsgLen = statusMsg.length();
				
				// validate status message
				if (statusMsgLen == 0) {
					errorMsg = "Can not sumbit empty status";
				} else if (statusMsgLen > MAXIMUM_COUNT) {
					errorMsg = "Status message cannot be longer than " + MAXIMUM_COUNT;
				}
				
				if (null != errorMsg) {
					submitStatusText.setTextColor(Color.RED);
					submitStatusText.setText(errorMsg);
				} else {
					submitStatusText.setTextColor(Color.GREEN);
					submitStatusText.setText("Sending message ...");
//					new SendPostTask((StatusActivity.this).execute(statusText.getText().toString());
					YambaService.post(StatusActivity.this, statusMsg, new Messenger(new YambaServiceHandler(StatusActivity.this)));
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.status, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (pollingStarted) {
			pollingStarted = false;
			YambaService.stopPolling(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!pollingStarted) {
			pollingStarted = true;
			YambaService.startPolling(this);
		}
	}
}


