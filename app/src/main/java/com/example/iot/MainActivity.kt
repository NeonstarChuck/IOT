package com.example.iot

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : ComponentActivity() {

    private var threshold = 60
    private val brokerUri = "tcp://172.20.10.6:1883"
    private val topic = "audio/volume/#"
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        // Koppla UI-komponenter
        val currentNoiseText = findViewById<TextView>(R.id.currentNoiseText)
        val sthlmNoiseText = findViewById<TextView>(R.id.sthlmNoiseText)
        val umeaNoiseText = findViewById<TextView>(R.id.umeaNoiseText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val alertText = findViewById<TextView>(R.id.alertText)
        val thresholdText = findViewById<TextView>(R.id.thresholdText)
        val thresholdSeekBar = findViewById<SeekBar>(R.id.thresholdSeekBar)
        val simulateButton = findViewById<Button>(R.id.simulateButton)
        val noiseProgressBar = findViewById<ProgressBar>(R.id.noiseProgressBar)


        // Konfigurera SeekBar
        thresholdSeekBar.progress = threshold
        thresholdText.text = "Threshold: $threshold dB"

        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress
                thresholdText.text = "Threshold: $threshold dB"
                val currentDb = currentNoiseText.text.toString().toIntOrNull() ?: 0
                updateAlert(currentDb, alertText)

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Simuleringsknapp
        simulateButton.setOnClickListener {
            val noise = (20..100).random()
            updateUI(noise, currentNoiseText, statusText, noiseProgressBar, alertText)
        }

        // --- MQTT Setup ---
        try {
            mqttClient = MqttClient(brokerUri, "AndroidClient_${System.currentTimeMillis()}", MemoryPersistence())
            mqttClient.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val db = message.toString().toFloatOrNull() ?: 0f
                    runOnUiThread {
                        when (topic) {
                            "audio/volume/pi_mic" -> updateUI(db.toInt(), currentNoiseText, statusText, noiseProgressBar, alertText)
                            "audio/volume/stockholm" -> sthlmNoiseText.text = "Stockholm: ${db.toInt()} dB"
                            "audio/volume/umea" -> umeaNoiseText.text = "Umeå: ${db.toInt()} dB"
                        }
                    }
                }
                override fun connectionLost(cause: Throwable?) {
                    runOnUiThread { statusText.text = "Status: Disconnected" }
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
                runOnUiThread { statusView.text = "Status: Connected" }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { statusView.text = "Status: MQTT Failed" }
            }
        }.start()
    }

    // Uppdaterad funktion som nu tar emot ProgressBar som ett argument
    private fun updateUI(noise: Int, noiseText: TextView, status: TextView, bar: ProgressBar, alertText: TextView) {
        noiseText.text = noise.toString()
        bar.progress = noise

        if (noise > threshold) {
            status.text = "Status: TOO LOUD"
            status.setTextColor(Color.RED)
        } else {
            status.text = "Status: OK"
            status.setTextColor(Color.BLACK)
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