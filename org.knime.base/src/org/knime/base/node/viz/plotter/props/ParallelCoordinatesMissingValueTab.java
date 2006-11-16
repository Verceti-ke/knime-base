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
package org.knime.base.node.viz.plotter.props;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinatesMissingValueTab extends PropertiesTab {

    
    private JRadioButton m_skipRowBtn;
    private JRadioButton m_skipValue;
    private JRadioButton m_showMissingValueBtn;
    
    /**
     * Three radio buttons for several missing value handling methods.
     *
     */
    public ParallelCoordinatesMissingValueTab() {
        m_skipRowBtn = new JRadioButton("Skip rows containing missing values", 
                true);
        m_skipValue = new JRadioButton("Skip only the missing value", false);
        m_showMissingValueBtn = new JRadioButton("Show missing values", false);
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(m_skipRowBtn);
        btnGroup.add(m_skipValue);
        btnGroup.add(m_showMissingValueBtn);
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(m_skipRowBtn);
        this.add(m_skipValue);
        this.add(m_showMissingValueBtn);
    }
    
    /**
     * 
     * @return checkbox for skipping the whole row
     */
    public JRadioButton getSkipRowButton() {
        return m_skipRowBtn;
    }
    
    /**
     * 
     * @return the checkbox for skipping only the missing value
     */
    public JRadioButton getSkipValueButton() {
        return m_skipValue;
    }
    
    /**
     * 
     * @return checkbox for adding a missing value point on coordinate
     */
    public JRadioButton getShowMissingValsButton() {
        return m_showMissingValueBtn;
    }
    
    
    /**
     * 
     * @see org.knime.base.node.viz.plotter.props.PropertiesTab#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return "Missing Values";
    }
    
}
