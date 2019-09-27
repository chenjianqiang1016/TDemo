package com.demo.tdemo;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助view，利用划线，帮助理解填空中的一些位置
 */
public class AuxiliaryView extends View {

    private Paint paint;

    private List<AnswerLocationBean> answerLocationBeans;

    private List<Integer> xList;
    private List<Integer> yList;

    private int width;
    private int height;

    public AuxiliaryView(Context context) {
        this(context, null);
    }

    public AuxiliaryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AuxiliaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStrokeWidth(2);

        answerLocationBeans = new ArrayList<>();
        xList = new ArrayList<>();
        yList = new ArrayList<>();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = w;
        height = h;
    }

    public void setAnswerLocation(List<AnswerLocationBean> list) {
        if (answerLocationBeans == null) {
            answerLocationBeans = new ArrayList<>();
        }
        answerLocationBeans.clear();
        answerLocationBeans.addAll(list);

        invalidate();
    }

    public void setXLocation(List<Integer> list) {
        if (xList == null) {
            xList = new ArrayList<>();
        }
        xList.clear();
        xList.addAll(list);

        invalidate();
    }

    public void setYLocation(List<Integer> list) {
        if (yList == null) {
            yList = new ArrayList<>();
        }
        yList.clear();
        yList.addAll(list);

        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (answerLocationBeans != null && answerLocationBeans.size() > 0) {

            paint.setColor(Color.RED);
            canvas.drawLine(answerLocationBeans.get(0).getxLeft(), 0, answerLocationBeans.get(0).getxLeft(), 450, paint);
            canvas.drawLine(answerLocationBeans.get(0).getxRight(), 0, answerLocationBeans.get(0).getxRight(), 450, paint);
            canvas.drawLine(0, answerLocationBeans.get(0).getxTop(), width, answerLocationBeans.get(0).getxTop(), paint);
            canvas.drawLine(0, answerLocationBeans.get(0).getxBottom(), width, answerLocationBeans.get(0).getxBottom(), paint);

            paint.setColor(Color.BLACK);
            canvas.drawLine(answerLocationBeans.get(1).getxLeft(), 0, answerLocationBeans.get(1).getxLeft(), 450, paint);
            canvas.drawLine(answerLocationBeans.get(1).getxRight(), 0, answerLocationBeans.get(1).getxRight(), 450, paint);
            canvas.drawLine(0, answerLocationBeans.get(1).getxTop(), width, answerLocationBeans.get(1).getxTop(), paint);
            canvas.drawLine(0, answerLocationBeans.get(1).getxBottom(), width, answerLocationBeans.get(1).getxBottom(), paint);

            paint.setColor(0xffff00ff);
            canvas.drawLine(answerLocationBeans.get(2).getxLeft(), 0, answerLocationBeans.get(2).getxLeft(), 450, paint);
            canvas.drawLine(answerLocationBeans.get(2).getxRight(), 0, answerLocationBeans.get(2).getxRight(), 450, paint);
            canvas.drawLine(0, answerLocationBeans.get(2).getxTop(), width, answerLocationBeans.get(2).getxTop(), paint);
            canvas.drawLine(0, answerLocationBeans.get(2).getxBottom(), width, answerLocationBeans.get(2).getxBottom(), paint);

            paint.setColor(Color.BLUE);
            canvas.drawLine(answerLocationBeans.get(3).getxLeft(), 0, answerLocationBeans.get(3).getxLeft(), 450, paint);
            canvas.drawLine(answerLocationBeans.get(3).getxRight(), 0, answerLocationBeans.get(3).getxRight(), 450, paint);
            canvas.drawLine(0, answerLocationBeans.get(3).getxTop(), width, answerLocationBeans.get(3).getxTop(), paint);
            canvas.drawLine(0, answerLocationBeans.get(3).getxBottom(), width, answerLocationBeans.get(3).getxBottom(), paint);

        }
        if (xList != null && xList.size() > 0) {

            paint.setColor(Color.BLACK);

            for (int i = 0; i < xList.size(); i++) {
                canvas.drawLine(xList.get(i), 500, xList.get(i), height, paint);
            }


        }
        if (yList != null && yList.size() > 0) {

            paint.setColor(Color.BLACK);

            for (int i = 0; i < yList.size(); i++) {
                canvas.drawLine(0, yList.get(i), width, yList.get(i), paint);
            }

        }


    }
}
