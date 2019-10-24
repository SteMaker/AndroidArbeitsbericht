package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout

class LinearLayoutVisListener @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    var visibilityListener: onVisibilityChange? = null

    fun setVisibilityChangeListener(listener: onVisibilityChange) {
        visibilityListener = listener
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        visibilityListener?.visibilityChanged(changedView, if(visibility==View.GONE) false else true)
        Log.d("Arbeitsbericht.LinearLayoutVisListener", "View " + changedView + " changed visibility to " + visibility);
    }

    interface onVisibilityChange {
        fun visibilityChanged(view: View, visible: Boolean)
    }
}