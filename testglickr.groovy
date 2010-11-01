def gf = new GlickrAPI()


//will add more test cases

//to test uploader, make sure to have a valid authtoken along with a secret and api key
gf.uploadPicToFlickr("<path to your image>",[title:"jvazquez glickr Program Trial 3", tags:"glickr cool api flickr test", safety_level:1,is_family:1,is_friend:0,is_public:0])
println "DONE!"