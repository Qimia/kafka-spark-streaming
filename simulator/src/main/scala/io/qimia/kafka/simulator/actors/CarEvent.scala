package io.qimia.kafka.simulator.actors

object CarEvent {

  /**
    * Does the move action.
    * Moves clock and timestamp forward, sets a new location and adjusts the expire value.
    *
    * @param loc New location.
    * @param event CarEvent.
    * @return A new CarEvent with updated values.
    */
  def move(loc: (Double, Double))(implicit event: CarEvent): CarEvent =
    apply(event.vin, loc, event.state, moveClock(event.clock), moveTimeStamp(event.timeStamp), event.expire - 1)

  /**
    * Does the transition action.
    * Moves clock and timestamp forward and sets the new state and expire values.
    *
    * @param state New state.
    * @param expire New expire value.
    * @param event CarEvent.
    * @return A new CarEvent with updated values.
    */
  def transition(state: String, expire: Int)(implicit event: CarEvent): CarEvent =
    apply(event.vin, event.location, state, moveClock(event.clock), moveTimeStamp(event.timeStamp), expire)

  /**
    * Does the block action.
    * Moves clock and timestamp forward, checks the expire value and saves the block value.
    *
    * @param block For how long block this car.
    * @param event CarEvent.
    * @return A new CarEvent with updated values.
    */
  def block(block: Int)(implicit event: CarEvent): CarEvent =
    apply(
      event.vin,
      event.location,
      event.state,
      moveClock(event.clock),
      moveTimeStamp(event.timeStamp),
      if (event.expire > 0) event.expire - 1 else 0,
      block
    )

  /**
    * Does the click action.
    * Moves clock and timestamp forward and checks the expire value.
    *
    * @param event CarEvent.
    * @return A new CarEvent with updated values.
    */
  def clock(implicit event: CarEvent): CarEvent =
    apply(
      event.vin,
      event.location,
      event.state,
      moveClock(event.clock),
      moveTimeStamp(event.timeStamp),
      if (event.expire > 0) event.expire - 1 else 0
    )

  /**
    * Moves clock one "tick" forward.
    *
    * @param clock The original clock value.
    * @return Clock + 1.
    */
  private def moveClock(clock: Int): Int = clock + 1

  /**
    * Moves timestamp one minute forward.
    *
    * @param ts Original timestamp.
    * @return The timestamp + 60 seconds.
    */
  private def moveTimeStamp(ts: Long): Long = ts + 60000L
}

case class CarEvent(
    vin: String,
    location: (Double, Double),
    state: String,
    clock: Int,
    timeStamp: Long,
    expire: Int,
    block: Int = 0
)
