import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

class OutputComparator {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java OutputComparator file1 file2");
      System.exit(0);
    }

    try {
      final Scanner file1 = new Scanner(new File(args[0]));
      final Scanner file2 = new Scanner(new File(args[1]));

      final ArrayList<String> file1Lines = OutputComparator.getFileLines(file1);
      final ArrayList<String> file2Lines = OutputComparator.getFileLines(file2);

      boolean filesMatch = true;
      for (int i = 0; i < file1Lines.size(); i++) {
        if (!file1Lines.get(i).equals(file2Lines.get(i))) {
          System.out.println(file1Lines.get(i) + " - " + file2Lines.get(i) + " - at line " + i);
          filesMatch = false;
          break;
        }
      }

      if (filesMatch) {
        System.out.println("These files DO match.");
      } else {
        System.out.println("These files DO NOT match.");
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public static ArrayList<String> getFileLines(final Scanner file) {
    final ArrayList<String> fileLines = new ArrayList<>();

    String nextLine;
    while (file.hasNext()) {
      nextLine = file.nextLine().trim();

      if (nextLine.length() > 0) {
        fileLines.add(nextLine);
      }
    }

    return fileLines;
  }
}