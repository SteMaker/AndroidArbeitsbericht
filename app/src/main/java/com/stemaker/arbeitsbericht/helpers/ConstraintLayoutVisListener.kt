package com.stemaker.arbeitsbericht.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

class ConstraintLayoutVisListener @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private var visibilityListener: OnVisibilityChange? = null

    fun setVisibilityChangeListener(listener: OnVisibilityChange) {
        visibilityListener = listener
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        visibilityListener?.visibilityChanged(changedView, visibility != View.GONE)
    }

    interface OnVisibilityChange {
        fun visibilityChanged(view: View, visible: Boolean)
    }
}
