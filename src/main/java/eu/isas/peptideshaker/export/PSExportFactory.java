package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.io.json.JsonMarshaller;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.io.export.features.peptideshaker.PsAnnotationFeature;
import com.compomics.util.io.export.features.peptideshaker.PsIdentificationAlgorithmMatchesFeature;
import com.compomics.util.io.export.features.peptideshaker.PsInputFilterFeature;
import com.compomics.util.io.export.features.peptideshaker.PsPeptideFeature;
import com.compomics.util.io.export.features.peptideshaker.PsProjectFeature;
import com.compomics.util.io.export.features.peptideshaker.PsProteinFeature;
import com.compomics.util.io.export.features.peptideshaker.PsPsmFeature;
import com.compomics.util.io.export.features.peptideshaker.PsPtmScoringFeature;
import com.compomics.util.io.export.features.peptideshaker.PsSearchFeature;
import com.compomics.util.io.export.features.peptideshaker.PsSpectrumCountingFeature;
import com.compomics.util.io.export.features.peptideshaker.PsValidationFeature;
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
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import eu.isas.peptideshaker.utils.ExportFactoryMarshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The PeptideShaker export factory.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
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
     * User defined JSON file containing the user schemes.
     */
    private static String JSON_FILE = System.getProperty("user.home") + "/.peptideshaker/exportFactory.json"; // @TODO: should not default to user.home
    /**
     * The user export schemes.
     */
    private final HashMap<String, ExportScheme> userSchemes = new HashMap<>();
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

                File savedFile = new File(JSON_FILE);
                instance = loadFromFile(savedFile);

            } catch (Exception e) {

                e.printStackTrace();
                instance = new PSExportFactory();

                try {

                    saveFactory(instance);

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
     * @param psExportFactory the instance of the factory to save
     * 
     * @throws IOException exception thrown whenever an error occurred while
     * saving the PSExportFactory
     */
    public static void saveFactory(PSExportFactory psExportFactory) throws IOException {

        File factoryFile = new File(JSON_FILE);

        if (!factoryFile.getParentFile().exists()) {
            factoryFile.getParentFile().mkdir();
        }

        JsonMarshaller jsonMarshaller = new ExportFactoryMarshaller();
        jsonMarshaller.saveObjectToJson(psExportFactory, factoryFile);

    }

    /**
     * Loads an export factory from a file. The file must be an export of the
     * factory in the json format.
     *
     * @param file the file to load
     *
     * @return the export factory saved in file
     *
     * @throws IOException exception thrown whenever an error occurred while
     * loading the file
     */
    public static PSExportFactory loadFromFile(File file) throws IOException {

        JsonMarshaller jsonMarshaller = new ExportFactoryMarshaller();
        PSExportFactory result = (PSExportFactory) jsonMarshaller.fromJson(PSExportFactory.class, file);

        return result;

    }

    /**
     * Returns a list of the name of the available user schemes.
     *
     * @return a list of the implemented user schemes
     */
    public ArrayList<String> getUserSchemesNames() {

        return new ArrayList<>(userSchemes.keySet());

    }

    /**
     * Returns the desired default export scheme. Null if not found.
     *
     * @param schemeName the name of the default export scheme
     *
     * @return the default export scheme
     */
    public static ExportScheme getDefaultExportScheme(
            String schemeName
    ) {

        return getDefaultExportSchemes().get(schemeName);

    }

    @Override
    public ExportScheme getExportScheme(
            String schemeName
    ) {

        ExportScheme exportScheme = userSchemes.get(schemeName);

        if (exportScheme == null) {

            exportScheme = getDefaultExportSchemes().get(schemeName);

        }

        return exportScheme;

    }

    @Override
    public void removeExportScheme(
            String schemeName
    ) {

        userSchemes.remove(schemeName);

    }

    @Override
    public void addExportScheme(
            ExportScheme exportScheme
    ) {

        userSchemes.put(exportScheme.getName(), exportScheme);

    }

    @Override
    public ArrayList<String> getImplementedSections() {

        ArrayList<String> result = new ArrayList<>();
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
    public ArrayList<ExportFeature> getExportFeatures(
            String sectionName,
            boolean includeSubFeatures
    ) {

        switch (sectionName) {

            case PsAnnotationFeature.type:
                return PsAnnotationFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsInputFilterFeature.type:
                return PsInputFilterFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsPeptideFeature.type:
                return PsPeptideFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsProjectFeature.type:
                return PsProjectFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsProteinFeature.type:
                return PsProteinFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsPsmFeature.type:
                return PsPsmFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsPtmScoringFeature.type:
                return PsPtmScoringFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsSearchFeature.type:
                return PsSearchFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsSpectrumCountingFeature.type:
                return PsSpectrumCountingFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsValidationFeature.type:
                return PsValidationFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsIdentificationAlgorithmMatchesFeature.type:
                return PsIdentificationAlgorithmMatchesFeature.values()[0].getExportFeatures(includeSubFeatures);

            default:
                return new ArrayList<>(0);

        }
    }

    /**
     * Returns a list of the default export schemes.
     *
     * @return a list of the default export schemes
     */
    public static String[] getDefaultExportSchemesNames() {

        TreeMap<String, ExportScheme> exportSchemesMap = getDefaultExportSchemes();

        return exportSchemesMap.navigableKeySet().toArray(new String[exportSchemesMap.size()]);

    }

    /**
     * Writes the desired export in text format. If an argument is not needed,
     * provide null (at your own risks).
     *
     * @param exportScheme the scheme of the export
     * @param destinationFile the destination file
     * @param exportFormat the format of export to use
     * @param gzip if true export text as gzipped file
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
     * @param nSurroundingAA the number of surrounding amino acids to export
     * (mandatory for the Peptide section)
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a provider for the protein sequences
     * @param proteinDetailsProvider the protein details provider
     * @param spectrumProvider the spectrum provider
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static void writeExport(
            ExportScheme exportScheme,
            File destinationFile,
            ExportFormat exportFormat,
            boolean gzip,
            String experiment,
            ProjectDetails projectDetails,
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            GeneMaps geneMaps,
            long[] proteinKeys,
            long[] peptideKeys,
            long[] psmKeys,
            int nSurroundingAA,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            SpectrumCountingParameters spectrumCountingPreferences,
            WaitingHandler waitingHandler
    )
            throws IOException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(
                exportFormat,
                destinationFile,
                exportScheme.getSeparator(),
                exportScheme.getSeparationLines(),
                gzip
        );

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

            switch (sectionName) {

                case PsAnnotationFeature.type:

                    PsAnnotationSection psAnnotationSection = new PsAnnotationSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psAnnotationSection.writeSection(
                            identificationParameters.getAnnotationParameters(),
                            waitingHandler
                    );
                    break;

                case PsInputFilterFeature.type:

                    PsInputFilterSection psInputFilterSection = new PsInputFilterSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psInputFilterSection.writeSection(
                            identificationParameters.getPeptideAssumptionFilter(),
                            waitingHandler
                    );
                    break;

                case PsPeptideFeature.type:

                    PsPeptideSection psPeptideSection = new PsPeptideSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psPeptideSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            proteinDetailsProvider,
                            spectrumProvider,
                            identificationParameters,
                            peptideKeys,
                            nSurroundingAA,
                            "",
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );
                    break;

                case PsProjectFeature.type:

                    PsProjectSection psProjectSection = new PsProjectSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psProjectSection.writeSection(
                            experiment,
                            projectDetails,
                            waitingHandler
                    );
                    break;

                case PsProteinFeature.type:

                    PsProteinSection psProteinSection = new PsProteinSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psProteinSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            proteinDetailsProvider,
                            spectrumProvider,
                            geneMaps,
                            identificationParameters,
                            proteinKeys,
                            nSurroundingAA,
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );
                    break;

                case PsPsmFeature.type:

                    PsPsmSection psPsmSection = new PsPsmSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psPsmSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            proteinDetailsProvider,
                            spectrumProvider,
                            identificationParameters,
                            psmKeys,
                            "",
                            nSurroundingAA,
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );
                    break;

                case PsIdentificationAlgorithmMatchesFeature.type:

                    PsIdentificationAlgorithmMatchesSection psIdentificationAlgorithmMatchesSection = new PsIdentificationAlgorithmMatchesSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psIdentificationAlgorithmMatchesSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            proteinDetailsProvider,
                            spectrumProvider,
                            identificationParameters,
                            psmKeys,
                            "",
                            nSurroundingAA,
                            waitingHandler
                    );
                    break;

                case PsPtmScoringFeature.type:

                    PsPtmScoringSection psPtmScoringSection = new PsPtmScoringSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psPtmScoringSection.writeSection(
                            identificationParameters.getModificationLocalizationParameters(),
                            waitingHandler
                    );
                    break;

                case PsSearchFeature.type:

                    PsSearchParametersSection psSearchParametersSection = new PsSearchParametersSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psSearchParametersSection.writeSection(
                            identificationParameters.getSearchParameters(),
                            projectDetails,
                            waitingHandler
                    );
                    break;

                case PsSpectrumCountingFeature.type:

                    PsSpectrumCountingSection psSpectrumCountingSection = new PsSpectrumCountingSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    psSpectrumCountingSection.writeSection(
                            spectrumCountingPreferences,
                            waitingHandler
                    );
                    break;

                case PsValidationFeature.type:

                    PsValidationSection psValidationSection = new PsValidationSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );
                    PSMaps psMaps = new PSMaps();
                    psMaps = (PSMaps) identification.getUrParam(psMaps);
                    psValidationSection.writeSection(
                            psMaps,
                            identificationParameters,
                            waitingHandler
                    );
                    break;

                default:
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
    public static void writeDocumentation(
            ExportScheme exportScheme,
            ExportFormat exportFormat,
            File destinationFile
    ) throws IOException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(
                exportFormat,
                destinationFile,
                exportScheme.getSeparator(),
                exportScheme.getSeparationLines(),
                false
        );

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

        StringBuilder options = new StringBuilder();

        for (int i = 0; i < implementedReports.size(); i++) {

            if (options.length() > 0) {

                options.append(", ");

            }

            options.append(i).append(": ").append(implementedReports.get(i));

        }

        return options.toString();

    }

    /**
     * Returns the default file name for the export of a report based on the
     * project details
     *
     * @param experiment the experiment of the project
     * @param exportName the name of the report type
     *
     * @return the default file name for the export
     */
    public static String getDefaultReportName(
            String experiment,
            String exportName
    ) {

        return experiment + "_" + exportName + ".txt";

    }

    /**
     * Returns the default file name for the export of the documentation of the
     * given report export type.
     *
     * @param exportName the export name
     *
     * @return the default file name for the export
     */
    public static String getDefaultDocumentation(
            String exportName
    ) {

        return exportName + "_documentation.txt";

    }

    /**
     * Returns the export type based on the number used in command line.
     *
     * @param commandLine the number used in command line option. See
     * getCommandLineOptions().
     *
     * @return the corresponding export name
     */
    public String getExportTypeFromCommandLineOption(
            int commandLine
    ) {

        if (implementedReports == null) {

            setUpReportList();

        }

        if (commandLine >= implementedReports.size()) {

            throw new IllegalArgumentException(
                    "Unrecognized report type: "
                    + commandLine
                    + ". Available reports are: "
                    + getCommandLineOptions()
                    + "."
            );

        }

        return implementedReports.get(commandLine);

    }

    /**
     * Initiates the sorted list of implemented reports.
     */
    private void setUpReportList() {

        implementedReports = Arrays.stream(getDefaultExportSchemesNames())
                .collect(Collectors.toCollection(ArrayList::new));

        implementedReports.addAll(
                userSchemes.keySet().stream()
                        .sorted()
                        .collect(Collectors.toList()));

    }

    /**
     * Returns the default schemes available.
     *
     * @return a list containing the default schemes
     */
    private static TreeMap<String, ExportScheme> getDefaultExportSchemes() {

        ///////////////////////////
        // Default hierarchical report
        ///////////////////////////
        HashMap<String, ArrayList<ExportFeature>> exportFeatures = new HashMap<>();
        ArrayList<ExportFeature> sectionContent = new ArrayList<>();

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
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.gene_name);
        sectionContent.add(PsProteinFeature.taxonomy);
        sectionContent.add(PsProteinFeature.chromosome);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides);
        sectionContent.add(PsProteinFeature.unique_validated_peptides);
        sectionContent.add(PsProteinFeature.psms);
        sectionContent.add(PsProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.all_coverage);
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

        ExportScheme proteinReport = new ExportScheme(
                "Default Protein Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Protein report (including non-validated matches)
        ///////////////////////////
        ExportScheme proteinWithNonValidatedReport = new ExportScheme(
                "Default Protein Report with non-validated matches",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                false,
                false
        );

        ///////////////////////////
        // Default peptide report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

        // accessions
        sectionContent.add(PsPeptideFeature.accessions);
        sectionContent.add(PsPeptideFeature.unique_group);

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

        ExportScheme peptideReport = new ExportScheme(
                "Default Peptide Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Peptide report (including non-validated matches)
        ///////////////////////////
        ExportScheme peptideWithNonValidatedReport = new ExportScheme(
                "Default Peptide Report with non-validated matches",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                false,
                false
        );

        ///////////////////////////
        // Default PSM report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

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

        // spectrum details
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.rt);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.identification_charge);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.theoretical_mass);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.isotope);
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.mz_error_ppm);

        // Cloning later reusable objects by "ALL PSM report"
        ArrayList<ExportFeature> sectionContentAllPSM = new ArrayList<>();

        for (ExportFeature section : sectionContent) {
            sectionContentAllPSM.add(section);
        }

        // spectrum file (not included in the extended PSM report)
        sectionContent.add(PsIdentificationAlgorithmMatchesFeature.spectrum_scan_number);

        // psm scores
        sectionContent.add(PsPsmFeature.confidence);
        sectionContent.add(PsPsmFeature.validated);

        exportFeatures.put(PsPsmFeature.type, sectionContent);

        ExportScheme psmReport = new ExportScheme(
                "Default PSM Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // PSM report (including non-validated matches)
        ///////////////////////////
        ExportScheme psmWithNonValidatedReport = new ExportScheme(
                "Default PSM Report with non-validated matches",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                false,
                false
        );

        ///////////////////////////
        // All PSM report
        ///////////////////////////
        exportFeatures = new HashMap<>();

        sectionContentAllPSM.add(PsIdentificationAlgorithmMatchesFeature.decoy);
        sectionContentAllPSM.add(PsPsmFeature.validated);
        exportFeatures.put(PsPsmFeature.type, sectionContentAllPSM);

        ExportScheme extendedPsmReport = new ExportScheme(
                "Extended PSM Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                false,
                true
        );

        ///////////////////////////
        // Default protein phospho report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.gene_name);
        sectionContent.add(PsProteinFeature.taxonomy);
        sectionContent.add(PsProteinFeature.chromosome);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);
        sectionContent.add(PsProteinFeature.unique_peptides);
        sectionContent.add(PsProteinFeature.unique_validated_peptides);
        sectionContent.add(PsProteinFeature.psms);
        sectionContent.add(PsProteinFeature.validated_psms);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.all_coverage);
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

        ExportScheme proteinPhosphoReport = new ExportScheme(
                "Default Protein Phosphorylation Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Default peptide phosphorylation report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

        // accessions
        sectionContent.add(PsPeptideFeature.accessions);
        sectionContent.add(PsPeptideFeature.unique_group);

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

        ExportScheme peptidePhosphoReport = new ExportScheme(
                "Default Peptide Phosphorylation Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Default PSM phosphorylation report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

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

        ExportScheme psmPhosphoReport = new ExportScheme(
                "Default PSM Phosphorylation Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Default fractions report
        ///////////////////////////
        exportFeatures = new HashMap<>();
        sectionContent = new ArrayList<>();

        // protein accessions and protein inferences 
        sectionContent.add(PsProteinFeature.accession);
        sectionContent.add(PsProteinFeature.protein_description);
        sectionContent.add(PsProteinFeature.gene_name);
        sectionContent.add(PsProteinFeature.taxonomy);
        sectionContent.add(PsProteinFeature.chromosome);
        sectionContent.add(PsProteinFeature.pi);
        sectionContent.add(PsProteinFeature.other_proteins);
        sectionContent.add(PsProteinFeature.protein_group);

        // peptide and spectrum counts
        sectionContent.add(PsProteinFeature.peptides);
        sectionContent.add(PsProteinFeature.validated_peptides);

        // protein coverage
        sectionContent.add(PsProteinFeature.coverage);
        sectionContent.add(PsProteinFeature.all_coverage);
        sectionContent.add(PsProteinFeature.possible_coverage);

        // protein scores
        sectionContent.add(PsProteinFeature.confidence);
        sectionContent.add(PsProteinFeature.validated);

        // fraction information
        sectionContent.add(PsProteinFeature.peptidesPerFraction);
        sectionContent.add(PsProteinFeature.spectraPerFraction);
        sectionContent.add(PsProteinFeature.averagePrecursorIntensty);
        sectionContent.add(PsProteinFeature.fractionMinMwPeptideRange);
        sectionContent.add(PsProteinFeature.fractionMaxMwPeptideRange);
        sectionContent.add(PsProteinFeature.fractionMinMwSpectraRange);
        sectionContent.add(PsProteinFeature.fractionMaxMwSpectraRange);

        exportFeatures.put(PsProteinFeature.type, sectionContent);

        ExportScheme fractionsReport = new ExportScheme(
                "Fractions Report",
                false,
                exportFeatures,
                "\t",
                true,
                true,
                0,
                false,
                true,
                false
        );

        ///////////////////////////
        // Certificate of analysis
        ///////////////////////////
        exportFeatures = new HashMap<>();
        ArrayList<String> sectionsList = new ArrayList<>();

        // project details
        sectionContent = new ArrayList<>();
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

        ExportScheme coa = new ExportScheme(
                "Certificate of Analysis",
                false,
                sectionsList,
                exportFeatures,
                ": ",
                true,
                false,
                2,
                true,
                false,
                true
        );

        TreeMap<String, ExportScheme> defaultSchemes = new TreeMap<>();
        defaultSchemes.put(topDownReport.getName(), topDownReport);
        defaultSchemes.put(proteinReport.getName(), proteinReport);
        defaultSchemes.put(peptideReport.getName(), peptideReport);
        defaultSchemes.put(psmReport.getName(), psmReport);
        defaultSchemes.put(extendedPsmReport.getName(), extendedPsmReport);
        defaultSchemes.put(proteinPhosphoReport.getName(), proteinPhosphoReport);
        defaultSchemes.put(peptidePhosphoReport.getName(), peptidePhosphoReport);
        defaultSchemes.put(psmPhosphoReport.getName(), psmPhosphoReport);
        defaultSchemes.put(coa.getName(), coa);
        defaultSchemes.put(proteinWithNonValidatedReport.getName(), proteinWithNonValidatedReport);
        defaultSchemes.put(peptideWithNonValidatedReport.getName(), peptideWithNonValidatedReport);
        defaultSchemes.put(psmWithNonValidatedReport.getName(), psmWithNonValidatedReport);
        defaultSchemes.put(fractionsReport.getName(), fractionsReport);

        return defaultSchemes;

    }

    /**
     * Returns the file where to save the implemented export schemes.
     *
     * @return the file where to save the implemented export schemes
     */
    public static String getJsonFile() {

        return JSON_FILE;

    }

    /**
     * Returns the folder where to save the implemented export schemes.
     *
     * @return the folder where to save the implemented export schemes
     */
    public static String getJsonFolder() {

        File tempFile = new File(getJsonFile());
        return tempFile.getParent();

    }

    /**
     * Sets the file where to save the implemented export schemes.
     *
     * @param serializationFolder the folder where to save the implemented
     * export schemes
     */
    public static void setJsonFolder(String serializationFolder) {

        PSExportFactory.JSON_FILE = serializationFolder + "/exportFactory.json";

    }
}
