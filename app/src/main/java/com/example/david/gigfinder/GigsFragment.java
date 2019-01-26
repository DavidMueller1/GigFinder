package com.example.david.gigfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.example.david.gigfinder.adapters.FavAdapter;
import com.example.david.gigfinder.adapters.PastGigsAdapter;
import com.example.david.gigfinder.adapters.UpcomingGigsAdapter;
import com.example.david.gigfinder.tools.GeoTools;
import com.example.david.gigfinder.tools.Utils;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class GigsFragment extends Fragment {

    private static final String TAG = "APPLOG - GigsFragment";
    private String idToken;
    private UpcomingGigsAdapter upcomingGigsAdapter;
    private PastGigsAdapter pastGigsAdapter;

    ArrayList<JSONObject> futureEventObjects;
    ArrayList<JSONObject> pastEventObjects;

    ArrayList<String[]> futureGigs;
    ArrayList<String[]> pastGigs;

    private ListView upcomingListView;
    private ListView pastListView;
    private FrameLayout progress;

    int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gigs, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.shared_prefs), MODE_PRIVATE);

        idToken = getArguments().getString("idToken");

        upcomingListView = (ListView) getView().findViewById(R.id.upcomingGigsListView);
        pastListView = (ListView) getView().findViewById(R.id.pastGigsListView);

        futureGigs = new ArrayList<>();
        pastGigs = new ArrayList<>();

        upcomingGigsAdapter = new UpcomingGigsAdapter(this.getContext(), futureGigs);
        upcomingListView.setAdapter(upcomingGigsAdapter);

        pastGigsAdapter = new PastGigsAdapter(this.getContext(), pastGigs);
        pastListView.setAdapter(pastGigsAdapter);

        futureEventObjects = new ArrayList<>();
        pastEventObjects = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONArray(prefs.getString("userProfile","x")).getJSONObject(0);
            userId = jsonObject.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(idToken.equals("offline")) {
            offlineMode();
        }else{
            //online mode
            if(isNetworkAvailable()) {
                GetParticipations getParticipations = new GetParticipations();
                getParticipations.execute();
            }

            upcomingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), EventProfileActivity.class);
                    intent.putExtra("idToken", idToken);
                    intent.putExtra("Event", futureEventObjects.get(position).toString());
                    startActivity(intent);
                }
            });

            pastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), EventProfileActivity.class);
                    intent.putExtra("idToken", idToken);
                    intent.putExtra("Event", pastEventObjects.get(position).toString());
                    startActivity(intent);
                }
            });
        }

    }

    /**
     * Checks internet connection and starts offline mode
     */
    private void offlineMode(){
        if(isNetworkAvailable()){
            //Go back to Login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        }else{
            //Show Popup
        }
    }

    private void showGigs(String result) {
//        ArrayList<String[]> futureEvents = new ArrayList<>();
//        ArrayList<String[]> pastEvents = new ArrayList<>();


        try {
            JSONArray jsonArray = new JSONArray(result);
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject event = jsonArray.getJSONObject(i).getJSONObject("event");
                if(Utils.convertStringToTimestamp(event.getString("start")).after(new Timestamp(System.currentTimeMillis()))) {
                    // event is in the future
                    futureEventObjects.add(event);
                    Log.d(TAG, "Event in the future: " + event.toString());
                    String time = Utils.getDateStringFromServerFormat(event.getString("start")) + " um " + Utils.getTimeStringFromServerFormat(event.getString("start")) + " Uhr";
                    futureGigs.add(new String[] {event.getString("title"), time, GeoTools.getAddressFromLatLng(getContext(), new LatLng(event.getDouble("latitude"), event.getDouble("longitude"))), "loading"});
                }
                else {
                    // event is in the past
                    pastEventObjects.add(event);
                    Log.d(TAG, "Event in the past: " + event.toString());
                    pastGigs.add(new String[] {event.getString("title"), GeoTools.getAddressFromLatLng(getContext(), new LatLng(event.getDouble("latitude"), event.getDouble("longitude")))});
                }
                String[] eString = {};
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Error getting Events: " + e.getStackTrace().toString());
        }

        upcomingGigsAdapter.notifyDataSetChanged();
        pastGigsAdapter.notifyDataSetChanged();
        checkStatus();
    }

    private void checkStatus() {
        for(int i = 0; i < futureEventObjects.size(); i++) {
                try {
                    if(isNetworkAvailable()) {
                        GetEventParticipants getEventParticipants = new GetEventParticipants();
                        getEventParticipants.execute(Integer.toString(futureEventObjects.get(i).getInt("id")), Integer.toString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
    }

    private void updateStatus(int index, String result) {
        boolean someoneAccepted = false;

        try {
            JSONArray participants = new JSONArray(result);
            for(int i = 0; i < participants.length(); i++) {
                if(participants.getJSONObject(i).getBoolean("accepted")) {
                    someoneAccepted = true;
                    if(participants.getJSONObject(i).getInt("artistId") == userId) {
                        futureGigs.get(index)[3] = "accepted";
                    }
                    else {
                        futureGigs.get(index)[3] = "canceled";
                    }
                    break;
                }
            }

            if(!someoneAccepted) {
                futureGigs.get(index)[3] = "pending";
            }

            upcomingGigsAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if Network is Available
     * @return True if there is an Internet Connection
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    class GetParticipations extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL("https://gigfinder.azurewebsites.net/api/participations?artist="+userId);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Authorization", idToken);
                urlConnection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Participations By This User: " + result);
            showGigs(result);
        }
    }

    /**
     *
     */
    class GetEventParticipants extends AsyncTask<String, Void, String> {
        int index;

        @Override
        protected String doInBackground(String... params) {
            index = Integer.parseInt(params[1]);
            try {
                Log.d(TAG, params[0]);
                URL url = new URL("https://gigfinder.azurewebsites.net/api/participations?event=" + params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Authorization", idToken);
                urlConnection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "Error: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result){
            Log.d(TAG, "Participants result: " + result);
            updateStatus(index, result);
            /*if(result.equals("[]")) {

            }else{

            }*/
        }
    }
}
