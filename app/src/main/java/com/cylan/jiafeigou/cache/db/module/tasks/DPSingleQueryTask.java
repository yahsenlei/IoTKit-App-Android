package com.cylan.jiafeigou.cache.db.module.tasks;

import com.cylan.entity.jniCall.JFGDPMsg;
import com.cylan.entity.jniCall.RobotoGetDataRsp;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.cache.db.impl.BaseDPTaskResult;
import com.cylan.jiafeigou.cache.db.module.DPEntity;
import com.cylan.jiafeigou.cache.db.view.DBAction;
import com.cylan.jiafeigou.cache.db.view.DBOption;
import com.cylan.jiafeigou.cache.db.view.IDPEntity;
import com.cylan.jiafeigou.cache.db.view.IDPSingleTask;
import com.cylan.jiafeigou.dp.DataPoint;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import rx.Observable;


/**
 * Created by yanzhendong on 2017/3/2.
 */

public class DPSingleQueryTask extends BaseDPTask<BaseDPTaskResult> {
    private DBOption.SingleQueryOption option;

    public DPSingleQueryTask() {
    }

    @Override
    public <R extends IDPSingleTask<BaseDPTaskResult>> R init(IDPEntity cache) throws Exception {
        this.option = cache.option(DBOption.SingleQueryOption.class);
        return super.init(cache);

    }

    @Override
    public Observable<BaseDPTaskResult> performLocal() {
        if (option.type == 1) {//one by one 精准查询
            return dpHelper.findDPMsg(entity.getUuid(), entity.getVersion(), entity.getMsgId())
                    .map(ret -> {
                        Object result = null;
                        if (ret != null && DBAction.AVAILABLE.accept(ret.action())) {
                            result = propertyParser.parser(ret.getMsgId(), ret.getBytes(), ret.getVersion());
                        }
                        return new BaseDPTaskResult().setResultCode(0).setResultResponse(result);
                    });
        } else if (option.type == 0) {
            return dpHelper.queryDPMsg(entity.getUuid(), entity.getVersion() == 0 ? Long.MAX_VALUE : entity.getVersion(), entity.getMsgId(), option.asc, option.limit)
                    .map(items -> {
                        List<DataPoint> result = null;
                        if (items != null && items.size() > 0) {
                            result = new ArrayList<>(items.size());
                            DataPoint dataPoint;
                            for (DPEntity item : items) {
                                dataPoint = propertyParser.parser(item.getMsgId(), item.getBytes(), item.getVersion());
                                result.add(dataPoint);
                            }
                        }
                        return new BaseDPTaskResult().setResultResponse(result).setResultCode(0);
                    });
        }
        return Observable.just(BaseDPTaskResult.SUCCESS);
    }

    @Override
    public Observable<BaseDPTaskResult> performServer() {
        if (entity.getVersion() == 0) {
            dpHelper.clearMsg(entity.getUuid(), entity.getMsgId());
        }
        return Observable.create((Observable.OnSubscribe<Long>) subscriber -> {
            try {
                AppLogger.w("正在发送查询请求,uuid:" + entity.getUuid() + ",version:" + entity.getVersion() + ",type" + option.type + "count:" + option.limit + "acs:" + option.asc);
                ArrayList<JFGDPMsg> params = new ArrayList<>();
                JFGDPMsg msg = new JFGDPMsg(entity.getMsgId(), entity.getVersion());
                params.add(msg);
                long seq = -1;
                if (option.type == 0) {
                    seq = appCmd.robotGetData(entity.getUuid() == null ? "" : entity.getUuid(), params, option.limit, option.asc, 0);//多请求一条数据,用来判断是否是一天最后一条
                } else if (option.type == 1) {
                    seq = appCmd.robotGetDataByTime(entity.getUuid() == null ? "" : entity.getUuid(), params, 0);
                }
                if (seq <= 0) {
                    throw new JfgException("内部错误");
                }
                subscriber.onNext(seq);
                subscriber.onCompleted();
            } catch (JfgException e) {
                AppLogger.e(MiscUtils.getErr(e));
                subscriber.onError(e);
            }
        })
                .flatMap(this::makeGetDataRspResponse)
                .flatMap(rsp -> {
                    AppLogger.w("收到从服务器返回数据!!!");
                    return parseServerRsp(rsp);
                });
    }

    protected Observable<BaseDPTaskResult> parseServerRsp(RobotoGetDataRsp rsp) {
        if (option.type == 1) {//one by one 精准查询
            Object result = null;
            if (rsp != null && rsp.map != null && rsp.map.size() > 0) {
                ArrayList<JFGDPMsg> msgs = rsp.map.entrySet().iterator().next().getValue();
                if (msgs != null && msgs.size() > 0) {
                    JFGDPMsg msg = msgs.get(0);
                    result = propertyParser.parser((int) msg.id, msg.packValue, msg.version);
                }
            }
            BaseDPTaskResult taskResult = new BaseDPTaskResult();
            taskResult.setResultCode(result == null ? -1 : 0);
            taskResult.setResultResponse(result);
            return Observable.just(taskResult);
        } else if (option.type == 0) {
            Set<DataPoint> result = new TreeSet<>();
            if (rsp != null && rsp.map != null && rsp.map.size() > 0) {
                for (Map.Entry<Integer, ArrayList<JFGDPMsg>> entry : rsp.map.entrySet()) {
                    if ((int) entity.getMsgId() == entry.getKey()) {
                        ArrayList<JFGDPMsg> msgs = entry.getValue();
                        if (msgs != null) {
                            for (JFGDPMsg msg : msgs) {
                                DataPoint dataPoint = propertyParser.parser((int) msg.id, msg.packValue, msg.version);
                                result.add(dataPoint);
                            }
                        }
                    }
                }

            }
            BaseDPTaskResult taskResult = new BaseDPTaskResult();
            taskResult.setResultCode(0);
            taskResult.setResultResponse(new ArrayList<>(result));
            return Observable.just(taskResult);
        }
        return Observable.just(BaseDPTaskResult.SUCCESS);
    }
}
