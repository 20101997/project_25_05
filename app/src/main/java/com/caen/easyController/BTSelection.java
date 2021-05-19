package com.caen.easyController;
import java.util.ArrayList;
import java.util.Set;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class BTSelection extends Activity {
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_ENABLE_BT_CANCELLED=10;
	private static final int REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
	private ProgressDialog mProgressDialog;
	private BluetoothDevice deviceBT = null;
	private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private ArrayAdapter<String> mArrayAdapter = null;
	private ArrayList<BluetoothDevice> mArrayDevice = null;
	private ProgressBar mSearchProgressBar=null;
	private TextView mSearchLabel=null;
	private IntentFilter mFilter, mFilter2, mFilter3;


	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// permission was granted, yay! Do the
					// contacts-related task you need to do.
					if (mBluetoothAdapter == null) {
						Toast.makeText(getApplicationContext(), "No bluetooth adapter...",
								Toast.LENGTH_SHORT).show();
					} else {
						if (!mBluetoothAdapter.isEnabled()) {
							Intent enableBtIntent = new Intent(
									BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

						}
						Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
								.getBondedDevices();
						// If there are paired devices
						if (pairedDevices.size() > 0) {
							// Loop through paired devices
							for (BluetoothDevice device : pairedDevices) {
								// Add the name and address to an array adapter to show
								// in a ListView
								mArrayAdapter.add(device.getName() + "\n"
										+ device.getAddress());
								mArrayDevice.add(device);
							}
						}
						mBluetoothAdapter.startDiscovery();
					}
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					setResult(REQUEST_ENABLE_BT_CANCELLED);
					finish();
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request.
		}
	}


	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				deviceBT = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in a
				// ListView
				String sdev = deviceBT.getName() + "\n" + deviceBT
						.getAddress();
				int ndev = mArrayAdapter.getCount();
				String tmp = null;
				for (int i = 0; i < ndev; i++) {
					tmp = mArrayAdapter.getItem(i);
					if (tmp.equalsIgnoreCase(sdev))
						return;
				}
				mArrayAdapter.add(sdev);
				mArrayDevice.add(deviceBT);
			}
		}
	};
	private final BroadcastReceiver mReceiverStart=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				mSearchProgressBar.setVisibility(ProgressBar.VISIBLE);
				mSearchLabel.setText("Searching device...");
			}
			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				mSearchProgressBar.setVisibility(ProgressBar.GONE);
				mSearchLabel.setText("Devices found:");
			}
		}
	};
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.bt_selection);

		setTitle("Choose bluetooth device...");
		
		mSearchProgressBar=findViewById(R.id.search_progress_bar);
		mSearchLabel=findViewById(R.id.search_label);
		mSearchProgressBar.setVisibility(ProgressBar.INVISIBLE);
		
		mProgressDialog=new ProgressDialog(this.getApplicationContext());

		
		mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, mFilter);
		
		mFilter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(mReceiverStart, mFilter2);

		mFilter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiverStart, mFilter3);

		mArrayAdapter = new ArrayAdapter<>(this,
				R.layout.bt_selection_item);
		mArrayDevice = new ArrayList<>();
		ListView lv = this.findViewById(R.id.bt_selection_list);
		lv.setAdapter(mArrayAdapter);
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				BluetoothDevice dev = mArrayDevice.get(position);
				Intent newIntent = new Intent();
				newIntent.putExtra("BT_DEVICE", dev);
				setResult(RESULT_OK, newIntent);
				mProgressDialog=ProgressDialog.show(parent.getContext(), "Connection", "Connecting to "+dev.getName(), true, true);
				if(mBluetoothAdapter.isDiscovering()) {
					mBluetoothAdapter.cancelDiscovery();
					new AsyncTask<Void,Void,Void>(){

						@Override
						protected Void doInBackground(Void... voids) {
							while(mBluetoothAdapter.isDiscovering()) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void aVoid) {
							super.onPostExecute(aVoid);
							finish();
						}
					}.execute();
				}else {
					finish();
				}
			}
		});
		if(
				( ContextCompat.checkSelfPermission(
				this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) || (
						ContextCompat.checkSelfPermission(
								this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
						)
		){
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					REQUEST_CODE_ACCESS_FINE_LOCATION);

		}else{
			if (mBluetoothAdapter == null) {
				Toast.makeText(getApplicationContext(), "No bluetooth adapter...",
						Toast.LENGTH_SHORT).show();
			} else {
				if (!mBluetoothAdapter.isEnabled()) {
					Intent enableBtIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

				}
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
						.getBondedDevices();
				// If there are paired devices
				if (pairedDevices.size() > 0) {
					// Loop through paired devices
					for (BluetoothDevice device : pairedDevices) {
						// Add the name and address to an array adapter to show
						// in a ListView
						mArrayAdapter.add(device.getName() + "\n"
								+ device.getAddress());
						mArrayDevice.add(device);
					}
				}
				mBluetoothAdapter.startDiscovery();
			}
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if(mBluetoothAdapter.isDiscovering()){
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				// A contact was picked. Here we will just display it
				// to the user.
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
						.getBondedDevices();
				// If there are paired devices
				if (pairedDevices.size() > 0) {
					// Loop through paired devices
					for (BluetoothDevice device : pairedDevices) {
						// Add the name and address to an array adapter to show
						// in a ListView
						mArrayAdapter.add(device.getName() + "\n"
								+ device.getAddress());
						mArrayDevice.add(device);
					}
				}
				mBluetoothAdapter.startDiscovery();
			}else{
				this.setResult(REQUEST_ENABLE_BT_CANCELLED);
				finish();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mBluetoothAdapter.isDiscovering())
			mBluetoothAdapter.cancelDiscovery();
		mProgressDialog.dismiss();
		unregisterReceiver(mReceiverStart);
		unregisterReceiver(mReceiver);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
