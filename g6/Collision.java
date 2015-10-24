package pb.g6;

import pb.sim.Point;
import pb.sim.Asteroid;

import java.util.ArrayList;
import java.util.Collections;

public class Collision {
    public Asteroid[] asteroids;
    long time = 0;
    long time_limit = 0;

    public Collision(Asteroid[] asteroids, long time, long time_limit) {
        this.asteroids = asteroids;
        this.time = time;
        this.time_limit = time_limit;
    }

    public Point[] getFoci(double a, double b, double A, double h, double k) {
        double c = 0.0;
        if (a == b) {
            return new Point[]{new Point(c, c)};
        } else if (a > b) {
            c = Math.sqrt(Math.pow(a, 2) - Math.pow(b, 2));
            Point f1 = new Point(c, 0.0);
            Point f2 = new Point(-c, 0.0);

            f1 = new Point(f1.x * Math.cos(A) - f1.y * Math.sin(A) + h, f1.x * Math.sin(A) + f1.y * Math.cos(A) + k);
            f2 = new Point(f2.x * Math.cos(A) - f2.y * Math.sin(A) + h, f2.x * Math.sin(A) + f2.y * Math.cos(A) + k);

            return new Point[]{f1, f2};
        } else {
            c = Math.sqrt(Math.pow(b, 2) - Math.pow(a, 2));
            Point f1 = new Point(0.0, c);
            Point f2 = new Point(0.0, -c);

            f1 = new Point(f1.x * Math.cos(A) - f1.y * Math.sin(A) + h, f1.x * Math.sin(A) + f1.y * Math.cos(A) + k);
            f2 = new Point(f2.x * Math.cos(A) - f2.y * Math.sin(A) + h, f2.x * Math.sin(A) + f2.y * Math.cos(A) + k);

            return new Point[]{f1, f2};
        }
    }

    public Point findPoint(double a, double b, double A, double h, double k, double t) {
        t = (t * Math.PI) / 180;
        return new Point(a * Math.cos(t) * Math.cos(A) - b * Math.sin(t) * Math.sin(A) + h, a * Math.cos(t) * Math.sin(A) + b * Math.sin(t) * Math.cos(A) + k);
    }

    public ArrayList<ArrayList<Point>> findIntersection(Asteroid a, int ignore_idx) {
        ArrayList<ArrayList<Point>> intersects = new ArrayList<>(asteroids.length);

        Ellipse[] ellipses = new Ellipse[asteroids.length];

        double a1 = a.orbit.a;
        double b1 = a.orbit.b;
        double A1 = a.orbit.A;
        Point c1 = a.orbit.center();

        for (int i = 0; i < asteroids.length; i++) {
            ArrayList<Point> inner = new ArrayList<>();
            intersects.add(inner);

            double a2 = asteroids[i].orbit.a;
            double b2 = asteroids[i].orbit.b;
            double A2 = asteroids[i].orbit.A;
            Point c2 = asteroids[i].orbit.center();
            Point[] f = getFoci(a2, b2, A2, c2.x, c2.y);

            ellipses[i] = new Ellipse(a2, b2, A2, c2.x, c2.y, f);
        }

        Point p = null;
        for (double angle = 0; angle < 360; angle += 0.1d) {
            p = findPoint(a1, b1, A1, c1.x, c1.y, angle);
            for (int i = 0; i < asteroids.length; i++) {
                if (i == ignore_idx) {
                    continue;
                }

                double sum = 0;
                for (Point f : ellipses[i].foci) {
                    sum += Point.distance(f, p);
                }

                double radius = asteroids[i].radius();
                if (ellipses[i].foci.length == 0) {
                    throw new Error();
                } else if (ellipses[i].foci.length == 1) {
                    if (sum > ellipses[i].a - (radius/16) && sum < ellipses[i].a + (radius/16)) {
                        intersects.get(i).add(p);
                    }
                } else {
                    if (sum > 2 * ellipses[i].a - (radius/16) && sum < 2 * ellipses[i].a + (radius/16)) {
                        intersects.get(i).add(p);
                    }
                }
            }
        }

        return intersects;
    }

    public long findCollision(Asteroid a, int ignore_idx) {
        ArrayList<ArrayList<Point>> intersects = findIntersection(a, ignore_idx);
        ArrayList<Long> collisions = new ArrayList<>();

        for (int i = 0; i < intersects.size(); i++) {
            if (i == ignore_idx) continue;

            ArrayList<Long> bicols = new ArrayList<>();

            ArrayList<Long> dts_f = timeAt(a, intersects.get(i));
            long period_f = a.orbit.period();
            ArrayList<Long> dts_g = timeAt(asteroids[i], intersects.get(i));
            long period_g = asteroids[i].orbit.period();

            for (int j = 0; j < intersects.get(i).size(); j++) {
                long f = 0;
                long time_f = dts_f.get(j) + f * period_f;

                while (time + time_f < time_limit) {
                    long g = 0;
                    long time_g = dts_g.get(j) + g * period_g;

                    while (time_g < time_f) {
                        g++;
                        time_g = dts_g.get(j) + g * period_g;
                    }

                    if (time_g == time_f) {
                        bicols.add(time_g);
                        break;
                    }

                    f++;
                    time_f = dts_f.get(j) + f * period_f;
                }
            }

            if (bicols.size() != 0) {
                collisions.add(Collections.min(bicols));
            }
        }
        if (collisions.size() != 0) {
            return Collections.min(collisions);
        } else {
            return 0;
        }
    }

    public ArrayList<Long> timeAt(Asteroid a, ArrayList<Point> pts) {
        ArrayList<Long> dts = new ArrayList<>(pts.size());

        for (Point p : pts) {
            long t0 = 0;
            Double dist = Point.distance(p, a.orbit.positionAt(time + t0 - a.epoch));
            long step = a.orbit.period() / 4;
            while (dist > a.radius()) {
                Double[] trials = new Double[2];
                trials[0] = Point.distance(p, a.orbit.positionAt(time + (t0 + step) - a.epoch));
                trials[1] = Point.distance(p, a.orbit.positionAt(time + (t0 - step) - a.epoch));
                if (trials[0] <= trials[1]) {
                    if (trials[0] > dist) {
                        if (step == 1) {
                            break;
                        }
                        long tmp = step / 2;
                        step = tmp < 1 ? 1 : tmp;
                        continue;
                    }

                    t0 += step;
                    dist = trials[0];
                } else {
                    if (trials[1] > dist) {
                        if (step == 1) {
                            break;
                        }
                        long tmp = step / 2;
                        step = tmp < 1 ? 1 : tmp;
                        continue;
                    }

                    t0 -= step;
                    dist = trials[1];
                }

            }
            if (t0 < 0) {
                t0 = a.orbit.period() + t0;
            }
            dts.add(t0);
        }
        return dts;
    }
}

