package eu.isas.peptideshaker.export;

import com.compomics.util.BinaryArrayImpl;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.pride.prideobjects.*;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.pride.PrideExportDialog;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to PRIDE XML.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class PRIDEExport {

    /**
     * The main instance of the GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The experiment title.
     */
    private String experimentTitle;
    /**
     * The experiment label.
     */
    private String experimentLabel;
    /**
     * The experiment description.
     */
    private String experimentDescription;
    /**
     * The experiment project.
     */
    private String experimentProject;
    /**
     * The references to include in the PRIDE xml file as utilities pride object
     */
    private ArrayList<Reference> references;
    /**
     * The contact utilities PRIDE object.
     */
    private Contact contact;
    /**
     * THe sample utilities PRIDE object.
     */
    private Sample sample;
    /**
     * The protocol utilities PRIDE object.
     */
    private Protocol protocol;
    /**
     * The instrument utilities PRIDE object.
     */
    private Instrument instrument;
    /**
     * The fileWriter.
     */
    private FileWriter r;
    /**
     * The buffered writer which will write the results in the desired file.
     */
    private BufferedWriter br;
    /**
     * integer keeping track of the number of tabs to include at the beginning
     * of each line.
     */
    private int tabCounter = 0;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum key to PRIDE spectrum index map - key: spectrum key,
     * element: PRIDE XML file spectrum index.
     */
    private HashMap<String, Long> spectrumIndexes;
    /**
     * The ptm to PRIDE map.
     */
    private PtmToPrideMap ptmToPrideMap;
    /**
     * The length of the task.
     */
    private long totalProgress;
    /**
     * The current progress.
     */
    private long progress = 0;
    /**
     * The number of decimals to use for the confidence values.
     */
    private final int CONFIDENCE_DECIMALS = 2;
    /**
     * A reference to the PrideExportDialog.
     */
    private PrideExportDialog prideExportDialog;

    /**
     * Constructor.
     *
     * @param peptideShakerGUI Instance of the main GUI class
     * @param prideExportDialog A reference to the PrideExportDialog.
     * @param experimentTitle Title of the experiment
     * @param experimentLabel Label of the experiment
     * @param experimentDescription Description of the experiment
     * @param experimentProject project of the experiment
     * @param references References for the experiment
     * @param contact Contact for the experiment
     * @param sample Samples in this experiment
     * @param protocol Protocol used in this experiment
     * @param instrument Instruments used in this experiment
     * @param outputFolder Output folder
     * @param fileName the file name without extension
     * @throws FileNotFoundException Exception thrown whenever a file was not
     * found
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a pride object
     */
    public PRIDEExport(PeptideShakerGUI peptideShakerGUI, PrideExportDialog prideExportDialog, String experimentTitle, String experimentLabel, String experimentDescription, String experimentProject,
            ArrayList<Reference> references, Contact contact, Sample sample, Protocol protocol, Instrument instrument,
            File outputFolder, String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.peptideShakerGUI = peptideShakerGUI;
        this.prideExportDialog = prideExportDialog;
        this.experimentTitle = experimentTitle;
        this.experimentLabel = experimentLabel;
        this.experimentDescription = experimentDescription;
        this.experimentProject = experimentProject;
        this.references = references;
        this.contact = contact;
        this.sample = sample;
        this.protocol = protocol;
        this.instrument = instrument;
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance();
        ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        r = new FileWriter(new File(outputFolder, fileName + ".xml"));
        br = new BufferedWriter(r);
    }

    /**
     * Creates the PRIDE xml file.
     *
     * @param progressDialog a dialog displaying progress to the user
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/wrinting a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    public void createPrideXmlFile(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        // the experiment start tag
        writeExperimentCollectionStartTag();

        // the experiment title
        writeTitle();

        // the references, if any
        if (references.size() > 0) {
            writeReferences();
        }

        // the short label
        writeShortLabel();

        // the protocol
        writeProtocol();

        if (prideExportDialog.progressCancelled()) {
            br.close();
            r.close();
            return;
        }

        // get the spectrum count
        totalProgress = 0;
        for (String mgfFile : spectrumFactory.getMgfFileNames()) {
            totalProgress += spectrumFactory.getNSpectra(mgfFile);
        }
        totalProgress = 2 * totalProgress;
        progressDialog.setIndeterminate(false);
        progressDialog.setMaxProgressValue(100);
        progressDialog.setValue(0);

        if (prideExportDialog.progressCancelled()) {
            br.close();
            r.close();
            return;
        }

        // the mzData element
        writeMzData(progressDialog);

        if (prideExportDialog.progressCancelled()) {
            br.close();
            r.close();
            return;
        }

        // the PSMs
        writePsms(progressDialog);

        if (prideExportDialog.progressCancelled()) {
            br.close();
            r.close();
            return;
        }

        // the additional tags
        writeAdditionalTags();

        if (prideExportDialog.progressCancelled()) {
            br.close();
            r.close();
            return;
        }

        // the experiment end tag
        writeExperimentCollectionEndTag();

        br.close();
        r.close();
    }

    /**
     * Writes all psms.
     *
     * @param progressDialog a progress dialog to display progress to the user
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writePsms(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        try {
            SequenceFactory sequenceFactory = SequenceFactory.getInstance();
            Identification identification = peptideShakerGUI.getIdentification();
            PSParameter proteinProbabilities = new PSParameter();
            PSParameter peptideProbabilities = new PSParameter();
            PSParameter psmProbabilities = new PSParameter();

            progressDialog.setTitle("Creating PRIDE XML File. Please Wait...  (Part 2 of 2: Exporting IDs)");
            long increment = totalProgress / (2 * identification.getProteinIdentification().size());

            PSMaps pSMaps = new PSMaps();
            pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);
            ProteinMap proteinTargetDecoyMap = pSMaps.getProteinMap();
            PsmSpecificMap psmTargetDecoyMap = pSMaps.getPsmSpecificMap();
            PeptideSpecificMap peptideTargetDecoyMap = pSMaps.getPeptideSpecificMap();

            // get the list of search engines used
            IdfileReaderFactory idFileReaderFactory = IdfileReaderFactory.getInstance();
            ArrayList<File> idFiles = peptideShakerGUI.getProjectDetails().getIdentificationFiles();

            ArrayList<Integer> seList = new ArrayList<Integer>();

            for (File file : idFiles) {
                int currentSE = idFileReaderFactory.getSearchEngine(file);
                if (!seList.contains(currentSE)) {
                    seList.add(currentSE);
                }
            }

            Collections.sort(seList);
            String searchEngineReport = SearchEngine.getName(seList.get(0));

            for (int i = 1; i < seList.size(); i++) {

                if (i == seList.size() - 1) {
                    searchEngineReport += " and ";
                } else {
                    searchEngineReport += ", ";
                }

                searchEngineReport += SearchEngine.getName(seList.get(i));
            }

            searchEngineReport += " post-processed by PeptideShaker v" + peptideShakerGUI.getVersion();

            for (String proteinKey : identification.getProteinIdentification()) {

                if (prideExportDialog.progressCancelled()) {
                    break;
                }
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                proteinProbabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinProbabilities);
                double confidenceThreshold;

                br.write(getCurrentTabSpace() + "<GelFreeIdentification>" + System.getProperty("line.separator"));
                tabCounter++;

                // protein accession and database
                br.write(getCurrentTabSpace() + "<Accession>" + proteinMatch.getMainMatch() + "</Accession>" + System.getProperty("line.separator"));
                br.write(getCurrentTabSpace() + "<Database>" + sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDatabaseType() + "</Database>" + System.getProperty("line.separator"));

                for (String peptideKey : proteinMatch.getPeptideMatches()) {

                    if (prideExportDialog.progressCancelled()) {
                        break;
                    }

                    PeptideMatch currentMatch = identification.getPeptideMatch(peptideKey);
                    peptideProbabilities = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, peptideProbabilities);

                    for (String spectrumKey : currentMatch.getSpectrumMatches()) {

                        if (prideExportDialog.progressCancelled()) {
                            break;
                        }

                        psmProbabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, psmProbabilities);
                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        PeptideAssumption bestAssumption = spectrumMatch.getBestAssumption();
                        Peptide tempPeptide = bestAssumption.getPeptide();

                        // the peptide
                        br.write(getCurrentTabSpace() + "<PeptideItem>" + System.getProperty("line.separator"));
                        tabCounter++;

                        // peptide sequence
                        br.write(getCurrentTabSpace() + "<Sequence>" + tempPeptide.getSequence() + "</Sequence>" + System.getProperty("line.separator"));

                        // peptide start and end
                        String proteinAccession = proteinMatch.getMainMatch();
                        String proteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                        int peptideStart = proteinSequence.lastIndexOf(tempPeptide.getSequence()) + 1;
                        br.write(getCurrentTabSpace() + "<Start>" + peptideStart + "</Start>" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<End>" + (peptideStart + tempPeptide.getSequence().length() - 1) + "</End>" + System.getProperty("line.separator"));

                        // spectrum index reference
                        br.write(getCurrentTabSpace() + "<SpectrumReference>" + spectrumIndexes.get(spectrumMatch.getKey()) + "</SpectrumReference>" + System.getProperty("line.separator"));

                        // modifications
                        writePtms(tempPeptide);

                        // fragment ions
                        writeFragmentIons(spectrumMatch);

                        // Get scores
                        HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
                        Double mascotScore = null;
                        for (int se : spectrumMatch.getAdvocates()) {
                            for (double eValue : spectrumMatch.getAllAssumptions(se).keySet()) {
                                for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                                    if (assumption.getPeptide().isSameAs(bestAssumption.getPeptide())) {
                                        if (!scores.containsKey(se) || scores.get(se) > eValue) {
                                            scores.put(se, eValue);
                                            if (se == Advocate.MASCOT) {
                                                mascotScore = ((MascotScore) assumption.getUrParam(new MascotScore(0))).getScore();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // PTM scores
                        ArrayList<String> modifications = new ArrayList<String>();

                        for (ModificationMatch modificationMatch : bestAssumption.getPeptide().getModificationMatches()) {
                            if (modificationMatch.isVariable()) {
                                if (!modifications.contains(modificationMatch.getTheoreticPtm())) {
                                    modifications.add(modificationMatch.getTheoreticPtm());
                                }
                            }
                        }

                        boolean first = true;
                        String dScore = "";
                        Collections.sort(modifications);
                        PSPtmScores ptmScores = new PSPtmScores();

                        first = true;

                        for (String mod : modifications) {

                            if (spectrumMatch.getUrParam(ptmScores) != null) {

                                if (first) {
                                    first = false;
                                } else {
                                    dScore += ", ";
                                }

                                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                dScore += mod + " (";

                                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                    String location = ptmScores.getPtmScoring(mod).getBestDeltaScoreLocations();
                                    if (location != null) {
                                        ArrayList<Integer> locations = PtmScoring.getLocations(location);
                                        Collections.sort(locations);
                                        first = true;
                                        String commaSeparated = "";
                                        for (int aa : locations) {
                                            if (first) {
                                                first = false;
                                            } else {
                                                commaSeparated += ", ";
                                            }
                                            commaSeparated += aa;
                                        }
                                        dScore += commaSeparated + ": ";
                                        dScore += ptmScores.getPtmScoring(mod).getDeltaScore(location);
                                    } else {
                                        dScore += "Not Scored";
                                    }
                                } else {
                                    dScore += "Not Scored";
                                }

                                dScore += ")";
                            }
                        }

                        String aScore = "";

                        if (peptideShakerGUI.getPtmScoringPreferences().aScoreCalculation()) {
                            first = true;

                            for (String mod : modifications) {


                                if (spectrumMatch.getUrParam(ptmScores) != null) {

                                    if (first) {
                                        first = false;
                                    } else {
                                        aScore += ", ";
                                    }
                                    ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                    aScore += mod + " (";

                                    if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {

                                        String location = ptmScores.getPtmScoring(mod).getBestAScoreLocations();
                                        if (location != null) {
                                            ArrayList<Integer> locations = PtmScoring.getLocations(location);
                                            Collections.sort(locations);
                                            first = true;
                                            String commaSeparated = "";
                                            for (int aa : locations) {

                                                if (first) {
                                                    first = false;
                                                } else {
                                                    commaSeparated += ", ";
                                                }
                                                commaSeparated += aa;
                                            }
                                            aScore += commaSeparated + ": ";
                                            aScore += ptmScores.getPtmScoring(mod).getAScore(location);
                                        } else {
                                            aScore += "Not Scored";
                                        }
                                    } else {
                                        aScore += "Not Scored";
                                    }

                                    aScore += ")";
                                }
                            }
                        }
                        
                        ArrayList<String> peptideParentProteins = tempPeptide.getParentProteins();
                        String peptideProteins = "";
                        for (String accession : peptideParentProteins) {
                                if (!peptideProteins.equals("")) {
                                    peptideProteins += ", ";
                                }
                                peptideProteins += accession;
                        }

                        // additional peptide id parameters
                        br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
                        tabCounter++;
                        br.write(getCurrentTabSpace() + "<userParam name=\"Spectrum File\" value=\"" + Spectrum.getSpectrumFile(spectrumKey) + "\" />" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<userParam name=\"Spectrum Title\" value=\"" + Spectrum.getSpectrumTitle(spectrumKey) + "\" />" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<userParam name=\"Protein inference\" value=\"" + peptideProteins + "\" />" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Confidence\" value=\"" + Util.roundDouble(peptideProbabilities.getPeptideConfidence(), CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        confidenceThreshold = peptideTargetDecoyMap.getTargetDecoyMap(peptideTargetDecoyMap.getCorrectedKey(peptideProbabilities.getSecificMapKey())).getTargetDecoyResults().getConfidenceLimit();
                        br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        if (peptideProbabilities.isValidated()) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Validation\" value=\"Yes\" />" + System.getProperty("line.separator"));
                        } else {
                            br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Validation\" value=\"No\" />" + System.getProperty("line.separator"));
                        }
                        br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence\" value=\"" + Util.roundDouble(psmProbabilities.getPsmConfidence(), CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        confidenceThreshold = psmTargetDecoyMap.getTargetDecoyMap(psmTargetDecoyMap.getCorrectedKey(psmProbabilities.getSecificMapKey())).getTargetDecoyResults().getConfidenceLimit();
                        br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        if (psmProbabilities.isValidated()) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PSM Validation\" value=\"Yes\" />" + System.getProperty("line.separator"));
                        } else {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PSM Validation\" value=\"No\" />" + System.getProperty("line.separator"));
                        }
                        br.write(getCurrentTabSpace() + "<userParam name=\"Identified Charge\" value=\"" + bestAssumption.getIdentificationCharge().toString() + "\" />" + System.getProperty("line.separator"));

                        // search engine specific parameters
                        ArrayList<Integer> searchEngines = new ArrayList<Integer>(scores.keySet());
                        Collections.sort(searchEngines);
                        Advocate advocate;
                        for (int se : searchEngines) {
                            advocate = AdvocateFactory.getInstance().getAdvocate(se);
                            br.write(getCurrentTabSpace() + "<userParam name=\"" + advocate.getName() + " e-value\" value=\"" + scores.get(se) + "\" />" + System.getProperty("line.separator"));
                        }
                        if (mascotScore != null) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"Mascot score\" value=\"" + mascotScore + "\" />" + System.getProperty("line.separator"));
                        }
                        
                        // PTM scoring
                        if (!dScore.equals("")) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PTM D-score\" value=\"" + dScore + "\" />" + System.getProperty("line.separator"));
                        }
                        if (peptideShakerGUI.getPtmScoringPreferences().aScoreCalculation()) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PTM A-score\" value=\"" + aScore + "\" />" + System.getProperty("line.separator"));
                        }
                        tabCounter--;
                        br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));
                        tabCounter--;
                        br.write(getCurrentTabSpace() + "</PeptideItem>" + System.getProperty("line.separator"));
                    }
                }

                // additional protein id parameters
                br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
                tabCounter++;
                if (SequenceFactory.isDecoy(proteinKey)) {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Decoy\" value=\"1\" />" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Decoy\" value=\"0\" />" + System.getProperty("line.separator"));
                }
                if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) {
                    br.write(getCurrentTabSpace() + "<userParam name=\"emPAI\" value=\"" + peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey) + "\" />" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<userParam name=\"NSAF+\" value=\"" + peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey) + "\" />" + System.getProperty("line.separator"));
                }
                if (proteinProbabilities.isValidated()) {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Protein Validation\" value=\"Yes\" />" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Protein Validation\" value=\"No\" />" + System.getProperty("line.separator"));
                }
                String otherProteins = "";
                boolean first = true;
                for (String otherAccession : proteinMatch.getTheoreticProteinsAccessions()) {
                    if (!otherAccession.equals(proteinMatch.getMainMatch())) {
                        if (first) {
                            first = false;
                        } else {
                            otherAccession += ", ";
                        }
                        otherProteins += otherAccession;
                    }
                }
                if (!otherProteins.equals("")) {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Secondary proteins\" value=\"" + otherProteins + "\" />" + System.getProperty("line.separator"));
                }
                tabCounter--;
                br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));

                // protein score
                br.write(getCurrentTabSpace() + "<Score>" + Util.roundDouble(proteinProbabilities.getProteinConfidence(), CONFIDENCE_DECIMALS) + "</Score>" + System.getProperty("line.separator"));

                // protein threshold
                confidenceThreshold = proteinTargetDecoyMap.getTargetDecoyMap().getTargetDecoyResults().getConfidenceLimit();
                br.write(getCurrentTabSpace() + "<Threshold>" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "</Threshold>" + System.getProperty("line.separator"));

                // the search engines used
                br.write(getCurrentTabSpace() + "<SearchEngine>" + searchEngineReport + "</SearchEngine>" + System.getProperty("line.separator"));

                tabCounter--;
                br.write(getCurrentTabSpace() + "</GelFreeIdentification>" + System.getProperty("line.separator"));

                progress += increment;
                progressDialog.setValue((int) ((100 * progress) / totalProgress));
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Writes the fragment ions for a given spectrum match.
     *
     * @param spectrumMatch the spectrum match considered
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writeFragmentIons(SpectrumMatch spectrumMatch) throws IOException, MzMLUnmarshallerException {

        Peptide peptide = spectrumMatch.getBestAssumption().getPeptide();
        SpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnorator();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        annotationPreferences.setCurrentSettings(peptide, spectrumMatch.getBestAssumption().getIdentificationCharge().value, true);
        MSnSpectrum tempSpectrum = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey()));

        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges(),
                spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                tempSpectrum, peptide,
                tempSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                annotationPreferences.getFragmentIonAccuracy(), false);

        for (int i = 0; i < annotations.size(); i++) {
            writeFragmentIon(annotations.get(i));
        }
    }

    /**
     * Writes the line corresponding to an ion match.
     *
     * @param ionMatch the ion match considered
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeFragmentIon(IonMatch ionMatch) throws IOException {


        // @TODO: to add neutral losses with more than one loss we need to create new CV terms!!
        // @TODO: to add phospho neutral losses we need to create new CV terms!!

        CvTerm fragmentIonTerm = ionMatch.ion.getPrideCvTerm();

        if (fragmentIonTerm != null) {
            if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                    || ionMatch.ion.getType() == IonType.IMMONIUM_ION
                    || ionMatch.ion.getType() == IonType.PRECURSOR_ION
                    || ionMatch.ion.getType() == IonType.REPORTER_ION) {
                br.write(getCurrentTabSpace() + "<FragmentIon>" + System.getProperty("line.separator"));
                tabCounter++;
                writeCvTerm(fragmentIonTerm);
                writeCvTerm(ionMatch.getMZPrideCvTerm());
                writeCvTerm(ionMatch.getIntensityPrideCvTerm());
                writeCvTerm(ionMatch.getIonMassErrorPrideCvTerm());
                writeCvTerm(ionMatch.getChargePrideCvTerm());
                tabCounter--;
                br.write(getCurrentTabSpace() + "</FragmentIon>" + System.getProperty("line.separator"));
            }
        }
    }

    /**
     * Writes the PTMs detected in a peptide.
     *
     * @param peptide the peptide of interest
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writePtms(Peptide peptide) throws IOException {

        for (int i = 0; i < peptide.getModificationMatches().size(); i++) {

            br.write(getCurrentTabSpace() + "<ModificationItem>" + System.getProperty("line.separator"));
            tabCounter++;

            ModificationMatch modMatch = peptide.getModificationMatches().get(i);
            String modName = modMatch.getTheoreticPtm();
            PTM ptm = ptmFactory.getPTM(modName);

            CvTerm cvTerm = ptmToPrideMap.getCVTerm(modName);
            String cvTermName;
            String ptmMass;

            if (cvTerm == null) {
                cvTermName = modName;
                ptmMass = "" + ptm.getMass();
            } else {
                cvTermName = cvTerm.getName();
                ptmMass = cvTerm.getValue();
            }

            br.write(getCurrentTabSpace() + "<ModLocation>" + modMatch.getModificationSite() + "</ModLocation>" + System.getProperty("line.separator"));

            if (cvTerm == null) {
                // @TODO: perhaps this should be handled differently? as there is no real mapping...
                br.write(getCurrentTabSpace() + "<ModAccession>" + cvTermName + "</ModAccession>" + System.getProperty("line.separator"));
                br.write(getCurrentTabSpace() + "<ModDatabase>" + "MOD" + "</ModDatabase>" + System.getProperty("line.separator"));
            } else {
                br.write(getCurrentTabSpace() + "<ModAccession>" + cvTerm.getAccession() + "</ModAccession>" + System.getProperty("line.separator"));
                br.write(getCurrentTabSpace() + "<ModDatabase>" + "MOD" + "</ModDatabase>" + System.getProperty("line.separator"));
            }

            br.write(getCurrentTabSpace() + "<ModMonoDelta>" + ptmMass + "</ModMonoDelta>" + System.getProperty("line.separator"));

            br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
            tabCounter++;
            if (cvTerm == null) {
                br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MOD\" accession=\"" + "unknown" + "\" name=\"" + cvTermName + "\" value=\"" + ptmMass + "\" />" + System.getProperty("line.separator"));
            } else {
                br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MOD\" accession=\"" + cvTerm.getAccession() + "\" name=\"" + cvTermName + "\" value=\"" + ptmMass + "\" />" + System.getProperty("line.separator"));
            }
            tabCounter--;
            br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</ModificationItem>" + System.getProperty("line.separator"));
        }
    }

    /**
     * Writes the spectra in the mzData format.
     *
     * @param progressDialog a progress dialog to display progress to the user
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writeMzData(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        br.write(getCurrentTabSpace() + "<mzData version=\"1.05\" accessionNumber=\"0\">" + System.getProperty("line.separator"));
        tabCounter++;

        // include the ontologies used, only MS is included by default
        br.write(getCurrentTabSpace() + "<cvLookup cvLabel=\"MS\" fullName=\"PSI Mass Spectrometry Ontology\" version=\"1.0.0\" address=\"http://psidev.sourceforge.net/ontology\" />" + System.getProperty("line.separator"));

        // write the mzData description (project description, sample details, contact details, instrument details and software details)
        writeMzDataDescription();

        // write the spectra
        writeSpectra(progressDialog);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</mzData>" + System.getProperty("line.separator"));
    }

    /**
     * Writes all spectra in the mzData format.
     *
     * @param progressDialog a progress dialog to display progress to the user
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writeSpectra(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        progressDialog.setTitle("Creating PRIDE XML File. Please Wait...  (Part 1 of 2: Exporting Spectra)");

        spectrumIndexes = new HashMap<String, Long>();

        long spectrumCounter = 0;

        br.write(getCurrentTabSpace() + "<spectrumList count=\"" + spectrumCounter + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        progressDialog.setIndeterminate(false);

        Identification identification = peptideShakerGUI.getIdentification();

        for (String mgfFile : spectrumFactory.getMgfFileNames()) {

            if (prideExportDialog.progressCancelled()) {
                break;
            }

            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(mgfFile)) {

                if (prideExportDialog.progressCancelled()) {
                    break;
                }

                String spectrumKey = Spectrum.getSpectrumKey(mgfFile, spectrumTitle);
                MSnSpectrum tempSpectrum = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey));
                if (!tempSpectrum.getPeakList().isEmpty()) {
                    boolean identified = identification.matchExists(spectrumKey);
                    writeSpectrum(tempSpectrum, identified, spectrumCounter);
                    if (identified) {
                        spectrumIndexes.put(spectrumKey, spectrumCounter);
                    }
                    spectrumCounter++;
                }
                progress++;
                progressDialog.setValue((int) ((100 * progress) / totalProgress));
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumList>" + System.getProperty("line.separator"));
    }

    /**
     * Writes a spectrum.
     *
     * @param spectrum The spectrum
     * @param matchExists boolean indicating whether the match exists
     * @param spectrumCounter index of the spectrum
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeSpectrum(MSnSpectrum spectrum, boolean matchExists, long spectrumCounter) throws IOException {

        br.write(getCurrentTabSpace() + "<spectrum id=\"" + spectrumCounter + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<spectrumDesc>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<spectrumSettings>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<spectrumInstrument mzRangeStop=\"" + spectrum.getMaxMz()
                + " \" mzRangeStart=\"" + spectrum.getMinMz()
                + "\" msLevel=\"" + spectrum.getLevel() + "\" />" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumSettings>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<precursorList count=\"1\">" + System.getProperty("line.separator")); // note that precursor count is hardcoded to 1
        tabCounter++;
        br.write(getCurrentTabSpace() + "<precursor msLevel=\"1\" spectrumRef=\"0\">" + System.getProperty("line.separator")); // note that precursor ms level is hardcoded to 1 with no corresponding spectrum
        tabCounter++;
        br.write(getCurrentTabSpace() + "<ionSelection>" + System.getProperty("line.separator"));
        tabCounter++;

        // precursor charge states
        for (int i = 0; i < spectrum.getPrecursor().getPossibleCharges().size(); i++) {
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000041\" name=\"ChargeState\" value=\""
                    + spectrum.getPrecursor().getPossibleCharges().get(i).value + "\" />" + System.getProperty("line.separator")); // note that charge is assumed to be positive...
        }

        // precursor m/z value
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000744\" name=\"selected ion m/z\" value=\""
                + spectrum.getPrecursor().getMz() + "\" />" + System.getProperty("line.separator"));

        // precursor intensity
        if (spectrum.getPrecursor().getIntensity() > 0) {
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000042\" name=\"peak intensity\" value=\""
                    + spectrum.getPrecursor().getIntensity() + "\" />" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ionSelection>" + System.getProperty("line.separator"));

        // activation
        br.write(getCurrentTabSpace() + "<activation />" + System.getProperty("line.separator")); // @TODO: always empty, but i think it's a required field?

        tabCounter--;
        br.write(getCurrentTabSpace() + "</precursor>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</precursorList>" + System.getProperty("line.separator"));

        if (matchExists) {
            br.write(getCurrentTabSpace() + "<comments>Identified</comments>" + System.getProperty("line.separator"));
        } else {
            br.write(getCurrentTabSpace() + "<comments>Not identified</comments>" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumDesc>" + System.getProperty("line.separator"));

        // get the m/z and intensity arrays
        double[][] arrays = spectrum.getMzAndIntensityAsArray();

        // write the m/z values
        br.write(getCurrentTabSpace() + "<mzArrayBinary>" + System.getProperty("line.separator"));
        tabCounter++;
        BinaryArrayImpl mzValues = new BinaryArrayImpl(arrays[0], BinaryArrayImpl.LITTLE_ENDIAN_LABEL);
        br.write(getCurrentTabSpace() + "<data precision=\"" + mzValues.getDataPrecision() + "\" endian=\"" + mzValues.getDataEndian()
                + "\" length=\"" + mzValues.getDataLength() + "\">" + mzValues.getBase64String() + "</data>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</mzArrayBinary>" + System.getProperty("line.separator"));

        // write the intensity values
        br.write(getCurrentTabSpace() + "<intenArrayBinary>" + System.getProperty("line.separator"));
        tabCounter++;
        BinaryArrayImpl intValues = new BinaryArrayImpl(arrays[1], BinaryArrayImpl.LITTLE_ENDIAN_LABEL);
        br.write(getCurrentTabSpace() + "<data precision=\"" + intValues.getDataPrecision() + "\" endian=\"" + intValues.getDataEndian()
                + "\" length=\"" + intValues.getDataLength() + "\">" + intValues.getBase64String() + "</data>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</intenArrayBinary>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrum>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the mzData description.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzDataDescription() throws IOException {

        // write the project description
        br.write(getCurrentTabSpace() + "<description>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<admin>" + System.getProperty("line.separator"));
        tabCounter++;

        // write the sample details
        writeSample();

        // write the contact details
        writeContact();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</admin>" + System.getProperty("line.separator"));

        // write the instrument details
        writeInstrument();

        // write the software details
        writeSoftware();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</description>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the software information.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeSoftware() throws IOException {

        br.write(getCurrentTabSpace() + "<dataProcessing>" + System.getProperty("line.separator"));
        tabCounter++;

        // write the software details
        br.write(getCurrentTabSpace() + "<software>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<name>" + "PeptideShaker" + "</name>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<version>" + peptideShakerGUI.getVersion() + "</version>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</software>" + System.getProperty("line.separator"));

        // write the processing details
        br.write(getCurrentTabSpace() + "<processingMethod>" + System.getProperty("line.separator"));
        tabCounter++;

        // fragment mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000161\" name=\"Fragment mass tolerance setting\" value=\""
                + peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy() + "\" />" + System.getProperty("line.separator"));

        // precursor mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000078\" name=\"Peptide mass tolerance setting\" value=\""
                + peptideShakerGUI.getSearchParameters().getPrecursorAccuracy() + "\" />" + System.getProperty("line.separator"));

        // allowed missed cleavages
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000162\" name=\"Allowed missed cleavages\" value=\""
                + peptideShakerGUI.getSearchParameters().getnMissedCleavages() + "\" />" + System.getProperty("line.separator"));

        // @TODO: add more settings??

        tabCounter--;
        br.write(getCurrentTabSpace() + "</processingMethod>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</dataProcessing>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the instrument description.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeInstrument() throws IOException {

        br.write(getCurrentTabSpace() + "<instrument>" + System.getProperty("line.separator"));
        tabCounter++;

        // write the instrument name
        br.write(getCurrentTabSpace() + "<instrumentName>" + instrument.getName() + "</instrumentName>" + System.getProperty("line.separator"));

        // write the source
        br.write(getCurrentTabSpace() + "<source>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(instrument.getSource());
        tabCounter--;
        br.write(getCurrentTabSpace() + "</source>" + System.getProperty("line.separator"));

        // write the analyzers
        br.write(getCurrentTabSpace() + "<analyzerList count=\"" + instrument.getCvTerms().size() + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        for (int i = 0; i < instrument.getCvTerms().size(); i++) {
            br.write(getCurrentTabSpace() + "<analyzer>" + System.getProperty("line.separator"));
            tabCounter++;
            writeCvTerm(instrument.getCvTerms().get(i));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</analyzer>" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</analyzerList>" + System.getProperty("line.separator"));


        // write the detector
        br.write(getCurrentTabSpace() + "<detector>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(instrument.getDetector());
        tabCounter--;
        br.write(getCurrentTabSpace() + "</detector>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</instrument>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the contact description.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeContact() throws IOException {

        br.write(getCurrentTabSpace() + "<contact>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<name>" + contact.getName() + "</name>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<institution>" + contact.getInstitution() + "</institution>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<contactInfo>" + contact.getEMail() + "</contactInfo>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</contact>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the sample description.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeSample() throws IOException {

        br.write(getCurrentTabSpace() + "<sampleName>" + sample.getName() + "</sampleName>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<sampleDescription>" + System.getProperty("line.separator"));
        tabCounter++;

        for (int i = 0; i < sample.getCvTerms().size(); i++) {
            writeCvTerm(sample.getCvTerms().get(i));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</sampleDescription>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the additional tags.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAdditionalTags() throws IOException {
        br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
        tabCounter++;

        // XML generation software
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000175\" name=\"XML generation software\" value=\"PeptideShaker v" + peptideShakerGUI.getVersion() + "\" />" + System.getProperty("line.separator"));

        // Project
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000097\" name=\"Project\" value=\"" + experimentProject + "\" />" + System.getProperty("line.separator"));

        // Experiment description
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000040\" name=\"Experiment description\" value=\"" + experimentDescription + "\" />" + System.getProperty("line.separator"));

        // Global peptide FDR
        //br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1001364\" name=\"pep:global FDR\" value=\"" + peptideShakerGUI. + "\" />" + System.getProperty("line.separator"));  // @TODO: add global peptide FDR?

        // @TODO: add global protein FDR??

        // search type
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1001083\" name=\"ms/ms search\" />" + System.getProperty("line.separator"));

        // @TODO: add more??

        tabCounter--;
        br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the title.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeTitle() throws IOException {
        br.write(getCurrentTabSpace() + "<Title>" + experimentTitle + "</Title>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the short label.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeShortLabel() throws IOException {
        br.write(getCurrentTabSpace() + "<ShortLabel>" + experimentLabel + "</ShortLabel>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the protocol description.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeProtocol() throws IOException {
        br.write(getCurrentTabSpace() + "<Protocol>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ProtocolName>" + protocol.getName() + "</ProtocolName>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<ProtocolSteps>" + System.getProperty("line.separator"));

        for (int i = 0; i < protocol.getCvTerms().size(); i++) {

            tabCounter++;

            br.write(getCurrentTabSpace() + "<StepDescription>" + System.getProperty("line.separator"));
            tabCounter++;
            writeCvTerm(protocol.getCvTerms().get(i));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</StepDescription>" + System.getProperty("line.separator"));

            tabCounter--;
        }

        br.write(getCurrentTabSpace() + "</ProtocolSteps>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Protocol>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the references.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeReferences() throws IOException {
        for (int i = 0; i < references.size(); i++) {

            Reference tempReference = references.get(i);

            br.write(getCurrentTabSpace() + "<Reference>" + System.getProperty("line.separator"));
            tabCounter++;

            br.write(getCurrentTabSpace() + "<RefLine>" + tempReference.getReference() + "</RefLine>" + System.getProperty("line.separator"));

            if (tempReference.getPmid() != null || tempReference.getDoi() != null) {
                br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
                tabCounter++;

                if (tempReference.getPmid() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000029\" name=\"PubMed\" value=\"" + tempReference.getPmid() + "\" />" + System.getProperty("line.separator"));
                }

                if (tempReference.getDoi() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000042\" name=\"DOI\" value=\"" + tempReference.getDoi() + "\" />" + System.getProperty("line.separator"));
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Reference>" + System.getProperty("line.separator"));
        }
    }

    /**
     * Writes the experiment collection start tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeExperimentCollectionStartTag() throws IOException {
        br.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>" + System.getProperty("line.separator"));
        br.write("<ExperimentCollection version=\"2.1\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Experiment>" + System.getProperty("line.separator"));
        tabCounter++;
    }

    /**
     * Writes the experiment collection end tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeExperimentCollectionEndTag() throws IOException {
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Experiment>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write("</ExperimentCollection>");
    }

    /**
     * Convenience method returning the tabs in the beginning of each line
     * depending on the tabCounter.
     *
     * @return the tabs in the beginning of each line as a string
     */
    private String getCurrentTabSpace() {

        String tabSpace = "";

        for (int i = 0; i < tabCounter; i++) {
            tabSpace += "\t";
        }

        return tabSpace;
    }

    /**
     * @return the experimentTitle
     */
    public String getExperimentTitle() {
        return experimentTitle;
    }

    /**
     * @param experimentTitle the experimentTitle to set
     */
    public void setExperimentTitle(String experimentTitle) {
        this.experimentTitle = experimentTitle;
    }

    /**
     * @return the experimentLabel
     */
    public String getExperimentLabel() {
        return experimentLabel;
    }

    /**
     * @param experimentLabel the experimentLabel to set
     */
    public void setExperimentLabel(String experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    /**
     * @return the experimentDescription
     */
    public String getExperimentDescription() {
        return experimentDescription;
    }

    /**
     * @param experimentDescription the experimentDescription to set
     */
    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }

    /**
     * @return the experimentProject
     */
    public String getExperimentProject() {
        return experimentProject;
    }

    /**
     * @param experimentProject the experimentProject to set
     */
    public void setExperimentProject(String experimentProject) {
        this.experimentProject = experimentProject;
    }

    /**
     * @return the references
     */
    public ArrayList<Reference> getReferences() {
        return references;
    }

    /**
     * @param references the references to set
     */
    public void setReferences(ArrayList<Reference> references) {
        this.references = references;
    }

    /**
     * @return the contact
     */
    public Contact getContact() {
        return contact;
    }

    /**
     * @param contact the contact to set
     */
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    /**
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * @param sample the sample to set
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * @return the protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the instrument
     */
    public Instrument getInstrument() {
        return instrument;
    }

    /**
     * @param instrument the instrument to set
     */
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    /**
     * Convenience method writing a cv Term.
     *
     * @param cvTerm the cvTerm
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvTerm(CvTerm cvTerm) throws IOException {

        br.write(getCurrentTabSpace() + "<cvParam "
                + "cvLabel=\"" + cvTerm.getOntology() + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + cvTerm.getName() + "\"");

        if (cvTerm.getValue() != null) {
            br.write(" value=\"" + cvTerm.getValue() + "\" />" + System.getProperty("line.separator"));
        } else {
            br.write(" />" + System.getProperty("line.separator"));
        }
    }
}
