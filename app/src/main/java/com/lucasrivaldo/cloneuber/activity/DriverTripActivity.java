package com.lucasrivaldo.cloneuber.activity;

import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.dynamic.IFragmentWrapper;
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

import androidx.annotation.NonNull;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.AlertDialogUtil;
import com.lucasrivaldo.cloneuber.helper.BitmapHelper;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.SearchURL;
import com.lucasrivaldo.cloneuber.model.Trip;
import com.lucasrivaldo.cloneuber.model.UberAddress;
import com.lucasrivaldo.cloneuber.model.User;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG_TEST;
import static com.lucasrivaldo.cloneuber.activity.UberDriverActivity.dType;
import static com.lucasrivaldo.cloneuber.config.ConfigurateFirebase.TYPE_DRIVER;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_ARRIVED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.ON_THE_WAY;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.PASSENGER_ABOARD;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.TRIP_FINALIZED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.neededPermissions;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;

public class DriverTripActivity extends FragmentActivity
        implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    private boolean mIsLocationEnabled, mIsTextsTabOpen, isToCheckAddress;
    private double mLatitude, mLongitude = 0;
    private String mStatusLoopHolder;

    private GoogleMap mMap;

    private User mLoggedDriver;
    private Trip mCurrentTrip;

    private HashMap<String, String> mRouteTextMap;
    private Marker mDriverMark, mPassengerMark, mDestinationMark;
    private Polyline mCurrentPolyline;
    private Circle mQueryTripCircle;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private DatabaseReference mTripsRef;
    private ValueEventListener mCTripValueEventListener;

    private GeoFire mGeoFire;
    private GeoQuery mGeoQueryTrip;
    private GeoQueryEventListener mTripQueryEventListener;

    private CircleImageView mCiv_uberDescription;
    private SearchView mSearchMyLocation, mSearchDestinyLocation;
    private TextView mTextTripDescriptionType, mTextUberDescriptionValue, mTextUberUserName,
            mTextDuration, mTextDistance;
    private Button mButtonSearchDriver;

    /** ##############################      INITIALIZATION     ################################ **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_trip);

        preLoad();

        // GETTING INTENT BUNDLE SAVED
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {


            mCurrentTrip = (Trip) bundle.getSerializable("trip");

            if (mCurrentTrip != null){
                if (mCurrentTrip.getDriver() != null) {

                    String driverId = mCurrentTrip.getDriver().getId();
                    if (driverId.equals(UserFirebase.getCurrentUserID())) {

                        mLoggedDriver = mCurrentTrip.getDriver();
                        if (mLoggedDriver != null) {

                            // STARTS ACTIVITY AFTER BUNDLE DATA CHECKED AND RESTORED
                            initContentView();
                        }

                    } else {
                        throwToast(getResources().getString(R.string.text_another_has_initiated),
                                false);
                        finish();
                    }

                }else { // NULL DRIVER ELSE

                    throwToast("NO DRIVER SAVED", false);
                    updateDriver(); // IF NO DRIVER SAVED, UPDATE AND START ACTIVITY
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

    private void preLoad() {

        mIsLocationEnabled = false;
        mStatusLoopHolder = null;

        mDriverMark = null;
        mPassengerMark = null;
        mDestinationMark = null;

        mLocationManager = null;
        mLocationListener = null;

        mGeoFire = ConfigurateFirebase.getGeoFire();
        mGeoQueryTrip = null;
        mTripQueryEventListener = null;

        mTripsRef = null;
        mCTripValueEventListener = null;
    }

    private void updateDriver() {
        UserFirebase.getLoggedUserData(dType, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    mLoggedDriver = dataSnapshot.getValue(User.class);
                    mCurrentTrip.setDriver(mLoggedDriver);

                    if (mCurrentTrip.update()){

                        throwToast("updating trip driver", false);
                        initContentView();

                    }else {

                        try {
                            throw new Exception();
                        } catch (Exception e){
                            e.printStackTrace();
                            Log.d(TAG, "onDataChange: "+e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void initContentView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name)+" Driver");
        setActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapDriverTrip);

        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();

        loadInterface();
        startTripListener( true);

        //STARTS LOCATION LISTENER FOR LOGGED DRIVER
        setLocationManager();
    }

    private void loadInterface() {

        // INCLUDE LAYOUT DURATION && DISTANCE
        mTextDuration = findViewById(R.id.textDuration);
        mTextDistance = findViewById(R.id.textDistance);

        mCiv_uberDescription = findViewById(R.id.civ_uberDescription);
        mTextTripDescriptionType = findViewById(R.id.textTripDescriptionType);
        mTextUberDescriptionValue = findViewById(R.id.textUberDescriptionValue);

        TextView textDescriptor = findViewById(R.id.textUberTripTabUserDescriptor);
        textDescriptor.setText(getString(R.string.text_passenger));

        mTextUberUserName = findViewById(R.id.textUberUserName);

        mSearchMyLocation = findViewById(R.id.searchMyLocationDescription);
        findViewById(R.id.btnMyLocDescription).setVisibility(View.GONE);

        mSearchDestinyLocation = findViewById(R.id.searchDestinyLocationDescription);
        findViewById(R.id.btnAddNewLocDescription).setVisibility(View.GONE);

        findViewById(R.id.textMaxDest).setVisibility(View.GONE);

        mButtonSearchDriver = findViewById(R.id.buttonSearchDriver);

        // SETS TRIP TAB
        setTripTab();

        // SET LISTENERS
        setClickListeners();
        setLocationListeners();
    }

    private void setTripTab() {

        int imgResId = UberHelper.returnImageResourceId(mCurrentTrip.getTripType());
        mCiv_uberDescription.setImageResource(imgResId);

        String tripType = UberHelper.returnTypeName(mCurrentTrip.getTripType(), getResources());
        mTextTripDescriptionType.setText(tripType);

        mTextUberDescriptionValue.setText(mCurrentTrip.getValue());
        mTextUberUserName.setText(mCurrentTrip.getPassenger().getName());

        mSearchMyLocation.setQueryHint(mCurrentTrip.getStartLoc().getAddressLines());
        findViewById(R.id.myLocLayoutDescription).setEnabled(false);
        findViewById(R.id.myLocLayoutDescription).setFocusable(false);
        findViewById(R.id.myLocLayoutDescription).setClickable(false);

        mSearchDestinyLocation.setQueryHint(mCurrentTrip.getDestination().getAddressLines());
        findViewById(R.id.destinationLayoutDescription).setEnabled(false);
        findViewById(R.id.destinationLayoutDescription).setFocusable(false);
        findViewById(R.id.destinationLayoutDescription).setClickable(false);
    }

    /** ############################      SET CLICK LISTENERS     ############################# **/

    private void setClickListeners() {

        mButtonSearchDriver.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();

        if (itemId == R.id.buttonSearchDriver) {

            switch (checkStatus()) {

                case PASSENGER_ABOARD:
                    updateTripStatus(ON_THE_WAY);
                    break;

                case TRIP_FINALIZED:
                    confirmTripPaymentAlertDialog();
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
                mTripsRef.removeEventListener(mCTripValueEventListener);
                mCTripValueEventListener = null;
            }

        }else {

            if (mCTripValueEventListener == null) {
                mTripsRef =
                        ConfigurateFirebase.getFireDBRef()
                                .child(ConfigurateFirebase.TRIP)
                                .child(mCurrentTrip.getTripId());

                mCTripValueEventListener = new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {
                            mCurrentTrip = dataSnapshot.getValue(Trip.class);

                            if (mCurrentTrip.isPassengerCancelling()) {
                                cancelTrip();

                            }else {

                                if (checkStatus().equals(PASSENGER_ABOARD)) {
                                    updateTripStatus(PASSENGER_ABOARD);

                                } else if (checkStatus().equals(DRIVER_COMING)) {
                                    if (mStatusLoopHolder == null) updateTripStatus(DRIVER_COMING);
                                }

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                };



                mTripsRef.addValueEventListener(mCTripValueEventListener);
            }
        }
    }

    @SuppressLint("NewApi")
    private void setLocationManager() {

        // CHECKING FOR PERMISSIONS REQUIRED IF WERE GRANTED, IF ANYTHING WENT WRONG ON VALIDATION
        if (checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            AlertDialogUtil.permissionValidationAlert(neededPermissions, this);
            return;
        }

        if (mLocationManager == null) {
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }

        //  WHAT WE NEED TO GET USER CURRENT LOCATION
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, // 1 - LOCATION PROVIDER
                1000,              // 2 - MIN. TIME BETWEEN LOC. UPDATES (in milliseconds)
                1,               // 3 - MIN. DISTANCE BETWEEN LOC. UPDATES (in meters)
                mLocationListener);           // 4 - LOCATION LISTENER

        mIsLocationEnabled = true;
    }

    private void setLocationListeners() {
        mLocationListener = location -> {

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            LatLng driverLoc = new LatLng(latitude,longitude);

            if (isToCheckAddress) {
                if (checkStatus().equals(DRIVER_COMING)||checkStatus().equals(DRIVER_ARRIVED)) {
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

            GeoLocation geoLoc = new GeoLocation(latitude, longitude);

            if (!geoLoc.equals( new GeoLocation(mLatitude, mLongitude))){

                mLatitude = latitude;
                mLongitude = longitude;

                if (mCurrentTrip != null) {
                    mCurrentTrip.updateDriverLocation(latitude, longitude);

                    mGeoFire.setLocation(mCurrentTrip.getTripId(),
                            geoLoc,
                            completionListener());
                }

                // DRAW ROUTES AND UPDATE TEXTS
                addTripMarker();
            }
        };
    }

    private GeoFire.CompletionListener completionListener() {
        return (key, error) -> {
            if (error != null) {
                throwToast("ERROR UPDATING GEOFIRE LOCATION: \n"
                        + error.getMessage(), true);
                try {
                    throw error.toException();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "completionListener: " + e.getMessage());
                }
            }
        };
    }

    private void setGeoQueryForTrip(Boolean isForStartLoc, boolean stopListeners) {
        double radius = 0.015; //(15m) measured in km

        if (stopListeners){
            throwToast("Canceling trip geo query", false);

            if (mTripQueryEventListener!=null) {
                mGeoQueryTrip.removeAllListeners();
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


                mGeoQueryTrip = mGeoFire.queryAtLocation(queryLoc, radius);


                mTripQueryEventListener = tripQueryEventListener();
            }
            mGeoQueryTrip.addGeoQueryEventListener(mTripQueryEventListener);

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

    private void cancelTripsListeners() {

        // REMOVE TRIP LISTENER
        startTripListener(false);
    }

    private void cancelTrip() {
        if (mIsLocationEnabled) {
            mLocationManager.removeUpdates(mLocationListener);
            mIsLocationEnabled = false;
        }

        mCurrentTrip.setDriver(null);
        updateTripStatus(AWAITING);

        // CLEAR LISTENERS
        cancelTripsListeners();

        mMap.clear();
        finish();
    }

    private void finalizeTrip() {
        mGeoFire.removeLocation(mCurrentTrip.getTripId(), (key, error) ->{
            if (error == null){
                cancelTripsListeners();
                finish();
            }else {
                try{
                    throw error.toException();
                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "finalizeTrip: "+e.getMessage());
                }
            }
        });

    }

    private void updateTripStatus(String status) {
        mCurrentTrip.setStatus(status);
        if (mCurrentTrip.update()) {

            Log.d(TAG, "updateTripStatus: STATUS UPDATED "+status);

            if (!status.equals(AWAITING)) addTripMarker();

            int gone = View.GONE;
            int visible = View.VISIBLE;

            switch (status) {

                case AWAITING: // IF DRIVER CANCELLED TRIP
                    mCurrentTrip = null;
                    break;

                case DRIVER_COMING:
                    // <ALREADY CALLED>

                    // OPEN BUTTON START TRAVEL DISABLED
                    mButtonSearchDriver.setText(getResources().getString(R.string.text_cancel_trip));
                    mButtonSearchDriver.setClickable(true);
                    mButtonSearchDriver.setVisibility(visible);

                    // CREATE GEOQUERY AT START LOC FOR NOTIFICATION WHEN DRIVER ARRIVES
                    if (mStatusLoopHolder == null) {

                        setGeoQueryForTrip(true, false);
                        mStatusLoopHolder = (DRIVER_COMING);
                    }

                    break;

                case DRIVER_ARRIVED:
                    // <ALREADY CALLED>

                    String progText = getResources().getString(R.string.text_progress_waiting_passenger);
                    UberHelper.toggleProgressBar // OPEN PROGRESS WAITING FOR PASSENGER
                            (progText, true, this);

                    // OPEN BUTTON START TRAVEL DISABLED
                    mButtonSearchDriver.setClickable(false);
                    mButtonSearchDriver.setVisibility(visible);

                    // CANCEL GEOQUERY AT START LOC FOR NOTIFICATION WHEN DRIVER ARRIVES
                    if (mStatusLoopHolder .equals(DRIVER_COMING)) {

                        setGeoQueryForTrip(null, true);
                        mStatusLoopHolder = (ON_THE_WAY);
                    }

                    break;

                case PASSENGER_ABOARD:
                    // <ALREADY CALLED>

                    // CLOSE && ENABLE BUTTON START TRAVEL
                    mButtonSearchDriver.setText(getResources().getString(R.string.text_start_trip));
                    mButtonSearchDriver.setClickable(true);


                    // CLOSE PROGRESS WAITING FOR PASSENGER
                    UberHelper.toggleProgressBar(null,false, this);

                    break;

                case ON_THE_WAY:
                    // <ALREADY CALLED>

                    mButtonSearchDriver.setVisibility(gone);


                    // CREATE GEOQUERY AT DESTINATION FOR NOTIFICATION WHEN ARRIVES
                    if (mStatusLoopHolder.equals(ON_THE_WAY)) {

                        setGeoQueryForTrip(false, false);
                        mStatusLoopHolder = (TRIP_FINALIZED);
                    }
                    break;

                case TRIP_FINALIZED:
                    // <ALREADY CALLED>

                    // CANCEL GEOQUERY AT DESTINATION FOR NOTIFICATION WHEN ARRIVES
                    if (mStatusLoopHolder.equals(TRIP_FINALIZED)) {

                        setGeoQueryForTrip(null, true);
                        mStatusLoopHolder = null;
                    }

                    // OPEN BUTTON FINALIZE TRAVEL
                    mButtonSearchDriver.setText(getResources().getString(R.string.text_finalize_trip));
                    mButtonSearchDriver.setClickable(true);
                    mButtonSearchDriver.setVisibility(visible);

                    break;
            }
        } else {
            throwToast("STATUS UPDATING ERROR", true);
            Log.d(TAG, "updateTripStatus: STATUS UPDATING ERROR");
        }
    }

    private void addTripMarker() {

        // CREATE MARKER ON MAP FOR DRIVER LOCATION
        LatLng driverLoc = new LatLng(mLatitude, mLongitude);

        if (mDriverMark != null) {

            if (!mDriverMark.getPosition().equals(driverLoc)) {
                double angle = SphericalUtil.computeHeading(mDriverMark.getPosition(), driverLoc);

                mDriverMark.setIcon(BitmapDescriptorFactory
                        .fromBitmap(BitmapHelper.rotateDriverIconBitmap(getResources(), angle)));
                mDriverMark.setPosition(driverLoc);
            }

            mDriverMark.setVisible(true);
        } else {
            mDriverMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(driverLoc)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_carr)));
        }


        // CREATE MARKERS FOR TRIP


        // CREATE MARKER ON MAP FOR PASSENGER LOCATION
        LatLng passengerLoc = new LatLng(mCurrentTrip.getStartLoc().getLatitude(),
                mCurrentTrip.getStartLoc().getLongitude());

        if (mPassengerMark != null) {

            if (!mPassengerMark.getPosition().equals(passengerLoc))
                mPassengerMark.setPosition(passengerLoc);

        } else {
            mPassengerMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(passengerLoc)
                            .title(mCurrentTrip.getPassenger().getName())
                            .icon(BitmapHelper.describeFromVector
                                    (R.drawable.ic_person_pin_circle_black_24dp,
                                            this)));
        }

        // CREATE MARKER ON MAP FOR DESTINATION LOCATION
        LatLng destinationLoc = new LatLng(mCurrentTrip.getDestination().getLatitude(),
                mCurrentTrip.getDestination().getLongitude());

        if (mDestinationMark != null) {

            if (!mDestinationMark.getPosition().equals(destinationLoc))
                mDestinationMark.setPosition(destinationLoc);

        } else {
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

    private void getRoutes() {
        if (mCurrentTrip != null)
            if (checkStatus().equals(DRIVER_COMING)) {

                LatLng driverLoc = mDriverMark.getPosition();
                LatLng passenger = mPassengerMark.getPosition();

                //  DRAWING ROUTES
                getDirections(driverLoc, passenger);


            } else if (checkStatus().equals(ON_THE_WAY)) {


                LatLng driverLoc = mDriverMark.getPosition();
                LatLng destinationLoc = mDestinationMark.getPosition();

                //  DRAWING ROUTES
                getDirections(driverLoc, destinationLoc);
            }
    }

    private void getDirections(LatLng startLoc, LatLng destination) {
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

            } else if (value.getClass().equals(HashMap.class)) {

                mRouteTextMap = (HashMap<String, String>) value;

                getRoutesValues();
            }
        }
    }

    private void centerPolyLine() {

        LatLngBounds.Builder centerBuilder = new LatLngBounds.Builder();

        //  SETTING CAMERA CENTRALIZATION BUILDER
        for (LatLng latLng : mCurrentPolyline.getPoints()) {
            centerBuilder.include(latLng);
        }
        LatLngBounds bounds = centerBuilder.build();

        int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.2);

        mMap.moveCamera
                (CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void getRoutesValues() {

        // UPDATE TEXTS
        String distanceText = mRouteTextMap.get(DISTANCE);
        String durationText = mRouteTextMap.get(DURATION);

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

            // CLOSE DURATION AND DISTANCE TEXTS
            if (mIsTextsTabOpen) {
                toggleDurationTab(false);
                toggleDistanceTab(false);
            }

        }else {

            // OPEN DURATION AND DISTANCE TEXTS
            if (!mIsTextsTabOpen) {
                toggleDurationTab(true);
                toggleDistanceTab(true);
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

            circleOptions = UberHelper.circleOptions(TYPE_DRIVER, latLng, true);

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

    /** ##############################     ACTIVITY PROCESSES    ############################## **/

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissionResult : grantResults) {
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                AlertDialogUtil.permissionValidationAlert(neededPermissions, this);
            } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                setLocationManager();
            }
        }
    }

    /** #################################        HELPERS       ################################ **/

    private void confirmTripPaymentAlertDialog() {

        String startLoc = "\n" + getResources().getString(R.string.text_alert_start_loc) + "\n";

        String destination = getResources().getString(R.string.text_alert_destination) + "\n";

        String value = "R$ " + mCurrentTrip.calculate(mCurrentTrip.getTripType());

        String tripText = UberHelper
                .returnTypeName(mCurrentTrip.getTripType(), getResources()).toUpperCase() + "\n"
                + startLoc
                + destination
                + value;

        AlertDialogUtil.confirmTripPaymentAlertDialog
                (this, tripText, (dialog, which) -> finalizeTrip());
    }

    private void toggleDistanceTab(boolean isOpening) {
        findViewById(R.id.layout_distance).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void toggleDurationTab(boolean isOpening) {
        mIsTextsTabOpen = isOpening;
        findViewById(R.id.layout_duration).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void throwToast(String message, boolean isLong) {
        int length =  isLong? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;

        Toast.makeText(this, message, length).show();
    }

    private String checkStatus() {
        return mCurrentTrip.getStatus();
    }
}


