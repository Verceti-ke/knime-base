/*
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.parallel;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class ReplaceColumn extends ColumnDestination {
    private final int m_index;
    
    /**
     * Creates a column destination that replaces a column.
     * 
     * @param index the index of the column to replace
     */
    ReplaceColumn(final int index) {
        m_index = index; 
    }
    
    /**
     * Returns the index of the column in the original table that should be
     * replaced.
     *  
     * @return the column's index
     */
    int getIndex() {
        return m_index;
    }
}
