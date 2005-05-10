var IFrameObj; // our IFrame object
var IFrameDoc;
var successfull = false;
var baseHref;

function callToServer(url) {
  baseHref = document.location.href.substring(0, document.location.href.lastIndexOf("/"));

  if (!document.createElement) {
    return true
  }
  IFrameDoc;
  if (!IFrameObj && document.createElement) {
      // create the IFrame and assign a reference to the
      // object to our global variable IFrameObj.
      // this will only happen the first time
      // callToServer() is called
      var tempIFrame=document.createElement('iframe');
      tempIFrame.setAttribute('id','RSIFrame');
      tempIFrame.style.border='0px';
      tempIFrame.style.width='0px';
      tempIFrame.style.height='0px';
      tempIFrame.setAttribute('src','excludelisttest.jsp');
      IFrameObj = document.body.appendChild(tempIFrame);

      if (document.frames) {
        // this is for IE5 Mac, because it will only
        // allow access to the document object
        // of the IFrame if we access it through
        // the document.frames array
        IFrameObj = document.frames['RSIFrame'];
      }
    }

  if (IFrameObj.contentDocument) {
    // For NS6
    IFrameDoc = IFrameObj.contentDocument;
  } else if (IFrameObj.contentWindow) {
    // For IE5.5 and IE6
    IFrameDoc = IFrameObj.contentWindow.document;
  } else if (IFrameObj.document) {
    // For IE5
    IFrameDoc = IFrameObj.document;
  } else {
    return true;
  }

  IFrameDoc.location.replace(url);

  return successfull;
}

function startTest(url, currentVersion, useExcludeList, autoupdate) {
    if (!useExcludeList) {
        window.graph.document.location.href="graph.jsp?mode=testnow&useExcludeList=no";
    } else if (autoupdate){
        callToServer(url + "?Show=1&checkVersion=" + currentVersion);
    } else {
        window.graph.document.location.href="graph.jsp?mode=testnow&useExcludeList=yes";
    }
}

function checkAndUpdate(doc) {
        if (!excludeListIsUpToDate(doc)) {
        	// start update process
        	alert("The Exclude List is no more valid.\nGoing to download the the latest version");
        	updateExcludeList(baseHref + "/update_exclude_list.jsp?action=update", doc);
        	alert("The Exclude List is update.\nGoing to start the test.");
     	}

     	window.graph.document.location.href=baseHref+"/graph.jsp?mode=testnow";
}

function excludeListIsUpToDate(doc) {
    var response = doc.getElementById("isUpToDate").innerHTML;
    if (response == "yes") {
        return true;
    } else {
        return false;
    }
}

function getExcludeList(doc) {
    return doc.getElementById("list").innerHTML;
}

function getVersion(doc) {
    return doc.getElementById("version").innerHTML;
}

function updateExcludeList(url, doc) {
    var httpcon = document.all ? new ActiveXObject("Microsoft.XMLHTTP") : new XMLHttpRequest();
    if (httpcon) {
    	url += "&version=" + escape(getVersion(doc)) + "&ExcludeList=" + escape(getExcludeList(doc))
        httpcon.open('POST', url, false);
        //httpcon.send("version=" + escape(getVersion(doc)) + "&ExcludeList=" + escape(getExcludeList(doc)));
        httpcon.send(null);
    }
}

