/*
 * Timesheet generator
 * Author: Vaclav Svejcar (vaclav.svejcar@gmail.com)
 * ------
 * Generates daily timesheet for the given number of mandays and maximum deviation from default
 * 8h/MD working day.
 *
 * How to run:
 *   1/ download Ammonite (http://ammonite.io)
 *   2/ run 'amm timesheet.sc' in command line
 */

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Random

val Version = 1

object Defaults {
  val LowerBound = 5.8
  val UpperBound = 10.2
  val MaxDeviation = 2.2
  val HoursPerMd = 8
  val Precision = 1
}

def random(lowerBound: Double, upperBound: Double): Double =
  lowerBound + Math.random() * (upperBound - lowerBound)


def round(number: Double): Double =
  BigDecimal(number).setScale(Defaults.Precision, BigDecimal.RoundingMode.HALF_UP).toDouble

def log(text: String): Unit = println(text)

def input(prompt: String, default: Option[String] = None): String = {
  val defaultText = default.map(d => s" (or ENTER for default '$d')").getOrElse("")
  val result = StdIn.readLine(prompt + defaultText + " > ")
  default.map(d => if (result.isEmpty) d else result).getOrElse(result)
}

def generateNumbers(count: Int, lowerBound: Double,
                    upperBound: Double, sum: Double): Seq[Double] = {

  @tailrec def impl(aggr: Seq[Double], nLowerBound: Double,
                    nUpperBound: Double, nSum: Double): Seq[Double] = {
    val rest = count - (aggr.size + 1)
    val restLowerBound = nLowerBound * rest
    val restUpperBound = nUpperBound * rest
    val nextLowerBound = Math.max(nLowerBound, nSum - restUpperBound)
    val nextUpperBound = Math.min(nUpperBound, nSum - restLowerBound)
    val next = round(random(nextLowerBound, nextUpperBound))

    if (aggr.size == count) {
      aggr
    } else {
      impl(aggr :+ next, nextLowerBound, nextUpperBound, nSum - next)
    }
  }

  Random.shuffle(impl(Seq.empty, lowerBound, upperBound, sum))
}

log(s"Welcome to the Timesheet generator v$Version")
val count = input("Please enter total number of working days").toInt
val deviation = input(
  s"Please enter max deviation from default hours per MD (${Defaults.HoursPerMd})",
  Some(Defaults.MaxDeviation.toString)).toDouble
val sum = count * Defaults.HoursPerMd

val lowerBound = Defaults.HoursPerMd - deviation
val upperBound = Defaults.HoursPerMd + deviation
val results = generateNumbers(count, lowerBound, upperBound, sum)
val resultsSum = round(results.sum)
assert(resultsSum == sum, s"Resulting sum '$resultsSum' did not equal to '$sum'")

log("Generated results are below:")
log("---------")
results foreach println
log("---------")
log("TOTAL SUM: " + resultsSum)
log("We wish you satisfying invoice :-)")
