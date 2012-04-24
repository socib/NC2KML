/**
 * 
 */
package es.socib.netcdftokml;

/**
 * @author ksebastian
 *
 */
public class DeploymentInfo {

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
	public DeploymentInfo(String time, String longitude, String latitude) {
		this.time = time;
		this.longitude = longitude;
		this.latitude = latitude;
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
