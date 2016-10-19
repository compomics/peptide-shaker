package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.Atom;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.io.export.xml.SimpleXmlWriter;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringEscapeUtils;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Export to PepXML.
 *
 * @author Marc Vaudel
 */
public class PepXmlExport {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Constructor
     */
    public PepXmlExport() {

    }

    /**
     * Writes the PSM results to the given file in the PepXML format.
     *
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param destinationFile the file where to write
     * @param peptideShakerVersion the PeptideShaker version
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     * @param exceptionHandler a handler for exceptions
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    public void writePepXmlFile(Identification identification, IdentificationParameters identificationParameters, File destinationFile, String peptideShakerVersion, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Peptide to Protein Mapping. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        sequenceFactory.getDefaultPeptideMapper(identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getSearchParameters().getPtmSettings(), identificationParameters.getPeptideVariantsPreferences(), waitingHandler, exceptionHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting PSMs. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }
        SimpleXmlWriter sw = new SimpleXmlWriter(new BufferedWriter(new FileWriter(destinationFile)));
        writeHeader(sw);
        writeMsmsPipelineAnalysis(sw, peptideShakerVersion, destinationFile, identification, identificationParameters, waitingHandler);
        sw.close();
    }

    /**
     * Writes the header
     *
     * @param sw the xml file writer
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while writing to the file
     */
    private void writeHeader(SimpleXmlWriter sw) throws IOException {
        sw.writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    /**
     * Writes the msms pipeline analysis block
     *
     * @param sw the xml file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param destinationFile the file where to write
     * @param peptideShakerVersion the PeptideShaker version
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    private void writeMsmsPipelineAnalysis(SimpleXmlWriter sw, String peptideShakerVersion, File destinationFile, Identification identification, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        sw.writeLine("<msms_pipeline_analysis xmlns=\"http://regis-web.systemsbiology.net/pepXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://sashimi.sourceforge.net/schema_revision/pepXML/pepXML_v117.xsd\" summary_xml=\"" + destinationFile.getAbsolutePath() + "\">");

        writeAnalysisSummary(sw, peptideShakerVersion);
        writeMsmsRunSummary(sw, identification, identificationParameters, waitingHandler);

        sw.writeLineDecreasedIndent("</msms_pipeline_analysis>");
    }

    /**
     * Writes the analysis summary section.
     *
     * @param sw the xml file writer
     * @param peptideShakerVersion the PeptideShaker version
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while writing to the file
     */
    private void writeAnalysisSummary(SimpleXmlWriter sw, String peptideShakerVersion) throws IOException {

        sw.writeLineIncreasedIndent("<analysis_summary>");

        sw.writeLineIncreasedIndent("<analysis>PeptideShaker</analysis>");
        sw.writeIndent();
        sw.write("<version>");
        sw.write(peptideShakerVersion);
        sw.write("</version>");
        sw.newLine();

        sw.writeLineDecreasedIndent("</analysis_summary>");
    }

    /**
     * Writes the msms run summary section.
     *
     * @param sw the xml file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    private void writeMsmsRunSummary(SimpleXmlWriter sw, Identification identification, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            // reset the progress bar
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        }

        for (String spectrumFileName : identification.getOrderedSpectrumFileNames()) {

            StringBuilder runStart = new StringBuilder();
            runStart.append("<msms_run_summary");
            File spectrumFile = spectrumFactory.getSpectrumFileFromIdName(spectrumFileName);
            if (spectrumFile != null) {
                String path = spectrumFile.getAbsolutePath();
                String baseName = Util.removeExtension(path);
                String extension = Util.getExtension(spectrumFile);
                runStart.append(" base_name=\"").append(baseName).append("\" ");
                runStart.append("raw_data_type=\"").append(extension).append("\" ");
                runStart.append("raw_data=\"").append(extension).append("\"");
            }
            runStart.append(">");

            sw.writeLine(runStart.toString());

            DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
            if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
                for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                    writeEnzyme(sw, enzyme);
                }
            }
            writeSearchSummary(sw, identificationParameters);
            writeSpectrumQueries(sw, identification, identificationParameters, spectrumFileName, waitingHandler);

            sw.writeLineDecreasedIndent("</msms_run_summary>");
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                break;
            }
        }
    }

    /**
     * Writes the enzyme section.
     *
     * @param sw the xml file writer
     * @param enzyme the enzyme
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeEnzyme(SimpleXmlWriter sw, Enzyme enzyme) throws IOException {

        String specificity;
        if (enzyme.isSemiSpecific()) {
            specificity = "semispecific";
        } else if (enzyme.isUnspecific()) {
            specificity = "nonspecific";
        } else {
            specificity = "specific";
        }

        sw.writeLineIncreasedIndent("<sample_enzyme name=\"" + enzyme.getName() + "\" fidelity=\"" + specificity + "\">");

        sw.increaseIndent();
        ArrayList<Character> aaBefore = enzyme.getAminoAcidBefore();
        if (aaBefore != null && !aaBefore.isEmpty()) {
            StringBuilder cut = new StringBuilder(aaBefore.size());
            for (Character aa : aaBefore) {
                cut.append(aa);
            }

            StringBuilder noCut = new StringBuilder(1);
            ArrayList<Character> restrictionAfter = enzyme.getRestrictionAfter();
            if (restrictionAfter != null && !restrictionAfter.isEmpty()) {
                for (Character aa : restrictionAfter) {
                    noCut.append(aa);
                }
            }
            sw.writeLine("<specificity cut=\"" + cut + "\" no_cut=\"" + noCut + "\" sense=\"C\">");
        }
        ArrayList<Character> aaAfter = enzyme.getAminoAcidAfter();
        if (aaAfter != null && !aaAfter.isEmpty()) {
            StringBuilder cut = new StringBuilder(aaAfter.size());
            for (Character aa : aaAfter) {
                cut.append(aa);
            }

            StringBuilder noCut = new StringBuilder(1);
            ArrayList<Character> restrictionBefore = enzyme.getRestrictionBefore();
            if (restrictionBefore != null && !restrictionBefore.isEmpty()) {
                for (Character aa : restrictionBefore) {
                    noCut.append(aa);
                }
            }
            sw.writeLine("<specificity cut=\"" + cut + "\" no_cut=\"" + noCut + "\" sense=\"N\">");
        }
        sw.writeLineDecreasedIndent("</sample_enzyme>");
    }

    /**
     * Writes the search summary section.
     *
     * @param sw the xml file writer
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeSearchSummary(SimpleXmlWriter sw, IdentificationParameters identificationParameters) throws IOException {
        sw.writeLine("<search_summary precursor_mass_type=\"monoisotopic\" fragment_mass_type=\"monoisotopic\">");
        sw.increaseIndent();
        for (String ptmName : identificationParameters.getSearchParameters().getPtmSettings().getFixedModifications()) {
            PTM ptm = ptmFactory.getPTM(ptmName);
            sw.writeLine(getPtmLine(ptm, false));
        }
        for (String ptmName : identificationParameters.getSearchParameters().getPtmSettings().getAllNotFixedModifications()) {
            PTM ptm = ptmFactory.getPTM(ptmName);
            sw.writeLine(getPtmLine(ptm, true));
        }
        sw.writeLineDecreasedIndent("</search_summary>");
    }

    /**
     * Gets the line for a PTM.
     *
     * @param ptm the ptm
     * @param variable a boolean indicating whether the modification is variable
     *
     * @return the line for a PTM
     */
    private String getPtmLine(PTM ptm, boolean variable) {

        StringBuilder modificationLine = new StringBuilder();
        int ptmType = ptm.getType();
        if (ptmType == PTM.MODAA || ptmType == PTM.MODCAA || ptmType == PTM.MODCPAA || ptmType == PTM.MODNAA || ptmType == PTM.MODNPAA) {
            modificationLine.append("<aminoacid_modification ");
            AminoAcidPattern aminoAcidPattern = ptm.getPattern();
            modificationLine.append("aminoacid=\"").append(aminoAcidPattern.asSequence()).append("\" ");
            modificationLine.append("massdiff=\"").append(ptm.getMass()).append("\" ");
            if (aminoAcidPattern.getAminoAcidsAtTarget().size() == 1) {
                Character aa = aminoAcidPattern.getAminoAcidsAtTarget().get(0);
                AminoAcid aminoAcid = AminoAcid.getAminoAcid(aa);
                if (!aminoAcid.iscombination()) {
                    Double mass = aminoAcid.getMonoisotopicMass() + ptm.getMass();
                    modificationLine.append("mass=\"").append(mass).append("\" ");
                }
            }
            if (ptm.isCTerm()) {
                modificationLine.append("peptide_terminus=\"c\" ");
            }
            if (ptm.isNTerm()) {
                modificationLine.append("peptide_terminus=\"n\" ");
            }
        } else {
            modificationLine.append("<terminal_modification ");
            if (ptm.isCTerm()) {
                modificationLine.append("terminus=\"c\" ");
            }
            if (ptm.isNTerm()) {
                modificationLine.append("terminus=\"n\" ");
            }
            modificationLine.append("massdiff=\"").append(ptm.getMass()).append("\" ");
            if (ptmType == PTM.MODC) {
                modificationLine.append("protein_terminus=\"c\" ");
            }
            if (ptmType == PTM.MODN) {
                modificationLine.append("protein_terminus=\"n\" ");
            }
            if (ptm.isNTerm()) {
                Double mass = Atom.H.getMonoisotopicMass() + ptm.getMass();
                modificationLine.append("mass=\"").append(mass).append("\" ");
            }
            if (ptm.isCTerm()) {
                Double mass = Atom.H.getMonoisotopicMass() + Atom.O.getMonoisotopicMass() + ptm.getMass();
                modificationLine.append("mass=\"").append(mass).append("\" ");
            }
        }
        if (variable) {
            modificationLine.append("variable=\"Y\" ");
        } else {
            modificationLine.append("variable=\"N\" ");
        }
        modificationLine.append("symbol=\"").append(ptm.getName()).append("\" ");
        CvTerm cvTerm = ptm.getCvTerm();
        if (cvTerm != null) {
            modificationLine.append("description=\"").append(cvTerm.getAccession()).append("\"");
        }
        modificationLine.append(">");
        return modificationLine.toString();
    }

    /**
     * Writes the spectrum queries.
     *
     * @param sw the xml file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    private void writeSpectrumQueries(SimpleXmlWriter sw, Identification identification, IdentificationParameters identificationParameters, String spectrumFile, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, parameters, true, null);

        while (psmIterator.hasNext()) {
            SpectrumMatch spectrumMatch = psmIterator.next();
            String spectrumKey = spectrumMatch.getKey();
            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
            StringBuilder spectrumQueryStart = new StringBuilder();
            spectrumQueryStart.append("<spectrum_query unique_search_id=\"").append(StringEscapeUtils.escapeHtml4(spectrumTitle)).append("\">");
            sw.writeLine(spectrumQueryStart.toString());
            writeSpectrumMatch(sw, identification, identificationParameters, spectrumMatch);
            sw.writeLineDecreasedIndent("</spectrum_query>");
            if (waitingHandler != null) {
                waitingHandler.increaseSecondaryProgressCounter();
                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }
        }

    }

    /**
     * Writes the content of a spectrum match.
     *
     * @param sw the xml file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    private void writeSpectrumMatch(SimpleXmlWriter sw, Identification identification, IdentificationParameters identificationParameters, SpectrumMatch spectrumMatch) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        String spectrumKey = spectrumMatch.getKey();
        Double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        PSParameter psParameter = new PSParameter();

        sw.writeLineIncreasedIndent("<search_result>");
        sw.increaseIndent();

        // PeptideShaker hit
        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
        if (peptideAssumption != null) {
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
            writeSearchHit(sw, identificationParameters, peptideAssumption, precursorMz, psParameter, true);
        }

        // Search engines results
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumMatch.getKey());
        for (HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> scoreMap : assumptions.values()) {
            for (ArrayList<SpectrumIdentificationAssumption> spectrumIdentificationAssumptions : scoreMap.values()) {
                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : spectrumIdentificationAssumptions) {
                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                        writeSearchHit(sw, identificationParameters, peptideAssumption, precursorMz, psParameter, false);
                    }
                }
            }
        }

        sw.writeLineDecreasedIndent("</search_result>");
    }

    /**
     * Writes a search hit section.
     *
     * @param sw the xml file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     * @throws SQLException exception thrown whenever an error is encountered
     * while interacting with the back-end database
     * @throws ClassNotFoundException exception thrown whenever an error is
     * encountered while deserializing an object
     * @throws InterruptedException exception thrown whenever an threading error
     * is encountered
     * @throws MzMLUnmarshallerException exception thrown whenever an error is
     * encountered while reading an mzML file
     */
    private void writeSearchHit(SimpleXmlWriter sw, IdentificationParameters identificationParameters, PeptideAssumption peptideAssumption, Double precursorMz, PSParameter psParameter, boolean mainHit) throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        Peptide peptide = peptideAssumption.getPeptide();

        StringBuilder searchHitStart = new StringBuilder();
        searchHitStart.append("<search_hit hit_rank=\"").append(peptideAssumption.getRank()).append("\" ");
        searchHitStart.append("peptide=\"").append(peptide.getSequence()).append("\" ");
        StringBuilder proteins = new StringBuilder();
        ArrayList<String> proteinAccessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
        for (String accession : proteinAccessions) {
            if (proteins.length() > 0) {
                proteins.append(", ");
            }
            proteins.append(accession);
        }
        searchHitStart.append("protein=\"").append(proteins).append("\" ");
        searchHitStart.append("num_tot_proteins=\"").append(proteinAccessions.size()).append("\" ");
        searchHitStart.append("calc_neutral_pep_mass=\"").append(peptideAssumption.getTheoreticMass()).append("\" ");
        searchHitStart.append("massdiff=\"").append(peptideAssumption.getDeltaMass(precursorMz, false, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection())).append("\">");

        sw.writeLine(searchHitStart.toString());

        sw.increaseIndent();
        if (proteinAccessions.size() > 1) {
            for (String accession : proteinAccessions) {
                StringBuilder proteinLine = new StringBuilder();
                proteinLine.append("<alternative_protein protein=\"").append(accession).append("\">");
                sw.writeLine(proteinLine.toString());
            }
        }
        ArrayList<ModificationMatch> modificationMatches = peptide.getModificationMatches();
        if (modificationMatches != null && !modificationMatches.isEmpty()) {
            HashMap<Integer, Double> modifiedAminoAcids = new HashMap<Integer, Double>(modificationMatches.size());
            Double nTermMass = null;
            Double cTermMass = null;
            for (ModificationMatch modificationMatch : modificationMatches) {
                PTM ptm = ptmFactory.getPTM(modificationMatch.getTheoreticPtm());
                if (ptm.getType() == PTM.MODN || ptm.getType() == PTM.MODNP) {
                    if (nTermMass == null) {
                        nTermMass = Atom.H.getMonoisotopicMass();
                    }
                    nTermMass += ptm.getMass();
                } else if (ptm.getType() == PTM.MODC || ptm.getType() == PTM.MODCP) {
                    if (cTermMass == null) {
                        cTermMass = Atom.H.getMonoisotopicMass() + Atom.O.getMonoisotopicMass();
                    }
                    cTermMass += ptm.getMass();
                } else {
                    Integer site = modificationMatch.getModificationSite();
                    Double modMass = modifiedAminoAcids.get(site);
                    if (modMass == null) {
                        modMass = 0.0;
                    }
                    modMass += ptm.getMass();
                    modifiedAminoAcids.put(site, modMass);
                }
            }
            StringBuilder modificationStart = new StringBuilder();
            modificationStart.append("<modification_info");
            if (nTermMass != null) {
                modificationStart.append(" mod_nterm_mass=\"").append(nTermMass).append("\"");
            }
            if (cTermMass != null) {
                modificationStart.append(" mod_cterm_mass=\"").append(nTermMass).append("\"");
            }
            modificationStart.append(">");
            sw.writeLine(modificationStart.toString());
            if (!modifiedAminoAcids.isEmpty()) {
                sw.increaseIndent();
                for (Integer site : modifiedAminoAcids.keySet()) {
                    Double modifiedMass = modifiedAminoAcids.get(site);
                    StringBuilder modificationSite = new StringBuilder();
                    modificationSite.append("<mod_aminoacid_mass position=\"").append(site).append("\" mass=\"").append(modifiedMass).append("\">");
                    sw.writeLine(modificationSite.toString());
                }
                sw.decreaseIndent();
            }
            sw.writeLine("</modification_info>");
        }
        if (mainHit) {
            StringBuilder searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM raw score\" value=\"").append(psParameter.getPsmProbabilityScore()).append("\">");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM score\" value=\"").append(psParameter.getPsmScore()).append("\">");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM PEP\" value=\"").append(psParameter.getPsmProbability()).append("\">");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM confidence\" value=\"").append(psParameter.getPsmConfidence()).append("\">");
            sw.writeLine(searchScore.toString());
        } else {
            if (peptideAssumption.getRawScore() != null) {
                StringBuilder searchScore = new StringBuilder();
                searchScore.append("<search_score name=\"Identification algorithm raw score\" value=\"").append(peptideAssumption.getRawScore()).append("\">");
                sw.writeLine(searchScore.toString());
            }
            StringBuilder searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm score\" value=\"").append(peptideAssumption.getScore()).append("\">");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm PEP\" value=\"").append(psParameter.getSearchEngineProbability()).append("\">");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm confidence\" value=\"").append(psParameter.getSearchEngineConfidence()).append("\">");
            sw.writeLine(searchScore.toString());
        }
        sw.writeLine("<analysis_result>");
        int advocateId = peptideAssumption.getAdvocate();
        Advocate advocate = Advocate.getAdvocate(advocateId);
        StringBuilder parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"algorithm\" value=\"").append(advocate.getName()).append("\">");
        sw.writeLineIncreasedIndent(parameterLine.toString());
        if (modificationMatches != null && !modificationMatches.isEmpty()) {
            for (ModificationMatch modificationMatch : modificationMatches) {
                if (modificationMatch.isVariable()) {
                    parameterLine = new StringBuilder();
                    parameterLine.append("<parameter name=\"ptm\" value=\"").append(modificationMatch.getTheoreticPtm()).append(" (").append(modificationMatch.getModificationSite()).append(")").append("\">");
                    sw.writeLine(parameterLine.toString());
                }
            }
        }
        parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"charge\" value=\"").append(peptideAssumption.getIdentificationCharge().toString()).append("\">");
        sw.writeLine(parameterLine.toString());
        parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"isotope\" value=\"").append(peptideAssumption.getIsotopeNumber(precursorMz, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection())).append("\">");
        sw.writeLine(parameterLine.toString());
        sw.writeLineDecreasedIndent("</analysis_result>");
        sw.writeLineDecreasedIndent("</search_hit>");
    }

}
