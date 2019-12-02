
/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: AlgorithmUtils.java
 * description: To aid in simulating a network (graph) of routers and the {Dijkstra, Bellman-Ford} algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * AlgorithmUtils contain some general utility methods for both Dijkstra and
 * Bellman-Ford
 */
public class AlgorithmUtils {
    private static Scanner fileScanner;

    /**
     * Printing press
     * 
     * @param toPrint string to print
     */
    public static void p(String toPrint) {
        System.out.println(toPrint);
    }

    /**
     * To update the adjacency matrix based on a single change given by the changes
     * file in the form of a Link
     * 
     * @param adjMat adjacency matrix to update
     * @param change change to integrate
     * @return updated adjacency matrix with change
     */
    public static int[][] applyChange(int[][] adjMat, Link change) {
        if (change.getCost() == -999) {
            adjMat[change.getSrc() - 1][change.getDest() - 1] = 0;
            adjMat[change.getDest() - 1][change.getSrc() - 1] = 0;

        } else {
            adjMat[change.getSrc() - 1][change.getDest() - 1] = change.getCost();
            adjMat[change.getDest() - 1][change.getSrc() - 1] = change.getCost();

        }
        return adjMat;
    }

    /**
     * To print the entire adjacency matrix
     * 
     * @param a
     */
    public static void printAdjacencyMatrix(int[][] a) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                System.out.print(a[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * To print upper half of adjacency matrix, because the matrix is symmetrical,
     * because we are assuming no directed graphs
     * 
     * @param a adjacency matrix to print
     */
    public static void printUpperHalfOfAdjacencyMatrix(int[][] a) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                if (j >= i) {
                    System.out.print(a[i][j] + " ");
                } else {
                    System.out.print("  ");
                }
            }
            System.out.println();
        }
    }

    /**
     * To get the adjacency matrix of the tree given by Dijkstra
     * 
     * @param l tree given by Dijkstra in proprietary list of Links
     * @return adjacency matrix
     */
    public static int[][] getNPrimeAdjacencyMatrix(List<Link> l) {
        // Assume input file is valid
        int numberOfNodes = getNumberOfNodes(l);
        int[][] ret = new int[numberOfNodes][numberOfNodes];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret.length; j++) {
                ret[i][j] = 0;
                ret[j][i] = 0;
            }
        }
        for (Link link : l) {
            ret[link.getSrc()][link.getDest()] = link.getCost();
            ret[link.getDest()][link.getSrc()] = link.getCost();

        }
        return ret;
    }

    /**
     * to get the adjacency matrix of the network given by proprietary list of Links
     * 
     * @param l list of Links which represent a network
     * @return adjacency matrix
     */
    public static int[][] getAdjacencyMatrix(List<Link> l) {
        // Assuming file input is valid
        int numberOfNodes = getNumberOfNodes(l);
        int[][] ret = new int[numberOfNodes][numberOfNodes];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret.length; j++) {
                ret[i][j] = 0;
                ret[j][i] = 0;
            }
        }
        for (Link link : l) {
            ret[link.getSrc() - 1][link.getDest() - 1] = link.getCost();
            ret[link.getDest() - 1][link.getSrc() - 1] = link.getCost();

        }
        return ret;
    }

    /**
     * to get number of discrete nodes in a network given by proprietary list of
     * Links
     * 
     * @param l list of Links which represent a network
     * @return number of discrete nodes (routers)
     */
    public static int getNumberOfNodes(List<Link> l) {
        List<Integer> discreteNodes = new ArrayList<Integer>();
        for (Link link : l) {
            if (!discreteNodes.contains(link.getSrc())) {
                discreteNodes.add(link.getSrc());
            }
            if (!discreteNodes.contains(link.getDest())) {
                discreteNodes.add(link.getDest());
            }
        }
        return discreteNodes.size();
    }

    /**
     * To get a network topology based upon the format outlined in the header
     * 
     * @param fileName the name of the topology file
     * @return the list of links got
     */
    public static List<Link> getTopology(String fileName) {
        // System.out.println("lsrouter::getTopology");
        List<Link> ret = new ArrayList<Link>();
        try {
            fileScanner = new Scanner(new File(fileName));
            while (fileScanner.hasNextLine()) {
                String[] line = fileScanner.nextLine().split(" ");
                ret.add(new Link(Integer.parseInt(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2])));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * To get messages into a list from message file provided
     * 
     * @param fileName the name of the message file
     * @return the list of messages got
     */
    public static List<Message> getMessages(String fileName) {
        List<Message> ret = new ArrayList<Message>();
        try {
            fileScanner = new Scanner(new File(fileName));
            while (fileScanner.hasNextLine()) {
                String fileLine = fileScanner.nextLine();
                int index = fileLine.indexOf(" ");
                int src = Integer.parseInt(fileLine.substring(0, index));
                fileLine = fileLine.substring(index + 1);
                index = fileLine.indexOf(" ");
                int dest = Integer.parseInt(fileLine.substring(0, index));
                String msg = fileLine.substring(index + 1);
                ret.add(new Message(src, dest, msg));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }
}