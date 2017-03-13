package com.cylan.jiafeigou.base.module;


import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseLongArray;

import com.cylan.entity.jniCall.JFGAccount;
import com.cylan.entity.jniCall.JFGDPMsg;
import com.cylan.entity.jniCall.JFGDPMsgCount;
import com.cylan.entity.jniCall.JFGDevice;
import com.cylan.entity.jniCall.JFGHistoryVideo;
import com.cylan.entity.jniCall.JFGShareListInfo;
import com.cylan.entity.jniCall.JFGVideo;
import com.cylan.entity.jniCall.RobotoGetDataRsp;
import com.cylan.ex.JfgException;
import com.cylan.jfgapp.jni.JfgAppCmd;
import com.cylan.jiafeigou.base.view.JFGSourceManager;
import com.cylan.jiafeigou.cache.LogState;
import com.cylan.jiafeigou.cache.db.impl.BaseDBHelper;
import com.cylan.jiafeigou.cache.db.module.Account;
import com.cylan.jiafeigou.cache.db.module.DPEntity;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.cache.db.view.DBAction;
import com.cylan.jiafeigou.cache.db.view.DBState;
import com.cylan.jiafeigou.cache.db.view.IDBHelper;
import com.cylan.jiafeigou.cache.video.History;
import com.cylan.jiafeigou.dp.DataPoint;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

import static com.cylan.jiafeigou.misc.JConstant.KEY_ACCOUNT;
import static com.cylan.jiafeigou.misc.JConstant.KEY_ACCOUNT_LOG_STATE;

/**
 * Created by yzd on 16-12-28.
 */

public class DataSourceManager implements JFGSourceManager {
    private final String TAG = getClass().getName();

    private IDBHelper dbHelper;
    /**
     * 只缓存当前账号下的数据,一旦注销将会清空所有的缓存,内存缓存方式
     */
    private Map<String, Device> mCachedDeviceMap = new LinkedHashMap<>();//和uuid相关的数据缓存
    private Account account;//账号相关的数据全部保存到这里面
    private static DataSourceManager mDataSourceManager;
    private ArrayList<JFGShareListInfo> shareList = new ArrayList<>();
    /**
     * 未读消息数
     */
    private HashMap<String, SparseLongArray> unreadMap = new HashMap<>();
    @Deprecated
    private boolean isOnline;
    private JFGAccount jfgAccount;

    private DataSourceManager() {
        dbHelper = BaseDBHelper.getInstance();
        initFromDB();
    }

    private void initFromDB() {
        dbHelper.getActiveAccount()
                .observeOn(Schedulers.io())
                .filter(account -> account != null)
                .map(dpAccount -> account = dpAccount)
                .flatMap(dpAccount -> dbHelper.queryDPMsgByUuid(null)
                        .observeOn(Schedulers.io())
                        .map(dpEntities -> {
                            JFGDPMsg msg;
                            for (DPEntity entity : dpEntities) {
                                msg = new JFGDPMsg(entity.getMsgId(), entity.getVersion());
                                msg.packValue = entity.getBytes();
                                dpAccount.setValue(msg);
                            }
                            return dpAccount;
                        })
                )
                .flatMap(account -> dbHelper.getAccountDevice(account.getAccount()))
                .flatMap(Observable::from)
                .map(device -> {
                    Device dev = create(device.getPid()).fill(device);
                    mCachedDeviceMap.put(device.getUuid(), dev);
                    return dev;
                })
                .flatMap(device -> dbHelper.queryDPMsgByUuid(device.uuid)
                        .observeOn(Schedulers.io())
                        .map(dpEntities -> {
                            JFGDPMsg msg;
                            for (DPEntity entity : dpEntities) {
                                msg = new JFGDPMsg(entity.getMsgId(), entity.getVersion());
                                msg.packValue = entity.getBytes();
                                device.setValue(msg);
                            }
                            return dpEntities;
                        })).subscribe();
    }

    public static DataSourceManager getInstance() {
        if (mDataSourceManager == null) {
            synchronized (DataSourceManager.class) {
                if (mDataSourceManager == null) {
                    mDataSourceManager = new DataSourceManager();
                }
            }
        }
        return mDataSourceManager;
    }

    @Deprecated
    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public <T extends Device> T getJFGDevice(String uuid) {
        Object o = mCachedDeviceMap.get(uuid);
        return (T) o;
    }

    @Override
    public List<Device> getAllJFGDevice() {
        List<Device> result = new ArrayList<>(mCachedDeviceMap.size());
        for (Map.Entry<String, ? extends DataPoint> entry : mCachedDeviceMap.entrySet()) {
            result.add(getJFGDevice(entry.getKey()));
        }
        return getValueWithAccountCheck(result);
    }

    public List<Device> getJFGDeviceByPid(int... pids) {
        if (pids == null) return null;

        List<Device> result = new ArrayList<>();
        for (Map.Entry<String, Device> device : mCachedDeviceMap.entrySet()) {
            for (int pid : pids) {
                if (device.getValue().pid == pid) {
                    result.add(getJFGDevice(device.getKey()));
                    break;
                }
            }
        }
        return getValueWithAccountCheck(result);
    }

    public List<String> getJFGDeviceUUIDByPid(int... pids) {
        if (pids == null) return null;
        List<String> result = new ArrayList<>();
        List<Device> devices = getJFGDeviceByPid(pids);
        for (Device device : devices) {
            result.add(device.uuid);
        }
        return getValueWithAccountCheck(result);
    }

    @Override
    public void cacheJFGDevices(com.cylan.entity.jniCall.JFGDevice... devices) {
        AppLogger.d("正在缓冲设备- start");
        Observable.just(devices)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(items -> {
                    Set<String> result;
                    for (JFGDevice device : items) {
                        mCachedDeviceMap.remove(device.uuid);
                    }
                    result = new HashSet<>(mCachedDeviceMap.keySet());
                    mCachedDeviceMap.clear();
                    AppLogger.d("已删除的设备数" + result.size());
                    return result;
                })
                .flatMap(items ->
                        items.size() == 0 ? Observable.just(devices) :
                                Observable.from(items).flatMap(uuid -> dbHelper.unBindDeviceWithConfirm(uuid)
                                        .filter(dev -> dev != null)
                                        .map(dev -> {
                                            RxBus.getCacheInstance().post(new RxEvent.DeviceUnBindedEvent(dev.getUuid()));
                                            return dev;
                                        })
                                        .buffer(items.size())
                                        .map(ret -> devices)
                                ))
                .flatMap(items -> dbHelper.updateDevice(devices))
                .map(dev -> {
                    Device dpDevice = create(dev.getPid()).fill(dev);
                    mCachedDeviceMap.put(dev.getUuid(), dpDevice);
                    ArrayList<JFGDPMsg> parameters = dpDevice.getQueryParameters(false);
                    AppLogger.d("正在同步设备信息:" + dpDevice.getUuid() + " " + new Gson().toJson(parameters) + "device:" + dpDevice);
                    try {
                        JfgCmdInsurance.getCmd().robotGetData(dpDevice.getUuid(), parameters, 1, false, 0);
                    } catch (JfgException e) {
                        e.printStackTrace();
                    }
                    return dpDevice;
                })
                .buffer(devices.length)
                .subscribe(items -> {
                    ArrayList<String> uuidList = new ArrayList<>();
                    for (Device device : items) {
                        if (!JFGRules.isShareDevice(device.uuid)) {
                            uuidList.add(device.uuid);
                        }
                    }
                    JfgCmdInsurance.getCmd().getShareList(uuidList);
                }, e -> {
                    AppLogger.d(e.getMessage());
                    e.printStackTrace();
                });
    }

    @Override
    public void cacheJFGAccount(com.cylan.entity.jniCall.JFGAccount account) {
        dbHelper.updateAccount(account)
                .doOnError(throwable -> AppLogger.e("err: " + throwable.getLocalizedMessage()))
                .doOnCompleted(() -> {
                    setJfgAccount(account);
                    if (jfgAccount != null)
                        setLoginState(new LogState(LogState.STATE_ACCOUNT_ON));
                    else {
                        AppLogger.e("jfgAccount is null");
                    }
                    RxBus.getCacheInstance().post(account);
                })
                .subscribe(act -> this.account = act);
    }

    //主动发起请求,来获取设备所有的属性
    @Override
    public void syncAllJFGDeviceProperty() {
        if (mCachedDeviceMap.size() == 0) return;
        ArrayList<String> uuidList = new ArrayList<>();
        for (Map.Entry<String, Device> entry : mCachedDeviceMap.entrySet()) {
            Device device = mCachedDeviceMap.get(entry.getKey());
            syncJFGDeviceProperty(entry.getKey());
            //非分享设备需要一些属性
            if (!JFGRules.isShareDevice(device)) {
                uuidList.add(device.uuid);
            }
        }
        /**
         * 设备分享列表
         */
        JfgCmdInsurance.getCmd().getShareList(uuidList);
//        syncAllJFGCameraWarnMsg(true);
    }

    /**
     * 很暴力地获取
     */
    public void syncDeviceUnreadCount() {
        for (Map.Entry<String, Device> entry : mCachedDeviceMap.entrySet()) {
            Device device = entry.getValue();
            if (JFGRules.isCamera(device.pid)) {
                ArrayList<Long> msgs = new ArrayList<>();
                try {
                    msgs.add(505L);
                    msgs.add(222L);
                    msgs.add(512L);
                    msgs.add(401L);
                    JfgCmdInsurance.getCmd().robotCountData(device.uuid, msgs, 0);
                } catch (Exception e) {
                    AppLogger.e("uuid is null: " + e.getLocalizedMessage());
                }
            }
        }
    }

    @Override
    public Observable<Account> logout() {
        return dbHelper.logout()
                .map(ret -> {
                    clear();
                    setLoginState(new LogState(LogState.STATE_ACCOUNT_OFF));
                    return ret;
                });
    }

    /**
     * 获取所有的报警消息{505,222}，1：保证有最新的报警消息，2.用于显示xx条新消息。
     *
     * @param ignoreShareDevice:忽略分享账号，一般都为true
     */
    @Override
    public void syncAllJFGCameraWarnMsg(boolean ignoreShareDevice) {
        for (Map.Entry<String, Device> entry : mCachedDeviceMap.entrySet()) {
            Device device = mCachedDeviceMap.get(entry.getKey());
            if (JFGRules.isShareDevice(device) && ignoreShareDevice) continue;
            syncJFGCameraWarn(entry.getKey(), false, 100);
        }
    }

    /**
     * 需要暴力操作。
     * 服务端任务太多，太杂，暂时实现不了。
     * 自力更生
     *
     * @param uuid
     */
    @Override
    public long syncJFGCameraWarn(String uuid, boolean asc, int count) {
        ArrayList<JFGDPMsg> list = MiscUtils.createGetCameraWarnMsgDp();
        try {
            return JfgCmdInsurance.getCmd().robotGetData(uuid, list, count, false, 0);
        } catch (JfgException e) {
            AppLogger.e("uuid is null");
            return 0L;
        }
    }

    @Override
    public void queryHistory(String uuid) {
        try {
            JfgCmdInsurance.getCmd().getVideoList(uuid);
        } catch (JfgException e) {
            AppLogger.e("uuid is null: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void cacheHistoryDataList(JFGHistoryVideo historyVideo) {
        History.getHistory().cacheHistoryDataList(historyVideo);
    }

    @Override
    public ArrayList<JFGVideo> getHistoryList(String uuid) {
        return History.getHistory().getHistoryList(uuid);
    }

    @Override
    public void cacheUnreadCount(long seq, String uuid, ArrayList<JFGDPMsgCount> unreadList) {

        SparseLongArray array = unreadMap.get(uuid);
        if (array == null) {
            array = new SparseLongArray();
        }
        if (unreadList != null) {
            for (JFGDPMsgCount count : unreadList) {
                array.put(count.id, count.count);
            }
            unreadMap.put(uuid, array);
        }
        RxBus.getCacheInstance().post(new RxEvent.UnreadCount(uuid, seq, unreadList));
    }

    @Override
    public Pair<Integer, Long> getUnreadCount(String uuid, long... ids) {
        if (unreadMap != null && ids != null && ids.length > 0) {
            SparseLongArray array = unreadMap.get(uuid);
            if (array != null) {
                int count = 0;
                long version = 0;
                for (long id : ids) {
                    count += array.get((int) id);
                    try {
                        long v = MiscUtils.getVersion(getValue(uuid, id), false);
                        version = version > v ? version : v;
                    } catch (Exception e) {
                        AppLogger.e("err: " + e.getLocalizedMessage());
                    }
                }
                return new Pair<>(count, version);
            }
        }
        return null;
    }

    @Override
    public void clear() {
        if (mCachedDeviceMap != null) mCachedDeviceMap.clear();
        isOnline = false;
        account = null;
        jfgAccount = null;
        this.jfgAccount = null;
        this.account = null;
        if (unreadMap != null) unreadMap.clear();
        if (shareList != null) shareList.clear();
    }

    @Override
    public void clearUnread(String uuid, long... ids) {
        try {
            ArrayList<Long> list = new ArrayList<>();
            if (ids != null && ids.length > 0) {
                for (long id : ids) {
                    list.add(id);
                }
                JfgCmdInsurance.getCmd().robotCountDataClear(uuid, list, 0);
                boolean result = unreadMap.remove(uuid) != null;
                AppLogger.d("clear unread count：" + result);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public <T extends DataPoint> List<T> getValueBetween(String uuid, long msgId, long startVersion, long endVersion) {
        List<T> result = new ArrayList<>();
        Object origin = getValue(uuid, msgId);
        if (origin == null)
            return result;
        if (origin instanceof DpMsgDefine.DPSet) {
            DpMsgDefine.DPSet<T> set = (DpMsgDefine.DPSet<T>) origin;
            if (set.value == null) return result;
            for (T t : set.value) {
                if (t.dpMsgVersion >= startVersion && t.dpMsgVersion < endVersion) {
                    result.add(t);
                }
            }
            return result;
        } else return null;
    }

    public void syncJFGDeviceProperty(String uuid) {
        if (TextUtils.isEmpty(uuid) || account == null) return;
        Device device = mCachedDeviceMap.get(uuid);
        if (device != null) {
            ArrayList<JFGDPMsg> parameters = device.getQueryParameters(false);
            AppLogger.d("syncJFGDeviceProperty: " + uuid + " " + new Gson().toJson(parameters));
            try {
                JfgCmdInsurance.getCmd().robotGetData(uuid, parameters, 1, false, 0);
            } catch (JfgException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Account getAJFGAccount() {
        return account;
    }

    @Override
    public JFGAccount getJFGAccount() {
        return jfgAccount;
    }

    @Override
    public void cacheRobotoGetDataRsp(RobotoGetDataRsp dataRsp) {
        Observable.from(dataRsp.map.entrySet())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(set -> Observable.from(set.getValue())
                        .flatMap(msg -> dbHelper.saveDPByte(dataRsp.identity, msg.version, (int) msg.id, msg.packValue)
                                .map(entity -> {
                                    Device device = mCachedDeviceMap.get(dataRsp.identity);
                                    boolean change = false;
                                    if (device != null) {//优先尝试写入device中
                                        change = device.setValue(msg, dataRsp.seq);
                                    }
                                    if (account != null) {//到这里说明无法将数据写入device中,则写入到account中
                                        change |= account.setValue(msg, dataRsp.seq);
                                        if (change)
                                            account.dpMsgVersion = System.currentTimeMillis();
                                    }
                                    return entity;
                                })
                        ))
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> {
                    syncDeviceUnreadCount();
                    RxBus.getCacheInstance().post(dataRsp);
                })
                .subscribe();
    }

    @Override
    public void cacheRobotoSyncData(boolean b, String s, ArrayList<JFGDPMsg> arrayList) {
        Observable.from(arrayList)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(msg -> dbHelper.saveOrUpdate(s, msg.version, (int) msg.id, msg.packValue, DBAction.SAVED, DBState.SUCCESS, null).map(ret -> msg))
                .subscribe(msg -> {
                    Device device = mCachedDeviceMap.get(s);
                    if (device != null) {
                        device.setValue(msg);
                    }
                }, Throwable::printStackTrace, () -> {
                    ArrayList<Long> updateIdList = new ArrayList<>();
                    for (JFGDPMsg msg : arrayList) {
                        updateIdList.add(msg.id);
                    }
                    RxBus.getCacheInstance().postSticky(new RxEvent.DeviceSyncRsp().setUuid(s, updateIdList));
                });
    }

    @Override
    public <T extends DataPoint> T getValue(String uuid, long msgId) {
        return getValue(uuid, msgId, -1);
    }

    public <T extends DataPoint> T getValue(String uuid, long msgId, long seq) {
        T result = null;
        Device device = mCachedDeviceMap.get(uuid);
        if (device != null) {
            //这里优先从根据UUID从device中获取数据
            result = device.getValue(msgId, seq);
        }
        if (result == null && account != null) {
            //如果无法从device中获取值,则从account中获取
            result = account.getValue(msgId, seq);
        }
        return getValueWithAccountCheck(result);
    }

    public <T> T getValueWithAccountCheck(T value) {
        return value;
    }


    @Override
    public void cacheShareList(ArrayList<JFGShareListInfo> arrayList) {
        if (shareList == null) shareList = new ArrayList<>();
        shareList.clear();
        shareList.addAll(arrayList);
        Log.d("shareList", "shareList: " + new Gson().toJson(shareList));
        RxBus.getCacheInstance().post(new RxEvent.GetShareListRsp());
    }

    @Override
    public boolean isDeviceSharedTo(String uuid) {
        int size = shareList == null ? 0 : shareList.size();
        for (int i = 0; i < size; i++) {
            JFGShareListInfo info = shareList.get(i);
            if (TextUtils.equals(uuid, info.cid)) {
                return info.friends != null && info.friends.size() > 0;
            }
        }
        return false;
    }


    @Override
    public ArrayList<JFGShareListInfo> getShareList() {
        return shareList;
    }

    public void setLoginState(LogState loginState) {
        PreferencesUtils.putInt(KEY_ACCOUNT_LOG_STATE, loginState.state);
        if (loginState.state == LogState.STATE_NONE) {
//            shareList.clear();
            setJfgAccount(null);
        } else if (loginState.state == LogState.STATE_ACCOUNT_OFF) {
//            shareList.clear();
        } else {

        }
        AppLogger.i("logState update: " + loginState.state);
    }

    public void setJfgAccount(JFGAccount jfgAccount) {
        this.jfgAccount = jfgAccount;
        AppLogger.i("setJfgAccount:" + (jfgAccount == null));
        if (jfgAccount != null) {
            PreferencesUtils.putString(KEY_ACCOUNT, new Gson().toJson(jfgAccount));
            RxBus.getCacheInstance().postSticky(new RxEvent.GetUserInfo(jfgAccount));
        } else PreferencesUtils.putString(KEY_ACCOUNT, "");
    }

    @Override
    public boolean updateJFGDevice(Device device) {
        dbHelper.updateDevice(device).subscribe();
        return true;
    }

    @Override
    public <T extends DataPoint> boolean updateValue(String uuid, T value, int msgId) throws
            IllegalAccessException {
        Device device = getJFGDevice(uuid);
        if (device == null) {
            AppLogger.e("device is null:" + uuid);
            return false;
        }
        boolean update = device.updateValue(msgId, value);
        ArrayList<JFGDPMsg> list = new ArrayList<>();
        JFGDPMsg msg = new JFGDPMsg(msgId, System.currentTimeMillis());
        msg.packValue = value.toBytes();
        list.add(msg);
        try {
            JfgAppCmd.getInstance().robotSetData(uuid, list);
            return true;
        } catch (JfgException e) {
            return false;
        }
    }

    @Override
    public boolean deleteByVersions(String uuid, long id, ArrayList<Long> versions) {
        return false;
    }

    public int getLoginState() {
        JFGAccount account = this.getJFGAccount();
        if (account == null || TextUtils.isEmpty(account.getAccount())) {
            return 0;//无账号
        } else {
            return PreferencesUtils.getInt(KEY_ACCOUNT_LOG_STATE, 0);
        }
    }

    /**
     * 不要使用switch case
     *
     * @param pid
     * @return
     */
    private Device create(int pid) {
        Device result = null;
        if (JFGRules.isCamera(pid))
            result = new JFGCameraDevice();
        else if (JFGRules.isBell(pid))
            result = new JFGDoorBellDevice();
        else
            result = new Device();
        return result;
    }
}
