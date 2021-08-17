package com.lucasrivaldo.cloneuber.helper;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;


import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.lucasrivaldo.cloneuber.activity.UberPassengerActivity;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.model.UberAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class UserFirebase {

    public static final int GET_LOGGED_USER_DATA = -1;
    public static final int GET_USER_DATA = 0;
    public static final int GET_TRIP_DATA = 1;
    public static final int GET_TRIPS_DATA = 10;

    public static final int GET_CURRENT_USER_LOC = 2;

    public static boolean signOut(){

        ConfigurateFirebase.getFirebaseAuth().signOut();

        return getMyUser() == null;
    }

    public static String getCurrentUserID() {

        return getMyUser().getUid();
    }

    public static FirebaseUser getMyUser() {

        return ConfigurateFirebase.getFirebaseAuth().getCurrentUser();
    }

    public static boolean updateUserProfImage(Uri profileImage) {

        try {
            FirebaseUser user = getMyUser();

            if(user!=null) {

                UserProfileChangeRequest userChangeRequest =
                        new UserProfileChangeRequest.Builder()
                                .setPhotoUri(profileImage)
                                .build();


                user.updateProfile(userChangeRequest).addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Log.i("USER SAVE ERROR", "Error updating profile image at firebase user. \n" + task.getException().getMessage());
                    }
                });
                return true;
            }
            return false;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        }
    }

    public static boolean updateUserProfName(String profileName) {

        try {
            FirebaseUser user = getMyUser();

            UserProfileChangeRequest userChangeRequest =
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(profileName)
                            .build();


            user.updateProfile(userChangeRequest).addOnCompleteListener(task -> {

                if (!task.isSuccessful()) {
                    Log.i("USER SAVE ERROR", "Error updating profile name at firebase user. \n" + task.getException().getMessage());
                }
            });
            return true;

        } catch (Exception e) {

            e.printStackTrace();
            return false;

        }
    }



    public static void getLoggedUserData(String uType, ValueEventListener valueEventListener){
        DatabaseReference userRef = ConfigurateFirebase.getFireDBRef()
                                            .child(ConfigurateFirebase.USERS)
                                                .child(uType)
                                                    .child(getCurrentUserID());

        userRef.addListenerForSingleValueEvent(valueEventListener);
    }



    public static void getLastLogin(ValueEventListener loginListener){
        DatabaseReference lastLoginRef = ConfigurateFirebase.getFireDBRef()
                                            .child(ConfigurateFirebase.LAST_LOGIN);

        DatabaseReference userRef = lastLoginRef.child(getCurrentUserID());

        userRef.child("loginType").addListenerForSingleValueEvent(loginListener);
    }

    public static void setLastLogin(String uType){

        DatabaseReference lastLoginRef = ConfigurateFirebase.getFireDBRef()
                                            .child(ConfigurateFirebase.LAST_LOGIN);

        DatabaseReference userRef = lastLoginRef.child(getCurrentUserID());

        userRef.child("loginType").setValue(uType);
    }


    public static List<UberAddress> getAddress(Context context, LatLng loc, int requestCode){
        List<UberAddress> addressList = new ArrayList<>();

        try {
            Geocoder addressCoder = new Geocoder(context, Locale.getDefault());

            List<Address> addressesDecodedFromLocation;
            if(requestCode==GET_CURRENT_USER_LOC) {
                addressesDecodedFromLocation =
                        addressCoder.getFromLocation(loc.latitude, loc.longitude, 1);
            }else {
                addressesDecodedFromLocation =
                        addressCoder.getFromLocation(loc.latitude, loc.longitude, 10);
            }

            for (Address geocoderAddress : addressesDecodedFromLocation ){
                        /*
                        ADDRESS CLASS DATA - (addressCoderFromLocation) :
                            addressLines=addressLines,
                            SubThoroughfare=addressNum,
                            SubLocality=bairro
                            admin=state,
                            sub-admin=city,
                            thoroughfare=address,
                            postalCode=postalCode,
                            countryCode=countryCode,
                            countryName=countryName,
                            latitude=latitude,
                            longitude=longitude
                         */

                UberAddress address = new UberAddress();

                address.setAddressLines(geocoderAddress.getAddressLine(0));
                address.setSubLocality(geocoderAddress.getSubLocality());
                address.setState(geocoderAddress.getAdminArea());
                address.setCity(geocoderAddress.getSubAdminArea());
                address.setAddress(geocoderAddress.getThoroughfare());
                address.setPostalCode(geocoderAddress.getPostalCode());
                address.setCountryCode(geocoderAddress.getCountryCode());
                address.setCountryName(geocoderAddress.getCountryName());
                address.setLatitude(geocoderAddress.getLatitude());
                address.setLongitude(geocoderAddress.getLongitude());

                if (geocoderAddress.getSubThoroughfare()!=null)
                    address.setAddressNum(geocoderAddress.getSubThoroughfare());
                else
                    address.setAddressNum("0");

                addressList.add(address);
            }
            return addressList;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<UberAddress> getAddressFromName
            (Context context, String locationName, int viewCode){

        List<UberAddress> addressList = new ArrayList<>();

        try {
            Geocoder addressCoder = new Geocoder(context, Locale.getDefault());

            List<Address> addressesDecodedFromLocationName =
                        addressCoder.getFromLocationName(locationName, 10);

            if (viewCode == UberPassengerActivity.DESTINATION) {
                UberAddress currentLocation = new UberAddress();
                currentLocation.setAddressLines(UberAddress.CURRENT_LOC);
                addressList.add(currentLocation);
            }

            if(addressesDecodedFromLocationName == null
                    || addressesDecodedFromLocationName.size() == 0){
                UberAddress noAddress = new UberAddress();
                noAddress.setAddressLines(UberAddress.NO_LOC_FOUND);
                addressList.add(noAddress);
            }

            for (Address geocoderAddress : addressesDecodedFromLocationName ){

                UberAddress address = new UberAddress();

                address.setAddressLines(geocoderAddress.getAddressLine(0));
                address.setSubLocality(geocoderAddress.getSubLocality());
                address.setState(geocoderAddress.getAdminArea());
                address.setCity(geocoderAddress.getSubAdminArea());
                address.setAddress(geocoderAddress.getThoroughfare());
                address.setPostalCode(geocoderAddress.getPostalCode());
                address.setCountryCode(geocoderAddress.getCountryCode());
                address.setCountryName(geocoderAddress.getCountryName());
                address.setLatitude(geocoderAddress.getLatitude());
                address.setLongitude(geocoderAddress.getLongitude());

                if (geocoderAddress.getSubThoroughfare()!=null)
                    address.setAddressNum(geocoderAddress.getSubThoroughfare());
                else
                    address.setAddressNum("0");

                addressList.add(address);
            }


            return addressList;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
