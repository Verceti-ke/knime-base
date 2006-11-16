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
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.parcoord;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;
import org.knime.exp.node.view.plotter.LabelPaintUtil;
import org.knime.exp.node.view.plotter.basic.BasicDrawingPane;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinateDrawingPane extends BasicDrawingPane {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            ParallelCoordinateDrawingPane.class);
    
    /** Constant for the space at the top before the axes begin. */
    public static final int TOP_SPACE = 30;
    
    /** Constant for the space at the bottom between margin and axes. */
    public static final int BOTTOM_SPACE = 80; 
    
    /** Constant for the dot size. */
    public static final int DOT_SIZE = 4;
    
    private int m_lineSize = 2;
    
    private static final int MAX_TOOLTIP_LENGTH = 10;
    
    private List<ParallelAxis>m_axes;
    
    private List<LineInfo> m_lines;
    
    private boolean m_skewLabels;
    
    private boolean m_showDots = true;
    
    private boolean m_skipValues;
    
    private boolean m_showMissingVals;
    
    private boolean m_fade;
    
    private boolean m_drawCurves;
    
    private static final String MISSING = "missing values";
    
    private static final int DASH = 8;
    

    
    /**
     * 
     *
     */
    public ParallelCoordinateDrawingPane() {
        setToolTipText("");
    }
    
    
    /**
     * Sets the axes with the data column spec, since it contains all necessary 
     * information (like domain, type, etc).
     * @param axes the parallel axes.
     */
    public void setAxes(final List<ParallelAxis> axes) {
        m_axes = axes;
    }
    
    /**
     * 
     * @param lines the lines / rows (mapped).
     */
    public void setLines(final List<LineInfo> lines) {
        m_lines = lines;
    }
    
    /**
     * 
     * @return the lines / rows (mapped).
     */
    public List<LineInfo> getLines() {
        return m_lines;
    }
    
    
    /**
     * 
     * @return the parallel axes
     */
    public List<ParallelAxis> getAxes() {
        return m_axes;
    }
    
    /**
     * 
     * @param showDots true if the dots on the axis should be painted, 
     * false otherwise.
     */
    public void setShowDots(final boolean showDots) {
        m_showDots = showDots;
    }
    
    /**
     * 
     * @param skip true if missing values should be skipped.
     */
    public void setSkipValues(final boolean skip) {
        if (skip) {
            m_showMissingVals = false;
        }
        m_skipValues = skip;
    }
    
    /**
     * 
     * @param showMissingVals true if missing values should have an extra place
     * on the referring axis.
     */
    public void setShowMissingValues(final boolean showMissingVals) {
        if (showMissingVals) {
            m_skipValues = false;
        }
        m_showMissingVals = showMissingVals;
    }
    
    /**
     * 
     * @param fade true if unhilited rows should be faded.
     */
    public void setFadeUnhilited(final boolean fade) {
        m_fade = fade;
    }
    
    /**
     * 
     * @param drawCurves true, if the rows should be displayed as curves.
     */
    public void setDrawCurves(final boolean drawCurves) {
        m_drawCurves = drawCurves;
    }
    
    /**
     * 
     * @param lineThickness the thickness of the lines.
     */
    public void setLineThickness(final int lineThickness) {
        m_lineSize = lineThickness;
    }

    
    /**
     * @see org.knime.exp.node.view.plotter.basic.BasicDrawingPane#
     * paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
        super.paintContent(g);
        Color backupColor = g.getColor();
        Stroke backupStroke = ((Graphics2D)g).getStroke();
        drawAxes(g);
        drawLines(g, m_lines);
        g.setColor(backupColor);
        ((Graphics2D)g).setStroke(backupStroke);
        
    }
    
    /**
     * @param g the graphics object.
     */
    protected void drawAxes(final Graphics g) {
        if (m_axes == null) {
            return;
        }
        // turn text antialias on
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // go through the axes
        for (ParallelAxis axis : m_axes) {
            // if the axis is selected paint it thicker
            if (axis.isSelected()) {
                ((Graphics2D)g).setStroke(new BasicStroke(3));
            } 
            // draw the axis line
            g.drawLine(axis.getXPosition(), TOP_SPACE, axis.getXPosition(), 
                    getHeight() - BOTTOM_SPACE);
            ((Graphics2D)g).setStroke(new BasicStroke(1));
            // and add textual information (min,max or possible values)
                int distance;
                // if it is not the last axis
                if (m_axes.indexOf(axis) < m_axes.size() - 1) {
                    // get distance to next axis
                    ParallelAxis nextAxis = m_axes.get(
                            m_axes.indexOf(axis) + 1);
                    distance = nextAxis.getXPosition() - axis.getXPosition();
                } else {
                    // get distance to border
                    distance = getWidth() - 10 - axis.getXPosition(); 
                }
                if (!axis.isNominal()) {
                    drawNumericAxis(g, (NumericParallelAxis)axis);
                } else {
                    m_skewLabels = !checkLabelSpace(
                            ((NominalParallelAxis)axis).getPossibleValues(),
                            g.getFontMetrics(), distance);
                    // paint possible values
                    drawNominalAxis(g, (NominalParallelAxis)axis);
                }
        }
        if (m_showMissingVals) {
            int y = getHeight() - (2 * g.getFontMetrics().getHeight());
            g.drawLine(10, y, getWidth() - 10, y);
            g.drawString(MISSING, 15, getHeight() 
                    -  g.getFontMetrics().getHeight());
        }
    }
    
    /**
     * Draws a nominal axis with the labels of all possible values.
     * @param g the graphics object
     * @param axis the axis.
     */
    protected void drawNominalAxis(final Graphics g,
            final NominalParallelAxis axis) {
        Graphics2D g2 = (Graphics2D)g;
        double ticks = ((double)((double)axis.getHeight() / (double)axis
                .getPossibleValues().size()));
        if (ticks == 0) {
            ticks = 2 * g.getFontMetrics().getHeight();
        }
        int vSpace = getWidth() / m_axes.size();
        m_skewLabels = LabelPaintUtil.rotateLabels(
                ((NominalCoordinate)axis.getCoordinate())
                .getReducedTickPositions(axis.getHeight()),
                vSpace, g.getFontMetrics());
        int i = 0;
        for (String value : axis.getPossibleValues()) {
                int x = axis.getXPosition() + g.getFontMetrics().getHeight();
                int y = (int)axis.getMappedValue(new StringCell(value))
                        - (g.getFontMetrics().getHeight() / 2);
                y = getHeight() - y - BOTTOM_SPACE - TOP_SPACE;
                Rectangle rect = new Rectangle(x, 
                        y - BOTTOM_SPACE + g.getFontMetrics().getDescent(), 
                        vSpace, 
                        axis.getHeight() / axis.getPossibleValues().size());
                LabelPaintUtil.drawLabel(value, g2, rect, 
                        LabelPaintUtil.Position.RIGHT, m_skewLabels);
//                g.drawString(value, x, y);
            i++;
        }
    }
    
    /**
     * Checks for all labels if the string length is smaller than the distance.
     * @param labels the labels
     * @param metrics the font metrics
     * @param distance the available space
     * @return true if the string length of all labels is smaller than the 
     * distance.
     */
    protected boolean checkLabelSpace(final Set<String> labels, 
            final FontMetrics metrics, final int distance) {
        for (String label : labels) {
            if (metrics.stringWidth(label) >= distance) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Draws a numeric axis with the min value at the bottom and the max value 
     * at the top.
     * @param g the graphics object.
     * @param axis the axis.
     */
    protected void drawNumericAxis(final Graphics g, 
            final NumericParallelAxis axis) {
        double min = axis.getMinValue();
        double max = axis.getMaxValue();
        int x = axis.getXPosition() - Math.max(
                g.getFontMetrics().stringWidth("" + min),
                g.getFontMetrics().stringWidth("" + max)) / 2;
        int minY = (int)(getHeight() - BOTTOM_SPACE 
                + (2 * g.getFontMetrics().getHeight()));
        int maxY = (int)TOP_SPACE - g.getFontMetrics().getHeight();
        g.drawString("" + min , x, minY);
        g.drawString("" + max, x, maxY);
    }
    
    
    /**
     * Draws the lines / rows.
     * @param lines the lines
     * @param g the graphics object.
     */
    protected void drawLines(final Graphics g, final List<LineInfo> lines) {
        if (lines == null || lines.size() == 0) {
            return;
        }
        if (m_axes.size() < 1) {
            return;
        }
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // if only one axis is selected paint only the dots on this axis
        if (m_axes.size() == 1 && m_showDots) {
            for (LineInfo line : lines) {
                if (m_fade && !line.isHilite()) {
                    if (line.isSelected()) {
                        g.setColor(ColorAttr.INACTIVE_SELECTED);
                    } else {
                        g.setColor(ColorAttr.INACTIVE);
                    }
                } else {
                    g.setColor(line.getColor().getColor(line.isSelected(),
                        line.isHilite()));
                }
                int size = (int)(DOT_SIZE + (DOT_SIZE * line.getSize()));
                ((Graphics2D)g).setStroke(new BasicStroke(getStrokeSize(
                        line.getSize())));
                for (Point p1 : line.getPoints()) {
                    paintDot(g, p1, line, size);
                }
            }
        }
        // more than one axis
        List<LineInfo> hilited = new ArrayList<LineInfo>();
        List<LineInfo> selected = new ArrayList<LineInfo>();
        for (LineInfo line : lines) {
            if (line.isHilite()) {
                hilited.add(line);
            } else if (line.isSelected()) {
                selected.add(line);
            } else {
                if (m_drawCurves) {
                    drawCurve((Graphics2D)g, line);
                } else {
                    drawLine(g, line);
                }
            }
        }
        // draw selected lines
        for (LineInfo line : selected) {
            if (m_drawCurves) {
                drawCurve((Graphics2D)g, line);
            } else {
                drawLine(g, line);
            }
        }
        // draw hilited lines a the end (in the foreground)
        for (LineInfo line : hilited) {
            if (m_drawCurves) {
                drawCurve((Graphics2D)g, line);
            } else {
                drawLine(g, line);
            }
        }
    }
    
    private void drawLine(final Graphics g, final LineInfo line) {
        if (m_fade && !line.isHilite()) {
            if (line.isSelected()) {
                g.setColor(ColorAttr.INACTIVE_SELECTED);
            } else {
                g.setColor(ColorAttr.INACTIVE);
            }
        } else {
            g.setColor(line.getColor().getColor(line.isSelected(),
                line.isHilite()));
        }
        int lineSize = getStrokeSize(line.getSize());
        Stroke selectionStroke = new BasicStroke(lineSize, 
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, 
                new float[]{DASH}, 0);
        ((Graphics2D)g).setStroke(new BasicStroke(lineSize));
        List<Point> points = line.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            if ((p1.y == ParallelCoordinatesPlotter.MISSING 
                    || p2.y == ParallelCoordinatesPlotter.MISSING)) {
                if (m_skipValues) {
                    continue;
                }
                if (m_showMissingVals) {
                    int pointPos = getHeight() -  (2 * g.getFontMetrics()
                            .getHeight());
                    if (p1.y == ParallelCoordinatesPlotter.MISSING) {
                        p1.y = pointPos;
                    }
                    if (p2.y == ParallelCoordinatesPlotter.MISSING) {
                        p2.y = pointPos;
                    }
                    
                }
            }
            // backup
            Color backupColor = g.getColor();
            if (line.isHilite()) {
                // draw additional "hilite" line#
                Stroke backupStroke = ((Graphics2D)g).getStroke();
                ((Graphics2D)g).setStroke(new BasicStroke(2 * lineSize));
                g.setColor(ColorAttr.HILITE);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
                g.setColor(backupColor);
                ((Graphics2D)g).setStroke(backupStroke);
            }
//          draw the line
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
            if (line.isSelected()) {
                // draw dotted line
                Stroke backupStroke = ((Graphics2D)g).getStroke();
                ((Graphics2D)g).setStroke(selectionStroke);
                g.setXORMode(Color.white);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
                ((Graphics2D)g).setStroke(backupStroke);
                g.setPaintMode();
            }
            // draw the point
            if (m_showDots) {
                paintDot(g, p1, line, lineSize);
            }
        }
        if (m_showDots) {
            // draw the points of the last axis
            Point p = line.getPoints().get(line.getPoints().size() - 1);
            paintDot(g, p, line, lineSize);
        }
        ((Graphics2D)g).setStroke(new BasicStroke(1));
    }
    
    private void drawCurve(final Graphics2D g, final LineInfo line) {
        // for each line
            GeneralPath path = null;
            Point ctrl = null;
//            Point newCtrl = null;
            g.setColor(line.getColor().getColor(line.isSelected(), 
                    line.isHilite()));
            int lineSize = getStrokeSize(line.getSize());
            Stroke selectionStroke = new BasicStroke(lineSize, 
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, 
                    new float[]{DASH}, 0);
            ((Graphics2D)g).setStroke(new BasicStroke(lineSize));
            if (m_fade && !line.isHilite()) {
                if (line.isSelected()) {
                    g.setColor(ColorAttr.INACTIVE_SELECTED);
                } else {
                    g.setColor(ColorAttr.INACTIVE);
                }
            }
            // for all points
            for (int i = 0; i < line.getPoints().size() - 1; i++) {
                Point p1 = line.getPoints().get(i);
                Point p2 = line.getPoints().get(i + 1);
                if (path == null) {
                    path = new GeneralPath();
                    path.moveTo(p1.x, p1.y);
                    // first control Point with offset
                    int x = p1.x + ((p2.x - p1.x) / 2);
                    int y = p1.y + ((p2.y - p1.y) / 2) + 10;
                    ctrl = new Point(x, y);
                } 
                // create cubic curve
                int firstX = p1.x + ((p2.x - p1.x) / 3);
                int secondX = p1.x + ((p2.x - p1.x) / 2);
                Point firstCtrl = getNextControlPoint(ctrl, p1, firstX);
                Point secondCtrl = createAdditionalCtrlPoint(secondX, p2, 
                        firstCtrl);
                path.curveTo(firstCtrl.x, firstCtrl.y, 
                        secondCtrl.x, secondCtrl.y,
                        p2.x, p2.y);
                ctrl = secondCtrl;
            }
            Color backupColor = g.getColor();
            if (line.isHilite()) {
                // draw additional "hilite" line
                g.setColor(ColorAttr.HILITE);
                Stroke backupStroke = g.getStroke();
                g.setStroke(new BasicStroke(2 * lineSize));
                g.draw(path);
                g.setColor(backupColor);
                g.setStroke(backupStroke);
            }
            g.draw(path);
            if (line.isSelected()) {
                // draw dotted line
                Stroke backupStroke = ((Graphics2D)g).getStroke();
                ((Graphics2D)g).setStroke(selectionStroke);
                g.setXORMode(Color.white);
                g.draw(path);
                ((Graphics2D)g).setStroke(backupStroke);
                g.setPaintMode();
            }
            if (m_showDots) {
                for (Point p : line.getPoints()) {
                    int size = getStrokeSize(line.getSize());
                    paintDot(g, p, line, size);
                }
            }
    }
    
    /**
     * Paints one dot.
     * @param g the graphics object
     * @param p the point
     * @param line the line
     * @param size the size
     */
    protected void paintDot(final Graphics g, final Point p, 
            final LineInfo line, final int size) {
        if (p.y == ParallelCoordinatesPlotter.MISSING 
                && m_skipValues) {
            return;
        }
        ShapeFactory.Shape shape = line.getShape();
        shape.paint(g, p.x, p.y, 2 * size, line.getColor().getColor(),
                line.isHilite(), line.isSelected(), 
                m_fade && !line.isHilite());
    }

    
    /*
     * chooses the next control point such that it lies with the old 
     * control point and p2 on one straight line.
     */
    private Point getNextControlPoint(final Point oldCtrl, 
            final Point p2, final int newX) {
        double m = ((p2.getY() - oldCtrl.getY()) 
                / (p2.getX() - oldCtrl.getX()));
        double newY = (m * (double)newX) - (m * oldCtrl.getX()) 
            + oldCtrl.getY();
//        double diff = (newY - p2.getY());
        Point p = new Point(newX, (int)newY);
        return p;
    }
    
    /*
     * creates a second control point for cubic curves.
     * depend on the first control point. if the first control point lies below 
     * p2 then a point above p2 is chosen, otherwise a point above p2.
     */
    private Point createAdditionalCtrlPoint(final int x, final Point p2, 
            final Point firstCtrlPoint) {
        int y;
        // first ctrl point below p2
        if (p2.y - firstCtrlPoint.y < 0) {
            // new y above p2
            y = p2.y - 30;
        } else {
            // new y below p2
            y = p2.y + 30;
        }
        return new Point(x, y);
    }
    
    
    private int getStrokeSize(final double lineSize) {
        return m_lineSize + (int)(3 * lineSize);
    }
    
    /**
     * 
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        Rectangle rect = new Rectangle(e.getX() 
                - ParallelCoordinatesPlotter.SENSITIVITY / 2,
                TOP_SPACE, ParallelCoordinatesPlotter.SENSITIVITY, 
                getHeight() - BOTTOM_SPACE - TOP_SPACE);
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        int i = 0;
        int containedPoints = 0;
        // first determine the selected axis
        for (ParallelAxis axis : m_axes) {
            if (axis.isContainedIn(rect)) {
                for (LineInfo line : m_lines) {
                    // check only the points on selected axis
                    int size = (int)(DOT_SIZE + (DOT_SIZE * line.getSize()));
                    Point p = line.getPoints().get(i);
                    Rectangle dot = new Rectangle(p.x - size / 2,
                            p.y - size / 2, size, size);
                    
                    if (dot.contains(e.getPoint())) {
                        if (buffer.length() == 0) {
                            buffer.append(line.getDomainValues().get(i) + ": ");
                        }
                        // if first rowk key without ","
                        if (first) {
                            buffer.append(line.getRowKey().getId());
                            first = false;
                        } else if (containedPoints > MAX_TOOLTIP_LENGTH) {
                            buffer.append(", ...");
                            break;
                        } else {
                            // more than one: add a ","
                            buffer.append(", " + line.getRowKey().getId());
                        }
                        containedPoints++;
                    }
                }
            }
            i++;
        }
        return buffer.toString();
    }

}
