package com.demo.tdemo;

public class MyEvent {

    public static String AnswerLocation = "AnswerLocation";
    public static String XSelectLocation = "XSelectLocation";
    public static String YSelectLocation = "YSelectLocation";

    private Object obj;

    private String flag;

    public MyEvent(String flag, Object obj) {
        this.obj = obj;
        this.flag = flag;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}
