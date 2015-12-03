package com.example.cake.phukettaxim;

import android.app.Application;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cake on 8/8/15 AD.
 */
public class globalVar extends Application {

    public Boolean allSelected = false;

    public placeDetail destinationLoc = new placeDetail();
    public placeDetail sourceLoc = new placeDetail();
    public  placeDetail custommerLocation = new placeDetail();
    public Bitmap custommerImage;
    public String taxiID;
    public String custommerID;
    public String nationality = "";


    public String omiseToken = "pkey_test_50zicu2el50z0t59id6";

    public String cardName;
    public String cardCity;
    public String cardPostalCode;
    public String cardNumber;
    public String cardExpirationMonth;
    public String cardExpirationYear;
    public String cardSecurityCode;
    public String cardEmail;


    public String fare;
    public  String isCash;

    public Map<String,taxiLocation > listTaxi =  new HashMap<String,taxiLocation >();
    public Map<String,String > taxiDistance =  new HashMap<String,String >();

    String pathToImage = "/images/custommers/";
    String mainHost = "128.199.97.22";
    public String distanceKey = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";

    public placeDetail getSelectedPlaceTo() {

        return destinationLoc;
    }

    public void setSelectedPlaceTo(placeDetail aName) {

        destinationLoc = aName;

    }

    public placeDetail getSelectedPlaceFrom() {

        return sourceLoc;
    }

    public void setSelectedPlaceFrom(placeDetail aEmail) {

        sourceLoc = aEmail;
    }

    public  void setCustommerImage(Bitmap x){
        this.custommerImage = x;
    }


}
