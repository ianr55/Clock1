package clock

import java.time.Instant

import scala.collection.mutable.ArrayBuffer
import java.util.NoSuchElementException

object Ticker {
  /* Uses javafx types due scalafx incomplete */
  /* Eclipse organise imports gets confused, so explicit */
  //import javafx.concurrent._
  private val oneSecond = new javafx.util.Duration(1000)
  private val service = new javafx.concurrent.ScheduledService[Instant] {
    setDelay(oneSecond)
    setPeriod(oneSecond)
    setRestartOnFailure(false) /* redundant */
    /* Default creates many new threads */
    setExecutor(java.util.concurrent.Executors.newSingleThreadScheduledExecutor)

    override def createTask() : javafx.concurrent.Task[Instant] = {
      val task = new javafx.concurrent.Task[Instant] {
        override def call() : Instant = Instant.now
      }
      task
    }
  }
  /* Wraps, not bind */
  val tick = new scalafx.beans.property.ReadOnlyObjectProperty(service.lastValueProperty)

  def start() { service.start() }
}

/* Run periodic GC.
   Defaults on 1.8.0 x64 server vm are large, and GC seems reluctant to run, so get gradual
    creep of commit and working set. Not really a memory leak.
    Rather than determine smaller vm allocations, just compact fragmentation.
 */
object PeriodicGC {
  private val oneHour = javafx.util.Duration.hours(1)
  var nextRun = Instant.now
  Ticker.tick.onChange {
    if (Instant.now.isAfter(nextRun)) {
      System.gc()
      nextRun = Instant.now.plusSeconds(oneHour.toSeconds.toLong)
    }
  }
  /* Needed to force object init and listener */
  def init() : Unit = {}
}

/* Utility classes */

class ArrayTup2[A, B](tup2s : (A, B)*) extends ArrayBuffer[(A, B)](tup2s.size) {
  this ++= tup2s
  def getAs : Seq[A] = this.map {tup => tup._1}
  def getBs : Seq[B] = this.map {tup => tup._2}
  @throws(classOf[NoSuchElementException])
  def getA(b : B) : A = this.find {tup => tup._2 == b}.get._1
  @throws(classOf[NoSuchElementException])
  def getB(a : A) : B = this.find {tup => tup._1 == a}.get._2
}

class Count(init : Int) {
  var value : Int = init
  def ++ : Int = {
    val current = value
    value += 1
    current
  }
}

