/*
 * Argo Bibliofilie (http://www.argo-bibliofilie.cz/) luxury books availability checker
 * Author: Vaclav Svejcar (vaclav.svejcar@gmail.com)
 * ------
 * This scripts fetches the list of all books, checks the availability and prints details.
 *
 * How to run:
 *   1/ download Ammonite (http://ammonite.io)
 *   2/ run 'amm argo.sc' in command line
 */

import $ivy.`com.lihaoyi::fansi:0.2.5`
import $ivy.`me.tongfei:progressbar:0.7.0`
import $ivy.`net.ruippeixotog::scala-scraper:2.1.0`
import fansi._
import me.tongfei.progressbar.ProgressBar
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

val Version = 1

object Url {
  val Base = "http://www.argo-bibliofilie.cz"
}

case class Book(name: String, author: String, detailUrl: String)

case class BookDetail(book: Book, price: String, available: Boolean)

val browser = JsoupBrowser()

def log(msg: Any): Unit = println("] " + msg)
def bold(text: Str): Str = text.overlay(Bold.On)
def greenB(text: Str): Str = bold(Color.Green(text))
def redB(text: Str): Str = bold(Color.Red(text))
def lightBlueB(text: Str) = bold(Color.LightBlue(text))


def fetchBookList(url: String): Seq[Book] = {
  val doc = browser.get(url)
  (doc >> elementList("#knihy .kniha")).map { elem =>
    val name = elem >> text("h2")
    val author = elem >> text("h3")
    val detailUrl = elem >> element("a.innerblock") >> attr("href")

    Book(name, author, detailUrl)
  }
}

def fetchBookDetail(book: Book): BookDetail = {
  val doc = browser.get(book.detailUrl)
  val price = doc >> text(".price-info .price")
  val available = (doc >?> element(".price-info .buy")).isDefined

  BookDetail(book, price, available)
}

log(bold(s".: Welcome to the Argo Bibliofilie book store checker v$Version :."))

log(s"Fetching list of all listed books: ${Url.Base}")
val bookList = fetchBookList(Url.Base)
log(s"Fetched ${bookList.size} book listings")

val progress = new ProgressBar("] Fetching books details", bookList.size)
val bookDetails = bookList.map { book =>
  progress.setExtraMessage(book.name)
  progress.step()
  fetchBookDetail(book)
}
progress.setExtraMessage("finished")
progress.close()

val numAvailable = bookDetails.count(_.available)
val numSold = bookDetails.size - numAvailable


println()
log(s"Total ${bookDetails.size} found (available: $numAvailable, sold out: $numSold):")
bookDetails.sortBy(detail => !detail.available).zipWithIndex.foreach { case (detail, idx) =>
  val colorFn = if (detail.available) greenB _ else redB _
  printf("  %2d", idx + 1)
  println(s"/ ${colorFn(detail.book.name)} (${detail.book.detailUrl})")
  println(s"        by ${bold(detail.book.author)}, price: ${lightBlueB("CZK " + detail.price)}")
}
