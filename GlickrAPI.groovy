import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class GlickrAPI {
	def nsid = ""
	private String secret = "<your secret>"
	private String akey = "<your api key>"
	def authToken = ""
	private String frob = ""
	private String restString = "http://flickr.com/services/rest/?method="
	private XmlParser xParser = new XmlParser()

	def authNewUser() {
        getFrob()
		createAuthURL()
		getToken()
	}

	def constructUrl(String sig,String flickrMethod, m =[:] ){
	  String s = this.restString+flickrMethod+"&api_key="+this.akey+m.collect{k,v-> k+v}.join()            
	  if(sig){s+="&auth_token=${this.authToken}&api_sig=${sig}"}
	  return s
	}
	def mapFlickrUser(){
		
	}
	//if bool is true then this is a Authenticated call
    private getSig(String method,boolean authenticated = false){
			String sigString="${this.secret}api_key${this.akey}"
		try {
			if(authenticated){
				sigString +="auth_token${this.authToken}${method}"				
			}
			else{
			    sigString+=method
			    }
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace()
		}
		return calcMD5(sigString)
    }
    /**
     * parameters map will take the following values 
     * title (optional)
     *    The title of the photo.
     * description (optional)
     *     A description of the photo. May contain some limited HTML.
     * tags (optional)
     *     A space-seperated list of tags to apply to the photo.
     * is_public, is_friend, is_family (optional)
     *     Set to 0 for no, 1 for yes. Specifies who can view the photo.
     * safety_level (optional)
     *     Set to 1 for Safe, 2 for Moderate, or 3 for Restricted.
     * content_type (optional)
     *    Set to 1 for Photo, 2 for Screenshot, or 3 for Other.
     * hidden (optional)
     */
    def uploadPicToFlickr(String filePath, Map parameters=[:]){
    	println("parameters ${parameters.inspect()}")
    	def paramMessages=[]
    	def pLength=0
	    def sig=""
    	    if(parameters==[:]){
    			sig = calcMD5("${this.secret}api_key${this.akey}auth_token${this.authToken}")
    		}else{
    			//sort them and add them to the signature
    			def pString=""
    			parameters.keySet().toList().sort().each{key->
    				//if the key ='tags' make it into space separated string
    		         pString+= "${key}${parameters[key]}"
    				paramMessages.add("---------------------------6ec047855a60a\r\nContent-Disposition: form-data; name=\"${key}\"\r\n\r\n${parameters[key]}\r\n"
    				)
    			}
    			sig = calcMD5("${this.secret}api_key${this.akey}auth_token${this.authToken}${pString}")
    			paramMessages.each{msg->pLength += msg.length()}
    			println pLength
    			//pLength = paramMessages.sum{it.length()}
    		}
    		//String filePath="JuanGolf08.jpg"
    		File file = new File(filePath)
    		//def connection = url.openConnection()
    		HttpURLConnection conn = new URL("http://api.flickr.com/services/upload/").openConnection()
    			def fis = new FileInputStream(file)
    			byte[] imageData= new byte[(int)file.length()]
    			def offset = 0;
    			def numRead = 0;
    			while (offset < imageData.length && (numRead=fis.read(imageData, offset, imageData.length-offset)) >= 0) {
    				offset += numRead
    			}

    			// Ensure all the bytes have been read in
    			if (offset < imageData.length) {
    				throw new IOException("The file was not completely read: "+file.getName());
    			}
    			println "filesize -> $imageData.length"
    			// Close the input stream, all file contents are in the bytes variable
    			fis.close();
            def pApiKey=    "---------------------------6ec047855a60a\r\nContent-Disposition: form-data; name=\"api_key\"\r\n\r\n${this.akey}\r\n"
    		def pAuthToken ="---------------------------6ec047855a60a\r\nContent-Disposition: form-data; name=\"auth_token\"\r\n\r\n${this.authToken}\r\n"
    		def pSig =    "---------------------------6ec047855a60a\r\nContent-Disposition: form-data; name=\"api_sig\"\r\n\r\n${sig}\r\n"
    		def header1 = "---------------------------6ec047855a60a\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"${filePath}\"\r\nContent-Type: image/jpeg\r\n\r\n" 

    		// the image is sent between the header1 and footer in the multipart message.

    		def footer = "\r\n---------------------------6ec047855a60a--\r\n"
println(pApiKey.length() + " "+pAuthToken.length() + " "+pSig.length() +" "+ header1.length() +" "+ footer.length() +" "+ imageData.length + " "+pLength)  		
    		def totalLength = pApiKey.length() + pAuthToken.length() + pSig.length() + header1.length() + footer.length() + imageData.length + pLength
    		println "length is ${totalLength}"
    		conn.setRequestMethod("POST")
    		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=-------------------------6ec047855a60a")
    		conn.setRequestProperty("Host", "api.flickr.com")
    		conn.setRequestProperty("Content-Length",totalLength.toString() )
    		conn.setDoOutput(true)
     		def os = conn.getOutputStream()
     		println pApiKey
    		os.write(pApiKey.getBytes())
     		println pAuthToken
    		os.write(pAuthToken.getBytes())
        	paramMessages.each{msg->os.write(msg.getBytes())}
     		println pSig
    		os.write(pSig.getBytes())
    		
    	    os.write(header1.getBytes())
        	os.write(imageData,0 ,imageData.length)
    		os.write(footer.getBytes())
    		os.flush()
    		os.close()
    		conn.connect()
    		conn.disconnect()
    		println	conn.content.text
	
    }


	def getFrob() {
		def sig = getSig("methodflickr.auth.getFrob")
		def url = "http://flickr.com/services/rest/?method=flickr.auth.getFrob&api_key=${this.akey}&api_sig=${sig}"
		def node = new XmlParser().parse(url)
		this.frob= node['frob'].text().toString()
		return this.frob
        
	}

	/**
	 * This function will calculate an MD5 sum of String Signature and return
	 * the String value of the Hash via strBuildup
	 * 
	 * @param signature
	 * @throws NoSuchAlgorithmException
	 * @returns strBuildup
	 */
	private String calcMD5(String signature) throws NoSuchAlgorithmException {
		String strBuildup = ""
		MessageDigest md5 = MessageDigest.getInstance("MD5")
		byte[] md5summe = md5.digest(signature.getBytes())
		for (int k = 0; k < md5summe.length; k++) {
			byte b = md5summe[k]
			String temp = Integer.toHexString(b & 0xFF)
			/*
			 * toHexString has the side effect of making stuff like 0x0F only
			 * one char F(when it should be '0F') so I check the length of
			 * string
			 */
			if (temp.length() < 2) {temp = "0" + temp}
			strBuildup += temp.toUpperCase()
		}
		return strBuildup
	}

	/**
	 * get the authentication token
	 * 
	 * @return groovy.util.Node
	 */
	def checkToken(){
		this.xParser.parse(constructUrl(getSig("methodflickr.auth.checkToken",true),
		                                        'flickr.auth.checkToken'))
	}
	/**
	 * getToken can only be used after a frob has been approved in order to get an approved authToken
	 * @return groovy.util.Node
	 */
	def getToken() {
		def sig = getSig("frob${this.frob}methodflickr.auth.getToken")
		def node = this.xParser.parse(
		        "${this.restString}flickr.auth.getToken&api_key=${this.akey}&frob=${this.frob}&api_sig=${sig}"
		        )
		//need to get the nsid and the authtoken
		this.authToken = node['auth']['token'].text()
		this.nsid = node['auth']['user'].'@nsid'.text()
		return node
	}
    /**
     * returns: the URL string
     */
	def createAuthURL() {
		return "http://flickr.com/services/auth/?api_key=${this.akey}&perms=delete&frob=${this.frob}&api_sig=" +
		        getSig("frob${this.frob}permsdelete")
	}

	/***************************************************************************
	 * AUTH
	 **************************************************************************/
	/***************************************************************************
	 * ACTIVITIES
	 **************************************************************************/
	//POLL ONLY ONCE AN HOUR!!!!!
	/**
	 * @return groovy.util.Node
	 */
	public activityGetUserComments(){
	    this.xParser.parse(constructUrl(getSig("methodflickr.activity.userComments",true),
                                	    'flickr.activity.userComments'))
	}
	/* **************************************************************************
	 * BLOGS
	 ************************************************************************* */
	/* **************************************************************************
	 * CONTACTS
	 ************************************************************************* */
	 /**
	  * @return groovy.util.Node
	  */
	 public void contactsGetList(){
	    this.xParser.parse(constructUrl(getSig("methodflickr.contacts.getList",true),
                                	    'flickr.contacts.getList'))	
	}
	/***************************************************************************
	 * FAVORITES
	 **************************************************************************/
	/***************************************************************************
	 * GROUPS
	 **************************************************************************/
	/***************************************************************************
	 * GROUP POOLS
	 **************************************************************************/

	/***************************************************************************
	 * INTERESTINGNESS
	 **************************************************************************/

    /***************************************************************************
     * PHOTOS
    *what if a return back an array of pictures?
     **************************************************************************/
    /**
    * @return groovy.util.Node
    */
    def photosGetNotInSet(){
		this.xParser.parse(constructUrl(getSig("methodflickr.photos.getNotInSet",true),
		                                'flickr.photos.getNotInSet'))
	}

    /**
     * @return groovy.util.Node
     */
	def photosGetContactsPhotos(){
	    this.xParser.parse(constructUrl(getSig("methodflickr.photos.getContactsPhotos",true),
                	                    'flickr.photos.getContactsPhotos'))
	}
	/**
	 * @return groovy.util.Node
	 */
	def photosGetRecentlyUploaded() {
	    this.xParser.parse(constructUrl(getSig("methodflickr.photos.getRecent",true),
                                        'flickr.photos.getRecent'))
	}
	/**
	 * @return groovy.util.Node
	 */
	def photosGetInfo(){
		
	}
	/* **************************************************************************
	 * PEOPLE
	 ************************************************************************* */
	/**
	 * @return groovy.util.Node
	 */
	def contactsGetList(){
		this.xParser.parse(constructUrl(getSig("methodflickr.contacts.getList",true),
		                                'flickr.contacts.getList',))		
	}
	/**
	 * @return groovy.util.Node
	 */
	def peopleGetUploadStatus(){
    	this.xParser.parse(constructUrl(getSig("methodflickr.people.getUploadStatus",true),
    	                                'flickr.people.getUploadStatus'))		
	}
	/**
	 * @return groovy.util.Node
	 */
	def peopleGetPublicPhotos() {
		this.xParser.parse(constructUrl(getSig("methodflickr.people.getPublicPhotosuser_id${this.nsid}",true),
		                                'flickr.people.getPublicPhotos',['&user_id=':"${this.nsid}"]))	
	}
	/**
	 * @return groovy.util.Node
	 */
	def peopleGetPeopleInfo() {
        this.xParser.parse(constructUrl(getSig("methodflickr.people.getInfouser_id${this.nsid}",true),
                                        'flickr.people.getInfo',['&user_id=':"${this.nsid}"]))
	}

	/********************************************
	* photosets
	************************/
	/**
	 * @return groovy.util.Node
	 */
   def photosetsGetList(String uid){
	  this.xParser.parse(constructUrl(getSig("methodflickr.photosets.getListuser_id${uid}",true),
                                     'flickr.photosets.getList',['&user_id=':"${uid}"]))	
    }
    /**
     * @return groovy.util.Node
     */
     def photosetsGetPhotos(String setID){
		this.xParser.parse(constructUrl(getSig("methodflickr.photosets.getPhotosphotoset_id${setID}",true),
                                        'flickr.photosets.getPhotos',['&photoset_id=':"${setID}"]))
     }
    /**
    Upload photos
    http://api.flickr.com/services/upload/

    */
    //get the picture when supplied the proper args
    //http://farm{farm-id}.static.flickr.com/{server-id}/{id}_{secret}_[mstb].jpg

    def getPicture(String farmId, String serverId, String photoid, String secret,String size ="s"){
    	return "http://farm${farmId}.static.flickr.com/${serverId}/${photoid}_${secret}_${size}.jpg"
    }
    def getPicture(Map map,String size){
    	return "http://farm${map['farm']}.static.flickr.com/${map['server']}/${map['id']}_${map['secret']}_${size}.jpg"
    }


}

