/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.viz.aggregation.util.AggrValModelComparator;
import org.knime.base.node.viz.aggregation.util.GUIUtils;
import org.knime.base.node.viz.pie.util.TooManySectionsException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * The abstract pie data model which provides method to hold the data which
 * should be displayed as a pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class PieDataModel {

    private final boolean m_supportHiliting;

    private final boolean m_detailsAvailable;

    /**Constructor for class AbstractPieDataModel.
     * @param supportHiliting if hiliting is supported
     * @param detailsAvailable <code>true</code> if details are available
     */
    protected PieDataModel(final boolean supportHiliting,
            final boolean detailsAvailable) {
        m_supportHiliting = supportHiliting;
        m_detailsAvailable = detailsAvailable;
    }

    /**
     * Creates the default missing section.
     * @param supportHiliting <code>true</code> if hiliting is supported
     * @return the default missing section
     */
    public static PieSectionDataModel createDefaultMissingSection(
            final boolean supportHiliting) {
        return new PieSectionDataModel(
                PieVizModel.MISSING_VAL_SECTION_CAPTION,
                PieVizModel.MISSING_VAL_SECTION_COLOR, supportHiliting);
    }

    /**
     * @param sections the sections to set the color
     */
    public static void setSectionColor(
            final List<PieSectionDataModel> sections) {
        if (sections == null) {
            throw new NullPointerException("sections must not be null");
        }
        int noOfNoneEmptySections = 0;
        for (final PieSectionDataModel section : sections) {
            if (!section.isEmpty()) {
                noOfNoneEmptySections++;
            }
        }
        int idx = 0;
        for (final PieSectionDataModel section : sections) {
            if (!section.isEmpty()) {
                final Color color = GUIUtils.generateDistinctColor(idx++,
                        noOfNoneEmptySections);
                section.setColor(color);
            }
        }
    }

    /**
     * @param sections the sections to sort
     * @param numerical if the pie column is numerical
     * @param ascending <code>true</code> if the section should be ordered
     * in ascending order
     */
    public static void sortSections(final List<PieSectionDataModel> sections,
            final boolean numerical, final boolean ascending) {
        final AggrValModelComparator comparator =
            new AggrValModelComparator(numerical, ascending);
        Collections.sort(sections, comparator);
    }

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * @return <code>true</code> if details are available
     */
    public boolean detailsAvailable() {
        return m_detailsAvailable;
    }

    /**
     * Adds the given row values to the histogram.
     * @param row the data row to add
     * @param rowColor the color of this row
     * @param pieCell the pie value
     * @param aggrCell the optional aggregation value
     * @throws TooManySectionsException if more sections are created than
     * supported
     */
    public abstract void addDataRow(final DataRow row, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell)
    throws TooManySectionsException;
}
