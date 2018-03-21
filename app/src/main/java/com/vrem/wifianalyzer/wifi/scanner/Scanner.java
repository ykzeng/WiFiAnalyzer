/*
 * WiFiAnalyzer
 * Copyright (C) 2018  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.vrem.wifianalyzer.wifi.scanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.vrem.wifianalyzer.navigation.items.ExportItem;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.vrem.wifianalyzer.wifi.WiFiDetailClosure;

public class Scanner implements ScannerService {
    private final List<UpdateNotifier> updateNotifiers;
    private final WifiManager wifiManager;
    private final Settings settings;
    private Transformer transformer;
    private WiFiData wiFiData;
    private Cache cache;
    private PeriodicScan periodicScan;
    public static String wifiHistData = "";
    public  static FileOutputStream fOut;
    private Context context;
    private String fileName = "";
    private OutputStreamWriter out = null;


    private void setupFile(Context context, String filePath, String fileName){
//        String state = Environment.getExternalStorageState();
//        if (!Environment.MEDIA_MOUNTED.equals(state)) {
//            Toast.makeText(this, "no external storage", Toast.LENGTH_SHORT);
//            return null;
//        }
        if (Build.VERSION.SDK_INT >= 23) {
            //do your check here

            int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                System.out.println("no permission for writing files");
                //ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1 );
                return;
            }
        }
        // Get the directory for the user's public pictures directory.
        final File path = Environment.getExternalStoragePublicDirectory
                (
                        //Environment.DIRECTORY_PICTURES
                        filePath
                );

        // Make sure the path directory exists.
        if(!path.exists())
        {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        File file = new File(path, fileName);
        fOut = null;

        try {
            file.createNewFile();
            fOut = new FileOutputStream(file, true);

            System.out.println("created file out put stream");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    Scanner(@NonNull Context context, @NonNull WifiManager wifiManager, @NonNull Handler handler, @NonNull Settings settings) {
        this.updateNotifiers = new ArrayList<>();
        this.wifiManager = wifiManager;
        this.settings = settings;
        this.wiFiData = WiFiData.EMPTY;
        this.setTransformer(new Transformer());
        this.setCache(new Cache());
        this.periodicScan = new PeriodicScan(this, handler, settings);
        this.context = context;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        this.fileName = "wifi_log_" + timestamp;
        this.setupFile(context, "/Movies/", fileName);
    }

    @Override
    public void update() {
        performWiFiScan();
        IterableUtils.forEach(updateNotifiers, new UpdateClosure());
    }

    @Override
    @NonNull
    public WiFiData getWiFiData() {
        return wiFiData;
    }

    @Override
    public void register(@NonNull UpdateNotifier updateNotifier) {
        updateNotifiers.add(updateNotifier);
    }

    @Override
    public void unregister(@NonNull UpdateNotifier updateNotifier) {
        updateNotifiers.remove(updateNotifier);
    }

    @Override
    public void pause() {
        periodicScan.stop();
    }

    @Override
    public boolean isRunning() {
        return periodicScan.isRunning();
    }

    @Override
    public void resume() {
        periodicScan.start();
    }

    @Override
    public void setWiFiOnExit() {
        if (settings.isWiFiOffOnExit()) {
            try {
                wifiManager.setWifiEnabled(false);
            } catch (Exception e) {
                // critical error: do not die
            }
        }
    }

    @NonNull
    PeriodicScan getPeriodicScan() {
        return periodicScan;
    }

    void setPeriodicScan(@NonNull PeriodicScan periodicScan) {
        this.periodicScan = periodicScan;
    }

    void setCache(@NonNull Cache cache) {
        this.cache = cache;
    }

    void setTransformer(@NonNull Transformer transformer) {
        this.transformer = transformer;
    }

    @NonNull
    List<UpdateNotifier> getUpdateNotifiers() {
        return updateNotifiers;
    }

    private void performWiFiScan() {
        List<ScanResult> scanResults = Collections.emptyList();
        WifiInfo wifiInfo = null;
        List<WifiConfiguration> configuredNetworks = null;
        try {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            // vincent
            // Registering Wifi Receiver
            BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);
            //vincent
            if (wifiManager.startScan()) {
                scanResults = wifiManager.getScanResults();
            }
            wifiInfo = wifiManager.getConnectionInfo();
            configuredNetworks = wifiManager.getConfiguredNetworks();
        } catch (Exception e) {
            // critical error: set to no results and do not die
        }
        cache.add(scanResults);
        wiFiData = transformer.transformToWiFiData(cache.getScanResults(), wifiInfo, configuredNetworks);

        List<WiFiDetail> wifiDetails = wiFiData.getWiFiDetails();
        String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

        final StringBuilder result = new StringBuilder();
        result.append(
                String.format(Locale.ENGLISH,
                        "Time Stamp|SSID|BSSID|Strength|Primary Channel|Primary Frequency|Center Channel|Center Frequency|Width (Range)|Distance|Security%n"));
        IterableUtils.forEach(wifiDetails, new WiFiDetailClosure(timestamp, result));

        wifiHistData += (result.toString() + "\n");
        // to prevent string overflow, set a max string length of 4MB
        if (out == null)
            out = new OutputStreamWriter(fOut);
        try{
            out.append(wifiHistData);
            out.flush();

            fOut.flush();
            wifiHistData = "";
        }catch (IOException e){
            e.printStackTrace();
        }
        Toast.makeText(this.context, "wifi info logged into " + fileName, Toast.LENGTH_SHORT);
    }

    private class UpdateClosure implements Closure<UpdateNotifier> {
        @Override
        public void execute(UpdateNotifier updateNotifier) {
            updateNotifier.update(wiFiData);
        }
    }
}
