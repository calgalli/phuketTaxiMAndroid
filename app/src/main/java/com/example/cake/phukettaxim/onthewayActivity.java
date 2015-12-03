package com.example.cake.phukettaxim;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class onthewayActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private mqttService mService;
    private boolean mBound = false;
    private LatLng curentLocation;
    private LatLng taxiLocation;
    private LatLng newTaxiLocation;
    private LatLng fromLoc;


    public  final  static  String isCash = "mapIsCash";
    public final static  String fareString = "fare";


    public class chatMessage {
        public String type;
        public String message;
    }


    private List<chatMessage> chatMessages = new ArrayList<chatMessage>();

    private WebView myWebView;
    private final Handler mWebViewScrollHandler = new Handler();

    private EditText chatText;
    private RelativeLayout chatInput;
    private TextView ETAview;

    public final static String chatActivityMessage = "CHAT_ACTIVITY_MESSAGE";


    //MARK:****************************  MQTT Part ******************************************

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.i("MQTT chat on the way", "Messagge from service in Chat");
                String payload = bundle.getString(mqttService.RESULT);
                String topic = bundle.getString(mqttService.TOPIC);

                onCustommerResponses(topic,payload);
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
                LatLng taxiLocation = new LatLng(mService.selectedTaxiLat, mService.selectedTaxiLon);

                final globalVar gv = ((globalVar)getApplicationContext());

                newTaxiLocation = taxiLocation;
                // Move the camera instantly to hamburg with a zoom of 15.
                Marker kiel = map.addMarker(new MarkerOptions()
                                .position(taxiLocation)
                                .title("Taxi")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_icon))
                        );

                map.addMarker(new MarkerOptions()
                                .position(gv.sourceLoc.location)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.custommer_icon))
                );

                new ApiDirectionsAsyncTask().execute();


                zoomLevel x = new zoomLevel(gv.sourceLoc.location, newTaxiLocation);
                Log.i("Zoom level =", String.valueOf(x.getZoomLevel()));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(gv.sourceLoc.location, x.getZoomLevel()));

                // Zoom in, animating the camera.
                map.animateCamera(CameraUpdateFactory.zoomTo(x.getZoomLevel()), 2000, null);


                // Set driver image


                ImageView imDriverImage = (ImageView) findViewById(R.id.driverImageView);
                imDriverImage.setImageBitmap(mService.selectedTaxiDriver.im);
                TextView driverName = (TextView) findViewById(R.id.driverName);
                driverName.setText(mService.selectedTaxiDriver.driverName);
                TextView licensePlate = (TextView) findViewById(R.id.licensePlateNumber);
                licensePlate.setText(mService.selectedTaxiDriver.licensePlate);




                MLRoundedImageView xx  = (MLRoundedImageView) findViewById(R.id.custommerImageView2);
                xx.setImageBitmap(gv.custommerImage);

            } catch (ClassCastException e) {
                // Pass
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private void onCustommerResponses(String topic, String payload){

        if(topic.equals(mService.gTaxiResponseTopic + "/" + mService.selectedTaxiID)){
            try {
                JSONObject obj = new JSONObject(payload);

                if(obj.get("type").equals("locationUpdate")){

                    try {
                        taxiLocation x = new taxiLocation();
                        newTaxiLocation = new LatLng(obj.getDouble("lat"), obj.getDouble("lon"));

                        Log.i("MQTT Map", newTaxiLocation.toString());
                        map.clear();
                        map.addMarker(new MarkerOptions()
                                        .position(newTaxiLocation)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_icon))
                        );

                        final globalVar gv = ((globalVar)getApplicationContext());

                        map.addMarker(new MarkerOptions()
                                        .position(gv.sourceLoc.location)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.custommer_icon))
                        );


                        new ApiDirectionsAsyncTask().execute();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }



                    //updateMaker();
                } else if(obj.get("type").equals("chat")){
                    chatMessage c1 = new chatMessage();
                    c1.type = "in";
                    c1.message = obj.get("message").toString();
                    chatMessages.add(c1);
                    displayChatMessages();

                } else if(obj.get("type").equals("picup")) {
                    Button cancelButton = (Button) findViewById(R.id.cancel);
                    cancelButton.setVisibility(View.GONE);

                } else if(obj.get("type").equals("onthewayCancel")) {

                    new AlertDialog.Builder(this)
                            .setTitle("Driver cancel")
                            .setMessage("Sorry, the taxi can't go to pick you for some reasons, please try another taxi")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {


//                                    unbindService(mConnection);

                                    final Intent mapViewIntent = new Intent(getApplicationContext(), mapActivity.class);

                                    startActivity(mapViewIntent);

                                }
                            }).show();


                    Button cancelButton = (Button) findViewById(R.id.cancel);
                    cancelButton.setVisibility(View.GONE);

                } else if(obj.get("type").equals("done")) {

                    mService.unsubscribe(mService.gTaxiResponseTopic + "/" + mService.selectedTaxiID);

                    final globalVar gv = ((globalVar)getApplicationContext());
                    gv.isCash = obj.get("cash").toString();
                    gv.fare = obj.get("message").toString();
                    Log.i("DONE", "DONEEEEEEEEEEE");
                    final Intent paymentIntent = new Intent(getApplicationContext(),paymentActivity.class);

                    paymentIntent.putExtra(fareString, obj.get("message").toString());
                    paymentIntent.putExtra(isCash, obj.get("cash").toString());
                    startActivity(paymentIntent);
                    /*
                    Intent x = new Intent(getApplicationContext(), paymentActivity.class);

                    startActivity(x);*/
                }









                } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //************************** End MQTT Part **************************************

    //************************** Web view part **************************************

    private void displayChatMessages(){
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><HEAD><LINK href=\"chatBubble.css\" type=\"text/css\" rel=\"stylesheet\"/></HEAD><body>");
        //sb.append(tables.toString());


        for (chatMessage temp : chatMessages) {
            if(temp.type.equals("in")) {
                sb.append("<div class=\"bubbledLeft\">");
                sb.append(temp.message);
                sb.append("</div>");
            } else {
                sb.append("<div class=\"bubbledRight\">");
                sb.append(temp.message);
                sb.append("</div>");
            }

        }

        sb.append("</body></HTML>");

        myWebView.loadDataWithBaseURL("file:///android_asset/", sb.toString(), "text/html", "utf-8", null);
        //Scroll down to the bottom webview
        mWebViewScrollHandler.removeCallbacks(mScrollWebViewTask);
        mWebViewScrollHandler.postDelayed(mScrollWebViewTask, 100);


    }

    //Scroll down to the bottom webview
    private final Runnable mScrollWebViewTask = new Runnable() {
        public void run() {
            myWebView.pageDown(true);
        }
    };

    //Button handles

    public void onCancel(View v){

        String removeCustomer = "removeContomer/" + mService.selectedTaxiID;
        mService.publish(removeCustomer,mService.userID);
        mService.selectedTaxiID = "";
        mService.selectedTaxiLat = 0.0;
        mService.selectedTaxiLon = 0.0;

        JSONObject done = new JSONObject();
        try {
            done.put("id", mService.userID);
            done.put("type", "cancel");
            done.put("message", "");

            String topic1 = mService.gCustomerResponseTopic + "/" + mService.userID;
            mService.publish(topic1, done.toString());
            mService.unsubscribe(mService.gTaxiResponseTopic + "/" + mService.selectedTaxiID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent onthewayIntent = new Intent(getApplicationContext(),mapActivity.class);

        startActivity(onthewayIntent);


    }

    public void onSendChatMessage(View v){
        if(chatText.getText().length() > 0) {
            JSONObject done = new JSONObject();
            try {
                done.put("id", mService.selectedTaxiID);
                done.put("type", "chat");
                done.put("message", chatText.getText().toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String topic1 = mService.gCustomerResponseTopic + "/" + mService.userID;
            mService.publish(topic1, done.toString());


            chatMessage c1 = new chatMessage();
            c1.type = "out";
            c1.message =chatText.getText().toString();
            chatMessages.add(c1);
            chatText.setText("", TextView.BufferType.EDITABLE);
            displayChatMessages();

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ontheway);

        Intent intent = new Intent(this, mqttService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);



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

        fromLoc = gv.sourceLoc.location;


    }

    @Override
    protected void onStart() {
        super.onStart();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        myWebView = (WebView) findViewById(R.id.webView);


        chatInput = (RelativeLayout) findViewById(R.id.chatInput);
        chatText = (EditText) findViewById(R.id.chatText);

        displayChatMessages();




    }


    @Override
    protected void onResume() {
        super.onResume();


        Log.d("MQTT Chat", "Resume");


        registerReceiver(receiver, new IntentFilter(mqttService.NOTIFICATION));
        mGoogleApiClient.connect();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        if(mBound == true) {
   //         unbindService(mConnection);
        }
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ontheway, menu);
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


            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.onTheWayMap))
                    .getMap();

            // Check if we were successful in obtaining the map.
            if (map != null) {
                map.setMyLocationEnabled(true);

            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

    }


    private void handleNewLocation(Location location) {
        Log.d("MAP", location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        curentLocation = latLng;




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
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("MAP", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    public class ApiDirectionsAsyncTask extends AsyncTask<URL, Integer, StringBuilder> {

        private static final String TAG = "MAP API";

        private static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
        private static final String OUT_JSON = "/json";

        // API KEY of the project Google Map Api For work
        //private static final String API_KEY = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";

        @Override
        protected StringBuilder doInBackground(URL... params) {
            Log.i(TAG, "doInBackground of ApiDirectionsAsyncTask");

            HttpURLConnection mUrlConnection = null;
            StringBuilder mJsonResults = new StringBuilder();
            final globalVar gv = ((globalVar)getApplicationContext());

            try {
                StringBuilder sb = new StringBuilder(DIRECTIONS_API_BASE + OUT_JSON);
                sb.append("?origin=" + String.valueOf(fromLoc.latitude) + "," +  String.valueOf(fromLoc.longitude));
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

                Log.i(TAG, String.valueOf(mJsonResults));


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
                    final String e = durationText.get("text").toString();



                    onthewayActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ETAview = (TextView) findViewById(R.id.distance);
                            ETAview.setText("Distance : " + d + " ETA : " + e);
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
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
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
            bmImage.setImageBitmap(result);
        }
    }




}
