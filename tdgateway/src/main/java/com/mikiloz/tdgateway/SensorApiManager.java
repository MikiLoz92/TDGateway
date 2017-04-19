package com.mikiloz.tdgateway;

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An API Manager class for the Telecom Design Sensor API.
 */
public class SensorApiManager {

    static final String DEVICE_AUTHENTICATION_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/crc.json?sn=%1$s&key=%2$s";

    static final String[] DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS = {"amount", "until"};
    static final String DEVICE_MESSAGES_HISTORY_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/history.json";

    static final String[] DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT_PARAMS = {"amount", "after"};
    static final String DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/recents.json";

    static final String[] DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS = {"limit", "before"};
    static final String DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/msgs/sfx/history.json";

    static final String DEVICE_CHILD_DEVICES_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/children.json";

    static final String DEVICE_CLEAR_MESSAGES_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/clear.json";

    static final String[] DEVICE_CHANGE_STATUS_ENDPOINT_PARAMS = {"id", "value"};
    static final String DEVICE_CHANGE_STATUS_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/status.json";

    static final String[] DEVICE_SET_ACTIVE_FLAG_ENDPOINT_PARAMS = {"id", "sn", "value"};
    static final String DEVICE_SET_ACTIVE_FLAG_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/status.json";

    static final String[] DEVICE_SET_MONITORING_FLAG_ENDPOINT_PARAMS = {"id", "sn", "value"};
    static final String DEVICE_SET_MONITORING_FLAG_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/watch.json";

    static final String[] DEVICE_SET_BIDIR_VALUE_ENDPOINT_PARAMS = {"id", "sn", "value"};
    static final String DEVICE_SET_BIDIR_VALUE_ENDPOINT =
            "https://sensor.insgroup.fr/iot/devices/bidirval.json";

    static final String[] DEVELOPER_GET_DEVICE_INFORMATION_ENDPOINT_PARAMS = {"id", "sn"};
    static final String DEVELOPER_GET_DEVICE_INFORMATION_ENDPOINT =
            "https://sensor.insgroup.fr/iot/developers/device.json";



    private Context context;
    private String developerAuthToken;
    private RequestQueue requestQueue;
    List<IotDevice> iotDevices = new ArrayList<>();

    private final static String DEVELOPER_AUTH_HEADER = "Authorization";


    public SensorApiManager(Context context) {
        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
    }

    public SensorApiManager(Context context, String developerAuthToken) {
        this(context);
        this.developerAuthToken = developerAuthToken;
    }

    public IotDevice newIotDeviceForDeviceApi(String sn, String key,
                                              @Nullable Runnable onDeviceApiAuthenticated,
                                              @Nullable Runnable onDeviceInformationAcquired,
                                              @Nullable SensorApiManager.SensorApiErrorListener errorListener) {
        final IotDevice device = new IotDevice(null, sn, key, this,
                onDeviceApiAuthenticated, onDeviceInformationAcquired, errorListener);
        iotDevices.add(device);
        return device;
    }

    public IotDevice newIotDeviceForDeveloperApi(String id, String sn,
                                                 @Nullable Runnable onDeviceApiAuthenticated,
                                                 @Nullable Runnable onDeviceInformationAcquired,
                                                 @Nullable SensorApiManager.SensorApiErrorListener errorListener) {
        final IotDevice device = new IotDevice(id, sn, null, this,
                onDeviceApiAuthenticated, onDeviceInformationAcquired, errorListener);
        iotDevices.add(device);
        return device;
    }

    public IotDevice newIotDevice(String id, String sn, String key,
                                  @Nullable Runnable onDeviceApiAuthenticated,
                                  @Nullable Runnable onDeviceInformationAcquired,
                                  @Nullable SensorApiManager.SensorApiErrorListener errorListener) {
        final IotDevice device = new IotDevice(id, sn, key, this,
                onDeviceApiAuthenticated, onDeviceInformationAcquired, errorListener);
        iotDevices.add(device);
        return device;
    }

    public void setDeveloperAuthToken(String developerAuthToken) {
        this.developerAuthToken = developerAuthToken;
    }
    public boolean hasDeveloperAuthToken() {
        return developerAuthToken != null;
    }

    void performRequest(Request request) {
        requestQueue.add(request);
    }

    void requestIotDeviceInformation(String id, String sn,
            final IotDevice.IotInformationResponseReceivedListener informationReceived)
            throws CannotUseDeveloperApiException {

        if (developerAuthToken == null) throw new CannotUseDeveloperApiException();

        SensorApiManager.AuthenticatedDeveloperJsonObjectRequest request =
                new AuthenticatedDeveloperJsonObjectRequest(Request.Method.GET,
                        SensorApiManager.DEVELOPER_GET_DEVICE_INFORMATION_ENDPOINT, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        informationReceived.onIotInformationResponseReceived(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        performRequest(request);
    }


    class AuthenticatedDeveloperJsonObjectRequest extends JsonObjectRequest {

        private final Map<String, String> headers = new HashMap<>();

        AuthenticatedDeveloperJsonObjectRequest(int method, String url,
                                      Response.Listener<JSONObject> listener,
                                      Response.ErrorListener errorListener) {
            super(method, url, null, listener, errorListener);
            headers.put(DEVELOPER_AUTH_HEADER, "Basic " + developerAuthToken);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }

    }

    public static class CannotUseDeveloperApiException extends Exception {}
    public static class CannotUseDeviceApiException extends Exception {}
    public static class SensorApiException extends Exception {}

    public interface SensorApiErrorListener {
        void onError(String details);
    }

}
