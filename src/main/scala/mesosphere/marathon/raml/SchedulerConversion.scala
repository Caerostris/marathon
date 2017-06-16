package mesosphere.marathon
package raml

trait SchedulerConversion {
  implicit val appSchedulerReader: Reads[raml.Scheduler, state.Scheduler] = Reads { ramlScheduler =>
    ramlScheduler.`type` match {
      case raml.SchedulerType.Continuous => state.Scheduler(state.SchedulerType.Continuous)
      case raml.SchedulerType.Manual => state.Scheduler(state.SchedulerType.Manual)
      case _ => state.Scheduler(state.SchedulerType.Continuous)
    }
  }

  implicit val appSchedulerWriter: Writes[state.Scheduler, raml.Scheduler] = Writes { stateScheduler =>
    stateScheduler.schedulerType match {
      case state.SchedulerType.Continuous => raml.Scheduler(raml.SchedulerType.Continuous)
      case state.SchedulerType.Manual => raml.Scheduler(raml.SchedulerType.Manual)
      case _ => raml.Scheduler.Default
    }
  }

  implicit val schedulerProtoRamlWriter: Writes[Protos.Scheduler, raml.Scheduler] = Writes { protoScheduler =>
    protoScheduler.getSchedulerType match {
      case Protos.Scheduler.SchedulerType.CONTINUOUS => raml.Scheduler(raml.SchedulerType.Continuous)
      case Protos.Scheduler.SchedulerType.MANUAL => raml.Scheduler(raml.SchedulerType.Manual)
      case _ => raml.Scheduler(raml.SchedulerType.Continuous)
    }
  }
}
