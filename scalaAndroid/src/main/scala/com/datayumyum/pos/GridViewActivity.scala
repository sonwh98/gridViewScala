package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.view.{ViewGroup, View, LayoutInflater}
import android.widget._
import android.animation.ValueAnimator
import android.view.animation.BounceInterpolator
import scala.collection.mutable

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_grid_view)

    def configureCategoryViews() {
      val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
      val catalog = Catalog.from(jsonStr)
      val entrees: List[Item] = catalog.findItemsByCategory("Entrees")
      val gridView: GridView = findViewById(R.id.gridview).asInstanceOf[GridView]
      gridView.setAdapter(entrees)
    }
    def configureLineItemView() {
      val listView: ListView = findViewById(R.id.list).asInstanceOf[ListView]
      listView.setAdapter(ShoppingCart)

      val touchListener = new SwipeDismissListViewTouchListener(listView, new SwipeDismissListViewTouchListener.DismissCallbacks() {
        override def canDismiss(position: Int): Boolean = {
          return true
        }

        override def onDismiss(listView: ListView, reverseSortedPositions: Array[Int]) {
          for (position <- reverseSortedPositions) {
            ShoppingCart.remove(position)
          }
        }
      })
      listView.setOnTouchListener(touchListener)
      listView.setOnScrollListener(touchListener.makeScrollListener())
    }

    configureCategoryViews()
    configureLineItemView()
  }

  implicit class GridAdapter(items: List[Item]) extends BaseAdapter {
    val itemButtonList: List[View] = items.map((item: Item) => {
      val inflater: LayoutInflater = getLayoutInflater()
      val itemButton: View = inflater.inflate(R.layout.item_button, null)
      val imageButton: ImageButton = itemButton.findViewById(R.id.item_image_button).asInstanceOf[ImageButton]
      new DownloadImageTask(imageButton).execute(item.imageURL)
      imageButton.setOnClickListener((v: View) => {
        val bounceAnimator: ValueAnimator = ValueAnimator.ofInt(0, imageButton.getHeight)
        bounceAnimator.setDuration(500)
        bounceAnimator.setInterpolator(new BounceInterpolator)
        bounceAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener {
          override def onAnimationUpdate(animation: ValueAnimator) {
            val value: Int = animation.getAnimatedValue.asInstanceOf[Int]
            imageButton.getLayoutParams.height = value
            imageButton.requestLayout
          }
        })
        bounceAnimator.start
        ShoppingCart.add(item)
      })

      val itemLabel: TextView = itemButton.findViewById(R.id.item_label).asInstanceOf[TextView]
      itemLabel.setText(item.name)
      itemButton
    })

    override def getCount: Int = {
      return itemButtonList.size
    }

    override def getItem(position: Int): Object = {
      return null
    }

    override def getItemId(position: Int): Long = {
      return 0
    }

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val imageView: View = itemButtonList(position)
      return imageView
    }
  }

  implicit class OnClickListener(onClickCallBack: View => Any) extends View.OnClickListener {

    def onClick(v: View) {
      onClickCallBack(v)
    }
  }


  object ShoppingCart extends BaseAdapter {
    val lineItems = new mutable.ArrayBuffer[(Int, Item)]()
    val TAG = "com.datayumyum.pos.ShoppingCart"
    val inflater: LayoutInflater = getLayoutInflater()

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
      var view = convertView
      if (view == null) {
        view = inflater.inflate(R.layout.row, null);
        val quantityTextView = view.findViewById(R.id.QUANTITY_CELL)
        val nameTextView = view.findViewById(R.id.DESCRIPTION_CELL)
        val priceTextView = view.findViewById(R.id.PRICE_CELL)
        val subTotalTextView = view.findViewById(R.id.SUB_TOTAL_CELL)
        view.setTag((quantityTextView, nameTextView, priceTextView, subTotalTextView))
      }

      val (quantityTextView: TextView, nameTextView: TextView, priceTextView: TextView, subTotalTextView: TextView) = view.getTag()
      val (quantity, item) = lineItems(position)
      quantityTextView.setText(quantity.toString)
      nameTextView.setText(item.name)
      priceTextView.setText(item.price.toString)
      val subTotal = quantity * item.price
      subTotalTextView.setText(subTotal.toString)
      return view
    }

    def add(item: Item) {
      val (quantity, foundItem) = lineItems.find {
        case (quantity, item1) => item == item1
      }.getOrElse((1, item))
      val i = lineItems.indexOf((quantity, foundItem))
      if (i > -1) {
        lineItems(i) = (quantity + 1, foundItem)
      } else {
        lineItems.append((quantity, foundItem))
      }
      notifyDataSetChanged()
    }

    def remove(position: Int) {
      lineItems.remove(position)
      notifyDataSetChanged()
    }
  }

}

