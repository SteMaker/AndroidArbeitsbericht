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
import com.stemaker.arbeitsbericht.helpers.LinearLayoutVisListener
import java.io.File

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
        Log.d("Arbeitsbericht.BindingAdapters.textInt.setTextFromInt","Value = $value")
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
        Log.d("Arbeitsbericht.BindingAdapters.textInt.getTextFromInt","ret = $ret")
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
                    Log.d("Arbeitsbericht.BindingAdapters.selectedItemAttrChanged.setListener","onItemSelected")
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
        Log.d("Arbeitsbericht.BindingAdapters.selectedItem.setSelectedItemText","Value = $value")

        val idx = configuration().lumpSums.indexOf(value)
        if(idx >= 0) {
            spinner.setSelection(idx)
        }
    }

    @JvmStatic
    @InverseBindingAdapter(attribute="selectedItem")
    fun getSelectedItemText(spinner: Spinner): String {
        val value = configuration().lumpSums[spinner.selectedItemPosition]
        Log.d("Arbeitsbericht.BindingAdapters.selectedItem.getSelectedItemText","Value = $value")
        return value
    }

    @JvmStatic
    @BindingAdapter("selectionList")
    fun setSelectionList(spinner: Spinner, value: List<String>) {
        Log.d("Arbeitsbericht.BindingAdapters.selectionList.setSelectionList","called")
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
        Log.d("Arbeitsbericht.BindingAdapters.dictionary.setDictionary","called")
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
        Log.d("Arbeitsbericht.BindingAdapters.srcFile.setSrcFile","called")
        if(file != "" && File(file).length() > 0)
            GlideApp.with(imgView.context).load(file).into(imgView)
    }

    /********************************************************/
    /* Binding Adapter to bind a svg string to an ImageView */
    /********************************************************/
    @JvmStatic
    @BindingAdapter("svgString")
    fun setSvgString(imgView: ImageView, svgString: String) {
        Log.d("Arbeitsbericht.BindingAdapters.svgString.setSvgString: ",if(svgString=="") "no data" else "with SVG")
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
}