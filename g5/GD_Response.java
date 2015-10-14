package pb.g5;

public class GD_Response {
	GradientDescent gd;
	int pushedIndex;
	public GD_Response(GradientDescent gd, int pushedIndex) {
		super();
		this.gd = gd;
		this.pushedIndex = pushedIndex;
	}
	public GradientDescent getGd() {
		return gd;
	}
	public int getPushedIndex() {
		return pushedIndex;
	}
	
}
