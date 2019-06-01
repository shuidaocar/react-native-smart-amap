package com.reactnativecomponent.amap;

import android.graphics.BitmapFactory;

import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RCTAMapModule extends ReactContextBaseJavaModule implements PoiSearch.OnPoiSearchListener {
    ReactApplicationContext mContext;

    //TODO:为了使用主项目中的资源，这里保存资源ID配置。因为这里无法直接访问主项目的R文件。
    public Map<String, Integer> mapIcons = new HashMap<String, Integer>();

    private PoiSearch poiSearch;
    private int defaultRadius = 3000;

    public RCTAMapModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        PoiSearch poiSearch = new PoiSearch(mContext, null);
        this.poiSearch = poiSearch;

        //TODO:为了使用主项目中的资源，手动在这里配置资源ID。
        mapIcons.put("map_icon_other", 0x7f030001);
        mapIcons.put("map_icon_shuidao", 0x7f030002);
    }

    @Override
    public String getName() {
        return "AMapModule";
    }

    @ReactMethod
    public void setOptions(final int reactTag, final ReadableMap options) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));
                if (options.hasKey("centerCoordinate")) {
                    ReadableMap centerCoordinateMap = options.getMap("centerCoordinate");
                    mapView.setLatLng(new LatLng(centerCoordinateMap.getDouble("latitude"), centerCoordinateMap.getDouble("longitude")));
                }
                if (options.hasKey("zoomLevel")) {
                    double zoomLevel = options.getDouble("zoomLevel");
                    mapView.setZoomLevel(zoomLevel);
                }
            }
        });
    }

    @ReactMethod
    public void setCenterCoordinate(final int reactTag, final ReadableMap coordinate) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));
                mapView.setCenterLocation(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
            }
        });
    }

    @ReactMethod
    public void searchPoiByCenterCoordinate(ReadableMap params) {

        String types = "";
        if (params.hasKey("types")) {
            types = params.getString("types");
        }
        String keywords = "";
        if (params.hasKey("keywords")) {
            keywords = params.getString("keywords");
        }

        PoiSearch.Query query = new PoiSearch.Query(keywords, types);

        if (params.hasKey("offset")) {
            int offset = params.getInt("offset");
            query.setPageSize(offset);// 设置每页最多返回多少条poiitem
        }
        if (params.hasKey("page")) {
            int page = params.getInt("page");
            query.setPageNum(page);//设置查询页码
        }
        poiSearch.setQuery(query);
        if (params.hasKey("coordinate")) {
            ReadableMap coordinateMap = params.getMap("coordinate");
            double latitude = coordinateMap.getDouble("latitude");
            double longitude = coordinateMap.getDouble("longitude");
            int radius = defaultRadius;
            if (params.hasKey("radius")) {
                radius = params.getInt("radius");
            }
            poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude, longitude), radius)); //设置周边搜索的中心点以及半径(单位: 米, 默认3公里)
        }
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();
    }

    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        List<PoiItem> poiItems;
        WritableMap dataMap = Arguments.createMap();
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                // 取得搜索到的poiitems有多少页
                poiItems = result.getPois();// 取得第一页的poiitem数据，页数从数字0开始

                WritableArray array = Arguments.createArray();
                for (PoiItem poi : poiItems) {
                    WritableMap data = Arguments.createMap();
                    data.putString("uid", poi.getPoiId());
                    data.putString("name", poi.getTitle());
                    data.putString("type", poi.getTypeDes());
                    data.putDouble("longitude", poi.getLatLonPoint().getLongitude());
                    data.putDouble("latitude", poi.getLatLonPoint().getLatitude());
                    data.putString("address", poi.getSnippet());
                    data.putString("tel", poi.getTel());
                    data.putInt("distance", poi.getDistance());
                    data.putString("cityCode", poi.getCityCode());
                    data.putString("cityName", poi.getCityName());
                    data.putString("provinceCode", poi.getProvinceCode());
                    data.putString("provinceName", poi.getProvinceName());
                    data.putString("adCode", poi.getAdCode());
                    data.putString("adName", poi.getAdName());
                    array.pushMap(data);
                }
                dataMap.putArray("searchResultList", array);
            }
        } else {
            WritableMap error = Arguments.createMap();
            error.putString("code", String.valueOf(rCode));
            dataMap.putMap("error", error);
        }

        mContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("amap.onPOISearchDone", dataMap);
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    @ReactMethod
    public void addMarker(final int reactTag, final ReadableMap markerConf) {
        mContext.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                final RCTAMapView mapView = ((RCTAMapView) mContext.getCurrentActivity().findViewById(reactTag));
                final String pinIcon = markerConf.getString("icon");
                final int pinIconId = mapIcons.get(pinIcon) == null ? -1 : mapIcons.get(pinIcon);

                MarkerOptions markerOption = new MarkerOptions();
                markerOption.position(new LatLng(markerConf.getDouble("latitude"), markerConf.getDouble("longitude")));
                markerOption.title(markerConf.getString("title")).snippet(markerConf.getString("subtitle"));
                markerOption.draggable(false);//设置Marker可拖动
                if (pinIconId != 0 && pinIconId != -1) {
                    markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(mContext.getCurrentActivity().getResources(), pinIconId)));
                } else {
                    markerOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }
                mapView.addMarkerWithOption(markerOption);
            }
        });
    }
}
