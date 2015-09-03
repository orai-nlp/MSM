package elh.eus.MSM;



public class InfluenceTagger {

	private Properties params = new Properties();	

	public InfluenceTagger (String config, boolean db)
	{
		params.load(new FileInputStream(new File(config)));
	}
	
	
	/** twitter
     * Key Rate Limits : - 10 Calls per second | 20,000 Calls per day
     * Key = f448qamftnpuy3c2a9j8m3az
     */
	private double KloutIndex (String userId, String key) 
	{
         // get klout id          
         JSONObject json=get("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+key);
         // if json is empty return influence 0.
         if (!json)
         {
             return 0;
         }      
         my $decoded_json = decode_json( $json );
         my $kloutid=$decoded_json->{'id'};
         // get klout score. Klout scroe range is [1..100]
         $json=get("http://api.klout.com/v2/user.json/"+kloutId"/score?key="+key);
         $decoded_json = decode_json( $json );
         double kloutscore=$decoded_json->{'score'};
         //print STDERR "$src - $srcType - $kloutscore\n";
         return kloutscore; 

	}
	
	
	
	/** websites - PageRank
     * Key Rate Limits : - 10 Calls per second | 20,000 Calls per day
     * Key = f448qamftnpuy3c2a9j8m3az
     */
	private double PageRankIndex (String id) 
	{
         // get klout id          
         JSONObject json=get("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+key);
         // if json is empty return influence 0.
         if (!json)
         {
             return 0;
         }      
         my $decoded_json = decode_json( $json );
         my $kloutid=$decoded_json->{'id'};
         // get klout score. Klout scroe range is [1..100]
         $json=get("http://api.klout.com/v2/user.json/"+kloutId"/score?key="+key);
         $decoded_json = decode_json( $json );
         double kloutscore=$decoded_json->{'score'};
         //print STDERR "$src - $srcType - $kloutscore\n";
         return kloutscore; 

	}
	
}

