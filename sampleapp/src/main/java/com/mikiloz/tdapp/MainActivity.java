package com.mikiloz.tdapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mikiloz.tdgateway.IotMessage;
import com.mikiloz.tdgateway.TdApiManager;
import com.mikiloz.tdgateway.TdDevice;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private TdDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TdApiManager tdApiManager = new TdApiManager(this);
        device = tdApiManager.newDevice("E4A3", "CFE3A995");
        device.initialize(new TdDevice.DeviceReadyListener() {
            @Override
            public void onDeviceReady() {
                System.out.println("Device initialized");
            }
        }, null);

        Button retrieveMessagesButton = (Button) findViewById(R.id.retrieve_messages_button);
        retrieveMessagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMessages();
            }
        });

    }

    private void getMessages() {
        if (device.isReady()) {
            device.getMessagesHistory(10, null, new TdDevice.MessagesReceivedListener() {
                @Override
                public void onMessagesReceived(List<IotMessage> messages) {
                    for (int i = 0; i < messages.size(); i++) {
                        IotMessage message = messages.get(i);
                        System.out.println("Message sent on " + message.getEmissionDate().toString()
                                + ", with contents \"" + Arrays.toString(message.getPayload())
                                + "\".");
                    }
                }
            }, new TdDevice.ErrorListener() {
                @Override
                public void onError(String details) {
                    System.err.println("An error occurred: " + details);
                }
            });
        } else Toast.makeText(MainActivity.this, "Device is not ready yet.", Toast.LENGTH_SHORT).show();
    }

}
