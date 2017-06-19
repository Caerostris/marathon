package mesosphere.marathon
package core.scheduling.behavior
package impl
import akka.Done
import com.google.inject.Inject
import mesosphere.marathon.core.instance.update.InstanceChange
import mesosphere.marathon.state.{ AppDefinition, SchedulerType }
import mesosphere.marathon.storage.repository.GroupRepository
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

private[behavior] class RepoProvider @Inject() (val groupRepository: GroupRepository)

/**
  * Responsible for handling instance changes by delegating the decisions to
  * be made to a specific behavior based on the scheduling of the associated
  * run spec.
  */
final class InstanceChangeBehaviorImpl(continuousBehavior: InstanceChangeBehavior, manualBehavior: InstanceChangeBehavior) extends InstanceChangeBehavior {
  override def handle(change: InstanceChange, groupRepository: GroupRepository)(implicit ec: ExecutionContext): Future[Done] = {
    // this will forward the decision to a specific behavioral definition in the future
    // since the continuous behavior is currently the only available one, this is hard coded
    val logger = LoggerFactory.getLogger(getClass)
    val runSpec = groupRepository.root().map(_.transitiveRunSpecsById.get(change.runSpecId))
    runSpec match {
      case appDefinition: AppDefinition if appDefinition.scheduler.schedulerType == SchedulerType.Continuous =>
        logger.warn(s"Routing change behavior for ${change.runSpecId} via Continuous")
        continuousBehavior.handle(change, groupRepository)
      case _ =>
        logger.warn(s"Routing change behavior for ${change.runSpecId} via Manual")
        manualBehavior.handle(change, groupRepository)
    }
  }
}