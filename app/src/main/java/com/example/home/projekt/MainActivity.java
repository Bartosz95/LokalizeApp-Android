package com.example.home.projekt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private Toast toast;

    private Location location;
    private Database db;
    private ArrayList<ArrayList<String>> listContacts;

    private Button btnEditSendMessage, btnEditContacts, btnSendMessage, btnAlarm, btnListening;

    public boolean listening = false, alarm = false;
    private AlarmLoop alarmLoop;
    private ListeningLoop listeningLoop;
    private Intent intentAlarm, intentListening;
    private boolean boundAlarm = false, boundListening = false;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        location = new Location(this);
        db = new Database(this);
        handler = new Handler(getApplicationContext().getMainLooper());

        btnEditSendMessage = (Button) findViewById(R.id.btnEditSendMessage);
        btnEditSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Statement.class));
            }
        });

        btnEditContacts = (Button) findViewById(R.id.btnEditContacts) ;
        btnEditContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Contacts.class));
            }
        });

        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    sendMessage();
            }
        });

        btnListening = (Button) findViewById(R.id.btnListening);
        btnListening.setText("Start listening");
        btnListening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopListening();
            }
        });

        btnAlarm = (Button) findViewById(R.id.btnAlarm);
        btnAlarm.setText("Start Alarm");
        btnAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listening) startStopListening();
                startStopAlarm();
            }
        });
    }

    private void toastMessage(String text) {
        toast.setText(text);
        toast.show();
    }

    // fcn activated/deactivated alarm
    private void startStopAlarm(){
        if(alarm){
            stopService(intentAlarm);
            btnAlarm.setText("Start Alarm");
            toastMessage("Alarm stopped");
            stop();
            alarm = false;
        } else { // wystartowanie alarmu
            sendMessage();
            intentAlarm = new Intent(MainActivity.this, AlarmLoop.class);
            bindService(intentAlarm, alarmServiceConnection, Context.BIND_AUTO_CREATE);
            startService(intentAlarm);
            btnAlarm.setText("Stop Alarm");
            toastMessage("Alarm started");
            alarm = true;
        }
        deactivatedButtons();
    }

    // fcn activated/deactivated listening
    private void startStopListening(){
        if(listening){ // wylaczenie nasluchiwania
            stopService(intentListening);
            btnListening.setText("Start listening");
            toastMessage("Listening stopped");
            stop();
            listening = false;
        } else {
            intentListening = new Intent(MainActivity.this, ListeningLoop.class);
            bindService(intentListening, listeningServiceConnection, Context.BIND_AUTO_CREATE);
            startService(intentListening);
            btnListening.setText("Stop listening");
            toastMessage("Listening started");
            listening = true;
        }
        deactivatedButtons();
    }

    private void deactivatedButtons(){
        if(alarm || listening){
            btnEditContacts.setEnabled(false);
            btnSendMessage.setEnabled(false);
            btnEditSendMessage.setEnabled(false);
        } else {
            btnEditContacts.setEnabled(true);
            btnSendMessage.setEnabled(true);
            btnEditSendMessage.setEnabled(true);
        }
        if(alarm){
            btnListening.setEnabled(false);
        } else {
            btnListening.setEnabled(true);
        }

    }

    public void startAlarm(){
        toastMessage("Alarm");
        fcn();

    }

    private void fcn(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    toastMessage("FCN ");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(listening) startStopListening();
                            startStopAlarm();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void sendMessage() {
        listContacts = db.getPhoneNumbersList();
        StringBuffer message = new StringBuffer("You send message to:");
        for(int i=0;i<listContacts.size();i++){
            message.append("\n").append(listContacts.get(i).get(1));
            Sms.SendMessage(String.format("%s My localization is: %s",db.getStatement(),location.getLastLocationString()), listContacts.get(i).get(2) );
        }
        toastMessage(message.toString());
    }

    protected void stop() {
        if (boundAlarm) {
            AlarmLoop.setCallbacks(null);
            unbindService(alarmServiceConnection);
            boundAlarm = false;
        }
        if (boundListening) {
            ListeningLoop.setCallbacks(null);
            unbindService(listeningServiceConnection);
            boundListening = false;
        }
    }

    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection alarmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            AlarmLoop.LocalBinder binder = (AlarmLoop.LocalBinder) service;
            alarmLoop = binder.getService();
            boundAlarm = true;
            alarmLoop.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            boundAlarm = false;
        }
    };

    private ServiceConnection listeningServiceConnection= new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ListeningLoop.LocalBinder binder1 = (ListeningLoop.LocalBinder) service;
            listeningLoop = binder1.getService();
            boundListening = true;
            listeningLoop.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            boundListening = false;
        }
    };
}

