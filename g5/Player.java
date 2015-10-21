package pb.g5;

import pb.g5.Hohmann;
import pb.g5.Utils;
import pb.g5.Push;
import pb.sim.Orbit;
import pb.sim.Asteroid;

import java.util.*;

public class Player implements pb.sim.Player {

    public static final int NUMBER_OF_DAYS_TO_SKIP = 300;

    // current time, time limit
    private long time = -1;
    private long time_limit;

    private boolean collisionImminent;
    private long nextCollision;
    private long lastFailedCheck;
    private long timeToPush;
    private GD_Response futurePush;
    private int n_asteroids;
    private int n_asteroid_At_Start;
    double maxPower;
    int pushed = -1;
    int daysSinceCollision = 0;

    ArrayList<Push> pushes;

    // print orbital information
    public void init(Asteroid[] asteroids, long time_limit) {
        if (Orbit.dt() != 24 * 60 * 60)
            throw new IllegalStateException("Time quantum is not a day");
        this.time_limit = time_limit;

        // calculating the maximum energy to use per push
        Asteroid[] sortedByRadius = Utils.sortByRadius(asteroids);
        double maxHohmmannEnergy = Hohmann.transfer(sortedByRadius[sortedByRadius.length - 1], sortedByRadius[0].orbit.a);
        maxPower = maxHohmmannEnergy;

        pushes = new ArrayList<>();

        collisionImminent = false;
        nextCollision = Long.MIN_VALUE;
        timeToPush = -1;
        n_asteroids = asteroids.length;
        n_asteroid_At_Start = asteroids.length;
        futurePush = null;
        lastFailedCheck = Long.MIN_VALUE;
    }

    private int getHeaviestAsteroid(Asteroid[] asteroids) {
        double heaviest = Double.MIN_VALUE;
        int index = -1;
        for (int i = 0; i < asteroids.length; ++i) {
            if (heaviest < asteroids[i].mass) {
                heaviest = asteroids[i].mass;
                index = i;
            }
        }
        return index;
    }

    // try to push asteroid
    public void play(Asteroid[] asteroids,
                     double[] energy, double[] direction) {
        ++time;
        ++daysSinceCollision;

        if (time % 365 == 0) {
            System.out.println("Year: " + (1 + time / 365));
            System.out.println("Day: " + (1 + time % 365));
        }

        if (asteroids.length < n_asteroids) {
            collisionImminent = false;
            --n_asteroids;
            daysSinceCollision = 0;
        }
        
        if (timeToPush > time) {
        	return;
        }
        
        if (timeToPush == time) {
        	collisionImminent = true;
        	int index = futurePush.pushedIndex;
        	Push push = futurePush.push;
        	energy[index] = push.energy;
        	direction[index] = push.direction;
        	nextCollision = timeToPush + futurePush.predictedTime;
        	return;
        }

        if (collisionImminent && time == nextCollision) {
        }

        if (collisionImminent) {
            return;
        }
        
        if (time < lastFailedCheck + NUMBER_OF_DAYS_TO_SKIP){
	        	return;
	        }
        
        int heaviest = 0;

        if(n_asteroid_At_Start == n_asteroids)
            heaviest = Hohmann.getLowestAverageHohmanTransfer(asteroids);
        else
            heaviest = getHeaviestAsteroid(asteroids);
        GD_Response[] gdResp = doPushWithGradientDescent(asteroids, energy, direction, heaviest);
        Push bestPush = null;
        GradientDescent bestGD = null;
        long bestPushTime = 0;
        GD_Response bestGDResponse = null;
        for(GD_Response response : gdResp) {
            if(response == null) {
            	continue;
            }
            Push p = response.tuneGd();

            if(p == null) continue;

            response.tunePushTime(time);
            p = response.push;

            if(bestPush == null || bestPush.energy > p.energy){
                bestPush = p;
                bestGD = response.getGd();
                bestGDResponse = response;
                bestPushTime = response.pushTime;
            }
        }

        if (bestPush != null) {
            pushes.add(bestPush);
            if(time == bestPushTime) {
	            int i = bestGDResponse.getPushedIndex();
	            energy[i] = bestPush.energy;
	            direction[i] = bestPush.direction;
	            collisionImminent = true;
	            nextCollision = bestPushTime + bestGD.predictedTime;
	        } else {
	        	futurePush = bestGDResponse;
	        	timeToPush = bestPushTime;
	        }
            return;
        } else {
        	lastFailedCheck = time;
        }

    }
    
    private GD_Response[] doPushWithGradientDescent(Asteroid[] asteroids,
    		double[] energy, double[] direction, int heaviestOrFathest) {

    	double energyMultiplier = Utils.getEnergyMultiplier(pushes, maxPower, n_asteroid_At_Start, n_asteroids, time_limit, time, daysSinceCollision);

    	long pushTime = time+3000;

    	// get the closest set of asteroids
    	Asteroid[] closestSet = Utils.getFirstHalfMass(asteroids, heaviestOrFathest);
    	GD_Response[] responses = new GD_Response[closestSet.length];
    	Asteroid collideWith = asteroids[heaviestOrFathest];

    	// create a gradient descent response for each asteroid in the closest set list
    	for(int i = 0; i < closestSet.length; i++) {
    		responses[i] = new GD_Response(closestSet[i], collideWith, pushTime,
    				maxPower * energyMultiplier, Utils.findIndexOfAsteroid(asteroids, closestSet[i].id));
    	}

    	return responses;
    }
}
