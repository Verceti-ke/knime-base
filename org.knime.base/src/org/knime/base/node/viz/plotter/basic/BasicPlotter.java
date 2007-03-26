/*
 * ------------------------------------------------------------------
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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.NodeLogger;

/**
 * Tha BasicPlotter provides means to add some simple, basic graphical forms to
 * the drawing pane, which are automatically mapped to the drawing pane's 
 * dimension. It may be used in the cases, where only the domain values for 
 * these forms are known. There are some possibilities to add these basic forms
 * directly:
 * {@link #addEllipse(double, double, double, double, Color, Stroke, boolean)},
 * {@link #addLine(double[], Color, Stroke)},
 * {@link #addRectangle(double, double, double, double,Color, Stroke, boolean)},
 * {@link #addText(String, DataCell, DataCell, Color)}. If some other forms are
 * needed, the 
 * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement} may be 
 * extended and added with the 
 * {@link #addBasicDrawingElement(BasicDrawingElement)}.
 * 
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BasicPlotter extends AbstractPlotter {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            BasicPlotter.class);
    
    
    /**
     * Always provide a possibility to construct a customized plotter.
     * 
     * @param panel the drawing pane
     * @param properties the properties
     */
    public BasicPlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        super(panel, properties);
    }
    
    
    /**
     * Constructs a plotter with a 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingPane} and the
     * {@link org.knime.base.node.viz.plotter.AbstractPlotterProperties}.
     *
     */
    public BasicPlotter() {
        super(new BasicDrawingPane(), new AbstractPlotterProperties());
    }
    
    
    

    
    /**
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        ((BasicDrawingPane)getDrawingPane()).clearPlot();
    }


    /**
     * Plots the column in the table specified by the column index as a 
     * line plot.
     * 
     * @param table the table containing the data to be plotted.
     * @param colIdx - the column index specifying the data to be plotted.
     * @param color the color of the line (may be null)
     * @param stroke the stroke of the line (may be null)
     */
    public void addLine(final DataArray table, final int colIdx,
            final Color color, final Stroke stroke) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }
        if (!table.getDataTableSpec().getColumnSpec(colIdx).getType()
                .isCompatible(DoubleValue.class)) {
            return;
        }
//        if (!checkCompatibleAxis()) {
//            return;
//        }
        // x axis
        Set<DataCell> rowKeys = new LinkedHashSet<DataCell>();
        for (int i = 0; i < table.size(); i++) {
            rowKeys.add(table.getRow(i).getKey().getId());
        }
        createNominalXCoordinate(rowKeys);

        double newYMin = ((DoubleValue)table.getDataTableSpec().getColumnSpec(
                colIdx).getDomain().getLowerBound()).getDoubleValue();
        double newYMax = ((DoubleValue)table.getDataTableSpec().getColumnSpec(
                colIdx).getDomain().getUpperBound()).getDoubleValue(); 
        createYCoordinate(newYMin, newYMax);

        BasicLine line = new BasicLine();
        if (color != null) {
            line.setColor(color);
        }
        if (stroke != null) {
            line.setStroke(stroke);
        }
        int x = 0;
        for (DataRow row : table) {
            DataCell value = row.getCell(colIdx);
            if (!value.isMissing()) {
                int mappedX = getMappedXValue(new IntCell(x));
                int mappedY = getMappedYValue(value);
                DataCellPoint domainPoint = new DataCellPoint(
                        row.getKey().getId(), value);
                line.addDomainValue(domainPoint);
                Point p = new Point(mappedX, mappedY);
                line.addPoint(p);
            } else {
//              if value.isMissing() -> create newLine
                ((BasicDrawingPane)getDrawingPane()).addDrawingElement(line);
                line = new BasicLine();
                line.setColor(color);
                line.setStroke(stroke);
            } 
            x++;
        }
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(line);
        fitToScreen();
    }
    
    /**
     * Paints the values in the double array as y-values and the x-values are 
     * simply the position in the array, which implies, that the y-values are 
     * equidistant. Color and Stroke might be <code>null</code>, then the 
     * default values for them are used (the currently set color and stroke in
     * the graphics object).
     * 
     * @param y the equidistant y values
     * @param color color of the line, might be <code>null</code>, then the 
     * default color is used
     * @param stroke stroke of the line, might be <code>null</code>, then the 
     * default  stroke is used
     */
    public void addLine(final double[] y, final Color color, 
            final Stroke stroke) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }        
        if (y.length == 0) {
            return;
        }
        BasicLine line = new BasicLine();
        if (color != null) {
            line.setColor(color);
        }
        if (stroke != null) {
            line.setStroke(stroke);
        }
        int[] xCoords = new int[y.length];
        for (int i = 0; i < y.length; i++) {
            xCoords[i] = i;
        }
        createXCoordinate(0, y.length);
        double[] yCopy = y.clone();
        Arrays.sort(yCopy);
        double yMin = yCopy[0];
        double yMax = yCopy[yCopy.length - 1];
        createYCoordinate(yMin, yMax);
        for (int i = 0; i < y.length; i++) {
            double value = y[i];
            double mappedValue = getYAxis().getCoordinate()
                .calculateMappedValue(new DoubleCell(value), 
                        getDrawingPaneDimension().height, true);
            int mappedX = (int)getXAxis().getCoordinate()
                .calculateMappedValue(new IntCell(xCoords[i]), 
                        getDrawingPaneDimension().width, true);
            Point p = new Point(mappedX, (int)getScreenYCoordinate(
                    mappedValue));
            line.addPoint(p);
            line.addDomainValue(new DataCellPoint(new DoubleCell(xCoords[i]), 
                    new DoubleCell(value)));
            }
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(line);    
        fitToScreen();
    }
   
    
    
    /**
     * Adds a line based on the points defined by the x and y values in the 
     * referring double arrays. Mind, that the values are assumed to be domain 
     * values.
     * 
     * @param x the x coordinates (not mapped)
     * @param y the y coordinates (not mapped)
     * @param color the color
     * @param stroke the stroke
     */
    public void addLine(final double[] x, final double[] y, final Color color, 
            final Stroke stroke) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }        
        if (x.length == 0 || y.length == 0) {
            return;
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y coordinates must " 
                    + "be of same length!");
        }
        BasicLine line = new BasicLine();
        if (color != null) {
            line.setColor(color);
        }
        if (stroke != null) {
            line.setStroke(stroke);
        }
        // create coordinates -> determine min and max
        double[] xCopy = x.clone();
        Arrays.sort(xCopy);
        double xMin = xCopy[0];
        double xMax = xCopy[xCopy.length - 1];
//        LOGGER.debug("proposed min and max: " + xMin + " " + xMax);
        createXCoordinate(xMin, xMax);
        double[] yCopy = y.clone();
        Arrays.sort(yCopy);
        double yMin = yCopy[0];
        double yMax = yCopy[yCopy.length - 1];
        createYCoordinate(yMin, yMax);
        for (int i = 0; i < y.length; i++) {
        double value = y[i];
        double mappedValue = getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(value), getDrawingPaneDimension().height,
                true);
        int mappedX = (int)getXAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(x[i]), getDrawingPaneDimension().width, true);
        Point p = new Point(mappedX, (int)getScreenYCoordinate(mappedValue));
        line.addPoint(p);
        line.addDomainValue(new DataCellPoint(new DoubleCell(x[i]), 
                new DoubleCell(value)));
        }
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(line);
    }
    
    
    /**
     * Adds an ellipse with the center point (!) and the height width.
     * 
     * @param xCenter x
     * @param yCenter y
     * @param width width
     * @param height height
     * @param color color
     * @param stroke stroke
     * @param filled true if the ellipse should be filled.
     */
    public void addEllipse(final double xCenter, final double yCenter, 
            final double width, final double height, final Color color, 
            final Stroke stroke, final boolean filled) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }        
        double x = xCenter - (width / 2.0);
        double y = yCenter - (height / 2.0);
//        LOGGER.debug("x: " + x + " y: "  + y + "w: " + width + " h: " 
//        + height);
        createXCoordinate(x, x + width);
        createYCoordinate(y, y + height);
        int mappedX = (int)getXAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(x), getDrawingPaneDimension().width, true);
        int mappedX2 = (int)getXAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(x + width), getDrawingPaneDimension().width, 
                true);
        
        int mappedY = (int) getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(y), getDrawingPaneDimension().height, true);
        int mappedY2 = (int) getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(y + height), getDrawingPaneDimension().height, 
                true);
        BasicEllipse ellipse = new BasicEllipse(filled);
        ellipse.setPoints(
                new Point(mappedX, mappedY), 
                new Point(mappedX2, mappedY2));
        ellipse.setDomainValues(new DataCellPoint(
                new DoubleCell(x), new DoubleCell(y)), 
                new DataCellPoint(new DoubleCell(x + width), 
                        new DoubleCell(y + height)));
        if (color != null) {
            ellipse.setColor(color);
        }
        if (stroke != null) {
            ellipse.setStroke(stroke);
        }
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(ellipse);
        fitToScreen();
    }
    
    /**
     * Adds a rectangle defined by the upper-left corner and the width and 
     * height.
     *  
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     * @param color color
     * @param stroke stroke
     * @param filled true if the rectangle should be filled.
     */
    public void addRectangle(final double x, final double y, final double width,
            final double height, final Color color, final Stroke stroke, 
            final boolean filled) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }
        createXCoordinate(x, x + width);
        createYCoordinate(y, y + height);
        int mappedX1 = (int) getXAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(x), getDrawingPaneDimension().width, true);
        int mappedX2 = (int)getXAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(x + width), getDrawingPaneDimension().width, 
                true);
        
        int mappedY1 = (int) getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(y), getDrawingPaneDimension().height, true);
        int mappedY2 = (int) getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(y + height), getDrawingPaneDimension().height, 
                true);
        BasicRectangle rectangle = new BasicRectangle(filled);
        rectangle.setPoints(
                new Point(mappedX1, (int)getScreenYCoordinate(mappedY1)), 
                new Point(mappedX2, (int)getScreenYCoordinate(mappedY2)));
        rectangle.setDomainValues(new DataCellPoint(new DoubleCell(x), 
                new DoubleCell(y)), 
                new DataCellPoint(new DoubleCell(x + width), 
                        new DoubleCell(y + height)));
        if (color != null) {
            rectangle.setColor(color);
        }
        if (stroke != null) {
            rectangle.setStroke(stroke);
        }
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(rectangle);
        fitToScreen();
    }
    
    /**
     * Adds a text at the position defined by the values of the both
     * {@link org.knime.core.data.DataCell}s (lower-left corner).
     * 
     * @param text the text to be displayed
     * @param x left position of the text
     * @param y the bottom position of the text
     * @param color the color of the text (might be null: then the default color
     * is used)
     */
    public void addText(final String text, final DataCell x, final DataCell y,
            final Color color) {
        if (getXAxis() == null || getXAxis().getCoordinate() == null 
                || getYAxis() == null || getYAxis().getCoordinate() == null) {
            return;
        }
        BasicText textElement = new BasicText(text);
        if (color != null) {
            textElement.setColor(color);
        }
        textElement.setDomainValues(new DataCellPoint(x, y));
        int mappedX = getMappedXValue(x);
        int mappedY = getMappedYValue(y);
        textElement.addPoint(new Point(mappedX, mappedY));
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(textElement);
    }
    
    /**
     * Adds a drawing element to the drawing pane. The mapping of the domain 
     * values to the screen coordinates is done here 
     * Only numeric values are supported. All the rest like color 
     * and stroke setting has to be done outside.
     * @param element a drawing element.
     */
    public void addBasicDrawingElement(final BasicDrawingElement element) {
        if (!(getDrawingPane() instanceof BasicDrawingPane)) {
            return;
        }
        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxY = Integer.MIN_VALUE;

        for (DataCellPoint p : element.getDomainValues()) {
            // determine the min and max values for the coordinates
            if (!p.getX().getType().isCompatible(DoubleValue.class)
                     || !p.getY().getType().isCompatible(DoubleValue.class)) {
                LOGGER.warn("a basic drawing element can onyl be defined for " 
                        + "numeric values!");
                return;
            }
            double x = ((DoubleValue)p.getX()).getDoubleValue();
            double y  = ((DoubleValue)p.getY()).getDoubleValue();
            minX = Math.min(x, minX);
            maxX = Math.max(x, maxX);
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);            
        }
        createXCoordinate(minX, maxX);
        createYCoordinate(minY, maxY);
        // calculate the mapped values
        List<Point> newPoints = new LinkedList<Point>();
        for (DataCellPoint p : element.getDomainValues()) {
            double mappedX = getXAxis().getCoordinate().calculateMappedValue(
                    p.getX(), getDrawingPaneDimension().width, true);
            double mappedY = getYAxis().getCoordinate().calculateMappedValue(
                   p.getY(), getDrawingPaneDimension().height, true);
            newPoints.add(new Point((int)mappedX, 
                    (int)getScreenYCoordinate(mappedY)));
        }
        element.setPoints(newPoints);
        ((BasicDrawingPane)getDrawingPane()).addDrawingElement(element);
    }
    
    /**
     * Updates the size by mapping the domain values of the 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingElement}s
     *  to the drawing pane's dimension.
     *  
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#updateSize()
     */
    @Override
    public void updateSize() {
//        if (getXAxis() == null || getXAxis().getCoordinate() == null 
//                || getYAxis() == null || getYAxis().getCoordinate() == null) {
//            return;
//        }
        if (getDrawingPane() instanceof BasicDrawingPane) {
            List<BasicDrawingElement> elements 
                = ((BasicDrawingPane)getDrawingPane()).getDrawingElements();
            if (elements != null) {
                // create a copy to avoid concurrent modification exception
                List<BasicDrawingElement> copy 
                    = new ArrayList<BasicDrawingElement>(elements);
                for (BasicDrawingElement element : copy) {
                    List<Point> newPoints = new LinkedList<Point>();
                    for (DataCellPoint p : element.getDomainValues()) {
                        Point newPoint;
                        // check if missing value
                        if (p.getX().isMissing()
                                || p.getY().isMissing()) {
                            // new point = -1, -1
                            newPoint = new Point(-1, -1);
                        } else {
                        // update position
                        int x = (int)getXAxis().getCoordinate()
                                .calculateMappedValue(p.getX(),
                                        getDrawingPaneDimension().width
                                        - (2 * getXAxis().getTickOffset()), 
                                        true);
                        x +=  getXAxis().getTickOffset();
                        int y = (int)getYAxis().getCoordinate()
                                .calculateMappedValue(p.getY(),
                                        getDrawingPaneDimension().height
                                        - (2 * getYAxis().getTickOffset()), 
                                        true);
                        y += getYAxis().getTickOffset();
                        newPoint = new Point(x,
                                (int)getScreenYCoordinate(y));
                        }
                        // store new point
                        newPoints.add(newPoint);
                    }
                    // set new points
                    element.setPoints(newPoints);
                }
            }
        }
    }    
    
}
