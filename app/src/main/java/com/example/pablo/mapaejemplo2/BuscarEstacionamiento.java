package com.example.pablo.mapaejemplo2;

import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.Manifest;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BuscarEstacionamiento extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap mapaB;
    private Marker marcador;
    private LocationManager mLocMgr;
    private Location location;
    double lati;
    double longi;
    private static final String TAG = "BuscarEstacionamiento";
    //Minimo tiempo para updates en Milisegundos
    private static final long MIN_CAMBIO_DISTANCIA_PARA_UPDATES = 10; // 10 metros
    //Minimo tiempo para updates en Milisegundos
    private static final long MIN_TIEMPO_ENTRE_UPDATES = 1000 * 20 * 1; // 20 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapa_buscar);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapbuscar);
        mapFragment.getMapAsync(this);

        mLocMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Requiere permisos para Android 6.0
            Log.e(TAG, "No se tienen permisos necesarios!, se requieren.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 225);
            return;
        } else {
            Log.i(TAG, "Permisos necesarios OK!.");
            mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIEMPO_ENTRE_UPDATES, MIN_CAMBIO_DISTANCIA_PARA_UPDATES, locListener, Looper.getMainLooper());
        }

    }

    public LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            LatLng santiago = new LatLng(-33.4724727,-70.9100196);
            mapaB.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            mapaB.moveCamera(CameraUpdateFactory.newLatLngZoom(santiago,10));

            //lati = location.getLatitude();
            //longi = location.getLongitude();
            lati = -33.43469177849155;
            longi = -70.62897715471804;
            LatLng coordena = new LatLng(lati, longi);
            CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordena, 15);
            mapaB.animateCamera(miUbicacion);

            mapaB.addMarker(new MarkerOptions().position(coordena).icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));

            CircleOptions circleOptions = new CircleOptions()
                    .center(coordena)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(0xFFFF00)
                    .radius(500); //en metros
            mapaB.addCircle(circleOptions);

            final Circle circle = mapaB.addCircle(new CircleOptions().center(coordena).strokeColor(Color.YELLOW));
            ValueAnimator vAnimator = new ValueAnimator();
            vAnimator.setRepeatCount(ValueAnimator.INFINITE);
            vAnimator.setRepeatMode(ValueAnimator.RESTART);  /* PULSE */
            vAnimator.setIntValues(0, 500);
            vAnimator.setDuration(1000);
            vAnimator.setEvaluator(new IntEvaluator());
            vAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            vAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float animatedFraction = valueAnimator.getAnimatedFraction();
                    circle.setRadius(animatedFraction * 500);
                }
            });
            vAnimator.start();

            new MarkerTask().execute();
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };
    @Override
    public void onMapReady(GoogleMap map) {
        mapaB = map;
    }

    private class MarkerTask extends AsyncTask<Void, Void, String> {
        private static final String LOG_TAG = "Easy Parking";
        private static final String SERVICE_URL = "http://192.168.0.12/Ejep/ver.php";
        // Duoc http://10.20.27.30/Ejep/ver.php"

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
            double destiLong;
            double destiLati;
            double iniciLong = lati;
            double iniciLati = longi;
            int cont = 0;
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    Object dis = jsonObj.get("disponibilidad");
                    destiLati = jsonObj.getDouble("lati");
                    destiLong = jsonObj.getDouble("long");
                    float[] result = new float[1];
                    location.distanceBetween(iniciLati, iniciLong, destiLati, destiLong, result);
                    if (result[0] <= 500) {
                        mapaB.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                .title(jsonObj.getString("nombre"))
                                .snippet("Disponibilidad: " + dis)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.estacionamientoicn))
                                .position(new LatLng(jsonObj.getDouble("long"), jsonObj.getDouble("lati"))));
                        cont++;
                    }
                }
                if(cont != 0)
                {
                    Toast.makeText(BuscarEstacionamiento.this, "Tiene " + cont + " estacionamientos mas cercanos", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(BuscarEstacionamiento.this, "No tiene estacionamientos cercanos", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }
        }
    }
}
