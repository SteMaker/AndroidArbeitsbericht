package com.stemaker.arbeitsbericht.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.stemaker.arbeitsbericht.AboutDialogFragment
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
        Log.d("test", "${a.getString(R.styleable.InfoButton_infoUrl)}");
        if(c is AppCompatActivity) {
            val act: AppCompatActivity = c as AppCompatActivity
            a.getString(R.styleable.InfoButton_infoUrl)?.let { infoUrl ->
                setOnClickListener {
                    val act: AppCompatActivity = c as AppCompatActivity
                    val helperDialog = HelperDialogFragment()
                    helperDialog.url = infoUrl
                    helperDialog.show(act.supportFragmentManager, "HelperDialog")
                    Log.d("test", "clicked :)")
                }
            }
        } else {
            Log.e("InfoButton", "InfoButton can only be used from AppCompatActivity")
        }
        a.recycle()
    }

}