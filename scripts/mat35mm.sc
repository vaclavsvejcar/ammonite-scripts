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
import $ivy.`me.tongfei:progressbar:0.7.0`
import $ivy.`net.ruippeixotog::scala-scraper:2.1.0`
import fansi._
import me.tongfei.progressbar.ProgressBar
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.annotation.tailrec
import scala.collection.mutable

object Url {
  val Base = "http://www.mat.cz"
  val Program = s"$Base/matclub/cz/kino/mesicni-program"
}

case class Screening(name: String, detailUrl: String, dates: Seq[String] = Seq.empty) {
  override def equals(obj: scala.Any) = obj match {
    case other: Screening => this.detailUrl == other.detailUrl
    case _ => false
  }

  override def hashCode() = detailUrl.hashCode
}

val browser = JsoupBrowser()

def log(msg: Any): Unit = println("] " + msg)
def fullUrl(path: String): String = Url.Base + path
def greenB(text: Str): Str = Color.Green(text).overlay(Bold.On)
def boldNum(num: Int): Str = Color.Reset(num.toString).overlay(Bold.On)

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
    val dateTimes = screenings35mm.map { screening =>
      val dateInfo = screening(0).innerHtml
      val timeInfo = screening(1).innerHtml
      dateInfo + " " + timeInfo
    }
    Some(screening.copy(dates = dateTimes))
  } else None
}


log(s"Fetching list of all future screenings: ${Url.Program}")
val screenings = fetchScreenings(Url.Program)

val pbar = new ProgressBar("] Looking for 35mm screenings", screenings.size)
val screenings35mm = screenings.flatMap { screening =>
  pbar.setExtraMessage(screening.name)
  pbar.step()
  find35mmScreenings(screening)
}
pbar.setExtraMessage("finished")
pbar.close()

println()
if (screenings35mm.nonEmpty) {
  log(s"Following ${boldNum(screenings35mm.size)} of ${boldNum(screenings.size)} upcoming screenings are in 35mm:")
  screenings35mm.zipWithIndex.foreach { case (screening, idx) =>
    printf("  %2d", idx + 1)
    println(s"/ ${greenB(screening.name)} (${screening.detailUrl})")
    screening.dates.zipWithIndex.foreach { case (date, idx2) =>
      println(s"        #${idx2 + 1}: $date")
    }
  }
} else {
  log(Color.Red("No upcoming 35mm screenings found ಥ_ಥ"))
}

