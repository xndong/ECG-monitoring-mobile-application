package com.hkyudong.ECGsystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.hkyudong.ECGsystem.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ECGmain extends Activity implements  Button.OnClickListener{
	
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final int FILE_READ = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String DIR = "WaveData";
    private String UserName = null;
    
    
    private final String filepathString = Environment.getExternalStorageDirectory().toString()+File.separator+DIR+File.separator;
    private String newfilepathString = null;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CONNECT_FILE = 3;
    // bt-uart constants

	private static final String MYTAG = "hkyudong01";

	//protected static final String USERNAME = "hkyudong";

	

	private TextView mfileStatus;
    private TextView mBTStatus;//运行状态TextView
    private TextView txtYshrink;//y周伸缩
    private TextView txtusername;
    private Button btn_scale_up, btn_scale_down;
    private Button mConnectButton;//连接button
    
    private Button readfileButton;
    private Button uploadfileButton;
    private Button closebtButton;
    private Button clearScreenButton;
    
    private ToggleButton run_buton;//运行状态ToggleButton
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter（适配器）
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfcommClient mRfcommClient = null;
    
    private ReadOldFile readOldFile = null;
    
    protected PowerManager.WakeLock mWakeLock;
    
    public WaveformView mWaveform = null;
    
    private PrintStream fileoutPrintStream = null;
    private String strNewFileName = null;	

	//my
	private  int Yshrink = 1;
	private int ScreenWidth = 0;
	private int ScreenHeight = 0;
	private int oldWidth=0;
	private int oldHeight=0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.main);
        
        WindowManager wm = (WindowManager) getBaseContext()
        .getSystemService(Context.WINDOW_SERVICE);
		ScreenWidth= wm.getDefaultDisplay().getWidth();
		ScreenHeight = wm.getDefaultDisplay().getHeight();
        Intent getuserIntent  =getIntent();
        Bundle bundle = getuserIntent.getExtras();
        UserName = bundle.getString("username");
        
        mBTStatus = (TextView) findViewById(R.id.txt_btstatus);
        mfileStatus = (TextView) findViewById(R.id.txt_filestatus);
        txtusername = (TextView)findViewById(R.id.txt_username);
        if (UserName == "temp") {
			txtusername.setText("匿名用户");
		}else {
			txtusername.setText(UserName);
		}
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "不存在蓝牙模块", Toast.LENGTH_LONG).show();
            finish();
            return;
        }else{Toast.makeText(this, "存在蓝牙模块", Toast.LENGTH_LONG).show();}
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mRfcommClient == null) SetupECG();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mRfcommClient != null) {
            if (mRfcommClient.getState() == BluetoothRfcommClient.STATE_NONE) {
              mRfcommClient.start();
            }
        }
    }

    private void SetupECG() {
        
       txtYshrink = (TextView)findViewById(R.id.txt_Yshrink);
       txtYshrink.setText(Integer.toString(Yshrink));
        run_buton = (ToggleButton) findViewById(R.id.tbtn_runtoggle);
        run_buton.setOnClickListener(this);
        
        btn_scale_up = (Button) findViewById(R.id.btn_scale_increase);
        btn_scale_down = (Button) findViewById(R.id.btn_scale_decrease);
        btn_scale_up.setOnClickListener(this);
        btn_scale_down.setOnClickListener(this);        
        
        readfileButton = (Button) findViewById(R.id.button_readfile);
        readfileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				clear();
				if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
					Intent filelistIntent = new Intent();
					filelistIntent.setClass(ECGmain.this, FileListActivity.class);
					filelistIntent.putExtra("username", UserName);
					startActivityForResult(filelistIntent, REQUEST_CONNECT_FILE);
					
				}else {
					Toast.makeText(getApplicationContext(), "读取失败，sd卡不存在", Toast.LENGTH_LONG).show();
				}
			    
			}
		});
        
        mConnectButton = (Button) findViewById(R.id.button_connect);
        mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				clear();
				BTConnect();
			}
		});
        
        uploadfileButton = (Button) findViewById(R.id.button_NET_sendfile);
        uploadfileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				clear();
				if (null != fileoutPrintStream) {//关闭输出文件流
					fileoutPrintStream.close();
				}
				Intent uploadIntent = new Intent();
				uploadIntent.setClass(ECGmain.this, Uploadfile.class);
				uploadIntent.putExtra("filepath", newfilepathString);
				uploadIntent.putExtra("username", UserName);
				startActivityForResult(uploadIntent,222);
			}
		});
        
        closebtButton = (Button)findViewById(R.id.button_closebt);
        closebtButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// TODO Auto-generated method stub
				 if (mRfcommClient != null) {mRfcommClient.stop(); run_buton.setChecked(false);}
				 
			}
		});
//        clearScreenButton = (Button)findViewById(R.id.btn_ClearScreen);
//        clearScreenButton.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				mWaveform.clearScreen();
//			}
//		});
        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfcommClient(this, mHandler);
        
        mWaveform = (WaveformView)findViewById(R.id.WaveformArea);
        mWaveform.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				if (mWaveform.get_method()==WaveformView.METHOD_NORMALSCREEN) {
					WindowManager wm = (WindowManager) getBaseContext()
					.getSystemService(Context.WINDOW_SERVICE);
					oldHeight=mWaveform.getHeight();
					oldWidth=mWaveform.getWidth();
					ScreenWidth= wm.getDefaultDisplay().getWidth();
					ScreenHeight = wm.getDefaultDisplay().getHeight();
					Log.i(MYTAG, "ScreenWidth"+Integer.toString(ScreenWidth)+"ScreenHeight"+Integer.toString(ScreenHeight));
					RelativeLayout.LayoutParams lp=new  RelativeLayout.LayoutParams(ScreenWidth,ScreenHeight);  
					//lp.=Gravity.CENTER_HORIZONTAL; 
					mWaveform.setLayoutParams(lp);
					mWaveform.set_method(WaveformView.METHOD_FULLSCREEN);
					return false;
				}else {
					RelativeLayout.LayoutParams lp=new  RelativeLayout.LayoutParams(oldWidth,oldHeight);  
					//lp.=Gravity.CENTER_HORIZONTAL; 
					mWaveform.setLayoutParams(lp);
					mWaveform.set_method(WaveformView.METHOD_NORMALSCREEN);
					return false;
				}
				
			}
		});
        
        
    }
    
    @Override
    public void  onClick(View v){
    	int buttonID;
    	buttonID = v.getId();
    	switch (buttonID){
    	case R.id.btn_scale_increase :
    		if(1 < Yshrink){
    			Yshrink--;
    			mWaveform.set_Yshrink(Yshrink);
    			txtYshrink.setText(Integer.toString(Yshrink));
    		}

   		break;
    	case R.id.btn_scale_decrease :
    		if(50 > Yshrink){
    			Yshrink++;
    			mWaveform.set_Yshrink(Yshrink);
    			txtYshrink.setText(Integer.toString(Yshrink));
    		}
   		break;
    	case R.id.tbtn_runtoggle :
    		if(run_buton.isChecked()){
    			if (null !=readOldFile) {
					readOldFile.set_wait(false);
					mWaveform.set_wait(false);
				};	
				if (null != mRfcommClient) {
					mRfcommClient.set_wait(false);
					mWaveform.set_wait(false);
				}
				
    		}else{
    			//readOldFile.cancle();
    			
    			if (null !=readOldFile) {
					readOldFile.set_wait(true);
					mWaveform.set_wait(true);
				};
				if (null != mRfcommClient) {
					mRfcommClient.set_wait(true);
					mWaveform.set_wait(true);
				}
 //   			Log.i(MYTAG, "aaaaaaaa", null);
    		}
    		break;
    	}
    }
    
    private void BTConnect(){
    	Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    @Override
    public void onDestroy() {    	
        super.onDestroy();
        clear();
        
        // release screen being on
        if (mWakeLock.isHeld()) { 
            mWakeLock.release();
        }
    }
    /**
     * 蓝牙和文件状态清零
     * 结束前面已经存在的读文件线程和蓝牙线程
     */
    private void clear() {
    	mWaveform.clearScreen();
    	mWaveform.set_wait(true);
    	run_buton.setChecked(false);
    	if (readOldFile != null) {
    		readOldFile.set_wait(true);
    		//readOldFile.cancel();
    		readOldFile = null;
    	}
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) {mRfcommClient.stop();}
        
        if (null != fileoutPrintStream) {//关闭输出文件流
			fileoutPrintStream.close();
		}
        
	}


    // The Handler that gets information back from the BluetoothRfcommClient
    private final Handler mHandler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothRfcommClient.STATE_CONNECTED:
                	run_buton.setChecked(true);
                    mBTStatus.setText(R.string.title_connected_to);
                    mBTStatus.append("\n" + mConnectedDeviceName);
                    //以下为建立一个存放数据的文件代码  
                	SimpleDateFormat    formatter    =   new    SimpleDateFormat    ("yyyyMMdd-hhmmss");     
                	Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间     
                	strNewFileName    =    formatter.format(curDate); 
                	newfilepathString = filepathString+UserName+File.separator+strNewFileName+".txt";
                	File file = new File(newfilepathString);
                	Log.i(MYTAG, filepathString+strNewFileName+".txt");
                	if (! file.getParentFile().getParentFile().exists()) {//父文件夹不存在就建立一个
          			file.getParentFile().getParentFile().mkdirs();
          			file.getParentFile().mkdirs();
          			}else {
          				if (! file.getParentFile().exists()) {//父文件夹不存在就建立一个
                  			file.getParentFile().mkdirs();
                  			}
					}
                	try {
                  
                		fileoutPrintStream = new PrintStream(new FileOutputStream(file));
                	} catch (Exception e) {
                		// TODO: handle exception
                		Log.i(MYTAG, "创建文件失败!");
                	} 
                	mWaveform.set_wait(false);
                    break;
                case BluetoothRfcommClient.STATE_CONNECTING:
                	mBTStatus.setText(R.string.title_connecting);
                    break;
                //case BluetoothRfcommClient.STATE_LISTEN:
                case BluetoothRfcommClient.STATE_NONE:
                	mBTStatus.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_READ:
            	//Log.i(MYTAG, "MESSAGE_READ", null);
                String readBuf = (String) msg.obj;
                int readBufint = Integer.parseInt(readBuf);
                //Log.i(MYTAG, readBuf, null);
                Log.i(MYTAG, Integer.toString(readBufint), null);
                mWaveform.set_data(readBufint);
                fileoutPrintStream.print(readBufint);
                fileoutPrintStream.print(",");                                
                Log.i(MYTAG, Integer.toString(readBufint), null);
                
                break;
            case FILE_READ : 
            	int readdata;
            	readdata = msg.arg1;
 //           	Log.i(MYTAG, Integer.toString(readdata), null);
            	mWaveform.set_data(readdata);            	
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {            	
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mRfcommClient.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
            	SetupECG();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        case REQUEST_CONNECT_FILE : 
        	if (Activity.RESULT_OK == resultCode) {
				String oldfilepathString = data.getExtras().getString(FileListActivity.EXTRA_FILE_PATH);
				Log.i(MYTAG, oldfilepathString);
				readOldFile = new ReadOldFile(ECGmain.this, mHandler, null,oldfilepathString);
			    readOldFile.start();
			    run_buton.setChecked(true);
			    readOldFile.set_wait(false);
			    mfileStatus.setText("已连接到文件："+data.getExtras().getString(FileListActivity.EXTRA_FILE_NAME));
			    mfileStatus.setVisibility(View.VISIBLE);
			    mWaveform.set_wait(false);
			}
        }
    }
    public void onBackPressed() { 
    	if (mWaveform.get_method()==WaveformView.METHOD_NORMALSCREEN){
    		clear();    		
			new AlertDialog.Builder(this).setTitle("确认退出吗？") 
    	    .setIcon(android.R.drawable.ic_dialog_info) 
    	    .setPositiveButton("确定", new DialogInterface.OnClickListener() { 
    	 
    	        @Override 
    	        public void onClick(DialogInterface dialog, int which) {     	        	
    	        // 点击“确认”后的操作 
    	        	
//    	        	new AlertDialog.Builder(ECGmain.this) 
//    	        	.setTitle("提示：")
//    	        	.setMessage("是否上传数据到服务器？")
//    	        	.setPositiveButton("是", null)
//    	        	.setNegativeButton("否", null)
//    	        	.show();
//    	        	clear();
    	        	ECGmain.this.finish();    	 
    	        } 
    	    }) 
    	    .setNegativeButton("返回", new DialogInterface.OnClickListener() { 
    	 
    	        @Override 
    	        public void onClick(DialogInterface dialog, int which) { 
    	        // 点击“返回”后的操作,这里不设置没有任何操作 
    	        } 
    	    }).show(); 
    	// super.onBackPressed(); 
    	   }else {
    		   RelativeLayout.LayoutParams lp=new  RelativeLayout.LayoutParams(oldWidth,oldHeight);  
				//lp.=Gravity.CENTER_HORIZONTAL; 
				mWaveform.setLayoutParams(lp);
				mWaveform.set_method(WaveformView.METHOD_NORMALSCREEN);
    	   }
	}
    	
     /**
      * 选项菜单
      */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(Menu.NONE, Menu.FIRST+1, 1, "关于软件");
		menu.add(Menu.NONE, Menu.FIRST+2, 2, " 退出");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		switch (item.getItemId()) {
		case Menu.FIRST+1:
			new AlertDialog.Builder(this).setTitle("关于心电监护系统") 
    	    .setIcon(android.R.drawable.ic_dialog_info) 
    	    .setMessage("西电国创项目成员：\n董晓宁，张金昌，于东\nSoft Design By 于东")
    	    .setNegativeButton("确定",null)
    	    .show();
			break;
		case Menu.FIRST+2:
			clear();
			ECGmain.this.finish();
			break;	

		default:
			break;
		}
		return false;
	}
   
}
