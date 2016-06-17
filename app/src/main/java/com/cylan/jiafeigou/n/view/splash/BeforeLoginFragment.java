package com.cylan.jiafeigou.n.view.splash;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.n.NewHomeActivity;
import com.cylan.jiafeigou.n.view.login.LoginFragment;
import com.cylan.jiafeigou.n.view.login.LoginModelActivity;
import com.cylan.jiafeigou.utils.ActivityUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 登陆之前的界面，可以选择登陆，或者随便看看
 * Created by lxh on 16-6-12.
 */

public class BeforeLoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash_before_login, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    public static BeforeLoginFragment newInstance(Bundle bundle) {
        BeforeLoginFragment fragment = new BeforeLoginFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @OnClick(R.id.btn_unlogin_use)
    public void unLoginUse(View view) {
        //start home activity
        getContext().startActivity(new Intent(getContext(), NewHomeActivity.class));
        getActivity().finish();
    }

    @OnClick(R.id.btn_login_use)
    public void loginUse(View view) {
        //start login loginModelActivity
        getContext().startActivity(new Intent(getContext(), LoginModelActivity.class));
    }


}
