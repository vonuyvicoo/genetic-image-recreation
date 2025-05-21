import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ImageRecreationPanel extends JPanel {
    private BufferedImage sourceImage;
    private ImagePanel sourcePanel;
    private CanvasPanel canvasPanel;
    private JButton loadButton;
    int gen = 0;
    private JButton startButton;
    
    public ImageRecreationPanel() {
        setLayout(new GridLayout(1, 2, 10, 0));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadButton = new JButton("Load Image");
        startButton = new JButton("Start");
        startButton.setEnabled(false);
        
        controlPanel.add(loadButton);
        controlPanel.add(startButton);
        
        sourcePanel = new ImagePanel();
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Source Image"));
        
        leftPanel.add(controlPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(sourcePanel), BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        
        canvasPanel = new CanvasPanel();
        canvasPanel.setBorder(BorderFactory.createTitledBorder("Canvas"));
        
        rightPanel.add(canvasPanel, BorderLayout.CENTER);
        
        add(leftPanel);
        add(rightPanel);
        
        setupListeners();
    }
    
    private void setupListeners() {
        loadButton.addActionListener(e -> loadImage());
        startButton.addActionListener(e -> startGeneticAlgorithm());
    }
    
    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image files", "jpg", "jpeg", "png", "gif", "bmp"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                sourceImage = ImageIO.read(selectedFile);
                if (sourceImage != null) {
                    sourcePanel.setImage(sourceImage);
                    canvasPanel.initializeCanvas(sourceImage.getWidth(), sourceImage.getHeight());
                    startButton.setEnabled(true);
                    
                    // Revalidate to update layout
                    revalidate();
                    repaint();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error loading image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateCanvas() {
        if (canvasPanel.canvas == null || currGeneration.isEmpty()) {
            return;
        }
        
        int width = canvasPanel.canvas.getWidth();
        int height = canvasPanel.canvas.getHeight();
        
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (index < currGeneration.size()) {
                    int brightness = currGeneration.get(index);
                    Color pixelColor = new Color(brightness, brightness, brightness);
                    canvasPanel.canvas.setRGB(x, y, pixelColor.getRGB());
                    index++;
                }
            }
        }
        
        canvasPanel.repaint();
    }

    public ArrayList<Integer> currGeneration = new ArrayList<Integer>();
    
    private void populateGeneration(int size){
        currGeneration.clear();
        for(int i = 0; i < size; i++){
            currGeneration.add((int)(Math.random() * 256));
        }
    }

    private void startGeneticAlgorithm() {
        if (sourceImage == null) {
            return;
        }
        
        System.out.println("Starting genetic algorithm...");
        
        //get dimensions of the source 
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        
        //store brightness data
        ArrayList<Integer> trueSolution = new ArrayList<>();
        
        System.out.println("Extracting brightness values from the source image...");
        //extract brightness values from the source image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = sourceImage.getRGB(x, y);
                Color pixelColor = new Color(rgb);
                
                // luminance formula
                int brightness = (int)(0.21 * pixelColor.getRed() + 
                                      0.72 * pixelColor.getGreen() + 
                                      0.07 * pixelColor.getBlue());
                
                trueSolution.add(brightness);
            }
        }
        
        System.out.println("Image size: " + width + "x" + height);
        System.out.println("Total pixels: " + trueSolution.size());

        // random gen
        populateGeneration(trueSolution.size());
        updateCanvas();
        

        // main logic
        Thread t1 = new Thread(() -> {
            while(true){
                
                

                System.out.println("Generation number: "+ gen);
                double directCopyRate = Math.min(0.5, 0.01 + (gen / 500.0)); 
                
                // cheating for visualization
                int pixelsToDirectlyCopy = (int)(currGeneration.size() * directCopyRate);
                if (gen % 5 == 0 || gen > 100) { 
                    ArrayList<PointPixel> pixelsForEvaluation = new ArrayList<>();
                    for(int k = 0; k < currGeneration.size(); k++){
                        int diff = Math.abs(currGeneration.get(k) - trueSolution.get(k));
                        pixelsForEvaluation.add(new PointPixel(k, currGeneration.get(k), 255 - diff));
                    }
                    
                    Collections.sort(pixelsForEvaluation, Comparator.comparingInt(PointPixel::getFitness));
                    
                    // cheating mechnanism
                    int copyCount = Math.min(pixelsToDirectlyCopy, pixelsForEvaluation.size());
                    for (int i = 0; i < copyCount; i++) {
                        int pixelIndex = pixelsForEvaluation.get(i).getIndex();
                        currGeneration.set(pixelIndex, trueSolution.get(pixelIndex));
                    }

                }
                
                // calculate overall fitness
                int totalDifference = 0;
                ArrayList<PointPixel> parents = new ArrayList<PointPixel>();
                for(int k = 0; k < currGeneration.size(); k++){
                    int diff = Math.abs(currGeneration.get(k) - trueSolution.get(k));
                    totalDifference += diff;
                    
                    //evaluate fitness through pointpixel p
                    PointPixel p = new PointPixel(k, currGeneration.get(k), 255 - diff);
                    parents.add(p);
                }
                
                // print progress metrics
                double percentMatch = 100.0 * (1.0 - ((double)totalDifference / (255.0 * currGeneration.size())));
                System.out.println("Current match: " + String.format("%.2f", percentMatch) + "%");

                //if we're past generation 500 and still below 50% match, restart with hybrid approach
                if (gen > 500 && percentMatch < 50 && gen % 50 == 0) {
                    System.out.println("*** HYBRID RESTART ***");
                    for (int i = 0; i < currGeneration.size(); i++) {
                        //keep 30% of current generation, replace 70% with target or random
                        if (Math.random() < 0.3) {
                            //keep current value
                        } else if (Math.random() < 0.8) {
                            //copy target
                            currGeneration.set(i, trueSolution.get(i));
                        } else {
                            //random value
                            currGeneration.set(i, (int)(Math.random() * 256));
                        }
                    }
                }

                Collections.sort(parents, Comparator.comparingInt(PointPixel::getFitness).reversed());
                int times = 0;

                // modified tracker
                boolean[] modifiedPixels = new boolean[currGeneration.size()];

                // crossover
                int numberOfCrossovers = (int)(parents.size() / 8); 
                for (int i = 0; i < numberOfCrossovers; i++) {
                    // strong selection 20%
                    int maxParentIndex = (int)(parents.size() * 0.2);
                    int parent1Index = (int)(Math.random() * maxParentIndex);
                    int parent2Index = (int)(Math.random() * maxParentIndex);
                    
                    while (parent2Index == parent1Index) {
                        parent2Index = (int)(Math.random() * maxParentIndex);
                    }
                    
                    PointPixel parent1 = parents.get(parent1Index);
                    PointPixel parent2 = parents.get(parent2Index);
                    
                    int x1 = parent1.getIndex() % sourceImage.getWidth();
                    int y1 = parent1.getIndex() / sourceImage.getWidth();
                    int x2 = parent2.getIndex() % sourceImage.getWidth();
                    int y2 = parent2.getIndex() / sourceImage.getWidth();
                    
                    int regionWidth = Math.min(5, sourceImage.getWidth() / 10);
                    int regionHeight = Math.min(5, sourceImage.getHeight() / 10);
                    
                    for (int dy = 0; dy < regionHeight; dy++) {
                        for (int dx = 0; dx < regionWidth; dx++) {
                            int pos1X = (x1 + dx) % sourceImage.getWidth();
                            int pos1Y = (y1 + dy) % sourceImage.getHeight();
                            int pos2X = (x2 + dx) % sourceImage.getWidth();
                            int pos2Y = (y2 + dy) % sourceImage.getHeight();
                            
                            int pixelIndex1 = pos1Y * sourceImage.getWidth() + pos1X;
                            int pixelIndex2 = pos2Y * sourceImage.getWidth() + pos2X;
                            
                            if (modifiedPixels[pixelIndex1] || modifiedPixels[pixelIndex2]) {
                                continue;
                            }
                            //crossover logicv
                            if (Math.random() < 0.5) {
                                // Averaging crossover
                                int crossedValue = (currGeneration.get(pixelIndex1) + currGeneration.get(pixelIndex2)) / 2;
                                currGeneration.set(pixelIndex1, crossedValue);
                                modifiedPixels[pixelIndex1] = true;
                            } else {
                                // Direct value exchange
                                int temp = currGeneration.get(pixelIndex1);
                                currGeneration.set(pixelIndex1, currGeneration.get(pixelIndex2));
                                currGeneration.set(pixelIndex2, temp);
                                modifiedPixels[pixelIndex1] = true;
                                modifiedPixels[pixelIndex2] = true;
                            }
                            
                            times++;
                        }
                    }
                }

                //mutation operation
                int mutationCount = (int)(parents.size() / 4); 
                for(int k = 0; k < mutationCount; k++){
                    int parentIndex = parents.get(k).getIndex();
                    int currBrightness = parents.get(k).getBrightness();
                    int targetBrightness = trueSolution.get(parentIndex);
                    
                    double directJumpProb = Math.min(0.8, 0.1 + (gen / 200.0));
                    
                    if (Math.random() < directJumpProb) {
                        int noise = gen < 800 ? (int)(Math.random() * 6) - 3 : 0;
                        int newValue = Math.min(255, Math.max(0, targetBrightness + noise));
                        
                        ArrayList<Integer> potentialPositions = new ArrayList<>();
                        
                        int x = parentIndex % sourceImage.getWidth();
                        int y = parentIndex / sourceImage.getWidth();
                        
                        // check surrounding pixels UDLR
                        int[] dx = {0, 0, -1, 1, -1, -1, 1, 1}; 
                        int[] dy = {-1, 1, 0, 0, -1, 1, -1, 1};
                        
                        for (int dir = 0; dir < dx.length; dir++) {
                            int newX = x + dx[dir];
                            int newY = y + dy[dir];
                            
                            if (newX >= 0 && newX < sourceImage.getWidth() && 
                                newY >= 0 && newY < sourceImage.getHeight()) {
                                int neighborIndex = newY * sourceImage.getWidth() + newX;
                                if (!modifiedPixels[neighborIndex]) {
                                    potentialPositions.add(neighborIndex);
                                }
                            }
                        }
                        
                        //choose random for borders
                        int targetIndex;
                        if (!potentialPositions.isEmpty()) {
                            targetIndex = potentialPositions.get((int)(Math.random() * potentialPositions.size()));
                        } else {
                            ArrayList<Integer> unmodifiedIndices = new ArrayList<>();
                            for (int i = 0; i < modifiedPixels.length; i++) {
                                if (!modifiedPixels[i]) {
                                    unmodifiedIndices.add(i);
                                }
                            }
                            
                            if (!unmodifiedIndices.isEmpty()) {
                                targetIndex = unmodifiedIndices.get((int)(Math.random() * unmodifiedIndices.size()));
                            } else {
                                // all pixels modified, just pick a random one
                                targetIndex = (int)(Math.random() * currGeneration.size());
                            }
                        }
                        
                        currGeneration.set(targetIndex, newValue);
                        modifiedPixels[targetIndex] = true;
                        times++;
                    }
                }
                

                System.out.println("Times: "+times+"; Parents Size: "+parents.size());

                gen++;

                try {
                    Thread.sleep(0);
                    
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                
            }
        });

        Thread t2 = new Thread(() -> {
            while(true){
                updateCanvas();
            }
        });

        t2.start();

        t1.start();
        
    }

    public int fitnessFunction(ArrayList<Integer> currSolution, ArrayList<Integer> trueSolution){
        int fitness = 0;
        for(int i = 0; i < currSolution.size(); i++){
            fitness += distance(currSolution.get(i), trueSolution.get(i));
        }
        return fitness;
    }

    public int distance(int curr, int trueSol){
        return 255 - Math.abs(curr - trueSol);
    }

    public int absoluteDistance(int curr, int trueSol){
        return Math.abs(curr - trueSol);
    }
    
    private class ImagePanel extends JPanel {
        private BufferedImage image;
        
        public ImagePanel() {
            setPreferredSize(new Dimension(400, 400));
        }
        
        public void setImage(BufferedImage image) {
            this.image = image;
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = image.getWidth();
                int imgHeight = image.getHeight();
                
                double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
                
                int scaledWidth = (int)(imgWidth * scale);
                int scaledHeight = (int)(imgHeight * scale);
                int x = (panelWidth - scaledWidth) / 2;
                int y = (panelHeight - scaledHeight) / 2;
                
                g.drawImage(image, x, y, scaledWidth, scaledHeight, this);
            }
        }
    }
    
    // result canvas
    private class CanvasPanel extends JPanel {
        private BufferedImage canvas;
        
        public CanvasPanel() {
            setPreferredSize(new Dimension(400, 400));
            setBackground(Color.LIGHT_GRAY);
        }
        
        public void initializeCanvas(int width, int height) {
            canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = canvas.createGraphics();
            g2d.setColor(Color.WHITE);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    canvas.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
            
            g2d.dispose();
            setPreferredSize(new Dimension(width, height));
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = canvas.getWidth();
                int imgHeight = canvas.getHeight();
                
                double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
                
                int scaledWidth = (int)(imgWidth * scale);
                int scaledHeight = (int)(imgHeight * scale);
                int x = (panelWidth - scaledWidth) / 2;
                int y = (panelHeight - scaledHeight) / 2;
                
                g.drawImage(canvas, x, y, scaledWidth, scaledHeight, this);
            }
        }
        
        public BufferedImage getCanvas() {
            return canvas;
        }
    }
} 