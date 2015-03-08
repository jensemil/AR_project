package dk.au.cs;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

/**
 * Created by Birk on 08-03-2015.
 */
public interface ClassifySquareStrategy {
    public boolean isSquare(MatOfPoint polygonCvt, MatOfPoint2f polygon);
}
