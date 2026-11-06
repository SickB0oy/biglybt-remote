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
import android.app.Application;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.biglybt.android.client.activity.LibraryActivity;
import com.biglybt.android.client.activity.PlayerActivity;

/**
 * Android 15+ draws apps behind the status and navigation bars, and from
 * Android 16 (target API 36) apps can no longer opt out. The original screens
 * were not designed for that, so their content is padded by the size of the
 * system bars (and of the keyboard, which no longer resizes the window).
 * <p/>
 * The player (full screen) and the media library (own handling) are skipped.
 */
public final class SystemBarInsets
	implements Application.ActivityLifecycleCallbacks
{
	private static final int TAG_KEY = R.id.plus_insets_applied;

	public static void register(@NonNull Application app) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			app.registerActivityLifecycleCallbacks(new SystemBarInsets());
		}
	}

	private static boolean handlesItself(@NonNull Activity activity) {
		return activity instanceof PlayerActivity
				|| activity instanceof LibraryActivity;
	}

	@Override
	public void onActivityStarted(@NonNull Activity activity) {
		if (handlesItself(activity)) {
			return;
		}
		View content = activity.findViewById(android.R.id.content);
		if (content == null || content.getTag(TAG_KEY) != null) {
			return;
		}
		content.setTag(TAG_KEY, Boolean.TRUE);
		final boolean night = (activity.getResources().getConfiguration().uiMode
				& Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
		final WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
				activity.getWindow(), activity.getWindow().getDecorView());
		final int l = content.getPaddingLeft(), t = content.getPaddingTop(),
				r = content.getPaddingRight(), b = content.getPaddingBottom();
		ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
			Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.displayCutout());
			Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
			v.setPadding(l + bars.left, t + bars.top, r + bars.right,
					b + Math.max(bars.bottom, ime.bottom));
			if (bars.top > 0 || bars.bottom > 0) {
				// Drawn behind the bars: they show the window background now,
				// so pick icons that stay readable on it
				controller.setAppearanceLightStatusBars(!night);
				controller.setAppearanceLightNavigationBars(!night);
			}
			return WindowInsetsCompat.CONSUMED;
		});
		ViewCompat.requestApplyInsets(content);
	}

	@Override
	public void onActivityCreated(@NonNull Activity activity,
			@Nullable Bundle savedInstanceState) {
	}

	@Override
	public void onActivityResumed(@NonNull Activity activity) {
	}

	@Override
	public void onActivityPaused(@NonNull Activity activity) {
	}

	@Override
	public void onActivityStopped(@NonNull Activity activity) {
	}

	@Override
	public void onActivitySaveInstanceState(@NonNull Activity activity,
			@NonNull Bundle outState) {
	}

	@Override
	public void onActivityDestroyed(@NonNull Activity activity) {
	}
}
