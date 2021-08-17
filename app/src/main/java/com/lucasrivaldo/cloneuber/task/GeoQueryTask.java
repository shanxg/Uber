package com.lucasrivaldo.cloneuber.task;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoQuery;
import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.model.User;

public class GeoQueryTask extends AsyncTask<Object, Object, Object> {

    private boolean mIsForTrip;
    private double mRadius;

    private GeoFire mGeoFire;
    private GeoQuery mGeoQuery;

    private TaskLoadedCallback mTaskLoadedCallback;
    private User mLoggedUser;

    public GeoQueryTask(Context mTaskLoadedCallback, User mLoggedUser) {
        this.mTaskLoadedCallback = (TaskLoadedCallback) mTaskLoadedCallback;
        this.mLoggedUser = mLoggedUser;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mLoggedUser.getType().equals(ConfigurateFirebase.TYPE_PASSENGER)){
            mRadius =  mIsForTrip? 0.015 : 0.5; // MEASURED IN KM (15m && 500m)
        }else {
            mRadius =  mIsForTrip? 0.015 : 1; // MEASURED IN KM (15m && 500m)
        }
    }

    @Override
    protected Object doInBackground(Object... objects) {
        for (Object object : objects){
            if (object.getClass().equals(String.class)){

            }

        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        mTaskLoadedCallback.onTaskDone(o);


    }


    /*
    private CircleOptions circleOptions(LatLng latLng, boolean isForTrip){
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(isForTrip ? 20 : 1000);  // MEASURED IN METERS
        circleOptions.strokeWidth(1);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(isForTrip ?
                Color.argb(77, 0, 0, 255)
                : Color.argb(77, 0, 255, 0));

        return circleOptions;
    }

    private GeoQueryEventListener requestsQueryEventListener(){
        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (requestList.contains(key))
                    getTripsData(key);

            }

            @Override
            public void onKeyExited(String key) {}
            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {

                throwToast("GEOQUERY READY", false);

                // ADDING CIRCLE FOR DEMONSTRATION


                if (queryReqsCircle != null)
                    queryReqsCircle.remove();
                //queryReqsCircle = mMap.addCircle(circleOptions);


            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                try {
                    throw error.toException();
                } catch (Exception e) {
                    throwToast(e.getMessage(), true);
                    e.printStackTrace();
                    Log.d("USERTESTERROR", "onGeoQueryError: " + e.toString());
                }
            }
        };
    }
    */


    /*
{

    private GeoQueryEventListener getGeoQueryEventListeners
            (CircleOptions circleOptions, boolean isForTrip) {

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!isForTrip) {

                    if (requestList.contains(key))
                        getTripsData(key);

                }else {

                    if (key.equals(currentTrip.getTripId()))
                        updateTripStatus(checkStatus().equals(Trip.DRIVER_COMING) ?
                                Trip.DRIVER_ARRIVED : Trip.TRIP_FINALIZED);
                }
            }

            @Override
            public void onKeyExited(String key) {}
            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {

                throwToast("GEOQUERY READY", false);

                // ADDING CIRCLE FOR DEMONSTRATION
                if (isForTrip) {

                    if (queryTripCircle != null)
                        queryTripCircle.remove();
                    queryTripCircle = mMap.addCircle(circleOptions);

                } else {

                    if (queryReqsCircle != null)
                        queryReqsCircle.remove();
                    queryReqsCircle = mMap.addCircle(circleOptions);

                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                try {
                    throw error.toException();
                } catch (Exception e) {
                    throwToast(e.getMessage(), true);
                    e.printStackTrace();
                    Log.d("USERTESTERROR", "onGeoQueryError: " + e.toString());
                }
            }
        };
    }



    private GeoQueryEventListener setGeoLocationQueryListener
            (double latitude, double longitude, boolean isForTrip) {

        /*
        geoQuery = ConfigurateFirebase.getGeoFire()
                .queryAtLocation(
                        new GeoLocation(latitude, longitude),
                        // (20M FOR TRIP && 1KM IF SEARCHING TRIPS)RADIUS MEASURED IN KM
                        isForTrip ? 0.02 : 1 );

        /*

        tripList.clear();

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(new LatLng(latitude, longitude));
        circleOptions.radius(isForTrip ? 20 : 1000);  // MEASURED IN METERS
        circleOptions.strokeWidth(1);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(isForTrip ?
                Color.argb(77, 0, 0, 255)
                : Color.argb(77, 0, 255, 0));

        return getGeoQueryEventListeners(circleOptions, isForTrip);

        /*
        //if (isForTrip){
            //if (tripQueryEventListener == null) {
                  tripQueryEventListener = getGeoQueryEventListeners(circleOptions, true);
                //geoQuery.addGeoQueryEventListener(tripQueryEventListener);
            //}

        //}else {
            //if (reqsQueryEventListener == null) {
                  reqsQueryEventListener = getGeoQueryEventListeners(circleOptions, false);
                //geoQuery.addGeoQueryEventListener(reqsQueryEventListener);
            //}
        //}


    }

}
*/



}





