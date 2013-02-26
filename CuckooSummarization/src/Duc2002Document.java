public class Duc2002Document {
	String docNo;
	String topic;
	String text;
	String[] sentences;
	int[] sentenceWordCounts;
	
	//getters
	public String getDocNo()
	{
		return docNo;
	}
	public String getTopic()
	{
		return topic;
	}
	public String getText()
	{
		return text;
	}
	public String[] getSentences()
	{
		return sentences;
	}
	public int[] getSentenceWordCounts()
	{
		return sentenceWordCounts;
	}
	
	//setters
	public void setDocNo(String docNo)
	{
		this.docNo=docNo;
	}
	public void setTopic(String topic)
	{
		this.topic=topic;
	}
	public void setText(String text)
	{
		this.text=text;
	}
	public void setSentences(String[] sentences)
	{
		this.sentences=sentences;
	}
	public void setSentenceWordCounts(int[] sentenceWordCounts)
	{
		this.sentenceWordCounts=sentenceWordCounts;
	}
}