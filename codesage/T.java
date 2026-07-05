import java.nio.file.*;
public class T { public static void main(String[] a) throws Exception {
  Path p = Path.of("/tmp/codesage/repos", "test-id");
  Files.createDirectories(p.getParent());
  System.out.println("path=" + p.toAbsolutePath() + " exists=" + Files.exists(p.getParent()));
}}
