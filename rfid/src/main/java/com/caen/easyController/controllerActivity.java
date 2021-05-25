package com.caen.easyController;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.caen.BLEPort.BLEPortEvent;
import com.caen.RFIDLibrary.CAENRFIDBLEConnectionEventListener;
import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDPort;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.CAENRFIDReaderInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;
public class controllerActivity extends AppCompatActivity implements CAENRFIDBLEConnectionEventListener {


	protected static final int ADD_READER_BT = 1;
	protected static final int DO_INVENTORY = 3;
	protected static final int ADD_READER_BLE = 4;
	protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	protected static boolean STARTED = true;
	protected static boolean DESTROYED = false;
	protected static boolean CONNECTION_SUCCESSFUL = false;

	private final ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
	private SimpleAdapter adapter;

	public static boolean returnFromActivity = false;

	private static boolean exitFromApp = false;

	public static Vector<DemoReader> Readers;

	public static int Selected_Reader;

	private Toolbar toolbar;
	private TextView toolbarTitle;
	private TextView toolbarSubtitle;

	private ProgressDialog tcpBtWaitProgressDialog;
	private final BroadcastReceiver mReceiverUUIDCached = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (DESTROYED)
				return;
			String action = intent.getAction();
			assert action != null;
			if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
				CONNECTION_SUCCESSFUL = true;
			}
		}
	};

	private final BroadcastReceiver mReceiverBTDisconnect = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (DESTROYED)
				return;
			String action = intent.getAction();
			assert action != null;
			if ((action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) && (!BluetoothAdapter
					.getDefaultAdapter().isEnabled()))
					|| (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))) {
				int pos = 0;
				Vector<Integer> toRemove = new Vector<Integer>();
				for (DemoReader demoReader : Readers) {
					try {
						if (demoReader.getConnectionType().equals(
								CAENRFIDPort.CAENRFID_BT)) {
							data.remove(pos);
							adapter.notifyDataSetChanged();
							demoReader.getReader().Disconnect();
							toRemove.add(pos);
						}
					} catch (CAENRFIDException e) {
						e.printStackTrace();
					}
					pos++;
				}
				for (int i = 0; i < toRemove.size(); i++) {
					Readers.remove(toRemove.get(i).intValue());
				}
				if (!toRemove.isEmpty()) {
					Toast.makeText(getApplicationContext(),
							"Bluetooth device disconnected!",
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	};



	@Override
	public void onBLEConnectionEvent(final CAENRFIDReader caenrfidReader, final BLEPortEvent blePortEvent) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (DESTROYED)
					return;
				if (blePortEvent.getEvent().equals(BLEPortEvent.ConnectionEvent.CONNECTION_LOST) ||
						blePortEvent.getEvent().equals(BLEPortEvent.ConnectionEvent.CONNECTION_CLOSED)
				) {
					int pos = 0;
					Vector<Integer> toRemove = new Vector<Integer>();
					for (DemoReader demoReader : Readers) {
						try {
							if (
									demoReader.getConnectionType().equals(CAENRFIDPort.CAENRFID_BLE)
											&& demoReader.getReader().equals(caenrfidReader)
							) {
								data.remove(pos);
								adapter.notifyDataSetChanged();
								demoReader.getReader().Disconnect();
								toRemove.add(pos);
							}
						} catch (CAENRFIDException e) {
							e.printStackTrace();
							toRemove.add(pos);
						}
						pos++;
					}
					for (int i = 0; i < toRemove.size(); i++) {
						Readers.remove(toRemove.get(i).intValue());
					}
					Toast.makeText(getApplicationContext(),
							"BLE device disconnected!", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

	}


	private class BLEConnector extends Thread{

		BluetoothDevice bleDevice = null;
		boolean error1 = false;
		boolean error2 = false;

		BLEConnector ( BluetoothDevice device) {
			bleDevice = device;
			setName("BLEConnector");
		}

		@Override
		public void run() {
			super.run();
			//connect to the reader...
			CAENRFIDReader r = new CAENRFIDReader();
			CAENRFIDReaderInfo info = null;
			String fwrel = null;
			try {
				r.addCAENRFIDBLEConnectionEventListener(controllerActivity.this);
				r.Connect(getApplicationContext(),bleDevice);
			} catch (CAENRFIDException e) {
				error1 = true;
			}
			if (!error1) {
				try {
					error2 = false;
					info = r.GetReaderInfo();
					fwrel = r.GetFirmwareRelease();
				} catch (CAENRFIDException e) {
					error2 = true;
				}
				if (!error2) {
					DemoReader dr = new DemoReader(r,
							info.GetModel(), info.GetSerialNumber(), fwrel,
							CAENRFIDPort.CAENRFID_BLE);
					Readers.add(dr);
				}
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onPostExecute(true);
				}
			});
		}

		protected void onPostExecute(Boolean result) {
			if (error1) {
				Toast.makeText(getApplicationContext(),
						"Error during connection...", Toast.LENGTH_SHORT)
						.show();
			} else if (error2) {
				Toast.makeText(getApplicationContext(),
						"Error retrieving info from reader...",
						Toast.LENGTH_SHORT).show();
			} else {
				updateReadersList();
			}
			tcpBtWaitProgressDialog.dismiss();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//toolbar = this.findViewById(R.id.toolbar);
		toolbarTitle = this.findViewById(R.id.toolbar_title);
		toolbarTitle.setText("Choose readers");
		toolbarSubtitle = this.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText("Available readers");


		if (!controllerActivity.returnFromActivity) {
			Readers = new Vector<>();
		} else
			controllerActivity.returnFromActivity = false;

		IntentFilter disc_filt = new IntentFilter( BluetoothDevice.ACTION_ACL_DISCONNECTED );
		this.registerReceiver(mReceiverBTDisconnect, disc_filt);



		IntentFilter disc_filt3 = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiverBTDisconnect, disc_filt3);

		IntentFilter disc_filt4 = new IntentFilter(
				BluetoothDevice.ACTION_ACL_CONNECTED);
		this.registerReceiver(mReceiverUUIDCached, disc_filt4);




	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onPostResume() {

		super.onPostResume();
	}

	@Override
	protected void onRestart() {

		super.onRestart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onStart() {
		super.onStart();
		STARTED = true;
		DESTROYED = false;
		ListView lv = this.findViewById(R.id.reader_list);


		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id) {
				//pass reder position
				Selected_Reader = position;
				Intent do_inventory = new Intent(getApplicationContext(),
						InventoryActivity.class);
				startActivityForResult(do_inventory, DO_INVENTORY);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DESTROYED = true;
		if (controllerActivity.exitFromApp) {
			for (DemoReader demoReader : Readers) {
				try {

					if ((demoReader.getConnectionType().equals(
							CAENRFIDPort.CAENRFID_BT) && BluetoothAdapter
							.getDefaultAdapter().isEnabled())

							|| (demoReader.getConnectionType().equals(
							CAENRFIDPort.CAENRFID_BLE) && BluetoothAdapter
							.getDefaultAdapter().isEnabled()))
					{
						demoReader.getReader().Disconnect();
						demoReader.getReader().removeCAENRFIDBLEConnectionEventListener(controllerActivity.this);
					}
				} catch (CAENRFIDException e) {
					e.printStackTrace();
				}
			}
			Readers = null;
		}
		this.unregisterReceiver(mReceiverBTDisconnect);
		this.unregisterReceiver(mReceiverUUIDCached);

		exitFromApp = false;
		returnFromActivity = false;
	}

	public Activity getActivity() {
		return this;
	}

/*	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		if (id == 1) {
			final CharSequence[] items = {"Bluetooth", "Ble"};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Connection Type");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) {
						if (BluetoothAdapter.getDefaultAdapter() == null) {
							Toast.makeText(getApplicationContext(), "No Bluetooth adapter found!", Toast.LENGTH_SHORT).show();
							return;
						}
						Toast.makeText(getApplicationContext(), items[item],
								Toast.LENGTH_SHORT).show();
						Intent addReader = new Intent(getApplicationContext(),
								BTSelection.class);
						getActivity().startActivityForResult(addReader,
								ADD_READER_BT);
					} else if (item == 1) {
						if (BluetoothAdapter.getDefaultAdapter() == null) {
							Toast.makeText(getApplicationContext(), "No Bluetooth adapter found!", Toast.LENGTH_SHORT).show();
							return;
						}
						Toast.makeText(getApplicationContext(), items[item],
								Toast.LENGTH_SHORT).show();
						Intent addReader = new Intent(getApplicationContext(),
								BLESelection.class);
						getActivity().startActivityForResult(addReader,
								ADD_READER_BLE);
					}
				}
			});
			dialog = builder.create();
		} else {
			dialog = null;
		}
		return dialog;
	}*/

	public void updateReadersList() {
		if (Readers != null) {
			((ListView) findViewById(R.id.reader_list)).setAdapter(null);
			data.clear();

			for (int i = 0; i < Readers.size(); i++) {
				DemoReader r = Readers.get(i);

				HashMap<String, Object> readerMap = new HashMap<>();

				readerMap.put("name", r.getReaderName());
				readerMap.put("info", "Serial: " + r.getSerialNumber());
				data.add(readerMap);
			}
		}
		String[] from = { "image", "name", "info" };
		int[] to = { R.id.reader_image, R.id.reader_name, R.id.reader_info };

		adapter = new SimpleAdapter(getApplicationContext(), data,
				R.layout.list_reader, from, to);
		adapter.notifyDataSetChanged();

		((ListView) findViewById(R.id.reader_list)).setAdapter(adapter);
	}

	public void addNewReaderActivity(View v) {
			if (BluetoothAdapter.getDefaultAdapter() == null) {
				Toast.makeText(getApplicationContext(), "No Bluetooth adapter found!", Toast.LENGTH_SHORT).show();
				return;
			}
		/*	Toast.makeText(getApplicationContext(), "Ble",
					Toast.LENGTH_SHORT).show();*/
			Intent addReader = new Intent(getApplicationContext(),
					BLESelection.class);
			getActivity().startActivityForResult(addReader,
					ADD_READER_BLE);
		}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		BluetoothDevice dev;
		switch (requestCode) {

		/*	case controllerActivity.ADD_READER_BT:
				if (resultCode == RESULT_OK) {
					dev = data.getParcelableExtra("BT_DEVICE");
					assert dev != null;
					tcpBtWaitProgressDialog = ProgressDialog.show(this,
							"Connection ", "Connecting to " + dev.getName(), true,
							true);
					new BTConnector().execute(dev);
				}
				break;*/

			case controllerActivity.ADD_READER_BLE:
				if(resultCode == RESULT_OK) {
					int selected_port = data.getIntExtra("BLE_SELECTION_RESULT", BLESelection.BLE_CANCELED);
					if(selected_port == BLESelection.BLE_SELECTED){
						final BluetoothDevice device = (BluetoothDevice) Objects.requireNonNull(data.getExtras()).get("BLE_DEVICE");
						assert device != null;
						tcpBtWaitProgressDialog = ProgressDialog.show(this,
								"Connection ","Connecting to "+ device.getName());
						new BLEConnector(device).start();
					}
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onBackPressed() {
		exitFromApp = true;
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}


}
