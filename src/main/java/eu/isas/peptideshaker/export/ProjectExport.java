package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.io.mass_spectrometry.cms.CmsFileUtils;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.io.IoUtil;
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
    public final static String DEFAULT_DATA_FOLDER = "data";
    /**
     * The name of the folder where to save report files.
     */
    public final static String DEFAULT_REPORTS_FOLDER = "reports";

    /**
     * Exports the project as zip file.
     *
     * @param zipFile the destination file
     * @param fastaFile path to the FASTA file
     * @param spectrumProvider the spectrum provider
     * @param cpsFile the cps file
     * @param moveFilesIntoZip if true, the files will be moved into the zip
     * file, i.e. not just copied
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException exception thrown whenever a problem occurred while
     * reading/writing the file
     */
    public static void exportProjectAsZip(
            File zipFile,
            File fastaFile,
            SpectrumProvider spectrumProvider,
            File cpsFile,
            boolean moveFilesIntoZip,
            WaitingHandler waitingHandler
    ) throws IOException {

        ProjectExport.exportProjectAsZip(
                zipFile,
                fastaFile,
                spectrumProvider,
                null,
                null,
                null,
                cpsFile,
                moveFilesIntoZip,
                waitingHandler
        );
    }

    /**
     * Exports the project as zip file adding reports to it.
     *
     * @param zipFile the destination file
     * @param fastaFile path to the FASTA file
     * @param spectrumProvider the spectrum provider
     * @param followupAnalysisFiles followup analysis files
     * @param reportFiles the identification features report files
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
    public static void exportProjectAsZip(
            File zipFile,
            File fastaFile,
            SpectrumProvider spectrumProvider,
            ArrayList<File> followupAnalysisFiles,
            ArrayList<File> reportFiles,
            File mzidFile,
            File cpsFile,
            boolean moveFilesIntoZip,
            WaitingHandler waitingHandler
    ) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting FASTA File. Please Wait...");
        }

        ArrayList<String> dataFiles = new ArrayList<>(2 * spectrumProvider.getFileNames().length + 1);
        dataFiles.add(fastaFile.getAbsolutePath());

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting Spectrum Files. Please Wait...");
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setSecondaryProgressCounter(0);
            waitingHandler.setMaxSecondaryProgressCounter(spectrumProvider.getFileNames().length);
        }

        for (String fileName : spectrumProvider.getFileNames()) {

            String cmsFilePath = spectrumProvider.getCmsFilePaths().get(fileName);
            File cmsFile = new File(cmsFilePath);

            if (cmsFile.exists()) {

                dataFiles.add(cmsFilePath);

            }
        }

        for (String fileName : spectrumProvider.getFileNames()) {

            String msFilePath = spectrumProvider.getFilePaths().get(fileName);
            File msFile = new File(msFilePath);

            if (!IoUtil.getExtension(msFile).equals(CmsFileUtils.EXTENSION) && msFile.exists()) {

                dataFiles.add(msFilePath);

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
        try (
                 ZipOutputStream out = new ZipOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(
                                        zipFile
                                )
                        )
                )) {

            // get the total uncompressed size
            long totalUncompressedSize = 0;
            if (cpsFile != null) {
                totalUncompressedSize += cpsFile.length();
            }
            for (String dataFilePath : dataFiles) {
                totalUncompressedSize += new File(dataFilePath).length();
            }

            if (followupAnalysisFiles != null) {
                for (File followupReportFile : followupAnalysisFiles) {
                    totalUncompressedSize += followupReportFile.length();
                }
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

            // add the identification features reports
            if (reportFiles != null && reportFiles.size() > 0) {

                // create the reports folder in the zip file
                ZipUtils.addFolderToZip(DEFAULT_REPORTS_FOLDER, out);

                // move the files to the reports folder
                for (File reportFile : reportFiles) {

                    if (waitingHandler != null) {

                        if (waitingHandler.isRunCanceled()) {

                            return;

                        }

                        waitingHandler.increaseSecondaryProgressCounter();

                    }

                    ZipUtils.addFileToZip(
                            DEFAULT_REPORTS_FOLDER,
                            reportFile,
                            out,
                            waitingHandler,
                            totalUncompressedSize
                    );

                    if (moveFilesIntoZip) {

                        reportFile.delete();

                    }
                }
            }

            if (followupAnalysisFiles != null && followupAnalysisFiles.size() > 0) {
                if (reportFiles == null || reportFiles.isEmpty()) {
                    // create the reports folder in the zip file if it was not previously created
                    ZipUtils.addFolderToZip(DEFAULT_REPORTS_FOLDER, out);
                }

                // move the files to the reports folder
                for (File followupAnalysisFile : followupAnalysisFiles) {

                    if (waitingHandler != null) {
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
                    }

                    ZipUtils.addFileToZip(
                            DEFAULT_REPORTS_FOLDER,
                            followupAnalysisFile,
                            out,
                            waitingHandler,
                            totalUncompressedSize
                    );

                    if (moveFilesIntoZip) {
                        followupAnalysisFile.delete();
                    }
                }
            }

            // add the data
            // create the data folder in the zip file
            ZipUtils.addFolderToZip(
                    DEFAULT_DATA_FOLDER,
                    out
            );

            // add the files to the data folder
            for (String dataFilePath : dataFiles) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                File dataFile = new File(dataFilePath);
                ZipUtils.addFileToZip(
                        DEFAULT_DATA_FOLDER,
                        dataFile,
                        out,
                        waitingHandler,
                        totalUncompressedSize
                );
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
                ZipUtils.addFileToZip(
                        mzidFile,
                        out,
                        waitingHandler,
                        totalUncompressedSize
                );
                if (moveFilesIntoZip) {
                    mzidFile.delete();
                }
            }

            // move the cps file to the zip
            if (cpsFile != null) {
                ZipUtils.addFileToZip(
                        cpsFile,
                        out,
                        waitingHandler,
                        totalUncompressedSize
                );
                if (moveFilesIntoZip) {
                    cpsFile.delete();
                }
            }

            if (waitingHandler != null) {
                waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            }
        }
    }
}
