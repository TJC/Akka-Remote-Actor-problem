package tjc.akka

import akka.actor.Actor
import Actor._
import java.util.concurrent.CountDownLatch

import tjc.akka.base._

object PiMaster extends App {
  remote.start("localhost", 2553)
  calculate(nrOfWorkers = 8, nrOfElements = 1000, nrOfMessages = 10000)


  // ==================
  // ===== Run it =====
  // ==================
  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {

    // this latch is only plumbing to know when the calculation is completed
    val latch = new CountDownLatch(1)

    // create the master
    remote.register("pi-boss",
        actorOf(
          new Master(nrOfWorkers, nrOfMessages, nrOfElements, latch)
        )
    )

    val master = remote.actorFor("pi-boss", "localhost", 2553)

    // start the calculation
    master ! Calculate

    // wait for master to shut down
    latch.await()
  }
}

