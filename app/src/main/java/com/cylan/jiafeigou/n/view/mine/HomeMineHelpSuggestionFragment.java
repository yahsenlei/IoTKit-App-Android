package com.cylan.jiafeigou.n.view.mine;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.n.mvp.contract.home.HomeMineHelpSuggestionContract;
import com.cylan.jiafeigou.n.mvp.impl.home.HomeMineHelpSuggestionImpl;
import com.cylan.jiafeigou.n.mvp.model.MineHelpSuggestionBean;
import com.cylan.jiafeigou.n.view.adapter.HomeMineHelpSuggestionAdapter;
import com.cylan.jiafeigou.support.softkeyboard.util.KPSwitchConflictUtil;
import com.cylan.jiafeigou.support.softkeyboard.util.KeyboardUtil;
import com.cylan.jiafeigou.support.softkeyboard.util.ViewUtil;
import com.cylan.jiafeigou.support.softkeyboard.widget.KPSwitchFSPanelLinearLayout;
import com.cylan.jiafeigou.support.superadapter.internal.SuperViewHolder;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.NetUtils;
import com.cylan.jiafeigou.utils.ToastUtil;
import com.cylan.jiafeigou.utils.ViewUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * 创建者     谢坤
 * 创建时间   2016/8/8 14:37
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class HomeMineHelpSuggestionFragment extends Fragment implements HomeMineHelpSuggestionContract.View {

    @BindView(R.id.rv_home_mine_suggestion)
    RecyclerView mRvMineSuggestion;
    @BindView(R.id.et_home_mine_suggestion)
    EditText mEtSuggestion;
    @BindView(R.id.tv_mine_help_suggestion_clear)
    TextView tvMineHelpSuggestionClear;
    @BindView(R.id.tv_home_mine_suggestion)
    TextView tvHomeMineSuggestion;
    @BindView(R.id.panel_root)
    KPSwitchFSPanelLinearLayout panelRoot;
    @BindView(R.id.iv_loading_rotate)
    ImageView ivLoadingRotate;
    @BindView(R.id.fl_loading_container)
    FrameLayout flLoadingContainer;
    @BindView(R.id.rl_home_mine_suggestion)
    FrameLayout rlHomeMineSuggestion;

    private HomeMineHelpSuggestionAdapter suggestionAdapter;
    private String suggestion;
    private HomeMineHelpSuggestionContract.Presenter presenter;
    private int itemPosition;
    private boolean resendFlag;

    public static HomeMineHelpSuggestionFragment newInstance(Bundle bundle) {
        HomeMineHelpSuggestionFragment fragment = new HomeMineHelpSuggestionFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine_help_suggestion, container, false);
        ButterKnife.bind(this, view);
        initKeyBoard();
        initPresenter();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewUtils.setViewPaddingStatusBar(rlHomeMineSuggestion);
    }

    private void initPresenter() {
        presenter = new HomeMineHelpSuggestionImpl(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (presenter != null) presenter.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (presenter != null) presenter.stop();
    }

    @OnClick({R.id.tv_mine_help_suggestion_clear, R.id.tv_home_mine_suggestion})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_mine_help_suggestion_clear:
                //弹出对话框
                showDialog();
                break;
            case R.id.tv_home_mine_suggestion:
                if (mEtSuggestion.getText().toString().length() < 10) {
                    ToastUtil.showNegativeToast(getString(R.string.Tap3_Feedback_TextFail));
                    return;
                }
                addInputItem();
                mEtSuggestion.setText("");
                break;
        }
    }

    /**
     * 自动回复
     */
    private void autoReply() {
        if (suggestionAdapter.getItemCount() == 0) {
            return;
        }
        if (suggestionAdapter.getItemCount() != 1) {
            if (presenter.checkOverTime(suggestionAdapter.getItem(suggestionAdapter.getItemCount() - 2).getDate())) {
                addAutoReply();
                presenter.getSystemAutoReply();
            }
        } else {
            addAutoReply();
            presenter.getSystemAutoReply();
        }
    }

    ;

    /**
     * 弹出对话框
     */
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
        builder.setTitle(getString(R.string.Tap3_Feedback_ClearTips))
                .setPositiveButton(getString(R.string.Tap3_Feedback_Clear), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        presenter.onClearAllTalk();
                        showLoadingDialog();
                        getView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                suggestionAdapter.clear();
                                suggestionAdapter.notifyDataSetHasChanged();
                                hideLoadingDialog();
                            }
                        }, 2000);
                    }
                })
                .setNegativeButton(getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * 用户进行反馈时添加一个自动回复的条目
     */
    @Override
    public void addAutoReply() {
        MineHelpSuggestionBean autoReplyBean = new MineHelpSuggestionBean();
        autoReplyBean.setType(0);
        autoReplyBean.setText(getString(R.string.Tap3_Feedback_AutoReply));
        autoReplyBean.setDate(System.currentTimeMillis() + "");
        suggestionAdapter.add(autoReplyBean);
        presenter.saveIntoDb(autoReplyBean);
    }

    /**
     * recycleview显示用户输入的条目
     */
    @Override
    public void addInputItem() {
        suggestion = mEtSuggestion.getText().toString();

        MineHelpSuggestionBean suggestionBean = new MineHelpSuggestionBean();
        suggestionBean.setType(1);

        suggestionBean.setText(suggestion);
        suggestionBean.setIcon(presenter.getUserPhotoUrl());
        String time = System.currentTimeMillis() + "";
        suggestionBean.setDate(time);

        if (suggestionAdapter.getItemCount() != 0) {
            if (presenter.checkOver20Min(suggestionAdapter.getList().get(suggestionAdapter.getItemCount() - 1).getDate())) {
                suggestionBean.isShowTime = true;
            } else {
                suggestionBean.isShowTime = false;
            }
        } else {
            suggestionBean.isShowTime = true;
        }

        if (NetUtils.getNetType(ContextUtils.getContext()) == -1) {
            suggestionBean.pro_falag = 1;
            presenter.saveIntoDb(suggestionBean);
            suggestionAdapter.add(suggestionBean);
            suggestionAdapter.notifyDataSetHasChanged();
            mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
            return;
        } else {
            suggestionBean.pro_falag = 0;
        }
        suggestionAdapter.add(suggestionBean);
        suggestionAdapter.notifyDataSetHasChanged();
        presenter.sendFeedBack(suggestionBean);
    }

    @Override
    public void showLoadingDialog() {
        flLoadingContainer.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.loading_progress_rotate);
        ivLoadingRotate.startAnimation(animation);
    }

    @Override
    public void hideLoadingDialog() {
        flLoadingContainer.setVisibility(View.GONE);
        ivLoadingRotate.clearAnimation();
    }

    /**
     * 系统的回复
     *
     * @param time
     * @param content
     */
    @Override
    public void addSystemAutoReply(long time, String content) {
        MineHelpSuggestionBean autoReplyBean = new MineHelpSuggestionBean();
        autoReplyBean.setType(0);
        autoReplyBean.setText(content);
        autoReplyBean.setDate(time + "");
        suggestionAdapter.add(autoReplyBean);
        suggestionAdapter.notifyDataSetHasChanged();
        presenter.saveIntoDb(autoReplyBean);
    }

    @Override
    public void refrshRecycleView(int code) {
        if (code != JError.ErrorOK) {
            if (resendFlag) {
                suggestionAdapter.getItem(itemPosition).pro_falag = 1;
                resendFlag = false;
                mRvMineSuggestion.setAdapter(suggestionAdapter);
                presenter.saveIntoDb(suggestionAdapter.getItem(itemPosition));
            } else {
                suggestionAdapter.getItem(suggestionAdapter.getItemCount() - 1).pro_falag = 1;
                mRvMineSuggestion.setAdapter(suggestionAdapter);
                presenter.saveIntoDb(suggestionAdapter.getItem(suggestionAdapter.getItemCount() - 1));
            }
        } else {
            if (resendFlag) {
                suggestionAdapter.getItem(itemPosition).pro_falag = 2;
                presenter.saveIntoDb(suggestionAdapter.getItem(itemPosition));
                resendFlag = false;
            } else {
                suggestionAdapter.getItem(suggestionAdapter.getItemCount() - 1).pro_falag = 2;
                presenter.saveIntoDb(suggestionAdapter.getItem(suggestionAdapter.getItemCount() - 1));
            }
            autoReply();
            mRvMineSuggestion.setAdapter(suggestionAdapter);
        }
        mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1); //滚动到集合最后一条显示；
    }

    /**
     * 初始化显示列表
     *
     * @param list
     */
    @Override
    public void initRecycleView(ArrayList<MineHelpSuggestionBean> list) {
        for (MineHelpSuggestionBean bean : list) {
            bean.icon = presenter.getUserPhotoUrl();
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRvMineSuggestion.setLayoutManager(layoutManager);
        suggestionAdapter = new HomeMineHelpSuggestionAdapter(getContext(), list, null);
        mRvMineSuggestion.setAdapter(suggestionAdapter);
        suggestionAdapter.setOnResendFeedBack(new HomeMineHelpSuggestionAdapter.OnResendFeedBackListener() {
            @Override
            public void onResend(SuperViewHolder holder, MineHelpSuggestionBean item, int position) {
                if (NetUtils.getNetType(ContextUtils.getContext()) == -1) {
                    ToastUtil.showToast(getString(R.string.NO_NETWORK_4));
                    return;
                }
                itemPosition = position;
                resendFlag = true;
                ImageView send_pro = (ImageView) holder.itemView.findViewById(R.id.iv_send_pro);
                send_pro.setImageDrawable(getContext().getResources().getDrawable(R.drawable.listview_loading));
                presenter.sendFeedBack(item);
                presenter.deleteOnItemFromDb(item);
            }
        });
    }

    @Override
    public void setPresenter(HomeMineHelpSuggestionContract.Presenter presenter) {
        this.presenter = presenter;
    }

    private void initKeyBoard() {
        KeyboardUtil.attach(getActivity(), panelRoot, new KeyboardUtil.OnKeyboardShowingListener() {
            @Override
            public void onKeyboardShowing(boolean isShowing) {
                mRvMineSuggestion.scrollToPosition(suggestionAdapter.getItemCount() - 1);
            }
        });
        KPSwitchConflictUtil.attach(panelRoot, mEtSuggestion,
                new KPSwitchConflictUtil.SwitchClickListener() {
                    @Override
                    public void onClickSwitch(boolean switchToPanel) {
                        if (switchToPanel) {
                            mEtSuggestion.clearFocus();
                        } else {
                            mEtSuggestion.requestFocus();
                        }
                    }
                });

        mRvMineSuggestion.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    KPSwitchConflictUtil.hidePanelAndKeyboard(panelRoot);
                }
                return false;
            }
        });
    }
}
