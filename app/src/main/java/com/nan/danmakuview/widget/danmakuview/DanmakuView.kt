package com.nan.danmakuview.widget.danmakuview

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.util.Pools
import com.nan.danmakuview.R
import java.util.*

private const val TAG = "DanmakuView"

private const val DEFAULT_ITEM_ANIMATION_VELOCITY = 0.3F
private const val DEFAULT_MAX_LINE = 3
private const val MAX_POOL_SIZE = 20

private const val STRATEGY_SEQUENTIAL = 1
private const val STRATEGY_RANDOM = 2

/**
 * 一个基于属性动画的简单可靠可自由修改弹幕内容布局的高性能弹幕库
 * 实现原理：
 * 1. 属性动画控制弹幕滚动、暂停、播放
 * 2. 通过缓存池防止内存抖动
 * 3. 自定义控件、测量等相关知识
 * 4. 队列数据结构实现弹幕排队
 */
class DanmakuView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 弹道选择策略
    private var strategy = STRATEGY_SEQUENTIAL

    // 弹幕高度
    private val lineHeight = resources.getDimensionPixelSize(R.dimen.item_danmaku_line_height)

    // 弹幕弹道间距
    private var lineMargin = 0

    // 相同弹道弹幕的最小宽度
    private var itemMinMargin = 0

    // 动画插值器
    private val itemInterpolator by lazy { LinearInterpolator() }

    // 动画时间，单位px/ms
    private var itemAnimationVelocity = DEFAULT_ITEM_ANIMATION_VELOCITY

    // 是否正在播放
    private var isPlaying = true

    // 最大弹道数
    private var maxLine = DEFAULT_MAX_LINE

    // 下一次展示的弹道行号
    private var nextLine = 0

    // 随机行号
    private val random by lazy { Random() }

    // 弹道容器
    private val lines = HashMap<Int, LinkedList<DanmakuItem>>()

    // 待发送弹幕容器
    private val sendQueue = LinkedList<String>()

    // 弹幕缓存池
    private val pool = Pools.SimplePool<View>(MAX_POOL_SIZE)

    // 是否已销毁
    private var isDestroy = false

    init {
        // 初始化控件属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DanmakuView)
        maxLine = typedArray.getInt(R.styleable.DanmakuView_danmaku_max_line, DEFAULT_MAX_LINE)
        lineMargin = typedArray.getDimensionPixelSize(R.styleable.DanmakuView_danmaku_line_margin, resources.getDimensionPixelSize(R.dimen.item_danmaku_line_margin))
        itemMinMargin = typedArray.getDimensionPixelSize(R.styleable.DanmakuView_danmaku_item_min_margin, resources.getDimensionPixelSize(R.dimen.item_danmaku_item_min_margin))
        itemAnimationVelocity = typedArray.getFloat(R.styleable.DanmakuView_danmaku_velocity, DEFAULT_ITEM_ANIMATION_VELOCITY)
        strategy = typedArray.getInt(R.styleable.DanmakuView_danmaku_strategy, STRATEGY_SEQUENTIAL)
        typedArray.recycle()
        // 初始化弹道
        for (i in 0 until maxLine) {
            lines[i] = LinkedList()
        }
    }

    /**
     * 添加弹幕
     */
    fun addDanmaku(content: String) {
        if (TextUtils.isEmpty(content)) {
            Log.d(TAG, "addDanmaku: content is empty")
            return
        }
        if (isPlaying) {
            // 初始化弹幕布局
            val danmakuItemView = pool.acquire() ?: LayoutInflater.from(context).inflate(R.layout.item_danmaku, this, false)
            // 设置内容，加空格是为了防止文字显示被截断
            val danmakuTextView = danmakuItemView.findViewById<TextView>(R.id.tv_danmaku)
            danmakuTextView.text = resources.getString(R.string.text_danmaku, content)
            // 根据策略确定需要展示弹道
            val line = nextLine
            // 切换下一次展示的弹道
            if (strategy == STRATEGY_SEQUENTIAL) {
                nextLine++
                if (nextLine >= maxLine) {
                    nextLine = 0
                }
            } else if (strategy == STRATEGY_RANDOM) {
                nextLine = random.nextInt(maxLine)
            }
            // 获取弹道
            val danmakuLine = lines.getValue(line)
            // 添加弹幕
            addView(danmakuItemView)
            // 测量弹幕长度
            danmakuItemView.measure(0, 0)
            // 计算起始坐标
            val lastDanmaku = danmakuLine.peekLast()
            if (null == lastDanmaku) {
                // 没有上一条弹幕，直接从屏幕进入
                danmakuItemView.x = width.toFloat()
            } else {
                val lastDanmakuItemRight = lastDanmaku.itemView.x + lastDanmaku.itemView.measuredWidth
                val lastDanmakuItemRightDiff = width - itemMinMargin - lastDanmakuItemRight
                if (lastDanmakuItemRightDiff <= 0F) {
                    // 上一条弹幕未进入屏幕或进入屏幕后滚动距离未超过最小弹幕距离，直接跟在上一条弹幕后面
                    danmakuItemView.x = lastDanmakuItemRight + itemMinMargin
                } else {
                    // 上一条弹幕进入屏幕后滚动的距离大于登录最小弹幕距离，直接从屏幕进入
                    danmakuItemView.x = width.toFloat()
                }
            }
            danmakuItemView.y = (line * (lineHeight + lineMargin)).toFloat()
            val danmakuItemAnimator = ObjectAnimator.ofFloat(danmakuItemView, "translationX", (-danmakuItemView.measuredWidth).toFloat())
            danmakuItemAnimator.repeatCount = 0
            danmakuItemAnimator.interpolator = itemInterpolator
            // 动画时间需要根据速度计算
            danmakuItemAnimator.duration = ((danmakuItemView.x + danmakuItemView.measuredWidth) / itemAnimationVelocity).toLong()
            // 添加到弹道容器
            val danmakuItem = DanmakuItem(line, content, danmakuItemView, danmakuItemAnimator)
            danmakuLine.offer(danmakuItem)
            danmakuItemAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    if (isDestroy) {
                        return
                    }
                    // 从父控件中移除
                    removeView(danmakuItemView)
                    // 从弹道中移除
                    danmakuLine.remove(danmakuItem)
                    // 回收弹幕
                    pool.release(danmakuItemView)
                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
            // 开始弹幕滚动动画
            danmakuItemAnimator.start()
        } else {
            // 当前是暂停状态，需要添加到发送队列
            sendQueue.offer(content)
        }
    }

    /**
     * 播放
     */
    fun resume() {
        if (!isPlaying) {
            isPlaying = true
            lines.values.forEach { queue ->
                queue.forEach {
                    it.animator.resume()
                }
            }
            while (sendQueue.peek() != null) {
                val content = sendQueue.poll() ?: ""
                addDanmaku(content)
            }
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        if (isPlaying) {
            isPlaying = false
            lines.values.forEach { queue ->
                queue.forEach {
                    it.animator.pause()
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(MeasureSpec.UNSPECIFIED, heightMeasureSpec)
    }

    override fun onDetachedFromWindow() {
        isPlaying = false
        isDestroy = true
        lines.values.forEach { queue ->
            queue.forEach {
                it.animator.cancel()
            }
            queue.clear()
        }
        sendQueue.clear()
        super.onDetachedFromWindow()
    }
}