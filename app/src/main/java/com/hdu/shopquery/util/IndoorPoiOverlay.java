package com.hdu.shopquery.util;

import android.os.Bundle;
import android.os.Parcelable;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.hdu.shopquery.OverlayManager;
import com.hdu.shopquery.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于显示poi的overly
 */
public class IndoorPoiOverlay extends OverlayManager {

    private static final int MAX_POI_SIZE = 10;

    private PoiIndoorResult mPoiIndoorResult = null;

    /**
     * 构造函数
     *
     * @param baiduMap   该 PoiOverlay 引用的 BaiduMap 对象
     */
    public IndoorPoiOverlay(BaiduMap baiduMap) {
        super(baiduMap);
    }

    /**
     * 设置POI数据
     * 
     * @param poiIndoorResult    设置POI数据
     */
    public void setData(PoiIndoorResult poiIndoorResult) {
        this.mPoiIndoorResult = poiIndoorResult;
    }

    @Override
    public final List<OverlayOptions> getOverlayOptions() {
        if (mPoiIndoorResult == null || mPoiIndoorResult.getArrayPoiInfo() == null) {
            return null;
        }

        List<OverlayOptions> markerList = new ArrayList<>();
        int markerSize = 0;

        for (int i = 0; i < mPoiIndoorResult.getArrayPoiInfo().size() && markerSize < MAX_POI_SIZE; i++) {
            if (mPoiIndoorResult.getArrayPoiInfo().get(i).latLng == null) {
                continue;
            }
            String iconIndex = markerSize + "";
            markerSize++;
            int icon = getResId("icon_mark" + iconIndex, R.drawable.class);
            Bundle bundle = new Bundle();
            bundle.putInt("index", i);
            bundle.putParcelable("info", (Parcelable) mPoiIndoorResult.getArrayPoiInfo().get(i));
            String a=mPoiIndoorResult.getArrayPoiInfo().get(i).name;
            markerList.add(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(icon))
                    .extraInfo(bundle)
                    .position(mPoiIndoorResult.getArrayPoiInfo().get(i).latLng));
        }

        return markerList;
    }
    private static int getResId(String variableName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(variableName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * 获取该PoiIndoorOverlay的poi数据
     * 
     * @return     POI数据
     */
    public PoiIndoorResult getPoiIndoorResult() {
        return mPoiIndoorResult;
    }

    /**
     * 覆写此方法以改变默认点击行为
     * 
     * @param i    被点击的poi在
     *             {@link PoiResult#getAllPoi()} 中的索引
     * @return     true--事件已经处理，false--事件未处理
     */
    public boolean onPoiClick(int i) {
//        if (mPoiResult.getAllPoi() != null
//                && mPoiResult.getAllPoi().get(i) != null) {
//            Toast.makeText(DemoApplication.getContext(),
//                    mPoiResult.getAllPoi().get(i).name, Toast.LENGTH_LONG)
//                    .show();
//            return true;
//        }
        return false;
    }

    @Override
    public final boolean onMarkerClick(Marker marker) {
        if (!mOverlayList.contains(marker)) {
            return false;
        }

        if (marker.getExtraInfo() != null) {
            return onPoiClick(marker.getExtraInfo().getInt("index"));
        }

        return false;
    }

    @Override
    public boolean onPolylineClick(Polyline polyline) {
        return false;
    }
}
