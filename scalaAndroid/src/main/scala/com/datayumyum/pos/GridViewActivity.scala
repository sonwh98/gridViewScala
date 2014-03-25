package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.util.Log
import android.view.{ViewGroup, View, LayoutInflater}
import android.widget.{TextView, ImageButton, BaseAdapter, GridView}

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_grid_view)

    val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
    val catalog = Catalog.from(jsonStr)
    val entrees: List[Item] = catalog.findItemsByCategory("Entrees")

    val inflater: LayoutInflater = LayoutInflater.from(this)
    val itemButtonList = entrees.map(item => {
      val itemButton: View = inflater.inflate(R.layout.item_button, null)
      val imageButton: ImageButton = itemButton.findViewById(R.id.item_image_button).asInstanceOf[ImageButton]
      new DownloadImageTask(imageButton).execute(item.imageURL)

      val itemLabel: TextView = itemButton.findViewById(R.id.item_label).asInstanceOf[TextView]
      itemLabel.setText(item.name)
      itemButton
    })

    val gridView: GridView = findViewById(R.id.gridview).asInstanceOf[GridView]
    gridView.setAdapter(new BaseAdapter() {
      @Override def getCount: Int = {
        return itemButtonList.size
      }

      @Override def getItem(position: Int): Object = {
        return null
      }

      @Override def getItemId(position: Int): Long = {
        return 0
      }

      @Override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val imageView: View = itemButtonList(position)
        return imageView
      }
    })

    Log.i(TAG, gridView.toString)

  }
}

