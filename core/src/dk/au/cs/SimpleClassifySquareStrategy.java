package dk.au.cs;

import com.badlogic.gdx.math.Vector2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.isContourConvex;

/**
 * Created by Birk on 08-03-2015.
 */
public class SimpleClassifySquareStrategy implements ClassifySquareStrategy{

    @Override
    public boolean isSquare(MatOfPoint polygonCvt, MatOfPoint2f polygon) {
        return (polygon.size().height == 4 && contourArea(polygonCvt) > 4000 && isContourConvex(polygonCvt));
    }

}
