package com.lab1.calculator

import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var text: String = ""
    var first: Double = 0.0
    var second: Double = 0.0
    var operation: String = ""

    fun getDisplayText(): String {
        return if (text.isNotEmpty()) text else "0"
    }

}
