package org.mort11.battest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private boolean running = false;
	private int syncs = 0;

	private final Handler timerHandler = new Handler();
	private Time startTime = new Time("UTC");
	private Time lastRunTime = new Time("UTC"); 
	private final Runnable timer = new Runnable() {

		@Override
		public void run() {
			Time now = new Time("UTC");
			now.setToNow();
			Time delta = new Time("UTC");
			delta.set(now.toMillis(true) - startTime.toMillis(true));
			((TextView) findViewById(R.id.timeElapsed)).setText(delta
					.format("%H:%M:%S"));
			timerHandler.postDelayed(this, 500);
		}

	};

	private final Intent enableBTIntent = new Intent(
			BluetoothAdapter.ACTION_REQUEST_ENABLE);
	private final int SEL_PARTNER = 1;
	private final int START_TEST = 2;
	private final int START_TEST_SERVER = 3;

	private final IntentFilter BTfilter = new IntentFilter(
			BluetoothDevicePicker.ACTION_DEVICE_SELECTED);
	private final BluetoothDeviceSelectedReceiver deviceSelectedRX = new BluetoothDeviceSelectedReceiver();

	private final Intent launchDevicePicker = new Intent(
			BluetoothDevicePicker.ACTION_LAUNCH).putExtra(
			BluetoothDevicePicker.EXTRA_NEED_AUTH, false).putExtra(
			BluetoothDevicePicker.EXTRA_FILTER_TYPE,
			BluetoothDevicePicker.FILTER_ALL);

	private final IntentFilter statusUpdates = new IntentFilter(
			SyncClientService.statusUpdate.getAction());
	private final BroadcastReceiver syncUpdate = new BroadcastReceiver() {
		public void onReceive(Context c, Intent i) {
			Log.i(getString(R.string.LogcatTag), "Success = " + i.getBooleanExtra("success", true));
			if (i.getBooleanExtra("success", true)) {
				syncs++;
				((TextView) findViewById(R.id.numSyncs)).setText(syncs
						+ " syncs");
				((TextView) findViewById(R.id.syncStatus))
						.setText(R.string.success);
			} else {
				if (running) {
					toggleTesting(findViewById(R.id.startTest));
					((TextView) findViewById(R.id.syncStatus))
							.setText(R.string.failed);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LocalBroadcastManager.getInstance(this).registerReceiver(syncUpdate, statusUpdates);
		Time last = new Time("UTC");
		last.set(getPreferences(MODE_PRIVATE).getLong("Last Run", 0));
		((TextView) findViewById(R.id.timeElapsed)).setText(last
				.format("%H:%M:%S"));
	}

	@Override
	protected void onStop() {
		getPreferences(MODE_PRIVATE).edit().putLong("Last Run",
				lastRunTime.toMillis(true)).apply();
		super.onStop();
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
			if (!running) {
				initBluetooth(START_TEST_SERVER);
			} else {
				stopTest(true);
			}
		} else {
			if (deviceSelectedRX.getPartner() == null) {
				Toast.makeText(this, "need partner", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!running) {
				initBluetooth(START_TEST);
			} else {
				stopTest(false);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case SEL_PARTNER:
				scanForPartners();
				break;
			case START_TEST:
				startTest(false);
				break;
			case START_TEST_SERVER:
				startTest(true);
				break;
			}
		} else {
			Toast.makeText(this, R.string.turnOnBT, Toast.LENGTH_SHORT).show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initBluetooth(int reqCode) {
		BluetoothAdapter mbtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mbtAdapter.isEnabled()) {
			startActivityForResult(enableBTIntent, reqCode);
		} else {
			onActivityResult(reqCode, RESULT_OK, null);
		}
	}

	private void scanForPartners() {
		registerReceiver(deviceSelectedRX, BTfilter);
		startActivity(launchDevicePicker);
		Log.i(getString(R.string.LogcatTag), "asked for partner");
	}

	private void startTest(boolean server) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (server) {
			Intent syncServerIntent = new Intent(this, SyncServerService.class);
			startService(syncServerIntent);
		} else {
			Intent syncClientIntent = new Intent(this, SyncClientService.class)
					.putExtra("MAC", deviceSelectedRX.getPartner().getAddress());
			startService(syncClientIntent);
		}
		syncs = 0;
		running = true;
		startTime.setToNow();
		timerHandler.post(timer);
		((TextView) findViewById(R.id.startTest)).setText(R.string.stopTest);
	}

	private void stopTest(boolean server) {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (server) {
			stopService(new Intent(this, SyncServerService.class));
		} else {
			stopService(new Intent(this, SyncClientService.class));
		}
		running = false;
		Time now = new Time("UTC");
		now.setToNow();
		lastRunTime.set(now.toMillis(true) - startTime.toMillis(true));
		timerHandler.removeCallbacks(timer);
		((TextView) findViewById(R.id.startTest)).setText(R.string.startTest);
	}

}
