package pods.cabs.test;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;


import org.junit.ClassRule;
import org.junit.Test;

//#definition
public class AkkaCabHailingTest {

	@ClassRule
	public static final TestKitJunitResource testKit = new TestKitJunitResource();

	

	@Test
	public void testMainStarted() {
		

	}

}
