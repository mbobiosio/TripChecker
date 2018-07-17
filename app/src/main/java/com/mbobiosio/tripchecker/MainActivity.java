package com.mbobiosio.tripchecker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.mbobiosio.tripchecker.activities.BaseActivity;
import com.mbobiosio.tripchecker.adapter.PlacesAdapter;
import com.mbobiosio.tripchecker.controller.DirectionFinder;
import com.mbobiosio.tripchecker.controller.Route;
import com.mbobiosio.tripchecker.interfaces.DirectionFinderListener;
import com.mbobiosio.tripchecker.utils.Constants;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Mbuodile Obiosio on Jul 15, 18.
 * cazewonder@gmail.com
 */

@SuppressWarnings("deprecation")
public class MainActivity extends BaseActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, DirectionFinderListener {


    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();

    @Bind(R.id.location)
    AutoCompleteTextView mLocation;
    @Bind(R.id.destination)
    AutoCompleteTextView mDestination;
    @Bind(R.id.search_path)
    FloatingActionButton mSearch;
    @Bind(R.id.distance)
    TextView mDistance;
    @Bind(R.id.time)
    TextView mTime;
    @Bind(R.id.detail_card)
    CardView mDetailCard;

    String location;
    String destination;
    SupportMapFragment mMapFragment;
    Animation mSlideUp, mFadeIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mMapFragment.getMapAsync(this);

        mSearch.setOnClickListener(v -> {
            doSearch();
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        mLocation.setAdapter(new PlacesAdapter(this, android.R.layout.simple_dropdown_item_1line));
        mLocation.setOnItemClickListener((parent, view, position, id) -> {
            String string = (String) parent.getItemAtPosition(position);
            showToast(string);
        });

        mDestination.setAdapter(new PlacesAdapter(this, android.R.layout.simple_dropdown_item_1line));
        mDestination.setOnItemClickListener((parent, view, position, id) -> {
            String string = (String) parent.getItemAtPosition(position);
            showToast(string);
        });

        mFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.setInterpolator(new AccelerateInterpolator());
        animationSet.addAnimation(mSlideUp);
        animationSet.addAnimation(mFadeIn);
        mDetailCard.startAnimation(animationSet);
    }

    public void doSearch() {
        location = mLocation.getText().toString();
        destination = mDestination.getText().toString();
        if (location.isEmpty()) {
            showToast(getString(R.string.location_missing));
            return;
        }
        if (destination.isEmpty()) {
            showToast(getString(R.string.destination_missing));
            return;
        }

        try {
            new DirectionFinder(this, location, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapFragment.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildApiClient();
                mMap.setMyLocationEnabled(true);
            } else {
                doPermissionCheck();
            }
        } else {
            buildApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(getString(R.string.your_location));
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11.5f));

        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onDirectionFinderStart() {
        showProgress(getString(R.string.finding_route_message));

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        hideProgress();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 10));
            mDetailCard.setVisibility(View.VISIBLE);
            mDistance.setText(route.distance.text);
            mTime.setText(route.duration.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(vectorToBitmap(R.drawable.ic_add_location, ContextCompat.getColor(this, R.color.colorPrimary)))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(
                    mMap.addMarker(new MarkerOptions()
                            .icon(vectorToBitmap(R.drawable.ic_location_on, ContextCompat.getColor(this, R.color.red)))
                            .title(route.endAddress)
                            .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    showToast(getString(R.string.permission_denied));
                }
                return;
            }
        }
    }
}
