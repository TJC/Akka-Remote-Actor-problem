import sbt._

class TutorialOneProject(info: ProjectInfo)
    extends DefaultProject(info) with AkkaProject
{
      val akkaRemote = akkaModule("remote")
}
