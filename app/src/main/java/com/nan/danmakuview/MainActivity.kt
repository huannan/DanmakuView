package com.nan.danmakuview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nan.danmakuview.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    val contents = listOf(
        "\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02",
        "声音好听到不行",
        "哈哈哈哈哈哈哈哈哈哈哈哈哈哈",
        "胡小盼我爱你",
        "根本停不下来",
        "卧槽",
        "前方高能！！！",
        "弹幕护体",
        "[○･｀Д´･ ○]",
        "完结撒花",
        "再见",
        "啊啊啊太甜了",
        "啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊",
        "哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈",
        "笑出猪叫声",
        "加油",
        "混入其中",
        "(*^▽^*)",
        "歌神",
        "前排围观",
        "日常打卡",
        "唱得太好听了，下次不要再唱了",
    )
    val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnSend.setOnClickListener {
            val index = random.nextInt(contents.size)
            viewBinding.danmakuView.addDanmaku(contents[index])
        }

        viewBinding.btnResume.setOnClickListener {
            viewBinding.danmakuView.resume()
        }

        viewBinding.btnPause.setOnClickListener {
            viewBinding.danmakuView.pause()
        }

        viewBinding.btnFinish.setOnClickListener {
            finish()
        }
    }
}