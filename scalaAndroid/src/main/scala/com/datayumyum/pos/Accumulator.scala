package com.datayumyum.pos

import android.widget.TextView

object Accumulator {
  var value: String = ""
  var display: TextView = null

  def push(data: String) {
    value = value + data
    display.setText(value)
  }

  def pop(): Double = {
    try {
      if (value.length > 0) {
        return value.toDouble
      } else {
        return 0
      }
    } finally {
      reset
    }
  }

  def reset() {
    value = ""
    display.setText(value)
  }


}