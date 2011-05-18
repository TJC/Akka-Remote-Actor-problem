package tjc.akka

import akka.actor.{Actor, PoisonPill}
import Actor._
import akka.routing.{Routing, CyclicIterator}
import Routing._

import java.util.concurrent.CountDownLatch

import tjc.akka.base._

object PiMaster extends App {
  calculate(nrOfWorkers = 1, nrOfElements = 100, nrOfMessages = 1)


  // ====================
  // ===== Messages =====
  // ====================
  case object Calculate extends PiMessage

  // ==================
  // ===== Master =====
  // ==================
  class Master(
    nrOfWorkers: Int,
    nrOfMessages: Int,
    nrOfElements: Int,
    latch: CountDownLatch
  ) extends Actor {

    var pi: Double = _
    var nrOfResults: Int = _
    var start: Long = _

    // create the workers
    val workers = Vector.fill(nrOfWorkers)(
        remote.actorFor("pi-service", "localhost", 2552)
    )

    // wrap them with a load-balancing router
    val router = Routing.loadBalancerActor(CyclicIterator(workers)).start()

    // message handler
    def receive = {
      case Calculate =>
        // schedule work
        for (i <- 0 until nrOfMessages) router ! Work(i * nrOfElements, nrOfElements)

        // send a PoisonPill to all workers telling them to shut down themselves
        router ! Broadcast(PoisonPill)

        // send a PoisonPill to the router, telling him to shut himself down
        router ! PoisonPill

      case Result(value) =>
        // handle result from the worker
        pi += value
        nrOfResults += 1
        if (nrOfResults == nrOfMessages) self.stop()
    }

    override def preStart() {
      start = System.currentTimeMillis
    }

    override def postStop() {
      // tell the world that the calculation is complete
      println(
        "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis"
        .format(pi, (System.currentTimeMillis - start)))
      latch.countDown()
    }
  }

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

