package dk.au.cs;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import weka.classifiers.trees.J48;
import weka.core.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.isContourConvex;

/**
 * Created by Birk on 08-03-2015.
 */
public class MLClassifySquareStrategy implements ClassifySquareStrategy {
    private J48 classifier;

    public MLClassifySquareStrategy() {
        classifier = new J48();
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("./squareClassifier.model"));
            classifier = (J48) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isSquare(MatOfPoint polygonCvt, MatOfPoint2f polygon) {
        List<MatOfPoint2f> polygons = new ArrayList<MatOfPoint2f>();
        List<MatOfPoint> polygonsCvt = new ArrayList<MatOfPoint>();
        polygons.add(polygon);
        polygonsCvt.add(polygonCvt);
        ImageData data = new ImageData(polygons, polygonsCvt, 100);
        List<ImageData> dataSet = new ArrayList<ImageData>();
        dataSet.add(data);
        Instances originalTrain = TrainMarkers.createArff(dataSet);
        originalTrain.setClassIndex(originalTrain.numAttributes() - 1);
        double value = 1;
        String prediction = "";
        try {
            value = classifier.classifyInstance(originalTrain.instance(0));
            prediction = originalTrain.classAttribute().value((int)value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prediction.equals("square");
    }
}
