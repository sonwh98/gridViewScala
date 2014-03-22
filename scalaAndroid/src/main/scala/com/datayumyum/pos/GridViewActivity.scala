package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.util.Log
import scala.util.parsing.json.JSON
import android.widget.Toast

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    def parseJson(): Map[String, List[Map[String, Tuple2[String, Any]]]] = {
      val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
      val result: Option[Any] = JSON.parseFull(jsonStr)
      result match {
        case Some(r: Map[String, List[Map[String, Tuple2[String, Any]]]]) => {
          return r
        }
        case Some(_) => {
          throw new RuntimeException("cannot parse json " + jsonStr)
        }
        case None => {
          Toast.makeText(this, "cannot parse json", 2000).show
          throw new RuntimeException("cannot parse json " + jsonStr)
        }
      }
    }

    val catalog: Map[String, Any] = parseJson()

    Log.i(TAG, catalog.toString)
    setContentView(R.layout.activity_grid_view)
  }
}

