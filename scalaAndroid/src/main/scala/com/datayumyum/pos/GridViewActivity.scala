package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.view.{MotionEvent, ViewGroup, View, LayoutInflater}
import android.widget._
import android.animation.{ArgbEvaluator, ValueAnimator}
import scala.collection.mutable
import android.util.Log
import java.util.Locale
import java.text.NumberFormat
import android.animation.ValueAnimator.AnimatorUpdateListener
import com.android.debug.hv.ViewServer
import java.io.IOException
import android.widget

class GridViewActivity extends Activity {
  val TAG = "com.datayumyum.pos.GridViewActivity"
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_grid_view)
    ViewServer.get(this).addWindow(this)

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
      catalog.keySet.foreach((category: String) => {
        val button = new Button(GridViewActivity.this)
        button.setText(category)
        button.setOnClickListener((v: View) => {
          gridView.setAdapter(gridAdapters(category))
        })
        categoryContainer.addView(button)
      })
    }
    def configureLineItemView() {
      val listView: ListView = findViewById(R.id.lineItemListView).asInstanceOf[ListView]
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
      val buttonIdList = List(R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7,
        R.id.button8, R.id.button9, R.id.decimalButton)
      buttonIdList.foreach {
        id => {
          val button = findViewById(id).asInstanceOf[Button]
          button.setOnClickListener((v: View) => {
            Accumulator.push(button.getText().toString())
          })
        }
      }

      findViewById(R.id.clearButton).setOnClickListener {
        (view: View) => Accumulator.reset()
      }

      val submitOrder = (v: View) => {
        val tender: Double = Accumulator.pop()
        Log.i(TAG, "submitOrder cashTender: " + tender.toString)
        Accumulator.reset()
        findViewById(R.id.tender).asInstanceOf[TextView].setText(currencyFormat.format(tender))

        val change: Double = tender - ShoppingCart.calculateTotal()
        findViewById(R.id.change).asInstanceOf[TextView].setText(currencyFormat.format(change))
        ShoppingCart.checkout()
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

  override def onDestroy() {
    super.onDestroy()
    ViewServer.get(this).removeWindow(this)
  }

  override def onResume() {
    super.onResume()
    ViewServer.get(this).setFocusedWindow(this)
  }

  class GridAdapter(items: List[Item]) extends BaseAdapter {
    val itemButtonList: List[View] = items.map((item: Item) => {
      val inflater: LayoutInflater = getLayoutInflater()
      val itemButton: View = inflater.inflate(R.layout.item_button, null)
      val imageButton: ImageButton = itemButton.findViewById(R.id.item_image_button).asInstanceOf[ImageButton]
      val itemLabel: TextView = itemButton.findViewById(R.id.item_label).asInstanceOf[TextView]

      new DownloadImageTask(imageButton).execute(item.imageURL)
      val itemClickCallBack = (v: View, event: MotionEvent) => {
        val actionType = event.getAction
        Log.i(TAG, actionType.toString)
        actionType match {
          case MotionEvent.ACTION_DOWN => {
            itemButton.setAlpha(0.5f)
            val quantity = Accumulator.pop().asInstanceOf[Int]
            if (quantity == 0) {
              ShoppingCart.add(item)
            } else {
              ShoppingCart.add(item, quantity)
            }
          }
          case MotionEvent.ACTION_MOVE => {
            def isInside(): Boolean = {
              event.getX() > 0 && event.getX() < itemButton.getWidth() && event.getY() > 0 && event.getY() < itemButton.getHeight()
            }
            if (isInside()) {
              itemButton.setAlpha(0.5f)
            } else {
              itemButton.setAlpha(1.0f)
            }
          }
          case _ => itemButton.setAlpha(1.0f)
        }
        true
      }
      imageButton.setOnTouchListener(itemClickCallBack)
      itemLabel.setOnTouchListener(itemClickCallBack)

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

    override def onClick(v: View) {
      onClickCallBack(v)
    }
  }

  implicit class OnTouchListener(onTouchCallBack: (View, MotionEvent) => Boolean) extends View.OnTouchListener {
    override def onTouch(view: View, event: MotionEvent): Boolean = {
      onTouchCallBack(view, event)
    }
  }

  def thread[F](f: => F) = (new Thread(new Runnable() {
    def run() {
      f
    }
  })).start

  def uiThread[F](f: => F) = runOnUiThread(new Runnable() {
    def run() {
      f
    }
  })

  object ShoppingCart extends BaseAdapter {
    val lineItems = new mutable.ArrayBuffer[(Int, Item)]()
    val lineItemViews = mutable.MutableList.empty[View]
    val lineItemListView = findViewById(R.id.lineItemListView).asInstanceOf[ListView]
    val TAG = "com.datayumyum.pos.ShoppingCart"
    val inflater: LayoutInflater = getLayoutInflater()
    var reset: Boolean = true

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
      if (position > lineItemViews.size - 1) {
        lineItemViews += view
      } else {
        lineItemViews(position) = view
      }

      val (quantityTextView: TextView, nameTextView: TextView, priceTextView: TextView, subTotalTextView: TextView) = view.getTag()
      val (quantity, item) = lineItems(position)
      quantityTextView.setText(quantity.toString)
      nameTextView.setText(item.name)
      priceTextView.setText(currencyFormat.format(item.price))
      val subTotal = quantity * item.price
      subTotalTextView.setText(currencyFormat.format(subTotal))

      return view
    }

    def add(item: Item, quantity: Int = 1) {
      val (currentQuantity, foundItem) = lineItems.find {
        case (quantity1, item1) => item == item1
      }.getOrElse((0, item))
      val i = lineItems.indexOf((currentQuantity, foundItem))
      if (i > -1) {
        val updatedQuantity = currentQuantity + quantity
        lineItems(i) = (updatedQuantity, item)
        val visible: Boolean = i >= lineItemListView.getFirstVisiblePosition && i <= lineItemListView.getLastVisiblePosition
        if (visible) {
          animateView(lineItemViews(i))
        }
      } else {
        lineItems.append((quantity, item))
      }
      displayTotals()
      notifyDataSetChanged()
    }

    def calculateSubTotal(): Double = {
      lineItems.map {
        case (quantity, item) => {
          quantity * item.price
        }
      }.sum
    }

    def calculateTax(): Double = {
      val taxRate = 0.08
      taxRate * calculateSubTotal()
    }

    def calculateTotal(): Double = {
      calculateSubTotal + calculateTax
    }

    def displayTotals() {
      findViewById(R.id.subTotal).asInstanceOf[TextView].setText(currencyFormat.format(calculateSubTotal()))
      findViewById(R.id.tax).asInstanceOf[TextView].setText(currencyFormat.format(calculateTax()))
      findViewById(R.id.total).asInstanceOf[TextView].setText(currencyFormat.format(calculateTotal()))
    }

    def animateView(view: View) {
      if (view != null) {
        val colorFrom: java.lang.Integer = getResources().getColor(R.color.salmon)
        val colorTo: java.lang.Integer = getResources().getColor(R.color.wild_blue)
        val colorAnimation: ValueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo).asInstanceOf[ValueAnimator]
        colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
          override def onAnimationUpdate(animator: ValueAnimator) {
            view.setBackgroundColor(animator.getAnimatedValue().asInstanceOf[Int])
          }

        })
        colorAnimation.start()
      }
    }

    def remove(position: Int) {
      lineItems.remove(position)
      displayTotals()
      notifyDataSetChanged()
    }

    def checkout() {
      val store = Store("QT Sandwich", Address("48 N 10th St", "Philadelphia", "PA", "19107"), "(267)639-4520", "http://www.qtshop.com")
      thread {
        try {
          Printer.print(Receipt(store, lineItems.toList))
          uiThread {
            clear()
          }
        } catch {
          case ex: Exception => uiThread {
            widget.Toast.makeText(getApplicationContext(), f"printer not available ${ex.getMessage}", Toast.LENGTH_LONG).show()
          }
        }
      }
    }

    def clear() {
      lineItems.clear()
      lineItemViews.clear()
      findViewById(R.id.tender).asInstanceOf[TextView].setText("")
      findViewById(R.id.change).asInstanceOf[TextView].setText("")
      displayTotals()
      notifyDataSetChanged()
    }
  }

  object Accumulator {
    var value: String = ""
    val display: TextView = findViewById(R.id.accumulatorDisplay).asInstanceOf[TextView]

    def push(data: String) {
      value = value + data
      display.setText(value)
    }

    def pop(): Double = {
      try {
        if (value.length > 0) {
          return value.toDouble
        } else {
          return 0
        }
      } finally {
        reset
      }
    }

    def reset() {
      value = ""
      display.setText(value)
    }
  }

}

