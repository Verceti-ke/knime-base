/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   08.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowNoRowFilter;
import de.unikn.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class RowNoRowFilterPanel extends RowFilterPanel {

    /** object version for serialization. */
    static final long serialVersionUID = 1;

    private final JSpinner m_first;

    private final JCheckBox m_tilEOT;

    private final JSpinner m_last;

    private final JLabel m_errText;

    /**
     * Creates a panel containing controls to adjust settings for a row number
     * range filter.
     */
    RowNoRowFilterPanel() {
        super(400, 350);

        m_errText = new JLabel("");
        m_errText.setForeground(Color.RED);
        m_first = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE,
                10));
        m_first.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        m_tilEOT = new JCheckBox("to the end of the table");
        m_tilEOT.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                tilEOTChanged();
            }
        });
        m_last = new JSpinner(new SpinnerNumberModel(1000, 1,
                Integer.MAX_VALUE, 10));
        m_last.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Row number range:"));

        Box labelBox = Box.createVerticalBox();
        labelBox.add(Box.createHorizontalStrut(300));
        Box textBox = Box.createHorizontalBox();
        textBox.add(Box.createHorizontalGlue());
        labelBox.add(textBox);

        Box firstBox = Box.createHorizontalBox();
        firstBox.add(new JLabel("first row number:"));
        firstBox.add(m_first);
        firstBox.add(Box.createHorizontalGlue());

        Box eotBox = Box.createHorizontalBox();
        eotBox.add(m_tilEOT);
        eotBox.add(Box.createHorizontalGlue());

        Box lastBox = Box.createHorizontalBox();
        lastBox.add(new JLabel("last row number:"));
        lastBox.add(m_last);
        lastBox.add(Box.createHorizontalGlue());

        Box errBox = Box.createHorizontalBox();
        errBox.add(m_errText);
        errBox.add(Box.createHorizontalGlue());

        panel.add(labelBox);
        panel.add(firstBox);
        panel.add(eotBox);
        panel.add(lastBox);
        panel.add(Box.createVerticalStrut(7));
        panel.add(errBox);
        panel.add(Box.createVerticalGlue()); // do we need some glue here?!?

        this.add(panel);
    }

    /**
     * sets the enabled status of the 'lastRow' spinner depending on the checked
     * status of the 'until the end' box.
     */
    protected void tilEOTChanged() {
        m_last.setEnabled(!m_tilEOT.isSelected());
        updateErrText();
    }

    /**
     * @see RowFilterPanel#loadSettingsFromFilter(RowFilter)
     */
    @Override
    public void loadSettingsFromFilter(final RowFilter filter)
            throws InvalidSettingsException {
        if (!(filter instanceof RowNoRowFilter)) {
            throw new InvalidSettingsException("Range filter can only load "
                    + "settings from a RowNumberFilter");
        }

        RowNoRowFilter rowNumberFilter = (RowNoRowFilter)filter;
        // do some consistency checks
        int first = rowNumberFilter.getFirstRow();
        int last = rowNumberFilter.getLastRow();
        if (first < 0) {
            throw new InvalidSettingsException("The RowNumberFilter range "
                    + "cannot start at a row number less than 1.");
        }
        if ((last != RowNoRowFilter.EOT) && (last < first)) {
            throw new InvalidSettingsException("The end of the RowNumberFilter"
                    + " range must be greater than the start.");
        }

        // the filter contains index values (starting from 0)
        // the spinner show the numbers, so we need to add 1 here.
        m_first.setValue(new Integer(first + 1));
        m_tilEOT.setSelected(last == RowNoRowFilter.EOT); // en/disables
                                                            // m_last
        if (last != RowNoRowFilter.EOT) {
            m_last.setValue(new Integer(last + 1));
        }
        updateErrText();
    }

    /**
     * @see de.unikn.knime.base.node.filter.row.RowFilterPanel
     *      #createFilter(boolean)
     */
    @Override
    public RowFilter createFilter(final boolean include)
            throws InvalidSettingsException {
        // just in case, because the err text is the indicator for err existence
        updateErrText();

        if (hasErrors()) {
            throw new InvalidSettingsException(m_errText.getText());
        }

        int start = readIntSpinner(m_first) - 1;
        int last = RowNoRowFilter.EOT;
        if (!m_tilEOT.isSelected()) {
            last = readIntSpinner(m_last) - 1;
        }
        return new RowNoRowFilter(start, last, include);
    }

    /*
     * sets a message in the error label if settings are not valid
     */
    private void updateErrText() {
        int first = readIntSpinner(m_first);
        int last = readIntSpinner(m_last);

        m_errText.setText("");

        if (first < 1) {
            m_errText.setText("The first row number of the range"
                    + " can't be smaller than 1.");
        }
        if ((!m_tilEOT.isSelected()) && (last < first)) {
            m_errText.setText("The row number range"
                    + " end must be larger than the start.");
        }

    }

    /**
     * @return true if the settings in the panel are invalid, false if they are
     *         consistent and usable.
     */
    public boolean hasErrors() {
        return m_errText.getText().length() > 0;
    }

    /**
     * @return a message to the user if hasErrors returns true
     */
    public String getErrMsg() {
        return m_errText.getText();
    }

    /*
     * read the current value from the spinner assuming it contains Integers
     */
    private int readIntSpinner(final JSpinner intSpinner) {
        try {
            intSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)intSpinner.getModel();
        return snm.getNumber().intValue();
    }
}
