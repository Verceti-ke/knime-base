/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * A dialog to apply data to basis functions. Can be used to set a name for the
 * new, applied column.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionPredictorNodeDialog extends NodeDialogPane {
    /** Prediction column. */
    private final JTextField m_apply = new JTextField();

    private final JRadioButton m_dftButton;

    private final JRadioButton m_setButton;
    
    private final JCheckBox m_ignButton;

    private final JSpinner m_dontKnow;

    /** Key for the applied column: <i>apply_column</i>. */
    public static final String APPLY_COLUMN = "apply_column";

    /** Key for don't know probability for the unknown class. */
    public static final String DONT_KNOW_PROP = "dont_know_prop";
    
    /** Config key if dont know should be ignored. */
    public static final String CFG_DONT_KNOW_IGNORE = "ignore_dont_know";

    /**
     * Creates a new predictor dialog to set a name for the applied column.
     */
    public BasisFunctionPredictorNodeDialog() {
        super();
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setPreferredSize(new Dimension(200, 150));

        // add apply column
        m_apply.setPreferredSize(new Dimension(175, 25));
        JPanel normPanel = new JPanel();
        normPanel.setBorder(BorderFactory.createTitledBorder(" Choose Name "));
        normPanel.add(m_apply);
        p.add(normPanel);

        // add don't know probability
        m_dftButton = new JRadioButton("Default ", true);
        m_setButton = new JRadioButton("Use ");
        m_ignButton = new JCheckBox("Ignore ", true);
        m_ignButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                selectionChanged();
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_dftButton);
        bg.add(m_setButton);
        m_dontKnow = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_dontKnow.setEditor(new JSpinner.NumberEditor(
                m_dontKnow, "#.##########"));
        m_dontKnow.setPreferredSize(new Dimension(75, 25));
        JPanel dontKnowPanel = new JPanel(new BorderLayout());
        dontKnowPanel.setBorder(BorderFactory
                .createTitledBorder(" Don't Know Class "));
        dontKnowPanel.add(m_ignButton, BorderLayout.NORTH);
        JPanel dftPanel = new JPanel(new FlowLayout());
        dftPanel.setBorder(BorderFactory
                .createTitledBorder(""));
        dftPanel.add(m_dftButton);
        dftPanel.add(m_setButton);
        dftPanel.add(m_dontKnow);
        dontKnowPanel.add(dftPanel, BorderLayout.CENTER);
        p.add(dontKnowPanel);

        m_dftButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(false);
            }
        });

        m_setButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(true);
            }
        });

        // add fuzzy learner tab
        super.addTab("Applied Column", p);
    }
    
    private void selectionChanged() {
        if (m_ignButton.isSelected()) {
            m_dftButton.setEnabled(false);
            m_setButton.setEnabled(false);
            m_dontKnow.setEnabled(false);
        } else {
            m_dftButton.setEnabled(true);
            m_setButton.setEnabled(true);
            if (m_dftButton.isSelected()) {
                m_dontKnow.setEnabled(false);
            } else {
                m_dontKnow.setEnabled(true);
            }
        }
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // prediction column name
        String apply = settings.getString(APPLY_COLUMN, "");
        m_apply.setText(apply);
        if (settings.getBoolean(CFG_DONT_KNOW_IGNORE, false)) {
            m_ignButton.setSelected(true);
            m_dontKnow.setValue(new Double(0.0));
        } else {
            m_ignButton.setSelected(false);
            double value = settings.getDouble(DONT_KNOW_PROP, -1.0);
            if (value < 0.0) {
                m_dftButton.setSelected(true);
                m_dontKnow.setValue(new Double(0.0));
            } else {
                m_setButton.setSelected(true);
                m_dontKnow.setValue(new Double(value));
            }
        }
        selectionChanged();
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // prediction column name
        String s = m_apply.getText().trim();
        if (s.length() == 0) {
            throw new InvalidSettingsException("Empty name not allowed.");
        }
        settings.addString(APPLY_COLUMN, s);
        if (m_ignButton.isSelected()) {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, true);
            settings.addDouble(DONT_KNOW_PROP, 0.0);
        } else {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, false);
            if (m_dftButton.isSelected()) {
                settings.addDouble(DONT_KNOW_PROP, -1.0);
            } else {
                Double value = (Double)m_dontKnow.getValue();
                settings.addDouble(DONT_KNOW_PROP, value.doubleValue());
            }
        }
    }
}
