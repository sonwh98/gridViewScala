package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.util.Log

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
    val catalog = Catalog.from(jsonStr)
    val entrees: List[Item] = catalog.findItemsByCategory("Entrees")
    for (item <- entrees) {
      Log.i(TAG, item.name + " " + item.price)
    }

    setContentView(R.layout.activity_grid_view)
  }
}

