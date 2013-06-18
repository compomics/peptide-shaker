package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.followup.FastaExport;
import eu.isas.peptideshaker.followup.InclusionListExport;
import eu.isas.peptideshaker.followup.PepnovoTrainingExport;
import eu.isas.peptideshaker.followup.ProgenesisExport;
import eu.isas.peptideshaker.followup.RecalibrationExporter;
import eu.isas.peptideshaker.followup.SpectrumExporter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class groups standard methods used by the different command line
 * interfaces.
 *
 * @author Marc Vaudel
 */
public class CLIMethods {

    /**
     * Recalibrates spectra as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param annotationPreferences the annotation preferences
     * @param waitingHandler a waiting handler to display progress
     *
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void recalibrateSpectra(FollowUpCLIInputBean followUpCLIInputBean, Identification identification,
            AnnotationPreferences annotationPreferences, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
        File recalibrationFolder = followUpCLIInputBean.getRecalibrationFolder();
        boolean ms1 = true;
        boolean ms2 = true;
        if (followUpCLIInputBean.getRecalibrationMode() == 1) {
            ms2 = false;
        } else if (followUpCLIInputBean.getRecalibrationMode() == 2) {
            ms1 = false;
        }
        RecalibrationExporter.writeRecalibratedSpectra(ms1, ms2, recalibrationFolder, identification, annotationPreferences, waitingHandler);
    }

    /**
     * Exports the spectra as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     *
     * @throws IOException
     * @throws MzMLUnmarshallerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportSpectra(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, WaitingHandler waitingHandler) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {
        File exportFolder = followUpCLIInputBean.getSpectrumExportFolder();
        SpectrumExporter spectrumExporter = new SpectrumExporter(identification);
        spectrumExporter.exportSpectra(exportFolder, waitingHandler, SpectrumExporter.ExportType.getTypeFromIndex(followUpCLIInputBean.getSpectrumExportTypeIndex()));
    }

    /**
     * Exports the accessions as specified in the follow-up input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param waitingHandler a waiting handler to display progress
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportAccessions(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFile = followUpCLIInputBean.getAccessionsExportFile();
        FastaExport.exportAccessions(destinationFile, identification, identificationFeaturesGenerator, FastaExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getAccessionsExportTypeIndex()), waitingHandler);
    }

    /**
     * Exports the protein details in Fasta format as specified in the follow-up
     * input bean.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param waitingHandler a waiting handler to display progress
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportFasta(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFile = followUpCLIInputBean.getFastaExportFile();
        FastaExport.exportFasta(destinationFile, identification, identificationFeaturesGenerator, FastaExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getFastaExportTypeIndex()), waitingHandler);
    }

    /**
     * Exports the identification in a progenesis compatible format.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param waitingHandler a waiting handler to display progress
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportProgenesis(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
        File destinationFile = followUpCLIInputBean.getProgenesisExportFile();
        ProgenesisExport.writeProgenesisExport(destinationFile, identification, ProgenesisExport.ExportType.getTypeFromIndex(followUpCLIInputBean.getProgenesisExportTypeIndex()), waitingHandler);
    }

    /**
     * Exports the files needed for the pepnovo training.
     *
     * @param followUpCLIInputBean the follow up input bean
     * @param identification the identification
     * @param annotationPreferences the annotation preferences
     * @param waitingHandler a waiting handler to display progress
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void exportPepnovoTrainingFiles(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, AnnotationPreferences annotationPreferences, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        File destinationFolder = followUpCLIInputBean.getPepnovoTrainingFolder();
        PepnovoTrainingExport.exportPepnovoTrainingFiles(destinationFolder, identification, annotationPreferences, followUpCLIInputBean.getPepnovoTrainingFDR(), followUpCLIInputBean.getPepnovoTrainingFNR(), followUpCLIInputBean.isPepnovoTrainingRecalibrate(), waitingHandler);
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
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void exportInclusionList(FollowUpCLIInputBean followUpCLIInputBean, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        ArrayList<InclusionListExport.PeptideFilterType> peptideFilterType = new ArrayList<InclusionListExport.PeptideFilterType>();
        for (int index : followUpCLIInputBean.getInclusionPeptideFilter()) {
            peptideFilterType.add(InclusionListExport.PeptideFilterType.getTypeFromIndex(index));
        }
        File detinationFile = followUpCLIInputBean.getInclusionFile();
        InclusionListExport.exportInclusionList(detinationFile, identification, identificationFeaturesGenerator, followUpCLIInputBean.getInclusionProteinFilter(), peptideFilterType, InclusionListExport.ExportFormat.getTypeFromIndex(followUpCLIInputBean.getInclusionFormat()), searchParameters, followUpCLIInputBean.getInclusionRtWindow(), waitingHandler);
    }
}
