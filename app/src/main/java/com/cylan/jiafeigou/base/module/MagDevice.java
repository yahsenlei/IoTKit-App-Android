package com.cylan.jiafeigou.base.module;

import android.os.Parcel;

import com.cylan.annotation.DPProperty;
import com.cylan.jiafeigou.dp.DpMsgDefine;

import static com.cylan.jiafeigou.dp.DpMsgMap.ID_401_BELL_CALL_STATE;
import static com.cylan.jiafeigou.dp.DpMsgMap.ID_402_BELL_VOICE_MSG;

/*
 *  @项目名：  JFGAndroid 
 *  @包名：    com.cylan.jiafeigou.base.module
 *  @文件名:   MagDevice
 *  @创建者:   yanzhendong
 *  @创建时间:  2017/1/8 11:34
 *  @描述：    TODO
 */
public class MagDevice extends JFGDevice {
    @DPProperty(msgId = ID_401_BELL_CALL_STATE)
    public DpMsgDefine.DPSet<DpMsgDefine.DPBellCallRecord> bell_call_state;//DpMsgMap.BELL_CALL_STATE_401
    @DPProperty(msgId = ID_402_BELL_VOICE_MSG)
    public DpMsgDefine.DPPrimary<Integer> bell_voice_msg;//DpMsgMap.BELL_VOICE_MSG_402

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.bell_call_state, flags);
        dest.writeParcelable(this.bell_voice_msg, flags);
    }

    public MagDevice() {
    }

    protected MagDevice(Parcel in) {
        super(in);
        this.bell_call_state = in.readParcelable(DpMsgDefine.DPBellCallRecord.class.getClassLoader());
    }

    public static final Creator<MagDevice> CREATOR = new Creator<MagDevice>() {
        @Override
        public MagDevice createFromParcel(Parcel source) {
            return new MagDevice(source);
        }

        @Override
        public MagDevice[] newArray(int size) {
            return new MagDevice[size];
        }
    };
}
