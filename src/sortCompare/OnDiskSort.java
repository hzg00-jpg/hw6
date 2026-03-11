package sortCompare;

import java.io.*;
import java.util.*;

/**
 * Implements an external (on-disk) mergesort to efficiently sort data that do
 * not fit into the main memory.
 * Instead, it reads files from the hard disk drive, loads in main memory small
 * chunks that fit in it, sorts them individually, and saves them in temporary
 * files.
 * It then repeatedly merges these temporary files into increasingly larger
 * sorted files until it sorts the entire original dataset.
 * Extra credit merging method done.
 */
public class OnDiskSort {

	int maxSize;
	private File workingDirectory;
	private Sorter<String> sorter;

	/**
	 * Creates a new sorter for sorting string data on disk. The sorter operates by
	 * reading in maxSize worth of data elements (in this case, Strings) and then
	 * sorts them using the provided sorter. It does this chunk by chunk for all of
	 * the data, at each stage writing the sorted data to temporary files in
	 * workingDirectory. Finally, the sorted files are merged together (in pairs)
	 * until there is a single sorted file. The final output of this sorting should
	 * be in outputFile
	 *
	 * @param maxSize
	 *                         the maximum number of items to put in a chunk
	 * @param workingDirectory
	 *                         the directory where any temporary files created
	 *                         during sorting
	 *                         should be placed
	 * @param sorter
	 *                         the sorter to use to sort the chunks in memory
	 */
	public OnDiskSort(int maxSize, File workingDirectory, Sorter<String> sorter) {
		this.maxSize = maxSize;
		this.workingDirectory = workingDirectory;
		this.sorter = sorter;

		// create directory if it doesn't exist
		if (!workingDirectory.exists()) {
			workingDirectory.mkdir();
		}
	}

	/**
	 * Remove all files that end with fileEnding from the workingDirectory
	 *
	 * If you name all of your temporary files with the same file ending, for
	 * example ".temp_sorted"
	 * then it's easy to clean them up using this method
	 *
	 * @param workingDirectory the directory to clear
	 * @param fileEnding       clear only those files with fileEnding
	 */
	private void clearOutDirectory(File workingDirectory, String fileEnding) {
		for (File file : workingDirectory.listFiles()) {
			if (file.getName().endsWith(fileEnding)) {
				file.delete();
			}
		}
	}

	/**
	 * Write the Strings stored in dataToWrite to outfile one String per line
	 *
	 * @param outfile     the output file
	 * @param dataToWrite the String data to write out
	 */
	private void writeToDisk(File outfile, ArrayList<String> dataToWrite) {
		try {
			PrintWriter out = new PrintWriter(new FileOutputStream(outfile));

			for (String s : dataToWrite) {
				out.println(s);
			}

			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Copy data from fromFile to toFile
	 *
	 * @param fromFile the file to be copied from
	 * @param toFile   the destination file to be copied to
	 */
	private void copyFile(File fromFile, File toFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fromFile));
			PrintWriter out = new PrintWriter(new FileOutputStream(toFile));

			String line = in.readLine();

			while (line != null) {
				out.println(line);
				line = in.readLine();
			}

			out.close();
			in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sort the data in dataReader using an on-disk version of sorting
	 *
	 * @param dataReader
	 *                   an Iterator that allows us to "scan"/read the data to be
	 *                   sorted
	 * @param outputFile
	 *                   the destination for the final sorted data
	 */
	public void sort(WordScanner dataReader, File outputFile) {
		int fileNumber = 0;

		ArrayList<String> chunk = new ArrayList<>();
    	ArrayList<File> sortedFiles = new ArrayList<>();

		try {
			while (dataReader.hasNext()) {
				chunk.add(dataReader.next());

				if (chunk.size() == maxSize) {
					sorter.sort(chunk);
					File tempFile = new File(workingDirectory, fileNumber + ".tempfile");
					fileNumber++;
					writeToDisk(tempFile, chunk);
					sortedFiles.add(tempFile);
					chunk.clear();
				}
			}

			if (!chunk.isEmpty()) {
				sorter.sort(chunk);
				File tempFile = new File(workingDirectory, fileNumber + ".tempfile");
				writeToDisk(tempFile, chunk);
				sortedFiles.add(tempFile);
        	}

			mergeFiles(sortedFiles, outputFile);
        	dataReader.close();
        	clearOutDirectory(workingDirectory, ".tempfile");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Merges all the Files in sortedFiles into one sorted file, whose destination
	 * is outputFile.
	 *
	 * @pre All of the files in sortedFiles contained data that is sorted
	 * @param sortedFiles a list of files containing sorted data
	 * @param outputFile  the destination file for the final sorted data
	 */
	protected void mergeFiles(ArrayList<File> sortedFiles, File outputFile) {
		if (sortedFiles.size() == 0) {
			return;
		} else if (sortedFiles.size() == 1) {
			copyFile(sortedFiles.get(0), outputFile);
		} else {
			try {
				File tempFile = new File(workingDirectory, "mergeExtra.temp");
				ArrayList<File> currentFiles = new ArrayList<>(sortedFiles);

				while (currentFiles.size() > 1) {
					ArrayList<File> nextRound = new ArrayList<>();
					int i = 0;

					while (i < currentFiles.size()) {
						if (i + 1 < currentFiles.size()) {
							// merge i and i+1
							merge(currentFiles.get(i), currentFiles.get(i + 1), tempFile);

							// copies back to first item in pair. for example, merging file3 and
							// file4 updates file3 and leaves file4 untouched.
							copyFile(tempFile, currentFiles.get(i));
							nextRound.add(currentFiles.get(i));
						} else {
							nextRound.add(currentFiles.get(i));
						}
					i += 2;
					}
            		currentFiles = nextRound;
        		}

        	copyFile(currentFiles.get(0), outputFile);
        	tempFile.delete();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Given two files containing sorted strings, one string per line, merge them
	 * into one sorted file
	 *
	 * @param file1   file containing sorted strings, one per line
	 * @param file2   file containing sorted strings, one per line
	 * @param outFile destination file for the results of merging the two files
	 */
	protected void merge(File file1, File file2, File outFile) {
		try {
			BufferedReader reader1 = new BufferedReader(new FileReader(file1));
			BufferedReader reader2 = new BufferedReader(new FileReader(file2));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

			String line1 = reader1.readLine();
			String line2 = reader2.readLine();

			while (line1 != null && line2 != null) {
				if (line1.compareTo(line2) <= 0) {
					writer.write(line1);
					writer.newLine();
					line1 = reader1.readLine();
				} else {
					writer.write(line2);
					writer.newLine();
					line2 = reader2.readLine();
				}
			}

			while (line1 != null) {
				writer.write(line1);
				writer.newLine();
				line1 = reader1.readLine();
			}

			while (line2 != null) {
				writer.write(line2);
				writer.newLine();
				line2 = reader2.readLine();
			}

			reader1.close();
			reader2.close();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a sorter that does a mergesort in memory
	 * Create a diskSorter to do external merges
	 * Use subdirectory "sorting_run" of your project as the working directory
	 * Create a word scanner to read King's "I have a dream" speech.
	 * Sort all the words of the speech and put them in file data.sorted
	 *
	 * @param args -- not used!
	 */
	public static void main(String[] args) {
		MergeSort<String> sorter = new MergeSort<String>();
		// 50 lines, should create 16 tempfiles, 0 to 15
		OnDiskSort diskSorter = new OnDiskSort(3, new File("sorting_run"), sorter); 

		WordScanner scanner = new WordScanner(new File("sorting_run//test.txt"));

		System.out.println("running");
		diskSorter.sort(scanner, new File("sorting_run//data.sorted"));
		System.out.println("done");
	}

}
