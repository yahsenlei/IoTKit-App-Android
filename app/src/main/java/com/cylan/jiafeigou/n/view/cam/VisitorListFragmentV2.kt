package com.cylan.jiafeigou.n.view.cam


import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.PopupWindowCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.base.module.DataSourceManager
import com.cylan.jiafeigou.dp.DpMsgDefine
import com.cylan.jiafeigou.misc.JConstant
import com.cylan.jiafeigou.n.base.BaseApplication
import com.cylan.jiafeigou.n.base.IBaseFragment
import com.cylan.jiafeigou.n.mvp.contract.cam.VisitorListContract
import com.cylan.jiafeigou.n.mvp.impl.cam.BaseVisitorPresenter
import com.cylan.jiafeigou.n.view.cam.item.FaceItem
import com.cylan.jiafeigou.server.cache.KeyValueStringItem
import com.cylan.jiafeigou.server.cache.longHash
import com.cylan.jiafeigou.support.log.AppLogger
import com.cylan.jiafeigou.utils.ActivityUtils
import com.cylan.jiafeigou.utils.NetUtils
import com.cylan.jiafeigou.utils.ToastUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.fragment_visitor_list.*


/**
 * A simple [Fragment] subclass.
 * Use the [VisitorListFragmentV2.newInstance] factory method to
 * create an instance of this fragment.
 */
open class VisitorListFragmentV2 : IBaseFragment<VisitorListContract.Presenter>(),
        VisitorListContract.View {

    override fun onDeleteFaceSuccess(type: Int, delMsg: Int) {
        AppLogger.w("删除面孔消息成功了")
        ToastUtil.showToast(getString(R.string.DELETED_SUC))
        when (type) {
            1 -> {
                //陌生人
                presenter.fetchStrangerVisitorList()
            }
            2 -> {
                //熟人
                presenter.fetchVisitorList()
            }
        }

    }

    override fun onDeleteFaceError() {
        AppLogger.w("删除面孔消息失败了")
        ToastUtil.showToast(getString(R.string.Tips_DeleteFail))

    }


    override fun onVisitsTimeRsp(faceId: String, cnt: Int, type: Int) {
        setFaceVisitsCounts(faceId, cnt, type)
    }


    var visitorListener: VisitorListener? = null

    private lateinit var faceAdapter: FaceAdapter
    private val allFace = FaceItem().withSetSelected(true).withFaceType(FaceItem.FACE_TYPE_ALL)
    private val strangerFace = FaceItem().withFaceType(FaceItem.FACE_TYPE_STRANGER)

    private val visitorItems = mutableListOf<FaceItem>().apply {
        add(allFace)
        add(strangerFace)
    }
    private val strangerItems = mutableListOf<FaceItem>()
    private var isLoadCache = false
    //    private var currentItem: FaceItem? = null
    @Volatile
    private var currentPosition: Int = 0

    private class FaceAdapter(var isNormalView: Boolean) : FastItemAdapter<FaceItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = BaseVisitorPresenter(this)
    }

    private fun restoreCache() {
        val boxFor = BaseApplication.getBoxStore().boxFor(KeyValueStringItem::class)
        val valueItem = boxFor["${VisitorListFragmentV2::javaClass.name}:$uuid:faceAdapter:dateItems".longHash()]
        val valueItem1 = boxFor["${VisitorListFragmentV2::javaClass.name}:$uuid:faceStrangerAdapter:dateItems".longHash()]
        valueItem?.value?.apply {
            val item = Gson().fromJson<List<FaceItem>>(this, object : TypeToken<List<FaceItem>>() {}.type)
            item.forEach { it.withSetSelected(false) }
            onVisitorListReady(item.toMutableList())
        }
        valueItem1?.value?.apply {
            val item1 = Gson().fromJson<List<FaceItem>>(this, object : TypeToken<List<FaceItem>>() {}.type)
            item1.forEach { it.withSetSelected(false) }
            strangerItems.addAll(item1)
//            strangerAdapter.populateItems(item1)
        }
    }

    override fun onPause() {
        super.onPause()
        saveCache()
    }

    private fun saveCache() {
        val boxFor = BaseApplication.getBoxStore().boxFor(KeyValueStringItem::class)
        val gson = Gson()
//        visitorItems.drop(2)
        visitorItems.drop(2).apply {
            boxFor.put(KeyValueStringItem("${VisitorListFragmentV2::javaClass.name}:$uuid:faceAdapter:dateItems".longHash(), gson.toJson(this)))
        }
        boxFor.put(KeyValueStringItem("${VisitorListFragmentV2::javaClass.name}:$uuid:faceStrangerAdapter:dateItems".longHash(), gson.toJson(strangerItems)))
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_visitor_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        faceAdapter = FaceAdapter(true)
        face_header.layoutManager = GridLayoutManager(context, 3)
        face_header.adapter = faceAdapter

//        val itemClickListener: ItemClickListener = object : ItemClickListener {
//            override fun itemClick(item: FaceItem, globalPosition: Int, position: Int, pageIndex: Int) {
//
//            }
//
//            override fun itemLongClick(globalPosition: Int, _p: Int, _v: View, faceType: Int, pageIndex: Int) {
//                val adapter = vp_default.adapter as FaceAdapter?
//                if (adapter != null) {
//                    val faceItem = adapter.dataItems[globalPosition]
//                    visitorListener?.onLoadItemInformation(faceItem)
////                    currentItem = faceItem
//                    this@VisitorListFragmentV2.currentPosition = globalPosition
//                    showHeaderFacePopMenu(faceItem, _p, _v, faceType)
//                    adapter.updateClickItem(globalPosition)
//                }
//            }
//
//        }
        faceAdapter.withOnClickListener { v, adapter, item, position ->
            //                val faceItem = adapter?.dataItems?.get(globalPosition)
            visitorListener?.onLoadItemInformation(item)
//            this@VisitorListFragmentV2.currentPosition = globalPosition
            when (item.getFaceType()) {
                FaceItem.FACE_TYPE_ALL -> {
                    presenter.fetchVisitorList()
                    cam_message_indicator_watcher_text.visibility = View.VISIBLE
                    presenter.fetchVisitsCount("", FILTER_TYPE_ALL)
                }
                FaceItem.FACE_TYPE_STRANGER -> {
                    cam_message_indicator_watcher_text.visibility = View.GONE
//                    vp_default.adapter = strangerAdapter
//                    setFaceHeaderPageIndicator(vp_default.currentItem, strangerAdapter?.dataItems?.size)
                    faceAdapter.setNewList(strangerItems)
                    presenter.fetchStrangerVisitorList()
                }
                FaceItem.FACE_TYPE_ACQUAINTANCE -> {
//                    val faceId = if (adapter?.isNormalVisitor == true) item.visitor?.personId else item.strangerVisitor?.faceId
                    val faceId = if (item.getFaceType() == FaceItem.FACE_TYPE_ACQUAINTANCE) item.visitor?.personId else item.strangerVisitor?.faceId
                    AppLogger.w("主列表的 faceId?personId")
                    cam_message_indicator_watcher_text.visibility = View.VISIBLE
                    presenter.fetchVisitsCount(faceId!!, FILTER_TYPE_ACQUAINTANCE)
                }
                FaceItem.FACE_TYPE_STRANGER_SUB -> {
//                    val faceId = if (adapter?.isNormalVisitor == true) item.visitor?.personId else item.strangerVisitor?.faceId
                    val faceId = if (item.getFaceType() == FaceItem.FACE_TYPE_STRANGER_SUB) item.strangerVisitor?.faceId else item.visitor?.personId
                    AppLogger.w("主列表的 faceId?personId")
                    cam_message_indicator_watcher_text.visibility = View.VISIBLE
                    presenter.fetchVisitsCount(faceId!!, FILTER_TYPE_STRANGER)
                }
            }
            return@withOnClickListener true
        }

        faceAdapter.withOnLongClickListener { v, adapter, item, position ->

            visitorListener?.onLoadItemInformation(item)
//                    currentItem = faceItem
//            this@VisitorListFragmentV2.currentPosition = globalPosition
            showHeaderFacePopMenu(item, position, v, item.getFaceType())
//            adapter.updateClickItem(globalPosition)
            return@withOnLongClickListener true
        }

        faceAdapter.withSelectable(true)
        faceAdapter.withMultiSelect(false)
//        adapter.withSelectOnLongClick(true)
        faceAdapter.withSelectWithItemUpdate(true)
        faceAdapter.withAllowDeselection(false)

//        faceAdapter.itemClickListener = itemClickListener
//        vp_default.enableScrollListener = EViewPager.EnableScrollListener { false }
        cam_message_indicator_holder.visibility = View.VISIBLE
//        val count =
//        val position = layoutManager.findFirstCompletelyVisibleItemPosition();
//        setFaceHeaderPageIndicator(vp_default.currentItem, count)
//        vp_default.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
//            override fun onPageScrollStateChanged(state: Int) {
//
//            }
//
//            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//            }
//
//            override fun onPageSelected(position: Int) {
//                val itemSize = (vp_default.adapter as FaceAdapter).getItemSize()
//                setFaceHeaderPageIndicator(position, itemSize)
//            }
//
//        })

        if (!isLoadCache && NetUtils.getNetType(context) == -1) {
            isLoadCache = true
            restoreCache()
        } else {
            refreshContent()
        }
    }

    private fun setFaceHeaderPageIndicator(currentItem: Int, total: Int) {
        cam_message_indicator_holder.visibility = if (total > 0) View.VISIBLE else View.GONE
        cam_message_indicator_page_text.text = String.format("%s/%s", currentItem + 1, total / 6 + if (total % 6 == 0) 0 else 1)
        cam_message_indicator_page_text.visibility = if (total > 3) View.VISIBLE else View.GONE
    }

    private fun setFaceVisitsCounts(faceId: String, count: Int, type: Int) {
        AppLogger.w("获取来访数: id:$faceId,count:$count,type:$type")
        cam_message_indicator_holder.visibility = View.VISIBLE
        cam_message_indicator_watcher_text.visibility = View.VISIBLE

        when (type) {
            FILTER_TYPE_ALL -> {
                if (TextUtils.isEmpty(faceId)) {
                    val string = SpannableString(getString(R.string.MESSAGES_FACE_VISIT_SUM, count.toString()))
                    val matcher = "\\d+".toPattern().matcher(string)
                    if (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val span = ForegroundColorSpan(Color.parseColor("#4B9Fd5"))
                        string.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        string.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    cam_message_indicator_watcher_text.post { cam_message_indicator_watcher_text.text = string }
                }

            }
            FILTER_TYPE_STRANGER -> {
                val faceId1 = strangerItems.getOrNull(currentPosition)?.strangerVisitor?.faceId
                AppLogger.w("actual face id:$faceId1")
                if (TextUtils.equals(faceId, faceId1)) {
                    val string = SpannableString(getString(R.string.MESSAGES_FACE_VISIT_TIMES, count.toString()))
                    val matcher = "\\d+".toPattern().matcher(string)
                    if (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val span = ForegroundColorSpan(Color.parseColor("#4B9Fd5"))
                        string.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    if (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val span = ForegroundColorSpan(Color.parseColor("#4B9Fd5"))
                        string.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        string.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    cam_message_indicator_watcher_text.post { cam_message_indicator_watcher_text.text = string }
                } else {
                    AppLogger.w("来访次数丢失了!!!!!!!")
                }
            }
            FILTER_TYPE_ACQUAINTANCE -> {
                val personId = visitorItems.getOrNull(currentPosition)?.visitor?.personId
                AppLogger.w("actual person id:$personId")
                if (/*adapter.isNormalVisitor&&*/ TextUtils.equals(faceId, personId)) {
                    val string = SpannableString(getString(R.string.MESSAGES_FACE_VISIT_TIMES, count.toString()))
                    val matcher = "\\d+".toPattern().matcher(string)
                    if (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val span = ForegroundColorSpan(Color.parseColor("#4B9Fd5"))
                        string.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    if (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val span = ForegroundColorSpan(Color.parseColor("#4B9Fd5"))
                        string.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        string.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    cam_message_indicator_watcher_text.post { cam_message_indicator_watcher_text.text = string }
                } else {
                    AppLogger.w("来访次数丢失了!!!!!!")
                }
            }
        }
    }

    override fun onVisitorListReady(visitorList: MutableList<FaceItem>) {
//        if (!(vp_default.adapter as FaceAdapter).isNormalVisitor) {
//            vp_default.adapter = faceAdapter
//        }
        visitorItems.addAll(visitorList)
        faceAdapter.isNormalView = true
        faceAdapter.setNewList(visitorItems)
        faceAdapter.select(0)
//        faceAdapter.populateItems(visitorList)
//        faceAdapter.updateClickItem(0)
        cam_message_indicator_watcher_text.visibility = View.VISIBLE
//        setFaceHeaderPageIndicator(vp_default.currentItem, (vp_default.adapter as FaceAdapter).getItemSize())

//        val newPos = Math.min(vp_default.currentItem * 6, faceAdapter.dataItems.size - 1)
//        currentPosition = newPos
        currentPosition = 0
//        currentItem = faceAdapter.dataItems[newPos]
        presenter.fetchVisitsCount("", FILTER_TYPE_ALL)
        visitorListener?.onVisitorReady(visitorList)
        visitorListener?.onLoadItemInformation(visitorItems[currentPosition])
    }

    open fun exitStranger() {
//        face_header.ad(faceAdapter, true)
//        vp_default.adapter = faceAdapter
//        faceAdapter.updateClickItem(0)
        faceAdapter.isNormalView = true
        faceAdapter.setNewList(visitorItems)
        faceAdapter.select(0)
        presenter.fetchVisitorList()
        cam_message_indicator_holder.visibility = View.VISIBLE
        cam_message_indicator_watcher_text.text = ""
        cam_message_indicator_watcher_text.visibility = View.GONE
    }

    override fun onStrangerVisitorListReady(visitorList: MutableList<FaceItem>) {
        AppLogger.d("陌生人列表")
//        if ((vp_default.adapter as FaceAdapter).isNormalVisitor) {
//            vp_default.adapter = strangerAdapter
//        }
        faceAdapter.isNormalView = false
        strangerItems.addAll(visitorList)
        faceAdapter.select(0)
//        strangerAdapter.populateItems(visitorList)
//        strangerAdapter.updateClickItem(0)
//        vp_default.swapAdapter(strangerAdapter, true)
//        setFaceHeaderPageIndicator(vp_default.currentItem, (vp_default.adapter as FaceAdapter).getItemSize())
        visitorListener?.onStrangerVisitorReady(visitorList)

//        if (strangerAdapter.dataItems.size > 0) {
//            val newPos = Math.min(vp_default.currentItem * 6, strangerAdapter.dataItems.size - 1)
//            var item = strangerAdapter.dataItems[newPos]
        cam_message_indicator_watcher_text.visibility = View.VISIBLE
//            currentPosition = newPos
        currentPosition = 0
        strangerItems.getOrNull(0)?.apply {
            presenter.fetchVisitsCount(strangerVisitor?.faceId!!, FILTER_TYPE_STRANGER)
            visitorListener?.onLoadItemInformation(this)
        }
//    }
    }

    open fun refreshContent() {
        if (faceAdapter.isNormalView) {
            cam_message_indicator_holder.post { presenter?.fetchVisitorList() }
        } else {
            cam_message_indicator_holder.post { presenter?.fetchStrangerVisitorList() }
        }
    }

    open fun disable(disable: Boolean) {
        if (disable) {
            cover_layer.visibility = View.VISIBLE
        } else {
            cover_layer.visibility = View.INVISIBLE
        }
    }

    companion object {
        const val FILTER_TYPE_ALL = 5
        const val FILTER_TYPE_STRANGER = 1
        const val FILTER_TYPE_ACQUAINTANCE = 2
        fun newInstance(uuid: String): VisitorListFragmentV2 {
            val fragment = VisitorListFragmentV2()
            val args = Bundle()
            args.putString(JConstant.KEY_DEVICE_ITEM_UUID, uuid)
            fragment.arguments = args
            return fragment
        }
    }

    private fun showHeaderFacePopMenu(item: FaceItem, position: Int, faceItem: View, faceType: Int) {
//        AppLogger.w("showHeaderFacePopMenu:$position,item:$faceItem")
        val contentView = View.inflate(context, R.layout.layout_face_page_pop_menu, null)
        // TODO: 2017/10/9 查看和识别二选一 ,需要判断,并且只有人才有查看识别二选一
        when (faceType) {
            FaceItem.FACE_TYPE_ACQUAINTANCE -> {
                contentView.findViewById(R.id.detect).visibility = View.GONE
            }
            FaceItem.FACE_TYPE_STRANGER, FaceItem.FACE_TYPE_STRANGER_SUB -> {
                contentView.findViewById(R.id.viewer).visibility = View.GONE
            }
        }
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWindow = PopupWindow(contentView, contentView.measuredWidth, contentView.measuredHeight)
        popupWindow.setBackgroundDrawable(ColorDrawable(0))
        popupWindow.isOutsideTouchable = true
        contentView.findViewById(R.id.delete).setOnClickListener { v ->
            // TODO: 2017/10/9 删除操作
            AppLogger.w("将删除面孔")
            popupWindow.dismiss()
            showDeleteFaceAlert(item)
        }

        contentView.findViewById(R.id.detect).setOnClickListener { v ->
            // TODO: 2017/10/9 识别操作
            AppLogger.w("将识别面孔")
            popupWindow.dismiss()
            showDetectFaceAlert(item.strangerVisitor)
        }

        contentView.findViewById(R.id.viewer).setOnClickListener { _ ->
            AppLogger.w("将查看面孔详细信息")
            popupWindow.dismiss()

            if (item != null) {
                val fragment = FaceInformationFragment.newInstance(uuid, item.visitor)
                ActivityUtils.addFragmentSlideInFromRight(activity.supportFragmentManager, fragment, android.R.id.content)
            } else {
                // TODO: 2017/10/16 为什么会出现这种情况?
            }
        }
//        popupWindow.showAsDropDown(faceItem.findViewById(R.id.img_item_face_selection))
        val anchor = faceItem.findViewById(R.id.img_item_face_selection)
//        showAsDropDown(popupWindow, anchor, 0, 0)
        var position = IntArray(2)
        anchor.getLocationOnScreen(position)
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, position[0], position[1] + anchor.measuredHeight)
        PopupWindowCompat.showAsDropDown(popupWindow, anchor, 0, 0, Gravity.NO_GRAVITY)
    }

    private fun showDetectFaceAlert(strangerVisitor: DpMsgDefine.StrangerVisitor?) {
        val dialog = AlertDialog.Builder(context)
                .setView(R.layout.layout_face_detect_pop_alert)
                .show()

        dialog.findViewById(R.id.detect_cancel)!!.setOnClickListener { v -> dialog.dismiss() }

        dialog.findViewById(R.id.detect_ok)!!.setOnClickListener { v ->
            val addTo = dialog.findViewById(R.id.detect_add_to) as RadioButton?
            val newFace = dialog.findViewById(R.id.detect_new_face) as RadioButton?
            if (addTo!!.isChecked) {
                val fragment = FaceListFragment.newInstance(DataSourceManager.getInstance().account.account,
                        uuid, strangerVisitor?.faceId ?: "", FaceListFragment.TYPE_ADD_TO)
                fragment.resultCallback = { o, o2, o3 ->
                    presenter.fetchStrangerVisitorList()

                }// TODO: 2017/10/10 移动到面孔的结果回调
                ActivityUtils.addFragmentSlideInFromRight(activity.supportFragmentManager, fragment, android.R.id.content)
            } else if (newFace!!.isChecked) {
                val fragment = CreateNewFaceFragment.newInstance(uuid, strangerVisitor)
                fragment.resultCallback = {
                    //todo 返回创建的personID
                    presenter.fetchStrangerVisitorList()
                }
                ActivityUtils.addFragmentSlideInFromRight(activity.supportFragmentManager, fragment, android.R.id.content)
            }
            dialog.dismiss()
        }
    }

    private fun showDeleteFaceAlert(item: FaceItem) {
        val dialog = AlertDialog.Builder(context)
                .setView(R.layout.layout_face_delete_pop_alert)
                .show()
        dialog.findViewById(R.id.delete_cancel)!!.setOnClickListener { v1 ->
            // TODO: 2017/10/9 取消了 什么也不做
            dialog.dismiss()

        }

        dialog.findViewById(R.id.delete_ok)!!.setOnClickListener { v ->
            val radioGroup = dialog.findViewById(R.id.delete_radio) as RadioGroup?
            val radioButtonId = radioGroup!!.checkedRadioButtonId
            if (radioButtonId == R.id.delete_only_face) {
                AppLogger.w("only face")
                when (item.getFaceType()) {
                    FaceItem.FACE_TYPE_ACQUAINTANCE -> {
                        presenter.deleteFace(2, item.visitor?.personId!!, 0)
                    }
                    FaceItem.FACE_TYPE_STRANGER_SUB -> {
                        presenter.deleteFace(1, item.strangerVisitor?.faceId!!, 0)

                    }
                }

            } else if (radioButtonId == R.id.delete_face_and_message) {
                AppLogger.w("face and message")
                when (item.getFaceType()) {
                    FaceItem.FACE_TYPE_ACQUAINTANCE -> {
                        presenter.deleteFace(2, item.visitor?.personId!!, 1)
                    }
                    FaceItem.FACE_TYPE_STRANGER_SUB -> {
                        presenter.deleteFace(1, item.strangerVisitor?.faceId!!, 1)
                    }
                }
            } else {
            }
            dialog.dismiss()
        }

    }

//    interface ItemClickListener {
//
//        fun itemClick(item: FaceItem, globalPosition: Int, position: Int, pageIndex: Int)
//        fun itemLongClick(globalPosition: Int, _p: Int, _v: View, faceType: Int, pageIndex: Int)
//    }

    interface VisitorListener {
        fun onLoadItemInformation(item: FaceItem)
        fun onStrangerVisitorReady(visitorList: MutableList<FaceItem>)
        fun onVisitorReady(visitorList: MutableList<FaceItem>)
    }

}// Required empty public constructor

//class FaceFastItemAdapter : ItemAdapter<FaceItem>()
//class ViewHolder(val itemview: View) {
//    val rvList: RecyclerView = itemview.findViewById(R.id.message_face_page_item) as RecyclerView
//    val visitorAdapter = FaceFastItemAdapter()
//    val adapter = FastAdapter<FaceItem>()
//    var itemClickListener: VisitorListFragmentV2.ItemClickListener? = null
//    private var isNormalVisitor: Boolean = true
//    private var items: List<FaceItem>? = null
//    private var pageIndex: Int = 0
//
//    fun bindItem(pageIndex: Int, isNormalVisitor: Boolean, items: List<FaceItem>, itemClickListener: VisitorListFragmentV2.ItemClickListener) {
//        this.itemClickListener = itemClickListener
//        this.isNormalVisitor = isNormalVisitor
//        this.items = items
//        this.pageIndex = pageIndex
//        visitorAdapter.setNewList(items)
//        adapter.notifyDataSetChanged()
//    }
//
//    init {
//        rvList.layoutManager = GridLayoutManager(itemview.context, 3)
//        rvList.adapter = visitorAdapter.wrap(adapter)
//        adapter.withSelectable(true)
//        adapter.withMultiSelect(false)
////        adapter.withSelectOnLongClick(true)
//        adapter.withSelectWithItemUpdate(true)
//        adapter.withAllowDeselection(false)
//        rvList.addItemDecoration(object : RecyclerView.ItemDecoration() {
//            override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State?) {
////                if (parent.getChildLayoutPosition(v) % 3 == 1) {
////                    val pixelOffset = itemview.context.resources.getDimensionPixelOffset(R.dimen.y18)
////                    outRect.left = pixelOffset
////                    outRect.right = pixelOffset
////                }
//            }
//
//        })
//        adapter.withOnClickListener { _, _, item, position ->
//            val globalPosition = pageIndex * JConstant.FACE_CNT_IN_PAGE + position
//            itemClickListener?.itemClick(visitorAdapter.getItem(position),
//                    globalPosition, position, pageIndex)
//            true
//        }
//        adapter.withOnLongClickListener { _v, _, _, _p ->
//            val globalPosition = pageIndex * JConstant.FACE_CNT_IN_PAGE + _p
//            if (globalPosition > 1 || !isNormalVisitor) {
//                itemClickListener?.itemLongClick(globalPosition, _p, _v, adapter.getItem(_p).getFaceType(), pageIndex)
//            }
//            false
//        }
//    }
//}

//class FaceAdapter(var isNormalVisitor: Boolean) : PagerAdapter() {
//    override fun isViewFromObject(view: View?, viewHolder: Any?): Boolean {
//        return (viewHolder as? ViewHolder)?.itemview == view
//    }
//
//    private val cachedItems = mutableListOf<ViewHolder>()
//
//
//    override fun getCount(): Int {
//        return JConstant.getPageCnt(ListUtils.getSize(dataItems))
//    }
//
//    override fun instantiateItem(container: ViewGroup, position: Int): Any {
//        val viewHolder = if (cachedItems.size > 0) {
//            cachedItems.removeAt(0)
//        } else {
//            val inflate = LayoutInflater.from(container.context).inflate(R.layout.message_face_page, container, false)
//            ViewHolder(inflate)
//        }
//        val start = JConstant.FACE_CNT_IN_PAGE * position
//        val end = Math.min(dataItems.size, start + JConstant.FACE_CNT_IN_PAGE)
//        AppLogger.e("start:$start,end:$end")
//        val list = (JConstant.FACE_CNT_IN_PAGE * position until Math.min(dataItems.size, start + JConstant.FACE_CNT_IN_PAGE)).map { dataItems[it] }
//        viewHolder.bindItem(position, isNormalVisitor, list, itemClickListener)
//        container.addView(viewHolder.itemview)
//        return viewHolder
//    }
//
//    override fun notifyDataSetChanged() {
//        super.notifyDataSetChanged()
//
//    }
//
//    override fun destroyItem(container: ViewGroup, position: Int, viewHolder: Any?) {
//        container.removeView((viewHolder as ViewHolder).itemview)
//        cachedItems.add(viewHolder)
//    }
//
//    override fun getItemPosition(`object`: Any?): Int {
//        return POSITION_NONE
//    }
//
//
//    lateinit var uuid: String
//    lateinit var itemClickListener: VisitorListFragmentV2.ItemClickListener
//
//    private var preloadItems = mutableListOf<FaceItem>()
//    var dataItems = mutableListOf<FaceItem>()
//
//    init {
//        val allFace = FaceItem()
//        allFace.withSetSelected(true)
//        allFace.withFaceType(FaceItem.FACE_TYPE_ALL)
//        preloadItems.add(allFace)
//
//        val strangerFace = FaceItem()
//        strangerFace.withFaceType(FaceItem.FACE_TYPE_STRANGER)
//        preloadItems.add(strangerFace)
//        if (isNormalVisitor) {
//            dataItems.addAll(preloadItems)
//        }
//    }
//
//    fun getItemSize(): Int {
//        return dataItems?.size ?: 0
//    }
//
//    fun populateItems(dataItems: List<FaceItem>) {
//        this.dataItems.clear()
//        if (isNormalVisitor) {
//            this.dataItems.addAll(preloadItems)
//        }
//        this.dataItems.addAll(dataItems)
//        notifyDataSetChanged()
//    }
//
//    fun updateClickItem(position: Int) {
//        dataItems.forEachIndexed { index, faceItem ->
//            if (index != position) {
//                faceItem.withSetSelected(false)
//            } else {
//                faceItem.withSetSelected(true)
//            }
//        }
//        notifyDataSetChanged()
//    }
//}
