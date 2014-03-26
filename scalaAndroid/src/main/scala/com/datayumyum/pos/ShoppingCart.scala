package com.datayumyum.pos

import android.widget.BaseAdapter
import android.view.{ViewGroup, View}
import android.util.Log

object ShoppingCart extends BaseAdapter {
  var lineItems: List[(Int, Item)] = List()
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
    val result: Option[(Int, Item)] = lineItems.find(lineItem => item == i)
    Log.i(TAG, item.name)

  }
}