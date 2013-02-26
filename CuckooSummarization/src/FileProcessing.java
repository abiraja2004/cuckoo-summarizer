import java.io.File;


public class FileProcessing {
	public void renameSummarizationFiles(String args[]) {
		File dir=new File("/home/krish/Dropbox/Cuckoo Summarization/DUC2002_Summaries1.0/");
		File[] listFiles=dir.listFiles();
		File f1,f2;
		for(int i=0;i<listFiles.length;i++) {
			f1=new File(listFiles[i].toString());
			f2=new File(listFiles[i].toString()+".system");
			f1.renameTo(f2);
		}
	}
	
	public static void main(String args[])
	{
		String str="" +
				"             " +
				"india" +
				"" +
				"    	krish";
		System.out.println(str.replaceAll("[\n\t ]", ""));
	}
}
