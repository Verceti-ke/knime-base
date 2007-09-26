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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;

/**
 * This abstract class holds the data of a particular aggregation value and its
 * {@link AggregationValSubModel}s.
 * @author Tobias Koetter, University of Konstanz
 * @param <T> the type of the concrete sub model implementation
 * @param <S> the basic shape
 * @param <H> the optional hilite shape
 */
public abstract class AggregationValModel
<T extends AggregationValSubModel<S, H>, S extends Shape, H extends Shape>
implements Serializable, AggregationModel<S, H> {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AggregationValModel.class);
    private final String m_name;

    private final Color m_color;

    private final boolean m_supportHiliting;

    private final Map<Color, T> m_elements =
            new HashMap<Color, T>();

    /**The number of rows including empty value rows.*/
    private int m_rowCounter = 0;

    /**The number of values without missing values!*/
    private int m_valueCount = 0;

    private double m_aggrSum = 0;

    private boolean m_presentable = false;

    private boolean m_isSelected = false;

    private S m_shape;

    /**If the different elements of this bar can't be draw because the bar
     * is to small this rectangle is calculated to reflect the proportion
     * of hilited rows in this bar. */
    private H m_hiliteShape;

    /**Constructor for class AttributeValModel.
     * @param name the name of this element
     * @param color the color to use for this element
     * @param supportHiliting if hiliting should be supported
     */
    protected AggregationValModel(final String name, final Color color,
            final boolean supportHiliting) {
        m_name = name;
        m_color = color;
        m_supportHiliting = supportHiliting;
    }

    /**Constructor for class AttributeValModel.
     * @param name the name of this element
     * @param color the color of this element
     * @param elements the sub elements
     * @param rowCounter the number of rows including missing values
     * @param valueCounter the number of values exl. missing values
     * @param aggrSum the aggregation sum
     * @param supportHiliting if hiliting should be supported
     */
    protected AggregationValModel(final String name, final Color color,
            final Map<Color, T> elements,
            final int rowCounter, final int valueCounter,
            final double aggrSum, final boolean supportHiliting) {
        m_name = name;
        m_color = color;
        m_elements.putAll(elements);
        m_rowCounter = rowCounter;
        m_valueCount = valueCounter;
        m_aggrSum = aggrSum;
        m_supportHiliting = supportHiliting;
    }

    /**
     * Adds a new row to this element.
     * @param color the color of the data row
     * @param rowKey the row key
     * @param cell the optional aggregation value cell
     */
    public void addDataRow(final Color color, final DataCell rowKey,
            final DataCell cell) {
        if (color == null) {
            throw new NullPointerException("color must not be null");
        }
        if (rowKey == null) {
            throw new NullPointerException("rowKey must not be null");
        }
        T element = getElement(color);
        if (element == null) {
            element = createElement(color);
            m_elements.put(color, element);
        }
        if (cell != null && !cell.isMissing()) {
            if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "DataCell should be numeric");
            }
            m_aggrSum += ((DoubleValue)cell).getDoubleValue();
            m_valueCount++;
        }
        element.addDataRow(rowKey, cell);
        m_rowCounter++;
    }

    /**
     * @param color the color of the new sub element
     * @return the new sub element with the given color
     */
    protected abstract T createElement(final Color color);

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @param color the color of the sub element
     * @return the sub element with the given color or <code>null</code> if none
     * sub element with the given color exists
     */
    public T getElement(final Color color) {
        return m_elements.get(color);
    }

    /**
     * @return all sub elements of this element
     */
    public Collection<T> getElements() {
        return m_elements.values();
    }

    /**
     * @return all selected sub elements of this element
     */
    public List<T> getSelectedElements() {
        final List<T> result = new ArrayList<T>(m_elements.size());
        for (final T element : m_elements.values()) {
            if (element.isSelected()) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * @return the number of sub elements
     */
    public int getNoOfElements() {
        return m_elements.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_rowCounter;
    }

    /**
     * {@inheritDoc}
     */
    public double getAggregationSum() {
        return m_aggrSum;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_valueCount;
    }

    /**
     * {@inheritDoc}
     */
    public double getAggregationValue(final AggregationMethod method) {
        if (AggregationMethod.COUNT.equals(method)) {
            return m_rowCounter;
        } else if (AggregationMethod.SUM.equals(method)) {
            return m_aggrSum;
        } else if (AggregationMethod.AVERAGE.equals(method)) {
            if (m_valueCount == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_valueCount;
        }
        throw new IllegalArgumentException("Aggregation method " + method
                + " not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public S getShape() {
        return m_shape;
    }

    /**
     * @param shape the shape check for selection and drawing
     * @param calculator the hilite shape calculator
     */
    public void setShape(final S shape,
            final HiliteShapeCalculator<S, H> calculator) {
        if (shape == null) {
            m_presentable = false;
        } else {
            m_presentable = true;
        }
        m_shape = shape;
        calculateHiliteShape(calculator);
    }

    /**
     * {@inheritDoc}
     */
    public H getHiliteShape() {
        return m_hiliteShape;
    }

    /**
     * @param shape the hilite shape to draw
     */
    protected void setHiliteShape(final H shape) {
        m_hiliteShape = shape;
    }

    /**
     * @param presentable <code>true</code> if this element is presentable
     */
    protected void setPresentable(final boolean presentable) {
        m_presentable = presentable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPresentable() {
        return m_presentable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * @param selected <code>true</code> if this element is selected
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setSelected(final boolean selected) {
        if (m_isSelected == selected) {
            return false;
        }
        m_isSelected = selected;
        for (final T element : getElements()) {
            element.setSelected(selected);
        }
        return true;
    }

    /**
     * @param point the {@link Point} to check
     * @param detailed if also the sub sections should be checked
     * @return <code>true</code> if at least one sub element of this element
     * contains the point
     */
    public boolean selectElement(final Point point, final boolean detailed) {
        if (m_shape != null && m_shape.contains(point)) {
            //if the element is to small to draw the different
            //elements we have to select all elements
            //of this element
            if (!detailed || !isPresentable()) {
                //select all sub element if the detail mode is of or
                //the element wasn't selected yet
                for (final T element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final T element : getElements()) {
                    m_isSelected = element.selectElement(point) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * Selects all sub element of this element which intersect the given
     * rectangle.
     * @param rect the {@link Rectangle2D} to check
     * @param detailed if also the sub sections should be checked
     * @return <code>true</code> if at least one sub element of this element
     * intersects the rectangle
     */
    public boolean selectElement(final Rectangle2D rect,
            final boolean detailed) {
        if (m_shape != null && m_shape.intersects(rect)) {
            //if the element is to small to draw the different
            //elements we have to select all elements
            //of this element
            if (!detailed || !isPresentable()) {
                //select all sub element if the detail mode is of or
                //the element wasn't selected yet
                for (final T element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final T element : getElements()) {
                    m_isSelected = element.selectElement(rect) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return m_rowCounter < 1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHilited() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        for (final T element : getElements()) {
            if ((element).isHilited()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getHiliteRowCount() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        int noOfHilitedKeys = 0;
        for (final T element : getElements()) {
            noOfHilitedKeys +=
                (element).getHiliteRowCount();
        }
        return noOfHilitedKeys;
    }

    /**
     * @param hilited the row keys to unhilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    public boolean removeHilitedKeys(final Collection<DataCell> hilited,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        boolean changed = false;
        for (final T element : getElements()) {
            changed = element.removeHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteShape(calculator);
        }
        return changed;
    }

    /**
     * @param hilited the row keys to hilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    public boolean setHilitedKeys(final Collection<DataCell> hilited,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        boolean changed = false;
        for (final T element : getElements()) {
            changed = element.setHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteShape(calculator);
        }
        return changed;
    }

    /**
     * Clears all hilite information.
     */
    public void clearHilite() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        for (final T element : getElements()) {
            element.clearHilite();
        }
        setHiliteShape(null);
    }

    /**
     * Overwrite this method to support hiliting.
     * @param calculator the optional hilite calculator
     */
    @SuppressWarnings("unchecked")
    protected void calculateHiliteShape(
            final HiliteShapeCalculator<S, H> calculator) {
        if (calculator == null) {
            return;
        }
        if (supportsHiliting()) {
            setHiliteShape(calculator.calculateHiliteShape(
                (AggregationValModel<AggregationValSubModel<S, H>, S, H>)this));
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected AggregationValModel <T, S , H> clone()
        throws CloneNotSupportedException {
        LOGGER.debug("Entering clone() of class AggregationValModel.");
        final long startTime = System.currentTimeMillis();
        AggregationValModel <T, S , H> clone =
            null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(this);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            clone = (AggregationValModel <T, S , H>)
                new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg =
                "Exception while cloning aggregation value model: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new CloneNotSupportedException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting clone() of class AggregationValModel.");
        return clone;
    }
}
