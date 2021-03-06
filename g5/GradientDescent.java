package pb.g5;

import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;
import pb.sim.Point;

public class GradientDescent {
	public Asteroid a;
	public Asteroid target;
	long timeOfStart, predictedTime;
	double maxEnergy;
	
	static int maxTries = 1;
	static long timeSpent = 0;

	public GradientDescent(Asteroid a, Asteroid target, long time, double maxEnergy) {
		super();
		this.a = a;
		this.target = target;
		timeOfStart = time;
		this.predictedTime = 0;
		this.maxEnergy = maxEnergy;
	}
	
	public Push tune() {
		
		double direction = 0;
		double energy = varyEnergy(direction);
		for(int i = 0; i < 3; ++i) {
			direction = varyDirection(energy);
			energy = varyEnergy(direction);
		}
		Asteroid pushed = Asteroid.push(a, timeOfStart, energy, direction);
		predictedTime = getTimeOfClosestDistance(pushed, timeOfStart);
		double dist = getClosestDistanceVaryTime(pushed, timeOfStart);
		if(dist < a.radius() + target.radius()) {
			return new Push(energy, direction);
		}
		return null;
	}
	
	
	private double getClosestDistanceVaryTime(Asteroid a, long timeOfStart) {
		int delta = 200;
		long timeDel = 1000;
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
			while(timeDel <= delta) {
				delta /= 2;
			}
		}
		return current;
	}
	
	private long getTimeOfClosestDistance(Asteroid a, long timeOfStart) {
		int delta = 200;
		long timeDel = 1000;
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
			while(timeDel <= delta) {
				delta /= 2;
			}
		}
		return timeDel;
	}

	
	private double varyDirection(double pushEnergy) {
		double currentDirection = 0;
		double delta = Math.PI/6;
		double current = Double.MAX_VALUE;
		try {
			Asteroid pushed = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection);
			current = getClosestDistanceVaryTime(pushed, timeOfStart);
		} catch (InvalidOrbitException e) {
			// do nothing;
		}
		double angleDel = 0;
		int count = 0;
		while(delta >= Math.PI/288) {
			++count;
			double dh = Double.MAX_VALUE;
			double dl = Double.MAX_VALUE;
			
			try {
				Asteroid higher = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection + delta);
				dh = getClosestDistanceVaryTime(higher, timeOfStart);
			} catch(InvalidOrbitException e) {
				// do nothing;
				dh = Double.MAX_VALUE;
			}
			try {
				Asteroid lower = Asteroid.push(a, timeOfStart, pushEnergy, currentDirection - delta);
				dl = getClosestDistanceVaryTime(lower, timeOfStart);
			} catch (InvalidOrbitException e) {
				// do nothing;
				dl = Double.MAX_VALUE;
			}
			
			if(dh < current) {
				angleDel += delta;
				current = dh;
			} else if(dl < current) {
				angleDel -= delta;
				current = dl;
			} else {
				if(delta >= Math.PI/36)
					delta /= 2;
				else if(delta > Math.PI/288)
					delta = Math.PI/288;
				else
					delta = 0;
			}
		}
		return angleDel;
	}
	
	private double varyEnergy(double pushDirection) {
		Point vel = a.orbit.velocityAt(timeOfStart - a.epoch);
		double asteroidEnergy = Math.sqrt(vel.x*vel.x + vel.y*vel.y) * a.mass * 0.5;
		double currentEnergy = asteroidEnergy * 0.65;
		double delta = asteroidEnergy * 0.25;
		Asteroid pushed = Asteroid.push(a, timeOfStart, currentEnergy, pushDirection);
		double current = getClosestDistanceVaryTime(pushed, timeOfStart);
		while(delta >= currentEnergy) {
			delta = currentEnergy/2;
		}
		int count = 0;
		while(delta >= asteroidEnergy * 0.01 && currentEnergy + delta  < maxEnergy) {
			double dh = Double.MAX_VALUE;
			double dl = Double.MAX_VALUE;
			++count;
			try {
				Asteroid higher = Asteroid.push(a, timeOfStart, currentEnergy + delta, pushDirection);
				dh = getClosestDistanceVaryTime(higher, timeOfStart);
			} catch (InvalidOrbitException | NumberFormatException e) {
				// do nothing;
				dh = Double.MAX_VALUE;
			}
			try {
				Asteroid lower = Asteroid.push(a, timeOfStart, currentEnergy - delta, pushDirection);
				dl = getClosestDistanceVaryTime(lower, timeOfStart);
			} catch (InvalidOrbitException | NumberFormatException e) {
				// do nothing;
				dl = Double.MAX_VALUE;
			}

			if(dl <= current) {
				currentEnergy -= delta;
				current = dl;
			} else if(dh < current) {
				currentEnergy += delta;
				current = dh;
				delta += delta;
			} else {
				if(delta >= asteroidEnergy * 0.02)
					delta /= 2;
				else if(delta > asteroidEnergy * 0.01)
					delta = asteroidEnergy * 0.01;
				else
					delta = 0;
			}
			while(delta >= currentEnergy) {
				delta = currentEnergy/2;
			}
		}
		return currentEnergy;
	}
	
	private double getDistance(Asteroid a, long time) {
		Point p1 = target.orbit.positionAt(time - target.epoch);
		Point p2 = a.orbit.positionAt(time - a.epoch);
		double sumRadii = target.radius() + a.radius();
		double dist = Point.distance(p1, p2);
		if(dist <= sumRadii)
			return 0;
		return dist;
	}
}
