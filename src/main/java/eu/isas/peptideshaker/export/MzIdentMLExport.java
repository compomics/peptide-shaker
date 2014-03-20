package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.*;
import static com.compomics.util.experiment.identification.Advocate.OMSSA;
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
import com.compomics.util.pride.prideobjects.*;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
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
     * The references to include in the mzIdentML file.
     */
    private ReferenceGroup referenceGroup;
    /**
     * The contact utilities mzIdentML file.
     */
    private ContactGroup contactGroup;
    /**
     * THe sample utilities mzIdentML file.
     */
    private Sample sample;
    /**
     * The protocol utilities mzIdentML file.
     */
    private Protocol protocol;
    /**
     * The instrument utilities mzIdentML file.
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
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum key to mzIdentML spectrum index map - key: spectrum key,
     * element: mzIdentML file spectrum index.
     */
    private HashMap<String, Long> spectrumIndexes;
    /**
     * The PTM to mzIdentML map.
     */
    private PtmToPrideMap ptmToPrideMap; // @TODO: should be renamed!!!
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
     * The peptide IDs.
     */
    private HashMap<String, String> peptideIds = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param peptideShakerVersion
     * @param identification
     * @param projectDetails
     * @param experimentTitle Title of the experiment
     * @param spectrumCountingPreferences
     * @param identificationFeaturesGenerator
     * @param searchParameters
     * @param annotationPreferences
     * @param spectrumAnnotator
     * @param experimentLabel Label of the experiment
     * @param ptmScoringPreferences
     * @param experimentDescription Description of the experiment
     * @param experimentProject project of the experiment
     * @param referenceGroup References for the experiment
     * @param contactGroup Contacts for the experiment
     * @param sample Samples in this experiment
     * @param protocol Protocol used in this experiment
     * @param instrument Instruments used in this experiment
     * @param outputFolder Output folder
     * @param fileName the file name without extension
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
            AnnotationPreferences annotationPreferences, String experimentTitle, String experimentLabel, String experimentDescription, String experimentProject,
            ReferenceGroup referenceGroup, ContactGroup contactGroup, Sample sample, Protocol protocol, Instrument instrument,
            File outputFolder, String fileName, WaitingHandler waitingHandler) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.searchParameters = searchParameters;
        this.ptmScoringPreferences = ptmScoringPreferences;
        this.spectrumCountingPreferences = spectrumCountingPreferences;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.spectrumAnnotator = spectrumAnnotator;
        this.annotationPreferences = annotationPreferences;
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
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance(); // @TODO: should be renamed!!!
        ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        r = new FileWriter(new File(outputFolder, fileName + ".mzid"));
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

        // @TODO: also add SearchGUI and search engines used
        br.write(getCurrentTabSpace() + "<AnalysisSoftware "
                + "name=\"PeptideShaker \" "
                + "version=\"" + peptideShakerVersion + "\" "
                + "id=\"ID_software\" "
                + "uri=\"http://peptide-shaker.googlecode.com\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SoftwareName>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001476", "PeptideShaker", "PeptideShaker")); // @TODO: add PeptideShaker CV term
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SoftwareName>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PS_DEV\">" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001267", "software vendor", null));
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
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001271", "researcher", null));// @TODO: add the provider role here!!
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
                + "firstName=\"firstname\" " // @TODO: add from user input
                + "lastName=\"lastname\" " // @TODO: add from user input
                + "id=\"PROVIDER\">"
                + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>" + System.getProperty("line.separator"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "my_contact_address")); // @TODO: add from user input
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", "my_contact_url")); // @TODO: add from user input
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "someone@someuniversity.edu")); // @TODO: add from user input
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Person>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"myworkplace\" id=\"ORG_DOC_OWNER\">" + System.getProperty("line.separator")); // @TODO: add from user input
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "my_contact_name")); // @TODO: add from user input
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "my_contact_address")); // @TODO: add from user input
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", "my_contact_url")); // @TODO: add from user input
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "my_contact_email")); // @TODO: add from user input
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Organization>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">" + System.getProperty("line.separator")); // @TODO: add from user input
        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "my_contact_name")); // @TODO: add ps details?
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "my_contact_address")); // @TODO: add ps details?
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact url", "my_contact_url")); // @TODO: add ps details?
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "my_contact_email")); // @TODO: add ps details?
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
        }

        iterator.close();

        int peptideCounter = 0;

        // iterate all the peptides
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null); // @TODO: add waiting handler
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null); // @TODO: add waiting handler

            // iterate the psms
            for (String psmKey : identification.getSpectrumIdentification(spectrumFileName)) {

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
                Peptide peptide = bestPeptideAssumption.getPeptide();
                String peptideSequence = peptide.getSequence();
                peptideIds.put(peptide.getKey(), "Pep_" + ++peptideCounter);

                br.write(getCurrentTabSpace() + "<Peptide id=\"Pep_" + peptideCounter + "\" >" + System.getProperty("line.separator"));
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

                    br.write(getCurrentTabSpace() + "</Modification>" + System.getProperty("line.separator"));
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</Peptide>" + System.getProperty("line.separator"));
            }
        }

        int peptideEvidenceCounter = 0;

        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null); // @TODO: add waiting handler
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null); // @TODO: add waiting handler

            // iterate the psms
            for (String psmKey : identification.getSpectrumIdentification(spectrumFileName)) {

                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
                Peptide peptide = bestPeptideAssumption.getPeptide();

                // get all the possible parent proteins
                ArrayList<String> possibleProteins = peptide.getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

                // @TODO: only use the retained proteins??
//            List<String> retainedProteins = new ArrayList<String>();
//            for (String proteinKey : identification.getProteinIdentification()) {
//                for (String protein : possibleProteins) {
//                    if (!retainedProteins.contains(protein) && proteinKey.contains(protein)) {
//                        retainedProteins.add(protein);
//                        if (retainedProteins.size() == possibleProteins.size()) {
//                            break;
//                        }
//                    }
//                }
//            }
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

                        String pepEvidenceKey = tempProtein + "_" + peptideStart + "_" + peptideEnd + "_" + peptide.getKey();
                        pepEvidenceIds.put(pepEvidenceKey, "PepEv_" + ++peptideEvidenceCounter);

                        br.write(getCurrentTabSpace() + "<PeptideEvidence isDecoy=\"" + peptide.isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy()) + "\" "
                                + "pre=\"" + aaBefore + "\" "
                                + "post=\"" + aaAfter + "\" "
                                + "start=\"" + peptideStart + "\" "
                                + "end=\"" + peptideEnd + "\" "
                                + "peptide_ref=\"" + peptideIds.get(peptide.getKey()) + "\" "
                                + "dBSequence_ref=\"" + sequenceFactory.getProtein(tempProtein).getAccession() + "\" "
                                + "id=\"" + pepEvidenceIds.get(pepEvidenceKey) + "\" "
                                + "/>" + System.getProperty("line.separator"));
                    }
                }
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
                + "spectrumIdentificationList_ref=\"SII_LIST_1\" " // @TODO: should not be hardcoded?
                + "spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" " // @TODO: should not be hardcoded?
                + "id=\"SpecIdent_1\">" // @TODO: should not be hardcoded?
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<InputSpectra spectraData_ref=\"SID_1\"/>" + System.getProperty("line.separator")); // @TODO: should not be hardcoded?
        br.write(getCurrentTabSpace() + "<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>" + System.getProperty("line.separator")); // @TODO: should not be hardcoded?

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentification>" + System.getProperty("line.separator"));

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

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationProtocol "
                + "analysisSoftware_ref=\"ID_software\" id=\"SearchProtocol_1\">" + System.getProperty("line.separator"));
        tabCounter++;

        // the search type
        br.write(getCurrentTabSpace() + "<SearchType>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001083\" "
                + "cvRef=\"PSI-MS\" "
                + "name=\"ms-ms search\" />"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchType>" + System.getProperty("line.separator"));

        // the search parameters
        br.write(getCurrentTabSpace() + "<AdditionalSearchParams>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001211\" "
                + "cvRef=\"PSI-MS\" "
                + "name=\"parent mass type mono\" />"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001256\" "
                + "cvRef=\"PSI-MS\" "
                + "name=\"fragment mass type mono\" />"
                + System.getProperty("line.separator"));

        // @TODO: add more search parameters??
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
            
            // @TODO: add specificity rules?
//            <SpecificityRules>
//                    <cvParam accession="MS:1001189" cvRef="PSI-MS" name="modification specificity N-term"/>
//                </SpecificityRules

            CvTerm cvTerm = ptmToPrideMap.getCVTerm(ptm);
            if (cvTerm != null) {
                writeCvTerm(cvTerm);
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</SearchModification>" + System.getProperty("line.separator"));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ModificationParams>" + System.getProperty("line.separator"));

        // enzyme
        br.write(getCurrentTabSpace() + "<Enzymes independent=\"false\">" + System.getProperty("line.separator"));
        // If there are multiple enzymes specified, independent is set to true if cleavage with different enzymes is performed independently
        tabCounter++;

        Enzyme enzyme = searchParameters.getEnzyme();
        br.write(getCurrentTabSpace() + "<Enzyme "
                + "missedCleavages=\"" + searchParameters.getnMissedCleavages() + "\" "
                + "semiSpecific=\"" + enzyme.isSemiSpecific() + "\" "
                //+ "cTermGain=\"OH\" " // Element formula gained at CTerm
                //+ "nTermGain=\"H\" " // Element formula gained at NTerm
                + "id=\"Enz1\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<EnzymeName>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001251\" " // @TODO: set the enzyme cv term!!
                + "cvRef=\"PSI-MS\" "
                + "name=\"" + enzyme.getName() + "\" />"
                + System.getProperty("line.separator"));
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
        } else {
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
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ParentTolerance>" + System.getProperty("line.separator"));

        // thresholds
        br.write(getCurrentTabSpace() + "<Threshold>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001494\" "
                + "cvRef=\"PSI-MS\" "
                + "name=\"no threshold\" />" // @TODO: set from the search results!!
                + System.getProperty("line.separator"));

//        br.write(getCurrentTabSpace() + "<cvParam "
//                + "accession=\"MS:1002369\" "
//                + "cvRef=\"PSI-MS\" "
//                + "name=\"protein group-level global FDR\""
//                + "value=\"" + protein fdr + "\" />"
//                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Threshold>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationProtocol>" + System.getProperty("line.separator"));

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

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationList id=\"SII_LIST_1\" xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        writeFragmentationTable();

        int psmCount = 0;
        // iterate the spectrum files
        for (String spectrumFileName : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFileName, null); // @TODO: add waiting handler
            identification.loadSpectrumMatchParameters(spectrumFileName, new PSParameter(), null); // @TODO: add waiting handler

            // iterate the psms
            for (String psmKey : identification.getSpectrumIdentification(spectrumFileName)) {
                writeSpectrumIdentificationResult(spectrumFileName, psmKey, ++psmCount);
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
    private void writeProteinDetectionList() throws IOException {

        br.write(getCurrentTabSpace() + "<ProteinDetectionList id=\"Protein_groups\">" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: annotate the protein groups
//        <ProteinAmbiguityGroup id="PAG_hit_4" > # not always ambiguity in these groups...
//            
//            <ProteinDetectionHypothesis id="protein 1" passThreshold="true"> 
//                
//                <PeptideHypothesis peptideEvidence_ref="peptide a(1)"> #maps to protein 1
//                    <SpectrumIdentificationItemRef spectrumIdentificationItem_ref="SII_1" />
//                </PeptideHypothesis>
//
//            </ProteinDetectionHypothesis>
//            
//            <ProteinDetectionHypothesis id="protein 2" passThreshold="true"> 
//
//                <PeptideHypothesis peptideEvidence_ref="peptide a(2)"> #maps to protein 2
//                    <SpectrumIdentificationItemRef spectrumIdentificationItem_ref="SII_2" />
//                </PeptideHypothesis>
//
//            </ProteinDetectionHypothesis>
//            
//            <ProteinDetectionHypothesis id="protein 3" passThreshold="true"> ?
//
//                <PeptideHypothesis peptideEvidence_ref="peptide a(3)"> #maps to protein 3
//                    <SpectrumIdentificationItemRef spectrumIdentificationItem_ref="SII_3" />
//                </PeptideHypothesis>
//                
//            </ProteinDetectionHypothesis>
//            
//        </ProteinAmbiguityGroup>
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ProteinDetectionList>" + System.getProperty("line.separator"));
    }

    /**
     * Write a spectrum identification result.
     *
     * @throws IOException
     */
    private void writeSpectrumIdentificationResult(String spectrumFileName, String psmKey, int psmIndex) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(psmKey);
        String spectrumTitleHtml = StringEscapeUtils.escapeHtml4(spectrumTitle);
        String spectrumIdentificationResultItemKey = "SIR_" + psmIndex;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationResult "
                + "spectraData_ref=\"" + spectrumFileName
                + "\" spectrumID=\"" + "index=" + spectrumFactory.getSpectrumIndex(spectrumTitle, spectrumFileName)
                + "\" id=\"" + spectrumIdentificationResultItemKey + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: iterate all assumptions and not just the best one!
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();
        PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(psmKey, new PSParameter());
        int rank = 1; // @TODO: should not be hardcoded?
        String spectrumIdentificationItemKey = "SII_" + psmIndex + "_" + rank;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationItem "
                + "passThreshold=\"" + pSParameter.getMatchValidationLevel().isValidated() + "\" " // @TODO: is this correct??
                + "rank=\"" + rank + "\" " 
                + "peptide_ref=\"" + peptideIds.get(bestPeptideAssumption.getPeptide().getKey()) + "\" " 
                + "calculatedMassToCharge=\"" + bestPeptideAssumption.getTheoreticMz() + "\" "
                + "experimentalMassToCharge=\"" + spectrumFactory.getPrecursor(psmKey).getMz() + "\" "
                + "chargeState=\"" + bestPeptideAssumption.getIdentificationCharge().value + "\" "
                + "id=\"" + spectrumIdentificationItemKey + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        // add the peptide evidence references
        // get all the possible parent proteins
        ArrayList<String> possibleProteins = bestPeptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

        // @TODO: only use the retained proteins??
//            List<String> retainedProteins = new ArrayList<String>();
//            for (String proteinKey : identification.getProteinIdentification()) {
//                for (String protein : possibleProteins) {
//                    if (!retainedProteins.contains(protein) && proteinKey.contains(protein)) {
//                        retainedProteins.add(protein);
//                        if (retainedProteins.size() == possibleProteins.size()) {
//                            break;
//                        }
//                    }
//                }
//            }

        // iterate all the possible protein parents for each peptide
        for (String tempProtein : possibleProteins) {

            // get the start indexes and the surrounding 
            ArrayList<Integer> peptideStarts = sequenceFactory.getProtein(tempProtein).getPeptideStart(
                    bestPeptideAssumption.getPeptide().getSequence(), PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());

            for (int start : peptideStarts) {
                String pepEvidenceKey = tempProtein + "_" + start + "_" + (start + bestPeptideAssumption.getPeptide().getSequence().length() - 1)
                        + "_" + bestPeptideAssumption.getPeptide().getKey();
                String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);
                br.write(getCurrentTabSpace() + "<PeptideEvidenceRef peptideEvidence_ref=\"" + peptideEvidenceId + "\"/>" + System.getProperty("line.separator"));
            }
        }

        // add the fragment ion annotation
//          br.write(getCurrentTabSpace() + "<Fragmentation>" + System.getProperty("line.separator")); // @TODO: add the fragment ion annotation
//          tabCounter++;
//          <IonType charge="3" index="7 21">
//              <FragmentArray measure_ref="Measure_MZ" values="274.303 802.321"/>
//              <FragmentArray measure_ref="Measure_Int" values="2.0 6.0"/>
//              <FragmentArray measure_ref="Measure_Error" values="-0.176605 -0.035175"/>
//              <cvParam accession="MS:1001229" cvRef="PSI-MS" name="frag: a ion"/>
//          </IonType>
//          ...
//          tabCounter--;
//          br.write(getCurrentTabSpace() + "</Fragmentation>" + System.getProperty("line.separator"));
        // add cv and user params // @TODO: add cv and user params
        // example:
        writeCvTerm(new CvTerm("PSI-MS", "MS:1002356", "PSM-level combined FDRScore", String.valueOf(pSParameter.getPeptideConfidence()))); // @TODO: MS:1001143 (search engine specific score for peptides
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001117", "theoretical mass", String.valueOf(bestPeptideAssumption.getTheoreticMass()))); // @TODO: MS:1001105 (peptide result details
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationItem>" + System.getProperty("line.separator"));

        // add the spectrum title
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "spectrum title", spectrumTitleHtml));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationResult>" + System.getProperty("line.separator"));
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
                default:
                    br.write(getCurrentTabSpace() + "<userParam name=\"Unknown\"/>"); // @TODO: add cv term
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
        br.write(getCurrentTabSpace() + "<userParam name=\"" + searchParameters.getFastaFile().getName() + "\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</DatabaseName>" + System.getProperty("line.separator"));
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
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000774", "multiple peak list nativeID format", null)); // @TODO: not sure about this one...
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
     * @return the reference group
     */
    public ReferenceGroup getReferenceGroup() {
        return referenceGroup;
    }

    /**
     * @param referenceGroup the references group to set
     */
    public void setReferenceGroup(ReferenceGroup referenceGroup) {
        this.referenceGroup = referenceGroup;
    }

    /**
     * @return the contact group
     */
    public ContactGroup getContactGroup() {
        return contactGroup;
    }

    /**
     * @param contactGroup the contact group to set
     */
    public void setContactGroup(ContactGroup contactGroup) {
        this.contactGroup = contactGroup;
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
            br.write(" value=\"" + cvTerm.getValue() + "\" />" + System.getProperty("line.separator"));
        } else {
            br.write(" />" + System.getProperty("line.separator"));
        }
    }

    /////////////////////////////////////////////
    // the code below this point is old pride code
    /////////////////////////////////////////////
    /**
     * Writes the fragment ions for a given spectrum match.
     *
     * @param spectrumMatch the spectrum match considered
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     * @throws MzMLUnmarshallerException exception thrown whenever a problem
     * occurred while reading the mzML file
     */
    private void writeFragmentIons(SpectrumMatch spectrumMatch) throws IOException, MzMLUnmarshallerException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        annotationPreferences.setCurrentSettings(spectrumMatch.getBestPeptideAssumption(), true, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
        MSnSpectrum tempSpectrum = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey()));

        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges(),
                spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
                tempSpectrum, peptide,
                tempSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                annotationPreferences.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());

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
        CvTerm fragmentIonTerm = ionMatch.ion.getPrideCvTerm(); // @TODO: should be renamed!!!

        if (fragmentIonTerm != null) {
            if (ionMatch.ion.getType() == IonType.PEPTIDE_FRAGMENT_ION
                    || ionMatch.ion.getType() == IonType.IMMONIUM_ION
                    || ionMatch.ion.getType() == IonType.PRECURSOR_ION
                    || ionMatch.ion.getType() == IonType.REPORTER_ION) {
                br.write(getCurrentTabSpace() + "<FragmentIon>" + System.getProperty("line.separator"));
                tabCounter++;
                writeCvTerm(fragmentIonTerm);
                writeCvTerm(ionMatch.getMZPrideCvTerm()); // @TODO: should be renamed!!!
                writeCvTerm(ionMatch.getIntensityPrideCvTerm()); // @TODO: should be renamed!!!
                writeCvTerm(ionMatch.getIonMassErrorPrideCvTerm()); // @TODO: should be renamed!!!
                writeCvTerm(ionMatch.getChargePrideCvTerm()); // @TODO: should be renamed!!!
                tabCounter--;
                br.write(getCurrentTabSpace() + "</FragmentIon>" + System.getProperty("line.separator"));
            }
        }
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
}
