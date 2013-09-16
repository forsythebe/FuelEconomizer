package test.example.helloworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.DecimalFormat;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;

public class obdService {
	
	
	Handler mHandler;
	BluetoothAdapter mBluetoothAdapter;
	ConnectThread mConnectThread;
	ConnectedThread mConnectedThread;
	
	
	Context parentContext;
	private int numSent = 0;
	public static final int STATE_CONNECTED = 3;
	public int mState = 0;
	
	public double vSpeed = 0; //vehicle speed in km/h
	public double MAF = 0; //mass air flow, g/s
	public double MPG = 0; //miles/gallon
	
	; //this is because device may not send the whole response at once
	
 	protected obdService(Context context, Handler handler){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		parentContext = context;
	}
	
	public synchronized void stop(){

    if (mConnectThread != null) {
        mConnectThread.cancel();
        mConnectThread = null;
    }

    if (mConnectedThread != null) {
        mConnectedThread.cancel();
        mConnectedThread = null;
    }

	}
	
	private class ConnectThread extends Thread{
		
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		protected BluetoothAdapter mBluetoothAdapter;
		
		
 		public ConnectThread(BluetoothDevice device){
			BluetoothSocket tmp = null;
			mmDevice = device;

			try{
				Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
		        tmp = (BluetoothSocket) m.invoke(device, 1);

			}catch(Exception e){}
			
			mmSocket = tmp;
		}
		
		public void run(){
			
			try{
				mmSocket.connect();
				
				mHandler.post(new Runnable(){
					@Override
					public void run(){
						AlertBox("connected", "Connected!");
					}
					
				});
				
			
				
			}catch(IOException connectException){
				
				final String errMess = connectException.toString();
				
				mHandler.post(new Runnable(){
					@Override
					public void run(){
						AlertBox("IOExcept", errMess);
					}
				});
				
				try{
					mmSocket.close();
				}
				catch(IOException closeException){}

				//AlertBox("IOExcept", connectException.toString());
				return;
			}
			
			mState = STATE_CONNECTED;
			connected(mmSocket);
			
		}
		
		public void cancel(){
			try{
				mmSocket.close();
			}catch (IOException e){}
		}
		
	};
	
	
	private class ConnectedThread extends Thread{
		
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private int numSent = 0;
		private String obdCommand = "";
		private String reply = "";
		
		public ConnectedThread(BluetoothSocket socket){
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}catch(IOException e){}
			
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		
		public void run(){

			byte[] buffer = new byte[8];
			int bytes;
			
			//first message we want to resest device, turn off echo, try to set protocal to Iso 9141
			obdCommand = "AT WS\r";
			write(obdCommand.getBytes());
			
			while(true){

				try{

					bytes = mmInStream.read(buffer);
					
					String sb = new String(buffer, 0, bytes);
				
					if(bytes > 0 ){
						Message message = mHandler.obtainMessage(MainActivity.WRITE_PROMPT, -1, -1);

						Bundle bundle = new Bundle();
						bundle.putString("commData", sb);
						message.setData(bundle);
						message.sendToTarget();
						
						parseResponse(sb, bytes);	
						//message = mHandler.obtainMessage(MainActivity.MESSAGE_READ, -1, -1, sb);						
					}				
				}catch(IOException e){	
					break;
				}
			}	
		}
		
		public void parseResponse(String response, int numBytes){
			String tmpStr = new String();
		
			int byteOne, byteTwo;
			
			Bundle bundle = new Bundle();	
			
			if(response.charAt(response.length()-1) == '>'){
				switch(numSent){
				case 0: 
					obdCommand = "AT SPA3\r";
					++numSent;
					break;
				case 1:
					obdCommand = "AT E0\r";
					++numSent;
					break;
				case 2:
					obdCommand = "01 0D\r";
					++numSent;
					break;
				case 3:
					obdCommand = "01 10\r";
					++numSent; //this is just incase i add more cases, 
					numSent =2;
					break;
				}
				Message message = mHandler.obtainMessage(MainActivity.WRITE_PROMPT, -1, -1);

				bundle.putString("commData", obdCommand);
				message.setData(bundle);
				message.sendToTarget();
				write(obdCommand.getBytes());
			}
			
			if(response.contains("41")){
				reply = response.substring(response.indexOf("41"));
			}else if(!reply.isEmpty()){
				reply = reply.concat(response);
			}

			if(reply.contains("\r")){
				reply = reply.substring(reply.indexOf("41"), reply.indexOf("\r")-1);


				if(reply.contains("41 0D")){
					tmpStr = reply.substring(6);//this only returns one byte, so this is that byte
					byteOne = Integer.parseInt(tmpStr, 16);
					vSpeed = byteOne;
				}else if(reply.contains("41 10")){
					tmpStr = reply.substring(6);
					byteOne = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(" ")), 16);
					byteTwo = Integer.parseInt(tmpStr.substring(tmpStr.indexOf(" ")+1), 16);
					MAF = (((double)byteOne*256.0)+(double)byteTwo)/100.0;

					MPG = (2757.142  *  .621371  *   vSpeed)
							/(3600.0  *  MAF  *  (1/14.7));
					DecimalFormat df = new DecimalFormat("#.###");
					MPG = Double.valueOf(df.format(MPG));

					tmpStr = "\r\rVehicle Speed: " + Double.toString(vSpeed) + "\rMass Air Flow: "
							+ Double.toString(MAF) + "\rMiles per Gallon: " + Double.toString(MPG) + "\r\r";

					bundle.putString("mpgData", Double.toString(MPG));
					Message calcMessage = mHandler.obtainMessage(MainActivity.WRITE_SCREEN, -1, -1);
					calcMessage.setData(bundle);
					calcMessage.sendToTarget();
				}
				reply = "";
			}

			
			
		}
		
		
		public void write(byte[] data){
				
			try{
				mmOutStream.write(data);
			}catch(IOException e){}
		}
		
		
		
		public void cancel(){
			try{
				mmSocket.close();
			}catch(IOException e){}
		}	
	};

	public synchronized void connect(BluetoothDevice device){
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}
	
	public synchronized void connected(BluetoothSocket socket){
		//mConnectThread.cancel();
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
	}
	
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

	public void AlertBox( String title, String message ){
	    new AlertDialog.Builder(parentContext)
	    .setTitle( title )
	    .setMessage( message + " Press OK to exit." )
	    .setPositiveButton("OK", new OnClickListener() {
	        public void onClick(DialogInterface arg0, int arg1) {
	          //finish();
	        }
	    }).show();
	  }
    
}
