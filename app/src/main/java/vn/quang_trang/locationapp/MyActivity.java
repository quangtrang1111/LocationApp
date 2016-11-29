package vn.quang_trang.locationapp;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

public class MyActivity extends ActionBarActivity {

    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    ActionBarDrawerToggle mDrawerToggle;
    String[] mDrawerListItems;

    private GoogleMap mMap;
    LatLng latLngStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer);
        mDrawerList = (ListView)findViewById(android.R.id.list);
        mDrawerListItems = getResources().getStringArray(R.array.drawer_list);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDrawerListItems));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doFunctions(position);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close){
            public void onDrawerClosed(View v){
                super.onDrawerClosed(v);
                invalidateOptionsMenu();
                syncState();
            }
            public void onDrawerOpened(View v){
                super.onDrawerOpened(v);
                invalidateOptionsMenu();
                syncState();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();

        setUpMapIfNeeded();
    }

    private void doFunctions(int position) {

        switch (position) {
            case 0:
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        mMap.addMarker(new MarkerOptions()
                                .draggable(true)
                                .position(latLng)
                                .title("Hello!")
                                .alpha(0.8f).snippet("Your can draw your marker on map!")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    }
                });
                break;

            case 1:
                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(Marker marker) {
                        marker.hideInfoWindow();
                    }

                    @Override
                    public void onMarkerDrag(Marker marker) {

                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {
                        marker.setSnippet("Latitude = " + marker.getPosition().latitude + " - Longitude = " + marker.getPosition().latitude);
                        marker.showInfoWindow();
                    }
                });

                break;

            case 2:
                mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng latLng) {

                        AsyncTask<LatLng, Void, String> task = new getAddressFromInternet();
                        task.execute(latLng);
                    }
                });

                break;

            case 3:
                mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        mMap.clear();
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                        mMap.addMarker(new MarkerOptions().title("Your location").position(latLng).snippet("Latitude = " + latLng.latitude + " - Longitude = " + latLng.latitude)).showInfoWindow();
                    }
                });
                break;

            case 4:
                latLngStart = null;
                mMap.clear();
                mMap.setOnMapClickListener(null);
                mMap.setOnMarkerDragListener(null);
                mMap.setOnMapLongClickListener(null);
                mMap.setOnMyLocationChangeListener(null);
                Toast.makeText(MyActivity.this, "Your map was reseted!", Toast.LENGTH_SHORT).show();
                break;

            case 5:
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        if (latLngStart == null) {
                            mMap.addMarker(new MarkerOptions().title("Start").position(latLng)).showInfoWindow();
                            latLngStart = latLng;
                        } else {
                            mMap.addMarker(new MarkerOptions().title("End").position(latLng)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))).showInfoWindow();
                            DirectionTask parserTask = new DirectionTask(latLngStart, latLng, mMap);
                            parserTask.execute();
                        }
                    }
                });
                break;

            case 6:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE);
                startActivityForResult(intent, PICK_CONTACT);
                break;
        }
    }

    private String getAddressText(Address add) {
        String text = "";

        if (add.getSubThoroughfare() != null)
            text += add.getSubThoroughfare() + ", ";

        if (add.getThoroughfare() != null)
            text += add.getThoroughfare() + ", ";

        if (add.getSubLocality() != null)
            text += "Phường " + add.getSubLocality() + ", ";

        if (add.getLocality() != null)
            text += "Quận " + add.getLocality() + ", ";

        if (add.getSubAdminArea() != null)
            text += add.getSubAdminArea() + ", ";

        if (add.getAdminArea() != null)
            text += add.getAdminArea() + ", ";

        if (add.getCountryName() != null)
            text += add.getCountryName();

        return  text;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.getUiSettings().setZoomControlsEnabled(true);
                mMap.setMyLocationEnabled(true);
                //Location loca = mMap.getMyLocation();// remember setMyLocationEnabled(true)
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: {
                if (mDrawerLayout.isDrawerOpen(mDrawerList)){
                    mDrawerLayout.closeDrawer(mDrawerList);
                } else {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    private final int PICK_CONTACT = 1;

    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT): {
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        try {
                            String contactAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));

                            if (contactAddress != null)
                            {
                                try {
                                    Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                                    List<Address> addresses = geo.getFromLocationName(contactAddress, 1);
                                    if (addresses.isEmpty()) {
                                        //"Waiting for Location";
                                    }
                                    else {
                                        if (addresses.size() > 0) {
                                            LatLng current = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                                            LatLng des = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                                            mMap.clear();
                                            mMap.addMarker(new MarkerOptions().position(des).title("Your contact here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                            DirectionTask parserTask = new DirectionTask(current, des, mMap);
                                            parserTask.execute();
                                        }
                                    }
                                }
                                catch (Exception e) {
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(MyActivity.this, "The contract have no address", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    private class getAddressFromInternet extends AsyncTask<LatLng, Void, String> {
        LatLng latLng = null;

        @Override
        protected String doInBackground(LatLng... params) {
            latLng = params[0];
            try {
                Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = geo.getFromLocation(latLng.latitude, latLng.longitude, 1);

                if (addresses.size() > 0) {
                    return getAddressText(addresses.get(0));
                }
            } catch (Exception e) {
            }

            return "Can't get address!";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Your address").snippet(s)).showInfoWindow();
        }
    };
}
