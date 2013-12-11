package eu.isas.peptideshaker.export;

import com.compomics.util.BinaryArrayImpl;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.advocates.SpectrumIdentificationAlgorithm;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.ptm.PtmScore;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PrideObjectsFactory;
import com.compomics.util.pride.PtmToPrideMap;
import com.compomics.util.pride.prideobjects.*;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.pride.PrideExportDialog;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to mzIdentML. (Work in
 * progress...)
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class MzIdentMLExport {

    // @TODO: make gui independent
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
     * A reference to the PrideExportDialog.
     */
    private PrideExportDialog prideExportDialog; // @TODO: should be renamed!!!
    /**
     * The mzIdentML xsd.
     */
    private String mzIdentMLXsd = "\"http://psidev.info/psi/pi/mzIdentML/1.1\"";

    /**
     * Constructor.
     *
     * @param peptideShakerGUI Instance of the main GUI class
     * @param prideExportDialog A reference to the PrideExportDialog.
     * @param experimentTitle Title of the experiment
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
     * @throws FileNotFoundException Exception thrown whenever a file was not
     * found
     * @throws IOException Exception thrown whenever an error occurred while
     * reading/writing a file
     * @throws ClassNotFoundException Exception thrown whenever an error
     * occurred while deserializing a pride object
     */
    public MzIdentMLExport(PeptideShakerGUI peptideShakerGUI, PrideExportDialog prideExportDialog, String experimentTitle, String experimentLabel, String experimentDescription, String experimentProject,
            ReferenceGroup referenceGroup, ContactGroup contactGroup, Sample sample, Protocol protocol, Instrument instrument,
            File outputFolder, String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        this.peptideShakerGUI = peptideShakerGUI;
        this.prideExportDialog = prideExportDialog;
        this.experimentTitle = experimentTitle;
        this.experimentLabel = experimentLabel;
        this.experimentDescription = experimentDescription;
        this.experimentProject = experimentProject;
        this.referenceGroup = referenceGroup;
        this.contactGroup = contactGroup;
        this.sample = sample;
        this.protocol = protocol;
        this.instrument = instrument;
        PrideObjectsFactory prideObjectsFactory = PrideObjectsFactory.getInstance(); // @TODO: should be renamed!!!
        ptmToPrideMap = prideObjectsFactory.getPtmToPrideMap();
        r = new FileWriter(new File(outputFolder, fileName + ".xml"));
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
     */
    public void createMzIdentMLFile(ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

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
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(100);
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
                + "<cv=id=\"PSI-MS\" "
                + "uri=\"http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo\" "
                + "version=\"2.25.0\" "
                + "fullName=\"PSI-MS\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv=id=\"UNIMODS\" "
                + "uri=\"http://www.unimod.org/obo/unimod.obo\" "
                + "fullName=\"UNIMOD\"/>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace()
                + "<cv=id=\"UO\" "
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

        // @TODO: also add SearchGUI and search engines used
        br.write(getCurrentTabSpace() + "<AnalysisSoftware "
                + "version=\"" + peptideShakerGUI.getVersion() + " "
                + "name=\"PeptideShaker "
                + "id=\"ID_software\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SoftwareName>" + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001476 " // @TODO: add PeptideShaker CV term
                + "cvRef=\"PSI-MS "
                + "name=\"PeptideShaker\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SoftwareName>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisSoftware>" + System.getProperty("line.separator"));
    }

    /**
     * Write the provider details.
     *
     * @throws IOException
     */
    private void writeProviderDetails() throws IOException {

        br.write(getCurrentTabSpace() + "<Provider id=PROVIDER xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ContactRole contact_ref=\"PERSON_DOC_OWNER\">" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<Role>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001271 " // @TODO: add PeptideShaker CV term
                + "cvRef=\"PSI-MS "
                + "name=\"researcher\">" // @TODO: add the data owner name here!!
                + System.getProperty("line.separator"));
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
                + "firstName=\"firstname " // @TODO: add from user input
                + "lastName=\"lastname " // @TODO: add from user input
                + "id=\"PERSON_DOC_OWNER\">"
                + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Affiliation organization_ref=\"ORG_DOC_OWNER\"/>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Person>" + System.getProperty("line.separator"));

        br.write(getCurrentTabSpace() + "<Organization name=\"myworkplace\" id=\"ORG_DOC_OWNER\"/>" + System.getProperty("line.separator")); // @TODO: add from user input

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AuditCollection>" + System.getProperty("line.separator"));
    }

    /**
     * Write the sequence collection.
     */
    private void writeSequenceCollection() {
        // <SequenceCollection xmlns="http://psidev.info/psi/pi/mzIdentML/1.1">
        // iterate all the protein sequences
//        <DBSequence accession="psu|NC_LIV_020800" searchDatabase_ref="SearchDB_1" length="376"
//            id="dbseq_psu|NC_LIV_020800">
//            <Seq>MADEEVQALVVDNGSGNVKAGVAGDDAPRAVFPSIVGKPKNPGIMVGMEEKDCYVGDEAQSKRGILTLKYPIEHGIVTNWDDMEKIWHHTFYNELRVAPEEHPVLLTEAPLNPKANRERMTQIMFETFNVPAMYVAIQAVLSLYSSGRTTGIVLDSGDGVSHTVPIYEGYALPHAIMRLDLAGRDLTEYMMKILHERGYGFTTSAEKEIVRDIKEKLCYIALDFDEEMKAAEDSSDIEKSYELPDGNIITVGNERFRCPEALFQPSFLGKEAAGVHRTTFDSIMKCDVDIRKDLYGNVVLSGGTTMYEGIGERLTKELTSLAPSTMKIKVVAPPERKYSVWIGGSILSSLSTFQQMWITKEEYDESGPSIVHRKCF</Seq>
//        </DBSequence>
        // iterate all the peptides
//        <Peptide id="LCYIALDFDEEMKAAEDSSDIEK_15.9949@M$228;_57.0215@C$218;_">
//            <PeptideSequence>LCYIALDFDEEMKAAEDSSDIEK</PeptideSequence>
//            <Modification monoisotopicMassDelta="57.0215" residues="C" location="2">
//                <cvParam accession="UNIMOD:4" cvRef="UNIMOD" name="Carbamidomethyl"/>
//            </Modification>
//            <Modification monoisotopicMassDelta="15.9949" residues="M" location="12">
//                <cvParam accession="UNIMOD:35" cvRef="UNIMOD" name="Oxidation"/>
//            </Modification>
//        </Peptide>
        // iterate the peptide evidence
//        <PeptideEvidence isDecoy="false" post="S" pre="K" end="239" start="217"
//            peptide_ref="LCYIALDFDEEMKAAEDSSDIEK_15.9949@M$228;_57.0215@C$218;_"
//            dBSequence_ref="dbseq_psu|NC_LIV_020800" id="PE1_2_0"/>
        // </SequenceCollection>
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
                + "accession=\"MS:1001083 "
                + "cvRef=\"PSI-MS "
                + "name=\"ms-ms search\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SearchType>" + System.getProperty("line.separator"));

        // the search parameters
        br.write(getCurrentTabSpace() + "<AdditionalSearchParams>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001211 "
                + "cvRef=\"PSI-MS "
                + "name=\"parent mass type mono\">"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001256 "
                + "cvRef=\"PSI-MS "
                + "name=\"fragment mass type mono\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</AdditionalSearchParams>" + System.getProperty("line.separator"));

        // the modifications
        br.write(getCurrentTabSpace() + "<ModificationParams>" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: add the modification mappings
//        br.write(getCurrentTabSpace() + "<SearchModification>" + System.getProperty("line.separator"));
//        tabCounter++;
//                <SearchModification residues="M" massDelta="15.99492" fixedMod="false">
//                    <cvParam accession="UNIMOD:35" cvRef="UNIMOD" name="Oxidation"/>
//                </SearchModification>
//                <SearchModification residues="C" massDelta="57.02147" fixedMod="true">
//                    <cvParam accession="UNIMOD:4" cvRef="UNIMOD" name="Carbamidomethyl"/>
//                </SearchModification>
//        
//        br.write(getCurrentTabSpace() + "<cvParam "
//                + "accession=\"MS:1001211 " // @TODO: add PeptideShaker CV term
//                + "cvRef=\"PSI-MS " 
//                + "name=\"parent mass type mono\">" // @TODO: add the data owner name here!!
//                + System.getProperty("line.separator"));
//        br.write(getCurrentTabSpace() + "<cvParam "
//                + "accession=\"MS:1001256 " // @TODO: add PeptideShaker CV term
//                + "cvRef=\"PSI-MS " 
//                + "name=\"fragment mass type mono\">" // @TODO: add the data owner name here!!
//                + System.getProperty("line.separator"));
//        tabCounter--;  
//        br.write(getCurrentTabSpace() + "</SearchModification>" + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ModificationParams>" + System.getProperty("line.separator"));

        // enzyme
        br.write(getCurrentTabSpace() + "<Enzymes independent=\"false\">" + System.getProperty("line.separator")); // @TODO: what does false mean???
        tabCounter++;

        // @TODO: set the enymes from the search params
        br.write(getCurrentTabSpace() + "<Enzyme "
                + "semiSpecific=\"false\" " // @TODO: what does false mean???
                + "cTermGain=\"OH\" "
                + "nTermGain=\"H\" "
                + "id=\"Enz1\">"
                + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<EnzymeName>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001251 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "name=\"Trypsin\">" // @TODO: set the enymes from the search params
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
                + "accession=\"MS:1001412 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "unitCvRef=\"UO "
                + "unitName=\"dalton "
                + "unitAccession=\"UO:0000221 "
                + "value=\"0.8 "
                + "name=\"search tolerance plus value\">"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "unitCvRef=\"UO "
                + "unitName=\"dalton "
                + "unitAccession=\"UO:0000221 "
                + "value=\"0.8 "
                + "name=\"search tolerance minus value\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</FragmentTolerance>" + System.getProperty("line.separator"));

        // precursor tolerance
        br.write(getCurrentTabSpace() + "<ParentTolerance>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001412 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "unitCvRef=\"UO "
                + "unitName=\"dalton "
                + "unitAccession=\"UO:0000221 "
                + "value=\"1.5 "
                + "name=\"search tolerance plus value\">"
                + System.getProperty("line.separator"));
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001413 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "unitCvRef=\"UO "
                + "unitName=\"dalton "
                + "unitAccession=\"UO:0000221 "
                + "value=\"1.5 "
                + "name=\"search tolerance minus value\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</ParentTolerance>" + System.getProperty("line.separator"));

        // thresholds
        br.write(getCurrentTabSpace() + "<Threshold>" + System.getProperty("line.separator"));
        tabCounter++;
        br.write(getCurrentTabSpace() + "<cvParam "
                + "accession=\"MS:1001494 " // @TODO: set the enymes from the search params
                + "cvRef=\"PSI-MS "
                + "name=\"no threshold\">"
                + System.getProperty("line.separator"));
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Threshold>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationProtocol>" + System.getProperty("line.separator"));

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisProtocolCollection>" + System.getProperty("line.separator"));
    }

    private void writeDataCollection() throws IOException {

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
    private void writeDataAnalysis() throws IOException {

        br.write(getCurrentTabSpace() + "<AnalysisData>" + System.getProperty("line.separator"));
        tabCounter++;

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationList id=\"SII_LIST_1\" xmlns=" + mzIdentMLXsd + ">" + System.getProperty("line.separator"));
        tabCounter++;

        writeFragmentationTable();

        // iterate the psms
        //for (all psms) {
        writeSpectrumIdentificationResult();
        //}

        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationList>" + System.getProperty("line.separator"));

        writeProteinDetectionList();

        tabCounter--;
        br.write(getCurrentTabSpace() + "</AnalysisData>" + System.getProperty("line.separator"));
    }

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

    private void writeSpectrumIdentificationResult() throws IOException {

        br.write(getCurrentTabSpace() + "<SpectrumIdentificationResult spectraData_ref=\"SID_1\" spectrumID=\"index=12\" id=\"SIR_1\">" + System.getProperty("line.separator"));
        tabCounter++;

        // @TODO: add the psm data
//        <SpectrumIdentificationItem passThreshold="true" rank="1"
//                        peptide_ref="LCYIALDFDEEMKAAEDSSDIEK_15.9949@M$228;_57.0215@C$218;_"
//                        experimentalMassToCharge="2709.148" chargeState="3" id="SII_1_1">
//                        <PeptideEvidenceRef peptideEvidence_ref="PE1_2_0"/>
//                        <Fragmentation>
//                            <IonType charge="3" index="7 21">
//                                <FragmentArray measure_ref="Measure_MZ" values="274.303 802.321"/>
//                                <FragmentArray measure_ref="Measure_Int" values="2.0 6.0"/>
//                                <FragmentArray measure_ref="Measure_Error"
//                                    values="-0.176605 -0.035175"/>
//                                <cvParam accession="MS:1001229" cvRef="PSI-MS" name="frag: a ion"/>
//                            </IonType>
//                            ...
//                                
//                        </Fragmentation>
//                        <cvParam accession="MS:1001330" cvRef="PSI-MS" value="1.7E-4" name="xtandem:expect"/>
//                        <cvParam accession="MS:1001331" cvRef="PSI-MS" value="79.6" name="xtandem:hyperscore"/>
//                    </SpectrumIdentificationItem>
//                    <cvParam accession="MS:1000796" cvRef="PSI-MS" value="55.1074.1074.3.dta" name="spectrum title"/>
        tabCounter--;
        br.write(getCurrentTabSpace() + "</SpectrumIdentificationResult>" + System.getProperty("line.separator"));
    }

    private void writeFragmentationTable() {
        // @TODO: add the fragment ion table
//                <FragmentationTable>
//                    <Measure id="Measure_MZ">
//                        <cvParam accession="MS:1001225" cvRef="PSI-MS" unitCvRef="PSI-MS"
//                            unitName="m/z" unitAccession="MS:1000040" name="product ion m/z"/>
//                    </Measure>
//                    <Measure id="Measure_Int">
//                        <cvParam accession="MS:1001226" cvRef="PSI-MS" name="product ion intensity"
//                        />
//                    </Measure>
//                    <Measure id="Measure_Error">
//                        <cvParam accession="MS:1001227" cvRef="PSI-MS" unitCvRef="PSI-MS"
//                            unitName="m/z" unitAccession="MS:1000040" name="product ion m/z error"/>
//                    </Measure>
//                </FragmentationTable>
    }

    private void writeInputFileDetails() {
        // @TODO: add the file details
//        <Inputs xmlns="http://psidev.info/psi/pi/mzIdentML/1.1">
//            <SourceFile location="build/classes/resources/55merge_tandem.xml" id="SourceFile_1">
//                <FileFormat>
//                    <cvParam accession="MS:1001401" cvRef="PSI-MS" name="xtandem xml file"/>
//                </FileFormat>
//            </SourceFile>
//            <SearchDatabase numDatabaseSequences="22348" location="no description" id="SearchDB_1">
//                <FileFormat>
//                    <cvParam accession="MS:1001348" cvRef="PSI-MS" name="FASTA format"/>
//                </FileFormat>
//                <DatabaseName>
//                    <userParam name="no description"/>
//                </DatabaseName>
//            </SearchDatabase>
//            <SpectraData location="D:/TestSpace/NeoTestMarch2011/55merge.mgf" id="SID_1">
//                <FileFormat>
//                    <cvParam accession="MS:1001062" cvRef="PSI-MS" name="Mascot MGF file"/>
//                </FileFormat>
//                <SpectrumIDFormat>
//                    <cvParam accession="MS:1000774" cvRef="PSI-MS"
//                        name="multiple peak list nativeID format"/>
//                </SpectrumIDFormat>
//            </SpectraData>
//        </Inputs>
    }

    /////////////////////////////////////////////
    // the code below this point is pride code
    /////////////////////////////////////////////
    /**
     * Writes all PSMs.
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

            progressDialog.setTitle("Creating mzIdentML File. Please Wait...  (Part 2 of 2: Exporting IDs)");
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
            String searchEngineReport = SpectrumIdentificationAlgorithm.getName(seList.get(0));

            for (int i = 1; i < seList.size(); i++) {

                if (i == seList.size() - 1) {
                    searchEngineReport += " and ";
                } else {
                    searchEngineReport += ", ";
                }

                searchEngineReport += SpectrumIdentificationAlgorithm.getName(seList.get(i));
            }

            searchEngineReport += " post-processed by PeptideShaker v" + peptideShakerGUI.getVersion();

            for (String spectrumFile : identification.getSpectrumFiles()) {
                identification.loadSpectrumMatches(spectrumFile, null);
            }
            identification.loadPeptideMatches(null);
            identification.loadProteinMatches(null);
            for (String spectrumFile : identification.getSpectrumFiles()) {
                identification.loadSpectrumMatchParameters(spectrumFile, psmProbabilities, null);
            }
            identification.loadPeptideMatchParameters(peptideProbabilities, null);
            identification.loadProteinMatchParameters(proteinProbabilities, null);

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

                identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null); // @TODO: should use the progress dialog here, but this messes up the overall progress bar...
                identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), peptideProbabilities, null);

                for (String peptideKey : proteinMatch.getPeptideMatches()) {

                    if (prideExportDialog.progressCancelled()) {
                        break;
                    }

                    PeptideMatch currentMatch = identification.getPeptideMatch(peptideKey);
                    peptideProbabilities = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, peptideProbabilities);

                    identification.loadSpectrumMatches(currentMatch.getSpectrumMatches(), null); // @TODO: should use the progress dialog here, but this messes up the overall progress bar...
                    identification.loadSpectrumMatchParameters(currentMatch.getSpectrumMatches(), psmProbabilities, null);

                    for (String spectrumKey : currentMatch.getSpectrumMatches()) {

                        if (prideExportDialog.progressCancelled()) {
                            break;
                        }

                        psmProbabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psmProbabilities);
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        PeptideAssumption bestAssumption = spectrumMatch.getBestPeptideAssumption();
                        Peptide tempPeptide = bestAssumption.getPeptide();

                        // the peptide
                        br.write(getCurrentTabSpace() + "<PeptideItem>" + System.getProperty("line.separator"));
                        tabCounter++;

                        // peptide sequence
                        br.write(getCurrentTabSpace() + "<Sequence>" + tempPeptide.getSequence() + "</Sequence>" + System.getProperty("line.separator"));

                        // peptide start and end
                        String proteinAccession = proteinMatch.getMainMatch();
                        String proteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                        int peptideStart = proteinSequence.lastIndexOf(tempPeptide.getSequence()) + 1; // @TODO: lastIndexOf should be avoided!!
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
                                for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(bestAssumption.getPeptide(), PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy())) {
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

                        if (peptideShakerGUI.getPtmScoringPreferences().isProbabilitsticScoreCalculation()) {

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
                                        ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                                        Collections.sort(sites);
                                        for (int site : sites) {
                                            if (firstSite) {
                                                firstSite = false;
                                            } else {
                                                probabilisticScore.append(", ");
                                            }
                                            probabilisticScore.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                                        }
                                    } else {
                                        probabilisticScore.append("Not Scored");
                                    }

                                    probabilisticScore.append(")");
                                }
                            }
                        }

                        ArrayList<String> peptideParentProteins = tempPeptide.getParentProteins(PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
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
                        confidenceThreshold = peptideTargetDecoyMap.getTargetDecoyMap(peptideTargetDecoyMap.getCorrectedKey(peptideProbabilities.getSpecificMapKey())).getTargetDecoyResults().getConfidenceLimit();
                        br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        if (peptideProbabilities.getMatchValidationLevel() == MatchValidationLevel.doubtful && !peptideProbabilities.getReasonDoubtful().equals("")) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Validation\" value=\"" + peptideProbabilities.getMatchValidationLevel() + " (" + peptideProbabilities.getReasonDoubtful() + ")" + "\" />" + System.getProperty("line.separator"));
                        } else {
                            br.write(getCurrentTabSpace() + "<userParam name=\"Peptide Validation\" value=\"" + peptideProbabilities.getMatchValidationLevel() + "\" />" + System.getProperty("line.separator"));
                        }
                        br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence\" value=\"" + Util.roundDouble(psmProbabilities.getPsmConfidence(), CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        confidenceThreshold = psmTargetDecoyMap.getTargetDecoyMap(psmTargetDecoyMap.getCorrectedKey(psmProbabilities.getSpecificMapKey())).getTargetDecoyResults().getConfidenceLimit();
                        br.write(getCurrentTabSpace() + "<userParam name=\"PSM Confidence Threshold\" value=\"" + Util.roundDouble(confidenceThreshold, CONFIDENCE_DECIMALS) + "\" />" + System.getProperty("line.separator"));
                        if (psmProbabilities.getMatchValidationLevel() == MatchValidationLevel.doubtful && !psmProbabilities.getReasonDoubtful().equals("")) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PSM Validation\" value=\"" + psmProbabilities.getMatchValidationLevel() + " (" + psmProbabilities.getReasonDoubtful() + ")" + "\" />" + System.getProperty("line.separator"));
                        } else {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PSM Validation\" value=\"" + psmProbabilities.getMatchValidationLevel() + "\" />" + System.getProperty("line.separator"));
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
                        if (dScore.length() > 0) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PTM D-score\" value=\"" + dScore + "\" />" + System.getProperty("line.separator"));
                        }
                        PTMScoringPreferences pTMScoringPreferences = peptideShakerGUI.getPtmScoringPreferences();
                        if (pTMScoringPreferences.isProbabilitsticScoreCalculation() && probabilisticScore.length() > 0) {
                            br.write(getCurrentTabSpace() + "<userParam name=\"PTM " + pTMScoringPreferences.getSelectedProbabilisticScore().getName() + "\" value=\"" + probabilisticScore + "\" />" + System.getProperty("line.separator"));
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
                    if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectrumCountingPreferences.SpectralCountingMethod.EMPAI) {
                        br.write(getCurrentTabSpace() + "<userParam name=\"emPAI\" value=\""
                                + peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey) + "\" />" + System.getProperty("line.separator"));
                    } else {
                        br.write(getCurrentTabSpace() + "<userParam name=\"NSAF+\" value=\""
                                + peptideShakerGUI.getIdentificationFeaturesGenerator().getSpectrumCounting(proteinKey) + "\" />" + System.getProperty("line.separator"));
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                if (proteinProbabilities.getMatchValidationLevel() == MatchValidationLevel.doubtful && !proteinProbabilities.getReasonDoubtful().equals("")) {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Protein Validation\" value=\"" + proteinProbabilities.getMatchValidationLevel() + " (" + proteinProbabilities.getReasonDoubtful() + ")" + "\" />" + System.getProperty("line.separator"));
                } else {
                    br.write(getCurrentTabSpace() + "<userParam name=\"Protein Validation\" value=\"" + proteinProbabilities.getMatchValidationLevel() + "\" />" + System.getProperty("line.separator"));
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
    private void writeFragmentIons(SpectrumMatch spectrumMatch) throws IOException, MzMLUnmarshallerException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
        PeptideSpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnorator();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        annotationPreferences.setCurrentSettings(spectrumMatch.getBestPeptideAssumption(), true, PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
        MSnSpectrum tempSpectrum = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumMatch.getKey()));

        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                annotationPreferences.getNeutralLosses(),
                annotationPreferences.getValidatedCharges(),
                spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value,
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

                // two extra tests to guard against problems with the cv terms, better to have a valid ptm than no ptm at all...
                if (cvTermName == null) {
                    cvTermName = modName;
                }
                if (ptmMass == null) {
                    ptmMass = "" + ptm.getMass();
                }
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
        br.write(getCurrentTabSpace() + "<cvLookup cvLabel=\"MS\" fullName=\"PSI Mass Spectrometry Ontology\" version=\"1.0.0\" "
                + "address=\"http://psidev.sourceforge.net/ontology\" />" + System.getProperty("line.separator"));

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

        progressDialog.setTitle("Creating mzIdentML File. Please Wait...  (Part 1 of 2: Exporting Spectra)");

        spectrumIndexes = new HashMap<String, Long>();

        long spectrumCounter = 0;

        br.write(getCurrentTabSpace() + "<spectrumList count=\"" + spectrumCounter + "\">" + System.getProperty("line.separator"));
        tabCounter++;

        progressDialog.setPrimaryProgressCounterIndeterminate(false);

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
                + "value=\"PeptideShaker v" + peptideShakerGUI.getVersion() + "\" />" + System.getProperty("line.separator"));

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
     * Writes the mzIdentML start tag.
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing a file
     */
    private void writeMzIdentMLStartTag() throws IOException {
        br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator"));
        br.write("MzIdentML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://www.psidev.info/sites/default/files/mzIdentML1.1.0.xsd\" "
                + "xmlns=\"http://psidev.info/psi/pi/mzIdentML/1.1\" version=\"1.1.0\">"
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
