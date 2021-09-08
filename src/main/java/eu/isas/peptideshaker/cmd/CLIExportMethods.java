package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.PSExportFactory;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.MzIdentMLExport;
import eu.isas.peptideshaker.followup.FastaExport;
import eu.isas.peptideshaker.followup.InclusionListExport;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import eu.isas.peptideshaker.followup.RecalibrationExporter;
import eu.isas.peptideshaker.followup.SpectrumExporter;
import com.compomics.util.gui.filtering.FilterParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import eu.isas.peptideshaker.utils.PsdbParent;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import eu.isas.peptideshaker.followup.DeepLcExport;
import eu.isas.peptideshaker.followup.Ms2PipExport;
import eu.isas.peptideshaker.followup.ProteoformExport;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * This class groups standard methods used by the different command line
 * interfaces.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class CLIExportMethods {

    /**
     * Recalibrates spectra as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param sequenceProvider the sequence provider
     * @param spectrumProvider The spectrum provider.
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress
     * @return ArrayList files containing the recalibrated spectra
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static ArrayList<File> recalibrateSpectra(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler
    ) throws IOException {

        File recalibrationFolder = followUpCLIInputBean.getRecalibrationFolder();
        if (!recalibrationFolder.exists()) {
            recalibrationFolder.mkdir();
        }
        boolean ms1 = true;
        boolean ms2 = true;
        if (followUpCLIInputBean.getRecalibrationMode() == 1) {
            ms2 = false;
        } else if (followUpCLIInputBean.getRecalibrationMode() == 2) {
            ms1 = false;
        }
        ArrayList<File> recalibratedSpectra = RecalibrationExporter.writeRecalibratedSpectra(
                ms1,
                ms2,
                recalibrationFolder,
                identification,
                sequenceProvider,
                spectrumProvider,
                identificationParameters,
                waitingHandler
        );
        return recalibratedSpectra;
    }

    /**
     * Exports the spectra as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler a waiting handler to display progress
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @return ArrayList files containing the spectra
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while interacting with the database
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     */
    public static ArrayList<File> exportSpectra(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler,
            SequenceMatchingParameters sequenceMatchingPreferences
    ) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        File exportFolder = followUpCLIInputBean.getSpectrumExportFolder();

        if (!exportFolder.exists()) {

            exportFolder.mkdir();

        }

        SpectrumExporter spectrumExporter = new SpectrumExporter(
                identification,
                spectrumProvider
        );
        ArrayList<File> exportedSpectra = spectrumExporter.exportSpectra(
                exportFolder,
                waitingHandler,
                SpectrumExporter.ExportType.getTypeFromIndex(
                        followUpCLIInputBean.getSpectrumExportTypeIndex()
                ),
                sequenceMatchingPreferences
        );

        return exportedSpectra;

    }

    /**
     * Exports the accessions as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param sequenceProvider the FASTA sequence provider
     * @param waitingHandler a waiting handler to display progress
     * @param filteringPreferences the filtering preferences
     * @return File file containing accessions
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static File exportAccessions(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler,
            FilterParameters filteringPreferences
    ) throws IOException {

        File destinationFileTemp = followUpCLIInputBean.getAccessionsExportFile();

        if (!destinationFileTemp.exists()) {

            destinationFileTemp.createNewFile();

        }

        File destinationFile = destinationFileTemp;
        FastaExport.export(
                destinationFile,
                sequenceProvider,
                identification,
                FastaExport.ExportType.getTypeFromIndex(
                        followUpCLIInputBean.getAccessionsExportTypeIndex()
                ),
                waitingHandler,
                true
        );
        return destinationFile;
    }

    /**
     * Exports the protein details in FASTA format as specified in the follow-up
     * input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param sequenceProvider the FASTA sequence provider
     * @param waitingHandler a waiting handler to display progress
     * @param filteringPreferences the filtering preferences
     * @return File file containing the protein details in FASTA format
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static File exportProteinSequences(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            SequenceProvider sequenceProvider,
            WaitingHandler waitingHandler,
            FilterParameters filteringPreferences
    ) throws IOException {

        File destinationFileTemp = followUpCLIInputBean.getProteinSequencesExportFile();

        if (!destinationFileTemp.exists()) {

            destinationFileTemp.createNewFile();

        }

        File destinationFile = destinationFileTemp;
        FastaExport.export(
                destinationFile,
                sequenceProvider,
                identification,
                FastaExport.ExportType.getTypeFromIndex(
                        followUpCLIInputBean.getProteinSequencesExportTypeIndex()
                ),
                waitingHandler,
                false
        );
        return destinationFile;
    }

    /**
     * Exports the identification in a Progenesis compatible format.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @return File file containing the identification data in a Progenesis
     * compatible format
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static File exportProgenesis(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            WaitingHandler waitingHandler,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SequenceMatchingParameters sequenceMatchingPreferences
    ) throws IOException {

        File destinationFileTemp = followUpCLIInputBean.getProgenesisExportFile();

        if (!destinationFileTemp.exists()) {

            destinationFileTemp.createNewFile();

        }

        File destinationFile = destinationFileTemp;
        ProgenesisExport.writeProgenesisExport(
                destinationFile,
                sequenceProvider,
                proteinDetailsProvider,
                identification,
                ProgenesisExport.ExportType.getTypeFromIndex(
                        followUpCLIInputBean.getProgenesisExportTypeIndex()
                ),
                waitingHandler,
                followUpCLIInputBean.getProgenesisTargetedPTMs(),
                sequenceMatchingPreferences
        );
        return destinationFile;
    }

    /**
     * Exports proteoforms.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     * 
     * @return File file containing the proteoforms data
     */
    public static File exportProteoforms(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            WaitingHandler waitingHandler
    ) {

        File destinationFile = followUpCLIInputBean.getProteoformsFile();

        ProteoformExport.writeProteoforms(
                destinationFile,
                identification,
                waitingHandler
        );
        return destinationFile;
    }

    /**
     * Exports files needed by DeepLC.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param modificationParameters The modification parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     * 
     * @return The files created by the export.
     */
    public static ArrayList<File> exportDeepLC(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            ModificationParameters modificationParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        String destinationStem = followUpCLIInputBean.getDeepLcStem();

        return DeepLcExport.deepLcExport(
                destinationStem, 
                identification, 
                modificationParameters, 
                sequenceMatchingParameters, 
                sequenceProvider, 
                spectrumProvider, 
                waitingHandler
        );
    }

    /**
     * Exports the files needed by ms2pip.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param searchParameters The search parameters.
     * @param sequenceMatchingParameters The sequence matching parameters.
     * @param sequenceProvider The sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param waitingHandler The waiting handler.
     * 
     * @return The files created by the export.
     */
    public static ArrayList<File> exportMs2pip(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            WaitingHandler waitingHandler
    ) {

        File destinationFile = followUpCLIInputBean.getMs2pipFile();
        String[] models = followUpCLIInputBean.getMs2pipModels();

        return Ms2PipExport.ms2pipExport(
                destinationFile, 
                models, 
                identification, 
                searchParameters, 
                sequenceMatchingParameters, 
                sequenceProvider, 
                spectrumProvider, 
                waitingHandler
        );
    }

    /**
     * Exports an inclusion list of the validated hits.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param spectrumProvider The spectrum provider.
     * @param searchParameters the search parameters
     * @param waitingHandler a waiting handler to display progress
     * @param filterPreferences the filter preferences
     * @return File file containing the inclusion list
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static File exportInclusionList(
            FollowUpCLIInputBean followUpCLIInputBean,
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SpectrumProvider spectrumProvider,
            SearchParameters searchParameters,
            WaitingHandler waitingHandler,
            FilterParameters filterPreferences
    ) throws IOException {

        ArrayList<InclusionListExport.PeptideFilterType> peptideFilterType = new ArrayList<>();

        for (int index : followUpCLIInputBean.getInclusionPeptideFilter()) {

            peptideFilterType.add(InclusionListExport.PeptideFilterType.getTypeFromIndex(index));

        }

        File destinationFileTemp = followUpCLIInputBean.getInclusionFile();

        if (!destinationFileTemp.exists()) {

            destinationFileTemp.createNewFile();

        }

        File destinationFile = destinationFileTemp;
        InclusionListExport.exportInclusionList(
                destinationFile,
                identification,
                identificationFeaturesGenerator,
                spectrumProvider,
                followUpCLIInputBean.getInclusionProteinFilter(),
                peptideFilterType,
                InclusionListExport.ExportFormat.getTypeFromIndex(
                        followUpCLIInputBean.getInclusionFormat()
                ),
                searchParameters,
                followUpCLIInputBean.getInclusionRtWindow(),
                waitingHandler,
                filterPreferences
        );
        return destinationFile;
    }

    /**
     * Writes an export according to the command line settings contained in the
     * reportCLIInputBean.
     *
     * @param reportCLIInputBean the command line settings
     * @param reportType the report type
     * @param experiment the experiment of the project
     * @param projectDetails the project details of the project
     * @param identification the identification of the project
     * @param geneMaps the gene maps
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param identificationParameters the identification parameters used
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param spectrumProvider The spectrum provider.
     * @param nSurroundingAA the number of amino acids to export on the side of
     * peptide sequences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param waitingHandler waiting handler displaying feedback to the user
     * @return File file containing the exported report
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static File exportReport(
            ReportCLIInputBean reportCLIInputBean,
            String reportType,
            String experiment,
            ProjectDetails projectDetails,
            Identification identification,
            GeneMaps geneMaps,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            int nSurroundingAA,
            SpectrumCountingParameters spectrumCountingPreferences,
            WaitingHandler waitingHandler
    ) throws IOException {

        PSExportFactory exportFactory = PSExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);

        String reportName = reportType.replaceAll(" ", "_");
        reportName = PSExportFactory.getDefaultReportName(experiment, reportName);
        if (reportCLIInputBean.getReportNamePrefix() != null) {
            reportName = reportCLIInputBean.getReportNamePrefix() + reportName;
        }

        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), reportName);

        //@TODO: allow format selection
        PSExportFactory.writeExport(
                exportScheme,
                reportFile,
                ExportFormat.text,
                reportCLIInputBean.isGzip(),
                experiment,
                projectDetails,
                identification,
                identificationFeaturesGenerator,
                geneMaps,
                null,
                null,
                null,
                nSurroundingAA,
                identificationParameters,
                sequenceProvider,
                proteinDetailsProvider,
                spectrumProvider,
                spectrumCountingPreferences,
                waitingHandler
        );

        return reportFile;
    }

    /**
     * Writes the documentation corresponding to an export given the command
     * line arguments.
     *
     * @param reportCLIInputBean the command line arguments
     * @param reportType the type of report of interest
     * @param waitingHandler waiting handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static void exportDocumentation(
            ReportCLIInputBean reportCLIInputBean,
            String reportType,
            WaitingHandler waitingHandler
    ) throws IOException {

        PSExportFactory exportFactory = PSExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);
        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), PSExportFactory.getDefaultDocumentation(reportType));

        //@TODO: allow format selection
        PSExportFactory.writeDocumentation(
                exportScheme,
                ExportFormat.text,
                reportFile
        );
    }

    /**
     * Exports the project in the mzIdentML format.
     *
     * @param mzidCLIInputBean the user input
     * @param psbdParent a psbd file parent allowing accessing the information it
     * contains
     * @param waitingHandler a waiting handler allowing display of progress and
     * interruption of the export
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static void exportMzId(
            MzidCLIInputBean mzidCLIInputBean,
            PsdbParent psbdParent,
            WaitingHandler waitingHandler
    ) throws IOException {

        ProjectDetails projectDetails = psbdParent.getProjectDetails();
        projectDetails.setContactFirstName(mzidCLIInputBean.getContactFirstName());
        projectDetails.setContactLastName(mzidCLIInputBean.getContactLastName());
        projectDetails.setContactEmail(mzidCLIInputBean.getContactEmail());
        projectDetails.setContactAddress(mzidCLIInputBean.getContactAddress());
        projectDetails.setContactUrl(mzidCLIInputBean.getContactUrl());
        projectDetails.setOrganizationName(mzidCLIInputBean.getOrganizationName());
        projectDetails.setOrganizationEmail(mzidCLIInputBean.getOrganizationMail());
        projectDetails.setOrganizationAddress(mzidCLIInputBean.getOrganizationAddress());
        projectDetails.setOrganizationUrl(mzidCLIInputBean.getOrganizationUrl());
        projectDetails.setIncludeProteinSequences(mzidCLIInputBean.getIncludeProteinSequences());
        projectDetails.setPrideOutputFolder(mzidCLIInputBean.getOutputFile().getAbsolutePath());

        IdentificationParameters identificationParameters = psbdParent.getIdentificationParameters();
        FastaSummary fastaSummary = FastaSummary.getSummary(projectDetails.getFastaFile(),
                identificationParameters.getFastaParameters(), waitingHandler);

        MzIdentMLExport mzIdentMLExport = new MzIdentMLExport(
                PeptideShaker.getVersion(),
                psbdParent.getIdentification(),
                psbdParent.getProjectDetails(),
                identificationParameters,
                psbdParent.getSequenceProvider(),
                psbdParent.getProteinDetailsProvider(),
                psbdParent.getSpectrumProvider(),
                ModificationFactory.getInstance(),
                fastaSummary,
                psbdParent.getIdentificationFeaturesGenerator(),
                mzidCLIInputBean.getOutputFile(),
                mzidCLIInputBean.getIncludeProteinSequences(),
                waitingHandler,
                mzidCLIInputBean.isGzip()
        );

        mzIdentMLExport.createMzIdentMLFile(mzidCLIInputBean.getMzIdentMLVersion());

    }
}
