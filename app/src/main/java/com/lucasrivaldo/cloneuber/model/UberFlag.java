package com.lucasrivaldo.cloneuber.model;

import com.firebase.geofire.GeoQueryEventListener;

import java.io.Serializable;

public class UberFlag implements Serializable {

    private boolean isForTrip;
    private double radius;
    private Trip currentTrip;


    public UberFlag(boolean isForTrip, double radius, Trip currentTrip) {
        this.isForTrip = isForTrip;
        this.radius = radius;
        this.currentTrip = currentTrip;
    }

    public boolean isForTrip() {
        return isForTrip;
    }

    public double getRadius() {
        return radius;
    }

    public Trip getCurrentTrip() {
        return currentTrip;
    }

}
