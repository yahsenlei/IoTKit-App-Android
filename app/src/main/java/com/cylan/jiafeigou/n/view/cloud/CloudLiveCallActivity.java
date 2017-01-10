package com.cylan.jiafeigou.n.view.cloud;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cylan.entity.jniCall.JFGMsgVideoResolution;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.misc.JfgCmdInsurance;
import com.cylan.jiafeigou.n.mvp.contract.cloud.CloudLiveCallContract;
import com.cylan.jiafeigou.n.mvp.impl.cloud.CloudLiveCallPresenterImp;
import com.cylan.jiafeigou.n.mvp.model.CloudLiveBaseBean;
import com.cylan.jiafeigou.n.mvp.model.CloudLiveBaseDbBean;
import com.cylan.jiafeigou.n.mvp.model.CloudLiveVideoTalkBean;
import com.cylan.jiafeigou.n.view.activity.CloudLiveActivity;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.video.VideoViewFactory;

import java.io.Serializable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 作者：zsl
 * 创建时间：2017/1/6
 * 描述：
 */
public class CloudLiveCallActivity extends AppCompatActivity implements CloudLiveCallContract.View {

    @BindView(R.id.iv_call_user_image_head)
    ImageView ivCallUserImageHead;
    @BindView(R.id.ll_top_layout)
    LinearLayout llTopLayout;
    @BindView(R.id.tv_ignore_call)
    TextView tvIgnoreCall;
    @BindView(R.id.tv_accept_call)
    TextView tvAcceptCall;
    @BindView(R.id.tv_video_time)
    Chronometer tvVideoTime;
    @BindView(R.id.tv_connet_text)
    TextView tvConnetText;
    @BindView(R.id.tv_loading)
    TextView tvLoading;
    @BindView(R.id.iv_hang_up)
    ImageView ivHangUp;
    @BindView(R.id.ll_myself_video)
    LinearLayout llMyselfVideo;
    @BindView(R.id.root_fl_video)
    FrameLayout rootFlVideoContainer;
    @BindView(R.id.rl_call_in_container)
    RelativeLayout rlCallInContainer;
    @BindView(R.id.fl_video_container)
    FrameLayout flVideoContainer;

    private CloudLiveCallContract.Presenter presenter;
    private SurfaceView mRenderSurfaceView;
    private SurfaceView mLocalSurfaceView;
    private String uuid;
    private boolean isCallIn = false;
    private static CloudMesgBackListener callBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_cloud_live_videochat_connect);
        ButterKnife.bind(this);
        this.uuid = getIntent().getStringExtra(JConstant.KEY_DEVICE_ITEM_UUID);
        this.isCallIn = getIntent().getBooleanExtra("call_in_or_out", false);
        initPresenter();
    }

    private void initPresenter() {
        presenter = new CloudLiveCallPresenterImp(this, uuid);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (presenter != null) presenter.start();
        initView();
        if (!isCallIn){
            presenter.onCloudCallConnettion();
        }
    }

    private void initView() {
        if (isCallIn) {
            rlCallInContainer.setVisibility(View.VISIBLE);
            rootFlVideoContainer.setVisibility(View.GONE);
        } else {
            rlCallInContainer.setVisibility(View.GONE);
            rootFlVideoContainer.setVisibility(View.VISIBLE);
            presenter.countTime();
        }
    }

    @OnClick({R.id.tv_ignore_call, R.id.tv_accept_call, R.id.iv_hang_up})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_ignore_call:
                finish();
                break;
            case R.id.tv_accept_call:
                rlCallInContainer.setVisibility(View.GONE);
                rootFlVideoContainer.setVisibility(View.VISIBLE);
                presenter.onCloudCallConnettion();
                break;
            case R.id.iv_hang_up:
                handlerHangUpResultData();
                presenter.stopPlayVideo();
                finish();
                break;
        }
    }

    /**
     * 各种点击挂断处理
     */
    private void handlerHangUpResultData() {
        if (presenter.getIsConnectOk()){
            if (isCallIn){
                // 呼入连接成功
                handlerCallingReuslt(JConstant.CLOUD_IN_CONNECT_OK);
            }else {
                // 呼出连接成功
                handlerCallingReuslt(JConstant.CLOUD_OUT_CONNECT_OK);
            }
        }else {
            if (isCallIn){
                // 呼入连接失败
                handlerCallingReuslt(JConstant.CLOUD_IN_CONNECT_FAILED);
            }else {
                // 呼出连接失败
                handlerCallingReuslt(JConstant.CLOUD_OUT_CONNECT_FAILED);
            }
        }
    }

    /**
     * 生成保存的消息bean
     * @param type
     * @param videoLength
     * @param hasConnet
     * @return
     */
    private CloudLiveBaseBean createCloudBackBean(int type, String videoLength, boolean hasConnet) {
        CloudLiveBaseBean newBean = new CloudLiveBaseBean();
        newBean.setType(type);
        CloudLiveVideoTalkBean newLeaveBean = new CloudLiveVideoTalkBean();
        newLeaveBean.setVideoLength(videoLength);
        newLeaveBean.setHasConnet(hasConnet);
        newLeaveBean.setVideoTime(presenter.parseTime(System.currentTimeMillis()));
        newBean.setData(newLeaveBean);
        return newBean;
    }

    /**
     * 响应设备分辨率回调
     * @param resolution
     * @throws JfgException
     */
    @Override
    public void onResolution(JFGMsgVideoResolution resolution) throws JfgException {
        hideLoadingView();
        initLocalVideoView();
        initRenderVideoView();
        JfgCmdInsurance.getCmd().setRenderLocalView(mLocalSurfaceView);
        JfgCmdInsurance.getCmd().setRenderRemoteView(mRenderSurfaceView);
    }

    /**
     * 呼叫的结果的处理
     * @param msgId
     */
    @Override
    public void handlerCallingReuslt(int msgId) {
        switch(msgId){
            case JConstant.CLOUD_IN_CONNECT_TIME_OUT:          // 中控呼入app无应答
                if (callBack != null){
                    CloudLiveBaseBean newBean = new CloudLiveBaseBean();
                    newBean.setType(1);
                    CloudLiveVideoTalkBean newLeaveBean = new CloudLiveVideoTalkBean();
                    newLeaveBean.setVideoLength("");
                    newLeaveBean.setHasConnet(false);
                    newLeaveBean.setVideoTime("");
                    newBean.setData(newLeaveBean);
                    callBack.onCloudMesgBack(newBean);
                }
                ToastUtil.showToast("未接通");
                finish();
                break;

            case JConstant.CLOUD_OUT_CONNECT_TIME_OUT:            // 呼出超时
                ToastUtil.showToast("连接超时");
                finish();
                break;

            case JConstant.CLOUD_IN_CONNECT_OK: //呼入连接成功 点击挂断按钮
                CloudLiveBaseBean newBean = createCloudBackBean(1,tvVideoTime.getText().toString(),true);
                if (callBack != null){
                    callBack.onCloudMesgBack(newBean);
                }
                //添加到数据库
                CloudLiveBaseDbBean dbBean = new CloudLiveBaseDbBean();
                dbBean.setType(1);
                dbBean.setData(presenter.getSerializedObject((Serializable) newBean.getData()));
                presenter.saveIntoDb(dbBean);

                // TODO　判断当前CloudActivity是否已经启动
                // 可能要延时
//                startActivity(new Intent(ContextUtils.getContext(), CloudLiveActivity.class)
//                        .putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid));
                break;

            case JConstant.CLOUD_IN_CONNECT_FAILED:
                CloudLiveBaseBean newBeanF = createCloudBackBean(1,"",false);
                if (callBack != null){
                    callBack.onCloudMesgBack(newBeanF);
                }
                //添加到数据库
                CloudLiveBaseDbBean dbBeanF = new CloudLiveBaseDbBean();
                dbBeanF.setType(1);
                dbBeanF.setData(presenter.getSerializedObject((Serializable) newBeanF.getData()));
                presenter.saveIntoDb(dbBeanF);

                // TODO　判断当前CloudActivity是否已经启动
                // 可能要延时
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        startActivity(new Intent(ContextUtils.getContext(), CloudLiveActivity.class)
//                                .putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid));
//                    }
//                },500);

                break;

            case JConstant.CLOUD_OUT_CONNECT_OK:
                CloudLiveBaseBean outBean = createCloudBackBean(1,tvVideoTime.getText().toString(),true);
                callBack.onCloudMesgBack(outBean);
                //添加到数据库
                CloudLiveBaseDbBean outDbBean = new CloudLiveBaseDbBean();
                outDbBean.setType(1);
                outDbBean.setData(presenter.getSerializedObject((Serializable) outBean.getData()));
                presenter.saveIntoDb(outDbBean);
                break;

            case JConstant.CLOUD_OUT_CONNECT_FAILED:
                CloudLiveBaseBean outBeanF = createCloudBackBean(1,"",false);
                callBack.onCloudMesgBack(outBeanF);
                //添加到数据库
                CloudLiveBaseDbBean outDbBeanF = new CloudLiveBaseDbBean();
                outDbBeanF.setType(1);
                outDbBeanF.setData(presenter.getSerializedObject((Serializable) outBeanF.getData()));
                presenter.saveIntoDb(outDbBeanF);
                break;
        }
    }

    private void initLocalVideoView() {
        if (mLocalSurfaceView == null){
            mLocalSurfaceView = (SurfaceView) VideoViewFactory.CreateRendererExt(false,
                    ContextUtils.getContext(), true);
            mLocalSurfaceView.setId("IVideoView".hashCode());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.width = ViewUtils.dp2px(86);
            params.height = ViewUtils.dp2px(132);
            mLocalSurfaceView.setLayoutParams(params);
            llMyselfVideo.removeAllViews();
            llMyselfVideo.addView(mLocalSurfaceView);
        }
        AppLogger.i("initRenderVideoView");
        mLocalSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mLocalSurfaceView.getHolder().setFormat(PixelFormat.OPAQUE);
    }

    private void initRenderVideoView() {
        if (mRenderSurfaceView == null) {
            mRenderSurfaceView = (SurfaceView) VideoViewFactory.CreateRendererExt(false,
                    ContextUtils.getContext(), true);
            mRenderSurfaceView.setId("IVideoView".hashCode());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRenderSurfaceView.setLayoutParams(params);
            flVideoContainer.removeAllViews();
            flVideoContainer.addView(mRenderSurfaceView);
        }
        AppLogger.i("initRenderVideoView");
        mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mRenderSurfaceView.getHolder().setFormat(PixelFormat.OPAQUE);
    }

    public static void setOnCloudMesgBackListener(CloudMesgBackListener listener){
        callBack = listener;
    }

    @Override
    public void setPresenter(CloudLiveCallContract.Presenter presenter) {

    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void showLoadingView() {
        tvLoading.setVisibility(View.VISIBLE);
        tvConnetText.setVisibility(View.VISIBLE);
        tvVideoTime.setVisibility(View.INVISIBLE);
        tvVideoTime.stop();
        llMyselfVideo.setVisibility(View.INVISIBLE);
    }

    @Override
    public void hideLoadingView() {
        tvLoading.setVisibility(View.INVISIBLE);
        tvConnetText.setVisibility(View.INVISIBLE);
        tvVideoTime.setVisibility(View.VISIBLE);
        tvVideoTime.setBase(SystemClock.elapsedRealtime());
        tvVideoTime.start();
        llMyselfVideo.setVisibility(View.VISIBLE);
    }

    @Override
    public void setLoadingText(String text) {
        tvLoading.setText(text);
    }

    @Override
    public void onLiveStop(int code) {
        switch (code){
            case JFGRules.PlayErr.ERR_NERWORK:
                ToastUtil.showNegativeToast(getString(R.string.OFFLINE_ERR_1));
                break;
            case JFGRules.PlayErr.ERR_UNKOWN:
                ToastUtil.showNegativeToast("出错了");
                break;
            case JFGRules.PlayErr.ERR_LOW_FRAME_RATE:
                ToastUtil.showNegativeToast("\"帧率太低,不足以播放,重试\"");
                break;
            case JFGRules.PlayErr.ERR_DEVICE_OFFLINE:
            case JError.ErrorVideoPeerNotExist:
                ToastUtil.showNegativeToast(getString(R.string.OFFLINE_ERR));
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (presenter != null){
            presenter.stopPlayVideo();
            presenter.stop();
        }
    }
}
