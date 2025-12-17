package com.example.iot

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.random.Random
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MainActivity : ComponentActivity() {

    private var threshold = 70
    // Use 10.0.2.2 for Emulator, or your MacBook IP for a real phone
    private val brokerUri = "tcp://192.168.0.85"
    private val topic = "audio/volume/#"
    private lateinit var mqttClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        // --- 1. Initialize UI Elements from XML ---
        val currentNoiseText = findViewById<TextView>(R.id.currentNoiseText)
        val sthlmNoiseText = findViewById<TextView>(R.id.sthlmNoiseText)
        val umeaNoiseText = findViewById<TextView>(R.id.umeaNoiseText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val ledStatusText = findViewById<TextView>(R.id.ledStatusText)
        val thresholdText = findViewById<TextView>(R.id.thresholdText)
        val thresholdSeekBar = findViewById<SeekBar>(R.id.thresholdSeekBar)
        val simulateButton = findViewById<Button>(R.id.simulateButton)

        // --- 2. SeekBar Logic ---
        thresholdSeekBar.progress = threshold
        thresholdText.text = "Threshold: $threshold dB"
        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress
                thresholdText.text = "Threshold: $threshold dB"
                // Re-check UI status when threshold moves
                val currentDb = currentNoiseText.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
                updateUI(currentDb, currentNoiseText, statusText, ledStatusText)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- 3. Local Simulation Button ---
        simulateButton.setOnClickListener {
            val noise = Random.nextInt(20, 100)
            updateUI(noise, currentNoiseText, statusText, ledStatusText)
        }

        // --- 4. MQTT Setup ---
        mqttClient = MqttAndroidClient(this, brokerUri, "AndroidClient")
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
            override fun connectionLost(cause: Throwable?) { statusText.text = "Status: Disconnected" }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        connectToBroker(statusText)
    }

    private fun connectToBroker(statusView: TextView) {
        try {
            mqttClient.connect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    mqttClient.subscribe(topic, 0)
                    statusView.text = "Status: Connected"
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    statusView.text = "Status: MQTT Failed"
                }
            })
        } catch (e: Exception) { e.printStackTrace() }
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