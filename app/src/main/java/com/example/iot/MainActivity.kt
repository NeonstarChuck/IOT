package com.example.iot

import com.example.iot.R
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.*
import kotlin.random.Random


class MainActivity : ComponentActivity() {

    private var threshold = 70

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

        thresholdSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress
                thresholdText.text = "Threshold: $threshold dB"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        simulateButton.setOnClickListener {
            val noise = Random.nextInt(40, 100)
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
}
