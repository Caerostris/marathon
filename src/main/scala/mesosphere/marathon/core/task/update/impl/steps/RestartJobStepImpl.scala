package mesosphere.marathon
package core.task.update.impl.steps
//scalastyle:off
import javax.inject.Named

import akka.Done
import akka.actor.ActorRef
import com.google.inject.{ Inject, Provider }
import mesosphere.marathon.MarathonSchedulerActor.StartInstances
import mesosphere.marathon.core.condition.Condition
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import mesosphere.marathon.core.launchqueue.LaunchQueue
import org.slf4j.LoggerFactory

import scala.concurrent.Future
//scalastyle:on
/**
  * Trigger rescale of affected app if a task died or a reserved task timed out.
  */
class RestartJobStepImpl @Inject() (
    launchQueueProvider: Provider[LaunchQueue],
    @Named("schedulerActor") schedulerActorProvider: Provider[ActorRef]) extends InstanceChangeHandler {

  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] lazy val schedulerActor = schedulerActorProvider.get()

  private[this] lazy val launchQueue = launchQueueProvider.get()

  override def name: String = "restartJob"

  override def process(update: InstanceChange): Future[Done] = continueOnError(name, update) { update =>
    calcRestartEvent(update).foreach(event => schedulerActor ! event)
    Future.successful(Done)
  }

  def calcRestartEvent(change: InstanceChange): Option[StartInstances] = {
    change.condition match {
      case _: Condition.Failure if change.instance.remainingRestarts.forall(_ > 0) =>
        val runSpecId = change.runSpecId
        log.warn(s"Initiating a restart for run spec [$runSpecId] due to failure")
        Some(StartInstances(runSpecId, change.runSpecVersion, numInstances = 1))
      case _ => None
    }
  }
}