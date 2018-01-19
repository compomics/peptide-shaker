package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.PSExportFactory;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.MzIdentMLExport;
import eu.isas.peptideshaker.followup.FastaExport;
import eu.isas.peptideshaker.followup.InclusionListExport;
import eu.isas.peptideshaker.followup.TrainingExport;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import eu.isas.peptideshaker.followup.RecalibrationExporter;
import eu.isas.peptideshaker.followup.SpectrumExporter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

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
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress
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
     */
    public static void recalibrateSpectra(FollowUpCLIInputBean followUpCLIInputBean, Identification identification,
            IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
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
        RecalibrationExporter.writeRecalibratedSpectra(ms1, ms2, recalibrationFolder, identification, identificationParameters, waitingHandler);
    }

    /**
     * Exports the spectra as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     * @param sequenceMatchingPreferences the sequence matching preferences
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
     */
    public static void exportSpectra(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, WaitingHandler waitingHandler, SequenceMatchingPreferences sequenceMatchingPreferences) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
        File exportFolder = followUpCLIInputBean.getSpectrumExportFolder();
        if (!exportFolder.exists()) {
            exportFolder.mkdir();
        }
        SpectrumExporter spectrumExporter = new SpectrumExporter(identification);
        spectrumExporter.exportSpectra(exportFolder, waitingHandler, SpectrumExporter.ExportType.getTypeFromIndex(followUpCLIInputBean.getSpectrumExportTypeIndex()), sequenceMatchingPreferences);
    }

    /**
     * Exports the accessions as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param waitingHandler a waiting handler to display progress
     * @param filteringPreferences the filtering preferences
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
    public static void exportAccessions(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler, FilterPreferences filteringPreferences) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFileTemp = followUpCLIInputBean.getAccessionsExportFile();
        if (!destinationFileTemp.exists()) {
            destinationFileTemp.createNewFile();
        }
        File destinationFile = destinationFileTemp;
        FastaExport.exportAccessions(destinationFile, identification, identificationFeaturesGenerator, FastaExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getAccessionsExportTypeIndex()), waitingHandler, filteringPreferences);
    }

    /**
     * Exports the protein details in FASTA format as specified in the follow-up
     * input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param waitingHandler a waiting handler to display progress
     * @param filteringPreferences the filtering preferences
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
    public static void exportFasta(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler, FilterPreferences filteringPreferences) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFileTemp = followUpCLIInputBean.getFastaExportFile();
        if (!destinationFileTemp.exists()) {
            destinationFileTemp.createNewFile();
        }
        File destinationFile = destinationFileTemp;
        FastaExport.exportFasta(destinationFile, identification, identificationFeaturesGenerator, FastaExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getFastaExportTypeIndex()), waitingHandler, filteringPreferences);
    }

    /**
     * Exports the identification in a Progenesis compatible format.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     * @param sequenceMatchingPreferences the sequence matching preferences
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
    public static void exportProgenesis(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, WaitingHandler waitingHandler, SequenceMatchingPreferences sequenceMatchingPreferences) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFileTemp = followUpCLIInputBean.getProgenesisExportFile();
        if (!destinationFileTemp.exists()) {
            destinationFileTemp.createNewFile();
        }
        File destinationFile = destinationFileTemp;
        ProgenesisExport.writeProgenesisExport(destinationFile, identification, ProgenesisExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getProgenesisExportTypeIndex()), waitingHandler, followUpCLIInputBean.getProgenesisTargetedPTMs(), sequenceMatchingPreferences);
    }

    /**
     * Exports the files needed for the PepNovo training.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress
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
     */
    public static void exportPepnovoTrainingFiles(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        File destinationFolder = followUpCLIInputBean.getPepnovoTrainingFolder();
        if (!destinationFolder.exists()) {
            destinationFolder.mkdir();
        }
        TrainingExport.exportPepnovoTrainingFiles(destinationFolder, identification, identificationParameters, followUpCLIInputBean.getPepnovoTrainingFDR(), followUpCLIInputBean.getPepnovoTrainingFNR(), followUpCLIInputBean.isPepnovoTrainingRecalibrate(), waitingHandler);
    }

    /**
     * Exports an inclusion list of the validated hits.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param searchParameters the search parameters
     * @param waitingHandler a waiting handler to display progress
     * @param filterPreferences the filter preferences
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
     */
    public static void exportInclusionList(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters, WaitingHandler waitingHandler, FilterPreferences filterPreferences) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        ArrayList<InclusionListExport.PeptideFilterType> peptideFilterType = new ArrayList<InclusionListExport.PeptideFilterType>();
        for (int index : followUpCLIInputBean.getInclusionPeptideFilter()) {
            peptideFilterType.add(InclusionListExport.PeptideFilterType.getTypeFromIndex(index));
        }
        File destinationFileTemp = followUpCLIInputBean.getInclusionFile();
        if (!destinationFileTemp.exists()) {
            destinationFileTemp.createNewFile();
        }
        File destinationFile = destinationFileTemp;
        InclusionListExport.exportInclusionList(destinationFile, identification, identificationFeaturesGenerator, followUpCLIInputBean.getInclusionProteinFilter(), peptideFilterType, InclusionListExport.ExportFormat.getTypeFromIndex(followUpCLIInputBean.getInclusionFormat()), searchParameters, followUpCLIInputBean.getInclusionRtWindow(), waitingHandler, filterPreferences);
    }

    /**
     * Writes an export according to the command line settings contained in the
     * reportCLIInputBean.
     *
     * @param reportCLIInputBean the command line settings
     * @param reportType the report type
     * @param experiment the experiment of the project
     * @param sample the sample of the project
     * @param replicateNumber the replicate number of the project
     * @param projectDetails the project details of the project
     * @param identification the identification of the project
     * @param geneMaps the gene maps
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param identificationParameters the identification parameters used
     * @param nSurroundingAA the number of amino acids to export on the side of
     * peptide sequences
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param waitingHandler waiting handler displaying feedback to the user
     * @return File file containing the exported report
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
    public static File exportReport(ReportCLIInputBean reportCLIInputBean, String reportType, String experiment, String sample, int replicateNumber,
            ProjectDetails projectDetails, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            IdentificationParameters identificationParameters, int nSurroundingAA, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler)
            throws IOException, SQLException, ClassNotFoundException,
            InterruptedException, MzMLUnmarshallerException, MathException {

        PSExportFactory exportFactory = PSExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);
        String reportName = reportType.replaceAll(" ", "_");
        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), PSExportFactory.getDefaultReportName(experiment, sample, replicateNumber, reportName));

        //@TODO: allow format selection
        PSExportFactory.writeExport(exportScheme, reportFile, ExportFormat.text, experiment, sample, replicateNumber, projectDetails, identification, identificationFeaturesGenerator, geneMaps,
                null, null, null, null, nSurroundingAA, identificationParameters, spectrumCountingPreferences, waitingHandler);
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
    public static void exportDocumentation(ReportCLIInputBean reportCLIInputBean, String reportType, WaitingHandler waitingHandler) throws IOException {
        PSExportFactory exportFactory = PSExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);
        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), PSExportFactory.getDefaultDocumentation(reportType));

        //@TODO: allow format selection
        PSExportFactory.writeDocumentation(exportScheme, ExportFormat.text, reportFile);
    }

    /**
     * Exports the project in the mzIdentML format.
     *
     * @param mzidCLIInputBean the user input
     * @param cpsParent a cps file parent allowing accessing the information it
     * contains
     * @param waitingHandler a waiting handler allowing display of progress and
     * interruption of the export
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
     * @throws org.apache.commons.math.MathException exception thrown whenever a
     * math error occurred
     */
    public static void exportMzId(MzidCLIInputBean mzidCLIInputBean, CpsParent cpsParent, WaitingHandler waitingHandler)
            throws IOException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException, SQLException, MathException {

        ProjectDetails projectDetails = cpsParent.getProjectDetails();
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

        MzIdentMLExport mzIdentMLExport = new MzIdentMLExport(PeptideShaker.getVersion(), cpsParent.getIdentification(), cpsParent.getProjectDetails(),
                cpsParent.getShotgunProtocol(), cpsParent.getIdentificationParameters(),
                cpsParent.getSpectrumCountingPreferences(), cpsParent.getIdentificationFeaturesGenerator(),
                mzidCLIInputBean.getOutputFile(), mzidCLIInputBean.getIncludeProteinSequences(), waitingHandler);
        mzIdentMLExport.createMzIdentMLFile(mzidCLIInputBean.getMzIdentMLVersion());
    }
}
