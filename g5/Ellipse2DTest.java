package pb.g5;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
public class Ellipse2DTest  extends Frame {  

	public Ellipse2DTest(){
	       
		  
		      super("Java AWT Examples");
		      prepareGUI();
		   

	}
	
	public static void main(String[] args){
		Ellipse2DTest  awtGraphicsDemo = new Ellipse2DTest();  
	      awtGraphicsDemo.setVisible(true);
	   }

	   private void prepareGUI(){
	      setSize(400,400);
	      addWindowListener(new WindowAdapter() {
	         public void windowClosing(WindowEvent windowEvent){
	            System.exit(0);
	         }        
	      }); 
	   }    

	   @Override
	   public void paint(Graphics g) {
	      Ellipse2D shape = new Ellipse2D.Float();
	      shape.setFrame(100, 150, 200,100);
	      Graphics2D g2 = (Graphics2D) g; 
	      g2.draw (shape);
	      Font font = new Font("Serif", Font.PLAIN, 24);
	      g2.setFont(font);
	      g.drawString("Welcome to TutorialsPoint", 50, 70);
	      g2.drawString("Ellipse2D.Oval", 100, 120); 
	   }
}


 