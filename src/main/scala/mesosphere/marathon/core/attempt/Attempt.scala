package mesosphere.marathon
package core.attempt

import com.fasterxml.uuid.{ EthernetAddress, Generators }
import mesosphere.marathon.core.attempt.Attempt.Launch
import mesosphere.marathon.core.condition.Condition
import mesosphere.marathon.core.instance.Instance
import mesosphere.marathon.raml.CancellationPolicy
import mesosphere.marathon.state.PathId
import org.apache.mesos.{ Protos => MesosProtos }
import org.slf4j.LoggerFactory

import scala.collection.immutable.ListMap
import scala.util.matching.Regex

case class Attempt(attemptId: Attempt.Id, cancellationPolicy: CancellationPolicy) {
  var remainingRetries: Option[Int] = cancellationPolicy.stopTryingAfterNumFailures.map(_ + 1)
  var launches: ListMap[Instance.Id, Launch] = ListMap.empty

  private[this] val log = LoggerFactory.getLogger(getClass.getName)

  def permitsRestart(): Boolean = remainingRetries.forall(_ > 0)

  def setInstance(instanceId: Instance.Id, condition: Condition) = {
    launches += (instanceId -> Launch(instanceId, condition))
  }

  def registerResult(instanceId: Instance.Id, condition: Condition) = {
    if (!launches.contains(instanceId)) {
      log.warn(s"Registered result for non-existent instance id $instanceId in attempt $attemptId")
    }
    setInstance(instanceId, condition)

    if (!finished) {
      remainingRetries = remainingRetries.map(_ - 1)
    }
  }

  def finished = launches.lastOption.exists(entry => !isFailure(entry._2.last_condition))

  private[this] def isFailure(condition: Condition): Boolean = {
    condition match {
      case _: Condition.Failure => true
      case _ => false
    }
  }
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
    private[this] val AttemptIdRegex: Regex = """^(.+)\.attempt-([^\.]+)$""".r

    def forRunSpec(id: PathId): Id = Attempt.Id(id.safePath + ".attempt-" + uuidGenerator.generate())

    def runSpecId(attemptId: String): PathId =
      attemptId match {
        case AttemptIdRegex(runSpecId, _, _) => PathId.fromSafePath(runSpecId)
        case _ => throw new MatchError("unable to extract runSpecId from attemptId " + attemptId)
      }
  }
}