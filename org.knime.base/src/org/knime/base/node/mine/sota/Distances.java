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
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class Distances {
    private Distances() {
    }

    /**
     * Returns the euclidean distance between the cells values and the number
     * cells of the given row.
     * 
     * @param row row to compute the distance
     * @param cell cell to compute the distance
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the euclidian distance between given row and cell
     */
    public static double getEuclideanDistance(final DataRow row,
            final SotaTreeCell cell, final boolean fuzzy) {
        int col = 0;
        double distance = 0;
        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    distance += Math.pow(
                            (cell.getData()[col].getValue() - ((DoubleValue)row
                                    .getCell(i)).getDoubleValue()), 2);
                    col++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    distance += Math
                            .pow(
                                    cell.getData()[col].getValue()
                                            - SotaFuzzyMath
                                                    .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                                            .getCell(i)), 2);
                    col++;
                }
            }
        }
        return Math.sqrt(distance);
    }

    /**
     * Calculates the euclidean distance between two rows. Only Number cell
     * columns are used to calculate the distance.
     * 
     * @param row1 the first row
     * @param row2 the second row
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return distance between the two rows
     */
    public static double getEuclideanDistance(final DataRow row1,
            final DataRow row2, final boolean fuzzy) {
        double distance = 0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type1 = row1.getCell(i).getType();
            DataType type2 = row2.getCell(i).getType();

            if (SotaUtil.isNumberType(type1) && SotaUtil.isNumberType(type2)
                    && !fuzzy) {
                distance += Math.pow((((DoubleValue)row1.getCell(i))
                        .getDoubleValue() - ((DoubleValue)row2.getCell(i))
                        .getDoubleValue()), 2);
            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {
                distance += Math.pow((SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)) - SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                .getCell(i))), 2);
            }
        }
        return Math.sqrt(distance);
    }

    /**
     * Computes the cosinus distance between the given two rows, with given
     * offset.
     * 
     * @param row1 first row to compute the cosinus distance of
     * @param row2 second row to compute the cosinus distance of
     * @param offset offset to substract cosinus distance from
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the cosinus distance between the given two rows
     */
    public static double getCosinusDistance(final DataRow row1,
            final DataRow row2, final double offset, final boolean fuzzy) {
        double distance = 0;
        double vectorMultRes = 0;
        double vector1Length = 0;
        double vector2Length = 0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type1 = row1.getCell(i).getType();
            DataType type2 = row2.getCell(i).getType();

            if (SotaUtil.isNumberType(type1) && SotaUtil.isNumberType(type2)
                    && !fuzzy) {

                vectorMultRes += ((DoubleValue)row1.getCell(i))
                        .getDoubleValue()
                        * ((DoubleValue)row2.getCell(i)).getDoubleValue();

                vector1Length += Math.pow(((DoubleValue)row1.getCell(i))
                        .getDoubleValue(), 2);

                vector2Length += Math.pow(((DoubleValue)row2.getCell(i))
                        .getDoubleValue(), 2);

            } else if (SotaUtil.isFuzzyIntervalType(type1)
                    && SotaUtil.isFuzzyIntervalType(type2) && fuzzy) {

                vectorMultRes += SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i))
                        * SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                        .getCell(i));

                vector1Length += Math.pow(SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)), 2);

                vector2Length += Math.pow(SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                .getCell(i)), 2);
            }
        }

        vector1Length = Math.sqrt(vector1Length);
        vector2Length = Math.sqrt(vector2Length);
        distance = vectorMultRes / (vector1Length * vector2Length);

        distance = offset - distance;
        return distance;
    }

    /**
     * Returns the cosinus distance between the cells values and the number
     * cells of the given row with a given offset.
     * 
     * @param row row to compute the cosinus distance of
     * @param cell cell to compute the cosinus distance of
     * @param offset offset to substract cosinus distance from
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the cosinus distance between given row and cell
     */
    public static double getCosinusDistance(final DataRow row,
            final SotaTreeCell cell, final double offset, final boolean fuzzy) {
        int col = 0;
        double distance = 0;
        double vectorMultRes = 0;
        double vectorLength = 0;
        double cellLength = 0;
        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    vectorMultRes += cell.getData()[col].getValue()
                            * ((DoubleValue)row.getCell(i)).getDoubleValue();

                    vectorLength += Math.pow(((DoubleValue)row.getCell(i))
                            .getDoubleValue(), 2);

                    cellLength += Math.pow(cell.getData()[col].getValue(), 2);

                    col++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    vectorMultRes += cell.getData()[col].getValue()
                            * SotaFuzzyMath
                                    .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                            .getCell(i));

                    vectorLength += Math.pow(SotaFuzzyMath
                            .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                    .getCell(i)), 2);

                    cellLength += Math.pow(cell.getData()[col].getValue(), 2);

                    col++;
                }
            }
        }

        vectorLength = Math.sqrt(vectorLength);
        cellLength = Math.sqrt(cellLength);
        distance = vectorMultRes / (vectorLength * cellLength);

        distance = offset - distance;
        return distance;
    }

    /**
     * Returns the coefficient of correlation distance between the cells values
     * and the number cells of the given row with a given offset.
     * 
     * @param row row to compute the coefficient of correlation
     * @param cell cell to compute the coefficient of correlation
     * @param offset offset to substract coefficient of correlation from
     * @param abs flags if correlations distance should be used absolute
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the coefficient of correlation between given row and cel
     */
    public static double getCorrelationDistance(final DataRow row,
            final SotaTreeCell cell, final double offset, final boolean abs,
            final boolean fuzzy) {
        int col = 0;
        double dist = 0;
        double meanRow = Distances.getMean(row, fuzzy);
        double meanCell = Distances.getMean(cell);
        double devRow = Distances.getStandardDeviation(row, fuzzy);
        double devCell = Distances.getStandardDeviation(cell);

        if (devRow == 0 || devCell == 0) {
            return (offset - 0);
        }

        int count = 0;

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                if (col < cell.getData().length) {
                    dist += (cell.getData()[col].getValue() - meanCell)
                            * (((DoubleValue)row.getCell(i)).getDoubleValue() - meanRow);
                    col++;
                    count++;
                }
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                if (col < cell.getData().length) {
                    dist += (cell.getData()[col].getValue() - meanCell)
                            * (SotaFuzzyMath
                                    .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                            .getCell(i)) - meanRow);
                    col++;
                    count++;
                }
            }
        }

        dist = offset - (dist / (count * devRow * devCell));
        if (abs) {
            dist = Math.abs(dist);
        }

        return dist;
    }

    /**
     * Returns the coefficient of correlation distance between the rows with a
     * given offset.
     * 
     * @param row1 first row to compute the coefficient of correlation
     * @param row2 second rell to compute the coefficient of correlation
     * @param offset offset to substract coefficient of correlation from
     * @param abs flags if correlations distance should be used absolute
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the coefficient of correlation between given rows
     */
    public static double getCorrelationDistance(final DataRow row1,
            final DataRow row2, final double offset, final boolean abs,
            final boolean fuzzy) {
        double dist = 0;
        double meanRow1 = Distances.getMean(row1, fuzzy);
        double meanRow2 = Distances.getMean(row2, fuzzy);
        double devRow1 = Distances.getStandardDeviation(row1, fuzzy);
        double devRow2 = Distances.getStandardDeviation(row2, fuzzy);

        if (devRow1 == 0 || devRow2 == 0) {
            return (offset - 0);
        }

        int count = 0;

        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType type = row1.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                dist += (((DoubleValue)row1.getCell(i)).getDoubleValue() - meanRow1)
                        * (((DoubleValue)row2.getCell(i)).getDoubleValue() - meanRow2);
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                dist += (SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row1
                                .getCell(i)) - meanRow1)
                        * (SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)row2
                                        .getCell(i)) - meanRow2);
                count++;
            }
        }

        dist = offset - (dist / (count * devRow1 * devRow2));
        if (abs) {
            dist = Math.abs(dist);
        }

        return dist;
    }

    /**
     * Returns the standard deviation of the given row.
     * 
     * @param row the row to compute the standard deviation of.
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the standard deviation of the given row
     */
    public static double getStandardDeviation(final DataRow row,
            final boolean fuzzy) {
        double dev = 0;
        int count = 0;
        double mean = Distances.getMean(row, fuzzy);

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                dev += Math
                        .pow(
                                (((DoubleValue)row.getCell(i)).getDoubleValue() - mean),
                                2);
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                dev += Math.pow((SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                .getCell(i)) - mean), 2);
                count++;
            }
        }
        return Math.sqrt((dev / (count - 1)));
    }

    /**
     * Returns the standard deviation of the given cell.
     * 
     * @param cell the SotaTreeCell to compute the standard deviation of
     * @return the standard deviation of the given cell
     */
    public static double getStandardDeviation(final SotaTreeCell cell) {
        double dev = 0;
        int count = 0;
        double mean = Distances.getMean(cell);

        for (int i = 0; i < cell.getData().length; i++) {
            dev += Math.pow((cell.getData()[i].getValue() - mean), 2);
            count++;
        }
        return Math.sqrt((dev / (count - 1)));
    }

    /**
     * Returns the mean value of the given row.
     * 
     * @param row row to get the mean value of
     * @param fuzzy if <code>true</code> only fuzzy data is respected, if
     *            <code>false</code> only number data
     * @return the mean value of the given row
     */
    public static double getMean(final DataRow row, final boolean fuzzy) {
        double mean = 0;
        int count = 0;

        for (int i = 0; i < row.getNumCells(); i++) {
            DataType type = row.getCell(i).getType();

            if (SotaUtil.isNumberType(type) && !fuzzy) {
                mean += ((DoubleValue)row.getCell(i)).getDoubleValue();
                count++;
            } else if (SotaUtil.isFuzzyIntervalType(type) && fuzzy) {
                mean += SotaFuzzyMath
                        .getCenterOfCoreRegion((FuzzyIntervalValue)row
                                .getCell(i));
                count++;
            }
        }
        return (mean / count);
    }

    /**
     * Returns the mean value of the given cell.
     * 
     * @param cell SotaTreeCell to get the mean value of
     * @return the mean value of the given cell
     */
    public static double getMean(final SotaTreeCell cell) {
        double mean = 0;
        int count = 0;

        for (int i = 0; i < cell.getData().length; i++) {
            mean += cell.getData()[i].getValue();
            count++;
        }
        return (mean / count);
    }
}
