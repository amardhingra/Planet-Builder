package pb.g6;

public class Push implements Comparable<Push> {
    public long dt;
    public double E;
    public double d;
    public int idx;

    public Push(long dt, double E, double d, int idx) {
        this.dt = dt;
        this.E = E;
        this.d = d;
        this.idx = idx;
    }

    @Override
    public int compareTo(Push o) {
        double e = this.E - o.E;
        if (e < 0) {
            return -1;
        } else if (e > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
