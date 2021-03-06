package pb.g7;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Iterator;

public class Player implements pb.sim.Player {

    // used to pick asteroid and velocity boost randomly
    private Random random = new Random(0);

    // current time, time limit
    private long time = -1;
    private long time_limit = -1;

    private int prevNumAsteroids = -1;

    private static final double G = Orbit.GM / Orbit.M;//6.67382967579392e-11;
    private static final double M = Orbit.M;//1.98855e30;

    private long accumulatorID;

    private HashMap<Long,Long> collidingAsteroids;

    HashSet<push_move> asteroids_in_phase;

    private int init_num_asteroids;

    private double accumulated_mass;

    private double target_mass;

    private boolean[] orbit_corrected;
    private boolean orbit_correction_done = true;
    private boolean greater_max_case;

    HashMap<Long, push_move> push_queue = new HashMap<Long, push_move>();

    public void init(Asteroid[] asteroids, long time_limit)
    {
	greater_max_case = is_greater_mass_case(asteroids);

	if(greater_max_case)
	    {
		target_mass = get_total_mass(asteroids);
	    }
	else
	    {
		target_mass = 0.5*get_total_mass(asteroids);
	    }

	if (Orbit.dt() != 24 * 60 * 60)
	    throw new IllegalStateException("Time quantum is not a day");
	this.time_limit = time_limit;
	prevNumAsteroids = asteroids.length;
	init_num_asteroids = asteroids.length;
	System.out.println("Computing now....");
	this.collidingAsteroids = calc_asteroids(asteroids);
	if (this.collidingAsteroids == null) {
	    orbit_correction_done = false;
	    orbit_corrected = new boolean[asteroids.length];
	    for (int i=0; i<asteroids.length; i++) {
		orbit_corrected[i] = false;
	    }
	}
	
   }

    public boolean is_greater_mass_case(Asteroid[] asteroids)
    {
    	double total_mass = get_total_mass(asteroids);
    	for(int i=0; i<asteroids.length; ++i)
    	{
    		if(asteroids[i].mass > total_mass/2.0)
    		{
    			return true;
    		}
    	}
    	return false;
    }

    private HashSet<push_move> getAsteroidsInPhase(Asteroid[] asteroids, long time_limit, int accumulator_index) {
	int n = asteroids.length;
	ArrayList<asteroid_index> asteroids_ordered = new ArrayList <asteroid_index> ();
	Point astr_location = new Point();
	for (int i=0; i<n; i++) {
	    asteroids[i].orbit.positionAt(time - asteroids[i].epoch, astr_location);
	    asteroids_ordered.add(new asteroid_index(i, l2norm(astr_location)));
	}
	Collections.sort(asteroids_ordered);	
	Asteroid accumulator = asteroids[asteroids_ordered.get(accumulator_index).index];
	Point v1 = new Point();
	Point v2 = new Point();
	double r2 = 0.5*(accumulator.orbit.a + accumulator.orbit.b);
	double omega2 = Math.sqrt(Orbit.GM / Math.pow(r2,3));

	HashSet<push_move> asteroids_in_phase = new HashSet<push_move>();
	for (int i=0; i<n; i++) {
	    if (i == accumulator_index) {continue;}
	    Asteroid push_asteroid = asteroids[asteroids_ordered.get(i).index];
	    double r1 = 0.5*(push_asteroid.orbit.a+push_asteroid.orbit.b);
	    double thresh = push_asteroid.radius() + accumulator.radius();
	    double tH = Math.PI * Math.sqrt( Math.pow(r1+r2,3) / (8*Orbit.GM));

	    for (long t = time+1 ; t < .8*time_limit ; ++t) {
		push_asteroid.orbit.velocityAt(t, v1);
		double theta1 = Math.atan2(v1.y,v1.x);
		accumulator.orbit.velocityAt(t, v2);
		double theta2 = Math.atan2(v2.y,v2.x);		    
		if ( Math.abs(theta1 + Math.PI - theta2 - tH*omega2) < thresh / r2) {
		    double deltav = Math.sqrt(Orbit.GM / r1) * (Math.sqrt( 2*r2 / (r1+r2)) - 1);
		    // double E = Math.abs(0.5*asteroids[asteroids_ordered.get(i).index].mass * (deltav * deltav + 2 * l2norm(v1) * deltav));
		    double E = 0.5*asteroids[asteroids_ordered.get(i).index].mass*(deltav*deltav);
		    asteroids_in_phase.add(new push_move(i,push_asteroid.id, E, theta1, time+t, E / push_asteroid.mass, push_asteroid.orbit.a));
		    break;
		}
	    }
	}	      
	return asteroids_in_phase;
    }

public double get_total_mass(Asteroid[] asteroids)
    {
    	double total_mass = 0.0;
    	for(Asteroid each_asteroid : asteroids)
    		total_mass += each_asteroid.mass;
    	return total_mass;
    }

public HashMap<Long, Long> calc_asteroids(Asteroid[] asteroids)
{
	//This function calculates the 'optimal' orbit and all the asteroids that come in phase within the time_limit with the 'optimal' orbit
	double threshold = .5;
	if(greater_max_case){threshold = 1.0;}
	int n = asteroids.length;
	ArrayList<asteroid_index> asteroids_ordered = new ArrayList <asteroid_index> ();
	Point astr_location = new Point();
	for (int i=0; i<n; i++) {
	    asteroids[i].orbit.positionAt(time - asteroids[i].epoch, astr_location);
	    asteroids_ordered.add(new asteroid_index(i, l2norm(astr_location)));
	}
	Collections.sort(asteroids_ordered);	
    
	double total_mass = get_total_mass(asteroids);
	HashMap<Long,Long> toPush = new HashMap<Long,Long>();
	HashMap<Long,Long> toPush_min = null;
	int accumulator_index_min = -1;

	Comparator<push_move> density_comparator = new Comparator<push_move>(){
	public int compare(push_move pm1, push_move pm2)
	{
		double d1 = pm1.density;
		double d2 = pm2.density;
		return Double.compare(d1, d2);
	} };
	

	Comparator<push_move> energy_comparator = new Comparator<push_move>(){
		public int compare(push_move pm1, push_move pm2)
		{
			double e1 = pm1.energy;
			double e2 = pm2.energy;
			return Double.compare(e1, e2);
		} };

	double total_energy_min = Double.MAX_VALUE;
	for (int accumulator_index=asteroids.length-1; accumulator_index>=0; accumulator_index--)
	{
		HashSet<push_move> asteroids_in_phase_accumulator = getAsteroidsInPhase(asteroids, this.time_limit * 9 / 10, accumulator_index);
		if(asteroids_in_phase_accumulator.isEmpty())
		{
			System.out.println("No asteroids come in phase.");
			continue;
		}

		PriorityQueue<push_move> min_density_q = new PriorityQueue<push_move>(asteroids_in_phase_accumulator.size(), density_comparator);
		PriorityQueue<push_move> min_energy_q = new PriorityQueue<push_move>(asteroids_in_phase_accumulator.size(), energy_comparator);
		Asteroid accumulator = asteroids[asteroids_ordered.get(accumulator_index).index];
		double accumulated_mass = accumulator.mass;
		double accumulator_velocity = Math.sqrt(Orbit.GM / asteroids[asteroids_ordered.get(accumulator_index).index].orbit.a);
		double total_energy = 0.0; // calculating the total energy expended using each orbit as accumulator
		double total_correction_energy = 0.0;

		for(push_move pm : asteroids_in_phase_accumulator)
		{
			min_density_q.add(pm);
			min_energy_q.add(pm);
		}
		while(accumulated_mass < total_mass*threshold)
		{
		    if(min_energy_q.isEmpty() || min_density_q.isEmpty())
			{
				total_energy = Double.MAX_VALUE;
			    System.out.println("Asteroids don't have enough mass");
			    break;
			}

		    push_move minE = min_energy_q.peek();
		    push_move minD = min_density_q.peek();
		    double massE = asteroids[asteroids_ordered.get(minE.index).index].mass; //density = Energy/Mass 
		    double massD = asteroids[asteroids_ordered.get(minD.index).index].mass;
		    double push_mass = 0;
		    double push_energy = 0;
		    double push_radius = 0;
		    if((accumulated_mass + massE)>= total_mass*threshold)
			{//By energy:
			    push_mass = massE;
			    push_energy = minE.energy;
			    push_radius = minE.radius;
			    toPush.put(minE.id, minE.time);
			    min_energy_q.remove(minE);
			    min_density_q.remove(minE);
			}
		    else
			{//By energy density:
			    push_mass = massD;
			    push_energy = minD.energy;
			    push_radius = minD.radius;
			    toPush.put(minD.id, minD.time);
			    total_energy += minD.energy;
			    min_energy_q.remove(minD); 
			    min_density_q.remove(minD);
			}		    
		    
		    total_energy += push_energy;
		    double velocity_after_push = 0;
		    if (push_radius < accumulator.orbit.a) {velocity_after_push = orbitVelocity(push_radius) + Math.sqrt(2*push_energy/push_mass);}
		    else {velocity_after_push = orbitVelocity(push_radius) - Math.sqrt(2*push_energy/push_mass);}
		    double velocity_at_collision = velocity_after_push * push_radius / accumulator.orbit.a;
		    double velocity_after_collision = (accumulated_mass * orbitVelocity(accumulator.orbit.a) + push_mass * velocity_at_collision) / (accumulated_mass + push_mass);
		    accumulated_mass += push_mass;		    
		    total_energy += Math.pow(orbitVelocity(accumulator.orbit.a) - velocity_after_collision,2) * accumulated_mass * 0.5;
		    
		    if (total_energy > total_energy_min) {
			break;
		    }		   		    
		    
		}		
		// System.out.println("accumulator index : "+accumulator_index+" | total_energy : "+total_energy+ " | Mass accumulated : "+accumulated_mass);
		if (total_energy < total_energy_min)
		{
			total_energy_min = total_energy;
			toPush_min = new HashMap<Long,Long>(toPush);
			accumulator_index_min = accumulator_index;

		}
		toPush.clear();
	}
	if(total_energy_min == Double.MAX_VALUE) return null;
	System.out.println("number of asteroids to push = " + toPush_min.size() + " | **Accumulator index = "+accumulator_index_min+" | Predicted energy = ~"+total_energy_min+" Joules");
	accumulatorID = asteroids[asteroids_ordered.get(accumulator_index_min).index].id;
	return toPush_min;
}
    
    private double orbitVelocity(double r) {return Math.sqrt(Orbit.GM / r);}
    private double l2norm(Point p) {return Math.sqrt(p.x*p.x+p.y*p.y);}
    private double l2norm(double x, double y) {return Math.sqrt(x*x+y*y);}

    private ArrayList<Double> calculatePush(double speed, double angle, double targetSpeed, double targetAngle) {
	double vx = speed * Math.cos(angle);
	double vy = speed * Math.sin(angle);
	double target_vx = targetSpeed * Math.cos(targetAngle);
	double target_vy = targetSpeed * Math.sin(targetAngle);
	double push_vx = target_vx - vx;
	double push_vy = target_vy - vy;
	double push_angle = Math.atan2(push_vy, push_vx);
	double push_speed = l2norm(push_vx, push_vy);

	ArrayList<Double> parameters = new ArrayList<Double>();
	parameters.add(new Double(push_speed));
	parameters.add(new Double(push_angle));
	return parameters;
    }

    public boolean pathFree(Asteroid[] asteroids, int push_index, int target_index, double push_energy, double push_dir, long push_time) {
    // This function checks if any asteroids come in the way of the asteroid being pushed
	Asteroid newAsteroid = null;
	try {
	    newAsteroid = Asteroid.push(asteroids[push_index], push_time, push_energy, push_dir);
	}
	catch (pb.sim.InvalidOrbitException e){
	    System.out.println(" Invalid orbit: " + e.getMessage());
	    return false;
	}
	long t = push_time;
	int n = asteroids.length;
	Point p1 = new Point();
	Point p2 = new Point();
	while (t < time_limit) {
	    newAsteroid.orbit.positionAt(t - newAsteroid.epoch, p1);
	    for (int i=0; i<n; i++) {
		if (i == push_index) {continue;}
		asteroids[i].orbit.positionAt(t - asteroids[i].epoch, p2);
		if (l2norm(p1.x-p2.x,p1.y-p2.y) < asteroids[i].radius() + newAsteroid.radius()) {
		    if (i == target_index) {
			return true;
		    }
		    else {
			return false;
		    }
		}
	    }
	    t++;
	}
	System.out.println("No collision...");
	return false;
    }

    public void perturb_orbits(Asteroid[] asteroids, double[] energy, double[] direction) {
    // This function is used to perturb the orbits fractionally, if no asteroids come in phase
	Point location = new Point();
	Point velocity = new Point();
	boolean allDone = true;
	for (int i=0; i<asteroids.length; i++) {
	    asteroids[i].orbit.positionAt(time - asteroids[i].epoch,location);
	    asteroids[i].orbit.velocityAt(time - asteroids[i].epoch,velocity);
	    if (asteroids[i].orbit.a == asteroids[i].orbit.b){// && !orbit_corrected[i]) {
		if (!orbit_corrected[i]) {
		    if (random.nextDouble() < 0.1) {
			double r2 = asteroids[i].orbit.a * (1 + random.nextDouble()*0.25);
			double r1 = asteroids[i].orbit.a;
			double deltav = Math.sqrt(Orbit.GM / r1) * (Math.sqrt( 2*r2 / (r1+r2)) - 1);
			double E = 0.5*asteroids[i].mass * deltav * deltav;
			double theta1 = Math.atan2(velocity.y,velocity.x);
			double dir = theta1;		
			energy[i] = E;
			direction[i] = dir;
			allDone = false;
			orbit_corrected[i] = true;
		    }
		    else {
			orbit_corrected[i] = true;
		    }
		}
	    }
	    else if ( Math.abs(l2norm(location) - Math.max(asteroids[i].orbit.a, asteroids[i].orbit.b)) < asteroids[i].radius() && asteroids[i].orbit.a != asteroids[i].orbit.b) {
		double r2 = l2norm(location);
		double orbit_speed = Math.sqrt(Orbit.GM / r2);
		double tangent_theta = Math.PI/2 + Math.atan2(location.y, location.x);
		double omega2 = Math.sqrt(Orbit.GM / Math.pow(r2,3));
		Point v2 = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);
		double normv2 = l2norm(v2);
		double theta2 = Math.atan2(v2.y,v2.x);
		ArrayList<Double> parameters = calculatePush(normv2, theta2, orbit_speed, tangent_theta);
		energy[i] = 0.5 * asteroids[i].mass * Math.pow(parameters.get(0),2);
		direction[i] = parameters.get(1);		
	    }
	    else {
		allDone = false;
	    }
	}
	if (allDone) {
	    orbit_correction_done = true;
	    this.collidingAsteroids = calc_asteroids(asteroids);
	    if (this.collidingAsteroids == null) {
		orbit_correction_done = false;
		for (int i=0; i<asteroids.length; i++) {
		    orbit_corrected[i] = false;
		}
	    }
	}
   }

    // try to push asteroid
    public void play(Asteroid[] asteroids,
		     double[] energy, double[] direction)
    {
	time++;
	if (!orbit_correction_done) {
	    perturb_orbits(asteroids,energy,direction);
	    return;
	}
	int n = asteroids.length;
	ArrayList<asteroid_index> asteroids_ordered = new ArrayList <asteroid_index> ();
	Point astr_location = new Point();
	for (int i=0; i<n; i++) {
	    asteroids[i].orbit.positionAt(time - asteroids[i].epoch, astr_location);
	    asteroids_ordered.add(new asteroid_index(i, l2norm(astr_location)));
	}
	Collections.sort(asteroids_ordered);

	if (n < prevNumAsteroids) { //collision occurred, correct orbit of outermost asteroid

	    boolean collided = true;
	    for (int i=0; i<asteroids.length; i++) {
		if (asteroids[i].id == accumulatorID) {
		    collided = false;
		    break;
		}
	    }

	    if (collided) {
		System.out.println("**Collision with accumulator at : "+"  Day: "  + (1 + time % 365)+"  Year: " + (1 + time / 365));
		accumulatorID = -1;
		int accumulatorIndex = -1;
		for (int i=0; i<asteroids.length; i++) {
		    if (asteroids[i].id > accumulatorID) {
			accumulatorID = asteroids[i].id;
			accumulatorIndex = i;
		    }
		}
		
		Point location = new Point();
		Asteroid accumulator = asteroids[accumulatorIndex];
		accumulator.orbit.positionAt(time - accumulator.epoch, location);
		double r2 = l2norm(location); 
		double orbit_speed = Math.sqrt(Orbit.GM / r2);
		double tangent_theta = Math.PI/2 + Math.atan2(location.y, location.x);
		double omega2 = Math.sqrt(Orbit.GM / Math.pow(r2,3));
		Point v2 = accumulator.orbit.velocityAt(time - accumulator.epoch);
		double normv2 = l2norm(v2);
		double theta2 = Math.atan2(v2.y,v2.x);
		ArrayList<Double> parameters = calculatePush(normv2, theta2, orbit_speed, tangent_theta);
		energy[accumulatorIndex] = 0.5 * accumulator.mass * Math.pow(parameters.get(0),2);
		direction[accumulatorIndex] = parameters.get(1);
		prevNumAsteroids = n;
		return;
	    }
	}

	int outerIndex = -1;
	for (int i=0; i<asteroids.length; i++) {
	    if (asteroids[i].id == accumulatorID) {
		outerIndex = i;
	    }
	}

	if (outerIndex == -1) {
	    System.out.println("No valid accumulator asteroid");
	    System.exit(0);
	}
	
	Asteroid outerAsteroid = asteroids[outerIndex];
	double r2 = outerAsteroid.orbit.a;
	double omega2 = Math.sqrt(Orbit.GM / Math.pow(r2,3));
	Point v2 = outerAsteroid.orbit.velocityAt(time - outerAsteroid.epoch);
	double theta2 = Math.atan2(v2.y,v2.x);
	double normv2 = l2norm(v2);	    
	
	int min_index = -1;
	long min_time = 0;
	for (int i=n-1; i>=0; i--) {
	    int innerIndex = asteroids_ordered.get(i).index;
	    if (asteroids[innerIndex].id == accumulatorID) {continue;}
	    Asteroid innerAsteroid = asteroids[innerIndex];
	    if (innerAsteroid.orbit.a != innerAsteroid.orbit.b) {continue;} //if its not a circle, continue
	    double r1 = asteroids_ordered.get(i).getRadius();

	    long t = time;
	    Point v1 = innerAsteroid.orbit.velocityAt(t - innerAsteroid.epoch);
	    double normv1 = l2norm(v1);
	    double theta1 = Math.atan2(v1.y,v1.x);
	    double tH = Math.PI * Math.sqrt( Math.pow(r1+r2,3) / (8*Orbit.GM));
	    double thresh = innerAsteroid.radius() + outerAsteroid.radius();
	    v2 = outerAsteroid.orbit.velocityAt(t - outerAsteroid.epoch);
	    theta2 = Math.atan2(v2.y,v2.x);

	    if ( Math.abs(theta1 + Math.PI - theta2 - tH*omega2) < thresh / r2) {
		if ((collidingAsteroids.containsKey(innerAsteroid.id) || time_limit - time < 50*365) && accumulated_mass < target_mass ){
		    double deltav = Math.sqrt(Orbit.GM / r1) * (Math.sqrt( 2*r2 / (r1+r2)) - 1);
		    double E = 0.5*asteroids[innerIndex].mass * deltav * deltav;
		    double dir = 0;
		    if (r2 < r1) {dir = theta1 + Math.PI;}
		    else {dir = theta1;}

		    if (pathFree(asteroids, innerIndex, outerIndex, E, dir, time)) {
			collidingAsteroids.remove(innerAsteroid.id);
			energy[innerIndex] = E;
			direction[innerIndex] = dir;
			accumulated_mass += innerAsteroid.mass;
			System.out.println("Asteroid " + innerAsteroid.id + " pushed.");
			if (time_limit - time < 50*365) {
			    System.out.println("Running out of time...");
			    System.out.println(collidingAsteroids.size() + " remaining asteroids in queue.");
			}
		    }
		}
	    }
	}
    }
}
