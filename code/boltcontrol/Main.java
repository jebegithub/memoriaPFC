package control.bolt.android.boltcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

public class Main extends Activity {
    private static final String LOG_TAG = "MainActivity";
    private Context context;
    private CustomBroadcastReceiver customBroadcastReceiver;
    private SharedPreferences preferences;
    private GUIManager GUIManager;
    private APTools apTools;
    private AsyncConnexion serverTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        GUIManager = new GUIManager(this);

        ImageButton settingsButton = (ImageButton) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, SettingsActivity.class));
            }
        });

        apTools = new APTools(this);

        serverTask = new AsyncConnexion(this);
        serverTask.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        IntentFilter customIntentFilter = new IntentFilter();
        customIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        customIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        customBroadcastReceiver = new CustomBroadcastReceiver(this, serverTask);
        registerReceiver(customBroadcastReceiver, customIntentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothConnexion.getRequestEnableBtCode() ) {
            if(resultCode == RESULT_OK){
                Log.i(LOG_TAG, "Bluetooth successfully enabled");
            }
            if (resultCode == RESULT_CANCELED) {
                Log.e(LOG_TAG, "Error while enabling bluetooth");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Start");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "Resume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(LOG_TAG, "Restart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "Stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Destroy");
        unregisterReceiver(customBroadcastReceiver);
        //bluetoothConnexion.closeBTConnexion();
        //TODO comprobar si existe la tarea asincrona bt y la wifi y cerrarlas
    }

    public APTools getApTools() {
        return apTools;
    }

    public GUIManager getGUIManager() {
        return GUIManager;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}



