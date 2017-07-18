package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.Util;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ImmoniumIon;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.PrecursorIon;
import com.compomics.util.experiment.biology.ions.RelatedIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
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
import com.compomics.util.experiment.io.identifications.MzIdentMLVersion;
import com.compomics.util.preferences.DigestionPreferences;
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
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.math.MathException;

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
    private final int CONFIDENCE_DECIMALS = 4;
    /**
     * The maximum number of neutral losses a fragment ion can have in order to
     * be annotated.
     */
    private int maxNeutralLosses;
    /**
     * D-score threshold.
     */
    private Double dScoreThreshold = 95.0; //@TODO: avoid this hard coded value!!
    /**
     * The version of the mzIdentML file to use, 1.1 by default.
     */
    private MzIdentMLVersion mzIdentMLVersion = MzIdentMLVersion.v1_1;
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
     * The spectrum key to parent peptide key map.
     */
    private HashMap<String, String> spectrumKeyToPeptideKeyMap = new HashMap<String, String>();
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
     * export.
     */
    private MatchValidationLevel peptideMatchValidationLevel;
    /**
     * The match validation level a PSM must have to be included in the export.
     */
    private MatchValidationLevel psmMatchValidationLevel;
    /**
     * The line break type.
     */
    private String lineBreak = System.getProperty("line.separator");
    /**
     * If true, the fragment ions will be written to the mzid file.
     */
    private boolean writeFragmentIons = true;
    /**
     * If true, the protein sequences are included in the mzid file.
     */
    private boolean includeProteinSequences = false;

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
     * @param outputFile the output file
     * @param includeProteinSequences if true, the protein sequences are
     * included in the output
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, File outputFile, boolean includeProteinSequences, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException {
        this(peptideShakerVersion, identification, projectDetails, shotgunProtocol, identificationParameters, spectrumCountingPreferences, identificationFeaturesGenerator, outputFile, includeProteinSequences, waitingHandler, MatchValidationLevel.none, MatchValidationLevel.none, MatchValidationLevel.none);
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
     * @param outputFile the output file
     * @param includeProteinSequences if true, the protein sequences are
     * included in the output
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     * @param proteinMatchValidationLevel the match validation level a protein
     * must have to be included in the export
     * @param peptideMatchValidationLevel the match validation level a peptide
     * must have to be included in the export
     * @param psmMatchValidationLevel the match validation level a PSM must have
     * to be included in the export
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, SpectrumCountingPreferences spectrumCountingPreferences,
            IdentificationFeaturesGenerator identificationFeaturesGenerator, File outputFile, boolean includeProteinSequences, WaitingHandler waitingHandler,
            MatchValidationLevel proteinMatchValidationLevel, MatchValidationLevel peptideMatchValidationLevel, MatchValidationLevel psmMatchValidationLevel) throws IOException, ClassNotFoundException {

        if (outputFile.getParent() == null) {
            throw new FileNotFoundException("The file " + outputFile + " does not have a valid parent folder. Please make sure that the parent folder exists.");
        }

        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.includeProteinSequences = includeProteinSequences;
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
     * @param mzIdentMLVersion The version of mzIdentML to use
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an error
     * occurred while reading an mzML file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     * @throws org.apache.commons.math.MathException exception thrown whenever a
     * math error occurred.
     */
    public void createMzIdentMLFile(MzIdentMLVersion mzIdentMLVersion) throws IOException, MzMLUnmarshallerException, ClassNotFoundException, InterruptedException, SQLException, MathException {

        this.mzIdentMLVersion = mzIdentMLVersion;
        switch (mzIdentMLVersion) {
            case v1_1:
                maxNeutralLosses = 0;
                break;
            case v1_2:
                maxNeutralLosses = 1;
                break;
            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
        }

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
        waitingHandler.setMaxPrimaryProgressCounter(sequenceFactory.getNSequences()
                + identification.getPeptideIdentification().size() * 2
                + identification.getSpectrumIdentificationSize()
                + identification.getProteinIdentification().size());

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

        br.write(getCurrentTabSpace() + "<cvList>" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace()
                + "<cv id=\"PSI-MS\" "
                + "uri=\"https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo\" "
                + "fullName=\"PSI-MS\"/>" + lineBreak);

        br.write(getCurrentTabSpace()
                + "<cv id=\"UNIMOD\" "
                + "uri=\"http://www.unimod.org/obo/unimod.obo\" "
                + "fullName=\"UNIMOD\"/>" + lineBreak);

        br.write(getCurrentTabSpace()
                + "<cv id=\"UO\" "
                + "uri=\"https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo\" "
                + "fullName=\"UNIT-ONTOLOGY\"/>" + lineBreak);

        br.write(getCurrentTabSpace()
                + "<cv id=\"PRIDE\" "
                + "uri=\"https://github.com/PRIDE-Utilities/pride-ontology/blob/master/pride_cv.obo\" "
                + "fullName=\"PRIDE\"/>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</cvList>" + lineBreak);
    }

    /**
     * Write the software list.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisSoftwareList() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisSoftwareList>" + lineBreak);
        tabCounter++;

        // @TODO: also add SearchGUI and/or search engines used?
        br.write(getCurrentTabSpace() + "<AnalysisSoftware "
                + "name=\"PeptideShaker\" "
                + "version=\"" + peptideShakerVersion + "\" "
                + "id=\"ID_software\" "
                + "uri=\"http://compomics.github.io/projects/peptide-shaker.html\">"
                + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PS_DEV\">" + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Role>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001267", "software vendor", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Role>" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ContactRole>" + lineBreak);

        br.write(getCurrentTabSpace() + "<SoftwareName>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1002458", "PeptideShaker", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SoftwareName>" + lineBreak);

        br.write(getCurrentTabSpace() + "<Customizations>No customisations</Customizations>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftware>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftwareList>" + lineBreak);
    }

    /**
     * Write the provider details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeProviderDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Provider id=\"PROVIDER\">" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PROVIDER\">" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Role>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001271", "researcher", null)); // @TODO: add user defined provider role?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Role>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ContactRole>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Provider>" + lineBreak);
    }

    /**
     * Write the audit collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAuditCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AuditCollection>" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Person "
                + "firstName=\"" + projectDetails.getContactFirstName() + "\" "
                + "lastName=\"" + projectDetails.getContactLastName() + "\" "
                + "id=\"PROVIDER\">"
                + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getContactAddress()));
        if (projectDetails.getContactUrl() != null && !projectDetails.getContactUrl().isEmpty()) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", projectDetails.getContactUrl()));
        }
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getContactEmail()));
        br.write(getCurrentTabSpace() + "<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Person>" + lineBreak);

        br.write(getCurrentTabSpace() + "<Organization name=\"" + projectDetails.getOrganizationName() + "\" id=\"ORG_DOC_OWNER\">" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", projectDetails.getOrganizationName()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getOrganizationAddress()));
        if (projectDetails.getOrganizationUrl() != null && !projectDetails.getOrganizationUrl().isEmpty()) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", projectDetails.getOrganizationUrl()));
        }
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getOrganizationEmail()));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + lineBreak);

        br.write(getCurrentTabSpace() + "<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "PeptideShaker developers"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "Proteomics Unit, Building for Basic Biology, University of Bergen, Jonas Liesvei 91, N-5009 Bergen, Norway"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", "http://compomics.github.io/projects/peptide-shaker.html"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "peptide-shaker@googlegroups.com"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AuditCollection>" + lineBreak);
    }

    /**
     * Write the sequence collection.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeSequenceCollection() throws IOException, InterruptedException, ClassNotFoundException, SQLException {

        br.write(getCurrentTabSpace() + "<SequenceCollection>" + lineBreak);
        tabCounter++;

        // get the sequence database
        ProteinIterator iterator = sequenceFactory.getProteinIterator(false);

//        String dbType = Header.getDatabaseTypeAsString(Header.DatabaseType.Unknown); // @TODO: add database type as cv param? children of MS:1001013 (database name)
//        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();
//        if (fastaIndex != null) {
//            dbType = Header.getDatabaseTypeAsString(fastaIndex.getDatabaseType());
//        }
//
        // iterate all the protein sequences // @TODO: should be able to do this faster? we're just reading through the file once?
        while (iterator.hasNext()) {
            Protein currentProtein = iterator.getNextProtein();
            br.write(getCurrentTabSpace() + "<DBSequence id=\"" + currentProtein.getAccession() + "\" "
                    + "accession=\"" + currentProtein.getAccession() + "\" searchDatabase_ref=\"" + "SearchDB_1" + "\" >" + lineBreak);
            tabCounter++;

            if (includeProteinSequences) {
                br.write(getCurrentTabSpace() + "<Seq>" + currentProtein.getSequence() + "</Seq>" + lineBreak);
            }
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001088", "protein description", StringEscapeUtils.escapeHtml4(sequenceFactory.getHeader(currentProtein.getAccession()).getDescription())));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</DBSequence>" + lineBreak);

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        iterator.close();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        // set up the spectrum key to peptide key map
        spectrumKeyToPeptideKeyMap = new HashMap<String, String>();

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getTheoreticPeptide();
            String peptideSequence = peptide.getSequence();

            // store the spectrum to peptide mapping for later
            for (String spectrumMatchKey : peptideMatch.getSpectrumMatchesKeys()) {
                spectrumKeyToPeptideKeyMap.put(spectrumMatchKey, peptideKey);
            }

            br.write(getCurrentTabSpace() + "<Peptide id=\"" + peptideKey + "\">" + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<PeptideSequence>" + peptideSequence + "</PeptideSequence>" + lineBreak);

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
                            + "location=\"" + ptmLocation + "\" >" + lineBreak);

                    CvTerm ptmCvTerm = currentPtm.getCvTerm();
                    if (ptmCvTerm != null) {
                        tabCounter++;
                        writeCvTerm(ptmCvTerm, false);
                        tabCounter--;
                    }

                    br.write(getCurrentTabSpace() + "</Modification>" + lineBreak);
                }
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Peptide>" + lineBreak);

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        int peptideEvidenceCounter = 0;

        // re-iterate the peptides to get peptide to protein mapping
        peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getTheoreticPeptide();

            // get the possible parent proteins
            ArrayList<String> possibleProteins = peptide.getParentProteins(sequenceMatchingPreferences);

            // iterate the possible protein parents
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

                    String pepEvidenceKey = tempProtein + "_" + peptideStart + "_" + peptideKey;
                    pepEvidenceIds.put(pepEvidenceKey, "PepEv_" + ++peptideEvidenceCounter);

                    br.write(getCurrentTabSpace() + "<PeptideEvidence isDecoy=\"" + peptide.isDecoy(sequenceMatchingPreferences) + "\" "
                            + "pre=\"" + aaBefore + "\" "
                            + "post=\"" + aaAfter + "\" "
                            + "start=\"" + peptideStart + "\" "
                            + "end=\"" + peptideEnd + "\" "
                            + "peptide_ref=\"" + peptideKey + "\" "
                            + "dBSequence_ref=\"" + sequenceFactory.getProtein(tempProtein).getAccession() + "\" "
                            + "id=\"" + pepEvidenceIds.get(pepEvidenceKey) + "\" "
                            + "/>" + lineBreak);
                }
            }

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SequenceCollection>" + lineBreak);
    }

    /**
     * Write the analysis collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisCollection>" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentification "
                + "spectrumIdentificationList_ref=\"SIL_1\" "
                + "spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" "
                + "id=\"SpecIdent_1\">"
                + lineBreak);
        tabCounter++;

        // iterate the spectrum files and add the file name refs
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {
            br.write(getCurrentTabSpace() + "<InputSpectra spectraData_ref=\"" + mgfFileName + "\"/>" + lineBreak);
        }

        br.write(getCurrentTabSpace() + "<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentification>" + lineBreak);

        // add protein detection
        br.write(getCurrentTabSpace() + "<ProteinDetection " // @TODO: add activityDate? example: activityDate="2011-03-25T13:33:51
                + "proteinDetectionProtocol_ref=\"PeptideShaker_1\" "
                + "proteinDetectionList_ref=\"Protein_groups\" "
                + "id=\"PD_1\">"
                + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<InputSpectrumIdentifications spectrumIdentificationList_ref=\"SIL_1\"/>" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetection>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisCollection>" + lineBreak);
    }

    /**
     * Write the analysis protocol.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisProtocol() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisProtocolCollection>" + lineBreak);
        tabCounter++;

        // add spectrum identification protocol
        br.write(getCurrentTabSpace() + "<SpectrumIdentificationProtocol "
                + "analysisSoftware_ref=\"ID_software\" id=\"SearchProtocol_1\">" + lineBreak);
        tabCounter++;

        // the search type
        br.write(getCurrentTabSpace() + "<SearchType>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001083", "ms-ms search", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchType>" + lineBreak);

        // the search parameters
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        br.write(getCurrentTabSpace() + "<AdditionalSearchParams>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001211", "parent mass type mono", null));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001256", "fragment mass type mono", null));
        switch (mzIdentMLVersion) {
            case v1_1:
                break;
            case v1_2:
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002492", "consensus scoring", null));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002490", "peptide-level scoring", null));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002497", "group PSMs by sequence with modifications", null));
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002491", "modification localization scoring", null));
                break;
            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
        }

        // @TODO: list all search parameters from the search engines used?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</AdditionalSearchParams>" + lineBreak);

        // the modifications
        br.write(getCurrentTabSpace() + "<ModificationParams>" + lineBreak);
        tabCounter++;

        // create the ptm index map
        switch (mzIdentMLVersion) {
            case v1_1:
                break;
            case v1_2:
                for (String ptm : searchParameters.getPtmSettings().getAllModifications()) {
                    PTM currentPtm = ptmFactory.getPTM(ptm);
                    Double ptmMass = currentPtm.getMass();
                    Integer index = ptmIndexMap.get(ptmMass);
                    if (index == null) {
                        ptmIndexMap.put(ptmMass, ptmIndexMap.size());
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
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
                    + "\" fixedMod= \"" + searchParameters.getPtmSettings().getFixedModifications().contains(ptm) + "\" >" + lineBreak);
            tabCounter++;

            // add modification specificity
            if (ptmType != PTM.MODAA && ptmType != PTM.MODMAX) {

                br.write(getCurrentTabSpace() + "<SpecificityRules>" + lineBreak);
                tabCounter++;

                switch (ptmType) {
                    case PTM.MODN:
                    case PTM.MODNAA:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002057", "modification specificity protein N-term", null));
                        break;
                    case PTM.MODNP:
                    case PTM.MODNPAA:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001189", "modification specificity peptide N-term", null));
                        break;
                    case PTM.MODC:
                    case PTM.MODCAA:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002058", "modification specificity protein C-term", null));
                        break;
                    case PTM.MODCP:
                    case PTM.MODCPAA:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001190", "modification specificity peptide C-term", null));
                        break;
                    default:
                        break;
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</SpecificityRules>" + lineBreak);
            }

            // add the modification cv term
            CvTerm ptmCvTerm = currentPtm.getCvTerm();
            if (ptmCvTerm != null) {
                writeCvTerm(ptmCvTerm);
            } else {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1001460", "unknown modification", null));
            }

            // add modification type/index
            switch (mzIdentMLVersion) {
                case v1_1:
                    break;
                case v1_2:
                    Integer ptmIndex = ptmIndexMap.get(ptmMass);
                    if (ptmIndex == null) {
                        throw new IllegalArgumentException("No index found for PTM " + currentPtm.getName() + " of mass " + ptmMass + ".");
                    }
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002504", "modification index", ptmIndex.toString()));
                    break;
                default:
                    throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SearchModification>" + lineBreak);
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ModificationParams>" + lineBreak);

        // Digestion
        DigestionPreferences digestionPreferences = searchParameters.getDigestionPreferences();

        if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.unSpecific) {

            br.write(getCurrentTabSpace() + "<Enzymes independent=\"false\">" + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<Enzyme name=\"unspecific cleavage\">"
                    + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<EnzymeName>" + lineBreak);
            tabCounter++;
            CvTerm enzymeCvTerm = new CvTerm("PSI-MS", "MS:1001091", "unspecific cleavage", null);
            writeCvTerm(enzymeCvTerm);
            tabCounter--;
            br.write(getCurrentTabSpace() + "</EnzymeName>" + lineBreak);

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Enzyme>" + lineBreak);

        } else if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.wholeProtein) {

            br.write(getCurrentTabSpace() + "<Enzymes independent=\"false\">" + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<Enzyme name=\"NoEnzyme\">"
                    + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<EnzymeName>" + lineBreak);
            tabCounter++;
            CvTerm enzymeCvTerm = new CvTerm("PSI-MS", "MS:1001955", "NoEnzyme", null);
            writeCvTerm(enzymeCvTerm);
            tabCounter--;
            br.write(getCurrentTabSpace() + "</EnzymeName>" + lineBreak);

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Enzyme>" + lineBreak);

        } else {
            ArrayList<Enzyme> enzymes = digestionPreferences.getEnzymes();
            br.write(getCurrentTabSpace() + "<Enzymes independent=\"" + (enzymes.size() > 1) + "\">" + lineBreak);

            for (Enzyme enzyme : enzymes) {
                String enzymeName = enzyme.getName();
                br.write(getCurrentTabSpace() + "<Enzyme "
                        + "missedCleavages=\"" + digestionPreferences.getnMissedCleavages(enzymeName) + "\" "
                        + "semiSpecific=\"" + (digestionPreferences.getSpecificity(enzymeName) == DigestionPreferences.Specificity.semiSpecific) + "\" "
                        //+ "cTermGain=\"OH\" " // Element formula gained at CTerm
                        //+ "nTermGain=\"H\" " // Element formula gained at NTerm
                        + "id=\"Enz1\" "
                        + "name=\"" + enzyme.getName() + "\">"
                        + lineBreak); // @TODO: add <SiteRegexp><![CDATA[(?<=[KR])(?!P)]]></SiteRegexp>?
                tabCounter++;

                br.write(getCurrentTabSpace() + "<EnzymeName>" + lineBreak);
                tabCounter++;
                CvTerm enzymeCvTerm = enzyme.getCvTerm();
                if (enzymeCvTerm != null) {
                    writeCvTerm(enzymeCvTerm);
                } else {
                    writeUserParam(enzyme.getName());
                }
                tabCounter--;
                br.write(getCurrentTabSpace() + "</EnzymeName>" + lineBreak);

                tabCounter--;
                br.write(getCurrentTabSpace() + "</Enzyme>" + lineBreak);

                tabCounter--;
            }
        }
        br.write(getCurrentTabSpace() + "</Enzymes>" + lineBreak);

        // fragment tolerance
        br.write(getCurrentTabSpace() + "<FragmentTolerance>" + lineBreak);
        tabCounter++;
        String fragmentIonToleranceUnit;
        String unitAccession;
        switch (searchParameters.getFragmentAccuracyType()) {
            case DA:
                fragmentIonToleranceUnit = "dalton";
                unitAccession = "UO:0000221";
                break;
            case PPM:
                fragmentIonToleranceUnit = "parts per million";
                unitAccession = "UO:0000169";
                break;
            default:
                throw new UnsupportedOperationException("CV term not implemented for fragment accuracy in " + searchParameters.getFragmentAccuracyType() + ".");
        }
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001412\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + fragmentIonToleranceUnit + "\" "
                + "unitAccession=\"" + unitAccession + "\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance plus value\" />"
                + lineBreak);
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + fragmentIonToleranceUnit + "\" "
                + "unitAccession=\"" + unitAccession + "\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance minus value\" />"
                + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentTolerance>" + lineBreak);

        // precursor tolerance
        br.write(getCurrentTabSpace() + "<ParentTolerance>" + lineBreak);
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
                + lineBreak);
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"" + precursorIonToleranceUnit + "\" "
                + "unitAccession=\"UO:0000169\" "
                + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                + "name=\"search tolerance minus value\" />"
                + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ParentTolerance>" + lineBreak);

        // thresholds
        br.write(getCurrentTabSpace() + "<Threshold>" + lineBreak);
        tabCounter++;

        boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();

        if (!targetDecoy) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));
        } else {
            // Initial global thresholds
            IdMatchValidationPreferences idMatchValidationPreferences = identificationParameters.getIdValidationPreferences();
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "peptide sequence-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPeptideFDR(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPsmFDR(), CONFIDENCE_DECIMALS))));

            PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
            if (ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
                if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.AScore) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002556", "Ascore threshold", ptmScoringPreferences.getProbabilisticScoreThreshold() + ""));
                } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002567", "phosphoRS score threshold", ptmScoringPreferences.getProbabilisticScoreThreshold() + ""));
                }
            }
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002557", "D-Score threshold", dScoreThreshold.toString()));

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
//                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "peptide sequence-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
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
        br.write(getCurrentTabSpace() + "</Threshold>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationProtocol>" + lineBreak);

        // add ProteinDetectionProtocol
        br.write(getCurrentTabSpace() + "<ProteinDetectionProtocol "
                + "analysisSoftware_ref=\"ID_software\" id=\"PeptideShaker_1\">" + lineBreak);
        tabCounter++;

//        br.write(getCurrentTabSpace() + "<AnalysisParams>" + lineBreak);
//        tabCounter++;
        // @TODO: add cv terms? (children of MS:1001302)
//        tabCounter--;
//        br.write(getCurrentTabSpace() + "</AnalysisParams>" + lineBreak);
        // protein level threshold
        br.write(getCurrentTabSpace() + "<Threshold>" + lineBreak);
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
        br.write(getCurrentTabSpace() + "</Threshold>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionProtocol>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisProtocolCollection>" + lineBreak);
    }

    /**
     * Write the data collection.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeDataCollection() throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        br.write(getCurrentTabSpace() + "<DataCollection>" + lineBreak);
        tabCounter++;
        writeInputFileDetails();
        writeDataAnalysis();
        tabCounter--;
        br.write(getCurrentTabSpace() + "</DataCollection>" + lineBreak);
    }

    /**
     * Write the data analysis section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeDataAnalysis() throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        br.write(getCurrentTabSpace() + "<AnalysisData>" + lineBreak);
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationList id=\"SIL_1\">" + lineBreak);
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

        //writeCvTerm(new CvTerm("PSI-MS", "MS:1002439", "final PSM list", null)); // @TODO: add children of MS:1001184 (search statistics)?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationList>" + lineBreak);

        writeProteinDetectionList();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisData>" + lineBreak);
    }

    /**
     * Write the protein groups.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while writing the export
     * @throws SQLException exception thrown whenever an error occurred while
     * interacting with the database
     */
    private void writeProteinDetectionList() throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        br.write(getCurrentTabSpace() + "<ProteinDetectionList id=\"Protein_groups\">" + lineBreak);
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

            br.write(getCurrentTabSpace() + "<ProteinAmbiguityGroup id=\"" + proteinGroupId + "\">" + lineBreak);
            tabCounter++;

            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinGroupKey, psParameter);

            String mainAccession = proteinMatch.getMainMatch();

            for (int j = 0; j < proteinMatch.getTheoreticProteinsAccessions().size(); j++) {

                String accession = proteinMatch.getTheoreticProteinsAccessions().get(j);

                br.write(getCurrentTabSpace() + "<ProteinDetectionHypothesis id=\"" + proteinGroupId + "_" + (j + 1) + "\" dBSequence_ref=\"" + accession
                        + "\" passThreshold=\"" + psParameter.getMatchValidationLevel().isValidated() + "\">" + lineBreak);
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

                            br.write(getCurrentTabSpace() + "<PeptideHypothesis peptideEvidence_ref=\"" + peptideEvidenceId + "\">" + lineBreak);
                            tabCounter++;

                            for (String spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {
                                br.write(getCurrentTabSpace() + "<SpectrumIdentificationItemRef spectrumIdentificationItem_ref=\""
                                        + spectrumIds.get(spectrumKey) + "\"/>" + lineBreak);
                            }

                            tabCounter--;
                            br.write(getCurrentTabSpace() + "</PeptideHypothesis>" + lineBreak);
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
                br.write(getCurrentTabSpace() + "</ProteinDetectionHypothesis>" + lineBreak);
            }

            // add protein group cv terms
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002470", "PeptideShaker protein group score", Double.toString(Util.roundDouble(psParameter.getProteinScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002471", "PeptideShaker protein group confidence", Double.toString(Util.roundDouble(psParameter.getProteinConfidence(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002542", "PeptideShaker protein confidence type", psParameter.getMatchValidationLevel().getName()));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002415", "protein group passes threshold", "" + psParameter.getMatchValidationLevel().isValidated()));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</ProteinAmbiguityGroup>" + lineBreak);

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1002404", "count of identified proteins", "" + identificationFeaturesGenerator.getNValidatedProteins()));
        // @TODO: add children of MS:1001184 - search statistics? (date / time search performed, number of molecular hypothesis considered, search time taken)

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionList>" + lineBreak);
    }

    /**
     * Write a spectrum identification result.
     *
     * @param psmKey the key of the PSM to write
     * @param psmIndex the index of the PSM
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
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
                + "\" id=\"" + spectrumIdentificationResultItemKey + "\">" + lineBreak);
        tabCounter++;

        // @TODO: iterate all assumptions and not just the best one?
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

        if (bestPeptideAssumption != null) {

            PSParameter psmParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter());
            int rank = 1; // @TODO: should not be hardcoded?
            String spectrumIdentificationItemKey = "SII_" + psmIndex + "_" + rank;
            spectrumIds.put(psmKey, spectrumIdentificationItemKey);

            //String bestPeptideKey = bestPeptideAssumption.getPeptide().getMatchingKey(identificationParameters.getSequenceMatchingPreferences());
            String peptideKey = spectrumKeyToPeptideKeyMap.get(psmKey);

            br.write(getCurrentTabSpace() + "<SpectrumIdentificationItem "
                    + "passThreshold=\"" + psmParameter.getMatchValidationLevel().isValidated() + "\" "
                    + "rank=\"" + rank + "\" "
                    + "peptide_ref=\"" + peptideKey + "\" "
                    + "calculatedMassToCharge=\"" + bestPeptideAssumption.getTheoreticMz() + "\" "
                    + "experimentalMassToCharge=\"" + spectrumFactory.getPrecursorMz(psmKey) + "\" "
                    + "chargeState=\"" + bestPeptideAssumption.getIdentificationCharge().value + "\" "
                    + "id=\"" + spectrumIdentificationItemKey + "\">" + lineBreak);
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
                    String pepEvidenceKey = tempProtein + "_" + start + "_" + peptideKey;
                    String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);
                    br.write(getCurrentTabSpace() + "<PeptideEvidenceRef peptideEvidence_ref=\"" + peptideEvidenceId + "\"/>" + lineBreak);
                }
            }

            // add the fragment ions detected
            if (writeFragmentIons) {

                // add the fragment ion annotation
                AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);
                SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrum.getSpectrumKey(), bestPeptideAssumption,
                        identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                ArrayList<IonMatch> matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, (MSnSpectrum) spectrum, bestPeptideAssumption.getPeptide());

                // organize the fragment ions by ion type
                HashMap<String, HashMap<Integer, ArrayList<IonMatch>>> allFragmentIons = new HashMap<String, HashMap<Integer, ArrayList<IonMatch>>>();

                for (IonMatch ionMatch : matches) {

                    if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                            || ionMatch.ion.getType() == IonType.IMMONIUM_ION
                            || ionMatch.ion.getType() == IonType.PRECURSOR_ION
                            || ionMatch.ion.getType() == IonType.REPORTER_ION
                            || ionMatch.ion.getType() == IonType.RELATED_ION) { // @TODO: what about tag fragment ion?

                        CvTerm fragmentIonCvTerm = ionMatch.ion.getPsiMsCvTerm();
                        Integer charge = ionMatch.charge;

                        // check if there is less than the maximum number of allowed neutral losses
                        boolean neutralLossesTestPassed = true;
                        if (ionMatch.ion.hasNeutralLosses()) {
                            neutralLossesTestPassed = ionMatch.ion.getNeutralLosses().length <= maxNeutralLosses;
                        }

                        // only include ions with cv terms
                        if (fragmentIonCvTerm != null && neutralLossesTestPassed) {

                            String fragmentIonName = ionMatch.ion.getName();

                            if (!allFragmentIons.containsKey(fragmentIonName)) {
                                allFragmentIons.put(fragmentIonName, new HashMap<Integer, ArrayList<IonMatch>>());
                            }
                            if (!allFragmentIons.get(fragmentIonName).containsKey(charge)) {
                                allFragmentIons.get(fragmentIonName).put(charge, new ArrayList<IonMatch>());
                            }

                            allFragmentIons.get(fragmentIonName).get(charge).add(ionMatch);
                        }
                    }
                }

                if (!allFragmentIons.isEmpty()) {

                    br.write(getCurrentTabSpace() + "<Fragmentation>" + lineBreak);
                    tabCounter++;

                    // add the fragment ions
                    Iterator<String> fragmentTypeIterator = allFragmentIons.keySet().iterator();

                    while (fragmentTypeIterator.hasNext()) {

                        String fragmentType = fragmentTypeIterator.next();
                        Iterator<Integer> chargeTypeIterator = allFragmentIons.get(fragmentType).keySet().iterator();

                        while (chargeTypeIterator.hasNext()) {

                            Integer fragmentCharge = chargeTypeIterator.next();
                            ArrayList<IonMatch> ionMatches = allFragmentIons.get(fragmentType).get(fragmentCharge);
                            Ion currentIon = ionMatches.get(0).ion;
                            CvTerm fragmentIonCvTerm = currentIon.getPsiMsCvTerm();

                            String indexes = "";
                            String mzValues = "";
                            String intensityValues = "";
                            String errorValues = "";

                            // get the fragment ion details
                            for (IonMatch ionMatch : ionMatches) {

                                if (ionMatch.ion instanceof PeptideFragmentIon) {
                                    indexes += ((PeptideFragmentIon) ionMatch.ion).getNumber() + " ";
                                } else if (ionMatch.ion instanceof ImmoniumIon) {

                                    // get the indexes of the corresponding residues
                                    char residue = ImmoniumIon.getResidue(((ImmoniumIon) ionMatch.ion).getSubType());
                                    char[] peptideAsArray = peptideSequence.toCharArray();
                                    for (int i = 0; i < peptideAsArray.length; i++) {
                                        if (peptideAsArray[i] == residue) {
                                            indexes += (i + 1) + " ";
                                        }
                                    }
                                } else if (ionMatch.ion instanceof ReporterIon
                                        || ionMatch.ion instanceof RelatedIon // @TODO: request cv terms for related ions?
                                        || ionMatch.ion instanceof PrecursorIon) {
                                    indexes = "0";
                                }

                                mzValues += ionMatch.peak.mz + " ";
                                intensityValues += ionMatch.peak.intensity + " ";
                                errorValues += ionMatch.getAbsoluteError() + " ";
                            }

                            // add the supported fragment ions
                            if (fragmentIonCvTerm != null) {
                                br.write(getCurrentTabSpace() + "<IonType charge=\"" + fragmentCharge + "\" index=\"" + indexes.trim() + "\">" + lineBreak);
                                tabCounter++;

                                br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_MZ\" values=\"" + mzValues.trim() + "\"/>" + lineBreak);
                                br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_Int\" values=\"" + intensityValues.trim() + "\"/>" + lineBreak);
                                br.write(getCurrentTabSpace() + "<FragmentArray measure_ref=\"Measure_Error\" values=\"" + errorValues.trim() + "\"/>" + lineBreak);

                                // add the cv term for the fragment ion type
                                writeCvTerm(fragmentIonCvTerm);

                                // add the cv term for the neutral losses
                                if (currentIon.getNeutralLosses() != null) {
                                    int neutralLossesCount = currentIon.getNeutralLosses().length;
                                    if (neutralLossesCount > maxNeutralLosses) {
                                        throw new IllegalArgumentException("A maximum of " + maxNeutralLosses + " neutral losses is allowed!");
                                    } else {
                                        for (NeutralLoss tempNeutralLoss : currentIon.getNeutralLosses()) {
                                            writeCvTerm(tempNeutralLoss.getPsiMsCvTerm());
                                        }
                                    }
                                }

                                tabCounter--;
                                br.write(getCurrentTabSpace() + "</IonType>" + lineBreak);
                            }
                        }
                    }

                    tabCounter--;
                    br.write(getCurrentTabSpace() + "</Fragmentation>" + lineBreak);
                }
            }

            // add peptide shaker score and confidence
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002466", "PeptideShaker PSM score", Double.toString(Util.roundDouble(psmParameter.getPsmScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002467", "PeptideShaker PSM confidence", Double.toString(Util.roundDouble(psmParameter.getPsmConfidence(), CONFIDENCE_DECIMALS))));

            switch (mzIdentMLVersion) {
                case v1_1:
                    break;
                case v1_2:

                    PTMScoringPreferences ptmScoringPreferences = identificationParameters.getPtmScoringPreferences();
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    PSPtmScores psPtmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());

                    if (psPtmScores != null) {

                        Peptide peptide = bestPeptideAssumption.getPeptide();
                        if (peptide.isModified()) {

                            ArrayList<String> scoredPtms = psPtmScores.getScoredPTMs();
                            HashSet<String> ptmsCovered = new HashSet<String>(scoredPtms.size());
                            
                            for (String ptmName : scoredPtms) {

                                PTM currentPtm = ptmFactory.getPTM(ptmName);

                                if (!ptmsCovered.contains(ptmName)) {
                                    ptmsCovered.add(ptmName);

                                    Double ptmMass = currentPtm.getMass();
                                    Integer ptmIndex = ptmIndexMap.get(ptmMass);
                                    if (ptmIndex == null) {
                                        throw new IllegalArgumentException("No index found for PTM " + ptmName + " of mass " + ptmMass + ".");
                                    }

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
                                                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001985", "Ascore", ptmIndex + ":" + score + ":" + site + ":" + valid));
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
                                                writeCvTerm(new CvTerm("PSI-MS", "MS:1002536", "D-Score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PSParameter peptideParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psmParameter);
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002469", "PeptideShaker peptide confidence", peptideParameter.getPeptideConfidence() + ""));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002468", "PeptideShaker peptide score", peptideParameter.getPeptideScore() + ""));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002500", "peptide passes threshold", peptideParameter.getMatchValidationLevel().isValidated() + ""));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002520", "peptide group ID", peptideKey));

                    psPtmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());

                    if (psPtmScores != null) {

                        Peptide peptide = peptideMatch.getTheoreticPeptide();
                        if (peptide.isModified()) {

                            ArrayList<String> scoredPtms = psPtmScores.getScoredPTMs();
                            HashSet<String> ptmsCovered = new HashSet<String>(scoredPtms.size());
                            
                            for (String ptmName : scoredPtms) {

                                if (!ptmsCovered.contains(ptmName)) {
                                    ptmsCovered.add(ptmName);

                                    PTM currentPtm = ptmFactory.getPTM(ptmName);
                                    Double ptmMass = currentPtm.getMass();
                                    Integer ptmIndex = ptmIndexMap.get(ptmMass);
                                    if (ptmIndex == null) {
                                        throw new IllegalArgumentException("No index found for PTM " + ptmName + " of mass " + ptmMass + ".");
                                    }
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
                                                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002551", "peptide:Ascore", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                    } else if (ptmScoringPreferences.getSelectedProbabilisticScore() == PtmScore.PhosphoRS) {
                                                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002550", "peptide:phosphoRS score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                    }
                                                }
                                            }
                                            double score = ptmScoring.getDeltaScore(site);
                                            if (score > 0) {
                                                String valid = "true";
                                                if (score < dScoreThreshold) {
                                                    valid = "false";
                                                }
                                                writeCvTerm(new CvTerm("PSI-MS", "MS:1002553", "peptide:D-Score", ptmIndex + ":" + score + ":" + site + ":" + valid));
                                                //writeCvTerm(new CvTerm("PSI-MS", "MS:???", "PeptideShaker PTM confidence type", "???")); // @TODO: can be at both the psm and peptide level...
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
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
            br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001117\" name=\"theoretical mass\" value=\"" + String.valueOf(bestPeptideAssumption.getTheoreticMass()) + "\" "
                    + "unitCvRef=\"UO\" unitAccession=\"UO:0000221\" unitName=\"dalton\"/>" + lineBreak);

            // add validation level information
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002540", "PeptideShaker PSM confidence type", psmParameter.getMatchValidationLevel().getName()));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationItem>" + lineBreak);

            // add the spectrum title
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "spectrum title", spectrumTitle));

            // add the precursor retention time
            Precursor precursor = spectrumFactory.getPrecursor(psmKey);
            if (precursor != null) {
                br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1000894\" name=\"retention time\" value=\"" + String.valueOf(precursor.getRt()) + "\" "
                        + "unitCvRef=\"UO\" unitAccession=\"UO:0000010\" unitName=\"second\"/>" + lineBreak);
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationResult>" + lineBreak);
        }
    }

    /**
     * Write the fragmentation table. (Note: all hard coded.)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeFragmentationTable() throws IOException {

        br.write(getCurrentTabSpace() + "<FragmentationTable>" + lineBreak);
        tabCounter++;

        // mz
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_MZ\">" + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001225\" name=\"product ion m/z\" "
                + "unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\" />" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + lineBreak);

        // intensity
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_Int\">" + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001226\" name=\"product ion intensity\" "
                + "unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000131\" unitName=\"number of detector counts\"/>" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + lineBreak);

        // mass error
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_Error\">" + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001227\" name=\"product ion m/z error\" "
                + "unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>" + lineBreak);
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + lineBreak);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentationTable>" + lineBreak);
    }

    /**
     * Write the input file details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeInputFileDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Inputs>" + lineBreak);
        tabCounter++;

        int sourceFileCounter = 1;

        // add the search result files
        for (File idFile : projectDetails.getIdentificationFiles()) {

            // @TODO: add MS:1000568 - MD5?
//            FileInputStream fis = new FileInputStream(new File("foo"));
//            String md5 = DigestUtils.md5Hex(fis);
//            fis.close();
            br.write(getCurrentTabSpace() + "<SourceFile location=\"" + idFile.toURI().toString() + "\" id=\"SourceFile_" + sourceFileCounter++ + "\">" + lineBreak);
            tabCounter++;
            br.write(getCurrentTabSpace() + "<FileFormat>" + lineBreak);
            tabCounter++;

            String idFileName = Util.getFileName(idFile);
            HashMap<String, ArrayList<String>> algorithms = projectDetails.getIdentificationAlgorithmsForFile(idFileName);

            for (String algorithmName : algorithms.keySet()) {
                Advocate advocate = Advocate.getAdvocate(algorithmName);
                int advocateIndex = advocate.getIndex();
                if (advocateIndex == Advocate.mascot.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001199", "Mascot DAT format", null));
                } else if (advocateIndex == Advocate.xtandem.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001401", "X!Tandem xml format", null));
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
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002576", "Andromeda result file", null)); // @TODO: term does not exist..?
                } else {
                    // no cv term available for the given advocate...
                }
            }

            // @TODO: add children of MS:1000561 - data file checksum type?
            tabCounter--;
            br.write(getCurrentTabSpace() + "</FileFormat>" + lineBreak);
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SourceFile>" + lineBreak);
        }

        // add the database
        File database = identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase();
        br.write(getCurrentTabSpace() + "<SearchDatabase numDatabaseSequences=\"" + sequenceFactory.getNSequences()
                + "\" location=\"" + database.toURI().toString() + "\" "
                + "id=\"" + "SearchDB_1\">" + lineBreak);
        tabCounter++;
        br.write(getCurrentTabSpace() + "<FileFormat>" + lineBreak);
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001348", "FASTA format", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FileFormat>" + lineBreak);
        br.write(getCurrentTabSpace() + "<DatabaseName>" + lineBreak);
        tabCounter++;
        writeUserParam(database.getName()); // @TODO: add database type? children of MS:1001013 - database name??? for example: MS:1001104 (database UniProtKB/Swiss-Prot)
        tabCounter--;
        br.write(getCurrentTabSpace() + "</DatabaseName>" + lineBreak);
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001073", "database type amino acid", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchDatabase>" + lineBreak);

        // add the spectra location
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {
            File mgfFile = projectDetails.getSpectrumFile(mgfFileName);

            br.write(getCurrentTabSpace() + "<SpectraData location=\"" + mgfFile.toURI().toString() + "\" id=\"" + mgfFileName
                    + "\" name=\"" + mgfFile.getName() + "\">" + lineBreak);
            tabCounter++;

            br.write(getCurrentTabSpace() + "<FileFormat>" + lineBreak);
            tabCounter++;
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001062", "Mascot MGF format", null));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</FileFormat>" + lineBreak);

            br.write(getCurrentTabSpace() + "<SpectrumIDFormat>" + lineBreak);
            tabCounter++;
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000774", "multiple peak list nativeID format", null));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIDFormat>" + lineBreak);

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectraData>" + lineBreak);
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Inputs>" + lineBreak);
    }

    /**
     * Writes the mzIdentML start tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzIdentMLStartTag() throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineBreak);

        switch (mzIdentMLVersion) {
            case v1_1:
                br.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.1 http://www.psidev.info/files/mzIdentML1.1.0.xsd\" "
                        + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\" version=\"1.1.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">"
                        + lineBreak);
                break;
            case v1_2:
                br.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.2 http://www.psidev.info/files/mzIdentML1.2.0.xsd\" "
                        + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.2\" version=\"1.2.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">"
                        + lineBreak);
                break;
            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
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
     * Convenience method writing a CV term.
     *
     * @param cvTerm the CV term
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvTerm(CvTerm cvTerm) throws IOException {
        writeCvTerm(cvTerm, true);
    }

    /**
     * Convenience method writing a CV term.
     *
     * @param cvTerm the CV term
     * @param showValue decides if the CV terms value (if existing) is printed
     * or not
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvTerm(CvTerm cvTerm, boolean showValue) throws IOException {

        br.write(getCurrentTabSpace() + "<cvParam "
                + "cvRef=\"" + StringEscapeUtils.escapeHtml4(cvTerm.getOntology()) + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + StringEscapeUtils.escapeHtml4(cvTerm.getName()) + "\"");

        writeCvTermValue(cvTerm, showValue);
    }

    /**
     * Convenience method writing the value element of a CV term.
     *
     * @param cvTerm the CV term
     * @param showValue decides if the CV terms value (if existing) is printed
     * or not
     * @throws IOException
     */
    private void writeCvTermValue(CvTerm cvTerm, boolean showValue) throws IOException {
        if (showValue && cvTerm.getValue() != null) {
            br.write(" value=\"" + StringEscapeUtils.escapeHtml4(cvTerm.getValue()) + "\"/>" + lineBreak);
        } else {
            br.write("/>" + lineBreak);
        }
    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param userParamAsString the user parameter as a string
     */
    private void writeUserParam(String userParamAsString) throws IOException {
        br.write(getCurrentTabSpace() + "<userParam name=\"" + StringEscapeUtils.escapeHtml4(userParamAsString) + "\"/>" + lineBreak); // @replace...
    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param name the name of the user parameter
     * @param value the value of the user parameter
     */
    private void writeUserParam(String name, String value) throws IOException {
        br.write(getCurrentTabSpace() + "<userParam name=\"" + StringEscapeUtils.escapeHtml4(name) + "\" value=\"" + StringEscapeUtils.escapeHtml4(value) + "\" />" + lineBreak);
    }
}
