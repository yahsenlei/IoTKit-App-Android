package com.cylan.jiafeigou.n.view.panorama

import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.base.injector.component.FragmentComponent
import com.cylan.jiafeigou.base.wrapper.BaseFragment

/**
 * Created by yanzhendong on 2017/9/5.
 */
class LivePermissionFragment : BaseFragment<LivePremissionContract.Presenter>(), LivePremissionContract.View {
    override fun setFragmentComponent(fragmentComponent: FragmentComponent?) {

    }

    override fun getContentViewID(): Int {
        return R.layout.fragment_live_permission
    }



    companion object {
        fun newInstance(): LivePermissionFragment {
            val fragment = LivePermissionFragment()
       R.id.live_permission_close_friends
            return fragment
        }
    }
}