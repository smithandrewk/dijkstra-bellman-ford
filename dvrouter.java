
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
        // To get offset (see AlgorithmUtils for explanation)
        int offset = AlgorithmUtils.getOffset(initialTopology);
        // get changes file
        List<Link> changes = AlgorithmUtils.getTopology(args[1]);
        // apply offset
        for (Link link : changes) {
            link.setSrc(link.getSrc() + (1 - offset));
            link.setDest(link.getDest() + (1 - offset));
        }
        // get messages file
        List<Message> messages = AlgorithmUtils.getMessages(args[2]);
        // apply offset
        for (Message message : messages) {
            message.setSrc(message.getSrc() + (1 - offset));
            message.setDest(message.getDest() + (1 - offset));
        }
        // initial topology in adjacency matrix format
        int[][] adjacencyMatrix = AlgorithmUtils.getAdjacencyMatrix(initialTopology, offset);
        // Control iteration of forwarding tables and message-simulating for each router
        printToFile(adjacencyMatrix, messages, offset);
        // An iteration of forwarding tables and message-simulating for each router for
        // each change in the changes file
        for (Link change : changes) {
            // network topology after applying this change
            adjacencyMatrix = AlgorithmUtils.applyChange(adjacencyMatrix, change);
            // actual iteration
            printToFile(adjacencyMatrix, messages, offset);
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
        Map<Integer, List<Distance>> distanceVectors = new HashMap<Integer, List<Distance>>();
        Map<Integer, List<Distance>> initialDistanceVectors = new HashMap<Integer, List<Distance>>();
        // to get initial distance vectors
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
            initialDistanceVectors.put(i, vector);
        }
        boolean change = true;
        // interate until convergence
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
                            // found less expensive route
                        } else if ((distanceVectors.get(integer).get(j).getCost()
                                + distanceVectors.get(i).get(integer).getCost()) < min) {
                            // update
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
                            // Tie break condition
                        } else if ((distanceVectors.get(integer).get(j).getCost()
                                + initialDistanceVectors.get(i).get(integer).getCost()) == min) {
                            // need next hop of current path compared to integer
                            if (integer < through) {
                                change = true;
                                through = integer;
                            }
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
    public static void printToFile(int[][] adjacencyMatrix, List<Message> messages, int offset) {

        Map<Integer, List<Distance>> dv = distanceVector(adjacencyMatrix);
        // each table
        for (Map.Entry<Integer, List<Distance>> entry : dv.entrySet()) {
            String table = "";
            for (int i = 0; i < entry.getValue().size(); i++) {
                table = table + ((i + 1) - (1 - offset)) + " "
                        + (entry.getValue().get(i).getThrough() + 1 - (1 - offset)) + " "
                        + entry.getValue().get(i).getCost() + "\n";
            }
            printWriter.println(table);
        }
        for (Message message : messages) {
            printWriter.println(getMessageEntry(dv, dv.get(message.getSrc() - 1), message, offset));
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
            List<Distance> distanceVectorOfSource, Message msg, int offset) {
        String ret = "";
        List<Integer> hops = new ArrayList<Integer>();
        hops.add(msg.getSrc() - (1 - offset));
        Distance temp = distanceVectorOfSource.get(msg.getDest() - 1);
        while (!(temp.getThrough() == (msg.getDest() - 1))) {
            hops.add(temp.getThrough() + 1 - (1 - offset));
            temp = distanceVectors.get(temp.getThrough()).get(msg.getDest() - 1);

        }
        hops.add(msg.getDest() - (1 - offset));

        ret = ret + "from " + (msg.getSrc() - (1 - offset)) + " to " + (msg.getDest() - (1 - offset)) + ": hops";
        for (int i = 0; i < hops.size(); i++) {
            ret = ret + " " + (hops.get(i)) + "";
        }
        ret = ret + "; message: " + msg.getMsg() + "\n";
        return ret;
    }

    /**
     * To pretty-print distance vectors
     * 
     * @param distanceVectors distance vectors to pretty-print
     */
    public static void printDistanceVectors(Map<Integer, List<Distance>> distanceVectors) {
        // print
        int j = 0;
        for (Map.Entry<Integer, List<Distance>> entry : distanceVectors.entrySet()) {
            System.out.print((j++) + "< ");
            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.print(
                        "( " + entry.getValue().get(i).getCost() + ", " + entry.getValue().get(i).getThrough() + " ) ");
            }
            AlgorithmUtils.p(">");
        }
    }
}