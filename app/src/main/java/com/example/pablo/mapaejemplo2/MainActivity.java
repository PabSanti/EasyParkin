package com.example.pablo.mapaejemplo2;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



import Mapa.Fragments.MainFragment;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback ,GoogleApiClient.OnConnectionFailedListener ,View.OnClickListener{

    private GoogleMap mMap;
    private Marker marcador;
    double lat = 0.0;
    double lon = 0.0;
    LocationManager locationManager;
    Location location;
    LocationListener locationListener;
    AlertDialog alert = null;

    SupportMapFragment sMapFragment;

    double mSourceLatLng;
    double mSourceLatLng2;
    Polyline line;
    TextView tvDistanceDuration;
    //
    private static final String TAG = "GoogleSignIn";
    private static final int RC_SIGN_IN = 9001;
    private final Context mContext = this;
    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView, mUserTextView, mEmailTextView;
    private CircleImageView mProfileImageView;
    private ProgressDialog mProgressDialog;
    String nombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sMapFragment = SupportMapFragment.newInstance();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //
        tvDistanceDuration = (TextView) findViewById(R.id.tv_distance_time);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        sMapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertNoGps();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return;
        }

        MostrarLocalizacion(location);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                MostrarLocalizacion(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
                MostrarLocalizacion(null);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);

        View headerLayout = navigationView.inflateHeaderView(R.layout.nav_header_main);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mUserTextView = (TextView) headerLayout.findViewById(R.id.userName);
        mEmailTextView = (TextView) headerLayout.findViewById(R.id.email);
        mProfileImageView = (CircleImageView) headerLayout.findViewById(R.id.profileImage);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        // FOR PROFILE PICTURE:
        // Ref: https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInAccount.html#getPhotoUrl%28%29
        // getPhotoUrl(): Gets the photo url of the signed in user.
        // Only non-null if requestProfile() is configured and user does have a Google+ profile picture.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.PLUS_LOGIN))
                .requestEmail()
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this , this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(Plus.API)
                .build();
        // [END build_client]

        // [START customize_button]
        // Customize sign-in button. The sign-in button can be displayed in
        // multiple sizes and color schemes. It can also be contextually
        // rendered based on the requested scopes. For example. a red button may
        // be displayed when Google+ scopes are requested, but a white button
        // may be displayed when only basic profile is requested. Try adding the
        // Scopes.PLUS_LOGIN scope to the GoogleSignInOptions to see the
        // difference.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());
        // [END customize_button]
    }

    private void AlertNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alert != null) {
            alert.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                locationManager.removeUpdates(locationListener);
            }
        } else {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            // G+
            if (mGoogleApiClient.hasConnectedApi(Plus.API)) {
                Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                if (person != null) {
                    Log.i(TAG, "--------------------------------");
                    Log.i(TAG, "Display Name: " + person.getDisplayName());
                    Log.i(TAG, "Gender: " + person.getGender());
                    Log.i(TAG, "Current Location: " + person.getCurrentLocation());
                    Log.i(TAG, "Language: " + person.getLanguage());
                } else {
                    Log.e(TAG, "Error!");
                }
            } else {
                Log.e(TAG, "Google+ not connected");
            }
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            nombre = getString(R.string.signed_in_fmt, acct.getDisplayName());
            // Views inside NavigationView's header
            mUserTextView.setText(acct.getDisplayName());
            mEmailTextView.setText(acct.getEmail());
            Uri uri = acct.getPhotoUrl();
            Picasso.with(mContext)
                    .load(uri)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(mProfileImageView);
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }


    public void MostrarLocalizacion(Location loc) {
        if (loc != null) {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            agregarMarcador(lat, lon);
            mSourceLatLng = lat;
            mSourceLatLng2 = lon;

        }
    }

    public void agregarMarcador(double lat, double lon) {
        LatLng coordenadas = new LatLng(lat, lon);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 16);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                .title("Ubicacion Actual"));
        new MarkerTask().execute();
        mMap.animateCamera(miUbicacion);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        FragmentManager fm = getFragmentManager();
        android.support.v4.app.FragmentManager sFm = getSupportFragmentManager();

        int id = item.getItemId();

        if (sMapFragment.isAdded())
            sFm.beginTransaction().hide(sMapFragment).commit();

        if (id == R.id.nav_inicio) {
            tvDistanceDuration.setText("");
            mStatusTextView.setText(nombre);
            updateUI(false);
            fm.beginTransaction().replace(R.id.content_frame, new MainFragment()).commit();

        } else if (id == R.id.nav_ruta) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.disconnect_button).setVisibility(View.GONE);
            mStatusTextView.setText("");
            if (!sMapFragment.isAdded())
                sFm.beginTransaction().add(R.id.maps, sMapFragment).commit();
            else
                sFm.beginTransaction().show(sMapFragment).commit();
        } else if (id == R.id.nav_buscar) {
            tvDistanceDuration.setText("");
            Intent intent = new Intent(getApplicationContext(),BuscarEstacionamiento.class);
            startActivity(intent);
        } else if (id == R.id.nav_listar) {
            tvDistanceDuration.setText("");
            Intent intent = new Intent(getApplicationContext(),ListarEstacionamiento.class);
            startActivity(intent);

        } else if (id == R.id.nav_facebook) {
            tvDistanceDuration.setText("");
            linkface();
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.disconnect_button).setVisibility(View.GONE);
            mStatusTextView.setText("");
        } else if (id == R.id.nav_send) {
            tvDistanceDuration.setText("");
            sedmail();
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.disconnect_button).setVisibility(View.GONE);
            mStatusTextView.setText("");
        }

        item.setCheckable(true);
        getSupportActionBar().setTitle(item.getTitle());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // "-33.4569400","-70.6482700"
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        miUbicacion();
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String url = makeURL(mSourceLatLng, mSourceLatLng2, marker.getPosition().latitude, marker.getPosition().longitude, "driving");
                new connectAsyncTask(url).execute();
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (line != null) {
                    line.remove();
                    Toast.makeText(MainActivity.this, "Ruta Eliminada", Toast.LENGTH_SHORT).show();
                    tvDistanceDuration.setText("Seleccione Estacionamiento");
                }
            }
        });
    }

    private void miUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        MostrarLocalizacion(location);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }

    // [START signIn]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Intent intent = new Intent(getApplicationContext(),BuscarEstacionamiento.class);
                        startActivity(intent);
                        updateUI(true);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        mUserTextView.setText("Usuario");
                        mEmailTextView.setText("Correo");
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

    }
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }
    ///

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
                    Object dis = jsonObj.get("disponibilidad");
                    mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                            .title(jsonObj.getString("nombre"))
                            .snippet("Disponibilidad: " + dis)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.estacionamientoicn))
                            .position(new LatLng(jsonObj.getDouble("long"), jsonObj.getDouble("lati"))));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing JSON", e);
            }
        }
    }

    private String makeURL (double sourcelat, double sourcelog, double destlat, double destlog,String mode){
        StringBuilder urlString = new StringBuilder();

        if(mode == null)
            mode = "driving";

        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode="+mode+"&alternatives=true&language=es");
        return urlString.toString();
    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }
        return poly;
    }


    private class connectAsyncTask extends AsyncTask<Void, Void, String>{
        private ProgressDialog progressDialog;
        String url;
        connectAsyncTask(String urlPass){
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Marcando Ruta,Por favor espere...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.hide();
            if(result!=null){
                drawPath(result);
            }
        }
    }

    public void drawPath(String  result) {
        try {
            String parsedDistance;
            String parsedDuration;
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONArray legs = routes.getJSONArray("legs");
            JSONObject steps = legs.getJSONObject(0);
            JSONObject distance = steps.getJSONObject("distance");
            parsedDistance = distance.getString("text");
            JSONObject duration = steps.getJSONObject("duration");
            parsedDuration = duration.getString("text");
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
                line = mMap.addPolyline(new PolylineOptions()
                                .addAll(list)
                                .width(12)
                                .color(Color.parseColor("#05b1fb"))//Google maps blue color
                                .geodesic(true)
                );
            //Toast.makeText(this,String.valueOf("Distancia a recorrer: "+parsedDistance+" "+"Tiempo estimado : "+parsedDuration),Toast.LENGTH_SHORT).show();
            tvDistanceDuration.setText("Distancia a recorrer: "+parsedDistance+" "+"\nTiempo estimado: "+parsedDuration);
        }
        catch (JSONException e) {
        }
    }

    public void sedmail()
    {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"easyparkinginbox@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Asunto");
        String textoApp = "Envio un email desde mi App de Easy Parking \n";
        emailIntent.putExtra(Intent.EXTRA_TEXT, textoApp);
        emailIntent.setType("message/rfc822");

        startActivity(Intent.createChooser(emailIntent, "Selecciona aplicación..."));
    }
    public void linkface()
    {
        Intent getIntent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.cl"));
        startActivity(getIntent);
    }

}