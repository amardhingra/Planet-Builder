package pb.g5;

import pb.sim.Asteroid;
import pb.sim.Point;
import pb.g5.Push;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Utils {

    public static final double MASS_PERCENT = 0.55;

    public static Asteroid[] sortByRadius(Asteroid[] asteroids){

        Asteroid[] radiusAsteroids = Arrays.copyOf(asteroids, asteroids.length);
        Arrays.sort(radiusAsteroids, new Comparator<Asteroid>() {
            @Override
            public int compare(Asteroid o1, Asteroid o2) {
                return -Double.compare(o1.orbit.a, o2.orbit.a);
            }
        });

        return radiusAsteroids;
    }

   
    /**
     * Does a Random push 
     * @param a1
     * @param time
     * @return
     */
    public static Push randomPush(Asteroid a1, long time){
    	Point vel = a1.orbit.velocityAt(time-a1.epoch);
    	double asteroidEnergy = 0.5*vel.magnitude()*vel.magnitude()*a1.mass;
    	//double maxPower=asteroidEnergy/10000;
    	double curDir = Math.atan2(vel.y, vel.x);
    	double pushDir = curDir ;
    	double energy = asteroidEnergy/10000;
    	return new Push(energy,pushDir);
    } 
    
    public static Asteroid[] sortByMass(Asteroid[] asteroids){

        Asteroid[] massAsteroids = Arrays.copyOf(asteroids, asteroids.length);
        Arrays.sort(massAsteroids, new Comparator<Asteroid>() {
            @Override
            public int compare(Asteroid o1, Asteroid o2) {
                return -Double.compare(o1.mass, o2.mass);
            }
        });

        return massAsteroids;
    }

    public static Asteroid[] sortByHohmannTransferEnergy(Asteroid[] asteroids, Asteroid collideWith){

        Asteroid[] hohmannAsteroids = Arrays.copyOf(asteroids, asteroids.length);
        Point center = new Point();
        collideWith.orbit.center(center);
        double c = center.magnitude();
        Arrays.sort(hohmannAsteroids, new Comparator<Asteroid>() {
            @Override
            public int compare(Asteroid o1, Asteroid o2) {
                if(o1.id == collideWith.id) return -1;
                if(o2.id == collideWith.id) return 1;
                return Double.compare(Math.max(Hohmann.transfer(o1, collideWith.orbit.a + c), Hohmann.transfer(o1, collideWith.orbit.a - c)),
                        Math.max(Hohmann.transfer(o2, collideWith.orbit.a + c), Hohmann.transfer(o2, collideWith.orbit.a - c)));
            }
        });


        return hohmannAsteroids;
    }

    public static Asteroid[] getFirstHalfMass(Asteroid[] asteroids, int collideWith){

        Asteroid[] hohmannAsteroids = sortByHohmannTransferEnergy(asteroids, asteroids[collideWith]);

        double totalMass = 0;
        for(Asteroid a:asteroids){
            totalMass += a.mass;
        }

        int i = 0;
        double massSoFar = 0;
        while(massSoFar/totalMass <= MASS_PERCENT){
            massSoFar += hohmannAsteroids[i].mass;
            i++;
        }

        Asteroid[] toReturn = new Asteroid[i - 1];
        for(int j = 1; j < i; j++){
            toReturn[j - 1] = hohmannAsteroids[j];
        }

        return toReturn;
    }

    public static int getClosestApproachToTargetWithinTime(Asteroid[] asteroids, int target, long time) {

        double closestDist = Double.MAX_VALUE;
        Asteroid a1, a2;
        a1 = asteroids[target];
        a2 = null;
        int index = 0;
        Point p1 = new Point(), p2 = new Point();
        for (long t = 0; t <= 1000; ++t) {
            a1.orbit.positionAt(time + t - a1.epoch, p1);
            for (int i = 0; i < asteroids.length; ++i) {
                if (i == target || asteroids[i] == null)
                    continue;
                a2 = asteroids[i];
                a2.orbit.positionAt(time + t - a2.epoch, p2);
                double dist = Point.distance(p1, p2);
                if (dist < closestDist) {
                    closestDist = dist;
                    a2 = asteroids[i];
                    index = i;
                }
            }
        }

        return index;
    }


    public static int findIndexOfAsteroid(Asteroid[] asteroids, long id){
        for(int i = 0; i < asteroids.length; i++){
            if(id == asteroids[i].id){
                return i;
            }
        }
        return -1;
    }

    public static double radius(Point point){
        return Math.sqrt(point.x * point.x + point.y + point.y);
    }

    public static long asteroidsCollide(Asteroid a1, Asteroid a2, long currentTime, long within){
        Point p1 = new Point();
        Point p2 = new Point();

        double tRadius = a1.radius() + a2.radius();
        for(long time = currentTime; time < currentTime + within; ++time){

            a1.orbit.positionAt(currentTime + time - a1.epoch, p1);
            a2.orbit.positionAt(currentTime + time - a2.epoch, p2);

            //System.out.println(p1);
            //System.out.println(p2);
            //System.out.println(Point.distance(p1, p2));
            if(Point.distance(p1, p2) < tRadius)
                return time + currentTime;
        }

        return currentTime;
    }

    public static double getHohmannEnergy(Asteroid toPush, Asteroid collideWith, double maxEnergy){

        double energy;
        double averageRadius = (collideWith.orbit.a + collideWith.orbit.b) / 2;

        if(toPush.orbit.a > averageRadius){
            energy =  Hohmann.reverseTransfer(toPush, averageRadius);
        } else {
            energy = Hohmann.transfer(toPush, averageRadius);
        }

        return 10 * energy;
    }


    public static double getEnergyMultiplier(ArrayList<Push> pushes, double maxEnergy,
                                             int initAsteroids, int n_asteroids,
                                             long time_limit, long time, int daysSinceCollision){

        if(pushes.size() == 0){
            return 0.33;
        }


        double asteroidsRemaining = n_asteroids / (double) initAsteroids;
        double timeRemaining = (time_limit - time)/(double) time_limit;

        double energyMultiplier = asteroidsRemaining / Math.pow(timeRemaining, 2);

        if(pushes.size() > 0 && (time_limit - time)/365 > 100) {
            double totalEnergySpent = 0;
            for (Push p : pushes) {
                totalEnergySpent += p.energy;
            }
            double averageEnergy = totalEnergySpent / pushes.size();
            energyMultiplier *= (averageEnergy/maxEnergy);
        }

        if((time_limit - time)/365 <= 20){
            energyMultiplier *= Math.pow(1.05, daysSinceCollision);
        } else if ((time_limit - time)/365 <= 50){
            energyMultiplier *= Math.pow(1.01, daysSinceCollision);
        } else if ((time_limit - time)/365 <= 100){
            energyMultiplier *= Math.pow(1.005, daysSinceCollision);
        } else if ((time_limit - time)/365 <= 250){
            energyMultiplier *= Math.pow(1.001, daysSinceCollision);
        } else {
            energyMultiplier *= Math.pow(1.0001, daysSinceCollision);
        }

        return energyMultiplier;
    }

}
