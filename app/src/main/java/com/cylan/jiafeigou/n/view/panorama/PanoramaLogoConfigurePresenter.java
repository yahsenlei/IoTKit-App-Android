package com.cylan.jiafeigou.n.view.panorama;

import com.cylan.jiafeigou.base.module.BasePanoramaApiHelper;
import com.cylan.jiafeigou.base.wrapper.BasePresenter;
import com.cylan.jiafeigou.support.log.AppLogger;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by yanzhendong on 2017/3/15.
 */
public class PanoramaLogoConfigurePresenter extends BasePresenter<PanoramaLogoConfigureContact.View> implements PanoramaLogoConfigureContact.Presenter {

    private boolean httpApiInitFinish;
    private String baseUrl;

    @Inject
    public PanoramaLogoConfigurePresenter(PanoramaLogoConfigureContact.View view) {
        super(view);
    }


    @Override
    public void start() {
        super.start();
        checkAndInitLogoOption();
    }

    private void checkAndInitLogoOption() {
        mSubscriptionManager.stop()
                .flatMap(ret -> BasePanoramaApiHelper.getInstance().getLogo(uuid))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rsp -> {
                    if (rsp.ret == 0) {//成功了
                        mView.onChangeLogoTypeSuccess(rsp.logtype);
                    } else {
                        // TODO: 2017/5/11 暂时不知道怎么处理
                    }
                }, e -> {
                    AppLogger.e(e);
                });
    }

    @Override
    public void changeLogoType(int position) {
        mSubscriptionManager.stop()
                .flatMap(ret -> BasePanoramaApiHelper.getInstance().setLogo(uuid, position))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rsp -> {
                    if (rsp.ret == 0) {
                        mView.onChangeLogoTypeSuccess(position);
                    } else {
                        mView.onChangeLogoTypeError(position);
                    }
                }, e -> {
                    AppLogger.e(e);
                });
    }
}
