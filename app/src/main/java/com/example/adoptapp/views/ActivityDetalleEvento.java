package com.example.adoptapp.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.adoptapp.R;
import com.example.adoptapp.utils.FirebaseUtils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ActivityDetalleEvento extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView imageViewFoto;
    private TextView tv_titulo;
    private EditText et_fecha;
    private EditText et_direccion;
    private EditText et_descripcion;
    private SupportMapFragment mapFragment;
    private EditText et_hora_inicio;
    private EditText et_hora_fin;

    private double latitud, longitud;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Geocoder mGeocoder;

    private GeoPoint ubicacionCliente;

    private LatLng latLng1, latLng2;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 4;
    private static final int REQUEST_CHECK_SETTINGS = 5;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private static String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    private GoogleMap mMap;
    private Polyline mLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_evento);

        imageViewFoto = findViewById(R.id.iv_evento_detalle);
        tv_titulo = findViewById(R.id.tv_titulo_evento_detalle);
        et_fecha = findViewById(R.id.et_fecha_detalle_evento);
        et_direccion = findViewById(R.id.et_direccion_evento_detalle);
        et_descripcion = findViewById(R.id.et_descripcion_evento_detalle);
        et_hora_inicio = findViewById(R.id.et_hora_inicio_detalle);
        et_hora_fin = findViewById(R.id.et_hora_fin_detalle);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_evento_detalle);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        Intent intent = getIntent();
        String titulo = intent.getStringExtra("titulo");
        final String foto_principal = intent.getStringExtra("foto_principal");
        String fecha = intent.getStringExtra("fecha");
        String hora_inicio = intent.getStringExtra("hora_inicio");
        String hora_fin = intent.getStringExtra("hora_fin");
        String descripcion = intent.getStringExtra("descripcion");
        String direccion = intent.getStringExtra("direccion");
        latitud = intent.getDoubleExtra("latitud", 0.0);
        longitud = intent.getDoubleExtra("longitud", 0.0);

        mGeocoder = new Geocoder( getBaseContext());

        ubicacionCliente = new GeoPoint(0,0);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();

        latLng2 = new LatLng(latitud, longitud);

        tv_titulo.setText(titulo);
        et_fecha.setText(fecha);
        et_direccion.setText(direccion);
        et_descripcion.setText(descripcion);
        et_hora_inicio.setText(hora_inicio);
        et_hora_fin.setText(hora_fin);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                final Location location = locationResult.getLastLocation();
                Log.i("LOCATION", "Location update in the callback: " + location);
                if (location != null) {
                    ubicacionCliente = new GeoPoint(location.getLatitude(), location.getLongitude());
                    stopLocationUpdates();

                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {

                            latLng1 = new LatLng(location.getLatitude(), location.getLongitude());
                            float zoom = 13;

                            Marker inicio = googleMap.addMarker(new MarkerOptions()
                                    .position(latLng1)
                                    .title("Tu ubicación")
                                    .icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng1, zoom));

                            Marker ubicacionReporte = googleMap.addMarker(new MarkerOptions()
                                    .position(latLng2)
                                    .title("Lugar del evento")
                                    .icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                            googleMap.getUiSettings().setCompassEnabled(true);
                            googleMap.getUiSettings().setZoomGesturesEnabled(true);
                            googleMap.getUiSettings().setZoomControlsEnabled(true);

                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng2, zoom));

                            stopLocationUpdates();

                            consumeRESTVolley();
                        }
                    });
                }
            }
        };

        new Thread(new Runnable() {
            public void run() {
                // a potentially time consuming task
                if (foto_principal != null && !foto_principal.equals("")) {
                    //descargar la imagen
                    FirebaseUtils.descargarFotoImageView(foto_principal, imageViewFoto);
                }
            }
        }).start();

        if (ContextCompat.checkSelfPermission(ActivityDetalleEvento.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            revisarActivacionGPS();
        }else{
            requestPermission(ActivityDetalleEvento.this, PERMISSIONS[0], "Acceso a GPS necesario",
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng latLng = new LatLng(4.657777, -74.093353);

        float zoom = 13; // Un zoom mayor que 13 hace que el emulador falle, pero un valor deseado para
        // callejero es 17 aprox.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //tasa de refresco en milisegundos
        mLocationRequest.setFastestInterval(1000); //máxima tasa de refresco
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void startLocationUpdates() {
        //Verificación de permiso!!
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void requestPermission(Activity context, String permiso, String justificacion, int idCode){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permiso)) {
                Toast toast = Toast.makeText(context, justificacion, Toast.LENGTH_SHORT);
                toast.show();
            }
            //Request the permission
            ActivityCompat.requestPermissions(context, new String[]{permiso}, idCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK) {
                    startLocationUpdates(); //Se encendió la localización!!!
                } else {
                    Toast.makeText(this,
                            "Sin acceso a localización, hardware deshabilitado!",
                            Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {

            case MY_PERMISSIONS_REQUEST_LOCATION:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, continue with task related to permission
                    revisarActivacionGPS();
                }else{

                    Toast.makeText(this,
                            "Sin acceso a localización, permiso denegado!",
                            Toast.LENGTH_LONG).show();
                }
            }
            break;
        }
    }

    private void revisarActivacionGPS(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationSettingsRequest.Builder builder = new
                    LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    startLocationUpdates(); //Todas las condiciones para recibir localizaciones
                }
            });

            task.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case CommonStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                            try {// Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(ActivityDetalleEvento.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sendEx) {
                                // Ignore the error.
                            } break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. No way to fix the settings so we won't show the dialog.
                            break;
                    }
                }
            });

        }else{
            Toast.makeText(this,
                    "Sin acceso a localización, permiso denegado!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void consumeRESTVolley(){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://maps.googleapis.com/maps/api/directions/json?";
        String origin = "origin="+latLng1.latitude+","+latLng1.longitude;
        String destination = "destination="+latLng2.latitude+","+latLng2.longitude;
        String mode = "mode=walking";
        String key = "key="+getResources().getString(R.string.direcciones_llave);
        StringRequest req = new StringRequest(Request.Method.GET, url+origin+"&"+destination+"&"+mode+"&"+key,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        String data = (String)response;
                        parseJSON(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("TAG", "Error handling rest invocation"+error.getCause());
                    } }
        );
        queue.add(req);
    }

    private void parseJSON(String data) {
        ArrayList<LatLng> result = new ArrayList<>();
        String distance="";
        Double d=0.0;

        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONArray steps = jsonObject.getJSONArray("routes");
            steps = steps.getJSONObject(0).getJSONArray("legs");
            d = steps.getJSONObject(0).getJSONObject("distance").getDouble("value");
            steps = steps.getJSONObject(0).getJSONArray("steps");

            result.add(new LatLng(((JSONObject)((JSONObject)steps.get(0)).get("start_location")).getDouble("lat"), ((JSONObject)((JSONObject)steps.get(0)).get("start_location")).getDouble("lng")));
            for(int i=0;i<steps.length();++i) {
                JSONObject punto = steps.getJSONObject(i);
                result.add(new LatLng(((JSONObject)punto.get("end_location")).getDouble("lat"), ((JSONObject)punto.get("end_location")).getDouble("lng")));
                Log.i("LATLNG", result.get(i).toString());
            }

            //distance = "La distancia es: " + d/1000.0 + " Km a su objetivo";
            //mMap.moveCamera(CameraUpdateFactory.zoomTo(11));

            //Toast.makeText(getApplicationContext(),  distance ,Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        drawRoute(result);
    }

    private void drawRoute(ArrayList<LatLng> result) {
        if(mLine!=null)
            mLine.remove();
        PolylineOptions line = new PolylineOptions();
        line.addAll(result);
        line.width(10);
        line.color(Color.BLUE);
        line.jointType(JointType.ROUND);
        mLine = mMap.addPolyline(line);

        //mMap.moveCamera(CameraUpdateFactory.newLatLng( midPoint() ) );
        CameraUpdateFactory.zoomBy(0.5f);
    }

}
