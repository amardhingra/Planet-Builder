package pb.g5;

import pb.sim.Asteroid;
import pb.sim.Orbit;

public class OrbitUtils {
	static double getEccentricity(final double a, final double b){
		// compute the eccentricity
		double b_a = b / a;
		double e = Math.sqrt(1.0 - b_a * b_a);
		return e;
	}
	
	static void printIntersections(Asteroid[] asteroids){
		for(int i=0;i<asteroids.length; i++){
			Orbit o1=asteroids[i].orbit;
			for(int j=i+1;j<asteroids.length; j++ ){
				if(i==j ) continue;
				Orbit o2=asteroids[j].orbit;
				double intersection=getOrbitalIntersections(o1, o2);
				if(intersection <1){
					System.out.println("Orbit " + i + "(a= "+ o1.a +", b= "+ o1.b +") | Orbit " + j + "(a= "+ o2.a +", b= "+ o2.b +") | Intersections : " + intersection);
				}
			}
		}
		
	}
	static double getOrbitalIntersections(Orbit orbit1, Orbit orbit2){
		//calculate Eccentricity 
		double e1=OrbitUtils.getEccentricity(orbit1.a, orbit1.b);
		double e2=OrbitUtils.getEccentricity(orbit2.a, orbit2.b);
		double intersection= (orbit1.a * orbit1.a)*(1-e1*e1)+ (orbit2.a * orbit2.a)*(1-e2*e2) + (2*orbit1.a*orbit2.a)*(e1*e2  -1);
		return intersection;
	}
}
