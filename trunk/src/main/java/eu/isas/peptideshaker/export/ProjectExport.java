package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class takes care of exporting the entire project as a single file.
 *
 * @author Marc
 */
public class ProjectExport {

    /**
     * Exports the project as zip file.
     *
     * @param zipFile the destination file
     * @param fastaFile the fasta file
     * @param spectrumFiles the spectrum files
     * @param cpsFile the cps file
     * @param waitingHandler a waiting handler to display progress to the user
     * and cancel the process (can be null)
     *
     * @throws IOException
     */
    public static void exportProjectAsZip(File zipFile, File fastaFile, ArrayList<File> spectrumFiles, File cpsFile, WaitingHandler waitingHandler) throws IOException {

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting FASTA File. Please Wait...");
        }

        ArrayList<String> dataFiles = new ArrayList<String>();
        dataFiles.add(fastaFile.getAbsolutePath());

        File indexFile = new File(fastaFile.getParentFile(), SequenceFactory.getIndexName(fastaFile.getName()));

        if (indexFile.exists()) {
            dataFiles.add(indexFile.getAbsolutePath());
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Getting Spectrum Files. Please Wait...");
            waitingHandler.setPrimaryProgressCounterIndeterminate(false);
            waitingHandler.setPrimaryProgressCounter(0);
            waitingHandler.setMaxPrimaryProgressCounter(spectrumFiles.size());
        }

        for (File spectrumFile : spectrumFiles) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increasePrimaryProgressCounter();
            }

            if (spectrumFile.exists()) {
                dataFiles.add(spectrumFile.getAbsolutePath());

                indexFile = new File(spectrumFile.getParentFile(), SpectrumFactory.getIndexName(spectrumFile.getName()));

                if (indexFile.exists()) {
                    dataFiles.add(indexFile.getAbsolutePath());
                }
            }
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Compressing project. Please Wait...");
            waitingHandler.setPrimaryProgressCounterIndeterminate(true);
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
                    final int BUFFER = 2048;
                    byte data[] = new byte[BUFFER];
                    int count;

                    // add the cps file
                    FileInputStream fi = new FileInputStream(cpsFile);
                    try {
                        BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                        try {
                            ZipEntry entry = new ZipEntry(cpsFile.getName());
                            out.putNextEntry(entry);
                            while ((count = origin.read(data, 0, BUFFER)) != -1) {

                                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                                    return;
                                }
                                out.write(data, 0, count);
                            }
                        } finally {
                            origin.close();
                        }
                    } finally {
                        fi.close();
                    }

                    // add the data files
                    if (waitingHandler != null) {
                        waitingHandler.setWaitingText("Compressing FASTA and Spectrum Files. Please Wait...");
                        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                        waitingHandler.setPrimaryProgressCounter(0);
                        waitingHandler.setMaxPrimaryProgressCounter(dataFiles.size());
                    }

                    // create the data folder in the zip file
                    out.putNextEntry(new ZipEntry("data/"));

                    // add the files to the data folder
                    for (int i = 0; i < dataFiles.size(); i++) {

                        if (waitingHandler != null) {
                            if (waitingHandler.isRunCanceled()) {
                                return;
                            }
                            waitingHandler.increasePrimaryProgressCounter();
                        }

                        fi = new FileInputStream(new File(dataFiles.get(i)));
                        try {
                            BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                            try {
                                ZipEntry entry = new ZipEntry("data/" + new File(dataFiles.get(i)).getName());
                                out.putNextEntry(entry);
                                while ((count = origin.read(data, 0, BUFFER)) != -1 && !waitingHandler.isRunCanceled()) {
                                    out.write(data, 0, count);
                                }
                            } finally {
                                origin.close();
                            }
                        } finally {
                            fi.close();
                        }
                    }

                    if (waitingHandler != null) {
                        waitingHandler.setPrimaryProgressCounterIndeterminate(true);
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
