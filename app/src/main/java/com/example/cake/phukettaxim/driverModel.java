package com.example.cake.phukettaxim;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by cake on 8/8/15 AD.
 */
public class driverModel {
    public String driverName;
    public String licensePlate;
    public String distance;
    public String carModel;
    public String id;
    public Bitmap im;
    public LatLng driverLoc;

    public driverModel(String dn, String lp, String dst, String cm, String id){
        this.carModel = cm;
        this.distance = dst;
        this.driverName = dn;
        this.licensePlate = lp;
        this.id = id;

    }

    public void setImage(Bitmap im){
        this.im = im;
    }

    public void setDriverLoc(LatLng ll){
        this.driverLoc = ll;
    }

    public void setDistance(String ll){
        this.distance = ll;
    }

}
