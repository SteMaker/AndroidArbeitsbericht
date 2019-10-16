package com.stemaker.arbeitsbericht

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import kotlinx.android.synthetic.main.activity_lump_sum_definition.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_report_editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LumpSumDefinitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lump_sum_definition)

        val lumpSums = storageHandler().configuration.lumpSums
        for (ls in lumpSums) {
            addLumpSumView(ls)
        }
    }

    fun onClickAddLumpSumDefinition(btn: View) {
        addLumpSumView("Unbekannt")
    }

    fun addLumpSumView(ls: String) {
        // Prepare a lump_sum_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.lump_sum_layout, null) as CardView
        cV.findViewById<TextView>(R.id.lump_sum_text).setText(ls)

        val btnDel = cV.findViewById<ImageButton>(R.id.lump_sum_del_button)
        btnDel.setTag(R.id.TAG_CARDVIEW, cV)

        val pos = lump_sums_container.getChildCount()
        Log.d("Arbeitsbericht.LumpSumDefinitionActivity.addLumpSumView", "Adding lump-sum card $pos to UI")
        lump_sums_container.addView(cV, pos)
    }

    fun onClickSave(btn: View) {
        Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickSave", "saving ${lump_sums_container.getChildCount()} lump-sums ...")
        val lumpSums = mutableListOf<String>()
        for (pos in 0 until lump_sums_container.getChildCount()) {
            val cV = lump_sums_container.getChildAt(pos) as CardView
            val lumpSum = cV.findViewById<EditText>(R.id.lump_sum_text).text.toString()
            lumpSums.add(lumpSum)
            Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickSave", "saving $pos")
        }
        storageHandler().configuration.lumpSums = lumpSums
        storageHandler().saveConfigurationToFile(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelLumpSum(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@LumpSumDefinitionActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickDelLumpSum", "deleting a lump sum element")
                lump_sums_container.removeView(btn.getTag(R.id.TAG_CARDVIEW) as View)
            } else {
                Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickDelLumpSum", "Cancelled deleting a lump sum element")
            }
        }
    }
}
