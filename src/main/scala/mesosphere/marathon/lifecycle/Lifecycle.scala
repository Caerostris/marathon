package mesosphere.marathon
package lifecycle

object Lifecycle {
  /**
    * The default lifecycle to be used for migrating old RunSpecs.
    */
  val DefaultLifecycle = raml.ContinuousSchedule(CancellationPolicy.DefaultCancellationPolicy)
}