/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: Link.java
 * description: To aid in simulating a network (graph) of routers and the {Dijkstra, Bellman-Ford} algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
/**
 * Links are created from the topology file and used to represent links between
 * routers in the network. A Link consists of a source router, a destination
 * router, and a cost.
 */
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