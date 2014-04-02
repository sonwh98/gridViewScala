package com.datayumyum.pos

import android.widget.TextView

object Accumulator {
  var value: String = ""
  var display: TextView = null

  def push(data: String) {
    value = value + data
    display.setText(value)
  }

  def evaluate(): Double = {
    if (value.length > 0) {
      return value.toDouble
    } else {
      return 0
    }
  }

  def reset() {
    value = ""
    display.setText(value)
  }


}