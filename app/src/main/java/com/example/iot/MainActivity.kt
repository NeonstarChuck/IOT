package com.example.iot

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : ComponentActivity() {

    private var threshold = 70
    private val brokerUri = "tcp://172.20.10.6:1883"
    private val topic = "audio/volume/#"
    private lateinit var mqttClient: MqttClient

    // Controls which topic updates the gauge
    private var gaugeTopic = "audio/volume/pi_mic"
    private var alertDialogShown = false
    private lateinit var locationText: TextView
    private lateinit var changeCityButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        // --- UI ---
        val currentNoiseText = findViewById<TextView>(R.id.currentNoiseText)
        val sthlmNoiseText = findViewById<TextView>(R.id.sthlmNoiseText)
        val umeaNoiseText = findViewById<TextView>(R.id.umeaNoiseText)
        val linkopingNoiseText = findViewById<TextView>(R.id.linkopingNoiseText)
        val goteborgNoiseText = findViewById<TextView>(R.id.goteborgNoiseText)
        val malmoNoiseText = findViewById<TextView>(R.id.malmoNoiseText)

        val statusText = findViewById<TextView>(R.id.statusText)
        val alertText = findViewById<TextView>(R.id.alertText)
        val thresholdText = findViewById<TextView>(R.id.thresholdText)
        val noiseProgressBar = findViewById<ProgressBar>(R.id.noiseProgressBar)

        locationText = findViewById(R.id.locationText)
        changeCityButton = findViewById(R.id.btnChangeCity)

        locationText.text = "Location: Kista"
        thresholdText.text = "Threshold: $threshold dB"

        // --- Button: Toggle gauge source ---
        changeCityButton.setOnClickListener {
            if (gaugeTopic == "audio/volume/pi_mic") {
                gaugeTopic = "audio/volume/stockholm"
                locationText.text = "Location: Stockholm"
            } else {
                gaugeTopic = "audio/volume/pi_mic"
                locationText.text = "Location: Kista"
            }
        }

        // --- MQTT ---
        try {
            mqttClient = MqttClient(
                brokerUri,
                "AndroidClient_${System.currentTimeMillis()}",
                MemoryPersistence()
            )

            mqttClient.setCallback(object : MqttCallback {

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val db = message.toString().toFloatOrNull() ?: 0f

                    runOnUiThread {

                        // Update gauge ONLY for selected city
                        if (topic == gaugeTopic) {
                            updateUI(
                                db.toInt(),
                                currentNoiseText,
                                statusText,
                                noiseProgressBar,
                                alertText
                            )
                        }

                        // Always update city text views
                        when (topic) {
                            "audio/volume/stockholm" ->
                                sthlmNoiseText.text = "Stockholm: ${db.toInt()} dB"

                            "audio/volume/umea" ->
                                umeaNoiseText.text = "Umeå: ${db.toInt()} dB"

                            "audio/volume/linkoping" ->
                                linkopingNoiseText.text = "Linköping: ${db.toInt()} dB"

                            "audio/volume/goteborg" ->
                                goteborgNoiseText.text = "Göteborg: ${db.toInt()} dB"

                            "audio/volume/malmo" ->
                                malmoNoiseText.text = "Malmö: ${db.toInt()} dB"
                        }
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    runOnUiThread {
                        statusText.text = "Status: Disconnected"
                        statusText.setTextColor(Color.RED)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            connectToBroker(statusText)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connectToBroker(statusView: TextView) {
        Thread {
            try {
                val options = MqttConnectOptions()
                options.isCleanSession = true
                mqttClient.connect(options)
                mqttClient.subscribe(topic, 0)

                runOnUiThread {
                    statusView.text = "Status: Connected"
                    statusView.setTextColor(Color.GREEN)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusView.text = "Status: MQTT Failed"
                    statusView.setTextColor(Color.RED)
                }
            }
        }.start()
    }
    private fun showNoiseDialog(location: String, noise: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Noise Warning")
            .setMessage("Location: $location\nThe noise level is too loud ($noise dB).")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateUI(
        noise: Int,
        noiseText: TextView,
        status: TextView,
        bar: ProgressBar,
        alertText: TextView
    ) {
        noiseText.text = noise.toString()
        bar.progress = noise

        val location =
            if (gaugeTopic == "audio/volume/pi_mic") "Kista" else "Stockholm"

        if (noise > threshold) {
            status.text = "Status: TOO LOUD"
            status.setTextColor(Color.RED)

            if (!alertDialogShown) {
                showNoiseDialog(location, noise)
                alertDialogShown = true
            }
        } else {
            status.text = "Status: OK"
            status.setTextColor(Color.BLACK)
            alertDialogShown = false // reset when noise drops
        }

        updateAlert(noise, alertText)
    }


    private fun updateAlert(noise: Int, alertText: TextView) {
        if (noise > threshold) {
            alertText.text = "Noise level exceeds recommended limits."
        } else {
            alertText.text = "Noise level within acceptable range"
        }
    }
}
