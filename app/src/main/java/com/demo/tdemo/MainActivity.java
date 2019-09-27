package com.demo.tdemo;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DragFillBlankView dfb_view;

    private AuxiliaryView aView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        dfb_view = findViewById(R.id.dfb_view);

        aView = findViewById(R.id.aView);

        initData();
    }


    private void initData() {

        //        String content = "昔在黄帝，生而神灵，弱而能言，幼而徇齐，长而敦敏，成而登天。" +
        //                "乃问于天师曰：余闻上古之人，春秋皆度百岁，而动作不衰；今时之人，年半百而动作皆衰者。时世异耶人将失之耶？" +
        //                "岐伯对曰：上古之人？其知道者，法于阴阳，和于术数，食饮有节，起居有常，不妄作劳，故能形与神俱，而尽终其天年，度百岁乃去。" +
        //                "今时之人不然也，以酒为浆，以妄为常，醉以入房，以欲竭其精，以耗散其真，不知持满，不时御神，务快其心，逆于生乐，起居无节，故半百而衰也。";

        String content = "昔在___，生而___，弱而能言，幼而___，长而___，成而登天。";

        // 选项集合
        List<String> selectList = new ArrayList<>();
        selectList.add("黄帝");
        selectList.add("皇帝");
        selectList.add("神灵");
        selectList.add("徇齐");
        selectList.add("敦敏");
        selectList.add("哈哈哈");

        //答案
        List<String> answerList = new ArrayList<>();
        answerList.add("黄帝");
        answerList.add("神灵");
        answerList.add("徇齐");
        answerList.add("敦敏");

        //答案区域集合。空格，在题目内容中的位置（下标从0开始，含头不含尾）
        List<AnswerRange> rangeList = new ArrayList<>();
        rangeList.add(new AnswerRange(2, 5));
        rangeList.add(new AnswerRange(8, 11));
        rangeList.add(new AnswerRange(19, 22));
        rangeList.add(new AnswerRange(25, 28));

        dfb_view.setData(content, selectList, answerList, rangeList);

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MyEvent event) {

        try {

            if (TextUtils.equals(MyEvent.AnswerLocation, event.getFlag())) {

                List<AnswerLocationBean> answerLocationList = (List<AnswerLocationBean>) event.getObj();

                aView.setAnswerLocation(answerLocationList);

            } else if (TextUtils.equals(MyEvent.XSelectLocation, event.getFlag())) {
                List<Integer> xList = (List<Integer>) event.getObj();

                aView.setXLocation(xList);

            } else if (TextUtils.equals(MyEvent.YSelectLocation, event.getFlag())) {
                List<Integer> yList = (List<Integer>) event.getObj();

                aView.setYLocation(yList);

            }

        } catch (Exception e) {

        }

    }


}
