function startTest(dlurl, checkerurl, currentVersion, useExcludeList) {
    if (!useExcludeList) {
        window.graph.document.location.href="graph.jsp?mode=testnow&useExcludeList=no";
    } else {
        if (excludeListIsUpToDate(checkerurl + "?v=" + currentVersion)) {
            window.graph.document.location.href="graph.jsp?mode=testnow&useExcludeList=yes";
        } else {
            if (window.confirm("The Exclude List is out of date.\nPress OK to continue testing or CANCEL to get pointed to the download page")) {
                window.graph.document.location.href="graph.jsp?mode=testnow&useExcludeList=yes";
            } else {
                var dlwin = window.open(dlurl,'DownloadExcludeList','width=800,height=600')
            }
        }
    }
}

function isUpToDate() {
    return true;
}

function isOutOfDate() {
    return false;
}

function excludeListIsUpToDate(url) {
    var tester = new Image();
    tester.onload = isUpToDate;
    tester.onerror = isOutOfDate;
    tester.src = url;
}


