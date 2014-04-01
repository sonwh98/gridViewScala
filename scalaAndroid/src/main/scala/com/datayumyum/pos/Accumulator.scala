package com.datayumyum.pos

object Accumulator {
  var value: String = ""

  def push(data: String) {
    value = value + data
  }

  def evaluate(): Int = {
    if (value.length > 0) {
      return value.toInt
    } else {
      return 0
    }
  }

  def reset() {
    value = ""
  }


}