package com.datayumyum.pos

case class Receipt(store: Store, lineItems: List[(Int, Item)])