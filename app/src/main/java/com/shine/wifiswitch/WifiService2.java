package com.shine.wifiswitch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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

public class WifiService2 extends Service {
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
        Log.d("startServiceMy", "startServiceMy");
        timer.schedule(task, 120000);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                startServiceMy();
            }
            super.handleMessage(msg);
        }
    };

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            // 需要做的事:发送消息
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    private void startServiceMy() {
        Log.d("startServiceMy", "startServiceMy");
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
        Flowable.interval(30, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        checkNetEnable();
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
            getHttp();
        }
    }


    String checkFile = "/extdata/work/show/system/shineWifiMonitor.ini";
    String serverFile = "/extdata/work/show/system/network.ini";
    String checkserver;
    String serverAddress;
    String checkurl;
    String commuip;

    public void getinfo() {
        commuip = getStringFromFIle(serverFile, "commuip=");
        Log.d("commuip", commuip + "");
        checkurl = getStringFromFIle(checkFile, "checkurl=");
        Log.d("checkurl", checkurl + "");
        checkserver = getStringFromFIle(checkFile, "checkserver=");
        serverAddress = commuip + checkurl;
        Log.d("getinfocheckserver", checkserver + "；；；；；" + commuip + ";;;" + checkurl);
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

    int currentEnable;

    public void checkNetEnable() {
        if (mWifiManager.getWifiState() == 0) {
            currentEnable = 0;
            Log.d("MyWifiStatus", "disabling");
        } else if (mWifiManager.getWifiState() == 1) {
            currentEnable = 0;
            Log.d("MyWifiStatus", "closed");
        } else if (mWifiManager.getWifiState() == 2) {
            currentEnable = 0;
            Log.d("MyWifiStatus", "opening");
        } else if (mWifiManager.getWifiState() == 3) {
            //判断如果没有设别连接 三次之后关闭在重新打开
            List<ScanResult> scanResults = mWifiManager.getScanResults();//搜索到的设备列表
            Log.d("scanResults", scanResults.size() + "scanResultsscanResults");
            if (scanResults.size() == 0) {
                Log.d("scanResults", "scanResultsscanResults");
                closeAndOpenWifi();
            }
            Log.d("MyWifiStatus", "opened");
        } else {
            currentEnable = 0;
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

    public String getStringFromFIle(String filePath, String key) {
        String sb = "";
        try {
            File file = new File(filePath);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (line.contains(key)) {
                        sb = line.substring(line.indexOf(key) + key.length(),
                                line.length());
                    }
                }
                br.close();
                isr.close();
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("sb", sb);
        return sb;
    }

    public void openNetCard() {
//        if (!mWifiManager.isWifiEnabled()) {
        Log.d("openNetCard", "openNetCard");
        mWifiManager.setWifiEnabled(true);
//        }
    }

    public void closeNetCard() {
//        if (mWifiManager.isWifiEnabled()) {
        Log.d("closeNetCard", "closeNetCard");
        mWifiManager.setWifiEnabled(false);
//        }
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

    public void getHttp() {
        try {
            Flowable
                    .create(new FlowableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(FlowableEmitter<Boolean> e) throws Exception {
                            try {
//                                String serverURL = "http://" + ip + "/index.php/Pda/Tools/ping";
                                String serverURL = "http://" + serverAddress;
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
