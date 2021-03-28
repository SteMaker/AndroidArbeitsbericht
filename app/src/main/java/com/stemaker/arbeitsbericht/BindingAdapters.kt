package com.stemaker.arbeitsbericht

import android.graphics.drawable.PictureDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.caverock.androidsvg.SVG
import com.google.android.material.textfield.TextInputEditText
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.helpers.LinearLayoutVisListener
import java.io.File
import java.text.DecimalFormatSymbols

object BindingAdapters {
    /******************************************************************/
    /* Binding Adapters to bind an EditText with an integer data value*/
    /******************************************************************/
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
                    Log.d("Arbeitsbericht.BindingAdapters.textInt.setListener","afterTextChanged")
                    listener.onChange()
                }
            })
        }
    }

    @JvmStatic
    @BindingAdapter("textInt")
    fun setTextFromInt(view: TextInputEditText, value: Int) {
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
        return ret
    }

    /***************************************************************/
    /* Binding Adapters to bind an EditText with a float data value*/
    /***************************************************************/
    @JvmStatic
    @BindingAdapter("textFloatAttrChanged")
    fun setListener2(editText: TextInputEditText, listener: InverseBindingListener?) {
        if (listener != null) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(editable: Editable) {
                    val localizedSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator()
                    editText.removeTextChangedListener(this)
                    val withDecimalComma = editable.toString().replace('.', localizedSeparator)
                    editable.replace(0, withDecimalComma.length, withDecimalComma)
                    editText.addTextChangedListener(this)
                    listener.onChange()
                }
            })
        }
    }

    @JvmStatic
    @BindingAdapter("textFloat")
    fun setTextFromFloat(view: TextInputEditText, value: Float) {
        val localizedSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator()
        try {
            val old = view.text.toString().replace(localizedSeparator, '.').toFloat()
            if (old == value) return
        } catch(e:NumberFormatException) {
        }

        if(value == value.toInt().toFloat()) {
            val i = value.toInt().toString()
            view.setText(i)
        } else {
            val t = value.toString().replace('.', localizedSeparator)
            view.setText(t)
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute="textFloat")
    fun getTextFromFloat(editText: TextInputEditText): Float {
        var ret = 0f
        try {
            val localizedSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator()
            ret = editText.text.toString().replace(localizedSeparator, '.').toFloat()
        } catch(e: NumberFormatException) {
        }
        return ret
    }
    /***************************************/
    /* Binding Adapters for a Spinner View */
    /***************************************/
    @JvmStatic
    @BindingAdapter("selectedItemAttrChanged")
    fun setListener(spinner: Spinner, listener: InverseBindingListener?) {
        if (listener != null) {
            spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    listener.onChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            })
        }
    }

    @JvmStatic
    @BindingAdapter("selectedItem")
    fun setSelectedItemText(spinner: Spinner, value: String) {
        val idx = configuration().lumpSums.indexOf(value)
        if(idx >= 0) {
            spinner.setSelection(idx)
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute="selectedItem")
    fun getSelectedItemText(spinner: Spinner): String {
        val value = configuration().lumpSums[spinner.selectedItemPosition]
        return value
    }

    @JvmStatic
    @BindingAdapter("selectionList")
    fun setSelectionList(spinner: Spinner, value: List<String>) {
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(spinner.context, android.R.layout.simple_list_item_1, value).also { adapter ->
            spinner.setAdapter(adapter)
        }
    }

    /**********************************************************/
    /* Binding Adapter to define a dictionary for an EditText */
    /**********************************************************/
    @JvmStatic
    @BindingAdapter("dictionary")
    fun setDictionary(textView: AutoCompleteTextView, value: Set<String>) {
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(textView.context, android.R.layout.simple_list_item_1, value.toList()).also { adapter ->
            textView.setAdapter(adapter)
        }
    }

    /*********************************************************/
    /* Binding Adapter to bind a file string to an ImageView */
    /*********************************************************/
    @JvmStatic
    @BindingAdapter("srcFile")
    fun setSrcFile(imgView: ImageView, file: String) {
        if(file != "" && File(file).length() > 0)
            GlideApp.with(imgView.context).load(file).into(imgView)
    }

    /********************************************************/
    /* Binding Adapter to bind a svg string to an ImageView */
    /********************************************************/
    @JvmStatic
    @BindingAdapter("svgString")
    fun setSvgString(imgView: ImageView, svgString: String) {
        if(svgString != "") {
            val svg = SVG.getFromString(svgString)
            val pd = PictureDrawable(svg.renderToPicture())
            imgView.setImageDrawable(pd)
        }
    }

    /*************************************************************/
    /* Binding Adapters to bind a View's visibility to a Boolean */
    /*************************************************************/
    @JvmStatic
    @BindingAdapter("visibilityAttrChanged")
    fun setListener(view: LinearLayoutVisListener, listener: InverseBindingListener?) {
        if (listener != null) {
            view.setVisibilityChangeListener(object: LinearLayoutVisListener.onVisibilityChange {
                override fun visibilityChanged(view: View, visible: Boolean) {
                    listener.onChange()
                }
            })
        }
    }

    @JvmStatic
    @BindingAdapter("visibility")
    fun setVisibility(view: LinearLayoutVisListener, visible: Boolean) {
        if(visible) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute="visibility")
    fun getVisibility(view: LinearLayoutVisListener): Boolean {
        return view.visibility!=View.GONE
    }

    /**********************************************************************************/
    /* Binding Adapters to bind a ReportState to an ImageView (done vs. in work icon) */
    /**********************************************************************************/
    @JvmStatic
    @BindingAdapter("doneFlag")
    fun setDoneFlag(imgView: ImageView, done: ReportData.ReportState) {
        when(done) {
            ReportData.ReportState.IN_WORK -> imgView.setImageResource(ArbeitsberichtApp.getInWorkIconDrawable())
            ReportData.ReportState.DONE -> imgView.setImageResource(ArbeitsberichtApp.getDoneIconDrawable())
            ReportData.ReportState.ON_HOLD -> imgView.setImageResource(ArbeitsberichtApp.getOnHoldIconDrawable())
            ReportData.ReportState.ARCHIVED -> imgView.setImageResource(ArbeitsberichtApp.getArchivedIconDrawable())
        }
    }
}