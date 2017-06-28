package mesosphere.marathon
package core.task.update.impl.steps
//scalastyle:off

import akka.Done
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import mesosphere.marathon.storage.repository.AttemptRepository
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
//scalastyle:on
/**
  * Trigger rescale of affected app if a task died or a reserved task timed out.
  */
class AttemptUpdateStepImpl(attemptRepository: AttemptRepository)(implicit exc: ExecutionContext) extends InstanceChangeHandler {

  private[this] val log = LoggerFactory.getLogger(getClass)

  override def name: String = "attemptUpdate"

  override def process(change: InstanceChange): Future[Done] = continueOnError(name, change) { change =>
    log.info(s"Trying to retrieve attempt ${change.instance.attemptId} from database")
    attemptRepository.get(change.instance.attemptId).map(maybeAttempt =>
      maybeAttempt.map(attempt => {
        log.info(s"Attempt ${change.instance.attemptId} retrieved.")
        attempt.registerInstanceChange(change)
        attemptRepository.store(attempt)
      })
    ).map(_ => Done)
  }
}