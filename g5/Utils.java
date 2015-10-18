package pb.g5;

import pb.sim.Asteroid;
import pb.sim.Point;

import java.util.Arrays;
import java.util.Comparator;

public class Utils {

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
        Arrays.sort(hohmannAsteroids, new Comparator<Asteroid>() {
            @Override
            public int compare(Asteroid o1, Asteroid o2) {
                return Double.compare(Hohmann.transfer(o1, collideWith.orbit.a),
                        Hohmann.transfer(o1, collideWith.orbit.a));
            }
        });

        return hohmannAsteroids;
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

    public static long getLargestID(Asteroid[] asteroids){
        long largest = -1;
        for(Asteroid a:asteroids){
            if(a.id > largest)
                largest = a.id;
        }
        return largest;
    }

    public static double getEnergyMultiplier(int initAsteroids, int n_asteroids, long time_limit, long time){

        double asteroidsRemaining = n_asteroids / (double) initAsteroids;
        double timeRemaining = (time_limit - time)/(double) time_limit;

        if(time % 10 == 0)
        System.err.println(timeRemaining/asteroidsRemaining);

        return 1;
    }

}
