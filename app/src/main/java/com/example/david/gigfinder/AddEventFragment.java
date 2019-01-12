package com.example.david.gigfinder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.david.gigfinder.data.Host;
import com.example.david.gigfinder.data.enums.Genre;
import com.example.david.gigfinder.tools.ColorTools;
import com.example.david.gigfinder.tools.ImageTools;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class AddEventFragment extends Fragment {
    private static final String TAG = "AddEventFragment";
    private static final int PICK_IMAGE = 1;
    private static final int PLACE_PICKER_REQUEST = 2;

    private EditText nameField;
    private EditText descriptionField;
    private LinearLayout locationButton;
    private TextView locationButtonText;
    private Spinner genreSpinner;
    private TextView timeFromText;
    private Button pickTimeFromButton;
    private TextView dateFromText;
    private Button pickDateFromButton;
    private TextView timeToText;
    private Button pickTimeToButton;
    private TextView dateToText;
    private Button pickDateToButton;
    private Button addEventButton;

    private LatLng position;

    String idToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_add, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        nameField = getView().findViewById(R.id.add_event_title);
        descriptionField = getView().findViewById(R.id.add_event_description);
        locationButton = getView().findViewById(R.id.add_event_location_container);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLocationSelection();
            }
        });
        locationButtonText = getView().findViewById(R.id.add_event_location_text);
        genreSpinner = getView().findViewById(R.id.add_event_genre);
        //Replaced the Strings with the Genre-Enum
        ArrayAdapter<Genre> adapter = new ArrayAdapter<Genre>(getContext(), android.R.layout.simple_spinner_dropdown_item, Genre.values());
        genreSpinner.setAdapter(adapter);


        addEventButton = getView().findViewById(R.id.button_add_event_save);
        addEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAddEvent();
            }
        });

        // region Time and Date
        // Time from
        timeFromText = getView().findViewById(R.id.add_event_time_from);
        pickTimeFromButton = getView().findViewById(R.id.button_add_event_select_time_from);
        pickTimeFromButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                // TODO save the time
                                timeFromText.setText(String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute));
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });

        // Date from
        dateFromText = getView().findViewById(R.id.add_event_date_from);
        pickDateFromButton = getView().findViewById(R.id.button_add_event_select_date_from);
        pickDateFromButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Date
                final Calendar c = Calendar.getInstance();
                final int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);


                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                // TODO save the date
                                dateFromText.setText(String.format("%02d", dayOfMonth) + "." + String.format("%02d", (monthOfYear + 1)) + "." + String.format("%04d", year));

                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        // Time to
        timeToText = getView().findViewById(R.id.add_event_time_to);
        pickTimeToButton = getView().findViewById(R.id.button_add_event_select_time_to);
        pickTimeToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                // TODO save the time
                                timeToText.setText(String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute));
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();
            }
        });

        // Date to
        dateToText = getView().findViewById(R.id.add_event_date_to);
        pickDateToButton = getView().findViewById(R.id.button_add_event_select_date_to);
        pickDateToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get Current Date
                final Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);


                DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                // TODO save the date
                                dateFromText.setText(String.format("%02d", dayOfMonth) + "." + String.format("%02d", (monthOfYear + 1)) + "." + String.format("%04d", year));

                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        /// endregion

        position = null;
    }

    /**
     *  called when the user presses the save-event-button
     */
    private void performAddEvent() {
        // TODO save Event
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /*if (requestCode == PICK_IMAGE) {
            if(resultCode == RESULT_OK) {
                Uri path = data.getData();

                try {
                    profilePicture = ImageTools.decodeUri(path, getContentResolver());

                    ViewGroup.LayoutParams params = profilePictureButton.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    profilePictureButton.setBackground(null);
                    profilePictureButton.setLayoutParams(params);
                    profilePictureButton.setImageBitmap(profilePicture);
                    profilePictureButton.setImageTintList(null);

                    findViewById(R.id.registration_host_image_hint).setVisibility(View.VISIBLE);
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found");
                }


            }
        }*/
        if(requestCode == PLACE_PICKER_REQUEST) {
            if(resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, getContext());
                position = place.getLatLng();

                String address = position.toString();
                List<Address> addresses = null;
                Geocoder geocoder = new Geocoder(getContext(), Locale.GERMANY);
                try {
                    addresses = geocoder.getFromLocation(position.latitude, position.longitude, 1);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(addresses.size() > 0) {
                    address = addresses.get(0).getAddressLine(0);
                }
                locationButtonText.setText(address);
                locationButtonText.setTypeface(Typeface.DEFAULT);
            }
        }
    }

    /**
     * Called when the user presses the choose location button
     */
    private void performLocationSelection() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("MYLOG", e.getMessage());
        }
    }

    /**
     * Called when the user presses the registrate button TODO modify to add event
     */
    /*private void performRegistration() {
        Log.d(TAG, "Checking user input...");
        if(checkUserInputBasic()) {
            Log.d(TAG, "User input ok");

            SendRegisterHost sendRegisterHost = new SendRegisterHost();
            sendRegisterHost.execute(host.getName(), host.getDescription(), String.valueOf(host.getColor())); //TODO params

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("idToken", idToken);
            intent.putExtra("user", "host");
            startActivity(intent);
            finish();
        }
    }

    private boolean checkUserInputBasic(){
        // Check whether name field is empty
        host.setName(nameField.getText().toString());
        if(host.getName().equals("")) {
            Toast.makeText(getApplicationContext(),"Namensfeld ist leer.",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Namefield empty.");
            return false;
        }

        // Description is optional (can be empty)
        host.setDescription(descriptionField.getText().toString());

        if(position == null) {
            Toast.makeText(getApplicationContext(),"Keine Location ausgewählt",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No Location choosen.");
            return false;
        }

        ArrayList genres = new ArrayList();
        genres.add((Genre)genreSpinner.getSelectedItem());
        host.setGenres(genres);
        if(host.getGenres().get(0).equals(getResources().getString(R.string.artist_genre_choose))) {
            Toast.makeText(getApplicationContext(),"Kein Genre ausgewählt",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No genre selected.");
            return false;
        }

        return true;
    }*/

    /*class SendRegisterHost extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL("https://gigfinder.azurewebsites.net/api/hosts");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Authorization", idToken);
                urlConnection.setRequestProperty("Content-Type","application/json");
                //urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setDoOutput(true);

                //Send data
                DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", params[0]);
                jsonObject.put("description", params[1]);
                jsonObject.put("backgroundColor", params[2]);
                //jsonObject.put("genre", params[3]);
                //jsonObject.put("image", params[4]);
                os.writeBytes(jsonObject.toString());
                os.close();

                //Get response
                InputStream is = null;
                try {
                    is = urlConnection.getInputStream();
                } catch (IOException ioe) {
                    if (urlConnection instanceof HttpURLConnection) {
                        HttpURLConnection httpConn = (HttpURLConnection) urlConnection;
                        int statusCode = httpConn.getResponseCode();
                        if (statusCode != 200) {
                            is = httpConn.getErrorStream();
                            Log.d(TAG, "SendRegisterHost: STATUS CODE: " + statusCode);
                            Log.d(TAG, "SendRegisterHost: RESPONESE MESSAGE: " + httpConn.getResponseMessage());
                            Log.d(TAG, httpConn.getURL().toString());
                        }
                    }
                }

                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                Log.d(TAG, "SendRegisterHost: RESPONSE:" + response.toString());

                return response.toString();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }*/
}