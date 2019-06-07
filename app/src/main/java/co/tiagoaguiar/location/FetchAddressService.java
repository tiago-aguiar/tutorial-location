package co.tiagoaguiar.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maio, 27 2019
 *
 * @author suporte@moonjava.com.br (Tiago Aguiar).
 */
public class FetchAddressService extends IntentService {

  protected ResultReceiver receiver;

  public FetchAddressService() {
    super("fetchAddressService");
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
    if (intent == null) return;

    Geocoder geocoder = new Geocoder(this, Locale.getDefault());

    Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
    receiver = intent.getParcelableExtra(Constants.RECEIVER);

    List<Address> addresses = null;

    try {
      addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),
              1);
    } catch (IOException e) {
      Log.e("Teste", "serviço indisponivel", e);
    } catch (IllegalArgumentException e) {
      Log.e("Teste", "latitude ou longitude invalida", e);
    }

    if (addresses == null || addresses.isEmpty()) {
      Log.e("Teste", "nenhum endereco encontado");
      deliverResultToReceiver(Constants.FAILURE_RESULT,
              "nenhum endereco encontrado");
    } else {
      Address address = addresses.get(0);
      List<String> addressF = new ArrayList<>();

      for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
        addressF.add(address.getAddressLine(i));
      }

      deliverResultToReceiver(Constants.SUCCESS_RESULT,
              TextUtils.join("|", addressF));
    }


  }

  private void deliverResultToReceiver(int resultCode, String message) {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.RESULT_DATA_KEY, message);
    receiver.send(resultCode, bundle);
  }

}
