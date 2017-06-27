package mesosphere.marathon
package lifecycle

import mesosphere.marathon.raml.{ ContinuousSchedule, ManualSchedule }

object Lifecycle {
  /**
    * The default lifecycle to be used for migrating old RunSpecs.
    */
  val DefaultLifecycle = raml.ContinuousSchedule(affectsDeployment = true)

  def toProto(lifecycle: raml.LifecycleSpec): Protos.LifecycleSpec = {
    val builder = Protos.LifecycleSpec.newBuilder
      .setAffectsDeployment(lifecycle.affectsDeployment)

    lifecycle match {
      case manualSchedule: ManualSchedule =>
        val manualScheduleBuilder = Protos.ManualScheduler.newBuilder()
        manualSchedule.cancellationPolicy.foreach(
          policy => manualScheduleBuilder.setCancellationPolicy(CancellationPolicy.toProto(policy)))
        manualScheduleBuilder.build()
      case _ => builder.setContinuous(Protos.ContinuousScheduler.newBuilder().build())
    }

    builder.build()
  }

  def fromProto(lifecycle: Protos.LifecycleSpec): raml.LifecycleSpec = {
    val affectsDeployment = if (lifecycle.hasAffectsDeployment) lifecycle.getAffectsDeployment else true

    if (lifecycle.hasManual) {
      val manualSchedule = lifecycle.getManual
      val cancellationPolicy = if (manualSchedule.hasCancellationPolicy) {
        Some(CancellationPolicy.fromProto(manualSchedule.getCancellationPolicy))
      } else {
        None
      }
      ManualSchedule(affectsDeployment, cancellationPolicy)
    } else {
      ContinuousSchedule(affectsDeployment)
    }
  }
}