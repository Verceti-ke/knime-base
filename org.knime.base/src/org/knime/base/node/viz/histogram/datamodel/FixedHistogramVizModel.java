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
 *    12.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;


/**
 * This class holds all visualization data of a histogram. 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramVizModel extends AbstractHistogramVizModel {

    private final Collection<ColorColumn> m_aggrColumns;
    
    private final DataColumnSpec m_xColSpec;
    
    /**
     * Constructor for class HistogramVizModel.
     * @param rowColors the different row colors
     * @param bins the bins
     * @param missingValueBin the bin with the rows with missing x values
     * @param xColSpec the column specification of the selected x column
     * @param aggrColumns the selected aggregation columns. Could be 
     * <code>null</code>
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     */
    public FixedHistogramVizModel(final SortedSet<Color> rowColors, 
            final List<BinDataModel> bins, final BinDataModel missingValueBin,
            final DataColumnSpec xColSpec, 
            final Collection<ColorColumn> aggrColumns, 
            final AggregationMethod aggrMethod, 
            final HistogramLayout layout) {
        super(rowColors, aggrMethod, layout, bins.size());
        if (aggrMethod == null) {
            throw new NullPointerException(
                    "Aggregation method must not be null");
        }
        if (layout == null) {
            throw new NullPointerException("Layout must not be null");
        }
        m_aggrColumns = aggrColumns;
        if (aggrColumns != null && aggrColumns.size() > 1) {
            setShowBarOutline(true);
        } else {
            setShowBarOutline(false);
        }
        m_xColSpec = xColSpec;
        setBins(bins, missingValueBin);
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getXColumnName()
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getXColumnSpec()
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getAggrColumns()
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#isFixed()
     */
    @Override
    public boolean isFixed() {
        return true;
    }
    
    // hiliting stuff

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getHilitedKeys()
     */
    @Override
    public Set<DataCell> getHilitedKeys() {
        return new HashSet<DataCell>(1);
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getSelectedKeys()
     */
    @Override
    public Set<DataCell> getSelectedKeys() {
        return new HashSet<DataCell>(1);
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#unHiliteAll()
     */
    @Override
    public void unHiliteAll() {
        //not supported in this implementation
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#updateHiliteInfo(java.util.Set, boolean)
     */
    @Override
    public void updateHiliteInfo(final Set<DataCell> hilited, 
            final boolean hilite) {
        //not supported in this implementation
    }
}
