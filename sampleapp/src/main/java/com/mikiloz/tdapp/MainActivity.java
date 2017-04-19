package com.mikiloz.tdapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mikiloz.tdgateway.IotDevice;
import com.mikiloz.tdgateway.IotMessage;
import com.mikiloz.tdgateway.SensorApiManager;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private IotDevice iotDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SensorApiManager sensorApiManager = new SensorApiManager(this);
        /*device = sensorApiManager.newDevice("E4A3", "CFE3A995");
        device.initialize(new TdDevice.DeviceReadyListener() {
            @Override
            public void onDeviceReady() {
                System.out.println("Device initialized");
            }
        }, null);*/

        iotDevice = sensorApiManager.newIotDeviceForDeviceApi("E4A3", "CFE3A995",
                new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("");
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Iot Information acquired");
                    }
                },
                new SensorApiManager.SensorApiErrorListener() {
                    @Override
                    public void onError(String details) {
                        System.out.println("Error");
                    }
                }
        );

        Button retrieveMessagesButton = (Button) findViewById(R.id.retrieve_messages_button);
        retrieveMessagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMessages();
            }
        });

    }

    private void getMessages() {

        try {
            iotDevice.
            iotDevice.getMessagesHistory(1, null, new IotDevice.IotMessagesReceivedListener() {
                @Override
                public void onMessagesReceived(List<IotMessage> messages) {
                    for (int i = 0; i < messages.size(); i++) {
                        IotMessage message = messages.get(i);
                        System.out.println("Message sent on " + message.getEmissionDate().toString()
                                + ", with contents \"" + Arrays.toString(message.getPayload())
                                + "\".");
                    }
                }
            }, new SensorApiManager.SensorApiErrorListener() {
                @Override
                public void onError(String details) {
                    System.err.println("An error occurred: " + details);
                }
            });
        } catch (SensorApiManager.CannotUseDeviceApiException e) {
            Toast.makeText(MainActivity.this, "Device is not ready yet.", Toast.LENGTH_SHORT).show();
        }
    }

}
