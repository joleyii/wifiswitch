package com.shine.wifiswitch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by 123 on 2017/5/15.
 */

public class WifiService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    int pingNumber = 0;
    int enablingNumber = 0;
    int disablingNumber = 0;
    int lastStatus = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();

        Flowable.interval(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        checkNetCardState();
                    }
                });
        File file = new File("/extdata/work/show/system/network.ini");
        if (file.exists()) {
            Flowable.interval(15, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            httpSend();
                        }
                    });
        } else {
            Log.d("WifiService", "file not exits");
        }

    }

    int readSignTick = 60;

    private void httpSend() {
        if (readSignTick == 60) {
            readSignTick = 0;
            Log.d("getinfo", "getinfo");
            getinfo();
        }
        readSignTick += 1;
        if (!TextUtils.isEmpty(checkserver) && checkserver.equals("1") && checkWifienable()) {
            getHttp(serverAddress);
        }
    }

    String checkserver;
    String serverAddress = "";

    public String getinfo() {
        String serverFile = "/extdata/work/show/system/network.ini";
        String readFile = readFile(serverFile);
        if (!TextUtils.isEmpty(readFile)) {
            if (readFile.contains("checkserver=")) {
                checkserver = readFile.substring(readFile.indexOf("checkserver=") + 12,
                        readFile.indexOf("checkserver=") + 13);
            } else {
                checkserver = "0";
            }
            String ip = "";
            if (readFile.contains("checkserver=")) {
                ip = readFile.substring(readFile.indexOf("commuip=") + 8,
                        readFile.indexOf("checkurl") - 1);
            }
            String port = "";
            if (readFile.contains("checkserver=")) {
                port = readFile.substring(readFile.indexOf("checkurl=") + 9,
                        readFile.indexOf("checkserver") - 1);
            }
            serverAddress = ip + port;
        }
        return serverAddress;
    }

    public void checkNetCardState() {
        if (mWifiManager.getWifiState() == 0) {
            disablingNumber = 0;
            if (lastStatus == 0) {
                enablingNumber += 1;
                if (enablingNumber == 3) {
                    closeAndOpenWifi();
                    enablingNumber = 0;
                }
//                Log.d("MyWifiStatus", "网卡正在关闭");
                Log.d("MyWifiStatus", "disabling");
            } else {
                lastStatus = 0;
                enablingNumber = 0;
            }
        } else if (mWifiManager.getWifiState() == 1) {
            enablingNumber = 0;
            disablingNumber = 0;
            lastStatus = 1;
//            Log.d("MyWifiStatus", "网卡已经关闭");
            Log.d("MyWifiStatus", "closed");
        } else if (mWifiManager.getWifiState() == 2) {
            enablingNumber = 0;
            if (lastStatus == 2) {
                disablingNumber += 1;
                if (disablingNumber == 3) {
                    closeAndOpenWifi();
                    disablingNumber = 0;
                }
            } else {
                lastStatus = 2;
                disablingNumber = 0;
            }
//            Log.d("MyWifiStatus", "网卡正在打开");
            Log.d("MyWifiStatus", "opening");
        } else if (mWifiManager.getWifiState() == 3) {
            lastStatus = 3;
            disablingNumber = 0;
            enablingNumber = 0;
//            Log.d("MyWifiStatus", "网卡已经打开");
            Log.d("MyWifiStatus", "opened");
        } else {
            lastStatus = -1;
            disablingNumber = 0;
            enablingNumber = 0;
//            Log.d("MyWifiStatus", "没有获取到状态");
            Log.d("MyWifiStatus", "wifinoinfo");
        }
    }

    public boolean checkWifienable() {
        if (mWifiManager.getWifiState() == 3) {
//            Log.d("MyWifiStatus", "网卡已经打开");
            Log.d("MyWifiStatus", "opened");
            return true;
        } else {
            return false;
        }
    }

    public boolean checkWifidisenable() {
        if (mWifiManager.getWifiState() == 1) {
            return true;
        } else {
            return false;
        }
    }

    private static void append(StringBuffer stringBuffer, String text) {
        if (stringBuffer != null) {
            stringBuffer.append(text + "\n");
        }
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

    public void openNetCard() {
        if (!mWifiManager.isWifiEnabled()) {
            Log.d("openNetCard", "openNetCard");
            mWifiManager.setWifiEnabled(true);
        }
    }

    public void closeNetCard() {
        if (mWifiManager.isWifiEnabled()) {
            Log.d("closeNetCard", "closeNetCard");
            mWifiManager.setWifiEnabled(false);
        }
    }

    public void closeAndOpenWifi() {
        closeNetCard();
        int time = 0;
        while (!checkWifidisenable()) {
            Log.d("closeAndOpenWifi", "checkWifidisenable");
            if (time == 10) {
                break;
            }
            try {
                Thread.sleep(500);
                time += 1;
            } catch (InterruptedException e) {
                openNetCard();
                e.printStackTrace();
            }
        }
        openNetCard();
    }

    public void getHttp(final String url) {
        try {
            Flowable
                    .create(new FlowableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(FlowableEmitter<Boolean> e) throws Exception {
                            try {
//                                String serverURL = "http://" + ip + "/index.php/Pda/Tools/ping";
                                String serverURL = "http://" + url;
                                Log.d("serverURL", serverURL + "");
                                HttpURLConnection connection = null;
                                URL url = new URL(serverURL);
                                connection = (HttpURLConnection) url.openConnection();
                                // 设置请求方法，默认是GET
                                connection.setRequestMethod("GET");
                                // 设置字符集
                                connection.setRequestProperty("Charset", "UTF-8");
                                // 设置文件类型
                                connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                                // 设置请求参数，可通过Servlet的getHeader()获取
                                connection.setRequestProperty("Cookie", "AppName=" + URLEncoder.encode("你好", "UTF-8"));
                                // 设置自定义参数
                                connection.setRequestProperty("MyProperty", "this is me!");
                                if (connection.getResponseCode() == 200) {

                                    e.onNext(true);
                                } else {
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
                            Log.d("getHttpaBoolean", aBoolean + "");
                            if (!aBoolean) {
                                pingNumber += 1;
                                if (pingNumber == 3) {
                                    Log.d("getHttpcloseAndOpenWifi", "closeAndOpenWifi");
                                    closeAndOpenWifi();
                                    pingNumber = 0;
                                }
                            } else {
                                pingNumber = 0;
                            }
                        }
                    });
        } catch (Exception e) {

        }
    }


    public static boolean ping(String host, int pingCount, StringBuffer stringBuffer) {
        String line = null;
        Process process = null;
        BufferedReader successReader = null;
        String command = "ping -c " + pingCount + " " + host;
        boolean isSuccess = false;
        try {
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                Log.e("MyPing", "ping fail:process is null.");
                append(stringBuffer, "ping fail:process is null.");
                return false;
            }
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = successReader.readLine()) != null) {
                Log.i("MyPing", line);
                append(stringBuffer, line);
            }
            int status = process.waitFor();
            if (status == 0) {
                Log.i("MyPing", "exec cmd success:" + command);
                append(stringBuffer, "exec cmd success:" + command);
                isSuccess = true;
            } else {
                Log.e("MyPing", "exec cmd fail.");
                append(stringBuffer, "exec cmd fail.");
                isSuccess = false;
            }
            Log.i("MyPing", "exec finished.");
            append(stringBuffer, "exec finished.");
        } catch (IOException e) {
            Log.e("MyPing", String.valueOf(e));
        } catch (InterruptedException e) {
            Log.e("MyPing", String.valueOf(e));
        } finally {
            Log.i("MyPing", "ping exit.");
            if (process != null) {
                process.destroy();
            }
            if (successReader != null) {
                try {
                    successReader.close();
                } catch (IOException e) {
                    Log.e("MyPing", String.valueOf(e));
                }
            }
        }
        return isSuccess;
    }
}
