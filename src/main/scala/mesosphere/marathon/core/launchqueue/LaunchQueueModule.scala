package mesosphere.marathon
package core.launchqueue

import akka.actor.{ ActorRef, Props }
import mesosphere.marathon.core.attempt.Attempt
import mesosphere.marathon.core.base.Clock
import mesosphere.marathon.core.flow.OfferReviver
import mesosphere.marathon.core.launcher.InstanceOpFactory
import mesosphere.marathon.core.launchqueue.impl._
import mesosphere.marathon.core.leadership.LeadershipModule
import mesosphere.marathon.core.matcher.manager.OfferMatcherManager
import mesosphere.marathon.core.task.tracker.InstanceTracker
import mesosphere.marathon.state.RunSpec
import mesosphere.marathon.storage.repository.AttemptRepository

/**
  * Provides a [[LaunchQueue]] implementation which can be used to launch tasks for a given RunSpec.
  */
class LaunchQueueModule(
    config: LaunchQueueConfig,
    leadershipModule: LeadershipModule,
    clock: Clock,
    subOfferMatcherManager: OfferMatcherManager,
    maybeOfferReviver: Option[OfferReviver],
    taskTracker: InstanceTracker,
    taskOpFactory: InstanceOpFactory,
    attemptRepository: AttemptRepository) {

  private[this] val offerMatchStatisticsActor: ActorRef = {
    leadershipModule.startWhenLeader(OfferMatchStatisticsActor.props(), "offerMatcherStatistics")
  }

  private[this] val launchQueueActorRef: ActorRef = {
    def runSpecActorProps(runSpec: RunSpec, count: Int, attempt: Option[Attempt]): Props =
      TaskLauncherActor.props(
        config,
        subOfferMatcherManager,
        clock,
        taskOpFactory,
        maybeOfferReviver,
        taskTracker,
        rateLimiterActor,
        offerMatchStatisticsActor,
        attemptRepository)(runSpec, count, attempt)
    val props = LaunchQueueActor.props(config, offerMatchStatisticsActor, runSpecActorProps)
    leadershipModule.startWhenLeader(props, "launchQueue")
  }

  val rateLimiter: RateLimiter = new RateLimiter(config, clock)
  private[this] val rateLimiterActor: ActorRef = {
    val props = RateLimiterActor.props(
      rateLimiter, launchQueueActorRef)
    leadershipModule.startWhenLeader(props, "rateLimiter")
  }
  val launchQueue: LaunchQueue = new LaunchQueueDelegate(config, launchQueueActorRef, rateLimiterActor)
}
