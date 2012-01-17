/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * JPanelNormalization.java
 *
 * Created on Aug 4, 2010, 4:21:12 PM
 */
package xmipp.viewer.gallery.normalization;

/**
 *
 * @author Juanjo Vega
 */
public class JPanelNormalization extends javax.swing.JPanel {

    protected iNormalizeListener listener;

    /** Creates new form JPanelNormalization */
    public JPanelNormalization(iNormalizeListener listener) {
        super();

        this.listener = listener;

        initComponents();
    }

    public void setMinMax(int min, int max) {
        jsMin.setMinimum(min);
        jsMin.setMaximum(max);
        jsMax.setMinimum(min);
        jsMax.setMaximum(max);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jlMin = new javax.swing.JLabel();
        jsMin = new javax.swing.JSlider();
        jlMax = new javax.swing.JLabel();
        jsMax = new javax.swing.JSlider();
        jlHistogram = new javax.swing.JLabel();
        jpButtons = new javax.swing.JPanel();
        jtbSet = new javax.swing.JToggleButton();
        jtbAuto = new javax.swing.JToggleButton();

        setLayout(new java.awt.GridBagLayout());

        jlMin.setText("Min:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(jlMin, gridBagConstraints);

        jsMin.setPaintLabels(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jsMin, gridBagConstraints);

        jlMax.setText("Max:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        add(jlMax, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jsMax, gridBagConstraints);

        jlHistogram.setIcon(new javax.swing.ImageIcon("/home/juanjo/Desktop/Xmipp_Browser/histogram.png")); // NOI18N
        jlHistogram.setText(" [Histogram]");
        jlHistogram.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jlHistogram.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        add(jlHistogram, gridBagConstraints);

        jpButtons.setLayout(new java.awt.GridLayout(1, 0));

        jtbSet.setText("Set");
        jtbSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtbSetActionPerformed(evt);
            }
        });
        jpButtons.add(jtbSet);

        jtbAuto.setText("Auto");
        jtbAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtbAutoActionPerformed(evt);
            }
        });
        jpButtons.add(jtbAuto);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jpButtons, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jtbSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtbSetActionPerformed
        if (jtbSet.isSelected()) {
            jtbAuto.setSelected(false);

            int min = jsMin.getValue();
            int max = jsMax.getValue();

            listener.normalize(min, max);
        } else {
            listener.disableNormalization();
        }
}//GEN-LAST:event_jtbSetActionPerformed

    private void jtbAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtbAutoActionPerformed
        if (jtbAuto.isSelected()) {
            jtbSet.setSelected(false);

            listener.setNormalizedAuto();
        } else {
            listener.disableNormalization();
        }
}//GEN-LAST:event_jtbAutoActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jlHistogram;
    private javax.swing.JLabel jlMax;
    private javax.swing.JLabel jlMin;
    private javax.swing.JPanel jpButtons;
    private javax.swing.JSlider jsMax;
    private javax.swing.JSlider jsMin;
    private javax.swing.JToggleButton jtbAuto;
    private javax.swing.JToggleButton jtbSet;
    // End of variables declaration//GEN-END:variables
}
