package control.bolt.android.boltcontrol;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class GUIManager {

private static final String LOG_TAG = "GUIManager";
private Main context;
private ImageView imageOpen;
private ImageView imageClosed;

    public GUIManager(Main context){
        this.context = context;
        imageOpen = (ImageView) context.findViewById(R.id.MainImageON);
        imageClosed = (ImageView) context.findViewById(R.id.MainImageOFF);
    }

    public void setImageOpen(boolean idDoorOpen) {
        if (idDoorOpen) {
            imageOpen.setVisibility(View.VISIBLE);
            imageClosed.setVisibility(View.GONE);
            context.findViewById(R.id.doorLayout).setBackgroundColor(context.getResources().getColor(R.color.light_yellow));
            Toast toast = Toast.makeText(context,R.string.OpenDoorText, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            imageOpen.setVisibility(View.GONE);
            imageClosed.setVisibility(View.VISIBLE);
            context.findViewById(R.id.doorLayout).setBackgroundColor(context.getResources().getColor(R.color.white));
        }

    }

    public void changeWifiIcon(Boolean isOnline ){
        ImageView wifiImage = (ImageView) context.findViewById(R.id.wifiImage);
        TextView wifiText = (TextView) context.findViewById(R.id.wifiStatusTextView);
        if (isOnline && !wifiImage.getDrawable().equals(R.drawable.wifi_green)) {
            wifiImage.setImageResource(R.drawable.wifi_green);
            wifiText.setText(R.string.WifiGreenText);
            //context.startUDPTask();
            //context.registerDevice();
        }
        else if (!wifiImage.getDrawable().equals(R.drawable.wifi_red)) {
            wifiImage.setImageResource(R.drawable.wifi_red);
            wifiText.setText(R.string.WifiRedText);
        }

    }

    public void changeBTIcon(int status){
        ImageView btImage = (ImageView) context.findViewById(R.id.bluetoothImage);
        TextView btText = (TextView) context.findViewById(R.id.bluetoothStatusTextView);
        switch (status){
            case 0:btImage.setImageResource(R.drawable.ic_action_bluetooth);
                btText.setText(R.string.bluetoothOfflineText);
                break;
            case 1:btImage.setImageResource(R.drawable.ic_action_bluetooth_server);
                btText.setText(R.string.bluetoothConnectingText);
                break;
            case 2:btImage.setImageResource(R.drawable.ic_action_bluetooth_server);
                btText.setText(R.string.bluetoothOnLineText);
                break;
        }
    }

}
