package com.cylan.jiafeigou;

import android.annotation.TargetApi;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.OnActivityReenterListener;
import com.cylan.jiafeigou.misc.RxEvent;
import com.cylan.jiafeigou.misc.SharedElementCallBackListener;
import com.cylan.jiafeigou.n.mvp.contract.home.NewHomeActivityContract;
import com.cylan.jiafeigou.n.mvp.impl.home.HomeMinePresenterImpl;
import com.cylan.jiafeigou.n.mvp.impl.home.HomePageListPresenterImpl;
import com.cylan.jiafeigou.n.mvp.impl.home.HomeWonderfulPresenterImpl;
import com.cylan.jiafeigou.n.mvp.impl.home.NewHomeActivityPresenterImpl;
import com.cylan.jiafeigou.n.view.activity.NeedLoginActivity;
import com.cylan.jiafeigou.n.view.home.HomeMineFragment;
import com.cylan.jiafeigou.n.view.home.HomePageListFragmentExt;
import com.cylan.jiafeigou.n.view.home.HomeWonderfulFragmentExt;
import com.cylan.jiafeigou.support.rxbus.RxBus;
import com.cylan.jiafeigou.widget.CustomViewPager;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NewHomeActivity extends NeedLoginActivity implements
        NewHomeActivityContract.View {
    @BindView(R.id.vp_home_content)
    CustomViewPager vpHomeContent;
    @BindView(R.id.rgLayout_home_bottom_menu)
    RadioGroup rgLayoutHomeBottomMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);
        ButterKnife.bind(this);
        setExitSharedElementCallback(mCallback);
        initBottomMenu();
        initMainContentAdapter();
        new NewHomeActivityPresenterImpl(this);
    }

    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    protected int[] getOverridePendingTransition() {
        return new int[]{R.anim.alpha_in, R.anim.alpha_out};
    }

    private void initMainContentAdapter() {
        HomeViewAdapter viewAdapter = new HomeViewAdapter(getSupportFragmentManager());
        vpHomeContent.setPagingEnabled(true);
        vpHomeContent.setAdapter(viewAdapter);
        vpHomeContent.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                final int id = position == 0 ? R.id.btn_home_list
                        : (position == 1 ? R.id.btn_home_wonderful : R.id.btn_home_mine);
                rgLayoutHomeBottomMenu.check(id);
            }
        });
    }

    private void initBottomMenu() {
        rgLayoutHomeBottomMenu.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.btn_home_list:
                        vpHomeContent.setCurrentItem(0);
                        break;
                    case R.id.btn_home_wonderful:
                        vpHomeContent.setCurrentItem(1);
                        break;
                    case R.id.btn_home_mine:
                        vpHomeContent.setCurrentItem(2);
                        break;
                }
            }
        });
    }

    @UiThread
    @Override
    public void initView() {
    }

    @Override
    public void setPresenter(NewHomeActivityContract.Presenter presenter) {
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        if (onActivityReenterListener != null)
            onActivityReenterListener.onActivityReenter(requestCode, data);
    }

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (sharedElementCallBackListener != null)
                sharedElementCallBackListener.onSharedElementCallBack(names, sharedElements);
        }
    };

    private SharedElementCallBackListener sharedElementCallBackListener;
    private OnActivityReenterListener onActivityReenterListener;

    /**
     * 主页的三个页面
     */
    class HomeViewAdapter extends FragmentPagerAdapter {
        private static final int INDEX_0 = 0;
        private static final int INDEX_1 = 1;
        private static final int INDEX_2 = 2;

        public HomeViewAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case INDEX_0: {
                    HomePageListFragmentExt fragment = HomePageListFragmentExt.newInstance(new Bundle());
                    new HomePageListPresenterImpl(fragment);
                    if (fragment != null && fragment.getContext() != null)
                        Toast.makeText(fragment.getContext(), "重新new了。。。1", Toast.LENGTH_SHORT).show();
                    return fragment;
                }
                case INDEX_1: {
                    final int bottomMenuContainerId = R.id.fLayout_new_home_bottom_menu;
                    Bundle bundle = new Bundle();
                    bundle.putInt(JConstant.KEY_NEW_HOME_ACTIVITY_BOTTOM_MENU_CONTAINER_ID,
                            bottomMenuContainerId);
                    HomeWonderfulFragmentExt fragment = HomeWonderfulFragmentExt.newInstance(bundle);
                    sharedElementCallBackListener = fragment;
                    onActivityReenterListener = fragment;
                    new HomeWonderfulPresenterImpl(fragment);
                    if (fragment != null && fragment.getContext() != null)
                        Toast.makeText(fragment.getContext(), "重新new了。。。2", Toast.LENGTH_SHORT).show();
                    return fragment;
                }
                case INDEX_2:
                    HomeMineFragment fragment = HomeMineFragment.newInstance(new Bundle());
                    new HomeMinePresenterImpl(fragment);
                    if (fragment != null && fragment.getContext() != null)
                        Toast.makeText(fragment.getContext(), "重新new了。。。3", Toast.LENGTH_SHORT).show();
                    return fragment;
            }
            return HomePageListFragmentExt.newInstance(new Bundle());
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //super.destroyItem(container, position, object);
            //复写这个函数,以免回收fragment.
        }
    }


}