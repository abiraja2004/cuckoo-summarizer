import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class PageRankSummarizer { 

  int[] sentenceLengths;
  int totalLength=0; //No of tokens

  public boolean isPunct(String token) {
    if(token.length() == 0) {
      return true;
    }
    else if(token.length() == 1) {
      switch(token.charAt(0)) {
	case '.':
	case ',':
	case '?':
	case '!':
	case ':':
	case ';':
	case '"':
	case '-':
	case '/':
	case '\'':
	case '`':
	case '|': return true;
	default: return false;
      }
    }
    else {
      return false;
    }
  }
  
  public String[] splitIntoSentences(String text) {
    InputStream modelIn;
    SentenceModel model;
    String sentences[]={""};
    try {
      modelIn = getClass().getResourceAsStream("en-sent.bin");
      model = new SentenceModel(modelIn);
      if (modelIn != null) {
           modelIn.close();
      }
      SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
      sentences = sentenceDetector.sentDetect(text);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return sentences;
  }

  public float getSimilarityScore(String[] tokens1, String[] tokens2) {
    int count=0,i,j;
    for(i=0;i<tokens1.length;i++) {
      for(j=0;j<tokens2.length;j++) {
	//System.out.println("i:"+i+" j:"+j+" tokens1: "+tokens1[i]+" tokens2: "+tokens2[j]);
	if(!isPunct(tokens1[i]) && tokens1[i].equals(tokens2[j])) {
	  count++;
	}
      }
    }
    float similarityScore=(float)count/(float)(Math.log((double)tokens1.length)+Math.log((double)tokens2.length));
    return similarityScore;
  }

  public float[][] constructSimilarityMatrix(String[][] tokens) {
    float[][] similarities = new float[200][200];
    int i,j;
    for(i=0;i<tokens.length;i++) {
      for(j=i+1;j<tokens.length;j++) {
	similarities[i][j]=getSimilarityScore(tokens[i],tokens[j]);
      }
    }
    return similarities;
  }

  public float[] scoreSentences(String[] sentences) {
    float[] scores;
    float[] positionScores, lengthScores, tfs, tss, pageRankScores;
    float[] sentenceSimilarities;
    HashMap hm=new HashMap();
    String[] tempTokens;
    int noOfSentences=sentences.length;
    String[][] tokens = new String[noOfSentences][];
    scores=new float[noOfSentences];
    positionScores=new float[noOfSentences];
    lengthScores=new float[noOfSentences];
    tfs=new float[noOfSentences];
    tss=new float[noOfSentences];
    pageRankScores=new float[noOfSentences];
    sentenceLengths=new int[noOfSentences];
    int noOfTokens=0;
    InputStream modelIn;
    TokenizerModel model;
    try {
      modelIn = getClass().getResourceAsStream("en-token.bin");
      model = new TokenizerModel(modelIn);
      Tokenizer tokenizer = new TokenizerME(model);
      int count,i,j,k,l;
      for(i=0;i<noOfSentences;i++) {
    	tokens[i] = tokenizer.tokenize(sentences[i]);
	sentenceLengths[i]=0;
	//only valid tokens
	for(k=0;k<tokens[i].length;k++) {
	  if(!isPunct(tokens[i][k])) {
	    sentenceLengths[i]++;
	  }
	}
	System.out.println("sentence "+i+": "+sentenceLengths[i]);
	totalLength+=sentenceLengths[i];
	for(j=0;j<tokens[i].length;j++) {
	  if(tokens[i][j].length() > 1) {
	    noOfTokens++;
	    if(!hm.containsKey(tokens[i][j])) {
	      hm.put(tokens[i][j],1);
	    }
	    else {
	     count=(int)Integer.parseInt(hm.get(tokens[i][j]).toString());
	     hm.put(tokens[i][j],count+1);
	    }
	  }
	}
      }
      float[][] similarityMatrix = constructSimilarityMatrix(tokens);
      if(modelIn != null) {
	modelIn.close();
      }
      for(i=0;i<noOfSentences;i++) {
	positionScores[i] = 1-(float)((float)(i+1)/(float)noOfSentences); //System.out.println("Pos score of "+i+" = "+positionScores[i]);
        lengthScores[i] = (float)sentenceLengths[i]/(float)totalLength; //System.out.println("Length score of "+i+" = "+lengthScores[i]);
	count = 0;
	for(j=0;j<tokens[i].length;j++) {
	  if(hm.containsKey(tokens[i][j])) {
	    count = count + (int)Integer.parseInt(hm.get(tokens[i][j]).toString());
	  }
	}
	tfs[i] = (float)count/(float)noOfTokens; //System.out.println("Term Freq Score of "+i+" = "+tfs[i]);
	scores[i] = positionScores[i] + lengthScores[i] + tfs[i];
      }
      for(i=0;i<noOfSentences;i++) {
	pageRankScores[i] = 0.15f*scores[i];
	for(j=0;j<noOfSentences;j++) {
	  if(i!=j) {
	    pageRankScores[i] = pageRankScores[i] + 0.85f*scores[i]*similarityMatrix[i<j?i:j][i>j?i:j];
	  }
	}
      }
    }
    catch (IOException e) {
       e.printStackTrace();
    }
    return pageRankScores;
  }
  
  public String  constructSummary(String[] sentences,float[] scores,int wordCount) {
	StringBuilder summary = new StringBuilder("");
    final Integer[] idx = new Integer[sentences.length];
    final float[] data = scores;
    int i;
    for(i=0;i<sentences.length;i++) {
      idx[i] = i;
    }
    Arrays.sort(idx, new Comparator<Integer>() {
    public int compare(final Integer o1, final Integer o2) {
        return Float.compare(data[o1], data[o2]);
    }
    });
    int currentLength = 0;
    for(i=0;i<idx.length;i++) {
    	if(currentLength >= wordCount) {
      	  break;
        }
      currentLength = currentLength + sentenceLengths[idx[i]];
      //System.out.println("Sentence "+i+": "+sentences[i]);
    }
    int[] summaryIndices = new int[i];
    for(i=0;i<summaryIndices.length;i++) {
      summaryIndices[i]=idx[i];
    }
    Arrays.sort(summaryIndices);
    for(i=0;i<summaryIndices.length;i++) {
      summary.append(sentences[summaryIndices[i]]+" ");
    }
    return summary.toString();
  }

  public String getSummary(String text, int wordCount) {
    String[] sentences=splitIntoSentences(text);
    float[] scores=scoreSentences(sentences);
    String summary=constructSummary(sentences,scores,wordCount);
    return summary;
  }
  
  public static void main(String[] args) {
    if(args.length < 1 || args.length > 2) {
      System.out.println("Usage: java Summarizer <input_file_name> <word count>(default: 100)");
      System.exit(-1);
    }
    StringBuilder text=new StringBuilder("");
    try {
      FileInputStream fstream = new FileInputStream(args[0]);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String str;
      while ((str = br.readLine()) != null) {
	text.append(str);
      }
      in.close();
    } catch (Exception e) {
      System.err.println(e);
    }
    PageRankSummarizer summarizer=new PageRankSummarizer();
    int wordCount;
    if(args.length == 2) {
      wordCount = Integer.parseInt(args[1]);
    }
    else {
      wordCount = 100; //default
    }
    String summary=summarizer.getSummary(text.toString(),wordCount);
    System.out.println(summary);
  }
}   