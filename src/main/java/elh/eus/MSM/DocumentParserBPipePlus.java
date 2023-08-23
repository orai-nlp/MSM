package elh.eus.MSM;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLHighlighter;
import de.l3s.boilerpipe.sax.ImageExtractor;
import de.l3s.boilerpipe.document.Image;

import org.apache.commons.text.StringEscapeUtils; 

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;

public class DocumentParserBPipePlus {
	
	/** arrays containing css selectors for jsoup, in order to detect metadata for an article.*/
	private final String[] metaDesc = {
			"meta[property=og:description]",
			"meta[property=twitter:description]",
			"meta[name=DC.Description]",
			"meta[name=Description]",
			
	};
	private final String[] metaTitle = {"meta[property=og:title]","meta[name=DC.Title]","meta[property=twitter:description]"};
	private final String[] metaImg = {"div[class~=irudia(-esk)?]","meta[property~=(og|twitter):image$]","meta[name~=(og|twitter):image$]"};//,"meta[property~=(og|twitter):image]","meta[name=DC.Image]","meta[name=shareaholic:image]"};
	private final String[] metaLang = {"meta[property=og:locale]","meta[http-equiv=content-language]","meta[name=DC.Language]","meta[name=shareaholic:language]"};
	private final String[] metaDate = {"meta[property=article:published_time]"};
	private final String[] langs = {"html[lang]","html[xml:lang]",};
	private final String[] categoryTags = {
			"meta[name=DC.Keywords]",
			"meta[name=article:tag]",
			"meta[property=article:section]",
			"div[class*=subject or class*=etiketa]",
			"span[class=subject]", //Hitza (guztiek ez dute erabiltzen) - zarauzkohitza, karkara, plaentxia, uriola, erran,...			
			"p[class~=^article-(saila|zintiloa)$]", //berria
			"div[class~=(kanala-goiburua|etiketa-nagusiak)]", //Argia
			"span[class=meta-cats]", //noaua
			"div[class*=bdc-f2]", //Diario de navarra
			"div[class=Migas]", //Grupo noticias
			"span[class=section]", //Naiz - Arriskutsua!!
			"div[class=topic]",
			"div[class=Migas]", //grupo noticias
			"nav[class=mh-breadcrumb]", //pamplonaActual
			"p[class=iniciativa]",// eitb
			"p[class=antetitulo]"// eitb azpikategoria

			//"div[class=albiste_herria]" //kronika.eus - kategoria tematikorik ez, albisteak herriaren arabera banatzen dira.
	};
	private final String[] authorTags = {
			"meta[name=DC.Creator.PersonalName]",
			//"meta[name=author]", // Diario de navarra, arriskutsua askotan medioaren izena datorrelako ez artikuluaren egilearena.  
			"div[class*=egileak]",
			"span[class*=egileak]",
			"div[id=author-link]",
			"div[class=article-controls]",
			"span[class=authorname]",
			"span[class*=author-name]", //plazanueva
			"span[class=signature]", //la tribuna del pais vasco
			"span[class=post-author]",
			"span[class=author]", //naiz
			"div[class=author__name]", //naiz
			"span[class=egilea]", //zuzeu			
			"span[class*=meta-author]", //noaua //pamplonaActual
			"span[class=firm]", //Diario de Navarra
			"p[class=FuenteFecha]",// Grupo noticias
			"p[class=article-egilea] span[itemprop=name]",// Berria
			"span[itemprop=author]",
			"author", //el Correo, Diario Vasco - html5
			"div[class=voc-author-info]", //el Correo, Diario Vasco
			"p[class=autor]",// eitb,
			"span[class=vcard]", //zinea.eus
			"[class=createdby][itemprop=author] span[itemprop=name]", //barren.eus
			"span[class='author vcard']", //gasteiz hoy
			"div[id=author]", //gaztezulo
			"div.metas > div[class=autor]"// hitzak			

	};
	private final String[] headers = {"header",
			"div[id~=(desktop|mobile)[_-]header]",
			"div[class~=(naiz)-header]",
			"div[class~=Cabecera]", //grupo noticias
	};
	private final String[] footers = {"footer",
			"div[class*=footer]",
			"div[id*=footer]",
			"div[class*=erlazionatutakoak]", //Argiak tiren azpian dituen albiste erlazionatuen segidak.
			"div[class~=(?i)(share|sare-sozialak)]",
			"span[class*=socialButton]",
			"div[id=pie]", // eitb tags_relacionados
			"div[class=tags_relacionados]", // eitb erlazionatutakoak
			"div[class=machine-related-contents]", //plazanueva
			"nav[class*=footer_navbar]",
			"div[class=Pie]", // grupo noticias

	};
	private final String[] banners = {
			"div[class*=bultza-kazetaritza-independentea]", //Argia
			"div[class*=egin-argiakoa-menua]", //Argia <nav class="navbar"> elementuak ezabatu orokorrean?
			//"div[class*=article-oharra]", //arriskutsua Berrian erabiltzen da.
			"div[class*=iragarki_merkea]", //kronika.eus
			"section[id~=geroa[-_]zugan]", //berria
			//"div[id=zutEsk]", //berria
			"div[id=secondary]", //hitza (oarsoaldea) eskuinueko zutabea
			"div[id=ca_banner]", //barren.eus
			"div[class=banner]", //grupo noticias
			"div[class=modal-dialog]", //naiz, rather than banners it is various functionality modals. Modals should be always discarded?
			"div[class*=gdpr-terms]", //naiz, legal notice
			"div[class=login-required]", //naiz, suscription needed  
			"div[class*=subscription]",//Naiz, suscription 
			"div[class*=naiz-sidebar]", // Naiz, eskuin zutabea.
			"div[class*=bg-modal-onplus2]", //DV pay per view content modal
			"div[class*=container-btn-onplus2]", //DV
			"section[class*=aviso-inferior-trial]", //DV
			"div[id=prescription1-modal]", //DV
			"div[class=voc-alerts-content]",//DV
			"div[class*=voc-cookie-message]", //DV cookies
			"div[class*=voc-newsletter]",//DV newsletter
			"div[class*=voc-aside-margin]",//DV eskuin zutabea
			"div[class=zona_derecha]", // eitb, eskuin zutabea.
			"div[id=compatibilidad_browser]", //eitb nabigatzaile bateragarritasuna.
			"aside", // pamplona actual eskuin zutabea.
			"div[class*=cdp-cookies]", // calle mayor cookies
			"div[class*=alert-cookies]", //tribuna del pais vasco
			"div[class*=tt_cookie_banner]", //navarracapital			
			"div[id*=aviso-cookie]", //Diario de Navarra
			"div[id=adblockadvice]", //tribuna del pais vasco
			"div[class=BANNER_COLUMN]", //tribuna del pais vasco, eskuin zutabea
			"div[class=COMMENT_WRAPPER]", //tribuna del pais vasco, IRUZKINAK
			"div[id=comments]", //Dato economico - Arriskutsua?
			"div[id=div_blocker]", // navarra confidendial
			"div[id=paywallWrapper]", // Diario de Navarra abMsgWrap
			"div[id=abMsgWrap]", // Diario de Navarra
			"div[class=content_bottom_right]", // navarra confidendial - eskuin zutabea
			"section[id=comments]", // el diario norte.
			//"div[class=news-latest]" // side div latest news
	};

	private final String[] dateTags = {
			//"div[class*=data]", //oso arriskutsua euskara ez beste hizkuntzetako kodea dagoenean.
			                      //e.g. Berrian "article-data" data da, baina "content-data" beste webgune batzuetan eduki nagusia da.
			"span[itemprop=datePublished]",
			"span[class=content-time]",
			"span[class~=(?i)dat[ae]2?]",
			"p[class=artikuludatuak] > span[class=data]",
			"div[class*=header-date]" //naiz
			};

	private final String[] boilerpipeCustomPartialSentenceRemove = {
			"Artikulu +hau +(\\S+) +eta +CC-by-sa +lizentziari +esker +ekarri +dugu",
	};
	private final String[] boilerpipeCustomStringsNext = {
			//"Honen bidez: Otamotz", //HITZAK
			"^Telegram$",
			"\\| Ikusi handiago \\| Argazki originala", //GUAIXE //ANBOTO #Baleike Uztarria hiruka  // euskalerriairratia //honek argazki oinak ezabatzen ditu osorik.
			"^guaixe.eus$",
			"^Uztarria.eus  \\|", //uztarria
			"^Informazio gehiago$",
			"^uriola.eus", //uriola
			"^Lagundu URIOLA ataria indartzen",
			"^erran.eus$",
			"^goiena.eus$",
			"^zarauzkohitza.eus$",
			"^anboto.org$", //ANBOTO
			"^Eposta \\(Datu hau ez da",// Aiurri
			"^hiruka.eus$",			// Hiruka
			"^egizu$", //Hiruka ?
			"^aiaraldea.eus$",
			"^maxixatzen.eus$", //maxixatzen
			"^alea.eus$", //alea
			"^karkara.eus$", //karkara
			"^Txintxarri ", //Txintxarri
			"^plaentxia.eus$", //plaentxia
			"^Xehetasunak$", //Barren
			"^barren.eus$",
			"^Sustatu.eus$", //sustatu
			"^Teknofiloen albistegia$",
			"^→egilearen albiste guztiak", //zinea
			"^aikor.eus$", //aikor
			"^Embed kodea:", //zientzia.eus
			"^\\[JAKIN\\]$",// Jakin
			"^JAKIN$",// Jakin
			"Itzuli Hasierako orrira", //Otamotz
			"Harpidetu RSS jariora",
			"Deja un comentario", //Calle Mayor
			"Has accedido a una noticia de pago y has consumido un clic de tu cuenta personal", //naiz
			"Ordaindu beharreko albiste batean sartu zara eta zure kontu pertsonaleko klik bat kontsumitu duzu", //naiz
			"(i?)^x$",
			"^This text will be replaced", //28 kanala txertatutako bideoaren ordez agertzen dela dirudi.
			"^Loading player...$", //naiz, txertatutako bideoak.
			"^Más información$", //DV
			"^MÃ¡s informaciÃ³n$", //DV
			"^He visto un error$", //eldiario.es
			"^Comentarios$", //DN
			"^A+ A-$", //DN
			"^Etiquetas$" //DN
	};
	
	
	private final String[] boilerpipeCustomStringsLast = {
			"^Etiketak$",
			"^Goierriko Hedabideak S.L.",
			"^Send$",
			"^Elkarbanatu ïŒŒElkarbanat",
			"^Cookie politika",
			"^Getariako triatloia 2017 â€“ Urola Triatloi Kluba$",
			//"^Â© Urola Kostako Hedabideak",
			"^Ez dago iruzkinik",
			"^Webgune honek bat egiten du Creative",
			"^HONEKIN[ ]+LOTUTA",			
			//"^Â© Lea-Artibaialdeko Hedabideak",
			"^â€¢ Lege Oharra",
			"^Codesyntaxek garatua",
			"^Erantzuteko, izena emanda egon",
			"^Zabaldu artikulua:", // #Erran  //GUAIXE //#Baleike //uztarria //uriola
			//"^Â© Oiartzualdeko Hedabideak S.L",
			"^DONOSTIAKO HEDABIDEAK S.L \\u2022 Webgune",//BERRIA
			"^Irakurle agurgarria:",
			"^Lagun agurgarria:",
			"^LAGUN AGURGARRIA:",
			"^Publizitatea",
			"^Webgune honek cookie-ak erabiltzen ditu",
			"^Martin Ugalde Kultur Parkea, Andoain 20140",
			"^Jaso egunero ARGIAko albisteen laburpena e-postan:",//Argia
			"^ARGIAn egiten dugun kazetaritza independenteak bultzada merezi duela uste duzu?",//Argia
			"^Informazio askea lantzen dugu ARGIAn, langileok gara proiektuaren jabeak eta",
			"^ALEA da Arabako euskarazko aldizkari bakarra, eta zu bezalako irakurleen babesa behar du aurrera egiteko", //Alea
			"^Kanal hauetan artxibatua:",
			"^Iruzkina idatzi$",
			"^Erantzun$",
			"^Bidali:",						
			"^Zure e-posta helbidea ez da argitaratuko", //Noaua
			"^Harpidetu$",
			"^HARPIDETU$", //Argia
			"^Informazio gehiago:$",
			"^Esteka fitxa berri baten ireki",
			"^Twitter$",
			"^Zabaldu albistea:", //Aiurri
			"^Iruzkina$",
			"^Albiste honi komentario bat gehitu", //Barren
			"Lotura duten albisteak",			
			"^Fitxategia deskargatu", //zientzia.eus
			"\\/5 rating",
			"^Etiketak$",
			"^Zure e-posta helbidea ez da argitaratuko",
			"^Loturak:$",// Hikhasi
			"^Harpidetu buletinean\\!$",
			"^Te recomendamos que antes de comentar, leas las normas de participación de Diario de Navarra$", //DN
			"^KLIK BAT EGUNERO", //Naiz
	};
	
	
	private Set<Pattern> bpipePatternsNext = new HashSet<Pattern>();
	private Set<Pattern> bpipePatternsLast = new HashSet<Pattern>();
	
	private Set<String> postbpipePatterns = new HashSet<String>();

	private static final Pattern wspace = Pattern.compile("^\\s*$");
	
	private SyndEntry feedEntry; 
	private String feedAcceptedLangs;
	private HTMLDocument input;
	private Document jsoupDoc;
	private Document jsoupDocOrig;

	
	private final BoilerpipeExtractor bpExtractor=new ArticleExtractor();
	private final ImageExtractor bpImgExtractor=ImageExtractor.INSTANCE;

	private final HTMLHighlighter bpHighlighter=HTMLHighlighter.newHighlightingInstance();
	private final HTMLHighlighter bpExtracter=HTMLHighlighter.newExtractingInstance();
	private static final String[] newlineTags = {"h1","h2","h3","h4","h5","h6","p"};
	private static final String[] formattingTags = {"em","b","strong","i"};

	private LangDetect LID = new LangDetect();
	
	public HTMLDocument getInput() {
		return input;
	}
	
	public void setInput(HTMLDocument input) {
		this.input = input;
	}
	
	public String getFeedLangs() {
		return feedAcceptedLangs;
	}

	public void setFeedLangs(String langs) {
		this.feedAcceptedLangs = langs;
	}

	
	public SyndEntry getFeedEntry() {
		return feedEntry;
	}
	public void setFeedEntry(SyndEntry feedEntry) {
		this.feedEntry = feedEntry;
	}
	public Document getJsoupDoc() {
		return jsoupDoc;
	}
	public Document getJsoupDocOrig() {
		return jsoupDocOrig;
	}
	public void setJsoupDoc(Document jsoupDoc) {
		this.jsoupDoc = jsoupDoc;
	}
	public void setJsoupDocOrig(Document jsoupDoc) {
		this.jsoupDocOrig = jsoupDoc;
	}
	public Set<String> getPostbpipePatterns() {
		return postbpipePatterns;
	}

	public void setPostbpipePatterns(Set<String> postbpipePatterns) {
		this.postbpipePatterns = postbpipePatterns;
	}
	
	/**
	 * Constructor
	 */
	public DocumentParserBPipePlus () {
		bpipePatternsNext = MSMUtils.constructPatterns(boilerpipeCustomStringsNext);
		bpipePatternsLast = MSMUtils.constructPatterns(boilerpipeCustomStringsLast);
	}
	
	
	/**
	 * 
	 * Parses a text based on the html document.
	 *  
	 * @param in
	 * @param link
	 * @param published
	 *
	 * @return
	 */
	public FeedArticle parseText(HTMLDocument in, SyndEntry fentry, Date published, String feedLangs) {
		setInput(in);
		setJsoupDoc(Jsoup.parse(new String(input.getData(), input.getCharset())));
		setJsoupDocOrig(getJsoupDoc());
		setFeedEntry(fentry);
		setFeedLangs(feedLangs);
		
		return fullExtraction(fentry.getLink(),published);		
	}
	
	
	/**
	 * 
	 * Parses a text based on the html document.
	 *  span class="x-boilerpipe-mark1"
	 * @param in
	 * @param link
	 * @param published
	 *
	 * @return
	 */
	public FeedArticle parseText(HTMLDocument in, String inputUrl) {
		setInput(in);
		//System.err.println("downloaded html:");
		//System.err.println(input.getCharset());
		//System.err.println(new String(input.getData(), input.getCharset()));
		setJsoupDoc(Jsoup.parse(new String(input.getData(), input.getCharset())));
		setJsoupDocOrig(getJsoupDoc());
		//System.err.println("DocumentParser::parseText -> \n"+getJsoupDocOrig()); 
		return fullExtraction(inputUrl,new Date());		
	}
	
	
	/**
	 * @param published 
	 * @param string 
	 * @return
	 */
	private FeedArticle fullExtraction(String url, Date publishdate ) {
		
		TextDocument doc;
		FeedArticle article = new FeedArticle();
		article.setPubDate(publishdate);
		article.setUrl(url);
		// WARNING: xpath based expressions containing "meta" tags are deleted by boilerpipe, so in order to find those, the original html should be parsed.
		//          as of 2022/08/04 this affects to extractAuthor, extractCategories and extractImages functions
		try {
			
			//normal kohlschuetter extractor call
			// parse the document into boilerpipe's internal data structure
			doc = new BoilerpipeSAXInput(getInput().toInputSource()).getTextDocument();
			
			// perform the extraction/classification process on "doc" if no boilerpipe patterns are present
			if (postbpipePatterns.isEmpty()) {	
				System.err.println("DocumentParser::fullExtraction ->  No Boilerplate patterns, boilerpipe default extractor will be used");
				bpExtractor.process(doc);	
				String highlighter = bpHighlighter.process(doc, getInput().toInputSource());
				//System.err.println("DocumentParser::fullExtraction -> boilerpipe result: \n"+highlighter);
				setJsoupDoc(Jsoup.parse(highlighter));
				
				//author extraction, author containing elements are removed from DOM.
				article.setAuthor(MSMUtils.JSONcleanEntitiesUnicode(extractAuthor(false)));

			}
			else {
				bpExtractor.process(doc);
				String highlighter = bpHighlighter.process(doc, getInput().toInputSource());
				//Charset chrset = getJsoupDoc().charset();
				//PrintWriter writer = new PrintWriter("kk.html", "UTF-8");
				//writer.println(highlighter);
				//writer.close();
				//System.err.println("DocumentParser::fullExtraction -> boilerpipe result: \n"+highlighter);
				setJsoupDoc(Jsoup.parse(highlighter));
				
				//author extraction, author containing elements are removed from DOM.
				article.setAuthor(MSMUtils.JSONcleanEntitiesUnicode(extractAuthor(false)));

				cleanFromBody();
			}
			
			
			//categories or tags extraction, containing elements are removed from DOM.
			article.setCategories(extractTags());
			//title
			article.setTitle(MSMUtils.JSONcleanEntitiesUnicode(extractTitle(doc)));
			//description
			article.setDescription(MSMUtils.JSONcleanEntitiesUnicode(extractDescription()));
			//text
			String boilerpipeResult="";
			if (postbpipePatterns.isEmpty()) {
				boilerpipeResult = doc.getText(true, false);
				//System.err.println("full extraction: \n"+boilerpipeResult);
			}
			else {
				Elements textSpans = jsoupDoc.select("span[class=x-boilerpipe-mark1]");
				StringBuilder sb = new StringBuilder();
				Set<Element> processed = new HashSet<Element>();
				String lastText="dummySentenceThatShouldNeverMatch";
				for (Element t: textSpans) {					
					Element p = t.parent();
					int tSiblingIndex= t.elementSiblingIndex();
					if (Arrays.asList(formattingTags).contains(p.tagName())) {
						tSiblingIndex= p.elementSiblingIndex();
						p=p.parent();
					}
					int childNumber = p.childNodeSize();
									
					sb.append(t.wholeText());
					if ((p.isBlock() && tSiblingIndex==childNumber-1) || (p.tagName().equalsIgnoreCase("span") && p.nextElementSibling() != null )) {//(Arrays.asList(newlineTags).contains(p.tagName())) {
						sb.append("\n");
					}
			
					//if ((tSiblingIndex == childNumber-1) && (Arrays.asList(newlineTags).contains(p.tagName()))) {
					//	sb.append("\n");											
					//}
					//sb.append(" ");					
				}
				boilerpipeResult = sb.toString();
			}
			//Additional boilerplate cleaning based on per feed xpath pattern elements
			// TO BE DONE
			String body = deleteHtmlTags(deleteBodyContent(boilerpipeResult,article));			
			article.setText(MSMUtils.JSONcleanEntitiesUnicode(body));
			//language
			article.setLang(extractLang(doc));
			//image
			List<Image> imgUrls=new ArrayList<Image>();
			try {
				imgUrls = bpImgExtractor.process(new URL(url), bpExtractor);
				// automatically sorts them by decreasing area, i.e. most probable true positives come first
				 Collections.sort(imgUrls);
			} catch (IOException ue) {
				// TODO Auto-generated catch block	
				System.err.println("DocumentParser::process ->  Boilerpipe could not extract images"+article.getUrl()+" : "+ue.getMessage());
			}

			String domain= ""; 
			try {
				URI url_uri= new URI(url);
				domain = url_uri.getHost();
			}catch (URISyntaxException urie) {
				System.err.println("DocumentParser::fullExtraction ->  error when extracting domain for image extraction: "+article.getUrl()+" : "+urie.getMessage());
			}
			article.setImages(extractImages(doc, imgUrls,domain));
			
			//date should only be extracted 
			/*String html_date=extractPublishDate(doc);
			if (! html_date.equalsIgnoreCase("")) {
				article.setPubDate(Utils.parseDate(html_date));				
			}*/
			
			return article;
			
		} catch (BoilerpipeProcessingException | SAXException be) {
			be.printStackTrace();
			System.err.println("DocumentParser::process ->  Boilerplate removal ERROR with"+article.getUrl()+" : "+be.getMessage());
			return null;
		} /*catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("DocumentParser::process ->  Boilerplate removal ERROR (file write) with"+article.getUrl()+" : "+e.getMessage());
			return null;
		} catch (UnsupportedEncodingException e) {
			System.err.println("DocumentParser::process ->  Boilerplate removal ERROR (file write) with"+article.getUrl()+" : "+e.getMessage());
			e.printStackTrace();
			return null;
		}*/
		
	}
	
	
	/**
	 * Void deleting elements from DOM tree based on xpath expressions.
	 * This function is applied over the output of boilerpipe, in order to clean boilerplate that boilerpipe failed to clean. 
	 * xpaths for boilerpipe elements are extracted externally.  
	 * 
	 * @param tagsToClean
	 */
	private void cleanFromBody() {
		for (Element e :jsoupDoc.select("span[class=x-boilerpipe-mark1]")) {
		                //System.err.println("element found: "+e.cssSelector());
				String e_xpath="";
				try {
					e_xpath= xpath_soup(e);
				} catch (org.jsoup.select.Selector.SelectorParseException se) {
					System.err.print("DocumentParserBPipePlus::CleanFromBody: error when trying to get xpath - current element SKIPPED:\n"+e.text());
					continue;
				}	
				//System.err.print("element xpath: "+e_xpath+"\n"+e.text());
				if (postbpipePatterns.contains(e_xpath)){
					e.remove();
					//System.err.println("REMOVED element xpath: "+e_xpath+"\n"+e.text()+"\n");
				}
			}
				
		setInput(new HTMLDocument(jsoupDoc.outerHtml()));
	}
	
	
	/**
	 * Function for extracting author information the original html.
	 * It is normally applied before feeding the html to boilerpipe, since it often fails to clean it.  
	 * 
	 */
	private String extractAuthor(boolean remove) {
		StringBuilder authsb = new StringBuilder();
		boolean meta = false;
		for (String fstr: authorTags) {
			if (fstr.startsWith("meta")){
				for (Element e: jsoupDoc.select(fstr)) {
					authsb.append(e.text()).append(",");
				}
				if (authsb.length() > 0) {
					meta=true;
				}
			}
			else {
				Elements selection = jsoupDoc.select(fstr);				
				if (selection.size()>0) {
					Element e = selection.first();
					//System.err.println(selection.first().outerHtml());
					if (!meta) {
						Elements a = e.select("a[rel=author]");
						if (a.size()>0 && !fstr.contains("author-link")) {
							authsb.append(a.first().text());
						}						
						else if (fstr.contains("author-link")){ //zinea.eus
							authsb.append(e.text().replaceAll("→egilearen albiste guztiak ikusi", "").trim());
						}
						else {
							authsb.append(e.text());
						}
					}
					//Remove the element from DOM
					if (remove) {
						e.remove();  						
					}
					break;
				}
			}
		}
		
		//setInput(new HTMLDocument(jsoupDoc.outerHtml()));
		
		return authsb.toString().replaceAll(",$", "");
	}
	
	
	
	/**
	 * 
	 * Extract title from document. Strategy:
	 *    1 - Try to find meta element [property=og:description].
	 *    2 - Get description from rome tools extracted description.
	 *
	 * @param doc
	 * @return
	 */
	private String extractTitle(TextDocument doc) {
		String result="";
		//1. option: html defined title meta property 
		Elements selection = findMetaElements(metaTitle);
		if (! selection.isEmpty()) {
			result= StringEscapeUtils.unescapeHtml4(selection.first().attr("content"));			
		}
		//2. option: rome tools feed entry title info:
		if (result.equalsIgnoreCase("") && getFeedEntry()!= null) {
			result=getFeedEntry().getTitle();
			result = result.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n").replaceAll("(?i)<br\\/?>","\n");							
		}
		//3. boilerpipe extracted title.
		if  (result.equalsIgnoreCase("")) {
			result = doc.getTitle();
		}
		
		//some previous functions may return null. Be sure and empty string is returned and not null, otherwise org.json won't print the property
		if (result == null ) {
			result="";
		}
		return deleteHtmlTags(result.replaceAll("[^\\S\\n]+", " "));
	}
	
		
	/**
	 * 
	 * Extract description from document. Strategy:
	 *    1 - Try to find meta element [property=og:description].
	 *    2 - Get description from rome tools extracted description.
	 *
	 * @param doc
	 * @return
	 */
	private String extractDescription() {
		String result="";
		//1. option: feed description info: 
		Elements selection = findMetaElements(metaDesc);
		if (! selection.isEmpty()) {
			result= StringEscapeUtils.unescapeHtml4(selection.first().attr("content"));
			//System.err.println("DocumentParser::extractDescription - meta: "+result);
		}
		
		if (result.equalsIgnoreCase("") && getFeedEntry()!= null ) {
			SyndContent desc = getFeedEntry().getDescription(); 
			if (desc != null ) {
				result= desc.getValue();
			}
			result = result.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n").replaceAll("(?i)<br\\/?>","\n");							
			//System.err.println("DocumentParser::extractDescription - desc from feed: "+result);
		}

		//some previous functions may return null. Be sure and empty string is returned and not null, otherwise org.json won't print the property
		if (result == null ) {
			result="";
		}
		return deleteHtmlTags(result.replaceAll("[^\\S\\n]+", " "));
	}
	
	/**
	 * 
	 * Extract top image from document. Strategy:
	 *    1 - Try to find meta element [property=og:description].
	 * 
	 * @param doc
	 * @param imgUrls 
	 * @param domain 
	 * @return
	 */
	private Set<String> extractImages(TextDocument doc, List<Image> imgUrls, String domain) {
		Set<String> result= new HashSet<>();
		PrintWriter writer;
		try {
			writer = new PrintWriter("kk.html", "UTF-8");
			writer.println(jsoupDoc.html());
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		//1. option: xpath expression: 
		Elements selection = findMetaElements(metaImg);
		if (! selection.isEmpty()) {
			System.err.println("DocumentParserBpipePlus::extractImage - found element in img meta tag list (xpath): "+selection.outerHtml());
			
			Element imgElement = selection.first();//selectFirst("img");
			if (imgElement != null) {
				//element is meta
				if (imgElement.tagName().equalsIgnoreCase("meta")) {
					result.add(imgElement.attr("content"));					
				}
				else {
					Element inner = imgElement.selectFirst("img");
					String one_img=inner.attr("src");
					result.add(addDomain2url(one_img, domain));
				}
			    System.err.println("DocumentParserBpipePlus::extractImage - images - "+result.toString());
			}
			
		}
		
		//3. option: Boilerpipe cleaned html first img
		if (! imgUrls.isEmpty()) {
			for (Image img : imgUrls) {
				System.err.println("DocumentParserBpipePlus::extractImage - found element by boilerpipe image extractor: "+img.getSrc()+
						"\n full image: "+img);
				String one_img=img.getSrc();
				result.add(addDomain2url(one_img, domain));
				
				if (result.size() > 5) {
					break;
				}
			}
		}
		    
		
		return result;
	}
	
	
	/**
	 * Function for extracting author information the original html.
	 * It is normally applied before feeding the html to boilerpipe, since it often fails to clean it.  
	 * 
	 */
	private String extractTags() {
		StringBuilder authsb = new StringBuilder();		
		StringBuilder metaTags = new StringBuilder();
		for (String fstr: categoryTags) {
			Elements selection = jsoupDoc.select(fstr);
			if (fstr.startsWith("meta")){
				for (Element e: selection) {
					metaTags.append(e.attr("content")).append(",");	
				}
			}
			else {			
				if (selection.size()>0) {
					for (Element e: selection) {
						authsb.append(e.text().replace(">", ",")).append(",");					
						//System.err.println("DocumentParser::extractTags - str:"+fstr+" - tags: _"+authsb.toString()+"_ element:"+e.cssSelector());
						//e.remove(); //commented out because we don't want to remove the element before applying boilerpipe.
						if (fstr.contains("meta-cats")||fstr.contains("class=section")){
							break;
						}						
					}								
					break;
				}
			}
			
		}
		
		//setInput(new HTMLDocument(jsoupDoc.outerHtml()));
		
		String tags = authsb.toString();
		if (tags.equalsIgnoreCase("") && metaTags.length()>0){
			tags = metaTags.toString();
		}
		return tags.replaceAll(",$", "").replaceAll("^Inicio", "").replaceAll(",,", ",").trim().replaceAll("^,", "");
	}
	
	/**
	 * 
	 * Extract language of the document. Strategy:
	 *    1 - Try to find meta element .
	 * 
	 * @param doc
	 * @return
	 */
	private String extractLang(TextDocument doc) {
		String result="";
		//1. option: xpath expression: 
		Elements selection = findMetaElements(metaLang);
		if (! selection.isEmpty()) {
			result= StringEscapeUtils.unescapeHtml4(selection.first().attr("content"));	
		}
		else {
			selection = findMetaElements(langs);
			if (! selection.isEmpty()) {
				result= StringEscapeUtils.unescapeHtml4(selection.first().val());
			}
		}
		if (result.contains("_"))
		{
			result=result.substring(0, 2);
		}
		System.err.println("DocumentParserBpipePlus::extractLang - lang after xpaths: "+result);

		//2. language detection
		if (result == null ) {
			result= LID.detectFeedLanguage(doc.getContent(), getFeedLangs())[0];
		}
		
		//some previous functions may return null. Be sure and empty string is returned and not null, otherwise org.json won't print the property
		if (result == null ) {
			result="";
		}
		
		
		
		return result;
	}
	
	/**
	 * 
	 * Extract language of the document. Strategy:
	 *    1 - Try to find meta element .
	 * 
	 * @param doc
	 * @return
	 */
	private String extractPublishDate(TextDocument doc) {
		String result="";
		//1. option: xpath expression: 
		Elements selection = findMetaElements(metaDate);
		if (! selection.isEmpty()) {
			result= StringEscapeUtils.unescapeHtml4(selection.first().attr("content"));	
		}
		else {
			selection = findMetaElements(dateTags);
			if (! selection.isEmpty()) {
				result= StringEscapeUtils.unescapeHtml4(selection.first().val());
			}
		}
		
		//some previous functions may return null. Be sure and empty string is returned and not null, otherwise org.json won't print the property
		if (result == null ) {
			result="";
		}
		System.err.println("extractpublishdate: "+result+"");
		return result;
	}
	
	/**
	 * 
	 *  Function to process all the expression in an array and find elements matching those expressions in the DOM generated by jsoup
	 * 
	 * @param metaExpressions
	 * @return
	 */
	private Elements findMetaElements(String[] metaExpressions) {
		Elements result= new Elements();
		for (String expr: metaExpressions) {
			result.addAll(jsoupDocOrig.select(expr));
			if (result.size() > 0) {
				break;
			}
		}	
		return result;
	}
	
	
	/**
	 * Funtzio honek boiler-pipe bidez erauzitako testua garbitzen du.
	 * Konkretuki, artikuluaren body-an aurkitzen diren patroi zehatzak betetzen
	 * dituen testu zatiak ezabatuko dira.
	 * 
	 * @param body
	 *            artikuluaren testua
	 * @return garbitutako testua
	 */
	protected String deleteBodyContent(String body, FeedArticle article) {

		StringBuilder out = new StringBuilder();

		String currentTitle = article.getTitle().replaceAll("\\.*$", "");		
		String currentDesc = article.getDescription().replaceAll("\\.*$", "");
		//System.err.println("DocumentParser::deleteBodycontent - title:"+currentTitle+"\n - desc: _"+currentDesc+"_");
		boolean title = false;
		if (wspace.matcher(currentTitle).matches()){
			title=true;
		}
		boolean desc = false;
		if (wspace.matcher(currentDesc).matches()){
			desc=true;
		}
		String[] lerroak = body.split("\n");

		for (String lerroa : lerroak) {
			boolean breakLoop = false;
		
			if (!title && lerroa.contains(currentTitle)) {
				article.setTitle(lerroa);
				title =true;
				continue;
			}
			
			if (!desc && lerroa.contains(currentDesc)) {
				article.setDescription(lerroa);
				desc=true;
				continue;
			}	
			
			for (String s: boilerpipeCustomPartialSentenceRemove) {
				lerroa=lerroa.replaceAll(s, "");
			}
			
			for (Pattern p : bpipePatternsLast)
			{
				//System.err.println("feedreader-elh::DocumentParser::deleteBodyContent - line: "+lerroa+" - pattern: "+p);				
				if (p.matcher(lerroa).find()) {
					System.err.println("feedreader-elh::DocumentParser::deleteBodyContent - found last element: "+lerroa+" - pattern: "+p);
					breakLoop=true;
					break;
				}
			}
			
			if (breakLoop) {
				break;
			}
			
			for (Pattern p : bpipePatternsNext)
			{
				if (p.matcher(lerroa).find()) {
					//System.err.println("feedreader-elh::DocumentParser::deleteBodyContent - found next element: "+lerroa+" - pattern: "+p);
					breakLoop=true;
					continue;
				}
			}
			if (breakLoop) {
				continue;
			}
			
			out.append(lerroa).append("\n");

		}
		return out.toString().replaceAll("[^\\S\\n]+", " ");

	}
	
	/**
	 * Html-ko etiketak kentzeko parametroz pasatako katean
	 * 
	 * @param sarrera katea
	 * @return katea html etiketa gabe
	 */
	protected String deleteHtmlTags(String sarrera) {
		
	    return sarrera.replaceAll("\\<.*?>","");
	}

	/**
	 * 
	 * get xpath from an element
	 * 	Generate xpath from BeautifulSoup4 element
	 *  :param element: BeautifulSoup4 element.
	 *  :type element: bs4.element.Tag or bs4.element.NavigableString
	 *  :return: xpath as string
	 *  :rtype: str    
	 *  >>> html = (
	 *  ...     '<html><head><title>title</title></head>'
	 *  ...     '<body><p>p <i>1</i></p><p>p <i>2</i></p></body></html>'
	 *  ...     )
	 *  >>> soup = bs4.BeautifulSoup(html, 'html.parser')
	 *  >>> xpath_soup(soup.html.body.p.i)
	 *  '/html/body/p[1]/i'
	 * @return
	 */
	private String xpath_soup(Element e) throws org.jsoup.select.Selector.SelectorParseException{
	    
	    ArrayList<String> components = new ArrayList<String>();
	    
	    //System.err.println("xpath_soup -> e selector: "+e.cssSelector()+"  -- "+e.tagName());

	    // highlighted by boilerpipe. Delete the last span and build its xpath 
	    //if (e.className().contains("span.x-boilerpipe-mark1")) {
	    //	e=e.parent();
	    //}
	    
	    for (Element p : e.parents()) {
	    	String p_tag = p.tagName();
	    	int index = p.elementSiblingIndex();
	    	Elements siblings = p.siblingElements();
	    	
	    	Elements tagSiblings = siblings.select(p_tag);	    	
	    	int tagCount = 0;	    		
		    
		    for (Element s: tagSiblings) {
		    	if (s.parent() == p.parent()) {
		    		tagCount = tagCount +1;
		    	}
		    }
		    
		    //System.err.println("xpath_soup -> parent siblings amount: "+tagCount+"  -- "+p.tagName()+"."+p.className());
	    	
		    int position = 1;
	    	for (int i=0; i<index; i++) { 
	    		Element s = siblings.get(i);
	    		if (s.tagName() == p_tag) {
	    			position = position+1;
	    		}
	    	}
	    	
	    	if (tagCount > 0) {
	    		components.add(p.tagName()+"["+position+"]");
	    	}
	    	else {
	    		components.add(p.tagName());
	    	}
	    }
	    
	    Collections.reverse(components);	    
	    return "/"+String.join("/", components);

	}
	
	private String addDomain2url (String url, String domain) {
		String result= url;
		if (url.startsWith("//")) {
			result="https:"+url;
		}
		else if (url.startsWith("/")) {
			result="https://"+domain+url;
		}
		
		return result;
	}
	
}
