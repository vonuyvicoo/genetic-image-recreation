public class PointPixel {
    private int index;
    private int brightness;
    private int fitness;

    public PointPixel(int index, int brightness, int fitness){
        this.index = index;
        this.fitness = fitness;
        this.brightness = brightness;
    }

    public int getIndex(){
        return index;
    }

    public int getBrightness(){
        return brightness;
    }

    public int getFitness(){
        return fitness;
    }

}
