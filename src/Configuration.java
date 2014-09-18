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
	
	
	
	/** CONFIGURATION FOR MAIN **/
	
	/**
	 * This variable sets the operation mode of the simulator. It can be set to
	 * the following different modes: 
	 * 0: best case (unpark always just 1 at a time)
	 * 1: worst case (unpark all cars with the same rank at once, starting with
	 *    the highest)
	 * 2: random case using poisson distribution with certain probabilities for
	 *    the parking duration
	 * 3: round robin test case
	 * 4: This setting is for csv files where only aggregated incoming cars and
	 *    outgoing cars are assigned to specific points in time (ticks). E.g.:
	 *    <tick>,<cars entering the parking lot>,<cars exiting the parking lot>
	 * 5: TODO use a plain CSV
	 */
	public final int simulatorCase = 0;
	
	
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
	public final boolean secureRandom = true;
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
	 * Here the layout of the parking lot is defined.
	 * The number parking spaces has to be a multiple of 6 times the height of
	 * the stacks because the number of the stacks is calculated.
	 * The size of cars does not directly affect the layout. Only the distances
	 * which has to be driven by cars inside the parking lot. That means
	 * everything gets bigger.
	 */
	public final int noOfParkingSpaces = 1440;
	public final int kHeight = 1;
	public final int carSize = 2;
	public final int parkingRows = (noOfParkingSpaces/kHeight)/6;
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/** CONFIGURATION FOR SIMULATOR **/
	
	/**
	 * This variable sets the level of text output coming from the simulator.
	 * If set to 0 no output is generated. 1 allows the output of despawn
	 * events. 2 and above prints every debug message from the whole simulation
	 * process in the command line interface.
	 * 
	 * This variable does -NOT- affect the logging of despawn events and the
	 * corresponding information of every car into the csv inside the folder
	 * the simulator creates prior to simulation.
	 */
	public final int verboseLevel = 0;
	
	/**
	 * This controls the visual output of the simulator. 0 means no visual
	 * output, 1 means an image every tick, 2 means an image every other tick,
	 * etc.
	 */
	public final int visualOutput = 0;
	
	/**
	 * If this boolean is set to true every KStack can unpark at any time given
	 * the needed space (tiles) is free. The simulator does care for the order
	 * unparking orders came in. As soon the space is free the KStack will
	 * unpark. This can lead to heavy delays for individual unparking events
	 * but might increase mean performance.
	 * If it is set to false the simulator will follow the order in which cars
	 * wanted to unpark and makes unparking events with lower priority wait.
	 */
	public final boolean chaoticUnparking = false;
	
	
	
	
	
	
	
	/** DEBUG CONFIGURATION FOR SIMULATOR **/
	
	/**
	 * This is variable can be set to a value between -1 and MAXINT. All cars
	 * will then be assigned to this stack. If the number is higher than the
	 * number of stacks the cars will be assigned to the stack with the highest
	 * id. So set to 15 with only 12 stacks total the simulator will send all
	 * cars to stack 11 (counting starts at 0). To disable it set it to -1. 
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
	 */
	public final int debugPeriodStart = -1;
	public final int debugPeriodStop = -1;
	public final boolean debugBreakAfter = false; 
	public final int debugPeriodVisual = 0;
	public final int debugPersionVerbose = 0;
	
	
	
	
	
	/** CONFIGURATION FOR CAR **/
	
	// for verboseLevel please see section CONFIGURATION FOR SIMULATOR

}
