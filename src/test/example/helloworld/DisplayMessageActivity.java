package test.example.helloworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Environment;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.view.*;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.os.Build;
import android.provider.Settings;



public class DisplayMessageActivity extends Activity {
	
	protected static final String EXTRA_DEVICE_ADDRESS = null;
	protected ArrayAdapter<BluetoothDevice> mArrayAdapter;
	protected BluetoothAdapter mBluetoothAdapter;
	
		
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mArrayAdapter.add(device);
			}else{
				//mArrayAdapter.add("No discoverable Bluetooth devices available");
			}
		}
	};
	
	public void toSettings(View view){
		Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivity(intent);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
		
		mArrayAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);

		setContentView(R.layout.activity_display_message);
		
		String message;
		int REQUEST_ENABLE_BT = 1;
		int ACT_RESULT = 1;	
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(mBluetoothAdapter == null){
			message = "Bluetooth not supported";
			finish();
		}else{
			message = "Paired Devices";
			
			if(!mBluetoothAdapter.isEnabled()){
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				onActivityResult(REQUEST_ENABLE_BT, ACT_RESULT, enableBtIntent);
				if(ACT_RESULT == RESULT_CANCELED){
					message = "You must enable Bluetooth.";
				}
			}
			
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			
			if(pairedDevices.size() > 0){
				for(BluetoothDevice device : pairedDevices){
					mArrayAdapter.add(device);
				}
			}else{
				message = "No bonded devices found!";
				AlertBox("noBonds", "No bonded devices found!");
				
			}		
		}

		ListView devListView = (ListView) findViewById(R.id.btDevList);
		
		devListView.setAdapter(mArrayAdapter);
		devListView.setOnItemClickListener(new OnItemClickListener(){
			
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			       BluetoothDevice curDevice = mArrayAdapter.getItem(position);
		            Intent intent = new Intent();
		            intent.putExtra(EXTRA_DEVICE_ADDRESS, curDevice.getAddress());

		            // Set result and finish this Activity
		            setResult(Activity.RESULT_OK, intent);
		            finish();
			       
			       
			       
			   }
		});
		
		TextView btStatView = (TextView) findViewById(R.id.btStatus);
		btStatView.setText(message);		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void AlertBox( String title, String message ){
	    new AlertDialog.Builder(this)
	    .setTitle( title )
	    .setMessage( message + " Press OK to exit." )
	    .setPositiveButton("OK", new OnClickListener() {
	        public void onClick(DialogInterface arg0, int arg1) {
	          //finish();
	        }
	    }).show();
	  }
	
}

