import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class CuckooSummarizer { 

	int[] sentenceLengths;
	int totalLength=0; //No of tokens
	float[][] similarityMatrix;

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
				//System.out.println("sentence "+i+": "+sentenceLengths[i]);
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
			similarityMatrix = constructSimilarityMatrix(tokens);
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
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return scores;
	}

	public int getSentenceWordCount(String sentence) throws Exception {
		InputStream modelIn = getClass().getResourceAsStream("en-token.bin");
		TokenizerModel model = new TokenizerModel(modelIn);
		Tokenizer tokenizer = new TokenizerME(model);
		String[] tokens = tokenizer.tokenize(sentence);
		int wordCount=0;
		for(int i=0;i<tokens.length;i++) {
			if(!isPunct(tokens[i])) {
				wordCount++;
			}
		}
		return wordCount;
	}

	public float getAverageSentenceWordCount(String sentences[]) throws Exception {
		int wordCount=0;
		for(int i=0;i<sentences.length;i++) {
			wordCount=wordCount+this.getSentenceWordCount(sentences[i]);
		}
		return ((float)wordCount/sentences.length);
	}

	public float calculateFitnessFunction(int[] candidateSummary, String sentences[], float[] scores) {
		float d=0.15f;
		float fitness=0;
		for(int i=0;i<candidateSummary.length;i++) {
			fitness+=d*scores[candidateSummary[i]];
			for(int j=0;j<candidateSummary.length;j++) {
				if(candidateSummary[i]!=candidateSummary[j]) {
					fitness+=(1-d)*scores[candidateSummary[i]]*similarityMatrix[candidateSummary[i]<candidateSummary[j]?candidateSummary[i]:candidateSummary[j]][candidateSummary[i]>candidateSummary[j]?candidateSummary[i]:candidateSummary[j]];
				}
			}
		}
		return fitness;
	}

	public String  constructSummary(String[] sentences,float[] scores,int wordCount) throws Exception {
		float p=0.40f;
		float avgWordCount=this.getAverageSentenceWordCount(sentences);
		int noOfSentences=sentences.length;
		int noOfSummarySentences=(int)(Math.ceil((double)wordCount/avgWordCount));
		int[] candidateSummary = new int[noOfSummarySentences];
		//Initialization of candidate summary with randomly picked sentences (initial set of eggs)
		boolean c;
		for(int i=0;i<noOfSummarySentences;i++) {
			c=false;
			while(!c) {
				candidateSummary[i]=(int)(Math.random()*noOfSentences);
				c=true;
				for(int j=0;j<i;j++) {
					if(candidateSummary[i]==candidateSummary[j]) {
						c=false;
					}
				}
			}
		}
		//Generate new candidate summary sentence (new egg)
		int currNoOfIterations=0,noOfIterations=50;
		while(currNoOfIterations++<noOfIterations) {
			System.out.println("\nIteration No "+currNoOfIterations);
			System.out.println("------------------------");
			System.out.println("Candidate Summary Indices: ");
			for(int i=0;i<noOfSummarySentences;i++) {
				System.out.print(candidateSummary[i]+" ");
			}
			float fitness=calculateFitnessFunction(candidateSummary, sentences, scores);
			int newSummarySentence=0;
			c=false;
			while(!c) {
				newSummarySentence=(int)(Math.random()*noOfSentences);
				c=true;
				for(int i=0;i<noOfSummarySentences;i++) {
					if(candidateSummary[i]==newSummarySentence)
						c=false;
				}
			}
			System.out.println("\nNew Candidate Summary Sentence Index: "+newSummarySentence);
			//Randomly select a sentence (egg) for replacement
			int[] newCandidateSummary=candidateSummary;
			int replacement=(int)(Math.random()*noOfSummarySentences);
			System.out.println("Replacement: "+candidateSummary[replacement]);
			newCandidateSummary[replacement]=newSummarySentence;
			System.out.println("New Candidate Summary Indices: ");
			for(int i=0;i<noOfSummarySentences;i++) {
				System.out.print(newCandidateSummary[i]+" ");
			}
			System.out.println("Current Fitness: "+fitness);
			float newFitness=calculateFitnessFunction(newCandidateSummary, sentences, scores);
			System.out.println("New Fitness: "+newFitness);
			if(newFitness>fitness) {
				candidateSummary=newCandidateSummary;
				System.out.println("\nNew Fitness is better");
			}
			else {
				System.out.println("\nNew Fitness is worse");
			}
			System.out.println("After Fitness comparison, Candidate Summary Indices: ");
			for(int i=0;i<noOfSummarySentences;i++) {
				System.out.print(candidateSummary[i]+" ");
			}
			//Calculate fitness values of all sentences in order to prepare for abandonment of worse eggs
			//Fitness value of one sentence is inversely proportional to the fitness of the rest of the sentences as a whole
			float fitnessValues[]=new float[noOfSummarySentences];
			for(int i=0;i<noOfSummarySentences;i++) {
				int[] leaveIOutSummary=new int[noOfSummarySentences-1];
				int j=0;
				for(int k=0;k<noOfSummarySentences;k++) {
					if(k!=i)
						leaveIOutSummary[j++]=candidateSummary[k];
				}
				fitnessValues[i]=1/calculateFitnessFunction(leaveIOutSummary, sentences, scores);
			}
			//Abandon p fraction of worse eggs
			int noOfAbandonments=(int)(p*noOfSummarySentences);
			final Integer[] idx = new Integer[noOfSummarySentences];
			for(int i=0;i<noOfSummarySentences;i++) {
				idx[i]=candidateSummary[i];
			}
			final float[] data = fitnessValues;
			for(int i=0;i<noOfSummarySentences;i++) {
				idx[i] = i;
			}
			Arrays.sort(idx, new Comparator<Integer>() {
				public int compare(final Integer o1, final Integer o2) {
					return Float.compare(data[o1], data[o2]);
				}
			});
			int[] sentencesToAbandon=new int[noOfAbandonments];
			System.out.println("\nSentences to Abandon: ");
			for(int i=0;i<noOfAbandonments;i++) {
				sentencesToAbandon[i]=idx[i];
				System.out.print(candidateSummary[sentencesToAbandon[i]]+" ");
			}
			for(int i=0;i<noOfAbandonments;i++) {
				int newSummarySentence2=0;
				while(true) {
					newSummarySentence2=(int)(Math.random()*noOfSentences);
					c=true;
					for(int j=0;j<noOfSummarySentences;j++) {
						if(candidateSummary[j]==newSummarySentence2)
							c=false;
					}
					if(c)
						break;
				}
				candidateSummary[sentencesToAbandon[i]]=newSummarySentence2;
			}
		}
		
		//print indices of summary
		System.out.println("\nSummary Indices: ");
		for(int i=0;i<candidateSummary.length;i++) {
			System.out.print(candidateSummary[i]+" ");
		}
		
		//construct summary
		StringBuilder summary = new StringBuilder("");
		Arrays.sort(candidateSummary);
		for(int i=0;i<noOfSummarySentences;i++) {
			summary.append(sentences[candidateSummary[i]]+" ");
		}
		return summary.toString();
	}

	public String getSummary(String text, int wordCount) throws Exception {
		String[] sentences=splitIntoSentences(text);
		float[] initialScores=scoreSentences(sentences);
		String summary=constructSummary(sentences,initialScores,wordCount);
		return summary;
	}

	public static void main(String[] args) throws Exception {
		CuckooSummarizer s=new CuckooSummarizer();
		System.out.println(s.getSummary(" The motion picture industry's most coveted award, Oscar, was created 60 years ago and 1,816 of the statuettes have been produced so far. Weighing 8 pounds and standing 13 inches tall, Oscar was created by Metro-Goldwyn-Mayer studios art director Cedric Gibbons, who went on to win 11 of the trophies. Oscar, manufactured by the R.S. Owens Co., Chicago, is made of Britannia metal, copper plate, nickel plate and gold plate. From 1942 to 1944, the trophy was made of plaster, but winners were later presented with the real thing. According to the Academy of Motion Pictures Arts and Sciences, the only engraving mistake was in 1938 when the best actor trophy given to Spencer Tracy for ``Boy's Town'' read: ``Best Actor: Dick Tracy''. The Academy holds all the rights on the statue and ``reserves the right to buy back an Oscar before someone takes it to a pawn shop,'' said Academy spokesman Bob Werden. The most-nominated film was ``All About Eve'' in 1950. It got 14 nominations. ``Ben-Hur'' in 1959 was the most-awarded film with 11, and Walt Disney was the most-awarded person with 32 .", 100));
		/*if(args.length < 1 || args.length > 2) {
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
		CuckooSummarizer summarizer=new CuckooSummarizer();
		int wordCount;
		if(args.length == 2) {
			wordCount = Integer.parseInt(args[1]);
		}
		else {
			wordCount = 100; //default
		}
		String summary=summarizer.getSummary(text.toString(),wordCount);
		System.out.println(summary);*/
	}
}   