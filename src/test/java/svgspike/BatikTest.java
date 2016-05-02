package svgspike;

import com.google.common.primitives.Doubles;
import org.apache.batik.dom.svg.*;
import org.apache.batik.util.XMLResourceDescriptor;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import com.google.common.io.Files;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGSVGElement;


/**
 * Created by pisarenko on 10.11.2015.
 */
public final class BatikTest {
	
	private class Point{
		private double x;
		private double y;
		
		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}
		
		public Point(double x, double y){
			this.x=x;
			this.y=y;
		}
		
		public String toString(){
			return "[x="+x+", y="+y+"]";
		}
	}
	
    @Test
    public void test() throws XPathExpressionException {
        try {
            final File initialFile =
                    new File("src/test/resources/scene05_signs.svg");
            InputStream sceneFileStream = Files.asByteSource(initialFile).openStream();


            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String uri = "http://www.example.org/diagram.svg";
            final SVGOMDocument doc = (SVGOMDocument) f.createDocument(
                    uri, sceneFileStream);

            final NodeList nodes =
                    doc.getDocumentElement().getElementsByTagName("g");
            SVGOMGElement signSouth = null;


            String viewBox = doc.getDocumentElement().getAttribute("viewBox");
            
            Point referencePoint = getReferencePoint(getGroupElement(doc, "signS"));

            double signSouthX = magicallyCalculateXCoordinate(referencePoint);
            double signSouthY = magicallyCalculateYCoordinate(referencePoint, viewBox);
            
            Assert.assertEquals(109.675, signSouthX, 0.0000001);
            Assert.assertEquals(533.581, signSouthY, 0.0000001);
            
            referencePoint = getReferencePoint(getGroupElement(doc, "signN"));
            Assert.assertEquals(109.906, magicallyCalculateXCoordinate(referencePoint), 0.0000001);
            Assert.assertEquals(578.293, magicallyCalculateYCoordinate(referencePoint, viewBox), 0.0000001);
            
            referencePoint = getReferencePoint(getGroupElement(doc, "signE"));
            Assert.assertEquals(129.672, magicallyCalculateXCoordinate(referencePoint), 0.0000001);
            Assert.assertEquals(554.077, magicallyCalculateYCoordinate(referencePoint, viewBox), 0.0000001);
            
            referencePoint = getReferencePoint(getGroupElement(doc, "signW"));
            Assert.assertEquals(93.398, magicallyCalculateXCoordinate(referencePoint), 0.0000001);
            Assert.assertEquals(553.833, magicallyCalculateYCoordinate(referencePoint, viewBox), 0.0000001);
			
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    private SVGOMGElement getGroupElement(SVGOMDocument doc, String id){
        final NodeList nodes = doc.getDocumentElement().getElementsByTagName("g");
        SVGOMGElement signGroup = null;
        for (int i=0; (i < nodes.getLength()) && (signGroup == null); i++) {
            final Node curNode = nodes.item(i);
            final Node idNode = curNode.getAttributes().getNamedItem("id");
            if (id.equals(idNode.getTextContent())) signGroup = (SVGOMGElement) curNode;
        }
        return signGroup;
    }
    
    /**
     * @param signGroup
     * @return the reference point, inkscape uses for group (bottom left corner of group)
     */
    private Point getReferencePoint(SVGOMGElement signGroup){
    	
    	Point referencePoint = new Point(0, 0);
    	
    	try {
            SVGOMRectElement rectElement = (SVGOMRectElement) signGroup.getElementsByTagName("rect").item(0);
            SVGOMPathElement pathElement = (SVGOMPathElement) signGroup.getElementsByTagName("path").item(0);
            
            //get all points of interest: outer points of path and rect with applied stroke and transform
            ArrayList<Point> poi = new ArrayList<Point>();
            
            double[][] transformationMatrix = getTransformationMatrix(signGroup.getAttribute("transform"));
            
            poi.addAll(getPathPoints(pathElement, transformationMatrix));
            poi.addAll(getRectPoints(rectElement, transformationMatrix));
            
            //find xMin and yMax in poi
            double xMin = poi.get(0).getX();
            double yMax = poi.get(0).getY();
            for (Point point : poi) {
				if(xMin>point.getX()) xMin = point.getX();
				if(yMax<point.getY()) yMax = point.getY();
			}
            referencePoint = new Point(xMin, yMax);
            
		} catch (Exception e) {
			e.printStackTrace();
		}
        return referencePoint;
    }
    
    /**
     * @param pathElement path element with three points
     * @param transformMatrix 3x3 transform matrix
     * @return calculated corner points of stroked, transformed path
     */
    private ArrayList<Point> getPathPoints(SVGOMPathElement pathElement, double[][] transformMatrix){
    	ArrayList<Point> poi = new ArrayList<Point>();
    	Point[] path = new Point[3];
    	double halfStrokeWidth = getStrokeWidth(pathElement.getAttributeNode("style").getValue())/2.0;
    	double hSquared = halfStrokeWidth*halfStrokeWidth;
    	String[] dPoints = pathElement.getAttribute("d").split(" ");
    	
    	String[][] coordinates = new String[3][2];
    	for (int i = 1; i < dPoints.length; i++) {
			coordinates[i-1] = dPoints[i].split(",");
		}
    	path[0]=new Point(Double.parseDouble(coordinates[0][0]),Double.parseDouble(coordinates[0][1]));
    	path[1]=new Point(path[0].getX()+Double.parseDouble(coordinates[1][0]),path[0].getY()+Double.parseDouble(coordinates[1][1]));
    	path[2]=new Point(path[1].getX()+Double.parseDouble(coordinates[2][0]),path[1].getY()+Double.parseDouble(coordinates[2][1]));
    	// determine equation of straight line
    	double m1 = (path[1].getY()-path[0].getY())/(path[1].getX()-path[0].getX());
    	double m2 = (path[2].getY()-path[1].getY())/(path[2].getX()-path[1].getX());
    	double b1 = path[0].getY()-m1*path[0].getX();
    	double b2 = path[1].getY()-m2*path[1].getX();
    	// determine translation of outer points with intercept theorem
    	double deltaY1 = halfStrokeWidth/Math.sqrt(m1*m1+1);
    	double deltaX1 = Math.sqrt(hSquared-deltaY1*deltaY1);
    	double deltaY3 = halfStrokeWidth/Math.sqrt(m2*m2+1);
    	double deltaX3 = Math.sqrt(hSquared-deltaY3*deltaY3);
    	// apply translation to outer points 
    	poi.add(0, new Point(path[0].getX()-deltaX1, path[0].getY()-deltaY1));
    	poi.add(1, new Point(path[0].getX()+deltaX1, path[0].getY()+deltaY1));
    	poi.add(2, new Point(path[2].getX()+deltaX3, path[2].getY()-deltaY3));
    	poi.add(3, new Point(path[2].getX()-deltaX3, path[2].getY()+deltaY3));
    	// determine equation of straight lines, translated by half stroke width
    	double b3 = poi.get(0).getY()-m1*poi.get(0).getX();
    	double b4 = poi.get(2).getY()-m2*poi.get(2).getX();
    	// calculate angle point as intercept of translated lines
    	double xAngle = (b4-b3)/(m1-m2);
    	poi.add(new Point(xAngle, m1*xAngle+b3));
    	// apply transform matrix
    	return getTransformedPoints(poi, transformMatrix);
    }
    
    /**
     * @param rectElement non sheared rect element
     * @return calculated corner points of stroked, transformed rectangle
     */
    private ArrayList<Point> getRectPoints(SVGOMRectElement rectElement, double[][] transformMatrix){
    	ArrayList<Point> rect = new ArrayList<Point>();
    	double halfStrokeWidth = getStrokeWidth(rectElement.getAttributeNode("style").getValue())/2.0;
    	
    	double xValue = Double.parseDouble(rectElement.getAttribute("x"));
    	double yValue = Double.parseDouble(rectElement.getAttribute("y"));
    	double height = Double.parseDouble(rectElement.getAttribute("height"));
    	double width = Double.parseDouble(rectElement.getAttribute("width"));
    	
    	Point ref = new Point(xValue-halfStrokeWidth, yValue-halfStrokeWidth);
    	
    	rect.add(ref);
    	rect.add(new Point(ref.getX(), ref.getY()+height+2*halfStrokeWidth));
    	rect.add(new Point(ref.getX()+width+2*halfStrokeWidth, ref.getY()));
    	rect.add(new Point(ref.getX()+width+2*halfStrokeWidth, ref.getY()+height+2*halfStrokeWidth));
    	
    	return getTransformedPoints(rect, transformMatrix);
    }
    
    private double getStrokeWidth(String styleAttributeValue){
        String strokeWidthString = styleAttributeValue.substring(styleAttributeValue.indexOf("stroke-width")).split(";",2)[0];
        return Double.parseDouble(strokeWidthString.split(":")[1].replaceAll("[^\\d.,]", ""));
    }
    
    /**
     * @param transformMatrix transform string (either in matrix or transform notation)
     * @return 3x3 transform matrix
     * @throws Exception
     */
    private double[][] getTransformationMatrix(String transformMatrix) throws Exception{
    	double[][] matrix = new double[3][3];
    	matrix[2][2]=1.0;
    	
    	if(transformMatrix.contains("translate")){
    		transformMatrix=transformMatrix.replace("translate(", "matrix(1,0,0,1,");
    	}
    	if(!transformMatrix.contains("matrix")){
    		throw new Exception("Not implemented, only matrix and translate transformation.");
    	}
    	
    	String[] entries = transformMatrix.substring(transformMatrix.indexOf("(")+1,transformMatrix.indexOf(")") ).split(",");
    	
    	for(int i=0;i<2;i++){
    		for(int j=0;j<3;j++) matrix[i][j]=Double.parseDouble(entries[i+j*2]);
    	}
    	return matrix;
    }
    
    private ArrayList<Point> getTransformedPoints(ArrayList<Point> points, double[][] transformMatrix){
    	ArrayList<Point> transformed = new ArrayList<Point>();
    	for (Point point : points) {
    		double transX = transformMatrix[0][0]*point.getX()+transformMatrix[0][1]*point.getY()+transformMatrix[0][2];
    		double transY = transformMatrix[1][0]*point.getX()+transformMatrix[1][1]*point.getY()+transformMatrix[1][2];
    		transformed.add(new Point(transX, transY));
		}
    	return transformed;
    }

    /**
     * inkscape states y coordinate with origin in left bottom corner, while svg uses top left corner as origin
     * @param referencePoint bottom left corner of group
     * @param viewBox in "originX originY width height" notation
     * @return corrected y coordinate, rounded to three decimal figures (half up)
     */
    private double magicallyCalculateYCoordinate(Point referencePoint, String viewBox) {
    	String[] viewBoxValues = viewBox.split(" ");
    	BigDecimal roundedY = new BigDecimal(Double.parseDouble(viewBoxValues[3])-referencePoint.getY());
    	roundedY = roundedY.setScale(3, BigDecimal.ROUND_HALF_UP);
    	return roundedY.doubleValue();
    }

    /**
     * @param referencePoint bottom left corner of group
     * @return x coordinate, rounded to three decimal figures (half up)
     */
    private double magicallyCalculateXCoordinate(Point referencePoint) {
    	BigDecimal roundedX = new BigDecimal(referencePoint.getX()).setScale(3, BigDecimal.ROUND_HALF_UP);
    	return roundedX.doubleValue();
    }

    public double doubleNodeValue(final Node cx) {
        return Doubles.tryParse(cx.getNodeValue()).doubleValue();
    }

}
