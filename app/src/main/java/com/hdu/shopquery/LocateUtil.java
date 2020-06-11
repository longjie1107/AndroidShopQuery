package com.hdu.shopquery;


import android.os.Handler;
import android.os.Message;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorInfo;
import com.baidu.mapapi.search.poi.PoiIndoorOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.List;

public class LocateUtil {
    public String bid="";
    BDLocation location;
    Handler myhandle;
    double end_latitude;
    double end_longitude;
    String end_floor;

    public void initLocationOption()  {
//定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        LocationClient locationClient = new LocationClient(DemoApplication.getContext());
//声明LocationClient类实例并配置定位参数
        LocationClientOption locationOption = new LocationClientOption();
        MyLocationListener myLocationListener = new MyLocationListener();
//注册监听函数
        locationClient.registerLocationListener(myLocationListener);
//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setCoorType("bd09ll");
//可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setScanSpan(1000);
//可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedAddress(true);
//可选，设置是否需要地址描述
        locationOption.setIsNeedLocationDescribe(true);
//可选，设置是否需要设备方向结果
        locationOption.setNeedDeviceDirect(false);
//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setLocationNotify(true);
//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIgnoreKillProcess(true);
//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationDescribe(true);
//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIsNeedLocationPoiList(true);
//可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.SetIgnoreCacheException(false);
//可选，默认false，设置是否开启Gps定位
        locationOption.setOpenGps(true);
//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setIsNeedAltitude(false);
//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        locationOption.setOpenAutoNotifyMode();
//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        locationOption.setOpenAutoNotifyMode(3000,1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
//需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        locationClient.setLocOption(locationOption);
//开始定位
//        locationClient.startIndoorMode();
        locationClient.start();
//        location=locationClient.getLastKnownLocation();
    }
    /**
     * 实现定位回调
     */



    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdlocation){
            if(bdlocation==null){
                return;
            }
            Message msg=Message.obtain();
            msg.what=123;msg.arg1=1;msg.arg2=2;msg.obj=bdlocation;
            myhandle.sendMessage(msg);
//              setLat(bdlocation.getLatitude());

//            myapp.my_longitude = bdlocation.getLongitude();
//            myapp.bid=bdlocation.getBuildingID();
//            myapp.my_floor = bdlocation.getFloor();
//            getLocType=bdlocation.getLocType();
//            StringBuffer cur =new StringBuffer();
//            cur.append("纬度：").append(my_latitude).append("\n");
//            cur.append("经度：").append(my_longitude).append("\n");
//            cur.append("楼层：").append(my_floor).append("\n");
//            cur.append("bid：").append(bid).append("\n");
//            cur.append(getLocType);
//            TextView positionText=(TextView) findViewById(R.id.position_text_view);
//            positionText.setText(cur);
//            System.out.println("123456");

        }
    }
    /**
     * 室内poi检索示例
     */
    public void IndoorPoiSearch(String PoiName) {
        //创建poi检索实例
        PoiSearch poiSearch = PoiSearch.newInstance();
        //创建poi监听者
        OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {
            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult result) {
                List<PoiIndoorInfo> poi_info=result.getArrayPoiInfo();
                PoiIndoorInfo poi=poi_info.get(0);
                end_latitude=poi.latLng.latitude;                                       //终点的信息
                end_longitude=poi.latLng.longitude;
                end_floor=poi.floor;
            }
        };
        //设置poi监听者该方法要先于检索方法searchNearby(PoiNearbySearchOption)前调用，否则会在某些场景出现拿不到回调结果的情况
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
        //设置请求参数
        PoiIndoorOption indoor_Option = new PoiIndoorOption().poiIndoorBid(bid).poiIndoorWd(PoiName);
        //发起请求
        poiSearch.searchPoiIndoor(indoor_Option);
        //释放检索对象
        poiSearch.destroy();
    }
}
