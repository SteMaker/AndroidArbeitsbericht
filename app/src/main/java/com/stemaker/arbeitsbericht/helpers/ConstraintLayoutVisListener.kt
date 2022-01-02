package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

class ConstraintLayoutVisListener @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    var visibilityListener: onVisibilityChange? = null

    fun setVisibilityChangeListener(listener: onVisibilityChange) {
        visibilityListener = listener
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        visibilityListener?.visibilityChanged(changedView, visibility != View.GONE)
    }

    interface onVisibilityChange {
        fun visibilityChanged(view: View, visible: Boolean)
    }
}
