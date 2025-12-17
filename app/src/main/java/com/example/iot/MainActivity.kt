package com.example.iot

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.random.Random
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MainActivity : ComponentActivity() {

    private var threshold = 70

    // MQTT
    private val brokerUri = "tcp://192.168.0.85" // <-- MACBOOK IP
    private val topic = "audio/volume/#" // The '#' catches all sub-topics
    private lateinit var mqttClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        val currentNoiseText = findViewById<TextView>(R.id.currentNoiseText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val thresholdText = findViewById<TextView>(R.id.thresholdText)
        val ledStatusText = findViewById<TextView>(R.id.ledStatusText)
        val thresholdSeekBar = findViewById<SeekBar>(R.id.thresholdSeekBar)
        val simulateButton = findViewById<Button>(R.id.simulateButton)

        thresholdSeekBar.progress = threshold
        thresholdText.text = "Threshold: $threshold dB"

        // ---------- SEEK BAR ----------
        thresholdSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress
                thresholdText.text = "Threshold: $threshold dB"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ---------- SIMULATION ----------
        simulateButton.setOnClickListener {
            val noise = Random.nextInt(20, 100)
            updateUI(noise, currentNoiseText, statusText, ledStatusText)
        }

        // ---------- MQTT ----------
        mqttClient = MqttAndroidClient(this, brokerUri, "AndroidClient")

        try {
            mqttClient.connect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    mqttClient.subscribe(topic, 0)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    statusText.text = "MQTT connection failed"
                }
            })

            mqttClient.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message.toString()
                    val db = payload.toFloatOrNull() ?: 0f

                    runOnUiThread {
                        when (topic) {
                            "audio/volume/pi_mic" -> {
                                updateUI(db.toInt(), currentNoiseText, statusText, ledStatusText)
                            }
                            "audio/volume/stockholm" -> {
                                // Update a Stockholm TextView if you have one
                                // stockholmText.text = "Sthlm: ${db.toInt()} dB"
                            }
                            "audio/volume/umea" -> {
                                // Update an Umea TextView if you have one
                                // umeaText.text = "Umeå: ${db.toInt()} dB"
                            }
                        }
                    }
                }
                override fun connectionLost(cause: Throwable?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------- SHARED UI LOGIC ----------
    private fun updateUI(
        noise: Int,
        currentNoiseText: TextView,
        statusText: TextView,
        ledStatusText: TextView
    ) {
        currentNoiseText.text = "Current: $noise dB"

        if (noise >= threshold) {
            statusText.text = "Status: OVER LIMIT"
            ledStatusText.text = "LED: ON"
        } else {
            statusText.text = "Status: OK"
            ledStatusText.text = "LED: OFF"
        }
    }
}
