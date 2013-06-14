package eu.isas.peptideshaker.fileimport;

import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.io.TarUtils;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * The Cps file importer extracts the information contained in a cps file.
 *
 * @author Marc Vaudel
 */
public class CpsFileImporter {

    /**
     * The experiment object.
     */
    private MsExperiment experiment;

    /**
     * Constructor.
     * 
     * @param cpsFile the cps file
     * @param waitingHandler the waiting handler
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public CpsFileImporter(File cpsFile, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {

        File matchFolder = new File(PeptideShaker.SERIALIZATION_DIRECTORY);

        // empty the existing files in the matches folder
        if (matchFolder.exists()) {
            for (File file : matchFolder.listFiles()) {
                if (file.isDirectory()) {
                    boolean deleted = Util.deleteDir(file);

                    if (!deleted) {
                        System.out.println("Failed to delete folder: " + file.getPath());
                    }
                } else {
                    boolean deleted = file.delete();

                    if (!deleted) {
                        System.out.println("Failed to delete file: " + file.getPath());
                    }
                }
            }
        }

        File experimentFile = PeptideShaker.getDefaultExperimentFile();

        if (waitingHandler != null) {
            waitingHandler.resetSecondaryProgressBar();
            waitingHandler.setMaxSecondaryProgressValue(100);
        }

        try {
            TarUtils.extractFile(cpsFile, waitingHandler);
        } catch (ArchiveException e) {
            //Most likely an old project
            experimentFile = cpsFile;
            e.printStackTrace();
        }

        experiment = ExperimentIO.loadExperiment(experimentFile);
    }

    /**
     * Returns the experiment settings as imported from the cps file.
     *
     * @return the experiment settings as imported from the cps file
     */
    public PeptideShakerSettings getExperimentSettings() {

        PeptideShakerSettings experimentSettings = new PeptideShakerSettings();

        if (experiment.getUrParam(experimentSettings) instanceof PSSettings) {

            // convert old settings files using utilities version 3.10.68 or older

            // convert the old ProcessingPreferences object
            PSSettings tempSettings = (PSSettings) experiment.getUrParam(experimentSettings);
            ProcessingPreferences tempProcessingPreferences = new ProcessingPreferences();
            tempProcessingPreferences.setProteinFDR(tempSettings.getProcessingPreferences().getProteinFDR());
            tempProcessingPreferences.setPeptideFDR(tempSettings.getProcessingPreferences().getPeptideFDR());
            tempProcessingPreferences.setPsmFDR(tempSettings.getProcessingPreferences().getPsmFDR());

            // convert the old PTMScoringPreferences object
            PTMScoringPreferences tempPTMScoringPreferences = new PTMScoringPreferences();
            tempPTMScoringPreferences.setaScoreCalculation(tempSettings.getPTMScoringPreferences().aScoreCalculation());
            tempPTMScoringPreferences.setaScoreNeutralLosses(tempSettings.getPTMScoringPreferences().isaScoreNeutralLosses());
            tempPTMScoringPreferences.setFlrThreshold(tempSettings.getPTMScoringPreferences().getFlrThreshold());

            experimentSettings = new PeptideShakerSettings(tempSettings.getSearchParameters(), tempSettings.getAnnotationPreferences(),
                    tempSettings.getSpectrumCountingPreferences(), tempSettings.getProjectDetails(), tempSettings.getFilterPreferences(),
                    tempSettings.getDisplayPreferences(),
                    tempSettings.getMetrics(), tempProcessingPreferences, tempSettings.getIdentificationFeaturesCache(),
                    tempPTMScoringPreferences, new GenePreferences(), new IdFilter());

        } else {
            experimentSettings = (PeptideShakerSettings) experiment.getUrParam(experimentSettings);
        }

        return experimentSettings;

    }

    /**
     * Returns the samples.
     * 
     * @return the samples
     */
    public ArrayList<Sample> getSamples() {
        return new ArrayList(experiment.getSamples().values());
    }

    /**
     * Returns the replicates for a given sample.
     * 
     * @param sample
     * @return the replicates
     */
    public ArrayList<Integer> getReplicates(Sample sample) {
        return new ArrayList(experiment.getAnalysisSet(sample).getReplicateNumberList());
    }

    /**
     * Returns the experiment.
     * 
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return experiment;
    }
}
