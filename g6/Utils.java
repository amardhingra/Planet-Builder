package pb.g6;

public class Utils {
    public static double cubicInc(long time) {
        return (3.0/ 10000000000.0) * Math.pow(time, 3) + 0.01;
    }
}
