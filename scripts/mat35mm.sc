/*
 * MAT cinema (http://www.mat.cz) 35mm screening checker
 * Author: Vaclav Svejcar (vaclav.svejcar@gmail.com)
 * ------
 * This script fetches all future cinema screenings and checks if there's any upcoming 35mm
 * film screening.
 *
 * How to run:
 *   1/ download Ammonite (http://ammonite.io)
 *   2/ run 'amm mat35mm.sc' in command line
 */

import $ivy.`com.lihaoyi::fansi:0.2.5`
import $ivy.`me.tongfei:progressbar:0.7.1`
import $ivy.`net.ruippeixotog::scala-scraper:2.1.0`
import fansi._
import me.tongfei.progressbar.ProgressBar
import net.ruippeixotog.scalascraper.browser.{HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.annotation.tailrec
import scala.collection.mutable

val Version = 2

object Url {
  val Base = "http://www.mat.cz"
  val Program = s"$Base/matclub/cz/kino/mesicni-program"
}

case class Screening(name: String, detailUrl: String, details: Seq[ScreeningDetails] = Seq.empty) {
  override def equals(obj: scala.Any) = obj match {
    case other: Screening => this.detailUrl == other.detailUrl
    case _ => false
  }

  override def hashCode() = detailUrl.hashCode
}

case class Seats(available: Int, total: Int)

case class ScreeningDetails(dateTime: String, seats: Seats)

val browser = JsoupBrowser()
val htmlUnitBrowser = new HtmlUnitBrowser()

def log(msg: Any): Unit = println("] " + msg)
def fullUrl(path: String): String = Url.Base + path
def greenB(text: Str): Str = bold(Color.Green(text))
def bold(text: Str): Str = text.overlay(Bold.On)
def bold(num: Int): Str = bold(num.toString)

def fetchScreenings(programUrl: String): Seq[Screening] = {
  @tailrec def impl(url: String, aggr: Seq[Screening]): Seq[Screening] = {
    val doc = browser.get(url)
    val screenings: Seq[Screening] = (doc >> elementList(".films .film2 a"))
      .map(link => Screening(link.innerHtml, fullUrl(link >> attr("href"))))
    val nextUrlOpt = if (screenings.nonEmpty)
      Some(fullUrl(doc >> element(".kinonext") >> attr("href"))) else None

    nextUrlOpt match {
      case None => aggr ++ screenings
      case Some(nextUrl) => impl(nextUrl, aggr ++ screenings)
    }
  }

  // recursively iterate through all future screenings and remove duplicates
  mutable.LinkedHashSet(impl(programUrl, Seq.empty): _*).toList
}


def find35mmScreenings(screening: Screening): Option[Screening] = {
  val doc = browser.get(screening.detailUrl)
  val screenings35mm = (doc >> table(".hrajemetab")).filter(_ (3).innerHtml.contains("35mm"))

  if (screenings35mm.nonEmpty) {
    val screeningDetails = screenings35mm.map { screeningTable =>
      val dateInfo = screeningTable(0).innerHtml
      val timeInfo = screeningTable(1).innerHtml
      val seatsReservationUrl = screeningTable(4) >> element("a.disdata") >> attr("href")
      val seats = seatsInfo(seatsReservationUrl)

      ScreeningDetails(dateInfo + " " + timeInfo, seats)
    }
    Some(screening.copy(details = screeningDetails))
  } else None
}

def seatsInfo(seatsReservationUrl: String): Seats = {
  val doc = htmlUnitBrowser.get(seatsReservationUrl)
  val allSeats = doc >> elementList("#plocha .sedacka")
  val availableSeats = allSeats.filter(element => (element >> attr("src")).contains("boxgray"))

  Seats(availableSeats.size, allSeats.size)
}


log(bold(s".: Welcome to the Kino MAT 35mm screening checker v$Version :."))
log(s"Fetching list of all upcoming screenings from: ${Url.Program}")
val screenings = fetchScreenings(Url.Program)
log(s"Fetched ${bold(screenings.size)} screenings to check")

val progress = new ProgressBar("] Looking for 35mm screenings", screenings.size)
val screenings35mm = screenings.flatMap { screening =>
  progress.setExtraMessage(screening.name)
  progress.step()
  find35mmScreenings(screening)
}
progress.setExtraMessage("finished")
progress.close()

println()
if (screenings35mm.nonEmpty) {
  log(s"Following ${bold(screenings35mm.size)} of ${bold(screenings.size)} upcoming screenings are in 35mm:")
  screenings35mm.zipWithIndex.foreach { case (screening, idx) =>
    printf("  %2d", idx + 1)
    println(s"/ ${greenB(screening.name)} (${screening.detailUrl})")
    screening.details.zipWithIndex.foreach { case (detail, idx2) =>
      print(s"        #${idx2 + 1}: ${detail.dateTime}")
      print(s" (${bold(detail.seats.available)} of ${bold(detail.seats.total)} seats available)")
      println()
    }
  }
} else {
  log(Color.Red(s"None of upcoming ${screenings.size} screenings is in 35mm ಥ_ಥ"))
}

