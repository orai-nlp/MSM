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
import java.util.Arrays;
import java.util.List;

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
	 *   supposedLang parameters tells the system that a previous identification was done, so the system 
	 *   takes this previous identification into account. 
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
		double minProb = 0.69; 
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
	public String detectLanguage(String input, String supposedLang)
	{
		String result = "unk";
		//query:
		TextObject textObject = textObjectFactory.forText(input);
		List<DetectedLanguage> langs = languageDetector.getProbabilities(textObject);
		double minProb = 0.69; 
		for (DetectedLanguage l : langs)
		{			
			//System.err.println("Utils::detectLanguage -> lang for text "+textObject+" ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			//System.err.println("Utils::detectLanguage -> lang for text ("+langs.indexOf(l) +") -> "+l.toString()+" ("+l.getLocale().getLanguage()+")");
			double prob = l.getProbability();
			if (prob > 0.70 && prob > minProb)
			{
				result = l.getLocale().getLanguage();
				minProb=prob;
			}
		}
		return result;
	}
	
}
