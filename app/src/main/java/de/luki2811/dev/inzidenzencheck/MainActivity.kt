package de.luki2811.dev.inzidenzencheck

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val data: CoronaData
        val sendButton = findViewById<Button>(R.id.button)
        sendButton.isEnabled = false
        val output = findViewById<TextView>(R.id.textOutput)
        if (availableConnection()) {
            data = CoronaData(this, this)
            data.start()
            output.text = resources.getString(R.string.download_data)
        } else {
            Toast.makeText(
                this,
                resources.getString(R.string.error_cant_load_data),
                Toast.LENGTH_LONG
            ).show()
            output.text = """
                ${resources.getString(R.string.error_no_connection)}
                ${resources.getString(R.string.error_app_restart_to_update)}
                """.trimIndent()
        }
        val file = File(applicationContext.filesDir, fileNameSettings)
        val datei = Datein(fileNameSettings)
        if (file.exists()) {
            val jsonTEMP: JSONObject
            var setOld = true
            try {
                jsonTEMP = JSONObject(datei.loadFromFile(this))
                setOld = jsonTEMP.getBoolean("automaticPlaceInput")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (setOld) {
                val inputText = findViewById<EditText>(R.id.textInput)
                val radio_lk = findViewById<RadioButton>(R.id.radioButton_landkreis)
                val radio_sk = findViewById<RadioButton>(R.id.radioButton_stadtkreis)
                val radio_bl = findViewById<RadioButton>(R.id.radioButton_bundesland)
                var oldType = -1
                val json: JSONObject
                var oldLocation: String? = null
                val dfile = Datein(fileNameSettings)
                try {
                    json = JSONObject(dfile.loadFromFile(this))
                    oldLocation = json.getString("oldLocation")
                    oldType = json.getInt("oldType")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                if (oldType == Corona.LANDKREIS) {
                    radio_bl.isChecked = false
                    radio_lk.isChecked = true
                } else if (oldType == Corona.STADTKREIS) {
                    radio_bl.isChecked = false
                    radio_sk.isChecked = true
                } else radio_bl.isChecked = true
                inputText.setText(oldLocation)
            }
        }
    }

    fun onClickRadioButtons(view: View?) {
        val data = CoronaData(this, this)
        data.setAutoCompleteList()
    }

    fun availableConnection(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun clickedButton(view: View?) {
        val output = findViewById<TextView>(R.id.textOutput)
        val inputText = findViewById<EditText>(R.id.textInput)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val textView_inzidenz = findViewById<TextView>(R.id.textInzidenz)
        val location = inputText.text.toString()
        var type = -1
        if (radioGroup.checkedRadioButtonId == R.id.radioButton_bundesland) type = Corona.BUNDESLAND
        if (radioGroup.checkedRadioButtonId == R.id.radioButton_landkreis) type = Corona.LANDKREIS
        if (radioGroup.checkedRadioButtonId == R.id.radioButton_stadtkreis) type = Corona.STADTKREIS
        var corona: Corona? = null
        if (availableConnection()) {
            val file = Datein(fileNameSettings)
            try {
                corona = Corona(location, type, this)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (corona!!.location == null) {
                if (type == Corona.LANDKREIS) Toast.makeText(
                    this,
                    resources.getString(R.string.error_cant_find_landkreis),
                    Toast.LENGTH_LONG
                ).show() else if (type == Corona.STADTKREIS) Toast.makeText(
                    this,
                    resources.getString(R.string.error_cant_find_stadtkreis),
                    Toast.LENGTH_LONG
                ).show() else if (type == Corona.BUNDESLAND) Toast.makeText(
                    this,
                    resources.getString(R.string.error_cant_find_bundesland),
                    Toast.LENGTH_LONG
                ).show() else Toast.makeText(
                    this,
                    resources.getString(R.string.error_analysis),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // set settings
                val jsonInFile: JSONObject
                val tempFile = File(applicationContext.filesDir, fileNameSettings)
                try {
                    jsonInFile =
                        if (tempFile.exists()) JSONObject(file.loadFromFile(this)) else JSONObject()
                    jsonInFile.put("oldLocation", location)
                    jsonInFile.put("oldType", type)
                    file.writeInFile(jsonInFile.toString(), this)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                // set text
                output.text = corona.location + " hat eine Coronainzidenz von:"
                textView_inzidenz.text = "" + corona.incidence + ""
                // set color for each incidence
                if (corona.incidence >= 1000) textView_inzidenz.setTextColor(getColor(R.color.DarkMagenta)) else if (corona.incidence >= 500 && corona.incidence < 1000) textView_inzidenz.setTextColor(
                    getColor(R.color.Magenta)
                ) else if (corona.incidence >= 200 && corona.incidence < 500) textView_inzidenz.setTextColor(
                    getColor(R.color.DarkRed)
                ) else if (corona.incidence >= 100 && corona.incidence < 200) textView_inzidenz.setTextColor(
                    getColor(R.color.Red)
                ) else if (corona.incidence >= 50 && corona.incidence < 100) textView_inzidenz.setTextColor(
                    getColor(R.color.Orange)
                ) else if (corona.incidence >= 25 && corona.incidence < 50) textView_inzidenz.setTextColor(
                    getColor(R.color.Yellow)
                ) else if (corona.incidence >= 10 && corona.incidence < 25) textView_inzidenz.setTextColor(
                    getColor(R.color.Green)
                ) else if (corona.incidence < 10) textView_inzidenz.setTextColor(getColor(R.color.DarkGreen)) else textView_inzidenz.setTextColor(
                    getColor(R.color.Gray)
                )
            }
        } else {
            output.text = resources.getString(R.string.error_no_connection)
        }
    }

    fun openSettings(view: View?) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val fileNameSettings = "settings.json"
        const val fileNameDataKreise = "data_kreise.json"
        const val fileNameDataBundeslaender = "data_bundeslaender.json"

        /**
         * Rundet den übergebenen Wert auf die Anzahl der übergebenen Nachkommastellen
         *
         * @param value ist der zu rundende Wert.
         * @param decimalPoints ist die Anzahl der Nachkommastellen, auf die gerundet werden soll.
         */
        fun round(value: Double, decimalPoints: Int): Double {
            val d = Math.pow(10.0, decimalPoints.toDouble())
            return Math.round(value * d) / d
        }
    }
}