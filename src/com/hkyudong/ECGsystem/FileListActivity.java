package com.hkyudong.ECGsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.hkyudong.ECGsystem.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileListActivity extends ListActivity {
	/** Called when the activity is first created. */  
	public static final String EXTRA_FILE_PATH = "filepath";
	public static final String EXTRA_FILE_NAME = "filename";
	
	//private static final String DIR = "WaveData";	
	private static final String MYTAG = "FileListActivity";	
    private List<String> items = null;//�������  
    private List<String> paths = null;//���·��  
    private String rootPath =Environment.getExternalStorageDirectory().toString()+File.separator+ECGmain.DIR+File.separator;  //"/";  
    private TextView tv;  
    private String usernameString = "";
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.file_list);  
        
        Intent getusernameIntent = getIntent();
        Bundle bundle = getusernameIntent.getExtras();
        usernameString = bundle.getString("username");
        if ("" != usernameString) {
			usernameString += File.separator;
		}
        tv = (TextView) this.findViewById(R.id.TextView_file);  
        this.getFileDir(rootPath+usernameString);//��ȡrootPathĿ¼�µ��ļ�.  
    }  
 
    public void getFileDir(String filePath) {  
        try{  
            this.tv.setText("��ǰ·��:"+filePath);// ���õ�ǰ����·��  
            items = new ArrayList<String>();  
            paths = new ArrayList<String>();  
            File f = new File(filePath); 
            if (!f.exists()) {
				f.mkdirs();
			}
            File[] files = f.listFiles();// �г������ļ�  
            // ������Ǹ�Ŀ¼,���г����ظ�Ŀ¼����һĿ¼ѡ��  
            if (!filePath.equals("/")) {  
                items.add("���ظ�Ŀ¼");  
                paths.add("/");  
                items.add("������һ��Ŀ¼");  
                paths.add(f.getParent());  
            }  
            // �������ļ�����list��  
            if(files != null){  
                int count = files.length;// �ļ�����  
                for (int i = 0; i < count; i++) {  
                    File file = files[i];  
                    items.add(file.getName());  
                    paths.add(file.getPath());  
                }  
            }  
  
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,  
                    android.R.layout.simple_list_item_1, items);  
            this.setListAdapter(adapter);  
        }catch(Exception ex){  
            ex.printStackTrace();  
        }  
  
    }  
  
    protected void onListItemClick(ListView l, View v, int position, long id) {  
        super.onListItemClick(l, v, position, id);  
        String path = paths.get(position);  
        File file = new File(path);  
        //������ļ��оͼ����ֽ�  
        if(file.isDirectory()){  
            this.getFileDir(path);  
        }else{  
            new AlertDialog.Builder(this).setTitle("��ʾ").setMessage(file.getName()+" ��һ���ļ���").setPositiveButton("OK", new DialogInterface.OnClickListener(){  
  
                public void onClick(DialogInterface dialog, int which) {  
                                          
                }  
                  
            }).show(); 
            
            String filepath = path;
            Log.i(MYTAG, filepath);
            Intent backIntent =new Intent();
            backIntent.putExtra(EXTRA_FILE_PATH, filepath);
            backIntent.putExtra(EXTRA_FILE_NAME, file.getName());
            setResult(Activity.RESULT_OK, backIntent);
            finish();
        }  
    }  

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}
