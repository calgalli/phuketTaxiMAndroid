package com.example.cake.phukettaxim;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class profileActivity extends ActionBarActivity implements AsyncResponse{

    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "MyPrefs" ;



    public class transactionDetail {
        public String licensePlate = "";
        public String driverName = "";
        public String date = "";
        public String time = "";
        public String  fare = "";
        public String driverId = "";
    }

    private ArrayList<transactionDetail> transactions = new ArrayList<transactionDetail>();

    public Map<String,transactionDetail > transactionsDict =  new HashMap<String,transactionDetail >();
    public Map<String,String > driverNames =  new HashMap<String,String >();
    public Map<String,String > licensePlate =  new HashMap<String,String >();


    private ListView listView;

    int totallist = 0;
    int accList = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
       // listView = (ListView) findViewById(R.id.profileListView);

        setContentView(R.layout.activity_profile);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);



    }

    @Override
    protected void onStart() {
        super.onStart();

        final globalVar gv = ((globalVar)getApplicationContext());

        String [] loginParameters = {gv.custommerID};

        Log.i("PROFILE", gv.custommerID);

        sendRequest loginRequest = new sendRequest();
        loginRequest.delegate = this;
        loginRequest.execute(loginParameters);



    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        final globalVar gv = ((globalVar)getApplicationContext());

        String [] loginParameters = {gv.custommerID};
        sendRequest loginRequest = new sendRequest();
        loginRequest.delegate = this;
        loginRequest.execute(loginParameters);*/



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
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

    public void onClickBackProfile(View v){
        Intent onthewayIntent = new Intent(getApplicationContext(),mapActivity.class);

        startActivity(onthewayIntent);
    }

    public void onClickLogout(View v) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("email", "");
        editor.putString("password", "");
        editor.putString("hasLogin", "no");
        editor.commit();
        Intent i_carent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i_carent);
    }


        public class sendRequest extends AsyncTask<String, Void, JSONArray> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONArray jj = null;
        public AsyncResponse delegate=null;
        @Override
        protected JSONArray doInBackground(String[] params1) {
            // do above Server call here
            final globalVar gv = ((globalVar)getApplicationContext());

            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://"+gv.mainHost+":1880/getTaxiTransactions");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String custommerID = "";

            if(params1.length == 1) {
                custommerID = params1[0];

            }

            Log.i("PROFILE", "https://"+gv.mainHost+":1880/getTaxiTransactions");

            nameValuePairs.add(new BasicNameValuePair("customerID", custommerID));
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
                    if(result.length() > 0) {
                        jj = result;
                        // Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http xxxxxx ", jj.toString());
                        return jj;
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
                        Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http", json.toString());
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
        protected void onPostExecute(JSONArray obj) {

            delegate.processFinishArray(obj);


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


    public class sendRequestDriverName extends AsyncTask<String, Void, JSONObject> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONObject jj = null;
        public AsyncResponse delegate=null;
        @Override
        protected JSONObject doInBackground(String[] params1) {
            // do above Server call here
            final globalVar gv = ((globalVar)getApplicationContext());

            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://"+gv.mainHost+":1880/getDriverData");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String driverID = "";


            if(params1.length == 1) {

                driverID = params1[0];

            }

           // Log.i("ID ==== ", driverID);

            nameValuePairs.add(new BasicNameValuePair("id", driverID));
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
                    if(result.length() > 0) {
                        jj = result.getJSONObject(0);
                        // Log.i("Http", response.getStatusLine().toString());
                        // Log.i("Http", jj.toString());
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
                        Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http", json.toString());
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


    @Override
    public void processFinish(JSONObject output) {
        accList++;
        try {
            driverNames.put(output.getString("id"), output.getString("firstName") + " " + output.getString("lastname"));
            licensePlate.put(output.getString("id"), output.getString("licensePlateNumber"));
        } catch (JSONException e) {
            e.printStackTrace();
        }




        if(accList == totallist){
            reloadTable();
            Log.i("Driver name = ", driverNames.toString());
        }

    }

    @Override
    public void processFinishID(JSONObject output) {

    }

    @Override
    public void loginFinish(JSONObject output) {




       // transactions
    }

    @Override
    public void processFinishString(String output, String idd) {

    }

    @Override
    public void processFinishArray(JSONArray output) {

        if(output != null) {

            Log.i("Http xxxxxx ", output.toString());


            int i;
            JSONObject tt;
            totallist = output.length();
            for (i = 0; i < output.length(); i++) {
                transactionDetail x = new transactionDetail();

                try {
                    tt = output.getJSONObject(i);

                    x.date = tt.getString("date");
                    x.driverName = "";
                    x.fare = tt.getString("fare");
                    x.time = tt.getString("time");
                    x.driverId = tt.getString("idNumber");

                    //transactionsDict.put(String.valueOf(i), x);
                    Log.i("DETAIL : ", "Date : " + x.date + " Time : " + x.time + " Fare : " + x.fare);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                transactions.add(i, x);

            }

            Log.i("GGGGGGG", transactions.toString());

            for (i = 0; i < transactions.size(); i++) {
                String[] loginParameters = {transactions.get(i).driverId};

                // Log.i("Driver ID = ", x.driverId);

                sendRequestDriverName loginRequest = new sendRequestDriverName();
                loginRequest.delegate = this;
                loginRequest.execute(loginParameters);
            }
        }


    }


    public void reloadTable(){
        //customeCell adapter = new customeCell(this, -1, arrayOfDrivers);

        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        for(int i = 0; i < transactions.size(); i++){

            transactionDetail x = transactions.get(i);
            Map<String, String> datum = new HashMap<String, String>(2);

            datum.put("title", driverNames.get(x.driverId) + " " + licensePlate.get(x.driverId));
            datum.put("subtitle", "Date : " + x.date + " Time : " + x.time + " Fare : " + x.fare);
            data.add(datum);

           // Log.i("KKKKK", data.toString());
        }



        Log.i("DETAIL : ", data.toString());
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "subtitle"},
                new int[] {android.R.id.text1,
                        android.R.id.text2});
        ListView listView1 = (ListView) findViewById(R.id.profile_list_view_custommer);

        listView1.setAdapter(adapter);
    }




}
