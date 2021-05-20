package pods.cabs;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import pods.cabs.utils.Logger;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import pods.cabs.utils.CborSerializable;

public class RideService extends AbstractBehavior<RideService.Command> {
	String entityId;
	
	public static final EntityTypeKey<Command> TypeKey = EntityTypeKey.create(RideService.Command.class,
			"RideServiceEntity");
	
	public RideService(ActorContext<RideService.Command> context, String rideServiceActorId) {
		super(context);
		this.entityId = rideServiceActorId;
//		Logger.logErr("In constructor of RideService actor, id : " + rideServiceActorId);
	}
	
	public static class Command implements CborSerializable{
		int dummy=0;
	}
	
	public Receive<RideService.Command> createReceive() {
		return newReceiveBuilder()
				.onMessage(RideService.Command.class, notUsed -> {
					Logger.logErr("Shouldn't have received this generic command for rideservice");
					return this;
				}).build();
	}

	public static Behavior<RideService.Command> create(String rideServiceActorId) {
		Logger.logErr("In 'create' of a new RideService actor, id : " + rideServiceActorId);
		return Behaviors.setup(context -> {
			return new RideService(context, rideServiceActorId);
		});
	}
}
