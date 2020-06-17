package com.example.covid19_local_messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements NaverMap.OnMapClickListener, Overlay.OnClickListener,
        OnMapReadyCallback, NaverMap.OnCameraChangeListener, NaverMap.OnCameraIdleListener{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationSource locationSource;
    private InfoWindow infoWindow;
    private NaverMap naverMap;

    private List<Marker> markerList = new ArrayList<Marker>();
    private boolean isCameraAnimated = false;

    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapView mapView = findViewById(R.id.map_view);
        mapView.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        //현재 위치 추적 (권한)
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Face);

        infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultViewAdapter(this) {
            @NonNull
            @Override
            protected View getContentView(@NonNull InfoWindow infoWindow) { // 인포윈도우
                Marker marker = infoWindow.getMarker();
                Store store = (Store) marker.getTag();

                View view = View.inflate(MainActivity.this, R.layout.info_view, null);

                ((TextView) view.findViewById(R.id.name)).setText(store.name); // 약국이름

                if ("plenty".equalsIgnoreCase(store.remain_stat)) { // 마스크 재고량
                    ((TextView) view.findViewById(R.id.stock)).setText("재고: 100개 ~");
                } else if ("some".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("재고: 30 ~ 100개");
                } else if ("few".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("재고: 2 ~ 30개");
                } else if ("empty".equalsIgnoreCase(store.remain_stat)){
                    ((TextView) view.findViewById(R.id.stock)).setText("재고: 0 ~ 1개");
                } else if ("break".equalsIgnoreCase(store.remain_stat)) {
                    ((TextView) view.findViewById(R.id.stock)).setText("판매중지");
                } else {
                    ((TextView) view.findViewById(R.id.stock)).setText("정보없음");
                }
                ((TextView) view.findViewById(R.id.addr)).setText(store.addr); // 약국 주소
                return view;
            }
        });

        //현재 위치로 이동 버튼
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        //중심점 위도 경도
        LatLng mapCenter = naverMap.getCameraPosition().target;
        fetchStoreSale(mapCenter.latitude, mapCenter.longitude, 2000);

        //지도 움직임 확인
        naverMap.addOnCameraChangeListener(this);
        naverMap.addOnCameraIdleListener(this);
        naverMap.setOnMapClickListener(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    float accuracy = location.getAccuracy();
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }



    @Override //위치 권한
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.Face);
            }
            return;
        }
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }



    private void fetchStoreSale(double lat, double lng, int m) { // 위도, 경도, 반경(m)
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://8oi9s0nnth.apigw.ntruss.com").addConverterFactory(GsonConverterFactory.create()).build();
        MaskApi maskApi = retrofit.create(MaskApi.class);
        maskApi.getStoresByGeo(lat, lng, m).enqueue(new Callback<StoreSaleResult>() {
            @Override
            public void onResponse(Call<StoreSaleResult> call, Response<StoreSaleResult> response) {
                if (response.code() == 200) { //200 - 성공 응답 코드
                    StoreSaleResult result = response.body();
                    updateMapMarkers(result);
                }
            }
            @Override
            public void onFailure(Call<StoreSaleResult> call, Throwable t) {
            }
        });
    }

    private void updateMapMarkers(StoreSaleResult result) {
        resetMarkerList();
        if (result.stores != null && result.stores.size() > 0) { //판매처가 한 곳이라도 있으면
            for (Store store : result.stores) { //그 갯수만큼 반복하며 마커 생성
                Marker marker = new Marker();
                marker.setPosition(new LatLng(store.lat, store.lng)); //마커 위치 설정
                marker.setTag(store); //마커에 store객체 붙여줌 - infowindow

                //마스크 잔량 별 색 표시
                if ("plenty".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.green));     //100개 ~ (녹색): 'plenty'

                } else if ("some".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.yellow));    //30 ~ 100개(노랑색): 'some'

                } else if ("few".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.red));       //2 ~ 30개(빨강색): 'few'

                } else {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.gray));      //0 ~ 1개 (회색): 'empty'
                }
                marker.setAnchor(new PointF(0.5f, 0.5f)); //마커 아이콘 x축, y축 정중앙이 해당 위치를 나타냄
                marker.setMap(naverMap);
                marker.setOnClickListener(this);
                markerList.add(marker);
            }
        }
    }

    private void resetMarkerList() {
        if (markerList != null && markerList.size() > 0) {
            for (Marker marker : markerList) {
                marker.setMap(null);
            }
            markerList.clear();
        }
    }

    @Override
    public void onCameraChange(int i, boolean b) { //카메라 이동 포착
        isCameraAnimated = true;
    }

    @Override
    public void onCameraIdle() { // 카메라 이동하면 다시 위/경도 지정
        if (isCameraAnimated) {
            LatLng mapCenter = naverMap.getCameraPosition().target;
            fetchStoreSale(mapCenter.latitude, mapCenter.longitude, 2000);
        }
    }

    @Override
    public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
    }

    @Override
    public boolean onClick(@NonNull Overlay overlay) {
        Marker marker = (Marker) overlay;
        infoWindow.open(marker);
        return false;
    }
}
