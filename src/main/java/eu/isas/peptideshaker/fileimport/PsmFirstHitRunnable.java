package eu.isas.peptideshaker.fileimport;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class tries to find a best hit per search engine for each spectrum
 * match.
 *
 * @author Marc Vaudel
 */
public class PsmFirstHitRunnable implements Runnable {

    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;
    /**
     * The spectrum provider.
     */
    private final SpectrumProvider spectrumProvider;
    /**
     * The input map.
     */
    private final InputMap inputMap;
    /**
     * The spectrum annotator to use for peptides.
     */
    private final PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * The best match selection module.
     */
    private final BestMatchSelection bestMatchSelection;
    /**
     * Iterator for the spectrum matches to import.
     */
    private final ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchQueue;
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
     * The number of retained first hits.
     */
    private int nRetained = 0;
    /**
     * The number of peptides where no protein was found.
     */
    private int missingProteins = 0;
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
    private final HashSet<Integer> charges = new HashSet<>();
    /**
     * The waiting handler to display feedback to the user.
     */
    private final WaitingHandler waitingHandler;
    /**
     * Exception handler.
     */
    private final ExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param spectrumMatchQueue The spectrum matches iterator to use.
     * @param identificationParameters The identification parameters.
     * @param sequenceProvider The protein sequence provider.
     * @param spectrumProvider The spectrum provider.
     * @param inputMap The input scores map.
     * @param proteinCount The map of protein occurrence.
     * @param waitingHandler The waiting handler to display feedback to the
     * user.
     * @param exceptionHandler The handler of exceptions.
     */
    public PsmFirstHitRunnable(
            ConcurrentLinkedQueue<SpectrumMatch> spectrumMatchQueue,
            IdentificationParameters identificationParameters,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            InputMap inputMap,
            HashMap<String, Integer> proteinCount,
            WaitingHandler waitingHandler,
            ExceptionHandler exceptionHandler
    ) {

        this.spectrumMatchQueue = spectrumMatchQueue;
        this.identificationParameters = identificationParameters;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;
        this.inputMap = inputMap;
        this.waitingHandler = waitingHandler;
        this.exceptionHandler = exceptionHandler;

        this.bestMatchSelection = new BestMatchSelection(
                proteinCount,
                sequenceProvider,
                spectrumProvider,
                identificationParameters,
                peptideSpectrumAnnotator
        );

    }

    @Override
    public void run() {

        try {

            SpectrumMatch spectrumMatch;
            while ((spectrumMatch = spectrumMatchQueue.poll()) != null) {

                processPsm(spectrumMatch);

                if (waitingHandler.isRunCanceled()) {
                    return;
                }

                waitingHandler.increaseSecondaryProgressCounter();

            }

        } catch (Exception e) {

            waitingHandler.setRunCanceled();
            exceptionHandler.catchException(e);

        }
    }

    /**
     * Selects the first hit for the given spectrum match.
     *
     * @param spectrumMatch
     */
    private void processPsm(
            SpectrumMatch spectrumMatch
    ) {

        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptions = spectrumMatch.getPeptideAssumptionsMap();

        for (Map.Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : peptideAssumptions.entrySet()) {

            int advocateId = entry.getKey();

            TreeMap<Double, ArrayList<PeptideAssumption>> assumptionsForAdvocate = entry.getValue();

            PeptideAssumption firstPeptideHit = null;
            PeptideAssumption firstPeptideHitNoProtein = null;
            TagAssumption firstTagHit = null;

            if (!assumptionsForAdvocate.isEmpty()) {

                for (Map.Entry<Double, ArrayList<PeptideAssumption>> entry1 : assumptionsForAdvocate.entrySet()) {

                    ArrayList<PeptideAssumption> firstHits = new ArrayList<>(1);
                    ArrayList<PeptideAssumption> firstHitsNoProteins = new ArrayList<>(1);

                    for (PeptideAssumption peptideAssumption : entry1.getValue()) {

                        Peptide peptide = peptideAssumption.getPeptide();
                        boolean filterPassed = true;

                        if (!peptideAssumptionFilter.validatePeptide(
                                peptide,
                                sequenceMatchingPreferences,
                                searchParameters.getDigestionParameters()
                        )) {

                            filterPassed = false;
                            peptideIssue++;

                        } else if (!peptideAssumptionFilter.validatePrecursor(
                                peptideAssumption,
                                spectrumFile,
                                spectrumTitle,
                                spectrumProvider,
                                searchParameters
                        )) {

                            filterPassed = false;
                            precursorIssue++;

                        } else if (!peptideAssumptionFilter.validateProteins(
                                peptide,
                                sequenceMatchingPreferences,
                                sequenceProvider
                        )) {

                            filterPassed = false;
                            proteinIssue++;

                        } else {

                            if (peptide.getProteinMapping().isEmpty()) {

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

                    if (!firstHits.isEmpty()) {

                        firstPeptideHit = bestMatchSelection.getBestMatch(
                                spectrumFile,
                                spectrumTitle,
                                firstHits,
                                true
                        );

                    }
                    if (firstPeptideHit != null) {

                        inputMap.addEntry(
                                advocateId,
                                spectrumFile,
                                firstPeptideHit.getScore(),
                                PeptideUtils.isDecoy(
                                        firstPeptideHit.getPeptide(),
                                        sequenceProvider
                                )
                        );
                        nRetained++;
                        break;

                    } else if (!firstHitsNoProteins.isEmpty()) {

                        // See if a peptide without protein can be a best match
                        firstPeptideHit = bestMatchSelection.getBestMatch(
                                spectrumFile,
                                spectrumTitle,
                                firstHits,
                                true
                        );
                    }
                }

                if (firstPeptideHit != null) {

                    savePeptidesMassErrorsAndCharges(
                            spectrumFile,
                            spectrumTitle,
                            firstPeptideHit
                    );

                } else {

                    // Check if a peptide with no protein can be a good candidate
                    if (firstPeptideHitNoProtein != null) {

                        savePeptidesMassErrorsAndCharges(
                                spectrumFile,
                                spectrumTitle,
                                firstPeptideHitNoProtein
                        );

                    } else {

                        // Try to find the best tag hit
                        TreeMap<Double, ArrayList<TagAssumption>> tagsForAdvocate = spectrumMatch.getAllTagAssumptions(advocateId);

                        if (tagsForAdvocate != null) {

                            firstTagHit = tagsForAdvocate.keySet().stream()
                                    .sorted()
                                    .flatMap(
                                            score -> tagsForAdvocate.get(score).stream()
                                    )
                                    .findFirst()
                                    .get();
                            checkTagMassErrorsAndCharge(
                                    spectrumFile,
                                    spectrumTitle,
                                    firstTagHit
                            );
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
     * Saves the peptide maximal mass error and found charge.
     *
     * @param spectrumFile The file name of the spectrum.
     * @param spectrumTitle The title of the spectrum.
     * @param peptideAssumption The peptide assumption.
     */
    private void savePeptidesMassErrorsAndCharges(
            String spectrumFile,
            String spectrumTitle,
            PeptideAssumption peptideAssumption
    ) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumProvider.getPrecursorMz(
                spectrumFile,
                spectrumTitle
        );

        maxPeptideErrorPpm = Math.max(
                maxPeptideErrorPpm,
                Math.abs(
                        peptideAssumption.getDeltaMass(
                                precursorMz,
                                true,
                                searchParameters.getMinIsotopicCorrection(),
                                searchParameters.getMaxIsotopicCorrection()
                        )
                )
        );

        maxPeptideErrorDa = Math.max(
                maxPeptideErrorDa,
                Math.abs(
                        peptideAssumption.getDeltaMass(
                                precursorMz,
                                false,
                                searchParameters.getMinIsotopicCorrection(),
                                searchParameters.getMaxIsotopicCorrection()
                        )
                )
        );

        charges.add(peptideAssumption.getIdentificationCharge());
    }

    /**
     * Saves the maximal precursor error and charge.
     *
     * @param spectrumFile The file name of the spectrum.
     * @param spectrumTitle The title of the spectrum.
     * @param tagAssumption The tag assumption.
     */
    private void checkTagMassErrorsAndCharge(
            String spectrumFile,
            String spectrumTitle,
            TagAssumption tagAssumption
    ) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumProvider.getPrecursorMz(
                spectrumFile,
                spectrumTitle
        );

        maxTagErrorPpm = Math.max(
                maxTagErrorPpm,
                Math.abs(
                        tagAssumption.getDeltaMass(
                                precursorMz, 
                                true, 
                                searchParameters.getMinIsotopicCorrection(), 
                                searchParameters.getMaxIsotopicCorrection()
                        )
                )
        );

        maxTagErrorDa = Math.max(
                maxTagErrorDa,
                Math.abs(
                        tagAssumption.getDeltaMass(
                                precursorMz, 
                                false, 
                                searchParameters.getMinIsotopicCorrection(), 
                                searchParameters.getMaxIsotopicCorrection()
                        )
                )
        );

        charges.add(tagAssumption.getIdentificationCharge());

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
}
