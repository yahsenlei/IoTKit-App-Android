package com.cylan.jiafeigou.n.view.panorama;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.cylan.entity.jniCall.JFGMsgVideoResolution;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.NewHomeActivity;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.injector.component.ActivityComponent;
import com.cylan.jiafeigou.base.module.BaseDeviceInformationFetcher;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.base.module.DeviceInformation;
import com.cylan.jiafeigou.base.wrapper.BaseActivity;
import com.cylan.jiafeigou.cache.db.module.DPEntity;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.databinding.LayoutPanoramaPopMenuBinding;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.misc.AlertDialogManager;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.n.view.activity.CamSettingActivity;
import com.cylan.jiafeigou.n.view.firmware.FirmwareUpdateActivity;
import com.cylan.jiafeigou.n.view.mine.HomeMineHelpActivity;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.support.photoselect.CircleImageView;
import com.cylan.jiafeigou.utils.BindUtils;
import com.cylan.jiafeigou.utils.FileUtils;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.NetUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.cylan.jiafeigou.utils.TimeUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.ImageViewTip;
import com.cylan.jiafeigou.widget.LoadingDialog;
import com.cylan.jiafeigou.widget.live.ILiveControl;
import com.cylan.jiafeigou.widget.video.PanoramicView720_Ext;
import com.cylan.jiafeigou.widget.video.VideoViewFactory;
import com.cylan.panorama.CommonPanoramicView;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rx.schedulers.Schedulers;

import static com.cylan.jiafeigou.base.module.PanoramaEvent.ERROR_CODE_HTTP_NOT_AVAILABLE;
import static com.cylan.jiafeigou.n.view.panorama.PanoramaCameraContact.View.PANORAMA_RECORD_MODE.MODE_LIVE;
import static com.cylan.jiafeigou.n.view.panorama.PanoramaCameraContact.View.PANORAMA_RECORD_MODE.MODE_SHORT;

/**
 * Created by yanzhendong on 2017/3/7.
 */
@RuntimePermissions
public class PanoramaCameraActivity extends BaseActivity<PanoramaCameraContact.Presenter> implements PanoramaCameraContact.View, CommonPanoramicView.PanoramaEventListener {

    @BindView(R.id.act_panorama_camera_banner)
    ViewSwitcher bannerSwitcher;
    @BindView(R.id.imgv_toolbar_right)
    ImageViewTip setting;
    @BindView(R.id.act_panorama_camera_banner_information_connection_icon)
    ImageView bannerConnectionIcon;
    @BindView(R.id.act_panorama_camera_banner_information_connection_text)
    TextView bannerConnectionText;
    @BindView(R.id.act_panorama_camera_banner_information_charge_icon)
    ImageView bannerChargeIcon;
    @BindView(R.id.act_panorama_camera_banner_information_charge_text)
    TextView bannerChargeText;
    @BindView(R.id.act_panorama_camera_toolbar)
    RelativeLayout panoramaToolBar;
    @BindView(R.id.act_panorama_camera_flow_speed)
    TextView liveFlowSpeedText;
    @BindView(R.id.act_panorama_camera_banner_bad_net_work_configure)
    TextView bannerWarmingTitle;
    @BindView(R.id.act_panorama_camera_bottom_panel_picture)
    RadioButton bottomPanelPictureMode;
    @BindView(R.id.act_panorama_camera_bottom_panel_video)
    RadioButton bottomPanelVideoMode;
    @BindView(R.id.act_panorama_camera_bottom_panel_rtmp)
    RadioButton bottomPanelRtmpMode;
    @BindView(R.id.act_panorama_camera_bottom_panel_more)
    ImageButton bottomPanelMoreItem;
    @BindView(R.id.act_panorama_bottom_panel_camera_photograph)
    ImageButton bottomPanelPhotoGraphItem;
    @BindView(R.id.act_panorama_camera_bottom_panel_live_setting)
    ImageButton bottomPanelLivePlatform;

    @BindView(R.id.act_panorama_camera_bottom_panel_album)
    CircleImageView bottomPanelAlbumItem;
    @BindView(R.id.act_panorama_camera_bottom_panel_switcher_menu_item)
    RadioGroup bottomPanelSwitcherItem1ViewMode;

    @BindView(R.id.act_panorama_camera_bottom_panel_switcher_menu)
    ViewSwitcher bottomPanelSwitcher;
    @BindView(R.id.act_panorama_camera_bottom_panel_switcher_menu_information)
    TextView bottomPanelSwitcherItem2Information;
    //    @BindView(R.id.act_panorama_camera_bottom_panel_switcher_menu_information_record_time)
//    TextView bottomPanelSwitcherItem2TimeText;
    @BindView(R.id.act_panorama_camera_bottom_panel_switcher_menu_information_red_dot)
    ImageView bottomPanelSwitcherItem2DotIndicator;

    @BindView(R.id.act_panorama_camera_video_container)
    FrameLayout videoLiveContainer;
    @BindView(R.id.act_panorama_camera_loading)
    ILiveControl loadingBar;
    @BindView(R.id.act_panorama_camera_bottom_count_down_line)
    View bottomCountDownLine;
    @BindView(R.id.tv_top_bar_left)
    Button topLeftMenu;
    @BindView(R.id.act_panorama_camera_bottom_panel)
    ConstraintLayout bottomPanelContainer;
    @BindView(R.id.act_panorama_bottom_panel_loading)
    ProgressBar bottomPanelLoading;
    @BindView(R.id.act_panorama_camera_upgrading)
    TextView cameraUpgrading;
    @BindView(R.id.imv_toolbar_message)
    ImageViewTip ivt_newMessageTips;
    @BindView(R.id.act_panorama_camera_banner_bad_net_work_close)
    ImageView bannerTextInformationClose;

    private AlertDialog deviceReportDialog;

    @SPEED_MODE
    private int speedMode = SPEED_MODE.AUTO;
    @PANORAMA_VIEW_MODE
    private int panoramaViewMode = PANORAMA_VIEW_MODE.MODE_PICTURE;
    @PANORAMA_RECORD_MODE
    private int panoramaRecordMode = PANORAMA_RECORD_MODE.MODE_NONE;
    private PopupWindow videoPopHint;
    private PanoramicView720_Ext surfaceView;
    private AlertDialog mobileAlert;
    private boolean justForTest = false;
    private boolean upgrading = false;
    private boolean alertSDFormatError = true;
    private boolean alertHttpNotAvailable = true;
    private boolean alertMobile = true;
    private PopupWindow popOption;
    private LayoutPanoramaPopMenuBinding menuBinding;
    private ObjectAnimator countDownAnimator;

    private List<AlertDialog> dislogs = new ArrayList<>();
    /**
     * 保存前一次的网络类型,只有改变的时候才 toast
     */
    private int preNetType = -1;
    private int livePlatform;
    private boolean apMode;
    //    private String rtmpAddress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        JConstant.KEY_CURRENT_PLAY_VIEW = this.getClass().getName();
    }

    @Override
    public void onViewer() {
        liveFlowSpeedText.setVisibility(View.INVISIBLE);
        loadingBar.setState(JConstant.PLAY_STATE_PREPARE, null);
        onHideBadNetWorkBanner();
    }

    @Override
    public void onDismiss() {
    }


    @Override
    protected void initViewAndListener() {
        super.initViewAndListener();
        menuBinding = LayoutPanoramaPopMenuBinding.inflate(LayoutInflater.from(this));//先初始化,以免为空
        View contentView = menuBinding.getRoot();
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popOption = new PopupWindow(contentView, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        popOption.setFocusable(true);
        popOption.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popOption.setOutsideTouchable(true);

        menuBinding.actPanoramaCameraQuickMenuItem2Voice.setEnabled(false);//默认不可点击,只有收到分辨率后才能点击
        menuBinding.actPanoramaCameraQuickMenuItem1Mic.setEnabled(false);
        menuBinding.actPanoramaCameraQuickMenuItem2Voice.setOnClickListener(this::clickedQuickMenuItem2SwitchVoice);
        menuBinding.actPanoramaCameraQuickMenuItem1Mic.setOnClickListener(this::clickedQuickMenuItem1SwitchMic);
        menuBinding.actPanoramaCameraQuickMenuItem3Resolution.setOnClickListener(this::clickedQuickMenuItem3Resolution);
        loadingBar.setState(JConstant.PLAY_STATE_IDLE, null);
        bottomPanelLoading.setEnabled(false);
        if (FileUtils.isFileExist(JConstant.MEDIA_PATH + File.separator + JConstant.PANORAMA_THUMB_PICTURE + File.separator + uuid + ".jpg")) {
            onShowPreviewPicture(JConstant.MEDIA_PATH + File.separator + JConstant.PANORAMA_THUMB_PICTURE + File.separator + uuid + ".jpg");
        }
        Device device = DataSourceManager.getInstance().getDevice(uuid);
        String alias = device.getAlias();
        topLeftMenu.setText(TextUtils.isEmpty(alias) ? getString(R.string._720PanoramicCamera) : alias);
        loadingBar.setAction(loadController);
        bottomPanelPhotoGraphItem.setOnTouchListener(photoGraphTouchListener);
        panoramaToolBar.setBackgroundResource(JFGRules.getTimeRule() == 0 ? R.color.color_0ba8cf : R.color.color_23344e);
//        alertSDFormatError = true;
//        deviceReportDialog =
        alertHttpNotAvailable = PreferencesUtils.getBoolean(JConstant.SWITCH_MODE_POP + ":" + uuid, alertHttpNotAvailable);


        Pair<DPEntity, Integer> pair = handleUnreadCount(device);
        ivt_newMessageTips.setShowDot(pair != null && pair.second > 0);
        bottomPanelAlbumItem.showHint(false);

        initPanoramaVideoView();
        panoramaViewMode = PreferencesUtils.getInt(JConstant.PANORAMA_VIEW_MODE + ":" + uuid, PANORAMA_VIEW_MODE.MODE_PICTURE);
        onRefreshViewModeUI(panoramaViewMode, false, false);
//        onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_LIVE, false, false);//just for test
//        showRtmpLiveSetting();
    }

    private View.OnTouchListener photoGraphTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (panoramaRecordMode == MODE_SHORT) {
                        AppLogger.w("录制短视频结束了");
                        if (countDownAnimator != null) {
                            countDownAnimator.cancel();
                        }
                        onRefreshControllerView(false, false);
                        presenter.stopVideoRecord(PANORAMA_RECORD_MODE.MODE_SHORT);
                        bottomPanelLoading.setVisibility(View.VISIBLE);
                        bottomPanelPhotoGraphItem.setEnabled(false);
                    }
                    break;
                }
            }
            return false;
        }
    };

    private ILiveControl.Action loadController = new ILiveControl.Action() {
        @Override
        public void clickImage(View view, int state) {
            onRefreshConnectionMode(NetUtils.getNetType(PanoramaCameraActivity.this));
//            onHideBadNetWorkBanner();
//            presenter.startViewer();
        }

        @Override
        public void clickText(View view) {

        }

        @Override
        public void clickHelp(View view) {

        }
    };

    @Override
    public void onSpeaker(boolean on) {
        menuBinding.actPanoramaCameraQuickMenuItem2Voice.setImageResource(on ? R.drawable.camera720_icon_voice_selector : R.drawable.camera720_icon_no_voice_selector);
    }

    @Override
    public void onMicrophone(boolean on) {
        menuBinding.actPanoramaCameraQuickMenuItem1Mic.setImageResource(on ? R.drawable.camera720_icon_talk_selector : R.drawable.camera720_icon_no_talk_selector);
    }

    private void initPanoramaVideoView() {
        videoLiveContainer.setVisibility(View.VISIBLE);
        if (surfaceView == null) {
            surfaceView = (PanoramicView720_Ext) VideoViewFactory.CreateRendererExt(VideoViewFactory.RENDERER_VIEW_TYPE.TYPE_PANORAMA_720, this, true);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            surfaceView.setLayoutParams(params);
            videoLiveContainer.addView(surfaceView);
            surfaceView.setDisplayMode(PanoramicView720_Ext.DM_Fisheye);
            surfaceView.setId("IVideoView".hashCode());
            surfaceView.setEventListener(this);
        }
        surfaceView.configV720();
    }

    @Override
    public void onResolution(JFGMsgVideoResolution resolution) throws JfgException {
        initPanoramaVideoView();

        BaseApplication.getAppComponent().getCmd().enableRenderSingleRemoteView(true, surfaceView);
        loadingBar.setState(JConstant.PLAY_STATE_IDLE, null);
        liveFlowSpeedText.setVisibility(View.VISIBLE);
        liveFlowSpeedText.setText("0K/s");
        onRefreshControllerView(true, false);
        onHideBadNetWorkBanner();
    }


    @Override
    public void onShowPreviewPicture(String picture) {
        bottomPanelAlbumItem.showHint(true);
        if (!TextUtils.isEmpty(picture)) {
            Glide.with(this)
                    .load(picture)
                    .animate(view -> {
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.2f, 1.0f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.2f, 1.0f);
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(scaleX, scaleY);
                        set.setDuration(300);
                        set.start();
                    })
                    .error(R.drawable.camera720_icon_album_selector)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new ImageViewTarget<GlideDrawable>(bottomPanelAlbumItem) {
                        @Override
                        protected void setResource(GlideDrawable resource) {
                            view.setImageDrawable(resource);
                            view.setAlpha(view.isEnabled() ? 1 : 0.3f);
                        }
                    });
        } else {
            if (surfaceView != null) {
                surfaceView.takeSnapshot(true);
            }
        }
    }

    @Override
    public void onFlowSpeed(int speed) {
        if (!liveFlowSpeedText.isShown()) {
            liveFlowSpeedText.setVisibility(View.VISIBLE);
        }
        liveFlowSpeedText.setText(MiscUtils.getByteFromBitRate(speed));
    }


    @Override
    public void onConnectDeviceTimeOut() {
        onRefreshConnectionMode(-1);
    }

    @Override
    public void onVideoDisconnect(int code) {
        if (code == JError.ErrorVideoPeerInConnect) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.CONNECTING)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (dialog, which) -> {
                        dialog.dismiss();
//                        presenter.dismiss();
                        finish();
                    })
                    .show();
        } else if (code == -3) {
            AppLogger.w("固件升级中....");
            onDeviceUpgrade();
        } else {
            onRefreshConnectionMode(-1);
        }
        onRefreshViewModeUI(panoramaViewMode, false, false);
        onRefreshControllerView(false, false);
    }

    @Override
    public void onDeviceUnBind() {
        AppLogger.w("当前设备已解绑" + uuid);
        presenter.cancelViewer();
        AlertDialog.Builder builder = AlertDialogManager.getInstance().getCustomDialog(getActivityContext());
        builder.setPositiveButton(getString(R.string.OK), (dialog, which) -> {
            finish();
            Intent intent = new Intent(this, NewHomeActivity.class);
            startActivity(intent);
        })
                .setMessage(getString(R.string.Tap1_device_deleted));
        AlertDialogManager.getInstance().showDialog("onDeviceUnBind", this, builder);
    }

    @Override
    public void onLoading(boolean loading) {
        loadingBar.setState(loading ? JConstant.PLAY_STATE_PREPARE : JConstant.PLAY_STATE_IDLE, null);
//        onRefreshControllerView(!loading);//loading 不再更新ControllerView 因为可能有冲突
        if (!loading) {
            onHideBadNetWorkBanner();
        }
        AppLogger.w("正在加载中.......");
    }

    @Override
    public void onShowVideoPreviewPicture(String picture) {

    }

    @Override
    public void hasNoAudioPermission() {
        AppLogger.w("没有声音权限.......");
    }

    @Override
    protected int getContentViewID() {
        return R.layout.activity_panorama_camera;
    }

    @Override
    public void onStart() {
        super.onStart();
        ViewUtils.setViewPaddingStatusBar(panoramaToolBar);
        initLiveConfigure();
        presenter.checkAndInitRecord();
//        }
    }

    private void initLiveConfigure() {
        livePlatform = PreferencesUtils.getInt(JConstant.LIVE_PLATFORM_KEY, -1);
        switch (livePlatform) {
            case 0: {
                bottomPanelLivePlatform.setImageResource(R.drawable.camera720_icon_live_menu_facebook_selector);
            }
            break;
            case 1: {
                bottomPanelLivePlatform.setImageResource(R.drawable.camera720_icon_live_menu_youtube_selector);
            }
            break;
            case 2: {
                bottomPanelLivePlatform.setImageResource(R.drawable.camera720_icon_live_menu_weibo_selector);
            }
            break;
            case 3: {
                bottomPanelLivePlatform.setImageResource(R.drawable.camera720_icon_live_menu_rtmp_selector);
            }
            break;
            default: {
                bottomPanelLivePlatform.setImageResource(R.drawable.camera720_icon_live_menu_facebook_selector);
            }
        }
    }


    private void updateHint() {
        try {
            Device device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
            if (JFGRules.isPanoramaCamera(device.pid)) return;
            if (JFGRules.isShareDevice(device)) return;
            String content = PreferencesUtils.getString(JConstant.KEY_FIRMWARE_CONTENT + uuid);
            RxEvent.CheckVersionRsp description = new Gson().fromJson(content, RxEvent.CheckVersionRsp.class);
            String currentV = device.$(207, "");
            boolean result = description.hasNew && BindUtils.versionCompare(description.version, currentV) > 0;
            setting.setShowDot(result);
        } catch (Exception e) {
            return;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setting.setShowDot(!TextUtils.isEmpty(PreferencesUtils.getString(JConstant.KEY_FIRMWARE_CONTENT + uuid)));
//        setting.setEnabled(false);
        onRefreshViewModeUI(panoramaViewMode, false, false);
        onRefreshControllerView(false, true);
        updateHint();
//        setting.setEnabled(true);
        preNetType = -1;
        int netType = NetUtils.getNetType(this);
        //不保存值到 sp 中了,只保存页面变量
//        boolean alertMobile = netType == ConnectivityManager.TYPE_MOBILE && PreferencesUtils.getBoolean(JConstant.ALERT_MOBILE, true);
//        if (!hasNetSetting) {//fragment 和 activity 会同时调用生命周期方法我们的播放逻辑必须在当前没有 fragment 的情况下进行
        onRefreshConnectionMode(netType);
        if (surfaceView != null) {
            surfaceView.onResume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ViewUtils.clearViewPaddingStatusBar(panoramaToolBar);
//        presenter.dismiss();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (surfaceView != null) {
            surfaceView.onPause();
        }
        presenter.cancelViewer();
        preNetType = -1;
        PreferencesUtils.putInt(JConstant.PANORAMA_VIEW_MODE + ":" + uuid, panoramaViewMode);
    }

    @Override
    protected void setActivityComponent(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    //    @Override
//    protected void setActivityComponent(ActivityComponent activityComponent) {
//        activityComponent.inject(this);
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (surfaceView != null) {
            surfaceView.onDestroy();
            surfaceView = null;
            videoLiveContainer.removeAllViews();
        }
    }

    public boolean muteAudio(boolean bMute) {
        boolean isSuccess;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (bMute) {
            int result = am.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            isSuccess = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        } else {
            int result = am.abandonAudioFocus(null);
            isSuccess = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        AppLogger.w("pauseMusic bMute=" + bMute + " result=" + isSuccess);
        return isSuccess;
    }

    @OnClick(R.id.act_panorama_camera_bottom_panel_live_setting)
    @Override
    public void showRtmpLiveSetting() {
        AppLogger.w("clickedBottomPanelLiveSetting");
        Intent intent = new Intent(this, LiveSettingActivity.class);
        intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
        startActivity(intent);
    }


    @OnClick(R.id.act_panorama_camera_bottom_panel_more)
    public void clickedBottomPanelMoreItem() {
        AppLogger.w("clickedBottomPanelMoreItem");
        if (popOption.isShowing()) {
            popOption.dismiss();
        } else {
            PopupWindowCompat.showAsDropDown(popOption, bottomPanelContainer, 0, 0, Gravity.TOP | Gravity.END);
        }
    }

    @OnLongClick(R.id.act_panorama_bottom_panel_camera_photograph)
    public boolean longClickedBottomPanelPhotoGraphItem() {
        AppLogger.w("longClickedBottomPanelPhotoGraphItem");
        if (panoramaViewMode == PANORAMA_VIEW_MODE.MODE_VIDEO && panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_NONE) {
//            onRefreshControllerView(false, false);
            presenter.startVideoRecord(PANORAMA_RECORD_MODE.MODE_SHORT);
        }
        hideVideoModePop();
        return true;
    }

    @OnClick(R.id.act_panorama_bottom_panel_camera_photograph)
    public void clickedBottomPanelPhotoGraphItem() {

        hideVideoModePop();
        onRefreshControllerView(false, false);

        switch (panoramaViewMode) {
            case PANORAMA_VIEW_MODE.MODE_PICTURE: {
                AppLogger.w("将进行拍照");
                presenter.makePhotograph();
                bottomPanelLoading.setVisibility(View.VISIBLE);
            }
            break;
            case PANORAMA_VIEW_MODE.MODE_VIDEO: {
                if (panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_NONE) {
                    AppLogger.w("将进行长视频录制");
                    presenter.startVideoRecord(panoramaRecordMode = PANORAMA_RECORD_MODE.MODE_LONG);
                } else if (panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_LONG) {
                    AppLogger.w("将结束长视频录制");
                    presenter.stopVideoRecord(panoramaRecordMode);
                }
                bottomPanelLoading.setVisibility(View.VISIBLE);
            }
            break;
            case PANORAMA_VIEW_MODE.MODE_LIVE: {
                if (panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_NONE) {
                    //先要判断是否是 AP 模式
                    if (apMode) {
                        showAPLiveAlert();
                        return;
                    } else if (presenter.getBattery() < 5) {
                        showLiveBatteryLowAlert();
                        return;
                    }

                    panoramaRecordMode = PANORAMA_RECORD_MODE.MODE_LIVE;
                    if (livePlatform == -1) {
                        showRtmpLiveSetting();
                    } else {
                        AppLogger.w("将进行 live 直播");
                        bottomPanelAlbumItem.setVisibility(View.GONE);

                        presenter.cameraLiveRtmpCtrl(livePlatform, 1);
                    }
                } else if (panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_LIVE) {
                    AppLogger.w("将结束 live 直播");
                    presenter.cameraLiveRtmpCtrl(livePlatform, 0);
                }
                bottomPanelLoading.setVisibility(View.GONE);
            }
            break;
        }
        bottomPanelPhotoGraphItem.setEnabled(false);

    }

    private void showLiveBatteryLowAlert() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.Tap1_LowPower)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, null)
                .show();
    }

    public void showAPLiveAlert() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.LIVE_LANMODE_POP)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, (dialog, which) -> {
                    Intent intent = new Intent(this, CamSettingActivity.class);
                    intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
                    startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(getApplicationContext(),
                            R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
                })
                .setNegativeButton(R.string.CANCEL, (dialog, which) -> onRefreshControllerView(presenter.getLiveAction().hasResolution, false))
                .show();
    }

    private String getPlatformString(int livePlatform) {
        switch (livePlatform) {
            case 0:
                return getString(R.string.LIVE_PLATFORM_FACEBOOK);
            case 1:
                return getString(R.string.LIVE_PLATFORM_YOUTUBE);
            case 2:
                return getString(R.string.LIVE_PLATFORM_WEIBO);
            case 3:
                return "RTMP";
        }
        return "RTMP";
    }

    @OnClick(R.id.act_panorama_camera_bottom_panel_album)
    public void clickedBottomPanelAlbumItem() {
        AppLogger.w("clickedBottomPanelAlbumItem");
        presenter.dismiss();
        bottomPanelAlbumItem.showHint(false);
        Intent intent = new Intent(this, PanoramaAlbumActivity.class);
        intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
        startActivity(intent);
    }

    @OnClick(R.id.act_panorama_camera_bottom_panel_picture)
    public void switchViewerModeToPicture() {
        AppLogger.w("switchViewerModeToPicture");
        hideVideoModePop();
        onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_PICTURE, presenter.getLiveAction().hasResolution, false);
        onRefreshControllerView(presenter.getLiveAction().hasResolution, true);
    }

    @OnClick(R.id.act_panorama_camera_bottom_panel_video)
    public void switchViewerModeToVideo() {
        AppLogger.w("switchViewerModeToVideo");
        if (PreferencesUtils.getBoolean(JConstant.KEY_PANORAMA_POP_HINT, true)) {//只提示一次
            PreferencesUtils.putBoolean(JConstant.KEY_PANORAMA_POP_HINT, false);
            showVideoModePop();
        }
        onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_VIDEO, presenter.getLiveAction().hasResolution, false);
        onRefreshControllerView(presenter.getLiveAction().hasResolution, true);
    }

    @OnClick(R.id.act_panorama_camera_bottom_panel_rtmp)
    public void switchViewerModeToRtmpLive() {
        AppLogger.w("switchViewerModeToRtmpLive");
        onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_LIVE, presenter.getLiveAction().hasResolution, false);
        onRefreshControllerView(presenter.getLiveAction().hasResolution, true);
    }


    public void showVideoModePop() {
        if (videoPopHint == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_panorama_pop_hint, null);
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            videoPopHint = new PopupWindow(view, view.getMeasuredWidth(), view.getMeasuredHeight());
            videoPopHint.setFocusable(false);
            videoPopHint.setOutsideTouchable(true);
            videoPopHint.setTouchable(false);
            videoPopHint.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        int xPos = (int) ((videoPopHint.getWidth() - bottomPanelVideoMode.getWidth()) / 2 + getResources().getDimension(R.dimen.y2));
        int yPos = (int) (videoPopHint.getHeight() + getResources().getDimension(R.dimen.y2) + bottomPanelVideoMode.getHeight());
        PopupWindowCompat.showAsDropDown(videoPopHint, bottomPanelVideoMode, -xPos, -yPos, Gravity.TOP | Gravity.START);
    }

    public boolean hideVideoModePop() {
        if (videoPopHint != null && videoPopHint.isShowing()) {
            videoPopHint.dismiss();
            return true;
        }
        return false;
    }

    @OnClick(R.id.imgv_toolbar_right)
    public void clickedToolBarSettingMenu() {
        AppLogger.w("clickedSettingMenu");
        hideVideoModePop();
//        presenter.dismiss();
        startActivity(new Intent(this, CamSettingActivity.class).putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid));
        presenter.dismiss();
    }

    @OnClick(R.id.tv_top_bar_left)
    public void onBackClick() {
        if (!hideVideoModePop()) {
//            presenter.dismiss();
//            finish();
        }
        onBackPressed();
        AppLogger.w("clickedToolBarBackMenu");
    }

    //    @Override
    protected void onPrepareToExit(Action action) {
        presenter.dismiss();
        if (surfaceView != null) {
            surfaceView.onPause();
            videoLiveContainer.removeAllViews();
            surfaceView = null;
            muteAudio(false);
        }
        action.actionDone();
    }

    @OnClick(R.id.act_panorama_camera_banner_bad_net_work_close)
    public void clickedCloseBadNetWorkBanner() {
        AppLogger.w("clickedCloseBadNetWorkBanner");
        onHideBadNetWorkBanner();
    }

    public void showBannerTextInformation(String text, boolean hasClose) {
        int childIndex = bannerSwitcher.getDisplayedChild();
        if (childIndex == 0) {
            bannerSwitcher.showNext();
        }
        bannerWarmingTitle.setText(text);
        bannerTextInformationClose.setVisibility(hasClose ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showBottomPanelInformation(String text, boolean hasDot) {
        int childIndex = bottomPanelSwitcher.getDisplayedChild();
        if (childIndex == 0) {
            bottomPanelSwitcher.showNext();
        }
        bottomPanelSwitcherItem2Information.setText(text);
        bottomPanelSwitcherItem2DotIndicator.setVisibility(hasDot ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.act_panorama_camera_banner_bad_net_work_configure)
    public void clickedConfigureNetWorkBanner() {
        AppLogger.d("clickedConfigureNetWorkBanner");
        CharSequence text = bannerWarmingTitle.getText();
        if (TextUtils.equals(text, getString(R.string.Tap1_Offline))) {
            Intent intent = new Intent(this, ConnectionActivity.class);
            intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
            intent.putExtra("which", "offline");
            startActivity(intent);
        } else if (TextUtils.equals(text, getString(R.string.Tap1_DisconnectedPleaseCheck))) {
            Intent intent = new Intent(this, ConnectionActivity.class);
            intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
            intent.putExtra("which", "no_network");
            startActivity(intent);
        }
    }


    public void clickedQuickMenuItem1SwitchMic(View view) {
        AppLogger.w("clickedQuickMenuItem1SwitchMic");
        PanoramaCameraActivityPermissionsDispatcher.switchMicroPhoneWithPermissionWithCheck(this);
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void switchSpeakerWithPermission() {
        presenter.switchSpeaker();
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    public void showEnablePermissionAlert(PermissionRequest request) {
        AppLogger.w("需要提醒用户给 APP 授权");
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    public void onNoAudioPermission() {
        AppLogger.d("没有控制扬声器和麦克风的权限");
    }


    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    void switchMicroPhoneWithPermission() {
        presenter.switchMicrophone();
    }

    public void clickedQuickMenuItem2SwitchVoice(View view) {
        AppLogger.w("clickedQuickMenuItem2SwitchVoice");
        PanoramaCameraActivityPermissionsDispatcher.switchSpeakerWithPermissionWithCheck(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PanoramaCameraActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public void clickedQuickMenuItem3Resolution(View view) {
        AppLogger.w("clickedQuickMenuItem3Resolution");
        presenter.switchVideoResolution(move(speedMode, true));
    }

    private
    @SPEED_MODE
    int move(@SPEED_MODE int mode, boolean next) {
        switch (mode) {
            case SPEED_MODE.NORMAL:
                return next ? SPEED_MODE.HD : SPEED_MODE.HD;
            case SPEED_MODE.HD:
                return next ? SPEED_MODE.NORMAL : SPEED_MODE.NORMAL;
            case SPEED_MODE.AUTO:
                return next ? SPEED_MODE.NORMAL : SPEED_MODE.HD;
            case SPEED_MODE.FLUENCY:
                return next ? SPEED_MODE.NORMAL : SPEED_MODE.HD;
            default:
                return SPEED_MODE.HD;
        }
    }

    public void onHideBadNetWorkBanner() {
        AppLogger.w("onHideBadNetWorkBanner");
        int childIndex = bannerSwitcher.getDisplayedChild();
        if (childIndex == 1) {
            bannerSwitcher.showPrevious();
        }
    }

    @Override
    public void onBellBatteryDrainOut() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.Tap1_LowPower)
                .setCancelable(false)
                .setPositiveButton(R.string.I_KNOW, null)
                .show();
    }

    @Override
    public void onDeviceBatteryChanged(Integer battery) {
        if (battery == -2) return;
        bannerChargeText.setVisibility(View.VISIBLE);
        bannerChargeIcon.setVisibility(View.VISIBLE);
        bannerChargeText.setText(battery == -1 ? getString(R.string.CHARGING) : battery + "%");
        if (battery == -1) {
            bannerChargeIcon.setImageResource(R.drawable.camera720_icon_electricity_charge);
        } else if (battery <= 20) {
            bannerChargeIcon.setImageResource(R.drawable.camera720_icon_electricity_low_power);
        } else if (battery > 20 && battery < 80) {
            bannerChargeIcon.setImageResource(R.drawable.camera720_icon_electricity_charge_half);
        } else if (battery >= 80) {
            bannerChargeIcon.setImageResource(R.drawable.camera720_icon_electricity_charge_full);
        }
    }

    @Override
    public void onSDFormatResult(int code) {
        LoadingDialog.dismissLoading();
        if (code == 1) {
            ToastUtil.showPositiveToast(getString(R.string.SD_INFO_3));
        } else if (code == -1) {
            ToastUtil.showNegativeToast(getString(R.string.SD_ERR_3));
        }
    }

    @Override
    public void onDeviceInitFinish() {

    }

    @Override
    public void onShowNewMsgHint() {
        ivt_newMessageTips.setShowDot(true);
    }

    @Override
    public void onDeviceOnLine() {
        upgrading = false;
//        if (upgrading || !presenter.getLiveAction().hasStarted) {
        onRefreshConnectionMode(-1);
//        }
    }

    @Override
    public void onSendCameraLiveResponse(int i, boolean success) {
        if (i == 1) {//开启的响应
            if (success) {
                // TODO: 2017/9/9 成功了
                if (livePlatform == 1) {
                    showBottomPanelInformation(getString(R.string.LIVE_TESTING), false);
                }
                AppLogger.w("rtmp 配置消息发送成功");
            } else {
                // TODO: 2017/9/9 失败了
                onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_LIVE, presenter.getLiveAction().hasResolution, false);
                onRefreshControllerView(presenter.getLiveAction().hasResolution, false);
                AppLogger.w("rtmp 配置消息发送失败了");
            }
        } else if (i == 0) {//结束的响应

        }
    }

    @Override
    public void onRtmpQueryResponse(DpMsgDefine.DPCameraLiveRtmpStatus unpackData) {
        if (unpackData.flag != 3 && unpackData.error == 0) {
            if (panoramaRecordMode != PANORAMA_RECORD_MODE.MODE_LIVE) {
                panoramaRecordMode = PANORAMA_RECORD_MODE.MODE_LIVE;
                onRefreshViewModeUI(PANORAMA_VIEW_MODE.MODE_LIVE, presenter.getLiveAction().hasResolution, true);
                onRefreshControllerView(presenter.getLiveAction().hasResolution, false);
            }
        }
    }

    @Override
    public void onRtmpAddressError() {
        switch (livePlatform) {
            case 0:
                PreferencesUtils.remove(JConstant.FACEBOOK_PREF_DESCRIPTION);
                break;
            case 1:
                PreferencesUtils.remove(JConstant.YOUTUBE_PREF_CONFIGURE + ":" + uuid);
                break;
            case 2:
                PreferencesUtils.remove(JConstant.WEIBO_PREF_LIVE_ID);
                break;
            case 3:
                PreferencesUtils.remove(JConstant.RTMP_PREF_CONFIGURE);
                break;
        }
//        rtmpAddress = null;
        livePlatform = -1;
        showBannerTextInformation(getString(R.string.LIVE_FAILED), false);
    }

    @Override
    public void onShowLiveCreateView() {
        bottomPanelAlbumItem.setVisibility(View.INVISIBLE);
        bottomPanelLivePlatform.setVisibility(View.INVISIBLE);
        bottomPanelMoreItem.setEnabled(true);
        bottomPanelMoreItem.setVisibility(View.VISIBLE);
    }

    @Override
    public void onWeiboException() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.LIVE_SINAWEIBO_UNABLE_LIVE)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, (dialog, which) -> {
                    Intent intent = new Intent(this, HomeMineHelpActivity.class);
                    intent.putExtra(JConstant.KEY_SHOW_SUGGESTION, JConstant.KEY_SHOW_SUGGESTION);
                    startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(this,
                            R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
                })
                .setNegativeButton(R.string.CANCEL, null)
                .show();
    }

    public void onDeviceUpgrade() {
        upgrading = true;
        cameraUpgrading.setVisibility(View.VISIBLE);
        setting.setEnabled(false);
        ivt_newMessageTips.setEnabled(false);
        onRefreshControllerView(false, true);
        bannerSwitcher.setVisibility(View.INVISIBLE);
        liveFlowSpeedText.setVisibility(View.INVISIBLE);
        loadingBar.setState(JConstant.PLAY_STATE_IDLE, null);
    }

    public void onSwitchSpeedMode(@SPEED_MODE int mode) {
        switch (this.speedMode = mode) {
            case SPEED_MODE.AUTO:
//                menuBinding.actPanoramaCameraQuickMenuItem3Content.setText(R.string.Tap1_Camera_Video_Auto);
                break;
            case SPEED_MODE.FLUENCY:
            case SPEED_MODE.NORMAL:
                menuBinding.actPanoramaCameraQuickMenuItem3Resolution.setText(R.string.Tap1_Camera_Video_SD);
                break;
            case SPEED_MODE.HD:
                menuBinding.actPanoramaCameraQuickMenuItem3Resolution.setText(R.string.Tap1_Camera_Video_HD);
                break;
        }
    }

    @Override
    public void onRefreshViewModeUI(int viewMode, boolean enable, boolean record) {
        AppLogger.w("onRefreshViewModeUI");
        if (upgrading) return;

        // TODO: 2017/9/5 先初始化
        panoramaViewMode = viewMode;
        bottomPanelAlbumItem.setVisibility(View.VISIBLE);
        bottomPanelMoreItem.setVisibility(View.VISIBLE);
        bottomCountDownLine.setVisibility(View.GONE);
        bottomPanelLoading.setVisibility(View.GONE);
        if (countDownAnimator != null) {
            countDownAnimator.cancel();
        }
        bottomCountDownLine.setVisibility(View.GONE);
        bottomCountDownLine.setScaleX(1);
        if (bottomPanelSwitcher.getDisplayedChild() == 1) {
            bottomPanelSwitcher.showPrevious();
        }
        if (enable) hideLoading();

        setting.setEnabled(true);
        ivt_newMessageTips.setEnabled(true);
        popOption.dismiss();

        switch (viewMode) {
            case PANORAMA_VIEW_MODE.MODE_PICTURE: {
                panoramaRecordMode = PANORAMA_RECORD_MODE.MODE_NONE;
                bottomPanelSwitcherItem1ViewMode.check(R.id.act_panorama_camera_bottom_panel_picture);
                bottomPanelPhotoGraphItem.setImageResource(R.drawable.camera720_icon_photograph_selector);
                bottomPanelLivePlatform.setVisibility(View.GONE);
                ViewGroup.LayoutParams layoutParams = bottomPanelMoreItem.getLayoutParams();
                int pixelSize = getResources().getDimensionPixelSize(R.dimen.y43);
                layoutParams.width = pixelSize;
                layoutParams.height = pixelSize;
                bottomPanelMoreItem.setLayoutParams(layoutParams);
            }
            break;
            case PANORAMA_VIEW_MODE.MODE_VIDEO: {
                panoramaRecordMode = record ? panoramaRecordMode : PANORAMA_RECORD_MODE.MODE_NONE;
                bottomPanelSwitcherItem1ViewMode.check(R.id.act_panorama_camera_bottom_panel_video);
                bottomPanelPhotoGraphItem.setImageResource(record ? R.drawable.camera720_icon_video_recording_selector : R.drawable.camera720_icon_short_video_selector);
                bottomPanelLivePlatform.setVisibility(View.GONE);
                ViewGroup.LayoutParams layoutParams = bottomPanelMoreItem.getLayoutParams();
                int pixelSize = getResources().getDimensionPixelSize(R.dimen.y43);
                layoutParams.width = pixelSize;
                layoutParams.height = pixelSize;
                bottomPanelMoreItem.setLayoutParams(layoutParams);
            }
            break;
            case PANORAMA_VIEW_MODE.MODE_LIVE: {
                panoramaRecordMode = record ? PANORAMA_RECORD_MODE.MODE_LIVE : PANORAMA_RECORD_MODE.MODE_NONE;
                bottomPanelSwitcherItem1ViewMode.check(R.id.act_panorama_camera_bottom_panel_rtmp);
                bottomPanelPhotoGraphItem.setImageResource(record ? R.drawable.camera720_icon_video_recording_selector : R.drawable.camera720_icon_live_recording_selector);
                bottomPanelLivePlatform.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = bottomPanelMoreItem.getLayoutParams();
                int pixelSize = getResources().getDimensionPixelSize(R.dimen.y26);
                layoutParams.width = pixelSize;
                layoutParams.height = pixelSize;
                bottomPanelMoreItem.setLayoutParams(layoutParams);
            }
            break;
        }
        onRefreshControllerView(enable && presenter.getLiveAction().hasResolution, false);
//        bottomPanelPhotoGraphItem.setImageResource(viewMode == PANORAMA_VIEW_MODE.MODE_PICTURE ? R.drawable.camera720_icon_photograph_selector : record ? R.drawable.camera720_icon_video_recording_selector : R.drawable.camera720_icon_short_video_selector);
//        bottomPanelSwitcherItem1ViewMode.check(viewMode == PANORAMA_VIEW_MODE.MODE_PICTURE ? R.id.act_panorama_camera_bottom_panel_picture : R.id.act_panorama_camera_bottom_panel_video);

    }

    public void onRefreshControllerViewVisible(boolean visible) {
        bottomPanelSwitcher.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        bottomPanelPictureMode.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        bottomPanelVideoMode.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        bottomPanelMoreItem.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        bottomPanelAlbumItem.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void onRefreshControllerView(boolean enable, boolean all) {
        boolean finalEnable = (presenter.getLiveAction().hasResolution && enable) || justForTest;
        bottomPanelSwitcher.setEnabled(finalEnable);
        bottomPanelPictureMode.setEnabled(finalEnable);
        bottomPanelVideoMode.setEnabled(finalEnable);

        bottomPanelRtmpMode.setEnabled(finalEnable);
        bottomPanelLivePlatform.setEnabled(finalEnable);

        bottomPanelMoreItem.setEnabled(finalEnable);

        if (!bottomPanelPhotoGraphItem.isPressed()) {//这里是为了让长按事件能收到 actionUp事件
            bottomPanelPhotoGraphItem.setEnabled((presenter.getLiveAction().hasResolution && enable) || justForTest);
        }
        bottomPanelAlbumItem.setEnabled(enable || !all);
        bottomPanelAlbumItem.setAlpha(bottomPanelAlbumItem.isEnabled() ? 1 : 0.3f);
    }

    @Override
    public void onRefreshConnectionMode(int connectionType) {//1:mobile,0:wifi

        Device device = DataSourceManager.getInstance().getDevice(uuid);
        String mac = device.$(DpMsgMap.ID_202_MAC, "");
        if (TextUtils.isEmpty(mac)) {
            DeviceInformation information = BaseDeviceInformationFetcher.getInstance().getDeviceInformation();
            if (information != null && information.mac != null) {
                mac = information.mac;
            }
        }
        DpMsgDefine.DPNet net = device.$(DpMsgMap.ID_201_NET, new DpMsgDefine.DPNet());
        apMode = JFGRules.isAPDirect(uuid, mac);
        boolean isOnline = net.net > 0;
        int netType = NetUtils.getNetType(this);

        if (upgrading) return;

        bannerSwitcher.setVisibility(View.VISIBLE);
        cameraUpgrading.setVisibility(View.GONE);

        bannerConnectionIcon.setImageResource(apMode ? R.drawable.camera720_icon_ap : R.drawable.camera720_icon_wifi);
        bannerConnectionIcon.setVisibility((apMode || isOnline) ? View.VISIBLE : View.GONE);
        bannerConnectionText.setVisibility(upgrading ? View.INVISIBLE : View.VISIBLE);
        bannerConnectionText.setText(apMode ? R.string.Tap1_OutdoorMode : isOnline ? R.string.DEVICE_WIFI_ONLINE : R.string.NOT_ONLINE);
        bottomPanelAlbumItem.setEnabled(!upgrading);
        bottomPanelAlbumItem.setAlpha(bottomPanelAlbumItem.isEnabled() ? 1 : 0.3f);
        bannerChargeText.setVisibility((!apMode && !isOnline) ? View.INVISIBLE : bannerChargeText.getVisibility());
        bannerChargeIcon.setVisibility((!apMode && !isOnline) ? View.INVISIBLE : bannerChargeIcon.getVisibility());
        menuBinding.actPanoramaCameraQuickMenuItem2Voice.setEnabled(!apMode);
        menuBinding.actPanoramaCameraQuickMenuItem1Mic.setEnabled(!apMode);
        liveFlowSpeedText.setVisibility(View.INVISIBLE);
        if (apMode) {//ap 模式禁用对讲功能
            onSpeaker(false);
            onMicrophone(false);
            //ap 模式下不需要再跟新 liveRecording 了
            if (panoramaRecordMode == PANORAMA_RECORD_MODE.MODE_LIVE) {
                RxBus.getCacheInstance().post(RecordFinishEvent.INSTANCE);
            }
        }
        if ((!apMode && !isOnline) || connectionType == -1) {

            if (bannerSwitcher.getDisplayedChild() == 0) {
                bannerSwitcher.showNext();
            }
            onRefreshControllerView(false, false);
            int info = netType == -1 ? R.string.Tap1_DisconnectedPleaseCheck :
                    connectionType == -1 ? R.string.Tips_Device_TimeoutRetry : R.string.Tap1_Offline;
            showBannerTextInformation(getString(info), true);
//            bannerWarmingTitle.setText(connectionType == -1 ? R.string.Tips_Device_TimeoutRetry :
//                    netType != -1 ? R.string.Tap1_Offline : R.string.Tap1_DisconnectedPleaseCheck);
            loadingBar.setState(connectionType == -1 ? JConstant.PLAY_STATE_LOADING_FAILED : JConstant.PLAY_STATE_IDLE, null);
            RxBus.getCacheInstance().post(RecordFinishEvent.INSTANCE);

            /*播放失败了需要显示黑屏,但又不能移除 surfaceview ,因为创建 surfaceview 会使屏幕产出闪烁的效果*/
            videoLiveContainer.setVisibility(View.INVISIBLE);

            preNetType = -1;
            return;
        }

        if (bannerSwitcher.getDisplayedChild() == 1) {
            bannerSwitcher.showPrevious();
        }

        onHideBadNetWorkBanner();//不管是 WiFi 还是移动网络,都应该隐藏网络不好的 banner

        if (preNetType == -1) {
            presenter.checkAndInitRecord();
        }

        if (connectionType == ConnectivityManager.TYPE_WIFI || netType == ConnectivityManager.TYPE_WIFI) {//wifi
            AppLogger.w("正在使用 WiFi 网络,可以放心观看");

            if (mobileAlert != null && mobileAlert.isShowing()) {
                mobileAlert.dismiss();
            }
           /* if (!hasNetSetting) {*/
            loadingBar.setState(JConstant.PLAY_STATE_PREPARE, null);
            /*}*/
            if (!presenter.getLiveAction().hasResolution) {
                presenter.startViewer();
            }
            if (preNetType != connectionType) {

                if (preNetType != -1) {
                    ToastUtil.showPositiveToast(getString(R.string.Tap1_SwitchedWiFi));
                }
            }
        } else if (connectionType == ConnectivityManager.TYPE_MOBILE || netType == ConnectivityManager.TYPE_MOBILE) {//mobile
            AppLogger.w("正在使用移动网络,请注意流量");
            if (alertMobile) {
                onRefreshControllerView(false, false);
                if (mobileAlert == null) {
                    mobileAlert = new AlertDialog.Builder(this)
                            .setMessage(R.string.Tap1_Firmware_DataTips)
                            .setPositiveButton(R.string.OK, (dialog, which) -> {
//                                PreferencesUtils.putBoolean(JConstant.ALERT_MOBILE, true);
                                loadingBar.setState(JConstant.PLAY_STATE_PREPARE, null);
                               /* if (!hasNetSetting) {*/
                                alertMobile = false;//点击继续不再弹提示框了,改为弹 toast
                                presenter.startViewer();
                                /*}*/
                            })
                            .setNegativeButton(R.string.CANCEL, (dialog, which) -> {
                                loadingBar.setState(JConstant.PLAY_STATE_LOADING_FAILED, null);
//                                alertMobile = false;
                            })
                            .create();
                }
                mobileAlert.show();
            } else {
                loadingBar.setState(JConstant.PLAY_STATE_PREPARE, null);
                if (!presenter.getLiveAction().hasResolution) {
                    presenter.startViewer();
                }
                if (preNetType != connectionType) {

                    ToastUtil.showPositiveToast(getString(R.string.Tap1_SwitchedNetwork));
                }
            }

        }
        preNetType = connectionType;
    }


    @Override
    public void onRefreshVideoRecordUI(int offset, @PANORAMA_RECORD_MODE int type) {
//        AppLogger.w("onRefreshVideoRecordUI");
        if (type == PANORAMA_RECORD_MODE.MODE_LIVE) {
            panoramaViewMode = PANORAMA_VIEW_MODE.MODE_LIVE;
        } else {
            panoramaViewMode = PANORAMA_VIEW_MODE.MODE_VIDEO;
        }
        panoramaRecordMode = type;
        if (!presenter.getLiveAction().hasResolution /*&& type != PANORAMA_RECORD_MODE.MODE_LIVE*/) {
            //第二个条件是为了测试用的,
            return;
        }

        if (bottomPanelSwitcher.getDisplayedChild() == 0) {
            bottomPanelSwitcher.showNext();
        }
        if (bottomPanelAlbumItem.getVisibility() == View.VISIBLE) {
            bottomPanelAlbumItem.setVisibility(View.GONE);
        }
        if (type == PANORAMA_RECORD_MODE.MODE_LIVE) {
            if (bottomPanelMoreItem.getVisibility() != View.VISIBLE) {
                bottomPanelMoreItem.setVisibility(View.VISIBLE);
            }
            if (!bottomPanelMoreItem.isEnabled()) {
                bottomPanelMoreItem.setEnabled(true);
            }
            if (bottomPanelLivePlatform.getVisibility() == View.VISIBLE) {
                bottomPanelLivePlatform.setVisibility(View.INVISIBLE);
            }
        } else {
            if (bottomPanelMoreItem.getVisibility() == View.VISIBLE) {
                bottomPanelMoreItem.setVisibility(View.INVISIBLE);
            }
            if (bottomPanelLivePlatform.getVisibility() == View.VISIBLE) {
                bottomPanelLivePlatform.setVisibility(View.INVISIBLE);
            }
        }

        if (bottomPanelLoading.getVisibility() != View.GONE) {
            bottomPanelLoading.setVisibility(View.GONE);
        }
        if (panoramaRecordMode != PANORAMA_RECORD_MODE.MODE_SHORT && !bottomPanelPhotoGraphItem.isEnabled()) {
            bottomPanelPhotoGraphItem.setEnabled(presenter.getLiveAction().hasResolution);
        }
        bottomPanelPhotoGraphItem.setImageResource(R.drawable.camera720_icon_video_recording_selector);
        setting.setEnabled(false);
        ivt_newMessageTips.setEnabled(false);

        switch (type) {
            case PANORAMA_RECORD_MODE.MODE_LONG:
//                bottomPanelSwitcherItem2Information.setText(TimeUtils.getHHMMSS(offset * 1000L));
                int visibility = bottomPanelSwitcherItem2DotIndicator.getVisibility();
                showBottomPanelInformation(TimeUtils.getHHMMSS(offset * 1000L), visibility != View.VISIBLE);
//                bottomPanelSwitcherItem2DotIndicator.setVisibility(visibility == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                break;
            case PANORAMA_RECORD_MODE.MODE_SHORT:
                if (bottomCountDownLine.getVisibility() != View.VISIBLE) {
                    bottomCountDownLine.setVisibility(View.VISIBLE);
                }
                bottomPanelSwitcherItem2DotIndicator.setVisibility(View.GONE);
                if (countDownAnimator == null) {
                    countDownAnimator = ObjectAnimator.ofFloat(bottomCountDownLine, "scaleX", 1, 0);
                }
                if (!countDownAnimator.isRunning()) {
                    countDownAnimator.setDuration(8000);
                    countDownAnimator.setCurrentPlayTime(offset * 1000);
                    countDownAnimator.setInterpolator(new LinearInterpolator());
                    countDownAnimator.addUpdateListener(animation -> {
                        float sec = (float) animation.getAnimatedValue();
                        showBottomPanelInformation((int) (8.0f * sec + 0.5f) + "S", false);
//                        bottomPanelSwitcherItem2Information.setText((int) (8.0f * sec + 0.5f) + "S");
                        if (sec == 0) {
//                            presenter.shouldRefreshUI(false);
                            RxBus.getCacheInstance().post(RecordFinishEvent.INSTANCE);
                            onRefreshViewModeUI(PanoramaCameraContact.View.PANORAMA_VIEW_MODE.MODE_VIDEO, presenter.getLiveAction().hasResolution, false);
                            onRefreshControllerView(presenter.getLiveAction().hasResolution, false);
                        }
                    });
                    countDownAnimator.start();
                }
                break;
            case MODE_LIVE: {
                int v = bottomPanelSwitcherItem2DotIndicator.getVisibility();
                showBottomPanelInformation(TimeUtils.getHHMMSS(offset * 1000L), v != View.VISIBLE);
            }
            break;
            default:
                bottomPanelSwitcherItem2Information.setText(TimeUtils.getHHMMSS(offset * 1000L));
                bottomPanelSwitcherItem2DotIndicator.setVisibility(bottomPanelSwitcherItem2DotIndicator.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                break;
        }
    }

    @Override
    public void onReportDeviceError(int err, boolean sync) {
        onRefreshViewModeUI(panoramaViewMode, presenter.getLiveAction().hasResolution, false);
        onRefreshControllerView(presenter.getLiveAction().hasResolution, false);
        onRefreshControllerViewVisible(true);
        bottomPanelPhotoGraphItem.setEnabled(presenter.getLiveAction().hasResolution);//有长按事件,需要特殊对待
        switch (err) {
            case 150://低电量
                AppLogger.w("设备电量过低");
                ToastUtil.showNegativeToast(getString(R.string.DOOR_LOWBETTERY));
                break;
            case 2003://sd 卡没有容量
                AppLogger.w("SD 卡内存已满");
                if (sync) {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(getString(R.string.Tap1_SDCardFullyTips))
                            .setPositiveButton(getString(R.string.Tap1_SDCardFullyTips), null)
                            .create();
                    show(dialog);
                } else {
                    ToastUtil.showNegativeToast(getString(R.string.Tap1_SDCardFullyTips));
                }
                break;
            case 2004://没有 sd 卡
                AppLogger.w("未检测到 SD 卡");

                if (sync) {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(R.string.MSG_SD_OFF)
                            .setPositiveButton(R.string.OK, null)
                            .create();
                    show(dialog);
                } else {
                    ToastUtil.showNegativeToast(getString(R.string.NO_SDCARD));
                }
                break;
            case 2007://正在录像
                AppLogger.w("正在录像中...");
                presenter.checkAndInitRecord();
                break;
            case 2008://sd 卡正在格式化
                AppLogger.w("SD 卡正在格式化");
                ToastUtil.showNegativeToast(getString(R.string.Formatting));
                break;
            case 2022://sd卡识别失败，需要格式化
                AppLogger.w("SD卡识别失败,需要格式化");
                if (alertSDFormatError || !sync) {//设备会一直推消息,这里过滤掉
                    alertSDFormatError = false;
                    AlertDialog dialog1 = new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(R.string.Tap1_NeedsInitializedTips)
                            .setPositiveButton(R.string.SD_INIT, (dialog, which) -> {
                                LoadingDialog.showLoading(this, getString(R.string.SD_INFO_2), false, null);
                                presenter.formatSDCard();
                            })
                            .setNegativeButton(R.string.CANCEL, null)
                            .create();
                    show(dialog1);
                } else {
                    ToastUtil.showNegativeToast(getString(R.string.NO_SDCARD));
                }
                break;
//                }
            //小于
            case -1:
                break;
            case -2:
                ToastUtil.showNegativeToast(getString(R.string.Tap1_LessThan3sTips));
                break;
            case ERROR_CODE_HTTP_NOT_AVAILABLE: {
                if (!alertHttpNotAvailable) return;
                //松开弹
                if (sync) {
                    AlertDialog dialog1 = new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(R.string.Switch_Mode)
                            .setPositiveButton(R.string.WELL_OK, (dialog, which) -> alertHttpNotAvailable = false)
                            .setNegativeButton(R.string.Dont_Show_Again, (dialog, which) -> PreferencesUtils.putBoolean(JConstant.SWITCH_MODE_POP + ":" + uuid, alertHttpNotAvailable = false))
                            .create();
                    show(dialog1);
                } else {
                    ToastUtil.showNegativeToast(getString(R.string.Switch_Mode));
                }
            }
        }

    }

    private void dismissOther() {
    }

    private void show(AlertDialog dialog) {
        if (deviceReportDialog != null && deviceReportDialog.isShowing()) {
            deviceReportDialog.dismiss();
        }
        (deviceReportDialog = dialog).show();
    }


    @Override
    public void onNewFirmwareRsp() {
//        if (isFinishing()) return;
        AlertDialogManager.getInstance().showDialog(this,
                getString(R.string.Tap1_Device_UpgradeTips), getString(R.string.Tap1_Device_UpgradeTips),
                getString(R.string.OK), (DialogInterface dialog, int which) -> {
                    Intent intent = new Intent(this, FirmwareUpdateActivity.class);
                    intent.putExtra(JConstant.KEY_COMPONENT_NAME, this.getClass().getName());
                    intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
                    startActivity(intent);
                }, getString(R.string.CANCEL), null, false);
    }

    @Override
    public void onSingleTap(float v, float v1) {

    }

    @Override
    public void onSnapshot(Bitmap bitmap, boolean tag) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(bottomPanelAlbumItem, "scaleX", 0.7f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(bottomPanelAlbumItem, "scaleY", 0.7f, 1.2f, 1.0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(300);
        set.start();
        bottomPanelAlbumItem.setImageBitmap(bitmap);
        bottomPanelAlbumItem.setAlpha(bottomPanelAlbumItem.isEnabled() ? 1 : 0.3f);
        Schedulers.io().createWorker().schedule(() -> {
            try {

                String filePath = JConstant.MEDIA_PATH + File.separator + JConstant.PANORAMA_THUMB_PICTURE + File.separator + uuid + ".jpg";
                FileUtils.saveBitmap(bitmap, filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @OnClick(R.id.imv_toolbar_message)
    public void onEnterMessage() {
        AppLogger.w("onEnterMessage");
        startActivity(new Intent(this, PanoramaMessageWrapperActivity.class).putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid));
        ivt_newMessageTips.setShowDot(false);
        presenter.dismiss();
    }

    private Pair<DPEntity, Integer> handleUnreadCount(Device mDevice) {
        if (JFGRules.isCamera(mDevice.pid)) {
            return new Pair<>(MiscUtils.getMaxVersionEntity(mDevice.getProperty(1001), mDevice.getProperty(1002), mDevice.getProperty(1003)),
                    mDevice.$(1001, 0) + mDevice.$(1002, 0) + mDevice.$(1003, 0));
        } else if (JFGRules.isBell(mDevice.pid)) {
            return new Pair<>(MiscUtils.getMaxVersionEntity(mDevice.getProperty(1004), mDevice.getProperty(1005)),
                    mDevice.$(1004, 0) + mDevice.$(1005, 0));
        }
        return null;
    }
}
