package com.flurdy.wishlist

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.select._

/**
  *
  * Simple Scala wrapper for Jsoup
  *
  **/
case class SoupElements(underlying: Elements) {

   def headOption: Option[Element] = if(isEmpty) None else Some(first())

   def first() = underlying.first()

   def isEmpty() = underlying.isEmpty()

}

case class SoupDocument(underlying: Document) {

   def head: Element = underlying.head()

   def select(selector: String) = SoupElements(underlying.select(selector))

}

object ScalaSoup {

   def parse(html: String) = SoupDocument(Jsoup.parse(html))

}
