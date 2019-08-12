package com.hkyudong.ECGsystem;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import com.hkyudong.ECGsystem.R;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends Activity {

	public static final String USER = "username";
	AutoCompleteTextView cardNumAuto;
	EditText passwordET;
	Button logBT;
	CheckBox savePasswordCB;
	SharedPreferences sp;
	String cardNumStr;
	String passwordStr;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		cardNumAuto = (AutoCompleteTextView) findViewById(R.id.cardNumAuto);
		passwordET = (EditText) findViewById(R.id.passwordET);
		logBT = (Button) findViewById(R.id.logBT);

		sp = this.getSharedPreferences("passwordFile", MODE_PRIVATE);
		savePasswordCB = (CheckBox) findViewById(R.id.savePasswordCB);
		savePasswordCB.setChecked(true);// Ĭ��Ϊ��ס����
		cardNumAuto.setThreshold(1);// ����1����ĸ�Ϳ�ʼ�Զ���ʾ
		passwordET.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		// ��������ΪInputType.TYPE_TEXT_VARIATION_PASSWORD��Ҳ����0x81
		// ��ʾ����ΪInputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD��Ҳ����0x91

		cardNumAuto.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				String[] allUserName = new String[sp.getAll().size()];// sp.getAll().size()���ص����ж��ٸ���ֵ��
				allUserName = sp.getAll().keySet().toArray(new String[0]);
				// sp.getAll()����һ��hash map
				// keySet()�õ�����a set of the keys.
				// hash map����key-value��ɵ�

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						Login.this,
						android.R.layout.simple_dropdown_item_1line,
						allUserName);

				cardNumAuto.setAdapter(adapter);// ��������������

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				passwordET.setText(sp.getString(cardNumAuto.getText()
						.toString(), ""));// �Զ���������

			}
		});

		// ��½
		logBT.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				cardNumStr = cardNumAuto.getText().toString();
				passwordStr = passwordET.getText().toString();

				if (!((cardNumStr.equals("test")) && (passwordStr
						.equals("test")))) {
					Toast.makeText(Login.this, "�����������������",
							Toast.LENGTH_SHORT).show();
				} else {
					if (savePasswordCB.isChecked()) {// ��½�ɹ��ű�������
						sp.edit().putString(cardNumStr, passwordStr).commit();
					}
					Toast.makeText(Login.this, "��½�ɹ�!",
							Toast.LENGTH_SHORT).show();
					// ��ת����һ��Activity
					// do something
					Intent temptIntent=new Intent();
					temptIntent.setClass(Login.this, ECGmain.class);
					temptIntent.putExtra(USER, cardNumStr);
					startActivity(temptIntent);
					Login.this.finish();

				}

			}
		});
		Button tempUserButton = (Button) findViewById(R.id.btn_TempUserLog);
		tempUserButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent temptIntent=new Intent();
				temptIntent.setClass(Login.this, ECGmain.class);
				temptIntent.putExtra(USER, "temp");
				startActivity(temptIntent);
				Login.this.finish();
			}
		});

	}

}