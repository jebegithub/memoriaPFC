package control.bolt.android.boltcontrol;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AsyncConnexion extends AsyncTask<Void, String, Boolean> {

    private final String CONNECT_CMD = "connect";
    private final String CONNECT_ACK= "connectACK";
    private final String AUTH_REQ= "authRequest";

    private final String LOG_TAG = "AsyncConnexion" ;

    private boolean btListener = false;
    private boolean wifiListener = false;
    private boolean isBTBusy = true;
    private BluetoothConnexion bluetoothConnexion;
    private WifiConnexion wifiConnexion;
    private Main context;
    private String answer;
    private Boolean isAuthRequired;
    private BlockingQueue passQ;


    public AsyncConnexion (Main context){
        this.bluetoothConnexion = new BluetoothConnexion(context);
        this.wifiConnexion = new WifiConnexion(8082);
        this.context = context;
        this.passQ = new ArrayBlockingQueue<String>(1);
        //this.bluetoothConnexion.initialize();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        DatagramSocket wifiSocket;
        BluetoothSocket bluetoothSocket;
        DatagramPacket receivePacket;
        DatagramPacket sendPacket;
        String receivedMessage;

        byte[] responseBuffer;
        byte[] buffer;
        int bytes;

        while (btListener || wifiListener || isBTBusy) {
            receivedMessage = "";
            answer = "";
            bluetoothSocket = null;
            wifiSocket = null;
            if (btListener) {
                if (bluetoothConnexion.initialize()) {
                    bytes = 0;
                    buffer = new byte[1024];
                    try {
                        Log.i(LOG_TAG, "Waiting on Bluetooth socket");
                        bluetoothSocket = bluetoothConnexion.getBluetoothServerSocket().accept();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    if (bluetoothSocket != null) {
                        Log.i(LOG_TAG, "Socket communication accepted");
                        try {
                            bytes = bluetoothSocket.getInputStream().read(buffer);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                        receivedMessage = new String(buffer, 0, bytes);
                    }
                }
                else {
                    setBTBusy(true);
                }
            }
            if (wifiListener) {
                if (wifiConnexion.initialize()) {
                    wifiSocket = wifiConnexion.getSocket();
                    buffer = new byte[1024];
                    receivePacket = new DatagramPacket(buffer, buffer.length);
                    Log.i(LOG_TAG, "Waiting on WIFI socket ");
                    try {
                        wifiSocket.receive(receivePacket);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    if (receivePacket.getLength() > 0) {
                        receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    }

                }
            }
            if (receivedMessage != "") {
                receivedMessage = receivedMessage.replaceAll("\u0000", "");

                if (receivedMessage.equals(CONNECT_CMD) && bluetoothSocket != null) {
                    try {
                        responseBuffer = CONNECT_ACK.getBytes("UTF-8");
                        bluetoothSocket.getOutputStream().write(responseBuffer);
                        Log.i(LOG_TAG, "Received connection request, will try to connect");
                        context.getApTools().connectToAP();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (receivedMessage.equals(AUTH_REQ) && wifiSocket != null) {
                    Log.e(LOG_TAG, "Auth request received");
                    SharedPreferences preferences = context.getPreferences();
                    String duration = preferences.getString("pref_time", "3");
                    String remoteIp = preferences.getString("pref_remote_host", "10.0.0.1");
                    int remotePort = Integer.parseInt(preferences.getString("pref_remote_port", "8082"));
                    String alertType = preferences.getString("pref_notification", "Vibrate");
                    isAuthRequired = preferences.getBoolean("pref_auth_req", false);

                    if (alertType.equals("Vibrate") || alertType.equals("Both")) {
                        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(500);
                    }
                    if (alertType.equals("Beep") || alertType.equals("Both")) {
                        // send the tone to the "alarm" stream (classic beeps go there) with 50% volume
                        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500); // 200 is duration in ms
                    }

                    answer = "allow" + ">>"
                            + context.getApTools().getMacAddress() + ">>"
                            + bluetoothConnexion.getBTAddress() + ">>"
                            + duration;

                    publishProgress(receivedMessage);
                    if (isAuthRequired) {
                        try {
                            answer += ">>" + passQ.take();

                        }
                        catch (InterruptedException e) {
                            Log.e(LOG_TAG,e.toString());
                        }
                    }
                    try {
                        InetAddress remoteAddr = InetAddress.getByName(remoteIp);
                        sendPacket = new DatagramPacket(answer.getBytes(), answer.length(), remoteAddr, remotePort);
                        wifiSocket.send(sendPacket);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error sending answer through WIFI");
                        Log.e(LOG_TAG, e.toString());
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        String progressMessage = values[0];
        Log.i(LOG_TAG, "got progress message: " +  progressMessage);
        if(progressMessage.equals("open") || progressMessage.equals("close")){
            context.getGUIManager().setImageOpen(progressMessage.equals("open"));
        }
        if(progressMessage.equals(AUTH_REQ)){
            if (isAuthRequired){
                AlertDialog.Builder passwordAlertBuilder = new PasswordDialog();
                AlertDialog passwordAlert = passwordAlertBuilder.create();
                passwordAlert.show();
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.e(LOG_TAG, "cancelled");
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        Log.e(LOG_TAG, "postExecuted");
    }

    public void setListeners(boolean btListener, boolean wifiListener){
        this.btListener =  btListener;
        this.wifiListener = wifiListener;

       if (!btListener && bluetoothConnexion.getBluetoothServerSocket()!= null){
           try {
               bluetoothConnexion.getBluetoothServerSocket().close();
               bluetoothConnexion.setInitialized(false);
           } catch (IOException e) {
               Log.e(LOG_TAG, e.toString());
           }
       }
       if (!wifiListener && wifiConnexion.getSocket() !=null){
           wifiConnexion.setInitialized(false);
           wifiConnexion.getSocket().close();
           Log.i(LOG_TAG, "WIFI server socket closed");
       }
    }

    public void setBTBusy(boolean isBusy){
        btListener = !isBusy;
        isBTBusy = isBusy;
    }

    private class PasswordDialog extends AlertDialog.Builder {
    public PasswordDialog() {
        super(context);
        //We need an inflater to create a view based on layout xml file
        LayoutInflater li = LayoutInflater.from(context);
        //We need a view to display
        View passView =  li.inflate(R.layout.password, null);
        this.setView(passView);
        final EditText passField = (EditText)passView.findViewById(R.id.password_field);

        setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    passQ.put("");
                }
                catch (InterruptedException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
        });

        setPositiveButton(R.string.open_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            try {
                passQ.put(passField.getText().toString());
            }
            catch (InterruptedException e) {
                Log.e(LOG_TAG, e.toString());
            }
            }
        });
    }

    @Override
    public AlertDialog.Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
        return super.setPositiveButton(text, listener);
    }

    @Override
    public AlertDialog.Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
        return super.setNegativeButton(text, listener);
    }
}

}
