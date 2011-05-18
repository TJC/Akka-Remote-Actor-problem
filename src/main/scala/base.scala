package tjc.akka.base
import akka.actor.{Actor, PoisonPill}
import Actor._
import akka.routing.{Routing, CyclicIterator}
import Routing._
import java.util.concurrent.CountDownLatch

// ====================
// ===== Messages =====
// ====================
trait PiMessage
case object Calculate extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage

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
// ===== Worker =====
// ==================
class Worker extends Actor {

    def calculatePiFor(start: Int, nrOfElements: Int): Double = {
        var acc = 0.0
        for (i <- start until (start + nrOfElements))
            acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
        acc
    }

    def receive = {
        case Work(start, nrOfElements) =>
            println("slave received Work message")
            val boss = remote.actorFor("pi-boss", "localhost", 2553)
            boss ! Result(calculatePiFor(start, nrOfElements))

        case _ => println("slave received unknown message!")
    }

}

