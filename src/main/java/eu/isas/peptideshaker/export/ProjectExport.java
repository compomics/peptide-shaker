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
     * @param fastaFile path to the FASTA file
     * @param spectrumFiles the spectrum files
     * @param cpsFile the cps file
     * @param moveFilesIntoZip if true, the files will be moved into the zip
     * file, i.e. not just copied
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public static void exportProjectAsZip(File zipFile, File fastaFile, ArrayList<File> spectrumFiles, File cpsFile, boolean moveFilesIntoZip, WaitingHandler waitingHandler) throws IOException {
        ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, null, null, cpsFile, moveFilesIntoZip, waitingHandler);
    }

    /**
     * Exports the project as zip file adding reports to it.
     *
     * @param zipFile the destination file
     * @param fastaFile path to the FASTA file
     * @param spectrumFiles the spectrum files
     * @param reportFiles the report files
     * @param mzidFile the mzid file
     * @param cpsFile the cps file
     * @param moveFilesIntoZip if true, the files will be moved into the zip
     * file, i.e. not just copied
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public static void exportProjectAsZip(File zipFile, File fastaFile, ArrayList<File> spectrumFiles, ArrayList<File> reportFiles, File mzidFile, File cpsFile, boolean moveFilesIntoZip, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting FASTA File. Please Wait...");
        }

        // Note: The order files are added to the zip matters: zip files can be read sequencially
        // and bigger and unused files should therefore be added after smaller and less used files.
        ArrayList<String> dataFiles = new ArrayList<String>();
        dataFiles.add(fastaFile.getAbsolutePath());

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting Spectrum Files. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounter(0);
            waitingHandler.setMaxSecondaryProgressCounter(spectrumFiles.size());
        }

        int indexesPositions = dataFiles.size();

        for (File spectrumFile : spectrumFiles) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            if (spectrumFile.exists()) {
                File indexFile = new File(spectrumFile.getParentFile(), SpectrumFactory.getIndexName(spectrumFile.getName()));
                if (indexFile.exists()) {
                    // the indexes are added after the FASTA and FASTA index files
                    dataFiles.add(indexesPositions++, indexFile.getAbsolutePath());
                }
                // add the spectrum files at the end to make index access faster
                dataFiles.add(spectrumFile.getAbsolutePath());
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
                    if (cpsFile != null) {
                        totalUncompressedSize += cpsFile.length();
                    }
                    for (String dataFilePath : dataFiles) {
                        totalUncompressedSize += new File(dataFilePath).length();
                    }
                    if (reportFiles != null) {
                        for (File reportFile : reportFiles) {
                            totalUncompressedSize += reportFile.length();
                        }
                    }
                    if (mzidFile != null) {
                        totalUncompressedSize += mzidFile.length();
                    }

                    // add the files to the zip
                    if (waitingHandler != null) {
                        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
                        waitingHandler.setSecondaryProgressCounter(0);
                        waitingHandler.setMaxSecondaryProgressCounter(100);
                    }

                    // add the reports
                    if (reportFiles != null && reportFiles.size() > 0) {
                        // create the reports folder in the zip file
                        ZipUtils.addFolderToZip(defaultReportsFolder, out);

                        // move the files to the reports folder
                        for (File reportFile : reportFiles) {

                            if (waitingHandler != null) {
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                                waitingHandler.increaseSecondaryProgressCounter();
                            }
                            ZipUtils.addFileToZip(defaultReportsFolder, reportFile, out, waitingHandler, totalUncompressedSize);
                            if (moveFilesIntoZip) {
                                reportFile.delete();
                            }
                        }
                    }

                    // add the data
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

                    // add the mzid file
                    if (mzidFile != null) {
                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                            waitingHandler.increaseSecondaryProgressCounter();
                        }

                        // move the mzid file to the zip folder
                        ZipUtils.addFileToZip(mzidFile, out, waitingHandler, totalUncompressedSize);
                        if (moveFilesIntoZip) {
                            mzidFile.delete();
                        }
                    }

                    // move the cps file to the zip
                    if (cpsFile != null) {
                        ZipUtils.addFileToZip(cpsFile, out, waitingHandler, totalUncompressedSize);
                        if (moveFilesIntoZip) {
                            cpsFile.delete();
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
