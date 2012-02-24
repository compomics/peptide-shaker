package eu.isas.peptideshaker.pride;

import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.pride.util.BinaryArrayImpl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The class that takes care of converting the data to PRIDE XML.
 *
 * @author Harald Barsnes
 */
public class PRIDEExport {

    private PeptideShakerGUI peptideShakerGUI;
    private String experimentTitle;
    private String experimentLabel;
    private String experimentDescription;
    private String experimentProject;
    private ArrayList<Reference> references;
    private Contact contact;
    private Sample sample;
    private Protocol protocol;
    private Instrument instrument;
    private File outputFolder;
    private int tabCounter = 0;

    public PRIDEExport(PeptideShakerGUI peptideShakerGUI, String experimentTitle, String experimentLabel, String experimentDescription, String experimentProject,
            ArrayList<Reference> references, Contact contact, Sample sample, Protocol protocol, Instrument instrument,
            File outputFolder) {
        this.peptideShakerGUI = peptideShakerGUI;
        this.experimentTitle = experimentTitle;
        this.experimentLabel = experimentLabel;
        this.experimentDescription = experimentDescription;
        this.experimentProject = experimentProject;
        this.references = references;
        this.contact = contact;
        this.sample = sample;
        this.protocol = protocol;
        this.instrument = instrument;
        this.outputFolder = outputFolder;
    }

    public void createPrideXmlFile(ProgressDialogX progressDialog) {

        try {

            FileWriter r = new FileWriter(new File(outputFolder, experimentTitle + ".xml"));
            BufferedWriter br = new BufferedWriter(r);

            // the experiment start tag
            writeExperimentCollectionStartTag(br);

            // the experiment title
            writeTitle(br);

            // the references, if any
            if (references.size() > 0) {
                writeReferences(br);
            }

            // the short label
            writeShortLabel(br);

            // the protocol
            writeProtocol(br);

            // the mzData element
            writeMzData(br, progressDialog);

            // the additional tags
            writeAdditionalTags(br);

            // the experiment end tag
            writeExperimentCollectionEndTag(br);

            br.close();
            r.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
        }
    }

    private void writeMzData(BufferedWriter br, ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        br.write(getCurrentTabSpace() + "<mzData version=\"1.05\" accessionNumber=\"0\">\n");
        tabCounter++;

        // include the ontologies used, only MS is included by default
        br.write(getCurrentTabSpace() + "<cvLookup cvLabel=\"MS\" fullName=\"PSI Mass Spectrometry Ontology\" version=\"1.0.0\" address=\"http://psidev.sourceforge.net/ontology\" />\n");

        // write the mzData description (project description, sample details, contact details, instrument details and software details)
        writeMzDataDescription(br);

        // write the spectra
        writeSpectra(br, progressDialog);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</mzData>\n");
    }

    private void writeSpectra(BufferedWriter br, ProgressDialogX progressDialog) throws IOException, MzMLUnmarshallerException {

        progressDialog.setTitle("Exporting Spectra. Please Wait.");

        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        int spectrumCounter = 0;

        // get the spectrum count
        for (String mgfFile : spectrumFactory.getMgfFileNames()) {
            spectrumCounter += spectrumFactory.getNSpectra(mgfFile);
        }

        br.write(getCurrentTabSpace() + "<spectrumList count=\"" + spectrumCounter + "\">\n");
        tabCounter++;

        int maxSpectrumCounter = spectrumCounter; // @TODO: debug variable. remove when testing it finished

        spectrumCounter = 1;

        progressDialog.setIndeterminate(false);
        progressDialog.setMax(maxSpectrumCounter);

        for (String mgfFile : spectrumFactory.getMgfFileNames()) {
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(mgfFile)) {
                String spectrumKey = Spectrum.getSpectrumKey(mgfFile, spectrumTitle);
                writeSpectrum(br, ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey)), spectrumCounter++);

                progressDialog.incrementValue();

                if (spectrumCounter > maxSpectrumCounter) {
                    break;
                }
            }

            if (spectrumCounter > maxSpectrumCounter) {
                break;
            }
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumList>\n");
    }

    private void writeSpectrum(BufferedWriter br, MSnSpectrum spectrum, int spectrumCounter) throws IOException {

        br.write(getCurrentTabSpace() + "<spectrum id=\"" + spectrumCounter + "\">\n");
        tabCounter++;

        br.write(getCurrentTabSpace() + "<spectrumDesc>\n");
        tabCounter++;

        br.write(getCurrentTabSpace() + "<spectrumSettings>\n");
        tabCounter++;
        br.write(getCurrentTabSpace() + "<spectrumInstrument mzRangeStop=\"" + spectrum.getMaxMz()
                + " \" mzRangeStart=\"" + spectrum.getMinMz()
                + "\" msLevel=\"2\" />\n"); // @TODO: note that ms level is hardcoded to 2
        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumSettings>\n");

        br.write(getCurrentTabSpace() + "<precursorList count=\"1\">\n"); // @TODO: note that precursor count is hardcoded to 1
        tabCounter++;
        br.write(getCurrentTabSpace() + "<precursor msLevel=\"1\" spectrumRef=\"0\">\n"); // @TODO: note that precursor ms level is hardcoded to 1 with no corresponding spectrum
        tabCounter++;
        br.write(getCurrentTabSpace() + "<ionSelection>\n");
        tabCounter++;

        // precursor charge states
        for (int i = 0; i < spectrum.getPrecursor().getPossibleCharges().size(); i++) {
            br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000041\" name=\"ChargeState\" value=\""
                    + spectrum.getPrecursor().getPossibleCharges().get(i).value + "\" />");
            // @TODO: note that charge is assumed to be positive...
        }

        // precursor m/z value
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1000744\" name=\"selected ion m/z\" value=\""
                + spectrum.getPrecursor().getMz() + "\" />");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</ionSelection>\n");

        // activation
        br.write(getCurrentTabSpace() + "<activation />\n"); // @TODO: always empty, but i think it's a required field

        tabCounter--;
        br.write(getCurrentTabSpace() + "</precursor>\n");
        tabCounter--;
        br.write(getCurrentTabSpace() + "</precursorList>\n");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrumDesc>\n");


        // get the m/z and intensity arrays
        double[][] arrays = spectrum.getMzAndIntensityAsArray();

        // write the m/z values
        br.write(getCurrentTabSpace() + "<mzArrayBinary>\n");
        tabCounter++;
        BinaryArrayImpl mzValues = new BinaryArrayImpl(arrays[0], BinaryArrayImpl.LITTLE_ENDIAN_LABEL);
        br.write("<data precision=\"" + mzValues.getDataPrecision() + "\" endian=\"" + mzValues.getDataEndian()
                + "\" length=\"" + mzValues.getDataLength() + "\">" + mzValues.getBase64String() + "</data>\n");
        tabCounter--;
        br.write(getCurrentTabSpace() + "</mzArrayBinary>\n");

        // write the intensity values
        br.write(getCurrentTabSpace() + "<intenArrayBinary>\n");
        tabCounter++;
        BinaryArrayImpl intValues = new BinaryArrayImpl(arrays[1], BinaryArrayImpl.LITTLE_ENDIAN_LABEL);
        br.write("<data precision=\"" + intValues.getDataPrecision() + "\" endian=\"" + intValues.getDataEndian()
                + "\" length=\"" + intValues.getDataLength() + "\">" + intValues.getBase64String() + "</data>\n");
        tabCounter--;
        br.write(getCurrentTabSpace() + "</intenArrayBinary>\n");


        tabCounter--;
        br.write(getCurrentTabSpace() + "</spectrum>\n");
    }

    private void writeMzDataDescription(BufferedWriter br) throws IOException {

        // write the project description
        br.write(getCurrentTabSpace() + "<description>\n");
        tabCounter++;

        br.write(getCurrentTabSpace() + "<admin>\n");
        tabCounter++;

        // write the sample details
        writeSample(br);

        // write the contact details
        writeContact(br);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</admin>\n");

        // write the instrument details
        writeInstrument(br);

        // write the software details
        writeSoftware(br);

        tabCounter--;
        br.write(getCurrentTabSpace() + "</description>\n");
    }

    private void writeSoftware(BufferedWriter br) throws IOException {

        br.write(getCurrentTabSpace() + "<dataProcessing>\n");
        tabCounter++;

        // @TODO: verify the software settings!! should this be peak list generation software??

        // write the software details
        br.write(getCurrentTabSpace() + "<software>\n"); // @TODO: add more software details? SearchGUI? Search engines used etc?
        tabCounter++;
        br.write(getCurrentTabSpace() + "<name>" + "PeptideShaker" + "</name>\n");
        br.write(getCurrentTabSpace() + "<version>" + peptideShakerGUI.getVersion() + "</version>\n");
        tabCounter--;
        br.write(getCurrentTabSpace() + "</software>\n");

        // write the processing details
        br.write(getCurrentTabSpace() + "<processingMethod>\n");
        tabCounter++;

        // @TODO: verify the use of settings below!!

        // fragment mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000161\" name=\"Fragment mass tolerance setting\" value=\""
                + peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy() + "\" />\n");

        // precursor mass accuracy
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000078\" name=\"Peptide mass tolerance setting\" value=\""
                + peptideShakerGUI.getSearchParameters().getPrecursorAccuracy() + "\" />\n");

        // allowed missed cleavages
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000162\" name=\"Allowed missed cleavages\" value=\""
                + peptideShakerGUI.getSearchParameters().getnMissedCleavages() + "\" />\n");

        // @TODO: add more settings??

        tabCounter--;
        br.write(getCurrentTabSpace() + "</processingMethod>\n");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</dataProcessing>\n");
    }

    private void writeInstrument(BufferedWriter br) throws IOException {

        br.write(getCurrentTabSpace() + "<instrument>\n");
        tabCounter++;

        // write the instrument name
        br.write(getCurrentTabSpace() + "<instrumentName>" + instrument.getName() + "</instrumentName>\n");

        // write the source
        br.write(getCurrentTabSpace() + "<source>\n");
        tabCounter++;
        writeCvTerm(br, instrument.getSource());
        tabCounter--;
        br.write(getCurrentTabSpace() + "</source>\n");

        // write the analyzers
        br.write(getCurrentTabSpace() + "<analyzerList count=\"" + instrument.getCvTerms().size() + "\">\n");
        tabCounter++;

        for (int i = 0; i < instrument.getCvTerms().size(); i++) {
            br.write(getCurrentTabSpace() + "<analyzer>\n");
            tabCounter++;
            writeCvTerm(br, instrument.getCvTerms().get(i));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</analyzer>\n");
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</analyzerList>\n");


        // write the detector
        br.write(getCurrentTabSpace() + "<detector>\n");
        tabCounter++;
        writeCvTerm(br, instrument.getDetector());
        tabCounter--;
        br.write(getCurrentTabSpace() + "</detector>\n");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</instrument>\n");
    }

    private void writeContact(BufferedWriter br) throws IOException {

        br.write(getCurrentTabSpace() + "<contact>\n");
        tabCounter++;

        br.write(getCurrentTabSpace() + "<name>" + contact.getName() + "</name>\n");
        br.write(getCurrentTabSpace() + "<institution>" + contact.getInstitution() + "</institution>\n");
        br.write(getCurrentTabSpace() + "<contactInfo>" + contact.getEMail() + "</contactInfo>\n");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</contact>\n");
    }

    private void writeSample(BufferedWriter br) throws IOException {

        br.write(getCurrentTabSpace() + "<sampleName>" + sample.getName() + "</sampleName>\n");

        br.write(getCurrentTabSpace() + "<sampleDescription>\n");
        tabCounter++;

        for (int i = 0; i < sample.getCvTerms().size(); i++) {
            writeCvTerm(br, sample.getCvTerms().get(i));
        }

        tabCounter--;
        br.write(getCurrentTabSpace() + "</sampleDescription>\n");
    }

    private void writeAdditionalTags(BufferedWriter br) throws IOException {
        br.write(getCurrentTabSpace() + "<additional>\n");
        tabCounter++;

        // XML generation software
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000175\" name=\"XML generation software\" value=\"PeptideShaker v" + peptideShakerGUI.getVersion() + "\" />\n");

        // Original MS data file format
        //br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000218\" name=\"Original MS data file format\" value=\"" + ?? + "\" />\n");  // @TODO: could/should this be added?

        // Project
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000097\" name=\"Project\" value=\"" + experimentProject + "\" />\n");

        // Experiment description
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000040\" name=\"Experiment description\" value=\"" + experimentDescription + "\" />\n");

        // Global peptide FDR
        //br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1001364\" name=\"pep:global FDR\" value=\"" + peptideShakerGUI. + "\" />\n");  // @TODO: add global peptide FDR?

        // @TODO: add global protein FDR??

        // search type
        br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"MS\" accession=\"MS:1001083\" name=\"ms/ms search\" />\n");

        // @TODO: add more??

        tabCounter--;
        br.write(getCurrentTabSpace() + "</additional>\n");
    }

    private void writeTitle(BufferedWriter br) throws IOException {
        br.write(getCurrentTabSpace() + "<Title>" + experimentTitle + "</Title>\n");
    }

    private void writeShortLabel(BufferedWriter br) throws IOException {
        br.write(getCurrentTabSpace() + "<ShortLabel>" + experimentLabel + "</ShortLabel>\n");
    }

    private void writeProtocol(BufferedWriter br) throws IOException {
        br.write(getCurrentTabSpace() + "<Protocol>\n");
        tabCounter++;

        br.write(getCurrentTabSpace() + "<ProtocolName>" + protocol.getName() + "</ProtocolName>\n");
        br.write(getCurrentTabSpace() + "<ProtocolSteps>\n");

        for (int i = 0; i < protocol.getCvTerms().size(); i++) {

            tabCounter++;

            br.write(getCurrentTabSpace() + "<StepDescription>\n");
            tabCounter++;
            writeCvTerm(br, protocol.getCvTerms().get(i));
            tabCounter--;
            br.write(getCurrentTabSpace() + "</StepDescription>\n");

            tabCounter--;
        }

        br.write(getCurrentTabSpace() + "</ProtocolSteps>\n");

        tabCounter--;
        br.write(getCurrentTabSpace() + "</Protocol>\n");
    }

    private void writeReferences(BufferedWriter br) throws IOException {
        for (int i = 0; i < references.size(); i++) {

            Reference tempReference = references.get(i);

            br.write(getCurrentTabSpace() + "<Reference>\n");
            tabCounter++;

            br.write(getCurrentTabSpace() + "<RefLine>" + tempReference.getReference() + "</RefLine>\n");

            if (tempReference.getPmid() != null || tempReference.getDoi() != null) {
                br.write(getCurrentTabSpace() + "<additional>\n");
                tabCounter++;

                if (tempReference.getPmid() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000029\" name=\"PubMed\" value=\"" + tempReference.getPmid() + "\" />\n");
                }

                if (tempReference.getDoi() != null) {
                    br.write(getCurrentTabSpace() + "<cvParam cvLabel=\"PRIDE\" accession=\"PRIDE:0000042\" name=\"DOI\" value=\"" + tempReference.getDoi() + "\" />\n");
                }

                tabCounter--;
                br.write(getCurrentTabSpace() + "</additional>\n");
            }

            tabCounter--;
            br.write(getCurrentTabSpace() + "</Reference>\n");
        }
    }

    private void writeExperimentCollectionStartTag(BufferedWriter br) throws IOException {
        br.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n");
        br.write("<ExperimentCollection version=\"2.1\">\n");
        tabCounter++;
        br.write(getCurrentTabSpace() + "<Experiment>\n");
        tabCounter++;
    }

    private void writeExperimentCollectionEndTag(BufferedWriter br) throws IOException {
        tabCounter--;
        br.write(getCurrentTabSpace() + "</Experiment>\n");
        tabCounter--;
        br.write("</ExperimentCollection>");
    }

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
     * @return the outputFolder
     */
    public File getOutputFolder() {
        return outputFolder;
    }

    /**
     * @param outputFolder the outputFolder to set
     */
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    private void writeCvTerm(BufferedWriter br, CvTerm cvTerm) throws IOException {

        br.write(getCurrentTabSpace() + "<cvParam "
                + "cvLabel=\"" + cvTerm.getOntology() + "\" "
                + "accession=\"" + cvTerm.getAccession() + "\" "
                + "name=\"" + cvTerm.getName() + "\"");

        if (cvTerm.getValue() != null) {
            br.write(" value=\"" + cvTerm.getValue() + "\" />\n");
        } else {
            br.write(" />\n");
        }
    }
}
