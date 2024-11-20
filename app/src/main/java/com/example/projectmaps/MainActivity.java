package com.example.projectmaps;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Spinner mapTypeSpinner;
    private LatLng currentLocation = new LatLng(-7.281946, 112.795331); // Default location: ITS
    private LatLng destinationLocation;
    private EditText originInput, destinationInput;
    private Button searchButton;
    private TextView distanceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        mapTypeSpinner = findViewById(R.id.map_type_spinner);
        originInput = findViewById(R.id.origin_input);
        destinationInput = findViewById(R.id.destination_input);
        searchButton = findViewById(R.id.search_button);
        distanceText = findViewById(R.id.distance_text);
        searchButton.setOnClickListener(v -> calculateRoute());

        // Setup map type spinner
        setupMapTypeSpinner();

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupMapTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Normal", "Hybrid", "Satellite", "Terrain"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.WHITE);  // Mengubah warna teks pada tampilan utama spinner
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.WHITE);  // Mengubah warna teks pada dropdown spinner
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapTypeSpinner.setAdapter(adapter);

        mapTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case 1:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    case 2:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case 3:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Handle case where no item is selected
            }
        });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Default location and zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

        mMap.setOnMapClickListener(latLng -> {
            destinationInput.setText(latLng.latitude + ", " + latLng.longitude);
            calculateRoute();
        });

        originInput.setOnEditorActionListener((v, actionId, event) -> {
            calculateRoute();
            return true;
        });

        destinationInput.setOnEditorActionListener((v, actionId, event) -> {
            calculateRoute();
            return true;
        });
    }

    private void setupSearchBar() {
        searchButton.setOnClickListener(v -> searchLocation());
    }

    private void searchLocation() {
        String locationInput = originInput.getText().toString().trim();

        if (TextUtils.isEmpty(locationInput)) {
            Toast.makeText(this, "Masukkan nama tempat atau koordinat", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng locationLatLng;

        try {
            // Cek apakah input adalah koordinat
            if (locationInput.matches("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$")) {
                String[] parts = locationInput.split(",");
                locationLatLng = new LatLng(
                        Double.parseDouble(parts[0].trim()),
                        Double.parseDouble(parts[1].trim())
                );
            } else {
                // Gunakan Geocoder jika input bukan koordinat
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(locationInput, 1);
                if (addresses.isEmpty()) {
                    Toast.makeText(this, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show();
                    return;
                }
                locationLatLng = new LatLng(
                        addresses.get(0).getLatitude(),
                        addresses.get(0).getLongitude()
                );
            }

            // Tambahkan marker dan fokuskan kamera pada lokasi
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(locationLatLng).title("Hasil Pencarian"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void calculateRoute() {
        String origin = originInput.getText().toString();
        String destination = destinationInput.getText().toString();

        if (TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination)) {
            Toast.makeText(this, "Masukkan lokasi awal dan tujuan", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> originAddresses = geocoder.getFromLocationName(origin, 1);
            List<Address> destinationAddresses = geocoder.getFromLocationName(destination, 1);

            if (originAddresses.isEmpty() || destinationAddresses.isEmpty()) {
                Toast.makeText(this, "Tidak dapat menemukan lokasi", Toast.LENGTH_SHORT).show();
                return;
            }

            LatLng originLatLng = new LatLng(originAddresses.get(0).getLatitude(), originAddresses.get(0).getLongitude());
            LatLng destinationLatLng = new LatLng(destinationAddresses.get(0).getLatitude(), destinationAddresses.get(0).getLongitude());

            // Update map with markers
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(originLatLng).title("Lokasi Awal: " + origin));
            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Lokasi Tujuan: " + destination));

            // Calculate distance and zoom level
            float[] results = new float[1];
            Location.distanceBetween(
                    originLatLng.latitude, originLatLng.longitude,
                    destinationLatLng.latitude, destinationLatLng.longitude,
                    results);
            float distance = results[0] / 1000; // Distance in km

            // Calculate zoom based on distance
            float zoomLevel = calculateZoomLevel(distance);

            // Calculate midpoint for camera focus
            LatLng midPoint = new LatLng(
                    (originLatLng.latitude + destinationLatLng.latitude) / 2,
                    (originLatLng.longitude + destinationLatLng.longitude) / 2
            );

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(midPoint, zoomLevel));

            // Draw route on the map and display distance
            drawRoute(originLatLng, destinationLatLng);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private float calculateZoomLevel(float distanceInKm) {
        if (distanceInKm < 1) return 16; // Close
        if (distanceInKm < 5) return 13; // Medium
        if (distanceInKm < 20) return 12; // Far
        if (distanceInKm < 50) return 10;  // Very far
        if (distanceInKm < 100) return 7; // Extremely far
        return 5; // Very large distance
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        List<LatLng> points = new ArrayList<>();
        points.add(origin);
        points.add(destination);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .color(android.graphics.Color.RED)
                .width(5);

        mMap.addPolyline(polylineOptions);

        float[] results = new float[1];
        Location.distanceBetween(
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                results);

        distanceText.setText(String.format(Locale.getDefault(), "Jarak: %.2f km", results[0] / 1000));
    }
}
