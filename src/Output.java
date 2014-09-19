import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;


public class Output {
	private BufferedWriter resultWriter;
	private BufferedWriter debugWriter;
	private BufferedWriter mappingWriter;
	private BufferedWriter configWriter;
	private Configuration config;
	
	/**
	 * Creation of BufferedWriter which are needed for the output;
	 * @param config Configuration file, which holds the information about the
	 * location where all results are saved
	 */
	public Output(Configuration config) {
		this.config = config;
		
		// create the new folder where all the data goes
		new File("./"+config.resultPostfix).mkdir();
		
		try{
			resultWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/results_"+config.resultPostfix+".csv"),true));
		} catch (Exception e) {System.out.println("Could not create resultWriter.");}
		
		try{
			debugWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/debug_"+config.resultPostfix+".txt"),true));
		} catch (Exception e) {System.out.println("Could not create debugWriter.");}
		
		try{
			mappingWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/mapping_"+config.resultPostfix+".txt"),true));
		} catch (Exception e) {System.out.println("Could not create mappingWriter.");}
	}
	
	
	
	
	
	public void writeToResultFile(String text) {
		try {
			resultWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/results_"+config.resultPostfix+".csv"),true));
			resultWriter.write(text+"\r\n");
			resultWriter.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	
	public void writeToDebugFile(String text) {
		try {
			debugWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/debug_"+config.resultPostfix+".txt"),true));
			debugWriter.write(text+"\r\n");
			debugWriter.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	
	public void writeToMappingFile(String text) {
		try {
			mappingWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/mapping_"+config.resultPostfix+".txt"),true));
			mappingWriter.write(text+"\r\n");
			mappingWriter.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	
	/**
	 * The debug output happens here. Depending on the set verbose level of the
	 * simulator and the priority the messages come in with this method prints
	 * them out or discards them.
	 * @param text text, which should be printed
	 * @param priority has to be less or equal to verbose level to get printed
	 */
	public void consoleOutput(String text, int priority, int tick) {
		if (priority <= config.verboseLevel && config.debugPeriodStart == -1 && config.debugPeriodStop == -1 || (config.debugPeriodStart<=tick && config.debugPeriodStop>=tick && config.debugPeriodVerbose>=priority)) {
			System.out.println(text);
		}
	}
	
	
	public void writeDownSettings() {
		
		try{
			configWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/config_"+config.resultPostfix+".txt"),true));
			configWriter.write("# Config\r\n");
			configWriter.write("Date: "+config.date+"\r\n");
			if (config.simulatorCase == 4 || config.simulatorCase == 5) {
				configWriter.write("Running data from a CSV file.\r\n");
			} else {
				String caseName[] = {"BEST CASE - unpark always just 1 car at a time",
						"WORST CASE - unpark all cars with the same rank at once, always the highest k",
						"RANDOM CASE  - using poisson distribution with certain probabilities for the\r\n          parking duration",
						"ROUND ROBIN TEST CASE - a lot of cars squeezing through the despawn at once"};
				configWriter.write("Running test data from case "+config.simulatorCase+"\r\n         ("+caseName[config.simulatorCase]+").\r\n");
				
			}
			if (config.simulatorCase == 2 || config.simulatorCase == 3 || config.simulatorCase == 4) {
				configWriter.write("Mapping from incoming to outgoing cars where randomized with: "+(config.secureRandom?"secureRandom":"Math.random")+" (config.secureRandom = "+(config.secureRandom?"true":"false")+")\r\n");
				configWriter.write("Seed: "+config.randomSeed+"\r\n");
			}
			configWriter.newLine();
			configWriter.write("Layout information\r\n");
			configWriter.write("A total of "+(config.kHeight*6*config.parkingRows)+" parking spots are available.\r\n");
			configWriter.write("kHeight = "+config.kHeight+"\r\n");
			configWriter.write("parkingRows = "+config.parkingRows+"\r\n");
			configWriter.write("car length = "+config.carSize+"\r\n");
			configWriter.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	
	
	
	
	
	/** GENERATION OF THE IMAGES SHOWING A STATE OF THE MAP **/
	
	

	public void generateImage(KStack[] kstack, Spawn spawn, Crossroad crossroad, int tick) throws Exception{
		int X = 2+config.parkingRows+config.carSize+config.carSize*config.kHeight, Y = (6*config.carSize*config.kHeight)+3;
		int x =0, y = 0, PIX_SIZE = 16;
		BufferedImage bi = new BufferedImage( PIX_SIZE * X, PIX_SIZE * Y, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D g=(Graphics2D)bi.getGraphics();
		String filename =  "tick_";
		if (tick < 100000)
			filename += "0";
		if (tick < 10000)
			filename += "0";
		if (tick < 1000)
			filename += "0";
		if (tick < 100)
			filename += "0";
		if (tick < 10)
			filename += "0";
		filename += tick+".png";
		
		// paint everything white
		for( int i = 0; i < X; i++ ){
            for( int j =0; j < Y; j++ ){
            	g.setColor(Color.WHITE);
            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
            }
		}
		
		// paint unused areas near spawn black
		for (int i = 0; i < config.carSize*config.kHeight; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
	            	g.setColor(Color.BLACK);
	            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
				}
			}
		}
		
		
		
		// paint unused areas near despawn black
		for (int i = config.carSize*config.kHeight+2+config.parkingRows; i < X; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
	            	g.setColor(Color.BLACK);
	            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
				}
			}
		}
		

		
		// paint unused areas near the corners black
		for (int j = 0; j < config.carSize*config.kHeight; j++) {
        	g.setColor(Color.BLACK);
        	g.fillRect((config.carSize*config.kHeight) * PIX_SIZE, j*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((config.carSize*config.kHeight+config.parkingRows+1) * PIX_SIZE, j*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((config.carSize*config.kHeight) * PIX_SIZE, (Y-j-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((config.carSize*config.kHeight+config.parkingRows+1) * PIX_SIZE, (Y-j-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
		}

		
		// paint middle streets gray or red (if blocked)
		Street tempStreet1 = spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		for (int i = 0; i < config.parkingRows+2+config.kHeight*config.carSize+config.carSize; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(i*PIX_SIZE, (Y/2)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			if (tempStreet1.next1 != null) // this should only catch the last case when the pointer already reached despawn
				tempStreet1 = tempStreet1.next1;
		}
		

		// paint left vertical streets gray or red (if blocked)
		tempStreet1 = spawn.next2;
		Street tempStreet2 = spawn.next3;
		for (int i = 1; i < 2*config.kHeight*config.carSize+2; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(config.carSize*config.kHeight*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(config.carSize*config.kHeight*PIX_SIZE, (((Y/2))+i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}
		
		// use the position of the streets and keep painting upper and lower horizontal streets
		for (int i = 0; i < config.parkingRows; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((config.carSize*config.kHeight+1+i)*PIX_SIZE, (((Y/2))-2*config.carSize*config.kHeight-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((config.carSize*config.kHeight+1+i)*PIX_SIZE, (((Y/2))+2*config.carSize*config.kHeight+1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}
		
		// and now the right vertical streets
		for (int i = 2*config.kHeight*config.carSize+1; i > 0; i--) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((config.carSize*config.kHeight+config.parkingRows+1)*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((config.carSize*config.kHeight+config.parkingRows+1)*PIX_SIZE, (((Y/2))+i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}

			
		
		// paint cars in the middle kstacks
		for (int i = 0; i < kstack.length; i++) {
			y = (Y/2);
			x = config.carSize*config.kHeight+1;
			tempStreet1 = spawn.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		
		// paint cars in the top kstacks
		for (int i = 0; i < kstack.length; i++) {
			y = config.carSize*config.kHeight;
			x = config.carSize*config.kHeight+1;
			tempStreet1 = spawn.next2;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		
		// paint cars in the bottom kstacks
		for (int i = 0; i < kstack.length; i++) {
			y = Y-(config.carSize*config.kHeight)-1;
			x = this.config.carSize*this.config.kHeight+1;
			tempStreet1 = spawn.next3;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		// save all the images!!
		g.dispose();
        saveToFile( bi, filename );
	}
	
	
	/**
	 * This method saves an image from state of the parking lot to an assigned
	 * destination.
	 * @param img the image itself
	 * @param file file where the image is supposed to go
	 * @throws IOException something can always go wrong
	 */
	private void saveToFile( BufferedImage img, String file ) throws IOException {
		ImageWriter writer = null;
		@SuppressWarnings("rawtypes")
		java.util.Iterator iter = ImageIO.getImageWritersByFormatName("png");
		if( iter.hasNext() ){
		    writer = (ImageWriter)iter.next();
		}
		String temp = "./"+config.resultPostfix+"/"+file;
		ImageOutputStream ios = ImageIO.createImageOutputStream( new File (temp) );
		writer.setOutput(ios);
		ImageWriteParam param = new JPEGImageWriteParam( java.util.Locale.getDefault() );
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
		param.setCompressionQuality(0.98f);
		writer.write(null, new IIOImage( img, null, null ), param);
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/** DIFFERENT METHODS TO SEE HOW MAP LOOKS LIKE -- FOR DEBUG PURPOSES **/
	
	
	
	
	
	
	
	
	public void printMidLane(Spawn spawn, int tick) {
		config.output.consoleOutput("==========",2,tick);
		config.output.consoleOutput("Middle Lane:",2,tick);
		Street tempStreet1 = spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		do {
			config.output.consoleOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2,tick);
			tempStreet1 = tempStreet1.next1;
		} while(tempStreet1 != null);
		config.output.consoleOutput("==========",2,tick);
	}
	
	public void printDespawn(Crossroad crossroad, Despawn despawn, int tick) {
		config.output.consoleOutput("==========",2,tick);
		config.output.consoleOutput("Exit of the parking Lot:",2,tick);
		Street tempStreet1 = crossroad;
		do {
			config.output.consoleOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2,tick);
			tempStreet1 = tempStreet1.next1;
		} while (tempStreet1 != despawn);
		config.output.consoleOutput("Street: "+despawn+", Car: "+despawn.car,2,tick);
		config.output.consoleOutput("==========",2,tick);
	}
	
	public void printStack(int stack, KStack[] kstack, int tick) {
		config.output.consoleOutput("==========",2,tick);
		config.output.consoleOutput("Stack "+stack+" (watermark: "+kstack[stack].watermark+"):",2,tick);
		Street tempStreet1 = kstack[stack];
		config.output.consoleOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", lockedForUnparking: "+((KStack)tempStreet1).lockedForUnparking+", lockedForParking: "+((KStack)tempStreet1).lockedForParking+", blocking kstack: "+tempStreet1.blockingKStack,2,tick);
		for (int i=0; i<(config.kHeight*config.carSize)-1; i++) {
			tempStreet1 = tempStreet1.next1;
			config.output.consoleOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2,tick);
		}
		config.output.consoleOutput("==========",2,tick);
	}
}