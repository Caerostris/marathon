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
import mesosphere.marathon.storage.StorageModule
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
//scalastyle:on
/**
  * Trigger rescale of affected app if a task died or a reserved task timed out.
  */
class RestartJobStepImpl @Inject() (
    @Named("schedulerActor") schedulerActorProvider: Provider[ActorRef]) extends InstanceChangeHandler {

  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] lazy val schedulerActor = schedulerActorProvider.get()

  override def name: String = "restartJob"

  override def process(change: InstanceChange, storageModule: StorageModule)(implicit exc: ExecutionContext): Future[Done] = continueOnError(name, change) { change =>
    if (change.condition == Condition.Failed) {
      storageModule.attemptRepository.get(change.instance.attemptId).map(maybeAttempt => {
        val mayRestart = maybeAttempt.forall(attempt => attempt.permitsRestart())
        if (mayRestart) {
          log.info(s"Restarting job after failure ${change.runSpecId}")
          schedulerActor ! StartInstances(change.runSpecId, change.runSpecVersion, 1)
        } else {
          log.warn(s"Job ${change.runSpecId} failures exceeded cancellation policy. Not restarting.")
        }
        Done
      })
    } else {
      Future.successful(Done)
    }
  }
}