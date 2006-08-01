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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.colorchooser.DefaultColorSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Color manager dialog which shows all columns of the input data and its
 * corresponding values inside two combo boxes divided by range and nominal
 * ones. The color chooser can then be used to select certain colors for each
 * value for one attribute value or range, min or max. If the attribute changes,
 * the color settings are locally saved. During save the settings are saved by
 * the underlying {@link java.awt.image.ColorModel} which in turn a read by the
 * model.
 * 
 * @see ColorManagerNodeModel
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManagerNodeDialogPane extends NodeDialogPane implements
        ItemListener {

    /** Keeps all columns. */
    private final JComboBox m_columns = new JComboBox();

    /** Nominal column. */
    private final JRadioButton m_buttonNominal = new JRadioButton("Nominal");

    /** Range column. */
    private final JRadioButton m_buttonRange = new JRadioButton("Range");

    /** Nominal color panel. */
    private final ColorManagerDialogNominal m_nominal;

    /** Range color panel. */
    private final ColorManagerDialogRange m_range;

    /**
     * Creates a new color manager dialog; all color settings are empty.
     */
    ColorManagerNodeDialogPane() {
        // create new super node dialog with name
        super();

        m_columns.setRenderer(new DataColumnSpecListCellRenderer());
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.setBorder(BorderFactory
                .createTitledBorder(" Select one Column "));
        columnPanel.add(m_columns);

        // init nominal and range color selection dialog
        m_nominal = new ColorManagerDialogNominal();
        m_range = new ColorManagerDialogRange();

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_buttonNominal);
        buttonGroup.add(m_buttonRange);

        /**
         * Overwrite default color selection model to throw color event even if
         * the color is the same and no event was created.
         */
        class MyColorSelectionModel extends DefaultColorSelectionModel {
            /**
             * @param color to set.
             * @see DefaultColorSelectionModel#setSelectedColor(java.awt.Color)
             */
            @Override
            public void setSelectedColor(final Color color) {
                super.setSelectedColor(color);
                if (color != null) {
                    colorChanged(color);
                }
            }
        }
        // init color chooser and the value combo box
        final JColorChooser jcc = new JColorChooser(new MyColorSelectionModel());
        // combo holding the values for a certain column
        final Color dftColor = ColorAttr.DEFAULT.getColor();
        jcc.setColor(dftColor);
        // remove preview
        jcc.setPreviewPanel(new JPanel());

        JPanel nominalPanel = new JPanel(new BorderLayout());
        nominalPanel.setBorder(BorderFactory.createTitledBorder(""));
        nominalPanel.add(m_buttonNominal, BorderLayout.NORTH);
        nominalPanel.add(m_nominal, BorderLayout.CENTER);

        JPanel rangePanel = new JPanel(new BorderLayout());
        rangePanel.setBorder(BorderFactory.createTitledBorder(""));
        rangePanel.add(m_buttonRange, BorderLayout.NORTH);
        rangePanel.add(m_range, BorderLayout.CENTER);

        // center panel that is added to the dialog pane's tabs
        JPanel center = new JPanel(new BorderLayout());
        center.add(columnPanel, BorderLayout.NORTH);
        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.add(nominalPanel);
        listPanel.add(rangePanel);
        center.add(listPanel, BorderLayout.CENTER);
        center.add(jcc, BorderLayout.SOUTH);
        super.addTab(" Color Settings ", center);
    }

    /* If the color has changed by the color chooser. */
    private void colorChanged(final Color color) {
        String cell = getSelectedItem();
        if (cell == null) {
            return;
        }
        if (m_buttonNominal.isSelected()) {
            m_nominal.update(cell, color);
        } else {
            if (m_buttonRange.isSelected()) {
                m_range.update(cell, color);
            }
        }
    }

    /**
     * Updates this dialog by refreshing all components in the color tab. Inits
     * the column name combo box and sets the values for the default selected
     * one.
     * 
     * @param settings the settings to load
     * @param specs the input table specs
     * @throws NotConfigurableException if no column found for color selection
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // remove all columns
        m_columns.removeItemListener(this);
        m_columns.removeAllItems();
        // reset nominal and range panel
        m_nominal.removeAllElements();
        m_range.removeAllElements();

        int hasNominals = -1;
        int hasRanges = -1;
        // read settings and write into the map
        String target = settings.getString(
                ColorManagerNodeModel.SELECTED_COLUMN, null);
        // null = not specified, true = nominal, and false = range
        Boolean nominalSelected = null;
        try {
            nominalSelected = settings
                    .getBoolean(ColorManagerNodeModel.IS_NOMINAL);
        } catch (InvalidSettingsException ise) {
            ColorManagerNodeModel.LOGGER.debug("Nominal/Range selection flag"
                    + " not available.");
        }

        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            DataColumnDomain domain = cspec.getDomain();
            if (domain.hasValues()) {
                m_nominal.add(cspec.getName(), domain.getValues());
                // select last possible nominal column
                hasNominals = i;
            }
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                DataCell lower = domain.getLowerBound();
                DataCell upper = domain.getUpperBound();
                m_range.add(cspec.getName(), lower, upper);
                if (hasRanges == -1) { // select first range column found
                    hasRanges = i;
                }
            }
        }

        // check for not configurable
        if (hasNominals == -1 && hasRanges == -1) {
            throw new NotConfigurableException("Please provide input table"
                    + " with at least one column with either nominal and/or"
                    + " lower and upper bounds defined.");
        }

        // update selected column index
        if (target == null
                || !specs[0].containsName(target)
                || (!specs[0].getColumnSpec(target).getDomain().hasValues() && !specs[0]
                        .getColumnSpec(target).getDomain().hasBounds())) {
            // select first nominal column if nothing could be selected
            if (hasNominals > -1) {
                target = specs[0].getColumnSpec(hasNominals).getName();
                nominalSelected = true;
            } else {
                if (hasRanges > -1) { // otherwise the first range column
                    target = specs[0].getColumnSpec(hasRanges).getName();
                    nominalSelected = false;
                } else {
                    target = null;
                    nominalSelected = null;
                }
            }
        } else {
            if (nominalSelected == null) {
                nominalSelected = specs[0].getColumnSpec(target).getDomain()
                        .hasValues();
            } else {
                if (nominalSelected
                        && !specs[0].getColumnSpec(target).getDomain()
                                .hasValues()) {
                    nominalSelected = false;
                } else {
                    if (!nominalSelected
                            && !specs[0].getColumnSpec(target).getDomain()
                                    .hasBounds()) {
                        nominalSelected = true;
                    }
                }
            }
        }

        // nominal
        if (hasNominals > -1) {
            m_nominal.loadSettings(settings, target);
            if (nominalSelected) {
                m_nominal.select(target);
            }
        } else {
            m_nominal.select(null);
        }
        // range
        if (hasRanges > -1) {
            m_range.loadSettings(settings, target);
            if (!nominalSelected) {
                m_range.select(target);
            }
        } else {
            m_range.select(null);
        }

        // add columns
        int cols = specs[0].getNumColumns();
        for (int i = 0; i < cols; i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            m_columns.addItem(cspec);
        }
        m_columns.addItemListener(this);
        m_columns.setSelectedIndex(specs[0].findColumnIndex(target));
    }

    /**
     * Method is invoked by the super class in order to force the dialog to
     * apply its changes.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if either nominal or range selection
     *             could not be saved
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        String cell = getSelectedItem();
        settings.addString(ColorManagerNodeModel.SELECTED_COLUMN, cell);
        if (cell != null) {
            if (m_buttonNominal.isSelected() && m_buttonNominal.isEnabled()) {
                settings.addBoolean(ColorManagerNodeModel.IS_NOMINAL, true);
                m_nominal.saveSettings(settings);
            } else {
                if (m_buttonRange.isSelected() && m_buttonRange.isEnabled()) {
                    settings
                            .addBoolean(ColorManagerNodeModel.IS_NOMINAL, false);
                    m_range.saveSettings(settings);
                } else {
                    throw new InvalidSettingsException("No color settings for "
                            + cell + " available.");
                }
            }
        }
    }

    /**
     * @param e the source event
     * @see ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(final ItemEvent e) {
        Object o = (DataColumnSpec)e.getItem();
        if (o == null) {
            return;
        }
        String cell = ((DataColumnSpec)o).getName();
        boolean hasRanges = m_range.select(cell);
        if (hasRanges) {
            m_buttonRange.setEnabled(true);
            m_buttonRange.setSelected(true);
        } else {
            m_buttonRange.setEnabled(false);
        }
        boolean hasNominal = m_nominal.select(cell);
        if (hasNominal) {
            m_buttonNominal.setEnabled(true);
            m_buttonNominal.setSelected(true);
        } else {
            m_buttonNominal.setEnabled(false);
        }
    }

    /* Find selected column in button group. */
    private String getSelectedItem() {
        Object o = m_columns.getSelectedItem();
        if (o == null) {
            return null;
        }
        return ((DataColumnSpec)o).getName();
    }
}
