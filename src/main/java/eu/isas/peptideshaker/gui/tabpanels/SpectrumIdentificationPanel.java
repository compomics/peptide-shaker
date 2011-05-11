package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.VennDiagram;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 *
 * @author Matrc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumIdentificationPanel extends javax.swing.JPanel {

    /** Creates new form SpectrumPanel */
    public SpectrumIdentificationPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        searchEnginetableJScrollPane.getViewport().setOpaque(false);
        spectrumTableJScrollPane.getViewport().setOpaque(false);
        peptideShakerJScrollPane.getViewport().setOpaque(false);
        xTandemTableJScrollPane.getViewport().setOpaque(false);
        mascotTableJScrollPane.getViewport().setOpaque(false);
        omssaTableJScrollPane.getViewport().setOpaque(false);

        fileNamesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        peptideShakerJTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")), new ImageIcon(this.getClass().getResource("/icons/Error_3.png"))));

        searchEngineTable.getTableHeader().setReorderingAllowed(false);
        peptideShakerJTable.getTableHeader().setReorderingAllowed(false);
        spectrumTable.getTableHeader().setReorderingAllowed(false);
        omssaTable.getTableHeader().setReorderingAllowed(false);
        mascotTable.getTableHeader().setReorderingAllowed(false);
        xTandemTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);
        searchEngineTable.setAutoCreateRowSorter(true);
        omssaTable.setAutoCreateRowSorter(true);
        mascotTable.setAutoCreateRowSorter(true);
        xTandemTable.setAutoCreateRowSorter(true);

        peptideShakerJTable.getColumn("  ").setMinWidth(30);
        peptideShakerJTable.getColumn("  ").setMaxWidth(30);

        omssaTable.getColumn(" ").setMinWidth(30);
        omssaTable.getColumn(" ").setMaxWidth(30);
        mascotTable.getColumn(" ").setMinWidth(30);
        mascotTable.getColumn(" ").setMaxWidth(30);
        xTandemTable.getColumn(" ").setMinWidth(30);
        xTandemTable.getColumn(" ").setMaxWidth(30);


        peptideShakerJTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        peptideShakerJTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        searchEngineTable.getColumn("Validated PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Unique PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("OMSSA").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("X!Tandem").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Mascot").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("All").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        spectrumTable.getColumn("Title").setMinWidth(200);
        spectrumTable.getColumn("Title").setMaxWidth(200);

        spectrumTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
    }

    /**
     * Displays or hide sparklines in the tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        searchEngineTable.revalidate();
        searchEngineTable.repaint();

        spectrumTable.revalidate();
        spectrumTable.repaint();

        peptideShakerJTable.revalidate();
        peptideShakerJTable.repaint();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        searchEnginetableJScrollPane = new javax.swing.JScrollPane();
        searchEngineTable = new javax.swing.JTable();
        vennDiagramButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        fileNamesCmb = new javax.swing.JComboBox();
        spectrumTableJScrollPane = new javax.swing.JScrollPane();
        spectrumTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        spectrumChartJPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        peptideShakerJScrollPane = new javax.swing.JScrollPane();
        peptideShakerJTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        omssaTableJScrollPane = new javax.swing.JScrollPane();
        omssaTable = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        xTandemTableJScrollPane = new javax.swing.JScrollPane();
        xTandemTable = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        mascotTableJScrollPane = new javax.swing.JScrollPane();
        mascotTable = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Engine Performance"));
        jPanel1.setOpaque(false);

        searchEngineTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Search Engine", "Validated PSMs", "Unique PSMs", "OMSSA", "X!Tandem", "Mascot", "All"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchEnginetableJScrollPane.setViewportView(searchEngineTable);

        vennDiagramButton.setBorderPainted(false);
        vennDiagramButton.setContentAreaFilled(false);
        vennDiagramButton.setFocusable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1133, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(vennDiagramButton, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(vennDiagramButton, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Selection"));
        jPanel2.setOpaque(false);

        fileNamesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "File Name" }));
        fileNamesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamesCmbActionPerformed(evt);
            }
        });

        spectrumTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title", "m/z", "Charge", "RT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spectrumTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumTableMouseReleased(evt);
            }
        });
        spectrumTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumTableKeyReleased(evt);
            }
        });
        spectrumTableJScrollPane.setViewportView(spectrumTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                    .addComponent(fileNamesCmb, 0, 528, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));
        jPanel3.setOpaque(false);

        spectrumChartJPanel.setBackground(new java.awt.Color(255, 255, 255));
        spectrumChartJPanel.setLayout(new javax.swing.BoxLayout(spectrumChartJPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide to Spectrum Matches"));
        jPanel4.setOpaque(false);

        peptideShakerJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Protein(s)", "Sequence", "Modifications", "Score", "Confidence", "delta p", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptideShakerJScrollPane.setViewportView(peptideShakerJTable);

        jLabel1.setText("Peptide-Shaker:");

        jPanel5.setOpaque(false);

        omssaTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide", "Modification(s)", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        omssaTable.setMinimumSize(new java.awt.Dimension(0, 0));
        omssaTableJScrollPane.setViewportView(omssaTable);

        jLabel3.setText("OMSSA:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addContainerGap(387, Short.MAX_VALUE))
            .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        jPanel7.setOpaque(false);

        jLabel4.setText("X!Tandem:");

        xTandemTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide", "Modification(s)", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        xTandemTable.setMinimumSize(new java.awt.Dimension(0, 0));
        xTandemTableJScrollPane.setViewportView(xTandemTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addContainerGap(376, Short.MAX_VALUE))
            .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        jPanel8.setOpaque(false);

        jLabel2.setText("Mascot:");

        mascotTableJScrollPane.setMinimumSize(new java.awt.Dimension(23, 87));

        mascotTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Peptide(s)", "Modification(s)", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        mascotTable.setMinimumSize(new java.awt.Dimension(0, 0));
        mascotTableJScrollPane.setViewportView(mascotTable);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addContainerGap(388, Short.MAX_VALUE))
            .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1316, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fileNamesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamesCmbActionPerformed
        fileSelectionChanged();
    }//GEN-LAST:event_fileNamesCmbActionPerformed

    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableMouseReleased

    private void spectrumTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumTableKeyReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableKeyReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox fileNamesCmb;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JTable mascotTable;
    private javax.swing.JScrollPane mascotTableJScrollPane;
    private javax.swing.JTable omssaTable;
    private javax.swing.JScrollPane omssaTableJScrollPane;
    private javax.swing.JScrollPane peptideShakerJScrollPane;
    private javax.swing.JTable peptideShakerJTable;
    private javax.swing.JTable searchEngineTable;
    private javax.swing.JScrollPane searchEnginetableJScrollPane;
    private javax.swing.JPanel spectrumChartJPanel;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableJScrollPane;
    private javax.swing.JButton vennDiagramButton;
    private javax.swing.JTable xTandemTable;
    private javax.swing.JScrollPane xTandemTableJScrollPane;
    // End of variables declaration//GEN-END:variables
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The spectrum collection
     */
    private SpectrumCollection spectrumCollection;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The spectra indexed by their file name
     */
    private HashMap<String, ArrayList<String>> filesMap = new HashMap<String, ArrayList<String>>();

    /**
     * Displays the results on the panel
     */
    public void displayResults() {
        spectrumCollection = peptideShakerGUI.getSpectrumCollection();
        identification = peptideShakerGUI.getIdentification();
        int m = 0;
        int o = 0;
        int x = 0;
        int mo = 0;
        int mx = 0;
        int ox = 0;
        int omx = 0;
        boolean mascot, omssa, xTandem;
        PSParameter probabilities = new PSParameter();

        for (SpectrumMatch spectrumMatch : identification.getSpectrumIdentification().values()) {
            mascot = false;
            omssa = false;
            xTandem = false;
            probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);
            if (probabilities.isValidated()) {
                if (spectrumMatch.getFirstHit(Advocate.MASCOT) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.MASCOT).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        mascot = true;
                    }
                }
                if (spectrumMatch.getFirstHit(Advocate.OMSSA) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.OMSSA).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        omssa = true;
                    }
                }
                if (spectrumMatch.getFirstHit(Advocate.XTANDEM) != null) {
                    if (spectrumMatch.getFirstHit(Advocate.XTANDEM).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                        xTandem = true;
                    }
                }
            }

            if (mascot && omssa && xTandem) {
                omx++;
            } else if (mascot && omssa) {
                mo++;
            } else if (omssa && xTandem) {
                ox++;
            } else if (mascot && xTandem) {
                mx++;
            } else if (mascot) {
                m++;
            } else if (omssa) {
                o++;
            } else if (xTandem) {
                x++;
            }
        }

        int nMascot = omx + mo + mx + m;
        int nOMSSA = omx + mo + ox + o;
        int nXTandem = omx + mx + ox + x;

        double biggestValue = Math.max(Math.max(nMascot, nOMSSA), nXTandem);

        updateVennDiagram(nOMSSA, nXTandem, nMascot,
                (ox + omx), (mo + omx), (mx + omx), omx,
                biggestValue, "OMSSA", "X!Tandem", "Mascot");

        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    "OMSSA",
                    nOMSSA, o, nOMSSA, ox + omx, mo + omx, omx
                });
        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    "X!Tandem",
                    nXTandem, x, ox + omx, nXTandem, mx + omx, omx
                });
        ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                    "Mascot",
                    nMascot, m, mo + omx, mx + omx, nMascot, omx
                });


        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).setMaxValue(biggestValue);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).setMaxValue(biggestValue);


        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                searchEngineTable.revalidate();
                searchEngineTable.repaint();
            }
        });

        String fileName;
        for (String key : spectrumCollection.getAllKeys()) {
            fileName = Spectrum.getSpectrumFile(key);
            if (!filesMap.containsKey(fileName)) {
                filesMap.put(fileName, new ArrayList<String>());
            }
            filesMap.get(fileName).add(key);
        }
        String[] filesArray = new String[filesMap.keySet().size()];
        int cpt = 0;
        for (String tempName : filesMap.keySet()) {
            filesArray[cpt] = tempName;
            cpt++;
        }
        fileNamesCmb.setModel(new DefaultComboBoxModel(filesArray));

        fileSelectionChanged();
    }

    private void updateVennDiagram(double a, double b, double c, double ab, double ac, double bc, double abc,
            double maxValue, String titleA, String titleB, String titleC) {

        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, c / maxValue, ab / maxValue, ac / maxValue, bc / maxValue, abc / maxValue);
        //chart.setTitle("Venn Diagram", Color.WHITE, 16);
        chart.setSize(vennDiagramButton.getWidth(), vennDiagramButton.getHeight());
        chart.setCircleLegends(titleA, titleB, titleC);
        chart.setCircleColors(Color.YELLOW, Color.RED, Color.BLUE);
        chart.setBackgroundFill(Fills.newSolidFill(Color.WHITE));

        try {

            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));
            vennDiagramButton.setIcon(icon);

            vennDiagramButton.setToolTipText("<html>"
                    + titleA + ": " + a + "<br>"
                    + titleB + ": " + b + "<br>"
                    + titleC + ": " + c + "<br><br>"
                    + titleA + " & " + titleB + ": " + ab + "<br>"
                    + titleA + " & " + titleC + ": " + ac + "<br>"
                    + titleB + " & " + titleC + ": " + bc + "<br><br>"
                    + titleA + " & " + titleB + " & " + titleC + ": " + abc
                    + "</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateVennDiagram(int a, int b, int c, int ab, int ac, int bc, int abc,
            double maxValue, String titleA, String titleB, String titleC) {

        // @TODO: add test of internet is not available

        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, c / maxValue, ab / maxValue, ac / maxValue, bc / maxValue, abc / maxValue);
        chart.setSize(vennDiagramButton.getWidth(), vennDiagramButton.getHeight());
        chart.setCircleLegends(titleA, titleB, titleC);
        chart.setCircleColors(Color.YELLOW, Color.RED, Color.BLUE);
        chart.setBackgroundFill(Fills.newSolidFill(Color.WHITE));

        try {
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));
            vennDiagramButton.setIcon(icon);

            vennDiagramButton.setToolTipText("<html>"
                    + titleA + ": " + a + "<br>"
                    + titleB + ": " + b + "<br>"
                    + titleC + ": " + c + "<br><br>"
                    + titleA + " & " + titleB + ": " + ab + "<br>"
                    + titleA + " & " + titleC + ": " + ac + "<br>"
                    + titleB + " & " + titleC + ": " + bc + "<br><br>"
                    + titleA + " & " + titleB + " & " + titleC + ": " + abc
                    + "</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method called whenever the file selection changed
     */
    private void fileSelectionChanged() {
        try {
            while (spectrumTable.getRowCount() > 0) {
                ((DefaultTableModel) spectrumTable.getModel()).removeRow(0);
            }

            String fileSelected = (String) fileNamesCmb.getSelectedItem();

            int maxCharge = Integer.MIN_VALUE;
            double maxMz = Double.MIN_VALUE;

            double lLowRT = Double.MAX_VALUE;
            double lHighRT = Double.MIN_VALUE;

            for (String spectrumKey : filesMap.get(fileSelected)) {
                MSnSpectrum spectrum = (MSnSpectrum) spectrumCollection.getSpectrum(spectrumKey);
                Precursor precursor = spectrum.getPrecursor();
                ((DefaultTableModel) spectrumTable.getModel()).addRow(new Object[]{
                            spectrum.getSpectrumTitle(),
                            precursor.getMz(),
                            precursor.getCharge().value,
                            precursor.getRt()
                        });

                if (precursor.getCharge().value > maxCharge) {
                    maxCharge = precursor.getCharge().value;
                }

                if (lLowRT > precursor.getRt()) {
                    lLowRT = precursor.getRt();
                }

                if (lHighRT < precursor.getRt()) {
                    lHighRT = precursor.getRt();
                }

                if (precursor.getMz() > maxMz) {
                    maxMz = precursor.getMz();
                }
            }

            lLowRT -= 1.0;
            double widthOfMarker = (lHighRT / lLowRT) * 4;

            ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).setMaxValue(maxCharge);
            ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMz);

            JSparklinesIntervalChartTableCellRenderer lRTCellRenderer = new JSparklinesIntervalChartTableCellRenderer(
                    PlotOrientation.HORIZONTAL, lLowRT - widthOfMarker / 2, lHighRT + widthOfMarker / 2, widthOfMarker,
                    peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor());
            spectrumTable.getColumn("RT").setCellRenderer(lRTCellRenderer);
            lRTCellRenderer.showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

            spectrumTable.setRowSelectionInterval(0, 0);
            spectrumSelectionChanged();
        } catch (MzMLUnmarshallerException e) {
            JOptionPane.showMessageDialog(this, "Error while importing mzML data.", "Peak Lists Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Method called whenever the spectrum selection changed
     */
    private void spectrumSelectionChanged() {
        String key = Spectrum.getSpectrumKey((String) fileNamesCmb.getSelectedItem(), (String) spectrumTable.getValueAt(spectrumTable.getSelectedRow(), 0));
        SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(key);
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) spectrumMatch.getUrParam(probabilities);

        while (peptideShakerJTable.getRowCount() > 0) {
            ((DefaultTableModel) peptideShakerJTable.getModel()).removeRow(0);
        }
        while (omssaTable.getRowCount() > 0) {
            ((DefaultTableModel) omssaTable.getModel()).removeRow(0);
        }
        while (mascotTable.getRowCount() > 0) {
            ((DefaultTableModel) mascotTable.getModel()).removeRow(0);
        }
        while (xTandemTable.getRowCount() > 0) {
            ((DefaultTableModel) xTandemTable.getModel()).removeRow(0);
        }

        MSnSpectrum currentSpectrum = null;

        spectrumChartJPanel.removeAll();

        if (spectrumTable.getSelectedRow() != -1) {

            try {

                currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(spectrumMatch.getKey());

                Precursor precursor = currentSpectrum.getPrecursor();
                SpectrumPanel spectrumB = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), precursor.getCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrumB.setBorder(null);

                spectrumChartJPanel.add(spectrumB);

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        spectrumChartJPanel.revalidate();
                        spectrumChartJPanel.repaint();
                    }
                });

            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
            }
        }

        // Fill peptide shaker table
        String proteins = "";
        for (Protein protein : spectrumMatch.getBestAssumption().getPeptide().getParentProteins()) {
            proteins += protein.getAccession() + " ";
        }
        String modifications = "";
        boolean firstline = true;
        for (ModificationMatch modificationMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
            if (!firstline) {
                modifications += ", ";
            } else {
                firstline = false;
            }
            modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
        }
        ((DefaultTableModel) peptideShakerJTable.getModel()).addRow(new Object[]{
                    proteins,
                    spectrumMatch.getBestAssumption().getPeptide().getSequence(),
                    modifications,
                    probabilities.getPsmScore(),
                    probabilities.getPsmConfidence(),
                    0,
                    probabilities.isValidated()
                });

        // Fill Mascot table
        if (spectrumMatch.getAllAssumptions(Advocate.MASCOT) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
            Collections.sort(eValues);
            PeptideAssumption currentAssumption;
            int rank = 0;
            for (double eValue : eValues) {
                currentAssumption = spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValue);
                proteins = "";
                for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                    proteins += protein.getAccession() + " ";
                }
                modifications = "";
                firstline = true;
                for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                    if (!firstline) {
                        modifications += ", ";
                    } else {
                        firstline = false;
                    }
                    modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                }
                ((DefaultTableModel) mascotTable.getModel()).addRow(new Object[]{
                            ++rank,
                            proteins,
                            currentAssumption.getPeptide().getSequence(),
                            modifications,
                            currentAssumption.getEValue(),
                            0
                        });
            }

            if (mascotTable.getRowCount() > 0) {
            }
        }

        // Fill OMSSA table

        if (spectrumMatch.getAllAssumptions(Advocate.OMSSA) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
            Collections.sort(eValues);
            PeptideAssumption currentAssumption;
            int rank = 0;
            for (double eValue : eValues) {
                currentAssumption = spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValue);
                proteins = "";
                for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                    proteins += protein.getAccession() + " ";
                }
                modifications = "";
                firstline = true;
                for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                    if (!firstline) {
                        modifications += ", ";
                    } else {
                        firstline = false;
                    }
                    modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                }
                ((DefaultTableModel) omssaTable.getModel()).addRow(new Object[]{
                            ++rank,
                            proteins,
                            currentAssumption.getPeptide().getSequence(),
                            modifications,
                            currentAssumption.getEValue(),
                            0
                        });
            }
        }

        // Fill X!Tandem table

        if (spectrumMatch.getAllAssumptions(Advocate.XTANDEM) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
            Collections.sort(eValues);
            PeptideAssumption currentAssumption;
            int rank = 0;
            for (double eValue : eValues) {
                currentAssumption = spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValue);
                proteins = "";
                for (Protein protein : currentAssumption.getPeptide().getParentProteins()) {
                    proteins += protein.getAccession() + " ";
                }
                modifications = "";
                firstline = true;
                for (ModificationMatch modificationMatch : currentAssumption.getPeptide().getModificationMatches()) {
                    if (!firstline) {
                        modifications += ", ";
                    } else {
                        firstline = false;
                    }
                    modifications += modificationMatch.getTheoreticPtm().getName() + " (" + modificationMatch.getModificationSite() + ")";
                }
                ((DefaultTableModel) xTandemTable.getModel()).addRow(new Object[]{
                            ++rank,
                            proteins,
                            currentAssumption.getPeptide().getSequence(),
                            modifications,
                            currentAssumption.getEValue(),
                            0
                        });
            }
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptideShakerJTable.revalidate();
                peptideShakerJTable.repaint();
                mascotTable.revalidate();
                mascotTable.repaint();
                xTandemTable.revalidate();
                xTandemTable.repaint();
                omssaTable.revalidate();
                omssaTable.repaint();
            }
        });
    }
}
