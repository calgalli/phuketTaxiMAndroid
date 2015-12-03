package com.example.cake.phukettaxim;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by cake on 8/15/15 AD.
 */
public class zoomLevel {

    private LatLng p1;
    private LatLng p2;



    public zoomLevel(LatLng p1, LatLng p2){
        this.p1 = p1;
        this.p2 = p2;
    }


    public int getZoomLevel() {
        int zoomLevel = 0;


            double radius = distance(p1.latitude,p1.longitude,p2.latitude,p2.longitude);
            double scale = radius / 500;
            zoomLevel =(int) (16 - Math.log(scale) / Math.log(2));

        return zoomLevel;
    }

    public float distance (double lat_a, double lng_a, double lat_b, double lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }
}
