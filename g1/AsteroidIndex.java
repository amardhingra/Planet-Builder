package pb.g1;

import pb.g7.asteroid_index;

public class AsteroidIndex implements Comparable<AsteroidIndex>{
	public int index;
	public final double mass;
	public double radius;
	
	public AsteroidIndex(int index, double mass,double radius)
	{
		this.index = index;
		this.mass = mass;
		this.radius = radius;
	}

	@Override
	public int compareTo(AsteroidIndex ai) {
		// TODO Auto-generated method stub
		if(radius > ai.radius ) return 1;
		else if(radius < ai.radius) return -1;
		else 
		return 0;
	}
	
}


