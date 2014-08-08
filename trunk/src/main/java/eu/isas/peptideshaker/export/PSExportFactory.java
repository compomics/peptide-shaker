package eu.isas.peptideshaker.export;

import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.export.exportfeatures.AnnotationFeature;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.IdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.InputFilterFeature;
import eu.isas.peptideshaker.export.exportfeatures.PeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.ProjectFeature;
import eu.isas.peptideshaker.export.exportfeatures.ProteinFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsmFeature;
import eu.isas.peptideshaker.export.exportfeatures.PtmScoringFeature;
import eu.isas.peptideshaker.export.exportfeatures.SearchFeature;
import eu.isas.peptideshaker.export.exportfeatures.SpectrumCountingFeature;
import eu.isas.peptideshaker.export.exportfeatures.ValidationFeature;
import eu.isas.peptideshaker.export.sections.AnnotationSection;
import eu.isas.peptideshaker.export.sections.IdentificationAlgorithmMatchesSection;
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
        result.add(ProteinFeature.type);
        result.add(PeptideFeature.type);
        result.add(PsmFeature.type);
        result.add(IdentificationAlgorithmMatchesFeature.type);
        result.add(FragmentFeature.type);
        result.add(AnnotationFeature.type);
        result.add(InputFilterFeature.type);
        result.add(ProjectFeature.type);
        result.add(PtmScoringFeature.type);
        result.add(SearchFeature.type);
        result.add(SpectrumCountingFeature.type);
        result.add(ValidationFeature.type);
        return result;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(String sectionName, boolean includeSubFeatures) {
        if (sectionName.equals(AnnotationFeature.type)) {
            return AnnotationFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(InputFilterFeature.type)) {
            return InputFilterFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PeptideFeature.type)) {
            return PeptideFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ProjectFeature.type)) {
            return ProjectFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ProteinFeature.type)) {
            return ProteinFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsmFeature.type)) {
            return PsmFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PtmScoringFeature.type)) {
            return PtmScoringFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(SearchFeature.type)) {
            return SearchFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(SpectrumCountingFeature.type)) {
            return SpectrumCountingFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ValidationFeature.type)) {
            return ValidationFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(IdentificationAlgorithmMatchesFeature.type)) {
            return IdentificationAlgorithmMatchesFeature.values()[0].getExportFeatures(includeSubFeatures);
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
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param idFilter the identification filer (mandatory for the Input Filter
     * section)
     * @param ptmcoringPreferences the PTM scoring preferences (mandatory for
     * the PTM scoring section)
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     * @throws org.apache.commons.math.MathException
     */
    public static void writeExport(ExportScheme exportScheme, File destinationFile, String experiment, String sample, int replicateNumber,
            ProjectDetails projectDetails, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, ArrayList<String> proteinKeys, ArrayList<String> peptideKeys, ArrayList<String> psmKeys,
            String proteinMatchKey, int nSurroundingAA, AnnotationPreferences annotationPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, IdFilter idFilter,
            PTMScoringPreferences ptmcoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

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
            if (sectionName.equals(AnnotationFeature.type)) {
                AnnotationSection section = new AnnotationSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(annotationPreferences, waitingHandler);
            } else if (sectionName.equals(InputFilterFeature.type)) {
                InputFilterSection section = new InputFilterSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(idFilter, waitingHandler);
            } else if (sectionName.equals(PeptideFeature.type)) {
                PeptideSection section = new PeptideSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, peptideKeys, nSurroundingAA, "", exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(ProjectFeature.type)) {
                ProjectSection section = new ProjectSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(experiment, sample, replicateNumber, projectDetails, waitingHandler);
            } else if (sectionName.equals(ProteinFeature.type)) {
                ProteinSection section = new ProteinSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, psmKeys, nSurroundingAA, exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsmFeature.type)) {
                PsmSection section = new PsmSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, psmKeys, "", exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(IdentificationAlgorithmMatchesFeature.type)) {
                IdentificationAlgorithmMatchesSection section = new IdentificationAlgorithmMatchesSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, psmKeys, mainTitle, waitingHandler);
            } else if (sectionName.equals(PtmScoringFeature.type)) {
                PtmScoringSection section = new PtmScoringSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(ptmcoringPreferences, waitingHandler);
            } else if (sectionName.equals(SearchFeature.type)) {
                SearchParametersSection section = new SearchParametersSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(searchParameters, waitingHandler);
            } else if (sectionName.equals(SpectrumCountingFeature.type)) {
                SpectrumCountingSection section = new SpectrumCountingSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(spectrumCountingPreferences, waitingHandler);
            } else if (sectionName.equals(ValidationFeature.type)) {
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
                boolean firstTitle = true;
                for (String title : exportFeature.getTitles()) {
                    if (firstTitle) {
                        firstTitle = false;
                    } else {
                        writer.write(", ");
                    }
                    writer.write(title);
                }
                writer.write(exportScheme.getSeparator());
                writer.write(exportFeature.getDescription());
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
        // Default hierarchical report
        ///////////////////////////
        HashMap<String, ArrayList<ExportFeature>> exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        ArrayList<ExportFeature> sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(ProteinFeature.accession);
        sectionContent.add(ProteinFeature.protein_description);
        sectionContent.add(ProteinFeature.pi);
        sectionContent.add(ProteinFeature.other_proteins);
        sectionContent.add(ProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(ProteinFeature.peptides);
        sectionContent.add(ProteinFeature.validated_peptides);
        sectionContent.add(ProteinFeature.unique_peptides);
        sectionContent.add(ProteinFeature.psms);
        sectionContent.add(ProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(ProteinFeature.coverage);
        sectionContent.add(ProteinFeature.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(ProteinFeature.mw);
        sectionContent.add(ProteinFeature.spectrum_counting_nsaf);
        sectionContent.add(ProteinFeature.spectrum_counting_empai);

        // variable_ptms
        sectionContent.add(ProteinFeature.confident_PTMs);
        sectionContent.add(ProteinFeature.other_PTMs);

        // protein scores
        sectionContent.add(ProteinFeature.confidence);
        sectionContent.add(ProteinFeature.decoy);
        sectionContent.add(ProteinFeature.validated);

        // Peptide sub-section
        // accessions
        sectionContent.add(PeptideFeature.accessions);

        // peptide sequence
        sectionContent.add(PeptideFeature.aaBefore);
        sectionContent.add(PeptideFeature.sequence);
        sectionContent.add(PeptideFeature.aaAfter);

        // variable_ptms
        sectionContent.add(PeptideFeature.variable_ptms);
        sectionContent.add(PeptideFeature.localization_confidence);
        sectionContent.add(PeptideFeature.fixed_ptms);

        // psms
        sectionContent.add(PeptideFeature.validated_psms);
        sectionContent.add(PeptideFeature.psms);

        // peptide scores
        sectionContent.add(PeptideFeature.confidence);
        sectionContent.add(PeptideFeature.decoy);
        sectionContent.add(PeptideFeature.validated);

        // PSM sub-section
        // protein accessions
        sectionContent.add(IdentificationAlgorithmMatchesFeature.accessions);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.sequence);

        // ptms
        sectionContent.add(IdentificationAlgorithmMatchesFeature.modified_sequence);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsmFeature.d_score);
        sectionContent.add(PsmFeature.probabilistic_score);
        sectionContent.add(PsmFeature.localization_confidence);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.fixed_ptms);

        // spectrum file
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(IdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.mz_error);

        // psm scores
        sectionContent.add(PsmFeature.confidence);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.decoy);
        sectionContent.add(PsmFeature.validated);

        exportFeatures.put(ProteinFeature.type, sectionContent);

        ExportScheme topDownReport = new ExportScheme("Default Hierarchical Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default protein report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions and protein inferences 
        sectionContent.add(ProteinFeature.accession);
        sectionContent.add(ProteinFeature.protein_description);
        sectionContent.add(ProteinFeature.gene_name);
        sectionContent.add(ProteinFeature.chromosome);
        sectionContent.add(ProteinFeature.pi);
        sectionContent.add(ProteinFeature.other_proteins);
        sectionContent.add(ProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(ProteinFeature.peptides);
        sectionContent.add(ProteinFeature.validated_peptides);
        sectionContent.add(ProteinFeature.unique_peptides);
        sectionContent.add(ProteinFeature.psms);
        sectionContent.add(ProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(ProteinFeature.coverage);
        sectionContent.add(ProteinFeature.possible_coverage);

        // molecular weight and spectrum counting
        sectionContent.add(ProteinFeature.mw);
        sectionContent.add(ProteinFeature.spectrum_counting_nsaf);
        sectionContent.add(ProteinFeature.spectrum_counting_empai);

        // variable_ptms
        sectionContent.add(ProteinFeature.confident_PTMs);
        sectionContent.add(ProteinFeature.other_PTMs);

        // protein scores
        sectionContent.add(ProteinFeature.confidence);
        sectionContent.add(ProteinFeature.decoy);
        sectionContent.add(ProteinFeature.validated);

        exportFeatures.put(ProteinFeature.type, sectionContent);

        ExportScheme proteinReport = new ExportScheme("Default Protein Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default peptide report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // accessions
        sectionContent.add(PeptideFeature.accessions);

        // peptide sequence
        sectionContent.add(PeptideFeature.aaBefore);
        sectionContent.add(PeptideFeature.sequence);
        sectionContent.add(PeptideFeature.aaAfter);

        // variable_ptms
        sectionContent.add(PeptideFeature.modified_sequence);
        sectionContent.add(PeptideFeature.variable_ptms);
        sectionContent.add(PeptideFeature.localization_confidence);
        sectionContent.add(PeptideFeature.fixed_ptms);

        // psms
        sectionContent.add(PeptideFeature.validated_psms);
        sectionContent.add(PeptideFeature.psms);

        // peptide scores
        sectionContent.add(PeptideFeature.confidence);
        sectionContent.add(PeptideFeature.decoy);
        sectionContent.add(PeptideFeature.validated);

        exportFeatures.put(PeptideFeature.type, sectionContent);

        ExportScheme peptideReport = new ExportScheme("Default Peptide Report", false, exportFeatures, "\t", true, true, 0, false, true, false);

        ///////////////////////////
        // Default PSM report
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        sectionContent = new ArrayList<ExportFeature>();

        // protein accessions
        sectionContent.add(IdentificationAlgorithmMatchesFeature.accessions);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.sequence);

        // ptms
        sectionContent.add(IdentificationAlgorithmMatchesFeature.variable_ptms);
        sectionContent.add(PsmFeature.d_score);
        sectionContent.add(PsmFeature.probabilistic_score);
        sectionContent.add(PsmFeature.localization_confidence);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.fixed_ptms);

        // spectrum file
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_file);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_title);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // spectrum details
        sectionContent.add(IdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.mz_error);

        // psm scores
        sectionContent.add(PsmFeature.confidence);
        sectionContent.add(IdentificationAlgorithmMatchesFeature.decoy);
        sectionContent.add(PsmFeature.validated);

        exportFeatures.put(PsmFeature.type, sectionContent);

        ExportScheme psmReport = new ExportScheme("Default PSM Report", false, exportFeatures, "\t", true, true, 1, false, true, false);

        ///////////////////////////
        // Certificate of analysis
        ///////////////////////////
        exportFeatures = new HashMap<String, ArrayList<ExportFeature>>();
        ArrayList<String> sectionsList = new ArrayList<String>();

        // project details
        sectionContent = new ArrayList<ExportFeature>();
        sectionsList.add(ProjectFeature.type);
        sectionContent.add(ProjectFeature.peptide_shaker);
        sectionContent.add(ProjectFeature.date);
        sectionContent.add(ProjectFeature.experiment);
        sectionContent.add(ProjectFeature.sample);
        sectionContent.add(ProjectFeature.replicate);
        sectionContent.add(ProjectFeature.identification_algorithms);
        exportFeatures.put(ProjectFeature.type, sectionContent);

        // search parameters
        sectionsList.add(SearchFeature.type);
        exportFeatures.put(SearchFeature.type, SearchFeature.values()[0].getExportFeatures(false));

        // input filters
        sectionsList.add(InputFilterFeature.type);
        exportFeatures.put(InputFilterFeature.type, InputFilterFeature.values()[0].getExportFeatures(false));

        // validation details
        sectionsList.add(ValidationFeature.type);
        exportFeatures.put(ValidationFeature.type, ValidationFeature.values()[0].getExportFeatures(false));

        // ptms
        sectionsList.add(PtmScoringFeature.type);
        exportFeatures.put(PtmScoringFeature.type, PtmScoringFeature.values()[0].getExportFeatures(false));

        // spectrum counting details
        sectionsList.add(SpectrumCountingFeature.type);
        exportFeatures.put(SpectrumCountingFeature.type, SpectrumCountingFeature.values()[0].getExportFeatures(false));

        // annotation settings
        sectionsList.add(AnnotationFeature.type);
        exportFeatures.put(AnnotationFeature.type, AnnotationFeature.values()[0].getExportFeatures(false));

        ExportScheme coa = new ExportScheme("Certificate of Analysis", false, sectionsList, exportFeatures, ": ", true, false, 2, true, false, true);

        HashMap<String, ExportScheme> defaultSchemes = new HashMap<String, ExportScheme>();
        defaultSchemes.put(topDownReport.getName(), topDownReport);
        defaultSchemes.put(proteinReport.getName(), proteinReport);
        defaultSchemes.put(peptideReport.getName(), peptideReport);
        defaultSchemes.put(psmReport.getName(), psmReport);
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
     * @param serializationFolder the folder where to save the implemented export schemes
     */
    public static void setSerializationFolder(String serializationFolder) {
        PSExportFactory.SERIALIZATION_FILE = serializationFolder + "/exportFactory.cus";
    }
    
}
