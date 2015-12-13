package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.BinaryArrayImpl;
import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.pride.prideobjects.*;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.maps.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.maps.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.lang3.StringEscapeUtils;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to PRIDE XML.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class PrideXmlExport {

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
     * The references to include in the PRIDE XML file.
     */
    private ReferenceGroup referenceGroup;
    /**
     * The contact utilities PRIDE object.
     */
    private ContactGroup contactGroup;
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
     * The PTM to PRIDE map.
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
     * The waiting handler.
     */
    private WaitingHandler waitingHandler;
    /**
     * The identifications.
     */
    private Identification identification;
    /**
     * The project details.
     */
    private ProjectDetails projectDetails;
    /**
     * The PeptideShaker version.
     */
    private String peptideShakerVersion;
    /**
     * Information about the protocol.
     */
    private ShotgunProtocol shotgunProtocol;
    /**
     * the identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingPreferences spectrumCountingPreferences;
    /**
     * The identification feature generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The peptide spectrum annotator.
     */
    private PeptideSpectrumAnnotator spectrumAnnotator;

    /**
     * Constructor.
     *
     * @param peptideShakerVersion the PeptideShaker version
     * @param identification the identification object which can be used to
     * retrieve identification matches and parameters
     * @param projectDetails the project details
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param experimentTitle Title of the experiment
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param spectrumAnnotator the spectrum annotator to use
     * @param experimentLabel Label of the experiment
     * @param experimentDescription Description of the experiment
     * @param experimentProject project of the experiment
     * @param referenceGroup References for the experiment
     * @param contactGroup Contacts for the experiment
     * @param sample Samples in this experiment
     * @param protocol Protocol used in this experiment
     * @param instrument Instruments used in this experiment
     * @param outputFolder Output folder
     * @param fileName the file name without extension
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     *
     * @throws FileNotFoundException Exception thrown whenever a file was not
     * found
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a pride object
     */
    public PrideXmlExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters,
            SpectrumCountingPreferences spectrumCountingPreferences, IdentificationFeaturesGenerator identificationFeaturesGenerator, PeptideSpectrumAnnotator spectrumAnnotator,
            String experimentTitle, String experimentLabel, String experimentDescription, String experimentProject,
            ReferenceGroup referenceGroup, ContactGroup contactGroup, Sample sample, Protocol protocol, Instrument instrument,
            File outputFolder, String fileName, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.spectrumAnnotator = spectrumAnnotator;
        this.experimentTitle = experimentTitle;
        this.experimentLabel = experimentLabel;
        this.experimentDescription = experimentDescription;
        this.experimentProject = experimentProject;
        this.referenceGroup = referenceGroup;
        this.contactGroup = contactGroup;
        this.sample = sample;
        this.protocol = protocol;
        this.instrument = instrument;
        this.waitingHandler = waitingHandler;
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance();
        ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        r = new FileWriter(new File(outputFolder, fileName + ".xml"));
        br = new BufferedWriter(r);
    }

    /**
     * Creates the PRIDE XML file.
     *
     * @param progressDialog a dialog displaying progress to the user
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading an mzML file
     * @throws SQLException exception thrown whenever a problem occurred while
     * accessing a database
     * @throws ClassNotFoundException exception thrown whenever a problem
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred
     */
    public void createPrideXmlFile(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        // the experiment start tag
        writeExperimentCollectionStartTag();

        // the experiment title
        writeTitle();

        // the references, if any
        if (referenceGroup != null && referenceGroup.getReferences().size() > 0) {
            writeReferences();
        }

        // the short label
        writeShortLabel();

        // the protocol
        writeProtocol();

        if (waitingHandler.isRunCanceled()) {
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
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(100);
        progressDialog.setValue(0);

        if (waitingHandler.isRunCanceled()) {
            br.close();
            r.close();
            return;
        }

        // the mzData element
        writeMzData(progressDialog);

        if (waitingHandler.isRunCanceled()) {
            br.close();
            r.close();
            return;
        }

        // the PSMs
        writePsms(progressDialog);

        if (waitingHandler.isRunCanceled()) {
            br.close();
            r.close();
            return;
        }

        // the additional tags
        writeAdditionalTags();

        if (waitingHandler.isRunCanceled()) {
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
     * Writes all PSMs.
     *
     * @param progressDialog a progress dialog to display progress to the user
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading an mzML file
     * @throws SQLException exception thrown whenever a problem occurred while
     * accessing a database
     * @throws ClassNotFoundException exception thrown whenever a problem
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred
     */
    private void writePsms(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        PSParameter proteinProbabilities = new PSParameter();
        PSParameter peptideProbabilities = new PSParameter();
        PSParameter psmProbabilities = new PSParameter();

        progressDialog.setTitle("Creating PRIDE XML File. Please Wait...  (Part 2 of 2: Exporting IDs)");
        long increment = totalProgress / (2 * identification.getProteinIdentification().size());

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        ProteinMap proteinTargetDecoyMap = pSMaps.getProteinMap();
        PsmSpecificMap psmTargetDecoyMap = pSMaps.getPsmSpecificMap();
        PeptideSpecificMap peptideTargetDecoyMap = pSMaps.getPeptideSpecificMap();

        // get the list of algorithms used
        String searchEngineReport;
        ArrayList<Integer> seList = projectDetails.getIdentificationAlgorithms();
        Collections.sort(seList);
        searchEngineReport = Advocate.getAdvocate(seList.get(0)).getName();

        for (int i = 1; i < seList.size(); i++) {

            if (i == seList.size() - 1) {
                searchEngineReport += " and ";
            } else {
                searchEngineReport += ", ";
            }

            searchEngineReport += Advocate.getAdvocate(seList.get(i)).getName();
        }

        searchEngineReport += " post-processed by PeptideShaker v" + peptideShakerVersion;

        PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(new PSParameter());

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            if (waitingHandler.isRunCanceled()) {
                break;
            }

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            proteinProbabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, proteinProbabilities);
            double confidenceThreshold;

            br.write(getCurrentTabSpace() + "<GelFreeIdentification>" + System.getProperty("line.separator"));
            tabCounter++;

            // protein accession and database
            br.write(getCurrentTabSpace() + "<Accession>" + proteinMatch.getMainMatch() + "</Accession>" + System.getProperty("line.separator"));
            br.write(getCurrentTabSpace() + "<Database>" + sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDatabaseType() + "</Database>" + System.getProperty("line.separator"));

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, true, parameters, waitingHandler);

            while (peptideMatchesIterator.hasNext()) {

                if (waitingHandler.isRunCanceled()) {
                    break;
                }

                PeptideMatch peptideMatch = peptideMatchesIterator.next();
                String peptideKey = peptideMatch.getKey();
                peptideProbabilities = (PSParameter) identification.getPeptideMatchParameter(peptideKey, peptideProbabilities);

                PsmIterator psmIterator = identification.getPsmIterator(peptideMatch.getSpectrumMatchesKeys(), parameters, true, waitingHandler);

                while (psmIterator.hasNext()) {

                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }

                    SpectrumMatch spectrumMatch = psmIterator.next();
                    String spectrumKey = spectrumMatch.getKey();
                    psmProbabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psmProbabilities);
                    PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();
                    Peptide tempPeptide = bestAssumption.getPeptide();

                    // the peptide
                    br.write(getCurrentTabSpace() + "<PeptideItem>" + System.getProperty("line.separator"));
                    tabCounter++;

                    // peptide sequence
                    br.write(getCurrentTabSpace() + "<Sequence>" + tempPeptide.getSequence() + "</Sequence>" + System.getProperty("line.separator"));

                    // peptide start and end
                    String proteinAccession = proteinMatch.getMainMatch();
                    Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
                    String peptideSequence = Peptide.getSequence(peptideKey);

                    // get the start and end indexes
                    ArrayList<Integer> startIndexes = currentProtein.getPeptideStart(peptideSequence, identificationParameters.getSequenceMatchingPreferences());
                    int peptideStart = startIndexes.get(0); // only one start-end pair is allowed, so we just pick the first in the list
                    int peptideEnd = peptideStart + tempPeptide.getSequence().length() - 1;
                    br.write(getCurrentTabSpace() + "<Start>" + peptideStart + "</Start>" + System.getProperty("line.separator"));
                    br.write(getCurrentTabSpace() + "<End>" + peptideEnd + "</End>" + System.getProperty("line.separator"));

                    // spectrum index reference
                    br.write(getCurrentTabSpace() + "<SpectrumReference>" + spectrumIndexes.get(spectrumMatch.getKey()) + "</SpectrumReference>" + System.getProperty("line.separator"));

                    // modifications
                    writePtms(tempPeptide);

                    // fragment ions
                    writeFragmentIons(spectrumMatch);

                    // Get scores
                    HashMap<Integer, Double> eValues = new HashMap<Integer, Double>();
                    Double mascotScore = null, msAmandaScore = null;
                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                    for (int se : assumptions.keySet()) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> seMap = assumptions.get(se);
                        for (double eValue : seMap.keySet()) {
                            for (SpectrumIdentificationAssumption assumption : seMap.get(eValue)) {
                                if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(bestAssumption.getPeptide(), identificationParameters.getSequenceMatchingPreferences())) {
                                        if (!eValues.containsKey(se) || eValues.get(se) > eValue) {
                                            eValues.put(se, eValue);
                                            if (se == Advocate.mascot.getIndex()) {
                                                mascotScore = assumption.getRawScore();
                                            } else if (se == Advocate.msAmanda.getIndex()) {
                                                msAmandaScore = assumption.getRawScore();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PTM scores
                    ArrayList<String> modifications = new ArrayList<String>();

                    Peptide peptide = bestAssumption.getPeptide();
                    if (peptide.isModified()) {
                        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                            if (modificationMatch.isVariable()) {
                                if (!modifications.contains(modificationMatch.getTheoreticPtm())) {
                                    modifications.add(modificationMatch.getTheoreticPtm());
                                }
                            }
                        }
                    }

                    StringBuilder dScore = new StringBuilder();
                    Collections.sort(modifications);
                    PSPtmScores ptmScores = new PSPtmScores();

                    for (String mod : modifications) {

                        if (spectrumMatch.getUrParam(ptmScores) != null) {

                            if (dScore.length() > 0) {
                                dScore.append(", ");
                            }

                            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                            dScore.append(mod).append(" (");

                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                boolean firstSite = true;
                                ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                                Collections.sort(sites);
                                for (int site : sites) {
                                    if (firstSite) {
                                        firstSite = false;
                                    } else {
                                        dScore.append(", ");
                                    }
                                    dScore.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                                }
                            } else {
                                dScore.append("Not Scored");
                            }
                            dScore.append(")");
                        }
                    }

                    StringBuilder probabilisticScore = new StringBuilder();

                    if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {

                        for (String mod : modifications) {

                            if (spectrumMatch.getUrParam(ptmScores) != null) {

                                if (probabilisticScore.length() > 0) {
                                    probabilisticScore.append(", ");
                                }

                                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                                probabilisticScore.append(mod).append(" (");

                                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                                    boolean firstSite = true;
                                    ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                                    Collections.sort(sites);
                                    for (int site : sites) {
                                        if (firstSite) {
                                            firstSite = false;
                                        } else {
                                            probabilisticScore.append(", ");
                                        }
                                        probabilisticScore.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));
                                    }
                                } else {
                                    probabilisticScore.append("Not Scored");
                                }

                                probabilisticScore.append(")");
                            }
                        }
                    }

                    // @TODO: the line below uses the protein tree, which has to be rebuilt if not available...
                    ArrayList<String> peptideParentProteins = tempPeptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
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
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "Spectrum Title", "" + Spectrum.getSpectrumTitle(spectrumKey)));
                    br.write(getCurrentTabSpace() + "<userParam name=\"Protein Inference\" value=\"" + peptideProteins + "\" />" + System.getProperty("line.separator"));
                    br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Confidence\" value=\"" + Util.roundDouble(peptideProbabilities.getPeptideConfidence(), CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                    confidenceThreshold = peptideTargetDecoyMap.getTargetDecoyMap(peptideTargetDecoyMap.getCorrectedKey(peptideProbabilities.getSpecificMapKey())).getTargetDecoyResults().getConfidenceLimit();
                    br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                    MatchValidationLevel matchValidationLevel = peptideProbabilities.getMatchValidationLevel();
                    br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Validation\" value=\"" + matchValidationLevel + "\" />" + System.getProperty("line.separator"));
                    br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence\" value=\"" + Util.roundDouble(psmProbabilities.getPsmConfidence(), CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                    Integer charge = new Integer(psmProbabilities.getSpecificMapKey());
                    String fileName = Spectrum.getSpectrumFile(spectrumKey);
                    confidenceThreshold = psmTargetDecoyMap.getTargetDecoyMap(charge, fileName).getTargetDecoyResults().getConfidenceLimit();
                    br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                    matchValidationLevel = psmProbabilities.getMatchValidationLevel();
                    br.write(getCurrentTabSpace() + "<userParam name=\"PSM Validation\" value=\"" + matchValidationLevel + "\" />" + System.getProperty("line.separator"));

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1000041", "Charge State", "" + bestAssumption.getIdentificationCharge().value)); // @TODO: is 2+ etc supported?
                    //br.write(getCurrentTabSpace() + "<userParam name=\"Identified Charge\" value=\"" + bestAssumption.getIdentificationCharge().value + "\" />" + System.getProperty("line.separator"));

                    // search engine specific parameters
                    ArrayList<Integer> searchEngines = new ArrayList<Integer>(eValues.keySet());
                    Collections.sort(searchEngines);

                    // add the search engine e-values
                    ArrayList<Integer> algorithms = new ArrayList<Integer>(eValues.keySet());
                    Collections.sort(algorithms);
                    for (int tempAdvocate : algorithms) {
                        double eValue = eValues.get(tempAdvocate);
                        if (tempAdvocate == Advocate.msgf.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002052", "MS-GF:SpecEValue", Double.toString(eValue)));
                        } else if (tempAdvocate == Advocate.mascot.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1001172", "Mascot:expectation value", Double.toString(eValue)));
                        } else if (tempAdvocate == Advocate.omssa.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1001328", "OMSSA:evalue", Double.toString(eValue)));
                        } else if (tempAdvocate == Advocate.xtandem.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1001330", "X!Tandem:expect", Double.toString(eValue)));
                        } else if (tempAdvocate == Advocate.comet.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002257", "Comet:expectation value", Double.toString(eValue)));
                        } else if (tempAdvocate == Advocate.myriMatch.getIndex()) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1001589", "MyriMatch:MVH", Double.toString(eValue)));
                        } else {
                            br.write(getCurrentTabSpace() + "<userParam name=\"" + Advocate.getAdvocate(tempAdvocate).getName()
                                    + " e-value\" value=\"" + eValue + "\" />" + System.getProperty("line.separator"));
                        }
                    }

                    // add the additional search engine scores
                    if (mascotScore != null) {
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001171", "Mascot:score", "" + mascotScore));
                    }
                    if (msAmandaScore != null) {
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002319", "Amanda:AmandaScore", "" + msAmandaScore));
                    }

                    // @TODO: add additional scores for OMSSA and X!Tandem as well
                    // "MS:1001329", "OMSSA:pvalue"
                    // "PRIDE:0000182","X|Tandem Z score"
                    // "MS:1001331", "X!Tandem:hyperscore"
                    // PTM scoring
                    if (dScore.length() > 0) {
                        br.write(getCurrentTabSpace() + "<userParam name=\"PTM D-score\" value=\"" + dScore + "\" />" + System.getProperty("line.separator"));
                    }
                    if (ptmScoringPreferences.isProbabilitsticScoreCalculation() && probabilisticScore.length() > 0) {
                        br.write(getCurrentTabSpace() + "<userParam name=\"PTM "
                                + ptmScoringPreferences.getSelectedProbabilisticScore().getName()
                                + "\" value=\"" + probabilisticScore + "\" />" + System.getProperty("line.separator"));
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
            if (ProteinMatch.isDecoy(proteinKey)) {
                br.write(getCurrentTabSpace() + "<userParam name=\"Decoy\" value=\"1\" />" + System.getProperty("line.separator"));
            } else {
                br.write(getCurrentTabSpace() + "<userParam name=\"Decoy\" value=\"0\" />" + System.getProperty("line.separator"));
            }
            try {
                if (spectrumCountingPreferences.getSelectedMethod() == SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001905", "emPAI value", "" + identificationFeaturesGenerator.getSpectrumCounting(proteinKey)));
                } else {
                    br.write(getCurrentTabSpace() + "<userParam name=\"NSAF+\" value=\""
                            + identificationFeaturesGenerator.getSpectrumCounting(proteinKey) + "\" />" + System.getProperty("line.separator"));
                }
            } catch (Exception e) {
                e.printStackTrace(); // @TODO: add better error handling
            }
            MatchValidationLevel matchValidationLevel = psmProbabilities.getMatchValidationLevel();
            br.write(getCurrentTabSpace() + "<userParam name=\"Protein Validation\" value=\"" + matchValidationLevel + "\" />" + System.getProperty("line.separator"));
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
    }

    /**
     * Writes the fragment ions for a given spectrum match.
     *
     * @param spectrumMatch the spectrum match considered
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writeFragmentIons(SpectrumMatch spectrumMatch) throws IOException, MzMLUnmarshallerException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
        Peptide peptide = peptideAssumption.getPeptide();
        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        MSnSpectrum spectrum = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey()));
        SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
        ArrayList<IonMatch> matches = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, peptide);
        for (IonMatch annotation : matches) {
            writeFragmentIon(annotation);
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
        // @TODO: implement reporter ions! (required cv terms not found)
        CvTerm fragmentIonTerm = ionMatch.ion.getPrideCvTerm();

        if (fragmentIonTerm != null) {
            if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                    || ionMatch.ion.getType() == IonType.IMMONIUM_ION
                    || ionMatch.ion.getType() == IonType.PRECURSOR_ION
                    || ionMatch.ion.getType() == IonType.REPORTER_ION) {
                SearchParameters searchParameters = identificationParameters.getSearchParameters();
                br.write(getCurrentTabSpace() + "<FragmentIon>" + System.getProperty("line.separator"));
                tabCounter++;
                writeCvTerm(fragmentIonTerm);
                writeCvTerm(ionMatch.getMZPrideCvTerm());
                writeCvTerm(ionMatch.getIntensityPrideCvTerm());
                writeCvTerm(ionMatch.getIonMassErrorPrideCvTerm(searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
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

        if (peptide.isModified()) {
            for (int i = 0; i < peptide.getModificationMatches().size(); i++) {

                br.write(getCurrentTabSpace() + "<ModificationItem>" + System.getProperty("line.separator"));
                tabCounter++;

                ModificationMatch modMatch = peptide.getModificationMatches().get(i);
                String modName = modMatch.getTheoreticPtm();
                PTM ptm = ptmFactory.getPTM(modName);

                CvTerm cvTerm = ptm.getCvTerm();
                String cvTermName, ptmMass;

                if (cvTerm == null) {
                    cvTermName = modName;
                    ptmMass = "" + ptm.getRoundedMass();
                } else {
                    cvTermName = cvTerm.getName();
                    ptmMass = cvTerm.getValue();

                    // two extra tests to guard against problems with the cv terms, better to have a valid ptm than no ptm at all...
                    if (cvTermName == null) {
                        cvTermName = modName;
                    }
                    if (ptmMass == null) {
                        ptmMass = "" + ptm.getRoundedMass();
                    }
                }

                // get the modification location
                int modLocation = modMatch.getModificationSite();

                // have to handle terminal ptms separatly
                if (ptm.isNTerm()) {
                    modLocation = 0;
                } else if (ptm.isCTerm()) {
                    modLocation = peptide.getSequence().length() + 1;
                }

                br.write(getCurrentTabSpace() + "<ModLocation>" + modLocation + "</ModLocation>" + System.getProperty("line.separator"));

                if (cvTerm == null) {
                    br.write(getCurrentTabSpace() + "<ModAccession>" + cvTermName + "</ModAccession>" + System.getProperty("line.separator"));
                    br.write(getCurrentTabSpace() + "<ModDatabase>" + "PSI-MS" + "</ModDatabase>" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<ModAccession>" + cvTerm.getAccession() + "</ModAccession>" + System.getProperty("line.separator"));
                    br.write(getCurrentTabSpace() + "<ModDatabase>" + "UNIMOD" + "</ModDatabase>" + System.getProperty("line.separator"));
                }

                br.write(getCurrentTabSpace() + "<ModMonoDelta>" + ptmMass + "</ModMonoDelta>" + System.getProperty("line.separator"));

                br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
                tabCounter++;
                if (cvTerm == null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1001460\" name=\"" + cvTermName + "\" value=\"" + ptmMass + "\" />" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"UNIMOD\" accession=\"" + cvTerm.getAccession() + "\" name=\"" + cvTermName + "\" value=\"" + ptmMass + "\" />" + System.getProperty("line.separator"));
                }
                tabCounter--;
                br.write(getCurrentTabSpace() + "</additional>" + System.getProperty("line.separator"));

                tabCounter--;
                br.write(getCurrentTabSpace() + "</ModificationItem>" + System.getProperty("line.separator"));
            }
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

        // include the ontologies used, only MS and Unimod is included by default
        br.write(getCurrentTabSpace() + "<cvLookup cvLabel=\"MS\" fullName=\"PSI Mass Spectrometry Ontology\" version=\"1.0.0\" "
                + "address=\"http://psidev.sourceforge.net/ontology\" />" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvLookup cvLabel=\"UNIMOD\" fullName=\"UNIMOD CV for modifications\" version=\"1.2\" "
                + "address=\"http://www.unimod.org/obo/unimod.obo\" />" + System.getProperty("line.separator"));

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

        progressDialog.setPrimaryProgressCounterIndeterminate(false);

        for (String mgfFile : spectrumFactory.getMgfFileNames()) {

            if (waitingHandler.isRunCanceled()) {
                break;
            }

            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(mgfFile)) {

                if (waitingHandler.isRunCanceled()) {
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
     * @param spectrum the spectrum
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
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000041\" name=\"charge state\" value=\""
                    + spectrum.getPrecursor().getPossibleCharges().get(i).value + "\" />" + System.getProperty("line.separator")); // @TODO: is 2+ etc supported?
        }

        // precursor m/z value
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000744\" name=\"selected ion m/z\" value=\""
                + spectrum.getPrecursor().getMz() + "\" />" + System.getProperty("line.separator"));

        // precursor intensity
        if (spectrum.getPrecursor().getIntensity() > 0) {
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000042\" name=\"peak intensity\" value=\""
                    + spectrum.getPrecursor().getIntensity() + "\" />" + System.getProperty("line.separator"));
        }

        // precursor retention time
        if (spectrum.getPrecursor().hasRTWindow()) {

            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000894\" name=\"retention time\" value=\""
                    + spectrum.getPrecursor().getRtWindow()[0] + "\" />" + System.getProperty("line.separator"));

            // @TODO: figure out how to annotate retention time windows properly...
            //spectrum.getPrecursor().getRtWindow()[0] + "-" + spectrum.getPrecursor().getRtWindow()[1]
        } else if (spectrum.getPrecursor().getRt() != -1) {
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000894\" name=\"retention time\" value=\""
                    + spectrum.getPrecursor().getRt() + "\" />" + System.getProperty("line.separator"));
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
        writeContacts();

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
        br.write(getCurrentTabSpace() + "<version>" + peptideShakerVersion + "</version>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</software>" + System.getProperty("line.separator"));

        // write the processing details
        br.write(getCurrentTabSpace() + "<processingMethod>" + System.getProperty("line.separator"));
        tabCounter++;

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        // fragment mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000161\" name=\"Fragment mass tolerance setting\" value=\""
                + searchParameters.getFragmentIonAccuracy() + "\" />" + System.getProperty("line.separator"));

        // precursor mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000078\" name=\"Peptide mass tolerance setting\" value=\""
                + searchParameters.getPrecursorAccuracy() + "\" />" + System.getProperty("line.separator"));

        // allowed missed cleavages
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000162\" name=\"Allowed missed cleavages\" value=\""
                + searchParameters.getnMissedCleavages() + "\" />" + System.getProperty("line.separator"));

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
     * Writes the contact descriptions.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeContacts() throws IOException {
        for (int i = 0; i < contactGroup.getContacts().size(); i++) {
            br.write(getCurrentTabSpace() + "<contact>" + System.getProperty("line.separator"));
            tabCounter++;

            br.write(getCurrentTabSpace() + "<name>" + contactGroup.getContacts().get(i).getName() + "</name>" + System.getProperty("line.separator"));
            br.write(getCurrentTabSpace() + "<institution>" + contactGroup.getContacts().get(i).getInstitution() + "</institution>" + System.getProperty("line.separator"));
            br.write(getCurrentTabSpace() + "<contactInfo>" + contactGroup.getContacts().get(i).getEMail() + "</contactInfo>" + System.getProperty("line.separator"));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</contact>" + System.getProperty("line.separator"));
        }
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
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000175\" name=\"XML generation software\" "
                + "value=\"PeptideShaker v" + peptideShakerVersion + "\" />" + System.getProperty("line.separator"));

        // Project
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000097\" name=\"Project\" "
                + "value=\"" + experimentProject + "\" />" + System.getProperty("line.separator"));

        // Experiment description
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000040\" name=\"Experiment description\" "
                + "value=\"" + experimentDescription + "\" />" + System.getProperty("line.separator"));

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
        for (int i = 0; i < referenceGroup.getReferences().size(); i++) {

            Reference tempReference = referenceGroup.getReferences().get(i);

            br.write(getCurrentTabSpace() + "<Reference>" + System.getProperty("line.separator"));
            tabCounter++;

            br.write(getCurrentTabSpace() + "<RefLine>" + tempReference.getReference() + "</RefLine>" + System.getProperty("line.separator"));

            if (tempReference.getPmid() != null || tempReference.getDoi() != null) {
                br.write(getCurrentTabSpace() + "<additional>" + System.getProperty("line.separator"));
                tabCounter++;

                if (tempReference.getPmid() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000029\" name=\"PubMed\" value=\""
                            + tempReference.getPmid() + "\" />" + System.getProperty("line.separator"));
                }

                if (tempReference.getDoi() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000042\" name=\"DOI\" value=\""
                            + tempReference.getDoi() + "\" />" + System.getProperty("line.separator"));
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

        switch (tabCounter) {
            case 0:
                return "";
            case 1:
                return "\t";
            case 2:
                return "\t\t";
            case 3:
                return "\t\t\t";
            case 4:
                return "\t\t\t\t";
            case 5:
                return "\t\t\t\t\t";
            case 6:
                return "\t\t\t\t\t\t";
            case 7:
                return "\t\t\t\t\t\t\t";
            case 8:
                return "\t\t\t\t\t\t\t\t";
            case 9:
                return "\t\t\t\t\t\t\t\t\t";
            case 10:
                return "\t\t\t\t\t\t\t\t\t\t";
            case 11:
                return "\t\t\t\t\t\t\t\t\t\t\t";
            case 12:
                return "\t\t\t\t\t\t\t\t\t\t\t\t";
            default:
                return "";
        }
    }

    /**
     * Returns the experiment title.
     *
     * @return the experimentTitle
     */
    public String getExperimentTitle() {
        return experimentTitle;
    }

    /**
     * Sets the experiment title.
     *
     * @param experimentTitle the experimentTitle to set
     */
    public void setExperimentTitle(String experimentTitle) {
        this.experimentTitle = experimentTitle;
    }

    /**
     * Returns the experiment label.
     *
     * @return the experimentLabel
     */
    public String getExperimentLabel() {
        return experimentLabel;
    }

    /**
     * Sets the experiment label.
     *
     * @param experimentLabel the experimentLabel to set
     */
    public void setExperimentLabel(String experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    /**
     * Returns the experiment description.
     *
     * @return the experimentDescription
     */
    public String getExperimentDescription() {
        return experimentDescription;
    }

    /**
     * Set the experiment description.
     *
     * @param experimentDescription the experimentDescription to set
     */
    public void setExperimentDescription(String experimentDescription) {
        this.experimentDescription = experimentDescription;
    }

    /**
     * Returns the experiment project.
     *
     * @return the experimentProject
     */
    public String getExperimentProject() {
        return experimentProject;
    }

    /**
     * Set the experiment project.
     *
     * @param experimentProject the experimentProject to set
     */
    public void setExperimentProject(String experimentProject) {
        this.experimentProject = experimentProject;
    }

    /**
     * Returns the references group.
     *
     * @return the reference group
     */
    public ReferenceGroup getReferenceGroup() {
        return referenceGroup;
    }

    /**
     * Set the reference group.
     *
     * @param referenceGroup the references group to set
     */
    public void setReferenceGroup(ReferenceGroup referenceGroup) {
        this.referenceGroup = referenceGroup;
    }

    /**
     * Returns the contact group.
     *
     * @return the contact group
     */
    public ContactGroup getContactGroup() {
        return contactGroup;
    }

    /**
     * Sets the contact group.
     *
     * @param contactGroup the contact group to set
     */
    public void setContactGroup(ContactGroup contactGroup) {
        this.contactGroup = contactGroup;
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * Sets the sample.
     *
     * @param sample the sample to set
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * Returns the protocol.
     *
     * @return the protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol.
     *
     * @param protocol the protocol to set
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the instrument.
     *
     * @return the instrument
     */
    public Instrument getInstrument() {
        return instrument;
    }

    /**
     * Set the instrument.
     *
     * @param instrument the instrument to set
     */
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    /**
     * Convenience method writing a CV Term.
     *
     * @param cvTerm the cvTerm
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvTerm(CvTerm cvTerm) throws IOException {

        br.write(getCurrentTabSpace() + "<cvParam "
                + "cvLabel=\"" + Charset.forName("UTF-8").encode(cvTerm.getOntology()) + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + Charset.forName("UTF-8").encode(cvTerm.getName()) + "\"");

        if (cvTerm.getValue() != null) {
            br.write(" value=\"" + Charset.forName("UTF-8").encode(cvTerm.getValue()) + "\" />" + System.getProperty("line.separator"));
        } else {
            br.write(" />" + System.getProperty("line.separator"));
        }
    }
}
