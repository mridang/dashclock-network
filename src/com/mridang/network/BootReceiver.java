package com.mridang.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

/**
 * Broadcast receiver class to help start the traffic monitoring service when the phone boots up
 */
public class BootReceiver extends BroadcastReceiver {

	/**
	 * Receiver method for the phone bootup that starts the traffic monitoring service
	 */
	@Override
	public void onReceive(Context ctxContext, Intent ittIntent) {
	    if (PreferenceManager.getDefaultSharedPreferences(ctxContext).getBoolean("notification", true)) {
	    	ctxContext.startService(new Intent(ctxContext, TrafficService.class));
		}
	}

}