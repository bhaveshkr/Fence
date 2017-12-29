package com.bhavesh.fence;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by bhavesh on 11-Dec-2016.
 */

public class SettingsFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.pref_general);
  }
}
