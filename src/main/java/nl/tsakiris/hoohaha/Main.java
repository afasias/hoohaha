package nl.tsakiris.hoohaha;

import lombok.SneakyThrows;

public class Main {

  @SneakyThrows
  public static void main(String[] args) {

    Configuration configuration = new TestConfiguration();
    FingerPrinter fingerPrinter = new FingerPrinter(configuration, null);
    Thread fingerPrinterThread = new Thread(fingerPrinter);
    fingerPrinter.setRunning(true);
    fingerPrinterThread.start();

    long startMillis = System.currentTimeMillis();
    while (fingerPrinter.isRunning()) {
      System.out.println("Total files: " + fingerPrinter.totalFiles());
      System.out.println("Total directories: " + fingerPrinter.totalDirectories());
      System.out.println("Total bytes: " + fingerPrinter.totalBytes());
      System.out.println("Processed files: " + fingerPrinter.getProcessedFiles());
      System.out.println("Processed bytes: " + fingerPrinter.getProcessedBytes());
      double elapsedMillis = (double) (System.currentTimeMillis() - startMillis);
      System.out.println("Elapsed time (sec): " + elapsedMillis / 1000);
      double estimatedMillis = (double) fingerPrinter.totalBytes() /
          (double) fingerPrinter.getProcessedBytes() * elapsedMillis;
      System.out.println("Time remaining (sec): " + (estimatedMillis - elapsedMillis) / 1000);
      System.out.println("--------------------------");
      Thread.sleep(1000);
    }
    fingerPrinterThread.join();
  }

}
