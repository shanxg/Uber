package com.lucasrivaldo.cloneuber.activity;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lucasrivaldo.cloneuber.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.lucasrivaldo.cloneuber.view_model.MapViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lucasrivaldo.cloneuber.activity.MainActivity.MY_TAG_ERROR;
import static com.lucasrivaldo.cloneuber.activity.MainActivity.MY_TAG_TEST;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;
import static com.lucasrivaldo.cloneuber.model.Trip.AWAITING;
import static com.lucasrivaldo.cloneuber.model.Trip.DRIVER_ARRIVED;
import static com.lucasrivaldo.cloneuber.model.Trip.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.model.Trip.ON_THE_WAY;
import static com.lucasrivaldo.cloneuber.model.Trip.PASSENGER_ABOARD;
import static com.lucasrivaldo.cloneuber.model.Trip.TRIP_FINALIZED;

public class DriverTripActivity extends FragmentActivity
        implements OnMapReadyCallback {
    private static final String uType = ConfigurateFirebase.TYPE_DRIVER;

    private String[] needPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private GoogleMap mMap;
    //private MapViewModel mMapViewModel;

    private User loggedDriver;

    private UberAddress myAddress;
    private List<String> requestList;
    private List<Trip> tripList;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView buttonWork;
    private ImageView imgStop;
    private ConstraintLayout workLayout;
    private LinearLayout includeProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        preLoad();
        // STARTS ACTICITY ONLY IF USER DATA NOT NULL
        UserFirebase.getLoggedUserData // RETURNS A SINGLE DATA VALUE( NOT LISTENER )
                (uType, valueEventListener(UserFirebase.GET_LOGGED_USER_DATA));


    }

    private void preLoad() {

        requestList = new ArrayList<>();
        tripList = new ArrayList<>();

        //reqsQueryEventListener = null;

        // MVVM (MODEL VIEW VIEW-MODEL)
        /*
        mMapViewModel = ViewModelProviders.of(this).get(MapViewModel.class);

        mMapViewModel.getMap().observe
                (this, googleMap ->{
                    mMap = googleMap;
                });
        */
    }

    private void setContentView() {
        //setContentView(R.layout.activity_driver_trip);
        setContentView(R.layout.activity_uber_driver_correction);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("(Clone) Uber Driver");
        setActionBar(toolbar);

        UserFirebase.setLastLogin(uType);
        SystemPermissions.validatePermissions(needPermissions, this, 1);

        loadInterface();


        throwToast("You logged as a driver successfully.", true);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void loadInterface() {
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

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
                permisionValidationAlert();
            } else if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                setLocationManager();
            }
        }

    }

    /** #################################        HELPERS       ################################ **/

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



    private void throwToast(String message, boolean isLong){
        int  lenght = Toast.LENGTH_LONG;
        if(!isLong)
            lenght = Toast.LENGTH_SHORT;

        Toast.makeText
                (this, message, lenght).show();

    }

}





