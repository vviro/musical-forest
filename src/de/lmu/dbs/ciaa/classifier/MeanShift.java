package de.lmu.dbs.ciaa.classifier;

public class MeanShift {
	
	private int windowSize;
	
	public MeanShift(int windowSize) {
		this.windowSize = windowSize/2;
	}
	
	public int[][] segmentation;
	
	public int[][] modes;
	
	public float[][] modeWeights;
	
	public void process(float[][] data, float threshold) {
	
		modes = new int[data.length][data[0].length];
		modeWeights = new float[data.length][data[0].length];
		segmentation = new int[data.length][data[0].length];
		
		int nextModeId = 1;
		for(int dx=0; dx<data.length; dx++) {
			for(int dy=0; dy<data[0].length; dy++) {
				if (data[dx][dy] >= threshold) {
					int x = dx;
					int y = dy;
					int iter = 0;
					double diff;
					boolean count = true;
					do {
						float avg = -Float.MAX_VALUE;
						int meanX = -1;
						int meanY = -1;
						int ch = 0; 
						for(int nx=x-windowSize; nx<x+windowSize; nx++) {
							if (nx >= 0 && nx < data.length) {
								for(int ny=y-windowSize; ny<y+windowSize; ny++) {
									if (ny >= 0 && ny < data[0].length) {
										if (data[nx][ny] >= threshold) {
											if (data[nx][ny] > avg) {
												avg = data[nx][ny];
												meanX = nx;
												meanY = ny;
												ch++;
											}
										}
									}
								}
							}
						}
						if (ch <= 1) {
							count = false;
						}
						diff = Math.sqrt((x-meanX)*(x-meanX) + (y-meanY)*(y-meanY));
						x = meanX;
						y = meanY;
						iter++;
					} while (count && diff > 0.5f && iter < 100);
					//System.out.println("Stopped at " + iter + ", diff: " + diff);
					if (count) {
						if (modes[x][y] == 0) {
							modes[x][y] = nextModeId;
							nextModeId++;
						}
						modeWeights[x][y]+= data[dx][dy] * data[dx][dy];
						segmentation[dx][dy] = modes[x][y];
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param bandwidth
	 * @return
	 *
	private static float[][] getKernel(float bandwidth) {
		int size = (int)Math.ceil(1.0f/bandwidth)*2*5; // TODO festwert? passt hier eigentlich, da normalvt.
		float[][] ret = new float[size][size];
		for(int x=0; x<size; x++) {
			for(int y=0; y<size; y++) {
				
			}
		}
		return ret;
	}
	
	public static float[][] gaussianBlur(float[][] data, float bandwidth, float threshold) {
		float[][] ret = new float[data.length][data[0].length];
		float[][] kernel = getKernel(bandwidth); 
		
		for (int x=0; x< data.length; x++) {
			for(int y=0; y<data[0].length; y++) {
				//if (data[x][y] < threshold) continue;
				for (int nx=0; nx< data.length; nx++) {
					for(int ny=0; ny<data[0].length; ny++) {
						float c = (nx-x)*(nx-x) + (ny-y)*(ny-y);
						if (c < 1.0f/bandwidth && data[nx][ny] >= threshold) {
							ret[x][y]+= data[nx][ny] * Math.exp(-c/(bandwidth*bandwidth));
						}
					}
				}
				
			}
		}
		ArrayUtils.normalize(ret);
		return ret;
	}
	//*/
}

/**
 * 
 * Based on a mean shift implementation from Elif Dandan: http://www.codeforge.com/read/109081/MeanShift.java__html
 * 
 * @author Elif DANDAN, Thomas Weber
 *
 *
public class MeanShift {
	int rad = 20; 
	int rad2 = rad*rad;
	float radCol = 10f; 
	float radCol2 = radCol*radCol;
	//int nPasses, pass;
	//boolean isRGB;

	/**
	 * Segments RGB image
	 * @param ip		The image processor
	 *
	public void segmentRGBImage(BufferedImage ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		//int[] pixels = (int[])ip.getPixels();
		float[][] pixelsf = new float[width*height][3];

		for (int i = 0; i < pixelsf.length; i++) {
			int argb = ip.getRGB(i%width, i/width); //pixels[i];
//System.out.println(argb);
			int r = (argb >> 16) & 0xff;
			int g = (argb >>  8) & 0xff;
			int b = (argb) & 0xff;

			pixelsf = rgb2luv(r, g, b, pixelsf,i);
		}

		float shift = 0;
		int iters = 0;
		for (int y=0; y<height; y++) {
			if (y%20==0) System.out.println("Status: " + y/(double)height);//showProgress( y/(double)height);
			for (int x=0; x<width; x++) {

				int xc = x;
				int yc = y;
				int xcOld, ycOld;
				float LcOld, UcOld, VcOld;
				int pos = y*width + x;
				float[] luv = pixelsf[pos];
				float Lc = luv[0];
				float Uc = luv[1];
				float Vc = luv[2];

				iters = 0;
				do {
					xcOld = xc;
					ycOld = yc;
					LcOld = Lc;
					UcOld = Uc;
					VcOld = Vc;

					float mx = 0;
					float my = 0;
					float mL = 0;
					float mU = 0;
					float mV = 0;
					int num=0;

					for (int ry=-rad; ry <= rad; ry++) {
						int y2 = yc + ry; 
						if (y2 >= 0 && y2 < height) {
							for (int rx=-rad; rx <= rad; rx++) {
								int x2 = xc + rx; 
								if (x2 >= 0 && x2 < width) {
									if (ry*ry + rx*rx <= rad2) {
										luv = pixelsf[y2*width + x2];

										float L2 = luv[0];
										float U2 = luv[1];
										float V2 = luv[2];

										float dL = Lc - L2;
										float dU = Uc - U2;
										float dV = Vc - V2;

										if (dL*dL+dU*dU+dV*dV <= radCol2) {
											mx += x2;
											my += y2;
											mL += L2;
											mU += U2;
											mV += V2;
											num++;
										}
									}
								}
							}
						}
					}

					float num_ = 1f/num;

					Lc = mL*num_;
					Uc = mU*num_;
					Vc = mV*num_;

					xc = (int) (mx*num_+0.5);
					yc = (int) (my*num_+0.5);
	
					int dx = xc-xcOld;
					int dy = yc-ycOld;
					float dL = Lc-LcOld;
					float dU = Uc-UcOld;
					float dV = Vc-VcOld;

					shift = dx*dx+dy*dy+dL*dL+dU*dU+dV*dV; 
					iters++;
				}
				while (shift > 3 && iters < 100);

				int rgbconverted[] = luv2rgb(Lc, Uc, Vc);

				
				int r_ = rgbconverted[0];
				int g_ = rgbconverted[1];
				int b_ = rgbconverted[2];
				
				ip.setRGB(pos%width, pos/width, (0xFF<<24)|(r_<<16)|(g_<<8)|b_);
				//pixels[pos] = (0xFF<<24)|(r_<<16)|(g_<<8)|b_;
			}

		}
	}

	/**
	 * Convert L*u*v* color space values to RGB color space
	 * @param l 	The L* value
	 * @param u 	The u* value
	 * @param v 	The v* value
	 *
	public static final int[] luv2rgb(float l, float u, float v){
	 
		final float Un=(float) 0.19793943 ;  
		final float Vn=(float) 0.46831096 ;  
	
		int[] rgb=new int[3] ;
		float y ;
		if(l>=8) y=(float)Math.pow((l+16)/116.0,3) ;
		else y=l/(float)903.3 ;
	
		float u1=u/((float)13.0*l)+Un ;
		float v1=v/((float)13.0*l)+Vn ;
		float x=(float)2.25*u1*y/v1 ;
		float z=((9/v1-15)*y-x)/(float)3.0 ;
		x*=255 ; y*=255; z*=255 ;
		rgb[0]=(int)(3.0596*x-1.3927*y-0.4747*z+0.5) ;
		rgb[1]=(int)(-0.9676*x+1.8748*y+0.0417*z+0.5) ;
		rgb[2]=(int)(0.0688*x-0.2299*y+1.0693*z+0.5) ;
	
		return rgb ;
	}
	
	/**
	 * Convert RGB color space values to L*u*v* color space
	 * @param r 	The red value(0-255)
	 * @param g 	The green value(0-255)
	 * @param b 	The blue value(0-255)
	 * @param luv The output L*u*v* 
	 *
	public static final float[][] rgb2luv(int r, int g, int b, float[][] luv, int i) {
		final float M11=(float) 0.431 ;
		final float M12=(float) 0.342 ;
		final float M13=(float) 0.178 ;
		final float M21=(float) 0.222 ;
		final float M22=(float) 0.707 ;
		final float M23=(float) 0.071 ;
		final float M31=(float) 0.020 ;
		final float M32=(float) 0.130 ;
		final float M33=(float) 0.939 ;
		final float Un=(float) 0.19793943 ;  
		final float Vn=(float) 0.46831096 ;
		float x, y, z ;
		float u,v ;
		float tmp ;
	
		x=(M11*r+M12*g+M13*b)/(float)255.0 ;
		y=(M21*r+M22*g+M23*b)/(float)255.0 ;
		z=(M31*r+M32*g+M33*b)/(float)255.0 ;
		tmp=x+15*y+3*z ;
		if(tmp==0.0) tmp=(float)1.0 ;
		u=(float)4.0*x/tmp ;
		v=(float)9.0*y/tmp ;
		if(y>0.008856) luv[i][0]=116*(float)Math.pow(y,1.0/3)-16;
		else luv[i][0]=(float)903.3*y;
		luv[i][1]=13*luv[i][0]*(u-Un) ;
		luv[i][2]=13*luv[i][0]*(v-Vn) ;
		return luv;
	}
}
*/
