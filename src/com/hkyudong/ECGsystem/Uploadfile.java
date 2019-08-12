package com.hkyudong.ECGsystem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Uploadfile extends Activity {
	protected static final int REQUEST_SELECTFILE = 1;

	private TextView mtv1 = null;
	private TextView mtv2 = null;
	private Button bupload = null;
	private Button backbtn = null;
	private Button selectfileButton = null;
	private Button changeNameButton = null;
	private String userNameString = "temp";

	private String uploadFilepath = null;
	private String actionUrl = "http://192.168.16.2/electrocardiogram/uploadfile.php";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upload);
		
		
		Intent newintent=getIntent();
		Bundle bundle=newintent.getExtras();
		uploadFilepath = bundle.getString("filepath");
		userNameString = bundle.getString("username");
		
		mtv1 = (TextView) findViewById(R.id.txt_filepath);
		mtv1.setText("文件路径：\n" + uploadFilepath);
		mtv2 = (TextView) findViewById(R.id.txt_actionpath);
		mtv2.setText("上传地址：\n" + actionUrl);
		bupload = (Button) findViewById(R.id.btn_sendfile);
		bupload.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (uploadFilepath == null) {
					new AlertDialog.Builder(Uploadfile.this)
					.setTitle("提示：")
					.setIcon(android.R.drawable.ic_delete)
					.setMessage("未选择文件")
					.setNegativeButton("确定", null)
					.show();					
				}else {
					FileUploadTask fileuploadtask = new FileUploadTask();
					fileuploadtask.execute();
				}
				
			}
		});
		backbtn = (Button) findViewById(R.id.btn_uploadback);
		backbtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent2 = new Intent();
				setResult(333,intent2);
				finish();
			}
		});
		
		selectfileButton = (Button) findViewById(R.id.btn_upload_changeFile);
		selectfileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent selectIntent = new Intent();
				selectIntent.setClass(Uploadfile.this, FileListActivity.class);
				selectIntent.putExtra("username", "");
				startActivityForResult(selectIntent, REQUEST_SELECTFILE);
			}
		});
		
		changeNameButton = (Button) findViewById(R.id.btn_upload_changeName);
		changeNameButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (uploadFilepath == null) {
					new AlertDialog.Builder(Uploadfile.this)
					.setTitle("提示：")
					.setIcon(android.R.drawable.ic_delete)
					.setMessage("未选择文件")
					.setNegativeButton("确定", null)
					.show();					
				}else {
					 final EditText inputServer = new EditText(Uploadfile.this);
				        new AlertDialog.Builder(Uploadfile.this).setTitle("请输入新文件名：")
				        .setIcon(android.R.drawable.ic_dialog_info)
				        .setView(inputServer)
				        .setNegativeButton("取消", null)
				        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

				            public void onClick(DialogInterface dialog, int which) {
				               //inputServer.getText().toString();
				               File renameFile = new File(uploadFilepath);
								String filepathString = renameFile.getParent()+"/";
								uploadFilepath=filepathString+inputServer.getText().toString()+".txt";
								renameFile.renameTo(new File(uploadFilepath));
								mtv1.setText(uploadFilepath);
				             }
				        }).show();
					
				}
			}
		});
	}

	class FileUploadTask extends AsyncTask<Object, Integer, Void> {

		private ProgressDialog dialog = null;
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		DataInputStream inputStream = null;
	
		
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";

		File uploadFile = new File(uploadFilepath);
		long totalSize = uploadFile.length(); // Get size of file, bytes

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(Uploadfile.this);
			dialog.setMessage("正在上传...");
			dialog.setIndeterminate(false);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setProgress(0);
			dialog.show();
		}

		@Override
		protected Void doInBackground(Object... arg0) {

			long length = 0;
			int progress;
			int bytesRead, bytesAvailable, bufferSize;
			byte[] buffer;
			int maxBufferSize = 256 * 1024;// 256KB

			try {
				FileInputStream fileInputStream = new FileInputStream(new File(
						uploadFilepath));

				URL url = new URL(actionUrl+"?user="+userNameString);
				connection = (HttpURLConnection) url.openConnection();

				// Set size of every block for post
				connection.setChunkedStreamingMode(256 * 1024);// 256KB

				// Allow Inputs & Outputs
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);

				// Enable POST method
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Connection", "Keep-Alive");
				connection.setRequestProperty("Charset", "UTF-8");
				connection.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);

				outputStream = new DataOutputStream(
						connection.getOutputStream());
				outputStream.writeBytes(twoHyphens + boundary + lineEnd);
				outputStream
						.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
								+ uploadFilepath + "\"" + lineEnd);
				outputStream.writeBytes(lineEnd);

				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// Read file
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				while (bytesRead > 0) {
					outputStream.write(buffer, 0, bufferSize);
					length += bufferSize;
					progress = (int) ((length * 100) / totalSize);
					publishProgress(progress);

					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}
				outputStream.writeBytes(lineEnd);
				outputStream.writeBytes(twoHyphens + boundary + twoHyphens
						+ lineEnd);
				publishProgress(100);



				fileInputStream.close();
				outputStream.flush();
				outputStream.close();

			} catch (Exception ex) {
				// Exception handling
				// showDialog("" + ex);
				// Toast toast = Toast.makeText(UploadtestActivity.this, "" +
				// ex,
				// Toast.LENGTH_LONG);

			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			dialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				dialog.dismiss();
				// TODO Auto-generated method stub
			} catch (Exception e) {
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
		case REQUEST_SELECTFILE:
			if (Activity.RESULT_OK == resultCode) {
				uploadFilepath = data.getExtras().getString(FileListActivity.EXTRA_FILE_PATH);
				mtv1.setText("文件路径：\n" + uploadFilepath);
			}
			
			break;
		default:
			
			break;
		
		};
	}
	
	
}
