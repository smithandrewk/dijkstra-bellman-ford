
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
    // handle printing to file
    private static PrintWriter printWriter;
    // path to output file
    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) throws FileNotFoundException {
        // Check for correct number of arguments
        if (!(args.length == 3)) {
            System.out.println("Usage: java lsrouter <topofile> <changesfile> <messagefile>");
            System.exit(0);
        }
        // init printwriter to output file
        printWriter = new PrintWriter(new FileOutputStream(OUTPUT_FILE));
        // get initial toppology from file -> initialTopology
        List<Link> initialTopology = AlgorithmUtils.getTopology(args[0]);
        // get offset (see AlgorithmUtils for explanation)
        int offset = AlgorithmUtils.getOffset(initialTopology);
        // get changes file -> changes
        List<Link> changes = AlgorithmUtils.getTopology(args[1]);
        // apply offset
        for (Link link : changes) {
            link.setSrc(link.getSrc() + (1 - offset));
            link.setDest(link.getDest() + (1 - offset));
        }
        // get messages file -> messages
        List<Message> messages = AlgorithmUtils.getMessages(args[2]);
        // apply offset
        for (Message message : messages) {
            message.setSrc(message.getSrc() + (1 - offset));
            message.setDest(message.getDest() + (1 - offset));
        }

        // format initial topology as adjacency matrix
        int[][] adjacencyMatrix = AlgorithmUtils.getAdjacencyMatrix(initialTopology, offset);
        // wrapper that handles printing and running Dijkstra
        runDijkstraAndPrintToFile(adjacencyMatrix, messages, offset);
        // now re-run for all changes in changes file
        for (Link change : changes) {
            // get new topology
            adjacencyMatrix = AlgorithmUtils.applyChange(adjacencyMatrix, change);
            // handle printing and run Dijkstra on new topology
            runDijkstraAndPrintToFile(adjacencyMatrix, messages, offset);
        }
        // close stream
        printWriter.close();
    }

    /**
     * Run's least-cost algorithm and handles file printing format
     * 
     * @param adjacencyMatrix adjacency matrix to run algorithm on
     * @param messages        messages to simulate path with
     */
    public static void runDijkstraAndPrintToFile(int[][] adjacencyMatrix, List<Message> messages, int offset) {
        // Run Dijkstra for every router
        for (int i = 1; i <= adjacencyMatrix.length; i++) {
            // get 'nprime' list from Dijkstra (actually a tree)
            List<Link> nprime = dijkstra(adjacencyMatrix, i);
            // get adjacencyMatrix for nearest neighbor tree as a result of Dijkstra from
            // source i
            int[][] nprimeAdj = AlgorithmUtils.getNPrimeAdjacencyMatrix(nprime);
            // print forwarding table for this router to output file
            printWriter.println(getForwardingEntries(nprime, nprimeAdj.length, offset));
        }
        // for each message in message file
        for (Message message : messages) {
            // simulate sending each message and print hops to output file
            // System.out.println("--------------messages--------------");
            printWriter.println(getMessageEntry(adjacencyMatrix, message, offset));
        }
    }

    /**
     * gets path for message and formats string to output to file
     * 
     * @param a   adjacency matrix of network topology
     * @param msg message to send
     * @return string formatted as “from <x> to <y>: hops <hop1> <hop2> <...>;
     *         message: <message>”
     */
    public static String getMessageEntry(int[][] a, Message msg, int offset) {
        // container for string to return
        String ret = "";
        // shortest path from message src to all reachable nodes
        List<Link> nprime = dijkstra(a, msg.getSrc());
        // Destination link for message
        // Used for hopping in reverse direction from destination to soure
        Link destLink = null;
        // To hold hops for message path (will be in reverse direction at first)
        List<Integer> hops = new ArrayList<Integer>();
        // for each link in the shortest path tree from message source
        for (Link link : nprime) {
            // looking for destination in nprime
            // because msg is in human form (first node is 1)
            if (link.getDest() == msg.getDest() - 1) {
                // set dest link
                destLink = link;
                // add to hops
                hops.add(destLink.getDest() - (1 - offset));
            }
        }
        // hop from destination in reverse order to source
        while (destLink.getSrc() != (msg.getSrc() - 1)) {
            // check every link in shortest path tree
            for (Link link : nprime) {
                // if link is source of current dest link, hop
                if (link.getDest() == destLink.getSrc()) {
                    destLink = link;
                    hops.add(destLink.getDest() - (1 - offset));
                }
            }
        }
        // add the source (human readable form)
        hops.add(msg.getSrc() - 1 - (1 - offset));
        // string format and return
        ret = ret + "from " + (msg.getSrc() - (1 - offset)) + " to " + (msg.getDest() - (1 - offset)) + ": hops";
        for (int i = 0; i < hops.size(); i++) {
            ret = ret + " " + (hops.get(hops.size() - i - 1) + 1) + "";
        }
        ret = ret + "; message: " + msg.getMsg() + "\n";
        return ret;
    }

    /**
     * To get all forwarding entries for each source router in Djikstra
     * 
     * @param nprime        least-cost tree from Dijkstra on a single node
     * @param numberOfNodes number of nodes in network
     * @return formatted forwarding tables to output to file
     */
    public static String getForwardingEntries(List<Link> nprime, int numberOfNodes, int offset) {
        String ret = "";
        // one table for each destination router
        for (int i = 1; i <= numberOfNodes; i++) {
            ret = ret + getForwardingEntry(nprime, i, offset);
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
    public static String getForwardingEntry(List<Link> nprime, int destination, int offset) {
        Link destLink = null;
        int cost = -1;
        int nextHop = -1;
        // for every link in shortest spanning tree from router
        for (Link link : nprime) {
            // if dest is dest in human readable
            if (link.getDest() == destination - 1) {
                // set cost and set dest
                cost = link.getCost();
                destLink = link;
            }
        }
        // find next hop
        while (destLink.getSrc() != (nprime.get(0).getSrc())) {
            for (Link link : nprime) {
                if (link.getDest() == destLink.getSrc()) {
                    destLink = link;
                }
            }
        }
        // next hop
        nextHop = destLink.getDest() + 1;
        return (destination - (1 - offset)) + " " + (nextHop - (1 - offset)) + " " + cost + "\n";
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
        // pop off curr node
        distances.remove(currNode.getSrc());
        // first in shortest path tree

        nprime.add(currNode);

        // for all nodes in graph
        for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
            // no edge from curr node to entry
            if (adjacencyMatrix[currNode.getDest()][entry.getKey()] == 0) {
                continue;
            }
            if ((adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost()) < entry.getValue()
                    .getCost()) {
                entry.setValue(new Distance(currNode.getDest(),
                        (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
            } else if ((adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost()) == entry.getValue()
                    .getCost()) {
                if (currNode.getDest() < entry.getKey()) {
                    entry.setValue(new Distance(currNode.getDest(),
                            (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
                }
            } else if (entry.getValue().getCost() == 0) {
                entry.setValue(new Distance(currNode.getDest(),
                        (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
            }
        }

        // while distance isn't empty
        while (distances.size() > 0) {
            Link minNode = new Link(0, 0, 0);
            for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
                // no edge to this node
                if (entry.getValue().getCost() == 0) {
                    continue;
                    // first node with edge
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
                } else if ((adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost()) == entry
                        .getValue().getCost()) {
                    if (currNode.getDest() < entry.getValue().getThrough()) {
                        entry.setValue(new Distance(currNode.getDest(),
                                (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
                    }
                } else if (entry.getValue().getCost() == 0) {
                    entry.setValue(new Distance(currNode.getDest(),
                            (adjacencyMatrix[currNode.getDest()][entry.getKey()] + currNode.getCost())));
                }
            }

        }
        return nprime;
    }

}