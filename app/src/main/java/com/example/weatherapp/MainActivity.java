package com.example.weatherapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.Manifest;

import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // Déclaration des variables pour les éléments d'interface utilisateur
    private RelativeLayout homeRl;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private TextView rain, wind, humidity;
    // Liste pour stocker les données météo
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    // Adaptateur pour la RecyclerView
    private WeatherRVAdapter weatherRVAdapter;
    // Gestionnaire de localisation
    private LocationManager locationManager;
    // Code de demande de permission
    private int PERMISSION_CODE = 1;
    // Nom de la ville
    private String cityName;
    // Référence à la base de données Firebase
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Masquer la barre d'état pour une interface utilisateur immersive
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        // Initialisation des éléments d'interface utilisateur
        homeRl = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        rain = findViewById(R.id.raint);
        wind = findViewById(R.id.windt);
        humidity = findViewById(R.id.humidityt);
        // Initialisation de la liste de données météo et de l'adaptateur
        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialisation de la référence de la base de données Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("weatherData");

        // Écouter les changements dans la base de données
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                weatherRVModalArrayList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    try {
                        WeatherRVModal weatherRVModal = dataSnapshot.getValue(WeatherRVModal.class);

                        //logs pour vérifier chaque champ
                        Log.d("Firebase", "Time: " + weatherRVModal.getTime());
                        Log.d("Firebase", "Temperature: " + weatherRVModal.getTemperature());
                        Log.d("Firebase", "Icon: " + weatherRVModal.getIcon());
                        Log.d("Firebase", "WindSpeed: " + weatherRVModal.getWindSpeed());

                        // Ajoutez une vérification pour éviter les données nulles
                        if (weatherRVModal != null) {
                            weatherRVModalArrayList.add(weatherRVModal);
                        }
                    } catch (Exception e) {
                        Log.e("Firebase", "Error parsing data: " + e.getMessage());
                    }
                }

                weatherRVAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data from Firebase: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error fetching data from Firebase", Toast.LENGTH_SHORT).show();
            }
        });

        // Vérifier les autorisations de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            // Obtenir la dernière position connue
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                cityName = getCityName(longitude, latitude);
                getWeatherInfo(cityName);
            } else {
                Toast.makeText(this, "Unable to get location. Please check your location settings.", Toast.LENGTH_SHORT).show();
            }
        }

        // Gérer le clic sur le bouton de recherche
        searchIV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTV.setText(cityName);
                    // Supprimer toutes les données existantes avant chaque nouvel ajout
                    databaseReference.removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@androidx.annotation.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {
                                // Succès de la suppression des données existantes
                                Log.d("Firebase", "Existing data removed successfully");

                                // Continuer avec la récupération des données de l'API
                                getWeatherInfo(city);
                            } else {
                                // Gestion des erreurs lors de la suppression des données existantes
                                Log.e("Firebase", "Error removing existing data: " + error.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    // Gérer la réponse à la demande de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted..", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please provide the permissions!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Obtenir le nom de la ville à partir des coordonnées géographiques
    private String getCityName(double longitude, double latitude) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> adresses = gcd.getFromLocation(latitude, longitude, 10);
            for (Address adr : adresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Toast.makeText(this, "City NOT FOUND..", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.fillInStackTrace();
        }
        return cityName;
    }

    // Obtenir les informations météo à partir de l'API
    private void getWeatherInfo(String cityName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=f8ab67fc6d854063bcc141440230612&q=" + cityName + "&days=1&aqi=no&alerts=no";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRl.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                    String rainPercentage = response.getJSONObject("current").getString("precip_mm");
                    String windSpeed = response.getJSONObject("current").getString("wind_kph");
                    String humidityPercentage = response.getJSONObject("current").getString("humidity");

                    // Updating TextViews with the extracted information
                    rain.setText(rainPercentage + " mm");
                    wind.setText(windSpeed + " kph");
                    humidity.setText(humidityPercentage + "%");
                    conditionTV.setText(condition);
                    if (isDay == 1) {
                        backIV.setImageResource(R.drawable.day); // set the day picture as a background
                    } else {
                        backIV.setImageResource(R.drawable.night); // set the day picture as a background
                    }

                    try {
                        JSONObject forecastObj = response.getJSONObject("forecast");
                        JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                        JSONArray hourArray = forecast0.getJSONArray("hour");

                        // Effacer les données actuelles dans la liste
                        weatherRVModalArrayList.clear();

                        for (int i = 0; i < hourArray.length(); i++) {
                            JSONObject hourObj = hourArray.getJSONObject(i);
                            String time = hourObj.getString("time");
                            String temper = hourObj.getString("temp_c");
                            String img = hourObj.getJSONObject("condition").getString("icon");
                            String wind = hourObj.getString("wind_kph");

                            // Créer une instance de WeatherRVModal
                            WeatherRVModal weatherRVModal = new WeatherRVModal(time, temper, img, wind);

                            // Ajouter l'instance à la liste
                            //weatherRVModalArrayList.add(weatherRVModal);

                            // Ajouter la donnée à Firebase
                            databaseReference.push().setValue(weatherRVModal);
                        }

                        // Notifier l'adaptateur du changement dans la liste
                        weatherRVAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erreur de traitement de la réponse JSON", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Firebase", "Error pushing data to Firebase: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error pushing data to Firebase", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Please enter a valid city name..", Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

}
