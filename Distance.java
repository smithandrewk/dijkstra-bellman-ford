/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: Distance.java
 * description: To aid in simulating a network (graph) of routers and the {Dijkstra, Bellman-Ford} algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
/**
 * A Distance is an object that has an attribute through, which is the last node
 * taken in a path to reach the Distance's current position, and a cost which is
 * the total cost to reach the current position.
 */
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