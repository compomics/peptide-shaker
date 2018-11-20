package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipOutputStream;

/**
 * This class takes care of exporting the entire project as a single file.
 *
 * @author Marc Vaudel
 */
public class ProjectExport {

    /**
     * Empty default constructor
     */
    public ProjectExport() {
    }

    /**
     * The name of the folder where to save the mgf and FASTA file.
     */
    public final static String defaultDataFolder = "data";
    /**
     * The name of the folder where to save report files.
     */
    public final static String defaultReportsFolder = "reports";

    
    
    
    /**
     * Exports the project as zip file.
     *
     * @param zipFile the destination file
     * @param fastaFile the FASTA file
     * @param spectrumFiles the spectrum files
     * @param cpsFile the cps file
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public static void exportProjectAsZip(File zipFile, File fastaFile, ArrayList<File> spectrumFiles, File cpsFile, WaitingHandler waitingHandler) throws IOException {
        ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, null,  cpsFile, waitingHandler);
    }
    
     /**
     * Exports the project as zip file adding reports to it.
     *
     * @param zipFile the destination file
     * @param fastaFile the FASTA file
     * @param spectrumFiles the spectrum files
     * @param reportFiles the report files
     * @param cpsFile the cps file
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public static void exportProjectAsZip(File zipFile, File fastaFile, ArrayList<File> spectrumFiles, ArrayList<File> reportFiles,  File cpsFile, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting FASTA File. Please Wait...");
        }

        ArrayList<String> dataFiles = new ArrayList<>();
        dataFiles.add(fastaFile.getAbsolutePath());

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting Spectrum Files. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounter(0);
            waitingHandler.setMaxSecondaryProgressCounter(spectrumFiles.size());
        }

        for (File spectrumFile : spectrumFiles) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            if (spectrumFile.exists()) {
                dataFiles.add(spectrumFile.getAbsolutePath());
                File indexFile = new File(spectrumFile.getParentFile(), SpectrumFactory.getIndexName(spectrumFile.getName()));
                if (indexFile.exists()) {
                    dataFiles.add(indexFile.getAbsolutePath());
                }
            }
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Zipping Project. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            if (waitingHandler.isRunCanceled()) {
                return;
            }
        }

        // zip the project
        FileOutputStream fos = new FileOutputStream(zipFile);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            try {
                ZipOutputStream out = new ZipOutputStream(bos);
                try {
                    // get the total uncompressed size
                    long totalUncompressedSize = 0;
                    totalUncompressedSize += cpsFile.length();
                    for (String dataFilePath : dataFiles) {
                        totalUncompressedSize += new File(dataFilePath).length();
                    }
                    if (reportFiles != null){
                        for (File reportFile : reportFiles) {
                            totalUncompressedSize += reportFile.length();
                        }
                    }
                    
                    // add the data files
                    if (waitingHandler != null) {
                        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                        waitingHandler.setSecondaryProgressCounter(0);
                        waitingHandler.setMaxSecondaryProgressCounter(100);
                    }

                    // add the cps file
                    ZipUtils.addFileToZip(cpsFile, out, waitingHandler, totalUncompressedSize);

                    // create the data folder in the zip file
                    ZipUtils.addFolderToZip(defaultDataFolder, out);

                    // add the files to the data folder
                    for (String dataFilePath : dataFiles) {

                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                            waitingHandler.increaseSecondaryProgressCounter();
                        }

                        File dataFile = new File(dataFilePath);
                        ZipUtils.addFileToZip(defaultDataFolder, dataFile, out, waitingHandler, totalUncompressedSize);
                    }
                    
                    if (reportFiles != null && reportFiles.size()> 0){
                        // create the data folder in the zip file
                        ZipUtils.addFolderToZip(defaultReportsFolder, out);

                        // add the files to the data folder
                        for (File reportFile : reportFiles) {

                            if (waitingHandler != null) {
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                                waitingHandler.increaseSecondaryProgressCounter();
                            }
                            ZipUtils.addFileToZip(defaultReportsFolder, reportFile, out, waitingHandler, totalUncompressedSize);
                        }
                    }
                    
                    if (waitingHandler != null) {
                        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
                    }

                } finally {
                    out.close();
                }
            } finally {
                bos.close();
            }
        } finally {
            fos.close();
        }
    }
}
