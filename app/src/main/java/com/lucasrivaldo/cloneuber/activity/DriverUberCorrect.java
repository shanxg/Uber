package com.lucasrivaldo.cloneuber.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import com.lucasrivaldo.cloneuber.adapter.AdapterTrips;
import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.RecyclerItemClickListener;
import com.lucasrivaldo.cloneuber.helper.SystemPermissions;
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

import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG_TEST;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.TAG;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_ARRIVED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.ON_THE_WAY;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.PASSENGER_ABOARD;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.TRIP_FINALIZED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_SLCT;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_X;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;


public class DriverUberCorrect extends FragmentActivity
        implements OnMapReadyCallback, TaskLoadedCallback {

    private static final String uType = ConfigurateFirebase.TYPE_DRIVER;

    private String[] needPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private boolean isSelected;
    private String  statusLoopHolder = null;

    private GoogleMap mMap;

    private User loggedDriver;
    private Trip currentTrip;
    private Marker driverMark, passengerMark, destinationMark;

    private UberAddress myAddress;
    private List<String> requestList;
    private List<Trip> tripList;

    private RecyclerView recyclerReqs;
    private AdapterTrips adapterTrips;

    private DatabaseReference requestRef, tripsRef;
    private ValueEventListener requestsEventListener;
    private ValueEventListener cTripValueEventListener;

    private GeoFire geoFire;
    private GeoQuery geoQueryPassenger, geoQueryDestination, geoQueryReqs;
    private GeoQueryEventListener tripStartQueryEventListener, tripDestQueryEventListener,
            reqsQueryEventListener;
    private Circle queryReqsCircle, queryTripCircle;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private SearchView searchMyLocation, searchDestinyLocation;
    private TextView textTripDescriptionType, textUberDescriptionValue, textUberUserName,
            textDuration, textDistance, progressText;
    private CircleImageView civ_uberDescription;

    private TextView buttonWork;
    private ImageView imgStop;
    private ConstraintLayout workLayout;
    private LinearLayout includeTripTab, includeProgressBar;
    private Button buttonSearchDriver;

    private HashMap<String, String> routeTextMap;
    private Polyline currentPolyline;
    //private QueryTask queryTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO REDIRECTS TO TEST ACTIVITY
        startActivity(new Intent(this, DriverTripActivity.class));
        finish();

        /*
        preLoad();
        // STARTS ACTICITY ONLY IF USER DATA NOT NULL
        UserFirebase.getLoggedUserData
                (uType, valueEventListener(UserFirebase.GET_LOGGED_USER_DATA));

         */

    }

    private void preLoad() {
        requestList = new ArrayList<>();
        tripList = new ArrayList<>();

        geoFire = ConfigurateFirebase.getGeoFire();
        /*
        tripStartQueryEventListener = null;
        reqsQueryEventListener = null;
         */
    }

    private void setContentView() {
        setContentView(R.layout.activity_uber_driver);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("(Clone) Uber Driver");
        setActionBar(toolbar);

        UserFirebase.setLastLogin(uType);
        SystemPermissions.validatePermissions(needPermissions, this, 1);

        loadInterface();


        throwToast("You logged as a driver successfully.", true);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDriver);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setOnMapClickListener();

        //STARTS LOCATION LISTENER FOR LOGGED DRIVER
        setLocationManager();
    }

    private void loadInterface() {

        // INCLUDE LAYOUT DURATION && DISTANCE
        textDuration = findViewById(R.id.textDuration);
        textDistance = findViewById(R.id.textDistance);

        //  INCLUDE LAYOUT PROGRESSBAR
        progressText = findViewById(R.id.progressText);
        includeProgressBar = findViewById(R.id.includeProgressBar);
        toggleProgressBar(false);

        // LAYOUT BUTTON WORK
        workLayout = findViewById(R.id.workLayout);
        buttonWork = findViewById(R.id.buttonStart);
        imgStop = findViewById(R.id.imgStop);

        // REQUESTS RECYCLER
        recyclerReqs = findViewById(R.id.recyclerReqs);

        // INCLUDE LAYOUT TRIP TAB
        includeTripTab = findViewById(R.id.includeTripTab);

        findViewById(R.id.selectTypeLayout).setVisibility(View.GONE);


        civ_uberDescription = findViewById(R.id.civ_uberDescription);
        textTripDescriptionType = findViewById(R.id.textTripDescriptionType);
        textUberDescriptionValue = findViewById(R.id.textUberDescriptionValue);

        TextView textDescriptor = findViewById(R.id.textUberTripTabUserDescriptor);
        textDescriptor.setText("Passenger");

        textUberUserName = findViewById(R.id.textUberUserName);

        searchMyLocation = findViewById(R.id.searchMyLocationDescription);
        findViewById(R.id.btnMyLocDescription).setVisibility(View.GONE);

        searchDestinyLocation = findViewById(R.id.searchDestinyLocationDescription);
        findViewById(R.id.btnAddNewLocDescription).setVisibility(View.GONE);

        findViewById(R.id.textMaxDest).setVisibility(View.GONE);

        buttonSearchDriver = findViewById(R.id.buttonSearchDriver);
        buttonSearchDriver.setVisibility(View.GONE);
        toggleTripNWorkLayout(false);

        // SET LISTENERS
        setClickListeners();
        setListeners();
        startRecyclerRequests();
    }

    private void startRecyclerRequests() {
        adapterTrips = new AdapterTrips(tripList, this);

        recyclerReqs.setLayoutManager(new LinearLayoutManager(this));
        recyclerReqs.setHasFixedSize(true);
        recyclerReqs.setAdapter(adapterTrips);

        setRecyclerReqsClickListeners();
    }

    /**
     * ###############################      SET LISTENERS     ################################
     **/

    private ValueEventListener valueEventListener(int requestCode) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                switch (requestCode) {

                    case UserFirebase.GET_LOGGED_USER_DATA:
                        if (dataSnapshot.exists()) {
                            loggedDriver = dataSnapshot.getValue(User.class);

                            if (loggedDriver != null) {
                                setContentView();
                            }
                        }
                        break;

                    case UserFirebase.GET_TRIP_DATA:
                        if (dataSnapshot.exists()) {
                            currentTrip = dataSnapshot.getValue(Trip.class);

                            if (currentTrip != null && currentTrip.getDriver() == null) {
                                currentTrip.setDriver(loggedDriver);
                                updateTripStatus(DRIVER_COMING);


                            } else if (currentTrip != null && currentTrip.getDriver() != null
                                    && !currentTrip.getDriver().getId().equals(loggedDriver.getId())) {

                                throwToast("Another driver has initiated this trip",
                                        false);
                            } else {
                                if (checkStatus().equals(PASSENGER_ABOARD))
                                    updateTripStatus(PASSENGER_ABOARD);
                            }

                        }
                        break;

                    case UserFirebase.GET_TRIPS_DATA:
                        Trip trip = dataSnapshot.getValue(Trip.class);

                        if (!tripList.contains(trip)) {
                            tripList.add(trip);
                            adapterTrips.notifyDataSetChanged();
                        }

                        break;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
    }

    private void setOnMapClickListener() {
        GoogleMap.OnMapClickListener mapClickListener = latLng -> {

            loggedDriver.setLatitude(latLng.latitude);
            loggedDriver.setLongitude(latLng.longitude);

            addTripMarker();
        };
        mMap.setOnMapClickListener(mapClickListener);
    }

    private void setRecyclerReqsClickListeners() {
        recyclerReqs.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerReqs,
                        new RecyclerItemClickListener.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                Trip trip = tripList.get(position);

                                // ACCEPT TRIP & STARTS TRIP LISTENER
                                startTripListener(trip);

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                            }

                            @Override
                            public void onItemClick
                                    (AdapterView<?> adapterView, View view, int i, long l) {
                            }
                        }));
    }

    private void startTripListener(Trip trip) {
        tripsRef =
                ConfigurateFirebase.getFireDBRef()
                        .child(ConfigurateFirebase.TRIP)
                        .child(trip.getTripId());

        cTripValueEventListener =
                tripsRef.addValueEventListener(valueEventListener(UserFirebase.GET_TRIP_DATA));
    }

    @SuppressLint("NewApi")
    private void setLocationManager() {

        // CHECKING FOR PERMISSIONS REQUIRED IF WERE GRANTED, IF ANYTHING WENT WRONG ON VALIDATION

        if (checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            permisionValidationAlert();
            return;
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /*  WHAT WE NEED TO GET USER CURRENT LOCATION
                1 - LOCATION PROVIDER
                2 - MINIMUM TIME BETWEEN LOCATION UPDATES (in milliseconds)
                3 - MINIMUM DISTANCE BETWEEN LOC. UPDATES (in meters)
                4 - LOCATION LISTENER
                */

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0,
                locationListener);

    }

    private void setListeners() {
        locationListener = location -> {

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            LatLng driverLatLng = new LatLng(latitude, longitude);
            GeoLocation geoLoc = new GeoLocation(latitude, longitude);

            if (!geoLoc.equals(
                    new GeoLocation(loggedDriver.getLatitude(), loggedDriver.getLongitude()))) {

                loggedDriver.setLatitude(latitude);
                loggedDriver.setLongitude(longitude);

                if (buttonWork.isSelected()) {
                    // UPDATE GEOFIRE LOCATION
                    geoFire.setLocation(loggedDriver.getId(),
                            geoLoc,
                            completionListener());

                    // SET GEOQUERY FOR TRIP REQUESTS

                    //setGeoLocationQueryListener(latitude, longitude, false);
                }

                if (currentTrip != null) {
                    currentTrip.updateDriverLocation(latitude, longitude);

                    geoFire.setLocation(currentTrip.getTripId(),
                            geoLoc,
                            completionListener());
                }

                if (myAddress == null) {
                    List<UberAddress> addressList = UserFirebase.getAddress
                            (this, driverLatLng, UserFirebase.GET_CURRENT_USER_LOC);

                    if (addressList != null) {
                        myAddress = addressList.get(0);
                    }
                }

                // DRAW ROUTES AND UPDATE TEXTS
                addTripMarker();
            }
        };
    }



    private void getTripsData(String tripId) {
        tripsRef =
                ConfigurateFirebase.getFireDBRef()
                        .child(ConfigurateFirebase.TRIP)
                        .child(tripId);

        tripsRef.addListenerForSingleValueEvent(valueEventListener(UserFirebase.GET_TRIPS_DATA));
    }

    private void setClickListeners(){

        View.OnClickListener clickListener = view -> {

            int itemId = view.getId();
            switch (itemId) {
                case R.id.workLayout:

                    isSelected = !buttonWork.isSelected();

                    toggleStartWorkInterface(isSelected);
                    startWorkRequests(isSelected);

                    break;
                case R.id.buttonSearchDriver:

                    switch (checkStatus()) {
                        case DRIVER_COMING:
                            cancelTrip();
                            break;
                        case PASSENGER_ABOARD:
                            updateTripStatus(ON_THE_WAY);
                            break;
                        case TRIP_FINALIZED:
                            confirmTripPaymentAlertDialog();
                            break;
                    }

                    break;

            }
        };

        buttonSearchDriver.setOnClickListener(clickListener);
        workLayout.setOnClickListener(clickListener);
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


    /** ################################      MY METHODS     ################################## **/

    private void updateTripStatus(String status){
        currentTrip.setStatus(status);
        if(currentTrip.update()){

            if (status.equals(AWAITING))
                currentTrip = null;

            addTripMarker();

            int gone = View.GONE;
            int visible = View.VISIBLE;

            switch (status) {

                case DRIVER_COMING:
                    // <ALREADY CALLED>

                    // CLEAR REQUEST LIST
                    cancelTripsListeners(false);

                    // RESETS BUTTON START WORK
                    buttonWork.setSelected(false);
                    toggleStartWorkInterface(false);

                    // OPEN BUTTON START TRAVEL DISABLED
                    buttonSearchDriver.setText(getResources().getString(R.string.text_cancel_trip));
                    buttonSearchDriver.setEnabled(true);
                    buttonSearchDriver.setVisibility(visible);

                    // HIDE SEARCH TRIP BAR
                    toggleProgressBar(false);

                    // SETS TRIP TAB
                    setTripTab();

                    // OPEN TRIP DESCRIPTION TAB
                    toggleTripNWorkLayout(true);

                    // CREATE GEOQUERY AT STARTLOC FOR NOTIFICATION WHEN DRIVER ARRIVES
                    if (statusLoopHolder == null) {

                        /*
                        cancelQueryEventListener();
                        setGeoLocationQueryListener(currentTrip.getStartLoc().getLatitude(),
                                                    currentTrip.getStartLoc().getLongitude(),
                                                   true);
                        */
                        statusLoopHolder = (DRIVER_COMING);
                    }

                    // OPEN DURATION AND DISTANCE TEXTS
                    toggleDurationTab(true);
                    toggleDistanceTab(true);

                    break;

                case DRIVER_ARRIVED:
                    // <ALREADY CALLED>

                    // OPEN PROGRESS WAITING FOR PASSENGER
                    progressText.setText("Waiting for passenger");
                    toggleProgressBar(true);

                    // OPEN BUTTON START TRAVEL DISABLED
                    buttonSearchDriver.setText("START TRIP");
                    buttonSearchDriver.setEnabled(false);
                    buttonSearchDriver.setVisibility(visible);

                    // CLOSE DURATION AND DISTANCE TEXTS
                    toggleDurationTab(false);
                    toggleDistanceTab(false);



                    break;

                case PASSENGER_ABOARD:
                    // <ALREADY CALLED>

                    // ENABLE BUTTON START TRAVEL
                    buttonSearchDriver.setEnabled(true);

                    // CLOSE PROGRESS WAITING FOR PASSENGER
                    toggleProgressBar(false);

                    break;

                case ON_THE_WAY:
                    // <ALREADY CALLED>

                    // CLOSE BUTTON START TRAVEL
                    buttonSearchDriver.setVisibility(gone);

                    // OPEN DURATION AND DISTANCE TEXTS
                    toggleDurationTab(true);
                    toggleDistanceTab(true);

                    // CREATE GEOQUERY AT DESTINATION FOR NOTIFICATION WHEN ARRIVES
                    if (statusLoopHolder.equals(DRIVER_COMING)) {
                        /*
                        cancelQueryEventListener();
                        setGeoLocationQueryListener(currentTrip.getDestination().getLatitude(),
                                                    currentTrip.getDestination().getLongitude(),
                                                   true);
                        */
                        statusLoopHolder = (ON_THE_WAY);
                    }

                    break;

                case TRIP_FINALIZED:
                    // <ALREADY CALLED>

                    // OPEN BUTTON FINALIZE TRAVEL
                    buttonSearchDriver.setText("FINALIZE TRIP");
                    buttonSearchDriver.setEnabled(true);
                    buttonSearchDriver.setVisibility(visible);


                    break;
            }
        }else{
            throwToast("STATUS UPDATING ERROR",true);
        }
    }





    private void startWorkRequests(boolean isStarting){
        if (isStarting) {
            progressText.setText("SEARCHING TRIP");
            toggleProgressBar(true);
            setLocationManager();

            //UPDATE GEOFIRE LOCATION
            geoFire.setLocation(loggedDriver.getId(),
                    new GeoLocation(loggedDriver.getLatitude(), loggedDriver.getLongitude()),
                    completionListener());

            //SET GEOQUERY FOR TRIP REQUESTS
            setGeoQueryReqs(false);
            /*
            setGeoLocationQueryListener
                    ( loggedDriver.getLatitude(), loggedDriver.getLongitude(), false);

            */

            requestRef =
                    ConfigurateFirebase.getFireDBRef()
                            .child(ConfigurateFirebase.REQUEST)
                            .child(myAddress.getCountryCode())
                            .child(myAddress.getState());


            requestList.clear();
            requestsEventListener = requestRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    requestList.clear();
                    for (DataSnapshot data: dataSnapshot.getChildren()){
                        String key = data.getKey();
                        if (!requestList.contains(key))
                            requestList.add(key);
                    }
                    adapterTrips.notifyDataSetChanged();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }else { // IF STOPPED REQUESTS SEARCH

            // REMOVE LOCATION LISTENER
            locationManager.removeUpdates(locationListener);
            toggleProgressBar(false);

            cancelTripsListeners(true);
        }
    }

    private void cancelTripsListeners(boolean isFinalizingTrip) {

        // REMOVE GEOFIRE LOCATION AND QUERIES
        //cancelQueryEventListener();
        geoFire.removeLocation(UserFirebase.getCurrentUserID(), completionListener());

        // REMOVE REQUESTS LISTENER
        requestRef.removeEventListener(requestsEventListener);

        // REMOVE TRIPS LISTENER
        if (isFinalizingTrip && cTripValueEventListener!=null)
            tripsRef.removeEventListener(cTripValueEventListener);

        // CLEAR ALL LISTS
        requestList.clear();
        tripList.clear();

        adapterTrips.notifyDataSetChanged();
    }

    private void cancelTrip(){

        currentTrip.setDriver(null);
        updateTripStatus(AWAITING);

        // CLEAR LISTENERS
        cancelTripsListeners(true);

        // CLOSES BUTTON START TRAVEL DISABLED
        buttonSearchDriver.setEnabled(true);
        buttonSearchDriver.setVisibility(View.GONE);

        // CLOSES TRIP DESCRIPTION TAB && OPEN WORK LAYOUT
        toggleTripNWorkLayout(false);

        // CLOSES DURATION AND DISTANCE TEXTS
        toggleDurationTab(false);
        toggleDistanceTab(false);

        queryTripCircle.remove();
        currentPolyline.remove();
    }

    private void finalizeTrip() {
        cancelTripsListeners(true);

        startActivity(new Intent(this, UberDriverActivity.class));
        finish();
    }

    private void setTripTab() {
        civ_uberDescription.setImageResource(returnImageResourceId(currentTrip.getTripType()));
        textTripDescriptionType.setText(returnTypeName(currentTrip.getTripType()));
        textUberDescriptionValue.setText(currentTrip.getValue());

        textUberUserName.setText(currentTrip.getPassenger().getName());

        searchMyLocation.setQueryHint(currentTrip.getStartLoc().getAddressLines());
        findViewById(R.id.myLocLayoutDescription).setEnabled(false);
        findViewById(R.id.myLocLayoutDescription).setFocusable(false);

        searchDestinyLocation.setQueryHint(currentTrip.getDestination().getAddressLines());
        findViewById(R.id.destinationLayoutDescription).setEnabled(false);
        findViewById(R.id.destinationLayoutDescription).setFocusable(false);
    }

    private void addTripMarker() {

        // CREATE MARKER ON MAP FOR DRIVER LOCATION
        LatLng driverLoc = new LatLng
                (loggedDriver.getLatitude(), loggedDriver.getLongitude());

        if (driverMark != null) {

            double angle = SphericalUtil.computeHeading(driverMark.getPosition(), driverLoc);

            if (!driverMark.getPosition().equals(driverLoc)) {
                driverMark.setIcon(BitmapDescriptorFactory.fromBitmap(rotateDriverIconBitmap(angle)));
                driverMark.setPosition(driverLoc);
            }
            driverMark.setVisible(true);
        } else {
            driverMark = mMap.addMarker(
                    new MarkerOptions()
                            .position(driverLoc)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_carr)));
        }

        // CENTER DRIVER WHEN LOOKING FOR TRIPS
        if (currentTrip==null)
            if (queryReqsCircle !=null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(queryReqsCircle.getCenter(), 15));
            }else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLoc, 15));
            }

        // CREATE MARKERS FOR TRIP
        if (currentTrip != null) {

            // CREATE MARKER ON MAP FOR PASSENGER LOCATION
            LatLng passengerLoc = new LatLng
                    (currentTrip.getStartLoc().getLatitude(),
                            currentTrip.getStartLoc().getLongitude());

            if (passengerMark != null) {
                passengerMark.setPosition(passengerLoc);

            } else {
                passengerMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(passengerLoc)
                                .title(currentTrip.getPassenger().getName())
                                .icon(bitmapDescriptorFromVector
                                        (R.drawable.ic_person_pin_circle_black_24dp)));
            }

            if (checkStatus().equals(DRIVER_ARRIVED))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLoc, 18));


            // CREATE MARKER ON MAP FOR DESTINATION LOCATION
            LatLng destinationLoc = new LatLng
                    (currentTrip.getDestination().getLatitude(),
                            currentTrip.getDestination().getLongitude());

            if (destinationMark != null) {
                destinationMark.setPosition(destinationLoc);

            } else {
                destinationMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(destinationLoc)
                                .title("Destination")
                                .snippet(currentTrip.getDestination().getAddressLines())
                                .icon(bitmapDescriptorFromVector
                                        (R.drawable.ic_pin_drop_black_24dp)));
            }

            if (checkStatus().equals(TRIP_FINALIZED))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLoc, 18));
        }

        getRoutes();
    }

    private void getRoutes() {
        if (currentTrip != null)
            if (checkStatus().equals(AWAITING) || checkStatus().equals(DRIVER_COMING)) {


                LatLng driverLoc = driverMark.getPosition();
                LatLng passenger = passengerMark.getPosition();

                //  DRAWING ROUTES
                getDirections(driverLoc, passenger);


            } else if (checkStatus().equals(ON_THE_WAY)) {


                LatLng driverLoc = driverMark.getPosition();
                LatLng destinationLoc = destinationMark.getPosition();

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
        for (Object value :values) {
            if (value.getClass().equals(PolylineOptions.class)) {
                if (currentPolyline != null)
                    currentPolyline.remove();

                currentPolyline = mMap.addPolyline((PolylineOptions) value);
                centerPolyLine();

            } else if(value.getClass().equals(HashMap.class)){

                routeTextMap = (HashMap<String, String>) value;

                getRoutesValues();
            }else if (value.getClass().equals(CircleOptions.class)){

                queryReqsCircle = mMap.addCircle((CircleOptions) value);
            }
        }
    }

    private void centerPolyLine(){

        LatLngBounds.Builder centerBuilder = new LatLngBounds.Builder();

        //  SETTING CAMERA CENTRALIZATION BUILDER
        for(LatLng latLng : currentPolyline.getPoints()){
            centerBuilder.include(latLng);
        }
        LatLngBounds bounds = centerBuilder.build();

        int padding = (int) (getResources().getDisplayMetrics().widthPixels*0.2);

        mMap.moveCamera
                (CameraUpdateFactory.newLatLngBounds(bounds,padding));
    }

    private void getRoutesValues() {

        // UPDATE TEXTS
        String distanceText = routeTextMap.get(DISTANCE);
        String durationText = routeTextMap.get(DURATION);

        String subLocality =
                (currentTrip.getDestination().getSubLocality() != null) ?
                        currentTrip.getDestination().getSubLocality() + ", " : "";

        String distanceText1 = "Destination -> "
                + currentTrip.getDestination().getAddress() + ", "
                + currentTrip.getDestination().getAddressNum() + ", "
                + subLocality + currentTrip.getDestination().getCity() + "."
                + "\n" + "Distance - " + distanceText;

        textDistance.setText(distanceText1);
        textDuration.setText(durationText);

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
        switch (item.getItemId()){
            case R.id.menuSignOut:
                if(UserFirebase.signOut()) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissionResult : grantResults) {
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                permisionValidationAlert();
            } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                setLocationManager();
            }
        }

    }

    /** #################################        HELPERS       ################################ **/

    private void confirmTripPaymentAlertDialog(){

        String startLoc = "\nSTART LOCATION: \n";

        String destination = "DESTINATION: \n";

        String value = "R$ "+currentTrip.calculate(currentTrip.getTripType());

        String tripText = returnTypeName(currentTrip.getTripType()).toUpperCase()+"\n"
                + startLoc
                + destination
                + value;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip confirmation:");
        builder.setMessage(tripText);
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm",
                (dialog, which) -> finalizeTrip());
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {});

        AlertDialog alert = builder.create();
        alert.show();
    }

    private BitmapDescriptor bitmapDescriptorFromVector(@DrawableRes int vectorDrawableResourceId) {

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

        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorDrawableResourceId);
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

    Bitmap rotateDriverIconBitmap(double angle) {
        Bitmap source = BitmapFactory.decodeResource(getResources(),
                R.drawable.icons_carr);
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angle);

        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    private void permisionValidationAlert(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions denied:");
        builder.setMessage("To keep using the App, you need to accept the Requested permissions.");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm",
                (dialog, which) -> SystemPermissions.validatePermissions
                        (needPermissions, this, 1));

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void toggleStartWorkInterface(boolean isSelected){

        // BUTTON SETTINGS
        String btnText = isSelected? " " :  getResources().getString(R.string.btn_start_text);

        buttonWork.setText(btnText);
        buttonWork.setSelected(isSelected);

        // IMAGE STOP SETTINGS
        int visibility = isSelected? View.VISIBLE : View.GONE;
        imgStop.setVisibility(visibility);
    }

    private void toggleTripNWorkLayout(boolean isOpening) {
        int gone = View.GONE;
        int visible= View.VISIBLE;

        if (isOpening) {
            includeTripTab.setVisibility(visible);
            workLayout.setVisibility(gone);
        }else {
            includeTripTab.setVisibility(gone);
            workLayout.setVisibility(visible);
        }
    }

    private void toggleDistanceTab(boolean isOpening){
        findViewById(R.id.layout_distance).setVisibility(isOpening? View.VISIBLE: View.GONE);
    }

    private void toggleDurationTab(boolean isOpening){
        findViewById(R.id.layout_duration).setVisibility(isOpening? View.VISIBLE: View.GONE);
    }

    private void toggleProgressBar(boolean isOpening) {
        includeProgressBar.setVisibility(isOpening? View.VISIBLE: View.GONE);
    }

    private void throwToast(String message, boolean isLong){
        int  lenght = Toast.LENGTH_LONG;
        if(!isLong)
            lenght = Toast.LENGTH_SHORT;

        Toast.makeText
                (this, message, lenght).show();

    }

    private String returnTypeName(double type){
        String typeText;

        if (type == UBER_X)
            typeText = getResources().getString(R.string.text_uber_x);
        else if (type == UBER_SLCT)
            typeText = getResources().getString(R.string.text_uber_select);
        else
            typeText = getResources().getString(R.string.text_uber_black);

        return typeText;
    }

    private int returnImageResourceId(double type){
        int resId;

        if (type == UBER_X)
            resId = R.drawable.uberx;
        else if (type == UBER_SLCT)
            resId = R.drawable.uber_select;
        else
            resId = R.drawable.uber_black;

        return resId;
    }

    private boolean checkTrips(String key) {
        for (Trip trip : tripList) {
            if (trip.getTripId().equals(key)){

                tripList.remove(trip);
                requestList.remove(key);

                return true;
            }
        }
        return false;
    }

    private String checkStatus(){
        return currentTrip.getStatus();
    }


    private void setGeoQueryReqs(boolean isCanceling){

        if (isCanceling){
            throwToast("canceling reqs query", false);
            geoQueryReqs.removeAllListeners();
            //queryTask.cancel(true);

        }else {
            throwToast("setting reqs query", false);
            GeoLocation queryLoc = new GeoLocation(loggedDriver.getLatitude(),
                    loggedDriver.getLongitude());



            reqsQueryEventListener = getGeoQueryEventListeners(false);

            geoQueryReqs = geoFire.queryAtLocation(queryLoc, 1);
            geoQueryReqs.addGeoQueryEventListener(reqsQueryEventListener);

            //queryTask = new QueryTask(geoQueryReqs, reqsQueryEventListener);
            //queryTask.execute();

            addCircles();
        }
    }

    private void setGeoQueryForTrip() {
        double radius = 0.015; //(15m) measured in km

        GeoLocation startLoc = new GeoLocation(currentTrip.getStartLoc().getLatitude(),
                currentTrip.getStartLoc().getLongitude());

        geoQueryPassenger = geoFire.queryAtLocation(startLoc, radius);

        GeoLocation destination = new GeoLocation(currentTrip.getDestination().getLatitude(),
                currentTrip.getDestination().getLongitude());

        geoQueryDestination = geoFire.queryAtLocation(destination, radius);


        tripStartQueryEventListener = getGeoQueryEventListeners(true);
        tripDestQueryEventListener = getGeoQueryEventListeners(true);

        geoQueryPassenger.addGeoQueryEventListener(tripStartQueryEventListener);
        geoQueryDestination.addGeoQueryEventListener(tripDestQueryEventListener);
    }

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


    private GeoQueryEventListener tripQueryEventListener() {
        LatLng latLng;

        if (checkStatus().equals(DRIVER_COMING)|| checkStatus().equals(AWAITING)) {
            latLng = new LatLng(currentTrip.getStartLoc().getLatitude(),
                    currentTrip.getStartLoc().getLongitude());
        }else {
            latLng = new LatLng(currentTrip.getDestination().getLatitude(),
                    currentTrip.getDestination().getLongitude());
        }

        CircleOptions circleOptions = circleOptions(latLng, true);

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (key.equals(currentTrip.getTripId()))
                    updateTripStatus(checkStatus().equals(DRIVER_COMING) ?
                            DRIVER_ARRIVED : TRIP_FINALIZED);

            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {

                Log.d(TAG_TEST, "onGeoQueryReady: GEOQUERY READY");

                // ADDING CIRCLE FOR DEMONSTRATION

                if (queryTripCircle != null)
                    queryTripCircle.remove();
                queryTripCircle = mMap.addCircle(circleOptions);

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                try {
                    throw error.toException();
                } catch (Exception e) {
                    throwToast(e.getMessage(), true);
                    e.printStackTrace();
                    Log.d(TAG, "onGeoQueryError: " + e.toString());
                }
            }
        };


    }

    /*
    private void cancelQueryEventListener() {

        if (geoQuery != null) {

            if (queryReqsCircle != null) {

                if (tripQueryEventListener != null) {
                    geoQuery.removeGeoQueryEventListener(tripQueryEventListener);
                    tripQueryEventListener = null;
                }

                queryReqsCircle.remove();
                queryReqsCircle = null;

            } else if (queryTripCircle!=null){

                if (reqsQueryEventListener != null) {

                    reqsQueryEventListener = null;
                }
                queryTripCircle.remove();
                queryTripCircle = null;
            }
        }
    }*/

    private GeoQueryEventListener getGeoQueryEventListeners(boolean isForTrip){ //(requests)

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!isForTrip) {

                    if (requestList.contains(key))
                        getTripsData(key);

                } else {
                    if (key.equals(loggedDriver.getId())) {
                        checkDriverEntered(key);
                    }

                    if (key.equals(currentTrip.getTripId()))
                        updateTrip();
                }
            }

            @Override
            public void onKeyExited(String key) {
                if (!isForTrip) {
                    if (key.equals(loggedDriver.getId())) {
                        restartReqsQuery();
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}
            @Override
            public void onGeoQueryReady() {
                queryReqsCircle.remove();
                //addCircles();
                setGeoQueryReqs(true);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                getErrors(error);
            }
        };
    }

    private void checkDriverEntered(String key) {
        Log.d(TAG_TEST, "checkDriverEntered: Driver "+key+" ENTERED");
        throwToast(key+" entered", false);
    }

    private void updateTrip() {
        updateTripStatus(checkStatus().equals(DRIVER_COMING) ?
                DRIVER_ARRIVED : TRIP_FINALIZED);
    }


    private void restartReqsQuery() {
        Log.d(TAG_TEST, "onGeoQueryReady: DRIVER LEFT");
        setGeoQueryReqs(true); // CANCELING PREVIOUS LISTENER
        if (buttonWork.isSelected())
            setGeoQueryReqs(false); // STARTING NEW
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

    private void  addCircles(){

        boolean isForTrip = currentTrip != null;
        CircleOptions circleOptions;

        // ADDING CIRCLE FOR DEMONSTRATION

        Log.d(TAG_TEST, "onGeoQueryReady: GEOQUERY READY");

        throwToast("GEOQUERY READY", false);

        if (isForTrip) {

            LatLng latLng = checkStatus().equals(DRIVER_COMING) ?
                    new LatLng(currentTrip.getStartLoc().getLatitude(),
                            currentTrip.getStartLoc().getLongitude())

                    : new LatLng(currentTrip.getDestination().getLatitude(),
                    currentTrip.getDestination().getLongitude());

            circleOptions = circleOptions(latLng, true);

            if (queryTripCircle != null) queryTripCircle.remove();
            queryTripCircle = mMap.addCircle(circleOptions);


        } else {

            if (queryReqsCircle != null) queryReqsCircle.remove();

            circleOptions = circleOptions
                    (new LatLng(geoQueryReqs.getCenter().latitude,
                            geoQueryReqs.getCenter().longitude), false);

            queryReqsCircle = mMap.addCircle(circleOptions);

        }
    }
/*
    class QueryTask extends AsyncTask<Void,Void, Void>{

        private GeoQuery mGeoQuery;
        private GeoQueryEventListener mGeoQueryEventListener;

        public QueryTask(GeoQuery mGeoQuery, GeoQueryEventListener mGeoQueryEventListener) {
            this.mGeoQuery = mGeoQuery;
            this.mGeoQueryEventListener = mGeoQueryEventListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            mGeoQuery.addGeoQueryEventListener(mGeoQueryEventListener);

            return null;
        }
    }*/


}