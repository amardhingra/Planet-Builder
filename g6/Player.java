package pb.g6;

import pb.sim.Point;
import pb.sim.Orbit;
import pb.sim.Asteroid;
import pb.sim.InvalidOrbitException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Player implements pb.sim.Player {

    // used to pick asteroid and velocity boost randomly
    private Random random = new Random(0);

    // current time, time limit
    private long time = -1;
    private long time_limit = -1;
    private int QUOTA = 1000;

    // time until next push
    private long time_of_push = 0;

    // number of retries
    private int retries_per_turn = 1;
    private int turns_per_retry = 3;

    // print orbital information
    public void init(Asteroid[] asteroids, long time_limit) {
        if (Orbit.dt() != 24 * 60 * 60)
            throw new IllegalStateException("Time quantum is not a day");
        this.time_limit = time_limit;
    }

    // try to push asteroid
    public void play(
            Asteroid[] asteroids,
            double[] energy,
            double[] direction
    ) {
        if (++time <= time_of_push) return;

        Collision cls = new Collision(asteroids, time, time_limit);
        ArrayList<Push> pushes = new ArrayList<>();

        for (int i = 0; i < asteroids.length; i++) {
            int TRIES = QUOTA / asteroids.length;
            for (int j = 0; j < TRIES; j++) {
                Point v = asteroids[i].orbit.velocityAt(time - asteroids[i].epoch);
                double v1 = Math.sqrt(v.x * v.x + v.y * v.y);
                double v2 = v1 * (random.nextDouble() * Utils.cubicInc(time / 365));
                double d1 = Math.atan2(v.y, v.x);
                double d2 = d1 + (random.nextDouble() - 0.5) * Math.PI * 0.5;
                double E = 0.5 * asteroids[i].mass * v2 * v2;
                Asteroid a1 = null;
                try {
                    a1 = Asteroid.push(asteroids[i], time, E, d2);
                } catch (InvalidOrbitException e) {
                    continue;
                }
                long dt = cls.findCollision(a1, i);
                if (dt != 0 && dt < ((time_limit - time) / (long) asteroids.length)) {
                    pushes.add(new Push(dt, E, d2, i));
                }
            }

        }

        if (pushes.size() != 0) {
            Push pmin = Collections.min(pushes);
            energy[pmin.idx] = pmin.E;
            direction[pmin.idx] = pmin.d;
            time_of_push = time + pmin.dt + 1;
        }
    }
}
