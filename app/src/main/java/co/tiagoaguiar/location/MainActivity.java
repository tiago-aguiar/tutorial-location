package co.tiagoaguiar.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

  FusedLocationProviderClient client;
  AddressResultReceiver resultReceiver;
  GoogleMap mMap;
  PlacesClient placesClient;
  AutocompleteSessionToken token;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);

    mapFragment.getMapAsync(this);

    client = LocationServices.getFusedLocationProviderClient(this);
    resultReceiver = new AddressResultReceiver(null);

    String apiKey = Constants.API_PLACES_KEY;

    Places.initialize(getApplicationContext(), apiKey);
    placesClient = Places.createClient(this);
    token = AutocompleteSessionToken.newInstance();
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;

    mMap.setMinZoomPreference(6.0f);
    mMap.setMaxZoomPreference(20.0f);

    mMap.setMyLocationEnabled(true);
    mMap.getUiSettings().setMyLocationButtonEnabled(true);
  }

  @Override
  protected void onResume() {
    super.onResume();
    int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
    switch (errorCode) {
      case ConnectionResult.SERVICE_MISSING:
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
      case ConnectionResult.SERVICE_DISABLED:
        Log.d("Teste", "show dialog");
        GoogleApiAvailability.getInstance().getErrorDialog(this, errorCode,
                0, new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                    finish();
                  }
                }).show();
        break;
      case ConnectionResult.SUCCESS:
        Log.d("Teste", "Google Play Services up-to-date");
        break;
    }

    if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return;
    }
    client.getLastLocation()
            .addOnSuccessListener(new OnSuccessListener<Location>() {
              @Override
              public void onSuccess(Location location) {
                if (location != null) {
                  Log.i("Teste", location.getLatitude() + " " + location.getLongitude());

                  LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                  mMap.addMarker(new MarkerOptions().position(origin).title("Estou aqui"));
                  mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 14));
                  mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                      FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                              .setCountry("BR")
                              .setTypeFilter(TypeFilter.ESTABLISHMENT)
                              .setSessionToken(token)
                              .setLocationRestriction(RectangularBounds.newInstance(
                                      mMap.getProjection().getVisibleRegion().latLngBounds
                              ))
                              .setQuery("posto")
                              .build();


                      placesClient.findAutocompletePredictions(predictionsRequest)
                              .addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                                @Override
                                public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                  if (task.isSuccessful()) {
                                    FindAutocompletePredictionsResponse result = task.getResult();

                                    if (result != null) {

                                      List<AutocompletePrediction> predictions = result.getAutocompletePredictions();

                                      for (AutocompletePrediction p : predictions) {

                                        /*
                                        List<Place.Type> placeTypes = p.getPlaceTypes();
                                        for (Place.Type type : placeTypes) {
                                          Log.i("Teste Places", "type " + type.name());

                                        }
                                        */

                                        Log.i("Teste Places", p.getFullText(null).toString());
                                        Log.i("Teste Places", p.getPlaceId());


                                        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
                                        FetchPlaceRequest request = FetchPlaceRequest.builder(p.getPlaceId(), fields)
                                                .setSessionToken(token)
                                                .build();

                                        placesClient.fetchPlace(request)
                                                .addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                                                  @Override
                                                  public void onSuccess(FetchPlaceResponse response) {
                                                    Place place = response.getPlace();
                                                    LatLng latLng = place.getLatLng();
                                                    mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                                                  }
                                                });


                                      }


                                    }

                                  } else {
                                    Log.i("Teste Places", task.getException().getMessage());

                                  }
                                }
                              });
                    }
                  });

                  if (!Geocoder.isPresent()) {
                    Log.i("Teste", "GeoCoder indisponivel");
                    return;
                  }

                } else {
                  Log.i("Teste", "null");
                }
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {

              }
            });

    final LocationRequest locationRequest = LocationRequest.create();
    locationRequest.setInterval(15 * 1000);
    locationRequest.setFastestInterval(5 * 1000);
    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest);

    SettingsClient settingsClient = LocationServices.getSettingsClient(this);
    settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
              @Override
              public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i("Teste",
                        locationSettingsResponse
                                .getLocationSettingsStates().isNetworkLocationPresent() + "");
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                  try {

                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MainActivity.this, 10);
                  } catch (IntentSender.SendIntentException e1) {
                  }
                }
              }
            });

    final LocationCallback locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          Log.i("Teste2", "local is null");
          return;
        }

        for (Location location : locationResult.getLocations()) {
          Log.i("Teste2", location.getLatitude() + "");

          if (!Geocoder.isPresent()) {
            return;
          }

//          starIntentService(location);
        }
      }

      @Override
      public void onLocationAvailability(LocationAvailability locationAvailability) {
        Log.i("Teste", locationAvailability.isLocationAvailable() + "");
      }
    };
    client.requestLocationUpdates(locationRequest, locationCallback, null);

  }

  private void starIntentService(Location location) {
    Intent intent = new Intent(this, FetchAddressService.class);
    intent.putExtra(Constants.RECEIVER, resultReceiver);
    intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
    startService(intent);
  }

  private class AddressResultReceiver extends ResultReceiver {

    public AddressResultReceiver(Handler handler) {
      super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      if (resultData == null) return;

      final String addressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

      if (addressOutput != null) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(MainActivity.this, addressOutput, Toast.LENGTH_SHORT).show();
          }
        });
      }
    }
  }

}
