package com.emrc_transport;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

import android.*;
import android.annotation.*;
import android.app.*;
import android.content.*;
import android.content.IntentFilter.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.location.*;
import android.net.*;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.*;
import android.provider.*;
import android.support.v4.view.*;
import android.support.v4.view.ViewPager.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.AdapterView.*;
import android.widget.*;

import com.emrc_transport.HomeListen.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import javax.net.ssl.*;

@SuppressWarnings("deprecation")
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener,
		OnItemSelectedListener, OnTouchListener, LocationListener, OnPageChangeListener {
	
	public static String ip = "xxx.xxx.xxx.xxx";
	public static int port = xxxx;
	private int id_Y = 2018, id_M = 10, id_D = 20;
	private int downX = 0, downY = 0;
	private boolean write = false, nfc = false, gps = false, car = false;
	private boolean home = false, sktRun = false, dblist = false;
	private double Lngitude, Latitude;
	private static final String TAG = "nfcproject";
	private static Spinner spinner;
	private static TextView medi_unit, medi_qunt, print_No, menu_No;
	private static ViewPager pager1;
	private AlertDialog alertDialog;
	private Button main_log, mediCar_btn0, mediCar_btn1, mediCar_btn2, mediCar_btn3;
	private Button mediCar_btn4, mediCar_btn5, mediCar_btn6, mediCar_btn7, mediCar_btn8;
	private Button mediCar_btn9, mediCar_btn10, mediCar_btn11;
	private EditText mediCar_unit, mediCar_brand;
	private HomeListen mHomeListen = null;
	private Handler SMSHandler = new Handler();
	private Handler GPSHandler = new Handler();
	private IntentFilter[] gNdefExchangeFilters, gWriteTagFilters;
	private ImageView main_left, main_right, medi_clear, medi_swap, menu_ok, menu_re, bg_emrc;
	// private TableRow btn_USER, btn_MASTER, btn_CAR;
	private LocationManager lms;
	private NfcAdapter nfcAdapter;
	private PendingIntent gNfcPendingIntent;
	private ProgressDialog mProgressDialog;
	private String bestProvider;
	private TextView print_item_0, print_item_1, print_item_2, print_item_3;
	private TextView print_item_4, print_item_5, print_item_6, mediCar_static;
	private TextWatcher textWatcher;
	// private ArrayList<Boolean> Car_trends = new ArrayList<Boolean>(); // 已回報
	private ArrayList<String> Treatment_location = new ArrayList<String>(); // 醫院座標
	private static final String KEY_STORE_CLIENT_PATH = "kserver.bks"; // 客户端要给服务器端认证的证书
	private static final String KEY_STORE_TRUST_PATH = "tclient.bks"; // 客户端验证服务器端的证书库
	private static final String KEY_STORE_PASSWORD = "123456"; // 客户端证书密码
	public static int gender = 2, leve_count = 5, phot_count = 0, dev_count = 20;
	public static int emrc_count = 0, hosp_count = 0, status_count = 0, textSize = 0;
	public static boolean net = false, link = false, user = false, pin = false, f = true, EN = false, dev = false;
	public static String mmsg, handmsg, number = "", car_brand = "", identity = "", emrgn;
	public static Socket skt;
	public static Bitmap info_photo = null, tmp_bmp = null;
	private static ListView menu_list, medi_list;

	public static DisplayMetrics metrics = new DisplayMetrics();
	public static ArrayList<String> menu_item = new ArrayList<String>();

	private static ArrayList<String> medi_item = new ArrayList<String>();
	private static ArrayList<String> lead_item = new ArrayList<String>();
	private static ArrayList<View> main_view = new ArrayList<View>();
	private static ArrayList<View> menu_view = new ArrayList<View>();
	private static ArrayList<View> medi_view = new ArrayList<View>();
	private static ArrayList<View> lead_view = new ArrayList<View>();

	public static ArrayList<Double> inju_front = new ArrayList<Double>();
	public static ArrayList<Double> inju_back = new ArrayList<Double>();
	public static ArrayList<Double> inju_target = new ArrayList<Double>();
	public static ArrayList<String> info_data = new ArrayList<String>();
	public static ArrayList<String> vita_data = new ArrayList<String>();
	public static ArrayList<String> emrgn_data = new ArrayList<String>();
	public static ArrayList<Bitmap> phot_bitmap = new ArrayList<Bitmap>();

	private ArrayList<String> Treatment_unit = new ArrayList<String>();
	private ArrayList<Integer> Treatment_leve = new ArrayList<Integer>();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		metrics = getResources().getDisplayMetrics();
		findViewById();
		load_DEV_DATA();
		checkToday(0, 0, 0, false);
		addTab();
		view_0();
		view_1();
		view_2();
		view_3();
		setTARGET();
		setNFC();
		setNET();
		setHOME();
		load_USER_DATA();
		// ==================================
		// 直接開啟救護車模式
		pager_upload();
		// for (int cc = 0; cc < 6; cc++) {
		// Car_trends.add(false);
		// }
		PPHandler.obtainMessage().sendToTarget();
		// ==================================
		setPermission();
	}

	protected void onStart() {
		super.onStart();
	}

	protected void onStop() {
		super.onStop();
	}

	protected void onPause() {
		super.onPause();
		if (home) {
			mHomeListen.stop();
		}
		if (nfc) {
			// 由於NfcAdapter啟動前景模式將相對花費更多的電力，要記得關閉。
			nfcAdapter.disableForegroundNdefPush(this);
		}
		if (gps) {
			lms.removeUpdates(this); // 離開頁面時停止更新
		}
	}

	protected void onResume() {
		super.onResume();
		if (home) {
			mHomeListen.start();
		}
		if (nfc) {
			// TODO 處理由Android系統送出應用程式處理的intent filter內容
			if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
				NdefMessage[] messages = getNdefMessages(getIntent());
				String data = Decrypt(new String(messages[0].getRecords()[0].getPayload()));
				// 往下送出該intent給其他的處理對象
				setIntent(new Intent());
				String msg = "";
				if (checkData(data)) {
					msg = Decrypt(data);
					// toast("De", this);
				} else {
					msg = data;
					// toast(msg, this);
					toast(getString(R.string.s_ts_57), this);
				}
				if (msg.length() > 0) {
					getNFCString(msg);
					// pager_upload(this);
					// pager.setCurrentItem(1);
					// USHandler.obtainMessage().sendToTarget();
				} else {
					toast(getString(R.string.s_ts_0), this);
				}
				// toast("onResume", this);
			}
			// 啟動前景模式支持Nfc intent處理
			enableNdefExchangeMode();
		}
		if (gps) {
			lms.requestLocationUpdates(bestProvider, 60000, 1, this);
			// 服務提供者、更新頻率60000毫秒=1分鐘、最短距離、地點改變時呼叫物件
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			save();
			new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.s_ts_61))
					.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {

						}
					}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							android.os.Process.killProcess(android.os.Process.myPid());
						}
					}).show();
			return true;
		default:
			break;
		}
		return false;
	}

	private void setHOME() {
		home = true;
		mHomeListen = new HomeListen(this);
		mHomeListen.setOnHomeBtnPressListener(new OnHomeBtnPressLitener() {
			public void onHomeBtnPress() {
				// toast(MainActivity.this, "按下Home按键！");
				save();
				android.os.Process.killProcess(android.os.Process.myPid());

			}

			public void onHomeBtnLongPress() {
				// toast(MainActivity.this, "长按Home按键！");
				save();
				android.os.Process.killProcess(android.os.Process.myPid());

			}
		});
	}

	@TargetApi(23)
	private void setPermission() {
		if (Build.VERSION.SDK_INT >= 23) {
			int PHONE = checkSelfPermission(Manifest.permission.CALL_PHONE);
			int LOCATION = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
			int STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			int CAMERA = checkSelfPermission(Manifest.permission.CAMERA);

			if (permission(PHONE) || permission(LOCATION) || permission(STORAGE) || permission(CAMERA)) {
				showMessageOKCancel("親愛的用戶您好:\n由於Android 6.0 以上的版本在權限上有些更動，我們需要您授權以下的權限，感謝。",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								showPermission();
							}
						});
			}
		}
	}

	private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", okListener).setCancelable(false)
				.create().show();
	}

	@TargetApi(23)
	@SuppressLint("NewApi")
	private void showPermission() {
		// We don't have permission so prompt the user
		List<String> permissions = new ArrayList<String>();
		permissions.add(Manifest.permission.CALL_PHONE);
		permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
		permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		permissions.add(Manifest.permission.CAMERA);
		requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
	}

	@TargetApi(23)
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			// 許可授權
		} else {
			// 沒有權限
			toast(getString(R.string.s_ts_58), this);
			showPermission();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		showPermission();
	}

	private boolean permission(int mp) {
		return mp != PackageManager.PERMISSION_GRANTED;
	}

	protected void onNewIntent(Intent intent) {
		// TODO 覆寫該Intent用於補捉如果有新的Intent進入時，可以觸發的事件任務。
		if (!write && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			NdefMessage[] messages = getNdefMessages(intent);
			String data = Decrypt(new String(messages[0].getRecords()[0].getPayload()));
			String msg = "";
			if (checkData(data)) {
				msg = Decrypt(data);
			} else {
				msg = data;
				toast(getString(R.string.s_ts_57), this);
			}
			if (msg.length() > 0) {
				getNFCString(msg);
			} else {
				toast(getString(R.string.s_ts_0), this);
			}
		}

		// 監測到有指定ACTION進入，代表要寫入資料至Tag中。
		if (write && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			writeTag(getNoteAsNdef(), detectedTag);
		}
	}

	private NdefMessage[] getNdefMessages(Intent intent) {
		// Parse the intent
		NdefMessage[] msgs = null;
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			} else {
				// Unknown tag type
				byte[] empty = new byte[] {};
				NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
				NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
				msgs = new NdefMessage[] { msg };
			}
		} else {
			Log.d(TAG, "Unknown intent.");
			finish();
		}
		return msgs;
	}

	private String getText(EditText ET) {
		return ET.getText().toString();
	}

	private void save() {
		String File = "", Date = "";
		if (user) {
			File = "DATA.txt";
			if (medi_item.size() > 0) {
				Date = medi_item.size() + "=";
				for (int i = 0; i < medi_item.size(); i++) {
					Date += medi_item.get(i);
				}
			}
		} else {
			File = "USER.txt";
			Date = getText(mediCar_unit) + "|" + getText(mediCar_brand) + "|";
			getFile(File, Date);
			File = "MEDI.txt";
			if (medi_item.size() > 0) {
				Date = medi_item.size() + "=";
				for (int i = 0; i < medi_item.size(); i++) {
					Date += medi_item.get(i);
				}
			} else {
				Date = "";
			}
			/*
			 * MODE_PRIVATE：為預設操作模式，代表該檔是私有資料，只能被應用本身訪問，在該模式下，寫入的內容會覆蓋原檔的內容。
			 * MODE_APPEND：模式會檢查檔是否存在，存在就往檔追加內容，否則就創建新檔。
			 * MODE_WORLD_READABLE和Context.
			 * MODE_WORLD_WRITEABLE用來控制其他應用是否有許可權讀寫該檔。
			 * MODE_WORLD_READABLE：表示當前檔可以被其他應用讀取。
			 * MODE_WORLD_WRITEABLE：表示當前檔可以被其他應用寫入。 如果希望檔被其他應用讀和寫，可以傳入：
			 * Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE
			 */
		}
		getFile(File, Date);

		if (dev) {
			File = "DEV.txt";
			Date = 1 + "";
			getFile(File, Date);
		}
	}

	private void getFile(String File, String Date) {
		try {
			FileOutputStream outStream = this.openFileOutput(File, MODE_PRIVATE);
			outStream.write(Date.getBytes());
			outStream.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private String getData(String filename) {
		String DATA = "";
		FileInputStream fin = null;
		ByteArrayOutputStream bos = null;
		try {
			fin = this.openFileInput(filename);
			bos = new ByteArrayOutputStream();
			int size;
			size = fin.available();

			byte[] data = new byte[size];
			fin.read(data);
			bos.write(data);
			DATA = bos.toString();
			bos.close();
			fin.close();
		} catch (IOException e) {
		}
		return DATA;
	}

	private void load_DEV_DATA() {
		String File = "DEV.txt";
		String Date = "";
		Date = getData(File);
		if (Date.length() > 0) {
			if (Integer.parseInt(Date) == 1)
				dev = true;
		}
	}

	private void load_USER_DATA() {
		String File = "USER.txt";
		String Date = "", DateTmp;
		int tmp = 0;
		ArrayList<EditText> datelist = new ArrayList<EditText>();
		datelist.add(mediCar_unit);
		datelist.add(mediCar_brand);
		Date = getData(File);
		if (Date.length() > 0) {
			for (int i = 0; i < 2; i++) {
				DateTmp = Date.substring(tmp).substring(0, Date.substring(tmp).indexOf('|'));
				if (!DateTmp.equals("null")) {
					datelist.get(i).setText(DateTmp);
				}
				tmp = tmp + DateTmp.length() + 1;
			}
		}
		String unit = mediCar_unit.getText().toString() + "";
		String brand = mediCar_brand.getText().toString() + "";
		if (unit.length() > 1 && brand.length() > 1) {
			pager1.setCurrentItem(0);
		} else {
			pager1.setCurrentItem(2);
		}
	}

	private void load_MEDI_DATA() {
		String File;
		if (user) {
			File = "DATA.txt";
		} else {
			File = "MEDI.txt";
		}
		String Date = "", DateTmp;
		int tmp = 0, run = 0;

		Date = getData(File);
		if (Date.length() > 0) {
			run = Integer.parseInt(Date.substring(tmp).substring(0, Date.substring(tmp).indexOf('=')));
			tmp += ((run + "").length() + 1);
			for (int i = 0; i < run; i++) {
				DateTmp = Date.substring(tmp + i).substring(0, Date.substring(tmp + i).indexOf('|'));
				medi_item.add(DateTmp + '|');
				tmp += DateTmp.length();
			}
			user_upload();
		}
	}

	private void findViewById() {
		main_left = (ImageView) findViewById(R.id.main_left);
		main_right = (ImageView) findViewById(R.id.main_right);
		pager1 = (ViewPager) findViewById(R.id.main_pager1);
		main_log = (Button) findViewById(R.id.main_log);
		main_left.getBackground().setAlpha(50);
		main_right.getBackground().setAlpha(50);
		main_log.setOnClickListener(this);
	}

	@SuppressWarnings("unused")
	private String getTime() {
		Calendar calendar = Calendar.getInstance();
		String h = "" + calendar.get(Calendar.HOUR_OF_DAY);
		if (h.length() == 1) {
			h = "0" + h;
		}
		String m = "" + calendar.get(Calendar.MINUTE);
		if (m.length() == 1) {
			m = "0" + m;
		}
		String s = "" + calendar.get(Calendar.SECOND);
		if (s.length() == 1) {
			s = "0" + s;
		}
		return h + ":" + m + ":" + s;
	}

	private void checkToday(int y, int m, int d, boolean check) {
		int cy, cm, cd;
		Calendar calendar = Calendar.getInstance();
		if (check) {
			if (calendar.get(Calendar.YEAR) != y || calendar.get(Calendar.MONTH) != m - 1
					|| calendar.get(Calendar.DATE) != d) {
				main_log.setVisibility(View.VISIBLE);
				main_log.setText(getString(R.string.s_ts_1));
				toast(getString(R.string.s_ts_1), this);
			}
		} else {
			boolean pass = false;
			y = id_Y;
			m = id_M;
			d = id_D;
			cy = calendar.get(Calendar.YEAR);
			cm = calendar.get(Calendar.MONTH);
			cd = calendar.get(Calendar.DATE);
			if (y > cy) {
				pass = true;
			} else {
				if (y == cy) {
					cm++;
					if (m > cm) {
						pass = true;
					} else {
						if (m == cm) {
							if (d > cd) {
								pass = true;
							} else {
								if (d == cd) {
									pass = true;
								}
							}
						}
					}
				}
			}
			if (!dev) {
				if (pass) {
					main_log.setVisibility(View.GONE);
					toast(getString(R.string.s_ts_2) + m + getString(R.string.s_ts_4) + d + getString(R.string.s_ts_5),
							this);
				} else {
					main_log.setVisibility(View.VISIBLE);
					toast(getString(R.string.s_ts_3) + m + getString(R.string.s_ts_4) + d + getString(R.string.s_ts_5),
							this);
					try {
						main_log.setText(
								"軟體版本:" + this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode
										+ "\n" + getString(R.string.s_ts_3) + m + getString(R.string.s_ts_4) + d
										+ getString(R.string.s_ts_5));
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}
			} else {
				main_log.setVisibility(View.GONE);
			}
		}
	}

	@SuppressWarnings("static-access")
	@SuppressLint("InflateParams")
	private void addTab() {
		main_view.clear();
		LayoutInflater mInflater = getLayoutInflater().from(this);
		View v0 = mInflater.inflate(R.layout.main_0_view, null); // START
		View v1 = mInflater.inflate(R.layout.main_1_view, null); // 傷患資訊
		View v2 = mInflater.inflate(R.layout.main_2_view, null); // 歷史紀錄
		View v3 = mInflater.inflate(R.layout.main_3_view, null); // 駕駛模式

		// View v5 = mInflater.inflate(R.layout.main_5_view, null);
		// View v6 = mInflater.inflate(R.layout.main_6_view, null);

		// 初始畫面
		main_view.add(v0);
		main_view.add(v1);
		main_view.add(v2);
		main_view.add(v3);

		// main_view.add(v5);
		// main_view.add(v6);

		// 設置標題
		// main_title_list.add("START檢傷");
		// main_title_list.add("檢傷卡");
		// main_title_list.add("即時車輛");
		// main_title_list.add("救護車設定");

		// 建立配適器
		pager1.setOnPageChangeListener(this);
		getDeviceWidth();
	}

	private void getDeviceWidth() {
		// 9吋 3904 * 3072
		// 5吋 2368 * 1440
		metrics = getResources().getDisplayMetrics();
		int mWidth = (int) (metrics.widthPixels * metrics.density); // 螢幕寬
		textSize = mWidth / 48;
		if (textSize > 60) {
			textSize /= 1.6;
		}
	}

	private void pager_upload() {
		pager1.setAdapter(new MyPagerAdapter_MAIN(main_view));
	}

	// 快速檢傷
	private void view_0() {
		bg_emrc = (ImageView) main_view.get(0).findViewById(R.id.print_background);
		print_No = (TextView) main_view.get(0).findViewById(R.id.main_no_print);
		print_item_0 = (TextView) main_view.get(0).findViewById(R.id.print_tv_0_green);
		print_item_1 = (TextView) main_view.get(0).findViewById(R.id.print_tv_1_black);
		print_item_2 = (TextView) main_view.get(0).findViewById(R.id.print_tv_2_red);
		print_item_3 = (TextView) main_view.get(0).findViewById(R.id.print_tv_3_red);
		print_item_4 = (TextView) main_view.get(0).findViewById(R.id.print_tv_4_red);
		print_item_5 = (TextView) main_view.get(0).findViewById(R.id.print_tv_5_red);
		print_item_6 = (TextView) main_view.get(0).findViewById(R.id.print_tv_6_gold);

		print_item_0.setOnClickListener(this);
		print_item_1.setOnClickListener(this);
		print_item_2.setOnClickListener(this);
		print_item_3.setOnClickListener(this);
		print_item_4.setOnClickListener(this);
		print_item_5.setOnClickListener(this);
		print_item_6.setOnClickListener(this);
		if (!this.getResources().getConfiguration().locale.getCountry().equals("TW")) {
			bg_emrc.setImageResource(R.drawable.bg_emrc);
			// Web = "http://120.119.155.80/en/";
			EN = true;
		}
	}

	// 檢傷卡
	@SuppressLint("InflateParams")
	private void view_1() {
		menu_No = (TextView) main_view.get(1).findViewById(R.id.main_no_menu);
		menu_list = (ListView) main_view.get(1).findViewById(R.id.menulist);
		menu_ok = (ImageView) main_view.get(1).findViewById(R.id.menu_ok);
		menu_re = (ImageView) main_view.get(1).findViewById(R.id.menu_re);
		spinner = (Spinner) main_view.get(1).findViewById(R.id.menu_spinner);
		menu_item.add(getString(R.string.s_Menu_1)); // information
		menu_item.add(getString(R.string.s_Menu_2)); // emergency
		menu_item.add(getString(R.string.s_Menu_3)); // injured
		menu_item.add(getString(R.string.s_Menu_4)); // vital signs
		menu_item.add(getString(R.string.s_Menu_5)); // level
		menu_item.add(getString(R.string.s_Menu_6)); // photo evidence
		Treatment_unit.add(getString(R.string.s_H)); // 0
		Treatment_location.add("");
		Treatment_leve.add(0);
		Treatment_unit.add(getString(R.string.s_H_Tainan_1)); // 1.成大醫院
		Treatment_location.add("23.002291,120.218917");
		Treatment_leve.add(1);
		Treatment_unit.add(getString(R.string.s_H_Tainan_2)); // 2.奇美醫院
		Treatment_location.add("23.021116,120.221472");
		Treatment_leve.add(1);
		Treatment_unit.add(getString(R.string.s_H_Tainan_3)); // 3.柳營奇美
		Treatment_location.add("23.289625,120.325462");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_4)); // 4.佳里奇美
		Treatment_location.add("23.181638,120.183853");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Tainan_5)); // 5.台南市醫
		Treatment_location.add("22.968939,120.226432");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_6)); // 6.衛部臺南
		Treatment_location.add("22.997351,120.209366");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_7)); // 7.台南榮總
		Treatment_location.add("22.997983,120.239994");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Tainan_8)); // 8.台南新樓
		Treatment_location.add("22.989220,120.212705");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_9)); // 9.麻豆新樓
		Treatment_location.add("23.180699,120.232423");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_10)); // 10.郭綜合醫院
		Treatment_location.add("22.994627,120.198932");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Tainan_11)); // 11.安南醫院
		Treatment_location.add("23.064470,120.223766");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Tainan_12)); // 12.衛部新化
		Treatment_location.add("23.063487,120.335736");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Tainan_13)); // 13.衛部新營
		Treatment_location.add("23.308968,120.313599");
		Treatment_leve.add(3);

		Treatment_unit.add(getString(R.string.s_H_Changhua_1)); // 14.秀傳醫院
		Treatment_location.add("24.065078,120.537304");
		Treatment_leve.add(1);
		Treatment_unit.add(getString(R.string.s_H_Changhua_2)); // 15.彰化基督教醫院
		Treatment_location.add("24.0709512,120.5446393");
		Treatment_leve.add(1);
		Treatment_unit.add(getString(R.string.s_H_Changhua_3)); // 16.彰濱秀傳醫院
		Treatment_location.add("24.078987,120.412039");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Changhua_4)); // 17.二林基督教醫院
		Treatment_location.add("23.893694,120.363822");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Changhua_5)); // 18.鹿港基督教醫院
		Treatment_location.add("24.060152,120.438532");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Changhua_6)); // 19.衛福部彰化醫院
		Treatment_location.add("23.949322,120.527273");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Changhua_7)); // 20.卓醫院
		Treatment_location.add("23.871781,120.515663");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Changhua_8)); // 21.仁和醫院
		Treatment_location.add("23.858099,120.587909");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Changhua_9)); // 22.員榮醫院
		Treatment_location.add("23.953454,120.575007");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Changhua_10)); // 23.道周醫院
		Treatment_location.add("24.110637,120.492225");
		Treatment_leve.add(3);
		Treatment_unit.add(getString(R.string.s_H_Changhua_11)); // 24.道安醫院
		Treatment_location.add("23.961575,120.480077");
		Treatment_leve.add(3);

		Treatment_unit.add(getString(R.string.s_H_Yunlin_1)); // 25.臺大醫院雲林分院
		Treatment_location.add("23.697808,120.525822");
		Treatment_leve.add(1);
		Treatment_unit.add(getString(R.string.s_H_Yunlin_2)); // 26.若瑟醫院
		Treatment_location.add("23.708148,120.438055");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Yunlin_3)); // 27.中醫大北港醫院
		Treatment_location.add("23.589272,120.307979");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Yunlin_4)); // 28.雲林基督教醫院
		Treatment_location.add("23.780811,120.440984");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Yunlin_5)); // 29.大醫院斗六分院
		Treatment_location.add("23.702108,120.545211");
		Treatment_leve.add(2);
		Treatment_unit.add(getString(R.string.s_H_Yunlin_6)); // 30.雲林長庚醫院
		Treatment_location.add("23.795332,120.218874");
		Treatment_leve.add(3);

		menu_upload(this);
		menu_list.setOnItemClickListener(this);
		menu_ok.setOnClickListener(this);
		menu_re.setOnClickListener(this);
		spinner.setOnItemSelectedListener(this);
		spinner.setAdapter(new MySpinnerAdapter_MAIN(this, Treatment_unit, Treatment_leve));
	}

	@SuppressLint("InflateParams")
	public static void menu_upload(Context con) {
		menu_view.clear();
		for (int i = 0; i < menu_item.size(); i++) {
			setMyMENUAdapter(i, con);
		}
		menu_list.setAdapter(new MyListAdapter(menu_view));
	}

	@SuppressLint("InflateParams")
	private static void setMyMENUAdapter(int f, Context con) {
		View vi = LayoutInflater.from(con).inflate(R.layout.style_textview, null);
		TextView textview = (TextView) vi.findViewById(R.id.textview);
		String msg = menu_item.get(f);
		textview.setText(msg);
		setViewColor(textview, leve_count);
		menu_view.add(vi);
	}

	@SuppressLint("InflateParams")
	private void setRYGB() {
		String mmsg = lead_item.get(0).toString();
		int resource = R.layout.style_textview4;
		if (EN) {
			resource = R.layout.style_textview6;
		}
		View vi = LayoutInflater.from(this).inflate(resource, null);
		TextView h_red = (TextView) vi.findViewById(R.id.tv_h_red);
		TextView h_yellow = (TextView) vi.findViewById(R.id.tv_h_yellow);
		TextView h_green = (TextView) vi.findViewById(R.id.tv_h_green);
		TextView h_black = (TextView) vi.findViewById(R.id.tv_h_black);
		TextView s_red = (TextView) vi.findViewById(R.id.tv_s_red);
		TextView s_yellow = (TextView) vi.findViewById(R.id.tv_s_yellow);
		TextView s_green = (TextView) vi.findViewById(R.id.tv_s_green);
		TextView s_black = (TextView) vi.findViewById(R.id.tv_s_black);
		// 17030600001/1/五身份證/5/ 其他處置 (保暖).|║0=n/║0=n/║n/║2/32/10/A1267767/
		int r = 0;
		String hr = getLine(mmsg, r, '/');
		r += hr.length() + 1;
		String hy = getLine(mmsg, r, '/');
		r += hy.length() + 1;
		String hg = getLine(mmsg, r, '/');
		r += hg.length() + 1;
		String hb = getLine(mmsg, r, '/');
		r += hb.length() + 1;
		String sr = getLine(mmsg, r, '/');
		r += sr.length() + 1;
		String sy = getLine(mmsg, r, '/');
		r += sy.length() + 1;
		String sg = getLine(mmsg, r, '/');
		r += sg.length() + 1;
		String sb = getLine(mmsg, r, '/');

		h_red.setText(hr);
		h_yellow.setText(hy);
		h_green.setText(hg);
		h_black.setText(hb);
		s_red.setText(sr);
		s_yellow.setText(sy);
		s_green.setText(sg);
		s_black.setText(sb);
		lead_view.add(vi);
	}

	@SuppressLint("InflateParams")
	private void setMyAdapter(int f, boolean db, ArrayList<String> ls, ArrayList<View> lv) {
		// db 17030600001/1/五身份證/5/ 其他處置 (保暖).|║0=n/║0=n/║n/║2/32/10/A1267767/
		// !db 17030600001/1/五身份證/5/ 其他處置 (保暖).║0=n/║0=n/║n/║2/32/10/A1267767/
		final int i = f;
		View vi = LayoutInflater.from(this).inflate(R.layout.style_textview3, null);
		RelativeLayout t_bg = (RelativeLayout) vi.findViewById(R.id.t3_relativeLayout);
		TextView t_name = (TextView) vi.findViewById(R.id.t3_name);
		TextView t_hosp = (TextView) vi.findViewById(R.id.t3_hospital);
		TextView t_other = (TextView) vi.findViewById(R.id.t3_other);
		String mmsg = ls.get(i);
		int r = 0;
		String number = getLine(mmsg, r, '/');
		r += number.length() + 3;
		String name = getLine(mmsg, r, '/');
		r += name.length() + 1;
		String age = getLine(mmsg, r, '/');
		r += age.length() + 1;
		String other = getLine(mmsg, r, '║');
		r += other.length() + 2;
		String inju_front = getLine(mmsg, r, '║');
		r += inju_front.length() + 1;
		String inju_back = getLine(mmsg, r, '║');
		r += inju_back.length() + 1;
		String vita_date = getLine(mmsg, r, '║');
		r += vita_date.length() + 1;
		int leve_count = Integer.parseInt(getLine(mmsg, r, '/'));
		r += 2;
		int emrc_count = Integer.parseInt(getLine(mmsg, r, '/'));
		r += (emrc_count + "").length() + 1;
		String hosp_count = getLine(mmsg, r, '/');
		String hospp;
		if (Integer.parseInt(hosp_count) != 0) {
			hospp = Treatment_unit.get(Integer.parseInt(hosp_count));
		} else {
			hospp = getString(R.string.s_List_38);
		}
		if (name.length() == 0) {
			t_name.setText(number);
		} else {
			t_name.setText(name);
		}
		t_hosp.setText(hospp);
		if (db) {
			// 去除標號
			other = getLine(other, 0, '|');
		}
		other += getEMRC(MainActivity.this, emrc_count, leve_count);
		t_other.setText(other);
		switch (leve_count + "") {
		case "0":
			t_name.setTextColor(Color.WHITE);
			t_hosp.setTextColor(Color.WHITE);
			t_other.setTextColor(Color.WHITE);
			t_bg.setBackgroundColor(Color.BLACK);
			break;
		case "1":
			t_name.setTextColor(Color.WHITE);
			t_hosp.setTextColor(Color.WHITE);
			t_other.setTextColor(Color.WHITE);
			t_bg.setBackgroundColor(Color.RED);
			break;
		case "2":
			t_bg.setBackgroundColor(Color.YELLOW);
			break;
		case "3":
			t_bg.setBackgroundColor(Color.GREEN);
			break;
		default:
			t_name.setTextColor(Color.BLACK);
			t_hosp.setTextColor(Color.BLACK);
			t_other.setTextColor(Color.BLACK);
			t_bg.setBackgroundColor(Color.WHITE);
			break;
		}
		lv.add(vi);
	}

	// 傷患紀錄與清單
	private void view_2() {
		medi_unit = (TextView) main_view.get(2).findViewById(R.id.medi_unit);
		medi_qunt = (TextView) main_view.get(2).findViewById(R.id.medi_qunt);
		medi_list = (ListView) main_view.get(2).findViewById(R.id.medi_list);
		medi_swap = (ImageView) main_view.get(2).findViewById(R.id.medi_swap);
		medi_clear = (ImageView) main_view.get(2).findViewById(R.id.medi_clear);
		medi_unit.setText(getString(R.string.s_List_1));

		load_MEDI_DATA(); // load medi_item
		medi_clear.setOnClickListener(this);
		medi_clear.setVisibility(View.INVISIBLE);
		medi_list.setOnItemClickListener(this);
		medi_list.setOnItemLongClickListener(this);
		medi_swap.setOnClickListener(this);
		medi_swap.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.fs_ambulance));
		user_upload();
	}

	private void lead_upload() {
		try {
			lead_view.clear();
			setRYGB();
			for (int i = 1; i < lead_item.size(); i++) {
				setMyAdapter(i, true, lead_item, lead_view);
			}
			medi_list.setAdapter(new MyListAdapter(lead_view));
			medi_qunt.setText(lead_item.size() - 1 + getString(R.string.s_List_5));
		} catch (Exception e) {
			Log.e("SSL.ERROR", e.toString());
		}
	}

	private void user_upload() {
		dblist = false;
		try {
			medi_view.clear();
			for (int i = 0; i < medi_item.size(); i++) {
				setMyAdapter(i, false, medi_item, medi_view);
			}
			medi_list.setAdapter(new MyListAdapter(medi_view));
			medi_qunt.setText(medi_item.size() + getString(R.string.s_List_5));
		} catch (Exception e) {
			Log.e("SSL.ERROR", e.toString());
		}
	}

	// 救護車
	private void view_3() {
		TextView mediCar_un = (TextView) main_view.get(3).findViewById(R.id.mediCar_un);
		TextView mediCar_lp = (TextView) main_view.get(3).findViewById(R.id.mediCar_lp);
		mediCar_static = (TextView) main_view.get(3).findViewById(R.id.mediCar_static);
		mediCar_unit = (EditText) main_view.get(3).findViewById(R.id.mediCar_unit);
		mediCar_brand = (EditText) main_view.get(3).findViewById(R.id.mediCar_license);
		mediCar_btn0 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn0); // 出勤
		mediCar_btn1 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn1); // 到場
		mediCar_btn2 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn2); // 離場
		mediCar_btn3 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn3); // 到院
		mediCar_btn4 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn4); // 離院
		mediCar_btn5 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn5); // 返回
		mediCar_btn6 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn6); // 取消
		mediCar_btn7 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn7); // 拒絕
		mediCar_btn8 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn8); // 導航
		mediCar_btn9 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn9); // 錄影
		mediCar_btn10 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn10); // 110
		mediCar_btn11 = (Button) main_view.get(3).findViewById(R.id.mediCar_btn11); // 119
		mediCar_static.setText(getString(R.string.s_Menu_20));
		mediCar_btn0.setOnClickListener(this); // 出勤通知
		mediCar_btn1.setOnClickListener(this); // 到達現場
		mediCar_btn2.setOnClickListener(this); // 離開現場
		mediCar_btn3.setOnClickListener(this); // 送達醫院
		mediCar_btn4.setOnClickListener(this); // 離開醫院
		mediCar_btn5.setOnClickListener(this); // 返隊待命
		mediCar_btn6.setOnClickListener(this); // 導航到場
		mediCar_btn7.setOnClickListener(this); // 導航到院
		mediCar_btn8.setOnClickListener(this); // 撥號
		mediCar_btn9.setOnClickListener(this);
		mediCar_btn10.setOnClickListener(this);
		mediCar_btn11.setOnClickListener(this);
		if (EN) {
			mediCar_un.setTextSize(25);
			mediCar_lp.setTextSize(25);
			mediCar_unit.setTextSize(25);
			mediCar_brand.setTextSize(25);
			mediCar_btn0.setTextSize(20);
			mediCar_btn1.setTextSize(20);
			mediCar_btn2.setTextSize(20);
			mediCar_btn3.setTextSize(20);
			mediCar_btn4.setTextSize(20);
			mediCar_btn5.setTextSize(20);
			mediCar_btn6.setTextSize(20);
			mediCar_btn7.setTextSize(20);
			mediCar_btn8.setTextSize(20);
			mediCar_btn9.setTextSize(20);
			mediCar_btn10.setTextSize(20);
			mediCar_btn11.setTextSize(20);
		}
		setTextWatcher();
	}

	public void onPageScrollStateChanged(int arg0) {

	}

	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	public void onPageSelected(int arg0) {
		if (nfc) {
			if (user) {
				switch (arg0) {
				case 0:
					main_left.setVisibility(View.GONE);
					main_right.setVisibility(View.VISIBLE);
					break;
				case 1:
					main_left.setVisibility(View.VISIBLE);
					main_right.setVisibility(View.VISIBLE);
					break;
				case 2:
					main_left.setVisibility(View.VISIBLE);
					main_right.setVisibility(View.GONE);
					break;
				default:
					break;
				}
			} else {
				switch (arg0) {
				case 0:
					main_left.setVisibility(View.GONE);
					main_right.setVisibility(View.VISIBLE);
					break;
				case 1:
					main_left.setVisibility(View.VISIBLE);
					main_right.setVisibility(View.VISIBLE);
					break;
				case 2:
					main_left.setVisibility(View.GONE);
					main_right.setVisibility(View.GONE);
					break;
				default:
					break;
				}
			}
		} else {
			main_left.setVisibility(View.GONE);
			main_right.setVisibility(View.GONE);
		}
	}

	private void setTextWatcher() {
		// 建立文字監聽
		textWatcher = new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String num = "";
				if (leve_count != 5) {
					num += number;
				}
				String unit = mediCar_unit.getText().toString() + "";
				String brand = mediCar_brand.getText().toString() + "";
				if (link && unit.length() > 0 && brand.length() > 0) {
					if (!car) {
						car = true;
						RSMSHandler.obtainMessage().sendToTarget(); // 開啟自動更新
						toast(getString(R.string.s_ts_6), MainActivity.this);
					}
					out("CAR/" + unit + "/" + brand + "/" + status_count + "/" + num + "|");
					car_brand = brand;
					save();
				} else {
					if (car) {
						car_brand = "";
						out("CAR///8/|");
					}
					if (gps) {
						gps = false;
					}
					status_count = 8;
					mediCar_static.setText(getString(R.string.s_Menu_20));
					car = false;
				}
			}
		};
		mediCar_unit.addTextChangedListener(textWatcher);
		mediCar_brand.addTextChangedListener(textWatcher);
	}

	private void setTARGET() {
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		inju_target.add(0.49);// 頭.0
		inju_target.add(0.2);
		inju_target.add(0.49);// 胸部.2
		inju_target.add(0.33);
		inju_target.add(0.49);// 腹部.4
		inju_target.add(0.4245);
		inju_target.add(0.575);// 右大腿.8
		inju_target.add(0.55);
		inju_target.add(0.4);// 左大腿.6
		inju_target.add(0.55);
		inju_target.add(0.6);// 右小腿.12
		inju_target.add(0.7);
		inju_target.add(0.375);// 左小腿.10
		inju_target.add(0.7);
		inju_target.add(0.68);// 右上臂.16
		inju_target.add(0.33);
		inju_target.add(0.31);// 左上臂.14
		inju_target.add(0.33);
		inju_target.add(0.76);// 右前臂.20
		inju_target.add(0.38);
		inju_target.add(0.22);// 左前臂.18
		inju_target.add(0.38);
		inju_target.add(0.89);// 右手.24
		inju_target.add(0.44);
		inju_target.add(0.1);// 左手.22
		inju_target.add(0.43);
	}

	private void setNFC() {
		// 取得該設備預設的無線感應裝置
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			toast(getString(R.string.s_ts_9), this);
			Loding(true);
		} else {
			nfc = true;
			// 註冊讓該Activity負責處理所有接收到的NFC Intents。
			gNfcPendingIntent = PendingIntent.getActivity(this, 0,
					// 指定該Activity為應用程式中的最上層Activity
					new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			// 建立要處理的Intent Filter負責處理來自Tag或p2p交換的資料。
			IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
			try {
				ndefDetected.addDataType("text/plain");
			} catch (MalformedMimeTypeException e) {
			}
			gNdefExchangeFilters = new IntentFilter[] { ndefDetected };
		}
	}

	private void addNFC() {
		if (link) {
			out("NFC/" + setNFCString());
			toast(getString(R.string.s_ts_10), this);
		}
		if (nfc) {
			// 先停止接收任何的Intent，準備寫入資料至tag；
			disableNdefExchangeMode();
			// 啟動寫入Tag模式，監測是否有Tag進入
			enableTagWriteMode();
			// Create LinearLayout Dynamically
			LinearLayout layout = new LinearLayout(this);
			// Setup Layout Attributes
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			layout.setLayoutParams(params);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(new mTextview(this, getString(R.string.s_ts_40)));
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(layout).setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					// 在取消模式下，先關閉監偵有Tag準備寫入的模式，再啟動等待資料交換的模式。
					// 停止寫入Tag模式，代表已有Tag進入
					disableTagWriteMode();
					// 啟動資料交換
					enableNdefExchangeMode();
				}
			});
			// 顯示對話框，告知將Tag或手機靠近本機的NFC感應區
			alertDialog = builder.create();
			alertDialog.show();
		}
		if (car) {
			updateCar();
		}
	}

	private class mTextview extends TextView {
		public mTextview(Context context, final String str) {
			super(context);
			this.setText(str);
			this.setTextSize(35);
			this.setTextColor(Color.BLACK);
			this.setGravity(Gravity.CENTER);
			this.setPadding(20, 20, 20, 20);
		}
	}

	private void checkItem(String msg) {
		String nb = getLine(msg, 0, '/');
		boolean run = false;
		// 有流水號比對方法
		if (nb.length() != 0) {
			for (int i = 0; i < medi_item.size(); i++) {
				String mmsg = medi_item.get(i);
				// 流水號
				if (nb.equals(getLine(mmsg, 0, '/'))) {
					// 流水號相同取代資料
					medi_item.remove(i);
					medi_item.add(msg);
					run = true;
					break;
				}
			}
			if (!run) {
				medi_item.add(msg);
			}
		} else {
			// 沒有流水號比對方法
			medi_item.add(msg);
		}
	}

	private void updateCar() {
		String num = "";
		if (leve_count != 5) {
			num += number;
		}
		String unit = mediCar_unit.getText().toString() + "";
		String brand = mediCar_brand.getText().toString() + "";
		out("CAR/" + unit + "/" + brand + "/" + status_count + "/" + num + "/|");
	}

	private String getPercent(Integer num, Integer totalPeople) {
		String ms = "";
		if (totalPeople != 0) {
			ms = "" + (num + 0.0) / (totalPeople + 0.0);
		} else {
			return "n/";
		}
		ms = ms.substring(0, 4);
		return ms;
		// NumberFormat nf = NumberFormat.getPercentInstance();
		// nf.setMinimumFractionDigits(2);// 控制保留小数点后几位，2：表示保留2位小数点
		// percent = nf.format(p3);
		// percent = percent.substring(0, percent.length() - 1);
	}

	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 開始位置
			downX = (int) event.getX();
			downY = (int) event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			// 移動位置
			break;
		case MotionEvent.ACTION_UP:
			// 離開位置
			metrics = getResources().getDisplayMetrics();
			int mWidth = metrics.widthPixels; // 螢幕寬
			int mHeight = metrics.heightPixels; // 螢幕長
			// Toast.makeText(this, "W:" + mWidth + " H:" + mHeight,
			// Toast.LENGTH_SHORT).show();
			int upX = (int) event.getX();
			int upY = (int) event.getY();
			// 點擊事件
			if (Math.abs(upX - downX) < 10 && Math.abs(upY - downY) < 10) {
				Toast.makeText(this, "X:" + getPercent(upX, mWidth) + " Y:" + getPercent(upY, mHeight),
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return true;
	}

	public void onClick(View v) {
		if (v == main_log) {
			dev_count--;
			if (dev_count == 10)
				toast("您只需完成剩餘的三個步驟，即可成為開發人員。", this);
			if (dev_count == 5)
				toast("您只需完成剩餘的二個步驟，即可成為開發人員。", this);
			if (dev_count == 1)
				toast("您只需完成剩餘的一個步驟，即可成為開發人員。", this);
			if (dev_count == 0) {
				toast("您現在已成為開發人員。", this);
				main_log.setVisibility(View.GONE);
				dev = true;
			}
		}
		if (v == medi_swap) {
			if (link) {
				if (!dblist) {
					// Patients List
					medi_swap.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.fs_database));
					medi_unit.setText(getString(R.string.s_List_2));
					out("c2");
					if (lead_item.size() != 0) {
						lead_upload();
					} else {
						medi_qunt.setText("");
					}
					dblist = true;
				} else {
					// Transport Record
					medi_swap
							.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.fs_ambulance));
					medi_unit.setText(getString(R.string.s_List_1));
					user_upload();
				}
			} else {
				toast(getString(R.string.s_ts_50), this);
			}
		}

		if (v == menu_ok) {
			// 保存NFC至檢傷紀錄
			// toast(setNFCString(), this);
			checkItem(setNFCString());
			user_upload();
			addNFC();
		}
		if (v == menu_re) {
			user_upload();
			getReset(true);
			toast(getResources().getString(R.string.s_ts_60), this);
		}
		if (v == mediCar_btn0) { // 出勤
			selectCar(0, mediCar_btn0);
		}
		if (v == mediCar_btn1) { // 到場
			selectCar(1, mediCar_btn1);
		}
		if (v == mediCar_btn2) { // 離場
			selectCar(2, mediCar_btn2);
		}
		if (v == mediCar_btn3) { // 到院
			selectCar(3, mediCar_btn3);
		}
		if (v == mediCar_btn4) { // 離院
			selectCar(4, mediCar_btn4);
		}
		if (v == mediCar_btn5) { // 待命
			selectCar(5, mediCar_btn5);
		}
		if (v == mediCar_btn6) { // 取消
			selectCar(6, mediCar_btn6);
			new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.s_ts_29))
					.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {

						}
					}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							user_upload();
							getReset(true);
							pager1.setCurrentItem(0);
						}
					}).show();
		}
		if (v == mediCar_btn7) { // 拒絕
			selectCar(7, mediCar_btn7);
			new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.s_ts_29))
					.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {

						}
					}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							user_upload();
							getReset(true);
							pager1.setCurrentItem(0);
						}
					}).show();
		}
		if (v == mediCar_btn8) { // 導航
			if (hosp_count != 0) {
				LocationActivity(1); // 導航到院
			} else {
				if (leve_count != 5) {
					// 有檢傷但未選擇醫院時
					new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.s_ts_56))
							.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface arg0, int arg1) {

								}
							}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									HSHandler.obtainMessage().sendToTarget();
								}
							}).show();
				} else {
					LocationActivity(0); // 開啟導航
				}
			}
		}
		if (v == mediCar_btn9) { // 錄影
			startActivity(new Intent(android.provider.MediaStore.INTENT_ACTION_VIDEO_CAMERA));
		}
		if (v == mediCar_btn10) { // 119
			startActivity(new Intent().setAction("android.intent.action.CALL").setData(Uri.parse("tel:119")));
		}
		if (v == mediCar_btn11) { // 110
			startActivity(new Intent().setAction("android.intent.action.CALL").setData(Uri.parse("tel:110")));
		}
	}

	private void selectCar(int i, Button btn) {
		// 駕駛是否登入
		if (car) {
			status_count = i;
			mediCar_static.setText(btn.getText().toString());
			// 是否出勤,到場,離場,到院,離院
			if (i < 5) {
				// 是否未開啟GPS上傳功能
				if (!gps) {
					// 檢傷等級不可為空白
					if (leve_count != 5) {
						UIHandler.obtainMessage().sendToTarget();
					}
				}
			} else {
				// 待命,拒絕,取消
				gps = false;
			}
			updateCar();
		} else {
			toast(getString(R.string.s_ts_15), this);
		}
	}

	// private void defauleBG() {
	// mediCar_btn0.setBackgroundResource(R.drawable.item_background);
	// mediCar_btn1.setBackgroundResource(R.drawable.item_background);
	// mediCar_btn2.setBackgroundResource(R.drawable.item_background);
	// mediCar_btn3.setBackgroundResource(R.drawable.item_background);
	// mediCar_btn4.setBackgroundResource(R.drawable.item_background);
	// mediCar_btn5.setBackgroundResource(R.drawable.item_background);
	// }

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		// parent = 事件發生的母體 spinner_items
		// position = 被選擇的項目index = parent.getSelectedItemPosition()
		// id = row id，通常給資料庫使用
		if (position != 0) {
			String Treatment = parent.getSelectedItem().toString();
			if (!user) {
				Toast.makeText(this, getString(R.string.s_ts_18) + Treatment, Toast.LENGTH_SHORT).show();
			}
		}
		hosp_count = position;
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		Toast.makeText(this, getString(R.string.s_ts_19), Toast.LENGTH_LONG).show();
	}

	// private void setEMRC(int c) {
	// leve_count = c;
	// // 傷患等極大於輕傷
	// if (c != 3) {
	// startActivity(new Intent(MainActivity.this, EMRCActivity.class));
	// }
	// }

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent == menu_list) {
			start(position);
		}
		if (parent == medi_list) {
			String pms = "";
			if (dblist) {
				if (position != 0)
					pms = lead_item.get(position);
			} else {
				pms = medi_item.get(position);
			}
			if (pms.length() != 0) {
				final String mmsg = pms;
				// 是否要取得傷患資訊(DB)
				new AlertDialog.Builder(this).setTitle(getString(R.string.s_ts_20))
						.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
							}
						}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								getNFCString(mmsg);
								// checkItem(setNFCString());
							}
						}).show();
			}
		}
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		if (!dblist) {
			new AlertDialog.Builder(this).setTitle(getString(R.string.s_ts_11))
					.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
						}
					}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							medi_item.remove(position);
							user_upload();
							if (user) {
								if (medi_item.size() == 0) {
									getFile("DATA.txt", "");
								}
							}
						}
					}).show();
		}
		return true;
	}

	private void disableNdefExchangeMode() {
		nfcAdapter.disableForegroundNdefPush(this);
		nfcAdapter.disableForegroundDispatch(this);
	}

	private void enableTagWriteMode() {
		write = true;
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		gWriteTagFilters = new IntentFilter[] { tagDetected };
		nfcAdapter.enableForegroundDispatch(this, gNfcPendingIntent, gWriteTagFilters, null);
	}

	private void disableTagWriteMode() {
		write = false;
		nfcAdapter.disableForegroundDispatch(this);
	}

	private void enableNdefExchangeMode() {
		// 讓NfcAdatper啟動前景Push資料至Tag或應用程式。
		nfcAdapter.enableForegroundNdefPush(this, getNoteAsNdef());

		// 讓NfcAdapter啟動能夠在前景模式下進行intent filter的dispatch。
		nfcAdapter.enableForegroundDispatch(this, gNfcPendingIntent, gNdefExchangeFilters, null);
	}

	private NdefMessage getNoteAsNdef() {
		// 啟動Ndef交換資料模式。
		byte[] textBytes = Encrypt(setNFCString()).getBytes();
		NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(), new byte[] {},
				textBytes);
		return new NdefMessage(new NdefRecord[] { textRecord });
	}

	private void getNFCString(String tag) {
		if (tag.length() > 0 || tag.indexOf('|') != -1) {
			getReset(false);
			int tmp = 0;
			String msg = tag;
			String numbers = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));
			tmp += numbers.length() + 1;
			number = numbers;
			int genders = Integer.parseInt(msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/')));
			tmp += 2;
			gender = genders;
			String info = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));
			tmp += info.length() + 1;
			Tools.getListString(info, info_data, 3);
			String inju_f = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));
			tmp += inju_f.length() + 1;
			Tools.getListString(inju_f, inju_front, 0);
			String inju_b = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));
			tmp += inju_b.length() + 1;
			Tools.getListString(inju_b, inju_back, 0);
			String vita = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));
			tmp += vita.length() + 1;
			Tools.getListString(vita, vita_data, 18);
			int leve = Integer.parseInt(msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/')));
			tmp += 2;
			leve_count = leve;
			int emrc = Integer.parseInt(msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/')));
			tmp += (emrc + "").length() + 1;
			emrc_count = emrc;
			int hosp = Integer.parseInt(msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/')));
			tmp += (hosp + "").length() + 1;
			hosp_count = hosp;
			String iden = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));
			identity = iden;
			uploadview(MainActivity.this);
			if (link) {
				if (numbers.length() < 2) {
					// 無流水號
					out("a0");
					// 上傳資料
					UPHandler.obtainMessage().sendToTarget();
				} else {
					// 搜尋照片
					out(number);
				}
			}
			if (user) {
				pager1.setCurrentItem(1);
			} else {
				pager1.setCurrentItem(0);
				if (link) {
					out("c0");
					if (hosp_count == 0) {
						out("c3");
					}
				}
			}
		} else {
			toast(getString(R.string.s_ts_0), this);
		}
		// user_name = null;
	}

	public static void uploadview(Context con) {
		if (number.length() > 2) {
			print_No.setText(number + "");
			menu_No.setText(number + "");
		} else {
			print_No.setText("Not obtained.");
			menu_No.setText("Not obtained.");
		}
		if (info_data.size() > 2) {
			String name = info_data.get(0);
			if (name.length() == 0) {
				name = con.getResources().getString(R.string.info_unknow);
			}
			switch (gender) {
			case 0:
				name += "," + con.getResources().getString(R.string.info_femal);
				break;
			case 1:
				name += "," + con.getResources().getString(R.string.info_male);
				break;
			default:
				break;
			}
			menu_item.set(0, con.getResources().getString(R.string.s_Menu_1) + "\n<" + name + ">");
		} else {
			menu_item.set(0, con.getResources().getString(R.string.s_Menu_1));
		}
		if (inju_front.size() > 0 || inju_back.size() > 0) {
			String title = "";
			if (inju_front.size() > 0) {
				for (int i = 0; i < inju_front.size(); i += 2) {
					Double x = inju_front.get(i), y = inju_front.get(i + 1);
					title += Tools.getInjury(con, x, y, 0);
				}
			}
			if (inju_back.size() > 0) {
				for (int i = 0; i < inju_back.size(); i += 2) {
					Double x = inju_back.get(i), y = inju_back.get(i + 1);
					title += Tools.getInjury(con, x, y, 1);
				}
			}
			if (title.length() > 1) {
				menu_item.set(2, con.getResources().getString(R.string.s_Menu_3) + "\n<" + title + ">");
			} else {
				menu_item.set(2, con.getResources().getString(R.string.s_Menu_3));
			}
		} else {
			menu_item.set(2, con.getResources().getString(R.string.s_Menu_3));
		}
		if (vita_data.size() > 0) {
			for (int i = 0; i < 3; i++) {
				String j = vita_data.get(i * 6);
				if (j.length() > 1) {
					menu_item.set(3, con.getResources().getString(R.string.s_Menu_4) + "\n<" + j + ">");
				}
			}
		} else {
			menu_item.set(3, con.getResources().getString(R.string.s_Menu_4));
		}
		if (leve_count == 5) {
			// String intArray[] = { "死亡", "極危險", "危險", "輕傷" };
			// menu_item.set(4, "檢傷分類 <" + intArray[leve_count] + ">");
			// } else {
		}
		if (phot_count > 0) {
			int cc = phot_count, set = 0;
			while (cc != 0) {
				if (cc - 32 >= 0) {
					cc -= 32;
					set++;
				} else if (cc - 16 >= 0) {
					cc -= 16;
					set++;
				} else if (cc - 8 >= 0) {
					cc -= 8;
					set++;
				} else if (cc - 4 >= 0) {
					cc -= 4;
					set++;
				} else if (cc - 2 >= 0) {
					cc -= 2;
					set++;
				} else {
					cc--;
					set++;
				}
			}
			menu_item.set(5,
					con.getResources().getString(R.string.s_Menu_6) + "\n<"
							+ con.getResources().getString(R.string.s_List_3) + set
							+ con.getResources().getString(R.string.s_List_4) + ">");
		} else {
			menu_item.set(5, con.getResources().getString(R.string.s_Menu_6));
		}
		if (emrc_count != 0) {
			if (menu_item.size() == 6) {
				menu_item.add(getEMRC(con, emrc_count, leve_count));
			} else {
				menu_item.set(6, getEMRC(con, emrc_count, leve_count));
			}
		} else {
			if (menu_item.size() == 7) {
				menu_item.remove(6);
			}
		}
		spinner.setSelection(hosp_count, false);

		menu_upload(con);
		if (user) {
			pager1.setCurrentItem(1);
		} else {
			pager1.setCurrentItem(0);
		}
		// if (c < 6) {
		// menu_list.setSelection(c);
		// }
	}

	private static String getEMRC(Context c, int count, int leve) {
		String msg = "";
		ArrayList<String> list_string = new ArrayList<String>();
		switch (leve) {
		case 0:
			list_string.add(c.getResources().getString(R.string.s_EMRC_00));
			list_string.add(c.getResources().getString(R.string.s_EMRC_01));
			list_string.add(c.getResources().getString(R.string.s_EMRC_02));
			list_string.add(c.getResources().getString(R.string.s_EMRC_03));
			list_string.add(c.getResources().getString(R.string.s_EMRC_04));
			break;
		case 1:
			list_string.add(c.getResources().getString(R.string.s_EMRC_10));
			list_string.add(c.getResources().getString(R.string.s_EMRC_11));
			list_string.add(c.getResources().getString(R.string.s_EMRC_12));
			list_string.add(c.getResources().getString(R.string.s_EMRC_13));
			list_string.add(c.getResources().getString(R.string.s_EMRC_14));
			list_string.add(c.getResources().getString(R.string.s_EMRC_15));
			list_string.add(c.getResources().getString(R.string.s_EMRC_16));
			list_string.add(c.getResources().getString(R.string.s_EMRC_17));
			list_string.add(c.getResources().getString(R.string.s_EMRC_18));
			list_string.add(c.getResources().getString(R.string.s_EMRC_19));
			break;
		case 2:
			list_string.add(c.getResources().getString(R.string.s_EMRC_20));
			list_string.add(c.getResources().getString(R.string.s_EMRC_21));
			list_string.add(c.getResources().getString(R.string.s_EMRC_22));
			list_string.add(c.getResources().getString(R.string.s_EMRC_23));
			list_string.add(c.getResources().getString(R.string.s_EMRC_24));
			list_string.add(c.getResources().getString(R.string.s_EMRC_25));
			list_string.add(c.getResources().getString(R.string.s_EMRC_26));
			break;
		}
		int getcheck = count;
		for (int i = 9; i >= 0; i--) {
			if (getcheck >= (1 << i)) {
				getcheck -= (1 << i);
				if (msg.length() < 1) {
					msg += (list_string.get(i));
				} else {
					msg += ("\n" + list_string.get(i));
				}
			}
		}
		return msg;
	}

	private static void setViewColor(TextView view, int count) {
		switch (count) {
		case 0:
			view.setTextColor(Color.WHITE);
			menu_list.setBackgroundColor(Color.BLACK);
			break;
		case 1:
			view.setTextColor(Color.WHITE);
			menu_list.setBackgroundColor(Color.RED);
			break;
		case 2:
			view.setTextColor(Color.BLACK);
			menu_list.setBackgroundColor(Color.YELLOW);
			break;
		case 3:
			view.setTextColor(Color.BLACK);
			menu_list.setBackgroundColor(Color.GREEN);
			break;
		default:
			view.setTextColor(Color.BLACK);
			menu_list.setBackgroundColor(Color.WHITE);
			break;
		}
	}

	private void start(int item) {
		switch (item) {
		case 0:
			startActivity(new Intent(this, InformationActivity.class));
			break;
		case 1:
			startActivity(new Intent(this, EmrgnActivity.class));
			break;
		case 2:
			startActivity(new Intent(this, InjuredActivity.class));
			break;
		case 3:
			startActivity(new Intent(this, VitalActivity.class));
			break;
		case 4:
			startActivity(new Intent(this, LevelActivity.class));
			break;
		case 5:
			startActivity(new Intent(this, PhotoActivity.class));
			break;
		case 6:
			startActivity(new Intent(this, EMRCActivity.class));
			break;
		}
	}

	private void getReset(boolean a0) {
		number = "";
		gender = 2;
		identity = "";
		leve_count = 5;
		phot_count = 0;
		emrc_count = 0;
		hosp_count = 0;
		info_photo = null;
		info_data.clear();
		inju_front.clear();
		inju_back.clear();
		vita_data.clear();
		phot_bitmap.clear();
		if (a0) {
			menu_item.clear();
			menu_item.add(getString(R.string.s_Menu_1)); // information
			menu_item.add(getString(R.string.s_Menu_2)); // emergency
			menu_item.add(getString(R.string.s_Menu_3)); // injured
			menu_item.add(getString(R.string.s_Menu_4)); // vital signs
			menu_item.add(getString(R.string.s_Menu_5)); // level
			menu_item.add(getString(R.string.s_Menu_6)); // photo
															// evidence
			menu_upload(this);
			spinner.setSelection(hosp_count, true);
			if (net && link) {
				out("a0");
			} else {
				print_No.setText(getString(R.string.id_000004));
				menu_No.setText(getString(R.string.id_000004));
			}
		}
	}

	@SuppressLint("TrulyRandom")
	private String Encrypt(String sSrc) {
		String s = "";
		try {
			byte[] raw = "AIzaSyCpvaLBjUPz".getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");// "算法/模式/补码方式"
			IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			s = Base64.encodeToString(cipher.doFinal(sSrc.getBytes()), android.util.Base64.NO_WRAP);
		} catch (Exception e) {
		}
		return s;
	}

	private boolean checkData(String sSrc) {
		if (sSrc.indexOf('|') != -1) {
			// Normal data
			return false;
		} else {
			// Encrypted data
			return true;
		}
	}

	private String Decrypt(String sSrc) {
		String s = "";
		try {
			byte[] raw = "AIzaSyCpvaLBjUPz".getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			s = new String(cipher.doFinal(Base64.decode(sSrc, android.util.Base64.NO_WRAP)));
		} catch (Exception e) {
		}
		return s;
	}

	private String setNFCString() {
		return number + '/' + gender + '/' + Tools.getList(info_data, true) + '║' + inju_front.size() + '='
				+ Tools.getList(inju_front, false) + '║' + inju_back.size() + '=' + Tools.getList(inju_back, false)
				+ '║' + Tools.getList(vita_data, false) + '║' + leve_count + '/' + emrc_count + '/' + hosp_count + '/'
				+ identity + "/|";
	}

	private boolean writeTag(NdefMessage message, Tag tag) {

		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();

				if (!ndef.isWritable()) {
					// 標籤是只讀的
					toast(getString(R.string.s_ts_21), this);
					nfcClose(false);
					return false;
				}
				if (ndef.getMaxSize() < size) {
					// 標籤容量ndef.getMaxSize() bytes,訊息是size bytes
					toast(getString(R.string.s_ts_22), this);
					toast(getString(R.string.s_ts_23) + ndef.getMaxSize() + " bytes, " + getString(R.string.s_ts_24)
							+ size + " bytes.", this);
					nfcClose(false);
					return false;
				}

				ndef.writeNdefMessage(message);
				// 將消息寫入預格式化的標籤
				toast(getString(R.string.s_ts_25), this);
				nfcClose(true);
				return true;
			} else {
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						// 格式化標籤和寫入資料
						toast(getString(R.string.s_ts_25), this);
						nfcClose(true);
						return true;
					} catch (IOException e) {
						// 無法格式化標籤
						toast(getString(R.string.s_ts_26), this);
						nfcClose(false);
						return false;
					}
				} else {
					// 標籤不支持NDEF
					toast(getString(R.string.s_ts_27), this);
					nfcClose(false);
					return false;
				}
			}
		} catch (Exception e) {
			// 無法寫入標籤
			toast(getString(R.string.s_ts_26), this);
			nfcClose(false);
			return false;
		}
	}

	private void nfcClose(boolean tag) {
		alertDialog.cancel();
		if (!tag) {
			toast(getString(R.string.s_ts_28), this);
		} else {
			if (user) {
				// 連線後,寫入成功
				new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.s_ts_29))
						.setPositiveButton(getString(R.string.s_ts_12), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {

							}
						}).setNegativeButton(getString(R.string.s_ts_13), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								user_upload();
								getReset(true);
							}
						}).show();
			}

		}
	}

	public static void toast(String text, Context con) {
		Toast.makeText(con, text + "", Toast.LENGTH_SHORT).show();
	}

	private void setNET() {
		// 取得通訊服務
		ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			// 是否有連線
			Boolean IsConnected = false;
			// 依序檢查各種網路連線方式
			for (NetworkInfo network : connectivity.getAllNetworkInfo()) {
				if (network.getState() == NetworkInfo.State.CONNECTED) {
					IsConnected = true;
					// 顯示網路連線種類
					Toast.makeText(getApplicationContext(),
							getString(R.string.s_ts_30) + network.getTypeName() + getString(R.string.s_ts_31),
							Toast.LENGTH_SHORT).show();
					net = true;
					ConnectAndSend();
				}
			}
			if (!IsConnected) {
				Toast.makeText(getApplicationContext(), getString(R.string.s_ts_32), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.s_ts_32), Toast.LENGTH_SHORT).show();
		}
	}

	private void ConnectAndSend() {
		new Thread(new Runnable() {
			public void run() {
				try {
					// 使用TLS協議
					SSLContext context = SSLContext.getInstance("TLS");

					// 服务器端需要验证的客户端证书 p12
					KeyStore keyManagers = KeyStore.getInstance("BKS");
					keyManagers.load(getResources().getAssets().open(KEY_STORE_CLIENT_PATH),
							KEY_STORE_PASSWORD.toCharArray());

					// 客户端信任的服务器端证书 bks
					KeyStore trustManagers = KeyStore.getInstance("BKS");
					trustManagers.load(getResources().getAssets().open(KEY_STORE_TRUST_PATH),
							KEY_STORE_PASSWORD.toCharArray());

					// 获得X509密钥库管理实例
					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
					keyManagerFactory.init(keyManagers, KEY_STORE_PASSWORD.toCharArray());
					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
					trustManagerFactory.init(trustManagers);

					// 初始化SSLContext
					context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

					// 获取SSLSocket
					skt = context.getSocketFactory().createSocket(ip, port);
					// OutputStream out = skt.getOutputStream();
					// out.write("Connection established.\n".getBytes());
					readData();

				} catch (UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException
						| KeyManagementException | IOException e) {
					Log.e("SSL.ERROR", e.toString());
				}
			}
		}).start();
	}

	private void Loding(Boolean bl) {
		Button p = (Button) main_view.get(0).findViewById(R.id.print_log);
		Button m = (Button) main_view.get(1).findViewById(R.id.menu_log);
		if (nfc) {
			if (bl) {
				p.setVisibility(View.VISIBLE);
				m.setVisibility(View.VISIBLE);
				main_left.setVisibility(View.GONE);
				main_right.setVisibility(View.GONE);
			} else {
				p.setVisibility(View.GONE);
				m.setVisibility(View.GONE);
			}
		} else {
			p.setVisibility(View.VISIBLE);
			m.setVisibility(View.VISIBLE);
		}
	}

	private void DateInput(Boolean i) {
		Adler32 inChecker = new Adler32();
		CheckedDataInput in = null;
		try {
			DataInputStream dis = new DataInputStream(skt.getInputStream());
			int size = dis.readInt();
			in = new CheckedDataInput(new DataInputStream(skt.getInputStream()), inChecker);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] data = new byte[size];
			in.readFully(data);
			out.write(data);
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			bitmap.compress(CompressFormat.JPEG, 100, out);
			if (i) {
				info_photo = bitmap;
			} else {
				phot_bitmap.add(bitmap);
			}
		} catch (IOException e) {
		}
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case 0:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setTitle(getString(R.string.s_ts_33)); // 设置标题
			mProgressDialog.setMessage(getString(R.string.s_ts_34) + ".."); // 设置body信息
			mProgressDialog.setMax(1); // 进度条最大值是100
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // 设置进度条样式是横向的
			return mProgressDialog;
		}
		return super.onCreateDialog(id);
	}

	public static void out(String msg) {
		if (net) {
			new MainSocketOutput(msg).start();
		}
	}

	public static String getLine(String msg, int run, char key) {
		return msg.substring(run).substring(0, msg.substring(run).indexOf(key));
	}

	public void onLocationChanged(Location location) { // 當地點改變時
		getLocation(location);
	}

	public void onProviderDisabled(String arg0) { // 當GPS或網路定位功能關閉時
		Toast.makeText(getBaseContext(), "Gps turned off ", Toast.LENGTH_LONG).show();
	}

	public void onProviderEnabled(String arg0) { // 當GPS或網路定位功能開啟
		Toast.makeText(getBaseContext(), "Gps turned on ", Toast.LENGTH_LONG).show();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) { // 定位狀態改變
		// status=OUT_OF_SERVICE 供應商停止服務
		// status=TEMPORARILY_UNAVAILABLE 供應商暫停服務
	}

	private void locationServiceInitial() {
		lms = (LocationManager) getSystemService(Context.LOCATION_SERVICE); // 取得系統定位服務
		Criteria criteria = new Criteria(); // 資訊提供者選取標準
		bestProvider = lms.getBestProvider(criteria, true); // 選擇精準度最高的提供者
		// 服務提供者、更新頻率60000毫秒=1分鐘、最短距離、地點改變時呼叫物件
		getLocation(lms.getLastKnownLocation(bestProvider));
	}

	private void getLocation(Location location) { // 將定位資訊顯示在畫面中
		if (location != null && gps) {
			Lngitude = (location.getLongitude()); // 取得經度
			Latitude = (location.getLatitude()); // 取得緯度
		}
	}

	private void outSMS() {
		out("SMS/" + car_brand + "/|");
	}

	private void outLocation() {
		boolean nbr = false;
		String La, Lo;
		La = String.valueOf(Latitude);
		Lo = String.valueOf(Lngitude);
		// 座標精準度確認
		if (La.length() > 4 && Lo.length() > 4 && medi_item.size() > 0) {
			for (int i = 0; i < medi_item.size(); i++) {
				String nbs = getLine(medi_item.get(i), 0, '/');
				out("GPS/" + nbs + "/" + La + "/" + Lo + "/|");
				if (nbs.equals(number)) {
					nbr = true;
				}
			}
		}
		if (La.length() > 4 && Lo.length() > 4 && !nbr) {
			out("GPS/" + number + "/" + La + "/" + Lo + "/|");
		}
	}

	private double gpsDistance(double lat_a, double lng_a, double lat_b, double lng_b) {
		double radLat1 = (lat_a * Math.PI / 180.0);
		double radLat2 = (lat_b * Math.PI / 180.0);
		double a = radLat1 - radLat2;
		double b = (lng_a - lng_b) * Math.PI / 180.0;
		double s = 2 * Math.asin(Math.sqrt(
				Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
		s = s * 6378137.0;
		s = Math.round(s * 10000) / 10000;
		return s;
	}

	private void LocationProvider() {
		// 取得系統定位服務
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (service.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			// 如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
			gps = true; // 確認開啟定位服務
			locationServiceInitial();
		} else {
			Toast.makeText(MainActivity.this, getString(R.string.s_ts_35), Toast.LENGTH_LONG).show();
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		}
	}

	private void LocationActivity(int mode) {
		String language = "tw";
		if (EN) {
			language = "en";
		}
		String Url = "https://maps.google.com/maps?f=d&hl=" + language;
		if (mode == 1) {
			if (hosp_count > 0) {
				// 啟動 Google地圖導航至指定醫院
				toast(getString(R.string.s_ts_36), MainActivity.this);
				String sLocation = Treatment_location.get(hosp_count);
				Url += "&saddr=&daddr=" + sLocation;
			}
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Url));
		intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
		startActivity(intent);
	}

	private void readData() {
		// TODO readData
		// new Thread(new Runnable() {
		// public void run() {
		try {
			BufferedReader buf = new BufferedReader(new InputStreamReader(skt.getInputStream(), "UTF-8"));
			if (number.length() < 2) {
				out("a0");
			} else {
				out(number);
			}
			sktRun = true;
			// setServiec();
			String Msg;
			while ((Msg = buf.readLine()) != null && sktRun) {
				// Log.e("SSL.msg", Msg);
				link = true;
				if (Msg.length() == 2) {
					switch (Msg) {
					case "a1":
						// 客戶端交握
						break;
					case "s1":
						out("s1");
						break;
					case "b0":
						DateInput(true);
						break;
					case "b1":
						DateInput(false);
						phot_count++;
						break;
					case "b2":
						DateInput(false);
						phot_count += 2;
						break;
					case "b3":
						DateInput(false);
						phot_count += 4;
						break;
					case "b4":
						DateInput(false);
						phot_count += 8;
						break;
					case "b5":
						DateInput(false);
						phot_count += 16;
						break;
					case "b6":
						DateInput(false);
						phot_count += 32;
						break;
					case "b7":
						PhotoHandler.obtainMessage().sendToTarget();
						break;
					case "c2":
						if (dblist) {
							out("c2");
						}
						break;
					default:
						break;
					}
				} else {
					// Msg.length() >= 3
					String tag = getLine(Msg, 0, '/');
					// 取得標籤
					switch (tag) {
					case "NO":
						String msgs = Msg.substring(3, Msg.indexOf('|'));
						number = msgs;
						NOHandler.obtainMessage().sendToTarget();
						break;
					case "DC":
						String DC = Msg.substring(3);
						if (!DC.equals("END")) {
							if (DC.length() > 10) {
								checkItem(DC);
							}
						} else {
							CPHandler.obtainMessage().sendToTarget();
						}
						break;
					case "C2":
						String C2 = Msg.substring(3);
						if (!C2.equals("END")) {
							String NUM = getLine(C2, 0, '/');
							if (NUM.length() < 5) {
								lead_item.clear();
							}
							lead_item.add(C2);
						} else {
							C2Handler.obtainMessage().sendToTarget();
						}
						break;
					case "C3":
						// 從後端取得傷患後自動導航
						// String C3 = Msg.substring(3);
						// if (!"0".equals(C3)) {
						// hosp_count = Integer.parseInt(C3);
						// if (hosp_count != 0) {
						// MSHandler.obtainMessage().sendToTarget();
						// }
						// } else {
						// HSHandler.obtainMessage().sendToTarget();
						// }
						break;
					case "NG":
						mmsg = Msg.substring(3);
						break;
					default:
						break;
					}
				}
			}
		} catch (Exception e) {
		}
		// }
		// }).start();
	}

	private Runnable sendSMS = new Runnable() {
		public void run() {
			if (car) {
				outSMS();
				SMSHandler.postDelayed(this, 10000);// 每10秒呼叫自己執行一次
			}
		}
	};

	private Handler RSMSHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			SMSHandler.removeCallbacks(sendSMS);// 如有運作則停止
			SMSHandler.postDelayed(sendSMS, 2000);// 設定2秒後啟動
		};
	};

	private Runnable sendGPS = new Runnable() {
		public void run() {
			if (gps) {
				outLocation();
				GPSHandler.postDelayed(this, 60000);// 每60秒呼叫自己執行一次
			}
		}
	};

	private Runnable GPSLine = new Runnable() {
		public void run() {
			if (gps) {
				String La, Ln;
				La = String.valueOf(Latitude);
				Ln = String.valueOf(Lngitude);
				// 座標精準度確認
				if (La.length() > 4 && Ln.length() > 4) {
					double at = Double.parseDouble(La);
					double ng = Double.parseDouble(Ln);
					ArrayList<String> aist = new ArrayList<String>();
					ArrayList<Double> dist = new ArrayList<Double>();
					for (int h = 1; h < Treatment_location.size(); h++) {
						String slat = getLine(Treatment_location.get(h), 0, ',');
						String slng = Treatment_location.get(h).substring(slat.length() + 1);
						double lat = Double.parseDouble(slat);
						double lng = Double.parseDouble(slng);
						double distance = gpsDistance(at, ng, lat, lng);
						aist.add(distance + "");
						dist.add(distance);
					}
					Collections.sort(dist); // 小到大自動排序
					int sdm = 0;
					String dm = dist.get(0) + "";
					for (int i = 0; i < aist.size(); i++) {
						if (aist.get(i).equals(dm)) { // 取得序列位置
							sdm = i;
							break;
						}
					}
					hosp_count = sdm + 1;
					if (hosp_count != 0) { // 救護車取得傷患資訊時
						MSHandler.obtainMessage().sendToTarget();
					}
				} else {
					toast("GPS定位失敗..無法自動搜尋", MainActivity.this);
				}
			}
		}
	};

	private Handler HSHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			LocationProvider();
			toast("正在搜尋醫院..", MainActivity.this);
			GPSHandler.postDelayed(GPSLine, 3000);// 設定3秒後啟動
		};
	};

	private Handler MSHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			spinner.setSelection(hosp_count, false);
			LocationActivity(1); // 開啟導航
		};
	};

	private Handler UIHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			LocationProvider();
			GPSHandler.removeCallbacks(sendGPS);// 如有運作則停止
			GPSHandler.postDelayed(sendGPS, 10000);// 設定10秒後啟動
		};
	};

	private Handler UPHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			new Timer(true).schedule(new TimerTask() {
				public void run() {
					out("NFC/" + setNFCString());
				}
			}, 2000);
		};
	};

	private Handler PPHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			new Timer(true).schedule(new TimerTask() {
				public void run() {
					if (link) {
						String unit = mediCar_unit.getText().toString() + "";
						String brand = mediCar_brand.getText().toString() + "";
						if (unit.length() > 1 && brand.length() > 1) {
							if (!car) {
								car = true;
								RSMSHandler.obtainMessage().sendToTarget(); // 開啟自動更新
							}
							out("CAR/" + unit + "/" + brand + "/5/|");
							car_brand = brand;
							save();
						}
					}
				}
			}, 3000);
		};
	};

	private Handler C2Handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (dblist) {
				// 檢視傷患資訊時刷新
				lead_upload();
			}
		};
	};

	private Handler CPHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			user_upload();
			pager1.setCurrentItem(1);
		};
	};

	private Handler NOHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String msgs = (number + "");
			print_No.setText(number);
			menu_No.setText(number);
			if (msgs.length() > 6) {
				String y = 20 + msgs.substring(0, 2);
				String m = msgs.substring(2, 4);
				String d = msgs.substring(4, 6);
				int Y = Integer.parseInt(y);
				int M = Integer.parseInt(m);
				int D = Integer.parseInt(d);
				checkToday(Y, M, D, true);
			}
		};
	};

	private Handler PhotoHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (phot_count > 0) {
				int cc = phot_count, set = 0;
				while (cc != 0) {
					if (cc - 32 >= 0) {
						cc -= 32;
						set++;
					} else if (cc - 16 >= 0) {
						cc -= 16;
						set++;
					} else if (cc - 8 >= 0) {
						cc -= 8;
						set++;
					} else if (cc - 4 >= 0) {
						cc -= 4;
						set++;
					} else if (cc - 2 >= 0) {
						cc -= 2;
						set++;
					} else {
						cc--;
						set++;
					}
				}
				menu_item.set(5, getString(R.string.s_Menu_6) + "<" + getString(R.string.s_List_3) + set
						+ getString(R.string.s_List_4) + ">");
				menu_upload(MainActivity.this);
			} else {
				menu_item.set(5, getString(R.string.s_Menu_6));
				menu_upload(MainActivity.this);
			}
		};
	};
}
