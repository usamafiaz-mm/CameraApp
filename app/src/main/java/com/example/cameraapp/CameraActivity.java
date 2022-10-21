package com.example.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Camera mCamera;
    private CameraPreview mPreview;
    private FusedLocationProviderClient fusedLocationClient;
    private LinearLayout cameraUtils, addressLayout;
    ImageView retryButton, doneButton;
    private GoogleMap mMap;
    Double lat, lon;
    RelativeLayout relativeLayout;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraUtils = findViewById(R.id.cameraUtilBtns);
        retryButton = findViewById(R.id.ivRefresh);
        doneButton = findViewById(R.id.ivCheck);
        relativeLayout = findViewById(R.id.screenShot);
        addressLayout = findViewById(R.id.addressLayout);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mCamera = getCameraInstance();


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera, cameraUtils, retryButton, doneButton, getWindow(), addressLayout);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        } else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationToken() {

                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return null;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            }).addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (location != null) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                                ((TextView) findViewById(R.id.lon)).setText("Longitude : " + location.getLongitude());
                                ((TextView) findViewById(R.id.lat)).setText("Langitude : " + location.getLatitude());
                                Geocoder geocoder;
                                List<Address> addresses;
                                geocoder = new Geocoder(CameraActivity.this, Locale.getDefault());

                                try {
                                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                    String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                                    String city = addresses.get(0).getLocality();
                                    String country = addresses.get(0).getCountryName();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) findViewById(R.id.tvAddress)).setText("Address : " + address);
                                            ((TextView) findViewById(R.id.tvCity)).setText("City : " + city);
                                            ((TextView) findViewById(R.id.tvCountry)).setText("Country : " + country);
                                            if (mMap != null) {
                                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                                mMap.addMarker(new MarkerOptions()
                                                        .position(currentLocation)
                                                        .title("Marker in Sydney"));
                                                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                                                mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

                                            }
                                        }
                                    });

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    });

                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);






        mMap.getUiSettings().setCompassEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

//        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
//
////        LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
////        mMap.addMarker(new MarkerOptions()
////                .position(sydney)
////                .title("Marker in Sydney"));
////        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
////        mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );


    }
}
