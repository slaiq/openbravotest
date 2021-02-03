function getXMLObject()  //XML OBJECT
{
   var xmlHttp = false;
   try {
     xmlHttp = new ActiveXObject("Msxml2.XMLHTTP")  // For Old Microsoft Browsers
   }
   catch (e) {
     try {
       xmlHttp = new ActiveXObject("Microsoft.XMLHTTP")  // For Microsoft IE 6.0+
     }
     catch (e2) {
       xmlHttp = false   // No Browser accepts the XMLHTTP Object then false
     }
   }
   if (!xmlHttp && typeof XMLHttpRequest != 'undefinedss') {
     xmlHttp = new XMLHttpRequest();        //For Mozilla, Opera Browsers
   }
   return xmlHttp;  // Mandatory Statement returning the ajax object created
}

function hidediv() {
if (document.getElementById) { // DOM3 = IE5, NS6
document.getElementById('errorMessage').style.visibility = 'hidden';
}
else {
if (document.layers) { // Netscape 4
document.errorMessage.visibility = 'hidden';
}
else { // IE 4
document.all.errorMessage.style.visibility = 'hidden';
}
}
}

function showdiv(Message) {
if (document.getElementById) { // DOM3 = IE5, NS6
document.getElementById('errorMessage').style.visibility = 'visible';
document.getElementById('msgId').innerHTML = Message;
}
else {
if (document.layers) { // Netscape 4
document.errorMessage.visibility = 'visible';
}
else { // IE 4
document.all.errorMessage.style.visibility = 'visible';
}
}
}
//For Success Message 
function SuccessMessage(Message)
{
   var ID=document.getElementById("messageBoxID");
   ID.className="MessageBoxERROR";
   ID2=document.getElementById("messageBoxIDTitle");
   ID3=document.getElementById("messageBoxIDMessage");
   ID2.innerHTML="Error:";
   ID3.innerHTML=Message;
}

//For Rollover Effect:

function changeBgColor(row)
{
	var rowstyle="DataGrid_Body_Row_Odd";
	var i=row.rowIndex;
	if(i%2==0)
	    rowstyle="DataGrid_Body_Row_Odd";
	else
	    rowstyle="DataGrid_Body_Row_Even";  
	row.className =rowstyle;
}

//For Hide Div
function hidRelationalDetails(RequestDetails){
    if (document.getElementById) { // DOM3 = IE5, NS6
        document.getElementById(RequestDetails).style.visibility = 'hidden';
    }
    else {
    if (document.layers) { // Netscape 4
        document.RequestDetails.visibility = 'hidden';
    }
    else { // IE 4
        document.all.RequestDetails.style.visibility = 'hidden';
    }
    }
}

function showRelationalDetails(RequestDetails){

    displayLogicElement('messageBoxID',false); //This is for hiding alert messages success or failure

    if (document.getElementById) { // DOM3 = IE5, NS6
        document.getElementById(RequestDetails).style.visibility = 'visible';
    }
    else {
    if (document.layers) { // Netscape 4
        document.RequestDetails.visibility = 'visible';
    }
    else { // IE 4
        document.all.RequestDetails.style.visibility = 'visible';
    }
    }
}
