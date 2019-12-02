#Makefile for dijkstra-bellman-ford simulation
default: dvrouter.class lsrouter.class

#dvrouter
dvrouter.class: dvrouter.java
	javac dvrouter.java

#lsrouter
lsrouter.class: lsrouter.java
	javac lsrouter.java
#run lsrouter
ls:
	java lsrouter topofile changesfile messagefile
#run dvrouter
dv:
	java dvrouter topofile changesfile messagefile
#remove class files
clean:
	rm -rf *.class \