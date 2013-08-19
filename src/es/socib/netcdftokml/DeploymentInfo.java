/**
 * 
 */
package es.socib.netcdftokml;

import org.apache.log4j.Logger;

/**
 * @author ksebastian
 *
 */
public class DeploymentInfo {
	
	/**
	 * logger the logger instance
	 */
	private static Logger logger = Logger.getLogger("processing");
	/**
	 * time The String time in UDUNITS format "n seconds since YYYY-MM-DD HH:MM:SS" 
	 */
	private String time;
	
	/**
	 * longitude The {@link String} longitude in degrees east
	 */
	private String longitude;
	
	/**
	 * latitude The {@link String} latitude in degrees north
	 */
	private String latitude;
	
	/**
	 * Construct a new {@link DeploymentInfo}
	 * 
	 * @param time the {@link String} time in <a href="http://www.unidata.ucar.edu/software/udunits/#documentation">UDUNITS</a> format "n seconds since YYYY-MM-DD HH:MM:SS".
	 * @param longitude the {@link String} longitude in degrees east.
	 * @param latitude the {@link String} latitude in degrees north.
	 */
	private DeploymentInfo(String time, String longitude, String latitude) {
		this.time = time;
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public static DeploymentInfo createDeploymentInfo(String time, String longitude, String latitude){
		
		if (null == time || null == longitude || null == latitude){
			throw new NullPointerException("createDeploymentInfo() -- One or more of the needed parameters are null. Null DeploymentInfo was returned");
		}
		
		return new DeploymentInfo(time, longitude, latitude);
	}

	public String getTime() {
		return time;
	}

	public String getLongitude() {
		return longitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	@Override
	public String toString() {
		return "DeploymentInfo [time=" + time + ", longitude=" + longitude
				+ ", latitude=" + latitude + "]";
	}
	
}
