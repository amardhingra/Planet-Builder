package pb.g0;

public class Push {
	public double direction;
	public double energy;

	public Push(double energy, double direction) {
		super();
		this.direction = direction;
		this.energy = energy;
	}
	
	public String toString() {
		return "direction: "+direction+"\tenergy"+energy;
	}
}
