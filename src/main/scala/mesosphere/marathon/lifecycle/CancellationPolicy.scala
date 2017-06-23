package mesosphere.marathon
package lifecycle

object CancellationPolicy {
  /**
    * The default cancellation policy to be used for migrating old RunSpecs.
    */
  val DefaultCancellationPolicy = raml.CancellationPolicy()

  def toProto(cancellationPolicy: raml.CancellationPolicy): Protos.CancellationPolicy = {
    val protos = Protos.CancellationPolicy.newBuilder
      .setMaxDurationPerInstanceSeconds(cancellationPolicy.maxDurationPerInstanceSeconds)
      .setStopTryingAfterSeconds(cancellationPolicy.stopTryingAfterSeconds)

    cancellationPolicy.stopTryingAfterNumFailures.map(protos.setStopTryingAfterNumFailures).getOrElse(protos).build()
  }

  def fromProto(cancellationPolicy: Protos.CancellationPolicy): raml.CancellationPolicy = {
    raml.CancellationPolicy(
      maxDurationPerInstanceSeconds = if (cancellationPolicy.hasMaxDurationPerInstanceSeconds) cancellationPolicy.getMaxDurationPerInstanceSeconds else raml.CancellationPolicy.DefaultMaxDurationPerInstanceSeconds,
      stopTryingAfterNumFailures = if (cancellationPolicy.hasStopTryingAfterNumFailures) Some(cancellationPolicy.getStopTryingAfterNumFailures) else None,
      stopTryingAfterSeconds = if (cancellationPolicy.hasStopTryingAfterSeconds) cancellationPolicy.getStopTryingAfterSeconds else raml.CancellationPolicy.DefaultStopTryingAfterSeconds
    )
  }
}