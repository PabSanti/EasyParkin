package com.example.pablo.mapaejemplo2;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Infomacion extends Activity {
    TextView passedView;
    int lugar;
    RatingBar R1;
    Button RatingBarCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informacion);
        String datapos = getIntent().getStringExtra("pos");
        passedView = (TextView) findViewById(R.id.passed);
        passedView.setText("Codigo: " + datapos);
        lugar = Integer.parseInt(datapos);
        new MarkerTask().execute();

        R1 = (RatingBar)findViewById(R.id.ratingBar);
        RatingBarCount = (Button)findViewById(R.id.button1);
        RatingBarCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Infomacion.this,"Calificacion: "+ String.valueOf(R1.getRating()), Toast.LENGTH_SHORT).show();
            }
        });
    }
        private class MarkerTask extends AsyncTask<Void, Void, String> {
            private static final String LOG_TAG = "Easy Parking";
            private static final String SERVICE_URL = "http://10.20.27.30/Ejep/ver.php";

            @Override
            protected String doInBackground(Void... params) {
                HttpURLConnection cnn = null;
                final StringBuilder json = new StringBuilder();
                try {
                    URL url = new URL(SERVICE_URL);
                    cnn = (HttpURLConnection) url.openConnection();
                    InputStreamReader in = new InputStreamReader(cnn.getInputStream());
                    int read;
                    char[] buff = new char[1024];
                    while ((read = in.read(buff)) != -1) {
                        json.append(buff, 0, read);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
                } finally {
                    if (cnn != null) cnn.disconnect();
                }
                return json.toString();
            }

            @Override
            protected void onPostExecute(String json) {
                int posis = lugar;
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    JSONObject jsonObj = jsonArray.getJSONObject(posis);
                    String nombr = jsonObj.getString("nombre");
                    String dispon = jsonObj.getString("disponibilidad");
                    float calif = Float.parseFloat(jsonObj.getString("calificacion"));

                    passedView = (TextView) findViewById(R.id.nombreInfo);
                    passedView.setText("Nombre: " + nombr);

                    passedView = (TextView) findViewById(R.id.dispon);
                    passedView.setText("Disponibilida: " + dispon);

                    R1.setRating(calif);


                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error processing JSON", e);
                }
            }
        }
    }
