import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DocProcessor {

	public static String getTitle(String folderStr, Integer docNo) {
		String title = "";

		final File folder = new File(folderStr);
		title = listFilesForFolder(folder, docNo);
		return title;

	}

	private static String listFilesForFolder(final File folder, Integer docNo) {
		String title = "";
		Integer fileName1;
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry, docNo);
			} else {
				String fileName = fileEntry.getName().replace("cranfield", "");
				fileName1 = Integer.parseInt(fileName);
				if (fileName1.equals(docNo)) {
					String filePath = folder + "/" + fileEntry.getName();
					title = readCranfieldforTitle(filePath, fileName);
					//readCranfieldfordocvector(filePath, fileName);
					return title;
				}

			}
		}
		return null;
	}

	/*private static void readCranfieldfordocvector(String filePath,
			String fileName) {
		// TODO Auto-generated method stub
		FileReader inputFile = new FileReader(filePath);
		BufferedReader bufferReader = new BufferedReader(inputFile);

		String line = "";

		while ((line = bufferReader.readLine()) != null) {
			
		
	}
*/
	private static String readCranfieldforTitle(String filePath, String fileName) {

		boolean nextLine = false;
		try {

			FileReader inputFile = new FileReader(filePath);
			BufferedReader bufferReader = new BufferedReader(inputFile);

			String line = "";

			while ((line = bufferReader.readLine()) != null) {
				if (nextLine) {
					String text = "";
					while (!line.equals("</TITLE>")) {
						text = text + " " + line;
						line = bufferReader.readLine();
					}
					bufferReader.close();
					return text;
				}
				if (line.equals("<TITLE>")) {
					nextLine = true;
				}

			}

			bufferReader.close();

		} catch (Exception e) {
			System.out.println("Error while reading file" + fileName);
		}
		return null;

	}
}
