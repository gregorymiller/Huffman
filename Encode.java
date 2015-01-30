package huffman;

import java.io.*;
import java.util.PriorityQueue;

/**
 * Handles the binary output to a file
 * @author Greg Miller
 */
class BinaryOutput {
    private BufferedOutputStream output;
    private int byteToWrite;
    private int bitsRemaining;
    
    /**
     * Constructor that creates a buffered output stream
     * @param outputFile file to be written to
     */
    public BinaryOutput(String outputFile) {
        File file = new File(outputFile);
        
        // If the file does not exist creat it
        try {
            if (!file.exists())
                file.createNewFile();

            this.output = new BufferedOutputStream(new FileOutputStream(file));
        }
        catch (IOException e) {
            System.out.println("Problem creating file to write to");
        }
    }
    
    /**
     * Adds a 0 or 1 to the byte to be written and if the byte to be
     * written is full it is written
     * @param bit a true or false value that determines whether 0 or 1 is written 
     */
    public void writeBit(boolean bit) {
        this.byteToWrite <<= 1;
        
        if (bit) {
            this.byteToWrite |= 1;
        }
        
        this.bitsRemaining++;
        
        if (this.bitsRemaining == 8) {
            clearBits();
        }
    }
    
    /**
     * Writes the low 8 bits of an integer
     * @param byteToBeWritten integer to be written
     */
    public void writeByte(int byteToBeWritten) {
        // If there is nothing in the buffer just write the 8 bits
        if (this.bitsRemaining == 0) {
            try {
                this.output.write(byteToBeWritten);
            }
            catch (IOException e) {
                System.out.println("Problem writing bits");
            }
        }
        // Else write the 8 bits out one at a time
        else {
            for (int i = 0; i < 8; i++) {
                if (((byteToBeWritten >>> (8 - i - 1)) & 1) == 1) {
                    writeBit(true);
                }
                else {
                    writeBit(false);
                }
            }
        }
    }
    
    /**
     * Writes the remaining bits in the byte to be written to the file
     */
    private void clearBits() {
        if (this.bitsRemaining > 0) {
            this.byteToWrite <<= (8 - this.bitsRemaining);
            
            try {
                this.output.write(this.byteToWrite);
            }
            catch (IOException e) {
                System.out.println("Problem writing bits");
            }
            
            this.bitsRemaining = 0;
            this.byteToWrite = 0;
        }
    }
    
    /**
     * Closes the output file by writing the remaining bits then flushing and
     * closing the buffer
     */
    public void close() {
        clearBits();
        
        try {
            this.output.flush();
            this.output.close();
        }
        catch (IOException e) {
            System.out.println("Problem closing the output");
        }
    }
}


/**
 * Builds the huffman tree
 * @author Greg Miller
 */
class HuffmanTree implements Comparable<HuffmanTree> {
    private int frequency;
    private HuffmanTree leftChild, rightChild;
    private char character;
    
    /**
     * Constructor
     * @param frequency frequency of the character or node
     * @param character character in the file or null
     * @param left the left child
     * @param right the right child
     */
    public HuffmanTree(int frequency, char character, HuffmanTree left, HuffmanTree right) {
        this.frequency = frequency;
        this.character = character;
        this.leftChild = left;
        this.rightChild = right;
    }
    
    /**
     * Compares the frequency of two huffman trees so that a priority queue can
     * be built
     * @param hTree the huffman tree to be compared to
     * @return the difference in frequencies between the two tress
     */
    public int compareTo(HuffmanTree hTree) {
        return this.frequency - hTree.getFrequency();
    }
    
    /**
     * @return gets the frequency of a tree
     */
    public int getFrequency() {
        return this.frequency;
    }
       
    /**
     * @return if the current tree is a leaf or not
     */
    public boolean isLeaf() {
        return (this.leftChild == null && this.rightChild == null);
    }
    
    /**
     * @return the character of the tree
     */
    public char getCharacter() {
        return this.character;
    }
        
    /**
     * @return the left child of the tree
     */
    public HuffmanTree getLeftChild() {
        return this.leftChild;
    }
   
    /**
     * @return the right child of the tree
     */
    public HuffmanTree getRightChild() {
        return this.rightChild;
    }
}

/**
 * Builds the huffman codes and encodes the file
 * @author Greg Miller
 */
class HuffmanCode {
    private HuffmanTree tree;
    private HuffmanTree canonicalTree;
    private int codeLengths[] = new int[256];
    private int maxCodeLength = 0;
    private int numberOfSymbols = 0;
    private String codes[] = new String[256];
    private BinaryOutput output;
    
    /**
     * Constructor
     */
    public HuffmanCode() {

    }
    
    /**
     * Builds the huffman tree
     * @param frequencies the frequency of all the characters in the file
     */
    public void buildTree(int[] frequencies) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();
        
        // Put all of the characters in the priority queue
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] != 0)
            {
                trees.offer(new HuffmanTree(frequencies[i], (char) i, null, null));
                this.numberOfSymbols++;
            }
        }
        
        // Until there is only one tree combine trees to build the entire tree
        while (trees.size() > 1) {
            HuffmanTree left = trees.poll();
            HuffmanTree right = trees.poll();
            
            int tempFrequency = left.getFrequency() + right.getFrequency();
            
            trees.offer(new HuffmanTree(tempFrequency, '\0', left, right));
        }
        
        this.tree = trees.poll();
    }
    
    /**
     * Builds the canonical values to encode the file
     */
    public void buildCanonicalTree() {        
        // Get the code lengths
        buildCodeLengths(this.tree, 0);
        
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

                    this.codes[j] = tempCode;

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
     * Finds the depth of each leaf
     * @param tree the tree to find the code lengths of
     * @param depth the depth of the current tree node
     */
    public void buildCodeLengths(HuffmanTree tree, int depth) {
        // If it is a leaf save its depth
        // Else traverse the tree
        if (tree.isLeaf())
        {
            this.codeLengths[(int) tree.getCharacter()] = depth;
            
            if (depth > this.maxCodeLength) {
                this.maxCodeLength = depth;
            }
        }
        else
        {
            buildCodeLengths(tree.getLeftChild(), depth + 1);
            buildCodeLengths(tree.getRightChild(), depth + 1);
        }
    }
    
    /**
     * Stores the huffman tree in the file
     */
    public void storeHuffmanTree() {      
        // Writes the number of characters in the file
        this.output.writeByte(this.numberOfSymbols);

        // Writes the code the then length
        for (int i = 0; i < this.codes.length; i++) {
            if ((this.codes[i] != null))
            {
                this.output.writeByte(i);
                this.output.writeByte(this.codes[i].length());                                
            }
        }
    }
    
    /**
     * Stores the compressed information in the output file
     * @param frequencies the frequencies of characters in the file
     * @param inputFile input file to be compressed
     * @param outputFile file to be written to
     */
    public void compressAndStore(int[] frequencies, String inputFile, String outputFile){
        // Build the tree and the canonical codes
        buildTree(frequencies);     
        buildCanonicalTree();
        
        // Open the output and store the tree
        this.output = new BinaryOutput(outputFile);
        storeHuffmanTree();        
        
        try {
            File input = new File(inputFile);
            
            int currentLetter;

            BufferedReader reader = new BufferedReader(new FileReader(input));
            
            // For each character in the file write its huffman code
            while ((currentLetter = reader.read()) != -1) {
                String code = this.codes[currentLetter];

                for (char binary : code.toCharArray()) {
                    if (binary == '0')
                        this.output.writeBit(false);
                    else
                        this.output.writeBit(true);
                }
            }
            
            reader.close();
        }
        catch (IOException e) {
            System.out.println("Failed writing data to file");
        }
        
        // Write the end of file huffman code to the file
        String EOF = this.codes[0];
        
        for (char binary : EOF.toCharArray()) {
            if (binary == '0')
                this.output.writeBit(false);
            else
                this.output.writeBit(true);
        }
        
        this.output.close();
    }
}

/**
 * Processes input from the command line and gets the character frequencies
 * @author Greg Miller
 */
public class Encode {
    
    /**
     * Gets the command line arguments the makes the call to compress the file
     * @param args command line arguments
     */
   public static void main(String args[]) {
        String inputFile = null;
        String outputFile = null;
        int frequencies[] = new int[256];
        
        // Add the end of file
        frequencies[0]++;
        
        // Get the input and output file name
        if (args.length > 0)  {
            inputFile = args[0];
            outputFile = args[1];
        }
        else {
            System.out.println("No commands given");
            
            System.exit(0);
        }
        
        getCharacterFrequencies(inputFile, frequencies);
        
        // Compress the file
        HuffmanCode code = new HuffmanCode();
        code.compressAndStore(frequencies, inputFile, outputFile);
    } 
    
   /**
    * Gets the frequency of the characters
    * @param input the input file
    * @param frequencies frequency array
    */
    public static void getCharacterFrequencies(String input, int[] frequencies) {
        File f = new File(input);
        
        if (!f.exists()) {
            System.out.println("Input file does not exist");
            
            System.exit(0);
        }
 
        try {

            int currentLetter;

            BufferedReader reader = new BufferedReader(new FileReader(input));

            // Add the frequency for each letter
            while ((currentLetter = reader.read()) != -1) {                        
                frequencies[currentLetter]++;
            }
                        
            reader.close();

        } catch (IOException e) {
                System.out.println("Problem reading the file");
        }
    }   
}
