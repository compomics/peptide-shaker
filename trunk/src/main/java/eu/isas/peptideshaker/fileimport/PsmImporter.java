package eu.isas.peptideshaker.fileimport;

import com.compomics.mascotdatfile.util.io.MascotIdfileReader;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationAlgorithmParameter;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.protein_inference.proteintree.ProteinTreeComponentsFactory;
import com.compomics.util.experiment.identification.ptm.PtmSiteMapping;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MsAmandaIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.io.identifications.idfilereaders.PepxmlIdfileReader;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.memory.MemoryConsumptionStatus;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import de.proteinms.omxparser.util.OMSSAIdfileReader;
import de.proteinms.xtandemparser.parser.XTandemIdfileReader;
import static eu.isas.peptideshaker.fileimport.FileImporter.ptmMassTolerance;
import eu.isas.peptideshaker.scoring.InputMap;
import java.io.File;
import java.io.FileNotFoundException;
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
     * The id file reader where the PSMs are from.
     */
    private IdfileReader fileReader;
    /**
     * The identification file where the PSMs are from.
     */
    private File idFile;
    /**
     * List of ignored OMSSA modifications.
     */
    private ArrayList<Integer> ignoredOMSSAModifications = new ArrayList<Integer>();
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
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    public void importPsms(LinkedList<SpectrumMatch> idFileSpectrumMatches, int nThreads, WaitingHandler waitingHandler)
            throws IOException, SQLException, FileNotFoundException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {
        if (nThreads == 1) {
            importPsmsSingleThread(idFileSpectrumMatches, waitingHandler);
        } else {
            importPsmsMultipleThreads(idFileSpectrumMatches, nThreads, waitingHandler);
        }
    }

    /**
     * Imports PSMs using multiple threads.
     *
     * @param idFileSpectrumMatches the PSMs to import
     * @param nThreads the number of threads to use
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    public void importPsmsMultipleThreads(LinkedList<SpectrumMatch> idFileSpectrumMatches, int nThreads, WaitingHandler waitingHandler)
            throws IOException, SQLException, FileNotFoundException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

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
        if (!pool.awaitTermination(12, TimeUnit.HOURS)) {
            throw new InterruptedException("PSM import timed out. Please contact the developers.");
        }
    }

    /**
     * Imports PSMs using a single thread
     *
     * @param idFileSpectrumMatches the PSMs to import
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    private void importPsmsSingleThread(LinkedList<SpectrumMatch> idFileSpectrumMatches, WaitingHandler waitingHandler)
            throws IOException, SQLException, FileNotFoundException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        while (!idFileSpectrumMatches.isEmpty()) {
            SpectrumMatch match = idFileSpectrumMatches.pollLast();
            importPsm(match, waitingHandler);
        }
    }

    /**
     * Imports a PSM.
     *
     * @param spectrumMatch the spectrum match to import
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     *
     * @throws IOException
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws InterruptedException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    private void importPsm(SpectrumMatch spectrumMatch, WaitingHandler waitingHandler)
            throws IOException, SQLException, FileNotFoundException, InterruptedException, ClassNotFoundException, MzMLUnmarshallerException {

        IdFilter idFilter = identificationParameters.getIdFilter();
        SequenceMatchingPreferences sequenceMatchingPreferences = identificationParameters.getSequenceMatchingPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        // free memory if needed
        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !peptideShakerCache.isEmpty()) {
            peptideShakerCache.reduceMemoryConsumption(0.5, null);
        }
        // free memory if needed
        if (MemoryConsumptionStatus.memoryUsed() > 0.9 && !ProteinTreeComponentsFactory.getInstance().getCache().isEmpty()) {
            ProteinTreeComponentsFactory.getInstance().getCache().reduceMemoryConsumption(0.5, null);
        }
        if (!MemoryConsumptionStatus.halfGbFree() && sequenceFactory.getNodesInCache() > 0) {
            sequenceFactory.reduceNodeCacheSize(0.5);
        }

        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();
        nPSMs++;
        nSecondary += spectrumMatch.getAllAssumptions().size() - 1;

        String spectrumKey = spectrumMatch.getKey();
        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        for (int advocateId : assumptions.keySet()) {

            if (advocateId == Advocate.xtandem.getIndex()) {
                verifyXTandemPtms();
            }

            for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions()) {
                if (assumption instanceof PeptideAssumption) {
                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                    if (!idFilter.validatePeptide(peptideAssumption.getPeptide(), sequenceMatchingPreferences)) {
                        spectrumMatch.removeAssumption(assumption);
                        peptideIssue++;
                    }
                }
            }

            if (!spectrumMatch.hasAssumption(advocateId)) {
                psmsRejected++;
            } else {

                // Check whether there is a potential first hit which does not belong to the target and the decoy database
                ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(advocateId).keySet());
                Collections.sort(eValues);

                for (Double eValue : eValues) {

                    ArrayList<SpectrumIdentificationAssumption> tempAssumptions
                            = new ArrayList<SpectrumIdentificationAssumption>(spectrumMatch.getAllAssumptions(advocateId).get(eValue));

                    for (SpectrumIdentificationAssumption assumption : tempAssumptions) {

                        if (assumption instanceof PeptideAssumption) {

                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            Peptide peptide = peptideAssumption.getPeptide();
                            String peptideSequence = peptide.getSequence();

                            // map the algorithm specific modifications on utilities modifications
                            // If there are not enough sites to put them all on the sequence, add an unknown modification
                            // Note: this needs to be done for tag based assumptions as well since the protein mapping can return erroneous modifications for some pattern based PTMs
                            ModificationProfile modificationProfile = searchParameters.getModificationProfile();

                            // set the matching type to amino acid for the fixed ptms
                            SequenceMatchingPreferences tempSequenceMatchingPreferences = SequenceMatchingPreferences.getDefaultSequenceMatching(searchParameters); // @TODO: is there a simpler way?
                            tempSequenceMatchingPreferences.setSequenceMatchingType(SequenceMatchingPreferences.MatchingType.aminoAcid);

                            boolean fixedPtmIssue = false;
                            try {
                                ptmFactory.checkFixedModifications(modificationProfile, peptide, tempSequenceMatchingPreferences);
                            } catch (IllegalArgumentException e) {
                                if (idFilter.removeUnknownPTMs()) {
                                    // Exclude peptides with aberrant PTM mapping
                                    System.out.println(e.getMessage());
                                    spectrumMatch.removeAssumption(assumption);
                                    ptmIssue++;
                                    fixedPtmIssue = true;
                                } else {
                                    throw e;
                                }
                            }

                            if (!fixedPtmIssue) {

                                HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<Integer, ArrayList<String>>();
                                HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<ModificationMatch, ArrayList<String>>();

                                for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                    HashMap<Integer, ArrayList<String>> tempNames = new HashMap<Integer, ArrayList<String>>();
                                    if (modMatch.isVariable()) {
                                        String sePTM = modMatch.getTheoreticPtm();
                                        if (fileReader instanceof OMSSAIdfileReader) {
                                            Integer omssaIndex = null;
                                            try {
                                                omssaIndex = new Integer(sePTM);
                                            } catch (Exception e) {
                                                waitingHandler.appendReport("Impossible to parse OMSSA modification " + sePTM + ".", true, true);
                                            }
                                            if (omssaIndex != null) {
                                                String omssaName = modificationProfile.getModification(omssaIndex);
                                                if (omssaName == null) {
                                                    if (!ignoredOMSSAModifications.contains(omssaIndex)) {
                                                        waitingHandler.appendReport("Impossible to find OMSSA modification of index "
                                                                + omssaIndex + ". The corresponding peptides will be ignored.", true, true);
                                                        ignoredOMSSAModifications.add(omssaIndex);
                                                    }
                                                    omssaName = PTMFactory.unknownPTM.getName();
                                                }
                                                tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, omssaName, ptmMassTolerance, sequenceMatchingPreferences);
                                            }
                                        } else if (fileReader instanceof MascotIdfileReader
                                                || fileReader instanceof XTandemIdfileReader
                                                || fileReader instanceof MsAmandaIdfileReader
                                                || fileReader instanceof MzIdentMLIdfileReader
                                                || fileReader instanceof PepxmlIdfileReader) {
                                            String[] parsedName = sePTM.split("@");
                                            double seMass = 0;
                                            try {
                                                seMass = new Double(parsedName[0]);
                                            } catch (Exception e) {
                                                throw new IllegalArgumentException("Impossible to parse \'" + sePTM + "\' as a tagged modification.\n"
                                                        + "Error encountered in peptide " + peptideSequence + " spectrum " + spectrumTitle + " in spectrum file "
                                                        + spectrumFileName + ".\n" + "Identification file: " + idFile.getName());
                                            }
                                            tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, seMass, ptmMassTolerance, sequenceMatchingPreferences);
                                        } else if (fileReader instanceof DirecTagIdfileReader) {
                                            PTM ptm = ptmFactory.getPTM(sePTM);
                                            if (ptm == PTMFactory.unknownPTM) {
                                                throw new IllegalArgumentException("PTM not recognized spectrum " + spectrumTitle + " of file " + spectrumFileName + ".");
                                            }
                                            tempNames = ptmFactory.getExpectedPTMs(modificationProfile, peptide, ptm.getMass(), ptmMassTolerance, sequenceMatchingPreferences);
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

                                // If a terminal modification cannot be elsewhere lock the terminus
                                ModificationMatch nTermModification = null;
                                for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                                    if (modMatch.isVariable() && !modMatch.getTheoreticPtm().equals(PTMFactory.unknownPTM.getName())) {
                                        double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                        int modSite = modMatch.getModificationSite();
                                        if (modSite == 1) {
                                            ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                            if (expectedNamesAtSite != null) {
                                                ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                                for (String ptmName : expectedNamesAtSite) {
                                                    PTM ptm = ptmFactory.getPTM(ptmName);
                                                    if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()) {
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
                                        double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                        int modSite = modMatch.getModificationSite();
                                        if (modSite == peptideSequence.length()) {
                                            ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                            if (expectedNamesAtSite != null) {
                                                ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                                for (String ptmName : expectedNamesAtSite) {
                                                    PTM ptm = ptmFactory.getPTM(ptmName);
                                                    if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()) {
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
                                        double refMass = getRefMass(modMatch.getTheoreticPtm(), modificationProfile);
                                        int modSite = modMatch.getModificationSite();
                                        boolean terminal = false;
                                        ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);
                                        if (expectedNamesAtSite != null) {
                                            ArrayList<String> filteredNamesAtSite = new ArrayList<String>(expectedNamesAtSite.size());
                                            ArrayList<String> modificationAtSite = siteToPtmMap.get(modSite);
                                            for (String ptmName : expectedNamesAtSite) {
                                                PTM ptm = ptmFactory.getPTM(ptmName);
                                                if (Math.abs(ptm.getMass() - refMass) < searchParameters.getFragmentIonAccuracy()
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
                                                            if (massError <= searchParameters.getFragmentIonAccuracy()
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
                                                } else if (modSite == peptideSequence.length()) {
                                                    Double minDiff = null;
                                                    String bestPtmName = null;
                                                    for (String modName : filteredNamesAtSite) {
                                                        PTM ptm = ptmFactory.getPTM(modName);
                                                        if (ptm.isCTerm() && cTermModification == null) {
                                                            double massError = Math.abs(refMass - ptm.getMass());
                                                            if (massError <= searchParameters.getFragmentIonAccuracy()
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
                                                            if (massError <= searchParameters.getFragmentIonAccuracy()
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

                                if (idFilter.validateModifications(peptide, sequenceMatchingPreferences, searchParameters.getModificationProfile())) {
                                    // Estimate the theoretic mass with the new modifications
                                    peptide.estimateTheoreticMass();
                                    if (!idFilter.validatePrecursor(peptideAssumption, spectrumKey, spectrumFactory)) {
                                        spectrumMatch.removeAssumption(assumption);
                                        precursorIssue++;
                                    } else if (!idFilter.validateProteins(peptideAssumption.getPeptide(), sequenceMatchingPreferences)) {
                                        // Check whether there is a potential first hit which does not belong to both the target and the decoy database
                                        spectrumMatch.removeAssumption(assumption);
                                        proteinIssue++;
                                    }
                                } else {
                                    spectrumMatch.removeAssumption(assumption);
                                    ptmIssue++;
                                }
                            }
                        }
                    }
                }

                if (spectrumMatch.hasAssumption(advocateId)) {
                    // try to find the best peptide hit
                    PeptideAssumption firstPeptideHit = null;
                    eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(advocateId).keySet());
                    Collections.sort(eValues);

                    for (Double eValue : eValues) {
                        for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(advocateId).get(eValue)) {
                            if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                checkPeptidesMassErrorsAndCharges(spectrumKey, peptideAssumption);
                                if (firstPeptideHit == null) {
                                    firstPeptideHit = peptideAssumption;
                                    if (!processingPreferences.isScoringNeeded(advocateId)) {
                                        inputMap.addEntry(advocateId, spectrumFileName, firstPeptideHit.getScore(), firstPeptideHit.getPeptide().isDecoy(sequenceMatchingPreferences));
                                    }
                                    identification.addSpectrumMatch(spectrumMatch);
                                }
                                nRetained++;
                            }
                        }
                        if (firstPeptideHit != null) {
                            break;
                        }
                    }
                    if (firstPeptideHit == null) {
                        // Try to find the best tag hit
                        TagAssumption firstTagHit = null;
                        for (Double eValue : eValues) {
                            for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(advocateId).get(eValue)) {
                                if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    firstTagHit = tagAssumption;
                                    checkTagMassErrorsAndCharge(spectrumKey, tagAssumption);
                                    identification.addSpectrumMatch(spectrumMatch);
                                    nRetained++;
                                    break;
                                }
                            }
                            if (firstTagHit != null) {
                                break;
                            }
                        }
                    }
                } else {
                    psmsRejected++;
                }

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.setSecondaryProgressCounter(++progress);
            }
        }
    }

    /**
     * Saves the peptide maximal mass error and found charge.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws MzMLUnmarshallerException
     */
    private synchronized void checkPeptidesMassErrorsAndCharges(String spectrumKey, PeptideAssumption peptideAssumption)
            throws IOException, InterruptedException, SQLException, ClassNotFoundException, MzMLUnmarshallerException {

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        double error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, true));

        if (error > maxPeptideErrorPpm) {
            maxPeptideErrorPpm = error;
        }

        error = Math.abs(peptideAssumption.getDeltaMass(precursorMz, false));

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
     * @throws MzMLUnmarshallerException
     * @throws IOException
     */
    private synchronized void checkTagMassErrorsAndCharge(String spectrumKey, TagAssumption tagAssumption) throws MzMLUnmarshallerException, IOException {

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);
        double error = Math.abs(tagAssumption.getDeltaMass(precursorMz, true));

        if (error > maxTagErrorPpm) {
            maxTagErrorPpm = error;
        }

        error = Math.abs(tagAssumption.getDeltaMass(precursorMz, false));

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
            ModificationProfile modificationProfile = searchParameters.getModificationProfile();
            IdentificationAlgorithmParameter algorithmParameter = searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex());
            if (algorithmParameter != null) {
                XtandemParameters xtandemParameters = (XtandemParameters) algorithmParameter;
                if (xtandemParameters.isProteinQuickAcetyl() && !modificationProfile.contains("acetylation of protein n-term")) {
                    PTM ptm = PTMFactory.getInstance().getPTM("acetylation of protein n-term");
                    modificationProfile.addVariableModification(ptm);
                }
                String[] pyroMods = {"pyro-cmc", "pyro-glu from n-term e", "pyro-glu from n-term q"};
                if (xtandemParameters.isQuickPyrolidone()) {
                    for (String ptmName : pyroMods) {
                        if (!modificationProfile.getVariableModifications().contains(ptmName)) {
                            PTM ptm = PTMFactory.getInstance().getPTM(ptmName);
                            modificationProfile.addVariableModification(ptm);
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
     * @param modificationProfile the modification profile of the identification
     *
     * @return the mass of the PTM
     */
    private double getRefMass(String sePtmName, ModificationProfile modificationProfile) {
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
                    String omssaName = modificationProfile.getModification(omssaIndex);
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
         * The spectrum match to import
         */
        private SpectrumMatch spectrumMatch;

        /**
         * The waiting handler
         */
        private WaitingHandler waitingHandler;

        /**
         * Constructor
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
                    importPsm(spectrumMatch, waitingHandler);
                }
            } catch (Exception e) {
                if (!waitingHandler.isRunCanceled()) {
                    exceptionHandler.catchException(e);
                }
            }
        }
    }
}
