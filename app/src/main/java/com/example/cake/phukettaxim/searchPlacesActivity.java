package com.example.cake.phukettaxim;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;



public class searchPlacesActivity extends ActionBarActivity implements TextWatcher, AsyncResponse,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private static final String LOG_TAG = "Google Places Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_DETAIL = "/details";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";



    private static final String API_KEY = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";

    private ListView listView1;
    public int PLACE_PICKER_REQUEST = 1;

    private static final int GOOGLE_API_CLIENT_ID = 0;




    public class locDetail{
        public String name;
        public String address;
        public LatLng location;
        public double distance;
        public String placeID;
    }

    List<locDetail> locAll = new ArrayList<locDetail>();

    public String selectedName;
    public String selectedAddress;
    public LatLng selectedLocation;

    public placeDetail selectedPlace;

    private  LatLng sw = new LatLng(6.954578, 97.622070);
    private LatLng ne = new LatLng(20.659322, 105.532227);

    private LatLngBounds mBounds = new LatLngBounds(sw, ne);


    private int fromOrTo = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_places);
        AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompView.addTextChangedListener(this);

        listView1 = (ListView) findViewById(R.id.listView);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            fromOrTo = extras.getInt("FROM_OR_TO");

        }


        mGoogleApiClient = new GoogleApiClient.Builder(searchPlacesActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();



        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i("LIST_PLACES", locAll.get(position).placeID);
                Log.i("LIST_PLACES", locAll.get(position).address);


                //loginRequest.delegate = this;
                sendPlaceIDRequest placeIDRequest = new sendPlaceIDRequest();

                placeIDRequest.execute(locAll.get(position).placeID);


            }
        });


    }

    protected void onStart() {
        super.onStart();

        //mBounds = Utility.boundsWithCenterAndLatLngDistance(new LatLng(13.91, 100.614471), 1000*1000, 1000*1000);


        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_places, menu);
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s != null) {
            // Retrieve the autocomplete results.
            //loginRequest.delegate = this;
            sendRequest autocompleteRequest = new sendRequest();
            autocompleteRequest.execute(s.toString());




        }

    }



    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void processFinish(JSONObject jsonResults) {
        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = null;
            try {
                jsonObj = new JSONObject(jsonResults.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");



            // Extract the Place descriptions from the results
            //resultList = new ArrayList(predsJsonArray.length());
            String [] pp = new String[predsJsonArray.length()];

            for (int i = 0; i < predsJsonArray.length(); i++) {
                System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
                System.out.println("============================================================");


                locDetail x = new locDetail();
                x.address = predsJsonArray.getJSONObject(i).getString("description");
                x.placeID = predsJsonArray.getJSONObject(i).getString("place_id");
                x.distance = 0;
                locAll.add(i,x);

                pp[i] = x.address;

            }



            ListAdapter places = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pp );
            listView1.setAdapter(places);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
    }



    public class sendRequest extends AsyncTask<String, Void, JSONObject > {

        HttpURLConnection conn = null;

        @Override
        protected JSONObject  doInBackground(String [] input) {
            ArrayList resultList = null;

            JSONObject jsonObj = null;
           // HttpURLConnection conn = null;
            StringBuilder jsonResults = new StringBuilder();
            try {
                StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
                sb.append("?key=" + API_KEY);
                sb.append("&components=country:th");
                sb.append("&input=" + URLEncoder.encode(input[0], "utf8"));

                Log.i("QUUUUUU", sb.toString());

                URL url = new URL(sb.toString());
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }


                try {
                    jsonObj = new JSONObject(jsonResults.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObj;
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error processing Places API URL", e);
                return jsonObj;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Places API", e);
                return jsonObj;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }


        }

        @Override
        protected void onPostExecute(JSONObject obj) {
            conn.disconnect();
            processFinish(obj);
        }

        @Override
        protected void onPreExecute() {

        }

    }

    @Override
    public void processFinishID(JSONObject jsonResults) {
        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = null;
            try {
                jsonObj = new JSONObject(jsonResults.toString());
                Log.i("Place ID",jsonResults.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject result = jsonObj.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
            //JSONObject loc = jsonObj.getJSONObject("geometry");




            selectedLocation = new LatLng(result.getDouble("lat"), result.getDouble("lng"));

            JSONObject result2 = jsonObj.getJSONObject("result");

            selectedAddress = result2.getString("formatted_address");
            selectedName = result2.getString("name");
            Log.i("Place ID",selectedName);
            Log.i("Place ID",selectedAddress);
            Log.i("Place ID",selectedLocation.toString());



            Intent intent = new Intent(getBaseContext(), mapActivity.class);

            Bundle args = new Bundle();
            args.putParcelable("Location", selectedLocation);

            intent.putExtra("SELECTED_PLACE_NAME", selectedName);
            intent.putExtra("SELECTED_PLACE_ADDRESS",selectedAddress);
            intent.putExtra("SELECTED_PLACE_LOCATION",args);
            intent.putExtra("FROM_OR_TO",fromOrTo);
            startActivity(intent);


        } catch (JSONException e) {
            Log.e("Fail", "Cannot process JSON results", e);
        }

    }

    @Override
    public void loginFinish(JSONObject output) {

    }

    @Override
    public void processFinishString(String output, String idd) {

    }

    @Override
    public void processFinishArray(JSONArray output) {

    }

    public class sendPlaceIDRequest extends AsyncTask<String, Void, JSONObject > {


        HttpURLConnection conn = null;
        @Override
        protected JSONObject  doInBackground(String [] input) {
            ArrayList resultList = null;

            JSONObject jsonObj = null;

            StringBuilder jsonResults = new StringBuilder();
            try {
                StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAIL + OUT_JSON);
                sb.append("?placeid=" + input[0]);
                sb.append("&key=" + API_KEY);


                Log.i("QUUUUUU", sb.toString());

                URL url = new URL(sb.toString());
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }


                try {
                    jsonObj = new JSONObject(jsonResults.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObj;
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error processing Places API URL", e);
                return jsonObj;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Places API", e);
                return jsonObj;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }


        }

        @Override
        protected void onPostExecute(JSONObject obj) {
            conn.disconnect();
            processFinishID(obj);
        }

        @Override
        protected void onPreExecute() {

        }

    }


    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public void onClickToMap(View v){


//        placeIDRequest.conn.disconnect();
       // autocompleteRequest.conn.disconnect();

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        Intent intent;
        try {
            intent = builder.build(getApplicationContext());
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
            System.out.println("start activity for result");
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                //String toastMsg = String.format("Place: %s", place.getName());
                //Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();



                selectedAddress = place.getAddress().toString();
                selectedName = place.getName().toString();
                selectedLocation = place.getLatLng();
                Log.i("Place ID",selectedName);
                Log.i("Place ID",selectedAddress);
                Log.i("Place ID", selectedLocation.toString());




                Intent intent = new Intent(getBaseContext(), mapActivity.class);

                Bundle args = new Bundle();
                args.putParcelable("Location", selectedLocation);

                intent.putExtra("SELECTED_PLACE_NAME", selectedName);
                intent.putExtra("SELECTED_PLACE_ADDRESS",selectedAddress);
                intent.putExtra("SELECTED_PLACE_LOCATION",args);
                intent.putExtra("FROM_OR_TO",fromOrTo);
                startActivity(intent);
            }
        }
    }
}
