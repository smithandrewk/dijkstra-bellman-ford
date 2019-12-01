import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class lsrouter {
    private static final String BREAK = "------------";
    private static Scanner fileScanner;
    private static PrintWriter printWriter;

    public static void main(String[] args) throws FileNotFoundException {
        if (!(args.length == 3)) {
            System.out.println("Usage: java lsrouter <topofile> <changesfile> <messagefile>");
            System.exit(0);
        }
        printWriter = new PrintWriter(new FileOutputStream("output.txt"));
        List<Link> initialTopology = getTopology(args[0]);
        List<Link> changes = getTopology(args[1]);
        List<Message> messages = getMessages(args[2]);
        int[][] adjacencyMatrix = getAdjacencyMatrix(initialTopology);
        printWriter.println(BREAK + "Output Before Changes" + BREAK);
        exec(adjacencyMatrix, messages);
        for (Link change : changes) {
            printWriter.println(BREAK + "Output After Change: " + change.getSrc() + " " + change.getDest() + " "
                    + change.getCost() + BREAK);
            adjacencyMatrix = applyChange(adjacencyMatrix, change);
            exec(adjacencyMatrix, messages);
        }
        printWriter.close();

    }

    public static void distanceVector(int[][] adjacencyMatrix) {
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
                    vector.add(new Distance(i, adjacencyMatrix[i][j]));
                }
            }
            distanceVectors.put(i, vector);
        }

        printDistanceVectors(distanceVectors);
        p(BREAK);
        boolean change = true;
        while (change) {
            change = false;
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                List<Distance> newVector = new ArrayList<Distance>();
                // get adjacent vectors
                List<Integer> adjacentVectors = new ArrayList<Integer>();
                for (int j = 0; j < distanceVectors.get(i).size(); j++) {
                    if (distanceVectors.get(i).get(j).getCost() > 0) {
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
                            through = integer;
                        } else if ((distanceVectors.get(integer).get(j).getCost()
                                + distanceVectors.get(i).get(integer).getCost()) < min) {
                            change = true;
                            min = distanceVectors.get(integer).get(j).getCost()
                                    + distanceVectors.get(i).get(integer).getCost();
                            through = integer;
                        }
                    }
                    newVector.add(new Distance(through, min));
                }
                // each iteration here
                distanceVectors.put(i, newVector);
            }
        }
        // convergence here
        printDistanceVectors(distanceVectors);
        // now print messages, apply change, run again

    }

    public static void printDistanceVectors(Map<Integer, List<Distance>> distanceVectors) {
        // print
        for (Map.Entry<Integer, List<Distance>> entry : distanceVectors.entrySet()) {
            System.out.print("< ");
            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.print(entry.getValue().get(i).getCost() + " ");
            }
            p(">");
        }
    }

    public static void exec(int[][] adjacencyMatrix, List<Message> messages) {
        // printUpperHalfOfAdjacencyMatrix(adjacencyMatrix);
        // System.out.println(BREAK);

        for (int i = 1; i <= adjacencyMatrix.length; i++) {
            printWriter.println("Forwarding Table for Router " + i);
            List<Link> nprime = dijkstra(adjacencyMatrix, i);
            int[][] nprimeAdj = getNPrimeAdjacencyMatrix(nprime);
            // printTree(nprime);
            // p(BREAK);
            printWriter.println(getForwardingEntries(nprime, nprimeAdj.length));
            // p(BREAK);
        }
        printWriter.println(BREAK + "Messages" + BREAK);
        for (Message message : messages) {
            printWriter.println(getMessageEntry(adjacencyMatrix, message));
        }
    }

    public static int[][] applyChange(int[][] adjMat, Link change) {
        if (change.getCost() == -999) {
            adjMat[change.getSrc() - 1][change.getDest() - 1] = 0;
        } else {
            adjMat[change.getSrc() - 1][change.getDest() - 1] = change.getCost();
        }
        return adjMat;
    }

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

    public static String getForwardingEntries(List<Link> nprime, int numberOfNodes) {
        String ret = "";
        for (int i = 1; i <= numberOfNodes; i++) {
            ret = ret + getForwardingEntry(nprime, i);
        }
        return ret;
    }

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

    public static void p(String toPrint) {
        System.out.println(toPrint);
    }

    // Dont freaking ask.
    // Source comes in as 1-5
    public static void printTree(List<Link> nprime) {
        List<Integer> sources = new ArrayList<Integer>();
        List<Integer> dests = new ArrayList<Integer>();
        int source = -1;
        for (Link link : nprime) {
            if (link.getCost() == 0) {
                // is source
                source = link.getSrc();
                continue;
            } else if (sources.contains(link.getSrc())) {
                p("");
                System.out.print(link.getSrc() + "->" + link.getDest());
            } else if (!(dests.contains(link.getSrc()))) {
                System.out.print(link.getSrc() + "->" + link.getDest());
            } else {
                System.out.print("->" + link.getDest());
            }
            dests.add(link.getDest());
            sources.add(link.getSrc());
        }
        p("");
    }

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
        // while all nodes aren't in nprime
        // or
        // while distance isn't empty
        while (distances.size() > 0) {
            Link minNode = new Link(0, 0, 0);
            for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
                // System.out.println("min: "+minNode+"entry: "+entry.getValue().getCost());
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
            // System.out.println(minNode);
            currNode = minNode;
            nprime.add(currNode);
            distances.remove(currNode.getDest());
            // getmincostindistance
            // add to nprime
            // for (Map.Entry<Integer, Distance> entry : distances.entrySet()){

            // System.out.println(entry.toString());
            // }
            for (Map.Entry<Integer, Distance> entry : distances.entrySet()) {
                // System.out.println(entry.getKey());
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
            // for (Map.Entry<Integer, Distance> entry : distances.entrySet()){

            // System.out.println(entry.toString());
            // }
        }
        return nprime;
    }

    public static void printAdjacencyMatrix(int[][] a) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                System.out.print(a[i][j] + " ");
            }
            System.out.println();
        }
    }

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

    // Assume input file is valid
    public static int[][] getNPrimeAdjacencyMatrix(List<Link> l) {
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

    // Assume input file is valid
    public static int[][] getAdjacencyMatrix(List<Link> l) {
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

    public static List<Message> getMessages(String fileName) {
        // System.out.println("lsrouter::getMessages");
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

class Distance {
    private int through, cost;

    public Distance(int through, int cost) {
        this.through = through;
        this.cost = cost;
    }

    public int getThrough() {
        return through;
    }

    public void setThrough(int through) {
        this.through = through;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "Distance [through=" + through + ", cost=" + cost + "]";
    }

}

class Link {
    private int src, dest, cost;

    public Link(int src, int dest, int cost) {
        this.src = src;
        this.dest = dest;
        this.cost = cost;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public int getDest() {
        return dest;
    }

    public void setDest(int dest) {
        this.dest = dest;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "Link [src=" + src + ", dest=" + dest + ", cost=" + cost + "]";
    }

}

class Message {
    private int src, dest;
    private String msg;

    public Message(int src, int dest, String msg) {
        this.src = src;
        this.dest = dest;
        this.msg = msg;
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public int getDest() {
        return dest;
    }

    public void setDest(int dest) {
        this.dest = dest;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Message [src=" + src + ", dest=" + dest + ", msg=" + msg + "]";
    }

}