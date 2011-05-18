package tjc.akka

import akka.actor.Actor
import Actor._

import tjc.akka.base._

object PiSlave extends App {
    remote.start("localhost", 2552)
    remote.register("pi-service", actorOf[Worker])

}

