package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.export.exportfeatures.AnnotationFeatures;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import eu.isas.peptideshaker.export.exportfeatures.InputFilterFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ProjectFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PtmScoringFeatures;
import eu.isas.peptideshaker.export.exportfeatures.SearchFeatures;
import eu.isas.peptideshaker.export.exportfeatures.SpectrumCountingFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures;
import eu.isas.peptideshaker.export.sections.AnnotationSection;
import eu.isas.peptideshaker.export.sections.InputFilterSection;
import eu.isas.peptideshaker.export.sections.PeptideSection;
import eu.isas.peptideshaker.export.sections.ProjectSection;
import eu.isas.peptideshaker.export.sections.ProteinSection;
import eu.isas.peptideshaker.export.sections.PsmSection;
import eu.isas.peptideshaker.export.sections.PtmScoringSection;
import eu.isas.peptideshaker.export.sections.SearchParametersSection;
import eu.isas.peptideshaker.export.sections.SpectrumCountingSection;
import eu.isas.peptideshaker.export.sections.ValidationSection;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This factory is used to manage and generate reports.
 *
 * @author Marc Vaudel
 */
public class ExportFactory implements Serializable {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = 1979509878742026942L;
    /**
     * The instance of the factory.
     */
    private static ExportFactory instance = null;
    /**
     * User defined factory containing the user schemes.
     */
    private static final String SERIALIZATION_FILE = System.getProperty("user.home") + "/.peptideshaker/exportFactory.cus";
    /**
     * The user export schemes.
     */
    private HashMap<String, ExportScheme> userSchemes = new HashMap<String, ExportScheme>();
    /**
     * Sorted list of the implemented reports.
     */
    private ArrayList<String> implementedReports = null;

    /**
     * Constructor.
     */
    private ExportFactory() {
    }

    /**
     * Static method to get the instance of the factory.
     *
     * @return the instance of the factory
     */
    public static ExportFactory getInstance() {
        if (instance == null) {
            try {
                File savedFile = new File(SERIALIZATION_FILE);
                instance = (ExportFactory) SerializationUtils.readObject(savedFile);
            } catch (Exception e) {
                instance = new ExportFactory();
                try {
                    instance.saveFactory();
                } catch (IOException ioe) {
                    // cancel save
                    ioe.printStackTrace();
                }
            }
        }
        return instance;
    }

    /**
     * Saves the factory in the user folder.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * saving the ptmFactory
     */
    public void saveFactory() throws IOException {
        File factoryFile = new File(SERIALIZATION_FILE);
        if (!factoryFile.getParentFile().exists()) {
            factoryFile.getParentFile().mkdir();
        }
        SerializationUtils.writeObject(instance, factoryFile);
    }

    /**
     * Returns a list of the name of the available user schemes.
     *
     * @return a list of the implemented user schemes
     */
    public ArrayList<String> getUserSchemesNames() {
        return new ArrayList<String>(userSchemes.keySet());
    }

    /**
     * Returns the export scheme indexed by the given name.
     *
     * @param schemeName the name of the desired export scheme
     * @return the desired export scheme
     */
    public ExportScheme getExportScheme(String schemeName) {
        ExportScheme exportScheme = userSchemes.get(schemeName);
        if (exportScheme == null) {
            exportScheme = getDefaultExportSchemes().get(schemeName);
        }
        return exportScheme;
    }

    /**
     * Removes a user scheme.
     *
     * @param schemeName the name of the scheme to remove
     */
    public void removeExportScheme(String schemeName) {
        userSchemes.remove(schemeName);
    }

    /**
     * Adds an export scheme to the map of user schemes.
     *
     * @param exportScheme the new export scheme, will be accessible via its
     * name
     */
    public void addExportScheme(ExportScheme exportScheme) {
        userSchemes.put(exportScheme.getName(), exportScheme);
    }

    /**
     * Returns a list of the default export schemes.
     *
     * @return a list of the default export schemes
     */
    public ArrayList<String> getDefaultExportSchemesNames() {
        ArrayList<String> result = new ArrayList<String>(getDefaultExportSchemes().keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * Writes the desired export in text format. If an argument is not needed,
     * provide null (at your own risks).
     *
     * @param exportScheme the scheme of the export
     * @param destinationFile the destination file
     * @param experiment the experiment corresponding to this project (mandatory
     * for the Project section)
     * @param sample the sample of the project (mandatory for the Project
     * section)
     * @param replicateNumber the replicate number of the project (mandatory for
     * the Project section)
     * @param projectDetails the project details (mandatory for the Project
     * section)
     * @param identification the identification (mandatory for the Protein,
     * Peptide and PSM sections)
     * @param identificationFeaturesGenerator the identification features
     * generator (mandatory for the Protein, Peptide and PSM sections)
     * @param searchParameters the search parameters (mandatory for the Protein,
     * Peptide, PSM and search parameters sections)
     * @param proteinKeys the protein keys to export (mandatory for the Protein
     * section)
     * @param peptideKeys the peptide keys to export (mandatory for the Peptide
     * section)
     * @param psmKeys the keys of the PSMs to export (mandatory for the PSM
     * section)
     * @param proteinMatchKey the protein match key when exporting peptides from
     * a single protein match (optional for the Peptide sections)
     * @param nSurroundingAA the number of surrounding amino acids to export
     * (mandatory for the Peptide section)
     * @param annotationPreferences the annotation preferences (mandatory for
     * the Annotation section)
     * @param idFilter the identification filer (mandatory for the Input Filter
     * section)
     * @param ptmcoringPreferences the PTM scoring preferences (mandatory for
     * the PTM scoring section)
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void writeExport(ExportScheme exportScheme, File destinationFile, String experiment, String sample, int replicateNumber,
            ProjectDetails projectDetails, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, ArrayList<String> proteinKeys, ArrayList<String> peptideKeys, ArrayList<String> psmKeys,
            String proteinMatchKey, int nSurroundingAA, AnnotationPreferences annotationPreferences, IdFilter idFilter,
            PTMScoringPreferences ptmcoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler) 
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        // @TODO: implement other formats, put sometimes text instead of tables

        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            writer.write(mainTitle);
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }

        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                writer.write(sectionName);
                writer.newLine();
            }
            if (sectionName.equals(AnnotationFeatures.type)) {
                AnnotationSection section = new AnnotationSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(annotationPreferences, waitingHandler);
            } else if (sectionName.equals(InputFilterFeatures.type)) {
                InputFilterSection section = new InputFilterSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(idFilter, waitingHandler);
            } else if (sectionName.equals(PeptideFeatures.type)) {
                PeptideSection section = new PeptideSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, peptideKeys, nSurroundingAA, "", waitingHandler);
            } else if (sectionName.equals(ProjectFeatures.type)) {
                ProjectSection section = new ProjectSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(experiment, sample, replicateNumber, projectDetails, waitingHandler);
            } else if (sectionName.equals(ProteinFeatures.type)) {
                ProteinSection section = new ProteinSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, psmKeys, nSurroundingAA, waitingHandler);
            } else if (sectionName.equals(PsmFeatures.type)) {
                PsmSection section = new PsmSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, psmKeys, "", waitingHandler);
            } else if (sectionName.equals(PtmScoringFeatures.type)) {
                PtmScoringSection section = new PtmScoringSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(ptmcoringPreferences, waitingHandler);
            } else if (sectionName.equals(SearchFeatures.type)) {
                SearchParametersSection section = new SearchParametersSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(searchParameters, waitingHandler);
            } else if (sectionName.equals(SpectrumCountingFeatures.type)) {
                SpectrumCountingSection section = new SpectrumCountingSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(spectrumCountingPreferences, waitingHandler);
            } else if (sectionName.equals(ValidationFeatures.type)) {
                ValidationSection section = new ValidationSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                PSMaps psMaps = new PSMaps();
                psMaps = (PSMaps) identification.getUrParam(psMaps);
                section.writeSection(psMaps, waitingHandler);
            } else {
                writer.write("Section " + sectionName + " not implemented in the ExportFactory.");
            }

            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }

        writer.close();
    }

    /**
     * Writes the documentation related to a report.
     *
     * @param exportScheme the export scheme of the report
     * @param destinationFile the destination file where to write the
     * documentation
     * @throws IOException
     */
    public static void writeDocumentation(ExportScheme exportScheme, File destinationFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            writer.write(mainTitle);
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }
        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                writer.write(sectionName);
                writer.newLine();
            }
            for (ExportFeature exportFeature : exportScheme.getExportFeatures(sectionName)) {
                writer.write(exportFeature.getTitle() + exportScheme.getSeparator() + exportFeature.getDescription());
                writer.newLine();
            }
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }
        writer.close();
    }

    /**
     * Writes section separation lines using the given writer.
     *
     * @param writer the writer
     * @param nSeparationLines the number of separation lines to write
     * @throws IOException
     */
    private static void writeSeparationLines(BufferedWriter writer, int nSeparationLines) throws IOException {
        for (int i = 1; i <= nSeparationLines; i++) {
            writer.newLine();
        }
    }

    /**
     * Returns the implemented sections.
     *
     * @return the implemented sections
     */
    public static ArrayList<String> getImplementedSections() {
        ArrayList<String> result = new ArrayList<String>();
        result.add(AnnotationFeatures.type);
        result.add(InputFilterFeatures.type);
        result.add(ProteinFeatures.type);
        result.add(PeptideFeatures.type);
        result.add(PsmFeatures.type);
        result.add(FragmentFeatures.type);
        result.add(ProjectFeatures.type);
        result.add(PtmScoringFeatures.type);
        result.add(SearchFeatures.type);
        result.add(SpectrumCountingFeatures.type);
        result.add(ValidationFeatures.type);
        return result;
    }

    /**
     * Returns the list of implemented reports as command line option.
     *
     * @return the list of implemented reports
     */
    public String getCommandLineOptions() {
        setUpReportList();
        String options = "";
        for (int i = 0; i < implementedReports.size(); i++) {
            if (!options.equals("")) {
                options += ", ";
            }
            options += i + ": " + implementedReports.get(i);
        }
        return options;
    }

    /**
     * Returns the default file name for the export of a report based on the
     * project details
     *
     * @param experiment the experiment of the project
     * @param sample the sample of the project
     * @param replicate the replicate number
     * @param exportName the name of the report type
     * @return the default file name for the export
     */
    public static String getDefaultReportName(String experiment, String sample, int replicate, String exportName) {
        return experiment + "_" + sample + "_" + replicate + "_" + exportName + ".txt";
    }

    /**
     * Returns the default file name for the export of the documentation of the
     * given report export type.
     *
     * @param exportName the export name
     * @return the default file name for the export
     */
    public static String getDefaultDocumentation(String exportName) {
        return exportName + "_documentation.txt";
    }

    /**
     * Returns the export type based on the number used in command line.
     *
     * @param commandLine the number used in command line option. See
     * getCommandLineOptions().
     * @return the corresponding export name
     */
    public String getExportTypeFromCommandLineOption(int commandLine) {
        if (implementedReports == null) {
            setUpReportList();
        }
        if (commandLine >= implementedReports.size()) {
            throw new IllegalArgumentException("Unrecognized report type: " + commandLine + ". Available reports are: " + getCommandLineOptions() + ".");
        }
        return implementedReports.get(commandLine);
    }

    /**
     * Initiates the sorted list of implemented reports.
     */
    private void setUpReportList() {
        implementedReports = new ArrayList<String>();
        implementedReports.addAll(getDefaultExportSchemesNames());
        ArrayList<String> userReports = new ArrayList<String>(userSchemes.keySet());
        Collections.sort(userReports);
        implementedReports.addAll(userReports);
    }

    /**
     * Returns the default schemes available.
     *
     * @return a list containing the default schemes
     */
    private static HashMap<String, ExportScheme> getDefaultExportSchemes() {

        ///////////////////////////
        // Default top down report
        ///////////////////////////
        HashMap<String, ArrayList<ExportFeature>> exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        ArrayList<ExportFeature> sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(ProteinFeatures.accession);
        sectionContent.add(ProteinFeatures.protein_description);
        sectionContent.add(ProteinFeatures.pi);
        sectionContent.add(ProteinFeatures.other_proteins);
        sectionContent.add(ProteinFeatures.protein_group);

        // peptide and spectrum counts
        sectionContent.add(ProteinFeatures.peptides);
        sectionContent.add(ProteinFeatures.validated_peptides);
        sectionContent.add(ProteinFeatures.unique_peptides);
        sectionContent.add(ProteinFeatures.psms);
        sectionContent.add(ProteinFeatures.validated_psms);

        // protein coverage
        sectionContent.add(ProteinFeatures.coverage);
        sectionContent.add(ProteinFeatures.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(ProteinFeatures.mw);
        sectionContent.add(ProteinFeatures.spectrum_counting_nsaf);
        sectionContent.add(ProteinFeatures.spectrum_counting_empai);

        // variable_ptms
        sectionContent.add(ProteinFeatures.confident_PTMs);
        sectionContent.add(ProteinFeatures.other_PTMs);

        // protein scores
        sectionContent.add(ProteinFeatures.score);
        sectionContent.add(ProteinFeatures.confidence);
        sectionContent.add(ProteinFeatures.decoy);
        sectionContent.add(ProteinFeatures.validated);

        // Peptide sub-section
        // accessions
        sectionContent.add(PeptideFeatures.accessions);

        // peptide sequence
        sectionContent.add(PeptideFeatures.aaBefore);
        sectionContent.add(PeptideFeatures.sequence);
        sectionContent.add(PeptideFeatures.aaAfter);

        // variable_ptms
        sectionContent.add(PeptideFeatures.variable_ptms);
        sectionContent.add(PeptideFeatures.localization_confidence);
        sectionContent.add(PeptideFeatures.fixed_ptms);

        // psms
        sectionContent.add(PeptideFeatures.validated_psms);
        sectionContent.add(PeptideFeatures.psms);

        // peptide scores
        sectionContent.add(PeptideFeatures.score);
        sectionContent.add(PeptideFeatures.confidence);
        sectionContent.add(PeptideFeatures.decoy);
        sectionContent.add(PeptideFeatures.validated);

        // PSM sub-section
        // protein accessions
        sectionContent.add(PsmFeatures.accessions);
        sectionContent.add(PsmFeatures.sequence);

        // ptms
        sectionContent.add(PsmFeatures.modified_sequence);
        sectionContent.add(PsmFeatures.variable_ptms);
        sectionContent.add(PsmFeatures.d_score);
        sectionContent.add(PsmFeatures.a_score);
        sectionContent.add(PsmFeatures.localization_confidence);
        sectionContent.add(PsmFeatures.fixed_ptms);

        // spectrum file
        sectionContent.add(PsmFeatures.spectrum_file);
        sectionContent.add(PsmFeatures.spectrum_title);
        sectionContent.add(PsmFeatures.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsmFeatures.rt);
        sectionContent.add(PsmFeatures.mz);
        sectionContent.add(PsmFeatures.spectrum_charge);
        sectionContent.add(PsmFeatures.identification_charge);
        sectionContent.add(PsmFeatures.theoretical_mass);
        sectionContent.add(PsmFeatures.isotope);
        sectionContent.add(PsmFeatures.mz_error);

        // psm scores
        sectionContent.add(PsmFeatures.score);
        sectionContent.add(PsmFeatures.confidence);
        sectionContent.add(PsmFeatures.decoy);
        sectionContent.add(PsmFeatures.validated);
        
        exportFeatures.put(ProteinFeatures.type, sectionContent);

        ExportScheme topDownReport = new ExportScheme("Default Top Down Report", false, exportFeatures, "\t", true, true, 0, false);


        ///////////////////////////
        // Default protein report
        ///////////////////////////
        
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(ProteinFeatures.accession);
        sectionContent.add(ProteinFeatures.protein_description);
        sectionContent.add(ProteinFeatures.gene_name);
        sectionContent.add(ProteinFeatures.chromosome);
        sectionContent.add(ProteinFeatures.pi);
        sectionContent.add(ProteinFeatures.other_proteins);
        sectionContent.add(ProteinFeatures.protein_group);

        // peptide and spectrum counts
        sectionContent.add(ProteinFeatures.peptides);
        sectionContent.add(ProteinFeatures.validated_peptides);
        sectionContent.add(ProteinFeatures.unique_peptides);
        sectionContent.add(ProteinFeatures.psms);
        sectionContent.add(ProteinFeatures.validated_psms);

        // protein coverage
        sectionContent.add(ProteinFeatures.coverage);
        sectionContent.add(ProteinFeatures.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(ProteinFeatures.mw);
        sectionContent.add(ProteinFeatures.spectrum_counting_nsaf);
        sectionContent.add(ProteinFeatures.spectrum_counting_empai);

        // variable_ptms
        sectionContent.add(ProteinFeatures.confident_PTMs);
        sectionContent.add(ProteinFeatures.other_PTMs);

        // protein scores
        sectionContent.add(ProteinFeatures.score);
        sectionContent.add(ProteinFeatures.confidence);
        sectionContent.add(ProteinFeatures.decoy);
        sectionContent.add(ProteinFeatures.validated);
        
        exportFeatures.put(ProteinFeatures.type, sectionContent);

        ExportScheme proteinReport = new ExportScheme("Default Protein Report", false, exportFeatures, "\t", true, true, 0, false);


        ///////////////////////////
        // Default peptide report
        ///////////////////////////
        
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // accessions
        sectionContent.add(PeptideFeatures.accessions);

        // peptide sequence
        sectionContent.add(PeptideFeatures.aaBefore);
        sectionContent.add(PeptideFeatures.sequence);
        sectionContent.add(PeptideFeatures.aaAfter);

        // variable_ptms
        sectionContent.add(PeptideFeatures.modified_sequence);
        sectionContent.add(PeptideFeatures.variable_ptms);
        sectionContent.add(PeptideFeatures.localization_confidence);
        sectionContent.add(PeptideFeatures.fixed_ptms);

        // psms
        sectionContent.add(PeptideFeatures.validated_psms);
        sectionContent.add(PeptideFeatures.psms);

        // peptide scores
        sectionContent.add(PeptideFeatures.score);
        sectionContent.add(PeptideFeatures.confidence);
        sectionContent.add(PeptideFeatures.decoy);
        sectionContent.add(PeptideFeatures.validated);
        
        exportFeatures.put(PeptideFeatures.type, sectionContent);

        ExportScheme peptideReport = new ExportScheme("Default Peptide Report", false, exportFeatures, "\t", true, true, 0, false);


        ///////////////////////////
        // Default PSM report
        ///////////////////////////
        
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions
        sectionContent.add(PsmFeatures.accessions);
        sectionContent.add(PsmFeatures.sequence);

        // ptms
        sectionContent.add(PsmFeatures.variable_ptms);
        sectionContent.add(PsmFeatures.d_score);
        sectionContent.add(PsmFeatures.a_score);
        sectionContent.add(PsmFeatures.localization_confidence);
        sectionContent.add(PsmFeatures.fixed_ptms);

        // spectrum file
        sectionContent.add(PsmFeatures.spectrum_file);
        sectionContent.add(PsmFeatures.spectrum_title);
        sectionContent.add(PsmFeatures.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsmFeatures.rt);
        sectionContent.add(PsmFeatures.mz);
        sectionContent.add(PsmFeatures.spectrum_charge);
        sectionContent.add(PsmFeatures.identification_charge);
        sectionContent.add(PsmFeatures.theoretical_mass);
        sectionContent.add(PsmFeatures.isotope);
        sectionContent.add(PsmFeatures.mz_error);

        // psm scores
        sectionContent.add(PsmFeatures.score);
        sectionContent.add(PsmFeatures.confidence);
        sectionContent.add(PsmFeatures.decoy);
        sectionContent.add(PsmFeatures.validated);
        
        exportFeatures.put(PeptideFeatures.type, sectionContent);

        ExportScheme psmReport = new ExportScheme("Default PSM Report", false, exportFeatures, "\t", true, true, 0, false);


        ///////////////////////////
        // Certificate of analysis
        ///////////////////////////
        
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
                ArrayList<String> sectionsList = new ArrayList<String>();

        // project details
        sectionContent = new ArrayList<ExportFeature>();
        sectionsList.add(ProjectFeatures.type);
        sectionContent.add(ProjectFeatures.peptide_shaker);
        sectionContent.add(ProjectFeatures.date);
        sectionContent.add(ProjectFeatures.experiment);
        sectionContent.add(ProjectFeatures.sample);
        sectionContent.add(ProjectFeatures.replicate);
        sectionContent.add(ProjectFeatures.search_engines);
        exportFeatures.put(ProjectFeatures.type, sectionContent);

        // search parameters
        sectionsList.add(SearchFeatures.type);
        exportFeatures.put(SearchFeatures.type, SearchFeatures.values()[0].getExportFeatures());

        // input filters
        sectionsList.add(InputFilterFeatures.type);
        exportFeatures.put(InputFilterFeatures.type, InputFilterFeatures.values()[0].getExportFeatures());

        // validation details
        sectionsList.add(ValidationFeatures.type);
        exportFeatures.put(ValidationFeatures.type, ValidationFeatures.values()[0].getExportFeatures());

        // ptms
        sectionsList.add(PtmScoringFeatures.type);
        exportFeatures.put(PtmScoringFeatures.type, PtmScoringFeatures.values()[0].getExportFeatures());

        // spectrum counting details
        sectionsList.add(SpectrumCountingFeatures.type);
        exportFeatures.put(SpectrumCountingFeatures.type, SpectrumCountingFeatures.values()[0].getExportFeatures());

        // annotation settings
        sectionsList.add(AnnotationFeatures.type);
        exportFeatures.put(AnnotationFeatures.type, AnnotationFeatures.values()[0].getExportFeatures());
        
        ExportScheme coa = new ExportScheme("Certificate of Analysis", false, sectionsList, exportFeatures, ": ", true, false, 2, true);

        HashMap<String, ExportScheme> defaultSchemes = new HashMap<String, ExportScheme>();
        defaultSchemes.put(topDownReport.getName(), topDownReport);
        defaultSchemes.put(proteinReport.getName(), proteinReport);
        defaultSchemes.put(peptideReport.getName(), peptideReport);
        defaultSchemes.put(psmReport.getName(), psmReport);
        defaultSchemes.put(coa.getName(), coa);
        return defaultSchemes;
    }
}
