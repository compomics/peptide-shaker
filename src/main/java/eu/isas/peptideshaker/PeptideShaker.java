package eu.isas.peptideshaker;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import eu.isas.peptideshaker.fdrestimation.InputMap;
import eu.isas.peptideshaker.fdrestimation.PeptideSpecificMap;
import eu.isas.peptideshaker.fdrestimation.SpectrumSpecificMap;
import eu.isas.peptideshaker.fdrestimation.TargetDecoyMap;
import eu.isas.peptideshaker.gui.StartPanel;
import eu.isas.peptideshaker.gui.WaitingDialog;
import eu.isas.peptideshaker.myparameters.SVParameter;
import eu.isas.peptideshaker.utils.Properties;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author  Marc Vaudel
 * @author  Harald Barsnes
 */
public class PeptideShaker {

    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The main frame.
     */
    private JFrame mainFrame;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    private MsExperiment experiment;
    private Sample sample;
    private int replicateNumber;
    SpectrumSpecificMap spectrumMap;
    PeptideSpecificMap peptideMap;
    TargetDecoyMap proteinMap;

    /**
     * Main method.
     *
     * @param args String[] with the start-up arguments.
     */
    public static void main(String[] args) {
        new PeptideShaker();
    }

    /**
     * main constructor.
     */
    public PeptideShaker() {
        // check if a newer version of PeptideShaker is available
        //checkForNewVersion(new Properties().getVersion());

        // set up the ErrorLog
        setUpLogFile();

        // Start the GUI
        createandshowGUI();
    }

    /**
     * Creates the GUI and adds the tabs to the frame. Then sets the size and
     * location of the frame and makes it visible.
     */
    private void createandshowGUI() {

        mainFrame = new JFrame("PeptideShaker " + new Properties().getVersion());

        mainFrame.addWindowListener(new WindowAdapter() {

            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                close(0);
            }
        });

        // sets the icon of the frame
        /**
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
        getResource("/icons/")));**/
        // update the look and feel after adding the panels
        setLookAndFeel();

        // display the start panel
        mainFrame.add(new StartPanel(this));

        // set size and location
        mainFrame.pack();
        //mainFrame.setResizable(false);
        mainFrame.setLocationRelativeTo(null);
        // Pack is the minimal size, so add 20 pixels in each dimension.
        mainFrame.setSize(new Dimension(mainFrame.getSize().width + 20, mainFrame.getSize().height));
        mainFrame.setVisible(true);
    }

    public void restart() {
        mainFrame.dispose();
        createandshowGUI();
    }

    /**
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running reporter
     */
    private static void checkForNewVersion(String currentVersion) {

        try {
            boolean deprecatedOrDeleted = false;
            URL downloadPage = new URL(
                    "http://code.google.com/p/peptide-shaker/downloads/detail?name=PeptideShaker-"
                    + currentVersion + ".zip");
            int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

            // 404 means that the file no longer exists, which means that
            // the running version is no longer available for download,
            // which again means that a never version is available.
            if (respons == 404) {
                deprecatedOrDeleted = true;
            } else {

                // also need to check if the available running version has been
                // deprecated (but not deleted)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(downloadPage.openStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
                    if (inputLine.lastIndexOf("Deprecated") != -1
                            && inputLine.lastIndexOf("Deprecated Downloads") == -1
                            && inputLine.lastIndexOf("Deprecated downloads") == -1) {
                        deprecatedOrDeleted = true;
                    }
                }

                in.close();
            }

            // informs the user about an updated version of the tool, unless the user
            // is running a beta version
            if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
                int option = JOptionPane.showConfirmDialog(null,
                        "A newer version of PeptideShaker is available.\n"
                        + "Do you want to upgrade?",
                        "Upgrade Available",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com/");
                    System.exit(0);
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/conf/PeptideShakerLog.log";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    file.createNewFile();

                    FileWriter w = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(w);

                    bw.close();
                    w.close();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the PeptideShaker Log.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    private String getJarFilePath() {
        String path = this.getClass().getResource("PeptideShaker.class").getPath();

        if (path.lastIndexOf("/PeptideShaker-") != -1) {
            path = path.substring(5, path.lastIndexOf("/PeptideShaker-"));
            path = path.replace("%20", " ");
        } else {
            path = ".";
        }

        return path;
    }

    /**
     * This method terminates the program.
     *
     * @param aStatus int with the completion status.
     */
    public void close(int aStatus) {
        mainFrame.setVisible(false);
        mainFrame.dispose();
        System.exit(aStatus);
    }

    /**
     * Sets the look and feel of the SearchGUI.
     * <p/>
     * Note that the GUI has been created with the following look and feel
     * in mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private void setLookAndFeel() {

        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            SwingUtilities.updateComponentTreeUI(mainFrame);
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public void displayResults() {


        mainFrame = new JFrame("PeptideShaker ");

        mainFrame.addWindowListener(new WindowAdapter() {

            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                close(0);
            }
        });

        // sets the icon of the frame
        /**
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
        getResource("/icons/")));**/
        // update the look and feel after adding the panels
        setLookAndFeel();

        // display the start panel
        //mainFrame.add(new ResultPanel(this, experiment, sample, replicateNumber, proteinMap, peptideMap, spectrumMap));

        // set size and location
        mainFrame.pack();
        //mainFrame.setResizable(false);
        mainFrame.setLocationRelativeTo(null);
        // Pack is the minimal size, so add 20 pixels in each dimension.
        mainFrame.setSize(new Dimension(mainFrame.getSize().width + 20, mainFrame.getSize().height));
        mainFrame.setVisible(true);
    }

    /**
     * @return the lastSelectedFolder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * @param lastSelectedFolder the lastSelectedFolder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

//    public void importIdentifications(MsExperiment experiment, Sample sample, int replicateNumber, IdFilter idFilter, ArrayList<File> idFiles) {
//        this.experiment = experiment;
//        this.sample = sample;
//        this.replicateNumber = replicateNumber;
//        WaitingDialog waitingDialog = new WaitingDialog(mainFrame, true, this, experiment.getReference());
//        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
//        Ms2Identification identification = new Ms2Identification();
//        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, identification);
//        IdImporter idImporter = new IdImporter(this, waitingDialog, identification, idFilter);
//        idImporter.importFiles(idFiles);
//    }

//    /**
//     * Method for processing of results from utilities data (no file). From ms_lims for instance.
//     * @param sample            The reference sample
//     * @param replicateNumber   The replicate number
//     */
//    public void processIdentifications(Sample sample, int replicateNumber) {
//        this.sample = sample;
//        this.replicateNumber = replicateNumber;
//        WaitingDialog waitingDialog = new WaitingDialog(mainFrame, true, this, experiment.getReference());
//        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
//        IdImporter idImporter = new IdImporter(this, waitingDialog, identification);
//        idImporter.importIdentifications();
//    }

    public void processIdentifications(InputMap inputMap, WaitingDialog waitingDialog, Identification identification) {
        if (inputMap.isMultipleSearchEngines()) {
            inputMap.computeProbabilities(waitingDialog);
        }
        waitingDialog.appendReport("Computing spectrum probabilities.");
        spectrumMap = new SpectrumSpecificMap();
        fillSpectrumMap(identification, inputMap);
        spectrumMap.cure(waitingDialog);
        spectrumMap.estimateProbabilities(waitingDialog);
        attachSpectrumProbabilities(identification);
        waitingDialog.appendReport("Computing peptide probabilities.");
        peptideMap = new PeptideSpecificMap();
        PeptideSpecificMap peptideSpecificMap = new PeptideSpecificMap(); //  @TODO: remove??
        fillPeptideMaps(identification);
        peptideMap.cure(waitingDialog);
        peptideMap.estimateProbabilities(waitingDialog);
        attachPeptideProbabilities(identification);
        waitingDialog.appendReport("Computing protein probabilities.");
        proteinMap = new TargetDecoyMap("protein");
        fillProteinMap(identification);
        proteinMap.estimateProbabilities(waitingDialog);
        attachProteinProbabilities(proteinMap, identification);
        waitingDialog.appendReport("Identification processing completed.");
        waitingDialog.setRunFinished();
    }

    private void fillSpectrumMap(Identification identification, InputMap inputMap) {
        HashMap<String, Double> identifications;
        HashMap<Double, PeptideAssumption> peptideAssumptions;
        SVParameter svParameter;
        PeptideAssumption peptideAssumption;
        if (inputMap.isMultipleSearchEngines()) {
            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                svParameter = new SVParameter();
                identifications = new HashMap<String, Double>();
                peptideAssumptions = new HashMap<Double, PeptideAssumption>();
                String id;
                double p, pScore = 1;
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    p = inputMap.getProbability(searchEngine, peptideAssumption.getEValue());
                    pScore = pScore * p;
                    id = peptideAssumption.getPeptide().getIndex();
                    if (identifications.containsKey(id)) {
                        p = identifications.get(id) * p;
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    } else {
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    }
                }
                double pMin = Collections.min(identifications.values());
                svParameter.setSpectrumProbabilityScore(pScore);
                spectrumMatch.addUrParam(svParameter);
                spectrumMatch.setBestAssumption(peptideAssumptions.get(pMin));
                spectrumMap.addPoint(pScore, spectrumMatch);
            }
        } else {
            double eValue;
            for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
                svParameter = new SVParameter();
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    eValue = peptideAssumption.getEValue();
                    svParameter.setSpectrumProbabilityScore(eValue);
                    spectrumMatch.setBestAssumption(peptideAssumption);
                    spectrumMap.addPoint(eValue, spectrumMatch);
                }
                spectrumMatch.addUrParam(svParameter);
            }
        }
    }

    private void attachSpectrumProbabilities(Identification identification) {
        SVParameter svParameter = new SVParameter();
        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
            svParameter = (SVParameter) spectrumMatch.getUrParam(svParameter);
            svParameter.setSpectrumProbability(spectrumMap.getProbability(spectrumMatch, svParameter.getSpectrumProbabilityScore()));
        }
    }

    private void fillPeptideMaps(Identification identification) {
        double probaScore;
        SVParameter svParameter = new SVParameter();
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
            probaScore = 1;
            for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                if (spectrumMatch.getBestAssumption().getPeptide().isSameAs(peptideMatch.getTheoreticPeptide())) {
                    svParameter = (SVParameter) spectrumMatch.getUrParam(svParameter);
                    probaScore = probaScore * svParameter.getSpectrumProbability();
                }
            }
            svParameter = new SVParameter();
            svParameter.setPeptideProbabilityScore(probaScore);
            peptideMatch.addUrParam(svParameter);
            peptideMap.addPoint(probaScore, peptideMatch);
        }
    }

    private void attachPeptideProbabilities(Identification identification) {
        SVParameter svParameter = new SVParameter();
        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {
            svParameter = (SVParameter) peptideMatch.getUrParam(svParameter);
            svParameter.setPeptideProbability(peptideMap.getProbability(peptideMatch, svParameter.getPeptideProbabilityScore()));
        }
    }

    private void fillProteinMap(Identification identification) {
        double probaScore;
        SVParameter svParameter = new SVParameter();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            probaScore = 1;
            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                if (peptideMatch.getTheoreticPeptide().getParentProteins().size() == 1) {
                    svParameter = (SVParameter) peptideMatch.getUrParam(svParameter);
                    probaScore = probaScore * svParameter.getPeptideProbability();
                }
            }
            svParameter = new SVParameter();
            svParameter.setProteinProbabilityScore(probaScore);
            proteinMatch.addUrParam(svParameter);
            proteinMap.put(probaScore, proteinMatch.isDecoy());
        }
    }

    private void attachProteinProbabilities(TargetDecoyMap proteinMap, Identification identification) {
        SVParameter svParameter = new SVParameter();
        for (ProteinMatch proteinMatch : identification.getProteinIdentification().values()) {
            svParameter = (SVParameter) proteinMatch.getUrParam(svParameter);
            svParameter.setProteinProbability(proteinMap.getProbability(svParameter.getProteinProbabilityScore()));
        }
    }
}
