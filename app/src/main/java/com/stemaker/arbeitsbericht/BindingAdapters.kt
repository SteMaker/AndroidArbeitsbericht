package com.stemaker.arbeitsbericht

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.textfield.TextInputEditText

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("textIntAttrChanged")
    fun setListener(editText: TextInputEditText, listener: InverseBindingListener?) {
        if (listener != null) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(editable: Editable) {
                    Log.d("BindingAdapters.textInt.setListener","afterTextChanged")
                    listener.onChange()
                }
            })
        }
    }

    @JvmStatic
    @BindingAdapter("textInt")
    fun setTextFromInt(view: TextInputEditText, value: Int) {
        Log.d("BindingAdapters.textInt.setTextFromInt","Value = $value")
        try {
            val old = view.text.toString().toInt()
            if (old == value) return
        } catch(e:NumberFormatException) {
        }
        view.setText(value.toString())
    }

    @JvmStatic
    @InverseBindingAdapter(attribute="textInt")
    fun getTextFromInt(editText: TextInputEditText): Int {
        var ret = 0
        try {
            ret = editText.text.toString().toInt()
        } catch(e: NumberFormatException) {
        }
        Log.d("BindingAdapters.textInt.getTextFromInt","ret = $ret")
        return ret
    }
}