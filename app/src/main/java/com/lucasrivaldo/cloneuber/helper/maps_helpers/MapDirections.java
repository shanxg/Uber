package com.lucasrivaldo.cloneuber.helper.maps_helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;



import static com.lucasrivaldo.cloneuber.activity.MainActivity.GOOGLE_MAPS_KEY;

public class MapDirections{

    private static final String MAPS_API_URL = "https://maps.googleapis.com/maps/api/";

    public static final String DISTANCE = "distanceText";
    public static final String DISTANCEv = "distanceValue";
    public static final String DURATION = "durationText";
    public static final String DURATIONv = "durationValue";

    public static String getMapsURL(LatLng origin, LatLng destination){
        String url;

        String originLoc = "origin="+origin.latitude+","+origin.longitude;
        String destLoc = "destination="+destination.latitude+","+destination.longitude;
        String mode = "mode=driving";

        String output = "json?";
        String params = originLoc+"&"+destLoc+"&"+mode;
        String key = "&key="+ GOOGLE_MAPS_KEY;

        url = MAPS_API_URL +"directions/"+output+params+key;

        return url;
    }
}

/**   SOME TESTS I'VE MADE ALONG THE PROCESS TO GET THE DIRECTIONS RESULTS FOR TEXT AND POLYLINE
 *
 ////////////////////////////////////// DEPENDENCIES FOR TESTS /////////////////////////////////////
 *
 *  // RetroFit & RX
 *     implementation 'com.squareup.retrofit2:retrofit:2.4.0'
 *     implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
 *     implementation 'com.squareup.retrofit2:adapter-rxjava2:2.4.0'
 *
 *     implementation 'io.reactivex.rxjava2:rxjava:2.2.2'
 *     implementation 'com.jakewharton.rxrelay2:rxrelay:2.0.0'
 *     implementation 'io.reactivex.rxjava2:rxandroid:2.0.2'
 *

  ////////////////////////////////////// MAP SERVICE INTERFACE /////////////////////////////////////

 * public interface MapService {
 *
 *     @GET("distancematrix/json")
 *     Single<Result> getDirectionsSingle(@Query("key")String key,
 *                                        @Query("origins")String origins,
 *                                        @Query("destinations")String destinations);
 *
 * }

  /////////////////////////////////////////// CALL METHOD //////////////////////////////////////////

 * public static void getTripDirections(LatLng originLoc, LatLng destination, SingleObserver<Result> observer){
 *
 *         String startLoc = "origin="+originLoc.latitude+","+originLoc.longitude;
 *         String destLoc = "destination="+destination.latitude+","+destination.longitude;
 *         String key  = GOOGLE_MAPS_KEY;
 *
 *         if (retrofit==null) {
 *           Retrofit retrofit =
 *                     new Retrofit.Builder()
 *                             .addConverterFactory(GsonConverterFactory.create())
 *                             .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
 *                             .baseUrl(MAPS_API_URL)
 *                             .build();
 *
 *             if (mapService==null){
 *         MapService mapService = retrofit.create(MapService.class);
 *             }
 *         }
 *
 *         mapService.getDirectionsSingle(key, startLoc, destLoc)
 *                 .subscribeOn(Schedulers.io())
 *                 .observeOn(AndroidSchedulers.mainThread())
 *                 .subscribe(observer);
 *     }
 *
 *
 * ///////////////////////////////////// CALLBACK METHOD ///////////////////////////////////////////
 *
 *         Log.d("USERTESTAPP", "calculateTripValues:\n"
 *                         +passengerMark.getPosition().latitude+","
 *                         +passengerMark.getPosition().longitude+"\n"
 *                         +destinationMark.getPosition().latitude+","
 *                         +destinationMark.getPosition().longitude);
 *
 *
 *         MapDirections.getTripDirections
 *                 (passengerMark.getPosition(),
 *                         destinationMark.getPosition(),
 *                         new SingleObserver<Result>() {
 *                     @Override
 *                     public void onSubscribe(Disposable d) {
 *                         Log.d("USERTESTAPP", "onSubscribe: "+d.isDisposed());
 *                     }
 *
 *                     @Override
 *                     public void onSuccess(Result result) {
 *
 *                         List<Result.Rows> rows = result.getRows();
 *
 *                         Log.d("USERTESTAPP",
 *                                 "SingleObserver: " + "\n"
 *                                         +"DIRECTIONS IS NULL?"+(result==null) +"\n"
 *                                         +"ROW IS NULL?"+(rows==null)+", LENGTH ->"+rows.size() +"\n");
 *
 *                         int i = 0;
 *                         for (Result.Rows row : rows) {
 *                             List<Result.Elements> elements = row.getElements();
 *
 *                             Log.d("USERTESTAPP",
 *                                     "SingleObserver: " + "\n"
 *                                             +"ROW["+i+"] IS NULL?"+(row==null) +"\n"
 *                                             +"ELEMENTS IS NULL?"+(elements==null)+", LENGTH ->"+elements.size() +"\n");
 *
 *                             int j = 0;
 *                             for (Result.Elements element : elements) {
 *
 *                                 Log.d("USERTESTAPP",
 *                                         "SingleObserver: " + "\n"
 *                                                 +"ELEMENT["+j+"] IS NULL?"+(element==null) +"\n"
 *                                                 +"DISTANCE IS NULL?"+(element.getDistance()==null)+", "
 *                                                 +"DURATION IS NULL?"+(element.getDuration()==null)+"\n"
 *                                                 //+distance+"\n"+duration
 *                                 );
 *
 *                                 j++;
 *                             }
 *                             i++;
 *                         }
 *
 *                     }
 *
 *                     @Override
 *                     public void onError(Throwable e) {
 *                         e.printStackTrace();
 *                         Log.d("USERTESTAPP", "SingleObserver: "+e.getMessage()+"\n"
 *                                 +e.getCause().toString()+"\n"+e.getLocalizedMessage());
 *                     }
 *                 });
 *
 * /////////////////////////////////// RESULT MODEL CLASS //////////////////////////////////////////
 *
 * public class Result {
 *
 *     public Result() {
 *     }
 *
 *     private List<String> destination_addresses;
 *     private List<String>  origin_addresses;
 *     private List<Rows> rows;
 *     private String status;
 *
 *     public List<String>  getDestination_addresses() {
 *         return destination_addresses;
 *     }
 *
 *     public void setDestination_addresses(List<String>  destination_addresses) {
 *         this.destination_addresses = destination_addresses;
 *     }
 *
 *     public List<String>  getOrigin_addresses() {
 *         return origin_addresses;
 *     }
 *
 *     public void setOrigin_addresses(List<String>  origin_addresses) {
 *         this.origin_addresses = origin_addresses;
 *     }
 *
 *     public List<Rows> getRows() {
 *         return rows;
 *     }
 *
 *     public void setRows(List<Rows> rows) {
 *         this.rows = rows;
 *     }
 *
 *     public String getStatus() {
 *         return status;
 *     }
 *
 *     public void setStatus(String status) {
 *         this.status = status;
 *     }
 *
 *      #########################     DISTANCE AND DURATION CLASS     #########################
 *
 *     public class Distance{
 *         private String text;
 *         private int value;
 *
 *         public String getText() {
 *             return text;
 *         }
 *
 *         public void setText(String text) {
 *             this.text = text;
 *         }
 *
 *         public int getValue() {
 *             return value;
 *         }
 *
 *         public void setValue(int value) {
 *             this.value = value;
 *         }
 *     }
 *
 *     public class Duration{
 *         private String text;
 *         private int value;
 *
 *         public String getText() {
 *             return text;
 *         }
 *
 *         public void setText(String text) {
 *             this.text = text;
 *         }
 *
 *         public int getValue() {
 *             return value;
 *         }
 *
 *         public void setValue(int value) {
 *             this.value = value;
 *         }
 *     }
 *
 *
 *      #################################     LEGS CLASS     ##################################
 *
 *     public class Elements {
 *         private Distance distance;
 *         private Duration duration;
 *         private String status;
 *
 *         public Distance getDistance() {
 *             return distance;
 *         }
 *
 *         public void setDistance(Distance distance) {
 *             this.distance = distance;
 *         }
 *
 *         public Duration getDuration() {
 *             return duration;
 *         }
 *
 *         public void setDuration(Duration duration) {
 *             this.duration = duration;
 *         }
 *
 *         public String getStatus() {
 *             return status;
 *         }
 *
 *         public void setStatus(String status) {
 *             this.status = status;
 *         }
 *     }
 *
 *      ################################     ROUTES CLASS     #################################
 *
 *     public class Rows {
 *         private List<Elements> elements;
 *
 *         public List<Elements> getElements() {
 *             return elements;
 *         }
 *
 *         public void setElements(List<Elements> elements) {
 *             this.elements = elements;
 *         }
 *
 *     }
 * }
 **/
