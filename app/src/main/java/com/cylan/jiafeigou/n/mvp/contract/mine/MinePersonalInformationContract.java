package com.cylan.jiafeigou.n.mvp.contract.mine;

import com.cylan.jiafeigou.n.mvp.BasePresenter;
import com.cylan.jiafeigou.n.mvp.BaseView;
import com.cylan.jiafeigou.n.mvp.model.UserInfoBean;

/**
 * 作者：zsl
 * 创建时间：2016/9/1
 * 描述：
 */
public interface MinePersonalInformationContract {

    interface View extends BaseView<Presenter> {
        void initPersonalInformation(UserInfoBean bean);        //初始化显示个人信息

        void jump2SetEmailFragment();

        void showChooseImageDialog();

        void showLogOutDialog();                                //退出登录提示框

    }


    interface Presenter extends BasePresenter {

        void setPersonName();                   //更改昵称

        void bindPersonEmail();                 //绑定邮箱

        void bindPersonPhone();                 //绑定手机

        void changePassword();                  //更改密码

        void initGallery();                    //grallery框架初始化

        void getUserInfomation(String url);     //获取到用户信息
    }

}