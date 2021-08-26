package com.lucasrivaldo.cloneuber.helper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.lucasrivaldo.cloneuber.R;

public class BitmapHelper {

    public static BitmapDescriptor describeFromVector(
            @DrawableRes int vectorDrawableResourceId, Context pmContext) {

        // FOR CUSTOM BACKGROUND VECTOR
        /*
        Drawable background =
                ContextCompat.getDrawable(context, R.drawable.ic_map_pin_filled_blue_48dp);
        background.setBounds
                (0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());

                Bitmap bitmap =
                Bitmap.createBitmap
                        (background.getIntrinsicWidth(),
                                background.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);
         */

        Drawable vectorDrawable = ContextCompat.getDrawable(pmContext, vectorDrawableResourceId);
        vectorDrawable.setBounds(
                0,
                0,
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());

        Bitmap bitmap =
                Bitmap.createBitmap
                        (vectorDrawable.getIntrinsicWidth(),
                                vectorDrawable.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(bitmap);
        //background.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    public static Bitmap rotateDriverIconBitmap(Resources resources,double angle) {

        Bitmap source = BitmapFactory.decodeResource(resources, R.drawable.icons_carr);
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angle);

        return Bitmap.createBitmap
                (source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
