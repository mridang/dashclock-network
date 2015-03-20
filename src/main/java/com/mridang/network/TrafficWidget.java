package com.mridang.network;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;

import com.google.android.apps.dashclock.api.ExtensionData;

import org.acra.ACRA;

import java.util.Calendar;
import java.util.Date;

/*
 * This class is the main class that provides the widget
 */
public class TrafficWidget extends ImprovedExtension {

    /* The amount of mobile data transferred before the previous boot */
    private Long lngMobile;
    /* The amount of total data transferred before the previous boot */
    private Long lngTotal;

    /*
     * (non-Javadoc)
     * @see com.mridang.network.ImprovedExtension#getIntents()
     */
    @Override
    protected IntentFilter getIntents() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.mridang.network.ImprovedExtension#getTag()
     */
    @Override
    protected String getTag() {
        return getClass().getSimpleName();
    }

    /*
     * (non-Javadoc)
     * @see com.mridang.network.ImprovedExtension#getUris()
     */
    @Override
    protected String[] getUris() {
        return null;
    }

    /*
     * @see
     * com.google.android.apps.dashclock.api.DashClockExtension#onInitialize
     * (boolean)
     */
    @Override
    protected void onInitialize(boolean booReconnect) {

        Log.d(getTag(), "Loading data-transfer statistics from file");
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

        Log.d(getTag(), "Checking if the service is running");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo rsiService : manager.getRunningServices(Integer.MAX_VALUE)) {

            if (TrafficService.class.getName().equals(rsiService.service.getClassName())) {
                super.onInitialize(booReconnect);
                return;
            }

        }

        Log.d(getTag(), "Starting the service since it isn't running");
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

        Log.d(getTag(), "Calculating the amount of data transferred");
        ExtensionData edtInformation = new ExtensionData();
        setUpdateWhenScreenOn(true);

        try {

            ComponentName cmpActivity = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");

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
            edtInformation.clickIntent(new Intent().setComponent(cmpActivity));

        } catch (Exception e) {
            edtInformation.visible(false);
            Log.e(getTag(), "Encountered an error", e);
            ACRA.getErrorReporter().handleSilentException(e);
        }

        edtInformation.icon(R.drawable.ic_dashclock);
        doUpdate(edtInformation);

    }

    /*
     * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
     */
    @Override
    public void onDestroy() {

        Log.d(getTag(), "Stopping the service if it is running");
        getApplicationContext().stopService(new Intent(getApplicationContext(), TrafficService.class));
        super.onDestroy();

    }

    /*
     * (non-Javadoc)
     * @see com.mridang.network.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
     */
    @Override
    protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {
        onUpdateData(UPDATE_REASON_MANUAL);
    }

}