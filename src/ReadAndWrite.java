import java.io.*;

public class ReadAndWrite {
    public ReadAndWrite() {

    }
    public int[] txtToArray(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int length = Integer.parseInt(reader.readLine().trim());
        int[] intArray = new int[length];
        for (int i = 0; i < length; i++) {
            intArray[i] = Integer.parseInt(reader.readLine().trim());}
        reader.close();
        return intArray;
    }
    public void arrayToTxt(int[] array, String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(Integer.toString(array.length/3));
        writer.newLine();

        for (int i = 0; i < array.length; i+=3) {
            writer.write(Integer.toString(array[i]));
            writer.write(" ");
            writer.write(Integer.toString(array[i+1]));
            writer.write(" ");
            writer.write(Integer.toString(array[i+2]));
            if(i+2 != array.length-1) {
                writer.newLine();
            }
        }
        writer.close();
    }
}
