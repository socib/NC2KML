package es.socib.netcdftokml;

public class AdditionalInfo {

	/**
	 * deploymentInfo the {@link DeploymentInfo} with the initial deployment time,
	 * 	latitude and longitude.
	 */
	private DeploymentInfo deploymentInfo;
	
	/**
	 * kmlStyleInfo the {@link KmlStyleInfo} with the kml style information.
	 */
	private KmlStyleInfo kmlStyleInfo;
	
	/**
	 * threddsLink the opendap link of the kml data.
	 */
	private String threddsLink;
	
	/**
	 * displayAncillaryVariables indicates if the ancillary variables must be displayed
	 * in the kml placemark, except quality controls variables.
	 */
	private boolean displayAncillaryVariables;

	/**
	 * Construct a new {@link AdditionalInfo}.
	 * 
	 * @param deploymentInfo the {@link DeploymentInfo} with the initial deployment time, latitude and longitude.
	 * @param threddsLink the {@link KmlStyleInfo} with the kml style information.
	 * @param kmlTitleName the opendap link of the kml data.
	 * @param displayAncillaryVariables indicates if the ancillary variables must be displayed
	 * in the kml placemark, except quality controls variables.
	 */
	public AdditionalInfo(DeploymentInfo deploymentInfo, KmlStyleInfo kmlStyleInfo, String threddsLink, boolean displayAncillaryVariables) {
		this.deploymentInfo = deploymentInfo;
		this.threddsLink = threddsLink;
		this.displayAncillaryVariables = displayAncillaryVariables;
		this.kmlStyleInfo = kmlStyleInfo;
	}

	public DeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

	public String getThreddsLink() {
		return threddsLink;
	}
	
	public boolean displayAncillaryVariables() {
		return displayAncillaryVariables;
	}

	public KmlStyleInfo getKmlStyleInfo() {
		return kmlStyleInfo;
	}

	public void setKmlStyleInfo(KmlStyleInfo kmlStyleInfo) {
		this.kmlStyleInfo = kmlStyleInfo;
	}

	public void setDisplayAncillaryVariables(boolean displayAncillaryVariables) {
		this.displayAncillaryVariables = displayAncillaryVariables;
	}

	public void setDeploymentInfo(DeploymentInfo deploymentInfo) {
		this.deploymentInfo = deploymentInfo;
	}

	public void setThreddsLink(String threddsLink) {
		this.threddsLink = threddsLink;
	}

	@Override
	public String toString() {
		return "AdditionalInfo [" +
				"\n     deploymentInfo=" + deploymentInfo +
				"\n     kmlStyleInfo=" + kmlStyleInfo + 
				"\n     threddsLink=" + threddsLink + 
				"\n     displayAncillaryVariables=" + displayAncillaryVariables + "]";
	}
	
}
