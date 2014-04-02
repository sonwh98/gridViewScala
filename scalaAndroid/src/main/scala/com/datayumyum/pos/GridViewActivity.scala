package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.view.{ViewGroup, View, LayoutInflater}
import android.widget._
import android.animation.ValueAnimator
import android.view.animation.BounceInterpolator
import scala.collection.mutable
import android.util.Log

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_grid_view)

    def configureCategories() {
      val jsonStr: String = Source.fromInputStream(getResources.openRawResource(R.raw.catalog)).mkString
      val catalog: mutable.HashMap[String, List[Item]] = Catalog.from(jsonStr)
      val gridAdapters: mutable.HashMap[String, GridAdapter] = catalog.map(entry => {
        val categoryName: String = entry._1
        val itemInCategory: List[Item] = entry._2
        (categoryName, new GridAdapter(itemInCategory))
      })

      val gridView: GridView = findViewById(R.id.gridview).asInstanceOf[GridView]
      gridView.setAdapter(gridAdapters("Entrees"))

      val categoryContainer = findViewById(R.id.categoryContainer).asInstanceOf[LinearLayout]
      catalog.keySet.foreach((catName: String) => {
        val button = new Button(GridViewActivity.this)
        button.setText(catName)
        button.setOnClickListener((v: View) => {
          gridView.setAdapter(gridAdapters(catName))
        })
        categoryContainer.addView(button)
      })
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
    def configureNumberPad() {
      Accumulator.display = findViewById(R.id.accumulatorDisplay).asInstanceOf[TextView]
      for (i <- R.id.button0 to R.id.decimalButton) {
        val button = findViewById(i).asInstanceOf[Button]
        button.setOnClickListener((v: View) => {
          Accumulator.push(button.getText().toString())
        })
      }

      val submitOrder = (v: View) => {
        val tender = Accumulator.evaluate()
        Log.i(TAG, "submitOrder cashTender: " + tender.toString)
        Accumulator.reset()
      }

      val cashButton = findViewById(R.id.cashButton)
      val creditButton = findViewById(R.id.creditButton)

      cashButton.setOnClickListener(submitOrder)
      creditButton.setOnClickListener(submitOrder)

    }
    configureCategories()
    configureLineItemView()
    configureNumberPad()
  }

  class GridAdapter(items: List[Item]) extends BaseAdapter {
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
        val quantity = Accumulator.evaluate().asInstanceOf[Int]
        if (quantity == 0) {
          ShoppingCart.add(item)
        } else {
          ShoppingCart.add(quantity, item)
        }
        Accumulator.reset()
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

    def add(quantity: Int, item: Item) {
      val (currentQuantity, foundItem) = lineItems.find {
        case (quantity1, item1) => item == item1
      }.getOrElse((0, item))
      val i = lineItems.indexOf((currentQuantity, foundItem))
      if (i > -1) {
        val updatedQuantity = currentQuantity + quantity
        lineItems(i) = (updatedQuantity, item)
      } else {
        lineItems.append((quantity, item))
      }
      notifyDataSetChanged()
    }

    def add(item: Item) {
      add(1, item)
    }

    def remove(position: Int) {
      lineItems.remove(position)
      notifyDataSetChanged()
    }
  }

}

