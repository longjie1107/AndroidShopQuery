package com.hdu.shopquery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.baidu.aip.asrwakeup3.core.mini.ActivityMiniRecog;
import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorPlanNode;
import com.baidu.mapapi.search.route.IndoorRoutePlanOption;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.hdu.shopquery.MiniActivity;
import com.hdu.shopquery.listener.MessageListener;
import com.hdu.shopquery.util.Auth;
import com.hdu.shopquery.util.DrawableEditText;
import com.hdu.shopquery.util.FileUtil;

import static com.hdu.shopquery.util.IOfflineResourceConst.DEFAULT_SDK_TTS_MODE;
/*
 * C1:7C:7B:B2:53:53:BF:8F:7E:1F:6C:C0:EF:BB:E9:15:48:6A:88:F6
 */
public class MainActivity extends ActivityMiniRecog {
    private static final int REQUEST_UI = 1,QUERY_WHAT = 1;
    private static final String TAG = "MainActivity",BASE_URL = "http://47.98.247.92:8080/shop/query?question=";;
    private DrawableEditText asrResult;
    private ImageButton searchBtn,asrBtn;
    private TextView answerText;
    private EventManager asr;
    private boolean logTime = true;
    private SpeechSynthesizer mSpeechSynthesizer;
    protected String appId,appKey,secretKey,sn;
    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    private TtsMode ttsMode = DEFAULT_SDK_TTS_MODE;
    // 定位与导航参数
    private static final int PERMISSIONS_REQUEST = 1;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private LocationClient mLocationClient = null;
    private Button button = null;
    private double myLatitude,myLongitude,dstLatitude,dstLongitude;
    private String myFloor,dstFloor;
    private String[] data = { "缴费机", "服务台", "收银台", "母婴室",
            "卫生间", "吸烟室", "ATM", "直梯", "出入口" };
    private String[] data2={"F1","F2","F3","F4","F5","F6","F7"};
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取权限
        List<String> permissionList = new ArrayList<>();
        // ACCESS_COARSE_LOCATION
        // ACCESS_FINE_LOCATION
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if(!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
        }
        setContentView(R.layout.activity_main);
        // 基于sdk集成1.1 初始化EventManager对象
        asr = EventManagerFactory.create(this, "asr");
        // 基于sdk集成1.3 注册自己的输出事件类
        asr.registerListener(this); //  EventListener 中 onEvent方法
        init();
        // 语音识别
        asrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });
        //listview
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                MainActivity.this,android.R.layout.simple_list_item_1,data
        );
        ArrayAdapter<String>adapter1=new ArrayAdapter<String>(
                MainActivity.this,android.R.layout.simple_list_item_1,data2
        );

        ListView listView=(ListView) findViewById(R.id.listview1);
        //listView.bringToFront();
        ListView listView1=(ListView) findViewById(R.id.listview2);
        //listView1.bringToFront();
        listView.setAdapter(adapter);
        listView1.setAdapter(adapter1);
        // 知识图谱查询
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thread2GetQueryResult(asrResult.getText().toString());
            }
        });

        // 获取我的位置信息
        final LocateUtil locateUtil = new LocateUtil();

        locateUtil.initLocationOption(); //定位初始化
        locateUtil.myhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what==123){
                    locateUtil.location=(BDLocation)msg.obj;           //定位后的对象，可以使用getLatitude,getLongitude,getFloor,getBuildingID获取信息
                    myLatitude=locateUtil.location.getLatitude();
                    myLongitude=locateUtil.location.getLongitude();
                    myFloor = locateUtil.location.getFloor();
                    locateUtil.bid = locateUtil.location.getBuildingID();
                    TextView tv;
                    tv = findViewById(R.id.MyPosText);
                    tv.setText(myLatitude+" "+myLongitude+" "+locateUtil.bid);
//                    locateUtil.IndoorPoiSearch("小龙坎");
                    dstLatitude = locateUtil.end_latitude;
                    dstLongitude = locateUtil.end_longitude;
                    dstFloor = locateUtil.end_floor;
                }
            }
        };
        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routePlanWithRouteNode();
            }
        });
        mBaiduMap.setMyLocationEnabled(true);//开启定位图层
        mBaiduMap.setIndoorEnable(true);//打开室内图，默认为关闭状态
        mBaiduMap.setOnBaseIndoorMapListener(new BaiduMap.OnBaseIndoorMapListener() {
            @Override
            public void onBaseIndoorMapMode(boolean on, MapBaseIndoorMapInfo mapBaseIndoorMapInfo) {
                if (on && mapBaseIndoorMapInfo != null) {
                    // 进入室内图
                    // 通过获取回调参数 mapBaseIndoorMapInfo 便可获取室内图信
                    //息，包含楼层信息，室内ID等
                    // 切换楼层信息
                    //strID 通过 mMapBaseIndoorMapInfo.getID()方法获得
                    MapBaseIndoorMapInfo.SwitchFloorError switchFloorError =
                            mBaiduMap.switchBaseIndoorMapFloor(mapBaseIndoorMapInfo.getCurFloor(), mapBaseIndoorMapInfo.getID());
                    Log.d(TAG, "switch floor status: " + switchFloorError.toString());
                } else {
                    // 移除室内图
                }
            }
        });
        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback(){
            @Override
            public void onMapLoaded(){
                BDLocation center = mLocationClient.getLastKnownLocation();
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(new LatLng(center.getLatitude(), center.getLongitude()));
                mBaiduMap.animateMapStatus(update);
            }
        });
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }
    //通过继承抽象类BDAbstractListener并重写其onReceieveLocation方法来获取定位数据，并将其传给MapView
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
        }
    }
    // 初始化视图与语音合成模块
    private void init() {
        asrResult = findViewById(R.id.AsrResult);
        answerText = findViewById(R.id.AnswerText);
        searchBtn = findViewById(R.id.SearchBtn);
        asrBtn = findViewById(R.id.AsrBtn);
        appId = Auth.getInstance(this).getAppId();
        appKey = Auth.getInstance(this).getAppKey();
        secretKey = Auth.getInstance(this).getSecretKey();
        sn = Auth.getInstance(this).getSn();
        SpeechSynthesizerListener listener = new MessageListener();
        // 1. 获取语音合成实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);
        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener(listener);
        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setApiKey");
        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声  3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
        // 6. 初始化
        result = mSpeechSynthesizer.initTts(ttsMode);
        checkResult(result, "initTts");
    }

    //语音合成功能
    private void speak() {
        if (mSpeechSynthesizer == null) {
            Log.i(TAG,"[ERROR], 初始化失败");
            return;
        }
        int result = mSpeechSynthesizer.speak(answerText.getText().toString());
//        mShowText.setText("");
//        print("合成并播放 按钮已经点击");
//        checkResult(result, "speak");
    }

    //语音识别开始
    private void start() {
        String event = SpeechConstant.ASR_START;
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        // 基于SDK集成2.1 设置识别参数
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        // 此段可以自动检测错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        txtLog.append(message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);
        String json = null; // 可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json
        asr.send(event, json, null, 0, 0);
        printLog("输入参数：" + json);
    }

    //停止语音识别
    private void stop() {
        printLog("停止识别：ASR_STOP");
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0); //
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "name: " + name;

        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            // 识别相关的结果都在这里
            if (params == null || params.isEmpty()) {
                return;
            }
            if (params.contains("\"nlu_result\"")) {
                // 一句话的语义解析结果
                if (length > 0 && data.length > 0) {
                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            } else if (params.contains("\"partial_result\"")) {
                // 一句话的临时识别结果
                logTxt += ", 临时识别结果：" + params;
            } else if (params.contains("\"final_result\"")) {
                // 一句话的最终识别结果
                logTxt += ", 最终识别结果：" + params;
                try{
                    JSONObject result = new JSONObject(params);
                    asrResult.setText(result.getString("best_result"));
                }catch (JSONException e){
                    e.printStackTrace();
                }
            } else {
                // 一般这里不会运行
                logTxt += " ;params :" + params;
                if (data != null) {
                    logTxt += " ;data length=" + data.length;
                }
            }
        } else {
            // 识别开始，结束，音量，音频数据回调
            if (params != null && !params.isEmpty()) {
                logTxt += " ;params :" + params;
            }
            if (data != null) {
                logTxt += " ;data length=" + data.length;
            }
        }
        printLog(logTxt);
    }

    private void printLog(String text) {
        if (logTime) {
            text += "  ;time=" + System.currentTimeMillis();
        }
        text += "\n";
        Log.i(getClass().getName(), text);
        txtLog.append(text + "\n");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.i(TAG,"error code :" + result + " method:" + method);
        }
    }

    public void thread2GetQueryResult(final String question){
        // 由于UI线程中不允许耗时操作
        // 此处创建一个线程来进行耗时操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(BASE_URL+question);
                HttpResponse httpResponse = null;
                try {
                    httpResponse = httpClient.execute(httpGet);
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        String temp = EntityUtils.toString(httpResponse.getEntity());
                        // 获取一个Message对象，设置what为QUERY_WHAT
                        Message msg = Message.obtain();
                        msg.obj = temp;
                        msg.what = QUERY_WHAT;
                        // 发送这个消息到消息队列中
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case QUERY_WHAT:
                    String res = (String) msg.obj;
                    answerText.setText(res);
                    speak();
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
            Log.i(TAG,"释放资源成功");
        }
        super.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
    }
    private void routePlanWithRouteNode() {
        RoutePlanSearch mSearch = RoutePlanSearch.newInstance();
        OnGetRoutePlanResultListener listener = new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {
                //创建IndoorRouteOverlay实例
                IndoorRouteOverlay overlay = new IndoorRouteOverlay(mBaiduMap);
                if (indoorRouteResult.getRouteLines() != null && indoorRouteResult.getRouteLines().size() > 0) {
                    //获取室内路径规划数据（以返回的第一条路线为例）
                    //为IndoorRouteOverlay实例设置数据
                    overlay.setData(indoorRouteResult.getRouteLines().get(0));
                    //在地图上绘制IndoorRouteOverlay
                    overlay.addToMap();
                }
            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        mSearch.setOnGetRoutePlanResultListener(listener);
        IndoorPlanNode startNode = new IndoorPlanNode(new LatLng(myLatitude, myLongitude), "F1");
        IndoorPlanNode endNode = new IndoorPlanNode(new LatLng(dstLatitude, dstLongitude), dstFloor);
        mSearch.walkingIndoorSearch(new IndoorRoutePlanOption()
                .from(startNode)
                .to(endNode));
        mSearch.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSIONS_REQUEST:
                if (grantResults.length > 0) {
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != getPackageManager().PERMISSION_GRANTED){
                            Log.d(TAG, "request " + permissions[i] + " permission fail");
                        }else{
                            Log.d(TAG, "request " + permissions[i] + " permission success");
                        }
                    }
                } else {
                    Log.d(TAG, "request permissions fail");
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
