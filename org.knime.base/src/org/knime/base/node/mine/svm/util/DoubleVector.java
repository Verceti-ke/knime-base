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
 */
package org.knime.base.node.mine.svm.util;

import java.util.ArrayList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * This class is used to represent a vector (in the sense of
 * input data sample). A vector contains double values and the class
 * value. 
 * 
 * @author Stefan, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class DoubleVector {
   
    /* keys for memorizing a vector into a ModelContent. */
    private static final String KEY_CATEGORY = "Result";
    private static final String KEY_POINTS = "Points";

    
    // Values
    private double[] m_values;

    // the class value
    private String m_classValue;

    /**
     * Default constructor.
     * 
     * @param values the double values of the vector.
     * @param classvalue the class value.
     */
    public DoubleVector(final ArrayList<Double> values,
            final String classvalue) {
        m_values = new double[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            m_values[i] = values.get(i);
        }
        m_classValue = classvalue;
    }

    /**
     * @return the class value
     */
    public String getClassValue() {
        return m_classValue;
    }

    /**
     * return the i'th value in the vector.
     * 
     * @param i the index of the value to return
     * @return the value at this index
     */
    public double getValue(final int i) {
        return m_values[i];
    }
    
    /**
     * @return the number of values.
     */
    public int getNumberValues() {
        return m_values.length;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < getNumberValues(); ++i) {
            result = result + getValue(i);
            if (i != getNumberValues() - 1) {
                result += ", ";
            } else {
                result += ": ";
            }
        }
        result += getClassValue();
        return result;
    }

  
    /**
     * Save the vector to a ModelContent object.
     * @param predParams where to save the vector
     * @param id used to identify this vector uniquely
     */
    public void saveTo(final ModelContentWO predParams, final String id) {
        predParams.addString(id + KEY_CATEGORY, m_classValue);
        predParams.addDoubleArray(id + KEY_POINTS, m_values);
    }
    
    /**
     * Loads a vector from a predParams object.
     * @param predParams from where to load
     * @param id used to identify this vector uniquely
     * @throws InvalidSettingsException if a key is not found
     */
    public DoubleVector(final ModelContentRO predParams, final String id) 
            throws InvalidSettingsException {
        m_classValue = predParams.getString(id + KEY_CATEGORY);
        m_values = predParams.getDoubleArray(id + KEY_POINTS);
    }
}
