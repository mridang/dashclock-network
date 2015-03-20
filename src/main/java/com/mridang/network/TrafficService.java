package com.mridang.network;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.acra.ACRA;

/**
 * Main service class that monitors the network speed and updates the
 * notification every second
 */
public class TrafficService extends Service {

	/**
	 * The handler class that runs every second to update the notification with
	 * the network speed. It also runs every minute to save the amount of
	 * data-transferred to the preferences.
	 */
	private static class NotificationHandler extends Handler {

		/**
		 * The value of the amount of data transferred in the previous
		 * invocation of the method
		 */
		private long lngPrevious = 0L;
		/**
		 * The instance of the preference editor to write the amount of data
		 * transferred
		 */
		private final Editor ediSettings;
		/** The amount of mobile data transferred before the previous boot */
		private final long lngMobile;
		/** The amount of total data transferred before the previous boot */
		private final long lngTotal;

		/**
		 * Simple constructor to initialize the initial value of the previous
		 */
		public NotificationHandler(Context ctxContext) {

			SharedPreferences speSettings = PreferenceManager.getDefaultSharedPreferences(ctxContext);
			ediSettings = speSettings.edit();
			lngPrevious = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
			lngMobile = speSettings.getLong("mobile", 0L);
			lngTotal = speSettings.getLong("total", 0L);

		}

		/**
		 * Handler method that updates the notification icon with the current
		 * speed. It is a very hackish method. We have icons for 1 KB/s to 999
		 * KB/s and 1.0 MB/s to 99.9 MB/s. Every time the method is invoked, we
		 * get the amount of data transferred. By subtracting this value with
		 * the previous value, we get the delta. Since this method is invoked
		 * every second, this delta value indicates the b/s. However, we need to
		 * convert this value into KB/s for values under 1 MB/s and we need to
		 * convert the value to MB/s for values over 1 MB/s. Since all our icon
		 * images are numbered sequentially we can assume that the R class
		 * generated will contain the integer references of the drawables in the
		 * sequential order.
		 */
		@Override
		public void handleMessage(Message msgMessage) {

			if (msgMessage.what == 2) {

				TrafficService.hndNotifier.sendEmptyMessageDelayed(2, 60000L);
				long lngMobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
				long lngTotal = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();

				ediSettings.putLong("mobile", lngMobile + this.lngMobile);
				ediSettings.putLong("total", lngTotal + this.lngTotal);
				ediSettings.commit();
				return;

			}

			TrafficService.hndNotifier.sendEmptyMessageDelayed(1, 1000L);

			long lngCurrent = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
			int lngSpeed = (int) (lngCurrent - lngPrevious);
			lngPrevious = lngCurrent;

			try {

				if (lngSpeed < 1024) {
					TrafficService.notBuilder.setSmallIcon(R.drawable.wkb000);
				} else if (lngSpeed < 1048576L) {

					TrafficService.notBuilder.setSmallIcon(R.drawable.wkb000 + (int) (lngSpeed / 1024L));
					if (lngSpeed > 1022976) {
						TrafficService.notBuilder.setSmallIcon(R.drawable.wkb000 + 1000);
					}

				} else if (lngSpeed <= 10485760) {
					TrafficService.notBuilder.setSmallIcon(990 + R.drawable.wkb000
							+ (int) (0.5D + (double) (10F * ((float) lngSpeed / 1048576F))));
				} else if (lngSpeed <= 103809024) {
					TrafficService.notBuilder.setSmallIcon(1080 + R.drawable.wkb000
							+ (int) (0.5D + (double) ((float) lngSpeed / 1048576F)));
				} else {
					TrafficService.notBuilder.setSmallIcon(1180 + R.drawable.wkb000);
				}
				TrafficService.mgrNotifications.notify(112, TrafficService.notBuilder.build());

			} catch (Exception e) {
				Log.e("NotificationHandler", "Error creating notification for speed " + lngSpeed);
			}

		}

	}

	/** The instance of the handler that updates the notification */
	private static NotificationHandler hndNotifier;
	/** The instance of the manager of the connectivity services */
	private static ConnectivityManager mgrConnectivity;
	/** The instance of the manager of the notification services */
	private static NotificationManager mgrNotifications;
	/** The instance of the manager of the wireless services */
	private static WifiManager mgrWireless;
	/** The instance of the manager of the telephony services */
	private static TelephonyManager mgrTelephony;
	/** The instance of the manager of the telephony services */
	private static PowerManager mgrPower;
	/** The instance of the notification builder to rebuild the notification */
	private static NotificationCompat.Builder notBuilder;
	/** The instance of the broadcast receiver to handle intents */
	private BroadcastReceiver recScreen;

	/**
	 * Initializes the service by getting instances of service managers and
	 * mainly setting up the receiver to receive all the necessary intents that
	 * this service is supposed to handle.
	 */
	@Override
	public void onCreate() {

		Log.i("TrafficService", "Starting the traffic service");
		ACRA.init(new AcraApplication(getApplicationContext()));
		super.onCreate();

		Log.d("TrafficService", "Setting up the service manager and the broadcast receiver");
		mgrConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mgrNotifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mgrWireless = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mgrTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mgrPower = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		hndNotifier = new NotificationHandler(getApplicationContext());
		hndNotifier.sendEmptyMessage(1);
		hndNotifier.sendEmptyMessage(2);
		recScreen = new BroadcastReceiver() {

			/**
			 * Handles the screen-on and the screen off intents to enable or
			 * disable the notification. We don't want to show the notification
			 * if the screen is off.
			 */
			@Override
			public void onReceive(Context ctcContext, Intent ittIntent) {

				if (ittIntent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {

					Log.d("TrafficService", "Screen off; hiding the notification");
					hndNotifier.removeMessages(1);
					mgrNotifications.cancel(112);

				} else if (ittIntent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {

					Log.d("TrafficService", "Screen on; showing the notification");
					connectivityUpdate();

				} else if (ittIntent.getAction().equalsIgnoreCase(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {

					if (ittIntent.getBooleanExtra("state", false)) {

						Log.d("TrafficService", "Airplane mode; hiding the notification");
						hndNotifier.removeMessages(1);
						hndNotifier.sendEmptyMessage(1);

					} else {

						Log.d("TrafficService", "Airplane mode; showing the notification");
						connectivityUpdate();

					}

				} else {

					Log.d("TrafficService", "Connectivity change; updating the notification");
					connectivityUpdate();
				}
			}
		};

		IntentFilter ittScreen = new IntentFilter();
		ittScreen.addAction(Intent.ACTION_SCREEN_ON);
		ittScreen.addAction(Intent.ACTION_SCREEN_OFF);
		ittScreen.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		ittScreen.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(recScreen, ittScreen);

	}

	/**
	 * Called when the service is being stopped. It doesn't do much except clear
	 * the message queue of the handler, hides the notification and unregisters
	 * the receivers.
	 */
	@Override
	public void onDestroy() {

		Log.d("TrafficService", "Stopping the traffic service");
		unregisterReceiver(recScreen);
		hndNotifier.removeMessages(1);
		hndNotifier.removeMessages(2);
		mgrNotifications.cancel(112);

	}

	@Override
	public int onStartCommand(Intent ittIntent, int flags, int startId) {

		Log.i("TrafficService", "Initializing the traffic service");

		Intent ittSettings = new Intent();
		ittSettings.setComponent(new ComponentName("com.android.settings",
				"com.android.settings.Settings$DataUsageSummaryActivity"));
		PendingIntent pitSettings = PendingIntent.getActivity(this, 0, ittSettings, 0);
		notBuilder = new NotificationCompat.Builder(this);
		notBuilder = notBuilder.setSmallIcon(R.drawable.ic_dashclock);
		notBuilder = notBuilder.setContentIntent(pitSettings);
		notBuilder = notBuilder.setOngoing(true);
		notBuilder = notBuilder.setWhen(0);
        notBuilder = notBuilder.setOnlyAlertOnce(true);
		notBuilder = notBuilder.setPriority(Integer.MAX_VALUE);
        notBuilder = notBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        notBuilder = notBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);

		connectivityUpdate();

		return super.onStartCommand(ittIntent, flags, startId);

	}

	/**
	 * Updates the notification with the new connectivity information. This
	 * method determines the type of connectivity and updates the notification
	 * with the network type and name. If there is no information about the
	 * active network, this will suppress the notification.
	 */
	private void connectivityUpdate() {

		NetworkInfo nifNetwork = mgrConnectivity.getActiveNetworkInfo();
		if (nifNetwork != null && nifNetwork.isConnectedOrConnecting()) {

			Log.d("TrafficService", "Network connected; showing the notification");
			if (nifNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

				Log.d("TrafficService", "Connected to a wireless network");
				WifiInfo wifInfo = mgrWireless.getConnectionInfo();
				if (wifInfo != null && !wifInfo.getSSID().trim().isEmpty()) {

					Log.d("TrafficService", wifInfo.getSSID());
					updateNotification(getString(R.string.wireless), wifInfo.getSSID().replaceAll("^\"|\"$", ""));

				} else {

					Log.d("TrafficService", "Unknown network without SSID");
					hndNotifier.removeMessages(1);
					mgrNotifications.cancel(112);

				}

			} else {

				Log.d("TrafficService", "Connected to a cellular network");
				if (!mgrTelephony.getNetworkOperatorName().trim().isEmpty()) {

					Log.d("TrafficService", mgrTelephony.getNetworkOperatorName());
					updateNotification(getString(R.string.cellular), mgrTelephony.getNetworkOperatorName());

				} else {

					Log.d("TrafficService", "Unknown network without IMSI");
					hndNotifier.removeMessages(1);
					mgrNotifications.cancel(112);

				}

			}

		} else {

			Log.d("TrafficService", "Network disconnected; hiding the notification");
			hndNotifier.removeMessages(1);
			mgrNotifications.cancel(112);

		}

	}

	/**
	 * Updates the title and message of the notification. This is invoked when
	 * the connectivity state changes from wireless to cellular or between
	 * networks.
	 * 
	 * @param strTitle The title of the notification explaining the type of
	 *            connectivity
	 * @param strMessage The title of the notification showing the name of the
	 *            network
	 */
	private void updateNotification(String strTitle, String strMessage) {

		notBuilder = notBuilder.setContentTitle(strTitle);
		notBuilder = notBuilder.setContentText(strMessage);
		if (mgrPower.isScreenOn()) {

			mgrNotifications.notify(112, notBuilder.build());
			hndNotifier.removeMessages(1);
			hndNotifier.sendEmptyMessage(1);

		}

	}

	/**
	 * Binder method to allow activities to bind to the service but since we
	 * don't have anything interacting with the service, we return null.
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intReason) {
		return null;
	}

}