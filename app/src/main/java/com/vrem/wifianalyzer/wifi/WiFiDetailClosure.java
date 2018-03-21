package com.vrem.wifianalyzer.wifi;

import android.support.annotation.NonNull;

import com.vrem.wifianalyzer.wifi.model.WiFiDetail;
import com.vrem.wifianalyzer.wifi.model.WiFiSignal;

import org.apache.commons.collections4.Closure;

import java.util.Locale;

/**
 * Created by yukun on 3/21/2018.
 */

public class WiFiDetailClosure implements Closure<WiFiDetail> {
    private final StringBuilder result;
    private final String timestamp;

    public WiFiDetailClosure(String timestamp, @NonNull StringBuilder result) {
        this.result = result;
        this.timestamp = timestamp;
    }

    @Override
    public void execute(WiFiDetail wiFiDetail) {
        WiFiSignal wiFiSignal = wiFiDetail.getWiFiSignal();
        result.append(String.format(Locale.ENGLISH, "%s|%s|%s|%ddBm|%d|%d%s|%d|%d%s|%d%s (%d - %d)|%.1fm|%s%n",
                timestamp,
                wiFiDetail.getSSID(),
                wiFiDetail.getBSSID(),
                wiFiSignal.getLevel(),
                wiFiSignal.getPrimaryWiFiChannel().getChannel(),
                wiFiSignal.getPrimaryFrequency(),
                WiFiSignal.FREQUENCY_UNITS,
                wiFiSignal.getCenterWiFiChannel().getChannel(),
                wiFiSignal.getCenterFrequency(),
                WiFiSignal.FREQUENCY_UNITS,
                wiFiSignal.getWiFiWidth().getFrequencyWidth(),
                WiFiSignal.FREQUENCY_UNITS,
                wiFiSignal.getFrequencyStart(),
                wiFiSignal.getFrequencyEnd(),
                wiFiSignal.getDistance(),
                wiFiDetail.getCapabilities()));
    }
}