# PeptideShaker #

  * [Introduction](#introduction)
  * [Read Me](#read-me)
  * [Troubleshooting](#troubleshooting)

  * [Bioinformatics for Proteomics Tutorial](http://compomics.com/bioinformatics-for-proteomics/)

---

## PeptideShaker Publication:
  * [Vaudel et al. Nature Biotechnol. 2015 Jan;33(1):22–24](http://www.nature.com/nbt/journal/v33/n1/full/nbt.3109.html).
  * If you use PeptideShaker as part of a publication please include this reference.

---

|   |   |   |
| :------------------------- | :---------------: | :--: |
| [![download](https://github.com/compomics/peptide-shaker/wiki/images/download_button.png)](http://genesis.ugent.be/maven2/eu/isas/peptideshaker/PeptideShaker/2.0.1/PeptideShaker-2.0.1.zip) | *v2.0.1 - All platforms* | [ReleaseNotes](https://github.com/compomics/peptide-shaker/wiki/ReleaseNotes) |

---

|  |  |  |
|:--:|:--:|:--:|
| [![](https://github.com/compomics/peptide-shaker/wiki/images/Overview_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/Overview.png) | [![](https://github.com/compomics/peptide-shaker/wiki/images/SpectrumIDs_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/SpectrumIDs.png) | [![](https://github.com/compomics/peptide-shaker/wiki/images/3DStructure_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/3DStructure.png) |
| [![](https://github.com/compomics/peptide-shaker/wiki/images/GO_Enrichment_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/GO_Enrichment.png) | [![](https://github.com/compomics/peptide-shaker/wiki/images/Validation_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/Validation.png) | [![](https://github.com/compomics/peptide-shaker/wiki/images/QC_Plots_small.png)](https://github.com/compomics/peptide-shaker/wiki/images/QC_Plots.png) |

(Click on an image to see the full size version)

---

## Introduction ##

PeptideShaker is a search engine independent platform for interpretation of proteomics identification results from multiple search and _de novo_ engines, currently supporting  [X!Tandem](http://www.thegpm.org/tandem), [MS-GF+](https://github.com/MSGFPlus/msgfplus), [MS Amanda](http://ms.imp.ac.at/?goto=msamanda), [OMSSA](http://www.ncbi.nlm.nih.gov/pubmed/15473683), [MyriMatch](http://www.ncbi.nlm.nih.gov/pubmed/?term=17269722), [Comet](http://comet-ms.sourceforge.net/), [Tide](http://cruxtoolkit.sourceforge.net), [Mascot](http://www.matrixscience.com), [Andromeda](http://www.coxdocs.org/doku.php?id=maxquant:andromeda:start), [MetaMorpheus](https://github.com/smith-chem-wisc/MetaMorpheus), [Novor](http://rapidnovor.com), [DirecTag](http://fenchurch.mc.vanderbilt.edu/bumbershoot/directag/) and [mzIdentML](http://www.psidev.info/mzidentml). 

PeptideShaker aggregates the results in a single identification set, annotates spectra, computes a consensus score, maps sequences and performs protein inference, scores post-translational modification localization, runs statistical validation, quality control, and annotates the results using multiple sources of information like Gene Ontology, UniProt and Ensembl annotation, and protein structures.

PeptideShaker can be used in command line and comes with rich visualization capabilities to navigate the results. It can be used on local data as well as on data sets deposited to the ProteomeXchange PRIDE repository.


PeptideShaker currently supports nine different analysis tasks:

  * **Overview:** get a simple yet detailed overview of all the proteins, peptides and PSMs in your dataset.
  * **Spectrum IDs:** compare the search engine performance and see how the search engine results are combined.
  * **Fractions:** inspect from which fractions proteins and peptides are likely to come from.
  * **Modifications:** get a detailed view of the post-translational modifications in the dataset.
  * **3D Structure:** map the detected peptides and modifications onto corresponding PDB structures.
  * **GO Enrichment:** perform GO enrichment and find enriched GO terms in your dataset.
  * **Validation:** inspect and fine tune the validation process.
  * **QC Plots:** examine the quality of the results with Quality Control plots.
  * **Reshake PRIDE:** re-analyze public datasets in PRIDE as if they were your own.

All data can also easily be exported for follow up analysis in other tools.

For further help see the [Bioinformatics for Proteomics Tutorial](http://compomics.com/bioinformatics-for-proteomics/).

If you have any questions, suggestions or remarks, feel free to contact us via the
[PeptideShaker Google Group](http://groups.google.com/group/peptide-shaker). For specific bug reports or issues please use the [issues tracker](https://github.com/compomics/peptide-shaker/issues).

To start using PeptideShaker, unzip the downloaded file, and double-click the `PeptideShaker-X.Y.Z.jar file`. No additional installation required!

[Go to top of page](#peptideshaker)

---

## Read Me ##

  * [Minimum Requirements](#minimum-requirements)
  * [From the Command Line](#from-the-command-line)
  * [Bioconda](#bioconda)
  * [Docker](#docker)
  * [SearchGUI](#searchgui)
  * [User Defined Modifications](#user-defined-modifications)
  * [Database Help](#database-help)
  * [Support Mascot Support](#mascot-support)
  * [mzIdentML Support](#mzidentml-support)
  * [Decoy Databases](#decoy-databases)
  * [Converting Spectrum Data](#converting-spectrum-data)

---

### Minimum Requirements ###

It should be possible to run PeptideShaker on almost any computer where Java 1.6 or newer is installed. We recommend always using the latest version of Java, 1.7 or newer is recommend.

However to get the best out of PeptideShaker a newer machine with at least 4 GB of memory is recommended. If parsing big datasets even more memory is required: the bigger the dataset the more memory you need.

Note that in order to use more than 1500 MB of memory you need to install the 64 bit version of Java. See our [Java Troubleshooting](https://github.com/compomics/compomics-utilities/wiki/JavaTroubleShooting) for help.

The minimum screen resolution for PeptideShaker is 1280 x 800, but it is highly recommended to use at least 1680 x 1050. Again, the bigger the better.

[Go to top of page](#peptideshaker)

---

### From the Command Line

The main purpose of PeptideShaker is to make it simpler to process and display the results of multiple search engines. A graphical user interface is the best choice for smaller projects. PeptideShaker can also be used _via_ the command line, and be incorporated in different analysis pipelines.

For details about the command line see: [PeptideShakerCLI](https://github.com/compomics/peptide-shaker/wiki/PeptideShakerCLI).

[Go to top of page](#peptideshaker)

----

### Bioconda ###
[![install with bioconda](https://img.shields.io/badge/install%20with-bioconda-brightgreen.svg?style=flat-square)](http://bioconda.github.io/recipes/peptide-shaker/README.html)
[![install with bioconda](https://anaconda.org/bioconda/peptide-shaker/badges/latest_release_relative_date.svg)](http://bioconda.github.io/recipes/peptide-shaker/README.html)
[![install with bioconda](https://anaconda.org/bioconda/peptide-shaker/badges/downloads.svg)](http://bioconda.github.io/recipes/peptide-shaker/README.html)


PeptideShaker is available as a [Miniconda package](http://conda.pydata.org/miniconda.html) in the [bioconda](https://bioconda.github.io) channel [here](https://anaconda.org/bioconda/peptide-shaker). 

You can install PeptideShaker with:

```bash
conda install -c conda-forge -c bioconda peptide-shaker
```

[Go to top of page](#peptideshaker)

----

### Docker ###


A [Docker](https://www.docker.com/) container is available via the [biocontainers](https://quai.io/repository/biocaontainers/) project. You can make use of the container via:

```bash
docker run quay.io/biocontainers/peptide-shaker:X.Y.Z--1 peptide-shaker eu.isas.peptideshaker.cmd.PeptideShakerCLI 
```

Replace X.Y.Z with the wanted PeptideShaker version number.

In case you need to use your own files, you will need to map (using `-v` Docker parameter) your local folders containing them into the Docker internal file system, like 


```bash
docker run -v /home/my_user/resources:/myresources 
quay.io/biocontainers/peptide-shaker:X.Y.Z--1 
peptide-shaker eu.isas.peptideshaker.cmd.PeptideShakerCLI 
...
```

[Go to top of page](#peptideshaker)

---

### SearchGUI ###

PeptideShaker has a strong connection to the [SearchGUI](http://compomics.github.io/projects/searchgui.html) project.

**SearchGUI** is a user-friendly, lightweight and open-source graphical user interface for configuring and running proteomics identification search engines, namely  [X! Tandem](http://www.thegpm.org/tandem), [MyriMatch](http://forge.fenchurch.mc.vanderbilt.edu/scm/viewvc.php/*checkout*/trunk/doc/index.html?root=myrimatch), [MS Amanda](http://ms.imp.ac.at/?goto=msamanda), [MS-GF+](https://github.com/MSGFPlus/msgfplus), [OMSSA](http://www.ncbi.nlm.nih.gov/pubmed/15473683), [Comet](http://comet-ms.sourceforge.net/), [Tide](http://cruxtoolkit.sourceforge.net), [Andromeda](http://www.andromeda-search.org), [MetaMorpheus](https://github.com/smith-chem-wisc/MetaMorpheus), [Novor](http://rapidnovor.com) and [DirecTag](http://fenchurch.mc.vanderbilt.edu/bumbershoot/directag/).

Importing output from **SearchGUI** is especially simple in PeptideShaker as the parameters and files used for the search is easily available.

For more information on **SearchGUI** see [http://compomics.github.io/projects/searchgui.html](http://compomics.github.io/projects/searchgui.html).

[Go to top of page](#peptideshaker)

---

### User Defined Modifications ###

To add user defined modifications see [User Defined Modifications in SearchGUI](https://github.com/compomics/searchgui#user-defined-modifications).

[Go to top of page](#peptideshaker)

---

### Database Help ###

For help on obtaining a valid sequence database see the [Database Help](https://github.com/compomics/searchgui/wiki/DatabaseHelp).

[Go to top of page](#peptideshaker)

---

### Mascot Support ###

PeptideShaker supports the import of data from [Mascot dat files](http://www.matrixscience.com/help/export_help.html#MascotDAT).
Make sure that the spectra are available in the mgf format where every spectrum should have a unique title.

Mascot's [Automatic Decoy Search](http://www.matrixscience.com/help/decoy_help.html#AUTO) is _not_ compatible with PeptideShaker. The reason for this is that Mascot uses a random decoy approach and not a reverse decoy approach. When combining results from different search engines it is important that the database and decoys used are identical, something that cannot be guaranteed when using the random approach.

To combine Mascot results with your results from [SearchGUI](http://compomics.github.io/projects/searchgui.html) you therefore have to use the same target-decoy database as the one used in the [SearchGUI](http://compomics.github.io/projects/searchgui.html) search and **_not_** select the decoy option when performing the Mascot search.

To get target-decoy databases that are fully compatible with PeptideShaker see the [Decoy Databases](#decoy-databases) section below.

[Go to top of page](#peptideshaker)

---

### mzIdentML Support ###

PeptideShaker can load results from virtually any identification algorithm in the [mzIdentML](http://www.psidev.info/mzidentml) format as long as the minimal peptide to spectrum match information is present in the file.

The following is required:
  * Spectrum file format has to be mgf.
  * Each PSM has a score or e-value as a [PSM score CV term](https://www.ebi.ac.uk/ontology-lookup/?termId=MS:1001143).

If you have mzIdentML files that fulfill these criteria but do not load in PeptideShaker, please [let us know](https://github.com/compomics/peptide-shaker/issues).

[Go to top of page](#peptideshaker)

---

### Decoy Databases ###

In order for PeptideShaker to be able validate your identifications you need to provide the sequence database (i.e., the FASTA file) as a concatenated target-decoy database.

This is achieved by adding non-existing protein sequences (so-called decoy sequences) to the original protein database. There are various ways of creating the artificial sequences. In the context of PeptideShaker, it is recommended to use reversed versions of the actual sequences. When working with multiple search engines, make sure that they use the exact same database.

This means that whenever a mistake is made when searching in the combined database, it is as likely to happen in the real database (called the target database) as it is in the artificial database (called the decoy database).

Note that we only guarantee the performance of PeptideShaker when using concatenated forward and reversed sequences. _If you use other types of databases it is at your own risks!_

Target-decoy database compatible with PeptideShaker can be created using [SearchGUI](#searchgui).

_Note that PeptideShaker will load search results from searches not using decoy databases, but this is not recommended as this makes it impossible to statistically validate the identifications!_

[Go to top of page](#peptideshaker)

---

### Converting Spectrum Data ###

PeptideShaker supports mgf and mzML files as the input format for the spectra. To convert your raw data we recommend using [ThermoRawFileParser](https://github.com/compomics/ThermoRawFileParser) for Thermo raw files and [msconvert](http://proteowizard.sourceforge.net) from [ProteoWizard](http://proteowizard.sourceforge.net) for other raw file formats.

[Go to top of page](#peptideshaker)

---

## Troubleshooting ##

  * **Mascot Issues** - See [Mascot Support](#mascot-support) and [Database Help](https://github.com/compomics/searchgui/wiki/DatabaseHelp).

  * **mzIdentML Issues** - See [mzIdentML Support](#mzidentml-support).

  * **Database Help** - For help on how to set up a proper FASTA database, please see [Database Help](https://github.com/compomics/searchgui/wiki/DatabaseHelp). For Mascot databases see [Mascot Support](#mascot-support). Also see [Databases Decoy Databases](#decoy).

  * **Does Not Start I** - Do you have Java installed? Download the latest version of Java  [here](http://java.sun.com/javase/downloads/index.jsp) and try again. (You only need the JRE version (and not the JDK version) to run PeptideShaker.)

  * **Does Not Start II** - Have you unzipped the zip file? You need to unzip the file before double clicking the jar file. If you get the message "A Java Exception has occurred", you are most likely trying to run PeptideShaker from within the zip file. Unzip the file and try again.

  * **Does Not Start III** - Is PeptideShaker installed in a path containing special characters, i.e. `[`, `%`, æ, ø, å, etc? If so, move the whole folder to a different location or rename the folder(s) causing the problem and try again. (Note that on Linux PeptideShaker has to be run from a path not containing spaces).

  * **Does Not Start IV** - If PeptideShaker fails during start-up a file called `startup.log` will be created in the PeptideShaker `resources\conf` folder. Here you can find detailed information about why the program was not able to start.

  * **Unidentified Developer** - If you run PeptideShaker on a Mac you can get the warning _"PeptideShaker" can't be opened because it is from an unidentified developer_. To escape this warning control-click on the file icon and then select "Open." This will give you the option of opening it regardless of its unidentified source. This only has to be done once for each PeptideShaker version.

  * **Memory Issues I** - Big datasets can require a lot of memory. If the software unexpectedly fails on a big project, and the software mentions that it ran out of memory, you should try to give the program more memory. This can be done by selecting `Java Options` on the `Edit` menu in PeptideShaker. Set the memory limit in MB, e.g., `2500` for a maximum of appr. 2.5GB of memory. _Please note that on a 32-bit operating system you cannot increase this value beyond 2000M (and usually the maximum limit is lower than this)._

  * **Memory Issues II** - Using more than 2GB of memory requires the installation of 64 bit Java. 64 bit Java is downloaded from the same place as the 32 bit version: [Java](http://java.sun.com/javase/downloads/index.jsp). _Note that 64 bit Java can only be used on 64 bit operating systems!_

  * **Java 32 bit vs 64 bit** - If you have both 32 and 64 bit versions of Java installed the operating system can sometimes get confused about which version to use to run PeptideShaker. For Windows platform PeptideShaker tries to default to the 64 bit version of Java if it is installed. You can however override this option by setting your own Java Home. This is done by creating a file called `JavaHome.txt` in the `resources\conf` folder of PeptideShaker, with the path to the bin folder of the Java version to use, e.g., `C:\Program Files\Java\jdk1.6.0_29\bin\`. If the folder does not exist (or it does not contain the required files), the default Java version will be used.

  * **Xlib/X11 errorrs** - When running the command lines on systems without a grahpical user interface you may get errors related to X11. If that happens try adding `-Djava.awt.headless=true` to the command line.

  * **Protein Not Found** - In order to provide the most comprehensive results, PeptideShaker needs to link the protein accession retrieved by the various search engines to the FASTA file. Various errors can result in PeptideShaker not being able to find your protein. First, verify that the accession number is indeed in your FASTA file. Then, set up an [issue](https://github.com/compomics/peptide-shaker/issues) describing the problem and provide the accession not found together with its header and sequence in the FASTA file. Please, also mention the database type and version. See also [Database Help](https://github.com/compomics/searchgui/wiki/DatabaseHelp), [Mascot Support](#mascot-support) and [Databases Decoy Databases](#decoy). Example for P60323 in UniProt:

```
            >sw|P60323|NANO3_HUMAN Nanos homolog 3 OS=Homo sapiens GN=NANOS3 PE=2 SV=1
            MGTFDLWTDYLGLAHLVRALSGKEGPETRLSPQPEPEPMLEPDQKRSLESSPAPERLCSFCKHNGESRAIYQSHV
            LKDEAGRVLCPILRDYVCPQCGATRERAHTRRFCPLTGQGYTSVYSHTTRNSAGKKLVRPDKAKTQDTGHRRGGG
            GGAGFRGAGKSEPSPSCSPSMST
```

  * **Proxy Server** - Are you using a proxy server to access the Internet? Then you need to add your proxy settings to the `JavaOptions.txt` file located in the `resources\conf` folder of PeptideShaker. Add the following lines (replacing the values between the brackets and skipping the last two lines if username and password is not required):

```
            -Dhttp.proxyHost=<myproxyserver.com>
            -Dhttp.proxyPort=<proxy port>
            -Dhttp.proxyUser=<user>
            -Dhttp.proxyPassword=<password>
```

  * **General Error Diagnosis** - If you go to `Help` and then `Bug Report`, you will find a log of the PeptideShaker activity. This includes transcripts of any errors that the application has encountered, and can be very useful in diagnosing issues.

  * **Problem Not Solved? Or Problem Not In List?** Contact the developers of PeptideShaker by setting up an [issue](https://github.com/compomics/peptide-shaker/issues) describing the problem.

[Go to top of page](#peptideshaker)

---
