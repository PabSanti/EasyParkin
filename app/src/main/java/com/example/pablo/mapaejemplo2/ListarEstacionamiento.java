package com.example.pablo.mapaejemplo2;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class ListarEstacionamiento extends AppCompatActivity {

    ArrayList<HashMap<String, String>> contactList;
    ListView lv;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        contactList = new ArrayList<HashMap<String, String>>();
        lv = (ListView) findViewById(R.id.list);
        new MarkerTask().execute();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Click", "click en el elemento " + position + " de mi ListView");
                muestraDialogo(position);

            }
        });
    }

    private void muestraDialogo(final int pos){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Quiere revisar mas informacion del estacionamiento ?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(),"Gracias ",Toast.LENGTH_SHORT).show();
                        Intent in = new Intent(ListarEstacionamiento.this,Infomacion.class);
                        in.putExtra("pos",String.valueOf(pos));
                        startActivity(in);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }



    private class MarkerTask extends AsyncTask<Void, Void, String> {
        private static final String LOG_TAG = "Easy Parking";
        private static final String SERVICE_URL = "http://192.168.0.12/Ejep/ver.php";

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
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    String nombr = jsonObj.getString("nombre");
                    String califi = jsonObj.getString("calificacion");
                    HashMap<String, String> contct = new HashMap<String, String>();
                    contct.put("nombre",nombr);
                    contct.put("calificacion", califi);
                    contactList.add(contct);
                    ListAdapter adapter = new SimpleAdapter(
                            ListarEstacionamiento.this,contactList,
                            R.layout.listview_row, new String[]{"nombre","calificacion"},new int[]
                            {R.id.nomb,R.id.calificacion});
                    lv.setAdapter(adapter);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }
        }
    }
}

