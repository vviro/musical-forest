package de.lmu.dbs.ciaa.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * Class for saving spectral data to a PNG image on disk.
 * 
 * @author Thomas Weber
 *
 */
public class SpectrumToImage {

	private int width;
	private int height;
	private int yspread;
	
	private Graphics2D g2d;
	private BufferedImage rendImage;

	/**
	 * Create an object to compose one PNG.
	 * 
	 * @param file
	 * @param width
	 * @param height
	 * @param yspread scale factor for y axis
	 */
	public SpectrumToImage(int width, int height, int yspread) {
		this.width = width;
		this.height = height;
		this.yspread = yspread;

	    rendImage = new BufferedImage(width, height*yspread, BufferedImage.TYPE_INT_RGB);
	    g2d = rendImage.createGraphics();
	}
	
	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public float add(final float[][] data, final Color color) throws Exception {
		return add(data, color, null, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public float add(final float[][] data, final Color color, final Scale scale) throws Exception {
		return add(data, color, scale, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @param threshold below or equal to this no value will be drawn. Use a negative value if all values should be drawn, i.e. -1
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public float add(final float[][] data, final Color color, final Scale scale, final double threshold) throws Exception {
	    // Get maximum
		float max = Float.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    // Draw image
	    for(int i=0; i<width; i++) {
	    	for(int j=0; j<height; j++) {
	    		if (data[i][j] < 0) throw new Exception("Negative value detected: data[" + i + "][" + j + "] = " + data[i][j]);
    			double norm = (scale == null) ? data[i][j]/max : scale.apply(data[i][j]/max);
    			if (norm <= threshold) continue;
	    		if (color == null) {
	    			g2d.setColor(getColor(norm));
	    		} else {
	    			g2d.setColor(new Color((int)(color.getRed()*norm), (int)(color.getGreen()*norm), (int)(color.getBlue()*norm)));
	    		}
			    g2d.drawLine(i,height*yspread-j*yspread,i,height*yspread-j*yspread-yspread+1);
	    	}
	    }
	    return max;
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public double add(final double[][] data, final Color color) throws Exception {
		return add(data, color, null, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public double add(final double[][] data, final Color color, final Scale scale) throws Exception {
		return add(data, color, scale, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @param threshold below or equal to this no value will be drawn. Use a negative value if all values should be drawn, i.e. -1
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public double add(final double[][] data, final Color color, final Scale scale, final double threshold) throws Exception {
	    // Get maximum
	    double max = Double.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    // Draw image
	    for(int i=0; i<width; i++) {
	    	for(int j=0; j<height; j++) {
	    		if (data[i][j] < 0) throw new Exception("Negative value detected: data[" + i + "][" + j + "] = " + data[i][j]);
    			double norm = (scale == null) ? data[i][j]/max : scale.apply(data[i][j]/max);
    			if (norm <= threshold) continue;
	    		if (color == null) {
	    			g2d.setColor(getColor(norm));
	    		} else {
	    			g2d.setColor(new Color((int)(color.getRed()*norm), (int)(color.getGreen()*norm), (int)(color.getBlue()*norm)));
	    		}
			    g2d.drawLine(i,height*yspread-j*yspread,i,height*yspread-j*yspread-yspread+1);
	    	}
	    }
	    return max;
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public long add(final long[][] data, final Color color) throws Exception {
		return add(data, color, null, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public long add(final long[][] data, final Color color, final Scale scale) throws Exception {
		return add(data, color, scale, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @param threshold below or equal to this no value will be drawn. Use a negative value if all values should be drawn, i.e. -1
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public long add(final long[][] data, final Color color, final Scale scale, final double threshold) throws Exception {
	    // Get maximum
		long max = Long.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    // Draw image
	    for(int i=0; i<width; i++) {
	    	for(int j=0; j<height; j++) {
	    	    if (data[i][j] < 0) throw new Exception("Negative value detected: data[" + i + "][" + j + "] = " + data[i][j]);
    			double norm = (scale == null) ? ((double)data[i][j]/max) : scale.apply((double)data[i][j]/max);
    			if (norm <= threshold) continue;
	    		if (color == null) {
	    			g2d.setColor(getColor(norm));
	    		} else {
	    			g2d.setColor(new Color((int)(color.getRed()*norm), (int)(color.getGreen()*norm), (int)(color.getBlue()*norm)));
	    		}
			    g2d.drawLine(i,height*yspread-j*yspread,i,height*yspread-j*yspread-yspread+1);
	    	}
	    }
	    return max;
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public int add(final int[][] data, final Color color) throws Exception {
		return add(data, color, null, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public int add(final int[][] data, final Color color, final Scale scale) throws Exception {
		return add(data, color, scale, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @param threshold below or equal to this no value will be drawn. Use a negative value if all values should be drawn, i.e. -1
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public int add(final int[][] data, final Color color, final Scale scale, final double threshold) throws Exception {
	    // Get maximum
	    int max = Integer.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    // Draw image
	    for(int i=0; i<width; i++) {
	    	for(int j=0; j<height; j++) {
	    		if (data[i][j] < 0) throw new Exception("Negative value detected: data[" + i + "][" + j + "] = " + data[i][j]);
    			double norm = (scale == null) ? (double)data[i][j]/max : scale.apply((double)data[i][j]/max);
    			if (norm <= threshold) continue;
	    		if (color == null) {
	    			g2d.setColor(getColor(norm));
	    		} else {
	    			g2d.setColor(new Color((int)(color.getRed()*norm), (int)(color.getGreen()*norm), (int)(color.getBlue()*norm)));
	    		}
			    g2d.drawLine(i,height*yspread-j*yspread,i,height*yspread-j*yspread-yspread+1);
	    	}
	    }
	    return max;
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public byte add(final byte[][] data, final Color color) throws Exception {
		return add(data, color, null, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public byte add(final byte[][] data, final Color color, final Scale scale) throws Exception {
		return add(data, color, scale, -1);
	}

	/**
	 * Adds a data array to PNG to visualize it. Values in data have to be positive, and are 
	 * drawn normalized, meaning that the highest value always gets the highest color representation.
	 * 
	 * @param data
	 * @param color if not null, this color is scaled over black background. If null, a special HSB spectrum is used by default.
	 * @param scale a scaling function object or null if no scaling is wanted
	 * @param threshold below or equal to this no value will be drawn. Use a negative value if all values should be drawn, i.e. -1
	 * @return the maximum sample value
	 * @throws Exception 
	 * @throws IOException
	 */
	public byte add(final byte[][] data, final Color color, final Scale scale, final double threshold) throws Exception {
	    // Get maximum
	    byte max = Byte.MIN_VALUE;
	    for(int i=0; i<data.length; i++) {
		    for(int j=0; j<data[i].length; j++) {
		    	if (data[i][j] > max) max = data[i][j];
		    }
		}
	    // Draw image
	    for(int i=0; i<width; i++) {
	    	for(int j=0; j<height; j++) {
	    	    if (data[i][j] < 0) throw new Exception("Negative value detected: data[" + i + "][" + j + "] = " + data[i][j]);
    			double norm = (scale == null) ? ((double)data[i][j]/max) : scale.apply((double)data[i][j]/max);
    			if (norm <= threshold) continue;
	    		if (color == null) {
	    			g2d.setColor(getColor(norm));
	    		} else {
	    			g2d.setColor(new Color((int)(color.getRed()*norm), (int)(color.getGreen()*norm), (int)(color.getBlue()*norm)));
	    		}
			    g2d.drawLine(i,height*yspread-j*yspread,i,height*yspread-j*yspread-yspread+1);
	    	}
	    }
	    return max;
	}

	/**
	 * Saves the PNG file to disk.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void save(File file) throws IOException {
	    g2d.dispose();
        // Save as PNG
        ImageIO.write(rendImage, "png", file);
	}
	
	/**
	 * Returns a color to visualize the double value. This has to be in [0.0, 1.0].
	 * 
	 * @param value
	 * @return Color instance
	 */
	private static Color getColor(final double value) {
		float hue = (float)(240 - value*240)/360;
		return new Color(Color.HSBtoRGB(hue, 1f,1f)); 
	}
	
}
