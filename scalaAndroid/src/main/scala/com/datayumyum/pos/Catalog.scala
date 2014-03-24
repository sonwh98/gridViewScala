package com.datayumyum.pos

import scala.util.parsing.json.JSON
import scala.collection.mutable.HashMap

object Catalog {
  def from(jsonStr: String): Catalog = {
    val result: Option[Any] = JSON.parseFull(jsonStr)
    val menu: HashMap[String, List[Item]] = result match {
      case Some(parsedMap: Map[String, List[Map[String, Any]]]) => {
        val menuMap = HashMap.empty[String, List[Item]]
        for ((category, mapList) <- parsedMap) {
          val itemList = mapList.map(m => {
            Item(m("name").asInstanceOf[String], m("imageURL").asInstanceOf[String], m("price").asInstanceOf[Double])
          })
          menuMap(category) = itemList
        }
        menuMap
      }
      case _ => throw new RuntimeException("cannot parse json " + jsonStr)
    }

    return new Catalog(menu)
  }
}

class Catalog(map: HashMap[String, List[Item]]) {
  def findItemsByCategory(name: String): List[Item] = {
    map(name)
  }
}

