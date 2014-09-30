/**
 * 
 */
package es.socib.netcdftokml;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Index1D;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.units.DateUnit;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.TimePrimitive;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;

/**
 * @author slora
 */
public class KmlManager implements Runnable {
	
	
	/**
	 * logger the logger instance
	 */
	private static Logger logger = Logger.getLogger(KmlManager.class.getSimpleName());
	
	private static final String FEATURE_TYPE = "featureType";
	
	private static final String FT_TRAJECTORY = "trajectory";
	
	private static final String FT_TRAJECTORY_PROFILE = "trajectoryProfile";
	
	private AdditionalInfo additionalInfo;
	
	private NetcdfDataset netcdfDataset;
	
	private List<Variable> variableListWithoutAncillaryVariables;
	
//	private List<Variable> ancillaryVariableList;
	
	private AncillaryVariablesManager ancillaryVariablesMananger;
	
	private Map<AxisType, VariableDS> coordinateVariableMap;
	
	/**
	 * variableArrayMap Map with the variable as key and the variable data as value.
	 * 					Used to improve the performance.	
	 */
	private Map<Variable, Array> variableArrayMap;
	
	private String netCdfFileLocation;
	
	private String outputFileName;

	/**
	 * Constructs a new {@link KmlManager}. The NetCDF file must be compliant with the following specification:
	 * 		Should have the featureType global attribute with value trajectory or trajectoryProfile.
	 * 		Must have only one time coordinate ({@link AxisType}.Time) of one dimension.
	 * 		Must have only one latitude coordinate ({@link AxisType}.Lat) with the same dimension as the time coordinate.
	 * 		Must have only one longitude coordinate ({@link AxisType}.Lon) with the same dimension as the time coordinate.
	 * 
	 * Also, additional info can be provided, like:
	 * 		The initial deployment info (time, latitude and longitude values).
	 * 		The kml style info (Kml title name, data format pattern, line style color, template path and home, regular and end icon sytle)
	 * 		The opendap link of the kml data
	 * 
	 * @param netCDFDataSet the NetCDF file location, can be a local file or opendap link
	 * @param additionalInfo the additional info provided. If null the default values will be provided to the kml manager.
	 * @param outputFileName 
	 * @throws IOException 
	 */
	public KmlManager(NetcdfDataset netCDFDataSet, AdditionalInfo additionalInfo, String outputFileName) throws IOException{
		
		try {
			
			logger.info("Initializing the Kml manager");
			
			this.outputFileName = outputFileName;
			
			// Open the NetCDF file
			this.netCdfFileLocation = netCDFDataSet.getLocation();
			netcdfDataset = netCDFDataSet;
		
			// Check that the files is compliant with the defined specifications
			checkFile(netcdfDataset);
			
			// Set the kml document name. Used as the balloon title name also.
			String kmlDocumentName;
			if (null != netcdfDataset.findGlobalAttribute(AttributesNamesAndValues.TITLE)){
				kmlDocumentName = netcdfDataset.findGlobalAttribute(AttributesNamesAndValues.TITLE).getStringValue();
			} else {
				kmlDocumentName = "Trajectory data";
			}
			
			// Set the additional info. If null then set to the default values
			this.additionalInfo = additionalInfo;
			if (null == this.additionalInfo){
				KmlStyleInfo kmlStyleInfo = new KmlStyleInfo(kmlDocumentName, null, null, null, null, null, null);
				this.additionalInfo = new AdditionalInfo(null, kmlStyleInfo, null, false);
			} else if (null == this.additionalInfo.getKmlStyleInfo()){
				KmlStyleInfo kmlStyleInfo = new KmlStyleInfo(kmlDocumentName, null, null, null, null, null, null);
				this.additionalInfo.setKmlStyleInfo(kmlStyleInfo);
			} else if (null == this.additionalInfo.getKmlStyleInfo().getKmlTitleName()){
				this.additionalInfo.getKmlStyleInfo().setKmlTitleName(kmlDocumentName);
			}
			
			// Get the coordinate variable list
			coordinateVariableMap = getCoordinateVariableList(netcdfDataset);
			
			// Initialize the ancillary variables manager
			ancillaryVariablesMananger = new AncillaryVariablesManager(netcdfDataset, coordinateVariableMap);
			
			// Retrieve the variable list whitout the ancillary variable
			variableListWithoutAncillaryVariables = ancillaryVariablesMananger.getVariableListWithotAncillaryVariables();
			
//			variableListWithoutAncillaryVariables.removeAll(netcdfDataset.getCoordinateAxes());
			
//			ancillaryVariableList = ancillaryVariablesMananger.getAncillaryVariableList();
			
			// Find cf role variable, if exists remove it from the variableListWithotAncillaryVariables
			Variable cfRole = findCFRoleVariable(variableListWithoutAncillaryVariables);
			if (null != cfRole){
				variableListWithoutAncillaryVariables.remove(cfRole);
			}
			
			initializeVariableArrayMap();
			
			logger.info("Kml manager initialiced");

		} catch (KmlManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			netcdfDataset.close();
		}
	}
	
	/**
	 * Initialize the map with variable as key and array data as value
	 */
	private void initializeVariableArrayMap() {
		
		variableArrayMap = new HashMap<Variable, Array>();
		
		for (Variable variable : variableListWithoutAncillaryVariables){

			try {
				variableArrayMap.put(variable, variable.read());
			} catch (IOException e) {
				logger.error("Impossible read the variable data of " + variable.getFullName());
				e.printStackTrace();
			}
			
		}

	}

	/**
	 * Create a {@link Kml} object representation of the NetCDF file of type trajectory or trajectoryProfile.
	 * 
	 * First add the deployment position, if not null.  Loop over the time array to read the variable data, it
	 * can be of one dimension (time) or two dimensions (time, level). If have two dimensions the value
	 * displayed the current time index and the level zero, array.get(i,0).
	 * 
	 * If the latitude and longitude have quality controls applied, the bad positions won't be added to the
	 * kml.
	 * 
	 * @return the {@link Kml} object
	 * @throws KmlManagerException
	 * @throws IOException
	 */
	public Kml createKMLFile() throws KmlManagerException, IOException {
		
		Kml kml = null;

		try {
			
			
//			NetcdfFile netcdfFile = NetcdfFile.open(net);
			
			// Create and initialize the kml object
			kml = new Kml();
			final Document document = new Document();
			KmlStyleInfo kmlStyleInfo =  additionalInfo.getKmlStyleInfo();
			String title = kmlStyleInfo.getKmlTitleName(additionalInfo.getThreddsLink());
			kml.setFeature(document);
			document.setName(title);
			document.setOpen(true);
			kmlStyleInfo.setKmlDocumentStyle(document);
			
			// Retrieve the time, latitude and longitude coordinate data
			Variable timeVariable = coordinateVariableMap.get(AxisType.Time);
			Variable lonVariable = coordinateVariableMap.get(AxisType.Lon);
			Variable latVariable = coordinateVariableMap.get(AxisType.Lat);
			Array timeArrayData = timeVariable.read();
			
			// I don't know why but if NetcdfDataset is used to read the 
			// latitude and longitude variable, then NaN values are read
			// in  some files. (Socib argo profiles).
			NetcdfFile netcdfFile = NetcdfFile.open(netCdfFileLocation);
			Array lonArrayData = netcdfFile.readSection(lonVariable.getFullName());
			Array latArrayData = netcdfFile.readSection(latVariable.getFullName());
			
			// The coordinate list needed to generate the line string. Represent the platform trajectory.
			List<Coordinate> coordinateList = new ArrayList<Coordinate>();
			
			// Retrieve the first good data index
			Integer firstGoodDataIdx = ancillaryVariablesMananger.getFirstGoodDataIdx();
			// Retrieve the last good data index
			Integer latestGoodDataIdx = ancillaryVariablesMananger.getLastestGoodDataIdx();
			
			// No filter data without quality controls
			if (null == latestGoodDataIdx || null == firstGoodDataIdx){
				firstGoodDataIdx = 0;
				latestGoodDataIdx = (int) timeVariable.getSize() - 1;
			}
			
			if (-1 == latestGoodDataIdx || -1 == firstGoodDataIdx){
				throw new KmlManagerException("createKMLFile() -- No good data were found in the NetCDF " + netcdfDataset.getLocation());
			}
			
			// Add the deployment info if not null
			if (null != additionalInfo.getDeploymentInfo()){
				addDeploymentPosition(document, coordinateList);
			}
			
			Index timeIndex1D = new Index1D(timeVariable.getShape());
			timeIndex1D.set(firstGoodDataIdx);
			for (int i = firstGoodDataIdx; i <= latestGoodDataIdx; i++){
				
				// It doesn't add to the kml bad positions
				if (!ancillaryVariablesMananger.isGoodPosition(timeIndex1D)){
					logger.debug("Bad data");
					timeIndex1D.incr();
					continue;
				}
				
				String timeString = timeArrayData.getObject(timeIndex1D).toString();
				String lonString = lonArrayData.getObject(timeIndex1D).toString();
				String latString = latArrayData.getObject(timeIndex1D).toString();
				
				if ("NaN".equals(latString) || "NaN".equals(lonString)){
					//logger.warn("The latitude or longitude value is NaN");
					timeIndex1D.incr();
					continue;
				}
				
				BigDecimal timeBigDecimal = new BigDecimal(timeString);
				Date date = DateUnit.getStandardOrISO(Long.valueOf(timeBigDecimal.longValue()) + " " + timeVariable.getUnitsString());
				BigDecimal lonBigDecimal = new BigDecimal(lonString);
				BigDecimal latBigDecimal = new BigDecimal(latString);
				
				// Add the header description (Title, time and position)
				StringBuffer placemarkBalloonDescription = new StringBuffer();
				placemarkBalloonDescription.append("<h3>" + title + "</h3>");
				placemarkBalloonDescription.append("<br> <strong>Time: </strong>" + format(date.getTime(), kmlStyleInfo.getDateFormatPattern()));
				placemarkBalloonDescription.append("<br> <strong>Position: </strong>" + PositionManager.getLatGeoCoordinate(latBigDecimal) + " " + PositionManager.getLonGeoCoordinate(lonBigDecimal));
				placemarkBalloonDescription.append("<br>");
				
				logger.debug("Time: " + format(date.getTime(), kmlStyleInfo.getDateFormatPattern()) + " Lat: " + latBigDecimal.toEngineeringString() + " Lon: " +  lonBigDecimal.toPlainString());
				
				// Add the variable name and current value to the description
				for (Variable variable : variableListWithoutAncillaryVariables){
					
					String data;
					try {
						data = readDataLike1D(variable, timeIndex1D);
					} catch (NullPointerException e){
						logger.error("createKMLFile() -- Null pointer exception reading " + variable.getFullName());
						continue;
					} catch (IOException e){
						logger.error(e.getMessage());
						continue;
					}
					
					placemarkBalloonDescription.append("<br><strong>" + variable.getFullName() + ": </strong>" + data);
					
					logger.debug("Data from " + variable.getFullName()  + " " + data);
					
				}
				
				// Add the opendap link 
				if (additionalInfo.hasThreddsLink()){
					placemarkBalloonDescription.append("<br> <strong>TDS link:</strong> <a href=\"" + additionalInfo.getThreddsLink() + "\" title=\"OPeNDAP link\"> OPeNDAP link</a>");
				}
				
				/*
				 * Add the deployment placemark ballom to the kml document 
				 */
				final Placemark placemarkBalloon = new Placemark();
				placemarkBalloon.setDescription(placemarkBalloonDescription.toString());
				placemarkBalloon.setStyleUrl("#styleForRegularIcon");
				final Point point = new Point();
				placemarkBalloon.setGeometry(point);
				final List<Coordinate> coord = new ArrayList<Coordinate>();
				point.setCoordinates(coord);
				coord.add(new Coordinate(lonBigDecimal.floatValue(), latBigDecimal.floatValue()));
				
				/*
				 * Add the coordinate to the coordinate list. Needed to create the line string, that
				 * represent the platform trajectory
				 */
				coordinateList.add(new Coordinate(lonBigDecimal.floatValue(), latBigDecimal.floatValue()));
				
				/*
				 * 
				 */
				TimeSpan ts = new TimeSpan();
				String beginTimeString = format(date.getTime(), "yyyy-MM-dd HH:mm:ss").replace(" ", "T");
				ts.setBegin(beginTimeString);
				
				//Seeking for the next good data index
				int nextGoodDataIdx = ancillaryVariablesMananger.nextGoodDataPositionIdx(i);
				
				logger.debug("Next good data " + nextGoodDataIdx);
				
				if (i < latestGoodDataIdx){
					BigDecimal endTimeBigDecimal = new BigDecimal(timeArrayData.getObject(nextGoodDataIdx).toString());
					Date endDate = DateUnit.getStandardOrISO(Long.valueOf(endTimeBigDecimal.longValue()) + " " + timeVariable.getUnitsString());
					String endTimeString = format(endDate.getTime(), "yyyy-MM-dd HH:mm:ss").replace(" ", "T"); 
					ts.setEnd(endTimeString);
				}
				
				ts.setId("timespanId");
				
				placemarkBalloon.withTimePrimitive((TimePrimitive) ts);
				
				document.getFeature().add(placemarkBalloon);
				
				timeIndex1D.incr();
				logger.debug("i = " + i + " index = " + timeIndex1D);
				
			}
			
			
			if (document.getFeature().size() > 0){
			
				/*
				 * Set first and last posistion icon style
				 */
				List<Feature> featureList = document.getFeature();
				featureList.get(0).setStyleUrl("#styleForHomeIcon");
				featureList.get(featureList.size() - 1).setStyleUrl("#styleForFinalIcon");
				
				/*
				 * Create the line string 
				 */
				final Placemark placemarkLine = new Placemark();
				document.getFeature().add(placemarkLine);
				final LineString linestring = new LineString();
				placemarkLine.setGeometry(linestring);
				linestring.setExtrude(false);
				linestring.setTessellate(true);
				linestring.setCoordinates(coordinateList);
				placemarkLine.setStyleUrl("#lineStyleId");
				
			} else {
				
				logger.error("createKMLFile() -- The kml documents doesm't have features");
				
			}
		
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			
			netcdfDataset.close();
			
		}
		
		logger.info("Kml object created from the NetCDF file " + netCdfFileLocation);
		
		return kml;
	}

	/**
	 * Add the deployment position and time to the kml document. Also add the position
	 * to the coordinate list, needed to the kml line string
	 * 
	 * @param document the kml document
	 * @param coordinateList the coordinate list
	 * @throws IOException
	 */
	private void addDeploymentPosition(Document document, List<Coordinate> coordinateList) throws IOException {
		
		logger.info("Adding deployment position");
		
		Variable timeVariable = coordinateVariableMap.get(AxisType.Time);
		
		String time = additionalInfo.getDeploymentInfo().getTime();
		BigDecimal lonBigDecimal = new BigDecimal(additionalInfo.getDeploymentInfo().getLongitude());
		BigDecimal latBigDecimal = new BigDecimal(additionalInfo.getDeploymentInfo().getLatitude());
		
		Date date = DateUnit.getStandardOrISO(time);
		
		StringBuffer placemarkBalloonDescription = new StringBuffer();
		placemarkBalloonDescription.append("<h3>" + additionalInfo.getKmlStyleInfo().getKmlTitleName(additionalInfo.getThreddsLink()) + "</h3>");
		placemarkBalloonDescription.append("<br> <strong>Position: </strong>" + PositionManager.getLatGeoCoordinate(latBigDecimal) + " " + PositionManager.getLonGeoCoordinate(lonBigDecimal));
		placemarkBalloonDescription.append("<br>");
		
		/*
		 * Add the deployment placemark ballom to the kml document 
		 */
		final Placemark placemarkBalloon = new Placemark();
		placemarkBalloon.setDescription(placemarkBalloonDescription.toString());
//		placemarkBalloon.setStyleUrl("#styleForHomeIcon");
		final Point point = new Point();
		placemarkBalloon.setGeometry(point);
		final List<Coordinate> coord = new ArrayList<Coordinate>();
		point.setCoordinates(coord);
		coord.add(new Coordinate(lonBigDecimal.floatValue(), latBigDecimal.floatValue()));
		
		/*
		 * Add the coordinate to the coordinate list. Needed to create the line string, that
		 * represent the platform trajectory
		 */
		coordinateList.add(new Coordinate(lonBigDecimal.floatValue(), latBigDecimal.floatValue()));
		
		/*
		 * Add the time period to the placemark
		 */
		TimeSpan ts = new TimeSpan();
		String beginTimeString = format(date.getTime(), "yyyy-MM-dd HH:mm:ss").replace(" ", "T");
		ts.setBegin(beginTimeString);
		
		//seeking for the next good data index
		int nextGoodDataIdx = ancillaryVariablesMananger.nextGoodDataPositionIdx(-1);
		
		logger.debug("Next good data " + nextGoodDataIdx);
		
		if (1 >= nextGoodDataIdx){
			BigDecimal endTimeBigDecimal = new BigDecimal(timeVariable.read().getObject(nextGoodDataIdx).toString());
			Date endDate = DateUnit.getStandardOrISO(Long.valueOf(endTimeBigDecimal.longValue()) + " " + timeVariable.getUnitsString());
			String endTimeString = format(endDate.getTime(), "yyyy-MM-dd HH:mm:ss").replace(" ", "T"); 
			ts.setEnd(endTimeString);
			ts.setId("timespanId");
			placemarkBalloon.withTimePrimitive((TimePrimitive) ts);
		}
		
		document.getFeature().add(placemarkBalloon);
		
		logger.info("Deployment position added");
		
	}

	/**
	 * Read the variable data at the specified index. The variable can be of one dimension (time) or 
	 * two dimensions (time, level). If have two dimensions, read the value
	 * from the current time index and the level zero, array.get(i,0).The specified index must be of 
	 * one dimension and must represent the time index.
	 * 
	 * The value returned is N/A if the value is bad data (from the quality controls, if exists) or
	 * the value is missing value.
	 * 
	 * @param variable the {@link Variable}
	 * @param timeIndex1D the {@link Index1D}
	 * @throws IOException
	 */
	private String readDataLike1D(Variable variable, Index timeIndex1D) throws IOException {
		
		Array dataVariable = variableArrayMap.get(variable);
		Index variableIndex;
		String stringDataWithUnits;
		
		List<CoordinateSystem> coordinateSystemsList = new ArrayList<CoordinateSystem>();
		CoordinateAxis coordinateAxis;

		int currentIndex0 = timeIndex1D.getCurrentCounter()[0];

		if (1 == dataVariable.getShape().length){
			
			variableIndex = new Index1D(dataVariable.getShape());
			int[] stride = {currentIndex0};
			variableIndex.set(stride);
			
			stringDataWithUnits = dataVariable.getObject(variableIndex).toString() + formatUnits(variable);
			
		} else if (2 == dataVariable.getShape().length){
			
			coordinateSystemsList = ((VariableDS) variable).getCoordinateSystems();
			if (coordinateSystemsList.size() == 0 || null == coordinateSystemsList){
				throw new NullPointerException("Coordinate systems of variable " + variable.getFullName() + " is null or 0");
			}
			
			coordinateAxis = coordinateSystemsList.get(0).findAxis(AxisType.Height);
			if (null == coordinateAxis){
				throw new NullPointerException("The variable " + variable.getFullName() + " doesn't have a coordinate axis of type height");
			}
			
			variableIndex = new Index2D(dataVariable.getShape());
			int[] stride = {currentIndex0, 0};
			variableIndex.set(stride);
			
			if (1 == coordinateAxis.getShape().length){
				
				Index coorinateIndex = new Index1D(dataVariable.getShape());
				int[] coordinateStride = {currentIndex0};
				coorinateIndex.set(coordinateStride);
				
				stringDataWithUnits = dataVariable.getObject(variableIndex).toString() + formatUnits(variable) + " at " + coordinateAxis.read().getObject(coorinateIndex).toString() + coordinateAxis.getUnitsString();
				
			} else if (2 == coordinateAxis.getShape().length) {
				
				stringDataWithUnits = dataVariable.getObject(variableIndex).toString() + formatUnits(variable) + " at " + coordinateAxis.read().getObject(variableIndex).toString() + coordinateAxis.getUnitsString();
				
			} else {
				
				throw new NullPointerException("The coordinate "  + coordinateAxis.getFullName() +  " must be 1 or 2 dimensional");
				
			}
			
		} else {
			
			throw new NullPointerException("The variable "  + variable.getFullName() +  " must be 1 or 2 dimensional");
		
		}
		
		String fillValue = "";
		if (null != variable.findAttribute(AttributesNamesAndValues.FILL_VALUE)){
			fillValue = variable.findAttribute(AttributesNamesAndValues.FILL_VALUE).getNumericValue().toString().trim();
		}
		
		String stringData = dataVariable.getObject(variableIndex).toString().trim();
		if (ancillaryVariablesMananger.isGoodDataVariable(variable, variableIndex) && !stringData.toLowerCase().equals("nan") && !stringData.toLowerCase().equals(fillValue)){
			return stringDataWithUnits;
		} else {
			return "N/A";
		}

	}
	
	/**
	 * Format units.
	 * 
	 * If the units are "1" or "" return "". Otherwise return " " + the untis.
	 * 
	 * @param units the units
	 * @return the units formatted
	 */
	private String formatUnits(Variable variable){
		
		String units = variable.getUnitsString();
		
		if (null == units){
			return "";
		}
		
		if ("1".equals(units.trim()) || "".equals(units.trim())){
			return "";
		} else {
			return " " + units.trim(); 
		}
		
	}

	/**
	 * Retrieve the cf role variable from the {@link Variable} list
	 * 
	 * @param variableListWithotAncillaryVariables the {@link Variable} list.
	 * @return the cf role {@link Variable}
	 */
	private static Variable findCFRoleVariable(List<Variable> variableListWithotAncillaryVariables) {
		
		for (Variable variable : variableListWithotAncillaryVariables){
			Attribute attribute = variable.findAttribute(AttributesNamesAndValues.CF_ROLE);
			if (null != attribute){
				return variable;
			}
			
		}
		return null;
	}

	/**
	 * Create map with the {@link AxisType} and {@link VariableDS} from the given {@link NetcdfDataset}.
	 * 
	 * @param netCdfFile the {@link NetcdfDataset}.
	 * @return the map with the {@link AxisType} and {@link VariableDS}.
	 * @throws KmlManagerException
	 */
	private static Map<AxisType, VariableDS> getCoordinateVariableList(NetcdfDataset netCdfFile) throws KmlManagerException{
		
		Map<AxisType, VariableDS> coordinateVariableMap = new HashMap<AxisType, VariableDS>();
		
		for (CoordinateAxis coordinateAxis: netCdfFile.getCoordinateAxes()){
			
			if (coordinateVariableMap.containsKey(coordinateAxis.getAxisType())){
				throw new KmlManagerException("Only one type axis " + coordinateAxis.getAxisType() + " is allowed.");
			}
			
			coordinateVariableMap.put(coordinateAxis.getAxisType(), coordinateAxis);
			
		}
		
		return coordinateVariableMap;
		
	}

	/**
	 * The NetCDF file must be compliant with the following specification:
	 * 		Should have the featureType global attribute with value trajectory or trajectoryProfile.
	 * 		Must have only one time coordinate ({@link AxisType}.Time) of one dimension.
	 * 		Must have only one latitude coordinate ({@link AxisType}.Lat) with the same dimension as the time coordinate.
	 * 		Must have only one longitude coordinate ({@link AxisType}.Lon) with the same dimension as the time coordinate.
	 * 
	 * @param netCdfFile the {@link NetcdfDataset}
	 * @throws KmlManagerException
	 */
	private static void checkFile(NetcdfDataset netCdfFile) throws KmlManagerException {
		
		if (null == netCdfFile.findGlobalAttribute(FEATURE_TYPE)){
			logger.warn("The Netcdf file doesn't have the featureType global attribute ");
			
		} else {
		
			String featureType = netCdfFile.findGlobalAttribute(FEATURE_TYPE).getValue(0).toString();
			if (!FT_TRAJECTORY.equals(featureType) && !FT_TRAJECTORY_PROFILE.equals(featureType)){
				logger.warn("The featureType global attribute must be trajectory or trajectoryProfile.");
			}
			
		}
		
		Variable timeVariable = netCdfFile.findCoordinateAxis(AxisType.Time);
		if (null == timeVariable){
			throw new KmlManagerException("The NetCDF dataset doesn't have time coordinate");	
		}
		if (timeVariable.getDimensions().size() > 1){
			throw new KmlManagerException("The time coordinate must be unidimensional variable");
		}
		
	}
	
	/**
	 * Format the time into a specific pattern in time zone GMT0
	 * 
	 * @param time the time expressed in milliseconds
	 * @param pattern  the pattern to use to format the date, not null
	 * @return the formatted date
	 */
	public static String format(long time, String pattern ){
		return DateFormatUtils.format(time, pattern, TimeZone.getTimeZone("GMT0"));
		
	}

	@Override
	public void run() {
		try {
			this.createKMLFile().marshalAsKmz(outputFileName);
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (KmlManagerException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
}
