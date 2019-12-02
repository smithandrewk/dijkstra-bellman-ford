
/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: dvrouter.java
 * description: To simulate a network (graph) of routers and the Bellman-Ford algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class utilizes the distance-vector algorithm, also known as the
 * Bellman-Ford algorithm, to produce a forwarding table for a network. This
 * class also simulates the sending of messages, along with the changing of link
 * costs and how that may effect message path.
 */
public class dvrouter {
    private static final String BREAK = "------------";
    private static PrintWriter printWriter;
    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) throws FileNotFoundException {
        // check args
        if (!(args.length == 3)) {
            System.out.println("Usage: java dvrouter <topofile> <changesfile> <messagefile>");
            System.exit(0);
        }
        // init PrintWriter to write to file
        printWriter = new PrintWriter(new FileOutputStream(OUTPUT_FILE));
        // get files
        List<Link> initialTopology = AlgorithmUtils.getTopology(args[0]);
        List<Link> changes = AlgorithmUtils.getTopology(args[1]);
        List<Message> messages = AlgorithmUtils.getMessages(args[2]);
        // initial topology in adjacency matrix format
        int[][] adjacencyMatrix = AlgorithmUtils.getAdjacencyMatrix(initialTopology);
        // prettify file
        printWriter.println(BREAK + "Output Before Changes" + BREAK);
        // Control iteration of forwarding tables and message-simulating for each router
        printToFile(adjacencyMatrix, messages);
        // An iteration of forwarding tables and message-simulating for each router for
        // each change in the changes file
        for (Link change : changes) {
            // prettify file
            printWriter.println(BREAK + "Output After Change: " + change.getSrc() + " " + change.getDest() + " "
                    + change.getCost() + BREAK);
            // network topology after applying this change
            adjacencyMatrix = AlgorithmUtils.applyChange(adjacencyMatrix, change);
            // actual iteration
            printToFile(adjacencyMatrix, messages);
        }
        // close stream
        printWriter.close();
    }

    /**
     * Bellman-Ford. Unlike Dijkstra, this algorithm has no source.
     * 
     * @param adjacencyMatrix adjacency matrix of network to run algorithm on
     * @return proprietary format of distance vectors for each node in the network
     */
    public static Map<Integer, List<Distance>> distanceVector(int[][] adjacencyMatrix) {

        // get initial distance vectors
        Map<Integer, List<Distance>> distanceVectors = new HashMap<Integer, List<Distance>>();
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            List<Distance> vector = new ArrayList<Distance>();
            for (int j = 0; j < adjacencyMatrix.length; j++) {
                if (j == i) {
                    vector.add(new Distance(i, 0));
                } else if (adjacencyMatrix[i][j] == 0) {
                    vector.add(new Distance(i, -1));
                } else {
                    vector.add(new Distance(j, adjacencyMatrix[i][j]));
                }
            }

            distanceVectors.put(i, vector);
        }

        boolean change = true;
        while (change) {
            change = false;
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                List<Distance> newVector = new ArrayList<Distance>();
                // get adjacent vectors
                List<Integer> adjacentVectors = new ArrayList<Integer>();
                for (int j = 0; j < adjacencyMatrix.length; j++) {
                    if (adjacencyMatrix[i][j] > 0) {
                        adjacentVectors.add(j);
                    }
                }
                for (int j = 0; j < distanceVectors.get(i).size(); j++) {
                    int min = distanceVectors.get(i).get(j).getCost();
                    int through = distanceVectors.get(i).get(j).getThrough();
                    for (Integer integer : adjacentVectors) {

                        if (distanceVectors.get(integer).get(j).getCost() == -1) {
                            // cant reach from this vector
                            continue;
                        } else if (min == -1) {
                            change = true;
                            min = distanceVectors.get(integer).get(j).getCost()
                                    + distanceVectors.get(i).get(integer).getCost();
                            Distance temp = distanceVectors.get(integer).get(i);
                            int last = integer;
                            while (!(i == temp.getThrough())) {
                                last = temp.getThrough();
                                temp = distanceVectors.get(temp.getThrough()).get(i);
                            }
                            through = last;

                        } else if ((distanceVectors.get(integer).get(j).getCost()
                                + distanceVectors.get(i).get(integer).getCost()) < min) {
                            change = true;
                            min = distanceVectors.get(integer).get(j).getCost()
                                    + distanceVectors.get(i).get(integer).getCost();
                            Distance temp = distanceVectors.get(integer).get(i);
                            int last = integer;

                            while (!(i == temp.getThrough())) {
                                last = temp.getThrough();
                                temp = distanceVectors.get(temp.getThrough()).get(i);

                            }

                            through = last;

                        }
                    }
                    newVector.add(new Distance(through, min));
                }
                // each iteration here
                distanceVectors.put(i, newVector);
            }
        }

        // convergence here
        return distanceVectors;
        // now print messages, apply change, run again

    }

    /**
     * Run's least-cost algorithm and handles file printing format
     * 
     * @param adjacencyMatrix adjacency matrix to run algorithm on
     * @param messages        messages to simulate path with
     */
    public static void printToFile(int[][] adjacencyMatrix, List<Message> messages) {

        Map<Integer, List<Distance>> dv = distanceVector(adjacencyMatrix);
        // each table
        for (Map.Entry<Integer, List<Distance>> entry : dv.entrySet()) {
            printWriter.println("Forwarding Table for Router " + (entry.getKey() + 1));
            // table
            String table = "";
            for (int i = 0; i < entry.getValue().size(); i++) {
                table = table + (i + 1) + " " + (entry.getValue().get(i).getThrough() + 1) + " "
                        + entry.getValue().get(i).getCost() + "\n";
            }
            printWriter.println(table);
        }
        printWriter.println(BREAK + "Messages" + BREAK);
        for (Message message : messages) {
            printWriter.println(getMessageEntry(dv, dv.get(message.getSrc() - 1), message));
        }
    }

    /**
     * gets path for message and formats into string to be output
     * 
     * @param distanceVectors        Map of distance vectors
     * @param distanceVectorOfSource distance vector of source (could be replaced by
     *                               source key, obviously)
     * @param msg                    message to send
     * @return formatted output string for file
     */
    public static String getMessageEntry(Map<Integer, List<Distance>> distanceVectors,
            List<Distance> distanceVectorOfSource, Message msg) {
        String ret = "";
        List<Integer> hops = new ArrayList<Integer>();
        hops.add(msg.getSrc());
        Distance temp = distanceVectorOfSource.get(msg.getDest() - 1);
        while (!(temp.getThrough() == (msg.getDest() - 1))) {
            hops.add(temp.getThrough() + 1);
            temp = distanceVectors.get(temp.getThrough()).get(msg.getDest() - 1);

        }
        hops.add(msg.getDest());

        ret = ret + "from <" + msg.getSrc() + "> to <" + msg.getDest() + ">: hops";
        for (int i = 0; i < hops.size(); i++) {
            ret = ret + " <" + (hops.get(i)) + ">";
        }
        ret = ret + "; message: <" + msg.getMsg() + ">\n";
        return ret;
    }

    /**
     * To pretty-print distance vectors
     * 
     * @param distanceVectors distance vectors to pretty-print
     */
    public static void printDistanceVectors(Map<Integer, List<Distance>> distanceVectors) {
        // print
        for (Map.Entry<Integer, List<Distance>> entry : distanceVectors.entrySet()) {
            System.out.print("< ");
            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.print(
                        "( " + entry.getValue().get(i).getCost() + ", " + entry.getValue().get(i).getThrough() + " ) ");
            }
            AlgorithmUtils.p(">");
        }
    }
}