package mesosphere.marathon
package state

object SchedulerType extends Enumeration {
  val Continuous, Manual = Value
}

case class Scheduler(schedulerType: SchedulerType.Value) {
  def toProto: Protos.Scheduler = {
    val protoSchedulerType = schedulerType match {
      case SchedulerType.Continuous => Protos.Scheduler.SchedulerType.CONTINUOUS
      case SchedulerType.Manual => Protos.Scheduler.SchedulerType.MANUAL
      case _ => Protos.Scheduler.SchedulerType.CONTINUOUS
    }

    Protos.Scheduler.newBuilder().setSchedulerType(protoSchedulerType).build()
  }
}

object Scheduler {
  def default = new Scheduler(SchedulerType.Continuous)
  def fromProto(proto: Protos.Scheduler): Scheduler = {
    proto.getSchedulerType match {
      case Protos.Scheduler.SchedulerType.CONTINUOUS => Scheduler(SchedulerType.Continuous)
      case Protos.Scheduler.SchedulerType.MANUAL => Scheduler(SchedulerType.Manual)
      case _ => Scheduler(SchedulerType.Continuous)
    }
  }
}
