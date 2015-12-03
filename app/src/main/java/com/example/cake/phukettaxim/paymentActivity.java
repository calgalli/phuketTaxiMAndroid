package com.example.cake.phukettaxim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;



public class paymentActivity extends ActionBarActivity implements AsyncResponse {

    private RatingBar ratingBar;
    private String rr;
    String fare;
    String isCash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Intent thisInt = getIntent();
        fare = thisInt.getStringExtra(onthewayActivity.fareString);
        isCash = thisInt.getStringExtra(onthewayActivity.isCash);

        Log.i("Payment", fare + " " + isCash);

        TextView fareLabel = (TextView) findViewById(R.id.fareLabel);
        fareLabel.setText(fare);

        Button paymentButton = (Button) findViewById(R.id.paymentButton);
        if(isCash.equals("yes")) {
            paymentButton.setText("Pay by cash");
        } else {
            paymentButton.setText("Pay by cradit card");
        }

         addListenerOnRatingBar();

    }

    public void onPaymentClick(View v){
        EditText tip = (EditText) findViewById(R.id.tipText);
        EditText comment = (EditText) findViewById(R.id.commentText);

        Log.i("PAYMENT", "Fare = " + fare  + " tip = " + tip.getText() + " rating = " + rr + " comment = " + comment.getText());

        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Calendar.getInstance().getTime());
        DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        String time = df2.format(Calendar.getInstance().getTime());

        JSONObject data = new JSONObject();
        final globalVar gv = ((globalVar) getApplicationContext());

        try {
            data.put("idNumber",gv.taxiID);
            data.put("customerID", gv.custommerID);
            data.put("fare",fare);
            data.put("tip", tip.getText());
            data.put("rating", rr);
            data.put("comment", comment.getText());
            data.put("date", date);
            data.put("time", time);
        } catch (JSONException e) {
            e.printStackTrace();
        }




        String [] loginParameters = {gv.taxiID,fare, tip.getText().toString(), gv.custommerID, date, time, rr, comment.getText().toString()};
        sendRequest tranRequest = new sendRequest();
        tranRequest.delegate = this;
        tranRequest.execute(loginParameters);

        if(isCash.equals("yes")){
            new AlertDialog.Builder(this)
                    .setTitle("Payment complete")
                    .setMessage("Thank you")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {



                            final Intent mapViewIntent = new Intent(getApplicationContext(), mapActivity.class);

                            startActivity(mapViewIntent);

                        }
                    }).show();
        } else {

            final Omise omise = new Omise();
            try {
                // Instantiate new TokenRequest with public key and card.
                Card card = new Card();
                card.setName(gv.cardName); // Required
                card.setCity(gv.cardCity); // Required
                card.setPostalCode(gv.cardPostalCode); // Required
                card.setNumber(gv.cardNumber); // Required
                card.setExpirationMonth(gv.cardExpirationMonth); // Required
                card.setExpirationYear(gv.cardExpirationYear); // Required
                card.setSecurityCode(gv.cardSecurityCode); // Required

                TokenRequest tokenRequest = new TokenRequest();
                tokenRequest.setPublicKey(gv.omiseToken); // Required
                tokenRequest.setCard(card);

                // Requesting token.
                omise.requestToken(tokenRequest, new RequestTokenCallback() {
                    @Override
                    public void onRequestSucceeded(Token token) {
                        //Your code here
                        //Ex.
                        String strToken = token.getId();
                        boolean livemode = token.isLivemode();

                        Log.i("TOKEN",strToken);



                        String payUrl = "http://"+ gv.mainHost + ":3000/pay?";

                        String pp = "token="+strToken+"&" + "amount="+fare+"00" + "&" + "email=";
                        try {
                            pp  = pp   + URLEncoder.encode(gv.cardEmail, "UTF-8") + "&" + "desc=" + "kkkkk";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }


                           // String kk = URLEncoder.encode(pp, "UTF-8");
                            HttpGet httpGet = new HttpGet(payUrl+pp);

                            Log.i("OMISE", payUrl + pp);

                            try {
                                StringBuilder builder = new StringBuilder();
                                HttpClient client = new DefaultHttpClient();

                                HttpResponse response = client.execute(httpGet);
                                StatusLine statusLine = response.getStatusLine();
                                Log.i("SSSSS", response.toString());
                                int statusCode = statusLine.getStatusCode();
                                if (statusCode == 200) {
                                    HttpEntity entity = response.getEntity();
                                    InputStream content = entity.getContent();
                                    BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(content));
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        builder.append(line);
                                    }

                                    Log.i("PAY OK", line);




                                    Log.e("OMISE", "Paid ok");
                                } else {
                                    Log.e("OMISE", "Failed to pay");
                                }
                            } catch (ClientProtocolException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }




                    }

                    @Override
                    public void onRequestFailed(final int errorCode) {
                        Log.i("TOKEN","Fucking fail!!!!");
                    }
                });
            } catch (OmiseException e) {
                e.printStackTrace();
            }



        }

        new AlertDialog.Builder(this)
                .setTitle("Payment complete")
                .setMessage("Thank you")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {



                        final Intent mapViewIntent = new Intent(getApplicationContext(), mapActivity.class);

                        startActivity(mapViewIntent);

                    }
                }).show();


    }


    public void addListenerOnRatingBar() {

        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //if rating value is changed,
        //display the current rating value in the result (textview) automatically
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {


                rr = String.valueOf(rating);
            }
        });
    }

    @Override
    public void processFinish(JSONObject output) {

    }

    @Override
    public void processFinishID(JSONObject output) {

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
            HttpPost get = new HttpPost("https://128.199.97.22:1880/updateTransaction");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
            String idNumber = "";
            String fare = "";
            String tip = "";
            String customerID ="";
            String date ="";
            String time = "";
            String rating = "";
            String comment = "";
            if(params1.length == 8) {
                idNumber = params1[0];
                fare = params1[1];
                tip = params1[2];
                customerID = params1[3];
                date = params1[4];
                time = params1[5];
                rating = params1[6];
                comment = params1[7];
            }




            nameValuePairs.add(new BasicNameValuePair("idNumber", idNumber));
            nameValuePairs.add(new BasicNameValuePair("fare", fare));
            nameValuePairs.add(new BasicNameValuePair("tip", tip));
            nameValuePairs.add(new BasicNameValuePair("customerID", customerID));
            nameValuePairs.add(new BasicNameValuePair("date", date));
            nameValuePairs.add(new BasicNameValuePair("time", time));
            nameValuePairs.add(new BasicNameValuePair("rating", rating));
            nameValuePairs.add(new BasicNameValuePair("comment", comment));
            try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                Log.i("Http", get.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);
                Log.i("Http",response.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }
            String json = null;

            try {
                json = EntityUtils.toString(response.getEntity());
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // String retSrc = EntityUtils.toString(entity);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return jj;
        }

        @Override
        protected void onPostExecute(JSONObject obj) {
            //Done go back to map view
            delegate.loginFinish(obj);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_payment, menu);
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
}
