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

package com.biglybt.android.client.media;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

/**
 * One libVLC instance for the whole app. Creating it loads the native
 * libraries and scans the codecs, which takes a noticeable moment: it is done
 * once, in the background, as soon as a screen that can play videos opens,
 * so pressing "Play" starts the video right away.
 */
public final class PlayerEngine
{
	private static final String TAG = "PlayerEngine";

	private static LibVLC libVLC;

	private PlayerEngine() {
	}

	@NonNull
	public static synchronized LibVLC get(@NonNull Context context) {
		if (libVLC == null) {
			ArrayList<String> options = new ArrayList<>();
			// Local files: short read-ahead, the disk is fast
			options.add("--file-caching=300");
			// Streams from this device (partial downloads): still short
			options.add("--network-caching=1000");
			options.add("--audio-time-stretch");
			// Most French subtitle files that aren't UTF-8 are Windows-1252
			options.add("--subsdec-encoding=Windows-1252");
			libVLC = new LibVLC(context.getApplicationContext(), options);
		}
		return libVLC;
	}

	/** Prepares libVLC in the background, ahead of the first "Play" */
	public static void warmUp(@NonNull Context context) {
		final Context app = context.getApplicationContext();
		new Thread(() -> {
			try {
				get(app);
			} catch (Throwable t) {
				Log.w(TAG, "warmUp", t);
			}
		}, "PlayerEngine.warmUp").start();
	}
}
