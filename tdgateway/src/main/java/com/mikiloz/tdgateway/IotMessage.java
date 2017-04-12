package com.mikiloz.tdgateway;

import android.support.annotation.Nullable;

import java.util.Date;


public class IotMessage {

    Date when, received;
    byte[] payload;

    public Date getReceptionDate() {
        return when;
    }
    public Date getEmissionDate() {
        return received;
    }

    /**
     * Returns the message's payload.
     * @return The message's payload, will be null if it has no payload.
     */
    public @Nullable byte[] getPayload() {
        return payload;
    }

}
