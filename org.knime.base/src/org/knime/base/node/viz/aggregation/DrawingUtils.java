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
 *    13.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class DrawingUtils {

    private DrawingUtils() {
        //avoid object creation
    }

    /**
     * Draws the given message in the center of the given rectangle.
     * @param g2 the graphic object to use
     * @param font the font
     * @param msg the message
     * @param bounds the boundary to write in
     */
    public static void drawMessage(final Graphics2D g2, final Font font,
            final String msg, final Rectangle2D bounds) {
        //save the original settings
        final Font origFont = g2.getFont();
        g2.setFont(font);
        final FontMetrics metrics = g2.getFontMetrics();
        final int textWidth = metrics.stringWidth(msg);
        final int textHeight = metrics.getHeight();
        //get the basic rectangle we have to draw in
        int textX = (int)bounds.getCenterX() - (textWidth / 2);
        int textY = (int)bounds.getCenterY() - (textHeight / 2);
        if (textX < 0) {
            textX = 0;
        }
        if (textY < 0) {
            textY = 0;
        }
        g2.drawString(msg, textX, textY);
        //set the original settings
        g2.setFont(origFont);
    }

    /**
     * Draws a horizontal line starting at the given x/y offset with
     * the given length.
     * @param g2 the graphics object to use
     * @param xOffset the x offset of the line
     * @param yOffset the y offset of the line
     * @param lineWidth the width of the line
     * @param color the drawing color
     * @param stroke the stroke to use
     */
    public static void paintHorizontalLine(final Graphics2D g2,
            final int xOffset, final int yOffset, final int lineWidth,
            final Color color, final BasicStroke stroke) {
        // save the original settings
        final Stroke origStroke = g2.getStroke();
        final Color origColor = g2.getColor();
        g2.setColor(color);
        g2.setStroke(stroke);
        g2.drawLine(xOffset, yOffset, lineWidth, yOffset);
        //set the original settings
        g2.setStroke(origStroke);
        g2.setColor(origColor);
    }

    /**
     * Draws a filled rectangle without a border and default transparency.
     * @param g2 the graphic object
     * @param shape the shape to fill
     * @param paint the filling color or TexturePaint
     */
    public static void drawBlock(final Graphics2D g2, final Shape shape,
            final Paint paint) {
        drawBlock(g2, shape, paint, 1.0f);
    }

    /**
     * Draws a filled shape without a border.
     * @param g2 the graphic object
     * @param shape the shape to fill
     * @param paint the filling color or TexturePaint
     * @param alpha the transparency
     */
    public static void drawBlock(final Graphics2D g2, final Shape shape,
            final Paint paint, final float alpha) {
        if (shape == null) {
            return;
        }
        // save the original settings
        final Paint origPaint = g2.getPaint();
        final Composite originalComposite = g2.getComposite();
        //draw the color block
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha));
        g2.setPaint(paint);
        g2.fill(shape);
        //set the old settings
        g2.setPaint(origPaint);
        g2.setComposite(originalComposite);
    }


    /**
     * Draws the outline of the shape.
     * @param g2 the graphic object
     * @param shape the shape to draw
     * @param paint the filling color or TexturePaint
     * @param stroke the {@link Stroke} to use
     */
    public static void drawOutline(final Graphics2D g2, final Shape shape,
            final Paint paint, final Stroke stroke) {
        if (shape == null) {
            return;
        }
        // save the original settings
        final Paint origPaint = g2.getPaint();
        final Stroke origStroke = g2.getStroke();
        g2.setStroke(stroke);
        g2.setPaint(paint);
        g2.draw(shape);
        //set the old settings
        g2.setPaint(origPaint);
        g2.setStroke(origStroke);
    }

    /**
     * Draws the outline of the given arc.
     * @param g2 the graphic object
     * @param arc the arc to draw
     * @param paint the filling color or TexturePaint
     * @param stroke the {@link Stroke} to use
     */
    public static void drawArc(final Graphics2D g2, final Arc2D arc,
            final Paint paint, final BasicStroke stroke) {
        if (arc == null) {
            return;
        }
        final Arc2D outlineArc = calculateBorderArc(arc, stroke);
        // save the original settings
        final Paint origPaint = g2.getPaint();
        final Stroke origStroke = g2.getStroke();
        g2.setStroke(stroke);
        g2.setPaint(paint);
        g2.draw(outlineArc);
        //set the old settings
        g2.setPaint(origPaint);
        g2.setStroke(origStroke);
    }

    /**
     * Draws an empty rectangle.
     *
     * @param g2 the graphics object
     * @param rect the rectangle to draw
     * @param color the {@link Color} of the rectangle border
     * @param stroke the {@link BasicStroke} to use
     */
    public static void drawRectangle(final Graphics2D g2,
            final Rectangle2D rect, final Color color,
            final BasicStroke stroke) {
        if (rect == null) {
            return;
        }
        final Stroke origStroke = g2.getStroke();
        final Paint origPaint = g2.getPaint();
        final Rectangle2D borderRect =
            calculateBorderRect(rect, stroke);
        g2.setStroke(stroke);
        g2.setPaint(color);
        g2.draw(borderRect);
        //set the old settings
        g2.setStroke(origStroke);
        g2.setPaint(origPaint);
    }

    /**
     * Calculates the size of the rectangle with the given stroke.
     * @param rect the original size of the rectangle
     * @param stroke the stroke which will be used to draw the rectangle
     * @return the {@link Rectangle2D} to draw
     */
    public static Rectangle2D calculateBorderRect(final Rectangle2D rect,
            final BasicStroke stroke) {
        final int strokeWidth = (int)stroke.getLineWidth();
        final int halfStrokeWidth = strokeWidth / 2;
        final int newX = (int) rect.getX() + halfStrokeWidth;
        final int newY = (int) rect.getY() + halfStrokeWidth;
        int newWidth = (int) rect.getWidth() - strokeWidth;
        int newHeight = (int) rect.getHeight() - strokeWidth;
        //check for negative values
        if (newWidth <= 0) {
            newWidth = 1;
        }
        if (newHeight <= 0) {
            newHeight = 1;
        }
        final Rectangle2D strokeRect =
            new Rectangle(newX, newY, newWidth, newHeight);
        return strokeRect;
    }

    /**
     * Calculates the size of the arc with the given stroke.
     * @param arc the original size of the arc
     * @param stroke the stroke which will be used to draw the arc
     * @return the {@link Arc2D} to draw
     */
    public static Arc2D calculateBorderArc(final Arc2D arc,
            final BasicStroke stroke) {
        return arc;
//        final int width = (int)stroke.getLineWidth();
//        if (width <= 1) {
//            return arc;
//        }
//        final int halfStrokeWidth = width / 2;
//        final Rectangle origBounds = arc.getBounds();
//        final Rectangle bounds = new Rectangle(origBounds.x + halfStrokeWidth,
//                origBounds.y + halfStrokeWidth, origBounds.width - width,
//                origBounds.height - width);
//        final double origStart = arc.getAngleStart();
//        final double origAngle = arc.getAngleExtent();
//
//        return new Arc2D.Double(bounds, origStart, origAngle, Arc2D.PIE);
    }

}
