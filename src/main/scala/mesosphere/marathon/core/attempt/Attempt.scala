package mesosphere.marathon
package core.attempt

import com.fasterxml.uuid.{ EthernetAddress, Generators }
import mesosphere.marathon.core.condition.Condition
import mesosphere.marathon.core.instance.Instance
import mesosphere.marathon.core.instance.update.InstanceChange
import mesosphere.marathon.raml.CancellationPolicy
import mesosphere.marathon.state.PathId
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.mutable
import scala.util.matching.Regex

case class Attempt(
    attemptId: Attempt.Id,
    cancellationPolicy: CancellationPolicy,
    launches: mutable.ListMap[Instance.Id, Option[Condition]] = mutable.ListMap.empty) {
  var totalTimeSeconds: Int = cancellationPolicy.stopTryingAfterSeconds

  private[this] val log = LoggerFactory.getLogger(getClass.getName)

  def remainingRetries: Int = cancellationPolicy.stopTryingAfterNumFailures - launches.size

  def permitsRestart(): Boolean = remainingRetries > 0

  def registerInstanceChange(change: InstanceChange) = {
    val instanceId = change.instance.instanceId
    if (!launches.contains(instanceId)) {
      log.warn(s"Registered result for non-existent instance id $instanceId in attempt $attemptId")
    }

    launches += instanceId -> Some(change.condition)
  }

  def registerNewInstance(instanceId: Instance.Id) = {
    log.info(s"Registered new instance for $attemptId: $instanceId")
    launches += instanceId -> None
  }

  def finished = !permitsRestart() || launches.lastOption.exists(_._2.contains(Condition.Finished))
}

private[this] case class InstanceConditionPair(id: Instance.Id, condition: Option[Condition])

private[this] object InstanceConditionPair {
  implicit val instanceConditionPairWrites: Writes[InstanceConditionPair] = Json.writes[InstanceConditionPair]
  implicit val instanceConditionPairReads: Reads[InstanceConditionPair] = Json.reads[InstanceConditionPair]
}

object Attempt {
  private val uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface())

  case class Id(idString: String) extends Ordered[Id] {
    val runSpecId: PathId = Attempt.Id.runSpecId(idString)
    override def toString: String = s"attempt [$idString]"
    override def compare(that: Id): Int = idString.compare(that.idString)
  }

  object Id {
    // private[this] val AttemptIdRegex: Regex = """^(.+)\.attempt-([^\.]+)$""".r
    private[this] val AttemptIdRegex: Regex = """^(.+)\.attempt-([^\._]+)$""".r

    def forRunSpec(id: PathId): Id = Attempt.Id(id.safePath + ".attempt-" + uuidGenerator.generate())

    def runSpecId(attemptId: String): PathId =
      attemptId match {
        case AttemptIdRegex(runSpecId, _) => PathId.fromSafePath(runSpecId)
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
      (__ \ "cancellationPolicy").write[CancellationPolicy] ~
      (__ \ "launches").write[List[InstanceConditionPair]]
    ) { (a) =>
        (
          a.attemptId,
          a.cancellationPolicy,
          a.launches.toList.map { case (id, condition) => InstanceConditionPair(id, condition) }
        )
      }
  }

  implicit val attemptJsonReads: Reads[Attempt] = {
    (
      (__ \ "attemptId").read[Attempt.Id] ~
      (__ \ "cancellationPolicy").read[CancellationPolicy] ~
      (__ \ "launches").read[List[InstanceConditionPair]]
    ) { (attemptId, cancellationPolicy, launches) => new Attempt(attemptId, cancellationPolicy, mutable.ListMap(launches.map { case InstanceConditionPair(id, condition) => (id, condition) }: _*)) }
  }
}