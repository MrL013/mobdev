package com.lab1.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lab1.calculator.CalculatorViewModel

class MainActivity : AppCompatActivity() {

    private val model: CalculatorViewModel by viewModels()

    lateinit var display: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)
        refreshDisplay()

        // Цифры
        findViewById<Button>(R.id.b1).setOnClickListener { pressNumber("1") }
        findViewById<Button>(R.id.b2).setOnClickListener { pressNumber("2") }
        findViewById<Button>(R.id.b3).setOnClickListener { pressNumber("3") }
        findViewById<Button>(R.id.b4).setOnClickListener { pressNumber("4") }
        findViewById<Button>(R.id.b5).setOnClickListener { pressNumber("5") }
        findViewById<Button>(R.id.b6).setOnClickListener { pressNumber("6") }
        findViewById<Button>(R.id.b7).setOnClickListener { pressNumber("7") }
        findViewById<Button>(R.id.b8).setOnClickListener { pressNumber("8") }
        findViewById<Button>(R.id.b9).setOnClickListener { pressNumber("9") }
        findViewById<Button>(R.id.b0).setOnClickListener { pressNumber("0") }
        findViewById<Button>(R.id.bDot).setOnClickListener { pressNumber(".") }

        // Операции
        findViewById<Button>(R.id.bPlus).setOnClickListener { pressOperation("+") }
        findViewById<Button>(R.id.bMinus).setOnClickListener { pressOperation("-") }
        findViewById<Button>(R.id.bMul).setOnClickListener { pressOperation("*") }
        findViewById<Button>(R.id.bDiv).setOnClickListener { pressOperation("/") }

        // Равно и очистка
        findViewById<Button>(R.id.bEqual).setOnClickListener { pressEqual() }
        findViewById<Button>(R.id.bClear).setOnClickListener { pressClear() }
    }

    fun pressNumber(n: String) {
        if (n == "." && model.text.contains(".")) return
        if (model.text == "0" && n != ".") {
            model.text = n
            refreshDisplay()
            return
        }
        if (model.text.isEmpty() && n == ".") {
            model.text = "0."
            refreshDisplay()
            return
        }
        model.text += n
        refreshDisplay()
    }

    fun pressOperation(op: String) {
        if (model.text.isNotEmpty()) {
            val parsed = model.text.toDoubleOrNull() ?: return
            model.first = parsed
            model.text = ""
            model.operation = op
            refreshDisplay()
        }
    }

    fun pressEqual() {
        if (model.text.isNotEmpty()) {
            val parsed = model.text.toDoubleOrNull() ?: return
            model.second = parsed

            val result = when (model.operation) {
                "+" -> model.first + model.second
                "-" -> model.first - model.second
                "*" -> model.first * model.second
                "/" -> if (model.second != 0.0) model.first / model.second else {
                    model.text = "Error"
                    model.operation = ""
                    refreshDisplay()
                    return
                }
                else -> {
                    refreshDisplay()
                    return
                }
            }

            model.text = result.toString()
            model.operation = ""
            refreshDisplay()
        }
    }

    fun pressClear() {
        model.text = ""
        model.first = 0.0
        model.second = 0.0
        model.operation = ""
        refreshDisplay()
    }

    private fun refreshDisplay() {
        display.text = model.getDisplayText()
    }
}
