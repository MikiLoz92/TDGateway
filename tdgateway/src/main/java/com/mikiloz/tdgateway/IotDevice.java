package com.mikiloz.tdgateway;


import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_AUTHENTICATION_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_CHANGE_STATUS_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_CHANGE_STATUS_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_MESSAGES_HISTORY_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_ACTIVE_FLAG_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_ACTIVE_FLAG_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_BIDIR_VALUE_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_BIDIR_VALUE_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_MONITORING_FLAG_ENDPOINT;
import static com.mikiloz.tdgateway.SensorApiManager.DEVICE_SET_MONITORING_FLAG_ENDPOINT_PARAMS;
import static com.mikiloz.tdgateway.SensorApiManager.GMT_TIMEZONE;
import static com.mikiloz.tdgateway.SensorApiManager.dateFormat;
import static com.mikiloz.tdgateway.Util.gmtNumberToString;

/**
 * This class represents a Telecom Design Sensor API IoT device.
 */
public class IotDevice {

    public enum PowerStatus { ON, OFF, UNKNOWN }
    public enum TemperatureStatus { LOW, OK, HIGH, UNKNOWN }

    private String id, uid, sn, key, gateway;
    private String category;
    private int index;
    private Date firstSeen, lastSeen;
    private boolean active;
    private String status;
    private int messageCount, lostCount;
    private PowerStatus networkStatus, batteryStatus, tamperStatus;
    private TemperatureStatus temperatureStatus;

    private Date lastInformationRetrievalDate;

    private SensorApiManager sensorApiManager;
    private static final String AUTH_HEADER = "X-Snsr-Device-Key";
    private String authToken;

    private IotDevice() {}

    IotDevice(@Nullable String id, String sn, @Nullable String key, SensorApiManager sensorApiManager) {
        this(id, sn, key, sensorApiManager, null, null, null);
    }

    IotDevice(@Nullable String id, String sn, @Nullable String key, SensorApiManager sensorApiManager,
              @Nullable Runnable onDeviceApiAuthenticated,
              @Nullable Runnable onDeviceInformationAcquired,
              @Nullable SensorApiManager.SensorApiErrorListener errorListener) {
        this.id = id;
        this.sn = sn;
        this.key = key;
        this.sensorApiManager = sensorApiManager;
        try { obtainIotDeviceInformation(onDeviceInformationAcquired); }
        catch (SensorApiManager.CannotUseDeveloperApiException e) { }
        obtainAuthToken(onDeviceApiAuthenticated, errorListener);
    }

    /**
     * Obtain this device's information from the Developer API.
     * @param onDeviceInformationAcquired Called when the retrieval process was successful.
     * @throws SensorApiManager.CannotUseDeveloperApiException When the {@link SensorApiManager} is missing
     * the Developer authentication token. Use {@link SensorApiManager#setDeveloperAuthToken(String)} on
     * your API manager to set a valid developer token.
     */
    public void obtainIotDeviceInformation(final Runnable onDeviceInformationAcquired)
            throws SensorApiManager.CannotUseDeveloperApiException {

        sensorApiManager.requestIotDeviceInformation(id, sn, new IotInformationResponseReceivedListener() {
            @Override
            public void onIotInformationResponseReceived(JSONObject response) {
                parseIotDeviceInformation(response, IotDevice.this);
                onDeviceInformationAcquired.run();
            }
        });
    }

    /**
     * Obtain an authentication token from the Device API, so that this device can perform queries
     * to it.
     * @param onDeviceApiAuthenticated Called when the authentication process was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws MissingInformationException When the device serial number or device key are missing.
     */
    public void obtainAuthToken(@Nullable final Runnable onDeviceApiAuthenticated,
                                 @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws MissingInformationException {

        if (sn == null || key == null) throw new MissingInformationException();

        String endpoint = String.format(DEVICE_AUTHENTICATION_ENDPOINT, sn, key);
        StringRequest deviceTokenRequest = new StringRequest(Request.Method.GET, endpoint,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        authToken = response;
                        if (onDeviceApiAuthenticated != null)
                            onDeviceApiAuthenticated.run();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null)
                    errorListener.onError("Couldn't authenticate device " + sn + ".");
            }
        });
        sensorApiManager.performRequest(deviceTokenRequest);
    }

    private static void parseIotDeviceInformation(JSONObject jsonObject, IotDevice device) {

        try { device.id = jsonObject.getString("id"); } catch (JSONException ignored) {}
        try { device.uid = jsonObject.getString("uid"); } catch (JSONException ignored) {}
        try { device.sn = jsonObject.getString("serial"); } catch (JSONException ignored) {}
        try { device.gateway = jsonObject.getString("gateway"); } catch (JSONException ignored) {}
        try { device.category = jsonObject.getString("category"); } catch (JSONException ignored) {}
        try { device.index = jsonObject.getInt("index"); } catch (JSONException ignored) {}
        try { device.firstSeen = dateFormat.parse(jsonObject.getString("firstseen")
                + " " + Util.gmtNumberToString(GMT_TIMEZONE)); }
        catch (JSONException | ParseException ignored) {}
        try { device.lastSeen = dateFormat.parse(jsonObject.getString("lastseen")
                + " " + Util.gmtNumberToString(GMT_TIMEZONE)); }
        catch (JSONException | ParseException ignored) {}
        try { device.active = jsonObject.getBoolean("active"); } catch (JSONException ignored) {}
        try { device.status = jsonObject.getString("status"); } catch (JSONException ignored) {}
        try { device.messageCount = jsonObject.getInt("msgs"); } catch (JSONException ignored) {}
        try { device.lostCount = jsonObject.getInt("losts"); } catch (JSONException ignored) {}
        try {
            String value = jsonObject.getString("network");
            device.networkStatus = PowerStatus.valueOf(value);
        } catch (JSONException | IllegalArgumentException ignored) {}
        try {
            String value = jsonObject.getString("battery");
            device.batteryStatus = PowerStatus.valueOf(value);
        } catch (JSONException | IllegalArgumentException ignored) {}
        try {
            String value = jsonObject.getString("tamper");
            device.tamperStatus = PowerStatus.valueOf(value);
        } catch (JSONException | IllegalArgumentException ignored) {}
        try {
            String value = jsonObject.getString("temp");
            device.temperatureStatus = TemperatureStatus.valueOf(value);
        } catch (JSONException | IllegalArgumentException ignored) {}

        device.lastInformationRetrievalDate = new Date();
    }

    /**
     * Returns the last {@link Date} when this device's information was acquired, or {@code null}
     * if it was never obtained.
     * @return The last {@link Date} when this device's information was acquired, or {@code null}
     * if it was never obtained.
     */
    public Date lastInformationRetrievalDate() {
        return lastInformationRetrievalDate;
    }

    /**
     * Returns whether this device is ready or not to perform queries to the TD API.
     * @return Whether this device has connected and obtained and authentication token from the TD
     * API.
     */
    public boolean canUseDeviceApi() {
        return sn != null && key != null && authToken != null;
    }


    //region API calls

    /**
     * Get a certain amount of the oldest messages until a certain Date, in descending order (from
     * newest to oldest). E.g.: Supposing that you have a message per day, if you set {@code amount}
     * to 7 and {@code until} to a week ago, you would get the messages starting from a week ago and
     * ending at two weeks ago.
     * @param amount The amount of messages to retrieve, use 0 to use the default API value.
     * @param until Until this {@link Date}. Use {@code null} for the current instant.
     * @param iotMessagesReceivedListener A functional interface to execute whenever the messages
     *                                    have been received.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws SensorApiManager.CannotUseDeviceApiException When there is no authentication token for
     * this device. Call {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} to
     * get a valid authentication token.
     */
    public void getMessagesHistory(int amount, @Nullable Date until,
                                   final IotMessagesReceivedListener iotMessagesReceivedListener,
                                   @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws SensorApiManager.CannotUseDeviceApiException {

        if (sn == null || key == null || authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();

        String[] params = new String[2];
        params[0] = amount == 0 ? null : String.valueOf(amount);
        params[1] = until == null ? null : String.valueOf(until.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_MESSAGES_HISTORY_ENDPOINT,
                    DEVICE_MESSAGES_HISTORY_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
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
                    if (errorListener != null)
                        errorListener.onError("Error parsing JSON array response");
                }
                iotMessagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null)
                    errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        sensorApiManager.performRequest(request);

    }

    /**
     * Get a certain amount of the latest messages until a certain Date, in descending order (from
     * newest to oldest). E.g.: Supposing that you have a message per day, if you set {@code amount}
     * to 7 and {@code until} to a week ago, you would get the messages starting from a week ago and
     * ending at today.
     * @param amount The amount of messages to retrieve, use 0 to use the default API value.
     * @param after Starting from this {@link Date}. Use {@code null} for the current instant.
     * @param iotMessagesReceivedListener A functional interface to execute whenever the messages have
     *                                 been received.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws SensorApiManager.CannotUseDeviceApiException When there is no authentication token for
     * this device. Call {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} to
     * get a valid authentication token.
     */
    public void getLatestMessages(int amount, @Nullable Date after,
                                  final IotMessagesReceivedListener iotMessagesReceivedListener,
                                  @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws SensorApiManager.CannotUseDeviceApiException {

        if (sn == null || key == null || authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();

        String[] params = new String[2];
        params[0] = amount == 0 ? null : String.valueOf(amount);
        params[1] = after == null ? null : String.valueOf(after.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT,
                    DEVICE_MESSAGES_HISTORY_LATEST_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
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
                    if (errorListener != null)
                        errorListener.onError("Error parsing JSON array response");
                }
                iotMessagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null)
                    errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        sensorApiManager.performRequest(request);

    }

    /**
     * Get the RAW messages received from this device.
     * @param limit The limit of raw messages to be retrieved, use 0 to use the default API value.
     * @param before The date as end date, Use {@code null} for the current instant.
     * @param rawMessagesReceivedListener A functional interface to execute whenever the messages
     *                                    have been received.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws SensorApiManager.CannotUseDeviceApiException When there is no authentication token for
     * this device. Call {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} to
     * get a valid authentication token.
     */
    public void getRawMessagesHistory(int limit, Date before,
                                      final RawMessagesReceivedListener rawMessagesReceivedListener,
                                      @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws SensorApiManager.CannotUseDeviceApiException {

        if (sn == null || key == null || authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();

        String[] params = new String[2];
        params[0] = limit == 0 ? null : String.valueOf(limit);
        params[1] = before == null ? null : String.valueOf(before.getTime());

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT,
                    DEVICE_RAW_MESSAGES_HISTORY_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
                errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedJsonArrayRequest request = new AuthenticatedJsonArrayRequest(Request.Method.GET,
                endpoint, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                List<RawMessage> messages = new ArrayList<>();
                try {
                    System.out.println("received response: " + response);
                    for (int i = 0; i < response.length(); i++) {
                        RawMessage message = parseRawMessage(response.getJSONObject(i));
                        messages.add(message);
                        System.out.println(response.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (errorListener != null)
                        errorListener.onError("Error parsing JSON array response");
                }
                if (rawMessagesReceivedListener != null)
                    rawMessagesReceivedListener.onMessagesReceived(messages);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                if (errorListener != null)
                    errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        sensorApiManager.performRequest(request);

    }

    /**
     * Get all the associated children devices behind this device, if it's a GW module. These
     * devices are populated with data, but haven't got a valid authentication token. Call
     * {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} if you want to
     * perform queries to the Device API using these {@link IotDevice} instances.
     * @param devicesReceivedListener A functional interface to execute whenever the list of devices
     *                                has been received.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws SensorApiManager.CannotUseDeviceApiException When there is no authentication token for
     * this device. Call {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} to
     * get a valid authentication token.
     */
    public void getDevices(final DevicesReceivedListener devicesReceivedListener,
                           @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws SensorApiManager.CannotUseDeviceApiException {

        if (sn == null || key == null || authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();

        String endpoint = SensorApiManager.DEVICE_CHILD_DEVICES_ENDPOINT;
        AuthenticatedJsonArrayRequest request =
                new AuthenticatedJsonArrayRequest(Request.Method.GET, endpoint,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                List<IotDevice> devices = new ArrayList<>();
                                try {
                                    for (int i = 0; i < response.length(); i++) {
                                        JSONObject deviceObject = response.getJSONObject(i);
                                        IotDevice device = new IotDevice();
                                        device.sensorApiManager = sensorApiManager;
                                        parseIotDeviceInformation(deviceObject, device);
                                        devices.add(device);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (errorListener != null)
                            errorListener.onError("Error performing the query: " + error.getMessage());
                    }
                });
        sensorApiManager.performRequest(request);

    }

    /**
     * Clears all messages within this device.
     * @param successListener A functional interface to execute if the request was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws SensorApiManager.CannotUseDeviceApiException When there is no authentication token for
     * this device. Call {@link #obtainAuthToken(Runnable, SensorApiManager.SensorApiErrorListener)} to
     * get a valid authentication token.
     */
    public void clearMessages(final Runnable successListener,
                              @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws SensorApiManager.CannotUseDeviceApiException {

        if (authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();

        final String endpoint = SensorApiManager.DEVICE_CLEAR_MESSAGES_ENDPOINT;
        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null)
                    errorListener.onError("Error performing the query: " + error.getMessage());
            }
        });
        sensorApiManager.performRequest(request);
    }

    /**
     * Change the functional status of this device. You need to have provided a developer
     * authentication token to the {@link SensorApiManager} and acquired this device's ID using the
     * developer API in order for this method to work.
     * @param value The functional status. This is a user-designed tag.
     * @param successListener A functional interface to execute if the request was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws MissingInformationException If the {@code id} of this device is unknown.
     * This may be because the Developer authentication token was not set in the
     * {@link SensorApiManager}.
     */
    public void changeFunctionalStatus(@Nullable String value, final Runnable successListener,
                                       @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws MissingInformationException, SensorApiManager.CannotUseDeviceApiException {

        if (authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();
        if (sn == null || id == null) throw new MissingInformationException();

        String[] params = new String[2];
        params[0] = String.valueOf(id);
        params[1] = value == null ? "UNKNOWN" : value;

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_CHANGE_STATUS_ENDPOINT,
                    DEVICE_CHANGE_STATUS_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
                errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null) {
                    errorListener.onError("Error performing the query: " + error.getMessage());
                }
            }
        });
        sensorApiManager.performRequest(request);
    }

    /**
     * Set the "active" flag of this device. You need to have provided a developer authentication
     * token to the {@link SensorApiManager} and acquired this device's ID using the developer API
     * in order for this method to work.
     * @param value The value to set the "active" flag to.
     * @param successListener A functional interface to execute if the request was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws MissingInformationException If the {@code id} of this device is unknown.
     * This may be because the Developer authentication token was not set in the
     * {@link SensorApiManager}.
     */
    public void setActiveFlag(boolean value, final Runnable successListener,
                                       @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws MissingInformationException, SensorApiManager.CannotUseDeviceApiException {

        if (authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();
        if (sn == null || key == null || id == null) throw new MissingInformationException();

        String[] params = new String[2];
        params[0] = String.valueOf(id);
        params[1] = String.valueOf(sn);
        params[2] = value ? "true" : "false";

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_SET_ACTIVE_FLAG_ENDPOINT,
                    DEVICE_SET_ACTIVE_FLAG_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
                errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null) {
                    errorListener.onError("Error performing the query: " + error.getMessage());
                }
            }
        });
        sensorApiManager.performRequest(request);
    }

    /**
     * Set the "monitoring" flag of this device. You need to have provided a developer authentication
     * token to the {@link SensorApiManager} and acquired this device's ID using the developer API
     * in order for this method to work.
     * @param value The value to set the "monitoring" flag to.
     * @param successListener A functional interface to execute if the request was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws MissingInformationException If the {@code id} of this device is unknown.
     * This may be because the Developer authentication token was not set in the
     * {@link SensorApiManager}.
     */
    public void setMonitoringFlag(boolean value, final Runnable successListener,
                              @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws MissingInformationException, SensorApiManager.CannotUseDeviceApiException {

        if (authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();
        if (sn == null || key == null || id == null) throw new MissingInformationException();

        String[] params = new String[2];
        params[0] = String.valueOf(id);
        params[1] = String.valueOf(sn);
        params[2] = value ? "true" : "false";

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_SET_MONITORING_FLAG_ENDPOINT,
                    DEVICE_SET_MONITORING_FLAG_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
                errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null) {
                    errorListener.onError("Error performing the query: " + error.getMessage());
                }
            }
        });
        sensorApiManager.performRequest(request);
    }

    /**
     * Set the "bidir" value of this device. You need to have provided a developer authentication
     * token to the {@link SensorApiManager} and acquired this device's ID using the developer API
     * in order for this method to work.
     * @param value The value to set the "bidir" value to.
     * @param successListener A functional interface to execute if the request was successful.
     * @param errorListener An error listener. Use {@code null} if you want to ignore the error.
     * @throws MissingInformationException If the {@code id} of this device is unknown.
     * This may be because the Developer authentication token was not set in the
     * {@link SensorApiManager}.
     */
    public void setBidirValue(boolean value, final Runnable successListener,
                                  @Nullable final SensorApiManager.SensorApiErrorListener errorListener)
            throws MissingInformationException, SensorApiManager.CannotUseDeviceApiException {

        if (authToken == null) throw new SensorApiManager.CannotUseDeviceApiException();
        if (sn == null || key == null || id == null) throw new MissingInformationException();

        String[] params = new String[2];
        params[0] = String.valueOf(id);
        params[1] = String.valueOf(sn);
        params[2] = value ? "true" : "false";

        String endpoint;
        try {
            endpoint = Util.populateUrlWithParams(DEVICE_SET_BIDIR_VALUE_ENDPOINT,
                    DEVICE_SET_BIDIR_VALUE_ENDPOINT_PARAMS, params);
        } catch (Util.InvalidParamsAndValuesLengthException e) {
            e.printStackTrace();
            if (errorListener != null)
                errorListener.onError("TDGateway error, invalid parameters size.");
            return;
        }

        AuthenticatedStringRequest request = new AuthenticatedStringRequest(Request.Method.POST,
                endpoint, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                successListener.run();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorListener != null) {
                    errorListener.onError("Error performing the query: " + error.getMessage());
                }
            }
        });
        sensorApiManager.performRequest(request);
    }

    //endregion

    //region Helpers

    private IotMessage parseIotMessage(JSONObject object) throws JSONException {

        IotMessage iotMessage = new IotMessage();
        iotMessage.received = new Date(object.getLong("received") /*- GMT_TIMEZONE*3600*/);
        iotMessage.when = new Date(object.getLong("when") /*- GMT_TIMEZONE*3600*/);
        //message.payload = object.has("payload") ? object.getString("payload").getBytes() : null;
        iotMessage.payload = null;
        if (object.has("extra")) {
            JSONObject extra = object.getJSONObject("extra");
            String message = extra.has("message") ? extra.getString("message") : null;
            if (message != null && message.length() != 0 && message.length() % 2 == 0) {
                iotMessage.payload = hexStringToByteArray(message);
            }
        }
        iotMessage.jsonObject = object;
        return iotMessage;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private RawMessage parseRawMessage(JSONObject object) throws JSONException {
        RawMessage message = new RawMessage();
        message.device = object.getString("device");
        message.time = new Date(object.getLong("time"));
        message.payload = object.has("payload") ? object.getString("payload").getBytes() : null;
        message.snr = object.getDouble("snr");
        JSONArray rinfos = object.getJSONArray("rinfos");
        for (int i = 0; i < rinfos.length(); i++) {
            JSONObject rinfo = rinfos.getJSONObject(i);
            String tap = rinfo.getString("tap");
            double snr = rinfo.getDouble("snr");
            double rssi = rinfo.getDouble("rssi");
            message.rinfos.add(new RawMessage.ReceptionInfo(tap, snr, rssi));
        }
        message.jsonObject = object;
        return message;
    }

    //endregion

    //region Field getters

    public @Nullable String getId() {
        return id;
    }

    public @Nullable String getUid() {
        return uid;
    }

    public @Nullable String getSerial() {
        return sn;
    }

    public @Nullable String getKey() {
        return key;
    }

    public @Nullable String getGateway() {
        return gateway;
    }

    public @Nullable String getCategory() {
        return category;
    }

    public int getIndex() {
        return index;
    }

    public @Nullable Date getFirstSeenDate() {
        return firstSeen;
    }

    public @Nullable Date getLastSeenDate() {
        return lastSeen;
    }

    public boolean isActive() {
        return active;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public int getLostMessageCount() {
        return lostCount;
    }

    public @Nullable PowerStatus getNetworkStatus() {
        return networkStatus;
    }

    public @Nullable PowerStatus getBatteryStatus() {
        return batteryStatus;
    }

    public @Nullable PowerStatus getTamperStatus() {
        return tamperStatus;
    }

    public @Nullable TemperatureStatus getTemperatureStatus() {
        return temperatureStatus;
    }

    //endregion

    private class AuthenticatedJsonArrayRequest extends SensorJsonRequest {

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
    private class AuthenticatedStringRequest extends StringRequest {

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

    interface IotInformationResponseReceivedListener {
        void onIotInformationResponseReceived(JSONObject response);
    }
    public interface IotMessagesReceivedListener {
        void onMessagesReceived(List<IotMessage> messages);
    }
    public interface RawMessagesReceivedListener {
        void onMessagesReceived(List<RawMessage> messages);
    }
    public interface DevicesReceivedListener {
        void onDevicesReceived(List<IotDevice> devices);
    }

    public static class MissingInformationException extends RuntimeException {}

}
