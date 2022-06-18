# sharepoint_file_download

Sample code for download a latest xls file in a specified sharepoint portal...



```

using Microsoft.SharePoint.Client;
using System.Collections.Generic;
using System.IO;
using System.Security;
using SP = Microsoft.SharePoint.Client;
using System;

...

 
using (var clientContext = new ClientContext(@"https://<sharepoint_url>"))
{
 
     SecureString password = new SecureString();
     foreach (char c in "<password>".ToCharArray()) password.AppendChar(c);
     clientContext.Credentials = new SharePointOnlineCredentials("<username>@<domain>", password);
     Web web = clientContext.Web;
     clientContext.Load(web);
     clientContext.ExecuteQuery();
 
     List list = clientContext.Web.Lists.GetByTitle("Documents");
 
     clientContext.Load(list);
     clientContext.Load(list.RootFolder);
     clientContext.Load(list.RootFolder.Folders);
     clientContext.Load(list.RootFolder.Files);
     clientContext.ExecuteQuery();
     FolderCollection fcol = list.RootFolder.Folders;
     List<string> lstFile = new List<string>();
     foreach (Folder f in fcol)
     {
           if (f.Name != "<expected folder>")
           {
                continue;
           }
 
           clientContext.Load(f.Files);
           clientContext.ExecuteQuery();
           FileCollection fileCol = f.Files;
           DateTime dt = DateTime.MinValue;
           SP.File lastFile = null;
           foreach (SP.File file in fileCol)
           {
                if (!file.Name.EndsWith(".xls"))
                {
                      continue;
                }
                if (file.TimeCreated>dt)
                {
                      dt = file.TimeCreated;
                      lastFile = file;
                } 
           }
 
           var fileRef = lastFile.ServerRelativeUrl;
           var fileInfo = SP.File.OpenBinaryDirect(clientContext, fileRef);
           var fileName = Path.Combine(@"c:\<download path>\", (string)lastFile.Name);
           using (var fileStream = System.IO.File.Create(fileName))
           {
                fileInfo.Stream.CopyTo(fileStream);
           }  
     }
}
 

...
