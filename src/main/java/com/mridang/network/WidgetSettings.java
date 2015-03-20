package com.mridang.network;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/*
 * This class is the activity which contains the preferences
 */
public class WidgetSettings extends PreferenceActivity {

	/*
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("ConstantConditions")
    @Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getActionBar().setIcon(R.drawable.ic_dashclock);
		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	/*
	 * @see android.app.Activity#onPostCreate(android.os.Bundle)
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		findPreference("notification").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference prePreference, Object objValue) {

				Intent ittService = new Intent(getApplicationContext(), TrafficService.class);
				if ((Boolean) objValue) {
					getApplicationContext().startService(ittService);
				} else {
					getApplicationContext().stopService(ittService);
				}
				return true;

			}

		});

	}

}