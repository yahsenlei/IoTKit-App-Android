package com.cylan.jiafeigou.n.view.cam;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cylan.entity.jniCall.JFGDPMsg;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.dp.DpUtils;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.n.base.IBaseFragment;
import com.cylan.jiafeigou.n.mvp.contract.cam.CamMessageListContract;
import com.cylan.jiafeigou.n.mvp.impl.cam.CamMessageListPresenterImpl;
import com.cylan.jiafeigou.n.mvp.model.CamMessageBean;
import com.cylan.jiafeigou.n.view.activity.CamSettingActivity;
import com.cylan.jiafeigou.n.view.activity.CameraLiveActivity;
import com.cylan.jiafeigou.n.view.adapter.CamMessageListAdapter;
import com.cylan.jiafeigou.n.view.media.CamMediaActivity;
import com.cylan.jiafeigou.n.view.panorama.PanoramaDetailActivity;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.support.superadapter.OnItemClickListener;
import com.cylan.jiafeigou.utils.AnimatorUtils;
import com.cylan.jiafeigou.utils.ListUtils;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.NetUtils;
import com.cylan.jiafeigou.utils.TimeUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.LoadingDialog;
import com.cylan.jiafeigou.widget.wheel.WonderIndicatorWheelView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.cylan.jiafeigou.n.view.media.CamMediaActivity.KEY_BUNDLE;
import static com.cylan.jiafeigou.n.view.media.CamMediaActivity.KEY_INDEX;
import static com.cylan.jiafeigou.support.photoselect.helpers.Constants.REQUEST_CODE;


/**
 * A simple {@link Fragment} subclass.
 */
public class CamMessageListFragment extends IBaseFragment<CamMessageListContract.Presenter>
        implements CamMessageListContract.View, SwipeRefreshLayout.OnRefreshListener,
        View.OnClickListener, OnItemClickListener {


    @BindView(R.id.tv_cam_message_list_date)
    TextView tvCamMessageListDate;
    @BindView(R.id.tv_cam_message_list_edit)
    TextView tvCamMessageListEdit;
    @BindView(R.id.rv_cam_message_list)
    RecyclerView rvCamMessageList;
    @BindView(R.id.srLayout_cam_list_refresh)
    SwipeRefreshLayout srLayoutCamListRefresh;
    @BindView(R.id.fLayout_cam_message_list_timeline)
    WonderIndicatorWheelView fLayoutCamMessageListTimeline;
    @BindView(R.id.fLayout_cam_msg_edit_bar)
    FrameLayout fLayoutCamMsgEditBar;
    @BindView(R.id.lLayout_no_message)
    LinearLayout lLayoutNoMessage;
    @BindView(R.id.rLayout_cam_message_list_top)
    FrameLayout rLayoutCamMessageListTop;
    @BindView(R.id.tv_msg_full_select)
    TextView tvMsgFullSelect;
    @BindView(R.id.tv_msg_delete)
    TextView tvMsgDelete;

//    private SimpleDialogFragment simpleDialogFragment;
    /**
     * 列表第一条可见item的position,用户刷新timeLine控件的位置。
     */
    private int currentPosition = -1;
    private CamMessageListAdapter camMessageListAdapter;
    private String uuid;

    /**
     * 加载更多
     */
    private boolean endlessLoading = false;
    private boolean mIsLastLoadFinish = true;


    public CamMessageListFragment() {
        // Required empty public constructor
    }

    public static CamMessageListFragment newInstance(Bundle bundle) {
        CamMessageListFragment fragment = new CamMessageListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.uuid = getArguments().getString(JConstant.KEY_DEVICE_ITEM_UUID);
        basePresenter = new CamMessageListPresenterImpl(this, uuid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cam_message_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mockView != null) {
            this.tvCamMessageListEdit.setVisibility(View.INVISIBLE);
            this.tvCamMessageListEdit = mockView;
        }
        this.tvCamMessageListEdit.setEnabled(false);
        srLayoutCamListRefresh.setColorSchemeColors(getResources().getColor(R.color.color_36BDFF));
        srLayoutCamListRefresh.setOnRefreshListener(this);
        camMessageListAdapter = new CamMessageListAdapter(this.uuid, getContext(), null, null);
        rvCamMessageList.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (Exception e) {
                    AppLogger.e("homepageList" + e.getMessage());
                }
            }
        });
//        camMessageListAdapter.setOnItemClickListener(this);
        rvCamMessageList.setAdapter(camMessageListAdapter);
        rvCamMessageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int pastVisibleItems, visibleItemCount, totalItemCount;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final int fPos = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                setCurrentPosition(fPos);
                if (dy > 0) { //check for scroll down
                    visibleItemCount = camMessageListAdapter.getLayoutManager().getChildCount();
                    totalItemCount = camMessageListAdapter.getLayoutManager().getItemCount();
                    pastVisibleItems = ((LinearLayoutManager) camMessageListAdapter.getLayoutManager()).findFirstVisibleItemPosition();
                    if (!endlessLoading && mIsLastLoadFinish) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            endlessLoading = true;
                            mIsLastLoadFinish = false;
                            Log.d("tag", "tag.....load more");
                            startRequest(false);
                        }
                    }
                }
            }
        });
        camMessageListAdapter.setOnclickListener(this);
        tvCamMessageListEdit.setVisibility(JFGRules.isShareDevice(uuid) && !JFGRules.isPan720(getDevice().pid) ? View.INVISIBLE : View.VISIBLE)
        ;

    }

    private void setupFootView() {
        CamMessageBean bean = new CamMessageBean();
        bean.viewType = CamMessageBean.ViewType.FOOT;
        if (camMessageListAdapter.getCount() > 0)
            bean.version = camMessageListAdapter.getItem(camMessageListAdapter.getCount() - 1).version + 1;
        rvCamMessageList.post(() -> camMessageListAdapter.add(bean));
    }

    @Override
    public void onStart() {
        super.onStart();
        //从通知栏跳进来
        boolean needRefresh = false;
        if (getActivity() != null && getActivity().getIntent() != null) {
            if (getActivity().getIntent().hasExtra(JConstant.KEY_JUMP_TO_MESSAGE)) {//需要每次进来都刷新数据,
                //客户端绑定门铃一代设备之后，门铃按呼叫键呼叫生成呼叫记录；
                //客户端进入“功能设置”界面点击“清空呼叫记录”，之后返回门铃的“消息”界面，呼叫记录依旧存在；iOS客户端的呼叫记录已经被清空；

                Intent intent = getActivity().getIntent();
                if (intent != null) intent.removeExtra(JConstant.KEY_JUMP_TO_MESSAGE);
                AppLogger.d("刷新数据中...");

                needRefresh = true;
            }
        }
        if (RxBus.getCacheInstance().hasStickyEvent(RxEvent.ClearDataEvent.class)) {
            needRefresh = true;
            camMessageListAdapter.clear();
            RxBus.getCacheInstance().removeStickyEvent(RxEvent.ClearDataEvent.class);
        }
        if (getArguments() != null && getArguments().getBoolean(JConstant.KEY_JUMP_TO_MESSAGE, false)) {
            //需要刷新
            needRefresh = true;
        }
        if (needRefresh) startRequest(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        //刚刚进入页面，尽量少点加载

        if (isVisibleToUser && basePresenter != null && getActivity() != null && isResumed()) {
//            if (camMessageListAdapter.getCount() == 0)
            startRequest(true);//需要每次刷新,而不是第一次刷新
            ViewUtils.setRequestedOrientation(getActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * @param asc：true:向前，false:向后(loadMore)
     */
    private void startRequest(boolean asc) {
        long time = 0;
        if (asc) {
            srLayoutCamListRefresh.setRefreshing(true);
            if (camMessageListAdapter.getCount() > 0)
                time = camMessageListAdapter.getItem(0).version;
        } else {
            if (!camMessageListAdapter.hasFooter())
                setupFootView();
            if (camMessageListAdapter.getCount() > 0)
                time = camMessageListAdapter.getItem(camMessageListAdapter.getCount() - 1).version;
        }
        if (basePresenter != null) basePresenter.fetchMessageList(time, asc, false);
    }

    /**
     * 更新顶部时间导航器
     *
     * @param position
     */
    private void setCurrentPosition(int position) {
        if (getView() != null && isAdded()) getView().post(() -> {
            if (camMessageListAdapter.getCount() == 0) return;
            long time = camMessageListAdapter.getList().get(0).version;
            boolean isToday = TimeUtils.isToday(time);
            String content = String.format(TimeUtils.getSuperString(time) + "%s", isToday ? "(" + getString(R.string.DOOR_TODAY) + ")" : "");
            tvCamMessageListDate.setText(content);
        });
    }


    @Override
    public void onDateMapRsp(List<WonderIndicatorWheelView.WheelItem> dateMap) {
        fLayoutCamMessageListTimeline.init(dateMap);
        fLayoutCamMessageListTimeline.setListener(time -> {
            AppLogger.d("scroll date： " + TimeUtils.getDayInMonth(time));
            if (basePresenter != null)
                basePresenter.fetchMessageList(TimeUtils.getSpecificDayEndTime(time), false, false);
            camMessageListAdapter.clear();
            LoadingDialog.showLoading(getActivity());
            boolean isToday = TimeUtils.isToday(time);
            String content = String.format(TimeUtils.getSuperString(time) + "%s", isToday ? "(" + getString(R.string.DOOR_TODAY) + ")" : "");
            tvCamMessageListDate.setText(content);
            boolean reset = tvCamMessageListDate.getTag() == null ||
                    ((int) tvCamMessageListDate.getTag() == R.drawable.wonderful_arrow_down);
            tvCamMessageListEdit.setEnabled(camMessageListAdapter.getCount() > 0 && reset);
        });

        LoadingDialog.dismissLoading();
    }

    @Override
    public void onListAppend(ArrayList<CamMessageBean> beanArrayList) {
        endlessLoading = false;
        mIsLastLoadFinish = true;
        srLayoutCamListRefresh.setRefreshing(false);
        LoadingDialog.dismissLoading();
        final int count = beanArrayList == null ? 0 : beanArrayList.size();
        if (count == 0) {
            AppLogger.i("没有数据");
            ToastUtil.showToast(getString(R.string.Loaded));
            return;
        }
        lLayoutNoMessage.post(() -> {
            makeSureRemoveFoot();
            camMessageListAdapter.addAll(beanArrayList);
            setCurrentPosition(0);
            lLayoutNoMessage.setVisibility(camMessageListAdapter.getCount() > 0 ? View.GONE : View.VISIBLE);
            rLayoutCamMessageListTop.setVisibility(camMessageListAdapter.getCount() == 0 ? View.GONE : View.VISIBLE);
            boolean reset = tvCamMessageListDate.getTag() == null ||
                    ((int) tvCamMessageListDate.getTag() == R.drawable.wonderful_arrow_down);
            tvCamMessageListEdit.setEnabled(camMessageListAdapter.getCount() > 0 && reset);
        });
    }

    private void makeSureRemoveFoot() {
        if (camMessageListAdapter.hasFooter() && getView() != null)
            camMessageListAdapter.remove(camMessageListAdapter.getItemCount() - 1);
    }

    @Override
    public void onListInsert(ArrayList<CamMessageBean> beanArrayList, int position) {
        endlessLoading = false;
        mIsLastLoadFinish = true;
        srLayoutCamListRefresh.setRefreshing(false);
        lLayoutNoMessage.post(() -> {
            makeSureRemoveFoot();
            int size = ListUtils.getSize(beanArrayList);
            for (int i = size - 1; i >= 0; i--)
                camMessageListAdapter.add(0, beanArrayList.get(i));//beanArrayList是一个降序
            lLayoutNoMessage.setVisibility(camMessageListAdapter.getCount() > 0 ? View.GONE : View.VISIBLE);
            rLayoutCamMessageListTop.setVisibility(camMessageListAdapter.getCount() == 0 ? View.GONE : View.VISIBLE);
        });
    }


    @Override
    public ArrayList<CamMessageBean> getList() {
        return (ArrayList<CamMessageBean>) camMessageListAdapter.getList();
    }

    @Override
    public void deviceInfoChanged(int id, JFGDPMsg o) throws IOException {
        // TODO: 2017/8/17 为什么消息页会需要监听同步消息? #118120
        // TODO:Android（1.1.0.534）局域网（公网），设备插入坏卡（显示读写失败-22），在报警消息页面刷新，会一直发初始化的消息，如图
        final int lPos = ((LinearLayoutManager) rvCamMessageList.getLayoutManager())
                .findLastVisibleItemPosition();
        switch (id) {
            case DpMsgMap.ID_201_NET:
                DpMsgDefine.DPNet net = DpUtils.unpackData(o.packValue, DpMsgDefine.DPNet.class);
                if (net == null) net = new DpMsgDefine.DPNet();
                camMessageListAdapter.notifyDeviceOnlineState(net.net > 0, lPos);
                break;
            case DpMsgMap.ID_222_SDCARD_SUMMARY:
//                rvCamMessageList.postDelayed(() -> {
                // TODO: 2017/8/17 只标志下当前有没有 SD 卡,而不把这条消息加入到 adapter 里去,@see:#118120
                try {
                    DpMsgDefine.DPSdcardSummary summary = DpUtils.unpackData(o.packValue, DpMsgDefine.DPSdcardSummary.class);
                    camMessageListAdapter.notifySdcardStatus(summary != null && summary.errCode == 0 && summary.hasSdcard, lPos);
//                    if (summary != null) {
//                        camMessageListAdapter.setCurrentSDcardSummary(summary);
//                    }
////                    if (summary == null) summary = new DpMsgDefine.DPSdcardSummary();
////                    camMessageListAdapter.notifySdcardStatus(summary.hasSdcard, lPos);
////                    CamMessageBean bean = new CamMessageBean();
////                    bean.sdcardSummary = summary;
////                    bean.id = DpMsgMap.ID_222_SDCARD_SUMMARY;
////                    bean.version = o.version;
////                    ArrayList<CamMessageBean> totalList = camMessageListAdapter.getSelectedItems();
////                    if (totalList != null && totalList.contains(bean)) {
////                        return;//重复了
////                    }
////                    camMessageListAdapter.add(0, bean);
////                    rvCamMessageList.scrollToPosition(0);
////                    lLayoutNoMessage.setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//        }
//                , 200);
                break;
        }

    }

    @Override
    public void onErr() {
        srLayoutCamListRefresh.post(() -> {
            srLayoutCamListRefresh.setRefreshing(false);
            makeSureRemoveFoot();
        });
        if (getView() != null && getActivity() != null) {
            LoadingDialog.dismissLoading();
        }
    }

    @Override
    public void onMessageDeleteSuc() {
        ToastUtil.showPositiveToast(getString(R.string.DELETED_SUC));
        if (getView() != null && getActivity() != null) {
            LoadingDialog.dismissLoading();
        }
        boolean reset = tvCamMessageListDate.getTag() == null ||
                ((int) tvCamMessageListDate.getTag() == R.drawable.wonderful_arrow_down);

        tvCamMessageListEdit.setEnabled(camMessageListAdapter.getCount() > 0 && reset);
    }

    @Override
    public void loadingDismiss() {
        makeSureRemoveFoot();
        LoadingDialog.dismissLoading();
    }

    @Override
    public boolean isUserVisible() {
        return getUserVisibleHint();
    }

    @Override
    public void setPresenter(CamMessageListContract.Presenter presenter) {
        this.basePresenter = presenter;
    }

    @Override
    public void onRefresh() {
        if (NetUtils.getJfgNetType(getContext()) == 0) {
            srLayoutCamListRefresh.setRefreshing(false);
            ToastUtil.showToast(getString(R.string.OFFLINE_ERR_1));
            return;
        }
//        srLayoutCamListRefresh.setRefreshing(true);
        startRequest(true);
    }

    @OnClick({R.id.tv_cam_message_list_date,
            R.id.tv_cam_message_list_edit,
            R.id.tv_msg_full_select,
            R.id.tv_msg_delete})
    public void onBindClick(View view) {
        final int lPos = ((LinearLayoutManager) rvCamMessageList.getLayoutManager())
                .findLastVisibleItemPosition();
        switch (view.getId()) {
            case R.id.tv_cam_message_list_date:
//                if (camMessageListAdapter.getCount() == 0&&)
//                    return;//呼入呼出
                if (TextUtils.equals(getString(R.string.CANCEL), tvCamMessageListEdit.getText()))
                    return;
                if (basePresenter != null && basePresenter.getDateList().size() == 0) {
                    LoadingDialog.showLoading(getActivity(), getString(R.string.LOADING));
                    AppLogger.d("日期加载中...");
                    basePresenter.refreshDateList(true);
                }
                boolean reset = tvCamMessageListDate.getTag() == null ||
                        ((int) tvCamMessageListDate.getTag() == R.drawable.wonderful_arrow_down);
                tvCamMessageListDate.setTag(reset ? R.drawable.wonderful_arrow_up : R.drawable.wonderful_arrow_down);
                ViewUtils.setDrawablePadding(tvCamMessageListDate, reset ? R.drawable.wonderful_arrow_up : R.drawable.wonderful_arrow_down, 2);
                if (reset) {
                    AnimatorUtils.slideIn(fLayoutCamMessageListTimeline, false);
                } else {
                    AnimatorUtils.slideOut(fLayoutCamMessageListTimeline, false);
                }
                tvCamMessageListEdit.setEnabled(!reset);
                break;
            case R.id.tv_cam_message_list_edit:
                if (camMessageListAdapter.getCount() == 0) return;
                String content = ((TextView) view).getText().toString();
                boolean toEdit = TextUtils.equals(content, getString(R.string.EDIT_THEME));
                if (toEdit) tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                camMessageListAdapter.reverseMode(toEdit, lPos);
                if (camMessageListAdapter.isEditMode())
                    AnimatorUtils.slideIn(fLayoutCamMsgEditBar, false);
                else {
                    AnimatorUtils.slideOut(fLayoutCamMsgEditBar, false);
                }
                ((TextView) view).setText(getString(toEdit ? R.string.CANCEL
                        : R.string.EDIT_THEME));
                break;
            case R.id.tv_msg_full_select://全选
                boolean selectAll = TextUtils.equals(tvMsgFullSelect.getText(), getString(R.string.SELECT_ALL));
                if (selectAll) {
                    camMessageListAdapter.markAllAsSelected(true, lPos);
                    tvMsgFullSelect.setText(getString(R.string.CANCEL));
                } else {
                    camMessageListAdapter.markAllAsSelected(false, lPos);
                    tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                }
                tvMsgDelete.setEnabled(camMessageListAdapter.getSelectedItems().size() > 0);
                break;
            case R.id.tv_msg_delete://删除
                final ArrayList<CamMessageBean> list = new ArrayList<>(camMessageListAdapter.getSelectedItems());
                if (ListUtils.isEmpty(list)) return;
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.Tips_SureDelete)
                        .setPositiveButton(R.string.OK, (dialog, which) -> {
                            camMessageListAdapter.removeAll(list);
                            if (basePresenter != null)
                                basePresenter.removeItems(list);
                            camMessageListAdapter.reverseMode(false, camMessageListAdapter.getCount());
                            AnimatorUtils.slideOut(fLayoutCamMsgEditBar, false);
                            tvCamMessageListEdit.setText(getString(R.string.EDIT_THEME));
                            LoadingDialog.showLoading(getActivity(), getString(R.string.DELETEING), false, null);
                        })
                        .setNegativeButton(R.string.CANCEL, null)
                        .setCancelable(false)
                        .show();
                break;
        }
    }


    @Override
    public void onClick(View v) {
        final int position = ViewUtils.getParentAdapterPosition(rvCamMessageList, v,
                R.id.lLayout_cam_msg_container);
        switch (v.getId()) {
            case R.id.tv_cam_message_item_delete: {//删除选中
                getAlertDialogManager().showDialog(getActivity(), getString(R.string.Tips_SureDelete), getString(R.string.Tips_SureDelete),
                        getString(R.string.OK), (DialogInterface dialog, int which) -> {
                            ArrayList<CamMessageBean> list = new ArrayList<>(camMessageListAdapter.getSelectedItems());
                            if (camMessageListAdapter.getCount() > position) {
                                list.add(camMessageListAdapter.getItem(position));
                            }
                            camMessageListAdapter.removeAll(list);
                            if (basePresenter != null)
                                basePresenter.removeItems(list);
                            LoadingDialog.showLoading(getActivity(), getString(R.string.DELETEING), false, null);
                        }, getString(R.string.CANCEL), null, false);
            }
            break;
            case R.id.lLayout_cam_msg_container: {//点击item,选中
                if (!camMessageListAdapter.isEditMode()) return;
                boolean itemSelected = camMessageListAdapter.markItemSelected(position);
                int size = ListUtils.getSize(camMessageListAdapter.getSelectedItems());
                tvMsgDelete.setEnabled(size > 0);
                if (!itemSelected) {
                    tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                }
            }
            break;
            case R.id.imgV_cam_message_pic0:
                if (!camMessageListAdapter.isEditMode()) {//编辑模式下点击不应该进入详情页
                    startActivity(getIntent(position, 0));
                } else {
                    boolean itemSelected = camMessageListAdapter.markItemSelected(position);
                    int size = ListUtils.getSize(camMessageListAdapter.getSelectedItems());
                    tvMsgDelete.setEnabled(size > 0);
                    if (!itemSelected) {
                        tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                    }
                }
                break;
            case R.id.imgV_cam_message_pic1:
                if (!camMessageListAdapter.isEditMode()) {//编辑模式下点击不应该进入详情页
                    startActivity(getIntent(position, 1));
                } else {
                    boolean itemSelected = camMessageListAdapter.markItemSelected(position);
                    int size = ListUtils.getSize(camMessageListAdapter.getSelectedItems());
                    tvMsgDelete.setEnabled(size > 0);
                    if (!itemSelected) {
                        tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                    }
                }
                break;
            case R.id.imgV_cam_message_pic2:
                if (!camMessageListAdapter.isEditMode()) {//编辑模式下点击不应该进入详情页
                    startActivity(getIntent(position, 2));
                } else {
                    boolean itemSelected = camMessageListAdapter.markItemSelected(position);
                    int size = ListUtils.getSize(camMessageListAdapter.getSelectedItems());
                    tvMsgDelete.setEnabled(size > 0);
                    if (!itemSelected) {
                        tvMsgFullSelect.setText(getString(R.string.SELECT_ALL));
                    }
                }
                break;
            case R.id.tv_jump_next: {
                try {
                    if (!JFGRules.isDeviceOnline(uuid)) {
                        ToastUtil.showToast(getString(R.string.NOT_ONLINE));
                        return;
                    }
                    CamMessageBean bean = camMessageListAdapter.getItem(position);
                    boolean jumpNext = bean != null && bean.alarmMsg != null && bean.sdcardSummary == null || bean.bellCallRecord != null;
                    if (jumpNext) {
                        if (JFGRules.isPan720(getDevice().pid)) {
                            startActivity(getIntent(position, 0));
                        } else {
                            Activity activity = getActivity();
                            if (activity != null && activity instanceof CameraLiveActivity) {
                                Bundle bundle = new Bundle();
//                            long time = bean.alarmMsg.version;//1498194000
                                long time = MiscUtils.getVersion(bean);
                                bundle.putLong(JConstant.KEY_CAM_LIVE_PAGE_PLAY_HISTORY_TIME, time);
                                bundle.putBoolean(JConstant.KEY_CAM_LIVE_PAGE_PLAY_HISTORY_INIT_WHEEL, true);
                                ((CameraLiveActivity) activity).addPutBundle(bundle);
                            }
                        }
                        AppLogger.d("alarm: " + bean);
                    } else {
                        Intent intent = new Intent(getActivity(), CamSettingActivity.class);
                        intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
//                        intent.putExtra(JConstant.KEY_JUMP_TO_CAM_DETAIL, true);
                        startActivityForResult(intent, REQUEST_CODE,
                                ActivityOptionsCompat.makeCustomAnimation(getActivity(),
                                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
                    }
                    AppLogger.d("jump next: " + jumpNext);
                } catch (Exception e) {
                    AppLogger.e("err: " + e.getLocalizedMessage());
                }
                break;
            }
        }
    }

    private Intent getIntent(int position, int index) {
        //720 和 普通报警消息进入的页面不一样
        Intent intent = null;
        CamMessageBean item = camMessageListAdapter.getItem(position);
        if (JFGRules.isPan720(getDevice().pid)) {
            // TODO: 2017/8/3 720 消息详情页
// intent= new Intent(getActivity(), PanoramaDetailActivity.class);
            intent = PanoramaDetailActivity.getIntentFromMessage(getActivity(), uuid, item, position, index + 1);
            return intent;
        } else {

            intent = new Intent(getActivity(), CamMediaActivity.class);
            intent.putExtra(KEY_INDEX, index);
            intent.putExtra(KEY_BUNDLE, item);
            intent.putExtra(JConstant.KEY_DEVICE_ITEM_IS_BELL, item.bellCallRecord != null);
            intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
            Log.d("imgV_cam_message_pic_0", "imgV_cam_:" + position + " " + camMessageListAdapter.getItem(position).alarmMsg);
        }

        return intent;
    }

    @Override
    public void onItemClick(View itemView, int viewType, int position) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @OnClick(R.id.tv_msg_delete)
    public void onClick() {
    }


    public void hookEdit(TextView tvToolbarRight) {
        this.mockView = tvToolbarRight;
        this.mockView.setId(R.id.tv_cam_message_list_edit);
        this.mockView.setOnClickListener(this::onBindClick);
    }

    private TextView mockView;
}
