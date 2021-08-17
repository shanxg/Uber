package com.lucasrivaldo.cloneuber.helper.maps_helpers;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCE;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DISTANCEv;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATION;
import static com.lucasrivaldo.cloneuber.helper.maps_helpers.MapDirections.DURATIONv;

public class DataParser {

    private boolean isForDirections;

    public DataParser(boolean isForDirections) {
        this.isForDirections = isForDirections;
    }

    public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

        List<List<HashMap<String, String>>> routes = new ArrayList<>();



        JSONArray iRoutes;
        JSONArray jLegs;
        JSONArray kSteps;
        try {
            iRoutes = jObject.getJSONArray("routes");


            /** Traversing all routes */
            for (int i = 0; i < iRoutes.length(); i++) {
                jLegs = ((JSONObject) iRoutes.get(i)).getJSONArray("legs");
                List path = new ArrayList<>();

                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    kSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    //IF IS FOR DIRECTIONS RETURN POLYLINE

                    if(isForDirections){
                        /** Traversing all steps */
                        for (int k = 0; k < kSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject)
                                    ((JSONObject) kSteps.get(k))
                                            .get("polyline"))
                                                .get("points");

                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude));
                                hm.put("lng", Double.toString((list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }else{
                        List<HashMap<String, String>> route = new ArrayList<>();

                        HashMap<String, String> routeTextMap = new HashMap<>();

                        JSONObject distance = (JSONObject) ((JSONObject) jLegs.get(j)).get("distance");
                        JSONObject duration = (JSONObject) ((JSONObject) jLegs.get(j)).get("duration");

                        routeTextMap.put(DISTANCE, distance.getString("text"));
                        routeTextMap.put(DISTANCEv, String.valueOf(distance.getInt("value")));

                        routeTextMap.put(DURATION, duration.getString("text"));
                        routeTextMap.put(DURATIONv, String.valueOf(duration.getInt("value")));

                        route.add(routeTextMap);

                        routes.add(route);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return routes;
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
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

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
