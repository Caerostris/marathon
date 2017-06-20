package mesosphere.marathon
package core.scheduling.behavior
package impl
import akka.Done
import mesosphere.marathon.core.instance.update.InstanceChange
import mesosphere.marathon.raml.{ ContinuousSchedule, ManualSchedule }
import mesosphere.marathon.state.RunSpec
import mesosphere.marathon.storage.repository.GroupRepository
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.async.Async.{ async, await }

/**
  * Responsible for handling instance changes by delegating the decisions to
  * be made to a specific behavior based on the scheduling of the associated
  * run spec.
  */
final class InstanceChangeBehaviorImpl(continuousBehavior: InstanceChangeBehavior, manualBehavior: InstanceChangeBehavior) extends InstanceChangeBehavior {
  override def handle(change: InstanceChange, groupRepository: GroupRepository)(implicit ec: ExecutionContext): Future[Done] = async {
    val logger = LoggerFactory.getLogger(getClass)
    val runSpec = await(groupRepository.root().map(_.transitiveRunSpecsById.get(change.runSpecId)))

    logger.info(s"Routing instance change behavior for ${change.runSpecId}")

    runSpec match {
      case Some(runSpec) =>
        getHandlerForRunSpec(runSpec).handle(change, groupRepository)
        Done
      case _ =>
        logger.warn(s"Handler could not obtain run spec for ${change.runSpecId}")
        Done
    }
  }

  def getHandlerForRunSpec(runSpec: RunSpec) = {
    runSpec.lifecycle match {
      case _: ContinuousSchedule => continuousBehavior
      case _: ManualSchedule => manualBehavior
      case _ => continuousBehavior
    }
  }
}