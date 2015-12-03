package com.example.cake.phukettaxim;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class mapActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, AsyncResponse {

    private mqttService mService;
    private boolean mBound = false;


    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LatLng curentLocation;

    private int fromOrTo = 0;

    private Button requestButton;
    private SupportMapFragment mMapFragment;
    ListView listView;

    private ArrayList<driverModel> arrayOfDrivers = new ArrayList<driverModel>();
    public Map<String,String > taxiDistance =  new HashMap<String,String >();


    private Boolean fireOnce = true;

    private TextView timer;

    int zoomLevel = 10000;
    Boolean isFihishMarers = false;

    public Location myLocation;

    private CountDownTimer waitTimer;
    //Set up a receiver for image donwloading
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final globalVar gv = ((globalVar)getApplicationContext());

            // String url = "http://" + gv.mainHost + gv.pathToImage + mService.idImage + ".png";
            // new DownloadImageTask(gv, (MLRoundedImageView) findViewById(R.id.custommerImageView)).execute(url);

            MLRoundedImageView cc = (MLRoundedImageView) findViewById(R.id.custommerImageView);
            cc.setImageBitmap(gv.custommerImage);


        }
    };



    //MARK:****************************  MQTT Part ******************************************

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.i("MQTT map", "Messagge from service in map");
                String payload = bundle.getString(mqttService.RESULT);
                String topic = bundle.getString(mqttService.TOPIC);


                if(bundle.getString(mqttService.TOPIC).equals(mService.updateTaxiLocationTopic+mService.userID)){
                    try {
                        JSONObject obj = new JSONObject(payload);
                        taxiLocation x = new taxiLocation();

                        x.id = obj.getString("id");
                        x.lat = obj.getDouble("lat");
                        x.lon = obj.getDouble("lon");
                        mService.listTaxi.put(x.id,x);

                        Log.i("Taxi id", x.id );

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i("MQTT Map",payload);
                    final globalVar gv = ((globalVar)getApplicationContext());



                    Iterator entries = mService.listTaxi.entrySet().iterator();
                    while (entries.hasNext()) {
                        Map.Entry thisEntry = (Map.Entry) entries.next();
                        //Object key = thisEntry.getKey();
                        taxiLocation value = (taxiLocation) thisEntry.getValue();
                        LatLng to = new LatLng(value.lat, value.lon);
                        MyTaskParams params = new MyTaskParams(gv.custommerLocation.location, to, value.id);

                        ApiDirectionsAsyncTask myTask = new ApiDirectionsAsyncTask();
                        myTask.execute(params);

                      /*  int y = new zoomLevel(gv.sourceLoc.location, to).getZoomLevel();
                        Log.i("Xoom Lvel = ", String.valueOf(y));
                        if( y > zoomLevel){
                            zoomLevel = y;
                        }*/


                        // ...
                    }


                    updateMaker();
                } else if(bundle.getString(mqttService.TOPIC).startsWith(mService.gTaxiResponseTopic)) {
                    try {
                        JSONObject obj = new JSONObject(payload);
                       if(obj.getString("type").equals("ack")){
                           waitTimer.cancel();
                            if(obj.getString("value").equals("REJECT")){

                                mService.listTaxi.remove(mService.selectedTaxiID);
                                String  removeCustomer = "removeContomer/" + mService.selectedTaxiID;
                                mService.publish(removeCustomer, mService.userID);


                                removeATaxi();

                                reloadTable();
                               // Toast.makeText(getApplicationContext(),
                               //         "Taxi reject", Toast.LENGTH_LONG)
                               //         .show();
                            } else {
                                waitTimer.cancel();



                                final globalVar gv = ((globalVar)getApplicationContext());
                                gv.sourceLoc.location = curentLocation;

                                JSONObject data = new JSONObject();
                                try {
                                    data.put("id", mService.userID);
                                    data.put("type", "costumerLocation");

                                    Log.i("xxxxxx=", String.valueOf(gv.sourceLoc.location.latitude));
                                    Log.i("xxxxxx=", String.valueOf(gv.sourceLoc.location.longitude));
                                    data.put("lat", String.valueOf(gv.sourceLoc.location.latitude));
                                    data.put("lon",String.valueOf(gv.sourceLoc.location.longitude));

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                String topic1 = mService.gCustomerResponseTopic+"/"+mService.userID;
                                mService.publish(topic1, data.toString());
                               // mService.listTaxi.remove(mService.selectedTaxiID);

                                mService.selectedTaxiID =  obj.getString("id");
                                mService.selectedTaxiLat = obj.getDouble("lat");
                                mService.selectedTaxiLon = obj.getDouble("lon");



                              //  Toast.makeText(getApplicationContext(),
                               //         "Taxi OK", Toast.LENGTH_LONG)
                               //         .show();
                                Intent onthewayIntent = new Intent(getApplicationContext(),onthewayActivity.class);

                                startActivity(onthewayIntent);
                            }
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //Start to watch the topic
              /*  if(bundle.getString(mqttService.TOPIC).equals(mService.requestTopic+mService.taxiID)){
                    try {
                        JSONObject obj = new JSONObject(payload);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i("MQTT Map",payload);
                } else if(bundle.get(mqttService.TOPIC).equals(mService.gCustomerResponseTopic + mService.reuestedCustomer)){

                }*/


            }
        }
    };

    public void removeATaxi() {
        arrayOfDrivers.clear();

        Iterator entries = mService.listTaxi.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            //Object key = thisEntry.getKey();
            taxiLocation value = (taxiLocation) thisEntry.getValue();
            Log.i("Taxi list", "+++++++++++++++++++++++++++++++++");
            Log.i("Id = ", value.id);

            String[] loginParameters = {value.id};
            sendRequest loginRequest = new sendRequest();
            loginRequest.delegate = this;
            loginRequest.execute(loginParameters);


        }

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        // @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                // We've bound to LocalService, cast the IBinder and get LocalService instance.
                mqttService.mqttBinder binder = (mqttService.mqttBinder) service;
                mService = binder.getService();
                mBound = true;
                //TODO add marker to the map

                mService.publish("request/" + mService.userID, mService.userID);


                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    String selectedName = extras.getString("SELECTED_PLACE_NAME");
                    String selectedAddress = extras.getString("SELECTED_PLACE_ADDRESS");

                    Bundle bundle = getIntent().getParcelableExtra("SELECTED_PLACE_LOCATION");
                    LatLng fromPosition = bundle.getParcelable("Location");
                    fromOrTo = extras.getInt("FROM_OR_TO");


                    placeDetail x = new placeDetail();
                    if(fromOrTo == 0 ) {
                        final globalVar gv = ((globalVar)getApplicationContext());
                        x.pname = selectedName;
                        x.address = selectedAddress;
                        x.location = fromPosition;
                        gv.setSelectedPlaceFrom(x);

                        placeDetail y = gv.getSelectedPlaceFrom();

                        TextView fromName = (TextView)findViewById(R.id.fromName);
                        fromName.setText("From : "+y.pname);
                        TextView fromAddress = (TextView)findViewById(R.id.fromAddress);
                        fromAddress.setText(y.address);


                    } else {
                        final globalVar gv = ((globalVar)getApplicationContext());
                        x.pname = selectedName;
                        x.address = selectedAddress;
                        x.location = fromPosition;
                        gv.setSelectedPlaceTo(x);

                        placeDetail y = gv.getSelectedPlaceTo();


                        TextView fromName = (TextView)findViewById(R.id.toName);
                        fromName.setText("To : " + y.pname);
                        TextView fromAddress = (TextView)findViewById(R.id.toAddress);
                        fromAddress.setText(y.address);

                        gv.allSelected = true;
                        requestButton.setVisibility(View.VISIBLE);

                    }
                    Log.i("Return val", selectedName);
                    Log.i("Return val", selectedAddress);
                    Log.i("Return val", fromPosition.toString());

                }



                updateMaker();

                final globalVar gv = ((globalVar)getApplicationContext());


                gv.custommerID = mService.userID;
                //new DownloadImageTask((MLRoundedImageView) findViewById(R.id.custommerImageView))
                  //      .execute(url);



            } catch (ClassCastException e) {
                // Pass
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    public void updateMaker(){
        map.clear();
        zoomLevel = 10000;
        final globalVar gv = ((globalVar)getApplicationContext());




        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id",mService.userID);
            locDetail.put("From", "Here");
            locDetail.put("to", "There");
            if(gv.custommerLocation.location != null) {
                locDetail.put("lat", String.valueOf(gv.custommerLocation.location.latitude));
                locDetail.put("lon", String.valueOf(gv.custommerLocation.location.longitude));
            }
            locDetail.put("requestFlag","0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Iterator entries = mService.listTaxi.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            //Object key = thisEntry.getKey();
            taxiLocation value = (taxiLocation) thisEntry.getValue();
            Log.i("Taxi list","+++++++++++++++++++++++++++++++++");
            Log.i("Id = ", value.id);
            Log.i("lat = ", String.valueOf(value.lat));
            Log.i("lon = ", String.valueOf(value.lon));

            LatLng latLng = new LatLng(value.lat, value.lon);

            map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_icon))
            );

            LatLng to = new LatLng(value.lat, value.lon);
            MyTaskParams params = new MyTaskParams(curentLocation, to, value.id);

            ApiDirectionsAsyncTask myTask = new ApiDirectionsAsyncTask();
            myTask.execute(params);

            Log.i("LOCATION", locDetail.toString());

            String topic = "cli/"+value.id;
            Log.i("LOCATION", topic);
            mService.publish(topic, locDetail.toString());
            if(gv.sourceLoc.location != null) {
                int y = new zoomLevel(gv.sourceLoc.location, to).getZoomLevel();
                Log.i("Xoom Lvel = ", String.valueOf(y));
                if (y < zoomLevel) {
                    zoomLevel = y;
                }
            } else {
                zoomLevel = 12;
            }

            // ...
        }

        if(gv.sourceLoc.location != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(gv.sourceLoc.location, zoomLevel));
        }

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);

        isFihishMarers = true;


    }




    public void onClickFrom(View v){
        Log.i("MAPView", "From is clicked");
        final Intent mapViewIntent = new Intent(getApplicationContext(),searchPlacesActivity.class);
        fromOrTo = 0;
        mapViewIntent.putExtra("FROM_OR_TO", fromOrTo);

        startActivity(mapViewIntent);
    }

    public void onClickTo(View v){
        Log.i("MAPView", "To is clicked");
        final Intent mapViewIntent = new Intent(getApplicationContext(),searchPlacesActivity.class);
        fromOrTo = 1;
        mapViewIntent.putExtra("FROM_OR_TO", fromOrTo);

        startActivity(mapViewIntent);
    }

    public void onClickRequest(View v){
        Log.i("Click request", "aaaaa *************************** ");

        arrayOfDrivers.clear();

        Iterator entries = mService.listTaxi.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            //Object key = thisEntry.getKey();
            taxiLocation value = (taxiLocation) thisEntry.getValue();
            Log.i("Taxi list","+++++++++++++++++++++++++++++++++");
            Log.i("Id = ", value.id);

            String [] loginParameters = {value.id};
            sendRequest loginRequest = new sendRequest();
            loginRequest.delegate = this;
            loginRequest.execute(loginParameters);

        }


        // Construct the data source
        //ArrayList<driverModel> arrayOfUsers = new ArrayList<driverModel>();
// Create the adapter to convert the array to views
        Log.i("DRIVERS", arrayOfDrivers.toString());
        reloadTable();





        FrameLayout layout = (FrameLayout) findViewById (R.id.mapFrame);
        layout.setVisibility(View.INVISIBLE); // or View.INVISIBLE, depending on what you exactly want

        Button backBt = (Button) findViewById(R.id.backButton);
        backBt.setVisibility(View.VISIBLE);

    }


    public void reloadTable(){
        customeCell adapter = new customeCell(this, -1, arrayOfDrivers);


        listView.setAdapter(adapter);
    }


    public void onClickProfile(View v){
        Intent onthewayIntent = new Intent(getApplicationContext(),profileActivity.class);

        startActivity(onthewayIntent);

    }


    public void onClickBack(View v){
        Log.i("Click on", "aaaaa *************************** ");

        FrameLayout layout1 = (FrameLayout) findViewById(R.id.taxiListFrame);
        layout1.setVisibility(View.INVISIBLE);

        FrameLayout layout = (FrameLayout) findViewById (R.id.mapFrame);
        layout.setVisibility(View.VISIBLE); // or View.INVISIBLE, depending on what you exactly want

        Button backBt = (Button) findViewById(R.id.backButton);
        backBt.setVisibility(View.INVISIBLE);

        requestButton.setVisibility(View.INVISIBLE);
        TextView fromName = (TextView)findViewById(R.id.toName);
        fromName.setText("To : ");
        TextView fromAddress = (TextView)findViewById(R.id.toAddress);
        fromAddress.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if(mBound == false) {

         //   Intent intent = new Intent(this, mqttService.class);

           // bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create().setSmallestDisplacement(10)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);

        setUpMapIfNeeded();




            final globalVar gv = ((globalVar)getApplicationContext());



            placeDetail y = gv.getSelectedPlaceTo();




            TextView toName = (TextView)findViewById(R.id.toName);
            toName.setText(y.pname);
            TextView toAddress = (TextView)findViewById(R.id.toAddress);
            toAddress.setText(y.address);

       // LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);






        final LocationManager locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        // Create a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Get Current Location
        //Location myLocation = locationManager.getLastKnownLocation(provider);

        String namePlace = "";

        Location myLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);



        TextView fromName = (TextView)findViewById(R.id.toName);
        fromName.setText("To : ");





        listView = (ListView) findViewById(R.id.listDriver);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                timer = (TextView) view.findViewById(R.id.counter);
                final int selectedPos = position;

                timer.setText("60");

                listView.setEnabled(false);
                waitTimer = new CountDownTimer(60000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        timer.setText(Long.toString(millisUntilFinished / 1000));
                        //acc.setMessage("00:"+ (millisUntilFinished/1000));
                    }

                    @Override
                    public void onFinish() {
                        //info.setVisibility(View.GONE);
                        //mService.listTaxi.remove(mService.selectedTaxiID);
                        String removeCustomer = "removeContomer/" + mService.selectedTaxiID;
                        mService.publish(removeCustomer, mService.userID);
                        Log.i("Timer finsh", "Finished");
                        arrayOfDrivers.remove(selectedPos);
                      //  removeATaxi();
                        listView.setEnabled(true);
                        reloadTable();

                        //TODO Cancel timer


                    }


                }.start();


                mService.selectedTaxiID = arrayOfDrivers.get(position).id;
                mService.selectedTaxiDriver = arrayOfDrivers.get(position);


                long tt = System.currentTimeMillis();
                final globalVar gv = ((globalVar) getApplicationContext());
                placeDetail x = gv.getSelectedPlaceFrom();
                placeDetail y = gv.getSelectedPlaceTo();
                gv.taxiID = mService.selectedTaxiID;

                JSONObject data = new JSONObject();
                try {
                    data.put("id", mService.userID);
                    data.put("From", x.pname);
                    if(x.address.equals("")) {
                        data.put("fromAddress", x.pname);
                    } else {
                        data.put("fromAddress", x.address);
                    }

                    data.put("to", y.pname);

                    if(y.address.equals("")) {
                        data.put("toAddress", y.pname);
                    } else {
                        data.put("toAddress", y.address);
                    }


                    data.put("currentTime", String.valueOf(tt));
                    data.put("lat", String.valueOf(x.location.latitude));
                    data.put("lon", String.valueOf(x.location.longitude));
                    data.put("requestFlag", "1");
                    data.put("note", "note");
                    data.put("nationality",gv.nationality);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                String topic = "cli/" + mService.selectedTaxiID;
                mService.publish(topic, data.toString());

                Log.i("JSON =", data.toString());
                Log.i("Topic =", topic);


                mService.subscribe(mService.gTaxiResponseTopic + "/" + mService.selectedTaxiID);


            }
        });


        requestButton = (Button) findViewById(R.id.Request);
        requestButton.setBackground(getImage("requestButton.png"));
        requestButton.setText("");

        //ImageView bImage = (ImageView) findViewById(R.id.buttonImageView);

        //bImage.setImageBitmap(drawableToBitmap(getImage("requestButton.png")));
        requestButton.setVisibility(View.INVISIBLE);

        Button backBt = (Button) findViewById(R.id.backButton);
        backBt.setVisibility(View.INVISIBLE);


    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MQTT", "Resume");


        registerReceiver(receiver, new IntentFilter(mqttService.NOTIFICATION));
        registerReceiver(downloadReceiver, new IntentFilter(MainActivity.DOWNLOAD_DONE));


        mGoogleApiClient.connect();
        setUpMapIfNeeded();

        final globalVar gv = ((globalVar)getApplicationContext());


        MLRoundedImageView cc = (MLRoundedImageView) findViewById(R.id.custommerImageView);
        cc.setImageBitmap(gv.custommerImage);

       // Intent intent = new Intent(this, mqttService.class);

       // bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        listView.setEnabled(true);


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(downloadReceiver);
        if(mBound == true) {
      //      unbindService(mConnection);
        }

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();

       // Intent intent = new Intent(this, mqttService.class);
       // stopService(intent);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //******************************** MAP calls *************************************************


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.


            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            // Check if we were successful in obtaining the map.
            if (map != null) {
                map.setMyLocationEnabled(true);

            }
        }
    }




    private void handleNewLocation(Location location) {
        Log.d("MAP", location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        curentLocation = latLng;

        Log.i("Location update", curentLocation.toString());

        if(isFihishMarers) {

            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            map.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);

        }

        //getDistanceInfo();

        //new ApiDirectionsAsyncTask().execute();



    }




    @Override
    public void onConnected(Bundle bundle) {

        final globalVar gv = ((globalVar)getApplicationContext());


        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        myLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);





        String namePlace = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        Log.i("LOCATION ************", myLocation.toString());

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(myLocation.getLatitude(), myLocation.getLongitude(), 1);
            if(addresses == null) {
                namePlace = "unknown";
            } else {
                String add = addresses.get(0).getThoroughfare();
                String cityName = addresses.get(0).getAddressLine(0);
                String stateName = addresses.get(0).getAddressLine(1);
                String countryName = addresses.get(0).getAddressLine(2);
                if (add != null) {
                   // Log.i("city_name", add);
                    namePlace = add;
                } else {
                   // Log.i("city_name", cityName);
                    namePlace = cityName;
                }



            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        gv.custommerLocation.pname = "Current location";
        gv.custommerLocation.address = namePlace;
        gv.custommerLocation.location = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());





        gv.sourceLoc.pname = gv.custommerLocation.pname;
        gv.sourceLoc.address = gv.custommerLocation.address;
        gv.sourceLoc.location = gv.custommerLocation.location;

        gv.setSelectedPlaceFrom(gv.custommerLocation);



        placeDetail y2 = gv.getSelectedPlaceFrom();

        TextView fromName = (TextView)findViewById(R.id.fromName);
        fromName.setText("From : " + y2.pname);
        TextView fromAddress = (TextView)findViewById(R.id.fromAddress);
        fromAddress.setText(y2.address);

        if(mBound == false) {

            Intent intent = new Intent(this, mqttService.class);

            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }



    public class sendRequest extends AsyncTask<String, Void, JSONObject> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONObject jj = null;
        public AsyncResponse delegate=null;
        @Override
        protected JSONObject doInBackground(String[] params1) {
            // do above Server call here
            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://128.199.97.22:1880/getDriverData");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String id = "";

            if(params1.length == 1) {
                id = params1[0];

            }
            nameValuePairs.add(new BasicNameValuePair("id", id));
             try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);
                Log.i("Http", response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            String json = null;

            try {
                json = EntityUtils.toString(response.getEntity());
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // String retSrc = EntityUtils.toString(entity);
                    // parsing JSON
                    JSONArray result = new JSONArray(json); //Convert String to JSON Object
                    if(result.length() == 1) {
                        jj = result.getJSONObject(0);
                        // Log.i("Http", response.getStatusLine().toString());
                        // Log.i("Http", jj.toString());
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jj;
        }

        @Override
        protected void onPostExecute(JSONObject obj) {
            //process message
            if(obj != null)
            {
                //do something
            }


            delegate.processFinish(obj);
        }

        @Override
        protected void onPreExecute() {
            SchemeRegistry schemeRegistry = new SchemeRegistry();

            // http scheme
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            // https scheme
            schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 1880));

            params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf8");
            clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);


        }

    }


    public class sendRequestDriverImage extends AsyncTask<String, Void, String> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONObject jj = null;
        public AsyncResponse delegate=null;
        public String idd = "";
        @Override
        protected String doInBackground(String[] params1) {
            // do above Server call here
            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://128.199.97.22:1880/getDriverImage");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String id = "";



            if(params1.length == 2) {
                id = params1[0];
                idd = params1[1];
            }

            String filename = id+".png";

            nameValuePairs.add(new BasicNameValuePair("filename", filename));
            try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);

                int responseCode = response.getStatusLine().getStatusCode();
                switch(responseCode)
                {
                    case 200:
                        HttpEntity entity = response.getEntity();
                        if(entity != null)
                        {
                            String responseBody = EntityUtils.toString(entity);
                            return responseBody;
                            //Log.i("Http ======== ", responseBody);
                        }
                        break;
                }



               // HttpEntity entity = response.getEntity();

            } catch (IOException e) {
                e.printStackTrace();
            }


            return "";
        }

        @Override
        protected void onPostExecute(String obj) {
            //process message
            if(obj != null)
            {
                //do something
            }



            delegate.processFinishString(obj, idd);
        }

        @Override
        protected void onPreExecute() {
            SchemeRegistry schemeRegistry = new SchemeRegistry();

            // http scheme
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            // https scheme
            schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 1880));

            params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf8");
            clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);


        }

    }


    //Asyn responses
    @Override
    public void processFinish(JSONObject output) {

        if(output == null){
            Toast.makeText(getApplicationContext(), "Login fail",
                    Toast.LENGTH_LONG).show();
        } else {
            Log.i("Http", output.toString());
            try {
                String licensePlateNumber = (String) output.get("licensePlateNumber");
                String carModel = (String) output.get("carModel");
                String name = (String) output.get("firstName") + (String) output.get("lastname");
                String id = (String) output.get("id");
                String distance = taxiDistance.get(id);


                driverModel data = new driverModel(name, licensePlateNumber, distance, carModel, id);
                arrayOfDrivers.add(data);

                String [] loginParameters = {id.replaceFirst("id",""), id};

                sendRequestDriverImage loginRequest = new sendRequestDriverImage();
                loginRequest.delegate = this;
                loginRequest.execute(loginParameters);



                } catch (JSONException e) {
                e.printStackTrace();
            }





        }

    }

    @Override
    public void processFinishString(String output, String idd) {

        Bitmap x = decodeBase64(output);

       /* Iterator<driverModel> entries = arrayOfDrivers.iterator();
        while (entries.hasNext()) {
           if(entries.next().id.equals(idd)){
               entries.next().setImage(x);
           }

        }*/

       int i = 0;
        for(i =0; i < arrayOfDrivers.size();i++){
            if(arrayOfDrivers.get(i).id.equals(idd)){
                arrayOfDrivers.get(i).setImage(x);
            }
        }



        customeCell adapter = new customeCell(this, -1, arrayOfDrivers);
// Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.listDriver);
        listView.setAdapter(adapter);





    }

    @Override
    public void processFinishArray(JSONArray output) {

    }

    @Override
    public void processFinishID(JSONObject output) {




    }

    @Override
    public void loginFinish(JSONObject output) {

    }

    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }


    private static class MyTaskParams {
        LatLng fromLoc;
        LatLng newTaxiLocation;
        String id;

        MyTaskParams(LatLng fromLoc, LatLng newTaxiLocation,  String id) {
            this.fromLoc = fromLoc;
            this.newTaxiLocation = newTaxiLocation;
            this.id = id;
        }
    }

    public class ApiDirectionsAsyncTask extends AsyncTask<MyTaskParams, Integer, StringBuilder> {

        private static final String TAG = "MAP API";

        private static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
        private static final String OUT_JSON = "/json";

        // API KEY of the project Google Map Api For work
        //private static final String API_KEY = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";

        @Override
        protected StringBuilder doInBackground(MyTaskParams... params) {
            Log.i(TAG, "doInBackground of ApiDirectionsAsyncTask");



            LatLng fromLoc = params[0].fromLoc;
            LatLng newTaxiLocation = params[0].newTaxiLocation;
            final String id = params[0].id;

            HttpURLConnection mUrlConnection = null;
            StringBuilder mJsonResults = new StringBuilder();
            final globalVar gv = ((globalVar)getApplicationContext());

            try {
                StringBuilder sb = new StringBuilder(DIRECTIONS_API_BASE + OUT_JSON);
                sb.append("?origin=" + String.valueOf(gv.sourceLoc.location.latitude) + "," +  String.valueOf(gv.sourceLoc.location.longitude));
                sb.append("&destination=" + String.valueOf(newTaxiLocation.latitude) + "," +  String.valueOf(newTaxiLocation.longitude));
                sb.append("&key=" + gv.distanceKey);
                sb.append("&mode=driving&sensor=false");

                URL url = new URL(sb.toString());
                mUrlConnection = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(mUrlConnection.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1){
                    mJsonResults.append(buff, 0, read);
                }

                //Log.i(TAG, String.valueOf(mJsonResults));


                JSONObject jsonObject = new JSONObject();
                try {

                    jsonObject = new JSONObject(String.valueOf(mJsonResults));

                    JSONArray array = jsonObject.getJSONArray("routes");

                    JSONObject routes = array.getJSONObject(0);

                    JSONArray legs = routes.getJSONArray("legs");

                    JSONObject steps = legs.getJSONObject(0);

                    JSONObject distance = steps.getJSONObject("distance");




                    JSONObject durationText = steps.getJSONObject("duration");


                    Log.i("Distance", distance.get("text").toString());
                    Log.i("Duration", durationText.get("text").toString());
                    //dist = Double.parseDouble(distance.getString("text").replaceAll("[^\\.0123456789]","") );
                    final String d = distance.get("text").toString();
                   // final String e = durationText.get("text").toString();



                    mapActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            taxiDistance.put(id,d);
                            Log.i("Fucking distance", id +"," + taxiDistance.get(id));
                           //Your code to run in GUI thread here
                        }//public void run() {
                    });



                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            } catch (MalformedURLException e) {
                Log.e(TAG, "Error processing Distance Matrix API URL");
                return null;

            } catch (IOException e) {
                System.out.println("Error connecting to Distance Matrix");
                return null;
            } finally {
                if (mUrlConnection != null) {
                    mUrlConnection.disconnect();
                }
            }

            return mJsonResults;
        }
    }


    private Drawable getImage(String name) {
        Drawable d = null;
        try {
            // get input stream
            InputStream ims = getAssets().open(name);
            // load image as Drawable
            d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            return  d;

        } catch (IOException ex) {
            return d;
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        globalVar bmImage;
        ImageView mm;

        public DownloadImageTask(globalVar bmImage, ImageView mm) {
            this.bmImage = bmImage;
            this.mm = mm;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setCustommerImage(result);
            mm.setImageBitmap(result);
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


}
