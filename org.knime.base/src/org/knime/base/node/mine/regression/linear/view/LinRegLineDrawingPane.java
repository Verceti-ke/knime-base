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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Apr 6, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.awt.Color;
import java.awt.Graphics;

import org.knime.base.node.viz.scatterplot.ScatterPlotDrawingPane;

/**
 * DrawingPane that also draws the regression line.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLineDrawingPane extends ScatterPlotDrawingPane {
    private int m_x1;

    private int m_y1;

    private int m_x2;

    private int m_y2;

    /**
     * Paints first the regression line, then calls super.
     * 
     * @see ScatterPlotDrawingPane#paintPlotDrawingPane(Graphics)
     */
    @Override
    protected void paintPlotDrawingPane(final Graphics g) {
        g.setColor(Color.BLACK);
        // bug fix#481, minimum thickness 1
        final int hDotSize = Math.max(1, getDotSize() / 3);
        for (int i = -hDotSize; i < hDotSize; i++) {
            g.drawLine(m_x1, m_y1 + i, m_x2, m_y2 + i);
        }
        super.paintPlotDrawingPane(g);
    }

    /**
     * Set first point of regression line.
     * 
     * @param x1 x-coordinate of first point
     * @param y1 y-coordinate of first point
     */
    void setLineFirstPoint(final int x1, final int y1) {
        m_x1 = x1;
        m_y1 = y1;
    }

    /**
     * Set last point of regression line.
     * 
     * @param x2 x-coordinate of last point
     * @param y2 y-coordinate of last point
     */
    void setLineLastPoint(final int x2, final int y2) {
        m_x2 = x2;
        m_y2 = y2;
    }
}
