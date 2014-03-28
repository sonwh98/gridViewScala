package com.datayumyum.pos

import scala.util.parsing.json.JSON
import scala.collection.mutable

object Catalog {
  def from(jsonStr: String): Catalog = {
    val result: Option[Any] = JSON.parseFull(jsonStr)
    val parsedMap: Map[String, List[Map[String, Any]]] = result.get.asInstanceOf[Map[String, List[Map[String, Any]]]]
    val menu: mutable.HashMap[String, List[Item]] = mutable.HashMap.empty[String, List[Item]]
    for ((category, mapList) <- parsedMap) {
      val itemList = mapList.map(m => {
        Item(m("name").asInstanceOf[String], m("imageURL").asInstanceOf[String], m("price").asInstanceOf[Double])
      })
      menu(category) = itemList
    }

    return new Catalog(menu)
  }
}

class Catalog(map: mutable.HashMap[String, List[Item]]) {
  def findItemsByCategory(name: String): List[Item] = {
    map(name)
  }

  def categories(): List[String] = {
    map.keySet.toList
  }
}

