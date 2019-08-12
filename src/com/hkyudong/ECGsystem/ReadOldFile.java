package com.hkyudong.ECGsystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class ReadOldFile extends Thread{
//	private static final String DIR = "WaveData";
	private static final String MYTAG = "readoldfile";
	private final String filepathString;
	private final String filenameString;
	private final Handler mHandler;
	private Scanner mScanner = null;
	private boolean wait = true;
	private File file = null;
	
	public ReadOldFile(Context context,Handler handler,String filename,String filepath) {//给定文件的绝对路径时使用的构造函数
		// TODO Auto-generated constructor stub
		mHandler = handler;
		filenameString = filename;
		filepathString = filepath;
		file = new File(filepathString);
	}	
	public ReadOldFile(Context context,Handler handler,String filename) {//只给定文件名的重载构造函数，默认路径为：/mnt/sdcard/WaveData/+filename
		// TODO Auto-generated constructor stub
		mHandler = handler;
		filenameString = filename;
		filepathString = null;
		file = new File(Environment.getExternalStorageDirectory().toString()+File.separator+ECGmain.DIR+File.separator+filenameString);
		if (! file.getParentFile().exists()) {//父文件夹不存在就建立一个
			file.getParentFile().mkdirs();
		}
	}
	public void set_wait(boolean newwait) {//暂停设置标志
		wait = newwait;
	}
	public void run() {
/*		try {
			mScanner = new Scanner(new FileInputStream(file)).useDelimiter(",");//已逗号作为整型数据切分标准			
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			System.out.println("建立Scaner失败");
		}
		while (true) {
//			if (cancel) {
//				return;
//			}
			if (wait) {
				try {
					sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if (mScanner != null && mScanner.hasNext()) {
				//int data1;
				data1 = mScanner.nextInt();
				//Log.i(MYTAG, Integer.toString(data1), null);
				mHandler.obtainMessage(ECGmain.FILE_READ, data1,-1).sendToTarget();
			}
			Log.i(MYTAG,mScanner.toString(), null);
			//Log.i(MYTAG, "while (true) still running", null);
		}
*/		
		try {
			mScanner = new Scanner(file).useDelimiter(",");//已逗号作为整型数据切分标准			
			while (mScanner.hasNext()) {
				while (wait) {//判断是否暂停
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
				}
				int data1;
				data1 = mScanner.nextInt();
				//Log.i(MYTAG, Integer.toString(data1), null);
				mHandler.obtainMessage(ECGmain.FILE_READ, data1,-1).sendToTarget();
			}
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			System.out.println("建立Scaner失败");
		}finally{
			if (mScanner !=null) {
				mScanner.close();//关闭文件
			}
		}
		
//		mScanner.close();
	}
/*	public void cancel() {
		//cancel = true;
		if (null != mScanner) {
			//this.set_wait(true);
			//mScanner.close();
			mScanner = null;
			
		}
	}
*/
}

