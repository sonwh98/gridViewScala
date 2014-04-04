package com.datayumyum.pos

import android.os.Bundle
import android.app.Activity
import scala.io.Source
import android.view.{ViewGroup, View, LayoutInflater}
import android.widget._
import android.animation.{ArgbEvaluator, ValueAnimator}
import android.view.animation.BounceInterpolator
import scala.collection.mutable
import android.util.Log
import java.util.Locale
import java.text.NumberFormat
import android.animation.ValueAnimator.AnimatorUpdateListener

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
      findViewById(R.id.clearButton).setOnClickListener {
        (view: View) => Accumulator.reset()
      }

      val submitOrder = (v: View) => {
        val tender = Accumulator.pop()
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
        val quantity = Accumulator.pop().asInstanceOf[Int]
        if (quantity == 0) {
          ShoppingCart.add(item)
        } else {
          ShoppingCart.add(quantity, item)
        }
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
    val lineItemViews = mutable.MutableList.empty[View]
    val TAG = "com.datayumyum.pos.ShoppingCart"
    val inflater: LayoutInflater = getLayoutInflater()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

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

    def add(quantity: Int, item: Item) {
      val (currentQuantity, foundItem) = lineItems.find {
        case (quantity1, item1) => item == item1
      }.getOrElse((0, item))
      val i = lineItems.indexOf((currentQuantity, foundItem))
      if (i > -1) {
        val updatedQuantity = currentQuantity + quantity
        lineItems(i) = (updatedQuantity, item)

        animateView(lineItemViews(i))
      } else {
        lineItems.append((quantity, item))
      }
      displayTotals()
      notifyDataSetChanged()
    }

    def displayTotals() {
      val subTotal: Double = lineItems.map {
        case (quantity, item) => {
          quantity * item.price
        }
      }.sum
      val subTotalTextView: TextView = findViewById(R.id.subTotal).asInstanceOf[TextView]
      val formattedSubTotal: String = currencyFormat.format(subTotal)
      subTotalTextView.setText(formattedSubTotal)
      val tax = 0.0
      val total = subTotal + tax
      val formattedTotal = currencyFormat.format(total)
      val totalTextView = findViewById(R.id.total).asInstanceOf[TextView]
      totalTextView.setText(formattedTotal)
    }

    def animateView(view: View) {
      if (view != null) {
        val colorFrom: java.lang.Integer = getResources().getColor(R.color.red)
        val colorTo: java.lang.Integer = getResources().getColor(R.color.background)
        val colorAnimation: ValueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo).asInstanceOf[ValueAnimator]
        colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
          override def onAnimationUpdate(animator: ValueAnimator) {
            Log.i(TAG, "view=" + view + " animator=" + animator)
            view.setBackgroundColor(animator.getAnimatedValue().asInstanceOf[Int])
          }

        })
        colorAnimation.start()
      }
    }

    def add(item: Item) {
      add(1, item)
    }

    def remove(position: Int) {
      lineItems.remove(position)
      displayTotals()
      notifyDataSetChanged()
    }
  }

}

