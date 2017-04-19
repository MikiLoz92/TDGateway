package com.mikiloz.tdgateway;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


/**
 * A special {@link com.android.volley.Request} class for the Telecom Design Sensor API, because
 * those fools can't decide whether returning JSON objects or arrays on an API call.
 */
class SensorJsonRequest extends JsonRequest<JSONArray> {

    public SensorJsonRequest(int method, String url, Response.Listener<JSONArray> listener,
                             Response.ErrorListener errorListener) {
        super(method, url, null, listener, errorListener);
    }


    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            if (jsonString.startsWith("[")) return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
            else return Response.success(new JSONObject(jsonString).getJSONArray("items"),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

}
