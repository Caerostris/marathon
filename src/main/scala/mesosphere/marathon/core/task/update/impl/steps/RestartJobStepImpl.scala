package mesosphere.marathon
package core.task.update.impl.steps
//scalastyle:off
import javax.inject.Named

import akka.Done
import akka.actor.ActorRef
import com.google.inject.{ Inject, Provider }
import mesosphere.marathon.MarathonSchedulerActor.ScaleRunSpec
import mesosphere.marathon.core.condition.Condition
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import org.slf4j.LoggerFactory

import scala.concurrent.Future
//scalastyle:on
/**
  * Trigger rescale of affected app if a task died or a reserved task timed out.
  */
class RestartJobStepImpl @Inject() (
    @Named("schedulerActor") schedulerActorProvider: Provider[ActorRef]) extends InstanceChangeHandler {

  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] lazy val schedulerActor = schedulerActorProvider.get()

  private[this] def failure: Condition => Boolean = {
    case Condition.Failed | Condition.UnreachableInactive | _: Condition.Terminal => true
    case _ => false
  }

  override def name: String = "scaleApp"

  override def process(update: InstanceChange): Future[Done] = continueOnError(name, update) { update =>
    // TODO(PODS): it should be up to a tbd TaskUnreachableBehavior how to handle Unreachable
    calcScaleEvent(update).foreach(event => schedulerActor ! event)
    Future.successful(Done)
  }

  def calcScaleEvent(change: InstanceChange): Option[ScaleRunSpec] = {
    change.condition match {
      case _: Condition.Failure =>
        val runSpecId = change.runSpecId
        log.warn(s"initiating a scale check for runSpec [$runSpecId] due to failure")
        Some(ScaleRunSpec(runSpecId))
      case _ => None
    }
  }
}
