package mesosphere.marathon
package lifecycle

object CancellationPolicy {
  /**
    * The default cancellation policy to be used for migrating old RunSpecs.
    */
  val DefaultCancellationPolicy = raml.CancellationPolicy()

  def toProto(cancellationPolicy: raml.CancellationPolicy): Protos.CancellationPolicy =
    Protos.CancellationPolicy.newBuilder
      .setStopTryingAfterSeconds(cancellationPolicy.stopTryingAfterSeconds)
      .setStopTryingAfterNumFailures(cancellationPolicy.stopTryingAfterNumFailures)
      .build()

  def fromProto(cancellationPolicy: Protos.CancellationPolicy): raml.CancellationPolicy =
    raml.CancellationPolicy(
      stopTryingAfterNumFailures = if (cancellationPolicy.hasStopTryingAfterNumFailures) cancellationPolicy.getStopTryingAfterNumFailures else raml.CancellationPolicy.DefaultStopTryingAfterNumFailures,
      stopTryingAfterSeconds = if (cancellationPolicy.hasStopTryingAfterSeconds) cancellationPolicy.getStopTryingAfterSeconds else raml.CancellationPolicy.DefaultStopTryingAfterSeconds
    )
}