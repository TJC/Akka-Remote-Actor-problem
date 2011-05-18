package tjc.akka

import akka.actor.Actor
import Actor._
import java.util.concurrent.CountDownLatch

import tjc.akka.base._

object PiMaster extends App {
  calculate(nrOfWorkers = 1, nrOfElements = 100, nrOfMessages = 1)


  // ==================
  // ===== Run it =====
  // ==================
  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {

    // this latch is only plumbing to know when the calculation is completed
    val latch = new CountDownLatch(1)

    // create the master
    val master = actorOf(
      new Master(nrOfWorkers, nrOfMessages, nrOfElements, latch)
    ).start()

    // start the calculation
    master ! Calculate

    // wait for master to shut down
    latch.await()
  }
}

