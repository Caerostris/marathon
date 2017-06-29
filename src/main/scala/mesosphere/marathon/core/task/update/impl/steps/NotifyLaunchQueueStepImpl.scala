package mesosphere.marathon
package core.task.update.impl.steps

import javax.inject.Inject

import akka.Done
import com.google.inject.Provider
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import mesosphere.marathon.core.launchqueue.LaunchQueue
import mesosphere.marathon.storage.StorageModule

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Notify the launch queue of this update.
  */
class NotifyLaunchQueueStepImpl @Inject() (launchQueueProvider: Provider[LaunchQueue]) extends InstanceChangeHandler {

  override def name: String = "notifyLaunchQueue"

  private[this] lazy val launchQueue = launchQueueProvider.get()

  override def process(update: InstanceChange, storageModule: StorageModule)(implicit ec: ExecutionContext): Future[Done] = continueOnError(name, update) { update =>
    launchQueue.notifyOfInstanceUpdate(update)
  }
}
