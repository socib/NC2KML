package netcdftokml.test;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import es.socib.netcdftokml.AdditionalInfo;
import es.socib.netcdftokml.DeploymentInfo;
import es.socib.netcdftokml.KmlManager;
import es.socib.netcdftokml.KmlManagerException;

public class Testing {
	
	/**
	 * logger the logger instance
	 */
	private static Logger logger = Logger.getLogger(Testing.class.getSimpleName());
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws XPathExpressionException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Load Log4j properties
		InputStream stream = KmlManager.class.getResourceAsStream("/log4j_kmz.properties");
		Properties properties = new Properties();
	    try {
	        properties.load(stream);
	    } catch (IOException e) {
	        logger.error(e.getLocalizedMessage());
	    }
		PropertyConfigurator.configure(properties);
			
		String netCdfFileLocation = "/home/ksebastian/opendap/observational/auv/glider/ideep02-ime_deep02/L1/2011/dep0001_ideep02_ime-deep02_L1_2011-12-01.nc";
		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/argos_kml/dep0001_ideep02_ime-deep02_L1_2011-12-01.kmz";
	    
//		String netCdfFileLocation = "/data/current/opendap/observational/drifter/surface_drifter/drifter_svp003-ime_svp003/L1/2011/dep0001_drifter-svp003_ime-svp003_L1_2011-09-08.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/dep0001_drifter-svp003_ime-svp003_L1_2011-09-08.kmz";
		
//		String netCdfFileLocation = "/data/current/opendap/observational/auv/glider/seaglider_sdeep002-scb_sdeep002/L1/2012/dep0001_seaglider-sdeep002_scb-sdeep002_L1_2012-03-12.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/dep0001_seaglider-sdeep002_scb-sdeep002_L1_2012-03-12.kmz";
		
//		String netCdfFileLocation = "/home/ksebastian/opendap/observational/drifter/surface_drifter/idrifter003-scb_idrifter003/L1/2012/dep0002_idrifter003_scb-idrifter003_L1_2012-02.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/dep0002_idrifter003_scb-idrifter003_L1_2012-02.kmz";
		
//		String netCdfFileLocation = "/home/ksebastian/opendap/observational/drifter/surface_drifter/drifter_svp004-ime_svp004/L1/2011/dep0001_drifter-svp004_ime-svp004_L1_2011-09-08.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/dep0001_drifter-svp004_ime-svp004_L1_2011-09-08.kmz";
		
//		String netCdfFileLocation = "/home/ksebastian/opendap/observational/auv/glider/IMOS_ANFOG_BCEOSTUV_20110202T013918Z_SL130_FV01_timeseries_END-20110220T190613Z.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/IMOS_ANFOG_BCEOSTUV_20110202T013918Z_SL130_FV01_timeseries_END-20110220T190613Z.kmz";
		
//		String netCdfFileLocation = "/home/ksebastian/opendap/observational/drifter/surface_drifter/drifter_svp001-ime_svp001/L1/2011/dep0001_drifter-svp001_ime-svp001_L1_2011-09-08.nc";
//		String kmlFileLocation = "/home/ksebastian/workspace/processing_web_files/dep0001_drifter-svp001_ime-svp001_L1_2011-09-08.kmz";
		
		try {
			DeploymentInfo deploymentInfo = DeploymentInfo.createDeploymentInfo("1 day since 2011-09-07 00:00:00", "3.400", "39.200");
			AdditionalInfo additionalInfo = new AdditionalInfo(deploymentInfo, null, null, false);
			KmlManager kmlManager = new KmlManager(NetcdfDataset.openDataset(netCdfFileLocation), additionalInfo);
			kmlManager.createKMLFile().marshalAsKmz(kmlFileLocation);
		} catch (KmlManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
//			try {
//				DateUnit dateUnit = new DateUnit("1023.50 days since 1970-01-01 00:00:00");
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
//			System.out.println(DateUnit.getStandardOrISO("3600.75 minutes since 1970-01-01 00:00:00").getTime()/1000);
		
		
	}
	
}