import java.util.*;
import java.io.*;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class imageProcessor {

    // Display Variables
    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;

    // Storage Variables
    double[][] pointValues;

    int[][] pixel_matrix;

    // Input Variables
    byte[] bytes;
    int width,height,size;


    public imageProcessor(){

        // The (width * height) is going to be constant for all images
        this.width = 352;
        this.height = 288;
        this.size = 352*288;
        pixel_matrix = new int[height][width];
        pointValues = new double[3][size];

    }

    public ArrayList<Integer> findBits(int noofvalues){

        ArrayList<Integer> values = new ArrayList<>();

        double delta = 256.0/noofvalues;

        double lastValue= delta;

        values.add(0);
        values.add((int)Math.round(delta));

        int index=2;
        while(index<noofvalues){

            lastValue+=delta;
            values.add((int)Math.round(lastValue));
            index+=1;

        }

        return values;
    }

    public void re_qunatize(int num){

        ArrayList<Integer> bits_num = findBits(num);

        double delta = 256.0/num;

        for(int index=0; index<size;index++){

            for(int color=0;color<3;color++){

                int valIndex = (int)Math.round(pointValues[color][index]/delta);
                if(valIndex>num-1){
                    valIndex=num-1;
                }
                pointValues[color][index]=bits_num.get(valIndex);
            }
        }
    }


    public double checkOverflow(double x){

        if(x<0){
            return 255+x;
        }
        return x;
    }

    public double snapToZero(double x){
        if(x<0){
            return 0;
        }
        return x;
    }

    public void populatePixel(){

        int index=0;
        for(int i=0;i<height;i++)
        {
            for(int j=0;j<width;j++){

                double R = pointValues[0][index];
                double G = pointValues[1][index];
                double B = pointValues[2][index];


                int pix = 0xff000000 | (((byte)R & 0xff) << 16) | (((byte)G & 0xff) << 8) | ((byte)B & 0xff);

                pixel_matrix[i][j]=pix;
                index++;
            }
        }
    }

    public BufferedImage populate_image(){


        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                img.setRGB(j, i, pixel_matrix[i][j]);
            }
        }

        return img;
    }

    public void readRGBFile(String filePath) {
        try {
            File file = new File(filePath);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

        } catch (FileNotFoundException ex){
            ex.printStackTrace();
        } catch (IOException ex){
            ex.printStackTrace();
        }

    }

    public void extract_RGB(){

        int index = 0;
        for(int y = 0; y < height; y++){

            for(int x = 0; x < width; x++){

                // Calculating the R, G & B values from input file
                Byte R = bytes[index];
                Byte G = bytes[index + size];
                Byte B = bytes[index + (size*2)];

                // Total points in the pointValues - 288*352
                pointValues[0][index]= checkOverflow((double)R.intValue());
                pointValues[1][index]= checkOverflow((double)G.intValue());
                pointValues[2][index]= checkOverflow((double)B.intValue());

                index++;
            }
        }
    }

    public void convertRGB_YUV(){

        for(int index=0; index<size;index++){

            double R = pointValues[0][index];
            double G = pointValues[1][index];
            double B = pointValues[2][index];

            // Conversion of RGB to YUV
            pointValues[0][index] = (R*0.299) + (G*0.587) + (B*0.114);
            pointValues[1][index] = (R*0.596) + (G*-0.274) + (B*-0.322);
            pointValues[2][index] = (R*0.211) + (G*-0.523) + (B*0.312);

        }
    }

    public void subSample(int y,int u,int v){

        for(int row=0; row<height; row++){

            for(int col=0;col<width;col++){

                if(col%y != 0 && y!=1){
                    pointValues[0][row*width+col]=Double.MAX_VALUE;
                }

                if(col%u != 0 && u!=1){
                    pointValues[1][row*width+col]=Double.MAX_VALUE;
                }

                if(col%v != 0 && v!=1){
                    pointValues[2][row*width+col]=Double.MAX_VALUE;
                }
            }

        }
    }

    public void upSample_component(int scale,int type) {

        for(int row=0; row<height; row++){

            for(int col=0;col<width;col++){

                if(col%scale!=0){
                    int st = col - (col % scale);
                    int en = st+scale;

                    if(en<width){
                        pointValues[type][row*width+col] = (pointValues[type][row*width+st] + pointValues[type][row*width+en]) / 2;
                    }
                    else{
                        pointValues[type][row*width+col] = pointValues[type][row*width+st];
                    }
                }
            }
        }
    }

    public void upSample(int y,int u,int v){

        if(y>1){
            upSample_component(y,0);
        }

        if(u>1){
            upSample_component(u,1);
        }

        if(v>1){
            upSample_component(v,2);
        }
    }

    public void convertYUV_RGB(){

        for(int index=0; index<size;index++){

            double Y = pointValues[0][index];
            double U = pointValues[1][index];
            double V = pointValues[2][index];

            // Conversion of YUV to RGB
            pointValues[0][index] = snapToZero(Math.round((Y*1.000) + (U*0.956) + (V*0.621)));
            pointValues[1][index] = snapToZero(Math.round((Y*1.000) + (U*-0.272) + (V*-0.647)));
            pointValues[2][index] = snapToZero(Math.round((Y*1.000) + (U*-1.106) + (V*1.703)));
        }
    }


    public void showImages(BufferedImage img1,BufferedImage img2){

        // Use labels to display the images
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbText1 = new JLabel("Original image (Left)");
        lbText1.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lbText2 = new JLabel("Image after modification (Right)");
        lbText2.setHorizontalAlignment(SwingConstants.CENTER);
        lbIm1 = new JLabel(new ImageIcon(img1));
        lbIm2 = new JLabel(new ImageIcon(img2));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame.getContentPane().add(lbText1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        frame.getContentPane().add(lbText2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        frame.getContentPane().add(lbIm2, c);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        imageProcessor ren = new imageProcessor();

        String location = args[0];
        int scaleY = Integer.parseInt(args[1]);
        int scaleU = Integer.parseInt(args[2]);
        int scaleV = Integer.parseInt(args[3]);
        int quantize = Integer.parseInt(args[4]);

        if(scaleY<1 || scaleU<1 || scaleV<1){
            System.out.println("Invalid scale value for Y, U or V");
        }
        else if(quantize<1 || quantize>256){
            System.out.println("Invalid quantization value");
        }
        else {

            // Reads the rgb file
            ren.readRGBFile(location);

            // Extract the RGB value for processing
            ren.extract_RGB();

            // Calculate the pixel value
            ren.populatePixel();

            // Populate the buffered image
            BufferedImage img1 = ren.populate_image();

            if(scaleY!=1 || scaleU!=1 || scaleV!=1) {

                // Convert to YUV space
                ren.convertRGB_YUV();

                ren.subSample(scaleY, scaleU, scaleV);

                ren.upSample(scaleY, scaleU, scaleV);

                // Convert to RGB space
                ren.convertYUV_RGB();
            }

            if(quantize<256) {

                // Re-Quantize the image
                ren.re_qunatize(quantize);
            }

            // Re - Calculate the pixel value
            ren.populatePixel();

            // Populate the buffered image
            BufferedImage img2 = ren.populate_image();

            // Display the images (original and modified)
            ren.showImages(img1, img2);
        }
    }

}
