package com.salesforce.training.android.yamba.receiver;

import com.salesforce.training.android.yamba.services.YambaService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		YambaService.startPolling(context);
	}

}
