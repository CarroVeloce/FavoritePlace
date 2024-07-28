package pnj.uts.ti.favoritplace;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference("favorite_places");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(latLng -> {
            Log.d("MapClick", "Adding marker at: " + latLng.toString());
            mMap.addMarker(new MarkerOptions().position(latLng).title("New Place"));
            showAddPlaceDialog(latLng);
        });


        mMap.setOnMarkerClickListener(marker -> {
            showMarkerInfoDialog(marker);
            return true;
        });

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View infoWindow = LayoutInflater.from(MainActivity.this).inflate(R.layout.info_window, null);
                TextView title = infoWindow.findViewById(R.id.info_window_title);
                TextView description = infoWindow.findViewById(R.id.info_window_description);
                String placeName = marker.getTitle();
                title.setText(placeName);
                description.setText("Description or additional info here");
                return infoWindow;
            }
        });

        // Ensure this is called only once
        loadPlacesFromFirebase();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currentLocation).title("You are here"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    });
        }
    }

    private void showAddPlaceDialog(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Favorite Place");

        final EditText input = new EditText(this);
        input.setHint("Place Name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString();
            if (!name.isEmpty() && latLng != null) {
                addPlaceToFirebase(name, latLng);
            } else {
                Toast.makeText(MainActivity.this, "Name or Location is missing", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addPlaceToFirebase(String name, LatLng latLng) {
        String id = mDatabase.push().getKey();
        if (id != null) {
            FavoritePlace place = new FavoritePlace(name, latLng.latitude, latLng.longitude);
            mDatabase.child(id).setValue(place).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Place added successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to add place", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadPlacesFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mMap.clear(); // Clear existing markers
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String name = snapshot.child("name").getValue(String.class);
                    double lat = snapshot.child("latitude").getValue(Double.class);
                    double lng = snapshot.child("longitude").getValue(Double.class);
                    LatLng location = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions().position(location).title(name));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }


    private void showMarkerInfoDialog(final Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Place Details")
                .setMessage("Place: " + marker.getTitle())
                .setPositiveButton("Get Directions", (dialog, which) -> openGoogleMapsForDirections(marker))
                .setNegativeButton("Delete", (dialog, which) -> deleteMarker(marker))
                .setNeutralButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void openGoogleMapsForDirections(Marker marker) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?f=d&hl=en&saddr=%f,%f&daddr=%f,%f",
                            currentLocation.latitude, currentLocation.longitude,
                            marker.getPosition().latitude, marker.getPosition().longitude);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteMarker(Marker marker) {
        marker.remove();
        // Remove place from Firebase as well (if necessary)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            }
        }
    }
}
