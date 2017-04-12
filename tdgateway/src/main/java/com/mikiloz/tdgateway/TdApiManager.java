package com.mikiloz.tdgateway;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;


public class TdApiManager {

    static final String DEVICE_AUTHENTICATION_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/crc.json?sn=%1$s&key=%2$s";

    static final String[] DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS = {"amount", "until"};
    static final String DEVICE_MESSAGES_HISTORY_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/history.json";

    static final String DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/recents.json?amount=%1$s&after=%2$s";

    static final String[] DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS = {"limit", "before"};
    static final String DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/sfx/history.json";

    private Context context;
    private RequestQueue requestQueue;
    List<TdDevice> devices = new ArrayList<>();


    public TdApiManager(Context context) {
        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
    }


    /**
     * Creates a new {@link TdDevice}.
     * @param sn The serial number.
     * @param key The device key.
     */
    public TdDevice newDevice(String sn, String key) {
        final TdDevice device = new TdDevice(sn, key, this);
        devices.add(device);
        return device;
    }

    void performRequest(Request request) {
        requestQueue.add(request);
    }

}
