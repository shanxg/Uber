package com.lucasrivaldo.cloneuber.view_model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.GoogleMap;

public class MapViewModel extends ViewModel {

    private MutableLiveData<GoogleMap> mMap = new MutableLiveData<>();

    public LiveData<GoogleMap> getMap(){
        return mMap;
    }

    public void setMap(GoogleMap map){
        mMap.postValue(map);
    }
}
