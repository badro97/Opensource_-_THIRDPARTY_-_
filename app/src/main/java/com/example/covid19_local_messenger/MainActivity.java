package com.example.covid19_local_messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.PointF;
import android.os.Bundle;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
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


public class MainActivity extends AppCompatActivity implements NaverMap.OnMapClickListener, Overlay.OnClickListener, OnMapReadyCallback, NaverMap.OnCameraChangeListener, NaverMap.OnCameraIdleListener{


    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private InfoWindow infoWindow;
    private List<Marker> markerList = new ArrayList<Marker>();
    private boolean isCameraAnimated = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mapFragment = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        //현재 위치 추적 (권한)
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);

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
    }


    @Override //위치 권한
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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

                //마스크 잔량 별 색 표시
                if ("plenty".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_green));     //100개 ~ (녹색): 'plenty'

                } else if ("some".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_yellow));    //30 ~ 100개(노랑색): 'some'

                } else if ("few".equalsIgnoreCase(store.remain_stat)) {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_red));       //2 ~ 30개(빨강색): 'few'

                } else {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.marker_gray));      //0 ~ 1개 (회색): 'empty'
                }

                marker.setAnchor(new PointF(0.5f, 0.5f)); //마커 아이콘 x축, y축 정중앙이 해당 위치를 나타냄
                marker.setMap(naverMap);
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
        return false;
    }
}
