/**
 * 
 */
package es.socib.netcdftokml;

import java.math.BigDecimal;

/**
 * @author ksebastian
 *
 */
public class PositionManager {
	
	private static final String N = "N";
	
	private static final String S = "S";
	
	private static final String E = "E";
	
	private static final String W = "W";
	
	public static String getLatGeoCoordinate(BigDecimal lat){
		
		if (lat.floatValue() < 0.0){
			return S + lat.negate().toPlainString();
		}
		
		return N + lat.toPlainString();
		
	}
	
	public static String getLonGeoCoordinate(BigDecimal lon){
		
		if (lon.floatValue() < 0.0){
			return W + lon.negate().toPlainString();
		}
		
		return E + lon.toPlainString();
		
	}

}
