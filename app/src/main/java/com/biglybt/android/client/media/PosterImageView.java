/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package com.biglybt.android.client.media;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** Image view that keeps the 2:3 shape of a movie poster */
public class PosterImageView
	extends AppCompatImageView
{
	public PosterImageView(Context context) {
		super(context);
	}

	public PosterImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PosterImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = w * 3 / 2;
		super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
	}
}
