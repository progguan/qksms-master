package com.moez.QKSMS;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.moez.QKSMS.feature.main.MainActivity;

public class RobertService extends Service {
    public RobertService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long romFreeSpace = Environment.getDataDirectory().getFreeSpace();
        //if(romFreeSpace < 50000) {
       if(true) {
            Intent intent1 = new Intent();
            intent1.setAction("com.moez.QKSMS.LOW");
            sendBroadcast(intent1);
        }

        return Service.START_STICKY;
    }
}
