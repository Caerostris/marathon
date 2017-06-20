package mesosphere.marathon
package lifecycle

object CancellationPolicy {
  /**
    * The default cancelation policy to be used for migrating old RunSpecs.
    */
  val DefaultCancellationPolicy = raml.CancelationPolicy()
}