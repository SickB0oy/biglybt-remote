/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.biglybt.android.client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.biglybt.android.client.activity.ServiceStoppedActivity;
import com.biglybt.android.client.session.SessionManager;
import com.biglybt.android.util.BiglyCoreUtils;

/**
 * On/off switch of the torrent service, driven by the button on the home
 * screen. The "stopped" choice is remembered: opening the app or rebooting
 * the device does not start the service again until the user taps
 * "Start the service".
 */
public final class ServiceSwitch
{
	private static final String PREFS = "plus_prefs";

	private static final String KEY_STOPPED = "service_stopped";

	private ServiceSwitch() {
	}

	public static boolean isStopped(@NonNull Context context) {
		return context.getApplicationContext().getSharedPreferences(PREFS,
				Context.MODE_PRIVATE).getBoolean(KEY_STOPPED, false);
	}

	public static void setStopped(@NonNull Context context, boolean stopped) {
		context.getApplicationContext().getSharedPreferences(PREFS,
				Context.MODE_PRIVATE).edit().putBoolean(KEY_STOPPED, stopped).apply();
	}

	/** Stops the service and shows the "service stopped" screen */
	public static void stop(@NonNull Activity activity,
			@Nullable String remoteProfileID) {
		setStopped(activity, true);
		BiglyCoreUtils.shutdownCoreService();
		if (remoteProfileID != null) {
			SessionManager.removeSession(remoteProfileID, true);
		}
		showStoppedScreen(activity);
		activity.finish();
	}

	public static void showStoppedScreen(@NonNull Activity activity) {
		Intent i = new Intent(activity, ServiceStoppedActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
				| Intent.FLAG_ACTIVITY_NO_ANIMATION);
		activity.startActivity(i);
	}

	/** Starts the service again and opens the torrent list */
	public static void start(@NonNull Activity activity) {
		setStopped(activity, false);
		RemoteUtils.openRemoteList(activity);
		activity.finish();
	}
}
