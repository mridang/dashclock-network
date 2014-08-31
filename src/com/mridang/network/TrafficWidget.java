package com.mridang.network;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.acra.ACRA;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class TrafficWidget extends DashClockExtension {

	/* The amount of mobile data transferred before the previous boot */
	Long lngMobile;
	/* The amount of total data transferred before the previous boot */
	Long lngTotal;

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onInitialize
	 * (boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		Log.d("TrafficWidget", "Loading data-transfer statistics from file");
		SharedPreferences speSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		lngMobile = speSettings.getLong("mobile", 0L);
		lngTotal = speSettings.getLong("total", 0L);

		Calendar calCalendar = Calendar.getInstance();
		calCalendar.setTime(new Date());
		calCalendar.add(Calendar.MONTH, 1);
		calCalendar.set(Calendar.DAY_OF_MONTH, 1);
		calCalendar.add(Calendar.DATE, -1);

		AlarmManager mgrAlarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent ittReset = new Intent(this, ResetReceiver.class);
		PendingIntent pitReset = PendingIntent.getBroadcast(this, 0, ittReset, PendingIntent.FLAG_UPDATE_CURRENT);
		mgrAlarms.set(AlarmManager.RTC_WAKEUP, calCalendar.getTimeInMillis(), pitReset);

		Log.d("TrafficWidget", "Checking if the service is running");
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo rsiService : manager.getRunningServices(Integer.MAX_VALUE)) {

			if (TrafficService.class.getName().equals(rsiService.service.getClassName())) {
				super.onInitialize(booReconnect);
				return;
			}

		}

		Log.d("TrafficWidget", "Starting the service since it isn't running");
		getApplicationContext().startService(new Intent(getApplicationContext(), TrafficService.class));
		super.onInitialize(booReconnect);

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d("TrafficWidget", "Calculating the amount of data tranferred");
		ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(true);

		try {

			Long lngMobile = this.lngMobile + TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
			Long lngTotal = this.lngTotal + TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
			Long lngWifi = lngTotal - lngMobile;

			String strMobile = Formatter.formatFileSize(getApplicationContext(), lngMobile);
			String strTotal = Formatter.formatFileSize(getApplicationContext(), lngTotal);
			String strWifi = Formatter.formatFileSize(getApplicationContext(), lngWifi);

			edtInformation.expandedTitle(String.format(getString(R.string.status), strTotal));
			edtInformation.status(strTotal);
			edtInformation.expandedBody(String.format(getString(R.string.message), strMobile, strWifi));
			edtInformation.visible(true);

			if (new Random().nextInt(5) == 0 && !(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0);

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri
								.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation
								.expandedBody("Thank you for using "
										+ intExtensions
										+ " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(true);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("TrafficWidget", "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("TrafficWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	@Override
	public void onDestroy() {

		Log.d("TrafficWidget", "Stopping the service if it is running");
		getApplicationContext().stopService(new Intent(getApplicationContext(), TrafficService.class));
		super.onDestroy();
		Log.d("TrafficWidget", "Destroyed");

	}

}