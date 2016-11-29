package vn.quang_trang.locationapp;

/**
 * Created by quangtrang on 07/04/2015.
 */

import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** A class to parse the Google Directions in JSON format */
public class DirectionTask extends
        AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

    private LatLng origin;
    private LatLng dest;
    private GoogleMap mMap;

    public  DirectionTask(LatLng originLocation, LatLng destLocation, GoogleMap map) {
        origin = originLocation;
        dest = destLocation;
        mMap = map;
    }

    // Parsing the data in non-ui thread
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(
            String... jsonData) {

        DirectionJSONParser parser = new DirectionJSONParser();
        List<List<HashMap<String, String>>> routes = null;
        String json;

        try {
            json = parser.downloadJSON(origin, dest);
        } catch (Exception e) {
            return routes;
        }

        try {
            JSONObject jObject = new JSONObject(json);

            // Starts parsing data
            routes = parser.parse(jObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routes;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> result) {
        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;

        try {
            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(5);
                lineOptions.color(Color.RED);
            }
        } catch (Exception e) {
            return;
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            mMap.addPolyline(lineOptions);
        }
    }
}