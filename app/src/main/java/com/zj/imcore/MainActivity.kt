package com.zj.imcore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zj.im.chat.utils.netUtils.IConnectivityManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        IConnectivityManager.init(application) {
            println("zjj -----  ${it.name}")
        }
        btn1.setOnClickListener {
            println("zjj -----  ${IConnectivityManager.isNetWorkActive.name}")
        }
        btn2.setOnClickListener {
            IConnectivityManager.shutDown(this.application)
        }

    }

}
