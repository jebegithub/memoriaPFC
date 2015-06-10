package control.bolt.android.boltcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;

import javax.xml.transform.Result;

public class BluetoothConnexion {


    private BluetoothServerSocket bluetoothServerSocket;
    private final String LOG_TAG = "BluetoothConnexion" ;
    private final String SERVICE_NAME = "BOLT_BT_SERVER";
    private final String UUID_STRING = "6725bf90-b304-4fb8-a413-ca4d4f162d69";




    private boolean isInitialized = false;

    private final static int REQUEST_ENABLE_BT_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private Main context;

    public BluetoothConnexion(Main context) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Log.e(LOG_TAG, "Device does not support bluetooth");
        }
    }

    public boolean initialize(){
        if (!isInitialized) {
            if (bluetoothAdapter.isEnabled()) {
                Log.i(LOG_TAG, "Device address-> " + bluetoothAdapter.getAddress());
                UUID uuid = UUID.fromString(UUID_STRING);
                Log.i(LOG_TAG, "Service uuid-> " + uuid.toString());
                try {
                    bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, uuid);
                } catch (Exception e) {
                    Log.i(LOG_TAG, e.toString());
                }
            }
            else{
                Log.e(LOG_TAG,bluetoothAdapter.getState()+"--");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_CODE);
            }
            isInitialized = (bluetoothServerSocket != null);
        }
        return isInitialized;
    }

    public static int getRequestEnableBtCode() {
        return REQUEST_ENABLE_BT_CODE;
    }

    public String getBTAddress(){
        return bluetoothAdapter.getAddress();
    }


    public BluetoothServerSocket getBluetoothServerSocket() {
        return bluetoothServerSocket;
    }

    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

}
