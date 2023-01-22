package com.stemaker.arbeitsbericht

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.databinding.WaitingStartupLayoutBinding
import kotlinx.coroutines.*

private const val TAG = "ArbeitsberichtActivity"

abstract class ArbeitsberichtActivity(): AppCompatActivity() {
    lateinit var waitScreenBinding: WaitingStartupLayoutBinding
    lateinit var app: ArbeitsberichtApp
    lateinit var prefs: AbPreferences

    override fun onStop() {
        super.onStop()
        runBlocking {
            if(app.waitForShutdown()) {
                Log.e(TAG, "Error: A shutdown job at application could not finish in time")
            }
        }
    }

    fun onCreateWrapper(savedInstanceState: Bundle?): Boolean {
        app = application as ArbeitsberichtApp
        val job = app.initJob?:GlobalScope.launch(Dispatchers.Main) {
            Log.e(TAG, "Error: Application initialization job non-existsnt during activity startup")
            delay(1000)
        }
        return if(job.isCompleted) {
            prefs = app.prefs
            true
        } else {
            super.onCreate(savedInstanceState)
            waitScreenBinding = DataBindingUtil.setContentView(this, R.layout.waiting_startup_layout)
            GlobalScope.launch(Dispatchers.Main) {
                job.join()
                recreate()
            }
            false
        }
    }
}