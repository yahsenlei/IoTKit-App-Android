package com.cylan.jiafeigou.n.base;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.Log;

import com.cylan.jiafeigou.DaemonReceiver1;
import com.cylan.jiafeigou.DaemonReceiver2;
import com.cylan.jiafeigou.DaemonService1;
import com.cylan.jiafeigou.DaemonService2;
import com.cylan.jiafeigou.base.injector.component.AppComponent;
import com.cylan.jiafeigou.base.injector.component.DaggerAppComponent;
import com.cylan.jiafeigou.base.injector.module.AppModule;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.n.engine.DataSourceService;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.OptionsImpl;
import com.cylan.jiafeigou.support.block.impl.BlockCanary;
import com.cylan.jiafeigou.support.block.impl.BlockCanaryContext;
import com.cylan.jiafeigou.support.block.log.PerformanceUtils;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.support.stat.BugMonitor;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.HandlerThreadUtils;
import com.cylan.jiafeigou.utils.PackageUtils;
import com.cylan.jiafeigou.utils.PathGetter;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.cylan.jiafeigou.utils.ProcessUtils;
import com.danikula.videocache.HttpProxyCacheServer;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.push.HuaweiPush;
import com.marswin89.marsdaemon.DaemonClient;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.squareup.leakcanary.LeakCanary;

import javax.inject.Inject;

import permissions.dispatcher.PermissionUtils;

/**
 * Created by hunt on 16-5-14.
 */
public class BaseApplication extends MultiDexApplication implements Application.ActivityLifecycleCallbacks, HuaweiApiClient.ConnectionCallbacks, HuaweiApiClient.OnConnectionFailedListener {

    private static final String TAG = "BaseApplication";

    private DaemonClient mDaemonClient;
    private HuaweiApiClient client;

    private static AppComponent appComponent;
    @Inject
    protected HttpProxyCacheServer proxy;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mDaemonClient = new DaemonClient(createDaemonConfigurations());
        mDaemonClient.onAttachBaseContext(base);
    }


    private DaemonConfigurations createDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                getPackageName() + ":process1",
                DaemonService1.class.getCanonicalName(),
                DaemonReceiver1.class.getCanonicalName());
        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                getPackageName() + ":process2",
                DaemonService2.class.getCanonicalName(),
                DaemonReceiver2.class.getCanonicalName());
        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        //return new DaemonConfigurations(configuration1, configuration2);//listener can be null
        return new DaemonConfigurations(configuration1, configuration2, listener);
    }

    @Override
    public void onConnected() {
        AppLogger.d("华为推送连接成功");
        HuaweiPush.HuaweiPushApi.getToken(client).setResultCallback(result -> {
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLogger.d("onConnectionSuspended" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        AppLogger.d("华为推送连接失败" + result.getErrorCode());
    }


    class MyDaemonListener implements DaemonConfigurations.DaemonListener {
        @Override
        public void onPersistentStart(Context context) {
            AppLogger.d("onPersistentStart");
        }

        @Override
        public void onDaemonAssistantStart(Context context) {
            AppLogger.d("onDaemonAssistantStart");
        }

        @Override
        public void onWatchDaemonDaed() {
            AppLogger.d("onWatchDaemonDaed");
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //这是主进程
        if (TextUtils.equals(ProcessUtils.myProcessName(this), getApplicationContext().getPackageName())) {
            PerformanceUtils.startTrace("appStart");
            PerformanceUtils.startTrace("appStart0");
            //Dagger2 依赖注入
            appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
            PreferencesUtils.init(getApplicationContext());
            enableDebugOptions();
            //每一个新的进程启动时，都会调用onCreate方法。
            try2init();
            initBugMonitor();
//        initLeakCanary();
            registerBootComplete();
            registerActivityLifecycleCallbacks(this);
            initHuaweiPushSDK();
            startService(new Intent(this, DataSourceService.class));
            PerformanceUtils.stopTrace("appStart");
        }
    }

    /**
     * 先检查，是否有读写权限
     */
    public void try2init() {
        if (PermissionUtils.hasSelfPermissions(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            initBlockCanary();
        } else {
            RxBus.getCacheInstance().postSticky(new RxEvent.ShouldCheckPermission());
            Log.d("try2init", "try2init failed");
        }
    }


    private void initHuaweiPushSDK() {
        AppLogger.d("正在初始化华为推送SDK");
        client = new HuaweiApiClient.Builder(this)
                .addApi(HuaweiPush.PUSH_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();
    }


    private void initLeakCanary() {
        HandlerThreadUtils.post(new Runnable() {
            @Override
            public void run() {
                LeakCanary.install(BaseApplication.this);
            }
        });
    }

    private void initBlockCanary() {
        HandlerThreadUtils.post(new Runnable() {
            @Override
            public void run() {
                AppLogger.d("initBlockCanary");
                //BlockCanary
                BlockCanary.install(ContextUtils.getContext(), new BlockCanaryContext()).start();
            }
        });
    }

    private void initBugMonitor() {
        HandlerThreadUtils.post(() ->
                //bugLy
                BugMonitor.init(ContextUtils.getContext()));
    }

    /**
     * 注册启动监听广播
     */
    private void registerBootComplete() {
        try {
            BootCompletedReceiver receiver = new BootCompletedReceiver();
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
            registerReceiver(receiver, intentFilter);
            Log.d(TAG, "bootComplete");
        } catch (Exception e) {
            Log.d(TAG, "bootComplete: e: " + e.toString());
        }
    }

    private void enableDebugOptions() {
        OptionsImpl.enableCrashHandler(this, PathGetter.createPath(JConstant.CRASH_PATH));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        AppLogger.d("进程已被销毁!!!!");
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                //should release some resource
                Log.d(TAG, "onTrimMemory: " + level);
//                shouldKillBellCallProcess();
                RxBus.getCacheInstance().post(new RxEvent.AppHideEvent());
//                JfgCmdInsurance.getCmd().closeDataBase();
                break;
        }
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory: ");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        AppLogger.i("life:onActivityCreated: " + activity.getClass().getSimpleName() + " " + savedInstanceState);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        AppLogger.i("life:onActivityStarted " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        AppLogger.i("life:onActivityResumed " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        AppLogger.i("life:onActivityPaused " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        AppLogger.i("life:onActivityStopped " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        AppLogger.i("life:onActivitySaveInstanceState " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        AppLogger.i("life:onActivityDestroyed " + activity.getClass().getSimpleName());
    }


    public static class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ProcessUtils.isServiceRunning(context, DataSourceService.class)) {
                AppLogger.i("start DataSourceService");
//                context.startService(new Intent(context, DataSourceService.class));
            }
        }
    }

    public HttpProxyCacheServer getProxy() {
        return proxy;
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }
}
