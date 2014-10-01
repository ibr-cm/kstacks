import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

/**
 * Confiugration file for the VCharge Simulator
 * 
 * 
 * 
 * @author Johannes van Balen
 * Sept. 2014
 *
 */

public class Configuration {
	
	
	/** CONFIGURATION FOR OUTPUT **/
	
	public Output output;
	
	/**
	 * This variable sets the level of text output coming from the simulator.
	 * If set to 0 no output is generated. 1 allows the output of despawn
	 * events. 2 and above prints every debug message from the whole simulation
	 * process in the command line interface.
	 * 
	 * This variable does -NOT- affect the logging of despawn events and the
	 * corresponding information of every car into the csv inside the folder
	 * the simulator creates prior to simulation.
	 * 
	 * DEFAULT: public final int verboseLevel = 0;
	 */
	public final int verboseLevel = 0;
	
	
	/**
	 * If this variable is set to TRUE there will be no files as output. This
	 * is only meant for debugging where all output will be printed into the
	 * console.
	 */
	public final boolean prohibitFileOutput = true;
	
	
	/** CONFIGURATION FOR MAIN **/
	
	/**
	 * This variable sets the operation mode of the simulator. It can be set to
	 * the following different modes: 
	 * 0: best case (unpark always just 1 at a time)
	 * 1: worst case (unpark all cars with the same rank at once, starting with
	 *    the highest)
	 * 2: random case using poisson distribution with certain probabilities for
	 *    the parking duration - PLEASE SEE ADDITIONAL OPTIONS BELOW
	 * 3: round robin test case
	 * 4: This setting is for csv files where only aggregated incoming cars and
	 *    outgoing cars are assigned to specific points in time (ticks). E.g.:
	 *    <tick>,<cars entering the parking lot>,<cars exiting the parking lot>
	 * 5: use a plain CSV
	 */
	public final int simulatorCase = 5;
	
	public final int assortByRandom = -1;
	public final int excludeFromResults = 100000;
	
	/**
	 * When a csv file of a specific form is used this setting is important.
	 * Some parking lots do not disclose data with the parking duration of
	 * individual cars but rather provide data where the number of incoming and
	 * unparking cars are summed up and aggregated over a certain period of
	 * time. This poses the problem that there is no information about how long
	 * each car stayed at the parking lot.
	 * So this simulator assignes each incoming car a random point in time,
	 * which is at least 15 minutes later, at which the par will unpark again.
	 * In order to do this two different algorithms can be used. Either the
	 * regular Math.random or SecureRandom.nextInt, which will be used to
	 * calculate a random number.
	 * The seed for both algorithms can also be changed by changing the
	 * variable randomSeed. When using Math.random (secureRandom = false) the
	 * results are the same per run (on same data) when using the same seed.
	 */
//	public final boolean secureRandom = true;
	public final int randomSeed = 1001;
	
	
	/**
	 * In case a csv file is the input this names the file. The file has to
	 * look like this:
	 * 
	 * 2511,15711
	 * 
	 * The first number is the time (in ticks) when the car enters the parking
	 * lot and the second is the point in time when the car is called back.
	 * The file can contain as many lines as needed. Each line corresponds one
	 * car. Lines with a smaller second number than the first will be omitted.
	 */
	public final String inputFileName = "adjusted.csv";
	
	
	/**
	 * This sets the way the stacks are assigned to streets within the layout.
	 * There are several different ways to assign stacks to streets:
	 * 0: simple layout - Starting near the entrance and assigning the first
	 *    third of stacks to the streets in the middle lane. The next third is
	 *    assigned to streets which are reachable by using next2 from spawn.
	 *    The last third is assigned to the remaining streets. The stack with
	 *    the lowest id is closest to the spawn. The id does also not always
	 *    give information about the position in the layout. Stack 17 might be
	 *    closer to the spawn or despawn than stack 42.
	 * 1: mirrored layout - This layout is basically a mirrored version of the
	 *    simple layout. Since the simulator fills the stacks starting with
	 *    stack 0. Now the cars park closer to the despawn but still the id
	 *    does not necessarily informs about the position in the layout.
	 * 2: radius layout - This layout ensures that stacks with lower ids are
	 *    closer to the despawn (to minimize the difference between the time
	 *    the cars get ordered back and the time they actually exit the parking
	 *    lot). So stack 17 might be closer to despawn than stack 42 but it is
	 *    certainly -NOT- further away.
	 *    
	 *    DEFAULT: public final int parkingLotLayout = 2;
	 */
	public final int parkingLotLayout = 2;
	
	
	/**
	 * Here the layout of the parking lot is defined.
	 * The number parking spaces has to be a multiple of 6 times the height of
	 * the stacks because the number of the stacks is calculated.
	 * The size of cars does not directly affect the layout. Only the distances
	 * which has to be driven by cars inside the parking lot. That means
	 * everything gets bigger.
	 */
	public final int noOfParkingSpaces = 1440;
	public final int kHeight = 4;
	public final int carSize = 2;
	public final int parkingRows = (noOfParkingSpaces/kHeight)/6; // -DO NOT TOUCH-
	
	
	/**
	 * Additional Options for the random case:
	 * The minimum parking duration is necessary to make cars stay in the
	 * parking lot for a certain time at least. The algorithm using creating
	 * the parking time generates a new parking duration until this condition
	 * is met. To avoid values that are not possible to comply with this is
	 * limited to a maximum of 60 minutes. That means all parking durations
	 * would be a full hour.
	 * 
	 * The default number of cars used for the random case are calculated as
	 * follows: In a realistic parking lot with 1250 spots a total number of
	 * 3928 cars were used in the time for 10 hours (8am - 6pm). Since there 
	 * are a different number of spots available here this has to be adjusted.
	 */
	public final int minParkDuration = 15;
	
	
	/**
	 * -DO NOT TOUCH-
	 * 
	 * This concatenates the string, which is used to make each simulation
	 * unique and put it in the right folder.
	 */
	public final String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	@SuppressWarnings("all")
	public final String resultPostfix = ((simulatorCase == 4 || simulatorCase == 5)?"csv-input_":"testCase_")+simulatorCase+"_"+date;
	
	
	/**
	 * -DO NOT TOUCH-
	 */
	public SecureRandom secRandom;
	public BufferedImage carFrame0, carFrame90, carFrame270, carColor0, carColor90, carColor270;
	public int PIX_SIZE = 16;
	public BufferedImage background;
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/** CONFIGURATION FOR SIMULATOR **/
	
	// for verboseLevel please see section CONFIGURATION FOR OUTPUT
	
	/**
	 * This controls the visual output of the simulator. 0 means no visual
	 * output, 1 means an image every tick, 2 means an image every other tick,
	 * etc.
	 * 
	 * DEFAULT: public final int visualOutput = 0;
	 */
	public final int visualOutput = 1;
	
	/**
	 * If this boolean is set to true every KStack can unpark at any time given
	 * the needed space (tiles) is free. The simulator does care for the order
	 * unparking orders came in. As soon the space is free the KStack will
	 * unpark. This can lead to heavy delays for individual unparking events
	 * but might increase mean performance.
	 * If it is set to false the simulator will follow the order in which cars
	 * wanted to unpark and makes unparking events with lower priority wait.
	 * 
	 * DEFAULT: public final boolean chaoticUnparking = false;
	 */
	public final boolean chaoticUnparking = true;
	
	
	
	
	
	
	
	/** DEBUG CONFIGURATION FOR SIMULATOR **/
	
	/**
	 * This is variable can be set to a value between -1 and MAXINT. All cars
	 * will then be assigned to this stack. If the number is higher than the
	 * number of stacks the cars will be assigned to the stack with the highest
	 * id. So set to 15 with only 12 stacks total the simulator will send all
	 * cars to stack 11 (counting starts at 0). To disable it set it to -1 or
	 * to an even lower value. 
	 * 
	 * DEFAULT: public final int debugSmallestStack = -1;
	 */
	public final int debugSmallestStack = -1;
	
	/**
	 * The next five variables are used to simulate only a small part of time
	 * with images and console output.
	 * If the first two integer are set to -1 and the boolean is set to false
	 * this debug option is disabled.
	 * E.g. To simulate a situation and only receive output (visual and on
	 * the command line interface) between tick 100 and 200 and abort the
	 * simulation afterwards the user has to set:
	 * debugPeriodStart = 100, debugPeriodStop = 200, debugBreakAfter = true
	 * 
	 * If debugBreakAfter is left at false the simulation continues after the
	 * interval with the settings for verboseLevel and visualOutput, which are
	 * set above in the section CONFIGURATION FOR SIMULATOR.
	 * 
	 * During the set period the the last two integer set the output the
	 * simulator is supposed to give. For further information please see the
	 * corresponding explanation in section CONFIGURATION FOR SIMULATOR.
	 * 
	 * DEFAULT: public final int debugPeriodStart = -1;
	 * DEFAULT: public final int debugPeriodStop = -1;
	 * DEFAULT: public final boolean debugBreakAfter = false; 
	 * DEFAULT: public final int debugPeriodVisual = 0;
	 * DEFAULT: public final int debugPersionVerbose = 0;
	 */
	public final int debugPeriodStart = 22000;
	public final int debugPeriodStop = 22500;
	public final boolean debugBreakAfter = true; 
	public final int debugPeriodVisual = 1;
	public final int debugPeriodVerbose = 0;
	
	
	
	
	
	/** CONFIGURATION FOR CAR **/
	
	// for verboseLevel please see section CONFIGURATION FOR OUTPUT
	
	/**
	 * If this boolean is set the car will add one stop to its counter for
	 * starts and stops. It also sets the car to isMoving = false, when the
	 * direction of driving is changed. E.g. when driving backwards in order to
	 * unpark and then drive forward in order to reach despawn.
	 * 
	 * DEFAULT: public final boolean stopAtUnparking = true;
	 */
	public final boolean stopAtUnparking = true;
	
	public int differentCars = 0;
	public BufferedImage[] cars0, cars90, cars270;
	public BufferedImage[][] allCars;

	
	public Configuration() {
		this.secRandom = new SecureRandom();
		this.output = new Output(this);
		
		while(check("./images/cars/car_"+(differentCars+1)+".png"))
			differentCars++;
		differentCars++;
		
		try {
			
			// creation of all cars with their individual orientation
			cars0 = new BufferedImage[differentCars];
			cars90 = new BufferedImage[differentCars];
			cars270 = new BufferedImage[differentCars];
			allCars = new BufferedImage[differentCars][3];
			
			AffineTransform at;
			AffineTransformOp scaleOp;
			
			for (int i = 0; i < differentCars; i++) {
				cars0[i] = ImageIO.read(new File("./images/cars/car_"+i+".png"));

				
				at = new AffineTransform();
				at.translate(0, PIX_SIZE);
				at.rotate(0.5*Math.PI,PIX_SIZE,0);
				scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
				cars270[i] = new BufferedImage(PIX_SIZE, PIX_SIZE*2, BufferedImage.TYPE_INT_ARGB);
				scaleOp.filter(cars0[i],cars270[i]);
				
				at = new AffineTransform();
				at.translate(-PIX_SIZE, PIX_SIZE);
				at.rotate(-0.5*Math.PI,PIX_SIZE,0);
				scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
				cars90[i] = new BufferedImage(PIX_SIZE, PIX_SIZE*2, BufferedImage.TYPE_INT_ARGB);
				scaleOp.filter(cars0[i], cars90[i]);
				
				allCars[i][0]  = new BufferedImage(PIX_SIZE*2, PIX_SIZE, BufferedImage.TYPE_INT_ARGB);
				allCars[i][1]  = new BufferedImage(PIX_SIZE, PIX_SIZE*2, BufferedImage.TYPE_INT_ARGB);
				allCars[i][2]  = new BufferedImage(PIX_SIZE, PIX_SIZE*2, BufferedImage.TYPE_INT_ARGB);
				
				allCars[i][0] = cars0[i]; // right
				allCars[i][1] = cars90[i]; // up
				allCars[i][2] = cars270[i]; // down
			}
			
			
			
		} catch (Exception e) {e.printStackTrace();}
	}
	
	private boolean check(String path) {
		try {
			ImageIO.read(new File(path));
			return true;
		} catch (Exception e) {}
		return false;
	}
	
}
