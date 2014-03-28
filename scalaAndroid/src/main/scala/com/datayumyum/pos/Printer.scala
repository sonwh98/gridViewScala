package com.datayumyum.pos

import com.starmicronics.stario.StarIOPort

object Printer {
  def sendCommand(bytes: List[Byte]) {
    val port: StarIOPort = StarIOPort.getPort("BT:Star Micronics", "", 1000)

  }
}