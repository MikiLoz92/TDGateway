package com.mikiloz.tdgateway;

import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mikiloz.tdgateway.TdApiManager.DEVICE_AUTHENTICATION_ENDPOINT;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_MESSAGES_HISTORY_ENDPOINT;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT;
import static com.mikiloz.tdgateway.TdApiManager.DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS;


public class TdDevice {

    private String sn, key;
    private String authToken;
    private TdApiManager tdApiManager;

    private static final String AUTH_HEADER = "X-Snsr-Device-Key";

    /**
     * Default constructor, stores the serial number and key information for this device.
     * @param sn The serial number.
     * @param key The device key.
     */
    TdDevice(String sn, String key, TdApiManager tdApiManager) {
        this.sn = sn;
        this.key = key;
        this.tdApiManager = tdApiManager;
    }

    /**
     * Obtains this device's authentication token, so you can perform any query that you want.
     * @param deviceReadyListener A listener that executes when the device is ready.
     * @param authErrorListener A listener that will execute if there's any error.
     */
    public void initialize(@Nullable final DeviceReadyListener deviceReadyListener,
                           @Nullable final AuthErrorListener authErrorListener) {

        String endpoint = String.format(DEVICE_AUTHENTICATION_ENDPOINT, sn, key);
        StringRequest deviceTokenRequest = new StringRequest(Request.Method.GET, endpoint,
        new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                authToken = response;
                if (deviceReadyListener != null) deviceReadyListener.onDeviceReady();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (authErrorListener != null)
                    authErrorListener.onDeviceAuthError();
            }
        });

        System.out.println("Sup");
        tdApiManager.performRequest(deviceTokenRequest);

    }

    /**
     * Get a certain amount of the oldest messages until a certain Date, in descending order (from
     * newest to oldest). E.g.: Supposing that you have a message per day, if you set {@code amount}
     * to 7 and {@code until} to a week ago, you would get the messages starting from a week ago and
     * ending at two weeks ago.
     * @param amount The amount of messages to retrieve, use 0 to use the default API value.
     * @param until Until this {@link Date}. Use {@code null} for the current instant.
     * @param messagesReceivedListener A functional interface to execute whenever the messages have
     *                                 been received.
     * @param errorListener An error listener.
     */
    public void getMessagesHistory(int amount, @Nullable Date until,
                                   final MessagesReceivedListener messagesReceivedListener,
                                   final ErrorListener errorListener) {

        String[] params = new String[2];
        params[0] = amount == 0 ? null : String.valueOf(amount);
        params[1] = until == null ? null : String.valueOf(until.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_MESSAGES_HISTORY_ENDPOINT,
                    DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedJsonArrayRequest request = new AuthenticatedJsonArrayRequest(Request.Method.GET,
                endpoint, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                List<IotMessage> messages = new ArrayList<>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        IotMessage message = parseIotMessage(response.getJSONObject(i));
                        messages.add(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorListener.onError("Error parsing JSON array response");
                }
                messagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        tdApiManager.performRequest(request);

    }

    /**
     * Get a certain amount of the latest messages until a certain Date, in descending order (from
     * newest to oldest). E.g.: Supposing that you have a message per day, if you set {@code amount}
     * to 7 and {@code until} to a week ago, you would get the messages starting from a week ago and
     * ending at today.
     * @param amount The amount of messages to retrieve, use 0 to use the default API value.
     * @param after Starting from this {@link Date}. Use {@code null} for the current instant.
     * @param messagesReceivedListener A functional interface to execute whenever the messages have
     *                                 been received.
     * @param errorListener An error listener.
     */
    public void getLatestMessages(int amount, @Nullable Date after,
                                  final MessagesReceivedListener messagesReceivedListener,
                                  final ErrorListener errorListener) {

        String[] params = new String[2];
        params[0] = amount == 0 ? null : String.valueOf(amount);
        params[1] = after == null ? null : String.valueOf(after.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT,
                    DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedJsonArrayRequest request = new AuthenticatedJsonArrayRequest(Request.Method.GET,
                endpoint, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                List<IotMessage> messages = new ArrayList<>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        IotMessage message = parseIotMessage(response.getJSONObject(i));
                        messages.add(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorListener.onError("Error parsing JSON array response");
                }
                messagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        tdApiManager.performRequest(request);

    }

    public void getRawMessagesHistory(int limit, Date before,
                                      final MessagesReceivedListener messagesReceivedListener,
                                      final ErrorListener errorListener) {

        String[] params = new String[2];
        params[0] = limit == 0 ? null : String.valueOf(limit);
        params[1] = before == null ? null : String.valueOf(before.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT,
                    DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedJsonArrayRequest request = new AuthenticatedJsonArrayRequest(Request.Method.GET,
                endpoint, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                List<IotMessage> messages = new ArrayList<>();
                try {
                    System.out.println("received response: " + response);
                    for (int i = 0; i < response.length(); i++) {
                        /*IotMessage message = parseIotMessage(messageArray.getJSONObject(i));
                        messages.add(message);*/
                        System.out.println(response.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (errorListener != null)
                        errorListener.onError("Error parsing JSON array response");
                }
                if (messagesReceivedListener != null)
                    messagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                if (errorListener != null)
                    errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        tdApiManager.performRequest(request);

    }

    /**
     * Get all the associated children devices behind this device, if it's a GW module.
     * @param devicesReceivedListener A functional interface to execute whenever the list of devices
     *                                has been received.
     * @param errorListener An error listener.
     */
    public void getDevices(final DevicesReceivedListener devicesReceivedListener,
                           final ErrorListener errorListener) {

        String endpoint = TdApiManager.DEVICE_CHILD_DEVICES_ENDPOINT;
        AuthenticatedJsonArrayRequest request =
                new AuthenticatedJsonArrayRequest(Request.Method.GET, endpoint,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                System.out.println(response.toString());
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        tdApiManager.performRequest(request);

    }

    /**
     * Clears all messages within this device.
     * @param successListener
     * @param errorListener
     */
    public void clearMessages(final Runnable successListener, final ErrorListener errorListener) {
        String endpoint = TdApiManager.DEVICE_CLEAR_MESSAGES_ENDPOINT;
        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        tdApiManager.performRequest(request);
    }

    /**
     * Returns whether this device is ready or not to perform queries to the TD API.
     * @return whether this device has connected and obtained and authentication token from the TD
     * API.
     */
    public boolean isReady() {
        return authToken != null;
    }

    private IotMessage parseIotMessage(JSONObject object) throws JSONException {
        IotMessage message = new IotMessage();
        message.received = new Date(object.getLong("received"));
        message.when = new Date(object.getLong("when"));
        message.payload = object.has("payload") ? object.getString("payload").getBytes() : null;
        return message;
    }

    // region Listeners

    public interface DeviceReadyListener {
        void onDeviceReady();
    }

    public interface AuthErrorListener {
        void onDeviceAuthError();
    }

    public interface ErrorListener {
        void onError(String details);
    }

    public interface MessagesReceivedListener {
        void onMessagesReceived(List<IotMessage> messages);
    }

    public interface DevicesReceivedListener {
        void onDevicesReceived(List<TdDevice> devices);
    }

    // endregion

    /*public class AuthenticatedJsonArrayRequest extends JsonArrayRequest {

        private final Map<String, String> headers = new HashMap<>();

        public AuthenticatedJsonArrayRequest(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(url, listener, errorListener);
            initialize();
        }
        public AuthenticatedJsonArrayRequest(int method, String url, JSONArray jsonRequest, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
            initialize();
        }

        void initialize() {
            headers.put(AUTH_HEADER, authToken);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }
    }

    public class AuthenticatedJsonObjectRequest extends JsonObjectRequest {

        private final Map<String, String> headers = new HashMap<>();

        public AuthenticatedJsonObjectRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
            initialize();
        }
        public AuthenticatedJsonObjectRequest(String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(url, jsonRequest, listener, errorListener);
            initialize();
        }

        void initialize() {
            headers.put(AUTH_HEADER, authToken);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }
    }*/

    class AuthenticatedJsonArrayRequest extends TdJsonRequest {

        private final Map<String, String> headers = new HashMap<>();

        AuthenticatedJsonArrayRequest(int method, String url,
                                             Response.Listener<JSONArray> listener,
                                             Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            headers.put(AUTH_HEADER, authToken);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }

    }

    class AuthenticatedStringRequest extends StringRequest {

        private final Map<String, String> headers = new HashMap<>();

        AuthenticatedStringRequest(int method, String url,
                                          Response.Listener<String> listener,
                                          Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            headers.put(AUTH_HEADER, authToken);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }
    }

}
