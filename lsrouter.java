
/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: lsrouter.java
 * description: To simulate a network (graph) of routers and Dijkstra's algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class utilizes Dijkstra's algorithm to produce a forwarding table for a
 * network, and it simulates the sending of messages, along with the changing of
 * link costs and how that may effect message path.
 */
public class lsrouter {
    private static final String BREAK = "------------";
    private static PrintWriter printWriter;
    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) throws FileNotFoundException {
        if (!(args.length == 3)) {
            System.out.println("Usage: java lsrouter <topofile> <changesfile> <messagefile>");
            System.exit(0);
        }
        printWriter = new PrintWriter(new FileOutputStream(OUTPUT_FILE));
        List<Link> initialTopology = AlgorithmUtils.getTopology(args[0]);
        List<Link> changes = AlgorithmUtils.getTopology(args[1]);
        List<Message> messages = AlgorithmUtils.getMessages(args[2]);
        int[][] adjacencyMatrix = AlgorithmUtils.getAdjacencyMatrix(initialTopology);
        printWriter.println(BREAK + "Output Before Changes" + BREAK);
        printToFile(adjacencyMatrix, messages);
        for (Link change : changes) {
            printWriter.println(BREAK + "Output After Change: " + change.getSrc() + " " + change.getDest() + " "
                    + change.getCost() + BREAK);
            adjacencyMatrix = AlgorithmUtils.applyChange(adjacencyMatrix, change);
            printToFile(adjacencyMatrix, messages);
        }
        printWriter.close();
    }

    /**
     * Run's least-cost algorithm and handles file printing format
     * 
     * @param adjacencyMatrix adjacency matrix to run algorithm on
     * @param messages        messages to simulate path with
     */
    public static void printToFile(int[][] adjacencyMatrix, List<Message> messages) {
        for (int i = 1; i <= adjacencyMatrix.length; i++) {
            printWriter.println("Forwarding Table for Router " + i);
            List<Link> nprime = dijkstra(adjacencyMatrix, i);
            int[][] nprimeAdj = AlgorithmUtils.getNPrimeAdjacencyMatrix(nprime);
            printWriter.println(getForwardingEntries(nprime, nprimeAdj.length));
        }
        printWriter.println(BREAK + "Messages" + BREAK);
        for (Message message : messages) {
            printWriter.println(getMessageEntry(adjacencyMatrix, message));
        }
    }

    /**
     * gets path for message and formats string to output to file
     * 
     * @param a   adjacency matrix of network topology
     * @param msg message to send
     * @return formatted string to output
     */
    public static String getMessageEntry(int[][] a, Message msg) {
        String ret = "";
        List<Link> nprime = dijkstra(a, msg.getSrc());
        Link destLink = null;
        List<Integer> hops = new ArrayList<Integer>();
        for (Link link : nprime) {
            if (link.getDest() == msg.getDest() - 1) {
                destLink = link;
                hops.add(destLink.getDest());
            }
        }
        while (destLink.getSrc() != (msg.getSrc() - 1)) {
            for (Link link : nprime) {
                if (link.getDest() == destLink.getSrc()) {
                    destLink = link;
                    hops.add(destLink.getDest());
                }
            }
        }
        hops.add(msg.getSrc() - 1);
        ret = ret + "from <" + msg.getSrc() + "> to <" + msg.getDest() + ">: hops";
        for (int i = 0; i < hops.size(); i++) {
            ret = ret + " <" + (hops.get(hops.size() - i - 1) + 1) + ">";
        }
        ret = ret + "; message: <" + msg.getMsg() + ">\n";
        return ret;
    }

    /**
     * To get all forwarding entries for each source router in Djikstra
     * 
     * @param nprime        least-cost tree from Dijkstra on a single node
     * @param numberOfNodes number of nodes in network
     * @return formatted forwarding tables to output to file
     */
    public static String getForwardingEntries(List<Link> nprime, int numberOfNodes) {
        String ret = "";
        for (int i = 1; i <= numberOfNodes; i++) {
            ret = ret + getForwardingEntry(nprime, i);
        }
        return ret;
    }

    /**
     * To get individual forwarding entry for a single source router in Dijkstra
     * 
     * @param nprime      least-cost tree from Dijkstra on a single node
     * @param destination destination node to find next hop
     * @return formatted string for specific forwarding entry
     */
    public static String getForwardingEntry(List<Link> nprime, int destination) {
        Link destLink = null;
        int cost = -1;
        int nextHop = -1;
        for (Link link : nprime) {
            if (link.getDest() == destination - 1) {
                cost = link.getCost();
                destLink = link;
            }
        }
        while (destLink.getSrc() != (nprime.get(0).getSrc())) {
            for (Link link : nprime) {
                if (link.getDest() == destLink.getSrc()) {
                    destLink = link;
                }
            }
        }
        // next hop
        nextHop = destLink.getDest() + 1;
        return destination + " " + nextHop + " " + cost + "\n";
    }

    /**
     * To print a tree as given by Dijkstra
     * 
     * @param nprime proprietary list of Links format for result of Dijkstra
     */
    public static void printTree(List<Link> nprime) {
        // Dont ask.
        // Source comes in as 1-5
        List<Integer> sources = new ArrayList<Integer>();
        List<Integer> dests = new ArrayList<Integer>();
        int source = -1;
        for (Link link : nprime) {
            if (link.getCost() == 0) {
                // is source
                source = link.getSrc();
                continue;
            } else if (sources.contains(link.getSrc())) {
                AlgorithmUtils.p("");
                System.out.print(link.getSrc() + "->" + link.getDest());
            } else if (!(dests.contains(link.getSrc()))) {
                System.out.print(link.getSrc() + "->" + link.getDest());
            } else {
                System.out.print("->" + link.getDest());
            }
            dests.add(link.getDest());
            sources.add(link.getSrc());
        }
        AlgorithmUtils.p("");
    }

    /**
     * Dijkstra. Calculates least-cost tree from a single node.
     * 
     * @param adjacencyMatrix network topology
     * @param source          source node
     * @return proprietary list of links with least-cost tree
     */
    public static List<Link> dijkstra(int[][] adjacencyMatrix, int source) {
        if (source < 1 || source > adjacencyMatrix.length) {
            System.out.println("Source out of bounds");
            return null;
        }
        source = source - 1;

        List<Link> nprime = new ArrayList<Link>();
        Map<Integer, Distance> distances = new HashMap<Integer, Distance>();
        // init all distances to 0
        // Distance -- through, cost
        for (int i = 0; i < adjacencyMatrix.length; i++) {
            distances.put(i, new Distance(0, 0));
        }
        Link currNode = new Link(source, source, 0);
        distances.remove(currNode.getSrc());
        nprime.add(currNode);

        for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
            if (adjacencyMatrix[currNode.getDest()][entry.getKey()] == 0) {
                continue;
            }
            if ((adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost()) < entry.getValue()
                    .getCost()) {
                entry.setValue(new Distance(currNode.getDest(),
                        (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
            } else if (entry.getValue().getCost() == 0) {
                entry.setValue(new Distance(currNode.getDest(),
                        (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
            }
        }

        // while distance isn't empty
        while (distances.size() > 0) {
            Link minNode = new Link(0, 0, 0);
            for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
                if (entry.getValue().getCost() == 0) {
                    continue;
                } else if (minNode.getCost() == 0) {
                    minNode = new Link(entry.getValue().getThrough(), entry.getKey(), entry.getValue().getCost());
                } else if ((entry.getValue().getCost()) < minNode.getCost()) {
                    minNode = new Link(entry.getValue().getThrough(), entry.getKey(), entry.getValue().getCost());
                } else {
                    continue;
                }
            }
            currNode = minNode;
            nprime.add(currNode);
            distances.remove(currNode.getDest());

            for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
                if (adjacencyMatrix[currNode.getDest()][entry.getKey()] == 0) {
                    continue;
                }
                if ((adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost()) < entry.getValue()
                        .getCost()) {
                    entry.setValue(new Distance(currNode.getDest(),
                            (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
                } else if (entry.getValue().getCost() == 0) {
                    entry.setValue(new Distance(currNode.getDest(),
                            (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
                }
            }

        }
        return nprime;
    }

}