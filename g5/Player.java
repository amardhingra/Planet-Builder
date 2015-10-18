package pb.g5;

import pb.g5.Hohmann;
import pb.g5.Utils;
import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class Player implements pb.sim.Player {

    // used to pick asteroid and velocity boost randomly

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

    // print orbital information
    public void init(Asteroid[] asteroids, long time_limit) {
        if (Orbit.dt() != 24 * 60 * 60)
            throw new IllegalStateException("Time quantum is not a day");
        this.time_limit = time_limit;

        // calculating the maximum energy to use per push
        Asteroid[] sortedByRadius = Utils.sortByRadius(asteroids);
        double maxHohmmannEnergy = Hohmann.transfer(sortedByRadius[sortedByRadius.length - 1], sortedByRadius[0].orbit.a);
        maxPower = maxHohmmannEnergy;//* Math.sqrt(time_limit) / Math.pow(asteroids.length, 2) ;
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

    private PriorityQueue<Asteroid> getFarthestAsteroid(Asteroid[] asteroids) {
        PriorityQueue<Asteroid> maxRadiusAsteroidsHeap = new PriorityQueue<Asteroid>(asteroids.length, new AsteroidComparator());

        double maxRadius = Double.MIN_VALUE;
        int index = -1;
        for (int i = 0; i < asteroids.length; ++i) {
            maxRadiusAsteroidsHeap.add(asteroids[i]);
        }
        return maxRadiusAsteroidsHeap;
    }

    // try to push asteroid
    public void play(Asteroid[] asteroids,
                     double[] energy, double[] direction) {
        ++time;

        if (time % 365 == 0) {
            System.out.println("Year: " + (1 + time / 365));
            System.out.println("Day: " + (1 + time % 365));
        }

        if (asteroids.length < n_asteroids) {
            collisionImminent = false;
            --n_asteroids;
        }

        if (collisionImminent && time == nextCollision) {
            Asteroid a1 = asteroids[closestPair[0]];
            Point p1 = a1.orbit.positionAt(time - a1.epoch);
            Asteroid a2 = asteroids[closestPair[1]];
            Point p2 = a2.orbit.positionAt(time - a2.epoch);
            if (Point.distance(p1, p2) > a1.radius() + a2.radius()) {
                System.out.println(Point.distance(p1, p2));
                //System.exit(0);
            }
        }

        if (collisionImminent) {
            return;
        }
        int heaviest = 0;

        if(n_asteroid_At_Start == n_asteroids)
            heaviest = Hohmann.getLowestAverageHohmanTransfer(asteroids);
        else
            heaviest = getHeaviestAsteroid(asteroids);
        GD_Response gdResp = doPushWithGradientDescent(asteroids, energy, direction, heaviest);
        if (gdResp == null) {
            return;
        }
        GradientDescent gd = gdResp.getGd();
        Push p = gd.tune();
        if (p != null) {
            if (p.energy > maxPower) {
                return;
            }
            int i = gdResp.getPushedIndex();
            energy[i] = p.energy;
            direction[i] = p.direction;
            collisionImminent = true;
            nextCollision = time + gd.predictedTime;
            return;
        }

    }


    private double l2norm(Point p) {
        return Math.sqrt(p.x * p.x + p.y * p.y);
    }

    private double l2norm(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public boolean isAsteroidInRange(Asteroid heavy, Asteroid lighter) {
        Point v1 = heavy.orbit.velocityAt(time - heavy.epoch);
        double normv1 = l2norm(v1);
        double theta1 = Math.atan2(v1.y, v1.x);

        Point v2 = lighter.orbit.velocityAt(time - lighter.epoch);
        double normv2 = l2norm(v2);
        double theta2 = Math.atan2(v2.y, v2.x);
        if (Math.abs(theta1 - theta2) < Math.PI / 6) {
            System.out.println("Asteroids are in Range ");
            return true;
        } else {
            System.out.println("Asteroids are not in range");
            return false;
        }
    }

    private GD_Response doPushWithGradientDescent(Asteroid[] asteroids,
                                                  double[] energy, double[] direction, int heaviestOrFathest) {

        double energyMultiplier = Utils.getEnergyMultiplier(n_asteroid_At_Start, n_asteroids, time_limit, time);

        closestPair = getClosestApproachToTargetWithinTime(asteroids, heaviestOrFathest, time);
        Asteroid a1 = asteroids[closestPair[0]];
        Asteroid a2 = asteroids[closestPair[1]];
        int pushedIndex = closestPair[0];
        if (a2.mass < a1.mass) {
            Asteroid temp = a1;
            a1 = a2;
            a2 = temp;
            pushedIndex = closestPair[1];
        }

        if (isAsteroidInRange(a1.mass > a2.mass ? a1 : a2, a1.mass > a2.mass ? a2 : a1) == false) {
            return null;
        }
        GradientDescent gd = new GradientDescent(a1, a2, time,energyMultiplier * maxPower);
        return new GD_Response(gd, pushedIndex);
    }
}
