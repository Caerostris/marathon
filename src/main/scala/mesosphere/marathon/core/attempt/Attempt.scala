package mesosphere.marathon
package core.attempt

import com.fasterxml.uuid.{ EthernetAddress, Generators }
import mesosphere.marathon.core.attempt.Attempt.Launch
import mesosphere.marathon.core.condition.Condition
import mesosphere.marathon.core.instance.Instance
import mesosphere.marathon.core.instance.update.InstanceChange
import mesosphere.marathon.raml.CancellationPolicy
import mesosphere.marathon.state.PathId
import org.apache.mesos.{ Protos => MesosProtos }
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.immutable.ListMap
import scala.util.matching.Regex

case class Attempt(attemptId: Attempt.Id, cancellationPolicy: CancellationPolicy) {
  var remainingRetries: Int = cancellationPolicy.stopTryingAfterNumFailures + 1
  var totalTimeSeconds: Option[Int] = if (cancellationPolicy.stopTryingAfterSeconds > 0) Some(cancellationPolicy.stopTryingAfterSeconds) else None

  var launches: ListMap[Instance.Id, Launch] = ListMap.empty

  private[this] val log = LoggerFactory.getLogger(getClass.getName)

  def permitsRestart(): Boolean = remainingRetries > 0

  def registerInstanceChange(change: InstanceChange) = {
    val instanceId = change.instance.instanceId
    if (!launches.contains(instanceId)) {
      log.warn(s"Registered result for non-existent instance id $instanceId in attempt $attemptId")
    }

    launches += (instanceId -> Launch(instanceId, change.condition))

    if (change.condition == Condition.Failed) {
      remainingRetries -= 1
    }
  }

  def finished = !permitsRestart() || launches.lastOption.exists(_._2.last_condition == Condition.Finished)
}

object Attempt {
  private val uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface())

  case class Id(idString: String) extends Ordered[Id] {
    val runSpecId: PathId = Attempt.Id.runSpecId(idString)
    lazy val mesosTaskId: MesosProtos.TaskID = MesosProtos.TaskID.newBuilder().setValue(idString).build()
    override def toString: String = s"task [$idString]"
    override def compare(that: Id): Int = idString.compare(that.idString)
  }

  case class Launch(instanceId: Instance.Id, last_condition: Condition)

  object Id {
    // private[this] val AttemptIdRegex: Regex = """^(.+)\.attempt-([^\.]+)$""".r
    private[this] val AttemptIdRegex: Regex = """^(.+)\.attempt$""".r

    def forRunSpec(id: PathId): Id = Attempt.Id(id.safePath + ".attempt")

    def runSpecId(attemptId: String): PathId =
      attemptId match {
        case AttemptIdRegex(runSpecId) => PathId.fromSafePath(runSpecId)
        case _ => throw new MatchError("unable to extract runSpecId from attemptId " + attemptId)
      }

    implicit val attemptIdFormat = Format(
      Reads.of[String](Reads.minLength[String](3)).map(Attempt.Id(_)),
      Writes[Attempt.Id] { id => JsString(id.idString) }
    )
  }

  implicit val attemptJsonWrites: Writes[Attempt] = {
    (
      (__ \ "attemptId").write[Attempt.Id] ~
      (__ \ "cancellationPolicy").write[CancellationPolicy]
    ) { (a) =>
        (
          a.attemptId,
          a.cancellationPolicy
        )
      }
  }

  implicit val attemptJsonReads: Reads[Attempt] = {
    (
      (__ \ "attemptId").read[Attempt.Id] ~
      (__ \ "cancellationPolicy").read[CancellationPolicy]
    ) { (attemptId, cancellationPolicy) => new Attempt(attemptId, cancellationPolicy) }
  }
}