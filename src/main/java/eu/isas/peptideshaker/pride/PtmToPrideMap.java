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
            return new CvTerm("MOD", "MOD:01328", "Carboxymethyl", "58.005479");
        } else if (ptmName.equalsIgnoreCase("carbamidomethyl C")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("deamidation of N and Q")) {
            return new CvTerm("MOD", "MOD:00400", "Deamidated", "0.984016");
        } else if (ptmName.equalsIgnoreCase("propionamide C")) {
            return new CvTerm("MOD", "MOD:00417", "Propionamide", "71.037114");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of Y")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("M cleavage from protein n-term")) {
            return new CvTerm("MOD", "MOD:01643", "Met-loss", "-131.040485");
        } else if (ptmName.equalsIgnoreCase("acetylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:00394", "acetylated residue", "42.010565");
        } else if (ptmName.equalsIgnoreCase("methylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.05650");
        } else if (ptmName.equalsIgnoreCase("tri-methylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:00420", "Trimethyl", "42.046950");
        } else if (ptmName.equalsIgnoreCase("beta methythiolation of D")) {
            return new CvTerm("MOD", "MOD:01153", "Methylthio", "45.987721");
        } else if (ptmName.equalsIgnoreCase("methylation of Q")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.05650");
        } else if (ptmName.equalsIgnoreCase("tri-methylation of K")) {
            return new CvTerm("MOD", "MOD:00430", "Trimethyl", "42.046950");
        } else if (ptmName.equalsIgnoreCase("methylation of D")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.05650");
        } else if (ptmName.equalsIgnoreCase("methylation of E")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.05650");
        } else if (ptmName.equalsIgnoreCase("methylation of peptide c-term")) {
            return new CvTerm("MOD", "MOD:01681", "Methylation", "14.05650");
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of D")) {
            return new CvTerm("MOD", "MOD:01241", "Methyl:2H(3)", "17.034480");
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of E")) {
            return new CvTerm("MOD", "MOD:01241", "Methyl:2H(3)", "17.034480");
        } else if (ptmName.equalsIgnoreCase("tri-deuteromethylation of peptide c-term")) {
            return new CvTerm("MOD", "MOD:01241", "Methyl:2H(3)", "17.034480");
        } else if (ptmName.equalsIgnoreCase("n-formyl met addition")) {
            return new CvTerm("MOD", "MOD:00030", "FormylMet", "159.035399");
        } else if (ptmName.equalsIgnoreCase("2-amino-3-oxo-butanoic acid T")) {
            return new CvTerm("MOD", "MOD:00370", "Didehydro", "-2.015650");
        } else if (ptmName.equalsIgnoreCase("acetylation of K")) {
            return new CvTerm("MOD", "MOD:00394", "acetylated residue", "42.010565");
        } else if (ptmName.equalsIgnoreCase("amidation of peptide c-term")) {
            return new CvTerm("MOD", "MOD:00674", "Amidated", "-0.984016");
        } else if (ptmName.equalsIgnoreCase("beta-methylthiolation of D (duplicate of 13)")) {
            return new CvTerm("MOD", "MOD:01153", "Methylthio", "45.987721");
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of K")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of H")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of D")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("carboxyamidomethylation of E")) {
            return new CvTerm("MOD", "MOD:010900", "Carbamidomethyl", "57.021464");
        } else if (ptmName.equalsIgnoreCase("carbamylation of K")) {
            return new CvTerm("MOD", "MOD:00398", "Carbamylation", "43.005814");
        } else if (ptmName.equalsIgnoreCase("carbamylation of n-term peptide")) {
            return new CvTerm("MOD", "MOD:00398", "Carbamylation", "43.005814");
        } else if (ptmName.equalsIgnoreCase("citrullination of R")) {
            return new CvTerm("MOD", "MOD:00219", "Citrullination", "0.984016");
        } else if (ptmName.equalsIgnoreCase("oxidation of C to cysteic acid")) {
            return new CvTerm("MOD", "MOD:00460", "Trioxidation", "47.984744");
        } else if (ptmName.equalsIgnoreCase("di-iodination of Y")) {
            return new CvTerm("MOD", "MOD:01140", "Diiodo", "251.793296");
        } else if (ptmName.equalsIgnoreCase("di-methylation of K")) {
            return new CvTerm("MOD", "MOD:00429", "Dimethyl", "28.031300");
        } else if (ptmName.equalsIgnoreCase("di-methylation of R")) {
            return new CvTerm("MOD", "MOD:00429", "Dimethyl", "28.031300");
        } else if (ptmName.equalsIgnoreCase("di-methylation of peptide n-term")) {
            return new CvTerm("MOD", "MOD:00429", "Dimethyl", "28.031300");
        } else if (ptmName.equalsIgnoreCase("oxidation of F to dihydroxyphenylalanine")) {
            return new CvTerm("MOD", "MOD:00267", "Dioxidation", "31.989829");
        } else if (ptmName.equalsIgnoreCase("gammathiopropionylation of K")) {
            return new CvTerm("MOD", "MOD:00497", "Thioacyl", "87.998285");
        } else if (ptmName.equalsIgnoreCase("gammathiopropionylation of peptide n-term")) {
            return new CvTerm("MOD", "MOD:00497", "Thioacyl", "87.998285");
        } else if (ptmName.equalsIgnoreCase("farnesylation of C")) {
            return new CvTerm("MOD", "MOD:00437", "Farnesyl", "204.187801");
        } else if (ptmName.equalsIgnoreCase("formylation of K")) {
            return new CvTerm("MOD", "MOD:00493", "Formyl", "27.994915");
        } else if (ptmName.equalsIgnoreCase("formylation of peptide n-term")) {
            return new CvTerm("MOD", "MOD:00493", "Formyl", "27.994915");
        } else if (ptmName.equalsIgnoreCase("oxidation of W to formylkynurenin")) {
            return new CvTerm("MOD", "MOD:00256", "Dioxidation", "31.989829");
        } else if (ptmName.equalsIgnoreCase("fluorophenylalanine")) {
            return new CvTerm("MOD", "MOD:01225", "Fluoro", "17.990578");
        } else if (ptmName.equalsIgnoreCase("beta-carboxylation of D")) {
            return new CvTerm("MOD", "MOD:01152", "Carboxy", "43.989829");
        } else if (ptmName.equalsIgnoreCase("gamma-carboxylation of E")) {
            return new CvTerm("MOD", "MOD:01152", "Carboxy", "43.989829");
        } else if (ptmName.equalsIgnoreCase("geranyl-geranyl")) {
            return new CvTerm("MOD", "MOD:00441", "GeranylGeranyl", "272.250401");
        } else if (ptmName.equalsIgnoreCase("glucuronylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:00447", "Glucuronyl", "176.032088");
        } else if (ptmName.equalsIgnoreCase("glutathione disulfide")) {
            return new CvTerm("MOD", "MOD:00234", "Glutathione", "305.068156");
        } else if (ptmName.equalsIgnoreCase("ubiquitinylation residue")) {
            return new CvTerm("MOD", "MOD:00492", "GlyGly", "114.042927");
        } else if (ptmName.equalsIgnoreCase("guanidination of K")) {
            return new CvTerm("MOD", "MOD:00445", "Guanidinyl", "42.021798");
        } else if (ptmName.equalsIgnoreCase("oxidation of H to N")) {
            return new CvTerm("MOD", "MOD:00775", "oxidation of H to N", "-23.015984");
        } else if (ptmName.equalsIgnoreCase("oxidation of H to D")) {
            return new CvTerm("MOD", "MOD:00776", "oxidation of H to D", "-22.031969");
        } else if (ptmName.equalsIgnoreCase("homoserine")) {
            return new CvTerm("MOD", "MOD:00403", "homoserine", "-29.992806");
        } else if (ptmName.equalsIgnoreCase("homoserine lactone")) {
            return new CvTerm("MOD", "MOD:00404", "homoserine lactone", "-48.003371");
        } else if (ptmName.equalsIgnoreCase("oxidation of W to hydroxykynurenin")) {
            return new CvTerm("MOD", "MOD:00463", "hydroxykynureninw", "19.989829");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of D")) {
            return new CvTerm("MOD", "MOD:00036", "hydroxylation of D", "15.994915");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of K")) {
            return new CvTerm("MOD", "MOD:01047", "hydroxylation of K", "15.994915");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of N")) {
            return new CvTerm("MOD", "MOD:01688", "hydroxylation of N", "15.994915");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of P")) {
            return new CvTerm("MOD", "MOD:01024", "hydroxylation of P", "15.994915");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of F")) {
            return new CvTerm("MOD", "MOD:01385", "hydroxylation of F", "15.994915");
        } else if (ptmName.equalsIgnoreCase("hydroxylation of Y")) {
            return new CvTerm("MOD", "MOD:00425", "hydroxylation of Y", "15.994915");
        } else if (ptmName.equalsIgnoreCase("iodination of Y")) {
            return new CvTerm("MOD", "MOD:00500", "iodination of Y", "125.896648");
        } else if (ptmName.equalsIgnoreCase("oxidation of W to kynurenin")) {
            return new CvTerm("MOD", "MOD:00462", "kynureninw", "3.994915");
        } else if (ptmName.equalsIgnoreCase("lipoyl K")) {
            return new CvTerm("MOD", "MOD:00127", "Lipoyl", "188.032956");
        } else if (ptmName.equalsIgnoreCase("methyl ester of peptide c-term (duplicate of 18)")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl ester of D")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl ester of E (duplicate of 17)")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl ester of S")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl ester of Y")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl C")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl H")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl N")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methylation of peptide n-term")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("methyl R")) {
            return new CvTerm("MOD", "MOD:01681", "Methyl", "14.015650");
        } else if (ptmName.equalsIgnoreCase("myristoleylation of G")) {
            return new CvTerm("MOD", "MOD:00503", "Myristoleyl", "208.182715");
        } else if (ptmName.equalsIgnoreCase("myristoyl-4H of G")) {
            return new CvTerm("MOD", "MOD:00504", "Myristoyl+Delta:H(-4)", "206.167065");
        } else if (ptmName.equalsIgnoreCase("myristoylation of peptide n-term G")) {
            return new CvTerm("MOD", "MOD:00438", "Myristoyl", "210.198366");
        } else if (ptmName.equalsIgnoreCase("myristoylation of K")) {
            return new CvTerm("MOD", "MOD:00438", "Myristoyl", "210.198366");
        } else if (ptmName.equalsIgnoreCase("formylation of protein n-term")) {
            return new CvTerm("MOD", "MOD:00493", "Formyl", "27.994915");
        } else if (ptmName.equalsIgnoreCase("NEM C")) {
            return new CvTerm("MOD", "MOD:00483", "Nethylmaleimide", "125.047679");
        } else if (ptmName.equalsIgnoreCase("NIPCAM")) {
            return new CvTerm("MOD", "MOD:00410", "NIPCAM", "99.068414");
        } else if (ptmName.equalsIgnoreCase("oxidation of W to nitro")) {
            return new CvTerm("MOD", "MOD:01352", "Nitro", "44.985078");
        } else if (ptmName.equalsIgnoreCase("oxidation of Y to nitro")) {
            return new CvTerm("MOD", "MOD:01352", "Nitro", "44.985078");
        } else if (ptmName.equalsIgnoreCase("O18 on peptide n-term")) {
            return new CvTerm("MOD", "MOD:01234", "O18 Labeling", "2.004246");
        } else if (ptmName.equalsIgnoreCase("di-O18 on peptide n-term")) {
            return new CvTerm("MOD", "MOD:00546", "O18 Labeling (2)", "4.00849");
        } else if (ptmName.equalsIgnoreCase("oxidation of H")) {
            return new CvTerm("MOD", "MOD:01047", "Oxidation", "15.994915");
        } else if (ptmName.equalsIgnoreCase("oxidation of W")) {
            return new CvTerm("MOD", "MOD:01047", "Oxidation", "15.994915");
        } else if (ptmName.equalsIgnoreCase("phosphopantetheine S")) {
            return new CvTerm("MOD", "MOD:00159", "Phosphopantetheine", "340.085794");
        } else if (ptmName.equalsIgnoreCase("palmitoylation of C")) {
            return new CvTerm("MOD", "MOD:00086", "Palmitoyl", "238.229666");
        } else if (ptmName.equalsIgnoreCase("palmitoylation of K")) {
            return new CvTerm("MOD", "MOD:00086", "Palmitoyl", "238.229666");
        } else if (ptmName.equalsIgnoreCase("palmitoylation of S")) {
            return new CvTerm("MOD", "MOD:00086", "Palmitoyl", "238.229666");
        } else if (ptmName.equalsIgnoreCase("palmitoylation of T")) {
            return new CvTerm("MOD", "MOD:00086", "Palmitoyl", "238.229666");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S with prompt loss")) {
            return new CvTerm("MOD", "MOD:00416", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T with prompt loss")) {
            return new CvTerm("MOD", "MOD:00416", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with prompt loss on Y")) {
            return new CvTerm("MOD", "MOD:00416", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on C")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on D")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on H")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("propionyl light K")) {
            return new CvTerm("MOD", "MOD:01232", "Propionyl", "56.026215");
        } else if (ptmName.equalsIgnoreCase("propionyl light on peptide n-term")) {
            return new CvTerm("MOD", "MOD:01232", "Propionyl", "56.026215");
        } else if (ptmName.equalsIgnoreCase("propionyl heavy K")) {
            return new CvTerm("MOD", "MOD:01231", "Propionyl:13C(3)", "59.036279");
        } else if (ptmName.equalsIgnoreCase("propionyl heavy peptide n-term")) {
            return new CvTerm("MOD", "MOD:01231", "Propionyl:13C(3)", "59.036279");
        } else if (ptmName.equalsIgnoreCase("pyridyl K")) {
            return new CvTerm("MOD", "MOD:00418", "Pyridylacetyl", "119.037114");
        } else if (ptmName.equalsIgnoreCase("pyridyl peptide n-term")) {
            return new CvTerm("MOD", "MOD:00418", "Pyridylacetyl", "119.037114");
        } else if (ptmName.equalsIgnoreCase("pyro-cmC")) {
            return new CvTerm("MOD", "MOD:00040", "2-pyrrolidone-5-carboxylic acid (Gln)", "-17.026549");
        } else if (ptmName.equalsIgnoreCase("pyro-glu from n-term E")) {
            return new CvTerm("MOD", "MOD:00420", "2-pyrrolidone-5-carboxylic acid (Gln)", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("pyro-glu from n-term Q")) {
            return new CvTerm("MOD", "MOD:00040", "2-pyrrolidone-5-carboxylic acid (Gln)", "-17.026549");
        } else if (ptmName.equalsIgnoreCase("oxidation of P to pyroglutamic acid")) {
            return new CvTerm("MOD", "MOD:00571", "pyroglutamicp", "13.979265");
        } else if (ptmName.equalsIgnoreCase("s-pyridylethylation of C")) {
            return new CvTerm("MOD", "MOD:00424", "Pyridylethyl", "105.057849");
        } else if (ptmName.equalsIgnoreCase("SeMet")) {
            return new CvTerm("MOD", "MOD:00530", "SeMet", "47.944449");
        } else if (ptmName.equalsIgnoreCase("sulfation of Y")) {
            return new CvTerm("MOD", "MOD:00695", "Sulfo", "79.956815");
        } else if (ptmName.equalsIgnoreCase("sulphone of M")) {
            return new CvTerm("MOD", "MOD:00256", "Dioxidation", "31.989829");
        } else if (ptmName.equalsIgnoreCase("tri-iodination of Y")) {
            return new CvTerm("MOD", "MOD:00502", "Dioxidation", "377.689944");
        } else if (ptmName.equalsIgnoreCase("tri-methylation of R")) {
            return new CvTerm("MOD", "MOD:00430", "Trimethyl", "42.046950");
        } else if (ptmName.equalsIgnoreCase("n-acyl diglyceride cysteine")) {
            return new CvTerm("MOD", "MOD:00444", "Tripalmitate", "788.725777");
        } else if (ptmName.equalsIgnoreCase("ICAT light")) {
            return new CvTerm("MOD", "MOD:00480", "ICAT light", "227.126991");
        } else if (ptmName.equalsIgnoreCase("ICAT heavy")) {
            return new CvTerm("MOD", "MOD:00481", "ICAT light", "236.157185");
        } else if (ptmName.equalsIgnoreCase("CAMthiopropanoyl K")) {
            return new CvTerm("MOD", "MOD:01695", "CAMthiopropanoyl", "145.019749");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on S")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation with neutral loss on T")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of S with ETD loss")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("phosphorylation of T with ETD loss")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("heavy arginine-13C6")) {
            return new CvTerm("MOD", "MOD:01331", "Label:13C(6)", "6.020129");
        } else if (ptmName.equalsIgnoreCase("heavy arginine-13C6-15N4")) {
            return new CvTerm("MOD", "MOD:00587", "Label:13C(6)15N(4)", "10.008269");
        } else if (ptmName.equalsIgnoreCase("heavy lysine-13C6")) {
            return new CvTerm("MOD", "MOD:01334", "Label:13C(6)15N(4)", "6.020129");
        } else if (ptmName.equalsIgnoreCase("PNGasF in O18 water")) {
            return new CvTerm("MOD", "MOD:00852", "Deamidated:18O(1)", "2.988261");
        } else if (ptmName.equalsIgnoreCase("beta elimination of S")) {
            return new CvTerm("MOD", "MOD:00189", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("beta elimination of T")) {
            return new CvTerm("MOD", "MOD:00190", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("oxidation of C to sulfinic acid")) {
            return new CvTerm("MOD", "MOD:00428", "Dioxidation", "31.989829");
        } else if (ptmName.equalsIgnoreCase("arginine to ornithine")) {
            return new CvTerm("MOD", "MOD:00796", "arginine to ornithine", "-42.021798");
        } else if (ptmName.equalsIgnoreCase("dehydro of S and T")) {
            return new CvTerm("MOD", "MOD:00190", "Dehydrated", "-18.010565");
        } else if (ptmName.equalsIgnoreCase("carboxykynurenin of W")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("sumoylation of K")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on nterm")) {
            return new CvTerm("MOD", "MOD:01486", "iTRAQ4plex114", "144.105918");
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on K")) {
            return new CvTerm("MOD", "MOD:01487", "iTRAQ4plex114", "144.105918");
        } else if (ptmName.equalsIgnoreCase("iTRAQ114 on Y")) {
            return new CvTerm("MOD", "MOD:01488", "iTRAQ4plex114", "144.105918");
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on nterm")) {
            return new CvTerm("MOD", "MOD:01493", "iTRAQ4plex115", "144.099599");
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on K")) {
            return new CvTerm("MOD", "MOD:01494", "iTRAQ4plex115", "144.099599");
        } else if (ptmName.equalsIgnoreCase("iTRAQ115 on Y")) {
            return new CvTerm("MOD", "MOD:01495", "iTRAQ4plex115", "144.099599");
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on nterm")) {
            return new CvTerm("MOD", "MOD:01500", "iTRAQ4plex116", "144.102063");
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on K")) {
            return new CvTerm("MOD", "MOD:01501", "iTRAQ4plex116", "144.102063");
        } else if (ptmName.equalsIgnoreCase("iTRAQ116 on Y")) {
            return new CvTerm("MOD", "MOD:01502", "iTRAQ4plex116", "144.102063");
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on nterm")) {
            return new CvTerm("MOD", "MOD:01507", "iTRAQ4plex117", "144.102063");
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on K")) {
            return new CvTerm("MOD", "MOD:01508", "iTRAQ4plex117", "144.102063");
        } else if (ptmName.equalsIgnoreCase("iTRAQ117 on Y")) {
            return new CvTerm("MOD", "MOD:01509", "iTRAQ4plex117", "144.102063");
        } else if (ptmName.equalsIgnoreCase("MMTS on C")) {
            return new CvTerm("MOD", "MOD:00110", "Methylthio", "45.987721");
        } else if (ptmName.equalsIgnoreCase("heavy lysine - 2H4")) {
            return new CvTerm("MOD", "MOD:00942", "Label:2H(4)", "4.025107");
        } else if (ptmName.equalsIgnoreCase("heavy lysine - 13C6 15N2")) {
            return new CvTerm("MOD", "MOD:00582", "Label:13C(6)15N(2)", "8.014199");
        } else if (ptmName.equalsIgnoreCase("Asparagine HexNAc")) {
            return new CvTerm("MOD", "MOD:01673", "HexNAc", "203.079373");
        } else if (ptmName.equalsIgnoreCase("Asparagine dHexHexNAc")) {
            return new CvTerm("MOD", "MOD:00510", "HexNAc(1)dHex(1)", "349.137281");
        } else if (ptmName.equalsIgnoreCase("Serine HexNAc")) {
            return new CvTerm("MOD", "MOD:01675", "HexNAc S", "203.079373");  
        } else if (ptmName.equalsIgnoreCase("Threonine HexNAc")) {
            return new CvTerm("MOD", "MOD:01676", "HexNAc S", "203.079373");  
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of S")) {
            return new CvTerm("MOD", "MOD:01423", "Palmitoleyl", "236.214016");  
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of C")) {
            return new CvTerm("MOD", "MOD:01423", "Palmitoleyl", "236.214016");
        } else if (ptmName.equalsIgnoreCase("palmitoleyl of T")) {
            return new CvTerm("MOD", "MOD:01423", "Palmitoleyl", "236.214016");
        } else if (ptmName.equalsIgnoreCase("CHD2-di-methylation of K")) {
            return new CvTerm("MOD", "MOD:01254", "Dimethyl:2H(4)", "32.056407");
        } else if (ptmName.equalsIgnoreCase("CHD2-di-methylation of peptide n-term")) {
            return new CvTerm("MOD", "MOD:01459", "Dimethyl:2H(4)", "32.056407");
        } else if (ptmName.equalsIgnoreCase("Maleimide-PEO2-Biotin of C")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("phosphorylation of H")) {
            return new CvTerm("MOD", "MOD:00696", "phosphorylated residue", "79.966331");
        } else if (ptmName.equalsIgnoreCase("oxidation of C")) {
            return new CvTerm("MOD", "MOD:01047", "Oxidation", "15.994915");
        } else if (ptmName.equalsIgnoreCase("oxidation of Y (duplicate of 64)")) {
            return new CvTerm("MOD", "MOD:01047", "Oxidation", "15.994915");
        } else if (ptmName.equalsIgnoreCase("Uniblue A on K")) {
            return new CvTerm("MOD", "MOD:01659", "Uniblue A", "484.039891");
        } else if (ptmName.equalsIgnoreCase("deamidation of N")) {
            return new CvTerm("MOD", "MOD:00400", "Deamidated", "0.984016");
        } else if (ptmName.equalsIgnoreCase("trideuteration of L (SILAC)")) {
            return new CvTerm("MOD", "MOD:00838", "Label:2H(3)", "3.018830");
        } else if (ptmName.equalsIgnoreCase("TMT duplex on K")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("TMT duplex on n-term peptide")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("TMT 6-plex on K")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("TMT 6-plex on n-term peptide")) {
            return new CvTerm("MOD", "MOD:01715", "TMT 6-plex", "229.162932");
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on nterm")) {
            return new CvTerm("MOD", "MOD:01715", "TMT 6-plex", "229.162932");
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on K")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(7)15N(1) on Y")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on nterm")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on K")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("iTRAQ8plex:13C(6)15N(2) on Y")) {
            return null; // @TODO: no mapping found!!
        } else if (ptmName.equalsIgnoreCase("selenocysteine")) {
            return new CvTerm("MOD", "MOD:00530", "selenocysteine", "47.944449");
        } else if (ptmName.equalsIgnoreCase("carboxymethylated selenocysteine")) {
            return null; // @TODO: no mapping found!!
        }
        
        return null;
    }
}
