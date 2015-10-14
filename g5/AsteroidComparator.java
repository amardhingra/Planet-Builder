package pb.g5;

import java.util.Comparator;

import pb.sim.Asteroid;

public class AsteroidComparator implements Comparator<Asteroid>
	{
	    public int compare( Asteroid x, Asteroid y )
	    {
	    	//largest orbit comes first
	        return  (int) (Math.min(y.orbit.a, y.orbit.b) - Math.min(x.orbit.a, x.orbit.b))  ;
	    }
	}

