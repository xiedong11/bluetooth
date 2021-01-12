package com.zhuandian.bluetooth;

import java.util.List;

import cn.bmob.v3.BmobObject;

/**
 * desc:
 * author: xiedong
 * date: 1/12/21
 **/
public class DataEntity extends BmobObject {

    private String name;
    private List<ItemEntity> list;

    public List<ItemEntity> getList() {
        return list;
    }

    public void setList(List<ItemEntity> list) {
        this.list = list;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public static class ItemEntity{
        private String level;
        private String time;

        public ItemEntity(String level, String time) {
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
}
