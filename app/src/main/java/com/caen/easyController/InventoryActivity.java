package com.caen.easyController;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.Dialog;
import com.caen.BLEPort.BLEPortEvent;
import com.caen.RFIDLibrary.CAENRFIDBLEConnectionEventListener;
import com.caen.RFIDLibrary.CAENRFIDEvent;
import com.caen.RFIDLibrary.CAENRFIDEventListener;
import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDLogicalSourceConstants;
import com.caen.RFIDLibrary.CAENRFIDNotify;
import com.caen.RFIDLibrary.CAENRFIDReader;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryActivity extends AppCompatActivity implements CAENRFIDBLEConnectionEventListener {

    public static final short INIT_RSSI_VALUE = -1;
    public static final short MAX_TAGS_PER_LIST = 700;
    public static Hashtable<String, Integer> mTagListPosition;
    public static int mSelectedTag;
    protected DemoReader mReader;
    protected RFIDTagAdapter mRFIDTagAdapter;
    protected Button mButtonInventory;
    protected ListView mInventoryList;
    protected TextView mTotalFound;
    protected TextView mCurrentFound;
    protected int reader_position = 0;

    protected short maxRssi = 0;
    protected short minRssi = 0;

    protected static AtomicInteger mCurrentFoundNum;
    protected static long mCurrentFoundTime;
    protected static long mBelltime;
    InventoryTask _inventory_task = null;

    @Override
    public void onBLEConnectionEvent(CAENRFIDReader caenrfidReader, BLEPortEvent blePortEvent) {
        if (blePortEvent.getEvent().equals(BLEPortEvent.ConnectionEvent.CONNECTION_LOST))
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
    }

    private final BroadcastReceiver mReceiverBTDisconnect = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) && (!BluetoothAdapter
                    .getDefaultAdapter().isEnabled()))
                    || (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))) {
                finish();
            }
        }
    };



    class CurrentUpdater implements Runnable {
        private String mNum;

        public CurrentUpdater(String val) {
            this.mNum = val;
        }

        @Override
        public void run() {
            mCurrentFound.setText(this.mNum);
        }

    }

    class InventoryTask extends AsyncTask<Object, Object, Void>
            implements CAENRFIDEventListener {

        private boolean _running = false;

        boolean running() {
            return _running;
        }

        void setRunning(boolean is_running) {
            _running = is_running;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Object... args) {
            CAENRFIDReader reader = ((DemoReader) args[0]).getReader();
            CAENRFIDLogicalSource TheSource = null;
            try {
                TheSource = reader.GetSource("Source_0");
            } catch (CAENRFIDException e1) {
                e1.printStackTrace();
            }
            try {
                assert TheSource != null;
                TheSource.SetReadCycle(0);
                reader.addCAENRFIDEventListener(this);
                    TheSource
                            .SetSelected_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED);
                    TheSource
                            .EventInventoryTag(
                                    new byte[0],
                                    (short) 0x0,
                                    (short) 0x0,
                                    (short) 0x06
                                   );


            } catch (CAENRFIDException e) {
                e.printStackTrace();
            }
            //update main layout, inventory has been started...
            this.publishProgress(new Object[]{null});

            try {
                int[] tmp_i = new int[]{0, 0, 0, 0, 0};
                int tmp_cur = 0;
                mCurrentFoundTime = System.currentTimeMillis();
                _running = true;
                while (true) {
                    Thread.sleep(10);
                    if ((System.currentTimeMillis() - mCurrentFoundTime) > 1000) {
                        tmp_cur = (tmp_cur - tmp_i[4]);
                        for (int i = 1; i < 5; i++) {
                            tmp_i[5 - i] = tmp_i[5 - i - 1];
                        }
                        tmp_i[0] = mCurrentFoundNum.getAndSet(0);
                        tmp_cur += tmp_i[0];
                        mCurrentFound.post(new CurrentUpdater(String.valueOf(tmp_cur / 5)));
                        mCurrentFoundTime = System.currentTimeMillis();
                    }
                    if (!running()) {
                        try {
                            reader.InventoryAbort();
                        } catch (CAENRFIDException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reader.removeCAENRFIDEventListener(this);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... elements) {
            if (elements[0] == null) {
                //Inventory has been started;
                mButtonInventory.setText(R.string.inventory_button_stop);
                mButtonInventory.setEnabled(true);
                return;
            }
            CAENRFIDNotify tag = (CAENRFIDNotify) elements[0];
            short tmp_rssi = tag.getRSSI();
            // controllo gli Rssi e aggiorno max e min
            if (maxRssi == INIT_RSSI_VALUE) {
                // update for first time the max and min rssi
                maxRssi = tmp_rssi;
                minRssi = tmp_rssi;
            }
            if (tmp_rssi > maxRssi)
                maxRssi = tmp_rssi;
            else if (tmp_rssi < minRssi)
                minRssi = tmp_rssi;
            mRFIDTagAdapter.addTag(
                    new RFIDTag(tag, RFIDTag
                            .toHexString(tag.getTagID()), tag.getRSSI()),
                    maxRssi, minRssi);

            mTotalFound.setText(Integer.toString(mRFIDTagAdapter.getCount()));
        }

        @Override
        protected void onPostExecute(Void unused) {
            Toast.makeText(InventoryActivity.this, "Done!", Toast.LENGTH_SHORT)
                    .show();
            mButtonInventory.setText(R.string.inventory_button_start);
            mButtonInventory.setEnabled(true);
        }

        @Override
        public void CAENRFIDTagNotify(CAENRFIDEvent evt) {
            mCurrentFoundNum.incrementAndGet();
            this.publishProgress((Object[]) evt.getData().toArray(new CAENRFIDNotify[0]));

        }
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_selection);



        reader_position = CAENRFIDEasyControllerActivity.Selected_Reader;
        mReader = CAENRFIDEasyControllerActivity.Readers.get(CAENRFIDEasyControllerActivity.Selected_Reader);
        mRFIDTagAdapter = new RFIDTagAdapter(getApplicationContext(), R.id.inventory_list);
        mButtonInventory = this.findViewById(R.id.inventory_button);
        mInventoryList = this.findViewById(R.id.inventory_list);
        mInventoryList.setAdapter(mRFIDTagAdapter);

        IntentFilter bt_acl_disconnect_filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiverBTDisconnect, bt_acl_disconnect_filter);



        IntentFilter bt_state_changed_filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiverBTDisconnect, bt_state_changed_filter);



        mReader.getReader().addCAENRFIDBLEConnectionEventListener(this);

        OnItemClickListener listListener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (inventoryRunning()) {
                    Toast.makeText(getApplicationContext(),
                            "Must stop inventory", Toast.LENGTH_SHORT).show();
                    return;
                }
                mSelectedTag = position;
                ExecutorService mExecutorService = Executors.newFixedThreadPool(1);
                FutureTask<AlertDialog.Builder> contextMenuTask = new FutureTask<>(
                        new Callable<AlertDialog.Builder>() {

                            @Override
                            public AlertDialog.Builder call() {

                                final CharSequence[] items_2 = {"Read",};


                                AlertDialog.Builder builder = new AlertDialog.Builder(InventoryActivity.this);
                                builder.setTitle("Choose action");
                                builder.setItems(
                                        items_2,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int item) {
                                                Bundle b = new Bundle();
                                                String s1 = mRFIDTagAdapter.getItem(
                                                        mSelectedTag).getId();
                                                b.putString("TAG_HEX", s1);


                                                switch (item) {

                                                    case 0:
                                                        // start inventory mode
                                                        Intent randw = new Intent(
                                                                getApplicationContext(),
                                                                ReadAndWriteActivity.class);
                                                        randw.putExtras(b);
                                                        startActivityForResult(randw, 0);
                                                        break;

                                                }
                                            }
                                        });
                                return builder;
                            }
                        });
                mExecutorService.execute(contextMenuTask);
                mExecutorService.shutdownNow();
                AlertDialog.Builder alertDialogBuilder = null;
                try {
                    alertDialogBuilder = contextMenuTask.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (alertDialogBuilder == null)
                    Toast.makeText(InventoryActivity.this, "Please, put tag on the antenna...", Toast.LENGTH_SHORT).show();
                else
                    alertDialogBuilder.create().show();
            }

        };
        mInventoryList.setOnItemClickListener(listListener);
        mTotalFound = this.findViewById(R.id.total_found_num);
        mCurrentFound = this.findViewById(R.id.current_found_num);

        maxRssi = INIT_RSSI_VALUE;
        minRssi = INIT_RSSI_VALUE;
        mTagListPosition = new Hashtable<>(MAX_TAGS_PER_LIST);
        mCurrentFoundNum = new AtomicInteger(0);
        mCurrentFoundTime = 0;
        mBelltime = System.currentTimeMillis();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC) {
            ClearInventory(null);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return new Dialog(InventoryActivity.this);
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mReceiverBTDisconnect);

        mReader.getReader().removeCAENRFIDBLEConnectionEventListener(this);
        super.onDestroy();
        if (inventoryRunning()) {
            mButtonInventory.setText(R.string.inventory_button_start);
            mButtonInventory.setEnabled(false);
            stopInventory();
        }
        CAENRFIDEasyControllerActivity.returnFromActivity = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    public void doInventoryAction(View v) {
        mButtonInventory.setEnabled(false);
        if (inventoryRunning()) {
            stopInventory();
        } else {
            this.maxRssi = InventoryActivity.INIT_RSSI_VALUE;
            this.minRssi = InventoryActivity.INIT_RSSI_VALUE;
            _inventory_task = null;
            startInventory();
        }
    }

    public void ClearInventory(View v) {
        if (inventoryRunning())
            Toast.makeText(getApplicationContext(), "Must stop inventory",
                    Toast.LENGTH_SHORT).show();
        else {
            mRFIDTagAdapter.clear();
            mTagListPosition = new Hashtable<>(MAX_TAGS_PER_LIST);
            this.mTotalFound.setText("0");
            minRssi = 0;
            maxRssi = 0;
        }
    }

    public void startInventory() {
        if (_inventory_task == null) {
            _inventory_task = new InventoryTask();
            _inventory_task.execute(this.mReader);
        }
    }

    public void stopInventory() {
        try {
            _inventory_task.setRunning(false);
        } catch (Exception exc) {
            exc.printStackTrace(); // [cg]NAJ> not a good solution, these exceptions shoul propagate or register a log entry
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (inventoryRunning()) {
            Toast.makeText(getApplicationContext(), "Must stop inventory",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


    private boolean inventoryRunning() {
        return _inventory_task != null && _inventory_task.running();
    }





}