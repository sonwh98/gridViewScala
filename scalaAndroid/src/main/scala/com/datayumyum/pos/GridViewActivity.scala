package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.view.{ViewGroup, View, LayoutInflater}
import android.widget.{TextView, ImageButton, BaseAdapter, GridView}
import android.util.Log

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_grid_view)

    val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
    val catalog = Catalog.from(jsonStr)
    val entrees: List[Item] = catalog.findItemsByCategory("Entrees")
    val gridView: GridView = findViewById(R.id.gridview).asInstanceOf[GridView]
    gridView.setAdapter(entrees)
  }

  implicit class GridAdapter(items: List[Item]) extends BaseAdapter {
    var itemButtonList: List[View] = List()

    override def getCount: Int = {
      return items.size
    }

    override def getItem(position: Int): Object = {
      return null
    }

    override def getItemId(position: Int): Long = {
      return 0
    }

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      if (itemButtonList.size == 0) {
        val inflater: LayoutInflater = LayoutInflater.from(GridViewActivity.this)
        itemButtonList = items.map((item: Item) => {
          val itemButton: View = inflater.inflate(R.layout.item_button, null)
          val imageButton: ImageButton = itemButton.findViewById(R.id.item_image_button).asInstanceOf[ImageButton]
          new DownloadImageTask(imageButton).execute(item.imageURL)
          imageButton.setOnClickListener((v: View) => {
            Log.i(TAG, item.name)
          })
          val itemLabel: TextView = itemButton.findViewById(R.id.item_label).asInstanceOf[TextView]
          itemLabel.setText(item.name)
          itemButton
        })
      }

      val imageView: View = itemButtonList(position)
      return imageView
    }
  }

  implicit class OnClickListener(onClickCallBack: View => Any) extends View.OnClickListener {

    def onClick(v: View) {
      onClickCallBack(v)
    }
  }

}

