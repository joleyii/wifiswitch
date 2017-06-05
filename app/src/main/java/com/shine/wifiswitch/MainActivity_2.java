package com.shine.wifiswitch;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MainActivity_2 extends AppCompatActivity {
    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d("BootCompletedReceiver", "MyBootCompletedReceiveronReceive");
//        Intent i = new Intent(this, WifiService2.class);
//        startService(i);
//        getHttp("172.168.66.92:80");
//        String readFile = "checkserver=1";
//        String serverAddress = readFile.substring(readFile.indexOf("checkserver=") + 12,
//                readFile.indexOf("checkserver=") + 13);

        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
        List<ScanResult> scanResults = mWifiManager.getScanResults();//搜索到的设备列表
        Log.d("scanResults", scanResults.size() + "scanResultsscanResults");

    }

    public void getHttp(final String ip) {
        try {
            Flowable
                    .create(new FlowableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(FlowableEmitter<Boolean> e) throws Exception {
                            try {
                                String serverURL = "http://" + ip + "/index.php/Pda/Tools/ping";
                                HttpGet httpRequest = new HttpGet(serverURL);// 建立http get联机
                                HttpResponse httpResponse = null;// 发出http请求
                                httpResponse = new DefaultHttpClient().execute(httpRequest);
                                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                    String result = EntityUtils.toString(httpResponse.getEntity());// 获取相应的字符串
                                    Log.d("MyhttpResponse", "200");
                                    e.onNext(true);
                                } else {
                                    Log.d("MyhttpResponse", "other");
                                    e.onNext(false);
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                e.onNext(false);
                            }
                        }
                    }, BackpressureStrategy.BUFFER)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) throws Exception {
                            Log.d("aBooleanaBoolean", aBoolean + "");
                            if (!aBoolean) {
                                Log.d("", "");
                            } else {
                                Log.d("", "");
                            }
                        }
                    });
        } catch (Exception e) {

        }
    }

    public static String getServerAddress() {
        String SERVER_FILE = "/extdata/work/show/system/network.ini";
//        String SERVER_FILE = "/sdcard/network";
        String readFile = readFile(SERVER_FILE);
        String serverAddress = "";
        if (!TextUtils.isEmpty(readFile)) {
            serverAddress = readFile.substring(readFile.indexOf("commuip=") + 8,
                    readFile.indexOf("commuport="));
        }
        Log.d("serverAddress", serverAddress);
        return serverAddress;
    }

    public static String readFile(String filePath) {
        StringBuilder sb = new StringBuilder();
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
