package com.mikiloz.tdgateway;


import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RawMessage {

    String device;
    Date time;
    double snr;
    byte[] payload;
    JSONObject jsonObject;
    List<ReceptionInfo> rinfos = new ArrayList<>();

    /**
     * Get the device id of the device that sent this message.
     * @return The device id of the device that sent this message.
     */
    public String getDevice() {
        return device;
    }

    /**
     * Get the message reception timestamp (GMT).
     * @return The message reception timestamp (GMT).
     */
    public Date getTime() {
        return time;
    }

    /**
     * Get the best SNR of the messages received by the network so far.
     * @return The best SNR of the messages received by the network so far.
     */
    public double getSnr() {
        return snr;
    }

    /**
     * Returns the message's payload.
     * @return The message's payload, will be {@code null} if it has no payload.
     */
    public @Nullable byte[] getPayload() {
        return payload;
    }

    /**
     * Get the list of all reception informations for each base stations.
     * @return The list of all reception informations for each base stations.
     */
    public List<ReceptionInfo> getReceptionInfoList() {
        return rinfos;
    }

    /**
     * Class that stores message reception information, for the radio stations involved in the
     * reception process.
     */
    public static class ReceptionInfo {

        String tap;
        double snr, rssi;

        public ReceptionInfo(String tap, double snr, double rssi) {
            this.tap = tap;
            this.snr = snr;
            this.rssi = rssi;
        }

        /**
         * Get the base station identifier.
         * @return Base station identifier.
         */
        public String getTap() {
            return tap;
        }

        /**
         * Get the best signal of all repetitions for this base station.
         * @return The best signal of all repetitions for this base station.
         */
        public double getSnr() {
            return snr;
        }

        /**
         * Get the received Signal Strength Indication (in dBm).
         * @return Received Signal Strength Indication (in dBm).
         */
        public double getRssi() {
            return rssi;
        }
    }
}
