package com.lucasrivaldo.cloneuber.model;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.lucasrivaldo.cloneuber.config.ConfigurateFirebase;
import com.lucasrivaldo.cloneuber.helper.UberHelper;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;




public class Trip implements Serializable {

    public Trip() {this.passengerCancelling = false;}

    private boolean passengerCancelling;

    private User driver, passenger;
    private String status, tripId, value, distanceText, durationText;

    private UberAddress startLoc, destination;

    // DISTANCE IN METERS && DURATION IN SECONDS
    private int distance, duration;
    private double tripType;


    public String calculate(double tripType){

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        double tripValue;

        tripValue = ((((this.getDistance()/1000)*0.5)+((this.getDuration()/60)*0.4)+2.50)*tripType);

        if (tripValue<5.50) {
            tripValue = 5.50;
        }else if(tripType == UberHelper.UBER_BLACK && tripValue < 8)
            tripValue = 8.00;

        return decimalFormat.format(tripValue);
    }


    public boolean save(){

        DatabaseReference dbRef = ConfigurateFirebase.getFireDBRef();
        DatabaseReference tripRef = dbRef.child(ConfigurateFirebase.TRIP).push();

        this.setTripId(tripRef.getKey());



        try {
            tripRef.setValue(this);
            this.request(true);

            return true;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("USERAPP", "SAVE ERROR: \n"+e.getMessage());
            return false;
        }
    }
    public boolean request(boolean isStarting){
        DatabaseReference dbRef = ConfigurateFirebase.getFireDBRef();
        if (isStarting){

            DatabaseReference requestRef = dbRef.child(ConfigurateFirebase.REQUEST)
                    .child(this.getStartLoc().getCountryCode())
                    .child(this.getStartLoc().getState())
                    .child(this.getTripId());

            try {
                requestRef.setValue("tripId", this.getTripId());
                return true;

            }catch (Exception e){
                e.printStackTrace();
                Log.d("USERAPP", "SAVE ERROR: \n"+e.getMessage());
                return false;
            }
        }else {
            DatabaseReference requestRef = dbRef.child(ConfigurateFirebase.REQUEST)
                    .child(this.getStartLoc().getCountryCode())
                    .child(this.getStartLoc().getState())
                    .child(this.getTripId());

            try {
                requestRef.removeValue();
                return true;

            }catch (Exception e){
                e.printStackTrace();
                Log.d("USERAPP", "SAVE ERROR: \n"+e.getMessage());
                return false;
            }
        }
    }

    public boolean updateDriverLocation(double latitude, double longitude){
        if(this.getTripId()!=null) {

            DatabaseReference tripDriverRef = ConfigurateFirebase.getFireDBRef()
                    .child(ConfigurateFirebase.TRIP)
                    .child(this.getTripId())
                    .child("driver");

            HashMap tripDriver = new HashMap();

            tripDriver.put("latitude", latitude);
            tripDriver.put("longitude", longitude);

            try {
                tripDriverRef.updateChildren(tripDriver);

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("USERTESTAPP", "update: " + e.getMessage());

                return false;
            }
        }else {
            if (this.getStatus().equals(UberHelper.START_TRIP)
                    || this.getStatus().equals(UberHelper.AWAITING))
                return true;
            else
                return false;
        }

    }

    public boolean update(){
        if(this.getTripId()!=null) {

            DatabaseReference tripRef = ConfigurateFirebase.getFireDBRef()
                                        .child(ConfigurateFirebase.TRIP)
                                        .child(this.getTripId());

            HashMap trip = new HashMap();
            trip.put("passengerCancelling", isPassengerCancelling());
            trip.put("status", this.getStatus());

            trip.put("driver", this.getDriver());

            trip.put("passenger", this.getPassenger());

            trip.put("distance", this.getDistance());
            trip.put("distanceText", this.getDistanceText());

            trip.put("duration", this.getDuration());
            trip.put("durationText", this.getDurationText());

            trip.put("value", this.getValue());


            try {

                tripRef.updateChildren(trip);
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("USERTESTAPP", "update: " + e.getMessage());

                return false;
            }
        }else {
            if (this.getStatus().equals(UberHelper.START_TRIP)
                    || this.getStatus().equals(UberHelper.AWAITING))
                return true;
            else
                return false;
        }
    }

    public boolean cancel(){

        DatabaseReference dbRef = ConfigurateFirebase.getFireDBRef();
        DatabaseReference tripRef = dbRef.child(ConfigurateFirebase.TRIP)
                                         .child(this.getTripId());

        try {
            tripRef.removeValue();
            this.request(false);

            return true;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("USERAPP", "SAVE ERROR: \n"+e.getMessage());
            return false;
        }
    }

    public boolean isPassengerCancelling() {
        return passengerCancelling;
    }

    public void setPassengerCancelling(boolean passengerCancelling) {
        this.passengerCancelling = passengerCancelling;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public long getDistance() { return distance; }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getTripType() {
        return tripType;
    }

    public void setTripType(double tripType) {
        this.tripType = tripType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value){
        this.value = value;
    }

    public void makeValue(double tripType) {

        this.setValue("R$ "+this.calculate(tripType));
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UberAddress getStartLoc() {
        return startLoc;
    }

    public void setStartLoc(UberAddress startLoc) {
        this.startLoc = startLoc;
    }

    public UberAddress getDestination() {
        return destination;
    }

    public void setDestination(UberAddress destination) {
        this.destination = destination;
    }

/*
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {


    }

 */
}
