/* 
 * 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package org.knime.base.data.bitvector;

import java.util.BitSet;
import java.util.List;

import org.knime.core.data.DataValue;


/**
 * Interface of a {@link BitVectorCell}, forces method to return
 * {@link java.util.BitSet}.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface BitVectorValue extends DataValue {
    /**
     * @return number of bits actually used
     */
    public int getNumBits();

    /**
     * @return a bit set
     */
    BitSet getBitSet();

    /**
     * @return hex string of this bitvector
     */
    String toHexString();

    /**
     * A mapping from bit position to earlier column name.
     * 
     * @return a mapping from bit position to name or null if none exist
     */
    List<String> getNaming();
}
