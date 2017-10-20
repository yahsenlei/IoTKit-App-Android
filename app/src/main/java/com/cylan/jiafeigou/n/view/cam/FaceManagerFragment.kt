package com.cylan.jiafeigou.n.view.cam

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.widget.PopupWindowCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import butterknife.ButterKnife
import butterknife.OnClick
import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.base.injector.component.FragmentComponent
import com.cylan.jiafeigou.base.module.DataSourceManager
import com.cylan.jiafeigou.base.wrapper.BaseFragment
import com.cylan.jiafeigou.dp.DpMsgDefine
import com.cylan.jiafeigou.misc.JConstant
import com.cylan.jiafeigou.n.view.cam.item.FaceManagerItem
import com.cylan.jiafeigou.support.log.AppLogger
import com.cylan.jiafeigou.utils.ActivityUtils
import com.cylan.jiafeigou.utils.AnimatorUtils
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.fragment_face_manager.*

/**
 * Created by yanzhendong on 2017/10/9.
 */
class FaceManagerFragment : BaseFragment<FaceManagerContact.Presenter>(), FaceManagerContact.View {
    override fun onFaceInformationReady(data: List<DpMsgDefine.FaceInformation>) {
        var face: FaceManagerItem
        val items: MutableList<FaceManagerItem> = mutableListOf()
        for (information in data) {
            face = FaceManagerItem().withFaceInformation(information)
            items.add(face)
        }
        adapter.add(items)
        custom_toolbar.setRightEnable(adapter.itemCount > 0)
    }

    lateinit var adapter: FastItemAdapter<FaceManagerItem>

    override fun setFragmentComponent(fragmentComponent: FragmentComponent) {
        fragmentComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_face_manager, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    private var personId: String? = null

    override fun onStart() {
        super.onStart()
        presenter.loadFacesByPersonId(personId ?: "")
    }

    override fun initViewAndListener() {
        super.initViewAndListener()
        personId = arguments.getString("person_id")

        val layoutManager = GridLayoutManager(context, 4)
        face_manager_items.layoutManager = layoutManager
        adapter = FastItemAdapter()
        adapter.withSelectable(false)
        adapter.withMultiSelect(true)
        adapter.withAllowDeselection(true)
        adapter.withSelectWithItemUpdate(true)
        adapter.itemAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                custom_toolbar.setRightEnable(adapter.itemCount > 0)
                empty_view.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                custom_toolbar.setRightEnable(true)
                empty_view.visibility = View.GONE
            }
        })

        adapter.withOnLongClickListener { v, adapter, iItem, position ->
            if (isEditMode()) {
                //编辑模式下
                return@withOnLongClickListener false
            }
            AppLogger.w("FaceManagerOnItemLongClicked:$v,$adapter,$iItem,$position")
            //todo 需要弹出菜单
            showFaceManagerPopMenu(position, v, iItem)
            return@withOnLongClickListener true
        }

        adapter.withSelectionListener { _, _ ->
            tv_msg_delete.isEnabled = adapter.selections.size > 0
            if (adapter.selections.size == adapter.itemCount) {
                tv_msg_full_select.setText(R.string.CANCEL)
            } else {
                tv_msg_full_select.setText(R.string.SELECT_ALL)
            }
        }

        face_manager_items.adapter = adapter
//        face_manager_items.addItemDecoration(GridItemDivider(resources.getDimensionPixelOffset(R.dimen.y5), 4))

        custom_toolbar.setRightAction {
            if (getString(R.string.EDIT_THEME) == custom_toolbar.tvToolbarRight.text) {
                adapter.withSelectable(true)
                custom_toolbar.setToolbarRightTitle(R.string.CANCEL)
                AnimatorUtils.slideIn(bottom_menu, false)
//                bottom_menu.visibility = View.VISIBLE
            } else {
                adapter.withSelectable(false)
                adapter.deselect()
                custom_toolbar.setToolbarRightTitle(R.string.EDIT_THEME)
                AnimatorUtils.slideOut(bottom_menu, false)
            }
            adapter.notifyDataSetChanged()
        }

        /// 默认是不可点击的,等有数据后才能点击
//        custom_toolbar.setRightEnable(false)
        custom_toolbar.setBackAction { fragmentManager.popBackStack() }

        //todo just for test

        val items: MutableList<FaceManagerItem> = mutableListOf()
        words.forEach {
            val item = FaceManagerItem()
            val information = DpMsgDefine.FaceInformation()
            information.face_name = it
            item.withFaceInformation(information)
            items.add(item)
        }
        adapter.add(items)

    }

    @OnClick(R.id.tv_msg_full_select)
    fun clickSelectAll() {
        AppLogger.w("clickSelectAll")

        if (TextUtils.equals(getString(R.string.SELECT_ALL), tv_msg_full_select.text)) {
            tv_msg_full_select.setText(R.string.CANCEL)
            adapter.select()
        } else {
            tv_msg_full_select.setText(R.string.SELECT_ALL)
            adapter.deselect()
        }
    }

    val words = arrayOf("普鹤骞", "田惠君", "貊怀玉", "潘鸿信", "士春柔", "阙子璇", "皇甫笑", "妍李颖", "初殷浩旷")

    private fun showFaceManagerPopMenu(position: Int, v: View?, faceManagerItem: FaceManagerItem) {
        val view = View.inflate(context, R.layout.layout_face_manager_pop_alert, null)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWindow = PopupWindow(view, view.measuredWidth, view.measuredHeight)
        popupWindow.setBackgroundDrawable(ColorDrawable(0))
        popupWindow.isOutsideTouchable = true
        view.findViewById(R.id.delete).setOnClickListener {
            AppLogger.w("面孔管理:删除")
            popupWindow.dismiss()
        }

        view.findViewById(R.id.move_to).setOnClickListener {
            AppLogger.w("面孔管理:移动到")
            popupWindow.dismiss()
            val fragment = FaceListFragment.newInstance(DataSourceManager.getInstance().account.account, uuid,
                    faceManagerItem.faceInformation?.face_id ?: "", FaceListFragment.TYPE_MOVE_TO)
            //TODO 监听 移动面孔的结果回调
//            fragment.resultCallback={
//
//            }
            ActivityUtils.addFragmentSlideInFromRight(fragmentManager, fragment, android.R.id.content)
        }
        PopupWindowCompat.showAsDropDown(popupWindow, v, 0, 0, Gravity.TOP)
    }

    private fun isEditMode(): Boolean {
        return TextUtils.equals(getString(R.string.CANCEL), custom_toolbar.tvToolbarRight.text)
    }

    companion object {
        fun newInstance(uuid: String, personId: String): FaceManagerFragment {
            val fragment = FaceManagerFragment()
            val argument = Bundle()
            argument.putString(JConstant.KEY_DEVICE_ITEM_UUID, uuid)
            argument.putString("person_id", personId)
            fragment.arguments = argument
            return fragment
        }
    }

}