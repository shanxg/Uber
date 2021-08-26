package com.lucasrivaldo.cloneuber.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.lucasrivaldo.cloneuber.R;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_PASSENGER;

public class UberHelper {

    public static final String START_TRIP = "SEARCH DRIVER";
    public static final String AWAITING = "Searching for driver";
    public static final String DRIVER_COMING = "Driver on the way";
    public static final String DRIVER_ARRIVED = "Driver arrived";
    public static final String PASSENGER_ABOARD = "Passenger aboard";
    public static final String ON_THE_WAY = "On the way to destination";
    public static final String TRIP_FINALIZED = "Trip finalized";

    public static final double UBER_X = 1;
    public static final double UBER_SLCT = 1.3;
    public static final double UBER_BLACK = 2;

    public static String[] neededPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void toggleProgressBar
            (String progressText, boolean isOpening, Activity activity) {

        LinearLayout mIncludeProgressBar = activity.findViewById(R.id.includeProgressBar);
        TextView mProgressText = activity.findViewById(R.id.progressText);

        if (progressText!=null)
            mProgressText.setText(progressText);

        mIncludeProgressBar.setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    public static String returnTypeName(double type, Resources resources){
        String typeText;

        if (type == UBER_X)
            typeText = resources.getString(R.string.text_uber_x);
        else if (type == UBER_SLCT)
            typeText = resources.getString(R.string.text_uber_select);
        else
            typeText = resources.getString(R.string.text_uber_black);

        return typeText;
    }

    public static int returnImageResourceId(double type){
        int resId;

        if (type == UBER_X)
            resId = R.drawable.uberx;
        else if (type == UBER_SLCT)
            resId = R.drawable.uber_select;
        else
            resId = R.drawable.uber_black;

        return resId;
    }

    public static void updateTypeButtons( int id, Activity activity){

        CircleImageView civ_uberX = activity.findViewById(R.id.civ_uberX);
        CircleImageView civ_uberSelect  = activity.findViewById(R.id.civ_uberSelect);
        CircleImageView civ_uberBlack = activity.findViewById(R.id.civ_uberBlack);


        int selectedColor = activity.getResources().getColor(R.color.btnSignIn);
        int defaultColor = activity.getResources().getColor(android.R.color.black);

        switch (id){
            case R.id.btnUberX:

                civ_uberX.setBorderColor(selectedColor);

                civ_uberSelect.setBorderColor(defaultColor);
                civ_uberBlack.setBorderColor(defaultColor);
                break;

            case R.id.btnUberSelect:

                civ_uberSelect.setBorderColor(selectedColor);

                civ_uberX.setBorderColor(defaultColor);
                civ_uberBlack.setBorderColor(defaultColor);
                break;

            case R.id.btnUberBlack:

                civ_uberBlack.setBorderColor(selectedColor);

                civ_uberX.setBorderColor(defaultColor);
                civ_uberSelect.setBorderColor(defaultColor);
                break;
        }
    }

    public static CircleOptions circleOptions(String uType , LatLng latLng, boolean isForTrip){
        double radius;

        if (uType.equals(TYPE_PASSENGER)){
            radius = isForTrip ? 0.015 : 0.5;
        }else {
            radius = isForTrip ? 0.015 : 1;
        }

        int color = isForTrip ?
                Color.argb(77, 0, 0, 255)
                : Color.argb(77, 0, 255, 0);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);  // MEASURED IN METERS
        circleOptions.strokeWidth(1);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(color);

        return circleOptions;
    }

    public static void hideKeyboardFrom(View view, Context context) {
        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }
}
