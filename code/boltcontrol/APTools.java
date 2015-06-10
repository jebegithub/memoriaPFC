package control.bolt.android.boltcontrol;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;


import java.util.List;

public class APTools {

    private static final String LOG_TAG = "ApTools";
    private WifiManager wifiManager;
    private Main context;
    private Boolean isConnectedToAP;
    //Default AP values
    private String networkSSID;
    private String networkPass;
    private int connexionThreshold;

    public APTools(Main context) {
        /*Load connexion values from the user preferences */
        networkSSID  = context.getPreferences().getString("pref_wifi_essid","RPiBoltControl");
        networkPass = context.getPreferences().getString("pref_wifi_pass", "bolt1234");
        connexionThreshold = Integer.parseInt(context.getPreferences().getString("pref_connexion_range","50"));

        this.context = context;
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        isConnectedToAP = false;
    }

    public int configureAP() {
        Log.i(LOG_TAG, "configureAP()");
        if (!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }
        List<WifiConfiguration> configuredList = wifiManager.getConfiguredNetworks();
        WifiConfiguration wifiConfiguration;
        int wifiConfigurationId = 0;
        if (configuredList != null) {
            for (WifiConfiguration i : configuredList) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiConfigurationId = i.networkId;
                    break;
                }
            }
            if (wifiConfigurationId < 1) {
                wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfigurationId = wifiManager.addNetwork(wifiConfiguration);
            }
        }
        return wifiConfigurationId;
    }

    public void connectToAP() {
        int wifiConfigurationId;
        Boolean isAPOnScanResults = isAPOnScanResults();
        Log.i(LOG_TAG, "connectToAP() -> isAPOnScanResults = "
                + isAPOnScanResults.toString() + " isConnectedToAP = "
                + isConnectedToAP.toString());
        if (isAPOnScanResults && !isConnectedToAP) {
            wifiConfigurationId = configureAP();
            Log.i(LOG_TAG, "ConfigID = " + wifiConfigurationId);
            Log.i(LOG_TAG, "Disconnect = " + wifiManager.disconnect());
            Log.i(LOG_TAG, "Enable AP = " + wifiManager.enableNetwork(wifiConfigurationId, true));
            Log.i(LOG_TAG, "Reconnect = " + wifiManager.reconnect());

        }
    }

    public void checkConnexionToAP() {
    context.getGUIManager().changeWifiIcon(isConnectedToAP);
    }

    public Boolean isAPOnScanResults() {
        //Checks if the ap is present in the scan result and the power is enough according with the user preferences
        Boolean isAPAvailable = false;
        List<ScanResult> scanList = wifiManager.getScanResults();
        for (ScanResult i : scanList) {
            if (i.SSID != null && i.SSID.equals(networkSSID) && WifiManager.calculateSignalLevel(i.level, 100) > connexionThreshold) {
                isAPAvailable = true;
                break;
            }
        }
        Log.i(LOG_TAG,"isAPOnScanResults() ->" + isAPAvailable);
        return isAPAvailable;
    }

    public String getMacAddress(){
        return wifiManager.getConnectionInfo().getMacAddress();
    }

    public String getNetworkSSID() {
        return networkSSID;
    }
    public void setIsConnectedToAP(Boolean isConnectedToAP) {
        this.isConnectedToAP = isConnectedToAP;
    }

    public Boolean getIsConnectedToAP() {
        return this.isConnectedToAP;
    }
}
