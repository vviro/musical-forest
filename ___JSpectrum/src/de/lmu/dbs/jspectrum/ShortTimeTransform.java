package de.lmu.dbs.jspectrum;

/**
 * Interface for short time transformations from time domain to time/freq domain.
 * 
 * @author Thomas Weber
 *
 */
public interface ShortTimeTransform {

	/**
     * Take a buff_in of plain audio samples and calculate the transformation coeffs.
     * <br><br>
     * Output format:<br>
     * Indexes i*2: real<br>
     * Indexes i*2+1: imaginary<br>
     *
     * @param buff_in
     * @param buff_out holds complex representation of each coeff (real and imaginary)
     */
	public void calc(final double[] buff_in, final double[] buff_out);

	/**
     * Take a buff_in of plain audio samples and calculate the transformation coeffs,
     * optimized for ready-to-use output format (non-complex).
     *
     * @param buff_in
     * @param buff_out 
     */
	public void calcMagnitude(final double[] buff_in, final double[] buff_out);

	/**
	 * Returns the window length
	 * 
	 * @return
	 */
	public int getWindowSize();

}
