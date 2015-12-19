package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ImmoniumIon;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory.ProteinIterator;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.maps.ProteinMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to mzIdentML.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class MzIdentMLExport {

    /**
     * The fileWriter.
     */
    private FileWriter r;
    /**
     * The buffered writer which will write the results in the desired file.
     */
    private BufferedWriter br;
    /**
     * Integer keeping track of the number of tabs to include at the beginning
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
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The number of decimals to use for the confidence values.
     */
    private final int CONFIDENCE_DECIMALS = 2;
    /**
     * D-score threshold.
     */
    private Double dScoreThreshold = 95.0; //@TODO: avoid this hard coded value!!
    /**
     * Write mzid 1.2? False, results in mzid 1.1.
     */
    private boolean mzidVersion_1_2 = false;
    /**
     * The PeptideShaker version.
     */
    private String peptideShakerVersion;
    /**
     * The identifications object.
     */
    private Identification identification;
    /**
     * The project details.
     */
    private ProjectDetails projectDetails;
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
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator;
    /**
     * The waiting handler.
     */
    private WaitingHandler waitingHandler;
    /**
     * The peptide evidence IDs.
     */
    private HashMap<String, String> pepEvidenceIds = new HashMap<String, String>();
    /**
     * The spectrum IDs.
     */
    private HashMap<String, String> spectrumIds = new HashMap<String, String>();
    /**
     * Information on the protocol.
     */
    private ShotgunProtocol shotgunProtocol;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * Map of PTM indexes: PTM mass to index.
     */
    private HashMap<Double, Integer> ptmIndexMap = new HashMap<Double, Integer>();
    /**
     * The match validation level a protein must have to be included in the
     * export.
     */
    private MatchValidationLevel proteinMatchValidationLevel;
    /**
     * The match validation level a peptide must have to be included in the
     * export
     */
    private MatchValidationLevel peptideMatchValidationLevel;
    /**
     * The match validation level a psm must have to be included in the export
     */
    private MatchValidationLevel psmMatchValidationLevel;

    /**
     * Constructor.
     *
     * @param peptideShakerVersion the PeptideShaker version
     * @param identification the identification object which can be used to
     * retrieve identification matches and parameters
     * @param projectDetails the project details
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param outputFile Output file
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, File outputFile, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException {
        this(peptideShakerVersion, identification, projectDetails, shotgunProtocol, identificationParameters, spectrumCountingPreferences, identificationFeaturesGenerator, outputFile, waitingHandler, MatchValidationLevel.none, MatchValidationLevel.none, MatchValidationLevel.none);
    }

    /**
     * Constructor.
     *
     * @param peptideShakerVersion the PeptideShaker version
     * @param identification the identification object which can be used to
     * retrieve identification matches and parameters
     * @param projectDetails the project details
     * @param shotgunProtocol information on the protocol
     * @param identificationParameters the identification parameters
     * @param spectrumCountingPreferences the spectrum counting preferences
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param outputFile Output file
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     * @param proteinMatchValidationLevel the match validation level a protein
     * must have to be included in the export
     * @param peptideMatchValidationLevel the match validation level a peptide
     * must have to be included in the export
     * @param psmMatchValidationLevel the match validation level a psm must have
     * to be included in the export
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, File outputFile, WaitingHandler waitingHandler,
            MatchValidationLevel proteinMatchValidationLevel, MatchValidationLevel peptideMatchValidationLevel, MatchValidationLevel psmMatchValidationLevel) throws IOException, ClassNotFoundException {
        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.waitingHandler = waitingHandler;
        this.proteinMatchValidationLevel = proteinMatchValidationLevel;
        this.peptideMatchValidationLevel = peptideMatchValidationLevel;
        this.psmMatchValidationLevel = psmMatchValidationLevel;
        this.peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
        r = new FileWriter(outputFile);
        br = new BufferedWriter(r);
    }

    /**
     * Creates the mzIdentML file.
     *
     * @param version12 if true, mzid 1.2 version information will be included
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while reading an mzML file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    public void createMzIdentMLFile(boolean version12) throws IOException, MzMLUnmarshallerException, ClassNotFoundException, InterruptedException, SQLException {

        mzidVersion_1_2 = version12;

        // @TODO: use the waiting handler more (especially for command line mode)
        // the mzIdentML start tag
        writeMzIdentMLStartTag();

        // write the cv list
        writeCvList();

        // write the AnalysisSoftwareList
        writeAnalysisSoftwareList();

        // write the Provider details
        writeProviderDetails();

        // write the AuditCollection details
        writeAuditCollection();

        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setMaxPrimaryProgressCounter(sequenceFactory.getNSequences() + identification.getSpectrumIdentificationSize() * 3 + identification.getProteinIdentification().size());

        // write the sequence collection
        writeSequenceCollection();

        // write the analyis collection
        writeAnalysisCollection();

        // write the analysis protocol
        writeAnalysisProtocol();

        // write the data collection
        writeDataCollection();

        if (waitingHandler.isRunCanceled()) {
            br.close();
            r.close();
            return;
        }

        // the experiment end tag
        writeMzIdentMLEndTag();

        br.close();
        r.close();
    }

    /**
     * Writes the CV list.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvList() throws IOException {

        br.write(getCurrentTabSpace() + "<cvList>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace()
                + "<cv id=\"PSI-MS\" "
                + "uri=\"http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo\" "
                + "version=\"3.73.0\" "
                + "fullName=\"PSI-MS\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv id=\"UNIMOD\" "
                + "uri=\"http://www.unimod.org/obo/unimod.obo\" "
                + "fullName=\"UNIMOD\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv id=\"UO\" "
                + "uri=\"http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo\" "
                + "fullName=\"UNIT-ONTOLOGY\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv id=\"PRIDE\" "
                + "uri=\"http://code.google.com/p/ebi-pride/source/browse/trunk/pride-core/schema/pride_cv.obo\" " // @TODO: will disappear at some point...
                + "fullName=\"PRIDE\"/>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</cvList>" + System.getProperty("line.separator"));
    }

    /**
     * Write the software list.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisSoftwareList() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisSoftwareList>" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: also add SearchGUI and/or search engines used?
        br.write(getCurrentTabSpace() + "<AnalysisSoftware "
                + "name=\"PeptideShaker\" "
                + "version=\"" + peptideShakerVersion + "\" "
                + "id=\"ID_software\" "
                + "uri=\"http://compomics.github.io/projects/peptide-shaker.html\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PS_DEV\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001267", "software vendor", "CompOmics"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Role>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ContactRole>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<SoftwareName>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1002458", "PeptideShaker", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SoftwareName>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Customizations>No customisations</Customizations>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftware>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftwareList>" + System.getProperty("line.separator"));
    }

    /**
     * Write the provider details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeProviderDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Provider id=\"PROVIDER\">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PROVIDER\">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001271", "researcher", null)); // @TODO: add user defined provider role?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Role>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ContactRole>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Provider>" + System.getProperty("line.separator"));
    }

    /**
     * Write the audit collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAuditCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AuditCollection>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Person "
                + "firstName=\"" + projectDetails.getContactFirstName() + "\" "
                + "lastName=\"" + projectDetails.getContactLastName() + "\" "
                + "id=\"PROVIDER\">"
                + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getContactAddress()));
        if (projectDetails.getContactUrl() != null && !projectDetails.getContactUrl().isEmpty()) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", projectDetails.getContactUrl()));
        }
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getContactEmail()));
        br.write(getCurrentTabSpace() + "<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Person>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"" + projectDetails.getOrganizationName() + "\" id=\"ORG_DOC_OWNER\">" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", projectDetails.getOrganizationName()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getOrganizationAddress()));
        if (projectDetails.getOrganizationUrl() != null && !projectDetails.getOrganizationUrl().isEmpty()) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", projectDetails.getOrganizationUrl()));
        }
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getOrganizationEmail()));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "PeptideShaker developers"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "Proteomics Unit, Building for Basic Biology, University of Bergen, Jonas Liesvei 91, N-5009 Bergen, Norway"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", "http://compomics.github.io/projects/peptide-shaker.html"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "peptide-shaker@googlegroups.com"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AuditCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the sequence collection.
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeSequenceCollection() throws IOException, InterruptedException, ClassNotFoundException, SQLException {

        br.write(getCurrentTabSpace() + "<SequenceCollection>" + System.getProperty("line.separator"));
        tabCounter++;

        // get the sequence database
        ProteinIterator iterator = sequenceFactory.getProteinIterator(false);

//        String dbType = Header.getDatabaseTypeAsString(Header.DatabaseType.Unknown); // @TODO: add database type as user or cv param?
//        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();
//        if (fastaIndex != null) {
//            dbType = Header.getDatabaseTypeAsString(fastaIndex.getDatabaseType());
//        }
        // iterate all the protein sequences
        while (iterator.hasNext()) {
            Protein currentProtein = iterator.getNextProtein();
            br.write(getCurrentTabSpace() + "<DBSequence id=\"" + currentProtein.getAccession() + "\" "
                    + "accession=\"" + currentProtein.getAccession() + "\" searchDatabase_ref=\"" + "SearchDB_1" + "\" >" + System.getProperty("line.separator"));
            tabCounter++;
            //br.write(getCurrentTabSpace() + "<Seq>" + currentProtein.getSequence() + "</Seq>" + System.getProperty("line.separator"));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001088", "protein description", sequenceFactory.getHeader(currentProtein.getAccession()).getDescription()));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</DBSequence>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        iterator.close();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getTheoreticPeptide();
            String peptideSequence = peptide.getSequence();

            br.write(getCurrentTabSpace() + "<Peptide id=\"" + peptideKey + "\">" + System.getProperty("line.separator"));
            tabCounter++;
            br.write(getCurrentTabSpace() + "<PeptideSequence>" + peptideSequence + "</PeptideSequence>" + System.getProperty("line.separator"));

            if (peptide.isModified()) {
                for (ModificationMatch modMatch : peptide.getModificationMatches()) {

                    PTM currentPtm = ptmFactory.getPTM(modMatch.getTheoreticPtm());
                    int ptmLocation = modMatch.getModificationSite();

                    if (currentPtm.isNTerm()) {
                        ptmLocation = 0;
                    } else if (currentPtm.isCTerm()) {
                        ptmLocation = peptideSequence.length() + 1;
                    }

                    br.write(getCurrentTabSpace() + "<Modification monoisotopicMassDelta=\"" + currentPtm.getRoundedMass() + "\" "
                            + "residues=\"" + peptideSequence.charAt(modMatch.getModificationSite() - 1) + "\" "
                            + "location=\"" + ptmLocation + "\" >" + System.getProperty("line.separator"));

                    CvTerm ptmCvTerm = currentPtm.getCvTerm();
                    if (ptmCvTerm != null) {
                        tabCounter++;
                        writeCvTerm(ptmCvTerm);
                        tabCounter--;
                    }

                    br.write(getCurrentTabSpace() + "</Modification>" + System.getProperty("line.separator"));
                }
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Peptide>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        int peptideEvidenceCounter = 0;

        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, null, false, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (bestPeptideAssumption != null) {

                    Peptide peptide = bestPeptideAssumption.getPeptide();

                    // get all the possible parent proteins
                    ArrayList<String> possibleProteins = peptide.getParentProteins(sequenceMatchingPreferences);

                    // iterate all the possible protein parents for each peptide
                    for (String tempProtein : possibleProteins) {

                        // get the start indexes and the surrounding 
                        HashMap<Integer, String[]> aaSurrounding = sequenceFactory.getProtein(tempProtein).getSurroundingAA(
                                peptide.getSequence(), 1, sequenceMatchingPreferences);

                        ArrayList<Integer> indexes = new ArrayList<Integer>();
                        ArrayList<String> before = new ArrayList<String>();
                        ArrayList<String> after = new ArrayList<String>();

                        if (aaSurrounding.size() == 1) {
                            for (int index : aaSurrounding.keySet()) {
                                indexes.add(index);
                                before.add(aaSurrounding.get(index)[0]);
                                after.add(aaSurrounding.get(index)[1]);
                            }
                        } else {
                            ArrayList<Integer> tempIndexes = new ArrayList<Integer>(aaSurrounding.keySet());
                            Collections.sort(tempIndexes);
                            for (int index : tempIndexes) {
                                indexes.add(index);
                                before.add(aaSurrounding.get(index)[0]);
                                after.add(aaSurrounding.get(index)[1]);
                            }
                        }

                        for (int i = 0; i < indexes.size(); i++) {
                            String aaBefore = "-";
                            String aaAfter = "-";

                            if (!before.get(i).isEmpty()) {
                                aaBefore = before.get(i);
                            }
                            if (!after.get(i).isEmpty()) {
                                aaAfter = after.get(i);
                            }

                            int peptideStart = indexes.get(i);
                            int peptideEnd = (indexes.get(i) + peptide.getSequence().length() - 1);

                            String peptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);
                            String pepEvidenceKey = tempProtein + "_" + peptideStart + "_" + peptideKey;
                            pepEvidenceIds.put(pepEvidenceKey, "PepEv_" + ++peptideEvidenceCounter);
                            String matchingPeptideKey = peptide.getMatchingKey(sequenceMatchingPreferences);

                            br.write(getCurrentTabSpace() + "<PeptideEvidence isDecoy=\"" + peptide.isDecoy(sequenceMatchingPreferences) + "\" "
                                    + "pre=\"" + aaBefore + "\" "
                                    + "post=\"" + aaAfter + "\" "
                                    + "start=\"" + peptideStart + "\" "
                                    + "end=\"" + peptideEnd + "\" "
                                    + "peptide_ref=\"" + matchingPeptideKey + "\" "
                                    + "dBSequence_ref=\"" + sequenceFactory.getProtein(tempProtein).getAccession() + "\" "
                                    + "id=\"" + pepEvidenceIds.get(pepEvidenceKey) + "\" "
                                    + "/>" + System.getProperty("line.separator"));
                        }
                    }

                    waitingHandler.increasePrimaryProgressCounter();

                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }
                }
            }

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SequenceCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the analysis collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisCollection>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentification "
                + "spectrumIdentificationList_ref=\"SIL_1\" "
                + "spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" "
                + "id=\"SpecIdent_1\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        // iterate the spectrum files and add the file name refs
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {
            br.write(getCurrentTabSpace() + "<InputSpectra spectraData_ref=\"" + mgfFileName + "\"/>" + System.getProperty("line.separator"));
        }

        br.write(getCurrentTabSpace() + "<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentification>" + System.getProperty("line.separator"));

        // add protein detection
        br.write(getCurrentTabSpace() + "<ProteinDetection " // @TODO: add activityDate? example: activityDate="2011-03-25T13:33:51
                + "proteinDetectionProtocol_ref=\"PeptideShaker_1\" "
                + "proteinDetectionList_ref=\"Protein_groups\" "
                + "id=\"PD_1\">"
                + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<InputSpectrumIdentifications spectrumIdentificationList_ref=\"SIL_1\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetection>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the analysis protocol.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisProtocol() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisProtocolCollection>" + System.getProperty("line.separator"));
        tabCounter++;

        // add spectrum identification protocol
        br.write(getCurrentTabSpace() + "<SpectrumIdentificationProtocol "
                + "analysisSoftware_ref=\"ID_software\" id=\"SearchProtocol_1\">" + System.getProperty("line.separator"));
        tabCounter++;

        // the search type
        br.write(getCurrentTabSpace() + "<SearchType>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001083", "ms-ms search", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchType>" + System.getProperty("line.separator"));

        // the search parameters
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        br.write(getCurrentTabSpace() + "<AdditionalSearchParams>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001211", "parent mass type mono", null));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001256", "fragment mass type mono", null));
        if (mzidVersion_1_2) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002492", "consensus scoring", null));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002490", "peptide-level scoring performed", null));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002497", "Group PSMs by distinct peptide sequence with taking modifications into account", null));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002489", "Modification localization scoring performed", null));
        }

        // @TODO: list all search parameters from the search engines used?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</AdditionalSearchParams>" + System.getProperty("line.separator"));

        // the modifications
        br.write(getCurrentTabSpace() + "<ModificationParams>" + System.getProperty("line.separator"));
        tabCounter++;

        // create the ptm index map
        for (String ptm : searchParameters.getPtmSettings().getAllModifications()) {
            PTM currentPtm = ptmFactory.getPTM(ptm);
            Double ptmMass = currentPtm.getMass();
            Integer index = ptmIndexMap.get(ptmMass);
            if (index == null) {
                ptmIndexMap.put(ptmMass, ptmIndexMap.size());
            }
        }

        // iterate and add the ptms
        for (String ptm : searchParameters.getPtmSettings().getAllModifications()) {

            PTM currentPtm = ptmFactory.getPTM(ptm);
            int ptmType = currentPtm.getType();
            Double ptmMass = currentPtm.getMass();

            String aminoAcidsAtTarget = "";
            if (ptmType == PTM.MODN
                    || ptmType == PTM.MODNP
                    || ptmType == PTM.MODC
                    || ptmType == PTM.MODCP) {
                aminoAcidsAtTarget = ".";
            } else {
                for (Character aa : currentPtm.getPattern().getAminoAcidsAtTarget()) {
                    aminoAcidsAtTarget += aa;
                }
            }

            br.write(getCurrentTabSpace() + "<SearchModification residues=\"" + aminoAcidsAtTarget + "\" massDelta=\"" + currentPtm.getRoundedMass()
                    + "\" fixedMod= \"" + searchParameters.getPtmSettings().getFixedModifications().contains(ptm) + "\" >" + System.getProperty("line.separator"));
            tabCounter++;

            // add modification specificity
            if (ptmType != PTM.MODAA && ptmType != PTM.MODMAX) {

                br.write(getCurrentTabSpace() + "<SpecificityRules>" + System.getProperty("line.separator"));
                tabCounter++;

                if (ptmType == PTM.MODN || ptmType == PTM.MODNAA) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002057", "modification specificity protein N-term", null));
                } else if (ptmType == PTM.MODNP || ptmType == PTM.MODNPAA) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001189", "modification specificity peptide N-term", null));
                } else if (ptmType == PTM.MODC || ptmType == PTM.MODCAA) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002058", "modification specificity protein C-term", null));
                } else if (ptmType == PTM.MODCP || ptmType == PTM.MODCPAA) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001190", "modification specificity peptide C-term", null));
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</SpecificityRules>" + System.getProperty("line.separator"));
            }

            // add the modification cv term
            CvTerm ptmCvTerm = currentPtm.getCvTerm();
            if (ptmCvTerm != null) {
                writeCvTerm(ptmCvTerm);
            } else {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1001460", "unknown modification", null));
            }

            // add modification type/index
            Integer ptmIndex = ptmIndexMap.get(ptmMass);
            if (ptmIndex == null) {
                throw new IllegalArgumentException("No index found for PTM " + currentPtm.getName() + " of mass " + ptmMass + ".");
            }
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002504", "modification index", ptmIndex.toString()));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SearchModification>" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ModificationParams>" + System.getProperty("line.separator"));

        // enzyme
        br.write(getCurrentTabSpace() + "<Enzymes independent=\"false\">" + System.getProperty("line.separator"));
        // note: if multiple enzymes are specified, independent is set to true if cleavage with different enzymes is performed independently
        tabCounter++;

        Enzyme enzyme = searchParameters.getEnzyme();
        br.write(getCurrentTabSpace() + "<Enzyme "
                + "missedCleavages=\"" + searchParameters.getnMissedCleavages() + "\" "
                + "semiSpecific=\"" + enzyme.isSemiSpecific() + "\" "
                //+ "cTermGain=\"OH\" " // Element formula gained at CTerm
                //+ "nTermGain=\"H\" " // Element formula gained at NTerm
                + "id=\"Enz1\" "
                + "name=\"" + enzyme.getName() + "\">"
                + System.getProperty("line.separator")); // @TODO: add <SiteRegexp><![CDATA[(?<=[KR])(?!P)]]></SiteRegexp>?
        tabCounter++;

        br.write(getCurrentTabSpace() + "<EnzymeName>" + System.getProperty("line.separator"));
        tabCounter++;
        CvTerm enzymeCvTerm = EnzymeFactory.getEnzymeCvTerm(enzyme);
        if (enzymeCvTerm != null) {
            writeCvTerm(enzymeCvTerm);
        } else {
            writeUserParam(enzyme.getName());
        }
        tabCounter--;
        br.write(getCurrentTabSpace() + "</EnzymeName>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Enzyme>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Enzymes>" + System.getProperty("line.separator"));

        // fragment tolerance
        br.write(getCurrentTabSpace() + "<FragmentTolerance>" + System.getProperty("line.separator"));
        tabCounter++;
        String fragmentIonToleranceUnit;
        switch (searchParameters.getFragmentAccuracyType()) {
            case DA:
                fragmentIonToleranceUnit = "dalton";
                break;
            case PPM:
                fragmentIonToleranceUnit = "parts per million";
                break;
            default:
                throw new UnsupportedOperationException("CV term not implemented for fragment accuracy in " + searchParameters.getFragmentAccuracyType() + ".");
        }
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001412\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + fragmentIonToleranceUnit + "\" "
                + "unitAccession=\"UO:0000221\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance plus value\" />"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + fragmentIonToleranceUnit + "\" "
                + "unitAccession=\"UO:0000221\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance minus value\" />"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentTolerance>" + System.getProperty("line.separator"));

        // precursor tolerance
        br.write(getCurrentTabSpace() + "<ParentTolerance>" + System.getProperty("line.separator"));
        tabCounter++;

        String precursorIonToleranceUnit;
        switch (searchParameters.getPrecursorAccuracyType()) {
            case DA:
                precursorIonToleranceUnit = "dalton";
                break;
            case PPM:
                precursorIonToleranceUnit = "parts per million";
                break;
            default:
                throw new UnsupportedOperationException("CV term not implemented for precursor accuracy in " + searchParameters.getFragmentAccuracyType() + ".");
        }
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001412\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + precursorIonToleranceUnit + "\" "
                + "unitAccession=\"UO:0000169\" "
                + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                + "name=\"search tolerance plus value\" />"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + precursorIonToleranceUnit + "\" "
                + "unitAccession=\"UO:0000169\" "
                + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                + "name=\"search tolerance minus value\" />"
                + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ParentTolerance>" + System.getProperty("line.separator"));

        // thresholds
        br.write(getCurrentTabSpace() + "<Threshold>" + System.getProperty("line.separator"));
        tabCounter++;

        boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();

        if (!targetDecoy) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));
        } else {
            // Initial global thresholds
            IdMatchValidationPreferences idMatchValidationPreferences = identificationParameters.getIdValidationPreferences();
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "distinct peptide-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPeptideFDR(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPsmFDR(), CONFIDENCE_DECIMALS))));

            PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
            if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002556", "Ascore threshold", ptmScoringPreferences.getProbabilisticScoreThreshold() + ""));
                } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002567", "phosphoRS score threshold", ptmScoringPreferences.getProbabilisticScoreThreshold() + ""));
                }
            }
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002557", "D-score threshold", dScoreThreshold.toString()));

            // @TODO: add peptide and psm level annotation
//            // peptideshaker maps
//            PSMaps psMaps = new PSMaps();
//            psMaps = (PSMaps) identification.getUrParam(psMaps);
//
//            // peptide level threshold
//            PeptideSpecificMap peptideSpecificMap = psMaps.getPeptideSpecificMap();
//            ArrayList<String> peptideGroupsKeys = peptideSpecificMap.getKeys();
//
//            for (String key : peptideGroupsKeys) { // @TODO: find a way of annotating all thresholds..?
//                TargetDecoyMap targetDecoyMap = peptideSpecificMap.getTargetDecoyMap(key);
//                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
//                double threshold = targetDecoyResults.getUserInput() / 100;
//                int thresholdType = targetDecoyResults.getInputType();
//                if (peptideGroupsKeys.size() > 1) {
//                    String peptideClass = PeptideSpecificMap.getKeyName(searchParameters.getModificationProfile(), key);
//                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002544", "PeptideShaker peptide confidence type", peptideClass)); // peptide confidence type
//                }
//                if (thresholdType == 0) {
//                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002463", "distinct peptide-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
//                } else if (targetDecoyResults.getInputType() == 1) {
//                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "distinct peptide-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
//                } else if (targetDecoyResults.getInputType() == 2) {
//                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002462", "distinct peptide-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
//                }
//            }
//
//            // psm level threshold
//            ArrayList<Integer> chargesWithFileSpecificity = new ArrayList<Integer>();
//            PsmSpecificMap psmSpecificMap = psMaps.getPsmSpecificMap();
//
//            for (Integer charge : psmSpecificMap.getPossibleCharges()) {
//                for (String file : psmSpecificMap.getFilesAtCharge(charge)) {
//                    if (!psmSpecificMap.isFileGrouped(charge, file)) { // @TODO: find a way of annotating all thresholds..?
//                        chargesWithFileSpecificity.add(charge);
//                        TargetDecoyMap targetDecoyMap = psMaps.getPsmSpecificMap().getTargetDecoyMap(charge, file);
//                        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
//                        double threshold = targetDecoyResults.getUserInput() / 100;
//                        int thresholdType = targetDecoyResults.getInputType();
//                        String psmClass = "Charge " + charge + " of file " + file; // @TODO: annotate class?
//                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002543", "PeptideShaker PSM confidence type", "???")); // psm confidence type
//                        if (thresholdType == 0) {
//                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002465", "PSM-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
//                        } else if (targetDecoyResults.getInputType() == 1) {
//                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
//                        } else if (targetDecoyResults.getInputType() == 2) {
//                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002464", "PSM-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
//                        }
//                    }
//                }
//            }
//            
//            //@TODO: set the PSM group label
//            for (int charge : psmSpecificMap.getChargesFromGroupedFiles()) {
//                int correctedCharge = psmSpecificMap.getCorrectedCharge(charge);
//                if (correctedCharge == charge) {
//                    TargetDecoyMap targetDecoyMap = psMaps.getPsmSpecificMap().getTargetDecoyMap(charge, null);
//                    TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
//                    double threshold = targetDecoyResults.getUserInput() / 100;
//                    int thresholdType = targetDecoyResults.getInputType();
//                    // @TODO: check the cv terms used!!!
//                    if (thresholdType == 0) {
//                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002465", "PSM-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
//                    } else if (targetDecoyResults.getInputType() == 1) {
//                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
//                    } else if (targetDecoyResults.getInputType() == 2) {
//                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002464", "PSM-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
//                    }
//                }
//            }
//
//            // @TODO: re-add psm ptm thresholds
//            PsmPTMMap psmPTMMap = psMaps.getPsmPTMMap();
//            if (psmPTMMap != null) { // backward compatibility: information only present in versions 0.28.2 and later
//                for (Double ptmMass : psmPTMMap.getModificationsScored()) {
//                    for (int mapKey : psmPTMMap.getKeys(ptmMass).keySet()) {
//                        TargetDecoyMap targetDecoyMap = psmPTMMap.getTargetDecoyMap(ptmMass, mapKey);
//                        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
//                        double threshold = targetDecoyResults.getUserInput() / 100;
//                        int thresholdType = targetDecoyResults.getInputType(); // For now only FDR is implemented but others will follow after my next transatlantic flight :)
//                        String ptmClass = "Modification of mass " + ptmMass;
//                        //@TODO: find cv terms
//                    }
//                }
//            }
            // @TODO: one for ptm scores, one per ptm per charge state per file
            // match quality thresholds 
            // @TODO: match quality thresholds?? some are per file...
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Threshold>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationProtocol>" + System.getProperty("line.separator"));

        // add ProteinDetectionProtocol
        br.write(getCurrentTabSpace() + "<ProteinDetectionProtocol "
                + "analysisSoftware_ref=\"ID_software\" id=\"PeptideShaker_1\">" + System.getProperty("line.separator"));
        tabCounter++;

//        br.write(getCurrentTabSpace() + "<AnalysisParams>" + System.getProperty("line.separator"));
//        tabCounter++;
        // @TODO: add cv terms? (children of MS:1001302)
//        tabCounter--;
//        br.write(getCurrentTabSpace() + "</AnalysisParams>" + System.getProperty("line.separator"));
        // protein level threshold
        br.write(getCurrentTabSpace() + "<Threshold>" + System.getProperty("line.separator"));
        tabCounter++;

        if (!targetDecoy) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));
        } else {
            PSMaps psMaps = new PSMaps();
            psMaps = (PSMaps) identification.getUrParam(psMaps);
            ProteinMap proteinMap = psMaps.getProteinMap();
            TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();

            double threshold = targetDecoyResults.getUserInput() / 100;
            int thresholdType = targetDecoyResults.getInputType();

            if (thresholdType == 0) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002461", "protein group-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
            } else if (targetDecoyResults.getInputType() == 1) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002369", "protein group-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
            } else if (targetDecoyResults.getInputType() == 2) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002460", "protein group-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
            }
        }
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Threshold>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionProtocol>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisProtocolCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the data collection.
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeDataCollection() throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        br.write(getCurrentTabSpace() + "<DataCollection>" + System.getProperty("line.separator"));
        tabCounter++;
        writeInputFileDetails();
        writeDataAnalysis();
        tabCounter--;
        br.write(getCurrentTabSpace() + "</DataCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the data analysis section.
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeDataAnalysis() throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        br.write(getCurrentTabSpace() + "<AnalysisData>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationList id=\"SIL_1\">" + System.getProperty("line.separator"));
        tabCounter++;

        writeFragmentationTable();

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(new PSParameter());
        int psmCount = 0;

        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFileName, parameters, true, waitingHandler);

            while (psmIterator.hasNext()) {

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                writeSpectrumIdentificationResult(spectrumKey, ++psmCount);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1002439", "final PSM list", null));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationList>" + System.getProperty("line.separator"));

        writeProteinDetectionList();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisData>" + System.getProperty("line.separator"));
    }

    /**
     * Write the protein groups.
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeProteinDetectionList() throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        br.write(getCurrentTabSpace() + "<ProteinDetectionList id=\"Protein_groups\">" + System.getProperty("line.separator"));
        tabCounter++;

        int groupCpt = 0;

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext()) {

            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinGroupKey = proteinMatch.getKey();

            String proteinGroupId = "PAG_" + groupCpt++;

            br.write(getCurrentTabSpace() + "<ProteinAmbiguityGroup id=\"" + proteinGroupId + "\">" + System.getProperty("line.separator"));
            tabCounter++;

            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinGroupKey, psParameter);

            String mainAccession = proteinMatch.getMainMatch();

            for (int j = 0; j < proteinMatch.getTheoreticProteinsAccessions().size(); j++) {

                String accession = proteinMatch.getTheoreticProteinsAccessions().get(j);

                br.write(getCurrentTabSpace() + "<ProteinDetectionHypothesis id=\"" + proteinGroupId + "_" + (j + 1) + "\" dBSequence_ref=\"" + accession
                        + "\" passThreshold=\"" + psParameter.getMatchValidationLevel().isValidated() + "\">" + System.getProperty("line.separator"));
                tabCounter++;

                ArrayList<String> peptideMatches = identification.getProteinMatch(proteinGroupKey).getPeptideMatchesKeys();
                PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(peptideMatches, null, false, null, waitingHandler);

                while (peptideMatchesIterator.hasNext()) {

                    PeptideMatch peptideMatch = peptideMatchesIterator.next();
                    String peptideKey = peptideMatch.getKey();
                    String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();

                    ArrayList<Integer> peptideStarts = sequenceFactory.getProtein(accession).getPeptideStart(
                            peptideSequence, identificationParameters.getSequenceMatchingPreferences());

                    for (int start : peptideStarts) {
                        String pepEvidenceKey = accession + "_" + start + "_" + peptideKey;
                        String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);

                        if (peptideEvidenceId != null) {

                            br.write(getCurrentTabSpace() + "<PeptideHypothesis peptideEvidence_ref=\"" + peptideEvidenceId + "\">" + System.getProperty("line.separator"));
                            tabCounter++;

                            for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                                br.write(getCurrentTabSpace() + "<SpectrumIdentificationItemRef spectrumIdentificationItem_ref=\""
                                        + spectrumIds.get(spectrumKey) + "\"/>" + System.getProperty("line.separator"));
                            }

                            tabCounter--;
                            br.write(getCurrentTabSpace() + "</PeptideHypothesis>" + System.getProperty("line.separator"));
                        } else {
                            throw new IllegalArgumentException("No peptide evidence id found for key '" + pepEvidenceKey + "'.");
                        }
                    }
                }

                // add main protein cv terms
                if (accession.equalsIgnoreCase(mainAccession)) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002403", "group representative", null));
                }

                writeCvTerm(new CvTerm("PSI-MS", "MS:1002401", "leading protein", null));

                // add protein coverage cv term - main protein only
                if (accession.equalsIgnoreCase(mainAccession)) {
                    Double validatedCoverage = identificationFeaturesGenerator.getValidatedSequenceCoverage(proteinGroupKey);
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001093", "sequence coverage", Double.toString(Util.roundDouble(validatedCoverage, CONFIDENCE_DECIMALS))));
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</ProteinDetectionHypothesis>" + System.getProperty("line.separator"));
            }

            // add protein group cv terms
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002470", "PeptideShaker protein group score", Double.toString(Util.roundDouble(psParameter.getProteinScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002471", "PeptideShaker protein group confidence", Double.toString(Util.roundDouble(psParameter.getProteinConfidence(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002545", "PeptideShaker protein confidence type", psParameter.getMatchValidationLevel().getName()));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002415", "protein group passes threshold", "" + psParameter.getMatchValidationLevel().isValidated()));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</ProteinAmbiguityGroup>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1002404", "count of identified proteins", "" + identificationFeaturesGenerator.getNValidatedProteins()));
        // @TODO: add children of MS:1001184 - search statistics? (date / time search performed, number of molecular hypothesis considered, search time taken)

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionList>" + System.getProperty("line.separator"));
    }

    /**
     * Write a spectrum identification result.
     *
     * @param psmKey the key of the PSM to write
     * @param psmIndex the index of the PSM
     *
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while reading an mzML file
     */
    private void writeSpectrumIdentificationResult(String psmKey, int psmIndex)
            throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(psmKey);
        String spectrumFileName = Spectrum.getSpectrumFile(psmKey);
        String spectrumIdentificationResultItemKey = "SIR_" + psmIndex;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationResult "
                + "spectraData_ref=\"" + spectrumFileName
                + "\" spectrumID=\"" + "index=" + spectrumFactory.getSpectrumIndex(spectrumTitle, spectrumFileName)
                + "\" id=\"" + spectrumIdentificationResultItemKey + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: iterate all assumptions and not just the best one?
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

        if (bestPeptideAssumption != null) {

            PSParameter psmParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter());
            int rank = 1; // @TODO: should not be hardcoded?
            String spectrumIdentificationItemKey = "SII_" + psmIndex + "_" + rank;
            spectrumIds.put(psmKey, spectrumIdentificationItemKey);

            String bestPeptideKey = bestPeptideAssumption.getPeptide().getMatchingKey(identificationParameters.getSequenceMatchingPreferences());

            br.write(getCurrentTabSpace() + "<SpectrumIdentificationItem "
                    + "passThreshold=\"" + psmParameter.getMatchValidationLevel().isValidated() + "\" "
                    + "rank=\"" + rank + "\" "
                    + "peptide_ref=\"" + bestPeptideKey + "\" "
                    + "calculatedMassToCharge=\"" + bestPeptideAssumption.getTheoreticMz() + "\" "
                    + "experimentalMassToCharge=\"" + spectrumFactory.getPrecursorMz(psmKey) + "\" "
                    + "chargeState=\"" + bestPeptideAssumption.getIdentificationCharge().value + "\" "
                    + "id=\"" + spectrumIdentificationItemKey + "\">" + System.getProperty("line.separator"));
            tabCounter++;

            // add the peptide evidence references
            // get all the possible parent proteins
            ArrayList<String> possibleProteins = bestPeptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
            String peptideSequence = bestPeptideAssumption.getPeptide().getSequence();

            // iterate all the possible protein parents for each peptide
            for (String tempProtein : possibleProteins) {

                // get the start indexes and the surrounding amino acids
                ArrayList<Integer> peptideStarts = sequenceFactory.getProtein(tempProtein).getPeptideStart(
                        peptideSequence, identificationParameters.getSequenceMatchingPreferences());

                for (int start : peptideStarts) {
                    String pepEvidenceKey = tempProtein + "_" + start + "_" + bestPeptideKey;
                    String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);
                    br.write(getCurrentTabSpace() + "<PeptideEvidenceRef peptideEvidence_ref=\"" + peptideEvidenceId + "\"/>" + System.getProperty("line.separator"));
                }
            }

            // add the fragment ion annotation
            AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);
            SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), bestPeptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
            ArrayList<IonMatch> matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, bestPeptideAssumption.getPeptide());

            // organize the fragment ions by ion type
            HashMap<String, HashMap<Integer, ArrayList<IonMatch>>> allFragmentIons = new HashMap<String, HashMap<Integer, ArrayList<IonMatch>>>();

            for (IonMatch ionMatch : matches) {

                if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                        || ionMatch.ion.getType() == IonType.IMMONIUM_ION) { // @TODO: add PRECURSOR_ION and REPORTER_ION
//                    || ionMatch.ion.getType() == IonType.PRECURSOR_ION
//                    || ionMatch.ion.getType() == IonType.REPORTER_ION) {

                    CvTerm fragmentIonTerm = ionMatch.ion.getPrideCvTerm(); // @TODO: replace by PSI-MS mappings... (children of MS:1001221)
                    Integer charge = ionMatch.charge.value;

                    if (fragmentIonTerm != null) {
                        if (!allFragmentIons.containsKey(fragmentIonTerm.getName())) {
                            allFragmentIons.put(fragmentIonTerm.getName(), new HashMap<Integer, ArrayList<IonMatch>>());
                        }
                        if (!allFragmentIons.get(fragmentIonTerm.getName()).containsKey(charge)) {
                            allFragmentIons.get(fragmentIonTerm.getName()).put(charge, new ArrayList<IonMatch>());
                        }
                        allFragmentIons.get(fragmentIonTerm.getName()).get(charge).add(ionMatch);
                    }
                }
            }

            if (!allFragmentIons.isEmpty()) {

                br.write(getCurrentTabSpace() + "<Fragmentation>" + System.getProperty("line.separator"));
                tabCounter++;

                // add the fragment ions
                Iterator<String> fragmentTypeIterator = allFragmentIons.keySet().iterator();

                while (fragmentTypeIterator.hasNext()) {

                    String fragmentType = fragmentTypeIterator.next();
                    Iterator<Integer> chargeTypeIterator = allFragmentIons.get(fragmentType).keySet().iterator();

                    while (chargeTypeIterator.hasNext()) {

                        Integer fragmentCharge = chargeTypeIterator.next();
                        ArrayList<IonMatch> ionMatches = allFragmentIons.get(fragmentType).get(fragmentCharge);
                        CvTerm fragmentIonTerm = ionMatches.get(0).ion.getPrideCvTerm();

                        String indexes = "";
                        String mzValues = "";
                        String intensityValues = "";
                        String errorValues = "";

                        // get the fragment ion details
                        for (IonMatch ionMatch : ionMatches) {

                            if (ionMatch.ion instanceof PeptideFragmentIon) {
                                indexes += ((PeptideFragmentIon) ionMatch.ion).getNumber() + " ";
                            } else if (ionMatch.ion instanceof ImmoniumIon) {
                                char residue = ImmoniumIon.getResidue(((ImmoniumIon) ionMatch.ion).getSubType());
                                char[] peptideAsArray = peptideSequence.toCharArray();
                                for (int i = 0; i < peptideAsArray.length; i++) {
                                    if (peptideAsArray[i] == residue) {
                                        indexes += (i + 1) + " ";
                                    }
                                }
                            } else {
                                // not yet implemented...
                            }

                            mzValues += ionMatch.peak.mz + " ";
                            intensityValues += ionMatch.peak.intensity + " ";
                            errorValues += ionMatch.getAbsoluteError() + " ";
                        }

                        br.write(getCurrentTabSpace() + "<IonType charge=\"" + fragmentCharge + "\" index=\"" + indexes.trim() + "\">" + System.getProperty("line.separator"));
                        tabCounter++;

                        br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_MZ\" values=\"" + mzValues.trim() + "\"/>" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_Int\" values=\"" + intensityValues.trim() + "\"/>" + System.getProperty("line.separator"));
                        br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_Error\" values=\"" + errorValues.trim() + "\"/>" + System.getProperty("line.separator"));

                        writeCvTerm(new CvTerm(fragmentIonTerm.getOntology(), fragmentIonTerm.getAccession(), fragmentIonTerm.getName(), null));

                        tabCounter--;
                        br.write(getCurrentTabSpace() + "</IonType>" + System.getProperty("line.separator"));
                    }
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</Fragmentation>" + System.getProperty("line.separator"));
            }

            // add peptide shaker score and confidence
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002466", "PeptideShaker PSM score", Double.toString(Util.roundDouble(psmParameter.getPsmScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002467", "PeptideShaker PSM confidence", Double.toString(Util.roundDouble(psmParameter.getPsmConfidence(), CONFIDENCE_DECIMALS))));

            if (mzidVersion_1_2) {

                PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
                PeptideMatch peptideMatch = identification.getPeptideMatch(bestPeptideKey);
                PSPtmScores psPtmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                if (psPtmScores != null) {

                    ArrayList<Integer> ptmIndexesCovered = new ArrayList<Integer>();

                    Peptide peptide = bestPeptideAssumption.getPeptide();
                    if (peptide.isModified()) {
                        for (ModificationMatch modMatch : peptide.getModificationMatches()) {

                            String ptmName = modMatch.getTheoreticPtm();
                            PTM currentPtm = ptmFactory.getPTM(ptmName);
                            Double ptmMass = currentPtm.getMass();
                            Integer ptmIndex = ptmIndexMap.get(ptmMass);
                            if (ptmIndex == null) {
                                throw new IllegalArgumentException("No index found for PTM " + ptmName + " of mass " + ptmMass + ".");
                            }

                            if (!ptmIndexesCovered.contains(ptmIndex)) {

                                ptmIndexesCovered.add(ptmIndex);
                                PtmScoring ptmScoring = psPtmScores.getPtmScoring(ptmName);

                                if (ptmScoring != null) {
                                    for (int site = 1; site <= peptideSequence.length(); site++) {
                                        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                                            double score = ptmScoring.getProbabilisticScore(site);
                                            if (score > 0) {
                                                String valid = "true";
                                                if (score < ptmScoringPreferences.getProbabilisticScoreThreshold()) {
                                                    valid = "false";
                                                }

                                                if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                                                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001985", "Ascore:Ascore", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                                                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001969", "phosphoRS score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                }
                                            }
                                        }

                                        double score = ptmScoring.getDeltaScore(site);
                                        if (score > 0) {
                                            String valid = "true";
                                            if (score < dScoreThreshold) {
                                                valid = "false";
                                            }
                                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002539", "D-score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PSParameter peptideParameter = (PSParameter) identification.getPeptideMatchParameter(bestPeptideKey, psmParameter);
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002469", "PeptideShaker peptide confidence", peptideParameter.getPeptideConfidence() + ""));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002468", "PeptideShaker peptide score", peptideParameter.getPeptideScore() + ""));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002500", "peptide passes threshold", peptideParameter.getMatchValidationLevel().isValidated() + ""));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002520", "peptide group ID", bestPeptideKey));

                psPtmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());

                if (psPtmScores != null) {

                    ArrayList<Integer> ptmIndexesCovered = new ArrayList<Integer>();

                    Peptide peptide = peptideMatch.getTheoreticPeptide();
                    if (peptide.isModified()) {
                        for (ModificationMatch modMatch : peptide.getModificationMatches()) {

                            String ptmName = modMatch.getTheoreticPtm();
                            PTM currentPtm = ptmFactory.getPTM(ptmName);
                            Double ptmMass = currentPtm.getMass();
                            Integer ptmIndex = ptmIndexMap.get(ptmMass);
                            if (ptmIndex == null) {
                                throw new IllegalArgumentException("No index found for PTM " + ptmName + " of mass " + ptmMass + ".");
                            }

                            if (!ptmIndexesCovered.contains(ptmIndex)) {

                                ptmIndexesCovered.add(ptmIndex);
                                PtmScoring ptmScoring = psPtmScores.getPtmScoring(modMatch.getTheoreticPtm());

                                if (ptmScoring != null) {
                                    for (int site = 1; site <= peptideSequence.length(); site++) {
                                        if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                                            double score = ptmScoring.getProbabilisticScore(site);
                                            if (score > 0) {
                                                String valid = "true";
                                                if (score < ptmScoringPreferences.getProbabilisticScoreThreshold()) {
                                                    valid = "false";
                                                }

                                                if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                                                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002554", "peptide:Ascore", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                                                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002553", "peptide:phosphoRS score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                }
                                            }
                                        }
                                        double score = ptmScoring.getDeltaScore(site);
                                        if (score > 0) {
                                            String valid = "true";
                                            if (score < dScoreThreshold) {
                                                valid = "false";
                                            }
                                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002556", "peptide:D-Score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                            //writeCvTerm(new CvTerm("PSI-MS", "MS:1002542", "PeptideShaker PTM confidence type", "???")); // @TODO: can be at both the psm and peptide level...
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // add the individual search engine results
            Double mascotScore = null, msAmandaScore = null;
            HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(psmKey);
            for (Integer tempAdvocate : assumptions.keySet()) {
                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMap = assumptions.get(tempAdvocate);
                for (double eValue : advocateMap.keySet()) {
                    for (SpectrumIdentificationAssumption currentAssumption : advocateMap.get(eValue)) {
                        if (currentAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) currentAssumption;
                            if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(
                                    bestPeptideAssumption.getPeptide(), identificationParameters.getSequenceMatchingPreferences())) {
                                Double currentMinEvalue = scores.get(tempAdvocate);
                                if (currentMinEvalue == null || eValue < currentMinEvalue) {
                                    scores.put(tempAdvocate, eValue);

                                    // save the special advocate scores
                                    if (tempAdvocate == Advocate.mascot.getIndex()) {
                                        mascotScore = peptideAssumption.getRawScore();
                                    } else if (tempAdvocate == Advocate.msAmanda.getIndex()) {
                                        msAmandaScore = peptideAssumption.getRawScore();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ArrayList<Integer> algorithms = new ArrayList<Integer>(scores.keySet());
            Collections.sort(algorithms);
            for (int tempAdvocate : algorithms) {
                double eValue = scores.get(tempAdvocate);
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
                    writeUserParam(Advocate.getAdvocate(tempAdvocate).getName() + " e-value", "" + eValue); // @TODO: add Tide if Tide CV term is added
                }
            }

            // add the additional search engine scores
            if (mascotScore != null) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1001171", "Mascot:score", "" + mascotScore));
            }
            if (msAmandaScore != null) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002319", "Amanda:AmandaScore", "" + msAmandaScore));
            }

            // add other cv and user params
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001117", "theoretical mass", String.valueOf(bestPeptideAssumption.getTheoreticMass())));

            // add validation level information
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002543", "PeptideShaker PSM confidence type", psmParameter.getMatchValidationLevel().getName()));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationItem>" + System.getProperty("line.separator"));

            // add the spectrum title
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "spectrum title", spectrumTitle));

            // add the precursor retention time
            Precursor precursor = spectrumFactory.getPrecursor(psmKey);
            if (precursor != null) {
                br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1000894\" name=\"retention time\" value=\"" + String.valueOf(precursor.getRt()) + "\" "
                        + "unitCvRef=\"UO\" unitAccession=\"UO:0000010\" unitName=\"seconds\"/>" + System.getProperty("line.separator"));
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationResult>" + System.getProperty("line.separator"));
        }
    }

    /**
     * Write the fragmentation table. (Note: all hard coded.)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeFragmentationTable() throws IOException {

        br.write(getCurrentTabSpace() + "<FragmentationTable>" + System.getProperty("line.separator"));
        tabCounter++;

        // mz
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_MZ\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam unitCvRef=\"PSI-MS\" accession=\"MS:1001225\" cvRef=\"PSI-MS\" unitName=\"m/z\" "
                + "unitAccession=\"MS:1000040\" name=\"product ion m/z\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + System.getProperty("line.separator"));

        // intensity
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_Int\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam accession=\"MS:1001226\" cvRef=\"PSI-MS\" "
                + "name=\"product ion intensity\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + System.getProperty("line.separator"));

        // mass error
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_Error\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam unitCvRef=\"PSI-MS\" accession=\"MS:1001227\" cvRef=\"PSI-MS\" "
                + "unitName=\"m/z\" unitAccession=\"MS:1000040\" name=\"product ion m/z error\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentationTable>" + System.getProperty("line.separator"));
    }

    /**
     * Write the input file details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeInputFileDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Inputs>" + System.getProperty("line.separator"));
        tabCounter++;

        int sourceFileCounter = 1;

        // add the search result files
        for (File idFile : projectDetails.getIdentificationFiles()) {

            // @TODO: add MS:1000568 - MD5?
//            FileInputStream fis = new FileInputStream(new File("foo"));
//            String md5 = DigestUtils.md5Hex(fis);
//            fis.close();
            br.write(getCurrentTabSpace() + "<SourceFile location=\"" + idFile.getAbsolutePath() + "\" id=\"SourceFile_" + sourceFileCounter++ + "\">" + System.getProperty("line.separator"));
            tabCounter++;
            br.write(getCurrentTabSpace() + "<FileFormat>" + System.getProperty("line.separator"));
            tabCounter++;

            String idFileName = Util.getFileName(idFile);
            HashMap<String, ArrayList<String>> algorithms = projectDetails.getIdentificationAlgorithmsForFile(idFileName);

            for (String algorithmName : algorithms.keySet()) {
                Advocate advocate = Advocate.getAdvocate(algorithmName);
                int advocateIndex = advocate.getIndex();
                if (advocateIndex == Advocate.mascot.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001199", "Mascot DAT format", null));
                } else if (advocateIndex == Advocate.xtandem.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001401", "xtandem xml file", null));
                } else if (advocateIndex == Advocate.omssa.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001400", "OMSSA xml format", null));
                } else if (advocateIndex == Advocate.msgf.getIndex() || advocateIndex == Advocate.myriMatch.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002073", "mzIdentML format", null));
                } else if (advocateIndex == Advocate.msAmanda.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002459", "MS Amanda csv format", null));
                } else if (advocateIndex == Advocate.comet.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001421", "pepXML format", null));
                } else if (advocateIndex == Advocate.tide.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1000914", "tab delimited text format", null));
                } else if (advocateIndex == Advocate.andromeda.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002576", "Andromeda result file", null));
                } else {
                    // no cv term available for the given advocate...
                }
            }

            // @TODO: add children of MS:1000561 - data file checksum type?
            tabCounter--;
            br.write(getCurrentTabSpace() + "</FileFormat>" + System.getProperty("line.separator"));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SourceFile>" + System.getProperty("line.separator"));
        }

        // add the database
        File database = identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase();
        br.write(getCurrentTabSpace() + "<SearchDatabase numDatabaseSequences=\"" + sequenceFactory.getNSequences()
                + "\" location=\"" + database.getAbsolutePath() + "\" "
                + "id=\"" + "SearchDB_1\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<FileFormat>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001348", "FASTA format", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FileFormat>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<DatabaseName>" + System.getProperty("line.separator"));
        tabCounter++;
        writeUserParam(database.getName()); // @TODO: add database type? children of MS:1001013 - database name???
        tabCounter--;
        br.write(getCurrentTabSpace() + "</DatabaseName>" + System.getProperty("line.separator"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001073", "database type amino acid", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchDatabase>" + System.getProperty("line.separator"));

        // add the spectra location
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {
            File mgfFile = projectDetails.getSpectrumFile(mgfFileName);

            br.write(getCurrentTabSpace() + "<SpectraData location=\"" + mgfFile.getAbsolutePath() + "\" id=\"" + mgfFileName
                    + "\" name=\"" + mgfFile.getName() + "\">" + System.getProperty("line.separator"));
            tabCounter++;

            br.write(getCurrentTabSpace() + "<FileFormat>" + System.getProperty("line.separator"));
            tabCounter++;
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001062", "Mascot MGF file", null));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</FileFormat>" + System.getProperty("line.separator"));

            br.write(getCurrentTabSpace() + "<SpectrumIDFormat>" + System.getProperty("line.separator"));
            tabCounter++;
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000774", "multiple peak list nativeID format", null));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIDFormat>" + System.getProperty("line.separator"));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectraData>" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Inputs>" + System.getProperty("line.separator"));
    }

    /**
     * Writes the mzIdentML start tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzIdentMLStartTag() throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator"));

        if (mzidVersion_1_2) {
            br.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.2 http://www.psidev.info/files/mzIdentML1.2.0.xsd\" "
                    + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.2\" version=\"1.2.0\" "
                    + "creationDate=\"" + df.format(new Date()) + "\">"
                    + System.getProperty("line.separator"));
        } else {
            // assumes version 1.1
            br.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.1 http://www.psidev.info/files/mzIdentML1.1.0.xsd\" "
                    + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\" version=\"1.1.0\" "
                    + "creationDate=\"" + df.format(new Date()) + "\">"
                    + System.getProperty("line.separator"));
        }

        tabCounter++;
    }

    /**
     * Writes the mzIdentML end tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzIdentMLEndTag() throws IOException {
        tabCounter--;
        br.write("</MzIdentML>");
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
     * Convenience method writing a CV Term.
     *
     * @param cvTerm the cvTerm
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvTerm(CvTerm cvTerm) throws IOException {

        br.write(getCurrentTabSpace() + "<cvParam "
                + "cvRef=\"" + Charset.forName("UTF-8").encode(cvTerm.getOntology()) + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + Charset.forName("UTF-8").encode(cvTerm.getName()) + "\"");

        if (cvTerm.getValue() != null) {
            br.write(" value=\"" + Charset.forName("UTF-8").encode(cvTerm.getValue()) + "\"/>" + System.getProperty("line.separator"));
        } else {
            br.write("/>" + System.getProperty("line.separator"));
        }
    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param userParamAsString the user parameter as a string
     */
    private void writeUserParam(String userParamAsString) throws IOException {
        br.write(getCurrentTabSpace() + "<userParam name=\"" + Charset.forName("UTF-8").encode(userParamAsString) + "\"/>" + System.getProperty("line.separator")); // @replace...
    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param name the name of the user parameter
     * @param value the value of the user parameter
     */
    private void writeUserParam(String name, String value) throws IOException {
        br.write(getCurrentTabSpace() + "<userParam name=\"" + Charset.forName("UTF-8").encode(name) + "\" value=\"" + Charset.forName("UTF-8").encode(value) + "\" />" + System.getProperty("line.separator"));
    }
}
