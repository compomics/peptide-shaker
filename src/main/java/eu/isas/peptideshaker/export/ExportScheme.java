package eu.isas.peptideshaker.export;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class allows creating a standard output scheme.
 *
 * @author Marc Vaudel
 */
public class ExportScheme implements Serializable {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = -4712918049667194600L;
    /**
     * The name of the scheme.
     */
    private String name;
    /**
     * A boolean indicating whether the scheme can be modified.
     */
    private boolean editable;
    /**
     * The title of the report.
     */
    private String mainTitle = null;
    /**
     * Ordered list of the features in that scheme.
     */
    private ArrayList<String> sectionList = new ArrayList<String>();
    /**
     * Map of the features to export indexed by feature type.
     */
    private HashMap<String, ArrayList<ExportFeature>> exportFeaturesMap = new HashMap<String, ArrayList<ExportFeature>>();
    /**
     * The separator used to separate columns.
     */
    private String separator = "\t";
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes = true;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header = true;
    /**
     * indicates how many lines shall be used to separate sections.
     */
    private int separationLines = 3;
    /**
     * Indicates whether the title of every section shall be included.
     */
    private boolean includeSectionTitles = true;

    /**
     * Constructor.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param sectionList ordered list of the sections included in the report
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     * @param mainTitle the title of the report
     */
    public ExportScheme(String name, boolean editable, ArrayList<String> sectionList, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles, String mainTitle) {
        this(name, editable, sectionList, exportFeatures, separator, indexes, header, separationLines, includeSectionTitles, mainTitle, null);
    }

    /**
     * Constructor allowing either a list of sections or a merged section
     * (depending on sectionFamily being null or not). For more clarity, use the
     * other constructors, dedicated to specific use cases.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param sectionList ordered list of the sections included in the report
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     * @param mainTitle the title of the report
     * @param sectionFamily the section family. If null the sections will be
     * automatically separated based on the feature type. Note, be sure that all
     * features are implemented for this section.
     */
    private ExportScheme(String name, boolean editable, ArrayList<String> sectionList, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles, String mainTitle, String sectionFamily) {
        this.sectionList = sectionList;
        if (sectionFamily == null) {
            for (ExportFeature exportFeature : exportFeatures) {
                String key = exportFeature.getFeatureFamily();
                if (!exportFeaturesMap.containsKey(key)) {
                    exportFeaturesMap.put(key, new ArrayList<ExportFeature>());
                }
                exportFeaturesMap.get(key).add(exportFeature);
                if (!sectionList.contains(key)) {
                    sectionList.add(key);
                }
            }
        } else {
            exportFeaturesMap.put(sectionFamily, exportFeatures);
            this.sectionList = new ArrayList<String>(exportFeaturesMap.keySet());
        }
        this.separator = separator;
        this.indexes = indexes;
        this.separationLines = separationLines;
        this.header = header;
        this.includeSectionTitles = includeSectionTitles;
        this.mainTitle = mainTitle;
        this.name = name;
        this.editable = editable;
    }

    /**
     * Constructor. Here sections will appear in a random order.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     * @param mainTitle the title of the report
     */
    public ExportScheme(String name, boolean editable, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles, String mainTitle) {
        this(name, editable, new ArrayList<String>(), exportFeatures, separator, indexes, header, separationLines, includeSectionTitles, mainTitle, null);
    }

    /**
     * Constructor. This report will not contain any title.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param sectionList ordered list of the sections included in the report
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     */
    public ExportScheme(String name, boolean editable, ArrayList<String> sectionList, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles) {
        this(name, editable, sectionList, exportFeatures, separator, indexes, header, separationLines, includeSectionTitles, null, null);
    }

    /**
     * Constructor. This report will not contain any title and sections will
     * appear in a random order.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     */
    public ExportScheme(String name, boolean editable, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles) {
        this(name, editable, new ArrayList<String>(), exportFeatures, separator, indexes, header, separationLines, includeSectionTitles, null, null);
    }

    /**
     * Constructor. This report will not contain any title and a single section.
     *
     * @param name the name of the scheme
     * @param editable a boolean indicating whether the scheme can be edited by
     * the user
     * @param sectionTitle the section title
     * @param exportFeatures list of features to be included in the report
     * @param separator the column separator to be used
     * @param indexes indicates whether lines shall be indexed
     * @param header indicates whether column headers shall be included
     * @param separationLines the number of lines to use for section separation
     * @param includeSectionTitles indicates whether section titles shall be
     * used
     */
    public ExportScheme(String name, boolean editable, String sectionTitle, ArrayList<ExportFeature> exportFeatures, String separator,
            boolean indexes, boolean header, int separationLines, boolean includeSectionTitles) {
        this(name, editable, new ArrayList<String>(), exportFeatures, separator, indexes, header, separationLines, includeSectionTitles, null, sectionTitle);
    }

    /**
     * Returns the column separator.
     *
     * @return the column separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Indicates whether lines shall be indexed.
     *
     * @return a boolean indicating whether lines shall be indexed
     */
    public boolean isIndexes() {
        return indexes;
    }

    /**
     * Indicates whether column header shall be used.
     *
     * @return a boolean indicating whether column header shall be used
     */
    public boolean isHeader() {
        return header;
    }

    /**
     * Returns the number of lines to be used to separate the sections.
     *
     * @return the number of lines to be used to separate the sections
     */
    public int getSeparationLines() {
        return separationLines;
    }

    /**
     * Indicates whether section titles shall be used.
     *
     * @return a boolean indicating whether section titles shall be used
     */
    public boolean isIncludeSectionTitles() {
        return includeSectionTitles;
    }

    /**
     * returns the list of sections to be included in the scheme.
     *
     * @return the list of sections to be included in the scheme
     */
    public ArrayList<String> getSections() {
        return sectionList;
    }

    /**
     * Returns the export features to be included in the given section.
     *
     * @param section the section of interest
     * @return the list of export features to export in this section
     */
    public ArrayList<ExportFeature> getExportFeatures(String section) {
        return exportFeaturesMap.get(section);
    }

    /**
     * Returns the main title of the report. Null if none.
     *
     * @return the main title of the report.
     */
    public String getMainTitle() {
        return mainTitle;
    }

    /**
     * Returns the name of the scheme.
     *
     * @return the name of the scheme
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the scheme.
     *
     * @param name the name of the scheme
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indicates whether the scheme is editable.
     *
     * @return a boolean indicating whether the scheme is editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets whether the scheme is editable.
     *
     * @param editable a boolean indicating whether the scheme shall be editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
