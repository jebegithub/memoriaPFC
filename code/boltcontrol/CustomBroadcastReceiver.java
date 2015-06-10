package control.bolt.android.boltcontrol;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class CustomBroadcastReceiver extends BroadcastReceiver {

    private APTools apTools;
    Main mainContext;
    AsyncConnexion serverTask;
    private static final String LOG_TAG = "NetBroadCastReceiver";

    public CustomBroadcastReceiver(Main context,AsyncConnexion serverTask){
        this.apTools = context.getApTools();
        this.mainContext = context;
        this.serverTask = serverTask;
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        WifiInfo wifiInfo;
        SupplicantState supplicantState;

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            apTools.setIsConnectedToAP(false);
            wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo!= null){
                supplicantState = wifiInfo.getSupplicantState();
                String ssid = wifiInfo.getSSID();
                int linkSpeed = wifiInfo.getLinkSpeed();
                if (supplicantState.equals(SupplicantState.COMPLETED) || supplicantState.equals(SupplicantState.DISCONNECTED)){
                    if (supplicantState.equals(SupplicantState.COMPLETED)
                            && linkSpeed > -1
                            && ssid.equals("\"" + apTools.getNetworkSSID() + "\"")
                            && !apTools.getIsConnectedToAP()) {
                        apTools.setIsConnectedToAP(true);
                        serverTask.setListeners(false, true);
                        mainContext.getGUIManager().changeBTIcon(0);
                    }
                    else{
                        serverTask.setListeners(true, false);
                        mainContext.getGUIManager().changeBTIcon(1);

                    }
                }
                apTools.checkConnexionToAP();
            }
        }

        else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON){
                Log.i(LOG_TAG, "Bluetooth turned on ");
                serverTask.setBTBusy(false);
            }
            if (state == BluetoothAdapter.STATE_OFF){
                Log.i(LOG_TAG, "Bluetooth turned off ");
                //TODO cerrar la app
                serverTask.setListeners(false,false);
            }
        }
    }
}