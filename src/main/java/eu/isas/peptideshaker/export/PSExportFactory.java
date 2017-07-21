package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.export.exportfeatures.PsAnnotationFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsInputFilterFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsProjectFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsProteinFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPtmScoringFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsSearchFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsSpectrumCountingFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsValidationFeature;
import eu.isas.peptideshaker.export.sections.PsAnnotationSection;
import eu.isas.peptideshaker.export.sections.PsIdentificationAlgorithmMatchesSection;
import eu.isas.peptideshaker.export.sections.PsInputFilterSection;
import eu.isas.peptideshaker.export.sections.PsPeptideSection;
import eu.isas.peptideshaker.export.sections.PsProjectSection;
import eu.isas.peptideshaker.export.sections.PsProteinSection;
import eu.isas.peptideshaker.export.sections.PsPsmSection;
import eu.isas.peptideshaker.export.sections.PsPtmScoringSection;
import eu.isas.peptideshaker.export.sections.PsSearchParametersSection;
import eu.isas.peptideshaker.export.sections.PsSpectrumCountingSection;
import eu.isas.peptideshaker.export.sections.PsValidationSection;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The PeptideShaker export factory
 *
 * @author Marc Vaudel
 */
public class PSExportFactory implements ExportFactory {

    /**
     * Serial number for backward compatibility.
     */
    static final long serialVersionUID = 1979509878742026942L;
    /**
     * The instance of the factory.
     */
    private static PSExportFactory instance = null;
    /**
     * User defined factory containing the user schemes.
     */
    private static String SERIALIZATION_FILE = System.getProperty("user.home") + "/.peptideshaker/exportFactory.cus";
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
    private PSExportFactory() {
    }

    /**
     * Static method to get the instance of the factory.
     *
     * @return the instance of the factory
     */
    public static PSExportFactory getInstance() {
        if (instance == null) {
            try {
                File savedFile = new File(SERIALIZATION_FILE);
                instance = (PSExportFactory) SerializationUtils.readObject(savedFile);
            } catch (Exception e) {
                e.getMessage(); // print the message to the error log
                instance = new PSExportFactory();
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
     * Returns the desired default export scheme. Null if not found.
     *
     * @param schemeName the name of the default export scheme
     *
     * @return the default export scheme
     */
    public static ExportScheme getDefaultExportScheme(String schemeName) {
        return getDefaultExportSchemes().get(schemeName);
    }

    @Override
    public ExportScheme getExportScheme(String schemeName) {
        ExportScheme exportScheme = userSchemes.get(schemeName);
        if (exportScheme == null) {
            exportScheme = getDefaultExportSchemes().get(schemeName);
        }
        return exportScheme;
    }

    @Override
    public void removeExportScheme(String schemeName) {
        userSchemes.remove(schemeName);
    }

    @Override
    public void addExportScheme(ExportScheme exportScheme) {
        userSchemes.put(exportScheme.getName(), exportScheme);
    }

    @Override
    public ArrayList<String> getImplementedSections() {
        ArrayList<String> result = new ArrayList<String>();
        result.add(PsProteinFeature.type);
        result.add(PsPeptideFeature.type);
        result.add(PsPsmFeature.type);
        result.add(PsIdentificationAlgorithmMatchesFeature.type);
        result.add(PsAnnotationFeature.type);
        result.add(PsInputFilterFeature.type);
        result.add(PsProjectFeature.type);
        result.add(PsPtmScoringFeature.type);
        result.add(PsSearchFeature.type);
        result.add(PsSpectrumCountingFeature.type);
        result.add(PsValidationFeature.type);
        return result;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(String sectionName, boolean includeSubFeatures) {
        if (sectionName.equals(PsAnnotationFeature.type)) {
            return PsAnnotationFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsInputFilterFeature.type)) {
            return PsInputFilterFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsPeptideFeature.type)) {
            return PsPeptideFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsProjectFeature.type)) {
            return PsProjectFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsProteinFeature.type)) {
            return PsProteinFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsPsmFeature.type)) {
            return PsPsmFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsPtmScoringFeature.type)) {
            return PsPtmScoringFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsSearchFeature.type)) {
            return PsSearchFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsSpectrumCountingFeature.type)) {
            return PsSpectrumCountingFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsValidationFeature.type)) {
            return PsValidationFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsIdentificationAlgorithmMatchesFeature.type)) {
            return PsIdentificationAlgorithmMatchesFeature.values()[0].getExportFeatures(includeSubFeatures);
        }
        return new ArrayList<ExportFeature>();
    }

    /**
     * Returns a list of the default export schemes.
     *
     * @return a list of the default export schemes
     */
    public static ArrayList<String> getDefaultExportSchemesNames() {
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
     * @param exportFormat the format of export to use
     * @param experiment the experiment corresponding to this project (mandatory
     * for the Project section)
     * @param projectDetails the project details (mandatory for the Project
     * section)
     * @param identification the identification (mandatory for the Protein,
     * Peptide and PSM sections)
     * @param identificationFeaturesGenerator the identification features
     * generator (mandatory for the Protein, Peptide and PSM sections)
     * @param geneMaps the gene maps
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
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while interacting with the database
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an exception occurred while estimating the theoretical coverage of a
     * protein
     */
    public static void writeExport(ExportScheme exportScheme, File destinationFile, ExportFormat exportFormat, String experiment,
            ProjectDetails projectDetails, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps,
            ArrayList<String> proteinKeys, ArrayList<String> peptideKeys, ArrayList<String> psmKeys,
            String proteinMatchKey, int nSurroundingAA, IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(exportFormat, destinationFile, exportScheme.getSeparator(), exportScheme.getSeparationLines());
        if (exportWriter instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter) exportWriter;
            PsExportStyle exportStyle = PsExportStyle.getReportStyle(excelWriter);
            excelWriter.setWorkbookStyle(exportStyle);
        }

        exportWriter.writeMainTitle(exportScheme.getMainTitle());

        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                exportWriter.startNewSection(sectionName);
            } else {
                exportWriter.startNewSection();
            }
            if (sectionName.equals(PsAnnotationFeature.type)) {
                PsAnnotationSection section = new PsAnnotationSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getAnnotationPreferences(), waitingHandler);
            } else if (sectionName.equals(PsInputFilterFeature.type)) {
                PsInputFilterSection section = new PsInputFilterSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getPeptideAssumptionFilter(), waitingHandler);
            } else if (sectionName.equals(PsPeptideFeature.type)) {
                PsPeptideSection section = new PsPeptideSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, identificationParameters, peptideKeys, nSurroundingAA, "", exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsProjectFeature.type)) {
                PsProjectSection section = new PsProjectSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(experiment, projectDetails, waitingHandler);
            } else if (sectionName.equals(PsProteinFeature.type)) {
                PsProteinSection section = new PsProteinSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, geneMaps, identificationParameters, psmKeys, nSurroundingAA, exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsPsmFeature.type)) {
                PsPsmSection section = new PsPsmSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, identificationParameters, psmKeys, "", nSurroundingAA, exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsIdentificationAlgorithmMatchesFeature.type)) {
                PsIdentificationAlgorithmMatchesSection section = new PsIdentificationAlgorithmMatchesSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, identificationParameters, psmKeys, "", nSurroundingAA, waitingHandler);
            } else if (sectionName.equals(PsPtmScoringFeature.type)) {
                PsPtmScoringSection section = new PsPtmScoringSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getPtmScoringPreferences(), waitingHandler);
            } else if (sectionName.equals(PsSearchFeature.type)) {
                PsSearchParametersSection section = new PsSearchParametersSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getSearchParameters(), waitingHandler);
            } else if (sectionName.equals(PsSpectrumCountingFeature.type)) {
                PsSpectrumCountingSection section = new PsSpectrumCountingSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(spectrumCountingPreferences, waitingHandler);
            } else if (sectionName.equals(PsValidationFeature.type)) {
                PsValidationSection section = new PsValidationSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                PSMaps psMaps = new PSMaps();
                psMaps = (PSMaps) identification.getUrParam(psMaps);
                section.writeSection(psMaps, identificationParameters, waitingHandler);
            } else {
                throw new UnsupportedOperationException("Section " + sectionName + " not implemented.");
            }
        }

        exportWriter.close();
    }

    /**
     * Writes the documentation related to a report.
     *
     * @param exportScheme the export scheme of the report
     * @param exportFormat the export format chosen by the user
     * @param destinationFile the destination file where to write the
     * documentation
     *
     * @throws IOException thrown if an IOException occurs
     */
    public static void writeDocumentation(ExportScheme exportScheme, ExportFormat exportFormat, File destinationFile) throws IOException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(exportFormat, destinationFile, exportScheme.getSeparator(), exportScheme.getSeparationLines());
        if (exportWriter instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter) exportWriter;
            PsExportStyle exportStyle = PsExportStyle.getReportStyle(excelWriter); //@TODO use another style?
            excelWriter.setWorkbookStyle(exportStyle);
        }

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            exportWriter.writeMainTitle(mainTitle);
        }
        for (String sectionName : exportScheme.getSections()) {
            exportWriter.startNewSection(sectionName);
            if (exportScheme.isIncludeSectionTitles()) {
                exportWriter.write(sectionName);
                exportWriter.newLine();
            }
            for (ExportFeature exportFeature : exportScheme.getExportFeatures(sectionName)) {
                exportWriter.write(exportFeature.getTitle());
                exportWriter.addSeparator();
                exportWriter.write(exportFeature.getDescription());
                exportWriter.newLine();
            }
        }
        exportWriter.close();
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
     * @param exportName the name of the report type
     * @return the default file name for the export
     */
    public static String getDefaultReportName(String experiment, String exportName) {
        return experiment + "_" + exportName + ".txt";
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
        // Default hierarchical report
        ///////////////////////////
        HashMap<String, ArrayList<ExportFeature>> exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        ArrayList<ExportFeature> sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides);
        sectionContent.add(PsProteinFeature.psms);
        sectionContent.add(PsProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(PsProteinFeature.mw);
        sectionContent.add(PsProteinFeature.spectrum_counting_nsaf);

        // variable_ptms
        sectionContent.add(PsProteinFeature.confident_modification_sites);
        sectionContent.add(PsProteinFeature.confident_modification_sites_number);
        sectionContent.add(PsProteinFeature.ambiguous_modification_sites);
        sectionContent.add(PsProteinFeature.ambiguous_modification_sites_number);

        // protein scores
        sectionContent.add(PsProteinFeature.confidence);
        sectionContent.add(PsProteinFeature.decoy);
        sectionContent.add(PsProteinFeature.validated);

        // Peptide sub-section
        // accessions
        sectionContent.add(PsPeptideFeature.accessions);

        // peptide sequence
        sectionContent.add(PsPeptideFeature.aaBefore);
        sectionContent.add(PsPeptideFeature.sequence);
        sectionContent.add(PsPeptideFeature.aaAfter);

        // variable_ptms
        sectionContent.add(PsPeptideFeature.fixed_ptms);
        sectionContent.add(PsPeptideFeature.variable_ptms);
        sectionContent.add(PsPeptideFeature.localization_confidence);

        // psms
        sectionContent.add(PsPeptideFeature.validated_psms);
        sectionContent.add(PsPeptideFeature.psms);

        // peptide scores
        sectionContent.add(PsPeptideFeature.confidence);
        sectionContent.add(PsPeptideFeature.decoy);
        sectionContent.add(PsPeptideFeature.validated);

        // PSM sub-section
        // protein accessions
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.accessions);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.sequence);

        // ptms
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.modified_sequence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsPsmFeature.d_score);
        sectionContent.add(PsPsmFeature.probabilistic_score);
        sectionContent.add(PsPsmFeature.localization_confidence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.fixed_ptms);

        // spectrum file
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz_error_ppm);

        // psm scores
        sectionContent.add(PsPsmFeature.confidence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.decoy);
        sectionContent.add(PsPsmFeature.validated);

        exportFeatures.put(PsProteinFeature.type, sectionContent);

        ExportScheme topDownReport = new ExportScheme("Default Hierarchical Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default protein report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.gene_name);
        sectionContent.add(PsProteinFeature.chromosome);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides);
        sectionContent.add(PsProteinFeature.unique_validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides_group);
        sectionContent.add(PsProteinFeature.unique_validated_peptides_group);
        sectionContent.add(PsProteinFeature.psms);
        sectionContent.add(PsProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(PsProteinFeature.mw);
        sectionContent.add(PsProteinFeature.spectrum_counting);

        // variable_ptms
        sectionContent.add(PsProteinFeature.confident_modification_sites);
        sectionContent.add(PsProteinFeature.confident_modification_sites_number);
        sectionContent.add(PsProteinFeature.ambiguous_modification_sites);
        sectionContent.add(PsProteinFeature.ambiguous_modification_sites_number);

        // protein scores
        sectionContent.add(PsProteinFeature.confidence);
        sectionContent.add(PsProteinFeature.validated);

        exportFeatures.put(PsProteinFeature.type, sectionContent);

        ExportScheme proteinReport = new ExportScheme("Default Protein Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default peptide report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // accessions
        sectionContent.add(PsPeptideFeature.accessions);
        sectionContent.add(PsPeptideFeature.unique_database);

        // protein group
        sectionContent.add(PsPeptideFeature.protein_groups);
        sectionContent.add(PsPeptideFeature.nValidatedProteinGroups);

        // position
        sectionContent.add(PsPeptideFeature.position);
        
        // peptide sequence
        sectionContent.add(PsPeptideFeature.aaBefore);
        sectionContent.add(PsPeptideFeature.sequence);
        sectionContent.add(PsPeptideFeature.aaAfter);

        // ptms
        sectionContent.add(PsPeptideFeature.modified_sequence);
        sectionContent.add(PsPeptideFeature.fixed_ptms);
        sectionContent.add(PsPeptideFeature.variable_ptms);
        sectionContent.add(PsPeptideFeature.localization_confidence);

        // psms
        sectionContent.add(PsPeptideFeature.validated_psms);
        sectionContent.add(PsPeptideFeature.psms);

        // peptide scores
        sectionContent.add(PsPeptideFeature.confidence);
        sectionContent.add(PsPeptideFeature.validated);

        exportFeatures.put(PsPeptideFeature.type, sectionContent);

        ExportScheme peptideReport = new ExportScheme("Default Peptide Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default PSM report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.accessions);

        // position
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.position);
        
        // peptide sequence
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.aaBefore);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.sequence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.aaAfter);

        // ptms
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.modified_sequence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.fixed_ptms);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsPsmFeature.d_score);
        sectionContent.add(PsPsmFeature.probabilistic_score);
        sectionContent.add(PsPsmFeature.localization_confidence);

        // spectrum file
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz_error_ppm);

        // psm scores
        sectionContent.add(PsPsmFeature.confidence);
        sectionContent.add(PsPsmFeature.validated);

        exportFeatures.put(PsPsmFeature.type, sectionContent);

        ExportScheme psmReport = new ExportScheme("Default PSM Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // All PSM report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.accessions);

        // position
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.position);
        
        // peptide sequence
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.aaBefore);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.sequence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.aaAfter);

        // ptms
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.modified_sequence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.fixed_ptms);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsPsmFeature.d_score);
        sectionContent.add(PsPsmFeature.probabilistic_score);
        sectionContent.add(PsPsmFeature.localization_confidence);

        // spectrum file
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz_error_ppm);

        // psm scores
        sectionContent.add(PsPsmFeature.confidence);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.decoy);
        sectionContent.add(PsPsmFeature.validated);

        exportFeatures.put(PsPsmFeature.type, sectionContent);

        ExportScheme extendedPsmReport = new ExportScheme("Extended PSM Report", false, exportFeatures, "\t", true, true, 0, false, false, true);

        ///////////////////////////
        // Default protein phospho report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.gene_name);
        sectionContent.add(PsProteinFeature.chromosome);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides);
        sectionContent.add(PsProteinFeature.unique_validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides_group);
        sectionContent.add(PsProteinFeature.unique_validated_peptides_group);
        sectionContent.add(PsProteinFeature.psms);
        sectionContent.add(PsProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(PsProteinFeature.mw);
        sectionContent.add(PsProteinFeature.spectrum_counting_nsaf);

        // phosphosites
        sectionContent.add(PsProteinFeature.confident_phosphosites);
        sectionContent.add(PsProteinFeature.confident_phosphosites_number);
        sectionContent.add(PsProteinFeature.ambiguous_phosphosites);
        sectionContent.add(PsProteinFeature.ambiguous_phosphosites_number);

        // protein scores
        sectionContent.add(PsProteinFeature.confidence);
        sectionContent.add(PsProteinFeature.validated);

        exportFeatures.put(PsProteinFeature.type, sectionContent);

        ExportScheme proteinPhosphoReport = new ExportScheme("Default Protein Phosphorylation Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default peptide phosphorylation report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // accessions
        sectionContent.add(PsPeptideFeature.accessions);
        sectionContent.add(PsPeptideFeature.unique_database);

        // protein group
        sectionContent.add(PsPeptideFeature.protein_groups);
        sectionContent.add(PsPeptideFeature.nValidatedProteinGroups);

        // peptide sequence
        sectionContent.add(PsPeptideFeature.aaBefore);
        sectionContent.add(PsPeptideFeature.sequence);
        sectionContent.add(PsPeptideFeature.aaAfter);

        // ptms
        sectionContent.add(PsPeptideFeature.fixed_ptms);
        sectionContent.add(PsPeptideFeature.modified_sequence);
        sectionContent.add(PsPeptideFeature.variable_ptms);
        sectionContent.add(PsPeptideFeature.localization_confidence);

        // phosphorylation
        sectionContent.add(PsPeptideFeature.confident_phosphosites);
        sectionContent.add(PsPeptideFeature.confident_phosphosites_number);
        sectionContent.add(PsPeptideFeature.ambiguous_phosphosites);
        sectionContent.add(PsPeptideFeature.ambiguous_phosphosites_number);

        // psms
        sectionContent.add(PsPeptideFeature.validated_psms);
        sectionContent.add(PsPeptideFeature.psms);

        // peptide scores
        sectionContent.add(PsPeptideFeature.confidence);
        sectionContent.add(PsPeptideFeature.validated);

        exportFeatures.put(PsPeptideFeature.type, sectionContent);

        ExportScheme peptidePhosphoReport = new ExportScheme("Default Peptide Phosphorylation Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default PSM phosphorylation report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.accessions);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.sequence);

        // ptms
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.fixed_ptms);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsPsmFeature.d_score);
        sectionContent.add(PsPsmFeature.probabilistic_score);
        sectionContent.add(PsPsmFeature.localization_confidence);
        sectionContent.add(PsPsmFeature.confident_phosphosites);
        sectionContent.add(PsPsmFeature.confident_phosphosites_number);
        sectionContent.add(PsPsmFeature.ambiguous_phosphosites);
        sectionContent.add(PsPsmFeature.ambiguous_phosphosites_number);

        // spectrum file
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz_error_ppm);

        // psm scores
        sectionContent.add(PsPsmFeature.confidence);
        sectionContent.add(PsPsmFeature.validated);

        exportFeatures.put(PsPsmFeature.type, sectionContent);

        ExportScheme psmPhosphoReport = new ExportScheme("Default PSM Phosphorylation Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Certificate of analysis
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        ArrayList<String> sectionsList = new ArrayList<String>();

        // project details
        sectionContent = new ArrayList<ExportFeature>();
        sectionsList.add(PsProjectFeature.type);
        sectionContent.add(PsProjectFeature.peptide_shaker);
        sectionContent.add(PsProjectFeature.date);
        sectionContent.add(PsProjectFeature.experiment);
        sectionContent.add(PsProjectFeature.sample);
        sectionContent.add(PsProjectFeature.replicate);
        sectionContent.add(PsProjectFeature.identification_algorithms);
        exportFeatures.put(PsProjectFeature.type, sectionContent);

        // search parameters
        sectionsList.add(PsSearchFeature.type);
        exportFeatures.put(PsSearchFeature.type, PsSearchFeature.values()[0].getExportFeatures(false));

        // input filters
        sectionsList.add(PsInputFilterFeature.type);
        exportFeatures.put(PsInputFilterFeature.type, PsInputFilterFeature.values()[0].getExportFeatures(false));

        // validation details
        sectionsList.add(PsValidationFeature.type);
        exportFeatures.put(PsValidationFeature.type, PsValidationFeature.values()[0].getExportFeatures(false));

        // ptms
        sectionsList.add(PsPtmScoringFeature.type);
        exportFeatures.put(PsPtmScoringFeature.type, PsPtmScoringFeature.values()[0].getExportFeatures(false));

        // spectrum counting details
        sectionsList.add(PsSpectrumCountingFeature.type);
        exportFeatures.put(PsSpectrumCountingFeature.type, PsSpectrumCountingFeature.values()[0].getExportFeatures(false));

        // annotation settings
        sectionsList.add(PsAnnotationFeature.type);
        exportFeatures.put(PsAnnotationFeature.type, PsAnnotationFeature.values()[0].getExportFeatures(false));

        ExportScheme coa = new ExportScheme("Certificate of Analysis", false, sectionsList, exportFeatures, ": ", true, false, 2, true, false, true);

        HashMap<String, ExportScheme> defaultSchemes = new HashMap<String, ExportScheme>();
        defaultSchemes.put(topDownReport.getName(), topDownReport);
        defaultSchemes.put(proteinReport.getName(), proteinReport);
        defaultSchemes.put(peptideReport.getName(), peptideReport);
        defaultSchemes.put(psmReport.getName(), psmReport);
        defaultSchemes.put(extendedPsmReport.getName(), extendedPsmReport);
        defaultSchemes.put(proteinPhosphoReport.getName(), proteinPhosphoReport);
        defaultSchemes.put(peptidePhosphoReport.getName(), peptidePhosphoReport);
        defaultSchemes.put(psmPhosphoReport.getName(), psmPhosphoReport);
        defaultSchemes.put(coa.getName(), coa);
        return defaultSchemes;
    }

    /**
     * Returns the file where to save the implemented export schemes.
     *
     * @return the file where to save the implemented export schemes
     */
    public static String getSerializationFile() {
        return SERIALIZATION_FILE;
    }

    /**
     * Returns the folder where to save the implemented export schemes.
     *
     * @return the folder where to save the implemented export schemes
     */
    public static String getSerializationFolder() {
        File tempFile = new File(getSerializationFile());
        return tempFile.getParent();
    }

    /**
     * Sets the file where to save the implemented export schemes.
     *
     * @param serializationFolder the folder where to save the implemented
     * export schemes
     */
    public static void setSerializationFolder(String serializationFolder) {
        PSExportFactory.SERIALIZATION_FILE = serializationFolder + "/exportFactory.cus";
    }
}
