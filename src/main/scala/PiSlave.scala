package tjc.akka

import akka.actor.Actor
import Actor._

import tjc.akka.base._

object PiSlave extends App {
    remote.start("localhost", 2552)
    remote.register("pi-service", actorOf[Worker])


    // ==================
    // ===== Worker =====
    // ==================
    class Worker extends Actor {

        // define the work
        def calculatePiFor(start: Int, nrOfElements: Int): Double = {
            var acc = 0.0
            for (i <- start until (start + nrOfElements))
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
            acc
        }

        def receive = {
            case Work(start, nrOfElements) =>
                println("slave received Work message")
                self.reply(
                    Result(calculatePiFor(start, nrOfElements))
                )
            case _ => println("slave received unknown message!")
        }
    }

}

