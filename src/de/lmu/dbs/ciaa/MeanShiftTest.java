package de.lmu.dbs.ciaa;

import java.awt.Color;
import java.io.File;

import de.lmu.dbs.ciaa.classifier.MeanShift;
import de.lmu.dbs.ciaa.util.ArrayToImage;
import de.lmu.dbs.ciaa.util.ArrayUtils;
import de.lmu.dbs.ciaa.util.FileIO;

public class MeanShiftTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String forestFile = "testforests/forest4_LR_2_3_dep14_10tone/forestdata";
		
		try {
			//BufferedImage img = ImageIO.read(new File(imageIn));
			//Graphics2D g2d = img.createGraphics();

			FileIO<float[][]> fio = new FileIO<float[][]>();
			float[][] fdata = fio.load(forestFile);
			
		    MeanShift ms = new MeanShift(20);
		    //float[][] fdata2 = MeanShift.gaussianBlur(fdata, 0.01f, 0.2f);
		    
		    ms.process(fdata, 0.2f);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    ArrayUtils.blur(ms.modeWeights, 0);
		    
			//g2d.dispose();
		    ArrayToImage img = new ArrayToImage(fdata.length, fdata[0].length);
		    System.out.println("Max data: " + img.add(fdata, Color.WHITE));
		    System.out.println("Max segmentation: " + img.addClassified(ms.segmentation));
		    System.out.println("Max modes: " + img.add(ms.modeWeights, Color.RED, null, 0));
		    img.save(new File(forestFile + "_meanshifted.png"));
			//ImageIO.write(img, "png", new File(imageIn + "_meanshifted.png"));
			//*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
