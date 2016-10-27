package com.cylan.jiafeigou.n.view.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.n.mvp.contract.mine.MineShareToContactContract;
import com.cylan.jiafeigou.n.mvp.impl.mine.MineShareToContactPresenterImp;
import com.cylan.jiafeigou.n.view.adapter.ShareToContactAdapter;
import com.cylan.jiafeigou.utils.ToastUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 作者：zsl
 * 创建时间：2016/9/13
 * 描述：
 */
public class MineShareToContactFragment extends Fragment implements MineShareToContactContract.View {

    @BindView(R.id.iv_mine_share_to_contact_back)
    ImageView ivMineShareToContactBack;
    @BindView(R.id.iv_mine_share_to_contact_search)
    ImageView ivMineShareToContactSearch;
    @BindView(R.id.rcy_mine_share_to_contact_list)
    RecyclerView rcyMineShareToContactList;
    @BindView(R.id.ll_no_contact)
    LinearLayout llNoContact;

    private MineShareToContactContract.Presenter presenter;

    public static MineShareToContactFragment newInstance() {
        return new MineShareToContactFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frgment_mine_share_to_contact, container, false);
        ButterKnife.bind(this, view);
        initPresenter();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (presenter != null){
            presenter.start();
        }
    }

    private void initPresenter() {
        presenter = new MineShareToContactPresenterImp(this);
    }

    @Override
    public void setPresenter(MineShareToContactContract.Presenter presenter) {

    }

    @OnClick({R.id.iv_mine_share_to_contact_back, R.id.iv_mine_share_to_contact_search})
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.iv_mine_share_to_contact_back:
                getFragmentManager().popBackStack();
                break;

            case R.id.iv_mine_share_to_contact_search:
                ToastUtil.showToast(getContext(), "正在搜索...");
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (presenter != null) {
            presenter.stop();
        }
    }

    @Override
    public void initContactReclyView(ShareToContactAdapter adapter) {
        rcyMineShareToContactList.setLayoutManager(new LinearLayoutManager(getContext()));
        rcyMineShareToContactList.setAdapter(adapter);
    }

    @Override
    public void showNoContactNullView() {
        llNoContact.setVisibility(View.VISIBLE);
    }
}
