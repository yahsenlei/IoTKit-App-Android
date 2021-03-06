package com.cylan.jiafeigou.cache.db.module;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by holy on 2017/3/19.
 */

@Entity(generateGettersSetters = false)
public final class HistoryFile implements Parcelable, Comparable<HistoryFile> {
    @Id
    public Long id;
    public long time;
    public int duration;
    @Generated(hash = 735721945)
    public String uuid;
    public String server;

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public Long getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public String getUuid() {
        return uuid;
    }

    public String getServer() {
        return server;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public void setTime(long time) {
        this.time = time;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public HistoryFile() {
    }

    @Override
    public String toString() {
        return "HistoryFile{" +
                "id=" + id +
                ", time=" + time +
                ", duration=" + duration +
                ", uuid='" + uuid + '\'' +
                ", server='" + server + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeLong(this.time);
        dest.writeInt(this.duration);
        dest.writeString(this.uuid);
        dest.writeString(this.server);
    }

    protected HistoryFile(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.time = in.readLong();
        this.duration = in.readInt();
        this.uuid = in.readString();
        this.server = in.readString();
    }

    @Generated(hash = 6595676)
    public HistoryFile(Long id, long time, int duration, String uuid,
                       String server) {
        this.id = id;
        this.time = time;
        this.duration = duration;
        this.uuid = uuid;
        this.server = server;
    }

    public static final Creator<HistoryFile> CREATOR = new Creator<HistoryFile>() {
        @Override
        public HistoryFile createFromParcel(Parcel source) {
            return new HistoryFile(source);
        }

        @Override
        public HistoryFile[] newArray(int size) {
            return new HistoryFile[size];
        }
    };

    @Override
    public int compareTo(@NonNull HistoryFile another) {
        return (int) (another.time - this.time);
    }
}
