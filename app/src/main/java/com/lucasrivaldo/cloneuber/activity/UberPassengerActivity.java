package com.lucasrivaldo.cloneuber.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.adapter.AdapterAddresses;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.AlertDialogUtil;
import com.lucasrivaldo.cloneuber.helper.BitmapHelper;
import com.lucasrivaldo.cloneuber.helper.RecyclerItemClickListener;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections;
import com.lucasrivaldo.cloneuber.helper.maps_helpers.SearchURL;
import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.model.Trip;
import com.lucasrivaldo.cloneuber.model.UberAddress;
import com.lucasrivaldo.cloneuber.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG_TEST;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.START_TRIP;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_BLACK;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_SLCT;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_X;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.neededPermissions;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCEv;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATIONv;



public class UberPassengerActivity extends FragmentActivity
        implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    private static final String uType = ConfigurateFirebase.TYPE_PASSENGER;

    public static final int START_LOC = 0;
    public static final int DESTINATION = 1;

    private boolean mIsLocationEnabled, queryLoopHolder;
    private int mVIEW_CODE, mLocationRequestCode;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private int mMapWidth, mMapHeight;

    private HashMap<String, Marker> mDriversLocMap;
    private Marker mPassengerMark, mDestinationMark;
    private Circle mQueryReqsCircle;
    private Polyline mCurrentPolyline;
    private HashMap<String, String> mRouteTextMap;

    private GeoFire mGeoFire;
    private GeoQuery mGeoQuery;
    private GeoQueryEventListener mReqsQueryEventListener;

    private DatabaseReference mTripRef;
    private ValueEventListener mTripListener;

    private Query mReqsRef;
    private ValueEventListener mReqsListener;
    private List<String> mReqsList;

    private User mLoggedPassenger;
    private Trip myTrip;
    private UberAddress myAddress;

    private LocationManager mLocationManager;

    private List<UberAddress> mAddressList, mNewAddressList;

    private SearchView mSearchMyLocation, mSearchDestinyLocation;
    private ImageView mBtnMyLoc, mBtnAddNewLoc;

    private RecyclerView mRecyclerNewDest, mRecyclerAddress;
    private AdapterAddresses mAdapterAddresses;

    private TextView mTextUberXValue, mTextUberSelectValue, mTextUberBlackValue,
            mProgressText, mTextDuration, mTextDistance;
    private LinearLayout mBtnUberX, mBtnUberSelect, mBtnUberBlack;
    private Button mButtonSearchDriver;

    private LinearLayout mIncludeSelectExperienceTab, mIncludeCallTab,
            mSelectTypeLayout, mIncludeLoadingSearchDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // START ACTIVITY AFTER LOGGED USER IS SET FOR NO NULL EXCEPTIONS
        preLoad();
        UserFirebase.getLoggedUserData // ADD SINGLE DATA VALUE LISTENER
                (uType, new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            mLoggedPassenger = dataSnapshot.getValue(User.class);

                            if (mLoggedPassenger != null) {
                                if(UserFirebase.setLastLogin(uType)) {
                                    setContentView();
                                    myTrip.setPassenger(mLoggedPassenger);
                                }else
                                    try {
                                        throw new Exception();
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        throwToast(e.getMessage(), true);
                                        Log.d(TAG, "LOGGED_USER_DATA onDataChange: \n"
                                                +e.getMessage());
                                    }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    private void preLoad() {
        queryLoopHolder = true;

        mAddressList = new ArrayList<>();
        mNewAddressList = new ArrayList<>();
        mReqsList = new ArrayList<>();

        mGeoQuery = null;
        mReqsQueryEventListener = null;
        mGeoFire = ConfigurateFirebase.getGeoFire();

        myTrip = new Trip();

        mDriversLocMap = new HashMap<>();
    }

    private void setContentView() {
        setContentView(R.layout.activity_uber_passenger);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name)+" Passenger");
        setActionBar(toolbar);


        throwToast(getResources().getString(R.string.text_logged_passenger), true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPassenger);

        if (mMapFragment != null) {
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        loadInterface();
    }

    private void loadInterface() {
        mTextDuration = findViewById(R.id.textDuration);
        mTextDistance = findViewById(R.id.textDistance);

        mIncludeLoadingSearchDriver = findViewById(R.id.includeLoadingSearchDriver);
        mProgressText = findViewById(R.id.progressText);

        mIncludeSelectExperienceTab = findViewById(R.id.includeSelectExperienceTab);
        mSelectTypeLayout = findViewById(R.id.selectTypeLayout);

        // ############################      SEARCH ADDRESS  TAB      #############################

        mSearchMyLocation = findViewById(R.id.searchMyLocation);
        mSearchDestinyLocation = findViewById(R.id.searchDestinyLocation);

        mBtnMyLoc = findViewById(R.id.btnMyLoc);
        mBtnAddNewLoc = findViewById(R.id.btnAddNewLoc);

        mRecyclerNewDest = findViewById(R.id.recyclerNewDest);
        mRecyclerAddress = findViewById(R.id.recyclerAddress);

        mIncludeCallTab = findViewById(R.id.includeCallTab);

        // ###########################      SELECT TRAVEL TYPE TAB      ###########################

        mTextUberXValue = findViewById(R.id.textUberXValue);
        mTextUberSelectValue = findViewById(R.id.textUberSelectValue);
        mTextUberBlackValue = findViewById(R.id.textUberBlackValue);

        mBtnUberX = findViewById(R.id.btnUberX);
        mBtnUberSelect = findViewById(R.id.btnUberSelect);
        mBtnUberBlack = findViewById(R.id.btnUberBlack);

        mButtonSearchDriver = findViewById(R.id.buttonSearchDriver);

        setListeners();
    }

    /** ###############################      SET LISTENERS     ################################ **/

    private void setListeners() {

        View.OnFocusChangeListener focusListener = (view, hasFocus) -> {
            if (hasFocus) {
                toggleSelectExperienceTab(false);
            } else if (myTrip.getDestination() != null) {
                toggleSelectExperienceTab(true);
            }
        };

        mSearchMyLocation.setOnQueryTextFocusChangeListener(focusListener);
        mSearchDestinyLocation.setOnQueryTextFocusChangeListener(focusListener);

        // ########################      SET LOCATION QUERY LISTENER      #########################

        mSearchMyLocation.setOnQueryTextListener(queryTextListener(START_LOC));
        mSearchDestinyLocation.setOnQueryTextListener(queryTextListener(DESTINATION));

        // ###########################       SET OTHER LISTENERS      #############################
        setClickListeners();

        updateTripStatus(START_TRIP);

        UberHelper.updateTypeButtons(R.id.btnUberSelect, this);
        setTripType(UBER_SLCT);

        getUserLocation(START_LOC);
    }


    private ValueEventListener valueEventListener(int requestCode) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                switch (requestCode) {

                    case UserFirebase.GET_REQS_DATA:
                        mReqsList.clear();
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            String key = data.getKey();
                            mReqsList.add(key);
                        }
                        break;

                    case UserFirebase.GET_TRIP_DATA:
                        try {
                            if (dataSnapshot.exists()) {

                                myTrip = dataSnapshot.getValue(Trip.class);

                                if (myTrip.getDriver() != null) {
                                    if (checkStatus().equals(AWAITING)
                                            || checkStatus().equals(DRIVER_COMING))
                                        updateTripStatus(DRIVER_COMING);
                                }
                            }
                        } catch (Exception e) {
                            throwToast(e.getMessage(), true);
                            Log.d("USERTESTERROR", "onDataChange: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
    }

    private void startTripActivity() {

        // CANCELING TRIP REQUEST
        myTrip.request(false);

        Intent tripIntent = new Intent(this, PassengerTripActivity.class);
        tripIntent.putExtra("trip", myTrip);

        startActivity(tripIntent);
    }


    private void getUserLocation(int requestCode) {

        mLocationRequestCode = requestCode;

        // CHECKING FOR PERMISSIONS REQUIRED IF WERE GRANTED, IF ANYTHING WENT WRONG ON VALIDATION
        if (checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            AlertDialogUtil.permissionValidationAlert(neededPermissions, this);
            return;
        }

        if (mLocationManager == null)
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,// 1 - LOCATION PROVIDER
                1000,             // 2 - MIN. TIME BETWEEN LOC. UPDATES (in milliseconds)
                0,              // 3 - MIN. DISTANCE BETWEEN LOC. UPDATES (in meters)
                locationListener(requestCode));// 4 - LOCATION LISTENER

        if (requestCode == START_LOC)
            mIsLocationEnabled = true;
    }

    private LocationListener locationListener(int requestCode) {
        return location -> {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (requestCode == START_LOC) {
                LatLng passengerLoc = new LatLng(latitude, longitude);

                if (!passengerLoc.equals(new LatLng(mLoggedPassenger.getLatitude(),
                        mLoggedPassenger.getLongitude()))) {

                    mLoggedPassenger.setLatitude(latitude);
                    mLoggedPassenger.setLongitude(longitude);

                    List<UberAddress> myAddressList = UserFirebase.getAddress
                            (this, passengerLoc, UserFirebase.GET_CURRENT_USER_LOC);

                    if (myAddressList != null) {
                        myAddress = myAddressList.get(0);
                        mSearchMyLocation.setQueryHint(myAddress.getAddressLines());

                        myTrip.setStartLoc(myAddress);

                        if(queryLoopHolder){
                            if (mReqsListener == null) setRequestsListener(true);
                            if (mReqsQueryEventListener == null) setGeoQueryReqs(true);
                            queryLoopHolder = false;
                        }
                    }
                }

            } else if (requestCode == DESTINATION) {

                mLocationManager.removeUpdates(locationListener(requestCode));

                LatLng destinationLoc = new LatLng(latitude, longitude);

                List<UberAddress> myAddressList = UserFirebase.getAddress
                        (this, destinationLoc, UserFirebase.GET_CURRENT_USER_LOC);

                if (myAddressList != null) {
                    UberAddress address = myAddressList.get(0);
                    mSearchDestinyLocation.setQueryHint(address.getAddressLines());

                    myTrip.setDestination(address);
                }
            }
            addTripMarker();
        };
    }

    private SearchView.OnQueryTextListener queryTextListener(int viewCode) {

        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String address) {
                SearchView view = viewCode == START_LOC ?
                        mSearchMyLocation : mSearchDestinyLocation;

                if (validateAddressText(address)) {

                    mVIEW_CODE = viewCode;
                    startRecyclerAddress(address);

                    UberHelper.hideKeyboardFrom(view, getApplicationContext());
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                SearchView view = viewCode == START_LOC ?
                        mSearchMyLocation : mSearchDestinyLocation;

                boolean b = s.length() > 0;

                if (!b) {
                    if (mAddressList != null)
                        mAddressList.clear();
                    if (mAdapterAddresses != null)
                        mAdapterAddresses.notifyDataSetChanged();

                    UberHelper.hideKeyboardFrom(view, getApplicationContext());
                }
                return true;
            }
        };
    }

    private void setRequestsListener(boolean isStarting) {

        if (!isStarting){

            if (mReqsListener != null) {
                mReqsRef.removeEventListener(mReqsListener);
                mReqsListener = null;
            }

        }else {
            if (mReqsRef == null) {
                mReqsRef =
                        ConfigurateFirebase.getFireDBRef()
                                .child(ConfigurateFirebase.REQUEST)
                                .child(myAddress.getCountryCode())
                                .child(myAddress.getState());
            }

            if (mReqsListener == null)
                mReqsListener = valueEventListener(UserFirebase.GET_REQS_DATA);

            mReqsRef.addValueEventListener(mReqsListener);
        }
    }

    private void setTripListener(boolean isStarting) {

        if (!isStarting){
            if (mTripListener != null) {
                mTripRef.removeEventListener(mTripListener);
                mTripListener = null;
            }
        }else {
            if (mTripRef == null) {
                mTripRef = ConfigurateFirebase.getFireDBRef()
                        .child(ConfigurateFirebase.TRIP)
                        .child(myTrip.getTripId());
            }

            if (mTripListener == null)
                mTripListener = valueEventListener(UserFirebase.GET_TRIP_DATA);

            mTripRef.addValueEventListener(mTripListener);
        }
    }

    private void setGeoQueryReqs(boolean isStarting){

        if (!isStarting){

            if (mReqsQueryEventListener != null) {
                mGeoQuery.removeAllListeners();
                mReqsQueryEventListener = null;
                createCircles(false);
            }

        }else {
            if (mGeoQuery == null) {
                GeoLocation queryLoc = new GeoLocation(mLoggedPassenger.getLatitude(),
                        mLoggedPassenger.getLongitude());

                mGeoQuery = mGeoFire.queryAtLocation(queryLoc, 0.5);
            }

            if (mReqsQueryEventListener == null) {
                mReqsQueryEventListener = getRequestsGeoQueryEventListeners();
            }

            mGeoQuery.addGeoQueryEventListener(mReqsQueryEventListener);

            createCircles(true);
        }
    }

    private GeoQueryEventListener getRequestsGeoQueryEventListeners() {

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!mReqsList.contains(key))
                    updateDriversMap(key, location, true);
            }

            @Override
            public void onKeyExited(String key) {

                if (!mReqsList.contains(key)) {
                    updateDriversMap(key, null, false);
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

                if (!mReqsList.contains(key))
                    moveMarkers(key, location);
            }

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

    /** ###############################   SET CLICK LISTENERS  ################################ **/

    private void setClickListeners() {
        mIncludeSelectExperienceTab.setOnClickListener(this);

        // ############################      SEARCH ADDRESS  TAB      #############################

        mBtnMyLoc.setOnClickListener(this);
        mBtnAddNewLoc.setOnClickListener(this);

        // ###########################      SELECT TRAVEL TYPE TAB      ###########################

        mBtnUberX.setOnClickListener(this);
        mBtnUberSelect.setOnClickListener(this);
        mBtnUberBlack.setOnClickListener(this);

        mButtonSearchDriver.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();

        if (itemId != R.id.includeCallTab) {
            mAddressList.clear();
            if (mAdapterAddresses != null)
                mAdapterAddresses.notifyDataSetChanged();
        }

        switch (itemId) {

            // ########################      SEARCH ADDRESS  TAB      #########################

            case R.id.btnMyLoc:

                if (!mIsLocationEnabled)
                    getUserLocation(START_LOC);
                break;

            case R.id.btnAddNewLoc:

                startRecyclerNewAddress();
                break;

            // #######################      SELECT TRIP TYPE TAB      #######################

            case R.id.btnUberX:

                UberHelper.updateTypeButtons(itemId, this);
                setTripType(UBER_X);
                break;

            case R.id.btnUberSelect:

                UberHelper.updateTypeButtons(itemId, this);
                setTripType(UBER_SLCT);
                break;

            case R.id.btnUberBlack:

                UberHelper.updateTypeButtons(itemId, this);
                setTripType(UBER_BLACK);
                break;

            case R.id.buttonSearchDriver:
                if (myTrip.getStartLoc() != null && myTrip.getDestination() != null) {
                    if (checkStatus().equals(START_TRIP)) {
                        confirmTripAlertDialog();
                    } else if (checkStatus().equals(AWAITING) ||
                            checkStatus().equals(DRIVER_COMING)) {
                        cancelTrip();
                    }

                }else {

                    if (myTrip.getDestination() == null)
                        throwToast("Your destination is empty, "
                                + "set it before you start your trip.", true);
                    else
                        throwToast("Your start location is empty, "
                                + "set it before you start your trip.", true);
                }
                break;
        }
    }

    private void startRecyclerAddress(String address) {

        mAddressList.clear();

        mAddressList = UserFirebase.getAddressFromName(this, address, mVIEW_CODE);

        if (mAddressList != null) {

            mAdapterAddresses = new AdapterAddresses(mAddressList);

            mRecyclerAddress.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerAddress.setHasFixedSize(true);
            mRecyclerAddress.setAdapter(mAdapterAddresses);

            setRecyclerAddressClickListener();

            mAdapterAddresses.notifyDataSetChanged();
        }
    }

    private void setRecyclerAddressClickListener() {
        mRecyclerAddress.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerAddress,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                UberAddress address = mAddressList.get(position);

                                SearchView searchView = mVIEW_CODE == START_LOC ?
                                        mSearchMyLocation : mSearchDestinyLocation;

                                if (address.getAddressLines().equals(UberAddress.CURRENT_LOC)) {
                                    getUserLocation(mVIEW_CODE);
                                    searchView.setQuery("", false);

                                    if (myTrip.getStartLoc() != null
                                            && myTrip.getDestination() != null)
                                        toggleSelectExperienceTab(true);

                                } else if (address.getAddressLines().equals(UberAddress.NO_LOC_FOUND)) {

                                    searchView.setQuery("", false);

                                } else {

                                    if (mVIEW_CODE == START_LOC) {
                                        if (mIsLocationEnabled) {
                                            mLocationManager.removeUpdates(locationListener(START_LOC));
                                        }
                                        mLoggedPassenger.setLatitude(address.getLatitude());
                                        mLoggedPassenger.setLongitude(address.getLongitude());
                                        myTrip.setPassenger(mLoggedPassenger);

                                        myTrip.setStartLoc(address);
                                        addTripMarker();
                                    } else {
                                        myTrip.setDestination(address);
                                        toggleSelectExperienceTab(true);

                                        addTripMarker();
                                    }
                                    searchView.setQueryHint(address.getAddressLines());
                                    searchView.setQuery("", false);
                                }

                                mAddressList.clear();
                                if (mAdapterAddresses != null)
                                    mAdapterAddresses.notifyDataSetChanged();
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {}

                            @Override
                            public void onItemClick
                                    (AdapterView<?> adapterView, View view, int i, long l) {}
                        }));
    }

    private void startRecyclerNewAddress() {
        //TODO ADD NEW SEARCH EDIT TEXT
    }

    /** ################################      MY METHODS     ################################## **/

    private void startSearchTripDriver(boolean isRestartingRequest) {

        if (!mIsLocationEnabled && mLocationRequestCode == START_LOC)
            getUserLocation(START_LOC);

        calculateTripValues();

        if (isRestartingRequest) {

            myTrip.request(true);

        } else {

            try {
                if (myTrip.save()) {

                    updateTripStatus(AWAITING);
                    if (mTripListener == null) setTripListener(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throwToast(e.getMessage(), true);
            }
        }
    }

    private void cancelTrip() {

        myTrip.setValue(null);

        if (checkStatus().equals(AWAITING) ||
                checkStatus().equals(DRIVER_COMING)) {

            try {
                if (myTrip.cancel()) {
                    throwToast("SEARCH CANCELLED", false);

                    mGeoFire.removeLocation(myTrip.getTripId(),
                            (key, error) -> {
                                if (error == null) {

                                    myTrip.setTripId(null);

                                    updateTripStatus(START_TRIP);

                                } else {
                                    throwToast("ERROR REMOVING GEOFIRE LOCATION: \n"
                                            + error.getMessage(), true);

                                    try {
                                        throw error.toException();
                                    } catch (Exception e) {

                                        e.printStackTrace();
                                        Log.d(TAG, "onComplete: " + e.getMessage());
                                    }
                                }
                            });
                }
            } catch (Exception e) {
                e.printStackTrace();
                throwToast(e.getMessage(), true);
            }
        } else {

            queryLoopHolder = true;

            myTrip.setDestination(null);

            mSearchDestinyLocation.requestFocus();
            mSearchDestinyLocation.setQueryHint(getResources().getString(R.string.text_destination));
            mSearchDestinyLocation.setQuery("", false);
            mSearchDestinyLocation.clearFocus();

            mMap.clear();
            if (mDestinationMark != null) {
                mDestinationMark = null;
            }
            addTripMarker();

            // UPDATES BOTH THE SEARCH LAYOUT && DISPLAY HOME UP ENABLED
            toggleLayoutSearch(true);

            // CLOSES
            toggleDistanceTab(false);
            toggleDurationTab(false);
        }
    }

    private void updateTripStatus(String status) {
        myTrip.setStatus(status);
        if (myTrip.update()) {

            int gone = View.GONE;
            int visible = View.VISIBLE;

            switch (status) {
                case START_TRIP:

                    getActionBar().setDisplayHomeAsUpEnabled(true);

                    // OPEN SELECT TRIP EXPERIENCE TAB
                    updatingSelectExperienceTab(true);

                    // CLOSES PROGRESS BAR SEARCH DRIVER
                    mIncludeLoadingSearchDriver.setVisibility(gone);

                    // UPDATE STATUS
                    mButtonSearchDriver.setText(status);

                    if (myAddress !=null && mReqsListener == null) setRequestsListener(true);

                    break;

                case AWAITING:

                    getActionBar().setDisplayHomeAsUpEnabled(true);

                    // OPEN PROGRESS BAR SEARCH DRIVER
                    mProgressText.setText(status);
                    mIncludeLoadingSearchDriver.setVisibility(visible);

                    // CLOSES SELECT TRIP EXPERIENCE TAB
                    updatingSelectExperienceTab(false);

                    // CANCEL GEOQUERY REQS
                    setGeoQueryReqs(false);

                    // UPDATE GEOFIRE LOCATION
                    GeoLocation geoLocation = new GeoLocation(myTrip.getStartLoc().getLatitude(),
                            myTrip.getStartLoc().getLongitude());
                    mGeoFire.setLocation(
                            myTrip.getTripId(),
                            geoLocation,
                            (key, error) -> {
                                if (error != null) {
                                    throwToast("ERROR UPDATING GEOFIRE LOCATION: \n"
                                            + error.getMessage(), true);
                                    try {
                                        throw error.toException();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Log.d("USERTESTERROR",
                                                "completionListener: " + e.getMessage());
                                    }
                                }

                            });

                    //UPDATE BUTTON TEXT
                    status = "CANCEL SEARCH";
                    mButtonSearchDriver.setText(status);

                    break;
                case DRIVER_COMING:

                    // REMOVE TRIP REQUEST
                    myTrip.request(false);

                    mIncludeLoadingSearchDriver.setVisibility(gone);

                    startTripActivity();
                    break;
            }
        } else {
            throwToast("STATUS UPDATING ERROR", true);
        }
    }

    private void addTripMarker() {

        // CREATE MARKER ON MAP FOR PASSENGER LOCATION
        if (myTrip.getStartLoc() != null) {

            LatLng passengerLoc = new LatLng
                    (myTrip.getStartLoc().getLatitude(), myTrip.getStartLoc().getLongitude());

            if (mPassengerMark != null) {
                mPassengerMark.setPosition(passengerLoc);

            } else {
                mPassengerMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(passengerLoc)
                                .title(mLoggedPassenger.getName())
                                .snippet(myTrip.getStartLoc().getAddressLines())
                                .icon(BitmapHelper.describeFromVector // ADDS PASSENGER MARK
                                        (R.drawable.ic_person_pin_circle_black_24dp, this)));
            }

            if (checkStatus().equals(START_TRIP) && myTrip.getDestination() == null)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLoc, 15));
        }

        // CREATE MARKER ON MAP FOR DESTINATION LOCATION
        if (myTrip.getDestination() != null) {

            LatLng destinationLoc = new LatLng
                    (myTrip.getDestination().getLatitude(), myTrip.getDestination().getLongitude());

            if (mDestinationMark != null) {
                mDestinationMark.setPosition(destinationLoc);


            } else {
                mDestinationMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(destinationLoc)
                                .title("Destination")
                                .snippet(myTrip.getDestination().getAddressLines())
                                .icon(BitmapHelper.describeFromVector // ADDS DESTINATION MARK
                                        (R.drawable.ic_pin_drop_black_24dp, this)));
            }
        }

        // CHECK STATUS OF MY TRIP TO CENTER MARKERS
        if (checkStatus().equals(START_TRIP))
            if (myTrip.getDestination() != null) {
                centerMarkers();
                toggleLayoutSearch(false);
            }
    }

    private void centerMarkers() {

        LatLngBounds.Builder centerBuilder = new LatLngBounds.Builder();

        /* METRICS  ARE UPDATED ON METHOD TOGGLE DRIVER TAB
            FOR MAP SIZE TO BE IN ACCORDANCE WITH DRIVER TAB */

        int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.30);

        LatLng startLoc = mPassengerMark.getPosition();
        LatLng destination = mDestinationMark.getPosition();

        //  DRAWING ROUTES
        getDirections(startLoc, destination);

        //  SETTING CAMERA CENTRALIZATION BUILDER
        centerBuilder.include(startLoc);
        centerBuilder.include(destination);

        LatLngBounds bounds = centerBuilder.build();

        //  SETTING CAMERA
        if (mCurrentPolyline == null)
            mMap.moveCamera
                    (CameraUpdateFactory.newLatLngBounds(bounds, mMapWidth, mMapHeight, padding));
    }

    private void getDirections(LatLng startLoc, LatLng destination) {
        String routeURL = MapDirections.getMapsURL(startLoc, destination);

        // GET DATA FOR DRAWING ROUTES
        new SearchURL(this, true).execute(routeURL, "driving");

        // STARTS ASYNC TO GET ROUTE VALUES
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

                calculateTripValues();
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


    private void calculateTripValues() {

        // UPDATE TEXTS
        String distanceText = mRouteTextMap.get(DISTANCE);
        String durationText = mRouteTextMap.get(DURATION);

        if (myTrip.getValue() == null) {
            // DISTANCE VALUE IS MEASURED IN METERS
            int distanceValue = Integer.parseInt(mRouteTextMap.get(DISTANCEv));

            // DURATION VALUE IS MEASURED IN SECONDS
            int durationValue = Integer.parseInt(mRouteTextMap.get(DURATIONv));

            // SETTING CURRENT TRIP DISTANCE AND DURATION
            myTrip.setDistance(distanceValue);
            myTrip.setDistanceText(distanceText);

            myTrip.setDuration(durationValue);
            myTrip.setDurationText(durationText);

            myTrip.makeValue(myTrip.getTripType());
        }

        if (myTrip.getDestination() != null) {

            String subLocality =
                    (myTrip.getDestination().getSubLocality() != null) ?
                            myTrip.getDestination().getSubLocality() + ", " : "";

            String distanceText1 = "Destination -> "
                    + myTrip.getDestination().getAddress() + ", "
                    + myTrip.getDestination().getAddressNum() + ", "
                    + subLocality + myTrip.getDestination().getCity() + "."
                    + "\n" + "Distance - " + distanceText;

            mTextDistance.setText(distanceText1);
            mTextDuration.setText(durationText);

            toggleDistanceTab(true);
            toggleDurationTab(true);
        }

        if (checkStatus().equals(START_TRIP) || checkStatus().equals(AWAITING)) {

            mTextUberXValue.setText("R$ " + myTrip.calculate(UBER_X));
            mTextUberSelectValue.setText("R$ " + myTrip.calculate(UBER_SLCT));
            mTextUberBlackValue.setText("R$ " + myTrip.calculate(UBER_BLACK));
        }
    }

   // ###############################      GEO QUERY METHODS     ##################################

    private void updateDriversMap(String key, GeoLocation location, boolean isAdding) {

        if (isAdding) {
            Log.d(TAG_TEST, "onKeyEntered: "+key + " ENTERED");

            LatLng loc = new LatLng(location.latitude, location.longitude);

            Marker marker =
                    mMap.addMarker(
                            new MarkerOptions()
                                    .position(loc)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.drawable.icons_carr))
                                    .visible(true));

            mDriversLocMap.put(key, marker);

        }else {
            Log.d(TAG_TEST, "onKeyExited: "+ key + " EXITED");


            if (mDriversLocMap.containsKey(key)) {

                Marker marker = mDriversLocMap.get(key);

                marker.remove();
                mDriversLocMap.remove(key);
            }
        }
    }

    private void moveMarkers(String key, GeoLocation location) {

        Log.d(TAG_TEST, "onKeyMoved: " + key + " MOVED");

        if (mDriversLocMap.containsKey(key)) {
            Marker marker = mDriversLocMap.get(key);

            LatLng newLatLng =
                    new LatLng(location.latitude, location.longitude);

            double angle = SphericalUtil.computeHeading(marker.getPosition(), newLatLng);

            marker.setIcon(BitmapDescriptorFactory
                    .fromBitmap(BitmapHelper
                            .rotateDriverIconBitmap(getResources(), angle)));

            marker.setVisible(true);
            marker.setPosition(newLatLng);

            mDriversLocMap.put(key, marker);

        } else {
            LatLng loc = new LatLng(location.latitude, location.longitude);


            Marker marker =
                    mMap.addMarker(
                            new MarkerOptions()
                                    .position(loc)
                                    .visible(false));

            mDriversLocMap.put(key, marker);
        }
    }

    private void createCircles(boolean isAdding) {

        if (!isAdding){

            if (mQueryReqsCircle!=null) {
                mQueryReqsCircle.remove();
                Log.d(TAG, "createCircles: REMOVING CIRCLE");
            }

        }else {
            CircleOptions circleOptions;

            // ADDING CIRCLE FOR DEMONSTRATION

            circleOptions = UberHelper.circleOptions // ADDS PASSENGER REQS CRICLE
                    (uType, new LatLng(mLoggedPassenger.getLatitude(),
                            mLoggedPassenger.getLongitude()), false);

            if (mQueryReqsCircle != null) mQueryReqsCircle.remove();
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

    private void stopActions(){

        setTripListener(false);

        setRequestsListener(false);

        if (mIsLocationEnabled) {
            mLocationManager.removeUpdates(locationListener(START_LOC));
            mIsLocationEnabled = false;
        }

    }

    private void resumeActions() {

        if (myTrip!=null && myTrip.getDestination()!= null) {
            if (mTripListener != null) setTripListener(true);
            if (mReqsListener != null) setRequestsListener(true);

            if (myTrip.isPassengerCancelling()) myTrip.setPassengerCancelling(false);

            startSearchTripDriver(true);
            updateTripStatus(AWAITING);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resumeActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeActions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopActions();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopActions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActions();
    }

    /** ##############################     ACTIVITY PROCESSES    ############################## **/

    @Override
    public boolean onNavigateUp() {

        cancelTrip();
        return true;
    }

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
                AlertDialogUtil.permissionValidationAlert(UberHelper.neededPermissions, this);
            } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                getUserLocation(START_LOC);
            }
        }
    }

    /** #################################        HELPERS       ################################ **/

    private boolean validateAddressText(String addressText) {
        if (addressText != null && !addressText.isEmpty())
            return true;
        else {
            String errorMessage = getResources().getString(R.string.text_empty_address);
            throwToast(errorMessage, true);
            return false;
        }
    }

    private void setTripType(double tripType) {
        myTrip.setTripType(tripType);

        if (myTrip.getDestination() != null){
            myTrip.makeValue(tripType);
        }
    }

    private String checkStatus() {
        return myTrip.getStatus();
    }

    private void confirmTripAlertDialog() {

        String startLoc = "\nSTART LOCATION: \n"
                + myTrip.getStartLoc().getAddress() + ", "
                + myTrip.getStartLoc().getAddressNum() + ", "
                + myTrip.getStartLoc().getSubLocality() + ", "
                + myTrip.getStartLoc().getCity() + " - "
                + myTrip.getStartLoc().getState() + "/"
                + myTrip.getStartLoc().getCountryCode() + "\n\n";

        String destination = "DESTINATION: \n"
                + myTrip.getDestination().getAddress() + ", "
                + myTrip.getDestination().getAddressNum() + ", "
                + myTrip.getDestination().getSubLocality() + ", "
                + myTrip.getDestination().getCity() + " - "
                + myTrip.getDestination().getState() + "/"
                + myTrip.getDestination().getCountryCode() + "\n\n";

        String value = "R$ " + myTrip.calculate(myTrip.getTripType());

        String tripText = UberHelper.
                returnTypeName(myTrip.getTripType(), getResources()).toUpperCase() + "\n"
                + startLoc
                + destination
                + value;

        AlertDialogUtil.confirmTripPaymentAlertDialog
                (this, tripText, (dialog, which) -> startSearchTripDriver(false));
    }

    private void toggleSelectExperienceTab(boolean isOpening) {
        mIncludeSelectExperienceTab.setVisibility(isOpening ? View.VISIBLE : View.GONE);

        // UPDATE METRICS FOR MAP ACCORDINGLY WITH THE DRIVER TAB
        updateMapMetrics();
    }

    private void updateMapMetrics() {
        mMapWidth = mMapFragment.getResources().getDisplayMetrics().widthPixels;
        mMapHeight = mMapFragment.getResources().getDisplayMetrics().heightPixels;
    }

    private void toggleDistanceTab(boolean isOpening){
        findViewById(R.id.layout_distance).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void toggleDurationTab(boolean isOpening){
        findViewById(R.id.layout_duration).setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void toggleLayoutSearch(boolean isOpening) {

        getActionBar().setDisplayHomeAsUpEnabled(!isOpening);
        mIncludeCallTab.setVisibility(isOpening ? View.VISIBLE : View.GONE);
    }

    private void updatingSelectExperienceTab(boolean makeVisible) {
        mSelectTypeLayout.setVisibility(makeVisible ? View.VISIBLE : View.GONE);
    }

    private void throwToast(String message, boolean isLong) {
        int length = isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;

        Toast.makeText(this, message, length).show();
    }
}




