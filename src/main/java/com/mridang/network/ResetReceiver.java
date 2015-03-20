package com.mridang.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Broadcast receiver class to reset the statistics using an alarm at the end of
 * every month
 */
public class ResetReceiver extends BroadcastReceiver {

	/**
	 * Receiver method for the end of the month to reset all the statistics
	 */
	@Override
	public void onReceive(Context ctxContext, Intent ittIntent) {

		Editor ediSettings = PreferenceManager.getDefaultSharedPreferences(ctxContext).edit();
		ediSettings.putLong("mobile", 0);
		ediSettings.putLong("total", 0);
		ediSettings.commit();

	}

}