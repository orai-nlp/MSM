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
import java.util.List;
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

/**
 * @author inaki
 *
 * Class for language detection using optimaize library.
 */
public class LangDetect {

	private List<LanguageProfile> languageProfiles;
	private LanguageDetector languageDetector;
	//create a text object factory
	TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	
	//Pattern to trust Twitter lang identification for certain languages
	private Pattern twtLangs = Pattern.compile("(en|es|fr|de|tr)");
	//Pattern to normalize hashtags and user names
	private Pattern userhashtag = Pattern.compile("[#@]([\\p{L}\\p{M}\\p{Nd}_]+\\b)");
	//Pattern to  match urls in tweets. There are more efficient ways to do this but, for 
	//the moment this is a fast solution
	private Pattern urlPattern = Pattern.compile("([fh]t?tps?://)?[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)+(/[#&\\n\\-=?\\+\\%/\\.\\w]+)?");  
	
	public LangDetect()
	{
		try {
			//load all languages:
			languageProfiles = new LanguageProfileReader().readAllBuiltIn();			
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
	public String detectLanguage(String input)
	{
		String result = "unk";
		//query:
		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		double minProb = 0.3;
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			if (prob > 0.70 && prob > minProb)
			{
				result = l.getLocale().getLanguage();
				minProb=prob;
			}
		}
				
		return result;
	}
	
	/**
	 *   Detects the language of a given string. 
	 *   If the language is not among those languages accepted "unk" (unknown) is returned)
	 *   supposedLang parameter tells the system what languages can be in the given text, so the system 
	 *   takes this previous identification into account. If no language detected achieves the minimum 
	 *   required confidence score (0.70), the language with the highest probability is returned, 
	 *   ONLY if that language is among those in supposedLangs. 
	 * 
	 * @param input
	 * @return
	 */
	public String detectFeedLanguage(String input, String supposedLang)
	{
		String result = "unk";
		//query:
		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		double minProb = 0.3;
		String maxProbLang = "";
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			if (prob > minProb)
			{
				minProb=prob;
				maxProbLang=l.getLocale().getLanguage();
				if (prob > 0.70 ) //ask for a minimum confidence score of 0.70
				{
					result = l.getLocale().getLanguage();
				}
			}
		}
		
		if (supposedLang==null)
		{
			supposedLang="";
		}
		if (result.equals("unk") && supposedLang.contains(maxProbLang))
		{
			result = maxProbLang;
		}
		
		return result;
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
	public String detectTwtLanguage(String input, String supposedLang)
	{
		String result = "unk";
		//query:
		String detectStr = userhashtag.matcher(input).replaceAll(" $1");
		detectStr = urlPattern.matcher(detectStr).replaceAll("").replaceAll("\\s+", " ");
		TextObject textObject = textObjectFactory.forText(detectStr);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);		
		double minProb = 0.69; 
		//System.err.println("Utils::detectTwtLanguage -> lang for text: "+detectStr);
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectTwtLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			//System.err.println("Utils::detectTwtLanguage -> lang for text ("+langs.indexOf(l) +") -> "+l.toString());
			double prob = l.getProbability();
			if (prob > 0.70 && prob > minProb)
			{
				result = l.getLocale().getLanguage();
				minProb=prob;
			}
		}
	
		// give a chance to twitter in case of es,en,fr,tr,de because they are major languages
	    if (result.equals("unk") && twtLangs.matcher(supposedLang).find())
	    {
	        result=supposedLang;
	    }
	    
		//System.err.println("Utils::detectTwtLanguage -> final language: "+result);
		
		return result;
	}
	
}
