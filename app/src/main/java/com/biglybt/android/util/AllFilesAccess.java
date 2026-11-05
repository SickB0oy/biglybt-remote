/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.biglybt.android.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.biglybt.android.client.BuildConfig;
import com.biglybt.android.client.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Asks for "All files access" (MANAGE_EXTERNAL_STORAGE) on Android 11+.
 * <p>
 * Without it, the torrent core can't create folders on USB drives or SD cards,
 * so every multi-file torrent fails with "Folder Permissions Needed"
 * (BiglyBT-Android issue #250), which is very common on Android TV.
 */
public class AllFilesAccess
{
	private static final String TAG = "AllFilesAccess";

	private static final String PREFS = "all_files_access";

	private static final String KEY_DONT_ASK = "dont_ask";

	private static boolean shownThisRun = false;

	public static boolean isGranted() {
		return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
	}

	/**
	 * Shows the explanation dialog once per app run, unless already granted,
	 * the user said "don't ask again", or this build has no local core.
	 */
	public static void maybeAsk(@NonNull Activity activity) {
		if (Build.VERSION.SDK_INT < 30 || isGranted() || shownThisRun) {
			return;
		}
		if (!"coreFlavor".equals(BuildConfig.FLAVOR_coreD)) {
			return;
		}
		SharedPreferences prefs = activity.getSharedPreferences(PREFS,
				Context.MODE_PRIVATE);
		if (prefs.getBoolean(KEY_DONT_ASK, false)) {
			return;
		}
		if (activity.isFinishing()) {
			return;
		}
		shownThisRun = true;

		AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.all_files_access_title)
				.setMessage(R.string.all_files_access_message)
				.setPositiveButton(R.string.all_files_access_open,
						(d, which) -> openSettings(activity))
				.setNegativeButton(R.string.all_files_access_later, null)
				.setNeutralButton(R.string.all_files_access_never,
						(d, which) -> prefs.edit().putBoolean(KEY_DONT_ASK, true).apply())
				.create();
		dialog.show();
	}

	public static void openSettings(@NonNull Activity activity) {
		if (Build.VERSION.SDK_INT < 30) {
			return;
		}
		Intent perApp = new Intent(
				Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
				Uri.parse("package:" + activity.getPackageName()));
		try {
			activity.startActivity(perApp);
			return;
		} catch (ActivityNotFoundException | SecurityException e) {
			Log.w(TAG, "Per-app all files access screen not available", e);
		}
		// Some Android TV builds only have the global list
		try {
			activity.startActivity(
					new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
			return;
		} catch (ActivityNotFoundException | SecurityException e) {
			Log.w(TAG, "All files access screen not available", e);
		}
		// Last resort: explain how to grant it with adb
		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.all_files_access_title)
				.setMessage(activity.getString(R.string.all_files_access_no_screen,
						activity.getPackageName()))
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}
}
