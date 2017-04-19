package com.mikiloz.tdgateway;

import android.support.annotation.NonNull;

import java.util.Locale;


class Util {

    @NonNull
    static String populateUrlWithParams(String url, String[] params, String[] values)
            throws InvalidParamsAndValuesLengthException {

        if (params.length != values.length) throw new InvalidParamsAndValuesLengthException();
        StringBuilder sb = new StringBuilder(url);
        for (int i = 0; i < params.length; i++) {
            boolean firstParameter = sb.toString().equals(url);
            if (values[i] != null)
                sb.append(firstParameter ? "?" : "&").append(params[i]).append("=").append(values[i]);
        } return sb.toString();

    }

    static String gmtNumberToString(int gmt) {
        String str = "GMT" + (gmt >= 0 ? "+" : "-") + String.format(Locale.US, "%02d", Math.abs(gmt))
                + ":00";
        return str;
    }

    static class InvalidParamsAndValuesLengthException extends Exception {}
}
