package org.mort11.battest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private BluetoothDevice partner = null;
	private boolean running = false;
	private int syncs = 0;

	private final Intent enableBTIntent = new Intent(
			BluetoothAdapter.ACTION_REQUEST_ENABLE);
	private final int SEL_PARTNER = 1;
	private final int START_TEST = 2;
	private final int STOP_TEST = 3;

	private final Intent launchDevicePicker = new Intent(
			"android.bluetooth.devicepicker.extra.LAUNCH");
	private final IntentFilter BTfilter = new IntentFilter(
			"android.bluetooth.devicepicker.action.DEVICE_SELECTED");
	private final BroadcastReceiver deviceSelectedRX = new BroadcastReceiver() {
		public void onReceive(Context c, Intent i) {
			partner = (BluetoothDevice) i.getExtras().get(
					BluetoothDevice.EXTRA_DEVICE);
		}
	};

	private final BroadcastReceiver syncUpdate = new BroadcastReceiver() {
		public void onReceive(Context c, Intent i ){
			if(i == SyncClientService.statusUpdate){
				syncs++;
			}else{
				if(running){
					toggleTesting(findViewById(R.id.startTest));
				}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume() {
		registerReceiver(deviceSelectedRX, BTfilter);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(deviceSelectedRX);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void selectPartner(View v) {
		initBluetooth(SEL_PARTNER);

	}

	public void toggleTesting(View v) {
		if (((Switch) findViewById(R.id.server)).isChecked()) {
			// pass
		} else {
			if (partner == null) {
				Toast.makeText(this, "need partner", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!running) {
				initBluetooth(START_TEST);
			} else {
				getWindow().clearFlags(
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				stopService(new Intent(this, SyncClientService.class));
				running = false;
				((TextView) v).setText(R.string.stopTest);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SEL_PARTNER:
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.turnOnBT, Toast.LENGTH_SHORT)
						.show();
			} else {
				scanForPartners();
			}
			break;
		case START_TEST:
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.turnOnBT, Toast.LENGTH_SHORT)
						.show();
			} else {
				getWindow().addFlags(
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				Intent syncClientIntent = new Intent(this,
						SyncClientService.class).putExtra("MAC",
						partner.getAddress());
				startService(syncClientIntent);
				syncs = 0;
				running = true;
				
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initBluetooth(int reqCode) {
		BluetoothAdapter mbtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mbtAdapter.isEnabled()) {
			startActivityForResult(enableBTIntent, reqCode);
		}
	}

	private void scanForPartners() {
		startActivity(launchDevicePicker);
	}

}
