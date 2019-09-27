package com.demo.tdemo;

/**
 * 记录答案展示位置（下划线）坐标的bean
 */
public class AnswerLocationBean {

    //用于表示，这个空位，是否是跨行了
    private boolean isOneLine = false;

    private float xLeft;
    private float xRight;
    private float xTop;
    private float xBottom;

    public AnswerLocationBean(boolean isOneLine, float xLeft, float xRight, float xTop, float xBottom) {
        this.isOneLine = isOneLine;
        this.xLeft = xLeft;
        this.xRight = xRight;
        this.xTop = xTop;
        this.xBottom = xBottom;
    }

    public boolean isOneLine() {
        return isOneLine;
    }

    public void setOneLine(boolean oneLine) {
        isOneLine = oneLine;
    }

    public float getxLeft() {
        return xLeft;
    }

    public void setxLeft(float xLeft) {
        this.xLeft = xLeft;
    }

    public float getxRight() {
        return xRight;
    }

    public void setxRight(float xRight) {
        this.xRight = xRight;
    }

    public float getxTop() {
        return xTop;
    }

    public void setxTop(float xTop) {
        this.xTop = xTop;
    }

    public float getxBottom() {
        return xBottom;
    }

    public void setxBottom(float xBottom) {
        this.xBottom = xBottom;
    }
}
