package pods.cabs.test;

import static org.junit.Assert.assertTrue;

import java.io.Console;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;
import akka.persistence.typed.PersistenceId;
import pods.cabs.Cab;
import pods.cabs.Globals;
import pods.cabs.RideService;
import pods.cabs.utils.InitFileReader;
import pods.cabs.utils.Logger;
import pods.cabs.utils.InitFileReader.InitReadWrapper;

public class TestInterface {
	
	public TestKitJunitResource testKit;
	
	private ClusterSharding _sharding = null;
	
	
	
	public ClusterSharding getSharding() {
	    if (_sharding == null) {
	      // initialize first time only
	      Cluster cluster = Cluster.get(testKit.system());
	      cluster.manager().tell(Join.create(cluster.selfMember().address()));

	      ClusterSharding sharding = ClusterSharding.get(testKit.system());
	      
	   // Create sharding proxy for RideService entities
		sharding.init(
				Entity.of(RideService.TypeKey, entityContext -> RideService.create(entityContext.getEntityId())));

		// Create sharding proxy for Cab entities
		sharding.init(Entity.of(Cab.TypeKey, entityContext -> Cab.create(entityContext.getEntityId(),
				PersistenceId.of(entityContext.getEntityTypeKey().name(), entityContext.getEntityId()))));

	      _sharding = sharding;
	    }
	    
	    return _sharding;
	}

	public TestInterface(TestKitJunitResource testKit) {
		super();
		this.testKit = testKit;

	}
	
	void resetAllCabs() {
		TestProbe<Cab.NumRidesReponse> testProbe = testKit.createTestProbe();
		for (String cabId : Globals.initReadWrapperObj.cabIDList) {
			EntityRef<Cab.Command> cabRef = getSharding().entityRefFor(Cab.TypeKey, cabId);
			cabRef.tell(new Cab.Reset(testProbe.getRef()));
			Cab.NumRidesReponse response = testProbe.receiveMessage();
		}
	}
	
	void resetAll() {
		resetAllCabs();
	}
	
	void startNewTest(String testName) {
		sleep();
		sleep();
		System.out.println(Logger.ANSI_PURPLE + "\n\n----------Starting New Test Case - " + testName + " ----------------\n" + Logger.ANSI_RESET); 
		sleep();

		resetAll();
		System.out.println("\n\n");
	}
 
	
	
	void cabSignIn(String cabId, long initialPos) {
		getSharding().entityRefFor(Cab.TypeKey, cabId).tell(new Cab.SignIn(initialPos));
	}
	
	void cabSignOut(String cabId) {
		getCabEntityRef(cabId).tell(new Cab.SignOut());
	}
	
	void rideEnded(String cabId, long rideId) {
		getCabEntityRef(cabId).tell(new Cab.RideEnded(rideId));
	}
	
	RideService.RideResponse requestRide(String custId, long srcPos, long destPos) {
		TestProbe<RideService.RideResponse> fufillRideTestProbe = testKit.createTestProbe();
		
		String randRideServiceEntityId = Globals.getRandRideService();
		EntityRef<RideService.Command> rideServiceRef = getSharding().entityRefFor(RideService.TypeKey, randRideServiceEntityId);
		
		rideServiceRef.tell(new RideService.RequestRide(custId, srcPos, destPos, fufillRideTestProbe.getRef()));
		RideService.RideResponse rideResponse =  fufillRideTestProbe.receiveMessage();
		
		Logger.logTestSuccess("Received Response : rideId: " +rideResponse.rideId + ", cabId: "+rideResponse.cabId);
		
		return rideResponse;
	}
	
	EntityRef<Cab.Command> getCabEntityRef(String cabEntityId){
		return getSharding().entityRefFor(Cab.TypeKey, cabEntityId);
	}
	
	EntityRef<RideService.Command> getRideServiceEntityRef(String rideServiceEntityId){
		return getSharding().entityRefFor(RideService.TypeKey, rideServiceEntityId);
	}
	
	void loadGlobalsData() {
		InitReadWrapper wrapperObj = new InitReadWrapper();
		Globals.initReadWrapperObj = wrapperObj;
		try {
			InitFileReader.readInitFile(wrapperObj);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	void sleep() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
