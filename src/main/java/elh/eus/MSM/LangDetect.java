/*
 * Copyright 2015 Elhuyar Fundazioa

This file is part of MSM.

    MSM is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MSM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MSM.  If not, see <http://www.gnu.org/licenses/>.
 */
package elh.eus.MSM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;


import com.carrotsearch.labs.langid.*;


/**
 * @author inaki
 *
 * Class for language detection using optimaize library.
 */
public class LangDetect {

	/**
	 * Optimaize estructures
	 * 		
	 */
	
	private List<LanguageProfile> languageProfiles;
	private LanguageDetector languageDetector;
	
	//create a text object factory
	TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	
	/**
	 * LangId estructures
	 * 		
	 */
	private LangIdV3 LangIdDetector;
	
	
	//Pattern to trust Twitter lang identification for certain languages
	private Pattern twtLangs = Pattern.compile("(en|es|fr|de|tr)");
	//Pattern to normalize hashtags and user names
	private Pattern hashtag = Pattern.compile("[#]([\\p{L}\\p{M}\\p{Nd}_]+\\b)");
	private Pattern user = Pattern.compile("[@]([\\p{L}\\p{M}\\p{Nd}_]+\\b)");
	//Pattern to  match urls in tweets. There are more efficient ways to do this but, for 
	//the moment this is a fast solution
	private Pattern urlPattern = Pattern.compile("([fh]t?tps?://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w]+)?");  
	
	
	//threshold to trust language detector.
	private double threshold = 0.70;
	
	//threshold to trust language detector.
	private boolean lowercase = false;
		
		
		
	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public boolean getLowercase() {
		return lowercase;
	}

	public void setLowercase(boolean lc) {
		this.lowercase = lc;
	}

	
	public LangDetect(List<String> langmodels,String alg)
	{
		if (langmodels.isEmpty()) {
			LangIdDetector = new LangIdV3();			
		}
		else {
			LangIdDetector = new LangIdV3(Model.detectOnly(new HashSet<String>(langmodels)));
		}
	}
	
	
	public LangDetect()
	{
		this(new ArrayList<String>());		
	}
	
	public LangDetect(List<String> langprofs)
	{
		try {
			if (langprofs.isEmpty()) {
				//load all languages:
				languageProfiles = new LanguageProfileReader().readAllBuiltIn();
			}
			else {
				//load only specific language profiles
				languageProfiles = new LanguageProfileReader().read(langprofs);
			}
		}
		catch (IOException ioe){
			System.err.println("Utils::detectLanguage -> Error when loading language models");
			System.exit(1);
		}
		//build language detector:
		languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
				.withProfiles(languageProfiles)
				.build();
		
		//create a text object factory
		textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	}
	
	
	
	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *    
	 * 
	 * @param input
	 * @return
	 */
	public String[] detectLanguage(String input)
	{
		return detectLanguage(input,getThreshold());
	}
	
	
	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 * 
	 * @param input
	 * @param thresh
	 * @return
	 */
	public String[] detectLanguage(String input, double thresh)
	{
		String result = "unk";
		//query:
		if (getLowercase()) {
			input = input.toLowerCase();
		}
		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		double minProb = 0.3;
		
		StringBuilder probabilitiesSb = new StringBuilder();
		probabilitiesSb.append("{");
				
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			if (prob > thresh && prob > minProb)
			{
				result = l.getLocale().getLanguage();
				minProb=prob;
			}
			
			probabilitiesSb.append(l.getLocale()).append(":").append(String.valueOf(l.getProbability())).append("; ");
		}
		probabilitiesSb.append("}");
			
		return new String[] {result, probabilitiesSb.toString()};
	}
	

	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *    
	 * 
	 * @param input
	 * @param supposedLang
	 * @return
	 */
	public String[] detectFeedLanguage(String input, String supposedLang)
	{
		return detectFeedLanguage(input,supposedLang,getThreshold());
	}
	
	
	
	/**
	 *   Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *   supposedLang parameter tells the system what languages can be in the given text, so the system 
	 *   takes this previous identification into account. If no language detected achieves the minimum 
	 *   required confidence score (0.70 default), the language with the highest probability is returned, 
	 *   ONLY if that language is among those in supposedLangs. 
	 * 
	 * @param input
	 * @param supposedLang
	 * @param thresh
	 * @return
	 */
	public String[] detectFeedLanguage(String input, String supposedLang, double thresh)
	{
		String result = "unk";
		//query:
		if (getLowercase()) {
			input = input.toLowerCase();
		}

		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		double minProb = 0.3;
		String maxProbLang = "";
		
		StringBuilder probabilitiesSb = new StringBuilder();
		probabilitiesSb.append("{");
		
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			if (prob > minProb)
			{
				minProb=prob;
				maxProbLang=l.getLocale().getLanguage();
				if (prob > thresh ) //ask for a minimum confidence score (0.7 by default)
				{
					result = l.getLocale().getLanguage();
				}
			}
			
			probabilitiesSb.append(l.getLocale()).append(":").append(String.valueOf(l.getProbability())).append("; ");
		}
		probabilitiesSb.append("}");
		
		if (supposedLang==null)
		{
			supposedLang="";
		}
		if (result.equals("unk") && supposedLang.contains(maxProbLang))
		{
			result = maxProbLang;
		}
		
		return new String[] {result, probabilitiesSb.toString()};
	}
	
	
	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *    
	 * 
	 * @param input
	 * @param supposedLang
	 * @return
	 */
	public String[] detectTwtLanguage(String input, String supposedLang)
	{
		return detectTwtLanguage(input,supposedLang,getThreshold());
	}
	
	
	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *   supposedLang parameters tells the system that a previous identification was done, so the system 
	 *   takes this previous identification into account. 
	 * 
	 * @param input
	 * @param supposedLang
	 * @return
	 * #@deprecated
	 */
	public String[] detectTwtLanguage(String input, String supposedLang, double thresh)
	{
		String result = "unk";
		//query:
		String detectStr = hashtag.matcher(input).replaceAll(" $1");
		detectStr = user.matcher(input).replaceAll(" ");
		detectStr = urlPattern.matcher(detectStr).replaceAll("").replaceAll("\\s+", " ").replaceAll("\\\\n", "\n");
		if (getLowercase()) {
			detectStr = detectStr.toLowerCase();
		}

		TextObject textObject = textObjectFactory.forText(detectStr);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);		
		double minProb = 0.69; 
		StringBuilder probabilitiesSb = new StringBuilder();
		probabilitiesSb.append("{");
		System.err.println("LangDetect::detectTwtLanguage -> lang for text: "+detectStr);
		for (DetectedLanguage l : langs)
		{			
			System.err.println("LangDetect::detectTwtLanguage -> lang ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+") - "+l.getProbability());
			//System.err.println("LangDetect::detectTwtLanguage -> lang for text ("+langs.indexOf(l) +") -> "+l.toString());
			double prob = l.getProbability();
			if (prob > thresh && prob > minProb)
			{
				result = l.getLocale().getLanguage();
				minProb=prob;
			}
			
			probabilitiesSb.append(l.getLocale()).append(":").append(String.valueOf(l.getProbability())).append("; ");
		}
	
		probabilitiesSb.append("}");
		
		// give a chance to twitter in case of es,en,fr,tr,de because they are major languages
	    if (result.equals("unk") && twtLangs.matcher(supposedLang).find())
	    {
	        result=supposedLang;
	    }
	    
		//System.err.println("LangDetect::detectTwtLanguage -> final language: "+result);
		
		return new String[] {result, probabilitiesSb.toString()};
	}
	

	/**
	 *   Main function of the class. Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *   supposedLang parameters tells the system that a previous identification was done, so the system 
	 *   takes this previous identification into account. 
	 * 
	 * @param input
	 * @param supposedLang
	 * @return
	 * #@deprecated
	 */
	public String[] detectTwtLanguageLangid(String input, String supposedLang, double thresh)
	{
		String result = "unk";
		//query:
		String detectStr = hashtag.matcher(input).replaceAll(" $1");
		detectStr = user.matcher(input).replaceAll(" ");
		detectStr = urlPattern.matcher(detectStr).replaceAll("").replaceAll("\\s+", " ").replaceAll("\\\\n", "\n");
		if (getLowercase()) {
			detectStr = detectStr.toLowerCase();
		}

		LangIdDetector.reset();
		LangIdDetector.append(detectStr);
		List<com.carrotsearch.labs.langid.DetectedLanguage> langs = LangIdDetector.rank(true);		
		double minProb = 0.69; 
		StringBuilder probabilitiesSb = new StringBuilder();
		probabilitiesSb.append("{");
		System.err.println("LangDetect::detectTwtLanguage -> lang for text: "+detectStr);
		for (com.carrotsearch.labs.langid.DetectedLanguage l : langs)
		{			
			System.err.println("LangDetect::detectTwtLanguageLangid -> lang ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.langCode+") - "+String.valueOf(l.getConfidence()));
			//System.err.println("LangDetect::detectTwtLanguage -> lang for text ("+langs.indexOf(l) +") -> "+l.toString());
			double prob = l.getConfidence();
			if (prob > thresh && prob > minProb)
			{
				result = l.getLangCode();
				minProb=prob;
			}
			
			probabilitiesSb.append(l.getLangCode()).append(":").append(String.valueOf(l.getConfidence())).append("; ");
		}
	
		probabilitiesSb.append("}");
		
		// give a chance to twitter in case of es,en,fr,tr,de because they are major languages
	    if (result.equals("unk") && twtLangs.matcher(supposedLang).find())
	    {
	        result=supposedLang;
	    }
	    
		//System.err.println("LangDetect::detectTwtLanguage -> final language: "+result);
		
		return new String[] {result, probabilitiesSb.toString()};
	}

	
	
	
	/**
	 *   return the probabilities for an input text. 
	 *   
	 *    
	 * 
	 * @param input
	 * @return
	 */
	public String probabilities(String input)
	{	
		//query:
		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (DetectedLanguage l : langs)
		{			
			System.err.println("LangDetect::probabilities -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			sb.append(l.getLocale()).append(":").append(String.valueOf(l.getProbability())).append("; ");
		}
		sb.append("}");		
		return sb.toString();
	}

	
}
