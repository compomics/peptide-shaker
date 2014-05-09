package eu.isas.peptideshaker.export;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.AminoAcid;
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
import com.compomics.util.experiment.identification.SequenceFactory.ProteinIterator;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.*;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.lang3.StringEscapeUtils;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to mzIdentML. (Work in
 * progress...)
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
     * The PTM to mzIdentML map.
     */
    private PtmToPrideMap ptmToPrideMap; // @TODO: should be renamed!!!
    /**
     * The number of decimals to use for the confidence values.
     */
    private final int CONFIDENCE_DECIMALS = 2;
    /**
     * The mzIdentML xsd.
     */
    private String mzIdentMLXsd = "\"http://psidev.info/psi/pi/mzIdentML/1.1\"";
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
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The PTM scoring preferences.
     */
    private PTMScoringPreferences ptmScoringPreferences;
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
     * The annotation preferences.
     */
    private AnnotationPreferences annotationPreferences;
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
     * Constructor.
     *
     * @param peptideShakerVersion
     * @param identification
     * @param projectDetails
     * @param spectrumCountingPreferences
     * @param identificationFeaturesGenerator
     * @param searchParameters
     * @param annotationPreferences
     * @param spectrumAnnotator
     * @param ptmScoringPreferences
     * @param outputFile Output file
     * @param waitingHandler
     * @throws FileNotFoundException Exception thrown whenever a file was not
     * found
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a pride object
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification, ProjectDetails projectDetails, SearchParameters searchParameters, PTMScoringPreferences ptmScoringPreferences,
            SpectrumCountingPreferences spectrumCountingPreferences, IdentificationFeaturesGenerator identificationFeaturesGenerator, PeptideSpectrumAnnotator spectrumAnnotator,
            AnnotationPreferences annotationPreferences, File outputFile, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.searchParameters = searchParameters;
        this.ptmScoringPreferences = ptmScoringPreferences;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.spectrumAnnotator = spectrumAnnotator;
        this.annotationPreferences = annotationPreferences;
        this.waitingHandler = waitingHandler;
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance(); // @TODO: should be renamed!!!
        ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        r = new FileWriter(outputFile);
        br = new BufferedWriter(r);
    }

    /**
     * Creates the mzIdentML file.
     *
     * @param progressDialog a dialog displaying progress to the user
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     * @throws SQLException
     */
    public void createMzIdentMLFile(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException, IllegalArgumentException, ClassNotFoundException, InterruptedException, SQLException {

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

        br.write(getCurrentTabSpace() + "<cvList xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace()
                + "<cv id=\"PSI-MS\" "
                + "uri=\"http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo\" "
                + "version=\"2.25.0\" "
                + "fullName=\"PSI-MS\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv id=\"UNIMOD\" "
                + "uri=\"http://www.unimod.org/obo/unimod.obo\" "
                + "fullName=\"UNIMOD\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv id=\"UO\" "
                + "uri=\"http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/unit.obo\" "
                + "fullName=\"UNIT-ONTOLOGY\"/>" + System.getProperty("line.separator"));

        // @TODO: add more? perhaps not hardcode?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</cvList>" + System.getProperty("line.separator"));
    }

    /**
     * Write the software list.
     *
     * @throws IOException
     */
    private void writeAnalysisSoftwareList() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisSoftwareList>" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: also add SearchGUI and/or search engines used?
        br.write(getCurrentTabSpace() + "<AnalysisSoftware "
                + "name=\"PeptideShaker \" "
                + "version=\"" + peptideShakerVersion + "\" "
                + "id=\"ID_software\" "
                + "uri=\"http://peptide-shaker.googlecode.com\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SoftwareName>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1002458", "PeptideShaker", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SoftwareName>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PS_DEV\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001267", "software vendor", "CompOmics"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Role>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ContactRole>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftware>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftwareList>" + System.getProperty("line.separator"));
    }

    /**
     * Write the provider details.
     *
     * @throws IOException
     */
    private void writeProviderDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Provider id=\"PROVIDER\" xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PROVIDER\">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001271", "researcher", null)); // @TODO: add the provider role here!!
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
     * @throws IOException
     */
    private void writeAuditCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AuditCollection xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Person "
                + "firstName=\"" + projectDetails.getContactFirstName() + "\" "
                + "lastName=\"" + projectDetails.getContactLastName() + "\" "
                + "id=\"PROVIDER\">"
                + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>" + System.getProperty("line.separator"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getContactAddress()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", projectDetails.getContactUrl()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getContactEmail()));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Person>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"" + projectDetails.getOrganizationName() + "\" id=\"ORG_DOC_OWNER\">" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", projectDetails.getOrganizationName()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getOrganizationAddress()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", projectDetails.getOrganizationUrl()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getOrganizationEmail()));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "PeptideShaker developers"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "Proteomics Unit, Building for Basic Biology, University of Bergen, Jonas Liesvei 91, N-5009 Bergen, Norway"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", "http://peptide-shaker.googlecode.com"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "peptide-shaker@googlegroups.com"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AuditCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the sequence collection.
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void writeSequenceCollection() throws IOException, IllegalArgumentException, InterruptedException, ClassNotFoundException, SQLException {

        br.write(getCurrentTabSpace() + "<SequenceCollection  xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\">" + System.getProperty("line.separator"));
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
            br.write(getCurrentTabSpace() + "<Seq>" + currentProtein.getSequence() + "</Seq>" + System.getProperty("line.separator"));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001088", "protein description", URLDecoder.decode(sequenceFactory.getHeader(currentProtein.getAccession()).getDescription(), "utf-8")));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</DBSequence>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        iterator.close();

        identification.loadPeptideMatches(null);
        identification.loadPeptideMatchParameters(new PSParameter(), null);

        // iterate all the peptides
        for (String peptideKey : identification.getPeptideIdentification()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            Peptide peptide = peptideMatch.getTheoreticPeptide();
            String peptideSequence = peptide.getSequence();

            br.write(getCurrentTabSpace() + "<Peptide id=\"" + peptideKey + "\" >" + System.getProperty("line.separator"));
            tabCounter++;
            br.write(getCurrentTabSpace() + "<PeptideSequence>" + peptideSequence + "</PeptideSequence>" + System.getProperty("line.separator"));

            for (ModificationMatch modMatch : peptide.getModificationMatches()) {

                PTM currentPtm = ptmFactory.getPTM(modMatch.getTheoreticPtm());
                int ptmLocation = modMatch.getModificationSite();

                if (currentPtm.isNTerm()) {
                    ptmLocation = 0;
                } else if (currentPtm.isCTerm()) {
                    ptmLocation = peptideSequence.length() + 1;
                }

                br.write(getCurrentTabSpace() + "<Modification monoisotopicMassDelta=\"" + currentPtm.getMass() + "\" "
                        + "residues=\"" + peptideSequence.charAt(modMatch.getModificationSite() - 1) + "\" "
                        + "location=\"" + ptmLocation + "\" >" + System.getProperty("line.separator"));

                CvTerm ptmCvTerm = PtmToPrideMap.getDefaultCVTerm(currentPtm.getName());
                if (ptmCvTerm != null) {
                    tabCounter++;
                    writeCvTerm(ptmCvTerm);
                    tabCounter--;
                }

                // add cv/user params
                // @TODO: ptm validation
                // @TODO: ptm localization scores across possible sites: PhosphoRS, A-score, d-score, ms-score
                br.write(getCurrentTabSpace() + "</Modification>" + System.getProperty("line.separator"));
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Peptide>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        int peptideEvidenceCounter = 0;

        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null);
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null);

            // iterate the psms
            for (String psmKey : identification.getSpectrumIdentification(spectrumFileName)) {

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (bestPeptideAssumption != null) {

                    Peptide peptide = bestPeptideAssumption.getPeptide();

                    // get all the possible parent proteins
                    ArrayList<String> possibleProteins = peptide.getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                    // iterate all the possible protein parents for each peptide
                    for (String tempProtein : possibleProteins) {

                        // get the start indexes and the surrounding 
                        HashMap<Integer, String[]> aaSurrounding = sequenceFactory.getProtein(tempProtein).getSurroundingAA(
                                peptide.getSequence(), 1, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

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

                            String peptideKey = peptide.getMatchingKey(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                            String pepEvidenceKey = tempProtein + "_" + peptideStart + "_" + peptideKey;
                            pepEvidenceIds.put(pepEvidenceKey, "PepEv_" + ++peptideEvidenceCounter);
                            String matchingPeptideKey = peptide.getMatchingKey(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                            br.write(getCurrentTabSpace() + "<PeptideEvidence isDecoy=\"" + peptide.isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy()) + "\" "
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
     * @throws IOException
     */
    private void writeAnalysisCollection() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisCollection xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentification "
                + "spectrumIdentificationList_ref=\"SIL_1\" " // @TODO: should not be hardcoded?
                + "spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" " // @TODO: should not be hardcoded?
                + "id=\"SpecIdent_1\">" // @TODO: should not be hardcoded?
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<InputSpectra spectraData_ref=\"SID_1\"/>" + System.getProperty("line.separator")); // @TODO: should not be hardcoded?
        br.write(getCurrentTabSpace() + "<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>" + System.getProperty("line.separator")); // @TODO: should not be hardcoded?

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentification>" + System.getProperty("line.separator"));

        // add protein detection
        br.write(getCurrentTabSpace() + "<ProteinDetection " // @TODO: add activityDate? example: activityDate="2011-03-25T13:33:51
                + "proteinDetectionProtocol_ref=\"PeptideShaker_1\" " // @TODO: should not be hardcoded?
                + "proteinDetectionList_ref=\"Protein_groups\" " // @TODO: should not be hardcoded?
                + "id=\"PD_1\">" // @TODO: should not be hardcoded?
                + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<InputSpectrumIdentifications spectrumIdentificationList_ref=\"SIL_1\"/>" + System.getProperty("line.separator")); // @TODO: should not be hardcoded?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetection>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the analysis protocol.
     *
     * @throws IOException
     */
    private void writeAnalysisProtocol() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisProtocolCollection xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
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
        br.write(getCurrentTabSpace() + "<AdditionalSearchParams>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001211", "parent mass type mono", null));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001256", "fragment mass type mono", null));

        // @TODO: list all search parameters from the search engines used?
        tabCounter--;
        br.write(getCurrentTabSpace() + "</AdditionalSearchParams>" + System.getProperty("line.separator"));

        // the modifications
        br.write(getCurrentTabSpace() + "<ModificationParams>" + System.getProperty("line.separator"));
        tabCounter++;

        // iterate and add the ptms
        for (String ptm : searchParameters.getModificationProfile().getAllModifications()) {

            PTM currentPtm = ptmFactory.getPTM(ptm);

            String aminoAcidsAtTarget = "";
            if (currentPtm.getType() == PTM.MODN
                    || currentPtm.getType() == PTM.MODNP
                    || currentPtm.getType() == PTM.MODC
                    || currentPtm.getType() == PTM.MODCP) {
                aminoAcidsAtTarget = ".";
            } else {
                for (AminoAcid aa : currentPtm.getPattern().getAminoAcidsAtTarget()) {
                    aminoAcidsAtTarget += aa.singleLetterCode;
                }
            }

            br.write(getCurrentTabSpace() + "<SearchModification residues=\"" + aminoAcidsAtTarget + "\" massDelta=\"" + currentPtm.getMass()
                    + "\" fixedMod= \"" + searchParameters.getModificationProfile().getFixedModifications().contains(ptm) + "\" >" + System.getProperty("line.separator"));
            tabCounter++;

            CvTerm cvTerm = ptmToPrideMap.getCVTerm(ptm);
            if (cvTerm != null) {
                writeCvTerm(cvTerm);
            }

            // add modification specificity
            if (currentPtm.getType() == PTM.MODN
                    || currentPtm.getType() == PTM.MODNAA) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002057", "modification specificity protein N-term", null));
            } else if (currentPtm.getType() == PTM.MODNP
                    || currentPtm.getType() == PTM.MODNPAA) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1001189", "modification specificity peptide N-term", null));
            } else if (currentPtm.getType() == PTM.MODC
                    || currentPtm.getType() == PTM.MODCAA) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1002058", "modification specificity protein C-term", null));
            } else if (currentPtm.getType() == PTM.MODCP
                    || currentPtm.getType() == PTM.MODCPAA) {
                writeCvTerm(new CvTerm("PSI-MS", "MS:1001190", "modification specificity peptide C-term", null));
            }

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
                + System.getProperty("line.separator"));
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

        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001412\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"dalton\" "
                + "unitAccession=\"UO:0000221\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance plus value\" />"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413\" "
                + "cvRef=\"PSI-MS\" "
                + "unitCvRef=\"UO\" "
                + "unitName=\"dalton\" "
                + "unitAccession=\"UO:0000221\" "
                + "value=\"" + searchParameters.getFragmentIonAccuracy() + "\" "
                + "name=\"search tolerance minus value\" />"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentTolerance>" + System.getProperty("line.separator"));

        // precursor tolerance
        br.write(getCurrentTabSpace() + "<ParentTolerance>" + System.getProperty("line.separator"));
        tabCounter++;

        if (searchParameters.isPrecursorAccuracyTypePpm()) {
            br.write(getCurrentTabSpace() + "<cvParam "
                    + "accession=\"MS:1001412\" "
                    + "cvRef=\"PSI-MS\" "
                    + "unitCvRef=\"UO\" "
                    + "unitName=\"parts per million\" "
                    + "unitAccession=\"UO:0000169\" "
                    + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                    + "name=\"search tolerance plus value\" />"
                    + System.getProperty("line.separator"));
            br.write(getCurrentTabSpace() + "<cvParam "
                    + "accession=\"MS:1001413\" "
                    + "cvRef=\"PSI-MS\" "
                    + "unitCvRef=\"UO\" "
                    + "unitName=\"parts per million\" "
                    + "unitAccession=\"UO:0000169\" "
                    + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                    + "name=\"search tolerance minus value\" />"
                    + System.getProperty("line.separator"));
        } else {
            br.write(getCurrentTabSpace() + "<cvParam "
                    + "accession=\"MS:1001412\" "
                    + "cvRef=\"PSI-MS\" "
                    + "unitCvRef=\"UO\" "
                    + "unitName=\"dalton\" "
                    + "unitAccession=\"UO:0000221\" "
                    + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                    + "name=\"search tolerance plus value\" />"
                    + System.getProperty("line.separator"));
            br.write(getCurrentTabSpace() + "<cvParam "
                    + "accession=\"MS:1001413\" "
                    + "cvRef=\"PSI-MS\" "
                    + "unitCvRef=\"UO\" "
                    + "unitName=\"dalton\" "
                    + "unitAccession=\"UO:0000221\" "
                    + "value=\"" + searchParameters.getPrecursorAccuracy() + "\" "
                    + "name=\"search tolerance minus value\" />"
                    + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ParentTolerance>" + System.getProperty("line.separator"));

        // thresholds
        br.write(getCurrentTabSpace() + "<Threshold>" + System.getProperty("line.separator"));
        tabCounter++;

        boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();

        if (!targetDecoy) {
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));
        } else {

            // peptideshaker maps
            PSMaps psMaps = new PSMaps();
            psMaps = (PSMaps) identification.getUrParam(psMaps);

            // peptide level threshold
            PeptideSpecificMap peptideSpecificMap = psMaps.getPeptideSpecificMap();
            ArrayList<String> peptideGroupsKeys = peptideSpecificMap.getKeys();

            for (String key : peptideGroupsKeys) { // @TODO: find a way of annotating all thresholds..?
                TargetDecoyMap targetDecoyMap = peptideSpecificMap.getTargetDecoyMap(key);
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double threshold = targetDecoyResults.getUserInput() / 100;
                int thresholdType = targetDecoyResults.getInputType();
                if (peptideGroupsKeys.size() > 1) {
                    String peptideClass = PeptideSpecificMap.getKeyName(searchParameters.getModificationProfile(), key);
                    // @TODO: somehow indicate the class of peptide thresholded?
                }
                if (thresholdType == 0) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002463", "distinct peptide-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
                } else if (targetDecoyResults.getInputType() == 1) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "distinct peptide-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
                } else if (targetDecoyResults.getInputType() == 2) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002462", "distinct peptide-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
                }
            }

            // psm level threshold
            ArrayList<Integer> chargesWithFileSpecificity = new ArrayList<Integer>();
            PsmSpecificMap psmSpecificMap = psMaps.getPsmSpecificMap();

            for (Integer charge : psmSpecificMap.getPossibleCharges()) {
                for (String file : psmSpecificMap.getFilesAtCharge(charge)) {
                    if (!psmSpecificMap.isFileGrouped(charge, file)) { // @TODO: find a way of annotating all thresholds..?
                        chargesWithFileSpecificity.add(charge);
                        TargetDecoyMap targetDecoyMap = psMaps.getPsmSpecificMap().getTargetDecoyMap(charge, file);
                        TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                        double threshold = targetDecoyResults.getUserInput() / 100;
                        int thresholdType = targetDecoyResults.getInputType();
                        String psmClass = "Charge " + charge + " of file " + file; // @TODO: annotate class?
                        if (thresholdType == 0) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002465", "PSM-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence
                        } else if (targetDecoyResults.getInputType() == 1) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR
                        } else if (targetDecoyResults.getInputType() == 2) {
                            writeCvTerm(new CvTerm("PSI-MS", "MS:1002464", "PSM-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR
                        }
                    }
                }
            }
            //@TODO: set the PSM group label
//            for (int charge : psmSpecificMap.getChargesFromGroupedFiles()) {
//                int correctedCharge = psmSpecificMap.getCorrectedCharge(charge);
//                if (correctedCharge == charge) {
//                    TargetDecoyMap targetDecoyMap = pSMaps.getPsmSpecificMap().getTargetDecoyMap(charge, null);
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
            // @TODO: re-add psm ptm thresholds
//            PsmPTMMap psmPTMMap = pSMaps.getPsmPTMMap();
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
     * @throws IOException
     */
    private void writeDataCollection() throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
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
     * @throws IOException
     */
    private void writeDataAnalysis() throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        br.write(getCurrentTabSpace() + "<AnalysisData>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationList id=\"SIL_1\" xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        writeFragmentationTable();

        int psmCount = 0;
        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null);
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null);

            // iterate the psms
            for (String psmKey : identification.getSpectrumIdentification(spectrumFileName)) {
                writeSpectrumIdentificationResult(psmKey, ++psmCount);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationList>" + System.getProperty("line.separator"));

        writeProteinDetectionList();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisData>" + System.getProperty("line.separator"));
    }

    /**
     * Write the protein groups.
     *
     * @throws IOException
     */
    private void writeProteinDetectionList() throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException {

        br.write(getCurrentTabSpace() + "<ProteinDetectionList id=\"Protein_groups\">" + System.getProperty("line.separator"));
        tabCounter++;

        identification.loadPeptideMatches(null);

        int groupCpt = 0;

        PSParameter psParameter = new PSParameter();
        identification.loadProteinMatches(null);
        identification.loadProteinMatchParameters(psParameter, null);

        for (String proteinGroupKey : identification.getProteinIdentification()) {

            String proteinGroupId = "PAG_" + groupCpt++;

            br.write(getCurrentTabSpace() + "<ProteinAmbiguityGroup id=\"" + proteinGroupId + "\">" + System.getProperty("line.separator"));
            tabCounter++;

            ProteinMatch proteinMatch = identification.getProteinMatch(proteinGroupKey);
            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinGroupKey, psParameter);

            String mainAccession = proteinMatch.getMainMatch();

            for (int j = 0; j < proteinMatch.getTheoreticProteinsAccessions().size(); j++) {

                String accession = proteinMatch.getTheoreticProteinsAccessions().get(j);

                br.write(getCurrentTabSpace() + "<ProteinDetectionHypothesis id=\"" + proteinGroupId + "_" + (j + 1) + "\" dBSequence_ref=\"" + accession
                        + "\" passThreshold=\"" + psParameter.getMatchValidationLevel().isValidated() + "\">" + System.getProperty("line.separator"));
                tabCounter++;

                ArrayList<String> peptideMatches = identification.getProteinMatch(proteinGroupKey).getPeptideMatches();
                identification.loadPeptideMatches(peptideMatches, null);

                for (String peptideKey : peptideMatches) {

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    String peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();

                    ArrayList<Integer> peptideStarts = sequenceFactory.getProtein(accession).getPeptideStart(
                            peptideSequence, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                    for (int start : peptideStarts) {
                        String pepEvidenceKey = accession + "_" + start + "_" + peptideKey;
                        String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);

                        if (peptideEvidenceId != null) {

                            br.write(getCurrentTabSpace() + "<PeptideHypothesis peptideEvidence_ref=\"" + peptideEvidenceId + "\">" + System.getProperty("line.separator"));
                            tabCounter++;

                            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
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
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001591", "anchor protein", null));
                    //writeCvTerm(new CvTerm("PSI-MS", "MS:1001594", "sequence same-set protein", null));
                } else {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001594", "sequence same-set protein", null)); // @TODO: validate these cv terms!! (children of MS:1001101)
                }
                
                // add protein coverage cv term
                //writeCvTerm(new CvTerm("PSI-MS", "MS:1001093", "sequence coverage", null)); // @TODO: sequence coverage??
                
                // add protein score
                // @TODO: add protein scores? don't think we have these..?
                // children off MS:1001153 or MS:1001116

                tabCounter--;
                br.write(getCurrentTabSpace() + "</ProteinDetectionHypothesis>" + System.getProperty("line.separator"));
            }

            // add protein group cv terms
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002470", "PeptideShaker protein group score", Double.toString(Util.roundDouble(psParameter.getProteinScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002471", "PeptideShaker protein group confidence", Double.toString(Util.roundDouble(psParameter.getProteinConfidence(), CONFIDENCE_DECIMALS))));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</ProteinAmbiguityGroup>" + System.getProperty("line.separator"));

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionList>" + System.getProperty("line.separator"));
    }

    /**
     * Write a spectrum identification result.
     *
     * @throws IOException
     */
    private void writeSpectrumIdentificationResult(String psmKey, int psmIndex) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(psmKey);
        String spectrumFileName = Spectrum.getSpectrumFile(psmKey);
        String spectrumTitleHtml = StringEscapeUtils.escapeHtml4(spectrumTitle);
        String spectrumIdentificationResultItemKey = "SIR_" + psmIndex;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationResult "
                + "spectraData_ref=\"" + spectrumFileName
                + "\" spectrumID=\"" + "index=" + spectrumFactory.getSpectrumIndex(spectrumTitle, spectrumFileName)
                + "\" id=\"" + spectrumIdentificationResultItemKey + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: iterate all assumptions and not just the best one?
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

        if (bestPeptideAssumption != null) {

            PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter());
            int rank = 1; // @TODO: should not be hardcoded?
            String spectrumIdentificationItemKey = "SII_" + psmIndex + "_" + rank;
            spectrumIds.put(psmKey, spectrumIdentificationItemKey);

            br.write(getCurrentTabSpace() + "<SpectrumIdentificationItem "
                    + "passThreshold=\"" + pSParameter.getMatchValidationLevel().isValidated() + "\" "
                    + "rank=\"" + rank + "\" "
                    + "peptide_ref=\"" + bestPeptideAssumption.getPeptide().getMatchingKey(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy()) + "\" "
                    + "calculatedMassToCharge=\"" + bestPeptideAssumption.getTheoreticMz() + "\" "
                    + "experimentalMassToCharge=\"" + spectrumFactory.getPrecursor(psmKey).getMz() + "\" "
                    + "chargeState=\"" + bestPeptideAssumption.getIdentificationCharge().value + "\" "
                    + "id=\"" + spectrumIdentificationItemKey + "\">" + System.getProperty("line.separator"));
            tabCounter++;

            // @TODO: add peptide level annotation
            // MS:1002462: distinct peptide-level global FNR
            // MS:1002463: distinct peptide-level global confidence
            // MS:1001364: distinct peptide-level global FDR
            //
            // add the peptide evidence references
            // get all the possible parent proteins
            ArrayList<String> possibleProteins = bestPeptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
            String peptideSequence = bestPeptideAssumption.getPeptide().getSequence();

            // iterate all the possible protein parents for each peptide
            for (String tempProtein : possibleProteins) {

                // get the start indexes and the surrounding amino acids
                ArrayList<Integer> peptideStarts = sequenceFactory.getProtein(tempProtein).getPeptideStart(
                        peptideSequence, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                for (int start : peptideStarts) {
                    String pepEvidenceKey = tempProtein + "_" + start + "_" + bestPeptideAssumption.getPeptide().getMatchingKey(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                    String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);
                    br.write(getCurrentTabSpace() + "<PeptideEvidenceRef peptideEvidence_ref=\"" + peptideEvidenceId + "\"/>" + System.getProperty("line.separator"));
                }
            }

            // add the fragment ion annotation
            int identificationCharge = bestPeptideAssumption.getIdentificationCharge().value;
            annotationPreferences.setCurrentSettings(bestPeptideAssumption, true, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);

            // get all the fragment ion annotations
            ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                    annotationPreferences.getNeutralLosses(),
                    annotationPreferences.getValidatedCharges(),
                    identificationCharge,
                    spectrum, bestPeptideAssumption.getPeptide(),
                    spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                    annotationPreferences.getFragmentIonAccuracy(), false,
                    annotationPreferences.isHighResolutionAnnotation());

            // organize the fragment ions by ion type
            HashMap<String, HashMap<Integer, ArrayList<IonMatch>>> allFragmentIons = new HashMap<String, HashMap<Integer, ArrayList<IonMatch>>>();

            for (IonMatch ionMatch : annotations) {

                if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                        || ionMatch.ion.getType() == IonType.IMMONIUM_ION) { // @TODO: add PRECURSOR_ION and REPORTER_ION
//                    || ionMatch.ion.getType() == IonType.PRECURSOR_ION
//                    || ionMatch.ion.getType() == IonType.REPORTER_ION) {

                    CvTerm fragmentIonTerm = ionMatch.ion.getPrideCvTerm(); // @TODO: replace by PSI-MS mappings...
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

            // add cv and user params
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001117", "theoretical mass", String.valueOf(bestPeptideAssumption.getTheoreticMass()))); // @TODO: add more? MS:1001105 - peptide result details

            // add peptide shaker score and confidence
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002466", "PeptideShaker: PSM score", Double.toString(Util.roundDouble(pSParameter.getPsmScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002467", "PeptideShaker PSM confidence", Double.toString(Util.roundDouble(pSParameter.getPsmConfidence(), CONFIDENCE_DECIMALS))));

            // add the individual search engine results
            HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
            for (Integer tempAdvocate : spectrumMatch.getAdvocates()) {
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(tempAdvocate).keySet());
                for (double eValue : eValues) {
                    for (SpectrumIdentificationAssumption currentAssumption : spectrumMatch.getAllAssumptions(tempAdvocate).get(eValue)) {
                        if (currentAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) currentAssumption;
                            if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(
                                    bestPeptideAssumption.getPeptide(), PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                                Double currentMinEvalue = scores.get(tempAdvocate);
                                if (currentMinEvalue == null || eValue < currentMinEvalue) {
                                    scores.put(tempAdvocate, eValue);
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
                if (tempAdvocate == Advocate.MSGF.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002052", "MS-GF:SpecEValue", Double.toString(eValue)));
                } else if (tempAdvocate == Advocate.Mascot.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001172", "Mascot:expectation value", Double.toString(eValue)));
                } else if (tempAdvocate == Advocate.OMSSA.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001328", "OMSSA:evalue", Double.toString(eValue))); // @TODO: or OMSSA p-value (MS:1001329)?
                } else if (tempAdvocate == Advocate.XTandem.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001330", "X!Tandem:expect", Double.toString(eValue))); // @TODO: is this the one? or is it "X!Tandem:hyperscore" (MS:1001331)?
                } else if (tempAdvocate == Advocate.msAmanda.getIndex()) {
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002319", "Amanda:AmandaScore", Double.toString(eValue)));
                }
            }
            // @TODO: 
            // add validation level information
            //writeUserParam(pSParameter.getMatchValidationLevel().getIndex());
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationItem>" + System.getProperty("line.separator"));

            // add the spectrum title
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "spectrum title", spectrumTitleHtml));

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SpectrumIdentificationResult>" + System.getProperty("line.separator"));
        }
    }

    /**
     * Write the fragmentation table. (Note: all hard coded.)
     *
     * @throws IOException
     */
    private void writeFragmentationTable() throws IOException {

        br.write(getCurrentTabSpace() + "<FragmentationTable>" + System.getProperty("line.separator"));
        tabCounter++;

        // mz
        br.write(getCurrentTabSpace() + "<Measure id=\"Measure_MZ\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam accession=\"MS:1001225\" cvRef=\"PSI-MS\" unitCvRef=\"PSI-MS\" unitName=\"m/z\" "
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
        br.write(getCurrentTabSpace() + "<cvParam accession=\"MS:1001227\" cvRef=\"PSI-MS\" unitCvRef=\"PSI-MS\" "
                + "unitName=\"m/z\" unitAccession=\"MS:1000040\" name=\"product ion m/z error\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Measure>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentationTable>" + System.getProperty("line.separator"));
    }

    /**
     * Write the input file details.
     *
     * @throws IOException
     */
    private void writeInputFileDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Inputs xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\">" + System.getProperty("line.separator"));
        tabCounter++;

        int sourceFileCounter = 1;

        // add the search result files
        for (File idFile : projectDetails.getIdentificationFiles()) {

            br.write(getCurrentTabSpace() + "<SourceFile location=\"" + idFile.getAbsolutePath() + "\" id=\"SourceFile_" + sourceFileCounter++ + "\">" + System.getProperty("line.separator"));
            tabCounter++;
            br.write(getCurrentTabSpace() + "<FileFormat>" + System.getProperty("line.separator"));
            tabCounter++;

            int searchEngine = IdfileReaderFactory.getInstance().getSearchEngine(idFile);
            Advocate advocate = Advocate.getAdvocate(searchEngine);

            switch (advocate) {
                case Mascot:
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001199", "Mascot DAT format", null));
                    break;
                case OMSSA:
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001401", "xtandem xml file", null));
                    break;
                case XTandem:
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001400", "OMSSA xml format", null));
                    break;
                case MSGF:
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002073", "mzIdentML format", null));
                    break;
                case msAmanda:
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002459", "MS Amanda csv format", null));
                    break;
                default:
                    writeUserParam("Unknown"); // @TODO: add cv term?
                    break;
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</FileFormat>" + System.getProperty("line.separator"));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</SourceFile>" + System.getProperty("line.separator"));
        }

        // add the database
        br.write(getCurrentTabSpace() + "<SearchDatabase numDatabaseSequences=\"" + sequenceFactory.getNSequences()
                + "\" location=\"" + searchParameters.getFastaFile().getAbsolutePath() + "\" "
                + "id=\"" + "SearchDB_1\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<FileFormat>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001348", "FASTA format", null));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FileFormat>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<DatabaseName>" + System.getProperty("line.separator"));
        tabCounter++;
        writeUserParam(searchParameters.getFastaFile().getName());
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
        br.write("<?xml version=\"1.1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator"));
        br.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.1 http://www.psidev.info/files/mzIdentML1.1.0.xsd\" "
                + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\" version=\"1.1.0\""
                + "creationDate=\"" + df.format(new Date()) + "\">"
                + System.getProperty("line.separator"));
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
                + "cvRef=\"" + cvTerm.getOntology() + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + cvTerm.getName() + "\"");

        if (cvTerm.getValue() != null) {
            br.write(" value=\"" + cvTerm.getValue() + "\"/>" + System.getProperty("line.separator"));
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
        br.write(getCurrentTabSpace() + "<userParam name=\"" + userParamAsString + "\"/>" + System.getProperty("line.separator"));
    }
}
