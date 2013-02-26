import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Duc2002Summarization {

	public Document parse(String file) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(file);
		return document;
	}

	// Parse XML document to get Duc2002Document object
	public Duc2002Document getParsedDocument(Document document) throws Exception {
		Duc2002Document ducDoc=new Duc2002Document();
		// iterate through child elements of root
		Element root = document.getRootElement();
		for ( Iterator i = root.elementIterator(); i.hasNext(); ) {
			Element element = (Element) i.next();
			String elementName=element.getQualifiedName();
			if(elementName.equals("DOCNO"))
				ducDoc.setDocNo(element.getStringValue());
			else if(elementName.equals("HEAD"))
				ducDoc.setTopic(element.getStringValue());
			else if(elementName.equals("TEXT"))
			{
				ducDoc.setText(element.getStringValue());
				String[] sentences = new String[element.elements().size()];
				int[] sentenceWordCounts = new int[element.elements().size()];
				int no=0;
				for(Iterator j=element.elementIterator(); j.hasNext();) {
					Element sentence = (Element) j.next();
					sentences[no]=sentence.getStringValue();
					for(Iterator k=sentence.attributeIterator(); k.hasNext(); ) {
						Attribute attribute=(Attribute) k.next();
						if(attribute.getQualifiedName().equals("wdcount"))
							sentenceWordCounts[no]=Integer.parseInt(attribute.getStringValue());
					}
					no++;
				}
				ducDoc.setSentences(sentences);
				ducDoc.setSentenceWordCounts(sentenceWordCounts);
			}
		}
		return ducDoc;
	}

	// get parsed array of documents that contain single document gold summaries from a "perdocs" in DUC 2002 data set
	public Duc2002Document[] getParsedSummariesFromPerDocs(String fileName) throws Exception {
		BufferedReader br=new BufferedReader(new FileReader(new File(fileName)));
		String line;
		Duc2002Document summary=new Duc2002Document();
		Duc2002Document[] goldSummaries=new Duc2002Document[20];
		int summaryCount=-1;
		while((line=br.readLine())!=null) {
			if(line.startsWith("DOCREF")) {
				System.out.println("docno: "+line.substring(line.indexOf("\"")+1, line.lastIndexOf("\"")));
				//summary.setDocNo(line.substring(line.indexOf("\"")+1, line.lastIndexOf("\"")));
				summary=new Duc2002Document();
				summary.setDocNo(line.substring(line.indexOf("\"")+1, line.lastIndexOf("\"")));
				summary.setText("");
			}
			else if(line.endsWith("\">")) { //encountering text
				br.readLine(); //empty newline
				String text="";
				while((line=br.readLine())!=null) {
					text=text+line;
					if(line.endsWith("</SUM>")) {
						text=text.substring(0,text.indexOf("</SUM>"));
						break;
					}
				}
				summary.setText(text);
				//System.out.println("text: "+summary.getText());
				goldSummaries[++summaryCount]=summary;
			}
		}
		Duc2002Document[] goldSummaries2=new Duc2002Document[summaryCount+1];
		for(int count=0;count<=summaryCount;count++)
			goldSummaries2[count]=goldSummaries[count];
		return goldSummaries2;
	}

	// Replace '&' in DUC 2002 Documents with 'and' so that XML files remain valid    
	public void replaceAmpersand(String dirPath) throws Exception {
		File dir=new File(dirPath);
		File[] listDirectories=dir.listFiles();
		for(int i=0;i<listDirectories.length;i++) {
			File[] listFiles=listDirectories[i].listFiles();
			for(int j=0;j<listFiles.length;j++) {
				BufferedReader br=new BufferedReader(new FileReader(listFiles[j].toString()));
				BufferedWriter bw=new BufferedWriter(new FileWriter(listFiles[j].toString()+"-new"));
				String line="";
				while ((line=br.readLine())!=null)
				{		  
					line=line.replaceAll("&","and");
					bw.write(line+"\n");
					bw.flush();
				}
				bw.close();
				File f=new File(listFiles[j].toString());
				f.delete();
				f=new File(listFiles[j].toString()+"-new");
				f.renameTo(new File(listFiles[j].toString()));
			}
		}
	}

	// Setup the entire DUC 2002 data set to output a directory containing all 533 docs with text only (titles are docNo's with extension ".doc")
	public void setupDuc2002Documents(String duc2002DocsWithSentenceBreaksDirPath, String outputDirPath) throws Exception {
		this.replaceAmpersand(duc2002DocsWithSentenceBreaksDirPath);
		File dir=new File(duc2002DocsWithSentenceBreaksDirPath);
		File[] listDirectories=dir.listFiles();
		BufferedWriter bw;
		String docDirPath=outputDirPath;
		for(int i=0;i<listDirectories.length;i++) {
			File[] listFiles=listDirectories[i].listFiles();
			for(int j=0;j<listFiles.length;j++) {
				Document doc=this.parse(listFiles[j].toString());
				Duc2002Document ducDoc=this.getParsedDocument(doc);
				String docNo=ducDoc.getDocNo().replaceAll("\n","");
				bw=new BufferedWriter(new FileWriter(new File(docDirPath+docNo+".doc")));
				String text=ducDoc.getText().replaceAll("\n","");
				bw.write(text);
				bw.close();
			}
		}
	}

	// Generate automated summaries of DUC 2002 data set to output a directory containing all 533 summaries with text only (titles are docNo's with extension ".summ")
	public void summarizeDuc2002Documents(String nameOfSystem, String duc2002DocsWithSentenceBreaksDirPath, String outputDirPath) throws Exception {
		this.replaceAmpersand(duc2002DocsWithSentenceBreaksDirPath);
		File dir=new File(duc2002DocsWithSentenceBreaksDirPath);
		File[] listDirectories=dir.listFiles();
		PageRankSummarizer s=new PageRankSummarizer();
		BufferedWriter bw2;
		String summaryDirPath=outputDirPath;
		for(int i=0;i<listDirectories.length;i++) {
			File[] listFiles=listDirectories[i].listFiles();
			for(int j=0;j<listFiles.length;j++) {
				Document doc=this.parse(listFiles[j].toString());
				Duc2002Document ducDoc=this.getParsedDocument(doc);
				String docNo=(ducDoc.getDocNo()).replaceAll("[\n\t ]","");
				System.out.println("\nSummarizing "+docNo+"...");
				bw2=new BufferedWriter(new FileWriter(new File(summaryDirPath+docNo+"."+nameOfSystem+".system")));
				String summarySentences[];
				if(s.splitIntoSentences(ducDoc.getText()).length <= 5) {
					summarySentences=s.splitIntoSentences(ducDoc.getText());
				}
				else {
					summarySentences=s.splitIntoSentences(s.getSummary(ducDoc.getText(),100));
				}
				for(int senNo=0;senNo<summarySentences.length;senNo++) {
					bw2.write(summarySentences[senNo]+"\n");
					bw2.flush();
				}
				bw2.close();
			}
		}
	}

	// Setup all DUC 2002 manual single document summaries to output a directory with all per doc summaries (titles are docNo's with extension ".gold")
	public void setupDuc2002ManualPerDocSummaries(String duc2002ExtractsAbstractsDirPath, String outputDirPath) throws Exception {
		//this.replaceAmpersand(duc2002ExtractsAbstractsDirPath);
		File dir=new File(duc2002ExtractsAbstractsDirPath);
		File[] listDirectories=dir.listFiles();
		BufferedWriter bw;
		String goldDirPath=outputDirPath;
		for(int i=0;i<listDirectories.length;i++) {
			File[] listFiles=listDirectories[i].listFiles();
			if(listFiles!=null) {
				for(int j=0;j<listFiles.length;j++) {
					String fileName=listFiles[j].toString();
					if(fileName.endsWith("perdocs")) {
						Duc2002Document[] goldSummaries=this.getParsedSummariesFromPerDocs(fileName);
						for(int k=0;k<goldSummaries.length;k++) {
							String newGoldDirName=goldDirPath+goldSummaries[k].getDocNo();
							File f=new File(newGoldDirName);
							if(!f.exists())
								f.mkdir();
							String newFileName=newGoldDirName+"/"+goldSummaries[k].getDocNo()+".gold";
							f=new File(newFileName);
							if(!f.exists()) {
								bw=new BufferedWriter(new FileWriter(new File(newFileName)));
							}
							else {
								int l=2;
								while((new File(newGoldDirName+"/"+goldSummaries[k].getDocNo()+"."+l+".gold").exists())) {
									l++;
								}
								bw=new BufferedWriter(new FileWriter(new File(newGoldDirName+"/"+goldSummaries[k].getDocNo()+"."+l+".gold")));
							}
							PageRankSummarizer s=new PageRankSummarizer();
							String summarySentences[]=s.splitIntoSentences(goldSummaries[k].getText());
							for(int senNo=0;senNo<summarySentences.length;senNo++) {
								bw.write(summarySentences[senNo]+"\n");
								bw.flush();
							}
							bw.close();
						}
					}
				}
			}
		}
	}

	public static void main(String args[]) throws Exception {
		Duc2002Summarization duc=new Duc2002Summarization();
		String duc2002DocsWithSentenceBreaksDirPath="/home/krish/Dropbox/Cuckoo Summarization/DUC DATA/DUC2002_Summarization_Documents/docs.with.sentence.breaks/";
		String duc2002ExtractsAbstractsDirPath="/home/krish/Dropbox/Cuckoo Summarization/DUC DATA/DUC2002_Summarization_Documents/extracts_abstracts/";
		String duc2002DataOutputDir="/home/krish/Dropbox/Cuckoo Summarization/DUC2002_Processed/DUC2002_Docs/";
		String duc2002GoldOutputDir="/home/krish/Dropbox/Cuckoo Summarization/DUC2002_Processed/DUC2002_GoldSummaries/";
		String duc2002SystemOutputDir="/home/krish/Dropbox/Cuckoo Summarization/DUC2002_Processed/DUC2002_SystemSummaries/";
		//duc.setupDuc2002Documents(duc2002DocsWithSentenceBreaksDirPath,duc2002DataOutputDir);
		//duc.setupDuc2002ManualPerDocSummaries(duc2002DocsWithSentenceBreaksDirPath,duc2002GoldOutputDir);
		String nameOfSystem="cuckoo1";
		duc.summarizeDuc2002Documents(nameOfSystem,duc2002DocsWithSentenceBreaksDirPath,duc2002SystemOutputDir);
	}
}