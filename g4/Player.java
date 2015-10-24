package pb.g4;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.*;

public class Player implements pb.sim.Player{
  private Point sun = new Point(0, 0);

  // current time, time limit
  private long time = -1;
  private long time_limit = -1;
  private long collisionTime = -1;

  private Asteroid furthestFromSun;
  private Asteroid closerToSun;
  private Asteroid largestAsteroid;
  private HashSet<Long> asteroidIds;
  private HashSet<Long> preCollisionAsteroidIds;
  private int indexToPush = -1;
  private int indexToHit = -1;
  private double fifty_percent_mass;

  private int num_otherAsteroidLocation_asteroids = 4;
  private int numAsteroids;
  private boolean firstCollision = true;
  private boolean[] colliding;
  private double r2;
  private double totalMass = 0;

  // print orbital information
  public void init(Asteroid[] asteroids, long time_limit) {
    asteroidIds = new HashSet<Long>();
    firstCollision = false;
    colliding = new boolean[asteroids.length];
    numAsteroids = asteroids.length;
    System.out.println("Init");
    if (Orbit.dt() != 24 * 60 * 60)
      throw new IllegalStateException("Time quantum is not a day");
    this.time_limit = time_limit;
    for (int i = 0; i < asteroids.length; i++) {
      totalMass += asteroids[i].mass;
      asteroidIds.add(asteroids[i].id);
    }
  }

  // try to push asteroid
  public void play(Asteroid[] asteroids,
      double[] energy, double[] direction) {
    correctCollidedOrbits(asteroids, energy, direction);
    if (time % 365 == 0) {
      System.out.println("Year: " + time / 365);
    }
    if (asteroids.length != numAsteroids) {
      preCollisionAsteroidIds = asteroidIds;
      asteroidIds = new HashSet<Long>();
      for (int i = 0; i < asteroids.length; i++) {
        asteroidIds.add(asteroids[i].id);
      }
      colliding = new boolean[asteroids.length];
      firstCollision = false;
      numAsteroids = asteroids.length;
    }
    else if (++time%10 == 0 && time > collisionTime) {
      pushAllToLargest(asteroids, energy, direction);
    }
  }

  public void correctCollidedOrbits(Asteroid[] asteroids, double[] energy, double[] direction) {

    for (int i = 0; i < asteroids.length; i++ ) {
      if (Math.abs(asteroids[i].orbit.b - asteroids[i].orbit.a) > asteroids[i].radius() && !colliding[i]) {
        Point position = asteroids[i].orbit.positionAt(time - largestAsteroid.epoch);
        //Velocity for a hypothetical circular orbit at this position
        Point circularVelocity = new Orbit(position).velocityAt(0);
        Point currentVelocity = asteroids[i].orbit.velocityAt(time - largestAsteroid.epoch);
        Point dv = new Point(circularVelocity.x - currentVelocity.x, circularVelocity.y - currentVelocity.y);

        double pushEnergy = asteroids[i].mass * Math.pow(dv.magnitude(), 2) / 2;
        double pushAngle = dv.direction();

        energy[i] = pushEnergy;
        direction[i] = pushAngle;
      }
    }
  }

  public int findLargestAsteroidIndex(Asteroid[] asteroids) {
    double max_mass = 0;
    int idx = 0;
    for(int i = 0; i < asteroids.length; i++)
    {
      if(asteroids[i].mass > max_mass)
      {
        max_mass = asteroids[i].mass;
        idx = i;
      }
    }
    return idx;
  }

  public int findTargetAsteroidIndex(Asteroid[] asteroids) {
    long targetId = -1;

    for (Long id : asteroidIds) {
      if (preCollisionAsteroidIds.contains(id)) {
        targetId = id;
      }
    }

    for (int i = 0; i < asteroids.length; i++) {
      if (asteroids[i].id == targetId) {
        return i;
      }
    }
    return -1;
  }

  public void pushAllToLargest(Asteroid[] asteroids, double[] energy, double[] direction) {
    int largestAsteroid_idx;
    if (!firstCollision) {
      largestAsteroid_idx = findLargestAsteroidIndex(asteroids);
    }
    else {
      largestAsteroid_idx = getBestAsteroid(asteroids);
    }

    largestAsteroid = asteroids[largestAsteroid_idx];
    PriorityQueue<Asteroid> heap = new PriorityQueue<Asteroid>(new AsteroidComparator()); 

    for (int i = 0; i < asteroids.length; i++) {
      if (i != largestAsteroid_idx) heap.add(asteroids[i]);
    }

    r2 = largestAsteroid.orbit.a;
    double cumulativeMass = largestAsteroid.mass;

    do {
      Asteroid otherAsteroid = heap.poll();
      double mass = otherAsteroid.mass;
      cumulativeMass += mass;

      double pushAngle = otherAsteroid.orbit.velocityAt(time - otherAsteroid.epoch).direction();

      double r1 = otherAsteroid.orbit.a;
      double dv = Math.sqrt(Orbit.GM / r1)
        * (Math.sqrt((2 * r2)/(r1 + r2)) - 1);

      if (dv < 0) pushAngle += Math.PI;

      double pushEnergy = mass * dv * dv * 0.5;
      long predictedTimeOfCollision = prediction(otherAsteroid, largestAsteroid, time, pushEnergy, pushAngle);

      //If collision predicted and it's better than previous best collision
      if (predictedTimeOfCollision > 0) {
        System.out.println("collision predicted" + " at energy: "
            + pushEnergy + " and direction: " + pushAngle + " at year: " + time / 365);

        int bestAsteroidIndex = 0;
        for (int i = 0; i < asteroids.length; i++) {
          if (asteroids[i] == otherAsteroid) {
            bestAsteroidIndex = i;
            break;
          }
        }
        colliding[bestAsteroidIndex] = true;
        energy[bestAsteroidIndex] = pushEnergy;
        direction[bestAsteroidIndex] = pushAngle;	
        collisionTime = predictedTimeOfCollision > collisionTime ? predictedTimeOfCollision : collisionTime;
      }
    } while (cumulativeMass < totalMass / 2);
  }

  public long prediction(Asteroid source, Asteroid target, long time, double energy, 
      double direction) {
    try {
      source = Asteroid.push(source, time, energy, direction);
    } catch (InvalidOrbitException e) {
      System.out.println("Invalid orbit predicted with energy " + energy + " and angle " + direction);
    }

    Point p1 = source.orbit.velocityAt(time - source.epoch);
    Point p2 = new Point();
    double r = source.radius() + target.radius();
    // look 10 years in the future for collision
    for (long ft = 0 ; ft != 3650; ++ft) {
      long t = time + ft;
      if (t >= time_limit) 
        break;
      source.orbit.positionAt(t - source.epoch, p1);
      target.orbit.positionAt(t - target.epoch, p2);
      // if collision, return push to the simulator
      if (Point.distance(p1, p2) < r) {
        System.out.println("Collision predicted at time " + t);
        return t;
      }
    }
    return -1;
  }

  public class AsteroidComparator implements Comparator<Asteroid> {
    public int compare(Asteroid a1, Asteroid a2) {
      double m1 = a1.mass;
      double m2 = a2.mass;
      double r11 = a1.orbit.a;
      double r12 = a2.orbit.a;
      double dv1 = Math.sqrt(Orbit.GM / r11) * (Math.sqrt((2 * r2)/(r11 + r2)) - 1);
      double dv2 = Math.sqrt(Orbit.GM / r12) * (Math.sqrt((2 * r2)/(r12 + r2)) - 1);

      double e1 = m1 * dv1 * dv1 * 0.5;
      double e2 = m2 * dv2 * dv2 * 0.5;

      if (e1 > e2) {
        return 1;
      } else if (e1 < e2) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  public class OrbitComparator implements Comparator<Asteroid>
  {
    public int compare(Asteroid a1, Asteroid a2)
    {
      return (int)(getDistanceFromSun(a1) - getDistanceFromSun(a2));
    }


    public double getDistanceFromSun(Asteroid a)
    {
      return Point.distance(a.orbit.positionAt(time), sun);
    }
  }

  public int getBestAsteroid(Asteroid[] asteroids, double percent)
  {
    ArrayList<Asteroid> center_asteroids = new ArrayList<Asteroid>();
    ArrayList<Asteroid> sorted_asteroids = new ArrayList<Asteroid>(Arrays.asList(asteroids));
    Collections.sort(sorted_asteroids, new OrbitComparator());

    int num_asteroids = sorted_asteroids.size();
    int select_asteroids = (int)(percent*num_asteroids);
    double max_mass = 0;
    Asteroid max_asteroid;
    for (int i = (int)(0.5*num_asteroids - 0.5*select_asteroids); i < (int)(0.5*num_asteroids + 0.5*select_asteroids); i++)
    {
      if(sorted_asteroids.get(i).mass > max_mass)
      {
        max_mass = asteroids[i].mass;
        max_asteroid = asteroids[i];
      }

    }
    for(int i = 0; i < asteroids.length; i++)
    {
      if(max_mass == asteroids[i].mass)
      {
        return i;
      }
    }
    return -1;
  }

  public int getBestAsteroid(Asteroid[] asteroids) {
    int bestAsteroidIndex = -1;
    double bestEnergy = Double.MAX_VALUE;
    double r1, pushEnergy, dv, sumMass;
    Asteroid target, source;
    double[] hohmannSums = new double[asteroids.length];
    Arrays.fill(hohmannSums, 0.0);

    for (int i = 0; i < asteroids.length; i++) {
      sumMass = asteroids[i].mass;
      target = asteroids[i];
      r2 = target.orbit.a;
      //sum of energies required to transfer all other asteroids to asteroid i
      PriorityQueue<Asteroid> heap = new PriorityQueue<Asteroid>(new AsteroidComparator());
      for (int t = 0; t < asteroids.length; t++) {
        if (t != i) {
          heap.add(asteroids[t]);
        }
      }

      while (sumMass < totalMass) {
        source = heap.poll();
        r1 = source.orbit.a;
        dv = Math.sqrt(Orbit.GM / r1)
            * (Math.sqrt((2 * r2)/(r1 + r2)) - 1);
        pushEnergy = source.mass * dv * dv * 0.5;
        hohmannSums[i] += pushEnergy;
        sumMass += source.mass;
      }

      if (hohmannSums[i] < bestEnergy) {
        bestEnergy = hohmannSums[i];
        bestAsteroidIndex = i;
      }
    }
    return bestAsteroidIndex;
  }

}
