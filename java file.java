/* File Name : SFwithGithubIntegrationController 
*  Description : This controller will Create a file in selected GitHub repository.
*  Version : 1.0
*  Copyright : Accenture Technologies Copyright (c) 2018 
*  @author : Robin Jain
*  Modification Log :
*/ 
public class SFwithGithubIntegrationController {
    
    public String selectlabel {get;set;}
    public map<String,String> valuekeyPair{get;set;}
    public String shalatestCommit;
    public String shaOfBaseTree;
    public String shaOfNewTree;
    public String shaofNewCommit;
    public blob fileBody { get; set; }
    public String fname{get;set;}
    public List<String> filelist {get;set;}
    public String fileContent='';
    public String readFileContent;
    
    public SFwithGithubIntegrationController()
    {
        filelist= new List<String>();
    }
    //This method will read content of file in String
    public void readContent() { 
		filelist.add(fname);
        fileContent = EncodingUtil.base64Encode(fileBody);
        //    fileBody.toString();
		System.debug('fileContent === '+fileContent);
        getreferenceSHAofLatestCommit();
    }
    
	//This method will read all the list from selected repository  
    public List<SelectOption> getAllRepository()
    {	
        //This will get list of all available repository from GitHub
        valuekeyPair= new Map<String,String>();	
        Http http = new Http();
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/users/robinjainaccenture/repos');
        req.setMethod('GET'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        HttpResponse res = http.send(req); 
        System.debug('@@@ response');
        System.debug(res.getBody());	
        List<RepositoryListWrapper> repositoryListWrapper = (List<RepositoryListWrapper>)JSON.deserialize(res.getBody(), List<RepositoryListWrapper>.class);
        List<SelectOption> options = new List<SelectOption>();
        options.add(new selectOption('', '- None -'));
        for(RepositoryListWrapper cp : repositoryListWrapper) {
            valuekeyPair.put(cp.id, cp.name);
            options.add(new selectOption(cp.id, cp.name));
        }
        //System.debug('options ==== '+options);
        return options;
    }
    
    //This method will get reference SHA of Latest Commit
    public void getreferenceSHAofLatestCommit()
    {
        System.debug('Selected Repository === ' + valuekeyPair.get(selectlabel));
        Http http = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/'+valuekeyPair.get(selectlabel)+'/git/refs/heads/master');
        req.setMethod('GET'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        HttpResponse res = http.send(req);
        String response1 = 	res.getBody().replace('object','result');        
        GetReferenceWrapper getReferenceWrapper = (GetReferenceWrapper)JSON.deserialize(response1, GetReferenceWrapper.class);
        shalatestCommit=getReferenceWrapper.result.sha;
        getSHAofBaseTree();
    }
    
    //This method will get SHA of Base Tree
    public void getSHAofBaseTree()
    {
        System.debug('Reference SHA of Latest Commit === ' + shalatestCommit);
        Http http = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/'+valuekeyPair.get(selectlabel) + '/git/commits/'+shalatestCommit);
        req.setMethod('GET'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        HttpResponse res = http.send(req);
        //System.debug('res.getBody() -> getSHAofBaseTree ==== '+res.getBody());        
        GetSHAofBaseTreeWrapper getSHAofBaseTreeWrapper = (GetSHAofBaseTreeWrapper)JSON.deserialize(res.getBody(), GetSHAofBaseTreeWrapper.class);
        shaOfBaseTree=getSHAofBaseTreeWrapper.tree.sha;
        getSHAofNewTree();
    }
    
    //This method will get SHA of New Tree
    public void getSHAofNewTree()
    {
        System.debug('Reference SHA of Base Tree === ' + shaOfBaseTree);
        SHAofNewTreeRequest reqBody = new SHAofNewTreeRequest();       
        NewTreeRequest treeobj = new NewTreeRequest();
        treeobj.path=fname;
        treeobj.mode='100644';
        treeobj.type='Blob';
        //treeobj.content=fileContent;
		treeobj.content=EncodingUtil.base64Decode(fileContent); // added to decode 
        List<NewTreeRequest> treeobjList = new List<NewTreeRequest>();
        treeobjList.add(treeobj);
    	reqBody.base_tree = shaOfBaseTree;
        reqBody.tree=treeobjList;       
        Http h = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/'+ valuekeyPair.get(selectlabel) + '/git/trees');
        req.setMethod('POST'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        String username = 'robinjainaccenture';
        String password = 'Robi@123';        
        Blob headerValue = Blob.valueOf(username + ':' + password);
        String authorizationHeader = 'BASIC ' +
        EncodingUtil.base64Encode(headerValue);
        req.setHeader('Authorization', authorizationHeader);
        req.setBody(JSON.serialize(reqBody));
        HttpResponse res = h.send(req);
        System.debug('res.getBody() getSHAofNewTree === ' + res.getBody());        
        GetSHAofNewTreeWrapper getSHAofNewTreeWrapper = (GetSHAofNewTreeWrapper)JSON.deserialize(res.getBody(), GetSHAofNewTreeWrapper.class);
        shaOfNewTree=getSHAofNewTreeWrapper.sha;
        createCommitOnNewTree();
    }
    
    //This method will create Commit On New Tree
    public void createCommitOnNewTree()
    {
        System.debug('Reference SHA of New Tree === ' + shaOfNewTree);
        Http h = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/'+ valuekeyPair.get(selectlabel) +'/git/commits');
        req.setMethod('POST'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        String username = 'robinjainaccenture';
        String password = 'Robi@123';        
        Blob headerValue = Blob.valueOf(username + ':' + password);
        String authorizationHeader = 'BASIC ' + EncodingUtil.base64Encode(headerValue);
        req.setHeader('Authorization', authorizationHeader);
        string body = '{"message": "My commited change from REST API","author": {"name": "robinjainaccenture","email": "robin.b.jain@accenture.com","date": "2018-03-24T16:13:30+12:00"},"parents": ["'+shalatestCommit+'"],"tree": "'+shaOfNewTree+'"}';
        req.setBody(body);
        HttpResponse res = h.send(req);
        System.debug('res.getBody() createCommitOnNewTree === ' + res.getBody());        
        GetSHAofNewCommit getSHAofNewCommit = (GetSHAofNewCommit)JSON.deserialize(res.getBody(), GetSHAofNewCommit.class);
        shaofNewCommit=getSHAofNewCommit.sha;        
        //System.debug('shaofNewCommit === ' + shaofNewCommit);
        createFinalFile();
    }
    
    //This method will create Final File
    public void createFinalFile()
    {
        System.debug('Reference SHA of New Commit === ' + shaofNewCommit);
        Http h = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/'+ valuekeyPair.get(selectlabel) +'/git/refs/heads/master');
        req.setMethod('POST'); 
        req.setHeader('Content-Type', 'application/vnd.github.v3+json');
        String username = 'robinjainaccenture';
        String password = 'Robi@123';       
        Blob headerValue = Blob.valueOf(username + ':' + password);
        String authorizationHeader = 'BASIC ' +EncodingUtil.base64Encode(headerValue);
        req.setHeader('Authorization', authorizationHeader);
        string body = '{"sha": "'+shaofNewCommit+'"}';
        req.setBody(body);
        HttpResponse res = h.send(req);
        System.debug('File Created === ' + res.getBody());   
        //readFile();
    }
	
	//This method is to read package.xml file
    public void readFile(){
        Http h = new Http(); 
        HttpRequest req = new HttpRequest();
        req.setEndpoint('https://api.github.com/repos/robinjainaccenture/MyRepo/contents/package.xml');
        req.setMethod('GET'); 
        req.setHeader('Content-Type', 'application/xml');
        HttpResponse res = h.send(req);
        System.debug('res.getBody() ==== '+res.getBody());
        
        ReadFileWrapper readFileWrapper = (ReadFileWrapper)JSON.deserialize(res.getBody(), ReadFileWrapper.class);
        readFileContent=readFileWrapper.content;
        Blob blobDoc =EncodingUtil.base64Decode(readFileContent);
        String xml= blobDoc.tostring();
        Dom.Document doc = new Dom.Document();
		doc.load(xml);
        System.debug('doc === '+doc);
        System.debug('xml === '+xml);
        Dom.XmlNode packageElement = doc.getRootElement(); 
        dom.XmlNode [] typeElementList = packageElement.getchildelements() ;
        Map<String,List<String>> mapNameMembers;
        List<String> member;
        //String name=null;
        for(Dom.XMLNode child : typeElementList){
            for (dom.XmlNode awr : child.getchildren() ) {
                if (awr.getname() == 'members'){
                    //system.debug('members === ' + awr.gettext());
                    member=new List<String>();
                    mapNameMembers=new  Map<String,List<String>>();
                    member.add(awr.gettext());
                }
                if (awr.getname() == 'name'){
                    system.debug('name === ' + awr.gettext());
                    String name=awr.gettext();
                    mapNameMembers.put(name,member);
                	system.debug('mapNameMembers === ' + mapNameMembers);
                }
        	}
        }	
    }    
    
    public class ReadFileWrapper{
        public String content;
    }
    public class RepositoryListWrapper{
        public String id;
        public String name;
        public String full_name;
    }
    public class GetReferenceWrapper {
        public String ref;
        public String url;
        public RefObject result;
    }
    public class RefObject{
        public String sha;
        public String type;
        public String url;
    }
    public class GetSHAofBaseTreeWrapper {
        public String sha;
        public String url;
        public String html_url;
        public RefObjectBaseTree tree;
        public String message;
        
    }
    public class RefObjectBaseTree{
        public String sha;
        public String url;
    }
    public class GetSHAofNewTreeWrapper {
        public String sha;
        public String url;
    }
    public class GetSHAofNewCommit {
        public String sha;
        public String url;
    }
    public class SHAofNewTreeRequest {
		public String base_tree;
        public List<NewTreeRequest> tree;
    } 

	public class NewTreeRequest {
		public String path;
        public String mode;
		public String type;
		public String content;
    }
}