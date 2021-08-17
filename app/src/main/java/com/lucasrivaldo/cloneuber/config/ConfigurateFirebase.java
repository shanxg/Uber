package com.lucasrivaldo.cloneuber.config;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfigurateFirebase {

    public static final String USERS = "users";
    public static final String TRIP = "trip";
    public static final String REQUEST = "request";
    public static final String GEOFIRE = "geoFire";
    public static final String LAST_LOGIN = "last_login";

    public static final String TYPE_DRIVER  = "driver";
    public static final String TYPE_PASSENGER  = "passenger";


    private static FirebaseAuth auth;
    private static DatabaseReference fireDBRef;
    private static GeoFire geoFire;

    public static DatabaseReference getFireDBRef(){

        if(fireDBRef==null){
            fireDBRef = FirebaseDatabase.getInstance().getReference();
        }

        return fireDBRef;
    }


    public static FirebaseAuth getFirebaseAuth(){

        if(auth==null){
            auth = FirebaseAuth.getInstance();
        }

        return auth;
    }



    public static GeoFire getGeoFire(){

        if (geoFire == null) {
            DatabaseReference geoFireRef = ConfigurateFirebase.getFireDBRef().child(GEOFIRE);
            geoFire = new GeoFire(geoFireRef);
        }

        return geoFire;
    }

}
