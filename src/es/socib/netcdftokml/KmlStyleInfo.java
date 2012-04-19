/**
 * 
 */
package es.socib.netcdftokml;

import de.micromata.opengis.kml.v_2_2_0.BalloonStyle;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Style;

/**
 * @author ksebastian
 *
 */
public class KmlStyleInfo {
	
	private static final String DEFAULT_LINE_COLOR ="#DDDDDD";
	
	private static final String DEFAULT_DATE_FORMAT_PATERN = "yyyy-MM-dd HH:mm:ss";
	
	private static final String DEFAULT_HOME_ICON_URL = "http://www.socib.es/images/gl-gohome-128x128.png";

	private static final String DEFAULT_REGULAR_ICON_URL = "http://www.socib.es/images/gl-forward-128x128.png";

	private static final String DEFAULT_END_ICON_URL = "http://www.socib.es/images/gl-down-128x128.png";

	/**
	 * kmlTitleName the kml and placemark description title name.
	 */
	private String kmlTitleName;	
	
	/**
	 * dateFormatPattern the pattern to use to format the date. If null then the default is yyyy-MM-dd HH:mm:ss.
	 */
	private String dateFormatPattern;
	
	/**
	 * lineStyleColor the kml line style color .
	 */
	private String lineStyleColor;
	
	/**
	 * templatePath the template location path to format the description of the kml placemark.
	 */
	private String templatePath;
	
	/**
	 * homeIconUrl the home icon server url or local file. The home icon is set to the first
	 * kml placemark. If null then the default value is http://www.socib.es/images/gl-gohome-128x128.png.
	 */
	private String homeIconUrl;
	
	/**
	 * regularIconUrl the regular icon server url or local file. The regular icon is set to all
	 * kml placemark, except the first and last placemark . If null then the default value is
	 * http://www.socib.es/images/gl-forward-128x128.png.
	 */
	private String regularIconUrl;
	
	/**
	 * endIconUrl the end icon server url or local file. The end icon is set to the last
	 * kml placemark. If null the then default value is http://www.socib.es/images/gl-down-128x128.png.
	 */
	private String endIconUrl;
	
	/**
	 * Construct a new {@link KmlStyleInfo}
	 * 
	 * @param kmlTitleName the kml and placemark description title name
	 * @param dateFormatPattern the pattern to use to format the date. If null the default is yyyy-MM-dd HH:mm:ss.
	 * @param lineStyleColor the kml line style color.
	 * @param templatePath the template location path to format the description of the kml placemark.
	 * @param homeIconUrl the home icon server url or local file. The home icon is set to the first
	 * kml placemark. If null the default value is http://www.socib.es/images/gl-gohome-128x128.png.
	 * @param regularIconUrl the regular icon server url or local file. The regular icon is set to all
	 * kml placemark, except the first and last placemark . If null the default value is.
	 * http://www.socib.es/images/gl-forward-128x128.png.
	 * @param endIconUrl the end icon server url or local file. The end icon is set to the last
	 * kml placemark. If null the default value is http://www.socib.es/images/gl-down-128x128.png.
	 */
	public KmlStyleInfo(String kmlTitleName, String dateFormatPattern, String lineStyleColor, String templatePath, String homeIconUrl, String regularIconUrl, String endIconUrl) {
		this.kmlTitleName = kmlTitleName;
		this.dateFormatPattern = dateFormatPattern;
		if (null == this.dateFormatPattern){
			this.dateFormatPattern =  DEFAULT_DATE_FORMAT_PATERN;
		}
		this.lineStyleColor = lineStyleColor;
		if (null == this.lineStyleColor){
			this.lineStyleColor = DEFAULT_LINE_COLOR;
		}
		this.templatePath = templatePath;
		this.homeIconUrl = homeIconUrl;
		if (null == this.homeIconUrl){
			this.homeIconUrl = DEFAULT_HOME_ICON_URL;
		}
		this.regularIconUrl = regularIconUrl;
		if (null == this.regularIconUrl){
			this.regularIconUrl = DEFAULT_REGULAR_ICON_URL;
		}
		this.endIconUrl = endIconUrl;
		if (null == this.endIconUrl){
			this.endIconUrl = DEFAULT_END_ICON_URL;
		}
	}

	public String getKmlTitleName() {
		return kmlTitleName;
	}

	public String getDateFormatPattern() {
		return dateFormatPattern;
	}

	public String getLineStyleColor() {
		return lineStyleColor;
	}

	public String getTemplatePath() {
		return templatePath;
	}

	public String getHomeIconUrl() {
		return homeIconUrl;
	}

	public String getRegularIconUrl() {
		return regularIconUrl;
	}

	public String getEndIconUrl() {
		return endIconUrl;
	}
	
	public void setKmlDocumentStyle(Document document) {
		
		final Style styleForHomeIcon = new Style();
		document.getStyleSelector().add(styleForHomeIcon);
		styleForHomeIcon.setId("styleForHomeIcon");
		
		final Style styleForRegularIcon = new Style();
		document.getStyleSelector().add(styleForRegularIcon);
		styleForRegularIcon.setId("styleForRegularIcon");
		
		final Style styleForFinalIcon = new Style();
		document.getStyleSelector().add(styleForFinalIcon);
		styleForFinalIcon.setId("styleForFinalIcon");

		//Setting up line style
		final LineStyle linestyle = new LineStyle();
		styleForHomeIcon.setLineStyle(linestyle);
		styleForRegularIcon.setLineStyle(linestyle);
		styleForFinalIcon.setLineStyle(linestyle);
		linestyle.setId("lineStyleId");
		linestyle.setColor(lineStyleColor);
		linestyle.setWidth(4.0d);

		//Setting up balloon style
		final BalloonStyle balloonstyle = new BalloonStyle();
		styleForHomeIcon.setBalloonStyle(balloonstyle);
		styleForRegularIcon.setBalloonStyle(balloonstyle);
		styleForFinalIcon.setBalloonStyle(balloonstyle);
		balloonstyle.setId("ID");
		balloonstyle.setBgColor("ffffffff");
		balloonstyle.setTextColor("ff000000");
		
		//Setting up Home icon style
		final IconStyle homeIconStyle = new IconStyle();
		styleForHomeIcon.setIconStyle(homeIconStyle);
		homeIconStyle.setColor("ffffffff");
		homeIconStyle.setScale(1.1d);
		final Icon icon1 = new Icon();
		icon1.setHref(homeIconUrl);
		homeIconStyle.setIcon(icon1);
		
		//Setting up icon style
		final IconStyle regularIconStyle = new IconStyle();
		styleForRegularIcon.setIconStyle(regularIconStyle);
		regularIconStyle.setColor("ffffffff");
		regularIconStyle.setScale(1.1d);
		final Icon icon2 = new Icon();
		icon2.setHref(regularIconUrl);
		regularIconStyle.setIcon(icon2);
		
		//Setting up icon style
		final IconStyle finalIconStyle = new IconStyle();
		styleForFinalIcon.setIconStyle(finalIconStyle);
		finalIconStyle.setColor("ffffffff");
		finalIconStyle.setScale(1.1d);
		final Icon icon3 = new Icon();
		icon3.setHref(endIconUrl);
		finalIconStyle.setIcon(icon3);
		
	}

	public void setKmlTitleName(String kmlTitleName) {
		this.kmlTitleName = kmlTitleName;
	}

	public void setDateFormatPattern(String dateFormatPattern) {
		this.dateFormatPattern = dateFormatPattern;
	}

	public void setLineStyleColor(String lineStyleColor) {
		this.lineStyleColor = lineStyleColor;
	}

	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}

	public void setHomeIconUrl(String homeIconUrl) {
		this.homeIconUrl = homeIconUrl;
	}

	public void setRegularIconUrl(String regularIconUrl) {
		this.regularIconUrl = regularIconUrl;
	}

	public void setEndIconUrl(String endIconUrl) {
		this.endIconUrl = endIconUrl;
	}

	@Override
	public String toString() {
		return "KmlStyleInfo [kmlTitleName=" + kmlTitleName
				+ ", dateFormatPattern=" + dateFormatPattern
				+ ", lineStyleColor=" + lineStyleColor + ", templatePath="
				+ templatePath + ", homeIconUrl=" + homeIconUrl
				+ ", regularIconUrl=" + regularIconUrl + ", endIconUrl="
				+ endIconUrl + "]";
	}
	
	
}
