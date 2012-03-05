package eu.isas.peptideshaker.pride;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Map linking user modification names to MOD PSI CV terms
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmToPrideMap implements Serializable {

    /**
     * Serialization number for backward compatibility.
     */
    static final long serialVersionUID = 5849284645982876968L;
    /**
     * The name of the file to save.
     */
    public static String fileName = "modMap.cus";
    /**
     * Map of the cv terms indexed by modification user name.
     */
    private HashMap<String, CvTerm> modToCvMap = new HashMap<String, CvTerm>();

    /**
     * Constructor.
     */
    public PtmToPrideMap() {
    }

    /**
     * Returns the cv term corresponding to the given PTM name. Null if not
     * found.
     *
     * @param ptmName the ptm name
     * @return the corresponding cv term
     */
    public CvTerm getCVTerm(String ptmName) {
        return modToCvMap.get(ptmName);
    }

    /**
     * Puts a new mapping in the map. If the modification name is already loaded
     * it will be silently overwritten.
     *
     * @param modName the modification name
     * @param cvTerm the corresponding cvTerm
     */
    public void putCVTerm(String modName, CvTerm cvTerm) {
        modToCvMap.put(modName, cvTerm);
    }

    /**
     * Returns the default cvTerm of a modification when it exists.
     *
     * @param ptmName the ptm Name according to the xml file
     * @return a default CV term
     */
    public static CvTerm getDefaultCVTerm(String ptmName) {
        if (ptmName.equalsIgnoreCase("methylation of K")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.015650");
        } else if (ptmName.equalsIgnoreCase("oxidation of M")) {
            return new CvTerm("MOD", "MOD:01047", "Oxidation", "15.994915");
        } else if (ptmName.equalsIgnoreCase("carboxymethyl C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carbamidomethyl C")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("deamidation of N and Q")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("propionamide C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of Y")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("M cleavage from protein n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("acetylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:00394", "acetylated residue", "42.010565");
        } else if (ptmName.equalsIgnoreCase("methylation of protein n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-methylation of protein n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("beta methythiolation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methylation of Q")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-methylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methylation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methylation of E")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methylation of peptide c-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of E")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of peptide c-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("n-formyl met addition")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("2-amino-3-oxo-butanoic acid T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("acetylation of K")) {
            return new CvTerm("MOD", "MOD:00394", "acetylated residue", "42.010565");
        } else if (ptmName.equalsIgnoreCase("amidation of peptide c-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("beta-methylthiolation of D (duplicate of 13)")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of H")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of E")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carbamylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carbamylation of n-term peptide")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("citrullination of R")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of C to cysteic acid")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("di-iodination of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("di-methylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("di-methylation of R")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("di-methylation of peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of F to dihydroxyphenylalanine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("gammathiopropionylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("gammathiopropionylation of peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("farnesylation of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("formylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("formylation of peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of W to formylkynurenin")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("fluorophenylalanine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("beta-carboxylation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("gamma-carboxylation of E")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("geranyl-geranyl")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("glucuronylation of protein n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("glutathione disulfide")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("ubiquitinylation residue")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("guanidination of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of H to N")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of H to D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("homoserine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("homoserine lactone")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of W to hydroxykynurenin")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of N")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of P")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of F")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("hydroxylation of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iodination of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of W to kynurenin")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("lipoyl K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl ester of peptide c-term (duplicate of 18)")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl ester of D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl ester of E (duplicate of 17)")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl ester of S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl ester of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl H")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl N")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methylation of peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("methyl R")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("myristoleylation of G")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("myristoyl-4H of G")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("myristoylation of peptide n-term G")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("myristoylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("formylation of protein n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("NEM C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("NIPCAM")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of W to nitro")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of Y to nitro")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("O18 on peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("di-O18 on peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of H")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of W")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphopantetheine S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoylation of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoylation of S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoylation of T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S with prompt loss")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T with prompt loss")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with prompt loss on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on D")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on H")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("propionyl light K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("propionyl light on peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("propionyl heavy K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("propionyl heavy peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("pyridyl K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("pyridyl peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("pyro-cmC")) {
            return new CvTerm("MOD", "MOD:00040", "2-pyrrolidone-5-carboxylic acid (Gln)", "-17.026549");
        } else if (ptmName.equalsIgnoreCase("pyro-glu from n-term E")) {
            return new CvTerm("MOD", "MOD:00420", "2-pyrrolidone-5-carboxylic acid (Gln)", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("pyro-glu from n-term Q")) {
            return new CvTerm("MOD", "MOD:00040", "2-pyrrolidone-5-carboxylic acid (Gln)", "-17.026549");
        } else if (ptmName.equalsIgnoreCase("oxidation of P to pyroglutamic acid")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("s-pyridylethylation of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("SeMet")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("sulfation of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("sulphone of M")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-iodination of Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("tri-methylation of R")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("n-acyl diglyceride cysteine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("ICAT light")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("ICAT heavy")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("CAMthiopropanoyl K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S with ETD loss")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T with ETD loss")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("heavy arginine-13C6")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("heavy arginine-13C6-15N4")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("heavy lysine-13C6")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("PNGasF in O18 water")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("beta elimination of S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("beta elimination of T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of C to sulfinic acid")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("arginine to ornithine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("dehydro of S and T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxykynurenin of W")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("sumoylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("MMTS on C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("heavy lysine - 2H4")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("heavy lysine - 13C6 15N2")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Asparagine HexNAc")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Asparagine dHexHexNAc")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Serine HexNAc")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Threonine HexNAc")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of S")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of T")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("CHD2-di-methylation of K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("CHD2-di-methylation of peptide n-term")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Maleimide-PEO2-Biotin of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("phosphorylation of H")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of C")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("oxidation of Y (duplicate of 64)")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("Uniblue A on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("deamidation of N")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("trideuteration of L (SILAC)")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("TMT duplex on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("TMT duplex on n-term peptide")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("TMT 6-plex on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("TMT 6-plex on n-term peptide")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on nterm")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on K")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on Y")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("selenocysteine")) {
            return null;
        } else if (ptmName.equalsIgnoreCase("carboxymethylated selenocysteine")) {
            return null;
        }
        return null;
    }
}
