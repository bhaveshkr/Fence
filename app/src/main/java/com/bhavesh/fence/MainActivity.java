package com.bhavesh.fence;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ServiceCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

  private SensorManager mSensorManager;
  private Sensor mSensor;
  private Sensor tiltSensor;
  private TriggerEventListener mTriggerEventListener;
  private TextToSpeech tts;
  static TextView notificationLabel;
  String phoneNumber;
  String deviceName;
  static MediaPlayer myMediaPlayer = null;
  JSONObject locationObject = new JSONObject();
  private AVLoadingIndicatorView avi;

  int PERMISSION_ALL = 1;
  String[] PERMISSIONS = {Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    notificationLabel = (TextView) findViewById(R.id.notificationLabel);

    requestPermissions();

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
    tiltSensor = mSensorManager.getDefaultSensor(22);
    myMediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.danger);
    LocationManager locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
          int result = tts.setLanguage(Locale.US);
          if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "This Language is not supported");
          }
          notificationLabel.setText("Tracking started...!");
        } else {
          Log.e("TTS", "Initilization Failed!");
        }
      }
    });

    mTriggerEventListener = new TriggerEventListener() {
      @Override
      public void onTrigger(TriggerEvent event) {
        readData();
        if (phoneNumber.trim() == "") {
          return;
        }
        speak("Your device is changing location.");
        SmsManager smsManager = SmsManager.getDefault();
        getLocation();
        String smsBody = "";
        try {
          if (locationObject.get("latitude") != null) {
            smsBody = "Your device : " + deviceName + " is changing location. Coordinate - " + locationObject.toString();
          } else {
            smsBody = "Your device : " + deviceName + " is changing location.";
          }
        } catch (Exception e) {
          smsBody = "Your device : " + deviceName + " is changing location.";
        }
        Toast.makeText(getApplicationContext(), smsBody, Toast.LENGTH_SHORT).show();
        // Send a text based SMS
        smsManager.sendTextMessage(phoneNumber, null, smsBody, null, null);
        mSensorManager.requestTriggerSensor(mTriggerEventListener, mSensor);
        notificationLabel.setText("Tracking started..!");
      }
    };
    mSensorManager.requestTriggerSensor(mTriggerEventListener, mSensor);
    mSensorManager.requestTriggerSensor(mTriggerEventListener, tiltSensor);

    String indicator=getIntent().getStringExtra("indicator");
    avi= (AVLoadingIndicatorView) findViewById(R.id.avi);
    avi.setIndicator(indicator);
    startAnim();
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    requestPermissions();
  }

  public static boolean hasPermissions(Context context, String... permissions) {
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
      for (String permission : permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      Intent settingsIntent = new Intent(this, SettingsActivity.class);
      startActivity(settingsIntent);
      return true;
    } else if (id == R.id.action_about) {
      Intent aboutIntent = new Intent(this, AboutUS.class);
      startActivity(aboutIntent);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void speak(String text) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    } else {
      tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
  }

  @Override
  public void onDestroy() {
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    super.onDestroy();
    mSensorManager.cancelTriggerSensor(mTriggerEventListener, mSensor);
    notificationLabel.setText("Tracking Stopped.");
    myMediaPlayer.stop();
    stopAnim();
  }

  public void readData() {
    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    phoneNumber = SP.getString("phoneNumber", "");
    deviceName = SP.getString("deviceName", "");
  }

  private void getLocation() {
    LocationManager locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Criteria cri = new Criteria();
    String provider = locationmanager.getBestProvider(cri, false);
    if (provider != null & !provider.equals("")) {
      if (checkPermission(this)) {
        Location location = locationmanager.getLastKnownLocation(provider);
        locationmanager.requestLocationUpdates(provider, 2000, 1, this);
        if (location != null) {
          onLocationChanged(location);
        }
      }
    }
  }

  private void requestPermissions() {
    if (!hasPermissions(this, PERMISSIONS)) {
      ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
    }
    LocationManager locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);
    if (!locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
      showDisabledGPSDialog();
    }
  }

  public static boolean checkPermission(final Context context) {
    return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  private void showDisabledGPSDialog() {
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
      alertDialogBuilder.setMessage("Enable GPS to receive coordinates.")
        .setCancelable(false)
        .setPositiveButton("Goto settings",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              Intent callGPSSettingIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
              startActivity(callGPSSettingIntent);
            }
          });
      alertDialogBuilder.setNegativeButton("Cancel",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
      AlertDialog alert = alertDialogBuilder.create();
      alert.show();
  }

  @Override
  public void onLocationChanged(Location location) {
    try {
      locationObject.put("latitude", location.getLatitude());
      locationObject.put("longitude", location.getLongitude());
    } catch (Exception e) {
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }

  void startAnim(){
    avi.show();
    // or avi.smoothToShow();
  }

  void stopAnim(){
    avi.hide();
    // or avi.smoothToHide();
  }
}
