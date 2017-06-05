package com.shine.wifiswitch;

import android.Manifest;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity {
    private WifiInfo mWifiInfo;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , Manifest.permission.RECORD_AUDIO
                        , Manifest.permission.INTERNET
                        , Manifest.permission.ACCESS_WIFI_STATE
                        , Manifest.permission.ACCESS_NETWORK_STATE
                        , Manifest.permission.CHANGE_WIFI_STATE
                        , Manifest.permission.CHANGE_NETWORK_STATE
                )
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        setWifi();
                        Flowable.interval(5, TimeUnit.SECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) throws Exception {
                                        checkNetCardState();
                                    }
                                });
                    }
                });
        findViewById(R.id.tv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNetCard();
            }
        });
        findViewById(R.id.tv_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNetCard();
            }
        });
    }

    private void setWifi() {
        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
        textView = (TextView) findViewById(R.id.tv_status);
    }

    TextView textView;

    public void checkNetCardState() {
        if (mWifiManager.getWifiState() == 0) {
            textView.setText("网卡正在关闭");
        } else if (mWifiManager.getWifiState() == 1) {
            textView.setText("网卡已经关闭");
        } else if (mWifiManager.getWifiState() == 2) {
            textView.setText("网卡正在打开");
        } else if (mWifiManager.getWifiState() == 3) {
            textView.setText("网卡已经打开");
        } else {
            textView.setText("没有获取到状态");
        }
    }

    public void openNetCard() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    public void closeNetCard() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }


}
