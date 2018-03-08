package eu.isas.peptideshaker.export;

import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.ions.Ion.IonType;
import com.compomics.util.experiment.biology.ions.NeutralLoss;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.ions.impl.ImmoniumIon;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.impl.PrecursorIon;
import com.compomics.util.experiment.biology.ions.impl.RelatedIon;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.modification.ModificationLocalizationScore;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.parameters.identification.advanced.IdMatchValidationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.io.identification.MzIdentMLVersion;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSModificationScores;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.scoring.ModificationScoring;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * The class that takes care of converting the data to mzIdentML.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class MzIdentMLExport {

    /**
     * The buffered writer which will write the results in the desired file.
     */
    private BufferedWriter bw;
    /**
     * Encoding for the file, cf the second rule.
     */
    public static final String encoding = "UTF-8";
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
    private ModificationFactory modificationFactory = ModificationFactory.getInstance();
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
     * The protein sequence provider.
     */
    private SequenceProvider sequenceProvider;
    /**
     * Summary information on the protein sequences file.
     */
    private FastaSummary fastaSummary;
    /**
     * A protein details provider.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
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
    private HashMap<String, String> pepEvidenceIds = new HashMap<>();
    /**
     * The spectrum IDs.
     */
    private HashMap<Long, String> spectrumIds = new HashMap<>();
    /**
     * The spectrum key to parent peptide key map.
     */
    private HashMap<Long, Long> spectrumKeyToPeptideKeyMap;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * Map of PTM indexes: PTM mass to index.
     */
    private HashMap<Double, Integer> modIndexMap = new HashMap<>();
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
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the sequence provider
     * @param fastaSummary summary information on the protein sequences file
     * @param proteinDetailsProvider the protein details provider
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param outputFile the output file
     * @param includeProteinSequences if true, the protein sequences are
     * included in the output
     * @param waitingHandler waiting handler used to display progress to the
     * user and interrupt the process
     * @param gzip if true export as gzipped file
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    public MzIdentMLExport(String peptideShakerVersion, Identification identification,
            ProjectDetails projectDetails, IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider,
            FastaSummary fastaSummary, IdentificationFeaturesGenerator identificationFeaturesGenerator, 
            File outputFile, boolean includeProteinSequences, 
            WaitingHandler waitingHandler, boolean gzip) throws IOException {

        if (outputFile.getParent() == null) {
            
            throw new FileNotFoundException("The file " + outputFile + " does not have a valid parent folder. Please make sure that the parent folder exists.");
        
        }

        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.identificationParameters = identificationParameters;
        this.sequenceProvider = sequenceProvider;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.fastaSummary = fastaSummary;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.includeProteinSequences = includeProteinSequences;
        this.waitingHandler = waitingHandler;
        this.peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        if (gzip) {

            // Setup the writer
            FileOutputStream fileStream = new FileOutputStream(outputFile);
            GZIPOutputStream gzipStream = new GZIPOutputStream(fileStream);
            OutputStreamWriter encoder = new OutputStreamWriter(gzipStream, encoding);
            bw = new BufferedWriter(encoder);

        } else {

            bw = new BufferedWriter(new FileWriter(outputFile));

        }
    }

    /**
     * Creates the mzIdentML file.
     *
     * @param mzIdentMLVersion The version of mzIdentML to use
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    public void createMzIdentMLFile(MzIdentMLVersion mzIdentMLVersion) throws IOException {

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
        waitingHandler.setMaxPrimaryProgressCounter(fastaSummary.nSequences
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

        // the experiment end tag
        writeMzIdentMLEndTag();

        bw.close();

    }

    /**
     * Writes the CV list.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeCvList() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<cvList>");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<cv id=\"PSI-MS\" ");
        bw.write("uri=\"https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo\" ");
        bw.write("fullName=\"PSI-MS\"/>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<cv id=\"UNIMOD\" ");
        bw.write("uri=\"http://www.unimod.org/obo/unimod.obo\" ");
        bw.write("fullName=\"UNIMOD\"/>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<cv id=\"UO\" ");
        bw.write("uri=\"https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo\" ");
        bw.write("fullName=\"UNIT-ONTOLOGY\"/>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<cv id=\"PRIDE\" ");
        bw.write("uri=\"https://github.com/PRIDE-Utilities/pride-ontology/blob/master/pride_cv.obo\" ");
        bw.write("fullName=\"PRIDE\"/>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</cvList>");
        bw.newLine();

    }

    /**
     * Write the software list.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisSoftwareList() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<AnalysisSoftwareList>");
        bw.newLine();
        tabCounter++;

        // @TODO: also add SearchGUI and/or search engines used?
        bw.write(getCurrentTabSpace());
        bw.write("<AnalysisSoftware name=\"PeptideShaker\" version=\"");
        bw.write(peptideShakerVersion);
        bw.write("\" id=\"ID_software\" uri=\"http://compomics.github.io/projects/peptide-shaker.html\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<ContactRole contact_ref=\"PS_DEV\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<Role>");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1001267", "software vendor", null));
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</Role>");
        bw.newLine();
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</ContactRole>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<SoftwareName>");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1002458", "PeptideShaker", null));
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</SoftwareName>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<Customizations>No customisations</Customizations>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AnalysisSoftware>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AnalysisSoftwareList>");
        bw.newLine();

    }

    /**
     * Write the provider details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeProviderDetails() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<Provider id=\"PROVIDER\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<ContactRole contact_ref=\"PROVIDER\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<Role>");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1001271", "researcher", null)); // @TODO: add user defined provider role?
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</Role>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ContactRole>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Provider>");
        bw.newLine();

    }

    /**
     * Write the audit collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAuditCollection() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<AuditCollection>");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<Person firstName=\"");
        bw.write(projectDetails.getContactFirstName());
        bw.write("\" lastName=\"");
        bw.write(projectDetails.getContactLastName());
        bw.write("\"id=\"PROVIDER\">");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getContactAddress()));

        if (projectDetails.getContactUrl() != null && !projectDetails.getContactUrl().isEmpty()) {

            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", projectDetails.getContactUrl()));

        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getContactEmail()));

        bw.write(getCurrentTabSpace());
        bw.write("<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>");
        bw.newLine();
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</Person>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<Organization name=\"");
        bw.write(projectDetails.getOrganizationName());
        bw.write("\" id=\"ORG_DOC_OWNER\">");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", projectDetails.getOrganizationName()));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", projectDetails.getOrganizationAddress()));

        if (projectDetails.getOrganizationUrl() != null && !projectDetails.getOrganizationUrl().isEmpty()) {

            writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", projectDetails.getOrganizationUrl()));

        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", projectDetails.getOrganizationEmail()));
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</Organization>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1000586", "contact name", "PeptideShaker developers"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000587", "contact address", "Proteomics Unit, Building for Basic Biology, University of Bergen, Jonas Liesvei 91, N-5009 Bergen, Norway"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000588", "contact URL", "http://compomics.github.io/projects/peptide-shaker.html"));
        writeCvTerm(new CvTerm("PSI-MS", "MS:1000589", "contact email", "peptide-shaker@googlegroups.com"));
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</Organization>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AuditCollection>");
        bw.newLine();

    }

    /**
     * Write the sequence collection.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    private void writeSequenceCollection() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<SequenceCollection>");
        bw.newLine();
        tabCounter++;

//        String dbType = Header.getDatabaseTypeAsString(Header.DatabaseType.Unknown); // @TODO: add database type as cv param? children of MS:1001013 (database name)
//        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();
//        if (fastaIndex != null) {
//            dbType = Header.getDatabaseTypeAsString(fastaIndex.getDatabaseType());
//        }
//
        // iterate all the protein sequences
        for (String accession : sequenceProvider.getAccessions()) {

            bw.write(getCurrentTabSpace());
            bw.write("<DBSequence id=\"");
            bw.write(accession);
            bw.write("\" ");
            bw.write("accession=\"");
            bw.write(accession);
            bw.write("\" searchDatabase_ref=\"SearchDB_1\" >");
            bw.newLine();
            tabCounter++;

            if (includeProteinSequences) {

                String sequence = sequenceProvider.getSequence(accession);
                bw.write(getCurrentTabSpace());
                bw.write("<Seq>");
                bw.write(sequence);
                bw.write("</Seq>");
                bw.newLine();

            }

            String description = proteinDetailsProvider.getDescription(accession);
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001088", "protein description", StringEscapeUtils.escapeHtml4(description)));

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</DBSequence>");
            bw.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        // set up the spectrum key to peptide key map
        spectrumKeyToPeptideKeyMap = new HashMap<>(identification.getSpectrumIdentificationSize());

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getPeptide();
            String peptideSequence = peptide.getSequence();

            // store the spectrum to peptide mapping for later
            for (long spectrumMatchKey : peptideMatch.getSpectrumMatchesKeys()) {

                spectrumKeyToPeptideKeyMap.put(spectrumMatchKey, peptideKey);

            }

            bw.write(getCurrentTabSpace());
            bw.write("<Peptide id=\"");
            bw.write(Long.toString(peptideKey));
            bw.write("\">");
            bw.newLine();
            tabCounter++;

            bw.write(getCurrentTabSpace());
            bw.write("<PeptideSequence>");
            bw.write(peptideSequence);
            bw.write("</PeptideSequence>");
            bw.newLine();

            for (ModificationMatch modMatch : peptide.getModificationMatches()) {

                Modification modification = modificationFactory.getModification(modMatch.getModification());

                int site;
                if (modification.getModificationType().isNTerm()) {

                    site = 0;

                } else if (modification.getModificationType().isCTerm()) {

                    site = peptideSequence.length() + 1;

                } else {

                    site = modMatch.getModificationSite();

                }

                bw.write(getCurrentTabSpace());
                bw.write("<Modification monoisotopicMassDelta=\"");
                bw.write(Double.toString(modification.getRoundedMass()));
                bw.write("\" residues=\"");
                bw.write(peptideSequence.charAt(modMatch.getModificationSite() - 1));
                bw.write("\" location=\"");
                bw.write(site);
                bw.write("\" >");
                bw.newLine();

                CvTerm ptmCvTerm = modification.getCvTerm();

                if (ptmCvTerm != null) {

                    tabCounter++;
                    writeCvTerm(ptmCvTerm, false);
                    tabCounter--;

                }

                bw.write(getCurrentTabSpace());
                bw.write("</Modification>");
                bw.newLine();

            }

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</Peptide>");
            bw.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        int peptideEvidenceCounter = 0;
        int nAa = 1;

        // re-iterate the peptides to write peptide to protein mapping
        peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getPeptide();

            // get the possible parent proteins
            TreeMap<String, int[]> proteinMapping = peptide.getProteinMapping();

            // iterate the possible protein parents
            for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

                String accession = entry.getKey();
                int[] indexes = entry.getValue();

                for (int index : indexes) {

                    String aaBefore = PeptideUtils.getAaBefore(peptide, accession, index, nAa, sequenceProvider);

                    if (aaBefore.length() == 0) {

                        aaBefore = "-";

                    }

                    String aaAfter = PeptideUtils.getAaAfter(peptide, accession, index, nAa, sequenceProvider);

                    if (aaAfter.length() == 0) {

                        aaAfter = "-";

                    }

                    int peptideStart = index;
                    int peptideEnd = index + peptide.getSequence().length();

                    String pepEvidenceKey = getPeptideEvidenceKey(accession, peptideStart, peptideKey);

                    StringBuilder pepEvidenceValueBuilder = new StringBuilder();
                    pepEvidenceValueBuilder.append("PepEv_")
                            .append(++peptideEvidenceCounter);
                    String pepEvidenceValue = pepEvidenceValueBuilder.toString();

                    pepEvidenceIds.put(pepEvidenceKey, pepEvidenceValue);

                    bw.write(getCurrentTabSpace());
                    bw.write("<PeptideEvidence isDecoy=\"");
                    bw.write(Boolean.toString(peptideMatch.getIsDecoy()));
                    bw.write("\" pre=\"");
                    bw.write(aaBefore);
                    bw.write("\" post=\"");
                    bw.write(aaAfter);
                    bw.write("\" start=\"");
                    bw.write(peptideStart);
                    bw.write("\" end=\"");
                    bw.write(peptideEnd);
                    bw.write("\" peptide_ref=\"");
                    bw.write(Long.toString(peptideKey));
                    bw.write("\" dBSequence_ref=\"");
                    bw.write(accession);
                    bw.write("\" id=\"");
                    bw.write(pepEvidenceValue);
                    bw.write("\" />");
                    bw.newLine();

                }
            }

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SequenceCollection>");
        bw.newLine();

    }

    /**
     * Returns the peptide evidence key as string for the given peptide
     * attributes.
     *
     * @param accession the protein accession
     * @param peptideStart the index of the peptide on the protein sequence
     * @param peptideKey the peptide match key
     *
     * @return the peptide evidence key as string for the given peptide
     * attributes
     */
    public static String getPeptideEvidenceKey(String accession, int peptideStart, long peptideKey) {

        String peptideStartAsString = Integer.toString(peptideStart);
        String peptideKeyAsString = Long.toString(peptideKey);

        StringBuilder pepEvidenceKeybuilder = new StringBuilder(accession.length()
                + peptideStartAsString.length() + peptideKeyAsString.length() + 2);

        pepEvidenceKeybuilder.append(accession)
                .append('_')
                .append(peptideStartAsString)
                .append('_')
                .append(peptideKeyAsString);

        return pepEvidenceKeybuilder.toString();
    }

    /**
     * Write the analysis collection.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisCollection() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<AnalysisCollection>");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<SpectrumIdentification spectrumIdentificationList_ref=\"SIL_1\" ");
        bw.write("spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" id=\"SpecIdent_1\">");
        bw.newLine();
        tabCounter++;

        // iterate the spectrum files and add the file name refs
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {

            bw.write(getCurrentTabSpace());
            bw.write("<InputSpectra spectraData_ref=\"");
            bw.write(mgfFileName);
            bw.write("\"/>");
            bw.newLine();

        }

        bw.write(getCurrentTabSpace());
        bw.write("<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SpectrumIdentification>");
        bw.newLine();

        // add protein detection
        // @TODO: add activityDate? example: activityDate="2011-03-25T13:33:51
        bw.write(getCurrentTabSpace());
        bw.write("<ProteinDetection proteinDetectionProtocol_ref=\"PeptideShaker_1\" ");
        bw.write("proteinDetectionList_ref=\"Protein_groups\" id=\"PD_1\">");
        bw.newLine();

        tabCounter++;
        bw.write(getCurrentTabSpace());
        bw.write("<InputSpectrumIdentifications spectrumIdentificationList_ref=\"SIL_1\"/>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ProteinDetection>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AnalysisCollection>");
        bw.newLine();

    }

    /**
     * Write the analysis protocol.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeAnalysisProtocol() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<AnalysisProtocolCollection>");
        bw.newLine();
        tabCounter++;

        // add spectrum identification protocol
        bw.write(getCurrentTabSpace());
        bw.write("<SpectrumIdentificationProtocol analysisSoftware_ref=\"ID_software\" id=\"SearchProtocol_1\">");
        bw.newLine();
        tabCounter++;

        // the search type
        bw.write(getCurrentTabSpace());
        bw.write("<SearchType>");
        bw.newLine();

        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001083", "ms-ms search", null));

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SearchType>");
        bw.newLine();

        // the search parameters
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        bw.write(getCurrentTabSpace());
        bw.write("<AdditionalSearchParams>");
        bw.newLine();

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
        bw.write(getCurrentTabSpace());
        bw.write("</AdditionalSearchParams>");
        bw.newLine();

        // the modifications
        bw.write(getCurrentTabSpace());
        bw.write("<ModificationParams>");
        bw.newLine();
        tabCounter++;

        // create the ptm index map
        switch (mzIdentMLVersion) {

            case v1_1:
                break;

            case v1_2:

                for (String modName : searchParameters.getModificationParameters().getAllModifications()) {

                    Modification modification = modificationFactory.getModification(modName);
                    Double modMass = modification.getMass();
                    Integer index = modIndexMap.get(modMass);

                    if (index == null) {

                        modIndexMap.put(modMass, modIndexMap.size());

                    }
                }

                break;

            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");

        }

        // iterate and add the modifications
        for (String modName : searchParameters.getModificationParameters().getAllModifications()) {

            Modification modification = modificationFactory.getModification(modName);
            ModificationType modificationType = modification.getModificationType();
            double modMass = modification.getMass();

            String aminoAcidsAtTarget;

            if (modificationType == ModificationType.modaa
                    || modificationType == ModificationType.modcaa_peptide
                    || modificationType == ModificationType.modcaa_protein
                    || modificationType == ModificationType.modnaa_peptide
                    || modificationType == ModificationType.modnaa_protein) {

                StringBuilder sb = new StringBuilder();

                for (Character aa : modification.getPattern().getAminoAcidsAtTarget()) {

                    sb.append(aa);

                }

                aminoAcidsAtTarget = sb.toString();

            } else {

                aminoAcidsAtTarget = ".";

            }

            bw.write(getCurrentTabSpace());
            bw.write("<SearchModification residues=\"");
            bw.write(aminoAcidsAtTarget);
            bw.write("\" massDelta=\"");
            bw.write(Double.toString(modification.getRoundedMass()));
            bw.write("\" fixedMod= \"");
            bw.write(Boolean.toString(searchParameters.getModificationParameters().getFixedModifications().contains(modName)));
            bw.write("\" >");
            bw.newLine();
            tabCounter++;

            // add modification specificity
            if (modificationType != ModificationType.modaa) {

                bw.write(getCurrentTabSpace());
                bw.write("<SpecificityRules>");
                bw.newLine();
                tabCounter++;

                switch (modificationType) {

                    case modn_protein:
                    case modnaa_protein:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002057", "modification specificity protein N-term", null));
                        break;

                    case modn_peptide:
                    case modnaa_peptide:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001189", "modification specificity peptide N-term", null));
                        break;

                    case modc_protein:
                    case modcaa_protein:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002058", "modification specificity protein C-term", null));
                        break;

                    case modc_peptide:
                    case modcaa_peptide:
                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001190", "modification specificity peptide C-term", null));
                        break;

                    default:
                        break;
                }

                tabCounter--;
                bw.write(getCurrentTabSpace());
                bw.write("</SpecificityRules>");
                bw.newLine();

            }

            // add the modification cv term
            CvTerm ptmCvTerm = modification.getCvTerm();

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

                    Integer modIndex = modIndexMap.get(modMass);

                    if (modIndex == null) {

                        throw new IllegalArgumentException("No index found for PTM " + modification.getName() + " of mass " + modMass + ".");

                    }

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002504", "modification index", modIndex.toString()));
                    break;

                default:
                    throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
            }

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</SearchModification>");
            bw.newLine();

        }

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ModificationParams>");
        bw.newLine();

        // Digestion
        DigestionParameters digestionPreferences = searchParameters.getDigestionParameters();

        if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.unSpecific) {

            bw.write(getCurrentTabSpace());
            bw.write("<Enzymes independent=\"false\">");
            bw.newLine();

            tabCounter++;
            bw.write(getCurrentTabSpace());
            bw.write("<Enzyme name=\"unspecific cleavage\">");
            bw.newLine();

            tabCounter++;
            bw.write(getCurrentTabSpace());
            bw.write("<EnzymeName>");
            bw.newLine();

            tabCounter++;
            CvTerm enzymeCvTerm = new CvTerm("PSI-MS", "MS:1001091", "unspecific cleavage", null);
            writeCvTerm(enzymeCvTerm);

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</EnzymeName>");
            bw.newLine();

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</Enzyme>");
            bw.newLine();

        } else if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.wholeProtein) {

            bw.write(getCurrentTabSpace());
            bw.write("<Enzymes independent=\"false\">");
            bw.newLine();
            tabCounter++;

            bw.write(getCurrentTabSpace());
            bw.write("<Enzyme name=\"NoEnzyme\">");
            bw.newLine();
            tabCounter++;

            bw.write(getCurrentTabSpace());
            bw.write("<EnzymeName>");
            bw.newLine();
            tabCounter++;

            CvTerm enzymeCvTerm = new CvTerm("PSI-MS", "MS:1001955", "NoEnzyme", null);
            writeCvTerm(enzymeCvTerm);

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</EnzymeName>");
            bw.newLine();

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</Enzyme>");
            bw.newLine();

        } else {

            ArrayList<Enzyme> enzymes = digestionPreferences.getEnzymes();

            bw.write(getCurrentTabSpace());
            bw.write("<Enzymes independent=\"");
            bw.write(Boolean.toString(enzymes.size() > 1));
            bw.write("\">");
            bw.newLine();

            for (Enzyme enzyme : enzymes) {

                String enzymeName = enzyme.getName();
                bw.write(getCurrentTabSpace());
                bw.write("<Enzyme missedCleavages=\"");
                bw.write(digestionPreferences.getnMissedCleavages(enzymeName));
                bw.write("\" semiSpecific=\"");
                bw.write(Boolean.toString(digestionPreferences.getSpecificity(enzymeName) == DigestionParameters.Specificity.semiSpecific));
                bw.write("\" ");
                //+ "cTermGain=\"OH\" " // Element formula gained at CTerm
                //+ "nTermGain=\"H\" " // Element formula gained at NTerm
                // @TODO: add <SiteRegexp><![CDATA[(?<=[KR])(?!P)]]></SiteRegexp>?
                bw.write("id=\"Enz1\" name=\"");
                bw.write(enzyme.getName());
                bw.write("\">");
                bw.newLine();
                tabCounter++;

                bw.write(getCurrentTabSpace());
                bw.write("<EnzymeName>");
                bw.newLine();

                tabCounter++;
                CvTerm enzymeCvTerm = enzyme.getCvTerm();

                if (enzymeCvTerm != null) {

                    writeCvTerm(enzymeCvTerm);

                } else {

                    writeUserParam(enzyme.getName());

                }

                tabCounter--;
                bw.write(getCurrentTabSpace());
                bw.write("</EnzymeName>");
                bw.newLine();

                tabCounter--;
                bw.write(getCurrentTabSpace());
                bw.write("</Enzyme>");
                bw.newLine();

                tabCounter--;

            }
        }

        bw.write(getCurrentTabSpace());
        bw.write("</Enzymes>");
        bw.newLine();

        // fragment tolerance
        bw.write(getCurrentTabSpace());
        bw.write("<FragmentTolerance>");
        bw.newLine();
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

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam accession=\"MS:1001412\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        bw.write(fragmentIonToleranceUnit);
        bw.write("\" unitAccession=\"");
        bw.write(unitAccession);
        bw.write("\" value=\"");
        bw.write(Double.toString(searchParameters.getFragmentIonAccuracy()));
        bw.write("\" ");
        bw.write("name=\"search tolerance plus value\" />");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam accession=\"MS:1001413\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        bw.write(fragmentIonToleranceUnit);
        bw.write("\" unitAccession=\"");
        bw.write(unitAccession);
        bw.write("\" value=\"");
        bw.write(Double.toString(searchParameters.getFragmentIonAccuracy()));
        bw.write("\" name=\"search tolerance minus value\" />");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</FragmentTolerance>");
        bw.newLine();

        // precursor tolerance
        bw.write(getCurrentTabSpace());
        bw.write("<ParentTolerance>");
        bw.newLine();
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

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam accession=\"MS:1001412\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        bw.write(precursorIonToleranceUnit);
        bw.write("\" unitAccession=\"UO:0000169\" value=\"");
        bw.write(Double.toString(searchParameters.getPrecursorAccuracy()));
        bw.write("\" name=\"search tolerance plus value\" />");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam accession=\"MS:1001413\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        bw.write(precursorIonToleranceUnit);
        bw.write("\" unitAccession=\"UO:0000169\" value=\"");
        bw.write(Double.toString(searchParameters.getPrecursorAccuracy()));
        bw.write("\" name=\"search tolerance minus value\" />");

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ParentTolerance>");
        bw.newLine();

        // thresholds
        bw.write(getCurrentTabSpace());
        bw.write("<Threshold>");
        bw.newLine();
        tabCounter++;

        boolean targetDecoy = identificationParameters.getSearchParameters().getFastaParameters().isTargetDecoy();

        if (!targetDecoy) {

            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));

        } else {

            // Initial global thresholds
            IdMatchValidationParameters idMatchValidationPreferences = identificationParameters.getIdValidationParameters();
            writeCvTerm(new CvTerm("PSI-MS", "MS:1001364", "peptide sequence-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPeptideFDR(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002350", "PSM-level global FDR", Double.toString(Util.roundDouble(idMatchValidationPreferences.getDefaultPsmFDR(), CONFIDENCE_DECIMALS))));

            ModificationLocalizationParameters ptmScoringPreferences = identificationParameters.getModificationLocalizationParameters();

            if (ptmScoringPreferences.isProbabilisticScoreCalculation()) {

                if (ptmScoringPreferences.getSelectedProbabilisticScore() == ModificationLocalizationScore.PhosphoRS) {

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
        bw.write(getCurrentTabSpace());
        bw.write("</Threshold>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SpectrumIdentificationProtocol>");
        bw.newLine();

        // add ProteinDetectionProtocol
        bw.write(getCurrentTabSpace());
        bw.write("<ProteinDetectionProtocol analysisSoftware_ref=\"ID_software\" id=\"PeptideShaker_1\">");
        bw.newLine();
        tabCounter++;

//        br.write(getCurrentTabSpace() + "<AnalysisParams>" + lineBreak);
//        tabCounter++;
        // @TODO: add cv terms? (children of MS:1001302)
//        tabCounter--;
//        br.write(getCurrentTabSpace() + "</AnalysisParams>" + lineBreak);
        // protein level threshold
        bw.write(getCurrentTabSpace());
        bw.write("<Threshold>");
        bw.newLine();
        tabCounter++;

        if (!targetDecoy) {

            writeCvTerm(new CvTerm("PSI-MS", "MS:1001494", "no threshold", null));

        } else {

            PSMaps psMaps = new PSMaps();
            psMaps = (PSMaps) identification.getUrParam(psMaps);
            TargetDecoyMap proteinMap = psMaps.getProteinMap();
            TargetDecoyResults proteinTargetDecoyResults = proteinMap.getTargetDecoyResults();

            double threshold = proteinTargetDecoyResults.getUserInput() / 100;
            int thresholdType = proteinTargetDecoyResults.getInputType();

            if (thresholdType == 0) {

                writeCvTerm(new CvTerm("PSI-MS", "MS:1002461", "protein group-level global confidence", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // confidence

            } else if (proteinTargetDecoyResults.getInputType() == 1) {

                writeCvTerm(new CvTerm("PSI-MS", "MS:1002369", "protein group-level global FDR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FDR

            } else if (proteinTargetDecoyResults.getInputType() == 2) {

                writeCvTerm(new CvTerm("PSI-MS", "MS:1002460", "protein group-level global FNR", Double.toString(Util.roundDouble(threshold, CONFIDENCE_DECIMALS)))); // FNR

            }
        }

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Threshold>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ProteinDetectionProtocol>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AnalysisProtocolCollection>");
        bw.newLine();

    }

    /**
     * Write the data collection.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    private void writeDataCollection() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<DataCollection>");
        bw.newLine();
        tabCounter++;

        writeInputFileDetails();
        writeDataAnalysis();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</DataCollection>");
        bw.newLine();

    }

    /**
     * Write the data analysis section.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    private void writeDataAnalysis() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<AnalysisData>");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<SpectrumIdentificationList id=\"SIL_1\">");
        bw.newLine();
        tabCounter++;

        writeFragmentationTable();

        int psmCount = 0;

        // iterate the PSMs
        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            long spectrumMatchKey = spectrumMatch.getKey();

            writeSpectrumIdentificationResult(spectrumMatchKey, ++psmCount);
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        //writeCvTerm(new CvTerm("PSI-MS", "MS:1002439", "final PSM list", null)); // @TODO: add children of MS:1001184 (search statistics)?
        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SpectrumIdentificationList>");
        bw.newLine();

        writeProteinDetectionList();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</AnalysisData>");
        bw.newLine();

    }

    /**
     * Write the protein groups.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    private void writeProteinDetectionList() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<ProteinDetectionList id=\"Protein_groups\">");
        bw.newLine();

        tabCounter++;

        int groupCpt = 0;

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

        ProteinMatch proteinMatch;
        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            long proteinGroupKey = proteinMatch.getKey();

            String proteinGroupId = "PAG_" + groupCpt++;

            bw.write(getCurrentTabSpace());
            bw.write("<ProteinAmbiguityGroup id=\"");
            bw.write(proteinGroupId);
            bw.write("\">");
            bw.newLine();
            tabCounter++;

            PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            String[] accessions = proteinMatch.getAccessions();
            String mainAccession = proteinMatch.getLeadingAccession();

            for (int j = 0; j < accessions.length; j++) {

                String accession = accessions[j];

                bw.write(getCurrentTabSpace());
                bw.write("<ProteinDetectionHypothesis id=\"");
                bw.write(proteinGroupId);
                bw.write('_');
                bw.write((j + 1));
                bw.write("\" dBSequence_ref=\"");
                bw.write(accession);
                bw.write("\" passThreshold=\"");
                bw.write(Boolean.toString(psParameter.getMatchValidationLevel().isValidated()));
                bw.write("\">");
                bw.newLine();
                tabCounter++;

                long[] peptideMatches = proteinMatch.getPeptideMatchesKeys();
                PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(peptideMatches, waitingHandler);
                PeptideMatch peptideMatch;

                while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                    long peptideKey = peptideMatch.getKey();
                    Peptide peptide = peptideMatch.getPeptide();

                    int[] indexes = peptide.getProteinMapping().get(accession);

                    for (int index : indexes) {

                        String pepEvidenceKey = getPeptideEvidenceKey(accession, index, peptideKey);
                        String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);

                        if (peptideEvidenceId != null) {

                            bw.write(getCurrentTabSpace());
                            bw.write("<PeptideHypothesis peptideEvidence_ref=\"");
                            bw.write(peptideEvidenceId);
                            bw.write("\">");
                            bw.newLine();
                            tabCounter++;

                            for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                                bw.write(getCurrentTabSpace());
                                bw.write("<SpectrumIdentificationItemRef spectrumIdentificationItem_ref=\"");
                                bw.write(spectrumIds.get(spectrumKey));
                                bw.write("\"/>");
                                bw.newLine();

                            }

                            tabCounter--;
                            bw.write(getCurrentTabSpace());
                            bw.write("</PeptideHypothesis>");
                            bw.newLine();

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

                    double validatedCoverage = identificationFeaturesGenerator.getValidatedSequenceCoverage(proteinGroupKey);
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001093", "sequence coverage",
                            Double.toString(Util.roundDouble(validatedCoverage, CONFIDENCE_DECIMALS))));

                }

                tabCounter--;
                bw.write(getCurrentTabSpace());
                bw.write("</ProteinDetectionHypothesis>");
                bw.newLine();

            }

            // add protein group cv terms
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002470", "PeptideShaker protein group score",
                    Double.toString(Util.roundDouble(psParameter.getScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002471", "PeptideShaker protein group confidence",
                    Double.toString(Util.roundDouble(psParameter.getConfidence(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002542", "PeptideShaker protein confidence type",
                    psParameter.getMatchValidationLevel().getName()));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002415", "protein group passes threshold",
                    Boolean.toString(psParameter.getMatchValidationLevel().isValidated())));

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</ProteinAmbiguityGroup>");
            bw.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        writeCvTerm(new CvTerm("PSI-MS", "MS:1002404", "count of identified proteins",
                Integer.toString(identificationFeaturesGenerator.getNValidatedProteins())));
        // @TODO: add children of MS:1001184 - search statistics? (date / time search performed, number of molecular hypothesis considered, search time taken)

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</ProteinDetectionList>");
        bw.newLine();

    }

    /**
     * Write a spectrum identification result.
     *
     * @param spectrumMatchKey the key of the spectrum match to write
     * @param spectrumMatchIndex the index of the spectrum match
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading/writing a file
     */
    private void writeSpectrumIdentificationResult(long spectrumMatchKey, int spectrumMatchIndex)
            throws IOException {

        SpectrumMatch spectrumMatch = (SpectrumMatch) identification.retrieveObject(spectrumMatchKey);

        String spectrumKey = spectrumMatch.getSpectrumKey();
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumIdentificationResultItemKey = "SIR_" + spectrumMatchIndex;

        bw.write(getCurrentTabSpace());
        bw.write("<SpectrumIdentificationResult spectraData_ref=\"");
        bw.write(spectrumFileName);
        bw.write("\" spectrumID=\"index=");
        bw.write(spectrumFactory.getSpectrumIndex(spectrumTitle, spectrumFileName));
        bw.write("\" id=\"");
        bw.write(spectrumIdentificationResultItemKey);
        bw.write("\">");
        bw.newLine();
        tabCounter++;

        // @TODO: iterate all assumptions and not just the best one?
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

        if (bestPeptideAssumption != null) {

            PSParameter psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

            int rank = 1;
            String spectrumIdentificationItemKey = "SII_" + spectrumMatchIndex + "_" + rank;
            spectrumIds.put(spectrumMatchKey, spectrumIdentificationItemKey);

            //String bestPeptideKey = bestPeptideAssumption.getPeptide().getMatchingKey(identificationParameters.getSequenceMatchingPreferences());
            long peptideMatchKey = spectrumKeyToPeptideKeyMap.get(spectrumMatchKey);

            bw.write(getCurrentTabSpace());
            bw.write("<SpectrumIdentificationItem passThreshold=\"");
            bw.write(Boolean.toString(psmParameter.getMatchValidationLevel().isValidated()));
            bw.write("\" rank=\"");
            bw.write(rank);
            bw.write("\" peptide_ref=\"");
            bw.write(Long.toString(peptideMatchKey));
            bw.write("\" calculatedMassToCharge=\"");
            bw.write(Double.toString(bestPeptideAssumption.getTheoreticMz()));
            bw.write("\" experimentalMassToCharge=\"");
            bw.write(Double.toString(spectrumFactory.getPrecursorMz(spectrumKey)));
            bw.write("\" chargeState=\"");
            bw.write(bestPeptideAssumption.getIdentificationCharge());
            bw.write("\" id=\"");
            bw.write(spectrumIdentificationItemKey);
            bw.write("\">");
            bw.newLine();
            tabCounter++;

            // add the peptide evidence references
            // get all the possible parent proteins
            TreeMap<String, int[]> proteinMapping = bestPeptideAssumption.getPeptide().getProteinMapping();
            String peptideSequence = bestPeptideAssumption.getPeptide().getSequence();

            // iterate all the possible protein parents for each peptide
            for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

                String accession = entry.getKey();
                int[] indexes = entry.getValue();

                for (int index : indexes) {

                    String pepEvidenceKey = getPeptideEvidenceKey(accession, index, peptideMatchKey);
                    String peptideEvidenceId = pepEvidenceIds.get(pepEvidenceKey);

                    bw.write(getCurrentTabSpace());
                    bw.write("<PeptideEvidenceRef peptideEvidence_ref=\"");
                    bw.write(peptideEvidenceId);
                    bw.write("\"/>");
                    bw.newLine();

                }
            }

            // add the fragment ions detected
            if (writeFragmentIons) {

                // add the fragment ion annotation
                AnnotationParameters annotationPreferences = identificationParameters.getAnnotationParameters();
                Spectrum spectrum = spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);
                SpecificAnnotationParameters specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationParameters(spectrumKey, bestPeptideAssumption);
                Stream<IonMatch> matches = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, spectrum, bestPeptideAssumption.getPeptide());

                // organize the fragment ions by ion type
                HashMap<String, HashMap<Integer, ArrayList<IonMatch>>> allFragmentIons = new HashMap<>();

                for (IonMatch ionMatch : matches.toArray(IonMatch[]::new)) {

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

                                allFragmentIons.put(fragmentIonName, new HashMap<>(1));

                            }

                            if (!allFragmentIons.get(fragmentIonName).containsKey(charge)) {

                                allFragmentIons.get(fragmentIonName).put(charge, new ArrayList<>(1));

                            }

                            allFragmentIons.get(fragmentIonName).get(charge).add(ionMatch);

                        }
                    }
                }

                if (!allFragmentIons.isEmpty()) {

                    bw.write(getCurrentTabSpace());
                    bw.write("<Fragmentation>");
                    bw.newLine();
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

                            StringBuilder indexes = new StringBuilder();
                            StringBuilder mzValues = new StringBuilder();
                            StringBuilder intensityValues = new StringBuilder();
                            StringBuilder errorValues = new StringBuilder();

                            // get the fragment ion details
                            for (IonMatch ionMatch : ionMatches) {

                                if (ionMatch.ion instanceof PeptideFragmentIon) {

                                    indexes.append(((PeptideFragmentIon) ionMatch.ion).getNumber())
                                            .append(' ');

                                } else if (ionMatch.ion instanceof ImmoniumIon) {

                                    // get the indexes of the corresponding residues
                                    char residue = ((ImmoniumIon) ionMatch.ion).aa;
                                    char[] peptideAsArray = peptideSequence.toCharArray();

                                    for (int i = 0; i < peptideAsArray.length; i++) {

                                        if (peptideAsArray[i] == residue) {

                                            indexes.append((i + 1))
                                                    .append(' ');

                                        }
                                    }

                                } else if (ionMatch.ion instanceof ReporterIon
                                        || ionMatch.ion instanceof RelatedIon // @TODO: request cv terms for related ions?
                                        || ionMatch.ion instanceof PrecursorIon) {

                                    indexes.append('0');

                                }

                                mzValues.append(ionMatch.peak.mz)
                                        .append(' ');
                                intensityValues.append(ionMatch.peak.intensity)
                                        .append(' ');
                                errorValues.append(ionMatch.getAbsoluteError())
                                        .append(' ');

                            }

                            // add the supported fragment ions
                            if (fragmentIonCvTerm != null) {

                                bw.write(getCurrentTabSpace());
                                bw.write("<IonType charge=\"");
                                bw.write(fragmentCharge);
                                bw.write("\" index=\"");
                                bw.write(indexes.toString().trim());
                                bw.write("\">");
                                bw.newLine();
                                tabCounter++;

                                bw.write(getCurrentTabSpace());
                                bw.write("<FragmentArray measure_ref=\"Measure_MZ\" values=\"");
                                bw.write(mzValues.toString().trim());
                                bw.write("\"/>");
                                bw.newLine();

                                bw.write(getCurrentTabSpace());
                                bw.write("<FragmentArray measure_ref=\"Measure_Int\" values=\"");
                                bw.write(intensityValues.toString().trim());
                                bw.write("\"/>");
                                bw.newLine();

                                bw.write(getCurrentTabSpace());
                                bw.write("<FragmentArray measure_ref=\"Measure_Error\" values=\"");
                                bw.write(errorValues.toString().trim());
                                bw.write("\"/>");
                                bw.newLine();

                                // add the cv term for the fragment ion type
                                writeCvTerm(fragmentIonCvTerm);

                                // add the cv term for the neutral losses
                                if (currentIon.getNeutralLosses() != null) {

                                    int neutralLossesCount = currentIon.getNeutralLosses().length;

                                    if (neutralLossesCount > maxNeutralLosses) {

                                        throw new IllegalArgumentException("A maximum of " + maxNeutralLosses + " neutral losses is supported.");

                                    } else {

                                        for (NeutralLoss tempNeutralLoss : currentIon.getNeutralLosses()) {

                                            writeCvTerm(tempNeutralLoss.getPsiMsCvTerm());

                                        }
                                    }
                                }

                                tabCounter--;
                                bw.write(getCurrentTabSpace());
                                bw.write("</IonType>");
                                bw.newLine();

                            }
                        }
                    }

                    tabCounter--;
                    bw.write(getCurrentTabSpace());
                    bw.write("</Fragmentation>");
                    bw.newLine();

                }
            }

            // add peptide shaker score and confidence
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002466", "PeptideShaker PSM score", Double.toString(Util.roundDouble(psmParameter.getScore(), CONFIDENCE_DECIMALS))));
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002467", "PeptideShaker PSM confidence", Double.toString(Util.roundDouble(psmParameter.getConfidence(), CONFIDENCE_DECIMALS))));

            switch (mzIdentMLVersion) {

                case v1_1:
                    break;

                case v1_2:

                    ModificationLocalizationParameters modificationLocalicationParameters = identificationParameters.getModificationLocalizationParameters();
                    PeptideMatch peptideMatch = (PeptideMatch) identification.retrieveObject(peptideMatchKey);
                    PSModificationScores psModificationScores = (PSModificationScores) spectrumMatch.getUrParam(new PSModificationScores());

                    if (psModificationScores != null) {

                        Peptide peptide = bestPeptideAssumption.getPeptide();

                        if (peptide.getModificationMatches().length > 0) {

                            Set<String> scoredModifications = psModificationScores.getScoredModifications();
                            HashSet<String> coveredModifications = new HashSet<>(scoredModifications.size());

                            for (String modName : scoredModifications) {

                                Modification modification = modificationFactory.getModification(modName);

                                if (!coveredModifications.contains(modName)) {

                                    coveredModifications.add(modName);

                                    double modMass = modification.getMass();
                                    Integer modIndex = modIndexMap.get(modMass);

                                    if (modIndex == null) {

                                        throw new IllegalArgumentException("No index found for modification " + modName + " of mass " + modMass + ".");

                                    }

                                    ModificationScoring modificationScoring = psModificationScores.getModificationScoring(modName);

                                    if (modificationScoring != null) {

                                        for (int site = 1; site <= peptideSequence.length(); site++) {

                                            if (modificationLocalicationParameters.isProbabilisticScoreCalculation()) {

                                                double score = modificationScoring.getProbabilisticScore(site);

                                                if (score > 0) {

                                                    String valid = "true";

                                                    if (score < modificationLocalicationParameters.getProbabilisticScoreThreshold()) {

                                                        valid = "false";

                                                    }

                                                    if (modificationLocalicationParameters.getSelectedProbabilisticScore() == ModificationLocalizationScore.PhosphoRS) {

                                                        StringBuilder sb = new StringBuilder();
                                                        sb.append(modIndex)
                                                                .append(':')
                                                                .append(score)
                                                                .append(':')
                                                                .append(site)
                                                                .append(':')
                                                                .append(valid);

                                                        writeCvTerm(new CvTerm("PSI-MS", "MS:1001969", "phosphoRS score", sb.toString()));

                                                    }
                                                }
                                            }

                                            double score = modificationScoring.getDeltaScore(site);

                                            if (score > 0) {

                                                String valid = "true";

                                                if (score < dScoreThreshold) {

                                                    valid = "false";

                                                }

                                                StringBuilder sb = new StringBuilder();
                                                sb.append(modIndex)
                                                        .append(':')
                                                        .append(score)
                                                        .append(':')
                                                        .append(site)
                                                        .append(':')
                                                        .append(valid);

                                                writeCvTerm(new CvTerm("PSI-MS", "MS:1002536", "D-Score", sb.toString()));

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002469", "PeptideShaker peptide confidence",
                            Double.toString(peptideParameter.getConfidence())));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002468", "PeptideShaker peptide score",
                            Double.toString(peptideParameter.getScore())));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002500", "peptide passes threshold",
                            Boolean.toString(peptideParameter.getMatchValidationLevel().isValidated())));
                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002520", "peptide group ID",
                            Long.toString(peptideMatchKey)));

                    psModificationScores = (PSModificationScores) peptideMatch.getUrParam(new PSModificationScores());

                    if (psModificationScores != null) {

                        Peptide peptide = peptideMatch.getPeptide();

                        if (peptide.getModificationMatches().length > 0) {

                            Set<String> scoredModifications = psModificationScores.getScoredModifications();
                            HashSet<String> coveredModifications = new HashSet<>(scoredModifications.size());

                            for (String modName : scoredModifications) {

                                if (!coveredModifications.contains(modName)) {

                                    coveredModifications.add(modName);
                                    Modification modification = modificationFactory.getModification(modName);
                                    double modMass = modification.getMass();
                                    Integer modIndex = modIndexMap.get(modMass);

                                    if (modIndex == null) {

                                        throw new IllegalArgumentException("No index found for modification " + modName + " of mass " + modMass + ".");
                                    }

                                    ModificationScoring modificationScoring = psModificationScores.getModificationScoring(modName);

                                    if (modificationScoring != null) {

                                        for (int site = 1; site <= peptideSequence.length(); site++) {

                                            if (modificationLocalicationParameters.isProbabilisticScoreCalculation()) {

                                                double score = modificationScoring.getProbabilisticScore(site);

                                                if (score > 0) {

                                                    String valid = "true";

                                                    if (score < modificationLocalicationParameters.getProbabilisticScoreThreshold()) {

                                                        valid = "false";

                                                    }

                                                    if (modificationLocalicationParameters.getSelectedProbabilisticScore() == ModificationLocalizationScore.PhosphoRS) {

                                                        StringBuilder sb = new StringBuilder();
                                                        sb.append(modIndex)
                                                                .append(':')
                                                                .append(score)
                                                                .append(':')
                                                                .append(site)
                                                                .append(':')
                                                                .append(valid);

                                                        writeCvTerm(new CvTerm("PSI-MS", "MS:1002550", "peptide:phosphoRS score", sb.toString()));

                                                    }
                                                }
                                            }

                                            double score = modificationScoring.getDeltaScore(site);

                                            if (score > 0) {

                                                String valid = "true";

                                                if (score < dScoreThreshold) {

                                                    valid = "false";

                                                }

                                                StringBuilder sb = new StringBuilder();
                                                sb.append(modIndex)
                                                        .append(':')
                                                        .append(score)
                                                        .append(':')
                                                        .append(site)
                                                        .append(':')
                                                        .append(valid);

                                                writeCvTerm(new CvTerm("PSI-MS", "MS:1002553", "peptide:D-Score", sb.toString()));

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
            TreeMap<Integer, Double> scores = new TreeMap<>();
            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = identification.getSpectrumMatch(spectrumMatchKey).getPeptideAssumptionsMap();

            for (Integer tempAdvocate : assumptions.keySet()) {

                TreeMap<Double, ArrayList<PeptideAssumption>> advocateMap = assumptions.get(tempAdvocate);

                for (double eValue : advocateMap.keySet()) {

                    for (PeptideAssumption peptideAssumption : advocateMap.get(eValue)) {

                        if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(
                                bestPeptideAssumption.getPeptide(), identificationParameters.getSequenceMatchingParameters())) {

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

            for (Entry<Integer, Double> entry : scores.entrySet()) {

                int advocate = entry.getKey();
                double eValue = entry.getValue();

                if (advocate == Advocate.msgf.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002052", "MS-GF:SpecEValue", Double.toString(eValue)));

                } else if (advocate == Advocate.mascot.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001172", "Mascot:expectation value", Double.toString(eValue)));

                } else if (advocate == Advocate.omssa.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001328", "OMSSA:evalue", Double.toString(eValue)));

                } else if (advocate == Advocate.xtandem.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001330", "X!Tandem:expect", Double.toString(eValue)));

                } else if (advocate == Advocate.comet.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1002257", "Comet:expectation value", Double.toString(eValue)));

                } else if (advocate == Advocate.myriMatch.getIndex()) {

                    writeCvTerm(new CvTerm("PSI-MS", "MS:1001589", "MyriMatch:MVH", Double.toString(eValue)));

                } else {

                    writeUserParam(Advocate.getAdvocate(advocate).getName() + " e-value", Double.toString(eValue)); // @TODO: add Tide if Tide CV term is added

                }
            }

            // add the additional search engine scores
            if (mascotScore != null) {

                writeCvTerm(new CvTerm("PSI-MS", "MS:1001171", "Mascot:score", Double.toString(mascotScore)));

            }
            if (msAmandaScore != null) {

                writeCvTerm(new CvTerm("PSI-MS", "MS:1002319", "Amanda:AmandaScore", Double.toString(msAmandaScore)));

            }

            // add other cv and user params
            bw.write(getCurrentTabSpace());
            bw.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001117\" name=\"theoretical mass\" value=\"");
            bw.write(Double.toString(bestPeptideAssumption.getTheoreticMass()));
            bw.write("\" unitCvRef=\"UO\" unitAccession=\"UO:0000221\" unitName=\"dalton\"/>");
            bw.newLine();

            // add validation level information
            writeCvTerm(new CvTerm("PSI-MS", "MS:1002540", "PeptideShaker PSM confidence type", psmParameter.getMatchValidationLevel().getName()));
            tabCounter--;

            bw.write(getCurrentTabSpace());
            bw.write("</SpectrumIdentificationItem>");
            bw.newLine();

            // add the spectrum title
            writeCvTerm(new CvTerm("PSI-MS", "MS:1000796", "spectrum title", spectrumTitle));

            // add the precursor retention time
            Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

            if (precursor != null) {

                bw.write(getCurrentTabSpace());
                bw.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1000894\" name=\"retention time\" value=\"");
                bw.write(Double.toString(precursor.getRt()));
                bw.write("\" unitCvRef=\"UO\" unitAccession=\"UO:0000010\" unitName=\"second\"/>");
                bw.newLine();

            }

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</SpectrumIdentificationResult>");
            bw.newLine();

        }
    }

    /**
     * Write the fragmentation table. (Note: all hard coded.)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeFragmentationTable() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<FragmentationTable>");
        bw.newLine();
        tabCounter++;

        // mz
        bw.write(getCurrentTabSpace());
        bw.write("<Measure id=\"Measure_MZ\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001225\" name=\"product ion m/z\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\" />");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Measure>");
        bw.newLine();

        // intensity
        bw.write(getCurrentTabSpace());
        bw.write("<Measure id=\"Measure_Int\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001226\" name=\"product ion intensity\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000131\" unitName=\"number of detector counts\"/>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Measure>");
        bw.newLine();

        // mass error
        bw.write(getCurrentTabSpace());
        bw.write("<Measure id=\"Measure_Error\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001227\" name=\"product ion m/z error\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Measure>");
        bw.newLine();

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</FragmentationTable>");
        bw.newLine();
    }

    /**
     * Write the input file details.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeInputFileDetails() throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<Inputs>");
        bw.newLine();
        tabCounter++;

        int sourceFileCounter = 1;

        // add the search result files
        for (File idFile : projectDetails.getIdentificationFiles()) {

            // @TODO: add MS:1000568 - MD5?
//            FileInputStream fis = new FileInputStream(new File("foo"));
//            String md5 = DigestUtils.md5Hex(fis);
//            fis.close();
            bw.write(getCurrentTabSpace());
            bw.write("<SourceFile location=\"");
            bw.write(idFile.toURI().toString());
            bw.write("\" id=\"SourceFile_");
            bw.write(sourceFileCounter++);
            bw.write("\">");
            bw.newLine();
            tabCounter++;

            bw.write(getCurrentTabSpace());
            bw.write("<FileFormat>");
            bw.newLine();
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
            bw.write(getCurrentTabSpace());
            bw.write("</FileFormat>");
            bw.newLine();

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</SourceFile>");
            bw.newLine();
        }

        // add the database
        File fastaFile = identificationParameters.getSearchParameters().getFastaFile();
        bw.write(getCurrentTabSpace());
        bw.write("<SearchDatabase numDatabaseSequences=\"");
        bw.write(fastaSummary.nSequences);
        bw.write("\" location=\"");
        bw.write(fastaFile.toURI().toString());
        bw.write("\" id=\"" + "SearchDB_1\">");
        bw.newLine();
        tabCounter++;

        bw.write(getCurrentTabSpace());
        bw.write("<FileFormat>");
        bw.newLine();
        tabCounter++;

        writeCvTerm(new CvTerm("PSI-MS", "MS:1001348", "FASTA format", null));
        tabCounter--;

        bw.write(getCurrentTabSpace());
        bw.write("</FileFormat>");
        bw.newLine();

        bw.write(getCurrentTabSpace());
        bw.write("<DatabaseName>");
        bw.newLine();
        tabCounter++;

        writeUserParam(fastaFile.getName()); // @TODO: add database type? children of MS:1001013 - database name??? for example: MS:1001104 (database UniProtKB/Swiss-Prot)

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</DatabaseName>");
        bw.newLine();

        writeCvTerm(new CvTerm("PSI-MS", "MS:1001073", "database type amino acid", null));

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</SearchDatabase>");
        bw.newLine();

        // add the spectra location
        for (String mgfFileName : spectrumFactory.getMgfFileNames()) {

            File mgfFile = projectDetails.getSpectrumFile(mgfFileName);

            bw.write(getCurrentTabSpace());
            bw.write("<SpectraData location=\"");
            bw.write(mgfFile.toURI().toString());
            bw.write("\" id=\"");
            bw.write(mgfFileName);
            bw.write("\" name=\"");
            bw.write(mgfFile.getName());
            bw.write("\">");
            bw.newLine();
            tabCounter++;

            bw.write(getCurrentTabSpace());
            bw.write("<FileFormat>");
            bw.newLine();
            tabCounter++;

            writeCvTerm(new CvTerm("PSI-MS", "MS:1001062", "Mascot MGF format", null));

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</FileFormat>");
            bw.newLine();

            bw.write(getCurrentTabSpace());
            bw.write("<SpectrumIDFormat>");
            bw.newLine();
            tabCounter++;

            writeCvTerm(new CvTerm("PSI-MS", "MS:1000774", "multiple peak list nativeID format", null));

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</SpectrumIDFormat>");
            bw.newLine();

            tabCounter--;
            bw.write(getCurrentTabSpace());
            bw.write("</SpectraData>");
            bw.newLine();

        }

        tabCounter--;
        bw.write(getCurrentTabSpace());
        bw.write("</Inputs>");
        bw.newLine();

    }

    /**
     * Writes the mzIdentML start tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzIdentMLStartTag() throws IOException {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        bw.newLine();

        switch (mzIdentMLVersion) {

            case v1_1:
                bw.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.1 http://www.psidev.info/files/mzIdentML1.1.0.xsd\" "
                        + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\" version=\"1.1.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">");
                bw.newLine();
                break;

            case v1_2:
                bw.write("<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.2 http://www.psidev.info/files/mzIdentML1.2.0.xsd\" "
                        + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.2\" version=\"1.2.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">");
                bw.newLine();
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
        bw.write("</MzIdentML>");

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
     *
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
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * writing to the file
     */
    private void writeCvTerm(CvTerm cvTerm, boolean showValue) throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<cvParam cvRef=\"");
        bw.write(StringEscapeUtils.escapeHtml4(cvTerm.getOntology()));
        bw.write("\" accession=\"");
        bw.write(cvTerm.getAccession());
        bw.write("\" name=\"");
        bw.write(StringEscapeUtils.escapeHtml4(cvTerm.getName()));
        bw.write("\"");

        writeCvTermValue(cvTerm, showValue);

    }

    /**
     * Convenience method writing the value element of a CV term.
     *
     * @param cvTerm the CV term
     * @param showValue decides if the CV terms value (if existing) is printed
     * or not
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * writing to the file
     */
    private void writeCvTermValue(CvTerm cvTerm, boolean showValue) throws IOException {

        String value = cvTerm.getValue();

        if (showValue && value != null) {

            bw.write(" value=\"");
            bw.write(StringEscapeUtils.escapeHtml4(value));
            bw.write("\"/>");

        } else {

            bw.write("/>");

        }

        bw.newLine();

    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param userParamAsString the user parameter as a string
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * writing to the file
     */
    private void writeUserParam(String userParamAsString) throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<userParam name=\"");
        bw.write(StringEscapeUtils.escapeHtml4(userParamAsString));
        bw.write("\"/>");

        bw.newLine();

    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param name the name of the user parameter
     * @param value the value of the user parameter
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * writing to the file
     */
    private void writeUserParam(String name, String value) throws IOException {

        bw.write(getCurrentTabSpace());
        bw.write("<userParam name=\"");
        bw.write(StringEscapeUtils.escapeHtml4(name));
        bw.write("\" value=\"");
        bw.write(StringEscapeUtils.escapeHtml4(value));
        bw.write("\" />");
        bw.newLine();

    }
}
