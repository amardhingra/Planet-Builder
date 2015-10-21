package pb.g5;

public class Push {
	public double direction;
	public double energy;
	public double days;

	public Push(double energy, double direction) {
		super();
		this.direction = direction;
		this.energy = energy;
		this.days = 0;
	}

	public Push(double energy, double direction, double days){
		super();
		this.direction = direction;
		this.energy = energy;
		this.days = days;
	}
	
	public String toString() {
		return "direction: "+direction+"\tenergy"+energy;
	}
}
