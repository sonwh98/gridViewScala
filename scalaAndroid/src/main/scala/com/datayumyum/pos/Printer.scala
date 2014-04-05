package com.datayumyum.pos

import com.starmicronics.stario.{StarIOPortException, StarPrinterStatus, StarIOPort}
import android.util.Log

object Printer {
  val CUT: Array[Byte] = Array(0x1b, 0x64, 0x02)
  val TAG = "com.datayumyum.pos.Printer"

  def sendCommand(cmd: Array[Byte]) {
    try {
      val port = StarIOPort.getPort("BT:Star Micronics", "", 1000)
      Thread.sleep(100)
      val status: StarPrinterStatus = port.beginCheckedBlock()
      port.writePort(cmd, 0, cmd.length)
      val status2 = port.endCheckedBlock()
      StarIOPort.releasePort(port)
    } catch {
      case ex: StarIOPortException => Log.e(TAG, "Printer not available")
    }
  }

  def cutPaper() {
    sendCommand(CUT)
  }
}