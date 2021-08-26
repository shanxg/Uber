package com.lucasrivaldo.cloneuber.activity;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.adapter.AdapterTrips;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.AlertDialogUtil;
import com.lucasrivaldo.cloneuber.helper.BitmapHelper;
import com.lucasrivaldo.cloneuber.helper.RecyclerItemClickListener;
import com.lucasrivaldo.cloneuber.helper.SystemPermissions;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.model.Trip;
import com.lucasrivaldo.cloneuber.model.UberAddress;
import com.lucasrivaldo.cloneuber.model.User;

import java.util.ArrayList;
import java.util.List;

import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG_TEST;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.neededPermissions;
import static com.lucasrivaldo.cloneuber.helper.UserFirebase.GET_REQS_DATA;


public class UberDriverActivity extends FragmentActivity
        implements OnMapReadyCallback, View.OnClickListener {

    public static final String dType = ConfigurateFirebase.TYPE_DRIVER;

    private boolean mBtnWorkIsSelected, mIsLocationEnabled;

    private GoogleMap mMap;
    private Marker mDriverMark;
    private Circle mQueryReqsCircle;

    private DatabaseReference mRequestRef, mTripsRef;
    private ValueEventListener mRequestsEventListener;

    private GeoFire mGeoFire;
    private GeoQuery mGeoQueryReqs;
    private GeoQueryEventListener mReqsQueryEventListener;

    private User mLoggedDriver;
    private Trip mCurrentTrip;

    private RecyclerView mRecyclerReqs;
    private AdapterTrips mAdapterTrips;

    private UberAddress myAddress;
    private List<String> mRequestList;
    private List<Trip> mTripList;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private TextView mButtonWork, mProgressText;
    private ImageView mImgStop;
    private ConstraintLayout mWorkLayout;
    private LinearLayout mIncludeProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preLoad();
        // STARTS ACTIVITY ONLY IF USER DATA NOT NULL
        UserFirebase.getLoggedUserData // RETURNS A SINGLE DATA VALUE( NOT LISTENER )
                (dType, new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            mLoggedDriver = dataSnapshot.getValue(User.class);

                            if (mLoggedDriver != null) {
                                setContentView();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });


    }

    private void preLoad() {
        mGeoFire = ConfigurateFirebase.getGeoFire();

        mRequestList = new ArrayList<>();
        mTripList = new ArrayList<>();

        mReqsQueryEventListener = null;

        mLocationManager = null;
        mLocationListener = null;
    }

    private void setContentView() {
        setContentView(R.layout.activity_uber_driver);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name)+" Driver");
        setActionBar(toolbar);

        UserFirebase.setLastLogin(dType);
        SystemPermissions.validatePermissions(neededPermissions, this, 1);

        loadInterface();

        throwToast(getString(R.string.text_logged_driver), true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDriver);

        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setLocationManager(); // START REQUEST LOCATION UPDATES ON MAP READY
    }

    private void loadInterface() {
        // REQUESTS RECYCLER
        mRecyclerReqs = findViewById(R.id.recyclerReqs);
        setUpRecyclerRequests();

        //  INCLUDE LAYOUT PROGRESSBAR
        mProgressText = findViewById(R.id.progressText);
        mIncludeProgressBar = findViewById(R.id.includeProgressBar);
        toggleProgressBar(false); // CLOSE PROG.BAR ON CREATE

        // LAYOUT BUTTON WORK
        mWorkLayout = findViewById(R.id.workLayout);
        mButtonWork = findViewById(R.id.buttonStart);
        mImgStop = findViewById(R.id.imgStop);
        toggleStartWorkInterface(false); // SET BUTTON WORK AS UNSELECTED ON CREATE

        setClickListeners();
        setLocationListeners();

    }

    private void setUpRecyclerRequests() {
        mAdapterTrips = new AdapterTrips(mTripList, this);

        mRecyclerReqs.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerReqs.setHasFixedSize(true);
        mRecyclerReqs.setAdapter(mAdapterTrips);
    }

    /** ###############################   SET CLICK LISTENERS  ################################ **/

    private void setClickListeners(){
        mWorkLayout.setOnClickListener(this);
        setRecyclerReqsClickListener();
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();

        if (itemId == R.id.workLayout) {
            mBtnWorkIsSelected = !mButtonWork.isSelected();

            toggleStartWorkInterface(mBtnWorkIsSelected); // ON CLICK ACTION
            startWorkRequests(mBtnWorkIsSelected);
        }
    }

    private void setRecyclerReqsClickListener() {
        mRecyclerReqs.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerReqs,
                        new RecyclerItemClickListener.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                Trip trip = mTripList.get(position);

                                // ACCEPT TRIP & STARTS TRIP LISTENER
                                startTripListener(trip);

                                cancelTripsListeners(); // CANCEL LISTENERS FOR TRIP ACTIVITY
                                mRequestList.clear();

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {}
                            @Override
                            public void onItemClick
                                    (AdapterView<?> adapterView, View view, int i, long l) {}
                        }));
    }

    /** ###############################      SET LISTENERS     ################################ **/

    private void startTripListener(Trip trip) {
        mTripsRef =
                ConfigurateFirebase.getFireDBRef()
                        .child(ConfigurateFirebase.TRIP)
                        .child(trip.getTripId());

        mTripsRef.addListenerForSingleValueEvent(valueEventListener(UserFirebase.GET_TRIP_DATA));
    }

    private ValueEventListener valueEventListener(int requestCode) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                switch (requestCode) {

                    case GET_REQS_DATA:
                        mRequestList.clear();
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            String key = data.getKey();
                            if (!mRequestList.contains(key))
                                mRequestList.add(key);
                        }
                        mAdapterTrips.notifyDataSetChanged();

                        break;

                    case UserFirebase.GET_TRIP_DATA:
                        if (dataSnapshot.exists()) {
                            mCurrentTrip = dataSnapshot.getValue(Trip.class);

                            if (mCurrentTrip != null && mCurrentTrip.getDriver() == null) {
                                mCurrentTrip.setDriver(mLoggedDriver);
                                mCurrentTrip.setStatus(DRIVER_COMING);

                                if (mCurrentTrip.update())
                                    openTripActivity();

                            } else if (mCurrentTrip != null && mCurrentTrip.getDriver() != null
                                    && !mCurrentTrip.getDriver().getId().equals(mLoggedDriver.getId())) {

                                throwToast(getResources().getString(R.string.text_another_has_initiated),
                                        false);
                            }
                        }
                        break;

                    case UserFirebase.GET_TRIPS_DATA:
                        Trip trip = dataSnapshot.getValue(Trip.class);

                        if (!mTripList.contains(trip)) {
                            mTripList.add(trip);
                            mAdapterTrips.notifyDataSetChanged();
                        }

                        break;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
    }

    private void openTripActivity() {

        Intent tripIntent = new Intent(this, DriverTripActivity.class);
        tripIntent.putExtra("trip", mCurrentTrip);

        startActivity(tripIntent);
        finish();
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

            LatLng driverLatLng = new LatLng(latitude, longitude);

            if (!driverLatLng.equals(new LatLng(mLoggedDriver.getLatitude(),
                    mLoggedDriver.getLongitude()))) {

                mLoggedDriver.setLatitude(latitude);
                mLoggedDriver.setLongitude(longitude);

                if (mButtonWork.isSelected()) {

                    // UPDATE GEOFIRE LOCATION
                    mGeoFire.setLocation(mLoggedDriver.getId(),
                            new GeoLocation(latitude, longitude),
                            completionListener());
                }

                if (myAddress == null) {
                    List<UberAddress> addressList = UserFirebase.getAddress
                            (this, driverLatLng, UserFirebase.GET_CURRENT_USER_LOC);

                    if (addressList != null) {
                        myAddress = addressList.get(0);

                        mRequestRef = // SETS REQUESTS REF
                                ConfigurateFirebase.getFireDBRef()
                                        .child(ConfigurateFirebase.REQUEST)
                                        .child(myAddress.getCountryCode())
                                        .child(myAddress.getState());
                    }
                }
                addMarker();
            }
        };
    }

    private GeoFire.CompletionListener completionListener() {
        return (key, error) -> {
            if (error != null){
                throwToast("ERROR UPDATING GEOFIRE LOCATION: \n"
                        +error.getMessage(), true);
                try {
                    throw error.toException();
                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "completionListener: "+ e.getMessage());
                }
            }
        };
    }

    private void setGeoQueryReqs(boolean isStarting){

        if (!isStarting){
            if (mReqsQueryEventListener!=null) {
                mGeoQueryReqs.removeAllListeners();
                mReqsQueryEventListener = null;

                createCircles(false);
            }

        }else {
            if (mGeoQueryReqs == null) {
                GeoLocation queryLoc =
                        new GeoLocation(mLoggedDriver.getLatitude(), mLoggedDriver.getLongitude());

                mGeoQueryReqs = mGeoFire.queryAtLocation(queryLoc, 1);
            }

            if (mReqsQueryEventListener == null) {
                mReqsQueryEventListener = getRequestsGeoQueryEventListeners();
            }

            mGeoQueryReqs.addGeoQueryEventListener(mReqsQueryEventListener);

            createCircles(true);
        }
    }

    private GeoQueryEventListener getRequestsGeoQueryEventListeners(){ //(requests)

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (mRequestList.contains(key))
                    getTripsData(key);
            }

            @Override
            public void onKeyExited(String key) {

                if (key.equals(mLoggedDriver.getId()))
                    restartReqsQuery();
            }

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

    private void startWorkRequests(boolean isStarting){
        if (isStarting) {
            mProgressText.setText(getResources().getString(R.string.text_searching_trip));
            toggleProgressBar(true); // OPEN PROG.BAR AT BUTTON WORK

            if (!mIsLocationEnabled) setLocationManager(); // START LOC. UPDATES BUTTON WORK

            //UPDATE GEOFIRE LOCATION
            mGeoFire.setLocation(mLoggedDriver.getId(),
                    new GeoLocation(mLoggedDriver.getLatitude(), mLoggedDriver.getLongitude()),
                    completionListener());

            //SET GEOQUERY FOR TRIP REQUESTS
            setGeoQueryReqs(true); // STARTING QUERY

            mRequestList.clear();
            if (mRequestsEventListener == null) {
                mRequestsEventListener =  valueEventListener(GET_REQS_DATA);

                // ADDING REQS DATA LISTENER
                mRequestRef.addValueEventListener(mRequestsEventListener);
            }

        }else { // IF STOPPED REQUESTS SEARCH

            if (mButtonWork.isSelected()) {
                mBtnWorkIsSelected = !mButtonWork.isSelected();
                mButtonWork.setSelected(mBtnWorkIsSelected);
            }

            cancelTripsListeners();// CANCEL LISTENERS AT BUTTON WORK
        }
    }

    private void cancelTripsListeners() {

        toggleProgressBar(false); // CLOSE PROG.BAR AT CANCEL TRIPS LISTENER

        // CANCELLING GEOQUERY LISTENER
        setGeoQueryReqs(false);// CANCEL GEOQUERY LISTENER

        // REMOVE LOCATION LISTENER
        if (mIsLocationEnabled){
            mLocationManager.removeUpdates(mLocationListener);
            mIsLocationEnabled = false;
        }

        // REMOVE GEOFIRE LOCATION AND QUERIES
        mGeoFire.removeLocation(mLoggedDriver.getId(), completionListener());

        // REMOVE REQUESTS LISTENER
        if (mRequestsEventListener != null) {

            mRequestRef.removeEventListener(mRequestsEventListener);
            mRequestsEventListener = null;
        }

        // CLEAR ALL LISTS
        mRequestList.clear();
        mTripList.clear();
        myAddress = null;

        mAdapterTrips.notifyDataSetChanged();
    }

    private void addMarker() {

        // CREATE MARKER ON MAP FOR DRIVER LOCATION
        LatLng driverLoc = new LatLng
                (mLoggedDriver.getLatitude(), mLoggedDriver.getLongitude());

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

        // CENTER DRIVER WHEN LOOKING FOR TRIPS

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLoc, 15));

    }

   // ###############################      GEO QUERY METHODS     ##################################

    private void getTripsData(String tripId) {
        if (mTripsRef == null) {
            mTripsRef =
                    ConfigurateFirebase.getFireDBRef()
                            .child(ConfigurateFirebase.TRIP)
                            .child(tripId);
        }

        mTripsRef // ADDS LISTENER FOR SINGLE VALUE
                .addListenerForSingleValueEvent(valueEventListener(UserFirebase.GET_TRIPS_DATA));
    }

    private void restartReqsQuery() {

        Log.d(TAG_TEST, "onGeoQueryReady: DRIVER LEFT");

        setGeoQueryReqs(false); // CANCELING PREVIOUS LISTENER
        if (mButtonWork.isSelected())
            setGeoQueryReqs(true); // STARTING NEW AT CURRENT LOCATION
    }

    private void createCircles(boolean isAdding) {
        if (!isAdding){

            if (mQueryReqsCircle!=null) {
                mQueryReqsCircle.remove();
                Log.d(TAG, "createCircles: REMOVING CIRCLE");
            }

        }else {
            CircleOptions circleOptions;


            if (mQueryReqsCircle != null) mQueryReqsCircle.remove();

            circleOptions = UberHelper.circleOptions
                    (dType, new LatLng(mGeoQueryReqs.getCenter().latitude,
                            mGeoQueryReqs.getCenter().longitude), false);

            if (mQueryReqsCircle!=null) mQueryReqsCircle.remove();
            mQueryReqsCircle = mMap.addCircle(circleOptions);

            Log.d(TAG, "createCircles: ADDING CIRCLE\n" +
                    "is circle options null?"+(circleOptions==null));
        }

    }

    private void getErrors(DatabaseError error){
        try {
            throw error.toException();
        } catch (Exception e) {
            throwToast(e.getMessage(), true);
            e.printStackTrace();
            Log.d(TAG, "onGeoQueryError: " + e.toString());
        }
    }

    /** #############################     ACTIVITY LIFE-CYCLE    ############################## **/

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /** ##############################     ACTIVITY PROCESSES    ############################## **/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuSignOut) {
            if (UserFirebase.signOut()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissionResult : grantResults) {
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                AlertDialogUtil.permissionValidationAlert(neededPermissions, this);
            } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                setLocationManager(); // START LOC. UPDATES ON REQUEST PERMISSION
            }
        }
    }

    /** #################################        HELPERS       ################################ **/

    private void toggleProgressBar(boolean isOpening) {
        mIncludeProgressBar.setVisibility(isOpening? View.VISIBLE: View.GONE);
    }

    private void toggleStartWorkInterface(boolean isSelected){

        // BUTTON SETTINGS
        String btnText = isSelected? " " :  getResources().getString(R.string.btn_start_text);

        mButtonWork.setText(btnText);
        mButtonWork.setSelected(isSelected);

        // IMAGE STOP SETTINGS
        int visibility = isSelected? View.VISIBLE : View.GONE;
        mImgStop.setVisibility(visibility);
    }

    private void throwToast(String message, boolean isLong){
        int  length = isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;

        Toast.makeText(this, message, length).show();
    }
}





