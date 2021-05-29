package pods.cabs.test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;
import pods.cabs.Cab;
import pods.cabs.RideService;
import pods.cabs.utils.Logger;

import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

//#definition
public class AkkaCabHailingTest {
	public static final Config config = ConfigFactory.parseString("akka {\n" +
			"loggers = [\"akka.event.slf4j.Slf4jLogger\"]\n"
			+ "loglevel = \"DEBUG\"\n" 
			+ "logging-filter = \"akka.event.slf4j.Slf4jLoggingFilter\"\n"
			+ "actor.provider = \"cluster\"\n"
			+ "actor.allow-java-serialization = on\n"
			+ "remote.artery.canonical.hostname = \"127.0.0.1\" \n" 
			+ "remote.artery.canonical.port = 0 \n"
			+ "cluster.seed-nodes = [\"akka://ClusterSystem@127.0.0.1:25251\", \"akka://ClusterSystem@127.0.0.1:25252\"]\n"
			+ "cluster.downing-provider-class= \"akka.cluster.sbr.SplitBrainResolverProvider\"\n"
			+ "persistence.journal.plugin=\"akka.persistence.journal.proxy\"\n" 
            + "persistence.journal.proxy.target-journal-plugin=\"akka.persistence.journal.leveldb\"\n" 
            + "persistence.journal.proxy.target-journal-address = \"akka://ClusterSystem@127.0.0.1:25251\"\n" 
            + "persistence.journal.proxy.start-target-journal = \"off\"\n" 
            + "}"
			);

	@ClassRule
	public static final TestKitJunitResource testKit = new TestKitJunitResource(config);
	
	
	
	public TestInterface testInterface;

	@Test
	public void testMainStarted() {
		this.testInterface = new TestInterface(testKit);
		Logger.log("Main Started\n");
		
		//Load Globals data
		testInterface.loadGlobalsData();
		
		publicTest1();
		naivePrivateTest();
		privateTestCase1();
		privateTestCase2();
		privateTestCase3();
		privateTestCase4();
	}
	
		
	public void publicTest1() {
		testInterface.startNewTest("Public Test 1");
		EntityRef<Cab.Command> cab101 = testInterface.getCabEntityRef("101");
		cab101.tell(new Cab.SignIn(10));
		EntityRef<RideService.Command> rideService = testInterface.getRideServiceEntityRef("rideService1");
		
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		TestProbe<RideService.RideResponse> probe = testKit.createTestProbe();
		rideService.tell(new RideService.RequestRide("201", 10, 100, probe.ref()));
		RideService.RideResponse resp = probe.receiveMessage();
		// Blocks and waits for a response message.
		// There is also an option to block for a bounded period of time
		// and give up after timeout.

		if(resp.rideId == -1) {
			Logger.logTestFail("Couldn't get a ride!");
		}
		else {
			cab101.tell(new Cab.RideEnded(resp.rideId));
			Logger.logTestSuccess("Got a ride and ended it!");
		}
	}
	
	
	// Just checking all functionality
		public void naivePrivateTest() {
			testInterface.startNewTest("Naive Test");

			TestProbe<Cab.NumRidesReponse> cabTestProbe = testKit.createTestProbe();
			testInterface.getCabEntityRef("101").tell(new Cab.NumRides(cabTestProbe.getRef()));
			cabTestProbe.expectMessage(new Cab.NumRidesReponse(0));
			Logger.log("Success : Cab NumRides functional\n");

			try {
				testInterface.getCabEntityRef("101").tell(new Cab.SignIn(0));
				testInterface.getCabEntityRef("102").tell(new Cab.SignIn(20));
				testInterface.getCabEntityRef("103").tell(new Cab.SignIn(40));
				testInterface.getCabEntityRef("104").tell(new Cab.SignIn(60));
				Thread.sleep(1000);
				testInterface.getCabEntityRef("101").tell(new Cab.SignOut());
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TestProbe<RideService.RideResponse> fufillRideTestProbe = testKit.createTestProbe();
			testInterface.getRideServiceEntityRef("rideService1").tell(new RideService.RequestRide("201", 50, 100, fufillRideTestProbe.getRef()));
			RideService.RideResponse rideResponse = fufillRideTestProbe.receiveMessage();

			if (rideResponse.rideId <= 0) {
				assertTrue(false);
			}

			testInterface.getCabEntityRef(rideResponse.cabId).tell(new Cab.RideEnded(rideResponse.rideId));
		}
		
		
		// Multiple requests to same cab and all alternate requests should be rejected
		public void privateTestCase1() {
			testInterface.startNewTest("Test 1 : Alternate Requests Reject");

			testInterface.cabSignIn("101", 10);
			testInterface.sleep();

			int acceptedRides = 0;

			RideService.RideResponse responseMsg ;

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				testInterface.getCabEntityRef(responseMsg.cabId).tell(new Cab.RideEnded(responseMsg.rideId));
				acceptedRides++;
			}

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				testInterface.getCabEntityRef(responseMsg.cabId).tell(new Cab.RideEnded(responseMsg.rideId));
				acceptedRides++;
			}

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				testInterface.getCabEntityRef(responseMsg.cabId).tell(new Cab.RideEnded(responseMsg.rideId));
				acceptedRides++;
			}

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				testInterface.getCabEntityRef(responseMsg.cabId).tell(new Cab.RideEnded(responseMsg.rideId));
				acceptedRides++;
			}

			Logger.logTestSuccess("Number of accepted rides : " + acceptedRides);
			assertTrue(acceptedRides <= 2);

		}
		
		
		// The cab with smallest id and in available state should always be assigned to the customer
		public void privateTestCase2() {
			testInterface.startNewTest("Test 2 : Smallest Index Cab Assignment");
			
			testInterface.cabSignIn("101", 10);
			testInterface.cabSignIn("103", 30);

			
			testInterface.sleep();
			
			RideService.RideResponse response;
			
			response = testInterface.requestRide("201", 45, 50);
			assert(response.cabId.equals("101"));
			
			response = testInterface.requestRide("202", 5, 10);
			assert(response.cabId.equals("103"));
			
		}
		
		
		// What if the nearest cab doesn't accept the request. Second nearest cab (which is available and accepting)
		// should be assigned.
		public void privateTestCase3() {
			testInterface.startNewTest("Test 3 : Second Nearest Cab Assignment");
			
			testInterface.cabSignIn("101", 10);
			testInterface.cabSignIn("102", 50);
			
			testInterface.sleep();
			
			RideService.RideResponse response;
			
			response = testInterface.requestRide("201", 5, 100);
			assert(response.cabId.equals("101"));
			
			if (response.rideId >= 0) {
				testInterface.getCabEntityRef(response.cabId).tell(new Cab.RideEnded(response.rideId));
			}
			
			testInterface.sleep();
			
			response = testInterface.requestRide("202", 105, 20);
			assert(!response.cabId.equals("101"));
			
		}
		
		
		// Multiple cabs and multiple ride requests
		public void privateTestCase4() {
			testInterface.startNewTest("Test 4 : Multiple cabs, Multiple ride requests");
			
			testInterface.cabSignIn("101", 10);
			testInterface.cabSignIn("102", 20);
			testInterface.cabSignIn("103", 30);
			
			RideService.RideResponse responseMsg;
			responseMsg = testInterface.requestRide("201", 5, 100);
			
			int acceptedRides = 0;
			
			if (responseMsg.rideId >= 0) {
				acceptedRides++;
			}

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				acceptedRides++;
			}

			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				acceptedRides++;
			}
			
			// since there are only 3 cabs, this ride request should fail
			responseMsg = testInterface.requestRide("201", 10, 20);
			if (responseMsg.rideId >= 0) {
				acceptedRides++;
			}
			
			Logger.logTestSuccess("Accepted Rides : " + acceptedRides);
			assertTrue(acceptedRides <= 3);
		}

}
