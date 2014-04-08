package com.datayumyum.pos

import com.starmicronics.stario.{StarIOPortException, StarPrinterStatus, StarIOPort}
import android.util.Log
import scala.collection.mutable.ArrayBuffer

object Printer {
  val TAG = "com.datayumyum.pos.Printer"

  val CUT_PAPER: Array[Byte] = Array(0x1b, 0x64, 0x02)
  val ALIGN_CENTER: Array[Byte] = Array(0x1b, 0x1d, 0x61, 0x01)
  val ALIGNMENT: Array[Byte] = Array(0x1b, 0x1d, 0x61, 0x00)
  val HORIZONTAL_TAB: Array[Byte] = Array(0x1b, 0x44, 0x02, 0x10, 0x22, 0x00)
  val BOLD_ON: Array[Byte] = Array(0x1b, 0x45)
  val BOLD_OFF: Array[Byte] = Array(0x1b, 0x46)

  def print(receipt: Receipt) {
    val store: Store = receipt.store
    val header = ALIGN_CENTER ++ store.name.getBytes() ++ ALIGN_CENTER ++ store.address.toString.getBytes() ++ ALIGNMENT ++ HORIZONTAL_TAB
    val body = receipt.lineItems.map {
      case (quantity, item) => {
        f"$quantity%s ${item.name} ${item.price * quantity}"
      }
    }.mkString("\n")

    val cmd = header ++ "\n\n".getBytes() ++ body.getBytes() ++ CUT_PAPER
    sendCommand(cmd)
  }

  def sendCommand(cmd: Array[Byte]) {
    val port = StarIOPort.getPort("BT:Star Micronics", "", 1000)
    Thread.sleep(100)
    val status: StarPrinterStatus = port.beginCheckedBlock()
    port.writePort(cmd, 0, cmd.length)
    val status2 = port.endCheckedBlock()
    StarIOPort.releasePort(port)
  }

  def cutPaper() {
    sendCommand(CUT_PAPER)
  }
}