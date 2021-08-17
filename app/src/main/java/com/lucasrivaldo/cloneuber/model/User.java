package com.lucasrivaldo.cloneuber.model;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;

import java.io.Serializable;

public class User implements Serializable {

    public User() {}

    private String name, email, id, pw;
    private String type;

    private double latitude, longitude;

    public boolean save(){

        DatabaseReference usersRef =
                ConfigurateFirebase.getFireDBRef().child(ConfigurateFirebase.USERS);

        DatabaseReference userRef =
                usersRef.child(this.getType()).child(this.getId());


        try {
            userRef.setValue(this);
            return true;

        }catch (Exception e){
            e.printStackTrace();
            Log.e("USER SAVE ERROR", "save: "+e.getMessage() );
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public  String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Exclude
    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}


