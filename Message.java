/**
 * author: Andrew Smith
 * last edited: 12/05/19
 * file: Message.java
 * description: To aid in simulating a network (graph) of routers and the {Dijkstra, Bellman-Ford} algorithm. The topology file must have line-by-line entries of the form <src> <dest> <cost>. The changes file must have the same format. The messages file must have line-by-line entries of the form <src> <dest> <message>.
 */
/**
 * Messages are created from the messages file. A message consists of a source
 * router, a destination router, and a message.
 */
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