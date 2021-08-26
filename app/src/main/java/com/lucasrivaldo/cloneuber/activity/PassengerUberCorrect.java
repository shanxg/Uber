package com.lucasrivaldo.cloneuber.activity;

import android.Manifest;
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
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.adapter.AdapterAddresses;
import com.lucasrivaldo.cloneuber.api.TaskLoadedCallback;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.AlertDialogUtil;
import com.lucasrivaldo.cloneuber.helper.RecyclerItemClickListener;
import com.lucasrivaldo.cloneuber.helper.SystemPermissions;
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

import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_ARRIVED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.ON_THE_WAY;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.PASSENGER_ABOARD;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.START_TRIP;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.TRIP_FINALIZED;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_BLACK;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_SLCT;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.UBER_X;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCEv;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATIONv;

public class PassengerUberCorrect extends FragmentActivity










        implements OnMapReadyCallback, TaskLoadedCallback{

    public static final int START_LOC = 0;
    public static final int DESTINATION = 1;

    private static final String uType = ConfigurateFirebase.TYPE_PASSENGER;

    private String[] needPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private int VIEW_CODE, locationRequestCode;
    private String statusLoopHolder;
    private boolean isFirstDriverComingToast;

    private User loggedPassenger;
    private Trip myTrip;
    private List<UberAddress> addressList, newAddressList;
    private List<String> reqsList;


    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private int mapWidth, mapHeight;

    private Marker passengerMark, driverMark, destinationMark;
    private Circle queryReqsCircle, queryTripCircle;

    private HashMap<String, String> routeTextMap;
    private HashMap<String, Marker> driversLocMap;
    private Polyline currentPolyline;
    private LocationManager locationManager;

    private GeoQuery geoQuery;
    private GeoQueryEventListener tripQueryEventListener, reqsQueryEventListener;


    private DatabaseReference tripRef;
    private ValueEventListener tripListener;

    private Query reqsRefQuery;
    private ValueEventListener reqsListener;

    private AdapterAddresses adapterAddresses;

    private SearchView searchMyLocation, searchDestinyLocation;
    private ImageView btnMyLoc, btnAddNewLoc;
    private RecyclerView recyclerNewDest, recyclerAddress;
    private TextView textUberXValue, textUberSelectValue, textUberBlackValue,
            textUberDescriptionValue,textUberUserName, progressText, textDuration, textDistance;
    private LinearLayout btnUberX, btnUberSelect, btnUberBlack;
    private Button buttonSearchDriver;

    private LinearLayout includeSelectDriverTab, includeCallTab,
            selectTypeLayout, includeLoadingSearchDriver;
    private UberAddress myAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // START ACTIVITY AFTER LOGGED USER IS SET FOR NO NULL EXCEPTIONS
        startActivity(new Intent(this, PassengerTripActivity.class));

        /*
        preLoad();
        UserFirebase.getLoggedUserData(uType, valueEventListener(UserFirebase.GET_LOGGED_USER_DATA));*/

    }
    private void preLoad(){
        addressList = new ArrayList<>();
        newAddressList = new ArrayList<>();
        reqsList = new ArrayList<>();

        geoQuery = null;

        myTrip = new Trip();

        driversLocMap = new HashMap<>();
    }

    private void setContentView(){
        setContentView(R.layout.activity_uber_passenger);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("(Clone) Uber Passenger");
        setActionBar(toolbar);

        UserFirebase.setLastLogin(uType);
        throwToast(getResources().getString(R.string.text_logged_passenger), true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPassenger);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        loadInterface();
    }

    private void loadInterface(){

        textDuration = findViewById(R.id.textDuration);
        textDistance = findViewById(R.id.textDistance);

        includeLoadingSearchDriver = findViewById(R.id.includeLoadingSearchDriver);
        progressText = findViewById(R.id.progressText);

        includeSelectDriverTab = findViewById(R.id.includeSelectExperienceTab);
        selectTypeLayout = findViewById(R.id.selectTypeLayout);

        // ############################      SEARCH ADDRESS  TAB      #############################

        searchMyLocation = findViewById(R.id.searchMyLocation);
        searchDestinyLocation = findViewById(R.id.searchDestinyLocation);

        btnMyLoc = findViewById(R.id.btnMyLoc);
        btnAddNewLoc = findViewById(R.id.btnAddNewLoc);

        recyclerNewDest = findViewById(R.id.recyclerNewDest);
        recyclerAddress = findViewById(R.id.recyclerAddress);

        includeCallTab = findViewById(R.id.includeCallTab);

        // ###########################      SELECT TRAVEL TYPE TAB      ###########################

        textUberXValue = findViewById(R.id.textUberXValue);
        textUberSelectValue = findViewById(R.id.textUberSelectValue);
        textUberBlackValue = findViewById(R.id.textUberBlackValue);
        textUberDescriptionValue = findViewById(R.id.textUberDescriptionValue);
        textUberUserName = findViewById(R.id.textUberUserName);

        btnUberX = findViewById(R.id.btnUberX);
        btnUberSelect = findViewById(R.id.btnUberSelect);
        btnUberBlack = findViewById(R.id.btnUberBlack);

        buttonSearchDriver = findViewById(R.id.buttonSearchDriver);
        updateTripStatus(START_TRIP);

        setListeners();
    }

    /** ###############################      SET LISTENERS     ################################ **/

    private ValueEventListener valueEventListener(int requestCode){
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                switch (requestCode) {
                    case UserFirebase.GET_LOGGED_USER_DATA:
                        if (dataSnapshot.exists()) {
                            loggedPassenger = dataSnapshot.getValue(User.class);

                            if (loggedPassenger != null) {
                                setContentView();
                                myTrip.setPassenger(loggedPassenger);
                            }
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
                                    else if (checkStatus().equals(ON_THE_WAY))
                                        updateTripStatus(checkStatus());


                                    double driverLat = myTrip.getDriver().getLatitude();
                                    double driverLng = myTrip.getDriver().getLongitude();
                                    LatLng driverLoc = new LatLng(driverLat, driverLng);

                                    if (!driverLoc.equals(driverMark.getPosition())) {
                                        addTripMarker();
                                    }
                                    textUberUserName.setText(myTrip.getDriver().getName());
                                } else {
                                    if (driverMark!=null)
                                        driverMark.setVisible(false);
                                    if (!checkStatus().equals(AWAITING)
                                            || !checkStatus().equals(START_TRIP)) {
                                        startTrip(true);
                                        updateTripStatus(AWAITING);
                                    }
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

    private void setListeners(){
        View.OnFocusChangeListener focusListener  = (view, hasFocus) ->{
            if(hasFocus){
                toggleSelectDriverTab(false);
            }else if (myTrip.getDestination()!=null){
                toggleSelectDriverTab(true);
            }
        };

        searchMyLocation.setOnQueryTextFocusChangeListener(focusListener);
        searchDestinyLocation.setOnQueryTextFocusChangeListener(focusListener);

        // ########################      SET LOCATION QUERY LISTENER      #########################

        searchMyLocation.setOnQueryTextListener(queryTextListener(START_LOC));
        searchDestinyLocation.setOnQueryTextListener(queryTextListener(DESTINATION));

        // ###########################       SET OTHER LISTENERS      #############################

        updateTypeButtons(R.id.btnUberSelect);
        setTripType(UBER_SLCT);

        getUserLocation(START_LOC);
        setClickListeners();
    }

    private void setClickListeners() {

        View.OnClickListener clickListener = view -> {

            int itemId = view.getId();
            switch (itemId) {

                // ########################      SEARCH ADDRESS  TAB      #########################

                case R.id.btnMyLoc:

                    getUserLocation(START_LOC);
                    break;

                case R.id.btnAddNewLoc:

                    startNewAddress();
                    break;

                // #######################      SELECT TRAVEL TYPE TAB      #######################

                case R.id.btnUberX:

                    updateTypeButtons(itemId);
                    setTripType(UBER_X);
                    break;

                case R.id.btnUberSelect:

                    updateTypeButtons(itemId);
                    setTripType(UBER_SLCT);
                    break;

                case R.id.btnUberBlack:

                    updateTypeButtons(itemId);
                    setTripType(UBER_BLACK);
                    break;

                case R.id.buttonSearchDriver:
                    if (myTrip.getStartLoc() != null && myTrip.getDestination() != null)
                        if (checkStatus().equals(START_TRIP)) {
                            confirmTripAlertDialog();
                        }else if (checkStatus().equals(AWAITING) ||
                                checkStatus().equals(DRIVER_COMING)){
                            cancelTrip();
                        }else if (checkStatus().equals(DRIVER_ARRIVED))
                            updateTripStatus(PASSENGER_ABOARD);
                        else if (checkStatus().equals(TRIP_FINALIZED))
                            finalizeTrip();

                        else {
                            if (myTrip.getDestination() == null)
                                throwToast("Your destination is empty, "
                                        + "set it before you start your trip.", true);
                            else
                                throwToast("Your start location is empty, "
                                        + "set it before you start your trip.", true);
                        }
                    break;
            }
            if (itemId != R.id.includeCallTab ){
                addressList.clear();
                if(adapterAddresses!=null)
                    adapterAddresses.notifyDataSetChanged();
            }
        };

        includeSelectDriverTab.setOnClickListener(clickListener);


        // ############################      SEARCH ADDRESS  TAB      #############################

        btnMyLoc.setOnClickListener(clickListener);
        btnAddNewLoc.setOnClickListener(clickListener);

        // ###########################      SELECT TRAVEL TYPE TAB      ###########################

        btnUberX.setOnClickListener(clickListener);
        btnUberSelect.setOnClickListener(clickListener);
        btnUberBlack.setOnClickListener(clickListener);

        buttonSearchDriver.setOnClickListener(clickListener);
    }

    private LocationListener locationListener(int requestCode){
        return location -> {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (checkStatus().equals(START_TRIP) || checkStatus().equals(AWAITING)) {
                if (geoQuery == null)
                    setGeoLocationListener(latitude, longitude, false);

            }else if(checkStatus().equals(DRIVER_COMING)){
                locationManager.removeUpdates(locationListener(requestCode));
            }

            if(requestCode == START_LOC){

                if ( !new GeoLocation(loggedPassenger.getLatitude(), loggedPassenger.getLongitude())
                        .equals(new GeoLocation(latitude, longitude))){

                    loggedPassenger.setLatitude(latitude);
                    loggedPassenger.setLongitude(longitude);

                    LatLng passengerLoc = new LatLng(latitude, longitude);

                    List<UberAddress> myAddressList = UserFirebase.getAddress
                            (this, passengerLoc, UserFirebase.GET_CURRENT_USER_LOC);

                    if (myAddressList != null) {
                        myAddress = myAddressList.get(0);
                        searchMyLocation.setQueryHint(myAddress.getAddressLines());

                        if (reqsRefQuery == null) {
                            setRequestsListener(myAddress);
                        }

                        myTrip.setStartLoc(myAddress);
                        addTripMarker();
                    }
                }

            }else if (requestCode == DESTINATION){

                locationManager.removeUpdates(locationListener(requestCode));

                LatLng destinationLoc = new LatLng(latitude, longitude);

                List<UberAddress> myAddressList = UserFirebase.getAddress
                        (this, destinationLoc, UserFirebase.GET_CURRENT_USER_LOC);

                if(myAddressList!=null){
                    UberAddress address = myAddressList.get(0);
                    searchDestinyLocation.setQueryHint(address.getAddressLines());

                    myTrip.setDestination(address);
                    addTripMarker();
                }
            }
        };
    }

    private void setRequestsListener(UberAddress address) {

        reqsRefQuery =
                ConfigurateFirebase.getFireDBRef()
                        .child(ConfigurateFirebase.REQUEST)
                        .child(address.getCountryCode())
                        .child(address.getState());

        reqsListener = reqsRefQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reqsList.clear();
                for (DataSnapshot data: dataSnapshot.getChildren()){
                    String key = data.getKey();
                    reqsList.add(key);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private SearchView.OnQueryTextListener queryTextListener(int viewCode){

        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String address) {
                SearchView view = viewCode==START_LOC?
                        searchMyLocation : searchDestinyLocation;

                if(validateAddressText(address)) {

                    VIEW_CODE = viewCode;
                    startRecyclerAddress(address);

                    hideKeyboardFrom(view);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                SearchView view = viewCode==START_LOC?
                        searchMyLocation : searchDestinyLocation;

                boolean b = s.length()>0;

                if (!b){
                    if (addressList!=null)
                        addressList.clear();
                    if (adapterAddresses!=null)
                        adapterAddresses.notifyDataSetChanged();

                    hideKeyboardFrom(view);
                }
                return true;
            }
        };
    }

    private void setRecyclerAddressListener(){
        recyclerAddress.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerAddress,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                UberAddress address = addressList.get(position);

                                SearchView searchView = VIEW_CODE == START_LOC?
                                        searchMyLocation:searchDestinyLocation;

                                if (address.getAddressLines().equals(UberAddress.CURRENT_LOC)){
                                    getUserLocation(VIEW_CODE);
                                    searchView.setQuery("", false);

                                    if (myTrip.getStartLoc() != null
                                            && myTrip.getDestination() != null)
                                        toggleSelectDriverTab(true);

                                }else if (address.getAddressLines().equals(UberAddress.NO_LOC_FOUND)){
                                    searchView.setQuery("", false);
                                }else {

                                    if(VIEW_CODE == START_LOC) {
                                        loggedPassenger.setLatitude(address.getLatitude());
                                        loggedPassenger.setLongitude(address.getLongitude());
                                        myTrip.setPassenger(loggedPassenger);

                                        myTrip.setStartLoc(address);
                                        addTripMarker();
                                    }else {
                                        myTrip.setDestination(address);
                                        toggleSelectDriverTab(true);

                                        addTripMarker();
                                    }
                                    searchView.setQueryHint(address.getAddressLines());
                                    searchView.setQuery("", false);
                                }

                                addressList.clear();
                                if(adapterAddresses!=null)
                                    adapterAddresses.notifyDataSetChanged();
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {}

                            @Override
                            public void onItemClick
                                    (AdapterView<?> adapterView, View view, int i, long l) {}
                        }));
    }



    private void setTripListener(){
        tripRef = ConfigurateFirebase.getFireDBRef()
                .child(ConfigurateFirebase.TRIP)
                .child(myTrip.getTripId());

        tripListener = tripRef.addValueEventListener(valueEventListener(UserFirebase.GET_TRIP_DATA));
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
                    Log.d("USERTESTERROR", "completionListener: "+ e.getMessage());
                }
            }

        };
    }

    /** ################################      MY METHODS     ################################## **/

    private boolean validateAddressText(String addressText){
        if(addressText!=null && !addressText.isEmpty())
            return true;
        else {
            String errorMessage = getResources().getString(R.string.text_empty_address);
            throwToast(errorMessage, true);
            return false;
        }
    }

    private void startRecyclerAddress(String address) {

        addressList.clear();

        addressList = UserFirebase.getAddressFromName(this, address, VIEW_CODE);

        if (addressList !=null) {

            adapterAddresses = new AdapterAddresses(addressList);

            recyclerAddress.setLayoutManager(new LinearLayoutManager(this));
            recyclerAddress.setHasFixedSize(true);
            recyclerAddress.setAdapter(adapterAddresses);

            setRecyclerAddressListener();

            adapterAddresses.notifyDataSetChanged();
        }
    }



    private void updateTripStatus(String status){
        myTrip.setStatus(status);
        if(myTrip.update()){

            int gone = View.GONE;
            int visible = View.VISIBLE;

            switch (status) {
                case START_TRIP:

                    statusLoopHolder = START_TRIP;

                    updatingSelectDriverTab(true);
                    toggleTripDescriptionTab(false);
                    getActionBar().setDisplayHomeAsUpEnabled(true);

                    // CLOSES PROGRESS BAR SEARCH DRIVER
                    includeLoadingSearchDriver.setVisibility(gone);

                    // UPDATE STATUS
                    buttonSearchDriver.setText(status);

                    // STARTS REQUESTS CHECK LISTENER
                    if (myAddress!=null)
                        setRequestsListener(myAddress);

                    break;

                case AWAITING:

                    getActionBar().setDisplayHomeAsUpEnabled(true);

                    // OPEN PROGRESS BAR SEARCH DRIVER
                    progressText.setText(status);
                    includeLoadingSearchDriver.setVisibility(visible);

                    // CLOSES SELECT TRIP EXPERIENCE TAB
                    updatingSelectDriverTab(false);


                    // UPDATE GEOFIRE LOCATION
                    GeoLocation geoLocation =
                            new GeoLocation(myTrip.getStartLoc().getLatitude(),
                                    myTrip.getStartLoc().getLongitude());

                    ConfigurateFirebase.getGeoFire().setLocation
                            (myTrip.getTripId(), geoLocation, completionListener());

                    //UPDATE BUTTON TEXT
                    status = "CANCEL SEARCH";
                    buttonSearchDriver.setText(status);

                    // UPDATE VAR FOR NON-REPEATABLE TOAST
                    isFirstDriverComingToast = true;

                    break;

                case DRIVER_COMING:

                    if (isFirstDriverComingToast) {
                        isFirstDriverComingToast = false;
                        throwToast("A driver accepted your request.\n"
                                + DRIVER_COMING, false);
                    }

                    // REMOVE TRIP REQUEST
                    myTrip.request(false);

                    // ADD MARKER FOR DRIVER, CENTER DRIVER AND PASSENGER, UPDATE ROUTES
                    addTripMarker();

                    // REMOVE REQUESTS CHECK LISTENER
                    reqsRefQuery.removeEventListener(reqsListener);
                    reqsList.clear();

                    // ADDS QUERY AT START LOC TO NOTIFY WHEN DRIVER ARRIVES
                    if (statusLoopHolder.equals(START_TRIP)) {
                        cancelQueryEventListener();
                        setGeoLocationListener(myTrip.getStartLoc().getLatitude(),
                                myTrip.getStartLoc().getLongitude(),
                                true);

                        statusLoopHolder = DRIVER_COMING;
                    }

                    includeLoadingSearchDriver.setVisibility(gone);
                    getActionBar().setDisplayHomeAsUpEnabled(false);

                    // OPEN TRIP DESCRIPTION TAB
                    toggleTripDescriptionTab(true);
                    setTripTab();
                    btnMyLoc.setVisibility(gone);
                    searchMyLocation.setEnabled(false);


                    //UPDATE BUTTON TEXT
                    buttonSearchDriver.setText(getResources().getString(R.string.text_cancel_trip));

                    break;

                case DRIVER_ARRIVED:
                    includeLoadingSearchDriver.setVisibility(visible);

                    // NOTIFY PASSENGER WHEN DRIVER ARRIVES AT START LOCATION
                    throwToast(getResources()
                                    .getString(R.string.text_driver_arrived_toast),
                            false);

                    status = getResources().getString(R.string.text_confirm_aboard);

                    progressText.setText(status);
                    buttonSearchDriver.setText(status);

                    break;

                case PASSENGER_ABOARD:

                    buttonSearchDriver.setVisibility(gone);
                    includeLoadingSearchDriver.setVisibility(gone);

                    break;

                case ON_THE_WAY:

                    //CREATE GEOQUERY AT DESTINATION FOR TRIP FINALIZATION STATUS
                    if (statusLoopHolder.equals(DRIVER_COMING)) {
                        cancelQueryEventListener();
                        setGeoLocationListener(myTrip.getDestination().getLatitude(),
                                myTrip.getDestination().getLongitude(),
                                true);

                        statusLoopHolder = ON_THE_WAY;
                    }

                    break;

                case TRIP_FINALIZED:

                    throwToast(getResources().getString(R.string.text_trip_finalization), false);

                    buttonSearchDriver.setVisibility(visible);
                    buttonSearchDriver.setText(status);

                    // REMOVE PREVIOUS GEOQUERY EVENT LISTENER
                    cancelQueryEventListener();

                    break;
            }
        }else{
            throwToast("STATUS UPDATING ERROR",true);
        }
    }

    private void setTripTab() {

        CircleImageView civ_uberDescription = findViewById(R.id.civ_uberDescription);
        civ_uberDescription.setImageResource(UberHelper.returnImageResourceId(myTrip.getTripType()));

        TextView textTripDescriptionType = findViewById(R.id.textTripDescriptionType);
        textTripDescriptionType.setText
                (UberHelper.returnTypeName(myTrip.getTripType(),getResources()));
        textUberDescriptionValue.setText(myTrip.getValue());

        textUberUserName.setText(myTrip.getPassenger().getName());

        findViewById(R.id.myLocLayoutDescription).setEnabled(false);
        findViewById(R.id.myLocLayoutDescription).setFocusable(false);
        SearchView searchMyLocationDescription = findViewById(R.id.searchMyLocationDescription);
        searchMyLocationDescription.setQueryHint(myTrip.getStartLoc().getAddressLines());

        SearchView searchDestinyLocationDescription = findViewById(R.id.searchDestinyLocationDescription);
        searchDestinyLocationDescription.setQueryHint(myTrip.getDestination().getAddressLines());
        searchDestinyLocationDescription.setEnabled(false);
        searchDestinyLocationDescription.setFocusable(false);
    }

    private void finalizeTrip() {
        myTrip = new Trip();
        cancelTripListener();

        startActivity(new Intent(this, UberPassengerActivity.class));
        finish();
    }

    private void getUserLocation(int requestCode) {

        locationRequestCode = requestCode;

        // CHECKING FOR PERMISSIONS REQUIRED IF WERE GRANTED, IF ANYTHING WENT WRONG ON VALIDATION

        if (checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            permissionValidationAlert();
            return;
        }

            /*
            WHAT WE NEED TO GET USER CURRENT LOCATION
                1 - LOCATION PROVIDER
                2 - MINIMUM TIME BETWEEN LOCATION UPDATES (in milliseconds)
                3 - MINIMUM DISTANCE BETWEEN LOC. UPDATES (in meters)
                4 - LOCATION LISTENER
            */

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0,
                locationListener(requestCode));
    }

    private void addTripMarker(){

        // CREATE MARKER ON MAP FOR PASSENGER LOCATION
        if (myTrip.getStartLoc() != null) {

            LatLng passengerLoc = new LatLng
                    (myTrip.getStartLoc().getLatitude(), myTrip.getStartLoc().getLongitude());

            if (passengerMark != null) {
                passengerMark.setPosition(passengerLoc);

            }else {
                passengerMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(passengerLoc)
                                .title(loggedPassenger.getName())
                                .snippet(myTrip.getStartLoc().getAddressLines())
                                .icon(bitmapDescriptorFromVector
                                        (R.drawable.ic_person_pin_circle_black_24dp)));
            }

            if (checkStatus().equals(START_TRIP) && myTrip.getDestination() == null)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLoc, 15));
        }

        // CREATE MARKER ON MAP FOR DRIVER LOCATION
        if (myTrip.getDriver() != null) {

            LatLng driverLoc = new LatLng
                    (myTrip.getDriver().getLatitude(), myTrip.getDriver().getLongitude());

            if (driverMark != null) {

                double angle = SphericalUtil.computeHeading(driverMark.getPosition(), driverLoc);

                driverMark.setIcon(BitmapDescriptorFactory.fromBitmap(rotateDriverIconBitmap(angle)));
                driverMark.setVisible(true);
                driverMark.setPosition(driverLoc);

            }else {
                driverMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(driverLoc)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_carr)));
            }

            if (checkStatus().equals(DRIVER_ARRIVED))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLoc, 18));
        }

        // CREATE MARKER ON MAP FOR DESTINATION LOCATION
        if (myTrip.getDestination() != null) {

            LatLng destinationLoc = new LatLng
                    (myTrip.getDestination().getLatitude(), myTrip.getDestination().getLongitude());

            if (destinationMark != null) {
                destinationMark.setPosition(destinationLoc);

            }else {
                destinationMark = mMap.addMarker(
                        new MarkerOptions()
                                .position(destinationLoc)
                                .title("Destination")
                                .snippet(myTrip.getDestination().getAddressLines())
                                .icon(bitmapDescriptorFromVector
                                        (R.drawable.ic_pin_drop_black_24dp)));
            }

            if (checkStatus().equals(TRIP_FINALIZED))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLoc, 18));
        }

        // CHECK STATUS OF MY TRIP TO CENTER MARKERS
        if (checkStatus().equals(START_TRIP))
            if (myTrip.getDestination() != null) {
                centerMarkers(true);
                toggleLayoutSearch(true);
            }
            else
                centerMarkers(false);
    }

    private void centerMarkers(boolean searchingDest){

        LatLngBounds.Builder centerBuilder = new LatLngBounds.Builder();

        /* METRICS  ARE UPDATED ON METHOD TOGGLE DRIVER TAB
            FOR MAP SIZE TO BE IN ACCORDANCE WITH DRIVER TAB */

        int padding = (int) (getResources().getDisplayMetrics().widthPixels*0.30);

        if (searchingDest) {

            LatLng startLoc = passengerMark.getPosition();
            LatLng destination = destinationMark.getPosition();

            //  DRAWING ROUTES
            getDirections(startLoc, destination);

            //  SETTING CAMERA CENTRALIZATION BUILDER
            centerBuilder.include(startLoc);
            centerBuilder.include(destination);

            LatLngBounds bounds = centerBuilder.build();

            //  SETTING CAMERA
            if (currentPolyline==null)
                mMap.moveCamera
                        (CameraUpdateFactory.newLatLngBounds(bounds, mapWidth, mapHeight, padding));

        }else {

            if (checkStatus().equals(AWAITING) || checkStatus().equals(DRIVER_COMING)) {


                if (driverMark!=null) {
                    LatLng driverLoc = driverMark.getPosition();
                    LatLng passenger = passengerMark.getPosition();

                    //  DRAWING ROUTES
                    getDirections(driverLoc, passenger);
                }


            } else if (checkStatus().equals(ON_THE_WAY)) {


                LatLng driverLoc = driverMark.getPosition();
                LatLng destinationLoc = destinationMark.getPosition();

                //  DRAWING ROUTES
                getDirections(driverLoc, destinationLoc);
            }
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

                calculateTripValues();
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

    private void startTrip(boolean restartRequest) {

        if (locationRequestCode ==START_LOC)
            getUserLocation(START_LOC);

        calculateTripValues();

        if (restartRequest){
            myTrip.request(true);
        }else {
            try {
                if (myTrip.save()) {

                    updateTripStatus(AWAITING);
                    setTripListener();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throwToast(e.getMessage(), true);
            }
        }
    }

    private void cancelTrip(){
        myTrip.setValue(null);

        if (checkStatus().equals(AWAITING)||
                checkStatus().equals(DRIVER_COMING)) {

            try {
                if (myTrip.cancel()) {
                    throwToast("SEARCH CANCELLED", false);

                    ConfigurateFirebase.getGeoFire().removeLocation(myTrip.getTripId(),
                            (key, error) -> {
                                if (error == null) {

                                    myTrip.setTripId(null);

                                    updateTripStatus(START_TRIP);
                                    cancelTripListener();
                                } else {
                                    throwToast("ERROR UPDATING GEOFIRE LOCATION: \n"
                                            +error.getMessage(), true);

                                    try { throw error.toException(); }
                                    catch (Exception e) {

                                        e.printStackTrace();
                                        Log.d("USERTESTERROR",
                                                "onComplete: " + e.getMessage());
                                    }
                                }
                            });
                }
            }catch (Exception e){
                e.printStackTrace();
                throwToast(e.getMessage(), true);
            }
        }else {

            myTrip.setDestination(null);

            searchDestinyLocation.requestFocus();
            searchDestinyLocation.setQueryHint(getResources().getString(R.string.text_destination));
            searchDestinyLocation.setQuery("", false);
            searchDestinyLocation.clearFocus();

            if (currentPolyline != null) {
                currentPolyline.remove();
                destinationMark.remove();
            }
            addTripMarker();

            toggleLayoutSearch(false);

            toggleDurationTab(false);
            toggleDistanceTab(false);

        }
    }

    private void calculateTripValues() {

        // UPDATE TEXTS
        String distanceText = routeTextMap.get(DISTANCE);
        String durationText = routeTextMap.get(DURATION);


        if (myTrip.getValue() == null) {
            // DISTANCE VALUE IS MEASURED IN METERS
            int distanceValue = Integer.parseInt(routeTextMap.get(DISTANCEv));

            // DURATION VALUE IS MEASURED IN SECONDS
            int durationValue = Integer.parseInt(routeTextMap.get(DURATIONv));

            // SETTING CURRENT TRIP DISTANCE AND DURATION
            myTrip.setDistance(distanceValue);
            myTrip.setDistanceText(routeTextMap.get(DISTANCE));

            myTrip.setDuration(durationValue);
            myTrip.setDurationText(routeTextMap.get(DURATION));

            myTrip.makeValue(myTrip.getTripType());

            textUberDescriptionValue.setText(myTrip.getValue());

            TextView textType = findViewById(R.id.textTripDescriptionType);
            textType.setText(UberHelper.returnTypeName(myTrip.getTripType(), getResources()));
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

            textDistance.setText(distanceText1);
            textDuration.setText(durationText);

            if (checkStatus().equals(DRIVER_COMING)
                    || checkStatus().equals(DRIVER_ARRIVED)) {
                toggleDistanceTab(false);
                toggleDurationTab(false);
            }else {
                toggleDistanceTab(true);
                toggleDurationTab(true);
            }

        }


        if (checkStatus().equals(START_TRIP) || checkStatus().equals(AWAITING)) {

            textUberXValue.setText      ("R$ " + myTrip.calculate(UBER_X));
            textUberSelectValue.setText ("R$ " + myTrip.calculate(UBER_SLCT));
            textUberBlackValue.setText  ("R$ " + myTrip.calculate(UBER_BLACK));
        }
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

    private void startNewAddress(){
        //TODO ADD NEW SEARCH EDIT TEXT
    }

    private void hideKeyboardFrom(SearchView view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }

    private BitmapDescriptor bitmapDescriptorFromVector
            (@DrawableRes int vectorDrawableResourceId) {

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

    private void confirmTripAlertDialog(){

        String startLoc = "\nSTART LOCATION: \n"
                + myTrip.getStartLoc().getAddress()+", "
                + myTrip.getStartLoc().getAddressNum()+", "
                + myTrip.getStartLoc().getSubLocality()+", "
                + myTrip.getStartLoc().getCity() +" - "
                + myTrip.getStartLoc().getState()+"/"
                + myTrip.getStartLoc().getCountryCode()+"\n\n";

        String destination = "DESTINATION: \n"
                + myTrip.getDestination().getAddress()+", "
                + myTrip.getDestination().getAddressNum()+", "
                + myTrip.getDestination().getSubLocality()+", "
                + myTrip.getDestination().getCity() +" - "
                + myTrip.getDestination().getState()+"/"
                + myTrip.getDestination().getCountryCode()+"\n\n";

        String value = "R$ "+myTrip.calculate(myTrip.getTripType());

        String tripText = UberHelper.returnTypeName(myTrip.getTripType(), getResources()).toUpperCase()+"\n"
                + startLoc
                + destination
                + value;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip confirmation:");
        builder.setMessage(tripText);
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm",
                (dialog, which) -> startTrip(false));
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {});

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void permissionValidationAlert(){

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

    private void updateMapMetrics(){
        mapWidth = mapFragment.getResources().getDisplayMetrics().widthPixels;
        mapHeight = mapFragment.getResources().getDisplayMetrics().heightPixels;
    }

    private void toggleSelectDriverTab(boolean isOpening){

        int visibility = View.VISIBLE;
        if (!isOpening)
            visibility = View.GONE;

        includeSelectDriverTab.setVisibility(visibility);

        // UPDATE METRICS FOR MAP ACCORDINGLY WITH THE DRIVER TAB
        updateMapMetrics();
    }

    private void toggleTripDescriptionTab(boolean isOpening){

        int visibility = View.VISIBLE;
        if (!isOpening)
            visibility = View.GONE;



        // UPDATE METRICS FOR MAP ACCORDINGLY WITH THE DRIVER TAB
        updateMapMetrics();
    }

    private void toggleDistanceTab(boolean isOpening){

        int visibility = View.VISIBLE;
        if (!isOpening)
            visibility = View.GONE;

        findViewById(R.id.layout_distance).setVisibility(visibility);
    }


    private void toggleDurationTab(boolean isOpening){

        int visibility = View.VISIBLE;
        if (!isOpening)
            visibility = View.GONE;

        findViewById(R.id.layout_duration).setVisibility(visibility);
    }

    private void toggleLayoutSearch(boolean closing){

        int visibility;

        if (closing) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            visibility = View.GONE;
        }else {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            visibility = View.VISIBLE;
        }

        includeCallTab.setVisibility(visibility);
    }

    private void updatingSelectDriverTab(boolean makeVisible){
        int visibility = View.GONE;
        if (makeVisible)
            visibility = View.VISIBLE;

        selectTypeLayout.setVisibility(visibility);
    }

    private void throwToast(String message, boolean isLong){
        int  lenght = Toast.LENGTH_LONG;
        if(!isLong)
            lenght = Toast.LENGTH_SHORT;

        Toast.makeText
                (this, message, lenght).show();
    }

    private void updateTypeButtons(int id){

        CircleImageView civ_uberX = findViewById(R.id.civ_uberX);
        CircleImageView civ_uberSelect = findViewById(R.id.civ_uberSelect);
        CircleImageView civ_uberBlack = findViewById(R.id.civ_uberBlack);
        CircleImageView civ_uberDescription = findViewById(R.id.civ_uberDescription);

        int selectedColor = getResources().getColor(R.color.btnSignIn);
        int defaultColor = getResources().getColor(android.R.color.black);

        switch (id){
            case R.id.btnUberX:

                civ_uberX.setBorderColor(selectedColor);
                civ_uberDescription.setImageResource(R.drawable.uberx);

                civ_uberSelect.setBorderColor(defaultColor);
                civ_uberBlack.setBorderColor(defaultColor);
                break;

            case R.id.btnUberSelect:

                civ_uberSelect.setBorderColor(selectedColor);
                civ_uberDescription.setImageResource(R.drawable.uber_select);

                civ_uberX.setBorderColor(defaultColor);
                civ_uberBlack.setBorderColor(defaultColor);
                break;

            case R.id.btnUberBlack:

                civ_uberBlack.setBorderColor(selectedColor);
                civ_uberDescription.setImageResource(R.drawable.uber_black);

                civ_uberX.setBorderColor(defaultColor);
                civ_uberSelect.setBorderColor(defaultColor);
                break;
        }
    }

    private void setTripType(double tripType){
        myTrip.setTripType(tripType);
    }

    private String checkStatus(){
        return myTrip.getStatus();
    }

    private void cancelTripListener() {
        if (tripRef!=null && tripListener!=null){
            tripRef.removeEventListener(tripListener);
        }
    }



    private GeoQueryEventListener getGeoQueryEventListeners
            (CircleOptions circleOptions, boolean isForTrip) {

        return new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                throwToast(key + " ENTERED", false);

                if (isForTrip) {
                    if ( key.equals(myTrip.getTripId() )) {
                        if (checkStatus().equals(ON_THE_WAY))
                            updateTripStatus(TRIP_FINALIZED);
                        else if (checkStatus().equals(DRIVER_COMING))
                            updateTripStatus(DRIVER_ARRIVED);
                    }
                } else {
                    if ( !reqsList.contains(key) ) {
                        LatLng loc = new LatLng(location.latitude, location.longitude);

                        Marker marker =
                                mMap.addMarker(
                                        new MarkerOptions()
                                                .position(loc)
                                                .icon(BitmapDescriptorFactory
                                                        .fromResource
                                                                (R.drawable.icons_carr))
                                                .visible(true));

                        driversLocMap.put(key, marker);
                    }
                }

            }

            @Override
            public void onKeyExited(String key) {

                throwToast(key + " EXITED", false);

                if (!reqsList.contains(key)) {
                    if (!isForTrip)
                        if (driversLocMap.containsKey(key)) {

                            Marker marker = driversLocMap.get(key);

                            marker.remove();
                            driversLocMap.remove(key);
                        }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

                throwToast(key + " MOVED", false);

                if (!isForTrip)
                    if (!reqsList.contains(key))
                        if (driversLocMap.containsKey(key)) {
                            Marker marker = driversLocMap.get(key);

                            LatLng newLatLng =
                                    new LatLng(location.latitude, location.longitude);

                            double angle =
                                    SphericalUtil.computeHeading(marker.getPosition(), newLatLng);

                            marker.setIcon(BitmapDescriptorFactory
                                    .fromBitmap(rotateDriverIconBitmap(angle)));

                            marker.setVisible(true);
                            marker.setPosition(newLatLng);

                            driversLocMap.put(key, marker);

                        } else {
                            LatLng loc = new LatLng(location.latitude, location.longitude);


                            Marker marker =
                                    mMap.addMarker(
                                            new MarkerOptions()
                                                    .position(loc)
                                                    .visible(false));

                            driversLocMap.put(key, marker);
                        }

            }

            @Override
            public void onGeoQueryReady() {
                if (isForTrip) {
                    if (queryTripCircle != null)
                        queryTripCircle.remove();
                    queryTripCircle = mMap.addCircle(circleOptions);
                }else {
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
                    geoQuery.removeGeoQueryEventListener(reqsQueryEventListener);
                    reqsQueryEventListener = null;
                }
                queryTripCircle.remove();
                queryTripCircle = null;
            }
        }
    }

    private void setGeoLocationListener(double latitude, double longitude, boolean isForTrip) {

        geoQuery = ConfigurateFirebase.getGeoFire()
                .queryAtLocation(
                        new GeoLocation(latitude, longitude),
                        //RADIUS MEASURED IN KM (20m & 500m, RESPECTIVELY)
                        isForTrip ? 0.02 : 0.5);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(new LatLng(latitude, longitude));
        circleOptions.radius(isForTrip ? 50 : 500);  // MEASURED IN METERS
        circleOptions.strokeWidth(1);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(isForTrip ?
                Color.argb(77, 0, 0, 255)
                : Color.argb(77, 0, 255, 0));

        if (isForTrip){
            if (tripQueryEventListener == null) {
                tripQueryEventListener = getGeoQueryEventListeners(circleOptions, true);
                geoQuery.addGeoQueryEventListener(tripQueryEventListener);
            }

        }else {
            if (reqsQueryEventListener == null) {
                reqsQueryEventListener = getGeoQueryEventListeners(circleOptions, false);
                geoQuery.addGeoQueryEventListener(reqsQueryEventListener);
            }
        }

    }



}
    /*
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm =
                    (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        view.clearFocus();
    }
    */









