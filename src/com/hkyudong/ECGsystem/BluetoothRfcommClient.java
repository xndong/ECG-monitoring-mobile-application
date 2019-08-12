package com.hkyudong.ECGsystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 给一个蓝牙地址 本类进行连接   BluetoothRfcommClient.connect(device)
 **/
public class BluetoothRfcommClient {

    // Unique UUID for this application
    private static final UUID MY_UUID = 
    	//UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    	UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    //public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	public boolean wait = false;
	
	
	/**
	 * 暂停设置
	 * 
	 */
	public void set_wait(boolean newwait) {//暂停设置标志
		wait = newwait;
	}
    /**
     *构造函数
     */
    public BluetoothRfcommClient(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * 设置状态
     * */
    private synchronized void setState(int state) {
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(ECGmain.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * 返回状态 
     * */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 开始函数入口 
     * */
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        setState(STATE_NONE);
    }

    /**
     * 提供设备地址，进行连接
     */
    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * 保持连接
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(ECGmain.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ECGmain.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     *stop
     */
    public synchronized void stop() {
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_NONE);
    }

    /**
     * 连接失败的操作.
     */
    private void connectionFailed() {
        setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(ECGmain.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ECGmain.TOAST, "无法连接设备e");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * 连接丢失的操作.
     */
    private void connectionLost() {
        setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(ECGmain.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ECGmain.TOAST, "设备连接丢失");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * 连接线程.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                //
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a  successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    //
                }
                // Start the service over to restart listening mode
                BluetoothRfcommClient.this.start();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothRfcommClient.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private static final String MYTAG = "BluetoothRfcommClient";
		private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
        }
        
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            String dataString = "";
            // Keep listening to the InputStream while connected
            while (true) {//这里的true可以改为一个开关
            	if (wait) {
    				try {
    					sleep(10);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}else{
    				try {
                    // Read from the InputStream
    					bytes = mmInStream.read(buffer);                    
    					for (int i = 0; i < bytes; i++) {
    						Log.i(MYTAG,Integer.toString(buffer[i]), null);
    							if (buffer[i]==44&& ""!=dataString) {
    								//int sendint = Integer.parseInt(dataString);
    								mHandler.obtainMessage(ECGmain.MESSAGE_READ, -1, -1, dataString)
    								.sendToTarget();
    								dataString = "";
    							}else	dataString = dataString+AsciiToChar(buffer[i]);
    					}                    
    				} catch (IOException e) {
                    //
    					connectionLost();
    					break;
    				}
    			}
            }
        }
        public char AsciiToChar(int asci) {//ascii码转化为他所代表的字符
        //	char aaa = (char)Integer.parseInt(Integer.toString(asci));;
        //	Log.i(MYTAG, Integer.toString(asci)+"---->"+aaa, null);
			return (char)Integer.parseInt(Integer.toString(asci));
		}
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //
            }
        }
    }
}
