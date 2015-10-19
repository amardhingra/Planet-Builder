package pb.g5;

import pb.g5.Hohmann;
import pb.g5.Utils;
import pb.g5.Push;
import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.*;

public class Player implements pb.sim.Player {

    public static final int NUMBER_OF_COLLIDING_ASTEROIDS = 5;

    // current time, time limit
    private long time = -1;
    private long time_limit;

    private boolean collisionImminent;
    private long nextCollision;
    private int n_asteroids;
    private int n_asteroid_At_Start;
    private Integer[] closestPair;
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
        n_asteroids = asteroids.length;
        n_asteroid_At_Start = asteroids.length;
        closestPair = null;
    }

    private Integer[] getClosestPairAtTime(Asteroid[] asteroids, long time) {
        double closestDist = Double.MAX_VALUE;
        Integer[] pair = new Integer[2];
        Asteroid a1, a2;
        a1 = a2 = null;
        for (int i = 0; i < asteroids.length; ++i) {
            a1 = asteroids[i];
            Point p1 = a1.orbit.positionAt(time - a1.epoch);
            for (int j = i + 1; j < asteroids.length; ++j) {
                a2 = asteroids[j];
                Point p2 = a2.orbit.positionAt(time - a2.epoch);
                double dist = Point.distance(p1, p2);
                if (dist < closestDist) {
                    closestDist = dist;
                    pair[0] = i;
                    pair[1] = j;
                }
            }
        }
        return pair;
    }

    private Asteroid[] getBestCollidingGroup(Asteroid[] asteroids, int target){

        Asteroid[] bestAsteroids = new Asteroid[Math.min(NUMBER_OF_COLLIDING_ASTEROIDS, asteroids.length)];

        Asteroid[] asteroidsCopy = Arrays.copyOf(asteroids, asteroids.length);

        for(int i = 0; i < bestAsteroids.length; ++i){
            int index = Utils.getClosestApproachToTargetWithinTime(asteroidsCopy, target, time);
            bestAsteroids[i] = asteroidsCopy[index];
            asteroidsCopy[index] = null;
        }

        return bestAsteroids;
    }

    private Integer[] getClosestApproachToTargetWithinTime(Asteroid[] asteroids, int target, long time) {

        double closestDist = Double.MAX_VALUE;
        Integer[] pair = new Integer[2];
        Asteroid a1, a2;
        a1 = asteroids[target];
        a2 = null;
        Point p1 = new Point(), p2 = new Point();
        for (long t = 0; t <= 1000; ++t) {
            a1.orbit.positionAt(time + t - a1.epoch, p1);
            for (int i = 0; i < asteroids.length; ++i) {
                if (i == target)
                    continue;
                a2 = asteroids[i];
                a2.orbit.positionAt(time + t - a2.epoch, p2);
                double dist = Point.distance(p1, p2);
                if (dist < closestDist) {
                    closestDist = dist;
                    pair[0] = i;
                    pair[1] = target;
                }
            }
        }
        return pair;
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

        if (collisionImminent && time == nextCollision) {
            /*Asteroid a1 = asteroids[closestPair[0]];
            Point p1 = a1.orbit.positionAt(time - a1.epoch);
            Asteroid a2 = asteroids[closestPair[1]];
            Point p2 = a2.orbit.positionAt(time - a2.epoch);
            if (Point.distance(p1, p2) > a1.radius() + a2.radius()) {
                System.out.println(Point.distance(p1, p2));
                //System.exit(0);
            }*/
        }

        if (collisionImminent) {
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
        GD_Response bestGDResponse = null;
        for(GD_Response response : gdResp){
            if(gdResp == null){
                System.err.println("Returning?");
                return;
            }
            GradientDescent gd = response.getGd();
            Push p = gd.tune();

            if(p == null) continue;

            if(bestPush == null || bestPush.energy > p.energy){
                bestPush = p;
                bestGD = gd;
                bestGDResponse = response;
            }
        }

        if (bestPush != null) {
            pushes.add(bestPush);
            int i = bestGDResponse.getPushedIndex();
            energy[i] = bestPush.energy;
            direction[i] = bestPush.direction;
            collisionImminent = true;
            nextCollision = time + bestGD.predictedTime;
            return;
        }

    }

    private GD_Response[] doPushWithGradientDescent(Asteroid[] asteroids,
                                                  double[] energy, double[] direction, int heaviestOrFathest) {

        double energyMultiplier = Utils.getEnergyMultiplier(pushes, maxPower, n_asteroid_At_Start, n_asteroids, time_limit, time, daysSinceCollision);

        // get the closest set of asteroids
        Asteroid[] closestSet = getBestCollidingGroup(asteroids, heaviestOrFathest);
        GD_Response[] responses = new GD_Response[closestSet.length];
        Asteroid collideWith = asteroids[heaviestOrFathest];

        // create a gradient descent response for each asteroid in the closest set list
        for(int i = 0; i < closestSet.length; i++){
            GradientDescent gd = new GradientDescent(closestSet[i], collideWith, time, energyMultiplier * maxPower);
            responses[i] = new GD_Response(gd, Utils.findIndexOfAsteroid(asteroids, closestSet[i].id));
        }

        return responses;
    }
}
