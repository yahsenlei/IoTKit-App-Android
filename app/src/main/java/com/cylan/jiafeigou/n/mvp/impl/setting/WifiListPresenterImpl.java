package com.cylan.jiafeigou.n.mvp.impl.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.cache.pool.GlobalDataProxy;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.misc.ScanResultListFilter;
import com.cylan.jiafeigou.misc.bind.UdpConstant;
import com.cylan.jiafeigou.n.mvp.contract.setting.WifiListContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxHelper;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.udpMsgPack.JfgUdpMsg;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by cylan-hunt on 17-2-12.
 */

public class WifiListPresenterImpl extends AbstractPresenter<WifiListContract.View>
        implements WifiListContract.Presenter {
    private WifiManager wifiManager;
    private Network network;
    private String uuid;

    public WifiListPresenterImpl(WifiListContract.View view, String uuid) {
        super(view);
        this.uuid = uuid;
        view.setPresenter(this);
        wifiManager = (WifiManager) ContextUtils.getContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected Subscription[] register() {
        return super.register();
    }

    @Override
    public void startScan() {
        if (wifiManager != null) wifiManager.startScan();
    }

    @Override
    public void sendWifiInfo(String ssid, String pwd, int security) {
        String mac = getLatestMac();
        if (TextUtils.isEmpty(mac)) {
            RxBus.getCacheInstance().toObservable(JfgUdpMsg.FPingAck.class)
                    .subscribeOn(Schedulers.newThread())
                    .throttleFirst(500, TimeUnit.MILLISECONDS)
                    .subscribe((JfgUdpMsg.FPingAck fPingAck) -> {
                        if (TextUtils.equals(uuid, fPingAck.cid)) {
                            sendInfo(ssid, pwd, security, fPingAck.mac);
                            AppLogger.i(String.format(Locale.getDefault(), "send info: %s_%s_%s_%s", ssid, pwd, mac, security));
                        }
                    }, (Throwable throwable) -> AppLogger.e("err:" + throwable.getLocalizedMessage()));
            return;
        }
        sendInfo(ssid, pwd, security, mac);
    }

    private void sendInfo(String ssid, String pwd, int security, String mac) {
        JfgUdpMsg.DoSetWifi setWifi = new JfgUdpMsg.DoSetWifi(uuid,
                mac,
                ssid, pwd);
        setWifi.security = security;
        //发送wifi配置
        try {
            JfgCmdInsurance.getCmd().sendLocalMessage(UdpConstant.IP,
                    UdpConstant.PORT,
                    setWifi.toBytes());
            JfgCmdInsurance.getCmd().sendLocalMessage(UdpConstant.IP,
                    UdpConstant.PORT,
                    setWifi.toBytes());
            AppLogger.i(TAG + new Gson().toJson(setWifi));
        } catch (JfgException e) {
            e.printStackTrace();
        }
    }

    /**
     * 每次fping都是为了得到最新的mac
     */
    private String getLatestMac() {
        String mac = GlobalDataProxy.getInstance().getValue(uuid, DpMsgMap.ID_202_MAC, "");
        if (JConstant.MAC_REG.matcher(mac).find()) {
            AppLogger.i("get mac from local: " + mac);
            return mac;
        }
        return "";
    }

    @Override
    public void start() {
        super.start();
        try {
            if (network == null) {
                network = new Network();
                final IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
                filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                ContextUtils.getContext().registerReceiver(network, filter);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void stop() {
        super.stop();
        try {
            if (network != null) {
                ContextUtils.getContext().unregisterReceiver(network);
                network = null;
            }
        } catch (Exception e) {

        }
    }

    /**
     * wifi列表
     */
    private void updateWifiResults(List<ScanResult> scanResults) {
        Observable.just(scanResults)
                //别那么频繁
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .filter((List<ScanResult> s) -> {
                    //非空返回,如果空,下面的map是不会有结果.
                    return getView() != null;
                })
                .map((List<ScanResult> results) -> {
                    ArrayList<ScanResult> list = ScanResultListFilter.extractPretty(results, false);
                    Collections.sort(list, (ScanResult lhs, ScanResult rhs) -> {
                        int left = WifiManager.calculateSignalLevel(lhs.level, 5);
                        int right = WifiManager.calculateSignalLevel(rhs.level, 5);
                        return right - left;
                    });
                    return list;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((List<ScanResult> s) -> {
                    getView().onResults((ArrayList<ScanResult>) s);
                }, new RxHelper.EmptyException("resultList call"));
    }

    private class Network extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(action, WifiManager.RSSI_CHANGED_ACTION)
                    || TextUtils.equals(action, WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                updateWifiResults(wifiManager.getScanResults());
            } else if (TextUtils.equals(action, ConnectivityManager.CONNECTIVITY_ACTION)) {
            } else if (TextUtils.equals(action, WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            }
        }
    }
}
