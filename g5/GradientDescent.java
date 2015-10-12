package pb.g5;

import pb.sim.Asteroid;
import pb.sim.Orbit;
import pb.sim.Point;

public class GradientDescent {
	Asteroid a;
	Point target;
	long timeOfStart;

	public GradientDescent(Asteroid a, Point target, long time) {
		super();
		this.a = a;
		this.target = target;
		timeOfStart = time;
		tune();
	}
	
	private void tune() {
		double direction = 0;
		double energy = varyEnergy(direction);
		for(int i = 0; i < 3; ++i) {
			System.out.println(direction + "," + energy);
			direction = varyDirection(energy);
			energy = varyEnergy(direction);
		}
		System.out.println(direction + "," + energy);
		Asteroid pushed = Asteroid.push(a, timeOfStart, energy, direction);
		double dist = getClosestDistanceVaryTime(pushed, timeOfStart);
		System.out.println("DIST : "+dist);
		System.out.println("RAD  : "+a.radius());
		System.out.println("DELTA: "+0.2*a.orbit.a);
	}
	
	private double getClosestDistanceVaryTime(Asteroid a, long timeOfStart) {
		int delta = 500;
		long timeDel = 0;
		double current = getDistance(a, timeOfStart + timeDel);
		while(delta >= 1) {
			double later = getDistance(a,timeOfStart + timeDel + delta);
			double earlier = getDistance(a,timeOfStart + timeDel - delta);
			if(later < current) {
				timeDel += delta;
				current = later;
			} else if(earlier < current) {
				timeDel -= delta;
				current = earlier;
			} else {
				if(delta >= 10)
					delta /= 10;
				else if(delta > 1)
					delta = 1;
				else
					delta = 0;
			}
		}
		Point p = a.orbit.positionAt(timeDel+timeOfStart-a.epoch);
		//System.out.println("Time:"+(timeOfStart+timeDel)+"\tdist:"+current+"\taLoc:"+p.x+","+p.y);
		return current;
	}
	
	private double varyDirection(double pushEnergy) {
		double currentDirection = 0;
		double delta = Math.PI/6;
		Asteroid pushed = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection);
		double current = getClosestDistanceVaryTime(pushed, timeOfStart);
		double angleDel = 0;
		while(delta >= Math.PI/72) {
			Asteroid higher = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection + delta);
			Asteroid lower = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection - delta);
			double dh = getClosestDistanceVaryTime(higher, timeOfStart);
			double dl = getClosestDistanceVaryTime(lower, timeOfStart);
			if(dh < current) {
				angleDel += delta;
				current = dh;
			} else if(dl < current) {
				angleDel -= delta;
				current = dl;
			} else {
				if(delta >= Math.PI/36)
					delta /= 2;
				else if(delta > Math.PI/72)
					delta = Math.PI/72;
				else
					delta = 0;
			}
		}
		return angleDel;
	}
	
	private double varyEnergy(double pushDirection) {
		Point vel = a.orbit.velocityAt(timeOfStart - a.epoch);
		double asteroidEnergy = Math.hypot(vel.x, vel.y) * a.mass * 0.5;
		double currentEnergy = asteroidEnergy * 0.4;
		double delta = asteroidEnergy * 0.1;
		Asteroid pushed = Asteroid.push(a, timeOfStart, currentEnergy, pushDirection);
		double current = getClosestDistanceVaryTime(pushed, timeOfStart);
		while(delta >= asteroidEnergy * 0.01) {
			Asteroid higher = Asteroid.push(a, timeOfStart, currentEnergy + delta, pushDirection);
			Asteroid lower = Asteroid.push(a, timeOfStart, currentEnergy - delta, pushDirection);
			double dh = getClosestDistanceVaryTime(higher, timeOfStart);
			double dl = getClosestDistanceVaryTime(lower, timeOfStart);
			if(dh < current) {
				currentEnergy += delta;
				current = dh;
			} else if(dl < current) {
				currentEnergy -= delta;
				current = dl;
			} else {
				if(delta >= asteroidEnergy * 0.02)
					delta /= 2;
				else if(delta > asteroidEnergy * 0.01)
					delta = asteroidEnergy * 0.01;
				else
					delta = 0;
			}
			while(delta >= currentEnergy) {
				delta /= 2;
			}
		}
		return currentEnergy;
	}
	
	private double getDistance(Asteroid a, long time) {
		return Point.distance(target, a.orbit.positionAt(time - a.epoch));
	}
}
