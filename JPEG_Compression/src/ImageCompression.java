import java.util.*;
import java.io.*;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class ImageCompression {

    // Display Variables
    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;

    // Storage Variables
    double[][] pointValues;
    double[][] matrix_R;
    double[][] matrix_G;
    double[][] matrix_B;

    ArrayList<double[][]> matrix_R_split,R_matrix;
    ArrayList<double[][]> matrix_G_split,G_matrix;
    ArrayList<double[][]> matrix_B_split,B_matrix;

    public BufferedImage img1,img2;


    int[][] pixel_matrix;

    // Input Variables
    byte[] bytes;
    int width,height,size, blockSize,latency;
    double qValue;
    int[] diagonalIndex;
    double maxnum;


    public ImageCompression(int Quantization,int Latency){

        // The (width * height) is going to be constant for all images
        this.width = 352;
        this.maxnum=0;
        this.height = 288;
        this.size = 352*288;
        this.blockSize = 8;
        this.qValue = Math.pow(2,Quantization);
        this.latency = Latency;
        this.diagonalIndex = new int[]{0,0,1,0,0,1,2,0,1,1,0,2,3,0,2,1,1,2,0,3,4,0,3,1,2,2,1,3,0,4,5,0,4,1,3,2,2,3,1,4,0,5,6,0,5,1,4,2,3,3,2,4,1,5,0,6,7,0,6,1,5,2,4,3,3,4,2,5,1,6,0,7,7,1,6,2,5,3,4,4,3,5,2,6,1,7,7,2,6,3,5,4,4,5,3,6,2,7,7,3,6,4,5,5,4,6,3,7,7,4,6,5,5,6,4,7,7,5,6,6,5,7,7,6,6,7,7,7};

        matrix_R = new double[height][width];
        matrix_B = new double[height][width];
        matrix_G = new double[height][width];

        matrix_R_split = new ArrayList<>();
        matrix_G_split = new ArrayList<>();
        matrix_B_split = new ArrayList<>();

        pixel_matrix = new int[height][width];

        frame=new JFrame();
    }

    // Core function for reading and writing the images

    /**
     *  Splits a channel Matrix into blocks
     *
     * @param channelMatrix
     * @return  ArrayList of 8X8 blocks
     */
    public ArrayList<double[][]> splitBlocks(double[][] channelMatrix){

        ArrayList<double[][]> tempSplit = new ArrayList<>();

        for(int row=0;row<height;row+=blockSize)
        {
            for(int col=0;col<width;col+=blockSize)
            {

                double[][] temp = new double[blockSize][blockSize];

                for(int i=row,k=0;i<row+blockSize && k<blockSize;i++,k++)
                {
                    for(int j=col,l=0;j<col+blockSize && l<blockSize;j++,l++)
                    {
                        temp[k][l]=channelMatrix[i][j];
                    }
                }

                tempSplit.add(temp);
            }
        }

        return tempSplit;
    }

    /**
     * Calls the split function for each channel
     */
    public void splitBlocksHelper(){

        matrix_R_split = splitBlocks(matrix_R);
        matrix_B_split = splitBlocks(matrix_B);
        matrix_G_split = splitBlocks(matrix_G);

    }

    /**
     *  Combines all the blocks in ArrayList into single channel Matrix
     *
     * @param channelSplitMatrix
     * @param channelMatrix
     * @param offset
     * @return combined channel Matrix
     */
    public double[][] unsplitBlock(ArrayList<double[][]> channelSplitMatrix, double[][] channelMatrix, int offset){

        int rowoffset=0, coloffset=0;
        int _size = channelSplitMatrix.size();

        if(offset>0){

            int count = offset * blockSize;

            coloffset = count%width;
            rowoffset = (int)Math.floor(count/width)*blockSize;
            _size =offset+1;
        }

        int index=offset;


        for(int row =rowoffset; row < height && index < _size; row+=blockSize)
        {
            for(int col=coloffset; col< width && index < _size;col+=blockSize){

                double[][] temp = channelSplitMatrix.get(index);

                for(int _row=0;_row<blockSize;_row++) {
                    for (int _col = 0; _col < blockSize; _col++) {
                        channelMatrix[row + _row][col + _col] = temp[_row][_col];
                    }
                }
                index++;

            }
        }
        return channelMatrix;
    }

    /**
     * Call the unsplit for each channel ArrayList
     * @param offset
     */
    public void unsplitBlockHelper(int offset){
        unsplitBlock(matrix_R_split,matrix_R,offset);
        unsplitBlock(matrix_G_split,matrix_G,offset);
        unsplitBlock(matrix_B_split,matrix_B,offset);
    }

    /**
     *  Performs a DCT transform on a matrix
     * @param matrix
     * @return DCT transformed matrix
     */
    public double[][] dctTransform(double[][] matrix){

        double[][] block_matrix = new double[blockSize][blockSize];
        for (int u=0;u<blockSize;u++) {
            for (int v=0;v<blockSize;v++) {

                double sum = 0.0;
                double cu=1,cv=1;
                if (u == 0)
                    cu = 1 / Math.sqrt(2);

                if (v == 0)
                    cv = 1 / Math.sqrt(2);

                for (int i=0;i<blockSize;i++) {
                    for (int j=0;j<blockSize;j++) {
                        sum+=Math.cos(((2*i+1)/(2.0*blockSize))*u*Math.PI)*Math.cos(((2*j+1)/(2.0*blockSize))*v*Math.PI)*matrix[i][j];
                    }
                }
                sum*=((cu*cv)/4.0);
                this.maxnum = Math.max(this.maxnum,Math.abs(sum));
                block_matrix[u][v]=Math.round(sum/qValue);
            }
        }
        return block_matrix;
    }

    /**
     * Calls the DCT Transform for each block in ArrayList
     * @param matrix_split
     * @return
     */
    public ArrayList<double[][]> dctTransformConvert(ArrayList<double[][]> matrix_split){

        for(int index =0;index<matrix_split.size();index++){
            matrix_split.set(index,dctTransform(matrix_split.get(index)));
        }

        return matrix_split;
    }

    /**
     * Calls DCT Transform Convert for each channel
     */
    public void dctTransformHelper(){

        matrix_R_split = dctTransformConvert(matrix_R_split);
        matrix_G_split = dctTransformConvert(matrix_G_split);
        matrix_B_split = dctTransformConvert(matrix_B_split);
    }

    /**
     *  Performs Inverse DCT Transform on a matrix
     * @param matrix
     * @return
     */
    public double[][] inverseDCTTransform(double[][] matrix) {

        double[][] block_matrix = new double[blockSize][blockSize];

        double cu, cv;

        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                double sum = 0.0;
                for (int u = 0; u < blockSize; u++) {
                    for (int v = 0; v < blockSize; v++) {

                        cu = 1;
                        cv = 1;

                        if (u == 0)
                            cu = 1 / Math.sqrt(2);

                        if (v == 0)
                            cv = 1 / Math.sqrt(2);

                        sum += (cu * cv) / 4.0 * Math.cos(((2 * i + 1) / (2.0 * blockSize)) * u * Math.PI) * Math.cos(((2 * j + 1) / (2.0 * blockSize)) * v * Math.PI) * matrix[u][v];
                    }
                }
                sum *= qValue;
                if (sum < 0)
                    block_matrix[i][j] = 0;
                else if (sum > 255)
                    block_matrix[i][j] = 255;
                else
                    block_matrix[i][j] = sum;

            }

        }
        return block_matrix;
    }

    /**
     * Performs inverse DCT transform on each block of ArrayList
     * @param matrix_split
     * @return
     */
    public ArrayList<double[][]> inverseDctTransformConvert(ArrayList<double[][]> matrix_split){

        for(int index =0;index<matrix_split.size();index++){
            matrix_split.set(index,inverseDCTTransform(matrix_split.get(index)));
        }

        return matrix_split;
    }

    /**
     * Invokes the inverse transform function for each channel
     */
    public void inverseDctTransformHelper(){

        matrix_R_split = inverseDctTransformConvert(matrix_R_split);
        matrix_G_split = inverseDctTransformConvert(matrix_G_split);
        matrix_B_split = inverseDctTransformConvert(matrix_B_split);
    }

    /**
     * All the helper function:
     *  Reading image
     *  Extracting R, G, B values
     *  Display Images
     *  Calculate each pixel value
     */

    public double checkOverflow(double x){

        if(x<0){
            return 255+x;
        }
        return x;
    }

    /*
    Calculate the pixel values from R, G & B
    * */
    public void populatePixel(){

        for(int i=0;i<height;i++)
        {
            for(int j=0;j<width;j++)
            {

                double R = matrix_R[i][j];
                double G = matrix_G[i][j];
                double B = matrix_B[i][j];

                int pix = 0xff000000 | (((byte)R & 0xff) << 16) | (((byte)G & 0xff) << 8) | ((byte)B & 0xff);

                pixel_matrix[i][j]=pix;
            }
        }
    }

    /*
    Populate the image pixel values from R, G, B matrix
    * */
    public BufferedImage populate_image(){


        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                img.setRGB(j, i, pixel_matrix[i][j]);
            }
        }

        return img;
    }

    /*
    Populate the image pixel values from R, G, B matrix
    * */
    public void populate_image(int offset, double[][] mat_r, double[][] mat_g, double[][] mat_b){

        int rowoffset=0, coloffset=0;
        if(offset>0){

            int count = offset * blockSize;

            coloffset = count%width;
            rowoffset = (int)Math.floor(count/width)*blockSize;
        }

        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {

                double R = mat_r[i][j];
                double G = mat_g[i][j];
                double B = mat_b[i][j];

                int pix = 0xff000000 | (((byte)R & 0xff) << 16) | (((byte)G & 0xff) << 8) | ((byte)B & 0xff);

                img2.setRGB(j+coloffset, i+rowoffset, pix);
            }
        }
    }

    /*
    Read the given RGB file to extract the bytes
    * */
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

    /*
    Extract the R,G,B components from the bytes array
    * */
    public void extract_RGB(){

        int index=0;

        for(int y = 0; y < height; y++){

            for(int x = 0; x < width; x++){

                // Calculating the R, G & B values from input file
                Byte R = bytes[index];
                Byte G = bytes[index + size];
                Byte B = bytes[index + (size*2)];

                // Total points in the pointValues - 288*352
                matrix_R[y][x] = checkOverflow((double)R.intValue());
                matrix_G[y][x] = checkOverflow((double)G.intValue());
                matrix_B[y][x] = checkOverflow((double)B.intValue());

                index++;
            }
        }
    }

    /*
    Display the images side by side
    * */
    public void showImages(BufferedImage img1,BufferedImage img2,String addition){

        GridBagConstraints c = new GridBagConstraints();

        if(frame.getContentPane().getComponents().length<=0)
        {
            // Use labels to display the images
            GridBagLayout gLayout = new GridBagLayout();
            frame.getContentPane().setLayout(gLayout);

            JLabel lbText1 = new JLabel("Original image (Left)");
            lbText1.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel lbText2 = new JLabel("Image after modification (Right)");
            lbText2.setHorizontalAlignment(SwingConstants.CENTER);


            lbIm1 = new JLabel(new ImageIcon(img1));
            lbIm2 = new JLabel(new ImageIcon(img2));



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
        }
        else{

            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 1;
            c.gridy = 0;


            JLabel lbText2 = new JLabel("Image after modification (Right) : " + addition);
            lbText2.setHorizontalAlignment(SwingConstants.CENTER);

            frame.getContentPane().remove(1);
            frame.getContentPane().add(lbText2,c,1);

            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 1;
            c.gridy = 1;

            lbIm2 = new JLabel(new ImageIcon(img2));
            frame.getContentPane().remove(3);
            frame.getContentPane().add(lbIm2, c,3);


        }


        frame.pack();
        frame.setVisible(true);
    }


    // Various display modes

    public void resetChannelMatrix(){

        R_matrix = (ArrayList<double[][]>) matrix_R_split.clone();
        G_matrix = (ArrayList<double[][]>) matrix_G_split.clone();
        B_matrix = (ArrayList<double[][]>) matrix_B_split.clone();

        matrix_R_split.clear();
        matrix_G_split.clear();
        matrix_B_split.clear();

        matrix_R = new double[height][width];
        matrix_G = new double[height][width];
        matrix_B = new double[height][width];
    }

    public double[][] getMSBBits(int n, double[][] matrix_x){

        double[][] result = new double[blockSize][blockSize];

        for(int row=0;row<blockSize;row++){
            for(int col=0;col<blockSize;col++){
                if(matrix_x[row][col] >= 0)
                    result[row][col]= (int)matrix_x[row][col] & (0xffffffff & (0xffffffff<<(32-n)));
                else
                    result[row][col]= -1 * ((int)(matrix_x[row][col] * -1) & (0xffffffff & (0xffffffff<<(32-n))));
            }
        }
        return result;
    }

    public int findMaxBits(){
        int bits = 0;
        int maxnum = (int) this.maxnum;
        while(maxnum > 0){
            bits++;
            maxnum >>= 1;
        }

        return bits;
    }

    public void successiveBitApprox(){

        this.resetChannelMatrix();

        int length  = R_matrix.size();
        int maxlength = 32;

        int maxBits = findMaxBits();

        for(int index =0; index<length;index++){
            double[][] temp = new double[blockSize][blockSize];
            matrix_R_split.add(temp);
            matrix_G_split.add(temp.clone());
            matrix_B_split.add(temp.clone());
        }

        for(int iter =32-maxBits-1; iter<=maxlength;iter++){

            for(int index =0;index<length;index++){

                matrix_R_split.set(index,getMSBBits(iter,R_matrix.get(index)));
                matrix_G_split.set(index,getMSBBits(iter,G_matrix.get(index)));
                matrix_B_split.set(index,getMSBBits(iter,B_matrix.get(index)));
            }

            inverseDctTransformHelper();

            unsplitBlockHelper(0);

            populatePixel();

            BufferedImage img2 = this.populate_image();

            this.showImages(this.img1,img2,"sending "+iter+" most significant bits");

            try {
                Thread.sleep(latency);
            }catch (InterruptedException ex)
            {
                System.out.println(ex.getMessage());
            }

        }


    }

    public void spectralSelection(){

        this.resetChannelMatrix();

        int length  = R_matrix.size();

        ArrayList<double[][]> R = new ArrayList<double[][]>();
        ArrayList<double[][]> G = new ArrayList<double[][]>();
        ArrayList<double[][]> B = new ArrayList<double[][]>();


        for(int index =0; index<length;index++){
            double[][] temp = new double[blockSize][blockSize];
            R.add(temp);
            temp = new double[blockSize][blockSize];
            G.add(temp);
            temp = new double[blockSize][blockSize];
            B.add(temp);
        }

        int ctr=0;
        for(int outIndex=0;outIndex<blockSize*blockSize;outIndex+=2){
                int row = diagonalIndex[outIndex];
                int col = diagonalIndex[outIndex+1];

                ctr++;
                for(int index=0;index<length;index++)
                {

                    R.get(index)[row][col] = R_matrix.get(index)[row][col];
                    G.get(index)[row][col] = G_matrix.get(index)[row][col];
                    B.get(index)[row][col] = B_matrix.get(index)[row][col];
                }

                matrix_R_split = (ArrayList<double[][]>) R.clone();
                matrix_G_split = (ArrayList<double[][]>) G.clone();
                matrix_B_split = (ArrayList<double[][]>) B.clone();

                this.inverseDctTransformHelper();

                this.unsplitBlockHelper(0);

                this.populatePixel();

                BufferedImage img2 = this.populate_image();

                // Show the images
                this.showImages(this.img1,img2,"sending "+ ctr + " coefficients");

                try {
                    Thread.sleep(latency);
                }catch (InterruptedException ex)
                {
                    System.out.println(ex.getMessage());
                }
                }
    }

    public void sequentialMode(){

        this.img2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int length  = matrix_R_split.size();

        for(int index = 0; index<length;index++){

            double[][] mat_R = this.inverseDCTTransform(matrix_R_split.get(index));
            double[][] mat_G = this.inverseDCTTransform(matrix_G_split.get(index));
            double[][] mat_B = this.inverseDCTTransform(matrix_B_split.get(index));

            // This populates the imgPixel block-wise
            this.populate_image(index,mat_R,mat_G,mat_B);

            // Show the images
            this.showImages(this.img1,this.img2,"sending packet "+ index);

            try {
                Thread.sleep(this.latency);
            }catch (InterruptedException ex)
            {
                System.out.println(ex.getMessage());
            }
        }

    }

    public static void main(String[] args) {

        String location=args[0];

        int quantization = Integer.parseInt(args[1]);
        int deliveryMode = Integer.parseInt(args[2]);
        int latency = Integer.parseInt(args[3]);


        if(quantization<0 || quantization>7){

            System.out.println("Invalid Quantization value");
        }
        else if( !Arrays.asList(1,2,3).contains(deliveryMode) )
        {
            System.out.println("Invalid Delivery mode");
        }
        else {


            ImageCompression ren = new ImageCompression(quantization, latency);

            // Reads the RGB file
            ren.readRGBFile(location);

            // Extract the RGB value for processing
            ren.extract_RGB();

            // Calculate the pixel value
            ren.populatePixel();

            // Populate the original image
            ren.img1 = ren.populate_image();

            // Process the image steps

            // Split the image 8*8 blocks
            ren.splitBlocksHelper();

            // DCT and Quantize the Image
            ren.dctTransformHelper();

            switch (deliveryMode) {

                case 1:
                    ren.sequentialMode();
                    break;
                case 2:
                    ren.spectralSelection();
                    break;
                case 3:
                    ren.successiveBitApprox();
                    break;
                default:
                    System.out.println("Not a valid option");
            }
        }

    }
}
