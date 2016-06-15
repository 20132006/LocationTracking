package com.example.netlab.locationtracking;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    Boolean connected = false;


    //DatagramSocket rSocket = null;

    double last_update=0.0;

    DatagramPacket rPacket = null;
    byte[] rMessage = new byte[1000];


    DatagramSocket gSocket = null;


    private User MyState = new User();

    public void startServerConnection() throws IOException {
        if (checkConnection() == true) {

            try {
                gSocket = new DatagramSocket(); // startServerConnection
            } catch (SocketException e) {
                e.printStackTrace();
            }

            sendRegisterRequest();

        } else {
            Toast.makeText(getApplicationContext(), "No WiFi Connection", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkConnection() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected())
        {
            MyState.getLocalIpAddress();
            return true;
        } else {
            return false;
        }
    }



    private void sendRegisterRequest() throws IOException
    {
        final CharSequence text;
        text = "Sensor_Device;New User;";
        MyState.send(gSocket, text.toString());

        Thread thread = new Thread(){
            public void run()
            {
                while (!connected)
                {
                    rPacket = new DatagramPacket(rMessage, rMessage.length);
                    try {
                        gSocket.receive(rPacket); //Received means we are connected to server
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    connected = true;
                }
            }
        };

        thread.start();
    }



    double[] gravity;
    double[] linear_acceleration;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gravity = new double[3];
        linear_acceleration = new double[3];

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        try {
            startServerConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.



        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        EditText editTextx = (EditText) findViewById(R.id.xaxis);
        EditText editTexty = (EditText) findViewById(R.id.yaxis);
        EditText editTextz = (EditText) findViewById(R.id.zaxis);

        editTextx.setText(String.valueOf(linear_acceleration[0]));
        editTexty.setText(String.valueOf(linear_acceleration[1]));
        editTextz.setText(String.valueOf(linear_acceleration[2]));


        if ( last_update -  System.currentTimeMillis() >= 500 && Math.max( Math.abs(linear_acceleration[0]),Math.abs(linear_acceleration[2]) ) > 1.5)
        {
            if ( Math.abs(linear_acceleration[0]) < Math.abs(linear_acceleration[2]) )
            {
                Toast.makeText(getApplicationContext(),"Moving", Toast.LENGTH_SHORT).show();
                MyState.sendLocation(gSocket, "Sensor_Device;Z-Location;" + String.valueOf(linear_acceleration[2]));
            }
            else
            {
                if (linear_acceleration[0] > 0 )
                {
                    Toast.makeText(getApplicationContext(),"Moving", Toast.LENGTH_SHORT).show();
                    MyState.sendLocation(gSocket, "Sensor_Device;X-Location_L;" + String.valueOf(linear_acceleration[2]));
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Moving", Toast.LENGTH_SHORT).show();
                    MyState.sendLocation(gSocket, "Sensor_Device;X-Location_R;" + String.valueOf(linear_acceleration[2]));
                }
            }
            last_update = System.currentTimeMillis();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

}
