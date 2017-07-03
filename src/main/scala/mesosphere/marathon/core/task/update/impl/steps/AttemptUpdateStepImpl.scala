package mesosphere.marathon
package core.task.update.impl.steps
//scalastyle:off

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import mesosphere.marathon.core.instance.update.{ InstanceChange, InstanceChangeHandler }
import mesosphere.marathon.storage.StorageModule
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
//scalastyle:on
/**
  * Update the a run spec's attempt whenever an instance changes
  */
class AttemptUpdateStepImpl @Inject() () extends InstanceChangeHandler {

  private[this] val log = LoggerFactory.getLogger(getClass)

  override def name: String = "attemptUpdate"

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def process(change: InstanceChange, storageModule: StorageModule)(implicit ec: ExecutionContext): Future[Done] = continueOnError(name, change) { change =>
    log.info(s"Trying to retrieve ${change.instance.attemptId} from database")
    storageModule.attemptRepository
      .all()
      .filter(_.launches.contains(change.id))
      .runForeach(attempt => {
        log.info(s"${change.instance.attemptId} retrieved.")
        attempt.registerInstanceChange(change)
        storageModule.attemptRepository.store(attempt)
      })

    // TODO (Keno): Is this right / safe / ...?
  }
}