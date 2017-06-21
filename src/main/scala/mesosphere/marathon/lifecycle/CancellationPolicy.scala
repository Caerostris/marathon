package mesosphere.marathon
package lifecycle

object CancellationPolicy {
  /**
    * The default cancellation policy to be used for migrating old RunSpecs.
    */
  val DefaultCancellationPolicy = raml.CancellationPolicy()

  def toProto(cancellationPolicy: raml.CancellationPolicy): Protos.CancellationPolicy = {
    Protos.CancellationPolicy.newBuilder
      .setMaxDurationPerInstanceSeconds(cancellationPolicy.maxDurationPerInstanceSeconds)
      .setStopTryingAfterNumFailures(cancellationPolicy.stopTryingAfterNumFailures)
      .setStopTryingAfterSeconds(cancellationPolicy.stopTryingAfterSeconds)
      .build()
  }

  def fromProto(cancellationPolicy: Protos.CancellationPolicy): raml.CancellationPolicy = {
    raml.CancellationPolicy(
      maxDurationPerInstanceSeconds = if (cancellationPolicy.hasMaxDurationPerInstanceSeconds) cancellationPolicy.getMaxDurationPerInstanceSeconds else raml.CancellationPolicy.DefaultMaxDurationPerInstanceSeconds,
      stopTryingAfterNumFailures = if (cancellationPolicy.hasStopTryingAfterNumFailures) cancellationPolicy.getStopTryingAfterNumFailures else raml.CancellationPolicy.DefaultStopTryingAfterNumFailures,
      stopTryingAfterSeconds = if (cancellationPolicy.hasStopTryingAfterSeconds) cancellationPolicy.getStopTryingAfterSeconds else raml.CancellationPolicy.DefaultStopTryingAfterSeconds
    )
  }
}