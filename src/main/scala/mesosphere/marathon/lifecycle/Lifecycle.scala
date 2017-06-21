package mesosphere.marathon
package lifecycle

import mesosphere.marathon.raml.{ ContinuousSchedule, ManualSchedule, PeriodicSchedule }

object Lifecycle {
  /**
    * The default lifecycle to be used for migrating old RunSpecs.
    */
  val DefaultLifecycle = raml.ContinuousSchedule(CancellationPolicy.DefaultCancellationPolicy)

  def toProto(lifecycle: raml.LifecycleSpec): Protos.LifecycleSpec = {
    var builder = Protos.LifecycleSpec.newBuilder
      .setCancellationPolicy(CancellationPolicy.toProto(lifecycle.cancellationPolicy))

    builder = lifecycle match {
      case _: ContinuousSchedule => builder.setContinuous(Protos.ContinuousScheduler.newBuilder().build())
      case _: ManualSchedule => builder.setManual(Protos.ManualScheduler.newBuilder().build())
      case _ => builder.setContinuous(Protos.ContinuousScheduler.newBuilder().build())
    }

    builder.build()
  }

  def fromProto(lifecycle: Protos.LifecycleSpec): raml.LifecycleSpec = {
    val cancellationPolicy = if (lifecycle.hasCancellationPolicy) {
      CancellationPolicy.fromProto(lifecycle.getCancellationPolicy)
    } else {
      CancellationPolicy.DefaultCancellationPolicy
    }

    if (lifecycle.hasContinuous) {
      ContinuousSchedule(cancellationPolicy)
    } else if (lifecycle.hasManual) {
      ManualSchedule(cancellationPolicy)
    } else if (lifecycle.hasPeriodic) {
      PeriodicSchedule(cancellationPolicy)
    } else {
      ContinuousSchedule(cancellationPolicy)
    }
  }
}