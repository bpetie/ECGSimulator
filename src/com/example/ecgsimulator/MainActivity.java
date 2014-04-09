package com.example.ecgsimulator;


import java.util.Timer;
import java.util.TimerTask;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity
{
  //Set to false to turn off all debug output
  private static final boolean D = true;
  private static final String TAG = "MainActivity";
  
  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
  private static final int REQUEST_ENABLE_BT = 3;
  
  // Message types sent from the BluetoothChatService Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;

  // Key names received from the BluetoothChatService Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";
  public static final String HEART_START = "0,0,0,0,0,0,0,0,0,0,10,15,20,15,0,0,0,0,0,-10,30,90,10,-30,0";
  public static final String GOOD_ST = "0,0,0,0,0,0,";
  public static final String BAD_ST = "30,30,30,30,30,30,";
  public static final String HEART_END = "10,15,18,22,25,24,22,18,10,0,0,0,5,0,0,0,0,0";
  
  public BluetoothAdapter mBluetoothAdapter;
  public BluetoothChatService mChatService;
  private StringBuffer mOutStringBuffer;
  private String mConnectedDeviceName = null;
  private Timer myTimer;
  private MyTimerTask myTask;
  private boolean badST = false;
  private Button UIButton;


  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if( savedInstanceState == null )
    {
      getSupportFragmentManager().beginTransaction()
          .add(R.id.container, new PlaceholderFragment()).commit();
    }

    //UIButton = (Button) findViewById(R.id.sendBadST);
    //UIButton.setBackgroundColor(Color.GREEN);

    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    // Timer for sending the data every second
    myTask = new MyTimerTask();
    myTimer = new Timer();
    
    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
        Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        finish();
        return;
    }
  }

  @Override
  public void onStart() {
      super.onStart();

      // If BT is not on, request that it be enabled.
      // setupChat() will then be called during onActivityResult
      if (!mBluetoothAdapter.isEnabled()) {
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      // Otherwise, setup the chat session
      } 
      else {
          if (mChatService == null) 
            setupChat();
      }
  }
  
  @Override
  public synchronized void onResume() {
      super.onResume();
      if(D) Log.e(TAG, "+ ON RESUME +");

      // Performing this check in onResume() covers the case in which BT was
      // not enabled during onStart(), so we were paused to enable it...
      // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
      if (mChatService != null) {
          // Only if the state is STATE_NONE, do we know that we haven't started already
          if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
            // Start the Bluetooth chat services
            mChatService.start();
          }
      }
  }
  
  @Override
  public void onDestroy() {
      super.onDestroy();
      // Stop the Bluetooth chat services
      myTimer.cancel();
      myTimer.purge();
      if (mChatService != null) mChatService.stop();
      if(D) Log.e(TAG, "--- ON DESTROY ---");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {

    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if( id == R.id.action_settings )
    {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  
  /**
   * A placeholder fragment containing a simple view.
   */
  public static class PlaceholderFragment extends Fragment
  {

    public PlaceholderFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState)
    {
      View rootView = inflater
          .inflate(R.layout.fragment_main, container, false);
      return rootView;
    }
  }
  
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(D) Log.d(TAG, "onActivityResult " + resultCode);
    switch (requestCode) {
    case REQUEST_CONNECT_DEVICE_SECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
            connectDevice(data, true);
        }
        break;
    case REQUEST_CONNECT_DEVICE_INSECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
            connectDevice(data, false);
        }
        break;
    case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
            // Bluetooth is now enabled, so set up a chat session
            setupChat();
        } else {
            // User did not enable Bluetooth or an error occurred
            Log.d(TAG, "BT not enabled");
            Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
  }
  private void connectDevice(Intent data, boolean secure) {
    // Get the device MAC address
    String address = data.getExtras()
        .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    // Get the BluetoothDevice object
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    // Attempt to connect to the device
    mChatService.connect(device, secure);
  }
  
  // The Handler that gets information back from the BluetoothChatService
  private final Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
          switch (msg.what) {
          case MESSAGE_STATE_CHANGE:
              if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
              switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                  if (D) Log.i(TAG, "Connected to " + mConnectedDeviceName);
                  startTimer();
                  break;
                  
                case BluetoothChatService.STATE_CONNECTING:
                  if (D) Log.i(TAG, getString(R.string.title_connecting));
                  stopTimer();
                  break;
                  
                case BluetoothChatService.STATE_LISTEN:
                  if (D) Log.i(TAG, "listening");
                  stopTimer();
                  break;

                case BluetoothChatService.STATE_NONE:
                  stopTimer();
                  if (D) Log.i(TAG, getString(R.string.title_not_connected));
                    //setStatus(R.string.title_not_connected);
                  break;
                }
                break;
              
          case MESSAGE_WRITE:
              byte[] writeBuf = (byte[]) msg.obj;
              // construct a string from the buffer
              String writeMessage = new String(writeBuf);
              //mConversationArrayAdapter.add("Me:  " + writeMessage);
              break;
          case MESSAGE_READ:
              byte[] readBuf = (byte[]) msg.obj;
              // construct a string from the valid bytes in the buffer
              String readMessage = new String(readBuf, 0, msg.arg1);
              if (D) Log.i(TAG, "recieved this message via bluetooth: '" + readMessage + "'");
              //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
              break;
          case MESSAGE_DEVICE_NAME:
              // save the connected device's name
              mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
              Toast.makeText(getApplicationContext(), "Connected to "
                             + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
              break;
          case MESSAGE_TOAST:
              Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                             Toast.LENGTH_SHORT).show();
              break;
          }
      }
  };
  
  
  private void setupChat() {

   /* // Initialize the send button with a listener that for click events
    mSendButton = (Button) findViewById(R.id.button_send);
    mSendButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            // Send a message using content of the edit text widget
            TextView view = (TextView) findViewById(R.id.edit_text_out);
            String message = view.getText().toString();
            sendMessage(message);
        }
    });*/

    // Initialize the BluetoothChatService to perform bluetooth connections
    mChatService = new BluetoothChatService(this, mHandler);

    // Initialize the buffer for outgoing messages
    mOutStringBuffer = new StringBuffer("");
  }
  
  public void connectButtonClicked(View v) {
    
    badST = !badST;
    if (badST) {
      //UIButton.setBackgroundColor(Color.RED);
    }
    else {
      //UIButton.setBackgroundColor(Color.GREEN);
    }
    /*// Launch the DeviceListActivity to see devices and do scan
    Intent serverIntent = new Intent(this, DeviceListActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);*/
 
  }
  
  public void discoverButtonClicked(View v) {
    if(D) Log.d(TAG, "ensure discoverable");
    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
  }
  
  public void startTimer() {
    myTimer = new Timer();
    myTask = new MyTimerTask();
    myTimer.schedule(myTask, 1000, 3000);
  }
  
  public void stopTimer() {
    myTimer.cancel();
    myTimer.purge();
    myTask.cancel();
  }

  
  class MyTimerTask extends TimerTask {
    public void run() {
      String sendString = HEART_START;
      if(D) Log.i(TAG, "ST is " + badST);
      if (badST) {
        sendString += BAD_ST;
        badST = false;
       // UIButton.setBackgroundColor(Color.GREEN);
      }
      else {
        sendString += GOOD_ST;
      }
      sendString += HEART_END; 
      
      byte[] send = sendString.getBytes();
      mChatService.write(send);
      if (D) Log.d(TAG, "Sent data");
    }
  }
}
