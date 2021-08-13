package eu.isas.peptideshaker.export;

import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.ions.Ion.IonType;
import com.compomics.util.experiment.biology.ions.NeutralLoss;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.ions.impl.ImmoniumIon;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.impl.PrecursorIon;
import com.compomics.util.experiment.biology.ions.impl.RelatedIon;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.biology.modifications.ModificationProvider;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.modification.ModificationLocalizationScore;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
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
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.PSMaps;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.peptide_shaker.PSModificationScores;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.experiment.identification.peptide_shaker.ModificationScoring;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.IoUtil;
import com.compomics.util.io.flat.SimpleFileWriter;
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
import org.apache.commons.text.StringEscapeUtils;

/**
 * The class that takes care of converting the data to mzIdentML.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class MzIdentMLExport {

    /**
     * The writer.
     */
    private SimpleFileWriter writer;
    /**
     * Integer keeping track of the number of tabs to include at the beginning
     * of each line.
     */
    private int tabCounter = 0;
    /**
     * The provider to use to get modification information.
     */
    private final ModificationProvider modificationProvider;
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
     * The version of the mzIdentML file to use, 1.1 by default.
     */
    private MzIdentMLVersion mzIdentMLVersion = MzIdentMLVersion.v1_1;
    /**
     * The protein sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * Summary information on the protein sequences file.
     */
    private final FastaSummary fastaSummary;
    /**
     * A protein details provider.
     */
    private final ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The PeptideShaker version.
     */
    private final String peptideShakerVersion;
    /**
     * The identifications object.
     */
    private final Identification identification;
    /**
     * The project details.
     */
    private final ProjectDetails projectDetails;
    /**
     * The identification feature generator.
     */
    private final IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The peptide spectrum annotator.
     */
    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator;
    /**
     * The waiting handler.
     */
    private final WaitingHandler waitingHandler;
    /**
     * The peptide evidence IDs.
     */
    private final HashMap<String, String> pepEvidenceIds = new HashMap<>();
    /**
     * The spectrum IDs.
     */
    private final HashMap<Long, String> spectrumIds = new HashMap<>();
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * Map of PTM indexes: PTM mass to index.
     */
    private final HashMap<Double, Integer> modIndexMap = new HashMap<>();
    /**
     * Map of the spectrum title to index.
     */
    private final HashMap<String, HashMap<String, Integer>> spectrumTitleToIndexMap = new HashMap<>(0);
    /**
     * If true, the fragment ions will be written to the mzid file.
     */
    private final boolean writeFragmentIons = true;
    /**
     * If true, the protein sequences are included in the mzid file.
     */
    private final boolean includeProteinSequences;

    /**
     * Constructor.
     *
     * @param peptideShakerVersion The PeptideShaker version.
     * @param identification The identification object.
     * @param projectDetails The project details.
     * @param identificationParameters The identification parameters.
     * @param sequenceProvider The sequence provider.
     * @param fastaSummary The summary information on the protein sequences
     * file.
     * @param proteinDetailsProvider The protein details provider.
     * @param spectrumProvider The spectrum provider.
     * @param modificationProvider The modifications provider.
     * @param identificationFeaturesGenerator The identification features
     * generator.
     * @param outputFile The output file.
     * @param includeProteinSequences If true, the protein sequences are
     * included in the output.
     * @param waitingHandler The waiting handler used to display progress to the
     * user and interrupt the process.
     * @param gzip If true export as gzipped file.
     */
    public MzIdentMLExport(
            String peptideShakerVersion,
            Identification identification,
            ProjectDetails projectDetails,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            SpectrumProvider spectrumProvider,
            ModificationProvider modificationProvider,
            FastaSummary fastaSummary,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            File outputFile,
            boolean includeProteinSequences,
            WaitingHandler waitingHandler,
            boolean gzip
    ) {

        this.peptideShakerVersion = peptideShakerVersion;
        this.identification = identification;
        this.projectDetails = projectDetails;
        this.identificationParameters = identificationParameters;
        this.sequenceProvider = sequenceProvider;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.spectrumProvider = spectrumProvider;
        this.modificationProvider = modificationProvider;
        this.fastaSummary = fastaSummary;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.includeProteinSequences = includeProteinSequences;
        this.waitingHandler = waitingHandler;
        this.peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        setSpectrumTitlesMap();

        writer = new SimpleFileWriter(outputFile, gzip);

    }

    /**
     * Sets the spectrum titles to index map.
     */
    private void setSpectrumTitlesMap() {

        for (String fileNameWithoutExtension : spectrumProvider.getOrderedFileNamesWithoutExtensions()) {

            String[] spectrumTitles = spectrumProvider.getSpectrumTitles(fileNameWithoutExtension);
            HashMap<String, Integer> tempMap = new HashMap<>(spectrumTitles.length);

            for (int i = 0; i < spectrumTitles.length; i++) {

                tempMap.put(spectrumTitles[i], i);

            }

            spectrumTitleToIndexMap.put(fileNameWithoutExtension, tempMap);

        }
    }

    /**
     * Creates the mzIdentML file.
     *
     * @param mzIdentMLVersion The version of mzIdentML to use.
     */
    public void createMzIdentMLFile(
            MzIdentMLVersion mzIdentMLVersion
    ) {

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

        waitingHandler.setPrimaryProgressCounterIndeterminate(true);

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
        waitingHandler.setMaxPrimaryProgressCounter(
                fastaSummary.nSequences
                + identification.getPeptideIdentification().size() * 2
                + identification.getSpectrumIdentificationSize()
                + identification.getProteinIdentification().size()
        );

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

        writer.close();

    }

    /**
     * Writes the CV list.
     */
    private void writeCvList() {

        writer.write(getCurrentTabSpace());
        writer.write("<cvList>");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<cv id=\"PSI-MS\" ");
        writer.write("uri=\"https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo\" ");
        writer.write("fullName=\"PSI-MS\"/>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<cv id=\"UNIMOD\" ");
        writer.write("uri=\"http://www.unimod.org/obo/unimod.obo\" ");
        writer.write("fullName=\"UNIMOD\"/>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<cv id=\"UO\" ");
        writer.write("uri=\"https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/unit.obo\" ");
        writer.write("fullName=\"UNIT-ONTOLOGY\"/>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<cv id=\"PRIDE\" ");
        writer.write("uri=\"https://github.com/PRIDE-Utilities/pride-ontology/blob/master/pride_cv.obo\" ");
        writer.write("fullName=\"PRIDE\"/>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</cvList>");
        writer.newLine();

    }

    /**
     * Write the software list.
     */
    private void writeAnalysisSoftwareList() {

        writer.write(getCurrentTabSpace());
        writer.write("<AnalysisSoftwareList>");
        writer.newLine();
        tabCounter++;

        // @TODO: also add SearchGUI and/or search engines used?
        writer.write(getCurrentTabSpace());
        writer.write("<AnalysisSoftware name=\"PeptideShaker\" version=\"");
        writer.write(peptideShakerVersion);
        writer.write("\" id=\"ID_software\" uri=\"https://compomics.github.io/projects/peptide-shaker.html\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<ContactRole contact_ref=\"PS_DEV\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<Role>");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001267",
                        "software vendor",
                        null
                )
        );
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</Role>");
        writer.newLine();
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</ContactRole>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<SoftwareName>");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1002458",
                        "PeptideShaker",
                        null
                )
        );
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</SoftwareName>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<Customizations>No customisations</Customizations>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AnalysisSoftware>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AnalysisSoftwareList>");
        writer.newLine();

    }

    /**
     * Write the provider details.
     */
    private void writeProviderDetails() {

        writer.write(getCurrentTabSpace());
        writer.write("<Provider id=\"PROVIDER\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<ContactRole contact_ref=\"PROVIDER\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<Role>");
        writer.newLine();
        tabCounter++;

        // @TODO: add user defined provider role?
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001271",
                        "researcher",
                        null
                )
        );
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</Role>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</ContactRole>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Provider>");
        writer.newLine();

    }

    /**
     * Write the audit collection.
     */
    private void writeAuditCollection() {

        writer.write(getCurrentTabSpace());
        writer.write("<AuditCollection>");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<Person firstName=\"");
        writer.write(StringEscapeUtils.escapeHtml4(projectDetails.getContactFirstName()));
        writer.write("\" lastName=\"");
        writer.write(StringEscapeUtils.escapeHtml4(projectDetails.getContactLastName()));
        writer.write("\" id=\"PROVIDER\">");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000587",
                        "contact address",
                        StringEscapeUtils.escapeHtml4(
                                projectDetails.getContactAddress()
                        )
                )
        );

        if (projectDetails.getContactUrl() != null && !projectDetails.getContactUrl().isEmpty()) {

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1000588",
                            "contact URL",
                            projectDetails.getContactUrl()
                    )
            );

        }

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000589",
                        "contact email",
                        projectDetails.getContactEmail()
                )
        );

        writer.write(getCurrentTabSpace());
        writer.write("<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>");
        writer.newLine();
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</Person>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<Organization name=\"");
        writer.write(StringEscapeUtils.escapeHtml4(projectDetails.getOrganizationName()));
        writer.write("\" id=\"ORG_DOC_OWNER\">");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000586",
                        "contact name",
                        StringEscapeUtils.escapeHtml4(projectDetails.getOrganizationName())
                )
        );
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000587",
                        "contact address",
                        StringEscapeUtils.escapeHtml4(projectDetails.getOrganizationAddress())
                )
        );

        if (projectDetails.getOrganizationUrl() != null && !projectDetails.getOrganizationUrl().isEmpty()) {

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1000588",
                            "contact URL",
                            projectDetails.getOrganizationUrl()
                    )
            );

        }

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000589",
                        "contact email",
                        projectDetails.getOrganizationEmail()
                )
        );
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</Organization>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<Organization name=\"PeptideShaker developers\" id=\"PS_DEV\">");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000586",
                        "contact name",
                        "PeptideShaker developers"
                )
        );
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000587",
                        "contact address",
                        "Proteomics Unit, Building for Basic Biology, University of Bergen, Jonas Liesvei 91, N-5009 Bergen, Norway"
                )
        );
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000588",
                        "contact URL",
                        "https://compomics.github.io/projects/peptide-shaker.html"
                )
        );
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1000589",
                        "contact email",
                        "peptide-shaker@googlegroups.com"
                )
        );

        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</Organization>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AuditCollection>");
        writer.newLine();

    }

    /**
     * Write the sequence collection.
     */
    private void writeSequenceCollection() {

        writer.write(getCurrentTabSpace());
        writer.write("<SequenceCollection>");
        writer.newLine();
        tabCounter++;

//        String dbType = Header.getDatabaseTypeAsString(Header.DatabaseType.Unknown); // @TODO: add database type as cv param? children of MS:1001013 (database name)
//        FastaIndex fastaIndex = sequenceFactory.getCurrentFastaIndex();
//        if (fastaIndex != null) {
//            dbType = Header.getDatabaseTypeAsString(fastaIndex.getDatabaseType());
//        }
//
        // iterate all the protein sequences
        for (String accession : sequenceProvider.getAccessions()) { // @TODO: include only protein sequences with at least one PeptideEvidence element referring to it (note: this is a SHOULD rule in the mzid specifications)

            writer.write(getCurrentTabSpace());
            writer.write("<DBSequence id=\"");
            writer.write(accession);
            writer.write("\" ");
            writer.write("accession=\"");
            writer.write(accession);
            writer.write("\" searchDatabase_ref=\"SearchDB_1\" >");
            writer.newLine();
            tabCounter++;

            if (includeProteinSequences) {

                String sequence = sequenceProvider.getSequence(accession);
                writer.write(getCurrentTabSpace());
                writer.write("<Seq>");
                writer.write(sequence);
                writer.write("</Seq>");
                writer.newLine();

            }

            String description = proteinDetailsProvider.getDescription(accession);
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1001088",
                            "protein description",
                            StringEscapeUtils.escapeHtml4(description)
                    )
            );

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</DBSequence>");
            writer.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        // Write the peptides, store the spectrum key to peptide key mapping.
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getPeptide();
            String peptideSequence = peptide.getSequence();

            writer.write(getCurrentTabSpace());
            writer.write("<Peptide id=\"");
            writer.write(Long.toString(peptideKey));
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            writer.write(getCurrentTabSpace());
            writer.write("<PeptideSequence>");
            writer.write(peptideSequence);
            writer.write("</PeptideSequence>");
            writer.newLine();

            String[] fixedModifications = peptide.getFixedModifications(
                    identificationParameters.getSearchParameters().getModificationParameters(),
                    sequenceProvider,
                    identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters()
            );

            for (int site = 0; site < fixedModifications.length; site++) {

                String modName = fixedModifications[site];

                if (modName != null) {

                    Modification modification = modificationProvider.getModification(modName);

                    int aa = Math.min(Math.max(site, 1), peptideSequence.length());

                    writer.write(getCurrentTabSpace());
                    writer.write("<Modification monoisotopicMassDelta=\"");
                    writer.write(Double.toString(modification.getRoundedMass()));
                    writer.write("\" residues=\"");
                    writer.write(Character.toString(peptideSequence.charAt(aa - 1)));
                    writer.write("\" location=\"");
                    writer.write(Integer.toString(site));
                    writer.write("\" >");
                    writer.newLine();

                    CvTerm ptmCvTerm = modification.getUnimodCvTerm();

                    if (ptmCvTerm != null) {

                        tabCounter++;
                        writeCvTerm(ptmCvTerm, false);
                        tabCounter--;

                    } else {

                        // try PSI-MOD instead
                        ptmCvTerm = modification.getPsiModCvTerm();

                        if (ptmCvTerm != null) {

                            tabCounter++;
                            writeCvTerm(ptmCvTerm, false);
                            tabCounter--;

                        }

                    }

                    writer.write(getCurrentTabSpace());
                    writer.write("</Modification>");
                    writer.newLine();

                }
            }

            for (ModificationMatch modMatch : peptide.getVariableModifications()) {

                Modification modification = modificationProvider.getModification(modMatch.getModification());

                int site = modMatch.getSite();
                int aa = Math.min(Math.max(site, 1), peptideSequence.length());

                writer.write(getCurrentTabSpace());
                writer.write("<Modification monoisotopicMassDelta=\"");
                writer.write(Double.toString(modification.getRoundedMass()));
                writer.write("\" residues=\"");
                writer.write(Character.toString(peptideSequence.charAt(aa - 1)));
                writer.write("\" location=\"");
                writer.write(Integer.toString(site));
                writer.write("\" >");
                writer.newLine();

                CvTerm ptmCvTerm = modification.getUnimodCvTerm();

                if (ptmCvTerm != null) {

                    tabCounter++;
                    writeCvTerm(ptmCvTerm, false);
                    tabCounter--;

                } else {

                    // try PSI-MOD instead
                    ptmCvTerm = modification.getPsiModCvTerm();

                    tabCounter++;
                    writeCvTerm(ptmCvTerm, false);
                    tabCounter--;

                }

                writer.write(getCurrentTabSpace());
                writer.write("</Modification>");
                writer.newLine();

            }

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</Peptide>");
            writer.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        int peptideEvidenceCounter = 0;
        int nAa = 1;

        // re-iterate the peptides to write peptide to protein mapping
        peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            long peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getPeptide();

            // get the possible proteins
            TreeMap<String, int[]> proteinMapping = peptide.getProteinMapping();

            // iterate the possible proteins
            for (Entry<String, int[]> entry : proteinMapping.entrySet()) {

                String accession = entry.getKey();
                int[] indexes = entry.getValue();

                for (int index : indexes) {

                    String aaBefore = PeptideUtils.getAaBefore(
                            peptide,
                            accession,
                            index,
                            nAa,
                            sequenceProvider
                    );

                    if (aaBefore.length() == 0) {

                        aaBefore = "-";

                    }

                    String aaAfter = PeptideUtils.getAaAfter(
                            peptide,
                            accession,
                            index,
                            nAa,
                            sequenceProvider
                    );

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

                    writer.write(getCurrentTabSpace());
                    writer.write("<PeptideEvidence isDecoy=\"");
                    writer.write(
                            Boolean.toString(
                                    PeptideUtils.isDecoy(
                                            peptideMatch.getPeptide(),
                                            sequenceProvider
                                    )
                            )
                    );
                    writer.write("\" pre=\"");
                    writer.write(aaBefore);
                    writer.write("\" post=\"");
                    writer.write(aaAfter);
                    writer.write("\" start=\"");
                    writer.write(Integer.toString(peptideStart + 1));
                    writer.write("\" end=\"");
                    writer.write(Integer.toString(peptideEnd + 1));
                    writer.write("\" peptide_ref=\"");
                    writer.write(Long.toString(peptideKey));
                    writer.write("\" dBSequence_ref=\"");
                    writer.write(accession);
                    writer.write("\" id=\"");
                    writer.write(pepEvidenceValue);
                    writer.write("\" />");
                    writer.newLine();

                }
            }

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</SequenceCollection>");
        writer.newLine();

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
    public static String getPeptideEvidenceKey(
            String accession,
            int peptideStart,
            long peptideKey
    ) {

        String peptideStartAsString = Integer.toString(peptideStart);
        String peptideKeyAsString = Long.toString(peptideKey);

        StringBuilder pepEvidenceKeybuilder = new StringBuilder(
                accession.length()
                + peptideStartAsString.length()
                + peptideKeyAsString.length() + 2
        );

        pepEvidenceKeybuilder
                .append(accession)
                .append('_')
                .append(peptideStartAsString)
                .append('_')
                .append(peptideKeyAsString);

        return pepEvidenceKeybuilder.toString();
    }

    /**
     * Write the analysis collection.
     */
    private void writeAnalysisCollection() {

        writer.write(getCurrentTabSpace());
        writer.write("<AnalysisCollection>");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<SpectrumIdentification spectrumIdentificationList_ref=\"SIL_1\" ");
        writer.write("spectrumIdentificationProtocol_ref=\"SearchProtocol_1\" id=\"SpecIdent_1\">");
        writer.newLine();
        tabCounter++;

        // iterate the spectrum files and add the file name refs
        for (String spectrumFileNameWithoutExtension : spectrumProvider.getOrderedFileNamesWithoutExtensions()) {

            writer.write(getCurrentTabSpace());
            writer.write("<InputSpectra spectraData_ref=\"");
            writer.write(spectrumFileNameWithoutExtension);
            writer.write("\"/>");
            writer.newLine();

        }

        writer.write(getCurrentTabSpace());
        writer.write("<SearchDatabaseRef searchDatabase_ref=\"SearchDB_1\"/>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</SpectrumIdentification>");
        writer.newLine();

        // add protein detection
        // @TODO: add activityDate? example: activityDate="2011-03-25T13:33:51
        writer.write(getCurrentTabSpace());
        writer.write("<ProteinDetection proteinDetectionProtocol_ref=\"PeptideShaker_1\" ");
        writer.write("proteinDetectionList_ref=\"Protein_groups\" id=\"PD_1\">");
        writer.newLine();

        tabCounter++;
        writer.write(getCurrentTabSpace());
        writer.write("<InputSpectrumIdentifications spectrumIdentificationList_ref=\"SIL_1\"/>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</ProteinDetection>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AnalysisCollection>");
        writer.newLine();

    }

    /**
     * Write the analysis protocol.
     */
    private void writeAnalysisProtocol() {

        writer.write(getCurrentTabSpace());
        writer.write("<AnalysisProtocolCollection>");
        writer.newLine();
        tabCounter++;

        // add spectrum identification protocol
        writer.write(getCurrentTabSpace());
        writer.write("<SpectrumIdentificationProtocol analysisSoftware_ref=\"ID_software\" id=\"SearchProtocol_1\">");
        writer.newLine();
        tabCounter++;

        // the search type
        writer.write(getCurrentTabSpace());
        writer.write("<SearchType>");
        writer.newLine();

        tabCounter++;
        writeCvTerm(new CvTerm("PSI-MS", "MS:1001083", "ms-ms search", null));

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</SearchType>");
        writer.newLine();

        // the search parameters
        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        writer.write(getCurrentTabSpace());
        writer.write("<AdditionalSearchParams>");
        writer.newLine();

        tabCounter++;
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001211",
                        "parent mass type mono",
                        null
                )
        );
        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001256",
                        "fragment mass type mono",
                        null
                )
        );

        switch (mzIdentMLVersion) {

            case v1_1:
                break;

            case v1_2:
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002492",
                                "consensus scoring",
                                null
                        )
                );
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002490",
                                "peptide-level scoring",
                                null
                        )
                );
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002497",
                                "group PSMs by sequence with modifications",
                                null
                        )
                );
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002491",
                                "modification localization scoring",
                                null
                        )
                );
                break;

            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");

        }

        // @TODO: list all search parameters from the search engines used?
        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AdditionalSearchParams>");
        writer.newLine();

        // the modifications
        writer.write(getCurrentTabSpace());
        writer.write("<ModificationParams>");
        writer.newLine();
        tabCounter++;

        // create the ptm index map
        switch (mzIdentMLVersion) {

            case v1_1:
                break;

            case v1_2:

                for (String modName : searchParameters.getModificationParameters().getAllModifications()) {

                    Modification modification = modificationProvider.getModification(modName);
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

            Modification modification = modificationProvider.getModification(modName);
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

            writer.write(getCurrentTabSpace());
            writer.write("<SearchModification residues=\"");
            writer.write(aminoAcidsAtTarget);
            writer.write("\" massDelta=\"");
            writer.write(Double.toString(modification.getRoundedMass()));
            writer.write("\" fixedMod= \"");
            writer.write(Boolean.toString(searchParameters.getModificationParameters().getFixedModifications().contains(modName)));
            writer.write("\" >");
            writer.newLine();
            tabCounter++;

            // add modification specificity
            if (modificationType != ModificationType.modaa) {

                writer.write(getCurrentTabSpace());
                writer.write("<SpecificityRules>");
                writer.newLine();
                tabCounter++;

                switch (modificationType) {

                    case modn_protein:
                    case modnaa_protein:
                        writeCvTerm(
                                new CvTerm(
                                        "PSI-MS",
                                        "MS:1002057",
                                        "modification specificity protein N-term",
                                        null
                                )
                        );
                        break;

                    case modn_peptide:
                    case modnaa_peptide:
                        writeCvTerm(
                                new CvTerm(
                                        "PSI-MS",
                                        "MS:1001189",
                                        "modification specificity peptide N-term",
                                        null
                                )
                        );
                        break;

                    case modc_protein:
                    case modcaa_protein:
                        writeCvTerm(
                                new CvTerm(
                                        "PSI-MS",
                                        "MS:1002058",
                                        "modification specificity protein C-term",
                                        null
                                )
                        );
                        break;

                    case modc_peptide:
                    case modcaa_peptide:
                        writeCvTerm(
                                new CvTerm(
                                        "PSI-MS",
                                        "MS:1001190",
                                        "modification specificity peptide C-term",
                                        null
                                )
                        );
                        break;

                    default:
                        break;
                }

                tabCounter--;
                writer.write(getCurrentTabSpace());
                writer.write("</SpecificityRules>");
                writer.newLine();

            }

            // add the modification cv term
            CvTerm ptmCvTerm = modification.getUnimodCvTerm();

            if (ptmCvTerm != null) {

                writeCvTerm(ptmCvTerm);

            } else {

                // try PSI-MOD instead
                ptmCvTerm = modification.getPsiModCvTerm();

                if (ptmCvTerm != null) {

                    writeCvTerm(ptmCvTerm);

                } else {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001460",
                                    "unknown modification",
                                    null
                            )
                    );
                }
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

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002504",
                                    "modification index",
                                    modIndex.toString()
                            )
                    );
                    break;

                default:
                    throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");
            }

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</SearchModification>");
            writer.newLine();

        }

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</ModificationParams>");
        writer.newLine();

        // Digestion
        DigestionParameters digestionPreferences = searchParameters.getDigestionParameters();

        if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.unSpecific) {

            writer.write(getCurrentTabSpace());
            writer.write("<Enzymes independent=\"false\">");
            writer.newLine();

            tabCounter++;
            writer.write(getCurrentTabSpace());
            writer.write("<Enzyme name=\"unspecific cleavage\">");
            writer.newLine();

            tabCounter++;
            writer.write(getCurrentTabSpace());
            writer.write("<EnzymeName>");
            writer.newLine();

            tabCounter++;
            CvTerm enzymeCvTerm = new CvTerm(
                    "PSI-MS",
                    "MS:1001091",
                    "unspecific cleavage",
                    null
            );
            writeCvTerm(enzymeCvTerm);

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</EnzymeName>");
            writer.newLine();

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</Enzyme>");
            writer.newLine();

        } else if (digestionPreferences.getCleavageParameter() == DigestionParameters.CleavageParameter.wholeProtein) {

            writer.write(getCurrentTabSpace());
            writer.write("<Enzymes independent=\"false\">");
            writer.newLine();
            tabCounter++;

            writer.write(getCurrentTabSpace());
            writer.write("<Enzyme name=\"NoEnzyme\">");
            writer.newLine();
            tabCounter++;

            writer.write(getCurrentTabSpace());
            writer.write("<EnzymeName>");
            writer.newLine();
            tabCounter++;

            CvTerm enzymeCvTerm = new CvTerm(
                    "PSI-MS",
                    "MS:1001955",
                    "NoEnzyme",
                    null
            );
            writeCvTerm(enzymeCvTerm);

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</EnzymeName>");
            writer.newLine();

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</Enzyme>");
            writer.newLine();

        } else {

            ArrayList<Enzyme> enzymes = digestionPreferences.getEnzymes();

            writer.write(getCurrentTabSpace());
            writer.write("<Enzymes independent=\"");
            writer.write(Boolean.toString(enzymes.size() > 1));
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            for (Enzyme enzyme : enzymes) {

                String enzymeName = enzyme.getName();
                writer.write(getCurrentTabSpace());
                writer.write("<Enzyme missedCleavages=\"");
                writer.write(Integer.toString(digestionPreferences.getnMissedCleavages(enzymeName)));
                writer.write("\" semiSpecific=\"");
                writer.write(Boolean.toString(digestionPreferences.getSpecificity(enzymeName) == DigestionParameters.Specificity.semiSpecific));
                writer.write("\" ");
                //+ "cTermGain=\"OH\" " // Element formula gained at CTerm
                //+ "nTermGain=\"H\" " // Element formula gained at NTerm
                // @TODO: add <SiteRegexp><![CDATA[(?<=[KR])(?!P)]]></SiteRegexp>?
                writer.write("id=\"Enz1\" name=\"");
                writer.write(enzyme.getName());
                writer.write("\">");
                writer.newLine();
                tabCounter++;

                writer.write(getCurrentTabSpace());
                writer.write("<EnzymeName>");
                writer.newLine();

                tabCounter++;
                CvTerm enzymeCvTerm = enzyme.getCvTerm();

                if (enzymeCvTerm != null) {

                    writeCvTerm(enzymeCvTerm);

                } else {

                    writeUserParam(enzyme.getName());

                }

                tabCounter--;
                writer.write(getCurrentTabSpace());
                writer.write("</EnzymeName>");
                writer.newLine();

                tabCounter--;
                writer.write(getCurrentTabSpace());
                writer.write("</Enzyme>");
                writer.newLine();

                tabCounter--;

            }
        }

        writer.write(getCurrentTabSpace());
        writer.write("</Enzymes>");
        writer.newLine();

        // fragment tolerance
        writer.write(getCurrentTabSpace());
        writer.write("<FragmentTolerance>");
        writer.newLine();
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

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam accession=\"MS:1001412\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        writer.write(fragmentIonToleranceUnit);
        writer.write("\" unitAccession=\"");
        writer.write(unitAccession);
        writer.write("\" value=\"");
        writer.write(Double.toString(searchParameters.getFragmentIonAccuracy()));
        writer.write("\" ");
        writer.write("name=\"search tolerance plus value\" />");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam accession=\"MS:1001413\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        writer.write(fragmentIonToleranceUnit);
        writer.write("\" unitAccession=\"");
        writer.write(unitAccession);
        writer.write("\" value=\"");
        writer.write(Double.toString(searchParameters.getFragmentIonAccuracy()));
        writer.write("\" name=\"search tolerance minus value\" />");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</FragmentTolerance>");
        writer.newLine();

        // precursor tolerance
        writer.write(getCurrentTabSpace());
        writer.write("<ParentTolerance>");
        writer.newLine();
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

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam accession=\"MS:1001412\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        writer.write(precursorIonToleranceUnit);
        writer.write("\" unitAccession=\"UO:0000169\" value=\"");
        writer.write(Double.toString(searchParameters.getPrecursorAccuracy()));
        writer.write("\" name=\"search tolerance plus value\" />");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam accession=\"MS:1001413\" cvRef=\"PSI-MS\" unitCvRef=\"UO\" unitName=\"");
        writer.write(precursorIonToleranceUnit);
        writer.write("\" unitAccession=\"UO:0000169\" value=\"");
        writer.write(Double.toString(searchParameters.getPrecursorAccuracy()));
        writer.write("\" name=\"search tolerance minus value\" />");

        tabCounter--;
        writer.newLine();
        writer.write(getCurrentTabSpace());
        writer.write("</ParentTolerance>");
        writer.newLine();

        // thresholds
        writer.write(getCurrentTabSpace());
        writer.write("<Threshold>");
        writer.newLine();
        tabCounter++;

        boolean targetDecoy = identificationParameters.getFastaParameters().isTargetDecoy();

        if (!targetDecoy) {

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1001494",
                            "no threshold",
                            null
                    )
            );

        } else {

            // Initial global thresholds
            IdMatchValidationParameters idMatchValidationPreferences = identificationParameters.getIdValidationParameters();
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1001364",
                            "peptide sequence-level global FDR",
                            Double.toString(
                                    Util.roundDouble(
                                            idMatchValidationPreferences.getDefaultPeptideFDR(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002350",
                            "PSM-level global FDR",
                            Double.toString(
                                    Util.roundDouble(
                                            idMatchValidationPreferences.getDefaultPsmFDR(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );

            ModificationLocalizationParameters ptmScoringPreferences = identificationParameters.getModificationLocalizationParameters();

            if (ptmScoringPreferences.isProbabilisticScoreCalculation()) {

                if (ptmScoringPreferences.getSelectedProbabilisticScore() == ModificationLocalizationScore.PhosphoRS) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002567",
                                    "phosphoRS score threshold",
                                    Double.toString(ptmScoringPreferences.getProbabilisticScoreThreshold())
                            )
                    );
                }
            }

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002557",
                            "D-Score threshold",
                            Double.toString(identificationParameters.getModificationLocalizationParameters().getDScoreThreshold())
                    )
            );

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
        writer.write(getCurrentTabSpace());
        writer.write("</Threshold>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</SpectrumIdentificationProtocol>");
        writer.newLine();

        // add ProteinDetectionProtocol
        writer.write(getCurrentTabSpace());
        writer.write("<ProteinDetectionProtocol analysisSoftware_ref=\"ID_software\" id=\"PeptideShaker_1\">");
        writer.newLine();
        tabCounter++;

//        br.write(getCurrentTabSpace() + "<AnalysisParams>" + lineBreak);
//        tabCounter++;
        // @TODO: add cv terms? (children of MS:1001302)
//        tabCounter--;
//        br.write(getCurrentTabSpace() + "</AnalysisParams>" + lineBreak);
        // protein level threshold
        writer.write(getCurrentTabSpace());
        writer.write("<Threshold>");
        writer.newLine();
        tabCounter++;

        if (!targetDecoy) {

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1001494",
                            "no threshold",
                            null
                    )
            );

        } else {

            PSMaps psMaps = new PSMaps();
            psMaps = (PSMaps) identification.getUrParam(psMaps);
            TargetDecoyMap proteinMap = psMaps.getProteinMap();
            TargetDecoyResults proteinTargetDecoyResults = proteinMap.getTargetDecoyResults();

            double threshold = proteinTargetDecoyResults.getUserInput() / 100;
            int thresholdType = proteinTargetDecoyResults.getInputType();

            if (thresholdType == 0) {

                // confidence
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002461",
                                "protein group-level global confidence",
                                Double.toString(
                                        Util.roundDouble(
                                                threshold,
                                                CONFIDENCE_DECIMALS
                                        )
                                )
                        )
                );

            } else if (proteinTargetDecoyResults.getInputType() == 1) {

                // FDR
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002369",
                                "protein group-level global FDR",
                                Double.toString(
                                        Util.roundDouble(
                                                threshold,
                                                CONFIDENCE_DECIMALS
                                        )
                                )
                        )
                );

            } else if (proteinTargetDecoyResults.getInputType() == 2) {

                // FNR
                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002460",
                                "protein group-level global FNR",
                                Double.toString(
                                        Util.roundDouble(
                                                threshold,
                                                CONFIDENCE_DECIMALS
                                        )
                                )
                        )
                );
            }
        }

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Threshold>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</ProteinDetectionProtocol>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AnalysisProtocolCollection>");
        writer.newLine();

    }

    /**
     * Write the data collection.
     */
    private void writeDataCollection() {

        writer.write(getCurrentTabSpace());
        writer.write("<DataCollection>");
        writer.newLine();
        tabCounter++;

        writeInputFileDetails();
        writeDataAnalysis();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</DataCollection>");
        writer.newLine();

    }

    /**
     * Write the data analysis section.
     */
    private void writeDataAnalysis() {

        writer.write(getCurrentTabSpace());
        writer.write("<AnalysisData>");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<SpectrumIdentificationList id=\"SIL_1\">");
        writer.newLine();
        tabCounter++;

        writeFragmentationTable();

        int psmCount = 0;

        // iterate the PSMs
        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            writeSpectrumIdentificationResult(
                    spectrumMatch.getSpectrumFile(),
                    spectrumMatch.getSpectrumTitle(),
                    ++psmCount
            );
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
        writer.write(getCurrentTabSpace());
        writer.write("</SpectrumIdentificationList>");
        writer.newLine();

        writeProteinDetectionList();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</AnalysisData>");
        writer.newLine();

    }

    /**
     * Write the protein groups.
     */
    private void writeProteinDetectionList() {

        writer.write(getCurrentTabSpace());
        writer.write("<ProteinDetectionList id=\"Protein_groups\">");
        writer.newLine();

        tabCounter++;

        int groupCpt = 0;

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

        ProteinMatch proteinMatch;
        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            long proteinGroupKey = proteinMatch.getKey();

            String proteinGroupId = "PAG_" + groupCpt++;

            writer.write(getCurrentTabSpace());
            writer.write("<ProteinAmbiguityGroup id=\"");
            writer.write(proteinGroupId);
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            String[] accessions = proteinMatch.getAccessions();
            String mainAccession = proteinMatch.getLeadingAccession();

            for (int j = 0; j < accessions.length; j++) {

                String accession = accessions[j];

                writer.write(getCurrentTabSpace());
                writer.write("<ProteinDetectionHypothesis id=\"");
                writer.write(proteinGroupId);
                writer.write("_");
                writer.write(Integer.toString(j + 1));
                writer.write("\" dBSequence_ref=\"");
                writer.write(accession);
                writer.write("\" passThreshold=\"");
                writer.write(Boolean.toString(psParameter.getMatchValidationLevel().isValidated()));
                writer.write("\">");
                writer.newLine();
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

                            writer.write(getCurrentTabSpace());
                            writer.write("<PeptideHypothesis peptideEvidence_ref=\"");
                            writer.write(peptideEvidenceId);
                            writer.write("\">");
                            writer.newLine();
                            tabCounter++;

                            for (long spectrumKey : peptideMatch.getSpectrumMatchesKeys()) {

                                writer.write(getCurrentTabSpace());
                                writer.write("<SpectrumIdentificationItemRef spectrumIdentificationItem_ref=\"");
                                writer.write(spectrumIds.get(spectrumKey));
                                writer.write("\"/>");
                                writer.newLine();

                            }

                            tabCounter--;
                            writer.write(getCurrentTabSpace());
                            writer.write("</PeptideHypothesis>");
                            writer.newLine();

                        } else {

                            throw new IllegalArgumentException("No peptide evidence id found for key '" + pepEvidenceKey + "'.");

                        }
                    }
                }

                // add main protein cv terms
                if (accession.equalsIgnoreCase(mainAccession)) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002403",
                                    "group representative",
                                    null
                            )
                    );
                }

                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002401",
                                "leading protein",
                                null
                        )
                );

                // add protein coverage cv term - main protein only
                if (accession.equalsIgnoreCase(mainAccession)) {

                    double validatedCoverage = identificationFeaturesGenerator.getValidatedSequenceCoverage(proteinGroupKey);
                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001093",
                                    "sequence coverage",
                                    Double.toString(
                                            Util.roundDouble(
                                                    validatedCoverage,
                                                    CONFIDENCE_DECIMALS
                                            )
                                    )
                            )
                    );
                }

                tabCounter--;
                writer.write(getCurrentTabSpace());
                writer.write("</ProteinDetectionHypothesis>");
                writer.newLine();

            }

            // add protein group cv terms
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002470",
                            "PeptideShaker protein group score",
                            Double.toString(
                                    Util.roundDouble(
                                            psParameter.getTransformedScore(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002471",
                            "PeptideShaker protein group confidence",
                            Double.toString(
                                    Util.roundDouble(
                                            psParameter.getConfidence(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002542",
                            "PeptideShaker protein confidence type",
                            psParameter.getMatchValidationLevel().getName()
                    )
            );
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002415",
                            "protein group passes threshold",
                            Boolean.toString(
                                    psParameter.getMatchValidationLevel().isValidated()
                            )
                    )
            );

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</ProteinAmbiguityGroup>");
            writer.newLine();

            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                break;

            }
        }

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1002404",
                        "count of identified proteins",
                        Integer.toString(
                                identificationFeaturesGenerator.getNValidatedProteins()
                        )
                )
        );
        // @TODO: add children of MS:1001184 - search statistics? (date / time search performed, number of molecular hypothesis considered, search time taken)

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</ProteinDetectionList>");
        writer.newLine();

    }

    /**
     * Write a spectrum identification result.
     *
     * @param spectrumFile The name of the spectrum file.
     * @param spectrumTitle The title of the spectrum.
     * @param spectrumMatchIndex The index of the spectrum match.
     */
    private void writeSpectrumIdentificationResult(
            String spectrumFile,
            String spectrumTitle,
            int spectrumMatchIndex
    ) {

        long spectrumKey = SpectrumMatch.getKey(spectrumFile, spectrumTitle);
        SpectrumMatch spectrumMatch = (SpectrumMatch) identification.retrieveObject(spectrumKey);

        // @TODO: iterate all assumptions and not just the best one?
        PeptideAssumption bestPeptideAssumption = spectrumMatch.getBestPeptideAssumption();

        if (bestPeptideAssumption != null) {

            String spectrumIdentificationResultItemKey = "SIR_" + spectrumMatchIndex;

            writer.write(getCurrentTabSpace());
            writer.write("<SpectrumIdentificationResult spectraData_ref=\"");
            writer.write(spectrumFile);
            writer.write("\" spectrumID=\"index=");
            writer.write(spectrumTitleToIndexMap.get(spectrumFile).get(spectrumTitle).toString());
            writer.write("\" id=\"");
            writer.write(spectrumIdentificationResultItemKey);
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            PSParameter psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

            int rank = 1;
            String spectrumIdentificationItemKey = "SII_" + spectrumMatchIndex + "_" + rank;
            spectrumIds.put(spectrumKey, spectrumIdentificationItemKey);

            long peptideMatchKey = bestPeptideAssumption.getPeptide().getMatchingKey(identificationParameters.getSequenceMatchingParameters());

            writer.write(getCurrentTabSpace());
            writer.write("<SpectrumIdentificationItem passThreshold=\"");
            writer.write(Boolean.toString(psmParameter.getMatchValidationLevel().isValidated()));
            writer.write("\" rank=\"");
            writer.write(Integer.toString(rank));
            writer.write("\" peptide_ref=\"");
            writer.write(Long.toString(peptideMatchKey));
            writer.write("\" calculatedMassToCharge=\"");
            writer.write(Double.toString(bestPeptideAssumption.getTheoreticMz()));
            writer.write("\" experimentalMassToCharge=\"");
            writer.write(Double.toString(spectrumProvider.getPrecursorMz(spectrumFile, spectrumTitle)));
            writer.write("\" chargeState=\"");
            writer.write(Integer.toString(bestPeptideAssumption.getIdentificationCharge()));
            writer.write("\" id=\"");
            writer.write(spectrumIdentificationItemKey);
            writer.write("\">");
            writer.newLine();
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

                    writer.write(getCurrentTabSpace());
                    writer.write("<PeptideEvidenceRef peptideEvidence_ref=\"");
                    writer.write(peptideEvidenceId);
                    writer.write("\"/>");
                    writer.newLine();

                }
            }

            // add the fragment ions detected
            if (writeFragmentIons) {

                // add the fragment ion annotation
                AnnotationParameters annotationParameters = identificationParameters.getAnnotationParameters();
                Spectrum spectrum = spectrumProvider.getSpectrum(spectrumFile, spectrumTitle);
                ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
                SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
                SpecificAnnotationParameters specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(
                        spectrumFile,
                        spectrumTitle,
                        bestPeptideAssumption,
                        modificationParameters,
                        sequenceProvider,
                        modificationSequenceMatchingParameters,
                        peptideSpectrumAnnotator
                );
                IonMatch[] matches = peptideSpectrumAnnotator.getSpectrumAnnotation(
                        annotationParameters,
                        specificAnnotationParameters,
                        spectrumFile,
                        spectrumTitle,
                        spectrum,
                        bestPeptideAssumption.getPeptide(),
                        modificationParameters,
                        sequenceProvider,
                        modificationSequenceMatchingParameters
                );

                // organize the fragment ions by ion type
                HashMap<String, HashMap<Integer, ArrayList<IonMatch>>> allFragmentIons = new HashMap<>();

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

                    writer.write(getCurrentTabSpace());
                    writer.write("<Fragmentation>");
                    writer.newLine();
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

                                mzValues.append(ionMatch.peakMz)
                                        .append(' ');
                                intensityValues.append(ionMatch.peakIntensity)
                                        .append(' ');
                                errorValues.append(ionMatch.getAbsoluteError())
                                        .append(' ');

                            }

                            // add the supported fragment ions
                            if (fragmentIonCvTerm != null) {

                                writer.write(getCurrentTabSpace());
                                writer.write("<IonType charge=\"");
                                writer.write(Integer.toString(fragmentCharge));
                                writer.write("\" index=\"");
                                writer.write(indexes.toString().trim());
                                writer.write("\">");
                                writer.newLine();
                                tabCounter++;

                                writer.write(getCurrentTabSpace());
                                writer.write("<FragmentArray measure_ref=\"Measure_MZ\" values=\"");
                                writer.write(mzValues.toString().trim());
                                writer.write("\"/>");
                                writer.newLine();

                                writer.write(getCurrentTabSpace());
                                writer.write("<FragmentArray measure_ref=\"Measure_Int\" values=\"");
                                writer.write(intensityValues.toString().trim());
                                writer.write("\"/>");
                                writer.newLine();

                                writer.write(getCurrentTabSpace());
                                writer.write("<FragmentArray measure_ref=\"Measure_Error\" values=\"");
                                writer.write(errorValues.toString().trim());
                                writer.write("\"/>");
                                writer.newLine();

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
                                writer.write(getCurrentTabSpace());
                                writer.write("</IonType>");
                                writer.newLine();

                            }
                        }
                    }

                    tabCounter--;
                    writer.write(getCurrentTabSpace());
                    writer.write("</Fragmentation>");
                    writer.newLine();

                }
            }

            // add peptide shaker score and confidence
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002466",
                            "PeptideShaker PSM score",
                            Double.toString(
                                    Util.roundDouble(
                                            psmParameter.getTransformedScore(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002467",
                            "PeptideShaker PSM confidence",
                            Double.toString(
                                    Util.roundDouble(
                                            psmParameter.getConfidence(),
                                            CONFIDENCE_DECIMALS
                                    )
                            )
                    )
            );

            switch (mzIdentMLVersion) {

                case v1_1:
                    break;

                case v1_2:

                    ModificationLocalizationParameters modificationLocalicationParameters = identificationParameters.getModificationLocalizationParameters();
                    PeptideMatch peptideMatch = (PeptideMatch) identification.retrieveObject(peptideMatchKey);
                    PSModificationScores psModificationScores = (PSModificationScores) spectrumMatch.getUrParam(PSModificationScores.dummy);

                    if (psModificationScores != null) {

                        Peptide peptide = bestPeptideAssumption.getPeptide();

                        if (peptide.getVariableModifications().length > 0) {

                            Set<String> scoredModifications = psModificationScores.getScoredModifications();
                            HashSet<String> coveredModifications = new HashSet<>(scoredModifications.size());

                            for (String modName : scoredModifications) {

                                Modification modification = modificationProvider.getModification(modName);

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

                                                        writeCvTerm(
                                                                new CvTerm(
                                                                        "PSI-MS",
                                                                        "MS:1001969",
                                                                        "phosphoRS score",
                                                                        sb.toString()
                                                                )
                                                        );
                                                    }
                                                }
                                            }

                                            double score = modificationScoring.getDeltaScore(site);

                                            if (score > 0) {

                                                String valid = "true";

                                                if (score < identificationParameters.getModificationLocalizationParameters().getDScoreThreshold()) {

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

                                                writeCvTerm(
                                                        new CvTerm(
                                                                "PSI-MS",
                                                                "MS:1002536",
                                                                "D-Score",
                                                                sb.toString()
                                                        )
                                                );
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002469",
                                    "PeptideShaker peptide confidence",
                                    Double.toString(
                                            peptideParameter.getConfidence()
                                    )
                            )
                    );
                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002468",
                                    "PeptideShaker peptide score",
                                    Double.toString(
                                            peptideParameter.getTransformedScore()
                                    )
                            )
                    );
                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002500",
                                    "peptide passes threshold",
                                    Boolean.toString(
                                            peptideParameter.getMatchValidationLevel().isValidated()
                                    )
                            )
                    );
                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002520",
                                    "peptide group ID",
                                    Long.toString(
                                            peptideMatchKey
                                    )
                            )
                    );

                    psModificationScores = (PSModificationScores) peptideMatch.getUrParam(PSModificationScores.dummy);

                    if (psModificationScores != null) {

                        Peptide peptide = peptideMatch.getPeptide();

                        if (peptide.getVariableModifications().length > 0) {

                            Set<String> scoredModifications = psModificationScores.getScoredModifications();
                            HashSet<String> coveredModifications = new HashSet<>(scoredModifications.size());

                            for (String modName : scoredModifications) {

                                if (!coveredModifications.contains(modName)) {

                                    coveredModifications.add(modName);
                                    Modification modification = modificationProvider.getModification(modName);
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

                                                        writeCvTerm(
                                                                new CvTerm(
                                                                        "PSI-MS",
                                                                        "MS:1002550",
                                                                        "peptide:phosphoRS score",
                                                                        sb.toString()
                                                                )
                                                        );
                                                    }
                                                }
                                            }

                                            double score = modificationScoring.getDeltaScore(site);

                                            if (score > 0) {

                                                String valid = "true";

                                                if (score < identificationParameters.getModificationLocalizationParameters().getDScoreThreshold()) {

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

                                                writeCvTerm(
                                                        new CvTerm(
                                                                "PSI-MS",
                                                                "MS:1002553",
                                                                "peptide:D-Score",
                                                                sb.toString()
                                                        )
                                                );
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
            HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = identification.getSpectrumMatch(spectrumKey).getPeptideAssumptionsMap();

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

            for (Entry<Integer, Double> entry : scores.entrySet()) { // @TODO: what about IdentiPy and Morpheus?

                int advocate = entry.getKey();
                double eValue = entry.getValue();

                if (advocate == Advocate.msgf.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002052",
                                    "MS-GF:SpecEValue",
                                    Double.toString(eValue)
                            )
                    );

                } else if (advocate == Advocate.mascot.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001172",
                                    "Mascot:expectation value",
                                    Double.toString(eValue)
                            )
                    );

                } else if (advocate == Advocate.omssa.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001328",
                                    "OMSSA:evalue",
                                    Double.toString(eValue)
                            )
                    );

                } else if (advocate == Advocate.xtandem.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001330",
                                    "X!Tandem:expect",
                                    Double.toString(eValue)
                            )
                    );

                } else if (advocate == Advocate.comet.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002257",
                                    "Comet:expectation value",
                                    Double.toString(eValue)
                            )
                    );

                } else if (advocate == Advocate.myriMatch.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001589",
                                    "MyriMatch:MVH",
                                    Double.toString(eValue)
                            )
                    );

                } else {

                    writeUserParam(Advocate.getAdvocate(advocate).getName() + " e-value", Double.toString(eValue)); // @TODO: add Tide if Tide CV term is added

                }
            }

            // add the additional search engine scores
            if (mascotScore != null) {

                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1001171",
                                "Mascot:score",
                                Double.toString(mascotScore)
                        )
                );

            }
            if (msAmandaScore != null) {

                writeCvTerm(
                        new CvTerm(
                                "PSI-MS",
                                "MS:1002319",
                                "Amanda:AmandaScore",
                                Double.toString(msAmandaScore)
                        )
                );
            }

            // add other cv and user params
            writer.write(getCurrentTabSpace());
            writer.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001117\" name=\"theoretical mass\" value=\"");
            writer.write(Double.toString(bestPeptideAssumption.getTheoreticMass()));
            writer.write("\" unitCvRef=\"UO\" unitAccession=\"UO:0000221\" unitName=\"dalton\"/>");
            writer.newLine();

            // add validation level information
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1002540",
                            "PeptideShaker PSM confidence type",
                            psmParameter.getMatchValidationLevel().getName()
                    )
            );
            tabCounter--;

            writer.write(getCurrentTabSpace());
            writer.write("</SpectrumIdentificationItem>");
            writer.newLine();

            // add the spectrum title
            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1000796",
                            "spectrum title",
                            spectrumTitle
                    )
            );

            // add the precursor retention time
            double precursorRt = spectrumProvider.getPrecursorRt(spectrumFile, spectrumTitle);

            if (!Double.isNaN(precursorRt)) {

                writer.write(getCurrentTabSpace());
                writer.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1000894\" name=\"retention time\" value=\"");
                writer.write(Double.toString(precursorRt));
                writer.write("\" unitCvRef=\"UO\" unitAccession=\"UO:0000010\" unitName=\"second\"/>");
                writer.newLine();

            }

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</SpectrumIdentificationResult>");
            writer.newLine();

        }
    }

    /**
     * Write the fragmentation table.
     */
    private void writeFragmentationTable() {

        writer.write(getCurrentTabSpace());
        writer.write("<FragmentationTable>");
        writer.newLine();
        tabCounter++;

        // mz
        writer.write(getCurrentTabSpace());
        writer.write("<Measure id=\"Measure_MZ\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001225\" name=\"product ion m/z\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\" />");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Measure>");
        writer.newLine();

        // intensity
        writer.write(getCurrentTabSpace());
        writer.write("<Measure id=\"Measure_Int\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001226\" name=\"product ion intensity\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000131\" unitName=\"number of detector counts\"/>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Measure>");
        writer.newLine();

        // mass error
        writer.write(getCurrentTabSpace());
        writer.write("<Measure id=\"Measure_Error\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam cvRef=\"PSI-MS\" accession=\"MS:1001227\" name=\"product ion m/z error\" unitCvRef=\"PSI-MS\" unitAccession=\"MS:1000040\" unitName=\"m/z\"/>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Measure>");
        writer.newLine();

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</FragmentationTable>");
        writer.newLine();
    }

    /**
     * Write the input file details.
     */
    private void writeInputFileDetails() {

        writer.write(getCurrentTabSpace());
        writer.write("<Inputs>");
        writer.newLine();
        tabCounter++;

        int sourceFileCounter = 1;

        // add the search result files
        for (String idFilePath : projectDetails.getIdentificationFiles()) {

            File idFile = new File(idFilePath);

            // @TODO: add MS:1000568 - MD5?
//            FileInputStream fis = new FileInputStream(new File("foo"));
//            String md5 = DigestUtils.md5Hex(fis);
//            fis.close();
            writer.write(getCurrentTabSpace());
            writer.write("<SourceFile location=\"");
            writer.write(idFile.toURI().toString());
            writer.write("\" id=\"SourceFile_");
            writer.write(Integer.toString(sourceFileCounter++));
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            writer.write(getCurrentTabSpace());
            writer.write("<FileFormat>");
            writer.newLine();
            tabCounter++;

            String idFileName = IoUtil.getFileName(idFile);
            HashMap<String, ArrayList<String>> algorithms = projectDetails.getIdentificationAlgorithmsForFile(idFileName);

            for (String algorithmName : algorithms.keySet()) {

                Advocate advocate = Advocate.getAdvocate(algorithmName);

                int advocateIndex = advocate.getIndex();

                if (advocateIndex == Advocate.mascot.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001199",
                                    "Mascot DAT format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.xtandem.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001401",
                                    "X!Tandem xml format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.omssa.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001400",
                                    "OMSSA xml format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.msgf.getIndex() || advocateIndex == Advocate.myriMatch.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002073",
                                    "mzIdentML format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.msAmanda.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002459",
                                    "MS Amanda csv format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.comet.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1001421",
                                    "pepXML format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.tide.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1000914",
                                    "tab delimited text format",
                                    null
                            )
                    );

                } else if (advocateIndex == Advocate.andromeda.getIndex()) {

                    writeCvTerm(
                            new CvTerm(
                                    "PSI-MS",
                                    "MS:1002576",
                                    "Andromeda result file",
                                    null
                            )
                    );

                } else {
                    // no cv term available for the given advocate...
                    // @TODO: should add for IdentiPy (pepxml only?) and Morpheus (mxid or pepxml?)
                }
            }

            // @TODO: add children of MS:1000561 - data file checksum type?
            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</FileFormat>");
            writer.newLine();

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</SourceFile>");
            writer.newLine();
        }

        // add the database
        File fastaFile = new File(projectDetails.getFastaFile());
        writer.write(getCurrentTabSpace());
        writer.write("<SearchDatabase numDatabaseSequences=\"");
        writer.write(Integer.toString(fastaSummary.nSequences));
        writer.write("\" location=\"");
        writer.write(fastaFile.toURI().toString());
        writer.write("\" id=\"" + "SearchDB_1\">");
        writer.newLine();
        tabCounter++;

        writer.write(getCurrentTabSpace());
        writer.write("<FileFormat>");
        writer.newLine();
        tabCounter++;

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001348",
                        "FASTA format",
                        null
                )
        );
        tabCounter--;

        writer.write(getCurrentTabSpace());
        writer.write("</FileFormat>");
        writer.newLine();

        writer.write(getCurrentTabSpace());
        writer.write("<DatabaseName>");
        writer.newLine();
        tabCounter++;

        writeUserParam(fastaFile.getName()); // @TODO: add database type? children of MS:1001013 - database name??? for example: MS:1001104 (database UniProtKB/Swiss-Prot)

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</DatabaseName>");
        writer.newLine();

        writeCvTerm(
                new CvTerm(
                        "PSI-MS",
                        "MS:1001073",
                        "database type amino acid",
                        null
                )
        );

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</SearchDatabase>");
        writer.newLine();

        // add the spectra location
        for (String spectrumFileNameWithoutExtension : spectrumProvider.getOrderedFileNamesWithoutExtensions()) {

            String spectrumFilePath = projectDetails.getSpectrumFilePath(spectrumFileNameWithoutExtension);
            File spectrumFile = new File(spectrumFilePath);

            writer.write(getCurrentTabSpace());
            writer.write("<SpectraData location=\"");
            writer.write(spectrumFile.toURI().toString());
            writer.write("\" id=\"");
            writer.write(spectrumFileNameWithoutExtension);
            writer.write("\" name=\"");
            writer.write(spectrumFile.getName());
            writer.write("\">");
            writer.newLine();
            tabCounter++;

            writer.write(getCurrentTabSpace());
            writer.write("<FileFormat>");
            writer.newLine();
            tabCounter++;

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1001062",
                            "Mascot MGF format",
                            null
                    )
            );

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</FileFormat>");
            writer.newLine();

            writer.write(getCurrentTabSpace());
            writer.write("<SpectrumIDFormat>");
            writer.newLine();
            tabCounter++;

            writeCvTerm(
                    new CvTerm(
                            "PSI-MS",
                            "MS:1000774",
                            "multiple peak list nativeID format",
                            null
                    )
            );

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</SpectrumIDFormat>");
            writer.newLine();

            tabCounter--;
            writer.write(getCurrentTabSpace());
            writer.write("</SpectraData>");
            writer.newLine();

        }

        tabCounter--;
        writer.write(getCurrentTabSpace());
        writer.write("</Inputs>");
        writer.newLine();

    }

    /**
     * Writes the mzIdentML start tag.
     */
    private void writeMzIdentMLStartTag() {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.newLine();

        switch (mzIdentMLVersion) {

            case v1_1:
                writer.writeLine(
                        "<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\""
                        + " xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\""
                        //+ " xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.1 http://www.psidev.info/files/mzIdentML1.1.0.xsd\""
                        + " xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\""
                        + " version=\"1.1.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">"
                );
                break;

            case v1_2:
                writer.writeLine(
                        "<MzIdentML id=\"PeptideShaker v" + peptideShakerVersion + "\""
                        + " xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" "
                        //+ " xsi:schemaLocation=\"http://psidev.info/psi/pi/mzIdentML/1.2 http://www.psidev.info/files/mzIdentML1.2.0.xsd\""
                        + " xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.2\""
                        + " version=\"1.2.0\" "
                        + "creationDate=\"" + df.format(new Date()) + "\">"
                );
                break;

            default:
                throw new UnsupportedOperationException("mzIdentML version " + mzIdentMLVersion.name + " not supported.");

        }

        tabCounter++;

    }

    /**
     * Writes the mzIdentML end tag.
     */
    private void writeMzIdentMLEndTag() {

        tabCounter--;
        writer.write("</MzIdentML>");

    }

    /**
     * Convenience method returning the tabs at the beginning of each line
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
                StringBuilder sb = new StringBuilder(tabCounter);
                for (int i = 0; i < tabCounter; i++) {
                    sb.append('\t');
                }
                return sb.toString();

        }
    }

    /**
     * Convenience method writing a CV term.
     *
     * @param cvTerm the CV term
     */
    private void writeCvTerm(CvTerm cvTerm) {

        writeCvTerm(cvTerm, true);

    }

    /**
     * Convenience method writing a CV term.
     *
     * @param cvTerm the CV term
     * @param showValue decides if the CV terms value (if existing) is printed
     * or not
     */
    private void writeCvTerm(
            CvTerm cvTerm,
            boolean showValue
    ) {

        writer.write(getCurrentTabSpace());
        writer.write("<cvParam cvRef=\"");
        writer.write(StringEscapeUtils.escapeHtml4(cvTerm.getOntology()));
        writer.write("\" accession=\"");
        writer.write(cvTerm.getAccession());
        writer.write("\" name=\"");
        writer.write(StringEscapeUtils.escapeHtml4(cvTerm.getName()));
        writer.write("\"");

        writeCvTermValue(cvTerm, showValue);

    }

    /**
     * Convenience method writing the value element of a CV term.
     *
     * @param cvTerm the CV term
     * @param showValue decides if the CV terms value (if existing) is printed
     * or not
     */
    private void writeCvTermValue(
            CvTerm cvTerm,
            boolean showValue
    ) {

        String value = cvTerm.getValue();

        if (showValue && value != null) {

            writer.write(" value=\"");
            writer.write(StringEscapeUtils.escapeHtml4(value));
            writer.write("\"/>");

        } else {

            writer.write("/>");

        }

        writer.newLine();

    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param userParamAsString the user parameter as a string
     */
    private void writeUserParam(
            String userParamAsString
    ) {

        writer.write(getCurrentTabSpace());
        writer.write("<userParam name=\"");
        writer.write(StringEscapeUtils.escapeHtml4(userParamAsString));
        writer.write("\"/>");

        writer.newLine();

    }

    /**
     * Convenience method writing a user parameter.
     *
     * @param name the name of the user parameter
     * @param value the value of the user parameter
     */
    private void writeUserParam(
            String name,
            String value
    ) {

        writer.write(getCurrentTabSpace());
        writer.write("<userParam name=\"");
        writer.write(StringEscapeUtils.escapeHtml4(name));
        writer.write("\" value=\"");
        writer.write(StringEscapeUtils.escapeHtml4(value));
        writer.write("\" />");
        writer.newLine();

    }
}
