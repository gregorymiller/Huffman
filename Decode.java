package huffman;

import java.io.*;
import java.util.HashMap;

/**
 * Handles the binary input from the file
 */
class BinaryInput {
    private BufferedInputStream input;
    private int byteToRead;
    private int bitsRemaning;
    
    /**
     * Constructor
     * @param inputFile the input file
     */
    public BinaryInput(String inputFile) {
        File file = new File(inputFile);
        
        try {
            if (!file.exists()) {
                System.out.println("Input file does not exist");
                System.exit(0);
            }

            this.input = new BufferedInputStream(new FileInputStream(file));
            fillBits();
        }
        catch (IOException e) {
            System.out.println("Problem creating file to write to");
        } 
    }
    
    /**
     * Reads a single character from the file
     */
    private void fillBits() {
        try {
            this.byteToRead = this.input.read();
            this.bitsRemaning = 8;
        }
        catch (IOException e) {
            this.byteToRead = -1;
            this.bitsRemaning = -1;
        }
    }
    
    /**
     * Reads a single bit a time and if there are no more bits it fills again
     * @return true or false depending on the bit
     */
    public boolean readBit() {
        this.bitsRemaning--;
        
        boolean bit = ((this.byteToRead >> this.bitsRemaning) & 1) == 1;
        
        if (this.bitsRemaning == 0) {
            fillBits();
        }
        
        return bit;
    }
    
    
    /**
     * Reads a single character from the file
     * @return a character
     */
    public char readChar() {
        // If there is the whole character read it and fill the bits
        if (this.bitsRemaning == 8) {
            int x = this.byteToRead;
            fillBits();
            // Just get the last 8 bits of the 32 bit integer
            return (char) (x & 0xff);
        }
        
        // Otherwise read what is left in the byte
        int x = this.byteToRead;
        x <<= (8 - this.bitsRemaning);
        
        // Save how many bits remain so that we know how much to shift in the next byte
        int oldBits = this.bitsRemaning;
        // Get the next byte
        fillBits();
        this.bitsRemaning = oldBits;
        
        // Shift however many bits are needed to fill the byte
        x |= (this.byteToRead >>> this.bitsRemaning);
        return (char) (x & 0xff);
    }
    
    /**
     * Reads a single byte by reading a character and only keeping the last 8 bits
     * @return a byte
     */
    public byte readByte() {
        char c = readChar();
        return (byte) (c & 0xff);
    }
    
    /**
     * Closes the input
     */
    public void close() {
        try {
        this.input.close();
        }
        catch (IOException e) {
            System.out.println("Unable to close input");
        }
    }
    
    /**
     * @return returns if there is anything left to be read from the file
     */
    public boolean isEmpty() {
        return this.byteToRead == -1;
    }
}

/**
 * Builds the huffman codes and decodes the files
 */
class Huffman {
    private HashMap<String, Character> hashCodes = new HashMap<String, Character>();
    private int[] codeLengths = new int[256];
    private BinaryInput input;
    private int maxCodeLength = 0;
    private String EOF;
    
    /**
     * Constructor
     */
    public Huffman() {
        
    }
    
    /**
     * Gets the code lengths to build the canonical codes
     */
    public void buildTreeFromFile() {
        int numberOfCodes = this.input.readByte();
        
        // For the number of codes read the character and the code length
        for (int i = 0; i < numberOfCodes; i++) {
            if (!this.input.isEmpty()) {
                byte tempCode = this.input.readByte();
                byte tempLength = this.input.readByte();

                if (tempLength > this.maxCodeLength)
                    this.maxCodeLength = tempLength;

                this.codeLengths[tempCode] = tempLength;
            }
        }
    }
    
    /**
     * Builds the canonical codes
     */
    public void buildCanonicalTree() {
        int codeNumber = 0;
        int numberOfCodesAtALength = 0;
        int beginNumber = 0;
        
        // Start at the max code length
        for (int i = this.maxCodeLength; i >= 1; i--) {
            beginNumber = codeNumber;
            
            for (int j = 0; j < this.codeLengths.length; j++) {
                // If the character has that length build its code
                if (this.codeLengths[j] == i) {
                    // Increment the amount of codes at this length
                    numberOfCodesAtALength++;
                    // Get the binary of the current code number
                    String tempCode = Integer.toBinaryString(codeNumber);

                    // Add leading zereos to the number
                    if (tempCode.length() - 1 < i) {
                        for (int x = tempCode.length(); x < i; x++)
                            tempCode = "0" + tempCode;
                    }

                    // Get the correct code length for numbers with uneccesary leading zereos
                    tempCode = tempCode.substring(0, i);

                    this.hashCodes.put(tempCode, (char) j);
                    
                    // If it is the null character record the code
                    if (j == 0)
                        EOF = tempCode;

                    codeNumber++;
                }               
            }
            
            // After all the codes are processed at a length calculate the next number
            // to start making codes at by the beginning code number and the 
            // number of codes at a length then shift it
            // This maintains the prefix codes
            codeNumber = (beginNumber + numberOfCodesAtALength) >> 1;
            numberOfCodesAtALength = 0;
        }
    }
    
    /**
     * Reads in the file and outputs the correct characters
     * @param inputFile input file
     * @param outputFile outsput file
     */
    public void expandAndStore(String inputFile, String outputFile) {
        // Open the input the build the tree and build the codes
        this.input = new BinaryInput(inputFile);
        buildTreeFromFile();
        buildCanonicalTree();

        try {
            File output = new File(outputFile);

            String code = "";
            
            if (!output.exists())
                output.createNewFile();
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            
            // While there is still more to read
            // Read bits and add that to the code
            while(!this.input.isEmpty()) {
                boolean bit = this.input.readBit();
                
                if (bit)
                    code += "1";
                else
                    code += "0";
                
                // Attempt to match the code in the hashmap if it is write the code
                if (this.hashCodes.containsKey(code) && (!this.hashCodes.get(code).equals(this.hashCodes.get(this.EOF)))) {
                    writer.write(this.hashCodes.get(code));
                    code = "";
                }
            }
        
            
            this.input.close();
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Failed writing data to file");
        }
    }
}

/**
 * Processes the command line arguments and builds the output
 */
public class Decode {
    
    /**
     * Processes the command line arguments and builds the output
     * @param args command line arguments
     */
    public static void main(String args[]) {
        String inputFile = null;
        String outputFile = null;
        
        if (args.length > 0)  {
            inputFile = args[0];
            outputFile = args[1];
        }
        else {
            System.out.println("No commands given");
            
            System.exit(0);
        }
        
        Huffman codes = new Huffman();
        codes.expandAndStore(inputFile, outputFile);
    }    
}
