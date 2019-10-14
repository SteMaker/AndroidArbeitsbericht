package com.stemaker.arbeitsbericht

import android.util.Log
import android.widget.EditText
import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.NumberFormatException

@Serializable
class WorkItem(var item: String = "") {
}

@Serializable
class LumpSum(var item: String = "") {
}

@Serializable
class Material(var item: String = "", var amount: Int = 0) {
}

@Serializable
class Photo(var file: String = "", var description: String = "") {
}


















