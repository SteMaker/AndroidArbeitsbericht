package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.stemaker.arbeitsbericht.R

class InfoButton : AppCompatImageView {
    constructor(c: Context): super(c) {
        initialize(c, null)
    }
    constructor(c: Context, attrs: AttributeSet?): super(c, attrs) {
        initialize(c, attrs)
    }
    constructor(c: Context, attrs: AttributeSet?, defStyleAttr: Int): super(c, attrs, defStyleAttr) {
        initialize(c, attrs)
    }
    private fun initialize(c: Context, attrs: AttributeSet?) {
        val a = c.obtainStyledAttributes(
            attrs,
            R.styleable.InfoButton)
        if(c is AppCompatActivity) {
            a.getString(R.styleable.InfoButton_infoUrl)?.let { infoUrl ->
                setOnClickListener {
                    val act: AppCompatActivity = c
                    val helperDialog = HelperDialogFragment()
                    helperDialog.url = infoUrl
                    helperDialog.show(act.supportFragmentManager, "HelperDialog")
                }
            }
        } else {
            Log.e("InfoButton", "InfoButton can only be used from AppCompatActivity")
        }
        a.recycle()
    }

}