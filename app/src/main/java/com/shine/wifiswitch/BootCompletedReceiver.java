package com.shine.wifiswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by 123 on 2017/5/15.
 */

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootCompletedReceiver", "MyBootCompletedReceiveronReceive");
        Intent i = new Intent(context, WifiService2.class);
        context.startService(i);
    }
}
