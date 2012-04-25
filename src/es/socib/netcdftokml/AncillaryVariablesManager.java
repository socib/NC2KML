/**
 * 
 */
package es.socib.netcdftokml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

/**
 * @author ksebastian
 *
 */
public class AncillaryVariablesManager {
	
	/**
	 * logger the logger instance
	 */
	private static Logger logger = Logger.getLogger(AncillaryVariablesManager.class.getSimpleName());
	
	private static String QC_PREFIX = "qc_";
	
	private static String QC_SUFFIX = "_qc";
	
	private static int PROBABLY_GOOD_DATA = 2;
	
	private NetcdfDataset netcdfDataset;
	
	private List<Variable> variableListWithotAncillaryVariables;
	
	private List<Variable> ancillaryVariableList;
	
	private Map<Variable, Array> mapVariableAncillaryVariableNames;
	
	private Array qcLatVariableArray;
	
	private Array qcLonVariableArray;
	
	private Integer latestGoodDataIdx;

	private Integer firstGoodDataIdx;
	
	/**
	 * Constructs a new {@link AncillaryVariablesManager}. Initialize:
	 * 		The latitude and longitude quality control array data.
	 * 		The variable list without the ancillary and coordinate variables.
	 * 		The ancillary variable list, only with the ancillary variables.
	 * 		The map with the variable as key and the array of ancillary variable names as value.
	 * 		The first and last good position index.
	 * 
	 * @param netcdfDataset
	 * @param coordinateAxisMap
	 */
	public AncillaryVariablesManager(NetcdfDataset netcdfDataset,  Map<AxisType, VariableDS> coordinateAxisMap){
		
		this.netcdfDataset = netcdfDataset;
		this.mapVariableAncillaryVariableNames = new HashMap<Variable, Array>();
		this.ancillaryVariableList = retrieveAncillaryVariablesWithinTheDataset();
		this.variableListWithotAncillaryVariables = retrieveVariableListWithoutAncillaryVariables();
		this.variableListWithotAncillaryVariables.removeAll(netcdfDataset.getCoordinateAxes());
		
		String qcLatVariableName = findQCAncillaryVariableName(findAncillaryVariableNames(coordinateAxisMap.get(AxisType.Lat)));
		String qcLonVariableName = findQCAncillaryVariableName(findAncillaryVariableNames(coordinateAxisMap.get(AxisType.Lon)));
		
		if (null != qcLatVariableName){
			qcLatVariableArray = readVariable(qcLatVariableName);
		} else {
			qcLatVariableArray = null;
		}
		
		if (null != qcLonVariableName){
			qcLonVariableArray = readVariable(qcLonVariableName);
		} else {
			qcLonVariableArray = null;
		}
		
		searchLastAndFirstGoodDataIdx();
		
	}

	/**
	 * Set the first and latest good data index. If the position doesn't have quality controls applied then
	 * the latest and first good data index set them to null. If there aren't good position then the latest 
	 * and first good data index set them to -1
	 */
	private void searchLastAndFirstGoodDataIdx() {
		
		if(null == qcLatVariableArray || null == qcLonVariableArray){
			latestGoodDataIdx = firstGoodDataIdx = null;
		}
		
		this.latestGoodDataIdx = 0;
		this.firstGoodDataIdx = (int) qcLatVariableArray.getSize();
		
		for (int i =  0; i < (int) qcLatVariableArray.getSize(); i++){
			if (qcLatVariableArray.getInt(i) < PROBABLY_GOOD_DATA || qcLonVariableArray.getInt(i) < PROBABLY_GOOD_DATA){
				if (latestGoodDataIdx < i){
					latestGoodDataIdx = i;
				}
				if (firstGoodDataIdx > i){
					firstGoodDataIdx =  i;
				}
			}
		}
		
		if (latestGoodDataIdx < firstGoodDataIdx){
			latestGoodDataIdx = firstGoodDataIdx = -1;
		}
		
	}
	
	/**
	 * Find the next good position data index.
	 * 
	 * @param currentIdx the current idx
	 * @return the next position if the position doesn't have quality controls applied or there aren't more good positions.
	 * Otherwise return the next good position if exists.
	 */
	public int nextGoodDataPositionIdx(int currentIdx) {
		
		int nextGoodDataIdx = currentIdx + 1;
		
		if(null == qcLatVariableArray || null == qcLonVariableArray){
			return nextGoodDataIdx;
		}
		
		Integer qcLatValue;
		Integer qcLonValue;
		for (int i = nextGoodDataIdx; i <= latestGoodDataIdx; i ++){
			qcLatValue = qcLatVariableArray.getInt(nextGoodDataIdx);
			qcLonValue = qcLonVariableArray.getInt(nextGoodDataIdx);
			if (qcLatValue >= PROBABLY_GOOD_DATA && qcLonValue >= PROBABLY_GOOD_DATA){
				nextGoodDataIdx++;
			} else {
				continue;
			}
		}
		
		return nextGoodDataIdx;
	}

	/**
	 * Retrieve the {@link Variable} list of ancillary variables in the {@link NetcdfDataset}.
	 * 
	 * @return the {@link Variable} list of ancillary variables.
	 */
	private List<Variable> retrieveAncillaryVariablesWithinTheDataset() {
		
		List<Variable> ancillaryVaraibleList = new ArrayList<Variable>();
		
		for (Variable variable : netcdfDataset.getRootGroup().getVariables()){
			Array ancillaryVariableNames = findAncillaryVariableNames(variable);
			List<Variable> variableAncillaryVariableList;
			if (null != ancillaryVariableNames){
				variableAncillaryVariableList = findAncillaryVariables(ancillaryVariableNames);
				ancillaryVaraibleList.addAll(variableAncillaryVariableList);
				mapVariableAncillaryVariableNames.put(variable, ancillaryVariableNames);
			}
		}
		
		return ancillaryVaraibleList;
	
	}

	/**
	 * Retrieve the {@link Variable} list of ancillary variables, in the {@link NetcdfDataset},
	 * from the the given {@link Array} of ancillary variable names
	 * 
	 * @param ancillaryVariableNames the given {@link Array} of ancillary variable names
	 * @return the {@link Variable} list of ancillary variables
	 */
	private List<Variable> findAncillaryVariables(Array ancillaryVariableNames) {
		
		List<Variable> ancillaryVaraibleList = new ArrayList<Variable>();
		
		while(ancillaryVariableNames.hasNext()){
			String ancVarVal = (String) ancillaryVariableNames.next();
			Variable variable = netcdfDataset.findVariable(ancVarVal);
			if (null != variable){
				ancillaryVaraibleList.add(variable);
			} else {
				logger.error("Ancillary variable " + ancVarVal + " isn't in the NetCDF dataset " + netcdfDataset.getLocation());
			}
		}
		
		return ancillaryVaraibleList;
		
	}

	/**
	 * Retrieve the {@link Variable} list from the {@link NetcdfDataset} without the ancillary variables. 
	 * 
	 * @return the variable list without the ancillary variables.
	 */
	private List<Variable> retrieveVariableListWithoutAncillaryVariables() {
		
		List<Variable> variableListWithoutAncillaryVariables = new ArrayList<Variable>(netcdfDataset.getRootGroup().getVariables());
		
		if (null != ancillaryVariableList){
			variableListWithoutAncillaryVariables.removeAll(ancillaryVariableList);
		} else {
			throw new NullPointerException("The ancillary variable list is null");
		}
		
		return variableListWithoutAncillaryVariables;

	}

	/**
	 * Retrieve the {@link Array} of ancillary variables names from the given variable {@link Variable}.
	 * 
	 * @param variable the given variable.
	 * @return the {@link Array} of ancillary variables if the variable has the ancillary_variables {@link Attribute} or the variable has a
	 * quality control variable in the dataset (not defined in the ancillary_variables attribute). Otherwise return null.
	 */
	private Array findAncillaryVariableNames(Variable variable) {
		
		if (null == variable){
			throw new NullPointerException("findAncillaryVariableNames() -- Imposible to find ancillary variable name from null variable");
		}
		
		Attribute ancillaryVariables = variable.findAttribute(AttributesNamesAndValues.ANCILLARY_VARIABLES);
		
		if (null == ancillaryVariables){
			
			logger.warn("The variable " + variable.getFullName() + " doesn't have the " + AttributesNamesAndValues.ANCILLARY_VARIABLES + " attribute");
			
		} else if (ancillaryVariables.getLength() == 0){
			
			logger.warn("The variable " + variable.getFullName() + " doesn't have ancillary variables");
			
		} else {
			
			return ancillaryVariables.getValues();
		}
		
		String qcVariableName = findQCNameFromVariableName(variable.getFullName());
		
		if (null != qcVariableName){
			ArrayObject.D1 ancillaryVariablesNamesArray = new ArrayObject.D1(String.class, 1);
			ancillaryVariablesNamesArray.set(0, qcVariableName);
			return ancillaryVariablesNamesArray;
		}
			
		return null;
		
	}
	
	/**
	 * Find the quality control variable name form the given {@link Array} of ancillary variable names.
	 * If the {@link Array} contains more than one quality control.
	 * 
	 * @param ancillaryVariablesValues the {@link Array} of ancillary variable names.
	 * @return the first quality control variable name from the {@link Array} of ancillary variable names, if exists. Otherwise return null.
	 */
	private static String findQCAncillaryVariableName(Array ancillaryVariablesValues){
		
		if (null == ancillaryVariablesValues){
			return null;
		}
		
		String ancillaryVariableName = null;
		int i = 0;
		int numberOfQCNames = 0;
		while(i < ancillaryVariablesValues.getSize()){
			
			String ancVarVal = (String) ancillaryVariablesValues.getObject(i).toString();
			if (isQCVariable(ancVarVal)){
				ancillaryVariableName = ancVarVal;
				numberOfQCNames++;
			}
			i++;
			
		}
		
		logger.debug("Ancillary variables names: " + ancillaryVariableName);
		
		if (numberOfQCNames > 1){
			logger.warn("The variable has more than one ancillary variable. Set the first ancillary variable " + ancillaryVariableName + " as the quality control variable");
		}
		
		return ancillaryVariableName;
		
	}
	
	/**
	 * Find the quality control variable name from the given variable name.
	 * 
	 * Quality control variable name must begin with QC_/qc_ or finish with _QC/_qc.
	 * 
	 * @param variableName the variable name.
	 * @return the quality control variable name if exists, otherwise return null.
	 */
	private String findQCNameFromVariableName(String variableName){
		
		Variable qcVaraible = netcdfDataset.findVariable(variableName + QC_SUFFIX);
		if (null != qcVaraible){
			return qcVaraible.getFullName();
		}
		qcVaraible = netcdfDataset.findVariable(variableName + QC_SUFFIX.toUpperCase());
		if (null != qcVaraible){
			return qcVaraible.getFullName();
		}
		qcVaraible = netcdfDataset.findVariable(QC_PREFIX + variableName);
		if (null != qcVaraible){
			return qcVaraible.getFullName();
		}
		qcVaraible = netcdfDataset.findVariable(QC_PREFIX.toUpperCase() + variableName);
		if (null != qcVaraible){
			return qcVaraible.getFullName();
		}
		
		return null;
	}
	
	/**
	 * Read the variable data from the given variable name.
	 * 
	 * @param variableName the variable name.
	 * @return the {@link Array} data of the variable.
	 */
	private Array readVariable(String variableName) {
		
		Variable qcVariable = netcdfDataset.findVariable(variableName);
		
		if (null == qcVariable){
			logger.warn("The variable " + variableName + " isn't in the Netcdf dataset " + netcdfDataset.getLocation());
			return null;
		} else {
			try {
				return qcVariable.read();
			} catch (IOException e) {
				logger.warn("Reading variable " + variableName + " " + e.getMessage());
				return null;
			}
		}

	}
	
	/**
	 * If the variable name is a quality control variable name.
	 * 
	 * The variable is name is a quality control variable name if the begin with QC_/qc_ or finish with _QC/_qc.
	 * 
	 * @param variableName the varaible name.
	 * @return true if is quality control variable name, othrewise return false.
	 */
	public static boolean isQCVariable(String variableName){
		
		if (variableName.toLowerCase().contains(QC_SUFFIX) || variableName.toLowerCase().contains(QC_PREFIX)){
			return true;
		}
		
		return false;
	}
	
	/**
	 * If the position (latitude and longitude variable) at the given index is good data.
	 * 
	 * @param ima the given {@link Index}.
	 * @return true if the position doesn't have quality controls applied or is good position. Otherwise return false.
	 */
	public boolean isGoodDataPosition(Index ima){
		
		int i = ima.getCurrentCounter()[0];
		
		if( null == qcLatVariableArray || null == qcLonVariableArray){
			return true;
		}
		
		Byte qcLatValue = qcLatVariableArray.getByte(i);
		Byte qcLonValue = qcLonVariableArray.getByte(i);
		
		if (qcLatValue.intValue() >= PROBABLY_GOOD_DATA || qcLonValue.intValue() >= PROBABLY_GOOD_DATA){
			return false;
		}
		
		return true;
	}
	
	/**
	 * If the data variable at the given index is good data.
	 * 
	 * @param variable the {@link Variable}.
	 * @param index the given {@link Index}.
	 * @return true if the variable doesn't have quality controls applied or is good data. Otherwise return false.
	 */
	public boolean isGoodDataVariable(Variable variable, Index index){
		
		String qcVariableName = findQCAncillaryVariableName(mapVariableAncillaryVariableNames.get(variable));
		
		if (null == qcVariableName){
			return true;
		}
		
		Array qcVariableArray = readVariable(qcVariableName);
		if (PROBABLY_GOOD_DATA <= qcVariableArray.getInt(index)){
			return false;
		}
		
		return true;
	}

	public List<Variable> getVariableListWithotAncillaryVariables() {
		return variableListWithotAncillaryVariables;
	}

	public Map<Variable, Array> getMapVariableAncillaryVariable() {
		return mapVariableAncillaryVariableNames;
	}

	public List<Variable> getAncillaryVariableList() {
		return ancillaryVariableList;
	}

	public Integer getLastestGoodDataIdx() {
		return latestGoodDataIdx;
	}
	
	public Integer getFirstGoodDataIdx() {
		// TODO Auto-generated method stub
		return firstGoodDataIdx;
	}

}
