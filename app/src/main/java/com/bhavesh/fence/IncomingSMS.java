package com.bhavesh.fence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * Created by bhavesh on 14-Dec-2016.
 */

public class IncomingSMS extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    final Bundle bundle = intent.getExtras();
    try {
      if (bundle != null) {
        final Object[] pdusObj = (Object[]) bundle.get("pdus");
        for (int i = 0; i < pdusObj.length; i++) {
          SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
          String phoneNumber = currentMessage.getDisplayOriginatingAddress();
          String senderNum = phoneNumber;
          String message = currentMessage.getDisplayMessageBody().trim();
          String msg[] = message.split(" ");
          if (msg[0].equalsIgnoreCase("fence") && msg[1].equalsIgnoreCase("start")) {
            MainActivity.myMediaPlayer.start();
            MainActivity.notificationLabel.setText("Alarm started.");
          } else if (msg[0].equalsIgnoreCase("fence") && msg[1].equalsIgnoreCase("stop")) {
            MainActivity.myMediaPlayer.stop();
            MainActivity.notificationLabel.setText("Alarm stopped.");
          }
        }
      }
    } catch (Exception e) {
    }
  }
}
