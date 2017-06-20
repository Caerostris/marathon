package mesosphere.marathon
package lifecycle

object Lifecycle {
  /**
    * The default lifecycle to be used for migrating old RunSpecs.
    */
  val DefaultLifecycle = raml.LifecycleSpec(
    schedule = raml.Schedule(
      strategy = raml.ContinuousSchedulingStrategy(affectsDeployment = true)
    )
  )
}