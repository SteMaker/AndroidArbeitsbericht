package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText

class ConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        val cfg = storageHandler().configuration
        findViewById<EditText>(R.id.config_employee_name).setText(cfg.employeeName)
        findViewById<EditText>(R.id.config_next_id).setText(cfg.currentId.toString())
        findViewById<EditText>(R.id.config_mail_receiver).setText(cfg.recvMail)

    }

    fun onClickSave(@Suppress("UNUSED_PARAMETER") saveButton: View) {
        val cfg = storageHandler().configuration
        cfg.employeeName = findViewById<EditText>(R.id.config_employee_name).getText().toString()
        cfg.currentId = findViewById<EditText>(R.id.config_next_id).getText().toString().toInt()
        cfg.recvMail = findViewById<EditText>(R.id.config_mail_receiver).getText().toString()
        storageHandler().saveConfigurationToFile(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java).apply {}
        startActivity(intent)
    }
}
