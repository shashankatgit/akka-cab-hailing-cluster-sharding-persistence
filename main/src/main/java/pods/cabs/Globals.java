package pods.cabs;

import java.util.concurrent.atomic.AtomicLong;

import pods.cabs.utils.InitFileReader.InitReadWrapper;

public class Globals {
	public static InitReadWrapper initReadWrapperObj;

	public static final AtomicLong rideIdSequence;

	static {
		rideIdSequence = new AtomicLong(0);
	}
}
