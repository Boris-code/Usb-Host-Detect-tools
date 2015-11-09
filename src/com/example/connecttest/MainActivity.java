package com.example.connecttest;

import java.io.IOException;
import java.util.List;

import android.provider.Settings.Secure;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

import com.avos.avoscloud.AVACL;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.CountCallback;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.SaveCallback;
//import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.SaveCallback;

public class MainActivity extends Activity {
	private final String appId="H5mXK7D6co9hC0mNS1KgwF2N";
	private final String appKey="3i6oGDIccraGHl8eK1A9Lymw";
	
	
	private TextView textView;

	private Button musicBtn;
	private Button usbBtn;
	private Button unSupportUsbBtn;
	private Button submitBtn;
	
	private TextView usbAudioTV;

	// 系统信息
	private String modelNumber; // 型号
	private String sdkVersion;
	private String androidVersion;
	private String usbAudio = null;
	private String usbHost = "不支持";
	private String basebandVersion; // 基带版本
	private String board;// 主版
	private String brand;// 系统定制商
	private String cpuAbi;// cpu指令集
	private String device;// 设备参数
	private String display;// 显示屏参数
	private String fingerPrint;// 硬件名称
	private String host;// 主机
	private String id;// 修订版本列表
	private String manufacturer;// 硬件制造商
	private String model;// 版本
	private String product;// 手机制造商
	private String tags;// 描述build的标签
	private String time;// 系统 时间
	private String type;// builder类型
	private String user;// 系统用户
	private String codeNameVersion;// 当前开发代号
	private String sdkIntVerstion;// 版本号
	
	private String imei;//手机的唯一标识 每个手机对应一个

	private MediaPlayer mediaPlayer;
	
	private boolean playMusic=false;

	private static MainActivity mainActivity = null;

	public static MainActivity getInstance() {
		return mainActivity;
	}

	public void findViewById() {
		textView = (TextView) findViewById(R.id.text);
		musicBtn = (Button) findViewById(R.id.musicBtn);
		usbBtn = (Button) findViewById(R.id.usbBtn);
		unSupportUsbBtn = (Button) findViewById(R.id.unSupportUsbBtn);
		submitBtn = (Button) findViewById(R.id.submitBtn);
		usbAudioTV=(TextView)findViewById(R.id.usbAudioTV);
	}

	public MediaPlayer createLocalMp3() {
		/**
		 * 创建音频文件的方法：
		 * 1、播放资源目录的文件：MediaPlayer.create(MainActivity.this,R.raw.music
		 * );//播放res/raw 资源目录下的MP3文件
		 */
		MediaPlayer mp = MediaPlayer.create(this, R.raw.journey_scene_bg);
		mp.stop();
		return mp;
	}

	public void buttonEvent() {
		// 播放音乐Button
		musicBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				if (musicBtn.getText().toString().equals("播放音乐")) {
					boolean createState = false;
					if (mediaPlayer == null) {
						mediaPlayer = createLocalMp3();
						createState = true;
						playMusic=true;
						
					}
					// 当播放完音频资源时，会触发onCompletion事件，可以在该事件中释放音频资源，
					// 以便其他应用程序可以使用该资源:
					mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer mp) {
									mp.release();// 释放音频资源
								}
							});
					try {
						// 在播放音频资源之前，必须调用Prepare方法完成些准备工作
						if (createState)
							mediaPlayer.prepare();
						// 开始播放音频
						mediaPlayer.start();
						musicBtn.setText("暂停");
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (musicBtn.getText().toString().equals("暂停")) {
					if (mediaPlayer != null) {
						mediaPlayer.pause();// 暂停
						musicBtn.setText("播放音乐");
					}
				}
			}
		});

		// UsbBtn
		usbBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(!playMusic){
					showDialog("提示", "请先连接钢琴，播放音乐，然后判断");
				}else{
					usbAudio = "支持";
					usbAudioTV.setText("Usb Audio: "+ "支持");
				}
			}
		});

		// unSupportUsbBtn
		unSupportUsbBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(!playMusic){
					showDialog("提示", "请先连接钢琴，播放音乐，然后判断");
				}else{
					usbAudio = "不支持";
					usbAudioTV.setText("Usb Audio: "+ "不支持");
				}
			}
		});

		// submitBtn
		submitBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (usbAudio == null||playMusic==false) {
					showDialog("提示", "请先判断是否支持USB host");
				} else {
					submitBtn.setText("正在上传..");

					// 查重
					AVQuery<AVObject> query = new AVQuery<AVObject>("AndroidMsg");
					query.whereEqualTo("imei", imei);
					query.findInBackground(new FindCallback<AVObject>() {
						public void done(List<AVObject> avObjects, AVException e) {
							if (e == null) {
								if (avObjects.size() > 0) {
									System.out.println("count " + avObjects.size());
									System.out.println("objectId "+avObjects.get(0).getObjectId());
									// 已存在
									showSelectDialog(avObjects.get(0).getObjectId());
//									showDialog("请注意", "数据库中已存在");
									submitBtn.setText("已存在");
								} else {
									// 数据库中不存在 可以存储
									saveMsg();
								}
							} else {
								// The request failed
								String error = e.getMessage();
								System.out.println(e);
								showDialog("上传失败", error);
								submitBtn.setText("重新上传");
							}
						}
					});
				}
			}
		});

	}

	public void saveMsg() {
		// 提交信息
		AVObject androidMsg = new AVObject("AndroidMsg");
		androidMsg.put("model_number", modelNumber);
		androidMsg.put("android_version", androidVersion);
		androidMsg.put("sdk_version", sdkVersion);
		androidMsg.put("usb_host", usbHost);
		androidMsg.put("usb_audio", usbAudio);
		androidMsg.put("baseband_version", basebandVersion);
		androidMsg.put("board", board);
		androidMsg.put("brand", brand);
		androidMsg.put("cpu_abi", cpuAbi);
		androidMsg.put("device", device);
		androidMsg.put("display", display);
		androidMsg.put("finger_print", fingerPrint);
		androidMsg.put("host", host);
		androidMsg.put("id", id);
		androidMsg.put("manufacturer", manufacturer);
		androidMsg.put("model", model);
		androidMsg.put("product", product);
		androidMsg.put("tags", tags);
		androidMsg.put("time", time);
		androidMsg.put("type", type);
		androidMsg.put("user", user);
		androidMsg.put("code_name_version", codeNameVersion);
		androidMsg.put("sdk_int_verstion", sdkIntVerstion);
		androidMsg.put("imei", imei);

		androidMsg.saveInBackground(new SaveCallback() {

			@Override
			public void done(AVException e) {
				// TODO Auto-generated method stub
				if (e == null) {
					// 保存成功
					showDialog("好消息", "上传成功");
					submitBtn.setText("上传成功");
				} else {
					// 保存失败
					String error = e.getMessage();
					System.out.println(error);
					showDialog("上传失败", error);
					submitBtn.setText("重新上传");
				}

			}
		});

	}
	
	//更新数据
	public void upateMsg(String objectId) {
		// 提交信息
		AVObject androidMsg = AVObject.createWithoutData("AndroidMsg",objectId);

		androidMsg.put("model_number", modelNumber);
		androidMsg.put("android_version", androidVersion);
		androidMsg.put("sdk_version", sdkVersion);
		androidMsg.put("usb_host", usbHost);
		androidMsg.put("usb_audio", usbAudio);
		androidMsg.put("baseband_version", basebandVersion);
		androidMsg.put("board", board);
		androidMsg.put("brand", brand);
		androidMsg.put("cpu_abi", cpuAbi);
		androidMsg.put("device", device);
		androidMsg.put("display", display);
		androidMsg.put("finger_print", fingerPrint);
		androidMsg.put("host", host);
		androidMsg.put("id", id);
		androidMsg.put("manufacturer", manufacturer);
		androidMsg.put("model", model);
		androidMsg.put("product", product);
		androidMsg.put("tags", tags);
		androidMsg.put("time", time);
		androidMsg.put("type", type);
		androidMsg.put("user", user);
		androidMsg.put("code_name_version", codeNameVersion);
		androidMsg.put("sdk_int_verstion", sdkIntVerstion);
		androidMsg.put("imei", imei);

		androidMsg.saveInBackground(new SaveCallback() {

			@Override
			public void done(AVException e) {
				// TODO Auto-generated method stub
				if (e == null) {
					// 保存成功
					showDialog("好消息", "更新成功");
					submitBtn.setText("更新成功");
				} else {
					// 保存失败
					String error = e.getMessage();
					System.out.println(error);
					showDialog("更新失败", error);
					submitBtn.setText("重新更新");
				}

			}
		});

	}

	// 弹出对话框
	public void showDialog(String title, String Msg) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(title);
		builder.setPositiveButton("OK", null);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setMessage(Msg);
		builder.show();
	}
	
	public void showSelectDialog(final String objectId) {
		new AlertDialog.Builder(this).setTitle("数据库中已存在").setMessage("确认更新该数据？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						System.out.println("确认更新  " + objectId);
						submitBtn.setText("正在更新..");
						upateMsg(objectId);
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}).show();
	}

	public void getSystemMsg() {
		Build mBuild = new Build();
		modelNumber = Build.MODEL;
		sdkVersion = Build.VERSION.SDK;
		androidVersion = Build.VERSION.RELEASE;
		basebandVersion = Build.VERSION.INCREMENTAL;
		board = mBuild.BOARD;// 主版
		brand = mBuild.BRAND;// 系统定制商
		cpuAbi = mBuild.CPU_ABI;// cpu指令集
		device = mBuild.DEVICE;// 设备参数
		display = mBuild.DISPLAY;// 显示屏参数
		fingerPrint = mBuild.FINGERPRINT;// 硬件名称
		host = mBuild.HOST;// 主机
		id = mBuild.ID;// 修订版本列表
		manufacturer = mBuild.MANUFACTURER;// 硬件制造商
		model = mBuild.MODEL;// 版本
		product = mBuild.PRODUCT;// 手机制造商
		tags = mBuild.TAGS;// 描述build的标签
		time = new String().valueOf(mBuild.TIME);// 系统 时间
		type = mBuild.TYPE;// builder类型
		user = mBuild.USER;// 系统用户
		codeNameVersion = Build.VERSION.CODENAME;// 当前开发代号
		sdkIntVerstion = new String().valueOf(Build.VERSION.SDK_INT);// 版本号
		
		TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE); 
		imei=TelephonyMgr.getDeviceId();

		// 显示
		textView.setText("手机型号: " + modelNumber + "\nSDK版本:" + sdkVersion
				+ "\n系统版本:" + androidVersion + "\n基带版本:" + basebandVersion);
	}

	public void supportUsbHost() {
		usbHost = "支持";
		textView.setText(textView.getText() + "\nUsb host :" + "支持");

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mainActivity = this;
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 竖屏

		findViewById();

		MidiDeviceManager.getInstance().init(this);
		getSystemMsg();

		// leanCloud
		AVOSCloud.initialize(this, appId,appKey);

		// Button 事件
		buttonEvent();
	}
	
	@Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	MidiDeviceManager.getInstance().init(this);
    }
    

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
