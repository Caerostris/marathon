package mesosphere.marathon
package core.scheduling.behavior
package impl

import akka.Done
import com.typesafe.scalalogging.StrictLogging
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import mesosphere.marathon.metrics.{ Metrics, ServiceMetric, Timer }
import mesosphere.marathon.storage.StorageModule
import mesosphere.marathon.storage.repository.GroupRepository

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Performs the steps necessary when instances of continuously scheduled run specs change.
  */
private[scheduling] class StepsProcessor(steps: Seq[InstanceChangeHandler], storageModule: StorageModule) extends InstanceChangeBehavior with StrictLogging {
  logger.info(
    "Initialized StepsProcessor with steps:\n{}",
    steps.map(step => s"* ${step.name}").mkString("\n"))

  private[this] val stepTimers: Map[String, Timer] = steps.map { step =>
    step.name -> Metrics.timer(ServiceMetric, getClass, s"step-${step.name}")
  }(collection.breakOut)

  override def handle(change: InstanceChange, groupRepository: GroupRepository)(implicit ec: ExecutionContext): Future[Done] = {
    steps.foldLeft(Future.successful(Done)) { (resultSoFar, nextStep) =>
      resultSoFar.flatMap { _ =>
        stepTimers(nextStep.name) {
          logger.debug(s"Executing ${nextStep.name} for [${change.instance.instanceId}]")
          nextStep.process(change, storageModule).map { _ =>
            logger.debug(s"Done with executing ${nextStep.name} for [${change.instance.instanceId}]")
            Done
          }
        }
      }
    }
  }
}