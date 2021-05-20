package pods.cabs;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import pods.cabs.RideService.Command;
import pods.cabs.utils.CborSerializable;
import pods.cabs.utils.Logger;
import pods.cabs.values.CabStates;

public class Cab extends EventSourcedBehavior<Cab.Command, Cab.CabEvent, Cab.CabState> {
	String cabId;
	String majorState;
	String minorState;
	long rideID;
	long numRides;
	long numRequestsRecvd;

	long timeCounter;
	String entityId;
	
	public static final EntityTypeKey<Command> TypeKey =
		    EntityTypeKey.create(Cab.Command.class, "CabEntity");

	public Cab(ActorContext<Command> context, PersistenceId persistenceId) {
		super(persistenceId);
	}

	public final class CabState implements CborSerializable {
		String majorState;
		String minorState;
		long rideID;
		long numRides;
		long numRequestsRecvd;
		long curPos;

		public CabState() {
			super();
			this.majorState = CabStates.MajorStates.SIGNED_OUT;
			this.minorState = CabStates.MinorStates.NONE;
			this.numRides = 0;
			this.numRequestsRecvd = 0;
			this.curPos = -1;
		}

	}

	public static class Command implements CborSerializable {
		int dummy;
	}

	// Base Event
	public static class CabEvent implements CborSerializable {
		int dummy;

	}

	public static Behavior<Command> create(String entityId, PersistenceId persistenceId) {
		Logger.logErr("In 'create' of a new cab entity with id: " + entityId);

		return Behaviors.setup(context -> new Cab(context, persistenceId));
	}

	@Override
	public CommandHandler<Command, CabEvent, CabState> commandHandler() {
		return newCommandHandlerBuilder().forAnyState().onCommand(Command.class, notUsed -> {
			Logger.logErr("Shouldn't have received this generic command for Cab Entity");
			return Effect().none();
		}).build();
	}

	@Override
	public CabState emptyState() {
		// TODO Auto-generated method stub
		return new CabState();
	}

	@Override
	public EventHandler<CabState, CabEvent> eventHandler() {
		return null;
	}
}