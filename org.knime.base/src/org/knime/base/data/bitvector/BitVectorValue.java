/* 
 * 
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
 *   07.07.2005 (mb): created
 */
package org.knime.base.data.bitvector;

import java.io.File;
import java.util.BitSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.knime.base.data.bitvector.BitVectorValueStringRenderer.Type;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;


/**
 * Interface of a {@link BitVectorCell}, forces method to return
 * {@link java.util.BitSet}.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface BitVectorValue extends DataValue {
    
    /** Utility factory for bitvector value. */
    public static final UtilityFactory UTILITY = new BitVectorUtilityFactory();
    
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
    
    /** Utility Factory for Bit Vector values. */
    static class BitVectorUtilityFactory extends UtilityFactory {
        
        private static final Icon ICON;

        /** Loads the bitvector icon. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = BitVectorValue.class.getClassLoader();
                String path = BitVectorValue.class.getPackage()
                    .getName().replace('.', File.separatorChar);
                icon = new ImageIcon(loader.getResource(path
                        + File.separatorChar + "bitvectoricon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }
        
        /** {@inheritDoc} */
        @Override
        protected DataValueComparator getComparator() {
            return new DataValueComparator() {
                /** {@inheritDoc} */
                @Override
                protected int compareDataValues(
                        final DataValue v1, final DataValue v2) {
                    BitVectorValue bv1 = (BitVectorValue)v1;
                    BitVectorValue bv2 = (BitVectorValue)v2;
                    return bv1.getBitSet().cardinality() 
                        - bv2.getBitSet().cardinality();
                }
            };
        }
        
        /** {@inheritDoc} */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    new BitVectorValueStringRenderer(Type.BIT),
                    new BitVectorValueStringRenderer(Type.HEX),
                    new BitVectorValuePixelRenderer());
        }
        
        /** {@inheritDoc} */
        @Override
        public Icon getIcon() {
            return ICON;
        }
    }
    
}
