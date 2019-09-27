package com.demo.tdemo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 拖拽填空题
 *
 * 缺陷：不能快速连续动画。操作不宜过快，否则，会因为计算等的不准备、不及时性，崩溃
 * 如：如果快速点击选项，多个选项做动画，会出现崩溃问题。
 */

public class DragFillBlankView extends RelativeLayout {

    private Context mContext;

    //展示题目的TextView
    private TextView tvContent;

    //展示题目的TextView 的高度
    private int tvHeight = 0;

    private int answerSize = 0;
    private int halfAnswerSize = 0;

    //存放选项的布局
    private RelativeLayout select_rl;
    // 初始数据
    private String originContent;
    // 填空题内容
    private SpannableStringBuilder content;
    // 选项列表
    private List<String> selectList;
    // 答案集合
    private List<String> answerList;
    // 答案范围集合
    private List<AnswerRange> answerRangeList;
    // 选项位置
    private int optionPosition;

    private int selectRootWidth = 0;
    private int selectRootHeight = 0;

    //列的个数
    private int columnNum = 2;

    //选项位置的集合。初始的显示位置。用于后续拖动松手后，选项做回归动画
    private List<String> selectInitialLocationList;

    private float topHeight = 0;

    //按下时候的x,y坐标
    private float downX = 0;
    private float downY = 0;

    //移动、滑动时候，当前位置的x,y坐标
    private float moveCurrentX;
    private float moveCurrentY;

    //进行滑动的TextView
    private TextView moveTextView;

    //当前点击或者按住了哪个选项
    private int currentOptionPosition = -1;

    //分隔符
    private String SeparateSymbol = "-";

    //认为的最小滑动值。超过这个值，就认为是滑动；如果没有超过，就是认为是点击
    private int reputeMixMoveValue = 10;

    //选项控件的大小，长=宽=50dp
    private int optionViewSize = 50;

    //填充到空格处的答案
    private String[] fillAnswer;

    //填空在坐标系中，位置的对象的集合。
    private List<AnswerLocationBean> answerLocationList;

    //是否选中了已经填充到题目中的答案
    private boolean isSelectFill = false;
    //选中哪个已经填充了答案的空格
    private int isClickSelectFill = -1;

    public DragFillBlankView(Context context) {
        this(context, null);
    }

    public DragFillBlankView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragFillBlankView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        answerSize = dp2px(50);
        halfAnswerSize = answerSize / 2;

        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.layout_drag_fill_blank, this);

        tvContent = (TextView) findViewById(R.id.tv_content);
        select_rl = findViewById(R.id.select_rl);

    }

    /**
     * 设置数据
     *
     * @param originContent   源数据
     * @param selectList      选项集合列表
     * @param answerList      答案集合列表
     * @param answerRangeList 答案范围集合
     */
    public void setData(String originContent, List<String> selectList, List<String> answerList, List<AnswerRange> answerRangeList) {
        if (TextUtils.isEmpty(originContent)
                || selectList == null || selectList.isEmpty()
                || answerList == null || answerList.isEmpty()
                || answerRangeList == null || answerRangeList.isEmpty()) {
            //原始数据为空、选项为空、答案为空、展示答案的位置范围为空，有任何一点，停止后续操作
            return;
        }

        // 初始数据
        this.originContent = originContent;

        // 获取课文内容
        this.content = new SpannableStringBuilder(originContent);
        // 选项列表
        this.selectList = selectList;
        // 答案
        this.answerList = answerList;
        // 答案范围集合
        this.answerRangeList = answerRangeList;

        prepareFillAnswer();

        answerLocationList = new ArrayList<>();

        selectInitialLocationList = new ArrayList<>();

        // 设置下划线颜色
        for (AnswerRange range : this.answerRangeList) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#4DB6AC"));
            content.setSpan(colorSpan, range.start, range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvContent.setMovementMethod(new TouchLinkMovementMethod());
        tvContent.setText(content);

        computeShowAnswerLocationWithViewTree();

        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                select_rl.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                handleSelect();

            }
        };
        select_rl.getViewTreeObserver().addOnGlobalLayoutListener(listener);

    }

    //准备"填充答案"的数组，用于保存填到空格处的答案
    private void prepareFillAnswer() {
        if (fillAnswer == null) {
            fillAnswer = new String[answerRangeList.size()];
            for (int i = 0; i < answerRangeList.size(); i++) {
                fillAnswer[i] = "";
            }
        }

    }

    //处理选项。计算选项，摆放选项的位置
    private void handleSelect() {

        List<Integer> xList = new ArrayList<>();
        List<Integer> yList = new ArrayList<>();

        selectRootWidth = select_rl.getWidth();

        selectRootHeight = select_rl.getHeight();

        /*
         * 题目TextView的高度+15dp*2（R.layout.layout_drag_fill_blank 中，）+10dp的间隔
         *
         * layout_margin = 15dp，即：题目距离上面15dp，距离下面15dp。
         */
        topHeight = tvHeight + dp2px(30) + dp2px(10);

        //经过这步的计算，得到展示选项的区域的宽度。减去30dp，是为了后续展示的时候，避免选项左右"贴边"父控件
        selectRootWidth -= dp2px(30);

        //经过这步的计算，得到展示选项的区域的高度
        selectRootHeight -= topHeight;

        //再减去25dp，是为了缩小选项展示区域的高度，避免选项贴到父控件的底部
        selectRootHeight -= dp2px(25);


        //移除之前的子控件（避免数据造成冲突）
        select_rl.removeAllViews();

        float x = 0;
        float y = 0;

        //为了避免选项重叠，就把展示区域划分不同的区域，每个区域去展示一个选项，这样，就避免了重叠

        //当前列
        int currentColumn = 0;
        //当前行
        int currentRow = 0;

        //一个小区域的宽
        int cellWidth = selectRootWidth / columnNum;
        //一个小区域的高
        int cellHeight = selectRootHeight / ((selectList.size() + 1) / 2);

        int xDifference = cellWidth - answerSize;
        int yDifference = cellHeight - answerSize;

        //生成随机数，实现位置的随机摆放
        Random random = new Random();

        for (int i = 0; i < selectList.size(); i++) {

            String option = selectList.get(i);

            TextView tvAnswer = createSelectTv(option);

            /*
             * 选项的摆放位置规则
             *
             *  0   1
             *
             *  2   3
             *
             *  4
             *
             */
            currentColumn = i % columnNum;

            if (xDifference > 0) {
                x = dp2px(15) + currentColumn * cellWidth + random.nextInt(xDifference);
            } else {
                x = dp2px(15) + currentColumn * cellWidth;
            }

            if (yDifference > 0) {
                y = topHeight + currentRow * cellHeight + random.nextInt(yDifference);
            } else {
                y = topHeight + currentRow * cellHeight;
            }

            //这里的setX、setY，是指选项（正方形）左上角的坐标位置
            tvAnswer.setX(x);
            tvAnswer.setY(y);

            //保存这个选项的坐标位置
            selectInitialLocationList.add(x + SeparateSymbol + y);

            select_rl.addView(tvAnswer);


            if (i % columnNum == columnNum - 1) {
                //满了一行，下一次，就要换行
                currentRow++;
                if (currentRow > (selectList.size() + 1) / 2) {
                    currentRow = (selectList.size() + 1) / 2;
                }
            }
        }

        //隐藏的，用于手指按在已填写的答案处时，需要展示出来的TextView
        TextView tempTv = createSelectTv("*");
        tempTv.setX(0);
        tempTv.setX(0);
        tempTv.setVisibility(View.GONE);
        select_rl.addView(tempTv);


        /*** 以下这些，用于辅助线的绘制，通过辅助线，帮助理解定位问题，实际开发用不到***/

        xList.add(dp2px(15));
        xList.add(dp2px(15) + selectRootWidth / 2);
        xList.add(dp2px(15) + selectRootWidth);

        yList.add((int) topHeight);

        yList.add((int) topHeight + cellHeight);
        yList.add((int) topHeight + cellHeight * 2);
        yList.add((int) topHeight + cellHeight * 3);

        //下面这句，和 yList.add((int)topHeight+cellHeight*3); 一样
        //yList.add((int)(select_rl.getHeight()-dp2px(25)));

        EventBus.getDefault().post(new MyEvent(MyEvent.XSelectLocation, xList));
        EventBus.getDefault().post(new MyEvent(MyEvent.YSelectLocation, yList));

        /*** 以上这些，用于辅助线的绘制，通过辅助线，帮助理解定位问题，实际开发用不到***/

    }

    /**
     * 创建选项TextView
     *
     * @param str textView上要展示的内容
     * @return
     */
    private TextView createSelectTv(String str) {

        TextView tv = new TextView(mContext);

        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#4DB6AC"));

        tv.setText(str);

        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        params.width = answerSize;
        params.height = answerSize;
        tv.setLayoutParams(params);
        tv.setGravity(Gravity.CENTER);

        return tv;


    }

    //利用视图树计算
    private void computeShowAnswerLocationWithViewTree() {

        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (tvContent.getLayout() != null) {
                    tvContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    tvHeight = tvContent.getHeight();

                    computeShowAnswerLocation();
                }
            }
        };
        tvContent.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }


    /**
     * 计算展示答案的位置坐标
     */
    private void computeShowAnswerLocation() {

        long startTime = System.currentTimeMillis();
        Log.e("开始计算 ", startTime + "");

        if (answerLocationList == null) {
            answerLocationList = new ArrayList<>();
        }

        answerLocationList.clear();

        Layout layout = tvContent.getLayout();

        for (int i = 0; i < answerRangeList.size(); i++) {
            AnswerRange range = answerRangeList.get(i);

            // 获取TextView中第一个字符的坐标
            Rect startBound = new Rect();
            layout.getLineBounds(layout.getLineForOffset(range.start), startBound);

            // 获取TextView中最后一个字符的坐标
            Rect endBound = new Rect();
            layout.getLineBounds(layout.getLineForOffset(range.end), endBound);

            /**
             * 获取到的坐标，仅仅是在TextView中的，不包括TextView本身距离外部设置的额外值。
             *
             * 在布局 R.layout.layout_drag_fill_blank 中，TextView 有个 android:layout_margin="15dp"
             */

            // 字符顶部y坐标
            int yAxisTop = startBound.top + dp2px(15);
            // 字符底部y坐标
            int yAxisBottom = endBound.bottom + dp2px(15);
            // 字符左边x坐标
            float xAxisLeft = layout.getPrimaryHorizontal(range.start) + dp2px(15);
            // 字符右边x坐标
            float xAxisRight = layout.getSecondaryHorizontal(range.end) + dp2px(15);

            // 一行的文本高度
            int lineHeight = startBound.bottom - startBound.top;
            // 当前的文本高度
            int currentLineHeight = endBound.bottom - startBound.top;

            answerLocationList.add(new AnswerLocationBean(currentLineHeight <= lineHeight, xAxisLeft, xAxisRight, yAxisTop, yAxisBottom));

        }

        long endTime = System.currentTimeMillis();
        Log.e("计算结束 ", endTime + "");
        Log.e("计算用时 ", endTime - startTime + "");

        //用于辅助线的绘制，通过辅助线，帮助理解定位问题，实际开发用不到
        EventBus.getDefault().post(new MyEvent(MyEvent.AnswerLocation, answerLocationList));

    }

    /**
     * 这个一定要有。否则，
     * <p>
     * 在点击题目的textView时，onTouchEvent 中的 ACTION_DOWN 事件，不会触发，
     * <p>
     * 会造成无法定位到用户点击了哪个空格（答案填写处）
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    isSelectFill = false;

                    if (moveTextView != null) {
                        moveTextView = null;
                    }
                    currentOptionPosition = -1;

                    downX = event.getX();
                    downY = event.getY();

                    //指定当前滑动的TextView
                    for (int i = 0; i < selectInitialLocationList.size(); i++) {

                        float x1 = Float.parseFloat(selectInitialLocationList.get(i).split(SeparateSymbol)[0]);
                        float x2 = x1 + answerSize;

                        float y1 = Float.parseFloat(selectInitialLocationList.get(i).split(SeparateSymbol)[1]);
                        float y2 = y1 + answerSize;

                        //x1、y1，是选项左上角的坐标。x2、y2，是选项右下角的坐标

                        if (downX > x1 && downX < x2 && downY > y1 && downY < y2) {
                            //拿到当前选择的这个选项的view
                            moveTextView = (TextView) select_rl.getChildAt(i);
                            //记下现在选的，是第几个选项
                            currentOptionPosition = i;
                            break;
                        }
                    }

                    if (currentOptionPosition == -1) {
                        //说明手指按下后，没有选中选项，这才需要进行下面的判断，避免无效的判断

                        int position = -1;

                        if (answerLocationList != null && answerLocationList.size() > 0) {

                            for (int i = 0; i < answerLocationList.size(); i++) {

                                AnswerLocationBean bean = answerLocationList.get(i);

                                if (bean.isOneLine()) {

                                    if (downX > bean.getxLeft() && downX < bean.getxRight() &&
                                            downY < bean.getxBottom() && downY > bean.getxTop()) {
                                        position = i;
                                        break;
                                    }

                                } else {

                                    if ((downX > bean.getxLeft() || downX < bean.getxRight()) &&
                                            downY < bean.getxBottom() && downY > bean.getxTop()) {
                                        position = i;
                                        break;
                                    }
                                }
                            }
                        }

                        /*
                         * position 表示按住了第几个要填空的位置（从0开始计数）
                         *
                         * 获取这个位置，填入的内容，然后将内容赋值到一个临时滑动的TextView中，最后，将这个空格位置恢复成"未填写"状态
                         */
                        if (position != -1) {

                            try {
                                AnswerRange range = answerRangeList.get(position);
                                String temp = tvContent.getText().toString().substring(range.start, range.end);

                                if (!TextUtils.equals("___", temp)) {

                                    moveTextView = (TextView) select_rl.getChildAt(select_rl.getChildCount() - 1);
                                    moveTextView.setX(downX - halfAnswerSize);
                                    moveTextView.setY(downY - halfAnswerSize);
                                    moveTextView.setText(temp);
                                    moveTextView.setVisibility(View.VISIBLE);

                                    isSelectFill = true;
                                    isClickSelectFill = position;

                                    currentOptionPosition = selectList.indexOf(temp);

                                    //这么做的目的是，把这个空格，恢复成"未填写"状态
                                    fillAnswer("___", position);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();

                            }

                        }

                    }

                    break;
                case MotionEvent.ACTION_MOVE:

                    if (moveTextView == null) {
                        return false;
                    }

                    moveCurrentX = event.getX();
                    moveCurrentY = event.getY();

                    if (Math.abs(moveCurrentX - downX) > reputeMixMoveValue && Math.abs(moveCurrentY - downY) > reputeMixMoveValue) {

                        //这里，按下的坐标，要减去选项的一半，让选项的中间，跟着手指动。否则，就是选项的左上角，跟着手指移动，不美观

                        moveTextView.setX(moveCurrentX - halfAnswerSize);
                        moveTextView.setY(moveCurrentY - halfAnswerSize);
                    }

                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    if (Math.abs(moveCurrentX - downX) < reputeMixMoveValue && Math.abs(moveCurrentY - downY) < reputeMixMoveValue) {
                        //视为点击

                        if (isSelectFill && isClickSelectFill != -1) {

                            fillAnswer(moveTextView.getText().toString(), isClickSelectFill);
                            setOptionIsShow();
                            operateFinish();

                        } else {

                            //先拿到点击的选项
                            for (int i = 0; i < selectInitialLocationList.size(); i++) {

                                float x1 = Float.parseFloat(selectInitialLocationList.get(i).split(SeparateSymbol)[0]);
                                float x2 = x1 + answerSize;

                                float y1 = Float.parseFloat(selectInitialLocationList.get(i).split(SeparateSymbol)[1]);
                                float y2 = y1 + answerSize;

                                if (downX > x1 && downX < x2 && downY > y1 && downY < y2) {
                                    moveTextView = (TextView) select_rl.getChildAt(i);
                                    currentOptionPosition = i;
                                    break;
                                }
                            }

                            if (currentOptionPosition != -1) {
                                //找到那个位置没有填充，如果全部填充完了，就定位到最后一个
                                int p = 0;
                                String s = tvContent.getText().toString();
                                for (int i = 0; i < answerRangeList.size(); i++) {
                                    AnswerRange range = answerRangeList.get(i);
                                    try {
                                        String temp = s.substring(range.start, range.start + 2);

                                        if (temp.equals("__")) {
                                            break;
                                        } else {
                                            p++;
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();

                                    }
                                }

                                if (p >= answerRangeList.size()) {
                                    p = answerRangeList.size() - 1;
                                }

                                /*
                                 * 到这里，p 表示
                                 *
                                 * 没有填充答案的那个 空位。
                                 *
                                 * 或
                                 *
                                 * 全部空位已经被填充了，就表示是最后一个 要填写答案的 空位(这个空位上已经有答案了)。
                                 */

                                try {
                                    AnswerLocationBean bean = answerLocationList.get(p);

                                    //表示的这个 填答案 的位置的左边的x坐标
                                    float xL = bean.getxLeft();
                                    //表示的这个 填答案 的位置的上边的y坐标
                                    float yT = bean.getxTop();

                                    /*
                                     * 这里做的是，选项的左上角，和空格处的左上角重合
                                     *
                                     * 其实，为了美观，应该是选项的中心，和空格处的中心重合，这个就涉及到一些计算了
                                     *
                                     * 其次，还要额外考虑，空格折行的情况。
                                     *
                                     * 这里为了简单，就这么写了
                                     */
                                    moveAnimator(2, moveTextView.getX(), moveTextView.getY(), xL, yT, p);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }


                        }

                    } else {
                        //滑动

                        int position = -1;

                        if (answerLocationList != null && answerLocationList.size() > 0) {

                            for (int i = 0; i < answerLocationList.size(); i++) {

                                AnswerLocationBean bean = answerLocationList.get(i);

                                if (bean.isOneLine()) {

                                    if (moveCurrentX > bean.getxLeft() && moveCurrentX < bean.getxRight() &&
                                            moveCurrentY < bean.getxBottom() && moveCurrentY > bean.getxTop()) {
                                        position = i;
                                        break;
                                    }

                                } else {

                                    if ((moveCurrentX > bean.getxLeft() || moveCurrentX < bean.getxRight()) &&
                                            moveCurrentY < bean.getxBottom() && moveCurrentY > bean.getxTop()) {
                                        position = i;
                                        break;
                                    }
                                }
                            }
                        }

                        if (position == -1) {
                            //说明没有放到空格处，就做回归动画
                            moveAnimator(0, 0, 0, 0, 0, -1);
                        } else {

                            moveAnimator(1, 0, 0, 0, 0, -1);
                            fillAnswer(moveTextView.getText().toString(), position);

                        }

                    }

                    break;

                default: {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    //操作结束
    private void operateFinish() {
        setOptionIsShow();

        isSelectFill = false;
        isClickSelectFill = -1;

        //抬起手后，释放滑动的TextView
        moveTextView = null;
        currentOptionPosition = -1;
    }

    /**
     * 保存填充到空格处的答案
     *
     * @param answer   当前点击或拖动选中的答案
     * @param position 第几个空格
     */
    private void saveFillAnswer(String answer, int position) {
        prepareFillAnswer();
        fillAnswer[position] = answer;

        computeShowAnswerLocationWithViewTree();
    }

    /**
     * 设置选项是否展示
     */
    private void setOptionIsShow() {
        if (fillAnswer == null) {
            return;
        }

        String s = "";

        for (int i = 0; i < fillAnswer.length; i++) {

            s = s + fillAnswer[i];
            s = s + SeparateSymbol;

        }

        Log.e("fillAnswer ", s);

        for (int j = 0; j < select_rl.getChildCount(); j++) {

            if (j == select_rl.getChildCount() - 1) {

                TextView tv = (TextView) select_rl.getChildAt(j);
                tv.setX(0);
                tv.setY(0);
                tv.setVisibility(View.GONE);

            } else {

                TextView tv = (TextView) select_rl.getChildAt(j);
                if (s.contains(tv.getText().toString())) {
                    select_rl.getChildAt(j).setVisibility(View.INVISIBLE);
                } else {
                    select_rl.getChildAt(j).setVisibility(View.VISIBLE);
                }
            }

        }
    }

    /**
     * 填充答案
     *
     * @param currentSelectAnswer 当前选择的答案
     * @param position            要填充的第position个空位（从0开始）
     */
    private void fillAnswer(String currentSelectAnswer, int position) {

        if (position < 0) {
            position = 0;
        }
        if (position >= answerRangeList.size()) {
            position = answerRangeList.size() - 1;
        }

        AnswerRange range = answerRangeList.get(position);
        content.replace(range.start, range.end, currentSelectAnswer);

        AnswerRange currentRange = new AnswerRange(range.start, range.start + currentSelectAnswer.length());
        answerRangeList.set(position, currentRange);

        content.setSpan(new UnderlineSpan(),
                currentRange.start, currentRange.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvContent.setText(content);

        for (int i = 0; i < answerRangeList.size(); i++) {
            if (i > position) {
                // 获取下一个答案原来的范围
                AnswerRange oldNextRange = answerRangeList.get(i);
                int oldNextAmount = oldNextRange.end - oldNextRange.start;
                // 计算新旧答案字数的差值
                int difference = currentRange.end - range.end;

                // 更新下一个答案的范围
                AnswerRange nextRange = new AnswerRange(oldNextRange.start + difference,
                        oldNextRange.start + difference + oldNextAmount);
                answerRangeList.set(i, nextRange);
            }
        }

        saveFillAnswer(currentSelectAnswer, position);

    }


    /**
     * 回归动画
     * type：
     * 0、没有拖动到空格处，松开手后控件回到初始位置
     * 1、拖动到空格处，松开手后，隐藏控件，静默回归
     * 2、点击选项，控件移动到空缺的位置
     * 3、点击选项，内容填充到空格处后，控件需要静默回归
     */
    private void moveAnimator(final int type, float sX, float sY, float eX, float eY, final int position) {

        Log.e("type is ", type + "");
        Log.e("currentOptionPosition ", currentOptionPosition + "");

        if (currentOptionPosition == -1 || moveTextView == null) {
            return;
        }

        if (type == 1 || type == 3) {
            moveTextView.setVisibility(INVISIBLE);
        } else {
            moveTextView.setVisibility(VISIBLE);
        }

        try {

            float startX = 0;
            float startY = 0;
            float endX = 0;
            float endY = 0;

            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator translationX;
            ObjectAnimator translationY;

            if (type == 0 || type == 1) {

                String location = selectInitialLocationList.get(currentOptionPosition);
                startX = moveTextView.getX();
                startY = moveTextView.getY();

                endX = Float.parseFloat(location.split(SeparateSymbol)[0]);
                endY = Float.parseFloat(location.split(SeparateSymbol)[1]);


            } else {
                startX = sX;
                startY = sY;
                endX = eX;
                endY = eY;
            }

            translationX = ObjectAnimator.ofFloat(
                    moveTextView,
                    "translationX",
                    startX,
                    endX
            );


            translationY = ObjectAnimator.ofFloat(
                    moveTextView,
                    "translationY",
                    startY,
                    endY
            );


            animatorSet.playTogether(translationX, translationY);

            long durationTime = 1000;

            if (type == 1 || type == 3) {
                //静默回归，速度可加快，因为用户看不见
                durationTime = 100;
            }
            if (type == 2 && isSelectFill) {
                durationTime = 100;
            }

            animatorSet.setDuration(durationTime);

            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {


                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    //动画结束，重新计算
                    computeShowAnswerLocationWithViewTree();

                    if (type != 2) {
                        operateFinish();
                    }

                    if (type == 2) {

                        if (isSelectFill) {

                            moveTextView.setVisibility(GONE);
                            operateFinish();

                        } else {
                            moveTextView.setVisibility(INVISIBLE);

                            fillAnswer(moveTextView.getText().toString(), position);

                            String s = selectInitialLocationList.get(currentOptionPosition);

                            float x = Float.parseFloat(s.split(SeparateSymbol)[0]);
                            float y = Float.parseFloat(s.split(SeparateSymbol)[1]);

                            moveAnimator(3, moveTextView.getX(), moveTextView.getY(), x, y, 0);
                        }

                    }

                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });

            animatorSet.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private int dp2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
