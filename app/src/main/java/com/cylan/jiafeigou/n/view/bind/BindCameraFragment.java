package com.cylan.jiafeigou.n.view.bind;


import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.n.base.IBaseFragment;
import com.cylan.jiafeigou.n.mvp.contract.bind.BindDeviceContract;
import com.cylan.jiafeigou.n.mvp.impl.bind.BindDevicePresenterImpl;
import com.cylan.jiafeigou.utils.ActivityUtils;
import com.cylan.jiafeigou.utils.AnimatorUtils;
import com.cylan.jiafeigou.utils.ListUtils;
import com.cylan.jiafeigou.utils.ViewUtils;
import com.cylan.jiafeigou.widget.CustomToolbar;
import com.nineoldandroids.animation.Animator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.cylan.jiafeigou.misc.JConstant.KEY_AUTO_SHOW_BIND;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BindCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BindCameraFragment extends IBaseFragment<BindDeviceContract.Presenter> implements BindDeviceContract.View {
    public static final String KEY_SUB_FRAGMENT_ID = "sub_key_id";
    public static final String KEY_DEVICE_LIST = "key_device_list";

    @BindView(R.id.imgV_camera_wifi_light_flash)
    ImageView imgVCameraWifiLightFlash;
    @BindView(R.id.imgV_camera_hand)
    ImageView imgVCameraHand;
    @BindView(R.id.imgV_camera_red_dot)
    ImageView imgVCameraRedDot;
    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;
    private Animator animator;

    public BindCameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create activity_cloud_live_mesg_call_out_item new instance of
     * this fragment using the provided parameters.
     *
     * @param bundle Parameter 2.
     * @return A new instance of fragment BindCameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BindCameraFragment newInstance(Bundle bundle) {
        BindCameraFragment fragment = new BindCameraFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.basePresenter = new BindDevicePresenterImpl(this);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        customToolbar.setBackAction(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null && getActivity().getIntent() != null) {
                    if (!TextUtils.isEmpty(getActivity().getIntent().getStringExtra(KEY_AUTO_SHOW_BIND))) {
                        //从wifi配置跳过来
                        getActivity().finish();
                        return;
                    }
                }
                if (getActivity() != null)
                    getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        initAnimation();
    }

//    @Override
//    protected int getSubContentViewId() {
//        return R.layout.fragment_bind_camera;
//    }


    private void initAnimation() {
        animator = AnimatorUtils.onHandMoveAndFlash(imgVCameraHand, imgVCameraRedDot, imgVCameraWifiLightFlash);
        animator.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (animator != null && animator.isRunning())
            animator.cancel();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: inflate activity_cloud_live_mesg_call_out_item fragment view
        View rootView = inflater.inflate(R.layout.fragment_bind_camera, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @OnClick(R.id.tv_bind_camera_tip)
    public void onClick() {
        if (getView() != null)
            ViewUtils.deBounceClick(getView().findViewById(R.id.tv_bind_camera_tip));
        Bundle bundle = new Bundle();
        bundle.putString(JConstant.KEY_BIND_DEVICE, getString(R.string.DOG_CAMERA_NAME));
        BindGuideFragment fragment = BindGuideFragment.newInstance(bundle);
        ActivityUtils.addFragmentSlideInFromRight(getActivity().getSupportFragmentManager(),
                fragment, android.R.id.content);
        cancelAnimation();
    }


    @Override
    public void onDevicesRsp(List<ScanResult> resultList) {
        final int count = ListUtils.getSize(resultList);
        if (count == 0) {
            Toast.makeText(getContext(), getString(R.string.Tap1_Index_NoDevice), Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void cancelAnimation() {
        if (animator != null && animator.isRunning())
            animator.cancel();
    }

    @Override
    public void onNoListError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(getContext(), getString(R.string.turn_on_gps), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getString(R.string.Tap1_Index_NoDevice), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNoJFGDevices() {
        Toast.makeText(getContext(), getString(R.string.Tap1_Index_NoDevice), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setPresenter(BindDeviceContract.Presenter presenter) {
        this.basePresenter = presenter;
    }
}
