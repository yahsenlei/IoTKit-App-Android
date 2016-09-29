package com.cylan.jiafeigou.n.view.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.n.mvp.contract.mine.MineSetRemarkNameContract;
import com.cylan.jiafeigou.n.mvp.impl.mine.MineSetRemarkNamePresenterImp;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.cylan.jiafeigou.utils.ToastUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 作者：zsl
 * 创建时间：2016/9/23
 * 描述：
 */
public class MineSetRemarkNameFragment extends Fragment implements MineSetRemarkNameContract.View {

    @BindView(R.id.iv_top_bar_left_back)
    ImageView ivTopBarLeftBack;
    @BindView(R.id.iv_mine_set_remarkname_bind)
    ImageView ivMineSetRemarknameBind;
    @BindView(R.id.et_mine_set_remarkname_new_name)
    EditText etMineSetRemarknameNewName;
    @BindView(R.id.view_mine_personal_set_remarkname_new_name_line)
    View viewMinePersonalSetRemarknameNewNameLine;
    @BindView(R.id.iv_mine_personal_set_remarkname_clear)
    ImageView ivMinePersonalSetRemarknameClear;

    private MineSetRemarkNameContract.Presenter presenter;

    private OnSetRemarkNameListener listener;

    public interface OnSetRemarkNameListener {
        void remarkNameChange(String name);
    }

    public void setOnSetRemarkNameListener(OnSetRemarkNameListener listener) {
        this.listener = listener;
    }

    public static MineSetRemarkNameFragment newInstance(Bundle bundle) {
        MineSetRemarkNameFragment fragment = new MineSetRemarkNameFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_remark_name, container, false);
        ButterKnife.bind(this, view);
        initPresenter();
        return view;
    }

    private void initPresenter() {
        presenter = new MineSetRemarkNamePresenterImp(this);
    }

    @Override
    public void setPresenter(MineSetRemarkNameContract.Presenter presenter) {

    }

    private void initEditListener() {
        etMineSetRemarknameNewName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmpty = TextUtils.isEmpty(getEditName());
                if(isEmpty){
                    ivMineSetRemarknameBind.setImageDrawable(getResources().getDrawable(R.drawable.icon_finish_disable));
                    ivMineSetRemarknameBind.setEnabled(false);
                }else {
                    ivMineSetRemarknameBind.setImageDrawable(getResources().getDrawable(R.drawable.icon_finish));
                    ivMineSetRemarknameBind.setEnabled(true);
                }

            }
        });
    }

    @OnClick({R.id.iv_top_bar_left_back, R.id.iv_mine_set_remarkname_bind, R.id.iv_mine_personal_set_remarkname_clear})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_top_bar_left_back:
                getFragmentManager().popBackStack();
                break;
            case R.id.iv_mine_set_remarkname_bind:
                if (presenter.isEditEmpty(getEditName())) {
                    ToastUtil.showToast(getContext(), "备注名不能为空");
                    return;
                } else {
                    PreferencesUtils.putString(getActivity(), getEditName(), "username");
                    ToastUtil.showToast(getContext(), "备注成功");
                    if (listener != null) {
                        listener.remarkNameChange(getEditName());
                    }
                    getFragmentManager().popBackStack();
                }
                break;
            case R.id.iv_mine_personal_set_remarkname_clear:
                etMineSetRemarknameNewName.setText("");
                break;
        }
    }

    @Override
    public String getEditName() {
        return etMineSetRemarknameNewName.getText().toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(TextUtils.isEmpty(getEditName())){
            ivMineSetRemarknameBind.setImageDrawable(getResources().getDrawable(R.drawable.icon_finish_disable));
            ivMineSetRemarknameBind.setEnabled(false);
        }else {
            ivMineSetRemarknameBind.setImageDrawable(getResources().getDrawable(R.drawable.icon_finish));
            ivMineSetRemarknameBind.setEnabled(true);
        }
    }
}