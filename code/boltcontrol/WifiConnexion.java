package control.bolt.android.boltcontrol;

import android.util.Log;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class WifiConnexion {

    private final String LOG_TAG = "WIFIConnexion";

    private boolean isInitialized;
    private DatagramSocket socket;
    private int port;

    public WifiConnexion(int port){
        this.port = port;
    }

    public boolean initialize() {
        if (!isInitialized) {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(port));
            } catch (SocketException e) {
                Log.e(LOG_TAG, e.toString());
            }
            isInitialized = (socket != null);
        }
        return isInitialized;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }
}


