package com.gis.wms;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WmsController {
	
	@CrossOrigin(origins = "*")
	@GetMapping("/wms")
	public void getMap(HttpServletRequest request,HttpServletResponse response) {
		MapContent map=new MapContent();
		Resource resource = new ClassPathResource("static/test.shp");
		Resource sldresource = new ClassPathResource("static/test.sld");
		Map<String,Object> params=getParams(request);
		try {
			addShpLayer(map,resource.getURL(),sldresource.getURL());
			getMapContent(map,params,response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private Map<String, Object> getParams(HttpServletRequest request) {
		String LAYERS = request.getParameter("LAYERS"),
                WIDTH = request.getParameter("WIDTH"),
                HEIGHT = request.getParameter("HEIGHT"),
                BBOX = request.getParameter("BBOX");

        int _w = Integer.parseInt(WIDTH),
                _h = Integer.parseInt(HEIGHT);

        String[] BBOXS = BBOX.split(",");
        double[] _bbox = new double[]{
            Double.parseDouble(BBOXS[0]),
            Double.parseDouble(BBOXS[1]),
            Double.parseDouble(BBOXS[2]),
            Double.parseDouble(BBOXS[3])
        };
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("bbox", _bbox);
        params.put("width", _w);
        params.put("height", _h);
        return params;
	}
	private void addShpLayer(MapContent map,URL shpPath,URL sldPath) {
		try {
			 ShapefileDataStore shpDataStore = null;
	         shpDataStore = new ShapefileDataStore(shpPath);
	         shpDataStore.setCharset(Charset.forName("utf-8"));
	         String typeName =shpDataStore.getTypeNames()[0];
	         SimpleFeatureSource featureSource = null;
	          featureSource = shpDataStore.getFeatureSource(typeName);
	          Style style = null;
	          if (sldPath != null) {
	                StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
	                SLDParser stylereader = new SLDParser(styleFactory, sldPath);
	                Style[] styles=(Style[]) stylereader.readXML();
	                style = styles[0];
	            } else {
	                SLD.setPolyColour((org.geotools.styling.Style) style, Color.GREEN);
	            }
	          Layer layer =new FeatureLayer(featureSource,(org.geotools.styling.Style) style);
	          map.addLayer(layer);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void getMapContent(MapContent map,Map params,HttpServletResponse response) {
		 try{
	            double[] bbox = (double[]) params.get("bbox");
	            double x1 = bbox[0], y1 = bbox[1],
	                    x2 = bbox[2], y2 = bbox[3];
	            int width = (Integer) params.get("width"),
	                    height=(Integer) params.get("height");

	            // 设置输出范围
	            CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
	            ReferencedEnvelope mapArea = new ReferencedEnvelope(x1, x2, y1, y2, crs);
	            // 初始化渲染器
	            StreamingRenderer sr = new StreamingRenderer();
	            sr.setMapContent(map);

	            BufferedImage bi = new BufferedImage(width, height,
	                    BufferedImage.TYPE_INT_ARGB);
	            Graphics g = bi.getGraphics();
	            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                    RenderingHints.VALUE_ANTIALIAS_ON);
	            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	            Rectangle rect = new Rectangle(0, 0, width, height);//图片大小
	            // 绘制地图
	            sr.paint((Graphics2D) g, rect, mapArea);
	            response.setContentType(MediaType.IMAGE_PNG_VALUE);
	            OutputStream os = response.getOutputStream();
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	           
	            try {
	            	boolean flag = ImageIO.write(bi, "png", os);
	                os.flush();
	                map.dispose();
	            }
	            catch (IOException e) {
	                e.printStackTrace();
	            }
	            finally {
	                os.close();
	                map.dispose();
	            }
	        }
	        catch(Exception e){
	            e.printStackTrace();
	        }
	}
}
