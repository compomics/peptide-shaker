package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import eu.isas.peptideshaker.fileimport.MgfFilesNotFoundDialog;
import eu.isas.peptideshaker.gui.interfaces.WaitingHandler;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * A dialog displaying progress details when the identification files are being
 * analyzed.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class WaitingDialog extends javax.swing.JDialog implements WaitingHandler {

    /**
     * Needed for the shaking feature.
     */
    private JDialog dialog;
    /**
     * Used in the shaking feature.
     */
    private Point naturalLocation;
    /**
     * Timer for the shaking feature.
     */
    private Timer shakeTimer;
    /**
     * A reference to the main frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Boolean indicating whether the run is finished
     */
    private boolean runFinished = false;
    /**
     * Boolean indicating whether the run is canceled
     */
    private boolean runCanceled = false;
    /**
     * Convenience date format
     */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
    /**
     * The tab space to add when using tab.
     */
    private String tab = "        "; // tab could be used, but lenght is locale dependent
    /**
     * An array list of the tip of the day.
     */
    private ArrayList<String> tips = new ArrayList<String>();
    /**
     * The current tip index.
     */
    private int currentTipIndex = -1;

    /**
     * Creates a new WaitingDialog.
     *
     * @param peptideShaker a reference to the main frame
     * @param modal
     * @param experimentReference the experiment reference
     */
    public WaitingDialog(PeptideShakerGUI peptideShaker, boolean modal, String experimentReference) {
        super(peptideShaker, modal);
        initComponents();

        setSecondaryProgressDialogIntermediate(true);

        // update the layout in the layered pane
        resizeLayeredPanes();

        // set up the tip of the day
        setUpTipOfTheDay();
        tipOfTheDayEditorPane.setText(getTipOfTheDay());

        this.setLocationRelativeTo(peptideShaker);
        this.peptideShakerGUI = peptideShaker;

        // change the peptide shaker icon to a "waiting version"
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
    }

    /**
     * Set up the list of tip of the day.
     */
    private void setUpTipOfTheDay() {
        try {
            InputStream stream = getClass().getResource("/tips.txt").openStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader b = new BufferedReader(streamReader);
            tips = new ArrayList<String>();
            String line;

            while ((line = b.readLine()) != null) {
                tips.add(line);
            }
        } catch (Exception e) {
            showTipOfTheDayCheckBox.setSelected(false);
            showTipOfTheDayCheckBox.setEnabled(false);
            tipOfTheDayJPanel.setVisible(false);
        }
    }

    /**
     * Set the maximum value of the progress bar.
     *
     * @param maxProgressValue the max value
     */
    public void setMaxProgressValue(int maxProgressValue) {
        progressBar.setMaximum(maxProgressValue);
    }

    /**
     * Increase the progress bar value by one "counter".
     */
    public void increaseProgressValue() {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    /**
     * Increase the progress bar value by the given amount.
     *
     * @param amount the amount to increase the value by
     */
    public void increaseProgressValue(int amount) {
        progressBar.setValue(progressBar.getValue() + amount);
    }

    /**
     * Set the maximum value of the secondary progress bar. And resets the value
     * to 0.
     *
     * @param maxProgressValue the max value
     */
    public void setMaxSecondaryProgressValue(int maxProgressValue) {
        secondaryJProgressBar.setValue(0);
        secondaryJProgressBar.setMaximum(maxProgressValue);
    }

    /**
     * Reset the secondary progress bar value to 0.
     */
    public void resetSecondaryProgressBar() {
        secondaryJProgressBar.setIndeterminate(false);
        secondaryJProgressBar.setStringPainted(true);
        secondaryJProgressBar.setValue(0);
    }

    /**
     * Increase the secondary progress bar value by one "counter".
     */
    public void increaseSecondaryProgressValue() {
        secondaryJProgressBar.setValue(secondaryJProgressBar.getValue() + 1);
    }

    /**
     * Sets the secondary progress bar to the given value.
     *
     * @param value the progress value
     */
    public void setSecondaryProgressValue(int value) {
        secondaryJProgressBar.setValue(value);
    }

    /**
     * Increase the secondary progress bar value by the given amount.
     *
     * @param amount the amount to increase the value by
     */
    public void increaseSecondaryProgressValue(int amount) {
        secondaryJProgressBar.setValue(secondaryJProgressBar.getValue() + amount);
    }

    /**
     * Sets the secondary progress bar to intermediate or not.
     *
     * @param intermediate if true, set to intermediate
     */
    public void setSecondaryProgressDialogIntermediate(boolean intermediate) {

        // this split pane trick should not be needed, but if not used the look and feel of the
        // intermediate progress bar changes when moving back and forth between the two...

        if (intermediate) {
            secondaryProgressBarSplitPane.setDividerLocation(secondaryProgressBarSplitPane.getWidth());
        } else {
            secondaryProgressBarSplitPane.setDividerLocation(0);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        layeredPane = new javax.swing.JLayeredPane();
        reportAreaScrollPane = new javax.swing.JScrollPane();
        reportArea = new javax.swing.JTextArea();
        tipOfTheDayJPanel = new javax.swing.JPanel();
        tipOfTheDayLayeredPane = new javax.swing.JLayeredPane();
        tipOfTheDayScrollPane = new javax.swing.JScrollPane();
        tipOfTheDayEditorPane = new javax.swing.JEditorPane();
        closeJButton = new javax.swing.JButton();
        nextJButton = new javax.swing.JButton();
        secondaryProgressBarSplitPane = new javax.swing.JSplitPane();
        secondaryJProgressBar = new javax.swing.JProgressBar();
        tempJProgressBar = new javax.swing.JProgressBar();
        okButton = new javax.swing.JButton();
        saveReportLabel = new javax.swing.JLabel();
        showTipOfTheDayCheckBox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Importing Data - Please Wait...");
        setMinimumSize(new java.awt.Dimension(500, 500));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(230, 230, 230));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Data Import Progress"));
        jPanel2.setOpaque(false);

        progressBar.setToolTipText("Total Progress");
        progressBar.setStringPainted(true);

        reportArea.setBackground(new java.awt.Color(254, 254, 254));
        reportArea.setColumns(20);
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setRows(5);
        reportAreaScrollPane.setViewportView(reportArea);

        reportAreaScrollPane.setBounds(0, 0, 842, 490);
        layeredPane.add(reportAreaScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        tipOfTheDayJPanel.setOpaque(false);
        tipOfTheDayJPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                tipOfTheDayJPanelComponentResized(evt);
            }
        });

        tipOfTheDayScrollPane.setBorder(new org.jdesktop.swingx.border.DropShadowBorder());
        tipOfTheDayScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tipOfTheDayScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tipOfTheDayScrollPane.setOpaque(false);

        tipOfTheDayEditorPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(240, 240, 240), 20));
        tipOfTheDayEditorPane.setContentType("text/html");
        tipOfTheDayEditorPane.setEditable(false);
        tipOfTheDayEditorPane.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n<body style=\"background-color:#F0F0F0;\">\n    <p style=\"margin-top: 0\" align=\"justify\">\r\n     <b> \rTip of the Day!</b>\n     <br><br>\n     Did you know that. Did you know that. Did you know that. Did you know that. Did you know that. \n     Did you know that.  Did you know that.  Did you know that.  Did you know that.  Did you know that.\n    <br><br>\n    Did you know that.  Did you know that.  Did you know that.  Did you know that.  Did you know that.\n    </p>\r\n  </body>\r\n</html>\r\n");
        tipOfTheDayEditorPane.setOpaque(false);
        tipOfTheDayEditorPane.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                tipOfTheDayEditorPaneHyperlinkUpdate(evt);
            }
        });
        tipOfTheDayScrollPane.setViewportView(tipOfTheDayEditorPane);

        tipOfTheDayScrollPane.setBounds(0, 2, 210, 260);
        tipOfTheDayLayeredPane.add(tipOfTheDayScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/close_grey.png"))); // NOI18N
        closeJButton.setToolTipText("Close");
        closeJButton.setBorderPainted(false);
        closeJButton.setContentAreaFilled(false);
        closeJButton.setIconTextGap(0);
        closeJButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
        closeJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/close.png"))); // NOI18N
        closeJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeJButtonMouseExited(evt);
            }
        });
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeJButtonActionPerformed(evt);
            }
        });
        closeJButton.setBounds(170, 0, 40, 33);
        tipOfTheDayLayeredPane.add(closeJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        nextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next_grey.png"))); // NOI18N
        nextJButton.setToolTipText("Next Tip");
        nextJButton.setBorderPainted(false);
        nextJButton.setContentAreaFilled(false);
        nextJButton.setIconTextGap(0);
        nextJButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
        nextJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next.png"))); // NOI18N
        nextJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nextJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nextJButtonMouseExited(evt);
            }
        });
        nextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextJButtonActionPerformed(evt);
            }
        });
        nextJButton.setBounds(170, 230, 40, 33);
        tipOfTheDayLayeredPane.add(nextJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout tipOfTheDayJPanelLayout = new javax.swing.GroupLayout(tipOfTheDayJPanel);
        tipOfTheDayJPanel.setLayout(tipOfTheDayJPanelLayout);
        tipOfTheDayJPanelLayout.setHorizontalGroup(
            tipOfTheDayJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tipOfTheDayLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
        );
        tipOfTheDayJPanelLayout.setVerticalGroup(
            tipOfTheDayJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tipOfTheDayLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
        );

        tipOfTheDayJPanel.setBounds(610, 200, 210, 270);
        layeredPane.add(tipOfTheDayJPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        secondaryProgressBarSplitPane.setBorder(null);
        secondaryProgressBarSplitPane.setDividerLocation(0);
        secondaryProgressBarSplitPane.setDividerSize(0);

        secondaryJProgressBar.setToolTipText("Current Process Progress");
        secondaryJProgressBar.setStringPainted(true);
        secondaryProgressBarSplitPane.setRightComponent(secondaryJProgressBar);

        tempJProgressBar.setToolTipText("Current Process Progress");
        tempJProgressBar.setIndeterminate(true);
        tempJProgressBar.setString("");
        secondaryProgressBarSplitPane.setLeftComponent(tempJProgressBar);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(layeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 842, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(secondaryProgressBarSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(secondaryProgressBarSplitPane)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(layeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE)
                .addContainerGap())
        );

        okButton.setText("Cancel");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        saveReportLabel.setText("<html><a href=\\\"dummy_link\"><i>Save Report</i></a></html>");
        saveReportLabel.setToolTipText("Save the report to a text file");
        saveReportLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                saveReportLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                saveReportLabelMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                saveReportLabelMouseReleased(evt);
            }
        });

        showTipOfTheDayCheckBox.setSelected(true);
        showTipOfTheDayCheckBox.setText("Show Tip of the Day");
        showTipOfTheDayCheckBox.setToolTipText("Show/Hide Tip of the Day");
        showTipOfTheDayCheckBox.setIconTextGap(10);
        showTipOfTheDayCheckBox.setOpaque(false);
        showTipOfTheDayCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showTipOfTheDayCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(saveReportLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(showTipOfTheDayCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 552, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(saveReportLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showTipOfTheDayCheckBox))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Cancels the analysis if ongoing or opens the results if finished.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (runFinished || runCanceled) {
            this.dispose();
        } else {
            setRunCanceled();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor when hovering above the Save Report
     * link.
     *
     * @param evt
     */
    private void saveReportLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveReportLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_saveReportLabelMouseEntered

    /**
     * Changing the cursor back to the default cursor.
     *
     * @param evt
     */
    private void saveReportLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveReportLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_saveReportLabelMouseExited

    /**
     * Saves the search report to file.
     *
     * @param evt
     */
    private void saveReportLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveReportLabelMouseReleased
        File outputFile = null;
        JFileChooser fc = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("txt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: Text (.txt)";
            }
        };

        fc.setFileFilter(filter);
        int result = fc.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            outputFile = fc.getSelectedFile();
            if (outputFile.exists()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        new String[]{"The file " + outputFile.getName() + " already exists!", "Overwrite?"},
                        "File Already Exists", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                }
            }
        }

        if (outputFile != null) {
            saveReport(outputFile);
        }
}//GEN-LAST:event_saveReportLabelMouseReleased

    /**
     * Update the layout in the layered pane.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                resizeLayeredPanes();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Close the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        okButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Update the tip of the day layered pane.
     *
     * @param evt
     */
    private void tipOfTheDayJPanelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_tipOfTheDayJPanelComponentResized
        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                resizeLayeredPanes();
            }
        });
    }//GEN-LAST:event_tipOfTheDayJPanelComponentResized

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void closeJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closeJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_closeJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void closeJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closeJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_closeJButtonMouseExited

    /**
     * Hide the tip of the day.
     *
     * @param evt
     */
    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        tipOfTheDayJPanel.setVisible(false);
        showTipOfTheDayCheckBox.setSelected(false);
    }//GEN-LAST:event_closeJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void nextJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_nextJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void nextJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_nextJButtonMouseExited

    /**
     * Open the next random tip.
     *
     * @param evt
     */
    private void nextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextJButtonActionPerformed
        tipOfTheDayEditorPane.setText(getTipOfTheDay());
    }//GEN-LAST:event_nextJButtonActionPerformed

    /**
     * Show/hide the tip of the day.
     *
     * @param evt
     */
    private void showTipOfTheDayCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTipOfTheDayCheckBoxActionPerformed
        tipOfTheDayJPanel.setVisible(showTipOfTheDayCheckBox.isSelected());
    }//GEN-LAST:event_showTipOfTheDayCheckBoxActionPerformed

    /**
     * Make the links active.
     *
     * @param evt
     */
    private void tipOfTheDayEditorPaneHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_tipOfTheDayEditorPaneHyperlinkUpdate
        if (evt.getEventType().toString().equalsIgnoreCase(
                javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.toString())) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            BareBonesBrowserLaunch.openURL(evt.getDescription());
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_tipOfTheDayEditorPaneHyperlinkUpdate
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLayeredPane layeredPane;
    private javax.swing.JButton nextJButton;
    private javax.swing.JButton okButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTextArea reportArea;
    private javax.swing.JScrollPane reportAreaScrollPane;
    private javax.swing.JLabel saveReportLabel;
    private javax.swing.JProgressBar secondaryJProgressBar;
    private javax.swing.JSplitPane secondaryProgressBarSplitPane;
    private javax.swing.JCheckBox showTipOfTheDayCheckBox;
    private javax.swing.JProgressBar tempJProgressBar;
    private javax.swing.JEditorPane tipOfTheDayEditorPane;
    private javax.swing.JPanel tipOfTheDayJPanel;
    private javax.swing.JLayeredPane tipOfTheDayLayeredPane;
    private javax.swing.JScrollPane tipOfTheDayScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Set the analysis as finished.
     */
    public void setRunFinished() {
        runFinished = true;
        okButton.setText("OK");
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());
        progressBar.setStringPainted(true);
        this.setTitle("Importing Data - Completed!");

        secondaryProgressBarSplitPane.setDividerLocation(0);
        secondaryJProgressBar.setIndeterminate(false);
        secondaryJProgressBar.setValue(secondaryJProgressBar.getMaximum());
        secondaryJProgressBar.setString("Import Completed!");

        // change the peptide shaker icon back to the default version
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

        // make the dialog shake for a couple of seconds
        startShake();
    }

    /**
     * Set the analysis as canceled.
     */
    public void setRunCanceled() {
        runCanceled = true;
        appendReportEndLine();
        appendReport("Import canceled.");
        okButton.setText("OK");
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        secondaryProgressBarSplitPane.setDividerLocation(0);
        secondaryJProgressBar.setIndeterminate(false);
        secondaryJProgressBar.setValue(0);
        secondaryJProgressBar.setString("Import Canceled!");

        this.setTitle("Importing Data - Canceled");

        // return the peptide shaker icon to the standard version
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
    }

    /**
     * Append text to the report.
     *
     * @param report the text to append
     */
    public void appendReport(String report) {
        Date date = new Date();
        reportArea.append(date + tab + report + "\n");
        reportArea.setCaretPosition(reportArea.getText().length());
    }

    /**
     * Append two tabs to the report. No new line.
     */
    public void appendReportNewLineNoDate() {
        reportArea.append(tab);
        reportArea.setCaretPosition(reportArea.getText().length());
    }

    /**
     * Append a new line to the report.
     */
    public void appendReportEndLine() {
        reportArea.append("\n");
        reportArea.setCaretPosition(reportArea.getText().length());
    }

    /**
     * Returns true if the run is canceled.
     *
     * @return true if the run is canceled
     */
    public boolean isRunCanceled() {
        return runCanceled;
    }

    /**
     * Saves the report in the given file.
     *
     * @param aFile file to save the report in
     */
    private void saveReport(File aFile) {
        StringBuffer output = new StringBuffer();
        String host = " @ ";

        try {
            host += InetAddress.getLocalHost().getHostName();


        } catch (UnknownHostException uhe) {
            // Disregard. It's not so bad if we can not report this.
        }

        // Write the file header.
        output.append("# ------------------------------------------------------------------"
                + "\n# PeptideShaker Report File"
                + "\n#"
                + "\n# Originally saved by: " + System.getProperty("user.name") + host
                + "\n#                  on: " + sdf.format(new Date())
                + "\n#                  as: " + aFile.getName()
                + "\n# ------------------------------------------------------------------\n");

        output.append(reportArea.getText() + "\n");

        BufferedWriter bw = null;

        try {
            String filePath = aFile.getAbsolutePath();

            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }

            bw = new BufferedWriter(new FileWriter(filePath));
            bw.write(output.toString());
            bw.flush();
            JOptionPane.showMessageDialog(this, "Report written to file '" + filePath + "'.", "Report Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(this, new String[]{"Error writing report to file:", ioe.getMessage()}, "Save Failed", JOptionPane.ERROR_MESSAGE);

        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(this, new String[]{"Error writing report to file:", ioe.getMessage()}, "Save Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Make the dialog shake when the analysis has completed.
     */
    private void startShake() {
        final long startTime;

        naturalLocation = this.getLocation();
        startTime = System.currentTimeMillis();

        dialog = this;

        shakeTimer = new Timer(5, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                double TWO_PI = Math.PI * 2.0;
                double SHAKE_CYCLE = 50;

                long elapsed = System.currentTimeMillis() - startTime;
                double waveOffset = (elapsed % SHAKE_CYCLE) / SHAKE_CYCLE;
                double angle = waveOffset * TWO_PI;

                int SHAKE_DISTANCE = 10;

                int shakenX = (int) ((Math.sin(angle) * SHAKE_DISTANCE) + naturalLocation.x);
                dialog.setLocation(shakenX, naturalLocation.y);
                dialog.repaint();

                int SHAKE_DURATION = 1000;

                if (elapsed >= SHAKE_DURATION) {
                    stopShake();
                }
            }
        });
        shakeTimer.start();
    }

    /**
     * Stop the dialog shake.
     */
    private void stopShake() {
        shakeTimer.stop();
        dialog.setLocation(naturalLocation);
        dialog.repaint();

        appendReport("Your peptides have been shaken!");

        // return the peptide shaker icon to the standard version
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
    }

    /**
     * Returns the secondary progress bar for updates from external processes.
     *
     * @return the secondary progress bar
     */
    public JProgressBar getSecondaryProgressBar() {
        return secondaryJProgressBar;
    }

    /**
     * Resize the layered panes.
     */
    private void resizeLayeredPanes() {

        // resize the report area
        layeredPane.getComponent(1).setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

        // move the tip of the day panel
        layeredPane.getComponent(0).setBounds(layeredPane.getWidth() - 255, layeredPane.getHeight() - 300,
                230, 280);

        layeredPane.revalidate();
        layeredPane.repaint();


        // resize the tip of the day panel
        tipOfTheDayLayeredPane.getComponent(2).setBounds(0, 0, tipOfTheDayLayeredPane.getWidth(), tipOfTheDayLayeredPane.getHeight());

        //move the buttons
        tipOfTheDayLayeredPane.getComponent(0).setBounds(
                tipOfTheDayLayeredPane.getWidth() - 40, -2,
                tipOfTheDayLayeredPane.getComponent(0).getWidth(), tipOfTheDayLayeredPane.getComponent(0).getHeight());

        tipOfTheDayLayeredPane.getComponent(1).setBounds(
                tipOfTheDayLayeredPane.getWidth() - 40, tipOfTheDayLayeredPane.getHeight() - 35,
                tipOfTheDayLayeredPane.getComponent(1).getWidth(), tipOfTheDayLayeredPane.getComponent(1).getHeight());

        tipOfTheDayLayeredPane.revalidate();
        tipOfTheDayLayeredPane.repaint();
    }

    /**
     * Returns a random tip of the day text as HTML ready to insert into the tip
     * of the day panel.
     *
     * @return a random tip of the day text as html
     */
    private String getTipOfTheDay() {

        String htmlStart = "<html><head></head><body style=\"background-color:#F0F0F0;\">"
                + " <p style=\"margin-top: 0\" align=\"justify\">"
                + "<b>Tip of the Day!</b><br><br>";

        String htmlEnd = "</p></body></html>";

        int newTipIndex = (int) (Math.random() * tips.size());

        while (newTipIndex == currentTipIndex) {
            newTipIndex = (int) (Math.random() * tips.size());
        }

        currentTipIndex = newTipIndex;

        return htmlStart + tips.get(currentTipIndex) + htmlEnd;
    }

    public void displayMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    public void displayHtmlMessage(JEditorPane messagePane, String title, int messageType) {
        JOptionPane.showMessageDialog(this, messagePane, title, messageType);
    }
    
    public void displayMissingMgfFilesMessage(HashMap<File, String> missingMgfFiles) {
        new MgfFilesNotFoundDialog(this, missingMgfFiles);
    }
    
    @Override
    public void setWaitingText(String text) {
        // ignore, not implemented for this waiting handler
    }
}
