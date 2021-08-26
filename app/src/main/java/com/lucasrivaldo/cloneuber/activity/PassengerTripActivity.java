package com.lucasrivaldo.cloneuber.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.BitmapHelper;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.SearchURL;
import com.lucasrivaldo.cloneuber.model.Trip;
import com.lucasrivaldo.cloneuber.model.UberAddress;
import com.lucasrivaldo.cloneuber.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG_TEST;
import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_PASSENGER;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_ARRIVED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.ON_THE_WAY;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.PASSENGER_ABOARD;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.TRIP_FINALIZED;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;


public class PassengerTripActivity extends FragmentActivity
        implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    private boolean isToCheckAddress;

    private String mStatusLoopHolder;

    private GoogleMap mMap;

    private Marker mPassengerMark, mDriverMark, mDestinationMark;
    private Circle  mQueryTripCircle;
    private Polyline mCurrentPolyline;
    private HashMap<String, String> mRouteTextMap;

    private GeoFire mGeoFire;
    private GeoQuery mGeoQuery;
    private GeoQueryEventListener mTripQueryEventListener;

    private DatabaseReference mTripRef;
    private ValueEventListener mCTripValueEventListener;

    private User mLoggedPassenger;
    private Trip mCurrentTrip;

    private UberAddress myAddress;
    private List<UberAddress> mNewAddressList;

    private SearchView mSearchMyLocation, mSearchDestinyLocation;
    private ImageView mBtnAddNewLoc;
    private TextView mTextTripDescriptionType, mTextUberDescriptionValue, mTextUberUserName,
            mTextDuration, mTextDistance;
    private Button mButtonSearchDriver;

    /** ##############################      INITIALIZATION     ################################ **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_trip);

        preLoad();

        // GETTING INTENT BUNDLE SAVED
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            mCurrentTrip = (Trip) bundle.getSerializable("trip");

            if (mCurrentTrip != null){

                mLoggedPassenger = mCurrentTrip.getPassenger();

                if (mCurrentTrip.getDriver() != null) {

                    // STARTS ACTIVITY AFTER BUNDLE DATA CHECKED AND RESTORED
                    initContentView();

                }else { // NULL DRIVER ELSE

                    throwToast("NO DRIVER SAVED", false);
                    finish();
                }

            }else { // NULL TRIP ELSE

                Log.d(TAG, "onCreate: TRIP VALUE IS NULL");
                throwToast("TRIP VALUE NULL", true);
                finish();
            }

        }else { // NULL BUNDLE ELSE

            Log.d(TAG, "onCreate: TRIP BUNDLE NULL");
            throwToast("TRIP BUNDLE NULL", true);
            finish();
        }
    }

    private void preLoad(){
        mNewAddressList = new ArrayList<>();
        mCurrentTrip = new Trip();

        mStatusLoopHolder = null;

        mDriverMark = null;
        mPassengerMark = null;
        mDestinationMark = null;

        mGeoFire = ConfigurateFirebase.getGeoFire();
        mGeoQuery = null;
        mTripQueryEventListener = null;

        mTripRef = null;
        mCTripValueEventListener = null;
    }

    private void initContentView(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name)+" Passenger");
        setActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPassengerTrip);

        if (mMapFragment != null) mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();

        loadInterface();
        startTripListener( true);
    }

    private void loadInterface(){

        mTextDuration = findViewById(R.id.textDuration);
        mTextDistance = findViewById(R.id.textDistance);

        // ############################      SEARCH ADDRESS  TAB      #############################

        mSearchMyLocation = findViewById(R.id.searchMyLocationDescription);
        mSearchDestinyLocation = findViewById(R.id.searchDestinyLocationDescription);

        findViewById(R.id.btnMyLocDescription).setVisibility(View.GONE);
        mBtnAddNewLoc = findViewById(R.id.btnAddNewLocDescription);

        // ###########################      SELECT TRAVEL TYPE TAB      ###########################

        mTextUberDescriptionValue = findViewById(R.id.textUberDescriptionValue);
        mTextTripDescriptionType = findViewById(R.id.textTripDescriptionType);
        mTextUberUserName = findViewById(R.id.textUberUserName);

        mButtonSearchDriver = findViewById(R.id.buttonSearchDriver);

        // SETS TRIP TAB
        setTripTab();

        updateTripStatus(DRIVER_COMING);
        setClickListeners();
    }

    private void setTripTab() {

        CircleImageView civ_uberDescription = findViewById(R.id.civ_uberDescription);

        int imgResId = UberHelper.returnImageResourceId(mCurrentTrip.getTripType());
        civ_uberDescription.setImageResource(imgResId);

        String tripType = UberHelper.returnTypeName(mCurrentTrip.getTripType(), getResources());
        mTextTripDescriptionType.setText(tripType);

        mTextUberDescriptionValue.setText(mCurrentTrip.getValue());
        mTextUberUserName.setText(mCurrentTrip.getDriver().getName());

        mSearchMyLocation.setQueryHint(mCurrentTrip.getStartLoc().getAddressLines());
        findViewById(R.id.myLocLayoutDescription).setEnabled(false);
        findViewById(R.id.myLocLayoutDescription).setFocusable(false);
        findViewById(R.id.myLocLayoutDescription).setClickable(false);

        mSearchDestinyLocation.setQueryHint(mCurrentTrip.getDestination().getAddressLines());
        mSearchDestinyLocation.setClickable(false);
        mSearchDestinyLocation.setEnabled(false);
        mSearchDestinyLocation.setFocusable(false);
    }

    /** ############################      SET CLICK LISTENERS     ############################# **/

    private void setClickListeners() {

        mBtnAddNewLoc.setOnClickListener(this);
        mButtonSearchDriver.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        int itemId = view.getId();
        switch (itemId) {

            // ########################      SEARCH ADDRESS  TAB      #########################
            case R.id.btnAddNewLoc:

                startNewAddress();
                break;

            // #######################      SELECT TRAVEL TYPE TAB      #######################

            case R.id.buttonSearchDriver:

                switch (checkStatus()) {
                    case DRIVER_ARRIVED:
                        mButtonSearchDriver.setVisibility(View.GONE);
                        updateTripStatus(PASSENGER_ABOARD);

                        break;
                    case TRIP_FINALIZED:
                        finalizeTrip();

                        break;
                    case DRIVER_COMING:
                        cancelTrip();
                        break;
                }

        }
    }

    /** ###############################      SET LISTENERS     ################################ **/

    private void startTripListener(boolean startListener) {

        if (!startListener){
            if (mCTripValueEventListener !=null){
                mTripRef.removeEventListener(mCTripValueEventListener);
                mCTripValueEventListener = null;
            }

        }else {

            if (mCTripValueEventListener == null) {
                mTripRef =
                        ConfigurateFirebase.getFireDBRef()
                                .child(ConfigurateFirebase.TRIP)
                                .child(mCurrentTrip.getTripId());

                mCTripValueEventListener = new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        try {
                            if (dataSnapshot.exists()) {
                                mCurrentTrip = dataSnapshot.getValue(Trip.class);

                                if (mCurrentTrip != null) {

                                    if (mCurrentTrip.getDriver() != null) {

                                        if (checkStatus().equals(DRIVER_ARRIVED)) {
                                            updateTripStatus(DRIVER_ARRIVED);
                                        }else if (checkStatus().equals(ON_THE_WAY)) {
                                            updateTripStatus(ON_THE_WAY);
                                        }else if (checkStatus().equals(TRIP_FINALIZED)) {
                                            updateTripStatus(TRIP_FINALIZED);
                                        }

                                        double driverLat = mCurrentTrip.getDriver().getLatitude();
                                        double driverLng = mCurrentTrip.getDriver().getLongitude();
                                        LatLng driverLoc = new LatLng(driverLat, driverLng);

                                        if (!driverLoc.equals(mDriverMark.getPosition())) {
                                            addTripMarker();
                                        }

                                        if (isToCheckAddress) {
                                            if (checkStatus().equals(DRIVER_COMING)) {
                                                if (checkAddress(driverLoc, true)) {
                                                    updateTripStatus(DRIVER_ARRIVED);
                                                    isToCheckAddress = false;
                                                }
                                            }else {
                                                if (checkAddress(driverLoc, false)) {
                                                    updateTripStatus(TRIP_FINALIZED);
                                                    isToCheckAddress = false;
                                                }
                                            }
                                        }
                                    } else { // DRIVER NULL ELSE

                                        updateTripStatus(AWAITING);

                                    }
                                }
                            }

                        } catch (Exception e) {
                            throwToast(e.getMessage(), true);
                            Log.d(TAG, "onDataChange: " + e.getMessage());
                            e.printStackTrace();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                };

                mTripRef.addValueEventListener(mCTripValueEventListener);
            }
        }
    }

    private void setGeoQueryForTrip(Boolean isForStartLoc, boolean stopListeners) {
        double radius = 0.015; //(15m) measured in km

        if (stopListeners){
            throwToast("Canceling trip geo query", false);

            if (mTripQueryEventListener!=null) {
                mGeoQuery.removeAllListeners();
                mTripQueryEventListener = null;

                createCircles(false);
            }

        }else {

            String logText = isForStartLoc ?
                    "SETTING QUERY FOR STARTLOC" : "SETTING QUERY FOR DESTINATION";
            Log.d(TAG, "setGeoQueryForTrip: "+logText);

            if (mTripQueryEventListener == null) {
                GeoLocation queryLoc = isForStartLoc ?
                        new GeoLocation(mCurrentTrip.getStartLoc().getLatitude(),
                                mCurrentTrip.getStartLoc().getLongitude())

                        : new GeoLocation(mCurrentTrip.getDestination().getLatitude(),
                        mCurrentTrip.getDestination().getLongitude());


                mGeoQuery = mGeoFire.queryAtLocation(queryLoc, radius);


                mTripQueryEventListener = tripQueryEventListener();
            }
            mGeoQuery.addGeoQueryEventListener(mTripQueryEventListener);

            createCircles(true);
        }
    }

    private GeoQueryEventListener tripQueryEventListener() {

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (key.equals(mCurrentTrip.getTripId())) checkDriverEntered(key);
            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG_TEST, "onGeoQueryReady: GEOQUERY READY");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                getErrors(error);
            }
        };
    }

    /** ################################      MY METHODS     ################################## **/

    private void cancelListeners(){
        //  CANCELLING TRIP LISTENER
        startTripListener(false);

        // CANCELLING CURRENT GEOQUERY LISTENER
        setGeoQueryForTrip(null, true);
    }

    private void cancelTrip(){

        if (checkStatus().equals(AWAITING)|| checkStatus().equals(DRIVER_COMING)) {
            mCurrentTrip.setPassengerCancelling(true);
            updateTripStatus(AWAITING);

        }
    }

    private void finalizeTrip() {

        cancelListeners();

        mCurrentTrip = new Trip();
        mMap.clear();

        startActivity(new Intent(this, UberPassengerActivity.class));
        finish();
    }

    private void updateTripStatus(String status){
        mCurrentTrip.setStatus(status);
        if(mCurrentTrip.update()){

            Log.d(TAG, "updateTripStatus: STATUS UPDATED "+status);

            if (!status.equals(AWAITING)) addTripMarker();

            int gone = View.GONE;
            int visible = View.VISIBLE;

            switch (status) {

                case AWAITING:

                    // CANCEL ALL LISTENER
                    cancelListeners();

                    // CLEAR MAP
                    mMap.clear();

                    // FINISH ACTIVITY
                    finish();

                    break;

                case DRIVER_COMING:

                    // CREATE GEOQUERY AT START LOC FOR NOTIFICATION WHEN DRIVER ARRIVES
                    if (mStatusLoopHolder == null) {

                        setGeoQueryForTrip(true, false);
                        mStatusLoopHolder = (DRIVER_COMING);
                    }

                    //UPDATE BUTTON TEXT
                    mButtonSearchDriver.setText(getResources().getString(R.string.text_cancel_trip));

                    break;

                case DRIVER_ARRIVED:

                    // CANCEL GEOQUERY AT START LOC FOR NOTIFICATION WHEN DRIVER ARRIVES
                    if (mStatusLoopHolder .equals(DRIVER_COMING)) {

                        setGeoQueryForTrip(null, true);
                        mStatusLoopHolder = (ON_THE_WAY);
                    }

                    // NOTIFY PASSENGER WHEN DRIVER ARRIVES AT START LOCATION
                    throwToast(getResources().getString(R.string.text_driver_arrived_toast),
                            false);

                    status = getResources().getString(R.string.text_confirm_aboard);

                    mButtonSearchDriver.setText(status);

                    break;

                case ON_THE_WAY:

                    mButtonSearchDriver.setVisibility(gone);

                    // CREATE GEOQUERY AT DESTINATION FOR NOTIFICATION WHEN ARRIVES
                    if (mStatusLoopHolder.equals(ON_THE_WAY)) {

                        setGeoQueryForTrip(false, false);
                        mStatusLoopHolder = (TRIP_FINALIZED);

                    }

                    break;

                case TRIP_FINALIZED:

                    // CANCEL GEOQUERY AT DESTINATION FOR NOTIFICATION WHEN ARRIVES
                    if (mStatusLoopHolder.equals(TRIP_FINALIZED)) {

                        setGeoQueryForTrip(null, true);
                        mStatusLoopHolder = null;
                    }

                    throwToast(getResources().getString(R.string.text_trip_finalization), false);

                    mButtonSearchDriver.setVisibility(visible);
                    mButtonSearchDriver.setText(status);


                    break;
            }
        }else{
            throwToast("STATUS UPDATING ERROR",true);
            Log.d(TAG, "updateTripStatus: STATUS UPDATING ERROR");
        }
    }

    private void addTripMarker() {

        // CREATE MARKER ON MAP FOR PASSENGER LOCATION

        LatLng passengerLoc = new LatLng
                (mCurrentTrip.getStartLoc().getLatitude(), mCurrentTrip.getStartLoc().getLongitude());

        if (mPassengerMark != null) {

            mPassengerMark.setPosition(passengerLoc);

        }else {

            mPassengerMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(passengerLoc)
                            .title(mLoggedPassenger.getName())
                            .snippet(mCurrentTrip.getStartLoc().getAddressLines())
                            .icon(BitmapHelper.describeFromVector
                                    (R.drawable.ic_person_pin_circle_black_24dp, this)));
        }


        // CREATE MARKER ON MAP FOR DRIVER LOCATION

        LatLng driverLoc = new LatLng
                (mCurrentTrip.getDriver().getLatitude(), mCurrentTrip.getDriver().getLongitude());

        if (mDriverMark != null) {

            double angle = SphericalUtil.computeHeading(mDriverMark.getPosition(), driverLoc);

            mDriverMark.setIcon(BitmapDescriptorFactory
                    .fromBitmap(BitmapHelper.rotateDriverIconBitmap(getResources(), angle)));
            mDriverMark.setVisible(true);
            mDriverMark.setPosition(driverLoc);

        }else {
            mDriverMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(driverLoc)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_carr)));
        }


        // CREATE MARKER ON MAP FOR DESTINATION LOCATION

        LatLng destinationLoc = new LatLng
                (mCurrentTrip.getDestination().getLatitude(), mCurrentTrip.getDestination().getLongitude());

        if (mDestinationMark != null) {
            mDestinationMark.setPosition(destinationLoc);

        }else {
            mDestinationMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(destinationLoc)
                            .title("Destination")
                            .snippet(mCurrentTrip.getDestination().getAddressLines())
                            .icon(BitmapHelper.describeFromVector
                                    (R.drawable.ic_pin_drop_black_24dp, this)));
        }

        // CENTER MARKERS
        if (checkStatus().equals(DRIVER_ARRIVED)) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLoc, 18));

        } else if (checkStatus().equals(TRIP_FINALIZED)) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLoc, 18));

        } else {
            getRoutes();
        }
    }

    private void getRoutes(){

            if (checkStatus().equals(AWAITING) || checkStatus().equals(DRIVER_COMING)) {

                if (mDriverMark !=null) {
                    LatLng driverLoc = mDriverMark.getPosition();
                    LatLng passenger = mPassengerMark.getPosition();

                    //  DRAWING ROUTES
                    getDirections(driverLoc, passenger);
                }


            } else if (checkStatus().equals(ON_THE_WAY)) {

                LatLng driverLoc = mDriverMark.getPosition();
                LatLng destinationLoc = mDestinationMark.getPosition();

                //  DRAWING ROUTES
                getDirections(driverLoc, destinationLoc);
            }
    }

    private void getDirections(LatLng startLoc, LatLng destination){
        String routeURL = MapDirections.getMapsURL(startLoc, destination);

        // GET DATA FOR DRAWING ROUTES
        new SearchURL(this, true).execute(routeURL, "driving");

        new SearchURL(this, false).execute(routeURL, "driving");
    }

    @Override
    public void onTaskDone(Object... values) {
        for (Object value : values) {
            if (value.getClass().equals(PolylineOptions.class)) {
                if (mCurrentPolyline != null)
                    mCurrentPolyline.remove();

                mCurrentPolyline = mMap.addPolyline((PolylineOptions) value);
                centerPolyLine();

            } else if(value.getClass().equals(HashMap.class)){

                mRouteTextMap = (HashMap<String, String>) value;

                getRoutesValues();
            }
        }
    }

    private void centerPolyLine(){

        LatLngBounds.Builder centerBuilder = new LatLngBounds.Builder();

        //  SETTING CAMERA CENTRALIZATION BUILDER
        for(LatLng latLng : mCurrentPolyline.getPoints()){
            centerBuilder.include(latLng);
        }
        LatLngBounds bounds = centerBuilder.build();

        int padding = (int) (getResources().getDisplayMetrics().widthPixels*0.2);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,padding));
    }

    private void getRoutesValues() {

        // UPDATE TEXTS
        String distanceText = mRouteTextMap.get(DISTANCE);
        String durationText = mRouteTextMap.get(DURATION);

        if (mCurrentTrip.getDestination() != null) {

            String subLocality =
                    (mCurrentTrip.getDestination().getSubLocality() != null) ?
                            mCurrentTrip.getDestination().getSubLocality() + ", " : "";

            String distanceText1 = "Destination -> "
                    + mCurrentTrip.getDestination().getAddress() + ", "
                    + mCurrentTrip.getDestination().getAddressNum() + ", "
                    + subLocality + mCurrentTrip.getDestination().getCity() + "."
                    + "\n" + "Distance - " + distanceText;

            mTextDistance.setText(distanceText1);
            mTextDuration.setText(durationText);

            if (checkStatus().equals(DRIVER_ARRIVED)) {
                toggleDistanceTab(false);
                toggleDurationTab(false);
            }else {
                toggleDistanceTab(true);
                toggleDurationTab(true);
            }
        }
    }

    private boolean checkAddress(LatLng checkLoc, boolean isForStartLoc){
        List<UberAddress> myAddressList = UserFirebase.getAddress
                (this, checkLoc, UserFirebase.GET_CURRENT_USER_LOC);

        if (myAddressList!=null) {
            UberAddress addressToCheck = myAddressList.get(0);

            String addressText = addressToCheck.getAddressLines()+","
                               + addressToCheck.getSubLocality()+","
                               + addressToCheck.getAddressNum();

            if (isForStartLoc){

                UberAddress origin = mCurrentTrip.getStartLoc();

                String startLoc = origin.getAddressLines()+","
                                + origin.getSubLocality()+","
                                + origin.getAddressNum();

                return addressText.equals(startLoc);
            }else {

                UberAddress destination = mCurrentTrip.getDestination();

                String destLoc = destination.getAddressLines()+","
                               + destination.getSubLocality()+","
                               + destination.getAddressNum();

                return addressText.equals(destLoc);
            }
        }
        return false;
    }

    // ###############################      GEO QUERY METHODS     ##################################

    private void checkDriverEntered(String key) {
        isToCheckAddress = true;

        Log.d(TAG_TEST, "checkDriverEntered: Driver " + key + " ENTERED\n" +
                "isToCheckAddress ? "+isToCheckAddress);

        throwToast(key + " entered", false);
    }

    private void createCircles(boolean isAdding) {

        if (!isAdding){

            if (mQueryTripCircle!=null) {
                mQueryTripCircle.remove();
                Log.d(TAG, "createCircles: REMOVING CIRCLE");
            }

        }else {
            CircleOptions circleOptions;


            LatLng latLng = checkStatus().equals(DRIVER_COMING) ?
                    new LatLng(mCurrentTrip.getStartLoc().getLatitude(),
                            mCurrentTrip.getStartLoc().getLongitude())

                    : new LatLng(mCurrentTrip.getDestination().getLatitude(),
                    mCurrentTrip.getDestination().getLongitude());

            circleOptions = UberHelper.circleOptions(TYPE_PASSENGER, latLng, true);

            if (mQueryTripCircle != null) mQueryTripCircle.remove();
            mQueryTripCircle = mMap.addCircle(circleOptions);

            Log.d(TAG, "createCircles: ADDING CIRCLE\n" +
                    "is circle options null?"+(circleOptions==null));
        }
    }

    private void getErrors(DatabaseError error) {
        try {
            throw error.toException();
        } catch (Exception e) {
            throwToast(e.getMessage(), true);
            e.printStackTrace();
            Log.d(TAG, "onGeoQueryError: " + e.toString());
        }
    }

    /** #################################        HELPERS       ################################ **/

    private void startNewAddress(){
        //TODO ADD NEW SEARCH EDIT TEXT
        // OPEN ADD EXTRA DESTINATION ACTIVITY
    }

    private void toggleDistanceTab(boolean isOpening){
        findViewById(R.id.layout_distance).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void toggleDurationTab(boolean isOpening){
        findViewById(R.id.layout_duration).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void throwToast(String message, boolean isLong){
        int  length = isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, message, length).show();
    }

    private String checkStatus(){ return mCurrentTrip.getStatus(); }
}