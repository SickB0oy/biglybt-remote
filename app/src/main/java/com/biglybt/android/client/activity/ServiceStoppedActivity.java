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

package com.biglybt.android.client.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.biglybt.android.client.R;
import com.biglybt.android.client.ServiceSwitch;

/**
 * Shown while the torrent service is stopped: one green button to start it
 * again, and a shortcut to the movies already downloaded.
 */
public class ServiceStoppedActivity
	extends AppCompatActivity
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_service_stopped);

		View start = findViewById(R.id.service_start);
		start.setOnClickListener(v -> ServiceSwitch.start(this));
		start.requestFocus();

		findViewById(R.id.service_library).setOnClickListener(
				v -> LibraryActivity.open(this, null));
	}
}
