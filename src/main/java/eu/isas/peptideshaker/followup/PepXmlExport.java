package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.aminoacids.AminoAcid;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidPattern;
import com.compomics.util.experiment.biology.atoms.Atom;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.io.export.xml.SimpleXmlWriter;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Export to PepXML.
 *
 * @author Marc Vaudel
 */
public class PepXmlExport {

    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The PTM factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();

    /**
     * Constructor.
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
     */
    public void writePepXmlFile(Identification identification, IdentificationParameters identificationParameters, File destinationFile, String peptideShakerVersion,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws IOException {

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
     * Writes the header.
     *
     * @param sw the XML file writer
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while writing to the file
     */
    private void writeHeader(SimpleXmlWriter sw) throws IOException {
        
        sw.writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    
    }

    /**
     * Writes the MS2 pipeline analysis block.
     *
     * @param sw the XML file writer
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
     */
    private void writeMsmsPipelineAnalysis(SimpleXmlWriter sw, String peptideShakerVersion, File destinationFile, Identification identification, IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler) throws IOException {

        sw.writeLine("<msms_pipeline_analysis xmlns=\"http://regis-web.systemsbiology.net/pepXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://sashimi.sourceforge.net/schema_revision/pepXML/pepXML_v117.xsd\" summary_xml=\"" + destinationFile.getAbsolutePath() + "\">");

        writeAnalysisSummary(sw, peptideShakerVersion);
        writeMsmsRunSummary(sw, identification, identificationParameters, waitingHandler);

        sw.writeLineDecreasedIndent("</msms_pipeline_analysis>");
        
    }

    /**
     * Writes the analysis summary section.
     *
     * @param sw the XML file writer
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
     * Writes the MS2 run summary section.
     *
     * @param sw the XML file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeMsmsRunSummary(SimpleXmlWriter sw, Identification identification, 
            IdentificationParameters identificationParameters, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            // reset the progress bar
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        
        }
        
        TreeSet<String> msFiles = new TreeSet<>(identification.getSpectrumIdentification().keySet());

        for (String spectrumFileName : msFiles) {

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

            DigestionParameters digestionPreferences = identificationParameters.getSearchParameters().getDigestionParameters();
            
            if (digestionPreferences.getCleavagePreference() == DigestionParameters.CleavagePreference.enzyme) {
            
                for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                
                    DigestionParameters.Specificity specificity = digestionPreferences.getSpecificity(enzyme.getName());
                    writeEnzyme(sw, enzyme, specificity);
                
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
     * @param sw the XML file writer
     * @param enzyme the enzyme
     * @param specificity the enzyme specificity
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeEnzyme(SimpleXmlWriter sw, Enzyme enzyme, DigestionParameters.Specificity specificity) throws IOException {

        sw.writeLineIncreasedIndent("<sample_enzyme name=\"" + enzyme.getName() + "\" fidelity=\"" + specificity + "\">");

        sw.increaseIndent();
        TreeSet<Character> aaBefore = new TreeSet<>(enzyme.getAminoAcidBefore());
        
        if (!aaBefore.isEmpty()) {
            
            String cut = aaBefore.stream()
                    .map(aa -> aa.toString())
                    .collect(Collectors.joining());
            
            TreeSet<Character> restrictionAfter = new TreeSet<>(enzyme.getRestrictionAfter());
            
            String noCut = restrictionAfter.stream()
                    .map(aa -> aa.toString())
                    .collect(Collectors.joining());
            
            sw.writeLine("<specificity cut=\"" + cut + "\" no_cut=\"" + noCut + "\" sense=\"C\"/>");
        
        }
        
        TreeSet<Character> aaAfter = new TreeSet<>(enzyme.getAminoAcidAfter());
        
        if (!aaAfter.isEmpty()) {
            
            String cut = aaAfter.stream()
                    .map(aa -> aa.toString())
                    .collect(Collectors.joining());

            TreeSet<Character> restrictionBefore = new TreeSet<>(enzyme.getRestrictionBefore());
            
            String noCut = restrictionBefore.stream()
                    .map(aa -> aa.toString())
                    .collect(Collectors.joining());
            
            sw.writeLine("<specificity cut=\"" + cut + "\" no_cut=\"" + noCut + "\" sense=\"N\"/>");
            
        }
        
        sw.writeLineDecreasedIndent("</sample_enzyme>");
    
    }

    /**
     * Writes the search summary section.
     *
     * @param sw the XML file writer
     * @param identificationParameters the identification parameters
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeSearchSummary(SimpleXmlWriter sw, IdentificationParameters identificationParameters) throws IOException {
        
        sw.writeLine("<search_summary precursor_mass_type=\"monoisotopic\" fragment_mass_type=\"monoisotopic\">");
        sw.increaseIndent();
        
        for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getFixedModifications()) {
        
            Modification modification = modificationFactory.getModification(modName);
            sw.writeLine(getModLine(modification, false));
        
        }
        
        for (String modName : identificationParameters.getSearchParameters().getModificationParameters().getAllNotFixedModifications()) {
        
            Modification modification = modificationFactory.getModification(modName);
            sw.writeLine(getModLine(modification, true));
        
        }
        
        sw.writeLineDecreasedIndent("</search_summary>");
    
    }

    /**
     * Gets the line for a modification.
     *
     * @param modification the modification
     * @param variable a boolean indicating whether the modification is variable
     *
     * @return the line for a modification
     */
    private String getModLine(Modification modification, boolean variable) {

        StringBuilder modificationLine = new StringBuilder();
        
        ModificationType modificationType = modification.getModificationType();
        
        if (modificationType == ModificationType.modaa 
                || modificationType == ModificationType.modnaa_peptide 
                || modificationType == ModificationType.modnaa_protein
                || modificationType == ModificationType.modcaa_peptide 
                || modificationType == ModificationType.modcaa_protein) {
            
            modificationLine.append("<aminoacid_modification ");
            AminoAcidPattern aminoAcidPattern = modification.getPattern();
            modificationLine.append("aminoacid=\"").append(aminoAcidPattern.toString()).append("\" ");
            modificationLine.append("massdiff=\"").append(modification.getMass()).append("\" ");
            
            if (aminoAcidPattern.getAminoAcidsAtTarget().size() == 1) {
            
                Character aa = aminoAcidPattern.getAminoAcidsAtTarget().get(0);
                AminoAcid aminoAcid = AminoAcid.getAminoAcid(aa);
                
                if (!aminoAcid.iscombination()) {
                
                    Double mass = aminoAcid.getMonoisotopicMass() + modification.getMass();
                    modificationLine.append("mass=\"").append(mass).append("\" ");
                
                }
            }
            
            if (modificationType.isCTerm()) {
                
                modificationLine.append("peptide_terminus=\"c\" ");
            
            }
            if (modificationType.isNTerm()) {
                
                modificationLine.append("peptide_terminus=\"n\" ");
            
            }
            
        } else {
            
            modificationLine.append("<terminal_modification ");
            
            if (modificationType.isCTerm()) {
                
                modificationLine.append("terminus=\"c\" ");
            
            }
            
            if (modificationType.isNTerm()) {
            
                modificationLine.append("terminus=\"n\" ");
            
            }
            
            modificationLine.append("massdiff=\"").append(modification.getMass()).append("\" ");
            
            if (modificationType == ModificationType.modc_protein) {
                
                modificationLine.append("protein_terminus=\"c\" ");
            
            }
            
            if (modificationType == ModificationType.modn_protein) {
                
                modificationLine.append("protein_terminus=\"n\" ");
            
            }
            
            if (modificationType.isNTerm()) {
                
                double mass = Atom.H.getMonoisotopicMass() + modification.getMass();
                modificationLine.append("mass=\"").append(mass).append("\" ");
            
            }
            
            if (modificationType.isCTerm()) {
            
                double mass = Atom.H.getMonoisotopicMass() + Atom.O.getMonoisotopicMass() + modification.getMass();
                modificationLine.append("mass=\"").append(mass).append("\" ");
            
            }
        }
        
        if (variable) {
        
            modificationLine.append("variable=\"Y\" ");
        
        } else {
        
            modificationLine.append("variable=\"N\" ");
        
        }
        
        modificationLine.append("symbol=\"").append(modification.getName()).append("\" ");
        CvTerm cvTerm = modification.getCvTerm();
        
        if (cvTerm != null) {
        
            modificationLine.append("description=\"").append(cvTerm.getAccession()).append("\"");
        
        }
        
        modificationLine.append("/>");
        return modificationLine.toString();
    
    }

    /**
     * Writes the spectrum queries.
     *
     * @param sw the XML file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeSpectrumQueries(SimpleXmlWriter sw, Identification identification, IdentificationParameters identificationParameters, String spectrumFile, WaitingHandler waitingHandler) throws IOException {
        
        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler, "spectrumFile == '" + spectrumFile + "'");
        SpectrumMatch spectrumMatch;
        
        while ((spectrumMatch = psmIterator.next()) != null) {
        
            String spectrumKey = spectrumMatch.getSpectrumKey();
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
     * @param sw the XML file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeSpectrumMatch(SimpleXmlWriter sw, Identification identification, IdentificationParameters identificationParameters, SpectrumMatch spectrumMatch) throws IOException {

        String spectrumKey = spectrumMatch.getSpectrumKey();
        Double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        PSParameter psParameter;

        sw.writeLineIncreasedIndent("<search_result>");
        sw.increaseIndent();

        // PeptideShaker hit
        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
        
        if (peptideAssumption != null) {
        
            psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
            writeSearchHit(sw, identificationParameters, peptideAssumption, precursorMz, psParameter, true);
        }

        // Search engines results
        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> assumptions = ((SpectrumMatch)identification.retrieveObject(spectrumMatch.getKey())).getPeptideAssumptionsMap();
        
        for (TreeMap<Double, ArrayList<PeptideAssumption>> scoreMap : assumptions.values()) {
        
            for (ArrayList<PeptideAssumption> spectrumIdentificationAssumptions : scoreMap.values()) {
            
                for (SpectrumIdentificationAssumption spectrumIdentificationAssumption : spectrumIdentificationAssumptions) {
                
                        peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        psParameter = (PSParameter) peptideAssumption.getUrParam(PSParameter.dummy);
                        writeSearchHit(sw, identificationParameters, peptideAssumption, precursorMz, psParameter, false);
                    
                }
            }
        }

        sw.writeLineDecreasedIndent("</search_result>");
        
    }

    /**
     * Writes a search hit section.
     *
     * @param sw the XML file writer
     * @param identification the identification object containing the
     * identification results
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler to display progress and allow
     * interrupting the process
     *
     * @throws IOException exception thrown whenever an error is encountered
     * while reading or writing a file
     */
    private void writeSearchHit(SimpleXmlWriter sw, IdentificationParameters identificationParameters, PeptideAssumption peptideAssumption, Double precursorMz, PSParameter psParameter, boolean mainHit) throws IOException {

        Peptide peptide = peptideAssumption.getPeptide();

        StringBuilder searchHitStart = new StringBuilder();
        searchHitStart.append("<search_hit hit_rank=\"").append(peptideAssumption.getRank()).append("\" ");
        searchHitStart.append("peptide=\"").append(peptide.getSequence()).append("\" ");
         TreeMap<String,int[]> proteinMapping = peptide.getProteinMapping();
        String proteins = proteinMapping.navigableKeySet().stream()
                .collect(Collectors.joining(","));
        
        searchHitStart.append("protein=\"").append(proteins).append("\" ");
        searchHitStart.append("num_tot_proteins=\"").append(proteinMapping.size()).append("\" ");
        searchHitStart.append("calc_neutral_pep_mass=\"").append(peptideAssumption.getTheoreticMass()).append("\" ");
        searchHitStart.append("massdiff=\"").append(peptideAssumption.getDeltaMass(precursorMz, false, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection())).append("\">");

        sw.writeLine(searchHitStart.toString());

        sw.increaseIndent();
        
        if (proteinMapping.size() > 1) {
            
            for (String accession : proteinMapping.navigableKeySet()) {
        
                StringBuilder proteinLine = new StringBuilder();
                proteinLine.append("<alternative_protein protein=\"").append(accession).append("\"/>");
                sw.writeLine(proteinLine.toString());
            
            }
        }
        
        ModificationMatch[] modificationMatches = peptide.getModificationMatches();
        
        if (modificationMatches.length > 0) {
            
            HashMap<Integer, Double> modifiedAminoAcids = new HashMap<>(modificationMatches.length);
            Double nTermMass = null;
            Double cTermMass = null;
            
            for (ModificationMatch modificationMatch : modificationMatches) {
            
                Modification modification = modificationFactory.getModification(modificationMatch.getModification());
                ModificationType modificationType = modification.getModificationType();
                
                if (modificationType == ModificationType.modn_protein || modificationType == ModificationType.modn_peptide) {
                    
                    if (nTermMass == null) {
                    
                        nTermMass = Atom.H.getMonoisotopicMass();
                    
                    }
                    
                    nTermMass += modification.getMass();
                
                } else if (modificationType == ModificationType.modc_protein || modificationType == ModificationType.modc_peptide) {
                    
                    if (cTermMass == null) {
                    
                        cTermMass = Atom.H.getMonoisotopicMass() + Atom.O.getMonoisotopicMass();
                    
                    }
                    
                    cTermMass += modification.getMass();
                
                } else {
                
                    int site = modificationMatch.getModificationSite();
                    Double modMass = modifiedAminoAcids.get(site);
                    
                    if (modMass == null) {
                    
                        modMass = 0.0;
                    
                    }
                    
                    modMass += modification.getMass();
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
                
                    double modifiedMass = modifiedAminoAcids.get(site);
                    StringBuilder modificationSite = new StringBuilder();
                    modificationSite.append("<mod_aminoacid_mass position=\"").append(site).append("\" mass=\"").append(modifiedMass).append("\"/>");
                    sw.writeLine(modificationSite.toString());
                
                }
                
                sw.decreaseIndent();
            
            }
            
            sw.writeLine("</modification_info>");
        
        }
        
        if (mainHit) {
        
            StringBuilder searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM raw score\" value=\"").append(psParameter.getPsmProbabilityScore()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM score\" value=\"").append(psParameter.getPsmScore()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM PEP\" value=\"").append(psParameter.getSpectrumMatchProbability()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"PSM confidence\" value=\"").append(psParameter.getPsmConfidence()).append("\"/>");
            sw.writeLine(searchScore.toString());
        
        } else {
        
            StringBuilder searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm raw score\" value=\"").append(peptideAssumption.getRawScore()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm score\" value=\"").append(peptideAssumption.getScore()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm PEP\" value=\"").append(psParameter.getSearchEngineProbability()).append("\"/>");
            sw.writeLine(searchScore.toString());
            searchScore = new StringBuilder();
            searchScore.append("<search_score name=\"Identification algorithm confidence\" value=\"").append(psParameter.getSearchEngineConfidence()).append("\"/>");
            sw.writeLine(searchScore.toString());
        
        }
        
        sw.writeLine("<analysis_result>");
        int advocateId = peptideAssumption.getAdvocate();
        Advocate advocate = Advocate.getAdvocate(advocateId);
        StringBuilder parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"algorithm\" value=\"")
                .append(advocate.getName())
                .append("\"/>");
        sw.writeLineIncreasedIndent(parameterLine.toString());
        
        
            for (ModificationMatch modificationMatch : modificationMatches) {
                if (modificationMatch.getVariable()) {
                    parameterLine = new StringBuilder();
                    parameterLine.append("<parameter name=\"ptm\" value=\"")
                            .append(modificationMatch.getModification())
                            .append(" (").append(modificationMatch.getModificationSite()).append(")")
                            .append("\"/>");
                    sw.writeLine(parameterLine.toString());
                }
            }
            
        parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"charge\" value=\"")
                .append(Integer.toString(peptideAssumption.getIdentificationCharge()))
                .append("\"/>");
        sw.writeLine(parameterLine.toString());
        parameterLine = new StringBuilder();
        parameterLine.append("<parameter name=\"isotope\" value=\"").append(peptideAssumption.getIsotopeNumber(precursorMz, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection())).append("\"/>");
        sw.writeLine(parameterLine.toString());
        sw.writeLineDecreasedIndent("</analysis_result>");
        sw.writeLineDecreasedIndent("</search_hit>");
        
    }
}
