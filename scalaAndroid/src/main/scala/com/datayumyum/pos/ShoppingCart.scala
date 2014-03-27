package com.datayumyum.pos

import android.widget.BaseAdapter
import android.view.{ViewGroup, View}
import android.util.Log
import scala.collection.mutable

object ShoppingCart extends BaseAdapter {
  val lineItems = new mutable.ArrayBuffer[(Int, Item)]()
  val TAG = "com.datayumyum.pos.ShoppingCart"

  override def getCount: Int = {
    return lineItems.size
  }

  override def getItem(position: Int): Object = {
    return lineItems(position)
  }

  override def getItemId(position: Int): Long = {
    return 0
  }

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    return convertView
  }

  def add(item: Item) {
    val (quantity, foundItem) = lineItems.find {
      case (quantity, item1) => item == item1
    }.getOrElse((1, item))
    lineItems.append((quantity + 1, foundItem))
    Log.i(TAG, item.name)

  }
}