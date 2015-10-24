import java.util.*;

class Orbits {

	private static class Point {

		public final double x;
		public final double y;

		public Point(double x, double y)
		{
			this.x = x;
			this.y = y;
		}
	}

	private static double[] random_orbits(int n,
	                                      double density,
	                                      double max_mass,
	                                      double min_orbit,
	                                      double max_orbit)
	{
		double volume = max_mass / density;
		double radius = Math.cbrt(0.75 * volume / Math.PI);
		double[] orbit = new double [n];
		for (int i = 0 ; i != n ; ++i) {
			orbit[i] = Math.random() * (max_orbit - min_orbit) + min_orbit;
			for (int j = 0 ; j != i ; ++j) {
				double distance = Math.abs(orbit[i] - orbit[j]);
				if (distance <= radius * 2.0) {
					i--;
					break;
				}
			}
		}
		return orbit;
	}

	private static double[] random_masses(int[] count,
	                                      double[] mass_category)
	{
		int n = 0;
		for (int i = 0 ; i != count.length ; ++i)
			n += count[i];
		double[] mass = new double [n];
		Random gen = new Random();
		for (int i = 0 ; i != n ; ++i) {
			int c = gen.nextInt(count.length);
			if (count[c] == 0) i--;
			else {
				double r = 1.0 + Math.random() * 0.002 - 0.001;
				count[c]--;
				mass[i] = mass_category[c] * r;
			}
		}
		return mass;
	}

	private static Point[] random_phases(int n,
	                                     double orbit,
	                                     double density,
	                                     double max_mass)
	{
		double volume = max_mass / density;
		double radius = Math.cbrt(0.75 * volume / Math.PI);
		Point[] pos = new Point [n];
		for (int i = 0 ; i != n ; ++i) {
			double angle = Math.random() * Math.PI * 2.0;
			pos[i] = new Point(orbit * Math.cos(angle),
			                   orbit * Math.sin(angle));
			for (int j = 0 ; j != i ; ++j) {
				double dx = pos[i].x - pos[j].x;
				double dy = pos[i].y - pos[j].y;
				if (Math.hypot(dx, dy) <= radius * 2.0) {
					i--;
					break;
				}
			}
		}
		return pos;
	}

	public static void main(String[] args)
	{
		if (args.length != 5) {
			System.err.println("Need 5 arguments");
			System.exit(1);
		}
		double density = 1410.0;
		double avg_mass = 1.98855e30;
		int orbits = Integer.parseInt(args[0]);
		int planets_per_orbit = Integer.parseInt(args[1]);
		double min_orbit = Double.parseDouble(args[2]);
		double max_orbit = Double.parseDouble(args[3]);
		int half_mass = Integer.parseInt(args[4]);
		int planets = orbits * planets_per_orbit;
		if (half_mass * 2 > planets)
			throw new IllegalArgumentException("Planet mismatch");
		if (min_orbit > max_orbit)
			throw new IllegalArgumentException("Invalid orbit range");
		int[] planets_per_mass = new int [] {half_mass, planets - 2 * half_mass, half_mass};
		double[] mass_categories = new double [] {avg_mass * 0.5, avg_mass, avg_mass * 1.5};
		double[] orbit = random_orbits(orbits, density, avg_mass * 1.55, min_orbit, max_orbit);
		double[] mass = random_masses(planets_per_mass, mass_categories);
		for (int i = 0 ; i != orbits ; ++i) {
			Point[] pos = random_phases(planets_per_orbit, orbit[i], density, avg_mass * 1.55);
			for (int j = 0 ; j != planets_per_orbit ; ++j) {
				int k = i * planets_per_orbit + j;
				System.out.println(pos[j].x + ", " + pos[j].y + ", " + mass[k]);
			}
		}
	}
}
