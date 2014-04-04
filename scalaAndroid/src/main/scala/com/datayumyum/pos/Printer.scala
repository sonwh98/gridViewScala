package com.datayumyum.pos

import com.starmicronics.stario.{StarPrinterStatus, StarIOPort}

object Printer {
  val CUT: Array[Byte] = Array(0x1b, 0x64, 0x02)

  def sendCommand(cmd: Array[Byte]) {
    val port = StarIOPort.getPort("BT:Star Micronics", "", 1000)
    Thread.sleep(100)
    val status: StarPrinterStatus = port.beginCheckedBlock()
    port.writePort(cmd, 0, cmd.length)
    val status2 = port.endCheckedBlock()
    StarIOPort.releasePort(port)
  }

  def cutPaper() {
    sendCommand(CUT)
  }
}