package com.cylan.jiafeigou.n.mvp.impl.bell;

import android.text.TextUtils;
import android.util.Pair;

import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.base.wrapper.BasePresenter;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.dp.DpUtils;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.n.mvp.contract.bell.BellDetailContract;
import com.cylan.jiafeigou.n.mvp.model.BeanBellInfo;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.rx.RxUiEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.google.gson.Gson;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by cylan-hunt on 16-8-3.
 */
public class BellDetailSettingPresenterImpl extends BasePresenter<BellDetailContract.View>
        implements BellDetailContract.Presenter {
    private BeanBellInfo beanBellInfo;

    @Override
    protected void onRegisterSubscription(CompositeSubscription subscriptions) {
        super.onRegisterSubscription(subscriptions);
        subscriptions.add(onBellInfoSubscription());
    }

    @Override
    public void onSetContentView() {
        super.onSetContentView();
        mView.onDeviceSyncRsp(mSourceManager.getJFGDevice(mUUID));

    }

    private Subscription onBellInfoSubscription() {
        //查询设备列表
        return RxBus.getCacheInstance().toObservableSticky(RxUiEvent.BulkDeviceListRsp.class)
                .subscribeOn(Schedulers.computation())
                .filter(list -> list != null && list.allDevices != null)
                .flatMap(new Func1<RxUiEvent.BulkDeviceListRsp, Observable<DpMsgDefine.DpWrap>>() {
                    @Override
                    public Observable<DpMsgDefine.DpWrap> call(RxUiEvent.BulkDeviceListRsp list) {
                        for (DpMsgDefine.DpWrap wrap : list.allDevices) {
                            if (wrap.baseDpDevice == null)
                                continue;
                            if (TextUtils.equals(wrap.baseDpDevice.uuid, mUUID)) {
                                return Observable.just(wrap);
                            }
                        }
                        return null;
                    }
                })
                .filter(dpWrap -> dpWrap != null && dpWrap.baseDpDevice != null)
                .flatMap(new Func1<DpMsgDefine.DpWrap, Observable<BeanBellInfo>>() {
                    @Override
                    public Observable<BeanBellInfo> call(DpMsgDefine.DpWrap dpWrap) {
                        BeanBellInfo info = new BeanBellInfo();
                        info.convert(dpWrap.baseDpDevice, dpWrap.baseDpMsgList);
                        beanBellInfo = info;
                        AppLogger.i("BeanCamInfo: " + new Gson().toJson(info));
                        return Observable.just(info);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(beanBellInfo1 -> {
                    //刷新
                    mView.onSettingInfoRsp(beanBellInfo1);
                });
    }

    @Override
    public BeanBellInfo getBellInfo() {
        return beanBellInfo;
    }

    @Override
    public void saveBellInfo(BeanBellInfo info, int id) {
        this.beanBellInfo = info;
        Observable.just(new Pair<>(info, id))
                .subscribeOn(Schedulers.io())
                .subscribe(beanCamInfoIntegerPair -> {
                    int id1 = beanCamInfoIntegerPair.second;
                    RxEvent.JFGAttributeUpdate update = new RxEvent.JFGAttributeUpdate();
                    update.uuid = beanBellInfo.deviceBase.uuid;
                    if (id1 == DpMsgMap.ID_2000003_BASE_ALIAS)
                        update.o = beanCamInfoIntegerPair.first.deviceBase.alias;
                    else update.o = beanCamInfoIntegerPair.first.getObject(id1);
                    update.msgId = id1;
                    update.version = System.currentTimeMillis();
                    RxBus.getCacheInstance().post(update);
                    if (id1 == DpMsgMap.ID_2000003_BASE_ALIAS) {
                        try {
                            JfgCmdInsurance.getCmd().setAliasByCid(beanBellInfo.deviceBase.uuid,
                                    beanBellInfo.deviceBase.alias);
                        } catch (JfgException e) {
                            e.printStackTrace();
                        }
                        AppLogger.i("setDevice alias: " + new Gson().toJson(beanBellInfo));
                        return;
                    }
                    try {
                        JfgCmdInsurance.getCmd().robotSetData(beanBellInfo.deviceBase.uuid,
                                DpUtils.getList(id1,
                                        beanCamInfoIntegerPair.first.getByte(id1)
                                        , System.currentTimeMillis()));
                    } catch (JfgException e) {
                        e.printStackTrace();
                    }
                    AppLogger.i("setDevice camInfo: " + new Gson().toJson(beanBellInfo));
                });
    }

}
