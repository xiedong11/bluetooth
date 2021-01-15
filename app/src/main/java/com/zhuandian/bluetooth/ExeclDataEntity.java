package com.zhuandian.bluetooth;

/**
 * desc:
 * author: xiedong
 * date: 1/15/21
 **/
public class ExeclDataEntity {
    private String level;
    private String time;

    public ExeclDataEntity(String level, String time) {
        this.level = level;
        this.time = time;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
