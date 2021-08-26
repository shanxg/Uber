package com.lucasrivaldo.cloneuber.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.adapter.AdapterAddresses;
import com.lucasrivaldo.cloneuber.helper.AlertDialogUtil;
import com.lucasrivaldo.cloneuber.helper.RecyclerItemClickListener;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.helper.UserFirebase;
import com.lucasrivaldo.cloneuber.model.UberAddress;

import java.util.List;

import static com.lucasrivaldo.cloneuber.helper.UberHelper.AWAITING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.DRIVER_COMING;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.START_TRIP;
import static com.lucasrivaldo.cloneuber.helper.UberHelper.neededPermissions;

public class AddExtraDestActivity {

    public static final int START_LOC = 0;
    public static final int DESTINATION = 1;

    private int mVIEW_CODE, mLocationRequestCode;
    private LocationManager mLocationManager;



    /** ##############################     ACTIVITY PROCESSES    ############################## **/
/*
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


     /** #####################################  TO CUT  ######################################## **/
/*


    // TODO TO CUT

    private SearchView.OnQueryTextListener queryTextListener(int viewCode){

        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String address) {
                SearchView view = viewCode==START_LOC?
                        mSearchMyLocation : mSearchDestinyLocation;

                if(validateAddressText(address)) {

                    mVIEW_CODE = viewCode;
                    startRecyclerAddress(address);

                    UberHelper.hideKeyboardFrom(view, PassengerTripActivity.this);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                SearchView view = viewCode==START_LOC?
                        mSearchMyLocation : mSearchDestinyLocation;

                boolean b = s.length()>0;

                if (!b){
                    if (mAddressList !=null)
                        mAddressList.clear();
                    if (mAdapterAddresses !=null)
                        mAdapterAddresses.notifyDataSetChanged();

                    UberHelper.hideKeyboardFrom(view, PassengerTripActivity.this);
                }
                return true;
            }
        };
    }

    private boolean validateAddressText(String addressText){
        if(addressText!=null && !addressText.isEmpty())
            return true;
        else {
            String errorMessage = getResources().getString(R.string.text_empty_address);
            throwToast(errorMessage, true);
            return false;
        }
    }

    private void getUserLocation(int requestCode) {

        mLocationRequestCode = requestCode;

        // CHECKING FOR PERMISSIONS REQUIRED IF WERE GRANTED, IF ANYTHING WENT WRONG ON VALIDATION

        if (checkSelfPermission
                (Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            AlertDialogUtil.permissionValidationAlert(neededPermissions, this);
            return;
        }

            /*
            WHAT WE NEED TO GET USER CURRENT LOCATION
                1 - LOCATION PROVIDER
                2 - MINIMUM TIME BETWEEN LOCATION UPDATES (in milliseconds)
                3 - MINIMUM DISTANCE BETWEEN LOC. UPDATES (in meters)
                4 - LOCATION LISTENER
            */
/*
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, // 1 - LOCATION PROVIDER
                1000,              // 2 - MIN. TIME BETWEEN LOC. UPDATES (in milliseconds)
                1,               // 3 - MIN. DISTANCE BETWEEN LOC. UPDATES (in meters)
                locationListener(requestCode)); // 4 - LOCATION LISTENER
    }


    private LocationListener locationListener(int requestCode){
        return location -> {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (checkStatus().equals(START_TRIP) || checkStatus().equals(AWAITING)) {
                // TODO setGeoLocationListener(latitude, longitude, false);

            }else if(checkStatus().equals(DRIVER_COMING)){
                mLocationManager.removeUpdates(locationListener(requestCode));
            }

            if(requestCode == START_LOC){

                if ( !new GeoLocation(mLoggedPassenger.getLatitude(), mLoggedPassenger.getLongitude())
                        .equals(new GeoLocation(latitude, longitude))){

                    mLoggedPassenger.setLatitude(latitude);
                    mLoggedPassenger.setLongitude(longitude);

                    LatLng passengerLoc = new LatLng(latitude, longitude);

                    List<UberAddress> myAddressList = UserFirebase.getAddress
                            (this, passengerLoc, UserFirebase.GET_CURRENT_USER_LOC);

                    if (myAddressList != null) {
                        myAddress = myAddressList.get(0);


                        addTripMarker();
                    }
                }

            }else if (requestCode == DESTINATION){

                mLocationManager.removeUpdates(locationListener(requestCode));

                LatLng destinationLoc = new LatLng(latitude, longitude);

                List<UberAddress> myAddressList = UserFirebase.getAddress
                        (this, destinationLoc, UserFirebase.GET_CURRENT_USER_LOC);

                if(myAddressList!=null){
                    UberAddress address = myAddressList.get(0);
                    mSearchDestinyLocation.setQueryHint(address.getAddressLines());

                    myTrip.setDestination(address);
                    addTripMarker();
                }
            }
        };
    }

    private void startRecyclerAddress(String address) {

        mAddressList.clear();

        mAddressList = UserFirebase.getAddressFromName(this, address, mVIEW_CODE);

        if (mAddressList !=null) {

            mAdapterAddresses = new AdapterAddresses(mAddressList);

            mRecyclerAddress.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerAddress.setHasFixedSize(true);
            mRecyclerAddress.setAdapter(mAdapterAddresses);

            setRecyclerAddressListener();

            mAdapterAddresses.notifyDataSetChanged();
        }
    }

    private void setRecyclerAddressListener(){
        mRecyclerAddress.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerAddress,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                UberAddress address = mAddressList.get(position);

                                SearchView searchView = mVIEW_CODE == START_LOC?
                                        mSearchMyLocation : mSearchDestinyLocation;

                                if (address.getAddressLines().equals(UberAddress.CURRENT_LOC)){
                                    getUserLocation(mVIEW_CODE);
                                    searchView.setQuery("", false);

                                }else if (address.getAddressLines().equals(UberAddress.NO_LOC_FOUND)){
                                    searchView.setQuery("", false);
                                }else {

                                    if(mVIEW_CODE == START_LOC) {
                                        mLoggedPassenger.setLatitude(address.getLatitude());
                                        mLoggedPassenger.setLongitude(address.getLongitude());
                                        myTrip.setPassenger(mLoggedPassenger);

                                        myTrip.setStartLoc(address);
                                        addTripMarker();
                                    }else {
                                        myTrip.setDestination(address);


                                        addTripMarker();
                                    }
                                    searchView.setQueryHint(address.getAddressLines());
                                    searchView.setQuery("", false);
                                }

                                mAddressList.clear();
                                if(mAdapterAddresses !=null)
                                    mAdapterAddresses.notifyDataSetChanged();
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {}

                            @Override
                            public void onItemClick
                                    (AdapterView<?> adapterView, View view, int i, long l) {}
                        }));
    }




     */


}
