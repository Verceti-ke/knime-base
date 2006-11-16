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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scattermatrix;

import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.props.ScatterPlotterAppearanceTab;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterMatrixProperties extends MultiColumnPlotterProperties {
    
    private final ScatterPlotterAppearanceTab m_appearance;
    
    /**
     * Default tab, column filter and scatter plotter appearance.
     *
     */
    public ScatterMatrixProperties() {
        super();
        m_appearance = new ScatterPlotterAppearanceTab();
        m_appearance.setDotSize(ScatterMatrixPlotter.DOT_SIZE);
        addTab(m_appearance.getDefaultName(), m_appearance);
    }
    
    
    /**
     * 
     * @return the currently set dot size.
     */
    public int getDotSize() {
        return m_appearance.getDotSize();
    }
    
    /**
     * 
     * @param listener change listener for the dot size.
     */
    public void addDotSizeChangeListener(final ChangeListener listener) {
        m_appearance.addDotSizeChangeListener(listener);
    }
    
    /**
     * 
     * @return the slider for the jitter rate.
     */
    public JSlider getJitterSlider() {
        return m_appearance.getJitterSlider();
    }
    
    

}
