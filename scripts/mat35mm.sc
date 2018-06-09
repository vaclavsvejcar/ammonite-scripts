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

import $ivy.`me.tongfei:progressbar:0.7.0`
import $ivy.`net.ruippeixotog::scala-scraper:2.1.0`
import me.tongfei.progressbar.ProgressBar
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.annotation.tailrec

object Url {
  val Base = "http://www.mat.cz"
  val Program = s"$Base/matclub/cz/kino/mesicni-program"
}

case class Screening(name: String, detailUrl: String)

val browser = JsoupBrowser()

def log(msg: Any): Unit = println("] " + msg)
def fullUrl(path: String): String = Url.Base + path

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
  impl(programUrl, Seq.empty).groupBy(_.detailUrl).map(_._2.head)(collection.breakOut)
}

def checkIf35mm(screening: Screening): Boolean = {
  val doc = browser.get(screening.detailUrl)
  val format = doc >> text(".hrajemetab > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(4)")

  format.contains("35mm")
}


log(s"Fetching list of all future screenings: ${Url.Program}")
val screenings = fetchScreenings(Url.Program)

val pbar = new ProgressBar("] Checking 35mm screenings", screenings.size)
val screenings35mm = screenings.filter { screening =>
  pbar.setExtraMessage(s"Checking movie: ${screening.name}")
  pbar.step()
  checkIf35mm(screening)
}
pbar.setExtraMessage("finished")
pbar.close()

println()
log(s"Following ${screenings35mm.size} of ${screenings.size} upcoming screenings are in 35mm:")
screenings35mm.zipWithIndex.foreach { case (screening, idx) =>
  printf("%2d", idx + 1)
  println(s"/ ${screening.name} (${screening.detailUrl})")
}
