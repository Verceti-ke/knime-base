/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * 
 * History
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Dialog pane used tpo specify colors by minimum and maximum bounds.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorManager2DialogRange extends JPanel {
    private class DataCellColorEntry {
        private final DataCell m_cell;

        private Color m_color;

        /**
         * Create new cell and color entry.
         * 
         * @param cell the cell
         * @param color the color
         */
        DataCellColorEntry(final DataCell cell, final Color color) {
            assert color != null;
            m_color = color;
            m_cell = cell;

        }

        /**
         * @return the cell
         */
        DataCell getCell() {
            return m_cell;
        }

        /**
         * @param color the new color to set
         */
        void setColor(final Color color) {
            m_color = color;
        }

        /**
         * @return the color
         */
        Color getColor() {
            return m_color;
        }
    }

    /** Keeps mapping from data cell name to color. */
    private final LinkedHashMap<String, DataCellColorEntry[]> m_map;

    /** Keeps the all possible column values. */
    private final JList m_columnValues;

    private final DefaultListModel m_columnModel;

    private final ColorManager2RangeIcon m_rangeLabel;

    /**
     * Creates a new empty dialog pane.
     */
    ColorManager2DialogRange() {
        super(new BorderLayout());
        m_rangeLabel = new ColorManager2RangeIcon();

        // map for key to color mapping
        m_map = new LinkedHashMap<String, DataCellColorEntry[]>();

        // create list for possible column values
        m_columnModel = new DefaultListModel();
        m_columnValues = new JList(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManager2IconRenderer());
        JPanel rangePanel = new JPanel(new GridLayout());
        rangePanel.add(new JScrollPane(m_columnValues));
        super.add(rangePanel, BorderLayout.CENTER);
        JPanel rangeBorder = new JPanel(new GridLayout());
        rangeBorder.setBorder(BorderFactory.createTitledBorder(" Preview "));
        rangeBorder.add(m_rangeLabel);
        super.add(rangeBorder, BorderLayout.SOUTH);

    }

    /**
     * Select new color for the selected attribute value of the the selected
     * column.
     * 
     * @param column the selected column
     * @param color the new color
     */
    void update(final String column, final Color color) {
        int idx = m_columnValues.getSelectedIndex();
        if (idx == 0 || idx == 1) {
            ColorManager2Icon icon = (ColorManager2Icon)m_columnValues
                    .getSelectedValue();
            icon.setColor(color);
            DataCell cell = icon.getCell();
            DataCellColorEntry ex = new DataCellColorEntry(cell, color);
            DataCellColorEntry[] e = m_map.get(column);
            assert e.length == 2;
            if (idx == 0) {
                m_map.put(column, new DataCellColorEntry[]{ex, e[1]});
                m_rangeLabel.setMinColor(color);
            }
            if (idx == 1) {
                m_map.put(column, new DataCellColorEntry[]{e[0], ex});
                m_rangeLabel.setMaxColor(color);
            }
            super.validate();
            super.repaint();
        }
    }

    /**
     * Called if the column selection has changed.
     * 
     * @param column the new selected column
     * @return <code>true</code>, if this call caused any changes
     */
    boolean select(final String column) {
        m_columnModel.removeAllElements();
        Object o = m_map.get(column);
        boolean flag;
        if (o == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            m_rangeLabel.setMinColor(ColorAttr.DEFAULT.getColor());
            m_rangeLabel.setMaxColor(ColorAttr.DEFAULT.getColor());
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            DataCellColorEntry[] e = (DataCellColorEntry[])o;
            m_columnModel.addElement(new ColorManager2Icon(e[0].getCell(),
                    "min=", e[0].getColor()));
            m_rangeLabel.setMinColor(e[0].getColor());
            m_columnModel.addElement(new ColorManager2Icon(e[1].getCell(),
                    "max=", e[1].getColor()));
            m_rangeLabel.setMaxColor(e[1].getColor());
            flag = true;
        }
        super.validate();
        super.repaint();
        return flag;
    }

    /**
     * Add new column with lower and upper bound.
     * 
     * @param column the column to add
     * @param low the lower bound
     * @param upp the upper bound
     */
    void add(final String column, final DataCell low, final DataCell upp) {
        DataCellColorEntry e1 = new DataCellColorEntry(low, Color.RED);
        DataCellColorEntry e2 = new DataCellColorEntry(upp, Color.GREEN);
        m_map.put(column, new DataCellColorEntry[]{e1, e2});
    }

    /**
     * Removes all elements.
     */
    void removeAllElements() {
        m_map.clear();
        m_columnModel.removeAllElements();
        this.setEnabled(false);
    }

    /**
     * Writes the color settings.
     * 
     * @param settings to write to
     */
    void saveSettings(final NodeSettingsWO settings) {
        assert m_columnModel.getSize() == 2;
        ColorManager2Icon i0 = (ColorManager2Icon)m_columnModel.getElementAt(0);
        settings.addInt(
                ColorManager2NodeModel.MIN_COLOR, i0.getColor().getRGB());
        ColorManager2Icon i1 = (ColorManager2Icon)m_columnModel.getElementAt(1);
        settings.addInt(
                ColorManager2NodeModel.MAX_COLOR, i1.getColor().getRGB());
    }

    /**
     * Reads color settings.
     * 
     * @param settings to read from
     * @param column the selected column
     */
    void loadSettings(final NodeSettingsRO settings, final String column) {
        if (column == null) {
            return;
        }
        Color c0 = new Color(settings.getInt(ColorManager2NodeModel.MIN_COLOR,
                Color.RED.getRGB()));
        Color c1 = new Color(settings.getInt(ColorManager2NodeModel.MAX_COLOR,
                Color.GREEN.getRGB()));
        DataCellColorEntry[] ex = m_map.get(column);
        if (ex == null) {
            return;
        }
        ex[0].setColor(c0);
        ex[1].setColor(c1);
    }
}
