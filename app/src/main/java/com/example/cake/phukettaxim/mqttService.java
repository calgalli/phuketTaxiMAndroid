package com.example.cake.phukettaxim;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;




public class mqttService extends Service {


    public String userID ="";
    public String idImage = "";
    public String selectedTaxiID = "";
    public Double selectedTaxiLat = 0.0;
    public Double selectedTaxiLon = 0.0;
    public driverModel selectedTaxiDriver;


    public String reuestedCustomer = "";
    public String customerMessage = "";





    public String gCustomerResponseTopic  = "customerResponse";
    public String gTaxiResponseTopic  = "taxiResponse";
    public String availableTopic = "available/";
    public String updateTaxiLocationTopic = "updateTaxiLocation/";
    public String removeTaxiTopic = "removeTaxi/";



    //var listTaxi = Dictionary<String, taxiLocation>()

    public Map<String,taxiLocation > listTaxi =  new HashMap<String,taxiLocation >();


    //public LatLng currentLocation;


    private static final String TAG = "MQTTService";
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private Thread thread;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;
    public String deviceId;
    private final IBinder mBinder = new mqttBinder();
    public static final String NOTIFICATION = "com.cake.android.service.receiver";
    public static final String RESULT = "result";
    public static final String TOPIC = "topic";

    private Boolean networkOK = false;

    public JSONObject userData;




    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

            for (int i = 0; i < infos.length; i++) {
                if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((infos[i].isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                } else if (infos[i].getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((infos[i].isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - " + (mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                //doConnect();
                networkOK = true;
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "doDisconnect()");
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();

                }
            }
        }
    }



    public class mqttBinder extends Binder {
        public mqttService getService() {
            return mqttService.this;
        }
    }

    @Override
    public void onCreate() {
        IntentFilter intentf = new IntentFilter();
        setClientID();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    private void setClientID() {
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wInfo = wifiManager.getConnectionInfo();
        //deviceId = "aaaaaaa";
        if (deviceId == null) {
            deviceId = MqttAsyncClient.generateClientId();
        }
    }

    public void publish(String topic, String message) {


        MqttMessage message1 = new MqttMessage();
        message1.setPayload(message.getBytes());
        try {
            mqttClient.publish(topic, message1);
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }

    public void subscribe(String topic) {
        IMqttToken token;

        try {
            token = mqttClient.subscribe(topic, 1);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }

    public  void unsubscribe(String topic){
        IMqttToken token;

        try {
            token = mqttClient.unsubscribe(topic);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }




    public void connect(){
        if(networkOK == true){
            doConnect();
        } else {
            Log.d("MQTT","Connection fail!!!!!");
        }
    }

    private void doConnect(){
        Log.d(TAG, "doConnect()");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        try {
            // mqttClient = new MqttAsyncClient("tcp://192.168.100.2:1883", deviceId, new MemoryPersistence());
            mqttClient = new MqttAsyncClient("tcp://128.199.97.22:1883", deviceId, new MemoryPersistence());
            token = mqttClient.connect();
            token.waitForCompletion(3500);
            mqttClient.setCallback(new MqttEventCallback());

            token = mqttClient.subscribe(availableTopic+userID, 0);
            token.waitForCompletion(5000);
            token = mqttClient.subscribe(updateTaxiLocationTopic+userID, 0);
            token.waitForCompletion(5000);
            token = mqttClient.subscribe(removeTaxiTopic+userID, 0);
            token.waitForCompletion(5000);



        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b"+ e.getMessage());
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");
        return START_STICKY;
    }


    private void insertTaxies(JSONObject jsonObj){

       // try {


        Iterator<?> keys = jsonObj.keys();
        Log.i("XXXX","&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            try {
                if ( jsonObj.get(key) instanceof JSONObject ) {
                    taxiLocation x = new taxiLocation();
                    Log.i("Id = ", jsonObj.get(key).toString());
                    x.id = ((JSONObject) jsonObj.get(key)).getString("id");
                    x.lat = ((JSONObject) jsonObj.get(key)).getDouble("lat");
                    x.lon = ((JSONObject) jsonObj.get(key)).getDouble("lon");
                    listTaxi.put(key,x);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        Iterator entries = listTaxi.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            //Object key = thisEntry.getKey();
            taxiLocation value = (taxiLocation) thisEntry.getValue();
            Log.i("Id = ", value.id);
            Log.i("lat = ", String.valueOf(value.lat));
            Log.i("lon = ", String.valueOf(value.lon));

            // ...
        }





       // } catch (JSONException e) {
        //    e.printStackTrace();
       // }


    }

    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {


        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic = " + topic);

            String payload =  msg.toString();
            if(topic.equals(availableTopic+userID)){

                JSONObject jsonObj = new JSONObject(payload);
                Log.i(TAG, "Aval taxi Payload = " + payload);

                insertTaxies(jsonObj);

                publishResults(topic, payload);
            } else if (topic.equals(updateTaxiLocationTopic+userID)) {

                Log.i(TAG, "Payload = " + payload);
                publishResults(topic, payload);
            } else if (topic.equals(removeTaxiTopic+userID)){

                JSONObject jsonObj = new JSONObject(payload);

                //self.removeTaxi(payload)

                //publishResults(topic, payload);
            } else if(topic.startsWith(gTaxiResponseTopic)) {
                publishResults(topic, payload);
            }




            Handler h = new Handler(getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }


    private void publishResults(String topic, String message) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, message);
        intent.putExtra(TOPIC, topic);
        sendBroadcast(intent);
    }

    public String getThread(){
        return Long.valueOf(thread.getId()).toString();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");
        //return mMessenger.getBinder();
        return mBinder;
    }




}

