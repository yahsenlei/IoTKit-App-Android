package com.cylan.jiafeigou.n.view.adapter;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.cache.db.module.SysMsgBean;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.support.superadapter.IMulItemViewType;
import com.cylan.jiafeigou.support.superadapter.SuperAdapter;
import com.cylan.jiafeigou.support.superadapter.internal.SuperViewHolder;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HomeMineMessageAdapter extends SuperAdapter<SysMsgBean> {
    private boolean isEditMode = false;

    public boolean isEditMode() {
        return isEditMode;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    @Override
    public void addAll(List<SysMsgBean> items) {
        if (items == null || items.size() == 0) {
            return;
        }
        mList.addAll(items);
        Collections.sort(mList, (o1, o2) -> {
            long t = o2.time - o1.time;
            return t > 0 ? 1 : t < 0 ? -1 : 0;
        });
        notifyDataSetChanged();
    }

    @Override
    public void add(int location, SysMsgBean item) {
        mList.add(location, item);
          Collections.sort(mList, (o1, o2) -> {
            long t = o2.time - o1.time;
            return t > 0 ? 1 : t < 0 ? -1 : 0;
        });
        notifyDataSetChanged();
    }

    public void select(boolean selectAll) {
        List<SysMsgBean> msgBeans = getList();
        if (msgBeans != null) {
            for (SysMsgBean bean : msgBeans) {
                bean.isCheck = selectAll ? 1 : 0;
            }
        }
        notifyDataSetChanged();
    }

    public List<SysMsgBean> getSelectedItems() {
        List<SysMsgBean> beanList = new ArrayList<>();
        List<SysMsgBean> msgBeans = getList();
        if (msgBeans != null) {
            for (SysMsgBean bean : getList()) {
                if (bean.isCheck == 1) {
                    beanList.add(bean);
                }
            }
        }
        return beanList;
    }

    public interface SelectionListener {
        void onSelectionChanged(int position, boolean isChecked);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    private SelectionListener listener;

    public HomeMineMessageAdapter(Context context, List<SysMsgBean> items, IMulItemViewType<SysMsgBean> mulItemViewType) {
        super(context, items, mulItemViewType);
    }

    @Override
    public void onBind(SuperViewHolder holder, int viewType, int layoutPosition, SysMsgBean item) {
        //处理消息时间
        holder.setText(R.id.item_time, parseTime(item.getTime()));
        holder.setVisibility(R.id.delete_check, isEditMode ? View.VISIBLE : View.GONE);
        holder.setChecked(R.id.delete_check, item.isCheck == 1);
        CheckBox checkBox = holder.getView(R.id.delete_check);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null) {
                    item.isCheck = isChecked ? 1 : 0;
                    listener.onSelectionChanged(layoutPosition, isChecked);
                }
            }
        });
        //头像icon
        if (item.type == 701) {
            //处理消息显示
            holder.setText(R.id.tv_device_name, ContextUtils.getContext().getString(R.string.Tap3_UserMessage_System));
            holder.setImageDrawable(R.id.iv_mesg_icon, getContext().getResources().getDrawable(R.drawable.pic_head));
        } else {
            //处理消息显示
            holder.setText(R.id.tv_device_name, item.getContent());
            int resId = JConstant.getMessageIcon(JFGRules.getOSType(item.content));
            holder.setImageResource(R.id.iv_mesg_icon, resId);
        }

        if (item.isDone == 1) {
            switch (item.type) {
                case 601:
                    String name = item.getName();
                    if (name == null) {
                        return;
                    }
                    String fName = null;
                    if (name.matches("^\\d{11}$")) {//前三后四
                        fName = name.replace(name.substring(3, name.length() - 4), "****");
                    } else if (name.matches("[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?")) {
                        String[] split = name.split("@");
                        if (split[0].length() == 1) {
                            fName = name;
                        } else if (split[0].length() == 2) {
                            fName = split[0].replace(split[0].substring(1, 2), "*") + "@" + split[1];
                        } else if (split[0].length() == 3) {
                            fName = split[0].replace(split[0].substring(1, 3), "**") + "@" + split[1];
                        } else if (split[0].length() > 3 && split[0].length() <= 8) {
                            fName = split[0].replace(split[0].substring(2, split[0].length() - 1), "****") + "@" + split[1];
                        } else if (split[0].length() > 8) {
                            fName = split[0].replace(split[0].substring(3, split[0].length() - 1), "****") + "@" + split[1];
                        }
                    } else {//第三方登录
                        if (name.length() == 1) {
                            fName = name;
                        } else if (name.length() == 2) {
                            fName = name.replace(name.substring(1), "*");
                        } else if (name.length() == 3) {
                            fName = name.replace(name.substring(1), "**");
                        } else if (name.length() == 4) {
                            fName = name.replace(name.substring(1, name.length() - 1), "***");
                        } else if (name.length() > 4 && name.length() <= 8) {
                            fName = name.replace(name.substring(2, name.length() - 1), "***");
                        } else if (name.length() > 8 && name.length() <= 16) {
                            fName = name.replace(name.substring(3, name.length() - 1), "***");
                        } else if (name.length() > 16) {
                            fName = name.replace(name.substring(3, name.length() - 4), "****");
                        }
                    }
                    if (fName == null) {
                        fName = name;
                    }

                    holder.setText(R.id.mesg_item_content, String.format(ContextUtils.getContext().getString(R.string.MSG_REBIND), fName));
                    break;
                case 603:
                    holder.setText(R.id.mesg_item_content, "该设备已分享");
                    break;
                case 604:
                    holder.setText(R.id.mesg_item_content, "亲友分享了改设备");
                    break;
            }
        } else {
            switch (item.type) {
                case 601:
                    holder.setText(R.id.mesg_item_content, ContextUtils.getContext().getString(R.string.MSG_UNBIND));
                    break;
                case 603:
                    holder.setText(R.id.mesg_item_content, ContextUtils.getContext().getString(R.string.Tap1_shareDevice_canceledshare));
                    break;
                case 604:
                    holder.setText(R.id.mesg_item_content, "亲友取消了分享该设备");
                    break;
                case 701:
                    holder.setText(R.id.mesg_item_content, Html.fromHtml(item.getName()).toString().trim());
                    break;
            }
        }

    }

    @Override
    protected IMulItemViewType<SysMsgBean> offerMultiItemViewType() {
        return new IMulItemViewType<SysMsgBean>() {
            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public int getItemViewType(int position, SysMsgBean mineMessageBean) {
                return 0;
            }

            @Override
            public int getLayoutId(int viewType) {
//                return R.layout.fragment_mine_message_system_items;
                return R.layout.fragment_mine_message_items;
            }
        };
    }

    public String parseTime(long times) {
        return TimeUtils.getWonderTime(times);
    }

    public boolean compareTime(String preStrTime, String nowStrTime) {
        long preTime = Long.parseLong(preStrTime);
        long nowTime = Long.parseLong(nowStrTime);
        return (nowTime - preTime >= 20000);
    }
}
