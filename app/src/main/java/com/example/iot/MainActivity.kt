package com.example.iot

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.random.Random
// IMPORTANT: Notice the new import below
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : ComponentActivity() {

    private var threshold = 70
    private val brokerUri = "tcp://192.168.0.85:1883" // Added default port 1883
    private val topic = "audio/volume/#"
    // Changed from MqttAndroidClient to MqttClient
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        val currentNoiseText = findViewById<TextView>(R.id.currentNoiseText)
        val sthlmNoiseText = findViewById<TextView>(R.id.sthlmNoiseText)
        val umeaNoiseText = findViewById<TextView>(R.id.umeaNoiseText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val ledStatusText = findViewById<TextView>(R.id.ledStatusText)
        val thresholdText = findViewById<TextView>(R.id.thresholdText)
        val thresholdSeekBar = findViewById<SeekBar>(R.id.thresholdSeekBar)
        val simulateButton = findViewById<Button>(R.id.simulateButton)

        thresholdSeekBar.progress = threshold
        thresholdText.text = "Threshold: $threshold dB"

        simulateButton.setOnClickListener {
            val noise = Random.nextInt(20, 100)
            updateUI(noise, currentNoiseText, statusText, ledStatusText)
        }

        // --- MQTT Setup (Simplified) ---
        try {
            mqttClient = MqttClient(brokerUri, "AndroidClient_${System.currentTimeMillis()}", MemoryPersistence())
            mqttClient.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val db = message.toString().toFloatOrNull() ?: 0f
                    runOnUiThread {
                        when (topic) {
                            "audio/volume/pi_mic" -> updateUI(db.toInt(), currentNoiseText, statusText, ledStatusText)
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
        Thread { // Run connection on a background thread to prevent UI freeze
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

    private fun updateUI(noise: Int, noiseView: TextView, statusView: TextView, ledView: TextView) {
        noiseView.text = "Current: $noise dB"
        if (noise >= threshold) {
            statusView.text = "Status: OVER LIMIT"
            ledView.text = "LED: ON"
        } else {
            statusView.text = "Status: OK"
            ledView.text = "LED: OFF"
        }
    }
}