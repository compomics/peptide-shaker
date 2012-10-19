package eu.isas.peptideshaker.utils;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.protein.Header;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class creates the display features needed for the GUI.
 *
 * @author Marc Vaudel
 */
public class DisplayFeaturesGenerator {

    /**
     * The main GUI instance.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Constructor.
     *
     * @param peptideShakerGUI the main instance of the GUI
     */
    public DisplayFeaturesGenerator(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
    }

    /**
     * Transforms the protein accession number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteinAccession the protein to get the database link for
     * @return the transformed accession number
     */
    public String addDatabaseLink(String proteinAccession) {

        String accessionNumberWithLink = proteinAccession;

        try {
            if (sequenceFactory.getHeader(proteinAccession) != null) {

                // try to find the database from the SequenceDatabase
                Header.DatabaseType databaseType = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                // create the database link
                if (databaseType != null) {

                    // @TODO: support more databases

                    if (databaseType == Header.DatabaseType.IPI || databaseType == Header.DatabaseType.UniProt) {
                        accessionNumberWithLink = "<html><a href=\"" + getUniProtAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else if (databaseType == Header.DatabaseType.NCBI) {
                        accessionNumberWithLink = "<html><a href=\"" + getNcbiAccessionLink(proteinAccession)
                                + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                + proteinAccession + "</font></a></html>";
                    } else {
                        // unknown database!
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }

        return accessionNumberWithLink;
    }

    /**
     * Transforms the protein accesion number into an HTML link to the
     * corresponding database. Note that this is a complete HTML with HTML and a
     * href tags, where the main use is to include it in the protein tables.
     *
     * @param proteins the list of proteins to get the database links for
     * @return the transformed accession number
     */
    public String addDatabaseLinks(ArrayList<String> proteins) {

        if (proteins.isEmpty()) {
            return "";
        }

        String accessionNumberWithLink = "<html>";

        for (int i = 0; i < proteins.size(); i++) {

            String proteinAccession = proteins.get(i);
            try {
                if (!SequenceFactory.isDecoy(proteins.get(i)) && sequenceFactory.getHeader(proteinAccession) != null) {

                    // try to find the database from the SequenceDatabase
                    DatabaseType database = sequenceFactory.getHeader(proteinAccession).getDatabaseType();

                    // create the database link
                    if (database != null) {

                        // @TODO: support more databases

                        if (database == DatabaseType.IPI || database == DatabaseType.UniProt) {
                            accessionNumberWithLink += "<a href=\"" + getUniProtAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else if (database == DatabaseType.NCBI) {
                            accessionNumberWithLink += "<a href=\"" + getNcbiAccessionLink(proteinAccession)
                                    + "\"><font color=\"" + peptideShakerGUI.getNotSelectedRowHtmlTagFontColor() + "\">"
                                    + proteinAccession + "</font></a>, ";
                        } else {
                            // unknown database!
                            accessionNumberWithLink += proteinAccession + ", ";
                        }
                    }
                } else {
                    accessionNumberWithLink += proteinAccession + ", ";
                }
            } catch (Exception e) {
                accessionNumberWithLink += proteinAccession + ", ";
            }
        }

        // remove the last ', '
        accessionNumberWithLink = accessionNumberWithLink.substring(0, accessionNumberWithLink.length() - 2);
        accessionNumberWithLink += "</html>";

        return accessionNumberWithLink;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://srs.ebi.ac.uk.
     *
     * @param proteinAccession the protein accession number
     * @param database the protein database
     * @return the protein accession web link
     */
    public String getSrsAccessionLink(String proteinAccession, String database) {
        return "http://srs.ebi.ac.uk/srsbin/cgi-bin/wgetz?-e+%5b" + database + "-AccNumber:" + proteinAccession + "%5d";
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.uniprot.org/uniprot.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getUniProtAccessionLink(String proteinAccession) {
        return "http://www.uniprot.org/uniprot/" + proteinAccession;
    }

    /**
     * Returns the protein accession number as a web link to the given protein
     * at http://www.ncbi.nlm.nih.gov/protein.
     *
     * @param proteinAccession the protein accession number
     * @return the protein accession web link
     */
    public String getNcbiAccessionLink(String proteinAccession) {
        return "http://www.ncbi.nlm.nih.gov/protein/" + proteinAccession;
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        String tooltip = "<html>";
        ArrayList<ModificationMatch> modifications = peptide.getModificationMatches();
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (int i = 0; i < modifications.size(); i++) {

            PTM ptm = ptmFactory.getPTM(modifications.get(i).getTheoreticPtm());

            if (ptm.getType() == PTM.MODAA && modifications.get(i).isVariable()) {

                int modSite = modifications.get(i).getModificationSite();
                String modName = modifications.get(i).getTheoreticPtm();
                char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(modifications.get(i).getTheoreticPtm());

                if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                    tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                            + affectedResidue
                            + "</span>"
                            + ": " + modName + "<br>";

                    alreadyAnnotated.add(modName + "_" + affectedResidue);
                }
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Returns the peptide with modification sites colored on the sequence.
     * Shall be used for peptides, not PSMs.
     *
     * @param peptideKey the peptide key
     * @param includeHtmlStartEndTag if true, html start and end tags are added
     * @return the colored peptide sequence
     */
    public String getColoredPeptideSequence(String peptideKey, boolean includeHtmlStartEndTag) {
        try {
            Identification identification = peptideShakerGUI.getIdentification();
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            PSPtmScores ptmScores = new PSPtmScores();
            ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
            if (ptmScores != null) {
                HashMap<Integer, ArrayList<String>> mainLocations = ptmScores.getMainModificationSites();
                HashMap<Integer, ArrayList<String>> secondaryLocations = ptmScores.getSecondaryModificationSites();
                return Peptide.getModifiedSequenceAsHtml(peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(),
                        includeHtmlStartEndTag, peptideMatch.getTheoreticPeptide(),
                        mainLocations, secondaryLocations);
            } else {
                return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                        peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), includeHtmlStartEndTag);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return "Error";
        }
    }
}
