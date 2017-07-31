package eu.isas.peptideshaker.fileimport;

import com.compomics.mascotdatfile.util.io.MascotIdfileReader;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.IdentificationAlgorithmParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.AndromedaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.OmssaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.XtandemParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.AndromedaIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MsAmandaIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.PepxmlIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.TideIdfileReader;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.protein_inference.PeptideMapperType;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTree;
import com.compomics.util.experiment.io.identifications.idfilereaders.NovorIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.OnyaseIdfileReader;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import de.proteinms.omxparser.util.OMSSAIdfileReader;
import de.proteinms.xtandemparser.parser.XTandemIdfileReader;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import static eu.isas.peptideshaker.fileimport.FileImporter.PTM_MASS_TOLERANCE;
import org.apache.commons.math.MathException;

/**
 * This class can be used to import PSMs from search engine results.
 *
 * @author Marc Vaudel
 */
public class PsmImporter {

    /**
     * The protein sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The object cache to use when encountering memory issues.
     */
    private ObjectsCache peptideShakerCache;
    /**
     * Indicates whether the check for X!Tandem modifications was done.
     */
    private boolean xTandemPtmsCheck = false;
    /**
     * The number of first hits.
     */
    private long nPSMs = 0;
    /**
     * The number of secondary hits.
     */
    private long nSecondary = 0;
    /**
     * The processing preferences.
     */
    private ProcessingPreferences processingPreferences;
    /**
     * The progress of the import.
     */
    private int progress = 0;
    /**
     * The number of PSMs which did not pass the import filters.
     */
    private int psmsRejected = 0;
    /**
     * The number of PSMs which were rejected due to a protein issue.
     */
    private int proteinIssue = 0;
    /**
     * The number of PSMs which were rejected due to a peptide issue.
     */
    private int peptideIssue = 0;
    /**
     * The number of PSMs which were rejected due to a precursor issue.
     */
    private int precursorIssue = 0;
    /**
     * The number of PSMs which were rejected due to a PTM issue.
     */
    private int ptmIssue = 0;
    /**
     * The number of retained first hits.
     */
    private int nRetained = 0;
    /**
     * The number of PTMs where no protein was found.
     */
    private int missingProteins = 0;
    /**
     * The id file reader where the PSMs are from.
     */
    private IdfileReader fileReader;
    /**
     * The identification file where the PSMs are from.
     */
    private File idFile;
    /**
     * List of ignored modifications.
     */
    private ArrayList<Integer> ignoredModifications = new ArrayList<Integer>(2);
    /**
     * The maximal peptide mass error found in ppm.
     */
    private double maxPeptideErrorPpm = 0;
    /**
     * The maximal peptide mass error found in Da.
     */
    private double maxPeptideErrorDa = 0;
    /**
     * The maximal tag mass error found in ppm.
     */
    private double maxTagErrorPpm = 0;
    /**
     * The maximal tag mass error found in Da.
     */
    private double maxTagErrorDa = 0;
    /**
     * The amount added per amino acid residue as part of the reference mass
     * when converting Dalton tolerances to ppm.
     */
    private double amountPerAminoAcidResidue = 100;
    /**
     * List of charges found.
     */
    private HashSet<Integer> charges = new HashSet<Integer>();
    /**
     * List of one hit wonders.
     */
    private HashSet<String> singleProteinList;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private HashMap<String, Integer> proteinCount;
    /**
     * The database connection.
     */
    private Identification identification;
    /**
     * The input map.
     */
    private InputMap inputMap;
    /**
     * The exception handler.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;

    /**
     * Constructor.
     *
     * @param peptideShakerCache the cache to use when memory issues are
     * encountered
     * @param identificationParameters the identification parameters
     * @param processingPreferences the processing preferences
     * @param fileReader the reader of the file which the matches are imported
     * from
     * @param idFile the file which the matches are imported from
     * @param identification the identification object where to store the
     * matches
     * @param inputMap the input map to use for scoring
     * @param proteinCount the protein count of this project
     * @param singleProteinList list of one hit wonders for this project
     * @param exceptionHandler handler for exceptions
     */
    public PsmImporter(ObjectsCache peptideShakerCache, IdentificationParameters identificationParameters, ProcessingPreferences processingPreferences, IdfileReader fileReader, File idFile,
            Identification identification, InputMap inputMap, HashMap<String, Integer> proteinCount, HashSet<String> singleProteinList,
            ExceptionHandler exceptionHandler) {
        this.peptideShakerCache = peptideShakerCache;
        this.identificationParameters = identificationParameters;
        this.processingPreferences = processingPreferences;
        this.fileReader = fileReader;
        this.idFile = idFile;
        this.identification = identification;
        this.inputMap = inputMap;
        this.proteinCount = proteinCount;
        this.singleProteinList = singleProteinList;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Imports PSMs.
     *
     * @param idFileSpectrumMatches the PSMs to import
     * @param nThreads the number of threads to use
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     */
    public void importPsms(LinkedList<SpectrumMatch> idFileSpectrumMatches, int nThreads, WaitingHandler waitingHandler)
            throws IOException, SQLException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        while (!idFileSpectrumMatches.isEmpty()) {
            SpectrumMatch match = idFileSpectrumMatches.pollLast();
            PsmImporterRunnable psmImporterRunnable = new PsmImporterRunnable(match, waitingHandler);
            pool.submit(psmImporterRunnable);
            if (waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
        }
        pool.shutdown();
        if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
            throw new InterruptedException("PSM import timed out. Please contact the developers.");
        }
    }

    /**
     * Imports a PSM.
     *
     * @param spectrumMatch the spectrum match to import
     * @param peptideSpectrumAnnotator the spectrum annotator to use to annotate
     * spectra
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math
     * exception occurred when estimating the noise level
     */
    private void importPsm(SpectrumMatch spectrumMatch, PeptideSpectrumAnnotator peptideSpectrumAnnotator, WaitingHandler waitingHandler)
            throws IOException, SQLException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException, MathException {

        // free memory if needed
        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !peptideShakerCache.isEmpty()) {
            peptideShakerCache.reduceMemoryConsumption(0.5, null);
        }
        // free memory from the tree if used
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        if (sequenceMatchingPreferences.getPeptideMapperType() == PeptideMapperType.tree) {
            if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
                ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
            }
            ProteinTree proteinTree = (ProteinTree) sequenceFactory.getDefaultPeptideMapper();
            if (!MemoryConsumptionStatus.halfGbFree() && proteinTree.getNodesInCache() > 0) {
                proteinTree.reduceNodeCacheSize(0.5);
            }
        }

        nPSMs++;

        String spectrumKey = spectrumMatch.getKey();

        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> matchAssumptions = spectrumMatch.getAssumptionsMap();
        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> rawDbAssumptions = null;
        if (fileReader.hasDeNovoTags()) { // for now only de novo results are stored in the database at this point
            rawDbAssumptions = identification.getRawAssumptions(spectrumKey);
        }

        if (matchAssumptions == null && rawDbAssumptions == null) {
            throw new IllegalArgumentException("No identification assumption found for PSM " + spectrumKey + ".");
        } else if (matchAssumptions != null && rawDbAssumptions != null) {
            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> combinedAssumptions
                    = new HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>>(Math.max(matchAssumptions.size(), rawDbAssumptions.size()));
            getAssumptions(matchAssumptions, combinedAssumptions);
            getAssumptions(rawDbAssumptions, combinedAssumptions);
            spectrumMatch.removeAssumptions();
            identification.removeRawAssumptions(spectrumKey);
            importAssumptions(spectrumMatch, combinedAssumptions, peptideSpectrumAnnotator, waitingHandler);
        } else if (matchAssumptions != null) {
            spectrumMatch.removeAssumptions();
            importAssumptions(spectrumMatch, matchAssumptions, peptideSpectrumAnnotator, waitingHandler);
        } else if (rawDbAssumptions != null) {
            identification.removeRawAssumptions(spectrumKey);
            importAssumptions(spectrumMatch, rawDbAssumptions, peptideSpectrumAnnotator, waitingHandler);
        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }
        waitingHandler.setSecondaryProgressCounter(++progress);
    }

    /**
     * Extracts the assumptions and adds them to the provided map.
     * 
     * @param matchAssumptions the match assumptions
     * @param combinedAssumptions the combined assumptions
     */
    private void getAssumptions(HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> matchAssumptions, HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> combinedAssumptions) {
        for (Integer algorithm : matchAssumptions.keySet()) {
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> algorithmMap = matchAssumptions.get(algorithm);
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> combinedAlgorithmMap = combinedAssumptions.get(algorithm);
            if (combinedAlgorithmMap == null) {
                combinedAlgorithmMap = new HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>(algorithmMap.size());
                combinedAssumptions.put(algorithm, algorithmMap);
            }
            for (Double score : algorithmMap.keySet()) {
                ArrayList<SpectrumIdentificationAssumption> scoreAssumptions = algorithmMap.get(score);
                ArrayList<SpectrumIdentificationAssumption> combinedScoreAssumptions = combinedAlgorithmMap.get(score);
                if (combinedScoreAssumptions == null) {
                    combinedScoreAssumptions = new ArrayList<SpectrumIdentificationAssumption>(scoreAssumptions.size());
                    combinedAlgorithmMap.put(score, scoreAssumptions);
                }
                combinedScoreAssumptions.addAll(scoreAssumptions);
            }
        }
    }

    /**
     * Import the assumptions. Maps algorithm specific PTMs to the compomics
     * utilities structure. Relocates aberrant modifications and removes
     * assumptions where not all PTMs are mapped. Verifies whether there is a
     * best match for the spectrum according to the search engine score.
     *
     * @param spectrumMatch the spectrum match to import
     * @param assumptions the assumptions to import
     * @param peptideSpectrumAnnotator the spectrum annotator to use to annotate
     * spectra
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown if a math
     * exception occurred when estimating the noise level
     */
    private void importAssumptions(SpectrumMatch spectrumMatch, HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions, PeptideSpectrumAnnotator peptideSpectrumAnnotator, WaitingHandler waitingHandler)
            throws IOException, SQLException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException, MathException {

        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SequenceMatchingPreferences ptmSequenceMatchingPreferences = identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        String spectrumKey = spectrumMatch.getKey();
        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        for (HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsForAdvocate : assumptions.values()) {
            for (ArrayList<SpectrumIdentificationAssumption> assumptionsAtScore : assumptionsForAdvocate.values()) {
                nSecondary += assumptionsAtScore.size();
            }
        }

        for (int advocateId : assumptions.keySet()) {

            if (advocateId == Advocate.xtandem.getIndex()) {
                verifyXTandemPtms();
            }

            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsForAdvocate = assumptions.get(advocateId);

            // Map PTMs
            HashSet<Double> scores = new HashSet<Double>(assumptionsForAdvocate.keySet());
            for (Double eValue : scores) {

                ArrayList<SpectrumIdentificationAssumption> oldAssumptions = assumptionsForAdvocate.get(eValue);
                ArrayList<SpectrumIdentificationAssumption> newAssumptions = new ArrayList<SpectrumIdentificationAssumption>(oldAssumptions.size());

                for (SpectrumIdentificationAssumption assumption : oldAssumptions) {

                    if (assumption instanceof PeptideAssumption) {

                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                        Peptide peptide = peptideAssumption.getPeptide();
                        String peptideSequence = peptide.getSequence();

                        // kick out peptides that are too long or too short
                        if (peptideSequence.length() >= peptideAssumptionFilter.getMinPepLength() && peptideSequence.length() <= peptideAssumptionFilter.getMaxPepLength()) {

                            // map the algorithm specific modifications on utilities modifications
                            // If there are not enough sites to put them all on the sequence, add an unknown modification
                            // Note: this needs to be done for tag based assumptions as well since the protein mapping can return erroneous modifications for some pattern based PTMs
                            PtmSettings modificationProfile = searchParameters.getPtmSettings();

                            // set the matching type to amino acid for the fixed ptms
                            boolean fixedPtmIssue = false;
                            try {
                                ptmFactory.checkFixedModifications(modificationProfile, peptide, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
                            } catch (IllegalArgumentException e) {
                                if (peptideAssumptionFilter.removeUnknownPTMs()) {
                                    // Exclude peptides with aberrant PTM mapping
                                    System.out.println(e.getMessage());
                                    ptmIssue++;
                                    fixedPtmIssue = true;
                                } else {
                                    throw e;
                                }
                            }

                            if (!fixedPtmIssue) {

                                HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<Integer, ArrayList<String>>();
                                HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<ModificationMatch, ArrayList<String>>();

                                if (peptide.isModified()) {
                                    for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                        HashMap<Integer, ArrayList<String>> tempNames = new HashMap<Integer, ArrayList<String>>();
                                        if (modMatch.isVariable()) {
                                            String sePTM = modMatch.getTheoreticPtm();
                                            if (fileReader instanceof OMSSAIdfileReader) {
                                                OmssaParameters omssaParameters = (OmssaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex());
                                                if (!omssaParameters.hasPtmIndexes()) {
                                                    throw new IllegalArgumentException("OMSSA modification indexes not set in the search parameters.");
                                                }
                                                Integer omssaIndex = null;
                                                try {
                                                    omssaIndex = new Integer(sePTM);
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException("Impossible to parse OMSSA modification index " + sePTM + ".");
                                                }
                                                String omssaName = omssaParameters.getModificationName(omssaIndex);
                                                if (omssaName == null) {
                                                    if (!ignoredModifications.contains(omssaIndex)) {
                                                        waitingHandler.appendReport("Impossible to find OMSSA modification of index "
                                                                + omssaIndex + ". The corresponding peptides will be ignored.", true, true);
                                                        ignoredModifications.add(omssaIndex);
                                                    }
                                                    omssaName = PTMFactory.unknownPTM.getName();
                                                }
                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, omssaName, PTM_MASS_TOLERANCE, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
                                            } else if (fileReader instanceof AndromedaIdfileReader) {
                                                AndromedaParameters andromedaParameters = (AndromedaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex());
                                                if (!andromedaParameters.hasPtmIndexes()) {
                                                    throw new IllegalArgumentException("Andromeda modification indexes not set in the search parameters.");
                                                }
                                                Integer andromedaIndex = null;
                                                try {
                                                    andromedaIndex = new Integer(sePTM);
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException("Impossible to parse Andromeda modification index " + sePTM + ".");
                                                }
                                                String andromedaName = andromedaParameters.getModificationName(andromedaIndex);
                                                if (andromedaName == null) {
                                                    if (!ignoredModifications.contains(andromedaIndex)) {
                                                        waitingHandler.appendReport("Impossible to find Andromeda modification of index "
                                                                + andromedaIndex + ". The corresponding peptides will be ignored.", true, true);
                                                        ignoredModifications.add(andromedaIndex);
                                                    }
                                                    andromedaName = PTMFactory.unknownPTM.getName();
                                                }
                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, andromedaName, PTM_MASS_TOLERANCE, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
                                            } else if (fileReader instanceof MascotIdfileReader
                                                    || fileReader instanceof XTandemIdfileReader
                                                    || fileReader instanceof MsAmandaIdfileReader
                                                    || fileReader instanceof MzIdentMLIdfileReader
                                                    || fileReader instanceof PepxmlIdfileReader
                                                    || fileReader instanceof TideIdfileReader) {
                                                String[] parsedName = sePTM.split("@");
                                                Double seMass;
                                                try {
                                                    seMass = new Double(parsedName[0]);
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException("Impossible to parse \'" + sePTM + "\' as a tagged modification.\n"
                                                            + "Error encountered in peptide " + peptideSequence + " spectrum " + spectrumTitle + " in spectrum file "
                                                            + spectrumFileName + ".\n" + "Identification file: " + idFile.getName());
                                                }
                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, seMass, PTM_MASS_TOLERANCE, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
                                            } else if (fileReader instanceof DirecTagIdfileReader
                                                    || fileReader instanceof NovorIdfileReader
                                                    || fileReader instanceof OnyaseIdfileReader) {
                                                PTM ptm = ptmFactory.getPTM(sePTM);
                                                if (ptm == PTMFactory.unknownPTM) {
                                                    throw new IllegalArgumentException("PTM not recognized spectrum " + spectrumTitle + " of file " + spectrumFileName + ".");
                                                }
                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, ptm.getMass(), PTM_MASS_TOLERANCE, sequenceMatchingPreferences, ptmSequenceMatchingPreferences);
                                            } else {
                                                throw new IllegalArgumentException("PTM mapping not implemented for the parsing of " + idFile.getName() + ".");
                                            }

                                            ArrayList<String> allNames = new ArrayList<String>();
                                            for (ArrayList<String> namesAtAA : tempNames.values()) {
                                                for (String name : namesAtAA) {
                                                    if (!allNames.contains(name)) {
                                                        allNames.add(name);
                                                    }
                                                }
                                            }
                                            modNames.put(modMatch, allNames);
                                            for (int pos : tempNames.keySet()) {
                                                ArrayList<String> namesAtPosition = expectedNames.get(pos);
                                                if (namesAtPosition == null) {
                                                    namesAtPosition = new ArrayList<String>(2);
                                                    expectedNames.put(pos, namesAtPosition);
                                                }
                                                for (String ptmName : tempNames.get(pos)) {
                                                    if (!namesAtPosition.contains(ptmName)) {
                                                        namesAtPosition.add(ptmName);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                initialPtmMapping(peptide, expectedNames, modNames, searchParameters);
                                newAssumptions.add(assumption);
                            }
                        } else {
                            peptideIssue++;
                        }
                    }
                }

                // see if we have any assumptions left
                if (!newAssumptions.isEmpty()) {
                    assumptionsForAdvocate.put(eValue, newAssumptions);
                } else {
                    assumptionsForAdvocate.remove(eValue);
                }
            }

            // try to find the best peptide hit passing the initial filters
            PeptideAssumption firstPeptideHit = null;
            PeptideAssumption firstPeptideHitNoProtein = null;
            TagAssumption firstTagHit = null;
            if (!assumptionsForAdvocate.isEmpty()) {
                ArrayList<Double> eValues = new ArrayList<Double>(assumptionsForAdvocate.keySet());
                Collections.sort(eValues);
                for (Double eValue : eValues) {
                    ArrayList<PeptideAssumption> firstHits = new ArrayList<PeptideAssumption>(1);
                    ArrayList<PeptideAssumption> firstHitsNoProteins = new ArrayList<PeptideAssumption>(1);
                    for (SpectrumIdentificationAssumption assumption : assumptionsForAdvocate.get(eValue)) {
                        if (assumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            Peptide peptide = peptideAssumption.getPeptide();
                            boolean filterPassed = true;
                            if (!peptideAssumptionFilter.validatePeptide(peptide, sequenceMatchingPreferences, searchParameters.getDigestionPreferences())) {
                                filterPassed = false;
                                peptideIssue++;
                            } else if (!peptideAssumptionFilter.validateModifications(peptide, sequenceMatchingPreferences, ptmSequenceMatchingPreferences, searchParameters.getPtmSettings())) {
                                filterPassed = false;
                                ptmIssue++;
                            } else if (!peptideAssumptionFilter.validatePrecursor(peptideAssumption, spectrumKey, spectrumFactory, searchParameters)) {
                                filterPassed = false;
                                precursorIssue++;
                            } else if (!peptideAssumptionFilter.validateProteins(peptide, sequenceMatchingPreferences)) {
                                filterPassed = false;
                                proteinIssue++;
                            } else {
                                ArrayList<String> accessions = peptide.getParentProteins(sequenceMatchingPreferences);
                                if (accessions == null || accessions.isEmpty()) {
                                    missingProteins++;
                                    filterPassed = false;
                                    if (firstPeptideHitNoProtein != null) {
                                        firstHitsNoProteins.add(peptideAssumption);
                                    }
                                }
                            }

                            if (filterPassed) {
                                firstHits.add(peptideAssumption);
                            }
                        }
                    }
                    if (!firstHits.isEmpty()) {
                        firstPeptideHit = BestMatchSelection.getBestHit(spectrumKey, firstHits, proteinCount, sequenceMatchingPreferences, identificationParameters, peptideSpectrumAnnotator);
                    }
                    if (firstPeptideHit != null) {
                        inputMap.addEntry(advocateId, spectrumFileName, firstPeptideHit.getScore(), firstPeptideHit.getPeptide().isDecoy(sequenceMatchingPreferences));
                        nRetained++;
                        break;
                    } else if (!firstHitsNoProteins.isEmpty()) {
                        // See if a peptide without protein can be a best match
                        firstPeptideHitNoProtein = BestMatchSelection.getBestHit(spectrumKey, firstHits, proteinCount, sequenceMatchingPreferences, identificationParameters, peptideSpectrumAnnotator);
                    }
                }
                if (firstPeptideHit != null) {
                    checkPeptidesMassErrorsAndCharges(spectrumKey, firstPeptideHit);
                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> previousAssumptions = identification.getAssumptions(spectrumKey);
                    identification.addAssumptions(spectrumKey, assumptions, previousAssumptions == null);
                    identification.addSpectrumMatch(spectrumMatch);
                }
                if (firstPeptideHit == null) {
                    // Check if a peptide with no protein can be a good candidate
                    if (firstPeptideHitNoProtein != null) {
                        checkPeptidesMassErrorsAndCharges(spectrumKey, firstPeptideHitNoProtein);
                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> previousAssumptions = identification.getAssumptions(spectrumKey);
                        identification.addAssumptions(spectrumKey, assumptions, previousAssumptions == null);
                        identification.addSpectrumMatch(spectrumMatch);
                    } else {
                        // Try to find the best tag hit
                        eValues = new ArrayList<Double>(assumptionsForAdvocate.keySet());
                        Collections.sort(eValues);
                        for (Double eValue : eValues) {
                            for (SpectrumIdentificationAssumption assumption : assumptionsForAdvocate.get(eValue)) {
                                if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    firstTagHit = tagAssumption;
                                    checkTagMassErrorsAndCharge(spectrumKey, tagAssumption);
                                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> previousAssumptions = identification.getAssumptions(spectrumKey);
                                    identification.addAssumptions(spectrumKey, assumptions, previousAssumptions == null);
                                    identification.addSpectrumMatch(spectrumMatch);
                                    break;
                                }
                            }
                            if (firstTagHit != null) {
                                break;
                            }
                        }
                    }
                }
                if (firstPeptideHit == null && firstPeptideHitNoProtein == null && firstTagHit == null) {
                    psmsRejected++;
                }
            }
        }
    }

    /**
     * Attempts at guessing whether a peptide as parsed from the search engine
     * results can carry a protein terminal PTM.
     *
     * @param fileReader the file reader used for the parsing
     * @param searchParameters the search parameters
     * @param peptide the peptide of interest
     * @param terminalModificationMasses convenience list containing the masses
     * of protein terminal PTMs
     *
     * @return true if the peptide can carry protein terminal PTM.
     */
    public static boolean hasPotentialTerminalModification(IdfileReader fileReader, SearchParameters searchParameters, Peptide peptide, LinkedList<Double> terminalModificationMasses) {
        PTMFactory ptmFactory = PTMFactory.getInstance();
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            String sePTM = modificationMatch.getTheoreticPtm();
            if (fileReader instanceof OMSSAIdfileReader) {
                OmssaParameters omssaParameters = (OmssaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex());
                if (!omssaParameters.hasPtmIndexes()) {
                    throw new IllegalArgumentException("OMSSA modification indexes not set in the search parameters.");
                }
                Integer omssaIndex;
                try {
                    omssaIndex = new Integer(sePTM);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Impossible to parse OMSSA modification index " + sePTM + ".");
                }
                String ptmName = omssaParameters.getModificationName(omssaIndex);
                if (ptmName != null) {
                    PTM ptm = ptmFactory.getPTM(ptmName);
                    if (ptm.getType() == PTM.MODC
                            || ptm.getType() == PTM.MODCAA
                            || ptm.getType() == PTM.MODN
                            || ptm.getType() == PTM.MODNAA) {
                        return true;
                    }
                }
            } else if (fileReader instanceof AndromedaIdfileReader) {
                AndromedaParameters andromedaParameters = (AndromedaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex());
                if (!andromedaParameters.hasPtmIndexes()) {
                    throw new IllegalArgumentException("Andromeda modification indexes not set in the search parameters.");
                }
                Integer andromedaIndex = null;
                try {
                    andromedaIndex = new Integer(sePTM);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Impossible to parse Andromdea modification index " + sePTM + ".");
                }
                String ptmName = andromedaParameters.getModificationName(andromedaIndex);
                if (ptmName != null) {
                    PTM ptm = ptmFactory.getPTM(ptmName);
                    if (ptm.getType() == PTM.MODC
                            || ptm.getType() == PTM.MODCAA
                            || ptm.getType() == PTM.MODN
                            || ptm.getType() == PTM.MODNAA) {
                        return true;
                    }
                }
            } else if (fileReader instanceof MascotIdfileReader
                    || fileReader instanceof XTandemIdfileReader
                    || fileReader instanceof MsAmandaIdfileReader
                    || fileReader instanceof MzIdentMLIdfileReader
                    || fileReader instanceof PepxmlIdfileReader
                    || fileReader instanceof TideIdfileReader) {
                String[] parsedName = sePTM.split("@");
                Double seMass;
                try {
                    seMass = new Double(parsedName[0]);
                    for (Double terminalModificationMass : terminalModificationMasses) {
                        if (Math.abs(seMass - terminalModificationMass) < 0.1) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Impossible to parse \'" + sePTM + "\' as a modification.");
                }
            } else if (fileReader instanceof DirecTagIdfileReader
                    || fileReader instanceof NovorIdfileReader
                    || fileReader instanceof OnyaseIdfileReader) {
                PTM ptm = ptmFactory.getPTM(sePTM);
                if (ptm.getType() == PTM.MODC
                        || ptm.getType() == PTM.MODCAA
                        || ptm.getType() == PTM.MODN
                        || ptm.getType() == PTM.MODNAA) {
                    return true;
                }
            } else {
                throw new IllegalArgumentException("PTM mapping not implemented for the parsing of " + fileReader.getClass() + ".");
            }
        }
        return false;
    }

    /**
     * Makes an initial PTM mapping based on the search engine results and the
     * compatibility to the searched PTMs.
     *
     * @param peptide the peptide
     * @param expectedNames the expected PTMs
     * @param modNames the modification names possible for every modification
     * match
     * @param searchParameters the search parameters
     */
    private void initialPtmMapping(Peptide peptide, HashMap<Integer, ArrayList<String>> expectedNames, HashMap<ModificationMatch, ArrayList<String>> modNames, SearchParameters searchParameters) {
        PtmSettings modificationProfile = searchParameters.getPtmSettings();
        int peptideLength = peptide.getSequence().length();

        // If a terminal modification cannot be elsewhere lock the terminus
        ModificationMatch nTermModification = null;
        if (peptide.isModified()) {
            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                    double refMass = getRefMass(modMatch.getTheoreticPtm(), searchParameters);
                    int modSite = modMatch.getModificationSite();
                    if (modSite == 1) {
                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                        if (expectedNamesAtSite != null) {
                            ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                            for (String ptmName : expectedNamesAtSite) {
                                PTM ptm = ptmFactory.getPTM(ptmName);
                                if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)) {
                                    filteredNamesAtSite.add(ptmName);
                                }
                            }
                            for (String modName : filteredNamesAtSite) {
                                PTM ptm = ptmFactory.getPTM(modName);
                                if (ptm.isNTerm()) {
                                    boolean otherPossibleMod = false;
                                    for (String tempName : modificationProfile.getAllNotFixedModifications()) {
                                        if (!tempName.equals(modName)) {
                                            PTM tempPTM = ptmFactory.getPTM(tempName);
                                            if (tempPTM.getMass() == ptm.getMass() && !tempPTM.isNTerm()) {
                                                otherPossibleMod = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!otherPossibleMod) {
                                        nTermModification = modMatch;
                                        modMatch.setTheoreticPtm(modName);
                                        break;
                                    }
                                }
                            }
                            if (nTermModification != null) {
                                break;
                            }
                        }
                    }
                }
            }
            ModificationMatch cTermModification = null;
            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName()) && modMatch != nTermModification) {
                    double refMass = getRefMass(modMatch.getTheoreticPtm(), searchParameters);
                    int modSite = modMatch.getModificationSite();
                    if (modSite == peptideLength) {
                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                        if (expectedNamesAtSite != null) {
                            ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                            for (String ptmName : expectedNamesAtSite) {
                                PTM ptm = ptmFactory.getPTM(ptmName);
                                if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)) {
                                    filteredNamesAtSite.add(ptmName);
                                }
                            }
                            for (String modName : filteredNamesAtSite) {
                                PTM ptm = ptmFactory.getPTM(modName);
                                if (ptm.isCTerm()) {
                                    boolean otherPossibleMod = false;
                                    for (String tempName : modificationProfile.getAllNotFixedModifications()) {
                                        if (!tempName.equals(modName)) {
                                            PTM tempPTM = ptmFactory.getPTM(tempName);
                                            if (tempPTM.getMass() == ptm.getMass() && !tempPTM.isCTerm()) {
                                                otherPossibleMod = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!otherPossibleMod) {
                                        cTermModification = modMatch;
                                        modMatch.setTheoreticPtm(modName);
                                        break;
                                    }
                                }
                            }
                            if (cTermModification != null) {
                                break;
                            }
                        }
                    }
                }
            }

            // Map the modifications according to search engine localization
            HashMap<Integer, ArrayList<String>> siteToPtmMap = new HashMap<Integer, ArrayList<String>>(); // Site to ptm name including termini
            HashMap<Integer, ModificationMatch> siteToMatchMap = new HashMap<Integer, ModificationMatch>(); // Site to Modification match excluding termini
            HashMap<ModificationMatch, Integer> matchToSiteMap = new HashMap<ModificationMatch, Integer>(); // Modification match to site excluding termini
            boolean allMapped = true;

            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                boolean mapped = false;
                if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                    double refMass = getRefMass(modMatch.getTheoreticPtm(), searchParameters);
                    int modSite = modMatch.getModificationSite();
                    boolean terminal = false;
                    ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                    if (expectedNamesAtSite != null) {
                        ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                        ArrayList<String> modificationAtSite = siteToPtmMap.get(modSite);
                        for (String ptmName : expectedNamesAtSite) {
                            PTM ptm = ptmFactory.getPTM(ptmName);
                            if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                    && (modificationAtSite == null || !modificationAtSite.contains(ptmName))) {
                                filteredNamesAtSite.add(ptmName);
                            }
                        }
                        if (filteredNamesAtSite.size() == 1) {
                            String ptmName = filteredNamesAtSite.get(0);
                            PTM ptm = ptmFactory.getPTM(ptmName);
                            if (ptm.isNTerm() && nTermModification == null) {
                                nTermModification = modMatch;
                                mapped = true;
                            } else if (ptm.isCTerm() && cTermModification == null) {
                                cTermModification = modMatch;
                                mapped = true;
                            } else if (!ptm.isNTerm() && !ptm.isCTerm()) {
                                matchToSiteMap.put(modMatch, modSite);
                                siteToMatchMap.put(modSite, modMatch);
                                mapped = true;
                            }
                            if (mapped) {
                                modMatch.setTheoreticPtm(ptmName);
                                if (modificationAtSite == null) {
                                    modificationAtSite = new ArrayList<String>(2);
                                    siteToPtmMap.put(modSite, modificationAtSite);
                                }
                                modificationAtSite.add(ptmName);
                            }
                        }
                        if (!mapped) {
                            if (filteredNamesAtSite.isEmpty()) {
                                filteredNamesAtSite = expectedNamesAtSite;
                            }
                            if (modSite == 1) {
                                Double minDiff = null;
                                String bestPtmName = null;
                                for (String modName : filteredNamesAtSite) {
                                    PTM ptm = ptmFactory.getPTM(modName);
                                    if (ptm.isNTerm() && nTermModification == null) {
                                        double massError = Math.abs(refMass - ptm.getMass());
                                        if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                                && (minDiff == null || massError < minDiff)) {
                                            bestPtmName = modName;
                                            minDiff = massError;
                                        }
                                    }
                                }
                                if (bestPtmName != null) {
                                    nTermModification = modMatch;
                                    modMatch.setTheoreticPtm(bestPtmName);
                                    terminal = true;
                                    if (modificationAtSite == null) {
                                        modificationAtSite = new ArrayList<String>(2);
                                        siteToPtmMap.put(modSite, modificationAtSite);
                                    }
                                    modificationAtSite.add(bestPtmName);
                                    mapped = true;
                                }
                            } else if (modSite == peptideLength) {
                                Double minDiff = null;
                                String bestPtmName = null;
                                for (String modName : filteredNamesAtSite) {
                                    PTM ptm = ptmFactory.getPTM(modName);
                                    if (ptm.isCTerm() && cTermModification == null) {
                                        double massError = Math.abs(refMass - ptm.getMass());
                                        if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                                && (minDiff == null || massError < minDiff)) {
                                            bestPtmName = modName;
                                            minDiff = massError;
                                        }
                                    }
                                }
                                if (bestPtmName != null) {
                                    cTermModification = modMatch;
                                    modMatch.setTheoreticPtm(bestPtmName);
                                    terminal = true;
                                    if (modificationAtSite == null) {
                                        modificationAtSite = new ArrayList<String>(2);
                                        siteToPtmMap.put(modSite, modificationAtSite);
                                    }
                                    modificationAtSite.add(bestPtmName);
                                    mapped = true;
                                }
                            }
                            if (!terminal) {
                                Double minDiff = null;
                                String bestPtmName = null;
                                for (String modName : filteredNamesAtSite) {
                                    PTM ptm = ptmFactory.getPTM(modName);
                                    if (!ptm.isCTerm() && !ptm.isNTerm() && modNames.get(modMatch).contains(modName) && !siteToMatchMap.containsKey(modSite)) {
                                        double massError = Math.abs(refMass - ptm.getMass());
                                        if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                                && (minDiff == null || massError < minDiff)) {
                                            bestPtmName = modName;
                                            minDiff = massError;
                                        }
                                    }
                                }
                                if (bestPtmName != null) {
                                    modMatch.setTheoreticPtm(bestPtmName);
                                    if (modificationAtSite == null) {
                                        modificationAtSite = new ArrayList<String>(2);
                                        siteToPtmMap.put(modSite, modificationAtSite);
                                    }
                                    modificationAtSite.add(bestPtmName);
                                    matchToSiteMap.put(modMatch, modSite);
                                    siteToMatchMap.put(modSite, modMatch);
                                    mapped = true;
                                }
                            }
                        }
                    }
                }
                if (!mapped) {
                    allMapped = false;
                }
            }

            if (!allMapped) {

                // Try to correct incompatible localizations
                HashMap<Integer, ArrayList<Integer>> remap = new HashMap<Integer, ArrayList<Integer>>();

                for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                    if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                        int modSite = modMatch.getModificationSite();
                        for (int candidateSite : expectedNames.keySet()) {
                            if (!siteToMatchMap.containsKey(candidateSite)) {
                                for (String modName : expectedNames.get(candidateSite)) {
                                    if (modNames.get(modMatch).contains(modName)) {
                                        PTM ptm = ptmFactory.getPTM(modName);
                                        if ((!ptm.isCTerm() || cTermModification == null)
                                                && (!ptm.isNTerm() || nTermModification == null)) {
                                            ArrayList<Integer> ptmSites = remap.get(modSite);
                                            if (ptmSites == null) {
                                                ptmSites = new ArrayList<Integer>(4);
                                                remap.put(modSite, ptmSites);
                                            }
                                            if (!ptmSites.contains(candidateSite)) {
                                                ptmSites.add(candidateSite);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HashMap<Integer, Integer> correctedIndexes = PtmSiteMapping.alignAll(remap);

                for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                    if (modMatch.isVariable() && modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch) && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                        Integer modSite = correctedIndexes.get(modMatch.getModificationSite());
                        if (modSite != null) {
                            if (expectedNames.containsKey(modSite)) {
                                for (String modName : expectedNames.get(modSite)) {
                                    if (modNames.get(modMatch).contains(modName)) {
                                        ArrayList<String> taken = siteToPtmMap.get(modSite);
                                        if (taken == null || !taken.contains(modName)) {
                                            matchToSiteMap.put(modMatch, modSite);
                                            modMatch.setTheoreticPtm(modName);
                                            modMatch.setModificationSite(modSite);
                                            if (taken == null) {
                                                taken = new ArrayList<String>(2);
                                                siteToPtmMap.put(modSite, taken);
                                            }
                                            taken.add(modName);
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            matchToSiteMap.put(modMatch, modSite);
                            modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                        }
                        if (!matchToSiteMap.containsKey(modMatch)) {
                            modMatch.setTheoreticPtm(PTMFactory.unknownPTM.getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves the peptide maximal mass error and found charge.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     */
    private synchronized void checkPeptidesMassErrorsAndCharges(String spectrumKey, PeptideAssumption peptideAssumption)
            throws IOException, InterruptedException, SQLException, ClassNotFoundException, MzMLUnmarshallerException {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

        if (error > maxPeptideErrorPpm) {
            maxPeptideErrorPpm = error;
        }

        error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

        if (error > maxPeptideErrorDa) {
            maxPeptideErrorDa = error;
        }

        int currentCharge = peptideAssumption.getIdentificationCharge().value;

        if (!charges.contains(currentCharge)) {
            charges.add(currentCharge);
        }

        ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences());
        for (String protein : accessions) {
            Integer count = proteinCount.get(protein);
            if (count != null) {
                proteinCount.put(protein, count + 1);
            } else {
                boolean oneHitWonder = singleProteinList.contains(protein);
                if (oneHitWonder) {
                    singleProteinList.remove(protein);
                    proteinCount.put(protein, 2);
                } else {
                    singleProteinList.add(protein);
                }
            }
        }
    }

    /**
     * Saves the maximal precursor error and charge.
     *
     * @param spectrumKey the key of the spectrum match
     * @param tagAssumption the tag assumption
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     * @throws InterruptedException exception thrown if a thread is interrupted
     */
    private synchronized void checkTagMassErrorsAndCharge(String spectrumKey, TagAssumption tagAssumption) throws MzMLUnmarshallerException, IOException, InterruptedException {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        double error = Math.abs(tagAssumption.getDeltaMass(precursorMz, true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

        if (error > maxTagErrorPpm) {
            maxTagErrorPpm = error;
        }

        error = Math.abs(tagAssumption.getDeltaMass(precursorMz, false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));

        if (error > maxTagErrorDa) {
            maxTagErrorDa = error;
        }

        int currentCharge = tagAssumption.getIdentificationCharge().value;

        if (!charges.contains(currentCharge)) {
            charges.add(currentCharge);
        }
    }

    /**
     * Verifies that the modifications targeted by the quick acetyl and quick
     * pyrolidone are included in the search parameters.
     */
    private synchronized void verifyXTandemPtms() {
        if (!xTandemPtmsCheck) {
            SearchParameters searchParameters = identificationParameters.getSearchParameters();
            PtmSettings modificationProfile = searchParameters.getPtmSettings();
            IdentificationAlgorithmParameter algorithmParameter = searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex());
            if (algorithmParameter != null) {
                XtandemParameters xtandemParameters = (XtandemParameters) algorithmParameter;
                if (xtandemParameters.isProteinQuickAcetyl() && !modificationProfile.contains("Acetylation of protein N-term")) {
                    PTM ptm = PTMFactory.getInstance().getPTM("Acetylation of protein N-term");
                    if (!modificationProfile.getRefinementVariableModifications().contains(ptm.getName())) {
                        modificationProfile.addRefinementVariableModification(ptm);
                    }
                }
                String[] pyroMods = {"Pyrolidone from E", "Pyrolidone from Q", "Pyrolidone from carbamidomethylated C"};
                if (xtandemParameters.isQuickPyrolidone()) {
                    for (String ptmName : pyroMods) {
                        if (!modificationProfile.getRefinementVariableModifications().contains(ptmName)) {
                            PTM ptm = PTMFactory.getInstance().getPTM(ptmName);
                            modificationProfile.addRefinementVariableModification(ptm);
                        }
                    }
                }
            }
            xTandemPtmsCheck = true;
        }
    }

    /**
     * Returns the mass indicated by the identification algorithm for the given
     * PTM. 0 if not found.
     *
     * @param sePtmName the name according to the identification algorithm
     * @param searchParameters the search parameters
     *
     * @return the mass of the PTM
     */
    private double getRefMass(String sePtmName, SearchParameters searchParameters) {

        Double refMass = 0.0;
        // Try utilities modifications
        PTM refPtm = ptmFactory.getPTM(sePtmName);
        if (refPtm == PTMFactory.unknownPTM) {
            // Try mass@AA
            int atIndex = sePtmName.indexOf("@");
            if (atIndex > 0) {
                refMass = new Double(sePtmName.substring(0, atIndex));
            } else {
                // Try OMSSA indexes
                try {
                    int omssaIndex = new Integer(sePtmName);
                    OmssaParameters omssaParameters = (OmssaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex());
                    if (!omssaParameters.hasPtmIndexes()) {
                        throw new IllegalArgumentException("OMSSA modification indexes not set in the search parameters.");
                    }
                    String omssaName = omssaParameters.getModificationName(omssaIndex);
                    if (omssaName != null) {
                        refPtm = ptmFactory.getPTM(omssaName);
                        if (refPtm != PTMFactory.unknownPTM) {
                            refMass = refPtm.getMass();
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } else {
            refMass = refPtm.getMass();
        }
        return refMass;
    }

    /**
     * Returns the number of PSMs processed.
     *
     * @return the number of PSMs processed
     */
    public long getnPSMs() {
        return nPSMs;
    }

    /**
     * Returns the number of secondary hits processed.
     *
     * @return the number of secondary hits processed
     */
    public long getnSecondary() {
        return nSecondary;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters.
     *
     * @return the number of PSMs which did not pass the import filters
     */
    public int getPsmsRejected() {
        return psmsRejected;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * protein issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * protein issue
     */
    public int getProteinIssue() {
        return proteinIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * peptide issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * peptide issue
     */
    public int getPeptideIssue() {
        return peptideIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * precursor issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * precursor issue
     */
    public int getPrecursorIssue() {
        return precursorIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * PTM issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * PTM issue
     */
    public int getPtmIssue() {
        return ptmIssue;
    }

    /**
     * Returns the number of PSMs where a protein was missing.
     *
     * @return the number of PSMs where a protein was missing
     */
    public int getMissingProteins() {
        return missingProteins;
    }

    /**
     * Returns the number of PSMs retained after filtering.
     *
     * @return the number of PSMs retained after filtering
     */
    public int getnRetained() {
        return nRetained;
    }

    /**
     * Returns the different charges found.
     *
     * @return the different charges found
     */
    public HashSet<Integer> getCharges() {
        return charges;
    }

    /**
     * Returns the maximal peptide mass error found in ppm.
     *
     * @return the maximal peptide mass error found in ppm
     */
    public double getMaxPeptideErrorPpm() {
        return maxPeptideErrorPpm;
    }

    /**
     * Returns the maximal peptide mass error found in Da.
     *
     * @return the maximal peptide mass error found in Da
     */
    public double getMaxPeptideErrorDa() {
        return maxPeptideErrorDa;
    }

    /**
     * Returns the maximal tag mass error found in ppm.
     *
     * @return the maximal tag mass error found in ppm
     */
    public double getMaxTagErrorPpm() {
        return maxTagErrorPpm;
    }

    /**
     * Returns the maximal tag mass error found in Da.
     *
     * @return the maximal tag mass error found in Da
     */
    public double getMaxTagErrorDa() {
        return maxTagErrorDa;
    }

    /**
     * Private runnable to import PSMs.
     */
    private class PsmImporterRunnable implements Runnable {

        /**
         * The spectrum match to import.
         */
        private SpectrumMatch spectrumMatch;

        /**
         * The waiting handler.
         */
        private WaitingHandler waitingHandler;

        /**
         * The peptide spectrum annotator used to annotate spectra for this
         * thread.
         */
        private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        /**
         * Constructor.
         *
         * @param spectrumMatch the match to import
         * @param waitingHandler a waiting handler to display progress and allow
         * canceling the process
         */
        public PsmImporterRunnable(SpectrumMatch spectrumMatch, WaitingHandler waitingHandler) {
            this.spectrumMatch = spectrumMatch;
            this.waitingHandler = waitingHandler;
        }

        @Override
        public void run() {

            try {
                if (!waitingHandler.isRunCanceled()) {
                    importPsm(spectrumMatch, peptideSpectrumAnnotator, waitingHandler);
                }
            } catch (Exception e) {
                if (!waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                    waitingHandler.setRunCanceled();
                }
            }
        }
    }
}
