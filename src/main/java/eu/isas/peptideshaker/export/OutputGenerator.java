package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.ptm.ptmscores.MDScore;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import no.uib.jsparklines.data.XYDataPoint;

/**
 * This class will generate the output as requested by the user.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class OutputGenerator {

    // @TODO: this class shall evolve to something gui independent or will disappear
    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The corresponding identification.
     */
    private Identification identification;
    /**
     * The separator (tab by default).
     */
    public static final String SEPARATOR = "\t";
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;
    /**
     * The line break type.
     */
    private String lineBreak = System.getProperty("line.separator");

    /**
     * Constructor.
     *
     * @param peptideShakerGUI the PeptideShakerGUI
     */
    public OutputGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
    }

    /**
     * Sends the desired protein output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param aParentDialog the parent dialog, can be null.
     * @param aProteinKeys The list of protein keys to output. If null, the
     * identification list will be used
     * @param aIndexes boolean indicating whether the first column shall be used
     * for line number
     * @param aOnlyValidated boolean indicating whether only validated hits
     * shall be returned
     * @param aMainAccession boolean indicating whether the accessions shall be
     * output
     * @param aOtherAccessions boolean indicating whether the the additional
     * protein accession numbers should be included or not
     * @param aPiDetails boolean indicating whether protein inference details
     * shall be output
     * @param aDescription boolean indicating whether protein description of the
     * main match shall be output
     * @param aNPeptides boolean indicating whether the number of validated
     * peptides shall be output
     * @param aEmPAI boolean indicating whether the emPAI index shall be output
     * @param aSequenceCoverage boolean indicating whether the sequence coverage
     * shall be output
     * @param aPtmSummary boolean indicating whether a ptm summary shall be
     * output
     * @param aNSpectra boolean indicating whether the number of validated
     * spectra shall be output
     * @param aNsaf boolean indicating whether the NSAF index shall be output
     * @param aScore boolean indicating whether the protein match score shall be
     * output
     * @param aConfidence boolean indicating whether the confidence shall be
     * output
     * @param aMW boolean indicating whether the molecular weight is to be
     * included in the output
     * @param aIncludeHeader boolean indicating whether the header shall be
     * output
     * @param aOnlyStarred boolean indicating whether only starred proteins
     * shall be output
     * @param aShowStar boolean indicating whether the starred proteins will be
     * indicated in a separate column
     * @param aIncludeHidden boolean indicating whether hidden hits shall be
     * output
     * @param aMaximalProteinSet if true an additional file with the maximal
     * protein set is created
     * @param aShowNonEnzymaticPeptidesColumn if true, a column indicating if
     * the protein has one or more non enzymatic peptides will be included
     * @param aGeneName if true, a column with the gene name will be included
     * @param aChromosomeNumber if true, a column indicating with the chromosome
     * number will be included
     */
    public void getProteinsOutput(JDialog aParentDialog, ArrayList<String> aProteinKeys, boolean aIndexes, boolean aOnlyValidated, boolean aMainAccession, boolean aOtherAccessions, boolean aPiDetails,
            boolean aDescription, boolean aNPeptides, boolean aEmPAI, boolean aSequenceCoverage, boolean aPtmSummary, boolean aNSpectra, boolean aNsaf,
            boolean aScore, boolean aConfidence, boolean aMW, boolean aIncludeHeader, boolean aOnlyStarred, boolean aShowStar, boolean aIncludeHidden, boolean aMaximalProteinSet,
            boolean aShowNonEnzymaticPeptidesColumn, boolean aGeneName, boolean aChromosomeNumber) {

        // create final versions of all variables use inside the export thread
        final ArrayList<String> proteinKeys;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean mainAccession = aMainAccession;
        final boolean otherAccessions = aOtherAccessions;
        final boolean piDetails = aPiDetails;
        final boolean description = aDescription;
        final boolean nPeptides = aNPeptides;
        final boolean emPAI = aEmPAI;
        final boolean sequenceCoverage = aSequenceCoverage;
        final boolean ptmSummary = aPtmSummary;
        final boolean nSpectra = aNSpectra;
        final boolean nsaf = aNsaf;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean mw = aMW;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean showStar = aShowStar;
        final boolean includeHidden = aIncludeHidden;
        final boolean createMaximalProteinSet = aMaximalProteinSet;
        final boolean showNonEnzymaticPeptidesColumn = aShowNonEnzymaticPeptidesColumn;
        final boolean geneName = aGeneName;
        final boolean chromosomeNumber = aChromosomeNumber;

        final JDialog parentDialog = aParentDialog;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile("protein_export.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            final String filePath = selectedFile.getPath();

            try {
                writer = new BufferedWriter(new FileWriter(selectedFile));

                if (aProteinKeys == null) {
                    if (onlyValidated) {
                        proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getValidatedProteins(progressDialog, peptideShakerGUI.getFilterPreferences());
                    } else {
                        proteinKeys = new ArrayList<String>(identification.getProteinIdentification());
                    }
                } else {
                    proteinKeys = aProteinKeys;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setTitle("Copying to File. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {
                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (mainAccession) {
                                writer.write("Accession" + SEPARATOR);
                            }
                            if (otherAccessions) {
                                writer.write("Other Protein(s) (alphabetical order)" + SEPARATOR);
                                writer.write("Complete Protein Group (alphabetical order)" + SEPARATOR);
                            }
                            if (piDetails) {
                                writer.write("Protein Inference Class" + SEPARATOR);
                            }
                            if (description) {
                                writer.write("Description" + SEPARATOR);
                            }
                            if (geneName) {
                                writer.write("Gene Name" + SEPARATOR);
                            }
                            if (chromosomeNumber) {
                                writer.write("Chromosome" + SEPARATOR);
                            }
                            if (sequenceCoverage) {
                                writer.write("Sequence Coverage (%)" + SEPARATOR);
                                writer.write("Observable Coverage (%)" + SEPARATOR);
                            }
                            if (showNonEnzymaticPeptidesColumn) {
                                writer.write("Non Enzymatic Peptides" + SEPARATOR);
                            }
                            if (ptmSummary) {
                                writer.write("Confident PTM Sites" + SEPARATOR);
                                writer.write("Ambiguous PTM Sites" + SEPARATOR);
                            }
                            if (nPeptides) {
                                writer.write("#Validated Peptides" + SEPARATOR);
                            }
                            if (nSpectra) {
                                writer.write("#Validated Spectra" + SEPARATOR);
                            }
                            if (emPAI) {
                                writer.write("emPAI" + SEPARATOR);
                            }
                            if (nsaf) {
                                writer.write("NSAF" + SEPARATOR);
                            }
                            if (mw) {
                                writer.write("MW (kDa)" + SEPARATOR);
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            if (!onlyValidated) {
                                writer.write("Validation" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            if (!onlyStarred && showStar) {
                                writer.write("Starred" + SEPARATOR);
                            }
                            writer.write(lineBreak);
                        }

                        PSParameter proteinPSParameter = new PSParameter();
                        PSParameter peptidePSParameter = new PSParameter();
                        int proteinCounter = 0;

                        progressDialog.setTitle("Loading Protein Matches. Please Wait...");
                        identification.loadProteinMatches(progressDialog, true);
                        progressDialog.setTitle("Loading Protein Details. Please Wait...");
                        identification.loadProteinMatchParameters(proteinPSParameter, progressDialog, true);

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        progressDialog.setMaxPrimaryProgressCounter(proteinKeys.size());
                        progressDialog.setValue(0);
                        progressDialog.setTitle("Copying to File. Please Wait...");

                        // store the maximal protein set of validated proteins
                        ArrayList<String> maximalProteinSet = new ArrayList<String>();

                        for (String proteinKey : proteinKeys) { // @TODO: replace by batch selection!!!

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinPSParameter);

                            if (!ProteinMatch.isDecoy(proteinKey) || !onlyValidated) {
                                if ((onlyValidated && proteinPSParameter.getMatchValidationLevel().isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !proteinPSParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && proteinPSParameter.isStarred()) || !onlyStarred) {
                                            if (indexes) {
                                                writer.write(++proteinCounter + SEPARATOR);
                                            }

                                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                                            if (mainAccession) {
                                                writer.write(proteinMatch.getMainMatch() + SEPARATOR);
                                            }
                                            if (createMaximalProteinSet && !maximalProteinSet.contains(proteinMatch.getMainMatch())) {
                                                maximalProteinSet.add(proteinMatch.getMainMatch());
                                            }
                                            if (createMaximalProteinSet || otherAccessions) {
                                                boolean first = true;

                                                // sort so that the protein accessions always come in the same order
                                                ArrayList<String> allProteins = proteinMatch.getTheoreticProteinsAccessions();
                                                Collections.sort(allProteins);
                                                StringBuilder completeProteinGroup = new StringBuilder();

                                                for (String otherProtein : allProteins) {
                                                    if (otherAccessions && !otherProtein.equalsIgnoreCase(proteinMatch.getMainMatch())) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        writer.write(otherProtein);
                                                    }
                                                    if (createMaximalProteinSet && !maximalProteinSet.contains(otherProtein)) {
                                                        maximalProteinSet.add(otherProtein);
                                                    }

                                                    if (completeProteinGroup.length() > 0) {
                                                        completeProteinGroup.append(", ");
                                                    }

                                                    completeProteinGroup.append(otherProtein);
                                                }

                                                if (otherAccessions) {
                                                    writer.write(SEPARATOR);
                                                    writer.write(completeProteinGroup.toString());
                                                    writer.write(SEPARATOR);
                                                }
                                            }
                                            if (piDetails) {
                                                writer.write(proteinPSParameter.getProteinInferenceClassAsString() + SEPARATOR);
                                            }
                                            if (description) {
                                                try {
                                                    writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription() + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }

                                            // gene name and chromosome number
                                            String tempGeneName = null;
                                            if (geneName || chromosomeNumber) {
                                                tempGeneName = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getGeneName();
                                            }
                                            if (geneName) {
                                                if (tempGeneName != null && !identification.getProteinMatch(proteinKey).isDecoy()) {
                                                    writer.write(tempGeneName);
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (chromosomeNumber) {
                                                GeneMaps geneMaps = peptideShakerGUI.getGeneMaps();
                                                String chromosomeNumber = geneMaps.getChromosome(tempGeneName);
                                                if (chromosomeNumber != null && !identification.getProteinMatch(proteinKey).isDecoy()) {
                                                    writer.write(chromosomeNumber);
                                                }
                                                writer.write(SEPARATOR);
                                            }

                                            if (sequenceCoverage) {
                                                try {
                                                    HashMap<Integer, Double> sequenceCoverage;
                                                    try {
                                                        sequenceCoverage = peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                                                    } catch (Exception e) {
                                                        peptideShakerGUI.catchException(e);
                                                        sequenceCoverage = new HashMap<Integer, Double>();
                                                    }
                                                    Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                                                    Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                                                    Double validatedCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful;
                                                    writer.write(validatedCoverage + SEPARATOR);
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey) * 100 + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }
                                            if (showNonEnzymaticPeptidesColumn) {

                                                ArrayList<String> peptideKeys = proteinMatch.getPeptideMatchesKeys();
                                                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                                boolean allPeptidesEnzymatic = true;

                                                identification.loadPeptideMatches(peptideKeys, null, true);
                                                identification.loadPeptideMatchParameters(peptideKeys, peptidePSParameter, null, true);

                                                // see if we have non-tryptic peptides
                                                for (String peptideKey : peptideKeys) {

                                                    String peptideSequence = identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getSequence();
                                                    peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);

                                                    if (peptidePSParameter.getMatchValidationLevel().isValidated()) {

                                                        boolean isEnzymatic = currentProtein.isEnzymaticPeptide(peptideSequence,
                                                                peptideShakerGUI.getShotgunProtocol().getEnzyme(),
                                                                peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());

                                                        if (!isEnzymatic) {
                                                            allPeptidesEnzymatic = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                writer.write(!allPeptidesEnzymatic + SEPARATOR);
                                            }
                                            if (ptmSummary) {
                                                try {
                                                    Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getConfidentPtmSites(proteinMatch, currentProtein.getSequence()) + SEPARATOR);
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getAmbiguousPtmSites(proteinMatch, currentProtein.getSequence()) + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }

                                            if (nPeptides) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    peptideShakerGUI.catchException(e);
                                                    writer.write(Double.NaN + SEPARATOR);
                                                }
                                            }
                                            if (nSpectra) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    peptideShakerGUI.catchException(e);
                                                    writer.write(Double.NaN + SEPARATOR);
                                                }
                                            }
                                            if (emPAI) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey,
                                                            SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }
                                            if (nsaf) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey,
                                                            SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }
                                            if (mw) {
                                                Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                                                writer.write(proteinMW + SEPARATOR);
                                            }
                                            if (score) {
                                                writer.write(proteinPSParameter.getProteinScore() + SEPARATOR);
                                            }
                                            if (confidence) {
                                                writer.write(proteinPSParameter.getProteinConfidence() + SEPARATOR);
                                            }
                                            if (!onlyValidated) {
                                                MatchValidationLevel matchValidationLevel = proteinPSParameter.getMatchValidationLevel();
                                                writer.write(matchValidationLevel.toString());
                                                writer.write(SEPARATOR);
                                            }
                                            if (includeHidden) {
                                                writer.write(proteinPSParameter.isHidden() + SEPARATOR);
                                            }
                                            if (!onlyStarred && showStar) {
                                                writer.write(proteinPSParameter.isStarred() + "");
                                            }
                                            writer.write(lineBreak);
                                        }
                                    }
                                }
                            }

                            progressDialog.increasePrimaryProgressCounter();
                        }

                        writer.close();

                        // print the maximal protein set to file
                        if (createMaximalProteinSet) {
                            writer = new BufferedWriter(new FileWriter(new File(selectedFile.getParentFile(), "maximal_protein_set.txt")));
                            for (String tempAccession : maximalProteinSet) {
                                writer.write(tempAccession + "\n");
                            }
                            writer.close();
                        }

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired peptide output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param parentDialog the parent dialog, can be null.
     * @param aPeptideKeys the peptide keys
     * @param aPeptidePdbArray the peptide PDB array
     * @param aIndexes boolean indicating whether the indexes shall be output
     * @param aOnlyValidated boolean indicating whether only validated are to to
     * exported
     * @param aAccession boolean indicating whether the protein accession is to
     * be exported
     * @param aProteinDescription boolean indicating whether the protein
     * descriptions is to be exported
     * @param aProteinInferenceType boolean indicating whether the protein
     * inference is to be exported
     * @param aLocation boolean indicating whether the location details is to be
     * exported
     * @param aSurroundings boolean indicating whether the peptide surrounding
     * details are to be exported
     * @param aSequence boolean indicating whether the peptide sequence is to be
     * exported
     * @param aModifications boolean indicating whether the peptide
     * modifications are to be exported
     * @param aPtmLocations boolean indicating whether the PTM localizations are
     * to be exported
     * @param aCharges boolean indicating whether the precursor charges is to be
     * exported
     * @param aNSpectra boolean indicating whether the number of spectra is to
     * be exported
     * @param aScore boolean indicating whether the peptide score is to be
     * exported
     * @param aConfidence boolean indicating whether the peptide confidence is
     * to be exported
     * @param aIncludeHeader boolean indicating whether the header should be
     * included
     * @param aOnlyStarred boolean indicating whether only starred peptide is to
     * be exported
     * @param aIncludeHidden boolean indicating whether the hidden peptides are
     * to be exported
     * @param aUniqueOnly boolean indicating whether the unique peptides only
     * are to be exported
     * @param aProteinKey the protein key
     * @param aEnzymatic boolean indicating whether the peptide enzymatic
     * details are to be exported
     */
    public void getPeptidesOutput(JDialog parentDialog, ArrayList<String> aPeptideKeys,
            ArrayList<String> aPeptidePdbArray, boolean aIndexes, boolean aOnlyValidated, boolean aAccession, boolean aProteinDescription,
            boolean aProteinInferenceType, boolean aLocation, boolean aSurroundings, boolean aSequence, boolean aModifications, boolean aPtmLocations, boolean aCharges,
            boolean aNSpectra, boolean aScore, boolean aConfidence, boolean aIncludeHeader, boolean aOnlyStarred,
            boolean aIncludeHidden, boolean aUniqueOnly, String aProteinKey, boolean aEnzymatic) {

        // create final versions of all variables use inside the export thread
        final ArrayList<String> peptideKeys;
        final ArrayList<String> peptidePdbArray = aPeptidePdbArray;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accession = aAccession;
        final boolean proteinDescription = aProteinDescription;
        final boolean proteinInferenceType = aProteinInferenceType;
        final boolean location = aLocation;
        final boolean surroundings = aSurroundings;
        final boolean sequence = aSequence;
        final boolean modifications = aModifications;
        final boolean ptmLocations = aPtmLocations;
        final boolean charges = aCharges;
        final boolean nSpectra = aNSpectra;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean includeHidden = aIncludeHidden;
        final boolean uniqueOnly = aUniqueOnly;
        final String proteinKey = aProteinKey;
        final boolean enzymatic = aEnzymatic;

        // get the file to send the output to
        File selectedFile = peptideShakerGUI.getUserSelectedFile("peptide_export.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            final String filePath = selectedFile.getPath();

            try {
                writer = new BufferedWriter(new FileWriter(selectedFile));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }

            if (aPeptideKeys == null) {
                peptideKeys = new ArrayList<String>(identification.getPeptideIdentification());
            } else {
                peptideKeys = aPeptideKeys;
            }

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {

                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (accession) {
                                writer.write("Protein" + SEPARATOR);
                                writer.write("Other Protein(s)" + SEPARATOR);
                                writer.write("Peptide Protein(s)" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein Description" + SEPARATOR);
                                writer.write("Other Protein Description(s)" + SEPARATOR);
                                writer.write("Peptide Proteins Description(s)" + SEPARATOR);
                            }
                            if (proteinInferenceType) {
                                writer.write("Protein Inference" + SEPARATOR);
                            }
                            if (surroundings) {
                                writer.write("AA Before" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                                writer.write("Sequence Tagged" + SEPARATOR);
                            }
                            if (surroundings) {
                                writer.write("AA After" + SEPARATOR);
                            }
                            if (enzymatic) {
                                writer.write("Enzymatic" + SEPARATOR);
                            }
                            if (location) {
                                writer.write("Peptide Start" + SEPARATOR);
                                writer.write("Peptide End" + SEPARATOR);
                            }
                            if (modifications) {
                                writer.write("Fixed Modification" + SEPARATOR);
                            }
                            if (modifications) {
                                writer.write("Variable Modification" + SEPARATOR);
                            }
                            if (ptmLocations) {
                                writer.write("Location Confidence" + SEPARATOR);
                            }
                            if (charges) {
                                writer.write("Precursor Charge(s)" + SEPARATOR);
                            }
                            if (nSpectra) {
                                writer.write("#Validated Spectra" + SEPARATOR);
                            }
                            if (peptidePdbArray != null) {
                                writer.write("PDB" + SEPARATOR);
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            if (!onlyValidated) {
                                writer.write("Validation" + SEPARATOR);
                                writer.write("Decoy" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            if (!onlyStarred) {
                                writer.write("Starred" + SEPARATOR);
                            }

                            writer.write(lineBreak);
                        }

                        PSParameter peptidePSParameter = new PSParameter();
                        PSParameter secondaryPSParameter = new PSParameter();
                        int peptideCounter = 0;
                        HashMap<String, HashMap<Integer, String[]>> surroundingAAs = new HashMap<String, HashMap<Integer, String[]>>();
                        ProteinMatch proteinMatch = null;
                        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
                        PtmSettings ptmProfile = identificationParameters.getSearchParameters().getPtmSettings();
                        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
                        ShotgunProtocol shotgunProtocol = peptideShakerGUI.getShotgunProtocol();

                        // @TODO: try to batch load the spectra? as this would speed up the export...
                        progressDialog.setTitle("Loading Peptide Matches. Please Wait...");
                        identification.loadPeptideMatches(progressDialog, true);
                        progressDialog.setTitle("Loading Peptide Details. Please Wait...");
                        identification.loadPeptideMatchParameters(peptidePSParameter, progressDialog, true);

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        progressDialog.setMaxPrimaryProgressCounter(peptideKeys.size());
                        progressDialog.setValue(0);
                        progressDialog.setTitle("Copying to File. Please Wait...");

                        for (String peptideKey : peptideKeys) {

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            boolean shared = false;
                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                            peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);

                            if (!peptideMatch.getTheoreticPeptide().isDecoy(sequenceMatchingPreferences) || !onlyValidated) {
                                if ((onlyValidated && peptidePSParameter.getMatchValidationLevel().isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !peptidePSParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && peptidePSParameter.isStarred()) || !onlyStarred) {

                                            Peptide peptide = peptideMatch.getTheoreticPeptide();
                                            ArrayList<String> possibleProteins = new ArrayList<String>();
                                            ArrayList<String> orderedProteinsKeys = new ArrayList<String>(); // @TODO: could be merged with one of the other maps perhaps?

                                            if (accession || proteinDescription || surroundings || location || uniqueOnly) {
                                                if (proteinKey == null) {
                                                    for (String parentProtein : peptide.getParentProteins(sequenceMatchingPreferences)) {
                                                        HashSet<String> tempParentProteins = identification.getProteinMap().get(parentProtein);
                                                        if (tempParentProteins != null) {
                                                            ArrayList<String> parentProteins = new ArrayList<String>(tempParentProteins);
                                                            for (String proteinKey : parentProteins) {
                                                                if (!possibleProteins.contains(proteinKey)) {
                                                                    try {
                                                                        proteinMatch = identification.getProteinMatch(proteinKey);
                                                                        if (proteinMatch.getPeptideMatchesKeys().contains(peptideKey)) {
                                                                            possibleProteins.add(proteinKey);
                                                                        }
                                                                    } catch (Exception e) {
                                                                        // protein deleted due to protein inference issue and not deleted from the map in versions earlier than 0.14.6
                                                                        System.out.println("Non-existing protein key in protein map: " + proteinKey);
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    shared = possibleProteins.size() > 1;
                                                    proteinMatch = identification.getProteinMatch(possibleProteins.get(0));
                                                } else {
                                                    proteinMatch = identification.getProteinMatch(proteinKey);
                                                }
                                            }

                                            if (shared && uniqueOnly) {
                                                // these will be ignored as the user requested unique only
                                            } else {

                                                if (indexes) {
                                                    writer.write(++peptideCounter + SEPARATOR);
                                                }

                                                if (accession || proteinDescription) {
                                                    String mainMatch, secondaryProteins = "", peptideProteins = "";
                                                    String mainMatchDescription, secondaryProteinsDescriptions = "", peptideProteinDescriptions = "";
                                                    ArrayList<String> accessions = new ArrayList<String>();

                                                    mainMatch = proteinMatch.getMainMatch();
                                                    mainMatchDescription = sequenceFactory.getHeader(mainMatch).getSimpleProteinDescription();
                                                    boolean first = true;

                                                    if (!shared) {
                                                        orderedProteinsKeys.add(mainMatch);
                                                    }

                                                    accessions.addAll(proteinMatch.getTheoreticProteinsAccessions());
                                                    Collections.sort(accessions);
                                                    for (String key : accessions) {
                                                        if (!key.equals(mainMatch)) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                secondaryProteins += "; ";
                                                                secondaryProteinsDescriptions += "; ";
                                                            }
                                                            secondaryProteins += key;
                                                            secondaryProteinsDescriptions += sequenceFactory.getHeader(key).getSimpleProteinDescription();
                                                            orderedProteinsKeys.add(key);
                                                        }
                                                    }

                                                    if (shared) {
                                                        mainMatch = "shared peptide";
                                                        mainMatchDescription = "shared peptide";
                                                    }

                                                    first = true;
                                                    ArrayList<String> peptideAccessions = new ArrayList<String>(peptide.getParentProteins(sequenceMatchingPreferences));
                                                    Collections.sort(peptideAccessions);
                                                    for (String key : peptideAccessions) {
                                                        if (shared || !accessions.contains(key)) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                peptideProteins += "; ";
                                                                peptideProteinDescriptions += "; ";
                                                            }
                                                            peptideProteins += key;
                                                            peptideProteinDescriptions += sequenceFactory.getHeader(key).getSimpleProteinDescription();
                                                            orderedProteinsKeys.add(key);
                                                        }
                                                    }

                                                    if (accession) {
                                                        writer.write(mainMatch + SEPARATOR);
                                                        writer.write(secondaryProteins + SEPARATOR);
                                                        writer.write(peptideProteins + SEPARATOR);
                                                    }
                                                    if (proteinDescription) {
                                                        writer.write(mainMatchDescription + SEPARATOR);
                                                        writer.write(secondaryProteinsDescriptions + SEPARATOR);
                                                        writer.write(peptideProteinDescriptions + SEPARATOR);
                                                    }
                                                }

                                                if (proteinInferenceType) {
                                                    writer.write(peptidePSParameter.getProteinInferenceClassAsString() + SEPARATOR);
                                                }

                                                if (location || surroundings) {
                                                    for (String proteinAccession : orderedProteinsKeys) {
                                                        surroundingAAs.put(proteinAccession,
                                                                sequenceFactory.getProtein(proteinAccession).getSurroundingAA(peptide.getSequence(),
                                                                        peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides(),
                                                                        sequenceMatchingPreferences));
                                                    }
                                                }

                                                if (surroundings) {

                                                    String subSequence = "";

                                                    for (String proteinAccession : orderedProteinsKeys) {
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.get(proteinAccession).keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int start : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                subSequence += ", ";
                                                            }
                                                            subSequence += surroundingAAs.get(proteinAccession).get(start)[0];
                                                        }

                                                        subSequence += "; ";
                                                    }

                                                    subSequence = subSequence.substring(0, subSequence.length() - 1);

                                                    writer.write(subSequence + SEPARATOR);
                                                }

                                                if (sequence) {
                                                    writer.write(peptide.getSequence() + SEPARATOR);
                                                    writer.write(peptide.getTaggedModifiedSequence(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings(),
                                                            false, false, true) + SEPARATOR);
                                                }

                                                if (surroundings) {

                                                    String subSequence = "";

                                                    for (String proteinAccession : orderedProteinsKeys) {
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.get(proteinAccession).keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int start : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                subSequence += ", ";
                                                            }
                                                            subSequence += surroundingAAs.get(proteinAccession).get(start)[1];
                                                        }
                                                        subSequence += "; ";
                                                    }

                                                    subSequence = subSequence.substring(0, subSequence.length() - 1);

                                                    writer.write(subSequence + SEPARATOR);
                                                }

                                                if (enzymatic) {
                                                    boolean isEnzymatic = sequenceFactory.getProtein(proteinMatch.getMainMatch()).isEnzymaticPeptide(peptide.getSequence(),
                                                            shotgunProtocol.getEnzyme(), sequenceMatchingPreferences);

                                                    writer.write(isEnzymatic + SEPARATOR);
                                                }

                                                if (location) {
                                                    String start = "";
                                                    String end = "";
                                                    for (String proteinAccession : orderedProteinsKeys) {
                                                        int endAA;
                                                        String sequence = peptide.getSequence();
                                                        ArrayList<Integer> starts = new ArrayList<Integer>(surroundingAAs.get(proteinAccession).keySet());
                                                        Collections.sort(starts);
                                                        boolean first = true;
                                                        for (int startAa : starts) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                start += ", ";
                                                                end += ", ";
                                                            }
                                                            start += startAa;
                                                            endAA = startAa + sequence.length();
                                                            end += endAA;
                                                        }

                                                        start += "; ";
                                                        end += "; ";
                                                    }

                                                    start = start.substring(0, start.length() - 2);
                                                    end = end.substring(0, end.length() - 2);

                                                    writer.write(start + SEPARATOR + end + SEPARATOR);
                                                }
                                                if (modifications) {
                                                    writer.write(getPeptideModificationsAsString(peptide, false));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (modifications) {
                                                    writer.write(getPeptideModificationsAsString(peptide, true));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (ptmLocations) {
                                                    writer.write(getPeptideModificationLocations(peptide, peptideMatch, ptmProfile));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (charges) {
                                                    writer.write(getPeptidePrecursorChargesAsString(peptideMatch));
                                                    writer.write(SEPARATOR);
                                                }
                                                if (nSpectra) {
                                                    int cpt = 0;

                                                    // @TODO: replace with: peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectraForPeptide(peptideKey);?
                                                    identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatchesKeys(), secondaryPSParameter, null, true);
                                                    for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                                                        secondaryPSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, secondaryPSParameter);
                                                        if (secondaryPSParameter.getMatchValidationLevel().isValidated()) {
                                                            cpt++;
                                                        }
                                                    }
                                                    writer.write(cpt + SEPARATOR);
                                                }
                                                if (peptidePdbArray != null) {
                                                    writer.write(peptidePdbArray.contains(peptideKey) + SEPARATOR);
                                                }
                                                if (score) {
                                                    writer.write(peptidePSParameter.getPeptideScore() + SEPARATOR);
                                                }
                                                if (confidence) {
                                                    writer.write(peptidePSParameter.getPeptideConfidence() + SEPARATOR);
                                                }
                                                if (!onlyValidated) {
                                                    MatchValidationLevel matchValidationLevel = peptidePSParameter.getMatchValidationLevel();
                                                    writer.write(matchValidationLevel.toString());
                                                    writer.write(SEPARATOR);
                                                    if (peptideMatch.getTheoreticPeptide().isDecoy(sequenceMatchingPreferences)) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                }
                                                if (includeHidden) {
                                                    writer.write(peptidePSParameter.isHidden() + SEPARATOR);
                                                }
                                                if (!onlyStarred) {
                                                    writer.write(peptidePSParameter.isStarred() + SEPARATOR);
                                                }
                                                writer.write(lineBreak);
                                            }
                                        }
                                    }
                                }
                            }
                            progressDialog.increasePrimaryProgressCounter();
                        }

                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();

                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired PSM output (based on the elements needed as provided in
     * arguments) to a user chosen file.
     *
     * @param parentDialog the parent dialog, can be null.
     * @param aPsmKeys the PSM keys
     * @param aIndexes boolean indicating whether the indexes shall be output
     * @param aOnlyValidated boolean indicating whether only validated are to to
     * exported
     * @param aAccessions boolean indicating whether the protein accessions
     * shall be output
     * @param aProteinDescription boolean indicating whether the protein
     * descriptions shall be output
     * @param aSequence boolean indicating whether the peptide sequence shall be
     * output
     * @param aModification boolean indicating whether the peptide modification
     * details shall be output
     * @param aLocation boolean indicating whether the modification location
     * details shall be output
     * @param aFile boolean indicating whether the spectrum file name shall be
     * output
     * @param aTitle boolean indicating whether the spectrum title shall be
     * output
     * @param aPrecursor boolean indicating whether the precursor details name
     * shall be output
     * @param aScore boolean indicating whether the PSM score shall be output
     * @param aConfidence boolean indicating whether the PSM confidence shall be
     * output
     * @param aIncludeHeader boolean indicating whether the column header shall
     * be output
     * @param aOnlyStarred boolean indicating whether only starred entries shall
     * be output
     * @param aIncludeHidden boolean indicating whether hidden entries shall be
     * output
     */
    public void getPSMsOutput(JDialog parentDialog, ArrayList<String> aPsmKeys, boolean aIndexes, boolean aOnlyValidated, boolean aAccessions, boolean aProteinDescription,
            boolean aSequence, boolean aModification, boolean aLocation, boolean aFile, boolean aTitle, boolean aPrecursor, boolean aScore, boolean aConfidence, boolean aIncludeHeader,
            boolean aOnlyStarred, boolean aIncludeHidden) {

        // create final versions of all variables to use inside the export thread
        final ArrayList<String> psmKeys;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accessions = aAccessions;
        final boolean proteinDescription = aProteinDescription;
        final boolean sequence = aSequence;
        final boolean modification = aModification;
        final boolean location = aLocation;
        final boolean file = aFile;
        final boolean title = aTitle;
        final boolean precursor = aPrecursor;
        final boolean score = aScore;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean includeHidden = aIncludeHidden;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile("psm_export.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            final String filePath = selectedFile.getPath();

            try {
                writer = new BufferedWriter(new FileWriter(selectedFile));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }

            psmKeys = aPsmKeys;

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {

                        PTMFactory ptmFactory = PTMFactory.getInstance();
                        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
                        PtmSettings ptmProfile = identificationParameters.getSearchParameters().getPtmSettings();
                        PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        if (psmKeys != null) {
                            progressDialog.setMaxPrimaryProgressCounter(psmKeys.size());
                        } else {
                            progressDialog.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize());
                        }

                        if (includeHeader) {
                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (accessions) {
                                writer.write("Protein(s)" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein(s) Descriptions" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                            }
                            if (modification) {
                                writer.write("Variable Modification(s)" + SEPARATOR);
                            }
                            if (location) {
                                writer.write("Location Confidence" + SEPARATOR);
                                if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                                    writer.write(ptmScoringPreferences.getSelectedProbabilisticScore().getName() + SEPARATOR);
                                }
                                writer.write("D-score" + SEPARATOR);
                            }
                            if (file) {
                                writer.write("Spectrum File" + SEPARATOR);
                            }
                            if (title) {
                                writer.write("Spectrum Title" + SEPARATOR);
                            }
                            if (precursor) {
                                writer.write("Precursor m/z" + SEPARATOR);
                                writer.write("Precursor Charge" + SEPARATOR);
                                writer.write("Idenitification Charge" + SEPARATOR);
                                writer.write("Precursor Retention Time" + SEPARATOR);
                                writer.write("Peptide Theoretical Mass" + SEPARATOR);

                                if (peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm()) {
                                    writer.write("Mass Error [ppm]" + SEPARATOR);
                                } else {
                                    writer.write("Mass Error [Da]" + SEPARATOR);
                                }
                                writer.write("Isotope number" + SEPARATOR);
                            }
                            if (score) {
                                writer.write("Score" + SEPARATOR);
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            writer.write("Validation" + SEPARATOR);
                            if (!onlyValidated) {
                                writer.write("Decoy" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }

                            writer.write(lineBreak);
                        }

                        PSParameter psParameter = new PSParameter();
                        int psmCounter = 0;

                        HashMap<String, HashSet<String>> spectrumKeys = new HashMap<String, HashSet<String>>();
                        if (psmKeys == null) {
                            spectrumKeys = identification.getSpectrumIdentificationMap();
                        } else {
                            for (String spectrumKey : psmKeys) {
                                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                                if (!spectrumKeys.containsKey(spectrumFile)) {
                                    spectrumKeys.put(spectrumFile, new HashSet<String>());
                                }
                                spectrumKeys.get(spectrumFile).add(spectrumKey);
                            }
                        }

                        int fileCounter = 0;

                        for (String spectrumFile : spectrumKeys.keySet()) {

                            if (psmKeys == null) {
                                progressDialog.setTitle("Copying Spectrum Matches to File. Please Wait... (" + ++fileCounter + "/" + spectrumKeys.size() + ")");
                                identification.loadSpectrumMatches(spectrumFile, progressDialog, true);
                                progressDialog.setTitle("Copying Spectrum Matches Details to File. Please Wait... (" + fileCounter + "/" + spectrumKeys.size() + ")");
                                identification.loadSpectrumMatchParameters(spectrumFile, psParameter, progressDialog, true);
                            } else {
                                progressDialog.setTitle("Copying Spectrum Matches to File. Please Wait... (" + ++fileCounter + "/" + spectrumKeys.size() + ")");
                                identification.loadSpectrumMatches(new ArrayList<String>(spectrumKeys.get(spectrumFile)), progressDialog, true);
                                progressDialog.setTitle("Copying Spectrum Matches Details to File. Please Wait... (" + fileCounter + "/" + spectrumKeys.size() + ")");
                                identification.loadSpectrumMatchParameters(new ArrayList<String>(spectrumKeys.get(spectrumFile)), psParameter, progressDialog, true);
                            }

                            progressDialog.setMaxPrimaryProgressCounter(spectrumKeys.get(spectrumFile).size());
                            progressDialog.setValue(0);
                            for (String psmKey : spectrumKeys.get(spectrumFile)) {

                                if (progressDialog.isRunCanceled()) {
                                    break;
                                }

                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, psParameter);
                                PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();

                                if (!bestAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences()) || !onlyValidated) {
                                    if ((onlyValidated && psParameter.getMatchValidationLevel().isValidated()) || !onlyValidated) {
                                        if ((!includeHidden && !psParameter.isHidden()) || includeHidden) {
                                            if ((onlyStarred && psParameter.isStarred()) || !onlyStarred) {

                                                if (indexes) {
                                                    writer.write(++psmCounter + SEPARATOR);
                                                }

                                                if (accessions || proteinDescription) {

                                                    String proteinAccessions = "";
                                                    String proteinDescriptions = "";

                                                    boolean first = true;
                                                    for (String protein : bestAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            if (accessions) {
                                                                proteinAccessions += ", ";
                                                            }
                                                            if (proteinDescription) {
                                                                proteinDescriptions += "; ";
                                                            }
                                                        }
                                                        if (accessions) {
                                                            proteinAccessions += protein;
                                                        }
                                                        if (proteinDescription) {
                                                            proteinDescriptions += sequenceFactory.getHeader(protein).getSimpleProteinDescription();
                                                        }
                                                    }
                                                    if (accessions) {
                                                        writer.write(proteinAccessions + SEPARATOR);
                                                    }
                                                    if (proteinDescription) {
                                                        writer.write(proteinDescriptions + SEPARATOR);
                                                    }
                                                }
                                                if (sequence) {
                                                    writer.write(bestAssumption.getPeptide().getSequence() + SEPARATOR);
                                                }
                                                if (modification) {
                                                    HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();
                                                    Peptide peptide = bestAssumption.getPeptide();
                                                    if (peptide.isModified()) {
                                                        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                                            if (modificationMatch.isVariable()) {
                                                                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                                                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                                                                }
                                                                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                                                            }
                                                        }
                                                    }
                                                    boolean first = true, first2;
                                                    ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
                                                    Collections.sort(mods);
                                                    for (String mod : mods) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        first2 = true;
                                                        writer.write(mod + "(");
                                                        for (int aa : modMap.get(mod)) {
                                                            if (first2) {
                                                                first2 = false;
                                                            } else {
                                                                writer.write(", ");
                                                            }
                                                            writer.write(aa + "");
                                                        }
                                                        writer.write(")");
                                                    }
                                                    writer.write(SEPARATOR);
                                                }
                                                if (location) {
                                                    ArrayList<String> modList = new ArrayList<String>();
                                                    Peptide peptide = bestAssumption.getPeptide();
                                                    if (peptide.isModified()) {
                                                        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                                            if (modificationMatch.isVariable()) {
                                                                PTM refPtm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                                                                for (String equivalentPtm : ptmProfile.getSimilarNotFixedModifications(refPtm.getMass())) {
                                                                    if (!modList.contains(equivalentPtm)) {
                                                                        modList.add(equivalentPtm);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    Collections.sort(modList);
                                                    PSPtmScores ptmScores = new PSPtmScores();
                                                    boolean first = true;
                                                    for (String mod : modList) {
                                                        if (spectrumMatch.getUrParam(ptmScores) != null) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                writer.write(", ");
                                                            }
                                                            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                                            writer.write(mod + " (");
                                                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                                                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                                                boolean firstSite = true;
                                                                for (int site : ptmScoring.getOrderedPtmLocations()) {
                                                                    if (firstSite) {
                                                                        firstSite = false;
                                                                    } else {
                                                                        writer.write(", ");
                                                                    }
                                                                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                                                                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                                                                        writer.write(site + ": Not Scored"); // Well this should not happen
                                                                    } else if (ptmConfidence == PtmScoring.RANDOM) {
                                                                        writer.write(site + ": Random");
                                                                    } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                                                                        writer.write(site + ": Doubtfull");
                                                                    } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                                                                        writer.write(site + ": Confident");
                                                                    } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                                                                        writer.write(site + ": Very Confident");
                                                                    }
                                                                }
                                                            } else {
                                                                writer.write("Not Scored");
                                                            }
                                                            writer.write(")");
                                                        }
                                                    }
                                                    writer.write(SEPARATOR);
                                                    if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                                                        first = true;
                                                        for (String mod : modList) {
                                                            if (spectrumMatch.getUrParam(ptmScores) != null) {
                                                                if (first) {
                                                                    first = false;
                                                                } else {
                                                                    writer.write(", ");
                                                                }
                                                                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                                                writer.write(mod + " (");
                                                                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                                                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                                                    boolean firstSite = true;
                                                                    ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                                                                    Collections.sort(sites);
                                                                    for (int site : sites) {
                                                                        if (firstSite) {
                                                                            firstSite = false;
                                                                        } else {
                                                                            writer.write(", ");
                                                                        }
                                                                        writer.write(site + ": " + ptmScoring.getProbabilisticScore(site));
                                                                    }
                                                                } else {
                                                                    writer.write("Not Scored");
                                                                }
                                                                writer.write(")");
                                                            }
                                                        }
                                                    }
                                                    writer.write(SEPARATOR);
                                                    first = true;
                                                    for (String mod : modList) {
                                                        if (spectrumMatch.getUrParam(ptmScores) != null) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                writer.write(", ");
                                                            }
                                                            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                                            writer.write(mod + " (");
                                                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                                                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                                                boolean firstSite = true;
                                                                ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                                                                Collections.sort(sites);
                                                                for (int site : sites) {
                                                                    if (firstSite) {
                                                                        firstSite = false;
                                                                    } else {
                                                                        writer.write(", ");
                                                                    }
                                                                    writer.write(site + ": " + ptmScoring.getDeltaScore(site));
                                                                }
                                                            } else {
                                                                writer.write("Not Scored");
                                                            }
                                                            writer.write(")");
                                                        }
                                                    }
                                                    writer.write(SEPARATOR);
                                                }
                                                if (file) {
                                                    writer.write(spectrumFile + SEPARATOR);
                                                }
                                                if (title) {
                                                    writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);
                                                }
                                                if (precursor) {
                                                    Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                                    SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                                                    writer.write(prec.getMz() + SEPARATOR);
                                                    writer.write(prec.getPossibleChargesAsString() + SEPARATOR);
                                                    writer.write(bestAssumption.getIdentificationCharge().value + SEPARATOR);
                                                    writer.write(prec.getRt() + SEPARATOR);
                                                    writer.write(bestAssumption.getPeptide().getMass() + SEPARATOR);
                                                    writer.write(bestAssumption.getDeltaMass(prec.getMz(), searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                                    writer.write(bestAssumption.getIsotopeNumber(prec.getMz(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                                }
                                                if (score) {
                                                    writer.write(psParameter.getPsmScore() + SEPARATOR);
                                                }
                                                if (confidence) {
                                                    writer.write(psParameter.getPsmConfidence() + SEPARATOR);
                                                }
                                                if (!onlyValidated) {
                                                    MatchValidationLevel matchValidationLevel = psParameter.getMatchValidationLevel();
                                                    writer.write(matchValidationLevel.toString());
                                                    writer.write(SEPARATOR);

                                                    if (bestAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                }
                                                if (includeHidden) {
                                                    writer.write(psParameter.isHidden() + SEPARATOR);
                                                }
                                                writer.write(lineBreak);
                                            }

                                        }
                                    }
                                }
                                progressDialog.increasePrimaryProgressCounter();
                            }
                        }
                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired phosphorylation output to a user chosen file.
     *
     * @param parentDialog the parent dialog, can be null
     */
    public void getPhosphoOutput(JDialog parentDialog) {

        final JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Result File");
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "(Two tab separated text files) *.txt";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showSaveDialog(parentDialog);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            final File selectedFile = fileChooser.getSelectedFile();

            final String filePath = selectedFile.getPath();

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {
                        String reducedName = selectedFile.getName();
                        if (reducedName.endsWith(".txt")) {
                            reducedName = reducedName.substring(0, reducedName.length() - 4);
                        }
                        try {
                            File outputFile = new File(selectedFile.getParent(), reducedName + "_PSMs_phospho.txt");
                            writer = new BufferedWriter(new FileWriter(outputFile));
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                            return;
                        }

                        PTMFactory ptmFactory = PTMFactory.getInstance();
                        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
                        PtmSettings ptmProfile = identificationParameters.getSearchParameters().getPtmSettings();
                        PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();

                        ProjectDetails projectDetails = peptideShakerGUI.getProjectDetails();
                        ArrayList<Integer> algorithms = projectDetails.getIdentificationAlgorithms();

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        progressDialog.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize());

                        writer.write("Index" + SEPARATOR);
                        writer.write("Protein(s)" + SEPARATOR);
                        writer.write("Protein(s) Descriptions" + SEPARATOR);
                        writer.write("Sequence" + SEPARATOR);
                        writer.write("Modification(s)" + SEPARATOR);
                        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                            writer.write(ptmScoringPreferences.getSelectedProbabilisticScore().getName() + " localization" + SEPARATOR);
                        }
                        writer.write("D-score localization" + SEPARATOR);
                        if (algorithms.size() == 1 && algorithms.get(0) == Advocate.mascot.getIndex()) {
                            writer.write("MD-score localization" + SEPARATOR);
                            writer.write("MD-score" + SEPARATOR);
                        }
                        writer.write("# phosphorylations" + SEPARATOR);
                        writer.write("# phosphorylation sites" + SEPARATOR);
                        writer.write("Spectrum File" + SEPARATOR);
                        writer.write("Spectrum Title" + SEPARATOR);
                        writer.write("Precursor m/z" + SEPARATOR);
                        writer.write("Precursor Charge" + SEPARATOR);
                        writer.write("Precursor Retention Time" + SEPARATOR);
                        writer.write("Peptide Theoretical Mass" + SEPARATOR);
                        if (peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm()) {
                            writer.write("Mass Error [ppm]" + SEPARATOR);
                        } else {
                            writer.write("Mass Error [Da]" + SEPARATOR);
                        }
                        writer.write("Isotope" + SEPARATOR);
                        writer.write("Confidence" + SEPARATOR);
                        writer.write("Validation" + SEPARATOR);
                        writer.write("Decoy" + SEPARATOR);

                        writer.write(lineBreak);

                        PSParameter psParameter = new PSParameter();
                        int psmCounter = 0;

                        HashMap<String, HashSet<String>> spectrumKeys = identification.getSpectrumIdentificationMap();

                        int fileCounter = 0;

                        for (String spectrumFile : spectrumKeys.keySet()) {

                            progressDialog.setTitle("Loading Spectrum Matches. Please Wait... (" + ++fileCounter + "/" + spectrumKeys.size() + ")");
                            identification.loadSpectrumMatches(spectrumFile, progressDialog, true);
                            progressDialog.setTitle("Loading Spectrum Matches Details. Please Wait... (" + fileCounter + "/" + spectrumKeys.size() + ")");
                            identification.loadSpectrumMatchParameters(spectrumFile, psParameter, progressDialog, true);
                            progressDialog.setTitle("Copying Spectrum Matches Phospho Details to File. Please Wait... (" + fileCounter + "/" + spectrumKeys.size() + ")");
                            progressDialog.setMaxPrimaryProgressCounter(spectrumKeys.get(spectrumFile).size());
                            progressDialog.setValue(0);

                            for (String psmKey : spectrumKeys.get(spectrumFile)) {

                                if (progressDialog.isRunCanceled()) {
                                    break;
                                }

                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, psParameter);
                                PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();

                                writer.write(++psmCounter + SEPARATOR);

                                String proteinAccessions = "";
                                String proteinDescriptions = "";

                                for (String protein : bestAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {
                                    if (!proteinAccessions.equals("")) {
                                        proteinAccessions += ", ";
                                        proteinDescriptions += "; ";
                                    }
                                    proteinAccessions += protein;
                                    proteinDescriptions += sequenceFactory.getHeader(protein).getSimpleProteinDescription();
                                }
                                writer.write(proteinAccessions + SEPARATOR);
                                writer.write(proteinDescriptions + SEPARATOR);
                                String sequence = bestAssumption.getPeptide().getSequence();
                                writer.write(sequence + SEPARATOR);
                                Peptide peptide = bestAssumption.getPeptide();
                                HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>(peptide.getNModifications());
                                if (peptide.isModified()) {
                                    for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                        if (modificationMatch.isVariable()) {
                                            if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                                modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                                            }
                                            modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                                        }
                                    }
                                }
                                boolean first = true, first2;
                                ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
                                Collections.sort(mods);
                                for (String mod : mods) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    first2 = true;
                                    writer.write(mod + "(");
                                    for (int aa : modMap.get(mod)) {
                                        if (first2) {
                                            first2 = false;
                                        } else {
                                            writer.write(", ");
                                        }
                                        writer.write(aa + "");
                                    }
                                    writer.write(")");
                                }
                                writer.write(SEPARATOR);
                                int nPhospho = 0;
                                ArrayList<String> modList = new ArrayList<String>();
                                peptide = bestAssumption.getPeptide();
                                if (peptide.isModified()) {
                                    for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                        if (modificationMatch.isVariable()) {
                                            String ptmName = modificationMatch.getTheoreticPtm();
                                            if (ptmName.contains("phospho")) {
                                                nPhospho++;
                                            }
                                            PTM refPtm = ptmFactory.getPTM(ptmName);
                                            for (String equivalentPtm : ptmProfile.getSimilarNotFixedModifications(refPtm.getMass())) {
                                                if (!modList.contains(equivalentPtm)) {
                                                    modList.add(equivalentPtm);
                                                }
                                            }
                                        }
                                    }
                                }

                                Collections.sort(modList);
                                PSPtmScores ptmScores = new PSPtmScores();
                                first = true;
                                StringBuilder dLocalizations = new StringBuilder();
                                StringBuilder probabilisticLocalizations = new StringBuilder();
                                String mdLocation = "";
                                String mdScore = "";
                                String[] split = sequence.split("[STY]");
                                int nSites = split.length - 1;
                                ArrayList<String> phosphoNames = new ArrayList<String>();

                                for (String mod : modList) {
                                    if (mod.contains("phospho")) {
                                        phosphoNames.add(mod);
                                        if (spectrumMatch.getUrParam(ptmScores) != null) {
                                            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                                ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                                                Collections.sort(sites);
                                                for (Integer site : sites) {
                                                    if (probabilisticLocalizations.length() > 0) {
                                                        probabilisticLocalizations.append(", ");
                                                    }
                                                    probabilisticLocalizations.append(site.toString()).append(": ").append(ptmScoring.getProbabilisticScore(site));
                                                }
                                                sites = new ArrayList<Integer>(ptmScoring.getDSites());
                                                Collections.sort(sites);
                                                for (Integer site : sites) {
                                                    if (dLocalizations.length() > 0) {
                                                        dLocalizations.append(", ");
                                                    }
                                                    dLocalizations.append(site.toString()).append(": ").append(ptmScoring.getDeltaScore(site));
                                                }
                                            }
                                        }
                                    }
                                }
                                if (algorithms.size() == 1 && algorithms.get(0) == Advocate.mascot.getIndex()) {
                                    if (!phosphoNames.isEmpty()) {
                                        PeptideAssumption mascotAssumption = null;
                                        double bestScore = 0;
                                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(psmKey);
                                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> mascotMap = assumptions.get(Advocate.mascot.getIndex());
                                        if (mascotMap != null) {
                                            ArrayList<SpectrumIdentificationAssumption> mascotAssumptions = new ArrayList<SpectrumIdentificationAssumption>();
                                            for (ArrayList<SpectrumIdentificationAssumption> peptideAssumptionList : mascotMap.values()) {
                                                mascotAssumptions.addAll(peptideAssumptionList);
                                                for (SpectrumIdentificationAssumption assumption : peptideAssumptionList) {
                                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                                    double mascotScore = peptideAssumption.getRawScore();
                                                    if (mascotScore > bestScore) {
                                                        mascotAssumption = peptideAssumption;
                                                        bestScore = mascotScore;
                                                    }
                                                }
                                            }
                                            if (mascotAssumption != null) {
                                                Peptide mascotPeptide = mascotAssumption.getPeptide();
                                                Double score = MDScore.getMDScore(mascotAssumptions, mascotPeptide, phosphoNames, identificationParameters.getSequenceMatchingPreferences(), 2);
                                                if (score != null) {
                                                    mdScore = score.toString();
                                                }
                                                ArrayList<Integer> sites = new ArrayList<Integer>();
                                                if (mascotPeptide.isModified()) {
                                                    for (ModificationMatch modificationMatch : mascotPeptide.getModificationMatches()) {
                                                        if (modificationMatch.getTheoreticPtm().contains("phospho")) {
                                                            sites.add(modificationMatch.getModificationSite());
                                                        }
                                                    }
                                                }
                                                Collections.sort(sites);
                                                for (int site : sites) {
                                                    if (!mdLocation.equals("")) {
                                                        mdLocation += ", ";
                                                    }
                                                    mdLocation += site;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                                    writer.write(probabilisticLocalizations + SEPARATOR);
                                }
                                writer.write(dLocalizations + SEPARATOR);
                                if (algorithms.size() == 1 && algorithms.get(0) == Advocate.mascot.getIndex()) {
                                    writer.write(mdLocation + SEPARATOR);
                                    writer.write(mdScore + SEPARATOR);
                                }
                                writer.write(nPhospho + SEPARATOR);
                                writer.write(nSites + SEPARATOR);
                                writer.write(spectrumFile + SEPARATOR);
                                writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);
                                Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                writer.write(prec.getMz() + SEPARATOR);
                                writer.write(bestAssumption.getIdentificationCharge().value + SEPARATOR);
                                writer.write(prec.getRt() + SEPARATOR);
                                writer.write(bestAssumption.getPeptide().getMass() + SEPARATOR);
                                SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                                writer.write(bestAssumption.getDeltaMass(prec.getMz(), peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                writer.write(bestAssumption.getIsotopeNumber(prec.getMz(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                writer.write(psParameter.getPsmConfidence() + SEPARATOR);
                                MatchValidationLevel matchValidationLevel = psParameter.getMatchValidationLevel();
                                writer.write(matchValidationLevel.toString());
                                writer.write(SEPARATOR);
                                if (bestAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
                                    writer.write(1 + SEPARATOR);
                                } else {
                                    writer.write(0 + SEPARATOR);
                                }
                                writer.write(lineBreak);
                                progressDialog.increasePrimaryProgressCounter();
                            }
                        }
                        writer.close();
                        try {
                            File outputFile = new File(selectedFile.getParent(), reducedName + "_Proteins_phospho.txt");
                            writer = new BufferedWriter(new FileWriter(outputFile));
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "An error occurred when saving the protein details.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                            return;
                        }

                        ArrayList<String> targetedPtms = new ArrayList<String>();
                        for (String ptm : ptmProfile.getAllNotFixedModifications()) {
                            if (ptm.contains("phospho")) {
                                targetedPtms.add(ptm);
                            }
                        }

                        writer.write(SEPARATOR);
                        writer.write("Accession" + SEPARATOR);
                        writer.write("Other Protein(s)" + SEPARATOR);
                        writer.write("Protein Inference Class" + SEPARATOR);
                        writer.write("Description" + SEPARATOR);
                        writer.write("Sequence Coverage (%)" + SEPARATOR);
                        writer.write("Observable Coverage (%)" + SEPARATOR);
                        writer.write("Confident Phosphorylation Sites" + SEPARATOR);
                        writer.write("# Confident" + SEPARATOR);
                        writer.write("Other Phosphorylation Sites" + SEPARATOR);
                        writer.write("# Other" + SEPARATOR);
                        writer.write("#Validated Peptides" + SEPARATOR);
                        writer.write("#Validated Spectra" + SEPARATOR);
                        writer.write("NSAF" + SEPARATOR);
                        writer.write("MW (kDa)" + SEPARATOR);
                        writer.write("Confidence" + SEPARATOR);
                        writer.write("Validation" + SEPARATOR);
                        writer.write("Decoy" + SEPARATOR);
                        writer.write(lineBreak);

                        PSParameter proteinPSParameter = new PSParameter();
                        int proteinCounter = 0;

                        progressDialog.setTitle("Loading Protein Matches. Please Wait...");
                        identification.loadProteinMatches(progressDialog, true);
                        progressDialog.setTitle("Loading Protein Details. Please Wait...");
                        identification.loadProteinMatchParameters(proteinPSParameter, progressDialog, true);

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        progressDialog.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size());
                        progressDialog.setValue(0);
                        progressDialog.setTitle("Copying Protein Phospho Details to File. Please Wait...");

                        for (String proteinKey : identification.getProteinIdentification()) {

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinPSParameter);

                            writer.write(++proteinCounter + SEPARATOR);

                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                            String accession = proteinMatch.getMainMatch();
                            writer.write(accession + SEPARATOR);
                            boolean first = true;
                            for (String otherProtein : proteinMatch.getTheoreticProteinsAccessions()) {
                                if (!otherProtein.equals(accession)) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        writer.write(", ");
                                    }
                                    writer.write(otherProtein);
                                }
                            }
                            writer.write(SEPARATOR);
                            writer.write(proteinPSParameter.getProteinInferenceClassAsString() + SEPARATOR);
                            try {
                                writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription() + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }

                            try {
                                HashMap<Integer, Double> sequenceCoverage;
                                try {
                                    sequenceCoverage = peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                                } catch (Exception e) {
                                    peptideShakerGUI.catchException(e);
                                    sequenceCoverage = new HashMap<Integer, Double>();
                                }
                                Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                                Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                                Double validatedCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful;
                                writer.write(validatedCoverage + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey) * 100 + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getConfidentPtmSites(proteinMatch, currentProtein.getSequence(), targetedPtms) + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getAmbiguousPtmSites(proteinMatch, currentProtein.getSequence(), targetedPtms) + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey) + SEPARATOR);
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                                writer.write(Double.NaN + SEPARATOR);
                            }
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey) + SEPARATOR);
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                                writer.write(Double.NaN + SEPARATOR);
                            }
                            try {
                                writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey,
                                        SpectrumCountingPreferences.SpectralCountingMethod.NSAF) + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            try {
                                Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                                writer.write(proteinMW + SEPARATOR);
                            } catch (Exception e) {
                                writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                            }
                            writer.write(proteinPSParameter.getProteinConfidence() + SEPARATOR);
                            MatchValidationLevel matchValidationLevel = proteinPSParameter.getMatchValidationLevel();
                            writer.write(matchValidationLevel.toString());
                            writer.write(SEPARATOR);
                            if (proteinMatch.isDecoy()) {
                                writer.write(1 + SEPARATOR);
                            } else {
                                writer.write(0 + SEPARATOR);
                            }
                            writer.newLine();
                            progressDialog.increasePrimaryProgressCounter();
                        }

                        writer.close();
//
//                        try {
//                            writer = new BufferedWriter(new FileWriter(new File(selectedFile.getParent(), "reduced.mgf")));
//                        } catch (IOException e) {
//                            JOptionPane.showMessageDialog(null, "An error occurred when saving the mgf file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
//                            e.printStackTrace();
//                            return;
//                        }
//
//                        ArrayList<String> taken = new ArrayList<String>();
//
//                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
//                        progressDialog.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size() + 2*identification.getSpectrumIdentificationSize());
//                        progressDialog.setValue(0);
//                        progressDialog.setTitle("Copying Protein Phospho Details to File. Please Wait...");
//                        for (String peptideKey : identification.getPeptideIdentification()) {
//                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
//                            String title = null;
//                            double tempPEP, pep = 1;
//                            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
//                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
//                                tempPEP = psParameter.getPsmProbabilityScore();
//                                if (tempPEP < pep) {
//                                    pep = tempPEP;
//                                    title = spectrumKey;
//                                }
//                                taken.add(title);
//                            }
//                            if (title != null) {
//                                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(title);
//                                writer.write(spectrum.asMgf());
//                            }
//                            progressDialog.increasePrimaryProgressCounter();
//                        }
//                        int ratio = 10;
//                        for (String mgfFile : spectrumFactory.getMgfFileNames()) {
//                            int cpt = 0;
//                            for (String title : spectrumFactory.getSpectrumTitles(mgfFile)) {
//                                if (!taken.contains(title)) {
//                                    if (cpt % ratio == 0) {
//                                        MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(mgfFile, title);
//                                        writer.write(spectrum.asMgf());
//                                    }
//                                    cpt++;
//                                }
//                            progressDialog.increasePrimaryProgressCounter();
//                            }
//                        }
//
//                        writer.close();
//
//
//                        writer = new BufferedWriter(new FileWriter(new File(selectedFile.getParent(), "reduced.fasta")));
//                        taken = new ArrayList<String>();
//                        for (String spectrumFile : identification.getOrderedSpectrumFileNames()) {
//                            for (String spectrumTitle : identification.getSpectrumIdentification(spectrumFile)) {
//                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumTitle);
//                                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
//                                    for (String accession : peptideAssumption.getPeptide().getParentProteins()) {
//                                        if (!taken.contains(accession)) {
//                                        writer.write(sequenceFactory.getHeader(accession).toString() + lineBreak);
//                                        writer.write(sequenceFactory.getProtein(accession).getSequence() + lineBreak);
//                                        taken.add(accession);
//                                        }
//                                    }
//                                }
//                            progressDialog.increasePrimaryProgressCounter();
//                            }
//                        }
//
//                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired assumption output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param parentDialog the parent dialog, can be null.
     * @param aPsmKeys the PSM keys
     * @param aOnlyValidated boolean indicating whether only validated shall be
     * output
     * @param aAccession boolean indicating whether protein accessions shall be
     * output
     * @param aProteinDescription boolean indicating whether protein
     * descriptions shall be output
     * @param aSequence boolean indicating whether the peptide sequence shall be
     * output
     * @param aModifications boolean indicating whether the modification details
     * shall be output
     * @param aFile boolean indicating whether the spectrum file name shall be
     * output
     * @param aTitle boolean indicating whether the spectrum title shall be
     * output
     * @param aPrecursor boolean indicating whether the precursor details shall
     * be output
     * @param aScores boolean indicating whether the assumption scores shall be
     * output
     * @param aConfidence boolean indicating whether the assumption confidences
     * shall be output
     * @param aIncludeHeader boolean indicating whether the column headers shall
     * be output
     */
    public void getAssumptionsOutput(JDialog parentDialog, ArrayList<String> aPsmKeys, boolean aOnlyValidated,
            boolean aAccession, boolean aProteinDescription, boolean aSequence, boolean aModifications,
            boolean aFile, boolean aTitle, boolean aPrecursor, boolean aScores, boolean aConfidence, boolean aIncludeHeader) {

        // create final versions of all variables use inside the export thread
        final ArrayList<String> psmKeys = aPsmKeys;
        final boolean onlyValidated = aOnlyValidated;
        final boolean accession = aAccession;
        final boolean proteinDescription = aProteinDescription;
        final boolean sequence = aSequence;
        final boolean modifications = aModifications;
        final boolean file = aFile;
        final boolean title = aTitle;
        final boolean precursor = aPrecursor;
        final boolean scores = aScores;
        final boolean confidence = aConfidence;
        final boolean includeHeader = aIncludeHeader;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile("assumptions_export.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            final String filePath = selectedFile.getPath();

            try {
                writer = new BufferedWriter(new FileWriter(selectedFile));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Copying to File. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {
                        if (accession) {
                            progressDialog.setPrimaryProgressCounterIndeterminate(false);
                            progressDialog.setWaitingText("Importing Peptide to Protein Mapping. Please Wait...");

                            // make sure that the tree is available for exporting secondary hit protein mappings
                            SequenceFactory.getInstance().getDefaultProteinTree();
                            progressDialog.resetPrimaryProgressCounter();
                            progressDialog.setPrimaryProgressCounterIndeterminate(true);
                            progressDialog.setTitle("Copying to File. Please Wait...");
                        }

                        if (includeHeader) {
                            writer.write("Search Engine" + SEPARATOR);
                            writer.write("Rank" + SEPARATOR);
                            if (accession) {
                                writer.write("Protein Accession" + SEPARATOR);
                            }
                            if (proteinDescription) {
                                writer.write("Protein Description" + SEPARATOR);
                            }
                            if (sequence) {
                                writer.write("Sequence" + SEPARATOR);
                            }
                            if (modifications) {
                                writer.write("Variable Modifications" + SEPARATOR);
                            }
                            if (file) {
                                writer.write("Spectrum File" + SEPARATOR);
                            }
                            if (title) {
                                writer.write("Spectrum Title" + SEPARATOR);
                            }
                            if (precursor) {
                                writer.write("Precursor m/z" + SEPARATOR);
                                writer.write("Precursor Charge" + SEPARATOR);
                                writer.write("Precursor RT" + SEPARATOR);
                                writer.write("Peptide Theoretical Mass" + SEPARATOR);

                                if (peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm()) {
                                    writer.write("Mass Error [ppm]" + SEPARATOR);
                                } else {
                                    writer.write("Mass Error [Da]" + SEPARATOR);
                                }

                                writer.write("Isotope number" + SEPARATOR);
                            }
                            if (scores) {
                                for (Integer advocateIndex : peptideShakerGUI.getProjectDetails().getIdentificationAlgorithms()) {
                                    writer.write(Advocate.getAdvocate(advocateIndex) + " e-value" + SEPARATOR);
                                }
                            }
                            if (confidence) {
                                writer.write("Confidence" + SEPARATOR);
                            }
                            writer.write("Retained as Main PSM" + SEPARATOR);
                            writer.write("Decoy" + SEPARATOR);
                            writer.write(lineBreak);
                        }

                        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();

                        PSParameter psParameter = new PSParameter();
                        int rank, progress = 0;

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        if (psmKeys != null) {
                            progressDialog.setMaxPrimaryProgressCounter(psmKeys.size());
                        } else {
                            progressDialog.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize());
                        }
                        HashMap<String, HashSet<String>> spectrumKeys = new HashMap<String, HashSet<String>>();
                        if (psmKeys == null) {
                            spectrumKeys = identification.getSpectrumIdentificationMap();
                        } else {
                            for (String spectrumKey : psmKeys) {
                                String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
                                if (!spectrumKeys.containsKey(spectrumFile)) {
                                    spectrumKeys.put(spectrumFile, new HashSet<String>());
                                }
                                spectrumKeys.get(spectrumFile).add(spectrumKey);
                            }
                        }
                        for (String spectrumFile : spectrumKeys.keySet()) {
                            if (psmKeys == null) {
                                identification.loadSpectrumMatches(spectrumFile, progressDialog, true);
                                identification.loadSpectrumMatchParameters(spectrumFile, psParameter, progressDialog, true);
                            } else {
                                identification.loadSpectrumMatches(new ArrayList<String>(spectrumKeys.get(spectrumFile)), progressDialog, true);
                                identification.loadSpectrumMatchParameters(new ArrayList<String>(spectrumKeys.get(spectrumFile)), psParameter, progressDialog, true);
                            }
                            for (String spectrumKey : spectrumKeys.get(spectrumFile)) {

                                if (progressDialog.isRunCanceled()) {
                                    break;
                                }

                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                                if (!onlyValidated || psParameter.getMatchValidationLevel().isValidated()) {
                                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                                    for (int se : assumptions.keySet()) {
                                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> seMap = assumptions.get(se);
                                        ArrayList<Double> eValues = new ArrayList<Double>(seMap.keySet());
                                        Collections.sort(eValues);
                                        rank = 1;
                                        for (double eValue : eValues) {
                                            for (SpectrumIdentificationAssumption assumption : seMap.get(eValue)) {
                                                if (assumption instanceof PeptideAssumption) {
                                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                                    writer.write(Advocate.getAdvocate(se).getName() + SEPARATOR);
                                                    writer.write(rank + SEPARATOR);
                                                    if (accession || proteinDescription) {

                                                        String proteinAccessions = "";
                                                        String proteinDescriptions = "";

                                                        boolean first = true;
                                                        for (String protein : peptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {
                                                            if (first) {
                                                                first = false;
                                                            } else {
                                                                if (accession) {
                                                                    proteinAccessions += ", ";
                                                                }
                                                                if (proteinDescription) {
                                                                    proteinDescriptions += "; ";
                                                                }
                                                            }
                                                            if (accession) {
                                                                proteinAccessions += protein;
                                                            }
                                                            if (proteinDescription) {
                                                                proteinDescriptions += sequenceFactory.getHeader(protein).getSimpleProteinDescription();
                                                            }
                                                        }
                                                        if (accession) {
                                                            writer.write(proteinAccessions + SEPARATOR);
                                                        }
                                                        if (proteinDescription) {
                                                            writer.write(proteinDescriptions + SEPARATOR);
                                                        }
                                                    }
                                                    if (sequence) {
                                                        writer.write(peptideAssumption.getPeptide().getSequence() + SEPARATOR);
                                                    }
                                                    if (modifications) {
                                                        boolean first = true;
                                                        Peptide peptide = peptideAssumption.getPeptide();
                                                        if (peptide.isModified()) {
                                                            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                                                if (modificationMatch.isVariable()) {
                                                                    if (first) {
                                                                        first = false;
                                                                    } else {
                                                                        writer.write(", ");
                                                                    }
                                                                    String modName = modificationMatch.getTheoreticPtm();
                                                                    writer.write(modName + "(" + modificationMatch.getModificationSite() + ")");
                                                                }
                                                            }
                                                        }
                                                        writer.write(SEPARATOR);
                                                    }
                                                    if (file) {
                                                        writer.write(Spectrum.getSpectrumFile(spectrumMatch.getKey()) + SEPARATOR);
                                                    }
                                                    if (title) {
                                                        writer.write(Spectrum.getSpectrumTitle(spectrumMatch.getKey()) + SEPARATOR);
                                                    }
                                                    if (precursor) {
                                                        Precursor prec = spectrumFactory.getPrecursor(spectrumMatch.getKey());
                                                        writer.write(prec.getMz() + SEPARATOR);
                                                        writer.write(peptideAssumption.getIdentificationCharge().value + SEPARATOR);
                                                        writer.write(prec.getRt() + SEPARATOR);
                                                        writer.write(peptideAssumption.getPeptide().getMass() + SEPARATOR);
                                                        SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                                                        writer.write(peptideAssumption.getDeltaMass(prec.getMz(),
                                                                peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                                        writer.write(peptideAssumption.getIsotopeNumber(prec.getMz(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                                    }
                                                    if (scores) {
                                                        for (Integer advocateIndex : peptideShakerGUI.getProjectDetails().getIdentificationAlgorithms()) {
                                                            if (se == advocateIndex) {
                                                                writer.write("" + eValue);
                                                            }
                                                            writer.write(SEPARATOR);
                                                        }
                                                    }
                                                    if (confidence) {
                                                        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                                                        writer.write(psParameter.getSearchEngineConfidence() + SEPARATOR);
                                                    }
                                                    if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(spectrumMatch.getBestPeptideAssumption().getPeptide(), identificationParameters.getSequenceMatchingPreferences())) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                    if (peptideAssumption.getPeptide().isDecoy(identificationParameters.getSequenceMatchingPreferences())) {
                                                        writer.write(1 + SEPARATOR);
                                                    } else {
                                                        writer.write(0 + SEPARATOR);
                                                    }
                                                    writer.write(lineBreak);
                                                    rank++;
                                                }
                                            }
                                        }
                                    }
                                }

                                progressDialog.setValue(++progress);
                            }
                        }

                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Sends the desired fraction output (based on the elements needed as
     * provided in arguments) to a user chosen file.
     *
     * @param aParentDialog the parent dialog, can be null.
     * @param aProteinKeys The list of protein keys to output. If null, the
     * identification list will be used
     * @param aIndexes boolean indicating whether indexes shall be output
     * @param aOnlyValidated boolean indicating whether only validated proteins
     * shall be output
     * @param aMainAccession boolean indicating whether the accessions shall be
     * output
     * @param aOtherAccessions boolean indicating whether the the additional
     * protein accession numbers should be included or not
     * @param aPiDetails boolean indicating whether protein inference details
     * shall be output
     * @param aDescription boolean indicating whether protein description of the
     * main match shall be output
     * @param aMW boolean indicating whether the molecular weight is to be
     * included in the output
     * @param aNPeptides boolean indicating whether the total number of
     * validated peptides for the protein shall be output
     * @param aNSpectra boolean indicating whether the total number of validated
     * spectra for the protein shall be output
     * @param aSequenceCoverage boolean indicating whether the sequence coverage
     * shall be output
     * @param aNPeptidesPerFraction boolean indicating whether the number of
     * validated
     * @param aNSpectraPerFraction boolean indicating whether the number of
     * spectra per fractions shall be output
     * @param aPrecursorIntensities boolean indicating whether the precursor
     * intensities shall be output
     * @param aFractionSpread boolean indicating whether a value representing
     * the spread of the fractions shall be output
     * @param aIncludeHeader boolean indicating whether the header shall be
     * output
     * @param aOnlyStarred boolean indicating whether only starred proteins
     * shall be output
     * @param aShowStar boolean indicating whether the starred proteins will be
     * indicated in a separate column
     * @param aIncludeHidden boolean indicating whether hidden hits shall be
     * output
     * @param aShowNonEnzymaticPeptidesColumn if true, a column indicating if
     * the protein has one or more non enzymatic peptides will be included
     */
    public void getFractionsOutput(JDialog aParentDialog, ArrayList<String> aProteinKeys, boolean aIndexes, boolean aOnlyValidated, boolean aMainAccession,
            boolean aOtherAccessions, boolean aPiDetails, boolean aDescription, boolean aMW, boolean aNPeptides, boolean aNSpectra, boolean aSequenceCoverage,
            boolean aNPeptidesPerFraction, boolean aNSpectraPerFraction, boolean aPrecursorIntensities, boolean aFractionSpread, boolean aIncludeHeader, boolean aOnlyStarred,
            boolean aShowStar, boolean aIncludeHidden, boolean aShowNonEnzymaticPeptidesColumn) {

        // @TODO: add the non enzymatic peptides detected information!!
        // create final versions of all variables use inside the export thread
        final ArrayList<String> proteinKeys;
        final boolean indexes = aIndexes;
        final boolean onlyValidated = aOnlyValidated;
        final boolean mainAccession = aMainAccession;
        final boolean otherAccessions = aOtherAccessions;
        final boolean piDetails = aPiDetails;
        final boolean description = aDescription;
        final boolean mw = aMW;
        final boolean nPeptides = aNPeptides;
        final boolean nSpectra = aNSpectra;
        final boolean sequenceCoverage = aSequenceCoverage;
        final boolean nPeptidesPerFraction = aNPeptidesPerFraction;
        final boolean nSpectraPerFraction = aNSpectraPerFraction;
        final boolean precursorIntensities = aPrecursorIntensities;
        final boolean fractionSpread = aFractionSpread;
        final boolean includeHeader = aIncludeHeader;
        final boolean onlyStarred = aOnlyStarred;
        final boolean showStar = aShowStar;
        final boolean includeHidden = aIncludeHidden;
        final boolean showNonEnzymaticPeptidesColumn = aShowNonEnzymaticPeptidesColumn;

        final JDialog parentDialog = aParentDialog;

        // get the file to send the output to
        final File selectedFile = peptideShakerGUI.getUserSelectedFile("fractions_export.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

        if (selectedFile != null) {

            final String filePath = selectedFile.getPath();

            try {
                writer = new BufferedWriter(new FileWriter(selectedFile));

                if (aProteinKeys == null) {
                    if (onlyValidated) {
                        proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getValidatedProteins(progressDialog, peptideShakerGUI.getFilterPreferences());
                    } else {
                        proteinKeys = new ArrayList<String>(identification.getProteinIdentification());
                    }
                } else {
                    proteinKeys = aProteinKeys;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "An error occurred when saving the file.", "Saving Failed", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }

            if (parentDialog != null) {
                progressDialog = new ProgressDialogX(parentDialog, peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            } else {
                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
            }

            progressDialog.setTitle("Copying to File. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {
                @Override
                public void run() {

                    try {
                        ArrayList<String> fractionFileNames = new ArrayList<String>();

                        for (String fileName : peptideShakerGUI.getIdentification().getOrderedSpectrumFileNames()) {
                            fractionFileNames.add(fileName);
                        }

                        if (includeHeader) {

                            if (indexes) {
                                writer.write(SEPARATOR);
                            }
                            if (mainAccession) {
                                writer.write("Accession" + SEPARATOR);
                            }
                            if (otherAccessions) {
                                writer.write("Other Protein(s)" + SEPARATOR);
                            }
                            if (piDetails) {
                                writer.write("Protein Inference Class" + SEPARATOR);
                            }
                            if (description) {
                                writer.write("Description" + SEPARATOR);
                            }
                            if (mw) {
                                writer.write("MW (kDa)" + SEPARATOR);
                            }
                            if (nPeptides) {
                                writer.write("#Validated Peptides" + SEPARATOR);
                            }
                            if (nSpectra) {
                                writer.write("#Validated Spectra" + SEPARATOR);
                            }
                            if (sequenceCoverage) {
                                writer.write("Sequence Coverage (%)" + SEPARATOR);
                                writer.write("Observable Coverage (%)" + SEPARATOR);
                            }
                            if (nPeptidesPerFraction) {
                                for (String fraction : fractionFileNames) {
                                    writer.write("#Peptides " + fraction + SEPARATOR);
                                }
                            }
                            if (nSpectraPerFraction) {
                                for (String fraction : fractionFileNames) {
                                    writer.write("#Spectra " + fraction + SEPARATOR);
                                }
                            }
                            if (precursorIntensities) {
                                for (String fraction : fractionFileNames) {
                                    writer.write("Average precursor intensity " + fraction + SEPARATOR);
                                }
                            }
                            if (fractionSpread) {
                                writer.write("Peptide Fraction Spread (lower range (kDa))" + SEPARATOR);
                                writer.write("Peptide Fraction Spread (upper range (kDa))" + SEPARATOR);
                                writer.write("Spectrum Fraction Spread (lower range (kDa))" + SEPARATOR);
                                writer.write("Spectrum Fraction Spread (upper range (kDa))" + SEPARATOR);
                            }
                            if (showNonEnzymaticPeptidesColumn) {
                                writer.write("Non Enzymatic Peptides" + SEPARATOR);
                            }
                            if (includeHidden) {
                                writer.write("Hidden" + SEPARATOR);
                            }
                            if (!onlyStarred && showStar) {
                                writer.write("Starred" + SEPARATOR);
                            }
                            writer.write(lineBreak);
                        }

                        PSParameter proteinPSParameter = new PSParameter();
                        PSParameter peptidePSParameter = new PSParameter();
                        int proteinCounter = 0;

                        progressDialog.setTitle("Loading Protein Matches. Please Wait...");
                        identification.loadProteinMatches(progressDialog, true);
                        progressDialog.setTitle("Loading Protein Details. Please Wait...");
                        identification.loadProteinMatchParameters(proteinPSParameter, progressDialog, true);

                        progressDialog.setPrimaryProgressCounterIndeterminate(false);
                        progressDialog.setMaxPrimaryProgressCounter(proteinKeys.size());
                        progressDialog.setValue(0);
                        progressDialog.setTitle("Copying to File. Please Wait...");

                        for (String proteinKey : proteinKeys) {

                            if (progressDialog.isRunCanceled()) {
                                break;
                            }

                            proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinPSParameter);

                            if (!ProteinMatch.isDecoy(proteinKey) || !onlyValidated) {
                                if ((onlyValidated && proteinPSParameter.getMatchValidationLevel().isValidated()) || !onlyValidated) {
                                    if ((!includeHidden && !proteinPSParameter.isHidden()) || includeHidden) {
                                        if ((onlyStarred && proteinPSParameter.isStarred()) || !onlyStarred) {
                                            if (indexes) {
                                                writer.write(++proteinCounter + SEPARATOR);
                                            }

                                            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                                            if (mainAccession) {
                                                writer.write(proteinMatch.getMainMatch() + SEPARATOR);
                                            }
                                            if (otherAccessions) {
                                                boolean first = true;
                                                for (String otherProtein : proteinMatch.getTheoreticProteinsAccessions()) {
                                                    if (!otherProtein.equals(proteinMatch.getMainMatch())) {
                                                        if (first) {
                                                            first = false;
                                                        } else {
                                                            writer.write(", ");
                                                        }
                                                        writer.write(otherProtein);
                                                    }
                                                }
                                                writer.write(SEPARATOR);
                                            }
                                            if (piDetails) {
                                                writer.write(proteinPSParameter.getProteinInferenceClassAsString() + SEPARATOR);
                                            }
                                            if (description) {
                                                try {
                                                    writer.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription() + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }
                                            if (mw) {
                                                Double proteinMW = sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch());
                                                writer.write(proteinMW + SEPARATOR);
                                            }
                                            if (nPeptides) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedPeptides(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    peptideShakerGUI.catchException(e);
                                                    writer.write(Double.NaN + SEPARATOR);
                                                }
                                            }
                                            if (nSpectra) {
                                                try {
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedSpectra(proteinKey) + SEPARATOR);
                                                } catch (Exception e) {
                                                    peptideShakerGUI.catchException(e);
                                                    writer.write(Double.NaN + SEPARATOR);
                                                }
                                            }
                                            if (sequenceCoverage) {
                                                try {
                                                    HashMap<Integer, Double> sequenceCoverage;
                                                    try {
                                                        sequenceCoverage = peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                                                    } catch (Exception e) {
                                                        peptideShakerGUI.catchException(e);
                                                        sequenceCoverage = new HashMap<Integer, Double>();
                                                    }
                                                    Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                                                    Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                                                    Double validatedCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful;
                                                    writer.write(validatedCoverage + SEPARATOR);
                                                    writer.write(peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey) * 100 + SEPARATOR);
                                                } catch (Exception e) {
                                                    writer.write("error: " + e.getLocalizedMessage() + SEPARATOR);
                                                }
                                            }

                                            if (nPeptidesPerFraction) {
                                                for (String fraction : fractionFileNames) {
                                                    if (proteinPSParameter.getFractions() != null && proteinPSParameter.getFractions().contains(fraction)
                                                            && proteinPSParameter.getFractionValidatedPeptides(fraction) != null) {
                                                        writer.write(proteinPSParameter.getFractionValidatedPeptides(fraction) + SEPARATOR);
                                                    } else {
                                                        writer.write("0.0" + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (nSpectraPerFraction) {
                                                for (String fraction : fractionFileNames) {
                                                    if (proteinPSParameter.getFractions() != null && proteinPSParameter.getFractions().contains(fraction)
                                                            && proteinPSParameter.getFractionValidatedSpectra(fraction) != null) {
                                                        writer.write(proteinPSParameter.getFractionValidatedSpectra(fraction) + SEPARATOR);
                                                    } else {
                                                        writer.write("0.0" + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (precursorIntensities) {
                                                for (String fraction : fractionFileNames) {
                                                    if (proteinPSParameter.getFractions() != null && proteinPSParameter.getFractions().contains(fraction)
                                                            && proteinPSParameter.getPrecursorIntensityAveragePerFraction(fraction) != null) {
                                                        writer.write(proteinPSParameter.getPrecursorIntensityAveragePerFraction(fraction) + SEPARATOR);
                                                    } else {
                                                        writer.write("0.0" + SEPARATOR);
                                                    }
                                                }
                                            }
                                            if (fractionSpread) {

                                                double maxMwRangePeptides = Double.MIN_VALUE;
                                                double minMwRangePeptides = Double.MAX_VALUE;

                                                for (String fraction : fractionFileNames) {
                                                    if (proteinPSParameter.getFractions() != null && proteinPSParameter.getFractions().contains(fraction)
                                                            && proteinPSParameter.getFractionValidatedPeptides(fraction) != null
                                                            && proteinPSParameter.getFractionValidatedPeptides(fraction) > 0) {

                                                        HashMap<String, XYDataPoint> expectedMolecularWeightRanges
                                                                = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFractionMolecularWeightRanges();

                                                        if (expectedMolecularWeightRanges != null && expectedMolecularWeightRanges.get(fraction) != null) {

                                                            double lower = expectedMolecularWeightRanges.get(fraction).getX();
                                                            double upper = expectedMolecularWeightRanges.get(fraction).getY();

                                                            if (lower < minMwRangePeptides) {
                                                                minMwRangePeptides = lower;
                                                            }
                                                            if (upper > maxMwRangePeptides) {
                                                                maxMwRangePeptides = upper;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (maxMwRangePeptides != Double.MIN_VALUE && minMwRangePeptides != Double.MAX_VALUE) {
                                                    writer.write(minMwRangePeptides + SEPARATOR + maxMwRangePeptides + SEPARATOR);
                                                } else {
                                                    writer.write("N/A" + SEPARATOR + "N/A" + SEPARATOR);
                                                }

                                                double maxMwRangeSpectra = Double.MIN_VALUE;
                                                double minMwRangeSpectra = Double.MAX_VALUE;

                                                for (String fraction : fractionFileNames) {
                                                    if (proteinPSParameter.getFractions() != null && proteinPSParameter.getFractions().contains(fraction)
                                                            && proteinPSParameter.getFractionValidatedSpectra(fraction) != null
                                                            && proteinPSParameter.getFractionValidatedSpectra(fraction) > 0) {

                                                        HashMap<String, XYDataPoint> expectedMolecularWeightRanges
                                                                = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFractionMolecularWeightRanges();

                                                        if (expectedMolecularWeightRanges != null && expectedMolecularWeightRanges.get(fraction) != null) {

                                                            double lower = expectedMolecularWeightRanges.get(fraction).getX();
                                                            double upper = expectedMolecularWeightRanges.get(fraction).getY();

                                                            if (lower < minMwRangeSpectra) {
                                                                minMwRangeSpectra = lower;
                                                            }
                                                            if (upper > maxMwRangeSpectra) {
                                                                maxMwRangeSpectra = upper;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (maxMwRangeSpectra != Double.MIN_VALUE && minMwRangeSpectra != Double.MAX_VALUE) {
                                                    writer.write(minMwRangeSpectra + SEPARATOR + maxMwRangeSpectra + SEPARATOR);
                                                } else {
                                                    writer.write("N/A" + SEPARATOR + "N/A" + SEPARATOR);
                                                }
                                            }
                                            if (showNonEnzymaticPeptidesColumn) {

                                                ArrayList<String> peptideKeys = proteinMatch.getPeptideMatchesKeys();
                                                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                                                boolean allPeptidesEnzymatic = true;

                                                identification.loadPeptideMatches(peptideKeys, null, true);
                                                identification.loadPeptideMatchParameters(peptideKeys, peptidePSParameter, null, true);

                                                // see if we have non-tryptic peptides
                                                for (String peptideKey : peptideKeys) {

                                                    String peptideSequence = identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getSequence();
                                                    peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptidePSParameter);

                                                    if (peptidePSParameter.getMatchValidationLevel().isValidated()) {

                                                        boolean isEnzymatic = currentProtein.isEnzymaticPeptide(peptideSequence,
                                                                peptideShakerGUI.getShotgunProtocol().getEnzyme(),
                                                                peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());

                                                        if (!isEnzymatic) {
                                                            allPeptidesEnzymatic = false;
                                                            break;
                                                        }
                                                    }
                                                }

                                                writer.write(!allPeptidesEnzymatic + SEPARATOR);
                                            }
                                            MatchValidationLevel matchValidationLevel = proteinPSParameter.getMatchValidationLevel();
                                            writer.write(matchValidationLevel.toString());
                                            writer.write(SEPARATOR);
                                            if (includeHidden) {
                                                writer.write(proteinPSParameter.isHidden() + SEPARATOR);
                                            }
                                            if (!onlyStarred && showStar) {
                                                writer.write(proteinPSParameter.isStarred() + "");
                                            }
                                            writer.write(lineBreak);
                                        }

                                    }
                                }
                            }

                            progressDialog.increasePrimaryProgressCounter();
                        }

                        writer.close();

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + filePath, "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Returns the possible precursor charges for a given peptide match. The
     * charges are returned in increasing order with each charge only appearing
     * once.
     *
     * @param peptideMatch the peptide match
     * @return the possible precursor charges
     */
    public String getPeptidePrecursorChargesAsString(PeptideMatch peptideMatch) {

        StringBuilder results = new StringBuilder();

        ArrayList<String> spectrumKeys = peptideMatch.getSpectrumMatchesKeys();
        ArrayList<Integer> charges = new ArrayList<Integer>(5);

        // find all unique the charges
        try {
            identification.loadSpectrumMatches(spectrumKeys, null, true);
        } catch (Exception e) {
            e.printStackTrace();
            //ignore caching error
        }
        for (String spectrumKey : spectrumKeys) {
            try {
                int tempCharge = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey).getBestPeptideAssumption().getIdentificationCharge().value;
                if (!charges.contains(tempCharge)) {
                    charges.add(tempCharge);
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return "Error";
            }
        }

        // sort the charges
        Collections.sort(charges);

        // add the charges to the output
        for (int i = 0; i < charges.size(); i++) {
            if (i > 0) {
                results.append(", ");
            }

            results.append(charges.get(i));
        }

        return results.toString();
    }

    /**
     * Returns the peptide modifications as a string.
     *
     * @param peptide the peptide
     * @param variablePtms if true, only variable PTMs are shown, false return
     * only the fixed PTMs
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide, boolean variablePtms) {

        StringBuilder result = new StringBuilder();
        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if ((variablePtms && modificationMatch.isVariable()) || (!variablePtms && !modificationMatch.isVariable())) {
                    if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                        modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                    }
                    modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                }
            }
        }

        boolean first = true;
        ArrayList<String> mods = new ArrayList<String>(modMap.keySet());
        Collections.sort(mods);

        for (String mod : mods) {

            if (first) {
                first = false;
            } else {
                result.append(", ");
            }

            boolean first2 = true;
            result.append(mod);
            result.append(" (");

            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    result.append(", ");
                }
                result.append(aa);
            }

            result.append(")");
        }

        return result.toString();
    }

    /**
     * Returns the peptide modification location confidence as a string.
     *
     * @param peptide the peptide
     * @param peptideMatch the peptide match
     * @param ptmProfile the PTM profile
     * @return the peptide modification location confidence as a string.
     */
    public static String getPeptideModificationLocations(Peptide peptide, PeptideMatch peptideMatch, PtmSettings ptmProfile) {

        PTMFactory ptmFactory = PTMFactory.getInstance();

        String result = "";
        ArrayList<String> modList = new ArrayList<String>();

        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if (modificationMatch.isVariable()) {
                    PTM refPtm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                    for (String equivalentPtm : ptmProfile.getSimilarNotFixedModifications(refPtm.getMass())) {
                        if (!modList.contains(equivalentPtm)) {
                            modList.add(equivalentPtm);
                        }
                    }
                }
            }
        }

        Collections.sort(modList);
        boolean first = true;

        for (String mod : modList) {
            if (first) {
                first = false;
            } else {
                result += ", ";
            }
            PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            result += mod + " (";
            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                boolean firstSite = true;
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                    if (firstSite) {
                        firstSite = false;
                    } else {
                        result += ", ";
                    }
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                        result += site + ": Not Scored"; // Well this should not happen
                    } else if (ptmConfidence == PtmScoring.RANDOM) {
                        result += site + ": Random";
                    } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                        result += site + ": Doubtfull";
                    } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                        result += site + ": Confident";
                    } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                        result += site + ": Very Confident";
                    }
                }
            } else {
                result += "Not Scored";
            }
            result += ")";
        }
        return result;
    }
}
