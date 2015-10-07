package pb.g0;

public class Push {
	public double direction;
	public double energy;

	public Push(double direction, double energy) {
		super();
		this.direction = direction;
		this.energy = energy;
	}
	
	public String toString() {
		return "direction: "+direction+"\tenergy"+energy;
	}
}
