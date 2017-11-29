package com.cylan.jiafeigou.n.mvp.impl.bind

import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.base.wrapper.BasePresenter
import com.cylan.jiafeigou.n.mvp.contract.bind.WireBindContract
import com.cylan.jiafeigou.support.log.AppLogger
import com.cylan.jiafeigou.utils.APObserver
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by yanzhendong on 2017/11/28.
 */
class WireBindPresenter @Inject constructor(view: WireBindContract.View) : BasePresenter<WireBindContract.View>(view)
        , WireBindContract.Presenter {
    override fun scanDogWiFi() {
        AppLogger.w("scanDogWiFi")
        val subscribe = APObserver.scanDogWiFi()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(applyLoading(R.string.addvideo_searching))
                .timeout(7, TimeUnit.SECONDS, Observable.just(null))
                .subscribe({
                    when (it) {
                        null -> {
                            mView.onScanDogWiFiTimeout()
                        }
                        else -> {
                            mView.onScanDogWiFiFinished(it)
                        }
                    }
                }) {
                    it.printStackTrace()
                    AppLogger.e(it)
                }
        addStopSubscription(subscribe)
    }


}