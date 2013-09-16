package test.example.helloworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.bluetooth.*;

public class MainActivity extends Activity {
    public final static String EXTRA_MESSAGE = "test.example.helloworld.MESSAGE";
	protected BluetoothAdapter mBluetoothAdapter;
	protected obdService mobdService = null;
	protected ArrayAdapter<String> cmdPrompt;
	
	ArrayList<String> mpgDataList = new ArrayList<String>();
	TextView mainText;
	
	public static final int WRITE_SCREEN = 1;
	public static final int WRITE_PROMPT = 2;
	public static final int WRITE_FILE = 3;
	public static final int FINISH_IT = 4;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cmdPrompt = new ArrayAdapter<String>(this, android.R.layout.list_content);
        mainText = (TextView) findViewById(R.id.mainDisplay);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			AlertBox("blue", "Bluetooth not supported");
			finish();
		}else{
			mobdService = new obdService(this, mHandler);
		}
    }

    Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {

    		switch (msg.what) {
    		case WRITE_PROMPT:
    			cmdPrompt.add(msg.getData().getString("commData"));
    			break;
    		case WRITE_FILE:
    			writeCommsToFile(cmdPrompt);
    			break;
    		case WRITE_SCREEN:
    			Time now = new Time();
    			now.setToNow();
    			String curTime = Integer.toString(now.year)+"-"+Integer.toString(now.month)+"-"+Integer.toString(now.monthDay)
    					+ " " + Integer.toString(now.hour) + ":" + Integer.toString(now.minute) + ":" +Integer.toString(now.second);
    			
    			mpgDataList.add(curTime+ ": " + msg.getData().getString("mpgData") + "\r");
    			mainText.setText(msg.getData().getString("mpgData"));
    			break;
    			
    		}
    	}	
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
            }
        

    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DisplayMessageActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mobdService.connect(device);
        
    }
    
    public void sendMessage(View view){
    	Intent intent = new Intent(this, DisplayMessageActivity.class);
    
    	startActivityForResult(intent, 1);
    }
    
    public void writeCommsToFile(ArrayAdapter<String> prompt){
    	File file = new File(Environment.getExternalStorageDirectory(), "hello_world_data.txt");
		OutputStream os = null;
		String str = "";
		try {
			os = new FileOutputStream(file, true);
		} catch (FileNotFoundException e1) {}
		
    	
    	
    	for(int i =0; i< prompt.getCount(); ++i){
    		str = str.concat(prompt.getItem(i));
    	}
    	try {
			os.write(str.getBytes());
			finish();
		} catch (IOException e) {
			try {
				os.close();
			} catch (IOException e1) {}
		}
    }

    public void writeAndFinish(View view){
    	mobdService.stop();
    	File file = new File(Environment.getExternalStorageDirectory(), "mpg_data.txt");
		OutputStream os = null;
		
		try {
			os = new FileOutputStream(file, true);
		} catch (FileNotFoundException e1) {}
		
		for(int i = 0; i< mpgDataList.size(); ++i){
			try {
				os.write(mpgDataList.get(i).getBytes());
			} catch (IOException e) {
				try {
					os.close();
				} catch (IOException e1) {}
			}
		}
		
		finish();
    }
    
	public void AlertBox( String title, String message ){
	    new AlertDialog.Builder(this)
	    .setTitle( title )
	    .setMessage( message + "\n Please Press OK" )
	    .setPositiveButton("OK", new OnClickListener() {
	        public void onClick(DialogInterface arg0, int arg1) {
	          //finish();
	        }
	    }).show();
	  }
    
}
