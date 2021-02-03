<%@ page import="sa.elm.ob.utility.properties.Resource"%><%String lang = (((String) session.getAttribute("#AD_LANGUAGE")));%>
<style type="text/css"> #DivOverlayBG:active + #DivOverlayContent { transform: scale3d(1.2, 1.2, 1.2); } #DivOverlayContent { transition: all 0.05s ease-in-out; } </style>
<div id="DivOverlayBG" style="display: none; position: absolute; z-index: 5000000; background-color: black; opacity: .5; top: 0px; left: 0px; width: 100%; height: 100%; filter: alpha(opacity=50)"></div>
<div id="DivOverlayContent" style="display: none; width: 800px; height: 0px; text-align: center; background: none; z-index: 5000001; position: absolute; margin: auto; top: -50px; right: 0; bottom: 0; left: 0;"><div><img src='../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/org.openbravo.client.application/images/system/windowLoading.gif'></img></div><div id='DivOverlayContentTXT' style="color: #ccd0d4; font-family: sans-serif; font-weight: bold; font-size: 14px; font-weight: bold; margin-top: 15px;"></div></div>
<input type="hidden" id="InputOverlayBG"></input>
<script type="text/javascript">
var pBStartSec = 0, pBEndDec = 0, pBMinTime = 0.3, pBMinTimeOut = 350, allowKeyDown = true;
function showProcessBar(t, val) {
    var value = "";
    if (typeof t == "undefined") return;
    if (typeof val == "undefined" || val == 0 || val == 1) value = '<%=Resource.getProperty("utility.loading", lang)%>';
    else if (val == 2) value = '<%=Resource.getProperty("utility.processing", lang)%>';
    else if (val == 3) value = '<%=Resource.getProperty("utility.downloading", lang)%>';
    else if (val == 4) value = '<%=Resource.getProperty("utility.uploading", lang)%>';
    else if (val == 5) value = '<%=Resource.getProperty("utility.saving", lang)%>';
    else value = val;
    if (typeof t == "undefined" || !t) {
        pBEndDec = parseFloat(new Date().getTime() / 1000).toFixed(1);
        if (parseFloat(pBEndDec - pBStartSec).toFixed(1) < pBMinTime) setTimeout(function() { hideProcessBar(); }, pBMinTimeOut);
        else hideProcessBar();
    }
    else if (t) {
        pBStartSec = parseFloat(new Date().getTime() / 1000).toFixed(1);
        document.getElementById("DivOverlayContentTXT").innerHTML = value + '...';
        document.getElementById("DivOverlayBG").style.display = "";
        document.getElementById("DivOverlayContent").style.display = "";
        allowKeyDown = false;
        document.getElementById("InputOverlayBG").focus();
    }
}
function hideProcessBar() { document.getElementById("DivOverlayBG").style.display = "none"; document.getElementById("DivOverlayContent").style.display = "none"; allowKeyDown = true; }
document.onkeydown = function(evt) { document.getElementById("DivOverlayContent").style.transform = "scale3d(1.1, 1.1, 1.1)"; setTimeout(function() { document.getElementById("DivOverlayContent").style.transform = "none"; }, 100); if (!allowKeyDown) return false; };
document.onkeypress = function(evt) { if (!allowKeyDown) return false; };
document.getElementById("DivOverlayContent").style.transition = "all 0.05s ease-in-out";
</script>