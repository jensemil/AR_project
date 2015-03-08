package dk.au.cs;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

import java.util.List;

/**
 * Created by Birk on 08-03-2015.
 */
public class ImageData {
    private List<MatOfPoint2f> polygons;
    private List<MatOfPoint> contoursCvt;
    private int indexOfPositiveMatch;

    public ImageData(List<MatOfPoint2f> polygons, List<MatOfPoint> contoursCvt, int indexOfPositiveMatch) {
        this.polygons = polygons;
        this.contoursCvt = contoursCvt;
        this.indexOfPositiveMatch = indexOfPositiveMatch;
    }

    public List<MatOfPoint> getContoursCvt() {
        return contoursCvt;
    }

    public int getIndexOfPositiveMatch() {
        return indexOfPositiveMatch;
    }


    public List<MatOfPoint2f> getPolygons() {
        return polygons;
    }
}
