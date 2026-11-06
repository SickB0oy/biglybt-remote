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
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/**
 * In-app language choice (system, French, English, German).
 * Uses AppCompat per-app locales: stored by AppCompat on Android 12 and
 * lower, handled by the system on Android 13+.
 */
public final class AppLanguage
{
	/** Empty tag = follow the system language */
	private static final String[] TAGS = {
		"",
		"fr",
		"en",
		"de"
	};

	private AppLanguage() {
	}

	/** Language tag chosen in the app, or "" for the system language */
	@NonNull
	public static String chosen() {
		LocaleListCompat list = AppCompatDelegate.getApplicationLocales();
		if (list.isEmpty()) {
			return "";
		}
		Locale l = list.get(0);
		return l == null ? "" : l.getLanguage();
	}

	/** Language actually in use: "fr", "en", "de"… */
	@NonNull
	public static String current() {
		String c = chosen();
		if (!c.isEmpty()) {
			return c;
		}
		Locale sys = ConfigurationCompat.getLocales(
				Resources.getSystem().getConfiguration()).get(0);
		return sys == null ? "en" : sys.getLanguage();
	}

	private static int chosenIndex() {
		String c = chosen();
		for (int i = 0; i < TAGS.length; i++) {
			if (TAGS[i].equals(c)) {
				return i;
			}
		}
		return 0;
	}

	@NonNull
	public static String label(@NonNull Activity activity) {
		return labels(activity)[chosenIndex()];
	}

	@NonNull
	private static String[] labels(@NonNull Activity activity) {
		return new String[] {
			activity.getString(R.string.language_system),
			"Français",
			"English",
			"Deutsch"
		};
	}

	public static void showPicker(@NonNull Activity activity) {
		new MaterialAlertDialogBuilder(activity).setTitle(
				R.string.preftitle_language).setSingleChoiceItems(labels(activity),
						chosenIndex(), (dialog, which) -> {
							dialog.dismiss();
							if (which == chosenIndex()) {
								return;
							}
							String tag = TAGS[which];
							// Recreates the open screens in the new language
							AppCompatDelegate.setApplicationLocales(tag.isEmpty()
									? LocaleListCompat.getEmptyLocaleList()
									: LocaleListCompat.forLanguageTags(tag));
						}).setNegativeButton(android.R.string.cancel, null).show();
	}
}
